# Chapter — Swagger / SpringDoc OpenAPI

### _Document your API so it tests itself. Never write a Postman collection by hand again._

---

> **Why this chapter exists:** Swagger is the first thing you set up after your first controller works — and it breaks in 6 different ways depending on your project setup. This chapter covers both the standard setup and the Spring Modulith setup, every mistake you'll make, and exactly how to fix them.

---

## The Problem This Solves

You write a controller. You want to test it. Your options are:
1. Write a curl command manually ❌
2. Set up Postman with every endpoint ❌
3. Add Swagger — it reads your code and auto-generates a live, testable UI ✅

Swagger also gives your Android/frontend team a **live contract** — they can see every endpoint, every request body field, every response, and test them all without writing any code.

---

## The Theory — What Swagger Actually Is

**Swagger UI** = the browser UI you open at `/swagger-ui/index.html`

**OpenAPI Spec** = the JSON document at `/v3/api-docs` that describes all your endpoints. Swagger UI reads this JSON and renders the UI.

**SpringDoc OpenAPI** = the library that scans your `@RestController` classes and automatically generates the OpenAPI JSON. You add it as a dependency — it does the scanning automatically.

```
Your @RestController classes
         ↓
SpringDoc scans them at startup
         ↓
Generates JSON at /v3/api-docs
         {
           "paths": {
             "/users": { "get": {...}, "post": {...} },
             "/users/{id}": { "get": {...}, "put": {...} }
           }
         }
         ↓
Swagger UI reads that JSON
         ↓
Renders the interactive UI
```

**The dependency version rule — memorise this:**

| Spring Boot version | SpringDoc version |
|--------------------|--------------------|
| Spring Boot 2.x | `springdoc-openapi-ui:1.x` |
| Spring Boot 3.x | `springdoc-openapi-starter-webmvc-ui:2.x` |
| Spring Boot 4.x | `springdoc-openapi-starter-webmvc-ui:3.x` |

Using the wrong version = startup failure or blank Swagger UI.

---

## Part 1 — Standard Setup (No Spring Modulith)

If your project does **not** use `spring-modulith-starter-core`, follow this path.

---

### Step 1 — Add the Dependency

```kotlin
// build.gradle.kts
dependencies {
    // Spring Boot 4.x → springdoc 3.x
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

    // Spring Boot 3.x → springdoc 2.x
    // implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
}
```

---

### Step 2 — Configure application.yaml

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs           # The JSON spec URL (default is already /v3/api-docs)
  swagger-ui:
    path: /swagger-ui.html       # The UI entry path
    try-it-out-enabled: true     # "Try it out" button is enabled by default
    operations-sorter: alpha     # Sort endpoints alphabetically
  show-actuator: false           # Don't show /actuator endpoints in Swagger
```

> ⚠️ **If you have `server.servlet.context-path: /api`:**
> All your URLs gain the `/api` prefix:
> - Swagger UI → `http://localhost:8080/api/swagger-ui/index.html`
> - API docs JSON → `http://localhost:8080/api/v3/api-docs`
>
> Do NOT include `/api` in the `springdoc` config paths — SpringDoc already knows about the context-path.

---

### Step 3 — Permit Swagger in Spring Security

Spring Security blocks **all** routes by default. You must explicitly permit Swagger paths.

**Critical rule:** Spring Security's filter chain runs **after** Tomcat strips the context-path. So if your context-path is `/api` and someone requests `/api/swagger-ui/index.html`, Spring Security sees `/swagger-ui/index.html` — without the `/api` prefix.

```kotlin
@Bean
fun securityFilter(http: HttpSecurity): SecurityFilterChain {
    http
        .csrf { it.disable() }
        .authorizeHttpRequests { auth ->
            auth
                // Swagger — paths WITHOUT the context-path prefix
                // Browser: /api/swagger-ui/index.html → Security sees: /swagger-ui/index.html
                .requestMatchers("/v3/api-docs").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/swagger-resources/**").permitAll()
                .requestMatchers("/webjars/**").permitAll()
                // Your own endpoints
                .requestMatchers("/users/**").permitAll()
                .anyRequest().authenticated()
        }
        .formLogin { it.disable() }
        .httpBasic { it.disable() }

    return http.build()
}
```

---

### Step 4 — Open the UI

With no Spring Security (or after permitting): `http://localhost:8080/swagger-ui/index.html`

With `context-path: /api`: `http://localhost:8080/api/swagger-ui/index.html`

You should see all your `@RestController` endpoints listed automatically.

---

### Step 5 — Add API Info (Optional but Professional)

```kotlin
// config/OpenApiConfig.kt
@Configuration
class OpenApiConfig {

    @Bean
    fun openApiInfo(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("DeliveryApp API")
                    .version("v1.0")
                    .description("REST API for the DeliveryApp backend")
                    .contact(Contact().name("Shreyas").email("shreyas@example.com"))
            )
            .addServersItem(Server().url("http://localhost:8080/api").description("Local Dev"))
    }
}
```

---

## Part 2 — Spring Modulith Setup

If your project has `spring-modulith-starter-core` in `build.gradle.kts`, the setup is different. Modulith adds its own springdoc integration that changes how endpoints are discovered and grouped.

---

### What Spring Modulith Does to Swagger

Spring Modulith treats your package structure as "modules". It auto-registers springdoc groups named after those modules. This is what you will see:

```
Swagger UI → "Select a definition" dropdown → "public"
URL: /api/v3/api-docs/public
Result: "No operations defined in spec!"
```

This happens because:
1. Modulith creates a `"public"` group mapped to its concept of "publicly visible APIs"
2. That group uses Modulith's internal visibility rules to find endpoints
3. If your controllers aren't explicitly marked as "public" in Modulith's terms, the group is empty

---

### The Fix — Define Your Own GroupedOpenApi Bean

When you define a `GroupedOpenApi` bean yourself with the same group name `"public"`, it **overrides** Modulith's auto-generated empty one.

```kotlin
// config/OpenApiConfig.kt
@Configuration
@OpenAPIDefinition(info = Info(title = "DeliveryApp API", version = "v1"))
class OpenApiConfig {

    @Bean
    fun publicApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("public")
            .packagesToScan("com.yourpackage.yourapp")  // ← your root app package
            // ⚠️ DO NOT add pathsToMatch() here
            // pathsToMatch("/api/**") would filter your endpoints to only /api/... paths
            // which matches nothing because context-path /api is already stripped
            .build()
    }
}
```

**`packagesToScan` must be your full root package.** For a project at `com.learn.learn`, it must be `"com.learn.learn"` — not `"com.learn"` (too shallow) and not `"com.learn.learn.controller"` (too specific, misses if you reorganise later).

---

### application.yaml for Modulith Project

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    try-it-out-enabled: true
    operations-sorter: alpha
  packages-to-scan: com.yourpackage.yourapp   # Belt-and-suspenders alongside the bean
  show-actuator: false
```

---

### How Modulith Modules Map to Swagger Groups (Advanced)

If you **want** separate groups per Modulith module (e.g. one group for "users", one for "orders"):

```kotlin
@Configuration
class OpenApiConfig {

    // Group 1: User module
    @Bean
    fun userApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("users")
            .packagesToScan("com.myapp.users")
            .build()

    // Group 2: Order module
    @Bean
    fun orderApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("orders")
            .packagesToScan("com.myapp.orders")
            .build()

    // Group 3: Everything (useful for a "full view")
    @Bean
    fun allApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("all")
            .packagesToScan("com.myapp")
            .build()
}
```

Each group becomes a separate entry in the "Select a definition" dropdown.

---

## Part 3 — Multiple Modules with Spring Modulith (Full Guide)

This is the real-world scenario: you have a proper Spring Modulith project with **separate modules** — users, orders, delivery, payments — each in their own package. You want Swagger to show each module as a separate group in the dropdown, AND a combined "all" view.

---

### What a Multi-Module Project Looks Like

```
src/main/kotlin/com/deliveryapp/
├── DeliveryAppApplication.kt          ← main class
│
├── users/                             ← Modulith module 1
│   ├── UserController.kt
│   ├── UserService.kt
│   ├── UserRepository.kt
│   ├── User.kt                        ← entity
│   └── dto/
│       ├── CreateUserRequest.kt
│       └── UserResponse.kt
│
├── orders/                            ← Modulith module 2
│   ├── OrderController.kt
│   ├── OrderService.kt
│   ├── OrderRepository.kt
│   ├── Order.kt
│   └── dto/
│       ├── CreateOrderRequest.kt
│       └── OrderResponse.kt
│
├── delivery/                          ← Modulith module 3
│   ├── DeliveryController.kt
│   ├── DeliveryService.kt
│   └── ...
│
├── payments/                          ← Modulith module 4
│   ├── PaymentController.kt
│   ├── PaymentService.kt
│   └── ...
│
└── config/                            ← shared config (NOT a Modulith module)
    ├── SecurityConfig.kt
    └── OpenApiConfig.kt
```

**In Spring Modulith, a "module" = a direct sub-package of your main package.**
So `com.deliveryapp.users` is the "users" module, `com.deliveryapp.orders` is the "orders" module, etc.

---

### Step 1 — The OpenApiConfig for Multiple Modules

```kotlin
// config/OpenApiConfig.kt
package com.deliveryapp.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    // ── Per-module groups ─────────────────────────────────────────────────

    @Bean
    fun usersModule(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("1. Users")                              // name shown in dropdown
            .packagesToScan("com.deliveryapp.users")        // exact module package
            .build()

    @Bean
    fun ordersModule(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("2. Orders")
            .packagesToScan("com.deliveryapp.orders")
            .build()

    @Bean
    fun deliveryModule(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("3. Delivery")
            .packagesToScan("com.deliveryapp.delivery")
            .build()

    @Bean
    fun paymentsModule(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("4. Payments")
            .packagesToScan("com.deliveryapp.payments")
            .build()

    // ── Combined "all" group ──────────────────────────────────────────────
    // Shows every endpoint from every module in one view
    // Most useful for QA / full API overview

    @Bean
    fun allModules(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("0. All Endpoints")                      // "0." sorts it to the top
            .packagesToScan("com.deliveryapp")              // root package = scans everything
            .build()

    // ── API metadata + JWT auth button ────────────────────────────────────

    @Bean
    fun openApiInfo(): OpenAPI {
        val jwtScheme = SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("Paste your JWT token here (without 'Bearer ' prefix)")

        return OpenAPI()
            .info(
                Info()
                    .title("DeliveryApp API")
                    .version("v1.0")
                    .description("Multi-module delivery platform API")
                    .contact(Contact().name("Backend Team").email("dev@deliveryapp.com"))
            )
            .addServersItem(Server().url("http://localhost:8080/api").description("Local Dev"))
            .addServersItem(Server().url("https://api.deliveryapp.com").description("Production"))
            .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
            .components(Components().addSecuritySchemes("bearerAuth", jwtScheme))
    }
}
```

---

### Step 2 — What Swagger UI Shows

After starting the app, open `http://localhost:8080/api/swagger-ui/index.html`:

```
"Select a definition" dropdown:
├── 0. All Endpoints    ← /api/v3/api-docs/0.%20All%20Endpoints
├── 1. Users            ← /api/v3/api-docs/1.%20Users
├── 2. Orders           ← /api/v3/api-docs/2.%20Orders
├── 3. Delivery         ← /api/v3/api-docs/3.%20Delivery
└── 4. Payments         ← /api/v3/api-docs/4.%20Payments
```

Each group only shows its own module's endpoints:
- "1. Users" → `POST /users`, `GET /users`, `GET /users/{id}`, `PUT /users/{id}`, `DELETE /users/{id}`
- "2. Orders" → `POST /orders`, `GET /orders/{id}`, `PUT /orders/{id}/status`
- "0. All Endpoints" → everything from all modules combined

---

### Step 3 — application.yaml for Multi-Module

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    try-it-out-enabled: true
    operations-sorter: alpha
    tags-sorter: alpha            # also sort tags (module names) alphabetically
  show-actuator: false

# NOTE: Do NOT set packages-to-scan at the root springdoc level
# when using per-module GroupedOpenApi beans.
# Each bean handles its own package scanning.
# Setting it at root level overrides the bean-level scanning.
```

---

### Step 4 — Security Config for Multi-Module

Same as single module — the paths don't change. The `/v3/api-docs/**` wildcard covers all groups:

```kotlin
.authorizeHttpRequests { auth ->
    auth
        // Swagger — all groups are served under /v3/api-docs/**
        .requestMatchers("/v3/api-docs").permitAll()
        .requestMatchers("/v3/api-docs/**").permitAll()    // ← covers /v3/api-docs/1.%20Users etc.
        .requestMatchers("/swagger-ui/**").permitAll()
        .requestMatchers("/swagger-ui.html").permitAll()
        .requestMatchers("/swagger-resources/**").permitAll()
        .requestMatchers("/webjars/**").permitAll()
        // Module public endpoints
        .requestMatchers("/users/**").permitAll()
        .requestMatchers("/health").permitAll()
        // Everything else requires JWT
        .anyRequest().authenticated()
}
```

---

### Step 5 — Tagging Controllers by Module

Add `@Tag` to each controller so Swagger groups endpoints visually within a module view:

```kotlin
// users/UserController.kt
@RestController
@RequestMapping("/users")
@Tag(name = "User Management", description = "Create, read, update, delete users")
class UserController(private val userService: UserService) { ... }

// orders/OrderController.kt
@RestController
@RequestMapping("/orders")
@Tag(name = "Order Management", description = "Place and track delivery orders")
class OrderController(private val orderService: OrderService) { ... }

// delivery/DeliveryController.kt
@RestController
@RequestMapping("/delivery")
@Tag(name = "Delivery Tracking", description = "Real-time delivery agent location and status")
class DeliveryController(private val deliveryService: DeliveryService) { ... }
```

---

### How the JSON Endpoints Work Per Module

Each `GroupedOpenApi` bean creates its own JSON endpoint:

```
GET /api/v3/api-docs              → full spec (all controllers, no group filter)
GET /api/v3/api-docs/1.%20Users  → only users module endpoints
GET /api/v3/api-docs/2.%20Orders → only orders module endpoints
```

You can hit these directly to check if a module's endpoints are being scanned:

```bash
# Check if users module endpoints are found
curl http://localhost:8080/api/v3/api-docs/1.%20Users | python3 -m json.tool | grep '"paths"' -A 20
```

If a module's JSON shows `"paths": {}`, the `packagesToScan` package is wrong for that module.

---

### Common Multi-Module Mistakes

#### ❌ Mistake A: `packagesToScan` pointing to wrong sub-package

```kotlin
// ❌ Package exists but has no controllers (controllers are in users.controller sub-package)
GroupedOpenApi.builder()
    .group("1. Users")
    .packagesToScan("com.deliveryapp.users.controller")  // too specific

// ✅ Point to the module root — SpringDoc scans all sub-packages recursively
GroupedOpenApi.builder()
    .group("1. Users")
    .packagesToScan("com.deliveryapp.users")   // finds controller, service, etc.
```

#### ❌ Mistake B: Two beans with the same group name

```kotlin
// ❌ Second bean silently wins — you see only one group, other is lost
@Bean fun usersApi() = GroupedOpenApi.builder().group("public").packagesToScan("com.deliveryapp.users").build()
@Bean fun allApi()   = GroupedOpenApi.builder().group("public").packagesToScan("com.deliveryapp").build()

// ✅ Every group must have a unique name
@Bean fun usersApi() = GroupedOpenApi.builder().group("1. Users").packagesToScan("com.deliveryapp.users").build()
@Bean fun allApi()   = GroupedOpenApi.builder().group("0. All").packagesToScan("com.deliveryapp").build()
```

#### ❌ Mistake C: Root-level `packages-to-scan` in YAML overrides all beans

```yaml
# ❌ This overrides ALL your GroupedOpenApi beans — every group now scans only this one package
springdoc:
  packages-to-scan: com.deliveryapp.users   # kills orders, delivery, payments groups

# ✅ Remove this when using GroupedOpenApi beans — let each bean handle its own scanning
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
  # No packages-to-scan here
```

#### ❌ Mistake D: Module controllers not in the right package

Spring Modulith enforces that module code stays inside the module package. If `OrderController` is in `com.deliveryapp.config` instead of `com.deliveryapp.orders`, it:
1. Violates Modulith's boundaries (Modulith will warn about this)
2. Won't appear in the "2. Orders" group (because `packagesToScan("com.deliveryapp.orders")` won't find it)

```kotlin
// ❌ Wrong location
package com.deliveryapp.config
class OrderController   // lives in config, not orders module

// ✅ Correct location
package com.deliveryapp.orders
class OrderController   // lives inside its module
```

---

### Full Working Example — DeliveryApp Multi-Module

Here is the complete, copy-paste ready setup for a 4-module DeliveryApp:

**build.gradle.kts:**
```kotlin
implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
implementation("org.springframework.modulith:spring-modulith-starter-core")
```

**application.yaml:**
```yaml
server:
  port: 8080
  servlet:
    context-path: /api

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    try-it-out-enabled: true
    operations-sorter: alpha
    tags-sorter: alpha
  show-actuator: false
```

**config/OpenApiConfig.kt:**
```kotlin
@Configuration
class OpenApiConfig {

    @Bean fun allModules()      = group("0. All Endpoints", "com.deliveryapp")
    @Bean fun usersModule()     = group("1. Users",         "com.deliveryapp.users")
    @Bean fun ordersModule()    = group("2. Orders",        "com.deliveryapp.orders")
    @Bean fun deliveryModule()  = group("3. Delivery",      "com.deliveryapp.delivery")
    @Bean fun paymentsModule()  = group("4. Payments",      "com.deliveryapp.payments")

    private fun group(name: String, pkg: String): GroupedOpenApi =
        GroupedOpenApi.builder().group(name).packagesToScan(pkg).build()

    @Bean
    fun openApiInfo(): OpenAPI = OpenAPI()
        .info(Info().title("DeliveryApp API").version("v1.0"))
        .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
        .components(Components().addSecuritySchemes("bearerAuth",
            SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
}
```

**config/SecurityConfig.kt (Swagger section):**
```kotlin
.requestMatchers("/v3/api-docs", "/v3/api-docs/**").permitAll()
.requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
.requestMatchers("/swagger-resources/**", "/webjars/**").permitAll()
```

**Result in Swagger UI dropdown:**
```
0. All Endpoints  ← every endpoint across all modules
1. Users          ← POST /users, GET /users, GET /users/{id} ...
2. Orders         ← POST /orders, GET /orders/{id} ...
3. Delivery       ← GET /delivery/{id}/track ...
4. Payments       ← POST /payments/charge ...
```



## Annotating Your Endpoints for Better Docs

SpringDoc reads your code as-is, but you can add annotations to make the docs much richer.

### Documenting a Controller

```kotlin
@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User management endpoints")  // Groups endpoints in UI
class UserController(private val userService: UserService) {

    @PostMapping
    @Operation(
        summary = "Create a new user",
        description = "Registers a new user with email and name. Email must be unique."
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "User created successfully"),
        ApiResponse(responseCode = "400", description = "Validation failed — check request body"),
        ApiResponse(responseCode = "409", description = "Email already registered")
    ])
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    @ApiResponse(responseCode = "404", description = "User not found")
    fun getUserById(
        @PathVariable
        @Parameter(description = "The user's unique ID", example = "42")
        id: Long
    ): UserResponse = userService.getUserById(id)
}
```

### Documenting a DTO

```kotlin
@Schema(description = "Request body to create a new user")
data class CreateUserRequest(

    @field:NotBlank
    @Schema(description = "Full name of the user", example = "Shreyas Damase")
    val name: String,

    @field:Email
    @Schema(description = "Unique email address", example = "shreyas@example.com")
    val email: String,

    @Schema(description = "Whether user has premium subscription", defaultValue = "false")
    val isPremium: Boolean = false
)
```

---

## Protecting Swagger in Production

In production, you do NOT want your API documented publicly. Two strategies:

### Strategy 1 — Disable via Profile

```yaml
# application-prod.yaml
springdoc:
  api-docs:
    enabled: false    # Disables /v3/api-docs endpoint entirely
  swagger-ui:
    enabled: false    # Disables Swagger UI entirely
```

Activate with: `SPRING_PROFILES_ACTIVE=prod`

### Strategy 2 — Require Auth for Swagger

```kotlin
.requestMatchers("/swagger-ui/**").hasRole("ADMIN")  // only ADMIN can see docs
.requestMatchers("/v3/api-docs/**").hasRole("ADMIN")
```

---

## Common Mistakes — The Complete List

### ❌ Mistake 1: Wrong springdoc version for your Spring Boot version

```
Error: java.lang.NoClassDefFoundError: jakarta/servlet/Filter
       (springdoc 2.x trying to load Spring Boot 4.x classes)
```

```kotlin
// ❌ Spring Boot 4.x with springdoc 2.x
implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

// ✅ Correct
implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
```

---

### ❌ Mistake 2: Forgetting to permit Swagger paths in Spring Security

```
Result: 403 Forbidden when opening Swagger UI
```

Spring Security's default is `anyRequest().authenticated()`. Swagger's paths (`/swagger-ui/**`, `/v3/api-docs/**`) must be explicitly permitted. See the security config in Step 3 above.

---

### ❌ Mistake 3: Adding `/api` prefix to security permit paths

```kotlin
// ❌ Wrong — Spring Security never sees the /api prefix
.requestMatchers("/api/swagger-ui/**").permitAll()

// ✅ Correct — Security sees the path AFTER context-path is stripped
.requestMatchers("/swagger-ui/**").permitAll()
```

Tomcat strips the context-path before handing the request to Spring Security.

---

### ❌ Mistake 4: `pathsToMatch()` filtering out your real endpoints

```kotlin
// ❌ This filters Swagger to only show paths starting with /api/v3/...
// which matches NONE of your controllers (/users, /orders, etc.)
GroupedOpenApi.builder()
    .group("public")
    .pathsToMatch("/api/v3/**")   // WRONG — don't add context-path here
    .build()

// ✅ Either omit pathsToMatch, or use the correct pattern
GroupedOpenApi.builder()
    .group("public")
    .packagesToScan("com.myapp")
    // No pathsToMatch — shows all endpoints
    .build()

// ✅ Or if you want to filter by path, use paths WITHOUT context-path:
GroupedOpenApi.builder()
    .group("public")
    .pathsToMatch("/users/**", "/orders/**")   // paths as Security sees them
    .build()
```

---

### ❌ Mistake 5: `packagesToScan` package too shallow

```kotlin
// ❌ com.learn exists but com.learn.learn.controller doesn't live directly in it
GroupedOpenApi.builder()
    .packagesToScan("com.learn")

// ✅ Must be the full root package of your app
GroupedOpenApi.builder()
    .packagesToScan("com.learn.learn")
```

SpringDoc scans the given package and all sub-packages. If the package is wrong, zero controllers are found.

---

### ❌ Mistake 6: Spring Modulith creating empty "public" group

```
Result: Swagger shows "public" in dropdown, "No operations defined in spec!"
URL:    /api/v3/api-docs/public returns { "paths": {} }
```

This is the Modulith integration hijacking springdoc. Fix: define your own `GroupedOpenApi` bean named `"public"` with `packagesToScan` pointing to your controllers. Your bean overrides Modulith's empty one.

---

### ❌ Mistake 7: Double `/api/api/` in the Swagger fetch URL

```
Error: Fetch error response status is 403 /api/api/v3/api-docs/public
```

Caused by setting `springdoc.swagger-ui.url: /api/v3/api-docs` when context-path is already `/api`. The context-path gets prepended automatically — you must NOT include it in the `url` property.

```yaml
# ❌ Wrong — causes /api/api/v3/api-docs
springdoc:
  swagger-ui:
    url: /api/v3/api-docs

# ✅ Correct — Swagger resolves this relative to the server, context-path added automatically
springdoc:
  swagger-ui:
    url: /v3/api-docs   # or just remove this line entirely
```

---

### ❌ Mistake 8: JPA config at root level instead of under `spring:`

```yaml
# ❌ Wrong — this key is ignored, hibernate settings don't apply
jpa:
  show-sql: true
  ddl-auto: create

# ✅ Correct
spring:
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: create
```

Not a Swagger mistake, but commonly made at the same time and causes confusing silent failures.

---

### ❌ Mistake 9: `@Controller` instead of `@RestController`

```kotlin
// ❌ @Controller is for server-side rendering (Thymeleaf, JSP)
// SpringDoc ignores it unless @ResponseBody is also present
@Controller
class UserController

// ✅ @RestController = @Controller + @ResponseBody — SpringDoc sees this
@RestController
class UserController
```

---

### ❌ Mistake 10: Swagger working locally but 404 in production

In production you probably set a different context-path, or disabled the springdoc endpoints. Check:
```yaml
# Make sure these are NOT set to false in your prod profile
springdoc:
  api-docs:
    enabled: true    # default is true
  swagger-ui:
    enabled: true    # default is true
```

---

## Debugging Swagger — Step by Step

When Swagger isn't working, follow this exact sequence:

### Step 1 — Check the raw JSON first (most important)

Before touching Swagger UI, go directly to:
```
http://localhost:8080/api/v3/api-docs
```

| What you see | What it means | Fix |
|---|---|---|
| `403 Forbidden` | Spring Security blocking it | Add `requestMatchers("/v3/api-docs/**").permitAll()` |
| `{"openapi":"3.1.0","info":{},"paths":{}}` | SpringDoc runs but finds no controllers | Fix `packagesToScan` or remove bad `pathsToMatch` |
| `{"openapi":"3.1.0","paths":{"/users":...}}` | SpringDoc is working, issue is only in UI | Check `swagger-ui.url` config |
| `404 Not Found` | SpringDoc not on classpath or wrong dependency | Check build.gradle.kts dependency |
| HTML login page | Form login is active and redirecting | Add `formLogin { it.disable() }` |

### Step 2 — Check the browser console

Open DevTools → Network tab → reload Swagger UI. Look for the fetch request to `/v3/api-docs` or `/v3/api-docs/public`. The status code tells you exactly what the problem is.

### Step 3 — Check startup logs

```
# Good log — SpringDoc found your controllers
o.s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped "{[/users],methods=[GET]}" onto ...
o.springdoc.api.AbstractOpenApiResource      : Init duration for springdoc-openapi is: 245 ms

# Bad log — no controllers found, Swagger will be empty
o.springdoc.api.AbstractOpenApiResource      : Init duration for springdoc-openapi is: 12 ms
# (very fast = found nothing to scan)
```

---

## DeliveryApp Connection

In the DeliveryApp, Swagger is used for:

1. **Internal testing** — test `POST /orders` with a real JSON body during development
2. **Team API contract** — the Android team reads Swagger to know exact field names and types
3. **Auth testing** — add the JWT token in the "Authorize" button, then test protected endpoints

```kotlin
// DeliveryApp OpenApiConfig — full production-ready version
@Configuration
class OpenApiConfig {

    @Bean
    fun publicApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("deliveryapp-api")
            .packagesToScan("com.deliveryapp")
            .build()

    @Bean
    fun openApiInfo(): OpenAPI {
        val bearerScheme = SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .name("Authorization")

        return OpenAPI()
            .info(Info()
                .title("DeliveryApp API")
                .version("v1.0")
                .description("Complete delivery platform REST API"))
            .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
            .components(Components().addSecuritySchemes("bearerAuth", bearerScheme))
            // ↑ This adds the "Authorize 🔒" button in Swagger UI
            // Paste your JWT token there to test protected endpoints
    }
}
```

---

## Chapter Project

Set up Swagger on your project so that:

- [ ] Swagger UI opens at `http://localhost:8080/api/swagger-ui/index.html`
- [ ] All endpoints from `UserController` and `HealthController` are visible
- [ ] `GET /users/{id}` shows the `@Parameter` description for `id`
- [ ] `POST /users` shows the request body schema with field descriptions from `@Schema`
- [ ] The API title is your project name (not "Your API Title")
- [ ] In production profile, Swagger is disabled (`springdoc.api-docs.enabled: false`)
- [ ] (Bonus) The "Authorize 🔒" button works and JWT token is sent with protected requests

---

## Quick Reference Card

```
Standard Project Setup:
1. Add springdoc dependency (version matches Spring Boot version)
2. Permit /swagger-ui/** and /v3/api-docs/** in SecurityConfig
   (WITHOUT context-path prefix)
3. Add springdoc config to application.yaml
4. Open: http://localhost:8080/{context-path}/swagger-ui/index.html

Spring Modulith Setup — extra step:
5. Define GroupedOpenApi bean with packagesToScan
6. Do NOT add pathsToMatch() unless you know exactly what you're filtering
7. Do NOT include context-path in url or pathsToMatch patterns

When debugging:
1. Hit /v3/api-docs directly — if 403, fix Security; if empty paths, fix packagesToScan
2. Check browser Network tab for the failing fetch URL
3. Look for double /api/api/ in the URL (means you added context-path to url property)
```
