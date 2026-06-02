# SD-3 — System Design: Rate Limiter

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> **Context**: Rate limiting is the difference between a resilient production system and one that goes down whenever someone runs an aggressive script against your API. This is not just about blocking bad actors — it's about ensuring fair resource distribution, protecting your backend from overload, and enforcing business rules (billing tiers, API quotas).

---

## Why Rate Limiting — The Production Reality

### Scenario 1: Credential Stuffing Attack
An attacker has 10 million leaked username/password combinations. They hit your `/login` endpoint at 10,000 requests/second. Without rate limiting: your DB is obliterated, 100% CPU, your legitimate users can't log in.

### Scenario 2: Poorly Written Client
A third-party partner integration has a bug — it retries on every error with no backoff. Their single server hammers your API at 500 req/sec. Your other partners get degraded service.

### Scenario 3: Free Tier Abuse
Your API has a free tier (100 requests/day). A developer figures out they can create infinite accounts and get unlimited free usage. Without enforcement, your compute costs scale with abuse.

### Scenario 4: Thundering Herd
Your app sends a push notification to 5 million users. 200,000 of them open the app simultaneously. All 200,000 clients hit the `/feed` endpoint in the same 30-second window. Your server dies.

**Rate limiting protects against all four** — but the implementation differs for each case.

---

## Types of Rate Limiting

| Type | Scope | Use Case |
|------|-------|----------|
| Per-IP | IP address | Block scrapers, brute-force attackers |
| Per-User | Authenticated user ID | Fair usage enforcement, abuse prevention |
| Per-API-Key | Partner API key | Billing tier enforcement |
| Per-Endpoint | Route + identifier | Expensive endpoints (search, export) |
| Global | Entire service | Protect downstream from any overload |

### Layered Rate Limiting (Production Pattern)

At Stripe, rate limiting is applied at multiple layers:
1. **NGINX layer**: per-IP, 1000 req/min global limit (protects infrastructure)
2. **API Gateway layer**: per-API-key based on plan (business rule enforcement)
3. **Application layer**: per-user for specific expensive operations (business logic)

Each layer has a different purpose. Don't try to solve all of them in one place.

---

## Rate Limiting Algorithms — Deep Dive

### 1. Fixed Window Counter

```
Window: 0–59 seconds (1 minute)
Limit: 100 requests

Counter starts at 0
Request 1 at T=0: counter = 1 (allow)
Request 50 at T=30: counter = 50 (allow)
Request 100 at T=55: counter = 100 (allow)
Request 101 at T=58: counter = 101 (DENY)

New window at T=60: counter resets to 0
```

**The boundary burst problem:**
```
Attacker sends 100 requests at T=59 (just before window resets)
New window at T=60 — attacker sends 100 more requests at T=61

Result: 200 requests in 2 seconds — 2× the limit is bypassed
```

**Redis implementation:**
```
INCR user:ratelimit:123:2024-01-01-15-30    → 1
EXPIRE user:ratelimit:123:2024-01-01-15-30 60
...
INCR user:ratelimit:123:2024-01-01-15-30    → 101 → DENY
```

**When to use**: Simple use cases where boundary bursting is acceptable (e.g., internal service-to-service calls where burst is expected).

---

### 2. Sliding Window Log

```
Limit: 100 requests per minute

For each request:
1. Remove all entries from the log older than NOW - 1 minute
2. Count remaining entries
3. If count >= 100: DENY
4. Else: ADD current timestamp to log, ALLOW
```

**Redis implementation using ZADD (sorted set):**
```
ZADD user:ratelimit:123 1704067260000 1704067260000  (add with score = timestamp)
ZREMRANGEBYSCORE user:ratelimit:123 0 (now - 60000)   (remove entries older than 1 min)
ZCARD user:ratelimit:123                               (count remaining)
```

**Pros**: Perfectly accurate — no boundary burst vulnerability
**Cons**: Stores every request timestamp → memory-heavy at scale

For a user making 100 req/min: stores 100 timestamps per user.
For 1 million users: potentially 100 million Redis entries.

**When to use**: High-accuracy requirements where memory cost is acceptable (authentication endpoints, financial operations).

---

### 3. Token Bucket

```
Bucket capacity: 100 tokens
Refill rate: 10 tokens/second

State: tokens = 100, last_refill = now

For each request:
1. Calculate elapsed = now - last_refill
2. Add (elapsed × refill_rate) tokens (cap at bucket capacity)
3. If tokens >= 1: consume 1 token, ALLOW
4. Else: DENY
```

**Why token bucket handles bursts well:**
- Normal usage: 10 req/sec (10 tokens consumed, 10 refilled) → indefinitely sustainable
- Burst: User sends 100 requests instantly → all 100 tokens consumed, all 100 allowed
- After burst: must wait for refill before sending more

**This is how Stripe's API works.** Their clients can burst, but sustained overuse is throttled.

**Redis Lua script for atomic token bucket:**
```lua
-- token_bucket.lua
local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])  -- tokens per second
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens = tonumber(bucket[1]) or capacity
local last_refill = tonumber(bucket[2]) or now

-- Calculate new tokens from elapsed time
local elapsed = math.max(0, now - last_refill)
local new_tokens = math.min(capacity, tokens + (elapsed * refill_rate))

-- Check if we can allow this request
if new_tokens >= requested then
    new_tokens = new_tokens - requested
    redis.call('HMSET', key, 'tokens', new_tokens, 'last_refill', now)
    redis.call('EXPIRE', key, 3600)  -- TTL: clean up inactive users
    return {1, math.floor(new_tokens)}  -- {allowed, remaining_tokens}
else
    redis.call('HMSET', key, 'tokens', new_tokens, 'last_refill', now)
    redis.call('EXPIRE', key, 3600)
    return {0, 0}  -- {denied, 0}
end
```

---

### 4. Leaky Bucket

```
Bucket: a queue with fixed size
Leak rate: constant (e.g., 10 req/sec processed)

Requests enter the bucket (queue)
If bucket is full: request is DROPPED (or queued and served later)
Requests leave the bucket at a constant rate

Effect: Smooths out bursty traffic into a constant rate
```

**When to use**: Video streaming (constant bitrate), financial ledgers (prevent DB write spikes), external API calls where you must respect the provider's rate limit.

**Key difference from Token Bucket**: Token Bucket allows bursts to pass through at full speed. Leaky Bucket smooths all traffic to a constant rate regardless.

---

### 5. Sliding Window Counter (Hybrid — Production Favorite)

Combines accuracy of Sliding Window Log with efficiency of Fixed Window Counter.

```
Concept:
- Maintain two fixed windows: current window and previous window
- Weight the previous window based on how far into the current window we are

Formula:
rate = (prev_window_count × (1 - position_in_current_window)) + current_window_count

Example:
Window size: 60 seconds, Limit: 100 req/min
Previous window (T=-60 to T=0): 80 requests
Current window (T=0 to T=60): 40 requests, we're at T=45 (75% through)

Effective rate = (80 × (1 - 0.75)) + 40 = 20 + 40 = 60 (under limit, ALLOW)

At T=55 (91.7% through):
Effective rate = (80 × 0.083) + 40 = 6.6 + 40 = 46.6 (under limit, ALLOW)
```

**This is the algorithm used by Cloudflare, NGINX, and most production rate limiters.**

---

## NGINX Rate Limiting

The cheapest rate limiter: handle it before requests even reach your application.

```nginx
http {
    # Define rate limit zones
    # $binary_remote_addr: client IP (binary uses less memory than string)
    # zone=api:10m: "api" zone, 10MB of shared memory (~160k IP addresses)
    # rate=100r/m: 100 requests per minute
    limit_req_zone $binary_remote_addr zone=api_per_ip:10m rate=100r/m;
    
    # Per-user rate limiting (requires extracting user from JWT or session)
    # Use $http_x_user_id header set by your auth middleware
    limit_req_zone $http_x_user_id zone=api_per_user:10m rate=200r/m;
    
    # Global rate limit for expensive search endpoint
    limit_req_zone $binary_remote_addr zone=search:10m rate=10r/m;
    
    # Response when rate limited
    limit_req_status 429;
    
    server {
        location /api/ {
            # burst=20: Allow up to 20 requests queued beyond the rate limit
            # nodelay: Process the burst immediately (don't delay, just allow up to burst+limit)
            limit_req zone=api_per_ip burst=20 nodelay;
            
            # Add rate limit headers so clients know what's happening
            add_header X-RateLimit-Limit 100;
            add_header Retry-After 60;
            
            proxy_pass http://springboot;
        }
        
        location /api/search {
            # Stricter limit for expensive search
            limit_req zone=search burst=5 nodelay;
            proxy_pass http://springboot;
        }
        
        location /api/auth/login {
            # Very strict for login — prevent brute force
            limit_req zone=api_per_ip burst=5 nodelay;
            limit_req_status 429;
            proxy_pass http://springboot;
        }
    }
}
```

> [!WARNING]
> **NGINX rate limiting is per-worker-process.** If NGINX has 4 worker processes and your limit is 100 req/min, each worker independently allows 100 req/min → effective limit is 400 req/min. The shared memory zone (`10m`) solves this for single-instance NGINX, but in multi-node setups, NGINX instances don't share state. Use application-level Redis rate limiting for multi-instance accuracy.

---

## Spring Boot Redis Rate Limiter — Production Implementation

```kotlin
package com.yourcompany.ratelimit

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.proxy.ProxyManager
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.lettuce.core.RedisClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.time.Duration
import java.util.function.Supplier

/**
 * Production rate limiter using Bucket4j + Redis.
 *
 * Bucket4j implements the Token Bucket algorithm with Redis-backed distributed state.
 * All instances of your Spring Boot app share the same rate limit counters via Redis.
 *
 * Why Bucket4j over rolling your own Redis INCR logic:
 * - Battle-tested distributed implementation
 * - Lua scripts for atomic Redis operations (no race conditions)
 * - Multiple bandwidth layers (e.g., 100/min AND 1000/hour)
 * - Works with Redis, Hazelcast, Infinispan, PostgreSQL
 */
@Configuration
class RateLimiterConfig {

    @Bean
    fun redisProxyManager(redisClient: RedisClient): ProxyManager<String> {
        val connection = redisClient.connect()
        return LettuceBasedProxyManager.builderFor(connection)
            .withExpirationStrategy(
                io.github.bucket4j.distributed.ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                    Duration.ofMinutes(5)
                )
            )
            .build()
    }
}

@Component
class RateLimitFilter(
    private val proxyManager: ProxyManager<String>,
    private val rateLimitConfig: RateLimitProperties
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val key = resolveRateLimitKey(request)
        val plan = resolvePlan(request)
        val bucket = proxyManager.builder().build(key, planConfigSupplier(plan))

        val probe = bucket.tryConsumeAndReturnRemaining(1)

        if (probe.isConsumed) {
            // Add rate limit headers — clients NEED this info to implement backoff
            response.addHeader("X-RateLimit-Limit", plan.requestsPerMinute.toString())
            response.addHeader("X-RateLimit-Remaining", probe.remainingTokens.toString())
            response.addHeader("X-RateLimit-Reset",
                (System.currentTimeMillis() / 1000 + probe.nanosToWaitForRefill / 1_000_000_000).toString()
            )
            filterChain.doFilter(request, response)
        } else {
            // 429 Too Many Requests
            val retryAfterSeconds = probe.nanosToWaitForRefill / 1_000_000_000
            response.status = 429
            response.addHeader("Retry-After", retryAfterSeconds.toString())
            response.addHeader("X-RateLimit-Limit", plan.requestsPerMinute.toString())
            response.addHeader("X-RateLimit-Remaining", "0")
            response.contentType = "application/json"
            response.writer.write("""
                {
                    "error": "rate_limit_exceeded",
                    "message": "Too many requests. Retry after ${retryAfterSeconds} seconds.",
                    "retry_after": $retryAfterSeconds
                }
            """.trimIndent())
        }
    }

    /**
     * Key resolution strategy — determines what the rate limit is scoped to.
     *
     * Priority:
     * 1. API key in header → rate limit per API key (partner integrations)
     * 2. Authenticated user JWT → rate limit per user
     * 3. IP address → rate limit per IP (unauthenticated requests)
     */
    private fun resolveRateLimitKey(request: HttpServletRequest): String {
        val apiKey = request.getHeader("X-API-Key")
        if (apiKey != null) return "apikey:$apiKey:${normalizeEndpoint(request.requestURI)}"

        val userId = request.getHeader("X-User-Id")  // Set by JWT filter upstream
        if (userId != null) return "user:$userId:${normalizeEndpoint(request.requestURI)}"

        val ip = getClientIp(request)
        return "ip:$ip:${normalizeEndpoint(request.requestURI)}"
    }

    /**
     * Plan resolution — determines rate limit tier.
     *
     * In production, you'd look up the API key in a cache (Redis/DB) to get their plan.
     * Here we show a simplified version.
     */
    private fun resolvePlan(request: HttpServletRequest): RateLimitPlan {
        val apiKey = request.getHeader("X-API-Key") ?: return RateLimitPlan.FREE

        // In production: lookup cache first, then DB
        return when {
            apiKey.startsWith("pro_") -> RateLimitPlan.PRO
            apiKey.startsWith("enterprise_") -> RateLimitPlan.ENTERPRISE
            else -> RateLimitPlan.FREE
        }
    }

    private fun planConfigSupplier(plan: RateLimitPlan): Supplier<BucketConfiguration> = Supplier {
        BucketConfiguration.builder()
            .addLimit(
                Bandwidth.builder()
                    .capacity(plan.requestsPerMinute.toLong())
                    .refillGreedy(plan.requestsPerMinute.toLong(), Duration.ofMinutes(1))
                    .build()
            )
            // Optional: hourly limit as a second layer
            .addLimit(
                Bandwidth.builder()
                    .capacity(plan.requestsPerHour.toLong())
                    .refillGreedy(plan.requestsPerHour.toLong(), Duration.ofHours(1))
                    .build()
            )
            .build()
    }

    private fun normalizeEndpoint(uri: String): String {
        // Normalize /api/orders/12345 → /api/orders/{id} to group dynamic IDs
        return uri.replace(Regex("/\\d+"), "/{id}")
                  .replace(Regex("[?#].*"), "")  // Remove query params
    }

    private fun getClientIp(request: HttpServletRequest): String {
        // Handle reverse proxy (NGINX, Cloudflare)
        val forwarded = request.getHeader("X-Forwarded-For")
        return forwarded?.split(",")?.firstOrNull()?.trim()
            ?: request.getHeader("X-Real-IP")
            ?: request.remoteAddr
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        // Don't rate limit health checks and metrics endpoints
        val path = request.requestURI
        return path.startsWith("/actuator") || path == "/health"
    }
}

enum class RateLimitPlan(
    val requestsPerMinute: Int,
    val requestsPerHour: Int
) {
    FREE(60, 1_000),
    PRO(1_000, 20_000),
    ENTERPRISE(10_000, 200_000)
}
```

---

## Distributed Rate Limiting — Why In-Memory is Wrong

> [!CAUTION]
> **If you implement rate limiting with an in-memory counter (a simple `ConcurrentHashMap`), it only works on a single instance. With 3 Spring Boot instances and an in-memory limit of 100/min, a user can actually make 300 requests/min (100 per instance). This is a critical security flaw for authentication endpoints.**

```kotlin
// WRONG: In-memory rate limiter — breaks with multiple instances
private val counters = ConcurrentHashMap<String, AtomicInteger>()

fun checkRateLimit(key: String, limit: Int): Boolean {
    val counter = counters.getOrPut(key) { AtomicInteger(0) }
    return counter.incrementAndGet() <= limit
    // This count is local to THIS JVM instance only
}

// RIGHT: Redis-backed rate limiter — shared across all instances
fun checkRateLimit(key: String, limit: Int): Boolean {
    val count = redisTemplate.opsForValue().increment("ratelimit:$key") ?: 0L
    if (count == 1L) {
        redisTemplate.expire("ratelimit:$key", Duration.ofMinutes(1))
    }
    return count <= limit
}
```

---

## Raw Redis Rate Limiter — Without Bucket4j

For teams that want full control without a library:

```kotlin
package com.yourcompany.ratelimit

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RedisRateLimiter(
    private val redisTemplate: RedisTemplate<String, String>
) {
    /**
     * Sliding window counter using two fixed windows.
     *
     * This is the most accurate Redis-efficient algorithm.
     * Used by Cloudflare in their rate limiter.
     */
    private val slidingWindowScript = DefaultRedisScript<List<Long>>().apply {
        setScriptText("""
            local current_key = KEYS[1]
            local previous_key = KEYS[2]
            local limit = tonumber(ARGV[1])
            local window_seconds = tonumber(ARGV[2])
            local position_fraction = tonumber(ARGV[3])  -- position in current window (0.0 to 1.0)
            
            local current_count = tonumber(redis.call('GET', current_key) or '0')
            local prev_count = tonumber(redis.call('GET', previous_key) or '0')
            
            -- Weight previous window
            local weighted_prev = prev_count * (1 - position_fraction)
            local effective_count = weighted_prev + current_count
            
            if effective_count >= limit then
                return {0, math.floor(effective_count)}  -- {denied, current_count}
            end
            
            -- Increment current window
            local new_count = redis.call('INCR', current_key)
            if new_count == 1 then
                -- Set expiry only on first request (window_seconds × 2 for overlap)
                redis.call('EXPIRE', current_key, window_seconds * 2)
            end
            
            return {1, math.floor(limit - effective_count - 1)}  -- {allowed, remaining}
        """.trimIndent())
        @Suppress("UNCHECKED_CAST")
        resultType = List::class.java as Class<List<Long>>
    }

    fun isAllowed(
        identifier: String,
        limit: Int,
        windowSeconds: Long = 60L
    ): RateLimitDecision {
        val now = System.currentTimeMillis() / 1000
        val windowStart = now - (now % windowSeconds)  // Current window boundary
        val prevWindowStart = windowStart - windowSeconds

        val currentKey = "ratelimit:${identifier}:$windowStart"
        val previousKey = "ratelimit:${identifier}:$prevWindowStart"
        val positionInWindow = (now % windowSeconds).toDouble() / windowSeconds

        @Suppress("UNCHECKED_CAST")
        val result = redisTemplate.execute(
            slidingWindowScript,
            listOf(currentKey, previousKey),
            limit.toString(),
            windowSeconds.toString(),
            positionInWindow.toString()
        ) as? List<Long> ?: listOf(1L, limit.toLong())

        val allowed = result[0] == 1L
        val remaining = result[1]

        return RateLimitDecision(
            allowed = allowed,
            remaining = remaining.coerceAtLeast(0),
            resetAfterSeconds = windowSeconds - (now % windowSeconds),
            limit = limit.toLong()
        )
    }
}

data class RateLimitDecision(
    val allowed: Boolean,
    val remaining: Long,
    val resetAfterSeconds: Long,
    val limit: Long
)
```

---

## Rate Limit Response Format

The 429 response must tell clients exactly what to do. A poorly formatted 429 leads to clients hammering with blind retries, making the problem worse.

```kotlin
@ControllerAdvice
class RateLimitExceptionHandler {

    @ExceptionHandler(RateLimitExceededException::class)
    fun handleRateLimit(
        ex: RateLimitExceededException,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<RateLimitErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)  // 429
            .header("Retry-After", ex.retryAfterSeconds.toString())
            .header("X-RateLimit-Limit", ex.limit.toString())
            .header("X-RateLimit-Remaining", "0")
            .header("X-RateLimit-Reset", (System.currentTimeMillis() / 1000 + ex.retryAfterSeconds).toString())
            .body(RateLimitErrorResponse(
                error = "rate_limit_exceeded",
                message = "You've exceeded the rate limit of ${ex.limit} requests per minute. " +
                         "Please retry after ${ex.retryAfterSeconds} seconds.",
                retryAfterSeconds = ex.retryAfterSeconds,
                limit = ex.limit,
                documentationUrl = "https://docs.yourcompany.com/api/rate-limits"
            ))
    }
}

data class RateLimitErrorResponse(
    val error: String,
    val message: String,
    val retryAfterSeconds: Long,
    val limit: Long,
    val documentationUrl: String
)
```

---

## Graceful Degradation vs Hard Rejection

Not all rate limiting should result in rejection. Sometimes, serving a degraded response is better than refusing entirely.

```kotlin
@Service
class ProductSearchService(
    private val rateLimiter: RedisRateLimiter,
    private val searchEngine: ElasticsearchService,
    private val simpleSearch: DatabaseSearchService
) {
    /**
     * Graceful degradation strategy:
     * 
     * Under normal load → Full Elasticsearch search (rich, relevant results)
     * Under high load (>70% of limit) → Simple SQL LIKE search (fast, less relevant)
     * Over limit → Serve cached results from 5 minutes ago
     * Way over limit → Return 429
     *
     * This is the strategy Netflix uses for recommendation degradation:
     * when their ML system is slow, they fall back to generic trending content
     * rather than showing an error.
     */
    fun search(query: String, userId: Long): SearchResult {
        val decision = rateLimiter.isAllowed(
            identifier = "search:$userId",
            limit = 30,
            windowSeconds = 60
        )

        return when {
            decision.allowed && decision.remaining > 5 -> {
                // Plenty of budget remaining — use the good search
                searchEngine.search(query)
            }
            decision.allowed && decision.remaining <= 5 -> {
                // Getting close to limit — use cheaper search
                simpleSearch.search(query)
            }
            !decision.allowed -> {
                // Over limit — return cached results if available
                val cached = getCachedSearchResults(query)
                if (cached != null) {
                    cached.copy(degraded = true, message = "Showing cached results due to high load")
                } else {
                    throw RateLimitExceededException(
                        retryAfterSeconds = decision.resetAfterSeconds,
                        limit = decision.limit
                    )
                }
            }
            else -> throw RateLimitExceededException(decision.resetAfterSeconds, decision.limit)
        }
    }

    private fun getCachedSearchResults(query: String): SearchResult? = null // Redis cache lookup
}
```

---

## application.yml Configuration

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6379
      timeout: 500ms   # Rate limiter should be FAST — fail open if Redis is slow
      lettuce:
        pool:
          max-active: 16

# Rate limit configuration (externalized for easy tuning without deployment)
rate-limit:
  enabled: true
  default:
    requests-per-minute: 100
    requests-per-hour: 1000
  
  endpoints:
    "/api/auth/login":
      requests-per-minute: 10     # Strict — brute force prevention
      requests-per-hour: 50
    
    "/api/search":
      requests-per-minute: 30     # Elasticsearch is expensive
    
    "/api/export":
      requests-per-minute: 2      # Very expensive operation
    
    "/api/orders":
      requests-per-minute: 200    # Core operation — be generous
```

---

## Rate Limiter at the API Gateway Level

For multi-service architectures, implement rate limiting at the API Gateway (Kong, AWS API Gateway, NGINX) rather than in each service:

```yaml
# Kong rate limiting plugin
plugins:
  - name: rate-limiting
    config:
      minute: 100
      policy: redis          # Use Redis for distributed counting
      redis_host: redis-host
      redis_port: 6379
      fault_tolerant: true   # CRITICAL: if Redis is down, allow requests (fail open)
                             # Fail closed = 100% outage when Redis has issues
      hide_client_headers: false
      limit_by: consumer    # Rate limit per authenticated consumer
```

> [!IMPORTANT]
> **Fail Open vs Fail Closed**: If your Redis rate limiter becomes unavailable:
> - **Fail open** (allow all traffic): Risk of temporary abuse, but your service stays up
> - **Fail closed** (deny all traffic): Your service is 100% down when Redis has issues
>
> For most business-critical APIs, **fail open is correct**. Rate limiting is a protective measure, not a core business function. Your service failing completely because the rate limiter is unavailable is worse than 1-2 minutes of unthrottled traffic.

---

## Advanced: Per-User Rate Limit from Database

In a SaaS product, each customer has a different rate limit based on their plan. You can't hardcode this.

```kotlin
@Service
class DynamicRateLimitService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val planRepository: SubscriptionPlanRepository
) {
    /**
     * Rate limits are stored per API key in Redis (with 1-hour TTL).
     * Background job syncs from DB to Redis when plans change.
     *
     * Hot path: Redis lookup (< 1ms) → check and decrement
     * Cold path (cache miss): DB lookup → store in Redis → check and decrement
     */
    fun getRateLimit(apiKey: String): RateLimitPlanConfig {
        val cacheKey = "plan:ratelimit:$apiKey"
        val cached = redisTemplate.opsForHash<String, String>().entries(cacheKey)

        if (cached.isNotEmpty()) {
            return RateLimitPlanConfig(
                requestsPerMinute = cached["rpm"]?.toIntOrNull() ?: 100,
                requestsPerHour = cached["rph"]?.toIntOrNull() ?: 1000
            )
        }

        // Cache miss: load from DB
        val plan = planRepository.findByApiKey(apiKey)
            ?: return RateLimitPlanConfig(requestsPerMinute = 60, requestsPerHour = 500) // Default free tier

        val config = RateLimitPlanConfig(
            requestsPerMinute = plan.rpmLimit,
            requestsPerHour = plan.rphLimit
        )

        // Cache for 1 hour
        redisTemplate.opsForHash<String, String>().putAll(cacheKey, mapOf(
            "rpm" to config.requestsPerMinute.toString(),
            "rph" to config.requestsPerHour.toString()
        ))
        redisTemplate.expire(cacheKey, Duration.ofHours(1))

        return config
    }
}

data class RateLimitPlanConfig(
    val requestsPerMinute: Int,
    val requestsPerHour: Int
)
```

---

## Production Checklist

> [!IMPORTANT]
> Before going to production with your rate limiter:
> - [ ] Rate limit headers are set on ALL responses (not just 429s) — clients need this to implement adaptive backoff
> - [ ] Rate limiter fails open when Redis is unavailable
> - [ ] Whitelisted IPs/keys for internal services and monitoring
> - [ ] Rate limit metrics exported to Prometheus (track 429 rate per endpoint)
> - [ ] Different limits for different endpoint categories (auth vs read vs write vs export)
> - [ ] Documentation published for API consumers about rate limits
> - [ ] Retry-After header is accurate — don't send wrong values
> - [ ] Load tested: verify limits hold under actual concurrent load (not just sequential)

> [!WARNING]
> **Testing rate limiters is non-trivial.** A unit test with a mock Redis doesn't prove the Lua script is correct. Run integration tests against a real Redis instance with multiple concurrent threads to verify the atomicity guarantees work.
