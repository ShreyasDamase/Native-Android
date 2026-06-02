# P0-3 — Database Landscape — When to Use PostgreSQL, Redis, Cassandra, MongoDB

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> You are not picking ONE database. You are assembling a persistence layer from purpose-built tools. Every senior engineer at Zepto, Swiggy, Stripe, and Uber runs a polyglot stack. The mistake junior engineers make is picking one database and bending it to do everything — then wondering why their system falls apart at 10x scale.

---

## 1. The Mental Model: Polyglot Persistence

Polyglot persistence is the architectural philosophy that **different data problems deserve different database solutions**. Just like you wouldn't use a hammer to tighten a screw, you don't use PostgreSQL to serve real-time pub/sub or Redis to store financial transactions.

A realistic production system at a company like Zepto (10-minute grocery delivery) uses:

| Layer | Database | Purpose |
|---|---|---|
| Core business data | PostgreSQL | Orders, users, payments, inventory |
| Session + Cache | Redis | Login sessions, product cache, rate limits |
| Search | Elasticsearch | Product search, autocomplete |
| Analytics / Events | Cassandra or ClickHouse | Order history, delivery events, time-series |
| Object storage | S3 / R2 | Product images, invoices |
| Message queue | Kafka / RabbitMQ | Order events, notifications |

> [!IMPORTANT]
> The decision of WHICH database to use is made at data-model design time, not after you have 10M records and things are slow. Retrofitting a database is one of the most expensive engineering operations you can do.

### Why Engineers Get This Wrong

The most common mistake is: **"MongoDB is flexible, let's just use that."**

Flexibility sounds good until:
- You try to enforce referential integrity between users and orders
- You run a financial audit and realize your data is inconsistent across documents
- You need a multi-step atomic transaction across two collections
- Your collection has 47 different shapes of documents because every developer added different fields

**Flexibility without schema enforcement = chaos at scale.**

The second most common mistake is: **"Redis is fast, let me store everything in Redis."**

Redis is in-memory. It is NOT designed to be a primary database:
- Persistence is eventual by default (RDB snapshots or AOF — not real-time)
- No complex query support
- Memory is expensive ($0.10/GB/hour on ElastiCache vs $0.02/GB/hour on RDS storage)
- Data loss risk if instance crashes between persistence cycles

---

## 2. PostgreSQL — Your Source of Truth

### 2.1 What PostgreSQL Is Built For

PostgreSQL is a **fully ACID-compliant relational database**. Every production system that deals with money, user accounts, inventory, or any data where correctness matters more than speed should have PostgreSQL as the source of truth.

ACID stands for:
- **Atomicity** — A transaction either fully commits or fully rolls back. You can never have half an order created.
- **Consistency** — The database always moves from one valid state to another. Constraints are enforced.
- **Isolation** — Concurrent transactions don't see each other's partial work (configurable isolation levels).
- **Durability** — Once committed, data survives crashes via the Write-Ahead Log (WAL).

### 2.2 The Write-Ahead Log (WAL) — How PostgreSQL Guarantees Durability

Every write in PostgreSQL is first written to the WAL **before** the actual data pages are modified. If the server crashes mid-write:

1. On restart, PostgreSQL replays the WAL from the last checkpoint
2. Uncommitted transactions are rolled back
3. Committed transactions that didn't make it to disk are replayed

This is why PostgreSQL data is **durable** — you don't lose committed data even on power failure.

> [!NOTE]
> The WAL is also the mechanism behind **streaming replication** (standby replicas) and **point-in-time recovery (PITR)** — critical for production backup strategy. AWS RDS leverages WAL for automated backups and read replicas.

### 2.3 JSONB — The Escape Hatch for Semi-Structured Data

PostgreSQL's `JSONB` column type lets you store JSON documents **with indexing support**. This is the right answer when:
- Your data is mostly relational but some fields are dynamic (e.g., product metadata varies by category)
- You don't want a separate MongoDB just for one semi-structured concern

```sql
-- Product table with structured core + JSONB for variable attributes
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    category_id UUID REFERENCES categories(id),
    attributes JSONB,  -- {"color": "red", "weight": "500g", "expiry": "2025-12"}
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Index inside JSONB for fast lookup by attribute
CREATE INDEX idx_products_color ON products USING GIN (attributes);

-- Query: find all red products under ₹200
SELECT * FROM products 
WHERE attributes @> '{"color": "red"}' 
AND price < 200;
```

> [!WARNING]
> Do NOT use JSONB as a way to avoid designing your schema. JSONB is for genuinely variable data (e.g., custom product specs). If you're putting `user_name`, `email`, `phone` into JSONB, you're just doing MongoDB in PostgreSQL — and losing all the type safety, constraint enforcement, and query optimization.

### 2.4 Indexes — The Most Impactful Performance Lever

PostgreSQL supports multiple index types. Choosing the wrong one wastes storage and CPU:

| Index Type | Best For | Example |
|---|---|---|
| B-Tree (default) | Equality, range, ORDER BY | `WHERE user_id = ?`, `WHERE price BETWEEN 100 AND 500` |
| GIN | JSONB, full-text search, arrays | `WHERE attributes @> '{}'` |
| GiST | Geometric data, ranges | `WHERE tsrange OVERLAPS ?` |
| BRIN | Large, append-only tables with natural ordering | Time-series, logs |
| Hash | Equality only, slightly faster than B-Tree | `WHERE email = ?` (pure lookup) |
| Partial | Subset of rows | `WHERE deleted_at IS NULL` |

```sql
-- Partial index: only index active orders (skips cancelled/delivered)
-- This is FAR more efficient than a full index if 90% of rows are completed
CREATE INDEX idx_active_orders ON orders (user_id, created_at)
WHERE status IN ('PENDING', 'CONFIRMED', 'OUT_FOR_DELIVERY');

-- Composite index: column ORDER matters! 
-- This serves: WHERE user_id = ? AND status = ?
-- But NOT: WHERE status = ? (without user_id)
CREATE INDEX idx_orders_user_status ON orders (user_id, status);
```

### 2.5 Row-Level Locking — Critical for Inventory Correctness

When two users simultaneously try to buy the last unit of an item (like Zepto flash sales), you need row-level locking to prevent overselling:

```sql
-- PESSIMISTIC LOCKING: lock the row for the duration of the transaction
BEGIN;
SELECT stock_quantity FROM inventory 
WHERE product_id = $1 
FOR UPDATE;  -- Locks this specific row — other transactions wait

-- If stock_quantity > 0, decrement
UPDATE inventory SET stock_quantity = stock_quantity - 1 
WHERE product_id = $1 AND stock_quantity > 0;

COMMIT;
```

```sql
-- OPTIMISTIC LOCKING: no locks, use version/timestamp to detect conflicts
UPDATE inventory 
SET stock_quantity = stock_quantity - 1, version = version + 1
WHERE product_id = $1 
AND stock_quantity > 0 
AND version = $2;  -- If version changed, UPDATE returns 0 rows → retry
-- Application checks rows_affected == 1, otherwise throws OptimisticLockException
```

> [!CAUTION]
> `SELECT FOR UPDATE` across multiple tables or long-running transactions causes **deadlocks**. Always acquire locks in a consistent order (e.g., always lock `inventory` before `order_items`) and keep transactions short. Deadlocks in production cause cascading timeouts.

### 2.6 Spring Boot Configuration for PostgreSQL

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sentinel_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      pool-name: SentinelHikariPool
      maximum-pool-size: 20         # Tune based on DB server connections
      minimum-idle: 5
      idle-timeout: 300000          # 5 minutes
      connection-timeout: 20000     # 20 seconds — fail fast
      max-lifetime: 1800000         # 30 minutes — recycle connections
      leak-detection-threshold: 60000  # Log if connection held > 60s
  jpa:
    hibernate:
      ddl-auto: validate            # NEVER use create/create-drop in production
    show-sql: false                 # Use slow query log instead
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 25            # Batch inserts for performance
        order_inserts: true
        order_updates: true
        generate_statistics: false  # Enable temporarily to debug N+1 queries
```

```kotlin
// build.gradle.kts dependencies
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")  // Schema migrations
}
```

---

## 3. Redis — Sub-Millisecond In-Memory Store

### 3.1 What Redis Actually Is

Redis is a **in-memory data structure server**. Everything lives in RAM. Operations are O(1) to O(log n). Response times are typically **< 1ms**. This is 100-1000x faster than a PostgreSQL query that hits disk.

Redis is NOT:
- A replacement for PostgreSQL
- A durable store for critical data (by default)
- A relational database
- Safe for data you cannot afford to lose

Redis IS:
- A cache layer that reduces DB load
- A session store for stateless authentication
- A rate limiter using atomic INCR operations
- A pub/sub message broker for real-time features
- A sorted set for leaderboards
- A Bloom filter (with RedisBloom module) for deduplication

### 3.2 Redis Data Structures and Use Cases

```
STRING: most common. Use for: cached JSON, counters, feature flags
  SET user:42:profile "{\"name\":\"Shreyas\",\"city\":\"Pune\"}" EX 3600

HASH: field-value pairs inside one key. Use for: user sessions, object representation
  HSET session:abc123 userId 42 role ADMIN lastSeen 1719832000

LIST: ordered, allows duplicates. Use for: message queues, activity feeds
  LPUSH notifications:user:42 "Your order has been shipped"
  LRANGE notifications:user:42 0 9  -- get last 10 notifications

SET: unordered, unique. Use for: tracking unique visitors, tags
  SADD product:42:viewers user:1 user:2 user:3
  SCARD product:42:viewers  -- count unique viewers

SORTED SET (ZSET): scored set. Use for: leaderboards, priority queues
  ZADD leaderboard 9850 "user:42"
  ZREVRANGE leaderboard 0 9 WITHSCORES  -- top 10

STREAM: append-only log. Use for: event sourcing, message queues
  XADD orders * orderId abc123 userId 42 amount 499
```

### 3.3 Rate Limiting with Redis (Production-Grade)

```kotlin
@Service
class RateLimiterService(private val redisTemplate: StringRedisTemplate) {

    fun isAllowed(userId: Long, endpoint: String, maxRequests: Int, windowSeconds: Long): Boolean {
        val key = "ratelimit:$userId:$endpoint"
        val current = redisTemplate.opsForValue().increment(key) ?: 1L
        
        if (current == 1L) {
            // First request in this window — set expiry
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds))
        }
        
        return current <= maxRequests
    }
}

// Usage in controller
@GetMapping("/products")
fun getProducts(@RequestHeader("X-User-Id") userId: Long): ResponseEntity<List<ProductDto>> {
    if (!rateLimiterService.isAllowed(userId, "products", maxRequests = 100, windowSeconds = 60)) {
        throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded")
    }
    return ResponseEntity.ok(productService.getAllProducts())
}
```

> [!WARNING]
> The above pattern has a **race condition** between `INCR` and `EXPIRE` — if the server crashes between those two calls, the key has no TTL and the user is permanently rate-limited. Use a Lua script to make it atomic:

```kotlin
fun isAllowedAtomic(userId: Long, endpoint: String, maxRequests: Int, windowSeconds: Long): Boolean {
    val script = """
        local key = KEYS[1]
        local limit = tonumber(ARGV[1])
        local window = tonumber(ARGV[2])
        local current = redis.call('INCR', key)
        if current == 1 then
            redis.call('EXPIRE', key, window)
        end
        return current <= limit and 1 or 0
    """.trimIndent()
    
    val result = redisTemplate.execute(
        RedisScript.of(script, Long::class.java),
        listOf("ratelimit:$userId:$endpoint"),
        maxRequests.toString(),
        windowSeconds.toString()
    )
    return result == 1L
}
```

### 3.4 Session Management with Redis

```yaml
# application.yml
spring:
  session:
    store-type: redis
    redis:
      flush-mode: on-save
      namespace: sentinel:session
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6379
      password: ${REDIS_PASSWORD:}
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2
          max-wait: 1000ms
      timeout: 2000ms
      connect-timeout: 1000ms
```

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.session:spring-session-data-redis")
    implementation("io.lettuce:lettuce-core")  // Netty-based, non-blocking
}
```

### 3.5 Redis Persistence Modes (Production Decision)

| Mode | Mechanism | Data Loss Risk | Performance Impact |
|---|---|---|---|
| No persistence | Data only in RAM | 100% on crash | Fastest |
| RDB snapshots | Full dump every N seconds | Up to N seconds | Low (background fork) |
| AOF (Append-Only File) | Log every write | Near zero | Medium (fsync overhead) |
| RDB + AOF | Both | Near zero | Medium |

> [!IMPORTANT]
> For production caching (non-critical data): **RDB snapshots only** — fast, low overhead.
> For Redis used as session store or rate limit store: **AOF with `fsync everysec`** — at most 1 second of data loss on crash.
> For Redis as primary store (inadvisable, but if forced): **AOF with `fsync always`** — zero data loss but 10-100x slower writes.

---

## 4. Apache Cassandra — Massive Write Throughput at Scale

### 4.1 What Cassandra Is Built For

Cassandra is a **distributed wide-column store** designed for:
- **Massive write throughput** — millions of writes per second across a cluster
- **No single point of failure** — every node is equal (no primary/replica distinction)
- **Linear scalability** — add nodes, capacity increases proportionally
- **Predictable latency** — writes are always fast regardless of data size
- **Geographic distribution** — multi-datacenter replication built-in

Cassandra was built at Facebook for the Inbox search feature. It's used at Netflix for viewing history, Uber for trip data, Discord for message storage.

### 4.2 The Data Model: Design for Your Queries

Cassandra has **no JOINs**. You design tables around your access patterns. This is the opposite of relational design.

In PostgreSQL: design for the data → derive queries  
In Cassandra: design for the queries → derive tables

```sql
-- Cassandra CQL: Order history by user, sorted by time
CREATE TABLE orders_by_user (
    user_id UUID,
    created_at TIMESTAMP,
    order_id UUID,
    total_amount DECIMAL,
    status TEXT,
    PRIMARY KEY (user_id, created_at, order_id)
) WITH CLUSTERING ORDER BY (created_at DESC);

-- This query is O(1) — hits exactly one partition
SELECT * FROM orders_by_user 
WHERE user_id = 550e8400-e29b-41d4-a716-446655440000 
LIMIT 20;
```

The `PRIMARY KEY (user_id, created_at, order_id)` means:
- `user_id` is the **partition key** — determines which node holds the data
- `created_at, order_id` are **clustering columns** — determine sort order within the partition

> [!WARNING]
> A Cassandra "partition" must fit in memory on one node. If a single `user_id` has 50 million orders, that partition is a **hot partition** and will crash your node. Bucket your partition keys: `(user_id, year_month)` instead of just `(user_id)`.

### 4.3 When NOT to Use Cassandra

Cassandra is the wrong choice for:

| Requirement | Why Cassandra Fails |
|---|---|
| JOINs between tables | Not supported — use PostgreSQL |
| Ad-hoc queries (unknown patterns) | You must pre-design tables per query |
| Strong ACID transactions | Only offers LWT (lightweight transactions) which is slow |
| Low-cardinality data (< 1M rows) | PostgreSQL handles this easily; Cassandra adds operational complexity |
| Frequent UPDATE of existing rows | Cassandra uses append-only writes; UPDATEs are implemented as tombstones + new writes → compaction overhead |
| Counting rows accurately | `SELECT COUNT(*)` requires a full table scan in Cassandra |

> [!CAUTION]
> **Never use Cassandra for financial data.** Cassandra's eventual consistency model means you can read stale data. For payment records, inventory, and balances — PostgreSQL with ACID is non-negotiable. Cassandra is for **analytical, append-heavy, read-by-known-key** workloads.

### 4.4 Spring Boot Configuration for Cassandra

```yaml
# application.yml
spring:
  cassandra:
    keyspace-name: sentinel_analytics
    contact-points: cassandra-node-1,cassandra-node-2,cassandra-node-3
    port: 9042
    local-datacenter: datacenter1
    username: ${CASSANDRA_USERNAME}
    password: ${CASSANDRA_PASSWORD}
    schema-action: none  # Never auto-create in production
    request:
      timeout: 5s
      consistency: LOCAL_QUORUM  # Reads + writes must confirm from majority of local DC
    connection:
      connect-timeout: 10s
      init-query-timeout: 10s
    pool:
      local:
        size: 4
```

```kotlin
// Cassandra entity for order events
@Table("order_events")
data class OrderEvent(
    @PrimaryKey
    val key: OrderEventKey,
    
    @Column("event_type")
    val eventType: String,
    
    @Column("payload")
    val payload: String,  // JSON blob
    
    @Column("created_at")
    val createdAt: Instant = Instant.now()
)

@PrimaryKeyClass
data class OrderEventKey(
    @PrimaryKeyColumn(name = "order_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    val orderId: UUID,
    
    @PrimaryKeyColumn(name = "event_time", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    val eventTime: Instant
) : Serializable

@Repository
interface OrderEventRepository : CassandraRepository<OrderEvent, OrderEventKey>
```

---

## 5. MongoDB — Document Store

### 5.1 Where MongoDB Actually Shines

MongoDB stores **BSON documents** (binary JSON) in collections. There is no fixed schema — each document can have different fields. This is genuinely useful when:

- **Content Management Systems**: blog posts, articles — each may have different metadata (tags, featured image, SEO fields, custom blocks)
- **Product catalogs with highly variable attributes**: an electronics product has `voltage`, `wattage`, `dimensions`; a clothing product has `size`, `color`, `material` — completely different attributes
- **Rapid prototyping**: schema evolution without migrations during early-stage development
- **Hierarchical data**: when data is naturally tree-structured and always accessed together
- **Event logs / audit trails**: append-only, no relationships needed

### 5.2 When MongoDB Hurts You

> [!WARNING]
> The following are production failure patterns that engineers discover only after 6-12 months of scale:

**1. Multi-document transactions are expensive**

MongoDB added multi-document transactions in v4.0, but they come with significant overhead — they use two-phase commit internally and are ~2-10x slower than single-document operations. If your use case requires frequent cross-document transactions, you're fighting the data model.

**2. Referential integrity is your problem now**

MongoDB has no foreign keys. Nothing stops you from deleting a `User` document that 10,000 `Order` documents reference. You now write application-level code to maintain integrity. At scale, this code has bugs, and you end up with orphaned documents that cause null pointer exceptions in production.

**3. Joins via `$lookup` are full collection scans**

MongoDB's `$lookup` (join) is not optimized like PostgreSQL's indexed join. For large collections, `$lookup` is slow. If you find yourself writing `$lookup` frequently, switch to PostgreSQL.

**4. Flexible schema becomes "nobody knows what's in this collection"**

Six months into development, your `products` collection has documents with `price` as String, as Number, as null, and missing entirely. Querying it consistently becomes impossible.

### 5.3 Spring Boot Configuration for MongoDB

```yaml
# application.yml
spring:
  data:
    mongodb:
      uri: mongodb+srv://${MONGO_USERNAME}:${MONGO_PASSWORD}@cluster0.mongodb.net/sentinel_cms?retryWrites=true&w=majority
      auto-index-creation: false  # Manage indexes explicitly in production
```

```kotlin
@Document(collection = "articles")
data class Article(
    @Id
    val id: ObjectId = ObjectId(),
    
    val title: String,
    val content: String,
    val authorId: String,  // Reference to PostgreSQL users table — integrity is YOUR job
    
    val metadata: Map<String, Any> = emptyMap(),  // Flexible: SEO, tags, custom fields
    
    @Indexed(expireAfterSeconds = 0)  // TTL index — MongoDB will auto-delete at `expiresAt`
    val expiresAt: Date? = null,
    
    val createdAt: Instant = Instant.now()
)

@Repository
interface ArticleRepository : MongoRepository<Article, ObjectId> {
    fun findByAuthorIdOrderByCreatedAtDesc(authorId: String, pageable: Pageable): Page<Article>
}
```

---

## 6. Decision Matrix — Which Database for Which Scenario

| Scenario | Primary DB | Why | Secondary |
|---|---|---|---|
| **User accounts, auth** | PostgreSQL | ACID, referential integrity, JOINs to orders | Redis (session cache) |
| **Order management** | PostgreSQL | Transactions, status updates, refund logic | Cassandra (event log) |
| **Inventory / stock** | PostgreSQL | Row-level locking, ACID, prevent overselling | Redis (hot item cache) |
| **User sessions** | Redis | Sub-ms reads, TTL-based expiry | — |
| **Product catalog** | PostgreSQL + JSONB | Core fields relational, attributes in JSONB | Elasticsearch (search) |
| **Product search** | Elasticsearch | Inverted index, fuzzy, facets, relevance | PostgreSQL (source of truth) |
| **Delivery tracking** | Cassandra | High write throughput, time-series, append-only | Redis (current location) |
| **Analytics / reports** | ClickHouse / Cassandra | Column-store, aggregation-optimized | — |
| **CMS / Blog** | MongoDB | Variable document structure, schema-free | — |
| **Rate limiting** | Redis | Atomic INCR, TTL, sub-ms | — |
| **Audit logs** | PostgreSQL (short-term) → S3/Cassandra (long-term) | ACID for recent, cheap storage for archive | — |
| **Leaderboards** | Redis ZSET | O(log n) sorted sets, in-memory ranking | — |
| **Notifications queue** | Redis List / Kafka | Fast enqueue, reliable delivery | — |
| **Geospatial (nearest store)** | PostgreSQL + PostGIS or Elasticsearch | Indexed geo queries | Redis geo (hot path) |

---

## 7. Real Cloud Pricing — The Cost Dimension

> [!NOTE]
> These are approximate AWS pricing (us-east-1, on-demand) as of 2025. Always check current pricing. The point is the **order of magnitude** differences.

### PostgreSQL (RDS)
- `db.t3.micro` (2 vCPU, 1 GB RAM): ~$15/month — dev/test only
- `db.t3.medium` (2 vCPU, 4 GB RAM): ~$55/month — small prod
- `db.r6g.xlarge` (4 vCPU, 32 GB RAM): ~$350/month — real workload
- Multi-AZ (high availability): **2x the instance price**
- Storage: $0.115/GB/month (gp3 SSD) + $0.20/GB/month for backups

### Redis (ElastiCache)
- `cache.t3.micro` (0.5 GB): ~$12/month
- `cache.r6g.large` (13 GB): ~$150/month
- Memory is the critical resource — plan your cache size carefully
- Redis Cluster (multiple nodes) multiplies cost proportionally

### Cassandra (Amazon Keyspaces — managed)
- $0.0004 per write unit (1 KB)
- $0.0004 per read unit (4 KB)
- $0.25/GB/month storage
- At 10M writes/day: ~$4/day = $120/month in write costs alone

### MongoDB (Atlas)
- M0 (free): 512 MB, development only
- M10 (2 GB RAM, 10 GB storage): ~$57/month
- M30 (8 GB RAM, 40 GB storage): ~$540/month
- Shared clusters NOT suitable for production

> [!CAUTION]
> Multi-database operational costs are not just money — they are **engineering time**. Each database requires expertise to tune, monitor, backup, and migrate. A two-person startup should start with PostgreSQL + Redis only. Add Cassandra and Elasticsearch only when you have concrete, measured bottlenecks that justify the complexity.

---

## 8. Operational Burden — What Nobody Talks About

### Backups
- **PostgreSQL**: automated daily snapshots (RDS) + WAL-based PITR. Test your restores quarterly.
- **Redis**: RDB snapshots are sufficient for cache. If using Redis for sessions — snapshot + AOF.
- **Cassandra**: `nodetool snapshot` per node, or use AWS Keyspaces point-in-time recovery.
- **MongoDB**: Atlas automated backups, or `mongodump` for self-hosted.

### Schema Migrations
- **PostgreSQL**: Use Flyway or Liquibase. Every schema change is versioned SQL. **Never alter a production table without a migration file.**
- **Cassandra**: `ALTER TABLE` is limited. Adding columns is safe. Changing types or removing columns is dangerous — requires data backups.
- **MongoDB**: No schema migrations by default. Use a migration library like `mongock` for production. Without it, schema drift is guaranteed.
- **Redis**: Keys are schema-less. You manage key naming conventions manually. Breaking changes require key namespace versioning.

### Replication

| Database | Replication Model | Failover |
|---|---|---|
| PostgreSQL | Primary + streaming replicas (async/sync) | Manual or Patroni for auto-failover |
| Redis | Master-Replica or Cluster | Redis Sentinel for auto-failover |
| Cassandra | Peer-to-peer, configurable RF (replication factor) | Automatic — no single point of failure |
| MongoDB | Replica Set (1 primary + N secondaries) | Automatic primary election |

> [!IMPORTANT]
> Always run with **at least 3 nodes** for Cassandra (RF=3) and **at least 3 nodes** for MongoDB replica sets. A 2-node setup cannot achieve quorum during a split-brain — you may end up with two primaries that accept conflicting writes.

---

## 9. Production Pitfall — The MongoDB Flexibility Trap

> This is a real pattern observed at multiple startups that scaled from 0 to 1M users.

**Month 1**: Team picks MongoDB. "It's flexible, we can iterate fast."  
**Month 3**: Collection has 12 different document shapes. No documentation.  
**Month 6**: New developer joins. Writes a query. Gets `NullPointerException` on fields that "should exist."  
**Month 9**: Team wants to run a financial report. `$lookup` across 3 collections takes 45 seconds.  
**Month 12**: Engineering decides to migrate to PostgreSQL. 3 months of migration work. Production downtime risk.

**The correct approach**:
1. Use MongoDB ONLY for genuinely schema-free data (CMS, user-generated content)
2. Even in MongoDB, enforce schemas using **JSON Schema Validation** at the collection level
3. For any data with relationships, transactions, or reporting needs — PostgreSQL from day 1

```javascript
// MongoDB: Enforce schema at collection level
db.createCollection("articles", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["title", "content", "authorId", "createdAt"],
      properties: {
        title: { bsonType: "string", maxLength: 300 },
        authorId: { bsonType: "string" },
        price: { bsonType: ["number", "null"] }  // Explicit null allowed
      }
    }
  }
})
```

---

## 10. Summary — Decision Rules You Can Follow Today

```
IF your data has relationships AND requires ACID → PostgreSQL
IF you need sub-millisecond reads AND can tolerate data loss → Redis  
IF you have > 10M writes/day AND time-series/append-only → Cassandra
IF your documents have genuinely variable schema AND no critical relationships → MongoDB
IF you need full-text search, facets, autocomplete → Elasticsearch (Chapter P0-4)

DEFAULT: Start with PostgreSQL. Add Redis for caching. 
         Add other databases ONLY when you have a measured, proven bottleneck.
```

> [!NOTE]
> The engineers who thrive in production are not the ones who know the most databases — they're the ones who know when NOT to add another database. Every database you add is a system you must monitor, backup, tune, and eventually migrate. Choose boring, proven technology until you can't.
