# P0-12 — Wallet & Ledger Architecture: Double-Entry, Transactions, Audit Trail

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> **Production Truth**: A wallet is not a number in a database column. It's a financial ledger — an immutable, append-only record of every rupee that ever flowed in or out. The moment you use a mutable `balance` field as your source of truth, you've created a system that can lose money, double-spend, and be impossible to audit. This chapter shows you how to build a wallet system that would survive a CA audit.

---

## Why Wallets Are Architecturally Complex

At first glance, a wallet seems simple: store a balance, debit or credit it. But consider:

1. **User A and User B simultaneously pay from the same wallet** (race condition)
2. **Server crashes after debiting wallet but before confirming order** (partial failure)
3. **Cashback expires after 30 days** — but how do you track which rupees are expired?
4. **Audit request**: "Show me every transaction for user X's wallet for the last 6 months"
5. **Reconciliation**: "The total of all wallet balances should equal total deposits minus total withdrawals — verify this"
6. **Refund**: "User returned the order — credit back exactly the rupees they spent"

A mutable balance column fails at every one of these scenarios. Double-entry bookkeeping solves all of them.

---

## Double-Entry Bookkeeping: The Foundation

### The Principle

Borrowed from 500-year-old accounting: **every financial transaction has two sides that must balance to zero**.

```
Transaction: User A pays ₹500 for an order

DEBIT  | A's Wallet    | ₹500  (wallet decreases)
CREDIT | Revenue       | ₹500  (company gains)

Sum of all entries for this transaction = 0 (₹500 debit - ₹500 credit = 0)
```

In a wallet context:
```
Transaction: User tops up ₹1000

DEBIT  | Bank Account (asset increases)  | ₹1000
CREDIT | User Wallet (liability increases)| ₹1000  — company owes user ₹1000

Transaction: User spends ₹500

DEBIT  | User Wallet (liability decreases)| ₹500   — company owes ₹500 less
CREDIT | Revenue (income increases)       | ₹500
```

### Why This Matters For You

In a production wallet system, double-entry gives you:
- **Auditability**: Every entry in the ledger has a corresponding opposite entry — nothing appears from nowhere
- **Consistency checking**: Sum of all ledger entries for any wallet = current balance. If it doesn't, data is corrupted.
- **No mutable state**: You never update a balance row; you only append new ledger entries
- **Refund correctness**: Reversing a transaction means creating equal-and-opposite entries, not fiddling with a number

---

## Database Schema: The Ledger-First Design

### Core Tables

```sql
-- ─── Wallets Table ─────────────────────────────────────────────────────────
-- Wallets represent the account, NOT the balance
-- Balance is derived from ledger entries, not stored here
CREATE TABLE wallets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         VARCHAR(100) NOT NULL UNIQUE,   -- One wallet per user (simplest model)
    wallet_type     VARCHAR(30) NOT NULL DEFAULT 'PRIMARY',
    -- wallet_type options: PRIMARY, CASHBACK, REFERRAL, PROMOTIONAL
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    -- status options: ACTIVE, FROZEN (fraud), CLOSED
    currency        VARCHAR(3) NOT NULL DEFAULT 'INR',
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Optional: snapshot balance for performance (must be validated against ledger)
    -- See "Snapshot + Ledger Hybrid" section below
    cached_balance  BIGINT NOT NULL DEFAULT 0,      -- In paise — snapshot, NOT source of truth
    snapshot_as_of  TIMESTAMP WITH TIME ZONE        -- When was this snapshot computed?
);

CREATE UNIQUE INDEX idx_wallets_user_id ON wallets(user_id);

-- ─── Ledger Entries Table — THE SOURCE OF TRUTH ─────────────────────────────
-- Immutable, append-only. Never UPDATE or DELETE rows in this table.
-- Every rupee movement is recorded here exactly once.
CREATE TABLE ledger_entries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Which wallet this entry belongs to
    wallet_id       UUID NOT NULL REFERENCES wallets(id),
    
    -- CREDIT: money coming IN to the wallet (balance increases)
    -- DEBIT:  money going OUT of the wallet (balance decreases)
    entry_type      VARCHAR(6) NOT NULL CHECK (entry_type IN ('CREDIT', 'DEBIT')),
    
    -- Amount in paise — ALWAYS positive. Direction comes from entry_type.
    -- Never store negative amounts. A DEBIT of 50000 means -₹500.
    amount          BIGINT NOT NULL CHECK (amount > 0),
    
    -- What caused this ledger entry?
    -- Reference to the business object that triggered this movement
    reference_id    VARCHAR(100) NOT NULL,      -- Order ID, refund ID, top-up ID, etc.
    reference_type  VARCHAR(50) NOT NULL,       -- 'ORDER_PAYMENT', 'REFUND', 'TOPUP', 'CASHBACK', 'EXPIRY'
    
    -- Balance AFTER this entry (running balance — useful for statement views)
    -- Computed at write time and stored. DO NOT use for authoritative balance.
    balance_after   BIGINT NOT NULL,
    
    -- Audit fields
    description     TEXT,                       -- Human-readable description
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by      VARCHAR(100),               -- User ID or 'SYSTEM'
    
    -- For cashback expiry tracking: which ledger entry does this expiry relate to?
    related_entry_id UUID REFERENCES ledger_entries(id),
    
    -- Idempotency: prevent duplicate entries for the same operation
    idempotency_key VARCHAR(255) UNIQUE,
    
    -- Metadata for audit trail
    metadata        JSONB DEFAULT '{}'
);

-- Indexes for common query patterns
CREATE INDEX idx_ledger_wallet_id ON ledger_entries(wallet_id);
CREATE INDEX idx_ledger_wallet_created ON ledger_entries(wallet_id, created_at DESC);
CREATE INDEX idx_ledger_reference ON ledger_entries(reference_id, reference_type);
CREATE INDEX idx_ledger_idempotency ON ledger_entries(idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- ─── Wallet Transactions Table — Business-Level Transaction Grouping ─────────
-- Groups related ledger entries that form a single business operation
-- Example: Wallet payment = 1 DEBIT entry on user wallet + 1 CREDIT entry on revenue account
CREATE TABLE wallet_transactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_type VARCHAR(50) NOT NULL,    -- 'PAYMENT', 'REFUND', 'TOPUP', 'CASHBACK'
    status          VARCHAR(30) NOT NULL,
    reference_id    VARCHAR(100) NOT NULL,    -- Business object that triggered this
    total_amount    BIGINT NOT NULL,
    description     TEXT,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at    TIMESTAMP WITH TIME ZONE
);

-- ─── Cashback / Promotional Balance Tracking ──────────────────────────────────
-- Tracks cashback grants with expiry for wallet_type = CASHBACK
CREATE TABLE wallet_credits (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id       UUID NOT NULL REFERENCES wallets(id),
    ledger_entry_id UUID NOT NULL REFERENCES ledger_entries(id),  -- The credit entry
    amount          BIGINT NOT NULL,
    remaining       BIGINT NOT NULL,    -- How much of this credit is unspent
    expires_at      TIMESTAMP WITH TIME ZONE,
    expired         BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_credits_wallet_expiry ON wallet_credits(wallet_id, expires_at)
    WHERE expired = FALSE;
```

---

## Balance Calculation: Never Trust a Stored Balance

### The Pure Ledger Approach

```sql
-- Authoritative balance calculation: sum of all credits minus sum of all debits
SELECT 
    w.id AS wallet_id,
    w.user_id,
    COALESCE(SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE 0 END), 0) -
    COALESCE(SUM(CASE WHEN le.entry_type = 'DEBIT' THEN le.amount ELSE 0 END), 0) AS balance_paise,
    (
        COALESCE(SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE 0 END), 0) -
        COALESCE(SUM(CASE WHEN le.entry_type = 'DEBIT' THEN le.amount ELSE 0 END), 0)
    ) / 100.0 AS balance_rupees
FROM wallets w
LEFT JOIN ledger_entries le ON le.wallet_id = w.id
WHERE w.user_id = 'usr_9K3m2'
GROUP BY w.id, w.user_id;
```

**Problem**: As ledger grows to millions of entries per wallet (high-frequency users), this query gets expensive.

### The Snapshot + Ledger Hybrid (Production Approach)

Keep a cached balance snapshot, but **always validate it against the ledger** for critical operations:

```sql
-- Wallet statement view: recent entries with running balance
SELECT 
    le.id,
    le.entry_type,
    le.amount / 100.0 AS amount_rupees,
    le.balance_after / 100.0 AS balance_after_rupees,
    le.reference_type,
    le.description,
    le.created_at
FROM ledger_entries le
WHERE le.wallet_id = $1
ORDER BY le.created_at DESC
LIMIT 50;
```

```kotlin
// Balance: use snapshot for display, recompute for transactions
fun getDisplayBalance(walletId: UUID): Long {
    val wallet = walletRepository.findById(walletId).orElseThrow()
    
    // If snapshot is recent (< 5 min old), use it for display
    if (wallet.snapshotAsOf != null && 
        wallet.snapshotAsOf.isAfter(Instant.now().minusSeconds(300))) {
        return wallet.cachedBalance
    }
    
    // Recompute from ledger and update snapshot
    val computedBalance = computeBalanceFromLedger(walletId)
    walletRepository.updateSnapshot(walletId, computedBalance, Instant.now())
    return computedBalance
}

fun computeBalanceFromLedger(walletId: UUID): Long {
    return ledgerRepository.computeBalance(walletId)
}
```

> [!WARNING]
> **NEVER use the cached/snapshot balance for authoritative payment decisions** (like "does the user have enough balance to pay?"). Always compute from the ledger inside the same transaction that will debit the wallet. The snapshot is for display purposes only. Using it for financial decisions creates race conditions.

---

## Concurrency: The Core Challenge

### The Race Condition Scenario

```
Time: 09:00:00.000
User wallet balance: ₹500

Thread A (Order 1 - ₹400): Read balance = ₹500, check > 400: ✅
Thread B (Order 2 - ₹300): Read balance = ₹500, check > 300: ✅

Thread A: Debit ₹400 → balance = ₹100
Thread B: Debit ₹300 → balance = ₹200  ← WRONG! Should be -₹200 (insufficient funds)
```

Both threads read the balance before either wrote. Neither saw the other's debit. The wallet is now overdrawn by ₹200.

### Solution 1: Pessimistic Locking (SELECT FOR UPDATE)

```kotlin
// src/main/kotlin/com/yourapp/wallet/service/WalletService.kt
@Service
class WalletService(
    private val walletRepository: WalletRepository,
    private val ledgerRepository: LedgerRepository
) {

    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun debitWallet(
        walletId: UUID,
        amountInPaise: Long,
        referenceId: String,
        referenceType: String,
        description: String,
        idempotencyKey: String
    ): LedgerEntry {
        require(amountInPaise > 0) { "Debit amount must be positive" }

        // ─── PESSIMISTIC LOCK: SELECT ... FOR UPDATE ─────────────────────────────
        // This locks the wallet row until the transaction commits or rolls back.
        // Any other transaction trying to lock this wallet BLOCKS here until we're done.
        // This serializes all concurrent debits on the same wallet — no race conditions.
        val wallet = walletRepository.findByIdForUpdate(walletId)
            ?: throw WalletNotFoundException("Wallet not found: $walletId")

        // Check wallet status
        if (wallet.status != WalletStatus.ACTIVE) {
            throw WalletFrozenException("Wallet is ${wallet.status}")
        }

        // Idempotency check: has this debit already been processed?
        val existingEntry = ledgerRepository.findByIdempotencyKey(idempotencyKey)
        if (existingEntry != null) {
            return existingEntry  // Return existing result — safe retry
        }

        // ─── BALANCE CHECK ───────────────────────────────────────────────────────
        // Compute balance from ledger INSIDE this transaction (after locking the wallet)
        // This is the authoritative balance — no cached values
        val currentBalance = ledgerRepository.computeBalance(walletId)

        if (currentBalance < amountInPaise) {
            throw InsufficientBalanceException(
                "Insufficient balance. Available: $currentBalance paise, Required: $amountInPaise paise"
            )
        }

        val balanceAfter = currentBalance - amountInPaise

        // ─── CREATE LEDGER ENTRY ─────────────────────────────────────────────────
        val entry = ledgerRepository.save(LedgerEntry(
            walletId = walletId,
            entryType = EntryType.DEBIT,
            amount = amountInPaise,
            referenceId = referenceId,
            referenceType = referenceType,
            balanceAfter = balanceAfter,
            description = description,
            idempotencyKey = idempotencyKey,
            createdBy = "SYSTEM"
        ))

        // ─── UPDATE SNAPSHOT ─────────────────────────────────────────────────────
        walletRepository.updateSnapshot(walletId, balanceAfter, Instant.now())

        return entry
        // Transaction commits → lock released → next waiting transaction proceeds
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun creditWallet(
        walletId: UUID,
        amountInPaise: Long,
        referenceId: String,
        referenceType: String,
        description: String,
        idempotencyKey: String,
        expiresAt: Instant? = null  // For cashback with expiry
    ): LedgerEntry {
        require(amountInPaise > 0) { "Credit amount must be positive" }

        val wallet = walletRepository.findByIdForUpdate(walletId)
            ?: throw WalletNotFoundException("Wallet not found: $walletId")

        // Idempotency check
        val existingEntry = ledgerRepository.findByIdempotencyKey(idempotencyKey)
        if (existingEntry != null) {
            return existingEntry
        }

        val currentBalance = ledgerRepository.computeBalance(walletId)
        val balanceAfter = currentBalance + amountInPaise

        val entry = ledgerRepository.save(LedgerEntry(
            walletId = walletId,
            entryType = EntryType.CREDIT,
            amount = amountInPaise,
            referenceId = referenceId,
            referenceType = referenceType,
            balanceAfter = balanceAfter,
            description = description,
            idempotencyKey = idempotencyKey,
            createdBy = "SYSTEM"
        ))

        // If this credit has an expiry (cashback), create tracking record
        if (expiresAt != null) {
            walletCreditRepository.save(WalletCredit(
                walletId = walletId,
                ledgerEntryId = entry.id!!,
                amount = amountInPaise,
                remaining = amountInPaise,
                expiresAt = expiresAt
            ))
        }

        walletRepository.updateSnapshot(walletId, balanceAfter, Instant.now())

        return entry
    }
}
```

```kotlin
// src/main/kotlin/com/yourapp/wallet/repository/WalletRepository.kt
package com.yourapp.wallet.repository

import com.yourapp.wallet.domain.Wallet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID
import jakarta.persistence.LockModeType

interface WalletRepository : JpaRepository<Wallet, UUID> {

    @Query("SELECT w FROM Wallet w WHERE w.id = :walletId")
    @Lock(LockModeType.PESSIMISTIC_WRITE)  // Translates to SELECT ... FOR UPDATE
    fun findByIdForUpdate(@Param("walletId") walletId: UUID): Wallet?

    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId")
    fun findByUserId(@Param("userId") userId: String): Wallet?

    @Modifying
    @Query("""
        UPDATE Wallet w 
        SET w.cachedBalance = :balance, w.snapshotAsOf = :asOf, w.updatedAt = NOW()
        WHERE w.id = :walletId
    """)
    fun updateSnapshot(
        @Param("walletId") walletId: UUID,
        @Param("balance") balance: Long,
        @Param("asOf") asOf: Instant
    )
}
```

```kotlin
// src/main/kotlin/com/yourapp/wallet/repository/LedgerRepository.kt
interface LedgerRepository : JpaRepository<LedgerEntry, UUID> {

    @Query("""
        SELECT COALESCE(
            SUM(CASE WHEN le.entryType = 'CREDIT' THEN le.amount ELSE -le.amount END), 
            0
        )
        FROM LedgerEntry le
        WHERE le.walletId = :walletId
    """)
    fun computeBalance(@Param("walletId") walletId: UUID): Long

    fun findByIdempotencyKey(idempotencyKey: String): LedgerEntry?

    @Query("""
        SELECT le FROM LedgerEntry le 
        WHERE le.walletId = :walletId 
        ORDER BY le.createdAt DESC
    """)
    fun findByWalletIdOrderByCreatedAtDesc(
        @Param("walletId") walletId: UUID,
        pageable: org.springframework.data.domain.Pageable
    ): org.springframework.data.domain.Page<LedgerEntry>
}
```

### Solution 2: Optimistic Locking (For Lower Contention Scenarios)

```kotlin
// Wallet entity with optimistic lock version
@Entity
@Table(name = "wallets")
class Wallet {
    @Id
    @GeneratedValue
    var id: UUID? = null

    @Version
    var version: Long = 0  // JPA's optimistic lock version

    var userId: String = ""
    var cachedBalance: Long = 0
    var status: WalletStatus = WalletStatus.ACTIVE
    // ... other fields
}

// Service using optimistic locking
fun debitWalletOptimistic(walletId: UUID, amount: Long): LedgerEntry {
    var attempts = 0
    val maxAttempts = 3

    while (attempts < maxAttempts) {
        try {
            return doDebitInTransaction(walletId, amount)
        } catch (e: org.springframework.orm.ObjectOptimisticLockingFailureException) {
            attempts++
            if (attempts >= maxAttempts) {
                throw WalletConflictException("Wallet update conflict after $maxAttempts attempts")
            }
            // Exponential backoff before retry
            Thread.sleep((attempts * 50).toLong())
        }
    }
    throw IllegalStateException("Should not reach here")
}
```

> [!IMPORTANT]
> **Pessimistic locking vs Optimistic locking** — when to use which:
> - **Pessimistic (SELECT FOR UPDATE)**: High contention (many concurrent transactions on same wallet). Guaranteed no retries. Higher DB lock overhead. Best for wallets that get many concurrent payments (high-usage users).
> - **Optimistic (@Version)**: Low contention (most users don't have concurrent payments). No lock overhead normally. Requires retry logic. Can fail under high contention. Best for low-traffic wallets.
>
> For a commerce wallet system, **pessimistic locking is the correct choice**. A failed optimistic lock retry means re-executing business logic, which can have side effects. Pessimistic locking blocks cleanly.

---

## Entity Classes

```kotlin
// src/main/kotlin/com/yourapp/wallet/domain/Wallet.kt
package com.yourapp.wallet.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "wallets")
class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(name = "user_id", nullable = false, unique = true)
    var userId: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type", nullable = false)
    var walletType: WalletType = WalletType.PRIMARY

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: WalletStatus = WalletStatus.ACTIVE

    @Column(nullable = false)
    var currency: String = "INR"

    @Column(name = "cached_balance", nullable = false)
    var cachedBalance: Long = 0

    @Column(name = "snapshot_as_of")
    var snapshotAsOf: Instant? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
}

enum class WalletType { PRIMARY, CASHBACK, REFERRAL, PROMOTIONAL }
enum class WalletStatus { ACTIVE, FROZEN, CLOSED }
```

```kotlin
// src/main/kotlin/com/yourapp/wallet/domain/LedgerEntry.kt
package com.yourapp.wallet.domain

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "ledger_entries")
class LedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(name = "wallet_id", nullable = false)
    var walletId: UUID = UUID.randomUUID()

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 6)
    var entryType: EntryType = EntryType.CREDIT

    @Column(nullable = false)
    var amount: Long = 0  // Always positive, direction from entryType

    @Column(name = "reference_id", nullable = false)
    var referenceId: String = ""

    @Column(name = "reference_type", nullable = false)
    var referenceType: String = ""

    @Column(name = "balance_after", nullable = false)
    var balanceAfter: Long = 0  // Balance after this entry

    var description: String? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "created_by")
    var createdBy: String? = null

    @Column(name = "idempotency_key", unique = true)
    var idempotencyKey: String? = null

    @Column(name = "related_entry_id")
    var relatedEntryId: UUID? = null  // For expiry entries: points to original cashback entry

    @JdbcTypeCode(SqlTypes.JSON)
    var metadata: Map<String, Any> = emptyMap()
}

enum class EntryType { CREDIT, DEBIT }
```

---

## Atomic Payment with Wallet: The Full Transaction

When a user pays for an order using their wallet, three things must happen atomically:
1. Debit user's wallet
2. Record the payment in payments table
3. Create ledger entry
4. Update order status

```kotlin
// src/main/kotlin/com/yourapp/payment/service/WalletPaymentService.kt
@Service
class WalletPaymentService(
    private val walletService: WalletService,
    private val paymentRepository: PaymentRepository,
    private val outboxRepository: OutboxRepository
) {

    private val log = LoggerFactory.getLogger(WalletPaymentService::class.java)

    /**
     * Processes a wallet payment atomically.
     * All operations happen in a single DB transaction.
     * If ANY operation fails, ALL are rolled back.
     */
    @Transactional(
        isolation = Isolation.READ_COMMITTED,
        rollbackFor = [Exception::class]  // Rollback on ANY exception, not just RuntimeException
    )
    fun processWalletPayment(
        userId: String,
        orderId: String,
        amountInPaise: Long
    ): WalletPaymentResult {
        log.info("Processing wallet payment - userId={} orderId={} amount={}", userId, orderId, amountInPaise)

        // 1. Find user's wallet
        val wallet = walletService.findWalletByUserId(userId)
            ?: throw WalletNotFoundException("Wallet not found for user: $userId")

        // Idempotency key: unique per order payment (not per attempt — retries use same key)
        val idempotencyKey = "wallet_payment_${orderId}"

        // 2. Debit wallet (includes lock + balance check + ledger entry)
        // This is atomic within the same transaction
        val ledgerEntry = walletService.debitWallet(
            walletId = wallet.id!!,
            amountInPaise = amountInPaise,
            referenceId = orderId,
            referenceType = "ORDER_PAYMENT",
            description = "Payment for order $orderId",
            idempotencyKey = idempotencyKey
        )

        // 3. Record payment
        val payment = paymentRepository.save(Payment(
            orderId = orderId,
            userId = userId,
            amount = amountInPaise,
            status = PaymentStatus.PAYMENT_CONFIRMED,
            gateway = "WALLET",
            gatewayPaymentId = ledgerEntry.id.toString(),
            confirmedAt = Instant.now()
        ))

        // 4. Write to outbox (for Order Service notification)
        outboxRepository.save(PaymentOutbox(
            eventType = "PAYMENT_CONFIRMED",
            orderId = orderId,
            payload = mapOf(
                "orderId" to orderId,
                "paymentId" to payment.id.toString(),
                "amount" to amountInPaise,
                "paymentMethod" to "WALLET"
            )
        ))

        log.info("Wallet payment successful - orderId={} ledgerEntryId={}", orderId, ledgerEntry.id)

        return WalletPaymentResult(
            paymentId = payment.id.toString(),
            ledgerEntryId = ledgerEntry.id.toString(),
            balanceAfterPaise = ledgerEntry.balanceAfter
        )
    }
}
```

> [!CAUTION]
> **Never call external services inside a `@Transactional` method**. If you call Razorpay's API inside a DB transaction, and the call takes 30 seconds, you're holding a DB connection and potentially a row lock for 30 seconds. At 100 concurrent users, you've exhausted your connection pool. Keep transactions short: acquire lock → check balance → write ledger → commit → then do external work.

---

## Wallet Types and Multi-Wallet Architecture

Real commerce apps have multiple wallet types for different purposes:

```kotlin
// Wallet priority for payment: which wallet to use first?
enum class WalletType(val priority: Int) {
    PROMOTIONAL(1),   // Use promotional credits first (they expire soonest)
    CASHBACK(2),      // Use cashback second
    REFERRAL(3),      // Use referral credits third
    PRIMARY(4)        // Use main wallet balance last
}

@Service
class MultiWalletPaymentService(
    private val walletRepository: WalletRepository,
    private val walletService: WalletService
) {

    /**
     * Pays using multiple wallets in priority order.
     * Example: ₹100 order, user has ₹30 cashback + ₹70 primary wallet
     *
     * Result: ₹30 from cashback wallet, ₹70 from primary wallet
     */
    @Transactional
    fun payWithMultipleWallets(
        userId: String,
        orderId: String,
        totalAmountInPaise: Long
    ): MultiWalletPaymentResult {
        val wallets = walletRepository.findAllByUserIdOrderByWalletTypePriority(userId)
        var remainingAmount = totalAmountInPaise
        val entries = mutableListOf<LedgerEntry>()

        for (wallet in wallets) {
            if (remainingAmount <= 0) break

            val walletBalance = walletService.computeBalance(wallet.id!!)
            val amountToDebitFromThisWallet = minOf(remainingAmount, walletBalance)

            if (amountToDebitFromThisWallet <= 0) continue

            val entry = walletService.debitWallet(
                walletId = wallet.id!!,
                amountInPaise = amountToDebitFromThisWallet,
                referenceId = orderId,
                referenceType = "ORDER_PAYMENT",
                description = "Order payment (${wallet.walletType}) for $orderId",
                idempotencyKey = "wallet_payment_${orderId}_${wallet.walletType}"
            )

            entries.add(entry)
            remainingAmount -= amountToDebitFromThisWallet
        }

        if (remainingAmount > 0) {
            throw InsufficientBalanceException(
                "Total wallet balance insufficient. Short by $remainingAmount paise"
            )
        }

        return MultiWalletPaymentResult(entries = entries, totalPaid = totalAmountInPaise)
    }
}
```

---

## Cashback Expiry

Cashback credits expire after a set period (e.g., 30 days). You need to:
1. Track which credits expire when
2. Not allow spending expired credits
3. Write expiry ledger entries (for accounting correctness)

```kotlin
// src/main/kotlin/com/yourapp/wallet/job/CashbackExpiryJob.kt
@Component
class CashbackExpiryJob(
    private val walletCreditRepository: WalletCreditRepository,
    private val walletService: WalletService,
    private val walletRepository: WalletRepository
) {

    private val log = LoggerFactory.getLogger(CashbackExpiryJob::class.java)

    /**
     * Runs every hour to expire cashback that has passed its expiry date.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    fun expireStaleCashback() {
        val now = Instant.now()

        // Find all unexpired credits that have passed their expiry time
        val expiredCredits = walletCreditRepository.findExpiredCredits(now)

        log.info("Processing {} expired cashback credits", expiredCredits.size)

        expiredCredits.forEach { credit ->
            if (credit.remaining <= 0) {
                // Already fully spent — just mark as expired
                credit.expired = true
                walletCreditRepository.save(credit)
                return@forEach
            }

            // Write DEBIT entry to remove expired cashback from balance
            walletService.debitWallet(
                walletId = credit.walletId,
                amountInPaise = credit.remaining,
                referenceId = credit.id.toString(),
                referenceType = "CASHBACK_EXPIRY",
                description = "Cashback expired (original credit: ${credit.ledgerEntryId})",
                idempotencyKey = "expiry_${credit.id}"
            )

            credit.remaining = 0
            credit.expired = true
            walletCreditRepository.save(credit)

            log.info("Cashback expired - creditId={} walletId={} amount={}",
                credit.id, credit.walletId, credit.amount)
        }
    }
}
```

```sql
-- Query to find expired cashback credits
-- Used by CashbackExpiryJob
SELECT wc.*
FROM wallet_credits wc
WHERE wc.expired = FALSE
  AND wc.expires_at <= NOW()
  AND wc.remaining > 0
ORDER BY wc.expires_at ASC
LIMIT 1000;  -- Process in batches to avoid long-running transactions
```

---

## Audit Trail: Complete History

```sql
-- Complete audit trail for a wallet
SELECT 
    le.id,
    le.entry_type,
    le.amount / 100.0 AS amount_rupees,
    le.balance_after / 100.0 AS balance_after_rupees,
    le.reference_type,
    le.reference_id,
    le.description,
    le.created_by,
    le.created_at,
    le.metadata
FROM ledger_entries le
WHERE le.wallet_id = $1
ORDER BY le.created_at DESC;

-- Wallet health check: verify snapshot matches ledger computation
SELECT 
    w.id,
    w.cached_balance / 100.0 AS snapshot_balance_rupees,
    COALESCE(SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE -le.amount END), 0) / 100.0 AS computed_balance_rupees,
    ABS(
        w.cached_balance - 
        COALESCE(SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE -le.amount END), 0)
    ) AS discrepancy_paise
FROM wallets w
LEFT JOIN ledger_entries le ON le.wallet_id = w.id
GROUP BY w.id, w.cached_balance
HAVING ABS(
    w.cached_balance - 
    COALESCE(SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE -le.amount END), 0)
) > 0;  -- Returns rows only where snapshot differs from computed balance
```

```kotlin
// src/main/kotlin/com/yourapp/wallet/service/AuditService.kt
@Service
class AuditService(
    private val walletRepository: WalletRepository,
    private val ledgerRepository: LedgerRepository,
    private val alertService: AlertService
) {

    /**
     * Validates that ALL wallet snapshots match their computed ledger balances.
     * Run this daily as a reconciliation job.
     * Any discrepancy indicates a bug or data corruption.
     */
    @Scheduled(cron = "0 30 2 * * *")  // 2:30 AM daily
    fun reconcileAllWallets() {
        val discrepancies = ledgerRepository.findWalletBalanceDiscrepancies()

        if (discrepancies.isNotEmpty()) {
            log.error("WALLET RECONCILIATION FAILED: {} wallets have balance discrepancies",
                discrepancies.size)

            discrepancies.forEach { discrepancy ->
                log.error("Wallet balance mismatch - walletId={} snapshotBalance={} computedBalance={}",
                    discrepancy.walletId,
                    discrepancy.snapshotBalance,
                    discrepancy.computedBalance)
            }

            alertService.sendCriticalAlert(
                "CRITICAL: ${discrepancies.size} wallet balance discrepancies found. " +
                "Immediate investigation required."
            )
        }
    }
}
```

---

## Negative Balance Prevention

```kotlin
// In WalletService.debitWallet() — happens inside the pessimistic lock

val currentBalance = ledgerRepository.computeBalance(walletId)

// This check is inside the transaction, after acquiring the lock
// No other thread can modify the balance between this check and the debit write
if (currentBalance < amountInPaise) {
    throw InsufficientBalanceException(
        message = "Insufficient wallet balance",
        availableBalance = currentBalance,
        requiredAmount = amountInPaise
    )
}

// Invariant: after this point, currentBalance >= amountInPaise
// And no other transaction can change this (we hold the lock)
```

> [!NOTE]
> In PostgreSQL, you can also add a database-level constraint to prevent negative balances, but it's complex with a ledger-based system (the constraint would need to check the sum of all entries). The application-level check inside a locked transaction is the correct and simpler approach.

---

## Wallet Statement API

```kotlin
// src/main/kotlin/com/yourapp/wallet/controller/WalletController.kt
@RestController
@RequestMapping("/api/v1/wallets")
class WalletController(
    private val walletService: WalletService,
    private val ledgerRepository: LedgerRepository
) {

    @GetMapping("/balance")
    fun getBalance(@RequestHeader("X-User-Id") userId: String): ResponseEntity<WalletBalanceResponse> {
        val wallet = walletService.findWalletByUserId(userId)
            ?: return ResponseEntity.notFound().build()

        val balance = walletService.getDisplayBalance(wallet.id!!)

        return ResponseEntity.ok(WalletBalanceResponse(
            balancePaise = balance,
            balanceRupees = balance / 100.0,
            currency = wallet.currency,
            walletId = wallet.id.toString()
        ))
    }

    @GetMapping("/statement")
    fun getStatement(
        @RequestHeader("X-User-Id") userId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) fromDate: String?,
        @RequestParam(required = false) toDate: String?
    ): ResponseEntity<WalletStatementResponse> {
        val wallet = walletService.findWalletByUserId(userId)
            ?: return ResponseEntity.notFound().build()

        val pageable = org.springframework.data.domain.PageRequest.of(
            page, size.coerceIn(1, 100)  // Max 100 entries per page
        )

        val entries = ledgerRepository.findByWalletIdOrderByCreatedAtDesc(wallet.id!!, pageable)

        return ResponseEntity.ok(WalletStatementResponse(
            entries = entries.content.map { it.toDto() },
            totalEntries = entries.totalElements,
            page = page,
            size = size
        ))
    }
}

data class WalletBalanceResponse(
    val balancePaise: Long,
    val balanceRupees: Double,
    val currency: String,
    val walletId: String
)

data class LedgerEntryDto(
    val id: String,
    val type: String,           // "CREDIT" or "DEBIT"
    val amountRupees: Double,
    val balanceAfterRupees: Double,
    val description: String?,
    val referenceType: String,
    val createdAt: String
)
```

---

## Production Configuration

```yaml
# application.yml — Transaction and connection pool tuning for wallet operations
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 10000        # 10s max wait for connection (fail fast)
      idle-timeout: 600000             # 10 min idle before closing
      max-lifetime: 1800000            # 30 min max connection age
      leak-detection-threshold: 30000  # Alert if connection held >30s (transaction leak detection)
  
  jpa:
    properties:
      hibernate:
        # Lock timeout: if we can't acquire pessimistic lock in 5s, fail
        # Prevents deadlock situations from blocking indefinitely
        javax.persistence.lock.timeout: 5000
        # Statement timeout: no single query should run >30s
        jdbc.batch_size: 50
        order_inserts: true
        order_updates: true

  transaction:
    default-timeout: 30  # Seconds — any transaction taking >30s is rolled back automatically
```

> [!WARNING]
> **Lock timeout configuration is critical**. If you have pessimistic locks without a timeout, a bug can cause a transaction to hold a lock indefinitely. All subsequent transactions on that wallet will block forever, cascading into your entire connection pool being exhausted. Set `javax.persistence.lock.timeout` to a sensible value (5-10 seconds). If you can't acquire a lock in 5 seconds, something is seriously wrong and you should fail fast rather than queue up 200 waiting threads.

---

## Complete Spring Boot Application: Wiring It Together

```kotlin
// src/main/kotlin/com/yourapp/wallet/WalletApplication.kt
@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
class WalletApplication

fun main(args: Array<String>) {
    runApplication<WalletApplication>(*args)
}

// Configuration class
@Configuration
class WalletConfig {

    @Bean
    fun transactionTemplate(transactionManager: PlatformTransactionManager): TransactionTemplate {
        return TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRED
            isolationLevel = TransactionDefinition.ISOLATION_READ_COMMITTED
            timeout = 30
        }
    }
}
```

---

## Reconciliation: Wallet Balance Snapshot vs Ledger

```sql
-- Reconciliation query: run daily
-- Finds wallets where cached_balance != sum of ledger entries

WITH ledger_computed AS (
    SELECT 
        wallet_id,
        COALESCE(SUM(CASE 
            WHEN entry_type = 'CREDIT' THEN amount 
            ELSE -amount 
        END), 0) AS true_balance
    FROM ledger_entries
    GROUP BY wallet_id
)
SELECT 
    w.id AS wallet_id,
    w.user_id,
    w.cached_balance AS snapshot_balance,
    lc.true_balance AS ledger_balance,
    (w.cached_balance - lc.true_balance) AS discrepancy_paise,
    (w.cached_balance - lc.true_balance) / 100.0 AS discrepancy_rupees
FROM wallets w
JOIN ledger_computed lc ON lc.wallet_id = w.id
WHERE w.cached_balance != lc.true_balance
ORDER BY ABS(w.cached_balance - lc.true_balance) DESC;
```

```kotlin
// Scheduled reconciliation — also validates no wallet has negative balance
@Scheduled(cron = "0 0 3 * * *")  // 3 AM daily
@Transactional(readOnly = true)
fun validateWalletIntegrity() {
    // 1. Find balance discrepancies
    val discrepancies = jdbcTemplate.queryForList("""
        WITH ledger_computed AS (
            SELECT wallet_id,
                   COALESCE(SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE -amount END), 0) AS true_balance
            FROM ledger_entries GROUP BY wallet_id
        )
        SELECT w.id, w.user_id, w.cached_balance, lc.true_balance
        FROM wallets w JOIN ledger_computed lc ON lc.wallet_id = w.id
        WHERE w.cached_balance != lc.true_balance
    """)

    // 2. Find wallets with negative computed balance (data corruption indicator)
    val negativeBalances = jdbcTemplate.queryForList("""
        SELECT wallet_id, SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE -amount END) AS balance
        FROM ledger_entries
        GROUP BY wallet_id
        HAVING SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE -amount END) < 0
    """)

    if (discrepancies.isNotEmpty() || negativeBalances.isNotEmpty()) {
        alertService.sendCriticalAlert(
            "Wallet integrity check failed: ${discrepancies.size} discrepancies, " +
            "${negativeBalances.size} negative balances. IMMEDIATE ACTION REQUIRED."
        )
    }
}
```

---

## Key Design Decisions Summary

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Balance storage | Computed from ledger (with snapshot cache) | Immutable, auditable, no data loss |
| Concurrency control | Pessimistic locking (SELECT FOR UPDATE) | Guaranteed consistency, no retries needed |
| Amount unit | Paise (smallest unit, Long) | No floating point precision errors |
| Idempotency | Per-operation idempotency key | Safe retries don't double-debit |
| Atomicity | @Transactional wrapping lock + balance check + ledger write | No partial state possible |
| Audit | Every entry immutable, timestamps, created_by | Full traceability |
| Expiry | WalletCredit tracking table + debit ledger entry on expiry | Double-entry stays balanced |
| Reconciliation | Daily job: snapshot vs computed balance | Catch bugs before they compound |

---

## Key Takeaways

1. **Ledger is immutable** — append-only, never UPDATE or DELETE rows
2. **Balance is computed, not stored** — the ledger IS the truth; the snapshot is a cache
3. **Pessimistic locking inside the balance check + debit write** — must happen atomically in one transaction
4. **All amounts in paise (Long)** — floating point is forbidden in financial systems
5. **Idempotency key on every debit/credit** — retries must be safe
6. **Short transactions** — acquire lock, check balance, write entry, commit, then do external work
7. **Daily reconciliation** — catch snapshot drift and data corruption before it becomes a crisis
8. **Multiple wallet types** with priority ordering for payment
9. **Cashback expiry** = debit ledger entry (double-entry stays balanced)
10. **Audit trail is a business requirement** — every rupee movement must be traceable to a person, a time, and a reason

> [!IMPORTANT]
> Every digital-first company that handles money at scale eventually builds a ledger system. Stripe's internal ledger system, called "Stripe Balance", is what powers their entire financial model. PhonePe, Google Pay, and Paytm all run on double-entry ledgers internally. This is not over-engineering — it's the minimal correct architecture for handling money. Building a mutable balance column is the over-simplification that you'll pay for with data loss and audit failures.
