# Backend Development

This folder groups backend notes by technology and role in a production system.

## Tracks

[[Springboot/Index|Spring Boot + Kotlin]]

[[PostgreSQL/Index|PostgreSQL]]

[[DSA/Index|DSA for Backend Development]]

## How These Fit Together

For serious backend projects, use this default stack:

```text
Spring Boot + Kotlin -> API and business logic
PostgreSQL -> source-of-truth database
DSA -> logic, invariants, complexity, backend problem-solving
Redis -> cache, rate limits, temporary state
Elasticsearch -> search/read index
Kafka/RabbitMQ -> async workflows
Cassandra -> huge event/location history when needed
```

PostgreSQL is the main database you should learn deeply first. Most production backend systems need strong relational modeling, transactions, constraints and indexes before they need more exotic databases.
