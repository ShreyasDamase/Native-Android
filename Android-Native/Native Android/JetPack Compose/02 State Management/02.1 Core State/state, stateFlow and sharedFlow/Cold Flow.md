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

## 📌 Table of Contents
1. [The Cold Flow Contract](#1-cold-flow-contract)
2. [Flow Builders](#2-flow-builders)
3. [callbackFlow — Bridging Legacy APIs](#3-callbackflow)
4. [channelFlow — Concurrent Emissions](#4-channelflow)
5. [Intermediate Operators](#5-operators)
6. [Combining Flows](#6-combining-flows)
7. [Backpressure Operators](#7-backpressure)
8. [flowOn — Thread Switching](#8-flowon)
9. [Error Handling](#9-error-handling)
10. [Terminal Operators](#10-terminal-operators)
11. [Cold → Hot: stateIn and shareIn](#11-statein-sharein)
12. [Repository Layer Architecture](#12-repository-architecture)
13. [Testing Flows with Turbine](#13-testing-with-turbine)
14. [Common Mistakes](#14-common-mistakes)
15. [Interview Q&A](#15-interview)

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

---

## 2. Flow Builders

### `flow { }` — The Standard Builder

```kotlin
val timerFlow: Flow<Int> = flow {
    for (i in 1..5) {
        delay(1000)   // suspend — doesn't block the thread
        emit(i)       // send value downstream
    }
}
```

**Rules inside `flow { }`:**
- You CAN call `emit()` to send values
- You CAN call `delay()` or other suspend functions
- You CANNOT use `withContext()` to change the dispatcher (use `flowOn` instead — see §8)
- You CAN call non-suspend functions

### `flowOf()` — Fixed Values

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

### `asFlow()` — Convert Collections

```kotlin
val listFlow: Flow<Int> = listOf(1, 2, 3, 4, 5).asFlow()
val rangeFlow: Flow<Int> = (1..10).asFlow()
val setFlow: Flow<String> = setOf("a", "b", "c").asFlow()
```

### `emitAll()` — Delegate to Another Flow

```kotlin
val flow: Flow<Int> = flow {
    emitAll(listOf(1, 2, 3).asFlow())    // equivalent to emitting each item
    emit(4)
    emitAll(anotherFlow)
}
```

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

| Builder | Concurrency | Use for |
|---|---|---|
| `flow { }` | Single coroutine, sequential | Most cases |
| `callbackFlow { }` | Single coroutine + external callbacks | Legacy API bridging |
| `channelFlow { }` | Multiple coroutines | Concurrent producers |

---

## 5. Intermediate Operators

Operators transform data **as it travels down the pipe**. They are lazy — they don't start the flow, they just add transformation steps.

### Transformation Operators

```kotlin
// map — transform each value (1:1)
flow.map { user -> user.name }

// filter — only let through values that match (1:0 or 1:1)
flow.filter { user -> user.age >= 18 }

// mapNotNull — map and filter nulls in one step
flow.mapNotNull { it.optionalField }

// transform — most powerful: can emit 0, 1, or many values per input
flow.transform { value ->
    emit(value)          // emit original
    emit(value * 2)      // emit transformed version
    if (value > 5) emit(-1)  // conditional emission
}

// onEach — side effects without transforming the value
flow.onEach { value ->
    Log.d("Flow", "Processing: $value")    // logging, analytics, etc.
}
```

### Limiting Operators

```kotlin
// take — complete the flow after N items (cancels producer)
flow.take(5)

// takeWhile — complete when condition becomes false
flow.takeWhile { value -> value < 100 }

// drop — skip the first N items
flow.drop(3)

// dropWhile — skip items while condition is true
flow.dropWhile { value -> value < 0 }
```

### State-Aware Operators

```kotlin
// distinctUntilChanged — skip consecutive duplicate values
flow.distinctUntilChanged()
// 1, 1, 2, 2, 3, 1 → emits: 1, 2, 3, 1

// distinctUntilChangedBy — compare by a specific field
flow.distinctUntilChangedBy { user -> user.email }

// debounce — wait for N ms of silence before emitting (search box use case)
flow.debounce(500)   // only emits if 500ms pass without a new value

// sample — emit latest value every N ms (throttle)
flow.sample(200)   // emit at most once per 200ms
```

### Complete Pipeline Example

```kotlin
// Search screen: user typing → search API call → display results
searchQueryFlow
    .filter { it.length >= 2 }          // only search if ≥ 2 chars
    .distinctUntilChanged()             // skip if query didn't change
    .debounce(300)                      // wait 300ms after user stops typing
    .map { query -> query.lowercase() } // normalize
    .flatMapLatest { query ->           // cancel previous search, start new one
        repository.search(query)        // returns Flow<List<Result>>
    }
    .catch { e -> emit(emptyList()) }  // handle errors gracefully
    .collect { results ->
        updateUI(results)
    }
```

---

## 6. Combining Flows

### zip — 1:1 Pairing

```kotlin
val numbers = flowOf(1, 2, 3)
val letters = flowOf("A", "B", "C")

numbers.zip(letters) { num, letter -> "$num$letter" }
    .collect { println(it) }
// Output: "1A", "2B", "3C"
// If one flow completes, the combined flow completes too
```

### combine — Latest from Each

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

### merge — Interleave Emissions

```kotlin
// Emits values from ALL flows, interleaved as they arrive
val flow1 = flow { emit("A"); delay(100); emit("C") }
val flow2 = flow { delay(50); emit("B"); delay(100); emit("D") }

merge(flow1, flow2).collect { println(it) }
// Output: A, B, C, D (in time order)
```

### flatMapLatest — Cancel and Restart

```kotlin
// Each new value from the outer flow CANCELS the previous inner flow
searchQuery
    .flatMapLatest { query ->
        repository.search(query)    // new search cancels old in-flight search
    }
    .collect { results -> updateUI(results) }

// Use case: search, live weather by city, chat room switching
```

### flatMapMerge — Run Concurrently

```kotlin
// All inner flows run CONCURRENTLY (unlike flatMapLatest)
userIds
    .flatMapMerge(concurrency = 4) { id ->  // at most 4 concurrent
        userRepository.getUser(id)
    }
    .collect { user -> processUser(user) }
```

### flatMapConcat — Sequential

```kotlin
// Each inner flow runs only AFTER the previous one completes
tasks
    .flatMapConcat { task ->
        processTask(task)   // one at a time, in order
    }
    .collect { result -> handleResult(result) }
```

---

## 7. Backpressure Operators

Backpressure happens when the producer emits faster than the collector can process.

```
Default behavior (sequential):
Producer emits 1 → waits for collector to process → emits 2 → waits → emits 3
Result: collector processes every value, but slow
```

### buffer() — Decouple with a Tank

```kotlin
// Adds a buffer between producer and collector
// Producer can keep emitting into the buffer without waiting
flow {
    emit(1); emit(2); emit(3); emit(4); emit(5)  // fast emitter
}
.buffer(capacity = 10)    // holds up to 10 unprocessed items
.collect { value ->
    delay(1000)            // slow collector — doesn't block the producer
    processValue(value)
}
```

### conflate() — Drop Intermediate Values

```kotlin
// Like StateFlow — skips intermediate values, only processes the latest
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

### collectLatest — Cancel and Restart Collector

```kotlin
// If a new value arrives while the collector is still processing, CANCEL the old work
searchQueryFlow
    .collectLatest { query ->
        // If user types again → this block is CANCELLED
        val result = api.search(query)    // in-flight request is cancelled too
        updateUI(result)
    }
// Result: only the latest search ever completes — all intermediate ones are cancelled
```

| Operator | Producer blocked? | What happens to intermediate values |
|---|---|---|
| Default | ✅ Yes — waits for collector | All processed in order |
| `buffer()` | ❌ No — has a tank | All buffered, then processed |
| `conflate()` | ❌ No | Skipped — only latest processed |
| `collectLatest` | ❌ No | Collector is cancelled and restarted |

---

## 8. flowOn — Thread Switching

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

## 9. Error Handling

### catch — Handle Upstream Errors

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

### retry — Automatic Retry on Failure

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

### retryWhen — Retry with Delay

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

### onCompletion — Always-Run Cleanup

```kotlin
flow { emit(1); emit(2); emit(3) }
    .onCompletion { cause ->
        if (cause != null) {
            println("Flow completed with error: $cause")
        } else {
            println("Flow completed successfully")
        }
        hideLoadingSpinner()   // always hides spinner — success or failure
    }
    .catch { emit(defaultValue) }
    .collect { process(it) }
```

---

## 10. Terminal Operators

Terminal operators **start** the flow and consume it. They are suspend functions.

```kotlin
// collect — consume all values
flow.collect { value -> process(value) }

// first — get first value then cancel
val firstUser = usersFlow.first()

// firstOrNull — same but null-safe
val firstUser = usersFlow.firstOrNull()

// last — collect all, return last value
val lastItem = flow.last()

// toList — collect all into a list (only for finite flows!)
val list: List<Int> = finiteFlow.toList()

// fold — reduce to a single value
val sum = flow.fold(0) { acc, value -> acc + value }

// count — count emissions
val count = flow.count()
val evenCount = flow.count { it % 2 == 0 }
```

---

## 11. Cold → Hot Conversion: stateIn and shareIn

Repositories return cold Flows. ViewModels should expose hot StateFlows. Here's the bridge:

### stateIn — Cold Flow → StateFlow

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

### shareIn — Cold Flow → SharedFlow

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

---

## 12. Repository Layer Architecture

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

## 13. Testing Flows with Turbine

Standard coroutine testing of Flows is verbose. **Turbine** (`app.cash.turbine:turbine`) makes it simple:

```kotlin
// build.gradle.kts
testImplementation("app.cash.turbine:turbine:1.2.0")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
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

---

## 14. Common Mistakes

### Mistake 1: withContext inside flow builder

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

### Mistake 2: Using heavy operations in collect on Main thread

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

### Mistake 3: Forgetting awaitClose in callbackFlow

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
```

### Mistake 5: Calling collect multiple times creating multiple subscriptions

```kotlin
// ❌ WRONG — two collect calls = two separate executions of the cold flow
// If flow makes an API call, it's called twice!
launch { vm.users.collect { updateList(it) } }
launch { vm.users.collect { updateBadge(it) } }

// ✅ CORRECT — convert to StateFlow first (single upstream execution, multiple UI observers)
val users = repo.getUsers().stateIn(viewModelScope, WhileSubscribed(5000), emptyList())
// Now multiple Compose composables can read `users` without duplicate API calls
```

---

## 15. Interview Q&A

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

---

## 📦 Gradle Dependencies

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

## 🔗 Connections

- **Hot state**: [[StateFlow]] — Cold Flow → `stateIn()` → StateFlow for ViewModels
- **Hot events**: [[SharedFlow]] — Cold Flow → `shareIn()` → SharedFlow for broadcasts
- **Compose UI**: [[State]] — `collectAsStateWithLifecycle()` and `snapshotFlow`
- **Legacy**: [[LiveData]] — Migration path: `Flow.asLiveData()` to bridge old XML UIs
- **Coroutines**: [[Coroutines/Coroutines in Kotlin Complete Notes]] — `viewModelScope`, `repeatOnLifecycle`, dispatchers
