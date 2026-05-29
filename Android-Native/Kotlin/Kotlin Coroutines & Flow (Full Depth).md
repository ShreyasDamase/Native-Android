# Kotlin Coroutines & Flow (Full Depth)

This document provides deep technical reference documentation for Kotlin Coroutines, structured concurrency, hot/cold reactive flow streams, channels, exception propagation trees, and asynchronous testing strategies.

---

## 1. Under the Hood: Continuation-Passing Style (CPS)

A coroutine is NOT a thread. It is a framework for executing suspendable computations over actual operating system threads.

### The `suspend` Bytecode Transformation
When the Kotlin compiler encounters a function marked `suspend`, it modifies the function signature at the bytecode level using **Continuation-Passing Style (CPS)**.
1. The compiler appends a hidden parameter of type `Continuation<T>` as the final parameter of the function.
2. The return type of the function is changed from `T` to `Any?`.
3. The function returns either the computed value `T`, or a special sentinel object: `COROUTINE_SUSPENDED`.

```kotlin
// Source Kotlin code
suspend fun fetchUserData(userId: String): User

// Compiled JVM equivalent (simplified)
fun fetchUserData(userId: String, completion: Continuation<User>): Any?
```

### The State Machine
The compiler generates a class (implementing `Continuation` and functioning as a state machine) to wrap the body of the suspending function. Every suspension point (e.g. calling other suspending functions) corresponds to a state index.
*   When a coroutine suspends, the execution state is saved inside the continuation object.
*   Once the asynchronous task finishes, it calls `continuation.resumeWith(result)`. This invokes the state machine function, resuming from the exact state index where it left off, bypassing the need to block the executing thread.

---

## 2. Coroutine Context & Custom Schedulers

### CoroutineContext Elements
A `CoroutineContext` behaves like a type-safe map where keys are associated with specific elements. The four core elements are:
1.  `Job`: Controls the lifecycle and cancellation hierarchy of the coroutine.
2.  `CoroutineDispatcher`: Determines the thread execution pool (e.g. `Dispatchers.IO`, `Dispatchers.Default`).
3.  `CoroutineName`: A developer-defined identifier for debugging purposes.
4.  `CoroutineExceptionHandler`: Catches unhandled exceptions thrown during `launch` builders.

Elements are joined using the `+` operator:
```kotlin
val context = Dispatchers.IO + Job() + CoroutineName("NetworkFetch")
```

### Custom Schedulers & `limitedParallelism()`
You can construct custom dispatchers wrapped around standard Java executors, or restrict existing dispatchers using `limitedParallelism`:

```kotlin
// Create a dispatcher from a Java Thread Pool
val myCustomDispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

// Restrict max concurrency on Dispatchers.IO (e.g. limit to 10 concurrent database operations)
val rateLimitedDispatcher = Dispatchers.IO.limitedParallelism(10)
```

---

## 3. Structured Concurrency & Exception Trees

Structured Concurrency guarantees that when a scope is cancelled, all coroutines launched within that scope are cancelled automatically, preventing execution leaks.

### Parent-Child Relationships & Job Hierarchy
*   A parent job waits for all its children to finish before completing.
*   If a parent job is cancelled, all its children jobs are recursively cancelled.
*   **Default Behavior (Standard `Job`)**: If a child coroutine fails with an exception other than `CancellationException`, it immediately propagates the failure upwards, cancelling its parent, which subsequently cancels all sibling coroutines.

---

### `supervisorScope` vs. `coroutineScope`

*   `coroutineScope`: Standard scope. Any child failure cancels the parent scope and all sibling coroutines.
*   `supervisorScope`: Supervisor scope. Uses a `SupervisorJob` internally. A failure of one child does **not** propagate upwards; siblings continue running unaffected.

```kotlin
// If taskA throws an exception, taskB is immediately cancelled
coroutineScope {
    val taskA = launch { throw RuntimeException("A failed") }
    val taskB = launch { delay(1000L); println("B finished") }
}

// If taskA throws an exception, taskB continues executing and completes
supervisorScope {
    val taskA = launch { throw RuntimeException("A failed") }
    val taskB = launch { delay(1000L); println("B finished") }
}
```

---

### Cancellation Propagation Rules
1.  **Cooperative Cancellation**: Coroutines are not forcibly terminated. They must check for cancellation periodically. Use `isActive`, `ensureActive()`, or call standard suspending functions (which check for cancellation internally).
2.  **`CancellationException`**: Used to signal cancellation. It is caught by the coroutine framework and does **not** propagate to the parent. **Never swallow `CancellationException`** in try-catch blocks:
    ```kotlin
    try {
        doWork()
    } catch (e: Exception) {
        if (e is CancellationException) throw e // MUST rethrow!
        handleFailure(e)
    }
    ```
3.  **`NonCancellable` Context**: To run suspend functions inside cleanup blocks (like `finally`), wrap them in `withContext(NonCancellable)` so they aren't cancelled.
    ```kotlin
    finally {
        withContext(NonCancellable) {
            database.closeConnection() // Runs even if parent was cancelled
        }
    }
    ```

---

## 4. Cold Flows, `channelFlow`, and Context Preservation

A `Flow` represents a cold, asynchronous stream. It emits values sequentially and does not produce data until terminal collection begins.

### Context Preservation & `flowOn`
A flow preserves the coroutine context of the collector. It is a runtime error to emit values from a different context/thread (e.g. inside `flow { withContext(Dispatchers.IO) { emit(x) } }`). To safely change the execution thread of a flow, use the `flowOn()` operator:

```kotlin
fun getStream(): Flow<Int> = flow {
    emit(fetchData()) // Runs on Dispatchers.IO
}.flowOn(Dispatchers.IO) // Changes dispatcher for upstream actions
```

---

### `callbackFlow` vs. `channelFlow`
*   `channelFlow`: A hot channel-backed flow. Useful when you need to emit values concurrently from multiple coroutine tasks inside the flow builder.
*   `callbackFlow`: A specialized variant of `channelFlow` designed to wrap listener callbacks and async API streams. It enforces usage of `awaitClose` to clean up resources when the flow collector cancels.

```kotlin
fun observeLocationUpdates(): Flow<Location> = callbackFlow {
    val callback = object : LocationCallback {
        override fun onLocationResult(res: LocationResult) {
            trySend(res.lastLocation) // Safely send to channel
        }
    }
    locationManager.registerListener(callback)
    
    // Mandatory: Keep flow alive and clean up resources on close
    awaitClose {
        locationManager.unregisterListener(callback)
    }
}
```

---

## 5. SharedFlow & StateFlow Internals

Hot flows exist independently of collectors. They manage subscription lists and broadcast updates.

### MutableSharedFlow Backpressure & Buffering
`MutableSharedFlow` can buffer emissions if subscribers are slow.
*   `replay`: The number of past emissions stored and replayed to new subscribers.
*   `extraBufferCapacity`: Additional buffer space to prevent producer suspension.
*   `onBufferOverflow`: Strategy for handling overflow:
    *   `BufferOverflow.SUSPEND` (Default): `emit()` suspends until space clears.
    *   `BufferOverflow.DROP_OLDEST`: Discards the oldest buffered item.
    *   `BufferOverflow.DROP_LATEST`: Discards the newest incoming item.

### The `WhileSubscribed` Optimization
To prevent hot flows from wasting CPU/battery when the app is in the background, configure them to start and stop dynamically using `WhileSubscribed(stopTimeoutMillis)`:

```kotlin
val state: StateFlow<UiState> = coldRepositoryFlow
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // Wait 5s before stopping upstream to handle configuration changes (like rotation)
        initialValue = UiState.Loading
    )
```

---

## 6. Channels

A `Channel` represents a hot concurrency primitive used to pass values between coroutines (like a blocking queue).

### Channel Buffer Types
1.  `Channel.RENDEZVOUS` (Capacity = 0): The sender suspends until a receiver retrieves the element, and vice-versa.
2.  `Channel.BUFFERED` (Default capacity): Holds a fixed-size buffer. Sender suspends only when the buffer is full.
3.  `Channel.CONFLATED`: Holds a buffer of size 1. The sender never suspends; new emissions overwrite the previous unread value.
4.  `Channel.UNLIMITED`: Holds an unbounded buffer. Sender never suspends, but can cause out-of-memory errors if consumer lags.

---

## 7. Advanced Coroutine Testing

Coroutines are tested using virtual-time execution, bypassing actual time delays to execute tests instantly.

### `runTest` & Schedulers
`runTest` executes tests on a `TestScope` and advances virtual time automatically when coroutines yield.
*   `StandardTestDispatcher`: Default test scheduler. Coroutines do not execute immediately when launched; you must call `runCurrent()` or advance time manually.
*   `UnconfinedTestDispatcher`: Coroutines are executed eagerly/immediately upon launch, matching simple unit tests.

### Controlling Virtual Time
*   `advanceTimeBy(millis)`: Advances the clock by a specific duration and runs pending tasks.
*   `advanceUntilIdle()`: Automatically executes all remaining pending tasks in the scheduler queue.
*   `runCurrent()`: Executes tasks currently scheduled at the current virtual time.

### Testing Flows with Turbine
`Turbine` is a lightweight testing library that simplifies collecting flow streams without spawning manual collections.

```kotlin
@Test
fun testFlowEmissions() = runTest {
    val myFlow = flowOf("A", "B", "C")
    
    myFlow.test {
        assertEquals("A", awaitItem())
        assertEquals("B", awaitItem())
        assertEquals("C", awaitItem())
        awaitComplete()
    }
}
```
