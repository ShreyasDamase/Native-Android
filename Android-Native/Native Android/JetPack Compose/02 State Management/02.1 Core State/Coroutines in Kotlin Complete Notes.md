 
---

## What is a Coroutine?

A coroutine is a way to run **asynchronous (background) code** in a simple, readable way — without blocking the main UI thread.

Think of it like this:

```
Normal code   → does one thing at a time, waits for each step
Coroutine     → can pause, do something else, and come back later
```

---

## The Problem Coroutines Solve

**Without coroutines — blocks UI thread:**

kotlin

```kotlin
// ❌ BAD — freezes the screen for 5 seconds
fun loadUser() {
    Thread.sleep(5000)       // UI freezes
    val user = api.getUser() // blocks everything
    showUser(user)
}
```

**With coroutines — non blocking:**

kotlin

````kotlin
// ✅ GOOD — UI stays smooth
suspend fun loadUser() {
    delay(5000)              // pauses BUT doesn't block UI
    val user = api.getUser() // waits without freezing
    showUser(user)
}
```

---

## Real World Analogy
Imagine you order food at a restaurant:
```
Without coroutine → waiter stands at your table doing nothing until 
                    food is ready, ignoring all other customers ❌

With coroutine    → waiter takes your order, goes to other tables, 
                    comes back when your food is ready ✅
````

---

## The `suspend` Keyword

A function marked with `suspend` can be **paused and resumed** without blocking the thread. It can only be called from another suspend function or a coroutine.

kotlin

```kotlin
// normal function — cannot pause
fun getName(): String {
    return "John"
}

// suspend function — can pause and resume
suspend fun getUser(): User {
    delay(1000)          // pauses here, doesn't block
    return api.getUser() // waits for network
}
```

---

## Coroutine Builders

These are the functions that actually **start** a coroutine:

### `launch` — Fire and forget

kotlin

```kotlin
// doesn't return a result
viewModelScope.launch {
    viewModel.loadBooks() // just run this, don't need result
}
```

### `async` — Returns a result

kotlin

```kotlin
// returns a Deferred<T> — like a promise
val userDeferred = async { api.getUser() }
val user = userDeferred.await() // wait for result
```

### `runBlocking` — Blocks the thread (avoid in production)

kotlin

```kotlin
// only use in tests or main function
runBlocking {
    val user = api.getUser()
}
```

---

## Coroutine Scopes

A scope defines the **lifetime** of a coroutine — when it starts and when it gets cancelled.

### `viewModelScope`

kotlin

```kotlin
// tied to ViewModel lifecycle
// auto cancelled when ViewModel is destroyed
class MyViewModel : ViewModel() {
    fun loadData() {
        viewModelScope.launch {
            val data = repo.getData()
        }
    }
}
```

### `lifecycleScope`

kotlin

```kotlin
// tied to Activity/Fragment lifecycle
// auto cancelled when screen is destroyed
lifecycleScope.launch {
    val data = repo.getData()
}
```

### `rememberCoroutineScope` (Compose)

kotlin

```kotlin
// tied to composable lifecycle
val scope = rememberCoroutineScope()
Button(onClick = {
    scope.launch {
        viewModel.save()
    }
})
```

### `GlobalScope` (avoid)

kotlin

```kotlin
// lives as long as the app — memory leaks ❌
GlobalScope.launch {
    // dont use this
}
```

---

## Coroutine Dispatchers

Dispatchers decide **which thread** the coroutine runs on:

kotlin

```kotlin
// Main thread — UI updates
viewModelScope.launch(Dispatchers.Main) {
    textView.text = "Hello" // UI work
}

// Background thread — heavy work
viewModelScope.launch(Dispatchers.IO) {
    val data = api.getData() // network / database
}

// CPU intensive work
viewModelScope.launch(Dispatchers.Default) {
    val sorted = list.sortedBy { it.name } // heavy computation
}
```

### `withContext` — Switch dispatcher inside a coroutine

kotlin

```kotlin
viewModelScope.launch(Dispatchers.Main) {
    // on main thread

    val result = withContext(Dispatchers.IO) {
        api.getBooks() // switches to IO for network call
    }

    // back on main thread automatically
    updateUI(result)
}
```

---

## Structured Concurrency

Coroutines follow a **parent-child** relationship:

kotlin

```kotlin
viewModelScope.launch {
    // parent coroutine

    launch {
        // child 1
        api.getUser()
    }

    launch {
        // child 2
        api.getBooks()
    }

    // if parent is cancelled → both children cancelled
    // if child fails → parent is notified
}
```

---

## Error Handling

### `try-catch`

kotlin

```kotlin
viewModelScope.launch {
    try {
        val user = api.getUser()
        _user.value = user
    } catch (e: Exception) {
        _error.value = e.message
    }
}
```

### `CoroutineExceptionHandler`

kotlin

```kotlin
val handler = CoroutineExceptionHandler { _, exception ->
    Log.e("Error", exception.message ?: "Unknown error")
}

viewModelScope.launch(handler) {
    api.getUser()
}
```

---

## Parallel Execution with `async`

kotlin

```kotlin
viewModelScope.launch {
    // ❌ sequential — takes 2 seconds total
    val user = api.getUser()    // 1 second
    val books = api.getBooks()  // 1 second

    // ✅ parallel — takes 1 second total
    val userDeferred = async { api.getUser() }   // starts immediately
    val booksDeferred = async { api.getBooks() } // starts immediately

    val user = userDeferred.await()   // wait for result
    val books = booksDeferred.await() // wait for result
}
```

---

## Cancellation

kotlin

```kotlin
val job = viewModelScope.launch {
    repeat(1000) {
        delay(1000)
        println("Running $it")
    }
}

// cancel anytime
job.cancel()

// cancel after timeout
withTimeout(5000) {
    api.getUser() // throws exception if takes more than 5 seconds
}

// cancel after timeout without exception
val user = withTimeoutOrNull(5000) {
    api.getUser() // returns null if timeout
}
```

---

## Flow — Coroutine Stream

A `Flow` is like a coroutine that **emits multiple values** over time:

kotlin

```kotlin
// emit multiple values
fun getBooks(): Flow<List<Book>> = flow {
    emit(cachedBooks)       // emit cached first
    val fresh = api.getBooks()
    emit(fresh)             // then emit fresh data
}

// collect in ViewModel
viewModelScope.launch {
    repo.getBooks().collect { books ->
        _books.value = books
    }
}
```

---

## Complete Real World Example

kotlin

```kotlin
@HiltViewModel
class BookViewModel @Inject constructor(
    private val repo: BookRepository
) : ViewModel() {

    var books by mutableStateOf<List<Book>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun loadBooks() {
        viewModelScope.launch {          // start coroutine
            loading = true
            error = null

            try {
                val result = withContext(Dispatchers.IO) {  // switch to IO
                    repo.getBooks()     // network call
                }
                books = result          // back on Main, update UI
            } catch (e: Exception) {
                error = e.message
            }

            loading = false
        }
    }
}
```

---

## Master Summary Table

|Concept|What it does|
|---|---|
|`suspend`|Marks a function that can pause|
|`launch`|Start coroutine, no result|
|`async`|Start coroutine, returns result|
|`delay`|Pause without blocking|
|`withContext`|Switch thread inside coroutine|
|`viewModelScope`|Scope tied to ViewModel|
|`Dispatchers.IO`|Background thread for network/db|
|`Dispatchers.Main`|Main thread for UI|
|`Flow`|Stream of multiple values|
|`job.cancel()`|Stop a coroutine|

---

## One Line Definitions

- **Coroutine** — lightweight thread that can pause and resume without blocking
- **suspend** — function that can be paused mid-execution
- **scope** — defines when coroutine lives and dies
- **dispatcher** — decides which thread to run on
- **Flow** — coroutine that emits multiple values over time