

### For Android Jetpack Compose Developers

---

## Table of Contents

1. [The Story — Why Retrofit Exists](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#1-the-story--why-retrofit-exists)
2. [What Retrofit Actually Is](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#2-what-retrofit-actually-is)
3. [The Stack — Retrofit, OkHttp, Okio](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#3-the-stack--retrofit-okhttp-okio)
4. [Version History — How It Evolved](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#4-version-history--how-it-evolved)
5. [How HTTP Works (The Foundation)](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#5-how-http-works-the-foundation)
6. [Retrofit Builder — Every Property Explained](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#6-retrofit-builder--every-property-explained)
7. [OkHttpClient — Every Property Explained](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#7-okhttpclient--every-property-explained)
8. [API Interface Annotations — Complete Reference](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#8-api-interface-annotations--complete-reference)
9. [Converter Factories — JSON Parsing](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#9-converter-factories--json-parsing)
10. [Interceptors — Deep Dive](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#10-interceptors--deep-dive)
11. [Call Adapters — Return Types](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#11-call-adapters--return-types)
12. [Response Handling](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#12-response-handling)
13. [Error Handling](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#13-error-handling)
14. [How a Request Flows — Step by Step](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#14-how-a-request-flows--step-by-step)
15. [Retrofit + Hilt — Production Setup](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#15-retrofit--hilt--production-setup)
16. [Common Interview Questions](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#16-common-interview-questions)
17. [Quick Reference Cheat Sheet](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#17-quick-reference-cheat-sheet)

---

---

# 1. The Story — Why Retrofit Exists

## The Problem (Before Retrofit, ~2010–2012)

Before Retrofit, Android developers had to make network calls using `HttpURLConnection` or Apache's `HttpClient`. Every network call looked like this in your head:

```
1. Open a connection to the server
2. Set HTTP method (GET, POST...)
3. Set headers manually
4. Write request body as bytes
5. Read response bytes
6. Convert bytes to String
7. Parse that String as JSON manually
8. Handle every possible exception
9. Close the connection
10. Do all of this on a background thread (crash if on main thread)
11. Switch back to main thread to update UI
```

Every single endpoint required 50–100 lines of boilerplate. Change the API? Rewrite everything. Add authentication headers? Add them to every single call manually.

## The Solution

In **2013**, **Square Inc.** — the same company behind OkHttp, Dagger, Picasso, and many Android tools — released **Retrofit**.

The idea was radical for its time: **describe your API as a Java interface, and Retrofit writes all the HTTP code for you.**

```
Before:  50-100 lines per endpoint, manual JSON parsing, manual threading
After:   1 line per endpoint, automatic JSON parsing, automatic threading
```

## Who Made It

- **Company:** Square, Inc. (the payments company — they make the card reader)
- **Key engineers:** Bob Lee, Jake Wharton (Jake Wharton later became the most prominent contributor and maintainer)
- **Jake Wharton** is one of the most respected Android engineers in the world. He also created/contributed to: Timber, Picasso, Butterknife, Moshi, SQLDelight, and many others.
- **License:** Apache 2.0 (free, open-source, use in commercial apps)
- **Repository:** `github.com/square/retrofit`

---

---

# 2. What Retrofit Actually Is

## The One-Sentence Definition

Retrofit is a **type-safe HTTP client** that turns your API description (written as a Kotlin interface) into actual working HTTP calls.

## "Type-safe" — What Does That Mean?

Without type safety:

```
// You call a function, it returns... something. You don't know until runtime.
val result = httpClient.get("https://api.example.com/users")
// result is a raw String or bytes — you don't know if it's a User or an Error
```

With type safety (Retrofit):

```kotlin
// The compiler KNOWS this returns List<User>
// If the server sends something else — it fails at a known, predictable point
suspend fun getUsers(): List<User>
```

The compiler catches mistakes before your app ships. That's type safety.

## What Retrofit Is NOT

- Retrofit is **not** an HTTP client — it doesn't open sockets or send bytes
- Retrofit is **not** a JSON parser — it doesn't know what JSON is by default
- Retrofit is a **coordinator** — it uses OkHttp to send requests, and a Converter to parse responses

Think of it like this:

```
Retrofit  =  Manager      (takes orders, delegates work, returns results)
OkHttp    =  Postman      (actually carries the letters/packages)
Gson/Moshi =  Translator  (converts JSON bytes to Kotlin objects)
```

---

---

# 3. The Stack — Retrofit, OkHttp, Okio

This is the internal dependency chain. Understanding this is critical.

```
Your Code
    │
    ▼
Retrofit                  ← You interact with this
    │  uses
    ▼
OkHttp                    ← Handles all HTTP protocol details
    │  uses
    ▼
Okio                      ← Handles all byte reading/writing efficiently
    │  uses
    ▼
Java Sockets / TLS        ← The actual network layer in the OS
```

## What Each Layer Does

### Retrofit (the top layer)

- Reads your annotated interface (`@GET`, `@POST`, etc.)
- Uses Java Reflection to understand what you wrote at runtime
- Builds an HTTP Request object and hands it to OkHttp
- Takes OkHttp's raw response and hands it to the Converter
- Returns the converted Kotlin object to your ViewModel

### OkHttp (the middle layer)

- Actually opens TCP connections to the server
- Handles HTTPS/TLS encryption and certificates
- Manages a **connection pool** (reuses open connections instead of opening new ones every time — faster)
- Handles **HTTP/2** (multiple requests over one connection)
- Handles **GZIP decompression** automatically
- Follows **redirects** (301, 302) automatically
- **Caches responses** on disk (if configured)
- Runs **interceptors** (your custom code that can modify every request/response)
- Handles **timeouts** (connect, read, write)
- Handles **retries** on failure

**This is why you need OkHttp even though Retrofit "does networking."** Retrofit doesn't know how to open a socket. OkHttp does.

### Okio (the bottom layer)

- A replacement for Java's `java.io` and `java.nio`
- More efficient byte buffer management
- You almost never touch this directly
- It's just what OkHttp uses internally to read and write bytes efficiently

## Why This Separation Matters

Because you can **customize OkHttp without changing Retrofit**. If you need to:

- Add auth headers to every request → add an OkHttp Interceptor
- Log all network traffic → add OkHttp's LoggingInterceptor
- Cache responses for offline use → configure OkHttp's Cache
- Add custom SSL certificates → configure OkHttp's SSLSocketFactory

All of this happens at the OkHttp layer and Retrofit benefits automatically.

---

---

# 4. Version History — How It Evolved

## Retrofit 1.x (2013 – 2015)

**State of Android:** Java-only, no Kotlin, AsyncTask was the standard for background work.

**What it introduced:**

- The core idea: annotated interfaces → HTTP calls
- Used **Apache HttpClient** by default (which was the Android standard at the time)
- OkHttp was optional
- Callbacks for async: `Callback<T>` with `success()` and `failure()` methods
- Synchronous calls returned the object directly
- Built-in JSON via Gson only

**What was painful:**

- Callback hell (nested callbacks for chained requests)
- No coroutine support (Kotlin didn't exist yet)
- OkHttp 2.x was separate and optional — not deeply integrated
- Migrating to version 2 required rewriting everything

**Package name:** `com.squareup.retrofit` (note: no "2")

---

## Retrofit 2.x (2016 – 2024)

**State of Android:** Kotlin was growing, RxJava was popular, coroutines were emerging.

**Major changes from 1.x (breaking — required code rewrite):**

- **OkHttp became mandatory** — no longer optional. Retrofit 2 uses OkHttp as its only HTTP engine
- **`RestAdapter` → renamed to `Retrofit`** (the main builder class)
- **`Call<T>` return type** — every API function now returns `Call<T>` instead of the raw type, giving you `.execute()` and `.enqueue()`
- **Converter system** became pluggable — add Gson, Moshi, Jackson, Protobuf, etc.
- **Call Adapter system** added — add RxJava, coroutines, LiveData support
- **`Response<T>`** wrapper — lets you inspect HTTP status codes, headers on success
- **Null safety improved**
- Minimum: Java 7, Android API 15

**Kotlin coroutines support added in 2.6.0 (2019):**

```kotlin
// Before 2.6.0 — had to use enqueue() callback or RxJava
@GET("users")
fun getUsers(): Call<List<User>>

// After 2.6.0 — native suspend function support!
@GET("users")
suspend fun getUsers(): List<User>
```

This was huge. `suspend` functions work directly with Kotlin coroutines and `viewModelScope`.

**Package name:** `com.squareup.retrofit2` (added the "2")

**Notable sub-versions:**

- `2.0` — initial release, OkHttp mandatory
- `2.4` — improved Kotlin support
- `2.6` — `suspend` function support added (the most important Kotlin addition)
- `2.9` — last widely used stable before Retrofit 3 work began
- `2.11` — latest in the 2.x line, still actively supported

---

## Retrofit 3.x (May 2025 – present)

**State of Android:** Kotlin-first, coroutines standard, Compose is the UI framework.

**Key changes from 2.x:**

- **OkHttp upgraded from 3.x to 4.12** — OkHttp 4.x is rewritten entirely in Kotlin (the previous OkHttp 3.x was Java)
- Because OkHttp 4.x is Kotlin, **Retrofit 3 now has a transitive Kotlin dependency** — even if you don't write Kotlin, Kotlin stdlib is now in your app
- **Better native Kotlin support** — more idiomatic APIs, fewer Java-style workarounds
- **`suspend` functions no longer need `Call<T>`** — declaring `suspend fun` without wrapping in `Call<>` is now the standard and recommended
- **BOM (Bill of Materials) published** — `com.squareup.retrofit2:retrofit-bom` lets you manage all Retrofit module versions in one place
- **Backward compatible with 2.x** — libraries compiled against Retrofit 2 still work with Retrofit 3 without changes (binary compatibility)
- Minimum: **Java 8+, Android API 21+**

**What did NOT change:**

- All annotations (`@GET`, `@POST`, `@Body`, `@Path`, etc.) are identical
- Builder pattern is identical
- Converter system is identical
- Your existing code works without modification

**Feature comparison table:**

|Feature|Retrofit 1.x|Retrofit 2.x|Retrofit 3.x|
|---|---|---|---|
|HTTP Engine|Apache HttpClient / OkHttp (optional)|OkHttp 3.x (mandatory)|OkHttp 4.12 (Kotlin)|
|Kotlin Support|None (Java only)|Via adapters|Native, first-class|
|Coroutines|No|Via `suspend` (2.6+)|Native `suspend`|
|Return Type|Raw object or Callback|`Call<T>`|`T` directly (suspend)|
|Backward Compat|Breaks with 2.x|Breaks with 1.x|Compatible with 2.x|
|Min Android API|API 9|API 15 → API 21|API 21|
|JSON default|Gson built-in|Pluggable|Pluggable|

---

---

# 5. How HTTP Works (The Foundation)

Understanding this makes Retrofit annotations make sense.

## What HTTP Is

HTTP (HyperText Transfer Protocol) is a text-based protocol for communication between a client (your app) and a server. Every network call is:

1. **Your app sends a Request** to a server
2. **The server sends back a Response**

## Anatomy of an HTTP Request

```
POST /api/users HTTP/1.1
Host: api.example.com
Content-Type: application/json
Authorization: Bearer eyJhbGci...
Accept: application/json

{
  "name": "Rahul",
  "email": "rahul@example.com"
}
```

Parts:

- **Method** (`POST`) — what action to take
- **Path** (`/api/users`) — which resource
- **HTTP Version** (`HTTP/1.1`)
- **Headers** — metadata (content type, auth token, etc.)
- **Body** — data you're sending (only for POST, PUT, PATCH)

## Anatomy of an HTTP Response

```
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 127

{
  "id": 42,
  "name": "Rahul",
  "email": "rahul@example.com"
}
```

Parts:

- **Status Code** (`200`) — did it work?
- **Headers** — metadata about the response
- **Body** — the actual data

## HTTP Methods and When to Use Each

|Method|Use for|Has Body?|Idempotent?|
|---|---|---|---|
|`GET`|Reading data|No|Yes|
|`POST`|Creating new data|Yes|No|
|`PUT`|Replacing an existing resource entirely|Yes|Yes|
|`PATCH`|Updating part of an existing resource|Yes|No|
|`DELETE`|Deleting a resource|No|Yes|

**Idempotent** means: calling it multiple times has the same result as calling it once. GET and DELETE are idempotent. POST is not (posting twice creates two records).

## HTTP Status Codes

|Code Range|Meaning|Common Codes|
|---|---|---|
|2xx|Success|200 OK, 201 Created, 204 No Content|
|3xx|Redirect|301 Moved Permanently, 304 Not Modified|
|4xx|Client error (your fault)|400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 422 Unprocessable Entity|
|5xx|Server error (their fault)|500 Internal Server Error, 503 Service Unavailable|

These matter because `response.isSuccessful` in Retrofit checks if the code is 200–299.

## Query Parameters vs Path Parameters vs Body

```
// Path parameter — part of the URL itself
GET https://api.example.com/users/42
                                    ^^
                              this is {userId}

// Query parameter — after the ? in the URL
GET https://api.example.com/users?page=2&limit=20
                                  ^^^^^^^^^^^^^^^^
                              these are query params

// Body — not in the URL, sent separately (only POST/PUT/PATCH)
POST https://api.example.com/users
Body: { "name": "Rahul" }
```

Retrofit has separate annotations for each of these (`@Path`, `@Query`, `@Body`).

---

---

# 6. Retrofit Builder — Every Property Explained

The `Retrofit.Builder` is how you configure Retrofit before using it. You build it once (usually as a Singleton via Hilt) and use it everywhere.

```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("https://api.example.com/")
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .addCallAdapterFactory(/* optional */)
    .build()
```

## `.baseUrl(String)` — REQUIRED

The root URL that all API endpoints are relative to.

```kotlin
.baseUrl("https://api.example.com/")
//                                 ^ trailing slash is MANDATORY
```

**Rules:**

- Must end with `/` — Retrofit will throw an `IllegalArgumentException` if it doesn't
- Must be a full URL including scheme (`https://`)
- Relative paths in your `@GET("users")` are appended to this

**How it combines:**

|baseUrl|@GET path|Final URL|
|---|---|---|
|`https://api.example.com/`|`users`|`https://api.example.com/users`|
|`https://api.example.com/v2/`|`users`|`https://api.example.com/v2/users`|
|`https://api.example.com/`|`users/42`|`https://api.example.com/users/42`|

**Important edge case:** If your `@GET` path starts with `/`, it replaces everything after the host:

```kotlin
.baseUrl("https://api.example.com/v2/")
@GET("/users")  // Final URL = https://api.example.com/users (v2 is GONE!)
@GET("users")   // Final URL = https://api.example.com/v2/users (correct)
```

Always use relative paths (without leading `/`) unless you intentionally want to override the base.

## `.client(OkHttpClient)` — strongly recommended

Provides a custom OkHttpClient. If you skip this, Retrofit creates a default OkHttpClient internally — but you lose the ability to add interceptors, timeouts, logging, etc.

```kotlin
.client(okHttpClient)
```

Always provide your own so you can add:

- `HttpLoggingInterceptor` (see network calls in Logcat)
- `AuthInterceptor` (add auth token to every request)
- Timeouts
- Caching

## `.addConverterFactory(Converter.Factory)` — required for JSON

Tells Retrofit how to convert JSON bytes ↔ Kotlin objects. Without this, Retrofit can only return `ResponseBody` (raw bytes).

```kotlin
.addConverterFactory(GsonConverterFactory.create())
// or
.addConverterFactory(MoshiConverterFactory.create())
```

You can add **multiple** converter factories. Retrofit tries them in order.

## `.addCallAdapterFactory(CallAdapter.Factory)` — optional

Changes what type API functions can return. By default they return `Call<T>`. With adapters you can return `Flow<T>`, RxJava types, etc.

```kotlin
.addCallAdapterFactory(RxJava3CallAdapterFactory.create())
```

> In Retrofit 3 with `suspend` functions, you usually don't need this — `suspend fun getUsers(): List<User>` just works.

## `.build()`

Validates your configuration and creates the `Retrofit` instance. Throws if configuration is invalid.

## `retrofit.create(ApiService::class.java)`

The magic step. Takes your interface and generates a working implementation using Java Reflection (it reads your annotations at runtime and builds HTTP requests from them).

```kotlin
val apiService: ApiService = retrofit.create(ApiService::class.java)
// apiService is now a real working object, even though you only wrote an interface
```

---

---

# 7. OkHttpClient — Every Property Explained

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .addInterceptor(loggingInterceptor)
    .addInterceptor(authInterceptor)
    .addNetworkInterceptor(networkInterceptor)
    .cache(cache)
    .retryOnConnectionFailure(true)
    .build()
```

## Timeouts

Three separate timeouts — all different stages of a request:

|Timeout|What it measures|When it triggers|
|---|---|---|
|`connectTimeout`|Time to establish TCP connection to server|Server is unreachable, network is slow|
|`readTimeout`|Time between receiving each chunk of data|Server started responding but is very slow|
|`writeTimeout`|Time to send your request body to the server|Your upload is very slow|

Default for all: **10 seconds**. For production apps, 30 seconds is common.

```kotlin
.connectTimeout(30, TimeUnit.SECONDS)
.readTimeout(30, TimeUnit.SECONDS)
.writeTimeout(30, TimeUnit.SECONDS)
```

## `.addInterceptor()` — Application Interceptor

Runs **before** OkHttp processes the request. Sees every request/response exactly once.

Use for:

- Adding auth headers (`Authorization: Bearer ...`)
- Logging request/response
- Modifying requests before they go out

## `.addNetworkInterceptor()` — Network Interceptor

Runs **after** OkHttp processes redirects/retries. Sees the actual network traffic including redirects.

Use for:

- Monitoring exact bytes sent/received
- Modifying cache headers on responses

## `.cache(Cache)` — Offline Caching

Saves HTTP responses to disk. On future requests, can serve cached responses without hitting the network.

```kotlin
val cacheSize = 10L * 1024 * 1024  // 10 MB
val cache = Cache(context.cacheDir, cacheSize)
.cache(cache)
```

The server controls caching behavior via `Cache-Control` headers. You can also force cache behavior with interceptors.

## `.retryOnConnectionFailure(Boolean)`

Default: `true`. If the connection fails on the first attempt, OkHttp tries once more automatically. Set to `false` if you want to handle retries manually.

## Connection Pool (automatic — no config needed)

OkHttp automatically maintains a pool of open HTTP connections. Instead of opening and closing a TCP connection for every request (slow), it keeps connections alive and reuses them.

Default: 5 connections, kept alive for 5 minutes. This is why multiple rapid requests are fast — they reuse the same connection.

## HTTP/2 Support (automatic)

If the server supports HTTP/2, OkHttp uses it automatically. HTTP/2 allows multiple requests to be sent over a single connection simultaneously (multiplexing), which is significantly faster than HTTP/1.1.

---

---

# 8. API Interface Annotations — Complete Reference

This is where you spend most of your time. The interface is the "menu" of everything your API can do.

## HTTP Method Annotations

These go on the function itself and define what HTTP method to use.

### `@GET`

```kotlin
@GET("users")
suspend fun getUsers(): List<User>

@GET("users/{id}")
suspend fun getUserById(@Path("id") userId: Int): User
```

For reading data. Never has a body.

### `@POST`

```kotlin
@POST("users")
suspend fun createUser(@Body user: CreateUserRequest): User
```

For creating new data. Usually has a `@Body`.

### `@PUT`

```kotlin
@PUT("users/{id}")
suspend fun updateUser(@Path("id") userId: Int, @Body user: UpdateUserRequest): User
```

Replaces the entire resource. Requires `@Body` with complete object.

### `@PATCH`

```kotlin
@PATCH("users/{id}")
suspend fun patchUser(@Path("id") userId: Int, @Body updates: Map<String, Any>): User
```

Updates part of a resource. `@Body` contains only the fields being changed.

### `@DELETE`

```kotlin
@DELETE("users/{id}")
suspend fun deleteUser(@Path("id") userId: Int)
// No body, no return type needed (Unit)
```

### `@HEAD`

```kotlin
@HEAD("users/{id}")
suspend fun checkUserExists(@Path("id") userId: Int): Response<Void>
```

Same as GET but returns only headers, no body. Used to check if a resource exists without downloading it.

### `@HTTP` — Custom method

```kotlin
@HTTP(method = "PURGE", path = "users/{id}", hasBody = false)
suspend fun purgeUser(@Path("id") userId: Int)
```

For non-standard HTTP methods (some APIs use custom ones).

---

## URL Manipulation Annotations

### `@Path` — Replace part of the URL

```kotlin
@GET("users/{id}/posts/{postId}")
suspend fun getPost(
    @Path("id") userId: Int,        // replaces {id}
    @Path("postId") postId: Int     // replaces {postId}
): Post
```

The name in `@Path("name")` must exactly match the `{name}` in the URL string.

### `@Query` — Add `?key=value` to the URL

```kotlin
@GET("users")
suspend fun getUsers(
    @Query("page") page: Int,
    @Query("limit") limit: Int,
    @Query("sort") sort: String?    // nullable = omitted if null
): List<User>
// Result: /users?page=1&limit=20&sort=name
```

If the value is `null`, Retrofit omits that query parameter entirely.

### `@QueryMap` — Multiple query params from a Map

```kotlin
@GET("search")
suspend fun search(@QueryMap filters: Map<String, String>): SearchResult
// filters = mapOf("q" to "android", "lang" to "kotlin")
// Result: /search?q=android&lang=kotlin
```

Useful when query params are dynamic.

### `@Url` — Dynamic full URL (overrides baseUrl)

```kotlin
@GET
suspend fun getFromDynamicUrl(@Url url: String): ResponseBody
```

Used when the URL comes from an API response (e.g., pagination "next page" URLs, pre-signed S3 upload URLs).

---

## Request Body Annotations

### `@Body` — Send a Kotlin object as JSON

```kotlin
@POST("users")
suspend fun createUser(@Body request: CreateUserRequest): User
```

The converter (Gson/Moshi) converts `CreateUserRequest` to JSON bytes automatically.

### `@Field` and `@FormUrlEncoded` — Send form data

```kotlin
@FormUrlEncoded
@POST("login")
suspend fun login(
    @Field("email") email: String,
    @Field("password") password: String
): LoginResponse
```

Sends data as `email=test%40test.com&password=1234` (URL-encoded form). Used for old-style form submissions, some OAuth flows.

### `@Part` and `@Multipart` — File uploads

```kotlin
@Multipart
@POST("users/{id}/avatar")
suspend fun uploadAvatar(
    @Path("id") userId: Int,
    @Part avatar: MultipartBody.Part,           // the file
    @Part("description") description: RequestBody  // text alongside the file
): User
```

`@Multipart` is required for file uploads. `MultipartBody.Part` wraps the file bytes.

---

## Header Annotations

### `@Headers` — Static headers (same for every call)

```kotlin
@Headers(
    "Accept: application/json",
    "Cache-Control: no-cache"
)
@GET("users")
suspend fun getUsers(): List<User>
```

### `@Header` — Dynamic header (different per call)

```kotlin
@GET("users")
suspend fun getUsers(@Header("Authorization") token: String): List<User>
// Called as: apiService.getUsers("Bearer eyJhbGci...")
```

In practice, you almost never use `@Header` for auth — you use an `Interceptor` instead (so you don't have to pass the token to every function call manually).

### `@HeaderMap` — Multiple dynamic headers

```kotlin
@GET("users")
suspend fun getUsers(@HeaderMap headers: Map<String, String>): List<User>
```

---

## Return Type Options

```kotlin
// Option 1: Direct object (Retrofit 3 + suspend)
// Throws exception on network error or non-2xx response
suspend fun getUser(): User

// Option 2: Response<T> wrapper
// Gives you access to HTTP status code and headers even on error
// Does NOT throw on 4xx/5xx — you check response.isSuccessful manually
suspend fun getUser(): Response<User>

// Option 3: Call<T> (Retrofit 2 style, still works)
// Gives manual control — call .execute() or .enqueue()
fun getUser(): Call<User>

// Option 4: ResponseBody (raw bytes, no converter)
// Use when you don't know the response shape or handling it yourself
suspend fun getRawData(): ResponseBody
```

---

---

# 9. Converter Factories — JSON Parsing

By default, Retrofit only knows about `ResponseBody` (raw bytes). A Converter Factory teaches it how to convert those bytes into your Kotlin data classes.

## Available Converters

|Library|Gradle Dependency|Notes|
|---|---|---|
|**Gson**|`converter-gson`|Most common, made by Google, Java-first|
|**Moshi**|`converter-moshi`|Made by Square (same as Retrofit), Kotlin-first, more strict|
|**Jackson**|`converter-jackson`|Most feature-rich, heavy, common in enterprise Java|
|**Kotlinx Serialization**|`converter-kotlinx-serialization`|Kotlin official, compile-time safe, no reflection|
|**Scalars**|`converter-scalars`|For returning plain `String`, `Boolean`, `Int`|
|**Protobuf**|`converter-protobuf`|Binary format, faster than JSON|

## Gson vs Moshi — Which to Choose

||Gson|Moshi|
|---|---|---|
|Made by|Google|Square (same as Retrofit)|
|Kotlin-first|No|Yes|
|Null safety|Lenient (dangerous)|Strict (safer)|
|Kotlin data classes|Works but can bypass null checks|Works properly with null checks|
|Reflection|Yes|Yes (or codegen with KSP)|
|Performance|Similar|Similar|
|Recommendation|Fine for learning/simple apps|Preferred for production Kotlin apps|

## How Conversion Works Internally

When Retrofit receives a response:

```
Server response bytes
        │
        ▼
OkHttp delivers ResponseBody (raw bytes + headers)
        │
        ▼
Retrofit asks each ConverterFactory: "Can you convert this to User.class?"
        │
        ▼
GsonConverterFactory says "Yes" → Gson reads the JSON bytes → creates User object
        │
        ▼
Your suspend function returns the User object
```

When you send a request with `@Body`:

```
Your User object
        │
        ▼
Retrofit asks ConverterFactory: "Convert this User to RequestBody"
        │
        ▼
Gson converts User to JSON bytes → wrapped in RequestBody
        │
        ▼
OkHttp sends those bytes to the server
```

## Multiple Converters

You can add multiple converter factories. Retrofit tries them in order and uses the first one that says "I can handle this type."

```kotlin
.addConverterFactory(MoshiConverterFactory.create())    // tried first
.addConverterFactory(GsonConverterFactory.create())     // fallback
```

---

---

# 10. Interceptors — Deep Dive

Interceptors are the most powerful feature of OkHttp, and by extension, of the Retrofit stack. They let you intercept every request and response and modify them.

## The Interceptor Concept

Think of interceptors like security checkpoints at an airport:

```
Your Request
     │
     ▼
[Application Interceptor 1]  ← add auth header
     │
     ▼
[Application Interceptor 2]  ← log the request
     │
     ▼
[OkHttp Core]               ← actually send the request
     │
     ▼
[Network Interceptor]        ← see raw response including redirects
     │
     ▼
[OkHttp Core]               ← process response
     │
     ▼
[Application Interceptors] (in reverse)  ← see final response
     │
     ▼
Your Response
```

## Application Interceptor vs Network Interceptor

||Application Interceptor (`addInterceptor`)|Network Interceptor (`addNetworkInterceptor`)|
|---|---|---|
|When it runs|Before OkHttp processes anything|After OkHttp sends/receives from network|
|Sees redirects|No — only the final response|Yes — sees each redirect response|
|Runs for cached responses|Yes|No|
|Can short-circuit|Yes (return cached response without network call)|No|
|Use for|Auth, logging, retry logic|Monitoring actual network bytes, cache control|

**Rule of thumb:** Use `addInterceptor` for 99% of cases. Only use `addNetworkInterceptor` if you specifically need to see redirect chains or modify cache headers.

## Common Interceptors

### 1. HttpLoggingInterceptor (from OkHttp library)

Logs all request and response details to Logcat. Essential for debugging.

```
Dependency: com.squareup.okhttp3:logging-interceptor
```

```
Logging Levels:
NONE    → no logging
BASIC   → request line + response code
HEADERS → request + response headers
BODY    → full request + response including body (use only in DEBUG builds)
```

**Important:** Always use `NONE` in release builds. The body level logs all JSON which could contain sensitive user data and also hurts performance.

### 2. Auth Interceptor (you write this)

Adds your auth token to every single request automatically — so you never have to pass it manually to each API function.

```
What it does:
1. Intercepts the outgoing request
2. Reads the original request
3. Adds "Authorization: Bearer {token}" header
4. Passes the modified request to the next interceptor (or to OkHttp)
5. Returns the response
```

### 3. Token Refresh Interceptor (Authenticator)

A special type of interceptor that handles 401 Unauthorized responses:

```
Flow:
1. Request goes out
2. Server returns 401 (token expired)
3. Authenticator intercepts the 401
4. Makes a synchronous call to refresh the token
5. Saves the new token
6. Retries the original request with the new token
7. Returns the new response (as if the 401 never happened)
```

OkHttp has a dedicated `Authenticator` interface for this pattern — separate from regular `Interceptor`.

### 4. Cache Interceptor (you write this)

Forces cached responses when offline:

```
1. Check if device has network
2. If no network → force cache: add "Cache-Control: only-if-cached" header
3. If has network → allow fresh responses with cache fallback
```

## Interceptor Order Matters

Interceptors run in the order they are added, and responses flow back in reverse order:

```kotlin
OkHttpClient.Builder()
    .addInterceptor(authInterceptor)      // runs 1st on request, last on response
    .addInterceptor(loggingInterceptor)   // runs 2nd on request, 1st on response
```

**Practical rule:** Add `loggingInterceptor` LAST so it logs the final modified request (including auth headers added by previous interceptors).

---

---

# 11. Call Adapters — Return Types

Call Adapters change what type your API interface functions can return.

## Default (no adapter): `Call<T>`

```kotlin
fun getUsers(): Call<List<User>>
// Call manually with:
call.enqueue(object : Callback<List<User>> {
    override fun onResponse(...) { }
    override fun onFailure(...) { }
})
```

## With Kotlin Coroutines (Retrofit 2.6+ and 3.x)

No adapter needed. Just add `suspend`:

```kotlin
suspend fun getUsers(): List<User>
// Call with:
viewModelScope.launch {
    val users = apiService.getUsers()
}
```

Retrofit automatically handles the coroutine adapter for `suspend` functions.

## With RxJava 3

```
Dependency: com.squareup.retrofit2:adapter-rxjava3
Builder:    .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
```

```kotlin
fun getUsers(): Observable<List<User>>
fun getUser(id: Int): Single<User>
fun deleteUser(id: Int): Completable
```

## With `Response<T>` wrapper

Available without any adapter. Gives you access to HTTP status code even on success:

```kotlin
suspend fun getUser(): Response<User>
// Access:
response.code()          // 200, 404, 500...
response.isSuccessful    // true if 200-299
response.body()          // the User object (null if not 2xx)
response.errorBody()     // the error response body (null if 2xx)
response.headers()       // response headers
```

---

---

# 12. Response Handling

## The Three Outcomes of a Retrofit Call

```
1. Success          → 2xx status code, body is your Kotlin object
2. HTTP Error       → 4xx or 5xx status code, body is an error message from server
3. Network Failure  → no response at all (no internet, timeout, DNS failure)
```

Retrofit handles these differently depending on your return type.

## With `suspend fun T` (direct object, Retrofit 3 style)

```kotlin
// Success → returns User
// HTTP Error (4xx/5xx) → throws HttpException
// Network Failure → throws IOException
suspend fun getUser(id: Int): User
```

You must wrap in try/catch:

```kotlin
try {
    val user = apiService.getUser(1)        // success path
} catch (e: HttpException) {
    val code = e.code()                     // e.g. 404
    val errorBody = e.response()?.errorBody()?.string()
} catch (e: IOException) {
    // No internet, timeout, etc.
}
```

## With `suspend fun Response<T>`

```kotlin
// Success → returns Response<User> with isSuccessful = true
// HTTP Error → returns Response<User> with isSuccessful = false (does NOT throw!)
// Network Failure → throws IOException
suspend fun getUser(id: Int): Response<User>
```

```kotlin
try {
    val response = apiService.getUser(1)
    if (response.isSuccessful) {
        val user = response.body()!!       // safe to use !!
    } else {
        val code = response.code()         // 404, 500, etc.
        val error = response.errorBody()?.string()
    }
} catch (e: IOException) {
    // No internet
}
```

## Wrapping in Result (recommended pattern)

Wrap your repository functions in Kotlin's `Result<T>` for clean error propagation to ViewModel:

```kotlin
// In repository:
suspend fun getUser(id: Int): Result<User> {
    return try {
        val user = apiService.getUser(id)
        Result.success(user)
    } catch (e: HttpException) {
        Result.failure(Exception("Server error: ${e.code()}"))
    } catch (e: IOException) {
        Result.failure(Exception("No internet connection"))
    }
}

// In ViewModel:
getUserUseCase(1)
    .onSuccess { user -> _uiState.update { it.copy(user = user) } }
    .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
```

---

---

# 13. Error Handling

## HttpException

Thrown when the server responds with a 4xx or 5xx status code.

```kotlin
catch (e: HttpException) {
    e.code()              // HTTP status code: 401, 404, 500...
    e.message()           // HTTP status message: "Not Found", "Unauthorized"
    e.response()          // the full Response object
    e.response()?.errorBody()?.string()  // the error body from server as String
}
```

## Parsing Error Body

Servers usually send error details in the body:

```json
{ "error": "User not found", "code": "USER_404" }
```

```kotlin
catch (e: HttpException) {
    val errorJson = e.response()?.errorBody()?.string()
    // Manually parse with Gson:
    val errorResponse = Gson().fromJson(errorJson, ErrorResponse::class.java)
}
```

## IOException

All network-level failures: no internet, DNS failure, connection timeout, SSL error.

```kotlin
catch (e: IOException) {
    // No internet → show offline message
    // SocketTimeoutException extends IOException → handle timeout specifically
    when (e) {
        is SocketTimeoutException -> "Request timed out"
        is UnknownHostException   -> "No internet connection"
        else                      -> "Network error: ${e.message}"
    }
}
```

## Common Status Codes and How to Handle Them

|Code|Meaning|How to Handle in App|
|---|---|---|
|200|OK|Normal success path|
|201|Created|New resource created, show confirmation|
|204|No Content|Success but no body — don't try to parse body|
|400|Bad Request|Your request was malformed — show validation error|
|401|Unauthorized|Token expired or missing — redirect to login|
|403|Forbidden|User doesn't have permission — show access denied|
|404|Not Found|Resource doesn't exist — show empty state|
|422|Unprocessable Entity|Validation error — show field-level errors|
|429|Too Many Requests|Rate limited — show "try again later"|
|500|Internal Server Error|Server crashed — show generic error|
|503|Service Unavailable|Server is down — show "service unavailable"|

---

---

# 14. How a Request Flows — Step by Step

This is what happens internally when you call `apiService.getUsers()`:

```
Step 1: You call apiService.getUsers()
        │
        │  (apiService is a Proxy object generated by Retrofit using Java Reflection)
        ▼
Step 2: Retrofit's Proxy intercepts the call
        - Reads the @GET("users") annotation
        - Reads all @Query, @Path, @Header parameter annotations
        - Builds a ServiceMethod object (cached after first call)
        │
        ▼
Step 3: Retrofit builds an OkHttp Request
        - Combines baseUrl + path = "https://api.example.com/users"
        - Adds method: GET
        - Adds headers (from @Headers or Interceptors)
        - Adds query params (from @Query)
        │
        ▼
Step 4: OkHttp receives the Request
        - Runs through Application Interceptors (your auth, logging)
        - Checks the cache — is there a fresh cached response?
        │
        ▼
Step 5: (if not cached) OkHttp sends the Request over the network
        - Gets a connection from the connection pool (or opens a new one)
        - Runs through Network Interceptors
        - Sends bytes over TCP socket
        │
        ▼
Step 6: Server processes and responds
        - HTTP status code + headers + body bytes come back
        │
        ▼
Step 7: OkHttp receives the raw Response
        - Follows redirects if needed (3xx)
        - Stores in cache if server says to
        - Runs response through Network Interceptors (reverse)
        - Runs response through Application Interceptors (reverse)
        │
        ▼
Step 8: Retrofit receives OkHttp's Response
        - Checks if it's a 2xx code
        - Passes the response body bytes to the Converter Factory
        │
        ▼
Step 9: Converter (Gson/Moshi) parses the bytes
        - JSON bytes → Kotlin object (e.g., List<User>)
        │
        ▼
Step 10: Retrofit returns the Kotlin object to your suspend function
        - Your coroutine resumes on the main thread
        - uiState is updated → Compose recomposes → UI updates
```

The whole journey from your call to the UI update happens in milliseconds.

---

---

# 15. Retrofit + Hilt — Production Setup

This is the pattern used in real Android projects.

## File: `di/NetworkModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // 1. Logging interceptor — logs all network traffic in debug builds only
    @Provides
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }

    // 2. Auth interceptor — adds token to every request
    @Provides
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor =
        AuthInterceptor(tokenManager)

    // 3. OkHttpClient — assembles all interceptors + timeouts
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)     // auth first
            .addInterceptor(loggingInterceptor)  // logging last (logs final request)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    // 4. Retrofit — uses OkHttpClient + Gson converter
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)       // never hardcode URLs
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    // 5. API service — the actual interface implementation
    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi =
        retrofit.create(UserApi::class.java)
}
```

## File: `data/local/preferences/TokenManager.kt`

```kotlin
class TokenManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val TOKEN_KEY = stringPreferencesKey("auth_token")
    }

    suspend fun saveToken(token: String) {
        dataStore.edit { it[TOKEN_KEY] = token }
    }

    fun getToken(): String? = runBlocking {
        dataStore.data.map { it[TOKEN_KEY] }.first()
    }
    // Note: runBlocking is acceptable here because
    // interceptors run on a background thread, not main thread
}
```

## File: `data/remote/interceptor/AuthInterceptor.kt`

```kotlin
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.getToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
```

## `build.gradle` — All dependencies

```kotlin
// Retrofit
implementation("com.squareup.retrofit2:retrofit:3.0.0")
implementation("com.squareup.retrofit2:converter-gson:3.0.0")
// or Moshi:
// implementation("com.squareup.retrofit2:converter-moshi:3.0.0")

// OkHttp + Logging
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
```

## `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<!-- Required — without this, all network calls fail silently -->
```

---

---

# 16. Common Interview Questions

**Q: What is Retrofit? How is it different from OkHttp?**

Retrofit is a type-safe HTTP client that converts annotated Kotlin interfaces into HTTP calls. OkHttp is the actual HTTP engine underneath — it opens connections, manages caches, and sends/receives bytes. Retrofit is the abstraction layer built on top of OkHttp. You use Retrofit for defining APIs, and OkHttp handles the actual network work.

---

**Q: Why do we need OkHttp if we have Retrofit?**

Retrofit cannot make network calls on its own. It needs an HTTP client to actually open sockets and communicate with servers. OkHttp is that client. Additionally, OkHttp provides features like connection pooling, interceptors, caching, and HTTP/2 support that Retrofit leverages automatically.

---

**Q: What is a Converter Factory? Name some.**

A Converter Factory teaches Retrofit how to convert JSON (or other formats) to Kotlin objects and back. Without one, Retrofit can only work with raw `ResponseBody`. Common ones: `GsonConverterFactory`, `MoshiConverterFactory`, `JacksonConverterFactory`, `KotlinxSerializationConverterFactory`.

---

**Q: What is an Interceptor? What are the two types?**

An interceptor is a piece of code that intercepts every HTTP request and response, allowing you to inspect or modify them. Two types: Application Interceptors (run before OkHttp processes redirects/retries, added via `addInterceptor`) and Network Interceptors (run at the network level, see raw traffic, added via `addNetworkInterceptor`).

---

**Q: What happens if you don't call `.addConverterFactory()`?**

Retrofit can only deserialize responses into `ResponseBody` and only accept `RequestBody` for request bodies. You lose the ability to use your Kotlin data classes directly.

---

**Q: Why must baseUrl end with a slash?**

Retrofit uses URL resolution rules where relative paths are resolved against the base. If the base doesn't end in `/`, the last path segment gets replaced rather than appended. Retrofit enforces the trailing slash with an exception to prevent this common mistake.

---

**Q: What is the difference between `@Query` and `@Path`?**

`@Path` replaces a placeholder inside the URL path itself (`/users/{id}` → `/users/42`). `@Query` adds key-value pairs after the `?` in the URL (`/users?page=1`).

---

**Q: What is `Response<T>` vs just `T` as return type?**

`Response<T>` wraps the response and gives you access to the HTTP status code, headers, and error body even on non-2xx responses (without throwing an exception). Returning `T` directly is simpler but throws `HttpException` on non-2xx codes — you must use try/catch.

---

**Q: How does Retrofit support coroutines?**

From Retrofit 2.6.0+, you can mark interface functions with `suspend`. Retrofit detects this and automatically wraps the call in a coroutine-compatible way, so you can call them directly inside `viewModelScope.launch { }` without callbacks.

---

**Q: What changed in Retrofit 3?**

OkHttp was upgraded from 3.x to 4.12 (now written in Kotlin), better native Kotlin support, `suspend fun` without `Call<T>` wrapper is now the standard, minimum requirement raised to Java 8+/API 21+, backward compatible with Retrofit 2.x.

---

---

# 17. Quick Reference Cheat Sheet

## Annotations at a Glance

```
HTTP METHODS          URL PARAMS             REQUEST BODY
@GET                  @Path("id")            @Body
@POST                 @Query("page")         @Field (needs @FormUrlEncoded)
@PUT                  @QueryMap              @Part (needs @Multipart)
@PATCH                @Url (full dynamic)    
@DELETE               
@HEAD                 HEADERS                RETURN TYPES
@HTTP (custom)        @Header("Auth")        T (direct, suspend)
                      @Headers("K: V")       Response<T>
                      @HeaderMap             Call<T>
```

## Builder Checklist

```
Retrofit.Builder()
  ✓ .baseUrl()                 ← ends with /
  ✓ .client(okHttpClient)      ← custom OkHttp
  ✓ .addConverterFactory()     ← Gson or Moshi
  ✓ .build()
  ✓ .create(ApiService::class.java)

OkHttpClient.Builder()
  ✓ .connectTimeout()
  ✓ .readTimeout()
  ✓ .writeTimeout()
  ✓ .addInterceptor(authInterceptor)      ← first
  ✓ .addInterceptor(loggingInterceptor)   ← last
  ✓ .build()
```

## The 3-Level Stack

```
Your Code → Retrofit → OkHttp → Okio → Network
           (maps)    (sends)  (bytes) (OS)
```

## When Things Go Wrong

```
HttpException  → server responded, but 4xx or 5xx code
IOException    → no response at all (no internet, timeout, DNS)
JsonSyntaxException → response came back but JSON doesn't match your data class
IllegalArgumentException → your Retrofit/OkHttp configuration is wrong (check baseUrl, annotations)
```

---

_Sources: square.github.io/retrofit, github.com/square/retrofit, square.github.io/okhttp_