# Spring Boot Kotlin Mini Projects

This sub-track turns backend theory into real Kotlin + Spring Boot projects.

The goal is not to copy random GitHub code. The goal is to build original mini apps that teach the same production ideas you will see in serious backends: REST APIs, PostgreSQL schema design, transactions, validation, authentication, concurrency, caching, events, tests and clean code structure.

Reference baseline:

- Official Spring guide: building web applications with Spring Boot and Kotlin
- Official Spring Boot Kotlin reference
- Public parking and booking sample repositories can be used only for feature inspiration, not copy-paste code

## Recommended Order

1. [[MP-1 — Parking Lot Management System]]
2. [[MP-2 — Ticket Booking System]]
3. [[MP-3 — Food Ordering and Delivery System]]
4. [[MP-4 — Wallet and Ledger System]]

## Full Reference Codebases

Open these folders when you want to study actual Kotlin + Spring Boot code:

- [parking-lot](/Users/shreyasdamase/Sentinel/Native-Android-Notes/Backend%20Development/Springboot/Mini%20Projects/code/parking-lot)
- [ticket-booking](/Users/shreyasdamase/Sentinel/Native-Android-Notes/Backend%20Development/Springboot/Mini%20Projects/code/ticket-booking)
- [food-ordering](/Users/shreyasdamase/Sentinel/Native-Android-Notes/Backend%20Development/Springboot/Mini%20Projects/code/food-ordering)
- [wallet-ledger](/Users/shreyasdamase/Sentinel/Native-Android-Notes/Backend%20Development/Springboot/Mini%20Projects/code/wallet-ledger)

## What These Projects Cover

| Project | Main Lesson | Backend Topics |
|---|---|---|
| Parking Lot | Resource allocation and state transitions | CRUD, transactions, optimistic/pessimistic locking, pricing rules |
| Ticket Booking | Seat reservation under concurrency | holds, expiry, idempotency, race-condition prevention |
| Food Ordering | End-to-end order lifecycle | auth, catalog, cart, checkout, order state machine, async events |
| Wallet Ledger | Financial correctness | double-entry ledger, immutable records, idempotency, auditability |

## Shared Tech Stack

Use the same stack for all projects unless a project says otherwise:

```text
Kotlin
Spring Boot 3
Spring Web
Spring Validation
Spring Data JPA
PostgreSQL
Flyway
Spring Security + JWT when auth is needed
Testcontainers for database integration tests
MockK or springmockk for unit tests
springdoc-openapi for Swagger UI
Docker Compose for local services
```

## Standard Codebase Shape

Use modular monolith structure first:

```text
src/main/kotlin/com/learning/app/
  AppApplication.kt

  common/
    config/
    error/
    security/
    web/
    time/

  feature_name/
    api/
    application/
    domain/
    infrastructure/
```

Meaning:

- `api`: controllers, request DTOs, response DTOs.
- `application`: use cases, commands, transaction boundaries.
- `domain`: business entities, value objects, rules, events.
- `infrastructure`: JPA repositories, external adapters, database-specific code.

## Project Completion Rule

A mini project is complete only when it has:

- Flyway migrations.
- Request validation.
- Global error handler.
- Clean DTOs, not exposed JPA entities.
- Unit tests for domain rules.
- Integration tests for main APIs.
- At least one concurrency or transaction test where the domain needs it.
- Swagger/OpenAPI documentation.
- `docker-compose.yml` for PostgreSQL.
- README with setup, API examples and learning notes.

## Learning Method

For each project:

1. Design the domain model first.
2. Write the database schema second.
3. Define APIs third.
4. Implement the simplest happy path.
5. Add validation and error handling.
6. Add transaction/concurrency correctness.
7. Add tests.
8. Add production polish: logs, metrics, Docker, README.

Do not jump directly to controllers. In backend systems, the controller is the door, not the house.
