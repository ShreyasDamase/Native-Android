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
