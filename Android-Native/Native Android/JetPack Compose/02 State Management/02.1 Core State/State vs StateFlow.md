# State vs StateFlow

> [!NOTE]
> `StateFlow` belongs to Kotlin Coroutines, while Compose `State` belongs to Jetpack Compose. For coroutine basics, use [[Coroutines/Coroutines in Kotlin Complete Notes]]. For Flow and SharedFlow details, use [[Android Jetpack Flow Study Notes]].

## 📌 Purpose
Both `MutableState<T>` and `MutableStateFlow<T>` represent a stream of state that can change over time. However, they belong to different ecosystems and handle observation differently. Understanding when to use which is crucial for a clean architecture in modern Android apps.

> [!NOTE]
> - `State<T>` is a Jetpack Compose primitive. Compose's compiler understands it natively and uses it to trigger recompositions.
> - `StateFlow<T>` is a Kotlin Coroutines primitive. It knows nothing about Compose or UI rendering; it just emits values over time.

## ⚖️ Comparison

| Feature | `MutableState<T>` | `MutableStateFlow<T>` |
|---|---|---|
| **Ecosystem** | Jetpack Compose | Kotlin Coroutines |
| **Observation** | Automatic by Compose compiler (`snapshot` system) | Requires `.collect()` or `collectAsStateWithLifecycle()` |
| **Usage Layer** | UI Layer (Composables) | Domain, Data Layer, ViewModels |
| **Initial Value** | Required | Required |
| **Thread Safety** | Designed for Main Thread (UI) | Thread-safe, easily transformed via coroutines |

## ✅ MutableState in Compose (UI Layer)

Compose tracks reads to `State<T>` objects automatically. If you read the value of a `State` inside a Composable, Compose will subscribe to it and recompose when the value changes.

```kotlin
@Composable
fun Counter() {
    // Compose natively understands this. No need to "collect" it.
    var count by remember { mutableIntStateOf(0) }

    Button(onClick = { count++ }) {
        // Just reading the variable tells Compose to recompose this Button when count changes
        Text("Count is $count")
    }
}
```

## 🚀 MutableStateFlow in ViewModels (Architecture)

It is highly recommended to use `StateFlow` in ViewModels instead of Compose `State`. This keeps your ViewModel decoupled from the UI framework (Compose), making it easier to test or share logic (e.g., Compose Multiplatform).

### 1. The ViewModel (Uses StateFlow)
```kotlin
class MyViewModel : ViewModel() {
    // 1. Backing property (MutableStateFlow)
    private val _uiState = MutableStateFlow("Initial")
    
    // 2. Publicly exposed property (StateFlow - immutable to UI)
    val uiState = _uiState.asStateFlow()

    fun updateData() {
        _uiState.value = "Updated Data"
    }
}
```

### 2. The Composable (Converts StateFlow to State)
```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    // 3. Convert StateFlow to Compose State
    val text by viewModel.uiState.collectAsStateWithLifecycle()

    Column {
        Text("State: $text")
        Button(onClick = viewModel::updateData) {
            Text("Update")
        }
    }
}
```

## ⚠️ Common Gotchas

1. **Using Compose `State` in ViewModels:**
   While technically possible (`var state by mutableStateOf(...)`), this couples your ViewModel tightly to Jetpack Compose. If you ever migrate to a non-Compose UI or use KMP (Kotlin Multiplatform) for iOS, that Compose dependency becomes a problem. Stick to `StateFlow` in ViewModels.
   
2. **Forgetting `.value` in StateFlow:**
   With Compose `State` and property delegation (`by`), you can assign directly: `count = 5`. With `MutableStateFlow`, you must assign to the `.value` property: `_stateFlow.value = 5` or use `.update { 5 }`.
   
3. **StateFlow equality checks:**
   Both `State` and `StateFlow` filter out consecutive identical values. If you try to emit the same object instance with mutated properties, it *will not* trigger an update. Always emit a new copy of a data class (`state.copy(name = "new")`).

## 💡 Interview Q&A

**Q: Why shouldn't I use `MutableState` inside my ViewModel?**
A: Using `MutableState` couples your ViewModel to Jetpack Compose UI dependencies. Using `StateFlow` keeps your architecture framework-agnostic, making your business logic easier to unit test, more portable (e.g., Kotlin Multiplatform), and strictly separates concerns.

**Q: How does Compose know when to recompose if I use `StateFlow`?**
A: Compose *doesn't* know how to observe `StateFlow` directly. You must use `collectAsStateWithLifecycle()` (or `collectAsState()`), which creates a Compose `State` object under the hood. It launches a coroutine to collect the `StateFlow` emissions and updates the underlying Compose `State`, which then triggers recomposition.

**Q: Can I use `SharedFlow` instead of `StateFlow` for UI State?**
A: Not for UI State. `StateFlow` always holds a single "current" state and requires an initial value, which perfectly aligns with the concept of UI State. `SharedFlow` is meant for one-time events (like navigation or toasts) because it doesn't hold state and new subscribers won't receive past emissions unless replay is configured.


---

## 🚀 Mastery Deep Dive (Added 2026)

> [!NOTE]
> The following deep dive notes were generated to provide mastery-level understanding, complementing the original notes above.

# Compose State vs Coroutine StateFlow

> [!NOTE]
> Which state container should you use in your ViewModel? `MutableState` (Compose) or `MutableStateFlow` (Coroutines)?
> **Google's official recommendation: Use `StateFlow` in ViewModels, use Compose `State` in composables.**
> This file explains *why* and exactly *how* to bridge them safely.

---

## 🧠 Mental Model — Read This First

**Compose State (`mutableStateOf`) is a camera film.**
It's specifically designed for Compose's Snapshot System. If you read it during composition, Compose records that read. It is intrinsically tied to the UI thread and the Compose runtime. If you put it in your ViewModel, your ViewModel becomes coupled to Compose UI.

**`StateFlow` is a generic radio broadcast.**
It constantly broadcasts the latest value to anyone listening. It doesn't know about Compose. It doesn't know about UIs. It's pure Kotlin logic. You can test it in a pure JVM environment, run it in background threads safely, and use it with XML views if needed.

**The Bridge:** `collectAsStateWithLifecycle()` is the radio receiver that takes the generic broadcast and exposes it as a piece of camera film for Compose to read.

---

## 🆚 Comparison Table

| Feature | `MutableState` (Compose) | `MutableStateFlow` (Coroutines) |
|---|---|---|
| **Where to use it** | Inside `@Composable` functions | Inside `ViewModel` or Repository |
| **Coupling** | Strongly coupled to Compose UI | Pure Kotlin, uncoupled |
| **Testing** | Requires Compose test rules | Simple unit tests (`Turbine`) |
| **Thread Safety** | Must be written from Main thread | Safe to write from any Dispatcher |
| **Observation** | Automatic via Snapshot System | Requires manual `collect` |
| **Default initial value** | Mandatory | Mandatory |
| **Event Replay** | Replays current state to new readers | Replays current state to new collectors |

---

## 🏗️ The Backing Property Pattern

This is the standard, non-negotiable pattern for exposing state from a ViewModel.

```kotlin
class ProfileViewModel : ViewModel() {

    // 1. PRIVATE MutableStateFlow — only the ViewModel can change it
    private val _uiState = MutableStateFlow(ProfileUiState())

    // 2. PUBLIC StateFlow — read-only for the UI
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun updateName(newName: String) {
        _uiState.value = _uiState.value.copy(name = newName)
    }
}
```

### Why do we do this?
- **Encapsulation:** The UI should only ever READ state and send EVENTS. It should never directly mutate the ViewModel's state.
- **Safety:** If the UI had access to `_uiState`, any composable could do `_uiState.value = ...`, breaking Unidirectional Data Flow and making bugs impossible to trace.
- **`asStateFlow()`:** This is a zero-cost cast that returns a read-only interface to the exact same object.

---

## 🔄 Updating StateFlow: `.value` vs `.update { }`

When you have a data class in a `MutableStateFlow`, you have two ways to update it.

### The Problem with `.value`

```kotlin
// ❌ DANGEROUS IN CONCURRENT ENVIRONMENTS
fun incrementScore() {
    // Read the value, then create a copy, then assign it
    _uiState.value = _uiState.value.copy(score = _uiState.value.score + 1)
}
```
If two coroutines run `incrementScore()` at the exact same millisecond:
1. Thread A reads score = 5
2. Thread B reads score = 5
3. Thread A writes score = 6
4. Thread B writes score = 6
**Result: 6 (lost an increment!). This is a race condition.**

### The Solution: `.update { }`

```kotlin
// ✅ ATOMIC AND THREAD-SAFE
fun incrementScore() {
    _uiState.update { currentState ->
        currentState.copy(score = currentState.score + 1)
    }
}
```
`update` uses an atomic compare-and-set loop under the hood. It ensures that if another thread modifies the state while this block is running, it will automatically retry the block with the new state. **Always use `.update { }` for data classes.**

---

## 🌉 The Bridge: `collectAsStateWithLifecycle()`

When you have a `StateFlow` in your ViewModel, how do you read it in Compose?

### ❌ The Old Way (Dangerous)

```kotlin
// ❌ DANGEROUS: Leaks resources in the background
val state by viewModel.uiState.collectAsState()
```
`collectAsState()` keeps the flow active even when the app is in the background (home button pressed). If your Flow is observing a database or location, it will keep draining battery and CPU while the app is invisible.

### ✅ The Modern Way (Safe)

```kotlin
// ✅ SAFE: Pauses collection when app is in background
val state by viewModel.uiState.collectAsStateWithLifecycle()
```
*Requires `androidx.lifecycle:lifecycle-runtime-compose` dependency.*

How it works:
1. Starts collecting the Flow when the composable enters the screen (Lifecycle `STARTED`).
2. **Pauses** collecting if the user presses the Home button (Lifecycle `STOPPED`).
3. **Resumes** collecting when the user returns.
4. Cancels completely when the composable leaves the screen (Lifecycle `DESTROYED`).

```kotlin
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel()) {
    // 1. Collect the generic StateFlow into a Compose State
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 2. Pass the unwrapped data class down (Hoisting)
    ProfileContent(
        state = uiState,
        onNameChange = viewModel::updateName
    )
}
```

---

## 🚫 Why NOT use Compose `State` in ViewModel?

Some developers prefer `mutableStateOf` in ViewModel to avoid typing `.collectAsStateWithLifecycle()` in the UI:

```kotlin
// ❌ ANTI-PATTERN: Compose State in ViewModel
class MyViewModel : ViewModel() {
    var uiState by mutableStateOf(MyState())
        private set
}
```

**Why Google says NO to this:**
1. **Thread Safety:** `MutableState` must be written to from the Main thread (or you must be very careful with the snapshot system). `MutableStateFlow` is thread-safe everywhere.
2. **Layering Violation:** ViewModels belong to the Architecture Components layer. They shouldn't depend on the Compose UI runtime.
3. **Testing:** Testing Compose `State` requires starting the Compose runtime (`ComposeTestRule`), which is slow. Testing `StateFlow` uses pure Kotlin `Turbine` (`uiState.test { ... }`), which is lightning fast.
4. **Reactive Chains:** You can't use powerful Flow operators (`combine`, `map`, `debounce`, `flatMapLatest`) on Compose State.

```kotlin
// ✅ Why StateFlow wins: you can do this easily
val searchResults = searchQuery
    .debounce(300)
    .flatMapLatest { query -> repository.search(query) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
```
*(You cannot do this with Compose `mutableStateOf`!)*

---

## 🔗 Connections

- **State primitives**: [[States]] — how Compose actually reads the result of `collectAsStateWithLifecycle()`
- **Hoisting**: [[State Hoisting Patterns]] — how to structure the UI once you collect the state
- **Flow Operators**: [[Android Jetpack Flow Study Notes]] — how to use `stateIn`, `map`, and `combine` to build the `StateFlow`
- **Compose + Flow Lifecycle**: [[Flow with Compose]] — detailed diagram of when collection pauses and resumes

---

## 💬 Interview Master Q&A

**Q: Should you use `MutableState` or `MutableStateFlow` in a ViewModel, and why?**
> You should use `MutableStateFlow`. ViewModels should not be coupled to the UI framework. `StateFlow` is pure Kotlin, meaning it's highly testable without Compose dependencies, can be easily combined with other Flows using operators like `debounce` or `combine`, and is thread-safe for background updates. Compose `State` is deeply tied to the Compose snapshot system, making it harder to test purely and risky to update from background threads.

**Q: What is the backing property pattern and why is it necessary?**
> The backing property pattern involves a private `MutableStateFlow` (`_uiState`) and a public read-only `StateFlow` (`uiState = _uiState.asStateFlow()`). It is necessary for encapsulation and Unidirectional Data Flow. The ViewModel is the sole owner of the state and the only one that can mutate it. The UI can only observe it and send events back. If the UI had access to the `MutableStateFlow`, it could mutate the state directly, causing race conditions and untraceable bugs.

**Q: Why should you use `collectAsStateWithLifecycle()` instead of `collectAsState()`?**
> `collectAsState()` does not respect the Android Activity lifecycle; it stays active even when the app goes into the background. If the Flow is backed by active work (like a database query, network socket, or location updates), it will continue draining battery and CPU while the app is invisible. `collectAsStateWithLifecycle()` automatically pauses collection when the app drops below the `STARTED` lifecycle state (e.g., user presses Home) and resumes when it returns, preventing resource leaks.

**Q: What is the difference between setting `.value` and using `.update {}` on a `MutableStateFlow`?**
> Setting `.value` involves reading the current state, modifying it, and assigning it back. In a concurrent environment where multiple coroutines might update the state simultaneously, this causes race conditions where updates are lost. `.update {}` uses an atomic compare-and-set mechanism under the hood. If the state changes while the update block is executing, it safely retries the block with the new state, ensuring 100% thread-safe atomic updates. Always use `.update {}` when the new state depends on the previous state.
