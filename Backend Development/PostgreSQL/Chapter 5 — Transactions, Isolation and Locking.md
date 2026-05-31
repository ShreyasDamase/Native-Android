# Chapter 5 — Transactions, Isolation and Locking

### _How PostgreSQL prevents broken orders, bookings and payments_

---

## 5.1 What Is a Transaction?

A transaction groups multiple database operations into one unit.

```sql
BEGIN;

UPDATE accounts
SET balance_cents = balance_cents - 1000
WHERE id = 'A';

UPDATE accounts
SET balance_cents = balance_cents + 1000
WHERE id = 'B';

COMMIT;
```

If something fails:

```sql
ROLLBACK;
```

Transactions give ACID behavior:

- Atomicity: all or nothing.
- Consistency: constraints remain valid.
- Isolation: concurrent transactions do not corrupt each other.
- Durability: committed data survives.

---

## 5.2 Booking Seat Lock

```sql
BEGIN;

SELECT *
FROM show_seats
WHERE show_id = '...'
AND seat_id IN ('...', '...')
FOR UPDATE;

UPDATE show_seats
SET status = 'HELD'
WHERE show_id = '...'
AND seat_id IN ('...', '...')
AND status = 'AVAILABLE';

COMMIT;
```

`FOR UPDATE` locks selected rows until commit. Other transactions must wait.

Use this for:

- booking seats,
- reserving hotel rooms,
- inventory decrement,
- wallet movement,
- driver assignment.

---

## 5.3 Optimistic Locking

Optimistic locking uses a version column.

```sql
ALTER TABLE orders ADD COLUMN version BIGINT;
```

Spring/JPA:

```kotlin
@Version
var version: Long? = null
```

If two requests update the same row, one wins and the other fails instead of silently overwriting.

Use when conflicts are rare.

---

## 5.4 Pessimistic Locking

Pessimistic locking locks rows before changing them.

Use when conflicts are likely or correctness is critical:

- two users booking same seat,
- checkout inventory,
- assigning same driver,
- wallet debit.

Spring Data JPA:

```kotlin
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select s from ShowSeat s where s.showId = :showId and s.seatId in :seatIds")
fun findForUpdate(showId: UUID, seatIds: Set<UUID>): List<ShowSeat>
```

---

## 5.5 Isolation Levels

Common isolation levels:

| Level | Meaning |
|---|---|
| READ COMMITTED | default, sees committed data |
| REPEATABLE READ | same rows remain stable in transaction |
| SERIALIZABLE | strongest, behaves like transactions ran one by one |

Most Spring Boot apps use PostgreSQL default `READ COMMITTED` plus explicit row locks for critical flows.

---

## 5.6 Deadlocks

Deadlock happens when transactions wait on each other.

Prevent by:

- locking rows in consistent order,
- keeping transactions short,
- not calling external APIs inside transactions,
- indexing lock queries,
- retrying deadlock failures safely.

