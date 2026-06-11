# Compose State Primitives

## 📌 Purpose
To manage local state within a Composable, Jetpack Compose provides several primitives. These functions allow Composables to "remember" data across recompositions and trigger UI updates when that data changes.

> [!NOTE]
> Jetpack Compose UI is a function of its state. If a Composable does not use a state primitive, it will forget its variable values every time it recomposes.

## 🔧 Core Functions

### `mutableStateOf`
```kotlin
fun <T> mutableStateOf(
    value: T,
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy()
): MutableState<T>
```
Creates an observable `MutableState<T>`. When the value is modified, Compose schedules a recomposition of any Composable reading it.

### Primitive specific versions (Performance Optimization)
```kotlin
fun mutableIntStateOf(value: Int): MutableIntState
fun mutableFloatStateOf(value: Float): MutableFloatState
fun mutableDoubleStateOf(value: Double): MutableDoubleState
fun mutableLongStateOf(value: Long): MutableLongState
```
> [!TIP]
> Always use primitive specific states (e.g., `mutableIntStateOf`) instead of `mutableStateOf(0)` to avoid autoboxing overhead.

### `remember`
```kotlin
@Composable
inline fun <T> remember(crossinline calculation: () -> T): T
```
Caches the result of `calculation`. During recomposition, it returns the cached value instead of executing `calculation` again. 

### `rememberSaveable`
```kotlin
@Composable
fun <T : Any> rememberSaveable(
    vararg inputs: Any?,
    saver: Saver<T, out Any> = autoSaver(),
    key: String? = null,
    init: () -> T
): T
```
Like `remember`, but it survives Activity or Process recreation (e.g., configuration changes like screen rotation) by saving the value in the Android `Bundle`.

## ✅ Basic Example

```kotlin
@Composable
fun NameInput() {
    // 1. mutableStateOf creates the state.
    // 2. remember caches it across recompositions.
    // 3. 'by' delegation allows us to use 'name' as a normal String.
    var name by remember { mutableStateOf("") }

    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Enter your name") }
    )
}
```

## 🚀 Advanced Examples

### Using `rememberSaveable` for configuration changes
If you rotate the device, standard `remember` will lose its value because the Activity is destroyed and recreated. `rememberSaveable` fixes this.

```kotlin
@Composable
fun RobustCounter() {
    // This value will survive screen rotations and dark mode toggles!
    var count by rememberSaveable { mutableIntStateOf(0) }

    Button(onClick = { count++ }) {
        Text("Count: $count")
    }
}
```

### Custom `Saver` for `rememberSaveable`
`rememberSaveable` out-of-the-box only supports types that can be saved in an Android `Bundle` (Strings, primitives, Parcelables). For custom data classes, you need a custom Saver.

```kotlin
data class User(val id: Int, val name: String)

val UserSaver = Saver<User, Map<String, Any>>(
    save = { mapOf("id" to it.id, "name" to it.name) },
    restore = { User(id = it["id"] as Int, name = it["name"] as String) }
)

@Composable
fun UserProfile() {
    var user by rememberSaveable(stateSaver = UserSaver) {
        mutableStateOf(User(1, "Alice"))
    }
    
    // UI here...
}
```

## ⚠️ Common Gotchas

1. **Forgetting `remember`**: `var state = mutableStateOf("")` (without remember) will recreate the state object on every single recomposition, losing any user input.
2. **Mutating Collections**: If you do `val list by remember { mutableStateOf(mutableListOf<String>()) }` and call `list.add("item")`, Compose **WILL NOT** recompose. `MutableState` only tracks when the instance itself is reassigned. Instead, use `remember { mutableStateListOf<String>() }`.
3. **Using `rememberSaveable` for large data**: `rememberSaveable` writes to the Android Bundle, which has a strict size limit (usually ~500kb). Do not store large lists or bitmaps here; use a ViewModel for that.

## 💡 Interview Q&A

**Q: What is the difference between `remember` and `rememberSaveable`?**
A: `remember` caches a value across recompositions, but its state is lost if the Composable leaves the composition or if the Activity is recreated (e.g., screen rotation). `rememberSaveable` saves the value into the Android `Bundle`, allowing it to survive configuration changes and process death.

**Q: Why should I use `mutableIntStateOf(0)` instead of `mutableStateOf(0)`?**
A: `mutableStateOf<T>` works with objects. Passing an `Int` to it causes autoboxing (converting primitive `int` to object `Integer`), which incurs a memory and performance penalty. `mutableIntStateOf` is optimized to work directly with the primitive `int`.

**Q: How does `remember` work under the hood?**
A: During the initial composition, `remember` executes its lambda and stores the result in the composition's slot table. During recomposition, it skips the lambda and simply retrieves the previously stored value from the slot table.


---

## 🚀 Mastery Deep Dive (Added 2026)

> [!NOTE]
> The following deep dive notes were generated to provide mastery-level understanding, complementing the original notes above.

# Compose State Primitives — The Complete Guide

> [!NOTE]
> This is the foundation of everything in Jetpack Compose. If you don't understand state, you can't understand recomposition, and if you can't understand recomposition, you can't write performant Compose UIs. Read the "Under the Hood" section — it will permanently change how you reason about Compose.

---

## 🧠 Mental Model — Read This First

**Compose is a camera. Your state is the scene. Recomposition is taking a new photo.**

A regular variable in a composable is like writing on water — the moment the composable "recomposes" (re-runs), the variable is freshly created, forgetting its old value.

`mutableStateOf` is the **motion sensor** attached to the camera. Without it, the camera never knows when the scene changed and never takes a new photo. With it, every change to the scene triggers a new photo automatically.

`remember` is the **memory card** in the camera. Without it, every photo is taken from scratch (the camera forgets its settings each frame). With it, the camera remembers its settings between shots.

Together: `val x by remember { mutableStateOf(initial) }` = "Create a scene sensor once, remember it across shots, and let it trigger new photos when it changes."

---

## ⚙️ Under the Hood — The Snapshot System

> [!IMPORTANT]
> Understanding this section means understanding WHY Compose is reactive and HOW it avoids unnecessary recompositions. This is the engine room.

### What is a Snapshot?

Compose uses a **Snapshot System** — think of it like a git commit system for your UI state.

When Compose runs a composable function (composition), it opens a **read-tracking snapshot**. During this snapshot, every `State<T>` object that is READ is automatically registered as a "dependency" of that composable.

When a `State<T>` value changes, Compose checks which snapshots (composables) read it and marks ONLY those composables for recomposition. Everything else is skipped.

```
State<String> (name = "Alice")
    ↑ READ during composition of NameText()
    ↑ READ during composition of ProfileCard()
    NOT READ in LoadingSpinner()

name.value = "Bob"
    → Only NameText() and ProfileCard() recompose ✅
    → LoadingSpinner() is NOT recomposed ✅ (never read name)
```

This is why Compose is efficient: **recomposition scope is as small as the lambda that actually reads the state.**

### The Slot Table — What `remember` Writes To

The Compose runtime maintains a data structure called the **Slot Table** — imagine a numbered array of storage slots, one per "call site" in your composable.

During first composition:
- Compose executes your composable top to bottom
- Each `remember { }` call fills a slot with the computed value
- The slot is keyed by **position** (call site order in the source code), not by variable name

During recomposition:
- Compose executes your composable again
- Each `remember { }` call **returns the already-stored value** from its slot
- The lambda inside `remember { }` is NEVER executed again (unless the key changes)

```kotlin
@Composable
fun MyScreen() {
    // Slot 0: stores the MutableState<Int> object
    var count by remember { mutableStateOf(0) }

    // Slot 1: stores the MutableState<String> object
    var name by remember { mutableStateOf("") }

    // Slot 2: stores the MutableState<Boolean> object
    var isVisible by remember { mutableStateOf(true) }
}
```

> [!CAUTION]
> **This is why you can NEVER put `remember` inside an `if` block, a loop, or any conditional.** The Slot Table is positional — if you conditionally skip a `remember` call, every subsequent slot shifts by one, and Compose reads the wrong value from the wrong slot. This is the exact same reason React's Hooks have the same rule.
>
> ```kotlin
> // ❌ ILLEGAL — if condition changes, slot positions shift
> @Composable
> fun BadExample(showExtra: Boolean) {
>     if (showExtra) {
>         var extra by remember { mutableStateOf("") }  // sometimes slot 0, sometimes skipped
>     }
>     var name by remember { mutableStateOf("") }  // slot 0 or 1 depending on showExtra — WRONG
> }
>
> // ✅ CORRECT — always call remember, use the condition inside
> @Composable
> fun GoodExample(showExtra: Boolean) {
>     var extra by remember { mutableStateOf("") }  // always slot 0
>     var name by remember { mutableStateOf("") }   // always slot 1
>     if (showExtra) {
>         TextField(value = extra, onValueChange = { extra = it })
>     }
> }
> ```

---

## 🔬 Syntax Anatomy — Every Character Justified

```kotlin
var name by remember { mutableStateOf("") }
```

Let's break this down **character by character**:

| Part | What it is | Why it's there |
|---|---|---|
| `var` | Kotlin `var` (mutable variable) | Needed because `by` delegation uses `setValue` — you'll reassign `name = "new"` |
| `name` | Your variable name | The local name you use to read and write the state |
| `by` | Kotlin property delegation operator | Calls `getValue()` on read and `setValue()` on write — removing the need for `.value` everywhere |
| `remember` | Compose API | Stores the result in the Slot Table; returns cached value on recomposition |
| `{ }` | Kotlin lambda | The "calculation" block — only runs ONCE (during first composition) |
| `mutableStateOf("")` | Compose API | Creates a `MutableState<String>` — an observable holder. When `.value` changes, Compose schedules recomposition |
| `""` | The initial value | What the state starts with before any user interaction |

### What `by` actually does (the delegation deep dive)

Without `by`:
```kotlin
val nameState: MutableState<String> = remember { mutableStateOf("") }

// Reading: must use .value
Text(nameState.value)

// Writing: must use .value
nameState.value = "Alice"
```

With `by`:
```kotlin
var name: String by remember { mutableStateOf("") }
// Kotlin generates:
//   get() = nameState.getValue(thisRef, property)  → returns nameState.value
//   set(v) = nameState.setValue(thisRef, property, v)  → sets nameState.value = v

// Reading: just use the variable
Text(name)

// Writing: looks like normal assignment
name = "Alice"
```

`MutableState<T>` implements two operator functions:
```kotlin
// These are defined on State<T> and MutableState<T>:
operator fun <T> State<T>.getValue(thisObj: Any?, property: KProperty<*>): T = value
operator fun <T> MutableState<T>.setValue(thisObj: Any?, property: KProperty<*>, value: T) {
    this.value = value
}
```

That's all `by` does — it calls these two operator functions. No magic. Just syntax sugar.

---

## 🏗️ Core Functions

### `mutableStateOf<T>` — The Observable Container

```kotlin
fun <T> mutableStateOf(
    value: T,
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy()
): MutableState<T>
```

| Parameter | Type | Default | Role |
|---|---|---|---|
| `value` | `T` | — | The initial value stored |
| `policy` | `SnapshotMutationPolicy<T>` | `structuralEqualityPolicy()` | Controls WHEN a change is considered "different enough" to trigger recomposition |

**The `policy` parameter explained:**
- `structuralEqualityPolicy()` (default): uses `==` (structural equality). If `newValue == oldValue`, recomposition is SKIPPED.
- `referentialEqualityPolicy()`: uses `===` (reference equality). Every new object triggers recomposition, even if structurally identical.
- `neverEqualPolicy()`: always triggers recomposition, regardless of value.

```kotlin
// Default policy — smart, avoids unnecessary recomposition
var name by remember { mutableStateOf("Alice") }
name = "Alice"  // SAME value → NO recomposition ✅

// Custom policy for a data class where you always want recomposition
var user by remember {
    mutableStateOf(User("Alice"), policy = neverEqualPolicy())
}
user = user.copy()  // would normally be equal, but neverEqualPolicy forces recomposition
```

---

### Primitive-Specific States — Performance Optimization

The generic `mutableStateOf<T>` uses Kotlin's type system, which **boxes** (wraps) primitives like `Int`, `Float`, `Long`, `Double` into their object equivalents (`Integer`, `Float`, `Long`, `Double`). Autoboxing costs memory allocation and GC pressure.

```kotlin
// ❌ Autoboxing: Int → Integer (object allocation on every state change)
var count by remember { mutableStateOf(0) }

// ✅ No boxing: stored as primitive int directly
var count by remember { mutableIntStateOf(0) }
```

| Function | Stores | Use for |
|---|---|---|
| `mutableIntStateOf(0)` | `int` (primitive) | Counters, indices, scores, pixel sizes |
| `mutableLongStateOf(0L)` | `long` (primitive) | Timestamps, file sizes, large IDs |
| `mutableFloatStateOf(0f)` | `float` (primitive) | Progress (0f–1f), slider values, animation values |
| `mutableDoubleStateOf(0.0)` | `double` (primitive) | Financial calculations, scientific values |

---

### Collection States — The Right Way to Handle Lists and Maps

> [!WARNING]
> `mutableStateOf(mutableListOf<String>())` is a trap. `MutableState` only triggers recomposition when the state **reference changes** (a new object is assigned). Adding/removing items from a `MutableList` stored in a `MutableState` does NOT change the reference → **NO recomposition!**

```kotlin
// ❌ BROKEN — adding items never recomposes
var items by remember { mutableStateOf(mutableListOf<String>()) }
Button(onClick = { items.add("new item") }) { Text("Add") }
// items.add() mutates the list in-place — same reference → Compose never sees a change

// ✅ CORRECT — create a new list on each change
var items by remember { mutableStateOf(listOf<String>()) }
Button(onClick = { items = items + "new item" }) { Text("Add") }
// items + "new item" creates a NEW list → new reference → Compose sees the change ✅

// ✅ ALSO CORRECT — use Compose's observable collection
val items = remember { mutableStateListOf<String>() }
Button(onClick = { items.add("new item") }) { Text("Add") }
// mutableStateListOf is DEEPLY observable — every mutation triggers recomposition ✅
```

| Type | Observable? | Usage |
|---|---|---|
| `mutableListOf<T>()` in `mutableStateOf` | ❌ Only on reference change | Don't use — causes bugs |
| `listOf<T>()` in `mutableStateOf` (immutable) | ✅ On reassignment | Good — always create a new list |
| `mutableStateListOf<T>()` | ✅ On every mutation | Best — no reassignment needed |
| `mutableStateMapOf<K, V>()` | ✅ On every mutation | Best for maps |

---

### `remember` — The Slot Table Writer

```kotlin
@Composable
inline fun <T> remember(crossinline calculation: () -> T): T
```

| Parameter | Role |
|---|---|
| `calculation: () -> T` | The block that creates the initial value. Runs ONLY on first composition. Returns cached value on all recompositions. |

**`remember` with keys — for conditional recalculation:**

```kotlin
@Composable
fun UserProfile(userId: String) {
    // If userId changes, 'remember' throws away the old value and runs the lambda again
    val userData by remember(userId) {
        mutableStateOf(loadUserData(userId))
    }
    //              ^^^^^^
    //              key: when this changes, recalculate
}
```

How keys work internally: Compose stores both the keys and the value in the Slot Table. On each recomposition, it compares the current keys to the stored keys. If different → runs the lambda again and stores new value. If same → returns cached value.

```kotlin
// Multiple keys: recalculate if ANY key changes
val result by remember(filter, sortOrder, page) {
    mutableStateOf(
        data.filter { it.matches(filter) }
            .sortedBy { sortOrder.comparator }
            .drop(page * 20)
            .take(20)
    )
}
```

**When to use keys:**
- When the remembered value depends on parameters that can change
- When you need to reset remembered state based on a parent-provided ID
- When you want to invalidate expensive computations

---

### `rememberSaveable` — Surviving Process Death

```kotlin
@Composable
fun <T : Any> rememberSaveable(
    vararg inputs: Any?,
    saver: Saver<T, out Any> = autoSaver(),
    key: String? = null,
    init: () -> T
): T
```

`rememberSaveable` = `remember` + Android Bundle persistence.

**What survives vs what doesn't:**

| Scenario | `remember` | `rememberSaveable` |
|---|---|---|
| Recomposition | ✅ Survives | ✅ Survives |
| Screen rotation | ❌ Lost | ✅ Survives |
| Dark mode toggle | ❌ Lost | ✅ Survives |
| App goes to background | ❌ Lost | ✅ Survives |
| Process death (app killed by OS) | ❌ Lost | ✅ Survives |
| User navigates away (back stack) | ❌ Lost (composable leaves tree) | ❌ Lost (composable leaves tree) |

**What types auto-save (via `autoSaver()`):**
- All primitives: `Int`, `Long`, `Float`, `Double`, `Boolean`, `String`
- `Parcelable` objects
- `Serializable` objects (discouraged — prefer Parcelable)
- `Bundle` itself

**Custom `Saver` for complex types:**

```kotlin
data class SearchState(
    val query: String = "",
    val filterActive: Boolean = false,
    val selectedCategory: String = "All"
)

// Saver<T, Saveable> — T is the type to save, Saveable is the bundle-compatible representation
val SearchStateSaver = Saver<SearchState, Bundle>(
    save = { state ->
        Bundle().apply {
            putString("query", state.query)
            putBoolean("filterActive", state.filterActive)
            putString("category", state.selectedCategory)
        }
    },
    restore = { bundle ->
        SearchState(
            query = bundle.getString("query", ""),
            filterActive = bundle.getBoolean("filterActive", false),
            selectedCategory = bundle.getString("category", "All")
        )
    }
)

@Composable
fun SearchScreen() {
    var searchState by rememberSaveable(stateSaver = SearchStateSaver) {
        mutableStateOf(SearchState())
    }
    // searchState survives rotation, process death — fully restored
}
```

> [!WARNING]
> **`rememberSaveable` is not for large data.** The Android Bundle has a transaction size limit of ~1MB total. Do NOT store large lists, bitmaps, or complex object graphs in `rememberSaveable`. Use ViewModel for that. `rememberSaveable` is for small UI state like text field content, scroll position indicators, and filter selections.

---

## 🎯 Recomposition Scope — What Actually Recomposes

The key insight that makes Compose performant: **recomposition is scoped to the smallest possible lambda that reads state.**

```kotlin
@Composable
fun Screen() {
    var count by remember { mutableIntStateOf(0) }

    Column {
        // This entire Text composable recomposes when count changes
        Text("Count: $count")   // reads 'count' → subscribed

        // This does NOT recompose when count changes (never reads count)
        Text("Hello World")     // does not read 'count' → NOT subscribed

        Button(onClick = { count++ }) {
            Text("Increment")
        }
    }
}
```

```kotlin
// Even better: inline lambdas create tighter recomposition scopes
@Composable
fun OptimizedScreen() {
    var count by remember { mutableIntStateOf(0) }

    Column {
        // Only this lambda recomposes — Column, Button, static Text are skipped
        Text(
            text = "Count: $count"   // Compose tracks this read at the Text composable level
        )
        Text("Static text")          // never recomposes when count changes
        Button(onClick = { count++ }) { Text("Increment") }
    }
}
```

---

## 📊 State API Comparison Table

| API | Use for | Observable? | Survives Recomposition | Survives Config Change |
|---|---|---|---|---|
| `mutableStateOf(x)` | Objects, Strings, data classes | ✅ reference change | ✅ (with `remember`) | ❌ |
| `mutableIntStateOf(n)` | Int values | ✅ | ✅ (with `remember`) | ❌ |
| `mutableLongStateOf(n)` | Long values | ✅ | ✅ (with `remember`) | ❌ |
| `mutableFloatStateOf(f)` | Float values | ✅ | ✅ (with `remember`) | ❌ |
| `mutableDoubleStateOf(d)` | Double values | ✅ | ✅ (with `remember`) | ❌ |
| `mutableStateListOf<T>()` | Observable lists | ✅ every mutation | ✅ (with `remember`) | ❌ |
| `mutableStateMapOf<K,V>()` | Observable maps | ✅ every mutation | ✅ (with `remember`) | ❌ |
| `rememberSaveable { }` | Any Bundle-compatible state | ✅ | ✅ | ✅ |

---

## ✅ Correct Usage Patterns

### Pattern 1: Simple local UI state

```kotlin
@Composable
fun ExpandableCard(title: String, content: String) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = { isExpanded = !isExpanded },
        modifier = Modifier.animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (isExpanded) {
                Spacer(Modifier.height(8.dp))
                Text(content)
            }
        }
    }
}
```

### Pattern 2: Text field with validation

```kotlin
@Composable
fun EmailInput() {
    var email by remember { mutableStateOf("") }
    val isError = email.isNotEmpty() && !email.contains("@")

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        isError = isError,
        supportingText = {
            if (isError) Text("Invalid email address", color = MaterialTheme.colorScheme.error)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )
}
```

### Pattern 3: Observable list with `mutableStateListOf`

```kotlin
@Composable
fun TodoList() {
    // mutableStateListOf: any add/remove triggers recomposition automatically
    val todos = remember { mutableStateListOf<String>() }
    var newTodo by remember { mutableStateOf("") }

    Column {
        LazyColumn {
            items(todos, key = { it }) { todo ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { todos.remove(todo) }
                        .padding(16.dp)
                ) {
                    Text(todo)
                }
            }
        }

        Row {
            OutlinedTextField(
                value = newTodo,
                onValueChange = { newTodo = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add todo...") }
            )
            IconButton(
                onClick = {
                    if (newTodo.isNotBlank()) {
                        todos.add(newTodo)
                        newTodo = ""
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    }
}
```

### Pattern 4: `rememberSaveable` for text that survives rotation

```kotlin
@Composable
fun SearchBar() {
    // This query text survives screen rotation, dark mode toggle, and process death
    var query by rememberSaveable { mutableStateOf("") }

    SearchTextField(
        query = query,
        onQueryChange = { query = it },
        onClear = { query = "" }
    )
}
```

---

## ❌ Common Mistakes & Why They Break

### Mistake 1: Missing `remember`

```kotlin
// ❌ BROKEN — mutableStateOf without remember
@Composable
fun BrokenCounter() {
    var count by mutableStateOf(0)  // ← no remember!
    // On EVERY recomposition, this line runs again:
    //   → creates a BRAND NEW MutableState(0)
    //   → count resets to 0 every single recomposition
    //   → click button → count = 1 → recomposition triggered → count reset to 0 → infinite reset loop!

    Button(onClick = { count++ }) { Text("Count: $count") }
}

// ✅ CORRECT
@Composable
fun WorkingCounter() {
    var count by remember { mutableStateOf(0) }   // created once, cached forever
    Button(onClick = { count++ }) { Text("Count: $count") }
}
```

### Mistake 2: Using `mutableStateOf(mutableListOf())` instead of `mutableStateListOf`

```kotlin
// ❌ BROKEN — mutations are invisible to Compose
var items by remember { mutableStateOf(mutableListOf<String>()) }
items.add("hello")   // list is mutated in-place → same reference → no recomposition → UI never updates

// ✅ OPTION A: immutable list + reassignment
var items by remember { mutableStateOf(listOf<String>()) }
items = items + "hello"   // new list created → new reference → recomposition triggered ✅

// ✅ OPTION B: Compose observable collection
val items = remember { mutableStateListOf<String>() }
items.add("hello")   // deeply observed → recomposition triggered automatically ✅
```

### Mistake 3: Storing large data in `rememberSaveable`

```kotlin
// ❌ BAD — storing hundreds of items crashes with TransactionTooLargeException
var allProducts by rememberSaveable { mutableStateOf(listOf<Product>()) }
// Products loaded from network (could be hundreds or thousands) → Bundle limit exceeded → crash

// ✅ CORRECT — ViewModel manages the data, rememberSaveable only stores UI state
var searchQuery by rememberSaveable { mutableStateOf("") }   // ← just the query string
// The product list lives in ViewModel → survives rotation automatically
```

### Mistake 4: Modifying state outside of composition or event handlers

```kotlin
// ❌ BAD — modifying state from a background thread directly
viewModel.data.observe(this) { data ->
    Thread {
        uiState.value = data   // writing State from a non-Main thread → race condition or crash
    }.start()
}

// ✅ CORRECT — Compose State must be written on the Main thread
// If using StateFlow in ViewModel + collectAsStateWithLifecycle(), this is handled for you
```

---

## 🔗 Connections

- **Next**: [[Remember Mutable State Of]] — deep dive into `remember` keys, `rememberSaveable` Saver, and `@Stable`/`@Immutable`
- **Derived state**: [[Derived State Of Computed State]] — computing state from other state efficiently
- **Hoisting**: [[State Hoisting Patterns]] — when and how to move state out of composables
- **ViewModel state**: [[State vs StateFlow]] — how ViewModel's `StateFlow` connects to Compose State
- **Snapshot System details**: This is the same system that makes `derivedStateOf` work

---

## 💬 Interview Master Q&A

**Q: What is `mutableStateOf` and why is it needed?**
> `mutableStateOf` creates an observable state container. Compose's recomposition engine — powered by the Snapshot System — tracks which composables READ a `State<T>` object during composition. When that state's value changes, Compose marks only the dependent composables for recomposition and skips everything else. Without `mutableStateOf`, Compose has no way to know that a value changed, so the UI would never update even if the variable's value changes.

**Q: What is the difference between `remember` and `rememberSaveable`?**
> `remember` stores its value in the Compose Slot Table — an in-memory array associated with the current composition. It survives recompositions (re-executions of the composable function) but is lost when the composable leaves the composition tree OR when the Activity/Fragment is recreated (screen rotation, dark mode toggle, process death). `rememberSaveable` additionally serializes its value into the Android `Bundle` (the same mechanism as `onSaveInstanceState`), so it survives configuration changes and even process death. The cost is that it only works with Bundle-compatible types; custom types need a `Saver`.

**Q: Why can't you put `remember` inside an `if` block?**
> Compose uses a positional Slot Table to store `remember` values. Each `remember` call is associated with its position in the execution order of the composable function. If a `remember` is inside a conditional, sometimes it runs and sometimes it doesn't, shifting the positions of all subsequent `remember` calls. Compose would then read the wrong value from the wrong slot — like shuffling cards in a deck and then trying to find card #3. This is the same fundamental reason React's Hooks cannot be called conditionally.

**Q: What is the difference between `mutableStateOf(mutableListOf())` and `mutableStateListOf()`?**
> `mutableStateOf` only triggers recomposition when the reference it holds changes — i.e., when you assign a completely new object. Calling `.add()` on a `MutableList` mutates the list in-place, keeping the same reference, so Compose never sees a change and never recomposes. `mutableStateListOf()` creates a Compose-observable list that is deeply tracked — every `add()`, `remove()`, and `set()` operation is individually observed and triggers recomposition. For mutable collections in Compose, always use `mutableStateListOf()` or `mutableStateMapOf()`.

**Q: What is the Snapshot System?**
> The Snapshot System is Compose's reactive state tracking mechanism. It works like a transaction log for state reads. During composition, Compose opens a read-tracking snapshot — any `State<T>` object that is read during this time registers itself with the snapshot. When a `State` value later changes, the snapshot system identifies which composables registered a read and schedules only those for recomposition. This is what makes Compose granular and efficient: you don't recompose an entire screen, only the composables that actually depend on the changed state.
