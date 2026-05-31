 # HireStory — Chapters 1 to 5 (Multi-Module Monolith)

### Spring Boot + Kotlin · Multi-Module Gradle · Theory + Code + Checkpoints

> **How to use this book:** Read the theory section of each chapter first. Understand it. Then implement the code. Never skip a chapter — every chapter depends on the previous one.

> **Architecture note:** This version builds a single Spring Boot application organized as Gradle modules. One JVM process, one port, one `application.yml`. Modules enforce code boundaries the same way packages would, but with hard compile-time isolation — the `content` module literally cannot import `user` internals unless `user` exposes them.

---

## Table of Contents

- [Chapter 1 — Tools Setup & Multi-Module Project](https://claude.ai/chat/d560d758-b044-4545-9251-ab09869bf765#chapter-1)
- [Chapter 2 — Kotlin & Spring Boot Fundamentals](https://claude.ai/chat/d560d758-b044-4545-9251-ab09869bf765#chapter-2)
- [Chapter 3 — Database Design with JPA & Flyway](https://claude.ai/chat/d560d758-b044-4545-9251-ab09869bf765#chapter-3)
- [Chapter 4 — Building the User Module](https://claude.ai/chat/d560d758-b044-4545-9251-ab09869bf765#chapter-4)
- [Chapter 5 — JWT Authentication & Spring Security](https://claude.ai/chat/d560d758-b044-4545-9251-ab09869bf765#chapter-5)

---

# Chapter 1 — Tools Setup & Multi-Module Project {#chapter-1}

## Theory

### What is Spring Boot?

Spring Boot is a framework that lets you build backend APIs in Kotlin or Java. It handles HTTP requests, talks to databases, manages security — everything a backend needs. Without a framework, you would write all of this from scratch. Spring Boot gives you 90% of it for free.

Think of it like this:

```
Your Android app  →  asks for data  →  Spring Boot (your server)
                                             ↓
                                       PostgreSQL (where data lives)
```

### What is a Multi-Module Gradle Project?

A multi-module project is one codebase split into separate compilation units that depend on each other. Instead of one giant source folder, you get named modules with explicit dependency rules.

```
hirestory-backend/          ← root project
├── core/                   ← module 1: shared entities, enums, exceptions
├── user/                   ← module 2: user registration, profiles
├── content/                ← module 3: interviews, feed, bookmarks
├── notification/           ← module 4: push notifications, FCM
└── app/                    ← module 5: main app, security, config, migrations
```

Why this matters:

- `content` can call `user` services directly as method calls — no HTTP, no network
- `core` cannot import `user` — the build system enforces this
- One JVM, one port (8080), one `application.yml`
- You open one IntelliJ window and see everything

### What is PostgreSQL?

PostgreSQL is the database where all your app data lives permanently. Users, interviews, bookmarks — everything stored here. It runs as a background service on your machine.

Why PostgreSQL over MySQL:

- Native UUID primary key support
- Full-text search built in (needed for interview search later)
- Better performance for concurrent reads
- JSONB column support for flexible data

### What is Redis?

Redis is an in-memory database — stores data in RAM, not on disk. Extremely fast (microseconds). You use it for:

- Caching API responses (feed, company list)
- Read counter per user per month
- URL deduplication for the crawler (separate project)

### What is RabbitMQ?

RabbitMQ is a message queue. You need it for:

- Notification delivery (content events → push notifications)

> **For now:** Install only PostgreSQL in Chapter 1. Redis in Chapter 7. RabbitMQ in Chapter 10.

---

## Implementation

### Step 1 — Install Java 21

**Windows:**

1. Go to https://adoptium.net
2. Download **Temurin 21 LTS**
3. Run installer — check "Set JAVA_HOME" option
4. Restart terminal

**Mac:**

```bash
brew install openjdk@21
echo 'export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

**Verify:**

```bash
java -version
# Should show: openjdk version "21.x.x"
```

### Step 2 — Install IntelliJ IDEA

Download from: https://www.jetbrains.com/idea/download

Choose **Community Edition** (free) — enough for Spring Boot.

> Note: IntelliJ IDEA is different from Android Studio. Install both — Android Studio for your KMP app, IntelliJ for this backend.

### Step 3 — Install PostgreSQL

**Windows:**

1. Go to https://www.postgresql.org/download/windows
2. Download installer (PostgreSQL 16)
3. Run installer:
    - Password: `hirestory123`
    - Port: `5432` (default — keep it)
    - Locale: default
4. Finish — PostgreSQL starts automatically as a Windows service

**Mac:**

```bash
brew install postgresql@16
brew services start postgresql@16
brew services list   # postgresql@16 should show: started
```

**Linux (Ubuntu):**

```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### Step 4 — Create Database and User

**Mac/Linux:**

```bash
psql postgres

CREATE USER hirestory WITH PASSWORD 'hirestory123';
CREATE DATABASE hirestory_dev OWNER hirestory;
GRANT ALL PRIVILEGES ON DATABASE hirestory_dev TO hirestory;
\q
```

**Windows** — open SQL Shell (psql) from Start Menu, press Enter for all prompts except password:

```sql
CREATE USER hirestory WITH PASSWORD 'hirestory123';
CREATE DATABASE hirestory_dev OWNER hirestory;
GRANT ALL PRIVILEGES ON DATABASE hirestory_dev TO hirestory;
```

**Verify:**

```bash
psql -U hirestory -d hirestory_dev -h localhost
# Enter password: hirestory123
# Should show: hirestory_dev=#
# Type \q to exit
```

### Step 5 — Install Redis

**Mac:**

```bash
brew install redis
brew services start redis
redis-cli ping   # Should return: PONG
```

**Linux:**

```bash
sudo apt install redis-server
sudo systemctl start redis
sudo systemctl enable redis
redis-cli ping
```

**Windows** — use Memurai (Redis-compatible, native Windows):

1. Download from https://www.memurai.com
2. Install — runs as Windows service automatically
3. Verify: `redis-cli ping` → PONG

### Step 6 — Create the Multi-Module Project Structure

Do NOT use start.spring.io for this — it only creates single-module projects. You will create the folder structure manually and open it in IntelliJ.

**Create this exact folder structure on your machine:**

```
hirestory-backend/
├── settings.gradle.kts
├── build.gradle.kts
├── core/
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/hirestory/core/
│       └── (empty for now)
├── user/
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/hirestory/user/
│       └── (empty for now)
├── content/
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/hirestory/content/
│       └── (empty for now)
├── notification/
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/hirestory/notification/
│       └── (empty for now)
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── kotlin/com/hirestory/app/
        │   │   └── (empty for now)
        │   └── resources/
        │       └── application.yml
        └── test/kotlin/com/hirestory/app/
```

**On Mac/Linux — run this to create all folders at once:**

```bash
mkdir -p hirestory-backend/{core,user,content,notification,app}
cd hirestory-backend

for module in core user content notification; do
  mkdir -p $module/src/main/kotlin/com/hirestory/$module
  touch $module/build.gradle.kts
done

mkdir -p app/src/main/kotlin/com/hirestory/app
mkdir -p app/src/main/resources
mkdir -p app/src/test/kotlin/com/hirestory/app
touch app/build.gradle.kts
```

### Step 7 — Root `settings.gradle.kts`

```kotlin
// hirestory-backend/settings.gradle.kts
rootProject.name = "hirestory-backend"

include(
    "core",
    "user",
    "content",
    "notification",
    "app"
)
```

### Step 8 — Root `build.gradle.kts`

```kotlin
// hirestory-backend/build.gradle.kts
plugins {
    kotlin("jvm")                         version "1.9.25" apply false
    kotlin("plugin.spring")               version "1.9.25" apply false
    kotlin("plugin.jpa")                  version "1.9.25" apply false
    id("org.springframework.boot")        version "3.3.4"  apply false
    id("io.spring.dependency-management") version "1.1.6"  apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "io.spring.dependency-management")

    group   = "com.hirestory"
    version = "0.0.1-SNAPSHOT"

    java.sourceCompatibility = JavaVersion.VERSION_21

    repositories {
        mavenCentral()
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.4")
        }
    }

    dependencies {
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget         = "21"
        }
    }
}
```

### Step 9 — Module `build.gradle.kts` Files

**`core/build.gradle.kts`**

```kotlin
plugins {
    kotlin("plugin.jpa")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
```

**`user/build.gradle.kts`**

```kotlin
plugins {
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":core"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
}
```

**`content/build.gradle.kts`**

```kotlin
plugins {
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":user"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
}
```

**`notification/build.gradle.kts`**

```kotlin
plugins {
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":user"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("com.google.firebase:firebase-admin:9.2.0")
}
```

**`app/build.gradle.kts`**

```kotlin
plugins {
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

dependencies {
    // All modules wired in here
    implementation(project(":core"))
    implementation(project(":user"))
    implementation(project(":content"))
    implementation(project(":notification"))

    // All Spring Boot starters live here — submodules only declare what they compile against
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("com.nimbusds:nimbus-jose-jwt:9.37.3")
    implementation("com.google.firebase:firebase-admin:9.2.0")
    runtimeOnly("org.postgresql:postgresql")
}
```

### Step 10 — Main Application Class

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
        "com.hirestory.notification"
        // core has no Spring beans — no scan needed
    ]
)
class HirestoryApplication

fun main(args: Array<String>) {
    runApplication<HirestoryApplication>(*args)
}
```

> **Why `scanBasePackages`?** By default Spring only scans the package of `@SpringBootApplication` and its sub-packages. Your other modules live in different root packages (`com.hirestory.user`, etc.), so you must explicitly list them here.

### Step 11 — `application.yml`

```yaml
# app/src/main/resources/application.yml
server:
  port: 8080

spring:
  application:
    name: hirestory-backend

  datasource:
    url: jdbc:postgresql://localhost:5432/hirestory_dev
    username: hirestory
    password: hirestory123
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  data:
    redis:
      host: localhost
      port: 6379

  flyway:
    enabled: true
    locations: classpath:db/migration

management:
  endpoints:
    web:
      exposure:
        include: health,info

firebase:
  project-id: your-firebase-project-id    # Firebase Console → Project Settings
```

### Step 12 — Open in IntelliJ and First Run

Open IntelliJ IDEA → File → Open → select the `hirestory-backend` folder (not a subfolder).

IntelliJ detects the multi-module Gradle project automatically. You will see all 5 modules in the Project panel.

```bash
# Run from terminal inside hirestory-backend/
./gradlew :app:bootRun
```

You will get a Flyway error on first run because there are no migration files yet — that is expected. The fix is in Chapter 3.

**Temporarily disable Flyway to verify the app starts:**

```yaml
# In application.yml — temporarily
spring:
  flyway:
    enabled: false
```

```bash
./gradlew :app:bootRun
# Console should show: Started HirestoryApplication in X.X seconds
```

Open browser: `http://localhost:8080/actuator/health` → `{"status":"UP"}`

Re-enable Flyway after — you need it for Chapter 3.

**Common startup errors:**

```
Error: Connection refused localhost:5432
Fix:   PostgreSQL not running
       Mac:   brew services start postgresql@16
       Linux: sudo systemctl start postgresql

Error: Failed to configure a DataSource
Fix:   Check application.yml credentials match Step 4

Error: Could not resolve com.google.firebase:firebase-admin:9.2.0
Fix:   Run: ./gradlew --refresh-dependencies
```

---

## Chapter 1 Checkpoint ✅

- [ ] Java 21 installed — `java -version` shows 21
- [ ] PostgreSQL running — `psql -U hirestory -d hirestory_dev -h localhost` connects
- [ ] Redis running — `redis-cli ping` returns PONG
- [ ] Folder structure created exactly as shown
- [ ] `settings.gradle.kts` lists all 5 modules
- [ ] IntelliJ shows 5 modules in the Project panel
- [ ] `http://localhost:8080/actuator/health` returns `{"status":"UP"}`
- [ ] Project pushed to GitHub

---

# Chapter 2 — Kotlin & Spring Boot Fundamentals {#chapter-2}

## Theory

### The 4 Layers of a Spring Boot App

Every Spring Boot application has the same 4 layers. Understanding this is the most important thing in this chapter.

```
HTTP Request
     ↓
┌─────────────────────────────────┐
│  CONTROLLER LAYER               │  ← handles HTTP (URL, method, request body)
│  @RestController                │
└─────────────────────────────────┘
     ↓
┌─────────────────────────────────┐
│  SERVICE LAYER                  │  ← business logic (rules, calculations)
│  @Service                       │
└─────────────────────────────────┘
     ↓
┌─────────────────────────────────┐
│  REPOSITORY LAYER               │  ← database queries only
│  @Repository / JpaRepository    │
└─────────────────────────────────┘
     ↓
┌─────────────────────────────────┐
│  DATABASE                       │  ← PostgreSQL
└─────────────────────────────────┘
```

**Rule:** Controller never touches the database. Service never handles HTTP. Repository never has business logic.

### What is Dependency Injection?

Spring creates your objects and connects them together. You never write `val service = UserService()`. Spring does it for you.

```kotlin
// WITHOUT DI — tightly coupled, hard to test
class UserController {
    val service    = UserService()
    val repository = UserRepository()
}

// WITH DI — Spring injects everything
class UserController(
    private val service: UserService   // Spring creates and injects this
)
```

Spring sees `@RestController`, `@Service`, `@Repository` → creates one instance of each → injects where needed. These instances are called **Beans**.

### What is a DTO?

DTO = Data Transfer Object. The JSON shape that travels over HTTP — intentionally different from your database entity.

```
HTTP Request JSON → DTO → Service → Entity → Database
Database         → Entity → Service → DTO → HTTP Response JSON
```

Why different? Your entity has fields you never want to expose — internal state, timestamps, foreign key IDs. DTOs let you control exactly what the client sees and sends.

```kotlin
// Entity — database shape (never send raw to client)
@Entity
data class User(
    val id: UUID,
    val firebaseUid: String,    // internal — client never needs this
    val email: String,
    var name: String,
    var isPremium: Boolean      // business-sensitive
)

// Response DTO — what client sees
data class UserProfileResponse(
    val id: UUID,
    val email: String,
    val name: String
    // isPremium and firebaseUid intentionally excluded
)
```

### What is @Valid?

`@Valid` triggers validation annotations on your request DTOs before the method runs:

```kotlin
data class UpdateProfileRequest(
    @field:NotBlank(message = "Name cannot be blank")
    @field:Size(max = 100, message = "Name too long")
    val name: String?,

    val avatarUrl: String?
)

@PutMapping("/profile")
fun updateProfile(
    @Valid @RequestBody request: UpdateProfileRequest,  // ← @Valid here
    @AuthenticationPrincipal uid: String
): ResponseEntity<UserProfileResponse> {
    // If name is blank → Spring throws MethodArgumentNotValidException
    // BEFORE this line runs → GlobalExceptionHandler catches it → 400 response
}
```

### What is ResponseEntity?

Controls exactly what HTTP status code your response sends:

```kotlin
ResponseEntity.ok(body)                              // 200 OK
ResponseEntity.status(HttpStatus.CREATED).body(body) // 201 Created
ResponseEntity.notFound().build()                    // 404
ResponseEntity.badRequest().body(error)              // 400
ResponseEntity.status(402).build()                   // 402 Payment Required (paywall)
ResponseEntity.noContent().build()                   // 204 No Content (delete)
```

### What is @RestControllerAdvice?

Catches ALL exceptions from ALL controllers in one central place:

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(e: ResourceNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(404).body(ErrorResponse(e.message ?: "Not found"))
    }
}
```

Without this — clients get raw Java stack traces when something goes wrong.

### How Modules Communicate

In your multi-module project, modules communicate by direct method calls — no HTTP, no network:

```kotlin
// content module — InterviewController.kt
class InterviewController(
    private val interviewService: InterviewService,
    private val userService: UserService    // ← imported from :user module
                                           //    no HTTP call, just a method call
)

// When a user submits an interview:
val userId = userService.resolveUserId(firebaseUid)   // direct call
interviewService.submitInterview(request, userId)
```

This works because both modules are compiled into the same JAR and run in the same JVM.

---

## Implementation

### `core` Module — Exceptions

The `core` module contains only things shared across all other modules. No Spring beans (no `@Service`, `@Controller`) — just data classes, entities, enums, and exceptions.

```
core/src/main/kotlin/com/hirestory/core/
├── exception/
│   ├── Exceptions.kt
│   └── GlobalExceptionHandler.kt
├── entity/
│   └── (added in Chapter 3)
└── enums/
    └── (added in Chapter 3)
```

```kotlin
// core/src/main/kotlin/com/hirestory/core/exception/Exceptions.kt
package com.hirestory.core.exception

class ResourceNotFoundException(message: String)  : RuntimeException(message)
class DuplicateResourceException(message: String) : RuntimeException(message)
class UnauthorizedException(message: String)      : RuntimeException(message)
class BadRequestException(message: String)        : RuntimeException(message)
class PaywallException(message: String)           : RuntimeException(message)
```

```kotlin
// core/src/main/kotlin/com/hirestory/core/exception/GlobalExceptionHandler.kt
package com.hirestory.core.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

data class ErrorResponse(
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val errors: Map<String, String>? = null
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(e: ResourceNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(e.message ?: "Resource not found"))

    @ExceptionHandler(DuplicateResourceException::class)
    fun handleDuplicate(e: DuplicateResourceException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(e.message ?: "Resource already exists"))

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(e: UnauthorizedException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(e.message ?: "Unauthorized"))

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(e: BadRequestException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(e.message ?: "Bad request"))

    @ExceptionHandler(PaywallException::class)
    fun handlePaywall(e: PaywallException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(402)
            .body(ErrorResponse(e.message ?: "Monthly read limit reached"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val fieldErrors = e.bindingResult.allErrors
            .filterIsInstance<FieldError>()
            .associate { it.field to (it.defaultMessage ?: "Invalid") }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("Validation failed", errors = fieldErrors))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(e: Exception): ResponseEntity<ErrorResponse> {
        println("Unhandled exception: ${e.message}")
        e.printStackTrace()
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("Something went wrong"))
    }
}
```

> **Why is `GlobalExceptionHandler` in `core` and not `app`?** Because `@RestControllerAdvice` beans need to be in a package that Spring scans. You scan `com.hirestory.user`, `com.hirestory.content`, etc. — but you do NOT scan `com.hirestory.core`. To fix this, register `core` as a scan target in a different way — see the note in Chapter 5 where you configure `scanBasePackages`.

Actually — move `GlobalExceptionHandler` to `app` module since `core` is not scanned. Core stays pure Kotlin.

**Revised structure:**

```kotlin
// core/src/main/kotlin/com/hirestory/core/exception/Exceptions.kt
// ← exceptions only here (plain Kotlin classes, no Spring needed)

// app/src/main/kotlin/com/hirestory/app/exception/GlobalExceptionHandler.kt
// ← handler here (Spring bean, scanned by app module)
```

```kotlin
// app/src/main/kotlin/com/hirestory/app/exception/GlobalExceptionHandler.kt
package com.hirestory.app.exception

import com.hirestory.core.exception.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

data class ErrorResponse(
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val errors: Map<String, String>? = null
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(e: ResourceNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(e.message ?: "Resource not found"))

    @ExceptionHandler(DuplicateResourceException::class)
    fun handleDuplicate(e: DuplicateResourceException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(e.message ?: "Resource already exists"))

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(e: UnauthorizedException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(e.message ?: "Unauthorized"))

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(e: BadRequestException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(e.message ?: "Bad request"))

    @ExceptionHandler(PaywallException::class)
    fun handlePaywall(e: PaywallException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(402)
            .body(ErrorResponse(e.message ?: "Monthly read limit reached. Upgrade to premium."))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val fieldErrors = e.bindingResult.allErrors
            .filterIsInstance<FieldError>()
            .associate { it.field to (it.defaultMessage ?: "Invalid") }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("Validation failed", errors = fieldErrors))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(e: Exception): ResponseEntity<ErrorResponse> {
        println("Unhandled exception: ${e.message}")
        e.printStackTrace()
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("Something went wrong"))
    }
}
```

---

## Full File Layout After Chapter 2

```
hirestory-backend/
├── settings.gradle.kts
├── build.gradle.kts
├── core/
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/hirestory/core/
│       └── exception/
│           └── Exceptions.kt          ✅ done
├── user/
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/hirestory/user/
│       └── (empty — Chapter 4)
├── content/
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/hirestory/content/
│       └── (empty — Chapter 6)
├── notification/
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/hirestory/notification/
│       └── (empty — Chapter 10)
└── app/
    ├── build.gradle.kts
    └── src/main/kotlin/com/hirestory/app/
        ├── HirestoryApplication.kt     ✅ done
        └── exception/
            └── GlobalExceptionHandler.kt  ✅ done
```

---

## Chapter 2 Checkpoint ✅

- [ ] You understand the 4 layers (Controller → Service → Repository → DB)
- [ ] You understand DI — Spring creates and injects beans
- [ ] You understand DTOs vs Entities — two different shapes for HTTP and DB
- [ ] You understand why modules communicate with method calls, not HTTP
- [ ] `core/exception/Exceptions.kt` created with 5 exception classes
- [ ] `app/exception/GlobalExceptionHandler.kt` created and handles all 5
- [ ] `./gradlew :app:bootRun` starts without compile errors

---

# Chapter 3 — Database Design with JPA & Flyway {#chapter-3}

## Theory

### What is JPA?

JPA (Java Persistence API) maps Kotlin classes to database tables. Hibernate is the JPA implementation Spring Boot uses.

```
Kotlin data class (@Entity) ←→ JPA/Hibernate ←→ PostgreSQL table
```

You write Kotlin. JPA generates the SQL.

### JPA Annotations You Need

```kotlin
@Entity                           // this class maps to a database table
@Table(name = "users")            // the exact table name

data class User(
    @Id                           // primary key column
    @Column(columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(
        nullable = false,         // NOT NULL constraint
        unique = true,            // UNIQUE constraint
        length = 255
    )
    val email: String,

    @Column(columnDefinition = "TEXT")   // TEXT type (no length limit)
    val bio: String? = null,

    @Column(name = "created_at",
            nullable = false,
            updatable = false)    // updatable = false → never changed after insert
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

### JPA Relationships

```kotlin
// MANY interviews belong to ONE company
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "company_id", nullable = false)
val company: Company

// ONE interview has MANY rounds
@OneToMany(
    mappedBy  = "interview",
    cascade   = [CascadeType.ALL],
    fetch     = FetchType.LAZY
)
val rounds: List<InterviewRound> = emptyList()
```

Always use `FetchType.LAZY` — avoids loading related data you did not ask for.

### The N+1 Problem

```kotlin
// BAD — 1 query to get 20 interviews + 20 more queries to get each company
val interviews = interviewRepository.findAll()
interviews.forEach { println(it.company.name) }   // ← LAZY load triggers here 20 times

// GOOD — 1 query with JOIN FETCH
@Query("SELECT i FROM Interview i JOIN FETCH i.company WHERE i.status = 'PUBLISHED'")
fun findPublishedWithCompany(): List<Interview>
```

### What is Flyway?

Flyway manages your database schema through numbered SQL files. Each file runs exactly once, in order, and never again.

```
app/src/main/resources/db/migration/
├── V1__create_initial_schema.sql   ← runs on first startup
├── V2__add_indexes.sql             ← runs after V1
└── V3__add_fulltext_search.sql     ← runs after V2
```

Naming rules (ALL required):

- Starts with `V` (capital)
- Version number: `1`, `2`, `3`
- Double underscore: `__`
- Description (underscores for spaces)
- Ends with `.sql`

**Why not `ddl-auto: create`?** It drops and recreates all tables on every startup. Destroys your data. Never use in real projects.

### Why UUID Primary Keys?

```
Sequential IDs (1, 2, 3...):
  → /interviews/3 exists → attacker tries /interviews/4 → found
  → reveals your scale: "they only have 12 interviews"

UUIDs (550e8400-e29b-41d4-a716-446655440000):
  → unpredictable — cannot enumerate
  → generated in your app — no database roundtrip needed
  → safe to merge data from crawlers + users
```

### Enums in PostgreSQL

PostgreSQL supports native enum types. You create them in SQL migrations and reference them in your entities. This is better than storing plain strings because PostgreSQL enforces the valid values at the DB level.

```sql
-- In migration file:
CREATE TYPE difficulty_enum AS ENUM ('EASY', 'MEDIUM', 'HARD');

-- Column uses that type:
difficulty difficulty_enum NOT NULL DEFAULT 'MEDIUM'
```

```kotlin
// In entity:
@Enumerated(EnumType.STRING)
@Column(columnDefinition = "difficulty_enum")
val difficulty: Difficulty = Difficulty.MEDIUM
```

---

## Implementation

### Create Migration Files Directory

```
app/src/main/resources/db/migration/
```

Create this folder inside `app/src/main/resources/`. The three SQL files all go here.

### V1 — All Tables

`app/src/main/resources/db/migration/V1__create_initial_schema.sql`

```sql
-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ── USERS ──────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    firebase_uid          VARCHAR(255) NOT NULL UNIQUE,
    email                 VARCHAR(255) NOT NULL UNIQUE,
    name                  VARCHAR(255) NOT NULL,
    avatar_url            TEXT,
    is_premium            BOOLEAN NOT NULL DEFAULT FALSE,
    read_count_this_month INT NOT NULL DEFAULT 0,
    referral_code         VARCHAR(20) NOT NULL UNIQUE,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── COMPANIES ──────────────────────────────────────────────────────────────
CREATE TABLE companies (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name             VARCHAR(255) NOT NULL UNIQUE,
    logo_url         TEXT,
    slug             VARCHAR(255) NOT NULL UNIQUE,
    interview_count  INT NOT NULL DEFAULT 0,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── INTERVIEWS ─────────────────────────────────────────────────────────────
CREATE TYPE difficulty_enum       AS ENUM ('EASY', 'MEDIUM', 'HARD');
CREATE TYPE outcome_enum          AS ENUM ('OFFER', 'REJECTED', 'GHOSTED');
CREATE TYPE source_type_enum      AS ENUM ('USER_SUBMITTED', 'CRAWLED');
CREATE TYPE interview_status_enum AS ENUM ('PENDING', 'PUBLISHED', 'REJECTED');

CREATE TABLE interviews (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id        UUID NOT NULL REFERENCES companies(id),
    user_id           UUID REFERENCES users(id),
    title             VARCHAR(500) NOT NULL,
    content           TEXT NOT NULL,
    role              VARCHAR(255),
    location          VARCHAR(255),
    experience_years  INT,
    rounds_count      INT NOT NULL DEFAULT 1,
    difficulty        difficulty_enum NOT NULL DEFAULT 'MEDIUM',
    outcome           outcome_enum NOT NULL DEFAULT 'OFFER',
    salary_lpa        DECIMAL(10,2),
    source_url        TEXT UNIQUE,
    source_type       source_type_enum NOT NULL DEFAULT 'USER_SUBMITTED',
    status            interview_status_enum NOT NULL DEFAULT 'PENDING',
    confidence_score  INT NOT NULL DEFAULT 100,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    published_at      TIMESTAMP
);

-- ── INTERVIEW ROUNDS ───────────────────────────────────────────────────────
CREATE TABLE interview_rounds (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    interview_id  UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    round_number  INT NOT NULL,
    title         VARCHAR(255) NOT NULL,
    questions     TEXT,
    difficulty    VARCHAR(50),
    notes         TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── TAGS ───────────────────────────────────────────────────────────────────
CREATE TABLE tags (
    id    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name  VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE interview_tags (
    interview_id  UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    tag_id        UUID NOT NULL REFERENCES tags(id)       ON DELETE CASCADE,
    PRIMARY KEY (interview_id, tag_id)
);

-- ── BOOKMARKS ──────────────────────────────────────────────────────────────
CREATE TABLE bookmarks (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id       UUID NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
    interview_id  UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, interview_id)
);

-- ── COMMENTS ───────────────────────────────────────────────────────────────
CREATE TABLE comments (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    interview_id  UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
    content       TEXT NOT NULL,
    is_deleted    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── USER PREFERENCES ───────────────────────────────────────────────────────
CREATE TABLE user_preferences (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    target_companies    TEXT,
    target_role         VARCHAR(255),
    experience_level    VARCHAR(100),
    interview_timeline  VARCHAR(100),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── READ HISTORY ───────────────────────────────────────────────────────────
CREATE TABLE read_history (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id       UUID NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
    interview_id  UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    read_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, interview_id)
);

-- ── NOTIFICATIONS ──────────────────────────────────────────────────────────
CREATE TYPE notification_type_enum AS ENUM (
    'COMMENT_ON_YOUR_INTERVIEW',
    'INTERVIEW_PUBLISHED',
    'REFERRAL_USED',
    'NEW_INTERVIEW_FOR_COMPANY',
    'ONBOARDING'
);

CREATE TABLE notifications (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type       notification_type_enum NOT NULL,
    title      VARCHAR(255) NOT NULL,
    body       TEXT NOT NULL,
    deep_link  VARCHAR(500),
    is_read    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── FCM TOKENS ─────────────────────────────────────────────────────────────
CREATE TYPE platform_enum AS ENUM ('ANDROID', 'IOS');

CREATE TABLE fcm_tokens (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      TEXT NOT NULL UNIQUE,
    platform   platform_enum NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── REFERRALS ──────────────────────────────────────────────────────────────
CREATE TABLE referrals (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    referrer_id  UUID NOT NULL REFERENCES users(id),
    referred_id  UUID NOT NULL REFERENCES users(id),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### V2 — Indexes

`app/src/main/resources/db/migration/V2__add_indexes.sql`

```sql
CREATE INDEX idx_interviews_company_id  ON interviews(company_id);
CREATE INDEX idx_interviews_status      ON interviews(status);
CREATE INDEX idx_interviews_difficulty  ON interviews(difficulty);
CREATE INDEX idx_interviews_outcome     ON interviews(outcome);
CREATE INDEX idx_interviews_created_at  ON interviews(created_at DESC);
CREATE INDEX idx_interviews_user_id     ON interviews(user_id);
CREATE INDEX idx_bookmarks_user_id      ON bookmarks(user_id);
CREATE INDEX idx_read_history_user_id   ON read_history(user_id);
CREATE INDEX idx_notifications_user_id  ON notifications(user_id, is_read);
```

### V3 — Full-Text Search + Seed Data

`app/src/main/resources/db/migration/V3__add_fulltext_search_and_seed.sql`

```sql
-- Generated column — PostgreSQL automatically updates this when title/role/content changes
ALTER TABLE interviews
    ADD COLUMN search_vector TSVECTOR
    GENERATED ALWAYS AS (
        to_tsvector('english',
            COALESCE(title,   '') || ' ' ||
            COALESCE(role,    '') || ' ' ||
            COALESCE(content, '')
        )
    ) STORED;

CREATE INDEX idx_interviews_search ON interviews USING GIN(search_vector);

-- Seed tags
INSERT INTO tags (name) VALUES
    ('DSA'), ('System Design'), ('HR'), ('Behavioral'), ('SQL'),
    ('React'), ('Spring Boot'), ('React Native'), ('Android'), ('iOS'),
    ('DevOps'), ('Machine Learning'), ('Low Level Design');

-- Seed companies
INSERT INTO companies (name, slug) VALUES
    ('Google', 'google'),       ('Amazon', 'amazon'),
    ('Microsoft', 'microsoft'), ('Flipkart', 'flipkart'),
    ('Swiggy', 'swiggy'),       ('Zomato', 'zomato'),
    ('Razorpay', 'razorpay'),   ('PhonePe', 'phonepe'),
    ('CRED', 'cred'),           ('Paytm', 'paytm'),
    ('Infosys', 'infosys'),     ('TCS', 'tcs'),
    ('Wipro', 'wipro'),         ('Zoho', 'zoho'),
    ('Atlassian', 'atlassian');
```

### JPA Entities in `core` Module

All entities go in `core` because both `user` and `content` modules need to read them. One definition, shared everywhere.

```
core/src/main/kotlin/com/hirestory/core/
├── exception/
│   └── Exceptions.kt
├── enums/
│   ├── Difficulty.kt
│   ├── Outcome.kt
│   ├── SourceType.kt
│   ├── InterviewStatus.kt
│   ├── NotificationType.kt
│   └── Platform.kt
└── entity/
    ├── User.kt
    ├── Company.kt
    ├── Interview.kt
    ├── InterviewRound.kt
    ├── Tag.kt
    ├── Bookmark.kt
    ├── ReadHistory.kt
    ├── Comment.kt
    ├── Notification.kt
    └── FcmToken.kt
```

**Enums** (one file each, in `core/enums/`):

```kotlin
// core/src/main/kotlin/com/hirestory/core/enums/Difficulty.kt
package com.hirestory.core.enums
enum class Difficulty { EASY, MEDIUM, HARD }

// core/src/main/kotlin/com/hirestory/core/enums/Outcome.kt
package com.hirestory.core.enums
enum class Outcome { OFFER, REJECTED, GHOSTED }

// core/src/main/kotlin/com/hirestory/core/enums/SourceType.kt
package com.hirestory.core.enums
enum class SourceType { USER_SUBMITTED, CRAWLED }

// core/src/main/kotlin/com/hirestory/core/enums/InterviewStatus.kt
package com.hirestory.core.enums
enum class InterviewStatus { PENDING, PUBLISHED, REJECTED }

// core/src/main/kotlin/com/hirestory/core/enums/NotificationType.kt
package com.hirestory.core.enums
enum class NotificationType {
    COMMENT_ON_YOUR_INTERVIEW,
    INTERVIEW_PUBLISHED,
    REFERRAL_USED,
    NEW_INTERVIEW_FOR_COMPANY,
    ONBOARDING
}

// core/src/main/kotlin/com/hirestory/core/enums/Platform.kt
package com.hirestory.core.enums
enum class Platform { ANDROID, IOS }
```

**Entities:**

```kotlin
// core/src/main/kotlin/com/hirestory/core/entity/User.kt
package com.hirestory.core.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "users")
data class User(
    @Id
    @Column(columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "firebase_uid", nullable = false, unique = true)
    val firebaseUid: String,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    var name: String,

    @Column(name = "avatar_url")
    var avatarUrl: String? = null,

    @Column(name = "is_premium", nullable = false)
    var isPremium: Boolean = false,

    @Column(name = "read_count_this_month", nullable = false)
    var readCountThisMonth: Int = 0,

    @Column(name = "referral_code", nullable = false, unique = true)
    val referralCode: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

```kotlin
// core/src/main/kotlin/com/hirestory/core/entity/Company.kt
package com.hirestory.core.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "companies")
data class Company(
    @Id
    @Column(columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val name: String,

    @Column(name = "logo_url")
    var logoUrl: String? = null,

    @Column(nullable = false, unique = true)
    val slug: String,

    @Column(name = "interview_count", nullable = false)
    var interviewCount: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

```kotlin
// core/src/main/kotlin/com/hirestory/core/entity/Interview.kt
package com.hirestory.core.entity

import com.hirestory.core.enums.*
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "interviews")
data class Interview(
    @Id
    @Column(columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    val company: Company,

    @Column(name = "user_id")
    val userId: UUID? = null,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    val role: String? = null,
    val location: String? = null,

    @Column(name = "experience_years")
    val experienceYears: Int? = null,

    @Column(name = "rounds_count", nullable = false)
    val roundsCount: Int = 1,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "difficulty_enum")
    val difficulty: Difficulty = Difficulty.MEDIUM,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "outcome_enum")
    val outcome: Outcome = Outcome.OFFER,

    @Column(name = "salary_lpa", precision = 10, scale = 2)
    val salaryLpa: BigDecimal? = null,

    @Column(name = "source_url", unique = true)
    val sourceUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, columnDefinition = "source_type_enum")
    val sourceType: SourceType = SourceType.USER_SUBMITTED,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "interview_status_enum")
    var status: InterviewStatus = InterviewStatus.PENDING,

    @Column(name = "confidence_score", nullable = false)
    val confidenceScore: Int = 100,

    @OneToMany(mappedBy = "interview", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @OrderBy("roundNumber ASC")
    val rounds: List<InterviewRound> = emptyList(),

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "interview_tags",
        joinColumns         = [JoinColumn(name = "interview_id")],
        inverseJoinColumns  = [JoinColumn(name = "tag_id")]
    )
    val tags: List<Tag> = emptyList(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "published_at")
    var publishedAt: LocalDateTime? = null
)
```

```kotlin
// core/src/main/kotlin/com/hirestory/core/entity/InterviewRound.kt
package com.hirestory.core.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "interview_rounds")
data class InterviewRound(
    @Id
    @Column(columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    val interview: Interview,

    @Column(name = "round_number", nullable = false)
    val roundNumber: Int,

    @Column(nullable = false)
    val title: String,

    @Column(columnDefinition = "TEXT")
    val questions: String? = null,

    val difficulty: String? = null,

    @Column(columnDefinition = "TEXT")
    val notes: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

```kotlin
// core/src/main/kotlin/com/hirestory/core/entity/Tag.kt
package com.hirestory.core.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "tags")
data class Tag(
    @Id
    @Column(columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val name: String
)
```

```kotlin
// core/src/main/kotlin/com/hirestory/core/entity/Bookmark.kt
package com.hirestory.core.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "bookmarks")
data class Bookmark(
    @Id
    @Column(columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    val interview: Interview,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

```kotlin
// core/src/main/kotlin/com/hirestory/core/entity/ReadHistory.kt
package com.hirestory.core.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "read_history")
data class ReadHistory(
    @Id
    @Column(columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "interview_id", nullable = false)
    val interviewId: UUID,

    @Column(name = "read_at", nullable = false, updatable = false)
    val readAt: LocalDateTime = LocalDateTime.now()
)
```

```kotlin
// core/src/main/kotlin/com/hirestory/core/entity/Comment.kt
package com.hirestory.core.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "comments")
data class Comment(
    @Id
    @Column(columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "interview_id", nullable = false)
    val interviewId: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

```kotlin
// core/src/main/kotlin/com/hirestory/core/entity/Notification.kt
package com.hirestory.core.entity

import com.hirestory.core.enums.NotificationType
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "notifications")
data class Notification(
    @Id
    @Column(columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "notification_type_enum")
    val type: NotificationType,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val body: String,

    @Column(name = "deep_link")
    val deepLink: String? = null,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

```kotlin
// core/src/main/kotlin/com/hirestory/core/entity/FcmToken.kt
package com.hirestory.core.entity

import com.hirestory.core.enums.Platform
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "fcm_tokens")
data class FcmToken(
    @Id
    @Column(columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false, unique = true)
    val token: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "platform_enum")
    val platform: Platform,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

---

## Full File Layout After Chapter 3

```
hirestory-backend/
├── core/
│   └── src/main/kotlin/com/hirestory/core/
│       ├── exception/
│       │   └── Exceptions.kt                  ✅
│       ├── enums/
│       │   ├── Difficulty.kt                  ✅
│       │   ├── Outcome.kt                     ✅
│       │   ├── SourceType.kt                  ✅
│       │   ├── InterviewStatus.kt             ✅
│       │   ├── NotificationType.kt            ✅
│       │   └── Platform.kt                    ✅
│       └── entity/
│           ├── User.kt                        ✅
│           ├── Company.kt                     ✅
│           ├── Interview.kt                   ✅
│           ├── InterviewRound.kt              ✅
│           ├── Tag.kt                         ✅
│           ├── Bookmark.kt                    ✅
│           ├── ReadHistory.kt                 ✅
│           ├── Comment.kt                     ✅
│           ├── Notification.kt                ✅
│           └── FcmToken.kt                    ✅
└── app/
    └── src/main/resources/
        └── db/migration/
            ├── V1__create_initial_schema.sql  ✅
            ├── V2__add_indexes.sql            ✅
            └── V3__add_fulltext_search_and_seed.sql ✅
```

---

## Chapter 3 Checkpoint ✅

- [ ] Migration directory created: `app/src/main/resources/db/migration/`
- [ ] V1, V2, V3 SQL files created with correct naming (capital V, double underscore)
- [ ] `./gradlew :app:bootRun` → Flyway runs all 3 migrations automatically
- [ ] Connect to DB in IntelliJ (Database panel → + → PostgreSQL → host localhost, db hirestory_dev) → all tables visible
- [ ] All 10 entity files created in `core/entity/` with correct annotations
- [ ] All 6 enum files created in `core/enums/`
- [ ] Verify: run `SELECT * FROM companies;` in IntelliJ DB console → 15 seed companies appear

---

# Chapter 4 — Building the User Module {#chapter-4}

## Theory

### Firebase Auth — Lazy User Creation

Firebase does not call your backend when a user signs up. That means you do not need a webhook.

Instead — **lazy user creation**: the first time a logged-in user calls any protected API, your backend creates the user record automatically:

```
User taps Sign In with Google in your app
        ↓
Firebase handles authentication → gives your app a JWT token
        ↓
Your app calls GET /api/profile (sends the token in the header)
        ↓
Spring Boot extracts Firebase UID from the token
        ↓
UserService: look up user in DB by firebase_uid
        ↓
Not found? → create user now → return profile
Found?     → return profile
```

This means you have zero webhook setup, zero race conditions, and it works identically for every auth provider Firebase supports (Google, Apple, email/password).

### Firebase UID

Firebase UID looks like: `abc123XYZ789defGHI` — a short alphanumeric string. It comes from the `sub` (subject) field in the Firebase JWT. It is stable — it never changes for that user, even if they change their email or name.

### What is `@Transactional`?

`@Transactional` wraps a method in a database transaction. If anything throws an exception inside it, every DB change made in that method is rolled back automatically.

```kotlin
@Transactional
fun getOrCreateUser(userInfo: FirebaseUserInfo): UserProfileResponse {
    // Check if exists
    userRepository.findByFirebaseUid(userInfo.uid)?.let { return it.toResponse() }

    // Create user
    val user = userRepository.save(User(...))
    // If save throws → transaction rolls back → no partial state in DB
    return user.toResponse()
}
```

Use `@Transactional` on any service method that writes to the database.

---

## Implementation

The user module owns everything related to users: the repository, service, controller, and DTOs.

```
user/src/main/kotlin/com/hirestory/user/
├── repository/
│   └── UserRepository.kt
├── service/
│   └── UserService.kt
├── controller/
│   └── UserController.kt
└── dto/
    └── UserDtos.kt
```

### UserRepository

```kotlin
// user/src/main/kotlin/com/hirestory/user/repository/UserRepository.kt
package com.hirestory.user.repository

import com.hirestory.core.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    fun findByFirebaseUid(firebaseUid: String): User?
    fun findByEmail(email: String): User?
    fun findByReferralCode(referralCode: String): User?
    fun existsByEmail(email: String): Boolean
    fun existsByFirebaseUid(firebaseUid: String): Boolean
}
```

### DTOs

```kotlin
// user/src/main/kotlin/com/hirestory/user/dto/UserDtos.kt
package com.hirestory.user.dto

import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

// ── Response DTOs ──────────────────────────────────────────────────────────

data class UserProfileResponse(
    val id: UUID,
    val email: String,
    val name: String,
    val avatarUrl: String?,
    val isPremium: Boolean,
    val readsRemainingThisMonth: Int,
    val referralCode: String,
    val createdAt: LocalDateTime
)

// ── Request DTOs ───────────────────────────────────────────────────────────

data class UpdateProfileRequest(
    @field:Size(min = 1, max = 100, message = "Name must be 1–100 characters")
    val name: String?,

    val avatarUrl: String?
)

// ── Internal DTOs — used within the app, not HTTP ─────────────────────────

// Passed from JWT filter → UserService on every authenticated request
data class FirebaseUserInfo(
    val uid: String,
    val email: String?,
    val name: String?,
    val avatarUrl: String?
)

// Used by content/notification modules when they need user info
data class InternalUserResponse(
    val id: UUID,
    val isPremium: Boolean
)
```

### UserService

```kotlin
// user/src/main/kotlin/com/hirestory/user/service/UserService.kt
package com.hirestory.user.service

import com.hirestory.core.entity.User
import com.hirestory.core.exception.ResourceNotFoundException
import com.hirestory.user.dto.*
import com.hirestory.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private const val FREE_READS_PER_MONTH = 25

@Service
class UserService(private val userRepository: UserRepository) {

    /**
     * Called on every authenticated request.
     * Creates the user in DB the first time they hit any protected endpoint.
     * Returns the profile on every subsequent call.
     */
    @Transactional
    fun getOrCreateUser(userInfo: FirebaseUserInfo): UserProfileResponse {
        // Already exists — return immediately
        userRepository.findByFirebaseUid(userInfo.uid)?.let {
            return it.toProfileResponse()
        }

        // First time this Firebase user hits our backend — create them now
        val displayName = userInfo.name
            ?: userInfo.email?.substringBefore("@")
            ?: "User"

        val user = User(
            firebaseUid  = userInfo.uid,
            email        = userInfo.email ?: "${userInfo.uid}@unknown.com",
            name         = displayName,
            avatarUrl    = userInfo.avatarUrl,
            referralCode = generateUniqueReferralCode()
        )

        return userRepository.save(user).toProfileResponse()
    }

    fun getProfile(firebaseUid: String): UserProfileResponse {
        val user = userRepository.findByFirebaseUid(firebaseUid)
            ?: throw ResourceNotFoundException("User not found")
        return user.toProfileResponse()
    }

    @Transactional
    fun updateProfile(firebaseUid: String, request: UpdateProfileRequest): UserProfileResponse {
        val user = userRepository.findByFirebaseUid(firebaseUid)
            ?: throw ResourceNotFoundException("User not found")

        request.name?.let      { user.name      = it }
        request.avatarUrl?.let { user.avatarUrl = it }

        return userRepository.save(user).toProfileResponse()
    }

    /**
     * Called directly by ContentService and NotificationService.
     * Returns UUID and premium status — no HTTP involved.
     */
    fun resolveUser(firebaseUid: String): InternalUserResponse {
        val user = userRepository.findByFirebaseUid(firebaseUid)
            ?: throw ResourceNotFoundException("User not found: $firebaseUid")
        return InternalUserResponse(id = user.id, isPremium = user.isPremium)
    }

    /**
     * Convenience method — returns only the UUID.
     * Used when content module just needs to associate content with a user.
     */
    fun resolveUserId(firebaseUid: String): UUID {
        return resolveUser(firebaseUid).id
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private fun generateUniqueReferralCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"  // no ambiguous chars (0/O, 1/I)
        var code: String
        do {
            code = "HS-" + (1..5).map { chars.random() }.joinToString("")
        } while (userRepository.findByReferralCode(code) != null)
        return code
    }

    private fun User.toProfileResponse() = UserProfileResponse(
        id                      = id,
        email                   = email,
        name                    = name,
        avatarUrl               = avatarUrl,
        isPremium               = isPremium,
        readsRemainingThisMonth = FREE_READS_PER_MONTH - readCountThisMonth,
        referralCode            = referralCode,
        createdAt               = createdAt
    )
}
```

### UserController

```kotlin
// user/src/main/kotlin/com/hirestory/user/controller/UserController.kt
package com.hirestory.user.controller

import com.hirestory.user.dto.*
import com.hirestory.user.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class UserController(private val userService: UserService) {

    /**
     * GET /api/profile
     *
     * First call after login — creates user record if it does not exist yet.
     * Every subsequent call just returns the profile.
     *
     * FirebaseJwtFilter stores FirebaseUserInfo as a request attribute.
     * We use that to pass email + name + avatar to getOrCreateUser.
     */
    @GetMapping("/profile")
    fun getProfile(
        @AuthenticationPrincipal firebaseUid: String,
        request: HttpServletRequest
    ): ResponseEntity<UserProfileResponse> {
        val userInfo = request.getAttribute("firebaseUserInfo") as? FirebaseUserInfo
            ?: FirebaseUserInfo(
                uid       = firebaseUid,
                email     = null,
                name      = null,
                avatarUrl = null
            )
        return ResponseEntity.ok(userService.getOrCreateUser(userInfo))
    }

    /**
     * PUT /api/profile
     *
     * Update name and/or avatar URL.
     * @Valid ensures name is 1–100 characters if provided.
     */
    @PutMapping("/profile")
    fun updateProfile(
        @AuthenticationPrincipal firebaseUid: String,
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<UserProfileResponse> {
        return ResponseEntity.ok(userService.updateProfile(firebaseUid, request))
    }
}
```

---

## Full File Layout After Chapter 4

```
hirestory-backend/
├── core/
│   └── src/main/kotlin/com/hirestory/core/
│       ├── exception/Exceptions.kt            ✅
│       ├── enums/(all 6 files)                ✅
│       └── entity/(all 10 files)              ✅
├── user/
│   └── src/main/kotlin/com/hirestory/user/
│       ├── repository/
│       │   └── UserRepository.kt              ✅
│       ├── service/
│       │   └── UserService.kt                 ✅
│       ├── controller/
│       │   └── UserController.kt              ✅
│       └── dto/
│           └── UserDtos.kt                    ✅
└── app/
    └── src/main/kotlin/com/hirestory/app/
        ├── HirestoryApplication.kt            ✅
        └── exception/
            └── GlobalExceptionHandler.kt      ✅
```

---

## Chapter 4 Checkpoint ✅

- [ ] `UserRepository` with `findByFirebaseUid` compiles without errors
- [ ] `UserService.resolveUserId()` method exists — content module will use this in Chapter 6
- [ ] `UserController` `/api/profile` endpoint is mapped
- [ ] `./gradlew :app:bootRun` starts successfully
- [ ] Temporarily disable security (next chapter) and test with Postman:
    - `GET /api/profile` with header `X-Test-UID: testuid123` → you will wire this properly in Chapter 5

---

# Chapter 5 — JWT Authentication & Spring Security {#chapter-5}

## Theory

### How Spring Security Works

Every HTTP request passes through a filter chain before reaching your controller:

```
HTTP Request
     ↓
[Filter 1: CORS headers]
     ↓
[Filter 2: FirebaseJwtFilter — extracts & validates JWT]   ← you write this
     ↓
[Filter 3: Authorization check — is this endpoint public or protected?]
     ↓
Your Controller method
```

You do not call your filter manually. Spring runs it automatically on every request.

### How Firebase JWT Validation Works

Firebase uses RS256 — asymmetric cryptography:

```
1. Firebase signs your JWT using their PRIVATE key
2. Firebase publishes their PUBLIC key at a known JWKS URL
3. Your Spring Boot fetches the PUBLIC key on startup (and caches it)
4. For each request: verify the JWT signature using the PUBLIC key
5. If signature is valid → trust the token → extract Firebase UID
```

Firebase JWKS URL (same for all Firebase projects):

```
https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com
```

Firebase JWT claims:

```
sub     → Firebase UID  (e.g. "abc123XYZ789")
email   → user's email  (present if Google/email auth)
name    → display name  (present if Google auth)
picture → avatar URL    (present if Google auth)
aud     → YOUR Firebase project ID — verify this to prevent token reuse across projects
```

### Public vs Protected Endpoints

```
Public (no token required):
  GET  /api/interviews/**   → anyone reads the feed
  GET  /api/companies/**    → anyone reads company list
  GET  /api/search          → anyone searches
  GET  /api/shorts/**       → anyone views shorts
  GET  /actuator/health     → health check

Protected (valid Firebase JWT required):
  GET  /api/profile         → logged-in users only
  PUT  /api/profile         → logged-in users only
  POST /api/interviews      → logged-in users only
  POST /api/bookmarks/**    → logged-in users only
  GET  /api/notifications   → logged-in users only

Admin only:
  /api/admin/**             → users with ROLE_ADMIN

Internal (crawler → backend):
  /internal/**              → protect with API key in production
                              permitted for now (localhost only)
```

### `@AuthenticationPrincipal` — How It Works

After your filter runs, it stores the Firebase UID in Spring's `SecurityContext`. The `@AuthenticationPrincipal` annotation in controller methods retrieves it automatically:

```kotlin
// Filter sets the principal:
val auth = UsernamePasswordAuthenticationToken(
    userInfo.uid,   // ← this becomes the "principal"
    null,
    listOf(SimpleGrantedAuthority("ROLE_USER"))
)
SecurityContextHolder.getContext().authentication = auth

// Controller reads the principal:
@GetMapping("/profile")
fun getProfile(
    @AuthenticationPrincipal firebaseUid: String   // ← Spring extracts from SecurityContext
): ResponseEntity<UserProfileResponse>
```

---

## Implementation

All security code lives in the `app` module. Security is a cross-cutting concern that wraps everything — it belongs at the application layer.

```
app/src/main/kotlin/com/hirestory/app/
├── HirestoryApplication.kt
├── exception/
│   └── GlobalExceptionHandler.kt       (from Chapter 2)
├── security/
│   ├── FirebaseJwtValidator.kt         ← validates JWT, returns FirebaseUserInfo
│   ├── FirebaseJwtFilter.kt            ← runs on every request, calls validator
│   └── SecurityConfig.kt              ← defines public/protected rules
└── config/
    └── WebConfig.kt                    ← RestTemplate bean (used by crawler client)
```

### FirebaseJwtValidator

```kotlin
// app/src/main/kotlin/com/hirestory/app/security/FirebaseJwtValidator.kt
package com.hirestory.app.security

import com.hirestory.user.dto.FirebaseUserInfo
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.RemoteJWKSet
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URL

@Component
class FirebaseJwtValidator {

    @Value("\${firebase.project-id}")
    private lateinit var projectId: String

    // Firebase's public key endpoint — identical for ALL Firebase projects
    private val jwksUrl =
        "https://www.googleapis.com/service_accounts/v1/jwk/" +
        "securetoken@system.gserviceaccount.com"

    // RemoteJWKSet caches the public key and refreshes when Firebase rotates it
    private val keySource by lazy {
        RemoteJWKSet<SecurityContext>(URL(jwksUrl))
    }

    /**
     * Validates the JWT and returns the Firebase user info extracted from claims.
     * Throws SecurityException if the token is invalid or expired.
     */
    fun validate(token: String): FirebaseUserInfo {
        val jwtProcessor = DefaultJWTProcessor<SecurityContext>()
        val keySelector  = JWSVerificationKeySelector(JWSAlgorithm.RS256, keySource)
        jwtProcessor.jwsKeySelector = keySelector

        val claims = jwtProcessor.process(token, null)

        // Verify this token was issued for YOUR project — prevents token reuse attacks
        val audience = claims.audience?.firstOrNull()
        if (audience != projectId) {
            throw SecurityException(
                "JWT audience mismatch. Expected: $projectId, Got: $audience"
            )
        }

        return FirebaseUserInfo(
            uid       = claims.subject,
            email     = claims.getStringClaim("email"),
            name      = claims.getStringClaim("name"),
            avatarUrl = claims.getStringClaim("picture")
        )
    }
}
```

### FirebaseJwtFilter

```kotlin
// app/src/main/kotlin/com/hirestory/app/security/FirebaseJwtFilter.kt
package com.hirestory.app.security

import com.hirestory.user.dto.FirebaseUserInfo
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class FirebaseJwtFilter(
    private val firebaseJwtValidator: FirebaseJwtValidator
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.removePrefix("Bearer ").trim()

            try {
                val userInfo = firebaseJwtValidator.validate(token)

                // Store full user info as request attribute so controllers can access
                // email, name, avatarUrl for first-time user creation (Chapter 4)
                request.setAttribute("firebaseUserInfo", userInfo)

                // Set principal in SecurityContext — @AuthenticationPrincipal reads this
                val auth = UsernamePasswordAuthenticationToken(
                    userInfo.uid,   // principal — the Firebase UID string
                    null,           // credentials — not needed
                    listOf(SimpleGrantedAuthority("ROLE_USER"))
                )
                SecurityContextHolder.getContext().authentication = auth

            } catch (e: Exception) {
                // Invalid or expired token — do not set auth
                // Request continues as anonymous
                // SecurityConfig will block it if the endpoint requires auth
                logger.debug("Firebase JWT validation failed: ${e.message}")
            }
        }

        // Always continue the filter chain — SecurityConfig decides if auth is required
        filterChain.doFilter(request, response)
    }
}
```

### SecurityConfig

```kotlin
// app/src/main/kotlin/com/hirestory/app/security/SecurityConfig.kt
package com.hirestory.app.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val firebaseJwtFilter: FirebaseJwtFilter
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf  { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .cors  { it.configurationSource(corsConfigurationSource()) }
            .authorizeHttpRequests { auth ->
                auth
                    // ── Public endpoints — no token required ──────────────
                    .requestMatchers(HttpMethod.GET,  "/api/interviews/**").permitAll()
                    .requestMatchers(HttpMethod.GET,  "/api/companies/**").permitAll()
                    .requestMatchers(HttpMethod.GET,  "/api/search").permitAll()
                    .requestMatchers(HttpMethod.GET,  "/api/shorts/**").permitAll()
                    .requestMatchers("/actuator/**").permitAll()

                    // ── Internal endpoints — crawler → backend ─────────────
                    // In production: add API key validation middleware here
                    .requestMatchers("/internal/**").permitAll()

                    // ── Admin endpoints ────────────────────────────────────
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")

                    // ── Everything else requires a valid Firebase token ────
                    .anyRequest().authenticated()
            }
            .addFilterBefore(firebaseJwtFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOrigins = listOf(
            "http://localhost:3000",          // local web dev
            "https://hirestory.com",
            "https://www.hirestory.com"
        )
        config.allowedMethods  = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        config.allowedHeaders  = listOf("*")
        config.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
```

### WebConfig (RestTemplate Bean)

```kotlin
// app/src/main/kotlin/com/hirestory/app/config/WebConfig.kt
package com.hirestory.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class WebConfig {

    // RestTemplate is used by the crawler client in later chapters
    // to POST extracted interviews to /internal/interviews/from-crawl
    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()
}
```

### Update `application.yml` — Add Firebase Project ID

```yaml
# app/src/main/resources/application.yml
server:
  port: 8080

spring:
  application:
    name: hirestory-backend

  datasource:
    url: jdbc:postgresql://localhost:5432/hirestory_dev
    username: hirestory
    password: hirestory123
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  data:
    redis:
      host: localhost
      port: 6379

  flyway:
    enabled: true
    locations: classpath:db/migration

management:
  endpoints:
    web:
      exposure:
        include: health,info

# ── Firebase ──────────────────────────────────────────────────────────────
firebase:
  project-id: your-firebase-project-id   # Firebase Console → Project Settings → Project ID
```

**How to find your Firebase project ID:**

1. Go to https://console.firebase.google.com
2. Select your project
3. Gear icon → Project Settings
4. Copy "Project ID" — looks like `hirestory-app-12345`

### How to Get a Firebase Token for Testing in Postman

```
POST https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=YOUR_WEB_API_KEY

Body (JSON):
{
  "email": "test@example.com",
  "password": "yourpassword",
  "returnSecureToken": true
}

Response contains "idToken" — use this as Bearer token in Postman
```

Find your Web API Key: Firebase Console → Project Settings → General → Web API Key

Tokens are valid for 1 hour. After expiry, get a new one with the same POST request.

---

## Full Final File Layout After Chapter 5

```
hirestory-backend/
├── settings.gradle.kts                         ✅
├── build.gradle.kts                            ✅
│
├── core/
│   ├── build.gradle.kts                        ✅
│   └── src/main/kotlin/com/hirestory/core/
│       ├── exception/
│       │   └── Exceptions.kt                  ✅
│       ├── enums/
│       │   ├── Difficulty.kt                  ✅
│       │   ├── Outcome.kt                     ✅
│       │   ├── SourceType.kt                  ✅
│       │   ├── InterviewStatus.kt             ✅
│       │   ├── NotificationType.kt            ✅
│       │   └── Platform.kt                    ✅
│       └── entity/
│           ├── User.kt                        ✅
│           ├── Company.kt                     ✅
│           ├── Interview.kt                   ✅
│           ├── InterviewRound.kt              ✅
│           ├── Tag.kt                         ✅
│           ├── Bookmark.kt                    ✅
│           ├── ReadHistory.kt                 ✅
│           ├── Comment.kt                     ✅
│           ├── Notification.kt                ✅
│           └── FcmToken.kt                    ✅
│
├── user/
│   ├── build.gradle.kts                        ✅
│   └── src/main/kotlin/com/hirestory/user/
│       ├── repository/
│       │   └── UserRepository.kt              ✅
│       ├── service/
│       │   └── UserService.kt                 ✅
│       ├── controller/
│       │   └── UserController.kt              ✅
│       └── dto/
│           └── UserDtos.kt                    ✅
│
├── content/
│   ├── build.gradle.kts                        ✅ (skeleton, code in Ch6)
│   └── src/main/kotlin/com/hirestory/content/
│       └── (empty — built in Chapter 6)
│
├── notification/
│   ├── build.gradle.kts                        ✅ (skeleton, code in Ch10)
│   └── src/main/kotlin/com/hirestory/notification/
│       └── (empty — built in Chapter 10)
│
└── app/
    ├── build.gradle.kts                        ✅
    └── src/
        ├── main/
        │   ├── kotlin/com/hirestory/app/
        │   │   ├── HirestoryApplication.kt     ✅
        │   │   ├── exception/
        │   │   │   └── GlobalExceptionHandler.kt ✅
        │   │   ├── security/
        │   │   │   ├── FirebaseJwtValidator.kt ✅
        │   │   │   ├── FirebaseJwtFilter.kt    ✅
        │   │   │   └── SecurityConfig.kt       ✅
        │   │   └── config/
        │   │       └── WebConfig.kt            ✅
        │   └── resources/
        │       ├── application.yml             ✅
        │       └── db/migration/
        │           ├── V1__create_initial_schema.sql      ✅
        │           ├── V2__add_indexes.sql                ✅
        │           └── V3__add_fulltext_search_and_seed.sql ✅
        └── test/kotlin/com/hirestory/app/
```

---

## Chapter 5 Checkpoint ✅

- [ ] `FirebaseJwtValidator` compiles — `nimbus-jose-jwt` dependency resolves
- [ ] `FirebaseJwtFilter` registered as Spring bean
- [ ] `SecurityConfig` starts without errors
- [ ] `firebase.project-id` set in `application.yml`
- [ ] `./gradlew :app:bootRun` starts successfully
- [ ] `GET /api/companies` (public) → 200, returns company list — **no token needed**
- [ ] `GET /api/profile` with no token → **401 Unauthorized**
- [ ] `GET /api/profile` with valid Firebase Bearer token → **200**, user created in DB on first call
- [ ] Open `users` table in IntelliJ DB browser — new row appeared with correct `firebase_uid`
- [ ] Call `GET /api/profile` again with same token → same user returned, no duplicate row

---

## How the Modules Connect — Summary Diagram

```
HTTP Request (Authorization: Bearer <firebase-token>)
        ↓
[app] FirebaseJwtFilter
  → calls FirebaseJwtValidator.validate(token)
  → sets SecurityContext principal = firebaseUid
  → sets request attribute "firebaseUserInfo" = FirebaseUserInfo(uid, email, name, avatar)
        ↓
[app] SecurityConfig — is this endpoint public or protected?
        ↓
[user] UserController.getProfile(@AuthenticationPrincipal firebaseUid)
  → reads request attribute "firebaseUserInfo"
  → calls UserService.getOrCreateUser(userInfo)
        ↓
[user] UserService
  → calls UserRepository.findByFirebaseUid(uid)
  → creates User entity if not found
  → returns UserProfileResponse DTO
        ↓
HTTP Response 200 { id, email, name, isPremium, readsRemaining, referralCode }
```

When Chapter 6 builds the content module, `InterviewController` will call `userService.resolveUserId(firebaseUid)` directly — same method, same bean, zero HTTP calls between them.