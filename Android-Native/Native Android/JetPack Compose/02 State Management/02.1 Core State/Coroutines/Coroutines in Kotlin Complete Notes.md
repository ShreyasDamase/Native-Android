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
