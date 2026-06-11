# Kotlin Coroutines Complete Notes

This file is now the index for coroutine learning inside `02.1 Core State`.

Reason for splitting:

- This folder already has separate notes for Flow, StateFlow, Compose State, and Flow with Compose.
- Keeping every coroutine and Flow detail in one huge file creates repetition.
- Each topic now has a focused subfile for interview revision and future practice.

Last updated: 2026-06-11

---

## Study Order

| Step | Topic | File | Status |
|---|---|---|---|
| 0 | Setup, first coroutine, coroutine meaning, `suspend` | [[Coroutine 00 Setup Basics Suspend]] | Covered |
| 1 | Builders, dispatchers, Android thread model, scopes | [[Coroutine 01 Builders Dispatchers Scopes]] | Covered |
| 2 | Structured concurrency, cancellation, timeout, exception handling | [[Coroutine 02 Structured Concurrency Cancellation Exceptions]] | Covered |
| 3 | Sequential vs parallel composition with `async` | [[Coroutine 03 Sequential Parallel Async Patterns]] | Covered |
| 4 | Channels and producer-consumer patterns | [[Coroutine 04 Channels Producer Consumer]] | Covered |
| 5 | Shared mutable state, testing, debugging, performance | [[Coroutine 05 Testing Debugging Shared State]] | Covered |
| 6 | Flow basics, operators, StateFlow, SharedFlow | [[../Android Jetpack Flow Study Notes]] | Covered |
| 7 | Collecting Flow safely in Compose | [[../Flow with Compose]] | Covered |
| 8 | Compose `State` vs coroutine `StateFlow` | [[../State vs StateFlow]] | Covered |

---

## Quick Interview Map

### Core coroutine topics

- Coroutine is a lightweight async task, not a thread.
- `suspend` means a function can pause and resume without blocking a thread.
- `launch` starts work and returns `Job`.
- `async` starts work that returns `Deferred<T>`.
- `await()` gets the result from `Deferred`.
- `withContext` switches dispatcher and returns a value.
- `Dispatchers.Main` is for UI.
- `Dispatchers.IO` is for network, database, and files.
- `Dispatchers.Default` is for CPU-heavy work.
- `viewModelScope` ties coroutine work to ViewModel lifecycle.
- `lifecycleScope` ties coroutine work to Activity or Fragment lifecycle.
- `rememberCoroutineScope` is for Compose UI event coroutines.
- `GlobalScope` is avoided in Android app code.

### Deep coroutine topics

- Structured concurrency means parent-child coroutine lifetime.
- `coroutineScope` is for all-or-nothing child work.
- `supervisorScope` is for independent child work.
- Cancellation is cooperative.
- Use `isActive`, `ensureActive()`, or `yield()` in CPU loops.
- `withTimeout` throws `TimeoutCancellationException`.
- `withTimeoutOrNull` returns `null` on timeout.
- Never swallow `CancellationException`.
- `launch` exceptions propagate immediately.
- `async` exceptions are observed when calling `await()`.

### Flow topics live in separate notes

- Flow is part of `kotlinx.coroutines`.
- Flow is a coroutine-based stream API.
- Flow emits multiple values over time.
- Flow is usually cold.
- `StateFlow` is hot state.
- `SharedFlow` is hot events.
- Compose should collect UI Flow using `collectAsStateWithLifecycle`.

---

## What Not To Duplicate Here

- Do not duplicate the full Flow guide here. Use [[../Android Jetpack Flow Study Notes]].
- Do not duplicate Compose collection details here. Use [[../Flow with Compose]].
- Do not duplicate State vs StateFlow architecture details here. Use [[../State vs StateFlow]].
- This file should stay as an index and interview map.

---

## Practice Checklist

- Explain coroutine vs thread.
- Explain blocking vs suspending.
- Explain `suspend` without saying it automatically uses background thread.
- Write `launch`, `async`, `await`, and `withContext` examples.
- Pick the correct dispatcher for Main, IO, and Default work.
- Use lifecycle-aware scopes in Android.
- Explain structured concurrency.
- Explain `coroutineScope` vs `supervisorScope`.
- Cancel old jobs for repeated actions like search.
- Handle Retrofit errors safely.
- Rethrow `CancellationException`.
- Use `StateFlow` for UI state.
- Use `SharedFlow` for one-time events.
- Test coroutine code with `runTest`.


---

## 🚀 Mastery Deep Dive (Added 2026)

> [!NOTE]
> The following deep dive notes were generated to provide mastery-level understanding, complementing the original notes above.

# Kotlin Coroutines — Complete Notes Index

Last updated: 2026-06-11

All coroutine files have been rewritten for deep mastery. Every file follows this structure:
- 🧠 Mental Model → 🔬 Syntax Anatomy → ⚙️ Under the Hood → ✅ Correct Usage → ❌ Mistakes → 💬 Interview Q&A

---

## Study Order

| Step | Topic | File | Depth |
|---|---|---|---|
| 0 | Setup, coroutine meaning, `suspend`, CPS, Continuation, state machine | [[Coroutine 00 Setup Basics Suspend]] | ⭐⭐⭐⭐⭐ |
| 1 | Builders, dispatchers, Job lifecycle, CoroutineContext map, all scopes | [[Coroutine 01 Builders Dispatchers Scopes]] | ⭐⭐⭐⭐⭐ |
| 2 | Structured concurrency, cancellation, CancellationException rule, NonCancellable, exception handling | [[Coroutine 02 Structured Concurrency Cancellation Exceptions]] | ⭐⭐⭐⭐⭐ |
| 3 | Sequential vs parallel, the async trap, awaitAll, Deferred, partial success | [[Coroutine 03 Sequential Parallel Async Patterns]] | ⭐⭐⭐⭐⭐ |
| 4 | Channels, callbackFlow, channelFlow, actor, trySend vs send | [[Coroutine 04 Channels Producer Consumer]] | ⭐⭐⭐⭐⭐ |
| 5 | Testing with runTest, Dispatchers.setMain, Turbine, shared state, performance | [[Coroutine 05 Testing Debugging Shared State]] | ⭐⭐⭐⭐⭐ |
| 6 | Flow basics, cold vs hot, all operators, StateFlow, SharedFlow, stateIn, shareIn | [[../Android Jetpack Flow Study Notes]] | ⭐⭐⭐⭐⭐ |
| 7 | Collecting Flow safely in Compose, lifecycle state diagram, LaunchedEffect vs collectAsStateWithLifecycle | [[../Flow with Compose]] | ⭐⭐⭐⭐ |
| 8 | Compose State vs coroutine StateFlow, backing property pattern, .update{} | [[../State vs StateFlow]] | ⭐⭐⭐⭐ |

---

## The 7 Laws You Must Never Forget

1. **`suspend` does NOT mean background thread.** Dispatcher controls the thread.
2. **`async { }.await()` is sequential.** Separate start from await for true parallelism.
3. **Always rethrow `CancellationException`.** Never swallow it.
4. **CPU loops must call `ensureActive()` or `yield()`.** Cancellation is cooperative.
5. **Never use `runBlocking` in Android UI code.** Use `viewModelScope.launch`.
6. **Use `viewLifecycleOwner.lifecycleScope` in Fragments, not `lifecycleScope`.**
7. **`awaitClose {}` is mandatory in `callbackFlow`.** Without it, the flow ends immediately and the callback leaks.

---

## Quick Interview Map

### `suspend` and basics
- `suspend` = function can pause and resume without blocking thread
- Kotlin compiler transforms `suspend` into a state machine using CPS
- `Continuation<T>` is the "bookmark" object created by the compiler
- `delay()` suspends coroutine; `Thread.sleep()` blocks the thread

### Builders
- `launch` → `Job` — fire and forget
- `async` → `Deferred<T>` — for parallel results
- `withContext` → switches dispatcher sequentially
- `runBlocking` → blocks thread (only for tests and main())

### Dispatchers
- `Main` → 1 thread, UI only
- `IO` → 64+ threads (elastic), for network/database/file
- `Default` → CPU core count threads, for CPU-heavy work

### Structured concurrency
- `coroutineScope` → all-or-nothing child failures
- `supervisorScope` → independent child failures
- Parent waits for ALL children before completing

### Cancellation
- Cooperative — must reach a suspension point or `ensureActive()`
- `CancellationException` must be rethrown — NEVER swallowed
- `withContext(NonCancellable)` for cleanup in `finally`

### Async Trap (most asked interview question)
- `async { }.await()` = sequential (chained)
- Store `Deferred`, THEN call `.await()` = parallel

### Channels and Flow builders
- `Channel` = hot queue between coroutines
- `callbackFlow` = wrap callback APIs into Flow (needs `awaitClose`)
- `channelFlow` = concurrent emissions from multiple coroutines

### Testing
- `Dispatchers.setMain(testDispatcher)` in `@Before`
- `Dispatchers.resetMain()` in `@After`
- `runTest` = virtual time, instant delays
- `advanceUntilIdle()` = run all pending coroutines
- `Turbine` = clean Flow assertions (`awaitItem`, `awaitComplete`, `awaitError`)

### Shared state
- Prefer confining state to one dispatcher (Main for ViewModels)
- `StateFlow.update {}` is atomic — prefer over `.value =` for concurrent updates
- `Mutex.withLock {}` for complex atomic operations

---

## Topics That Live in Other Files

| Topic | File |
|---|---|
| Flow cold/hot, operators, combine, zip | [[../Android Jetpack Flow Study Notes]] |
| StateFlow, SharedFlow in detail | [[../Android Jetpack Flow Study Notes]] |
| `stateIn`, `shareIn` | [[../Android Jetpack Flow Study Notes]] |
| Collecting Flow in Compose | [[../Flow with Compose]] |
| Compose State vs StateFlow architecture | [[../State vs StateFlow]] |
| Compose recomposition and State primitives | [[../States]] |
