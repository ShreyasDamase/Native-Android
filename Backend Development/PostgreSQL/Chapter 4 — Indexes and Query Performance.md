# Chapter 4 — Indexes and Query Performance

### _How PostgreSQL makes reads fast_

---

## 4.1 What an Index Does

An index helps PostgreSQL find rows without scanning the whole table.

Without index:

```text
Check every order row for user_id = X
```

With index:

```text
Jump directly to rows for user_id = X
```

Indexes speed reads but slow writes slightly. Add them based on real query patterns.

---

## 4.2 Basic Index

```sql
CREATE INDEX idx_orders_user_id
ON orders(user_id);
```

Good for:

```sql
SELECT *
FROM orders
WHERE user_id = '...';
```

---

## 4.3 Composite Index

```sql
CREATE INDEX idx_orders_user_created
ON orders(user_id, created_at DESC);
```

Good for:

```sql
SELECT *
FROM orders
WHERE user_id = '...'
ORDER BY created_at DESC
LIMIT 20;
```

Column order matters. Put equality filters first, then sorting/range columns.

---

## 4.4 Unique Index

```sql
CREATE UNIQUE INDEX uk_users_email_lower
ON users(lower(email));
```

Good for case-insensitive unique email.

---

## 4.5 Partial Index

Partial indexes index only some rows.

```sql
CREATE UNIQUE INDEX uk_driver_one_active_trip
ON trips(driver_id)
WHERE status IN ('DRIVER_ASSIGNED', 'ARRIVING', 'IN_PROGRESS');
```

This enforces:

```text
One driver can have many historical trips,
but only one active trip.
```

This is a powerful system design tool.

---

## 4.6 EXPLAIN

Use `EXPLAIN ANALYZE` to see what PostgreSQL actually does.

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM orders
WHERE user_id = '...'
ORDER BY created_at DESC
LIMIT 20;
```

Look for:

- sequential scan on huge table,
- missing index,
- too many rows scanned,
- slow sort,
- nested loops with huge row counts.

---

## 4.7 Index Mistakes

Avoid:

- indexing every column,
- ignoring composite index order,
- indexing low-cardinality columns alone,
- forgetting indexes on foreign keys,
- not using `EXPLAIN`,
- optimizing before knowing the query.

