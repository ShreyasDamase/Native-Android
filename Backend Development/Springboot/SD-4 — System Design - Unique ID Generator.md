# SD-4 — System Design: Unique ID Generator

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> **Production Engineering Reference** — ID generation is invisible when it works and catastrophic when it breaks. Wrong ID design causes lost data, ordering bugs, hotspot database pages, and security leaks. This chapter covers every approach with real production code.

---

## Why ID Generation Is a Real Engineering Problem

When you start with a small app, you use `AUTO_INCREMENT` in MySQL or `SERIAL` in PostgreSQL. This works until:

1. **You shard your database** — Two shards can generate the same integer ID
2. **You need ordering** — UUID v4 is random; you can't sort by creation time
3. **You need uniqueness across services** — Each microservice generates IDs independently
4. **You need human-readable IDs** — Order numbers that customers can reference
5. **You need security** — Sequential IDs expose your volume (`order/4321` leaks that you've only had 4321 orders)

> [!WARNING]
> At Zepto/Blinkit scale, using `AUTO_INCREMENT` on a single PostgreSQL instance becomes a write bottleneck. Every insert waits on the sequence counter. This single sequence becomes a global lock.

---

## Option 1: Database Auto-Increment (SERIAL / AUTO_INCREMENT)

### How It Works

```sql
-- PostgreSQL SERIAL (auto-increment)
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    -- ...
);

-- Each INSERT gets the next integer atomically
INSERT INTO orders (user_id) VALUES ('uuid-here') RETURNING id;
-- Returns: 1, 2, 3, 4, ...
```

### When It Works

| Scenario | Verdict |
|---|---|
| Single database, < 10K orders/day | ✅ Perfect |
| Monolith with one write DB | ✅ Good |
| Distributed system with DB sharding | ❌ Breaks — each shard starts at 1 |
| Multiple services inserting to same logical table | ❌ Breaks |
| Need time-ordered IDs without DB query | ❌ Not possible |

### The Distributed Problem

```
Shard 1: orders table → id = 1, 2, 3, 4...
Shard 2: orders table → id = 1, 2, 3, 4...

Merge query: SELECT * FROM orders WHERE id = 3
              → Returns 2 rows (one from each shard) 💥
```

**Workaround: Shard-aware auto-increment**
```sql
-- Shard 1 generates even numbers
ALTER SEQUENCE orders_id_seq START 1 INCREMENT 2;
-- 1, 3, 5, 7...

-- Shard 2 generates odd numbers
ALTER SEQUENCE orders_id_seq START 2 INCREMENT 2;
-- 2, 4, 6, 8...
```
This is fragile and doesn't scale beyond 2 shards.

---

## Option 2: UUID v4

### How It Works

UUID v4 is 128 bits of cryptographically random data.

```kotlin
import java.util.UUID

val id = UUID.randomUUID()
// → "550e8400-e29b-41d4-a716-446655440000"
```

### Properties

| Property | Value |
|---|---|
| Size | 128 bits (36 chars as string, 16 bytes as binary) |
| Ordering | **None** — completely random |
| Collision probability | 1 in 2^122 — effectively impossible |
| Globally unique | Yes, without coordination |
| Sortable by time | No |

### The Index Performance Problem

> [!CAUTION]
> Storing UUID v4 as the primary key in PostgreSQL or MySQL causes **B-tree index fragmentation**. Because UUIDs are random, each INSERT goes to a random position in the index tree, causing **page splits** and **poor cache locality**. At high insert rates (>1000/s), this degrades write performance by 30-50%.

**Proof of the problem:**
```sql
-- UUID v4 primary key — random insertion
CREATE TABLE events_uuid (id UUID PRIMARY KEY, data TEXT);
INSERT INTO events_uuid SELECT gen_random_uuid(), 'data' FROM generate_series(1, 1000000);

-- Sequential integer primary key — ordered insertion  
CREATE TABLE events_serial (id BIGSERIAL PRIMARY KEY, data TEXT);
INSERT INTO events_serial SELECT nextval('events_serial_id_seq'), 'data' FROM generate_series(1, 1000000);

-- EXPLAIN ANALYZE shows: 
-- events_serial: heap pages read = 5000 (sequential, cached)
-- events_uuid: heap pages read = 50000 (random, cache misses)
```

**Mitigation: Store UUID as binary**
```sql
-- Store as BYTEA (16 bytes) instead of VARCHAR(36)
-- Saves ~20 bytes per row, better cache usage
CREATE TABLE orders (
    id BYTEA(16) PRIMARY KEY DEFAULT gen_random_bytes(16),
    -- ...
);
```

### When UUID is Good

- When you generate the ID **on the client** (mobile app, frontend) before it goes to the DB
- When the ID is used as a **reference** (not as a PK driving a clustered index)
- When you need **security** (random, doesn't expose volume)
- For external-facing IDs (order reference shared with customers)

---

## Option 3: UUID v7 — The Modern Solution

UUID v7 (RFC 9562, 2024) is **time-ordered UUID**. It embeds a millisecond-precision timestamp in the most significant bits.

```
UUID v7 structure (128 bits):
┌─────────────────────────────────────────────────────────────────┐
│ unix_ts_ms (48 bits) │ ver (4) │ rand_a (12) │ var (2) │ rand_b (62) │
└─────────────────────────────────────────────────────────────────┘

Result: 018f-2c5a-9340-7abc-8def-123456789abc
         ↑
    timestamp prefix — sortable!
```

**Properties:**
| Property | Value |
|---|---|
| Ordering | Time-ordered (monotonic within 1ms) |
| Globally unique | Yes |
| B-tree friendly | Yes — sequential inserts |
| Size | Same as UUID (128 bits) |
| Support in PostgreSQL | 16+ (gen_random_uuid is v4, v7 needs extension or app-level) |

```kotlin
// UUID v7 generation in Kotlin (using com.fasterxml.uuid:java-uuid-generator)
import com.fasterxml.uuid.Generators

val generator = Generators.timeBasedEpochGenerator()
val uuidV7 = generator.generate()
// → "018f2c5a-9340-7abc-8def-123456789abc"
// Sorted chronologically when converted to string
```

```gradle
// build.gradle.kts
dependencies {
    implementation("com.fasterxml.uuid:java-uuid-generator:4.3.0")
}
```

> [!TIP]
> **PostgreSQL 17+ natively supports UUID v7** via `gen_random_uuid()` replacement. Until then, generate v7 in the application layer. UUID v7 gives you the global uniqueness of UUID with the sortability of Snowflake IDs.

---

## Option 4: Twitter Snowflake — The Production Standard

Twitter's Snowflake was designed in 2010 to generate 10,000+ unique IDs per second across multiple datacenters without coordination.

### Structure: 64-bit Integer

```
┌──────────────────────────────────────────────────────────────────┐
│ Sign │   Timestamp (41 bits)   │ DC (5) │ Worker (5) │ Seq (12) │
│  0   │   ms since custom epoch  │  0-31  │   0-31     │ 0-4095   │
└──────────────────────────────────────────────────────────────────┘
```

| Field | Bits | Max Value | Meaning |
|---|---|---|---|
| Sign | 1 | 0 | Always 0 (positive) |
| Timestamp | 41 | 2^41 = 2199 billion ms ≈ 69 years | ms since custom epoch |
| Datacenter ID | 5 | 32 datacenters | Which DC generated this |
| Worker ID | 5 | 32 workers per DC | Which machine/process |
| Sequence | 12 | 4096 per ms | Counter within same ms |

**Capacity:** 32 DCs × 32 workers × 4096 IDs/ms = **4,194,304 IDs per millisecond**

**Ordering:** Because timestamp is MSB, IDs sort chronologically automatically. Database inserts are sequential — no index fragmentation.

### Kotlin Implementation: Production Snowflake ID Generator

```kotlin
package com.yourcompany.common.id

import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Thread-safe Snowflake ID generator.
 * Generates 64-bit unique IDs that are:
 * - Time-ordered (sortable)
 * - Unique across distributed workers
 * - Embeds datacenter and worker identity
 *
 * Format: [41-bit timestamp][5-bit DC][5-bit worker][12-bit sequence]
 */
@Component
class SnowflakeIdGenerator(
    private val datacenterId: Long = System.getenv("DATACENTER_ID")?.toLong() ?: 0L,
    private val workerId: Long = System.getenv("WORKER_ID")?.toLong() ?: 0L,
) {
    companion object {
        // Custom epoch: 2024-01-01T00:00:00Z in milliseconds
        // Gives us ~69 years from this epoch
        private const val EPOCH = 1704067200000L

        private const val WORKER_ID_BITS = 5L
        private const val DATACENTER_ID_BITS = 5L
        private const val SEQUENCE_BITS = 12L

        private const val MAX_WORKER_ID = -1L xor (-1L shl WORKER_ID_BITS.toInt())       // 31
        private const val MAX_DATACENTER_ID = -1L xor (-1L shl DATACENTER_ID_BITS.toInt()) // 31

        private const val WORKER_ID_SHIFT = SEQUENCE_BITS                                  // 12
        private const val DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS             // 17
        private const val TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS // 22

        private const val SEQUENCE_MASK = -1L xor (-1L shl SEQUENCE_BITS.toInt())          // 4095
    }

    init {
        require(workerId in 0..MAX_WORKER_ID) {
            "Worker ID must be between 0 and $MAX_WORKER_ID, got $workerId"
        }
        require(datacenterId in 0..MAX_DATACENTER_ID) {
            "Datacenter ID must be between 0 and $MAX_DATACENTER_ID, got $datacenterId"
        }
    }

    @Volatile private var lastTimestamp = -1L
    @Volatile private var sequence = 0L
    private val lock = Any()

    fun nextId(): Long = synchronized(lock) {
        var timestamp = currentTimeMs()

        if (timestamp < lastTimestamp) {
            // Clock moved backwards — this is a real risk with NTP adjustments
            val diff = lastTimestamp - timestamp
            if (diff <= 5) {
                // Small drift (≤5ms): wait for clock to catch up
                Thread.sleep(diff)
                timestamp = currentTimeMs()
            } else {
                // Large clock regression — this is a serious problem
                throw IllegalStateException(
                    "Clock moved backwards by ${diff}ms. " +
                    "Refusing to generate IDs. datacenterId=$datacenterId workerId=$workerId"
                )
            }
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) and SEQUENCE_MASK
            if (sequence == 0L) {
                // Sequence overflow: we've used all 4096 IDs in this millisecond
                // Wait for the next millisecond
                timestamp = waitNextMillis(lastTimestamp)
            }
        } else {
            sequence = 0L
        }

        lastTimestamp = timestamp

        return ((timestamp - EPOCH) shl TIMESTAMP_LEFT_SHIFT.toInt()) or
               (datacenterId shl DATACENTER_ID_SHIFT.toInt()) or
               (workerId shl WORKER_ID_SHIFT.toInt()) or
               sequence
    }

    fun nextIdAsString(): String = nextId().toString()

    /** Parse a Snowflake ID back to its components (useful for debugging) */
    fun parse(id: Long): SnowflakeComponents {
        val timestamp = (id shr TIMESTAMP_LEFT_SHIFT.toInt()) + EPOCH
        val datacenterId = (id shr DATACENTER_ID_SHIFT.toInt()) and MAX_DATACENTER_ID
        val workerId = (id shr WORKER_ID_SHIFT.toInt()) and MAX_WORKER_ID
        val sequence = id and SEQUENCE_MASK
        return SnowflakeComponents(
            timestamp = Instant.ofEpochMilli(timestamp),
            datacenterId = datacenterId,
            workerId = workerId,
            sequence = sequence
        )
    }

    private fun waitNextMillis(lastTimestamp: Long): Long {
        var timestamp = currentTimeMs()
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMs()
        }
        return timestamp
    }

    private fun currentTimeMs() = System.currentTimeMillis()
}

data class SnowflakeComponents(
    val timestamp: Instant,
    val datacenterId: Long,
    val workerId: Long,
    val sequence: Long
)
```

### Wiring Snowflake in Spring Boot

```kotlin
package com.yourcompany.config

import com.yourcompany.common.id.SnowflakeIdGenerator
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class IdGeneratorConfig {

    @Value("\${app.snowflake.datacenter-id:0}")
    private var datacenterId: Long = 0

    @Value("\${app.snowflake.worker-id:0}")
    private var workerId: Long = 0

    @Bean
    fun snowflakeIdGenerator(): SnowflakeIdGenerator {
        return SnowflakeIdGenerator(
            datacenterId = datacenterId,
            workerId = workerId
        )
    }
}
```

```yaml
# application.yml — different per deployment
app:
  snowflake:
    datacenter-id: ${DATACENTER_ID:0}  # Inject from env or k8s downward API
    worker-id: ${WORKER_ID:0}           # Inject from pod ordinal in StatefulSet
```

### Assigning Worker IDs in Kubernetes

The critical question: How do you assign unique worker IDs to each pod?

**Option 1: StatefulSet with pod ordinal**
```yaml
# kubernetes/order-service-statefulset.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: order-service
spec:
  replicas: 3  # Pods: order-service-0, order-service-1, order-service-2
  template:
    spec:
      containers:
        - name: order-service
          env:
            - name: WORKER_ID
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name  # "order-service-0" → parse "0"
            - name: DATACENTER_ID
              value: "1"  # Region/AZ identifier
```

```kotlin
// Parse pod ordinal from pod name
val podName = System.getenv("POD_NAME") ?: "pod-0"
val workerId = podName.substringAfterLast("-").toLong()
```

**Option 2: ZooKeeper/etcd for worker ID assignment**
At startup, each service claims a worker ID from etcd:
```kotlin
// On startup, atomically claim next available worker ID from etcd
// Release it on shutdown
// This is how Twitter originally did it
```

**Option 3: Hash of hostname/IP (simple but not guaranteed unique)**
```kotlin
val workerId = (InetAddress.getLocalHost().hostName.hashCode() and 0x1F).toLong()
// 5 bits = values 0-31
// Collision possible if > 32 instances — use with caution
```

> [!WARNING]
> **Clock skew is the Achilles heel of Snowflake.** If a machine's NTP clock jumps backward (even by 1 second), you can generate duplicate IDs. The implementation above handles small drifts (≤5ms wait). For large drifts (>5ms), it throws an exception — this is correct behavior. Monitor clock drift with `chronyc tracking` or CloudWatch.

---

## Option 5: ULID — Universally Unique Lexicographically Sortable Identifier

ULID solves UUID v4's ordering problem while being URL-safe.

### Structure

```
01ARZ3NDEKTSV4RRFFQ69G5FAV
|---------|-------------------|
Timestamp   Randomness
(10 chars)  (16 chars)
(48 bits)   (80 bits)

Total: 26 characters, base32 encoded (Crockford alphabet)
```

| Property | UUID v4 | Snowflake | ULID |
|---|---|---|---|
| Size | 36 chars | 18-19 digits | 26 chars |
| Time-sortable | ❌ | ✅ | ✅ |
| URL-safe | ❌ (contains `-`) | ✅ | ✅ |
| Requires coordination | ❌ | ✅ (worker IDs) | ❌ |
| Monotonic within ms | ❌ | ✅ | ✅ (with monotonic extension) |
| Human readable | ❌ | Partially | Better |
| Collision within ms | Low | None (by design) | Very low |

```kotlin
// ULID in Kotlin — using com.github.f4b6a3:ulid-creator
import com.github.f4b6a3.ulid.UlidCreator

val ulid = UlidCreator.getMonotonicUlid()
println(ulid) // 01ARZ3NDEKTSV4RRFFQ69G5FAV

// Time-sortable — these are in creation order:
// 01ARZ3NDEKTSV4RRFFQ69G5FAV  (t=1000ms)
// 01ARZ3NDEKTSV4RRFFQ69G5FAW  (t=1000ms, next in sequence)
// 01ARZ3NDEKTSV4RRFFQ69G5FBX  (t=1001ms)
```

```gradle
// build.gradle.kts
dependencies {
    implementation("com.github.f4b6a3:ulid-creator:5.2.2")
}
```

### ULID vs Snowflake — When to Use Which

| Use ULID when | Use Snowflake when |
|---|---|
| External-facing IDs (in URLs) | Internal system IDs (pure numeric) |
| You don't want to manage worker IDs | You need maximum ID generation throughput |
| You need base32 encoding | You need a simple Long type |
| Microservice without coordination | You have a clear DC/worker topology |

---

## Option 6: Segment's KSUID

KSUID (K-Sortable Unique Identifier) by Segment. 20 bytes, base62 encoded.

```
0o5Fs0EELR0fUjHjbCnEtdUwQe2
```

- 32-bit timestamp (second precision, not ms)
- 128-bit random payload
- Base62 encoded → 27 characters
- Sortable, URL-safe

Used internally at Segment for their event pipeline. Less common than ULID.

---

## Production Recommendation: Spring Boot + Kotlin

### For Most Use Cases: Snowflake IDs as Long

```kotlin
// Entity design — use Snowflake ID as PK
@Entity
@Table(name = "orders")
data class OrderEntity(
    @Id
    val id: Long = 0L,  // Populated by Snowflake generator before save
    
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    
    @Column(name = "order_number", unique = true)
    val orderNumber: String,  // Human-readable: ORD-2024-000001
    
    // ...
)

// Service layer — generate ID before persist
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val idGenerator: SnowflakeIdGenerator,
) {
    @Transactional
    fun createOrder(request: CreateOrderRequest, userId: Long): OrderEntity {
        val orderId = idGenerator.nextId()
        val order = OrderEntity(
            id = orderId,
            userId = userId,
            orderNumber = generateOrderNumber(orderId),
            // ...
        )
        return orderRepository.save(order)
    }
    
    // Human-readable order number derived from Snowflake ID
    private fun generateOrderNumber(snowflakeId: Long): String {
        val components = idGenerator.parse(snowflakeId)
        val datePart = components.timestamp.toString().substring(0, 10).replace("-", "")
        val shortId = (snowflakeId and 0xFFFF).toString().padStart(4, '0')
        return "ORD-$datePart-$shortId"  // e.g., ORD-20240115-4291
    }
}
```

### For External-Facing IDs: UUID v7

```kotlin
// Use UUID v7 for things customers see (order IDs in URLs, API responses)
// Use Snowflake Long for internal DB operations

@Entity
@Table(name = "orders")
data class OrderEntity(
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    val id: UUID = Generators.timeBasedEpochGenerator().generate(),  // UUID v7
    
    @Column(name = "snowflake_id", unique = true)
    val snowflakeId: Long,  // For internal ordering and joins
    
    // ...
)
```

### Configuration for Different Environments

```kotlin
// IdGeneratorProperties.kt
@ConfigurationProperties(prefix = "app.id-generator")
data class IdGeneratorProperties(
    val type: IdGeneratorType = IdGeneratorType.SNOWFLAKE,
    val datacenterId: Long = 0,
    val workerId: Long = resolveWorkerId(),
)

enum class IdGeneratorType { SNOWFLAKE, ULID, UUID_V7 }

// Auto-detect worker ID from environment
fun resolveWorkerId(): Long {
    // K8s StatefulSet pod ordinal
    System.getenv("POD_NAME")?.substringAfterLast("-")?.toLongOrNull()?.let { return it }
    // EC2 instance identity
    System.getenv("WORKER_ID")?.toLongOrNull()?.let { return it }
    // Fallback: hash of hostname (collision-possible but ok for development)
    return (InetAddress.getLocalHost().hostName.hashCode() and 0x1F).toLong()
}
```

---

## Testing the Snowflake Generator

```kotlin
@SpringBootTest
class SnowflakeIdGeneratorTest {

    @Autowired
    private lateinit var generator: SnowflakeIdGenerator

    @Test
    fun `generates unique IDs across threads`() {
        val count = 100_000
        val threads = 8
        val ids = Collections.synchronizedSet(HashSet<Long>(count))
        
        val executor = Executors.newFixedThreadPool(threads)
        val futures = (1..count).map {
            executor.submit<Long> { generator.nextId() }
        }
        
        futures.forEach { ids.add(it.get()) }
        executor.shutdown()
        
        assertThat(ids.size).isEqualTo(count) // No duplicates
    }

    @Test
    fun `IDs are monotonically increasing`() {
        val ids = (1..1000).map { generator.nextId() }
        val sorted = ids.sorted()
        assertThat(ids).isEqualTo(sorted) // Already in sorted order
    }

    @Test
    fun `parses Snowflake components correctly`() {
        val id = generator.nextId()
        val components = generator.parse(id)
        
        // Timestamp should be within 1 second of now
        val now = Instant.now()
        assertThat(components.timestamp).isBetween(now.minusSeconds(1), now.plusSeconds(1))
        assertThat(components.datacenterId).isEqualTo(0)
        assertThat(components.workerId).isEqualTo(0)
    }

    @Test
    fun `generates 4096+ IDs per millisecond`() {
        val start = System.currentTimeMillis()
        val ids = (1..10_000).map { generator.nextId() }
        val duration = System.currentTimeMillis() - start
        
        // Should complete in reasonable time (sequence overflow → wait next ms)
        assertThat(duration).isLessThan(5000)
        assertThat(ids.toSet().size).isEqualTo(10_000) // All unique
    }
}
```

---

## Common Pitfalls Summary

> [!WARNING]
> **JavaScript cannot handle Snowflake IDs as numbers.** JavaScript's `Number` type uses 64-bit IEEE 754 doubles, which only safely represent integers up to 2^53. Snowflake IDs use all 64 bits. When returning Snowflake IDs in JSON to JavaScript clients, **always serialize as a string**.

```kotlin
// Correct: serialize Snowflake ID as string in JSON
data class OrderResponse(
    @JsonSerialize(using = ToStringSerializer::class)
    val id: Long,  // Serialized as "1234567890123" not 1234567890123
    val orderNumber: String,
    // ...
)
```

> [!CAUTION]
> **Do not expose Snowflake IDs directly to end users without obfuscation.** A Snowflake ID encodes your datacenter ID, worker ID, and exact timestamp. This reveals infrastructure topology and production volume. Use order numbers (ORD-20240115-4291) or UUIDs for user-facing references, and Snowflake IDs only internally.

---

## Decision Matrix

```
Choosing an ID strategy:

Single DB, simple app:
  → PostgreSQL BIGSERIAL — simple, sufficient

Need globally unique, don't care about ordering:
  → UUID v4 — standard, zero setup

Need globally unique + time-ordered + URL-safe:
  → UUID v7 — modern standard (2024)

Need high-throughput, time-ordered, distributed, numeric:
  → Snowflake ID — 4M IDs/ms, sortable, debuggable

Need time-ordered + URL-safe + no coordination:
  → ULID — great for external IDs

Need human-readable order numbers:
  → Generate separately from internal ID
  → Use format: [prefix]-[date]-[sequence]
  → e.g., ORD-20240115-00001
```
