# Coroutine 03 Sequential Parallel Async Patterns

This note covers sequential composition, parallel composition, `async`, `await`, `awaitAll`, and real Android usage.

---

## Sequential Composition

### Meaning

- One suspend call finishes before the next starts.
- This is the default behavior.
- Use when the second call depends on the first result.

```kotlin
suspend fun loadUserPosts(): List<Post> {
    val user = api.getUser()
    return api.getPosts(user.id)
}
```

### Real-life example

- Login first.
- Receive auth token.
- Use token to fetch profile.

```kotlin
suspend fun loginAndLoadProfile(email: String, password: String): Profile {
    val token = authApi.login(email, password)
    return profileApi.getProfile(token.value)
}
```

### Interview explanation

- "Suspend calls are sequential by default. I use sequential composition when one result is required for the next call."

---

## Parallel Composition

### Meaning

- Independent suspend calls start together.
- Use `async` inside a structured scope.
- Use `await()` to get results.
- Faster when calls do not depend on each other.

```kotlin
suspend fun loadHome(): HomeData = coroutineScope {
    val user = async { api.getUser() }
    val books = async { api.getBooks() }
    val notifications = async { api.getNotifications() }

    HomeData(
        user = user.await(),
        books = books.await(),
        notifications = notifications.await()
    )
}
```

### Android ViewModel example

```kotlin
fun loadHome() {
    viewModelScope.launch {
        _uiState.value = HomeUiState(loading = true)

        try {
            val data = repository.loadHome()
            _uiState.value = HomeUiState(data = data)
        } catch (e: Exception) {
            _uiState.value = HomeUiState(error = e.message)
        }
    }
}
```

### Interview explanation

- "I use `async` for parallel independent tasks, and I keep it inside `coroutineScope` or `supervisorScope` so the child coroutines are structured."

---

## `awaitAll`

### Meaning

- Waits for multiple `Deferred` values.
- Returns a list of results.
- Throws if any deferred fails.

```kotlin
suspend fun loadAll(): List<Any> = coroutineScope {
    val users = async { api.getUsers() }
    val books = async { api.getBooks() }

    awaitAll(users, books)
}
```

### Caution

- Result type may become broad like `List<Any>` if deferred types differ.
- Explicit `await()` can be clearer for typed results.
- With `supervisorScope`, `awaitAll()` still throws if one child fails.

---

## Partial Success Pattern

Use `supervisorScope` plus `runCatching` when each section is independent.

```kotlin
suspend fun loadDashboard(): DashboardUiState = supervisorScope {
    val users = async { runCatching { api.getUsers() } }
    val books = async { runCatching { api.getBooks() } }
    val offers = async { runCatching { api.getOffers() } }

    val usersResult = users.await()
    val booksResult = books.await()
    val offersResult = offers.await()

    DashboardUiState(
        users = usersResult.getOrDefault(emptyList()),
        books = booksResult.getOrDefault(emptyList()),
        offers = offersResult.getOrDefault(emptyList()),
        error = if (
            usersResult.isFailure ||
            booksResult.isFailure ||
            offersResult.isFailure
        ) {
            "Some sections failed to load"
        } else {
            null
        }
    )
}
```

### Real-life example

- Home screen can show profile even if recommendations fail.
- E-commerce app can show products even if offers fail.
- Dashboard can show users even if analytics fail.

---

## Lazy Async

### Meaning

- `CoroutineStart.LAZY` means async does not start immediately.
- It starts on `start()` or `await()`.
- Use rarely.

```kotlin
val deferred = async(start = CoroutineStart.LAZY) {
    api.getUsers()
}

deferred.start()
val users = deferred.await()
```

### Interview explanation

- "Lazy async delays coroutine start until `start()` or `await()` is called. Most Android code does not need it, but it is useful when I want explicit control over when a deferred task begins."

---

## Choosing Sequential or Parallel

| Situation | Use |
|---|---|
| Second call needs first result | Sequential |
| Calls are independent | Parallel with `async` |
| All calls must succeed | `coroutineScope` |
| Partial success is useful | `supervisorScope` |
| Only switching thread | `withContext` |


---

## 🚀 Mastery Deep Dive (Added 2026)

> [!NOTE]
> The following deep dive notes were generated to provide mastery-level understanding, complementing the original notes above.

# Coroutine 03 — Sequential vs Parallel & Async Patterns

> [!NOTE]
> This file covers the most practical patterns you'll use when building real apps. The async trap explained here is the single most asked coroutine question in interviews and also the most common production bug.

---

## 🧠 Mental Model — Read This First

**Think of ordering food for a group.**

- **Sequential**: You call Restaurant A, wait on hold until food is ready, THEN call Restaurant B. Total time = A + B.
- **Parallel with `async`**: You call both restaurants AT THE SAME TIME (two phone calls simultaneously). You wait for whoever finishes last. Total time ≈ max(A, B).
- **The async trap**: You call Restaurant A, but you press "hold" and STARE at the phone waiting for A to answer. Only after A is done do you call B. This is what `async { }.await()` looks like — it's sequential, even though it uses `async`.

---

## 1️⃣ Sequential Composition — The Default

In a coroutine, every `suspend` function call is sequential by default. The next line does NOT start until the current suspend function completes.

```kotlin
suspend fun loginAndLoadProfile(email: String, password: String): Profile {
    val token   = authApi.login(email, password)   // step 1: waits until login completes
    val profile = profileApi.getProfile(token)     // step 2: only starts after step 1
    return profile
}
```

**When to use sequential:**
- Step 2 depends on step 1's result (like above — you need the token to get the profile)
- Order matters (e.g., validate form → submit → navigate)
- You need to ensure step 1's side effects are complete before step 2

```kotlin
// Real app: checkout flow — order matters, each step depends on previous
suspend fun processOrder(cart: Cart): OrderResult {
    val inventory  = inventoryApi.reserveItems(cart.items)   // 1. reserve stock
    val payment    = paymentApi.charge(cart.total)            // 2. charge (needs reserved inventory)
    val order      = orderApi.create(inventory, payment)      // 3. create order (needs both)
    emailApi.sendConfirmation(order.id)                       // 4. confirm (needs order)
    return OrderResult.Success(order)
}
```

---

## 2️⃣ Parallel Composition — The Right Way with `async`

Use `async` for **independent** work that doesn't need the other's result to start.

### The Step-by-Step Pattern

```kotlin
suspend fun loadHomeScreen(): HomeData = coroutineScope {
    // STEP 1: Start ALL async coroutines first (they begin running immediately)
    val userDeferred          = async { userRepo.getCurrentUser() }
    val feedDeferred          = async { feedRepo.getFeed() }
    val notificationsDeferred = async { notifRepo.getUnreadCount() }
    val bannersDeferred       = async { bannerRepo.getActiveBanners() }

    // STEP 2: Now await all of them (they've been running in parallel this whole time)
    HomeData(
        user          = userDeferred.await(),
        feed          = feedDeferred.await(),
        notifications = notificationsDeferred.await(),
        banners       = bannersDeferred.await()
    )
    // Time saved: instead of 4 sequential calls (e.g., 4 × 300ms = 1200ms),
    // we wait only for the slowest one (~300ms). 4× faster!
}
```

---

## 🚨 The Async Trap — THE Most Important Section

> [!CAUTION]
> This is the single most common coroutine mistake in production code AND the most asked coroutine interview question. Memorize this.

### Understanding WHY the trap happens

```kotlin
// Let's break this apart token by token:
val users = async { api.getUsers() }.await()
//           ^--------------------^    ^---^
//           This creates a Deferred   This immediately suspends the
//           AND starts the coroutine  coroutine until the result is ready
```

When you chain `.await()` directly after `async { }`:
1. `async { }` starts the coroutine → returns a `Deferred<T>`
2. `.await()` is called IMMEDIATELY on that `Deferred`
3. The current coroutine suspends and WAITS for the result
4. Only after the result arrives does the NEXT line execute

This is functionally identical to writing a plain `suspend` call — no parallelism at all.

```kotlin
// ❌ TRAP: These two blocks of code do the SAME thing (both sequential)
// Version 1: Chained async (looks parallel, isn't)
val users = async { api.getUsers() }.await()
val books = async { api.getBooks() }.await()

// Version 2: Plain suspend calls (obviously sequential)
val users = api.getUsers()
val books = api.getBooks()

// Both take: time(getUsers) + time(getBooks) = ~600ms if each takes 300ms
```

```kotlin
// ✅ CORRECT: Separate start from await
val usersDeferred = async { api.getUsers() }   // starts immediately, returns Deferred
val booksDeferred = async { api.getBooks() }   // starts immediately, returns Deferred
// At this point, BOTH are already running in parallel ^^^^^

val users = usersDeferred.await()              // wait for users (may already be done)
val books = booksDeferred.await()              // wait for books (may already be done)

// Takes: max(time(getUsers), time(getBooks)) = ~300ms ← 2× faster!
```

### Visual Timeline

```
❌ Sequential (async trap):
Time: 0ms────────300ms────────600ms
      [getUsers running]
                 [getBooks running]

✅ Parallel (correct):
Time: 0ms────────300ms
      [getUsers running]
      [getBooks running]
      Both finish by ~300ms!
```

### The Interview Proof Code

```kotlin
// You can PROVE the difference with timing:
suspend fun sequentialTime() = coroutineScope {
    val start = System.currentTimeMillis()
    val a = async { delay(300); "A" }.await()   // waits 300ms
    val b = async { delay(300); "B" }.await()   // then waits another 300ms
    val elapsed = System.currentTimeMillis() - start
    println("Sequential: ${elapsed}ms")   // prints: Sequential: ~600ms
}

suspend fun parallelTime() = coroutineScope {
    val start = System.currentTimeMillis()
    val aDeferred = async { delay(300); "A" }   // starts
    val bDeferred = async { delay(300); "B" }   // starts in parallel
    val a = aDeferred.await()
    val b = bDeferred.await()
    val elapsed = System.currentTimeMillis() - start
    println("Parallel: ${elapsed}ms")   // prints: Parallel: ~300ms
}
```

---

## 3️⃣ `awaitAll` — Clean Syntax for Multiple Parallel Results

`awaitAll()` waits for ALL the passed `Deferred` values and returns a list. If ANY of them fail, it throws.

```kotlin
// When all tasks have the same return type:
suspend fun loadAllPosts(): List<Post> = coroutineScope {
    val deferreds = userIds.map { userId ->
        async { postRepo.getPostsForUser(userId) }
    }
    awaitAll(*deferreds.toTypedArray())   // flatten to List<Post>
        .flatten()
}

// Or using awaitAll extension on list:
val allPosts = deferreds.awaitAll()   // extension on List<Deferred<T>>
```

```kotlin
// Real app: load data for multiple items on a detail screen
suspend fun loadProductDetails(productId: String): ProductDetails = coroutineScope {
    val productDeferred  = async { productRepo.getProduct(productId) }
    val reviewsDeferred  = async { reviewRepo.getReviews(productId) }
    val relatedDeferred  = async { productRepo.getRelated(productId) }

    val (product, reviews, related) = awaitAll(
        productDeferred,
        reviewsDeferred,
        relatedDeferred
    )

    ProductDetails(
        product = product as Product,
        reviews = reviews as List<Review>,
        related = related as List<Product>
    )
}
```

> [!WARNING]
> When return types differ between `Deferred` instances, `awaitAll` returns `List<Any>` which requires casting. In that case, explicit individual `await()` calls with proper types are cleaner and safer.

---

## 4️⃣ Partial Success Pattern — `supervisorScope` + `runCatching`

For a real app like a dashboard or home screen where each section is independent:

```kotlin
suspend fun loadDashboard(): DashboardUiState = supervisorScope {
    // All three start in parallel
    val usersDeferred  = async { runCatching { userRepo.getUsers() } }
    val ordersDeferred = async { runCatching { orderRepo.getRecentOrders() } }
    val offersDeferred = async { runCatching { offerRepo.getActiveOffers() } }

    // Wait for all three
    val usersResult  = usersDeferred.await()
    val ordersResult = ordersDeferred.await()
    val offersResult = offersDeferred.await()

    // Build state with whatever succeeded
    DashboardUiState(
        users         = usersResult.getOrDefault(emptyList()),
        recentOrders  = ordersResult.getOrDefault(emptyList()),
        activeOffers  = offersResult.getOrDefault(emptyList()),
        hasError      = usersResult.isFailure || ordersResult.isFailure || offersResult.isFailure,
        errorMessage  = when {
            usersResult.isFailure -> "Failed to load users"
            else -> null
        }
    )
}
```

---

## 5️⃣ `Deferred<T>` — Deep Understanding

`Deferred<T>` extends `Job` — it has all the same lifecycle states plus one extra state: "it has a result."

```kotlin
// Deferred<T> is Job<T> with a value
interface Deferred<out T> : Job {
    suspend fun await(): T   // suspends caller until result is available, then returns it
    fun getCompleted(): T    // non-suspend, throws if not completed yet
    fun getCompletionExceptionOrNull(): Throwable?  // returns exception if failed
}
```

### Exception timing with `async`

```kotlin
viewModelScope.launch {
    val deferred = async {
        delay(1000)
        throw IOException("Network failed")   // exception occurs here at 1 second
    }

    delay(500)   // at this point, the exception hasn't been thrown to US yet
    println("Still here")   // this prints!

    deferred.await()   // NOW the IOException propagates to this coroutine
    // This line never runs:
    println("Never reached")
}
```

This is fundamentally different from `launch`:
```kotlin
viewModelScope.launch {
    delay(1000)
    throw IOException("Network failed")   // propagates to parent IMMEDIATELY
}
// Other launch{} coroutines in viewModelScope are unaffected (SupervisorJob)
```

---

## 6️⃣ `CoroutineStart.LAZY` — Controlled Start

By default, `async { }` starts IMMEDIATELY when called. `CoroutineStart.LAZY` delays the start until either `.start()` or `.await()` is called.

```kotlin
// Lazy async — doesn't start yet
val deferred = async(start = CoroutineStart.LAZY) {
    expensiveCalculation()
}

// Some time later...
if (userRequestedData) {
    deferred.start()    // starts the coroutine (non-suspend)
    // do other things...
    val result = deferred.await()  // wait for result
}
// If userRequestedData is never true, expensiveCalculation() never runs!
```

**Real use case: conditional pre-fetching**
```kotlin
suspend fun loadScreen(showAdvanced: Boolean) = coroutineScope {
    val basicData   = async { repo.getBasicData() }    // always needed
    val advancedData = async(start = CoroutineStart.LAZY) {  // only if needed
        repo.getAdvancedData()
    }

    if (showAdvanced) {
        advancedData.start()   // kick off only when we know we need it
    }

    ScreenData(
        basic    = basicData.await(),
        advanced = if (showAdvanced) advancedData.await() else null
    )
}
```

---

## 📊 Choosing the Right Pattern

| Situation | Pattern | Code shape |
|---|---|---|
| Step B needs step A's result | Sequential | `val a = suspend1(); val b = suspend2(a)` |
| A and B are independent, all must succeed | Parallel `coroutineScope` | `val aD = async {...}; val bD = async {...}; a = aD.await(); b = bD.await()` |
| A and B are independent, partial success OK | Parallel `supervisorScope` | Same but with `runCatching` around each `await()` |
| Switch to a different thread | `withContext` | `val result = withContext(Dispatchers.IO) { blockingWork() }` |
| Many items processed in parallel | `map { async { } }.awaitAll()` | `items.map { async { process(it) } }.awaitAll()` |
| Conditional/delayed start | `CoroutineStart.LAZY` | `val d = async(start = LAZY) { ... }; if (needed) d.start()` |

---

## ✅ Real App: E-Commerce Product Screen

```kotlin
class ProductViewModel(
    private val productRepo: ProductRepository,
    private val reviewRepo: ReviewRepository,
    private val recommendRepo: RecommendationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductUiState>(ProductUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _uiState.value = ProductUiState.Loading
            try {
                // Product details and reviews must be shown together (atomic)
                // Recommendations are optional
                val productDetails = coroutineScope {
                    val productDeferred = async { productRepo.getProduct(productId) }
                    val reviewsDeferred = async { reviewRepo.getReviews(productId, limit = 5) }

                    // These two MUST both succeed
                    ProductDetails(
                        product = productDeferred.await(),
                        reviews = reviewsDeferred.await()
                    )
                }

                // Now load recommendations independently (failure doesn't break the screen)
                val recommendations = try {
                    recommendRepo.getRecommendations(productId)
                } catch (e: Exception) {
                    emptyList()  // recommendations are optional
                }

                _uiState.value = ProductUiState.Success(
                    details         = productDetails,
                    recommendations = recommendations
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = ProductUiState.Error(e.message ?: "Failed to load product")
            }
        }
    }
}
```

---

## 🔗 Connections

- **Previous**: [[Coroutine 02 Structured Concurrency Cancellation Exceptions]] — `coroutineScope` vs `supervisorScope`
- **Next**: [[Coroutine 04 Channels Producer Consumer]] — when async isn't enough
- **In ViewModels**: the patterns here are how you implement state loading in [[State vs StateFlow]]

---

## 💬 Interview Master Q&A

**Q: What is the async trap?**
> The async trap is writing `val result = async { doWork() }.await()`. Even though it uses `async`, this is completely sequential. `async { }` starts the coroutine and returns a `Deferred`, but immediately calling `.await()` suspends the current coroutine until that `Deferred` completes — preventing any other coroutine from starting in parallel. The correct pattern is to start all `async` calls first (storing their `Deferred` references), then call `.await()` on each. This gives you actual parallel execution.

**Q: What is the difference between `async` and `withContext`?**
> `withContext` switches the dispatcher and runs the block sequentially, suspending the current coroutine until the block is done. It does NOT create a new coroutine — it reuses the current one. `async` creates a NEW coroutine that runs concurrently with the caller. Use `withContext` for thread-switching (e.g., moving to IO for a Retrofit call), and `async` for truly parallel work where you need multiple things running simultaneously.

**Q: When would you use `awaitAll()` vs individual `await()` calls?**
> Use `awaitAll()` when all tasks have the same return type and all must succeed for the result to be valid — it's concise and fails fast if any task fails. Use individual `await()` calls wrapped in `runCatching {}` when tasks have different return types or when partial success is acceptable. For example, on a dashboard screen where each section loads independently, individual `await()` + `runCatching` is the right choice so one failed section doesn't blank out the entire screen.

**Q: What is `Deferred<T>` and how does it differ from `Job`?**
> `Deferred<T>` extends `Job` — it represents an in-progress coroutine computation that will eventually produce a value of type `T`. `Job` is fire-and-forget (no return value). `Deferred<T>` adds the `await()` suspend function that pauses the caller until the value is ready. Importantly, exceptions in `async` are stored inside the `Deferred` and only thrown when `.await()` is called, unlike `launch` which propagates exceptions to the parent immediately.
