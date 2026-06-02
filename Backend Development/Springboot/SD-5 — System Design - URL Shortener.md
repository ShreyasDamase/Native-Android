# SD-5 — System Design: URL Shortener

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> **Context**: The URL shortener is the "Hello World" of system design interviews, but building one that handles 10B+ redirects/day is a real engineering problem. Bitly serves 10 billion redirects per month. TinyURL has been running since 2002. This chapter covers a production-grade implementation you could actually deploy, not toy code.

---

## Requirements Analysis — Being Precise Before Designing

Ambiguous requirements → bad design. Let's be explicit.

### Functional Requirements
1. Given a long URL, return a short URL (7-8 character code)
2. Given a short URL, redirect to the original long URL
3. Custom aliases (vanity URLs): `bit.ly/zepto-groceries` instead of `bit.ly/a3k9mq`
4. Link expiration: configurable TTL per URL
5. Analytics: click count, geo, device, referrer (async, not on the critical redirect path)

### Non-Functional Requirements
```
Scale: 100 million URLs created (total, lifetime)
Traffic: 100 million new URLs / (7 years × 365 days × 86,400 seconds) ≈ 0.45 writes/second
Read:write ratio: 10:1 → 4.5 reads/second... but traffic is not uniform

Peak reads (viral links): A single tweet from a celebrity goes viral.
100,000 people click in 10 minutes = 166 clicks/second for that one URL.
If 1,000 such URLs exist simultaneously → 166,000 redirects/second peak.

Storage:
  100M URLs × avg 200 bytes/record = 20GB for URLs table (fits in RAM of any modern DB)
  Analytics: 100M URLs × 10 clicks/day × 7 years × 100 bytes = ~2.5TB

Availability: 99.99% uptime (redirect should never fail — broken links are unacceptable)
Latency: < 10ms for redirect (user is waiting for the page to load)
```

---

## Key Generation — The Hardest Decision

The short code is the core of this system. Every other decision flows from this.

### Option A: Hash-Based (MD5/SHA256 of URL)

```
long_url = "https://www.zepto.com/product/maggi-noodles-2min-pack-70g?ref=homepage&utm_source=google"
md5(long_url) = "a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8"
short_code = first 7 characters = "a3b4c5d"

URL: https://sh.rt/a3b4c5d → redirects to long_url
```

**Problems with hash approach:**

1. **Collisions**: Different URLs can produce the same first 7 characters of their hash.
   - Probability: with Base16 (hex), 7 chars = 16^7 = 268 million combinations
   - At 100 million URLs: birthday problem says collision probability ≈ 16% — unacceptable
   - Fix: if collision detected, append a counter and rehash (expensive)

2. **Same URL gets same short code**: If two users shorten the same URL, they get the same code. Sometimes desired (deduplication), sometimes not (user B can see user A's analytics).

3. **Pre-computation attack**: Adversary can precompute hashes of known long URLs to enumerate your short codes.

### Option B: Auto-Increment ID + Base62 Encoding ✅ (Recommended)

```
Database generates: id = 1000000001
Encode to Base62:   1000000001 → "43uLTJ"

Why Base62? Alphanumeric only: 0-9 (10) + a-z (26) + A-Z (26) = 62 characters
No special characters that break URLs: no /, +, =, &

Namespace:
  6 chars: 62^6 = 56.8 billion combinations
  7 chars: 62^7 = 3.5 trillion combinations

At 0.45 writes/second: 7 chars lasts 3.5 trillion / (0.45 × 86400 × 365) = 246,000 years
6 chars lasts 56.8 billion / same ≈ 4,000 years — more than sufficient
```

**Base62 Encoding:**

```kotlin
package com.yourcompany.urlshortener.service

object Base62Encoder {
    private const val ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val BASE = 62

    /**
     * Encode a positive Long to a Base62 string.
     * 
     * The database auto-increment ID ensures:
     * - Uniqueness (DB guarantees it)
     * - Monotonically increasing (no collisions possible)
     * - Predictable short code length (grows slowly)
     *
     * Starting ID at 1,000,000,000 (1 billion) ensures all codes
     * start at 6 characters → consistent appearance.
     * 
     * encode(1_000_000_000) = "15ftgG"  (6 chars)
     * encode(1_000_000_001) = "15ftgH"  (6 chars)
     */
    fun encode(num: Long): String {
        require(num > 0) { "Input must be positive" }
        
        val sb = StringBuilder()
        var n = num
        
        while (n > 0) {
            sb.append(ALPHABET[(n % BASE).toInt()])
            n /= BASE
        }
        
        return sb.reverse().toString()
    }

    /**
     * Decode a Base62 string back to a Long (for looking up in DB by ID).
     * Alternative: just query WHERE short_code = ? (also fine with an index).
     */
    fun decode(str: String): Long {
        var num = 0L
        for (char in str) {
            num = num * BASE + ALPHABET.indexOf(char)
        }
        return num
    }
}
```

> [!IMPORTANT]
> **Security consideration with sequential IDs**: Sequential IDs mean a user can enumerate short URLs (a3b4c5, a3b4c6, a3b4c7...) and discover others' links. If your URLs contain sensitive info (document links, invite tokens), use a non-sequential encoding:
> - Add a random salt to the ID before encoding
> - Use a hash with salt: `Base62.encode(sha256(id + secret_salt).slice(0, 7))`
> - Or start IDs at a large random number

---

## Database Schema

```sql
-- Core URL table
CREATE TABLE short_urls (
    id              BIGSERIAL PRIMARY KEY,
    short_code      VARCHAR(12) UNIQUE NOT NULL,
    original_url    TEXT NOT NULL,
    
    -- Custom alias support
    is_custom       BOOLEAN NOT NULL DEFAULT false,
    
    -- Owner (for analytics and management)
    user_id         BIGINT,              -- null for anonymous shortening
    api_key         VARCHAR(64),         -- which API key created this
    
    -- Metadata
    title           VARCHAR(500),        -- optional, extracted from page title
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at      TIMESTAMP WITH TIME ZONE,  -- null = never expires
    
    -- Simple analytics counters (incremented async)
    -- Heavy analytics go to ClickHouse/BigQuery
    click_count     BIGINT NOT NULL DEFAULT 0,
    last_accessed_at TIMESTAMP WITH TIME ZONE,
    
    -- Soft delete
    is_active       BOOLEAN NOT NULL DEFAULT true,
    deactivated_at  TIMESTAMP WITH TIME ZONE,
    deactivated_by  VARCHAR(100),   -- user or admin who deactivated
    
    CONSTRAINT check_expires CHECK (expires_at > created_at OR expires_at IS NULL)
);

-- Primary lookup index: the most critical index in the system
-- Every redirect hits this index
CREATE UNIQUE INDEX idx_short_urls_short_code 
    ON short_urls(short_code) 
    WHERE is_active = true;  -- Partial index: only active URLs

-- For listing user's URLs
CREATE INDEX idx_short_urls_user_id ON short_urls(user_id, created_at DESC) WHERE user_id IS NOT NULL;

-- For cleanup job: find expired URLs
CREATE INDEX idx_short_urls_expires_at ON short_urls(expires_at) WHERE expires_at IS NOT NULL AND is_active = true;

-- Click events table (append-only, partitioned by month)
CREATE TABLE click_events (
    id              BIGSERIAL,
    short_code      VARCHAR(12) NOT NULL,
    
    -- When
    clicked_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Who
    ip_address      INET,
    user_agent      TEXT,
    referer         TEXT,
    
    -- Parsed (async enrichment)
    country_code    CHAR(2),
    device_type     VARCHAR(20),    -- MOBILE, DESKTOP, TABLET, BOT
    browser         VARCHAR(50),
    os              VARCHAR(50),
    
    PRIMARY KEY (id, clicked_at)
) PARTITION BY RANGE (clicked_at);

-- Create monthly partitions
CREATE TABLE click_events_2024_01 PARTITION OF click_events
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

-- Index for analytics queries: "clicks for URL X in last 30 days"
CREATE INDEX idx_click_events_short_code ON click_events(short_code, clicked_at DESC);
```

> [!NOTE]
> **Table partitioning for click_events**: At 10 click events/URL/day × 100M URLs = 1 billion rows/day. At this scale, a monolithic table becomes impossible to query or maintain. Partitioning by month means:
> - Queries filtered by date automatically scan only relevant partitions
> - Old partitions can be archived or dropped without table locks
> - INSERT performance doesn't degrade as the table grows
> 
> In practice, at Bitly's scale, click events go directly to a dedicated analytics system (ClickHouse, BigQuery, Druid) — not PostgreSQL.

---

## Complete Spring Boot Implementation

### Entity

```kotlin
package com.yourcompany.urlshortener.domain

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "short_urls",
    indexes = [
        Index(name = "idx_short_code", columnList = "short_code", unique = true),
        Index(name = "idx_user_id", columnList = "user_id")
    ]
)
data class ShortUrl(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "short_code", nullable = false, unique = true, length = 12)
    var shortCode: String = "",

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    val originalUrl: String,

    @Column(name = "is_custom", nullable = false)
    val isCustom: Boolean = false,

    @Column(name = "user_id")
    val userId: Long? = null,

    @Column(name = "api_key", length = 64)
    val apiKey: String? = null,

    @Column(length = 500)
    val title: String? = null,

    @Column(name = "created_at", updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "expires_at")
    val expiresAt: Instant? = null,

    @Column(name = "click_count", nullable = false)
    var clickCount: Long = 0L,

    @Column(name = "last_accessed_at")
    var lastAccessedAt: Instant? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "deactivated_at")
    var deactivatedAt: Instant? = null
) {
    fun isExpired(): Boolean = expiresAt != null && Instant.now().isAfter(expiresAt)
    fun isServable(): Boolean = isActive && !isExpired()
}
```

### Repository

```kotlin
package com.yourcompany.urlshortener.repository

import com.yourcompany.urlshortener.domain.ShortUrl
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface ShortUrlRepository : JpaRepository<ShortUrl, Long> {

    fun findByShortCodeAndIsActiveTrue(shortCode: String): ShortUrl?

    fun findByOriginalUrlAndIsActiveTrue(originalUrl: String): ShortUrl?

    /**
     * Atomic click count increment — avoids read-modify-write race condition.
     * @Modifying ensures this is an UPDATE not a SELECT.
     * @Transactional must be on the calling service method.
     */
    @Modifying
    @Query("""
        UPDATE ShortUrl s SET 
            s.clickCount = s.clickCount + 1,
            s.lastAccessedAt = :now
        WHERE s.shortCode = :shortCode
    """)
    fun incrementClickCount(@Param("shortCode") shortCode: String, @Param("now") now: Instant)

    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<ShortUrl>

    /**
     * Find expired URLs for cleanup scheduler.
     * Runs nightly to deactivate expired links.
     */
    @Modifying
    @Query("""
        UPDATE ShortUrl s SET s.isActive = false, s.deactivatedAt = :now
        WHERE s.isActive = true 
        AND s.expiresAt IS NOT NULL 
        AND s.expiresAt < :now
    """)
    fun deactivateExpiredUrls(@Param("now") now: Instant): Int
}
```

### Service

```kotlin
package com.yourcompany.urlshortener.service

import com.yourcompany.urlshortener.domain.ShortUrl
import com.yourcompany.urlshortener.repository.ShortUrlRepository
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class UrlShortenerService(
    private val shortUrlRepository: ShortUrlRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val kafkaTemplate: KafkaTemplate<String, ClickEvent>,
    private val urlValidator: UrlValidator
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val CACHE_PREFIX = "url:"
        private val CACHE_TTL = Duration.ofHours(24)
        private val NEGATIVE_CACHE_TTL = Duration.ofMinutes(5) // Cache "not found" too
        private const val CACHE_MISS_VALUE = "NOT_FOUND"

        // Starting ID — all 6-char codes (62^5 = 916M, so starting at 10M gives headroom)
        private const val ID_OFFSET = 10_000_000L
    }

    /**
     * Create a new short URL.
     *
     * Deduplication: if the same long URL has already been shortened
     * (by the same user or anonymously), return the existing short code
     * to avoid cluttering the database. This is optional — some systems
     * always create new entries for analytics isolation.
     */
    @Transactional
    fun createShortUrl(
        longUrl: String,
        customAlias: String? = null,
        userId: Long? = null,
        apiKey: String? = null,
        expiresInDays: Int? = null
    ): CreateUrlResult {
        // Validate the URL
        if (!urlValidator.isValid(longUrl)) {
            return CreateUrlResult.InvalidUrl("URL format is invalid or points to a blocked domain")
        }

        // Handle custom alias
        if (customAlias != null) {
            return createCustomAlias(longUrl, customAlias, userId, apiKey, expiresInDays)
        }

        // Deduplication check (optional — skip if you want per-user analytics)
        val existing = shortUrlRepository.findByOriginalUrlAndIsActiveTrue(longUrl)
        if (existing != null && existing.userId == userId) {
            return CreateUrlResult.Success(
                shortCode = existing.shortCode,
                shortUrl = buildShortUrl(existing.shortCode),
                isExisting = true
            )
        }

        // Create new entry — get the ID first, then encode it
        val entity = ShortUrl(
            originalUrl = longUrl,
            userId = userId,
            apiKey = apiKey,
            expiresAt = expiresInDays?.let { Instant.now().plus(it.toLong(), ChronoUnit.DAYS) }
        )

        val saved = shortUrlRepository.save(entity)

        // Encode the auto-generated ID to Base62
        val shortCode = Base62Encoder.encode(saved.id + ID_OFFSET)
        saved.shortCode = shortCode
        shortUrlRepository.save(saved)

        // Cache immediately — this URL will likely be accessed soon
        cacheUrl(shortCode, longUrl)

        log.info("Created short URL: $shortCode → ${longUrl.take(50)}...")
        return CreateUrlResult.Success(
            shortCode = shortCode,
            shortUrl = buildShortUrl(shortCode),
            isExisting = false
        )
    }

    private fun createCustomAlias(
        longUrl: String,
        alias: String,
        userId: Long?,
        apiKey: String?,
        expiresInDays: Int?
    ): CreateUrlResult {
        // Validate alias format
        if (!alias.matches(Regex("[a-zA-Z0-9_-]{3,50}"))) {
            return CreateUrlResult.InvalidUrl("Custom alias must be 3-50 alphanumeric characters")
        }

        // Check if alias is taken
        val existing = shortUrlRepository.findByShortCodeAndIsActiveTrue(alias)
        if (existing != null) {
            return CreateUrlResult.AliasAlreadyTaken("The alias '$alias' is already in use")
        }

        val entity = ShortUrl(
            originalUrl = longUrl,
            shortCode = alias,
            isCustom = true,
            userId = userId,
            apiKey = apiKey,
            expiresAt = expiresInDays?.let { Instant.now().plus(it.toLong(), ChronoUnit.DAYS) }
        )

        shortUrlRepository.save(entity)
        cacheUrl(alias, longUrl)

        return CreateUrlResult.Success(
            shortCode = alias,
            shortUrl = buildShortUrl(alias),
            isExisting = false
        )
    }

    /**
     * REDIRECT: The critical path. Must be < 10ms.
     *
     * Flow:
     * 1. Check Redis cache (< 1ms)
     * 2. Cache hit: return URL, publish click event to Kafka async
     * 3. Cache miss: query PostgreSQL (5-10ms), populate cache, return URL
     *
     * We do NOT do analytics in the redirect path (no DB write for click count).
     * Analytics are published to Kafka and processed async by a consumer.
     * This keeps redirect latency at 1-10ms regardless of analytics load.
     */
    fun resolveUrl(shortCode: String, clickContext: ClickContext): ResolveResult {
        // Step 1: Check Redis cache
        val cached = redisTemplate.opsForValue().get("$CACHE_PREFIX$shortCode")

        val originalUrl = when {
            cached == CACHE_MISS_VALUE -> {
                // We've cached that this URL doesn't exist — save DB round-trip
                return ResolveResult.NotFound
            }
            cached != null -> cached  // Cache hit!
            else -> {
                // Cache miss — query database
                val entity = shortUrlRepository.findByShortCodeAndIsActiveTrue(shortCode)
                    ?: run {
                        // Cache the miss to prevent DB hammering for invalid codes
                        redisTemplate.opsForValue().set(
                            "$CACHE_PREFIX$shortCode",
                            CACHE_MISS_VALUE,
                            NEGATIVE_CACHE_TTL
                        )
                        return ResolveResult.NotFound
                    }

                if (entity.isExpired()) {
                    return ResolveResult.Expired(entity.expiresAt!!)
                }

                // Populate cache
                cacheUrl(shortCode, entity.originalUrl, entity.expiresAt)
                entity.originalUrl
            }
        }

        // Step 2: Publish click event to Kafka asynchronously
        // The Kafka consumer will update click_count in DB and enrich with geo/device data
        kafkaTemplate.send(
            "url-clicks",
            shortCode,
            ClickEvent(
                shortCode = shortCode,
                clickedAt = Instant.now(),
                ipAddress = clickContext.ipAddress,
                userAgent = clickContext.userAgent,
                referer = clickContext.referer
            )
        )

        return ResolveResult.Found(originalUrl)
    }

    private fun cacheUrl(shortCode: String, url: String, expiresAt: Instant? = null) {
        val ttl = if (expiresAt != null) {
            Duration.between(Instant.now(), expiresAt).coerceAtLeast(Duration.ofMinutes(1))
        } else {
            CACHE_TTL
        }
        redisTemplate.opsForValue().set("$CACHE_PREFIX$shortCode", url, ttl)
    }

    private fun buildShortUrl(shortCode: String) = "https://sh.rt/$shortCode"

    @Scheduled(cron = "0 0 2 * * *")  // 2 AM daily
    @Transactional
    fun deactivateExpiredUrls() {
        val count = shortUrlRepository.deactivateExpiredUrls(Instant.now())
        log.info("Deactivated $count expired URLs")
    }
}

// Result types
sealed class CreateUrlResult {
    data class Success(val shortCode: String, val shortUrl: String, val isExisting: Boolean) : CreateUrlResult()
    data class InvalidUrl(val reason: String) : CreateUrlResult()
    data class AliasAlreadyTaken(val reason: String) : CreateUrlResult()
}

sealed class ResolveResult {
    data class Found(val originalUrl: String) : ResolveResult()
    object NotFound : ResolveResult()
    data class Expired(val expiredAt: Instant) : ResolveResult()
}

data class ClickContext(
    val ipAddress: String?,
    val userAgent: String?,
    val referer: String?
)

data class ClickEvent(
    val shortCode: String,
    val clickedAt: Instant,
    val ipAddress: String?,
    val userAgent: String?,
    val referer: String?
)
```

### Controller

```kotlin
package com.yourcompany.urlshortener.controller

import com.yourcompany.urlshortener.service.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.time.Instant

@RestController
class UrlShortenerController(
    private val urlShortenerService: UrlShortenerService
) {
    /**
     * Redirect endpoint — the most performance-critical endpoint.
     *
     * 301 vs 302:
     * - 301 Permanent: Browser caches the redirect permanently.
     *   ✓ Faster for repeat visitors (browser doesn't hit your server again)
     *   ✗ You lose analytics for repeat clicks (browser redirects locally)
     *   ✗ If you change the destination, cached clients still go to old URL
     *   → Use for: permanent redirects where analytics don't matter
     *
     * - 302 Temporary: Browser does NOT cache.
     *   ✓ Every click hits your server → analytics are accurate
     *   ✓ You can change destination URL anytime
     *   ✗ Slightly slower for repeat visitors (extra server round-trip)
     *   → Use for: analytics-tracked links, marketing campaigns, A/B testing
     *
     * Decision: Use 302 by default. The performance difference is negligible
     * with Redis caching (< 2ms difference). Analytics value is high.
     */
    @GetMapping("/{shortCode}")
    fun redirect(
        @PathVariable shortCode: String,
        request: HttpServletRequest
    ): ResponseEntity<Void> {
        val clickContext = ClickContext(
            ipAddress = extractClientIp(request),
            userAgent = request.getHeader("User-Agent"),
            referer = request.getHeader("Referer")
        )

        return when (val result = urlShortenerService.resolveUrl(shortCode, clickContext)) {
            is ResolveResult.Found -> {
                ResponseEntity.status(HttpStatus.FOUND)  // 302
                    .header(HttpHeaders.LOCATION, result.originalUrl)
                    .header("Cache-Control", "no-cache, no-store")
                    .build()
            }
            is ResolveResult.NotFound -> {
                ResponseEntity.status(HttpStatus.NOT_FOUND).build()
            }
            is ResolveResult.Expired -> {
                ResponseEntity.status(HttpStatus.GONE)  // 410 Gone — more semantic than 404
                    .build()
            }
        }
    }

    /**
     * Create short URL endpoint.
     */
    @PostMapping("/api/shorten")
    fun createShortUrl(
        @RequestBody request: CreateShortUrlRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<CreateShortUrlResponse> {
        val apiKey = httpRequest.getHeader("X-API-Key")

        return when (val result = urlShortenerService.createShortUrl(
            longUrl = request.url,
            customAlias = request.customAlias,
            userId = extractUserId(httpRequest),
            apiKey = apiKey,
            expiresInDays = request.expiresInDays
        )) {
            is CreateUrlResult.Success -> ResponseEntity.ok(
                CreateShortUrlResponse(
                    shortUrl = result.shortUrl,
                    shortCode = result.shortCode,
                    originalUrl = request.url,
                    isExisting = result.isExisting,
                    createdAt = Instant.now()
                )
            )
            is CreateUrlResult.InvalidUrl ->
                ResponseEntity.badRequest().body(
                    CreateShortUrlResponse(error = result.reason)
                )
            is CreateUrlResult.AliasAlreadyTaken ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(
                    CreateShortUrlResponse(error = result.reason)
                )
        }
    }

    private fun extractClientIp(request: HttpServletRequest): String? {
        return request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: request.getHeader("X-Real-IP")
            ?: request.remoteAddr
    }

    private fun extractUserId(request: HttpServletRequest): Long? {
        return request.getHeader("X-User-Id")?.toLongOrNull()
    }
}

data class CreateShortUrlRequest(
    val url: String,
    val customAlias: String? = null,
    val expiresInDays: Int? = null
)

data class CreateShortUrlResponse(
    val shortUrl: String? = null,
    val shortCode: String? = null,
    val originalUrl: String? = null,
    val isExisting: Boolean = false,
    val createdAt: Instant? = null,
    val error: String? = null
)
```

---

## Caching Strategy — 80/20 Rule

> [!IMPORTANT]
> **The Pareto principle applies perfectly to URL shorteners**: 20% of URLs receive 80% of all clicks. A viral link shared by a major brand gets millions of clicks. The long tail of URLs are clicked once or twice. Cache the hot 20%, and your DB load drops by 80%.

```kotlin
/**
 * Cache warming: pre-populate Redis with URLs that are expected to get traffic.
 *
 * Scenario: Marketing team sends an email blast with a short URL to 1 million subscribers.
 * If all 1M click within 10 minutes and the URL isn't cached:
 * - 1M DB reads in 10 minutes = ~1,666 DB queries/second just for this one URL
 * - With Redis cache: 1 DB read, then 999,999 Redis reads → DB barely notices
 *
 * Pre-warming: Marketing creates the URL → we cache it immediately.
 * This happens automatically in createShortUrl().
 *
 * But for existing URLs about to go viral: provide a manual warm endpoint for ops.
 */
@PostMapping("/api/admin/warm-cache")
@PreAuthorize("hasRole('ADMIN')")
fun warmCache(@RequestParam shortCode: String): ResponseEntity<String> {
    val url = shortUrlRepository.findByShortCodeAndIsActiveTrue(shortCode)
        ?: return ResponseEntity.notFound().build()
    
    redisTemplate.opsForValue().set(
        "$CACHE_PREFIX$shortCode",
        url.originalUrl,
        Duration.ofHours(48)  // Longer TTL for known-hot URLs
    )
    
    return ResponseEntity.ok("Cache warmed for $shortCode")
}
```

### Cache Eviction Strategy

```yaml
# Redis memory policy — when Redis is full
# allkeys-lru: Evict least-recently-used keys from ALL keys
# volatile-lru: Evict LRU keys ONLY from keys with TTL set
# allkeys-lfu: Evict least-frequently-used (better for URL shortener — hot URLs stay cached)

# In redis.conf:
maxmemory 4gb
maxmemory-policy allkeys-lfu  # LFU: keeps frequently accessed URLs in cache
```

---

## Analytics — Async via Kafka

Never do analytics on the redirect critical path.

```kotlin
package com.yourcompany.urlshortener.analytics

import com.yourcompany.urlshortener.service.ClickEvent
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import ua_parser.Parser

@Service
class ClickEventConsumer(
    private val clickEventRepository: ClickEventRepository,
    private val shortUrlRepository: ShortUrlRepository,
    private val geoIpService: GeoIpService,
    private val uaParser: Parser
) {
    /**
     * Kafka consumer for click events.
     *
     * This processes click events asynchronously, AFTER the redirect has already
     * happened (user is already on the destination page).
     *
     * Processing per event:
     * 1. Parse User-Agent: detect browser, OS, device type
     * 2. GeoIP lookup: map IP → country, city
     * 3. Insert into click_events table (partitioned by month)
     * 4. Increment click_count on short_urls table
     *
     * At high throughput (10k clicks/sec):
     * - Use batch inserts (JDBC batch, 100 events per batch)
     * - Increment click_count via atomic SQL UPDATE (not read-modify-write)
     * - Consider writing to ClickHouse instead of PostgreSQL for analytics
     */
    @KafkaListener(topics = ["url-clicks"], groupId = "analytics-consumer")
    fun processClickEvent(event: ClickEvent) {
        try {
            val deviceInfo = parseUserAgent(event.userAgent)
            val geoInfo = event.ipAddress?.let { geoIpService.lookup(it) }

            clickEventRepository.save(
                ClickEventEntity(
                    shortCode = event.shortCode,
                    clickedAt = event.clickedAt,
                    ipAddress = event.ipAddress,
                    userAgent = event.userAgent,
                    referer = event.referer,
                    countryCode = geoInfo?.countryCode,
                    deviceType = deviceInfo.deviceType,
                    browser = deviceInfo.browser,
                    os = deviceInfo.os
                )
            )

            // Atomic increment — safe for concurrent consumers
            shortUrlRepository.incrementClickCount(event.shortCode, event.clickedAt)

        } catch (e: Exception) {
            // Log and don't rethrow — analytics failure must not affect redirect service
            log.error("Failed to process click event for ${event.shortCode}", e)
        }
    }

    private fun parseUserAgent(ua: String?): DeviceInfo {
        if (ua == null) return DeviceInfo("UNKNOWN", "UNKNOWN", "UNKNOWN")

        val client = uaParser.parse(ua)
        val deviceType = when {
            ua.contains("bot", ignoreCase = true) || ua.contains("crawler", ignoreCase = true) -> "BOT"
            ua.contains("Mobile") -> "MOBILE"
            ua.contains("Tablet") -> "TABLET"
            else -> "DESKTOP"
        }
        return DeviceInfo(deviceType, client.userAgent.family, client.os.family)
    }

    private val log = org.slf4j.LoggerFactory.getLogger(javaClass)
}

data class DeviceInfo(val deviceType: String, val browser: String, val os: String)
```

---

## Scaling to 10B+ Redirects/Day

### Current architecture limits:

```
Redis (single instance):
  Reads: ~100,000/second (more than enough for most startups)
  Memory: 10GB Redis holds ~50M URLs (assuming 200 bytes/entry average)

PostgreSQL (single primary + 1 read replica):
  Reads: Cache miss rate × total redirects
  If 99% cache hit rate: 10B × 0.01 = 100M DB reads/day = ~1,155/sec
  With read replica: 577/sec per instance → fully manageable

Writes (new URL creation):
  0.45/second — trivially handled by any DB
```

### When do you need to scale beyond this?

```
> 10M active URLs that don't fit in Redis memory → Redis Cluster
> 1M cache misses/second → Read replica fleet (5-10 replicas)
> DB write bottleneck → Sharding by short_code (rare for URL shorteners)
```

### Read Replica for Cache Misses

```kotlin
@Configuration
class DataSourceConfig {
    
    @Bean
    @Primary
    fun primaryDataSource(): DataSource {
        return HikariDataSource().apply {
            jdbcUrl = "jdbc:postgresql://${primaryHost}:5432/urldb"
            // Configuration
        }
    }

    @Bean
    @Qualifier("readReplica")
    fun replicaDataSource(): DataSource {
        return HikariDataSource().apply {
            jdbcUrl = "jdbc:postgresql://${replicaHost}:5432/urldb"
            isReadOnly = true
        }
    }
}

@Service
class UrlLookupService(
    @Qualifier("readReplica") private val replicaJdbc: JdbcTemplate
) {
    /**
     * Cache misses go to the read replica, not the primary.
     * Read replica lag is typically < 100ms for PostgreSQL streaming replication.
     * For URL lookups, 100ms eventual consistency is completely acceptable.
     * A URL won't change destination milliseconds after creation.
     */
    fun findByShortCode(shortCode: String): String? {
        return replicaJdbc.queryForObject(
            "SELECT original_url FROM short_urls WHERE short_code = ? AND is_active = true",
            String::class.java,
            shortCode
        )
    }
}
```

---

## URL Validation — Security Considerations

```kotlin
package com.yourcompany.urlshortener.service

import org.springframework.stereotype.Component
import java.net.URI

@Component
class UrlValidator(
    private val maliciousUrlService: MaliciousUrlService
) {
    /**
     * URL validation is a security-critical operation.
     *
     * You MUST prevent:
     * 1. Shortening localhost or private IP URLs (SSRF — Server-Side Request Forgery)
     *    Attacker shortens http://10.0.0.1/admin → your server fetches it
     *
     * 2. Shortening of phishing sites, malware distribution URLs
     *    Use: Google Safe Browsing API, VirusTotal URL check
     *
     * 3. URL shorteners shortening other URL shorteners (redirect chains)
     *    A chain of 10 shorteners = 10 DNS + TCP round trips for the user
     *
     * 4. Your own short URL domain being shortened (infinite loop)
     */
    fun isValid(url: String): Boolean {
        if (url.isBlank() || url.length > 2048) return false

        val uri = try { URI(url) } catch (e: Exception) { return false }

        // Must be HTTP or HTTPS
        if (uri.scheme !in listOf("http", "https")) return false

        val host = uri.host ?: return false

        // Block private/loopback IPs (SSRF prevention)
        if (isPrivateAddress(host)) return false

        // Block localhost variations
        if (host == "localhost" || host.endsWith(".local")) return false

        // Block shortening of other URL shorteners (configurable blocklist)
        val knownShorteners = setOf("bit.ly", "tinyurl.com", "t.co", "sh.rt")
        if (host in knownShorteners) return false

        // Check against malicious URL databases (async, with timeout)
        if (maliciousUrlService.isMalicious(url)) return false

        return true
    }

    private fun isPrivateAddress(host: String): Boolean {
        return try {
            val address = java.net.InetAddress.getByName(host)
            address.isSiteLocalAddress ||    // 10.x, 172.16-31.x, 192.168.x
            address.isLoopbackAddress ||     // 127.x
            address.isLinkLocalAddress ||    // 169.254.x
            address.isAnyLocalAddress ||
            address.isMCGlobal
        } catch (e: Exception) {
            false // If we can't resolve, allow (resolution is a separate step)
        }
    }
}
```

> [!CAUTION]
> **SSRF via URL shortener is a real attack vector.** If your shortener makes an HTTP request to preview/validate the destination URL (common for link preview features), and doesn't validate the URL first, an attacker can use your server to make requests to internal services. Always validate URLs before making any outbound requests.

---

## application.yml

```yaml
spring:
  application:
    name: url-shortener

  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/urldb
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10   # URL shortener is read-heavy — Redis handles most reads
      minimum-idle: 3

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6379
      timeout: 500ms   # Aggressive timeout — Redis should respond in < 1ms
      lettuce:
        pool:
          max-active: 20

  kafka:
    bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}
    producer:
      acks: 1           # Leader acknowledgment only — analytics can tolerate some loss
      retries: 3
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: analytics-consumer
      auto-offset-reset: earliest
      enable-auto-commit: false  # Manual commit for at-least-once processing

url-shortener:
  domain: "https://sh.rt"
  default-expiry-days: 0          # 0 = no expiry by default
  max-custom-alias-length: 50
  cache:
    default-ttl-hours: 24
    negative-ttl-minutes: 5

# Security
malicious-url:
  google-safe-browsing-api-key: ${SAFE_BROWSING_API_KEY}
  check-timeout-ms: 500
```

---

## Production Architecture Diagram

```
                    Internet
                        │
                    Cloudflare
                    (DDoS protection, edge caching for 301 redirects)
                        │
                    Load Balancer
                   /     │     \
           ┌──────┘      │      └──────┐
           │             │             │
      App Instance   App Instance  App Instance
      (Spring Boot)  (Spring Boot)  (Spring Boot)
           │             │             │
           └──────┬──────┘─────────────┘
                  │
          ┌───────┼───────┐
          │               │
       Redis          PostgreSQL
       Cluster         Primary
       (hot URL       (writes,
        cache)         source of truth)
                         │
                     Read Replica
                     (cache misses)
                         
           ┌─────────────┘
           │
        Kafka Cluster
           │
    Analytics Consumer
           │
        ClickHouse
    (analytics queries)
           │
        Grafana
      (dashboards)
```

---

## Key Metrics to Monitor

```kotlin
@Component
class UrlShortenerMetrics(
    private val meterRegistry: MeterRegistry
) {
    // Track redirect latency (should be < 10ms p99)
    val redirectTimer: Timer = Timer.builder("url.redirect.latency")
        .description("Time to resolve short URL and initiate redirect")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(meterRegistry)

    // Track cache hit rate (should be > 95%)
    val cacheHits = meterRegistry.counter("url.cache.hits")
    val cacheMisses = meterRegistry.counter("url.cache.misses")

    // Track error rates
    val notFoundCount = meterRegistry.counter("url.not_found")
    val expiredCount = meterRegistry.counter("url.expired")

    fun cacheHitRate(): Double {
        val hits = cacheHits.count()
        val total = hits + cacheMisses.count()
        return if (total > 0) hits / total else 0.0
    }
}
```

**Alert thresholds:**
- Redirect p99 latency > 50ms → Check Redis latency, DB query time
- Cache hit rate < 95% → Cache eviction happening, increase Redis memory
- Error rate > 0.1% → Check for systematic failures (DB down, Redis OOM)
- Click event Kafka lag > 100,000 → Analytics consumer is behind, scale consumers
