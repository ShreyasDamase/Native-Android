# Chapter 12 — Production Backend Architecture for Any App

### _How to design Kotlin Spring Boot systems for delivery, Uber-like, booking, marketplace, SaaS and AI apps_

---

## 12.1 The Goal

The older chapters use one specific app as the example. This chapter is the generic mental model. If you understand this, you can build many backend products:

- Food/grocery delivery: restaurants, menus, carts, orders, drivers, dispatch, payments, notifications.
- Uber-like ride app: riders, drivers, location stream, matching, pricing, trips, settlement.
- Booking app: inventory, availability, reservations, payments, cancellation, reminders.
- Marketplace: sellers, catalog, search, checkout, disputes, reviews.
- AI/NLP app: document ingestion, embeddings, vector search, LLM orchestration, audit logs.

The important idea: production apps are not "one database plus controllers". They are a set of data stores and workflows, each chosen for one job.

---

## 12.2 The Production Backend Map

```text
Mobile/Web Client
    |
API Gateway / Load Balancer
    |
Spring Boot API Services
    |
    |-- PostgreSQL: source of truth, transactions, money, bookings, users
    |-- Redis: cache, rate limits, sessions, locks, counters, temporary state
    |-- Elasticsearch: search, autocomplete, geo search, denormalized read index
    |-- Cassandra: very high-scale append/read patterns, location history, events
    |-- Kafka/RabbitMQ: async workflows, notifications, outbox events, retries
    |-- Object Storage: images, invoices, files, exports
    |-- Vector DB / Spring AI: semantic search, RAG, recommendations, NLP features
    |
Observability: logs, metrics, traces, alerts, dashboards
CI/CD: tests, migrations, build, deploy, rollback
```

Do not add every technology on day one. A strong first production version is usually:

```text
Spring Boot + PostgreSQL + Flyway + Redis + Testcontainers + Actuator + Docker
```

Then add Elasticsearch, Kafka, Cassandra, object storage, and AI only when the product need is real.

---

## 12.3 How to Choose the Right Storage

| Need | Best default | Why |
|---|---|---|
| Users, orders, payments, bookings | PostgreSQL | Transactions, constraints, joins, durability |
| Cart, OTP, session, rate limit | Redis | Very fast, temporary, TTL-based |
| Search restaurants, hotels, products | Elasticsearch | Full-text search, filters, scoring, autocomplete |
| Driver location history, sensor events | Cassandra | Huge write throughput, predictable queries |
| Background email/payment/dispatch | Kafka or RabbitMQ | Retryable async work |
| Images, PDFs, invoices | S3-compatible object storage | Files do not belong in relational tables |
| Semantic search, chat over docs | Vector store + Spring AI | Meaning-based retrieval |

Production mistake: using a technology because it sounds advanced. Industry-standard architecture means using fewer moving parts until the pressure is real.

---

## 12.4 Core Service Boundaries

For a large app, start as a modular monolith unless the team is already large enough to operate microservices.

```text
src/main/kotlin/com/company/app/
    AppApplication.kt
    common/
        config/
        error/
        security/
        observability/
    identity/
        controller/
        service/
        repository/
        model/
        dto/
    catalog/
    order/
    payment/
    notification/
    search/
    dispatch/
```

Each module owns its models, services, repositories, and API DTOs. Do not let every module freely reach into every other module's repository. Use service methods or domain events.

Bad:

```kotlin
class PaymentService(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val driverRepository: DriverRepository
)
```

Better:

```kotlin
class PaymentService(
    private val orderReadService: OrderReadService,
    private val paymentGateway: PaymentGateway,
    private val domainEventPublisher: DomainEventPublisher
)
```

The second design says: payment needs order facts, an external payment provider, and a way to publish results. It does not need to know the storage details of every domain.

---

## 12.5 Layers That Scale

```text
Controller -> Application Service -> Domain Model -> Repository/Gateway
```

Controller:

- HTTP request/response only.
- Uses DTOs.
- Handles validation annotations.
- Does not call multiple repositories.

Application service:

- Coordinates use case.
- Owns transactions.
- Calls repositories and external gateways.
- Publishes events.

Domain model:

- Encapsulates business rules.
- Prevents invalid state.
- Should not know HTTP or database details.

Repository/gateway:

- Repository talks to database.
- Gateway talks to payment provider, map API, email provider, LLM provider, etc.

Example:

```kotlin
@Service
class BookingService(
    private val availabilityRepository: AvailabilityRepository,
    private val bookingRepository: BookingRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    @Transactional
    fun reserve(command: ReserveSlotCommand): BookingResponse {
        val slot = availabilityRepository.findForUpdate(command.slotId)
            ?: throw NotFoundException("Slot not found")

        slot.reserveFor(command.userId)

        val booking = bookingRepository.save(
            Booking.confirmed(
                userId = command.userId,
                slotId = slot.id,
                amount = slot.price
            )
        )

        eventPublisher.publishEvent(BookingConfirmedEvent(booking.id))
        return BookingResponse.from(booking)
    }
}
```

Annotation explanation:

- `@Service`: registers the class as a Spring bean and communicates that it holds business/application logic.
- `@Transactional`: opens one database transaction for the method. If an exception escapes, Spring rolls back the transaction.
- `ApplicationEventPublisher`: useful inside a modular monolith. For distributed systems, combine events with the outbox pattern from Chapter 17.

---

## 12.6 Production Use Cases by App Type

### Delivery App

| Capability | Storage/workflow |
|---|---|
| User, restaurant, menu, order | PostgreSQL |
| Cart | Redis with TTL or PostgreSQL if checkout recovery matters |
| Restaurant/menu search | Elasticsearch |
| Driver live location | Redis GEO for current location; Cassandra for history |
| Dispatch assignment | Kafka/RabbitMQ worker or scheduled matching service |
| Payment | PostgreSQL transaction + idempotency key + gateway |
| Notifications | Queue + push/email/SMS gateway |

### Uber-like App

| Capability | Storage/workflow |
|---|---|
| Rider/driver profiles | PostgreSQL |
| Live driver location | Redis GEO or specialized geospatial store |
| Trip lifecycle | PostgreSQL |
| Location history | Cassandra |
| Matching | Redis sorted sets/geospatial + service logic |
| Surge pricing | Redis counters + Postgres pricing rules |
| Audit and settlement | PostgreSQL ledger tables |

### Booking App

| Capability | Storage/workflow |
|---|---|
| Inventory and reservations | PostgreSQL with locking |
| Availability search | Elasticsearch read model |
| Payment hold/capture | PostgreSQL + payment gateway |
| Reservation expiry | Redis TTL + scheduled reconciliation |
| Calendar sync | Async events |
| Cancellation/refunds | Saga workflow |

---

## 12.7 Cross-Cutting Concerns

Every production app needs:

- Authentication: JWT/resource server, OAuth2, or session depending on product.
- Authorization: roles, ownership checks, tenant isolation.
- Validation: input DTO annotations plus domain invariants.
- Idempotency: repeated payment/order requests should not double-charge.
- Rate limiting: protect login, OTP, search, checkout, AI endpoints.
- Migrations: Flyway/Liquibase, never `ddl-auto=update` in production.
- Observability: logs, metrics, traces, health checks.
- Resilience: timeouts, retries, circuit breakers, backoff.
- Testing: unit, slice, integration, Testcontainers.
- Secrets: environment variables or secret manager, never committed.

---

## 12.8 Dependencies for a Serious Kotlin Spring Boot Backend

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}
```

Add Cassandra, Kafka, RabbitMQ, Spring AI, or vector stores only when those chapters become relevant.

---

## 12.9 API Design Standards

Use nouns for resources:

```text
POST   /api/v1/bookings
GET    /api/v1/bookings/{bookingId}
POST   /api/v1/bookings/{bookingId}/cancel
GET    /api/v1/restaurants?query=pizza&lat=...&lng=...
POST   /api/v1/orders/{orderId}/payment-intents
```

Use commands for actions that are not simple CRUD:

```kotlin
data class CreateBookingRequest(
    @field:NotNull val slotId: UUID,
    @field:NotNull val paymentMethodId: UUID,
    @field:Size(max = 500) val note: String?
)
```

Annotation explanation:

- `@field:NotNull`: Kotlin places annotations on constructor parameters by default. Bean Validation needs field/getter targets. Use `@field:` with data class request DTOs.
- `@field:Size(max = 500)`: validates string length before service logic runs.
- Nullable `String?` means optional. Non-null `String` means the client must provide it.

---

## 12.10 Good Production Defaults

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
  jackson:
    default-property-inclusion: non_null
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      probes:
        enabled: true
  observations:
    annotations:
      enabled: true
```

Good practice:

- `ddl-auto: validate`: Hibernate checks that mappings match the database, but Flyway owns schema changes.
- `open-in-view: false`: prevents lazy database access during JSON serialization.
- Graceful shutdown: lets in-flight requests finish during deploys.
- Actuator probes: exposes health/readiness for containers.

---

## 12.11 Sources to Recheck

These docs change over time. Recheck them when starting a new project:

- Spring Boot data docs: https://docs.spring.io/spring-boot/reference/data/index.html
- Spring Boot SQL docs: https://docs.spring.io/spring-boot/reference/data/sql.html
- Spring Boot caching docs: https://docs.spring.io/spring-boot/reference/io/caching.html
- Spring Boot observability docs: https://docs.spring.io/spring-boot/reference/actuator/observability.html
- Spring Boot Testcontainers docs: https://docs.spring.io/spring-boot/reference/testing/testcontainers.html

