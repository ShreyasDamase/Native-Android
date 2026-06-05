# Wallet and Ledger System

Standalone Kotlin + Spring Boot reference mini project.

## Run

```bash
docker compose up -d
gradle bootRun
```

Swagger:

```text
http://localhost:8084/swagger-ui.html
```

## API Flow

```http
POST /api/v1/wallets
GET /api/v1/wallets/{walletId}/balance
POST /api/v1/wallets/{walletId}/deposits
POST /api/v1/wallet-transfers
GET /api/v1/ledger/transactions/{transactionId}/entries
```

## What To Study

- `LedgerTransaction.validateBalanced`: double-entry correctness.
- `WalletService.transfer`: locks wallets, checks balance, posts debit and credit atomically.
- `WalletService.balance`: derives balance from immutable ledger entries.
- `idempotencyKey`: prevents duplicate deposits and transfers.
