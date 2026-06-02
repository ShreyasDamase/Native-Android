# P0-2 — Spring Boot + Kotlin Ecosystem

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> [!WARNING]
> **The Production Paradox**
> Spring Boot is incredibly easy to start, but notoriously complex to master for production. "Magic" annotations like `@Transactional` or `@Cacheable` will save you hours in development, but if you don't understand what proxy classes and connection pools they generate under the hood, they will take down your servers during traffic spikes.

Kotlin paired with Spring Boot 3 is the modern gold standard for backend development. Kotlin gives you null-safety and concise syntax, while Spring Boot provides an industrial-grade, battle-tested ecosystem.

Here is the blueprint of the essential Spring Boot ecosystem components you must master to build a company.

---

## 1. Kotlin + Spring Boot: The Marriage
Spring is historically a Java framework. Kotlin is interoperable with Java, but to make Spring work perfectly with Kotlin, you need a few critical compiler plugins:

- **`kotlin-spring` plugin:** Spring requires classes and methods to be `open` (non-final) to create CGLIB proxies (used for `@Transactional`, `@Async`, etc.). Kotlin classes are `final` by default. This plugin automatically opens classes annotated with Spring annotations.
- **`kotlin-jpa` plugin:** Hibernate/JPA requires a no-argument constructor for entities. Kotlin data classes don't have one by default. This plugin generates synthetic no-arg constructors.

### Why Kotlin over Java for Startups?
- **Null Safety (`?`):** Eliminates `NullPointerException`, the most common cause of 500 errors in production.
- **Data Classes:** Reduces hundreds of lines of Java boilerplate (Getters/Setters/`equals`/`hashCode`) down to a single line.
- **Coroutines:** Spring Boot 3 natively supports Kotlin Coroutines (`suspend` functions), allowing you to write highly scalable, non-blocking asynchronous code as if it were synchronous.

---

## 2. Spring Data JPA (The Database Bridge)

> [!CAUTION]
> **N+1 Query Problem:** The silent killer of Spring Boot apps. If you fetch a list of 100 Orders, and each Order lazy-loads its User, JPA will execute 1 query for the orders, and 100 queries for the users. Your database will instantly bottleneck.

Spring Data JPA is an abstraction over Hibernate. It translates your Kotlin interfaces into SQL queries automatically.

**Production Rules for JPA:**
1. **Never use `@Data` or full `data class` for JPA Entities:** Data classes generate `equals()` and `hashCode()` that include all properties. If you include relationships (like `@OneToMany`), evaluating `equals` can trigger lazy loading of thousands of rows, causing out-of-memory errors.
2. **Use `@EntityGraph` or `JOIN FETCH`:** Always explicitly define how relationships should be fetched when you need them, to prevent N+1 queries.
3. **Understand `@Transactional`:** It holds a database connection open for the entire duration of the method. **Never** do network calls (like calling a third-party API or sending an email) inside a `@Transactional` block, or you will exhaust your HikariCP connection pool and take down the app.

---

## 3. Spring Security

Spring Security is a massive, highly customizable framework for Authentication (who are you?) and Authorization (what can you do?).

### The Production Setup:
For a modern REST API (like a delivery app backend), you do **not** use sessions (cookies). You use **Stateless JWT (JSON Web Tokens)**.

1. **The Filter Chain:** Spring Security wraps your app in a chain of filters. The request hits `JwtAuthenticationFilter` first. It extracts the token from the `Authorization: Bearer <token>` header, validates the signature, and sets the `SecurityContext`.
2. **Role-Based Access Control (RBAC):** You protect endpoints using annotations.
   ```kotlin
   @PreAuthorize("hasRole('ADMIN')")
   @PostMapping("/inventory")
   fun updateInventory() { ... }
   ```
3. **Password Hashing:** **Never** store plain text passwords. Always use Spring's `BCryptPasswordEncoder`.

---

## 4. Spring Cache (`@Cacheable`)

When your app scales, the database becomes the bottleneck. Spring Cache provides a transparent abstraction to cache method return values.

### How it Works:
```kotlin
@Cacheable(value = ["products"], key = "#productId")
fun getProduct(productId: Long): Product {
    // This code only runs if the product is NOT in the cache.
    return productRepository.findById(productId)
}
```

### Production Rules for Caching:
- **Use Redis:** Do not use the default `ConcurrentHashMap` (in-memory) cache for production. If you have 3 instances of your app behind a load balancer, they will have out-of-sync caches. Always configure Spring to use **Redis** as the distributed cache provider.
- **Cache Eviction (`@CacheEvict`):** The hardest part of caching is invalidation. When a product price updates, you **must** evict the old cache entry, or users will checkout with stale prices.

---

## 5. Spring Boot Actuator

> [!IMPORTANT]
> Flying a plane without instruments is suicide. Running a company backend without Actuator is exactly the same.

Actuator automatically exposes operational endpoints (`/actuator/health`, `/actuator/metrics`) to monitor your application's health.

### Critical Actuator Endpoints:
- **`/health`:** Kubernetes or your Load Balancer uses this to check if the app is alive. If it returns `DOWN` (e.g., because the database connection died), Kubernetes will automatically restart the pod.
- **`/metrics`:** Exposes JVM memory usage, garbage collection times, HikariCP database connection pool stats, and HTTP request latencies.
- **Prometheus Integration:** Actuator seamlessly exposes these metrics in a format that **Prometheus** can scrape, allowing you to build beautiful **Grafana** dashboards to monitor your company in real-time.

---

## Summary
The Spring Boot + Kotlin ecosystem provides everything a startup needs to reach enterprise scale. But remember:
1. Master JPA relationships and connection pools.
2. Secure your boundaries statelessly with JWT.
3. Cache expensive queries in Redis.
4. Always expose Actuator metrics so you know exactly what is happening in production.

## Book-Aligned Corrections

From `Pro Spring Boot 3 with Kotlin`, the ecosystem should be understood through starters and auto-configuration:

- Adding a starter changes the application because Spring Boot auto-configures beans based on the classpath.
- `spring-boot-starter-web` means Spring MVC plus embedded servlet server defaults.
- `spring-boot-starter-data-jpa` means JPA/Hibernate plus datasource auto-configuration.
- HikariCP is the default connection pool when present.
- `spring-boot-starter-actuator` exposes operational endpoints after endpoint exposure/security is configured.
- `spring-boot-docker-compose` can auto-start local backing services during development.

Correct mental model:

```text
dependency on classpath
  -> auto-configuration condition matches
  -> Spring creates infrastructure beans
  -> application.yml/properties override defaults
```

Do not memorize annotations as magic. Learn which starter, auto-configuration, and property is responsible.
