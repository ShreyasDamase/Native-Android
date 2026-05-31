# Chapter 2 — Tables, Keys, Constraints and Schema Design

### _How to design PostgreSQL tables that protect your backend data_

---

## 2.1 The Goal

PostgreSQL is valuable because it does not only store data. It protects data.

Your schema should express business truth:

- users have unique emails,
- orders belong to users,
- order item quantity must be positive,
- a driver can have only one active trip,
- a booking cannot exist without a show,
- payment amount cannot be negative.

Do not leave all correctness to application code. Application bugs happen. Database constraints are your final safety net.

---

## 2.2 Primary Keys

Every important table needs a primary key.

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(180) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

Common choices:

| Key type | Good for | Notes |
|---|---|---|
| UUID | distributed apps, public ids | safe to generate in app |
| BIGSERIAL / identity | simple internal systems | smaller and index-friendly |
| natural key | rare cases | avoid if value can change |

For Spring Boot production apps, UUID is often a good default for business entities.

---

## 2.3 Foreign Keys

Foreign keys protect relationships.

```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

This prevents an order from referencing a user that does not exist.

Good practice:

- Use foreign keys inside one PostgreSQL database.
- Add indexes on foreign key columns used for lookups.
- Avoid cross-service database foreign keys if each microservice owns its own database.

---

## 2.4 Unique Constraints

Unique constraints enforce business rules.

```sql
ALTER TABLE users
ADD CONSTRAINT uk_users_email UNIQUE (email);
```

Order idempotency:

```sql
ALTER TABLE orders
ADD CONSTRAINT uk_orders_user_idempotency
UNIQUE (user_id, idempotency_key);
```

This protects against duplicate order creation when a mobile app retries the same request.

---

## 2.5 Check Constraints

Check constraints validate values.

```sql
CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price_cents BIGINT NOT NULL CHECK (unit_price_cents >= 0)
);
```

Use checks for:

- positive quantities,
- non-negative money,
- allowed numeric ranges,
- valid percentage range,
- simple enum-like values when not using enum types.

---

## 2.6 Timestamps

Most tables need timestamps.

```sql
created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
```

Use `TIMESTAMPTZ`, not `TIMESTAMP`, for production apps. It stores an absolute instant and avoids many timezone mistakes.

---

## 2.7 Money

Do not use floating point for money.

Good:

```sql
amount_cents BIGINT NOT NULL,
currency CHAR(3) NOT NULL
```

Avoid:

```sql
amount DOUBLE PRECISION
```

Floating point can produce rounding errors. Store money in the smallest unit, like cents or paise.

---

## 2.8 Soft Delete

For business records, prefer soft delete or status transitions.

```sql
deleted_at TIMESTAMPTZ
```

Use soft delete for:

- users,
- restaurants,
- products,
- menu items,
- support records.

Do not physically delete:

- orders,
- payments,
- ledger entries,
- invoices,
- audit records.

---

## 2.9 Production Table Example

```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    restaurant_id UUID NOT NULL REFERENCES restaurants(id),
    status VARCHAR(50) NOT NULL,
    total_amount_cents BIGINT NOT NULL CHECK (total_amount_cents >= 0),
    currency CHAR(3) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT,
    CONSTRAINT uk_orders_user_idempotency UNIQUE (user_id, idempotency_key)
);

CREATE INDEX idx_orders_user_created
ON orders(user_id, created_at DESC);
```

