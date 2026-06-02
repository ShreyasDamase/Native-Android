# Chapter 16 — Cassandra High-Scale Data Modeling

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

### _Massive writes, predictable reads and query-first modeling_

---

## 16.1 When Cassandra Makes Sense

Cassandra is not a better PostgreSQL. It is a different tool for different pressure:

- Extremely high write volume.
- Large append-only data.
- Multi-node horizontal scale.
- Predictable query patterns.
- Data spread across partitions.
- Tunable consistency.

Use Cassandra for:

- Driver location history.
- Trip telemetry.
- Clickstream events.
- Chat messages at very large scale.
- IoT/sensor events.
- Audit/event feeds that are queried by known keys.

Do not use Cassandra for:

- Payments.
- Relational joins.
- Complex ad hoc filtering.
- Strong transactional workflows.
- Booking inventory correctness.

---

## 16.2 Query-First Design

In PostgreSQL, you model entities first and write many queries later.

In Cassandra, you model each table around one query.

Question:

```text
Get the latest 100 locations for driver D on date 2026-05-31.
```

Table:

```sql
CREATE TABLE driver_location_by_day (
    driver_id uuid,
    day date,
    recorded_at timestamp,
    latitude double,
    longitude double,
    trip_id uuid,
    PRIMARY KEY ((driver_id, day), recorded_at)
) WITH CLUSTERING ORDER BY (recorded_at DESC);
```

Partition key:

```text
(driver_id, day)
```

Clustering key:

```text
recorded_at
```

This supports fast reads for one driver's day, ordered by time.

---

## 16.3 Spring Data Cassandra Dependencies

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-cassandra")
}
```

Configuration:

```yaml
spring:
  cassandra:
    contact-points: ${CASSANDRA_CONTACT_POINTS:localhost}
    port: ${CASSANDRA_PORT:9042}
    keyspace-name: app
    local-datacenter: ${CASSANDRA_LOCAL_DATACENTER:datacenter1}
    schema-action: none
```

Good practice: manage Cassandra schema outside application startup for production. Do not let the app casually mutate cluster schema.

---

## 16.4 Cassandra Entity

```kotlin
@Table("driver_location_by_day")
data class DriverLocationByDay(
    @PrimaryKey
    val key: DriverLocationKey,

    @Column("latitude")
    val latitude: Double,

    @Column("longitude")
    val longitude: Double,

    @Column("trip_id")
    val tripId: UUID?
)

@PrimaryKeyClass
data class DriverLocationKey(
    @PrimaryKeyColumn(name = "driver_id", type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    val driverId: UUID,

    @PrimaryKeyColumn(name = "day", type = PrimaryKeyType.PARTITIONED, ordinal = 1)
    val day: LocalDate,

    @PrimaryKeyColumn(name = "recorded_at", type = PrimaryKeyType.CLUSTERED, ordinal = 2)
    val recordedAt: Instant
) : Serializable
```

Annotation explanation:

- `@Table`: maps class to Cassandra table.
- `@PrimaryKeyClass`: groups partition and clustering columns.
- `PrimaryKeyType.PARTITIONED`: decides data distribution across the cluster.
- `PrimaryKeyType.CLUSTERED`: decides ordering inside a partition.
- `ordinal`: order of key columns.

---

## 16.5 Repository

```kotlin
interface DriverLocationRepository :
    CassandraRepository<DriverLocationByDay, DriverLocationKey> {

    fun findTop100ByKeyDriverIdAndKeyDayOrderByKeyRecordedAtDesc(
        driverId: UUID,
        day: LocalDate
    ): List<DriverLocationByDay>
}
```

Long method names are common but can become unreadable. For complex queries, use `CassandraTemplate`.

```kotlin
@Service
class DriverLocationHistoryService(
    private val cassandraTemplate: CassandraTemplate
) {
    fun latest(driverId: UUID, day: LocalDate, limit: Int): List<DriverLocationByDay> {
        val query = Query.query(
            Criteria.where("driver_id").`is`(driverId),
            Criteria.where("day").`is`(day)
        ).limit(limit)

        return cassandraTemplate.select(query, DriverLocationByDay::class.java)
    }
}
```

---

## 16.6 Bucketing and Hot Partitions

Bad partition:

```text
city_id = "mumbai"
```

If all Mumbai events go into one partition, that partition becomes too hot.

Better:

```text
(city_id, bucket_hour, shard)
```

Example:

```sql
CREATE TABLE delivery_events_by_city_hour (
    city_id uuid,
    bucket_hour timestamp,
    shard int,
    event_time timestamp,
    event_id uuid,
    event_type text,
    payload text,
    PRIMARY KEY ((city_id, bucket_hour, shard), event_time, event_id)
) WITH CLUSTERING ORDER BY (event_time DESC);
```

Choose shard using hash of event id or driver id.

---

## 16.7 Consistency

Cassandra lets you choose consistency per read/write. Common options:

- `ONE`: fastest, least strict.
- `QUORUM`: balanced correctness and availability.
- `LOCAL_QUORUM`: common in multi-datacenter setups.

For location history, eventual consistency is usually acceptable. For financial state, use PostgreSQL instead.

---

## 16.8 TTL and Time-Series Data

Cassandra supports TTL for automatic expiry:

```sql
INSERT INTO driver_location_by_day (...)
VALUES (...)
USING TTL 2592000;
```

Use TTL for location history or telemetry you only keep for 30/90/180 days.

Good practice:

- Set retention based on legal/product need.
- Avoid keeping infinite telemetry forever.
- Design compaction strategy for time-series workloads.

---

## 16.9 Cassandra Checklist

- Know every query before creating tables.
- Duplicate data intentionally for different queries.
- Keep partitions bounded.
- Avoid unbounded rows in one partition.
- Avoid secondary indexes as a replacement for real modeling.
- Avoid joins. Cassandra does not work like SQL.
- Use idempotent writes where possible.
- Test with realistic data volume, not 100 rows.

---

## 16.10 Source Links

- Spring Data Cassandra: https://docs.spring.io/spring-data/cassandra/reference/index.html
- Spring Boot data overview: https://docs.spring.io/spring-boot/reference/data/index.html

