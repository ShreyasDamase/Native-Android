# Chapter 13 — PostgreSQL Production Data Modeling

### _The source of truth for users, orders, bookings, payments, ledgers and business rules_

---

## 13.1 Why PostgreSQL Is the Default

For most backend systems, PostgreSQL should be your first database. It gives you:

- ACID transactions.
- Foreign keys and unique constraints.
- Rich indexes.
- JSONB when a small part of the model is flexible.
- Row-level locks for booking and inventory.
- Geospatial support through PostGIS.
- Mature backup, replication, and monitoring ecosystem.

Use PostgreSQL for the facts the business cannot afford to lose: users, orders, bookings, payments, invoices, driver payouts, restaurant menus, hotel inventory, subscriptions, audit records and permissions.

Redis, Elasticsearch and Cassandra may copy or accelerate data, but PostgreSQL should usually remain the source of truth.

---

## 13.2 Entity Design in Kotlin

Kotlin data classes are excellent for DTOs, but JPA entities need care because Hibernate creates proxies and manages object identity.

```kotlin
@Entity
@Table(
    name = "bookings",
    indexes = [
        Index(name = "idx_bookings_user_created", columnList = "user_id, created_at"),
        Index(name = "idx_bookings_status", columnList = "status")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_bookings_idempotency", columnNames = ["user_id", "idempotency_key"])
    ]
)
class Booking(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(name = "slot_id", nullable = false)
    var slotId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: BookingStatus,

    @Column(name = "idempotency_key", nullable = false, length = 80)
    var idempotencyKey: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Version
    var version: Long? = null
) {
    fun cancel(now: Instant = Instant.now()) {
        require(status == BookingStatus.CONFIRMED) { "Only confirmed bookings can be cancelled" }
        status = BookingStatus.CANCELLED
    }
}
```

Annotation explanation:

- `@Entity`: tells Hibernate this class maps to a database table.
- `@Table`: controls table name, indexes and constraints.
- `@Id`: primary key.
- `@GeneratedValue(strategy = GenerationType.UUID)`: lets Hibernate generate UUID ids.
- `@Column(nullable = false)`: generates/validates a non-null database column.
- `@Enumerated(EnumType.STRING)`: stores enum names, not ordinal numbers. Ordinals are dangerous because reordering enum values corrupts meaning.
- `@Version`: enables optimistic locking. If two requests update the same row, one fails instead of silently overwriting.

Good practice: put business transitions in methods (`cancel`, `confirm`, `assignDriver`) instead of letting every service mutate fields randomly.

---

## 13.3 Flyway Migrations

Production schema should be versioned with SQL files:

```text
src/main/resources/db/migration/
    V001__create_users.sql
    V002__create_bookings.sql
    V003__add_booking_idempotency_key.sql
```

Example:

```sql
CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    slot_id UUID NOT NULL,
    status VARCHAR(40) NOT NULL,
    idempotency_key VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT,
    CONSTRAINT uk_bookings_idempotency UNIQUE (user_id, idempotency_key)
);

CREATE INDEX idx_bookings_user_created ON bookings(user_id, created_at DESC);
CREATE INDEX idx_bookings_status ON bookings(status);
```

Production rule:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

Never use `ddl-auto=update` in production. It hides schema changes, makes rollback unclear, and can perform unsafe alterations.

---

## 13.4 Transactions and Locking

Booking, checkout, inventory and wallet flows need transaction discipline.

```kotlin
interface SlotRepository : JpaRepository<Slot, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Slot s where s.id = :slotId")
    fun findByIdForUpdate(slotId: UUID): Slot?
}
```

```kotlin
@Service
class SlotReservationService(
    private val slotRepository: SlotRepository,
    private val bookingRepository: BookingRepository
) {
    @Transactional
    fun reserve(command: ReserveSlotCommand): Booking {
        val slot = slotRepository.findByIdForUpdate(command.slotId)
            ?: throw NotFoundException("Slot not found")

        if (slot.remainingCapacity <= 0) {
            throw ConflictException("Slot is sold out")
        }

        slot.remainingCapacity -= 1
        return bookingRepository.save(Booking.confirmed(command))
    }
}
```

Annotation explanation:

- `@Lock(PESSIMISTIC_WRITE)`: asks the database to lock the selected row until transaction commit.
- `@Transactional`: keeps the read, validation and write inside one atomic unit.

Use pessimistic locking when overselling is unacceptable and contention is expected. Use optimistic locking when conflicts are rare and retry is acceptable.

---

## 13.5 Idempotency for Payments and Orders

Mobile apps retry. Networks fail. Users double tap. Payment providers send webhooks multiple times.

Idempotency means: the same logical request can run twice but only produce one business effect.

```kotlin
@PostMapping("/orders")
fun createOrder(
    @RequestHeader("Idempotency-Key") idempotencyKey: String,
    @Valid @RequestBody request: CreateOrderRequest,
    principal: JwtAuthenticationToken
): ResponseEntity<OrderResponse> {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(orderService.create(request.toCommand(principal.userId(), idempotencyKey)))
}
```

Repository:

```kotlin
interface OrderRepository : JpaRepository<Order, UUID> {
    fun findByUserIdAndIdempotencyKey(userId: UUID, idempotencyKey: String): Order?
}
```

Service pattern:

```kotlin
@Transactional
fun create(command: CreateOrderCommand): OrderResponse {
    orderRepository.findByUserIdAndIdempotencyKey(command.userId, command.idempotencyKey)
        ?.let { return OrderResponse.from(it) }

    val order = orderRepository.save(Order.from(command))
    return OrderResponse.from(order)
}
```

The unique database constraint is still required. The service check improves UX; the constraint protects correctness.

---

## 13.6 Indexing Rules

Indexes speed reads and slow writes. Add them intentionally.

Good indexes:

```sql
-- "My bookings ordered by newest"
CREATE INDEX idx_bookings_user_created ON bookings(user_id, created_at DESC);

-- "Active drivers in city"
CREATE INDEX idx_drivers_city_status ON drivers(city_id, status);

-- Case-insensitive email lookup
CREATE UNIQUE INDEX uk_users_email_lower ON users(lower(email));
```

Avoid:

- Indexing every column.
- Indexing low-cardinality columns alone, like `status`, unless the filtered subset is small.
- Ignoring query shape. Index column order should match how you filter and sort.

Always inspect with:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM bookings
WHERE user_id = '...'
ORDER BY created_at DESC
LIMIT 20;
```

---

## 13.7 Money and Ledger Tables

Never store money as floating point.

```kotlin
@Embeddable
data class Money(
    @Column(name = "amount_cents", nullable = false)
    val amountCents: Long,

    @Column(name = "currency", nullable = false, length = 3)
    val currency: String
)
```

For wallets, driver payouts, commissions and refunds, use ledger entries:

```sql
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    direction VARCHAR(10) NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount_cents BIGINT NOT NULL CHECK (amount_cents > 0),
    currency CHAR(3) NOT NULL,
    reference_type VARCHAR(50) NOT NULL,
    reference_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

Ledger rule: append new entries; do not edit old financial facts.

---

## 13.8 JSONB: Useful, But Not a Trash Can

JSONB is good for:

- External provider payload snapshot.
- Feature flags/settings.
- Flexible metadata that is not frequently queried.

JSONB is bad for:

- Core relational facts.
- Values that need foreign keys.
- Values used in many joins.
- Money, bookings, inventory, permissions.

Example:

```sql
ALTER TABLE payment_attempts
ADD COLUMN provider_response JSONB NOT NULL DEFAULT '{}';
```

---

## 13.9 PostGIS for Delivery and Booking Apps

For serious geospatial queries, use PostGIS:

```sql
CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE restaurants
ADD COLUMN location GEOGRAPHY(POINT, 4326) NOT NULL;

CREATE INDEX idx_restaurants_location
ON restaurants USING GIST(location);
```

Use it for:

- Restaurants within 3 km.
- Hotels near a point.
- Service areas.
- Delivery distance calculations.

Use Redis GEO for very fast temporary "nearby available drivers" lookups. Use PostGIS for durable business data and complex geospatial filtering.

---

## 13.10 Source Links

- Spring Boot SQL databases: https://docs.spring.io/spring-boot/reference/data/sql.html
- Spring Boot data overview: https://docs.spring.io/spring-boot/reference/data/index.html
- Flyway: https://documentation.red-gate.com/fd

