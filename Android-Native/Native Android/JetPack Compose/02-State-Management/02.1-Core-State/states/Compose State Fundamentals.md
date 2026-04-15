# 📱 Android State Management — Complete Deep-Dive Notes

> **For:** Jetpack Compose + Kotlin | Updated: 2025–2026 **Purpose:** Full theory + motivation + practical examples for every state tool in modern Android development

---

## 📖 Before We Begin — Why Is State Management Even a Problem?

### The Problem with Traditional Android (XML Views)

In the old View system (XML + `Activity`/`Fragment`), the developer was responsible for **manually synchronising** the UI with the underlying data. If a variable changed, you had to remember to call:

```kotlin
textView.text = newValue
button.isEnabled = someCondition
progressBar.visibility = View.GONE
```

This approach has serious problems:

- **Fragile**: Forget one `setText()` call and the UI goes out of sync
- **Hard to test**: The UI is tightly coupled with logic
- **Error-prone on rotation**: `Activity` was destroyed and recreated — all your in-memory data was gone
- **Hard to reason about**: Any piece of code anywhere in the app could change the UI at any time

### How Jetpack Compose Changes Everything

Compose flips the model. Instead of _you_ updating the UI, you declare **what the UI should look like for a given state**, and Compose automatically re-renders the affected parts when state changes.

```
Old model:  Data changes → You manually update each View
New model:  Data changes → Compose automatically re-renders
```

This is called a **declarative UI** paradigm, borrowed from React and Flutter. The entire UI is a pure function of state:

```
UI = f(State)
```

When `State` changes, Compose calls your composable functions again (this is called **recomposition**) and produces a new UI.

### Why Are There So Many State Tools?

Because state has different **lifetimes**, **scopes**, and **purposes**:

|Question|Determines which tool|
|---|---|
|Does this state need to survive screen rotation?|`rememberSaveable` vs `remember`|
|Does this state belong to the UI or to business logic?|`remember` vs `ViewModel`|
|Should it survive the app being killed by the OS?|`SavedStateHandle`|
|Is it a continuous stream or a one-time event?|`StateFlow` vs `SharedFlow`|
|Is it computed from other state?|`derivedStateOf`|
|Is it a collection that changes at runtime?|`mutableStateListOf`|

Understanding _why_ each tool exists is the key to choosing the right one. This document covers exactly that — the history, motivation, internal mechanics, and full practical examples for every tool.

---

## 📑 Index — All Chapters

|#|Chapter|What It Covers|
|---|---|---|
|01|[Compose State Fundamentals](https://claude.ai/chat/e0120311-b1bb-47d0-af10-765d6c6a5412#chapter-01-compose-state-fundamentals)|`mutableStateOf`, `remember`, `rememberSaveable`, `derivedStateOf`|
|02|[Snapshot State Collections](https://claude.ai/chat/e0120311-b1bb-47d0-af10-765d6c6a5412#chapter-02-snapshot-state-collections)|`mutableStateListOf`, `mutableStateMapOf`, `toMutableStateList`|
|03|[Flow-Based State](https://claude.ai/chat/e0120311-b1bb-47d0-af10-765d6c6a5412#chapter-03-flow-based-state)|`Flow`, `StateFlow`, `MutableStateFlow`, `SharedFlow`, operators|
|04|[ViewModel + State](https://claude.ai/chat/e0120311-b1bb-47d0-af10-765d6c6a5412#chapter-04-viewmodel--state)|ViewModel lifecycle, exposing state, coroutine scope|
|05|[SavedStateHandle](https://claude.ai/chat/e0120311-b1bb-47d0-af10-765d6c6a5412#chapter-05-savedstatehandle)|Process death, `getStateFlow()`, `saveable()` delegate|
|06|[Connecting State to UI](https://claude.ai/chat/e0120311-b1bb-47d0-af10-765d6c6a5412#chapter-06-connecting-state-to-ui)|`collectAsState`, `collectAsStateWithLifecycle`, `observeAsState`|
|07|[State Hoisting & UDF](https://claude.ai/chat/e0120311-b1bb-47d0-af10-765d6c6a5412#chapter-07-state-hoisting--unidirectional-data-flow)|State hoisting, Unidirectional Data Flow|
|08|[UI State Architecture](https://claude.ai/chat/e0120311-b1bb-47d0-af10-765d6c6a5412#chapter-08-ui-state-architecture)|Sealed class / data class UiState, SSOT|
|09|[State Stability & Performance](https://claude.ai/chat/e0120311-b1bb-47d0-af10-765d6c6a5412#chapter-09-state-stability--performance)|`@Stable`, `@Immutable`, recomposition, ImmutableList|
|10|[LiveData (Legacy)](https://claude.ai/chat/e0120311-b1bb-47d0-af10-765d6c6a5412#chapter-10-livedata--legacy)|When you still see it, how to migrate to Flow|
|11|[KMP — State Across Platforms](https://claude.ai/chat/e0120311-b1bb-47d0-af10-765d6c6a5412#chapter-11-kmp--state-across-platforms)|commonMain tools, iOS bridge, SKIE|
|12|[Gradle Dependencies Master List](https://claude.ai/chat/e0120311-b1bb-47d0-af10-765d6c6a5412#chapter-12-gradle-dependencies-master-list)|Every library with version|
|13|[Common Mistakes & Errors](https://claude.ai/chat/e0120311-b1bb-47d0-af10-765d6c6a5412#chapter-13-common-mistakes--errors)|Real mistakes and fixes|
|14|[Decision Guide](https://claude.ai/chat/e0120311-b1bb-47d0-af10-765d6c6a5412#chapter-14-decision-guide--what-to-use-when)|Quick reference table + mental model|

---

## Chapter 01: Compose State Fundamentals

### 🔷 What Is "State" in Compose? — The Full Picture

#### The Recomposition Model

In Jetpack Compose, your UI composable functions are regular Kotlin functions. Compose calls them during the initial composition and again whenever state they read from changes. This re-calling is called **recomposition**.

```
Initial launch   → Compose calls all composables → UI appears
State changes    → Compose re-calls only affected composables → UI updates
```

Compose is smart about this — it uses a technique called **positional memoization** (backed by a data structure called the **Slot Table**) to track which composables read which state, and only re-runs the ones that are actually affected.

```kotlin
// This composable reads `count` — it recomposes when count changes
@Composable
fun CountDisplay(count: Int) {
    Text(text = "Count: $count")
}

// This composable doesn't read count — it never recomposes when count changes
@Composable
fun StaticLabel() {
    Text(text = "Hello")
}
```

#### The Snapshot System — How Compose Tracks State

Under the hood, Compose uses a system called the **Snapshot System** (from `androidx.compose.runtime.snapshots`). This is inspired by MVCC (Multiversion Concurrency Control) used in databases.

Every `mutableStateOf` is tracked in a global snapshot. When a composable **reads** a state value during composition, the snapshot system records that relationship. When the state **changes**, the snapshot system knows exactly which composables need to recompose.

This is why you cannot use regular Kotlin variables — Compose simply doesn't know they exist or that they changed.

```kotlin
// Compose has NO idea this variable changed — it's invisible to the snapshot system
var count = 0
count++  // Nothing happens in UI

// Compose KNOWS this changed — it's inside the snapshot system
val count = mutableStateOf(0)
count.value++  // Triggers recomposition of any composable that read count
```

---

### 🔷 1.1 `mutableStateOf`

#### Why Does It Exist?

Before `mutableStateOf`, there was no standard way for a Compose composable to hold and observe a piece of data. The entire Compose state system needed a foundation — a primitive observable value container. `mutableStateOf` is that foundation.

It's the Compose equivalent of what a `ViewModel` with `LiveData` was in the old world, but scoped to a single composable function.

#### What It Is

`mutableStateOf` creates a **`MutableState<T>`** object — a holder for a single value that is integrated with the Compose snapshot system. When its value changes, any composable that is currently reading it will be scheduled for recomposition.

#### Which Library

Part of **Compose Runtime** — included in Compose itself, no extra dependency.

```kotlin
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue  // Required for `by` delegate
import androidx.compose.runtime.setValue  // Required for `by` delegate
```

#### Three Ways to Write It

```kotlin
// ─────────────────────────────────────────────
// Way 1: Explicit — always access via .value
// ─────────────────────────────────────────────
	val count: MutableState<Int> = mutableStateOf(0)
Text(text = count.value.toString())
count.value++

// ─────────────────────────────────────────────
// Way 2: Destructuring — separates getter/setter
// ─────────────────────────────────────────────
val (count, setCount) = mutableStateOf(0)
Text(text = count.toString())
setCount(count + 1)  // Can only set, not use += etc.

// ─────────────────────────────────────────────
// Way 3: Property delegate with `by` — RECOMMENDED
// Cleanest syntax, works just like a regular variable
// ─────────────────────────────────────────────
var count by mutableStateOf(0)
Text(text = count.toString())  // No .value needed
count++                         // No .value needed
```

> **Important:** The `by` delegate works because `MutableState<T>` has `operator fun getValue()` and `operator fun setValue()` defined. These require the imports `getValue` and `setValue` from `androidx.compose.runtime`. **Forgetting these imports is one of the most common beginner mistakes.**

#### The Critical Problem: Mutable State Alone Does Not Survive Recomposition

This is confusing at first, so let's be very explicit:

```kotlin
@Composable
fun BrokenCounter() {
    // ❌ BROKEN: This line runs EVERY time BrokenCounter recomposes.
    // That means every recomposition resets count back to 0.
    // The counter can never go above 0!
    var count by mutableStateOf(0)

    Button(onClick = { count++ }) {
        Text("Count: $count")
    }
}
```

Why does this happen? When you click the button, `count` becomes `1`, which triggers a recomposition of `BrokenCounter`. When Compose calls `BrokenCounter` again, `var count by mutableStateOf(0)` executes again, creating a **brand new** `MutableState` initialized to `0`. The counter resets.

The fix is `remember {}`, covered next.

---

### 🔷 1.2 `remember`

#### Why Does It Exist?

When Compose recomposes a function, it re-executes the function body from scratch. Without some mechanism to preserve values across recompositions, every local variable would be reset. `remember` provides that mechanism — it stores a value in the composition's **Slot Table** so it persists across recompositions.

Think of `remember` as giving Compose a sticky note: _"Remember this value — don't recalculate it next time."_

#### What It Is Internally

`remember` is implemented using Compose's internal **Slot Table** — a flat array-like structure that stores data keyed by the position of the composable in the composition tree (positional memoization). During recomposition, when Compose revisits the same position in the tree, it retrieves the stored value instead of recalculating.

This is why `remember` is tied to the **lifecycle of the composable** — the slot is created when the composable enters the composition and removed when it leaves.

#### Which Library

**Compose Runtime** — no extra dependency.

```kotlin
import androidx.compose.runtime.remember
```

#### Basic Usage

```kotlin
@Composable
fun WorkingCounter() {
    // ✅ CORRECT: remember {} stores the MutableState in the Slot Table.
    // On recomposition, the SAME MutableState object is returned, not a new one.
    var count by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Count: $count",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { count-- }) { Text("−") }
            Button(onClick = { count++ }) { Text("+") }
            Button(onClick = { count = 0 }) { Text("Reset") }
        }
    }
}
```

#### `remember` with Computation

`remember` can store any computed value, not just state:

```kotlin
@Composable
fun UserGreeting(userId: String) {
    // This expensive computation runs once, result is remembered
    val formattedName by remember {
        val profile = database.getUserProfile(userId)   // expensive
        mutableStateOf(profile.displayName.uppercase())
    }

    Text("Hello, $formattedName!")
}
```

#### `remember` with Keys — Cache Invalidation

By default, `remember` stores a value forever (until the composable leaves). But what if the value depends on an input that can change? Pass a key:

```kotlin
@Composable
fun UserAvatar(userId: String) {
    // Re-runs the block ONLY when userId changes
    val avatarBitmap by remember(userId) {
        mutableStateOf(loadAvatarFromCache(userId))
    }

    Image(bitmap = avatarBitmap, contentDescription = "Avatar")
}
```

You can pass multiple keys:

```kotlin
val result by remember(key1, key2, key3) {
    mutableStateOf(computeSomething(key1, key2, key3))
}
```

#### Full Practical Example — Toggle Dialog

```kotlin
@Composable
fun DeleteConfirmationButton(onConfirmDelete: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Button(
        onClick = { showDialog = true },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
    ) {
        Text("Delete Account")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onConfirmDelete()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
```

#### Full Practical Example — Tab Selection

```kotlin
@Composable
	fun ProfileScreen() {
	    val tabs = listOf("Posts", "Followers", "Following")
	    var selectedTabIndex by remember { mutableStateOf(0) }
	
	    Column {
	        TabRow(selectedTabIndex = selectedTabIndex) {
	            tabs.forEachIndexed { index, title ->
	                Tab(
	                    selected = selectedTabIndex == index,
	                    onClick = { selectedTabIndex = index },
	                    text = { Text(title) }
	                )
	            }
	        }
	        Spacer(modifier = Modifier.height(16.dp))
	        when (selectedTabIndex) {
	            0 -> PostsGrid()
	            1 -> FollowersList()
	            2 -> FollowingList()
	        }
	    }
	}
```

#### What `remember` Does NOT Survive

|Event|Does `remember` survive?|
|---|---|
|Recomposition|✅ Yes — that's its purpose|
|Screen rotation|❌ No — Activity recreated, composition discarded|
|Language/font size change|❌ No — same as rotation|
|Process death|❌ No — app killed, nothing survives|
|User navigates back and returns|❌ No — composable left composition|

#### When to Use `remember`

✅ **Use when:**

- Toggle state: dialog open/closed, menu expanded/collapsed
- Locally managed UI decorations
- State that doesn't matter if lost (no user input to preserve)
- Derived UI calculations you want to cache

❌ **Don't use when:**

- User has typed something (use `rememberSaveable`)
- The state represents business data (use ViewModel)
- Multiple composables need the same state (hoist it up)
- Screen rotation must preserve the value

---

### 🔷 1.3 `rememberSaveable`

#### Why Does It Exist?

`remember` solves the recomposition problem but not the **configuration change** problem. When a user rotates their phone, Android destroys and recreates the `Activity`, which destroys the entire composition — and with it, all your `remember`'d values.

This was already a well-known pain point in the old View system (solved there by `onSaveInstanceState(Bundle)`). `rememberSaveable` brings that same mechanism into Compose — it automatically saves state to a `Bundle` when the composition is destroyed and restores it when it's recreated.

#### What It Is Internally

`rememberSaveable` hooks into Android's **saved instance state** mechanism. It:

1. Registers a `SaveableStateRegistry` in the composition
2. Before the composition is destroyed, saves the value to a Bundle (using Android's `onSaveInstanceState`)
3. When the composition is recreated, restores the value from the Bundle
4. Acts exactly like `remember` for normal recompositions (no serialization overhead during recomposition)

#### Which Library

**Compose Runtime Saveable** — part of the standard Compose setup.

```kotlin
import androidx.compose.runtime.saveable.rememberSaveable
```

#### Basic Usage

```kotlin
@Composable
fun SearchBar() {
    // Survives rotation — user's search text is preserved
    var query by rememberSaveable { mutableStateOf("") }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("Search") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        modifier = Modifier.fillMaxWidth()
    )
}
```

#### Supported Types (No Custom Saver Needed)

These types can be saved directly to a Bundle without any extra work:

|Type|Example|
|---|---|
|`Int`|`rememberSaveable { mutableStateOf(0) }`|
|`Long`|`rememberSaveable { mutableStateOf(0L) }`|
|`Float`|`rememberSaveable { mutableStateOf(0.0f) }`|
|`Double`|`rememberSaveable { mutableStateOf(0.0) }`|
|`Boolean`|`rememberSaveable { mutableStateOf(false) }`|
|`String`|`rememberSaveable { mutableStateOf("") }`|
|`Parcelable`|Works via `@Parcelize`|
|`Serializable`|Works but slower than Parcelable|

#### Custom Data Class — Using `@Parcelize`

When you want to save a custom data class, the easiest path is `@Parcelize`:

```kotlin
// 1. Add the plugin to your build.gradle.kts
plugins {
    id("kotlin-parcelize")
}

// 2. Mark your class as Parcelable
@Parcelize
data class SearchFilters(
    val minPrice: Int = 0,
    val maxPrice: Int = 10000,
    val category: String = "All",
    val sortBy: String = "Relevance"
) : Parcelable

// 3. Use it — rememberSaveable handles it automatically
@Composable
fun FilterPanel() {
    var filters by rememberSaveable {
        mutableStateOf(SearchFilters())
    }

    Column {
        Text("Min price: ${filters.minPrice}")
        Slider(
            value = filters.minPrice.toFloat(),
            onValueChange = { filters = filters.copy(minPrice = it.toInt()) },
            valueRange = 0f..10000f
        )
        // More filter controls...
    }
}
```

#### Custom Saver — When `@Parcelize` Isn't Possible

Sometimes you can't use `@Parcelize` — the class might come from a third-party library, or it's too complex. Use a custom `Saver`:

```kotlin
data class LatLng(val lat: Double, val lng: Double)  // Third-party class

// Define how to save/restore this type
val LatLngSaver = run {
    val latKey = "lat"
    val lngKey = "lng"
    mapSaver(
        save = { mapOf(latKey to it.lat, lngKey to it.lng) },
        restore = { LatLng(it[latKey] as Double, it[lngKey] as Double) }
    )
}

@Composable
fun MapScreen() {
    var cameraPosition by rememberSaveable(stateSaver = LatLngSaver) {
        mutableStateOf(LatLng(28.6139, 77.2090))  // New Delhi
    }

    // Map composable using cameraPosition...
}
```

For lists, use `listSaver`:

```kotlin
val StringListSaver = listSaver<List<String>, String>(
    save = { it },
    restore = { it }
)
```

#### `TextFieldValue` Saver — Common Real-World Case

When using `TextFieldValue` for rich text field control (cursor position, selection), use its built-in saver:

```kotlin
@Composable
fun RichSearchBar() {
    var searchValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    OutlinedTextField(
        value = searchValue,
        onValueChange = { searchValue = it },
        label = { Text("Search") }
    )
}
```

#### Full Practical Example — Multi-Field Form That Survives Rotation

```kotlin
@Composable
fun RegistrationForm(onSubmit: (String, String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var agreedToTerms by rememberSaveable { mutableStateOf(false) }

    val isFormValid = name.isNotBlank() && email.contains("@") && agreedToTerms

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Create Account", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone (optional)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = agreedToTerms,
                onCheckedChange = { agreedToTerms = it }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("I agree to the Terms and Conditions")
        }

        Button(
            onClick = { onSubmit(name, email, phone) },
            enabled = isFormValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Account")
        }
    }
}
// If the user rotates the device while filling this form,
// all typed values are preserved — no frustration!
```

#### What `rememberSaveable` Does NOT Survive

|Event|Survives?|
|---|---|
|Recomposition|✅ Yes|
|Screen rotation|✅ Yes|
|Config changes (language, font size)|✅ Yes|
|System-initiated process death (low memory)|✅ Yes|
|User swipes app away from recents|❌ No|
|App crash|❌ No|

#### KMP Note

`rememberSaveable` is **Android-only** because it relies on the Android `Bundle` system. For Compose Multiplatform (KMP), use `rememberSerializable` (available in `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.9.0+`).

---

### 🔷 1.4 `derivedStateOf`

#### Why Does It Exist?

Imagine a search screen. The user is typing in a text field. With each keystroke, `searchText` changes — potentially dozens of times per second. Somewhere on screen you have a "Search" button that should only be enabled when the text is 3+ characters long.

Without `derivedStateOf`:

```kotlin
// isValid recomputes AND triggers recomposition on EVERY keystroke
val isValid = searchText.length >= 3
```

The button recomposes every time the user types. Even when the result (`true` or `false`) doesn't change — like when typing the 4th, 5th, 6th characters (all valid, all `true`).

`derivedStateOf` solves this by creating a state that only notifies dependents when the **computed result changes**, not when the source state changes.

#### What It Is Internally

`derivedStateOf` creates a derived `State<T>` backed by the snapshot system. It:

1. Records which state values it reads when computing the result
2. Re-computes only when those source states change
3. Only emits a new value (and triggers recomposition) when the computed result is **different** from the previous one

It's like `distinctUntilChanged()` for computed state.

#### Which Library

**Compose Runtime** — no extra dependency.

```kotlin
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
```

#### Basic Usage

```kotlin
@Composable
fun SearchScreen() {
    var searchText by remember { mutableStateOf("") }

    // derivedStateOf: only recomposes button when isValid flips between true/false
    // NOT on every single keystroke
    val isValid by remember {
        derivedStateOf { searchText.length >= 3 }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { /* perform search */ },
            enabled = isValid
        ) {
            Text("Search")
        }
    }
}
```

#### Full Practical Example — Password Strength Indicator

```kotlin
@Composable
fun PasswordField() {
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // These derived states only update when their boolean result changes
    val hasMinLength by remember { derivedStateOf { password.length >= 8 } }
    val hasUpperCase by remember { derivedStateOf { password.any { it.isUpperCase() } } }
    val hasDigit by remember { derivedStateOf { password.any { it.isDigit() } } }
    val hasSpecialChar by remember { derivedStateOf { password.any { "!@#$%^&*".contains(it) } } }
    val isStrongEnough by remember {
        derivedStateOf { hasMinLength && hasUpperCase && hasDigit }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None
                                   else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility
                                      else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Password requirements checklist
        PasswordRequirement("At least 8 characters", hasMinLength)
        PasswordRequirement("Contains uppercase letter", hasUpperCase)
        PasswordRequirement("Contains a number", hasDigit)
        PasswordRequirement("Contains special character (!@#$...)", hasSpecialChar)

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(visible = isStrongEnough) {
            Text(
                text = "✓ Password is strong",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun PasswordRequirement(text: String, isMet: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isMet) Color.Green else Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isMet) MaterialTheme.colorScheme.onSurface else Color.Gray
        )
    }
}
```

#### Full Practical Example — Scroll-Based FAB Visibility

```kotlin
@Composable
fun ArticleScreen(articles: List<Article>) {
    val lazyListState = rememberLazyListState()

    // Expensive calculation avoided: only recomposes FAB when boolean flips
    val showScrollToTopFab by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 2 }
    }

    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = lazyListState) {
            items(articles) { article ->
                ArticleCard(article)
            }
        }

        AnimatedVisibility(
            visible = showScrollToTopFab,
            enter = scaleIn(),
            exit = scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(0)
                    }
                }
            ) {
                Icon(Icons.Default.KeyboardArrowUp, "Scroll to top")
            }
        }
    }
}
```

#### When to Use `derivedStateOf`

The key question to ask is: _"Does the source state change much more often than the computed result?"_

✅ **Use when:**

- Validation result computed from a text field (text changes many times, boolean flips rarely)
- Scroll position threshold flags (position changes constantly, flag flips rarely)
- Filtering or searching a list (query changes often, results change less dramatically)
- Computing whether a set of conditions is all satisfied

❌ **Don't use when:**

- The source state and computed result change at the same frequency (no optimization benefit)
- Simple reads like `name.length` — just read directly

```kotlin
// ❌ Unnecessary — the result changes AS OFTEN as name changes
val nameLength by remember { derivedStateOf { name.length } }
Text("Length: $nameLength")

// ✅ Just read it directly
Text("Length: ${name.length}")

// ✅ derivedStateOf makes sense here — boolean changes less often than text
val isTooLong by remember { derivedStateOf { name.length > 50 } }
```

---

## Chapter 02: Snapshot State Collections

### 🔷 Why Do We Need Special Collections?

#### The Problem with Regular Collections

Kotlin's standard `mutableListOf()`, `mutableMapOf()` etc. are **not observable**. When you add or remove an item from them, nothing in the outside world (including Compose) is notified. Compose has no way to know the collection changed.

```kotlin
// ❌ This does NOT trigger recomposition when items change
var items by remember { mutableStateOf(mutableListOf("A", "B")) }
items.add("C")  // Compose doesn't see this — UI stuck showing only A and B!
```

You might try the workaround of reassigning:

```kotlin
// This technically works, but it's awkward and error-prone
items = items.toMutableList().also { it.add("C") }
```

`mutableStateListOf` and friends solve this properly.

#### How Snapshot Collections Work

Snapshot collections are built on the same Compose Snapshot system as `mutableStateOf`. Every mutation (`add`, `remove`, `set`, etc.) is automatically recorded as a state change, causing Compose to recompose any composable that was reading the collection.

They behave exactly like regular Kotlin collections (they implement the same interfaces), but every mutation notifies Compose.

---

### 🔷 2.1 `mutableStateListOf`

#### Which Library

**Compose Runtime** — no extra dependency.

```kotlin
import androidx.compose.runtime.mutableStateListOf
```

#### Basic Usage

```kotlin
@Composable
fun SimpleTaskList() {
    val tasks = remember { mutableStateListOf("Buy groceries", "Pay bills") }

    Column {
        tasks.forEach { task ->
            Text(text = "• $task")
        }
        Button(onClick = { tasks.add("New task ${tasks.size + 1}") }) {
            Text("Add Task")
        }
    }
}
```

#### Full Practical Example — Todo App

```kotlin
data class Todo(
    val id: Int,
    val text: String,
    val isDone: Boolean = false
)

@Composable
fun TodoApp() {
    val todos = remember {
        mutableStateListOf(
            Todo(1, "Read a book"),
            Todo(2, "Go for a walk"),
            Todo(3, "Write code")
        )
    }
    var newTodoText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("My Todos", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Add new todo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newTodoText,
                onValueChange = { newTodoText = it },
                label = { Text("New todo") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (newTodoText.isNotBlank()) {
                        todos.add(Todo(System.currentTimeMillis().toInt(), newTodoText))
                        newTodoText = ""
                    }
                }
            ) {
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Todo list
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(todos, key = { it.id }) { todo ->
                TodoItem(
                    todo = todo,
                    onToggle = {
                        val index = todos.indexOf(todo)
                        // ✅ Replace the item (immutable copy pattern)
                        todos[index] = todo.copy(isDone = !todo.isDone)
                    },
                    onDelete = { todos.remove(todo) }
                )
            }
        }

        // Summary
        val doneCount = todos.count { it.isDone }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$doneCount of ${todos.size} completed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TodoItem(
    todo: Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = todo.isDone, onCheckedChange = { onToggle() })
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = todo.text,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (todo.isDone) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
```

#### The Immutable Item Rule

```kotlin
// ❌ WRONG — data class with var properties
// Mutating a field does NOT notify Compose
data class Task(var name: String, var isDone: Boolean)
val tasks = remember { mutableStateListOf(Task("Buy milk", false)) }
tasks[0].isDone = true  // Compose WON'T see this — UI doesn't update!

// ✅ CORRECT — data class with val properties
// Use copy() to create a new instance
data class Task(val name: String, val isDone: Boolean)
val index = 0
tasks[index] = tasks[index].copy(isDone = true)  // New object = Compose sees it
```

This is because `mutableStateListOf` only observes **item replacement** (via `set`, `add`, `remove`). It cannot observe internal mutations to items. The items themselves must be immutable.

---

### 🔷 2.2 `mutableStateMapOf`

#### Why It Exists

When you need a key-value mapping where changes to the map (adding/removing entries or updating values) should trigger recomposition.

#### Which Library

**Compose Runtime** — no extra dependency.

```kotlin
import androidx.compose.runtime.mutableStateMapOf
```

#### Full Practical Example — Online User Status Tracker

```kotlin
@Composable
fun OnlineUsersPanel() {
    // Map of userId → online status
    val userStatus = remember {
        mutableStateMapOf(
            "alice" to true,
            "bob" to false,
            "charlie" to true
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Team Status", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        userStatus.entries.sortedBy { it.key }.forEach { (userId, isOnline) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (isOnline) Color.Green else Color.Gray,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(userId.replaceFirstChar { it.uppercase() })
                }
                Switch(
                    checked = isOnline,
                    onCheckedChange = { userStatus[userId] = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val onlineCount = userStatus.values.count { it }
        Text(
            text = "$onlineCount / ${userStatus.size} online",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
```

#### Full Practical Example — Form Field Errors Map

```kotlin
@Composable
fun ValidatedForm() {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    val errors = remember { mutableStateMapOf<String, String>() }

    fun validate(): Boolean {
        errors.clear()
        if (name.isBlank()) errors["name"] = "Name is required"
        if (!email.contains("@")) errors["email"] = "Enter a valid email"
        if (age.toIntOrNull() == null) errors["age"] = "Enter a valid age"
        else if (age.toInt() < 18) errors["age"] = "Must be 18 or older"
        return errors.isEmpty()
    }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormField("Name", name, { name = it }, errors["name"])
        FormField("Email", email, { email = it }, errors["email"])
        FormField("Age", age, { age = it }, errors["age"])

        Button(
            onClick = { if (validate()) { /* submit */ } },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit")
        }
    }
}

@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = error != null,
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
```

---

### 🔷 2.3 `toMutableStateList()` — Converting Existing Lists

When you have an existing list (from ViewModel, from a database, from an API) and want to make it mutable and observable inside a composable:

```kotlin
@Composable
fun EditableList(initialItems: List<String>) {
    // Convert the incoming immutable list to a snapshot list
    val items = remember(initialItems) {
        initialItems.toMutableStateList()
    }

    LazyColumn {
        items(items) { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(item)
                TextButton(onClick = { items.remove(item) }) {
                    Text("Remove")
                }
            }
        }
    }
}
```

> **Note:** `remember(initialItems)` means the list is re-created when `initialItems` changes. Use this pattern when the source data can change.

---

## Chapter 03: Flow-Based State

### 🔷 What Is "Flow" — The Full Background

#### The Reactive Programming Problem

In early Android development, fetching data from a network or database was done with callbacks:

```kotlin
// Old style — callback hell
repository.getUser(id) { user ->
    runOnUiThread {
        nameTextView.text = user.name
        repository.getPosts(user.id) { posts ->
            runOnUiThread {
                postsAdapter.submitList(posts)
            }
        }
    }
}
```

This was difficult to read, difficult to compose, and error-prone for threading.

RxJava came along and introduced **reactive streams** — the idea that data could be modeled as a sequence of values emitted over time, and you could use operators (`map`, `filter`, `flatMap`) to transform and combine these streams. But RxJava was large, complex, and not idiomatic Kotlin.

Kotlin's **Flow** is the coroutine-native answer to reactive streams. It is:

- Built into Kotlin Coroutines (no extra library needed beyond coroutines)
- Simpler and more idiomatic than RxJava
- Structured — cancellation and error handling work with Kotlin's structured concurrency
- Backpressure-aware — the producer can be suspended to avoid overwhelming the consumer

#### Flow Fundamentals

A `Flow<T>` is a **cold**, **asynchronous** stream of values of type `T`. "Cold" means the flow doesn't start producing values until someone starts collecting (subscribing) to it.

```
Producer (emits values) ──→ Flow<T> ──→ Collector (receives each T)
```

Everything is sequential and suspending by default.

---

### 🔷 Library for ALL Flow Types

All Flow types live in `kotlinx-coroutines-core`:

```kotlin
// build.gradle.kts (app module)
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    // For KMP commonMain: kotlinx-coroutines-core:1.10.2
}

// Imports (use only what you need)
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
```

---

### 🔷 3.1 Cold `Flow`

#### What Makes It "Cold"?

A cold flow only executes when a collector subscribes. Each collector gets its **own independent execution** of the flow — there is no shared state between collectors. Think of a cold flow like a function: calling it once runs it once for you; calling it twice runs it twice, each independently.

```kotlin
val myFlow: Flow<Int> = flow {
    println("Flow started!")   // This prints for each collector separately
    emit(1)
    delay(1000)
    emit(2)
    delay(1000)
    emit(3)
}

// Two separate collectors — two separate executions
launch { myFlow.collect { println("Collector 1: $it") } }
launch { myFlow.collect { println("Collector 2: $it") } }
// Output: Two independent streams, "Flow started!" printed twice
```

#### Full Practical Example — Room Database Queries

Room's database queries return `Flow<>` automatically. This is one of the most common uses:

```kotlin
// ─── Database Layer (Room DAO) ───────────────────────────────
@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY created_at DESC")
    fun getAllNotes(): Flow<List<Note>>  // Room emits new list whenever DB changes

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: Int): Flow<Note?>
}

// ─── Repository Layer ────────────────────────────────────────
class NoteRepository(private val dao: NoteDao) {
    fun getAllNotes(): Flow<List<Note>> = dao.getAllNotes()
    fun getNoteById(id: Int): Flow<Note?> = dao.getNoteById(id)

    // Custom flow for network data
    fun fetchRemoteNotes(): Flow<List<Note>> = flow {
        emit(emptyList())  // Initial empty state
        val notes = apiService.fetchNotes()
        emit(notes)
    }
}

// ─── ViewModel Layer ─────────────────────────────────────────
class NoteViewModel(private val repository: NoteRepository) : ViewModel() {
    // Convert cold Flow to hot StateFlow using stateIn
    val notes: StateFlow<List<Note>> = repository.getAllNotes()
        .map { notes ->
            notes.sortedBy { it.title }  // Transform each emission
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
```

#### Full Practical Example — Polling (Periodic Refresh)

```kotlin
// Emits current time every 30 seconds
val timeFlow: Flow<String> = flow {
    while (true) {
        val formatted = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            .format(Date())
        emit(formatted)
        delay(1_000)  // Wait 1 second before next emission
    }
}

@Composable
fun LiveClock() {
    val time by timeFlow.collectAsStateWithLifecycle(initialValue = "")
    Text(
        text = time,
        style = MaterialTheme.typography.displaySmall
    )
}
```

---

### 🔷 3.2 `StateFlow`

#### Why Does It Exist — The Gap in Regular Flow

Regular `Flow` is excellent for streams of data, but it has limitations for UI state:

1. **No initial value** — a new collector must wait for the first emission
2. **Cold** — if the UI and the ViewModel each try to collect the same flow, they get separate executions (inefficient)
3. **No "current value"** — you can't ask "what is the latest value right now?" without suspending

`StateFlow` was created to fill this gap. It models **observable state** — always has a current value, is hot (shared between all collectors), and immediately delivers the latest value to new collectors.

#### What `StateFlow` Is — Full Theory

`StateFlow<T>` is a hot, stateful, conflated observable value holder:

- **Hot**: Exists independently of whether anyone is collecting it
- **Stateful**: Always holds exactly one current value (accessible via `.value`)
- **Conflated**: If value changes multiple times between collection calls, only the latest is delivered
- **Equality-based**: Only emits a new value if the new value is NOT equal (via `equals()`) to the previous value — setting the same value twice does not emit

```
MutableStateFlow(0) → value = 1 → value = 2 → value = 2 (no emit, same value) → value = 3
Collector receives:   1           2                                               3
```

#### Code — Standard ViewModel Pattern

```kotlin
class UserProfileViewModel(
    private val repository: UserRepository
) : ViewModel() {

    // ─── State ────────────────────────────────────────────────
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    // ─── Actions ──────────────────────────────────────────────
    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val profile = repository.getUserProfile()
                _uiState.update { it.copy(isLoading = false, profile = profile) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onEditNameClicked(newName: String) {
        viewModelScope.launch {
            try {
                repository.updateName(newName)
                _uiState.update { it.copy(profile = it.profile?.copy(name = newName)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update name") }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class UserProfileUiState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val error: String? = null
)
```

#### `.value` vs `.update {}` — Thread Safety

```kotlin
// .value assignment — simple but not thread-safe
_uiState.value = _uiState.value.copy(name = "John")
// Problem: if two coroutines do this simultaneously:
// Coroutine A reads value → {name: "Alice", age: 30}
// Coroutine B reads value → {name: "Alice", age: 30}
// Coroutine A writes → {name: "John", age: 30}
// Coroutine B writes → {name: "Alice", age: 25}  ← Coroutine A's change lost!

// .update {} — atomic compare-and-set (CAS) operation — thread-safe
_uiState.update { currentState ->
    currentState.copy(name = "John")
}
// The lambda receives the MOST CURRENT value at time of execution
// If two coroutines call update simultaneously, one retries with the updated state
```

**Always prefer `.update {}` in production code.**

---

### 🔷 3.3 `MutableStateFlow` — The Writable Half

`MutableStateFlow` is the writable version of `StateFlow`. The convention is always:

- Keep `MutableStateFlow` **private** to the ViewModel (only VM can write)
- Expose read-only `StateFlow` to the UI (UI can only read)

```kotlin
// Full pattern with backing property
private val _count = MutableStateFlow(0)
val count: StateFlow<Int> = _count.asStateFlow()  // .asStateFlow() wraps it read-only

// Writing (ViewModel only)
_count.value = 5              // Direct assignment
_count.update { it + 1 }      // Atomic update (preferred)
_count.value++                // Works but not atomic — avoid in multithreaded contexts
```

---

### 🔷 3.4 `SharedFlow`

#### Why Does It Exist — The Problem with StateFlow for Events

`StateFlow` is perfect for **state** — things that have a current value, like "is loading?", "what is the current user?". But it's wrong for **events** — things that happen once and should be consumed once.

Consider navigation: when the user logs in successfully, you want to navigate to the home screen. If you model this as `StateFlow<String?>(null)`:

```kotlin
// ❌ Using StateFlow for navigation — problematic
private val _navigateToHome = MutableStateFlow(false)
```

Problem: When the user navigates back from home, your composable resubscribes and immediately replays the `true` value — navigating home again! You're stuck in a navigation loop.

`SharedFlow` solves this by being event-based, not state-based:

- **No current value** — it's not stateful
- **Configurable replay** — new subscribers can receive 0, 1, or N past events
- **Multiple subscribers** — all active subscribers receive each emission simultaneously

#### What `SharedFlow` Is — Full Theory

`SharedFlow` is a hot, event-based, multi-broadcast observable stream. It's the Kotlin equivalent of an **event bus** but type-safe and lifecycle-aware.

- **Hot**: exists and emits independently of collectors
- **Replay buffer**: configurable (usually 0 for events to prevent replaying)
- **Suspension-based**: `emit()` is a suspending function — it waits if the buffer is full
- **`tryEmit()`**: non-suspending version — returns false if buffer is full

#### Code — Events Pattern

```kotlin
// ─── Event definitions ────────────────────────────────────────
sealed class AuthEvent {
    data object NavigateToHome : AuthEvent()
    data object NavigateToOnboarding : AuthEvent()
    data class ShowError(val message: String) : AuthEvent()
    data class ShowSnackbar(val message: String, val isError: Boolean = false) : AuthEvent()
}

// ─── ViewModel ────────────────────────────────────────────────
class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // replay = 0: events should not be replayed to new subscribers
    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun onLoginClicked(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val result = authRepository.login(email, password)
                when (result) {
                    is AuthResult.Success -> {
                        _uiState.update { it.copy(isLoading = false) }
                        if (result.isFirstLogin) {
                            _events.emit(AuthEvent.NavigateToOnboarding)
                        } else {
                            _events.emit(AuthEvent.NavigateToHome)
                        }
                    }
                    is AuthResult.WrongPassword -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _events.emit(AuthEvent.ShowSnackbar("Wrong password", isError = true))
                    }
                    is AuthResult.NetworkError -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _events.emit(AuthEvent.ShowError("No internet connection"))
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(AuthEvent.ShowError(e.message ?: "Unknown error"))
            }
        }
    }
}

data class LoginUiState(
    val isLoading: Boolean = false
)

// ─── Composable ───────────────────────────────────────────────
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showErrorDialog by remember { mutableStateOf<String?>(null) }

    // Collect one-time events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AuthEvent.NavigateToHome -> navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
                AuthEvent.NavigateToOnboarding -> navController.navigate("onboarding") {
                    popUpTo("login") { inclusive = true }
                }
                is AuthEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
                    message = event.message,
                    duration = SnackbarDuration.Short
                )
                is AuthEvent.ShowError -> showErrorDialog = event.message
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LoginContent(
            isLoading = uiState.isLoading,
            onLogin = { email, password -> viewModel.onLoginClicked(email, password) },
            modifier = Modifier.padding(padding)
        )
    }

    showErrorDialog?.let { message ->
        AlertDialog(
            onDismissRequest = { showErrorDialog = null },
            title = { Text("Error") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = null }) { Text("OK") }
            }
        )
    }
}
```

#### `SharedFlow` Parameters Explained

```kotlin
MutableSharedFlow<T>(
    replay = 0,                              // New subscribers get no past events
    extraBufferCapacity = 0,                 // Additional buffer slots
    onBufferOverflow = BufferOverflow.SUSPEND // What to do when buffer is full
)
```

|`replay`|Effect|Use Case|
|---|---|---|
|`0`|New subscriber gets nothing from past|Navigation, one-time events|
|`1`|New subscriber gets last event|Last known good value|
|`N`|New subscriber gets last N events|Audit log, undo buffer|

|`onBufferOverflow`|Effect|
|---|---|
|`SUSPEND` (default)|`emit()` suspends until there's space — no events lost|
|`DROP_OLDEST`|Drops oldest buffered event when full|
|`DROP_LATEST`|Drops the incoming event when full|

For fire-and-forget events from UI, a common pattern is `extraBufferCapacity = 1` + `DROP_OLDEST`:

```kotlin
private val _events = MutableSharedFlow<UiEvent>(
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)

// Now you can safely call tryEmit() from non-suspending code
fun onButtonClicked() {
    _events.tryEmit(UiEvent.ShowToast("Clicked!"))
}
```

---

### 🔷 3.5 Flow Operators — The Full Toolkit

Flow operators transform streams between the producer and consumer. They are **lazy** — they don't do any work until a collector subscribes.

#### Transformation Operators

```kotlin
// map — transform each emitted value
val userNames: Flow<String> = usersFlow.map { user -> user.name }

// filter — only emit values matching a predicate
val premiumUsers: Flow<User> = usersFlow.filter { user -> user.isPremium }

// transform — fully custom, can emit 0 or multiple values per input
val notifications: Flow<Notification> = eventsFlow.transform { event ->
    if (event.isImportant) emit(Notification(event))
    if (event.requiresAlert) emit(AlertNotification(event))
}

// flatMapLatest — switch to a new flow on each emission (cancels previous)
val searchResults: Flow<List<Result>> = searchQueryFlow
    .flatMapLatest { query -> repository.search(query) }
    // When user types quickly, previous searches are cancelled
```

#### Combining Operators

```kotlin
// combine — merge two flows, emit whenever either changes
val displayText: Flow<String> = combine(
    nameFlow,
    ageFlow
) { name, age -> "$name is $age years old" }

// zip — pair values one-to-one (waits for both to emit)
val pairs: Flow<Pair<String, Int>> = nameFlow.zip(ageFlow) { name, age -> Pair(name, age) }

// merge — interleave emissions from multiple flows
val allEvents: Flow<Event> = merge(clickEvents, scrollEvents, networkEvents)
```

#### `stateIn` — Converting Cold Flow to StateFlow

This is very common in ViewModels — you have a `Flow` from Room or a repository, and you want to expose it as a `StateFlow` to the UI:

```kotlin
class ProductViewModel(repository: ProductRepository) : ViewModel() {

    // ✅ Pattern: Convert repository Flow to StateFlow in ViewModel
    val products: StateFlow<List<Product>> = repository.getProducts()
        .map { products -> products.filter { !it.isArchived } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
```

**`SharingStarted` options:**

|Option|Behavior|Best For|
|---|---|---|
|`WhileSubscribed(5000)`|Starts on first subscriber, stops 5s after last subscriber leaves|ViewModel (saves resources in background)|
|`Eagerly`|Starts immediately, never stops|Always-needed data|
|`Lazily`|Starts on first subscriber, never stops|One-time startup flows|

The 5-second grace period in `WhileSubscribed(5000)` is intentional — if the user rotates the device, the composable briefly has no subscribers. Without this grace period, the upstream flow would be cancelled and restarted, causing an unnecessary network/DB call. 5 seconds is enough for any rotation.

#### Utility Operators

```kotlin
// distinctUntilChanged — skip duplicate consecutive emissions
val uniqueQuery = searchQueryFlow.distinctUntilChanged()

// debounce — wait for emissions to pause before processing (for search)
val debouncedQuery = searchQueryFlow.debounce(300)  // Wait 300ms after last keystroke

// catch — handle errors in the flow
val safeFlow = repository.getDataFlow()
    .catch { e -> emit(emptyList()) }  // Recover from errors

// onEach — side effects without transforming values
val trackedFlow = eventsFlow
    .onEach { event -> analytics.track(event) }  // Log each event
```

#### Full Practical Example — Live Search with Debounce

```kotlin
class SearchViewModel(private val repository: SearchRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<SearchUiState> = _searchQuery
        .debounce(300)              // Wait 300ms after typing stops
        .distinctUntilChanged()     // Don't search if query unchanged
        .filter { it.isNotBlank() } // Don't search empty string
        .flatMapLatest { query ->   // Cancel previous search when new query arrives
            flow {
                emit(SearchUiState.Loading)
                try {
                    val results = repository.search(query)
                    emit(SearchUiState.Success(results))
                } catch (e: Exception) {
                    emit(SearchUiState.Error(e.message ?: "Search failed"))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SearchUiState.Idle
        )

    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }
}

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val results: List<SearchResult>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}
```

---

## Chapter 04: ViewModel + State

### 🔷 The History and Purpose of ViewModel

#### Before ViewModel — The Configuration Change Problem

Prior to `ViewModel`, handling screen rotation was painful. When a user rotated their device:

1. Android destroyed the current `Activity`
2. Created a fresh `Activity` instance
3. All your variables, ongoing network calls, and in-memory data were lost
4. You had to restore everything manually using `onSaveInstanceState(Bundle)`

But `Bundle` has limits — it's meant for small UI state (what was selected, what was typed), not for large data objects (lists of products, bitmaps, etc.).

The common "solutions" were:

- Store everything in `Application` singleton (breaks separation of concerns, causes leaks)
- Use retained `Fragment`s (very confusing, boilerplate-heavy)
- Re-fetch from network on every rotation (expensive and slow)

#### What ViewModel Solves

`ViewModel` is a lifecycle-aware component that:

- **Survives configuration changes** (rotation, language change, etc.)
- **Provides a dedicated scope** (`viewModelScope`) for coroutines that auto-cancels when the VM is destroyed
- **Separates UI concerns** from business logic concerns

```
User rotates phone →
  Activity: Destroyed & recreated (new instance)
  ViewModel: Not destroyed, same instance is returned
```

#### Library

```kotlin
// build.gradle.kts
dependencies {
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
}

// Imports
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
```

---

### 🔷 ViewModel Lifecycle — Exact Details

```
                  ┌─────────────────────────────────────┐
                  │          Activity Lifecycle          │
                  ├─────────────────────────────────────┤
  onCreate()  ────┤─── ViewModel.init{}  ←─── CREATED   │
                  │                                     │
  onPause()   ────┤                                     │
                  │                                     │
  onStop()    ────┤                                     │
                  │                                     │
  onDestroy() ────┤─── (due to rotation: VM SURVIVES)   │ ← Activity destroyed
                  │                                     │
  onCreate()  ────┤─── (same VM instance returned)      │ ← New Activity created
  ...             │                                     │
                  │                                     │
  onDestroy() ────┤─── ViewModel.onCleared() called     │ ← User exits: VM destroyed
                  └─────────────────────────────────────┘
```

`onCleared()` is called when:

- The user presses back and the screen is truly gone
- The `Activity` finishes (not from rotation)
- The `Fragment` is detached permanently

---

### 🔷 Full Standard ViewModel Pattern

```kotlin
// ─── Data Layer ───────────────────────────────────────────────
data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val imageUrl: String,
    val isInCart: Boolean = false
)

// ─── UI State ─────────────────────────────────────────────────
data class ProductListUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val cartItemCount: Int = 0,
    val error: String? = null,
    val searchQuery: String = ""
)

// ─── Events ───────────────────────────────────────────────────
sealed class ProductListEvent {
    data class NavigateToDetail(val productId: String) : ProductListEvent()
    data class ShowUndoSnackbar(val product: Product) : ProductListEvent()
    data class ShowError(val message: String) : ProductListEvent()
}

// ─── ViewModel ────────────────────────────────────────────────
class ProductListViewModel(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProductListEvent>()
    val events: SharedFlow<ProductListEvent> = _events.asSharedFlow()

    private var lastRemovedItem: Product? = null

    init {
        loadProducts()
        observeCartCount()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            productRepository.getProducts()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { products ->
                    _uiState.update { it.copy(isLoading = false, products = products) }
                }
        }
    }

    private fun observeCartCount() {
        viewModelScope.launch {
            cartRepository.getCartCount().collect { count ->
                _uiState.update { it.copy(cartItemCount = count) }
            }
        }
    }

    fun onProductClicked(product: Product) {
        viewModelScope.launch {
            _events.emit(ProductListEvent.NavigateToDetail(product.id))
        }
    }

    fun onAddToCartClicked(product: Product) {
        viewModelScope.launch {
            try {
                cartRepository.addItem(product)
                _events.emit(ProductListEvent.ShowUndoSnackbar(product))
            } catch (e: Exception) {
                _events.emit(ProductListEvent.ShowError("Failed to add to cart"))
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onRetryClicked() {
        _uiState.update { it.copy(error = null) }
        loadProducts()
    }

    override fun onCleared() {
        super.onCleared()
        // Any cleanup if needed (usually not, coroutines auto-cancel)
        println("ViewModel cleared — coroutines automatically cancelled")
    }
}
```

#### Connecting to Composable

```kotlin
@Composable
fun ProductListScreen(
    navController: NavController,
    viewModel: ProductListViewModel = viewModel()  // Lifecycle-managed
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect one-time events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProductListEvent.NavigateToDetail -> {
                    navController.navigate("product/${event.productId}")
                }
                is ProductListEvent.ShowUndoSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "${event.product.name} added to cart",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        // Handle undo
                    }
                }
                is ProductListEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Products") },
                actions = {
                    BadgedBox(badge = { Badge { Text(uiState.cartItemCount.toString()) } }) {
                        IconButton(onClick = { navController.navigate("cart") }) {
                            Icon(Icons.Default.ShoppingCart, "Cart")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.error != null -> ErrorScreen(
                message = uiState.error!!,
                onRetry = viewModel::onRetryClicked
            )

            else -> ProductGrid(
                products = uiState.products,
                onProductClick = viewModel::onProductClicked,
                onAddToCart = viewModel::onAddToCartClicked,
                modifier = Modifier.padding(padding)
            )
        }
    }
}
```

---

### 🔷 ViewModel with Hilt Injection

In production apps, you inject dependencies into ViewModels using Hilt (the recommended DI framework):

```kotlin
// ─── Module ───────────────────────────────────────────────────
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideProductRepository(api: ProductApi, db: AppDatabase): ProductRepository =
        ProductRepositoryImpl(api, db.productDao())
}

// ─── ViewModel with Hilt ──────────────────────────────────────
@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModel() { /* ... */ }

// ─── Composable ───────────────────────────────────────────────
@Composable
fun ProductListScreen(viewModel: ProductListViewModel = hiltViewModel()) {
    // hiltViewModel() instead of viewModel() — handles Hilt injection
}
```

---

## Chapter 05: SavedStateHandle

### 🔷 The Problem — ViewModel Does NOT Survive Process Death

This is a subtle but critical distinction:

```
Screen Rotation:
  OS kills Activity → OS recreates Activity → SAME ViewModel instance returned
  ✅ ViewModel state survives

Process Death (low memory):
  OS kills the entire app process → User returns → App cold starts
  ❌ ViewModel is gone — a new one is created
  ❌ All MutableStateFlow values reset to defaults
```

Process death happens when the OS needs memory for other apps. The user might have been on a checkout form, filled in their credit card number, put the phone down for a few minutes, and come back — only to find the form is empty.

`SavedStateHandle` solves this: it saves key-value pairs to Android's **saved instance state Bundle**, which persists through process death. When the app is cold-started to the exact activity/destination where the user was, the Bundle is restored.

---

### 🔷 Library

```kotlin
// Usually included already with lifecycle-viewmodel
implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.7")

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
```

---

### 🔷 How to Inject SavedStateHandle

With Hilt — automatic, no extra code:

```kotlin
@HiltViewModel
class FormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,  // Hilt provides this automatically
    private val repository: FormRepository
) : ViewModel()
```

Without Hilt — pass it as a constructor parameter (works if you use `viewModel()` in Compose):

```kotlin
class FormViewModel(
    private val savedStateHandle: SavedStateHandle  // Compose's viewModel() provides this
) : ViewModel()
```

---

### 🔷 Usage Patterns

#### Pattern 1: `getStateFlow()` — Recommended for observing in ViewModel

```kotlin
class CheckoutViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Backed by saved state — survives process death
    val shippingAddress: StateFlow<String> =
        savedStateHandle.getStateFlow("shipping_address", "")

    val paymentMethod: StateFlow<String> =
        savedStateHandle.getStateFlow("payment_method", "")

    fun onAddressChanged(address: String) {
        savedStateHandle["shipping_address"] = address  // Auto-saved to Bundle
    }

    fun onPaymentMethodChanged(method: String) {
        savedStateHandle["payment_method"] = method
    }
}
```

#### Pattern 2: `saveable` Delegate — Modern API (Lifecycle 2.9.0+)

The cleanest syntax — combines `SavedStateHandle` with `mutableStateOf`:

```kotlin
class ProfileEditViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // These survive process death AND look like regular Compose state
    var displayName by savedStateHandle.saveable { mutableStateOf("") }
        private set

    var bio by savedStateHandle.saveable { mutableStateOf("") }
        private set

    var selectedAvatarIndex by savedStateHandle.saveable { mutableStateOf(0) }
        private set

    fun onDisplayNameChange(name: String) { displayName = name }
    fun onBioChange(bio: String) { this.bio = bio }
    fun onAvatarSelected(index: Int) { selectedAvatarIndex = index }
}
```

#### Pattern 3: Navigation Arguments — Automatic

When using Navigation Compose, `SavedStateHandle` automatically receives navigation arguments:

```kotlin
// Navigation route with arguments
// "product/{productId}/edit?tab={tab}"

class ProductEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ProductRepository
) : ViewModel() {

    // Automatically populated from nav argument
    private val productId: String = checkNotNull(savedStateHandle["productId"])
    private val initialTab: String = savedStateHandle["tab"] ?: "details"

    init {
        loadProduct(productId)
    }
}
```

---

### 🔷 Full Practical Example — Multi-Step Checkout Form

```kotlin
@Parcelize
data class ShippingAddress(
    val fullName: String = "",
    val street: String = "",
    val city: String = "",
    val zipCode: String = "",
    val country: String = "India"
) : Parcelable

class CheckoutViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository
) : ViewModel() {

    companion object {
        const val KEY_STEP = "checkout_step"
        const val KEY_ADDRESS = "shipping_address"
        const val KEY_PAYMENT = "payment_method"
    }

    val currentStep: StateFlow<Int> =
        savedStateHandle.getStateFlow(KEY_STEP, 1)

    val shippingAddress: StateFlow<ShippingAddress> =
        savedStateHandle.getStateFlow(KEY_ADDRESS, ShippingAddress())

    val paymentMethod: StateFlow<String> =
        savedStateHandle.getStateFlow(KEY_PAYMENT, "")

    private val _events = MutableSharedFlow<CheckoutEvent>()
    val events: SharedFlow<CheckoutEvent> = _events.asSharedFlow()

    fun onAddressUpdated(address: ShippingAddress) {
        savedStateHandle[KEY_ADDRESS] = address
    }

    fun onPaymentMethodSelected(method: String) {
        savedStateHandle[KEY_PAYMENT] = method
    }

    fun onNextStep() {
        val step = currentStep.value
        if (step < 3) savedStateHandle[KEY_STEP] = step + 1
    }

    fun onPreviousStep() {
        val step = currentStep.value
        if (step > 1) savedStateHandle[KEY_STEP] = step - 1
    }

    fun onPlaceOrder() {
        viewModelScope.launch {
            try {
                val address = shippingAddress.value
                val payment = paymentMethod.value
                orderRepository.placeOrder(address, payment)
                _events.emit(CheckoutEvent.OrderPlaced)
            } catch (e: Exception) {
                _events.emit(CheckoutEvent.ShowError(e.message ?: "Order failed"))
            }
        }
    }
}
// If the OS kills the app in step 2 with the address filled in,
// when the user returns, they're back at step 2 with their address intact.
```

---

### 🔷 What Data SavedStateHandle Supports

|Type|Directly Supported?|
|---|---|
|`Int`, `Long`, `Float`, `Double`, `Boolean`, `String`|✅|
|Arrays of primitives|✅|
|`Parcelable` (via `@Parcelize`)|✅|
|`Serializable`|✅ (slower)|
|`ArrayList<Parcelable>`|✅|
|Large bitmap / large list|❌ Avoid — `TransactionTooLargeException`|

> **Golden rule:** Save only **identifiers** (IDs, keys) in `SavedStateHandle`. Re-fetch the actual data from the repository using those IDs.

---

## Chapter 06: Connecting State to UI

### 🔷 The Bridge Between Flows and Compose State

Compose UI is driven by Compose `State<T>` objects. `StateFlow` and other flows are not Compose state — they're Kotlin coroutine constructs. To use them in Compose, you need to **convert them** to Compose state. That's what `collectAsState` and `collectAsStateWithLifecycle` do.

---

### 🔷 6.1 `collectAsStateWithLifecycle` — The Android Standard

#### Why It Exists

Simply collecting a `Flow` inside a composable without lifecycle awareness means the collection continues even when the app is in the background (screen off, other app in foreground). This wastes CPU, network bandwidth, and battery.

`collectAsStateWithLifecycle` integrates with the Android `Lifecycle` — it automatically **pauses** collection when the lifecycle falls below the minimum state (default: `STARTED`) and **resumes** when it returns.

#### Library

```kotlin
// REQUIRED
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

import androidx.lifecycle.compose.collectAsStateWithLifecycle
```

#### Usage

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {

    // Automatically pauses when app goes background, resumes on foreground
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Use uiState...
}
```

#### With Custom Minimum State

```kotlin
// Default: STARTED (pauses in CREATED, collects in STARTED/RESUMED)
val state by viewModel.flow.collectAsStateWithLifecycle()

// Only collect when fully visible (RESUMED)
val state by viewModel.flow.collectAsStateWithLifecycle(
    minActiveState = Lifecycle.State.RESUMED
)
```

#### Full Practical Example

```kotlin
@Composable
fun NewsScreen(viewModel: NewsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // When phone goes to sleep: collection pauses, no more network calls
    // When user returns: collection resumes, new news loads

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.articles.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.error != null && uiState.articles.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.WifiOff, contentDescription = null, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(uiState.error!!)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = viewModel::retry) { Text("Retry") }
                }
            }
            else -> {
                LazyColumn {
                    items(uiState.articles) { article ->
                        ArticleCard(article)
                    }
                    if (uiState.isLoading) {
                        item { CircularProgressIndicator(modifier = Modifier.padding(16.dp)) }
                    }
                }
            }
        }
    }
}
```

---

### 🔷 6.2 `collectAsState` — KMP-Compatible Alternative

#### Why It Exists

`collectAsStateWithLifecycle` requires the Android Lifecycle — it doesn't work in KMP (Compose Multiplatform for iOS/Desktop). `collectAsState` is the platform-agnostic version.

The trade-off: it keeps collecting even when the app is backgrounded.

```kotlin
// No extra import needed — included in compose-runtime
import androidx.compose.runtime.collectAsState

@Composable
fun SharedScreenComponent(viewModel: SharedViewModel = viewModel()) {
    // Works on Android, iOS, Desktop — lifecycle-unaware
    val uiState by viewModel.uiState.collectAsState()
}
```

#### Comparison Table

|Feature|`collectAsStateWithLifecycle`|`collectAsState`|
|---|---|---|
|Lifecycle-aware|✅ Pauses in background|❌ Always collects|
|Battery/CPU efficient|✅ Yes|❌ Less efficient|
|Works on KMP|❌ Android only|✅ All platforms|
|Extra dependency needed|✅ Yes|❌ No|
|Recommended for Android|✅ Yes|Only if KMP needed|

#### Rule of Thumb

```kotlin
// If Android-only code:
val state by viewModel.uiState.collectAsStateWithLifecycle()

// If shared KMP code (commonMain):
val state by viewModel.uiState.collectAsState()
```

---

### 🔷 6.3 Initial Values

Both functions accept an optional initial value that's displayed before the first emission:

```kotlin
// Provide an initial value for the brief moment before first emission
val products by productFlow.collectAsStateWithLifecycle(
    initialValue = emptyList()
)

// Or for StateFlow — no initialValue needed (StateFlow always has a value)
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
// uiState.value is immediately available (StateFlow guarantees this)
```

---

## Chapter 07: State Hoisting & Unidirectional Data Flow

### 🔷 State Hoisting — The Theory

#### What Is It?

State hoisting is the pattern of **moving state up** from a child composable to its parent (or further up), and passing the current value and an event handler **down**. The child becomes stateless — it just displays what it receives and calls the lambda when the user interacts.

The name comes from the idea of "hoisting" (lifting) the state higher in the composition tree.

#### Why Does It Matter?

Without state hoisting:

- Your composables are hard to **reuse** (they own their own state — you can't initialize them with a value)
- Your composables are hard to **test** (you'd need to interact with the UI to change their state)
- Multiple composables can't share state without prop drilling or singletons
- The UI becomes a tightly-coupled mess

With state hoisting:

- Composables become **pure functions** of their inputs
- They're trivially **testable** (just call them with different parameters)
- They're highly **reusable** (any parent can control them)
- The state has a clear, single owner

#### The Golden Rule

```
State flows DOWN  (parent → child as parameter)
Events flow UP    (child → parent via lambda)
```

---

### 🔷 State Hoisting Examples

#### Basic — Stateful to Stateless Transformation

```kotlin
// ❌ Stateful — tied to its own state, hard to test, hard to reuse
@Composable
fun StatefulRatingBar() {
    var rating by remember { mutableStateOf(0) }
    RatingStars(rating = rating, onRatingChange = { rating = it })
}

// ✅ Stateless — pure, reusable, testable
@Composable
fun RatingBar(
    rating: Int,           // State comes in
    onRatingChange: (Int) -> Unit  // Events go out
) {
    Row {
        repeat(5) { index ->
            val starIndex = index + 1
            IconButton(onClick = { onRatingChange(starIndex) }) {
                Icon(
                    imageVector = if (starIndex <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Star $starIndex",
                    tint = if (starIndex <= rating) Color.Yellow else Color.Gray
                )
            }
        }
    }
}

// Caller owns the state — can initialize to any value, can react to changes
@Composable
fun ReviewForm() {
    var rating by rememberSaveable { mutableStateOf(3) }  // State owned here
    var reviewText by rememberSaveable { mutableStateOf("") }

    Column {
        Text("Rate your experience:")
        RatingBar(
            rating = rating,
            onRatingChange = { rating = it }  // State update happens here
        )
        OutlinedTextField(
            value = reviewText,
            onValueChange = { reviewText = it },
            label = { Text("Write your review") }
        )
    }
}
```

#### Intermediate — Hoisting to Screen Level

```kotlin
// ─── Stateless building blocks ────────────────────────────────
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Search") },
        trailingIcon = {
            IconButton(onClick = onSearch) {
                Icon(Icons.Default.Search, "Search")
            }
        },
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier
    )
}

@Composable
fun FilterChips(
    filters: List<String>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(filters) { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter) }
            )
        }
    }
}

// ─── Screen composable owns state ─────────────────────────────
@Composable
fun SearchScreen(viewModel: SearchViewModel = viewModel()) {
    // ViewModel manages business state
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Local UI state hoisted to screen level
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("All") }

    Column(modifier = Modifier.padding(16.dp)) {
        SearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { viewModel.search(searchQuery, selectedFilter) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        FilterChips(
            filters = listOf("All", "Products", "Brands", "Categories"),
            selectedFilter = selectedFilter,
            onFilterSelected = {
                selectedFilter = it
                viewModel.search(searchQuery, it)  // Re-search on filter change
            }
        )
        // Results...
    }
}
```

---

### 🔷 Unidirectional Data Flow (UDF)

#### What Is It?

UDF is an architectural pattern where data flows in **one direction only** through the app:

```
         ┌──────────────────────────────────────────┐
         │                                          │
    User Actions                              State Update
         │                                          ↑
         ↓                                          │
   ViewModel (events in)             ViewModel (state out)
         │                                          │
         ↓                                          │
   Repository / UseCase              StateFlow<UiState>
         │                                          │
         ↓                                          │
   Data Source            ─────────────────→   Composable
   (DB / Network)                               (reads state,
                                                 shows UI)
```

#### Why UDF?

The old way (View-based): the UI could directly modify other parts of the UI. A click handler could call `textView.text = "..."`, another method could call `button.isEnabled = false`, and a background thread might also be calling `listView.adapter.notifyDataSetChanged()`. Debugging who changed what and when was a nightmare.

With UDF:

- **One source of truth** — the `UiState` in the ViewModel is the only authoritative description of the screen
- **All state changes go through ViewModel** — the composable cannot modify state directly
- **Predictable** — for a given `UiState`, the UI always looks the same
- **Easy to test** — unit-test the ViewModel in isolation; UI is just a display layer

#### Full UDF Example — Cart Screen

```kotlin
// ─── State ────────────────────────────────────────────────────
data class CartUiState(
    val isLoading: Boolean = false,
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 40.0,
    val promoCode: String = "",
    val promoDiscount: Double = 0.0,
    val error: String? = null
) {
    val total: Double get() = subtotal + deliveryFee - promoDiscount
    val isEmpty: Boolean get() = items.isEmpty()
}

// ─── Events ───────────────────────────────────────────────────
sealed class CartUiEvent {
    data object NavigateToCheckout : CartUiEvent()
    data class ShowUndoRemove(val item: CartItem) : CartUiEvent()
    data class ShowError(val message: String) : CartUiEvent()
}

// ─── ViewModel ────────────────────────────────────────────────
class CartViewModel(private val cartRepository: CartRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CartUiEvent>()
    val events: SharedFlow<CartUiEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            cartRepository.getCartItems()
                .collect { items ->
                    val subtotal = items.sumOf { it.price * it.quantity }
                    _uiState.update {
                        it.copy(items = items, subtotal = subtotal)
                    }
                }
        }
    }

    // All state changes go through these functions
    fun onQuantityChanged(itemId: String, newQuantity: Int) {
        viewModelScope.launch {
            if (newQuantity == 0) {
                removeItem(itemId)
            } else {
                cartRepository.updateQuantity(itemId, newQuantity)
            }
        }
    }

    fun onRemoveItem(item: CartItem) {
        viewModelScope.launch {
            cartRepository.removeItem(item.id)
            _events.emit(CartUiEvent.ShowUndoRemove(item))
        }
    }

    fun onUndoRemove(item: CartItem) {
        viewModelScope.launch {
            cartRepository.addItem(item)
        }
    }

    fun onPromoCodeApplied(code: String) {
        viewModelScope.launch {
            val discount = cartRepository.validatePromoCode(code)
            if (discount != null) {
                _uiState.update { it.copy(promoCode = code, promoDiscount = discount) }
            } else {
                _events.emit(CartUiEvent.ShowError("Invalid promo code"))
            }
        }
    }

    fun onCheckoutClicked() {
        viewModelScope.launch {
            if (_uiState.value.isEmpty) return@launch
            _events.emit(CartUiEvent.NavigateToCheckout)
        }
    }

    private fun removeItem(itemId: String) {
        viewModelScope.launch {
            cartRepository.removeItem(itemId)
        }
    }
}
```

---

## Chapter 08: UI State Architecture

### 🔷 Single State Object Pattern — Why and How

#### The Problem with Multiple Boolean Flags

```kotlin
// ❌ BAD — separate variables create inconsistent combinations
class BadViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    private val _data = MutableStateFlow<List<Item>>(emptyList())
    private val _error = MutableStateFlow<String?>(null)

    // Problem: intermediate states where isLoading=false AND data=empty AND error=null
    // Is that initial state? An empty list? An error state?
    // You can never be sure. And you can accidentally have isLoading=true AND error!=null.
}
```

#### The Solution — Single UiState Data Class

```kotlin
// ✅ GOOD — single object, always consistent
data class ItemsUiState(
    val isLoading: Boolean = false,
    val items: List<Item> = emptyList(),
    val error: String? = null,
    val isRefreshing: Boolean = false
)
// Impossible to have isLoading=true AND error!=null if you manage them together

class GoodViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ItemsUiState())
    val uiState: StateFlow<ItemsUiState> = _uiState.asStateFlow()

    fun loadItems() {
        viewModelScope.launch {
            // Atomic state transition
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val items = repository.getItems()
                _uiState.update { it.copy(isLoading = false, items = items) }
            } catch (e: Exception) {
                // Another atomic transition
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
```

---

### 🔷 Sealed Class UiState — For Mutually Exclusive Screens

Use when states are completely distinct and cannot overlap:

```kotlin
// Sealed class makes each state explicit
sealed class ProductDetailUiState {
    data object Loading : ProductDetailUiState()
    data class Success(
        val product: Product,
        val relatedProducts: List<Product>,
        val reviews: List<Review>,
        val averageRating: Float
    ) : ProductDetailUiState()
    data class Error(
        val message: String,
        val isNetworkError: Boolean = false
    ) : ProductDetailUiState()
    data object ProductNotFound : ProductDetailUiState()
}

class ProductDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ProductRepository
) : ViewModel() {

    private val productId: String = checkNotNull(savedStateHandle["productId"])

    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init { loadProduct() }

    private fun loadProduct() {
        viewModelScope.launch {
            _uiState.value = ProductDetailUiState.Loading
            try {
                val product = repository.getProduct(productId)
                if (product == null) {
                    _uiState.value = ProductDetailUiState.ProductNotFound
                } else {
                    val related = repository.getRelatedProducts(productId)
                    val reviews = repository.getReviews(productId)
                    _uiState.value = ProductDetailUiState.Success(
                        product = product,
                        relatedProducts = related,
                        reviews = reviews,
                        averageRating = reviews.map { it.rating }.average().toFloat()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProductDetailUiState.Error(
                    message = e.message ?: "Unknown error",
                    isNetworkError = e is IOException
                )
            }
        }
    }
}

// ─── Composable ───────────────────────────────────────────────
@Composable
fun ProductDetailScreen(viewModel: ProductDetailViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Sealed class + when = exhaustive, compiler-checked
    when (val state = uiState) {
        ProductDetailUiState.Loading -> {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ProductDetailUiState.Success -> {
            ProductDetailContent(
                product = state.product,
                relatedProducts = state.relatedProducts,
                reviews = state.reviews,
                averageRating = state.averageRating
            )
        }
        is ProductDetailUiState.Error -> {
            ErrorContent(
                message = state.message,
                showRetryButton = state.isNetworkError,
                onRetry = viewModel::retry
            )
        }
        ProductDetailUiState.ProductNotFound -> {
            NotFoundContent()
        }
    }
}
```

#### When to Use Each

|Pattern|Use When|
|---|---|
|`data class UiState`|States can coexist (loading + partial data), most screens|
|`sealed class UiState`|States are mutually exclusive (only Loading OR Success OR Error)|

---

### 🔷 Single Source of Truth (SSOT)

**Only one component in your app should own and mutate a given piece of data.**

Violations lead to inconsistencies — two places think they have the "current" value, and they disagree.

```kotlin
// ❌ SSOT violation — user data in two places
class UserViewModel : ViewModel() {
    val users = MutableStateFlow<List<User>>(emptyList())
}

@Composable
fun UserListScreen(viewModel: UserViewModel = viewModel()) {
    // ❌ This makes a local copy — now there are two sources!
    val users by remember { mutableStateOf(listOf<User>()) }
    // Which one is correct? They'll diverge.
}

// ✅ SSOT — only ViewModel holds user data
@Composable
fun UserListScreen(viewModel: UserViewModel = viewModel()) {
    val users by viewModel.users.collectAsStateWithLifecycle()  // ✅ One source
    // UI is just a mirror of ViewModel state
}
```

---

## Chapter 09: State Stability & Performance

### 🔷 How Recomposition Works — The Full Picture

#### Skip Optimization

Compose has an optimization called **smart recomposition**: when a composable is called during recomposition, if all its parameters are **stable** and unchanged, Compose can **skip** that composable entirely, saving the work of calling it.

```kotlin
@Composable
fun UserCard(user: User, onFollowClick: () -> Unit) {
    // If `user` and `onFollowClick` haven't changed since last composition,
    // Compose MAY skip calling this function entirely
}
```

This optimization is critical for performance in complex UIs. But it only works if Compose can verify that the parameters are stable.

---

### 🔷 What Is "Stable" — The Exact Rules

Compose considers a type **stable** if it meets these criteria:

1. `equals()` is consistent and correct (two equal instances always produce the same UI)
2. When any property changes, the change is notified to Compose (so the snapshot system can trigger recomposition)
3. All public properties are also stable

**Automatically stable types:**

- All primitive types: `Int`, `Long`, `Boolean`, `Float`, `Double`, `Char`
- `String`
- Kotlin `data class` with only `val` properties that are all themselves stable
- Compose `State<T>` types
- `@Immutable` annotated classes
- `@Stable` annotated classes

**Types that may be considered unstable by the Compose compiler:**

- `List<T>`, `Map<K, V>`, `Set<T>` — these are Kotlin interfaces that technically allow mutation; even though you're using them immutably, the compiler doesn't know that
- Any class from a third-party library that the Compose compiler hasn't analyzed
- `data class` with any `var` property

---

### 🔷 `@Immutable` Annotation

```kotlin
import androidx.compose.runtime.Immutable
```

`@Immutable` is a promise to Compose: _"I guarantee that all properties of this class will never change after construction."_ Compose trusts this promise and marks the type as stable.

```kotlin
// List<Product> is unstable by default (List interface could be mutable)
data class ProductsUiState(
    val products: List<Product>  // ← unstable!
)
// ProductsUiState will be considered unstable, causing unnecessary recompositions

// ✅ Fix 1: Mark the whole state as @Immutable
@Immutable
data class ProductsUiState(
    val products: List<Product>  // ← now stable because the holder class is @Immutable
)
```

---

### 🔷 `@Stable` Annotation

```kotlin
import androidx.compose.runtime.Stable
```

`@Stable` is a softer version: _"This type may change, but it will notify Compose via the snapshot system when it does."_ Compose can still optimize recomposition for classes marked `@Stable`.

Use `@Stable` for classes that hold `MutableState` internally:

```kotlin
@Stable
class CartState(
    initialItems: List<CartItem> = emptyList()
) {
    private val _items = mutableStateListOf<CartItem>(*initialItems.toTypedArray())
    val items: List<CartItem> = _items  // Compose knows to observe this

    fun addItem(item: CartItem) { _items.add(item) }
    fun removeItem(id: String) { _items.removeIf { it.id == id } }
    val totalPrice: Double get() = _items.sumOf { it.price * it.quantity }
}
```

---

### 🔷 ImmutableList — The Production Solution

For production apps with large lists, use `kotlinx-collections-immutable`:

```kotlin
// build.gradle.kts
implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
```

```kotlin
// ✅ ImmutableList is explicitly stable — Compose handles it perfectly
data class SearchUiState(
    val results: ImmutableList<SearchResult> = persistentListOf(),
    val isLoading: Boolean = false
)

// In ViewModel
val uiState: StateFlow<SearchUiState> = searchResultsFlow
    .map { results ->
        SearchUiState(results = results.toImmutableList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())
```

---

### 🔷 Full Practical Example — Diagnosing Unnecessary Recompositions

```kotlin
// Suspect: this recomposes too often
@Composable
fun ProductCard(product: Product, onAddToCart: () -> Unit) {
    Card {
        Column {
            Text(product.name)
            Text("₹${product.price}")
            Button(onClick = onAddToCart) { Text("Add to Cart") }
        }
    }
}

// ─── Diagnosis ────────────────────────────────────────────────
// Product might be: data class Product(val id: String, val name: String, val price: Double)
// This is a stable data class with all val — should be fine.
// But onAddToCart: () -> Unit — lambdas are NOT stable by default!

// ─── Fix: Use stable lambda ───────────────────────────────────
@Composable
fun ProductListScreen(viewModel: ProductListViewModel = viewModel()) {
    val products by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn {
        items(products.items, key = { it.id }) { product ->
            ProductCard(
                product = product,
                onAddToCart = remember(product.id) {  // Stable lambda
                    { viewModel.onAddToCart(product.id) }
                }
            )
        }
    }
}
```

---

## Chapter 10: LiveData — Legacy

### 🔷 History and Why It Still Exists

`LiveData` was introduced in 2017 as part of Android Architecture Components — the first official lifecycle-aware observable from Google. Before `LiveData`:

- Developers used `EventBus`, `RxJava`, custom Observer patterns, or direct UI manipulation
- None of these were lifecycle-aware — they caused memory leaks and crashes from updating UI after `Activity` stopped

`LiveData` was revolutionary at the time: it was simple, lifecycle-aware, and worked with `Observer` in Java. But it predates Kotlin coroutines and has limitations:

- Java-centric API
- No built-in operators (no `map`, `filter`, `combine` without `Transformations.map()`)
- Null-unsafe (default type is nullable)
- Not KMP-compatible

Google's recommendation today: **use `StateFlow` for new code**. But millions of lines of existing Android code use `LiveData`, so you'll encounter it.

#### Library

```kotlin
implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
```

---

### 🔷 Basic LiveData in ViewModel

```kotlin
class LegacyViewModel : ViewModel() {

    private val _userName = MutableLiveData<String>("")
    val userName: LiveData<String> = _userName

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Transformation using Transformations.map (LiveData operator)
    val userNameUppercase: LiveData<String> = _userName.map { it.uppercase() }

    fun updateName(name: String) {
        _userName.value = name          // Main thread (safe)
        // _userName.postValue(name)    // Background thread
    }

    // liveData builder — for async operations
    val userData: LiveData<User> = liveData {
        emit(repository.getUser())
    }
}
```

#### Observing LiveData in Compose

```kotlin
// Requires this dependency:
implementation("androidx.compose.runtime:runtime-livedata:1.x.x")

import androidx.compose.runtime.livedata.observeAsState

@Composable
fun LegacyScreen(viewModel: LegacyViewModel = viewModel()) {
    val userName by viewModel.userName.observeAsState(initial = "")
    val isLoading by viewModel.isLoading.observeAsState(initial = false)

    Column {
        if (isLoading) CircularProgressIndicator()
        Text("User: $userName")
    }
}
```

---

### 🔷 Migration: LiveData → StateFlow

```kotlin
// Step 1: In ViewModel, convert existing LiveData to StateFlow
class MigratedViewModel(
    private val repository: UserRepository
) : ViewModel() {

    // Before (LiveData)
    // private val _user = MutableLiveData<User>()
    // val user: LiveData<User> = _user

    // After (StateFlow)
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    // If you have existing LiveData from a Room DAO or library:
    // Convert using .asFlow().stateIn(...)
    val legacyRoomData: StateFlow<List<Item>> = legacyLiveData
        .asFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}

// Step 2: In Composable, use collectAsStateWithLifecycle
@Composable
fun MigratedScreen(viewModel: MigratedViewModel = viewModel()) {
    // Before:
    // val user by viewModel.user.observeAsState()

    // After:
    val user by viewModel.user.collectAsStateWithLifecycle()
}
```

---

### 🔷 LiveData vs StateFlow — Full Comparison

|Feature|`LiveData`|`StateFlow`|
|---|---|---|
|Language|Java-friendly|Kotlin-first|
|Always has value|❌ (can be null initially)|✅ (requires initial value)|
|Null safety|❌ Default nullable|✅ Non-null by default|
|Operators|Limited (map, switchMap)|Full Flow operators|
|Backpressure|❌ No|✅ Yes (via Flow)|
|KMP support|❌ Android only|✅ Full KMP|
|Testing|Requires `InstantTaskExecutorRule`|Standard coroutine testing|
|Google recommendation|Legacy|✅ Preferred|

---

## Chapter 11: KMP — State Across Platforms

### 🔷 What Is KMP and Why Does State Management Change?

**Kotlin Multiplatform (KMP)** allows you to write shared Kotlin code that compiles to:

- JVM bytecode for Android
- Native code for iOS (via Kotlin/Native)
- JavaScript for web
- Desktop JVM for macOS/Windows/Linux

The shared code lives in `commonMain`. Platform-specific code lives in `androidMain`, `iosMain`, etc.

The challenge: **Android-specific APIs don't exist in `commonMain`**. This includes:

- `rememberSaveable` (uses Android `Bundle`)
- `LiveData` (Android-only observable)
- `collectAsStateWithLifecycle` (uses Android `Lifecycle`)
- `AndroidX ViewModel` (uses Android's `ViewModelProvider`)

For KMP, you use the **JetBrains KMP lifecycle library** (`org.jetbrains.androidx.lifecycle:*`) which provides cross-platform equivalents.

---

### 🔷 What Works in `commonMain`

|State Tool|KMP `commonMain`?|Notes|
|---|---|---|
|`mutableStateOf`|✅|With Compose Multiplatform|
|`remember`|✅|With Compose Multiplatform|
|`derivedStateOf`|✅|With Compose Multiplatform|
|`mutableStateListOf`|✅|With Compose Multiplatform|
|`Flow` / `StateFlow` / `SharedFlow`|✅|`kotlinx-coroutines-core`|
|`ViewModel` (JetBrains)|✅|`org.jetbrains.androidx.lifecycle:lifecycle-viewmodel`|
|`SavedStateHandle` (JetBrains 2.9+)|✅|`org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-savedstate`|
|`collectAsState`|✅|`compose-runtime`|
|`rememberSaveable`|❌|Android-only|
|`collectAsStateWithLifecycle`|❌ (Android-only) / ✅ (JetBrains)|JetBrains version works|
|`LiveData`|❌|Android-only|

---

### 🔷 KMP Dependencies

```kotlin
// libs.versions.toml
[versions]
kotlin = "2.1.0"
coroutines = "1.10.2"
lifecycle = "2.9.0"                    # JetBrains KMP lifecycle
compose-multiplatform = "1.7.0"

[libraries]
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }

# JetBrains KMP lifecycle (NOT AndroidX)
lifecycle-viewmodel-kmp = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel", version.ref = "lifecycle" }
lifecycle-savedstate-kmp = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-savedstate", version.ref = "lifecycle" }
lifecycle-runtime-compose-kmp = { module = "org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }

// shared/build.gradle.kts
kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.lifecycle.viewmodel.kmp)
            implementation(libs.lifecycle.savedstate.kmp)
            implementation(libs.lifecycle.runtime.compose.kmp)  // collectAsState for KMP
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            // AndroidX lifecycle for lifecycle-aware collection
            implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
        }
    }
}
```

---

### 🔷 Shared ViewModel Example (commonMain)

```kotlin
// ─── shared/src/commonMain/kotlin/viewmodel/SharedViewModel.kt ─
import org.jetbrains.androidx.lifecycle.ViewModel
import org.jetbrains.androidx.lifecycle.viewModelScope  // KMP version

class SharedCounterViewModel : ViewModel() {  // JetBrains ViewModel

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    fun increment() { _count.update { it + 1 } }
    fun decrement() { _count.update { if (it > 0) it - 1 else 0 } }
    fun reset() { _count.value = 0 }
}

// ─── Android composable (androidMain or shared) ───────────────
@Composable
fun AndroidCounterScreen(viewModel: SharedCounterViewModel = viewModel()) {
    val count by viewModel.count.collectAsStateWithLifecycle()  // Android-specific
    CounterUI(count, viewModel::increment, viewModel::decrement)
}

// ─── iOS composable (iosMain) ─────────────────────────────────
@Composable
fun IosCounterScreen(viewModel: SharedCounterViewModel) {
    val count by viewModel.count.collectAsState()  // KMP-compatible
    CounterUI(count, viewModel::increment, viewModel::decrement)
}

// ─── Shared UI (commonMain) ───────────────────────────────────
@Composable
fun CounterUI(
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", style = MaterialTheme.typography.displayLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onDecrement) { Text("−") }
            Button(onClick = onIncrement) { Text("+") }
        }
    }
}
```

---

### 🔷 The iOS Problem — Bridging Flow to Swift

Kotlin's `StateFlow` and `SharedFlow` cannot be directly consumed from Swift. Swift doesn't understand Kotlin's suspension model.

**Option 1: SKIE (recommended)**

SKIE (Swift Kotlin Interface Enhancer) is a Gradle plugin that transforms Kotlin flows into Swift-native `AsyncSequence`:

```kotlin
// project-level build.gradle.kts
plugins {
    id("co.touchlab.skie") version "0.8.4" apply false
}

// shared/build.gradle.kts
plugins {
    id("co.touchlab.skie")
}
```

After SKIE, your Swift code simply becomes:

```swift
// iOS Swift code
@MainActor
class CounterViewController: UIViewController {
    let viewModel = SharedCounterViewModel()

    override func viewDidLoad() {
        super.viewDidLoad()
        Task {
            for await count in viewModel.count {
                countLabel.text = "\(count)"
            }
        }
    }
}
```

**Option 2: KMP-NativeCoroutines**

```kotlin
// Add annotations in shared code
import com.rickclephas.kmm.foundation.coroutines.NativeCoroutinesState
import com.rickclephas.kmm.foundation.coroutines.NativeCoroutines

class MyViewModel : ViewModel() {
    @NativeCoroutinesState
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    @NativeCoroutines
    val events: SharedFlow<Event> = _events.asSharedFlow()
}
```

---

## Chapter 12: Gradle Dependencies Master List

### 🔷 Android-Only Projects (BOM Approach)

```kotlin
// build.gradle.kts (app module)
dependencies {
    // ── Compose BOM (manages all Compose versions together) ──────
    val composeBom = platform("androidx.compose:compose-bom:2025.05.00")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
    // runtime-saveable usually included via material3 or foundation

    // ── Lifecycle / ViewModel ──────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")  // collectAsStateWithLifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.7")

    // ── Coroutines ─────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // ── Optional: Immutable collections (performance) ──────────
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")

    // ── Optional: LiveData in Compose (legacy) ─────────────────
    implementation("androidx.compose.runtime:runtime-livedata")  // via BOM

    // ── Optional: Hilt (dependency injection) ──────────────────
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ── Optional: Room (database with Flow) ────────────────────
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")  // Flow support
    kapt("androidx.room:room-compiler:2.6.1")
}
```

---

### 🔷 KMP Projects

```kotlin
// shared/build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("co.touchlab.skie") version "0.8.4"  // Optional: iOS Flow bridging
}

kotlin {
    androidTarget()
    iosX64(); iosArm64(); iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.9.0")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-savedstate:2.9.0")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")
        }
        androidMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
            implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
        }
    }
}
```

---

### 🔷 Minimum Versions Reference

|Library|Min Kotlin|Min API|KMP|
|---|---|---|---|
|`kotlinx-coroutines-core` 1.8+|1.8|21|✅|
|`lifecycle-viewmodel` 2.8+|1.8|21|✅ (JetBrains)|
|`lifecycle-runtime-compose` 2.6+|1.8|21|✅ (JetBrains)|
|`rememberSaveable`|Any Compose|21|❌|
|`rememberSerializable`|Kotlin 2.0|21|✅|
|`SavedStateHandle` KMP|Kotlin 2.0|21|✅ (JetBrains 2.9+)|
|Compose Multiplatform|Kotlin 2.0|21|✅|

---

## Chapter 13: Common Mistakes & Errors

### ❌ Mistake 1: Missing `getValue`/`setValue` Imports for `by` Delegate

**Symptom:** `Unresolved reference: getValue` / red squiggly under `by`

```kotlin
// ❌ ERROR — missing imports
var text by remember { mutableStateOf("") }

// ✅ Fix — add both imports
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

var text by remember { mutableStateOf("") }  // Now works
```

**Why:** The `by` keyword uses Kotlin's operator overloading. `getValue` and `setValue` are extension operators defined on `MutableState<T>`. Without importing them, the compiler can't find them.

---

### ❌ Mistake 2: Using Regular MutableList in State

**Symptom:** Items added to the list but UI doesn't update

```kotlin
// ❌ Wrong — regular mutableListOf is not observable
var items by remember { mutableStateOf(mutableListOf<String>()) }
items.add("item")  // List mutated but Compose not notified — UI stuck

// ❌ Also wrong — reassigning same list reference doesn't trigger recomposition
items = items  // Same reference, equals() returns true — no recomposition

// ✅ Correct — snapshot list
val items = remember { mutableStateListOf<String>() }
items.add("item")  // Compose notified automatically

// ✅ Also correct — create a new list for StateFlow/State
var itemsState by remember { mutableStateOf(listOf<String>()) }
itemsState = itemsState + "item"  // New list reference → recomposition
```

---

### ❌ Mistake 3: Creating ViewModel Manually Inside Composable

**Symptom:** State resets every time the composable recomposes; ViewModel doesn't survive rotation

```kotlin
// ❌ Creates NEW ViewModel every single recomposition
@Composable
fun MyScreen() {
    val viewModel = MyViewModel()  // New instance every time!
    // State from previous recomposition is lost
}

// ✅ Compose infrastructure manages the ViewModel lifecycle
@Composable
fun MyScreen() {
    val viewModel: MyViewModel = viewModel()  // Same instance across recompositions
    // OR with Hilt:
    val viewModel: MyViewModel = hiltViewModel()
}
```

---

### ❌ Mistake 4: Collecting Flow in `LaunchedEffect` Without Lifecycle Awareness

**Symptom:** App collects data in background, wasting battery; potential memory leaks

```kotlin
// ❌ Collects even when app is in background, no lifecycle awareness
@Composable
fun MyScreen(viewModel: MyViewModel = viewModel()) {
    var state by remember { mutableStateOf<MyState?>(null) }

    LaunchedEffect(Unit) {
        viewModel.stateFlow.collect { newState ->
            state = newState  // Keeps running even when app is hidden
        }
    }

    // Use state...
}

// ✅ Use collectAsStateWithLifecycle — handles everything
@Composable
fun MyScreen(viewModel: MyViewModel = viewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    // Automatically pauses when app is backgrounded
}
```

---

### ❌ Mistake 5: Exposing `MutableStateFlow` from ViewModel

**Symptom:** UI directly modifies state, bypassing ViewModel logic, breaking UDF

```kotlin
// ❌ UI can write to this directly — breaks encapsulation
class BadViewModel : ViewModel() {
    val uiState = MutableStateFlow(MyState())  // Public mutable!
}

@Composable
fun BadScreen(viewModel: BadViewModel = viewModel()) {
    // UI is now the source of truth — ViewModel is useless
    viewModel.uiState.value = MyState(count = 99)  // Bypasses all VM logic!
}

// ✅ Private mutable, public read-only
class GoodViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MyState())
    val uiState: StateFlow<MyState> = _uiState.asStateFlow()  // Read-only

    fun onAction() {  // Only way to change state
        _uiState.update { it.copy(count = it.count + 1) }
    }
}
```

---

### ❌ Mistake 6: Using `remember` Instead of `rememberSaveable` for User Input

**Symptom:** User typed text disappears on screen rotation

```kotlin
// ❌ Lost on rotation — frustrating for the user
@Composable
fun RegistrationForm() {
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    // User types email, rotates phone → email is gone!
}

// ✅ Survives rotation
@Composable
fun RegistrationForm() {
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
}
```

**Rule of thumb:** If the user **typed it** or **selected it**, use `rememberSaveable`.

---

### ❌ Mistake 7: Using `StateFlow` for Navigation/Toast Events

**Symptom:** Navigation triggers again when the user navigates back; toast shown multiple times

```kotlin
// ❌ StateFlow holds its last value — new subscribers replay it
class BadViewModel : ViewModel() {
    private val _navigateTo = MutableStateFlow<String?>(null)
    val navigateTo: StateFlow<String?> = _navigateTo.asStateFlow()
}

@Composable
fun BadScreen(navController: NavController, viewModel: BadViewModel = viewModel()) {
    val navigateTo by viewModel.navigateTo.collectAsStateWithLifecycle()

    LaunchedEffect(navigateTo) {
        navigateTo?.let {
            navController.navigate(it)
            // Problem: can't "consume" it — next time this composable recomposes,
            // navigateTo still has the value → navigates again!
        }
    }
}

// ✅ SharedFlow(replay=0) — events are fire-once
class GoodViewModel : ViewModel() {
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    fun navigate(route: String) {
        viewModelScope.launch { _events.emit(UiEvent.Navigate(route)) }
    }
}
```

---

### ❌ Mistake 8: Storing Large Objects in `rememberSaveable` / `SavedStateHandle`

**Symptom:** `TransactionTooLargeException` crash; very slow save/restore

```kotlin
// ❌ Bundle has a ~1MB limit — large objects crash the app
var products by rememberSaveable { mutableStateOf(listOf<Product>()) }
savedStateHandle["all_products"] = largeProductList

// ✅ Store only the ID or key — refetch data from repository
var selectedProductId by rememberSaveable { mutableStateOf("") }
savedStateHandle["selected_id"] = productId
// Then load the product from repository using the ID

// ✅ Keep large data in ViewModel (survives rotation but not process death)
// If it can be refetched cheaply, just refetch on process death
```

---

### ❌ Mistake 9: Non-Atomic State Updates (Race Condition)

**Symptom:** Counts are wrong; state updates appear to be lost

```kotlin
// ❌ Race condition — two coroutines can read the same value
// and both overwrite each other's changes
fun incrementLikes() {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            likeCount = _uiState.value.likeCount + 1  // Non-atomic read-modify-write
        )
    }
}

// ✅ Atomic update — lambda receives guaranteed current value
fun incrementLikes() {
    _uiState.update { state ->
        state.copy(likeCount = state.likeCount + 1)  // Atomic
    }
}
```

---

### ❌ Mistake 10: Mutating `var` Properties in Data Classes

**Symptom:** Properties change but UI never updates

```kotlin
// ❌ Mutable properties — Compose can't observe field mutations
data class Counter(var count: Int)  // var = mutable field

var counter by remember { mutableStateOf(Counter(0)) }
counter.count++  // Field mutated but MutableState doesn't know!
// counter.value didn't change (same object reference) → no recomposition

// ✅ Immutable properties + replace the whole object
data class Counter(val count: Int)  // val = immutable

var counter by remember { mutableStateOf(Counter(0)) }
counter = counter.copy(count = counter.count + 1)  // New object → recomposition
```

---

### ❌ Mistake 11: Calling `stateIn` Inside a Composable

**Symptom:** New StateFlow created on every recomposition; upstream re-subscribed constantly

```kotlin
// ❌ stateIn inside composable — called every recomposition
@Composable
fun MyScreen(viewModel: MyViewModel = viewModel()) {
    // This creates a NEW StateFlow on every recomposition!
    val items by viewModel.itemsFlow
        .stateIn(rememberCoroutineScope(), SharingStarted.Eagerly, emptyList())
        .collectAsState()
}

// ✅ stateIn in ViewModel — created once, shared properly
class MyViewModel(repository: Repository) : ViewModel() {
    val items: StateFlow<List<Item>> = repository.getItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
```

---

### ❌ Mistake 12: Not Using `key` in `LazyColumn` Items

**Symptom:** Animations broken; wrong items recompose; performance issues

```kotlin
// ❌ No key — Compose uses index as identity
LazyColumn {
    items(products) { product ->
        ProductCard(product)
    }
}

// ✅ Stable key — Compose tracks items by identity
LazyColumn {
    items(products, key = { product -> product.id }) { product ->
        ProductCard(product)
        // When items reorder, existing composables are reused — no flicker
    }
}
```

---

## Chapter 14: Decision Guide — What to Use When

### 🔷 Quick Decision Flowchart

```
Is this UI-local state?
├── YES → Does it need to survive rotation?
│         ├── YES → rememberSaveable { mutableStateOf() }
│         └── NO  → remember { mutableStateOf() }
└── NO  → Is it business/screen logic?
          ├── YES → ViewModel + StateFlow
          │         ├── Is it a one-time event? → SharedFlow
          │         └── Must survive process death? → SavedStateHandle
          └── NO  → Is it a stream from DB/network?
                    └── YES → Flow → stateIn → StateFlow in ViewModel

Is it a list/map that changes at runtime in UI?
└── mutableStateListOf / mutableStateMapOf

Is it computed from other state?
└── derivedStateOf (when result changes less often than source)

Reading state in composable?
├── Android only → collectAsStateWithLifecycle()
└── KMP (commonMain) → collectAsState()
```

---

### 🔷 Complete Decision Table

|Situation|Recommended Tool|Reason|
|---|---|---|
|Toggle dialog open/closed|`remember { mutableStateOf(false) }`|UI-local, no need to survive rotation|
|Text field input|`rememberSaveable { mutableStateOf("") }`|User typed it — preserve on rotation|
|Form data across multiple fields|`rememberSaveable { mutableStateOf(...) }`|User input must survive rotation|
|Dynamic list that user edits|`remember { mutableStateListOf() }`|Compose-observable mutable list|
|Key-value data in UI|`remember { mutableStateMapOf() }`|Compose-observable mutable map|
|Screen-level loading/error/data state|`StateFlow<UiState>` in ViewModel|Survives rotation, shared across composables|
|Async operation result|`StateFlow` + coroutine in ViewModel|Proper scope + lifecycle management|
|One-time event (navigate, toast)|`SharedFlow(replay=0)` in ViewModel|Fire-once, not re-delivered|
|Computed/derived flag|`derivedStateOf { }`|Only recomposes when result changes|
|State that survives process death (UI)|`rememberSaveable`|Backed by Bundle|
|State that survives process death (business)|`SavedStateHandle` in ViewModel|Backed by Bundle, VM-level|
|Database query result|`Flow` from Room → `stateIn()`|Room emits on every DB change|
|Polling/repeated data|Cold `Flow` with `delay()` loop|Simple periodic emission|
|Legacy code observable|`LiveData` (or migrate to StateFlow)|Already exists in codebase|
|Reading state in Compose (Android)|`collectAsStateWithLifecycle()`|Lifecycle-aware, battery efficient|
|Reading state in Compose (KMP)|`collectAsState()`|Platform-agnostic|

---

### 🔷 State Ownership Levels

```
┌─────────────────────────────────────────────────────────────────────┐
│ LEVEL 1: Single Composable                                          │
│   → remember { mutableStateOf() }     — recomposition survivor     │
│   → rememberSaveable { mutableStateOf() } — rotation survivor      │
│   → mutableStateListOf / mutableStateMapOf — collections           │
├─────────────────────────────────────────────────────────────────────┤
│ LEVEL 2: Shared Across Composables on One Screen                    │
│   → State hoisting to common parent composable                      │
│   → Or ViewModel (StateFlow) — clean separation, testable          │
├─────────────────────────────────────────────────────────────────────┤
│ LEVEL 3: Shared Across Screens / Business Logic                     │
│   → ViewModel (StateFlow) — survives navigation transitions         │
│   → SavedStateHandle — survives process death                       │
├─────────────────────────────────────────────────────────────────────┤
│ LEVEL 4: App-Wide / Global                                          │
│   → Singleton ViewModel (scoped to NavGraph or Activity)            │
│   → Repository with Flow<> from Room or network                     │
│   → DataStore (persistent key-value, Flow-based)                    │
└─────────────────────────────────────────────────────────────────────┘
```

---

### 🔷 Mental Model — Memorize This

```
User typed or selected something?
    → rememberSaveable

Simple UI flag (dialog, toggle)?
    → remember { mutableStateOf() }

Screen state (loading, data, errors)?
    → ViewModel + StateFlow

Something happened once (navigate, toast)?
    → ViewModel + SharedFlow

State must survive being killed by OS?
    → UI level:       rememberSaveable
    → Business level: ViewModel + SavedStateHandle

Compute from other state?
    → derivedStateOf

Observable list/map in UI?
    → mutableStateListOf / mutableStateMapOf

Data from database / network?
    → Flow → stateIn → StateFlow in ViewModel

Reading state in composable (Android)?
    → collectAsStateWithLifecycle()

Reading state in composable (KMP)?
    → collectAsState()
```

---

## 📌 Final Summary Table

|Tool|Library|KMP|Survives Rotation|Survives Process Death|Scope|
|---|---|---|---|---|---|
|`mutableStateOf`|compose-runtime|✅|❌|❌|Inline|
|`remember`|compose-runtime|✅|❌|❌|Composable|
|`rememberSaveable`|compose-runtime-saveable|❌ Android|✅|✅|Composable|
|`rememberSerializable`|lifecycle-runtime-compose|✅|✅|✅|Composable|
|`derivedStateOf`|compose-runtime|✅|❌|❌|Composable|
|`mutableStateListOf`|compose-runtime|✅|❌|❌|Composable|
|`mutableStateMapOf`|compose-runtime|✅|❌|❌|Composable|
|`Flow`|kotlinx-coroutines-core|✅|N/A (stream)|N/A|Repository|
|`StateFlow`|kotlinx-coroutines-core|✅|✅ (via VM)|❌|ViewModel|
|`MutableStateFlow`|kotlinx-coroutines-core|✅|✅ (via VM)|❌|ViewModel|
|`SharedFlow`|kotlinx-coroutines-core|✅|✅ (via VM)|❌|ViewModel|
|`ViewModel`|lifecycle-viewmodel|✅ (JB)|✅|❌|Activity/Screen|
|`SavedStateHandle`|lifecycle-viewmodel-savedstate|✅ (JB 2.9+)|✅|✅|ViewModel|
|`LiveData`|lifecycle-livedata|❌ Android|✅ (via VM)|❌|ViewModel|
|`collectAsStateWithLifecycle`|lifecycle-runtime-compose|✅ (JB)|N/A|N/A|Composable|
|`collectAsState`|compose-runtime|✅|N/A|N/A|Composable|

> **JB = JetBrains** KMP lifecycle library (`org.jetbrains.androidx.lifecycle:*`) **Android = AndroidX** — Android platform only

---

> 📝 **Version Note:** All dependency versions shown are current as of early 2026. Always verify at [developer.android.com/jetpack/androidx/releases](https://developer.android.com/jetpack/androidx/releases) before adding to your project. For KMP lifecycle, check [github.com/JetBrains/compose-multiplatform](https://github.com/JetBrains/compose-multiplatform).

> 📝 **KMP Note:** When building for multiple platforms, always use JetBrains lifecycle libraries (`org.jetbrains.androidx.lifecycle:*`) in `commonMain` — not AndroidX lifecycle. AndroidX lifecycle is Android-only.