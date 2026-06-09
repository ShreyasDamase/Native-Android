# derivedStateOf (Computed State)

## 📌 Purpose
`derivedStateOf` is used to optimize performance by computing a new state from other existing states. It ensures that recomposition *only* happens when the **computed result changes**, rather than every time the underlying states change.

> [!IMPORTANT]
> Use `derivedStateOf` when your UI state is derived from another state that changes more frequently than you need your UI to update.

## 🔧 Function Signature
```kotlin
@Composable
fun <T> derivedStateOf(
    calculation: () -> T
): State<T>
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `calculation` | `() -> T` | — | A block of code that computes the derived value. It will track any Compose `State` read within it. |

## ✅ Basic Example

Imagine a scrollable list. You want to show a "Scroll to Top" button, but only when the user has scrolled past the first item.

### ❌ BAD: Recomposes on every single pixel scroll
```kotlin
@Composable
fun BadScrollList() {
    val listState = rememberLazyListState()
    
    // BAD! listState.firstVisibleItemIndex changes frequently during scrolling.
    // Compose reads this value directly, causing this entire Composable to 
    // recompose continuously as the user scrolls.
    val showButton = listState.firstVisibleItemIndex > 0

    LazyColumn(state = listState) {
        // items...
    }
    
    if (showButton) {
        Button(onClick = { /* scroll to top */ }) { Text("Up") }
    }
}
```

### ✅ GOOD: Using derivedStateOf
```kotlin
@Composable
fun GoodScrollList() {
    val listState = rememberLazyListState()
    
    // GOOD! The calculation block reads listState, but 'showButton' 
    // ONLY changes its boolean state (and triggers recomposition) 
    // when the condition switches from false -> true, or true -> false.
    val showButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    LazyColumn(state = listState) {
        // items...
    }
    
    if (showButton) {
        Button(onClick = { /* scroll to top */ }) { Text("Up") }
    }
}
```

## 🚀 Advanced Examples

### Deriving state from multiple sources
`derivedStateOf` is perfect when combining multiple fast-changing states into a single, slower-changing result.

```kotlin
@Composable
fun PasswordValidator(passwordState: State<String>, confirmPasswordState: State<String>) {
    // Both text fields change on every keystroke.
    // But isValid will only trigger recomposition when it flips between true/false.
    val isValid by remember {
        derivedStateOf {
            passwordState.value.length >= 8 &&
            passwordState.value == confirmPasswordState.value
        }
    }

    Button(
        onClick = { /* submit */ },
        enabled = isValid
    ) {
        Text("Submit")
    }
}
```

## ⚠️ Common Gotchas

1. **Forgetting `remember`**: `derivedStateOf` creates a `State` object. If you don't wrap it in `remember`, you are allocating a new `State` object on every recomposition, completely defeating its optimization purpose.
   - ❌ `val value by derivedStateOf { ... }`
   - ✅ `val value by remember { derivedStateOf { ... } }`

2. **Using it when NOT needed**: Do NOT use `derivedStateOf` if the result changes just as frequently as the source state.
   - ❌ `val fullName by remember { derivedStateOf { "$firstName $lastName" } }`
   - ✅ `val fullName = "$firstName $lastName"` (Just compute it directly! The composable already recomposes when firstName/lastName change).

3. **Reading non-Compose state**: `derivedStateOf` only tracks reads of Compose `State` objects. If you read standard variables or Kotlin Flows inside the block, it will not react to their changes.

## 💡 Interview Q&A

**Q: What problem does `derivedStateOf` solve?**
A: It solves performance issues caused by excessive recomposition. When a UI relies on a condition (like a boolean) derived from a rapidly changing state (like scroll position), `derivedStateOf` buffers those rapid changes and only triggers recomposition when the resulting computed condition actually changes.

**Q: When should you NOT use `derivedStateOf`?**
A: You should not use it when the resulting value changes at the exact same frequency as the inputs. For example, joining two strings (`firstName + lastName`). In that case, `derivedStateOf` adds unnecessary overhead. Just do the math or string concatenation directly in the Composable body.

**Q: Why must `derivedStateOf` be wrapped in `remember`?**
A: Because `derivedStateOf` produces an object (a `DerivedSnapshotState`). If not remembered, Compose will recreate that object on every recomposition, losing the caching mechanism and causing performance degradation.
