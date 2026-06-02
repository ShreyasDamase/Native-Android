# P0-13 — Inventory Architecture: Stock Reservation, Overselling Prevention & Concurrency

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> **Context**: This is not a tutorial on "how inventory works." This is a war manual for preventing the single most catastrophic failure mode in e-commerce — selling a product you don't have. Zepto, Blinkit, and Instamart deal with this at millions of orders/day. Every design decision here has a real production failure story behind it.

---

## The Overselling Nightmare — What Actually Happens

Imagine you have 1 unit of iPhone 15 Pro left. Ten users simultaneously hit the checkout button. Without proper locking, here's what the database sees:

```
Thread 1: SELECT stock FROM products WHERE id = 42; → returns 1
Thread 2: SELECT stock FROM products WHERE id = 42; → returns 1
Thread 3: SELECT stock FROM products WHERE id = 42; → returns 1
... (all 10 threads read 1)

Thread 1: UPDATE products SET stock = stock - 1 WHERE id = 42; → stock = 0
Thread 2: UPDATE products SET stock = stock - 1 WHERE id = 42; → stock = -1
Thread 3: UPDATE products SET stock = stock - 1 WHERE id = 42; → stock = -2
... (you've sold -9 units)
```

This is a **classic Read-Modify-Write race condition**. The check and the update are two separate operations with a gap between them. At scale, that gap is a canyon.

**Real-world consequences:**
- Refunding customers creates CS overhead and damages trust
- Chargebacks from customers who never got their product
- Legal liability in regulated categories (medicines, alcohol in quick commerce)
- Inventory reconciliation nightmares — your books show negative stock which breaks reorder logic

> [!CAUTION]
> At Zepto/Blinkit scale (100k+ orders/day), even a 0.01% oversell rate means 10 oversells per day. With high-value items, that's ₹50,000+ in daily losses from refunds alone. This compounds into supplier relationship damage and cash flow problems.

---

## Inventory State Machine

Before writing a single line of code, understand that inventory is NOT a simple counter. It's a **state machine**.

```
AVAILABLE ──────────────────────────────────────────── RESERVED
    │                                                       │
    │   (user adds to cart, starts checkout)                │
    │ ─────────────────────────────────────────────────► │
    │                                                       │
    │                                             (payment success)
    │                                                       │
    │                                                   COMMITTED
    │                                                       │
    │   (timeout / cancellation / payment fail)             │
    │◄──────────────────────────────────────────────────── │
    │                                                       │
    │                                              (return/refund)
AVAILABLE ◄──────────────────────────────────────── RETURNED
```

### State Definitions

| State | Meaning | DB Representation |
|-------|---------|-------------------|
| `AVAILABLE` | Can be sold | `stock_available > 0` |
| `RESERVED` | Soft-locked during checkout, not yet paid | Separate `reservations` table row |
| `COMMITTED` | Payment confirmed, stock permanently decremented | `stock_available` decremented, `stock_committed` incremented |
| `RETURNED` | Customer returned item, back in saleable stock | Increment `stock_available` again |
| `DAMAGED` | Item inspected after return and deemed unsellable | Neither available nor committable |

> [!IMPORTANT]
> The key insight: `RESERVED` inventory is NOT subtracted from `stock_available` immediately. It is tracked in a separate reservations table. Only `COMMITTED` inventory is permanently deducted. This two-phase approach gives you the ability to release reservations without corrupting your inventory count.

---

## SQL Schema — The Foundation

```sql
-- Products table with inventory counters
CREATE TABLE products (
    id                  BIGSERIAL PRIMARY KEY,
    sku                 VARCHAR(50) UNIQUE NOT NULL,
    name                VARCHAR(255) NOT NULL,
    
    -- Inventory counters — these must ALWAYS be consistent
    stock_available     INTEGER NOT NULL DEFAULT 0,      -- can be sold right now
    stock_reserved      INTEGER NOT NULL DEFAULT 0,      -- soft-locked in active checkouts
    stock_committed     INTEGER NOT NULL DEFAULT 0,      -- paid and fulfilled
    stock_safety_buffer INTEGER NOT NULL DEFAULT 0,      -- never sell below this
    
    -- Constraints to prevent negative stock
    CONSTRAINT check_stock_available CHECK (stock_available >= 0),
    CONSTRAINT check_stock_reserved CHECK (stock_reserved >= 0),
    CONSTRAINT check_stock_committed CHECK (stock_committed >= 0),
    CONSTRAINT check_stock_safety_buffer CHECK (stock_safety_buffer >= 0),
    
    version             BIGINT NOT NULL DEFAULT 0,       -- optimistic lock version
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index for fast SKU lookups
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_low_stock ON products(stock_available) WHERE stock_available < 10;

-- Reservations table — tracks every soft lock
CREATE TABLE inventory_reservations (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL REFERENCES products(id),
    order_id        VARCHAR(50) NOT NULL,
    user_id         BIGINT NOT NULL,
    quantity        INTEGER NOT NULL CHECK (quantity > 0),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, COMMITTED, RELEASED, EXPIRED
    
    -- Expiry: if not committed within window, auto-release
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    committed_at    TIMESTAMP WITH TIME ZONE,
    released_at     TIMESTAMP WITH TIME ZONE,
    
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT check_status CHECK (status IN ('ACTIVE', 'COMMITTED', 'RELEASED', 'EXPIRED'))
);

-- Critical index: find expired reservations efficiently
CREATE INDEX idx_reservations_expires_at ON inventory_reservations(expires_at) 
    WHERE status = 'ACTIVE';

-- Index for order-based lookups (cancel order → release reservation)
CREATE INDEX idx_reservations_order_id ON inventory_reservations(order_id);
CREATE INDEX idx_reservations_product_status ON inventory_reservations(product_id, status);

-- Audit log — never lose inventory history
CREATE TABLE inventory_events (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL,
    event_type      VARCHAR(30) NOT NULL,  -- RESERVE, COMMIT, RELEASE, RESTOCK, ADJUSTMENT
    quantity_delta  INTEGER NOT NULL,      -- positive = add, negative = subtract
    reference_id    VARCHAR(100),          -- order_id, restock_batch_id, etc.
    actor           VARCHAR(100),          -- who/what caused this event
    notes           TEXT,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_inventory_events_product ON inventory_events(product_id, created_at DESC);
```

> [!NOTE]
> The `stock_safety_buffer` column is critical for quick commerce. Even if `stock_available = 5`, if `stock_safety_buffer = 5`, you should not sell any. This buffer absorbs: items being picked and discovered damaged, returned items pending quality check, discrepancies between digital and physical count.

---

## Two-Phase Inventory: RESERVE → COMMIT → RELEASE

This is the **most important architectural decision** in inventory management.

### Phase 1: RESERVE (Soft Lock)

When a user starts checkout (or adds to cart in quick commerce with aggressive reservation):

1. Check `stock_available - stock_reserved > quantity_requested` (respecting safety buffer)
2. Atomically increment `stock_reserved` on the product
3. Insert a row into `inventory_reservations` with `expires_at = NOW() + 10 minutes`

> [!WARNING]
> In quick commerce (Blinkit/Zepto), reservation happens at **add-to-cart** time, not checkout. The 10-minute delivery promise requires knowing if stock is truly available before the customer even confirms. This means reservation windows are shorter (2-5 minutes) and expiry is more aggressive.

### Phase 2: COMMIT (On Payment Success)

When payment gateway confirms success:

1. Find the `ACTIVE` reservation for this order
2. Set reservation status = `COMMITTED`
3. Decrement `stock_available`, decrement `stock_reserved`, increment `stock_committed`
4. All three updates must happen in one transaction

### Phase 3: RELEASE (On Timeout or Cancellation)

When reservation expires or user cancels:

1. Find the `ACTIVE` reservation
2. Set status = `RELEASED` or `EXPIRED`
3. Decrement `stock_reserved` — the item goes back to being fully available

```
INVARIANT that must ALWAYS hold:
stock_available + stock_reserved + stock_committed + stock_damaged = total_ever_received
```

---

## Kotlin Entity with @Version (Optimistic Locking)

```kotlin
// build.gradle.kts dependencies
// implementation("org.springframework.boot:spring-boot-starter-data-jpa")
// implementation("org.springframework.boot:spring-boot-starter-web")
// implementation("org.springframework.boot:spring-boot-starter-data-redis")
// runtimeOnly("org.postgresql:postgresql")

package com.yourcompany.inventory.domain

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "products",
    indexes = [
        Index(name = "idx_products_sku", columnList = "sku", unique = true)
    ]
)
data class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 50)
    val sku: String,

    @Column(nullable = false)
    val name: String,

    @Column(name = "stock_available", nullable = false)
    var stockAvailable: Int = 0,

    @Column(name = "stock_reserved", nullable = false)
    var stockReserved: Int = 0,

    @Column(name = "stock_committed", nullable = false)
    var stockCommitted: Int = 0,

    @Column(name = "stock_safety_buffer", nullable = false)
    val stockSafetyBuffer: Int = 0,

    /**
     * @Version is the cornerstone of optimistic locking in JPA.
     *
     * How it works:
     * - JPA automatically increments this field on every UPDATE
     * - When two transactions try to update the same row, the second one sees
     *   the version has changed and throws OptimisticLockException
     * - The UPDATE SQL becomes:
     *   UPDATE products SET stock_available = ?, version = version + 1
     *   WHERE id = ? AND version = ?  ← the version check
     *
     * If the WHERE clause matches 0 rows, JPA knows someone else updated first.
     */
    @Version
    var version: Long = 0,

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),

    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now()
) {
    /**
     * How many units can actually be sold right now.
     * This accounts for the safety buffer.
     */
    fun effectivelyAvailable(): Int =
        (stockAvailable - stockSafetyBuffer).coerceAtLeast(0)

    fun canReserve(quantity: Int): Boolean =
        effectivelyAvailable() >= quantity

    /**
     * Validates the fundamental inventory invariant.
     * Call this in tests and monitoring checks.
     */
    fun isConsistent(totalReceivedFromPO: Int): Boolean =
        stockAvailable + stockReserved + stockCommitted <= totalReceivedFromPO
}

@Entity
@Table(
    name = "inventory_reservations",
    indexes = [
        Index(name = "idx_res_order", columnList = "order_id"),
        Index(name = "idx_res_expires", columnList = "expires_at")
    ]
)
data class InventoryReservation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,

    @Column(name = "order_id", nullable = false, length = 50)
    val orderId: String,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val quantity: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ReservationStatus = ReservationStatus.ACTIVE,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "committed_at")
    var committedAt: Instant? = null,

    @Column(name = "released_at")
    var releasedAt: Instant? = null,

    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now()
)

enum class ReservationStatus {
    ACTIVE,
    COMMITTED,
    RELEASED,
    EXPIRED
}
```

---

## SELECT FOR UPDATE — Pessimistic Locking

Optimistic locking is great under low contention. But when you have flash sales (100 users fighting for 5 units), optimistic locking generates massive retry storms. Under high contention, use **pessimistic locking**.

```kotlin
package com.yourcompany.inventory.repository

import com.yourcompany.inventory.domain.InventoryReservation
import com.yourcompany.inventory.domain.Product
import com.yourcompany.inventory.domain.ReservationStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface ProductRepository : JpaRepository<Product, Long> {

    /**
     * SELECT FOR UPDATE — acquires an exclusive row-level lock.
     *
     * This SQL translates to:
     *   SELECT * FROM products WHERE id = ? FOR UPDATE
     *
     * The database guarantees that only ONE transaction holds this lock at a time.
     * All other transactions trying to lock the same row will WAIT (block)
     * until the current transaction commits or rolls back.
     *
     * When to use: flash sales, limited-quantity items, any scenario where
     * multiple concurrent writes to the same product are expected.
     *
     * Performance cost: lock acquisition overhead (~1-5ms per lock),
     * but prevents the much more expensive retry storms of optimistic locking.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :productId")
    fun findByIdWithLock(@Param("productId") productId: Long): Product?

    /**
     * SKIP LOCKED — the secret weapon for queue-based reservation processing.
     *
     * If you have 100 products being reserved simultaneously:
     * - Normal FOR UPDATE: all workers queue up waiting for the same locked row
     * - FOR UPDATE SKIP LOCKED: workers skip already-locked rows and process
     *   unlocked ones immediately. No blocking, maximum throughput.
     *
     * This is how you build a high-throughput reservation queue.
     * The SQL equivalent is: SELECT * FROM products WHERE id = ? FOR UPDATE SKIP LOCKED
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :productId", 
           hints = [jakarta.persistence.QueryHint(
               name = "jakarta.persistence.lock.timeout",
               value = "-2"  // -2 = SKIP_LOCKED in Hibernate
           )])
    fun findByIdWithSkipLock(@Param("productId") productId: Long): Product?

    fun findBySku(sku: String): Product?

    @Query("SELECT p FROM Product p WHERE p.stockAvailable <= p.stockSafetyBuffer + :threshold")
    fun findLowStockProducts(@Param("threshold") threshold: Int): List<Product>
}

interface ReservationRepository : JpaRepository<InventoryReservation, Long> {

    fun findByOrderIdAndStatus(orderId: String, status: ReservationStatus): List<InventoryReservation>

    /**
     * Find all expired active reservations for the cleanup scheduler.
     * This query runs every minute via @Scheduled.
     */
    @Query("""
        SELECT r FROM InventoryReservation r 
        WHERE r.status = 'ACTIVE' 
        AND r.expiresAt < :now
    """)
    fun findExpiredReservations(@Param("now") now: Instant): List<InventoryReservation>

    @Query("""
        SELECT r FROM InventoryReservation r 
        WHERE r.orderId = :orderId 
        AND r.status = 'ACTIVE'
    """)
    fun findActiveByOrderId(@Param("orderId") orderId: String): List<InventoryReservation>
}
```

> [!WARNING]
> **NEVER** use `@Lock(LockModeType.PESSIMISTIC_WRITE)` without a query timeout. If your application freezes and never commits, the lock will hold until the DB connection drops. Always configure `spring.datasource.hikari.connection-timeout` and set a statement-level lock timeout:
> ```sql
> SET LOCAL lock_timeout = '5s';
> ```

---

## The Complete Inventory Service

```kotlin
package com.yourcompany.inventory.service

import com.yourcompany.inventory.domain.*
import com.yourcompany.inventory.repository.*
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class InventoryService(
    private val productRepository: ProductRepository,
    private val reservationRepository: ReservationRepository,
    private val redisInventoryCache: RedisInventoryCache,
    private val inventoryEventPublisher: InventoryEventPublisher
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val RESERVATION_TTL_MINUTES = 10L
        const val MAX_RETRY_ATTEMPTS = 3
    }

    /**
     * RESERVE: Phase 1 of two-phase inventory.
     *
     * Strategy: Optimistic locking by default. If OptimisticLockException is thrown
     * (another transaction modified the row between our read and write),
     * Spring Retry automatically retries with exponential backoff.
     *
     * For flash sales, switch to pessimistic: call reserveWithPessimisticLock() instead.
     *
     * @Transactional(isolation = REPEATABLE_READ): Ensures that within this
     * transaction, we always read the same version of a row. Prevents phantom reads.
     * PostgreSQL's default READ COMMITTED would let us read updated rows mid-transaction.
     */
    @Retryable(
        value = [ObjectOptimisticLockingFailureException::class],
        maxAttempts = MAX_RETRY_ATTEMPTS,
        backoff = Backoff(delay = 100, multiplier = 2.0, random = true)
    )
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    fun reserveStock(
        productId: Long,
        orderId: String,
        userId: Long,
        quantity: Int
    ): ReservationResult {
        val product = productRepository.findByIdOrNull(productId)
            ?: return ReservationResult.Failure("Product not found: $productId")

        // Check Redis fast-path first (before hitting PostgreSQL)
        val redisAvailable = redisInventoryCache.getAvailableStock(productId)
        if (redisAvailable != null && redisAvailable < quantity) {
            log.debug("Redis fast-path: insufficient stock for product $productId")
            return ReservationResult.Failure("Insufficient stock (fast-path check)")
        }

        // Now check actual DB state with locking
        if (!product.canReserve(quantity)) {
            return ReservationResult.Failure(
                "Insufficient stock. Available: ${product.effectivelyAvailable()}, Requested: $quantity"
            )
        }

        // Increment reserved counter
        product.stockReserved += quantity
        product.updatedAt = Instant.now()
        // @Version field is auto-incremented by JPA on save.
        // If two threads reach here simultaneously with the same version,
        // the second save will throw ObjectOptimisticLockingFailureException
        productRepository.save(product)

        // Create reservation record
        val reservation = InventoryReservation(
            product = product,
            orderId = orderId,
            userId = userId,
            quantity = quantity,
            expiresAt = Instant.now().plus(RESERVATION_TTL_MINUTES, ChronoUnit.MINUTES)
        )
        reservationRepository.save(reservation)

        // Update Redis cache (decrement for fast future checks)
        redisInventoryCache.decrementAvailable(productId, quantity)

        log.info("Reserved $quantity units of product $productId for order $orderId")
        return ReservationResult.Success(reservation.id)
    }

    /**
     * RESERVE with pessimistic locking — use during flash sales.
     *
     * This acquires a row-level exclusive lock via SELECT FOR UPDATE.
     * All concurrent reservation requests for this product will queue up
     * in the database and be processed serially.
     *
     * Trade-off: Slower throughput (no concurrency) but zero retry storms
     * and guaranteed correctness.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun reserveStockPessimistic(
        productId: Long,
        orderId: String,
        userId: Long,
        quantity: Int
    ): ReservationResult {
        // This acquires the lock. Other transactions calling this for the same
        // productId will block here until we commit.
        val product = productRepository.findByIdWithLock(productId)
            ?: return ReservationResult.Failure("Product not found: $productId")

        if (!product.canReserve(quantity)) {
            return ReservationResult.Failure("Insufficient stock: ${product.effectivelyAvailable()}")
        }

        product.stockReserved += quantity
        product.updatedAt = Instant.now()
        productRepository.save(product)

        val reservation = InventoryReservation(
            product = product,
            orderId = orderId,
            userId = userId,
            quantity = quantity,
            expiresAt = Instant.now().plus(RESERVATION_TTL_MINUTES, ChronoUnit.MINUTES)
        )
        reservationRepository.save(reservation)

        redisInventoryCache.decrementAvailable(productId, quantity)

        return ReservationResult.Success(reservation.id)
    }

    /**
     * COMMIT: Phase 2 — payment succeeded, permanently deduct stock.
     *
     * This is called by the payment webhook handler.
     * Must be idempotent — if called twice for the same orderId, should not
     * double-commit. Check reservation status before processing.
     */
    @Transactional
    fun commitReservation(orderId: String): CommitResult {
        val reservations = reservationRepository.findByOrderIdAndStatus(
            orderId, ReservationStatus.ACTIVE
        )

        if (reservations.isEmpty()) {
            // Check if already committed (idempotency)
            val committed = reservationRepository.findByOrderIdAndStatus(
                orderId, ReservationStatus.COMMITTED
            )
            if (committed.isNotEmpty()) {
                log.warn("Reservation for order $orderId already committed — idempotent OK")
                return CommitResult.AlreadyCommitted
            }
            return CommitResult.Failure("No active reservation found for order: $orderId")
        }

        reservations.forEach { reservation ->
            val product = productRepository.findByIdWithLock(reservation.product.id)
                ?: throw IllegalStateException("Product disappeared during commit: ${reservation.product.id}")

            // Atomic: deduct from available, deduct from reserved, add to committed
            product.stockAvailable -= reservation.quantity
            product.stockReserved -= reservation.quantity
            product.stockCommitted += reservation.quantity
            product.updatedAt = Instant.now()

            // Guard: should never go negative, but defense in depth
            check(product.stockAvailable >= 0) {
                "CRITICAL: stock_available went negative for product ${product.id}"
            }
            check(product.stockReserved >= 0) {
                "CRITICAL: stock_reserved went negative for product ${product.id}"
            }

            productRepository.save(product)

            reservation.status = ReservationStatus.COMMITTED
            reservation.committedAt = Instant.now()
            reservationRepository.save(reservation)

            // Publish event for analytics, reorder triggers, etc.
            inventoryEventPublisher.publishCommit(
                productId = product.id,
                orderId = orderId,
                quantity = reservation.quantity,
                remainingStock = product.stockAvailable
            )

            // Trigger low-stock alert if needed
            if (product.stockAvailable <= product.stockSafetyBuffer + 5) {
                inventoryEventPublisher.publishLowStockAlert(product)
            }
        }

        log.info("Committed reservation for order $orderId")
        return CommitResult.Success
    }

    /**
     * RELEASE: Called on cancellation, payment failure, or timeout.
     *
     * Returns reserved stock back to available.
     * Must be idempotent — safe to call multiple times.
     */
    @Transactional
    fun releaseReservation(orderId: String, reason: ReleaseReason): ReleaseResult {
        val reservations = reservationRepository.findActiveByOrderId(orderId)

        if (reservations.isEmpty()) {
            log.debug("No active reservations for order $orderId — possibly already released")
            return ReleaseResult.NothingToRelease
        }

        reservations.forEach { reservation ->
            val product = productRepository.findByIdWithLock(reservation.product.id)
                ?: throw IllegalStateException("Product not found: ${reservation.product.id}")

            product.stockReserved -= reservation.quantity
            product.updatedAt = Instant.now()
            productRepository.save(product)

            reservation.status = when (reason) {
                ReleaseReason.TIMEOUT -> ReservationStatus.EXPIRED
                else -> ReservationStatus.RELEASED
            }
            reservation.releasedAt = Instant.now()
            reservationRepository.save(reservation)

            // Restore Redis cache
            redisInventoryCache.incrementAvailable(reservation.product.id, reservation.quantity)

            log.info("Released ${reservation.quantity} units of product ${reservation.product.id} " +
                    "for order $orderId. Reason: $reason")
        }

        return ReleaseResult.Success
    }

    /**
     * EXPIRY SCHEDULER: Runs every 60 seconds, finds expired reservations
     * and releases them back to available stock.
     *
     * This is critical — without this, a user who abandons checkout during
     * a flash sale would hold inventory hostage for infinity.
     *
     * Production consideration: with multiple Spring Boot instances,
     * multiple schedulers will run simultaneously. Use @SchedulerLock
     * from ShedLock library to ensure only ONE instance runs this at a time.
     */
    @Scheduled(fixedDelay = 60_000)  // every 60 seconds
    @Transactional
    fun expireStaleReservations() {
        val expired = reservationRepository.findExpiredReservations(Instant.now())

        if (expired.isEmpty()) return

        log.info("Found ${expired.size} expired reservations to release")

        // Group by order to release in bulk
        expired.groupBy { it.orderId }.forEach { (orderId, _) ->
            try {
                releaseReservation(orderId, ReleaseReason.TIMEOUT)
            } catch (e: Exception) {
                log.error("Failed to expire reservation for order $orderId", e)
                // Don't rethrow — process the rest
            }
        }
    }
}

// Result types — avoid throwing exceptions for expected failure cases
sealed class ReservationResult {
    data class Success(val reservationId: Long) : ReservationResult()
    data class Failure(val reason: String) : ReservationResult()
}

sealed class CommitResult {
    object Success : CommitResult()
    object AlreadyCommitted : CommitResult()
    data class Failure(val reason: String) : CommitResult()
}

sealed class ReleaseResult {
    object Success : ReleaseResult()
    object NothingToRelease : ReleaseResult()
}

enum class ReleaseReason {
    USER_CANCELLED,
    PAYMENT_FAILED,
    TIMEOUT,
    ADMIN_OVERRIDE
}
```

---

## Redis Atomic Decrement — Fast Path Before PostgreSQL

Redis is 10-100x faster than PostgreSQL for simple counter operations. Use Redis as a **pre-filter** to reject obviously-insufficient-stock requests before they hit your database.

```kotlin
package com.yourcompany.inventory.service

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisInventoryCache(
    private val redisTemplate: RedisTemplate<String, String>
) {
    companion object {
        private const val KEY_PREFIX = "inv:available:"
        private val TTL = Duration.ofMinutes(30)
    }

    private fun key(productId: Long) = "$KEY_PREFIX$productId"

    /**
     * Redis DECR is atomic — guaranteed no race conditions at the Redis level.
     *
     * DECR atomically decrements by 1. DECRBY decrements by N.
     * Both are O(1) operations that return the new value.
     *
     * IMPORTANT: This is a pre-filter, NOT the source of truth.
     * The actual reservation still goes through PostgreSQL with proper locking.
     * Redis can have stale data — we only use it to fast-reject obviously
     * insufficient stock, saving DB load.
     *
     * If Redis says "stock = 0" → reject immediately (save DB round-trip)
     * If Redis says "stock = 5" → still need to verify with DB (might be stale)
     */
    fun decrementAvailable(productId: Long, quantity: Int): Long? {
        val ops = redisTemplate.opsForValue()
        return ops.decrement(key(productId), quantity.toLong())
    }

    fun incrementAvailable(productId: Long, quantity: Int): Long? {
        val ops = redisTemplate.opsForValue()
        return ops.increment(key(productId), quantity.toLong())
    }

    fun getAvailableStock(productId: Long): Long? {
        return redisTemplate.opsForValue().get(key(productId))?.toLongOrNull()
    }

    /**
     * Sync Redis from DB — call this on startup and periodically to fix drift.
     *
     * Redis is an in-memory cache. If it crashes and restarts, it loses all data.
     * You need to re-seed from PostgreSQL.
     */
    fun syncFromDatabase(productId: Long, availableStock: Int) {
        redisTemplate.opsForValue().set(
            key(productId),
            availableStock.toString(),
            TTL
        )
    }

    /**
     * Lua script for atomic check-and-decrement.
     *
     * Problem: What if stock is 2 and two requests each try to decrement by 2?
     * Two separate DECR calls could both succeed, leaving you at -2.
     *
     * Solution: Lua script runs atomically in Redis — no other commands execute
     * between the check and the decrement.
     */
    private val checkAndDecrementScript = """
        local current = tonumber(redis.call('GET', KEYS[1]))
        if current == nil then
            return -1  -- key doesn't exist, fallthrough to DB
        end
        if current < tonumber(ARGV[1]) then
            return -2  -- insufficient stock
        end
        return redis.call('DECRBY', KEYS[1], ARGV[1])
    """.trimIndent()

    fun atomicCheckAndDecrement(productId: Long, quantity: Int): AtomicDecrementResult {
        val result = redisTemplate.execute(
            org.springframework.data.redis.core.script.DefaultRedisScript<Long>().apply {
                setScriptText(checkAndDecrementScript)
                resultType = Long::class.java
            },
            listOf(key(productId)),
            quantity.toString()
        )

        return when (result) {
            null, -1L -> AtomicDecrementResult.CacheMiss        // Redis doesn't have it, check DB
            -2L -> AtomicDecrementResult.InsufficientStock       // Definitive: not enough
            else -> AtomicDecrementResult.Success(result)        // Decremented, new value returned
        }
    }
}

sealed class AtomicDecrementResult {
    object CacheMiss : AtomicDecrementResult()
    object InsufficientStock : AtomicDecrementResult()
    data class Success(val newStock: Long) : AtomicDecrementResult()
}
```

---

## Optimistic vs Pessimistic Locking — When to Use What

| Scenario | Recommended Approach | Reason |
|---------|---------------------|--------|
| Normal inventory (low contention) | Optimistic (`@Version`) | Zero lock overhead, high throughput |
| Flash sale (high contention, same SKU) | Pessimistic (`SELECT FOR UPDATE`) | Eliminates retry storms |
| Multiple SKUs in one order | Pessimistic with ordered lock acquisition | Prevents deadlocks |
| Distributed inventory across warehouses | Redis atomic + DB eventual | Minimize cross-region DB calls |
| Return processing | Pessimistic | Low throughput operation, data integrity critical |

> [!CAUTION]
> **Deadlock danger with pessimistic locking on multiple products**: If Thread A locks Product 1 then Product 2, while Thread B locks Product 2 then Product 1 — you have a deadlock. PostgreSQL will detect this and kill one transaction. Prevention: always acquire locks in the **same order** (e.g., always by ascending product ID).

```kotlin
// Safe multi-product locking — always sort by ID first
@Transactional
fun reserveMultipleProducts(
    items: List<ReservationItem>,
    orderId: String,
    userId: Long
): List<ReservationResult> {
    // CRITICAL: Sort by product ID to prevent deadlocks
    val sortedItems = items.sortedBy { it.productId }

    return sortedItems.map { item ->
        val product = productRepository.findByIdWithLock(item.productId)
            ?: return@map ReservationResult.Failure("Product not found: ${item.productId}")

        if (!product.canReserve(item.quantity)) {
            return@map ReservationResult.Failure("Insufficient stock for ${item.productId}")
        }

        product.stockReserved += item.quantity
        productRepository.save(product)

        val reservation = InventoryReservation(
            product = product,
            orderId = orderId,
            userId = userId,
            quantity = item.quantity,
            expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES)
        )
        reservationRepository.save(reservation)
        ReservationResult.Success(reservation.id)
    }
}

data class ReservationItem(val productId: Long, val quantity: Int)
```

---

## Distributed Inventory — Multiple Warehouses/Dark Stores

Zepto operates 300+ dark stores across India. Each dark store has its own inventory. When an order comes in, you need to:

1. Find the nearest dark store(s) with the required stock
2. Reserve from the correct store
3. Prevent over-allocation from a single store

### Allocation Algorithm

```kotlin
package com.yourcompany.inventory.service

import org.springframework.stereotype.Service

@Service
class DistributedInventoryAllocator(
    private val warehouseRepository: WarehouseRepository,
    private val inventoryService: InventoryService
) {
    /**
     * Allocation strategy: Nearest-first with fallback.
     *
     * 1. Get warehouses sorted by distance to delivery address
     * 2. Check if the nearest warehouse has all required items
     * 3. If yes: allocate entirely from that warehouse (single-pickup, fast)
     * 4. If no: check if split across warehouses is feasible
     *    - Split fulfillment means two separate pickers → slower delivery
     *    - Zepto/Blinkit generally avoid split for speed — they mark item
     *      as unavailable if nearest store doesn't have it
     *
     * For quick commerce: NEVER split across warehouses.
     * For standard e-commerce (Amazon): splitting is acceptable.
     */
    fun allocateOrder(
        items: List<ReservationItem>,
        deliveryLat: Double,
        deliveryLng: Double,
        allowSplitFulfillment: Boolean = false
    ): AllocationResult {
        val nearbyWarehouses = warehouseRepository.findNearbyWarehouses(
            lat = deliveryLat,
            lng = deliveryLng,
            radiusKm = 5.0
        )

        if (nearbyWarehouses.isEmpty()) {
            return AllocationResult.NoWarehouseInRange
        }

        // Try to fulfill from a single warehouse (preferred)
        for (warehouse in nearbyWarehouses) {
            val canFulfillAll = items.all { item ->
                val stock = warehouse.getAvailableStock(item.productId)
                stock >= item.quantity
            }

            if (canFulfillAll) {
                return AllocationResult.SingleWarehouse(
                    warehouseId = warehouse.id,
                    items = items
                )
            }
        }

        if (!allowSplitFulfillment) {
            return AllocationResult.InsufficientStock(
                "No single warehouse can fulfill the entire order"
            )
        }

        // Split fulfillment logic (standard e-commerce only)
        return allocateSplit(items, nearbyWarehouses)
    }

    private fun allocateSplit(
        items: List<ReservationItem>,
        warehouses: List<Warehouse>
    ): AllocationResult {
        val allocations = mutableMapOf<Long, MutableList<ReservationItem>>() // warehouseId → items

        for (item in items) {
            val warehouse = warehouses.firstOrNull { wh ->
                wh.getAvailableStock(item.productId) >= item.quantity
            } ?: return AllocationResult.InsufficientStock(
                "Cannot fulfill item ${item.productId} from any warehouse"
            )

            allocations.getOrPut(warehouse.id) { mutableListOf() }.add(item)
        }

        return AllocationResult.MultiWarehouse(
            allocations.map { (warehouseId, warehouseItems) ->
                WarehouseAllocation(warehouseId, warehouseItems)
            }
        )
    }
}

sealed class AllocationResult {
    object NoWarehouseInRange : AllocationResult()
    data class SingleWarehouse(val warehouseId: Long, val items: List<ReservationItem>) : AllocationResult()
    data class MultiWarehouse(val allocations: List<WarehouseAllocation>) : AllocationResult()
    data class InsufficientStock(val reason: String) : AllocationResult()
}

data class WarehouseAllocation(
    val warehouseId: Long,
    val items: List<ReservationItem>
)
```

---

## Safety Stock — Never Sell to Zero

Safety stock is the buffer between "sellable stock" and "physically present stock."

**Why you need it:**

| Reason | Impact Without Safety Stock |
|--------|----------------------------|
| Item damaged during picking | Promised customer gets cancelled order |
| Return pending quality check | Item in transit, not actually available |
| Physical count discrepancy | Digital says 1, physical says 0 |
| Supplier delivery variance | Expected restock delayed — buffer runs out |

### Safety Stock Calculation (Velocity-Based)

```kotlin
/**
 * Safety stock formula:
 * Safety Stock = Z × σ(demand) × √(Lead Time)
 *
 * Where:
 * Z = service level factor (1.65 for 95%, 2.05 for 98%)
 * σ(demand) = standard deviation of daily demand
 * Lead Time = reorder lead time in days
 *
 * For quick commerce with supplier delivering daily:
 * Lead Time ≈ 1 day
 * Safety Stock ≈ 2 × daily_demand_std_dev (for ~95% availability)
 */
fun calculateSafetyStock(
    avgDailyDemand: Double,
    stdDevDailyDemand: Double,
    leadTimeDays: Double,
    serviceLevelFactor: Double = 1.65  // 95% service level
): Int {
    val safetyStock = serviceLevelFactor * stdDevDailyDemand * Math.sqrt(leadTimeDays)
    return safetyStock.toInt().coerceAtLeast(1)  // at minimum, always keep 1
}

/**
 * Reorder point = average demand during lead time + safety stock
 * When stock_available drops to this level, trigger a purchase order.
 */
fun calculateReorderPoint(
    avgDailyDemand: Double,
    leadTimeDays: Double,
    safetyStock: Int
): Int {
    return (avgDailyDemand * leadTimeDays + safetyStock).toInt()
}
```

---

## ShedLock — Preventing Duplicate Scheduler Runs

When you deploy 3 instances of your Spring Boot app, each has its own `@Scheduled` expiry job. Without coordination, all 3 will try to expire and release the same reservations simultaneously, causing phantom releases.

```kotlin
// build.gradle.kts
// implementation("net.javacrumbs.shedlock:shedlock-spring:5.10.0")
// implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.10.0")

// Schema for ShedLock:
// CREATE TABLE shedlock (
//     name VARCHAR(64) NOT NULL PRIMARY KEY,
//     lock_until TIMESTAMP NOT NULL,
//     locked_at TIMESTAMP NOT NULL,
//     locked_by VARCHAR(255) NOT NULL
// );

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
class ShedLockConfig {
    @Bean
    fun lockProvider(dataSource: DataSource): LockProvider =
        JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(JdbcTemplate(dataSource))
                .usingDbTime()
                .build()
        )
}

// In the scheduler:
@Scheduled(fixedDelay = 60_000)
@SchedulerLock(
    name = "expireReservations",
    lockAtLeastFor = "PT50S",   // hold lock for at least 50 seconds
    lockAtMostFor = "PT5M"      // release lock after 5 minutes max (if instance dies)
)
fun expireStaleReservations() {
    // Only ONE instance runs this at a time
}
```

---

## Inventory Monitoring & Alerting

```kotlin
package com.yourcompany.inventory.monitoring

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Gauge
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class InventoryMetrics(
    private val productRepository: ProductRepository,
    private val reservationRepository: ReservationRepository,
    private val meterRegistry: MeterRegistry,
    private val alertService: AlertService
) {
    /**
     * Expose inventory metrics to Prometheus/Grafana.
     *
     * Dashboard alerts to set up:
     * - stock_available < safety_buffer → critical (product will be unavailable)
     * - active_reservations growing without commits → checkout abandonment spike
     * - commit_rate dropping → payment system issues
     * - reservation_expiry_rate spiking → checkout flow problems
     */
    @Scheduled(fixedDelay = 30_000)
    fun reportMetrics() {
        val lowStockProducts = productRepository.findLowStockProducts(threshold = 10)

        Gauge.builder("inventory.low_stock_product_count") { lowStockProducts.size.toDouble() }
            .description("Number of products at or near safety stock level")
            .register(meterRegistry)

        // Alert if too many products are low stock
        if (lowStockProducts.size > 50) {
            alertService.sendAlert(
                level = AlertLevel.WARNING,
                message = "${lowStockProducts.size} products at low stock — check reorder triggers"
            )
        }

        // Velocity-based reorder calculations
        lowStockProducts.forEach { product ->
            val soldLast7Days = calculateSalesVelocity(product.id, days = 7)
            val reorderPoint = calculateReorderPoint(
                avgDailyDemand = soldLast7Days / 7.0,
                leadTimeDays = 1.0,
                safetyStock = product.stockSafetyBuffer
            )

            if (product.stockAvailable <= reorderPoint) {
                alertService.sendRestockAlert(
                    productId = product.id,
                    sku = product.sku,
                    currentStock = product.stockAvailable,
                    suggestedOrderQty = (reorderPoint * 2).coerceAtLeast(10)
                )
            }
        }
    }

    private fun calculateSalesVelocity(productId: Long, days: Int): Int {
        // Query committed reservations in last N days from inventory_events
        return reservationRepository.countCommittedSince(
            productId = productId,
            since = java.time.Instant.now().minus(days.toLong(), java.time.temporal.ChronoUnit.DAYS)
        )
    }
}
```

---

## application.yml — Production Configuration

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/inventory_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      # Connection pool tuning
      maximum-pool-size: 20          # Don't go above 20-30 for PostgreSQL
      minimum-idle: 5
      connection-timeout: 3000       # Fail fast if pool exhausted
      idle-timeout: 600000
      max-lifetime: 1800000
      # Critical: set statement-level lock timeout to prevent lock pile-ups
      connection-init-sql: "SET lock_timeout = '5s'"

  jpa:
    hibernate:
      ddl-auto: validate            # NEVER 'create-drop' or 'update' in production
    properties:
      hibernate:
        # Optimistic lock retry
        jakarta.persistence.lock.timeout: 5000
        # Batch inserts for better performance
        jdbc.batch_size: 50
        order_inserts: true
        order_updates: true

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6379
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms               # Fail fast — Redis should be sub-millisecond
      lettuce:
        pool:
          max-active: 16
          max-idle: 8

# Retry configuration
spring.retry:
  enabled: true

# Scheduler — disable in non-primary instances if not using ShedLock
scheduling:
  enabled: true
```

---

## Production Pitfalls — The Real Lessons

> [!WARNING]
> **Connection pool exhaustion during flash sales**: If your pool has 20 connections and 200 concurrent reservations come in, 180 requests will wait for a connection. This causes cascading timeouts. During known flash sale events, pre-warm connections and consider temporarily increasing the pool size.

> [!CAUTION]
> **Forgetting to release Redis when DB release fails**: If your release logic updates DB but then throws before updating Redis, your Redis cache will show 0 stock forever (until TTL expires or cache is reseeded). Always use try-finally or transactional outbox patterns when syncing cache with DB.

> [!WARNING]
> **The N+1 expiry problem**: If 10,000 reservations expire at the exact same second (e.g., all created during a flash sale with the same TTL), your scheduler will try to process 10,000 releases in one batch. This creates a DB spike. Randomize expiry times: `RESERVATION_TTL_MINUTES + Random.nextInt(-1, 2)` to spread the load.

> [!IMPORTANT]
> **Audit log is non-negotiable**: Every inventory movement must be logged to `inventory_events`. When a merchant calls saying "I had 100 units but now I have 80 and only 15 orders went through," you need to trace exactly what happened. Without an audit log, this is forensically impossible.

> [!NOTE]
> **Negative stock as a business decision**: Some businesses explicitly allow negative stock (backorders). If you support backorders, `CHECK (stock_available >= 0)` must be removed. Add a separate `backorder_allowed` flag per product and handle the two paths explicitly in code.

---

## Summary Architecture

```
User Checkout Request
        │
        ▼
Redis DECR (fast pre-check, ~1ms)
        │
   Stock too low? ──YES──► Return 4xx immediately
        │
       NO
        │
        ▼
PostgreSQL SELECT FOR UPDATE / Optimistic Lock
        │
   Stock sufficient? ──NO──► Return 4xx + Release Redis
        │
       YES
        │
        ▼
INSERT into inventory_reservations
UPDATE products SET stock_reserved += quantity
        │
        ▼
Payment flow begins (10-minute TTL on reservation)
        │
   ┌────┴────┐
   │         │
Payment    Payment
Success    Failure/Timeout
   │         │
   ▼         ▼
COMMIT    RELEASE
(stock_  (stock_
available  reserved
decrements decrements,
stock_     stock back
committed  to available)
increments)
```
