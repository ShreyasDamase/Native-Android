# State vs StateFlow

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
