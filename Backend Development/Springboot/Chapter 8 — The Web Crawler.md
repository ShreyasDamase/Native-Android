# Backend Engineering with Spring Boot & Kotlin

## The HireStory Builder's Guide

---

# Chapter 8 — The Web Crawler

### _Filling HireStory with content automatically — Jsoup, Reddit API, and scheduled crawling_

---

## 8.1 The Problem This Chapter Solves

HireStory needs content. Lots of it. Waiting for users to submit interviews one by one is too slow at launch. You need a way to automatically discover and import interview experiences from the internet.

That is the crawler. It does four things:

1. **Discovers** new interview experience posts from Reddit, GeeksForGeeks, and other sources on a schedule
2. **Deduplicates** — never processes the same URL twice
3. **Stores** the raw content as a `CrawlJob` in the database
4. **Queues** the raw content to RabbitMQ for AI extraction (Chapter 9)

The crawler does not decide if content is good. It does not extract structure. It just finds raw text and hands it off. That separation matters — the crawler is fast and dumb. The AI is slow and smart. They work independently.

---

## 8.2 What You Will Build

```
Spring Scheduler (every 6 hours)
        ↓
CrawlerOrchestrator
  ├── RedditCrawler      → fetches posts from r/cscareerquestions, r/india
  ├── GfgCrawler         → scrapes GeeksForGeeks interview experiences
  └── DevToCrawler       → fetches posts from dev.to API
        ↓
For each discovered URL:
  ├── Check Redis dedup key — skip if seen
  ├── Check database — skip if exists
  └── Create CrawlJob (status=PENDING) + publish to RabbitMQ
        ↓
Admin endpoint: POST /api/admin/crawler/trigger — manual trigger for testing
```

---

## 8.3 Dependencies

```kotlin
// build.gradle.kts

dependencies {
    // Jsoup — HTML parsing and scraping
    implementation("org.jsoup:jsoup:1.18.3")

    // OkHttp — HTTP client for API calls (Reddit, dev.to)
    // Spring's RestTemplate works too — OkHttp is cleaner for this use case
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Already have: Spring Scheduling (part of spring-boot-starter)
    // Already have: RabbitMQ (Chapter 7)
    // Already have: Redis (Chapter 6)
}
```

---

## 8.4 Enable Scheduling

Add one annotation to your application class:

```kotlin
// src/main/kotlin/com/example/hirestory/HireStoryApplication.kt

@SpringBootApplication
@EnableConfigurationProperties(HireStoryProperties::class)
@EnableScheduling    // ← Activates @Scheduled annotations
class HireStoryApplication

fun main(args: Array<String>) {
    runApplication<HireStoryApplication>()
}
```

Without `@EnableScheduling`, every `@Scheduled` annotation in your app is silently ignored. This is the most common scheduling mistake.

---

## 8.5 Crawler Configuration

Add crawler settings to `application.yml` and your properties class:

```yaml
# application.yml

hirestory:
  crawler:
    enabled: true                    # Master switch — set false to disable all crawling
    reddit:
      user-agent: "HireStory/1.0 (interview aggregator; contact@hirestory.com)"
      subreddits:
        - cscareerquestions
        - india
        - developersIndia
        - jobs
      search-terms:
        - "interview experience"
        - "got offer"
        - "interview questions"
        - "placement experience"
    gfg:
      base-url: "https://www.geeksforgeeks.org"
      experience-path: "/experiences/"
    devto:
      api-url: "https://dev.to/api"
      tags:
        - interview
        - career
        - placement
    rate-limit-ms: 1000              # Wait 1 second between requests to same domain
    max-items-per-run: 50            # Maximum new items to queue per crawler run
```

```kotlin
// Update HireStoryProperties.kt

@ConfigurationProperties(prefix = "hirestory")
data class HireStoryProperties(
    val clerk: ClerkProperties,
    val freeTier: FreeTierProperties,
    val crawler: CrawlerProperties
) {
    data class ClerkProperties(val jwksUrl: String, val webhookSecret: String)
    data class FreeTierProperties(@DefaultValue("25") val monthlyReadLimit: Int)

    data class CrawlerProperties(
        @DefaultValue("true") val enabled: Boolean,
        val reddit: RedditProperties,
        val gfg: GfgProperties,
        val devto: DevToProperties,
        @DefaultValue("1000") val rateLimitMs: Long,
        @DefaultValue("50") val maxItemsPerRun: Int
    )

    data class RedditProperties(
        val userAgent: String,
        val subreddits: List<String>,
        val searchTerms: List<String>
    )

    data class GfgProperties(
        val baseUrl: String,
        val experiencePath: String
    )

    data class DevToProperties(
        val apiUrl: String,
        val tags: List<String>
    )
}
```

---

## 8.6 The Crawl Result — What Every Crawler Returns

Every crawler source returns the same structure regardless of where the content came from. This is the contract between crawlers and the orchestrator:

```kotlin
// src/main/kotlin/com/example/hirestory/crawler/CrawlResult.kt

package com.example.hirestory.crawler

data class CrawlResult(
    val url: String,           // The canonical URL of the post
    val title: String,         // Post title
    val rawContent: String,    // Full text content — unstructured
    val source: CrawlSource,   // Where this came from
    val publishedAt: String?   // When it was published (ISO string or null)
)

enum class CrawlSource {
    REDDIT,
    GEEKSFORGEEKS,
    DEVTO
}
```

---

## 8.7 Understanding Jsoup — HTML Parsing

Jsoup downloads a webpage and gives you a structured object you can query with CSS selectors. The same CSS selectors you know from web dev.

```kotlin
// How Jsoup works — the mental model

// 1. Fetch the page
val doc: Document = Jsoup.connect("https://www.geeksforgeeks.org/experiences/")
    .userAgent("HireStory/1.0")
    .timeout(10_000)       // 10 second timeout
    .get()

// 2. The document is a tree of HTML elements
// Select elements with CSS selectors

// Find all <a> tags with class "article-title"
val links: Elements = doc.select("a.article-title")

// Find the first <div> with id "content"
val content: Element? = doc.selectFirst("div#content")

// Find all <p> tags inside a specific div
val paragraphs: Elements = doc.select("div.post-content p")

// Get text content of an element
val text: String = content?.text() ?: ""

// Get an attribute value
val href: String = linkElement.attr("href")
val absoluteUrl: String = linkElement.attr("abs:href")   // abs: prefix = absolute URL
```

### Finding the Right CSS Selectors

Before writing any scraper, open the target website in Chrome:

1. Right-click on the content you want → Inspect
2. Look at the HTML structure around it
3. Find a unique class or ID that identifies that element
4. Test your selector in Chrome DevTools console: `document.querySelectorAll("your.selector")`

This is the actual skill of web scraping. The Jsoup code is easy once you have the right selectors.

---

## 8.8 The Reddit Crawler

Reddit provides a public JSON API — no scraping needed. Add `.json` to any Reddit URL and get structured data back.

```kotlin
// src/main/kotlin/com/example/hirestory/crawler/RedditCrawler.kt

package com.example.hirestory.crawler

import com.example.hirestory.config.HireStoryProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RedditCrawler(
    private val properties: HireStoryProperties,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(RedditCrawler::class.java)

    // OkHttpClient with timeouts — important for external HTTP calls
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun crawl(): List<CrawlResult> {
        val results = mutableListOf<CrawlResult>()
        val config = properties.crawler.reddit

        // Search each subreddit for each search term
        config.subreddits.forEach { subreddit ->
            config.searchTerms.forEach { searchTerm ->
                try {
                    val posts = searchSubreddit(subreddit, searchTerm)
                    results.addAll(posts)
                    // Rate limiting — be respectful to Reddit's API
                    Thread.sleep(properties.crawler.rateLimitMs)
                } catch (e: Exception) {
                    log.warn("Reddit crawl failed for r/{} '{}': {}", subreddit, searchTerm, e.message)
                }
            }
        }

        log.info("Reddit crawler found {} potential posts", results.size)
        return results
    }

    private fun searchSubreddit(subreddit: String, query: String): List<CrawlResult> {
        // Reddit's search API — returns JSON
        val url = "https://www.reddit.com/r/$subreddit/search.json" +
                  "?q=${query.replace(" ", "+")}" +
                  "&sort=new" +
                  "&restrict_sr=1" +    // Restrict to this subreddit
                  "&limit=25" +
                  "&t=week"            // Posts from the past week

        val request = Request.Builder()
            .url(url)
            // Reddit requires a descriptive User-Agent — they block generic ones
            .header("User-Agent", properties.crawler.reddit.userAgent)
            .build()

        val responseBody = httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                log.warn("Reddit API returned {}: {}", response.code, url)
                return emptyList()
            }
            response.body?.string() ?: return emptyList()
        }

        return parseRedditResponse(responseBody)
    }

    private fun parseRedditResponse(json: String): List<CrawlResult> {
        val results = mutableListOf<CrawlResult>()

        try {
            val root = objectMapper.readTree(json)
            val posts = root.path("data").path("children")

            posts.forEach { child ->
                val data = child.path("data")

                // Skip if removed, deleted, or too short
                if (data.path("removed_by_category").asText().isNotBlank()) return@forEach
                if (data.path("is_self").asBoolean(false).not()) return@forEach  // Skip link posts

                val title = data.path("title").asText()
                val selfText = data.path("selftext").asText()
                val permalink = data.path("permalink").asText()
                val createdUtc = data.path("created_utc").asLong()

                // Filter: must look like an interview experience post
                if (!isInterviewExperience(title, selfText)) return@forEach

                // Skip very short posts — not useful content
                if (selfText.length < 200) return@forEach

                val fullUrl = "https://www.reddit.com$permalink"

                results.add(
                    CrawlResult(
                        url = fullUrl,
                        title = title,
                        rawContent = buildRedditContent(title, selfText, data),
                        source = CrawlSource.REDDIT,
                        publishedAt = java.time.Instant.ofEpochSecond(createdUtc).toString()
                    )
                )
            }
        } catch (e: Exception) {
            log.error("Failed to parse Reddit response: {}", e.message)
        }

        return results
    }

    // Build a rich content string combining post title, text, and metadata
    // This is what gets sent to the AI for extraction
    private fun buildRedditContent(
        title: String,
        selfText: String,
        data: JsonNode
    ): String {
        val subreddit = data.path("subreddit_name_prefixed").asText()
        val score = data.path("score").asInt()
        val commentCount = data.path("num_comments").asInt()

        return """
            Source: Reddit $subreddit
            Title: $title
            Upvotes: $score | Comments: $commentCount
            
            $selfText
        """.trimIndent()
    }

    // Keywords that suggest this is a real interview experience post
    private val EXPERIENCE_INDICATORS = listOf(
        "interview experience", "got offer", "placement experience",
        "interview questions", "got selected", "cracked", "placed at",
        "my interview", "recently interviewed", "just got",
        "interview process", "selection process", "hiring process"
    )

    private fun isInterviewExperience(title: String, content: String): Boolean {
        val combined = (title + " " + content).lowercase()
        return EXPERIENCE_INDICATORS.any { indicator -> indicator in combined }
    }
}
```

---

## 8.9 The GeeksForGeeks Crawler

GFG has a dedicated interview experiences section. This uses Jsoup to scrape their HTML directly.

> **Legal note:** GFG's public interview experience pages are crawlable (their robots.txt allows it). Always check `site.com/robots.txt` before scraping any website.

```kotlin
// src/main/kotlin/com/example/hirestory/crawler/GfgCrawler.kt

package com.example.hirestory.crawler

import com.example.hirestory.config.HireStoryProperties
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class GfgCrawler(private val properties: HireStoryProperties) {

    private val log = LoggerFactory.getLogger(GfgCrawler::class.java)

    private val BASE_URL = "https://www.geeksforgeeks.org"
    private val EXPERIENCES_URL = "$BASE_URL/experiences/"

    fun crawl(): List<CrawlResult> {
        val results = mutableListOf<CrawlResult>()

        try {
            // Step 1: Fetch the experience listings page
            val listingDoc = fetchPage(EXPERIENCES_URL) ?: return emptyList()

            // Step 2: Extract article URLs from the listing page
            // GFG article links are in <a> tags with class "article-title"
            // Note: CSS selectors change when GFG updates their site
            // If this breaks, re-inspect their HTML and update the selector
            val articleLinks = listingDoc.select("a.article-title, div.article--viewer__content a")
                .map { it.attr("abs:href") }
                .filter { it.contains("geeksforgeeks.org") }
                .distinct()
                .take(properties.crawler.maxItemsPerRun)

            log.info("GFG: Found {} article links to process", articleLinks.size)

            // Step 3: Fetch and parse each article
            articleLinks.forEach { url ->
                try {
                    val result = parseArticle(url)
                    if (result != null) results.add(result)
                    Thread.sleep(properties.crawler.rateLimitMs)  // Rate limiting
                } catch (e: Exception) {
                    log.warn("GFG: Failed to parse article {}: {}", url, e.message)
                }
            }

        } catch (e: Exception) {
            log.error("GFG crawler failed: {}", e.message, e)
        }

        log.info("GFG crawler found {} usable articles", results.size)
        return results
    }

    private fun parseArticle(url: String): CrawlResult? {
        val doc = fetchPage(url) ?: return null

        // Extract the article title
        val title = doc.selectFirst("h1.article-title, h1")?.text()
            ?: return null   // No title = not a valid article

        // Skip if title does not suggest interview experience
        if (!isInterviewExperience(title)) return null

        // Extract the main article content
        // GFG wraps content in div with class "article--viewer__content"
        val contentDiv = doc.selectFirst("div.article--viewer__content, div.entry-content")
            ?: return null

        // Get all text paragraphs — skip code blocks and irrelevant sections
        val content = contentDiv.select("p, li")
            .joinToString("\n") { it.text() }
            .trim()

        // Skip very short articles — probably not a full experience
        if (content.length < 300) return null

        return CrawlResult(
            url = url,
            title = title,
            rawContent = "Source: GeeksForGeeks\nTitle: $title\n\n$content",
            source = CrawlSource.GEEKSFORGEEKS,
            publishedAt = extractPublishedDate(doc)
        )
    }

    private fun fetchPage(url: String): Document? {
        return try {
            Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; HireStoryBot/1.0)")
                .timeout(15_000)
                .followRedirects(true)
                .get()
        } catch (e: Exception) {
            log.warn("GFG: Failed to fetch {}: {}", url, e.message)
            null
        }
    }

    private fun extractPublishedDate(doc: Document): String? {
        // GFG shows date in a meta tag or a time element
        return doc.selectFirst("meta[property='article:published_time']")
            ?.attr("content")
            ?: doc.selectFirst("time[datetime]")?.attr("datetime")
    }

    private fun isInterviewExperience(title: String): Boolean {
        val lower = title.lowercase()
        return lower.contains("interview experience") ||
               lower.contains("placement experience") ||
               lower.contains("on-campus") ||
               lower.contains("off-campus") ||
               lower.contains("interview questions")
    }
}
```

---

## 8.10 The dev.to Crawler

dev.to has a public REST API — clean and well-documented. No scraping needed.

```kotlin
// src/main/kotlin/com/example/hirestory/crawler/DevToCrawler.kt

package com.example.hirestory.crawler

import com.example.hirestory.config.HireStoryProperties
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class DevToCrawler(
    private val properties: HireStoryProperties,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(DevToCrawler::class.java)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun crawl(): List<CrawlResult> {
        val results = mutableListOf<CrawlResult>()
        val config = properties.crawler.devto

        config.tags.forEach { tag ->
            try {
                val articles = fetchArticlesByTag(tag)
                results.addAll(articles)
                Thread.sleep(properties.crawler.rateLimitMs)
            } catch (e: Exception) {
                log.warn("dev.to crawl failed for tag '{}': {}", tag, e.message)
            }
        }

        log.info("dev.to crawler found {} potential articles", results.size)
        return results.filter { isInterviewExperience(it.title, it.rawContent) }
    }

    private fun fetchArticlesByTag(tag: String): List<CrawlResult> {
        // dev.to public API — no auth needed for reading
        val url = "${properties.crawler.devto.apiUrl}/articles?tag=$tag&per_page=30&state=fresh"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "HireStory/1.0")
            .build()

        val responseBody = httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            response.body?.string() ?: return emptyList()
        }

        return parseDevToResponse(responseBody)
    }

    private fun parseDevToResponse(json: String): List<CrawlResult> {
        val results = mutableListOf<CrawlResult>()

        try {
            val articles = objectMapper.readTree(json)

            articles.forEach { article ->
                val title = article.path("title").asText()
                val url = article.path("url").asText()
                val description = article.path("description").asText()
                val publishedAt = article.path("published_at").asText()
                val bodyMarkdown = article.path("body_markdown").asText()

                // dev.to API gives us the markdown content directly
                // If body_markdown is empty, we only have the description
                val content = if (bodyMarkdown.isNotBlank()) bodyMarkdown else description
                if (content.length < 200) return@forEach

                results.add(
                    CrawlResult(
                        url = url,
                        title = title,
                        rawContent = "Source: dev.to\nTitle: $title\n\n$content",
                        source = CrawlSource.DEVTO,
                        publishedAt = publishedAt.ifBlank { null }
                    )
                )
            }
        } catch (e: Exception) {
            log.error("Failed to parse dev.to response: {}", e.message)
        }

        return results
    }

    private fun isInterviewExperience(title: String, content: String): Boolean {
        val combined = (title + " " + content).lowercase()
        return listOf(
            "interview experience", "interview process", "got hired",
            "job interview", "technical interview", "system design interview",
            "coding interview", "placement", "cracked the interview"
        ).any { it in combined }
    }
}
```

---

## 8.11 The Crawl Job Service — Dedup and Storage

This service receives discovered URLs, deduplicates them, stores them, and queues them for processing:

```kotlin
// src/main/kotlin/com/example/hirestory/service/CrawlJobService.kt

package com.example.hirestory.service

import com.example.hirestory.crawler.CrawlResult
import com.example.hirestory.crawler.CrawlSource
import com.example.hirestory.entity.CrawlJob
import com.example.hirestory.entity.CrawlStatus
import com.example.hirestory.messaging.CrawlPublisher
import com.example.hirestory.repository.CrawlJobRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CrawlJobService(
    private val crawlJobRepository: CrawlJobRepository,
    private val crawlPublisher: CrawlPublisher,
    private val cacheService: CacheService
) {
    private val log = LoggerFactory.getLogger(CrawlJobService::class.java)

    // Process a batch of crawl results — deduplicate and queue new ones
    // Returns the count of newly queued jobs
    @Transactional
    fun processBatch(results: List<CrawlResult>): Int {
        var queued = 0

        results.forEach { result ->
            if (queueIfNew(result)) queued++
        }

        log.info("Processed batch of {} results, queued {} new jobs", results.size, queued)
        return queued
    }

    private fun queueIfNew(result: CrawlResult): Boolean {
        val url = result.url.normalizeUrl()

        // Layer 1: Redis check — fast, O(1)
        if (cacheService.isUrlAlreadyCrawled(url)) {
            log.debug("Skipping already-crawled URL (Redis): {}", url.take(80))
            return false
        }

        // Layer 2: Database check — catches cases where Redis was cleared
        if (crawlJobRepository.existsBySourceUrl(url)) {
            log.debug("Skipping already-crawled URL (DB): {}", url.take(80))
            // Mark in Redis so next time we skip at Redis level
            cacheService.isUrlAlreadyCrawled(url)   // Sets the Redis key
            return false
        }

        // New URL — create a crawl job
        val crawlJob = crawlJobRepository.save(
            CrawlJob(
                sourceUrl = url,
                rawText = result.rawContent.take(50_000),   // Cap at 50KB
                status = CrawlStatus.PENDING
            )
        )

        // Publish to RabbitMQ for AI processing (Chapter 9)
        // Note: This is AFTER the save — not inside a transaction
        // (See Chapter 7 checkpoint question 3)
        crawlPublisher.publishCrawlJob(
            crawlJobId = crawlJob.id!!,
            sourceUrl = url,
            rawText = result.rawContent
        )

        log.info("Queued new crawl job {}: {}", crawlJob.id, url.take(80))
        return true
    }

    // Normalize URLs to avoid duplicates with trivial differences
    private fun String.normalizeUrl(): String {
        return this
            .lowercase()
            .trimEnd('/')
            .replace("http://", "https://")    // Treat http and https as same
            .replace("www.", "")               // Treat www and non-www as same
            .substringBefore("?utm_")          // Remove UTM tracking params
            .substringBefore("&utm_")
    }
}
```

---

## 8.12 The Crawler Orchestrator

The orchestrator coordinates all crawlers, respects the master switch, and handles the scheduling:

```kotlin
// src/main/kotlin/com/example/hirestory/crawler/CrawlerOrchestrator.kt

package com.example.hirestory.crawler

import com.example.hirestory.config.HireStoryProperties
import com.example.hirestory.service.CrawlJobService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

@Component
class CrawlerOrchestrator(
    private val properties: HireStoryProperties,
    private val redditCrawler: RedditCrawler,
    private val gfgCrawler: GfgCrawler,
    private val devToCrawler: DevToCrawler,
    private val crawlJobService: CrawlJobService
) {
    private val log = LoggerFactory.getLogger(CrawlerOrchestrator::class.java)

    // Prevents two crawler runs from overlapping
    // If the 6-hour job has not finished and the next trigger fires, skip it
    private val isRunning = AtomicBoolean(false)

    // Run every 6 hours: at 00:00, 06:00, 12:00, 18:00
    @Scheduled(cron = "0 0 */6 * * *")
    fun scheduledCrawl() {
        run(triggeredManually = false)
    }

    // Called from admin endpoint for manual triggering
    fun run(triggeredManually: Boolean = false): CrawlerRunResult {
        if (!properties.crawler.enabled) {
            log.info("Crawler is disabled (hirestory.crawler.enabled=false)")
            return CrawlerRunResult(0, 0, "Crawler disabled")
        }

        // Prevent concurrent runs
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("Crawler already running — skipping this trigger")
            return CrawlerRunResult(0, 0, "Already running")
        }

        val trigger = if (triggeredManually) "MANUAL" else "SCHEDULED"
        log.info("Starting crawler run [{}]", trigger)
        val startTime = System.currentTimeMillis()

        try {
            val allResults = mutableListOf<CrawlResult>()

            // Run each crawler — collect all results
            runSafely("Reddit") { redditCrawler.crawl() }
                ?.let { allResults.addAll(it) }

            runSafely("GFG") { gfgCrawler.crawl() }
                ?.let { allResults.addAll(it) }

            runSafely("dev.to") { devToCrawler.crawl() }
                ?.let { allResults.addAll(it) }

            log.info("All crawlers complete. Total discovered: {}", allResults.size)

            // Deduplicate across sources (same URL from Reddit AND GFG)
            val uniqueResults = allResults
                .distinctBy { it.url.lowercase().trimEnd('/') }
                .take(properties.crawler.maxItemsPerRun)

            // Process: deduplicate against DB/Redis, save, queue
            val queued = crawlJobService.processBatch(uniqueResults)

            val duration = System.currentTimeMillis() - startTime
            log.info("Crawler run complete. Discovered: {}, Queued: {}, Duration: {}ms",
                uniqueResults.size, queued, duration)

            return CrawlerRunResult(
                discovered = uniqueResults.size,
                queued = queued,
                message = "Completed in ${duration}ms"
            )

        } finally {
            // Always release the lock — even if an exception is thrown
            isRunning.set(false)
        }
    }

    // Run a crawler and return null if it throws — do not let one failure stop others
    private fun runSafely(name: String, block: () -> List<CrawlResult>): List<CrawlResult>? {
        return try {
            log.info("Starting {} crawler", name)
            val results = block()
            log.info("{} crawler complete: {} results", name, results.size)
            results
        } catch (e: Exception) {
            log.error("{} crawler failed: {}", name, e.message, e)
            null   // Return null — orchestrator continues with other crawlers
        }
    }
}

data class CrawlerRunResult(
    val discovered: Int,
    val queued: Int,
    val message: String
)
```

---

## 8.13 The Admin Crawler Endpoint

Wire the orchestrator to the admin controller for manual triggering:

```kotlin
// Update AdminController.kt

@RestController
@RequestMapping("/admin")
class AdminController(
    private val adminService: AdminService,
    private val crawlerOrchestrator: CrawlerOrchestrator   // Add this
) {

    // POST /api/admin/crawler/trigger
    // Use this during development to test the crawler without waiting 6 hours
    @PostMapping("/crawler/trigger")
    fun triggerCrawler(): ResponseEntity<CrawlerTriggerResultDto> {
        val result = crawlerOrchestrator.run(triggeredManually = true)
        return ResponseEntity.ok(
            CrawlerTriggerResultDto(
                queued = result.queued,
                message = "Discovered: ${result.discovered}, Queued: ${result.queued}. ${result.message}"
            )
        )
    }

    // GET /api/admin/crawler/stats
    @GetMapping("/crawler/stats")
    fun getCrawlerStats(): ResponseEntity<CrawlerStatsDto> {
        return ResponseEntity.ok(adminService.getCrawlerStats())
    }
}
```

```kotlin
// Update AdminService.kt

fun getCrawlerStats(): CrawlerStatsDto {
    return CrawlerStatsDto(
        pending = crawlJobRepository.countByStatus(CrawlStatus.PENDING),
        processing = crawlJobRepository.countByStatus(CrawlStatus.PROCESSING),
        done = crawlJobRepository.countByStatus(CrawlStatus.DONE),
        failed = crawlJobRepository.countByStatus(CrawlStatus.FAILED),
        totalJobs = crawlJobRepository.count()
    )
}

data class CrawlerStatsDto(
    val pending: Long,
    val processing: Long,
    val done: Long,
    val failed: Long,
    val totalJobs: Long
)
```

---

## 8.14 Scheduler Configuration — One Scheduler Thread

By default, Spring uses a single thread for all `@Scheduled` tasks. If one task takes too long, it blocks all others. Configure a thread pool:

```kotlin
// src/main/kotlin/com/example/hirestory/config/SchedulerConfig.kt

package com.example.hirestory.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.config.ScheduledTaskRegistrar

@Configuration
class SchedulerConfig : SchedulingConfigurer {

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        val scheduler = ThreadPoolTaskScheduler().apply {
            poolSize = 3          // 3 threads — crawler, onboarding, any future jobs
            threadNamePrefix = "scheduler-"
            setWaitForTasksToCompleteOnShutdown(true)
            awaitTerminationSeconds = 60   // Wait up to 60s for tasks to finish on shutdown
            initialize()
        }
        taskRegistrar.setTaskScheduler(scheduler)
    }
}
```

---

## 8.15 Cron Expression Reference

```
# Cron format: second minute hour day-of-month month day-of-week

@Scheduled(cron = "0 0 */6 * * *")    # Every 6 hours
@Scheduled(cron = "0 0 10 * * *")     # Every day at 10:00 AM
@Scheduled(cron = "0 */30 * * * *")   # Every 30 minutes
@Scheduled(cron = "0 0 0 1 * *")      # First day of every month at midnight
@Scheduled(cron = "0 0 9 * * MON")    # Every Monday at 9:00 AM

# Simpler alternatives for simple intervals:
@Scheduled(fixedDelay = 3600000)       # Every 1 hour AFTER previous run finishes
@Scheduled(fixedRate = 3600000)        # Every 1 hour regardless of when previous finished
@Scheduled(initialDelay = 60000, fixedDelay = 3600000)
# Wait 1 minute after startup, then run every hour

# The difference between fixedDelay and fixedRate matters when your task takes time:
# fixedRate = 1 hour: if task takes 10 min, next run is 50 min after it finished
# fixedDelay = 1 hour: next run is always exactly 1 hour after previous RAN
# For crawlers: use fixedDelay — never overlap
```

---

## 8.16 Handling Robots.txt — Being a Good Crawler

Every website has a `robots.txt` that defines crawling rules. Respecting it is both legally important and ethically correct.

```kotlin
// src/main/kotlin/com/example/hirestory/crawler/RobotsChecker.kt

package com.example.hirestory.crawler

import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class RobotsChecker {

    private val log = LoggerFactory.getLogger(RobotsChecker::class.java)
    private val httpClient = OkHttpClient()

    // Cache robots.txt per domain — fetch once, check many times
    private val robotsCache = ConcurrentHashMap<String, String>()

    // Returns true if your bot is allowed to crawl this URL
    fun isAllowed(url: String, userAgent: String = "HireStoryBot"): Boolean {
        return try {
            val domain = extractDomain(url)
            val robotsTxt = robotsCache.getOrPut(domain) { fetchRobotsTxt(domain) }
            !isDisallowed(url, robotsTxt, userAgent)
        } catch (e: Exception) {
            log.warn("Could not check robots.txt for {}: {}", url, e.message)
            true   // If we cannot check, assume allowed (err on side of crawling)
        }
    }

    private fun fetchRobotsTxt(domain: String): String {
        return try {
            val request = Request.Builder()
                .url("$domain/robots.txt")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() ?: "" else ""
            }
        } catch (e: Exception) {
            ""   // Cannot fetch robots.txt — assume allowed
        }
    }

    private fun isDisallowed(url: String, robotsTxt: String, userAgent: String): Boolean {
        if (robotsTxt.isBlank()) return false

        var applicable = false
        val path = url.substringAfter("://").substringAfter("/").let { "/$it" }

        robotsTxt.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("User-agent:", ignoreCase = true) -> {
                    val agent = trimmed.substringAfter(":").trim()
                    applicable = agent == "*" || agent.equals(userAgent, ignoreCase = true)
                }
                applicable && trimmed.startsWith("Disallow:", ignoreCase = true) -> {
                    val disallowedPath = trimmed.substringAfter(":").trim()
                    if (disallowedPath.isNotBlank() && path.startsWith(disallowedPath)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun extractDomain(url: String): String {
        val uri = java.net.URI(url)
        return "${uri.scheme}://${uri.host}"
    }
}
```

---

## 8.17 Common Mistakes in Chapter 8

### Mistake 1 — Forgetting @EnableScheduling

```kotlin
// ❌ @Scheduled annotations do nothing — no error, just silence
@SpringBootApplication
class HireStoryApplication

// ✅ Activates all @Scheduled annotations
@SpringBootApplication
@EnableScheduling
class HireStoryApplication
```

### Mistake 2 — Not rate limiting external HTTP calls

```kotlin
// ❌ Hammering Reddit/GFG with requests — IP gets banned
config.subreddits.forEach { subreddit ->
    val posts = searchSubreddit(subreddit, searchTerm)  // No delay between calls
}

// ✅ Always sleep between calls to same domain
config.subreddits.forEach { subreddit ->
    val posts = searchSubreddit(subreddit, searchTerm)
    Thread.sleep(properties.crawler.rateLimitMs)   // 1 second between calls
}
```

### Mistake 3 — Not handling network timeouts

```kotlin
// ❌ No timeout — hangs forever if server is slow
val doc = Jsoup.connect(url).get()

// ✅ Always set timeout — external calls can hang for minutes
val doc = Jsoup.connect(url)
    .timeout(15_000)    // 15 seconds max
    .get()
```

### Mistake 4 — Allowing concurrent scheduler runs

```kotlin
// ❌ Two instances of the crawler run simultaneously — double processing
@Scheduled(cron = "0 0 */6 * * *")
fun scheduledCrawl() {
    crawl()   // If previous run takes > 6 hours, both run at the same time
}

// ✅ Guard with AtomicBoolean
private val isRunning = AtomicBoolean(false)

@Scheduled(cron = "0 0 */6 * * *")
fun scheduledCrawl() {
    if (!isRunning.compareAndSet(false, true)) return  // Already running — skip
    try { crawl() } finally { isRunning.set(false) }
}
```

### Mistake 5 — Publishing to RabbitMQ inside @Transactional

```kotlin
// ❌ If transaction rolls back, messages are already in the queue
@Transactional
fun queueIfNew(result: CrawlResult): Boolean {
    val crawlJob = crawlJobRepository.save(CrawlJob(...))
    crawlPublisher.publishCrawlJob(crawlJob.id!!, ...)  // Inside transaction — dangerous
    return true
}

// ✅ Publish AFTER transaction commits
@Transactional
fun saveCrawlJob(result: CrawlResult): CrawlJob {
    return crawlJobRepository.save(CrawlJob(...))
}

// In the orchestrator (outside @Transactional):
fun queueIfNew(result: CrawlResult): Boolean {
    val job = crawlJobService.saveCrawlJob(result)  // Transaction commits here
    crawlPublisher.publishCrawlJob(job.id!!, ...)    // Then publish
    return true
}
```

### Mistake 6 — Using absolute CSS selectors that break when site updates

```kotlin
// ❌ Brittle — breaks if GFG adds one more div wrapper
val title = doc.select("body > div.page > div.main > div.article > h1").text()

// ✅ Resilient — works as long as an h1 exists inside the article div
val title = doc.selectFirst("article h1, div.article h1, h1")?.text()
```

---

## 8.18 HireStory Connection — What You Built in Chapter 8

By the end of Chapter 8, HireStory has a complete content pipeline:

- `RedditCrawler` — searches three subreddits for interview experience posts using Reddit's public JSON API. Filters by keywords. Rate limits between calls.
- `GfgCrawler` — scrapes GeeksForGeeks experience pages using Jsoup. Extracts title and content. Respects rate limits.
- `DevToCrawler` — fetches posts by tag from dev.to's public API.
- `CrawlJobService` — two-layer deduplication (Redis + database). URL normalization. Saves to database and publishes to RabbitMQ AFTER the transaction commits.
- `CrawlerOrchestrator` — coordinates all crawlers, prevents concurrent runs with `AtomicBoolean`, collects and deduplicates across sources.
- `@Scheduled(cron = "0 0 */6 * * *")` — runs every 6 hours automatically.
- `POST /api/admin/crawler/trigger` — manual trigger for testing.
- `GET /api/admin/crawler/stats` — health monitoring for the admin panel.
- `SchedulerConfig` — 3-thread pool so scheduler never blocks itself.
- `RobotsChecker` — respects robots.txt before crawling any URL.

The crawl pipeline flow is now complete:

```
Scheduler fires every 6h
    → RedditCrawler + GfgCrawler + DevToCrawler discover URLs
    → CrawlJobService deduplicates and saves to DB
    → CrawlPublisher sends to RabbitMQ
    → CrawlConsumer picks up (Chapter 7)
    → AiExtractionService processes (Chapter 9)
    → Interview created and published automatically
```

---

## 8.19 Chapter Project — Build It Before You Move On

### What to build

A working crawler that discovers real content and saves it to your database.

**Step 1 — Add @EnableScheduling**

Add it to `HireStoryApplication`. Verify with a test:

```kotlin
@Component
class TestScheduler {
    private val log = LoggerFactory.getLogger(TestScheduler::class.java)

    @Scheduled(fixedDelay = 5000)   // Every 5 seconds
    fun test() {
        log.info("Scheduler is working!")
    }
}
```

See the log line every 5 seconds. Delete this after verifying.

**Step 2 — Add Jsoup**

Add the dependency. Write a standalone test:

```kotlin
val doc = Jsoup.connect("https://www.geeksforgeeks.org/experiences/")
    .userAgent("Mozilla/5.0")
    .timeout(15_000)
    .get()
println(doc.title())          // Should print the page title
println(doc.select("a").size) // Should print number of links
```

**Step 3 — Build the Reddit crawler**

Implement `RedditCrawler.crawl()`. Test it by calling it directly from a test. Print the first 3 results — verify they look like interview experiences.

**Step 4 — Build CrawlJobService**

Implement `processBatch()` with Redis + database deduplication. Test: call it twice with the same URLs. Second call should queue 0 new jobs.

**Step 5 — Wire the orchestrator**

Implement `CrawlerOrchestrator.run()`. Trigger it from the admin endpoint: `POST /api/admin/crawler/trigger`. Verify crawl jobs appear in your database.

**Step 6 — Check deduplication**

Trigger the crawler twice in a row. Verify the second run queues 0 new jobs (all URLs are already known).

### Checkpoint questions — answer before moving on

1. Your Reddit crawler finds 30 posts. Your GFG crawler finds 15 posts. Three of those URLs appear in both. After `distinctBy { it.url }`, how many unique results do you have? How many actually get queued if 10 were already in the database?
    
2. The `@Scheduled` cron job takes 8 hours to complete one run. Without the `AtomicBoolean` guard, what happens at the 6-hour mark when the scheduler fires again?
    
3. You change a GFG CSS selector from `"a.article-title"` to `"h2.title"`. The app restarts. What happens to the 500 existing crawl jobs in the database? What happens to future crawl runs?
    
4. Your `CrawlJobService.queueIfNew()` saves to the database inside `@Transactional` and then calls `crawlPublisher.publishCrawlJob()`. The transaction commits. The publisher throws an exception. What is the state of your system? Is this recoverable?
    
5. `Thread.sleep()` inside your crawler blocks the scheduler thread. If you have three `@Scheduled` jobs and one of them sleeps for 5 minutes between requests — what happens to the other two jobs if you only have one scheduler thread?
    

---

_Chapter 9 → Spring AI — Extracting Structured Data From Raw Text_

---

> **Book Progress:** Chapter 8 of 15 complete. Chapters ahead: Spring AI · Testing · Deployment