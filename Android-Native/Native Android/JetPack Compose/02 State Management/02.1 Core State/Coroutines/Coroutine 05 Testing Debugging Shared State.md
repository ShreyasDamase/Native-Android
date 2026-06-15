# Coroutine 05 Testing Debugging Shared State

This note covers shared mutable state, testing, debugging, and performance.

---

## Shared Mutable State

### Problem

- Coroutines can run on multiple threads.
- Multiple coroutines updating the same variable can cause race conditions.

Bad:

```kotlin
var counter = 0

coroutineScope {
    repeat(1000) {
        launch(Dispatchers.Default) {
            counter++
        }
    }
}
```

### `Mutex`

- Coroutine-friendly lock.
- Suspends instead of blocking a thread.

```kotlin
val mutex = Mutex()
var counter = 0

coroutineScope {
    repeat(1000) {
        launch(Dispatchers.Default) {
            mutex.withLock {
                counter++
            }
        }
    }
}
```

### Atomic types

- Good for simple thread-safe values.

```kotlin
val counter = AtomicInteger(0)

coroutineScope {
    repeat(1000) {
        launch(Dispatchers.Default) {
            counter.incrementAndGet()
        }
    }
}
```

### State confinement

- Keep state updates on one dispatcher.
- Android UI state is usually updated on Main.
- Prefer immutable data class copies.

```kotlin
_uiState.value = _uiState.value.copy(loading = true)
```

### Interview explanation

- "For shared state, I avoid uncontrolled mutation. I use immutable state, `Mutex`, atomic classes, or confine updates to one dispatcher."

---

## Testing Coroutines

### Main tools

- `runTest`
  - Creates a test coroutine scope.
  - Uses virtual time.
- `StandardTestDispatcher`
  - Controlled execution.
- `UnconfinedTestDispatcher`
  - Eager execution.
- `advanceUntilIdle()`
  - Runs pending coroutine work.
- `advanceTimeBy()`
  - Moves virtual time forward.

### Simple suspend test

```kotlin
@Test
fun loadUsers_returnsUsers() = runTest {
    val users = repository.getUsers()
    assertEquals(2, users.size)
}
```

### Testing delay

```kotlin
@Test
fun delay_isControlledByVirtualTime() = runTest {
    var done = false

    launch {
        delay(5000)
        done = true
    }

    assertFalse(done)
    advanceTimeBy(5000)
    assertTrue(done)
}
```

### Inject dispatchers

```kotlin
class UserRepository(
    private val api: UserApi,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun getUsers(): List<User> = withContext(ioDispatcher) {
        api.getUsers()
    }
}
```

### ViewModel test idea

```kotlin
@Test
fun loadUsers_updatesState() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    val repository = FakeUserRepository(dispatcher)
    val viewModel = UserViewModel(repository)

    viewModel.loadUsers()
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.loading)
    assertEquals(2, viewModel.uiState.value.users.size)
}
```

### Flow testing belongs here and Flow notes

- For Flow basics and operators, see [[Android Jetpack Flow Study Notes]].
- For Compose collection, see [[Flow with Compose]].
- Flow can be tested with `toList()` or Turbine.

```kotlin
@Test
fun flowEmitsValues() = runTest {
    val values = countFlow().toList()
    assertEquals(listOf(1, 2, 3), values)
}
```

### Interview explanation

- "I test coroutine code with `runTest`, inject dispatchers, use virtual time for delays, and use `advanceUntilIdle()` to finish pending work."

---

## Debugging

### Coroutine names

```kotlin
viewModelScope.launch(CoroutineName("LoadUsers")) {
    repository.getUsers()
}
```

### Log current thread

```kotlin
fun threadInfo(): String = Thread.currentThread().name
```

```kotlin
viewModelScope.launch {
    Log.d("Coroutine", "Started on ${threadInfo()}")

    withContext(Dispatchers.IO) {
        Log.d("Coroutine", "IO on ${threadInfo()}")
    }

    Log.d("Coroutine", "Back on ${threadInfo()}")
}
```

### Debug mode

```text
-Dkotlinx.coroutines.debug
```

### Interview explanation

- "For debugging, I use coroutine names, log thread names, and inspect cancellation/error paths."

---

## Performance Checklist

- Do not run CPU-heavy work on Main.
- Do not use `runBlocking` in Android UI.
- Do not launch unlimited coroutines in loops.
- Avoid `GlobalScope`.
- Cancel old jobs for repeated actions.
- Use `Dispatchers.IO` for blocking IO.
- Use `Dispatchers.Default` for CPU-heavy work.
- Use `limitedParallelism()` if too many tasks overload a resource.


---

## 🚀 Mastery Deep Dive (Added 2026)

> [!NOTE]
> The following deep dive notes were generated to provide mastery-level understanding, complementing the original notes above.

# Coroutine 05 — Testing, Debugging & Shared State

> [!NOTE]
> Knowing HOW to write coroutines is half the skill. Knowing how to TEST them is what separates good engineers from great ones. Untested async code is a ticking time bomb.

---

## 🧠 Mental Model — Read This First

**Think of a time-controlled simulation.**

Testing async code normally is like testing a car on a real highway — you have to wait for real traffic, real signals, real time. That's slow and unpredictable.

`runTest` gives you a **test simulation room with a virtual clock**. You control time: jump 5 seconds ahead instantly, pause everything, check state mid-flight. No real waiting. A test that would take 60 seconds with `Thread.sleep` takes milliseconds with virtual time.

---

## 🧪 Testing Coroutines — The Foundation

### The Core Problem: Real Dispatchers Don't Exist in Tests

```kotlin
// ❌ This test will FAIL — viewModelScope uses Dispatchers.Main internally
// But there is no Android UI Main thread in a JVM unit test!
@Test
fun loadUsers_updatesState() {
    val viewModel = UserViewModel(fakeRepository)
    viewModel.loadUsers()   // internally calls viewModelScope.launch { }
    // launch{} schedules work on Main dispatcher... which doesn't exist → exception
    assertEquals(...)
}
```

### Solution: `Dispatchers.setMain()` — Give Tests a Main Dispatcher

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelTest {

    // TestDispatcher to replace the real Main dispatcher
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)   // ← CRITICAL: replaces Dispatchers.Main for tests
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()   // ← CRITICAL: always reset after test
    }

    @Test
    fun loadUsers_updatesState() = runTest {
        val fakeRepo = FakeUserRepository()
        val viewModel = UserViewModel(fakeRepo)

        viewModel.loadUsers()
        advanceUntilIdle()   // run all pending coroutines

        assertEquals(2, viewModel.uiState.value.users.size)
    }
}
```

> [!IMPORTANT]
> Always call `Dispatchers.setMain()` in `@Before` and `Dispatchers.resetMain()` in `@After`. Failing to reset leaves the test dispatcher in place for subsequent tests, causing flaky behavior.

---

## 🔧 The Test Toolkit

### `runTest` — The Main Test Scope

```kotlin
@Test
fun myTest() = runTest {
    // Inside runTest:
    // 1. A TestScope is created with virtual time
    // 2. delay() uses virtual time (no real waiting!)
    // 3. All launched coroutines are tracked
    // 4. At the end, runTest waits for all coroutines to complete

    delay(10_000)   // completes instantly in tests! (virtual time jumps 10 seconds)
    assertEquals("expected", actualValue)
}
```

**`runTest` vs `runBlocking`:**
| | `runTest` | `runBlocking` |
|---|---|---|
| `delay()` behavior | Instant (virtual time) | Real time wait |
| Coroutine leak detection | ✅ Warns if coroutines escape scope | ❌ No check |
| Recommended for | All coroutine tests | Only simple cases |

---

### `StandardTestDispatcher` — Controlled Execution

With `StandardTestDispatcher`, coroutines do NOT run until you explicitly advance time or call `advanceUntilIdle()`. You have full control over execution order.

```kotlin
@Test
fun testWithStandardDispatcher() = runTest(StandardTestDispatcher()) {
    var result = ""

    launch {
        delay(1000)
        result = "done"
    }

    assertEquals("", result)       // coroutine hasn't run yet!

    advanceTimeBy(1000)            // advance virtual time by 1000ms
    // delay(1000) has now "elapsed" — coroutine resumes

    assertEquals("done", result)   // ✅
}
```

---

### `UnconfinedTestDispatcher` — Eager Execution

Coroutines start immediately on the calling thread without needing explicit advancement. Good for most ViewModel tests.

```kotlin
@Test
fun testWithUnconfinedDispatcher() = runTest(UnconfinedTestDispatcher()) {
    var result = ""

    launch {
        delay(1000)
        result = "done"
    }
    // launch{} starts and runs eagerly, virtual delay elapses immediately

    assertEquals("done", result)   // ✅ already done!
}
```

**Which to use?**
- Use `UnconfinedTestDispatcher` (default via `Dispatchers.setMain`) for ViewModel tests — simpler
- Use `StandardTestDispatcher` when you need to test intermediate states (check state DURING an operation, not just before/after)

---

### Time Control Functions

```kotlin
runTest {
    advanceTimeBy(500)        // advance virtual clock by 500ms (doesn't run coroutines yet with Standard)
    runCurrent()              // run all coroutines that are currently ready to run
    advanceUntilIdle()        // advance time + run coroutines until nothing is pending
    currentTime               // property: current virtual time in milliseconds

    // Practical example:
    launch {
        delay(1000)
        _state.value = "loaded"
    }
    assertEquals("", _state.value)    // before
    advanceUntilIdle()
    assertEquals("loaded", _state.value) // after
}
```

---

## 🧑‍🔬 ViewModel Testing — Full Pattern

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ProductViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeProductRepository
    private lateinit var viewModel: ProductViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeProductRepository()
        viewModel = ProductViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loading products shows loading state then success`() = runTest {
        // Arrange: set up fake repo to delay 500ms before returning
        fakeRepository.delay = 500L
        fakeRepository.products = listOf(Product("1", "Phone"), Product("2", "Laptop"))

        // Act
        viewModel.loadProducts()

        // At this point with UnconfinedTestDispatcher, loading state was shown briefly
        // but since we're past delay, it's now in success state
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertIs<ProductUiState.Success>(state)
        assertEquals(2, (state as ProductUiState.Success).products.size)
    }

    @Test
    fun `error response shows error state`() = runTest {
        fakeRepository.shouldThrow = IOException("Network error")

        viewModel.loadProducts()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<ProductUiState.Error>(state)
    }

    @Test
    fun `loading state is shown while fetching`() = runTest(StandardTestDispatcher()) {
        // With StandardTestDispatcher, we can observe intermediate states

        fakeRepository.delay = 1000L
        viewModel.loadProducts()

        // Before any advancement — loading should be shown
        assertEquals(ProductUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()

        // After all coroutines run — should be success
        assertIs<ProductUiState.Success>(viewModel.uiState.value)
    }
}

// Fake repository — controls behavior for tests
class FakeProductRepository : ProductRepository {
    var products: List<Product> = emptyList()
    var delay: Long = 0L
    var shouldThrow: Exception? = null

    override suspend fun getProducts(): List<Product> {
        if (delay > 0) delay(delay)
        shouldThrow?.let { throw it }
        return products
    }
}
```

---

## 🌊 Testing StateFlow — Collecting Multiple States

When you want to verify multiple STATE CHANGES (not just the final state), you need to collect while the ViewModel runs:

```kotlin
@Test
fun `state transitions: Loading → Success`() = runTest {
    val states = mutableListOf<ProductUiState>()

    // Start collecting all state changes in background
    val collectionJob = launch(UnconfinedTestDispatcher()) {
        viewModel.uiState.collect { states.add(it) }
    }

    viewModel.loadProducts()
    advanceUntilIdle()

    collectionJob.cancel()   // stop collecting

    // Verify state transitions happened in order
    assertEquals(3, states.size)
    assertEquals(ProductUiState.Idle, states[0])
    assertEquals(ProductUiState.Loading, states[1])
    assertIs<ProductUiState.Success>(states[2])
}
```

---

## 🌀 Testing Flows with Turbine

**Turbine** is the standard library for testing Flows cleanly. Add it to your test dependencies:

```kotlin
testImplementation("app.cash.turbine:turbine:1.1.0")
```

### Without Turbine (verbose)

```kotlin
@Test
fun flowTest_without_turbine() = runTest {
    val values = mutableListOf<Int>()
    val job = launch(UnconfinedTestDispatcher()) {
        myFlow.collect { values.add(it) }
    }
    advanceUntilIdle()
    job.cancel()
    assertEquals(listOf(1, 2, 3), values)
}
```

### With Turbine (clean and expressive)

```kotlin
@Test
fun flowTest_with_turbine() = runTest {
    myFlow.test {
        assertEquals(1, awaitItem())    // wait for and assert item 1
        assertEquals(2, awaitItem())    // wait for and assert item 2
        assertEquals(3, awaitItem())    // wait for and assert item 3
        awaitComplete()                 // assert flow completed
    }
}

@Test
fun errorFlow_with_turbine() = runTest {
    errorFlow.test {
        assertEquals("initial", awaitItem())
        val error = awaitError()        // assert flow threw an exception
        assertIs<IOException>(error)
    }
}

@Test
fun stateFlow_with_turbine() = runTest {
    viewModel.uiState.test {
        assertEquals(UiState.Loading, awaitItem())     // first emission
        assertEquals(UiState.Success(data), awaitItem()) // second emission

        // cancelAndIgnoreRemainingEvents() if you don't care about subsequent events
        cancelAndIgnoreRemainingEvents()
    }
}
```

**Turbine API:**

| Function | What it does |
|---|---|
| `awaitItem()` | Suspends until the next item is emitted, then returns it |
| `awaitComplete()` | Suspends until the flow completes normally |
| `awaitError()` | Suspends until the flow throws, returns the exception |
| `cancelAndIgnoreRemainingEvents()` | Cancels collection, skips any remaining items |
| `expectMostRecentItem()` | Returns the last emitted item without waiting |

---

## 🔒 Shared Mutable State — Race Conditions and Solutions

### The Problem

Multiple coroutines on multiple threads updating the same variable without synchronization:

```kotlin
// ❌ Race condition — final value is UNPREDICTABLE
var counter = 0

coroutineScope {
    repeat(1000) {
        launch(Dispatchers.Default) {
            counter++   // read-modify-write is NOT atomic — data race!
        }
    }
}
// counter might be 987, 993, 1000 — depends on thread timing
```

### Solution 1: `Mutex` — Coroutine-Safe Lock

```kotlin
val mutex = Mutex()
var counter = 0

coroutineScope {
    repeat(1000) {
        launch(Dispatchers.Default) {
            mutex.withLock {
                counter++   // only one coroutine inside withLock at a time
            }
        }
    }
}
// counter is always 1000 ✅
```

**`Mutex` vs `synchronized`:**
| | `Mutex` | `synchronized` |
|---|---|---|
| Blocking behavior | Suspends coroutine (thread free) | Blocks thread |
| Coroutine-aware? | ✅ Yes | ❌ No |
| Use in coroutines | ✅ Correct choice | ❌ Can deadlock with coroutines |

### Solution 2: `AtomicInteger` — For Simple Counter/Flag State

```kotlin
val counter = AtomicInteger(0)

coroutineScope {
    repeat(1000) {
        launch(Dispatchers.Default) {
            counter.incrementAndGet()   // atomically increment — no lock needed
        }
    }
}
println(counter.get())   // always 1000 ✅
```

**Use atomic types for:** simple counters, boolean flags, simple references. Not suitable for compound operations (read-then-write based on condition).

### Solution 3: Confine to Single Dispatcher (Best for Android)

The cleanest solution in Android is to never share mutable state across threads — do all state mutations on a single thread (usually Main):

```kotlin
class UserViewModel : ViewModel() {
    // All state updates happen on Main thread — no race conditions possible!
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {   // launches on Main (viewModelScope default)
            val data = withContext(Dispatchers.IO) { repo.getData() }   // switch to IO for fetch
            // Automatically back on Main after withContext
            _uiState.update { it.copy(data = data) }   // update on Main — safe!
        }
    }
}
```

### Solution 4: Immutable State + `.update {}` (Preferred Android Pattern)

```kotlin
// StateFlow.update {} is atomic — safe to call from multiple coroutines
private val _uiState = MutableStateFlow(UiState())

// Even if two coroutines call this simultaneously, .update{} handles it safely:
_uiState.update { currentState ->
    currentState.copy(isLoading = true)   // always operates on the LATEST state
}
```

**`_uiState.value = ...` vs `_uiState.update { }`:**
```kotlin
// .value = is a simple assignment — NOT safe for concurrent use
_uiState.value = _uiState.value.copy(count = _uiState.value.count + 1)
// If two coroutines do this simultaneously, one update may be lost (race!)

// .update {} is atomic — safe for concurrent coroutines
_uiState.update { it.copy(count = it.count + 1) }
// Uses CAS (Compare-And-Swap) internally — always applies update to latest state
```

---

## 🐛 Debugging Coroutines

### Coroutine Names — Label Your Coroutines

```kotlin
viewModelScope.launch(CoroutineName("SyncProducts")) {
    repository.syncProducts()
}

viewModelScope.launch(CoroutineName("LoadUser")) {
    repository.getUser()
}
// In logs, you'll see: "SyncProducts" and "LoadUser" instead of generic numbers
```

### Enable Coroutine Debug Mode

Add to your test or debug build's JVM arguments:
```
-Dkotlinx.coroutines.debug
```

This adds the coroutine name and ID to every thread name printed in logs:
```
Thread: DefaultDispatcher-worker-1 @SyncProducts#1
Thread: DefaultDispatcher-worker-2 @LoadUser#2
```

### Log Thread Names for Dispatcher Debugging

```kotlin
fun threadInfo() = "Thread: ${Thread.currentThread().name}"

viewModelScope.launch {
    Log.d("Debug", "launch started: ${threadInfo()}")           // Main

    withContext(Dispatchers.IO) {
        Log.d("Debug", "inside IO: ${threadInfo()}")             // DefaultDispatcher-worker-X
    }

    Log.d("Debug", "back after IO: ${threadInfo()}")            // Main
}
```

### Detecting Coroutine Leaks in Tests

`runTest` automatically fails if any coroutine launched inside it is still running when the test ends:

```kotlin
@Test
fun coroutineLeakTest() = runTest {
    val job = launch {
        delay(Long.MAX_VALUE)   // never completes
    }
    // Test ends here but 'job' is still running → runTest will FAIL with:
    // "After waiting for 60s, no more work was done but the test was not completed"
    // This catches coroutine leaks in your code!
}
```

---

## 🔄 Inject Dispatchers — Make Code Testable

If you hardcode `Dispatchers.IO` in your repository, tests can't control it:

```kotlin
// ❌ Hardcoded dispatcher — tests must deal with real IO thread pool
class UserRepository(private val api: UserApi) {
    suspend fun getUsers(): List<User> = withContext(Dispatchers.IO) {
        api.getUsers()
    }
}

// ✅ Injected dispatcher — tests can provide TestDispatcher
class UserRepository(
    private val api: UserApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO   // default is real IO
) {
    suspend fun getUsers(): List<User> = withContext(ioDispatcher) {
        api.getUsers()
    }
}

// In tests:
val testDispatcher = UnconfinedTestDispatcher()
val repository = UserRepository(fakeApi, ioDispatcher = testDispatcher)
```

---

## 📋 Performance Checklist

- [ ] Never use `Dispatchers.Main` for network, database, or file operations
- [ ] Never use `runBlocking` in Android UI code
- [ ] Never launch unlimited coroutines in a loop (use Channel or `limitedParallelism()`)
- [ ] Never use `GlobalScope` — use lifecycle-aware scopes
- [ ] Cancel previous jobs for repeated user actions (search queries, refresh pulls)
- [ ] Use `Dispatchers.IO` for blocking IO, `Dispatchers.Default` for CPU-heavy work
- [ ] Use `limitedParallelism(n)` on `Dispatchers.IO` when calling APIs with rate limits
- [ ] Inject dispatchers into repositories and ViewModels for testability
- [ ] Add coroutine names in debug builds for easier log tracing

```kotlin
// limitedParallelism — useful for rate-limited APIs
val limitedDispatcher = Dispatchers.IO.limitedParallelism(4)  // max 4 concurrent calls

val results = items.map { item ->
    async(limitedDispatcher) { api.process(item) }
}.awaitAll()
```

---

## ✅ Real App: Full Test Suite for a ViewModel

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `add item to cart updates total price`() = runTest {
        val viewModel = CartViewModel(FakeCartRepository())
        viewModel.addItem(Product("1", "Phone", price = 499.99))
        assertEquals(499.99, viewModel.cartState.value.total, 0.01)
    }

    @Test
    fun `remove item recalculates total`() = runTest {
        val viewModel = CartViewModel(FakeCartRepository())
        viewModel.addItem(Product("1", "Phone", price = 499.99))
        viewModel.addItem(Product("2", "Case", price = 19.99))
        viewModel.removeItem("1")
        assertEquals(19.99, viewModel.cartState.value.total, 0.01)
    }

    @Test
    fun `checkout failure shows error event`() = runTest {
        val repo = FakeCartRepository().apply { shouldFailCheckout = true }
        val viewModel = CartViewModel(repo)

        viewModel.events.test {
            viewModel.checkout()
            val event = awaitItem()
            assertIs<CartEvent.CheckoutFailed>(event)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `checkout success navigates to confirmation`() = runTest {
        val viewModel = CartViewModel(FakeCartRepository())

        viewModel.events.test {
            viewModel.checkout()
            val event = awaitItem()
            assertIs<CartEvent.NavigateToConfirmation>(event)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

---

## 🔗 Connections

- **Previous**: [[Coroutine 04 Channels Producer Consumer]] — Channels and Flow builders
- **Flow testing**: also use Turbine for testing [[Android Jetpack Flow Study Notes]]
- **Testing StateFlow**: applies to [[State vs StateFlow]] patterns
- **All coroutines overview**: [[Coroutines in Kotlin Complete Notes]]

---

## 💬 Interview Master Q&A

**Q: How do you test coroutines in Android?**
> I use the `kotlinx-coroutines-test` library. The three key pieces are: `Dispatchers.setMain(testDispatcher)` to replace the Main dispatcher with a test-controlled one, `runTest` to run the test in a virtual-time coroutine scope where `delay()` is instant, and `advanceUntilIdle()` to run all pending coroutines. I inject dispatchers into my repositories rather than hardcoding `Dispatchers.IO`, so tests can provide a `TestDispatcher`. For asserting Flow emissions, I use the Turbine library which provides `awaitItem()`, `awaitComplete()`, and `awaitError()`.

**Q: What is the difference between `StandardTestDispatcher` and `UnconfinedTestDispatcher`?**
> `StandardTestDispatcher` gives you full manual control — coroutines don't run until you explicitly call `advanceTimeBy()`, `runCurrent()`, or `advanceUntilIdle()`. This lets you verify intermediate states (like checking that loading is shown BEFORE the data arrives). `UnconfinedTestDispatcher` runs coroutines eagerly on the current thread without needing explicit advancement — simpler for tests where you only care about the final state, not intermediate transitions.

**Q: How do you handle shared mutable state safely in coroutines?**
> There are four approaches in order of preference for Android: First, confine state to a single dispatcher (all ViewModel state on Main), which eliminates races entirely. Second, use `StateFlow.update {}` which is atomically safe for concurrent updates. Third, use `Mutex.withLock {}` for cases requiring complex operations that must be atomic. Fourth, use `AtomicInteger` or `AtomicReference` for simple counters or flags. I generally avoid `synchronized` in coroutine code because it blocks the thread rather than suspending the coroutine, which can cause deadlocks.

**Q: What is Turbine?**
> Turbine is a testing library by Cash App specifically for Kotlin Flow. It provides a clean DSL for asserting Flow emissions: `awaitItem()` suspends until the next item arrives and returns it for assertion, `awaitComplete()` verifies the flow finished normally, and `awaitError()` verifies the flow threw an exception. Without Turbine, you'd need to manually launch a collection coroutine, accumulate items into a list, cancel after testing, and then assert on the list — much more boilerplate. Turbine handles all that infrastructure, making Flow tests as simple as `flow.test { assertEquals(expected, awaitItem()) }`.
