# Chapter 19 — Observability, Performance and Production Readiness

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

### _The difference between "it runs" and "we can operate it in production"_

---

## 19.1 Production-Ready Means Observable

You cannot fix what you cannot see.

Every production backend needs:

- Health checks.
- Metrics.
- Structured logs.
- Distributed traces.
- Error reporting.
- Alerts.
- Dashboards.
- Slow query visibility.
- Deployment/rollback history.

Spring Boot Actuator and Micrometer are the normal starting point.

---

## 19.2 Actuator Setup

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
}
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,loggers
  endpoint:
    health:
      probes:
        enabled: true
      show-details: when_authorized
  metrics:
    tags:
      application: backend-api
```

Endpoints:

- `/actuator/health`: basic health.
- `/actuator/health/readiness`: can receive traffic.
- `/actuator/health/liveness`: process is alive.
- `/actuator/prometheus`: scrape metrics.
- `/actuator/loggers`: inspect/change log levels if secured.

Good practice: expose only safe endpoints publicly. Put actuator behind internal network/auth.

---

## 19.3 Metrics That Matter

API:

- request count
- latency p50/p95/p99
- error rate
- status codes

Database:

- connection pool active/idle/pending
- slow queries
- deadlocks
- transaction duration

Redis:

- command latency
- cache hit ratio
- memory usage
- evictions

Elasticsearch:

- index latency
- search latency
- rejected requests
- cluster health

Kafka/RabbitMQ:

- consumer lag
- retries
- dead-letter count
- publish failures

AI:

- model latency
- token usage
- cost per endpoint
- retrieval hit rate
- no-answer rate

---

## 19.4 Tracing

Tracing follows a request across services:

```text
POST /orders
    -> OrderService.create
    -> PostgreSQL insert
    -> Outbox event
    -> Kafka publish
    -> Payment worker
    -> Payment provider
```

Add observations:

```kotlin
@Service
class PricingService {
    @Observed(name = "pricing.quote", contextualName = "calculate quote")
    fun quote(command: QuoteCommand): Quote {
        // pricing logic
    }
}
```

Annotation explanation:

- `@Observed`: tells Micrometer to create metrics/tracing observations around the method.
- `name`: stable metric/span name.
- `contextualName`: human-readable span label.

Enable annotation scanning:

```yaml
management:
  observations:
    annotations:
      enabled: true
```

---

## 19.5 Structured Logging

Bad log:

```text
Something failed
```

Good log:

```kotlin
logger.warn(
    "Payment authorization failed orderId={} paymentProvider={} reason={}",
    orderId,
    provider,
    reason
)
```

Always include:

- correlation/request id
- user id when safe
- tenant id
- business entity id
- provider name
- error code

Never log:

- passwords
- tokens
- OTPs
- full card details
- private documents
- sensitive model prompts unless explicitly approved

---

## 19.6 Performance Defaults

Database:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 2000
  jpa:
    open-in-view: false
```

HTTP client:

```kotlin
@Bean
fun restClient(builder: RestClient.Builder): RestClient {
    return builder
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build()
            )
        )
        .build()
}
```

Good practice:

- Every external call needs a timeout.
- Retries need backoff and max attempts.
- Do not retry non-idempotent operations blindly.
- Use pagination for list APIs.
- Avoid N+1 queries.
- Use database indexes based on actual query plans.

---

## 19.7 Testcontainers for Real Integration Tests

Test against real infrastructure where behavior matters.

```kotlin
@SpringBootTest
@Testcontainers
class BookingIntegrationTest {
    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
```

Use Testcontainers for:

- PostgreSQL migrations.
- Redis cache behavior.
- Elasticsearch indexing/search.
- Kafka consumer/producer tests.
- Cassandra repository tests.

---

## 19.8 Deployment Checklist

Before production:

- Flyway migrations run successfully.
- `ddl-auto=validate`.
- Secrets are outside git.
- Actuator health/readiness configured.
- Logs are structured.
- Metrics scraped.
- Alerts configured for error rate, latency, DB pool exhaustion, queue lag.
- Backups tested.
- Rollback plan exists.
- Payment and webhook paths are idempotent.
- Rate limits enabled on sensitive endpoints.
- Load test important flows.
- Security headers/CORS configured.
- Dependency vulnerabilities checked.

---

## 19.9 Source Links

- Spring Boot observability: https://docs.spring.io/spring-boot/reference/actuator/observability.html
- Spring Boot metrics: https://docs.spring.io/spring-boot/reference/actuator/metrics.html
- Spring Boot Actuator endpoints: https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
- Spring Boot Testcontainers: https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
## Book-Aligned Corrections: Actuator First

The book's production-readiness path starts with Spring Boot Actuator:

- Add `spring-boot-starter-actuator`.
- Configure endpoint exposure.
- Use `/actuator/health` for liveness/readiness style checks.
- Use `/actuator/metrics` and Prometheus integration for monitoring.
- Use `/actuator/conditions`, `/actuator/beans`, `/actuator/configprops`, and `/actuator/env` for debugging only in protected environments.
- Add custom health indicators for dependencies like RabbitMQ, Redis, and payment providers.

Correct order:

```text
Actuator
  -> Micrometer metrics
  -> Prometheus scrape
  -> Grafana dashboards
  -> tracing/log correlation
```

Never expose sensitive Actuator endpoints publicly.
