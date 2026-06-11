# Coroutine 02 Structured Concurrency Cancellation Exceptions

This note covers structured concurrency, cancellation, timeouts, and exception handling.

---

## Structured Concurrency

### Meaning

- Coroutines are organized as parent-child jobs.
- Parent waits for child coroutines to finish.
- Cancelling parent cancels children.
- Child failure normally cancels parent and siblings.
- This prevents leaked background work.

```kotlin
viewModelScope.launch {
    launch { api.getUser() }
    launch { api.getBooks() }
}
```

### Interview explanation

- "Structured concurrency means child coroutines are bound to parent lifetime. It makes cancellation and error propagation predictable."

---

## `coroutineScope`

### Rules

- Creates a child scope inside a suspend function.
- Suspends until all children complete.
- If one child fails, siblings are cancelled.
- Best for all-or-nothing work.

```kotlin
suspend fun checkout(): CheckoutResult = coroutineScope {
    val price = async { pricingApi.calculatePrice() }
    val address = async { addressApi.validateAddress() }
    val token = async { paymentApi.createToken() }

    CheckoutResult(
        price = price.await(),
        address = address.await(),
        token = token.await()
    )
}
```

### Real-life use

- Checkout.
- Payment.
- Order creation.
- Upload where every part must succeed.

---

## `supervisorScope`

### Rules

- Creates a child scope with supervision.
- Child failure does not cancel siblings.
- Best when partial success is allowed.

```kotlin
suspend fun loadDashboard(): DashboardData = supervisorScope {
    val users = async { api.getUsers() }
    val books = async { api.getBooks() }

    DashboardData(
        users = runCatching { users.await() }.getOrDefault(emptyList()),
        books = runCatching { books.await() }.getOrDefault(emptyList())
    )
}
```

### Real-life use

- Dashboard widgets.
- Home screen sections.
- Profile plus recommendations.
- Optional analytics or secondary API calls.

---

## `coroutineScope` vs `supervisorScope`

| Topic | `coroutineScope` | `supervisorScope` |
|---|---|---|
| Failure | One child failure cancels siblings | One child failure does not cancel siblings |
| Use case | All-or-nothing | Independent tasks |
| Example | Payment flow | Dashboard sections |
| Result | Whole operation succeeds/fails | Partial success possible |

### Important `awaitAll()` note

- `supervisorScope` prevents sibling cancellation.
- `await()` still throws if that child failed.
- `awaitAll()` still throws if any deferred failed.
- For partial success, catch each `await()` separately.

```kotlin
viewModelScope.launch {
    supervisorScope {
        val usersDeferred = async(Dispatchers.IO) { api.getAllUsers() }
        val booksDeferred = async(Dispatchers.IO) { api.getBooks() }

        val usersResult = runCatching { usersDeferred.await() }
        val booksResult = runCatching { booksDeferred.await() }

        usersResult.onSuccess { _users.value = it }
        booksResult.onSuccess { _books.value = it }

        if (usersResult.isFailure || booksResult.isFailure) {
            _error.value = "Some data failed to load"
        }
    }
}
```

---

## Cancellation

### Meaning

- Cancellation is cooperative.
- Coroutine is not force-killed.
- It stops when it reaches a cancellation check.
- Suspending functions like `delay`, `await`, `withContext`, and Flow collection check cancellation.
- CPU loops need manual checks.

### `Job.cancel()`

```kotlin
private var fetchJob: Job? = null

fun fetchUsers() {
    fetchJob?.cancel()

    fetchJob = viewModelScope.launch {
        _loading.value = true
        try {
            _users.value = repository.getUsers()
        } finally {
            _loading.value = false
        }
    }
}
```

### `isActive`

```kotlin
suspend fun calculate(): Int = withContext(Dispatchers.Default) {
    var result = 0
    for (i in 1..100_000_000) {
        if (!isActive) break
        result += i
    }
    result
}
```

### `ensureActive()`

```kotlin
suspend fun processItems(items: List<Item>) = withContext(Dispatchers.Default) {
    items.forEach { item ->
        ensureActive()
        process(item)
    }
}
```

### `yield()`

```kotlin
suspend fun processLargeList(items: List<Item>) = withContext(Dispatchers.Default) {
    items.forEachIndexed { index, item ->
        process(item)
        if (index % 100 == 0) yield()
    }
}
```

### Interview explanation

- "Cancellation is cooperative. Suspending functions check cancellation automatically, but CPU-heavy loops should use `isActive`, `ensureActive()`, or `yield()`."

---

## Timeouts

### `withTimeout`

- Cancels block if time limit is reached.
- Throws `TimeoutCancellationException`.

```kotlin
viewModelScope.launch {
    try {
        val users = withTimeout(3000) {
            repository.getUsers()
        }
        _users.value = users
    } catch (e: TimeoutCancellationException) {
        _error.value = "Request timed out"
    }
}
```

### `withTimeoutOrNull`

- Cancels block if time limit is reached.
- Returns `null` instead of throwing.

```kotlin
val cachedData = withTimeoutOrNull(1000) {
    repository.getCachedData()
} ?: emptyList()
```

### Cleanup with `finally`

```kotlin
viewModelScope.launch {
    try {
        uploadLargeFile()
    } finally {
        _loading.value = false
    }
}
```

### `NonCancellable`

- Used when cleanup itself must call suspend functions.
- Keep it short.

```kotlin
viewModelScope.launch {
    try {
        uploadLargeFile()
    } finally {
        withContext(NonCancellable) {
            repository.markUploadStopped()
        }
    }
}
```

---

## Exception Handling

### Key rules

- `launch` exceptions propagate to parent immediately.
- `async` exceptions are thrown when `await()` is called.
- Use local `try/catch` for expected errors.
- `CancellationException` should usually be rethrown.

### Retrofit error handling

```kotlin
viewModelScope.launch {
    try {
        val users = repository.getUsers()
        _users.value = users
    } catch (e: CancellationException) {
        throw e
    } catch (e: UnknownHostException) {
        _error.value = "No internet connection"
    } catch (e: SocketTimeoutException) {
        _error.value = "Network timeout"
    } catch (e: HttpException) {
        _error.value = "HTTP ${e.code()}: ${e.message()}"
    } catch (e: IOException) {
        _error.value = "Network error"
    } catch (e: Exception) {
        _error.value = "Something went wrong"
    }
}
```

### `CoroutineExceptionHandler`

- Handles uncaught exceptions.
- Mostly useful for root `launch`.
- Does not replace local `try/catch` for expected errors.
- Does not catch `async` exceptions until they are awaited.

```kotlin
val handler = CoroutineExceptionHandler { _, exception ->
    Log.e("Coroutine", "Unhandled", exception)
}

viewModelScope.launch(handler) {
    throw RuntimeException("Boom")
}
```

### Interview explanation

- "`launch` throws to parent immediately. `async` exposes exceptions through `await()`. I use local `try/catch` for expected network errors and rethrow `CancellationException`."


---

## 🚀 Mastery Deep Dive (Added 2026)

> [!NOTE]
> The following deep dive notes were generated to provide mastery-level understanding, complementing the original notes above.

# Coroutine 02 — Structured Concurrency, Cancellation & Exceptions

> [!NOTE]
> This is the most critical file for writing safe Android apps. The bugs caused by bad cancellation and exception handling are the hardest to find and reproduce. Read carefully.

---

## 🧠 Mental Model — Read This First

**Think of a military platoon on a mission.**

- **Structured Concurrency** = every soldier (coroutine) is under a commander (scope). When the mission ends or is aborted, ALL soldiers stop — no soldier runs off on their own.
- **Cancellation** = the commander radios "abort!" The soldiers don't die instantly — they finish their current step, check their radio at every checkpoint, then stop safely.
- **`CancellationException`** = the "abort" signal itself. If a soldier swallows the signal (ignores `CancellationException`), they keep running forever even after the mission is cancelled. The army (your app) has a ghost soldier that can never be recalled.
- **`coroutineScope`** = one-for-all. If any soldier fails, the whole platoon retreats.
- **`supervisorScope`** = independent teams. If team A fails, team B keeps going.

---

## 🏛️ Structured Concurrency

### What it means

Every coroutine has a **parent**. A parent cannot complete until ALL its children complete (or are cancelled). This forms a tree:

```
viewModelScope (parent)
    ├── launch { fetchUsers() }       (child 1)
    │   └── launch { updateCache() } (grandchild)
    └── launch { fetchBooks() }       (child 2)
```

**The 3 guarantees of structured concurrency:**

1. **Containment**: If parent is cancelled → all children are cancelled
2. **Propagation**: If a child fails → parent is notified (and usually fails too)
3. **Completion**: Parent only completes when ALL children complete

```kotlin
viewModelScope.launch {
    launch { delay(1000); println("Child 1 done") }
    launch { delay(2000); println("Child 2 done") }
    // viewModelScope will NOT be "idle" until both children finish
}
```

### Why it prevents leaks

```kotlin
// ❌ OLD CALLBACK HELL — no structure, work can leak
fun fetchUserProfile(userId: String, callback: (Profile) -> Unit) {
    Thread {
        val user = api.getUser(userId)   // what if the caller is destroyed here?
        val posts = api.getPosts(userId) // this still runs! Callback fires on a dead screen!
        callback(Profile(user, posts))   // crash: callback touches a destroyed view
    }.start()
}

// ✅ COROUTINE — structured, safe
suspend fun fetchUserProfile(userId: String): Profile = coroutineScope {
    val userDeferred  = async { api.getUser(userId) }
    val postsDeferred = async { api.getPosts(userId) }
    Profile(userDeferred.await(), postsDeferred.await())
    // If the caller's scope (e.g., viewModelScope) is cancelled,
    // BOTH async calls are cancelled immediately. No leak.
}
```

---

## 🔱 `coroutineScope` vs `supervisorScope`

### `coroutineScope` — All-or-Nothing

```kotlin
// Creates a new scope that inherits parent context.
// ALL children must succeed, or ALL are cancelled.

suspend fun processPayment(): PaymentResult = coroutineScope {
    val price      = async { pricingApi.calculatePrice() }    // must succeed
    val token      = async { paymentApi.createToken() }       // must succeed
    val address    = async { addressApi.validate() }          // must succeed

    PaymentResult(
        price   = price.await(),   // if this throws...
        token   = token.await(),   // ...this gets cancelled
        address = address.await()  // ...and this gets cancelled
    )
    // If ANY fails → the whole payment fails cleanly → no partial state
}
```

**Use `coroutineScope` when:** the operation only makes sense if ALL parts succeed (payment, checkout, multi-step upload, atomic database write).

---

### `supervisorScope` — Independent Failures

```kotlin
// Like coroutineScope but child failures do NOT propagate to siblings.

suspend fun loadDashboard(): DashboardState = supervisorScope {
    val usersDeferred  = async { userRepo.getUsers() }     // independent
    val statsDeferred  = async { statsRepo.getStats() }    // independent
    val offersDeferred = async { offersRepo.getOffers() }  // independent

    // Each await() must be individually guarded
    DashboardState(
        users  = runCatching { usersDeferred.await() }.getOrDefault(emptyList()),
        stats  = runCatching { statsDeferred.await() }.getOrDefault(Stats.empty()),
        offers = runCatching { offersDeferred.await() }.getOrDefault(emptyList())
    )
    // If offers API is down → users and stats still show ✅
}
```

**Use `supervisorScope` when:** partial success is acceptable (dashboard widgets, independent screen sections, optional features like analytics, recommendations, or ads).

---

### Side-by-Side Comparison

| Aspect | `coroutineScope` | `supervisorScope` |
|---|---|---|
| Child failure → siblings? | ✅ Cancels all siblings | ❌ Siblings unaffected |
| Child failure → parent? | ✅ Propagates to parent | ❌ Parent unaffected |
| Use case | Payment, checkout, upload | Dashboard, home screen, optional features |
| Exception handling | One try/catch around the whole block | `runCatching {}` around each `await()` |

> [!IMPORTANT]
> **`awaitAll()` does NOT give you partial success** even inside `supervisorScope`. `awaitAll()` throws if any single `Deferred` failed. For partial success, use separate `await()` calls each wrapped in `runCatching {}`.
> ```kotlin
> // ❌ awaitAll throws if either fails — no partial success
> val (users, books) = awaitAll(usersDeferred, booksDeferred)
>
> // ✅ Individual await with runCatching — partial success possible
> val users = runCatching { usersDeferred.await() }.getOrDefault(emptyList())
> val books = runCatching { booksDeferred.await() }.getOrDefault(emptyList())
> ```

---

## ❌ Cancellation — How It Really Works

### Cancellation is COOPERATIVE (not forced)

When you call `job.cancel()`, Android does NOT kill the coroutine like `Thread.stop()`. Instead, it sets a `isCancelled` flag to `true`. The coroutine only actually stops when it CHECKS this flag — which happens at **suspension points**.

```kotlin
// Every suspend function is a cancellation checkpoint:
delay(1000)            // ← checks cancellation. If cancelled, throws CancellationException
await()                // ← checks cancellation
withContext(...)       // ← checks cancellation
flow.collect { }       // ← checks cancellation on each emission
yield()                // ← explicitly checks cancellation + gives other coroutines a turn
ensureActive()         // ← explicitly checks cancellation, throws if cancelled
```

### CPU-Bound Loops MUST Check Manually

```kotlin
// ❌ DANGER — cancel() is called but this never stops
suspend fun processImages(images: List<Bitmap>): List<Bitmap> =
    withContext(Dispatchers.Default) {
        images.map { bitmap ->
            applyHeavyFilter(bitmap)   // no suspension point here — cancellation is NEVER checked
        }
    }
// Calling job.cancel() does nothing until map{} finishes processing ALL images!

// ✅ SAFE — check cancellation every iteration
suspend fun processImages(images: List<Bitmap>): List<Bitmap> =
    withContext(Dispatchers.Default) {
        images.map { bitmap ->
            ensureActive()             // throws CancellationException if cancelled → loop stops
            applyHeavyFilter(bitmap)
        }
    }

// OR use yield() every N iterations (also suspends briefly for other coroutines)
suspend fun processItems(items: List<Item>): List<Result> =
    withContext(Dispatchers.Default) {
        items.mapIndexed { index, item ->
            if (index % 100 == 0) yield()   // check cancellation + cooperate every 100 items
            processItem(item)
        }
    }
```

**`isActive` vs `ensureActive()` vs `yield()`:**

| Function | What it does | When to use |
|---|---|---|
| `isActive` | Returns `Boolean` — you check and break manually | When you need custom cleanup before stopping |
| `ensureActive()` | Throws `CancellationException` if not active | Cleaner way to stop — exception propagates normally |
| `yield()` | Checks cancellation + suspends to let other coroutines run | CPU loops where fairness matters |

```kotlin
// isActive — manual break
suspend fun work() = withContext(Dispatchers.Default) {
    for (i in 1..1_000_000) {
        if (!isActive) {
            cleanupResources()   // do something before stopping
            break
        }
        process(i)
    }
}

// ensureActive — clean exception propagation
suspend fun work() = withContext(Dispatchers.Default) {
    for (i in 1..1_000_000) {
        ensureActive()   // throws if cancelled — try/finally above will run
        process(i)
    }
}
```

---

## 🚨 `CancellationException` — The Rule You Must Never Break

### Why it must ALWAYS be rethrown

`CancellationException` is the mechanism by which the coroutine framework communicates "this coroutine has been cancelled." It propagates UP the coroutine tree so every parent knows its child was cancelled and can clean up.

**When you catch and swallow `CancellationException`, you break the entire cancellation chain:**

```kotlin
// ❌ CATASTROPHIC BUG — swallowing CancellationException
viewModelScope.launch {
    try {
        delay(5000)   // user navigates away → viewModelScope.cancel() → this throws CancellationException
    } catch (e: Exception) {   // catches EVERYTHING including CancellationException!
        Log.e("Error", e.message ?: "")
        // CancellationException is SWALLOWED HERE
        // The parent Job never learns this child was cancelled
        // The coroutine is now "stuck" — it appears active but is in a broken state
    }
}

// ✅ CORRECT — always rethrow CancellationException
viewModelScope.launch {
    try {
        delay(5000)
    } catch (e: CancellationException) {
        throw e   // rethrow FIRST before any other handling
    } catch (e: IOException) {
        _error.value = "Network error"
    } catch (e: Exception) {
        _error.value = "Something went wrong"
    }
}

// ✅ ALSO CORRECT — catch only specific non-cancellation exceptions
viewModelScope.launch {
    try {
        delay(5000)
    } catch (e: IOException) {
        // IOException is NOT CancellationException, so this is fine
        _error.value = "Network error"
    }
    // CancellationException propagates naturally (not caught here)
}
```

> [!CAUTION]
> If you use `catch (e: Exception)` or `catch (e: Throwable)`, you MUST rethrow `CancellationException`. Failure to do so creates coroutines that are impossible to cancel — a serious memory leak and behavioral bug.

### The `runCatching` trap

```kotlin
// ❌ runCatching also catches CancellationException!
val result = runCatching {
    delay(5000)  // if cancelled, CancellationException is caught and stored as failure
}
// The cancellation is now lost. result.isFailure == true but nobody rethrows it.

// ✅ Check and rethrow after runCatching if you use it
val result = runCatching {
    delay(5000)
}.onFailure { e ->
    if (e is CancellationException) throw e  // restore cancellation
}
```

---

## ⏱️ Timeouts

### `withTimeout` — Throws on Timeout

```kotlin
viewModelScope.launch {
    try {
        val profile = withTimeout(5_000L) {   // 5 seconds max
            userRepo.fetchFullProfile()        // if this takes > 5s...
        }
        _uiState.value = UiState.Success(profile)
    } catch (e: TimeoutCancellationException) {   // ...this is thrown
        _uiState.value = UiState.Error("Request took too long. Please try again.")
    }
}
```

> [!IMPORTANT]
> `TimeoutCancellationException` IS a subclass of `CancellationException`. If you're catching `CancellationException` to rethrow it, but want to handle timeouts specially, catch `TimeoutCancellationException` FIRST (it's more specific):
> ```kotlin
> try {
>     withTimeout(5000) { doWork() }
> } catch (e: TimeoutCancellationException) {
>     // handle timeout specifically
> } catch (e: CancellationException) {
>     throw e   // rethrow other cancellations
> }
> ```

### `withTimeoutOrNull` — Returns `null` on Timeout

```kotlin
// Cleaner when timeout means "use fallback" not "show error"
val cachedData = withTimeoutOrNull(500L) {   // try cache for 500ms
    cacheRepo.getData()
} ?: run {
    // cache miss or too slow → fetch fresh
    networkRepo.getData()
}
```

---

## 🧹 Cleanup with `finally` — Always Runs

`finally` runs whether the coroutine:
- Completes normally
- Throws an exception
- Is cancelled

```kotlin
viewModelScope.launch {
    _isLoading.value = true
    try {
        val file = openFile()
        uploadFile(file)
    } finally {
        _isLoading.value = false   // always hides the loading spinner
        // Note: you CANNOT call suspend functions in finally during cancellation
        // UNLESS you use NonCancellable (see below)
    }
}
```

---

## 🚫 `NonCancellable` — Use Only in `finally` Blocks

During cancellation, the coroutine framework prevents suspension in `finally` blocks (because the coroutine is being torn down). If you need to call a `suspend` function during cleanup (e.g., save state, log to server), you must use `NonCancellable`:

```kotlin
viewModelScope.launch {
    try {
        uploadLargeFile()
    } finally {
        // ❌ This would throw "Cannot use this scope after cancellation"
        // repository.markUploadStopped()

        // ✅ NonCancellable creates a non-cancellable continuation for cleanup
        withContext(NonCancellable) {
            repository.markUploadStopped()   // suspend call during cancellation — now works
        }
    }
}
```

> [!WARNING]
> `NonCancellable` should ONLY be used inside `finally` blocks for critical cleanup. Never use it as a general "I don't want this to be cancelled" — that defeats structured concurrency and creates un-cancellable work.

---

## ⚡ Exception Handling

### `launch` vs `async` exception behavior

```kotlin
// launch — exception propagates to parent IMMEDIATELY
viewModelScope.launch {
    throw RuntimeException("Boom!")   // immediately propagates to viewModelScope
    // viewModelScope's SupervisorJob catches this, other launch{} blocks unaffected
}

// async — exception stored in Deferred, thrown only on await()
val deferred = viewModelScope.async {
    throw RuntimeException("Boom!")   // NOT propagated yet — stored in Deferred
}
// ... deferred.await() throws RuntimeException here, when YOU call it
```

### Standard Retrofit Error Handling Pattern

```kotlin
viewModelScope.launch {
    _uiState.update { it.copy(isLoading = true) }
    try {
        val users = userRepo.getUsers()
        _uiState.update { it.copy(isLoading = false, users = users) }
    } catch (e: CancellationException) {
        throw e                              // ALWAYS rethrow cancellation
    } catch (e: UnknownHostException) {
        _uiState.update { it.copy(isLoading = false, error = "No internet") }
    } catch (e: SocketTimeoutException) {
        _uiState.update { it.copy(isLoading = false, error = "Connection timed out") }
    } catch (e: HttpException) {
        val message = when (e.code()) {
            401 -> "Unauthorized — please log in again"
            403 -> "You don't have permission"
            404 -> "Not found"
            500 -> "Server error — please try again"
            else -> "HTTP error ${e.code()}"
        }
        _uiState.update { it.copy(isLoading = false, error = message) }
    } catch (e: IOException) {
        _uiState.update { it.copy(isLoading = false, error = "Network error") }
    }
}
```

### `CoroutineExceptionHandler` — Centralized Logging, NOT Error Recovery

```kotlin
val handler = CoroutineExceptionHandler { context, exception ->
    // context.job is the failed job
    // exception is the unhandled exception
    Log.e("Coroutine", "Unhandled exception in ${context[CoroutineName]}", exception)
    crashReporter.report(exception)
}

viewModelScope.launch(handler) {
    riskyOperation()   // if this throws and isn't caught inside → handler fires
}
```

**What `CoroutineExceptionHandler` CANNOT do:**
- It does NOT catch exceptions from `async` until they are `await()`-ed (those are your responsibility)
- It does NOT catch `CancellationException`
- It does NOT replace `try/catch` for expected errors like network failures
- It only works for **root** coroutines (launched directly on a scope, not children)

**Think of it as a last-resort crash reporter, not an error handler.**

---

## ✅ Real App Patterns

### Pattern: Debounced search with proper cancellation

```kotlin
class SearchViewModel(private val repo: SearchRepository) : ViewModel() {

    private val _results = MutableStateFlow<List<Product>>(emptyList())
    val results = _results.asStateFlow()

    private var searchJob: Job? = null

    fun onSearchQuery(query: String) {
        searchJob?.cancel()   // cancel previous search (sends CancellationException to it)

        if (query.isBlank()) {
            _results.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)   // debounce — if another query arrives within 300ms, this is cancelled

            try {
                val results = repo.search(query)
                _results.value = results
            } catch (e: CancellationException) {
                throw e   // rethrow — this coroutine was cancelled by a newer query
            } catch (e: IOException) {
                _results.value = emptyList()
                // optionally show error
            }
        }
    }
}
```

### Pattern: Uploading a file with cancellation and cleanup

```kotlin
fun uploadFile(uri: Uri) {
    viewModelScope.launch {
        _uploadState.value = UploadState.Uploading(progress = 0f)
        try {
            fileRepo.upload(uri) { progress ->
                _uploadState.value = UploadState.Uploading(progress)
            }
            _uploadState.value = UploadState.Success
        } catch (e: CancellationException) {
            _uploadState.value = UploadState.Cancelled
            throw e   // rethrow!
        } catch (e: Exception) {
            _uploadState.value = UploadState.Error(e.message ?: "Upload failed")
        } finally {
            withContext(NonCancellable) {
                fileRepo.cleanUpTempFiles()   // always clean up, even on cancellation
            }
        }
    }
}

fun cancelUpload() {
    viewModelScope.coroutineContext[Job]?.cancelChildren()
    // or track the specific job and cancel it
}
```

---

## ❌ Common Mistakes Summary

| Mistake | Consequence | Fix |
|---|---|---|
| `catch(e: Exception)` swallows `CancellationException` | Coroutine cannot be cancelled, memory leak | Always `if (e is CancellationException) throw e` |
| CPU loop without `ensureActive()` | `cancel()` has no effect until loop finishes | Add `ensureActive()` or `yield()` inside loop |
| Calling `suspend` in `finally` during cancellation | `IllegalStateException` | Use `withContext(NonCancellable) { }` |
| Using `awaitAll()` for partial success | All-or-nothing even in `supervisorScope` | Use individual `await()` + `runCatching {}` |
| Using `runCatching` without rethrowing `CancellationException` | Cancellation silently swallowed | Check and rethrow after `runCatching` |

---

## 🔗 Connections

- **Previous**: [[Coroutine 01 Builders Dispatchers Scopes]] — builders and Job lifecycle
- **Next**: [[Coroutine 03 Sequential Parallel Async Patterns]] — the parallel patterns that rely on structured concurrency
- **Flow cancellation**: Flow collection is also cancellation-cooperative — see [[Android Jetpack Flow Study Notes]]

---

## 💬 Interview Master Q&A

**Q: What is structured concurrency?**
> Structured concurrency means every coroutine must have a parent scope, and parent scopes cannot complete until all their child coroutines complete. This forms a tree where cancellation flows top-down (parent cancelled → children cancelled) and failures flow bottom-up (child fails → parent is notified). It eliminates coroutine leaks because no coroutine can outlive the scope that started it.

**Q: What is the difference between `coroutineScope` and `supervisorScope`?**
> Both create a new child scope that suspends the caller until all children complete. The difference is failure propagation: in `coroutineScope`, any single child failure cancels ALL other children and fails the scope. In `supervisorScope`, children fail independently — one failure doesn't affect siblings. Use `coroutineScope` for atomic all-or-nothing operations (payment flow), and `supervisorScope` for independent parallel tasks where partial success is acceptable (dashboard sections).

**Q: Why must `CancellationException` always be rethrown?**
> `CancellationException` is the Kotlin coroutine framework's signal that a coroutine has been cancelled. When it propagates up the coroutine tree, each parent can properly clean up and mark itself as cancelled. If you catch and swallow it, the parent Job never learns the child was cancelled, potentially leaving the parent in an "active" state indefinitely. This is a serious memory and resource leak. The rule is: catch it if you need to do cleanup, but always `throw e` after.

**Q: Is cancellation in coroutines immediate?**
> No. Cancellation in Kotlin coroutines is cooperative, not forced. Calling `job.cancel()` sets a cancellation flag but does not immediately stop the coroutine. The coroutine only stops when it reaches the next suspension point (like `delay`, `await`, `withContext`, or `yield()`), which checks the flag and throws `CancellationException`. For CPU-bound loops with no suspension points, you must manually call `ensureActive()` or `yield()` periodically. This cooperative design ensures resources can be cleaned up safely before stopping.

**Q: When should you use `NonCancellable`?**
> `NonCancellable` should only be used inside `finally` blocks when you need to call a `suspend` function as part of cleanup during cancellation. Normally, the coroutine framework prevents suspension after cancellation begins. Wrapping cleanup code in `withContext(NonCancellable) { }` creates a non-cancellable context that allows the cleanup to complete. It should never be used outside of `finally` blocks, as it would create uncancellable work that defeats the purpose of structured concurrency.
