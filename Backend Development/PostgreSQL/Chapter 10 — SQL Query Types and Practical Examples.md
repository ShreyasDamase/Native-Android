# Chapter 10 — SQL Query Types and Practical Examples

### _What each query does, when to use it, and production examples_

---

## 10.1 SQL Query Categories

SQL queries are usually grouped like this:

| Category | Meaning | Examples |
|---|---|---|
| DDL | define schema | `CREATE`, `ALTER`, `DROP` |
| DML | change data | `INSERT`, `UPDATE`, `DELETE` |
| DQL | read data | `SELECT` |
| TCL | transaction control | `BEGIN`, `COMMIT`, `ROLLBACK` |
| DCL | permissions | `GRANT`, `REVOKE` |

Backend developers mostly write DQL and DML. Migrations use DDL. Production workflows use transactions.

---

## 10.2 SELECT

Reads data from a table.

```sql
SELECT id, email, created_at
FROM users;
```

Use when:

- fetching list pages,
- loading user profile,
- reading orders,
- building admin dashboards.

Avoid `SELECT *` in production APIs when you only need a few columns.

---

## 10.3 WHERE

Filters rows.

```sql
SELECT id, status, total_amount_cents
FROM orders
WHERE user_id = '11111111-1111-1111-1111-111111111111';
```

Multiple filters:

```sql
SELECT *
FROM orders
WHERE user_id = '...'
AND status = 'DELIVERED';
```

Use indexes for frequent filters.

---

## 10.4 ORDER BY

Sorts results.

```sql
SELECT id, status, created_at
FROM orders
WHERE user_id = '...'
ORDER BY created_at DESC;
```

Good index:

```sql
CREATE INDEX idx_orders_user_created
ON orders(user_id, created_at DESC);
```

---

## 10.5 LIMIT and OFFSET Pagination

```sql
SELECT id, status, created_at
FROM orders
WHERE user_id = '...'
ORDER BY created_at DESC
LIMIT 20 OFFSET 40;
```

What it does:

- `LIMIT 20`: return 20 rows.
- `OFFSET 40`: skip first 40 rows.

Good for small pages. For very large data, prefer keyset pagination.

---

## 10.6 Keyset Pagination

Better for infinite scroll and large tables.

```sql
SELECT id, status, created_at
FROM orders
WHERE user_id = '...'
AND created_at < '2026-05-31T10:00:00Z'
ORDER BY created_at DESC
LIMIT 20;
```

Use when:

- feed,
- order history,
- chat messages,
- event logs.

Why better: database does not need to skip thousands of rows.

---

## 10.7 INSERT

Adds rows.

```sql
INSERT INTO users (id, email, created_at)
VALUES ('...', 'user@example.com', now());
```

Return inserted row:

```sql
INSERT INTO users (id, email)
VALUES ('...', 'user@example.com')
RETURNING id, email, created_at;
```

`RETURNING` is very useful in PostgreSQL.

---

## 10.8 UPDATE

Changes existing rows.

```sql
UPDATE orders
SET status = 'DELIVERED',
    updated_at = now()
WHERE id = '...';
```

Safe update rule: always check the `WHERE` clause.

State transition update:

```sql
UPDATE orders
SET status = 'DELIVERED',
    updated_at = now()
WHERE id = '...'
AND status = 'OUT_FOR_DELIVERY';
```

This prevents invalid transitions.

---

## 10.9 DELETE

Deletes rows.

```sql
DELETE FROM cart_items
WHERE cart_id = '...';
```

For business history, prefer soft delete:

```sql
UPDATE restaurants
SET deleted_at = now()
WHERE id = '...';
```

Do not delete orders, payments, invoices or ledger entries casually.

---

## 10.10 UPSERT

Insert if new, update if already exists.

```sql
INSERT INTO user_devices (user_id, device_id, last_seen_at)
VALUES ('...', 'device-123', now())
ON CONFLICT (user_id, device_id)
DO UPDATE SET last_seen_at = excluded.last_seen_at;
```

Use for:

- device registration,
- counters,
- user preferences,
- deduplication records.

`excluded` means the row you attempted to insert.

---

## 10.11 JOIN

Combines related tables.

```sql
SELECT o.id, o.status, u.email
FROM orders o
JOIN users u ON u.id = o.user_id
WHERE o.id = '...';
```

Use when:

- order needs user data,
- menu item needs restaurant data,
- booking needs show/movie data.

---

## 10.12 LEFT JOIN

Returns rows even when related row is missing.

```sql
SELECT o.id, o.status, p.status AS payment_status
FROM orders o
LEFT JOIN payment_attempts p ON p.order_id = o.id
WHERE o.id = '...';
```

Use when relationship is optional:

- order may not have payment yet,
- user may not have profile yet,
- delivery assignment may not exist yet.

---

## 10.13 GROUP BY

Groups rows for aggregation.

```sql
SELECT restaurant_id, COUNT(*) AS order_count
FROM orders
WHERE created_at >= now() - interval '1 day'
GROUP BY restaurant_id;
```

Use for:

- analytics,
- reports,
- counts,
- totals,
- dashboards.

---

## 10.14 HAVING

Filters grouped results.

```sql
SELECT restaurant_id, COUNT(*) AS order_count
FROM orders
GROUP BY restaurant_id
HAVING COUNT(*) > 100;
```

`WHERE` filters rows before grouping.

`HAVING` filters groups after grouping.

---

## 10.15 Aggregate Functions

```sql
SELECT
    COUNT(*) AS total_orders,
    SUM(total_amount_cents) AS revenue_cents,
    AVG(total_amount_cents) AS average_order_value,
    MIN(created_at) AS first_order_at,
    MAX(created_at) AS latest_order_at
FROM orders
WHERE status = 'DELIVERED';
```

Common aggregate functions:

- `COUNT`
- `SUM`
- `AVG`
- `MIN`
- `MAX`

---

## 10.16 Subquery

A query inside another query.

```sql
SELECT *
FROM restaurants
WHERE id IN (
    SELECT restaurant_id
    FROM orders
    WHERE created_at >= now() - interval '1 day'
);
```

Use when one query depends on another result.

For readability, CTEs are often better.

---

## 10.17 EXISTS

Checks whether matching rows exist.

```sql
SELECT *
FROM restaurants r
WHERE EXISTS (
    SELECT 1
    FROM menu_items m
    WHERE m.restaurant_id = r.id
    AND m.available = true
);
```

Use `EXISTS` when you only care whether related rows exist, not how many.

---

## 10.18 CTE

CTE means Common Table Expression. It creates a named temporary result inside one query.

```sql
WITH recent_orders AS (
    SELECT *
    FROM orders
    WHERE created_at >= now() - interval '1 day'
)
SELECT restaurant_id, COUNT(*)
FROM recent_orders
GROUP BY restaurant_id;
```

Use CTEs for:

- readability,
- multi-step reports,
- complex transformations.

---

## 10.19 Window Functions

Window functions calculate values across related rows without collapsing them.

```sql
SELECT
    id,
    user_id,
    total_amount_cents,
    ROW_NUMBER() OVER (
        PARTITION BY user_id
        ORDER BY created_at DESC
    ) AS order_rank
FROM orders;
```

Use for:

- ranking,
- latest row per group,
- running totals,
- analytics.

Latest order per user:

```sql
WITH ranked_orders AS (
    SELECT
        *,
        ROW_NUMBER() OVER (
            PARTITION BY user_id
            ORDER BY created_at DESC
        ) AS rn
    FROM orders
)
SELECT *
FROM ranked_orders
WHERE rn = 1;
```

---

## 10.20 DISTINCT

Removes duplicate values.

```sql
SELECT DISTINCT restaurant_id
FROM orders;
```

Use when you need unique values.

PostgreSQL also supports `DISTINCT ON`:

```sql
SELECT DISTINCT ON (user_id)
    user_id, id, created_at
FROM orders
ORDER BY user_id, created_at DESC;
```

This returns the latest order per user.

---

## 10.21 CASE

Conditional logic inside SQL.

```sql
SELECT
    id,
    total_amount_cents,
    CASE
        WHEN total_amount_cents >= 100000 THEN 'HIGH'
        WHEN total_amount_cents >= 30000 THEN 'MEDIUM'
        ELSE 'LOW'
    END AS order_value_band
FROM orders;
```

Use for reporting and computed labels.

---

## 10.22 Transactions

```sql
BEGIN;

UPDATE wallets
SET balance_cents = balance_cents - 50000
WHERE user_id = '...';

INSERT INTO payment_attempts (id, order_id, amount_cents, status)
VALUES ('...', '...', 50000, 'AUTHORIZED');

COMMIT;
```

If anything fails:

```sql
ROLLBACK;
```

Use transactions for:

- orders,
- payments,
- bookings,
- inventory,
- wallet/ledger movement.

---

## 10.23 SELECT FOR UPDATE

Locks rows for update inside a transaction.

```sql
BEGIN;

SELECT *
FROM show_seats
WHERE show_id = '...'
AND seat_id IN ('...', '...')
FOR UPDATE;

UPDATE show_seats
SET status = 'BOOKED'
WHERE show_id = '...'
AND seat_id IN ('...', '...');

COMMIT;
```

Use for:

- seat booking,
- hotel room reservation,
- inventory decrement,
- assigning one driver,
- wallet debit.

---

## 10.24 JSONB Queries

```sql
SELECT *
FROM payment_attempts
WHERE provider_response ->> 'status' = 'succeeded';
```

Check if JSON contains value:

```sql
SELECT *
FROM audit_events
WHERE payload @> '{"type": "ORDER_CREATED"}';
```

Use JSONB for flexible metadata, not core relational data.

---

## 10.25 Full-Text Search Query

```sql
SELECT *
FROM restaurants
WHERE search_vector @@ plainto_tsquery('pizza');
```

Rank results:

```sql
SELECT
    *,
    ts_rank(search_vector, plainto_tsquery('pizza')) AS rank
FROM restaurants
WHERE search_vector @@ plainto_tsquery('pizza')
ORDER BY rank DESC;
```

Use PostgreSQL full-text for basic search. Use Elasticsearch for advanced search products.

---

## 10.26 EXPLAIN ANALYZE

Shows query plan and real runtime.

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM orders
WHERE user_id = '...'
ORDER BY created_at DESC
LIMIT 20;
```

Use it when:

- API is slow,
- query scans too many rows,
- index may be missing,
- join is expensive.

---

## 10.27 DDL Queries

Create table:

```sql
CREATE TABLE coupons (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    discount_percent INT NOT NULL CHECK (discount_percent BETWEEN 1 AND 100)
);
```

Alter table:

```sql
ALTER TABLE orders
ADD COLUMN coupon_id UUID REFERENCES coupons(id);
```

Drop table:

```sql
DROP TABLE coupons;
```

In production, DDL should be done through Flyway migrations.

---

## 10.28 GRANT and REVOKE

Permissions:

```sql
GRANT SELECT, INSERT, UPDATE ON orders TO app_user;
REVOKE DELETE ON orders FROM app_user;
```

Use separate database users for:

- application,
- migration,
- read-only analytics,
- admin.

---

## 10.29 Practical Query Map

| Need | Query type |
|---|---|
| list user orders | `SELECT WHERE ORDER BY LIMIT` |
| create user | `INSERT RETURNING` |
| update order status | `UPDATE WHERE status = ...` |
| delete cart item | `DELETE` |
| create or refresh device token | `UPSERT` |
| load order with user | `JOIN` |
| include optional payment | `LEFT JOIN` |
| count restaurant orders | `GROUP BY` |
| restaurants with menu items | `EXISTS` |
| latest order per user | window function or `DISTINCT ON` |
| book seats safely | transaction + `FOR UPDATE` |
| debug slow API | `EXPLAIN ANALYZE` |

