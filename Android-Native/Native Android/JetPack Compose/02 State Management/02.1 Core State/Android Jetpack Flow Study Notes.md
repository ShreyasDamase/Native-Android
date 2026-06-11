# 🚀 Android Jetpack — Mastering LiveData, StateFlow, Flow & SharedFlow

> [!NOTE]
> Flow is part of `kotlinx.coroutines`. This file is the source of truth for Flow, StateFlow, and SharedFlow details in this folder. For pure coroutine basics like `launch`, `async`, dispatchers, cancellation, and structured concurrency, use [[Coroutines/Coroutines in Kotlin Complete Notes]].


---

## 📌 Table of Contents
1. [Why This Matters — The Big Picture](#1-why-this-matters)
2. [Reactive Programming Fundamentals](#2-reactive-programming-fundamentals)
3. [LiveData — The OG Observer](#3-livedata)
4. [Kotlin Flow — The Coroutine Stream](#4-kotlin-flow)
5. [StateFlow — The LiveData Killer](#5-stateflow)
6. [SharedFlow — The Event Bus](#6-sharedflow)
7. [Head-to-Head Comparison Table](#7-comparison-table)
8. [ViewModel Integration Patterns](#8-viewmodel-integration-patterns)
9. [UI Collection — Safe Patterns](#9-ui-collection-safe-patterns)
10. [Real-World Architecture Example](#10-real-world-architecture-example)
11. [Common Mistakes & How to Avoid Them](#11-common-mistakes)
12. [Interview Questions & Answers](#12-interview-questions)
13. [Cheat Sheet](#13-cheat-sheet)

---

## 1. Why This Matters

Before Jetpack, Android developers suffered:
- Manual lifecycle management (memory leaks everywhere)
- God Activities with thousands of lines
- Callbacks nested inside callbacks (callback hell)
- No standardized way to observe data

Jetpack's reactive components solve ALL of this. Understanding the difference between `LiveData`, `Flow`, `StateFlow`, and `SharedFlow` is the **#1 skill** that separates junior Android devs from seniors.

The image you shared is a classic demo showing all four in action — each button triggers a different reactive pattern. Let's dissect each one.

---

## 2. Reactive Programming Fundamentals

Reactive programming means: **"Don't ask for data — subscribe to it."**

```
Traditional (Pull):          Reactive (Push):
result = getData()           dataSource.observe { result ->
use(result)                      use(result)
                             }
```

### Core Concepts You Must Know

**Producer** — emits data (ViewModel, Repository, API call)  
**Consumer** — receives data (Fragment, Activity, Composable)  
**Operator** — transforms data in between (`map`, `filter`, `combine`)  
**Lifecycle** — dictates when the consumer listens (STARTED, RESUMED, etc.)

### Coroutines Crash Course (required for Flow)

```kotlin
// CoroutineScope — defines lifetime of coroutines
// viewModelScope — tied to ViewModel lifecycle (auto-cancelled)
// lifecycleScope — tied to Fragment/Activity lifecycle

viewModelScope.launch {
    // suspend functions run here
}

// Flow runs inside coroutine scope
viewModelScope.launch {
    myFlow.collect { value ->
        // received each emission
    }
}
```

---

## 3. LiveData

### What Is It?
`LiveData` is a **lifecycle-aware observable data holder** from the `androidx.lifecycle` package. It was introduced as part of Android Architecture Components in 2017 and was the first first-class solution for safe UI updates.

### The Golden Rule of LiveData
> LiveData ONLY delivers updates when the observer is in an ACTIVE lifecycle state (STARTED or RESUMED). It auto-removes observers when the lifecycle is DESTROYED.

### Setup

```kotlin
// build.gradle (app)
implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.7.0"
implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0"
```

### ViewModel — Producing LiveData

```kotlin
class MainViewModel : ViewModel() {

    // MutableLiveData is the writable version (keep private)
    private val _message = MutableLiveData<String>()
    
    // LiveData is the read-only version (expose publicly)
    val message: LiveData<String> = _message

    fun fetchData() {
        viewModelScope.launch {
            delay(1000) // simulate network
            _message.value = "Hello World!"  // Main thread: use .value
            // _message.postValue("Hello World!") // Background thread: use .postValue
        }
    }
}
```

### Fragment/Activity — Consuming LiveData

```kotlin
class MainActivity : AppCompatActivity() {
    
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Safe: auto-clears when Activity is destroyed
        viewModel.message.observe(this) { message ->
            binding.textView.text = message
        }
        
        binding.btnLiveData.setOnClickListener {
            viewModel.fetchData()
        }
    }
}
```

### LiveData Transformations

```kotlin
// map — transform the value
val upperMessage: LiveData<String> = message.map { it.uppercase() }

// switchMap — swap to a different LiveData based on value
val userId = MutableLiveData<Int>()
val user: LiveData<User> = userId.switchMap { id ->
    userRepository.getUser(id) // returns LiveData<User>
}

// MediatorLiveData — merge multiple sources
val mediator = MediatorLiveData<String>()
mediator.addSource(source1) { value -> mediator.value = value }
mediator.addSource(source2) { value -> mediator.value = value }
```

### LiveData Under the Hood

```
Fragment (RESUMED) ─── observe() ──► LiveData
                                        │
ViewModel updates value                 ▼
                                  Observer notified ✅

Fragment (STOPPED) ─── observe() ──► LiveData
                                        │
ViewModel updates value                 ▼
                                  Observer NOT notified ⛔
                                  (but value is stored)

Fragment returns to RESUMED             ▼
                                  Last value delivered ✅
```

### LiveData Pros & Cons

**Pros:**
- Simple API, very beginner-friendly
- Lifecycle-aware out of the box
- No boilerplate for lifecycle management
- Works perfectly with DataBinding

**Cons:**
- Java-first design (not idiomatic Kotlin)
- No built-in operators (`map` on LiveData is primitive vs Flow)
- No error handling mechanism
- Doesn't work well outside Android (hard to unit test)
- Always replays last value (sometimes you don't want this)
- Configuration changes can trigger re-delivery of old data

### When to Use LiveData
Use LiveData when: you need simple UI state in a ViewModel and don't require complex stream operations. Many teams are migrating away from LiveData to StateFlow, but it's still widely used and completely valid.

---

## 4. Kotlin Flow

### What Is It?
`Flow` is a **cold asynchronous data stream** built on Kotlin Coroutines. "Cold" means it doesn't produce values until someone collects it.

```
Hot Stream (SharedFlow/StateFlow) → emits regardless of collectors
Cold Stream (Flow)                → only emits when collected
```

### The Cold Flow Contract

```kotlin
val coldFlow = flow {
    println("Starting emission") // only prints when collected
    emit(1)
    delay(1000)
    emit(2)
    delay(1000)
    emit(3)
}

// Nothing happens yet — no println, no emission

coldFlow.collect { value ->
    println(value) // NOW it starts: prints "Starting emission", then 1, 2, 3
}
```

**Each new collector gets its own independent execution of the flow body.**

### Building Flows

```kotlin
// flow builder
val myFlow: Flow<Int> = flow {
    for (i in 1..5) {
        delay(500)
        emit(i)
    }
}

// flowOf — emit fixed values
val fixedFlow: Flow<Int> = flowOf(1, 2, 3)

// asFlow — convert collections
val listFlow: Flow<Int> = listOf(1, 2, 3).asFlow()

// channelFlow — for concurrent emissions
val concurrent = channelFlow {
    launch { send(fetchFromNetwork()) }
    launch { send(fetchFromCache()) }
}
```

### Flow Operators — The Power Tools

```kotlin
myFlow
    .filter { it % 2 == 0 }          // only even numbers
    .map { it * 10 }                   // multiply by 10
    .take(3)                           // first 3 items
    .onEach { println("Got: $it") }    // side effects
    .catch { e -> emit(-1) }           // handle errors
    .onCompletion { println("Done") }  // runs when flow ends
    .collect { value ->
        println(value)
    }
```

### Combining Flows

```kotlin
val flow1 = flowOf(1, 2, 3)
val flow2 = flowOf("A", "B", "C")

// zip — pairs items 1:1
flow1.zip(flow2) { num, letter -> "$num$letter" }
    .collect { println(it) } // 1A, 2B, 3C

// combine — emits whenever EITHER flow emits (uses latest from other)
flow1.combine(flow2) { num, letter -> "$num$letter" }
    .collect { println(it) }

// flatMapLatest — cancel previous when new value arrives (great for search)
searchQuery
    .debounce(300)
    .flatMapLatest { query ->
        searchRepository.search(query) // returns Flow<List<Result>>
    }
    .collect { results -> showResults(results) }
```

### Flow Context & Dispatchers

```kotlin
// flowOn — switch upstream dispatcher (NOT downstream)
flow {
    emit(fetchFromNetwork()) // runs on IO
}
.flowOn(Dispatchers.IO)     // ← this applies to everything ABOVE it
.collect { result ->
    updateUI(result)         // runs on Main (caller's context)
}
```

**Critical Rule:** Never use `withContext` inside a flow builder — use `flowOn` instead.

### Error Handling

```kotlin
flow {
    emit(riskyOperation())
}
.catch { exception ->
    // handle error, optionally emit fallback
    emit(defaultValue)
    // or: throw exception to propagate
}
.collect { value ->
    // only successful values reach here
}
```

### Flow vs LiveData in Repository Layer

```kotlin
// ✅ Preferred: Repository returns Flow
class UserRepository {
    fun getUsers(): Flow<List<User>> = flow {
        emit(db.getUsers())          // emit from cache
        val fresh = api.getUsers()
        db.insertAll(fresh)
        emit(db.getUsers())          // emit fresh data
    }.flowOn(Dispatchers.IO)
}

// ViewModel converts to LiveData if needed (bridge)
val users: LiveData<List<User>> = userRepository
    .getUsers()
    .asLiveData()
```

---

## 5. StateFlow

### What Is It?
`StateFlow` is a **hot, state-holding flow** — it's essentially LiveData but built on Kotlin Coroutines. It always has a value, replays the latest value to new collectors, and is perfect for UI state.

```
StateFlow = LiveData + Kotlin Coroutines + Flow operators
```

### The Key Differences from LiveData

| Feature | LiveData | StateFlow |
|---|---|---|
| Language | Java-first | Kotlin-first |
| Default value | nullable (no default) | requires initial value |
| Lifecycle aware | ✅ built-in | ❌ need repeatOnLifecycle |
| Operators | limited | full Flow operators |
| Unit testing | needs InstantTaskExecutorRule | pure coroutine testing |
| Equality check | no | ✅ skips duplicate values |

### ViewModel — Producing StateFlow

```kotlin
class MainViewModel : ViewModel() {

    // Always keep MutableStateFlow private
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Simple string state
    private val _message = MutableStateFlow("Initial Value")
    val message: StateFlow<String> = _message.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val result = repository.fetchData()
                _message.value = result
                _uiState.value = UiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

// Define a sealed class for UI states
sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val data: String) : UiState()
    data class Error(val message: String) : UiState()
}
```

### Fragment — Consuming StateFlow (The RIGHT Way)

```kotlin
class MainFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ CORRECT: repeatOnLifecycle stops collection when view is STOPPED
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UiState.Idle -> showIdle()
                        is UiState.Loading -> showLoading()
                        is UiState.Success -> showData(state.data)
                        is UiState.Error -> showError(state.message)
                    }
                }
            }
        }

        // ❌ WRONG: this leaks — collect never stops when Fragment goes to background
        // lifecycleScope.launch {
        //     viewModel.uiState.collect { ... }
        // }
    }
}
```

### StateFlow's Equality Trick

```kotlin
val _count = MutableStateFlow(0)

_count.value = 5
_count.value = 5 // ← collector NOT notified (same value, skipped)
_count.value = 6 // ← collector IS notified
```

This is great for performance — no redundant recompositions. But be careful with data classes:

```kotlin
data class User(val name: String, val age: Int)

_user.value = User("Alice", 30)
_user.value = User("Alice", 30) // NOT emitted (structurally equal)
_user.value = _user.value.copy(age = 31) // IS emitted
```

### Converting Flow to StateFlow

```kotlin
// In ViewModel
val users: StateFlow<List<User>> = userRepository
    .getUsers()                       // returns Flow<List<User>>
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // ← gold standard
        initialValue = emptyList()
    )
```

`SharingStarted.WhileSubscribed(5000)` means:
- Start collecting when first subscriber appears
- Keep collecting for 5 seconds after last subscriber leaves (survives config changes)
- Stop collecting when no one is subscribed for >5 seconds

---

## 6. SharedFlow

### What Is It?
`SharedFlow` is a **hot flow that broadcasts to multiple collectors** without holding state. Unlike `StateFlow`, it has no initial value and doesn't replay the last value by default.

**The key mental model:** SharedFlow is an **event bus**. Use it for one-time events like navigation, toasts, snackbars — things that should NOT be re-delivered on configuration change.

### StateFlow vs SharedFlow Decision Tree

```
Does the data represent STATE?     → StateFlow
  (current user, loading flag, list items)

Does the data represent an EVENT?  → SharedFlow  
  (navigate, show toast, show dialog, error popup)
```

### ViewModel — Producing SharedFlow

```kotlin
class MainViewModel : ViewModel() {

    // SharedFlow for one-time events
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    fun onButtonClicked() {
        viewModelScope.launch {
            _events.emit(UiEvent.ShowToast("Operation successful!"))
        }
    }

    fun onLoginClicked() {
        viewModelScope.launch {
            val success = repository.login()
            if (success) {
                _events.emit(UiEvent.NavigateToDashboard)
            } else {
                _events.emit(UiEvent.ShowError("Invalid credentials"))
            }
        }
    }
}

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class ShowError(val message: String) : UiEvent()
    object NavigateToDashboard : UiEvent()
}
```

### Fragment — Consuming SharedFlow

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is UiEvent.ShowError -> showErrorDialog(event.message)
                UiEvent.NavigateToDashboard -> findNavController().navigate(R.id.dashboardFragment)
            }
        }
    }
}
```

### SharedFlow Configuration Parameters

```kotlin
MutableSharedFlow<String>(
    replay = 0,                        // how many past emissions new collectors receive
    extraBufferCapacity = 64,          // buffer size before suspending emitter
    onBufferOverflow = BufferOverflow.DROP_OLDEST // what to do when buffer is full
)
```

**replay values explained:**
- `replay = 0` (default) — new collectors get nothing from the past. Pure event bus.
- `replay = 1` — new collectors get the last emission. Similar to StateFlow but nullable.
- `replay = n` — new collectors get last n emissions.

```kotlin
// SharedFlow with replay=1 ≈ StateFlow but with no initial value
private val _data = MutableSharedFlow<String>(replay = 1)
```

### The SharedFlow Trick — tryEmit vs emit

```kotlin
// emit — suspends if buffer is full (use inside coroutine)
viewModelScope.launch {
    _events.emit(UiEvent.ShowToast("Hello"))
}

// tryEmit — non-suspending, returns false if buffer full (use outside coroutine)
_events.tryEmit(UiEvent.ShowToast("Hello")) // returns Boolean
```

---

## 7. Comparison Table

| Feature | LiveData | Flow (cold) | StateFlow | SharedFlow |
|---|---|---|---|---|
| **Hot/Cold** | Hot | Cold | Hot | Hot |
| **Holds state** | ✅ | ❌ | ✅ | Optional (replay) |
| **Initial value** | No (nullable) | N/A | ✅ Required | ❌ Not required |
| **Lifecycle aware** | ✅ Auto | ❌ Manual | ❌ Manual | ❌ Manual |
| **Multiple collectors** | ✅ | Each gets own stream | ✅ Shared | ✅ Shared |
| **Replay on subscribe** | Last value | Full stream | Last value | Configurable |
| **Duplicate filtering** | ❌ | ❌ | ✅ | ❌ |
| **Error handling** | ❌ | ✅ catch{} | Limited | Limited |
| **Operators** | Very limited | Full | Full (via Flow) | Full (via Flow) |
| **Unit testing** | Needs rule | Pure Kotlin | Pure Kotlin | Pure Kotlin |
| **Best for** | Simple UI state | Data pipelines | UI state | One-time events |
| **Android only?** | ✅ Yes | ❌ Pure Kotlin | ❌ Pure Kotlin | ❌ Pure Kotlin |

---

## 8. ViewModel Integration Patterns

### Pattern 1: Single UI State Class (Recommended)

```kotlin
// One StateFlow to rule them all
data class HomeUiState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val error: String? = null
)

class HomeViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val users = repository.getUsers()
                _uiState.update { it.copy(isLoading = false, users = users) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
```

### Pattern 2: UI State + UI Events (Best Practice)

```kotlin
class HomeViewModel : ViewModel() {

    // STATE — survives config change, should be re-delivered
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // EVENTS — one-time, should NOT be re-delivered
    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    fun onSaveClicked() {
        viewModelScope.launch {
            val success = repository.save()
            if (success) {
                _events.emit(HomeEvent.NavigateBack)
            } else {
                _events.emit(HomeEvent.ShowError("Save failed"))
            }
        }
    }
}
```

### Pattern 3: stateIn for Repository Flows

```kotlin
class HomeViewModel(
    private val repository: UserRepository
) : ViewModel() {

    // Convert repository Flow to StateFlow once
    val users: StateFlow<List<User>> = repository
        .getUsers()                // Flow<List<User>> from Room/API
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
```

---

## 9. UI Collection — Safe Patterns

### The Most Important Pattern in This Entire Guide

```kotlin
// ✅ THE SAFE WAY — always use this
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            // render state
        }
    }
}
```

Why `repeatOnLifecycle`? Because:
- When Fragment goes to background (STOPPED), collection is **paused**
- When Fragment returns to foreground (STARTED), collection **resumes**
- No unnecessary processing in background
- No UI updates attempted on invisible views

### Collecting Multiple Flows Simultaneously

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
            viewModel.uiState.collect { renderState(it) }
        }
        launch {
            viewModel.events.collect { handleEvent(it) }
        }
    }
}
```

### Jetpack Compose — Even Simpler

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    // collectAsStateWithLifecycle is the correct way in Compose
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    when {
        uiState.isLoading -> CircularProgressIndicator()
        uiState.error != null -> ErrorView(uiState.error)
        else -> UserList(uiState.users)
    }
}
```

---

## 10. Real-World Architecture Example

This is what a full feature looks like — from database to UI:

```
┌─────────────────────────────────────────────────────┐
│                     Fragment/Activity                │
│  viewLifecycleOwner.repeatOnLifecycle(STARTED) {    │
│      viewModel.uiState.collect { render(it) }       │
│      viewModel.events.collect { handle(it) }        │
│  }                                                   │
└───────────────────────┬─────────────────────────────┘
                        │ observes
┌───────────────────────▼─────────────────────────────┐
│                     ViewModel                        │
│  val uiState: StateFlow<UiState>                    │
│  val events: SharedFlow<UiEvent>                    │
│  // calls repository, updates state                 │
└───────────────────────┬─────────────────────────────┘
                        │ calls
┌───────────────────────▼─────────────────────────────┐
│                    Repository                        │
│  fun getData(): Flow<Data> = flow {                 │
│      emit(localDb.getData())    // cache first      │
│      val remote = api.getData() // fetch fresh      │
│      localDb.save(remote)                           │
│      emit(localDb.getData())    // emit fresh       │
│  }.flowOn(Dispatchers.IO)                           │
└───────────────────────┬─────────────────────────────┘
                   ┌────┴────┐
      ┌────────────▼──┐  ┌───▼────────────┐
      │  Room Database │  │   Retrofit API  │
      └───────────────┘  └────────────────┘
```

### Full Implementation

```kotlin
// --- Data Layer ---
@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>> // Room supports Flow natively!
}

// --- Repository ---
class UserRepository(
    private val dao: UserDao,
    private val api: UserApi
) {
    fun getUsers(): Flow<List<User>> = flow {
        emitAll(dao.getAllUsers())     // emit from DB (updates automatically)
        try {
            val remote = api.getUsers()
            dao.insertAll(remote)     // save to DB (triggers new DB emission)
        } catch (e: Exception) {
            // DB data still flows, API failure is graceful
        }
    }.flowOn(Dispatchers.IO)
}

// --- ViewModel ---
class UserViewModel(private val repo: UserRepository) : ViewModel() {

    val users: StateFlow<List<User>> = repo.getUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    fun deleteUser(user: User) {
        viewModelScope.launch {
            repo.delete(user)
            _events.emit("User deleted")
        }
    }
}

// --- Fragment ---
class UserFragment : Fragment() {
    private val vm: UserViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.users.collect { adapter.submitList(it) } }
                launch { vm.events.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() } }
            }
        }
    }
}
```

---

## 11. Common Mistakes

### Mistake 1: Leaking Flow collection
```kotlin
// ❌ WRONG — never stops, leaks in background
lifecycleScope.launch {
    viewModel.uiState.collect { ... }
}

// ✅ CORRECT
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { ... }
    }
}
```

### Mistake 2: Using emit() outside coroutine for SharedFlow
```kotlin
// ❌ WRONG — emit is a suspend function
fun onClick() {
    _events.emit(SomeEvent) // compile error
}

// ✅ CORRECT
fun onClick() {
    viewModelScope.launch {
        _events.emit(SomeEvent)
    }
}
```

### Mistake 3: Using StateFlow for one-time events
```kotlin
// ❌ WRONG — StateFlow replays last value. 
// If user rotates device, the toast shows AGAIN.
private val _showToast = MutableStateFlow(false)

// ✅ CORRECT — SharedFlow doesn't replay
private val _showToast = MutableSharedFlow<Unit>()
```

### Mistake 4: flowOn vs withContext in flow builder
```kotlin
// ❌ WRONG
val myFlow = flow {
    withContext(Dispatchers.IO) { // throws IllegalStateException
        emit(fetchData())
    }
}

// ✅ CORRECT
val myFlow = flow {
    emit(fetchData()) // fetchData() can use withContext internally
}.flowOn(Dispatchers.IO)
```

### Mistake 5: Not using asStateFlow() / asSharedFlow()
```kotlin
// ❌ WRONG — exposes mutable type, can be cast and modified externally
val state: StateFlow<String> = _state  // still MutableStateFlow under the hood

// ✅ CORRECT
val state: StateFlow<String> = _state.asStateFlow() // creates true read-only wrapper
```

### Mistake 6: Heavy operations in flow collect
```kotlin
// ❌ WRONG — main thread, UI jank
viewModel.data.collect { list ->
    val sorted = list.sortedBy { it.name } // runs on Main!
    adapter.submitList(sorted)
}

// ✅ CORRECT — use operators to move work off main thread
viewModel.rawData
    .map { list -> list.sortedBy { it.name } } // runs in ViewModel/IO context
    .collect { sorted -> adapter.submitList(sorted) }
```

---

## 12. Interview Questions

**Q: What is the difference between LiveData and StateFlow?**
A: Both hold state and notify observers, but StateFlow is built on Kotlin Coroutines, requires an initial value, uses equality checks to skip duplicate emissions, supports full Flow operators, and is not Android-specific. LiveData auto-manages lifecycle but StateFlow requires `repeatOnLifecycle` wrapper. For new projects, StateFlow is preferred.

**Q: When would you use SharedFlow over StateFlow?**
A: SharedFlow is for events that should fire once and not be replayed (navigation, toasts, dialogs). StateFlow is for persistent state that should survive configuration changes and be re-delivered to new collectors (user data, loading flags, error state).

**Q: What is the difference between hot and cold flows?**
A: Cold flows (regular `Flow`) don't execute until collected — each collector gets an independent execution. Hot flows (`StateFlow`, `SharedFlow`) run independently of collectors, sharing emissions among all active collectors.

**Q: What does repeatOnLifecycle do and why is it important?**
A: It restarts the given block (typically flow collection) when the lifecycle reaches the specified state, and cancels it when the lifecycle falls below that state. Without it, collecting a flow in a background Fragment wastes resources and can cause crashes when updating views that aren't visible.

**Q: What is stateIn() and what does SharingStarted.WhileSubscribed(5000) mean?**
A: `stateIn()` converts a cold `Flow` into a `StateFlow`. `WhileSubscribed(5000)` means the upstream flow starts when the first collector subscribes and stops 5 seconds after the last collector unsubscribes. The 5-second window prevents unnecessary restarts during configuration changes (rotation takes ~1-2 seconds).

**Q: Can you explain the difference between emit() and tryEmit() in SharedFlow?**
A: `emit()` is a suspend function that waits if the buffer is full. `tryEmit()` is non-suspending — it returns false if the buffer is full instead of waiting. Use `emit()` inside coroutines and `tryEmit()` in non-coroutine contexts like callbacks.

---

## 13. Cheat Sheet

```
WHICH ONE TO USE?
─────────────────────────────────────────────────────
Need simple, lifecycle-aware state?              → LiveData
                                                   (legacy, but fine)
                                                   
Need UI state with Kotlin power?                 → StateFlow
                                                   MutableStateFlow(initialValue)
                                                   
Need one-time events (toast, navigate)?          → SharedFlow
                                                   MutableSharedFlow<Event>()
                                                   
Need a data pipeline (Room → API → UI)?          → Flow (cold)
                                                   Convert to StateFlow in ViewModel
                                                   via .stateIn()

GOLDEN RULES
─────────────────────────────────────────────────────
1. Never expose MutableStateFlow/MutableSharedFlow
   → Always expose as StateFlow/SharedFlow via .asStateFlow()/.asSharedFlow()

2. Always use repeatOnLifecycle(STARTED) when collecting in Fragment
   → Prevents leaks and background processing

3. StateFlow = State (survives rotation, re-delivered)
   SharedFlow = Event (one-time, NOT re-delivered)

4. Use .stateIn(viewModelScope, WhileSubscribed(5000), initial) 
   to convert repository Flow to UI-ready StateFlow

5. flowOn() moves UPSTREAM work. collect{} always runs on caller's context.

6. Use update{} lambda for atomic MutableStateFlow updates:
   _state.update { it.copy(loading = true) }

DEPENDENCY VERSIONS (2024)
─────────────────────────────────────────────────────
implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0"
implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.7.0"
implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0"
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
```

---

*"The difference between a good Android developer and a great one is knowing not just HOW to use these APIs, but WHY — and which one fits the problem. Master the mental model, and the code writes itself."*


---

## 🚀 Mastery Deep Dive (Added 2026)

> [!NOTE]
> The following deep dive notes were generated to provide mastery-level understanding, complementing the original notes above.

# Android Jetpack Flow — Complete Study Notes

> [!NOTE]
> `Flow` is Kotlin's reactive streams implementation. It completely replaces RxJava and LiveData. If Coroutines are for *one-shot* async operations (like an API call), Flow is for *multiple* async values over time (like a websocket, location updates, or a database query that updates when data changes).

---

## 🧠 Mental Model — Read This First

**A Flow is a cold water pipe.**
- **Cold:** The pipe does nothing until someone turns on the faucet at the end (`collect`). If you don't collect a Flow, the code inside the builder NEVER runs.
- **Sequential:** Water drops (emissions) travel down the pipe one by one. Drop 2 cannot leave until Drop 1 is processed by the collector.
- **Suspension:** If the collector is slow at processing drops, the entire pipe pauses. The producer waits for the collector.

**StateFlow is a water tank with a gauge.**
- **Hot:** The tank always has water in it, regardless of whether anyone is looking.
- **Stateful:** It always holds exactly one value (the current water level).
- **Conflated:** If the level changes from 1 to 2 to 3 very fast, a slow observer might only see 1 and then 3. Drop 2 is overwritten (conflated).

**SharedFlow is a loudspeaker.**
- **Hot:** The speaker plays music whether anyone is in the room or not.
- **Event-driven:** Once a sound is played, it's gone (unless you configure replay). If you walk into the room late, you missed it.

---

## 🧊 Cold Flows (Standard `Flow`)

### The Basics

```kotlin
// Builder: Defines the pipe
val timerFlow: Flow<Int> = flow {
    println("Pipe turned on!")
    for (i in 1..3) {
        delay(1000) // fake work
        emit(i)     // send down the pipe
    }
}

// Terminal Operator: Turns on the faucet
// Without this, the println above NEVER runs!
coroutineScope.launch {
    timerFlow.collect { value ->
        println("Received $value")
    }
}
```

### Flow Builders
- `flow { emit() }` — The most common, flexible builder.
- `flowOf(1, 2, 3)` — For a fixed set of values.
- `listOf(1, 2, 3).asFlow()` — Convert collections.
- `callbackFlow { }` — Critical for wrapping legacy callback APIs (Firebase, LocationManager).

### `callbackFlow` — Bridging the Legacy World

> [!IMPORTANT]
> This is a highly tested interview concept. `callbackFlow` bridges traditional callback-based APIs into Kotlin Flows. It uses a Channel under the hood so you can `trySend()` from non-suspend callbacks.

```kotlin
fun getLocationFlow(locationManager: LocationManager): Flow<Location> = callbackFlow {
    // 1. Define the callback
    val callback = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // trySend pushes the value into the flow.
            // It doesn't suspend, so it's safe to call from standard interfaces.
            trySend(location)
        }
    }

    // 2. Register the callback
    locationManager.requestLocationUpdates(callback)

    // 3. MANDATORY: Suspend until the flow is cancelled (collector goes away)
    // When cancelled, the block inside awaitClose runs to clean up.
    awaitClose {
        locationManager.removeUpdates(callback) // Prevent memory leaks!
    }
}
```

---

## 🔄 Operators (The Pipe Filters)

Operators transform the data *as it travels down the pipe*. They are intermediate — they don't start the flow.

| Operator | What it does | Example |
|---|---|---|
| `map` | Transforms each value | `map { user -> user.name }` |
| `filter` | Drops values that don't match | `filter { age >= 18 }` |
| `take` | Cancels the flow after N items | `take(5)` |
| `transform` | Can emit multiple times per input | `transform { emit(it); emit(it * 2) }` |
| `catch` | Catches exceptions from UPSTREAM | `catch { emit(BackupData()) }` |
| `onEach` | Side-effects (logging, etc) | `onEach { Log.d("Flow", "Saw $it") }` |

### Backpressure Operators (When the collector is too slow)

Because Flow is sequential, if `emit` is fast but `collect` is slow, the whole pipe slows down.

1. **`buffer()`**: Adds a tank between producer and collector. Producer can keep emitting into the tank without waiting for the collector.
2. **`conflate()`**: Drops intermediate values. If producer emits 1, 2, 3 fast, but collector is still processing 1, the collector will skip 2 and only receive 3.
3. **`collectLatest { }`**: If a new value arrives while the collector is still processing the old one, it **cancels** the collector block and restarts it with the new value. (Great for network requests driven by search UI).

```kotlin
// collectLatest example: Search Debounce
searchQueryFlow
    .debounce(300) // Wait 300ms for user to stop typing
    .collectLatest { query ->
        // If user types again while this is running, this block is CANCELLED
        val results = api.search(query)
        updateUI(results)
    }
```

---

## 🔥 Hot Flows (`StateFlow` & `SharedFlow`)

Cold flows re-run their builder code from scratch for *every* collector. Hot flows run once, and multiple collectors "listen in" to the same broadcast.

### `StateFlow` (The State Holder)

Replaces `LiveData`. Always has a value. Always conflates (if you set 1 then 2 instantly, observers might only see 2).

```kotlin
private val _uiState = MutableStateFlow(UiState())
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// Update state atomically (thread-safe!)
fun increment() {
    _uiState.update { it.copy(count = it.count + 1) }
}
```

### `SharedFlow` (The Event Bus)

For one-time events (navigation, toast messages). By default, it doesn't replay past events. If a collector attaches *after* an event is sent, it misses it.

```kotlin
// replay = 0 means no one gets old events when they attach
private val _events = MutableSharedFlow<UiEvent>()
val events: SharedFlow<UiEvent> = _events.asSharedFlow()

fun showToast() {
    viewModelScope.launch {
        _events.emit(UiEvent.ShowToast("Saved!"))
    }
}
```

---

## 🏗️ `stateIn` and `shareIn` (Cold to Hot Conversion)

Often, your Room database or Retrofit API returns a cold `Flow`. But the UI needs a hot `StateFlow` so that rotating the screen doesn't cause a re-query to the database.

```kotlin
val usersState: StateFlow<List<User>> = repository.getUsersFlow() // Cold Flow
    .map { users -> users.filter { it.isActive } }
    .stateIn(
        scope = viewModelScope,
        // WhileSubscribed(5000) keeps the flow alive for 5 seconds after the UI disappears.
        // This handles configuration changes (rotation) gracefully without restarting the flow!
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList() // StateFlow MUST have an initial value
    )
```

**SharingStarted Options:**
- `Eagerly`: Starts immediately, never stops. Wastes resources if no one is looking.
- `Lazily`: Starts when the first collector arrives, then never stops.
- `WhileSubscribed(5000)`: **(Best Practice for Android)** Starts when the first collector arrives. Stops 5 seconds after the last collector leaves. The 5-second buffer ensures screen rotations don't kill and restart the flow unnecessarily.

---

## 🧪 Testing Flows (Turbine)

Testing Flows with standard coroutines is hard because Flows don't finish. Use **Turbine** (`app.cash.turbine:turbine`).

```kotlin
@Test
fun `test increment updates state`() = runTest {
    val viewModel = MyViewModel()

    // .test is Turbine's extension function
    viewModel.uiState.test {
        // 1. Assert the initial state
        assertEquals(0, awaitItem().count)

        // 2. Trigger an action
        viewModel.increment()

        // 3. Assert the new state
        assertEquals(1, awaitItem().count)

        // 4. Cancel the collector (since StateFlows never complete naturally)
        cancelAndIgnoreRemainingEvents()
    }
}
```

---

## 🔗 Connections

- **Compose integration**: [[Flow with Compose]] — how to safely collect these flows in UI using `collectAsStateWithLifecycle`
- **ViewModel Architecture**: [[State vs StateFlow]] — why we use `MutableStateFlow` instead of Compose `State`
- **Channels**: `callbackFlow` is built on top of Coroutine Channels. See Phase 4 Coroutines notes.

---

## 💬 Interview Master Q&A

**Q: What is the difference between Flow, StateFlow, and SharedFlow?**
> `Flow` is cold — it starts fresh for every collector. If you have 3 collectors, the code block runs 3 times.
> `StateFlow` is hot and stateful — it holds exactly one latest value. It requires an initial value. It conflates fast updates. Multiple collectors share the same state. It is the modern replacement for LiveData.
> `SharedFlow` is hot and event-driven — it broadcasts events to all current subscribers. By default, it has no memory (replay=0), so late subscribers miss past events. It is used for one-time actions like navigation or showing Snackbars.

**Q: Why must you use `awaitClose` inside `callbackFlow`?**
> `callbackFlow` converts a callback API into a Flow. `awaitClose` suspends the coroutine that is running the flow builder. Without it, the flow builder would finish immediately, the coroutine would exit, and the flow would complete before any callbacks could fire. `awaitClose` also provides a mandatory block to unregister the listener (e.g., `removeLocationUpdates`) when the collector cancels the flow. If you forget to unregister, you create a memory leak.

**Q: What is the purpose of `SharingStarted.WhileSubscribed(5000)` in `stateIn`?**
> It tells the StateFlow to stop collecting from its upstream Cold Flow when there are no active UI collectors, but to wait 5 seconds before doing so. On Android, an Activity is destroyed and recreated during screen rotation. For a brief moment, there are 0 collectors. If we stopped immediately, the upstream flow (like a database query) would be cancelled and immediately restarted, wasting CPU and battery. The 5000ms delay bridges the gap of a configuration change perfectly.

**Q: What happens if a Flow producer is faster than the collector?**
> Because Flow is fundamentally sequential and suspends, the producer will suspend and wait for the collector to process the item. This creates natural backpressure. If you don't want the producer to wait, you can use `.buffer()` to decouple them via a channel, `.conflate()` to drop intermediate unread values, or `collectLatest { }` to cancel the slow collector block and restart it with the new value.
