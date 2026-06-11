# Coroutine 00 Setup Basics Suspend

This note covers setup, first coroutine, coroutine meaning, blocking vs suspending, and `suspend` functions.

---

## Setup

### Dependencies

- `kotlinx-coroutines-core`
  - Core coroutine APIs.
  - Provides `launch`, `async`, `delay`, `coroutineScope`, Flow, Channel, etc.
- `kotlinx-coroutines-android`
  - Android integration.
  - Provides `Dispatchers.Main`.
- `lifecycle-viewmodel-ktx`
  - Provides `viewModelScope`.
- `lifecycle-runtime-ktx`
  - Provides `lifecycleScope`.
- `kotlinx-coroutines-test`
  - Used for coroutine unit tests.

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
```

### First coroutine

```kotlin
viewModelScope.launch {
    delay(1000)
    _uiText.value = "Loaded"
}
```

### Interview explanation

- Coroutines need library support from `kotlinx.coroutines`.
- Android apps need `kotlinx-coroutines-android` for `Dispatchers.Main`.
- `viewModelScope.launch { }` is a common first coroutine in Android.

---

## What Is a Coroutine?

### Definition

- A coroutine is a lightweight asynchronous task.
- It can suspend and resume.
- It does not block the underlying thread when suspended.
- It runs on real threads through dispatchers.
- Many coroutines can share a small number of threads.

### Coroutine is not a thread

- Thread:
  - OS-level execution resource.
  - Expensive compared to coroutine.
  - Blocking a thread wastes that resource.
- Coroutine:
  - Kotlin-level async task.
  - Lightweight.
  - Can suspend and release the thread.

### Real-life example

- Thread blocking:
  - A waiter takes an order and stands still until food is ready.
  - Other customers are ignored.
- Coroutine suspending:
  - A waiter takes an order and lets the kitchen cook.
  - The waiter serves other tables.
  - The waiter comes back when food is ready.

### Android example

- Network call waits for server response.
- Main thread stays free for UI drawing and touch events.
- Coroutine resumes when response arrives.

### Interview explanation

- "A coroutine is a lightweight async task. It can suspend without blocking a thread, so Android can keep the UI responsive during network, database, or long-running work."

---

## Blocking vs Suspending

### Blocking

- Blocks the whole thread.
- Main thread blocking causes UI freeze.
- Long main-thread blocking can cause ANR.

```kotlin
fun badExample() {
    Thread.sleep(5000)
    showUser()
}
```

### Suspending

- Pauses only the coroutine.
- Frees the thread for other work.
- Coroutine resumes later from the same point.

```kotlin
suspend fun goodExample() {
    delay(5000)
    showUser()
}
```

### `Thread.sleep()` vs `delay()`

| Topic | `Thread.sleep()` | `delay()` |
|---|---|---|
| Behavior | Blocks thread | Suspends coroutine |
| Thread usage | Thread is occupied | Thread is released |
| Android UI | Can freeze UI | Does not freeze by itself |
| Cancellation | Not coroutine-friendly | Cancellation-friendly |

### Interview explanation

- "`Thread.sleep()` blocks the thread. `delay()` suspends the coroutine and lets the thread do other work."

---

## The `suspend` Keyword

### Meaning

- `suspend` marks a function that can pause and resume.
- A suspend function can call other suspend functions.
- A normal function cannot directly call a suspend function.
- It must be called from another suspend function or from a coroutine builder.

```kotlin
suspend fun getUser(): User {
    delay(1000)
    return api.getUser()
}
```

### What `suspend` does not mean

- It does not automatically create a background thread.
- It does not automatically make work parallel.
- It does not automatically handle exceptions.
- It only allows suspension.

### Bad CPU example

```kotlin
suspend fun badCpuWork(): Int {
    var sum = 0
    for (i in 1..100_000_000) {
        sum += i
    }
    return sum
}
```

- This is suspend, but still CPU-heavy.
- If called on Main, it can freeze UI.

### Correct CPU example

```kotlin
suspend fun goodCpuWork(): Int = withContext(Dispatchers.Default) {
    var sum = 0
    for (i in 1..100_000_000) {
        sum += i
    }
    sum
}
```

### Interview explanation

- "`suspend` means the function can suspend and resume. It does not mean the function automatically runs on a background thread."


---

## 🚀 Mastery Deep Dive (Added 2026)

> [!NOTE]
> The following deep dive notes were generated to provide mastery-level understanding, complementing the original notes above.

# Coroutine 00 — Setup, Basics & The `suspend` Keyword

> [!NOTE]
> This is the foundation. Every other coroutine file builds on this. Read this slowly. The "Under the Hood" section will change how you think about async code forever.

---

## 🧠 Mental Model — Read This First

**Think of a waiter at a busy restaurant.**

A **blocking** waiter takes your order, walks to the kitchen, and STANDS THERE waiting for food. No other customer is served. The whole restaurant (your thread) is frozen.

A **coroutine** waiter takes your order, gives it to the kitchen, and immediately walks to the NEXT table. When the kitchen calls "order up!", that waiter picks it back up exactly where they left off — table number, order details, everything.

The "bookmark" that the waiter mentally places — "I was at table 7, waiting for pasta" — is called a **Continuation**. That's the actual object the Kotlin compiler creates under the hood.

---

## 📦 Setup

### Dependencies Explained

```kotlin
dependencies {
    // Core coroutine APIs — provides launch, async, delay, Flow, Channel, etc.
    // This is the brain. Without this, nothing works.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // Android integration — provides Dispatchers.Main (the Android UI thread dispatcher)
    // Without this, Dispatchers.Main throws an error on Android.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Provides viewModelScope — the coroutine scope tied to ViewModel lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")

    // Provides lifecycleScope and repeatOnLifecycle — tied to Activity/Fragment lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Test utilities: runTest, StandardTestDispatcher, advanceUntilIdle
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
```

> [!TIP]
> Always keep `coroutines-core` and `coroutines-android` on the SAME version. Version mismatches cause subtle runtime errors that are very hard to debug.

### Your First Coroutine (What the Code Says vs What it Means)

```kotlin
viewModelScope.launch {   // "Inside the ViewModel's safe area, START a coroutine"
    delay(1000)            // "Pause THIS coroutine for 1 second (thread is free to do other things)"
    _uiText.value = "Loaded"  // "Resume here and update UI"
}
```

| Part | What it is | Why it's there |
|---|---|---|
| `viewModelScope` | A `CoroutineScope` tied to ViewModel | Ensures the coroutine is cancelled when ViewModel is cleared, preventing leaks |
| `.launch { }` | A coroutine builder | Starts a new coroutine. Returns a `Job` (a handle to cancel/track it). Does NOT block. |
| `delay(1000)` | A `suspend` function | Pauses the coroutine (not the thread) for 1000ms. Thread goes to serve other coroutines. |

---

## 🔬 Coroutine vs Thread — The Core Distinction

### Thread (the old way)
- **OS-level** resource. Expensive to create (~1MB stack).
- When you call `Thread.sleep(5000)`, the **entire OS thread** is frozen. No other work can use it.
- You can have maybe 100–200 active threads before the OS struggles.

### Coroutine (the new way)
- **Kotlin-level** lightweight task. Costs ~a few hundred bytes.
- When you call `delay(5000)`, only **the coroutine** is paused. The underlying thread is returned to a thread pool and can run 1000s of OTHER coroutines.
- You can have **100,000+ active coroutines** without running out of memory.

```kotlin
// ❌ OLD WAY — Blocks the Main thread. UI freezes. ANR in 5 seconds.
fun loadData() {
    Thread.sleep(5000)      // Thread completely frozen
    showResult()
}

// ✅ COROUTINE WAY — Only the coroutine pauses. UI stays alive.
fun loadData() {
    viewModelScope.launch {
        delay(5000)          // Coroutine pauses, thread is free
        showResult()         // Resumes here after 5 seconds
    }
}
```

### `Thread.sleep()` vs `delay()` — Side by Side

| Dimension | `Thread.sleep(ms)` | `delay(ms)` |
|---|---|---|
| What pauses? | The whole OS thread | Only this coroutine |
| Other work on same thread? | ❌ Blocked | ✅ Runs freely |
| Android UI during wait? | ❌ Frozen | ✅ Responsive |
| Cancellation aware? | ❌ No | ✅ Yes — instantly stops if coroutine is cancelled |
| Type | Regular function | `suspend` function |

---

## 🔑 The `suspend` Keyword — Anatomy

### What `suspend` ACTUALLY means (not the textbook definition)

```kotlin
suspend fun getUser(): User {
    delay(1000)
    return api.fetchUser()
}
```

`suspend` is a **compiler contract** that says:
> "This function is allowed to pause execution at certain points (called **suspension points**) and resume later, WITHOUT blocking the thread it runs on."

### What `suspend` does NOT mean (myths to kill)

| Myth | Truth |
|---|---|
| "`suspend` automatically runs on a background thread" | ❌ WRONG. `suspend` alone does NOT change the thread. A `suspend` function called from Main still runs on Main. |
| "`suspend` means async/parallel" | ❌ WRONG. Two sequential `suspend` calls are still sequential. |
| "`suspend` handles errors automatically" | ❌ WRONG. You still need try/catch. |
| "`suspend` is only for IO/network" | ❌ WRONG. `delay()`, `awaitAll()`, and many UI functions are `suspend` too. |

### The Myth Proved with Code

```kotlin
// This is suspend but STILL FREEZES THE UI if called on Main!
suspend fun badCpuWork(): Int {
    var sum = 0
    for (i in 1..100_000_000) {  // 100 million iterations on Main thread = ANR
        sum += i
    }
    return sum   // NO suspension point here — thread is blocked the whole time
}

// ✅ CORRECT — uses withContext to move to Default dispatcher
suspend fun goodCpuWork(): Int = withContext(Dispatchers.Default) {
    var sum = 0
    for (i in 1..100_000_000) {
        sum += i
    }
    sum   // last expression is the return value (Kotlin idiom)
}
```

> [!IMPORTANT]
> The `suspend` modifier only ALLOWS a function to be paused. The actual THREAD switch is controlled by the **dispatcher**. `suspend` alone does nothing without a suspension point (like `delay`, `withContext`, `await`, etc.).

### Rules for calling `suspend` functions

```kotlin
// A suspend function can only be called from:
// 1. Another suspend function
suspend fun parent() {
    val user = getUser()  // ✅ OK — both are suspend
}

// 2. A coroutine builder (launch, async, runBlocking)
viewModelScope.launch {
    val user = getUser()  // ✅ OK — inside a coroutine
}

// ❌ Cannot call from a regular function directly
fun regularFunction() {
    val user = getUser()  // ❌ COMPILE ERROR: "Suspend function can only be called from a coroutine"
}
```

---

## ⚙️ Under the Hood — What the Compiler Actually Does

> [!NOTE]
> This section explains the magic. Understanding this makes you dangerous in interviews and helps you debug async bugs that no one else can figure out.

### Continuation Passing Style (CPS)

When the Kotlin compiler sees a `suspend` function, it transforms it using **Continuation Passing Style (CPS)**. It adds a hidden parameter called `Continuation<T>` to every `suspend` function.

**The `Continuation<T>` interface:**
```kotlin
// This is what the Kotlin stdlib defines
interface Continuation<in T> {
    val context: CoroutineContext  // metadata: which dispatcher, which Job, etc.
    fun resumeWith(result: Result<T>)  // called to resume the coroutine with a value OR an exception
}
```

Think of `Continuation` as the **return address** — like a piece of paper with the instruction "when you're done, call ME back with the result."

### The State Machine a `suspend` Function Becomes

Your code:
```kotlin
suspend fun loginAndLoadProfile(email: String, password: String): Profile {
    val token = authApi.login(email, password)   // suspension point 1
    val profile = profileApi.getProfile(token)   // suspension point 2
    return profile
}
```

What the compiler ACTUALLY generates (simplified for clarity):
```kotlin
// The compiler adds a 'completion: Continuation<Profile>' parameter
fun loginAndLoadProfile(
    email: String,
    password: String,
    completion: Continuation<Profile>   // ← hidden parameter added by compiler
): Any {                                // ← return type becomes Any (could be result or COROUTINE_SUSPENDED)

    // The compiler creates a state machine using a class
    class LoginStateMachine(completion: Continuation<Profile>) : ContinuationImpl(completion) {
        var label = 0       // which step are we at?
        var token: Token? = null   // stored intermediate result

        override fun invokeSuspend(result: Result<Any?>): Any? {
            return loginAndLoadProfile(email, password, this)  // re-enter with the same state machine
        }
    }

    val sm = completion as? LoginStateMachine ?: LoginStateMachine(completion)

    when (sm.label) {
        0 -> {
            sm.label = 1
            // Call authApi.login. If it suspends, return COROUTINE_SUSPENDED immediately.
            val result = authApi.login(email, password, sm)
            if (result == COROUTINE_SUSPENDED) return COROUTINE_SUSPENDED
            sm.token = result as Token
            // Fall through to label 1 if login completed synchronously
        }
        1 -> {
            sm.token = sm.result.getOrThrow() as Token
            sm.label = 2
            val result = profileApi.getProfile(sm.token!!, sm)
            if (result == COROUTINE_SUSPENDED) return COROUTINE_SUSPENDED
        }
        2 -> {
            val profile = sm.result.getOrThrow() as Profile
            completion.resumeWith(Result.success(profile))  // done!
            return Unit
        }
    }
}
```

### What this means in plain English

1. Every `suspend` function becomes a **state machine** with numbered labels (0, 1, 2...).
2. Each label represents a position AFTER a suspension point.
3. When the coroutine suspends, it stores its current label and any local variables inside the `Continuation` object.
4. When it resumes, it jumps back to the correct label and picks up where it left off.
5. The `Continuation` object IS the "bookmark" the waiter places on the table.

### Why this means coroutines are cheap

A thread's state is stored in OS memory (stack, registers) — expensive to save/restore (context switching costs).

A coroutine's state is stored in a **heap-allocated `Continuation` object** — just a tiny Kotlin object. Switching between 100,000 coroutines means simply calling different `resumeWith()` methods. The OS never needs to know.

---

## 🔴 Blocking vs Suspending — The Line You Must Never Cross

### What "blocking the Main thread" means in Android

Android's Main (UI) thread runs at 60fps. That means it has **16 milliseconds** to complete ONE frame. If you block it for longer:
- Animations stutter → bad UX
- After 5 seconds → Android kills the app with an **ANR (Application Not Responding)** dialog

```kotlin
// ❌ FORBIDDEN on Main thread
fun badNetworkCall() {
    val response = URL("https://api.example.com").readText()  // blocks thread for 1-5+ seconds
    updateUI(response)  // by now, ANR may have fired
}

// ✅ CORRECT — coroutine frees the thread during the wait
fun goodNetworkCall() {
    viewModelScope.launch {                         // coroutine on Main by default
        val response = withContext(Dispatchers.IO) { // switch to IO thread for blocking work
            URL("https://api.example.com").readText()
        }                                            // automatically switches back to Main
        updateUI(response)                           // runs on Main — safe to update UI
    }
}
```

### The Three "Blocking" Culprits to Always Move Off Main

```kotlin
viewModelScope.launch {
    // 1. Network calls
    val data = withContext(Dispatchers.IO) { retrofit.getData() }

    // 2. Database operations
    val users = withContext(Dispatchers.IO) { dao.getAllUsers() }

    // 3. File I/O
    val content = withContext(Dispatchers.IO) { file.readText() }

    // After withContext, you're automatically back on Main — safe to touch UI
    _uiState.value = UiState.Success(data)
}
```

---

## ✅ Real App Patterns

### Pattern 1: ViewModel making a network call (most common pattern you'll write)

```kotlin
class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {             // [1] Start coroutine on Main dispatcher
            _uiState.value = UserUiState.Loading
            try {
                val user = repository.getUser()   // [2] suspend function — coroutine pauses here
                _uiState.value = UserUiState.Success(user)  // [3] resumes here on Main — safe
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

// The repository moves work to IO:
class UserRepository(private val api: UserApi) {
    suspend fun getUser(): User = withContext(Dispatchers.IO) {  // moves to IO thread
        api.getUser()   // Retrofit call — runs on IO, not Main
    }               // automatically returns to caller's dispatcher (Main)
}
```

### Pattern 2: Loading data once when a composable appears (Compose)

```kotlin
@Composable
fun UserScreen(userId: String, viewModel: UserViewModel = viewModel()) {
    // LaunchedEffect starts a coroutine tied to this composable's lifecycle
    // Restarts if userId changes
    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState) {
        is UserUiState.Loading  -> CircularProgressIndicator()
        is UserUiState.Success  -> UserContent((uiState as UserUiState.Success).user)
        is UserUiState.Error    -> ErrorView((uiState as UserUiState.Error).message)
    }
}
```

---

## ❌ Common Mistakes & Why They Break

### Mistake 1: Forgetting `suspend` doesn't mean background thread

```kotlin
// ❌ WRONG — this runs on Main even though it's suspend, ANR risk
suspend fun processImages(images: List<Bitmap>): List<Bitmap> {
    return images.map { heavyFilter(it) }  // heavy CPU on Main!
}

// ✅ CORRECT
suspend fun processImages(images: List<Bitmap>): List<Bitmap> =
    withContext(Dispatchers.Default) {
        images.map { heavyFilter(it) }  // CPU work on Default thread pool
    }
```

### Mistake 2: Calling `suspend` from a regular click listener

```kotlin
// ❌ COMPILE ERROR
binding.saveButton.setOnClickListener {
    saveData()  // Suspend function 'saveData' can only be called from a coroutine body or another suspend function
}

// ✅ CORRECT — start a coroutine first
binding.saveButton.setOnClickListener {
    viewLifecycleOwner.lifecycleScope.launch {
        saveData()  // Now inside a coroutine — OK!
    }
}

// ✅ ALSO CORRECT in Compose
Button(onClick = {
    scope.launch { saveData() }  // 'scope' from rememberCoroutineScope()
})
```

### Mistake 3: Using `runBlocking` in Android UI code

```kotlin
// ❌ DISASTER — runBlocking blocks the calling thread completely
class MyViewModel : ViewModel() {
    fun loadData() {
        runBlocking {          // This blocks viewModelScope's Main thread!
            repository.getUser()  // If this takes 3s, the whole UI is frozen for 3s
        }
    }
}

// ✅ CORRECT — use launch, never runBlocking in UI layer
class MyViewModel : ViewModel() {
    fun loadData() {
        viewModelScope.launch {    // Starts a coroutine, does NOT block
            repository.getUser()
        }
    }
}
```

> [!WARNING]
> `runBlocking` has exactly ONE valid use case in Android: in unit tests where you need to bridge between blocking test code and suspend functions. Even then, `runTest` is preferred.

### Mistake 4: Infinite `suspend` without a suspension point

```kotlin
// ❌ This function NEVER suspends even though it's marked suspend.
// Any coroutine running this will monopolize the thread until it finishes.
suspend fun countToMillion(): Int {
    var count = 0
    while (count < 1_000_000) {
        count++  // No suspension point here — never yields
    }
    return count
}

// ✅ Add yield() to periodically give other coroutines a chance to run
suspend fun countToMillion(): Int = withContext(Dispatchers.Default) {
    var count = 0
    while (count < 1_000_000) {
        count++
        if (count % 10_000 == 0) yield()  // Suspension point — also checks cancellation
    }
    count
}
```

---

## 🔗 Connections

- **Next**: [[Coroutine 01 Builders Dispatchers Scopes]] — now that you know what a coroutine IS, learn how to start one correctly
- **Dispatchers**: explained fully in [[Coroutine 01 Builders Dispatchers Scopes]]
- **Flow** uses the same suspend/continuation machinery: [[Android Jetpack Flow Study Notes]]
- **Compose integration**: `LaunchedEffect`, `rememberCoroutineScope` in [[Flow with Compose]]

---

## 💬 Interview Master Q&A

**Q: What is a coroutine?**
> A coroutine is a lightweight asynchronous task that can suspend and resume without blocking the underlying OS thread. Unlike threads which cost ~1MB each, coroutines store their state in a heap-allocated `Continuation` object, letting you run hundreds of thousands of them simultaneously. The Kotlin compiler transforms every `suspend` function into a state machine with numbered labels, and resumes it by jumping to the correct label when the awaited operation completes.

**Q: What does `suspend` actually mean?**
> `suspend` is a compiler keyword that marks a function as being capable of pausing at defined suspension points (like `delay`, `await`, `withContext`). It does NOT automatically move work to a background thread — the dispatcher controls that. `suspend` only means the function is allowed to pause without blocking the thread it's running on. If a suspend function has no actual suspension points inside it, it runs synchronously on whatever thread calls it.

**Q: What is the difference between `Thread.sleep()` and `delay()`?**
> `Thread.sleep(ms)` completely blocks the OS thread — no other work can happen on that thread during the sleep. `delay(ms)` suspends only the coroutine, returning the thread to the thread pool so it can run other coroutines. Additionally, `delay()` is cancellation-aware — if the coroutine is cancelled during a `delay`, it immediately throws `CancellationException` and stops. `Thread.sleep()` cannot be interrupted by coroutine cancellation.

**Q: How does the Kotlin compiler implement `suspend`?**
> The compiler uses **Continuation Passing Style (CPS)**. It adds a hidden `Continuation<T>` parameter to every suspend function and transforms the function body into a state machine. Each suspension point becomes a new state label. When the function suspends, its local variables and current label are stored in the `Continuation` object on the heap. When resumed, the function re-enters the state machine at the saved label and reads back its local variables from the `Continuation`. This is why coroutines are cheap — the "context switch" is just a method call on a heap object, not an OS-level operation.

**Q: Can a `suspend` function freeze the UI?**
> Yes, absolutely. A `suspend` function with no actual suspension points, or one that does heavy CPU work without using `withContext(Dispatchers.Default)`, will run on whatever thread calls it. If called from a coroutine launched on the Main dispatcher, it WILL freeze the UI and can cause ANR. The `suspend` keyword is not a magic "run in background" command — it's just permission to pause. The developer is responsible for moving heavy work to the correct dispatcher.
