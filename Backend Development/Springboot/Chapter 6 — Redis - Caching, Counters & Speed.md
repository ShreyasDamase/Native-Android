# Backend Engineering with Spring Boot & Kotlin

## The HireStory Builder's Guide

---

# Chapter 6 — Redis: Caching, Counters & Speed

### _Making your API fast — and keeping it fast under real load_

---

## 6.1 The Problem This Chapter Solves

Your API works correctly. Now let us talk about what happens under load.

The HireStory feed endpoint currently does this on every request:

```
GET /api/interviews?page=0&size=20
        ↓
Query 1: SELECT id FROM interviews WHERE status = 'PUBLISHED' ORDER BY added_at DESC LIMIT 20
Query 2: SELECT * FROM interviews JOIN companies JOIN tags WHERE id IN (...)
Query 3: SELECT interview_id FROM bookmarks WHERE user_id = 5 AND interview_id IN (...)
```

Three database queries. Each one takes 5–20ms. Total: 15–60ms per request.

Now imagine 500 users loading the feed at the same time. That is 1,500 database queries per second for a page that almost never changes. Your database slows down. Your connection pool fills up. Response times climb. The app feels slow.

**Redis solves this.** Instead of hitting the database every time, you store the result in Redis the first time and return it instantly on every subsequent request. Redis answers in under 1ms. 500 users loading the same feed page: 1 database query, 499 Redis reads.

This chapter covers everything Redis does in HireStory:

- Feed caching — reducing database load by 95%
- Read counter — the free tier enforcement from Chapter 5, done properly
- URL deduplication — the crawler never processes the same URL twice
- Shorts sequence — pre-generated swipe order for each user
- Session data — anything fast and temporary

---

## 6.2 What Redis Is — The Mental Model

Redis is an **in-memory key-value store**. Everything it holds lives in RAM. RAM is 100x faster than disk. That is why Redis is so fast.

```
PostgreSQL: Data on disk → read from disk → 5-20ms
Redis:      Data in RAM  → read from RAM  → 0.1-1ms
```

The trade-off: RAM is limited and volatile. If Redis restarts, data is lost (unless you enable persistence, which you do not need for HireStory's use cases). This is why you use Redis for data that:

- Can be regenerated from the database
- Is temporary by nature (counters that reset monthly)
- Changes often and does not need permanent storage

You never replace your database with Redis. They work together. PostgreSQL is the source of truth. Redis is the fast cache in front of it.

### Key-Value Store

Everything in Redis is a key and a value:

```
Key: "feed:page:0:size:20"         Value: "[{interview JSON...}]"
Key: "reads:5:2024-01"             Value: "17"
Key: "crawled:abc123def456"        Value: "1"
Key: "company:1"                   Value: "{company JSON...}"
```

Keys are strings. Values can be strings, numbers, lists, sets, hashes, or sorted sets. For HireStory, you mostly use strings (for cached JSON) and numbers (for counters).

### TTL — Time To Live

Every Redis key can have a TTL — a time after which it automatically disappears.

```
"feed:page:0" → TTL 5 minutes
After 5 minutes, Redis deletes it automatically
Next request hits the database, result is cached again for another 5 minutes
```

TTL is how you prevent stale data. The feed cache expires in 5 minutes, so at most 5 minutes of old data is served. The read counter for January 2024 expires in February — you never manually clean it up.

---

## 6.3 Redis Configuration

Your `application.yml` already has the Redis connection from Chapter 1. Now add the full configuration:

```yaml
# application.yml

spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
      lettuce:                        # Lettuce is the Redis client Spring Boot uses
        pool:
          max-active: 10              # Max concurrent Redis connections
          max-idle: 5
          min-idle: 2
          max-wait: 1000ms            # Wait max 1s to get a connection from pool
```

Now create the Redis configuration bean:

```kotlin
// src/main/kotlin/com/example/hirestory/config/RedisConfig.kt

package com.example.hirestory.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
@EnableCaching   // Activates Spring's @Cacheable, @CacheEvict, @CachePut annotations
class RedisConfig {

    // ── ObjectMapper for Redis serialisation ──────────────────────────
    // Separate from your main ObjectMapper — Redis needs type information embedded in JSON
    @Bean("redisObjectMapper")
    fun redisObjectMapper(): ObjectMapper = ObjectMapper().apply {
        registerModule(JavaTimeModule())       // Handle LocalDateTime
        registerModule(kotlinModule())         // Handle Kotlin data classes
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        // Embeds the class type in JSON so Redis knows how to deserialise
        activateDefaultTyping(
            polymorphicTypeValidator,
            ObjectMapper.DefaultTyping.NON_FINAL
        )
    }

    // ── StringRedisTemplate ───────────────────────────────────────────
    // For simple string operations: counters, flags, deduplication
    // Keys: String, Values: String
    // This is what you use for read counters and URL dedup
    @Bean
    fun stringRedisTemplate(factory: RedisConnectionFactory): StringRedisTemplate {
        return StringRedisTemplate(factory)
    }

    // ── RedisTemplate<String, Any> ────────────────────────────────────
    // For storing complex objects: cached DTOs, lists, etc.
    @Bean
    fun redisTemplate(
        factory: RedisConnectionFactory,
        redisObjectMapper: ObjectMapper
    ): RedisTemplate<String, Any> {
        return RedisTemplate<String, Any>().apply {
            connectionFactory = factory
            // Keys are always plain strings
            keySerializer = StringRedisSerializer()
            hashKeySerializer = StringRedisSerializer()
            // Values are JSON (using the type-aware ObjectMapper)
            valueSerializer = GenericJackson2JsonRedisSerializer(redisObjectMapper)
            hashValueSerializer = GenericJackson2JsonRedisSerializer(redisObjectMapper)
            afterPropertiesSet()
        }
    }

    // ── RedisCacheManager ─────────────────────────────────────────────
    // Powers the @Cacheable annotation
    // Defines default TTL and per-cache TTL overrides
    @Bean
    fun cacheManager(
        factory: RedisConnectionFactory,
        redisObjectMapper: ObjectMapper
    ): RedisCacheManager {
        val jsonSerializer = GenericJackson2JsonRedisSerializer(redisObjectMapper)

        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))         // Default TTL: 5 minutes
            .disableCachingNullValues()               // Never cache null results
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
            )

        // Per-cache TTL overrides — some caches expire faster or slower
        val cacheConfigurations = mapOf(
            "companies"         to defaultConfig.entryTtl(Duration.ofHours(1)),
            "feed"              to defaultConfig.entryTtl(Duration.ofMinutes(5)),
            "interview-detail"  to defaultConfig.entryTtl(Duration.ofMinutes(30)),
            "search-results"    to defaultConfig.entryTtl(Duration.ofMinutes(10)),
            "tags"              to defaultConfig.entryTtl(Duration.ofHours(6)),
        )

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }
}
```

---

## 6.4 Spring Cache Abstraction — @Cacheable, @CacheEvict, @CachePut

Spring provides three annotations that add caching without changing your business logic code. The logic is the same — you just add annotations.

### @Cacheable — Cache the Result

```kotlin
@Cacheable(
    cacheNames = ["companies"],     // Which cache bucket to use
    key = "#root.methodName"        // Cache key — "findAll" in this case
)
fun findAll(): List<CompanyDto> {
    // This code only runs if the result is NOT in the cache
    // If it IS in cache, Spring returns the cached value without calling this method
    return companyRepository.findAll().map { it.toDto() }
}
```

What happens on the first call:

1. Spring checks Redis for key `"companies::findAll"`
2. Not found — calls `findAll()` body
3. Stores the result in Redis with key `"companies::findAll"` and TTL 1 hour
4. Returns the result

What happens on every subsequent call (for the next hour):

1. Spring checks Redis for key `"companies::findAll"`
2. Found — returns the cached value immediately
3. `findAll()` body never runs — database never queried

### @CacheEvict — Invalidate the Cache

When data changes, the cached version is stale. Evict it:

```kotlin
@CacheEvict(
    cacheNames = ["companies"],
    allEntries = true               // Remove ALL entries in the "companies" cache
)
@Transactional
fun createCompany(dto: CreateCompanyDto): CompanyDto {
    // After creating a company, the "companies" cache is stale
    // @CacheEvict removes it — next request fetches fresh data
    val company = companyRepository.save(toEntity(dto))
    return company.toDto()
}
```

### @CachePut — Update the Cache

Like `@Cacheable` but always executes the method AND updates the cache:

```kotlin
@CachePut(
    cacheNames = ["interview-detail"],
    key = "#result.slug"            // Use the returned object's slug as the key
)
@Transactional
fun approve(id: Long): InterviewDetailDto {
    // Always runs — updates the database AND puts the result in cache
    interviewRepository.updateStatus(id, InterviewStatus.PUBLISHED, LocalDateTime.now())
    val interview = interviewRepository.findByIdOrNull(id)!!
    return interview.toDetailDto()
}
```

### The Cache Key — How Spring Identifies Cached Results

The cache key determines which cached value to return. Same method with different parameters = different keys = different cached values.

```kotlin
// Each company slug gets its own cache entry
@Cacheable(cacheNames = ["companies"], key = "#slug")
fun findBySlug(slug: String): CompanyDto {
    // "companies::google" → Google's data
    // "companies::amazon" → Amazon's data
    // Two separate cache entries
}

// Feed with different filter combinations gets its own entry
@Cacheable(
    cacheNames = ["feed"],
    key = "T(String).join('-', #page, #size, #companyId ?: 'all', #difficulty ?: 'all', #outcome ?: 'all')"
)
fun getFeed(page: Int, size: Int, companyId: Long?, difficulty: Difficulty?, outcome: Outcome?): SliceResponse<InterviewSummaryDto> {
    // "feed::0-20-all-all-all"     → unfiltered page 0
    // "feed::0-20-1-HARD-OFFER"    → Google, Hard, Offer, page 0
}
```

### When NOT to Use Spring Cache Abstraction

`@Cacheable` is elegant but inflexible. Use `RedisTemplate` directly when you need:

- Custom TTL per individual key (not per cache name)
- Increment/decrement operations (counters)
- Checking existence without fetching value
- Atomic operations
- Complex data structures (lists, sets)

For HireStory: read counters, URL dedup, and shorts sequences use `StringRedisTemplate` directly. Company list and interview detail use `@Cacheable`.

---

## 6.5 The Cache Service — HireStory's Redis Layer

Rather than scattering Redis logic across all your services, put it in one dedicated service:

```kotlin
// src/main/kotlin/com/example/hirestory/service/CacheService.kt

package com.example.hirestory.service

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class CacheService(private val redis: StringRedisTemplate) {

    private val log = LoggerFactory.getLogger(CacheService::class.java)

    // ── Read Counter ──────────────────────────────────────────────────

    // Increment and return the new count
    fun incrementReadCount(userId: Long): Long {
        val key = readCountKey(userId)
        val count = redis.opsForValue().increment(key) ?: 0L
        // Set TTL on first increment — expires at the end of next month
        // (gives a buffer so the counter is available at month boundaries)
        if (count == 1L) {
            redis.expire(key, Duration.ofDays(45))
        }
        return count
    }

    fun getReadCount(userId: Long): Int {
        return redis.opsForValue().get(readCountKey(userId))?.toIntOrNull() ?: 0
    }

    fun resetReadCount(userId: Long) {
        redis.delete(readCountKey(userId))
    }

    private fun readCountKey(userId: Long): String {
        val month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        return "reads:$userId:$month"
    }

    // ── URL Deduplication (for crawler) ──────────────────────────────

    // Returns true if the URL has been seen before (already crawled)
    // Returns false if this URL is new — also marks it as seen
    fun isUrlAlreadyCrawled(url: String): Boolean {
        val key = "crawled:${url.sha256()}"
        // SETNX — Set if Not eXists: atomically sets and returns true if key was new
        val wasNew = redis.opsForValue().setIfAbsent(key, "1", Duration.ofDays(30))
        return wasNew != true    // wasNew = true means it was NOT already there
    }

    // ── Trending Searches ─────────────────────────────────────────────

    // Record a search query — used to build trending list
    fun recordSearch(query: String) {
        val key = "trending:searches"
        // Sorted set: increment the score of this query
        // After many searches, popular queries have high scores
        redis.opsForZSet().incrementScore(key, query.lowercase().trim(), 1.0)
        redis.expire(key, Duration.ofHours(24))
    }

    // Get the top N trending search queries
    fun getTrendingSearches(limit: Long = 10): List<String> {
        val key = "trending:searches"
        return redis.opsForZSet()
            .reverseRange(key, 0, limit - 1)    // Highest scores first
            ?.toList() ?: emptyList()
    }

    // ── Shorts Sequence ───────────────────────────────────────────────

    // Store the pre-generated sequence of interview IDs for a user's Shorts feed
    fun setShortSequence(userId: Long, interviewIds: List<Long>) {
        val key = "shorts:$userId"
        val values = interviewIds.map { it.toString() }.toTypedArray()
        redis.delete(key)
        if (values.isNotEmpty()) {
            redis.opsForList().rightPushAll(key, *values)
            redis.expire(key, Duration.ofHours(24))
        }
    }

    // Pop the next interview ID from the user's Shorts sequence
    fun popNextShort(userId: Long): Long? {
        val key = "shorts:$userId"
        return redis.opsForList().leftPop(key)?.toLongOrNull()
    }

    // How many Shorts does this user have queued?
    fun getShortSequenceLength(userId: Long): Long {
        return redis.opsForList().size("shorts:$userId") ?: 0L
    }

    // ── Generic cache helpers ─────────────────────────────────────────

    fun delete(key: String) {
        redis.delete(key)
    }

    fun deleteByPattern(pattern: String) {
        // Use with caution on large datasets — SCAN is safer than KEYS in production
        val keys = redis.keys(pattern)
        if (keys.isNotEmpty()) {
            redis.delete(keys)
        }
    }

    fun exists(key: String): Boolean = redis.hasKey(key) == true
}

// SHA-256 hash of a string — used to create fixed-length keys from URLs
private fun String.sha256(): String {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    return digest.digest(toByteArray())
        .joinToString("") { "%02x".format(it) }
        .take(16)   // First 16 chars is enough for dedup — collision probability negligible
}
```

---

## 6.6 Caching the Company List

The company list changes rarely. Cache it for one hour:

```kotlin
// src/main/kotlin/com/example/hirestory/service/CompanyService.kt

package com.example.hirestory.service

import com.example.hirestory.dto.CompanyDto
import com.example.hirestory.dto.toDto
import com.example.hirestory.exception.ResourceNotFoundException
import com.example.hirestory.repository.CompanyRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CompanyService(private val companyRepository: CompanyRepository) {

    // ── Cached reads ──────────────────────────────────────────────────

    @Cacheable(cacheNames = ["companies"], key = "'all'")
    @Transactional(readOnly = true)
    fun findAll(): List<CompanyDto> {
        // Only runs on first call or after cache is evicted
        return companyRepository.findAllByOrderByInterviewCountDesc()
            .map { it.toDto() }
    }

    @Cacheable(cacheNames = ["companies"], key = "#slug")
    @Transactional(readOnly = true)
    fun findBySlug(slug: String): CompanyDto {
        return companyRepository.findBySlug(slug)?.toDto()
            ?: throw ResourceNotFoundException("Company", slug)
    }

    // ── Cache-evicting writes ─────────────────────────────────────────

    @CacheEvict(cacheNames = ["companies"], allEntries = true)
    @Transactional
    fun create(name: String, logoUrl: String?): CompanyDto {
        // After creating a new company, evict the entire companies cache
        // Next request to findAll() will fetch fresh data including the new company
        val company = companyRepository.save(
            Company(name = name, slug = name.toSlug(), logoUrl = logoUrl)
        )
        return company.toDto()
    }

    @CacheEvict(cacheNames = ["companies"], allEntries = true)
    @Transactional
    fun updateLogo(slug: String, logoUrl: String): CompanyDto {
        val company = companyRepository.findBySlug(slug)
            ?: throw ResourceNotFoundException("Company", slug)
        company.logoUrl = logoUrl
        return companyRepository.save(company).toDto()
    }
}
```

> **💡 Why `allEntries = true` instead of evicting by key?** When you add a new company, the `findAll()` list is stale. But which key holds `findAll()`? It is `"companies::all"`. If you also have individual company entries cached under their slugs, you would have to evict each one separately. `allEntries = true` removes everything in the "companies" cache at once. Simpler and safer — costs one extra Redis operation, saves multiple.

---

## 6.7 Caching the Interview Feed

The feed is the highest-traffic endpoint in HireStory. Each combination of filters is a different cache entry:

```kotlin
// src/main/kotlin/com/example/hirestory/service/InterviewService.kt

@Service
class InterviewService(
    private val interviewRepository: InterviewRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val readTrackingService: ReadTrackingService
) {

    // The feed is cached per filter combination
    // Anonymous users (no currentUser) get cached responses
    // Logged-in users get the same feed but with isBookmarked set — NOT cached per user
    // (caching per user would require one cache entry per user per filter combo — too much memory)
    @Cacheable(
        cacheNames = ["feed"],
        key = "T(String).valueOf(#page) + '-' + #size + '-' + (#companyId ?: 'all') + '-' + (#difficulty ?: 'all') + '-' + (#outcome ?: 'all')",
        condition = "#currentUser == null"
        // Only cache for anonymous requests
        // Authenticated requests need personalised isBookmarked flags
        // Those cannot be cached globally
    )
    @Transactional(readOnly = true)
    fun getFeed(
        page: Int,
        size: Int,
        companyId: Long?,
        difficulty: Difficulty?,
        outcome: Outcome?,
        currentUser: User?
    ): SliceResponse<InterviewSummaryDto> {

        val pageable = PageRequest.of(page, size)
        val idSlice = interviewRepository.findFeedIds(
            companyId = companyId,
            difficulty = difficulty,
            outcome = outcome,
            pageable = pageable
        )

        if (idSlice.isEmpty) return SliceResponse(emptyList(), false, page, size)

        val interviews = interviewRepository.findByIdsWithDetails(idSlice.content)

        // For logged-in users, add their bookmark status
        val bookmarkedIds = currentUser?.let {
            bookmarkRepository.findBookmarkedInterviewIds(it, idSlice.content).toSet()
        } ?: emptySet()

        val dtos = idSlice.content.mapNotNull { id ->
            interviews.find { it.id == id }?.toSummaryDto()
        }

        return SliceResponse(dtos, idSlice.hasNext(), page, size)
    }

    // When a new interview is published, evict all feed cache entries
    // Different caches need different eviction strategies
    @CacheEvict(cacheNames = ["feed"], allEntries = true)
    fun evictFeedCache() {
        // Called from AdminService.approve() after publishing an interview
        // Empty method — the annotation does the work
    }

    @Cacheable(
        cacheNames = ["interview-detail"],
        key = "#slug",
        condition = "#currentUser == null"
    )
    @Transactional
    fun getBySlug(slug: String, currentUser: User?): InterviewDetailDto {
        val interview = interviewRepository.findPublishedBySlugWithDetails(slug)
            ?: throw ResourceNotFoundException("Interview", slug)

        if (currentUser != null) {
            readTrackingService.trackRead(currentUser, interview)
        }

        val isBookmarked = currentUser != null &&
            bookmarkRepository.existsByUserAndInterview(currentUser, interview)

        return interview.toDetailDto(isBookmarked = isBookmarked)
    }
}
```

### Evicting Feed Cache When an Interview Is Published

```kotlin
// src/main/kotlin/com/example/hirestory/service/AdminService.kt

@Service
class AdminService(
    private val interviewRepository: InterviewRepository,
    private val companyRepository: CompanyRepository,
    private val interviewService: InterviewService   // For cache eviction
) {

    @Transactional
    fun approve(id: Long): InterviewDetailDto {
        val interview = interviewRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Interview", id)

        interviewRepository.updateStatus(id, InterviewStatus.PUBLISHED, LocalDateTime.now())
        companyRepository.incrementInterviewCount(interview.company.id!!)

        // Evict the feed cache — new interview should appear immediately
        interviewService.evictFeedCache()

        val updated = interviewRepository.findByIdOrNull(id)!!
        return updated.toDetailDto()
    }
}
```

---

## 6.8 The Read Counter — Proper Redis Implementation

In Chapter 5 you wrote a basic `ReadTrackingService`. Now make it production-grade with proper Redis operations:

```kotlin
// src/main/kotlin/com/example/hirestory/service/ReadTrackingService.kt

package com.example.hirestory.service

import com.example.hirestory.config.HireStoryProperties
import com.example.hirestory.entity.Interview
import com.example.hirestory.entity.ReadHistory
import com.example.hirestory.entity.User
import com.example.hirestory.exception.PaywallException
import com.example.hirestory.repository.ReadHistoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReadTrackingService(
    private val readHistoryRepository: ReadHistoryRepository,
    private val cacheService: CacheService,
    private val properties: HireStoryProperties
) {

    @Transactional
    fun trackRead(user: User, interview: Interview) {

        // Step 1: Has this user already read this interview?
        // Check database — permanent record of every read ever
        val alreadyRead = readHistoryRepository.existsByUserAndInterview(user, interview)
        if (alreadyRead) {
            // User is re-reading — do not count it again, just let them read
            return
        }

        // Step 2: Enforce free tier limit for non-premium users
        if (!user.isPremium) {
            val currentCount = cacheService.getReadCount(user.id!!)
            val limit = properties.freeTier.monthlyReadLimit

            if (currentCount >= limit) {
                throw PaywallException(readsUsed = currentCount, limit = limit)
            }
        }

        // Step 3: Record the read in the database (permanent history)
        readHistoryRepository.save(ReadHistory(user = user, interview = interview))

        // Step 4: Increment the Redis counter for this month
        // This is the fast path — checking the counter next time costs 1ms not 20ms
        cacheService.incrementReadCount(user.id!!)
    }

    fun getMonthlyReadCount(user: User): Int {
        return cacheService.getReadCount(user.id!!)
    }

    fun getRemainingReads(user: User): Int {
        if (user.isPremium) return Int.MAX_VALUE
        val used = getMonthlyReadCount(user)
        return maxOf(0, properties.freeTier.monthlyReadLimit - used)
    }
}
```

---

## 6.9 URL Deduplication — The Crawler Guard

The crawler (Chapter 10) will discover URLs from Reddit, GeeksForGeeks, and other sources. The same URL might appear multiple times across different crawl runs. Redis prevents processing duplicates:

```kotlin
// src/main/kotlin/com/example/hirestory/service/CrawlerService.kt
// (Preview — full implementation in Chapter 10)

@Service
class CrawlerService(
    private val crawlJobRepository: CrawlJobRepository,
    private val cacheService: CacheService
) {

    fun queueUrlForCrawling(url: String): Boolean {
        // Check Redis first — fast O(1) operation
        if (cacheService.isUrlAlreadyCrawled(url)) {
            return false   // Already seen — skip
        }

        // Also check database — Redis might have been cleared
        if (crawlJobRepository.existsBySourceUrl(url)) {
            return false   // In database — skip
        }

        // New URL — create a crawl job
        crawlJobRepository.save(
            CrawlJob(sourceUrl = url, status = CrawlStatus.PENDING)
        )
        return true   // Queued for processing
    }
}
```

`isUrlAlreadyCrawled` uses `SETNX` (Set if Not Exists) which is atomic. Even if two threads try to queue the same URL simultaneously, only one succeeds. The Redis operation guarantees this.

---

## 6.10 Cache-Aside Pattern — The Professional Standard

The patterns above use Spring's `@Cacheable`. But sometimes you need more control. The cache-aside pattern gives it to you:

```
Read request comes in
        ↓
Check Redis cache
        ↓
Cache HIT → return cached value (fast path — 1ms)
        ↓
Cache MISS → query database → store in Redis → return value
```

```kotlin
@Service
class TagService(
    private val tagRepository: TagRepository,
    private val redisTemplate: RedisTemplate<String, Any>
) {
    private val TTL = Duration.ofHours(6)

    fun getAllTags(): List<TagDto> {
        val cacheKey = "tags:all"

        // Step 1: Try cache first
        @Suppress("UNCHECKED_CAST")
        val cached = redisTemplate.opsForValue().get(cacheKey) as? List<TagDto>
        if (cached != null) {
            return cached    // Cache hit — return immediately
        }

        // Step 2: Cache miss — fetch from database
        val tags = tagRepository.findAll()
            .sortedBy { it.name }
            .map { TagDto(id = it.id!!, name = it.name) }

        // Step 3: Store in cache for next time
        redisTemplate.opsForValue().set(cacheKey, tags, TTL)

        return tags
    }

    fun evictTagCache() {
        redisTemplate.delete("tags:all")
    }
}
```

Use this pattern when:

- You need conditional caching logic (`@Cacheable` cannot express)
- You need the cached value and the database value for comparison
- You want to log cache hits and misses explicitly
- You are storing a complex data structure that `@Cacheable` handles awkwardly

---

## 6.11 Shorts Sequence — Pre-Generated Swipe Feed

The Shorts screen shows one interview at a time in a swipe format. The sequence of which interview comes next is pre-generated and stored in Redis as a list per user:

```kotlin
// src/main/kotlin/com/example/hirestory/service/ShortsService.kt

package com.example.hirestory.service

import com.example.hirestory.dto.InterviewSummaryDto
import com.example.hirestory.dto.toSummaryDto
import com.example.hirestory.entity.InterviewStatus
import com.example.hirestory.entity.User
import com.example.hirestory.repository.InterviewRepository
import com.example.hirestory.repository.ReadHistoryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ShortsService(
    private val interviewRepository: InterviewRepository,
    private val readHistoryRepository: ReadHistoryRepository,
    private val cacheService: CacheService
) {
    private val BATCH_SIZE = 20   // Pre-load 20 shorts at a time

    @Transactional(readOnly = true)
    fun getNextShort(currentUser: User): InterviewSummaryDto? {

        // Step 1: Try to get next ID from the user's pre-generated sequence
        var nextId = cacheService.popNextShort(currentUser.id!!)

        // Step 2: If sequence is empty, generate a new one
        if (nextId == null) {
            generateSequenceForUser(currentUser)
            nextId = cacheService.popNextShort(currentUser.id!!) ?: return null
        }

        // Step 3: Load the interview
        val interview = interviewRepository.findByIdsWithDetails(listOf(nextId))
            .firstOrNull() ?: return getNextShort(currentUser)  // Skip deleted interviews

        return interview.toSummaryDto()
    }

    private fun generateSequenceForUser(user: User) {
        // Get IDs the user has already read — exclude them from Shorts
        val readIds = readHistoryRepository.findReadInterviewIdsByUser(user).toSet()

        // Find interviews the user has NOT read, sorted by relevance
        val pageable = PageRequest.of(0, BATCH_SIZE + readIds.size)
        val allRecentIds = interviewRepository.findFeedIds(
            companyId = null,
            difficulty = null,
            outcome = null,
            pageable = pageable
        ).content

        // Filter out already-read interviews and take BATCH_SIZE
        val newIds = allRecentIds
            .filter { it !in readIds }
            .take(BATCH_SIZE)

        if (newIds.isEmpty()) return

        // Shuffle for variety — same user sees different order each time
        val shuffled = newIds.shuffled()

        // Store in Redis — expires in 24 hours
        cacheService.setShortSequence(user.id!!, shuffled)
    }
}
```

---

## 6.12 Handling Redis Failures Gracefully

Redis is fast and reliable, but it can go down. If your app crashes whenever Redis is unavailable, you have a single point of failure. The correct approach: if Redis fails, fall back to the database.

```kotlin
// src/main/kotlin/com/example/hirestory/service/CacheService.kt

@Service
class CacheService(private val redis: StringRedisTemplate) {

    private val log = LoggerFactory.getLogger(CacheService::class.java)

    fun getReadCount(userId: Long): Int {
        return try {
            redis.opsForValue().get(readCountKey(userId))?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            // Redis is unavailable — log and return 0
            // The database check in trackRead() is the safety net
            log.warn("Redis unavailable for read count check: {}", e.message)
            0
        }
    }

    fun incrementReadCount(userId: Long): Long {
        return try {
            val key = readCountKey(userId)
            val count = redis.opsForValue().increment(key) ?: 0L
            if (count == 1L) redis.expire(key, Duration.ofDays(45))
            count
        } catch (e: Exception) {
            log.warn("Redis unavailable for read count increment: {}", e.message)
            0L   // Cannot increment — the database history is still recorded
        }
    }

    fun isUrlAlreadyCrawled(url: String): Boolean {
        return try {
            val key = "crawled:${url.sha256()}"
            redis.opsForValue().setIfAbsent(key, "1", Duration.ofDays(30)) != true
        } catch (e: Exception) {
            log.warn("Redis unavailable for URL dedup check: {}", e.message)
            false   // If Redis is down, fall through to database check
        }
    }
}
```

The pattern is always: try Redis, catch any exception, log it, return a safe default. The database is always your fallback source of truth.

---

## 6.13 Cache Invalidation Strategy for HireStory

Cache invalidation is famously described as one of the hardest problems in computer science. For HireStory, the rules are clear:

|What Changed|What to Evict|When|
|---|---|---|
|New interview published|All feed cache entries|In `AdminService.approve()`|
|Interview detail updated|That interview's cache entry|In `AdminService.update()`|
|New company added|All company cache entries|In `CompanyService.create()`|
|Company logo updated|That company's cache entry + all entries|In `CompanyService.updateLogo()`|
|New tag added|All tag cache entries|In `TagService.create()`|
|User's read count changes|User's read count key|Updated by `CacheService.incrementReadCount()`|

The rule: **evict immediately when the underlying data changes**. Never let stale data sit in the cache intentionally except for the TTL expiry window you chose (5 minutes for feed, 1 hour for companies).

---

## 6.14 Monitoring Your Cache — Are You Getting Hits?

Add cache hit/miss logging to verify your caching is actually working:

```yaml
# application-dev.yml

logging:
  level:
    org.springframework.cache: TRACE    # Logs every cache hit and miss
```

With this setting, you will see in your logs:

```
TRACE o.s.cache.interceptor.CacheInterceptor - Computed cache key 'all' for operation
TRACE o.s.cache.interceptor.CacheInterceptor - Cache entry for key 'all' found in cache 'companies'
```

`found in cache` = hit. `No cache entry` = miss. First request after startup: miss. Every subsequent request for 1 hour: hit.

You can also verify directly in Redis:

```bash
# Connect to Redis CLI
redis-cli

# See all keys
KEYS *

# See a specific cached value
GET "companies::all"

# See TTL remaining on a key (in seconds)
TTL "companies::all"

# Delete a key manually (force cache miss on next request)
DEL "companies::all"

# See memory usage
INFO memory
```

---

## 6.15 Common Mistakes in Chapter 6

### Mistake 1 — Caching mutable user-specific data globally

```kotlin
// ❌ The feed is cached globally but includes user-specific isBookmarked flags
// Every user would see each other's bookmark status
@Cacheable(cacheNames = ["feed"], key = "#page + '-' + #size")
fun getFeed(page: Int, size: Int, currentUser: User?): SliceResponse<InterviewSummaryDto> {
    val dtos = ... // includes isBookmarked based on currentUser
}

// ✅ Only cache anonymous responses — authenticated responses are personal
@Cacheable(
    cacheNames = ["feed"],
    key = "#page + '-' + #size",
    condition = "#currentUser == null"    // Only cache when there is no user
)
fun getFeed(page: Int, size: Int, currentUser: User?): SliceResponse<InterviewSummaryDto> { }
```

### Mistake 2 — Forgetting @EnableCaching

```kotlin
// ❌ @Cacheable annotations do nothing without this
@Configuration
class RedisConfig {
    // Missing @EnableCaching — caching is silently disabled
}

// ✅ Add @EnableCaching to your Redis config or any @Configuration class
@Configuration
@EnableCaching
class RedisConfig { }
```

### Mistake 3 — Calling a @Cacheable method from within the same class

```kotlin
// ❌ Self-invocation bypasses the cache proxy — @Cacheable does nothing
@Service
class CompanyService {

    @Cacheable(cacheNames = ["companies"], key = "'all'")
    fun findAll(): List<CompanyDto> { ... }

    fun doSomething() {
        val companies = findAll()   // Calls the method directly — cache NEVER used
        // Spring's cache works through a proxy class that wraps your bean
        // Internal calls go directly to the method, bypassing the proxy
    }
}

// ✅ Inject the service and call via the proxy
@Service
class CompanyService(
    private val self: CompanyService   // Spring injects the proxy, not 'this'
) {
    fun doSomething() {
        val companies = self.findAll()   // Goes through proxy — cache works
    }
}
// Or better: restructure so you do not need to call @Cacheable methods internally
```

### Mistake 4 — Not setting TTL on Redis keys

```kotlin
// ❌ Key lives forever — Redis memory fills up over time
redis.opsForValue().set("some-key", "some-value")

// ✅ Always set TTL when storing in Redis
redis.opsForValue().set("some-key", "some-value", Duration.ofMinutes(30))
```

### Mistake 5 — Using KEYS command in production

```kotlin
// ❌ KEYS blocks Redis while scanning — freezes your entire cache under load
val keys = redis.keys("feed:*")    // DO NOT use in production with large datasets

// ✅ Use SCAN for production (iterates in small batches, non-blocking)
// Spring Data Redis provides this via RedisTemplate.scan()
val scanOptions = ScanOptions.scanOptions().match("feed:*").count(100).build()
redisTemplate.scan(scanOptions).use { cursor ->
    cursor.forEach { key -> redisTemplate.delete(key) }
}
```

### Mistake 6 — Caching exceptions / null results

```kotlin
// ❌ If the database call throws, @Cacheable might cache the exception behaviour
// Configure this explicitly

// ✅ In RedisCacheConfiguration
.disableCachingNullValues()    // Already in your RedisConfig — never cache null
```

---

## 6.16 HireStory Connection — What You Built in Chapter 6

By the end of Chapter 6, HireStory has a complete caching layer:

- `RedisConfig` — `StringRedisTemplate`, `RedisTemplate<String, Any>`, and `RedisCacheManager` with per-cache TTL overrides
- `CacheService` — centralised Redis operations: read counter, URL dedup, trending searches, Shorts sequence pre-generation
- `CompanyService` — company list and individual companies cached with 1-hour TTL, evicted on any change
- `InterviewService` — feed cached per filter combination for 5 minutes, only for anonymous users. Detail cached for 30 minutes.
- `ReadTrackingService` — free tier enforcement using Redis counter + database history
- `ShortsService` — pre-generated swipe sequence per user, stored as Redis list
- `AdminService` — evicts feed cache when an interview is published
- Graceful Redis failure handling — every Redis operation is wrapped in try-catch

The impact on your API:

- Feed response time: 15–60ms → 1–3ms on cache hit
- Company list: database query on every request → database query once per hour
- Read count check: database query → Redis read (sub-millisecond)

---

## 6.17 Chapter Project — Build It Before You Move On

### What to build

Add caching to your existing project.

**Step 1 — Verify Redis is running**

```bash
redis-cli ping    # Should return PONG
```

If not installed: `brew install redis` on Mac, or use Docker:

```bash
docker run -d -p 6379:6379 redis:7-alpine
```

**Step 2 — Add @EnableCaching and configure RedisCacheManager**

Copy `RedisConfig` from Section 6.3. Verify the app starts without errors.

**Step 3 — Cache the company list**

Add `@Cacheable` to `CompanyService.findAll()`. Test: call `GET /api/companies` twice. Check your SQL logs. First call: SQL query runs. Second call: no SQL query.

**Step 4 — Add @CacheEvict to company creation**

Create a company via direct database insert. Call `GET /api/companies` — old list. Add `@CacheEvict` to the service method. Create company again — cache evicted. Call `GET /api/companies` — new list appears.

**Step 5 — Implement the read counter**

Wire `CacheService.incrementReadCount()` into `ReadTrackingService`. Read an interview 3 times as the same user. Check Redis: `redis-cli GET "reads:1:2024-01"` — should show `1`, not `3`. (Only the first read counts.)

**Step 6 — Test URL dedup**

Call `cacheService.isUrlAlreadyCrawled("https://reddit.com/r/test/abc")` twice. First call: returns `false` (new URL, stored in Redis). Second call: returns `true` (already seen).

### Checkpoint questions — answer before moving on

1. Your `@Cacheable` feed cache is working for anonymous users. A logged-in user loads the feed — why does this NOT use the cache, and what does the client receive instead?
    
2. You publish a new interview via `AdminService.approve()`. The cache is evicted. Two seconds later, 50 users simultaneously request the feed. How many database queries run? (This is called the "cache stampede" problem — just identify it, you do not need to solve it for HireStory.)
    
3. Your `CacheService.incrementReadCount()` wraps the Redis call in try-catch and returns 0 on failure. A user reads their 25th interview. Redis is down so `incrementReadCount` returns 0. `getReadCount` also returns 0. What happens — does the paywall trigger? Is this a problem?
    
4. You add `@Cacheable(cacheNames = ["feed"], key = "#page")` to `getFeed()`. A user requests `?page=0&size=20` and gets 20 results cached. Another user requests `?page=0&size=50`. What does the second user get?
    
5. The `@Cacheable` annotation is on a method in `CompanyService`. Another method inside `CompanyService` calls this method directly. Does caching work? Why or why not?
    

---

_Chapter 7 → RabbitMQ — Async Processing and Notifications_

---

> **Book Progress:** Chapter 6 of 15 complete. Chapters ahead: RabbitMQ · Spring AI · Jsoup/Crawler · Scheduler · Testing · Deployment