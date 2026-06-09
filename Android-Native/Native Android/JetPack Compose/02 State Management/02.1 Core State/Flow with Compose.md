# Consuming Flows in Jetpack Compose

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
