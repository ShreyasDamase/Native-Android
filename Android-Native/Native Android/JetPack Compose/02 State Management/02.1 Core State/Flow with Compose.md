# Consuming Flows in Jetpack Compose

> [!NOTE]
> This file focuses only on collecting Flow/StateFlow in Compose. For coroutine foundations, use [[Coroutines/Coroutines in Kotlin Complete Notes]]. For Flow theory and operators, use [[Android Jetpack Flow Study Notes]].

## 📌 Purpose
While Jetpack Compose has its own `State` system (`MutableState`), Android apps heavily rely on Kotlin Coroutines and `Flow` / `StateFlow` for domain and data layers. To use a `Flow` in Compose, it must be converted into Compose `State` so that emissions trigger recompositions.

> [!NOTE]
> Converting a Flow to State is necessary because Compose's recomposition engine only knows how to track reads of `androidx.compose.runtime.State`.

## 🔧 Function Signatures

### `collectAsStateWithLifecycle`
```kotlin
@Composable
fun <T> StateFlow<T>.collectAsStateWithLifecycle(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    context: CoroutineContext = EmptyCoroutineContext
): State<T>
```

### `collectAsState` (⚠️ Generally discouraged for UI Layer)
```kotlin
@Composable
fun <T> StateFlow<T>.collectAsState(
    context: CoroutineContext = EmptyCoroutineContext
): State<T>
```

## 📋 Props / Parameters (`collectAsStateWithLifecycle`)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `lifecycleOwner` | `LifecycleOwner` | `LocalLifecycleOwner.current` | The lifecycle owner to bind to. |
| `minActiveState` | `Lifecycle.State` | `Lifecycle.State.STARTED` | The minimum lifecycle state the owner must be in for the Flow to be collected. |
| `context` | `CoroutineContext` | `EmptyCoroutineContext` | Context to run the collection in. |

## ✅ Basic Example

```kotlin
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun UserProfileScreen(viewModel: UserProfileViewModel) {
    // Safely collect the StateFlow. The UI will only collect when it is at least STARTED.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState) {
        is UiState.Loading -> CircularProgressIndicator()
        is UiState.Success -> Text("User: ${(uiState as UiState.Success).name}")
        is UiState.Error -> Text("Error loading profile")
    }
}
```

## 🚀 Advanced Examples

### Collecting a regular Flow (requires an initial value)

When collecting a standard `Flow<T>` (not a `StateFlow`), you must provide an `initialValue` because a standard Flow doesn't hold a current value.

```kotlin
@Composable
fun LocationScreen(locationFlow: Flow<Location>) {
    // Requires an initial value since standard Flows don't have a "current" value
    val location by locationFlow.collectAsStateWithLifecycle(
        initialValue = Location.Unknown
    )

    Text("Current Location: $location")
}
```

### Changing the Minimum Active State

Sometimes you might want to continue collecting even if the app goes into the background but is not yet destroyed. (e.g., Audio playback UI).

```kotlin
@Composable
fun AudioPlayerScreen(viewModel: AudioViewModel) {
    // Collect even when paused, stop only when destroyed.
    val audioState by viewModel.audioState.collectAsStateWithLifecycle(
        minActiveState = Lifecycle.State.CREATED
    )

    // UI code here
}
```

## ⚠️ Common Gotchas

> [!WARNING]
> **Why `collectAsState` is dangerous:** `collectAsState` keeps collecting the Flow as long as the Composable is in the composition, *even if the app is in the background*. This means if your Flow is backed by a repository making database or network calls, it will keep doing work in the background, wasting battery and potentially crashing the app if it tries to access resources unavailable in the background.

1. **Missing Dependency**: `collectAsStateWithLifecycle` is not built into the core Compose runtime. You need the `androidx.lifecycle:lifecycle-runtime-compose` artifact.
2. **Double Collecting**: Do not collect the flow in a `LaunchedEffect` and also use `collectAsStateWithLifecycle`. Choose one. Usually, `collectAsStateWithLifecycle` is for UI state, while `LaunchedEffect` is for one-time side effects (like showing a Snackbar).
3. **Using `collectAsState` by habit**: Always default to `collectAsStateWithLifecycle` for UI state. Only use `collectAsState` in platform-agnostic code (like Compose Multiplatform) where Android lifecycles don't apply.

## 💡 Interview Q&A

**Q: What is the difference between `collectAsState` and `collectAsStateWithLifecycle`?**
A: `collectAsState` collects the Flow as long as the Composable is in the UI tree, ignoring the Android Lifecycle. This can lead to wasted resources when the app is in the background. `collectAsStateWithLifecycle` automatically pauses collection when the app goes into the background (e.g., below the `STARTED` state) and resumes when it comes to the foreground, saving resources and preventing background crashes.

**Q: How do you handle one-time events (like navigation or showing a toast) from a Flow?**
A: You should *not* use `collectAsStateWithLifecycle` for one-time events because state represents a value over time, and a screen rotation might re-trigger the state read. Instead, use a `LaunchedEffect` to collect the event Flow, or use a specific event-handling mechanism in your ViewModel.

**Q: Can I use `collectAsStateWithLifecycle` on a standard `Flow`?**
A: Yes, but you must provide an `initialValue` parameter, as standard Flows do not inherently store a current state like `StateFlow` does.


---

## 🚀 Mastery Deep Dive (Added 2026)

> [!NOTE]
> The following deep dive notes were generated to provide mastery-level understanding, complementing the original notes above.

# Collecting Flow in Jetpack Compose

> [!NOTE]
> There are multiple ways to collect a Flow in Compose. Only one is correct for state, and only one is correct for one-time events. Doing this wrong causes silent resource leaks in production.

---

## 🧠 Mental Model — Read This First

**Compose is a Lifecycle.**
Your screen exists (STARTED/RESUMED). Then the user presses the Home button, and your screen is hidden (STOPPED). The Android OS needs memory, so it might kill your app.

**Flow is a Firehose.**
If you connect the firehose to the screen, but the screen is hidden, the firehose *keeps spraying water*. This drains the battery and crashes the app if you try to update UI that isn't there.

**The Solution:** You need a smart valve that automatically shuts off the firehose when the screen is hidden, and turns it back on when the screen returns.

---

## 📊 The Flow Lifecycle Diagram

What happens when you use `collectAsStateWithLifecycle()`:

```
App State:      | Background (Home button) | Foreground | Background | Foreground
Lifecycle: CREATED      STARTED   RESUMED      STOPPED     RESUMED      STOPPED
              │            │         │            │           │            │
collectAs     │            │         │            │           │            │
StateWith     │ (Connect)  │         │(Disconnect)│ (Connect) │(Disconnect)│
Lifecycle:    ├────────────►    Spraying...       x           ► Spraying...x
              │            │                      │           │            │
```

Notice it connects/disconnects at `STARTED`/`STOPPED`. This is perfectly aligned with when the UI is actually visible to the user.

---

## ✅ 1. Collecting State (`StateFlow` / `Flow`)

Use this for data that represents the current UI (lists, counters, text fields).

### The Golden Standard

```kotlin
// In your build.gradle:
// implementation "androidx.lifecycle:lifecycle-runtime-compose:2.6.x"

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    // 🔥 THIS IS THE ONLY CORRECT WAY TO COLLECT STATE IN COMPOSE
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Text("Hello, ${uiState.name}")
}
```

### Why the old way is dangerous

```kotlin
@Composable
fun BadProfileScreen(viewModel: ProfileViewModel) {
    // ❌ DANGEROUS: collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Text("Hello, ${uiState.name}")
}
```
If you use `collectAsState()` and the user puts the app in the background, the ViewModel continues collecting the Flow. If that Flow is listening to a database (Room) or a GPS location stream, it will continue executing the query/GPS hardware checks forever, draining the battery.

---

## ✅ 2. Collecting Events (`SharedFlow`)

Use this for one-time actions (Navigation, Snackbars, Toasts).

> [!WARNING]
> DO NOT use `collectAsStateWithLifecycle()` for events! If you convert a `SharedFlow` of events into State, Compose might drop events during rapid recompositions, or it might incorrectly trigger the same event twice.

### The Golden Standard for Events

```kotlin
@Composable
fun LoginScreen(viewModel: LoginViewModel, onNavigate: (String) -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }

    // 🔥 THIS IS THE CORRECT WAY TO COLLECT ONE-TIME EVENTS
    // Use the true lifecycle owner, not just the composition lifecycle
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel.events, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is UiEvent.NavigateToHome -> onNavigate("home")
                    is UiEvent.ShowError -> snackbarHostState.showSnackbar(event.msg)
                }
            }
        }
    }

    // ... UI elements
}
```

### Why the old way is dangerous

```kotlin
@Composable
fun BadLoginScreen(viewModel: LoginViewModel) {
    // ❌ DANGEROUS: LaunchedEffect(Unit)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            // ...
        }
    }
}
```
`LaunchedEffect(Unit)` is tied to the **Composition** lifecycle, not the **Activity/Fragment** lifecycle. If the user presses the Home button, the composition is NOT destroyed. The LaunchedEffect keeps running. If a navigation event comes in while the app is in the background, the app will crash trying to execute a FragmentTransaction.

`repeatOnLifecycle(STARTED)` safely pauses the collection when the app goes to the background and resumes it when it comes back.

---

## 🚦 Handling Side Effects with Flow in Compose

Sometimes you want to fire an API call based on Compose State changing.

### Debouncing a Search Field

```kotlin
@Composable
fun SearchScreen(viewModel: SearchViewModel) {
    var query by remember { mutableStateOf("") }

    // Convert Compose State to a Flow so we can use powerful Flow operators!
    val queryFlow = snapshotFlow { query }

    LaunchedEffect(Unit) {
        queryFlow
            .debounce(500) // Wait 500ms after user stops typing
            .filter { it.length >= 3 } // Only search if >= 3 chars
            .distinctUntilChanged()
            .collectLatest { validQuery ->
                // collectLatest cancels the previous block if a new query arrives
                viewModel.search(validQuery)
            }
    }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it }
    )
}
```
`snapshotFlow { }` is the exact opposite of `collectAsState`. It takes a Compose `State` and converts it INTO a cold Kotlin `Flow`.

---

## 🆚 Quick Reference Table

| You have... | You want... | Use this |
|---|---|---|
| `StateFlow<T>` in ViewModel | `T` in Compose UI | `collectAsStateWithLifecycle()` |
| `SharedFlow<Event>` in ViewModel | To run an action (Toast) | `LaunchedEffect` + `repeatOnLifecycle` |
| `Flow<T>` from Room DB | `T` in Compose UI | `collectAsStateWithLifecycle(initial)` |
| Compose `State<T>` | `Flow<T>` for operators | `snapshotFlow { state }` |
| A suspend function call | To run on first load | `LaunchedEffect(Unit) { ... }` |

---

## 💬 Interview Master Q&A

**Q: What is the difference between `collectAsState()` and `collectAsStateWithLifecycle()`?**
> `collectAsState()` stays active as long as the composable is in the composition tree. If the user puts the app in the background, the composition is paused, but not destroyed, meaning `collectAsState()` keeps collecting the Flow, wasting resources. `collectAsStateWithLifecycle()` uses Android's true `Lifecycle`. It automatically pauses collection when the lifecycle falls below `STARTED` (e.g., app goes to background) and resumes it when `STARTED` again. This prevents memory leaks and battery drain.

**Q: How do you handle one-time UI events like Navigation or Snackbars from a ViewModel in Compose?**
> UI Events should be exposed as a `SharedFlow` from the ViewModel. In Compose, you should NOT collect them as state, because state drops rapid duplicate values and is meant to represent continuous data. Instead, you use a `LaunchedEffect` with `LocalLifecycleOwner.current`. Inside the block, you use `lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED)` to safely collect the `SharedFlow`. This ensures events are only processed when the UI is actually visible and capable of handling them safely.

**Q: What does `snapshotFlow` do?**
> `snapshotFlow` bridges Compose back to pure Kotlin. It takes a block of code that reads Compose `State` variables and converts it into a cold Kotlin `Flow`. Every time the Compose State changes, the flow emits a new value. This is incredibly useful when you want to use advanced Flow operators — like `debounce`, `filter`, or `distinctUntilChanged` — on state that originates in the Compose UI, such as a user typing in a search text field.
