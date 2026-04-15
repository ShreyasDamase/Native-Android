# Backend Engineering with Spring Boot & Kotlin

## The HireStory Builder's Guide

---

# Chapter 3 — Repositories and Queries

### _Talking to your database — from simple lookups to complex filtered feeds_

---

## 3.1 The Problem Repositories Solve

In Node.js with Mongoose, every database operation required you to write explicit code:

```javascript
// Node.js — you write every query yourself
const interviews = await Interview
  .find({ status: 'PUBLISHED', company: 'Google' })
  .sort({ createdAt: -1 })
  .limit(20)
  .skip(page * 20)
  .populate('company')
  .populate('user')
```

This works, but every query is manual. You write the filtering, the sorting, the pagination, the population of related objects — all by hand, every time.

Spring Data JPA takes a different approach. You define an interface, Spring generates the implementation. For the majority of queries you will ever write, you never touch SQL at all.

```kotlin
// Spring Boot — Spring generates this query automatically from the method name
fun findByCompanyAndStatusOrderByAddedAtDesc(
    company: Company,
    status: InterviewStatus
): List<Interview>
```

Spring reads that method name, understands what you want, and generates the correct SQL. No implementation required. No query written.

This chapter teaches you the full system — from the simplest free methods to complex paginated feeds with custom SQL.

---

## 3.2 JpaRepository — Your Free Database Layer

Every repository interface in Spring Boot extends `JpaRepository`. This single interface gives you a complete set of database operations for free — no implementation needed.

```kotlin
// src/main/kotlin/com/example/hirestory/repository/CompanyRepository.kt

package com.example.hirestory.repository

import com.example.hirestory.entity.Company
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CompanyRepository : JpaRepository<Company, Long>
// That is the entire file. Spring generates all of this automatically:
//
// save(entity)             — INSERT or UPDATE
// saveAll(entities)        — INSERT or UPDATE multiple
// findById(id)             — SELECT WHERE id = ?  → returns Optional<Company>
// findAll()                — SELECT * FROM companies
// findAll(pageable)        — SELECT * with pagination
// findAllById(ids)         — SELECT WHERE id IN (...)
// count()                  — SELECT COUNT(*)
// existsById(id)           — SELECT EXISTS WHERE id = ?
// deleteById(id)           — DELETE WHERE id = ?
// delete(entity)           — DELETE the given entity
// deleteAll()              — DELETE FROM companies (use carefully)
```

The two type parameters in `JpaRepository<Company, Long>` are:

- `Company` — the entity type this repository manages
- `Long` — the type of the primary key (`id` is `Long` in your entities)

### Using the Free Methods

```kotlin
@Service
class CompanyService(private val companyRepository: CompanyRepository) {

    // Get all companies
    fun findAll(): List<Company> = companyRepository.findAll()

    // Get one company — returns null if not found (Kotlin extension)
    fun findById(id: Long): Company? = companyRepository.findByIdOrNull(id)

    // Save a new company
    fun create(company: Company): Company = companyRepository.save(company)

    // Check if company exists
    fun exists(id: Long): Boolean = companyRepository.existsById(id)

    // Count all companies
    fun count(): Long = companyRepository.count()
}
```

### findById vs findByIdOrNull

`JpaRepository.findById()` returns `Optional<Company>` — a Java type that wraps a nullable value. In Kotlin, this is awkward:

```kotlin
// ❌ Java-style Optional — verbose in Kotlin
val company: Optional<Company> = companyRepository.findById(id)
if (company.isPresent) {
    return company.get()
} else {
    throw NotFoundException()
}

// ✅ Kotlin extension — much cleaner
val company: Company? = companyRepository.findByIdOrNull(id)
    ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found")
```

`findByIdOrNull` is a Kotlin extension function provided by Spring Data. Import it with:

```kotlin
import org.springframework.data.repository.findByIdOrNull
```

Always prefer `findByIdOrNull` over `findById` in Kotlin.

---

## 3.3 Query Methods — Spring's Method Name Magic

Spring Data reads your method name and generates a SQL query from it. This is called **derived queries** or **query methods**. Once you understand the rules, you can write almost any query just by naming the method correctly.

### The Pattern

```
findBy + FieldName + Condition + And/Or + FieldName + Condition + OrderBy + FieldName + Asc/Desc
```

### Simple Lookups

```kotlin
interface InterviewRepository : JpaRepository<Interview, Long> {

    // SELECT * FROM interviews WHERE slug = ?
    fun findBySlug(slug: String): Interview?

    // SELECT * FROM interviews WHERE status = ?
    fun findByStatus(status: InterviewStatus): List<Interview>

    // SELECT * FROM interviews WHERE company_id = ?
    fun findByCompany(company: Company): List<Interview>

    // SELECT * FROM interviews WHERE company_id = ? AND status = ?
    fun findByCompanyAndStatus(company: Company, status: InterviewStatus): List<Interview>

    // SELECT * FROM interviews WHERE outcome = ? OR difficulty = ?
    fun findByOutcomeOrDifficulty(outcome: Outcome, difficulty: Difficulty): List<Interview>

    // SELECT COUNT(*) FROM interviews WHERE company_id = ?
    fun countByCompany(company: Company): Long

    // SELECT * FROM interviews WHERE salary_lpa IS NOT NULL
    fun findBySalaryLpaIsNotNull(): List<Interview>

    // SELECT * FROM interviews WHERE location IS NULL
    fun findByLocationIsNull(): List<Interview>

    // SELECT * FROM interviews WHERE experience_years >= ?
    fun findByExperienceYearsGreaterThanEqual(years: Int): List<Interview>

    // SELECT EXISTS WHERE source_url = ?
    fun existsBySourceUrl(sourceUrl: String): Boolean
}
```

### Sorting in Method Names

```kotlin
interface InterviewRepository : JpaRepository<Interview, Long> {

    // SELECT * WHERE status = ? ORDER BY added_at DESC
    fun findByStatusOrderByAddedAtDesc(status: InterviewStatus): List<Interview>

    // SELECT * WHERE company_id = ? ORDER BY added_at DESC
    fun findByCompanyOrderByAddedAtDesc(company: Company): List<Interview>

    // SELECT * ORDER BY added_at DESC (no filter)
    fun findAllByOrderByAddedAtDesc(): List<Interview>
}
```

### The Condition Keywords

|Keyword|SQL equivalent|Example|
|---|---|---|
|`Is`, `Equals`|`=`|`findByStatus(status)`|
|`Not`|`!=`|`findByStatusNot(status)`|
|`IsNull`|`IS NULL`|`findByLocationIsNull()`|
|`IsNotNull`|`IS NOT NULL`|`findBySalaryLpaIsNotNull()`|
|`GreaterThan`|`>`|`findByExperienceYearsGreaterThan(n)`|
|`GreaterThanEqual`|`>=`|`findByExperienceYearsGreaterThanEqual(n)`|
|`LessThan`|`<`|`findByExperienceYearsLessThan(n)`|
|`LessThanEqual`|`<=`|`findByExperienceYearsLessThanEqual(n)`|
|`Between`|`BETWEEN ? AND ?`|`findByAddedAtBetween(start, end)`|
|`In`|`IN (...)`|`findByStatusIn(statuses)`|
|`NotIn`|`NOT IN (...)`|`findByStatusNotIn(statuses)`|
|`Like`|`LIKE ?`|`findByTitleLike("%google%")`|
|`Containing`|`LIKE %?%`|`findByTitleContaining("google")`|
|`StartingWith`|`LIKE ?%`|`findByTitleStartingWith("Google")`|
|`True` / `False`|`= true` / `= false`|`findByIsPremiumTrue()`|
|`OrderBy...Asc`|`ORDER BY ... ASC`|`findAllByOrderByAddedAtAsc()`|
|`OrderBy...Desc`|`ORDER BY ... DESC`|`findAllByOrderByAddedAtDesc()`|

---

## 3.4 Pagination — The Feed Engine

The HireStory feed shows 20 interviews at a time. The user scrolls and loads more. This is pagination. Spring Data has first-class support for it.

### Understanding Pageable

```kotlin
// Pageable is an object that describes:
// - Which page you want (0-indexed)
// - How many items per page
// - How to sort

val pageable = PageRequest.of(
    0,                              // Page 0 = first page
    20,                             // 20 items per page
    Sort.by(Sort.Direction.DESC, "addedAt")  // Sort by addedAt descending
)
```

### Page vs Slice

Spring Data gives you two options for paginated results:

```kotlin
// Page<T> — includes total count (requires extra COUNT query)
// Use when you need to show "Page 3 of 47" or total item count
fun findByStatus(status: InterviewStatus, pageable: Pageable): Page<Interview>

// Slice<T> — no total count (faster — no extra query)
// Use for infinite scroll where you only need "is there a next page?"
fun findByStatus(status: InterviewStatus, pageable: Pageable): Slice<Interview>
```

For HireStory's feed — infinite scroll, no page numbers shown — use `Slice`. For the admin panel — where you need "showing 41-60 of 847 pending interviews" — use `Page`.

### Pagination in Practice

```kotlin
// Repository
interface InterviewRepository : JpaRepository<Interview, Long> {

    fun findByStatus(
        status: InterviewStatus,
        pageable: Pageable
    ): Slice<Interview>

    fun findByCompanyAndStatus(
        company: Company,
        status: InterviewStatus,
        pageable: Pageable
    ): Slice<Interview>
}

// Service
@Service
class InterviewService(private val interviewRepository: InterviewRepository) {

    @Transactional(readOnly = true)
    fun getFeed(page: Int, size: Int): Slice<Interview> {
        val pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "addedAt")
        )
        return interviewRepository.findByStatus(InterviewStatus.PUBLISHED, pageable)
    }
}

// Controller
@RestController
@RequestMapping("/interviews")
class InterviewController(private val interviewService: InterviewService) {

    // GET /api/interviews?page=0&size=20
    @GetMapping
    fun getFeed(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<SliceResponse<InterviewSummaryDto>> {
        val slice = interviewService.getFeed(page, size)
        return ResponseEntity.ok(
            SliceResponse(
                content = slice.content.map { it.toSummaryDto() },
                hasNext = slice.hasNext(),
                page = page,
                size = size
            )
        )
    }
}

// Response wrapper — what the app receives
data class SliceResponse<T>(
    val content: List<T>,
    val hasNext: Boolean,   // App uses this to decide whether to show "Load More"
    val page: Int,
    val size: Int
)
```

---

## 3.5 @Query — When Method Names Are Not Enough

Method names work for simple queries. When your query gets complex — multiple optional filters, JOIN FETCH to avoid N+1, full-text search — you write JPQL or native SQL directly.

### JPQL vs Native SQL

```kotlin
// JPQL — Java Persistence Query Language
// Looks like SQL but uses entity class names and field names, not table/column names
// Spring translates it to the correct SQL for your database
@Query("SELECT i FROM Interview i WHERE i.status = :status ORDER BY i.addedAt DESC")
fun findPublished(status: InterviewStatus): List<Interview>

// Native SQL — actual PostgreSQL syntax
// Use when you need database-specific features (full text search, window functions, etc.)
@Query(
    value = "SELECT * FROM interviews WHERE status = :status ORDER BY added_at DESC",
    nativeQuery = true
)
fun findPublishedNative(status: InterviewStatus): List<Interview>
```

Prefer JPQL unless you need something database-specific. JPQL works regardless of which database you use.

### JOIN FETCH — Solving N+1 With @Query

Remember the N+1 problem from Chapter 2? Here is the fix:

```kotlin
interface InterviewRepository : JpaRepository<Interview, Long> {

    // ❌ N+1 problem — loads interviews, then one query per interview for company
    fun findByStatus(status: InterviewStatus): List<Interview>

    // ✅ One query — loads interviews AND their companies together
    @Query("""
        SELECT i FROM Interview i
        JOIN FETCH i.company
        WHERE i.status = :status
        ORDER BY i.addedAt DESC
    """)
    fun findByStatusWithCompany(
        @Param("status") status: InterviewStatus
    ): List<Interview>

    // ✅ Load interviews with company AND tags in one query
    @Query("""
        SELECT DISTINCT i FROM Interview i
        JOIN FETCH i.company
        LEFT JOIN FETCH i.tags
        WHERE i.status = :status
        ORDER BY i.addedAt DESC
    """)
    fun findByStatusWithCompanyAndTags(
        @Param("status") status: InterviewStatus
    ): List<Interview>
}
```

> **⚠️ Important:** You cannot use `JOIN FETCH` with `Pageable` directly in JPQL — Hibernate will load all records into memory and paginate in Java, which is worse than N+1. The solution is covered in Section 3.7.

### Optional Filters — The HireStory Feed Query

The HireStory feed supports optional filters: company, difficulty, outcome. A user might filter by company only, or by difficulty only, or by both, or by none. Writing a method name for every combination is impossible.

The correct solution is JPQL with conditional clauses:

```kotlin
interface InterviewRepository : JpaRepository<Interview, Long> {

    @Query("""
        SELECT i FROM Interview i
        JOIN FETCH i.company c
        WHERE i.status = 'PUBLISHED'
        AND (:companyId IS NULL OR c.id = :companyId)
        AND (:difficulty IS NULL OR i.difficulty = :difficulty)
        AND (:outcome IS NULL OR i.outcome = :outcome)
        AND (:role IS NULL OR LOWER(i.role) LIKE LOWER(CONCAT('%', :role, '%')))
        ORDER BY i.addedAt DESC
    """)
    fun findFeed(
        @Param("companyId") companyId: Long?,
        @Param("difficulty") difficulty: Difficulty?,
        @Param("outcome") outcome: Outcome?,
        @Param("role") role: String?,
        pageable: Pageable
    ): Slice<Interview>
}
```

When `companyId` is `null`, the condition `(:companyId IS NULL OR c.id = :companyId)` evaluates to `TRUE` (because the first part is true), so the filter is skipped. When `companyId` is `5L`, it becomes `(FALSE OR c.id = 5)` — which means only interviews from company 5.

This single query handles all filter combinations.

### @Modifying — For UPDATE and DELETE Queries

`@Query` by default is for SELECT. For UPDATE and DELETE, add `@Modifying`:

```kotlin
interface InterviewRepository : JpaRepository<Interview, Long> {

    // Update the status of one interview
    @Modifying
    @Transactional
    @Query("UPDATE Interview i SET i.status = :status, i.publishedAt = :publishedAt WHERE i.id = :id")
    fun updateStatus(
        @Param("id") id: Long,
        @Param("status") status: InterviewStatus,
        @Param("publishedAt") publishedAt: LocalDateTime?
    ): Int   // Returns number of rows affected

    // Increment a company's interview count
    @Modifying
    @Transactional
    @Query("UPDATE Company c SET c.interviewCount = c.interviewCount + 1 WHERE c.id = :id")
    fun incrementInterviewCount(@Param("id") id: Long)

    // Soft delete a comment
    @Modifying
    @Transactional
    @Query("UPDATE Comment c SET c.isDeleted = true WHERE c.id = :id AND c.user.id = :userId")
    fun softDelete(@Param("id") id: Long, @Param("userId") userId: Long): Int
}
```

---

## 3.6 Full Text Search — The Search Feature

HireStory needs `GET /api/search?q=google+system+design`. PostgreSQL has built-in full-text search. This is a place where native SQL is the right choice.

### Adding the Search Vector Column

First, add a migration to create a search vector column:

```sql
-- src/main/resources/db/migration/V3__add_search_vector.sql

-- Add a tsvector column that PostgreSQL uses for full-text search
ALTER TABLE interviews ADD COLUMN search_vector tsvector;

-- Create a GIN index on it (GIN is the correct index type for full-text search)
CREATE INDEX idx_interviews_search ON interviews USING GIN(search_vector);

-- Populate search_vector from title, headline, content, and role
-- to_tsvector('english', ...) tokenises and normalises the text
-- The weights (A, B, C) affect relevance ranking
UPDATE interviews SET search_vector =
    setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
    setweight(to_tsvector('english', coalesce(role, '')), 'A') ||
    setweight(to_tsvector('english', coalesce(headline, '')), 'B') ||
    setweight(to_tsvector('english', coalesce(content, '')), 'C');

-- Create a trigger to keep search_vector updated automatically
-- when title, headline, content, or role changes
CREATE OR REPLACE FUNCTION update_interview_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', coalesce(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(NEW.role, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(NEW.headline, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(NEW.content, '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER interviews_search_vector_update
    BEFORE INSERT OR UPDATE OF title, role, headline, content
    ON interviews
    FOR EACH ROW
    EXECUTE FUNCTION update_interview_search_vector();
```

### The Search Query

```kotlin
interface InterviewRepository : JpaRepository<Interview, Long> {

    // Full text search — native PostgreSQL query
    @Query(value = """
        SELECT i.* FROM interviews i
        JOIN companies c ON i.company_id = c.id
        WHERE i.status = 'PUBLISHED'
        AND i.search_vector @@ plainto_tsquery('english', :query)
        ORDER BY ts_rank(i.search_vector, plainto_tsquery('english', :query)) DESC,
                 i.added_at DESC
        LIMIT :size OFFSET :offset
    """, nativeQuery = true)
    fun search(
        @Param("query") query: String,
        @Param("size") size: Int,
        @Param("offset") offset: Int
    ): List<Interview>

    // Count total results for pagination
    @Query(value = """
        SELECT COUNT(*) FROM interviews
        WHERE status = 'PUBLISHED'
        AND search_vector @@ plainto_tsquery('english', :query)
    """, nativeQuery = true)
    fun countSearchResults(@Param("query") query: String): Long
}
```

What `plainto_tsquery('english', :query)` does:

- Input: `"google system design"`
- Output: a query that matches documents containing `google`, `system`, `design`
- It handles stemming: `designing` matches `design`, `googled` matches `google`

What `@@` does: matches `search_vector` against the query.

What `ts_rank` does: scores each result by relevance — results with the search terms in the title (weight A) rank higher than results with terms only in the content (weight C).

### The Search Service

```kotlin
@Service
class SearchService(private val interviewRepository: InterviewRepository) {

    @Transactional(readOnly = true)
    fun search(query: String, page: Int, size: Int): SearchResult {
        if (query.isBlank()) {
            return SearchResult(emptyList(), 0, false)
        }

        val cleanQuery = query.trim()
        val offset = page * size
        val interviews = interviewRepository.search(cleanQuery, size + 1, offset)
        // Fetch size + 1 to check if there is a next page
        // If we got more than size results, there is a next page

        val hasNext = interviews.size > size
        val content = if (hasNext) interviews.dropLast(1) else interviews

        return SearchResult(
            interviews = content.map { it.toSummaryDto() },
            total = interviewRepository.countSearchResults(cleanQuery),
            hasNext = hasNext
        )
    }
}

data class SearchResult(
    val interviews: List<InterviewSummaryDto>,
    val total: Long,
    val hasNext: Boolean
)
```

---

## 3.7 Solving Pagination With JOIN FETCH

Earlier you learned that `JOIN FETCH` does not work with `Pageable` in JPQL. Here is why and the correct solution.

### Why It Breaks

```kotlin
// ❌ This causes Hibernate to load ALL records into memory and paginate there
// Hibernate logs a warning: "HHH90003004: firstResult/maxResults specified with collection fetch"
@Query("""
    SELECT DISTINCT i FROM Interview i
    JOIN FETCH i.tags          ← This is the problem
    WHERE i.status = :status
""")
fun findWithTags(status: InterviewStatus, pageable: Pageable): Slice<Interview>
```

When you `JOIN FETCH` a collection (`@OneToMany` or `@ManyToMany`), Hibernate cannot paginate in SQL because the JOIN produces multiple rows per interview. So it loads everything and paginates in Java. For 10,000 interviews this is catastrophic.

### The Solution — Two-Query Approach

```kotlin
interface InterviewRepository : JpaRepository<Interview, Long> {

    // Query 1: Get the IDs with pagination (no JOIN FETCH — safe to paginate)
    @Query("""
        SELECT i.id FROM Interview i
        WHERE i.status = :status
        ORDER BY i.addedAt DESC
    """)
    fun findIdsByStatus(
        @Param("status") status: InterviewStatus,
        pageable: Pageable
    ): Slice<Long>

    // Query 2: Load full data for those specific IDs (JOIN FETCH is safe — fixed set of IDs)
    @Query("""
        SELECT DISTINCT i FROM Interview i
        JOIN FETCH i.company
        LEFT JOIN FETCH i.tags
        WHERE i.id IN :ids
        ORDER BY i.addedAt DESC
    """)
    fun findByIdsWithDetails(@Param("ids") ids: List<Long>): List<Interview>
}

// Service that combines both queries
@Service
class InterviewService(private val interviewRepository: InterviewRepository) {

    @Transactional(readOnly = true)
    fun getFeed(page: Int, size: Int): SliceResult<Interview> {
        val pageable = PageRequest.of(page, size)

        // Step 1: Get paginated IDs (fast — no joins)
        val idSlice = interviewRepository.findIdsByStatus(
            InterviewStatus.PUBLISHED,
            pageable
        )

        if (idSlice.isEmpty) {
            return SliceResult(emptyList(), false)
        }

        // Step 2: Load full data for those IDs (one query with joins)
        val interviews = interviewRepository.findByIdsWithDetails(idSlice.content)

        // Preserve the original order (IN clause does not guarantee order)
        val orderedInterviews = idSlice.content.mapNotNull { id ->
            interviews.find { it.id == id }
        }

        return SliceResult(orderedInterviews, idSlice.hasNext())
    }
}

data class SliceResult<T>(val content: List<T>, val hasNext: Boolean)
```

This is the professional pattern. Two queries, one for pagination metadata, one for actual data. Total: 2 database roundtrips instead of N+1.

---

## 3.8 All HireStory Repositories

Now let us write every repository the app needs:

```kotlin
// src/main/kotlin/com/example/hirestory/repository/UserRepository.kt

package com.example.hirestory.repository

import com.example.hirestory.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {

    fun findByClerkId(clerkId: String): User?

    fun findByEmail(email: String): User?

    fun findByReferralCode(referralCode: String): User?

    fun existsByClerkId(clerkId: String): Boolean

    fun existsByReferralCode(referralCode: String): Boolean
}
```

```kotlin
// src/main/kotlin/com/example/hirestory/repository/CompanyRepository.kt

package com.example.hirestory.repository

import com.example.hirestory.entity.Company
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface CompanyRepository : JpaRepository<Company, Long> {

    fun findBySlug(slug: String): Company?

    fun findByName(name: String): Company?

    fun existsByName(name: String): Boolean

    fun existsBySlug(slug: String): Boolean

    // Increment interview count atomically — avoids race conditions
    @Modifying
    @Query("UPDATE Company c SET c.interviewCount = c.interviewCount + 1 WHERE c.id = :id")
    fun incrementInterviewCount(@Param("id") id: Long)

    // Most active companies — for the company listing screen
    fun findAllByOrderByInterviewCountDesc(): List<Company>
}
```

```kotlin
// src/main/kotlin/com/example/hirestory/repository/InterviewRepository.kt

package com.example.hirestory.repository

import com.example.hirestory.entity.*
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface InterviewRepository : JpaRepository<Interview, Long> {

    fun findBySlug(slug: String): Interview?

    fun existsBySourceUrl(sourceUrl: String): Boolean

    fun existsBySlug(slug: String): Boolean

    fun countByStatus(status: InterviewStatus): Long

    fun countByCompanyAndStatus(company: Company, status: InterviewStatus): Long

    // ── Feed queries ────────────────────────────────────────────────

    // Step 1 of two-query pagination: get IDs only
    @Query("""
        SELECT i.id FROM Interview i
        JOIN i.company c
        WHERE i.status = 'PUBLISHED'
        AND (:companyId IS NULL OR c.id = :companyId)
        AND (:difficulty IS NULL OR i.difficulty = :difficulty)
        AND (:outcome IS NULL OR i.outcome = :outcome)
        ORDER BY i.addedAt DESC
    """)
    fun findFeedIds(
        @Param("companyId") companyId: Long?,
        @Param("difficulty") difficulty: Difficulty?,
        @Param("outcome") outcome: Difficulty?,
        pageable: Pageable
    ): Slice<Long>

    // Step 2 of two-query pagination: load full data for the IDs
    @Query("""
        SELECT DISTINCT i FROM Interview i
        JOIN FETCH i.company
        LEFT JOIN FETCH i.tags
        WHERE i.id IN :ids
    """)
    fun findByIdsWithDetails(@Param("ids") ids: List<Long>): List<Interview>

    // Single interview with all details — for the detail screen
    @Query("""
        SELECT i FROM Interview i
        JOIN FETCH i.company
        LEFT JOIN FETCH i.tags
        LEFT JOIN FETCH i.user
        WHERE i.slug = :slug AND i.status = 'PUBLISHED'
    """)
    fun findPublishedBySlugWithDetails(@Param("slug") slug: String): Interview?

    // ── Admin queries ───────────────────────────────────────────────

    // Pending interviews for admin review
    @Query("""
        SELECT i FROM Interview i
        JOIN FETCH i.company
        LEFT JOIN FETCH i.user
        WHERE i.status = 'PENDING'
        ORDER BY i.addedAt ASC
    """)
    fun findPendingForReview(pageable: Pageable): Slice<Interview>

    // ── Status updates ──────────────────────────────────────────────

    @Modifying
    @Query("""
        UPDATE Interview i
        SET i.status = :status, i.publishedAt = :publishedAt
        WHERE i.id = :id
    """)
    fun updateStatus(
        @Param("id") id: Long,
        @Param("status") status: InterviewStatus,
        @Param("publishedAt") publishedAt: LocalDateTime?
    ): Int

    // ── User's interviews ───────────────────────────────────────────

    fun findByUserAndStatusOrderByAddedAtDesc(
        user: User,
        status: InterviewStatus
    ): List<Interview>

    // ── Search ──────────────────────────────────────────────────────

    @Query(value = """
        SELECT i.* FROM interviews i
        WHERE i.status = 'PUBLISHED'
        AND i.search_vector @@ plainto_tsquery('english', :query)
        ORDER BY ts_rank(i.search_vector, plainto_tsquery('english', :query)) DESC,
                 i.added_at DESC
        LIMIT :size OFFSET :offset
    """, nativeQuery = true)
    fun search(
        @Param("query") query: String,
        @Param("size") size: Int,
        @Param("offset") offset: Int
    ): List<Interview>

    @Query(value = """
        SELECT COUNT(*) FROM interviews
        WHERE status = 'PUBLISHED'
        AND search_vector @@ plainto_tsquery('english', :query)
    """, nativeQuery = true)
    fun countSearchResults(@Param("query") query: String): Long
}
```

```kotlin
// src/main/kotlin/com/example/hirestory/repository/BookmarkRepository.kt

package com.example.hirestory.repository

import com.example.hirestory.entity.Bookmark
import com.example.hirestory.entity.Interview
import com.example.hirestory.entity.User
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BookmarkRepository : JpaRepository<Bookmark, Long> {

    fun existsByUserAndInterview(user: User, interview: Interview): Boolean

    fun findByUserAndInterview(user: User, interview: Interview): Bookmark?

    fun deleteByUserAndInterview(user: User, interview: Interview)

    fun countByUser(user: User): Long

    // User's bookmarks with interview details pre-loaded
    @Query("""
        SELECT b FROM Bookmark b
        JOIN FETCH b.interview i
        JOIN FETCH i.company
        WHERE b.user = :user
        ORDER BY b.createdAt DESC
    """)
    fun findByUserWithDetails(
        @Param("user") user: User,
        pageable: Pageable
    ): Slice<Bookmark>

    // Check which interviews from a list the user has bookmarked
    // Useful for the feed — set the isBookmarked flag on each interview card
    @Query("""
        SELECT b.interview.id FROM Bookmark b
        WHERE b.user = :user AND b.interview.id IN :interviewIds
    """)
    fun findBookmarkedInterviewIds(
        @Param("user") user: User,
        @Param("interviewIds") interviewIds: List<Long>
    ): List<Long>
}
```

```kotlin
// src/main/kotlin/com/example/hirestory/repository/CommentRepository.kt

package com.example.hirestory.repository

import com.example.hirestory.entity.Comment
import com.example.hirestory.entity.Interview
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface CommentRepository : JpaRepository<Comment, Long> {

    // Only show non-deleted comments, with user pre-loaded
    @Query("""
        SELECT c FROM Comment c
        JOIN FETCH c.user
        WHERE c.interview = :interview AND c.isDeleted = false
        ORDER BY c.createdAt ASC
    """)
    fun findByInterviewWithUser(
        @Param("interview") interview: Interview,
        pageable: Pageable
    ): Slice<Comment>

    fun countByInterviewAndIsDeletedFalse(interview: Interview): Long
}
```

```kotlin
// src/main/kotlin/com/example/hirestory/repository/ReadHistoryRepository.kt

package com.example.hirestory.repository

import com.example.hirestory.entity.Interview
import com.example.hirestory.entity.ReadHistory
import com.example.hirestory.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ReadHistoryRepository : JpaRepository<ReadHistory, Long> {

    fun existsByUserAndInterview(user: User, interview: Interview): Boolean

    // IDs of interviews this user has already read
    // Used to exclude already-read interviews from the feed
    @Query("""
        SELECT rh.interview.id FROM ReadHistory rh
        WHERE rh.user = :user
    """)
    fun findReadInterviewIdsByUser(@Param("user") user: User): List<Long>
}
```

```kotlin
// src/main/kotlin/com/example/hirestory/repository/CrawlJobRepository.kt

package com.example.hirestory.repository

import com.example.hirestory.entity.CrawlJob
import com.example.hirestory.entity.CrawlStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CrawlJobRepository : JpaRepository<CrawlJob, Long> {

    fun existsBySourceUrl(sourceUrl: String): Boolean

    fun findByStatus(status: CrawlStatus): List<CrawlJob>

    fun countByStatus(status: CrawlStatus): Long
}
```

---

## 3.9 Projections — Returning Only What You Need

Sometimes you do not need the full entity. You only need a few fields. Loading the entire entity (especially with `content TEXT` which can be thousands of characters) wastes memory and bandwidth.

Spring Data supports **projections** — interfaces that define only the fields you want:

```kotlin
// Define a projection interface
// Only the fields declared here are loaded from the database
interface InterviewSummaryProjection {
    val id: Long
    val slug: String
    val title: String
    val role: String
    val difficulty: Difficulty
    val outcome: Outcome
    val experienceYears: Int
    val roundsCount: Int
    val addedAt: LocalDateTime
    // Note: content is NOT here — we skip loading thousands of chars
    // Note: company is accessed via nested projection below
    fun getCompany(): CompanyProjection

    interface CompanyProjection {
        val id: Long
        val name: String
        val logoUrl: String?
    }
}

// Use it in your repository
interface InterviewRepository : JpaRepository<Interview, Long> {

    @Query("""
        SELECT i FROM Interview i
        JOIN FETCH i.company
        WHERE i.status = 'PUBLISHED'
        ORDER BY i.addedAt DESC
    """)
    fun findSummaries(pageable: Pageable): Slice<InterviewSummaryProjection>
}
```

For HireStory, this matters especially for the feed — where you show 20 cards, each needing only the summary data. Not loading `content` (which can be 2000+ characters) for 20 interviews saves significant memory per request.

---

## 3.10 Entity Mapper — Converting Between Entity and DTO

Your controller must never return raw entities. It returns DTOs. You need a way to convert. The cleanest approach in Kotlin is extension functions:

```kotlin
// src/main/kotlin/com/example/hirestory/dto/Mappers.kt

package com.example.hirestory.dto

import com.example.hirestory.entity.Company
import com.example.hirestory.entity.Interview
import com.example.hirestory.entity.InterviewRound
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ── Company ─────────────────────────────────────────────────────────

fun Company.toDto() = CompanyDto(
    id = id!!,
    name = name,
    logoUrl = logoUrl,
    slug = slug,
    interviewCount = interviewCount
)

data class CompanyDto(
    val id: Long,
    val name: String,
    val logoUrl: String?,
    val slug: String,
    val interviewCount: Int
)

// ── Interview Summary (for feed) ─────────────────────────────────────

fun Interview.toSummaryDto() = InterviewSummaryDto(
    id = id!!,
    slug = slug,
    title = title,
    company = company.toDto(),       // company must be loaded (use JOIN FETCH)
    role = role,
    difficulty = difficulty,
    outcome = outcome,
    experienceYears = experienceYears,
    roundsCount = roundsCount,
    addedAt = addedAt.formatDisplay(),
    tags = tags.map { it.name }      // tags must be loaded (use JOIN FETCH)
)

data class InterviewSummaryDto(
    val id: Long,
    val slug: String,
    val title: String,
    val company: CompanyDto,
    val role: String,
    val difficulty: Difficulty,
    val outcome: Outcome,
    val experienceYears: Int,
    val roundsCount: Int,
    val addedAt: String,
    val tags: List<String>
)

// ── Interview Detail (for detail screen) ─────────────────────────────

fun Interview.toDetailDto(isBookmarked: Boolean = false) = InterviewDetailDto(
    id = id!!,
    slug = slug,
    title = title,
    headline = headline,
    content = content,
    company = company.toDto(),
    role = role,
    location = location,
    difficulty = difficulty,
    outcome = outcome,
    experienceYears = experienceYears,
    salaryLpa = salaryLpa,
    rounds = rounds.sortedBy { it.roundNumber }.map { it.toDto() },
    tags = tags.map { it.name },
    author = user?.let { AuthorDto(it.name, it.avatarUrl) },
    addedAt = addedAt.formatDisplay(),
    sourceUrl = sourceUrl,
    isBookmarked = isBookmarked
)

data class InterviewDetailDto(
    val id: Long,
    val slug: String,
    val title: String,
    val headline: String,
    val content: String,
    val company: CompanyDto,
    val role: String,
    val location: String?,
    val difficulty: Difficulty,
    val outcome: Outcome,
    val experienceYears: Int,
    val salaryLpa: Double?,
    val rounds: List<RoundDto>,
    val tags: List<String>,
    val author: AuthorDto?,         // Null if anonymous
    val addedAt: String,
    val sourceUrl: String?,
    val isBookmarked: Boolean
)

data class AuthorDto(val name: String, val avatarUrl: String?)

// ── Round ────────────────────────────────────────────────────────────

fun InterviewRound.toDto() = RoundDto(
    id = id!!,
    roundNumber = roundNumber,
    title = title,
    questions = questions,
    difficulty = difficulty,
    notes = notes
)

data class RoundDto(
    val id: Long,
    val roundNumber: Int,
    val title: String,
    val questions: String,
    val difficulty: String?,
    val notes: String?
)

// ── Date formatting ──────────────────────────────────────────────────

private val displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

fun LocalDateTime.formatDisplay(): String = this.format(displayFormatter)
// Result: "15 Jan 2024"
```

---

## 3.11 Putting It All Together — The Feed Service

Here is the complete feed implementation using everything from this chapter:

```kotlin
// src/main/kotlin/com/example/hirestory/service/InterviewService.kt

package com.example.hirestory.service

import com.example.hirestory.dto.*
import com.example.hirestory.entity.*
import com.example.hirestory.repository.*
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class InterviewService(
    private val interviewRepository: InterviewRepository,
    private val companyRepository: CompanyRepository,
    private val bookmarkRepository: BookmarkRepository
) {

    // The main feed — used by home screen
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

        // Step 1: Get IDs with pagination
        val idSlice = interviewRepository.findFeedIds(
            companyId = companyId,
            difficulty = difficulty,
            outcome = outcome,
            pageable = pageable
        )

        if (idSlice.isEmpty) {
            return SliceResponse(emptyList(), false, page, size)
        }

        // Step 2: Load full data for those IDs
        val interviews = interviewRepository.findByIdsWithDetails(idSlice.content)

        // Step 3: Find which of these interviews the user has bookmarked
        val bookmarkedIds = if (currentUser != null) {
            bookmarkRepository.findBookmarkedInterviewIds(currentUser, idSlice.content).toSet()
        } else {
            emptySet()
        }

        // Step 4: Convert to DTOs preserving original order
        val dtos = idSlice.content.mapNotNull { id ->
            interviews.find { it.id == id }?.toSummaryDto()
        }

        return SliceResponse(dtos, idSlice.hasNext(), page, size)
    }

    // Single interview detail
    @Transactional(readOnly = true)
    fun getBySlug(slug: String, currentUser: User?): InterviewDetailDto {
        val interview = interviewRepository.findPublishedBySlugWithDetails(slug)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Interview not found")

        val isBookmarked = currentUser != null &&
            bookmarkRepository.existsByUserAndInterview(currentUser, interview)

        return interview.toDetailDto(isBookmarked = isBookmarked)
    }
}
```

---

## 3.12 Common Mistakes in Chapter 3

### Mistake 1 — N+1 on the feed

```kotlin
// ❌ This runs 1 + N queries (one per interview to load company)
val interviews = interviewRepository.findByStatus(InterviewStatus.PUBLISHED)
interviews.forEach { println(it.company.name) }  // Each access hits DB

// ✅ One query loads everything
val interviews = interviewRepository.findByStatusWithCompany(InterviewStatus.PUBLISHED)
interviews.forEach { println(it.company.name) }  // No extra queries
```

### Mistake 2 — JOIN FETCH with Pageable on a collection

```kotlin
// ❌ Hibernate loads ALL records into memory, then paginates in Java
@Query("SELECT DISTINCT i FROM Interview i JOIN FETCH i.tags WHERE i.status = 'PUBLISHED'")
fun findWithTags(pageable: Pageable): Slice<Interview>  // Memory disaster for large tables

// ✅ Two-query approach
// Query 1: paginated IDs (no JOIN FETCH)
// Query 2: full data for those IDs (JOIN FETCH is safe — fixed set)
```

### Mistake 3 — Forgetting @Param

```kotlin
// ❌ Without @Param — Spring cannot match parameter to :status placeholder
@Query("SELECT i FROM Interview i WHERE i.status = :status")
fun findByStatus(status: InterviewStatus): List<Interview>

// ✅ With @Param
@Query("SELECT i FROM Interview i WHERE i.status = :status")
fun findByStatus(@Param("status") status: InterviewStatus): List<Interview>
```

### Mistake 4 — Optional on findById instead of findByIdOrNull

```kotlin
// ❌ Verbose Java-style Optional in Kotlin
val result = interviewRepository.findById(id)
if (result.isEmpty) throw NotFoundException()
val interview = result.get()

// ✅ Clean Kotlin nullable
val interview = interviewRepository.findByIdOrNull(id)
    ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Not found")
```

### Mistake 5 — Calling count() to check existence

```kotlin
// ❌ Loads count from DB — slower than needed
if (interviewRepository.countBySourceUrl(url) > 0) { ... }

// ✅ Database can stop at first match — much faster
if (interviewRepository.existsBySourceUrl(url)) { ... }
```

### Mistake 6 — Forgetting @Transactional on @Modifying queries

```kotlin
// ❌ Throws TransactionRequiredException at runtime
@Modifying
@Query("UPDATE Interview i SET i.status = :status WHERE i.id = :id")
fun updateStatus(...)

// ✅ @Modifying queries need a transaction
@Modifying
@Transactional
@Query("UPDATE Interview i SET i.status = :status WHERE i.id = :id")
fun updateStatus(...)
```

---

## 3.13 HireStory Connection — What You Built in Chapter 3

By the end of this chapter, HireStory has:

- All 8 repositories — `User`, `Company`, `Interview`, `Bookmark`, `Comment`, `ReadHistory`, `CrawlJob`, plus tag support
- A complete feed query with optional filters (company, difficulty, outcome) using the two-query pagination pattern — safe, correct, performant
- Full-text search with PostgreSQL's native search engine
- Entity-to-DTO mappers using Kotlin extension functions
- A complete `InterviewService.getFeed()` and `getBySlug()` that handle the `isBookmarked` flag per user

The feed endpoint is the most complex query in the app. It is now solved correctly.

---

## 3.14 Chapter Project — Build It Before You Move On

### What to build

Extend your Chapter 2 project with a fully working feed and company endpoints.

**Step 1 — Add InterviewRepository**

Write the interview repository with at minimum:

- `findBySlug(slug: String): Interview?`
- `findByStatusOrderByAddedAtDesc(status: InterviewStatus): List<Interview>`
- `existsBySlug(slug: String): Boolean`

**Step 2 — Seed interviews**

Update your `DataInitializer` to create 10 interviews across 3 companies with different difficulties and outcomes.

**Step 3 — Build the feed endpoint**

`GET /api/interviews` — returns a list of published interviews sorted by date.

**Step 4 — Build the detail endpoint**

`GET /api/interviews/{slug}` — returns a single interview. Returns 404 with a proper error body if not found.

**Step 5 — Add filtering**

Add optional `?company={id}&difficulty=HARD&outcome=OFFER` query params. Implement this using `@Query` with nullable parameters.

**Step 6 — Verify with SQL logs**

Enable `show_sql: true` in `application.yml` and count how many queries each endpoint makes. The feed should make 2 queries (IDs + data). The detail should make 1 query (single interview with JOIN FETCH).

### Checkpoint questions — answer before moving on

1. Your feed query currently makes 1 query for IDs and 1 query for data. If you add `LEFT JOIN FETCH i.rounds` to the data query, what problem appears and how do you solve it?
    
2. A user searches for "google". Your search returns 0 results even though you have Google interviews in the database. What are 3 possible reasons?
    
3. You have `findByCompanyAndStatus(company, status)` working. Now you want to add sorting by `addedAt DESC`. How do you modify the method name to add sorting without writing `@Query`?
    
4. Your `DataInitializer` calls `interviewRepository.save(interview)` inside a loop 10 times. Is this efficient? What is the better approach?
    
5. The method `findByIdOrNull` is not defined in `JpaRepository`. Where does it come from and how do you import it?
    

---

_Chapter 4 → REST Controllers and DTOs — Building Your Complete API Layer_

---

> **Book Progress:** Chapter 3 of 15 complete. Chapters ahead: REST API · Spring Security · JWT/Clerk · Redis · RabbitMQ · Spring AI · Jsoup · Scheduler · Testing · Deployment