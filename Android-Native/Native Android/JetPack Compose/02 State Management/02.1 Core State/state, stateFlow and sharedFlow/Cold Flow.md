# 🧊 Cold Flow — Complete Guide

> [!NOTE]
> **Sub-note for Cold Flow.** For hot flows that hold state, see [[StateFlow]]. For hot flows for events, see [[SharedFlow]]. For how to collect flows safely in Compose UI, see [[State]].

---

## 🧠 Mental Model — Read This First

**A Flow is a cold water pipe.**

- **Cold:** The pipe does absolutely nothing until someone turns the tap at the end (`collect`). If nobody collects, the code inside the Flow builder **never runs** — not even once.
- **Sequential:** Water drops (values) travel down the pipe one by one. Drop 2 cannot leave until Drop 1 is fully processed by the collector.
- **Independent:** Every collector gets their OWN independent execution of the pipe. 3 collectors = 3 separate API calls / database queries.

```
Cold Flow:
Producer code runs? ─── NO ───── until collect() is called
                         YES ──── once per collector, independently
```

**Contrast with Hot Flows:**
```
StateFlow / SharedFlow:
Producer runs? ──── YES ──── always, regardless of collectors
Collectors? ────────────────── all share the SAME emissions
```

---

## 🔍 Master Lookup Table — Find Any Method Instantly

> Click any method name to jump straight to its full explanation, syntax, and example.

### Flow Builders

| Method | One-Line Purpose | Jump To |
|---|---|---|
| `flow { }` | General-purpose builder — full control, can delay/emit conditionally | [[#2.1 `flow { }` — The Standard Builder\|§2.1 flow { }]] |
| `flowOf(a, b, c)` | Emit a fixed set of known values | [[#2.2 `flowOf()` — Fixed Values\|§2.2 flowOf]] |
| `.asFlow()` | Convert any List, Array, Sequence, Range → Flow | [[#2.3 `.asFlow()` — Convert Collections\|§2.3 asFlow]] |
| `emptyFlow()` | A flow that emits nothing and completes immediately | [[#2.4 `emptyFlow()`\|§2.4 emptyFlow]] |
| `emitAll(source)` | Inside a builder, delegate all emissions to another flow/iterable | [[#2.5 `emitAll()` — Delegate Emissions\|§2.5 emitAll]] |
| `callbackFlow { }` | Bridge legacy callback APIs (Firebase, GPS, Bluetooth) → Flow | [[#3. callbackFlow — Bridging Legacy APIs\|§3 callbackFlow]] |
| `channelFlow { }` | Multiple concurrent coroutines all emitting into the same flow | [[#4. channelFlow — Concurrent Emissions\|§4 channelFlow]] |

### Terminal Operators (start the flow, return a result)

| Method | One-Line Purpose | Jump To |
|---|---|---|
| `collect { }` | Process every value; runs until flow completes | [[#11.1 `collect`\|§11.1 collect]] |
| `first()` | Return first value, cancel rest. Throws if empty | [[#11.2 `first` / `firstOrNull`\|§11.2 first]] |
| `firstOrNull()` | Return first value, or `null` if flow is empty | [[#11.2 `first` / `firstOrNull`\|§11.2 firstOrNull]] |
| `single()` | Expect exactly 1 value. Throws if 0 or 2+ | [[#11.3 `single` / `singleOrNull`\|§11.3 single]] |
| `singleOrNull()` | Expect exactly 1 value, or `null` if 0. Throws if 2+ | [[#11.3 `single` / `singleOrNull`\|§11.3 singleOrNull]] |
| `last()` | Wait for all emissions, return last. Throws if empty | [[#11.4 `last` / `lastOrNull`\|§11.4 last]] |
| `lastOrNull()` | Wait for all emissions, return last, or `null` if empty | [[#11.4 `last` / `lastOrNull`\|§11.4 lastOrNull]] |
| `toList()` | Collect all values into a `List<T>` | [[#11.5 `toList` / `toSet`\|§11.5 toList]] |
| `toSet()` | Collect all values into a `Set<T>` (deduplicates) | [[#11.5 `toList` / `toSet`\|§11.5 toSet]] |
| `fold(initial)` | Reduce all values to one result, starting from `initial` | [[#11.6 `fold` / `reduce`\|§11.6 fold]] |
| `reduce { }` | Like `fold` but uses first element as initial accumulator | [[#11.6 `fold` / `reduce`\|§11.6 reduce]] |
| `count()` | Count how many values the flow emits | [[#11.7 `count`\|§11.7 count]] |
| `launchIn(scope)` | Non-suspending fire-and-forget collection in a coroutine scope | [[#11.8 `launchIn`\|§11.8 launchIn]] |

### Transformation Operators (lazy — don't start the flow)

| Method | One-Line Purpose | Jump To |
|---|---|---|
| `map { }` | Transform each value 1-to-1 | [[#5.1 `map`\|§5.1 map]] |
| `mapNotNull { }` | Transform and silently drop `null` results | [[#5.2 `mapNotNull`\|§5.2 mapNotNull]] |
| `transform { }` | Most powerful — emit 0, 1, or many values per input | [[#5.3 `transform`\|§5.3 transform]] |
| `scan(initial)` | Emit running accumulated value after each emission | [[#5.4 `scan`\|§5.4 scan]] |
| `onEach { }` | Side-effects (logging, analytics) without changing values | [[#6.2 `onEach`\|§6.2 onEach]] |

### Filtering Operators

| Method | One-Line Purpose | Jump To |
|---|---|---|
| `filter { }` | Only pass values matching the predicate | [[#5.5 `filter`\|§5.5 filter]] |
| `filterNot { }` | Only pass values NOT matching the predicate | [[#5.5 `filter`\|§5.5 filterNot]] |
| `filterIsInstance<T>()` | Only pass values of a specific type | [[#5.5 `filter`\|§5.5 filterIsInstance]] |
| `take(n)` | Complete after first `n` emissions | [[#5.6 `take` / `takeWhile`\|§5.6 take]] |
| `takeWhile { }` | Complete when predicate becomes `false` | [[#5.6 `take` / `takeWhile`\|§5.6 takeWhile]] |
| `drop(n)` | Skip the first `n` values | [[#5.7 `drop` / `dropWhile`\|§5.7 drop]] |
| `dropWhile { }` | Skip values while predicate is `true` | [[#5.7 `drop` / `dropWhile`\|§5.7 dropWhile]] |
| `distinctUntilChanged()` | Skip consecutive duplicate values | [[#5.8 `distinctUntilChanged`\|§5.8 distinctUntilChanged]] |
| `distinctUntilChangedBy { }` | Skip consecutive duplicates by a key field | [[#5.8 `distinctUntilChanged`\|§5.8 distinctUntilChangedBy]] |
| `debounce(ms)` | Wait for silence before emitting (search box) | [[#5.9 `debounce` / `sample`\|§5.9 debounce]] |
| `sample(ms)` | Emit at most once per time window | [[#5.9 `debounce` / `sample`\|§5.9 sample]] |

### Lifecycle Operators

| Method | One-Line Purpose | Jump To |
|---|---|---|
| `onStart { }` | Run a block before the flow starts emitting | [[#6.1 `onStart`\|§6.1 onStart]] |
| `onEach { }` | Run side-effects on each value without transforming | [[#6.2 `onEach`\|§6.2 onEach]] |
| `onCompletion { }` | Always run on completion (success or error) — like `finally` | [[#6.3 `onCompletion`\|§6.3 onCompletion]] |

### Combining Operators

| Method | One-Line Purpose | Jump To |
|---|---|---|
| `zip(other)` | Pair each emission 1-to-1 from two flows | [[#7.1 `zip`\|§7.1 zip]] |
| `combine(other)` | Re-emit whenever EITHER flow emits, using latest of both | [[#7.2 `combine`\|§7.2 combine]] |
| `merge(f1, f2…)` | Interleave emissions from multiple flows as they arrive | [[#7.3 `merge`\|§7.3 merge]] |
| `flatMapLatest { }` | Cancel previous inner flow, start new one on each emission | [[#7.4 `flatMapLatest`\|§7.4 flatMapLatest]] |
| `flatMapMerge { }` | Run all inner flows concurrently | [[#7.5 `flatMapMerge`\|§7.5 flatMapMerge]] |
| `flatMapConcat { }` | Run inner flows sequentially (one finishes, next starts) | [[#7.6 `flatMapConcat`\|§7.6 flatMapConcat]] |

### Backpressure Operators

| Method | One-Line Purpose | Jump To |
|---|---|---|
| `buffer(n)` | Decouple producer and collector with a channel buffer | [[#8.1 `buffer`\|§8.1 buffer]] |
| `conflate()` | Drop intermediate values; collector only processes latest | [[#8.2 `conflate`\|§8.2 conflate]] |
| `collectLatest { }` | Cancel running collector block when new value arrives | [[#8.3 `collectLatest`\|§8.3 collectLatest]] |

### Context / Threading

| Method | One-Line Purpose | Jump To |
|---|---|---|
| `flowOn(dispatcher)` | Run everything UPSTREAM on a different dispatcher | [[#9. flowOn — Thread Switching\|§9 flowOn]] |

### Error Handling

| Method | One-Line Purpose | Jump To |
|---|---|---|
| `catch { }` | Catch exceptions from upstream, emit fallback | [[#`catch` — Handle Upstream Errors\|§10 catch]] |
| `retry(n)` | Automatically retry upstream on failure | [[#`retry` — Automatic Retry on Failure\|§10 retry]] |
| `retryWhen { }` | Retry with custom logic (delay, max attempts, error type) | [[#`retryWhen` — Retry with Delay\|§10 retryWhen]] |

### Cold → Hot Conversion

| Method | One-Line Purpose | Jump To |
|---|---|---|
| `stateIn(scope, started, init)` | Convert cold Flow → `StateFlow` (holds latest value) | [[#12.1 `stateIn`\|§12.1 stateIn]] |
| `shareIn(scope, started, replay)` | Convert cold Flow → `SharedFlow` (broadcasts to multiple) | [[#12.2 `shareIn`\|§12.2 shareIn]] |

---

## 📌 Table of Contents

1. [The Cold Flow Contract](#1-the-cold-flow-contract)
2. [Flow Builders](#2-flow-builders)
3. [callbackFlow — Bridging Legacy APIs](#3-callbackflow--bridging-legacy-apis)
4. [channelFlow — Concurrent Emissions](#4-channelflow--concurrent-emissions)
5. [Transformation Operators](#5-transformation-operators)
6. [Lifecycle Operators](#6-lifecycle-operators)
7. [Combining Flows](#7-combining-flows)
8. [Backpressure Operators](#8-backpressure-operators)
9. [flowOn — Thread Switching](#9-flowon--thread-switching)
10. [Error Handling](#10-error-handling)
11. [Terminal Operators](#11-terminal-operators)
12. [Cold → Hot: stateIn and shareIn](#12-cold--hot-statein-and-sharein)
13. [Repository Layer Architecture](#13-repository-layer-architecture)
14. [Testing Flows with Turbine](#14-testing-flows-with-turbine)
15. [Common Mistakes](#15-common-mistakes)
16. [Interview Q&A](#16-interview-qa)
17. [Remaining Operator Reference](#17-remaining-operator-reference)
18. [Gradle Dependencies](#18-gradle-dependencies)
19. [Connections](#19-connections)

---

## 1. The Cold Flow Contract

```kotlin
val coldFlow: Flow<Int> = flow {
    println("🟢 Pipe turned on!")  // only runs when collected
    emit(1)
    delay(1000)
    emit(2)
    delay(1000)
    emit(3)
    println("🔴 Pipe done!")
}

// ❌ Nothing happens yet — no println, no emission
// ← Flow is just a DESCRIPTION of work, not the work itself

coroutineScope.launch {
    coldFlow.collect { value ->
        println("Received: $value")     // NOW it starts
    }
}
// Prints: "🟢 Pipe turned on!", "Received: 1", "Received: 2", "Received: 3", "🔴 Pipe done!"

// Second collector: runs the ENTIRE block again — independent!
coroutineScope.launch {
    coldFlow.collect { value ->
        println("Second collector: $value")   // another full run of the pipe
    }
}
```

> [!IMPORTANT]
> Cold Flow = a **recipe**, not the cooking. `collect` = starting to cook. Each collector cooks their own meal from the recipe independently.

### Key Contract Summary

| Property | Cold Flow | Hot StateFlow / SharedFlow |
|---|---|---|
| Starts running | Only when `collect` is called | Immediately / on first subscriber |
| Per collector | Independent full execution | Shared single execution |
| Stores values | No | StateFlow: yes (latest). SharedFlow: replay |
| Multiple collectors | ✅ Safe, but = multiple executions | ✅ All share same emissions |

---

## 2. Flow Builders

### 2.1 `flow { }` — The Standard Builder

```kotlin
val timerFlow: Flow<Int> = flow {
    for (i in 1..5) {
        delay(1000)   // suspend — doesn't block the thread
        emit(i)       // send value downstream
    }
}
```

**Rules inside `flow { }`:**

| Allowed | Not Allowed |
|---|---|
| `emit(value)` — send values | `withContext()` — throws IllegalStateException |
| `delay()` and other suspend functions | Emitting from a different coroutine/thread |
| `if/else`, loops, conditions | (use `flowOn` for thread switching) |
| `emitAll(otherFlow)` | |

### 2.2 `flowOf()` — Fixed Values

```kotlin
// Emits the exact values you list, then completes
val fixedFlow: Flow<String> = flowOf("Alice", "Bob", "Charlie")

// Equivalent to:
val equivalentFlow = flow {
    emit("Alice")
    emit("Bob")
    emit("Charlie")
}
```

> [!TIP]
> Use `flowOf` for test data, hard-coded constants, error messages, or small known value sets.

### 2.3 `.asFlow()` — Convert Collections

```kotlin
val listFlow: Flow<Int>    = listOf(1, 2, 3, 4, 5).asFlow()
val rangeFlow: Flow<Int>   = (1..10).asFlow()
val setFlow: Flow<String>  = setOf("a", "b", "c").asFlow()
val arrayFlow: Flow<Int>   = arrayOf(10, 20, 30).asFlow()
val seqFlow: Flow<Long>    = sequenceOf(1L, 2L, 3L).asFlow()
```

> [!CAUTION]
> `flowOf(listOf(1,2,3))` emits ONE value — the whole list. `listOf(1,2,3).asFlow()` emits THREE values. Never confuse these.

### 2.4 `emptyFlow()`

```kotlin
val nothingFlow: Flow<Int> = emptyFlow()
// collect lambda never runs — flow completes immediately
```

> [!TIP]
> Use `emptyFlow()` as a safe placeholder when a feature flag is off, or in tests where you need a no-op flow.

### 2.5 `emitAll()` — Delegate Emissions

```kotlin
val sourceFlow = flowOf(1, 2, 3)

val composedFlow: Flow<Int> = flow {
    emit(0)                          // single value
    emitAll(sourceFlow)              // emit all from another flow
    emitAll(listOf(4, 5, 6))         // emit all from an iterable
    emit(7)
}
// composedFlow emits: 0, 1, 2, 3, 4, 5, 6, 7
```

> [!TIP]
> Real-world use: `emitAll(cacheFlow)` then `emitAll(networkFlow)` — show cached data first, then refresh with fresh data.

---

## 3. callbackFlow — Bridging Legacy APIs

> [!IMPORTANT]
> **Highly tested in interviews.** `callbackFlow` converts traditional callback-based APIs (Firebase Realtime DB, LocationManager, Bluetooth, sensors) into Kotlin Flows. It uses a `Channel` under the hood, allowing `trySend()` from non-suspend callback interfaces.

### The Pattern

```kotlin
fun getLocationUpdates(locationManager: LocationManager): Flow<Location> = callbackFlow {
    // 1. Define the callback that bridges to the Flow
    val callback = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            trySend(location)    // ← non-suspend, safe to call from any callback
            // returns ChannelResult — true if sent, false if channel closed/full
        }
        override fun onProviderDisabled(provider: String) {
            close()              // ← signals the flow to complete
        }
    }

    // 2. Register the callback with the system API
    locationManager.requestLocationUpdates(
        LocationManager.GPS_PROVIDER, 1000L, 0f, callback
    )

    // 3. MANDATORY: awaitClose suspends the coroutine until the collector cancels
    // The block inside MUST unregister the callback to prevent memory leaks
    awaitClose {
        locationManager.removeUpdates(callback)    // ← cleanup on cancellation
    }
}
```

> [!CAUTION]
> **`awaitClose` is mandatory in `callbackFlow`.** Without it, the flow builder exits immediately, the coroutine finishes, and the flow completes before any callbacks fire. The flow would be useless. Also: forgetting to unregister in `awaitClose` = memory leak.

### Firebase Example

```kotlin
fun getUserDocument(userId: String): Flow<User?> = callbackFlow {
    val listener = FirebaseFirestore.getInstance()
        .collection("users")
        .document(userId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)   // ← signals the flow with an exception
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(User::class.java))
        }

    awaitClose { listener.remove() }   // detach Firestore listener on cancellation
}
```

### Sensor Data Example

```kotlin
fun getSensorReadings(sensorManager: SensorManager, sensorType: Int): Flow<Float> = callbackFlow {
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            trySend(event.values[0])
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    val sensor = sensorManager.getDefaultSensor(sensorType)
    sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)

    awaitClose {
        sensorManager.unregisterListener(listener)
    }
}
```

---

## 4. channelFlow — Concurrent Emissions

`callbackFlow` is single-threaded by default. `channelFlow` allows launching multiple coroutines inside the builder that all `send()` to the same flow:

```kotlin
// Fetch from network and cache concurrently, emit whichever comes first
val data: Flow<Data> = channelFlow {
    launch {
        val cached = localCache.getData()
        send(cached)                // ← send (not emit) inside channelFlow
    }
    launch {
        val fresh = api.getData()
        localCache.save(fresh)
        send(fresh)
    }
}
```

### Builder Comparison

| Builder | Concurrency | `emit` or `send` | Use for |
|---|---|---|---|
| `flow { }` | Single coroutine, sequential | `emit` | Most cases |
| `callbackFlow { }` | Single coroutine + external callbacks | `trySend` | Legacy API bridging |
| `channelFlow { }` | Multiple coroutines | `send` | Concurrent producers |

---

## 5. Transformation Operators

Operators transform data **as it travels down the pipe**. They are lazy — they don't start the flow, they just add transformation steps to be applied when the flow is collected.

### 5.1 `map`

Transform each value 1-to-1. The output count equals the input count.

```kotlin
// Int → String
flow { emit(1); emit(2); emit(3) }
    .map { number -> "Item #$number" }
    .collect { println(it) }
// "Item #1", "Item #2", "Item #3"

// Real-world: map User entity → UI model
userFlow
    .map { user -> UserUiModel(user.name, user.email) }
    .collect { updateUI(it) }
```

### 5.2 `mapNotNull`

Transform each value, and silently skip emissions where the result is `null`.

```kotlin
flow { emit("Alice"); emit(null); emit("Bob") }
    .mapNotNull { name -> name?.uppercase() }
    .collect { println(it) }
// "ALICE", "BOB"  (null is dropped)

// Real-world: parse optional fields
responseFlow
    .mapNotNull { response -> response.optionalField }  // skip if null
    .collect { process(it) }
```

### 5.3 `transform`

The most powerful transformation — can emit **0, 1, or many values** per input. The superset of `map` and `filter`.

```kotlin
flow { emit(1); emit(2); emit(3) }
    .transform { value ->
        emit(value)          // emit original
        emit(value * 10)     // emit transformed version
        if (value > 1) {
            emit(-value)     // conditional third emission
        }
    }
    .collect { println(it) }
// 1, 10, 2, 20, -2, 3, 30, -3

// Real-world: emit Loading, then Result
requestFlow
    .transform { request ->
        emit(UiState.Loading)
        val result = api.fetch(request)
        emit(UiState.Success(result))
    }
```

### 5.4 `scan`

Like `fold`, but **emits every intermediate accumulator value** as it builds up. Useful for running totals, undo history, state accumulation.

```kotlin
flowOf(1, 2, 3, 4, 5)
    .scan(0) { accumulator, value -> accumulator + value }
    .collect { println(it) }
// 0, 1, 3, 6, 10, 15
// ↑ emits initial value, then running sum after each element

// Real-world: build up a list incrementally
eventsFlow
    .scan(emptyList<Event>()) { history, event -> history + event }
    .collect { updateHistoryUI(it) }
```

### 5.5 `filter`

Only pass values that match the predicate. Drops all others.

```kotlin
(1..10).asFlow()
    .filter { it % 2 == 0 }     // only even numbers
    .collect { println(it) }
// 2, 4, 6, 8, 10

// filterNot — opposite of filter
(1..10).asFlow()
    .filterNot { it % 2 == 0 }  // only odd numbers

// filterIsInstance — type filtering
mixedFlow
    .filterIsInstance<String>()  // only String values pass through
```

### 5.6 `take` / `takeWhile`

```kotlin
// take — complete after first N emissions (cancels producer)
(1..100).asFlow()
    .take(5)
    .collect { println(it) }
// 1, 2, 3, 4, 5  (producer cancelled after 5)

// takeWhile — complete when condition becomes false (exclusive of failing element)
(1..10).asFlow()
    .takeWhile { it < 5 }
    .collect { println(it) }
// 1, 2, 3, 4  (5 fails the check → flow completes)
```

> [!TIP]
> `take(1)` is equivalent to `first()` but as an intermediate operator you can chain further before collecting.

### 5.7 `drop` / `dropWhile`

```kotlin
// drop — skip first N values, then pass all remaining
(1..5).asFlow()
    .drop(2)
    .collect { println(it) }
// 3, 4, 5

// dropWhile — skip values while condition is true, then pass ALL remaining
(1..5).asFlow()
    .dropWhile { it < 3 }
    .collect { println(it) }
// 3, 4, 5
// Note: once condition is false once, ALL subsequent values pass — even if they'd match again
```

### 5.8 `distinctUntilChanged`

Skip consecutive duplicate values. Only emits when the value has changed from the previous one.

```kotlin
flowOf(1, 1, 2, 2, 3, 1)
    .distinctUntilChanged()
    .collect { println(it) }
// 1, 2, 3, 1
// ↑ the repeated 1 at end is included — it's not consecutive with the previous 3

// distinctUntilChangedBy — compare by a field, not the whole object
userFlow
    .distinctUntilChangedBy { user -> user.email }  // only emit when email changes
    .collect { updateUI(it) }
```

> [!TIP]
> Essential in search boxes to avoid triggering a new API call when the user re-types the same query.

### 5.9 `debounce` / `sample`

```kotlin
// debounce — wait for N ms of silence before emitting (search box anti-spam)
searchQueryFlow
    .debounce(300)   // only emit if user hasn't typed for 300ms
    .collect { query -> searchApi(query) }

// sample — emit the latest value from the last N ms window (throttle by time)
sensorReadingFlow
    .sample(200)     // emit at most once per 200ms, always the latest value
    .collect { reading -> updateChart(reading) }
```

| Operator | When it emits | Drops values? | Use for |
|---|---|---|---|
| `debounce` | After N ms of silence | Yes — intermediate values | Search box typing |
| `sample` | Every N ms (latest value) | Yes — intermediate values | Sensor throttle, live charts |

---

## 6. Lifecycle Operators

These operators let you hook into the flow's lifecycle — before it starts, on each value, and when it ends.

### 6.1 `onStart`

Run a block **before** the flow starts emitting. Runs exactly once per collection. Does NOT transform values.

```kotlin
flow { emit("Data") }
    .onStart { emit("Loading...") }   // emits BEFORE the flow starts
    .collect { println(it) }
// "Loading...", "Data"

// Real-world: show loading spinner, emit initial state
dataFlow
    .onStart { emit(UiState.Loading) }
    .catch { emit(UiState.Error(it.message)) }
    .collect { uiState -> render(uiState) }
```

> [!IMPORTANT]
> `onStart` can call `emit()` — the emitted values appear upstream of operators placed before `onStart` in the chain. Place `onStart` after `catch` to ensure loading state isn't affected by error handling.

### 6.2 `onEach`

Run a side-effect block on each emitted value. The value is passed through unchanged.

```kotlin
apiFlow
    .onEach { result -> Log.d("Flow", "Received: $result") }   // logging
    .map { it.toUiModel() }
    .collect { render(it) }

// launchIn shortcut — fire-and-forget with onEach
apiFlow
    .onEach { updateUI(it) }
    .launchIn(viewModelScope)   // starts collection, returns Job
```

### 6.3 `onCompletion`

Run a block when the flow finishes — whether successfully, with an error, or via cancellation. Like `finally`.

```kotlin
flow { emit(1); emit(2); emit(3) }
    .onCompletion { cause ->
        if (cause != null) {
            println("Flow completed with error: $cause")
        } else {
            println("Flow completed successfully ✅")
        }
        hideLoadingSpinner()   // ALWAYS hides spinner — success, failure, or cancel
    }
    .catch { emit(defaultValue) }
    .collect { process(it) }
```

> [!TIP]
> `onCompletion` is called for cancellation too (the `cause` will be a `CancellationException`). Use it for cleanup: hiding spinners, releasing resources, analytics.

### Complete Pipeline Example

```kotlin
// Search screen: user typing → API → display results
searchQueryFlow
    .onEach { Log.d("Search", "Query: $it") }   // log every keystroke
    .filter { it.length >= 2 }                  // only search if ≥ 2 chars
    .distinctUntilChanged()                     // skip if query didn't change
    .debounce(300)                              // wait 300ms after user stops typing
    .map { query -> query.lowercase() }         // normalize
    .flatMapLatest { query ->                   // cancel previous search, start new one
        repository.search(query)                // returns Flow<List<Result>>
    }
    .onStart { emit(emptyList()) }              // start with empty list
    .catch { e -> emit(emptyList()) }           // handle errors gracefully
    .onCompletion { hideKeyboard() }            // cleanup when done
    .collect { results ->
        updateUI(results)
    }
```

---

## 7. Combining Flows

### 7.1 `zip`

Pair each emission 1-to-1 from two flows. If one flow is longer, extra values are discarded.

```kotlin
val numbers = flowOf(1, 2, 3)
val letters = flowOf("A", "B", "C")

numbers.zip(letters) { num, letter -> "$num$letter" }
    .collect { println(it) }
// Output: "1A", "2B", "3C"
// If one flow completes, the combined flow completes too
```

> [!TIP]
> Use `zip` when you need to combine two **synchronized** streams (e.g., animation frames and data, or two API calls that must be paired).

### 7.2 `combine`

Re-emits whenever **either** flow emits, using the latest value from each.

```kotlin
// Emits EVERY TIME either flow emits, using the latest value from the other
val names = flowOf("Alice", "Bob")
val ages = flowOf(25, 30, 35)

combine(names, ages) { name, age -> "$name is $age" }
    .collect { println(it) }
// "Alice is 25", "Alice is 30", "Bob is 30", "Bob is 35"
// ← every emission from either triggers a new combined emission

// Real-world: combine search query + filter settings
val results = combine(searchQuery, filterState) { query, filter ->
    repository.search(query, filter)
}.flatMapLatest { it }
```

### 7.3 `merge`

Interleave emissions from multiple flows as they arrive in time order.

```kotlin
// Emits values from ALL flows, interleaved as they arrive
val flow1 = flow { emit("A"); delay(100); emit("C") }
val flow2 = flow { delay(50); emit("B"); delay(100); emit("D") }

merge(flow1, flow2).collect { println(it) }
// Output: A, B, C, D (in time order)
```

> [!TIP]
> Use `merge` when you have multiple independent data sources (e.g., push notifications from multiple channels) and want a single stream of all events.

### 7.4 `flatMapLatest`

For each new value, start a new inner flow and **cancel the previous inner flow**.

```kotlin
// Each new value from the outer flow CANCELS the previous inner flow
searchQuery
    .flatMapLatest { query ->
        repository.search(query)    // new search cancels old in-flight search
    }
    .collect { results -> updateUI(results) }

// Use case: search, live weather by city, chat room switching
```

> [!IMPORTANT]
> `flatMapLatest` is the most common in Android — it prevents stale results from old queries arriving after newer ones.

### 7.5 `flatMapMerge`

Run all inner flows **concurrently**. Control max concurrency with the `concurrency` parameter.

```kotlin
// All inner flows run CONCURRENTLY (unlike flatMapLatest)
userIds
    .flatMapMerge(concurrency = 4) { id ->  // at most 4 concurrent
        userRepository.getUser(id)
    }
    .collect { user -> processUser(user) }
```

> [!TIP]
> Use when order doesn't matter but you want throughput — like downloading multiple files in parallel.

### 7.6 `flatMapConcat`

Each inner flow runs only **after the previous one completes**. Preserves order.

```kotlin
// Each inner flow runs only AFTER the previous one completes
tasks
    .flatMapConcat { task ->
        processTask(task)   // one at a time, in order
    }
    .collect { result -> handleResult(result) }
```

### flatMap Comparison

| Operator | Concurrency | Order preserved | Use for |
|---|---|---|---|
| `flatMapLatest` | Only latest | N/A (cancels old) | Search, live data by selection |
| `flatMapMerge` | All concurrent | No | Parallel downloads |
| `flatMapConcat` | Sequential | Yes | Ordered task queue |

---

## 8. Backpressure Operators

Backpressure happens when the **producer emits faster than the collector can process**.

```
Default behavior (sequential):
Producer emits 1 → waits for collector to process → emits 2 → waits → emits 3
Result: collector processes every value, but slow collector blocks producer
```

### 8.1 `buffer`

Adds a channel buffer between producer and collector. Producer can keep emitting without waiting.

```kotlin
flow {
    emit(1); emit(2); emit(3); emit(4); emit(5)  // fast emitter
}
.buffer(capacity = 10)    // holds up to 10 unprocessed items
.collect { value ->
    delay(1000)            // slow collector — doesn't block the producer
    processValue(value)
}
// Producer emits all 5 immediately; collector processes one per second
```

### 8.2 `conflate`

Like StateFlow — skips intermediate values. Collector only processes the **latest** value.

```kotlin
flow {
    emit(1); delay(100); emit(2); delay(100); emit(3)
}
.conflate()
.collect { value ->
    delay(250)    // slower than producer
    println(value)
}
// Prints: 1, 3  (2 is skipped — it was already stale by processing time)
```

### 8.3 `collectLatest`

If a new value arrives **while the collector is still processing**, the current collector block is cancelled and restarted with the new value.

```kotlin
// If user types again → this block is CANCELLED and restarted
searchQueryFlow
    .collectLatest { query ->
        val result = api.search(query)    // in-flight request is cancelled too
        updateUI(result)
    }
// Result: only the latest search ever completes — all intermediate ones are cancelled
```

### Backpressure Comparison

| Strategy | Producer blocked? | What happens to intermediate values | Use for |
|---|---|---|---|
| Default (sequential) | ✅ Yes — waits for collector | All processed in order | Most cases |
| `buffer()` | ❌ No — has a tank | All buffered, then processed | Fast producer, slow consumer |
| `conflate()` | ❌ No | Skipped — only latest | UI updates, sensor data |
| `collectLatest` | ❌ No | Collector is cancelled and restarted | Search, autocomplete |

---

## 9. flowOn — Thread Switching

> [!IMPORTANT]
> `flowOn` changes the dispatcher for everything **UPSTREAM** of where it's placed. The `collect {}` block always runs on the **caller's** coroutine context. This is the opposite of what many people expect.

```kotlin
flow {
    // Runs on Dispatchers.IO (set by flowOn below)
    val data = heavyNetworkCall()
    emit(data)
}
.map { it.transform() }       // also runs on IO
.flowOn(Dispatchers.IO)       // ← everything ABOVE runs on IO
.map { it.toLightweightForm() } // runs on MAIN (caller's context)
.collect { result ->
    // Runs on MAIN — safe to update UI
    updateUI(result)
}
```

### Multiple `flowOn` in One Chain

```kotlin
flow { emit(fetchFromNetwork()) }    // IO
    .flowOn(Dispatchers.IO)
    .map { process(it) }             // Default (CPU)
    .flowOn(Dispatchers.Default)
    .collect { updateUI(it) }        // Main
```

> [!CAUTION]
> **Never use `withContext()` inside a `flow { }` builder** — it throws `IllegalStateException`. Use `flowOn()` instead.
> ```kotlin
> // ❌ WRONG
> val myFlow = flow {
>     withContext(Dispatchers.IO) {   // throws IllegalStateException!
>         emit(fetchData())
>     }
> }
>
> // ✅ CORRECT
> val myFlow = flow {
>     emit(fetchData())   // fetchData() can use withContext internally
> }.flowOn(Dispatchers.IO)
> ```

---

## 10. Error Handling

### `catch` — Handle Upstream Errors

```kotlin
flow {
    emit("start")
    throw RuntimeException("Network error!")
    emit("never reached")
}
.catch { exception ->
    // Catches any exception from UPSTREAM (not from collect{})
    emit("fallback value")           // optional: emit a fallback
    // OR: throw exception            // re-throw to propagate
    // OR: just log and complete      // silently handle
}
.collect { value ->
    println(value)
}
// Prints: "start", "fallback value"
```

> [!CAUTION]
> `catch` does NOT catch exceptions thrown inside the `collect {}` lambda — only upstream exceptions. Wrap the `collect` body in try/catch if needed.

### `retry` — Automatic Retry on Failure

```kotlin
flow {
    emit(api.fetchData())   // might fail
}
.retry(retries = 3) { exception ->
    // Return true to retry, false to stop
    exception is IOException    // only retry on network errors
}
.catch { emit(cachedData) }     // if all retries fail, use cache
.collect { data -> updateUI(data) }
```

### `retryWhen` — Retry with Delay

```kotlin
flow {
    emit(api.fetchData())
}
.retryWhen { cause, attempt ->
    if (cause is IOException && attempt < 3) {
        delay(2.0.pow(attempt.toDouble()).toLong() * 1000)  // exponential backoff
        true    // retry
    } else {
        false   // give up
    }
}
```

### Error Handling Placement Rules

```
flow { ... }
    .map { ... }          ← errors here are caught by catch below
    .catch { ... }        ← catches errors from EVERYTHING above
    .onCompletion { ... } ← runs regardless (success, error, cancel)
    .collect { ... }      ← errors here are NOT caught by catch above
```

---

## 11. Terminal Operators

Terminal operators **start** the flow and consume it. They are suspend functions (except `launchIn`).

### 11.1 `collect`

The most fundamental terminal operator. Processes every emitted value.

```kotlin
// Consume all values
flow.collect { value -> process(value) }

// collect with no-op body (just drain the flow)
flow.collect()
```

### 11.2 `first` / `firstOrNull`

```kotlin
// first — get first value then cancel the flow. Throws NoSuchElementException if empty.
val firstUser = usersFlow.first()
val firstAdult = usersFlow.first { it.age >= 18 }   // with predicate

// firstOrNull — same but returns null instead of throwing
val firstUser = usersFlow.firstOrNull()
val firstAdult = usersFlow.firstOrNull { it.age >= 18 }
```

> [!TIP]
> `first()` cancels the flow after receiving one value — it's efficient, the producer stops.

### 11.3 `single` / `singleOrNull`

```kotlin
// single — flow must emit EXACTLY one value. Throws if 0 or more than 1.
val user = userFlow.single()

// singleOrNull — returns null if 0 values, throws if more than 1
val user = userFlow.singleOrNull()

// Example of each case:
flowOf(42).single()           // ✅ returns 42
emptyFlow<Int>().single()     // ❌ throws NoSuchElementException
flowOf(1, 2).single()         // ❌ throws IllegalArgumentException

flowOf(42).singleOrNull()     // ✅ returns 42
emptyFlow<Int>().singleOrNull() // ✅ returns null
flowOf(1, 2).singleOrNull()   // ❌ throws IllegalArgumentException
```

> [!TIP]
> Use `single()` when you query by ID and expect exactly one result — fail fast if the data is corrupt.

### 11.4 `last` / `lastOrNull`

```kotlin
// last — collect the entire flow, return the last value. Throws if empty.
val lastItem = flow.last()

// lastOrNull — same but returns null if the flow is empty
val lastItem = flow.lastOrNull()

// Example:
flowOf(1, 2, 3).last()        // returns 3 (entire flow runs to completion)
emptyFlow<Int>().lastOrNull() // returns null
```

### 11.5 `toList` / `toSet`

```kotlin
// toList — collect all values into a List<T> (only for finite flows!)
val list: List<Int> = finiteFlow.toList()
// [1, 2, 2, 3]

// toSet — collect all values into a Set<T> (deduplicates)
val set: Set<Int> = finiteFlow.toSet()
// {1, 2, 3}
```

> [!CAUTION]
> Never call `toList()` on an infinite flow — it will suspend forever and eventually cause an OOM.

### 11.6 `fold` / `reduce`

```kotlin
// fold — reduce to a single value, starting from initial
val sum = flowOf(1, 2, 3).fold(0) { acc, value -> acc + value }  // 6
val product = flowOf(1, 2, 3).fold(1) { acc, value -> acc * value }  // 6

// reduce — same as fold, but uses the first element as the initial accumulator
val sum = flowOf(1, 2, 3, 4, 5).reduce { acc, value -> acc + value }  // 15
// reduce throws NoSuchElementException if the flow is empty
```

### 11.7 `count`

```kotlin
val total = flowOf(1, 2, 3, 4, 5).count()                     // 5
val evenCount = flowOf(1, 2, 3, 4, 5).count { it % 2 == 0 }   // 2
```

### 11.8 `launchIn`

The only **non-suspending** terminal operator. Starts collection in a given scope and returns a `Job`.

```kotlin
// Non-suspending; starts collection in background
val job = flowOf(1, 2, 3)
    .onEach { println(it) }
    .launchIn(viewModelScope)
// The flow starts immediately, and `job` can be cancelled later

// Equivalent to:
val job = viewModelScope.launch {
    flowOf(1, 2, 3).onEach { println(it) }.collect()
}
```

> [!TIP]
> `launchIn` paired with `onEach` is the idiomatic replacement for `collect` in `viewModelScope.launch { ... }` blocks. It's more concise and returns a cancellable `Job`.

### Terminal Operator Summary

| Operator | Returns | Throws if empty | Cancels early | Use for |
|---|---|---|---|---|
| `collect` | `Unit` | No | No | Full consumption |
| `first()` | `T` | ✅ NoSuchElementException | ✅ Yes | First value |
| `firstOrNull()` | `T?` | No — returns null | ✅ Yes | First value safely |
| `single()` | `T` | ✅ NoSuchElementException | N/A | Expect exactly one |
| `singleOrNull()` | `T?` | No — returns null | N/A | At most one |
| `last()` | `T` | ✅ NoSuchElementException | No | Last value |
| `lastOrNull()` | `T?` | No — returns null | No | Last value safely |
| `toList()` | `List<T>` | No — empty list | No | Collect all |
| `toSet()` | `Set<T>` | No — empty set | No | Deduplicated all |
| `fold(init)` | `R` | No | No | Aggregation |
| `reduce` | `T` | ✅ NoSuchElementException | No | Aggregation (no init) |
| `count()` | `Int` | No — returns 0 | No | Count emissions |
| `launchIn(scope)` | `Job` | N/A | N/A | Fire-and-forget |

---

## 12. Cold → Hot: stateIn and shareIn

Repositories return cold Flows. ViewModels should expose hot StateFlows. Here's the bridge:

### 12.1 `stateIn`

Converts a cold Flow → `StateFlow`. Shares one upstream execution, stores the latest value.

```kotlin
class HomeViewModel(private val repo: UserRepository) : ViewModel() {

    val users: StateFlow<List<User>> = repo.getUsersFlow()  // cold Flow from Room/API
        .map { users -> users.filter { it.isActive } }      // transform
        .catch { emit(emptyList()) }                         // error fallback
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
```

### 12.2 `shareIn`

Converts a cold Flow → `SharedFlow`. Broadcasts to multiple collectors.

```kotlin
val sharedUpdates: SharedFlow<Update> = repo.getUpdates()
    .shareIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        replay = 1
    )
```

### SharingStarted Options

| Option | Starts | Stops | Use for |
|---|---|---|---|
| `Eagerly` | Immediately on creation | Never | Always-needed streams |
| `Lazily` | First collector | Never | One-time setup |
| `WhileSubscribed(5000)` | First collector | 5s after last collector | **Android UI (standard)** |

**Why 5 seconds?** Screen rotation destroys and recreates the UI — briefly leaving 0 collectors. 5 seconds bridges this gap without cancelling and restarting expensive upstream flows (DB queries, network calls).

### stateIn vs shareIn

| | `stateIn` | `shareIn` |
|---|---|---|
| Returns | `StateFlow<T>` | `SharedFlow<T>` |
| Stores latest value | ✅ Yes | Optional (`replay` param) |
| New subscriber gets | Latest value immediately | Only new emissions (or `replay` values) |
| Use for | UI state | Events / broadcasts |

---

## 13. Repository Layer Architecture

This is the full clean architecture pattern using cold Flow in the data layer and StateFlow in the ViewModel:

```
┌─────────────────────────────────────────────────┐
│              UI (Fragment / Composable)          │
│  repeatOnLifecycle { vm.uiState.collect { } }   │
└──────────────────────┬──────────────────────────┘
                       │ observes StateFlow
┌──────────────────────▼──────────────────────────┐
│                    ViewModel                     │
│  val users = repo.getUsers()                    │
│      .stateIn(viewModelScope, WhileSub, list)   │
└──────────────────────┬──────────────────────────┘
                       │ subscribes to cold Flow
┌──────────────────────▼──────────────────────────┐
│                  Repository                      │
│  fun getUsers(): Flow<List<User>> = flow {      │
│      emitAll(dao.getUsers())   // cache first   │
│      val fresh = api.getUsers()                 │
│      dao.insertAll(fresh)      // save fresh    │
│      // Room Flow auto-emits on DB change       │
│  }.flowOn(Dispatchers.IO)                       │
└──────────────────────┬──────────────────────────┘
                  ┌────┴─────┐
    ┌─────────────▼──┐  ┌────▼───────────┐
    │  Room Database  │  │  Retrofit API  │
    └────────────────┘  └────────────────┘
```

### Full Code Implementation

```kotlin
// --- Data Layer: Dao ---
@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<User>>   // Room returns cold Flow that updates on DB change

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<User>)

    @Delete
    suspend fun delete(user: User)
}

// --- Data Layer: API ---
interface UserApi {
    @GET("users")
    suspend fun getUsers(): List<User>
}

// --- Domain Layer: Repository ---
class UserRepository(
    private val dao: UserDao,
    private val api: UserApi
) {
    // Returns cold Flow — doesn't start until ViewModel subscribes
    fun getUsers(): Flow<List<User>> = flow {
        emitAll(dao.getAllUsers())      // emit from local DB immediately (fast)
        try {
            val fresh = api.getUsers() // fetch from network (slow)
            dao.insertAll(fresh)       // save to DB
            // Room Flow auto-emits when DB changes — no need to emit again!
        } catch (e: IOException) {
            // Network failed — that's OK, we already served the cached data
        }
    }.flowOn(Dispatchers.IO)   // entire chain runs on IO thread

    suspend fun deleteUser(user: User) {
        dao.delete(user)               // Room's Flow updates automatically
    }
}

// --- Presentation Layer: ViewModel ---
class UserViewModel(private val repo: UserRepository) : ViewModel() {

    // Cold Flow → Hot StateFlow (lifecycle-safe)
    val users: StateFlow<List<User>> = repo.getUsers()
        .catch { emit(emptyList()) }   // never crash the UI on error
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Events (one-time actions)
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun deleteUser(user: User) {
        viewModelScope.launch {
            repo.deleteUser(user)
            _events.emit("${user.name} deleted")
        }
    }
}

// --- UI Layer: Fragment (XML) ---
class UserFragment : Fragment(R.layout.fragment_user) {
    private val vm: UserViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Safe collection — automatically pauses in background
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.users.collect { users ->
                        adapter.submitList(users)
                    }
                }
                launch {
                    vm.events.collect { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

// --- UI Layer: Composable (Jetpack Compose) ---
@Composable
fun UserScreen(vm: UserViewModel = viewModel()) {
    val users by vm.users.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(vm.events, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.events.collect { message ->
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
        LazyColumn {
            items(users, key = { it.id }) { user ->
                UserItem(user, onDelete = { vm.deleteUser(user) })
            }
        }
    }
}
```

---

## 14. Testing Flows with Turbine

Standard coroutine testing of Flows is verbose. **Turbine** (`app.cash.turbine:turbine`) makes it simple:

```kotlin
// build.gradle.kts
testImplementation("app.cash.turbine:turbine:1.2.0")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
```

### Testing Cold Flow

```kotlin
@Test
fun `getUsers emits cached then fresh data`() = runTest {
    val fakeDao = FakeUserDao(cachedUsers = listOf(User("Alice")))
    val fakeApi = FakeUserApi(freshUsers = listOf(User("Alice"), User("Bob")))
    val repo = UserRepository(fakeDao, fakeApi)

    repo.getUsers().test {
        // First emission: cached data
        assertEquals(listOf(User("Alice")), awaitItem())

        // Second emission: fresh data saved to DB, Room emits again
        assertEquals(listOf(User("Alice"), User("Bob")), awaitItem())

        cancelAndIgnoreRemainingEvents()
    }
}
```

### Testing StateFlow

```kotlin
@Test
fun `increment updates count state`() = runTest {
    val viewModel = CounterViewModel()

    viewModel.uiState.test {
        // Assert initial value (StateFlow always emits current on subscribe)
        assertEquals(0, awaitItem().count)

        // Trigger action
        viewModel.increment()

        // Assert next emission
        assertEquals(1, awaitItem().count)

        // Cancel the flow (StateFlow never completes naturally)
        cancelAndIgnoreRemainingEvents()
    }
}
```

### Testing SharedFlow Events

```kotlin
@Test
fun `deleteUser emits deleted event`() = runTest {
    val vm = UserViewModel(fakeRepository)
    val user = User("Alice")

    vm.events.test {
        vm.deleteUser(user)
        assertEquals("Alice deleted", awaitItem())
        cancelAndIgnoreRemainingEvents()
    }
}
```

### Turbine API Reference

| Function | Purpose |
|---|---|
| `awaitItem()` | Wait for the next emission and return it |
| `awaitComplete()` | Wait for the flow to complete |
| `awaitError()` | Wait for the flow to throw an exception |
| `cancelAndIgnoreRemainingEvents()` | Cancel collection, ignore leftover emissions |
| `expectNoEvents()` | Assert that no events have fired (yet) |
| `turbineScope { }` | Test multiple flows concurrently |

---

## 15. Common Mistakes

### Mistake 1: `withContext` inside flow builder

```kotlin
// ❌ WRONG — throws IllegalStateException
val myFlow = flow {
    withContext(Dispatchers.IO) {
        emit(fetchData())
    }
}

// ✅ CORRECT
val myFlow = flow {
    emit(fetchData())
}.flowOn(Dispatchers.IO)
```

### Mistake 2: Heavy operations in collect on Main thread

```kotlin
// ❌ WRONG — sorting on main thread = UI jank
vm.users.collect { list ->
    val sorted = list.sortedBy { it.name }   // runs on Main thread!
    adapter.submitList(sorted)
}

// ✅ CORRECT — move sorting upstream
val sortedUsers = repo.getUsers()
    .map { list -> list.sortedBy { it.name } }  // runs on IO via flowOn
    .flowOn(Dispatchers.IO)
    .stateIn(...)
```

### Mistake 3: Forgetting `awaitClose` in `callbackFlow`

```kotlin
// ❌ WRONG — flow completes immediately, no callbacks ever fire
fun locationFlow() = callbackFlow {
    val callback = LocationCallback { trySend(it) }
    locationManager.addCallback(callback)
    // ← missing awaitClose: flow completes now, callback fires into the void
}

// ✅ CORRECT
fun locationFlow() = callbackFlow {
    val callback = LocationCallback { trySend(it) }
    locationManager.addCallback(callback)
    awaitClose { locationManager.removeCallback(callback) }
}
```

### Mistake 4: Collecting cold Flow directly in UI without lifecycle protection

```kotlin
// ❌ WRONG — keeps collecting even in background
lifecycleScope.launch {
    vm.users.collect { ... }   // never pauses!
}

// ✅ CORRECT
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        vm.users.collect { ... }   // pauses at STOPPED, resumes at STARTED
    }
}
// ✅ EVEN BETTER in Compose
val users by vm.users.collectAsStateWithLifecycle()
```

### Mistake 5: Multiple `collect` calls creating multiple cold executions

```kotlin
// ❌ WRONG — two collect calls = two separate executions of the cold flow
// If flow makes an API call, it's called twice!
launch { vm.users.collect { updateList(it) } }
launch { vm.users.collect { updateBadge(it) } }

// ✅ CORRECT — convert to StateFlow first (single upstream execution, multiple UI observers)
val users = repo.getUsers().stateIn(viewModelScope, WhileSubscribed(5000), emptyList())
// Now multiple Compose composables can read `users` without duplicate API calls
```

### Mistake 6: `flowOf(list)` vs `list.asFlow()`

```kotlin
// ❌ WRONG — emits ONE value: the whole List object
val wrongFlow = flowOf(listOf(1, 2, 3))

// ✅ CORRECT — emits THREE values: 1, 2, 3
val correctFlow = listOf(1, 2, 3).asFlow()
```

### Mistake 7: Calling `toList()` on an infinite flow

```kotlin
// ❌ WRONG — suspends forever, then OOM
val all = infiniteTimerFlow.toList()

// ✅ CORRECT — use take() first
val first10 = infiniteTimerFlow.take(10).toList()
```

---

## 16. Interview Q&A

**Q: What is the difference between a cold Flow and a hot StateFlow?**
> A cold `Flow` runs its builder code fresh for every collector — 3 collectors = 3 independent executions. If the flow makes an API call, that call happens 3 times. `StateFlow` is hot — it runs once, shares the result, and all collectors receive the same emissions. New collectors immediately get the latest stored value.

**Q: What is `callbackFlow` and when do you use it?**
> `callbackFlow` converts traditional callback-based APIs (LocationManager, Firebase, Bluetooth) into Kotlin Flows. It uses a Channel internally so `trySend()` can be called from regular (non-suspend) callback interfaces. The mandatory `awaitClose {}` block suspends the flow until the collector cancels, and must unregister the callback to prevent memory leaks.

**Q: What does `flowOn` do and why can't you use `withContext` inside a flow builder?**
> `flowOn` changes the coroutine dispatcher for all operations upstream of where it's placed. `withContext` is for changing context temporarily inside a coroutine, but inside a `flow {}` builder the coroutine is the flow itself — using `withContext` would try to change the context of a structured concurrency scope in a way that violates Flow's emission guarantees.

**Q: What happens when a Flow producer emits faster than the collector can consume?**
> Flow is sequential by default — the producer suspends and waits for the collector to finish processing before emitting the next value. If you want to decouple them: `buffer()` adds a buffer tank so the producer can keep emitting; `conflate()` drops intermediate values and the collector only processes the latest; `collectLatest {}` cancels the collector's current work when a new value arrives and restarts with the new value.

**Q: Why is `SharingStarted.WhileSubscribed(5000)` the standard for Android?**
> It starts the upstream cold flow when the first UI subscriber appears, and stops it 5 seconds after the last subscriber leaves. The 5-second buffer is critical for screen rotation — the Activity is destroyed and recreated, briefly leaving 0 subscribers. Without the delay, the upstream flow (like a database query) would be cancelled and restarted mid-rotation, wasting resources. 5 seconds is much longer than any rotation takes (~1-2 seconds).

**Q: What is the difference between `zip` and `combine`?**
> `zip` pairs emissions 1-to-1 — it waits for both flows to emit, then combines the pair. It emits once per pair and stops when the shorter flow completes. `combine` emits every time either flow emits, using the most recent value from each. It doesn't wait for both to emit simultaneously — each emission triggers a new combined output.

**Q: What is the difference between `flatMapLatest`, `flatMapMerge`, and `flatMapConcat`?**
> All three transform each emission into an inner flow. `flatMapLatest` cancels the previous inner flow when a new outer value arrives — useful for search. `flatMapMerge` runs all inner flows concurrently — useful for parallel work where order doesn't matter. `flatMapConcat` runs them sequentially — each inner flow must complete before the next starts.

**Q: What is `scan` and how is it different from `fold`?**
> Both accumulate values with an accumulator function. `fold` is a terminal operator — it waits for the flow to complete and returns one final value. `scan` is an intermediate operator — it emits the accumulated value after every emission, so you get a stream of running totals. Use `scan` to build up state incrementally in the UI (e.g., accumulate a list of events).

**Q: What is `single()` and when would you use it vs `first()`?**
> `first()` gets the first value and immediately cancels the rest of the flow — it works fine if the flow emits one or many values. `single()` asserts that the flow emits exactly one value — it throws an exception if it gets zero or more than one. Use `single()` when querying by a unique ID where you expect exactly one result and want to fail fast if the data is inconsistent.

---

## 17. Remaining Operator Reference

This section covers additional operators that appear in practice but weren't covered in depth above.

### `withIndex()`

Wraps each emitted value with its zero-based index.

```kotlin
flowOf("a", "b", "c")
    .withIndex()
    .collect { (index, value) -> println("$index: $value") }
// 0: a, 1: b, 2: c
```

### `runningFold` / `runningReduce`

Aliases for `scan`. `runningFold` = `scan` with initial value. `runningReduce` = `scan` without initial.

```kotlin
flowOf(1, 2, 3, 4).runningFold(0) { acc, v -> acc + v }
// emits: 0, 1, 3, 6, 10

flowOf(1, 2, 3, 4).runningReduce { acc, v -> acc + v }
// emits: 1, 3, 6, 10
```

### `chunked(n)` (available via extension)

```kotlin
(1..10).asFlow()
    .chunked(3)   // groups into lists of 3
    .collect { println(it) }
// [1, 2, 3], [4, 5, 6], [7, 8, 9], [10]
```

### `zip` with three or more flows

Use `combine` for 3+ flows:

```kotlin
combine(flowA, flowB, flowC) { a, b, c -> Triple(a, b, c) }
    .collect { (a, b, c) -> println("$a $b $c") }
```

### `onEmpty`

Emits a fallback value if the flow completes without emitting anything.

```kotlin
emptyFlow<String>()
    .onEmpty { emit("Default") }
    .collect { println(it) }
// "Default"
```

### `timeout` (Kotlin 1.9+)

Throws `TimeoutCancellationException` if the flow doesn't emit within the given time.

```kotlin
slowFlow
    .timeout(5.seconds)
    .catch { if (it is TimeoutCancellationException) emit(cachedValue) }
    .collect { updateUI(it) }
```

### `flowOfSuspend { }` pattern — lazy one-shot async

A common pattern to turn a single suspend call into a cold Flow:

```kotlin
// Equivalent using flow:
fun fetchUserFlow(id: String): Flow<User> = flow {
    emit(api.getUser(id))   // single suspend call wrapped in flow
}

// Equivalent using flow builder with error handling:
fun fetchUserFlow(id: String): Flow<Result<User>> = flow {
    emit(Result.success(api.getUser(id)))
}.catch { emit(Result.failure(it)) }
```

---

## 18. Gradle Dependencies

```kotlin
// Cold Flow — included in Kotlin Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

// stateIn + shareIn — included above
// collectAsStateWithLifecycle — for Compose UI
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

// Room Flow support — included with Room
implementation("androidx.room:room-ktx:2.6.1")

// Turbine — Flow testing
testImplementation("app.cash.turbine:turbine:1.2.0")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
```

---

## 19. Connections

- **Hot state**: [[StateFlow]] — Cold Flow → `stateIn()` → StateFlow for ViewModels
- **Hot events**: [[SharedFlow]] — Cold Flow → `shareIn()` → SharedFlow for broadcasts
- **Compose UI**: [[State]] — `collectAsStateWithLifecycle()` and `snapshotFlow`
- **Legacy**: [[LiveData]] — Migration path: `Flow.asLiveData()` to bridge old XML UIs
- **Coroutines**: [[Coroutines/Coroutines in Kotlin Complete Notes]] — `viewModelScope`, `repeatOnLifecycle`, dispatchers
