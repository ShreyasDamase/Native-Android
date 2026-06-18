# 🔥 StateFlow — Complete Guide

> [!NOTE]
> **Source of truth for StateFlow.** For SharedFlow (events), see [[SharedFlow]]. For Compose State primitives (`remember`, `mutableStateOf`), see [[State]]. For cold Flow theory and operators, see [[Cold Flow]].

---

## 🔍 Master Lookup Table — Find Any Concept Instantly

> Click any item to jump straight to its full explanation, code example, and gotchas.

### Creating & Exposing StateFlow

| API / Concept | One-Line Purpose | Jump To |
|---|---|---|
| `MutableStateFlow(initial)` | Create a writable state holder with an initial value | [[#2. MutableStateFlow vs StateFlow\|§2 MutableStateFlow vs StateFlow]] |
| `.asStateFlow()` | Cast to read-only `StateFlow<T>` — zero-cost compile-time contract | [[#2. MutableStateFlow vs StateFlow\|§2 MutableStateFlow vs StateFlow]] |
| Backing property `_x` / `x` | Pattern: private mutable + public read-only | [[#3. ViewModel Integration & Lifecycle\|§3 ViewModel]] |
| `@HiltViewModel` + `@Inject` | Inject dependencies into ViewModel using Hilt | [[#3. ViewModel Integration & Lifecycle\|§3 ViewModel]] |

### Reading / Collecting StateFlow in UI

| API / Concept                   | One-Line Purpose                                                     | Jump To                                                        |
| ------------------------------- | -------------------------------------------------------------------- | -------------------------------------------------------------- |
| `collectAsStateWithLifecycle()` | Collect StateFlow in Compose, lifecycle-aware (pauses in background) | [[#4. Collecting StateFlow in Compose\|§4 Collecting in Compose]] |
| `collectAsState()`              | Collect StateFlow in Compose — NOT lifecycle-aware (KMP use only)    | [[#4. Collecting StateFlow in Compose\|§4 Collecting in Compose]] |
| `by` delegation                 | Unwraps `State<T>` → `T` automatically — no `.value` needed          | [[#4. Collecting StateFlow in Compose\|§4 Collecting in Compose]] |
| `repeatOnLifecycle(STARTED)`    | Lifecycle-safe collection in XML (Fragment/Activity)                 | [[#4b. Collecting in XML (Fragment/Activity)\|§4b XML Collection]] |

### Writing / Updating State

| API / Concept | One-Line Purpose | Jump To |
|---|---|---|
| `_x.value = newValue` | Direct assignment — fine for simple single-threaded updates | [[#6. copy() and update{}\|§6 copy() and update{}]] |
| `_x.update { it.copy(...) }` | Thread-safe atomic update — preferred in concurrent ViewModels | [[#6. copy() and update{}\|§6 copy() and update{}]] |
| `data class.copy()` | Create a new immutable state object with only selected fields changed | [[#6. copy() and update{}\|§6 copy() and update{}]] |
| Immutable `val` fields in UiState | Required so StateFlow can detect changes via equality | [[#5. Immutable UiState Pattern\|§5 Immutable UiState]] |

### UiState Patterns

| Pattern | One-Line Purpose | Jump To |
|---|---|---|
| Single `data class` UiState | Group all screen state into one object — atomic, consistent updates | [[#5. Immutable UiState Pattern\|§5 Immutable UiState]] |
| `sealed interface UiState` | Mutually exclusive states (Loading/Success/Error) — impossible combos prevented by type system | [[#8. Sealed Class UiState (Loading / Success / Error)\|§8 Sealed Class UiState]] |
| `StateFlow<Boolean>` (loading flag) | Simple boolean flag inside a data class UiState | [[#5. Immutable UiState Pattern\|§5 Immutable UiState]] |
| `combine()` for derived state | Combine 2+ StateFlows into one derived StateFlow (e.g., form validity) | [[#11. Multiple StateFlows & Form Validation\|§11 Form Validation]] |

### Equality & Emission Behavior

| Behavior | One-Line Explanation | Jump To |
|---|---|---|
| Structural equality check `==` | StateFlow suppresses emission if `newValue == oldValue` | [[#7. StateFlow Equality Behavior\|§7 Equality Behavior]] |
| Data class `==` checks all fields | Changing even one field triggers emission | [[#7. StateFlow Equality Behavior\|§7 Equality Behavior]] |
| Mutating a field (var) silently fails | Same object reference → `==` true → NO emission | [[#7. StateFlow Equality Behavior\|§7 Equality Behavior]] |
| Replay = 1 (always) | New collectors immediately get the latest value | [[#14. StateFlow Internals\|§14 Internals]] |

### Cold Flow → StateFlow Conversion

| API / Concept | One-Line Purpose | Jump To |
|---|---|---|
| `.stateIn(scope, started, initial)` | Convert a cold `Flow<T>` → `StateFlow<T>` — single shared upstream | [[#9. Converting Flow → StateFlow with stateIn\|§9 stateIn]] |
| `SharingStarted.WhileSubscribed(5000)` | Standard for Android — starts on first collector, stops 5s after last leaves | [[#9. Converting Flow → StateFlow with stateIn\|§9 stateIn]] |
| `SharingStarted.Eagerly` | Start immediately on creation, never stop | [[#9. Converting Flow → StateFlow with stateIn\|§9 stateIn]] |
| `SharingStarted.Lazily` | Start on first collector, never stop | [[#9. Converting Flow → StateFlow with stateIn\|§9 stateIn]] |

### Operators on StateFlow

| Operator | One-Line Purpose | Jump To |
|---|---|---|
| `.map { }` | Derive a new StateFlow by transforming each value (needs `.stateIn`) | [[#10. StateFlow Operators\|§10 Operators]] |
| `combine(f1, f2) { }` | Merge 2+ flows, emit when either changes | [[#10. StateFlow Operators\|§10 Operators]] |
| `.flatMapLatest { }` | Cancel old inner flow when new value arrives (search use case) | [[#10. StateFlow Operators\|§10 Operators]] |
| `.distinctUntilChanged()` | Already built into StateFlow — no need to add manually | [[#10. StateFlow Operators\|§10 Operators]] |

### Comparisons

| Comparison | Jump To |
|---|---|
| StateFlow vs LiveData | [[#12. StateFlow vs LiveData\|§12 vs LiveData]] |
| StateFlow vs SharedFlow | [[#13. StateFlow vs SharedFlow\|§13 vs SharedFlow]] |
| StateFlow vs MutableState (Compose) | [[#16. Interview Q&A\|§16 Interview Q&A]] |

### Common Mistakes (Quick Reference)

| Mistake | Fix | Jump To |
|---|---|---|
| Mutating `var` field directly on UiState | Use `val` + `.copy()` + `.update{}` | [[#15. Common Mistakes\|§15 Mistakes]] |
| Exposing `MutableStateFlow` publicly | Private `_x`, public `x = _x.asStateFlow()` | [[#15. Common Mistakes\|§15 Mistakes]] |
| `collectAsState()` instead of `collectAsStateWithLifecycle()` | Use `collectAsStateWithLifecycle()` on Android | [[#15. Common Mistakes\|§15 Mistakes]] |
| StateFlow for one-time events (navigation, toast) | Use `SharedFlow` with `replay=0` | [[#15. Common Mistakes\|§15 Mistakes]] |
| No initial value | `MutableStateFlow` always requires an initial value | [[#15. Common Mistakes\|§15 Mistakes]] |

---

## 🧠 Mental Model — Read This First

**StateFlow = a storage box that always has a value inside.**

```
var count = 0       ← Compose doesn't know it changed
count++             ← UI never updates

private val _count = MutableStateFlow(0)   ← Compose IS notified
_count.value++                             ← UI updates automatically
```

**StateFlow is a water tank with a gauge.**
- **Hot:** The tank always has water, regardless of whether anyone is looking.
- **Stateful:** It always holds exactly one value (the "current level").
- **Conflated:** If the level changes 1 → 2 → 3 very fast, a slow observer might only see 1 then 3.

---

## 📌 Table of Contents
1. [What Problem Does StateFlow Solve?](#1-what-problem)
2. [MutableStateFlow vs StateFlow](#2-mutablestateflow-vs-stateflow)
3. [ViewModel Responsibilities](#3-viewmodel-responsibilities)
4. [Collecting StateFlow in Compose](#4-collecting-in-compose)
5. [Immutable UiState Pattern](#5-immutable-uistate-pattern)
6. [copy() and update{}](#6-copy-and-update)
7. [StateFlow Equality Behavior](#7-equality-behavior)
8. [Sealed Class UiState (Loading/Success/Error)](#8-sealed-class-uistate)
9. [Converting Flow → StateFlow with stateIn](#9-stateIn)
10. [StateFlow Operators](#10-operators)
11. [Multiple StateFlows & Form Validation](#11-form-validation)
12. [StateFlow vs LiveData](#12-vs-livedata)
13. [StateFlow vs SharedFlow](#13-vs-sharedflow)
14. [StateFlow Internals](#14-internals)
15. [Common Mistakes](#15-common-mistakes)
16. [Interview Q&A](#16-interview)

---

## 1. What Problem Does StateFlow Solve?

### Why Normal Variables Don't Work

```kotlin
// ❌ Compose has NO idea this changed
class MyViewModel : ViewModel() {
    var count = 0
    fun increment() { count++ }  // UI never updates!
}
```

Compose doesn't observe regular Kotlin variables. When `count++` runs, no recomposition happens.

### StateFlow Fixes This

```kotlin
// ✅ Compose IS notified
class MyViewModel : ViewModel() {
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    fun increment() {
        _count.value++   // collectors are immediately notified
    }
}
```

**StateFlow = current state container** — think of anything that has a "current value":
- Current score = 10
- Current username = "Shreyas"
- Current theme = Dark
- Current loading state = true

---

## 2. MutableStateFlow vs StateFlow

```kotlin
// MutableStateFlow — writable (stays PRIVATE inside ViewModel)
private val _count = MutableStateFlow(0)

// StateFlow — read-only view exposed to UI
val count: StateFlow<Int> = _count.asStateFlow()
```

| Aspect | MutableStateFlow | StateFlow |
|---|---|---|
| Who can write | Only ViewModel | Nobody (read-only) |
| Who can read | Anyone | Anyone |
| Typical visibility | `private` | `public val` |
| How created | `MutableStateFlow(initial)` | `_x.asStateFlow()` |

> [!IMPORTANT]
> `asStateFlow()` is a **zero-cost cast** — it returns a read-only interface to the same object. It's not a copy. It's purely a compile-time contract: "the UI cannot modify this."

---

## 3. ViewModel Integration & Lifecycle

### 🔷 The History and Purpose of ViewModel

Prior to the introduction of `ViewModel` in Android Architecture Components, handling screen rotation was a major pain point. When a user rotated their device:
1. Android destroyed the current `Activity` instance.
2. A new `Activity` instance was created from scratch.
3. All variables, ongoing network calls, and in-memory data were lost.
4. You had to save and restore state manually using `onSaveInstanceState(Bundle)`.

However, the Android `Bundle` has a strict size limit (~1MB total transaction limit). It is meant for small UI state (toggles, text input, selections), not for large datasets like lists of products, cache, or heavy business logic objects.

`ViewModel` solves this:
* **Survives configuration changes**: It survives screen rotation, keyboard slide-out, and language switches. Same VM instance is returned to the new Activity.
* **Coroutine scope (`viewModelScope`)**: Automatically cancels ongoing asynchronous operations when the screen is closed permanently.
* **Separation of Concerns**: Decouples UI rendering from business logic.

```
User rotates phone →
  Activity: Destroyed & recreated (new instance)
  ViewModel: Survives, same instance is returned to the new Activity
```

### 🔷 ViewModel Lifecycle Diagram

```
                  ┌─────────────────────────────────────┐
                  │          Activity Lifecycle          │
                  ├─────────────────────────────────────┤
  onCreate()  ────┤─── ViewModel.init{}  ←─── CREATED   │
                  │                                     │
  onPause()   ────┤                                     │
                  │                                     │
  onStop()    ────┤                                     │
                  │                                     │
  onDestroy() ────┤─── (due to rotation: VM SURVIVES)   │ ← Activity destroyed
                  │                                     │
  onCreate()  ────┤─── (same VM instance returned)      │ ← New Activity created
  ...             │                                     │
                  │                                     │
  onDestroy() ────┤─── ViewModel.onCleared() called     │ ← User exits: VM destroyed
                  └─────────────────────────────────────┘
```

The `onCleared()` method is called when the user permanently exits the screen (e.g., presses Back, finishes the Activity, or clears the destination from the back stack). This is where resource cleanup happens, although `viewModelScope` cancels all active coroutines automatically.

### 🔷 Basic Encapsulation Example

```kotlin
class CounterViewModel : ViewModel() {

    // 1. HOLD state (private mutable state)
    private val _count = MutableStateFlow(0)

    // 2. PROTECT & EXPOSE state (read-only StateFlow)
    val count: StateFlow<Int> = _count.asStateFlow()

    // 3. UPDATE state via explicit functions (encapsulation)
    fun increment() {
        if (_count.value < 100) {  // business logic
            _count.value++
        }
    }
    fun decrement() { _count.value-- }
    fun reset()     { _count.value = 0 }
}
```

**Why use functions instead of modifying `.value` directly from the UI?**
```kotlin
// ❌ BAD — UI can bypass business rules and mutate state directly
viewModel._count.value = -999   // breaks encapsulation

// ✅ GOOD — UI calls ViewModel functions; VM decides how to apply updates
viewModel.increment()
```

### 🔷 Dependency Injection with Hilt

In production apps, you inject dependencies into ViewModels using Hilt (the recommended DI framework):

```kotlin
// 1. Dependency declaration
@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModel() { /* ... */ }

// 2. Composable consumption
@Composable
fun ProductListScreen(viewModel: ProductListViewModel = hiltViewModel()) {
    // hiltViewModel() automatically handles Hilt injection
}
```

Dependencies required in `build.gradle.kts`:
```kotlin
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")  // for hiltViewModel()
```

---

## 4. Collecting StateFlow in Compose

### The Complete Chain

```
Button Click
    ↓
ViewModel Function called (e.g., increment())
    ↓
MutableStateFlow value updated (_count.value++)
    ↓
StateFlow emits new value
    ↓
collectAsStateWithLifecycle() receives it
    ↓
Compose State<Int> updated
    ↓
Recomposition scheduled
    ↓
UI redraws with new value
```

### collectAsStateWithLifecycle() — The Modern Standard

```kotlin
@Composable
fun CounterScreen(viewModel: CounterViewModel = viewModel()) {

    // Converts StateFlow<Int> → State<Int> → Int (via 'by' delegation)
    val count by viewModel.count.collectAsStateWithLifecycle()

    Column {
        Text("Count: $count")
        Button(onClick = { viewModel.increment() }) { Text("Increment") }
        Button(onClick = { viewModel.decrement() }) { Text("Decrement") }
        Button(onClick = { viewModel.reset() })     { Text("Reset") }
    }
}
```

### Type Transformation

```
StateFlow<Int>          ← in ViewModel
    ↓
collectAsStateWithLifecycle()
    ↓
State<Int>              ← what the function returns
    ↓
by (Kotlin delegation) automatically calls .value
    ↓
Int                     ← what 'count' actually is in your composable
```

**Without `by`** — you must use `.value` manually:
```kotlin
val countState = viewModel.count.collectAsStateWithLifecycle()
Text("${countState.value}")     // must access .value
```

**With `by`** — Kotlin calls `.value` automatically:
```kotlin
val count by viewModel.count.collectAsStateWithLifecycle()
Text("$count")                  // count IS the Int directly
```

### collectAsState() vs collectAsStateWithLifecycle()

| | `collectAsState()` | `collectAsStateWithLifecycle()` |
|---|---|---|
| Lifecycle aware | ❌ No | ✅ Yes |
| Pauses in background | ❌ Keeps running | ✅ Pauses at STOPPED |
| Battery/resource impact | Higher | Lower |
| Platform | Compose (any) | Android only |
| **Use for** | KMP/non-Android | Android screens |

**Why two functions?** Compose is multiplatform (Desktop, iOS, Web). `collectAsState()` is generic — it doesn't depend on Android lifecycle. `collectAsStateWithLifecycle()` was added separately as an Android-specific extension so the base Compose API stays platform-neutral.

**Rule:** For every Android Compose screen → always use `collectAsStateWithLifecycle()`.

---

## 5. Immutable UiState Pattern

### Why a Data Class Instead of Multiple StateFlows?

```kotlin
// ❌ Multiple separate StateFlows — messy to coordinate
private val _isLoading  = MutableStateFlow(false)
private val _errorMsg   = MutableStateFlow<String?>(null)
private val _email      = MutableStateFlow("")

// ✅ Single UiState data class — single source of truth
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

private val _uiState = MutableStateFlow(LoginUiState())
val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
```

**Benefits of single UiState:**
- All related state changes atomically (no inconsistent intermediate states)
- Easy to read: one place describes the entire screen
- Easy to unit test: set the whole state at once

### Why UiState Must Be Immutable (val, not var)

```kotlin
// ❌ Mutable fields — mutation is INVISIBLE to StateFlow
data class LoginUiState(
    var isLoading: Boolean = false   // var!
)

// This does NOT trigger StateFlow emission:
_uiState.value.isLoading = true
// ↑ StateFlow still holds the SAME object reference → no emission → UI doesn't update
```

**How StateFlow detects changes:**
```
StateFlow checks: oldValue == newValue

If you mutate a field INSIDE the object:
    same object reference → equal → NO emission

If you create a new object with copy():
    new object reference → not equal → EMISSION ✅
```

```kotlin
// ✅ Immutable fields — changes are visible
data class LoginUiState(
    val isLoading: Boolean = false   // val!
)

// This DOES trigger emission:
_uiState.value = _uiState.value.copy(isLoading = true)
// ↑ New object created → different reference → StateFlow emits → UI updates ✅
```

---

## 6. copy() and update{}

### What copy() Does

`copy()` is auto-generated by Kotlin for every `data class`. It creates a new object, keeping all fields the same except the ones you specify:

```kotlin
val state = LoginUiState(email = "abc@gmail.com", isLoading = false)

val newState = state.copy(isLoading = true)
// Result: LoginUiState(email = "abc@gmail.com", isLoading = true)
// email was NOT specified → kept from original
// isLoading WAS specified → changed to true
```

**The pattern:**
```kotlin
// Old object (unchanged):   LoginUiState(email = "x", isLoading = false)
// After copy(isLoading=true): LoginUiState(email = "x", isLoading = true)

// StateFlow sees: new object assigned → emits → UI updates ✅
_uiState.value = _uiState.value.copy(isLoading = true)
```

### update{} — The Thread-Safe Way

```kotlin
// Style 1: .value + copy()
_uiState.value = _uiState.value.copy(isLoading = true)

// Style 2: .update{} — preferred for concurrent environments
_uiState.update { it.copy(isLoading = true) }
//               ↑ 'it' = current StateFlow value
```

**Why update{} is safer:**

```kotlin
// ❌ Race condition possible with .value:
// Thread A reads score = 5
// Thread B reads score = 5
// Thread A writes score = 6
// Thread B writes score = 6  ← Thread A's increment is LOST

// ✅ update{} uses atomic compare-and-set internally:
_uiState.update { it.copy(score = it.score + 1) }
// If another thread modified state while this runs, update retries automatically
```

**Rule:** For simple apps, `.value = ...copy()` is fine. As ViewModels grow complex with multiple coroutines, always use `.update { it.copy(...) }`.

---

## 7. StateFlow Equality Behavior

> [!IMPORTANT]
> StateFlow uses **equality checks** (`==`), NOT object identity (`===`). Creating a new object does NOT automatically mean emission.

### When StateFlow Emits

```kotlin
val _count = MutableStateFlow(0)

_count.value = 5    // 0 → 5: different → EMITS ✅
_count.value = 5    // 5 → 5: same → DOES NOT EMIT ✅ (prevents unnecessary recomposition)
_count.value = 6    // 5 → 6: different → EMITS ✅
```

### With Data Classes

```kotlin
data class User(val name: String, val age: Int)

_user.value = User("Alice", 30)
_user.value = User("Alice", 30)    // structurally equal → NO emission (== checks all fields)
_user.value = _user.value.copy(age = 31)  // different → EMITS ✅
```

**Why this is good:** If you call `_uiState.update { it.copy(isLoading = true) }` when `isLoading` is already `true`, StateFlow skips the emission. No wasted recomposition.

**A real-world gotcha:**
```kotlin
// You forgot to clear error before showing loading:
_uiState.update { it.copy(isLoading = true) }
// _uiState.value is now: UiState(isLoading=true, error="previous error")

// Login fails again:
_uiState.update { it.copy(error = "previous error") }
// New state == old state → NO EMISSION! User doesn't see any change.
// Always clear old values when transitioning to a new state.
```

---

## 8. Sealed Class UiState (Loading / Success / Error)

### For Complex Screen States

When a screen can only be in ONE state at a time, a sealed class is cleaner than boolean flags:

```kotlin
// ❌ Flags can create impossible combinations:
data class UiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
// Can happen: isLoading=true AND isSuccess=true simultaneously — broken state!

// ✅ Sealed class/interface — only ONE state at a time:
sealed interface UiState {
    object Idle    : UiState
    object Loading : UiState
    data class Success(val data: String)   : UiState
    data class Error(val message: String)  : UiState
}
```

### ViewModel with Sealed UiState

```kotlin
class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            try {
                val result = repository.login(email, password)
                _uiState.value = UiState.Success(result.welcomeMessage)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

### Composable with Sealed UiState

```kotlin
@Composable
fun LoginScreen(viewModel: LoginViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is UiState.Idle    -> LoginForm(onLogin = viewModel::login)
        is UiState.Loading -> CircularProgressIndicator()
        is UiState.Success -> Text("Welcome! ${state.data}")
        is UiState.Error   -> ErrorMessage(state.message, onRetry = viewModel::retry)
    }
}
```

### State Transitions

```
Idle → Loading → Success
Idle → Loading → Error → Loading → Success
```

Notice: Success and Error can **never** happen at the same time. The type system enforces this.

---

## 9. Converting Flow → StateFlow with stateIn

### The Common Architecture Pattern

Repositories return `Flow` (cold). ViewModels should expose `StateFlow` (hot).

```kotlin
// Repository — returns cold Flow
class UserRepository {
    fun getUsers(): Flow<List<User>> = dao.getAllUsers()  // Room Flow
}

// ViewModel — converts to hot StateFlow
class HomeViewModel(private val repo: UserRepository) : ViewModel() {

    val users: StateFlow<List<User>> = repo.getUsers()
        .catch { emit(emptyList()) }       // handle errors gracefully
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
```

### SharingStarted Options

| Option | Starts | Stops |
|---|---|---|
| `Eagerly` | Immediately | Never |
| `Lazily` | On first collector | Never |
| `WhileSubscribed(5000)` | On first collector | 5 sec after last collector leaves |

**Why `WhileSubscribed(5000)` is the standard:**
- Starts when UI attaches
- Stops **5 seconds** after last UI collector leaves (not immediately)
- The 5-second buffer handles screen rotation (rotation takes ~1-2 seconds during which there are 0 collectors temporarily)
- After 5 seconds with no collector: upstream Flow (DB query, API) is cancelled → saves resources

---

## 10. StateFlow Operators

```kotlin
class ProductViewModel(private val repo: ProductRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // map — transform each value
    val isSearchActive: StateFlow<Boolean> = _searchQuery
        .map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // combine — combine two StateFlows into one
    private val _sortOrder = MutableStateFlow(SortOrder.ASCENDING)
    val products: StateFlow<List<Product>> = combine(
        _searchQuery,
        _sortOrder
    ) { query, sort ->
        repo.getProducts(query, sort)
    }
    .flatMapLatest { it }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // distinctUntilChanged — skip emissions where value didn't change
    // (StateFlow already does this automatically — no need to add manually)
}
```

---

## 11. Multiple StateFlows & Form Validation

```kotlin
class FormViewModel : ViewModel() {

    private val _email    = MutableStateFlow("")
    private val _password = MutableStateFlow("")

    val email: StateFlow<String>    = _email.asStateFlow()
    val password: StateFlow<String> = _password.asStateFlow()

    // combine multiple flows into a derived StateFlow
    val isFormValid: StateFlow<Boolean> = combine(
        _email, _password
    ) { email, password ->
        email.contains("@") && password.length >= 6
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onEmailChange(value: String)    { _email.value = value }
    fun onPasswordChange(value: String) { _password.value = value }
}
```

---

## 4b. Collecting in XML (Fragment/Activity)

For non-Compose screens (XML layouts), the collection pattern is different:

```kotlin
class UserFragment : Fragment(R.layout.fragment_user) {
    private val vm: UserViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ CORRECT — repeatOnLifecycle pauses at STOPPED, resumes at STARTED
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.uiState.collect { state ->
                    when (state) {
                        is UiState.Loading -> showLoading()
                        is UiState.Success -> showData(state.data)
                        is UiState.Error   -> showError(state.message)
                    }
                }
            }
        }

        // ❌ WRONG — collects even in background, wastes resources, can crash
        // lifecycleScope.launch {
        //     vm.uiState.collect { ... }   ← never pauses!
        // }
    }
}
```

**Collecting multiple flows simultaneously in XML:**
```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { vm.uiState.collect { renderState(it) } }   // state
        launch { vm.events.collect { handleEvent(it) } }    // events
    }
}
```

---

## 12. StateFlow vs LiveData

| Feature | LiveData | StateFlow |
|---|---|---|
| Language | Java-first | Kotlin-first |
| Initial value | Optional (nullable) | **Required** |
| Lifecycle aware | ✅ Built-in | ❌ Needs `collectAsStateWithLifecycle()` |
| Flow operators | Very limited | Full Flow operators |
| Duplicate filtering | ❌ | ✅ |
| Unit testing | Needs `InstantTaskExecutorRule` | Pure coroutine (`Turbine`) |
| Platform | Android only | Pure Kotlin (KMP-ready) |
| **Modern recommendation** | Legacy | ✅ Preferred |

**Migration pattern:**
```kotlin
// Old LiveData:
val users: LiveData<List<User>> = userRepository.getUsers().asLiveData()

// New StateFlow:
val users: StateFlow<List<User>> = userRepository.getUsers()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

---

## 13. StateFlow vs SharedFlow

| | StateFlow | SharedFlow |
|---|---|---|
| Holds state | ✅ Always has current value | ❌ No current value by default |
| Initial value | Required | Not required |
| Replay | Always replays last value | Configurable (default: 0) |
| Duplicate filtering | ✅ Skips duplicates | ❌ Emits every call |
| **Use for** | UI State (loading, user data, list) | One-time Events (navigate, toast) |

```
Does the data represent STATE?   → StateFlow
  Examples: current user, loading flag, list items

Does the data represent an EVENT? → SharedFlow
  Examples: navigate to screen, show snackbar, show dialog
```

> [!CAUTION]
> **Using StateFlow for events is a common bug.** If you put `isNavigating = true` in a StateFlow and the user rotates the screen, the new composition reads the StateFlow immediately, sees `isNavigating = true`, and navigates again — even though navigation already happened!

---

## 14. StateFlow Internals

### Hot Flow

StateFlow is **hot** — it runs regardless of collectors:

```kotlin
private val _count = MutableStateFlow(0)

// Even with NO collectors attached:
_count.value = 100   // stored, current value is 100

// Later, when a collector attaches:
_count.collectAsStateWithLifecycle()
// → Immediately receives: 100  (the latest value)
```

### Equality Check Detail

StateFlow internally uses:
```kotlin
// Default: structural equality (data class == checks all fields)
structuralEqualityPolicy()

// Can override if needed:
val _state = MutableStateFlow(
    value = MyState(),
    // no policy = structuralEqualityPolicy by default
)
```

### Replay = 1

StateFlow always has a replay of 1 (the current value). A new collector always immediately receives the latest value — never has to "wait" for the next emission.

---

## 15. Common Mistakes

### Mistake 1: Mutating UiState fields directly

```kotlin
// ❌ WRONG — mutation invisible to StateFlow
_uiState.value.isLoading = true    // compile error if val, silent bug if var

// ✅ CORRECT — create new object
_uiState.update { it.copy(isLoading = true) }
```

### Mistake 2: Exposing MutableStateFlow

```kotlin
// ❌ WRONG — UI can modify state directly
val uiState = MutableStateFlow(UiState())    // public, mutable!

// ✅ CORRECT — private mutable, public read-only
private val _uiState = MutableStateFlow(UiState())
val uiState: StateFlow<UiState> = _uiState.asStateFlow()
```

### Mistake 3: Using collectAsState() instead of collectAsStateWithLifecycle()

```kotlin
// ❌ WRONG in Android — keeps collecting in background
val state by viewModel.uiState.collectAsState()

// ✅ CORRECT — lifecycle-aware
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

### Mistake 4: Using StateFlow for one-time events

```kotlin
// ❌ WRONG — StateFlow replays last value on rotation
private val _showToast = MutableStateFlow(false)
// After rotation → screen collects StateFlow → sees true → shows toast again!

// ✅ CORRECT — SharedFlow doesn't replay
private val _toastEvent = MutableSharedFlow<String>()
```

### Mistake 5: Modifying state from wrong thread

```kotlin
// ❌ WRONG — StateFlow.value= on non-main thread can cause issues in Compose
Thread {
    _uiState.value = UiState(isLoading = true)  // not on main thread!
}

// ✅ CORRECT — use viewModelScope (defaults to Main for state updates)
viewModelScope.launch {
    _uiState.update { it.copy(isLoading = true) }
}

// ✅ ALSO CORRECT — .update{} is thread-safe (atomic)
_uiState.update { it.copy(isLoading = true) }
```

### Mistake 6: Not providing an initial value

```kotlin
// ❌ WRONG — StateFlow REQUIRES an initial value
private val _count = MutableStateFlow()       // compile error

// ✅ CORRECT
private val _count = MutableStateFlow(0)
```

---

## 16. Interview Q&A

**Q: What is StateFlow and why was it introduced?**
> StateFlow is a hot, state-holding coroutine flow that always has a current value and emits updates to collectors. It was introduced as a Kotlin-native replacement for LiveData — with full coroutine operator support, no Android dependency, and structural equality filtering that prevents duplicate emissions.

**Q: What is the backing property pattern?**
> It involves a private `MutableStateFlow` (`_uiState`) and a public read-only `StateFlow` (`uiState = _uiState.asStateFlow()`). This enforces encapsulation: only the ViewModel can modify state; the UI can only observe it. If the UI had access to `MutableStateFlow`, it could mutate state directly, breaking Unidirectional Data Flow.

**Q: Why use `update{}` instead of `.value =`?**
> `.value =` involves read-then-write, which is a race condition in concurrent environments. `.update{}` uses an atomic compare-and-set loop internally — if another thread modifies state between the read and write, `update` automatically retries with the fresh value.

**Q: What does `SharingStarted.WhileSubscribed(5000)` mean in stateIn?**
> Start collecting from upstream when the first UI subscriber appears. Stop collecting 5 seconds after the last subscriber leaves. The 5-second delay prevents cancelling and restarting an upstream database query during screen rotation — rotation takes ~1-2 seconds, well within the 5-second window.

**Q: Why doesn't StateFlow emit when I assign the same value?**
> StateFlow uses structural equality (`==`) to compare old and new values. If `newValue == oldValue`, the emission is suppressed. This is intentional — it prevents unnecessary recompositions. For data classes, equality compares all fields. If you want to force emission, you must change at least one field.

**Q: Why should StateFlow be in ViewModel, not MutableState?**
> `MutableState` couples the ViewModel to the Compose UI runtime (Android-specific). `StateFlow` is pure Kotlin, making ViewModels: (1) testable without a Compose environment, (2) usable with Kotlin Multiplatform for iOS/Desktop, (3) composable with powerful Flow operators like `debounce`, `flatMapLatest`, `combine`.

**Q: What is the difference between a cold Flow and a StateFlow?**
> A cold `Flow` runs its builder code fresh for every collector — 3 collectors = 3 API calls or 3 database queries. StateFlow is hot — it runs once, shares the result, and all collectors receive the same emissions. New collectors immediately get the latest stored value.

---

## 📦 Gradle Dependencies

```kotlin
// StateFlow is part of Kotlin Coroutines — no extra dependency if using Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

// For collectAsStateWithLifecycle() in Compose
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

// For ViewModel and viewModelScope
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")

// For Turbine (StateFlow testing)
testImplementation("app.cash.turbine:turbine:1.2.0")
```

---

## 🔗 Connections

- **Compose side**: [[State]] — how `collectAsStateWithLifecycle()` bridges StateFlow into Compose
- **Events**: [[SharedFlow]] — when to use SharedFlow for navigation, toasts, snackbars
- **Cold Flow / operators / Repository pattern**: [[Cold Flow]] — `flow {}`, `callbackFlow`, `stateIn`, all operators
- **LiveData (legacy)**: [[LiveData]] — when you see it in existing code + migration guide
- **Coroutines**: [[Coroutines/Coroutines in Kotlin Complete Notes]] — viewModelScope, dispatchers
