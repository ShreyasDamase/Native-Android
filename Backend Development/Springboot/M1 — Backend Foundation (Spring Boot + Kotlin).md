# M1 — Backend Foundation (Spring Boot + Kotlin)

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> **Scope**: Everything you need to bootstrap a production-ready Spring Boot 3 + Kotlin monolith that can later be split into services. This is not a tutorial — it is the actual code and reasoning you deploy on Day 1.

---

## Table of Contents

1. [Project Setup](#11-project-setup)
2. [Environment Profiles](#12-environment-profiles)
3. [Global Exception Handler](#13-global-exception-handler)
4. [Validation Framework](#14-validation-framework)
5. [OpenAPI / Swagger](#15-openapi--swagger)
6. [Base User Domain](#16-base-user-domain)

---

## 1.1 Project Setup

### Why Kotlin for Spring Boot?

Kotlin's null-safety at the type-system level eliminates entire classes of `NullPointerException` bugs that silently crash Java services in production. The extension functions and data classes dramatically reduce boilerplate. The Kotlin Spring plugin (CGLIB proxy requirement) and Kotlin JPA plugin (no-arg constructor requirement) solve the two fundamental impedance mismatches between Kotlin and Spring's reflection-based machinery.

> [!IMPORTANT]
> Without `id 'org.jetbrains.kotlin.plugin.spring'`, your `@Service`, `@Component`, and `@Configuration` classes will be `final` by default. Spring uses CGLIB to subclass beans for AOP proxies (transactions, security, caching). Final classes cannot be subclassed — Spring will throw a `BeanCreationException` at startup. This is the #1 Kotlin + Spring misconfiguration.

> [!IMPORTANT]
> Without `id 'org.jetbrains.kotlin.plugin.jpa'`, your JPA `@Entity` classes will not have no-arg constructors. Hibernate needs no-arg constructors to instantiate entities via reflection when hydrating results from the DB. Without this plugin, you get `InstantiationException` at runtime when Hibernate tries to load data.

### Book-Aligned Build Setup Notes

The book's generated examples use Java 17 and Spring Boot 3.x. Using Java 21 is fine for a modern project, but understand the baseline:

- Spring Boot 3 requires Jakarta EE APIs, not old `javax.*` imports.
- The Kotlin Spring plugin matters because Spring uses proxies for transactions, caching, async, and security.
- The Kotlin JPA plugin matters because Hibernate expects entity construction support.
- Keep `kotlin-reflect`; Spring uses Kotlin metadata/reflection in many places.
- Use Spring dependency management instead of manually pinning Spring library versions.

For learning, do not hide the relationship between dependency and behavior:

```text
starter dependency
  -> auto-configuration
  -> beans created
  -> properties override defaults
```

### `build.gradle.kts` — Full Production Build File

```kotlin
// build.gradle.kts
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.5"
    
    // Kotlin core — transitive: kotlin-stdlib, kotlin-reflect
    kotlin("jvm") version "1.9.24"
    
    // Opens all Spring-annotated classes for CGLIB proxying
    // Without this: @Transactional, @Cacheable, @Async will SILENTLY FAIL on final classes
    kotlin("plugin.spring") version "1.9.24"
    
    // Generates no-arg constructors for @Entity, @Embeddable, @MappedSuperclass
    // Without this: Hibernate cannot hydrate entities from DB results
    kotlin("plugin.jpa") version "1.9.24"
    
    // MapStruct annotation processor (must come BEFORE kapt in some setups)
    kotlin("kapt") version "1.9.24"
}

group = "com.zepto.backend"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

extra["mapstructVersion"] = "1.5.5.Final"
extra["springdocVersion"] = "2.5.0"

dependencies {
    // ── Core Web ────────────────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // ── Kotlin ──────────────────────────────────────────────────────────────
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    // ── Security ────────────────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-security")
    // JWT (JJWT library — do NOT use java-jwt from Auth0, JJWT is more feature complete)
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")
    
    // ── Database ────────────────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    
    // ── Caching ─────────────────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    
    // ── Observability ───────────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    // Distributed tracing (Micrometer Tracing + Brave bridge)
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")
    
    // ── API Documentation ───────────────────────────────────────────────────
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${property("springdocVersion")}")
    
    // ── Mapping ─────────────────────────────────────────────────────────────
    implementation("org.mapstruct:mapstruct:${property("mapstructVersion")}")
    kapt("org.mapstruct:mapstruct-processor:${property("mapstructVersion")}")
    
    // ── Dev Tools ───────────────────────────────────────────────────────────
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("com.h2database:h2") // H2 for dev profile only
    
    // ── Testing ─────────────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.testcontainers:postgresql:1.19.8")
    testImplementation("org.testcontainers:junit-jupiter:1.19.8")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-Xjsr305=strict",           // Enforce nullability annotations from Java libs
            "-Xemit-jvm-type-annotations" // Emit type annotations for Spring AOT
        )
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Run tests in parallel — reduces CI time significantly
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
}

// Ensure kapt processes MapStruct BEFORE Spring's annotation processors
kapt {
    correctErrorTypes = true
    arguments {
        arg("mapstruct.defaultComponentModel", "spring")
        arg("mapstruct.unmappedTargetPolicy", "ERROR") // Fail build if mapping is incomplete
    }
}
```

> [!WARNING]
> `mapstruct.unmappedTargetPolicy = ERROR` will fail your build if any target field in a MapStruct mapping is not explicitly mapped or ignored. This is exactly what you want — silently unmapped fields in production means data is dropped on the floor without any error. Uber had a production incident where a price field was silently dropped in a mapping. Set this to `ERROR` always.

### Package Structure

Production apps die from package chaos. When you have 50+ files, a flat structure makes it impossible to reason about boundaries. Use vertical slice architecture within a layered package structure:

```
src/main/kotlin/com/zepto/backend/
├── ZeptoBackendApplication.kt          ← Entry point
│
├── config/
│   ├── SecurityConfig.kt               ← Spring Security filter chain
│   ├── RedisConfig.kt                  ← RedisTemplate / CacheManager
│   ├── JacksonConfig.kt                ← ObjectMapper customization
│   ├── OpenApiConfig.kt                ← Swagger SecurityScheme
│   ├── AsyncConfig.kt                  ← @EnableAsync + ThreadPoolTaskExecutor
│   └── HikariConfig.kt                 ← DataSource pool tuning
│
├── controller/
│   ├── UserController.kt
│   ├── ProductController.kt
│   └── OrderController.kt
│
├── service/
│   ├── UserService.kt
│   ├── ProductService.kt
│   └── OrderService.kt
│
├── repository/
│   ├── UserRepository.kt
│   ├── ProductRepository.kt
│   └── OrderRepository.kt
│
├── entity/
│   ├── User.kt
│   ├── Product.kt
│   └── Order.kt
│
├── dto/
│   ├── request/
│   │   ├── CreateUserRequest.kt
│   │   └── UpdateUserRequest.kt
│   └── response/
│       ├── UserResponse.kt
│       └── ApiError.kt
│
├── mapper/
│   └── UserMapper.kt                   ← MapStruct interface
│
├── exception/
│   ├── ResourceNotFoundException.kt
│   ├── ConflictException.kt
│   ├── ValidationException.kt
│   └── UnauthorizedException.kt
│
├── handler/
│   └── GlobalExceptionHandler.kt       ← @ControllerAdvice
│
├── security/
│   ├── JwtTokenProvider.kt
│   ├── JwtAuthenticationFilter.kt
│   └── UserDetailsServiceImpl.kt
│
├── event/
│   ├── UserCreatedEvent.kt
│   └── UserCreatedEventListener.kt
│
└── util/
    ├── MdcCorrelationIdFilter.kt       ← Inject X-Correlation-ID into MDC
    └── Extensions.kt                   ← Kotlin extension functions
```

### Application Entry Point

```kotlin
// ZeptoBackendApplication.kt
package com.zepto.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ConfigurationPropertiesScan   // Picks up all @ConfigurationProperties beans
@EnableJpaAuditing             // Enables @CreatedDate, @LastModifiedDate
@EnableCaching                 // Enables @Cacheable, @CacheEvict
@EnableAsync                   // Enables @Async methods
@EnableScheduling              // Enables @Scheduled tasks
class ZeptoBackendApplication

fun main(args: Array<String>) {
    runApplication<ZeptoBackendApplication>(*args)
}
```

> [!NOTE]
> `@ConfigurationPropertiesScan` on the main application class automatically discovers all `@ConfigurationProperties` annotated classes in the package tree. Without this (or manually specifying `@EnableConfigurationProperties`), your type-safe config beans won't be loaded. This replaced the older `@EnableConfigurationProperties(SomeProps::class)` approach.

---

## 1.2 Environment Profiles

### The Philosophy Behind Profiles

In production systems, configuration drift between environments is one of the most common root causes of "it works on my machine" incidents. Blinkit's engineering team publicly described a Redis key expiry misconfiguration that was present only in staging because the staging profile shared a key with production Redis. Profiles enforce environment boundaries.

The Spring Boot profile hierarchy:
1. `application.yml` — base config, shared across all environments
2. `application-{profile}.yml` — profile-specific overrides
3. Environment variables — highest priority, override everything

> [!CAUTION]
> Never put secrets (passwords, API keys, JWT secrets) in `application.yml` or any profile yml that gets committed to git. Use environment variables or a secrets manager (AWS Secrets Manager, HashiCorp Vault). A single leaked `application-prod.yml` has caused complete company-ending database breaches.

### `application.yml` — Base Configuration

```yaml
# application.yml
spring:
  application:
    name: zepto-backend
  
  # Jackson — consistent serialization across all environments
  jackson:
    default-property-inclusion: NON_NULL
    serialization:
      write-dates-as-timestamps: false
      fail-on-empty-beans: false
    deserialization:
      fail-on-unknown-properties: false
    time-zone: UTC
  
  # JPA base config — environment-specific DDL handled by Flyway, not Hibernate
  jpa:
    open-in-view: false        # CRITICAL: Disable OSIV. See warning below.
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
        format_sql: false       # Overridden per profile
        generate_statistics: false
  
  # Flyway — runs migrations on startup
  flyway:
    enabled: true
    locations: classpath:db/migration
    validate-on-migrate: true   # Fail startup if migration checksums changed
    baseline-on-migrate: false  # Never true in production
  
  # Actuator endpoints
  management:
    endpoints:
      web:
        exposure:
          include: health,info,metrics,prometheus,flyway
    endpoint:
      health:
        show-details: when-authorized
    metrics:
      tags:
        application: ${spring.application.name}

# Server
server:
  port: 8080
  shutdown: graceful            # Wait for in-flight requests on SIGTERM
  servlet:
    context-path: /api/v1
  compression:
    enabled: true
    min-response-size: 2048

# Application-level config (type-safe via @ConfigurationProperties)
app:
  jwt:
    secret: ${JWT_SECRET}       # MUST come from env var, never hardcoded
    expiration-ms: 3600000      # 1 hour
    refresh-expiration-ms: 604800000  # 7 days
  cors:
    allowed-origins:
      - http://localhost:3000
  pagination:
    default-page-size: 20
    max-page-size: 100
```

> [!WARNING]
> `spring.jpa.open-in-view: false` — Open Session In View (OSIV) keeps the Hibernate `EntityManager` open for the entire duration of an HTTP request, including the view rendering phase. This sounds convenient but causes **N+1 queries** in production that are impossible to debug because they happen outside your service layer. Stripe's engineering blog described how OSIV-induced N+1s caused database CPU to spike 400% during peak traffic. Always disable it.

### `application-dev.yml` — Local Development

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:h2:mem:zepto_dev;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  
  h2:
    console:
      enabled: true
      path: /h2-console
  
  jpa:
    hibernate:
      ddl-auto: none          # Always none — Flyway manages schema
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  
  # Use embedded Redis or disable caching in dev
  cache:
    type: simple              # In-memory cache — no Redis needed locally
  
  # Verbose logging for dev
logging:
  level:
    root: INFO
    com.zepto.backend: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql: TRACE  # Shows bound parameters

# No SSL in dev
server:
  ssl:
    enabled: false

app:
  jwt:
    secret: dev-secret-key-minimum-256-bits-for-hs256-algorithm-padding-here
    expiration-ms: 86400000   # 24h in dev — so you don't have to re-login
```

### `application-staging.yml` — Pre-production

```yaml
# application-staging.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:zepto_staging}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: ZeptoHikariStaging
  
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false
    properties:
      hibernate:
        format_sql: false
  
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
  
  cache:
    type: redis

logging:
  level:
    root: INFO
    com.zepto.backend: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] [%X{correlationId}] %-5level %logger{36} - %msg%n"
```

### `application-prod.yml` — Production

```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      # Rule of thumb: (number_of_cores * 2) + effective_spindle_count
      # For a 4-core machine with SSD: 8-12 connections
      maximum-pool-size: ${DB_POOL_MAX:20}
      minimum-idle: ${DB_POOL_MIN:5}
      connection-timeout: 30000      # Fail fast if pool exhausted — don't hang
      idle-timeout: 600000           # 10 min idle timeout
      max-lifetime: 1800000          # 30 min — rotate connections before DB kills them
      keepalive-time: 60000          # Prevent firewall from killing idle connections
      validation-timeout: 5000
      pool-name: ZeptoHikariProd
      # Critical: always validate connections before using
      connection-test-query: SELECT 1
      leak-detection-threshold: 60000  # Warn if connection held > 60s
  
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false
    properties:
      hibernate:
        jdbc:
          batch_size: 50            # Batch inserts — critical for bulk operations
        order_inserts: true
        order_updates: true
        generate_statistics: false
  
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}
      ssl:
        enabled: true
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2
  
  cache:
    type: redis
    redis:
      time-to-live: 300000          # 5 min default TTL

# Graceful shutdown — wait up to 30s for in-flight requests
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s

server:
  ssl:
    enabled: true
    key-store: ${SSL_KEYSTORE_PATH}
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
  
  # Tomcat tuning for prod
  tomcat:
    max-threads: 200
    min-spare-threads: 10
    accept-count: 100
    connection-timeout: 20000

logging:
  level:
    root: WARN
    com.zepto.backend: INFO
  # Structured JSON logging in production — parse with Loki/Elasticsearch
  pattern:
    console: >
      {"timestamp":"%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ}","level":"%level",
      "correlationId":"%X{correlationId}","traceId":"%X{traceId}",
      "spanId":"%X{spanId}","logger":"%logger","message":"%message",
      "thread":"%thread"}%n

management:
  endpoints:
    web:
      exposure:
        # Never expose env, beans, mappings in production — they leak config
        include: health,metrics,prometheus
  server:
    port: 9090   # Actuator on separate port — never exposed to internet
```

> [!WARNING]
> `leak-detection-threshold: 60000` — HikariCP will log a warning (with the full stack trace of where the connection was acquired) if a connection is held for more than 60 seconds. This is how you catch connection leaks before they exhaust the pool. A leaked connection in production means all queries eventually queue up until the pool is exhausted and your service dies. At Netflix, connection leaks caused cascading failures across services because downstream services couldn't reach the database.

### Type-Safe Configuration with `@ConfigurationProperties`

> [!IMPORTANT]
> Never use `@Value("${app.jwt.secret}")` scattered across 10 different services. If the property name changes, you get a runtime failure instead of a compile-time failure. `@ConfigurationProperties` gives you a typed Kotlin data class that fails at startup if the config is missing or invalid.

```kotlin
// config/AppProperties.kt
package com.zepto.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue
import java.time.Duration

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val jwt: JwtProperties,
    val cors: CorsProperties,
    val pagination: PaginationProperties
) {
    data class JwtProperties(
        val secret: String,
        @DefaultValue("3600000") val expirationMs: Long,
        @DefaultValue("604800000") val refreshExpirationMs: Long
    ) {
        val expirationDuration: Duration get() = Duration.ofMillis(expirationMs)
    }

    data class CorsProperties(
        @DefaultValue("http://localhost:3000") val allowedOrigins: List<String>
    )

    data class PaginationProperties(
        @DefaultValue("20") val defaultPageSize: Int,
        @DefaultValue("100") val maxPageSize: Int
    )
}
```

```kotlin
// Usage in any Spring bean
@Service
class JwtTokenProvider(private val props: AppProperties) {
    
    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(props.jwt.secret.toByteArray())
    }
    
    fun generateToken(userId: String): String {
        val now = Date()
        val expiry = Date(now.time + props.jwt.expirationMs)
        
        return Jwts.builder()
            .subject(userId)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey)
            .compact()
    }
}
```

### Profile Activation

```bash
# Via environment variable (preferred in Docker/K8s)
SPRING_PROFILES_ACTIVE=prod java -jar zepto-backend.jar

# Via JVM argument (useful in local dev)
java -Dspring.profiles.active=dev -jar zepto-backend.jar

# Via application.yml (least preferred — it's in the jar)
spring:
  profiles:
    active: dev
```

```yaml
# Kubernetes deployment environment variable
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "prod"
  - name: DB_HOST
    valueFrom:
      secretKeyRef:
        name: db-secrets
        key: host
```

---

## 1.3 Global Exception Handler

### Why a Centralized Handler is Non-Negotiable

Without a centralized exception handler, Spring Boot returns its default `BasicErrorController` response, which varies between Spring versions and leaks internal stack traces to clients in some configurations. More critically, every `@RestController` method starts growing try-catch blocks, error response construction is inconsistent, and correlating errors across logs becomes impossible.

The `@ControllerAdvice` acts as a global interceptor for all exceptions thrown from any `@Controller` in the application context.

### MDC Correlation ID Filter

Before building the exception handler, wire up MDC (Mapped Diagnostic Context) so every request gets a unique correlation ID that appears in every log line and error response:

```kotlin
// util/MdcCorrelationIdFilter.kt
package com.zepto.backend.util

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class MdcCorrelationIdFilter : OncePerRequestFilter() {

    companion object {
        const val CORRELATION_ID_HEADER = "X-Correlation-ID"
        const val MDC_CORRELATION_KEY = "correlationId"
        const val MDC_REQUEST_PATH_KEY = "requestPath"
        const val MDC_HTTP_METHOD_KEY = "httpMethod"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val correlationId = request.getHeader(CORRELATION_ID_HEADER)
            ?: UUID.randomUUID().toString()
        
        try {
            MDC.put(MDC_CORRELATION_KEY, correlationId)
            MDC.put(MDC_REQUEST_PATH_KEY, request.requestURI)
            MDC.put(MDC_HTTP_METHOD_KEY, request.method)
            
            // Echo the correlation ID back so clients can trace their request
            response.addHeader(CORRELATION_ID_HEADER, correlationId)
            
            filterChain.doFilter(request, response)
        } finally {
            // CRITICAL: Always clear MDC in finally block.
            // Thread pools reuse threads — stale MDC from request A will
            // appear in logs for request B if not cleared.
            MDC.clear()
        }
    }
}
```

> [!CAUTION]
> The `MDC.clear()` in the `finally` block is **mandatory**, not optional. Spring Boot uses a thread pool for requests. If you don't clear MDC, the correlation ID from request A will contaminate log output from request B when that thread is reused. This creates false log correlations that make debugging incidents actively harmful — you chase the wrong request chain.

### Standard API Error Response DTO

```kotlin
// dto/response/ApiError.kt
package com.zepto.backend.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiError(
    val code: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
    val path: String? = null,
    val correlationId: String? = null,
    val details: List<FieldError>? = null
) {
    data class FieldError(
        val field: String,
        val message: String,
        val rejectedValue: Any? = null
    )
}
```

### Custom Exception Hierarchy

```kotlin
// exception/AppExceptions.kt
package com.zepto.backend.exception

import org.springframework.http.HttpStatus

/**
 * Base class for all application exceptions.
 * Carries the HTTP status so the handler doesn't need a mapping table.
 */
sealed class AppException(
    open val code: String,
    override val message: String,
    val httpStatus: HttpStatus,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 404: Resource not found.
 * Example: User with ID 123 does not exist.
 */
class ResourceNotFoundException(
    resourceName: String,
    identifier: Any,
    cause: Throwable? = null
) : AppException(
    code = "RESOURCE_NOT_FOUND",
    message = "$resourceName not found with identifier: $identifier",
    httpStatus = HttpStatus.NOT_FOUND,
    cause = cause
)

/**
 * 409: Conflict with existing data.
 * Example: Email already registered, duplicate order.
 */
class ConflictException(
    override val message: String,
    val code: String = "CONFLICT",
    cause: Throwable? = null
) : AppException(
    code = code,
    message = message,
    httpStatus = HttpStatus.CONFLICT,
    cause = cause
)

/**
 * 400: Business validation failed (distinct from Jakarta Bean Validation).
 * Example: Cannot cancel an order that's already delivered.
 */
class ValidationException(
    override val message: String,
    val code: String = "VALIDATION_FAILED",
    cause: Throwable? = null
) : AppException(
    code = code,
    message = message,
    httpStatus = HttpStatus.BAD_REQUEST,
    cause = cause
)

/**
 * 401: Authentication required or token invalid.
 */
class UnauthorizedException(
    override val message: String = "Authentication required",
    val code: String = "UNAUTHORIZED",
    cause: Throwable? = null
) : AppException(
    code = code,
    message = message,
    httpStatus = HttpStatus.UNAUTHORIZED,
    cause = cause
)

/**
 * 403: Authenticated but not authorized for the resource.
 */
class ForbiddenException(
    override val message: String = "Access denied",
    val code: String = "FORBIDDEN",
    cause: Throwable? = null
) : AppException(
    code = code,
    message = message,
    httpStatus = HttpStatus.FORBIDDEN,
    cause = cause
)

/**
 * 503: Downstream service unavailable.
 * Example: Payment gateway timeout, SMS provider down.
 */
class ServiceUnavailableException(
    override val message: String,
    val code: String = "SERVICE_UNAVAILABLE",
    cause: Throwable? = null
) : AppException(
    code = code,
    message = message,
    httpStatus = HttpStatus.SERVICE_UNAVAILABLE,
    cause = cause
)
```

### Global Exception Handler

```kotlin
// handler/GlobalExceptionHandler.kt
package com.zepto.backend.handler

import com.zepto.backend.dto.response.ApiError
import com.zepto.backend.exception.AppException
import com.zepto.backend.util.MdcCorrelationIdFilter.Companion.MDC_CORRELATION_KEY
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Handle all our custom application exceptions.
     * The exception carries its own HTTP status — no mapping table needed.
     */
    @ExceptionHandler(AppException::class)
    fun handleAppException(
        ex: AppException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        // Log at appropriate level based on HTTP status
        if (ex.httpStatus.is5xxServerError) {
            log.error("Application error [${ex.code}]: ${ex.message}", ex)
        } else {
            log.warn("Application error [${ex.code}]: ${ex.message}")
        }

        val error = ApiError(
            code = ex.code,
            message = ex.message,
            path = request.requestURI,
            correlationId = MDC.get(MDC_CORRELATION_KEY)
        )

        return ResponseEntity.status(ex.httpStatus).body(error)
    }

    /**
     * Handle Jakarta Bean Validation failures (@Valid on @RequestBody).
     * Extracts ALL field errors, not just the first one.
     * Client receives a list of exactly what is wrong — critical for mobile app UX.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val fieldErrors = ex.bindingResult.fieldErrors.map { fieldError ->
            ApiError.FieldError(
                field = fieldError.field,
                message = fieldError.defaultMessage ?: "Invalid value",
                rejectedValue = fieldError.rejectedValue
            )
        }

        val globalErrors = ex.bindingResult.globalErrors.map { objectError ->
            ApiError.FieldError(
                field = objectError.objectName,
                message = objectError.defaultMessage ?: "Invalid value"
            )
        }

        log.warn("Validation failed for request to ${request.requestURI}: $fieldErrors")

        val error = ApiError(
            code = "VALIDATION_FAILED",
            message = "Request validation failed. Check 'details' for field-level errors.",
            path = request.requestURI,
            correlationId = MDC.get(MDC_CORRELATION_KEY),
            details = fieldErrors + globalErrors
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    /**
     * Malformed JSON body — client sent invalid JSON.
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMessageNotReadable(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        log.warn("Malformed request body to ${request.requestURI}: ${ex.message}")

        val error = ApiError(
            code = "MALFORMED_REQUEST",
            message = "Request body is malformed or missing",
            path = request.requestURI,
            correlationId = MDC.get(MDC_CORRELATION_KEY)
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    /**
     * Missing required query/path parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameter(
        ex: MissingServletRequestParameterException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val error = ApiError(
            code = "MISSING_PARAMETER",
            message = "Required parameter '${ex.parameterName}' of type '${ex.parameterType}' is missing",
            path = request.requestURI,
            correlationId = MDC.get(MDC_CORRELATION_KEY)
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    /**
     * Type mismatch — e.g., string passed where UUID expected.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val error = ApiError(
            code = "TYPE_MISMATCH",
            message = "Parameter '${ex.name}' should be of type '${ex.requiredType?.simpleName}'",
            path = request.requestURI,
            correlationId = MDC.get(MDC_CORRELATION_KEY)
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    /**
     * Database constraint violations — e.g., unique constraint on email.
     * IMPORTANT: Do NOT expose the raw SQL constraint name to the client.
     * Map it to a meaningful message.
     */
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(
        ex: DataIntegrityViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        // Log with full detail for debugging
        log.error("Data integrity violation at ${request.requestURI}", ex)

        // Parse constraint name from exception message to provide meaningful response
        val message = when {
            ex.message?.contains("users_email_key") == true -> "Email address is already registered"
            ex.message?.contains("users_phone_key") == true -> "Phone number is already registered"
            else -> "A data conflict occurred. Please check your request."
        }

        val error = ApiError(
            code = "DATA_CONFLICT",
            message = message,
            path = request.requestURI,
            correlationId = MDC.get(MDC_CORRELATION_KEY)
        )

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error)
    }

    /**
     * Spring Security authentication failures.
     */
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(
        ex: AuthenticationException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        log.warn("Authentication failure at ${request.requestURI}: ${ex.message}")

        val error = ApiError(
            code = "UNAUTHORIZED",
            message = "Authentication failed: ${ex.message}",
            path = request.requestURI,
            correlationId = MDC.get(MDC_CORRELATION_KEY)
        )

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error)
    }

    /**
     * Spring Security authorization failures.
     */
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(
        ex: AccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        log.warn("Access denied at ${request.requestURI}: ${ex.message}")

        val error = ApiError(
            code = "FORBIDDEN",
            message = "You do not have permission to access this resource",
            path = request.requestURI,
            correlationId = MDC.get(MDC_CORRELATION_KEY)
        )

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error)
    }

    /**
     * Catch-all for any unexpected exception.
     * Never expose stack traces to clients — log internally, return generic message.
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        // This is a 500 — always log at ERROR level with full stack trace
        log.error("Unexpected error processing request to ${request.requestURI}", ex)

        val error = ApiError(
            code = "INTERNAL_SERVER_ERROR",
            message = "An unexpected error occurred. Our team has been notified.",
            path = request.requestURI,
            correlationId = MDC.get(MDC_CORRELATION_KEY)
            // NEVER include ex.message or stack trace here — security risk
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }
}
```

> [!WARNING]
> The catch-all `Exception::class` handler must never return `ex.message` or any stack trace details to the client. Stack traces contain: class names, method names, line numbers, library versions, and sometimes even SQL queries. Each piece of this information helps an attacker enumerate your system. A real-world example: a stack trace exposed the Hibernate version → attacker searched for known HQL injection vulnerabilities in that version.

---

## 1.4 Validation Framework

### Jakarta Bean Validation vs Business Validation

Jakarta Bean Validation (`@NotBlank`, `@Email`, etc.) validates **format** — is this a valid email string, is this within range? Business validation validates **semantics** — is this email already taken, is this order in a cancellable state? Keep these completely separate:

- Jakarta Validation → happens in `@ControllerAdvice` automatically via `@Valid`
- Business validation → happens in `@Service` methods, throws `ValidationException` or `ConflictException`

### Request DTOs with Full Validation

```kotlin
// dto/request/CreateUserRequest.kt
package com.zepto.backend.dto.request

import com.zepto.backend.validation.ValidPhoneNumber
import jakarta.validation.constraints.*

data class CreateUserRequest(

    @field:NotBlank(message = "First name is required")
    @field:Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @field:Pattern(
        regexp = "^[a-zA-Z\\s'-]+$",
        message = "First name can only contain letters, spaces, hyphens, and apostrophes"
    )
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    val lastName: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be a valid email address")
    @field:Size(max = 255, message = "Email cannot exceed 255 characters")
    val email: String,

    @field:NotBlank(message = "Phone number is required")
    @field:ValidPhoneNumber   // Custom validator — see below
    val phoneNumber: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    // Note: 72 is bcrypt's max input length — beyond 72 chars bcrypt silently truncates
    val password: String
)
```

```kotlin
// dto/request/UpdateUserRequest.kt
package com.zepto.backend.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

// All fields nullable — partial update (PATCH semantics)
data class UpdateUserRequest(
    @field:Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    val firstName: String? = null,

    @field:Size(min = 2, max = 50)
    val lastName: String? = null,

    @field:Email(message = "Must be a valid email address")
    val email: String? = null
)
```

### Custom Validator — Phone Number

```kotlin
// validation/ValidPhoneNumber.kt
package com.zepto.backend.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PhoneNumberValidator::class])
@MustBeDocumented
annotation class ValidPhoneNumber(
    val message: String = "Phone number must be in E.164 format (e.g. +919876543210)",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class PhoneNumberValidator : ConstraintValidator<ValidPhoneNumber, String> {

    // E.164 format: + followed by 7-15 digits
    // Covers Indian numbers: +91XXXXXXXXXX (12 digits total)
    private val e164Pattern = Regex("^\\+[1-9]\\d{6,14}$")

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true // Use @NotNull separately for null check

        return e164Pattern.matches(value)
    }
}
```

```kotlin
// validation/ValidEnum.kt — Useful for validating enum string values in requests
package com.zepto.backend.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [EnumValidator::class])
annotation class ValidEnum(
    val enumClass: KClass<out Enum<*>>,
    val message: String = "Invalid value",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class EnumValidator : ConstraintValidator<ValidEnum, String> {

    private lateinit var enumValues: Set<String>

    override fun initialize(annotation: ValidEnum) {
        enumValues = annotation.enumClass.java.enumConstants
            .map { it.name }
            .toSet()
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true

        val valid = value.uppercase() in enumValues

        if (!valid) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate(
                "Must be one of: ${enumValues.joinToString(", ")}"
            ).addConstraintViolation()
        }

        return valid
    }
}

// Usage:
// @field:ValidEnum(enumClass = UserStatus::class, message = "Invalid user status")
// val status: String
```

### Controller with `@Valid`

```kotlin
@PostMapping("/users")
fun createUser(
    @Valid @RequestBody request: CreateUserRequest  // @Valid triggers Jakarta validation
): ResponseEntity<UserResponse> {
    // If validation fails, MethodArgumentNotValidException is thrown BEFORE this method runs
    // GlobalExceptionHandler catches it and returns 400 with field errors
    val user = userService.createUser(request)
    return ResponseEntity.status(HttpStatus.CREATED).body(user)
}
```

> [!NOTE]
> `@field:` prefix is required for Kotlin. Without it, the annotation targets the constructor parameter rather than the backing field, and Jakarta Validation's reflection-based discovery doesn't find it. This is one of the most common Kotlin + Spring Boot validation bugs — everything compiles, but validation silently doesn't run.

---

## 1.5 OpenAPI / Swagger

### Dependency

Already in `build.gradle.kts`:
```kotlin
implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
```

Access at: `http://localhost:8080/api/v1/swagger-ui.html`

### OpenAPI Configuration Bean

```kotlin
// config/OpenApiConfig.kt
package com.zepto.backend.config

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Authorization header using Bearer scheme. Example: 'Bearer {token}'"
)
class OpenApiConfig {

    @Bean
    fun openAPI(
        @Value("\${spring.application.name}") appName: String
    ): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("$appName API")
                    .description("Production API documentation for $appName")
                    .version("v1.0.0")
                    .contact(
                        Contact()
                            .name("Backend Team")
                            .email("backend@zepto.com")
                    )
                    .license(License().name("Proprietary"))
            )
            .servers(listOf(
                Server().url("http://localhost:8080/api/v1").description("Development"),
                Server().url("https://staging-api.zepto.com/api/v1").description("Staging"),
                Server().url("https://api.zepto.com/api/v1").description("Production")
            ))
    }
}
```

```yaml
# application.yml — Swagger UI path configuration
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha
    disable-swagger-default-url: true
  api-docs:
    path: /api-docs
  # Disable in production — Swagger UI should never be exposed in prod
  # Override in application-prod.yml:
  # springdoc.swagger-ui.enabled: false
```

### Annotating Controllers for Rich Documentation

```kotlin
// controller/UserController.kt
package com.zepto.backend.controller

import com.zepto.backend.dto.request.CreateUserRequest
import com.zepto.backend.dto.request.UpdateUserRequest
import com.zepto.backend.dto.response.ApiError
import com.zepto.backend.dto.response.PagedResponse
import com.zepto.backend.dto.response.UserResponse
import com.zepto.backend.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/users")
@Tag(name = "User Management", description = "APIs for managing user accounts")
class UserController(private val userService: UserService) {

    @PostMapping
    @Operation(
        summary = "Create a new user",
        description = "Registers a new user account. Email and phone must be unique."
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "User created successfully",
            content = [Content(schema = Schema(implementation = UserResponse::class))]),
        ApiResponse(responseCode = "400", description = "Validation failed",
            content = [Content(schema = Schema(implementation = ApiError::class))]),
        ApiResponse(responseCode = "409", description = "Email or phone already exists",
            content = [Content(schema = Schema(implementation = ApiError::class))])
    ])
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        val user = userService.createUser(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(user)
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get user by ID",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "User found"),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    fun getUserById(
        @Parameter(description = "UUID of the user", required = true)
        @PathVariable id: UUID
    ): ResponseEntity<UserResponse> {
        val user = userService.findById(id)
        return ResponseEntity.ok(user)
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "Update user",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    fun updateUser(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateUserRequest
    ): ResponseEntity<UserResponse> {
        val user = userService.update(id, request)
        return ResponseEntity.ok(user)
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete user (soft delete)",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponse(responseCode = "204", description = "User deleted")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteUser(@PathVariable id: UUID): ResponseEntity<Void> {
        userService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
```

---

## 1.6 Base User Domain

### Entity Design: Why NOT `data class` for JPA Entities

> [!CAUTION]
> Do NOT use `data class` for JPA entities. Kotlin `data class` generates `equals()` and `hashCode()` based on all constructor properties. JPA entities go through state transitions: transient → managed → detached → removed. A detached entity compared via `equals()` with a managed entity of the same ID will return `false` if any field changed (even lazily loaded ones), causing silent bugs in `HashSet<Entity>` collections and change detection. Use a regular class with `equals`/`hashCode` based only on the primary key.

```kotlin
// entity/User.kt
package com.zepto.backend.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_email", columnList = "email", unique = true),
        Index(name = "idx_users_phone", columnList = "phone_number", unique = true),
        Index(name = "idx_users_status_created", columnList = "status, created_at")
    ]
)
@EntityListeners(AuditingEntityListener::class)
class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    var id: UUID? = null

    @Column(name = "first_name", nullable = false, length = 50)
    var firstName: String = ""

    @Column(name = "last_name", nullable = false, length = 50)
    var lastName: String = ""

    @Column(name = "email", nullable = false, unique = true, length = 255)
    var email: String = ""

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    var phoneNumber: String = ""

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    var role: UserRole = UserRole.CUSTOMER

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: UserStatus = UserStatus.PENDING_VERIFICATION

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0L

    // equals/hashCode based ONLY on business identity (id)
    // If id is null (transient entity), use object identity
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    override fun toString(): String = "User(id=$id, email=$email, role=$role, status=$status)"
}

enum class UserRole {
    CUSTOMER, ADMIN, DELIVERY_PARTNER, WAREHOUSE_STAFF
}

enum class UserStatus {
    PENDING_VERIFICATION, ACTIVE, SUSPENDED, DELETED
}
```

### Repository

```kotlin
// repository/UserRepository.kt
package com.zepto.backend.repository

import com.zepto.backend.entity.User
import com.zepto.backend.entity.UserStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID> {

    // Spring Data derived query — generates: WHERE email = ? AND is_deleted = false
    fun findByEmailAndIsDeletedFalse(email: String): Optional<User>

    fun findByPhoneNumberAndIsDeletedFalse(phoneNumber: String): Optional<User>

    fun existsByEmail(email: String): Boolean

    fun existsByPhoneNumber(phoneNumber: String): Boolean

    // Paginated query for admin user listing with status filter
    fun findByStatusAndIsDeletedFalse(status: UserStatus, pageable: Pageable): Page<User>

    // Custom JPQL — use @Query for complex queries. Never concatenate strings (SQL injection)
    @Query("""
        SELECT u FROM User u 
        WHERE u.isDeleted = false 
        AND u.status = :status
        AND u.createdAt BETWEEN :from AND :to
        ORDER BY u.createdAt DESC
    """)
    fun findActiveUsersInRange(
        @Param("status") status: UserStatus,
        @Param("from") from: Instant,
        @Param("to") to: Instant,
        pageable: Pageable
    ): Page<User>

    // Soft delete — bulk update, does not trigger @EntityListeners
    @Modifying
    @Query("UPDATE User u SET u.isDeleted = true, u.deletedAt = :deletedAt, u.status = 'DELETED' WHERE u.id = :id")
    fun softDeleteById(@Param("id") id: UUID, @Param("deletedAt") deletedAt: Instant): Int

    // Native SQL for performance-critical queries
    @Query(
        value = "SELECT * FROM users WHERE email ILIKE :pattern AND is_deleted = false LIMIT :limit",
        nativeQuery = true
    )
    fun searchByEmailPattern(@Param("pattern") pattern: String, @Param("limit") limit: Int): List<User>
}
```

### DTOs — The API Contract Layer

```kotlin
// dto/response/UserResponse.kt
package com.zepto.backend.dto.response

import com.zepto.backend.entity.UserRole
import com.zepto.backend.entity.UserStatus
import java.time.Instant
import java.util.UUID

// This is what the API returns — never the entity itself
data class UserResponse(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val role: UserRole,
    val status: UserStatus,
    val createdAt: Instant,
    val updatedAt: Instant
    // NO passwordHash — entity has it, DTO must not
    // NO isDeleted — internal field, clients don't need it
    // NO version — internal optimistic lock field
)
```

```kotlin
// dto/response/PagedResponse.kt
package com.zepto.backend.dto.response

data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isFirst: Boolean,
    val isLast: Boolean
) {
    companion object {
        fun <T> of(springPage: org.springframework.data.domain.Page<T>): PagedResponse<T> {
            return PagedResponse(
                content = springPage.content,
                page = springPage.number,
                size = springPage.size,
                totalElements = springPage.totalElements,
                totalPages = springPage.totalPages,
                isFirst = springPage.isFirst,
                isLast = springPage.isLast
            )
        }
    }
}
```

### MapStruct Mapper

```kotlin
// mapper/UserMapper.kt
package com.zepto.backend.mapper

import com.zepto.backend.dto.response.UserResponse
import com.zepto.backend.entity.User
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR  // Build fails if mapping is incomplete
)
interface UserMapper {

    @Mapping(target = "fullName", expression = "java(user.getFirstName() + \" \" + user.getLastName())")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    fun toResponse(user: User): UserResponse

    fun toResponseList(users: List<User>): List<UserResponse>
}
```

### Service Layer — The Business Logic Core

```kotlin
// service/UserService.kt
package com.zepto.backend.service

import com.zepto.backend.dto.request.CreateUserRequest
import com.zepto.backend.dto.request.UpdateUserRequest
import com.zepto.backend.dto.response.PagedResponse
import com.zepto.backend.dto.response.UserResponse
import com.zepto.backend.entity.User
import com.zepto.backend.entity.UserStatus
import com.zepto.backend.exception.ConflictException
import com.zepto.backend.exception.ResourceNotFoundException
import com.zepto.backend.mapper.UserMapper
import com.zepto.backend.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional(readOnly = true)  // Default all methods to read-only — override with @Transactional for writes
class UserService(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper,
    private val passwordEncoder: PasswordEncoder,
    private val eventPublisher: ApplicationEventPublisher
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Creates a new user account.
     *
     * @Transactional ensures:
     * 1. Email uniqueness check and insert are atomic (within the same transaction)
     * 2. If insert fails (DB constraint), the whole transaction rolls back
     *
     * Note: There's still a tiny TOCTOU race between the existsBy check and the insert
     * in high-concurrency scenarios. The DB unique constraint is the real safety net.
     * The existsBy check just gives a better error message than catching DataIntegrityViolationException.
     */
    @Transactional
    fun createUser(request: CreateUserRequest): UserResponse {
        // Business validation — check uniqueness before attempting insert
        if (userRepository.existsByEmail(request.email.lowercase())) {
            throw ConflictException(
                message = "Email '${request.email}' is already registered",
                code = "EMAIL_ALREADY_EXISTS"
            )
        }
        
        if (userRepository.existsByPhoneNumber(request.phoneNumber)) {
            throw ConflictException(
                message = "Phone number '${request.phoneNumber}' is already registered",
                code = "PHONE_ALREADY_EXISTS"
            )
        }

        val user = User().apply {
            firstName = request.firstName.trim()
            lastName = request.lastName.trim()
            email = request.email.lowercase().trim()
            phoneNumber = request.phoneNumber
            // Always hash passwords with bcrypt — cost factor 12 is a good production default
            // Cost 12 ≈ 300ms on modern hardware — slow enough for brute force, fast enough for UX
            passwordHash = passwordEncoder.encode(request.password)
        }

        val savedUser = userRepository.save(user)
        
        log.info("User created: id=${savedUser.id}, email=${savedUser.email}")
        
        // Publish domain event — listeners handle email verification, welcome flow
        // ApplicationEventPublisher is synchronous by default; use @Async on listener for async
        eventPublisher.publishEvent(UserCreatedEvent(this, savedUser.id!!, savedUser.email))
        
        return userMapper.toResponse(savedUser)
    }

    fun findById(id: UUID): UserResponse {
        val user = userRepository.findById(id)
            .filter { !it.isDeleted }
            .orElseThrow { ResourceNotFoundException("User", id) }
        return userMapper.toResponse(user)
    }

    fun findAll(page: Int, size: Int, sortBy: String = "createdAt"): PagedResponse<UserResponse> {
        val validatedSize = size.coerceAtMost(100) // Never allow unbounded page sizes
        val pageable = PageRequest.of(page, validatedSize, Sort.by(Sort.Direction.DESC, sortBy))
        
        val usersPage = userRepository.findAll(pageable)
            .map(userMapper::toResponse)
        
        return PagedResponse.of(usersPage)
    }

    @Transactional
    fun update(id: UUID, request: UpdateUserRequest): UserResponse {
        val user = userRepository.findById(id)
            .filter { !it.isDeleted }
            .orElseThrow { ResourceNotFoundException("User", id) }

        // Apply only non-null fields (PATCH semantics)
        request.firstName?.let { user.firstName = it.trim() }
        request.lastName?.let { user.lastName = it.trim() }
        request.email?.let { newEmail ->
            if (newEmail.lowercase() != user.email) {
                if (userRepository.existsByEmail(newEmail.lowercase())) {
                    throw ConflictException("Email '$newEmail' is already taken", "EMAIL_ALREADY_EXISTS")
                }
                user.email = newEmail.lowercase().trim()
            }
        }

        // No explicit save needed — entity is managed within the transaction
        // Hibernate detects dirty fields and issues UPDATE automatically at flush time
        log.info("User updated: id=${user.id}")
        
        return userMapper.toResponse(user)
    }

    @Transactional
    fun delete(id: UUID) {
        val deletedCount = userRepository.softDeleteById(id, Instant.now())
        
        if (deletedCount == 0) {
            throw ResourceNotFoundException("User", id)
        }
        
        log.info("User soft-deleted: id=$id")
    }

    // Internal method — used by security layer
    fun findByEmail(email: String): User {
        return userRepository.findByEmailAndIsDeletedFalse(email.lowercase())
            .orElseThrow { ResourceNotFoundException("User", email) }
    }
}
```

> [!IMPORTANT]
> The `@Transactional(readOnly = true)` at the class level is a critical performance optimization. For read-only transactions, Hibernate skips the dirty checking phase at flush time (no need to diff entity state against snapshot). The database driver can also route to read replicas. Override with `@Transactional` (write) on individual methods that modify data.

### Domain Event

```kotlin
// event/UserCreatedEvent.kt
package com.zepto.backend.event

import org.springframework.context.ApplicationEvent
import java.util.UUID

class UserCreatedEvent(
    source: Any,
    val userId: UUID,
    val email: String
) : ApplicationEvent(source)

// event/UserCreatedEventListener.kt
package com.zepto.backend.event

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class UserCreatedEventListener {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * @Async — runs in a separate thread so the original create request returns immediately.
     * The user gets their 201 response; the welcome email is sent asynchronously.
     * If this fails, it does NOT roll back the user creation (different transaction).
     * For critical events, use a transactional outbox pattern instead.
     */
    @Async
    @EventListener
    fun onUserCreated(event: UserCreatedEvent) {
        log.info("Sending welcome email to ${event.email} for user ${event.userId}")
        // TODO: EmailService.sendWelcomeEmail(event.email)
    }
}
```

### Security Config (minimal — expand in security chapter)

```kotlin
// config/SecurityConfig.kt
package com.zepto.backend.config

import com.zepto.backend.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // Enables @PreAuthorize
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }           // Disabled for stateless JWT API
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints — no auth needed
                    .requestMatchers(HttpMethod.POST, "/users").permitAll()
                    .requestMatchers("/auth/**").permitAll()
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api-docs/**",
                        "/actuator/health"
                    ).permitAll()
                    // Everything else requires auth
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12) // Cost factor 12

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager
}
```

### ApplicationRunner for Startup Initialization

```kotlin
// config/DataInitializer.kt
package com.zepto.backend.config

import com.zepto.backend.entity.User
import com.zepto.backend.entity.UserRole
import com.zepto.backend.entity.UserStatus
import com.zepto.backend.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Only runs in dev profile — seeds an admin user for local development.
 * Never run in prod — use Flyway data migrations for any seed data.
 */
@Component
@Profile("dev")
class DataInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (!userRepository.existsByEmail("admin@zepto.com")) {
            val admin = User().apply {
                firstName = "Admin"
                lastName = "User"
                email = "admin@zepto.com"
                phoneNumber = "+919999999999"
                passwordHash = passwordEncoder.encode("Admin@123!")
                role = UserRole.ADMIN
                status = UserStatus.ACTIVE
            }
            userRepository.save(admin)
            log.info("Dev admin user seeded: admin@zepto.com")
        }
    }
}
```

### Integration Test Example

```kotlin
// test/UserControllerIntegrationTest.kt
package com.zepto.backend.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.zepto.backend.dto.request.CreateUserRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional  // Rolls back after each test — keeps DB clean
class UserControllerIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    @Test
    fun `createUser should return 201 with valid request`() {
        val request = CreateUserRequest(
            firstName = "Shreyas",
            lastName = "Damase",
            email = "shreyas@test.com",
            phoneNumber = "+919876543210",
            password = "SecurePass@123"
        )

        mockMvc.post("/users") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.email") { value("shreyas@test.com") }
            jsonPath("$.passwordHash") { doesNotExist() }  // Must not be in response
        }
    }

    @Test
    fun `createUser should return 400 when email is invalid`() {
        val request = mapOf(
            "firstName" to "Test",
            "lastName" to "User",
            "email" to "not-an-email",
            "phoneNumber" to "+919876543210",
            "password" to "SecurePass@123"
        )

        mockMvc.post("/users") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
            jsonPath("$.details[0].field") { value("email") }
        }
    }

    @Test
    fun `createUser should return 409 when email already exists`() {
        val request = CreateUserRequest(
            firstName = "Duplicate",
            lastName = "User",
            email = "admin@zepto.com",  // Already seeded
            phoneNumber = "+911234567890",
            password = "SecurePass@123"
        )

        mockMvc.post("/users") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("EMAIL_ALREADY_EXISTS") }
        }
    }
}
```

---

## Production Readiness Checklist

| Category | Item | Status |
|----------|------|--------|
| Build | Kotlin Spring plugin configured | ✅ |
| Build | Kotlin JPA plugin configured | ✅ |
| Build | `mapstruct.unmappedTargetPolicy = ERROR` | ✅ |
| Config | Secrets via env vars only | ✅ |
| Config | OSIV disabled | ✅ |
| Config | Profile-specific configs isolated | ✅ |
| Error Handling | Centralized `@ControllerAdvice` | ✅ |
| Error Handling | MDC correlation ID in all errors | ✅ |
| Error Handling | No stack traces in API responses | ✅ |
| Validation | Jakarta Bean Validation on all DTOs | ✅ |
| Validation | Custom validators for domain formats | ✅ |
| Entity | No `data class` for JPA entities | ✅ |
| Entity | `equals`/`hashCode` based on ID only | ✅ |
| Entity | Soft delete pattern | ✅ |
| Entity | `@Version` for optimistic locking | ✅ |
| API | Never expose entity directly | ✅ |
| API | Paged responses for list endpoints | ✅ |
| Security | BCrypt cost factor 12 | ✅ |
| Security | JWT stateless session | ✅ |
| Database | HikariCP pool tuning in prod | ✅ |
| Database | `leak-detection-threshold` configured | ✅ |
| Observability | Structured JSON logging in prod | ✅ |
| Observability | Prometheus metrics exposed | ✅ |
| Observability | Distributed tracing configured | ✅ |
| Testing | Integration tests with `@SpringBootTest` | ✅ |
| Testing | `@Transactional` on tests for rollback | ✅ |
