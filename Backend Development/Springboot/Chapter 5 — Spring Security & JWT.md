# Backend Engineering with Spring Boot & Kotlin

## The HireStory Builder's Guide

---

# Chapter 5 — Spring Security & JWT

### _Locking down your API — who you are, what you can do, and how Spring knows_

---

## 5.1 The Problem This Chapter Solves

Right now your API has a serious problem. Anyone can call any endpoint.

```http
DELETE /api/admin/interviews/1/reject   ← Anyone can reject interviews
POST   /api/bookmarks/5                 ← Anyone can bookmark as anyone
GET    /api/admin/stats                 ← Anyone can see your internal data
```

You need to answer three questions for every request:

- **Authentication** — Who are you? Are you even a real user?
- **Authorisation** — Even if you are real, are you allowed to do this?
- **Identity** — Once I know who you are, how do I use that in my code?

Spring Security answers all three. It is the most powerful and the most confusing part of Spring Boot. This chapter explains it clearly, from how it works internally to exactly how you wire Clerk's JWT tokens into it.

---

## 5.2 How Spring Security Actually Works — The Filter Chain

Spring Security is not magic. It is a series of filters that every HTTP request passes through before it reaches your controller.

```
Incoming HTTP Request
        ↓
┌─────────────────────────────────────────┐
│           Spring Security               │
│         Filter Chain                    │
│                                         │
│  Filter 1: CorsFilter                   │
│        ↓                                │
│  Filter 2: JwtAuthenticationFilter  ←── YOUR custom filter
│        ↓  (reads token, sets user)      │
│  Filter 3: UsernamePasswordFilter       │
│        ↓  (not used — Clerk handles it) │
│  Filter 4: ExceptionTranslationFilter   │
│        ↓  (converts auth errors → 401)  │
│  Filter 5: AuthorizationFilter          │
│        ↓  (checks permissions)          │
└─────────────────────────────────────────┘
        ↓
  DispatcherServlet
        ↓
  Your Controller
```

Each filter decides:

- Pass the request to the next filter (continue)
- Stop the request and return a response (reject with 401 or 403)

Your job is to write one custom filter — `JwtAuthenticationFilter` — that reads the JWT token, validates it, and tells Spring Security who this user is. Everything else Spring Security handles for you.

### The Security Context

Spring Security stores the authenticated user in a thread-local object called `SecurityContextHolder`. Think of it as a request-scoped variable that any part of your code can read.

```
Request arrives with token
        ↓
JwtAuthenticationFilter validates token
        ↓
Filter puts user into SecurityContextHolder
        ↓
Request reaches your controller
        ↓
Controller reads user from SecurityContextHolder
        ↓
Request finishes — SecurityContextHolder is cleared
```

This is how `currentUser` stops being `null`. By the time your controller method runs, the filter has already put the authenticated user in the security context. Spring injects it automatically.

---

## 5.3 What JWT Is — The Token Your App Uses

You know the concept. Let us be precise about how it works for HireStory.

### JWT Structure

A JWT token is three Base64-encoded strings joined by dots:

```
eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyXzEyMyIsImVtYWlsIjoidGVzdEBleGFtcGxlLmNvbSJ9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
└─────────────────┘  └────────────────────────────────────────────────┘  └──────────────────────────────────────────────────────────┘
      Header                              Payload                                                  Signature
  (algorithm used)                   (the actual data)                              (cryptographic proof it was not tampered with)
```

**Header** — algorithm used to sign the token:

```json
{ "alg": "RS256", "typ": "JWT" }
```

**Payload** — the data (called claims):

```json
{
  "sub": "user_2abc123def456",   ← Clerk user ID — your main identifier
  "email": "test@example.com",
  "name": "Arjun Sharma",
  "iat": 1705312200,             ← Issued at (Unix timestamp)
  "exp": 1705315800,             ← Expires at (Unix timestamp)
  "iss": "https://your-app.clerk.accounts.dev"  ← Issuer — who created this token
}
```

**Signature** — proves the token was created by Clerk and not tampered with. Clerk signs it using their private key. You verify it using their public key (which Clerk publishes at a JWKS URL).

### Why Signature Verification Matters

The payload is just Base64 encoded — not encrypted. Anyone can decode it. The signature is what you cannot fake — you cannot produce a valid signature without Clerk's private key. So when you verify the signature using Clerk's public key and it matches, you know:

1. This token was definitely created by Clerk
2. The payload data (user ID, email) was not tampered with
3. You can trust the `sub` claim as the real user's Clerk ID

---

## 5.4 How Clerk Fits Into This

Clerk handles everything on the client side:

- Login form and Google OAuth
- Creating accounts
- Issuing JWT tokens after successful login
- Refreshing expired tokens

Your Spring Boot backend handles:

- Receiving the JWT token with each request
- Validating the token signature against Clerk's public key
- Extracting the user's Clerk ID from the token
- Looking up the user in your database by Clerk ID
- Making that user available to your controllers

Clerk publishes their public key at a JWKS URL. JWKS stands for JSON Web Key Set — it is a standard URL format that returns the public keys used to verify tokens.

```
Your app.yml:
hirestory:
  clerk:
    jwks-url: https://your-app.clerk.accounts.dev/.well-known/jwks.json
```

You fetch this URL once on startup, cache the keys, and use them to verify every incoming token.

---

## 5.5 Dependencies — What You Need

Add these to `build.gradle.kts`:

```kotlin
dependencies {
    // Spring Security — already in your dependencies from Chapter 1
    implementation("org.springframework.boot:spring-boot-starter-security")

    // JWT library — reads, parses, and validates JWT tokens
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Spring Security test support
    testImplementation("org.springframework.security:spring-security-test")
}
```

---

## 5.6 Custom Properties — Reading Clerk Config From YML

You already have the Clerk properties in your `application.yml`. Now create a type-safe class to read them:

```kotlin
// src/main/kotlin/com/example/hirestory/config/HireStoryProperties.kt

package com.example.hirestory.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue

@ConfigurationProperties(prefix = "hirestory")
data class HireStoryProperties(
    val clerk: ClerkProperties,
    val freeTier: FreeTierProperties
) {
    data class ClerkProperties(
        val jwksUrl: String,
        val webhookSecret: String
    )

    data class FreeTierProperties(
        @DefaultValue("25") val monthlyReadLimit: Int
    )
}
```

Register it in your application class:

```kotlin
// src/main/kotlin/com/example/hirestory/HireStoryApplication.kt

@SpringBootApplication
@EnableConfigurationProperties(HireStoryProperties::class)
class HireStoryApplication

fun main(args: Array<String>) {
    runApplication<HireStoryApplication>()
}
```

---

## 5.7 The JWT Service — Validating Tokens

This service does one job: take a raw JWT token string and return the claims inside it if the token is valid.

```kotlin
// src/main/kotlin/com/example/hirestory/security/JwtService.kt

package com.example.hirestory.security

import com.example.hirestory.config.HireStoryProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.JwkSet
import io.jsonwebtoken.security.Jwks
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URI
import java.security.PublicKey

@Service
class JwtService(private val properties: HireStoryProperties) {

    private val log = LoggerFactory.getLogger(JwtService::class.java)

    // Cache the public keys — fetched once on first use, then reused
    // Clerk rotates keys rarely — caching is safe and much more efficient
    private val publicKeys: List<PublicKey> by lazy {
        fetchPublicKeys()
    }

    // Validate the token and return its claims
    // Returns null if the token is invalid, expired, or cannot be verified
    fun validateAndExtractClaims(token: String): Claims? {
        return try {
            // Try each public key — Clerk may have multiple active keys
            publicKeys.firstNotNullOfOrNull { key ->
                tryValidateWithKey(token, key)
            }
        } catch (e: Exception) {
            log.debug("JWT validation failed: {}", e.message)
            null
        }
    }

    private fun tryValidateWithKey(token: String, key: PublicKey): Claims? {
        return try {
            Jwts.parser()
                .verifyWith(key)       // Set the public key for signature verification
                .build()
                .parseSignedClaims(token)
                .payload              // Returns Claims if valid, throws if invalid/expired
        } catch (e: Exception) {
            null                      // This key did not work — try next key
        }
    }

    // Extract the Clerk user ID from validated claims
    // Clerk puts the user ID in the "sub" (subject) claim
    fun extractClerkId(claims: Claims): String {
        return claims.subject
            ?: throw IllegalArgumentException("JWT token has no subject claim")
    }

    // Extract email from claims (Clerk includes this)
    fun extractEmail(claims: Claims): String? {
        return claims["email"] as? String
    }

    // Fetch public keys from Clerk's JWKS endpoint
    private fun fetchPublicKeys(): List<PublicKey> {
        return try {
            log.info("Fetching Clerk public keys from {}", properties.clerk.jwksUrl)
            val jwksUrl = URI(properties.clerk.jwksUrl).toURL()
            val jwkSet = JwkSet.load(jwksUrl.openStream())

            jwkSet.keys
                .mapNotNull { it.toKey() as? PublicKey }
                .also { log.info("Loaded {} Clerk public keys", it.size) }
        } catch (e: Exception) {
            log.error("Failed to fetch Clerk public keys: {}", e.message, e)
            throw IllegalStateException("Cannot start without Clerk public keys", e)
        }
    }
}
```

---

## 5.8 The JWT Authentication Filter

This is the core of your security. It runs before every request.

```kotlin
// src/main/kotlin/com/example/hirestory/security/JwtAuthenticationFilter.kt

package com.example.hirestory.security

import com.example.hirestory.repository.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

// OncePerRequestFilter guarantees this filter runs exactly once per request
// Even if the request is forwarded internally, it still only runs once
@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain  // The rest of the filter chain
    ) {
        // Step 1: Extract the token from the Authorization header
        val token = extractToken(request)

        if (token == null) {
            // No token — this is either a public endpoint or an unauthenticated request
            // Let the request continue — SecurityConfig will reject it if the endpoint requires auth
            filterChain.doFilter(request, response)
            return
        }

        // Step 2: Validate the token
        val claims = jwtService.validateAndExtractClaims(token)

        if (claims == null) {
            // Token exists but is invalid or expired
            // Continue without setting authentication — SecurityConfig will reject if needed
            log.debug("Invalid or expired JWT token from {}", request.remoteAddr)
            filterChain.doFilter(request, response)
            return
        }

        // Step 3: Extract Clerk ID from claims
        val clerkId = jwtService.extractClerkId(claims)

        // Step 4: Look up the user in your database
        val user = userRepository.findByClerkId(clerkId)

        if (user == null) {
            // Token is valid but user does not exist in your database yet
            // This happens if the Clerk webhook has not fired yet (race condition on signup)
            // Continue without authentication — the webhook will create the user soon
            log.warn("Valid Clerk token but user not found in DB: {}", clerkId)
            filterChain.doFilter(request, response)
            return
        }

        // Step 5: Determine the user's roles/authorities
        val authorities = mutableListOf(SimpleGrantedAuthority("ROLE_USER"))
        if (user.isPremium) authorities.add(SimpleGrantedAuthority("ROLE_PREMIUM"))
        // Admin role will be managed separately (e.g., a flag in the database)

        // Step 6: Create an Authentication object and put it in the Security Context
        // This is what tells Spring Security "this request is authenticated as this user"
        val authentication = UsernamePasswordAuthenticationToken(
            user,           // principal — the actual User object (accessible in controllers)
            null,           // credentials — null because we use tokens, not passwords
            authorities     // what this user is allowed to do
        )
        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

        // Put in SecurityContextHolder — now any code in this request can access the user
        SecurityContextHolder.getContext().authentication = authentication

        // Step 7: Continue to the next filter
        filterChain.doFilter(request, response)
    }

    // Extract the raw token string from "Authorization: Bearer <token>"
    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        if (!header.startsWith("Bearer ")) return null
        val token = header.removePrefix("Bearer ").trim()
        return if (token.isNotEmpty()) token else null
    }
}
```

---

## 5.9 SecurityConfig — Defining the Rules

This is where you define which endpoints are public and which require authentication.

```kotlin
// src/main/kotlin/com/example/hirestory/config/SecurityConfig.kt

package com.example.hirestory.config

import com.example.hirestory.security.JwtAuthenticationFilter
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import java.time.LocalDateTime

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // Enables @PreAuthorize on controller methods
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val objectMapper: ObjectMapper
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // ── Disable defaults we do not use ──────────────────────────
            .csrf { it.disable() }
            // CSRF protection is for browser-based form submissions
            // REST APIs using JWT tokens do not need CSRF protection
            // Reason: CSRF attacks rely on the browser automatically sending cookies
            // JWT tokens in Authorization headers are never sent automatically

            .formLogin { it.disable() }
            // We do not have a login form — Clerk handles login

            .httpBasic { it.disable() }
            // No username/password in headers — JWT only

            // ── Stateless session ────────────────────────────────────────
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                // Never create or use HTTP sessions
                // Each request is authenticated independently via its JWT token
                // This is essential for a REST API — sessions belong to web apps
            }

            // ── Add your JWT filter ──────────────────────────────────────
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java
                // Run your JWT filter BEFORE Spring's default auth filter
            )

            // ── Define access rules ──────────────────────────────────────
            .authorizeHttpRequests { auth ->
                auth
                    // ── Completely public — no token needed ──────────────
                    .requestMatchers(HttpMethod.GET, "/interviews").permitAll()
                    .requestMatchers(HttpMethod.GET, "/interviews/{slug}").permitAll()
                    .requestMatchers(HttpMethod.GET, "/interviews/search").permitAll()
                    .requestMatchers(HttpMethod.GET, "/companies").permitAll()
                    .requestMatchers(HttpMethod.GET, "/companies/{slug}").permitAll()
                    .requestMatchers(HttpMethod.GET, "/companies/{slug}/interviews").permitAll()
                    .requestMatchers("/actuator/health").permitAll()

                    // ── Clerk webhook — called by Clerk, not by users ────
                    // Must be public — Clerk does not send user JWT tokens
                    .requestMatchers(HttpMethod.POST, "/webhooks/clerk").permitAll()

                    // ── Admin only ───────────────────────────────────────
                    .requestMatchers("/admin/**").hasRole("ADMIN")

                    // ── Everything else requires authentication ──────────
                    // This covers: POST /interviews, /bookmarks/**, /profile/**, etc.
                    .anyRequest().authenticated()
            }

            // ── Custom error responses for auth failures ─────────────────
            .exceptionHandling { exceptions ->
                exceptions
                    // 401 — No token or invalid token on a protected endpoint
                    .authenticationEntryPoint { request, response, _ ->
                        sendError(response, 401, "Unauthorized",
                            "Authentication required. Include a valid Bearer token.", request)
                    }
                    // 403 — Valid token but insufficient permissions (e.g. non-admin on /admin)
                    .accessDeniedHandler { request, response, _ ->
                        sendError(response, 403, "Forbidden",
                            "You do not have permission to access this resource.", request)
                    }
            }

        return http.build()
    }

    // Writes a consistent JSON error response for auth failures
    // These happen before your GlobalExceptionHandler runs
    // (The filter chain rejects before reaching your controller)
    private fun sendError(
        response: HttpServletResponse,
        status: Int,
        error: String,
        message: String,
        request: HttpServletRequest
    ) {
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE

        val body = mapOf(
            "status" to status,
            "error" to error,
            "message" to message,
            "path" to request.requestURI,
            "timestamp" to LocalDateTime.now().toString()
        )
        response.writer.write(objectMapper.writeValueAsString(body))
    }
}
```

> **⚠️ Why is CSRF disabled safe here?** CSRF attacks work by tricking a browser into sending a request using cookies it already has. Your API uses JWT tokens in the Authorization header — browsers never send headers automatically. So CSRF is not a threat. Disabling it is correct for JWT-based REST APIs.

---

## 5.10 Getting the Current User in Controllers

Now the `currentUser = null` placeholder from Chapter 4 becomes real. Spring Security provides `@AuthenticationPrincipal` to inject the authenticated user directly into your controller method.

```kotlin
// How Spring Security injects the current user

@RestController
@RequestMapping("/interviews")
class InterviewController(private val interviewService: InterviewService) {

    @PostMapping
    fun submit(
        @RequestBody @Valid dto: CreateInterviewDto,
        @AuthenticationPrincipal currentUser: User?
        // ↑ Spring reads this from SecurityContextHolder automatically
        // It is the same User object your JwtAuthenticationFilter put there
        // It is nullable because public endpoints have no authenticated user
    ): ResponseEntity<InterviewDetailDto> {
        val created = interviewService.submit(dto, currentUser)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @GetMapping("/{slug}")
    fun getOne(
        @PathVariable slug: String,
        @AuthenticationPrincipal currentUser: User?
        // Nullable — public endpoint, user may or may not be logged in
        // If logged in: isBookmarked flag is set correctly
        // If not: isBookmarked is always false
    ): ResponseEntity<InterviewDetailDto> {
        return ResponseEntity.ok(interviewService.getBySlug(slug, currentUser))
    }
}
```

```kotlin
@RestController
@RequestMapping("/bookmarks")
class BookmarkController(private val bookmarkService: BookmarkService) {

    @PostMapping("/{interviewId}")
    fun bookmark(
        @PathVariable interviewId: Long,
        @AuthenticationPrincipal currentUser: User
        // Not nullable — this endpoint requires authentication (@anyRequest().authenticated())
        // If no token is provided, Spring rejects at the filter level
        // Your method is only called when there IS an authenticated user
    ): ResponseEntity<BookmarkDto> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(bookmarkService.bookmark(interviewId, currentUser))
    }

    @DeleteMapping("/{interviewId}")
    fun removeBookmark(
        @PathVariable interviewId: Long,
        @AuthenticationPrincipal currentUser: User
    ): ResponseEntity<Void> {
        bookmarkService.removeBookmark(interviewId, currentUser)
        return ResponseEntity.noContent().build()
    }

    @GetMapping
    fun getMyBookmarks(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal currentUser: User
    ): ResponseEntity<SliceResponse<InterviewSummaryDto>> {
        return ResponseEntity.ok(bookmarkService.getMyBookmarks(currentUser, page, size))
    }
}
```

### Updating Your Services

Update `BookmarkService` to accept `User` directly instead of `currentUserId: Long`:

```kotlin
@Service
class BookmarkService(
    private val bookmarkRepository: BookmarkRepository,
    private val interviewRepository: InterviewRepository
) {

    @Transactional
    fun bookmark(interviewId: Long, currentUser: User): BookmarkDto {
        // No more userRepository.findByIdOrNull — user is already loaded
        val interview = interviewRepository.findByIdOrNull(interviewId)
            ?: throw ResourceNotFoundException("Interview", interviewId)

        if (bookmarkRepository.existsByUserAndInterview(currentUser, interview)) {
            throw DuplicateResourceException("Bookmark", "Already bookmarked")
        }

        val bookmark = bookmarkRepository.save(
            Bookmark(user = currentUser, interview = interview)
        )
        return BookmarkDto(
            id = bookmark.id!!,
            interviewId = interviewId,
            createdAt = bookmark.createdAt.toString()
        )
    }

    @Transactional
    fun removeBookmark(interviewId: Long, currentUser: User) {
        val interview = interviewRepository.findByIdOrNull(interviewId)
            ?: throw ResourceNotFoundException("Interview", interviewId)

        val bookmark = bookmarkRepository.findByUserAndInterview(currentUser, interview)
            ?: throw ResourceNotFoundException("Bookmark", interviewId)

        bookmarkRepository.delete(bookmark)
    }

    @Transactional(readOnly = true)
    fun getMyBookmarks(
        currentUser: User,
        page: Int,
        size: Int
    ): SliceResponse<InterviewSummaryDto> {
        val pageable = PageRequest.of(page, size)
        val slice = bookmarkRepository.findByUserWithDetails(currentUser, pageable)
        val dtos = slice.content.map { it.interview.toSummaryDto() }
        return SliceResponse(dtos, slice.hasNext(), page, size)
    }
}
```

---

## 5.11 Method-Level Security — @PreAuthorize

For admin endpoints, you have two options. The `SecurityConfig` approach (`/admin/**` requires `ROLE_ADMIN`) is the simpler option. But what if you need finer control — like "a user can only delete their own comment"?

That is where `@PreAuthorize` comes in:

```kotlin
// Enable method security — already done in SecurityConfig with @EnableMethodSecurity

@RestController
@RequestMapping("/admin/interviews")
class AdminInterviewController(private val adminService: AdminService) {

    // Only ADMIN role can call this — checked before the method runs
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/approve")
    fun approve(@PathVariable id: Long): ResponseEntity<InterviewDetailDto> {
        return ResponseEntity.ok(adminService.approve(id))
    }
}

@RestController
@RequestMapping("/comments")
class CommentController(private val commentService: CommentService) {

    // The user can only delete their own comment
    // #currentUser references the @AuthenticationPrincipal parameter
    @PreAuthorize("#currentUser.id == @commentRepository.findById(#id).get().user.id")
    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
        @AuthenticationPrincipal currentUser: User
    ): ResponseEntity<Void> {
        commentService.delete(id, currentUser)
        return ResponseEntity.noContent().build()
    }
}
```

For HireStory, the simpler approach is better for most cases:

- Public endpoints in `SecurityConfig.permitAll()`
- Admin endpoints in `SecurityConfig.hasRole("ADMIN")`
- User ownership checks in the service layer (throw `ForbiddenOperationException`)

---

## 5.12 Clerk Webhook — Creating Users on Signup

When a user signs up through your app, Clerk sends a POST request to your backend with the user's data. This is how the user gets created in your database.

### The Flow

```
User signs up on your app (Clerk handles the UI)
        ↓
Clerk creates the user in their system
        ↓
Clerk sends POST /api/webhooks/clerk to your backend
Body: { "type": "user.created", "data": { "id": "user_abc", "email_addresses": [...] } }
        ↓
Your webhook endpoint creates the user in your database
        ↓
User can now make authenticated requests
```

### Verifying the Webhook Signature

Anyone could send a fake POST to your webhook endpoint. Clerk signs every webhook with an HMAC signature. You verify it.

Add the Svix library (Clerk uses Svix for webhooks):

```kotlin
// build.gradle.kts
implementation("com.svix:svix:1.38.0")
```

### The Webhook Controller

```kotlin
// src/main/kotlin/com/example/hirestory/controller/WebhookController.kt

package com.example.hirestory.controller

import com.example.hirestory.config.HireStoryProperties
import com.example.hirestory.service.UserService
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.svix.webhooks.Webhook
import com.svix.webhooks.WebhookVerificationException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/webhooks")
class WebhookController(
    private val properties: HireStoryProperties,
    private val userService: UserService,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(WebhookController::class.java)

    // This endpoint must be in SecurityConfig.permitAll()
    // Clerk calls this without a user JWT token
    @PostMapping("/clerk")
    fun handleClerkWebhook(
        @RequestBody rawBody: String,
        request: HttpServletRequest
    ): ResponseEntity<String> {

        // Step 1: Verify the webhook signature
        if (!verifySignature(rawBody, request)) {
            log.warn("Clerk webhook signature verification failed")
            return ResponseEntity.status(401).body("Invalid signature")
        }

        // Step 2: Parse the event
        val event = objectMapper.readTree(rawBody)
        val eventType = event.get("type").asText()
        val data = event.get("data")

        log.info("Received Clerk webhook: {}", eventType)

        // Step 3: Handle the event type
        when (eventType) {
            "user.created" -> handleUserCreated(data)
            "user.updated" -> handleUserUpdated(data)
            "user.deleted" -> handleUserDeleted(data)
            else -> log.debug("Unhandled Clerk event type: {}", eventType)
        }

        // Always return 200 — Clerk retries if you return non-2xx
        return ResponseEntity.ok("OK")
    }

    private fun handleUserCreated(data: JsonNode) {
        val clerkId = data.get("id").asText()
        val email = data.get("email_addresses")
            .firstOrNull { it.get("id").asText() == data.get("primary_email_address_id").asText() }
            ?.get("email_address")?.asText()
            ?: return

        val name = buildString {
            val firstName = data.get("first_name")?.asText() ?: ""
            val lastName = data.get("last_name")?.asText() ?: ""
            append("$firstName $lastName".trim())
        }.ifBlank { email.substringBefore("@") }

        val avatarUrl = data.get("image_url")?.asText()

        userService.createFromClerk(clerkId, email, name, avatarUrl)
        log.info("Created user from Clerk webhook: {}", clerkId)
    }

    private fun handleUserUpdated(data: JsonNode) {
        val clerkId = data.get("id").asText()
        val name = buildString {
            append("${data.get("first_name")?.asText() ?: ""} ${data.get("last_name")?.asText() ?: ""}".trim())
        }
        val avatarUrl = data.get("image_url")?.asText()
        userService.updateFromClerk(clerkId, name, avatarUrl)
    }

    private fun handleUserDeleted(data: JsonNode) {
        val clerkId = data.get("id").asText()
        userService.deactivateByClerkId(clerkId)
    }

    private fun verifySignature(body: String, request: HttpServletRequest): Boolean {
        return try {
            val webhook = Webhook(properties.clerk.webhookSecret)
            val headers = mapOf(
                "svix-id" to (request.getHeader("svix-id") ?: ""),
                "svix-timestamp" to (request.getHeader("svix-timestamp") ?: ""),
                "svix-signature" to (request.getHeader("svix-signature") ?: "")
            )
            webhook.verify(body, headers)
            true
        } catch (e: WebhookVerificationException) {
            false
        }
    }
}
```

### The User Service

```kotlin
// src/main/kotlin/com/example/hirestory/service/UserService.kt

package com.example.hirestory.service

import com.example.hirestory.entity.User
import com.example.hirestory.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(private val userRepository: UserRepository) {

    private val log = LoggerFactory.getLogger(UserService::class.java)

    @Transactional
    fun createFromClerk(
        clerkId: String,
        email: String,
        name: String,
        avatarUrl: String?
    ): User {
        // Guard against duplicate webhook delivery — Clerk guarantees at-least-once
        if (userRepository.existsByClerkId(clerkId)) {
            log.warn("User already exists for clerkId: {} — skipping", clerkId)
            return userRepository.findByClerkId(clerkId)!!
        }

        val user = userRepository.save(
            User(
                clerkId = clerkId,
                email = email,
                name = name,
                avatarUrl = avatarUrl,
                referralCode = generateReferralCode()
            )
        )
        log.info("Created new user: {} ({})", user.name, user.id)
        return user
    }

    @Transactional
    fun updateFromClerk(clerkId: String, name: String, avatarUrl: String?) {
        val user = userRepository.findByClerkId(clerkId) ?: return
        user.name = name
        user.avatarUrl = avatarUrl
        userRepository.save(user)
    }

    @Transactional
    fun deactivateByClerkId(clerkId: String) {
        // Soft delete — do not actually delete the user
        // Their interviews, bookmarks, and comments should remain
        val user = userRepository.findByClerkId(clerkId) ?: return
        log.info("Deactivating user: {}", clerkId)
        // Add an isActive field if you want — for now just log
    }

    // Generates a unique 8-character referral code
    // Retries if the code already exists (collision is astronomically rare)
    private fun generateReferralCode(): String {
        var code: String
        do {
            code = UUID.randomUUID().toString()
                .replace("-", "")
                .take(8)
                .uppercase()
        } while (userRepository.existsByReferralCode(code))
        return code
    }
}
```

---

## 5.13 The Read Count — Free Tier Enforcement

HireStory's free tier allows 25 interview reads per month. This logic lives in the service, triggered when someone reads an interview detail.

```kotlin
// src/main/kotlin/com/example/hirestory/service/ReadTrackingService.kt

package com.example.hirestory.service

import com.example.hirestory.config.HireStoryProperties
import com.example.hirestory.entity.Interview
import com.example.hirestory.entity.ReadHistory
import com.example.hirestory.entity.User
import com.example.hirestory.exception.PaywallException
import com.example.hirestory.repository.ReadHistoryRepository
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class ReadTrackingService(
    private val readHistoryRepository: ReadHistoryRepository,
    private val redisTemplate: StringRedisTemplate,
    private val properties: HireStoryProperties
) {

    // Call this when a logged-in user reads an interview detail
    @Transactional
    fun trackRead(user: User, interview: Interview) {
        // Step 1: Has this user already read this interview?
        // If yes — do not count it again, just let them read
        if (readHistoryRepository.existsByUserAndInterview(user, interview)) {
            return
        }

        // Step 2: Not read before — check if free tier limit is reached
        if (!user.isPremium) {
            val currentCount = getMonthlyReadCount(user)
            if (currentCount >= properties.freeTier.monthlyReadLimit) {
                throw PaywallException(
                    readsUsed = currentCount,
                    limit = properties.freeTier.monthlyReadLimit
                )
            }
        }

        // Step 3: Record the read in database (permanent history)
        readHistoryRepository.save(ReadHistory(user = user, interview = interview))

        // Step 4: Increment the Redis counter (fast monthly count)
        val key = readCountKey(user)
        redisTemplate.opsForValue().increment(key)
    }

    // Get the current month's read count from Redis
    fun getMonthlyReadCount(user: User): Int {
        val key = readCountKey(user)
        return redisTemplate.opsForValue().get(key)?.toIntOrNull() ?: 0
    }

    // Redis key format: "reads:userId:2024-01"
    // Automatically expires as months change (you reset or let them accumulate)
    private fun readCountKey(user: User): String {
        val monthKey = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        return "reads:${user.id}:$monthKey"
    }
}
```

Now wire it into `InterviewService.getBySlug()`:

```kotlin
@Service
class InterviewService(
    private val interviewRepository: InterviewRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val readTrackingService: ReadTrackingService
) {

    @Transactional(readOnly = true)
    fun getBySlug(slug: String, currentUser: User?): InterviewDetailDto {
        val interview = interviewRepository.findPublishedBySlugWithDetails(slug)
            ?: throw ResourceNotFoundException("Interview", slug)

        // Track the read if the user is logged in
        // This may throw PaywallException — which GlobalExceptionHandler returns as 402
        if (currentUser != null) {
            readTrackingService.trackRead(currentUser, interview)
        }

        val isBookmarked = currentUser != null &&
            bookmarkRepository.existsByUserAndInterview(currentUser, interview)

        return interview.toDetailDto(isBookmarked = isBookmarked)
    }
}
```

> **⚠️ Transaction note:** `trackRead` is `@Transactional` and `getBySlug` is `@Transactional(readOnly = true)`. When `getBySlug`calls `trackRead`, Spring detects that a transaction already exists and uses it — but since it is readOnly, the write in `trackRead` will fail.
> 
> Fix: Remove `readOnly = true` from `getBySlug`, or extract the track call to happen after the read-only transaction completes. For HireStory, removing `readOnly` from `getBySlug` is the simplest correct solution.

---

## 5.14 The Profile Endpoint — Seeing Your Own Data

```kotlin
// src/main/kotlin/com/example/hirestory/controller/ProfileController.kt

package com.example.hirestory.controller

import com.example.hirestory.dto.ProfileDto
import com.example.hirestory.entity.User
import com.example.hirestory.service.ProfileService
import com.example.hirestory.service.ReadTrackingService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/profile")
class ProfileController(
    private val profileService: ProfileService,
    private val readTrackingService: ReadTrackingService
) {

    // GET /api/profile
    @GetMapping
    fun getMyProfile(
        @AuthenticationPrincipal currentUser: User
    ): ResponseEntity<ProfileDto> {
        val readsThisMonth = readTrackingService.getMonthlyReadCount(currentUser)
        val readsRemaining = if (currentUser.isPremium) {
            Int.MAX_VALUE
        } else {
            maxOf(0, 25 - readsThisMonth)
        }

        return ResponseEntity.ok(
            ProfileDto(
                id = currentUser.id!!,
                name = currentUser.name,
                email = currentUser.email,
                avatarUrl = currentUser.avatarUrl,
                isPremium = currentUser.isPremium,
                referralCode = currentUser.referralCode,
                readsThisMonth = readsThisMonth,
                readsRemaining = readsRemaining
            )
        )
    }
}

data class ProfileDto(
    val id: Long,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val isPremium: Boolean,
    val referralCode: String,
    val readsThisMonth: Int,
    val readsRemaining: Int
)
```

---

## 5.15 Testing Security — What To Verify

Add these to your `.http` file:

```http
### Test auth — no token (should return 401)
GET {{baseUrl}}/bookmarks
Accept: application/json

###

### Test auth — with valid token
GET {{baseUrl}}/profile
Authorization: Bearer {{your_clerk_token}}
Accept: application/json

###

### Test admin — non-admin user (should return 403)
GET {{baseUrl}}/admin/stats
Authorization: Bearer {{non_admin_token}}
Accept: application/json

###

### Test paywall — read 26 interviews as free user
# After 25 reads, should return 402
GET {{baseUrl}}/interviews/google-sde-1-bangalore-2024
Authorization: Bearer {{free_user_token}}
Accept: application/json
```

### Getting a Test Token From Clerk

During development, get a token from Clerk's dashboard:

1. Go to your Clerk dashboard → Users
2. Click on a user → Sessions
3. Copy the active session token
4. Paste it as the Bearer token in your requests

---

## 5.16 How Everything Connects — The Full Auth Flow

Reading this end to end once will make everything click:

```
1. User opens the Android app
2. User logs in with Google via Clerk's SDK
3. Clerk returns a JWT token to the app
4. App stores the token in secure storage

5. User taps "Submit Interview"
6. App sends: POST /api/interviews
             Authorization: Bearer eyJhbGc...
             Body: { interview data }

7. Request hits Tomcat
8. JwtAuthenticationFilter runs
   a. Reads "eyJhbGc..." from Authorization header
   b. Calls jwtService.validateAndExtractClaims("eyJhbGc...")
   c. Fetches Clerk's public key (cached after first request)
   d. Verifies signature — valid
   e. Extracts clerkId = "user_2abc123"
   f. Calls userRepository.findByClerkId("user_2abc123")
   g. Finds User(id=5, name="Arjun Sharma", isPremium=false)
   h. Creates UsernamePasswordAuthenticationToken(user, null, [ROLE_USER])
   i. Puts it in SecurityContextHolder

9. Spring Security checks: does ROLE_USER have access to POST /interviews?
   Yes — anyRequest().authenticated() allows it

10. DispatcherServlet routes to InterviewController.submit()
11. Spring injects @AuthenticationPrincipal currentUser = User(id=5, ...)
12. InterviewController.submit(dto, currentUser) runs
13. InterviewService.submit(dto, currentUser) runs
14. Interview saved with user = currentUser
15. Response: 201 Created + interview JSON

16. SecurityContextHolder cleared for this thread
    (ready for the next request)
```

---

## 5.17 Common Mistakes in Chapter 5

### Mistake 1 — Forgetting to add the JWT filter before the default filter

```kotlin
// ❌ JWT filter added after — runs too late
http.addFilterAfter(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

// ✅ JWT filter runs before Spring's default filter
http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
```

### Mistake 2 — Not setting SessionCreationPolicy.STATELESS

```kotlin
// ❌ Missing session policy — Spring creates HTTP sessions, your app becomes stateful
// JWT tokens become useless because Spring trusts its own session cookie

// ✅ Always set this for JWT-based APIs
.sessionManagement { session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
}
```

### Mistake 3 — Not making the webhook endpoint public

```kotlin
// ❌ Webhook requires auth — Clerk cannot call it without a user token
// Result: Clerk gets 401, retries endlessly, users never get created

// ✅ In SecurityConfig
.requestMatchers(HttpMethod.POST, "/webhooks/clerk").permitAll()
```

### Mistake 4 — Using @AuthenticationPrincipal on wrong type

```kotlin
// ❌ Wrong type — Spring cannot cast and returns null or throws
@AuthenticationPrincipal currentUser: UserDetails  // UserDetails is Spring's interface

// ✅ Use your actual entity type — this is what you put in the security context
@AuthenticationPrincipal currentUser: User         // com.example.hirestory.entity.User
```

### Mistake 5 — Not verifying the Clerk webhook signature

```kotlin
// ❌ Anyone can send fake user.created events to your webhook
@PostMapping("/clerk")
fun handleClerkWebhook(@RequestBody body: String): ResponseEntity<String> {
    // Directly processing without verification — dangerous
    val event = objectMapper.readTree(body)
    handleUserCreated(event.get("data"))
    return ResponseEntity.ok("OK")
}

// ✅ Always verify the Svix signature first
if (!verifySignature(body, request)) {
    return ResponseEntity.status(401).body("Invalid signature")
}
```

### Mistake 6 — Counting re-reads toward the free tier

```kotlin
// ❌ Every read of the same interview counts against the limit
fun trackRead(user: User, interview: Interview) {
    val currentCount = getMonthlyReadCount(user)
    if (currentCount >= 25) throw PaywallException(...)
    redisTemplate.opsForValue().increment(readCountKey(user))
}

// ✅ Check read history first — only new reads count
fun trackRead(user: User, interview: Interview) {
    if (readHistoryRepository.existsByUserAndInterview(user, interview)) {
        return  // Already read — does not count again
    }
    // Now check and increment
}
```

---

## 5.18 HireStory Connection — What You Built in Chapter 5

By the end of Chapter 5, HireStory has a complete, production-grade authentication system:

- `JwtService` — validates Clerk JWT tokens against their public JWKS keys
- `JwtAuthenticationFilter` — reads token on every request, loads the user, puts them in the security context
- `SecurityConfig` — defines exactly which endpoints are public, which require auth, and which require admin role. Returns clean JSON for 401 and 403.
- `WebhookController` — receives Clerk user events, verifies signatures, creates users in your database
- `UserService` — creates and updates users from Clerk webhooks
- `ReadTrackingService` — enforces the 25-read free tier, stores history in database, counts in Redis
- `@AuthenticationPrincipal` — the `currentUser = null` placeholder replaced everywhere with the real authenticated user

The app now behaves correctly for all four user states:

- **Anonymous** — can read the feed and interview detail (no read counting)
- **Free user** — 25 reads per month, tracked in Redis and enforced with 402
- **Premium user** — unlimited reads
- **Admin** — access to `/admin/**` endpoints

---

## 5.19 Chapter Project — Build It Before You Move On

### What to build

Wire authentication end to end.

**Step 1 — Add the JWT dependencies**

Add `jjwt-api`, `jjwt-impl`, `jjwt-jackson` to `build.gradle.kts`.

**Step 2 — Set up Clerk**

- Create a free Clerk account at clerk.com
- Create an application
- Find the JWKS URL in your dashboard (API Keys → Advanced → JWKS URL)
- Add it to `application.yml`

**Step 3 — Write JwtService**

Implement `validateAndExtractClaims()` and `extractClerkId()`. Test it in isolation first — write a small test that decodes a real token from Clerk's dashboard.

**Step 4 — Write JwtAuthenticationFilter**

Implement the filter. Add a log line at each step so you can trace what is happening with every request.

**Step 5 — Write SecurityConfig**

Wire the filter in. Configure the access rules. Make sure the feed and company endpoints are public.

**Step 6 — Update controllers**

Replace `currentUser = null` with `@AuthenticationPrincipal currentUser: User?` on public endpoints and `User` (non-nullable) on protected ones.

**Step 7 — Test with real tokens**

- Get a Clerk token from the dashboard
- Hit `GET /api/profile` without a token — verify 401
- Hit `GET /api/profile` with the token — verify your user's data
- Hit `POST /api/bookmarks/1` without a token — verify 401
- Hit `POST /api/bookmarks/1` with a token — verify it works

### Checkpoint questions — answer before moving on

1. A user sends a valid JWT token, but the token expired 2 minutes ago. What happens in `JwtService.validateAndExtractClaims()`? What does the client receive?
    
2. Your app starts but throws `IllegalStateException: Cannot start without Clerk public keys`. What are two possible causes?
    
3. A new user signs up via Clerk on the Android app. They immediately try to read an interview. Their token is valid but `userRepository.findByClerkId(clerkId)` returns null. Why does this happen and what does your filter do about it?
    
4. You want only admin users to access `GET /api/admin/stats`. Currently you handle this in `SecurityConfig` with `.requestMatchers("/admin/**").hasRole("ADMIN")`. Where in your codebase would you set the ADMIN role for a specific user?
    
5. The `readOnly = true` problem from Section 5.13 — why does Spring's `@Transactional(readOnly = true)` cause the write in `trackRead` to fail even though `trackRead` has its own `@Transactional`?
    

---

_Chapter 6 → Redis — Caching, Counters and Speed_

---

> **Book Progress:** Chapter 5 of 15 complete. Chapters ahead: Redis · RabbitMQ · Spring AI · Jsoup · Scheduler · Testing · Deployment