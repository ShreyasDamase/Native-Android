# Chapter 14 — Redis Production Caching, Rate Limits and Realtime State

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

### _Using Redis for speed without turning it into an unsafe second database_

---

## 14.1 Redis Jobs in Production

Redis is not only `@Cacheable`. In real apps it is used for:

- API response cache.
- Session store.
- OTP and verification codes.
- Rate limiting.
- Idempotency short-term records.
- Distributed locks.
- Cart state.
- Current driver location.
- Counters and leaderboards.
- Pub/Sub or Streams for lightweight realtime workflows.

The rule: Redis should hold data that is temporary, derived, or safely recoverable. PostgreSQL remains the source of truth.

---

## 14.2 Spring Cache Abstraction

Enable caching:

```kotlin
@Configuration
@EnableCaching
class CacheConfig
```

Use it at service level:

```kotlin
@Service
class RestaurantQueryService(
    private val restaurantRepository: RestaurantRepository
) {
    @Cacheable(
        cacheNames = ["restaurant-detail"],
        key = "#restaurantId",
        unless = "#result == null"
    )
    fun getRestaurant(restaurantId: UUID): RestaurantResponse? {
        return restaurantRepository.findByIdOrNull(restaurantId)
            ?.let(RestaurantResponse::from)
    }

    @CacheEvict(cacheNames = ["restaurant-detail"], key = "#restaurantId")
    fun evictRestaurant(restaurantId: UUID) {
    }
}
```

Annotation explanation:

- `@EnableCaching`: turns on Spring's cache proxy support.
- `@Cacheable`: checks cache before running the method. If cache hit, method is skipped.
- `cacheNames`: logical cache bucket.
- `key`: SpEL expression that builds the cache key.
- `unless`: condition for not caching the result.
- `@CacheEvict`: removes stale data after updates.

Good practice: do not cache inside controllers. Cache query services because they represent reusable reads.

---

## 14.3 Redis Cache Manager

```kotlin
@Configuration
@EnableCaching
class RedisCacheConfig {
    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(GenericJackson2JsonRedisSerializer())
            )

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(
                mapOf(
                    "restaurant-detail" to defaultConfig.entryTtl(Duration.ofMinutes(30)),
                    "search-page" to defaultConfig.entryTtl(Duration.ofMinutes(2)),
                    "menu" to defaultConfig.entryTtl(Duration.ofMinutes(10))
                )
            )
            .transactionAware()
            .build()
    }
}
```

Good practice:

- Give every cache a TTL.
- Do not cache nulls unless you intentionally want negative caching.
- Use short TTL for search pages because ranking and availability change.
- Use explicit eviction after writes.

---

## 14.4 Rate Limiting

Rate limiting protects login, OTP, checkout and AI endpoints.

Simple fixed-window limiter:

```kotlin
@Service
class RateLimitService(
    private val redisTemplate: StringRedisTemplate
) {
    fun check(key: String, limit: Long, window: Duration) {
        val count = redisTemplate.opsForValue().increment(key) ?: 1

        if (count == 1L) {
            redisTemplate.expire(key, window)
        }

        if (count > limit) {
            throw TooManyRequestsException("Too many requests")
        }
    }
}
```

Usage:

```kotlin
rateLimitService.check(
    key = "rate:otp:${phoneNumber}:${LocalDate.now()}",
    limit = 5,
    window = Duration.ofDays(1)
)
```

Better production options:

- Token bucket algorithm.
- Sliding window with sorted sets.
- Gateway-level rate limiting.
- Separate limits by user id, IP, device id, endpoint and tenant.

---

## 14.5 OTP and Temporary Codes

```kotlin
fun storeOtp(phone: String, otp: String) {
    redisTemplate.opsForValue().set(
        "otp:$phone",
        passwordEncoder.encode(otp),
        Duration.ofMinutes(5)
    )
}

fun verifyOtp(phone: String, rawOtp: String): Boolean {
    val stored = redisTemplate.opsForValue().get("otp:$phone") ?: return false
    val valid = passwordEncoder.matches(rawOtp, stored)
    if (valid) redisTemplate.delete("otp:$phone")
    return valid
}
```

Good practice:

- Store hashed OTPs, not raw OTPs.
- Short TTL.
- Delete after successful verification.
- Rate-limit attempts.

---

## 14.6 Distributed Locks

Use locks sparingly. Prefer database constraints and transactions when possible.

```kotlin
fun <T> withLock(key: String, ttl: Duration, block: () -> T): T {
    val token = UUID.randomUUID().toString()
    val acquired = redisTemplate.opsForValue()
        .setIfAbsent(key, token, ttl) == true

    if (!acquired) throw ConflictException("Resource is busy")

    try {
        return block()
    } finally {
        val current = redisTemplate.opsForValue().get(key)
        if (current == token) {
            redisTemplate.delete(key)
        }
    }
}
```

Production warning: this simple lock is acceptable for many single Redis deployments, but for strict distributed correctness use a mature lock library and understand failure modes. For money and inventory, a PostgreSQL transaction is often safer.

---

## 14.7 Current Location and Nearby Drivers

Redis GEO can store current driver locations:

```kotlin
fun updateDriverLocation(driverId: UUID, lat: Double, lng: Double) {
    redisTemplate.opsForGeo().add(
        "geo:drivers:available",
        Point(lng, lat),
        driverId.toString()
    )
    redisTemplate.expire("geo:drivers:available", Duration.ofMinutes(10))
}
```

Query nearby:

```kotlin
fun nearbyDrivers(lat: Double, lng: Double): List<String> {
    val result = redisTemplate.opsForGeo().radius(
        "geo:drivers:available",
        Circle(Point(lng, lat), Distance(3.0, Metrics.KILOMETERS)),
        RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
            .includeDistance()
            .sortAscending()
            .limit(20)
    )

    return result?.mapNotNull { it.content.name } ?: emptyList()
}
```

Use Cassandra or object storage for long-term location history. Redis GEO is for current matching state.

---

## 14.8 Cache Invalidation Patterns

Hard part of caching: stale data.

Patterns:

- TTL only: simplest, acceptable for non-critical read pages.
- Write-through eviction: update PostgreSQL, then evict cache.
- Versioned keys: include `updatedAt` or version in key.
- Event-driven invalidation: publish `RestaurantUpdatedEvent`, evict related caches.
- Read model rebuild: for complex search/index caches, rebuild Elasticsearch or materialized views asynchronously.

Example:

```kotlin
@Transactional
fun updateMenu(command: UpdateMenuCommand) {
    menuRepository.save(command.toEntity())
    eventPublisher.publishEvent(MenuUpdatedEvent(command.restaurantId))
}

@EventListener
fun onMenuUpdated(event: MenuUpdatedEvent) {
    cacheManager.getCache("menu")?.evict(event.restaurantId)
    cacheManager.getCache("restaurant-detail")?.evict(event.restaurantId)
}
```

---

## 14.9 Source Links

- Spring Boot caching: https://docs.spring.io/spring-boot/reference/io/caching.html
- Spring Data Redis cache: https://docs.spring.io/spring-data/redis/reference/redis/redis-cache.html

