# Backend Engineering with Spring Boot & Kotlin

## The HireStory Builder's Guide

---

# Chapter 10 — Testing: Unit Tests, Integration Tests & Confidence

### _Writing tests that prove your code works — before your users discover it doesn't_

---

## 10.1 The Problem This Chapter Solves

You have built nine chapters of production code. Controllers, services, repositories, security filters, crawlers, AI extraction. All of it works — right now, on your machine, with the data you tested manually.

But consider what happens next:

- You add a new field to `Interview` and the feed query silently breaks
- You change the free tier limit from 25 to 30 and the paywall stops firing
- You update the JWT filter and accidentally allow unauthenticated users through to protected endpoints
- You deploy to Railway and the database migration fails, but you do not notice for two hours because you were not watching the logs

Without tests, every change is a gamble. With tests, your code tells you immediately when something breaks.

This chapter teaches you three levels of testing for HireStory:

- **Unit tests** — test one class in isolation, fast, no database
- **Integration tests** — test one slice of the app with a real database
- **End-to-end tests** — test the full HTTP request lifecycle

---

## 10.2 Testing Tools in Spring Boot

Spring Boot includes everything you need. No extra frameworks required for most cases:

```kotlin
// build.gradle.kts — already have this from Chapter 1
testImplementation("org.springframework.boot:spring-boot-starter-test")
// Includes: JUnit 5, Mockito, AssertJ, Hamcrest, MockMvc, Spring Test

// For Kotlin-friendly mocking — better than Mockito in Kotlin
testImplementation("com.ninja-squad:springmockk:4.0.2")
// SpringMockK provides @MockkBean and @SpykBean — the Kotlin versions of
// @MockBean and @SpyBean

// Already in your dependencies:
testImplementation("org.springframework.security:spring-security-test")
```

### The Testing Toolkit

|Tool|What It Does|When You Use It|
|---|---|---|
|`JUnit 5`|Test runner — finds and runs your test methods|Always|
|`AssertJ`|Fluent assertions: `assertThat(x).isEqualTo(y)`|Always|
|`Mockk`|Creates mock objects in Kotlin|Unit tests|
|`@MockkBean`|Replaces a Spring bean with a mock|Controller/integration tests|
|`MockMvc`|Simulates HTTP requests without a real server|Controller tests|
|`@DataJpaTest`|Starts only the database layer — fast|Repository tests|
|`@WebMvcTest`|Starts only the web layer — fast|Controller tests|
|`@SpringBootTest`|Starts the full application|End-to-end tests|
|`TestContainers`|Starts a real PostgreSQL in Docker for tests|Integration tests|

---

## 10.3 Unit Tests — Testing in Isolation

Unit tests test one class with all dependencies mocked. They run in milliseconds. They do not touch the database, Redis, or RabbitMQ. They are your fastest feedback loop.

### The Pattern

```kotlin
// Every unit test follows this structure:

class SomeServiceTest {

    // 1. Create mocks of all dependencies
    private val someRepository = mockk<SomeRepository>()
    private val otherService = mockk<OtherService>()

    // 2. Create the class under test with mocked dependencies
    private val someService = SomeService(someRepository, otherService)

    @Test
    fun `descriptive name in backticks - Kotlin style`() {
        // 3. ARRANGE — set up what mocks return
        every { someRepository.findById(1L) } returns Optional.of(someEntity)

        // 4. ACT — call the method you are testing
        val result = someService.doSomething(1L)

        // 5. ASSERT — verify the result
        assertThat(result).isNotNull()
        assertThat(result.name).isEqualTo("Expected Name")

        // 6. VERIFY — confirm mocks were called correctly (optional)
        verify { someRepository.findById(1L) }
    }
}
```

### Testing InterviewService

```kotlin
// src/test/kotlin/com/example/hirestory/service/InterviewServiceTest.kt

package com.example.hirestory.service

import com.example.hirestory.entity.*
import com.example.hirestory.exception.ResourceNotFoundException
import com.example.hirestory.repository.*
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl

class InterviewServiceTest {

    private val interviewRepository = mockk<InterviewRepository>()
    private val companyRepository   = mockk<CompanyRepository>()
    private val bookmarkRepository  = mockk<BookmarkRepository>()
    private val readTrackingService = mockk<ReadTrackingService>()

    private val interviewService = InterviewService(
        interviewRepository,
        companyRepository,
        bookmarkRepository,
        readTrackingService
    )

    // Test data — created once, reused across tests
    private val googleCompany = Company(id = 1L, name = "Google", slug = "google")
    private val testUser      = User(id = 5L, clerkId = "user_abc", email = "test@test.com",
                                     name = "Test User", referralCode = "TEST123")
    private val testInterview = Interview(
        id = 1L,
        title = "Google SDE-1 Interview",
        headline = "Got an offer after 5 rounds",
        content = "Full content here...",
        role = "Software Engineer",
        experienceYears = 2,
        difficulty = Difficulty.HARD,
        outcome = Outcome.OFFER,
        slug = "google-sde-1-2024",
        status = InterviewStatus.PUBLISHED,
        company = googleCompany,
        user = testUser
    )

    @BeforeEach
    fun setUp() {
        // Clear all mock state before each test
        clearAllMocks()
    }

    // ── getFeed tests ─────────────────────────────────────────────────

    @Test
    fun `getFeed returns empty response when no interviews exist`() {
        // Arrange
        every {
            interviewRepository.findFeedIds(
                companyId = null,
                difficulty = null,
                outcome = null,
                pageable = any()
            )
        } returns SliceImpl(emptyList())

        // Act
        val result = interviewService.getFeed(
            page = 0, size = 20,
            companyId = null, difficulty = null, outcome = null,
            currentUser = null
        )

        // Assert
        assertThat(result.content).isEmpty()
        assertThat(result.hasNext).isFalse()

        // Verify: when IDs are empty, we never call findByIdsWithDetails
        verify(exactly = 0) { interviewRepository.findByIdsWithDetails(any()) }
    }

    @Test
    fun `getFeed returns interviews in correct order`() {
        // Arrange
        val ids = listOf(3L, 1L, 2L)  // The order from the database
        val interviews = listOf(
            testInterview.copy(id = 1L),
            testInterview.copy(id = 2L),
            testInterview.copy(id = 3L)
        )

        every {
            interviewRepository.findFeedIds(any(), any(), any(), any())
        } returns SliceImpl(ids, PageRequest.of(0, 20), false)

        every {
            interviewRepository.findByIdsWithDetails(ids)
        } returns interviews

        every {
            bookmarkRepository.findBookmarkedInterviewIds(any(), any())
        } returns emptyList()

        // Act
        val result = interviewService.getFeed(
            page = 0, size = 20,
            companyId = null, difficulty = null, outcome = null,
            currentUser = testUser
        )

        // Assert: order must match the ID slice order (3, 1, 2) not the list order
        assertThat(result.content).hasSize(3)
        assertThat(result.content[0].id).isEqualTo(3L)
        assertThat(result.content[1].id).isEqualTo(1L)
        assertThat(result.content[2].id).isEqualTo(2L)
    }

    @Test
    fun `getFeed marks bookmarked interviews correctly`() {
        // Arrange
        val ids = listOf(1L, 2L, 3L)
        every { interviewRepository.findFeedIds(any(), any(), any(), any()) } returns
            SliceImpl(ids, PageRequest.of(0, 20), false)
        every { interviewRepository.findByIdsWithDetails(any()) } returns
            listOf(testInterview.copy(id = 1L), testInterview.copy(id = 2L), testInterview.copy(id = 3L))
        // Only interview 2 is bookmarked
        every { bookmarkRepository.findBookmarkedInterviewIds(testUser, ids) } returns listOf(2L)

        // Act
        val result = interviewService.getFeed(
            page = 0, size = 20, companyId = null, difficulty = null, outcome = null,
            currentUser = testUser
        )

        // Assert: isBookmarked flag is correct per interview
        assertThat(result.content.find { it.id == 1L }?.isBookmarked).isFalse()
        assertThat(result.content.find { it.id == 2L }?.isBookmarked).isTrue()
        assertThat(result.content.find { it.id == 3L }?.isBookmarked).isFalse()
    }

    // ── getBySlug tests ───────────────────────────────────────────────

    @Test
    fun `getBySlug throws ResourceNotFoundException when interview not found`() {
        // Arrange
        every {
            interviewRepository.findPublishedBySlugWithDetails("nonexistent-slug")
        } returns null

        // Act & Assert
        assertThatThrownBy {
            interviewService.getBySlug("nonexistent-slug", currentUser = null)
        }
            .isInstanceOf(ResourceNotFoundException::class.java)
            .hasMessageContaining("Interview")
            .hasMessageContaining("nonexistent-slug")
    }

    @Test
    fun `getBySlug tracks read for authenticated user`() {
        // Arrange
        every {
            interviewRepository.findPublishedBySlugWithDetails("google-sde-1-2024")
        } returns testInterview

        every { bookmarkRepository.existsByUserAndInterview(testUser, testInterview) } returns false
        every { readTrackingService.trackRead(testUser, testInterview) } just runs

        // Act
        interviewService.getBySlug("google-sde-1-2024", currentUser = testUser)

        // Assert: readTrackingService.trackRead was called once with the correct user
        verify(exactly = 1) { readTrackingService.trackRead(testUser, testInterview) }
    }

    @Test
    fun `getBySlug does NOT track read for anonymous user`() {
        // Arrange
        every {
            interviewRepository.findPublishedBySlugWithDetails("google-sde-1-2024")
        } returns testInterview

        // Act
        interviewService.getBySlug("google-sde-1-2024", currentUser = null)

        // Assert: readTrackingService was never called
        verify(exactly = 0) { readTrackingService.trackRead(any(), any()) }
    }
}
```

### Testing ReadTrackingService

```kotlin
// src/test/kotlin/com/example/hirestory/service/ReadTrackingServiceTest.kt

package com.example.hirestory.service

import com.example.hirestory.config.HireStoryProperties
import com.example.hirestory.entity.*
import com.example.hirestory.exception.PaywallException
import com.example.hirestory.repository.ReadHistoryRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReadTrackingServiceTest {

    private val readHistoryRepository = mockk<ReadHistoryRepository>()
    private val cacheService          = mockk<CacheService>()
    private val properties            = HireStoryProperties(
        clerk = HireStoryProperties.ClerkProperties("", ""),
        freeTier = HireStoryProperties.FreeTierProperties(monthlyReadLimit = 25),
        crawler = mockk(),
        ai = mockk()
    )

    private val service = ReadTrackingService(readHistoryRepository, cacheService, properties)

    private val freeUser    = User(id = 1L, clerkId = "u1", email = "a@b.com",
                                   name = "Free User", isPremium = false, referralCode = "FREE1")
    private val premiumUser = User(id = 2L, clerkId = "u2", email = "b@b.com",
                                   name = "Premium", isPremium = true, referralCode = "PREM1")
    private val interview   = mockk<Interview>(relaxed = true)

    @BeforeEach fun setUp() = clearAllMocks()

    @Test
    fun `trackRead does nothing when user already read this interview`() {
        every { readHistoryRepository.existsByUserAndInterview(freeUser, interview) } returns true

        service.trackRead(freeUser, interview)

        // Nothing else should happen — no counter increment, no save
        verify(exactly = 0) { cacheService.incrementReadCount(any()) }
        verify(exactly = 0) { readHistoryRepository.save(any()) }
    }

    @Test
    fun `trackRead throws PaywallException when free user reaches limit`() {
        every { readHistoryRepository.existsByUserAndInterview(freeUser, interview) } returns false
        every { cacheService.getReadCount(1L) } returns 25  // At the limit

        assertThatThrownBy { service.trackRead(freeUser, interview) }
            .isInstanceOf(PaywallException::class.java)

        // No read recorded — paywall fired before saving
        verify(exactly = 0) { readHistoryRepository.save(any()) }
        verify(exactly = 0) { cacheService.incrementReadCount(any()) }
    }

    @Test
    fun `trackRead succeeds and increments counter for free user under limit`() {
        every { readHistoryRepository.existsByUserAndInterview(freeUser, interview) } returns false
        every { cacheService.getReadCount(1L) } returns 10  // Under limit
        every { readHistoryRepository.save(any()) } returns mockk()
        every { cacheService.incrementReadCount(1L) } returns 11L

        service.trackRead(freeUser, interview)

        verify(exactly = 1) { readHistoryRepository.save(any()) }
        verify(exactly = 1) { cacheService.incrementReadCount(1L) }
    }

    @Test
    fun `trackRead never enforces paywall for premium user`() {
        every { readHistoryRepository.existsByUserAndInterview(premiumUser, interview) } returns false
        every { readHistoryRepository.save(any()) } returns mockk()
        every { cacheService.incrementReadCount(2L) } returns 100L

        // Premium user reads their 500th interview — no exception
        service.trackRead(premiumUser, interview)

        verify(exactly = 1) { readHistoryRepository.save(any()) }
        // cacheService.getReadCount is NEVER called for premium users
        verify(exactly = 0) { cacheService.getReadCount(any()) }
    }
}
```

---

## 10.4 Repository Tests — @DataJpaTest

`@DataJpaTest` starts only the JPA layer: entities, repositories, and an in-memory H2 database. Everything else (controllers, services, security) is not loaded. These tests run in about 3-5 seconds.

```kotlin
// src/test/kotlin/com/example/hirestory/repository/InterviewRepositoryTest.kt

package com.example.hirestory.repository

import com.example.hirestory.entity.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest

@DataJpaTest
class InterviewRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var interviewRepository: InterviewRepository

    @Autowired
    private lateinit var companyRepository: CompanyRepository

    // Create test data and flush to in-memory H2
    private lateinit var google: Company
    private lateinit var amazon: Company
    private lateinit var publishedInterview: Interview
    private lateinit var pendingInterview: Interview

    @BeforeEach
    fun setUp() {
        google = entityManager.persistAndFlush(
            Company(name = "Google", slug = "google")
        )
        amazon = entityManager.persistAndFlush(
            Company(name = "Amazon", slug = "amazon")
        )

        publishedInterview = entityManager.persistAndFlush(
            Interview(
                title = "Google SDE-1",
                headline = "Got offer",
                content = "Full content",
                role = "SDE-1",
                experienceYears = 2,
                difficulty = Difficulty.HARD,
                outcome = Outcome.OFFER,
                slug = "google-sde-1-2024",
                status = InterviewStatus.PUBLISHED,
                company = google
            )
        )

        pendingInterview = entityManager.persistAndFlush(
            Interview(
                title = "Amazon SDE-2",
                headline = "Pending review",
                content = "Content here",
                role = "SDE-2",
                experienceYears = 4,
                difficulty = Difficulty.MEDIUM,
                outcome = Outcome.REJECTED,
                slug = "amazon-sde-2-2024",
                status = InterviewStatus.PENDING,
                company = amazon
            )
        )

        entityManager.clear()  // Clear persistence context — forces fresh DB reads
    }

    @Test
    fun `findBySlug returns interview when it exists`() {
        val result = interviewRepository.findBySlug("google-sde-1-2024")
        assertThat(result).isNotNull()
        assertThat(result!!.title).isEqualTo("Google SDE-1")
    }

    @Test
    fun `findBySlug returns null when slug does not exist`() {
        val result = interviewRepository.findBySlug("does-not-exist")
        assertThat(result).isNull()
    }

    @Test
    fun `findFeedIds returns only PUBLISHED interviews`() {
        val pageable = PageRequest.of(0, 20)
        val ids = interviewRepository.findFeedIds(
            companyId = null,
            difficulty = null,
            outcome = null,
            pageable = pageable
        )

        // Only publishedInterview should appear — pendingInterview is PENDING
        assertThat(ids.content).hasSize(1)
        assertThat(ids.content[0]).isEqualTo(publishedInterview.id)
    }

    @Test
    fun `findFeedIds filters by company correctly`() {
        val pageable = PageRequest.of(0, 20)

        // Filter by Google
        val googleResults = interviewRepository.findFeedIds(
            companyId = google.id,
            difficulty = null,
            outcome = null,
            pageable = pageable
        )
        assertThat(googleResults.content).hasSize(1)

        // Filter by Amazon — but the Amazon interview is PENDING, not PUBLISHED
        val amazonResults = interviewRepository.findFeedIds(
            companyId = amazon.id,
            difficulty = null,
            outcome = null,
            pageable = pageable
        )
        assertThat(amazonResults.content).isEmpty()
    }

    @Test
    fun `existsBySlug returns true for existing slug`() {
        assertThat(interviewRepository.existsBySlug("google-sde-1-2024")).isTrue()
    }

    @Test
    fun `existsBySlug returns false for non-existing slug`() {
        assertThat(interviewRepository.existsBySlug("does-not-exist")).isFalse()
    }

    @Test
    fun `findFeedIds returns results in descending addedAt order`() {
        // Create a third interview with older timestamp
        val olderInterview = entityManager.persistAndFlush(
            Interview(
                title = "Old Interview",
                headline = "Old",
                content = "Old content",
                role = "SDE-1",
                experienceYears = 1,
                difficulty = Difficulty.EASY,
                outcome = Outcome.OFFER,
                slug = "old-interview-2023",
                status = InterviewStatus.PUBLISHED,
                company = google,
                addedAt = LocalDateTime.now().minusDays(10)  // 10 days ago
            )
        )
        entityManager.clear()

        val pageable = PageRequest.of(0, 20)
        val ids = interviewRepository.findFeedIds(null, null, null, pageable)

        // Newest first — publishedInterview is newer than olderInterview
        assertThat(ids.content).hasSize(2)
        assertThat(ids.content[0]).isEqualTo(publishedInterview.id)
        assertThat(ids.content[1]).isEqualTo(olderInterview.id)
    }
}
```

### Using a Real PostgreSQL for Tests — TestContainers

H2 (the in-memory database `@DataJpaTest` uses by default) does not support all PostgreSQL features. Your full-text search queries, the `DISTINCT` keyword on JPQL with `JOIN FETCH`, and PostgreSQL-specific functions will behave differently in H2.

For queries that must match production behaviour exactly, use TestContainers:

```kotlin
// build.gradle.kts
testImplementation("org.testcontainers:postgresql:1.20.4")
testImplementation("org.testcontainers:junit-jupiter:1.20.4")
```

```kotlin
// src/test/kotlin/com/example/hirestory/repository/PostgresRepositoryTest.kt

package com.example.hirestory.repository

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

// @Testcontainers — JUnit extension that manages container lifecycle
@Testcontainers
// Replace H2 with the real PostgreSQL container
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
abstract class PostgresRepositoryTest {

    companion object {
        // Static — one container shared across all tests in this class
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("hirestory_test")
            withUsername("test")
            withPassword("test")
        }

        // Wire the container's connection details into Spring's config
        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}

// Extend this class for any test that needs a real PostgreSQL
class InterviewRepositoryPostgresTest : PostgresRepositoryTest() {
    // Your tests here run against real PostgreSQL
}
```

---

## 10.5 Controller Tests — @WebMvcTest

`@WebMvcTest` loads only the web layer: controllers, filters, exception handlers. Services and repositories are not loaded — you mock them.

```kotlin
// src/test/kotlin/com/example/hirestory/controller/InterviewControllerTest.kt

package com.example.hirestory.controller

import com.example.hirestory.dto.*
import com.example.hirestory.entity.*
import com.example.hirestory.security.JwtAuthenticationFilter
import com.example.hirestory.service.InterviewService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import com.fasterxml.jackson.databind.ObjectMapper

// @WebMvcTest — loads only this controller and related web infrastructure
@WebMvcTest(InterviewController::class)
// You need to import your security config — otherwise Spring applies its default security
// which blocks everything
class InterviewControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    // Replace the real InterviewService with a mock
    @MockkBean
    private lateinit var interviewService: InterviewService

    // Mock the JWT filter — we test security separately
    @MockkBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    // Test data
    private val sampleDto = InterviewSummaryDto(
        id = 1L,
        slug = "google-sde-1-2024",
        title = "Google SDE-1 Interview",
        company = CompanyDto(1L, "Google", null, "google", 5),
        role = "SDE-1",
        difficulty = Difficulty.HARD,
        outcome = Outcome.OFFER,
        experienceYears = 2,
        roundsCount = 5,
        addedAt = "15 Jan 2024",
        tags = listOf("DSA", "System Design")
    )

    // ── GET /interviews tests ──────────────────────────────────────────

    @Test
    @WithMockUser    // Simulates an authenticated user — bypasses JWT filter in tests
    fun `GET interviews returns 200 with interview list`() {
        // Arrange
        every {
            interviewService.getFeed(
                page = 0, size = 20,
                companyId = null, difficulty = null, outcome = null,
                currentUser = any()
            )
        } returns SliceResponse(listOf(sampleDto), false, 0, 20)

        // Act & Assert
        mockMvc.get("/interviews") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.content") { isArray() }
            jsonPath("$.content.length()") { value(1) }
            jsonPath("$.content[0].slug") { value("google-sde-1-2024") }
            jsonPath("$.content[0].company.name") { value("Google") }
            jsonPath("$.hasNext") { value(false) }
        }
    }

    @Test
    @WithMockUser
    fun `GET interviews with invalid page parameter returns 400`() {
        mockMvc.get("/interviews?page=abc") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.status") { value(400) }
            jsonPath("$.error") { value("Invalid Parameter") }
        }
    }

    @Test
    @WithMockUser
    fun `GET interviews by nonexistent slug returns 404`() {
        every {
            interviewService.getBySlug("does-not-exist", any())
        } throws ResourceNotFoundException("Interview", "does-not-exist")

        mockMvc.get("/interviews/does-not-exist") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.status") { value(404) }
            jsonPath("$.error") { value("Not Found") }
        }
    }

    // ── POST /interviews tests ─────────────────────────────────────────

    @Test
    @WithMockUser
    fun `POST interviews with valid data returns 201`() {
        val createDto = CreateInterviewDto(
            title = "Google SDE-1 Interview Experience 2024",
            headline = "Got offer after 5 tough rounds. Focus on graphs.",
            content = "A".repeat(100),   // Minimum 100 chars
            role = "SDE-1",
            experienceYears = 2,
            difficulty = Difficulty.HARD,
            outcome = Outcome.OFFER
        )

        every { interviewService.submit(any(), any()) } returns mockk(relaxed = true)

        mockMvc.post("/interviews") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createDto)
        }.andExpect {
            status { isCreated() }   // 201, not 200
        }
    }

    @Test
    @WithMockUser
    fun `POST interviews with blank title returns 400 with field error`() {
        val invalidDto = mapOf(
            "title" to "",          // Blank — violates @field:NotBlank
            "content" to "x",       // Too short — violates @field:Size(min=100)
            "experienceYears" to -1 // Negative — violates @field:Min(0)
        )

        mockMvc.post("/interviews") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(invalidDto)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors") { isArray() }
            // At least one field error for the title
            jsonPath("$.fieldErrors[?(@.field == 'title')]") { exists() }
        }
    }

    @Test
    fun `GET interviews feed is accessible without authentication`() {
        // This endpoint must be public — no @WithMockUser
        every {
            interviewService.getFeed(any(), any(), any(), any(), any(), any())
        } returns SliceResponse(emptyList(), false, 0, 20)

        mockMvc.get("/interviews") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            // Must be 200, not 401 — the feed is public
            status { isOk() }
        }
    }

    @Test
    fun `POST interviews without authentication returns 401`() {
        // No @WithMockUser — simulates anonymous request
        mockMvc.post("/interviews") {
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect {
            status { isUnauthorized() }   // 401
        }
    }
}
```

---

## 10.6 Security Tests — Testing the JWT Filter

Test your `JwtAuthenticationFilter` directly:

```kotlin
// src/test/kotlin/com/example/hirestory/security/JwtAuthenticationFilterTest.kt

package com.example.hirestory.security

import com.example.hirestory.entity.User
import com.example.hirestory.repository.UserRepository
import io.jsonwebtoken.Claims
import io.mockk.*
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthenticationFilterTest {

    private val jwtService      = mockk<JwtService>()
    private val userRepository  = mockk<UserRepository>()
    private val filter          = JwtAuthenticationFilter(jwtService, userRepository)

    private val request         = mockk<HttpServletRequest>(relaxed = true)
    private val response        = mockk<HttpServletResponse>(relaxed = true)
    private val filterChain     = mockk<FilterChain>(relaxed = true)

    private val testUser = User(
        id = 1L, clerkId = "user_abc", email = "test@test.com",
        name = "Test User", referralCode = "TEST1"
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        SecurityContextHolder.clearContext()  // Clean security context between tests
    }

    @Test
    fun `request with no Authorization header continues without authentication`() {
        every { request.getHeader("Authorization") } returns null

        filter.doFilterInternal(request, response, filterChain)

        // Filter chain continues — request not blocked
        verify { filterChain.doFilter(request, response) }
        // No authentication set in context
        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test
    fun `request with invalid token continues without authentication`() {
        every { request.getHeader("Authorization") } returns "Bearer invalid.token.here"
        every { jwtService.validateAndExtractClaims("invalid.token.here") } returns null

        filter.doFilterInternal(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test
    fun `request with valid token sets authentication in security context`() {
        val mockClaims = mockk<Claims>()
        every { request.getHeader("Authorization") } returns "Bearer valid.jwt.token"
        every { jwtService.validateAndExtractClaims("valid.jwt.token") } returns mockClaims
        every { jwtService.extractClerkId(mockClaims) } returns "user_abc"
        every { userRepository.findByClerkId("user_abc") } returns testUser

        filter.doFilterInternal(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }

        // Authentication MUST be set in the security context
        val auth = SecurityContextHolder.getContext().authentication
        assertThat(auth).isNotNull()
        assertThat(auth.principal).isEqualTo(testUser)
        assertThat(auth.authorities.map { it.authority }).contains("ROLE_USER")
    }

    @Test
    fun `valid token for unknown user continues without authentication`() {
        val mockClaims = mockk<Claims>()
        every { request.getHeader("Authorization") } returns "Bearer valid.jwt.token"
        every { jwtService.validateAndExtractClaims("valid.jwt.token") } returns mockClaims
        every { jwtService.extractClerkId(mockClaims) } returns "user_unknown"
        // User exists in Clerk but not in your database yet
        every { userRepository.findByClerkId("user_unknown") } returns null

        filter.doFilterInternal(request, response, filterChain)

        // Filter chain continues — the endpoint will decide if auth is required
        verify { filterChain.doFilter(request, response) }
        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test
    fun `premium user gets ROLE_PREMIUM authority`() {
        val premiumUser = testUser.copy(isPremium = true)
        val mockClaims = mockk<Claims>()
        every { request.getHeader("Authorization") } returns "Bearer valid.jwt.token"
        every { jwtService.validateAndExtractClaims(any()) } returns mockClaims
        every { jwtService.extractClerkId(any()) } returns premiumUser.clerkId
        every { userRepository.findByClerkId(premiumUser.clerkId) } returns premiumUser

        filter.doFilterInternal(request, response, filterChain)

        val auth = SecurityContextHolder.getContext().authentication
        val authorities = auth.authorities.map { it.authority }
        assertThat(authorities).contains("ROLE_USER")
        assertThat(authorities).contains("ROLE_PREMIUM")
    }
}
```

---

## 10.7 Integration Tests — @SpringBootTest

Integration tests start the full Spring Boot application and test the complete request-to-database flow. They are slower but give you the most confidence.

```kotlin
// src/test/kotlin/com/example/hirestory/integration/InterviewApiIntegrationTest.kt

package com.example.hirestory.integration

import com.example.hirestory.entity.*
import com.example.hirestory.repository.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.transaction.annotation.Transactional

// @SpringBootTest — full application context
// @AutoConfigureMockMvc — gives you MockMvc without a real HTTP server
// @ActiveProfiles("test") — uses application-test.yml
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class InterviewApiIntegrationTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var companyRepository: CompanyRepository
    @Autowired private lateinit var interviewRepository: InterviewRepository

    @BeforeEach
    fun setUp() {
        interviewRepository.deleteAll()
        companyRepository.deleteAll()
    }

    @Test
    @Order(1)
    fun `feed returns empty list when no interviews exist`() {
        mockMvc.get("/api/interviews") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.content") { isArray() }
            jsonPath("$.content.length()") { value(0) }
        }
    }

    @Test
    @Order(2)
    @Transactional
    fun `feed returns only published interviews`() {
        // Seed directly into DB
        val company = companyRepository.save(Company(name = "Google", slug = "google"))
        interviewRepository.save(Interview(
            title = "Google SDE-1",
            headline = "Offer",
            content = "Content",
            role = "SDE-1",
            experienceYears = 2,
            difficulty = Difficulty.HARD,
            outcome = Outcome.OFFER,
            slug = "google-sde-1",
            status = InterviewStatus.PUBLISHED,
            company = company
        ))
        interviewRepository.save(Interview(
            title = "Google SDE-2 Pending",
            headline = "Pending",
            content = "Content",
            role = "SDE-2",
            experienceYears = 4,
            difficulty = Difficulty.MEDIUM,
            outcome = Outcome.REJECTED,
            slug = "google-sde-2-pending",
            status = InterviewStatus.PENDING,  // This should NOT appear in feed
            company = company
        ))

        mockMvc.get("/api/interviews") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.content.length()") { value(1) }  // Only the published one
            jsonPath("$.content[0].slug") { value("google-sde-1") }
        }
    }

    @Test
    @Order(3)
    fun `GET nonexistent interview returns 404`() {
        mockMvc.get("/api/interviews/does-not-exist") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.status") { value(404) }
            jsonPath("$.error") { exists() }
            jsonPath("$.message") { exists() }
            jsonPath("$.path") { value("/api/interviews/does-not-exist") }
            jsonPath("$.timestamp") { exists() }
        }
    }
}
```

### Test application.yml

Create a separate config for tests:

```yaml
# src/test/resources/application-test.yml

spring:
  datasource:
    # Use H2 in-memory for tests — fast, no setup needed
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false;MODE=PostgreSQL
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop    # OK for tests — recreate schema each run
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
  flyway:
    enabled: false    # Flyway does not work with ddl-auto: create-drop
  data:
    redis:
      # Use a separate Redis database for tests (database 1, not 0)
      database: 1

hirestory:
  clerk:
    jwks-url: https://test.clerk.accounts.dev/.well-known/jwks.json
    webhook-secret: test-secret
  free-tier:
    monthly-read-limit: 25
  ai:
    extraction:
      min-confidence-score: 50
      auto-publish-threshold: 80
      max-retries: 1
  crawler:
    enabled: false    # Never run the crawler during tests

logging:
  level:
    root: WARN        # Quiet logs during tests
    com.example.hirestory: INFO
```

---

## 10.8 Testing Validation — Asserting Field Errors

```kotlin
// src/test/kotlin/com/example/hirestory/controller/ValidationTest.kt

@WebMvcTest(InterviewController::class)
class ValidationTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper
    @MockkBean private lateinit var interviewService: InterviewService

    @Test
    @WithMockUser
    fun `submit with all invalid fields returns all field errors`() {
        val invalidPayload = mapOf(
            "title" to "",           // NotBlank violation
            "headline" to "",        // NotBlank violation
            "content" to "short",    // Size min=100 violation
            "role" to "",            // NotBlank violation
            "experienceYears" to -1, // Min=0 violation
            "difficulty" to null,    // NotNull violation
            "outcome" to null        // NotNull violation
        )

        mockMvc.post("/interviews") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(invalidPayload)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors") { isArray() }
            // Assert each field error exists
            jsonPath("$.fieldErrors[?(@.field == 'title')]") { exists() }
            jsonPath("$.fieldErrors[?(@.field == 'content')]") { exists() }
            jsonPath("$.fieldErrors[?(@.field == 'experienceYears')]") { exists() }
        }
    }

    @Test
    @WithMockUser
    fun `submit with malformed JSON returns 400`() {
        mockMvc.post("/interviews") {
            contentType = MediaType.APPLICATION_JSON
            content = "{ this is not valid json }"
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("Malformed Request") }
        }
    }
}
```

---

## 10.9 Testing the Crawler

```kotlin
// src/test/kotlin/com/example/hirestory/crawler/CrawlJobServiceTest.kt

package com.example.hirestory.crawler

import com.example.hirestory.entity.CrawlJob
import com.example.hirestory.entity.CrawlStatus
import com.example.hirestory.messaging.CrawlPublisher
import com.example.hirestory.repository.CrawlJobRepository
import com.example.hirestory.service.CacheService
import com.example.hirestory.service.CrawlJobService
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CrawlJobServiceTest {

    private val crawlJobRepository = mockk<CrawlJobRepository>()
    private val crawlPublisher     = mockk<CrawlPublisher>(relaxed = true)
    private val cacheService       = mockk<CacheService>()
    private val service            = CrawlJobService(crawlJobRepository, crawlPublisher, cacheService)

    @BeforeEach fun setUp() = clearAllMocks()

    @Test
    fun `processBatch skips URL already in Redis`() {
        val results = listOf(
            CrawlResult("https://reddit.com/r/test/1", "Title", "Content", CrawlSource.REDDIT, null)
        )

        // Redis says it has seen this URL before
        every { cacheService.isUrlAlreadyCrawled(any()) } returns true

        val queued = service.processBatch(results)

        assertThat(queued).isEqualTo(0)
        verify(exactly = 0) { crawlJobRepository.save(any()) }
        verify(exactly = 0) { crawlPublisher.publishCrawlJob(any(), any(), any()) }
    }

    @Test
    fun `processBatch skips URL already in database`() {
        val results = listOf(
            CrawlResult("https://reddit.com/r/test/2", "Title", "Content", CrawlSource.REDDIT, null)
        )

        every { cacheService.isUrlAlreadyCrawled(any()) } returns false
        every { crawlJobRepository.existsBySourceUrl(any()) } returns true  // In DB

        val queued = service.processBatch(results)

        assertThat(queued).isEqualTo(0)
        verify(exactly = 0) { crawlJobRepository.save(any()) }
    }

    @Test
    fun `processBatch queues new URL and publishes to RabbitMQ`() {
        val url = "https://reddit.com/r/test/3"
        val results = listOf(
            CrawlResult(url, "New Title", "New Content", CrawlSource.REDDIT, null)
        )

        every { cacheService.isUrlAlreadyCrawled(any()) } returns false
        every { crawlJobRepository.existsBySourceUrl(any()) } returns false
        every { crawlJobRepository.save(any()) } returns CrawlJob(
            id = 42L, sourceUrl = url, status = CrawlStatus.PENDING
        )

        val queued = service.processBatch(results)

        assertThat(queued).isEqualTo(1)
        verify(exactly = 1) { crawlJobRepository.save(any()) }
        verify(exactly = 1) { crawlPublisher.publishCrawlJob(42L, any(), any()) }
    }

    @Test
    fun `processBatch deduplicates within the same batch`() {
        // Same URL appears twice in the same crawl run (Reddit + GFG both found it)
        val url = "https://example.com/interview"
        val results = listOf(
            CrawlResult(url, "Title 1", "Content 1", CrawlSource.REDDIT, null),
            CrawlResult(url, "Title 2", "Content 2", CrawlSource.GEEKSFORGEEKS, null)
        )

        // First call: Redis says new. Second call: Redis now says exists (set by first call)
        every { cacheService.isUrlAlreadyCrawled(any()) } returnsMany listOf(false, true)
        every { crawlJobRepository.existsBySourceUrl(any()) } returns false
        every { crawlJobRepository.save(any()) } returns CrawlJob(id = 1L, sourceUrl = url)

        val queued = service.processBatch(results)

        assertThat(queued).isEqualTo(1)  // Only queued once despite appearing twice
    }
}
```

---

## 10.10 Test Coverage — What to Test and What to Skip

Not everything needs a test. Here is the practical guide:

### Always test

- **Service business logic** — read count enforcement, bookmark duplicate prevention, referral validation, slug uniqueness, any decision-making code
- **Repository custom queries** — anything you wrote with `@Query`, especially the feed query with optional filters
- **Security filter** — token extraction, validation, user loading, context setting
- **Validation** — that `@field:` annotations actually reject bad input
- **Error responses** — that `GlobalExceptionHandler` returns the right shape

### Test if the logic is complex

- Crawler deduplication logic
- AI extraction processor decision tree (confidence thresholds)
- Cache eviction — does approve() actually evict the feed cache?
- Transaction rollback — does a failure mid-submit leave clean state?

### Skip

- Simple getter/setter DTO classes
- Entity classes with no logic
- Repository method name queries (Spring generates these — trust the framework)
- `DataInitializer` — it is only for development
- Trivial controllers that just delegate to a service

---

## 10.11 Common Mistakes in Chapter 10

### Mistake 1 — Testing implementation details instead of behaviour

```kotlin
// ❌ Testing HOW it works — fragile, breaks on refactoring
@Test
fun `submit calls save exactly once`() {
    interviewService.submit(dto, user)
    verify(exactly = 1) { interviewRepository.save(any()) }
    // If you refactor to use saveAll() — test breaks even though behaviour is correct
}

// ✅ Testing WHAT it does — robust, survives refactoring
@Test
fun `submit creates an interview with PENDING status`() {
    val savedSlot = slot<Interview>()
    every { interviewRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

    interviewService.submit(dto, user)

    assertThat(savedSlot.captured.status).isEqualTo(InterviewStatus.PENDING)
    assertThat(savedSlot.captured.title).isEqualTo(dto.title)
}
```

### Mistake 2 — Not clearing SecurityContextHolder between tests

```kotlin
// ❌ Security context from test 1 leaks into test 2
@Test
fun test1() {
    SecurityContextHolder.getContext().authentication = someAuth
    // Test runs...
    // SecurityContext NOT cleared — next test inherits this auth
}

// ✅ Always clear in @BeforeEach or @AfterEach
@BeforeEach
fun setUp() {
    SecurityContextHolder.clearContext()
}
```

### Mistake 3 — @DataJpaTest with PostgreSQL-specific queries using H2

```kotlin
// ❌ Your JPQL query uses PostgreSQL function — fails on H2
@DataJpaTest  // Uses H2 by default
class InterviewRepositoryTest {
    @Test
    fun `full text search works`() {
        // search_vector @@ plainto_tsquery — this is PostgreSQL-only
        // H2 does not understand it — test fails with SQL error
    }
}

// ✅ Use TestContainers for PostgreSQL-specific queries
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InterviewRepositoryTest : PostgresRepositoryTest() { ... }
```

### Mistake 4 — Forgetting @ActiveProfiles("test")

```kotlin
// ❌ Integration test uses your real application.yml
// Connects to your real PostgreSQL and Redis
// Mutates real data
@SpringBootTest
class SomeIntegrationTest { }

// ✅ Use test profile — in-memory DB, separate Redis database
@SpringBootTest
@ActiveProfiles("test")
class SomeIntegrationTest { }
```

### Mistake 5 — Not using relaxed = true for mocks you do not care about

```kotlin
// ❌ Mockk throws for every unstubbed call — you stub things you do not need
private val someService = mockk<SomeService>()
// You must stub every method even if you do not care about it in this test

// ✅ relaxed = true returns default values for unstubbed calls
private val someService = mockk<SomeService>(relaxed = true)
// Only stub what you actually need to assert
```

---

## 10.12 HireStory Connection — What You Built in Chapter 10

By the end of Chapter 10, HireStory has a test suite that:

- **Unit tests** for `InterviewService` — feed ordering, bookmark flags, read tracking delegation
- **Unit tests** for `ReadTrackingService` — paywall enforcement, premium bypass, re-read prevention
- **Unit tests** for `JwtAuthenticationFilter` — all five token scenarios
- **Unit tests** for `CrawlJobService` — Redis dedup, database dedup, batch dedup
- **Repository tests** with `@DataJpaTest` — feed query, filters, ordering, slug uniqueness
- **Controller tests** with `@WebMvcTest` — HTTP status codes, JSON response shape, validation errors, authentication requirements
- **Integration tests** with `@SpringBootTest` — full request-to-database flow for critical paths

Every critical business rule has a test:

- Free tier limit throws paywall at exactly 25 reads
- Re-reading does not count against the limit
- Premium users are never paywalled
- Feed returns only PUBLISHED interviews
- Duplicate bookmarks are rejected
- Invalid JWT tokens do not authenticate

---

## 10.13 Chapter Project — Build It Before You Move On

### What to build

A meaningful test suite for the three most important parts of HireStory.

**Part 1 — ReadTrackingService unit tests (most critical business logic)**

Write all four tests from Section 10.3. Run them. All four must pass. Pay attention to: the `verify(exactly = 0)` assertions — they confirm the code does NOT do something it should not.

**Part 2 — InterviewRepository tests with @DataJpaTest**

Write tests for:

- `findBySlug` returns interview when it exists
- `findBySlug` returns null when it does not
- `findFeedIds` only returns PUBLISHED interviews
- `findFeedIds` returns results newest-first
- `existsBySlug` returns correct boolean

**Part 3 — Controller tests with @WebMvcTest**

Write tests for `InterviewController`:

- `GET /interviews` returns 200 with correct structure
- `GET /interviews/slug` with nonexistent slug returns 404 with error body
- `POST /interviews` with blank title returns 400 with field errors
- `POST /interviews` without authentication returns 401
- `GET /interviews` (feed) is accessible without authentication

**Run everything:**

```bash
./gradlew test

# See test results
./gradlew test --info

# Generate HTML coverage report
./gradlew test jacocoTestReport
# Open: build/reports/jacoco/test/html/index.html
```

### Checkpoint questions — answer before moving on

1. `@DataJpaTest` starts faster than `@SpringBootTest`. Why? What does `@DataJpaTest` NOT load that `@SpringBootTest` does?
    
2. You write a test that calls `interviewService.submit()` and asserts the returned DTO has the correct company name. Is this a unit test or an integration test? What is the fastest way to run this test?
    
3. Your `ReadTrackingService` test mocks `CacheService`. In production, `CacheService` wraps Redis in try-catch and returns 0 on failure. Your mock returns whatever you tell it to. What important production behaviour does your unit test NOT cover? How would you cover it?
    
4. You have 47 unit tests and they all pass. You deploy to production and the feed returns 500 because the `JOIN FETCH` on tags conflicts with pagination in your specific version of Hibernate. Which test would have caught this? What type of test should you add?
    
5. You run `./gradlew test` and one test fails: `expected 401 but got 403`. The endpoint is `POST /api/bookmarks/1`. What does this tell you about either your `SecurityConfig` or your `@WithMockUser` setup?
    

---

_Chapter 11 → Deployment — Getting HireStory Live on Railway_

---

> **Book Progress:** Chapter 10 of 15 complete. Chapters ahead: Deployment · KMP Shared Module · Android UI · Next.js · Final Integration