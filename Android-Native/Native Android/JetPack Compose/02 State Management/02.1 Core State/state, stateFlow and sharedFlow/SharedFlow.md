# 📡 SharedFlow — Complete Guide

> [!NOTE]
> **Source of truth for SharedFlow.** For StateFlow (UI state), see [[StateFlow]]. For Compose State primitives (`remember`, `mutableStateOf`), see [[State]]. For cold Flow theory and operators, see [[Cold Flow]].

---

## 🔍 Master Lookup Table — Find Any Concept Instantly

> Click any item to jump straight to its full explanation, code example, and gotchas.

### Creating & Exposing SharedFlow

| API / Concept | One-Line Purpose | Jump To |
|---|---|---|
| `MutableSharedFlow<T>()` | Create a writable broadcast flow — no initial value required | [[#2. Creating SharedFlow\|§2 Creating SharedFlow]] |
| `.asSharedFlow()` | Cast to read-only `SharedFlow<T>` — prevents UI from emitting | [[#2. Creating SharedFlow\|§2 Creating SharedFlow]] |
| Backing property `_events` / `events` | Pattern: private mutable + public read-only | [[#2. Creating SharedFlow\|§2 Creating SharedFlow]] |
| `sealed class UiEvent` | Type-safe set of one-time events (Navigate, ShowToast, etc.) | [[#2. Creating SharedFlow\|§2 Creating SharedFlow]] |

### Emitting Events

| API / Concept | One-Line Purpose | Jump To |
|---|---|---|
| `emit(value)` | Suspend function — safe, waits if buffer is full. Use inside coroutine | [[#3. Emitting Events — emit() vs tryEmit()\|§3 emit() vs tryEmit()]] |
| `tryEmit(value)` | Non-suspending — returns `false` if buffer full. Use in callbacks | [[#3. Emitting Events — emit() vs tryEmit()\|§3 emit() vs tryEmit()]] |
| Multiple sequential `emit()` calls | Events fire in order within the same coroutine | [[#6. Real-World Patterns\|§6 Patterns]] |

### Collecting SharedFlow in UI

| API / Concept | One-Line Purpose | Jump To |
|---|---|---|
| `LaunchedEffect + repeatOnLifecycle(STARTED)` | **Correct** way to collect events in Compose — lifecycle-safe | [[#4. Collecting SharedFlow in Compose\|§4 Collecting in Compose]] |
| `LaunchedEffect(Unit)` | ❌ **Wrong** for events — uses composition lifecycle, not Android lifecycle | [[#4. Collecting SharedFlow in Compose\|§4 Collecting in Compose]] |
| `collectAsStateWithLifecycle()` | ❌ **Wrong** for SharedFlow events — treats one-time event as state | [[#4. Collecting SharedFlow in Compose\|§4 Collecting in Compose]] |
| `repeatOnLifecycle + collect` | Correct way to collect in XML Fragment/Activity | [[#4. Collecting SharedFlow in Compose\|§4 Collecting in Compose]] |

### Configuration Parameters

| Parameter | One-Line Purpose | Jump To |
|---|---|---|
| `replay = 0` (default) | New collectors miss past events — pure event bus | [[#5. SharedFlow Configuration\|§5 Configuration]] |
| `replay = 1` | New collectors get the last emission — use StateFlow instead if this is needed | [[#5. SharedFlow Configuration\|§5 Configuration]] |
| `replay = N` | New collectors get last N emissions — log/notification history | [[#5. SharedFlow Configuration\|§5 Configuration]] |
| `extraBufferCapacity` | How many events can queue up before emitter suspends | [[#5. SharedFlow Configuration\|§5 Configuration]] |
| `BufferOverflow.SUSPEND` (default) | `emit()` suspends when buffer is full | [[#5. SharedFlow Configuration\|§5 Configuration]] |
| `BufferOverflow.DROP_OLDEST` | Drop oldest buffered event to make room for new one | [[#5. SharedFlow Configuration\|§5 Configuration]] |
| `BufferOverflow.DROP_LATEST` | Drop the incoming new event when buffer is full | [[#5. SharedFlow Configuration\|§5 Configuration]] |

### Real-World Event Patterns

| Pattern | One-Line Purpose | Jump To |
|---|---|---|
| Delete + Undo Snackbar | Emit event with payload to show undo action | [[#6. Real-World Patterns\|§6 Patterns]] |
| Form submit → navigate back | Emit success event to trigger navigation | [[#6. Real-World Patterns\|§6 Patterns]] |
| Multiple sequential events | Dismiss dialog → show loading → hide loading → navigate | [[#6. Real-World Patterns\|§6 Patterns]] |
| One-Time Events Architecture | ViewModel: StateFlow for state + SharedFlow for events | [[#7. One-Time Events Architecture\|§7 Events Architecture]] |

### The Core Decision

| Question | Answer | Jump To |
|---|---|---|
| Should it survive screen rotation and re-show? | ✅ StateFlow | [[#1. StateFlow vs SharedFlow — The Decision\|§1 Decision]] |
| Should it fire once and never repeat? | ✅ SharedFlow (replay=0) | [[#1. StateFlow vs SharedFlow — The Decision\|§1 Decision]] |
| Navigation event | ✅ SharedFlow | [[#1. StateFlow vs SharedFlow — The Decision\|§1 Decision]] |
| Toast / Snackbar (temporary) | ✅ SharedFlow | [[#1. StateFlow vs SharedFlow — The Decision\|§1 Decision]] |
| Persistent error text in form | ✅ StateFlow (it's display state) | [[#7. One-Time Events Architecture\|§7 Events Architecture]] |
| Loading indicator | ✅ StateFlow | [[#1. StateFlow vs SharedFlow — The Decision\|§1 Decision]] |

### Comparisons

| Comparison | Jump To |
|---|---|
| SharedFlow vs StateFlow | [[#1. StateFlow vs SharedFlow — The Decision\|§1 Decision]] |
| SharedFlow vs Channel | [[#8. SharedFlow vs Channel\|§8 vs Channel]] |
| emit() vs tryEmit() | [[#3. Emitting Events — emit() vs tryEmit()\|§3 Emitting]] |

### Common Mistakes (Quick Reference)

| Mistake | Fix | Jump To |
|---|---|---|
| `collectAsStateWithLifecycle()` for events | Use `LaunchedEffect + repeatOnLifecycle + collect` | [[#9. Common Mistakes\|§9 Mistakes]] |
| `emit()` outside a coroutine | Wrap in `viewModelScope.launch { }` | [[#9. Common Mistakes\|§9 Mistakes]] |
| StateFlow for navigation (re-navigates on rotation) | Use `SharedFlow(replay=0)` | [[#9. Common Mistakes\|§9 Mistakes]] |
| Exposing `MutableSharedFlow` publicly | Private `_events`, public `events = _events.asSharedFlow()` | [[#9. Common Mistakes\|§9 Mistakes]] |
| `SharedFlow(replay=1)` when you mean StateFlow | Use `StateFlow` — it's clearer and has an initial value | [[#9. Common Mistakes\|§9 Mistakes]] |

---

## 🧠 Mental Model — Read This First

**SharedFlow is a loudspeaker.**
- **Hot:** The speaker plays music whether anyone is in the room or not.
- **Event-driven:** Once a sound plays, it's gone. If you walk in late, you missed it (unless `replay > 0`).
- **Multiple listeners:** Everyone in the room hears the same sound at the same time.

**The key question to ask:**

```
"Should the UI see this again on screen rotation?"

YES → StateFlow  (it replays last value automatically)
NO  → SharedFlow (replay=0, no re-delivery)
```

---

## 📌 Table of Contents
1. [StateFlow vs SharedFlow — The Decision](#1-decision)
2. [Creating SharedFlow](#2-creating)
3. [Emitting Events — emit() vs tryEmit()](#3-emitting)
4. [Collecting SharedFlow in Compose](#4-collecting)
5. [SharedFlow Configuration](#5-configuration)
6. [Real-World Patterns](#6-patterns)
7. [One-Time Events Architecture](#7-events-architecture)
8. [SharedFlow vs Channel](#8-vs-channel)
9. [Common Mistakes](#9-mistakes)
10. [Interview Q&A](#10-interview)

---

## 1. StateFlow vs SharedFlow — The Decision

| | StateFlow | SharedFlow |
|---|---|---|
| Holds current state | ✅ Always | ❌ Optional (replay) |
| Initial value required | ✅ Yes | ❌ No |
| Replays on new collector | ✅ Last value | Configurable (default 0) |
| Duplicate filtering | ✅ Skips equal values | ❌ Emits every call |
| **Perfect for** | UI State | One-time Events |

### The Decision Tree

```
Does this data represent what the screen currently LOOKS LIKE?
    YES → StateFlow
    (current user, loading flag, error state, list items)

Does this data represent something that HAPPENED ONCE?
    YES → SharedFlow
    (navigate to next screen, show toast, show snackbar, show dialog)
```

### Why Navigation Must Be SharedFlow

```kotlin
// ❌ WRONG — StateFlow approach for navigation
private val _navigateToHome = MutableStateFlow(false)

// Flow:
// 1. Login succeeds → _navigateToHome.value = true → navigates ✅
// 2. User rotates screen
// 3. New composition collects StateFlow → sees true → navigates AGAIN 💥
// Result: user navigated twice from the same login!

// ✅ CORRECT — SharedFlow approach
private val _events = MutableSharedFlow<UiEvent>()

// Flow:
// 1. Login succeeds → emit(UiEvent.NavigateToHome) → navigates ✅
// 2. User rotates screen
// 3. New composition collects SharedFlow → sees nothing (replay=0) → no duplicate navigation ✅
```

---

## 2. Creating SharedFlow

### Basic Declaration

```kotlin
class LoginViewModel : ViewModel() {

    // SharedFlow — for one-time UI events
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()
}
```

### Sealed Class for Events

```kotlin
sealed class UiEvent {
    data class ShowToast(val message: String)   : UiEvent()
    data class ShowError(val message: String)   : UiEvent()
    data class NavigateTo(val route: String)    : UiEvent()
    object NavigateBack                         : UiEvent()
    object ShowLoadingDialog                    : UiEvent()
}
```

### Complete ViewModel Pattern (State + Events)

```kotlin
class LoginViewModel : ViewModel() {

    // STATE — persists, survives rotation, always has current value
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // EVENTS — one-time, NOT re-delivered on rotation
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    fun onLoginClicked(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                repository.login(email, password)
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(UiEvent.NavigateTo("home"))        // one-time: navigate
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(UiEvent.ShowError(e.message ?: "Login failed"))  // one-time: snackbar
            }
        }
    }
}
```

---

## 3. Emitting Events — emit() vs tryEmit()

### emit() — The Standard Way

```kotlin
// emit() is a suspend function — must be called inside a coroutine
viewModelScope.launch {
    _events.emit(UiEvent.ShowToast("Saved!"))
}

// Or directly inside a coroutine builder
fun onDeleteClicked(itemId: String) {
    viewModelScope.launch {
        repository.delete(itemId)
        _events.emit(UiEvent.ShowToast("Item deleted"))
        _events.emit(UiEvent.NavigateBack)
    }
}
```

### tryEmit() — Non-Suspending Alternative

```kotlin
// tryEmit() returns Boolean — true if emitted, false if buffer was full
// Use when you're NOT in a coroutine (callbacks, click listeners without coroutine scope)
_events.tryEmit(UiEvent.ShowToast("Hello"))

// Check return value if needed
val emitted = _events.tryEmit(UiEvent.ShowToast("Hello"))
if (!emitted) {
    // Buffer was full — emission dropped
    Log.w("TAG", "Event dropped: buffer full")
}
```

| | `emit()` | `tryEmit()` |
|---|---|---|
| Suspends on full buffer | ✅ Yes | ❌ No (returns false) |
| Context required | Coroutine | Anywhere |
| Use in | `viewModelScope.launch {}` | Callbacks, non-suspend code |
| Safety | Always emits if caller waits | May drop if buffer is full |

---

## 4. Collecting SharedFlow in Compose

> [!WARNING]
> Do NOT use `collectAsStateWithLifecycle()` for SharedFlow events. This converts a one-time event into a state — and Compose might read the same event multiple times or drop events during rapid recompositions.

### Correct Pattern — LaunchedEffect + repeatOnLifecycle

```kotlin
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onNavigate: (String) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // ✅ Collect events with lifecycle awareness
    LaunchedEffect(viewModel.events, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is UiEvent.ShowToast ->
                        snackbarHostState.showSnackbar(event.message)
                    is UiEvent.ShowError ->
                        snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Long)
                    is UiEvent.NavigateTo ->
                        onNavigate(event.route)
                    UiEvent.NavigateBack ->
                        onNavigate("back")
                }
            }
        }
    }

    // State collection — normal way
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
        // UI here using uiState
    }
}
```

### Why LaunchedEffect + repeatOnLifecycle?

```kotlin
// ❌ WRONG — LaunchedEffect(Unit) uses composition lifecycle, not Android lifecycle
LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
        // If user presses Home → composition is NOT destroyed → keeps collecting
        // If navigation event fires while in background → crash!
    }
}

// ✅ CORRECT — repeatOnLifecycle uses Android Activity/Fragment lifecycle
LaunchedEffect(viewModel.events, lifecycleOwner) {
    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        // Automatically pauses when app goes to background (STOPPED)
        // Automatically resumes when app comes back (STARTED)
        viewModel.events.collect { event -> /* handle */ }
    }
}
```

---

## 5. SharedFlow Configuration

```kotlin
MutableSharedFlow<T>(
    replay = 0,                              // how many past values new collectors get
    extraBufferCapacity = 0,                 // additional buffer beyond replay
    onBufferOverflow = BufferOverflow.SUSPEND // what to do when buffer is full
)
```

### replay Parameter

```kotlin
// replay = 0 (default): pure event bus — new collectors miss past events
private val _events = MutableSharedFlow<UiEvent>()

// replay = 1: new collectors get the last event — similar to StateFlow but no initial value
private val _data = MutableSharedFlow<String>(replay = 1)

// replay = N: new collectors get the last N events
private val _logs = MutableSharedFlow<String>(replay = 10)
```

| replay | Behavior | Use Case |
|---|---|---|
| 0 (default) | Pure event bus — new collectors miss past | Navigation, toast, one-time actions |
| 1 | New collectors see last emission | "Latest alert" scenarios |
| N | New collectors see last N emissions | Log viewer, notification history |

### extraBufferCapacity

```kotlin
// Without buffer: if no collector is active, emit() suspends until one attaches
private val _events = MutableSharedFlow<UiEvent>()

// With buffer: emit() can fire even if no collectors are ready, up to 64 buffered
private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 64)
```

### onBufferOverflow

```kotlin
MutableSharedFlow<T>(
    extraBufferCapacity = 64,
    onBufferOverflow = BufferOverflow.DROP_OLDEST  // drop oldest when full
    // OR:
    // onBufferOverflow = BufferOverflow.DROP_LATEST  // drop new events when full
    // onBufferOverflow = BufferOverflow.SUSPEND      // suspend emitter (default)
)
```

---

## 6. Real-World Patterns

### Pattern 1: Delete with Undo Snackbar

```kotlin
class ItemListViewModel(private val repo: ItemRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemListState())
    val uiState: StateFlow<ItemListState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ItemEvent>()
    val events: SharedFlow<ItemEvent> = _events.asSharedFlow()

    fun onDeleteItem(item: Item) {
        viewModelScope.launch {
            repo.delete(item)
            _uiState.update { it.copy(items = it.items - item) }
            _events.emit(ItemEvent.ShowUndoSnackbar("${item.name} deleted", item))
        }
    }

    fun onUndoDelete(item: Item) {
        viewModelScope.launch {
            repo.insert(item)
            _uiState.update { it.copy(items = it.items + item) }
        }
    }
}

sealed class ItemEvent {
    data class ShowUndoSnackbar(val message: String, val item: Item) : ItemEvent()
    object NavigateBack : ItemEvent()
}
```

### Pattern 2: Form Submit with Navigation

```kotlin
class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>()
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

    fun onSaveProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                repo.saveProfile(_uiState.value.toProfile())
                _events.emit(ProfileEvent.ShowSnackbar("Profile saved!"))
                _events.emit(ProfileEvent.NavigateBack)
            } catch (e: Exception) {
                _events.emit(ProfileEvent.ShowSnackbar("Failed: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}

sealed class ProfileEvent {
    data class ShowSnackbar(val message: String) : ProfileEvent()
    object NavigateBack : ProfileEvent()
}
```

### Pattern 3: Multiple Events in Sequence

```kotlin
// Multiple events in the same coroutine execute in order
fun onConfirmCheckout() {
    viewModelScope.launch {
        _events.emit(UiEvent.DismissDialog)         // 1. close confirmation dialog
        _events.emit(UiEvent.ShowLoading)            // 2. show progress
        repo.processPayment()
        _events.emit(UiEvent.HideLoading)            // 3. hide progress
        _events.emit(UiEvent.NavigateTo("success"))  // 4. go to success screen
    }
}
```

---

## 7. One-Time Events Architecture

### The Full Picture

```
                    ViewModel
            ┌────────────────────────────┐
            │  StateFlow → UI State      │ ← what's currently shown
            │  SharedFlow → UI Events    │ ← what just happened (once)
            └────────────────────────────┘
                         │
                    Composable
            ┌────────────────────────────┐
            │  collectAsStateWithLifecycle → State  │ ← for display
            │  LaunchedEffect + repeatOnLifecycle   │ ← for events
            └────────────────────────────┘
```

### Why Not Put Error in StateFlow?

```kotlin
// ❌ Error as STATE — replays on rotation
data class UiState(
    val error: String? = "Login failed"
)
// User sees error snackbar
// User rotates screen
// StateFlow replays last state → error is still there → snackbar shows AGAIN

// ✅ Error as EVENT — no replay
sealed class UiEvent {
    data class ShowError(val message: String) : UiEvent()
}
// User sees error snackbar
// User rotates screen
// SharedFlow (replay=0) → new collector gets nothing → no duplicate snackbar
```

**However — for persistent error display (a visible error text in the form), use StateFlow:**
```kotlin
// If the error message should be VISIBLE in the UI and survive rotation:
data class UiState(val emailError: String? = null)
// This is STATE — use StateFlow

// If the error is a temporary snackbar:
sealed class UiEvent { data class ShowSnackbar(val msg: String) : UiEvent() }
// This is an EVENT — use SharedFlow
```

---

## 8. SharedFlow vs Channel

Both can be used for one-time events. Here's the comparison:

| | SharedFlow | Channel |
|---|---|---|
| Multiple collectors | ✅ Broadcasts to all | ❌ Only ONE collector receives |
| Missed events | Drops (replay=0) | Buffers (queued) |
| Design | Broadcast | Point-to-point |
| Typical use | UI Events (multiple composables might listen) | Background task results |

**Use SharedFlow for UI events** — the ViewModel doesn't know which composable will handle the event. Multiple composables can all listen (e.g., Scaffold, NavHost, Screen all collecting events).

---

## 9. Common Mistakes

### Mistake 1: Using collectAsStateWithLifecycle() for events

```kotlin
// ❌ WRONG — events converted to State may be duplicated
val latestEvent by viewModel.events.collectAsStateWithLifecycle(initialValue = null)

// ✅ CORRECT — use LaunchedEffect
LaunchedEffect(viewModel.events, lifecycleOwner) {
    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { /* handle */ }
    }
}
```

### Mistake 2: Calling emit() outside a coroutine

```kotlin
// ❌ WRONG — emit is a suspend function
fun onClick() {
    _events.emit(UiEvent.ShowToast("Hello"))  // compile error
}

// ✅ CORRECT
fun onClick() {
    viewModelScope.launch {
        _events.emit(UiEvent.ShowToast("Hello"))
    }
}
```

### Mistake 3: Using StateFlow for navigation

```kotlin
// ❌ WRONG — replays on rotation
private val _shouldNavigate = MutableStateFlow(false)

// ✅ CORRECT
private val _events = MutableSharedFlow<NavigationEvent>()
```

### Mistake 4: Exposing MutableSharedFlow

```kotlin
// ❌ WRONG — any code can emit events
val events = MutableSharedFlow<UiEvent>()    // public, mutable!

// ✅ CORRECT
private val _events = MutableSharedFlow<UiEvent>()
val events: SharedFlow<UiEvent> = _events.asSharedFlow()
```

### Mistake 5: Using replay=1 when you need StateFlow

```kotlin
// ❌ CONFUSING — SharedFlow(replay=1) ≈ StateFlow but with no initial value requirement
// This is an anti-pattern — just use StateFlow if you need replay=1
private val _data = MutableSharedFlow<String>(replay = 1)

// ✅ If you need the last value to be available always → use StateFlow
private val _data = MutableStateFlow("")
```

---

## 10. Interview Q&A

**Q: When should you use SharedFlow instead of StateFlow?**
> SharedFlow is for one-time UI events — things that should happen once and NOT be re-delivered when the screen rotates. Examples: navigate to another screen, show a Snackbar, dismiss a dialog. StateFlow is for persistent state — things that represent what the screen currently looks like and SHOULD be re-delivered after rotation (loading flag, user data, error text visible in a form).

**Q: What is replay in SharedFlow and what does replay=0 mean?**
> `replay` controls how many past emissions a new collector receives when it first subscribes. `replay=0` (the default) means new collectors get NO past events — they only receive future emissions. This is exactly what you want for navigation or toasts: if the event fired before the screen was visible, the screen shouldn't receive it after becoming visible.

**Q: What is the difference between `emit()` and `tryEmit()`?**
> `emit()` is a suspend function that will suspend the caller if the SharedFlow's buffer is full. `tryEmit()` is non-suspending — it immediately returns `false` if the buffer is full instead of waiting. Use `emit()` inside coroutines (it's safer). Use `tryEmit()` in non-coroutine contexts like legacy callbacks where you cannot suspend.

**Q: Why should events be SharedFlow (not Channel)?**
> Channels are point-to-point — only one collector receives each emission. SharedFlow broadcasts to ALL active collectors simultaneously. In a Compose UI, multiple parts of the screen (Scaffold, NavHost, individual composables) might collect events from the same ViewModel. SharedFlow is the correct choice because it ensures every interested party receives the event, not just one.

**Q: Can I use LaunchedEffect without repeatOnLifecycle for events?**
> Not safely. `LaunchedEffect(Unit)` ties to the Compose composition lifecycle, not the Android Activity lifecycle. If the user presses the Home button, the Activity is in STOPPED state but the composition is still alive. Without `repeatOnLifecycle`, the event collection continues in the background. If a navigation event fires at this point, the app will try to execute a navigation transaction while invisible — causing crashes. `repeatOnLifecycle(STARTED)` automatically pauses collection when the app is in the background and resumes when it returns.

---

## 📦 Gradle Dependencies

```kotlin
// SharedFlow is part of Kotlin Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

// For ViewModel and viewModelScope
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")

// For lifecycle-aware collection in Compose
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
// (provides collectAsStateWithLifecycle and LocalLifecycleOwner)
```

---

## 🔗 Connections

- **State counterpart**: [[StateFlow]] — when to use StateFlow vs SharedFlow
- **Compose primitives**: [[State]] — how Compose state system works
- **Full Flow theory**: [[Cold Flow]] — Flow operators, hot vs cold, stateIn
- **Coroutines**: [[Coroutines/Coroutines in Kotlin Complete Notes]] — viewModelScope, repeatOnLifecycle
