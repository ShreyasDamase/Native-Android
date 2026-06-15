# ⚖️ Comparison — State vs Flow vs StateFlow vs LiveData vs SharedFlow

> [!IMPORTANT]
> This is the **master decision guide**. Read this first when you're unsure which reactive primitive to use. Each primitive has one job — picking the wrong one causes bugs, leaks, and duplicate UI events.

---

## 🧠 The One-Sentence Summary for Each

| Primitive | One Sentence |
|---|---|
| **Compose `State`** | A value Compose reads and re-draws when it changes — lives inside a composable |
| **Cold `Flow`** | A recipe for producing values — only runs when someone collects it |
| **`StateFlow`** | A hot broadcast that always has a current value — the modern LiveData |
| **`LiveData`** | A lifecycle-aware holder — the old LiveData, still in legacy code |
| **`SharedFlow`** | A hot broadcast with no stored value — fire-and-forget events |

---

## 📌 Table of Contents
1. [The Big Decision Tree](#1-decision-tree)
2. [Layer Ownership — Where Each Lives](#2-layer-ownership)
3. [Hot vs Cold — The Fundamental Split](#3-hot-vs-cold)
4. [Master Comparison Table](#4-master-comparison-table)
5. [Memory Model — What Each Stores](#5-memory-model)
6. [Lifecycle Behaviour](#6-lifecycle-behaviour)
7. [How to Write a Value](#7-how-to-write)
8. [How to Read / Observe a Value](#8-how-to-read)
9. [Duplicate Value Behaviour](#9-duplicate-filtering)
10. [Error Handling](#10-error-handling)
11. [Thread Safety](#11-thread-safety)
12. [Testability](#12-testability)
13. [When Each One Breaks — Anti-Patterns](#13-anti-patterns)
14. [Real-World Mapping](#14-real-world-mapping)
15. [Interview Cheat Sheet](#15-interview-cheat-sheet)

---

## 1. The Big Decision Tree

```
You need to store/observe some data. Ask:

STEP 1 — WHERE does this data live?
├── Inside a @Composable function?
│       └── ✅ Compose State  (mutableStateOf + remember)
│
└── Inside a ViewModel / Repository?
        │
        STEP 2 — WHAT kind of data is it?
        │
        ├── Current state of the screen?
        │   (loading flag, user data, list content, form text)
        │       ├── New project / Compose UI → ✅ StateFlow
        │       └── Legacy / XML DataBinding  → LiveData
        │
        ├── Something that happened ONCE?
        │   (navigate, show toast, show snackbar, dismiss dialog)
        │       └── ✅ SharedFlow  (replay = 0)
        │
        └── A stream of data from DB / network / sensor?
            (Room query, API polling, callbackFlow)
                ├── Inside Repository (produce it) → ✅ Cold Flow
                └── In ViewModel (expose it to UI) → .stateIn() → StateFlow
```

**The 80% rule:**
- Screen state → **StateFlow**
- One-time action → **SharedFlow**
- Data pipeline → **Cold Flow** (convert to StateFlow at ViewModel boundary)
- Local UI toggle → **Compose State**
- Old codebase → **LiveData** (read and migrate gradually)

---

## 2. Layer Ownership — Where Each Lives

```
┌─────────────────────────────────────────────┐
│              UI LAYER                        │
│                                              │
│  @Composable fun Screen() {                  │
│      var count by remember {                 │◄── Compose State
│          mutableStateOf(0)                   │    (UI-only, ephemeral)
│      }                                       │
│      val uiState by                          │
│          vm.uiState                          │◄── read StateFlow here
│              .collectAsStateWithLifecycle()  │
│  }                                           │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│           PRESENTATION LAYER                 │
│                                              │
│  class ViewModel {                           │
│      val uiState: StateFlow<UiState>        │◄── StateFlow (UI state)
│      val events: SharedFlow<UiEvent>        │◄── SharedFlow (one-time)
│  }                                           │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│             DOMAIN LAYER                     │
│                                              │
│  class Repository {                          │
│      fun getData(): Flow<Data>              │◄── Cold Flow (stream)
│  }                                           │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│              DATA LAYER                      │
│                                              │
│  Room Dao: Flow<List<User>>                 │◄── Cold Flow from DB
│  Retrofit: suspend fun getUser()            │    (Room auto-emits on change)
└─────────────────────────────────────────────┘
```

**Rule:** Never put Compose `State` in a ViewModel. Never put `LiveData` in a Repository. Never expose `MutableStateFlow` publicly.

---

## 3. Hot vs Cold — The Fundamental Split

This is the most important conceptual divide:

```
COLD (Flow)                          HOT (StateFlow, SharedFlow, LiveData)
────────────────────────────────     ──────────────────────────────────────
Does nothing without a collector     Runs regardless of collectors
Each collector: independent run      All collectors: share same emissions
Like a recipe                        Like a radio broadcast
Like a factory that builds per order Like a factory that always produces

Example: Room Flow query             Example: StateFlow of login state
→ query runs each time you collect   → state is always current, new collector
                                       immediately gets the latest value
```

### Visual

```
Cold Flow:
Collector A subscribes → [1, 2, 3, 4, 5] → A receives all
Collector B subscribes → [1, 2, 3, 4, 5] → B receives all (fresh run!)

Hot StateFlow:
StateFlow emitting: ..3...4...5...
Collector A subscribes at start:       [3, 4, 5]
Collector B subscribes at "4":              [4, 5]  ← misses 3, but gets current
New Collector C subscribes at "5":              [5] ← immediately gets latest
```

---

## 4. Master Comparison Table

| Feature | Compose State | Cold Flow | StateFlow | LiveData | SharedFlow |
|---|---|---|---|---|---|
| **Ecosystem** | Jetpack Compose | Kotlin Coroutines | Kotlin Coroutines | AndroidX Lifecycle | Kotlin Coroutines |
| **Hot / Cold** | Hot (snapshot) | ❄️ Cold | 🔥 Hot | 🔥 Hot | 🔥 Hot |
| **Holds current value** | ✅ Yes | ❌ No | ✅ Yes | ✅ Yes | ⚙️ Optional (replay) |
| **Initial value required** | ✅ Yes | ❌ N/A | ✅ Yes | ❌ Optional | ❌ No |
| **Replay to new observer** | ✅ Current | Full stream | ✅ Last value | ✅ Last value | ⚙️ Configurable (default 0) |
| **Lifecycle aware** | ✅ Compose | ❌ Manual | ❌ Manual | ✅ Auto | ❌ Manual |
| **Duplicate filtering** | ✅ Yes (==) | ❌ No | ✅ Yes (==) | ❌ No | ❌ No |
| **Error handling** | ❌ No | ✅ `catch{}` | Limited | ❌ No | Limited |
| **Flow operators** | ❌ No | ✅ Full | ✅ Full | `map`, `switchMap` | ✅ Full |
| **Thread safety** | Main thread | Any (flowOn) | ✅ Any thread | Main / postValue | ✅ Any thread |
| **Unit testing** | Compose rules | Pure Kotlin | Pure Kotlin (Turbine) | InstantTaskExecutorRule | Pure Kotlin (Turbine) |
| **Android only** | ✅ Yes | ❌ No | ❌ No | ✅ Yes | ❌ No |
| **KMP support** | ✅ CMP | ✅ Yes | ✅ Yes | ❌ No | ✅ Yes |
| **Best for** | UI toggles in composables | Data pipelines, DB, sensors | UI state in ViewModel | Legacy UI state | One-time events |
| **Typical location** | Inside `@Composable` | Repository | ViewModel | ViewModel (legacy) | ViewModel |

---

## 5. Memory Model — What Each Stores

```
Compose State:        [  current value  ]
                      mutableStateOf("Alice")
                      → always has a value
                      → stored in Compose Slot Table (in-memory, per composition)

Cold Flow:            [  no memory  ]
                      flow { emit(1); emit(2) }
                      → stores nothing
                      → every collector re-runs the full producer block

StateFlow:            [  current value  ]
                      MutableStateFlow("Alice")
                      → always has exactly ONE value
                      → new collectors immediately receive the current value

LiveData:             [  current value  ]
                      MutableLiveData<String>()
                      → may start with null (no mandatory initial value)
                      → new observers receive the last set value

SharedFlow:           [  configurable buffer  ]
                      MutableSharedFlow<Event>(replay = 0)
                      → replay=0: stores NOTHING (new collectors get nothing past)
                      → replay=N: stores last N emissions
                      → default: pure event bus, no memory
```

---

## 6. Lifecycle Behaviour

### When the app goes to background (Home button pressed):

```
Compose State:        Kept in Slot Table → survives (but lost on rotation)
                      Use rememberSaveable to survive rotation

Cold Flow + collect:  ❌ Keeps running! You MUST use repeatOnLifecycle
                      → collects in background, wastes battery, can crash

StateFlow:            ❌ Keeps running by default
                      → use collectAsStateWithLifecycle() in Compose
                      → use repeatOnLifecycle(STARTED) in XML

LiveData:             ✅ Auto-pauses — built into its design
                      → stops delivering to STOPPED observers automatically
                      → resumes and delivers latest on STARTED

SharedFlow:           ❌ Keeps running by default
                      → use LaunchedEffect + repeatOnLifecycle in Compose
                      → use repeatOnLifecycle in XML
```

### On screen rotation:

```
Compose State         ❌ LOST — remember is in-memory only
(remember):           Use rememberSaveable for rotation-survival

Compose State         ✅ SURVIVES — saved to Android Bundle
(rememberSaveable):

Cold Flow:            Not applicable (stateless)

StateFlow:            ✅ SURVIVES — lives in ViewModel, which survives rotation
                      New UI observer immediately gets current value

LiveData:             ✅ SURVIVES — lives in ViewModel
                      New observer immediately gets last value

SharedFlow:           ✅ ViewModel survives, BUT events are NOT replayed (replay=0)
                      This is the CORRECT behaviour for events!
```

---

## 7. How to Write a Value

```kotlin
// Compose State
var count by remember { mutableStateOf(0) }
count = 5                           // direct assignment via 'by' delegation
count++                             // also works

// Cold Flow — you emit inside the builder
val myFlow = flow {
    emit(1)                         // only while flow is being collected
    emit(2)
}

// StateFlow
private val _state = MutableStateFlow(0)
_state.value = 5                    // simple assignment (thread-safe read, Main write)
_state.update { currentValue -> currentValue + 1 }   // atomic update (thread-safe)
_state.update { it.copy(name = "Alice") }            // for data classes

// LiveData
private val _liveData = MutableLiveData(0)
_liveData.value = 5                 // Main thread only
_liveData.postValue(5)              // any thread (async, may drop intermediates)

// SharedFlow
private val _events = MutableSharedFlow<UiEvent>()
viewModelScope.launch {
    _events.emit(UiEvent.ShowToast("Hello"))   // suspend, inside coroutine
}
_events.tryEmit(UiEvent.ShowToast("Hello"))    // non-suspend, may return false
```

### Thread Safety Summary

| | Main only | Any thread | Background safe |
|---|---|---|---|
| Compose State | ✅ (write from Main) | ❌ | ❌ |
| Cold Flow | emit on current dispatcher | ✅ with flowOn | ✅ |
| StateFlow `.value` | Main recommended | ✅ reads anywhere | `.update{}` is safe anywhere |
| LiveData `.value` | ✅ Main only | ❌ | `.postValue()` |
| SharedFlow `.emit()` | Any coroutine | ✅ | ✅ |

---

## 8. How to Read / Observe a Value

```kotlin
// ─── Inside a @Composable ───────────────────────────────────────────────────

// Compose State — automatic (Snapshot System tracks reads)
val name by remember { mutableStateOf("") }
Text(name)                            // Compose auto-recomposes when name changes

// StateFlow → Compose State bridge
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

// Cold Flow → Compose State bridge
val data by coldFlow.collectAsStateWithLifecycle(initialValue = null)

// SharedFlow events — NEVER collectAsStateWithLifecycle
LaunchedEffect(viewModel.events, lifecycleOwner) {
    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { event -> handle(event) }
    }
}

// ─── Inside a Fragment / Activity (XML) ─────────────────────────────────────

// LiveData — simplest
viewModel.user.observe(viewLifecycleOwner) { user ->
    binding.name.text = user.name
}

// StateFlow — needs repeatOnLifecycle
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state -> render(state) }
    }
}

// SharedFlow — same pattern as StateFlow in XML
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { event -> handle(event) }
    }
}

// ─── Inside a ViewModel ──────────────────────────────────────────────────────

// Read StateFlow current value synchronously (no collection needed)
val current = _uiState.value

// Read and react to another StateFlow
viewModelScope.launch {
    anotherStateFlow.collect { value -> /* react */ }
}

// Convert Cold Flow to StateFlow (standard pattern)
val data: StateFlow<Data> = repository.getData()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Data.Empty)
```

---

## 9. Duplicate Value Behaviour

```kotlin
// Compose State — skips recomposition if value is structurally equal
var name by remember { mutableStateOf("Alice") }
name = "Alice"      // ← no recomposition (== check)
name = "Bob"        // ← recomposition

// Cold Flow — emits every value, no duplicate filtering
flow { emit(1); emit(1); emit(1) }
    .collect { println(it) }   // prints: 1, 1, 1

// StateFlow — skips emission if value is structurally equal
_state.value = "Alice"
_state.value = "Alice"  // ← NOT emitted to collectors (== check)
_state.value = "Bob"    // ← emitted

// LiveData — NO duplicate filtering
_liveData.value = "Alice"
_liveData.value = "Alice"  // ← observer IS called again!
_liveData.value = "Bob"    // ← observer called

// SharedFlow — NO duplicate filtering (emits every call)
_events.emit(UiEvent.ShowToast("Hello"))
_events.emit(UiEvent.ShowToast("Hello"))  // ← both emitted to collectors
```

> [!WARNING]
> **StateFlow data class gotcha:** If you mutate a field IN-PLACE instead of using `copy()`, StateFlow won't re-emit — because the reference is the same and `==` sees no change.
> ```kotlin
> // ❌ WRONG — mutating in place, StateFlow won't see change
> _state.value.items.add(item)   // same reference!
>
> // ✅ CORRECT — new object via copy()
> _state.update { it.copy(items = it.items + item) }
> ```

---

## 10. Error Handling

```kotlin
// Compose State — no error concept; put error in state model
data class UiState(val error: String? = null)

// Cold Flow — full error handling with catch{}
repository.getData()
    .catch { exception ->
        emit(fallbackData)           // recover with fallback
        // or: throw exception       // propagate
    }
    .collect { data -> updateUI(data) }

// StateFlow — no built-in catch; wrap with try/catch in launch
viewModelScope.launch {
    try {
        val data = repository.getData()
        _uiState.update { it.copy(data = data) }
    } catch (e: Exception) {
        _uiState.update { it.copy(error = e.message) }
    }
}
// OR: use catch{} before stateIn
val data = repository.getDataFlow()
    .catch { emit(emptyList()) }     // ← on the cold flow before stateIn
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

// LiveData — no built-in error handling; use Result<T>
private val _result = MutableLiveData<Result<User>>()
_result.value = Result.success(user)
_result.value = Result.failure(exception)

// SharedFlow — wrap the emit in try/catch
viewModelScope.launch {
    try {
        val result = repository.save()
        _events.emit(UiEvent.ShowSuccess("Saved!"))
    } catch (e: Exception) {
        _events.emit(UiEvent.ShowError(e.message ?: "Unknown error"))
    }
}
```

---

## 11. Thread Safety

```kotlin
// Compose State — ONLY Main thread
// Writing from IO thread: race condition or crash
✅  _count = 5                              // Main thread
❌  withContext(Dispatchers.IO) { _count = 5 }  // dangerous!

// Cold Flow — safe with flowOn
flow {
    emit(api.fetchData())        // runs on IO (set by flowOn)
}.flowOn(Dispatchers.IO)
 .collect { updateUI(it) }      // runs on Main (caller's context)

// StateFlow — fully thread-safe
✅  _state.value = newValue      // safe from any thread
✅  _state.update { it.copy() } // atomic — safe from any thread concurrently

// LiveData
✅  _liveData.value = x          // Main thread only
✅  _liveData.postValue(x)       // any thread, but drops intermediate values

// SharedFlow
✅  _events.emit(event)           // any coroutine, any dispatcher
✅  _events.tryEmit(event)        // any thread (non-suspend)
```

---

## 12. Testability

```kotlin
// ─── Compose State ────────────────────────────────────────────────────────
// Needs Compose test rules (slow)
@get:Rule val composeRule = createComposeRule()

// ─── Cold Flow ────────────────────────────────────────────────────────────
// Pure Kotlin — fast, no Android dependency
@Test
fun `flow emits correct values`() = runTest {
    val flow = flowOf(1, 2, 3)
    val result = flow.toList()
    assertEquals(listOf(1, 2, 3), result)
}

// With Turbine for infinite flows
@Test
fun `repository stream emits items`() = runTest {
    fakeRepository.getItems().test {
        assertEquals(initialItem, awaitItem())
        cancelAndIgnoreRemainingEvents()
    }
}

// ─── StateFlow ────────────────────────────────────────────────────────────
@Test
fun `login updates ui state`() = runTest {
    val vm = LoginViewModel(fakeRepo)

    vm.uiState.test {
        assertEquals(LoginUiState(), awaitItem())          // initial
        vm.onLoginClicked("user@test.com", "pass123")
        assertEquals(awaitItem().isLoading, true)          // loading
        assertEquals(awaitItem().isLoggedIn, true)         // success
        cancelAndIgnoreRemainingEvents()
    }
}

// ─── LiveData ─────────────────────────────────────────────────────────────
// Needs Android dependency and InstantTaskExecutorRule (slow)
@get:Rule val rule = InstantTaskExecutorRule()

@Test
fun `livedata updates`() {
    val vm = UserViewModel(fakeRepo)
    val observed = mutableListOf<User>()
    vm.user.observeForever { observed.add(it) }
    vm.loadUser("1")
    assertEquals(expectedUser, observed.last())
}

// ─── SharedFlow ───────────────────────────────────────────────────────────
@Test
fun `delete emits undo event`() = runTest {
    val vm = ItemViewModel(fakeRepo)

    vm.events.test {
        vm.deleteItem(item)
        val event = awaitItem()
        assertTrue(event is UiEvent.ShowUndoSnackbar)
        cancelAndIgnoreRemainingEvents()
    }
}
```

**Speed ranking:** Cold Flow = StateFlow = SharedFlow tests (**fast**) > LiveData tests (**medium, needs rule**) > Compose State tests (**slow, needs Compose runtime**)

---

## 13. When Each One Breaks — Anti-Patterns

### Compose State in a ViewModel

```kotlin
// ❌ WRONG
class UserViewModel : ViewModel() {
    var name by mutableStateOf("")   // Compose UI dependency in ViewModel!
}
// Problems: can't unit test without Compose, not thread-safe, no KMP
```

### Cold Flow exposed directly to UI without stateIn

```kotlin
// ❌ WRONG
class UserViewModel : ViewModel() {
    val users: Flow<List<User>> = repository.getUsers()  // cold Flow exposed!
}

// Composable 1 collects → triggers 1 DB query
// Composable 2 collects → triggers ANOTHER DB query!
// Rotate screen → collection restarts → ANOTHER DB query!

// ✅ CORRECT — single upstream, hot StateFlow
val users = repository.getUsers().stateIn(
    viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
)
```

### StateFlow for one-time events

```kotlin
// ❌ WRONG — StateFlow for navigation
private val _shouldNavigate = MutableStateFlow(false)
// Rotate screen → new collector sees true → navigates AGAIN 💥

// ✅ CORRECT — SharedFlow for events
private val _events = MutableSharedFlow<UiEvent>()
// Rotate screen → replay=0 → new collector sees nothing → no duplicate navigation
```

### LiveData for events

```kotlin
// ❌ WRONG — LiveData always replays last value
private val _toast = MutableLiveData<String>()
_toast.value = "Item saved"    // shows toast
// Rotate screen → new observer → LiveData replays "Item saved" → toast shows AGAIN 💥

// ✅ CORRECT — SharedFlow for events
private val _events = MutableSharedFlow<UiEvent>()
```

### SharedFlow for persistent state

```kotlin
// ❌ WRONG — SharedFlow for UI state
private val _username = MutableSharedFlow<String>(replay = 0)
// New collector (fresh composition, rotation) gets NOTHING
// Screen shows blank username even though it was set

// ✅ CORRECT — StateFlow for persistent state
private val _username = MutableStateFlow("")
// New collector immediately gets the current username
```

### collectAsStateWithLifecycle() for SharedFlow events

```kotlin
// ❌ WRONG — events converted to State can cause duplicate handling
val latestEvent by viewModel.events.collectAsStateWithLifecycle(initialValue = null)
// Compose may re-read this state during recomposition → processes same event twice!

// ✅ CORRECT
LaunchedEffect(viewModel.events, lifecycleOwner) {
    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { event -> handle(event) }
    }
}
```

---

## 14. Real-World Mapping

Here's how a complete Login feature maps everything to the correct primitive:

```kotlin
// ─── DATA LAYER ───────────────────────────────────────────────────────────
class AuthRepository {
    // Cold Flow — produced on demand, uses callbackFlow for Firebase callback
    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.getInstance()
            .addAuthStateListener { auth -> trySend(auth.currentUser) }
        awaitClose { FirebaseAuth.getInstance().removeAuthStateListener(listener) }
    }
}

// ─── PRESENTATION LAYER ───────────────────────────────────────────────────
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null   // persistent error text in form → STATE
)

sealed class LoginUiEvent {
    object NavigateToHome : LoginUiEvent()       // one-time → SharedFlow
    data class ShowSnackbar(val msg: String) : LoginUiEvent()
}

class LoginViewModel(private val repo: AuthRepository) : ViewModel() {

    // StateFlow — persistent form state, survives rotation
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // SharedFlow — one-time events, NOT replayed on rotation
    private val _events = MutableSharedFlow<LoginUiEvent>()
    val events: SharedFlow<LoginUiEvent> = _events.asSharedFlow()

    // StateFlow from cold Flow — auth state, auto-updates when Firebase changes
    val isLoggedIn: StateFlow<Boolean> = repo.observeAuthState()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun onLoginClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repo.login(_uiState.value.email, _uiState.value.password)
                _events.emit(LoginUiEvent.NavigateToHome)       // one-time
            } catch (e: InvalidCredentialsException) {
                // Persistent error — shown in the form → update STATE
                _uiState.update { it.copy(emailError = "Invalid credentials") }
            } catch (e: Exception) {
                // Temporary snackbar → one-time EVENT
                _events.emit(LoginUiEvent.ShowSnackbar("Login failed: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

// ─── UI LAYER (Compose) ───────────────────────────────────────────────────
@Composable
fun LoginScreen(vm: LoginViewModel = viewModel(), onNavigateHome: () -> Unit) {

    // Compose State — local UI only (no business value, only aesthetics)
    var passwordVisible by remember { mutableStateOf(false) }

    // StateFlow → Compose State — the bridge
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    // SharedFlow events — correct pattern
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(vm.events, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.events.collect { event ->
                when (event) {
                    LoginUiEvent.NavigateToHome      -> onNavigateHome()
                    is LoginUiEvent.ShowSnackbar     -> snackbarHostState.showSnackbar(event.msg)
                }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
        Column {
            // reads uiState (StateFlow result) and passwordVisible (Compose State)
            OutlinedTextField(
                value = uiState.email,
                onValueChange = vm::onEmailChange,
                isError = uiState.emailError != null,
                supportingText = { uiState.emailError?.let { Text(it) } }
            )
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { /* vm.onPasswordChange(it) */ },
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        // passwordVisible is Compose State — no business value
                    }
                }
            )
            Button(
                onClick = vm::onLoginClicked,
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) CircularProgressIndicator()
                else Text("Login")
            }
        }
    }
}
```

**Mapping summary from the above:**

| Data | Primitive | Reason |
|---|---|---|
| `passwordVisible` | `Compose State` | Pure UI decoration, no business value |
| `email`, `password`, `isLoading`, `emailError` | `StateFlow<UiState>` | Persistent form state — must survive rotation |
| `NavigateToHome`, `ShowSnackbar` | `SharedFlow<UiEvent>` | One-time — must NOT re-fire on rotation |
| `observeAuthState()` | `Cold Flow` → `StateFlow` | Firebase callback → cold Flow → hot StateFlow via stateIn |

---

## 15. Interview Cheat Sheet

```
QUESTION: What is the difference between StateFlow and SharedFlow?

StateFlow:
- Always has a current value (initial value required)
- Replays last value to new collectors
- Skips duplicate values (== check)
- Use for: UI state (loading, data, errors that persist)

SharedFlow:
- No stored value by default (replay=0)
- New collectors get nothing from the past
- Every emit() reaches collectors
- Use for: one-time events (navigate, toast, snackbar)

───────────────────────────────────────────────────────────

QUESTION: What is the difference between LiveData and StateFlow?

Same purpose (hold UI state), but:
LiveData:   - Android-only, Java-first
            - Lifecycle-aware BUILT IN (no repeatOnLifecycle needed)
            - No duplicate filtering
            - Limited operators (map, switchMap only)
            - Needs InstantTaskExecutorRule in tests

StateFlow:  - Pure Kotlin, Kotlin-first
            - Needs repeatOnLifecycle manually
            - Skips duplicate values
            - Full Flow operators (combine, debounce, flatMapLatest)
            - Pure coroutine tests (Turbine) - fast and clean
            - Works with Kotlin Multiplatform

Recommendation: StateFlow for new code. Read LiveData in legacy code.

───────────────────────────────────────────────────────────

QUESTION: What is the difference between Flow, StateFlow, and SharedFlow?

Flow (cold):
- Doesn't run until collected
- Each collector gets independent execution
- Use in Repository layer for DB / network streams

StateFlow (hot):
- Always running, always has a value
- All collectors share the same stream
- Replays current value to late collectors
- Use in ViewModel for UI state

SharedFlow (hot):
- Always running, no stored value (default)
- All collectors share the same stream
- No replay by default — late collectors miss past events
- Use in ViewModel for one-time events

───────────────────────────────────────────────────────────

QUESTION: When would you use Compose State vs StateFlow?

Compose State (mutableStateOf):
- Ephemeral UI-only state (is dropdown open, animation progress)
- State that has zero business meaning
- Lives inside @Composable functions
- Lost on rotation (unless rememberSaveable)

StateFlow:
- State that has business meaning
- State that must survive rotation (lives in ViewModel)
- State that can be tested without Compose
- State that can be combined using Flow operators
- State shared between multiple composables or screens

───────────────────────────────────────────────────────────

GOLDEN RULES (one-liners):

"StateFlow = current state. SharedFlow = something just happened."
"Compose State = inside the composable. StateFlow = inside the ViewModel."
"Cold Flow = in the repository. Hot Flow = in the ViewModel."
"LiveData = legacy. StateFlow = modern. SharedFlow = events."
"Never expose MutableStateFlow. Never use SharedFlow for state."
```

---

## 🔗 Connections

Deep dives for each primitive:
- [[State]] — Compose State, `remember`, `rememberSaveable`, `derivedStateOf`, hoisting
- [[StateFlow]] — ViewModel pattern, `update{}`, `stateIn`, sealed UiState
- [[SharedFlow]] — Event bus, `emit` vs `tryEmit`, `LaunchedEffect` collection
- [[Cold Flow]] — `flow{}`, `callbackFlow`, operators, backpressure, Repository architecture
- [[LiveData]] — Legacy guide, `observe`, `postValue`, migration to StateFlow
