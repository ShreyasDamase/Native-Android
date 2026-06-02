# P0-5 — Caching Strategies — Redis, Cache-Aside, TTL, Distributed Cache Patterns

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> Every database query costs time and money. At 1 request/second, a 50ms database query is invisible. At 10,000 requests/second, that same query destroys your DB connection pool, spikes CPU, costs you hundreds of dollars in RDS compute, and causes user-facing latency spikes. Caching is not optional at scale — it is a core architectural concern.

---

## 1. Why Cache — The Numbers That Drive the Decision

### 1.1 Query Cost at Scale

| Operation | Typical Latency | At 10K RPS Impact |
|---|---|---|
| PostgreSQL query (indexed, cached in PG buffer) | 2-10ms | 10K × 10ms = 100s of CPU time/sec |
| PostgreSQL query (disk I/O required) | 50-200ms | Connection pool saturation |
| Redis GET (in-memory) | 0.1-0.5ms | Negligible overhead |
| Redis GET (over network, AWS same AZ) | 0.5-2ms | Very low |
| In-process HashMap get | < 0.01ms | Zero network overhead |

A product catalog API that queries 5 tables per request WITHOUT caching at 1000 RPS:
- 5000 queries/second hitting PostgreSQL
- Each connection from a pool of 20-50 connections
- Average connection wait time grows exponentially after pool saturation
- Response times go from 50ms to 2-5 seconds under load
- Users abandon the app

WITH caching (Cache Hit Rate 90%):
- Only 500 queries/second reach PostgreSQL (10% misses)
- Pool is never saturated
- 90% of requests served in < 5ms from Redis
- Database cost drops proportionally

### 1.2 Cloud Cost Reduction

For a system doing 10M product page views/day:
- Without cache: 10M × 5 DB queries = 50M queries/day. At AWS RDS r6g.xlarge ($350/month), you'll need 3-5 instances.
- With 90% cache hit rate: 5M queries/day → one RDS instance handles it comfortably.
- ElastiCache r6g.large ($150/month) vs 2 extra RDS instances ($700/month) → cache pays for itself.

---

## 2. Cache-Aside (Lazy Loading) — The Most Common Pattern

### 2.1 How It Works

The application code is responsible for managing the cache. The cache is NOT automatically kept in sync — you populate it on demand.

```
Read Request:
  1. Check cache for key
  2a. CACHE HIT → return cached value (fast path, ~1ms)
  2b. CACHE MISS → query database, store result in cache, return value
```

```kotlin
@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val redisTemplate: RedisTemplate<String, Any>
) {
    companion object {
        const val PRODUCT_CACHE_TTL_SECONDS = 3600L  // 1 hour
        private const val PRODUCT_CACHE_PREFIX = "product:v1:"
    }

    fun getProduct(productId: UUID): ProductDto {
        val cacheKey = "$PRODUCT_CACHE_PREFIX$productId"
        
        // 1. Check cache (CACHE-ASIDE: application drives this logic)
        val cached = redisTemplate.opsForValue().get(cacheKey)
        if (cached != null) {
            return cached as ProductDto  // Cache hit
        }
        
        // 2. Cache miss — fetch from DB
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Product not found: $productId") }
        
        val dto = product.toDto()
        
        // 3. Populate cache with TTL
        redisTemplate.opsForValue().set(cacheKey, dto, Duration.ofSeconds(PRODUCT_CACHE_TTL_SECONDS))
        
        return dto  // Cache miss — returned from DB
    }
}
```

### 2.2 Advantages and Disadvantages of Cache-Aside

| Aspect | Detail |
|---|---|
| **Resilience** | If Redis goes down, app falls back to DB — no total outage |
| **Only caches what's needed** | Cold data never wastes cache memory |
| **First request is always slow** | Cache miss penalty on first access after TTL expiry |
| **Stale data risk** | DB update doesn't automatically invalidate cache |
| **Read-heavy workloads** | Excellent. Used by Twitter, Instagram, Zepto for product data |
| **Write-heavy workloads** | Poor — cache becomes stale constantly |

> [!IMPORTANT]
> Cache-Aside is the DEFAULT pattern for read-heavy APIs. Use it for: product details, user profiles, configuration data, category trees, pricing tables — anything that changes infrequently and is read frequently.

---

## 3. Write-Through Cache — Keeping Cache Always Fresh

### 3.1 How It Works

On EVERY write, update BOTH the database AND the cache simultaneously. Reads never miss (after the first write).

```
Write Request:
  1. Write to Database
  2. Write to Cache (same data, with TTL)
  3. Return success

Read Request:
  1. Check cache → always a HIT (after first write) → return
```

```kotlin
@Service
class InventoryService(
    private val inventoryRepository: InventoryRepository,
    private val redisTemplate: RedisTemplate<String, Any>
) {

    @Transactional
    fun updateStock(productId: UUID, newQuantity: Int): InventoryDto {
        val inventory = inventoryRepository.findByProductIdForUpdate(productId)
            ?: throw ResourceNotFoundException("Inventory not found")
        
        inventory.stockQuantity = newQuantity
        val saved = inventoryRepository.save(inventory)
        
        // Write-through: update cache immediately after DB write
        val dto = saved.toDto()
        val cacheKey = "inventory:$productId"
        redisTemplate.opsForValue().set(cacheKey, dto, Duration.ofMinutes(30))
        
        return dto
    }

    fun getStock(productId: UUID): InventoryDto {
        val cacheKey = "inventory:$productId"
        
        // With write-through, this should almost always be a hit
        return (redisTemplate.opsForValue().get(cacheKey) as? InventoryDto)
            ?: run {
                // First access or Redis restart — fall back to DB
                val inventory = inventoryRepository.findByProductId(productId)
                    ?: throw ResourceNotFoundException("Inventory not found")
                val dto = inventory.toDto()
                redisTemplate.opsForValue().set(cacheKey, dto, Duration.ofMinutes(30))
                dto
            }
    }
}
```

### 3.2 When to Use Write-Through

Use write-through when:
- Data is written AND read frequently (both traffic patterns are high)
- Stale data is unacceptable (e.g., inventory levels shown to customers)
- You have a moderate number of keys (not billions — all are pre-cached)

> [!WARNING]
> Write-through causes **write amplification** — every DB write becomes two operations. For write-heavy workloads with infrequent reads, you're populating cache with data that may never be read. Use Cache-Aside for cold data, Write-Through for hot data with write traffic.

---

## 4. Write-Behind (Write-Back) Cache — The Dangerous Pattern

### 4.1 How It Works

Write to cache ONLY. Return success immediately. A background worker asynchronously flushes cached writes to the database.

```
Write Request:
  1. Write to Cache → return success immediately (ultra-fast)
  [background, async]
  2. Worker reads dirty keys from cache
  3. Worker writes to Database
```

### 4.2 Why This Is Dangerous

> [!CAUTION]
> **NEVER use write-behind for financial data, orders, inventory, or any data where loss is unacceptable.**
>
> If Redis crashes between step 1 (cache write) and step 3 (DB flush), **your data is permanently lost**. Redis default persistence (RDB snapshots every 5 minutes) means you can lose up to 5 minutes of writes.
>
> The only safe use case for write-behind is analytics counters (page view counts, click counts) where approximate values are acceptable.

```kotlin
// ONLY FOR NON-CRITICAL ANALYTICS
@Service
class AnalyticsService(private val redisTemplate: StringRedisTemplate) {
    
    // This counter is in-cache only — may lose last N minutes on crash
    // Acceptable for analytics. NOT acceptable for orders or payments.
    fun trackProductView(productId: UUID) {
        val key = "analytics:product_views:${LocalDate.now()}:$productId"
        redisTemplate.opsForValue().increment(key)
        redisTemplate.expire(key, Duration.ofDays(2))  // Auto-cleanup old counters
    }
}

// Background job to persist to DB (best-effort, not transactional)
@Component
class AnalyticsFlushJob(
    private val redisTemplate: StringRedisTemplate,
    private val analyticsRepository: AnalyticsRepository
) {
    @Scheduled(fixedDelay = 60_000)  // Every minute
    fun flushViewCounts() {
        val pattern = "analytics:product_views:${LocalDate.now()}:*"
        val keys = redisTemplate.keys(pattern)
        
        keys?.forEach { key ->
            val count = redisTemplate.opsForValue().get(key)?.toLongOrNull() ?: return@forEach
            val productId = key.substringAfterLast(":")
            analyticsRepository.upsertViewCount(UUID.fromString(productId), count)
        }
    }
}
```

---

## 5. Spring `@Cacheable` — How the Proxy Works Under the Hood

### 5.1 The Mechanics

Spring's `@Cacheable` is implemented using **AOP (Aspect-Oriented Programming) proxies**. When you annotate a method, Spring generates a proxy class that wraps your bean:

```
Client calls ProductService.getProduct(id)
  → Spring AOP proxy intercepts the call
  → Checks cache for key (generated from arguments)
  → CACHE HIT: return cached value, SKIP the real method
  → CACHE MISS: call real method, store return value in cache, return value
```

```kotlin
@Service
class ProductService(private val productRepository: ProductRepository) {

    @Cacheable(
        cacheNames = ["products"],
        key = "#productId",                           // SpEL expression for cache key
        condition = "#productId != null",             // Only cache when condition is true
        unless = "#result == null"                    // Don't cache null results
    )
    fun getProduct(productId: UUID): ProductDto? {
        // This method body ONLY runs on cache miss
        return productRepository.findById(productId).map { it.toDto() }.orElse(null)
    }

    @CachePut(
        cacheNames = ["products"],
        key = "#result.id"   // Update cache with new value after write
    )
    fun updateProduct(productId: UUID, request: UpdateProductRequest): ProductDto {
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Product not found") }
        product.name = request.name
        product.price = request.price
        return productRepository.save(product).toDto()
        // Return value is automatically stored in cache
    }

    @CacheEvict(
        cacheNames = ["products"],
        key = "#productId"   // Remove this key from cache
    )
    fun deleteProduct(productId: UUID) {
        productRepository.deleteById(productId)
    }
    
    @CacheEvict(
        cacheNames = ["products"],
        allEntries = true   // Nuke entire cache — use sparingly
    )
    fun clearAllProductCache() {
        // Used after bulk import or major data refresh
    }
}
```

> [!WARNING]
> **The Self-Invocation Trap**: `@Cacheable` only works when called through the Spring proxy. If you call a `@Cacheable` method FROM WITHIN the same bean (e.g., `this.getProduct(id)` inside `ProductService`), the proxy is bypassed and caching does NOT happen. This is a common Spring AOP gotcha that wastes hours in debugging.

```kotlin
// BROKEN: self-invocation bypasses proxy
@Service
class ProductService {
    @Cacheable(cacheNames = ["products"], key = "#id")
    fun getProduct(id: UUID): ProductDto { /* ... */ }
    
    fun getSomeOtherData(id: UUID): SomeDto {
        val product = this.getProduct(id)  // ← Cache is NEVER used here!
        // ...
    }
}

// FIX: inject self or extract to separate bean
@Service
class ProductService(
    @Lazy private val self: ProductService  // Inject own proxy
) {
    fun getSomeOtherData(id: UUID): SomeDto {
        val product = self.getProduct(id)  // ← Goes through proxy, cache works
        // ...
    }
}
```

### 5.2 Complete Spring Cache Configuration with Redis

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("io.lettuce:lettuce-core")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}
```

```kotlin
@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): CacheManager {
        val mapper = ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // CRITICAL: include type info so deserialization works
            activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                    .allowIfBaseType(Any::class.java)
                    .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
            )
        }
        
        val serializer = GenericJackson2JsonRedisSerializer(mapper)
        
        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(60))       // Default TTL: 1 hour
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer)
            )
            .disableCachingNullValues()              // Don't cache null — causes subtle bugs
        
        // Per-cache TTL configuration
        val cacheConfigurations = mapOf(
            "products"      to defaultConfig.entryTtl(Duration.ofHours(2)),
            "categories"    to defaultConfig.entryTtl(Duration.ofHours(12)),  // Changes rarely
            "userProfiles"  to defaultConfig.entryTtl(Duration.ofMinutes(30)),
            "inventory"     to defaultConfig.entryTtl(Duration.ofMinutes(5)), // Changes often
            "sessions"      to defaultConfig.entryTtl(Duration.ofDays(7)),
            "rateLimits"    to defaultConfig.entryTtl(Duration.ofMinutes(1))
        )
        
        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }
}
```

```yaml
# application.yml - Complete Redis configuration
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0
      timeout: 2000ms           # Command timeout — fail fast
      connect-timeout: 1000ms   # Connection establishment timeout
      lettuce:
        pool:
          max-active: 16        # Max connections in pool
          max-idle: 8           # Max idle connections
          min-idle: 2           # Min idle connections (pre-warmed)
          max-wait: 1000ms      # Max wait for a connection from pool
        shutdown-timeout: 100ms

  cache:
    type: redis
    redis:
      time-to-live: 3600000     # Default TTL in ms (1 hour) — overridden by CacheConfig
      key-prefix: "sentinel:"   # Global key prefix — isolates this app from others
      use-key-prefix: true
      cache-null-values: false
```

---

## 6. TTL Strategy — The Art of Picking Expiry Times

### 6.1 The Two Failure Modes

**TTL too SHORT → Cache Miss Storm**

If you set TTL = 30 seconds and 10,000 users request the same product simultaneously:
- At t=0: cache miss, one request goes to DB, 9,999 requests wait or also miss
- At t=30s: ALL keys expire simultaneously
- At t=31s: another 10,000 cache misses hit the DB together

**TTL too LONG → Stale Data**

If you cache product prices for 24 hours and update the price at 3pm, users see old prices until tomorrow at 3pm. For pricing, inventory, and real-time data — long TTLs are dangerous.

### 6.2 TTL Guidelines by Data Type

| Data Type | Recommended TTL | Rationale |
|---|---|---|
| Product details (name, description, images) | 2-12 hours | Changes infrequently |
| Product price | 5-15 minutes | Flash sales, dynamic pricing |
| Inventory/stock count | 30 seconds - 2 minutes | Changes every order |
| Category tree | 12-24 hours | Changes rarely |
| User profile | 30 minutes | Updated on edit |
| Search results | 5 minutes | Index changes continuously |
| Configuration/feature flags | 5 minutes | Quick rollout needed |
| Home page featured items | 15-30 minutes | Curated, changes daily |
| Rate limit counters | Exactly the window size | 1 minute window = 60s TTL |
| JWT token blacklist | Match token expiry | Precision required |

### 6.3 Adding TTL Jitter to Prevent Synchronized Expiry

```kotlin
@Service
class CacheService(private val redisTemplate: RedisTemplate<String, Any>) {
    
    private val random = Random(System.currentTimeMillis())
    
    // Add ±10% random jitter to TTL to prevent mass simultaneous expiry
    fun setWithJitter(key: String, value: Any, baseTtlSeconds: Long) {
        val jitter = (baseTtlSeconds * 0.1 * (random.nextDouble() * 2 - 1)).toLong()
        val finalTtl = baseTtlSeconds + jitter
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(finalTtl))
    }
}
```

This ensures that even if 1 million keys were set at the same time, they expire over a spread window rather than all at once.

---

## 7. Cache Stampede (Thundering Herd Problem)

### 7.1 What Happens

```
10,000 concurrent users request product:42
  → TTL expired simultaneously for all 10,000
  → All 10,000 get cache MISS
  → All 10,000 fire database queries simultaneously
  → DB CPU hits 100%, queries queue up
  → Response times → 5-30 seconds
  → DB falls over → 500 errors
  → Cache never repopulates (DB is down)
  → Cascading failure
```

This is called a **thundering herd** or **cache stampede**. It's one of the most dangerous production failure modes.

### 7.2 Solution 1 — Mutex Lock (Only One Request Refreshes)

```kotlin
@Service
class StampedeLockCacheService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val productRepository: ProductRepository
) {
    private val lockTimeoutSeconds = 5L
    private val lockRetryDelayMs = 50L

    fun getProduct(productId: UUID): ProductDto {
        val cacheKey = "product:$productId"
        val lockKey = "lock:product:$productId"
        
        // Fast path — try cache first
        (redisTemplate.opsForValue().get(cacheKey) as? ProductDto)?.let { return it }
        
        // Cache miss — try to acquire mutex lock
        val acquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "1", Duration.ofSeconds(lockTimeoutSeconds))
        
        if (acquired == true) {
            // This instance won the lock — fetch from DB and populate cache
            return try {
                val product = productRepository.findById(productId)
                    .orElseThrow { ResourceNotFoundException("Not found") }
                val dto = product.toDto()
                redisTemplate.opsForValue().set(cacheKey, dto, Duration.ofHours(1))
                dto
            } finally {
                redisTemplate.delete(lockKey)  // Release lock
            }
        } else {
            // Another instance holds the lock — wait and retry
            Thread.sleep(lockRetryDelayMs)
            // Recursive retry (add max retry count in production)
            return getProduct(productId)
        }
    }
}
```

> [!NOTE]
> The mutex approach introduces latency for all waiting threads. For extremely high traffic APIs, use **probabilistic early expiration** instead — it's stateless and more elegant.

### 7.3 Solution 2 — Probabilistic Early Expiration (XFetch Algorithm)

The idea: don't wait for the TTL to expire — proactively refresh the cache slightly BEFORE expiry with some probability that increases as expiry approaches. Only one thread will refresh.

```kotlin
@Service
class XFetchCacheService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val productRepository: ProductRepository
) {
    private val beta = 1.0  // Tuning parameter: 1.0 is standard, higher = refresh sooner
    
    data class CachedEntry<T>(
        val value: T,
        val expiryEpochMs: Long,
        val computeTimeMs: Long  // How long it took to compute this value
    )
    
    fun getProductWithXFetch(productId: UUID): ProductDto {
        val cacheKey = "product:xfetch:$productId"
        
        val entry = redisTemplate.opsForValue().get(cacheKey) as? CachedEntry<ProductDto>
        
        if (entry != null) {
            val now = System.currentTimeMillis()
            val remainingMs = entry.expiryEpochMs - now
            
            // XFetch probability: as TTL decreases, probability of early refresh increases
            // P(refresh) = exp(-remainingMs / (beta * computeTimeMs)) * beta * computeTimeMs / remainingMs
            val refreshProbability = Math.exp(-remainingMs.toDouble() / (beta * entry.computeTimeMs.toDouble()))
            
            if (Math.random() > refreshProbability) {
                return entry.value  // Serve from cache (most of the time)
            }
            // else: probabilistically decided to refresh early
        }
        
        // Compute fresh value
        val startMs = System.currentTimeMillis()
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Not found") }
        val dto = product.toDto()
        val computeTimeMs = System.currentTimeMillis() - startMs
        
        val ttlSeconds = 3600L
        val newEntry = CachedEntry(
            value = dto,
            expiryEpochMs = System.currentTimeMillis() + (ttlSeconds * 1000),
            computeTimeMs = computeTimeMs
        )
        
        redisTemplate.opsForValue().set(cacheKey, newEntry, Duration.ofSeconds(ttlSeconds))
        return dto
    }
}
```

---

## 8. Cache Invalidation — The Hardest Problem

> Phil Karlton famously said: *"There are only two hard things in computer science: cache invalidation and naming things."* He was right. This section is why.

### 8.1 The Invalidation Problem

When you update a product in PostgreSQL:
- `product:42` key in Redis still has the old data for up to TTL seconds
- Users querying `product:42` get stale price / name / stock level
- For pricing or inventory: this directly impacts business

### 8.2 Strategy 1 — TTL-Based Expiry (Simplest, Always Stale)

Do nothing. Wait for TTL to expire. Simple. Correct-ish within TTL window.

Use when: data staleness within TTL window is acceptable (product descriptions, category names).

### 8.3 Strategy 2 — Active Invalidation via @CacheEvict

```kotlin
@Service
class ProductService {

    @Transactional
    @CacheEvict(cacheNames = ["products"], key = "#productId")
    fun updateProductPrice(productId: UUID, newPrice: BigDecimal) {
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Not found") }
        product.price = newPrice
        productRepository.save(product)
        // @CacheEvict fires AFTER method returns — cache entry removed
        // Next request will be a cache miss → fetches fresh data from DB
    }
    
    // Evict multiple related caches at once
    @Caching(evict = [
        CacheEvict(cacheNames = ["products"], key = "#productId"),
        CacheEvict(cacheNames = ["productsByCategory"], key = "#product.categoryId")
    ])
    fun updateProduct(productId: UUID, request: UpdateProductRequest): ProductDto {
        // ...
    }
}
```

> [!WARNING]
> `@CacheEvict` only evicts from the LOCAL cache manager's view of Redis. If you have a cache key pattern like `products:page:1`, `products:page:2`, etc., you cannot use a single `@CacheEvict` to evict all pages. You need Redis `SCAN + DEL` pattern:

```kotlin
fun evictProductPageCaches(categoryId: UUID) {
    val pattern = "sentinel:products:category:$categoryId:*"
    var cursor = ScanCursor.INITIAL
    
    do {
        val scanResult = redisTemplate.scan(
            ScanOptions.scanOptions().match(pattern).count(100).build()
        )
        val keys = scanResult.toList()
        if (keys.isNotEmpty()) {
            redisTemplate.delete(keys)
        }
        cursor = scanResult as ScanCursor
    } while (!cursor.isFinished)
}
```

> [!CAUTION]
> **Never use `KEYS *` pattern in production Redis.** `KEYS` is a blocking operation — it blocks the entire Redis server while scanning. On a Redis instance with millions of keys, this can block for seconds. Always use `SCAN` which is cursor-based and non-blocking.

### 8.4 Strategy 3 — Event-Driven Invalidation (Production Grade)

For distributed systems where multiple services may cache the same data:

```kotlin
// Publisher: when product is updated, publish invalidation event
@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    @Transactional
    fun updateProduct(productId: UUID, request: UpdateProductRequest): Product {
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Not found") }
        product.name = request.name
        product.price = request.price
        val saved = productRepository.save(product)
        
        // Publish event AFTER transaction commits
        eventPublisher.publishEvent(ProductUpdatedEvent(productId = productId))
        
        return saved
    }
}

// Redis Pub/Sub invalidation — notifies ALL instances
@Component
class CacheInvalidationPublisher(private val redisTemplate: RedisTemplate<String, Any>) {
    
    fun publishInvalidation(cacheKey: String) {
        redisTemplate.convertAndSend("cache:invalidations", cacheKey)
    }
}

@Component
class CacheInvalidationListener(
    private val cacheManager: CacheManager
) : MessageListener {
    
    override fun onMessage(message: Message, pattern: ByteArray?) {
        val cacheKey = message.body.toString(Charsets.UTF_8)
        // Evict from local cache manager
        cacheManager.cacheNames.forEach { cacheName ->
            cacheManager.getCache(cacheName)?.evict(cacheKey)
        }
    }
}
```

---

## 9. Distributed Cache vs In-Process Cache — Why HashMap Is Wrong

### 9.1 The Multi-Instance Problem

In development, you run ONE instance of your app. `ConcurrentHashMap` as a cache works fine.

In production, you run 5-20 instances behind a load balancer. Each instance has its own in-memory `HashMap`. Now:

```
User A updates their profile → hits Instance 1 → Instance 1's cache updated
User A next request → hits Instance 3 (load balanced) → Instance 3 has STALE data
User A sees their OLD profile despite just updating it
```

This is a **cache coherence problem**. The solution: **centralized cache (Redis)** shared by all instances.

```
User A updates profile → hits Instance 1 → writes to Redis → Redis is shared
User A next request → hits Instance 3 → reads from Redis → gets fresh data ✓
```

### 9.2 Local Cache as L1, Redis as L2

For ultra-high-frequency reads (millions/minute) where Redis latency (~1-2ms) is still too much, use a **two-tier cache**:

```kotlin
@Service
class TwoTierCacheService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val productRepository: ProductRepository
) {
    // L1: in-process cache (Caffeine) — ~0.01ms, but per-instance
    private val localCache: Cache<String, ProductDto> = Caffeine.newBuilder()
        .maximumSize(1000)              // Only keep 1000 most recent items
        .expireAfterWrite(30, TimeUnit.SECONDS)  // Short TTL — stale window is small
        .build()
    
    fun getProduct(productId: UUID): ProductDto {
        val key = "product:$productId"
        
        // L1 check (fastest — 0.01ms)
        localCache.getIfPresent(key)?.let { return it }
        
        // L2 check (Redis — ~1ms)
        (redisTemplate.opsForValue().get(key) as? ProductDto)?.let { dto ->
            localCache.put(key, dto)  // Populate L1
            return dto
        }
        
        // DB fetch (slowest — 10-50ms)
        val dto = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Not found") }
            .toDto()
        
        redisTemplate.opsForValue().set(key, dto, Duration.ofHours(1))  // Populate L2
        localCache.put(key, dto)  // Populate L1
        
        return dto
    }
}
```

> [!NOTE]
> For the two-tier cache to work safely, your L1 cache TTL MUST be much shorter than L2 (Redis) TTL. L1 stale window = 30 seconds maximum. Otherwise invalidation events won't propagate in time.

---

## 10. Redis Data Structures for Advanced Production Patterns

### 10.1 Sorted Sets for Leaderboards

```kotlin
@Service
class LeaderboardService(private val redisTemplate: RedisTemplate<String, Any>) {
    
    private val LEADERBOARD_KEY = "leaderboard:weekly"
    
    fun addScore(userId: Long, score: Double) {
        redisTemplate.opsForZSet().add(LEADERBOARD_KEY, "user:$userId", score)
    }
    
    fun incrementScore(userId: Long, delta: Double) {
        redisTemplate.opsForZSet().incrementScore(LEADERBOARD_KEY, "user:$userId", delta)
    }
    
    fun getTopN(n: Long): List<LeaderboardEntry> {
        return redisTemplate.opsForZSet()
            .reverseRangeWithScores(LEADERBOARD_KEY, 0, n - 1)
            ?.map { entry ->
                LeaderboardEntry(
                    userId = entry.value.toString().removePrefix("user:").toLong(),
                    score = entry.score ?: 0.0,
                    rank = redisTemplate.opsForZSet().reverseRank(LEADERBOARD_KEY, entry.value!!)!! + 1
                )
            } ?: emptyList()
    }
    
    fun getUserRank(userId: Long): Long? {
        return redisTemplate.opsForZSet()
            .reverseRank(LEADERBOARD_KEY, "user:$userId")
            ?.plus(1)  // Convert 0-indexed to 1-indexed
    }
}
```

### 10.2 Pipeline Batching for Performance

```kotlin
@Service
class BulkCacheService(private val redisTemplate: RedisTemplate<String, Any>) {
    
    // BAD: N round trips
    fun getProductsBad(productIds: List<UUID>): Map<UUID, ProductDto?> {
        return productIds.associateWith { id ->
            redisTemplate.opsForValue().get("product:$id") as? ProductDto
        }
    }
    
    // GOOD: 1 round trip using pipeline (pipelining)
    fun getProductsGood(productIds: List<UUID>): Map<UUID, ProductDto?> {
        val keys = productIds.map { "product:$it" }
        
        val values = redisTemplate.executePipelined { connection ->
            keys.forEach { key ->
                connection.stringCommands().get(key.toByteArray())
            }
            null
        }
        
        return productIds.zip(values).associate { (id, value) ->
            id to (value as? ProductDto)
        }
    }
    
    // Multi-GET using mGet for even simpler approach
    fun getProductsMGet(productIds: List<UUID>): Map<UUID, ProductDto?> {
        val keys = productIds.map { "product:$it" }
        val values = redisTemplate.opsForValue().multiGet(keys)
        
        return productIds.zip(values ?: emptyList()).associate { (id, value) ->
            id to (value as? ProductDto)
        }
    }
}
```

### 10.3 Hash for Session Storage

```kotlin
@Service
class SessionService(private val redisTemplate: RedisTemplate<String, Any>) {
    
    fun createSession(userId: Long, sessionId: String, userRole: String, deviceInfo: String) {
        val key = "session:$sessionId"
        val sessionData = mapOf(
            "userId" to userId,
            "role" to userRole,
            "deviceInfo" to deviceInfo,
            "createdAt" to Instant.now().epochSecond,
            "lastActive" to Instant.now().epochSecond
        )
        
        redisTemplate.opsForHash<String, Any>().putAll(key, sessionData)
        redisTemplate.expire(key, Duration.ofDays(7))
    }
    
    fun refreshSession(sessionId: String): Boolean {
        val key = "session:$sessionId"
        if (!redisTemplate.hasKey(key)) return false
        
        redisTemplate.opsForHash<String, Any>().put(key, "lastActive", Instant.now().epochSecond)
        redisTemplate.expire(key, Duration.ofDays(7))  // Reset TTL
        return true
    }
    
    fun getUserIdFromSession(sessionId: String): Long? {
        val key = "session:$sessionId"
        return redisTemplate.opsForHash<String, Any>().get(key, "userId") as? Long
    }
    
    fun invalidateSession(sessionId: String) {
        redisTemplate.delete("session:$sessionId")
    }
}
```

---

## 11. Redis Cluster vs Redis Sentinel — When to Use Which

### 11.1 Redis Sentinel

**Purpose**: High availability for a single Redis primary  
**How it works**: 3+ Sentinel processes monitor the primary. If primary fails, Sentinels vote and promote a replica to primary. Clients reconnect to new primary.

```
                 ┌─────────────┐
         ┌──────▶│  Sentinel 1 │
         │       └─────────────┘
         │       ┌─────────────┐
Monitor  ├──────▶│  Sentinel 2 │
         │       └─────────────┘
         │       ┌─────────────┐
         └──────▶│  Sentinel 3 │ ← Quorum: 2/3 to elect new primary
                 └─────────────┘
                 
┌──────────┐      ┌──────────┐      ┌──────────┐
│ Primary  │─────▶│ Replica 1│      │ Replica 2│
│  (reads  │ repl │ (read    │      │ (read    │
│  +writes)│      │  only)   │      │  only)   │
└──────────┘      └──────────┘      └──────────┘
```

**Use Sentinel when**:
- Single primary handles your write volume
- Total data fits in one machine's RAM (< ~100 GB)
- You want HA without sharding complexity
- Small to medium scale (startup → series A/B company)

**Limitation**: All writes go to ONE primary. If write throughput exceeds primary capacity, Sentinel cannot help.

```yaml
# application.yml — Redis Sentinel configuration
spring:
  data:
    redis:
      sentinel:
        master: mymaster        # Sentinel master name
        nodes:
          - sentinel-1:26379
          - sentinel-2:26379
          - sentinel-3:26379
      password: ${REDIS_PASSWORD}
      lettuce:
        pool:
          max-active: 16
```

### 11.2 Redis Cluster

**Purpose**: Horizontal scaling across multiple primaries  
**How it works**: Data is sharded across 16,384 hash slots. Each primary owns a subset of slots. Reads/writes route to the correct primary automatically.

```
Slot 0-5460     Slot 5461-10922     Slot 10923-16383
┌──────────┐    ┌──────────────┐    ┌────────────────┐
│Primary A │    │  Primary B   │    │   Primary C    │
│+Replica A│    │ +Replica B   │    │  +Replica C    │
└──────────┘    └──────────────┘    └────────────────┘
```

**Use Redis Cluster when**:
- Write throughput exceeds a single primary's capacity
- Data volume exceeds single node memory
- You need linear write scaling
- Series B+ company / high-scale platform

**Key difference from Sentinel**: Cluster CAN run multi-key commands ONLY if all keys hash to the same slot. Use **hash tags** `{user:42}:cart` and `{user:42}:session` to force co-location.

```yaml
# application.yml — Redis Cluster configuration
spring:
  data:
    redis:
      cluster:
        nodes:
          - redis-node-1:6379
          - redis-node-2:6379
          - redis-node-3:6379
          - redis-node-4:6379
          - redis-node-5:6379
          - redis-node-6:6379
        max-redirects: 3  # Max redirections for hash slot misses
      password: ${REDIS_PASSWORD}
```

> [!IMPORTANT]
> Lettuce (the default Spring Boot Redis client) supports Redis Cluster natively, including automatic topology refresh when nodes join/leave. **Do NOT use Jedis for Cluster** — Jedis has known connection handling issues under Cluster topology changes. Always use Lettuce for production.

### 11.3 Why Lettuce Over Jedis

| Aspect | Lettuce | Jedis |
|---|---|---|
| Threading model | Netty (non-blocking, single thread per connection) | Blocking per thread |
| Connection pool | Not needed (multiplexed) | Required (pool per thread) |
| Cluster support | Native, auto-topology refresh | Manual, fragile |
| Reactive support | Yes (Project Reactor) | No |
| Spring Boot default | ✅ Yes | ❌ No (manual setup) |
| Performance under high concurrency | Excellent — fewer connections | Degrades — one connection per thread |

Jedis was built for a world of thread-per-request servers (Tomcat blocking I/O). Lettuce was built for modern reactive/async systems. Spring Boot 3 uses Lettuce by default — keep it that way.

---

## 12. Production Pitfall — Data Leaks from Bad Cache Keys

> [!CAUTION]
> This is a real security incident that has happened at multiple companies. Caching user-specific data without proper key namespacing causes one user to see another user's data.

**Scenario**: You cache the "my orders" response for a user.

```kotlin
// DANGEROUS: key doesn't include user context
@Cacheable(cacheNames = ["myOrders"])  // key defaults to method params
fun getMyOrders(): List<OrderDto> {  // No parameter! Same key for all users!
    val userId = SecurityContextHolder.getContext().authentication.principal as Long
    return orderRepository.findByUserId(userId).map { it.toDto() }
}
// Result: first user's orders cached under key "myOrders:SimpleKey []"
// Every subsequent user gets the FIRST user's orders!
```

```kotlin
// CORRECT: always include user ID in cache key for user-specific data
@Cacheable(cacheNames = ["myOrders"], key = "#userId")
fun getMyOrders(userId: Long): List<OrderDto> {
    return orderRepository.findByUserId(userId).map { it.toDto() }
}

// Or use SpEL to extract from Security Context safely
@Cacheable(
    cacheNames = ["myOrders"],
    key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().authentication.name"
)
fun getMyOrders(): List<OrderDto> { /* ... */ }
```

**Key naming convention** for production:
```
sentinel:{service}:{entity}:{id}:{qualifier}

Examples:
sentinel:product:detail:42
sentinel:user:profile:userId:99
sentinel:order:list:userId:99:page:1
sentinel:inventory:stock:productId:42:darkStoreId:7
```

---

## 13. Complete Production Cache Health Monitoring

```kotlin
@RestController
@RequestMapping("/internal/cache")
@PreAuthorize("hasRole('ADMIN')")  // Internal only
class CacheHealthController(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val cacheManager: CacheManager
) {
    
    @GetMapping("/stats")
    fun cacheStats(): CacheStatsDto {
        val info = redisTemplate.execute { connection ->
            connection.serverCommands().info("stats")
        }
        
        val dbSize = redisTemplate.execute { connection ->
            connection.serverCommands().dbSize()
        }
        
        val memoryInfo = redisTemplate.execute { connection ->
            connection.serverCommands().info("memory")
        }
        
        return CacheStatsDto(
            totalKeys = dbSize ?: 0,
            cacheNames = cacheManager.cacheNames.toList(),
            redisInfo = parseRedisInfo(info?.toString() ?: "")
        )
    }
    
    @PostMapping("/evict/{cacheName}")
    fun evictCache(@PathVariable cacheName: String): ResponseEntity<String> {
        cacheManager.getCache(cacheName)?.clear()
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok("Cache '$cacheName' cleared")
    }
    
    private fun parseRedisInfo(info: String): Map<String, String> {
        return info.lines()
            .filter { it.contains(":") && !it.startsWith("#") }
            .associate { line ->
                val (key, value) = line.split(":", limit = 2)
                key.trim() to value.trim()
            }
    }
}
```

---

## 14. Summary — The Caching Decision Tree

```
Is data user-specific?
  YES → Include userId in cache key ALWAYS. No exceptions.
  NO  → Can use shared key

How often does data change?
  < once/hour  → Cache-Aside with 1-2 hour TTL + @CacheEvict on update
  1-10x/minute → Write-Through with 5-15 min TTL
  Every request → Don't cache (or cache aggregation only)

How critical is data freshness?
  Pricing / inventory → 30s-5min TTL, Write-Through, active eviction
  Product content      → 1-12 hour TTL, Cache-Aside
  Static config        → 12-24 hour TTL, manual eviction on change

Architecture:
  Single instance → In-process cache (Caffeine) is fine
  Multiple instances → Redis is REQUIRED for cache coherence
  Ultra-high RPS → Two-tier: Caffeine (L1) + Redis (L2)

Redis topology:
  < 100GB data, writes on single node → Redis Sentinel
  > 100GB or write-sharding needed   → Redis Cluster

Client:
  ALWAYS use Lettuce (Spring Boot default)
  NEVER switch to Jedis in new projects
```

> [!NOTE]
> Caching is the highest-leverage performance optimization available to you. A well-tuned cache can make a mediocre database-heavy application serve millions of users. A badly tuned cache (wrong TTLs, stampede vulnerability, user data leaks) will cause production incidents and security breaches. Get the fundamentals right first: correct key namespacing, appropriate TTLs with jitter, active eviction on writes, and Redis for multi-instance deployments.
