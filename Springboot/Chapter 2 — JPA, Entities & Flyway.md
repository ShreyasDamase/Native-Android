# Backend Engineering with Spring Boot & Kotlin

## The HireStory Builder's Guide

---

# Chapter 2 — JPA, Entities & Flyway

### _Mapping your database to Kotlin classes — and never losing your data_

---

## 2.1 The Problem JPA Solves

In Node.js with Mongoose, you probably wrote something like this:

```javascript
// Node.js — Mongoose
const interviewSchema = new Schema({
  title: { type: String, required: true },
  company: { type: String, required: true },
  content: String,
  createdAt: { type: Date, default: Date.now }
})

const Interview = mongoose.model('Interview', interviewSchema)

// Saving
const interview = new Interview({ title: 'My Google Interview', company: 'Google' })
await interview.save()

// Finding
const interviews = await Interview.find({ company: 'Google' })
```

Mongoose is an **ODM** — Object Document Mapper. It maps JavaScript objects to MongoDB documents.

**JPA is the same idea for SQL databases.** JPA stands for Jakarta Persistence API. It maps Kotlin classes to PostgreSQL tables. Instead of writing raw SQL like:

```sql
SELECT * FROM interviews WHERE company_id = 5 AND status = 'PUBLISHED' ORDER BY added_at DESC;
```

You write Kotlin:

```kotlin
interviewRepository.findByCompanyIdAndStatusOrderByAddedAtDesc(5L, Status.PUBLISHED)
```

JPA generates the SQL for you. You work with Kotlin objects. The database does not care — it still gets SQL. You just never had to write it.

> **⚠️ Important distinction:** JPA is a **specification** — a set of rules about how Java/Kotlin objects should be mapped to relational databases. **Hibernate** is the **implementation** that actually does the work — it is the library that reads your annotations and generates SQL. Spring Boot automatically uses Hibernate as the JPA implementation. When you see "JPA" in documentation, the actual work is done by Hibernate underneath.

---

## 2.2 Flyway — Never Lose Your Data

Before we write a single Entity, you need to understand Flyway. This is non-negotiable for a production application.

### The Problem Without Flyway

In development, many beginners use:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create    # ← THE MOST DANGEROUS SETTING IN SPRING BOOT
```

This tells Hibernate: _every time the app starts, drop all tables and recreate them from your Entity classes._

What happens when you deploy this to production? **All your data is deleted.** Every user. Every interview. Every bookmark. Gone.

Even in development it causes problems. You add a new field to your Entity, restart the app, and your test data is wiped. You seed the database again. This wastes time constantly.

### The Solution — Flyway

Flyway is a **database migration tool**. Here is how it thinks:

- Your database has a history of changes
- Each change is a numbered SQL file: `V1__create_tables.sql`, `V2__add_slug_column.sql`
- Flyway tracks which files have already run in a special table called `flyway_schema_history`
- On startup, Flyway runs any files that have not run yet
- Files that have already run are **never touched again**

This means:

- Your data is **never deleted**
- Every developer on the team has the exact same database state
- Deploying to production applies only the new changes
- You can see the full history of every database change ever made

### Flyway File Naming — The Rule You Cannot Break

```
V{version}__{description}.sql
 ↑          ↑↑
 V is       Two underscores
 required   (double underscore)

Examples:
V1__create_initial_schema.sql
V2__add_slug_to_articles.sql
V3__create_tags_table.sql
V4__add_search_vector_index.sql
```

**Breaking this naming convention means Flyway ignores the file silently.** This is one of the most common sources of confusion for beginners — they add a migration file and nothing happens, because they used one underscore instead of two.

### Where Migration Files Live

```
src/
└── main/
    └── resources/
        └── db/
            └── migration/
                ├── V1__create_initial_schema.sql   ← Run first, run once, never again
                ├── V2__add_slug_column.sql          ← Run after V1, once, never again
                └── V3__create_indexes.sql           ← Run after V2, once, never again
```

The path `db/migration` matches your `application.yml`:

```yaml
spring:
  flyway:
    locations: classpath:db/migration
```

### The Golden Rule of Flyway

> **Never edit a migration file that has already been run.**

Once `V1__create_initial_schema.sql` has run on any environment — your machine, your teammate's machine, production — it is frozen. If you need to change the schema, add a new file: `V2__fix_column_name.sql`.

If you edit an existing migration file after it has run, Flyway detects the checksum mismatch and **refuses to start your application**. You will see:

```
FlywayException: Validate failed: Migration checksum mismatch for migration version 1
```

This is intentional — Flyway is protecting you from accidentally changing a migration that already ran in production.

---

## 2.3 Your First Migration — The HireStory Schema

Let us write the complete schema for HireStory. This is your `V1__create_initial_schema.sql`:

```sql
-- src/main/resources/db/migration/V1__create_initial_schema.sql
-- This file runs ONCE and is never touched again.

-- ── USERS ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    clerk_id        VARCHAR(255) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    avatar_url      TEXT,
    is_premium      BOOLEAN NOT NULL DEFAULT FALSE,
    read_count      INTEGER NOT NULL DEFAULT 0,
    referral_code   VARCHAR(20) NOT NULL UNIQUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── COMPANIES ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS companies (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL UNIQUE,
    logo_url        TEXT,
    slug            VARCHAR(255) NOT NULL UNIQUE,
    interview_count INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── INTERVIEWS ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS interviews (
    id                BIGSERIAL PRIMARY KEY,
    title             VARCHAR(255) NOT NULL,
    headline          VARCHAR(500) NOT NULL,
    content           TEXT NOT NULL,
    role              VARCHAR(255) NOT NULL,
    location          VARCHAR(255),
    experience_years  INTEGER NOT NULL,
    rounds_count      INTEGER NOT NULL DEFAULT 0,
    difficulty        VARCHAR(20) NOT NULL,
    outcome           VARCHAR(20) NOT NULL,
    salary_lpa        DECIMAL(10, 2),
    source_url        TEXT UNIQUE,
    source_type       VARCHAR(20) NOT NULL DEFAULT 'USER_SUBMITTED',
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    confidence_score  INTEGER,
    slug              VARCHAR(255) NOT NULL UNIQUE,
    company_id        BIGINT NOT NULL REFERENCES companies(id),
    user_id           BIGINT REFERENCES users(id),   -- NULL for crawled interviews
    added_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    published_at      TIMESTAMP
);

-- ── INTERVIEW ROUNDS ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS interview_rounds (
    id            BIGSERIAL PRIMARY KEY,
    interview_id  BIGINT NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    round_number  INTEGER NOT NULL,
    title         VARCHAR(255) NOT NULL,
    questions     TEXT NOT NULL,
    difficulty    VARCHAR(20),
    notes         TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── TAGS ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tags (
    id    BIGSERIAL PRIMARY KEY,
    name  VARCHAR(100) NOT NULL UNIQUE
);

-- ── INTERVIEW_TAGS (junction table) ────────────────────────────────
CREATE TABLE IF NOT EXISTS interview_tags (
    interview_id  BIGINT NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    tag_id        BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (interview_id, tag_id)
);

-- ── BOOKMARKS ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bookmarks (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    interview_id  BIGINT NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, interview_id)   -- Cannot bookmark same interview twice
);

-- ── COMMENTS ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS comments (
    id            BIGSERIAL PRIMARY KEY,
    interview_id  BIGINT NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    user_id       BIGINT NOT NULL REFERENCES users(id),
    content       TEXT NOT NULL,
    is_deleted    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── READ HISTORY ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS read_history (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    interview_id  BIGINT NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    read_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, interview_id)   -- One record per user per interview
);

-- ── REFERRALS ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS referrals (
    id           BIGSERIAL PRIMARY KEY,
    referrer_id  BIGINT NOT NULL REFERENCES users(id),
    referred_id  BIGINT NOT NULL REFERENCES users(id),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── NOTIFICATIONS ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type       VARCHAR(50) NOT NULL,
    title      VARCHAR(255) NOT NULL,
    body       TEXT NOT NULL,
    deep_link  VARCHAR(500),
    is_read    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── FCM TOKENS ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS fcm_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      TEXT NOT NULL UNIQUE,
    platform   VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── CRAWL JOBS ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS crawl_jobs (
    id            BIGSERIAL PRIMARY KEY,
    source_url    TEXT NOT NULL UNIQUE,
    raw_text      TEXT,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    confidence_score INTEGER,
    interview_id  BIGINT REFERENCES interviews(id),
    error_message TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at  TIMESTAMP
);
```

Now the indexes — add this as a second migration file:

```sql
-- src/main/resources/db/migration/V2__create_indexes.sql

-- Interviews are filtered by these columns constantly
CREATE INDEX idx_interviews_company_id   ON interviews(company_id);
CREATE INDEX idx_interviews_status       ON interviews(status);
CREATE INDEX idx_interviews_difficulty   ON interviews(difficulty);
CREATE INDEX idx_interviews_outcome      ON interviews(outcome);
CREATE INDEX idx_interviews_added_at     ON interviews(added_at DESC);

-- Bookmarks and read history are always queried by user
CREATE INDEX idx_bookmarks_user_id       ON bookmarks(user_id);
CREATE INDEX idx_read_history_user_id    ON read_history(user_id);

-- Crawler deduplication
CREATE UNIQUE INDEX idx_crawl_jobs_source_url ON crawl_jobs(source_url);

-- Notifications queried by user, sorted by date
CREATE INDEX idx_notifications_user_id   ON notifications(user_id);
```

> **💡 Why separate files?** V1 creates the tables. V2 creates the indexes. If you need to add a new index later, you add V3 — you never touch V1 or V2 again. This is the Flyway mindset: every change is additive, never destructive.

---

## 2.4 Understanding JPA Entities

An Entity is a Kotlin class that maps to a database table. Each field maps to a column. Each instance of the class maps to a row.

Let us start with the simplest possible entity and build from there:

```kotlin
// The simplest possible entity — a Company
@Entity                          // "This class maps to a database table"
@Table(name = "companies")       // "The table is called 'companies'"
class Company(

    @Id                          // "This is the primary key"
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // IDENTITY = use the database's auto-increment (BIGSERIAL in PostgreSQL)
    val id: Long? = null,

    val name: String,            // Maps to column "name" automatically
    val logoUrl: String? = null, // Maps to column "logo_url" (Spring converts camelCase → snake_case)
    val slug: String,
    val interviewCount: Int = 0,

    @Column(name = "created_at") // Explicit column name (optional — Spring would figure this out)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

### The Column Name Mapping Rule

Spring Boot automatically converts `camelCase` Kotlin field names to `snake_case` database column names. You almost never need `@Column(name = "...")`.

|Kotlin Field Name|Database Column Name|
|---|---|
|`name`|`name`|
|`logoUrl`|`logo_url`|
|`interviewCount`|`interview_count`|
|`addedAt`|`added_at`|
|`isPremium`|`is_premium`|
|`sourceUrl`|`source_url`|

This works automatically. Only use `@Column(name = "...")` when the actual database column name does not follow this pattern — for example, a legacy database with unusual naming.

---

## 2.5 JPA Annotations — Every One You Need for HireStory

### @Entity and @Table

```kotlin
@Entity          // Required — marks this as a JPA entity
@Table(
    name = "interviews",    // Table name
    uniqueConstraints = [
        // Matches the UNIQUE constraint in your SQL migration
        UniqueConstraint(columnNames = ["slug"]),
        UniqueConstraint(columnNames = ["source_url"])
    ]
)
class Interview(...)
```

### @Id and @GeneratedValue

```kotlin
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
val id: Long? = null
// Why nullable? Because when you CREATE a new Interview object in Kotlin,
// it does not have an ID yet — the database assigns it on INSERT.
// After saving, the id field is populated by JPA automatically.
```

The `id` is `Long?` (nullable) because:

- Before saving: `interview.id == null`
- After saving: `interview.id == 1L` (database assigned it)

### @Column — Only When You Need It

```kotlin
// You need @Column when:

// 1. Setting length constraints
@Column(length = 500)
val headline: String,

// 2. Marking a column as not nullable at JPA level (in addition to Kotlin's type system)
@Column(nullable = false)
val title: String,

// 3. Preventing a field from being updated after creation
@Column(updatable = false)
val createdAt: LocalDateTime = LocalDateTime.now(),

// 4. Setting precision for decimals
@Column(precision = 10, scale = 2)
val salaryLpa: Double? = null,
```

### @Enumerated — Storing Enums

Your interviews have `difficulty`, `outcome`, `status`, and `sourceType` as enums. Here is how to store them:

```kotlin
// Define your enums
enum class Difficulty { EASY, MEDIUM, HARD }
enum class Outcome { OFFER, REJECTED, GHOSTED }
enum class InterviewStatus { PENDING, PUBLISHED, REJECTED }
enum class SourceType { USER_SUBMITTED, CRAWLED }

// Use them in your Entity
@Enumerated(EnumType.STRING)
// EnumType.STRING stores "EASY", "MEDIUM", "HARD" in the database
// EnumType.ORDINAL stores 0, 1, 2 — NEVER use this
// Why? If you reorder your enum values, ORDINAL breaks all existing data
val difficulty: Difficulty,
```

> **⚠️ Always use `EnumType.STRING`, never `EnumType.ORDINAL`.** `ORDINAL` stores 0, 1, 2 instead of "EASY", "MEDIUM", "HARD". If you ever add a new enum value in the middle of the list, all existing data becomes corrupted. `STRING` is always safe.

---

## 2.6 Entity Relationships — The Most Important Section in This Chapter

Relationships are where most Spring Boot beginners get confused. Take your time here. Read each section twice.

Your HireStory database has these relationships:

- One Company → many Interviews
- One Interview → many Rounds
- One Interview → many Tags (and one Tag → many Interviews)
- One User → many Bookmarks
- One Interview → many Bookmarks

### @ManyToOne — "Many of me belong to one of them"

This is the most common relationship. Many Interviews belong to one Company.

```kotlin
@Entity
@Table(name = "interviews")
class Interview(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val title: String,
    val content: String,

    // Many Interviews belong to ONE Company
    @ManyToOne(fetch = FetchType.LAZY)    // Explained below
    @JoinColumn(name = "company_id")      // The foreign key column in THIS table
    val company: Company,

    // Many Interviews belong to ONE User (nullable — crawled interviews have no user)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User? = null,

    // ... other fields
)
```

`@JoinColumn(name = "company_id")` tells JPA: _the foreign key in the `interviews` table is the column called `company_id`_.

### @OneToMany — "One of me has many of them"

One Company has many Interviews. But you need to be careful here — this relationship is usually **not what you want to map** in your Entity.

```kotlin
@Entity
@Table(name = "companies")
class Company(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val name: String,
    val slug: String,

    // ONE Company has MANY Interviews
    // mappedBy = "company" means: the Interview entity's field named "company" owns this relationship
    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    val interviews: List<Interview> = emptyList()
    // ⚠️ Read Section 2.7 before using @OneToMany
)
```

### @ManyToMany — "Many of me relate to many of them"

Interviews can have many Tags, and Tags can belong to many Interviews. This uses a junction table (`interview_tags` in your schema).

```kotlin
@Entity
@Table(name = "interviews")
class Interview(
    // ...

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "interview_tags",              // The junction table name
        joinColumns = [JoinColumn(name = "interview_id")],      // FK to this entity
        inverseJoinColumns = [JoinColumn(name = "tag_id")]       // FK to the other entity
    )
    val tags: MutableList<Tag> = mutableListOf()
)

@Entity
@Table(name = "tags")
class Tag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val name: String,

    // The other side of the relationship
    // mappedBy = "tags" — the Interview entity's field named "tags" owns this
    @ManyToMany(mappedBy = "tags")
    val interviews: MutableList<Interview> = mutableListOf()
)
```

---

## 2.7 FetchType — The Setting That Causes 90% of JPA Performance Problems

This is the most misunderstood concept in JPA. Understanding it properly will save you from the most common performance disaster in Spring Boot applications.

### What FetchType Controls

When you load an Interview from the database, should JPA also automatically load the related Company? The related User? The related list of Rounds? The list of Tags?

FetchType controls the answer:

- `FetchType.EAGER` — Yes, load everything immediately in one big query
- `FetchType.LAZY` — No, load it only when I actually access it in code

### The N+1 Problem — What EAGER Causes

Imagine you load 20 interviews for the feed page. With `FetchType.EAGER` on the `company` field:

```
Query 1:  SELECT * FROM interviews LIMIT 20
Query 2:  SELECT * FROM companies WHERE id = 1   ← For interview 1
Query 3:  SELECT * FROM companies WHERE id = 2   ← For interview 2
Query 4:  SELECT * FROM companies WHERE id = 5   ← For interview 3
...
Query 21: SELECT * FROM companies WHERE id = 8   ← For interview 20
```

21 queries for 20 interviews. For 100 interviews it is 101 queries. For 1000 it is 1001 queries. This is the **N+1 problem**and it silently destroys the performance of Spring Boot applications.

### Always Use FetchType.LAZY

```kotlin
// ✅ CORRECT — Lazy everywhere
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "company_id")
val company: Company,

@OneToMany(mappedBy = "interview", fetch = FetchType.LAZY)
val rounds: List<Round> = emptyList(),

@ManyToMany(fetch = FetchType.LAZY)
val tags: MutableList<Tag> = mutableListOf()
```

With LAZY, JPA only loads the company data when your code actually accesses `interview.company`. If you never access it, no extra query runs.

### How to Load Related Data Efficiently When You Need It

When you need the company along with the interview, use a JOIN FETCH in your query — covered in Chapter 3:

```kotlin
// One query loads interviews AND their companies
@Query("SELECT i FROM Interview i JOIN FETCH i.company WHERE i.status = :status")
fun findAllWithCompany(status: InterviewStatus): List<Interview>
```

This gives you exactly what you need in one query instead of N+1 queries.

> **📝 Note:** The `open-in-view: false` setting in your `application.yml` from Chapter 1 is related to this. When `open-in-view` is `true` (Spring Boot's problematic default), the database session stays open for the entire HTTP request, making lazy loading work from anywhere — including your templates. This hides the N+1 problem rather than fixing it. Setting it to `false` forces you to be explicit about what data you load, which makes your app faster and your queries predictable.

---

## 2.8 The Complete HireStory Entities

Now let us write all the entities for HireStory. These map directly to the tables in your `V1` migration.

First, create a file for all your enums:

```kotlin
// src/main/kotlin/com/example/hirestory/entity/Enums.kt

package com.example.hirestory.entity

enum class Difficulty {
    EASY, MEDIUM, HARD
}

enum class Outcome {
    OFFER, REJECTED, GHOSTED
}

enum class InterviewStatus {
    PENDING, PUBLISHED, REJECTED
}

enum class SourceType {
    USER_SUBMITTED, CRAWLED
}

enum class CrawlStatus {
    PENDING, PROCESSING, DONE, FAILED
}

enum class NotificationType {
    INTERVIEW_PUBLISHED,
    COMMENT_RECEIVED,
    REFERRAL_USED,
    NEW_INTERVIEW_FOR_TARGET_COMPANY,
    ONBOARDING_DAY_1,
    ONBOARDING_DAY_3,
    ONBOARDING_DAY_7
}

enum class Platform {
    ANDROID, IOS
}
```

Now the entities:

```kotlin
// src/main/kotlin/com/example/hirestory/entity/User.kt

package com.example.hirestory.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // Clerk's user ID — this is how you identify users from JWT tokens
    @Column(name = "clerk_id", nullable = false, unique = true)
    val clerkId: String,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    var name: String,      // var because users can update their name

    @Column(name = "avatar_url")
    var avatarUrl: String? = null,

    @Column(name = "is_premium", nullable = false)
    var isPremium: Boolean = false,

    // This is tracked in Redis for speed, but also stored here as backup
    @Column(name = "read_count", nullable = false)
    var readCount: Int = 0,

    @Column(name = "referral_code", nullable = false, unique = true)
    val referralCode: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

```kotlin
// src/main/kotlin/com/example/hirestory/entity/Company.kt

package com.example.hirestory.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "companies")
class Company(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val name: String,

    @Column(name = "logo_url")
    var logoUrl: String? = null,

    // URL-friendly version of the name: "Google" → "google"
    @Column(nullable = false, unique = true)
    val slug: String,

    // Denormalised count — kept in sync by a service when interviews are published
    // Faster than COUNT(*) on every request
    @Column(name = "interview_count", nullable = false)
    var interviewCount: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

```kotlin
// src/main/kotlin/com/example/hirestory/entity/Interview.kt

package com.example.hirestory.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "interviews")
class Interview(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, length = 500)
    val headline: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(nullable = false)
    val role: String,

    val location: String? = null,

    @Column(name = "experience_years", nullable = false)
    val experienceYears: Int,

    @Column(name = "rounds_count", nullable = false)
    var roundsCount: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val difficulty: Difficulty,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val outcome: Outcome,

    @Column(name = "salary_lpa", precision = 10, scale = 2)
    val salaryLpa: Double? = null,

    @Column(name = "source_url", unique = true, columnDefinition = "TEXT")
    val sourceUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    val sourceType: SourceType = SourceType.USER_SUBMITTED,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: InterviewStatus = InterviewStatus.PENDING,

    @Column(name = "confidence_score")
    var confidenceScore: Int? = null,

    @Column(nullable = false, unique = true)
    val slug: String,

    // Relationships — all LAZY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    val company: Company,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User? = null,

    @OneToMany(
        mappedBy = "interview",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],   // Deleting interview deletes all its rounds
        orphanRemoval = true
    )
    val rounds: MutableList<InterviewRound> = mutableListOf(),

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "interview_tags",
        joinColumns = [JoinColumn(name = "interview_id")],
        inverseJoinColumns = [JoinColumn(name = "tag_id")]
    )
    val tags: MutableList<Tag> = mutableListOf(),

    @Column(name = "added_at", nullable = false, updatable = false)
    val addedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "published_at")
    var publishedAt: LocalDateTime? = null
)
```

```kotlin
// src/main/kotlin/com/example/hirestory/entity/InterviewRound.kt

package com.example.hirestory.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "interview_rounds")
class InterviewRound(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // This round belongs to one Interview
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    val interview: Interview,

    @Column(name = "round_number", nullable = false)
    val roundNumber: Int,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val questions: String,

    val difficulty: String? = null,

    @Column(columnDefinition = "TEXT")
    val notes: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

```kotlin
// src/main/kotlin/com/example/hirestory/entity/Tag.kt

package com.example.hirestory.entity

import jakarta.persistence.*

@Entity
@Table(name = "tags")
class Tag(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val name: String    // "DSA", "System Design", "HR", "Behavioral", "SQL"
)
```

```kotlin
// src/main/kotlin/com/example/hirestory/entity/Bookmark.kt

package com.example.hirestory.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "bookmarks",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "interview_id"])]
)
class Bookmark(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    val interview: Interview,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

```kotlin
// src/main/kotlin/com/example/hirestory/entity/Comment.kt

package com.example.hirestory.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "comments")
class Comment(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    val interview: Interview,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

```kotlin
// src/main/kotlin/com/example/hirestory/entity/ReadHistory.kt

package com.example.hirestory.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "read_history",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "interview_id"])]
)
class ReadHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    val interview: Interview,

    @Column(name = "read_at", nullable = false, updatable = false)
    val readAt: LocalDateTime = LocalDateTime.now()
)
```

```kotlin
// src/main/kotlin/com/example/hirestory/entity/CrawlJob.kt

package com.example.hirestory.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "crawl_jobs")
class CrawlJob(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "source_url", nullable = false, unique = true, columnDefinition = "TEXT")
    val sourceUrl: String,

    @Column(name = "raw_text", columnDefinition = "TEXT")
    var rawText: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: CrawlStatus = CrawlStatus.PENDING,

    @Column(name = "confidence_score")
    var confidenceScore: Int? = null,

    // The interview created from this crawl job (null until processed)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    var interview: Interview? = null,

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "processed_at")
    var processedAt: LocalDateTime? = null
)
```

---

## 2.9 Why Not Data Classes for Entities?

You might be wondering: _"I know Kotlin. Entities look like they should be `data class`. Why are they regular classes?"_

This is an important question. The answer comes down to how JPA works internally.

```kotlin
// ❌ PROBLEMATIC — data class entity
@Entity
data class Interview(
    @Id val id: Long? = null,
    val title: String,
    @ManyToOne val company: Company
)
```

Problems with `data class` for JPA entities:

**1. `equals()` and `hashCode()` are generated from all fields** `data class` generates `equals()` based on all properties. JPA entities are considered equal if they have the same ID. A `data class` would say two interviews are different if any field differs, even if they are the same row with one field temporarily in a different state during a transaction.

**2. `copy()` bypasses JPA tracking** JPA tracks changes to entities (this is called the "dirty checking" mechanism). When you use `copy()`, JPA does not know the object changed and will not save the changes.

**3. Lazy loading breaks with `data class`** The `toString()` generated by `data class` accesses all fields, including lazy-loaded relationships. This triggers database queries unexpectedly — including inside log statements.

**The correct approach:**

```kotlin
// ✅ CORRECT — Regular class with only what you need
@Entity
class Interview(
    @Id val id: Long? = null,
    val title: String,
    // ...
) {
    // Custom equals/hashCode based only on id
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Interview) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
```

For most entities, you do not even need to override `equals` and `hashCode`. Just use regular classes and let JPA handle identity tracking.

---

## 2.10 Seeding Data — The DataInitializer

For development, you need test data. Create a component that runs on startup and seeds the database if it is empty:

```kotlin
// src/main/kotlin/com/example/hirestory/config/DataInitializer.kt

package com.example.hirestory.config

import com.example.hirestory.entity.*
import com.example.hirestory.repository.*
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("dev")    // Only runs when active profile is "dev" — never in production
class DataInitializer(
    private val companyRepository: CompanyRepository,
    private val interviewRepository: InterviewRepository,
    private val tagRepository: TagRepository,
    private val userRepository: UserRepository
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        // Guard: do not seed if data already exists
        if (companyRepository.count() > 0) {
            println("Database already has data — skipping seed")
            return
        }

        println("Seeding development database...")

        // Create companies
        val google = companyRepository.save(
            Company(name = "Google", slug = "google", logoUrl = "https://logo.clearbit.com/google.com")
        )
        val amazon = companyRepository.save(
            Company(name = "Amazon", slug = "amazon", logoUrl = "https://logo.clearbit.com/amazon.com")
        )
        val microsoft = companyRepository.save(
            Company(name = "Microsoft", slug = "microsoft")
        )

        // Create tags
        val dsaTag = tagRepository.save(Tag(name = "DSA"))
        val systemDesignTag = tagRepository.save(Tag(name = "System Design"))
        val hrTag = tagRepository.save(Tag(name = "HR"))
        val sqlTag = tagRepository.save(Tag(name = "SQL"))

        // Create a test user
        val testUser = userRepository.save(
            User(
                clerkId = "user_test_123",
                email = "test@hirestory.com",
                name = "Test User",
                referralCode = "TEST123"
            )
        )

        // Create interviews
        val interview1 = Interview(
            title = "Google SDE-1 Interview Experience — Bangalore 2024",
            headline = "Got an offer after 5 rounds. Focus on graphs and dynamic programming.",
            content = """
                Applied through LinkedIn referral. Got a response within 2 weeks.
                
                Round 1 — Online Assessment: 2 LeetCode medium problems in 90 minutes.
                Both were graph problems. One was a BFS/DFS, one was dynamic programming.
                
                Round 2 — Technical Phone Screen: Discussed time and space complexity.
                Interviewer was helpful. Asked about my projects.
                
                ...more content here...
            """.trimIndent(),
            role = "Software Development Engineer - I",
            location = "Bangalore",
            experienceYears = 2,
            difficulty = Difficulty.HARD,
            outcome = Outcome.OFFER,
            salaryLpa = 28.0,
            slug = "google-sde-1-bangalore-2024",
            status = InterviewStatus.PUBLISHED,
            company = google,
            user = testUser
        )
        interview1.tags.addAll(listOf(dsaTag, systemDesignTag))
        interviewRepository.save(interview1)

        val interview2 = Interview(
            title = "Amazon SDE-2 Interview — Hyderabad",
            headline = "Rejected after final round. Leadership principles matter a lot.",
            content = "Full content of the interview experience here...",
            role = "Software Development Engineer - II",
            location = "Hyderabad",
            experienceYears = 4,
            difficulty = Difficulty.MEDIUM,
            outcome = Outcome.REJECTED,
            slug = "amazon-sde-2-hyderabad-2024",
            status = InterviewStatus.PUBLISHED,
            company = amazon,
            user = testUser
        )
        interview2.tags.addAll(listOf(dsaTag, hrTag))
        interviewRepository.save(interview2)

        println("Seeding complete. Created ${companyRepository.count()} companies, ${interviewRepository.count()} interviews")
    }
}
```

---

## 2.11 The @Transactional Annotation — When Data Must Be All-Or-Nothing

This concept confused you in the "areas I'm least confident about" — so let us make it completely clear.

### The Problem Without Transactions

Imagine a user submits an interview. You need to:

1. Save the Interview record
2. Update the Company's `interview_count`
3. Save all the InterviewRounds

```kotlin
// What happens if step 2 fails?
interviewRepository.save(interview)        // ✅ Saved
companyRepository.save(updatedCompany)     // ❌ FAILS — network timeout, constraint violation, anything
roundRepository.saveAll(rounds)            // ← Never reached
```

Now you have an interview in the database but the company's count is wrong and no rounds were saved. Your data is **inconsistent**. This is a serious problem.

### The Solution — @Transactional

```kotlin
@Service
class InterviewService(
    private val interviewRepository: InterviewRepository,
    private val companyRepository: CompanyRepository,
    private val roundRepository: RoundRepository
) {

    @Transactional    // "Treat everything in this method as ONE unit"
    fun submitInterview(dto: CreateInterviewDto): Interview {
        val interview = interviewRepository.save(toEntity(dto))

        // If this line throws any exception:
        val company = companyRepository.findById(dto.companyId).orElseThrow()
        company.interviewCount++
        companyRepository.save(company)

        // ALL changes are rolled back — interview save is undone too
        // The database is left exactly as it was before this method was called
        roundRepository.saveAll(dto.rounds.map { toRoundEntity(it, interview) })

        return interview
        // If we reach here with no exceptions: ALL changes are committed together
    }
}
```

`@Transactional` wraps your entire method in a database transaction:

- If the method completes without throwing: **all changes are committed** to the database together
- If the method throws any exception: **all changes are rolled back** — the database is restored to its state before the method was called

### Where to Put @Transactional

```kotlin
// ✅ CORRECT — On Service methods (the layer that coordinates operations)
@Service
class InterviewService {

    @Transactional
    fun submitInterview(dto: CreateInterviewDto): Interview { ... }

    @Transactional(readOnly = true)    // Use for read operations — slight performance improvement
    fun findAll(): List<Interview> { ... }
}

// ❌ WRONG — On Controller methods
@RestController
class InterviewController {
    @Transactional    // Transactions belong in the Service layer, not here
    @PostMapping
    fun submit(...) { ... }
}

// ❌ WRONG — On Repository methods you write yourself
// (Spring Data already wraps repository methods in transactions automatically)
```

### readOnly = true

```kotlin
@Transactional(readOnly = true)
fun findAll(): List<Interview> {
    return interviewRepository.findAll()
}
```

When `readOnly = true`, Spring tells the database: _this transaction will only read, not write_. The database can then:

- Skip tracking which rows changed (no dirty checking needed)
- Use read replicas if you have them configured
- Apply certain optimisations

Always use `readOnly = true` on methods that only read data.

---

## 2.12 @Transactional Rollback — What Triggers It

By default, `@Transactional` only rolls back for `RuntimeException` and `Error`. It does NOT roll back for checked exceptions.

```kotlin
// This WILL rollback — RuntimeException
@Transactional
fun create(dto: CreateInterviewDto) {
    interviewRepository.save(toEntity(dto))
    throw IllegalStateException("Something went wrong")   // ← Rollback happens
}

// This will NOT rollback by default — checked exception
@Transactional
fun create(dto: CreateInterviewDto) {
    interviewRepository.save(toEntity(dto))
    throw IOException("Network issue")   // ← No rollback! Data is committed.
}

// Fix: specify which exceptions should trigger rollback
@Transactional(rollbackFor = [IOException::class])
fun create(dto: CreateInterviewDto) {
    interviewRepository.save(toEntity(dto))
    throw IOException("Network issue")   // ← Now rollback happens
}
```

In Kotlin and Spring Boot, almost all exceptions you throw will be `RuntimeException` subclasses — so the default behaviour covers most cases. But it is good to know this rule.

---

## 2.13 Common Mistakes in Chapter 2

### Mistake 1 — Editing a migration file that has already run

```
❌ Editing V1__create_initial_schema.sql after it has run anywhere

Result: FlywayException on next startup — app refuses to start
Fix: Add a new file V3__fix_column_type.sql with ALTER TABLE
```

### Mistake 2 — Using EnumType.ORDINAL

```kotlin
// ❌ NEVER do this
@Enumerated(EnumType.ORDINAL)     // Stores 0, 1, 2 — fragile
val difficulty: Difficulty

// ✅ Always
@Enumerated(EnumType.STRING)      // Stores "EASY", "MEDIUM", "HARD" — safe
val difficulty: Difficulty
```

### Mistake 3 — Using ddl-auto: create instead of Flyway

```yaml
# ❌ This deletes all your data on every restart
spring:
  jpa:
    hibernate:
      ddl-auto: create

# ✅ Use Flyway, set this to validate
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

### Mistake 4 — Forgetting CascadeType on OneToMany

```kotlin
// ❌ Without cascade — deleting Interview does not delete its Rounds
@OneToMany(mappedBy = "interview", fetch = FetchType.LAZY)
val rounds: MutableList<InterviewRound> = mutableListOf()

// ✅ With cascade — deleting Interview automatically deletes all its Rounds
@OneToMany(
    mappedBy = "interview",
    fetch = FetchType.LAZY,
    cascade = [CascadeType.ALL],
    orphanRemoval = true
)
val rounds: MutableList<InterviewRound> = mutableListOf()
```

### Mistake 5 — Using data class for Entity

```kotlin
// ❌ Causes lazy loading issues, incorrect equals/hashCode
@Entity
data class Interview(...)

// ✅ Regular class
@Entity
class Interview(...)
```

### Mistake 6 — @Transactional on a private method

```kotlin
// ❌ @Transactional has NO effect on private methods
// Spring cannot create a proxy for private methods
@Transactional
private fun internalSave(interview: Interview) { ... }

// ✅ @Transactional must be on public methods
@Transactional
fun save(interview: Interview) { ... }
```

---

## 2.14 HireStory Connection — What You Built In Chapter 2

By the end of this chapter, HireStory has:

- A complete database schema in `V1__create_initial_schema.sql` — all 14 tables
- All indexes in `V2__create_indexes.sql` for query performance
- All JPA entity classes mapped to those tables
- All enum types defined and used correctly with `EnumType.STRING`
- All relationships mapped with `FetchType.LAZY` everywhere
- A `DataInitializer` that seeds development data automatically
- Understanding of `@Transactional` — when to use it and why

When you run the application now, Flyway runs both migration files, creates all 14 tables and all indexes, and `DataInitializer` seeds Google, Amazon, Microsoft, and two sample interviews. You can open IntelliJ's database browser and see real tables with real data.

---

## 2.15 Chapter Project — Build It Before You Move On

### What to build

Add a database to your Chapter 1 project. Replace the hardcoded company list with real database data.

**Step 1 — Write the migration**

Create `V1__create_companies.sql` in `db/migration`:

```sql
CREATE TABLE IF NOT EXISTS companies (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL UNIQUE,
    logo_url        TEXT,
    slug            VARCHAR(255) NOT NULL UNIQUE,
    interview_count INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
```

**Step 2 — Create the Entity**

Write `Company.kt` with all the correct annotations.

**Step 3 — Create the Repository**

```kotlin
interface CompanyRepository : JpaRepository<Company, Long> {
    fun findBySlug(slug: String): Company?
}
```

_(Repositories are covered in full in Chapter 3. For now just write this and it will work — Spring generates the implementation.)_

**Step 4 — Update the Service**

Replace the hardcoded list in `CompanyService` with real database calls.

**Step 5 — Update application.yml**

Connect to a real PostgreSQL database. Set `ddl-auto: validate`. Enable Flyway.

**Step 6 — Seed data**

Write a `DataInitializer` that creates 5 companies on first startup.

### What success looks like

- App starts and Flyway runs — check logs for "Successfully applied 1 migration"
- `GET /api/companies` returns 5 companies from the database
- `GET /api/companies/1` returns one company
- `GET /api/companies/999` returns a proper 404
- Open IntelliJ database browser — see the `companies` table with your data
- Restart the app — data is still there (Flyway does not recreate the table)

### Checkpoint questions — answer before moving on

1. What does Flyway's `flyway_schema_history` table store and why does it exist?
2. You add a new column `website_url` to your Company entity in Kotlin. The app crashes on startup with "Schema-validation: missing column". What do you do to fix it without losing data?
3. Why is `FetchType.EAGER` dangerous? Describe the N+1 problem in your own words.
4. A user submits an interview. Your service saves the interview, then tries to update the company's `interview_count`. The company update fails with a database error. With `@Transactional`, what happens to the interview that was already saved?
5. Your `DataInitializer` runs every time the app starts and tries to insert duplicate companies — causing a unique constraint error. How do you fix this?

---

_Chapter 3 → Repositories and Queries — Talking to Your Database_

---

> **Book Progress:** Chapter 2 of 15 complete. Chapters ahead: Repositories & Queries · REST API · Spring Security · JWT/Clerk · Redis · RabbitMQ · Spring AI · Jsoup · Scheduler · Testing · Deployment