# HireStory — Chapters 6 to 10 (Multi-Module Monolith)

### Content Module · Redis · Notification Module · Crawler Project · AI Extraction

---

## Where Everything Lives Now

```
hirestory-backend/              ← single Spring Boot app (port 8080)
  core/                         ← entities, enums, exceptions, shared messaging
  user/                         ← Chapter 4 ✅ Chapter 5 ✅
  content/                      ← Chapter 6: interviews, feed, search, bookmarks
                                   Chapter 7: Redis cache, read counter
  notification/                 ← Chapter 10: FCM push, in-app list
  app/                          ← security, Redis config, RabbitMQ config, main

hirestory-crawler/              ← separate standalone project (not deployed)
                                   Chapter 8: web crawler, Reddit, GFG
                                   Chapter 9: AI extraction, RabbitMQ
```

> **Key rule:** `content` and `notification` modules call `UserService` directly as a Spring bean — no HTTP, no `RestTemplate`, no `UserLookupClient`. Everything runs in the same JVM.

---

# Chapter 6 — Content Module {#chapter-6}

## Theory

### What is Pagination?

When you have 10,000 interviews you cannot return all of them at once. Pagination splits results into pages.

```
GET /api/interviews?page=0&size=20  →  rows 1–20
GET /api/interviews?page=1&size=20  →  rows 21–40
```

Spring Data gives you this for free with `Pageable`:

```kotlin
// Repository method — just add Pageable parameter
fun findByStatus(status: InterviewStatus, pageable: Pageable): Page<Interview>

// Service usage
val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
val result   = repository.findByStatus(InterviewStatus.PUBLISHED, pageable)

// What Page<T> gives you:
result.content        // the actual list of items for this page
result.totalElements  // total count in DB
result.totalPages     // how many pages exist
result.number         // current page index (0-based)
result.isLast         // no more pages after this one
```

### What is JpaSpecificationExecutor?

For dynamic filters — user can filter by company AND/OR difficulty AND/OR outcome in any combination — you cannot write one query method per combination. `JpaSpecificationExecutor` lets you compose filters dynamically:

```kotlin
// Without Specification — 8+ query methods for every combination
fun findByCompanyAndDifficulty(...)
fun findByCompanyAndOutcome(...)
fun findByCompanyAndDifficultyAndOutcome(...)
// ... unmaintainable

// With Specification — one call handles every filter combination
interviewRepository.findAll(
    Specification.where(published())
        .and(companySlug?.let { byCompanySlug(it) })
        .and(difficulty?.let  { byDifficulty(it) })
        .and(outcome?.let     { byOutcome(it) }),
    pageable
)
```

### What is `nativeQuery = true`?

JPQL (Spring's query language) cannot express PostgreSQL-specific features like full-text search. For those, write raw SQL with `nativeQuery = true`:

```kotlin
@Query(
    value = "SELECT * FROM interviews WHERE search_vector @@ plainto_tsquery(:q)",
    nativeQuery = true
)
fun fullTextSearch(@Param("q") query: String, pageable: Pageable): Page<Interview>
```

### How Content Module Calls User Module

```kotlin
// OLD (microservices) — HTTP call, network overhead, can fail
val userId = restTemplate.getForObject("http://user-service/internal/users/$uid", ...)

// NEW (multi-module) — direct method call, same JVM, cannot fail due to network
val userId = userService.resolveUserId(firebaseUid)
```

`ContentService` and `UserService` are both Spring beans in the same application context. Kotlin sees them as a simple function call.

---

## Implementation

### Update `content/build.gradle.kts`

```kotlin
// content/build.gradle.kts
plugins {
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":user"))      // ← direct UserService access, no HTTP
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
}
```

### Content Module File Layout

```
content/src/main/kotlin/com/hirestory/content/
├── repository/
│   ├── InterviewRepository.kt
│   ├── InterviewSpecification.kt
│   ├── InterviewRoundRepository.kt
│   ├── CompanyRepository.kt
│   ├── TagRepository.kt
│   ├── BookmarkRepository.kt
│   ├── CommentRepository.kt
│   └── ReadHistoryRepository.kt
├── service/
│   ├── InterviewService.kt
│   ├── CompanyService.kt
│   ├── BookmarkService.kt
│   ├── ReadCounterService.kt          ← added Chapter 7
│   └── NotificationEventPublisher.kt  ← publishes to RabbitMQ
├── controller/
│   ├── InterviewController.kt
│   ├── CompanyController.kt
│   ├── AdminController.kt
│   └── InternalInterviewController.kt ← called by crawler
└── dto/
    ├── InterviewDtos.kt
    └── CompanyDtos.kt
```

### Shared Messaging Constants in `core`

Queue names must be identical between the publisher (`content`) and consumer (`notification`). Put them in `core` — both modules depend on it.

```kotlin
// core/src/main/kotlin/com/hirestory/core/messaging/MessagingConstants.kt
package com.hirestory.core.messaging

object MessagingConstants {
    // Notification queue — content publishes, notification consumes
    const val NOTIFICATION_QUEUE       = "notification.queue"
    const val NOTIFICATION_DLQ         = "notification.dlq"
    const val NOTIFICATION_EXCHANGE    = "notification.exchange"
    const val NOTIFICATION_ROUTING_KEY = "notification.send"
}
```

### Shared NotificationEvent in `core`

Both `content` (publisher) and `notification` (consumer) need the same class. Put it in `core`:

```kotlin
// core/src/main/kotlin/com/hirestory/core/messaging/NotificationEvent.kt
package com.hirestory.core.messaging

data class NotificationEvent(
    val type: String,                           // matches NotificationType name
    val recipientUserId: String,                // UUID as string
    val title: String,
    val body: String,
    val deepLink: String? = null,
    val data: Map<String, String> = emptyMap()
)
```

### Repositories

```kotlin
// content/src/main/kotlin/com/hirestory/content/repository/InterviewRepository.kt
package com.hirestory.content.repository

import com.hirestory.core.entity.Interview
import com.hirestory.core.enums.InterviewStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface InterviewRepository :
    JpaRepository<Interview, UUID>,
    JpaSpecificationExecutor<Interview> {

    @Query(
        value = """
            SELECT * FROM interviews
            WHERE status = 'PUBLISHED'
            AND search_vector @@ plainto_tsquery('english', :query)
            ORDER BY ts_rank(search_vector, plainto_tsquery('english', :query)) DESC
        """,
        countQuery = """
            SELECT COUNT(*) FROM interviews
            WHERE status = 'PUBLISHED'
            AND search_vector @@ plainto_tsquery('english', :query)
        """,
        nativeQuery = true
    )
    fun fullTextSearch(@Param("query") query: String, pageable: Pageable): Page<Interview>

    fun existsBySourceUrl(sourceUrl: String): Boolean
    fun countByStatus(status: InterviewStatus): Long
}
```

```kotlin
// content/src/main/kotlin/com/hirestory/content/repository/InterviewSpecification.kt
package com.hirestory.content.repository

import com.hirestory.core.entity.Company
import com.hirestory.core.entity.Interview
import com.hirestory.core.enums.Difficulty
import com.hirestory.core.enums.InterviewStatus
import com.hirestory.core.enums.Outcome
import jakarta.persistence.criteria.JoinType
import org.springframework.data.jpa.domain.Specification
import java.util.UUID

object InterviewSpecification {

    fun published(): Specification<Interview> =
        Specification { root, _, cb ->
            cb.equal(root.get<InterviewStatus>("status"), InterviewStatus.PUBLISHED)
        }

    fun pending(): Specification<Interview> =
        Specification { root, _, cb ->
            cb.equal(root.get<InterviewStatus>("status"), InterviewStatus.PENDING)
        }

    fun byCompanySlug(slug: String): Specification<Interview> =
        Specification { root, _, cb ->
            val company = root.join<Interview, Company>("company", JoinType.INNER)
            cb.equal(company.get<String>("slug"), slug.lowercase())
        }

    fun byDifficulty(difficulty: Difficulty): Specification<Interview> =
        Specification { root, _, cb ->
            cb.equal(root.get<Difficulty>("difficulty"), difficulty)
        }

    fun byOutcome(outcome: Outcome): Specification<Interview> =
        Specification { root, _, cb ->
            cb.equal(root.get<Outcome>("outcome"), outcome)
        }

    fun byRole(role: String): Specification<Interview> =
        Specification { root, _, cb ->
            cb.like(cb.lower(root.get("role")), "%${role.lowercase()}%")
        }

    fun excludeReadByUser(readInterviewIds: List<UUID>): Specification<Interview> =
        Specification { root, _, cb ->
            if (readInterviewIds.isEmpty()) cb.conjunction()
            else root.get<UUID>("id").`in`(readInterviewIds).not()
        }
}
```

```kotlin
// content/src/main/kotlin/com/hirestory/content/repository/InterviewRoundRepository.kt
package com.hirestory.content.repository

import com.hirestory.core.entity.InterviewRound
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface InterviewRoundRepository : JpaRepository<InterviewRound, UUID>
```

```kotlin
// content/src/main/kotlin/com/hirestory/content/repository/CompanyRepository.kt
package com.hirestory.content.repository

import com.hirestory.core.entity.Company
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CompanyRepository : JpaRepository<Company, UUID> {
    fun findBySlug(slug: String): Company?
    fun findByNameIgnoreCase(name: String): Company?
    fun findAllByOrderByInterviewCountDesc(): List<Company>

    @Modifying
    @Query("UPDATE Company c SET c.interviewCount = c.interviewCount + 1 WHERE c.id = :id")
    fun incrementInterviewCount(@Param("id") id: UUID)
}
```

```kotlin
// content/src/main/kotlin/com/hirestory/content/repository/TagRepository.kt
package com.hirestory.content.repository

import com.hirestory.core.entity.Tag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TagRepository : JpaRepository<Tag, UUID> {
    fun findByName(name: String): Tag?
}
```

```kotlin
// content/src/main/kotlin/com/hirestory/content/repository/BookmarkRepository.kt
package com.hirestory.content.repository

import com.hirestory.core.entity.Bookmark
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BookmarkRepository : JpaRepository<Bookmark, UUID> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID, pageable: Pageable): Page<Bookmark>
    fun existsByUserIdAndInterviewId(userId: UUID, interviewId: UUID): Boolean
    fun deleteByUserIdAndInterviewId(userId: UUID, interviewId: UUID)
}
```

```kotlin
// content/src/main/kotlin/com/hirestory/content/repository/CommentRepository.kt
package com.hirestory.content.repository

import com.hirestory.core.entity.Comment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CommentRepository : JpaRepository<Comment, UUID> {
    fun findByInterviewIdAndIsDeletedFalseOrderByCreatedAtAsc(
        interviewId: UUID, pageable: Pageable
    ): Page<Comment>

    @Modifying
    @Query("UPDATE Comment c SET c.isDeleted = true WHERE c.id = :id AND c.userId = :userId")
    fun softDelete(@Param("id") id: UUID, @Param("userId") userId: UUID): Int
}
```

```kotlin
// content/src/main/kotlin/com/hirestory/content/repository/ReadHistoryRepository.kt
package com.hirestory.content.repository

import com.hirestory.core.entity.ReadHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ReadHistoryRepository : JpaRepository<ReadHistory, UUID> {
    fun existsByUserIdAndInterviewId(userId: UUID, interviewId: UUID): Boolean

    @Query("SELECT r.interviewId FROM ReadHistory r WHERE r.userId = :userId")
    fun findInterviewIdsByUserId(@Param("userId") userId: UUID): List<UUID>
}
```

### DTOs

```kotlin
// content/src/main/kotlin/com/hirestory/content/dto/InterviewDtos.kt
package com.hirestory.content.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

// ── Response DTOs ──────────────────────────────────────────────────────────

data class InterviewSummaryResponse(
    val id: UUID,
    val companyName: String,
    val companyLogoUrl: String?,
    val companySlug: String,
    val title: String,
    val role: String?,
    val difficulty: String,
    val outcome: String,
    val roundsCount: Int,
    val salaryLpa: BigDecimal?,
    val tags: List<String>,
    val createdAt: LocalDateTime
)

data class InterviewDetailResponse(
    val id: UUID,
    val companyName: String,
    val companyLogoUrl: String?,
    val companySlug: String,
    val title: String,
    val content: String,
    val role: String?,
    val location: String?,
    val experienceYears: Int?,
    val roundsCount: Int,
    val rounds: List<RoundResponse>,
    val difficulty: String,
    val outcome: String,
    val salaryLpa: BigDecimal?,
    val tags: List<String>,
    val sourceUrl: String?,
    val sourceType: String,
    val createdAt: LocalDateTime,
    val publishedAt: LocalDateTime?
)

data class RoundResponse(
    val roundNumber: Int,
    val title: String,
    val questions: String?,
    val difficulty: String?,
    val notes: String?
)

data class InterviewShortsResponse(
    val id: UUID,
    val companyName: String,
    val companyLogoUrl: String?,
    val role: String?,
    val difficulty: String,
    val outcome: String,
    val roundsCount: Int,
    val preview: String,       // first 200 chars of content
    val tags: List<String>
)

// ── Request DTOs ───────────────────────────────────────────────────────────

data class CreateInterviewRequest(
    @field:NotBlank(message = "Title is required")
    val title: String,

    @field:NotBlank(message = "Content is required")
    @field:Size(min = 100, message = "Content must be at least 100 characters")
    val content: String,

    @field:NotBlank(message = "Company name is required")
    val companyName: String,

    val companySlug: String = "",

    val role: String?,
    val location: String?,
    val experienceYears: Int?,

    @field:NotEmpty(message = "At least one round is required")
    val rounds: List<CreateRoundRequest>,

    @field:NotBlank(message = "Difficulty is required")
    val difficulty: String,

    @field:NotBlank(message = "Outcome is required")
    val outcome: String,

    val salaryLpa: BigDecimal? = null,
    val tagNames: List<String> = emptyList(),
    val isAnonymous: Boolean = false
)

data class CreateRoundRequest(
    val title: String,
    val questions: String? = null,
    val difficulty: String? = null,
    val notes: String? = null
)

// ── Internal — called by crawler ───────────────────────────────────────────

data class CrawledInterviewRequest(
    val companyName: String,
    val title: String,
    val content: String,
    val role: String?,
    val location: String?,
    val experienceYears: Int?,
    val difficulty: String,
    val outcome: String,
    val roundsCount: Int,
    val rounds: List<CreateRoundRequest>,
    val tagNames: List<String>,
    val salaryLpa: BigDecimal?,
    val sourceUrl: String,
    val confidenceScore: Int,
    val autoPublish: Boolean   // true = PUBLISHED, false = PENDING for admin review
)
```

```kotlin
// content/src/main/kotlin/com/hirestory/content/dto/CompanyDtos.kt
package com.hirestory.content.dto

import java.util.UUID

data class CompanyResponse(
    val id: UUID,
    val name: String,
    val logoUrl: String?,
    val slug: String,
    val interviewCount: Int
)
```

### NotificationEventPublisher

```kotlin
// content/src/main/kotlin/com/hirestory/content/service/NotificationEventPublisher.kt
package com.hirestory.content.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.hirestory.core.messaging.MessagingConstants
import com.hirestory.core.messaging.NotificationEvent
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class NotificationEventPublisher(
    private val rabbitTemplate: RabbitTemplate,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(NotificationEventPublisher::class.java)

    fun commentOnInterview(
        interviewAuthorId: UUID,
        commenterName: String,
        interviewId: UUID,
        companyName: String
    ) {
        publish(
            NotificationEvent(
                type            = "COMMENT_ON_YOUR_INTERVIEW",
                recipientUserId = interviewAuthorId.toString(),
                title           = "New comment on your interview",
                body            = "$commenterName commented on your $companyName interview",
                deepLink        = "hirestory://interview/$interviewId?comments=1"
            )
        )
    }

    fun interviewPublished(submitterId: UUID, interviewId: UUID, companyName: String) {
        publish(
            NotificationEvent(
                type            = "INTERVIEW_PUBLISHED",
                recipientUserId = submitterId.toString(),
                title           = "Your interview is live! 🎉",
                body            = "Your $companyName interview experience is now visible to everyone",
                deepLink        = "hirestory://interview/$interviewId"
            )
        )
    }

    fun referralUsed(referrerId: UUID, referredUserName: String) {
        publish(
            NotificationEvent(
                type            = "REFERRAL_USED",
                recipientUserId = referrerId.toString(),
                title           = "Someone used your referral code!",
                body            = "$referredUserName joined using your code. You got +5 bonus reads!",
                deepLink        = "hirestory://profile"
            )
        )
    }

    private fun publish(event: NotificationEvent) {
        try {
            val json = objectMapper.writeValueAsString(event)
            rabbitTemplate.convertAndSend(
                MessagingConstants.NOTIFICATION_EXCHANGE,
                MessagingConstants.NOTIFICATION_ROUTING_KEY,
                json
            )
        } catch (e: Exception) {
            // Never fail the main operation because of a notification failure
            log.error("Failed to publish notification event: ${e.message}")
        }
    }
}
```

### InterviewService

```kotlin
// content/src/main/kotlin/com/hirestory/content/service/InterviewService.kt
package com.hirestory.content.service

import com.hirestory.content.dto.*
import com.hirestory.content.repository.*
import com.hirestory.core.entity.*
import com.hirestory.core.enums.*
import com.hirestory.core.exception.BadRequestException
import com.hirestory.core.exception.ResourceNotFoundException
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class InterviewService(
    private val interviewRepository: InterviewRepository,
    private val interviewRoundRepository: InterviewRoundRepository,
    private val companyRepository: CompanyRepository,
    private val tagRepository: TagRepository,
    private val readHistoryRepository: ReadHistoryRepository,
    private val notificationEventPublisher: NotificationEventPublisher
) {

    // ── Feed ──────────────────────────────────────────────────────────────

    @Cacheable(
        value  = ["interviews-feed"],
        key    = "'p'+#page+'_s'+#size+'_c'+#companySlug+'_d'+#difficulty+'_o'+#outcome+'_r'+#role"
    )
    fun getFeed(
        page: Int,
        size: Int = 20,
        companySlug: String? = null,
        difficulty: String? = null,
        outcome: String? = null,
        role: String? = null,
        currentUserId: UUID? = null
    ): Page<InterviewSummaryResponse> {

        var spec = InterviewSpecification.published()

        companySlug?.let { spec = spec.and(InterviewSpecification.byCompanySlug(it)) }

        difficulty?.let {
            runCatching { Difficulty.valueOf(it.uppercase()) }.getOrNull()
                ?.let { d -> spec = spec.and(InterviewSpecification.byDifficulty(d)) }
        }

        outcome?.let {
            runCatching { Outcome.valueOf(it.uppercase()) }.getOrNull()
                ?.let { o -> spec = spec.and(InterviewSpecification.byOutcome(o)) }
        }

        role?.let { spec = spec.and(InterviewSpecification.byRole(it)) }

        // Exclude already-read interviews so logged-in users see fresh content
        if (currentUserId != null) {
            val readIds = readHistoryRepository.findInterviewIdsByUserId(currentUserId)
            if (readIds.isNotEmpty()) {
                spec = spec.and(InterviewSpecification.excludeReadByUser(readIds))
            }
        }

        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        return interviewRepository.findAll(spec, pageable).map { it.toSummary() }
    }

    // ── Detail ────────────────────────────────────────────────────────────

    @Cacheable(value = ["interview-detail"], key = "#id")
    fun getDetail(id: UUID): InterviewDetailResponse {
        val interview = interviewRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Interview not found: $id") }

        if (interview.status != InterviewStatus.PUBLISHED) {
            throw ResourceNotFoundException("Interview not found: $id")
        }

        return interview.toDetail()
    }

    // ── Search ────────────────────────────────────────────────────────────

    fun search(query: String, page: Int): Page<InterviewSummaryResponse> {
        if (query.trim().length < 2) throw BadRequestException("Query too short")
        val pageable = PageRequest.of(page, 20)
        return interviewRepository.fullTextSearch(query.trim(), pageable).map { it.toSummary() }
    }

    // ── Shorts ────────────────────────────────────────────────────────────

    @Cacheable(value = ["shorts-feed"], key = "'p'+#page")
    fun getShorts(page: Int): Page<InterviewShortsResponse> {
        val pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending())
        return interviewRepository.findAll(InterviewSpecification.published(), pageable)
            .map { it.toShorts() }
    }

    // ── Submit (user-facing) ──────────────────────────────────────────────

    @Transactional
    fun submitInterview(request: CreateInterviewRequest, userId: UUID): InterviewSummaryResponse {
        val company = findOrCreateCompany(request.companyName, request.companySlug)
        val tags    = findOrCreateTags(request.tagNames)

        val interview = interviewRepository.save(
            Interview(
                company          = company,
                userId           = if (request.isAnonymous) null else userId,
                title            = request.title,
                content          = request.content,
                role             = request.role,
                location         = request.location,
                experienceYears  = request.experienceYears,
                roundsCount      = request.rounds.size,
                difficulty       = Difficulty.valueOf(request.difficulty.uppercase()),
                outcome          = Outcome.valueOf(request.outcome.uppercase()),
                salaryLpa        = request.salaryLpa,
                sourceType       = SourceType.USER_SUBMITTED,
                status           = InterviewStatus.PENDING,   // admin must approve
                tags             = tags
            )
        )

        saveRounds(interview, request.rounds)
        return interview.toSummary()
    }

    // ── Create from crawler (internal) ────────────────────────────────────

    @Transactional
    fun createFromCrawl(request: CrawledInterviewRequest): UUID {
        val company = findOrCreateCompany(request.companyName, "")
        val tags    = findOrCreateTags(request.tagNames)

        val status = if (request.autoPublish) InterviewStatus.PUBLISHED else InterviewStatus.PENDING

        val interview = interviewRepository.save(
            Interview(
                company          = company,
                userId           = null,              // crawled interviews have no user
                title            = request.title,
                content          = request.content,
                role             = request.role,
                location         = request.location,
                experienceYears  = request.experienceYears,
                roundsCount      = request.roundsCount,
                difficulty       = runCatching { Difficulty.valueOf(request.difficulty.uppercase()) }
                                       .getOrDefault(Difficulty.MEDIUM),
                outcome          = runCatching { Outcome.valueOf(request.outcome.uppercase()) }
                                       .getOrDefault(Outcome.OFFER),
                salaryLpa        = request.salaryLpa,
                sourceUrl        = request.sourceUrl,
                sourceType       = SourceType.CRAWLED,
                status           = status,
                confidenceScore  = request.confidenceScore,
                tags             = tags,
                publishedAt      = if (request.autoPublish) LocalDateTime.now() else null
            )
        )

        saveRounds(interview, request.rounds)

        if (request.autoPublish) {
            companyRepository.incrementInterviewCount(company.id)
        }

        return interview.id
    }

    // ── Admin: Approve ────────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = ["interviews-feed", "shorts-feed"], allEntries = true)
    fun approveInterview(id: UUID): InterviewDetailResponse {
        val interview = interviewRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Interview not found: $id") }

        if (interview.status == InterviewStatus.PUBLISHED) {
            throw BadRequestException("Interview already published")
        }

        interview.status      = InterviewStatus.PUBLISHED
        interview.publishedAt = LocalDateTime.now()
        val saved = interviewRepository.save(interview)

        companyRepository.incrementInterviewCount(interview.company.id)

        // Notify the user who submitted this interview
        if (interview.userId != null) {
            notificationEventPublisher.interviewPublished(
                submitterId = interview.userId!!,
                interviewId = saved.id,
                companyName = interview.company.name
            )
        }

        return saved.toDetail()
    }

    @Transactional
    fun rejectInterview(id: UUID): InterviewDetailResponse {
        val interview = interviewRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Interview not found: $id") }
        interview.status = InterviewStatus.REJECTED
        return interviewRepository.save(interview).toDetail()
    }

    fun getPendingInterviews(page: Int): Page<InterviewDetailResponse> {
        val pageable = PageRequest.of(page, 20, Sort.by("createdAt").descending())
        return interviewRepository.findAll(InterviewSpecification.pending(), pageable)
            .map { it.toDetail() }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private fun findOrCreateCompany(name: String, slugHint: String): Company {
        val slug = slugHint.ifBlank {
            name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        }
        return companyRepository.findBySlug(slug)
            ?: companyRepository.findByNameIgnoreCase(name)
            ?: companyRepository.save(Company(name = name, slug = slug))
    }

    private fun findOrCreateTags(tagNames: List<String>): List<Tag> =
        tagNames.map { name ->
            tagRepository.findByName(name) ?: tagRepository.save(Tag(name = name))
        }

    private fun saveRounds(interview: Interview, rounds: List<CreateRoundRequest>) {
        rounds.forEachIndexed { index, r ->
            interviewRoundRepository.save(
                InterviewRound(
                    interview   = interview,
                    roundNumber = index + 1,
                    title       = r.title,
                    questions   = r.questions,
                    difficulty  = r.difficulty,
                    notes       = r.notes
                )
            )
        }
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    private fun Interview.toSummary() = InterviewSummaryResponse(
        id             = id,
        companyName    = company.name,
        companyLogoUrl = company.logoUrl,
        companySlug    = company.slug,
        title          = title,
        role           = role,
        difficulty     = difficulty.name,
        outcome        = outcome.name,
        roundsCount    = roundsCount,
        salaryLpa      = salaryLpa,
        tags           = tags.map { it.name },
        createdAt      = createdAt
    )

    private fun Interview.toDetail() = InterviewDetailResponse(
        id             = id,
        companyName    = company.name,
        companyLogoUrl = company.logoUrl,
        companySlug    = company.slug,
        title          = title,
        content        = content,
        role           = role,
        location       = location,
        experienceYears= experienceYears,
        roundsCount    = roundsCount,
        rounds         = rounds.map {
            RoundResponse(it.roundNumber, it.title, it.questions, it.difficulty, it.notes)
        },
        difficulty     = difficulty.name,
        outcome        = outcome.name,
        salaryLpa      = salaryLpa,
        tags           = tags.map { it.name },
        sourceUrl      = sourceUrl,
        sourceType     = sourceType.name,
        createdAt      = createdAt,
        publishedAt    = publishedAt
    )

    private fun Interview.toShorts() = InterviewShortsResponse(
        id             = id,
        companyName    = company.name,
        companyLogoUrl = company.logoUrl,
        role           = role,
        difficulty     = difficulty.name,
        outcome        = outcome.name,
        roundsCount    = roundsCount,
        preview        = content.take(200),
        tags           = tags.map { it.name }
    )
}
```

### BookmarkService

```kotlin
// content/src/main/kotlin/com/hirestory/content/service/BookmarkService.kt
package com.hirestory.content.service

import com.hirestory.content.dto.InterviewSummaryResponse
import com.hirestory.content.repository.BookmarkRepository
import com.hirestory.content.repository.InterviewRepository
import com.hirestory.core.entity.Bookmark
import com.hirestory.core.exception.ResourceNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BookmarkService(
    private val bookmarkRepository: BookmarkRepository,
    private val interviewRepository: InterviewRepository
) {

    @Transactional
    fun addBookmark(userId: UUID, interviewId: UUID) {
        if (bookmarkRepository.existsByUserIdAndInterviewId(userId, interviewId)) return

        val interview = interviewRepository.findById(interviewId)
            .orElseThrow { ResourceNotFoundException("Interview not found: $interviewId") }

        bookmarkRepository.save(Bookmark(userId = userId, interview = interview))
    }

    @Transactional
    fun removeBookmark(userId: UUID, interviewId: UUID) {
        bookmarkRepository.deleteByUserIdAndInterviewId(userId, interviewId)
    }

    fun getBookmarks(userId: UUID, page: Int): Page<InterviewSummaryResponse> {
        val pageable = PageRequest.of(page, 20, Sort.by("createdAt").descending())
        return bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map { bookmark ->
                val i = bookmark.interview
                InterviewSummaryResponse(
                    id             = i.id,
                    companyName    = i.company.name,
                    companyLogoUrl = i.company.logoUrl,
                    companySlug    = i.company.slug,
                    title          = i.title,
                    role           = i.role,
                    difficulty     = i.difficulty.name,
                    outcome        = i.outcome.name,
                    roundsCount    = i.roundsCount,
                    salaryLpa      = i.salaryLpa,
                    tags           = i.tags.map { it.name },
                    createdAt      = i.createdAt
                )
            }
    }
}
```

### CompanyService

```kotlin
// content/src/main/kotlin/com/hirestory/content/service/CompanyService.kt
package com.hirestory.content.service

import com.hirestory.content.dto.CompanyResponse
import com.hirestory.content.repository.CompanyRepository
import com.hirestory.core.exception.ResourceNotFoundException
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class CompanyService(private val companyRepository: CompanyRepository) {

    @Cacheable(value = ["companies"])
    fun getAllCompanies(): List<CompanyResponse> =
        companyRepository.findAllByOrderByInterviewCountDesc()
            .map { CompanyResponse(it.id, it.name, it.logoUrl, it.slug, it.interviewCount) }

    @Cacheable(value = ["company-detail"], key = "#slug")
    fun getBySlug(slug: String): CompanyResponse {
        val company = companyRepository.findBySlug(slug)
            ?: throw ResourceNotFoundException("Company not found: $slug")
        return CompanyResponse(company.id, company.name, company.logoUrl, company.slug, company.interviewCount)
    }
}
```

### Controllers

```kotlin
// content/src/main/kotlin/com/hirestory/content/controller/InterviewController.kt
package com.hirestory.content.controller

import com.hirestory.content.dto.*
import com.hirestory.content.service.*
import com.hirestory.user.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api")
class InterviewController(
    private val interviewService: InterviewService,
    private val bookmarkService: BookmarkService,
    private val readCounterService: ReadCounterService,
    private val userService: UserService          // ← direct bean, no HTTP
) {

    // ── Feed ──────────────────────────────────────────────────────────────

    @GetMapping("/interviews")
    fun getFeed(
        @RequestParam(defaultValue = "0")  page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) company: String?,
        @RequestParam(required = false) difficulty: String?,
        @RequestParam(required = false) outcome: String?,
        @RequestParam(required = false) role: String?,
        @AuthenticationPrincipal firebaseUid: String?   // null for anonymous users
    ): ResponseEntity<Page<InterviewSummaryResponse>> {
        val userId = firebaseUid?.let { userService.resolveUserId(it) }
        return ResponseEntity.ok(
            interviewService.getFeed(page, size, company, difficulty, outcome, role, userId)
        )
    }

    // ── Detail ────────────────────────────────────────────────────────────

    @GetMapping("/interviews/{id}")
    fun getDetail(
        @PathVariable id: UUID,
        @AuthenticationPrincipal firebaseUid: String?
    ): ResponseEntity<InterviewDetailResponse> {
        val interview = interviewService.getDetail(id)

        // Check paywall for logged-in users
        if (firebaseUid != null) {
            val resolved  = userService.resolveUser(firebaseUid)
            val canRead   = readCounterService.checkAndRecordRead(resolved.id, id, resolved.isPremium)
            if (!canRead) return ResponseEntity.status(402).build()
        }

        return ResponseEntity.ok(interview)
    }

    // ── Search ────────────────────────────────────────────────────────────

    @GetMapping("/search")
    fun search(
        @RequestParam q: String,
        @RequestParam(defaultValue = "0") page: Int
    ): ResponseEntity<Page<InterviewSummaryResponse>> =
        ResponseEntity.ok(interviewService.search(q, page))

    // ── Shorts ────────────────────────────────────────────────────────────

    @GetMapping("/shorts")
    fun getShorts(
        @RequestParam(defaultValue = "0") page: Int
    ): ResponseEntity<Page<InterviewShortsResponse>> =
        ResponseEntity.ok(interviewService.getShorts(page))

    // ── Submit ────────────────────────────────────────────────────────────

    @PostMapping("/interviews")
    fun submit(
        @Valid @RequestBody request: CreateInterviewRequest,
        @AuthenticationPrincipal firebaseUid: String
    ): ResponseEntity<InterviewSummaryResponse> {
        val userId = userService.resolveUserId(firebaseUid)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(interviewService.submitInterview(request, userId))
    }

    // ── Bookmarks ─────────────────────────────────────────────────────────

    @PostMapping("/interviews/{id}/bookmark")
    fun addBookmark(
        @PathVariable id: UUID,
        @AuthenticationPrincipal firebaseUid: String
    ): ResponseEntity<Unit> {
        bookmarkService.addBookmark(userService.resolveUserId(firebaseUid), id)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/interviews/{id}/bookmark")
    fun removeBookmark(
        @PathVariable id: UUID,
        @AuthenticationPrincipal firebaseUid: String
    ): ResponseEntity<Unit> {
        bookmarkService.removeBookmark(userService.resolveUserId(firebaseUid), id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/bookmarks")
    fun getBookmarks(
        @RequestParam(defaultValue = "0") page: Int,
        @AuthenticationPrincipal firebaseUid: String
    ): ResponseEntity<Page<InterviewSummaryResponse>> =
        ResponseEntity.ok(bookmarkService.getBookmarks(userService.resolveUserId(firebaseUid), page))
}
```

```kotlin
// content/src/main/kotlin/com/hirestory/content/controller/CompanyController.kt
package com.hirestory.content.controller

import com.hirestory.content.dto.CompanyResponse
import com.hirestory.content.service.CompanyService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/companies")
class CompanyController(private val companyService: CompanyService) {

    @GetMapping
    fun getAllCompanies(): ResponseEntity<List<CompanyResponse>> =
        ResponseEntity.ok(companyService.getAllCompanies())

    @GetMapping("/{slug}")
    fun getBySlug(@PathVariable slug: String): ResponseEntity<CompanyResponse> =
        ResponseEntity.ok(companyService.getBySlug(slug))
}
```

```kotlin
// content/src/main/kotlin/com/hirestory/content/controller/AdminController.kt
package com.hirestory.content.controller

import com.hirestory.content.dto.InterviewDetailResponse
import com.hirestory.content.service.InterviewService
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin")
class AdminController(private val interviewService: InterviewService) {

    @GetMapping("/interviews/pending")
    fun getPending(
        @RequestParam(defaultValue = "0") page: Int
    ): ResponseEntity<Page<InterviewDetailResponse>> =
        ResponseEntity.ok(interviewService.getPendingInterviews(page))

    @PutMapping("/interviews/{id}/approve")
    fun approve(@PathVariable id: UUID): ResponseEntity<InterviewDetailResponse> =
        ResponseEntity.ok(interviewService.approveInterview(id))

    @PutMapping("/interviews/{id}/reject")
    fun reject(@PathVariable id: UUID): ResponseEntity<InterviewDetailResponse> =
        ResponseEntity.ok(interviewService.rejectInterview(id))
}
```

```kotlin
// content/src/main/kotlin/com/hirestory/content/controller/InternalInterviewController.kt
package com.hirestory.content.controller

import com.hirestory.content.dto.CrawledInterviewRequest
import com.hirestory.content.service.InterviewService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Called by hirestory-crawler project only.
 * Not exposed to internet — SecurityConfig permits /internal/** from localhost.
 * In production: add API key header validation.
 */
@RestController
@RequestMapping("/internal")
class InternalInterviewController(private val interviewService: InterviewService) {

    @PostMapping("/interviews/from-crawl")
    fun createFromCrawl(
        @RequestBody request: CrawledInterviewRequest
    ): ResponseEntity<Map<String, String>> {
        val id = interviewService.createFromCrawl(request)
        return ResponseEntity.ok(mapOf("id" to id.toString()))
    }
}
```

---

## Chapter 6 Checkpoint ✅

- [ ] `content/build.gradle.kts` includes `:user` and `:core` dependencies
- [ ] All repositories created — project compiles without errors
- [ ] `core/messaging/MessagingConstants.kt` and `NotificationEvent.kt` created
- [ ] `./gradlew :app:bootRun` starts without errors
- [ ] `GET /api/companies` → 200 with 15 seed companies
- [ ] `GET /api/interviews` → 200 with empty list (no published interviews yet)
- [ ] `POST /api/interviews` with valid Firebase token → creates PENDING interview in DB
- [ ] `PUT /api/admin/interviews/{id}/approve` → interview status = PUBLISHED
- [ ] `GET /api/interviews` → 200 now shows the approved interview
- [ ] `GET /api/search?q=google` → returns matching interviews

---

# Chapter 7 — Redis Caching & Read Counter {#chapter-7}

## Theory

### What is Caching?

Without cache, every feed request hits PostgreSQL:

```
GET /api/interviews?page=0
  → JOIN query across interviews + companies + tags
  → 50–200ms per request
  → 1,000 concurrent users = 1,000 DB queries/second → DB melts
```

With Redis cache:

```
Request 1  → hits PostgreSQL (200ms) → stores result in Redis with 5-minute TTL
Requests 2–10,000 → Redis returns cached result (0.5ms)
→ 99.9% fewer DB queries
```

### Cache TTL Strategy

```
interviews-feed     → 5 minutes    feed changes when interviews are approved
interview-detail    → 30 minutes   individual interviews rarely change
companies           → 1 hour       company list barely changes
shorts-feed         → 5 minutes
search results      → NOT cached   too many query combinations, low reuse
```

### When to Evict

When you approve an interview, the cached feed shows old data. You must clear it:

```kotlin
@CacheEvict(value = ["interviews-feed", "shorts-feed"], allEntries = true)
fun approveInterview(id: UUID): InterviewDetailResponse {
    // After this method completes, Spring deletes all entries from those cache names
    // Next request to /api/interviews will hit PostgreSQL and re-cache
}
```

### Redis Key Design

```
Cache keys (auto-generated by @Cacheable):
  interviews-feed::p0_s20_cnull_dnull_onull_rnull
  interview-detail::550e8400-uuid-here

Read counter keys (manually managed):
  reads:{user-uuid}:2025-01   ← reads this month, key expires in 35 days
  reads:{user-uuid}:2025-02   ← new month = new key = automatic reset

URL dedup keys (crawler project):
  crawled:{sha256-of-url}     ← prevents re-crawling same URL
```

---

## Implementation

### Redis Config in `app` Module

`@EnableCaching` belongs in `app` because it is a cross-cutting concern that needs to be active for all modules.

```kotlin
// app/src/main/kotlin/com/hirestory/app/config/RedisConfig.kt
package com.hirestory.app.config

import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
@EnableCaching   // ← activates @Cacheable, @CacheEvict across ALL modules
class RedisConfig {

    @Bean
    fun redisTemplate(factory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory  = factory
        template.keySerializer      = StringRedisSerializer()
        template.valueSerializer    = GenericJackson2JsonRedisSerializer()
        template.hashKeySerializer  = StringRedisSerializer()
        template.hashValueSerializer = GenericJackson2JsonRedisSerializer()
        template.afterPropertiesSet()
        return template
    }

    @Bean
    fun cacheManager(factory: RedisConnectionFactory): RedisCacheManager {
        val json = GenericJackson2JsonRedisSerializer()

        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(json)
            )
            .disableCachingNullValues()

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withCacheConfiguration(
                "interview-detail",
                defaultConfig.entryTtl(Duration.ofMinutes(30))
            )
            .withCacheConfiguration(
                "companies",
                defaultConfig.entryTtl(Duration.ofHours(1))
            )
            .withCacheConfiguration(
                "company-detail",
                defaultConfig.entryTtl(Duration.ofHours(1))
            )
            .withCacheConfiguration(
                "shorts-feed",
                defaultConfig.entryTtl(Duration.ofMinutes(5))
            )
            .build()
    }
}
```

### RabbitMQ Config in `app` Module

```kotlin
// app/src/main/kotlin/com/hirestory/app/config/RabbitMQConfig.kt
package com.hirestory.app.config

import com.hirestory.core.messaging.MessagingConstants
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    // ── Notification queue ─────────────────────────────────────────────────
    // content module publishes here, notification module consumes here

    @Bean
    fun notificationQueue(): Queue =
        QueueBuilder.durable(MessagingConstants.NOTIFICATION_QUEUE)
            .withArgument("x-dead-letter-exchange",   "")
            .withArgument("x-dead-letter-routing-key", MessagingConstants.NOTIFICATION_DLQ)
            .build()

    @Bean
    fun notificationDeadLetterQueue(): Queue =
        QueueBuilder.durable(MessagingConstants.NOTIFICATION_DLQ).build()

    @Bean
    fun notificationExchange(): DirectExchange =
        DirectExchange(MessagingConstants.NOTIFICATION_EXCHANGE)

    @Bean
    fun notificationBinding(): Binding =
        BindingBuilder
            .bind(notificationQueue())
            .to(notificationExchange())
            .with(MessagingConstants.NOTIFICATION_ROUTING_KEY)

    // ── Shared converter ───────────────────────────────────────────────────

    @Bean
    fun messageConverter(): Jackson2JsonMessageConverter = Jackson2JsonMessageConverter()

    @Bean
    fun rabbitTemplate(factory: ConnectionFactory): RabbitTemplate {
        val template = RabbitTemplate(factory)
        template.messageConverter = messageConverter()
        return template
    }
}
```

### ReadCounterService

```kotlin
// content/src/main/kotlin/com/hirestory/content/service/ReadCounterService.kt
package com.hirestory.content.service

import com.hirestory.content.repository.ReadHistoryRepository
import com.hirestory.core.entity.ReadHistory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class ReadCounterService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val readHistoryRepository: ReadHistoryRepository
) {
    companion object {
        const val FREE_READS_PER_MONTH = 25
        private val MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM")
    }

    /**
     * Returns true  → user can read (read recorded)
     * Returns false → free limit hit (show paywall — caller returns 402)
     */
    fun checkAndRecordRead(userId: UUID, interviewId: UUID, isPremium: Boolean): Boolean {
        // Premium users always can read — still track history
        if (isPremium) {
            saveToHistoryIfNew(userId, interviewId)
            return true
        }

        // Re-reading same interview is always free
        if (readHistoryRepository.existsByUserIdAndInterviewId(userId, interviewId)) {
            return true
        }

        val key          = readKey(userId)
        val currentCount = getCount(key)

        if (currentCount >= FREE_READS_PER_MONTH) {
            return false   // paywall
        }

        // Increment Redis counter + save read history
        redisTemplate.opsForValue().increment(key)
        // 35-day TTL ensures key expires naturally after the month ends
        redisTemplate.expire(key, 35, TimeUnit.DAYS)
        saveToHistoryIfNew(userId, interviewId)

        return true
    }

    fun getMonthlyReadCount(userId: UUID): Int = getCount(readKey(userId))

    fun getRemainingReads(userId: UUID): Int =
        maxOf(0, FREE_READS_PER_MONTH - getMonthlyReadCount(userId))

    // Called when user upgrades to premium — clear their counter
    fun resetMonthlyCount(userId: UUID) {
        redisTemplate.delete(readKey(userId))
    }

    private fun saveToHistoryIfNew(userId: UUID, interviewId: UUID) {
        if (!readHistoryRepository.existsByUserIdAndInterviewId(userId, interviewId)) {
            readHistoryRepository.save(ReadHistory(userId = userId, interviewId = interviewId))
        }
    }

    private fun getCount(key: String): Int {
        val raw = redisTemplate.opsForValue().get(key)
        return when (raw) {
            is Int    -> raw
            is Long   -> raw.toInt()
            is String -> raw.toIntOrNull() ?: 0
            else      -> 0
        }
    }

    private fun readKey(userId: UUID): String =
        "reads:$userId:${LocalDate.now().format(MONTH_FORMAT)}"
}
```

### Update `application.yml` — Add RabbitMQ

```yaml
# app/src/main/resources/application.yml  (add to existing file)
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: hirestory
    password: hirestory123
```

### Install RabbitMQ

**Mac:**

```bash
brew install rabbitmq
brew services start rabbitmq
# Management UI: http://localhost:15672  (guest / guest)
```

**Linux:**

```bash
sudo apt install rabbitmq-server
sudo systemctl start rabbitmq-server
sudo systemctl enable rabbitmq-server
sudo rabbitmq-plugins enable rabbitmq_management
# Management UI: http://localhost:15672
```

**Windows:**

1. Download from https://www.rabbitmq.com/install-windows.html
2. Install Erlang first (required), then RabbitMQ
3. Start from Services or `rabbitmq-service.bat start`
4. Enable management: `rabbitmq-plugins enable rabbitmq_management`

---

## Chapter 7 Checkpoint ✅

- [ ] `app/config/RedisConfig.kt` created with `@EnableCaching`
- [ ] `app/config/RabbitMQConfig.kt` created with notification queue beans
- [ ] RabbitMQ running — `http://localhost:15672` shows management UI
- [ ] `./gradlew :app:bootRun` — no connection errors for Redis or RabbitMQ
- [ ] Call `GET /api/interviews` twice — second call is faster (cache hit)
- [ ] Approve an interview via admin endpoint — cache evicted — next feed call is fresh
- [ ] Read an interview as logged-in user 25 times → 26th returns **402**
- [ ] Re-read the same interview after hitting limit → still **200** (re-reads are free)
- [ ] `GET /api/companies` cached for 1 hour — confirmed in Redis CLI: `redis-cli keys "*companies*"`

---

# Chapter 8 — Web Crawler (Separate Project) {#chapter-8}

## Theory

### Why the Crawler is a Separate Project

The crawler is genuinely different from the consumer backend:

```
hirestory-backend     → serves HTTP requests from users 24/7, deployed on Railway
hirestory-crawler     → runs on YOUR machine or a cron job, NOT deployed publicly
                        makes HTTP requests to external sites (slow, can fail)
                        uses OpenAI API (expensive, don't do this on every deploy)
                        connects to same PostgreSQL and Redis as the backend
```

The crawler posts extracted interviews to `http://localhost:8080/internal/interviews/from-crawl` — one HTTP call to your backend per extracted interview. That is the only connection between the two projects.

### Crawler Architecture

```
@Scheduled every 6h           @Scheduled every 12h
      ↓                               ↓
RedditCrawlerService          GfgCrawlerService
      ↓                               ↓
      └──────────┬────────────────────┘
                 ↓
         CrawlJobService
           ↓         ↓
        Redis      Database
        (dedup)    (crawl_jobs table)
           ↓
        RabbitMQ (crawl.queue)
           ↓
        CrawlJobConsumer
           ↓
        AiExtractionService (OpenAI)
           ↓ high confidence
        POST /internal/interviews/from-crawl (hirestory-backend)
```

### How Jsoup Works

```kotlin
// Fetch and parse any HTML page
val doc = Jsoup.connect("https://www.geeksforgeeks.org/some-article/")
    .userAgent("Mozilla/5.0")
    .timeout(15_000)
    .get()

// CSS selectors — same syntax as frontend CSS
val title     = doc.select("h1.entry-title").first()?.text()
val paragraphs = doc.select("div.entry-content p")
val fullText   = paragraphs.joinToString("\n") { it.text() }
val links      = doc.select("a[href]").map { it.attr("abs:href") }  // abs: = absolute URL
```

### Reddit Public JSON API

Reddit exposes a JSON API without authentication for public subreddits:

```
GET https://www.reddit.com/r/cscareerquestions/search.json
    ?q=interview+experience
    &sort=new
    &limit=25
    &restrict_sr=1    ← only within this subreddit
    &t=week           ← only last week's posts

Required header: User-Agent: YourApp/1.0
```

---

## Implementation

### Create the Crawler Project

Create `hirestory-crawler/` as a **sibling folder** to `hirestory-backend/`:

```
(your workspace)/
├── hirestory-backend/    ← multi-module project (done)
└── hirestory-crawler/    ← new standalone project
    ├── settings.gradle.kts
    ├── build.gradle.kts
    └── src/main/kotlin/com/hirestory/crawler/
```

**`hirestory-crawler/settings.gradle.kts`**

```kotlin
rootProject.name = "hirestory-crawler"
```

**`hirestory-crawler/build.gradle.kts`**

```kotlin
plugins {
    kotlin("jvm")            version "1.9.25"
    kotlin("plugin.spring")  version "1.9.25"
    kotlin("plugin.jpa")     version "1.9.25"
    id("org.springframework.boot")        version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

group   = "com.hirestory"
version = "0.0.1-SNAPSHOT"

java.sourceCompatibility = JavaVersion.VERSION_21

repositories { mavenCentral() }

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.4")
        mavenBom("org.springframework.ai:spring-ai-bom:1.0.0")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget         = "21"
    }
}
```

### Crawler File Layout

```
hirestory-crawler/src/main/kotlin/com/hirestory/crawler/
├── CrawlerApplication.kt
├── config/
│   └── RabbitMQConfig.kt
├── entity/
│   └── CrawlJob.kt            ← maps to crawl_jobs table (in shared DB)
├── repository/
│   └── CrawlJobRepository.kt
├── service/
│   ├── CrawlJobService.kt     ← deduplication + queue submission
│   ├── AiExtractionService.kt ← OpenAI extraction
│   ├── CrawlJobConsumer.kt    ← RabbitMQ consumer → calls AI → posts to backend
│   ├── InterviewPublishClient.kt ← POST to /internal/interviews/from-crawl
│   ├── reddit/
│   │   └── RedditCrawlerService.kt
│   └── gfg/
│       └── GfgCrawlerService.kt
├── controller/
│   └── CrawlerAdminController.kt   ← manual triggers for testing
└── dto/
    └── ExtractionDtos.kt
```

### `application.yml` for Crawler

```yaml
# hirestory-crawler/src/main/resources/application.yml
server:
  port: 8083

spring:
  application:
    name: hirestory-crawler

  datasource:
    url: jdbc:postgresql://localhost:5432/hirestory_dev
    username: hirestory
    password: hirestory123

  jpa:
    hibernate:
      ddl-auto: validate    # tables already created by hirestory-backend migrations
    show-sql: false

  flyway:
    enabled: false          # NEVER run migrations from crawler — backend owns them

  data:
    redis:
      host: localhost
      port: 6379

  rabbitmq:
    host: localhost
    port: 5672
    username: hirestory
    password: hirestory123

  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.1
          max-tokens: 1500

crawler:
  rate-limit-ms: 2000
  max-content-length: 8000

services:
  backend:
    url: http://localhost:8080    # hirestory-backend
```

### Application Class

```kotlin
// hirestory-crawler/src/main/kotlin/com/hirestory/crawler/CrawlerApplication.kt
package com.hirestory.crawler

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class CrawlerApplication

fun main(args: Array<String>) {
    runApplication<CrawlerApplication>(*args)
}
```

### CrawlJob Entity

```kotlin
// hirestory-crawler/src/main/kotlin/com/hirestory/crawler/entity/CrawlJob.kt
package com.hirestory.crawler.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "crawl_jobs")
data class CrawlJob(
    @Id
    @Column(columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "source_url", nullable = false, unique = true)
    val sourceUrl: String,

    @Column(name = "raw_text", columnDefinition = "TEXT")
    var rawText: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "crawl_status_enum")
    var status: CrawlStatus = CrawlStatus.PENDING,

    @Column(name = "confidence_score")
    var confidenceScore: Int? = null,

    @Column(name = "interview_id", columnDefinition = "UUID")
    var interviewId: UUID? = null,

    @Column(name = "error_message")
    var errorMessage: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "processed_at")
    var processedAt: LocalDateTime? = null
)

enum class CrawlStatus { PENDING, PROCESSING, DONE, FAILED }
```

### CrawlJobRepository

```kotlin
// hirestory-crawler/src/main/kotlin/com/hirestory/crawler/repository/CrawlJobRepository.kt
package com.hirestory.crawler.repository

import com.hirestory.crawler.entity.CrawlJob
import com.hirestory.crawler.entity.CrawlStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CrawlJobRepository : JpaRepository<CrawlJob, UUID> {
    fun existsBySourceUrl(url: String): Boolean
    fun countByStatus(status: CrawlStatus): Long
}
```

### RabbitMQ Config (internal to crawler)

The crawler uses its own `crawl.queue` for its internal AI processing pipeline. This is separate from the backend's `notification.queue`.

```kotlin
// hirestory-crawler/src/main/kotlin/com/hirestory/crawler/config/RabbitMQConfig.kt
package com.hirestory.crawler.config

import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    companion object {
        const val CRAWL_QUEUE       = "crawl.queue"
        const val CRAWL_DLQ         = "crawl.dlq"
        const val CRAWL_EXCHANGE    = "crawl.exchange"
        const val CRAWL_ROUTING_KEY = "crawl.job"
    }

    @Bean
    fun crawlQueue(): Queue =
        QueueBuilder.durable(CRAWL_QUEUE)
            .withArgument("x-dead-letter-exchange",    "")
            .withArgument("x-dead-letter-routing-key", CRAWL_DLQ)
            .build()

    @Bean
    fun crawlDeadLetterQueue(): Queue = QueueBuilder.durable(CRAWL_DLQ).build()

    @Bean
    fun crawlExchange(): DirectExchange = DirectExchange(CRAWL_EXCHANGE)

    @Bean
    fun crawlBinding(): Binding =
        BindingBuilder.bind(crawlQueue()).to(crawlExchange()).with(CRAWL_ROUTING_KEY)

    @Bean
    fun messageConverter(): Jackson2JsonMessageConverter = Jackson2JsonMessageConverter()

    @Bean
    fun rabbitTemplate(factory: ConnectionFactory): RabbitTemplate {
        val template = RabbitTemplate(factory)
        template.messageConverter = messageConverter()
        return template
    }
}
```

### CrawlJobService (deduplication)

```kotlin
// hirestory-crawler/src/main/kotlin/com/hirestory/crawler/service/CrawlJobService.kt
package com.hirestory.crawler.service

import com.hirestory.crawler.config.RabbitMQConfig
import com.hirestory.crawler.entity.CrawlJob
import com.hirestory.crawler.entity.CrawlStatus
import com.hirestory.crawler.repository.CrawlJobRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@Service
class CrawlJobService(
    private val crawlJobRepository: CrawlJobRepository,
    private val rabbitTemplate: RabbitTemplate,
    private val redisTemplate: RedisTemplate<String, Any>
) {
    private val log = LoggerFactory.getLogger(CrawlJobService::class.java)

    @Value("\${crawler.max-content-length:8000}")
    private var maxContentLength: Int = 8000

    /**
     * Returns true  → new URL, job queued for AI processing
     * Returns false → already seen, skipped
     */
    fun submitJob(url: String, rawText: String?): Boolean {
        if (url.isBlank()) return false

        val urlHash  = sha256(url)
        val redisKey = "crawled:$urlHash"

        // Layer 1: Redis check (microseconds)
        if (redisTemplate.hasKey(redisKey) == true) {
            log.debug("Skip (Redis cache): $url")
            return false
        }

        // Layer 2: DB check (backup if Redis was restarted)
        if (crawlJobRepository.existsBySourceUrl(url)) {
            redisTemplate.opsForValue().set(redisKey, "1", 30, TimeUnit.DAYS)
            return false
        }

        val trimmedText = rawText?.take(maxContentLength)
        if ((trimmedText?.length ?: 0) < 50) {
            log.debug("Skip (content too short): $url")
            return false
        }

        val job = crawlJobRepository.save(CrawlJob(sourceUrl = url, rawText = trimmedText))
        redisTemplate.opsForValue().set(redisKey, "1", 30, TimeUnit.DAYS)

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.CRAWL_EXCHANGE,
            RabbitMQConfig.CRAWL_ROUTING_KEY,
            job.id.toString()
        )

        log.info("Queued job ${job.id}: $url")
        return true
    }

    fun getStats() = CrawlStats(
        pending = crawlJobRepository.countByStatus(CrawlStatus.PENDING),
        done    = crawlJobRepository.countByStatus(CrawlStatus.DONE),
        failed  = crawlJobRepository.countByStatus(CrawlStatus.FAILED)
    )

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    data class CrawlStats(val pending: Long, val done: Long, val failed: Long)
}
```

### Reddit Crawler

```kotlin
// hirestory-crawler/src/main/kotlin/com/hirestory/crawler/service/reddit/RedditCrawlerService.kt
package com.hirestory.crawler.service.reddit

import com.fasterxml.jackson.databind.ObjectMapper
import com.hirestory.crawler.service.CrawlJobService
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class RedditCrawlerService(
    private val crawlJobService: CrawlJobService,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(RedditCrawlerService::class.java)

    @Scheduled(cron = "0 0 */6 * * *")   // every 6 hours
    fun crawl() {
        log.info("Reddit crawl starting")

        val subreddits = listOf("cscareerquestions", "developersIndia", "india")
        val queries    = listOf(
            "interview experience",
            "got offer",
            "got rejected",
            "interview questions"
        )

        var totalNew = 0
        subreddits.forEach { sub ->
            queries.forEach { query ->
                try {
                    totalNew += crawlSubreddit(sub, query)
                    Thread.sleep(3_000)
                } catch (e: Exception) {
                    log.error("Reddit r/$sub '$query' failed: ${e.message}")
                }
            }
        }

        log.info("Reddit crawl done. New jobs: $totalNew")
    }

    private fun crawlSubreddit(subreddit: String, query: String): Int {
        val url = "https://www.reddit.com/r/$subreddit/search.json" +
                  "?q=${query.replace(" ", "+")}&sort=new&limit=25&restrict_sr=1&t=week"

        val json = Jsoup.connect(url)
            .userAgent("HireStoryBot/1.0")
            .header("Accept", "application/json")
            .ignoreContentType(true)
            .timeout(20_000)
            .get().body().text()

        val posts = objectMapper.readTree(json).path("data").path("children")

        var newCount = 0

        posts.forEach { post ->
            val data      = post.path("data")
            val title     = data.path("title").asText("")
            val selftext  = data.path("selftext").asText("")
            val permalink = data.path("permalink").asText("")
            val isSelf    = data.path("is_self").asBoolean(false)

            if (!isSelf || selftext.length < 50 || permalink.isBlank()) return@forEach

            val isNew = crawlJobService.submitJob(
                url     = "https://reddit.com$permalink",
                rawText = "$title\n\n$selftext"
            )
            if (isNew) newCount++

            Thread.sleep(500)
        }

        return newCount
    }
}
```

### GFG Crawler

```kotlin
// hirestory-crawler/src/main/kotlin/com/hirestory/crawler/service/gfg/GfgCrawlerService.kt
package com.hirestory.crawler.service.gfg

import com.hirestory.crawler.service.CrawlJobService
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class GfgCrawlerService(private val crawlJobService: CrawlJobService) {
    private val log = LoggerFactory.getLogger(GfgCrawlerService::class.java)

    @Scheduled(cron = "0 30 */12 * * *")   // every 12 hours, offset 30min from Reddit
    fun crawl() {
        log.info("GFG crawl starting")
        var totalNew = 0
        var page     = 1

        while (page <= 3) {
            try {
                totalNew += crawlPage(page)
                page++
                Thread.sleep(4_000)
            } catch (e: HttpStatusException) {
                if (e.statusCode == 404) break
                log.error("GFG page $page failed: ${e.message}")
                break
            } catch (e: Exception) {
                log.error("GFG page $page error: ${e.message}")
                break
            }
        }

        log.info("GFG crawl done. New jobs: $totalNew")
    }

    private fun crawlPage(pageNum: Int): Int {
        val base = "https://www.geeksforgeeks.org/category/interview-experiences/"
        val url  = if (pageNum == 1) base else "${base}page/$pageNum/"

        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (compatible; HireStoryBot/1.0)")
            .timeout(20_000).get()

        val links = doc.select("article h2 a, h2.entry-title a")
            .map { it.attr("abs:href") }
            .filter { it.contains("geeksforgeeks.org") }
            .distinct()

        if (links.isEmpty()) return 0

        var newCount = 0
        links.forEach { articleUrl ->
            try {
                val rawText = fetchArticle(articleUrl)
                if (rawText.length > 100) {
                    if (crawlJobService.submitJob(articleUrl, rawText)) newCount++
                }
            } catch (e: Exception) {
                log.debug("Failed GFG article $articleUrl: ${e.message}")
            }
            Thread.sleep(2_000)
        }

        return newCount
    }

    private fun fetchArticle(url: String): String {
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (compatible; HireStoryBot/1.0)")
            .timeout(20_000).get()

        val title   = doc.select("h1.entry-title, h1").firstOrNull()?.text() ?: ""
        val content = doc.select("div.entry-content, div.article-body").firstOrNull()
            ?.also { it.select("div.code-block, ins, .ads").remove() }
            ?.text() ?: ""

        return "$title\n\n$content"
    }
}
```

### Admin Controller (manual trigger for testing)

```kotlin
// hirestory-crawler/src/main/kotlin/com/hirestory/crawler/controller/CrawlerAdminController.kt
package com.hirestory.crawler.controller

import com.hirestory.crawler.service.CrawlJobService
import com.hirestory.crawler.service.gfg.GfgCrawlerService
import com.hirestory.crawler.service.reddit.RedditCrawlerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/crawler")
class CrawlerAdminController(
    private val crawlJobService: CrawlJobService,
    private val redditCrawler: RedditCrawlerService,
    private val gfgCrawler: GfgCrawlerService
) {

    @PostMapping("/trigger/reddit")
    fun triggerReddit(): ResponseEntity<Map<String, String>> {
        redditCrawler.crawl()
        return ResponseEntity.ok(mapOf("status" to "triggered"))
    }

    @PostMapping("/trigger/gfg")
    fun triggerGfg(): ResponseEntity<Map<String, String>> {
        gfgCrawler.crawl()
        return ResponseEntity.ok(mapOf("status" to "triggered"))
    }

    @GetMapping("/stats")
    fun getStats(): ResponseEntity<CrawlJobService.CrawlStats> =
        ResponseEntity.ok(crawlJobService.getStats())
}
```

---

## Chapter 8 Checkpoint ✅

- [ ] `hirestory-crawler/` folder created alongside `hirestory-backend/`
- [ ] Crawler `build.gradle.kts` resolves all dependencies
- [ ] `CrawlJob` entity maps to `crawl_jobs` table — `./gradlew :bootRun` starts crawler without errors
- [ ] RabbitMQ running — `http://localhost:15672` shows `crawl.queue` after first run
- [ ] `POST http://localhost:8083/admin/crawler/trigger/reddit` → new rows in `crawl_jobs` table
- [ ] Trigger again → same URLs skipped (Redis dedup: `redis-cli keys "crawled:*"`)
- [ ] RabbitMQ UI shows messages queued in `crawl.queue`

---

# Chapter 9 — AI Extraction {#chapter-9}

## Theory

### Spring AI

Spring AI is the official Spring library for AI integration. It works with OpenAI, Anthropic, Gemini, and others through a unified API so you can switch providers without changing code.

```kotlin
// build.gradle.kts — already added in Chapter 8
implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter")

// application.yml — already added in Chapter 8
spring.ai.openai.api-key: ${OPENAI_API_KEY}
spring.ai.openai.chat.options.model: gpt-4o-mini
```

### Structured Output

You want a Kotlin object back, not a text paragraph. Spring AI's `.entity()` method sends your class structure to the model and auto-deserializes the JSON response:

```kotlin
val result: ExtractedInterview? = chatClient
    .prompt()
    .system(systemPrompt)
    .user("Extract from: $rawText")
    .call()
    .entity(ExtractedInterview::class.java)   // ← magic: JSON → Kotlin object
```

### Confidence Score — Why It Matters

Not everything scraped from Reddit is a real interview experience. The AI scores each post 0–100:

```
90–100  Real first-hand experience with company + role + round details → auto-publish
70–89   Likely real but missing some details (no rounds, no company) → auto-publish
50–69   Could be real but vague → save as PENDING for admin review
20–49   Mentions interviews but is advice or asking for help → discard
0–19    Not interview-related at all → discard
```

### Cost

gpt-4o-mini pricing: ~$0.15/million input tokens, ~$0.60/million output tokens.

Each extraction: ~800 tokens in + ~300 tokens out = **$0.0003 per post** (0.03 cents).

First 1,000 crawled interviews: **~$0.30 total**.

---

## Implementation

### ExtractionDtos

```kotlin
// hirestory-crawler/src/main/kotlin/com/hirestory/crawler/dto/ExtractionDtos.kt
package com.hirestory.crawler.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExtractedInterview(
    val companyName: String?         = null,
    val role: String?                = null,
    val location: String?            = null,
    val experienceYears: Int?        = null,
    val difficulty: String?          = null,   // EASY, MEDIUM, HARD
    val outcome: String?             = null,   // OFFER, REJECTED, GHOSTED
    val roundsCount: Int?            = null,
    val rounds: List<ExtractedRound>? = null,
    val tags: List<String>?          = null,
    val salaryLpa: Double?           = null,
    val confidenceScore: Int         = 0,
    val confidenceReason: String     = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExtractedRound(
    val roundNumber: Int   = 1,
    val title: String?     = null,
    val questions: String? = null,
    val difficulty: String? = null
)
```

### AiExtractionService

```kotlin
// hirestory-crawler/src/main/kotlin/com/hirestory/crawler/service/AiExtractionService.kt
package com.hirestory.crawler.service

import com.hirestory.crawler.dto.ExtractedInterview
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class AiExtractionService(chatClientBuilder: ChatClient.Builder) {

    private val log        = LoggerFactory.getLogger(AiExtractionService::class.java)
    private val chatClient = chatClientBuilder.build()   // thread-safe, build once

    fun extract(rawText: String): ExtractedInterview? {
        if (rawText.isBlank()) return null

        return try {
            chatClient
                .prompt()
                .system(SYSTEM_PROMPT)
                .user("Extract interview data from the following text:\n\n${rawText.take(4000)}")
                .call()
                .entity(ExtractedInterview::class.java)
        } catch (e: Exception) {
            log.error("AI extraction failed: ${e.message}")
            null
        }
    }

    companion object {
        private val SYSTEM_PROMPT = """
            You are an expert at extracting structured data from tech interview experience posts.
            
            Your job: Read the post and extract interview experience details.
            Return ONLY a valid JSON object. No extra text, no markdown, no explanation.
            
            JSON SCHEMA (return exactly this structure):
            {
              "companyName": "string or null",
              "role": "string or null — e.g. Software Engineer, SDE-2, Backend Developer",
              "location": "string or null — e.g. Bangalore, Hyderabad, Remote",
              "experienceYears": number or null,
              "difficulty": "EASY or MEDIUM or HARD or null",
              "outcome": "OFFER or REJECTED or GHOSTED or null",
              "roundsCount": number or null,
              "rounds": [
                {
                  "roundNumber": 1,
                  "title": "Online Assessment or Technical Round 1 etc",
                  "questions": "describe what was asked in this round",
                  "difficulty": "Easy or Medium or Hard"
                }
              ],
              "tags": ["DSA", "System Design", "HR", "Behavioral", "SQL", "React",
                       "Spring Boot", "React Native", "Android", "iOS", "DevOps",
                       "Machine Learning", "Low Level Design"],
              "salaryLpa": number or null,
              "confidenceScore": 0-100,
              "confidenceReason": "one sentence explanation"
            }
            
            CONFIDENCE SCORE RULES:
            90-100: Real first-hand experience with company + role + detailed rounds
            70-89:  Likely real but missing some details
            50-69:  Possible experience but vague or secondhand
            20-49:  Advice/tips/questions — not sharing an experience
            0-19:   Not interview-related at all
            
            STRICT RULES:
            - Return null for any field you cannot confidently extract
            - salaryLpa must be in Indian Lakhs Per Annum (LPA)
            - Use only the listed tags — no others
            - rounds array only if specific round details are mentioned
            - Advice posts: confidenceScore must be under 30
        """.trimIndent()
    }
}
```

### InterviewPublishClient

```kotlin
// hirestory-crawler/src/main/kotlin/com/hirestory/crawler/service/InterviewPublishClient.kt
package com.hirestory.crawler.service

import com.hirestory.crawler.dto.ExtractedInterview
import com.hirestory.crawler.entity.CrawlJob
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.util.UUID

@Service
class InterviewPublishClient(private val restTemplate: RestTemplate) {

    private val log = LoggerFactory.getLogger(InterviewPublishClient::class.java)

    @Value("\${services.backend.url}")
    private lateinit var backendUrl: String

    fun publishInterview(job: CrawlJob, extracted: ExtractedInterview): UUID =
        post(buildRequest(job, extracted, autoPublish = true))

    fun savePendingInterview(job: CrawlJob, extracted: ExtractedInterview): UUID =
        post(buildRequest(job, extracted, autoPublish = false))

    private fun post(request: Map<String, Any?>): UUID {
        val response = restTemplate.postForObject(
            "$backendUrl/internal/interviews/from-crawl",
            request,
            Map::class.java
        ) ?: throw RuntimeException("Backend /internal/interviews/from-crawl returned null")

        return UUID.fromString(response["id"] as String)
    }

    private fun buildRequest(
        job: CrawlJob,
        extracted: ExtractedInterview,
        autoPublish: Boolean
    ): Map<String, Any?> = mapOf(
        "companyName"     to (extracted.companyName ?: "Unknown"),
        "title"           to buildTitle(extracted),
        "content"         to (job.rawText ?: ""),
        "role"            to extracted.role,
        "location"        to extracted.location,
        "experienceYears" to extracted.experienceYears,
        "difficulty"      to (extracted.difficulty ?: "MEDIUM"),
        "outcome"         to (extracted.outcome ?: "OFFER"),
        "roundsCount"     to (extracted.roundsCount ?: extracted.rounds?.size ?: 1),
        "rounds"          to (extracted.rounds?.map { r -> mapOf(
            "title"      to (r.title ?: "Round ${r.roundNumber}"),
            "questions"  to r.questions,
            "difficulty" to r.difficulty,
            "notes"      to null
        )} ?: emptyList<Any>()),
        "tagNames"        to (extracted.tags ?: emptyList<String>()),
        "salaryLpa"       to extracted.salaryLpa?.let { BigDecimal.valueOf(it) },
        "sourceUrl"       to job.sourceUrl,
        "confidenceScore" to extracted.confidenceScore,
        "autoPublish"     to autoPublish
    )

    private fun buildTitle(e: ExtractedInterview): String {
        val company = e.companyName ?: "Tech Company"
        val role    = e.role ?: "Software Engineer"
        val outcome = when (e.outcome) {
            "OFFER"    -> "Got Offer"
            "REJECTED" -> "Rejected"
            "GHOSTED"  -> "Ghosted"
            else       -> "Interview Experience"
        }
        return "$company $role — $outcome"
    }
}
```

### Add RestTemplate Bean to Crawler

```kotlin
// hirestory-crawler/src/main/kotlin/com/hirestory/crawler/config/WebConfig.kt
package com.hirestory.crawler.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class WebConfig {
    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()
}
```

### CrawlJobConsumer

```kotlin
// hirestory-crawler/src/main/kotlin/com/hirestory/crawler/service/CrawlJobConsumer.kt
package com.hirestory.crawler.service

import com.hirestory.crawler.config.RabbitMQConfig
import com.hirestory.crawler.entity.CrawlStatus
import com.hirestory.crawler.repository.CrawlJobRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class CrawlJobConsumer(
    private val crawlJobRepository: CrawlJobRepository,
    private val aiExtractionService: AiExtractionService,
    private val interviewPublishClient: InterviewPublishClient
) {
    private val log = LoggerFactory.getLogger(CrawlJobConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.CRAWL_QUEUE])
    @Transactional
    fun consume(jobIdStr: String) {
        val jobId = runCatching { UUID.fromString(jobIdStr) }.getOrElse {
            log.error("Invalid job ID: $jobIdStr")
            return
        }

        val job = crawlJobRepository.findById(jobId).orElse(null) ?: run {
            log.warn("Job not found: $jobId")
            return
        }

        if (job.status != CrawlStatus.PENDING) {
            log.debug("Job $jobId already ${ job.status}, skipping")
            return
        }

        job.status = CrawlStatus.PROCESSING
        crawlJobRepository.save(job)

        try {
            val extracted = aiExtractionService.extract(job.rawText ?: "")

            if (extracted == null) {
                job.status       = CrawlStatus.FAILED
                job.errorMessage = "AI extraction returned null"
                job.processedAt  = LocalDateTime.now()
                crawlJobRepository.save(job)
                return
            }

            job.confidenceScore = extracted.confidenceScore
            log.info("Score ${extracted.confidenceScore} for: ${job.sourceUrl}")

            when {
                extracted.confidenceScore >= 70 -> {
                    val id = interviewPublishClient.publishInterview(job, extracted)
                    job.interviewId = id
                    job.status      = CrawlStatus.DONE
                    log.info("Published: $id")
                }
                extracted.confidenceScore in 50..69 -> {
                    val id = interviewPublishClient.savePendingInterview(job, extracted)
                    job.interviewId = id
                    job.status      = CrawlStatus.DONE
                    log.info("Saved as PENDING (score=${extracted.confidenceScore}): $id")
                }
                else -> {
                    job.status       = CrawlStatus.FAILED
                    job.errorMessage = "Low confidence: ${extracted.confidenceScore} — ${extracted.confidenceReason}"
                    log.info("Discarded (score=${extracted.confidenceScore}): ${job.sourceUrl}")
                }
            }

        } catch (e: Exception) {
            log.error("Failed job ${job.id}: ${e.message}", e)
            job.status       = CrawlStatus.FAILED
            job.errorMessage = e.message?.take(500)
        }

        job.processedAt = LocalDateTime.now()
        crawlJobRepository.save(job)
    }
}
```

---

## Chapter 9 Checkpoint ✅

- [ ] `OPENAI_API_KEY` environment variable set (IntelliJ: Run → Edit Configurations → Environment Variables)
- [ ] Crawler starts — Spring AI `ChatClient` bean wires without errors
- [ ] Trigger Reddit crawl: `POST http://localhost:8083/admin/crawler/trigger/reddit`
- [ ] Watch crawler logs — AI extraction scores printed per URL
- [ ] Interviews with score ≥ 70 appear in `interviews` table as PUBLISHED
- [ ] Interviews with score 50–69 appear as PENDING
- [ ] Low confidence entries show FAILED in `crawl_jobs` with reason
- [ ] `GET http://localhost:8080/api/interviews` → crawled interviews appear in feed

---

# Chapter 10 — Push Notifications {#chapter-10}

## Theory

### Notification Flow in the Monolith

Since everything runs in the same JVM, we still use RabbitMQ for notifications — not because of network separation but because it keeps the response fast and the delivery reliable:

```
User submits comment (POST /api/comments)
          ↓
CommentService saves comment (fast DB write)
          ↓
NotificationEventPublisher.publish(event) → RabbitMQ (2ms, non-blocking)
          ↓
API responds to user immediately (no waiting for FCM)
          ↓ (async, separate thread)
NotificationConsumer picks up event from queue
          ↓
Saves to notifications table (in-app list)
          ↓
Fetches FCM tokens for recipient
          ↓
Firebase Admin SDK → FCM → user's Android/iOS device
```

If FCM is slow or down, the API never feels it. If the consumer crashes, the message stays in RabbitMQ and is retried.

### Deep Links

When the user taps a push notification, the app opens to the right screen:

```
hirestory://interview/{uuid}             → interview detail screen
hirestory://interview/{uuid}?comments=1  → interview detail, scrolled to comments
hirestory://profile                      → profile screen
```

Your KMP app registers a URL scheme handler for `hirestory://` to handle these.

---

## Implementation

### Update `notification/build.gradle.kts`

```kotlin
// notification/build.gradle.kts
plugins {
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":user"))      // to resolve firebaseUid → userId
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("com.google.firebase:firebase-admin:9.2.0")
}
```

### Notification Module File Layout

```
notification/src/main/kotlin/com/hirestory/notification/
├── repository/
│   ├── NotificationRepository.kt
│   └── FcmTokenRepository.kt
├── service/
│   ├── NotificationService.kt
│   └── NotificationConsumer.kt
├── controller/
│   └── NotificationController.kt
├── config/
│   └── FirebaseAdminConfig.kt
└── dto/
    └── NotificationDtos.kt
```

### Firebase Admin Config

```kotlin
// notification/src/main/kotlin/com/hirestory/notification/config/FirebaseAdminConfig.kt
package com.hirestory.notification.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.ByteArrayInputStream

@Configuration
class FirebaseAdminConfig {

    /**
     * Initialize Firebase Admin SDK for sending push notifications.
     *
     * FIREBASE_SERVICE_ACCOUNT_KEY env var must contain the full JSON content
     * of your Firebase service account key file.
     *
     * How to get it:
     * Firebase Console → Project Settings → Service Accounts → Generate New Private Key
     * Download the JSON → set entire JSON as the env var value
     */
    @Bean
    fun firebaseApp(): FirebaseApp {
        val serviceAccountJson = System.getenv("FIREBASE_SERVICE_ACCOUNT_KEY")
            ?: throw IllegalStateException(
                "FIREBASE_SERVICE_ACCOUNT_KEY environment variable not set. " +
                "Download from Firebase Console → Project Settings → Service Accounts."
            )

        val options = FirebaseOptions.builder()
            .setCredentials(
                GoogleCredentials.fromStream(
                    ByteArrayInputStream(serviceAccountJson.toByteArray())
                )
            )
            .build()

        return if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        } else {
            FirebaseApp.getInstance()
        }
    }
}
```

### Repositories

```kotlin
// notification/src/main/kotlin/com/hirestory/notification/repository/NotificationRepository.kt
package com.hirestory.notification.repository

import com.hirestory.core.entity.Notification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NotificationRepository : JpaRepository<Notification, UUID> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID, pageable: Pageable): Page<Notification>
    fun findByIdAndUserId(id: UUID, userId: UUID): Notification?
    fun countByUserIdAndIsReadFalse(userId: UUID): Long

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    fun markAllReadForUser(@Param("userId") userId: UUID): Int
}
```

```kotlin
// notification/src/main/kotlin/com/hirestory/notification/repository/FcmTokenRepository.kt
package com.hirestory.notification.repository

import com.hirestory.core.entity.FcmToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FcmTokenRepository : JpaRepository<FcmToken, UUID> {
    fun findByUserId(userId: UUID): List<FcmToken>
    fun findByToken(token: String): FcmToken?
    fun deleteByTokenIn(tokens: List<String>)
}
```

### DTOs

```kotlin
// notification/src/main/kotlin/com/hirestory/notification/dto/NotificationDtos.kt
package com.hirestory.notification.dto

import java.time.LocalDateTime
import java.util.UUID

data class NotificationResponse(
    val id: UUID,
    val type: String,
    val title: String,
    val body: String,
    val deepLink: String?,
    val isRead: Boolean,
    val createdAt: LocalDateTime
)

data class RegisterTokenRequest(
    val token: String,
    val platform: String    // "ANDROID" or "IOS"
)

data class UnreadCountResponse(val count: Long)
```

### NotificationService

```kotlin
// notification/src/main/kotlin/com/hirestory/notification/service/NotificationService.kt
package com.hirestory.notification.service

import com.hirestory.core.entity.FcmToken
import com.hirestory.core.enums.Platform
import com.hirestory.core.exception.ResourceNotFoundException
import com.hirestory.notification.dto.NotificationResponse
import com.hirestory.notification.repository.FcmTokenRepository
import com.hirestory.notification.repository.NotificationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val fcmTokenRepository: FcmTokenRepository
) {

    @Transactional
    fun registerToken(userId: UUID, token: String, platformStr: String) {
        val platform = runCatching { Platform.valueOf(platformStr.uppercase()) }
            .getOrElse { Platform.ANDROID }

        // If the token already belongs to this user, skip (idempotent)
        fcmTokenRepository.findByToken(token)?.let { existing ->
            if (existing.userId == userId) return
            // Token moved to different user (re-login on same device) — reassign
            fcmTokenRepository.delete(existing)
        }

        fcmTokenRepository.save(FcmToken(userId = userId, token = token, platform = platform))
    }

    fun getNotifications(userId: UUID, page: Int): Page<NotificationResponse> {
        val pageable = PageRequest.of(page, 20, Sort.by("createdAt").descending())
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map { n -> NotificationResponse(n.id, n.type.name, n.title, n.body, n.deepLink, n.isRead, n.createdAt) }
    }

    fun getUnreadCount(userId: UUID): Long =
        notificationRepository.countByUserIdAndIsReadFalse(userId)

    @Transactional
    fun markAsRead(notificationId: UUID, userId: UUID) {
        val notification = notificationRepository.findByIdAndUserId(notificationId, userId)
            ?: throw ResourceNotFoundException("Notification not found")
        notification.isRead = true
        notificationRepository.save(notification)
    }

    @Transactional
    fun markAllAsRead(userId: UUID) {
        notificationRepository.markAllReadForUser(userId)
    }

    @Transactional
    fun deleteExpiredTokens(tokens: List<String>) {
        if (tokens.isNotEmpty()) {
            fcmTokenRepository.deleteByTokenIn(tokens)
        }
    }
}
```

### NotificationConsumer

```kotlin
// notification/src/main/kotlin/com/hirestory/notification/service/NotificationConsumer.kt
package com.hirestory.notification.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.messaging.*
import com.hirestory.core.entity.Notification
import com.hirestory.core.enums.NotificationType
import com.hirestory.core.messaging.MessagingConstants
import com.hirestory.core.messaging.NotificationEvent
import com.hirestory.notification.repository.FcmTokenRepository
import com.hirestory.notification.repository.NotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class NotificationConsumer(
    private val notificationRepository: NotificationRepository,
    private val fcmTokenRepository: FcmTokenRepository,
    private val notificationService: NotificationService,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(NotificationConsumer::class.java)

    @RabbitListener(queues = [MessagingConstants.NOTIFICATION_QUEUE])
    @Transactional
    fun consume(message: String) {
        try {
            val event = objectMapper.readValue(message, NotificationEvent::class.java)
            processEvent(event)
        } catch (e: Exception) {
            log.error("Failed to process notification: ${e.message}", e)
            throw e   // re-throw → message goes to DLQ after max retries
        }
    }

    private fun processEvent(event: NotificationEvent) {
        val userId = UUID.fromString(event.recipientUserId)

        // 1. Persist to notifications table — shows in in-app notification list
        notificationRepository.save(
            Notification(
                userId   = userId,
                type     = NotificationType.valueOf(event.type),
                title    = event.title,
                body     = event.body,
                deepLink = event.deepLink
            )
        )

        // 2. Push to all user's registered devices via FCM
        val tokens = fcmTokenRepository.findByUserId(userId)

        if (tokens.isEmpty()) {
            log.debug("No FCM tokens for user $userId — notification saved, no push sent")
            return
        }

        val expiredTokens = mutableListOf<String>()

        tokens.forEach { fcmToken ->
            try {
                val fcmMessage = Message.builder()
                    .setToken(fcmToken.token)
                    .setNotification(
                        com.google.firebase.messaging.Notification.builder()
                            .setTitle(event.title)
                            .setBody(event.body)
                            .build()
                    )
                    .putAllData(
                        event.data + mapOf(
                            "deepLink" to (event.deepLink ?: ""),
                            "type"     to event.type
                        )
                    )
                    .setAndroidConfig(
                        AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build()
                    )
                    .build()

                FirebaseMessaging.getInstance().send(fcmMessage)
                log.debug("FCM sent to ${fcmToken.platform} device for user $userId")

            } catch (e: FirebaseMessagingException) {
                when (e.messagingErrorCode) {
                    MessagingErrorCode.UNREGISTERED,
                    MessagingErrorCode.INVALID_ARGUMENT -> {
                        // Token is stale — mark for deletion
                        expiredTokens.add(fcmToken.token)
                        log.info("Marking expired FCM token for user $userId")
                    }
                    else -> log.error("FCM send failed: ${e.message}")
                }
            }
        }

        // Clean up stale tokens so we don't keep sending to dead devices
        if (expiredTokens.isNotEmpty()) {
            notificationService.deleteExpiredTokens(expiredTokens)
            log.info("Removed ${expiredTokens.size} expired tokens for user $userId")
        }
    }
}
```

### NotificationController

```kotlin
// notification/src/main/kotlin/com/hirestory/notification/controller/NotificationController.kt
package com.hirestory.notification.controller

import com.hirestory.notification.dto.*
import com.hirestory.notification.service.NotificationService
import com.hirestory.user.service.UserService          // ← direct bean injection
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService,
    private val userService: UserService                // ← no HTTP, same JVM
) {

    /**
     * Android/iOS app calls this on startup after login.
     * Registers or refreshes the FCM token for this device.
     */
    @PostMapping("/token")
    fun registerToken(
        @RequestBody request: RegisterTokenRequest,
        @AuthenticationPrincipal firebaseUid: String
    ): ResponseEntity<Unit> {
        val userId = userService.resolveUserId(firebaseUid)
        notificationService.registerToken(userId, request.token, request.platform)
        return ResponseEntity.ok().build()
    }

    /**
     * In-app notification list — paginated, newest first.
     */
    @GetMapping
    fun getNotifications(
        @RequestParam(defaultValue = "0") page: Int,
        @AuthenticationPrincipal firebaseUid: String
    ): ResponseEntity<Page<NotificationResponse>> {
        val userId = userService.resolveUserId(firebaseUid)
        return ResponseEntity.ok(notificationService.getNotifications(userId, page))
    }

    /**
     * Unread badge count — call on app launch to show the red dot.
     */
    @GetMapping("/unread-count")
    fun getUnreadCount(
        @AuthenticationPrincipal firebaseUid: String
    ): ResponseEntity<UnreadCountResponse> {
        val userId = userService.resolveUserId(firebaseUid)
        return ResponseEntity.ok(UnreadCountResponse(notificationService.getUnreadCount(userId)))
    }

    /**
     * Called when user taps a specific notification.
     */
    @PutMapping("/{id}/read")
    fun markAsRead(
        @PathVariable id: UUID,
        @AuthenticationPrincipal firebaseUid: String
    ): ResponseEntity<Unit> {
        val userId = userService.resolveUserId(firebaseUid)
        notificationService.markAsRead(id, userId)
        return ResponseEntity.ok().build()
    }

    /**
     * Called when user opens the notification list screen — mark all read.
     */
    @PutMapping("/read-all")
    fun markAllAsRead(
        @AuthenticationPrincipal firebaseUid: String
    ): ResponseEntity<Unit> {
        val userId = userService.resolveUserId(firebaseUid)
        notificationService.markAllAsRead(userId)
        return ResponseEntity.ok().build()
    }
}
```

### Update `HirestoryApplication` — Add Notification Scan

```kotlin
// app/src/main/kotlin/com/hirestory/app/HirestoryApplication.kt
package com.hirestory.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = [
        "com.hirestory.app",
        "com.hirestory.user",
        "com.hirestory.content",
        "com.hirestory.notification"   // ← add this
    ]
)
class HirestoryApplication

fun main(args: Array<String>) {
    runApplication<HirestoryApplication>(*args)
}
```

---

## Chapter 10 Checkpoint ✅

- [ ] `notification/build.gradle.kts` resolves all dependencies
- [ ] `FIREBASE_SERVICE_ACCOUNT_KEY` env var set in IntelliJ Run Configuration
- [ ] `./gradlew :app:bootRun` starts without errors (Firebase Admin initializes)
- [ ] `POST /api/notifications/token` with valid JWT → FCM token stored in `fcm_tokens` table
- [ ] Approve an interview via admin endpoint → check `notifications` table → notification row created
- [ ] On a real Android device: install your KMP app, log in, register token, approve an interview → **push notification arrives**
- [ ] `GET /api/notifications` → returns the notification in the in-app list
- [ ] `PUT /api/notifications/read-all` → all notifications marked read
- [ ] `GET /api/notifications/unread-count` → returns 0 after marking all read
- [ ] Revoke an FCM token manually in Firebase Console → send a notification → expired token deleted from DB

---

## Complete File Layout After Chapter 10

```
hirestory-backend/
├── core/
│   └── src/main/kotlin/com/hirestory/core/
│       ├── exception/Exceptions.kt
│       ├── enums/(6 files)
│       ├── entity/(10 files)
│       └── messaging/
│           ├── MessagingConstants.kt   ✅ Ch10
│           └── NotificationEvent.kt   ✅ Ch10
│
├── user/
│   └── src/main/kotlin/com/hirestory/user/
│       ├── repository/UserRepository.kt
│       ├── service/UserService.kt       ← resolveUserId() used by content + notification
│       ├── controller/UserController.kt
│       └── dto/UserDtos.kt
│
├── content/
│   └── src/main/kotlin/com/hirestory/content/
│       ├── repository/(8 files)
│       ├── service/
│       │   ├── InterviewService.kt
│       │   ├── CompanyService.kt
│       │   ├── BookmarkService.kt
│       │   ├── ReadCounterService.kt
│       │   └── NotificationEventPublisher.kt
│       ├── controller/
│       │   ├── InterviewController.kt
│       │   ├── CompanyController.kt
│       │   ├── AdminController.kt
│       │   └── InternalInterviewController.kt
│       └── dto/(InterviewDtos, CompanyDtos, CrawledInterviewRequest)
│
├── notification/
│   └── src/main/kotlin/com/hirestory/notification/
│       ├── repository/
│       │   ├── NotificationRepository.kt
│       │   └── FcmTokenRepository.kt
│       ├── service/
│       │   ├── NotificationService.kt
│       │   └── NotificationConsumer.kt
│       ├── controller/
│       │   └── NotificationController.kt
│       ├── config/
│       │   └── FirebaseAdminConfig.kt
│       └── dto/NotificationDtos.kt
│
└── app/
    └── src/main/kotlin/com/hirestory/app/
        ├── HirestoryApplication.kt
        ├── exception/GlobalExceptionHandler.kt
        ├── security/
        │   ├── FirebaseJwtValidator.kt
        │   ├── FirebaseJwtFilter.kt
        │   └── SecurityConfig.kt
        └── config/
            ├── RedisConfig.kt       ✅ Ch7
            ├── RabbitMQConfig.kt    ✅ Ch7
            └── WebConfig.kt

hirestory-crawler/
└── src/main/kotlin/com/hirestory/crawler/
    ├── CrawlerApplication.kt
    ├── config/(RabbitMQConfig, WebConfig)
    ├── entity/CrawlJob.kt
    ├── repository/CrawlJobRepository.kt
    ├── service/
    │   ├── CrawlJobService.kt
    │   ├── AiExtractionService.kt
    │   ├── CrawlJobConsumer.kt
    │   ├── InterviewPublishClient.kt
    │   ├── reddit/RedditCrawlerService.kt
    │   └── gfg/GfgCrawlerService.kt
    ├── controller/CrawlerAdminController.kt
    └── dto/ExtractionDtos.kt
```

---

## What Runs Where

```
./gradlew :app:bootRun       → starts hirestory-backend on port 8080
                               serves all user-facing API endpoints
                               consumes notification.queue via NotificationConsumer

cd ../hirestory-crawler
./gradlew bootRun            → starts crawler on port 8083
                               crawls Reddit + GFG on schedule
                               processes via AI → posts to localhost:8080/internal
                               consumes crawl.queue via CrawlJobConsumer

Infrastructure (always running):
  PostgreSQL  port 5432  → all persistent data
  Redis       port 6379  → cache, read counters, URL dedup
  RabbitMQ    port 5672  → crawl.queue + notification.queue
```