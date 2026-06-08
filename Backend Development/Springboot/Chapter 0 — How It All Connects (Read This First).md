# Chapter 0 — How It All Connects

### _Read this before any other chapter. This is the map. Every chapter is one room in this building._

---

> **Why this chapter exists:** The biggest complaint from Spring Boot learners is that they understand each individual concept but cannot see how they fit together. This chapter shows you the **complete picture** — what happens when a phone hits your API, what code runs in what order, and what happens when something goes wrong.

---

## The Building Blocks — All 6 Pieces

Before the flow, know the 6 pieces. Every Spring Boot backend has all of these:

```
┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│    Entity   │   │     DTO     │   │ Repository  │
│             │   │             │   │             │
│ Maps to a   │   │ Shape of    │   │ Talks to    │
│ DB table    │   │ your JSON   │   │ database    │
│ (@Entity)   │   │ (data class)│   │ (interface) │
└─────────────┘   └─────────────┘   └─────────────┘

┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│   Service   │   │ Controller  │   │  Exception  │
│             │   │             │   │  Handler    │
│ Business    │   │ Receives    │   │ Converts    │
│ logic       │   │ HTTP        │   │ errors to   │
│ (@Service)  │   │(@RestCtrl.) │   │ JSON        │
└─────────────┘   └─────────────┘   └─────────────┘
```

**The direction of calling is always:**

```
Client (phone/web)
     ↕  HTTP
Controller        ← only layer that knows about HTTP
     ↕  Kotlin method call
Service           ← only layer that contains business rules
     ↕  Kotlin method call
Repository        ← only layer that talks to the database
     ↕  SQL (generated automatically)
PostgreSQL
```

**Rule: each layer only ever calls the one directly below it.**
- Controller never calls Repository directly
- Service never knows about HTTP (no `ResponseEntity`, no `@RequestParam`)
- Repository never contains business logic

---

## The Complete Flow — Step by Step

Let's trace exactly what happens when the Android app calls:

```
GET /api/users/42
```

This is a request to fetch the user with ID 42.

---

### Step 1 — The HTTP Request Arrives

```
Android App
    │
    │  GET /api/users/42
    │  Headers: { Authorization: "Bearer eyJhbGci..." }
    ▼
Embedded Tomcat (the web server inside Spring Boot)
    │
    │  Spring Security checks the token FIRST
    │  (before your controller even runs)
    ▼
Spring DispatcherServlet
    │
    │  Which controller handles GET /api/users/{id}?
    │  → Finds UserController.findById()
    ▼
UserController.findById(id = 42)
```

**What Spring does automatically:**
- Extracts `42` from the URL and converts it to a `Long` — you get `id: Long = 42`
- Reads the JWT token from the Authorization header and validates it
- If the token is invalid → returns `401 Unauthorized` **before your code runs**

---

### Step 2 — Controller Receives the Request

```kotlin
@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService   // injected by Spring
) {

    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ResponseEntity<UserDto> {
        // Controller's ONLY job:
        // 1. Extract the input from the HTTP request ✓ (done by @PathVariable)
        // 2. Call the service
        // 3. Wrap the result in a ResponseEntity and return it

        val user = userService.findById(id)   // → goes to Step 3
        return ResponseEntity.ok(user)        // ← comes back here after Step 3
    }
}
```

**What controller does NOT do:**
- ❌ No `SELECT * FROM users WHERE id = ?` — that's the repository's job
- ❌ No `if (user.isPremium) doSomething()` — that's the service's job
- ❌ No `try/catch` for every possible error — that's the exception handler's job

---

### Step 3 — Service Receives the Call

```kotlin
@Service
class UserService(
    private val userRepository: UserRepository   // injected by Spring
) {

    @Transactional(readOnly = true)
    fun findById(id: Long): UserDto {
        // Service's job:
        // 1. Business rules: is this request allowed? any conditions?
        // 2. Call the repository to get data
        // 3. Transform the Entity into a DTO before returning

        // Business rule check (if any)
        // For a simple find, there may be none — the rule is just "find the user"

        // Call repository → goes to Step 4
        val user = userRepository.findById(id)
            ?: throw UserNotFoundException(id)   // ← if null, throw exception → goes to Error Flow

        // Convert Entity → DTO (never return raw Entity to controller)
        return UserDto(
            id = user.id!!,
            name = user.name,
            email = user.email,
            isPremium = user.isPremium
        )
        // → goes back to Controller Step 2
    }
}
```

**What `@Transactional(readOnly = true)` does:**
- Opens a database session (connection) when this method starts
- Closes it when this method ends
- `readOnly = true` tells the DB "I'm only reading, no writes" → slight performance boost
- All repository calls inside this method share the same session

---

### Step 4 — Repository Hits the Database

```kotlin
@Repository
interface UserRepository : JpaRepository<User, Long> {
    // JpaRepository gives findById() for free — you write zero code here
}
```

**What Spring generates for `findById(42)`:**

```sql
SELECT u.id, u.name, u.email, u.is_premium, u.created_at
FROM users u
WHERE u.id = 42
LIMIT 1
```

**What the repository returns:**
- If a row exists → returns `User?` (a Kotlin object mapped from the database row)
- If no row → returns `null`

That `null` goes back to the Service in Step 3, which throws `UserNotFoundException`.

---

### Step 5 — The Response Travels Back

```
PostgreSQL returns the row
    ↓
Repository maps it to a User entity (Kotlin object)
    ↓
Service converts User entity → UserDto
    ↓
Controller wraps UserDto in ResponseEntity.ok(userDto)
    ↓
Spring converts UserDto to JSON automatically (Jackson library)
    ↓
Tomcat sends HTTP 200 response to the Android app

HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": 42,
  "name": "Shreyas",
  "email": "shreyas@example.com",
  "isPremium": false
}
```

**That's the complete success path. 5 steps. Same pattern for every single API endpoint in your app.**

---

## The Complete Flow Diagram

```
Android App
    │
    │  GET /api/users/42
    │  Header: Bearer <token>
    ▼
┌─────────────────────────────────────────────┐
│  Spring Security Filter                     │
│  • Reads JWT token                          │
│  • Validates signature                      │
│  • Extracts userId from token               │
│  • If invalid → 401 Unauthorized IMMEDIATELY│
└─────────────────────────────────────────────┘
    │  token is valid
    ▼
┌─────────────────────────────────────────────┐
│  UserController.findById(id = 42)           │
│  • Extracts id from URL path                │
│  • Calls userService.findById(42)           │
└─────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────┐
│  UserService.findById(id = 42)              │
│  • Opens DB session (@Transactional)        │
│  • Calls userRepository.findById(42)        │
│  • If null → throws UserNotFoundException   │
│  • If found → converts Entity to DTO        │
│  • Closes DB session                        │
└─────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────┐
│  UserRepository.findById(42)                │
│  • Generates SQL: SELECT * FROM users       │
│               WHERE id = 42                 │
│  • Runs it against PostgreSQL               │
│  • Maps result row → User entity            │
│  • Returns User? (null if not found)        │
└─────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────┐
│  PostgreSQL                                 │
│  • Executes the SQL                         │
│  • Returns 0 or 1 row                       │
└─────────────────────────────────────────────┘
    │ 
    │ (response travels back UP through the layers)
    │
    ▼
┌─────────────────────────────────────────────┐
│  Jackson (JSON library)                     │
│  • Converts UserDto → JSON string           │
└─────────────────────────────────────────────┘
    │
    ▼
Android App receives:
HTTP 200 OK  +  { "id": 42, "name": "Shreyas", ... }
```

---

## The Error Flow — What Happens When Things Go Wrong

This is the part most tutorials skip. Here are **all 4 types of errors** and exactly what happens.

---

### Error Type 1 — User Not Found (Business Logic Error)

```
Android App calls GET /api/users/999
User with ID 999 does not exist in the database
```

**The flow:**

```
UserController.findById(999)
    ↓
UserService.findById(999)
    ↓
userRepository.findById(999) → returns null
    ↓
Service throws:
    throw UserNotFoundException(999)
        ↓
This exception travels UP through all the layers
It does NOT go back to the controller as a return value
It JUMPS directly to...
    ↓
@ControllerAdvice GlobalExceptionHandler
    ↓
@ExceptionHandler(UserNotFoundException::class)
fun handleUserNotFound(ex: UserNotFoundException): ResponseEntity<ErrorResponse>
    ↓
Returns: HTTP 404 + JSON error body
```

**The code for this:**

```kotlin
// 1. Define the exception
class UserNotFoundException(id: Long) : RuntimeException("User not found: id=$id")

// 2. Throw it in the service
val user = userRepository.findById(id)
    ?: throw UserNotFoundException(id)

// 3. Catch it in the global handler (one handler catches all 404s across your entire app)
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(ex: UserNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)              // HTTP 404
            .body(ErrorResponse(
                status = 404,
                error = "Not Found",
                message = ex.message ?: "Resource not found"
            ))
    }
}

// 4. The error shape your Android app receives
data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val timestamp: String = LocalDateTime.now().toString()
)
```

**Android app receives:**
```json
HTTP 404 Not Found
{
  "status": 404,
  "error": "Not Found",
  "message": "User not found: id=999",
  "timestamp": "2024-01-15T10:30:00"
}
```

---

### Error Type 2 — Validation Error (Bad Input From App)

```
Android App calls POST /api/users
Body: { "name": "", "email": "not-an-email" }
```

**The flow:**

```
UserController.create(@RequestBody @Valid dto: CreateUserDto)
    ↓
Spring validates the DTO BEFORE your method even runs
    • name is blank → @NotBlank fails
    • email format wrong → @Email fails
    ↓
Spring throws MethodArgumentNotValidException
    ↓
@ExceptionHandler(MethodArgumentNotValidException::class)
    ↓
Returns: HTTP 400 + list of all validation errors
```

**The code:**

```kotlin
// 1. DTO with validation annotations
data class CreateUserDto(
    @field:NotBlank(message = "Name cannot be empty")
    val name: String,

    @field:Email(message = "Must be a valid email")
    val email: String,

    @field:Min(value = 0, message = "Age cannot be negative")
    val age: Int
)

// 2. Controller uses @Valid
@PostMapping
fun create(@RequestBody @Valid dto: CreateUserDto): ResponseEntity<UserDto> {
    // This code NEVER runs if validation fails
    val user = userService.create(dto)
    return ResponseEntity.status(HttpStatus.CREATED).body(user)
}

// 3. Handler in GlobalExceptionHandler
@ExceptionHandler(MethodArgumentNotValidException::class)
fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
    val errors = ex.bindingResult.fieldErrors
        .map { "${it.field}: ${it.defaultMessage}" }

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse(
            status = 400,
            error = "Validation Failed",
            message = errors.joinToString(", ")
        ))
}
```

**Android app receives:**
```json
HTTP 400 Bad Request
{
  "status": 400,
  "error": "Validation Failed",
  "message": "name: Name cannot be empty, email: Must be a valid email",
  "timestamp": "2024-01-15T10:30:00"
}
```

---

### Error Type 3 — Authentication Error (No or Bad Token)

```
Android App calls GET /api/users/42
No Authorization header, or token is expired
```

**The flow:**

```
HTTP request arrives at Tomcat
    ↓
Spring Security Filter reads Authorization header
    ↓
Header missing → throws AuthenticationException
Token expired → throws ExpiredJwtException
Token signature invalid → throws SignatureException
    ↓
Spring Security returns HTTP 401 IMMEDIATELY
Your controller, service, repository — NONE of these run
    ↓
```

**Android app receives:**
```json
HTTP 401 Unauthorized
{
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token has expired"
}
```

**The key point:** Security errors happen before your code runs. This is by design.

---

### Error Type 4 — Unexpected Server Error (Bug in Your Code)

```
Any request that causes a NullPointerException, a database constraint violation,
or any exception you didn't explicitly handle
```

**The flow:**

```
UserService.create(dto) 
    ↓
Accesses user.address.city  ← user.address is null → NullPointerException
    ↓
Exception travels up through all layers
    ↓
No specific @ExceptionHandler exists for NullPointerException
    ↓
Falls through to the generic catch-all handler
    ↓
Returns: HTTP 500 + generic error message
(Never expose the actual exception message to the client in production)
```

**The catch-all handler:**

```kotlin
@ExceptionHandler(Exception::class)  // Catches EVERYTHING not caught above
fun handleGeneric(ex: Exception): ResponseEntity<ErrorResponse> {
    // Log the full error so YOU can see what went wrong
    log.error("Unhandled exception", ex)

    // Return a safe, generic message to the client
    // Never expose internal stack traces
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse(
            status = 500,
            error = "Internal Server Error",
            message = "Something went wrong. Please try again."
        ))
}
```

**Android app receives:**
```json
HTTP 500 Internal Server Error
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Something went wrong. Please try again."
}
```

---

## The Complete Error Flow Diagram

```
Any API Request
    │
    ▼
Spring Security
    ├── No token / Bad token → 401 Unauthorized  (your code never runs)
    └── Valid token → continue
    │
    ▼
Controller
    ├── @Valid fails → 400 Bad Request  (your service never runs)
    └── Valid input → call service
    │
    ▼
Service
    ├── Business rule fails → throw CustomException  ─────────────────┐
    └── Calls repository                                               │
    │                                                                  │
    ▼                                                                  │
Repository                                                             │
    ├── DB constraint violated → throws DataIntegrityViolationException│
    └── Query runs successfully                                        │
    │                                                                  │
    ▼                                                                  │
PostgreSQL                                                             │
    ├── Connection error → DataAccessException ─────────────────────── │
    └── Returns data                                                   │
    │                                                                  │
    ▼  (happy path)                               (error path)        │
Response                                               │               │
travels back                                           ▼               ▼
normally                                    @ControllerAdvice catches it
                                            Maps exception → HTTP status + JSON
                                            Returns error response to Android app
```

---

## The Global Exception Handler — Your Safety Net

This single file catches ALL exceptions across your entire app. You write it once. It handles everything.

```kotlin
// src/main/kotlin/com/example/deliveryapp/exception/GlobalExceptionHandler.kt

@RestControllerAdvice  // Applies to ALL controllers
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(this::class.java)

    // ── Your custom exceptions ───────────────────────────────────────────

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(ex: UserNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(404, "Not Found", ex.message!!))
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse(403, "Forbidden", "You don't have permission"))
    }

    // ── Validation errors (from @Valid on DTOs) ──────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(400, "Validation Failed", errors))
    }

    // ── Database errors ──────────────────────────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDuplicateKey(ex: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
        // Happens when you try to insert a duplicate email, slug, etc.
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(409, "Conflict", "A record with this value already exists"))
    }

    // ── Catch-all for unexpected bugs ────────────────────────────────────

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception: ${ex.javaClass.simpleName}", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(500, "Internal Server Error", "Something went wrong"))
    }
}

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val timestamp: String = LocalDateTime.now().toString()
)
```

---

## HTTP Status Code Reference

Every response from your API has a status code. Here are the ones you'll use constantly:

| Code | Name | When to use |
|------|------|-------------|
| `200` | OK | `GET` request succeeded, returning data |
| `201` | Created | `POST` request succeeded, resource was created |
| `204` | No Content | `DELETE` request succeeded, nothing to return |
| `400` | Bad Request | Client sent invalid input (validation failed) |
| `401` | Unauthorized | No token, or token is expired/invalid |
| `403` | Forbidden | Token is valid but user doesn't have permission |
| `404` | Not Found | Requested resource doesn't exist |
| `409` | Conflict | Duplicate value (email already registered, slug taken) |
| `422` | Unprocessable Entity | Input format is valid but semantically wrong |
| `500` | Internal Server Error | Bug in your code — you need to check the logs |

---

## Real Example: Creating a User (POST Flow)

Let's trace `POST /api/users` with a valid body to see the full create flow:

```
Android App
POST /api/users
Body: { "name": "Shreyas", "email": "shreyas@example.com" }

Step 1: Security checks token → valid, continue

Step 2: Controller receives request
    @PostMapping
    fun create(@RequestBody @Valid dto: CreateUserDto): ResponseEntity<UserDto>
    
    → @Valid checks: name is not blank ✓, email format valid ✓
    → Calls userService.create(dto)

Step 3: Service runs business logic
    @Transactional
    fun create(dto: CreateUserDto): UserDto
    
    → Checks: does email already exist?
        userRepository.existsByEmail(dto.email)  → false, continue
    
    → Generates a referral code
        val referralCode = UUID.randomUUID().toString().take(8).uppercase()
    
    → Creates the entity (Kotlin object, not yet in DB)
        val user = User(
            name = dto.name,
            email = dto.email,
            clerkId = dto.clerkId,
            referralCode = referralCode
        )
    
    → Saves to database
        val savedUser = userRepository.save(user)
        (user.id is now populated — DB assigned it)
    
    → Converts to DTO and returns
        return UserDto(id = savedUser.id!!, name = savedUser.name, ...)

Step 4: Repository generates SQL
    INSERT INTO users (name, email, clerk_id, referral_code, created_at)
    VALUES ('Shreyas', 'shreyas@example.com', 'clk_xxx', 'ABC12345', NOW())
    RETURNING id  ← PostgreSQL returns the generated ID

Step 5: Controller wraps in ResponseEntity
    return ResponseEntity.status(201).body(userDto)

Step 6: Jackson converts UserDto → JSON

Android App receives:
HTTP 201 Created
{
  "id": 1,
  "name": "Shreyas",
  "email": "shreyas@example.com",
  "isPremium": false
}
```

---

## What Each File in Your Project Does

Read this table and then look at your project structure. Every file has exactly one job.

| File | Layer | Job | What it knows about |
|------|-------|-----|---------------------|
| `UserController.kt` | Controller | Handles HTTP | URLs, request bodies, response codes |
| `UserService.kt` | Service | Business rules | Entities, repositories, business decisions |
| `UserRepository.kt` | Repository | Database | SQL (auto-generated), Entity classes |
| `User.kt` | Entity | DB mapping | Database columns, relationships |
| `UserDto.kt` | DTO | API contract | JSON field names, validation rules |
| `GlobalExceptionHandler.kt` | Cross-cutting | Error handling | Exceptions → HTTP responses |
| `application.yml` | Config | Settings | DB URL, port, credentials |

---

## The "Missing Concepts" Glossary

These terms appear in every chapter. Understand them here once.

### Bean
Any class that Spring manages (creates, wires, destroys). Every `@Service`, `@Repository`, `@RestController`, `@Component` is a bean. Spring creates exactly one instance of each bean and reuses it.

### Application Context
Spring's "container" — the object that holds all your beans. When Spring Boot starts, it builds the Application Context: scans all your classes, creates all beans, wires them together. If building the context fails (bad config, missing bean), the app crashes at startup before Tomcat even starts.

### Dependency Injection
Spring sees that `UserController` needs `UserService`. Spring creates `UserService` first, then creates `UserController` and passes `UserService` into its constructor. You never call `new` — Spring does it.

### @Transactional
Wraps your method in a database transaction. Think of it as:
```kotlin
@Transactional
fun doSomething() {
    // Implicit: BEGIN TRANSACTION
    
    userRepository.save(user)   // writes to DB
    notificationRepository.save(notification)  // writes to DB
    
    // Implicit: COMMIT (both writes happen together)
    // If anything throws: ROLLBACK (both writes are cancelled)
}
```
Without `@Transactional`: each `save()` is its own separate transaction. If the second one fails, the first one is already committed. Data is inconsistent.

### Entity vs DTO
- **Entity** (`User.kt` with `@Entity`) — this is your database table mapped to Kotlin. Only the Service and Repository layer should handle entities. Never return an entity directly from a controller.
- **DTO** (`UserDto.kt`, just a `data class`) — this is the JSON shape your API exposes. Controller → Client. Service converts entities to DTOs before returning.

### `interface` in UserRepository
Why is it an interface? Because **you don't write the implementation**. Spring Data JPA reads your interface at startup and generates the implementation automatically. You write:
```kotlin
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
}
```
Spring generates: a class that implements this, with `findByEmail` running `SELECT * FROM users WHERE email = ?`. You never write that SQL.

### `lateinit var` vs Constructor Injection
```kotlin
// ❌ Don't use this — field injection
@Service
class UserService {
    @Autowired
    lateinit var userRepository: UserRepository  // "lateinit" = "I promise it won't be null when used"
}

// ✅ Use this — constructor injection
@Service
class UserService(
    private val userRepository: UserRepository  // Spring passes this in when creating UserService
)
```
Constructor injection is better because: if `userRepository` is missing, the app crashes at startup (you know immediately). With `lateinit var`, it crashes at runtime when the method runs (you find out much later).

### `data class` vs `class`
```kotlin
// data class — use for DTOs (things sent over the API)
// Automatically gets: equals(), hashCode(), toString(), copy()
// Signals: "this is just data, not a managed object"
data class UserDto(val id: Long, val name: String)

// class — use for JPA Entities
// JPA needs to create instances without calling your constructor (for lazy loading proxies)
// data class + JPA causes subtle bugs
@Entity
class User(val id: Long?, val name: String)
```

---

## How the Chapters Build on This

Every chapter in this guide adds one or more pieces to this map:

| Chapter | Adds to the map | Key concept |
|---------|----------------|-------------|
| **Chapter 1** | Project setup, all 6 layers introduced | Dependency injection, `application.yml`, entry point |
| **Chapter 2** | The Entity layer | JPA annotations, relationships, FetchType, Flyway |
| **Chapter 3** | The Repository layer | Query methods, pagination, `@Query`, N+1 fix |
| **Chapter 4** | The Controller + Service layers | DTOs, validation, `@ControllerAdvice`, error handling |
| **Chapter 5** | The Security layer (before controller) | JWT, Spring Security filter chain, roles |
| **Chapter 6** | Redis (between Service and DB) | Caching, counters, session state |
| **Chapter 7** | Message queue (outside the request flow) | Async processing, RabbitMQ |
| **Chapter 10** | Testing every layer | MockMvc, Testcontainers, unit tests |

---

## Before Moving to Chapter 1 — Your Checklist

You are ready for Chapter 1 when you can answer:

- [ ] What are the 3 layers of a Spring Boot backend, and what does each one do?
- [ ] What is the direction of method calls? (Controller calls Service calls Repository — never backwards)
- [ ] What happens before your controller runs? (Spring Security checks the token)
- [ ] Where does exception handling happen? (GlobalExceptionHandler — one place, catches everything)
- [ ] Why is `UserRepository` an interface with no method implementations?
- [ ] What is a DTO and why don't you return an Entity directly from a controller?
- [ ] What does `@Transactional` do in one sentence?

If you cannot answer these, re-read the sections above. These 7 concepts are the foundation everything else is built on.

---

_→ Continue to [[Chapter 1 — Spring Boot Fundamentals]] — Project setup, Dependency Injection deep dive, your first REST endpoint_
