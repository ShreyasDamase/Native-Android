# Backend Engineering with Spring Boot & Kotlin

## The HireStory Builder's Guide

---

# Chapter 4 — REST Controllers, Validation & Error Handling

### _Building a professional API layer — clean inputs, clean outputs, clean errors_

---

## 4.1 The Problem This Chapter Solves

After Chapters 2 and 3, your database layer is solid. You can read and write data correctly. But your API layer — the part the outside world actually talks to — is still incomplete.

A professional API does three things your current code does not yet do:

**1. It rejects bad input before it reaches your service.** If someone sends `experienceYears: -5` or an empty title, your database should never even see it. Validation catches it at the door.

**2. It returns consistent, predictable errors.** Right now, if something goes wrong, Spring returns its default white-label error page — HTML in a JSON API. Every error your API returns should look the same: a JSON object with a clear message, the right HTTP status code, and enough detail to fix the problem.

**3. It is structured so that adding new endpoints takes minutes.** By the end of this chapter, you have a pattern you repeat for every new feature. `CompanyController`, `InterviewController`, `BookmarkController` — they all follow the same shape.

---

## 4.2 The Full Request Lifecycle

Before writing code, understand exactly what happens from the moment a request arrives to the moment a response leaves.

```
Client sends: POST /api/interviews
              Body: { "title": "", "company": "Google" }
                         ↓
1. Tomcat receives the raw HTTP request
                         ↓
2. Spring Security filter chain
   → Is this endpoint public or protected?
   → If protected: is the JWT token valid?
   → If no token / invalid: return 401 immediately
                         ↓
3. DispatcherServlet
   → Which controller handles POST /api/interviews?
   → Found: InterviewController.submit()
                         ↓
4. Argument resolution
   → @RequestBody: deserialise JSON → CreateInterviewDto
   → @AuthenticationPrincipal: extract current user from Security context
                         ↓
5. @Valid — Bean Validation runs on CreateInterviewDto
   → title is blank → validation fails
   → MethodArgumentNotValidException is thrown
                         ↓
6. GlobalExceptionHandler catches the exception
   → Creates a clean error response
   → Returns 400 Bad Request with field-level errors
                         ↓
7. If validation passed: InterviewController.submit() runs
   → Calls InterviewService.submit(dto, user)
   → Service returns InterviewDetailDto
                         ↓
8. Jackson serialises InterviewDetailDto → JSON
                         ↓
9. Response sent: 201 Created + JSON body
```

Every step in this chain is something you control. This chapter covers steps 4, 5, 6, and 9 in depth.

---

## 4.3 Bean Validation — Rejecting Bad Input

Spring Boot includes the Bean Validation API (Jakarta Validation). You annotate your DTO fields with constraints. When a request arrives, Spring validates the DTO before your controller method even runs.

### The Validation Annotations You Need

```kotlin
// src/main/kotlin/com/example/hirestory/dto/InterviewDtos.kt

package com.example.hirestory.dto

import com.example.hirestory.entity.Difficulty
import com.example.hirestory.entity.Outcome
import jakarta.validation.Valid
import jakarta.validation.constraints.*

data class CreateInterviewDto(

    @field:NotBlank(message = "Title cannot be empty")
    @field:Size(min = 10, max = 255, message = "Title must be between 10 and 255 characters")
    val title: String,

    @field:NotBlank(message = "Headline cannot be empty")
    @field:Size(max = 500, message = "Headline cannot exceed 500 characters")
    val headline: String,

    @field:NotBlank(message = "Content cannot be empty")
    @field:Size(min = 100, message = "Content must be at least 100 characters")
    val content: String,

    @field:NotBlank(message = "Role cannot be empty")
    val role: String,

    val location: String? = null,

    @field:NotNull(message = "Experience years is required")
    @field:Min(value = 0, message = "Experience years cannot be negative")
    @field:Max(value = 50, message = "Experience years seems too high")
    val experienceYears: Int,

    @field:NotNull(message = "Difficulty is required")
    val difficulty: Difficulty,

    @field:NotNull(message = "Outcome is required")
    val outcome: Outcome,

    @field:DecimalMin(value = "0.0", message = "Salary cannot be negative")
    @field:DecimalMax(value = "500.0", message = "Salary seems too high")
    val salaryLpa: Double? = null,

    val isAnonymous: Boolean = false,

    @field:Valid                    // Validates each item in the list too
    @field:Size(max = 10, message = "Cannot have more than 10 rounds")
    val rounds: List<CreateRoundDto> = emptyList(),

    val tagNames: List<String> = emptyList()
)

data class CreateRoundDto(

    @field:NotNull(message = "Round number is required")
    @field:Min(value = 1, message = "Round number must be at least 1")
    val roundNumber: Int,

    @field:NotBlank(message = "Round title cannot be empty")
    val title: String,

    @field:NotBlank(message = "Questions cannot be empty")
    val questions: String,

    val difficulty: String? = null,
    val notes: String? = null
)
```

> **⚠️ The `@field:` prefix is required in Kotlin.** In Java, you write `@NotBlank`. In Kotlin, you must write `@field:NotBlank`. Without `@field:`, the annotation targets the property accessor (the getter), not the actual field. Bean Validation reads the field — so the annotation is silently ignored and validation never runs. This is the most common validation mistake in Kotlin + Spring Boot.

### Activating Validation in the Controller

```kotlin
@PostMapping
fun submit(
    @RequestBody @Valid dto: CreateInterviewDto,  // @Valid triggers validation
    //          ^^^^^^ Without this, validation annotations are ignored
): ResponseEntity<InterviewDetailDto> {
    ...
}
```

`@Valid` tells Spring: _before passing this object to my method, run the Bean Validation constraints on it. If any fail, throw`MethodArgumentNotValidException`._

Without `@Valid`, the validation annotations on your DTO do nothing. The request gets through regardless of the content.

### Complete Validation Annotations Reference

```kotlin
// Strings
@NotNull          // Field must not be null (but can be empty "")
@NotBlank         // Field must not be null AND not empty AND not just whitespace
@NotEmpty         // Field must not be null AND not empty (but can be whitespace)
@Size(min, max)   // String length OR collection size
@Email            // Must be a valid email format
@Pattern(regexp)  // Must match the regex

// Numbers
@Min(value)       // Must be >= value
@Max(value)       // Must be <= value
@DecimalMin       // For Double/BigDecimal — must be >= value
@DecimalMax       // For Double/BigDecimal — must be <= value
@Positive         // Must be > 0
@PositiveOrZero   // Must be >= 0
@Negative         // Must be < 0
@NegativeOrZero   // Must be <= 0

// Other
@NotNull          // Works on any type
@Null             // Must be null (rarely used)
@Valid            // Cascade validation into nested objects
@Past             // Date must be in the past
@Future           // Date must be in the future
```

---

## 4.4 Global Exception Handler — The Most Important Class in Your API

Without a global exception handler, Spring Boot returns different error formats depending on what went wrong:

- Validation error: Spring's HTML white-label error page
- Entity not found: a Java stack trace in JSON
- Your custom exception: potentially nothing (500 with no detail)

A global exception handler catches every exception and returns a consistent JSON error response. Every client — Android app, iOS app, Next.js web — receives the same predictable error format.

### Your Error Response Shape

First, define what every error response looks like:

```kotlin
// src/main/kotlin/com/example/hirestory/exception/ErrorResponse.kt

package com.example.hirestory.exception

import java.time.LocalDateTime

// Every error the API returns has this exact shape
data class ErrorResponse(
    val status: Int,              // HTTP status code: 400, 401, 403, 404, 500
    val error: String,            // Short error type: "Validation Failed", "Not Found"
    val message: String,          // Human-readable explanation
    val path: String,             // Which endpoint caused this: "/api/interviews/999"
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val fieldErrors: List<FieldError>? = null  // Only present for validation errors
)

data class FieldError(
    val field: String,            // Which field failed: "title", "experienceYears"
    val message: String,          // Why it failed: "Title cannot be empty"
    val rejectedValue: Any?       // What was sent: "" or -5
)
```

On the Android or web app, every error response looks like:

```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "Request contains invalid fields",
  "path": "/api/interviews",
  "timestamp": "2024-01-15T10:30:00",
  "fieldErrors": [
    {
      "field": "title",
      "message": "Title cannot be empty",
      "rejectedValue": ""
    },
    {
      "field": "experienceYears",
      "message": "Experience years cannot be negative",
      "rejectedValue": -5
    }
  ]
}
```

### Custom Exceptions

Before the handler, define your custom exceptions:

```kotlin
// src/main/kotlin/com/example/hirestory/exception/Exceptions.kt

package com.example.hirestory.exception

// 404 — Resource was not found
class ResourceNotFoundException(
    val resourceName: String,
    val identifier: Any
) : RuntimeException("$resourceName not found: $identifier")

// 409 — Conflict (duplicate bookmark, duplicate company, etc.)
class DuplicateResourceException(
    val resourceName: String,
    val detail: String
) : RuntimeException("$resourceName already exists: $detail")

// 403 — User is not allowed to do this specific thing
class ForbiddenOperationException(
    message: String
) : RuntimeException(message)

// 402 — Free tier limit reached
class PaywallException(
    val readsUsed: Int,
    val limit: Int
) : RuntimeException("Monthly read limit reached: $readsUsed/$limit")

// 400 — Business rule violation (not a validation error — a logic error)
class BusinessRuleException(
    message: String
) : RuntimeException(message)
```

### The Global Exception Handler

```kotlin
// src/main/kotlin/com/example/hirestory/exception/GlobalExceptionHandler.kt

package com.example.hirestory.exception

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

// @RestControllerAdvice = @ControllerAdvice + @ResponseBody
// This class intercepts exceptions thrown from ANY @RestController in your app
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // ── Validation errors ────────────────────────────────────────────
    // Thrown by @Valid when a DTO fails validation constraints
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationError(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {

        val fieldErrors = ex.bindingResult.fieldErrors.map { error ->
            FieldError(
                field = error.field,
                message = error.defaultMessage ?: "Invalid value",
                rejectedValue = error.rejectedValue
            )
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                status = 400,
                error = "Validation Failed",
                message = "Request contains ${fieldErrors.size} invalid field(s)",
                path = request.requestURI,
                fieldErrors = fieldErrors
            )
        )
        // Log at DEBUG — validation errors are expected, not alarming
        .also { log.debug("Validation failed for {}: {}", request.requestURI, fieldErrors) }
    }

    // ── Resource not found ───────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(
        ex: ResourceNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(
                status = 404,
                error = "Not Found",
                message = ex.message ?: "Resource not found",
                path = request.requestURI
            )
        )
    }

    // ── Duplicate resource ───────────────────────────────────────────
    @ExceptionHandler(DuplicateResourceException::class)
    fun handleDuplicate(
        ex: DuplicateResourceException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse(
                status = 409,
                error = "Conflict",
                message = ex.message ?: "Resource already exists",
                path = request.requestURI
            )
        )
    }

    // ── Forbidden operation ──────────────────────────────────────────
    @ExceptionHandler(ForbiddenOperationException::class)
    fun handleForbidden(
        ex: ForbiddenOperationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ErrorResponse(
                status = 403,
                error = "Forbidden",
                message = ex.message ?: "You are not allowed to perform this action",
                path = request.requestURI
            )
        )
    }

    // ── Paywall ──────────────────────────────────────────────────────
    @ExceptionHandler(PaywallException::class)
    fun handlePaywall(
        ex: PaywallException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(
            ErrorResponse(
                status = 402,
                error = "Paywall",
                message = "You have used ${ex.readsUsed} of ${ex.limit} free reads this month. Upgrade to continue.",
                path = request.requestURI
            )
        )
    }

    // ── Business rule violation ──────────────────────────────────────
    @ExceptionHandler(BusinessRuleException::class)
    fun handleBusinessRule(
        ex: BusinessRuleException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                status = 400,
                error = "Bad Request",
                message = ex.message ?: "Request violates a business rule",
                path = request.requestURI
            )
        )
    }

    // ── Malformed JSON body ──────────────────────────────────────────
    // Thrown when the request body cannot be parsed as JSON
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedJson(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                status = 400,
                error = "Malformed Request",
                message = "Request body is not valid JSON or has incorrect field types",
                path = request.requestURI
            )
        )
    }

    // ── Missing required query parameter ────────────────────────────
    // Thrown when a @RequestParam(required = true) is missing
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(
        ex: MissingServletRequestParameterException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                status = 400,
                error = "Missing Parameter",
                message = "Required parameter '${ex.parameterName}' is missing",
                path = request.requestURI
            )
        )
    }

    // ── Wrong type for path variable or query param ──────────────────
    // Thrown when ?page=abc is passed where Int is expected
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                status = 400,
                error = "Invalid Parameter",
                message = "Parameter '${ex.name}' has invalid value: '${ex.value}'",
                path = request.requestURI
            )
        )
    }

    // ── Catch-all — anything not handled above ───────────────────────
    // Log the full stack trace — this is unexpected and needs investigation
    @ExceptionHandler(Exception::class)
    fun handleGeneric(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        // Log at ERROR with full stack trace — this is always a bug
        log.error("Unhandled exception at {}: {}", request.requestURI, ex.message, ex)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                status = 500,
                error = "Internal Server Error",
                // Never expose internal error details to the client in production
                message = "An unexpected error occurred. Please try again later.",
                path = request.requestURI
            )
        )
    }
}
```

> **💡 Order of @ExceptionHandler methods does not matter.** Spring matches the most specific exception class first. `ResourceNotFoundException` is matched before `Exception`. Your catch-all `Exception` handler only runs if nothing more specific matches.

---

## 4.5 ResponseEntity — Controlling Your HTTP Response

`ResponseEntity<T>` is the return type that gives you full control over what the HTTP response looks like — status code, headers, and body.

```kotlin
// Return 200 OK with a body
return ResponseEntity.ok(dto)

// Return 201 Created with a body
return ResponseEntity.status(HttpStatus.CREATED).body(dto)

// Return 204 No Content (for DELETE — nothing to return)
return ResponseEntity.noContent().build()

// Return 404 Not Found with no body
return ResponseEntity.notFound().build()

// Adding custom headers
return ResponseEntity
    .status(HttpStatus.CREATED)
    .header("X-Interview-Id", interview.id.toString())
    .body(dto)
```

### HTTP Status Codes — Use The Right One

Most beginners return 200 for everything. This is wrong. Status codes communicate meaning to clients:

|Operation|Correct Status|Why|
|---|---|---|
|GET — found|200 OK|Standard success|
|POST — created|201 Created|Specifically means "new resource created"|
|PUT/PATCH — updated|200 OK|Resource was updated and returned|
|DELETE — success|204 No Content|Deleted, nothing to return|
|Validation error|400 Bad Request|Client sent wrong data|
|Not authenticated|401 Unauthorized|No valid token|
|Authenticated but no permission|403 Forbidden|Token valid, action not allowed|
|Not found|404 Not Found|Resource does not exist|
|Duplicate|409 Conflict|Resource already exists|
|Paywall|402 Payment Required|Free tier limit reached|
|Server error|500 Internal Server Error|Bug on your side|

---

## 4.6 The Complete HireStory API Controllers

Now write every controller. Each one follows the same pattern:

- Constructor injection of the service
- Request mapping on the class
- Method-level mappings for each operation
- `@Valid` on every `@RequestBody`
- Correct HTTP status codes
- DTOs — never raw entities

### Interview Controller

```kotlin
// src/main/kotlin/com/example/hirestory/controller/InterviewController.kt

package com.example.hirestory.controller

import com.example.hirestory.dto.*
import com.example.hirestory.entity.Difficulty
import com.example.hirestory.entity.Outcome
import com.example.hirestory.service.InterviewService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/interviews")
class InterviewController(private val interviewService: InterviewService) {

    // GET /api/interviews
    // GET /api/interviews?company=1&difficulty=HARD&outcome=OFFER&page=0&size=20
    @GetMapping
    fun getFeed(
        @RequestParam(required = false) companyId: Long?,
        @RequestParam(required = false) difficulty: Difficulty?,
        @RequestParam(required = false) outcome: Outcome?,
        @RequestParam(required = false) role: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
        // currentUser injection covered in Chapter 5 (Spring Security)
        // For now leave it null
    ): ResponseEntity<SliceResponse<InterviewSummaryDto>> {
        val result = interviewService.getFeed(
            page = page,
            size = size.coerceIn(1, 50),     // Never let client request more than 50
            companyId = companyId,
            difficulty = difficulty,
            outcome = outcome,
            currentUser = null               // Will be injected in Chapter 5
        )
        return ResponseEntity.ok(result)
    }

    // GET /api/interviews/google-sde-1-bangalore-2024
    @GetMapping("/{slug}")
    fun getOne(@PathVariable slug: String): ResponseEntity<InterviewDetailDto> {
        val interview = interviewService.getBySlug(slug, currentUser = null)
        return ResponseEntity.ok(interview)
    }

    // POST /api/interviews
    @PostMapping
    fun submit(
        @RequestBody @Valid dto: CreateInterviewDto
    ): ResponseEntity<InterviewDetailDto> {
        val created = interviewService.submit(dto, currentUser = null)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    // GET /api/interviews/search?q=google+system+design
    @GetMapping("/search")
    fun search(
        @RequestParam q: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<SearchResultDto> {
        if (q.isBlank()) {
            return ResponseEntity.ok(SearchResultDto(emptyList(), 0, false))
        }
        val result = interviewService.search(q.trim(), page, size.coerceIn(1, 50))
        return ResponseEntity.ok(result)
    }
}
```

### Company Controller

```kotlin
// src/main/kotlin/com/example/hirestory/controller/CompanyController.kt

package com.example.hirestory.controller

import com.example.hirestory.dto.CompanyDto
import com.example.hirestory.dto.SliceResponse
import com.example.hirestory.service.CompanyService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/companies")
class CompanyController(private val companyService: CompanyService) {

    // GET /api/companies
    @GetMapping
    fun findAll(): ResponseEntity<List<CompanyDto>> {
        return ResponseEntity.ok(companyService.findAll())
    }

    // GET /api/companies/google
    @GetMapping("/{slug}")
    fun findOne(@PathVariable slug: String): ResponseEntity<CompanyDto> {
        return ResponseEntity.ok(companyService.findBySlug(slug))
    }

    // GET /api/companies/google/interviews
    @GetMapping("/{slug}/interviews")
    fun getInterviews(
        @PathVariable slug: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<SliceResponse<InterviewSummaryDto>> {
        return ResponseEntity.ok(
            companyService.getInterviews(slug, page, size.coerceIn(1, 50))
        )
    }
}
```

### Bookmark Controller

```kotlin
// src/main/kotlin/com/example/hirestory/controller/BookmarkController.kt

package com.example.hirestory.controller

import com.example.hirestory.dto.BookmarkDto
import com.example.hirestory.dto.SliceResponse
import com.example.hirestory.service.BookmarkService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/bookmarks")
class BookmarkController(private val bookmarkService: BookmarkService) {

    // GET /api/bookmarks?page=0&size=20
    @GetMapping
    fun getMyBookmarks(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
        // currentUser: injected in Chapter 5
    ): ResponseEntity<SliceResponse<InterviewSummaryDto>> {
        // Placeholder — will use currentUser from JWT in Chapter 5
        return ResponseEntity.ok(SliceResponse(emptyList(), false, page, size))
    }

    // POST /api/bookmarks/{interviewId}
    @PostMapping("/{interviewId}")
    fun bookmark(@PathVariable interviewId: Long): ResponseEntity<BookmarkDto> {
        val bookmark = bookmarkService.bookmark(interviewId, currentUserId = 1L) // placeholder
        return ResponseEntity.status(HttpStatus.CREATED).body(bookmark)
    }

    // DELETE /api/bookmarks/{interviewId}
    @DeleteMapping("/{interviewId}")
    fun removeBookmark(@PathVariable interviewId: Long): ResponseEntity<Void> {
        bookmarkService.removeBookmark(interviewId, currentUserId = 1L) // placeholder
        return ResponseEntity.noContent().build()
    }
}
```

### Admin Controller

```kotlin
// src/main/kotlin/com/example/hirestory/controller/AdminController.kt

package com.example.hirestory.controller

import com.example.hirestory.dto.*
import com.example.hirestory.service.AdminService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// This controller will be locked to ADMIN role in Chapter 5
@RestController
@RequestMapping("/admin")
class AdminController(private val adminService: AdminService) {

    // GET /api/admin/interviews/pending
    @GetMapping("/interviews/pending")
    fun getPendingInterviews(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<SliceResponse<InterviewDetailDto>> {
        return ResponseEntity.ok(adminService.getPendingInterviews(page, size))
    }

    // PUT /api/admin/interviews/{id}/approve
    @PutMapping("/interviews/{id}/approve")
    fun approve(@PathVariable id: Long): ResponseEntity<InterviewDetailDto> {
        return ResponseEntity.ok(adminService.approve(id))
    }

    // PUT /api/admin/interviews/{id}/reject
    @PutMapping("/interviews/{id}/reject")
    fun reject(
        @PathVariable id: Long,
        @RequestBody dto: RejectInterviewDto
    ): ResponseEntity<Void> {
        adminService.reject(id, dto.reason)
        return ResponseEntity.noContent().build()
    }

    // GET /api/admin/stats
    @GetMapping("/stats")
    fun getStats(): ResponseEntity<AdminStatsDto> {
        return ResponseEntity.ok(adminService.getStats())
    }

    // POST /api/admin/crawler/trigger
    @PostMapping("/crawler/trigger")
    fun triggerCrawler(): ResponseEntity<CrawlerTriggerResultDto> {
        return ResponseEntity.ok(adminService.triggerCrawler())
    }
}

data class RejectInterviewDto(val reason: String = "Does not meet quality standards")
```

---

## 4.7 Services — The Business Logic Layer

Your controllers are thin. All logic lives in services. Here are the services that power the controllers above:

```kotlin
// src/main/kotlin/com/example/hirestory/service/InterviewService.kt

package com.example.hirestory.service

import com.example.hirestory.dto.*
import com.example.hirestory.entity.*
import com.example.hirestory.exception.*
import com.example.hirestory.repository.*
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InterviewService(
    private val interviewRepository: InterviewRepository,
    private val companyRepository: CompanyRepository,
    private val tagRepository: TagRepository,
    private val bookmarkRepository: BookmarkRepository
) {

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
        val bookmarkedIds = currentUser?.let {
            bookmarkRepository.findBookmarkedInterviewIds(it, idSlice.content).toSet()
        } ?: emptySet()

        val dtos = idSlice.content.mapNotNull { id ->
            interviews.find { it.id == id }?.toSummaryDto()
        }

        return SliceResponse(dtos, idSlice.hasNext(), page, size)
    }

    @Transactional(readOnly = true)
    fun getBySlug(slug: String, currentUser: User?): InterviewDetailDto {
        val interview = interviewRepository.findPublishedBySlugWithDetails(slug)
            ?: throw ResourceNotFoundException("Interview", slug)

        val isBookmarked = currentUser != null &&
            bookmarkRepository.existsByUserAndInterview(currentUser, interview)

        return interview.toDetailDto(isBookmarked = isBookmarked)
    }

    @Transactional
    fun submit(dto: CreateInterviewDto, currentUser: User?): InterviewDetailDto {
        // Find or create the company
        val company = companyRepository.findByName(dto.company)
            ?: companyRepository.save(
                Company(
                    name = dto.company,
                    slug = dto.company.toSlug()
                )
            )

        // Handle tags — find existing, create new ones
        val tags = dto.tagNames.map { tagName ->
            tagRepository.findByName(tagName)
                ?: tagRepository.save(Tag(name = tagName))
        }

        // Build the slug from title — ensure uniqueness
        val baseSlug = dto.title.toSlug()
        val slug = ensureUniqueSlug(baseSlug)

        // Build the interview entity
        val interview = Interview(
            title = dto.title,
            headline = dto.headline,
            content = dto.content,
            role = dto.role,
            location = dto.location,
            experienceYears = dto.experienceYears,
            difficulty = dto.difficulty,
            outcome = dto.outcome,
            salaryLpa = dto.salaryLpa,
            slug = slug,
            company = company,
            user = if (dto.isAnonymous) null else currentUser,
            status = InterviewStatus.PENDING   // Always starts as pending
        )
        interview.tags.addAll(tags)

        val saved = interviewRepository.save(interview)

        // Save rounds
        val rounds = dto.rounds.mapIndexed { index, roundDto ->
            InterviewRound(
                interview = saved,
                roundNumber = roundDto.roundNumber,
                title = roundDto.title,
                questions = roundDto.questions,
                difficulty = roundDto.difficulty,
                notes = roundDto.notes
            )
        }
        saved.rounds.addAll(interviewRoundRepository.saveAll(rounds))
        saved.roundsCount = rounds.size
        interviewRepository.save(saved)

        return saved.toDetailDto()
    }

    @Transactional(readOnly = true)
    fun search(query: String, page: Int, size: Int): SearchResultDto {
        val offset = page * size
        val interviews = interviewRepository.search(query, size + 1, offset)
        val hasNext = interviews.size > size
        val content = if (hasNext) interviews.dropLast(1) else interviews

        return SearchResultDto(
            interviews = content.map { it.toSummaryDto() },
            total = interviewRepository.countSearchResults(query),
            hasNext = hasNext
        )
    }

    // Ensures slug is unique by appending a number if needed
    // "google-sde-1" → "google-sde-1-2" → "google-sde-1-3"
    private fun ensureUniqueSlug(baseSlug: String): String {
        if (!interviewRepository.existsBySlug(baseSlug)) return baseSlug
        var counter = 2
        while (interviewRepository.existsBySlug("$baseSlug-$counter")) counter++
        return "$baseSlug-$counter"
    }
}

// Extension to generate URL-friendly slug from any string
fun String.toSlug(): String = this
    .lowercase()
    .replace(Regex("[^a-z0-9\\s-]"), "")
    .trim()
    .replace(Regex("\\s+"), "-")
    .replace(Regex("-+"), "-")
```

```kotlin
// src/main/kotlin/com/example/hirestory/service/BookmarkService.kt

package com.example.hirestory.service

import com.example.hirestory.dto.BookmarkDto
import com.example.hirestory.entity.Bookmark
import com.example.hirestory.exception.DuplicateResourceException
import com.example.hirestory.exception.ResourceNotFoundException
import com.example.hirestory.repository.BookmarkRepository
import com.example.hirestory.repository.InterviewRepository
import com.example.hirestory.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookmarkService(
    private val bookmarkRepository: BookmarkRepository,
    private val interviewRepository: InterviewRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun bookmark(interviewId: Long, currentUserId: Long): BookmarkDto {
        val user = userRepository.findByIdOrNull(currentUserId)
            ?: throw ResourceNotFoundException("User", currentUserId)

        val interview = interviewRepository.findByIdOrNull(interviewId)
            ?: throw ResourceNotFoundException("Interview", interviewId)

        // Check for duplicate bookmark
        if (bookmarkRepository.existsByUserAndInterview(user, interview)) {
            throw DuplicateResourceException("Bookmark", "Interview $interviewId already bookmarked")
        }

        val bookmark = bookmarkRepository.save(Bookmark(user = user, interview = interview))
        return BookmarkDto(id = bookmark.id!!, interviewId = interviewId, createdAt = bookmark.createdAt.toString())
    }

    @Transactional
    fun removeBookmark(interviewId: Long, currentUserId: Long) {
        val user = userRepository.findByIdOrNull(currentUserId)
            ?: throw ResourceNotFoundException("User", currentUserId)

        val interview = interviewRepository.findByIdOrNull(interviewId)
            ?: throw ResourceNotFoundException("Interview", interviewId)

        val bookmark = bookmarkRepository.findByUserAndInterview(user, interview)
            ?: throw ResourceNotFoundException("Bookmark", "Interview $interviewId is not bookmarked")

        bookmarkRepository.delete(bookmark)
    }
}
```

```kotlin
// src/main/kotlin/com/example/hirestory/service/AdminService.kt

package com.example.hirestory.service

import com.example.hirestory.dto.*
import com.example.hirestory.entity.InterviewStatus
import com.example.hirestory.exception.ResourceNotFoundException
import com.example.hirestory.repository.*
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AdminService(
    private val interviewRepository: InterviewRepository,
    private val companyRepository: CompanyRepository,
    private val crawlJobRepository: CrawlJobRepository
) {

    @Transactional(readOnly = true)
    fun getPendingInterviews(page: Int, size: Int): SliceResponse<InterviewDetailDto> {
        val pageable = PageRequest.of(page, size)
        val slice = interviewRepository.findPendingForReview(pageable)
        val dtos = slice.content.map { it.toDetailDto() }
        return SliceResponse(dtos, slice.hasNext(), page, size)
    }

    @Transactional
    fun approve(id: Long): InterviewDetailDto {
        val interview = interviewRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Interview", id)

        interviewRepository.updateStatus(id, InterviewStatus.PUBLISHED, LocalDateTime.now())
        companyRepository.incrementInterviewCount(interview.company.id!!)

        // Reload to get updated state
        val updated = interviewRepository.findByIdOrNull(id)!!
        return updated.toDetailDto()
    }

    @Transactional
    fun reject(id: Long, reason: String) {
        if (!interviewRepository.existsById(id)) {
            throw ResourceNotFoundException("Interview", id)
        }
        interviewRepository.updateStatus(id, InterviewStatus.REJECTED, null)
    }

    @Transactional(readOnly = true)
    fun getStats(): AdminStatsDto {
        return AdminStatsDto(
            totalPublished = interviewRepository.countByStatus(InterviewStatus.PUBLISHED),
            totalPending = interviewRepository.countByStatus(InterviewStatus.PENDING),
            totalRejected = interviewRepository.countByStatus(InterviewStatus.REJECTED),
            totalCompanies = companyRepository.count(),
            pendingCrawlJobs = crawlJobRepository.countByStatus(CrawlStatus.PENDING)
        )
    }

    fun triggerCrawler(): CrawlerTriggerResultDto {
        // Covered in Chapter 10 — Web Crawler
        return CrawlerTriggerResultDto(queued = 0, message = "Crawler not implemented yet")
    }
}
```

---

## 4.8 Remaining DTOs

```kotlin
// src/main/kotlin/com/example/hirestory/dto/CommonDtos.kt

package com.example.hirestory.dto

// Generic paginated response
data class SliceResponse<T>(
    val content: List<T>,
    val hasNext: Boolean,
    val page: Int,
    val size: Int
)

// Search result
data class SearchResultDto(
    val interviews: List<InterviewSummaryDto>,
    val total: Long,
    val hasNext: Boolean
)

// Bookmark confirmation
data class BookmarkDto(
    val id: Long,
    val interviewId: Long,
    val createdAt: String
)

// Admin stats
data class AdminStatsDto(
    val totalPublished: Long,
    val totalPending: Long,
    val totalRejected: Long,
    val totalCompanies: Long,
    val pendingCrawlJobs: Long
)

// Crawler trigger result
data class CrawlerTriggerResultDto(
    val queued: Int,
    val message: String
)
```

---

## 4.9 Configuring CORS

Your Android app and Next.js web app are on different origins than your API. Without CORS configuration, browsers block web requests entirely. Configure it once:

```kotlin
// src/main/kotlin/com/example/hirestory/config/WebConfig.kt

package com.example.hirestory.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")              // Apply to all endpoints
            .allowedOrigins(
                "http://localhost:3000",        // Next.js dev
                "https://hirestory.com",        // Production web
                "https://www.hirestory.com"
            )
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)             // Allow cookies and auth headers
            .maxAge(3600)                       // Cache CORS preflight for 1 hour
    }
}
```

> **📝 Note:** Android apps do not use browsers, so they do not have CORS. CORS only applies to web browsers. Your Android app can call any URL freely. The CORS config above is only for your Next.js web app.

---

## 4.10 Testing Your API With IntelliJ HTTP Files

IntelliJ has a built-in REST client. Create `.http` files to test every endpoint. These become your living documentation.

```http
### application.http
### src/test/resources/requests.http

@baseUrl = http://localhost:8080/api

### Get all companies
GET {{baseUrl}}/companies
Accept: application/json

###

### Get company by slug
GET {{baseUrl}}/companies/google
Accept: application/json

###

### Get feed
GET {{baseUrl}}/interviews?page=0&size=20
Accept: application/json

###

### Get feed with filters
GET {{baseUrl}}/interviews?companyId=1&difficulty=HARD&outcome=OFFER
Accept: application/json

###

### Get interview by slug
GET {{baseUrl}}/interviews/google-sde-1-bangalore-2024
Accept: application/json

###

### Search
GET {{baseUrl}}/interviews/search?q=google+system+design
Accept: application/json

###

### Submit interview — valid
POST {{baseUrl}}/interviews
Content-Type: application/json

{
  "title": "Google SDE-1 Interview — Bangalore 2024",
  "headline": "Got an offer after 5 tough rounds. Focus on graphs and DP.",
  "content": "Applied through LinkedIn referral. The process took 3 weeks from first contact to offer. Round 1 was an online assessment with 2 medium-level problems. Round 2 was a technical phone screen...",
  "role": "Software Development Engineer - I",
  "location": "Bangalore",
  "experienceYears": 2,
  "difficulty": "HARD",
  "outcome": "OFFER",
  "salaryLpa": 28.0,
  "isAnonymous": false,
  "tagNames": ["DSA", "System Design"],
  "rounds": [
    {
      "roundNumber": 1,
      "title": "Online Assessment",
      "questions": "Two LeetCode medium problems. BFS and Dynamic Programming.",
      "difficulty": "MEDIUM"
    }
  ]
}

###

### Submit interview — validation failure test
POST {{baseUrl}}/interviews
Content-Type: application/json

{
  "title": "",
  "content": "too short",
  "experienceYears": -5
}

###

### Approve interview (admin)
PUT {{baseUrl}}/admin/interviews/1/approve

###

### Admin stats
GET {{baseUrl}}/admin/stats
```

---

## 4.11 Common Mistakes in Chapter 4

### Mistake 1 — Forgetting `@field:` prefix in Kotlin validation

```kotlin
// ❌ Validation silently does nothing — @NotBlank targets the property getter
data class CreateInterviewDto(
    @NotBlank val title: String
)

// ✅ @field: targets the backing field — validation runs correctly
data class CreateInterviewDto(
    @field:NotBlank val title: String
)
```

### Mistake 2 — Forgetting `@Valid` on @RequestBody

```kotlin
// ❌ DTO fields have @field:NotBlank but validation never runs
fun submit(@RequestBody dto: CreateInterviewDto) { }

// ✅ @Valid activates Bean Validation
fun submit(@RequestBody @Valid dto: CreateInterviewDto) { }
```

### Mistake 3 — Putting business logic in the controller

```kotlin
// ❌ Controller doing service work
@PostMapping
fun submit(@RequestBody @Valid dto: CreateInterviewDto): ResponseEntity<InterviewDetailDto> {
    val company = companyRepository.findByName(dto.company)   // ← wrong layer
    val interview = Interview(title = dto.title, company = company, ...)
    interviewRepository.save(interview)                        // ← wrong layer
    return ResponseEntity.status(HttpStatus.CREATED).body(interview.toDetailDto())
}

// ✅ Controller only orchestrates
@PostMapping
fun submit(@RequestBody @Valid dto: CreateInterviewDto): ResponseEntity<InterviewDetailDto> {
    return ResponseEntity.status(HttpStatus.CREATED).body(interviewService.submit(dto, null))
}
```

### Mistake 4 — Returning 200 for creation

```kotlin
// ❌ 200 OK means "I fetched something" — not "I created something"
return ResponseEntity.ok(created)

// ✅ 201 Created specifically means "new resource was created"
return ResponseEntity.status(HttpStatus.CREATED).body(created)
```

### Mistake 5 — Exposing internal error details in production

```kotlin
// ❌ Exposes your internal system details to the client
@ExceptionHandler(Exception::class)
fun handleGeneric(ex: Exception): ResponseEntity<ErrorResponse> {
    return ResponseEntity.status(500).body(
        ErrorResponse(message = ex.message!!)  // Stack trace details visible to client
    )
}

// ✅ Log the details internally, return a generic message externally
@ExceptionHandler(Exception::class)
fun handleGeneric(ex: Exception): ResponseEntity<ErrorResponse> {
    log.error("Unhandled exception: {}", ex.message, ex)   // Full details in your logs
    return ResponseEntity.status(500).body(
        ErrorResponse(message = "An unexpected error occurred. Please try again later.")
    )
}
```

### Mistake 6 — Not validating nested objects with `@Valid`

```kotlin
// ❌ Rounds are never validated even though CreateRoundDto has @field: annotations
data class CreateInterviewDto(
    val rounds: List<CreateRoundDto>    // Missing @field:Valid
)

// ✅ @field:Valid cascades validation into the list
data class CreateInterviewDto(
    @field:Valid
    val rounds: List<CreateRoundDto>
)
```

---

## 4.12 HireStory Connection — What You Built in Chapter 4

By the end of Chapter 4, HireStory has a complete, professional API layer:

- All request DTOs with field-level validation using correct `@field:` prefix
- A `GlobalExceptionHandler` that catches every exception type and returns consistent JSON errors to every client
- Custom exceptions: `ResourceNotFoundException`, `DuplicateResourceException`, `ForbiddenOperationException`, `PaywallException`, `BusinessRuleException`
- `InterviewController` — feed, detail, submit, search
- `CompanyController` — list and detail
- `BookmarkController` — add and remove
- `AdminController` — review queue, approve, reject, stats
- Complete service layer with proper `@Transactional` usage
- CORS configured for web clients
- IntelliJ `.http` file to test every endpoint

The `currentUser` parameter in controllers is `null` for now. Chapter 5 (Spring Security + JWT) is where you wire in the actual logged-in user. That one change — replacing `null` with the real user from the JWT token — activates every user-specific feature: personalised feed, bookmarks, read counting, referrals.

---

## 4.13 Chapter Project — Build It Before You Move On

### What to build

Complete the full controller + service + error handling layer for companies and interviews.

**Step 1 — Add GlobalExceptionHandler**

Copy the handler from Section 4.4. Then test it:

- Hit `GET /api/interviews/does-not-exist` — verify you get a clean 404 JSON
- Hit `POST /api/interviews` with an empty body — verify you get a 400 with field errors
- Hit `GET /api/interviews?page=abc` — verify you get a 400 type mismatch error

**Step 2 — Add validation to CreateInterviewDto**

Write the full DTO with all `@field:` annotations from Section 4.3.

**Step 3 — Test validation**

Send the failing request from Section 4.10. Verify the response has `fieldErrors` listing exactly which fields failed.

**Step 4 — Add the search endpoint**

Implement `GET /api/interviews/search?q=google`. For now, if the full-text search column is not set up, return an empty list.

**Step 5 — Add the admin approve endpoint**

`PUT /api/admin/interviews/{id}/approve` — change status to PUBLISHED.

**Step 6 — Verify correct status codes**

- Submit interview → must return 201, not 200
- Delete bookmark → must return 204, not 200
- Interview not found → must return 404, not 500

### Checkpoint questions — answer before moving on

1. A user sends `{ "experienceYears": "five" }` in the request body. Which exception does Spring throw and which handler in your `GlobalExceptionHandler` catches it?
    
2. Your `GlobalExceptionHandler` has a handler for `ResourceNotFoundException` and a catch-all for `Exception`. A `ResourceNotFoundException` is thrown. Which handler runs and why?
    
3. You add a new custom exception `RateLimitException`. What two things do you need to do to make `GlobalExceptionHandler` handle it?
    
4. A controller method returns `ResponseEntity.ok(null)`. What does the client receive and what status code?
    
5. Your DTO has `@field:Size(min = 100)` on `content`. A user sends content that is exactly 99 characters. What happens — where is this caught, and what does the response look like?
    

---

_Chapter 5 → Spring Security and JWT — Locking Down Your API_

---

> **Book Progress:** Chapter 4 of 15 complete. Chapters ahead: Spring Security · JWT/Clerk · Redis · RabbitMQ · Spring AI · Jsoup · Scheduler · Testing · Deployment