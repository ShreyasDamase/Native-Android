# SD-2 — System Design: CAP Theorem

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> **Production Engineering Reference** — The CAP theorem is not academic trivia. It is the lens through which every distributed data storage decision must be evaluated. Every time you choose a database, you are implicitly making a CAP trade-off. This chapter makes that trade-off explicit.

---

## The CAP Theorem — Plain Production Language

Eric Brewer's CAP theorem (2000) states that in a distributed system, you can guarantee at most **two** of the following three properties simultaneously:

| Property | What it means in practice |
|---|---|
| **C — Consistency** | Every read receives the most recent write, or an error. All nodes see the same data at the same time. |
| **A — Availability** | Every request receives a response (not necessarily the most recent data). System never refuses to answer. |
| **P — Partition Tolerance** | The system continues to operate even when network packets between nodes are dropped or delayed. |

### The Critical Insight: P is Not Optional

> [!IMPORTANT]
> Network partitions **will happen**. AWS us-east-1 has had inter-AZ packet loss. Your Kubernetes nodes will lose connectivity. Your cross-DC links will degrade. Assuming P is not a real-world option means you're building for a fantasy network.

This means **every distributed system must tolerate partitions**. The real choice is always:

**CP or AP?** — When a network partition occurs, do you sacrifice Consistency or Availability?

---

## CP Systems — Consistency over Availability

During a network partition, a CP system will **refuse to respond** (return an error) rather than return potentially stale data.

### ZooKeeper (CP)

ZooKeeper is the reference CP system. Used by Kafka (for old leader election), HBase, and Hadoop.

**How it works:**
- Uses the **ZAB protocol** (ZooKeeper Atomic Broadcast) — a variant of Paxos
- A write is only acknowledged after a **quorum** (majority of nodes) confirms it
- If quorum cannot be reached (partition), the minority partition **refuses writes and reads**

**Real behavior during partition:**
```
Cluster: 3 ZooKeeper nodes (A, B, C)
Partition: C is isolated from A and B

A + B (majority):  → Continue serving reads/writes (quorum = 2/3)
C (minority):      → REFUSES ALL REQUESTS (returns error)
                     clients connecting to C will see: "not currently serving requests"
```

**Where ZooKeeper is used in production:**
- **Kafka 2.x**: Leader election for partition brokers (being replaced by KRaft)
- **HBase**: Region server coordination
- **Hadoop**: NameNode HA failover
- **Distributed locks** (though Etcd is now preferred)

---

### Etcd (CP)

The backbone of Kubernetes. Stores all cluster state (pods, services, secrets, configmaps).

**Why it MUST be CP:**
- If two API servers see different cluster states, Kubernetes might schedule the same pod twice on two nodes
- Correctness is non-negotiable for infrastructure control planes

**Raft consensus:**
```
Leader election: One node becomes leader
Write path:
  1. Client writes to leader
  2. Leader replicates to followers
  3. Majority ACK → Leader commits → ACK to client
  4. If leader dies: elect new leader from committed log
```

**Behavior during partition:**
```
3-node etcd cluster, partition isolates 1 node
→ Minority node (1): refuses reads and writes
→ Majority (2):      continues with quorum
→ Clients on minority: get connection errors, must retry against majority
```

> [!WARNING]
> If you run a 2-node etcd cluster, you have zero partition tolerance — one failure = no quorum. Always use odd numbers: 3, 5, or 7 nodes. 3 is sufficient for most; 5 if you need to survive 2 simultaneous failures.

---

### HBase (CP)

HBase is built on HDFS and provides strong row-level consistency. Used by:
- **Facebook Messages** (before they migrated away)
- **Alibaba** for financial data
- Any system needing consistent writes at massive scale

HBase achieves consistency through:
- Single **RegionServer** owns each row range (region)
- Writes go to WAL (Write Ahead Log) + MemStore
- ZooKeeper coordinates RegionServer assignment
- During a RegionServer failure: that region is **unavailable** until reassigned

---

## AP Systems — Availability over Consistency

During a network partition, an AP system will **keep responding with potentially stale data** rather than return an error.

### Cassandra (AP / Tunable)

Cassandra is the canonical AP system. Used by Netflix, Instagram, Discord, and Apple.

**Core mechanism: Consistent Hashing + Replication Factor**
```
Replication Factor (RF) = 3 → Each row exists on 3 nodes
Write Consistency Level:
  - ALL:    Must ACK from all 3 nodes (CP-like, high latency)
  - QUORUM: Must ACK from 2/3 nodes (balanced)
  - ONE:    ACK from 1 node (AP, lowest latency, highest availability)
```

**During partition:**
```
RF=3 cluster, CL=ONE
Partition isolates Node C from A and B

Client writes to A: 
  → A writes locally → ACK to client ✓
  → A tries to replicate to C → FAILS (partition)
  → A stores "hint" for C (hinted handoff)
  → When partition heals, A replays hints to C

Client reads from C (before healing):
  → Returns stale data (C missed the write)
  → This is the price of AP
```

**Cassandra's conflict resolution: Last Write Wins (LWW)**
- Every write carries a timestamp
- When two conflicting versions exist: highest timestamp wins
- Problem: clock skew between nodes can cause newer writes to lose

> [!CAUTION]
> Cassandra's LWW + clock skew is a real production bug. If two nodes have 50ms clock drift and you're doing concurrent updates, you will silently lose writes. Always use NTP (or AWS Time Sync Service) and keep drift < 1ms in production Cassandra clusters.

---

### DynamoDB (AP / Tunable)

AWS DynamoDB defaults to **eventually consistent reads** but offers **strongly consistent reads** as an option (at double the cost in read capacity units).

```kotlin
// Eventually consistent read (default, cheaper)
val request = GetItemRequest {
    tableName = "Orders"
    key = mapOf("orderId" to AttributeValue { s = orderId })
    // consistentRead = false (default)
}

// Strongly consistent read (2x RCU, but guaranteed fresh)
val requestStrong = GetItemRequest {
    tableName = "Orders"
    key = mapOf("orderId" to AttributeValue { s = orderId })
    consistentRead = true
}
```

**When to use each in practice:**
| Operation | Consistency Level | Why |
|---|---|---|
| Display product listing | Eventually consistent | Stale by 100ms is fine |
| Check order status | Eventually consistent | Usually fine |
| Read inventory before purchase | Strongly consistent | Must be accurate |
| User balance check | Strongly consistent | Financial data |
| Social feed | Eventually consistent | Milliseconds don't matter |

---

### CouchDB (AP)

CouchDB uses **Multi-Version Concurrency Control (MVCC)** and allows conflict detection (unlike Cassandra which silently resolves). When two nodes have conflicting revisions, both are preserved — application must resolve.

Used heavily in **offline-first applications** (e.g., mobile apps that sync when back online).

---

## Where Does PostgreSQL Sit?

> [!NOTE]
> Technically, PostgreSQL is a **CA system** — it guarantees Consistency and Availability, but it does NOT tolerate partitions in a distributed sense. This is because a single-node PostgreSQL is not "distributed" — it has no partition to tolerate.

When you run PostgreSQL with a streaming replica:
- You get **one writer** (the primary)
- If the primary is partitioned away from replicas, **the replicas stop receiving updates** (they become stale)
- There is no automatic failover that preserves consistency without a coordination layer (like Patroni + etcd)

**PostgreSQL HA Stack:**
```
[Primary PG] ←→ [Streaming Replication] ←→ [Replica PG]
       ↑
[Patroni] (etcd-backed) → Manages leader election
       ↑
[HAProxy] → Routes writes to current primary
```

With Patroni:
- etcd holds the "who is primary" lock
- If primary dies, Patroni runs leader election via etcd
- **During failover (typically 10-30 seconds):** writes fail — you pay a consistency price for HA
- This effectively makes PostgreSQL HA more **CP** when used with Patroni

---

## Eventual Consistency in Practice

Eventual consistency doesn't mean "wrong data forever." It means:

> All replicas will converge to the same value **given enough time** and no new updates.

In practice, this convergence happens in **milliseconds to seconds** under normal network conditions.

### Shopping Cart: Perfect AP Use Case

Amazon's famous 2007 Dynamo paper described the shopping cart. Here's why AP is correct for carts:

**The failure mode if you chose CP:**
- User adds item to cart
- Network partition occurs
- CP system refuses the write → user sees an error
- User closes app thinking their cart is empty → LOST SALE

**The failure mode with AP:**
- User adds item on one device during partition
- Another device also modifies cart
- After partition heals: **merge both carts** (union of items)
- User sees more items than expected → minor surprise, not a disaster

```
AP Cart Merge Strategy:
Device A adds: [milk, bread]
Device B adds: [eggs, milk] (during partition)
After merge:   [milk, bread, eggs]  ← union, deduped

Acceptable outcome: Yes
Unacceptable outcome (CP): User sees "Error: cart unavailable"
```

---

### Social Media Feed: AP Use Case

When Shreyas posts a photo on Instagram:
- Photo is written to one region
- Followers in other regions may see the post 500ms - 2s later
- **This is acceptable** — nobody checks timestamps that precisely

If Instagram were CP:
- Any regional partition → nobody can post or read feeds
- Instagram would be down in South Asia every time the Mumbai region has packet loss

---

## Strong Consistency Requirements: Financial Systems

> [!CAUTION]
> Never use an AP system as your primary data store for financial transactions. The consequences of stale reads are regulatory violations and financial losses — not just "slightly wrong data."

### Inventory: The Overselling Problem

Zepto promises items are in stock. If inventory uses eventual consistency:

```
Scenario: 1 unit of product X in stock

Node A reads: qty=1 → allows purchase → decrements to 0
Node B reads: qty=1 (stale!) → allows purchase → decrements to 0

Result: -1 inventory. Two customers paid for 1 item.
```

**Correct approach:**
```sql
-- Atomic check-and-decrement with PostgreSQL advisory lock or FOR UPDATE
BEGIN;
SELECT quantity 
FROM inventory 
WHERE product_id = 'X' AND warehouse_id = 'W1'
FOR UPDATE;  -- exclusive row lock

-- If quantity > 0:
UPDATE inventory 
SET quantity = quantity - 1,
    quantity_reserved = quantity_reserved + 1
WHERE product_id = 'X' AND warehouse_id = 'W1';

COMMIT;
```

This is CP behavior — only one transaction wins, others wait or fail.

---

### Financial Ledger: Double-Entry Accounting

```sql
-- This MUST be atomic. AP is not acceptable.
BEGIN;

INSERT INTO ledger_entries (account_id, amount, type, reference_id)
VALUES 
  ('user_wallet_123', -500.00, 'DEBIT', 'order_abc'),
  ('company_revenue', +500.00, 'CREDIT', 'order_abc');

-- Verify balance never goes negative
SELECT balance FROM accounts WHERE id = 'user_wallet_123' FOR UPDATE;
-- If balance < 0: ROLLBACK
COMMIT;
```

At Stripe, every financial operation is backed by PostgreSQL with strong consistency. There is no "eventually consistent" in a double-entry ledger. Every debit must match a credit.

---

## How Kafka Relates to CAP

Kafka is often misunderstood in the context of CAP.

**Kafka is a CP system for partition leadership:**
- Each Kafka partition has one leader and N replicas
- `min.insync.replicas` (ISR) controls how many replicas must ACK a write
- If ISR < `min.insync.replicas`, Kafka **refuses writes** (CP behavior)

```yaml
# Kafka topic config for financial events — CP behavior
min.insync.replicas: 2  # At least 2 replicas must be in sync
acks: all               # Producer must wait for all ISR replicas to ACK
replication.factor: 3   # 3 copies total
```

```yaml
# Kafka topic config for analytics events — AP behavior
min.insync.replicas: 1  # Only leader needs to confirm
acks: 1                 # Producer waits only for leader ACK
replication.factor: 2   # 2 copies
```

> [!IMPORTANT]
> For payment events, order events, and inventory events — **use `acks=all` and `min.insync.replicas=2`**. Losing a financial event from Kafka is a data integrity disaster. For click events and analytics, `acks=1` is fine.

**Kafka Consumer Consistency:**

```kotlin
// At-most-once (AP, can lose messages)
consumer.commitSync() // before processing
processMessage(record)

// At-least-once (AP, can duplicate)
processMessage(record)
consumer.commitSync() // after processing

// Exactly-once (CP-like, requires idempotent consumers)
// Use Kafka Transactions + idempotent producers
producer.beginTransaction()
producer.send(ProducerRecord("output-topic", key, value))
consumer.commitSync(offsets, producerGroupMetadata) // transactional
producer.commitTransaction()
```

---

## PACELC — The Extension of CAP

CAP only describes behavior during **partitions**. PACELC extends this:

> If there is a Partition (P), choose between Availability (A) or Consistency (C). Else (E), choose between Latency (L) or Consistency (C).

| System | Partition | Else |
|---|---|---|
| DynamoDB | AP | EL (low latency, eventual consistency) |
| Cassandra | AP | EL |
| PostgreSQL | CP | EC (higher latency for consistency) |
| MongoDB | CP (default) | EC |
| Spanner (Google) | CP | EC |

This matters because **most of the time there is no partition**. The normal operating mode trade-off is latency vs consistency.

---

## Real Design Decisions Your Company Will Face

### Scenario 1: User Profile Service

**Question:** User updates their display name. Must all services see it immediately?

**Answer:** No. Use AP (Cassandra or DynamoDB). Accept 500ms - 1s lag across regions. The downside of a stale name display is negligible.

---

### Scenario 2: OTP Verification

**Question:** User enters OTP. Must the OTP check be consistent?

**Answer:** Yes. Use Redis (single writer) or PostgreSQL. If the OTP is stored on Node A and the verify call hits Node B (which has stale data), the OTP check fails. This is a horrible UX and a security risk.

```kotlin
// Correct: Redis SETNX for OTP — atomic, consistent
@Service
class OtpService(private val redisTemplate: StringRedisTemplate) {
    
    fun storeOtp(phone: String, otp: String) {
        val key = "otp:$phone"
        redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(5))
    }
    
    fun verifyOtp(phone: String, inputOtp: String): Boolean {
        val key = "otp:$phone"
        val storedOtp = redisTemplate.opsForValue().get(key) ?: return false
        if (storedOtp == inputOtp) {
            redisTemplate.delete(key) // Invalidate after use
            return true
        }
        return false
    }
}
```

---

### Scenario 3: Product Catalog

**Question:** Product price is updated. All users see the new price?

**Answer:** AP with TTL-based cache is fine. Cache the price for 60 seconds in Redis. Users may see old price for up to 60 seconds. This is acceptable for most e-commerce. For flash sales, use shorter TTL or event-driven cache invalidation.

---

### Scenario 4: Flash Sale Inventory

**Question:** 100 units of iPhone at ₹1. 10,000 concurrent users hit "Buy Now."

**Answer:** **Must be CP**. Use Redis `DECR` for atomic decrement (single-writer), or PostgreSQL `FOR UPDATE`. Any AP approach will oversell.

```kotlin
// Redis atomic decrement for flash sale inventory
fun reserveInventoryForFlashSale(productId: String, quantity: Int): Boolean {
    val key = "flash_inventory:$productId"
    val lua = """
        local current = redis.call('GET', KEYS[1])
        if current == false then return -1 end
        if tonumber(current) < tonumber(ARGV[1]) then return 0 end
        return redis.call('DECRBY', KEYS[1], ARGV[1])
    """.trimIndent()
    
    val result = redisTemplate.execute(
        RedisScript.of(lua, Long::class.java),
        listOf(key),
        quantity.toString()
    ) ?: -1L
    
    return result >= 0 // success if result >= 0
}
```

---

## CAP Decision Framework

```
CHOOSING YOUR DATABASE:

1. Is the data financial? (payments, balances, ledger)
   → MUST be CP → PostgreSQL + proper transactions

2. Is the data inventory for purchasing?
   → MUST be CP → PostgreSQL or Redis with atomic operations

3. Is the data user-facing but not financial? (profiles, preferences)
   → Can be AP → Cassandra, DynamoDB

4. Is the data time-series? (events, logs, metrics)
   → AP is fine → Cassandra, ClickHouse, TimescaleDB

5. Is the data a distributed lock or leader election?
   → MUST be CP → etcd, ZooKeeper, Redis (with RedLock caution)

6. Is the data session/auth tokens?
   → CP preferred → Redis (single primary), but replication lag is usually OK

7. Is the data a newsfeed or social graph?
   → AP → Cassandra, DynamoDB

8. Is the data search indices?
   → AP → Elasticsearch (eventually consistent)
```

---

## Summary Table

| System | CAP | Use When | Avoid When |
|---|---|---|---|
| PostgreSQL (single node) | CA | Financial data, inventory, transactions | Massive global scale across regions |
| PostgreSQL + Patroni | CP | HA financial data | Sub-millisecond write latency required |
| Cassandra | AP (tunable) | User timelines, logs, analytics | Financial transactions requiring atomicity |
| DynamoDB | AP (tunable) | Serverless, variable load, AWS-native | Need complex queries, joins |
| Redis (single primary) | CP (effectively) | OTP, sessions, rate limiting, locks | Persistence-critical primary store |
| Redis Cluster | AP (partition resilient) | Distributed cache, sessions at scale | True strong consistency |
| etcd | CP | Distributed locks, config, leader election | High-throughput data store |
| ZooKeeper | CP | Legacy distributed coordination | New greenfield — use etcd instead |
| Kafka | CP (for writes) | Event streaming, audit logs, integration | Simple job queues (use Redis instead) |
| Elasticsearch | AP | Full-text search, log analytics | Primary source of truth for any data |

> [!NOTE]
> Most production systems use MULTIPLE databases — each chosen for its specific CAP characteristics. PostgreSQL for financial data, Cassandra for timelines, Redis for sessions, Elasticsearch for search. This is called **polyglot persistence**. The skill is knowing which trade-off to make for each domain.
