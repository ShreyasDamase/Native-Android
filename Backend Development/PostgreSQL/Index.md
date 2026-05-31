# PostgreSQL Notes

## Core Chapters

[[Chapter 1 — PostgreSQL vs SQL and Why It Is the Default Backend Database]]
[[Chapter 2 — Tables, Keys, Constraints and Schema Design]]
[[Chapter 3 — Relationships and ERD Modeling]]
[[Chapter 4 — Indexes and Query Performance]]
[[Chapter 5 — Transactions, Isolation and Locking]]
[[Chapter 6 — Flyway Migrations and Production Schema Changes]]
[[Chapter 7 — PostgreSQL with Spring Boot and Kotlin]]
[[Chapter 8 — JSONB, Full Text Search and PostGIS]]
[[Chapter 9 — Production Operations, Backups and Monitoring]]
[[Chapter 10 — SQL Query Types and Practical Examples]]

## Recommended Order

1. Start with Chapter 1 to understand why PostgreSQL is usually the best first database for Spring Boot production projects.
2. Read Chapter 2 for table design, primary keys, foreign keys, unique constraints and checks.
3. Read Chapter 3 for one-to-one, one-to-many, many-to-many and ERD design.
4. Read Chapter 4 before optimizing APIs; most slow backends are slow because of bad queries/indexes.
5. Read Chapter 5 before building booking, payments, inventory or driver assignment.
6. Read Chapter 6 before deploying schema changes to production.
7. Read Chapter 7 when wiring PostgreSQL with Spring Boot, JPA and Kotlin.
8. Read Chapter 8 when you need flexible metadata, search or location features.
9. Read Chapter 9 before calling your database production-ready.
10. Read Chapter 10 when you want to understand every common SQL query type, what it does, and when to use it.

## Rule of Thumb

SQL is the language. PostgreSQL is the database. For delivery apps, Uber-like apps, booking apps, marketplaces, payments and most Spring Boot backends, PostgreSQL should be your source of truth.
