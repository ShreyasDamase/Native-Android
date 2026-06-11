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


---

## 🚀 Mastery Deep Dive (Added 2026)

> [!NOTE]
> The following deep dive notes were generated to provide mastery-level understanding, complementing the original notes above.

# Coroutine 01 — Builders, Dispatchers & Scopes

> [!NOTE]
> Before reading this, you must understand what a coroutine IS and what `suspend` means. If you haven't, go to [[Coroutine 00 Setup Basics Suspend]] first.

---

## 🧠 Mental Model — Read This First

**Think of a construction company.**

- The **CoroutineScope** is the construction company — it defines the project lifetime. If the company shuts down, all active construction jobs stop.
- The **CoroutineContext** is the company's instruction manual — it says which tools to use (dispatcher), what the job is called (CoroutineName), who handles disasters (ExceptionHandler), and what project the work belongs to (Job).
- The **Dispatcher** is the team assignment — which crew (thread pool) handles the work.
- The **Job** is the individual work order — you can check its status, wait for it, or cancel it.

---

## 🏗️ Coroutine Builders

Builders are functions that START a coroutine. They are NOT `suspend` functions — they are regular functions that create and schedule coroutine work.

### `launch` — Fire and Forget

```kotlin
val job: Job = viewModelScope.launch {
    repository.syncData()
}
```

**Anatomy:**
```
viewModelScope  .launch  {  repository.syncData()  }
│                │            │
│                │            └── body of the coroutine — any suspend calls allowed here
│                └── builder: starts coroutine, returns Job immediately (does not wait)
└── scope: defines lifetime + context (dispatcher, Job, ExceptionHandler)
```

| Property | Value |
|---|---|
| Return type | `Job` |
| Blocks caller? | ❌ No — returns immediately, work runs in background |
| Result value? | ❌ No result — fire and forget |
| Exception handling | Propagates to the parent scope immediately |
| Use case | Any work where you don't need the result |

```kotlin
// Real app example: triggering a sync when screen opens
fun syncOnOpen() {
    viewModelScope.launch {
        _uiState.update { it.copy(isSyncing = true) }
        try {
            repository.sync()
        } finally {
            _uiState.update { it.copy(isSyncing = false) }  // always runs, even on cancellation
        }
    }
}
```

---

### `async` — For Parallel Work with a Result

```kotlin
val deferred: Deferred<User> = viewModelScope.async {
    repository.getUser()
}
val user: User = deferred.await()  // suspend here until result is ready
```

**Anatomy:**
```
val deferred = scope  .async  {  repository.getUser()  }
│              │       │          │
│              │       │          └── coroutine body
│              │       └── builder: starts coroutine, returns Deferred<T>
│              └── scope
└── Deferred<T> is like a "promise" — you can await() it later
```

| Property | Value |
|---|---|
| Return type | `Deferred<T>` |
| Blocks caller? | ❌ No — returns immediately |
| Result value? | ✅ Yes — call `.await()` to get it |
| Exception handling | Stored in `Deferred` — only thrown when `.await()` is called |
| Use case | Parallel independent work where you need results |

> [!CAUTION]
> **The #1 async mistake — looks parallel, runs sequential:**
> ```kotlin
> // ❌ SEQUENTIAL — async starts AND awaits immediately, one after another
> val users = async { api.getUsers() }.await()   // starts, waits, finishes
> val books = async { api.getBooks() }.await()   // only then starts
> // Total time = getUsers time + getBooks time
>
> // ✅ PARALLEL — both start, THEN we wait for results
> val usersDeferred = async { api.getUsers() }   // starts (returns Deferred immediately)
> val booksDeferred = async { api.getBooks() }   // starts in parallel (also returns Deferred)
> val users = usersDeferred.await()              // wait for users
> val books = booksDeferred.await()              // wait for books (likely already done!)
> // Total time ≈ max(getUsers time, getBooks time)
> ```
> This is the single most asked coroutine interview question. The fix is simple: **separate the `async` call from the `await()` call.**

```kotlin
// Real app example: loading a home screen with 3 independent data sources
suspend fun loadHome(): HomeData = coroutineScope {
    val userDeferred        = async { userRepo.getCurrentUser() }
    val feedDeferred        = async { feedRepo.getFeed() }
    val notificationsDeferred = async { notifRepo.getUnread() }

    // All three start simultaneously. We wait here.
    HomeData(
        user          = userDeferred.await(),
        feed          = feedDeferred.await(),
        notifications = notificationsDeferred.await()
    )
    // If ANY of the three fails → all are cancelled (because coroutineScope)
}
```

---

### `withContext` — Switch Dispatcher, Get a Result

```kotlin
suspend fun getUsers(): List<User> = withContext(Dispatchers.IO) {
    api.getUsers()   // runs on IO thread
}                    // returns List<User>, automatically back to caller's dispatcher
```

**Anatomy:**
```
withContext  (Dispatchers.IO)  {  api.getUsers()  }
│             │                    │
│             │                    └── block runs on the specified dispatcher
│             └── the target dispatcher (where the block will run)
└── suspends the current coroutine, runs the block, returns its result
```

| Property | Value |
|---|---|
| Return type | The result of the block (`T`) |
| Creates new coroutine? | ❌ No — reuses the same coroutine, just changes dispatcher |
| Parallel? | ❌ No — sequential, suspends caller until done |
| Use case | Switching dispatcher for a specific block of work |

**`withContext` vs `async`:**
```kotlin
// withContext: sequential, same coroutine, different thread
val user = withContext(Dispatchers.IO) { api.getUser() }   // waits here
val books = withContext(Dispatchers.IO) { api.getBooks() } // then waits here

// async: parallel, new coroutine, can run simultaneously
val userDeferred = async { api.getUser() }
val booksDeferred = async { api.getBooks() }
val user = userDeferred.await()
val books = booksDeferred.await()
```

---

### `runBlocking` — Only for Tests and `main()`

```kotlin
fun main() = runBlocking {         // blocks the current thread until block completes
    val users = repository.getUsers()
    println(users)
}
```

> [!CAUTION]
> **Never use `runBlocking` inside Android UI code.** It defeats the entire purpose of coroutines by blocking the calling thread. Its ONLY valid uses:
> 1. The `main()` function of a Kotlin script/JVM app
> 2. In unit tests as a last resort (prefer `runTest` instead)

---

## 🧭 Dispatchers — Which Thread Pool Runs Your Code

A **Dispatcher** is the part of `CoroutineContext` that decides which thread (or thread pool) runs the coroutine code.

### `Dispatchers.Main`

```kotlin
// Used for: all UI updates, reading UI state, collecting StateFlow in Composables
viewModelScope.launch {               // default context for viewModelScope is Main
    _uiText.value = "Hello"           // safe: on Main thread
}

// Or explicitly:
viewModelScope.launch(Dispatchers.Main) {
    updateUI()
}
```

**Internals:**
- Backed by Android's `Looper.getMainLooper()`.
- There is exactly **ONE** Main thread in any Android process.
- Do NOT run any blocking or CPU-heavy work here.
- `Dispatchers.Main.immediate` — if already on Main, runs immediately without posting to the message queue (optimization).

---

### `Dispatchers.IO`

```kotlin
// Used for: network, database, file reading/writing, shared preferences, anything that "waits"
suspend fun fetchUsers(): List<User> = withContext(Dispatchers.IO) {
    userApi.getUsers()   // Retrofit call — blocks this IO thread, but Main is free
}
```

**Internals:**
- Backed by a **shared elastic thread pool**.
- Default size: **64 threads** (or CPU core count if higher).
- The pool can **grow beyond 64** if all threads are blocked (it's elastic, not hard-capped by default).
- This means 64 simultaneous blocking IO operations can run at the same time before the pool starts queueing.

```kotlin
// Example: fetch 100 users in parallel on IO
val deferreds = (1..100).map { id ->
    async(Dispatchers.IO) { userApi.getUserById(id) }
}
val users = deferreds.awaitAll()
// All 100 fire simultaneously — IO pool handles the thread management
```

> [!TIP]
> Retrofit and Room are already internally dispatched correctly, so you often only need `withContext(Dispatchers.IO)` in your own Repository layer, not inside every individual call.

---

### `Dispatchers.Default`

```kotlin
// Used for: CPU-intensive work — sorting, JSON parsing, image processing, encryption
suspend fun sortUsers(users: List<User>): List<User> = withContext(Dispatchers.Default) {
    users.sortedWith(compareBy({ it.lastName }, { it.firstName }))
}

suspend fun parseJson(json: String): List<Product> = withContext(Dispatchers.Default) {
    Json.decodeFromString<List<Product>>(json)
}
```

**Internals:**
- Backed by a thread pool of size **equal to the number of CPU cores** on the device.
- On your Redmi Note 11 Pro (8 cores) → 8 threads maximum.
- Why? CPU-bound work cannot run faster than the number of physical cores. Adding more threads causes context switching overhead with zero benefit.

| Dispatcher | Thread Pool | Thread Count | For |
|---|---|---|---|
| `Main` | Android Main Looper | 1 | UI updates |
| `IO` | Elastic shared pool | 64+ (elastic) | Network, DB, File |
| `Default` | Fixed pool | CPU core count | Sorting, Parsing, Math |
| `Unconfined` | None | Runs on caller's thread | Advanced/special cases only |

---

### `Dispatchers.Unconfined` — Avoid Unless You Know Exactly Why

```kotlin
// Starts on current thread, resumes on whatever thread the suspension resumes on
launch(Dispatchers.Unconfined) {
    println("Before: ${Thread.currentThread().name}")   // Main
    delay(100)
    println("After: ${Thread.currentThread().name}")    // DefaultExecutor (may be different!)
}
```

> [!WARNING]
> `Dispatchers.Unconfined` breaks the predictability of which thread your code runs on. In Android, any UI operation after a suspension point could run on a background thread, causing crashes. **Do not use in Android app code.** It exists for advanced coroutine infrastructure code.

---

## 🔑 CoroutineContext — The Instruction Manual

Every coroutine carries a `CoroutineContext`. Think of it as a **typed map** (key → value). Each element type is its own key.

```kotlin
// CoroutineContext is composed of elements using the + operator
val context: CoroutineContext =
    Dispatchers.IO +                           // which thread pool
    CoroutineName("UserSync") +                // name for debugging
    SupervisorJob() +                          // job type (failure isolation)
    CoroutineExceptionHandler { _, e ->        // what happens on uncaught exceptions
        Log.e("Coroutine", "Unhandled", e)
    }

val scope = CoroutineScope(context)
scope.launch { doWork() }
```

**The elements:**

| Element | Type | Purpose |
|---|---|---|
| `Job` / `SupervisorJob` | `Job` | The work order — tracks state, can be cancelled |
| `Dispatchers.IO` etc | `CoroutineDispatcher` | Decides which thread |
| `CoroutineName("x")` | `CoroutineName` | Labeling for debugging/logging |
| `CoroutineExceptionHandler` | `CoroutineExceptionHandler` | Catches uncaught exceptions in `launch` |

**Inheritance:**
When a child coroutine is launched, it inherits the parent's context. The child can OVERRIDE specific elements:

```kotlin
viewModelScope.launch {                          // context: Main + Job + ...
    launch(Dispatchers.IO) {                     // context: IO (overrides Main) + child Job + ...
        // runs on IO thread
    }
    // back on Main here
}
```

---

## 🔁 Job — The Work Order

`Job` is what `launch` returns. It represents a single unit of work.

### Job Lifecycle States

```
         ┌─────────┐
         │   New   │  (created but not started — only with CoroutineStart.LAZY)
         └────┬────┘
              │ start
         ┌────▼────┐
         │ Active  │  (running normally)
         └────┬────┘
     complete │  cancel/fail
    ┌─────────┴──────────┐
┌───▼──────┐        ┌────▼──────┐
│Completing│        │Cancelling │
└───┬──────┘        └────┬──────┘
    │ all children done  │ all children done
┌───▼──────┐        ┌────▼──────┐
│Completed │        │Cancelled  │
└──────────┘        └───────────┘
```

```kotlin
val job = viewModelScope.launch {
    delay(5000)
    println("Done")
}

println(job.isActive)      // true (while running)
println(job.isCancelled)   // false
println(job.isCompleted)   // false

job.cancel()               // request cancellation

println(job.isCancelled)   // true
println(job.isCompleted)   // true (cancelled jobs are also "completed")

job.join()                 // suspend until the job is done (useful in tests)
```

### `Job` vs `SupervisorJob` — Failure Propagation

```
Regular Job (default):           SupervisorJob:

   Parent Job                       Parent SupervisorJob
   /        \                        /              \
Child A    Child B                Child A          Child B
  ❌ fails                           ❌ fails
   │                                  │
   ▼                                  ▼
Child B gets cancelled            Child B UNAFFECTED ✅
Parent gets cancelled             Parent UNAFFECTED ✅
```

```kotlin
// Default Job — all-or-nothing
val scope = CoroutineScope(Job() + Dispatchers.IO)
scope.launch {
    val a = async { riskyOperationA() }   // if this fails...
    val b = async { riskyOperationB() }   // ...this gets cancelled too
    a.await()
    b.await()
}

// SupervisorJob — independent failures
val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
scope.launch {
    supervisorScope {
        val a = async { riskyOperationA() }   // if this fails...
        val b = async { riskyOperationB() }   // ...this CONTINUES running
        val resultA = runCatching { a.await() }
        val resultB = runCatching { b.await() }
        // handle both results independently
    }
}
```

> [!IMPORTANT]
> `viewModelScope` uses `SupervisorJob` internally. This means one failed `launch` inside a ViewModel will NOT cancel other `launch` calls. This is intentional — a failed analytics call should not cancel a data fetch.

---

## 🏛️ Lifecycle-Aware Scopes in Android

### `viewModelScope` — Business Logic in ViewModel

```kotlin
class ProductViewModel(
    private val repo: ProductRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()

    fun loadProducts() {
        viewModelScope.launch {          // ← tied to this ViewModel instance
            _products.value = repo.getProducts()
        }
    }

    // When the user navigates away → ViewModel.onCleared() is called
    // → viewModelScope.cancel() is called automatically
    // → the launch{} above is cancelled (no memory leak!)
}
```

**Internal structure:**
```kotlin
// viewModelScope is defined approximately as:
val ViewModel.viewModelScope: CoroutineScope
    get() = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
```

---

### `lifecycleScope` — UI-Level Work in Fragment/Activity

```kotlin
// In Fragment — for observing UI state and handling UI events
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    viewLifecycleOwner.lifecycleScope.launch {
        // ← always use viewLifecycleOwner (not 'this@Fragment') in Fragment
        // viewLifecycleOwner.lifecycleScope is destroyed when the Fragment VIEW is destroyed
        // 'this@Fragment.lifecycleScope' lives until the Fragment is destroyed (longer!)

        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiState.collect { state ->
                renderUI(state)
            }
        }
    }
}
```

> [!CAUTION]
> In Fragments, **always use `viewLifecycleOwner.lifecycleScope`**, not `lifecycleScope`.
> - `lifecycleScope` = tied to Fragment's lifecycle (survives view destruction during backstack)
> - `viewLifecycleOwner.lifecycleScope` = tied to Fragment's VIEW lifecycle (destroyed with the view)
> Using the wrong one can cause attempts to update a destroyed view → `IllegalStateException`.

---

### `rememberCoroutineScope` — For Compose UI Events

```kotlin
@Composable
fun ProfileScreen(snackbarHostState: SnackbarHostState) {
    // rememberCoroutineScope gives you a scope tied to this Composable's position in the tree
    // It's cancelled when this Composable leaves the composition
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            // onClick is a regular lambda, NOT a coroutine
            // We need scope.launch to start a coroutine from here
            scope.launch {
                snackbarHostState.showSnackbar("Profile saved!")
            }
        }
    ) {
        Text("Save Profile")
    }
}
```

**Why `rememberCoroutineScope` instead of `LaunchedEffect`?**
- `LaunchedEffect` is for work that should start automatically when composition happens
- `rememberCoroutineScope().launch {}` is for work that starts on USER INTERACTION (button clicks, etc.)

---

### `LaunchedEffect` — Auto-Starting Side Effects in Compose

```kotlin
@Composable
fun UserScreen(userId: String) {
    // LaunchedEffect runs the block when the composable enters composition
    // If 'userId' changes, the previous coroutine is CANCELLED and a new one starts
    LaunchedEffect(userId) {
        viewModel.loadUser(userId)   // suspend call — safe here
    }
}
```

**Key rules for `LaunchedEffect`:**
1. The key (`userId` above) determines when to restart
2. `LaunchedEffect(Unit)` — runs once when composition enters, never restarts
3. `LaunchedEffect(value1, value2)` — restarts whenever either value changes
4. The coroutine is cancelled when the Composable leaves composition

---

### `GlobalScope` — The Anti-Pattern

```kotlin
// ❌ NEVER DO THIS in Android app code
GlobalScope.launch {
    delay(30_000)      // 30 second timer
    sendAnalytics()    // By now, user may have closed the app
                       // But this coroutine is still alive!
                       // GlobalScope lives as long as the JVM process
}
```

**Why GlobalScope is dangerous:**
- Not tied to any lifecycle → work keeps running after the user leaves the screen
- No structured concurrency → exceptions go nowhere
- Memory leaks → the coroutine can keep references to ViewModels, Fragments, etc.
- **Use `viewModelScope`, `lifecycleScope`, or a custom `CoroutineScope(SupervisorJob())` with explicit lifecycle management instead.**

---

## 📊 Scope Comparison Table

| Scope | Lifecycle | Cancelled When | Default Dispatcher | Use For |
|---|---|---|---|---|
| `viewModelScope` | ViewModel | ViewModel.onCleared() | Main | Business logic, data fetching |
| `viewLifecycleOwner.lifecycleScope` | Fragment View | View destroyed | Main | UI events, Flow collection |
| `lifecycleScope` | Activity/Fragment | onDestroy() | Main | Activity-level work |
| `rememberCoroutineScope()` | Composition | Leaves composition | Inherited | User interaction in Compose |
| `LaunchedEffect` | Composition | Key changes or leaves | Inherited | Auto-starting effects in Compose |
| `CoroutineScope(SupervisorJob())` | Manual | scope.cancel() | Manual | App-level services, DI singletons |
| `GlobalScope` | Process | Process death | Default | ❌ Avoid |

---

## ✅ Real App Patterns

### Pattern: Cancel previous search on new keystroke

```kotlin
class SearchViewModel(private val repo: SearchRepository) : ViewModel() {

    private val _results = MutableStateFlow<List<Result>>(emptyList())
    val results = _results.asStateFlow()

    private var searchJob: Job? = null   // track the running search

    fun onSearchQuery(query: String) {
        searchJob?.cancel()              // cancel old search coroutine

        searchJob = viewModelScope.launch {
            delay(300)                    // debounce: wait 300ms before searching
            _results.value = repo.search(query)
        }
    }
}
```

### Pattern: Parallel loading with individual error handling

```kotlin
fun loadDashboard() {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        supervisorScope {
            val usersJob = async { runCatching { userRepo.getUsers() } }
            val statsJob = async { runCatching { statsRepo.getStats() } }

            val users = usersJob.await().getOrNull() ?: emptyList()
            val stats = statsJob.await().getOrNull() ?: Stats.empty()

            _uiState.update {
                it.copy(isLoading = false, users = users, stats = stats)
            }
        }
    }
}
```

---

## ❌ Common Mistakes

### Mistake 1: `async` without a scope (structured concurrency violation)

```kotlin
// ❌ BAD — async without coroutineScope/supervisorScope is unstructured
suspend fun loadData(): Data {
    val a = viewModelScope.async { api.getA() }   // attaches to viewModelScope, not to this function
    val b = viewModelScope.async { api.getB() }
    return Data(a.await(), b.await())
}

// ✅ GOOD — use coroutineScope to make async calls structured
suspend fun loadData(): Data = coroutineScope {
    val a = async { api.getA() }   // child of coroutineScope
    val b = async { api.getB() }
    Data(a.await(), b.await())
}
```

### Mistake 2: Forgetting `viewLifecycleOwner` in Fragments

```kotlin
// ❌ Memory leak: this scope outlives the view, may try to update a destroyed view
lifecycleScope.launch {
    viewModel.uiState.collect { render(it) }
}

// ✅ Correct: scoped to the view's lifecycle
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { render(it) }
    }
}
```

### Mistake 3: Launching coroutines inside `remember {}` in Compose

```kotlin
// ❌ WRONG — remember lambda is not a coroutine, and this creates unstructured work
val data by remember { mutableStateOf(launch { fetchData() }) }  // doesn't even compile right

// ✅ CORRECT — use LaunchedEffect for side effects
LaunchedEffect(Unit) {
    data = fetchData()
}
```

---

## 🔗 Connections

- **Previous**: [[Coroutine 00 Setup Basics Suspend]] — what suspend means and how Continuation works
- **Next**: [[Coroutine 02 Structured Concurrency Cancellation Exceptions]] — how cancellation and error propagation work through the Job tree
- **Scopes in practice**: see [[Flow with Compose]] for `LaunchedEffect` and `collectAsStateWithLifecycle`

---

## 💬 Interview Master Q&A

**Q: What is the difference between `launch` and `async`?**
> `launch` starts a coroutine and returns a `Job`. It's used for fire-and-forget work where no result is needed. Exceptions propagate to the parent immediately. `async` starts a coroutine and returns a `Deferred<T>`. It's used when you need a result from parallel work. Exceptions are stored in the `Deferred` and only thrown when `.await()` is called. Both are non-blocking — they return their result (Job or Deferred) immediately without waiting for the coroutine to complete.

**Q: What is the most common mistake with `async`?**
> Chaining `.await()` immediately after `async` instead of separating them. `async { }.await()` is sequential — it starts the coroutine and immediately waits for it, blocking any subsequent coroutine from starting. The correct pattern is: start all the `async` coroutines first (storing their `Deferred`), then call `.await()` on all of them. This allows them to run in parallel.

**Q: What are the differences between `Dispatchers.IO` and `Dispatchers.Default`?**
> `Dispatchers.IO` is designed for blocking IO operations — it has a large elastic thread pool (64+ threads) because these threads spend most of their time waiting for network or disk responses. `Dispatchers.Default` is designed for CPU-bound computation — it has a thread pool equal to the number of CPU cores because adding more threads than cores would cause overhead with no speed benefit. Use IO for Retrofit calls, Room queries, and file operations; use Default for sorting, parsing, or heavy calculations.

**Q: What is `CoroutineContext`?**
> `CoroutineContext` is a typed map that every coroutine carries. Each element in it has a unique type key. It can contain a `Job` (defining the coroutine's lifecycle and parent-child relationship), a `CoroutineDispatcher` (deciding which thread it runs on), a `CoroutineName` (for debugging), and a `CoroutineExceptionHandler` (for handling uncaught exceptions). Contexts are combined with the `+` operator, and child coroutines inherit the parent's context while being able to override specific elements.

**Q: Why do we use `viewLifecycleOwner.lifecycleScope` instead of `lifecycleScope` in Fragments?**
> A Fragment has two distinct lifecycles: the Fragment itself and its view. When a Fragment goes on the backstack, the view is destroyed but the Fragment stays alive. If we used `lifecycleScope` (tied to the Fragment), coroutines would keep running and might try to update a view that no longer exists, causing `IllegalStateException`. `viewLifecycleOwner.lifecycleScope` is tied to the view lifecycle and is automatically cancelled when the view is destroyed, preventing these crashes.
