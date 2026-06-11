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


---

## 🚀 Mastery Deep Dive (Added 2026)

> [!NOTE]
> The following deep dive notes were generated to provide mastery-level understanding, complementing the original notes above.

# derivedStateOf — Computed State

> [!NOTE]
> `derivedStateOf` is a targeted performance tool. Most composables don't need it. But when you do need it, it makes the difference between a smooth 60fps scroll and a janky one.

---

## 🧠 Mental Model — Read This First

**`derivedStateOf` is like a smoke detector, not a microphone.**

A microphone records every tiny sound — background noise, air conditioning hum, your breathing. That's what reading a fast-changing state directly does: every pixel of scroll = one recomposition.

A smoke detector only triggers an alarm when the air changes from "normal" to "smoky." It ignores the constant background. That's `derivedStateOf`: it evaluates the calculation on every source state change, but only triggers recomposition when the **result** of that calculation actually changes.

---

## 🔬 Function Signature — Every Word Justified

```kotlin
val showButton by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 0 }
}
//  │               │          │
//  │               │          └── calculation: reads other State objects, computes a result
//  │               └── derivedStateOf: creates a DerivedSnapshotState that re-evaluates
//  │                   the calculation when any state it reads changes, but ONLY propagates
//  │                   recomposition when the computed result itself changes
//  └── remember: MANDATORY — caches the DerivedSnapshotState across recompositions
```

| Part | Role |
|---|---|
| `remember { }` | **Mandatory wrapper.** `derivedStateOf` creates a `DerivedSnapshotState` object. Without `remember`, a new object is allocated on EVERY recomposition — destroying the entire purpose of optimization. |
| `derivedStateOf { }` | Creates the computed state. The lambda is re-executed whenever any `State` read inside it changes. |
| `{ listState.firstVisibleItemIndex > 0 }` | The calculation. Reads `listState` (a State object). Returns a `Boolean`. |
| `val ... by` | Property delegation — returns the `Boolean` directly, not the `State<Boolean>` |

---

## ⚙️ Under the Hood — DerivedSnapshotState

`derivedStateOf` creates a `DerivedSnapshotState<T>` — a special state object that:

1. **On creation**: registers itself as a reader of any `State` objects accessed inside the `calculation` lambda
2. **When source state changes**: re-runs the `calculation` lambda
3. **Compares the result**: If `newResult == oldResult` (using `==`) → does NOT propagate invalidation → composables that read `derivedStateOf` are NOT recomposed
4. **If result changed**: propagates invalidation → composables that read the `derivedStateOf` result ARE recomposed

```
Without derivedStateOf:
listState.firstVisibleItemIndex changes (every pixel of scroll)
    ↓
EVERY composable that reads firstVisibleItemIndex recomposes
    ↓ (if showing a 1000-item list, this is catastrophic)

With derivedStateOf:
listState.firstVisibleItemIndex changes (every pixel of scroll)
    ↓
DerivedSnapshotState re-evaluates: (firstVisibleItemIndex > 0)
    ↓
Result: true (was already true from last scroll) → NO recomposition ✅
Result: false → true (user scrolled past item 0) → recomposition ✅
Result: true → false (user scrolled back to top) → recomposition ✅
```

This is the key insight: **the source state can change 1000 times per scroll, but `derivedStateOf` only fires recomposition the 1-2 times the boolean actually flips.**

---

## ✅ Basic Example — Scroll-to-Top FAB Button

```kotlin
@Composable
fun ProductListScreen() {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // ❌ BAD — recomposes on EVERY pixel of scroll (fires hundreds of times per second)
    // val showScrollToTop = listState.firstVisibleItemIndex > 0

    // ✅ GOOD — only recomposes when the boolean CHANGES (at most twice: false→true, true→false)
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    Box {
        LazyColumn(state = listState) {
            items(1000) { index ->
                ListItem(headlineContent = { Text("Item $index") })
            }
        }

        // AnimatedVisibility reads 'showScrollToTop' — only recomposes when FAB appears/disappears
        AnimatedVisibility(
            visible = showScrollToTop,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                }
            ) {
                Icon(Icons.Default.KeyboardArrowUp, "Scroll to top")
            }
        }
    }
}
```

---

## 🚀 Advanced Examples

### Multi-Field Form Validation

Both password fields change on every keystroke. The submit button only cares about the final boolean result.

```kotlin
@Composable
fun PasswordForm() {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    // Both text fields update on every character typed (very frequent)
    // But isFormValid only changes when the VALIDITY flips (infrequent)
    val isFormValid by remember {
        derivedStateOf {
            password.length >= 8 &&
            password.any { it.isDigit() } &&
            password.any { it.isUpperCase() } &&
            password == confirm
        }
    }

    Column {
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation()
        )

        // This Button only recomposes when isFormValid changes — not every keystroke!
        Button(
            onClick = { /* submit */ },
            enabled = isFormValid
        ) {
            Text("Submit")
        }
    }
}
```

### Search Results Count Badge

```kotlin
@Composable
fun SearchScreen() {
    var query by remember { mutableStateOf("") }
    val allProducts = remember { sampleProducts() }

    // query changes on every character → allProducts.filter runs on every character
    // But resultCountLabel only changes when the count actually changes
    val filteredProducts by remember {
        derivedStateOf {
            if (query.isBlank()) allProducts
            else allProducts.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
    }

    val resultCountLabel by remember {
        derivedStateOf {
            when (filteredProducts.size) {
                0 -> "No results"
                1 -> "1 result"
                else -> "${filteredProducts.size} results"
            }
        }
    }

    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search products...") }
        )
        Text(resultCountLabel)   // only recomposes when count changes, not every character
        LazyColumn {
            items(filteredProducts, key = { it.id }) { product ->
                ProductRow(product)
            }
        }
    }
}
```

### Multi-Section Scroll Header

```kotlin
@Composable
fun ArticleScreen() {
    val listState = rememberLazyListState()

    // Complex derivation: which section header should be shown at the top?
    val currentSection by remember {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex
            when {
                firstVisible < 5  -> "Introduction"
                firstVisible < 15 -> "Main Content"
                firstVisible < 25 -> "Examples"
                else              -> "Conclusion"
            }
        }
    }
    // Changes at most 3 times during the entire scroll — very efficient

    Column {
        // This header only recomposes when you cross a section boundary
        SectionHeader(title = currentSection)

        LazyColumn(state = listState) {
            items(50) { index -> ArticleParagraph(index) }
        }
    }
}
```

### Cart Total — Derived from a List

```kotlin
@Composable
fun CartScreen() {
    val cartItems = remember { mutableStateListOf<CartItem>() }

    // cartItems changes on every add/remove/quantity change
    // But total only recomposes when the numerical total actually changes
    val cartTotal by remember {
        derivedStateOf {
            cartItems.sumOf { it.price * it.quantity }
        }
    }

    val itemCount by remember {
        derivedStateOf { cartItems.sumOf { it.quantity } }
    }

    Column {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(cartItems, key = { it.id }) { item ->
                CartItemRow(
                    item = item,
                    onQuantityChange = { newQty ->
                        val index = cartItems.indexOf(item)
                        if (index >= 0) cartItems[index] = item.copy(quantity = newQty)
                    },
                    onRemove = { cartItems.remove(item) }
                )
            }
        }

        // Total row — only recomposes when total price changes
        Surface(tonalElevation = 8.dp) {
            Row(modifier = Modifier.padding(16.dp)) {
                Text("$itemCount items", modifier = Modifier.weight(1f))
                Text("$${"%.2f".format(cartTotal)}", fontWeight = FontWeight.Bold)
            }
        }

        Button(
            onClick = { /* checkout */ },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            enabled = cartItems.isNotEmpty()
        ) {
            Text("Checkout")
        }
    }
}
```

---

## ❌ When NOT to Use `derivedStateOf`

> [!IMPORTANT]
> `derivedStateOf` adds overhead: it creates an additional `DerivedSnapshotState` object, registers read callbacks, and runs an equality check on every source state change. If the result changes at the SAME rate as the source, you get the overhead with NO benefit.

### Case 1: The result changes as often as the source

```kotlin
// ❌ WRONG USE — fullName changes every time firstName OR lastName changes
// The boolean result would change just as frequently → no savings
val fullName by remember {
    derivedStateOf { "$firstName $lastName" }
}

// ✅ CORRECT — just compute it. The composable already recomposes when these change.
val fullName = "$firstName $lastName"
```

### Case 2: The source doesn't change frequently

```kotlin
// ❌ OVERKILL — isNetworkAvailable doesn't change 60 times per second
val isConnected by remember {
    derivedStateOf { networkState.isAvailable }
}

// ✅ CORRECT — just read it directly
val isConnected = networkState.isAvailable
```

### Case 3: The computation itself is trivial

```kotlin
// ❌ WRONG — the overhead of derivedStateOf exceeds the cost of the if-else
val buttonText by remember {
    derivedStateOf { if (isLoading) "Loading..." else "Submit" }
}

// ✅ CORRECT — just compute inline
val buttonText = if (isLoading) "Loading..." else "Submit"
```

**The Golden Rule:** Use `derivedStateOf` when:
- The SOURCE state changes **more frequently** than you need to recompose
- The computed result changes **less frequently** than the source (like a boolean that flips occasionally vs a scroll position that changes continuously)

---

## 📊 derivedStateOf vs Plain Computation

| Scenario | Use | Reason |
|---|---|---|
| Scroll index → show FAB button | `derivedStateOf` | Index changes hundreds of times/sec; boolean flips maybe twice |
| Form validity from multiple text fields | `derivedStateOf` | Text changes every keystroke; validity flips only occasionally |
| First name + last name → full name string | Plain `val` | Both change at the same rate as the result |
| Product price × quantity → subtotal | Plain `val` (if simple) or `derivedStateOf` (if complex, expensive) | Depends on whether the data changes faster than needed |
| Filter large list on every keystroke | `derivedStateOf` | Filter runs every character, but result changes less often |

---

## 🔗 Connections

- **Foundation**: [[States]] — Snapshot System is what makes `derivedStateOf` work
- **Remember**: [[Remember Mutable State Of]] — `remember` is always required around `derivedStateOf`
- **Performance**: [[State Hoisting Patterns]] — `derivedStateOf` is a complement to hoisting for performance
- **Scroll**: `rememberLazyListState` is the most common `derivedStateOf` use case

---

## 💬 Interview Master Q&A

**Q: What problem does `derivedStateOf` solve?**
> It solves excessive recomposition caused by fast-changing state when only the derived result needs to trigger UI updates. For example, during list scrolling, `firstVisibleItemIndex` changes hundreds of times per second. If a composable reads this directly, it recomposes on every pixel of scroll — very expensive. `derivedStateOf` wraps the calculation and only propagates a recomposition signal when the RESULT of the calculation changes (like when the boolean "has the user scrolled past item 0?" flips from false to true), which might happen only twice in the entire scroll session.

**Q: Why must `derivedStateOf` always be wrapped in `remember`?**
> `derivedStateOf` creates a `DerivedSnapshotState` object — a special observable container that tracks which state objects it reads and caches its computed result. Without `remember`, Compose creates a brand-new `DerivedSnapshotState` on EVERY recomposition. This means the old state tracking is discarded, a new tracking registration starts from scratch, the cached result is lost, and you're re-evaluating on every recomposition. The entire optimization is destroyed. `remember` ensures the SAME `DerivedSnapshotState` object persists across recompositions so it can accumulate its change-comparison history.

**Q: When should you NOT use `derivedStateOf`?**
> When the computed result changes at the same rate as the source state. For example, computing `val fullName = "$firstName $lastName"` doesn't benefit from `derivedStateOf` because fullName changes every time either name changes — the result is never "filtered." Using `derivedStateOf` in this case adds overhead (object allocation, equality checking) with zero benefit. The rule is: use `derivedStateOf` only when the source changes more frequently than the result needs to trigger recomposition.

**Q: Can `derivedStateOf` read from multiple state sources?**
> Yes. The calculation lambda can read any number of `State` objects, and `DerivedSnapshotState` will track reads of all of them. If any of them changes, the lambda is re-evaluated. The key optimization is still in the equality check on the result: even if one source changes 100 times, if the derived boolean result stays `true` throughout, no recomposition is triggered. This makes it ideal for form validation where multiple text fields feed a single "is form valid?" boolean.
