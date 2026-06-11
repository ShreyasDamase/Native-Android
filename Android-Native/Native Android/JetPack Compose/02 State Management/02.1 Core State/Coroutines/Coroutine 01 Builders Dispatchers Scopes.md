# Coroutine 01 Builders Dispatchers Scopes

This note covers builders, dispatchers, Android thread model, and lifecycle-aware scopes.

---

## Coroutine Builders

Builders start coroutines.

### `launch`

- Starts a coroutine.
- Returns `Job`.
- Does not return a result.
- Best for fire-and-forget work.
- Uncaught exceptions propagate to the parent.

```kotlin
viewModelScope.launch {
    repository.syncData()
}
```

### Android example

```kotlin
fun refreshUsers() {
    viewModelScope.launch {
        _loading.value = true
        try {
            _users.value = repository.getUsers()
        } finally {
            _loading.value = false
        }
    }
}
```

### `async`

- Starts a coroutine that returns a result.
- Returns `Deferred<T>`.
- Use `await()` to get the result.
- Good for parallel independent tasks.
- Exceptions are observed on `await()`.

```kotlin
viewModelScope.launch {
    val usersDeferred = async { repository.getUsers() }
    val booksDeferred = async { repository.getBooks() }

    val users = usersDeferred.await()
    val books = booksDeferred.await()
}
```

### `runBlocking`

- Starts a coroutine and blocks the current thread.
- Useful in `main()` functions and some tests.
- Avoid in Android UI code.

```kotlin
fun main() = runBlocking {
    println(repository.getUsers())
}
```

### `withContext`

- Switches coroutine context.
- Returns the result of the block.
- Does not create parallel work like `async`.
- Commonly used in repositories.

```kotlin
suspend fun getUsers(): List<User> = withContext(Dispatchers.IO) {
    api.getUsers()
}
```

### Builder comparison

| Builder | Returns | Blocks thread | Use case |
|---|---|---|---|
| `launch` | `Job` | No | Start work without result |
| `async` | `Deferred<T>` | No | Parallel result |
| `runBlocking` | Result | Yes | `main()` or tests |
| `withContext` | Result | No | Switch dispatcher |

### Interview explanation

- "`launch` is for work without a result. `async` is for concurrent work with result. `withContext` switches dispatcher and waits for the block to finish. `runBlocking` blocks the thread and should not be used in Android UI."

---

## Dispatchers and Android Thread Model

Dispatchers decide which thread or thread pool runs coroutine code.

### `Dispatchers.Main`

- Android main UI thread.
- Used for UI state updates.
- Should not run heavy CPU or blocking IO work.

```kotlin
viewModelScope.launch(Dispatchers.Main) {
    _uiText.value = "Loaded"
}
```

### `Dispatchers.IO`

- Network calls.
- Database calls.
- File reading/writing.
- Blocking IO operations.

```kotlin
suspend fun loadBooks(): List<Book> = withContext(Dispatchers.IO) {
    api.getBooks()
}
```

### `Dispatchers.Default`

- CPU-heavy work.
- Sorting large lists.
- Parsing huge data.
- Image processing.
- Calculations.

```kotlin
suspend fun sortUsers(users: List<User>): List<User> = withContext(Dispatchers.Default) {
    users.sortedBy { it.name }
}
```

### `Dispatchers.Unconfined`

- Starts on current thread.
- After suspension, may resume on a different thread.
- Rarely useful in Android app code.
- Avoid unless you know the exact reason.

### My phone mental model

- My Redmi Note 11 Pro has 8 CPU cores.
- Only a limited amount of CPU-heavy work can run truly at the same instant.
- Android can still schedule many software threads.
- Coroutines help by suspending instead of blocking threads.

### Interview explanation

- "`Main` is for UI, `IO` is for network/database/files, and `Default` is for CPU-heavy work. A suspend function does not automatically change thread; dispatcher controls where it runs."

---

## CoroutineScope and Lifecycle Scopes

### CoroutineScope

- Defines coroutine lifetime.
- Contains `CoroutineContext`.
- Usually contains a `Job`.
- Cancelling the scope cancels its child coroutines.

### `viewModelScope`

- Tied to ViewModel lifecycle.
- Cancelled automatically when ViewModel is cleared.
- Best for screen-level business logic.

```kotlin
class UserViewModel(
    private val repository: UserRepository
) : ViewModel() {

    fun loadUsers() {
        viewModelScope.launch {
            _users.value = repository.getUsers()
        }
    }
}
```

### `lifecycleScope`

- Tied to Activity or Fragment lifecycle.
- Cancelled when lifecycle is destroyed.
- Good for lifecycle-bound UI work.

```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            render(state)
        }
    }
}
```

### `rememberCoroutineScope`

- Tied to current Compose composition.
- Used for UI event coroutines.
- Examples:
  - Snackbar.
  - Drawer open/close.
  - Scroll animation.

```kotlin
@Composable
fun SaveButton(snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            scope.launch {
                snackbarHostState.showSnackbar("Saved")
            }
        }
    ) {
        Text("Save")
    }
}
```

### `LaunchedEffect`

- Starts coroutine tied to composable lifecycle.
- Restarts when key changes.
- Good for loading by ID or collecting one-time events.

```kotlin
LaunchedEffect(userId) {
    viewModel.loadUser(userId)
}
```

### `GlobalScope`

- App-process lifetime.
- Not lifecycle-aware.
- Can leak work after screen is gone.
- Avoid in normal Android app code.

```kotlin
GlobalScope.launch {
    delay(10_000)
    updateUI()
}
```

### Interview explanation

- "In Android, I use lifecycle-aware scopes like `viewModelScope`, `lifecycleScope`, and Compose effects. They cancel work automatically. I avoid `GlobalScope` because it creates unstructured work."
