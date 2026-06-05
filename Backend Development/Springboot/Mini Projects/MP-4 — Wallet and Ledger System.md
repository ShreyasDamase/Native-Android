# MP-4 — Wallet and Ledger System

Build a backend wallet with deposits, transfers and immutable ledger entries.

This is the most correctness-focused mini project. It teaches why financial systems do not simply update a balance column and hope for the best.

Full reference codebase:

[code/wallet-ledger](/Users/shreyasdamase/Sentinel/Native-Android-Notes/Backend%20Development/Springboot/Mini%20Projects/code/wallet-ledger)

## Learning Goals

- Understand double-entry ledger design.
- Keep financial records immutable.
- Use transactions correctly.
- Prevent duplicate money movement with idempotency.
- Build audit-friendly APIs.
- Learn why consistency matters more than speed in payment systems.

## Core Features

### Phase 1: Accounts and Wallets

Build:

- User account.
- Wallet account.
- Currency support.
- Balance query.

APIs:

```http
POST /api/v1/wallets
GET /api/v1/wallets/{walletId}
GET /api/v1/wallets/{walletId}/balance
```

### Phase 2: Deposits

API:

```http
POST /api/v1/wallets/{walletId}/deposits
```

Rules:

- Payment provider is sandbox only.
- Deposit creates ledger entries.
- Balance is derived from ledger or updated inside the same transaction.
- Repeating the same idempotency key must not duplicate the deposit.

### Phase 3: Transfers

API:

```http
POST /api/v1/wallet-transfers
```

Request:

```json
{
  "fromWalletId": "uuid",
  "toWalletId": "uuid",
  "amount": "250.00",
  "currency": "INR"
}
```

Rules:

- Debit sender.
- Credit receiver.
- Total debit must equal total credit.
- Sender cannot go below zero unless overdraft is explicitly enabled.
- Transfer must be atomic.

### Phase 4: Ledger History

APIs:

```http
GET /api/v1/wallets/{walletId}/transactions
GET /api/v1/ledger/entries?referenceId=uuid
```

Rules:

- Ledger entries are append-only.
- Do not edit or delete old entries.
- Reversals are new entries pointing to the original transaction.

## Domain Model

```text
User
Wallet
LedgerTransaction
LedgerEntry
Money
IdempotencyRecord
```

States:

```text
LedgerTransactionStatus = PENDING, POSTED, REVERSED, FAILED
LedgerEntryType = DEBIT, CREDIT
WalletStatus = ACTIVE, FROZEN, CLOSED
```

## Database Tables

Start with:

```text
users
wallets
ledger_transactions
ledger_entries
idempotency_keys
```

Important constraints:

- Amount must be positive.
- Currency must match between related entries.
- Ledger transaction must balance debits and credits.
- Idempotency key must be unique per operation.
- Wallet cannot be closed with non-zero balance.

## Package Structure

```text
wallet/
  api/
    WalletController.kt
    DepositController.kt
    TransferController.kt
    LedgerController.kt
  application/
    CreateWalletUseCase.kt
    DepositMoneyUseCase.kt
    TransferMoneyUseCase.kt
    ReverseTransactionUseCase.kt
  domain/
    Money.kt
    Wallet.kt
    LedgerTransaction.kt
    LedgerEntry.kt
    DoubleEntryValidator.kt
  infrastructure/
    WalletJpaRepository.kt
    LedgerTransactionJpaRepository.kt
    LedgerEntryJpaRepository.kt
```

## Correctness Lesson

Bad design:

```text
wallet.balance = wallet.balance - amount
receiver.balance = receiver.balance + amount
```

Better design:

```text
LedgerTransaction
  DEBIT  sender_wallet   250 INR
  CREDIT receiver_wallet 250 INR
```

Then either:

- Derive balance from ledger entries.
- Maintain a balance table that is updated only in the same transaction as ledger posting.

For learning, first derive balance from entries. Later add cached balance and learn how to keep it correct.

## Tests

Required tests:

- Deposit creates balanced ledger transaction.
- Transfer creates one debit and one credit.
- Transfer fails when sender has insufficient funds.
- Failed transfer creates no partial ledger entries.
- Duplicate idempotency key returns original result.
- Ledger transaction with unequal debit/credit is rejected.
- Reversal creates new opposite entries, not edits to old entries.

## Stretch Features

- Wallet freeze/unfreeze.
- Refunds.
- Multi-currency guardrails.
- Daily transfer limits.
- Audit export.
- Outbox event: `WalletTransactionPosted`.
