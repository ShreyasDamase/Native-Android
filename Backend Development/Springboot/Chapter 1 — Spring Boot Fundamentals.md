# Backend Engineering with Spring Boot & Kotlin

## The HireStory Builder's Guide

_From Node.js Developer to Spring Boot Professional_

---

> **How to use this book:** Every chapter follows the same structure — The Problem → The Theory → The Code → Node.js Comparison → Common Mistakes → HireStory Connection → Chapter Project. Never move to the next chapter without completing the Chapter Project. It is small on purpose. It is a checkpoint.

---

# Chapter 1 — Spring Boot Fundamentals

### _How the framework thinks — and how to think like it_

---

## 1.1 The Problem Spring Boot Solves

Before Spring Boot existed (before 2014), building a Java backend meant spending **days on configuration** before writing a single line of business logic. You had to manually wire every component together, write XML files hundreds of lines long, package your app into a WAR file, and deploy it to a separately installed application server like Tomcat or JBoss.

Spring Boot's answer was simple:

> _What if the framework made all the sensible decisions for you by default, and only asked you to override the decisions that your specific application needs to change?_

This idea is called **convention over configuration**. You have seen it before — Rails does this, and Express does it to some degree. Spring Boot took a much more opinionated approach and the result is that a modern Spring Boot app goes from zero to a running web server in under five minutes.

---

## 1.2 Spring Framework vs Spring Boot — The Distinction That Matters

Most tutorials use these terms interchangeably. They are not the same thing, and understanding the difference will save you real confusion when reading documentation.

||Spring Framework|Spring Boot|
|---|---|---|
|**Released**|2003 — the original toolkit|2014 — built on top of Spring Framework|
|**What it gives you**|The building blocks: DI, MVC, Data, Security|Auto-configures those building blocks based on your dependencies|
|**Configuration**|You configure everything explicitly|You only configure what differs from the default|
|**Web server**|You manage your own Tomcat/Jetty separately|Tomcat is embedded — your app IS the server|
|**Packaging**|WAR file, deployed to a server|JAR file — run it with `java -jar`|
|**Boilerplate**|Lots of XML or Java Config|`application.yml` replaces almost all of it|

Think of Spring Framework as a collection of powerful LEGO bricks. Spring Boot is someone who pre-assembled a house from those bricks — you can still move walls, but you do not have to start from scratch.

> **⚠️ Important:** When you Google a Spring problem and find an article from 2015 or earlier, it is probably showing the Spring Framework way without Boot. The XML configuration files and manual bean wiring you see in those articles are things Spring Boot eliminates. Always look for the Boot-specific solution.

---

## 1.3 Your Mental Map — Node.js to Spring Boot

You already know how to build a backend. Every concept exists in Spring Boot — just with different names and structure. Use this table whenever you feel lost.

|Concept|Node.js / Express|Spring Boot / Kotlin|
|---|---|---|
|App entry point|`index.js` → `app.listen(3000)`|`HireStoryApplication.kt` → `runApplication<App>()`|
|Route handler|`app.get('/users', handler)`|`@GetMapping("/users")` on a method|
|URL parameters|`req.params.id`|`@PathVariable id: String`|
|Query strings|`req.query.page`|`@RequestParam page: Int = 0`|
|Request body|`req.body` (after json middleware)|`@RequestBody dto: CreateDto`|
|Middleware|`app.use(myMiddleware)`|`Filter` or `@ControllerAdvice`|
|Auth middleware|`app.use(verifyToken)`|Spring Security filter chain|
|Config / .env|`process.env.DB_URL`|`application.yml` + `@Value` or `@ConfigurationProperties`|
|ORM model (Mongoose)|`Schema + Model`|`@Entity` class + `JpaRepository<T, ID>`|
|Custom query|`Model.find({ company: x })`|`@Query("SELECT...")` or method name convention|
|Error handling|`next(error)` + error middleware|`@ControllerAdvice` with `@ExceptionHandler`|
|Dependency injection|Manual: `const repo = new Repo()`|Automatic: Spring injects via constructor|
|`package.json`|Defines dependencies and scripts|`build.gradle.kts` does the same|
|`npm install`|Adds to `node_modules`|Adds to `dependencies {}` block in Gradle|
|`npm run dev`|Starts dev server|`./gradlew bootRun`|
|Nodemon (hot reload)|Nodemon watches files|`spring-boot-devtools` does the same|

---

## 1.4 Understanding Your Project Structure

When you generate a project from `start.spring.io`, you get a specific directory layout. Unlike Node where you create folders yourself, Spring Boot has a standard structure every Spring developer recognises. Learn it once, read any Spring project.

```
hirestory/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/example/hirestory/
│   │   │       ├── HireStoryApplication.kt   ← Entry point
│   │   │       ├── controller/               ← HTTP layer
│   │   │       ├── service/                  ← Business logic
│   │   │       ├── repository/               ← Database layer
│   │   │       ├── entity/                   ← Database models
│   │   │       ├── dto/                      ← API shapes (in/out)
│   │   │       ├── config/                   ← Spring configuration
│   │   │       └── exception/                ← Custom exceptions
│   │   └── resources/
│   │       ├── application.yml               ← Your config file
│   │       └── db/migration/                 ← Flyway SQL files
│   └── test/
│       └── kotlin/
│           └── com/example/hirestory/
│               └── ...                       ← Your test files
├── build.gradle.kts                          ← Dependencies + build config
├── settings.gradle.kts                       ← Project name
└── gradlew                                   ← Run Gradle without installing it
```

### The Three-Layer Rule

The `controller / service / repository` structure is not arbitrary. It represents the three distinct responsibilities every feature in your app has:

**1. Controller** — Receives the HTTP request. Validates it. Calls the service. Returns the response. That is all. No database calls. No business logic. No decisions.

**2. Service** — Contains all business logic. Coordinates between repositories. Makes decisions. This is where your rules live: _"a user can only read 25 articles per month"_, _"a pending interview needs approval before publishing"_, _"you cannot bookmark your own interview"_.

**3. Repository** — Talks to the database. That is its only job. No business logic. No HTTP concerns. No decisions.

> **💡 Why this matters for HireStory:** When you need to add bookmarks, you create `BookmarkController`, `BookmarkService`, and `BookmarkRepository`. Each file has one job. When a bug appears in database queries, you look in the Repository. When a bug is in read count logic, you look in the Service. When a bug is in the response shape, you look in the Controller. This saves hours of debugging on a project this size.

---

## 1.5 build.gradle.kts — Your package.json

The `build.gradle.kts` file is your `package.json`. It defines your dependencies, project metadata, and how to build the application. Unlike `package.json` which uses JSON, this file uses **Kotlin DSL** — meaning it is actual Kotlin code.

```kotlin
// build.gradle.kts

plugins {
    // Adds Spring Boot tooling: bootRun, bootJar tasks
    id("org.springframework.boot") version "3.4.1"

    // Manages Spring dependency versions automatically
    // You almost never specify versions for Spring libs manually
    id("io.spring.dependency-management") version "1.1.7"

    // Kotlin compiler for JVM
    kotlin("jvm") version "2.1.0"

    // Opens classes annotated with @Component, @Service, etc.
    // Required because Kotlin classes are final by default
    // Spring needs to create subclasses (proxies) for some features
    kotlin("plugin.spring") version "2.1.0"

    // Opens @Entity classes — needed for JPA lazy loading
    kotlin("plugin.jpa") version "2.1.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

repositories {
    mavenCentral()  // Like npm registry, but for Java/Kotlin
}

dependencies {
    // Spring Web — REST controllers, embedded Tomcat server
    implementation("org.springframework.boot:spring-boot-starter-web")

    // JPA + Hibernate — ORM layer (talk to database with Kotlin classes)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Spring Security — authentication and authorisation
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Validation — @NotBlank, @Email, @Min, @Max, etc.
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Redis — caching and counters
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // PostgreSQL driver — connects to your database
    runtimeOnly("org.postgresql:postgresql")

    // Flyway — manages database schema migrations
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    // Kotlin JSON serialisation — converts your data classes to/from JSON
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Kotlin reflection — Spring needs this internally
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Hot reload in development — restart on code changes
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Health check endpoint — GET /actuator/health
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kotlin {
    compilerOptions {
        // Strict null safety for Spring annotations
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}
```

> **📝 Note:** Most Spring Boot dependencies have no version number. That is the `dependency-management` plugin working — it knows exactly which version of every Spring library is compatible with Spring Boot 3.4.1 and sets them automatically. This prevents the dependency version conflicts that plagued Java projects for years.

### Common Gradle Commands

```bash
./gradlew bootRun          # Start the application (like npm run dev)
./gradlew build            # Compile and run all tests
./gradlew test             # Run tests only
./gradlew bootJar          # Build a runnable JAR file for deployment
./gradlew dependencies     # See all dependencies and their versions
./gradlew clean            # Delete build output (like rm -rf node_modules)
```

---

## 1.6 application.yml — The Configuration File

The `application.yml` file is the central nervous system of your Spring Boot application. It controls database connections, server port, logging, caching, security settings, and any custom configuration your app needs.

### 1.6.1 YAML Rules You Must Know

Breaking these rules causes cryptic startup errors:

```yaml
# Rule 1: Indentation uses SPACES, never tabs.
# Rule 2: Two spaces per indentation level (standard convention).
# Rule 3: Nesting creates hierarchy.

# CORRECT
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/hirestory_dev
    username: postgres

# WRONG — tabs will crash the app on startup
spring:
	datasource:    ← TAB CHARACTER — THIS WILL CRASH

# Rule 4: Lists use dash + space
allowed-origins:
  - http://localhost:3000
  - https://hirestory.com

# Rule 5: Strings with special characters need quotes
some-url: "jdbc:postgresql://localhost:5432/db?ssl=true&sslmode=require"

# Rule 6: Booleans are just true or false, no quotes
show-sql: true
enabled: false
```

### 1.6.2 Your Full HireStory application.yml

This is your complete configuration file. Every line is explained. Build it incrementally as you add features — start with just the database and server sections for Phase 1.

```yaml
# src/main/resources/application.yml

# ── SERVER ────────────────────────────────────────────────────────
server:
  port: 8080
  servlet:
    # Adds /api prefix to ALL endpoints automatically
    # @GetMapping("/interviews") becomes GET /api/interviews
    context-path: /api

# ── SPRING ────────────────────────────────────────────────────────
spring:
  application:
    name: hirestory    # Appears in logs and monitoring tools

  # ── DATABASE ────────────────────────────────────────────────────
  datasource:
    url: jdbc:postgresql://localhost:5432/hirestory_dev
    username: ${DB_USERNAME:postgres}     # Env var with fallback default
    password: ${DB_PASSWORD:yourpassword}
    driver-class-name: org.postgresql.Driver
    hikari:                               # HikariCP is the connection pool Spring Boot uses
      maximum-pool-size: 10              # Max concurrent DB connections
      minimum-idle: 2                    # Always keep 2 connections warm
      connection-timeout: 30000          # Wait max 30s to get a connection
      idle-timeout: 600000               # Close idle connections after 10 min

  # ── JPA / HIBERNATE ─────────────────────────────────────────────
  jpa:
    hibernate:
      # CRITICAL: NEVER use "create" or "create-drop" in real projects
      # "create" drops and recreates all tables on every restart — you lose all data
      # "validate" — Hibernate checks your entities match the DB. Flyway creates the tables.
      ddl-auto: validate
    properties:
      hibernate:
        show_sql: true          # Log every SQL query — turn off in production
        format_sql: true        # Format SQL across multiple lines for readability
        dialect: org.hibernate.dialect.PostgreSQLDialect
        use_sql_comments: true  # Add comments showing which code triggered each query
    open-in-view: false         # Explained fully in Chapter 3. Always set to false.

  # ── FLYWAY ──────────────────────────────────────────────────────
  flyway:
    enabled: true
    locations: classpath:db/migration    # Where your SQL migration files live
    baseline-on-migrate: true

  # ── REDIS ───────────────────────────────────────────────────────
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}    # Empty by default (local Redis has no password)
      timeout: 2000ms

  # ── JACKSON (JSON serialisation) ────────────────────────────────
  jackson:
    property-naming-strategy: LOWER_CAMEL_CASE    # addedAt not added_at in JSON
    default-property-inclusion: non_null          # Do not include null fields in response
    serialization:
      write-dates-as-timestamps: false            # "2024-01-15T10:30:00" not 1705312200000
    deserialization:
      fail-on-unknown-properties: false           # Ignore extra fields in incoming JSON

# ── ACTUATOR ──────────────────────────────────────────────────────
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics    # Only expose safe endpoints publicly
  endpoint:
    health:
      show-details: when-authorized

# ── LOGGING ───────────────────────────────────────────────────────
logging:
  level:
    root: INFO
    com.example.hirestory: DEBUG        # Your own code logs verbosely in dev
    org.hibernate.SQL: DEBUG            # See every SQL query
    org.springframework.security: DEBUG # Remove this in production
  pattern:
    console: "%d{HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# ── YOUR CUSTOM PROPERTIES ────────────────────────────────────────
hirestory:
  free-tier:
    monthly-read-limit: 25
  clerk:
    jwks-url: ${CLERK_JWKS_URL}
    webhook-secret: ${CLERK_WEBHOOK_SECRET}
  openai:
    api-key: ${OPENAI_API_KEY}
    model: gpt-4o-mini
  cors:
    allowed-origins:
      - http://localhost:3000
      - https://hirestory.com
```

### 1.6.3 The `${VAR:default}` Pattern

Look at this line:

```yaml
username: ${DB_USERNAME:postgres}
```

This means: _look for an environment variable called `DB_USERNAME`. If it exists, use its value. If it does not, use `postgres`as the default._

|Environment|What Happens|
|---|---|
|Local development|No env var set → uses the default from yml|
|Railway (production)|`DB_USERNAME` set in Railway dashboard → uses that|
|CI/CD pipeline|`DB_USERNAME` set in pipeline secrets → uses that|

The same yml file works everywhere. You never commit secrets to Git.

> **💡 Tip:** Keep a file called `.env.example` in your repo listing which variables are needed, without their real values. You will forget six months from now.

### 1.6.4 Spring Profiles — Dev vs Production

Spring Boot lets you have separate config files per environment:

```yaml
# application.yml — loaded always (shared config)
spring:
  application:
    name: hirestory
  profiles:
    active: dev    # Change to "prod" for production
```

```yaml
# application-dev.yml — only loaded when profile is "dev"
spring:
  jpa:
    properties:
      hibernate:
        show_sql: true
logging:
  level:
    com.example.hirestory: DEBUG
```

```yaml
# application-prod.yml — only loaded when profile is "prod"
spring:
  jpa:
    properties:
      hibernate:
        show_sql: false
logging:
  level:
    root: WARN    # Only warnings and errors in production
```

On Railway, set the environment variable `SPRING_PROFILES_ACTIVE=prod` and the production config activates automatically.

---

## 1.7 How Spring Boot Actually Starts — The Magic Explained

When you click Run in IntelliJ, a precise sequence of events happens. Understanding this sequence means you can debug startup failures instead of being confused by them.

```
1. JVM starts
         ↓
2. main() runs → runApplication<HireStoryApplication>()
         ↓
3. Spring reads application.yml
         ↓
4. Spring scans your code for annotations
   (@Controller, @Service, @Repository, @Component, @Entity...)
         ↓
5. Spring creates instances of every annotated class
   (these instances are called "beans")
         ↓
6. Spring wires beans together
   (if Service needs Repository, Spring injects it automatically)
         ↓
7. Auto-configuration runs
   (sees PostgreSQL driver on classpath → configures DataSource automatically)
   (sees Spring Web on classpath → configures Tomcat automatically)
   (sees Spring Security on classpath → adds security filter chain automatically)
         ↓
8. Flyway runs → checks db/migration/ → runs any new SQL files
         ↓
9. Tomcat starts on port 8080
         ↓
10. "Started HireStoryApplication in 3.4 seconds" appears in logs
```

The most important step is **step 5 and 6** — this is Dependency Injection. It is the concept everything else in Spring Boot depends on.

---

## 1.8 Dependency Injection — The Concept That Powers Everything

This is the most important concept in Spring Boot. If you understand this deeply, the rest of the framework makes sense. If you skip it, everything feels like magic and you cannot debug it.

### The Problem Without DI

In Node.js, you probably do this:

```javascript
// Node.js — manual dependency management
const userRepository = new UserRepository(db)
const articleRepository = new ArticleRepository(db)
const userService = new UserService(userRepository)
const articleService = new ArticleService(articleRepository, userService)
const articleController = new ArticleController(articleService)

app.get('/articles', articleController.findAll)
```

This works for small apps. But as your app grows:

- Creating objects in the right order becomes complex
- Testing is hard — to test `ArticleService` you have to create `ArticleRepository` first
- Changing a constructor parameter means changing it everywhere it is created

### The Solution — Let Spring Manage It

In Spring Boot, you annotate your classes and Spring creates and connects them:

```kotlin
// Spring Boot — Spring manages creation and wiring

@Repository   // "I am a database class — Spring, please create me"
class ArticleRepository(
    private val entityManager: EntityManager
)

@Service      // "I am a business logic class — Spring, please create me"
class ArticleService(
    private val articleRepository: ArticleRepository  // Spring injects this automatically
)

@RestController  // "I am a web handler class — Spring, please create me"
class ArticleController(
    private val articleService: ArticleService  // Spring injects this automatically
)
```

Spring Boot sees `ArticleController` needs `ArticleService`. Spring Boot sees `ArticleService` needs `ArticleRepository`. It creates them in the right order and wires them together. You never call `new` yourself.

### The Bean

Every class that Spring manages is called a **bean**. A bean is just an object whose lifecycle Spring controls. Spring creates it, wires its dependencies, and destroys it when the app shuts down.

What makes a class a bean? An annotation:

|Annotation|What It Tells Spring|Used For|
|---|---|---|
|`@RestController`|"I handle HTTP requests"|Controllers|
|`@Service`|"I contain business logic"|Services|
|`@Repository`|"I talk to the database"|Repositories|
|`@Component`|"I am a generic Spring-managed class"|Utilities, helpers|
|`@Configuration`|"I define other beans"|Config classes|

### How Spring Injects Dependencies

Spring Boot supports three ways to inject dependencies. **Constructor injection is the correct way**:

```kotlin
// ✅ CORRECT — Constructor injection (always use this)
@Service
class ArticleService(
    private val articleRepository: ArticleRepository,   // Injected by Spring
    private val userRepository: UserRepository,          // Injected by Spring
    private val redisTemplate: RedisTemplate<String, String>  // Injected by Spring
) {
    fun findAll(): List<Article> {
        return articleRepository.findAll()
    }
}
```

```kotlin
// ❌ AVOID — Field injection with @Autowired
@Service
class ArticleService {
    @Autowired
    private lateinit var articleRepository: ArticleRepository  // Bad practice
}
```

> **Why constructor injection is better:**
> 
> - Your dependencies are explicit — looking at the constructor tells you exactly what the class needs
> - You can write tests without Spring — just pass mock objects to the constructor
> - The class cannot be created without its dependencies — no null pointer surprises
> - IntelliJ can detect circular dependencies at compile time

---

## 1.9 Your First Spring Boot Application — The Entry Point

```kotlin
// src/main/kotlin/com/example/hirestory/HireStoryApplication.kt

package com.example.hirestory

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication  // Three annotations in one (explained below)
class HireStoryApplication

fun main(args: Array<String>) {
    runApplication<HireStoryApplication>(*args)
    // *args spreads the array — Kotlin syntax for passing array as varargs
    // Equivalent to SpringApplication.run(HireStoryApplication::class.java, *args)
}
```

`@SpringBootApplication` is actually three annotations combined:

|Annotation|What It Does|
|---|---|
|`@SpringBootConfiguration`|Marks this as a configuration class — same as `@Configuration`|
|`@EnableAutoConfiguration`|Tells Spring Boot to auto-configure based on your classpath|
|`@ComponentScan`|Tells Spring to scan this package and all sub-packages for beans|

The `@ComponentScan` is why your package structure matters. Spring only finds your `@Service` and `@Repository` classes if they are in the same package as `HireStoryApplication` or a sub-package. If you put a class outside this package tree, Spring will not find it and you will get a confusing `NoSuchBeanDefinitionException`.

---

## 1.10 Your First REST Endpoint

Now you understand the foundation. Let us write a real endpoint — the same one you wrote in Node.js a hundred times, but in Spring Boot.

```kotlin
// Node.js version (what you know)
app.get('/health', (req, res) => {
    res.json({ status: 'UP', app: 'hirestory' })
})
```

```kotlin
// Spring Boot version (what you are learning)

// src/main/kotlin/com/example/hirestory/controller/HealthController.kt

package com.example.hirestory.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

// @RestController = @Controller + @ResponseBody
// Means: "this class handles HTTP, and return values are JSON automatically"
@RestController

// All methods in this class are under /health path
// Combined with server.servlet.context-path: /api from yml
// This becomes GET /api/health
@RequestMapping("/health")
class HealthController {

    // Handles GET /api/health
    @GetMapping
    fun health(): ResponseEntity<HealthResponse> {
        // ResponseEntity lets you control the HTTP status code
        return ResponseEntity.ok(
            HealthResponse(
                status = "UP",
                app = "hirestory",
                timestamp = LocalDateTime.now()
            )
        )
    }
}

// The shape of your response JSON
// Data class = all fields become JSON properties automatically
data class HealthResponse(
    val status: String,
    val app: String,
    val timestamp: LocalDateTime
)
```

Run the app and hit `GET http://localhost:8080/api/health`. You get:

```json
{
  "status": "UP",
  "app": "hirestory",
  "timestamp": "2024-01-15T10:30:00"
}
```

### Understanding @RequestMapping and the HTTP method annotations

```kotlin
@RestController
@RequestMapping("/api/interviews")    // Base path for all methods in this class
class InterviewController {

    @GetMapping                        // GET  /api/interviews
    fun findAll() { }

    @GetMapping("/{id}")               // GET  /api/interviews/123
    fun findOne(@PathVariable id: Long) { }

    @PostMapping                       // POST /api/interviews
    fun create(@RequestBody dto: CreateInterviewDto) { }

    @PutMapping("/{id}")               // PUT  /api/interviews/123
    fun update(@PathVariable id: Long, @RequestBody dto: UpdateInterviewDto) { }

    @DeleteMapping("/{id}")            // DELETE /api/interviews/123
    fun delete(@PathVariable id: Long) { }

    @GetMapping("/search")             // GET  /api/interviews/search?q=google
    fun search(@RequestParam q: String) { }
}
```

### Extracting Data From Requests

```kotlin
@RestController
@RequestMapping("/api/interviews")
class InterviewController(
    private val interviewService: InterviewService
) {

    // GET /api/interviews?page=0&size=20&company=Google&difficulty=MEDIUM
    @GetMapping
    fun findAll(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) company: String?,   // Optional — null if not provided
        @RequestParam(required = false) difficulty: String?
    ): ResponseEntity<List<InterviewSummaryDto>> {
        val results = interviewService.findAll(page, size, company, difficulty)
        return ResponseEntity.ok(results)
    }

    // GET /api/interviews/my-awesome-google-interview
    @GetMapping("/{slug}")
    fun findOne(@PathVariable slug: String): ResponseEntity<InterviewDetailDto> {
        val interview = interviewService.findBySlug(slug)
        return ResponseEntity.ok(interview)
    }

    // POST /api/interviews
    // Body: { "title": "My Google Interview", "company": "Google", ... }
    @PostMapping
    fun create(@RequestBody @Valid dto: CreateInterviewDto): ResponseEntity<InterviewDetailDto> {
        // @Valid triggers validation annotations on CreateInterviewDto fields
        val created = interviewService.create(dto)
        // 201 Created is the correct status for resource creation, not 200 OK
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }
}
```

---

## 1.11 DTOs — The Shape of Your API

A DTO (Data Transfer Object) is the JSON shape your API sends and receives. It is separate from your database Entity. This separation is important — what you store in the database and what you expose in your API should not be the same object.

```kotlin
// ❌ WRONG — Returning the database Entity directly
@GetMapping("/{id}")
fun findOne(@PathVariable id: Long): Interview {  // Interview is your @Entity
    return interviewRepository.findById(id)
    // Problems:
    // 1. Exposes internal database fields (createdAt, updatedAt, internal IDs)
    // 2. Cannot add computed fields (formatted date, author name)
    // 3. Changing database schema breaks your API contract
    // 4. Cannot hide sensitive fields
}

// ✅ CORRECT — Returning a DTO
@GetMapping("/{id}")
fun findOne(@PathVariable id: Long): InterviewDetailDto {
    val interview = interviewRepository.findById(id)
    return interview.toDetailDto()   // Convert Entity → DTO
}
```

For HireStory, you will have DTOs like these:

```kotlin
// src/main/kotlin/com/example/hirestory/dto/InterviewDtos.kt

// What the API receives when someone submits an interview
data class CreateInterviewDto(
    @field:NotBlank(message = "Title cannot be empty")
    val title: String,

    @field:NotBlank(message = "Company name cannot be empty")
    val company: String,

    @field:NotBlank(message = "Role cannot be empty")
    val role: String,

    @field:Min(value = 0, message = "Experience cannot be negative")
    val experienceYears: Int,

    @field:NotBlank(message = "Content cannot be empty")
    @field:Size(min = 100, message = "Content must be at least 100 characters")
    val content: String,

    val difficulty: Difficulty,
    val outcome: Outcome,
    val salaryLpa: Double?,     // Optional — nullable
    val isAnonymous: Boolean = false,
    val rounds: List<CreateRoundDto> = emptyList()
)

// What the API returns for a list item (less data = faster response)
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
    val addedAt: String,           // Formatted: "January 15th 2024"
    val tags: List<String>
)

// What the API returns for a single interview (full data)
data class InterviewDetailDto(
    val id: Long,
    val slug: String,
    val title: String,
    val company: CompanyDto,
    val role: String,
    val location: String?,
    val difficulty: Difficulty,
    val outcome: Outcome,
    val experienceYears: Int,
    val salaryLpa: Double?,
    val content: String,
    val rounds: List<RoundDto>,
    val tags: List<String>,
    val author: AuthorDto?,        // Null if anonymous
    val addedAt: String,
    val sourceUrl: String?,        // If crawled from external source
    val isBookmarked: Boolean      // Only meaningful if user is logged in
)

enum class Difficulty { EASY, MEDIUM, HARD }
enum class Outcome { OFFER, REJECTED, GHOSTED }
```

---

## 1.12 Common Mistakes in Chapter 1

### Mistake 1 — Putting business logic in the Controller

```kotlin
// ❌ WRONG — Controller doing too much
@GetMapping
fun findAll(): List<InterviewSummaryDto> {
    val interviews = interviewRepository.findAll()
    // Read count logic in controller — wrong layer
    if (currentUser.readCount >= 25 && !currentUser.isPremium) {
        throw PaywallException()
    }
    return interviews.map { it.toSummaryDto() }
}

// ✅ CORRECT — Controller delegates, Service decides
@GetMapping
fun findAll(): List<InterviewSummaryDto> {
    return interviewService.findAll()   // Service handles the read count logic
}
```

### Mistake 2 — Using `ddl-auto: create` and losing data

```yaml
# ❌ WILL DELETE ALL YOUR DATA on every restart
spring:
  jpa:
    hibernate:
      ddl-auto: create

# ✅ Correct — Flyway manages the schema, Hibernate just validates
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

### Mistake 3 — Field injection instead of constructor injection

```kotlin
// ❌ AVOID — Hard to test, hides dependencies
@Service
class InterviewService {
    @Autowired
    private lateinit var interviewRepository: InterviewRepository
}

// ✅ CORRECT — Dependencies are explicit and testable
@Service
class InterviewService(
    private val interviewRepository: InterviewRepository
)
```

### Mistake 4 — Returning 200 for resource creation

```kotlin
// ❌ Wrong status code
return ResponseEntity.ok(created)            // 200 is for reading, not creating

// ✅ Correct status code
return ResponseEntity.status(HttpStatus.CREATED).body(created)  // 201
```

### Mistake 5 — Package outside the component scan

```
com/example/hirestory/HireStoryApplication.kt   ← @ComponentScan starts here
com/example/hirestory/controller/ArticleController.kt   ← ✅ Found
com/example/utils/SomeHelper.kt   ← ❌ OUTSIDE scan — Spring never finds this
```

---

## 1.13 HireStory Connection — What You Just Built

At the end of Chapter 1, your HireStory backend has:

- A running Spring Boot 3 application with embedded Tomcat
- `application.yml` configured with database, Redis, logging, and custom properties
- A proper three-layer project structure ready to be filled in
- A health check endpoint at `GET /api/health`
- A mental model of how Spring Boot starts and wires your code
- Understanding of Dependency Injection — the foundation for every chapter ahead

This is the skeleton. Every subsequent chapter adds a body part:

- Chapter 2 adds the database layer (JPA + Flyway)
- Chapter 3 adds data fetching (Repositories + Queries)
- Chapter 4 adds the full REST API
- Chapter 5 adds authentication
- And so on until HireStory is complete

---

## 1.14 Chapter Project — Build It Before You Move On

Build the following. Do not move to Chapter 2 until this runs end to end.

### What to build

A Spring Boot application with three endpoints:

**1. `GET /api/health`** — Returns app status

```json
{
  "status": "UP",
  "app": "hirestory",
  "version": "1.0.0",
  "timestamp": "2024-01-15T10:30:00"
}
```

**2. `GET /api/companies`** — Returns a hardcoded list (no database yet)

```json
[
  { "id": 1, "name": "Google", "location": "Bangalore" },
  { "id": 2, "name": "Amazon", "location": "Hyderabad" },
  { "id": 3, "name": "Microsoft", "location": "Pune" }
]
```

**3. `GET /api/companies/{id}`** — Returns one company, or 404 if not found

```json
{ "id": 1, "name": "Google", "location": "Bangalore" }
```

### Requirements

- Use the three-layer structure: `CompanyController` → `CompanyService` → no repository yet (service holds hardcoded data)
- `application.yml` must have server port, context path `/api`, and logging configured
- The 404 must return a proper error response, not Spring's default white-label error page:

```json
{
  "error": "Company not found",
  "id": 99
}
```

- Hit all three endpoints in a REST client (IntelliJ has one built in — `.http` files) and see correct responses

### What you should NOT do

- Do not connect a database yet — that is Chapter 2
- Do not add authentication — that is Chapter 5
- Do not add Redis — that is Chapter 6

### Checkpoint questions — answer these before moving on

1. What is the difference between `@RestController` and `@Controller`?
2. If you put `CompanyService` in the package `com.utils` instead of `com.example.hirestory.service`, what error do you get and why?
3. What does `@RequestParam(required = false)` do differently than `@RequestParam`?
4. Why do you use `ResponseEntity<T>` as a return type instead of just `T`?
5. Change the server port to 9090 in `application.yml`. Does the app work on 9090 without changing any Kotlin code? Why?

---

_Chapter 2 → JPA and Entities — Mapping Your Database To Kotlin Classes_

---

> **Book Progress:** Chapter 1 of 15 complete. Chapters ahead: JPA · Repositories · REST API · Spring Security · JWT/Clerk · Redis · RabbitMQ · Spring AI · Jsoup · Scheduler · Testing · Deployment