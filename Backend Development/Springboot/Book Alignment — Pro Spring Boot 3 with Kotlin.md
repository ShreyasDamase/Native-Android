# Book Alignment — Pro Spring Boot 3 with Kotlin

Source: `Backend Development/Pro_Spring_Boot_3_with_Kotlin_3rd_Edition_-_Peter_Spath.pdf`

This note records corrections and upgrades from the local book so the Spring Boot notes stay grounded in Spring Boot 3 + Kotlin instead of generic backend advice.

## Biggest Corrections

### 1. Spring Boot is mostly auto-configuration, not magic

The book repeatedly shows that Spring Boot configures infrastructure based on classpath dependencies and properties:

- `spring-boot-starter-web` triggers Spring MVC + embedded Tomcat defaults.
- JDBC/JPA starters trigger `DataSource` configuration.
- HikariCP is the default connection pool when present.
- Actuator endpoints appear when `spring-boot-starter-actuator` is present and configured.
- Docker Compose integration can auto-start local backing services in development.

Correction for notes: explain what dependency/property causes a behavior. Do not just say "Spring does it automatically."

### 2. Kotlin needs Spring/JPA compiler plugins

Kotlin classes are final by default. Spring often needs proxies for AOP, transactions, caching, security, and async behavior. The Kotlin Spring plugin opens Spring-annotated classes. JPA/Hibernate also needs no-arg construction support for entities.

Correct baseline:

```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}
```

Also keep:

```kotlin
implementation("org.jetbrains.kotlin:kotlin-reflect")
implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
```

### 3. `application.yml` / `application.properties` is central

The book uses properties for:

- Datasource configuration
- Profiles
- SQL initialization
- Docker Compose integration
- Actuator endpoints
- Security/user setup
- Service discovery/gateway config

Correction for notes: every production topic should mention which config properties control it.

### 4. Use profiles deliberately

The book emphasizes application profiles for different environments. Do not mix local H2, local Docker Compose, staging PostgreSQL, and production PostgreSQL into one config file.

Suggested structure:

```text
application.yml
application-local.yml
application-dev.yml
application-test.yml
application-prod.yml
```

### 5. Data access has layers: JDBC, Spring Data JDBC, JPA, REST, NoSQL

The book does not treat JPA as the only database path. It covers:

- Spring SQL data access
- JDBC / JdbcTemplate
- Spring Data JDBC
- Spring Data JPA
- Spring Data REST
- MongoDB
- Redis

Correction for notes: use JPA for rich relational domain modeling, but remember Spring Data JDBC can be simpler for aggregate-centric data without JPA lazy-loading complexity.

### 6. Docker Compose integration is a Spring Boot feature, not only external DevOps

Spring Boot 3.1+ can integrate with Docker Compose in development via `spring-boot-docker-compose`. The book uses this to auto-configure services like PostgreSQL based on `docker-compose.yaml`.

Correction for notes: Docker Compose can be both:

- A local development tool
- A Spring Boot development integration

But production still needs explicit environment/service configuration.

### 7. Testing should use slices, not only full `@SpringBootTest`

The book covers:

- `@SpringBootTest`
- `@WebMvcTest`
- `@DataJpaTest`
- `@DataMongoTest`
- `@JsonTest`
- `@WebFluxTest`
- MockMvc
- WebTestClient
- Testcontainers
- `@ServiceConnection`

Correction for notes: use full context tests sparingly. Use slice tests when testing one layer.

### 8. Security is a filter-chain architecture

The book anchors Spring Security around:

- `SecurityFilterChain`
- `HttpSecurity`
- Authentication
- Authorization
- `UserDetails`
- `PasswordEncoder` / `BCryptPasswordEncoder`
- OAuth2/social login
- CORS

Correction for notes: JWT is one REST API strategy, not "Spring Security itself." First understand filter chain, authentication provider, authorization rules, then add JWT.

### 9. Messaging is broader than Kafka

The book covers:

- Message-oriented middleware
- JMS
- AMQP/RabbitMQ
- Exchanges, bindings, queues
- `RabbitTemplate`
- Message converters
- WebSockets/STOMP
- RSocket

Correction for notes: Kafka is not the default answer to every async problem. RabbitMQ is often better for work queues, routing, and command-style async processing.

### 10. Actuator is the production-readiness foundation

The book's Actuator chapter covers:

- `/actuator/health`
- `/actuator/beans`
- `/actuator/conditions`
- `/actuator/configprops`
- `/actuator/env`
- Custom endpoints
- Health indicators
- JMX
- Metrics and observability

Correction for notes: Actuator should be introduced before Prometheus/Grafana. Prometheus scrapes metrics; Actuator exposes them.

### 11. Spring Cloud Gateway is reactive/WebFlux-based

The book's gateway chapter uses Spring Cloud Gateway. Gateway is not just "NGINX in Java." It is a programmable routing/filter layer in the Spring Cloud ecosystem and is WebFlux-based.

Correction for notes: distinguish:

- NGINX / load balancer / ingress: edge infrastructure
- Spring Cloud Gateway: application-level gateway with routes, predicates, filters, service discovery integration

### 12. Spring Modulith matters before microservices

The book includes Spring Modulith as a new-project topic. This is important for your delivery/Blinkit learning path: build a modular monolith first, verify module boundaries, then split services only when the domain demands it.

Correction for notes: microservices should not be the first implementation. Start modular.

## Corrected Spring Boot Learning Order

1. Spring Initializr and project structure
2. Gradle Kotlin DSL and plugins
3. `@SpringBootApplication`, component scanning, auto-configuration
4. Externalized config and profiles
5. Spring MVC controllers and validation
6. JDBC / Spring Data JDBC / JPA tradeoffs
7. Spring Data repositories and transactions
8. Security filter chain, then JWT/OAuth2
9. Testing slices and Testcontainers
10. Redis and NoSQL where appropriate
11. Messaging with RabbitMQ/JMS before Kafka scale assumptions
12. Actuator health/metrics/custom health indicators
13. Docker Compose integration
14. Spring Cloud Gateway and service discovery
15. Kubernetes only after the application architecture is stable
16. Spring Modulith to enforce module boundaries

## Notes To Keep Correct

Whenever updating a Spring Boot note, ask:

- Which starter dependency enables this?
- Which auto-configuration is involved?
- Which property controls it?
- Is this servlet MVC or reactive WebFlux?
- Is this a slice test or full-context test?
- Is this local dev config or production config?
- Does this need a proxy, transaction, cache, or async boundary?
- Is this truly a microservice concern, or just a module boundary?

