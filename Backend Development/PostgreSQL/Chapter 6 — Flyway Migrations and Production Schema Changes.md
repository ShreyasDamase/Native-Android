# Chapter 6 — Flyway Migrations and Production Schema Changes

### _How to change PostgreSQL schema safely_

---

## 6.1 Why Migrations Matter

Production databases must change predictably.

Do not use:

```yaml
spring.jpa.hibernate.ddl-auto: update
```

Use:

```yaml
spring.jpa.hibernate.ddl-auto: validate
spring.flyway.enabled: true
```

Hibernate validates. Flyway changes schema.

---

## 6.2 Migration Files

```text
src/main/resources/db/migration/
    V001__create_users.sql
    V002__create_restaurants.sql
    V003__create_orders.sql
    V004__add_order_idempotency.sql
```

Example:

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(180) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

Flyway records applied migrations in `flyway_schema_history`.

---

## 6.3 Safe Schema Change Pattern

For big production tables, avoid dangerous one-step changes.

Example: adding non-null column.

Step 1:

```sql
ALTER TABLE orders ADD COLUMN source VARCHAR(40);
```

Step 2: deploy app that writes `source`.

Step 3: backfill old rows.

```sql
UPDATE orders
SET source = 'APP'
WHERE source IS NULL;
```

Step 4:

```sql
ALTER TABLE orders
ALTER COLUMN source SET NOT NULL;
```

This avoids breaking old rows and large locks.

---

## 6.4 Rollback Thinking

Flyway migrations usually move forward. Instead of relying on rollback, create a new migration that fixes the issue.

Good practice:

- test migrations locally,
- test on staging with production-like data,
- keep migrations small,
- avoid mixing schema and huge data changes,
- backup before risky migrations.

