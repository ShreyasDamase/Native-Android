# M5 — Payments & Wallet

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

The book does not make payments the center of Spring Boot. The Spring lesson here is transaction boundaries, idempotency, validation, security, and async messaging.

## Implementation Order

5.1 Build checkout quote before payment:

- Cart total
- Delivery fee
- Tax
- Coupon discount
- Inventory reservation TTL

5.2 Integrate Razorpay/Cashfree only in sandbox mode.

5.3 Implement payment intent creation with idempotency.

5.4 Implement webhook handler with idempotency and signature verification.

5.5 Build Wallet ledger:

- Credit
- Debit
- Balance
- Audit log
- Reconciliation status

5.6 Implement refund flow and reconciliation.

5.7 Build Coupon and Promo engine.

## Transaction Rule

Do not hold a database transaction open while calling a payment provider. Persist the checkout/payment intent, call the provider outside the transaction, then update state from provider response/webhook.
