# snapshotFlow

## 📌 Purpose
`snapshotFlow` bridges the gap between Jetpack Compose's state system and Kotlin Coroutines. While `collectAsState` converts a Flow to Compose State, `snapshotFlow` does the exact opposite: **it converts Compose State into a Kotlin Flow**.

> [!NOTE]
> Use `snapshotFlow` when you need to observe Compose state changes in a background coroutine (e.g., to send analytics, debounce user input, or trigger side effects without causing recompositions).

## 🔧 Function Signature
```kotlin
fun <T> snapshotFlow(
    block: () -> T
): Flow<T>
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `block` | `() -> T` | — | A block of code that reads Compose `State` objects. The resulting Flow will emit whenever any `State` read within this block changes. |

## ✅ Basic Example

Imagine you want to log an analytics event every time the user reaches the end of a scrolling list. You don't want to do this in the UI recomposition scope.

```kotlin
@Composable
fun AnalyticsList(items: List<String>) {
    val listState = rememberLazyListState()

    // LaunchedEffect creates a coroutine scope
    LaunchedEffect(listState) {
        // Convert Compose state (listState) into a Flow
        snapshotFlow { 
            // Compute a boolean: are we at the end?
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            
            lastVisible >= totalItems - 1
        }
        // Use Flow operators!
        .distinctUntilChanged() 
        .filter { isAtEnd -> isAtEnd } // Only proceed if true
        .collect {
            // This runs in a coroutine, safely separated from UI recomposition
            Analytics.logEvent("reached_list_end")
        }
    }

    LazyColumn(state = listState) {
        items(items) { Text(it) }
    }
}
```

## 🚀 Advanced Examples

### Debouncing User Input
If you have a search field that queries a database, you don't want to query on every single keystroke. You can use `snapshotFlow` with Flow operators like `debounce`.

```kotlin
@Composable
fun SearchScreen(viewModel: SearchViewModel) {
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
            .debounce(500L) // Wait 500ms after the user stops typing
            .filter { it.isNotBlank() } // Ignore empty queries
            .collect { query ->
                viewModel.performSearch(query)
            }
    }

    OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        label = { Text("Search") }
    )
}
```

## ⚠️ Common Gotchas

1. **Recomposition vs Coroutines**: Do not use `snapshotFlow` just to update other UI state. If you are computing UI state from another UI state, use `derivedStateOf`. Use `snapshotFlow` *only* when you need to bridge Compose state into a Coroutine Flow for side effects (like network calls, database writes, or analytics).
2. **Missing `distinctUntilChanged`**: By default, `snapshotFlow` emits when the state it reads changes. However, if your block returns an object that evaluates as equal to the previous emission, it will still emit unless you use `.distinctUntilChanged()` (Note: `snapshotFlow` has some internal deduplication, but it's best practice to use Flow operators to be explicit about what you are filtering).
3. **Reading Non-Compose State**: The `block` passed to `snapshotFlow` must read `androidx.compose.runtime.State` objects. If it reads plain Kotlin variables, it will not know when to emit new values.

## 💡 Interview Q&A

**Q: What is the difference between `snapshotFlow` and `derivedStateOf`?**
A: Both track Compose state changes. However, `derivedStateOf` outputs a new Compose `State` and is used to minimize recompositions when computing UI logic. `snapshotFlow` outputs a Kotlin `Flow` and is used to export Compose state out of the UI layer into a Coroutine, usually to perform asynchronous side effects (like debouncing or network requests).

**Q: In what block do you typically call `snapshotFlow`?**
A: `snapshotFlow` is almost always called inside a `LaunchedEffect`, because it returns a `Flow` which must be `.collect()`ed within a Coroutine scope tied to the Composable's lifecycle.

**Q: Will `snapshotFlow` emit if the State read inside it doesn't change, but recomposition happens anyway?**
A: No. `snapshotFlow` only re-evaluates its block when the specific Compose `State` objects read inside of it change. It is isolated from the Composable's general recomposition cycle.
