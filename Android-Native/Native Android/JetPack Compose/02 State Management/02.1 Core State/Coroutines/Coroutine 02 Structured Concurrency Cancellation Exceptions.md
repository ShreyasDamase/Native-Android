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
