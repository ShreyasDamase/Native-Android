# 🎯 Compose State — Complete Guide

> [!NOTE]
> **Source of truth for Compose State primitives.** For ViewModel state (StateFlow), see [[StateFlow]]. For one-time events (SharedFlow), see [[SharedFlow]]. For cold Flow theory, see [[Cold Flow]].

---

## 🧠 Mental Model — Read This First

**Compose is a camera. Your state is the scene. Recomposition is taking a new photo.**

```
UI = f(State)
```

When State changes → Compose re-runs your composable functions → UI updates.

```kotlin
// ❌ Compose has NO idea this changed
var count = 0
count++         // nothing happens in UI

// ✅ Compose IS notified — snapshot system tracks this read
val count = mutableStateOf(0)
count.value++   // recomposition scheduled for composables that read count
```

**Why does this work?** Compose uses the **Snapshot System** — a reactive tracking mechanism. When a composable runs, every `State<T>` object it reads is registered as a dependency. When that state's value changes, only the composables that read it are recomposed.

---

## 📌 Table of Contents
1. [mutableStateOf — The Foundation](#1-mutablestateof)
2. [remember — Surviving Recomposition](#2-remember)
3. [rememberSaveable — Surviving Rotation](#3-remembersaveable)
4. [Primitive States — Performance Optimization](#4-primitive-states)
5. [Collection States — Lists and Maps](#5-collection-states)
6. [derivedStateOf — Computed State](#6-derivedstateof)
7. [State Hoisting — Lifting State Up](#7-state-hoisting)
8. [Unidirectional Data Flow (UDF)](#8-udf)
9. [Collecting StateFlow in Compose](#9-collecting-stateflow)
10. [State vs StateFlow — When to Use Which](#10-state-vs-stateflow)
11. [The Snapshot System — Under the Hood](#11-snapshot-system)
12. [Common Mistakes](#12-mistakes)
13. [Interview Q&A](#13-interview)

---

## 1. mutableStateOf — The Foundation

### What It Is

`mutableStateOf` creates a `MutableState<T>` — an observable value container integrated with Compose's Snapshot System. When its value changes, any composable reading it is scheduled for recomposition.

### Three Ways to Write It

```kotlin
// Way 1: Explicit — access via .value (verbose)
val count: MutableState<Int> = mutableStateOf(0)
Text(text = count.value.toString())
count.value++

// Way 2: Destructuring
val (count, setCount) = mutableStateOf(0)
Text(text = count.toString())
setCount(count + 1)

// Way 3: by delegation — RECOMMENDED (cleanest)
var count by mutableStateOf(0)
Text(text = count.toString())  // no .value needed
count++                         // no .value needed
```

> [!IMPORTANT]
> The `by` keyword works because `MutableState<T>` implements `getValue()` and `setValue()` operators. This requires these imports:
> ```kotlin
> import androidx.compose.runtime.getValue
> import androidx.compose.runtime.setValue
> ```
> **Forgetting these imports is the #1 beginner mistake.**

### The Critical Problem: Without `remember`

```kotlin
@Composable
fun BrokenCounter() {
    // ❌ BROKEN — this line runs on EVERY recomposition
    // Every recomposition creates a brand new MutableState(0)
    // Clicking: count=1 → recomposition → count reset to 0 → infinite loop!
    var count by mutableStateOf(0)

    Button(onClick = { count++ }) { Text("Count: $count") }
}
```

Fix: wrap in `remember`.

### The Mutation Policy

```kotlin
fun <T> mutableStateOf(
    value: T,
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy()
): MutableState<T>
```

| Policy | Equality Check | When to Use |
|---|---|---|
| `structuralEqualityPolicy()` (default) | `==` | Data classes, most types |
| `referentialEqualityPolicy()` | `===` | Force recomposition on every new object |
| `neverEqualPolicy()` | Always different | Force recomposition on every set |

```kotlin
// Default: same value → no recomposition (performance-friendly)
var name by remember { mutableStateOf("Alice") }
name = "Alice"  // no recomposition (structurally equal)

// Force: always recompose even if values look identical
var user by remember {
    mutableStateOf(User("Alice"), policy = neverEqualPolicy())
}
```

---

## 2. remember — Surviving Recomposition

### What It Is

`remember` stores a value in Compose's **Slot Table** — a flat, position-keyed array. During recomposition, the same position retrieves the stored value instead of recalculating.

```kotlin
@Composable
fun WorkingCounter() {
    // ✅ CORRECT — remember stores the MutableState in the Slot Table
    // On recomposition, the SAME MutableState is returned — not a new one
    var count by remember { mutableStateOf(0) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Count: $count", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { count-- }) { Text("−") }
            Button(onClick = { count++ }) { Text("+") }
            Button(onClick = { count = 0 }) { Text("Reset") }
        }
    }
}
```

### remember with Keys — Conditional Recalculation

```kotlin
@Composable
fun UserAvatar(userId: String) {
    // Recalculates ONLY when userId changes
    val avatarBitmap by remember(userId) {
        mutableStateOf(loadAvatarFromCache(userId))
    }
    Image(bitmap = avatarBitmap, contentDescription = "Avatar")
}

// Multiple keys: recalculate when ANY key changes
val result by remember(filter, sortOrder, page) {
    mutableStateOf(data.filter { ... }.sortedBy { ... })
}
```

### What remember Does NOT Survive

| Event | `remember` survives? |
|---|---|
| Recomposition | ✅ Yes |
| Screen rotation | ❌ No — Activity recreated |
| Dark mode toggle | ❌ No |
| Process death | ❌ No |
| User navigates away | ❌ No (composable leaves composition) |

> [!CAUTION]
> **Never put `remember` inside an `if` block or a loop.** The Slot Table is positional — if you conditionally skip a `remember` call, all subsequent slots shift, and Compose reads wrong values from wrong slots. This is the exact same rule as React's Hooks.
> ```kotlin
> // ❌ ILLEGAL
> if (showExtra) {
>     var extra by remember { mutableStateOf("") }  // sometimes slot 0, sometimes skipped
> }
> var name by remember { mutableStateOf("") }  // position shifts depending on condition!
>
> // ✅ CORRECT — always call remember unconditionally
> var extra by remember { mutableStateOf("") }  // always slot 0
> var name by remember { mutableStateOf("") }   // always slot 1
> if (showExtra) {
>     TextField(value = extra, onValueChange = { extra = it })
> }
> ```

### When to Use `remember`

✅ Toggle state (dialog open/closed, expanded/collapsed)  
✅ Locally managed UI decorations  
✅ State that doesn't matter if lost on rotation  
✅ Derived UI calculations you want to cache  

❌ User-typed text (use `rememberSaveable`)  
❌ Business data (use ViewModel)  
❌ State shared between composables (hoist it up)  

---

## 3. rememberSaveable — Surviving Rotation

### What It Is

`rememberSaveable` = `remember` + Android Bundle persistence. It saves the value to `Bundle` before the Activity is destroyed, and restores it when recreated.

```kotlin
@Composable
fun SearchBar() {
    // Survives rotation — user's search text is preserved
    var query by rememberSaveable { mutableStateOf("") }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("Search") }
    )
}
```

### What rememberSaveable Survives

| Event | `remember` | `rememberSaveable` |
|---|---|---|
| Recomposition | ✅ | ✅ |
| Screen rotation | ❌ | ✅ |
| Dark mode toggle | ❌ | ✅ |
| Process death (OS kills app) | ❌ | ✅ |
| User swipes app away from recents | ❌ | ❌ |

### Supported Types (No Custom Saver Needed)

`Int`, `Long`, `Float`, `Double`, `Boolean`, `String`, `Parcelable`, `Serializable`

### Custom Data Class — @Parcelize (Easiest)

```kotlin
// 1. Add plugin to build.gradle.kts
plugins { id("kotlin-parcelize") }

// 2. Mark data class
@Parcelize
data class SearchFilters(
    val minPrice: Int = 0,
    val category: String = "All"
) : Parcelable

// 3. Use normally — rememberSaveable handles it
var filters by rememberSaveable { mutableStateOf(SearchFilters()) }
```

### Custom Saver — For Third-Party Classes

```kotlin
val LatLngSaver = mapSaver(
    save = { mapOf("lat" to it.lat, "lng" to it.lng) },
    restore = { LatLng(it["lat"] as Double, it["lng"] as Double) }
)

var position by rememberSaveable(stateSaver = LatLngSaver) {
    mutableStateOf(LatLng(28.6, 77.2))
}
```

> [!WARNING]
> **Do not store large data in `rememberSaveable`.** The Android Bundle has a ~500KB-1MB transaction limit. Store only small UI state (text field content, selected tab, filter selection). Large data belongs in ViewModel.

### KMP Note

`rememberSaveable` is **Android-only** (relies on Android Bundle). For Compose Multiplatform, use `rememberSerializable` from `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.9.0+`.

---

## 4. Primitive States — Performance Optimization

Generic `mutableStateOf<T>` boxes primitives (`Int` → `Integer` object). Use primitive-specific APIs to avoid this:

```kotlin
// ❌ Boxing overhead: Int → Integer on every state change
var count by remember { mutableStateOf(0) }

// ✅ No boxing: stored as primitive int directly
var count by remember { mutableIntStateOf(0) }
```

| Function | Stores | Use for |
|---|---|---|
| `mutableIntStateOf(0)` | `int` (primitive) | Counters, indices, scores |
| `mutableLongStateOf(0L)` | `long` (primitive) | Timestamps, file sizes, large IDs |
| `mutableFloatStateOf(0f)` | `float` (primitive) | Progress (0–1), slider values, animation |
| `mutableDoubleStateOf(0.0)` | `double` (primitive) | Financial, scientific calculations |

```kotlin
@Composable
fun GameScreen() {
    var score by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    var timestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // ...
}
```

---

## 5. Collection States — Lists and Maps

> [!WARNING]
> `mutableStateOf(mutableListOf<T>())` is a trap. `MutableState` only triggers recomposition when the **reference changes** (new object assigned). Adding/removing from a `MutableList` in-place does NOT change the reference → **no recomposition, no UI update!**

```kotlin
// ❌ BROKEN — mutations are invisible
var items by remember { mutableStateOf(mutableListOf<String>()) }
items.add("hello")   // mutates in-place → same reference → no recomposition → UI stuck!

// ✅ OPTION A: immutable list + reassignment
var items by remember { mutableStateOf(listOf<String>()) }
items = items + "hello"   // creates new list → new reference → recomposition ✅

// ✅ OPTION B: Compose observable collection (BEST for lists that change)
val items = remember { mutableStateListOf<String>() }
items.add("hello")   // deeply observed → every mutation triggers recomposition ✅
```

| Type | Observable? | Recomposes on |
|---|---|---|
| `mutableListOf<T>()` in `mutableStateOf` | ❌ | Only on reference change |
| `listOf<T>()` in `mutableStateOf` | ✅ | On reassignment |
| `mutableStateListOf<T>()` | ✅ | Every `add()`, `remove()`, `set()` |
| `mutableStateMapOf<K, V>()` | ✅ | Every map mutation |

```kotlin
@Composable
fun TodoList() {
    val todos = remember { mutableStateListOf<String>() }
    var newTodo by remember { mutableStateOf("") }

    Column {
        LazyColumn {
            items(todos, key = { it }) { todo ->
                Row(modifier = Modifier.clickable { todos.remove(todo) }) {
                    Text(todo)
                }
            }
        }
        Row {
            OutlinedTextField(value = newTodo, onValueChange = { newTodo = it })
            IconButton(onClick = {
                if (newTodo.isNotBlank()) { todos.add(newTodo); newTodo = "" }
            }) { Icon(Icons.Default.Add, "Add") }
        }
    }
}
```

---

## 6. derivedStateOf — Computed State

### What It Is

`derivedStateOf` creates a state that only recomposes dependents when the **computed result changes**, not every time the source state changes.

```
Without derivedStateOf:
scroll position changes 500 times per second
→ 500 recompositions for each pixel of scroll

With derivedStateOf:
scroll position changes 500 times per second
→ evaluation happens 500 times (fast)
→ recomposition only when boolean FLIPS (maybe 2 times)
```

**Mental model:** `derivedStateOf` is like a smoke detector, not a microphone. A microphone records every tiny sound (every pixel of scroll). A smoke detector only triggers an alarm when the air changes from "normal" to "smoky" (when the computed result actually changes).

### Basic Scroll-to-Top FAB Example

#### ❌ BAD: Recomposes on every single pixel scroll
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

#### ✅ GOOD: Recomposes only when the boolean flips
```kotlin
@Composable
fun ProductListScreen() {
    val listState = rememberLazyListState()

    // GOOD — only recomposes when the boolean CHANGES (at most twice: false→true, true→false)
    val showFab by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    Box {
        LazyColumn(state = listState) { /* items */ }
        AnimatedVisibility(visible = showFab, modifier = Modifier.align(Alignment.BottomEnd)) {
            FloatingActionButton(onClick = { /* scroll to top */ }) {
                Icon(Icons.Default.KeyboardArrowUp, "Scroll to top")
            }
        }
    }
}
```

### Advanced Examples

#### 1. Multi-Source Form Validation
Both text fields change on every keystroke. The submit button only cares about the final boolean result.

```kotlin
@Composable
fun PasswordForm() {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    // Both fields change on every keystroke → isFormValid only changes when validity flips
    val isFormValid by remember {
        derivedStateOf {
            password.length >= 8 &&
            password.any { it.isDigit() } &&
            password.any { it.isUpperCase() } &&
            password == confirm
        }
    }

    Column {
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") })
        OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("Confirm Password") })
        Button(onClick = { /* submit */ }, enabled = isFormValid) { Text("Submit") }
    }
}
```

#### 2. Search Results Count Badge
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
            else allProducts.filter { it.name.contains(query, ignoreCase = true) }
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
        OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { Text("Search products...") })
        Text(resultCountLabel)   // only recomposes when count changes, not every character
        LazyColumn {
            items(filteredProducts, key = { it.id }) { product -> ProductRow(product) }
        }
    }
}
```

#### 3. Multi-Section Scroll Header
```kotlin
@Composable
fun ArticleScreen() {
    val listState = rememberLazyListState()

    // Complex derivation: changes at most 3 times during the entire scroll session
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

    Column {
        SectionHeader(title = currentSection) // Only recomposes when section boundary crossed
        LazyColumn(state = listState) {
            items(50) { index -> ArticleParagraph(index) }
        }
    }
}
```

#### 4. Cart Total (Observable List Sum)
```kotlin
@Composable
fun CartScreen() {
    val cartItems = remember { mutableStateListOf<CartItem>() }

    // cartItems changes on every add/remove/quantity change
    // But total only recomposes when the numerical total actually changes
    val cartTotal by remember {
        derivedStateOf { cartItems.sumOf { it.price * it.quantity } }
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
        Row(modifier = Modifier.padding(16.dp)) {
            Text("$itemCount items", modifier = Modifier.weight(1f))
            Text("$${"%.2f".format(cartTotal)}", fontWeight = FontWeight.Bold)
        }
    }
}
```

### When NOT to Use derivedStateOf

`derivedStateOf` adds overhead: it creates an additional `DerivedSnapshotState` object, registers read callbacks, and runs an equality check on every source state change. If the result changes at the same rate as the source, you get the overhead with no benefit.

* **Case 1: The result changes as often as the source**
  ```kotlin
  // ❌ WRONG — fullName changes every time firstName OR lastName changes
  val fullName by remember { derivedStateOf { "$firstName $lastName" } }

  // ✅ CORRECT — just compute it. The composable already recomposes when these change.
  val fullName = "$firstName $lastName"
  ```

* **Case 2: The source doesn't change frequently**
  ```kotlin
  // ❌ OVERKILL — network status changes very rarely
  val isConnected by remember { derivedStateOf { networkState.isAvailable } }

  // ✅ CORRECT
  val isConnected = networkState.isAvailable
  ```

* **Case 3: The computation itself is trivial**
  ```kotlin
  // ❌ WRONG — overhead of derivedStateOf exceeds the cost of simple if-else
  val buttonText by remember { derivedStateOf { if (isLoading) "Loading..." else "Submit" } }

  // ✅ CORRECT
  val buttonText = if (isLoading) "Loading..." else "Submit"
  ```

**Golden Rule:** Use `derivedStateOf` when the source state changes **more frequently** than you need to recompose, and the computed result changes **less frequently** than the source.

> [!WARNING]
> Always wrap `derivedStateOf` in `remember`. Without it, a brand-new `DerivedSnapshotState` object is created on every recomposition — destroying the entire optimization.

---

### derivedStateOf vs Plain Computation

| Scenario | Use | Reason |
|---|---|---|
| Scroll index → show FAB button | `derivedStateOf` | Index changes hundreds of times/sec; boolean flips maybe twice |
| Form validity from multiple text fields | `derivedStateOf` | Text changes every keystroke; validity flips only occasionally |
| First name + last name → full name string | Plain `val` | Both change at the same rate as the result |
| Product price × quantity → subtotal | Plain `val` | Price/quantity updates require immediate subtotal recalculation |
| Filter large list on every keystroke | `derivedStateOf` | Filter runs every character, but result list changes less often |

---

## 7. State Hoisting — Lifting State Up

### What Is Hoisting?

State hoisting = moving state from a child composable to its parent. Replace:
- `var state by remember { mutableStateOf(...) }` inside child
- With two parameters: `value: T` (current state) and `onValueChange: (T) -> Unit` (event callback)

**Mental model:** Moving the TV remote from inside the TV to the coffee table. Anyone can use it, you can see which channel is on, and the TV itself stays simple.

### Before Hoisting (Stateful, not reusable)

```kotlin
@Composable
fun StatefulUsernameInput() {
    var username by remember { mutableStateOf("") }
    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") })
    // Problem: any parent that wants the username value can't get it
}
```

### After Hoisting (Stateless, reusable)

```kotlin
// Child: stateless — just displays and reports events
@Composable
fun UsernameInput(
    username: String,
    onUsernameChange: (String) -> Unit,
    modifier: Modifier = Modifier   // always add Modifier as last param with default
) {
    OutlinedTextField(value = username, onValueChange = onUsernameChange, label = { Text("Username") }, modifier = modifier)
}

// Parent: owns the state
@Composable
fun SignupScreen() {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Column {
        Text("Hello, $username")
        UsernameInput(username = username, onUsernameChange = { username = it })
        // SAME stateless composable reused for email!
        UsernameInput(username = email, onUsernameChange = { email = it })
    }
}
```

### Level 2: Hoisting to ViewModel

For state representing real business data, hoist all the way up:

```kotlin
class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun onUsernameChange(newUsername: String) {
        if (newUsername.length <= 30) {  // business rules live here
            _uiState.update { it.copy(username = newUsername) }
        }
    }
}

// Screen: bridges ViewModel to UI (knows about ViewModel — hard to test)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileContent(
        uiState = uiState,
        onUsernameChange = viewModel::onUsernameChange
    )
}

// Content: pure function — easily testable with fake data
@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    onUsernameChange: (String) -> Unit
) {
    // ... UI code
}
```

**Why split Screen from Content?**
- `ProfileScreen` knows about ViewModel → Android-specific, needs real ViewModel to test
- `ProfileContent` is a pure Kotlin function → pass any fake `ProfileUiState` to test it

### Level 3: Plain State Holder Class

For complex UI-only state (multi-step form, drag state, animation state):

```kotlin
@Stable
class MultiStepFormState(initialStep: Int = 0) {
    var currentStep by mutableIntStateOf(initialStep)
        private set

    val isFirstStep get() = currentStep == 0
    val isLastStep  get() = currentStep == totalSteps - 1

    fun goToNextStep()     { if (!isLastStep)  currentStep++ }
    fun goToPreviousStep() { if (!isFirstStep) currentStep-- }
}

@Composable
fun rememberMultiStepFormState(initialStep: Int = 0): MultiStepFormState {
    return remember { MultiStepFormState(initialStep) }
}
```

### The Three Levels

| Level | Where State Lives | Use For |
|---|---|---|
| Composable | `var x by remember {...}` | Pure UI mechanics: is dropdown open, animation progress |
| State Holder Class | Plain class with `mutableStateOf` | Complex UI state with multiple related fields |
| ViewModel | `MutableStateFlow` | All business data: loaded content, user data, settings |

---

## 8. Unidirectional Data Flow (UDF)

**State flows DOWN. Events flow UP.**

```
           ViewModel
    ┌─────────────────────────┐
    │  StateFlow<UiState>     │ ← state (flows DOWN)
    │  SharedFlow<UiEvent>    │
    └───────────┬─────────────┘
                │
          collectAsStateWithLifecycle()
                │
    ┌───────────▼─────────────┐
    │   Screen Composable     │
    │  val uiState = ...      │
    └───────────┬─────────────┘
                │ passes state down
    ┌───────────▼─────────────┐
    │   Stateless Children    │
    │  value = uiState.xxx    │
    │  onEvent = { vm.fn() }  │
    └───────────┬─────────────┘
                │ events (flow UP as lambdas)
                ▼
          viewModel.onEvent()
```

**The UDF guarantee:** Every state change has ONE path, ONE source of truth. The UI is always `UI = f(State)`.

### State vs Events — The Critical Distinction

**State** = "What is currently true" → `StateFlow`, survives rotation, re-delivered:
```kotlin
data class ProfileUiState(
    val username: String,      // what's in the field RIGHT NOW
    val isLoading: Boolean,    // is the screen currently loading?
    val users: List<User>      // the list currently displayed
)
```

**Events** = "Something happened once" → `SharedFlow`, one-time, NOT re-delivered:
```kotlin
sealed class ProfileUiEvent {
    data class ShowSnackbar(val message: String) : ProfileUiEvent()
    object NavigateToHome : ProfileUiEvent()
}
```

```kotlin
// Full ViewModel pattern (State + Events)
class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileUiEvent>()
    val events: SharedFlow<ProfileUiEvent> = _events.asSharedFlow()

    fun onSaveClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                repo.save()
                _events.emit(ProfileUiEvent.ShowSnackbar("Saved!"))  // one-time
                _events.emit(ProfileUiEvent.NavigateToHome)           // one-time
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
```

---

## 9. Collecting StateFlow in Compose

### collectAsStateWithLifecycle() — The Standard

```kotlin
@Composable
fun UserProfileScreen(viewModel: UserProfileViewModel) {
    // Converts StateFlow<T> → State<T> → T (via 'by' delegation)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState) {
        is UiState.Loading -> CircularProgressIndicator()
        is UiState.Success -> Text("User: ${(uiState as UiState.Success).name}")
        is UiState.Error   -> Text("Error loading profile")
    }
}
```

### What collectAsStateWithLifecycle Does

```
App State:   | Background | Foreground | Background | Foreground
Lifecycle:      STOPPED      STARTED      STOPPED      STARTED
                  │              │              │              │
collectAsWith     │  (pause)     │   collecting  │  (pause)    │   collecting
Lifecycle:        x─────────────►               x─────────────►
```

Pauses at `STOPPED` → resumes at `STARTED`. This prevents battery drain when app is in background.

### Collecting Regular Flow (not StateFlow)

```kotlin
@Composable
fun LocationScreen(locationFlow: Flow<Location>) {
    // Standard Flow doesn't hold a value — must provide initialValue
    val location by locationFlow.collectAsStateWithLifecycle(
        initialValue = Location.Unknown
    )
    Text("Current Location: $location")
}
```

### Collecting Events (SharedFlow)

> [!WARNING]
> Do NOT use `collectAsStateWithLifecycle()` for events! Use `LaunchedEffect` + `repeatOnLifecycle`.

```kotlin
@Composable
fun LoginScreen(viewModel: LoginViewModel, onNavigate: (String) -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // ✅ Correct way to collect one-time events
    LaunchedEffect(viewModel.events, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is UiEvent.NavigateToHome -> onNavigate("home")
                    is UiEvent.ShowError      -> snackbarHostState.showSnackbar(event.msg)
                }
            }
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ... UI
}
```

### snapshotFlow — Converting Compose State to Flow

```kotlin
@Composable
fun SearchScreen(viewModel: SearchViewModel) {
    var query by remember { mutableStateOf("") }

    // snapshotFlow: the opposite of collectAsState
    // Converts Compose State → cold Flow → enables Flow operators
    LaunchedEffect(Unit) {
        snapshotFlow { query }
            .debounce(500)
            .filter { it.length >= 3 }
            .distinctUntilChanged()
            .collectLatest { validQuery ->
                viewModel.search(validQuery)
            }
    }

    OutlinedTextField(value = query, onValueChange = { query = it })
}
```

### Quick Reference

| You have | You want | Use |
|---|---|---|
| `StateFlow<T>` in ViewModel | `T` in Compose | `collectAsStateWithLifecycle()` |
| `SharedFlow<Event>` | Run an action | `LaunchedEffect` + `repeatOnLifecycle` |
| `Flow<T>` from Room | `T` in Compose | `collectAsStateWithLifecycle(initial)` |
| Compose `State<T>` | `Flow<T>` for operators | `snapshotFlow { state }` |
| A suspend function on first load | Run once | `LaunchedEffect(Unit) { ... }` |

---

## 10. State vs StateFlow — When to Use Which

| Feature | `MutableState` (Compose) | `MutableStateFlow` (Coroutines) |
|---|---|---|
| Where to use | Inside `@Composable` functions | Inside ViewModel or Repository |
| Coupling | Tightly coupled to Compose | Pure Kotlin, decoupled |
| Testing | Requires Compose test rules | Simple unit tests (Turbine) |
| Thread Safety | Main thread | Thread-safe any dispatcher |
| Observation | Automatic (Snapshot System) | Requires `.collect()` |
| Flow operators | ❌ Can't use | ✅ Full operator support |

**Google's recommendation: StateFlow in ViewModels. Compose State in composables.**

```kotlin
// ✅ WHY StateFlow wins in ViewModel:
val searchResults = searchQuery
    .debounce(300)
    .flatMapLatest { query -> repository.search(query) }
    .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())
// You CANNOT do this with Compose mutableStateOf!
```

---

## 11. The Snapshot System — Under the Hood

### How Read Tracking Works

```
State<String> (name = "Alice")
    ↑ READ during composition of NameText()
    ↑ READ during composition of ProfileCard()
    NOT READ in LoadingSpinner()

name.value = "Bob"
    → Only NameText() and ProfileCard() recompose ✅
    → LoadingSpinner() is NOT recomposed ✅
```

### The Slot Table (How remember Works)

```kotlin
@Composable
fun MyScreen() {
    var count by remember { mutableStateOf(0) }  // Slot 0: stores MutableState<Int>
    var name  by remember { mutableStateOf("") }  // Slot 1: stores MutableState<String>
    var open  by remember { mutableStateOf(true) } // Slot 2: stores MutableState<Boolean>
}
```

During recomposition: each `remember` call returns the previously stored value from its slot. The lambda is NOT re-executed. This is positional memoization — the position in the slot table determines which value is returned.

### How derivedStateOf Connects

```kotlin
val showFab by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 0 }
}
```

`DerivedSnapshotState`:
1. Registers itself as a reader of `listState.firstVisibleItemIndex`
2. Every time `firstVisibleItemIndex` changes → re-evaluates the lambda
3. Compares old result with new result using `==`
4. Only propagates recomposition if the result actually changed

---

## 12. Common Mistakes

### Mistake 1: Missing `remember`

```kotlin
// ❌ BROKEN — resets to 0 on every recomposition
@Composable
fun BrokenCounter() {
    var count by mutableStateOf(0)   // no remember!
    Button(onClick = { count++ }) { Text("Count: $count") }
}

// ✅ CORRECT
@Composable
fun WorkingCounter() {
    var count by remember { mutableStateOf(0) }
    Button(onClick = { count++ }) { Text("Count: $count") }
}
```

### Mistake 2: mutableListOf inside mutableStateOf

```kotlin
// ❌ BROKEN — add/remove never recomposes
var items by remember { mutableStateOf(mutableListOf<String>()) }
items.add("hello")  // in-place mutation → same reference → no recomposition

// ✅ Option A: immutable list
var items by remember { mutableStateOf(listOf<String>()) }
items = items + "hello"  // new list → recomposition ✅

// ✅ Option B: observable collection
val items = remember { mutableStateListOf<String>() }
items.add("hello")  // deeply observed → recomposition ✅
```

### Mistake 3: Large data in rememberSaveable

```kotlin
// ❌ CRASH — Bundle limit ~500KB
var allProducts by rememberSaveable { mutableStateOf(listOf<Product>()) }
// Hundreds of products → TransactionTooLargeException

// ✅ CORRECT — only store UI state
var searchQuery by rememberSaveable { mutableStateOf("") }
// Product list lives in ViewModel — survives rotation automatically
```

### Mistake 4: derivedStateOf without remember

```kotlin
// ❌ BROKEN — new DerivedSnapshotState on every recomposition → defeats optimization
val showFab by derivedStateOf { listState.firstVisibleItemIndex > 0 }

// ✅ CORRECT
val showFab by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
```

### Mistake 5: Over-hoisting (Prop Drilling)

```kotlin
// ❌ BAD — passing through many composables that don't use it
@Composable
fun AppRoot() {
    var searchQuery by remember { mutableStateOf("") }
    MainScreen(searchQuery = searchQuery, onQueryChange = { searchQuery = it })
}
@Composable
fun MainScreen(searchQuery: String, onQueryChange: (String) -> Unit) {
    // MainScreen doesn't USE searchQuery — just passes it down
    ContentArea(searchQuery = searchQuery, onQueryChange = onQueryChange)
}
// Solutions: CompositionLocal, ViewModel, or keep state closer to where it's used
```

### Mistake 6: Calling ViewModel functions during composition

```kotlin
// ❌ WRONG — fires on EVERY recomposition
@Composable
fun ProductScreen(productId: String, viewModel: ProductViewModel = viewModel()) {
    viewModel.loadProduct(productId)  // called during composition!
    val state by viewModel.uiState.collectAsStateWithLifecycle()
}

// ✅ CORRECT — use LaunchedEffect for side effects
@Composable
fun ProductScreen(productId: String, viewModel: ProductViewModel = viewModel()) {
    LaunchedEffect(productId) {  // runs once when productId changes
        viewModel.loadProduct(productId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
}
```

---

## 13. Interview Q&A

**Q: What is `mutableStateOf` and why is it needed?**
> `mutableStateOf` creates an observable state container integrated with Compose's Snapshot System. During composition, every `State<T>` object that is READ is registered as a dependency of that composable. When the state's value changes, Compose marks only those composables for recomposition and skips everything else. Without `mutableStateOf`, Compose has no way to know a value changed, so the UI would never update.

**Q: What is the difference between `remember` and `rememberSaveable`?**
> `remember` stores its value in the Compose Slot Table — an in-memory structure. It survives recompositions but is lost when the Activity is recreated (rotation, dark mode toggle, process death). `rememberSaveable` additionally serializes its value into the Android Bundle (`onSaveInstanceState`), so it survives configuration changes and process death. The cost: only Bundle-compatible types work without a custom `Saver`.

**Q: Why can't you put `remember` inside an `if` block?**
> Compose uses a positional Slot Table — each `remember` call is associated with its position in the composable's execution order. If `remember` is inside a conditional, it sometimes runs and sometimes doesn't, shifting the positions of all subsequent `remember` calls. Compose then reads the wrong value from the wrong slot. Same fundamental reason as React's Hooks Rule.

**Q: What is the difference between `mutableStateOf(mutableListOf())` and `mutableStateListOf()`?**
> `mutableStateOf` only triggers recomposition when the reference it holds changes (new object assigned). Calling `.add()` on a `MutableList` mutates in-place, keeping the same reference → Compose never sees a change → UI never updates. `mutableStateListOf()` is deeply observable — every `add()`, `remove()`, `set()` operation is individually tracked and triggers recomposition.

**Q: What problem does `derivedStateOf` solve?**
> It prevents excessive recomposition when a composable depends on a computed result that changes less often than the source state. Example: scroll position changes hundreds of times per second during scrolling, but the FAB's visibility (a boolean) only changes twice (first item scrolls past 0, then back). `derivedStateOf` evaluates the calculation on every source change but only propagates recomposition when the result actually changes.

**Q: What is state hoisting and why is it important?**
> State hoisting = moving state from a child composable to its parent by replacing internal `mutableStateOf` variables with two parameters: the current value and a callback for change requests. This creates a single source of truth (state in exactly one place), makes composables stateless and reusable, enables proper Unidirectional Data Flow (state flows down, events flow up), and makes composables testable without real state infrastructure.

**Q: Should you use `MutableState` or `MutableStateFlow` in a ViewModel?**
> `MutableStateFlow`. ViewModels should not depend on Compose UI runtime. `StateFlow` is pure Kotlin — highly testable without Compose dependencies, combinable with operators like `debounce` and `flatMapLatest`, and thread-safe for background updates. Compose `State` in a ViewModel also prevents Kotlin Multiplatform usage.

**Q: What is the Snapshot System?**
> Compose's reactive state tracking mechanism. During composition, Compose opens a read-tracking snapshot. Every `State<T>` object read during this time registers itself with the snapshot. When a State value later changes, the snapshot system identifies which composables registered a read and schedules only those for recomposition. This is what makes Compose granular and efficient — recomposition scope is as small as the lambda that actually reads the state.

---

## 📦 Gradle Dependencies

```kotlin
// Compose State — included in Compose Runtime (no extra dependency)
implementation("androidx.compose.runtime:runtime:1.7.8")

// rememberSaveable — included in Compose Runtime Saveable
implementation("androidx.compose.runtime:runtime-saveable:1.7.8")

// collectAsStateWithLifecycle
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
```

---

## 🔗 Connections

- **ViewModel state**: [[StateFlow]] — how ViewModel's StateFlow connects to Compose State
- **One-time events**: [[SharedFlow]] — SharedFlow for navigation, toasts, snackbars
- **Full Flow theory**: [[Cold Flow]] — cold Flow, operators, stateIn
- **Architecture**: [[Comparison — State vs Flow vs StateFlow vs LiveData vs SharedFlow]] — detailed decision guide and comparison
