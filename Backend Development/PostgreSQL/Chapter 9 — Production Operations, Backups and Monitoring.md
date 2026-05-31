# Chapter 9 — Production Operations, Backups and Monitoring

### _How to keep PostgreSQL healthy in production_

---

## 9.1 Production Checklist

Before production:

- automated backups enabled,
- restore tested,
- migrations tested,
- connection pool configured,
- slow query logging enabled,
- key queries indexed,
- disk alerts configured,
- CPU/memory alerts configured,
- replication/read replica plan understood,
- secrets outside git,
- `ddl-auto=validate`.

---

## 9.2 Backups

Backups are useless until restore is tested.

You need:

- daily backups,
- point-in-time recovery if possible,
- retention policy,
- restore drill,
- encrypted backup storage.

Ask your hosting provider:

```text
How do I restore to 10 minutes before accidental deletion?
How long does restore take?
Where are backups stored?
Are backups encrypted?
```

---

## 9.3 Monitoring

Watch:

- CPU,
- memory,
- disk,
- active connections,
- slow queries,
- locks,
- deadlocks,
- replication lag,
- cache hit ratio,
- index usage,
- table bloat.

Spring Boot also exposes datasource metrics through Actuator/Micrometer.

---

## 9.4 Connection Pool

Spring Boot commonly uses HikariCP.

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 2000
```

Too many connections can hurt PostgreSQL. Do not blindly set pool size to 200.

---

## 9.5 Slow Queries

Enable slow query logging in managed PostgreSQL if possible.

Investigate with:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT ...
```

Common fixes:

- add correct composite index,
- reduce selected columns,
- paginate,
- avoid N+1 queries,
- archive old data,
- denormalize into read model where appropriate.

