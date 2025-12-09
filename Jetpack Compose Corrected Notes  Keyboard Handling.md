# Jetpack Compose: Corrected Notes & Keyboard Handling

---

## ✅ CORRECTIONS TO YOUR NOTES

### Correction 1: What `@Composable` Returns

**❌ Your Note Said:**

> "A `@Composable` function returns UI instructions that Compose stores in an internal slot table."

**✅ Corrected Understanding:** `@Composable` functions actually **return `Unit`**, not "UI instructions." The function body contains **imperative calls** that emit UI tree nodes during the composition phase. The slot table stores the **state and metadata**, not instructions—the actual UI tree is built in memory during execution.

**Why This Matters:**

- You can't capture the "return value" of a Composable
- Composables are procedural builders, not declaration expressions
- The UI tree is built by the _effect_ of function execution, not its return type

**Example:**

```kotlin
@Composable
fun MyScreen() {  // Returns Unit, not UI
    Column {       // This is an imperative call
        Text("Hello")  // Text() also returns Unit
        Button(onClick = {}) {
            Text("Click")
        }
    }
}
```

---

### Correction 2: Modifier Order Matters (Critical!)

**❌ Common Mistake:** Many notes treat modifiers as unordered. They're not.

**✅ Correct Understanding:** Modifiers are **applied left-to-right as a chain**. Order fundamentally changes behavior.

**Examples of Order Mattering:**

```kotlin
// WRONG ORDER - Size first, then padding
Text(
    "Hello",
    modifier = Modifier
        .size(100.dp)           // Text is 100x100
        .padding(16.dp)         // Padding is OUTSIDE the 100x100
)

// CORRECT ORDER - Padding first, then size
Text(
    "Hello",
    modifier = Modifier
        .padding(16.dp)         // Add padding to content
        .size(100.dp)           // Then constrain to 100x100
)

// BACKGROUND + PADDING ORDER
Text(
    "Hello",
    modifier = Modifier
        .background(Color.Blue)
        .padding(16.dp)
    // Result: Blue background BEHIND padding
)

// VS

Text(
    "Hello",
    modifier = Modifier
        .padding(16.dp)
        .background(Color.Blue)
    // Result: Blue background wraps the padded area
)
```

**Key Rule:** Read modifiers **right-to-left for visual effect**. The modifier closest to the composable applies first.

---

### Correction 3: `remember` is NOT Just Memory

**❌ Your Note Said:**

> "`remember` stores values across recompositions but resets when the Composable leaves composition."

**✅ Complete Picture:** `remember` does more than store—it ties values to **composition identity**. This means:

- Values persist as long as the **key identity** remains in composition
- Leaving and re-entering with the same `key` restores the value
- Identity is based on **Composable position in the tree** (not name)
- The `key()` parameter explicitly controls identity

**Example:**

```kotlin
// Without explicit key - identity based on position
Column {
    if (showScreen1) {
        RememberedScreen1()  // Remembers values at position 1
    } else {
        RememberedScreen2()  // Occupies same position 1, loses Screen1's state
    }
}

// With explicit key - state preserved even if position changes
Column {
    if (showScreen1) {
        key("screen1") {
            RememberedScreen1()  // State tied to "screen1" key
        }
    } else {
        key("screen2") {
            RememberedScreen2()  // State tied to "screen2" key
        }
    }
}

@Composable
fun RememberedScreen1() {
    var count by remember { mutableStateOf(0) }  // Tied to "screen1" key
    Button(onClick = { count++ }) {
        Text("Count: $count")
    }
}
```

---

### Correction 4: LazyColumn vs Column Layout Behavior

**❌ Oversimplification:**

> "LazyColumn is like RecyclerView."

**✅ Important Nuance:** LazyColumn does lazy composition AND lazy layout measurement. This has side effects:

- **No intrinsic size calculation** - can't calculate child sizes until they're composed
- **Affects parent arrangement** - parent can't know total height
- **No `.size()` on LazyColumn** works like you'd expect
- **Keys matter for item identity** - without keys, state can mix between items

**Critical Example:**

```kotlin
// DON'T DO THIS - Each item loses state on scroll
LazyColumn {
    items(1000) { index ->
        var expanded by remember { mutableStateOf(false) }
        ExpandableItem(expanded, index)  // State resets on recomposition
    }
}

// DO THIS - State tied to item identity
LazyColumn {
    items(
        count = 1000,
        key = { index -> index }  // Explicit key for item identity
    ) { index ->
        var expanded by remember { mutableStateOf(false) }
        ExpandableItem(expanded, index)  // State preserved correctly
    }
}
```

---

### Correction 5: Button vs FilledButton vs ElevatedButton

**❌ Your Notes Didn't Clarify:** Material 3 has multiple button types for different use cases, not just styling differences.

**✅ Proper Distinction:**

|Button Type|Emphasis|Use Case|Elevation|
|---|---|---|---|
|`Button` (filled)|High|Primary action on screen|0dp (filled)|
|`FilledButton`|High|Primary action (explicit Material 3)|0dp (filled)|
|`FilledTonalButton`|Medium|Secondary action with less contrast|0dp (filled tonal)|
|`OutlinedButton`|Low|Alternative action, cancel|0dp (border)|
|`ElevatedButton`|High|Primary on surface (uses elevation instead of color)|4dp+ (shadow)|
|`TextButton`|Minimal|Tertiary action, dialogs|0dp (text only)|

**Usage Example:**

```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    // Primary action
    Button(
        onClick = { onSave() },
        modifier = Modifier.weight(1f)
    ) { Text("Save") }
    
    // Alternative action
    OutlinedButton(
        onClick = { onCancel() },
        modifier = Modifier.weight(1f)
    ) { Text("Cancel") }
}
```

---

### Correction 6: Column/Row `weight` Modifier

**❌ Common Misunderstanding:** `weight` doesn't work outside Row/Column contexts.

**✅ Correct Usage:** `weight` distributes **remaining space** proportionally among siblings in Row or Column.

```kotlin
Row(modifier = Modifier.fillMaxWidth()) {
    Text("Left", modifier = Modifier.weight(1f))   // Takes 50%
    Text("Right", modifier = Modifier.weight(1f))  // Takes 50%
}

Row(modifier = Modifier.fillMaxWidth()) {
    Text("Left", modifier = Modifier.weight(1f))    // Takes 66%
    Text("Right", modifier = Modifier.weight(2f))   // Takes 33%
}

// This doesn't work - weight ignored
Box(modifier = Modifier.weight(1f)) {  // ❌ No effect outside Row/Column
    Text("This won't expand")
}
```

---

## 📱 KEYBOARD HANDLING IN JETPACK COMPOSE

Keyboard handling is crucial for TextField-heavy screens. Here's the complete guide:

---

### 1. **Basic Keyboard Type Configuration**

```kotlin
@Composable
fun KeyboardTypeExample() {
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,  // Shows @ and . keys
            imeAction = ImeAction.Next          // "Next" button instead of "Done"
        ),
        keyboardActions = KeyboardActions(
            onNext = { /* Focus next field */ }
        )
    )

    OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("Phone") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,  // Shows numeric keypad
            imeAction = ImeAction.Done
        )
    )

    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { /* Submit form */ }
        )
    )

    OutlinedTextField(
        value = number,
        onValueChange = { number = it },
        label = { Text("Number") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        )
    )
}
```

**KeyboardType Options:**

- `Text` - Standard text keyboard
- `Email` - Shows @ and . symbols
- `Phone` - Numeric keypad with +, -, ()
- `Number` - Numbers only
- `Password` - Hides characters
- `NumberPassword` - Numbers only, hidden
- `Decimal` - Numbers with decimal point
- `Uri` - Shows / and . for URLs
- `Ascii` - ASCII characters only

**ImeAction Options:**

- `Done` - "Done" button
- `Next` - "Next" button for multi-field forms
- `Go` - "Go" button
- `Search` - "Search" button
- `Send` - "Send" button
- `Previous` - Navigate to previous field

---

### 2. **Prevent Keyboard Overlap with Scrolling**

**Problem:** Keyboard covers content when it appears.

**Solution: Use `imePadding()` Modifier**

```kotlin
@Composable
fun KeyboardAvoidingForm() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()  // 🔑 KEY: Adds padding when keyboard appears
    ) {
        item {
            Text("Registration Form", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        
        item {
            Spacer(Modifier.height(24.dp))
        }
        
        item {
            var email by remember { mutableStateOf("") }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )
        }
        
        item {
            Spacer(Modifier.height(16.dp))
        }
        
        item {
            var password by remember { mutableStateOf("") }
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )
        }
        
        item {
            Spacer(Modifier.height(32.dp))
        }
        
        item {
            Button(
                onClick = { /* Submit */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Up")
            }
        }
    }
}
```

**How `imePadding()` Works:**

- Automatically detects keyboard visibility
- Adds bottom padding equal to keyboard height
- Composable scrolls content into view
- Removes padding when keyboard closes

---

### 3. **Manual Keyboard Control**

```kotlin
@Composable
fun ManualKeyboardControl() {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var email by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()  // 🔑 Hide keyboard
                    focusManager.clearFocus()   // Remove focus
                }
            )
        )

        Button(
            onClick = {
                keyboardController?.hide()  // Programmatically hide keyboard
            }
        ) {
            Text("Hide Keyboard")
        }
    }
}
```

**Key Components:**

- `LocalSoftwareKeyboardController.current` - Get keyboard controller
- `keyboardController?.hide()` - Hide keyboard
- `LocalFocusManager.current` - Manage focus
- `focusManager.clearFocus()` - Remove focus from all fields

---

### 4. **Complete Form Example with Keyboard Handling**

```kotlin
@Composable
fun RegisterFormWithKeyboardHandling() {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()  // 🔑 Prevent keyboard overlap
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(Modifier.height(40.dp))
        }

        item {
            Text(
                text = "Create Account",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Spacer(Modifier.height(32.dp))
        }

        item {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next  // Go to next field
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true
            )
        }

        item {
            Spacer(Modifier.height(12.dp))
        }

        item {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true
            )
        }

        item {
            Spacer(Modifier.height(12.dp))
        }

        item {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true
            )
        }

        item {
            Spacer(Modifier.height(12.dp))
        }

        item {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done  // Last field
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()  // Hide keyboard after last field
                        focusManager.clearFocus()
                    }
                ),
                singleLine = true
            )
        }

        item {
            Spacer(Modifier.height(32.dp))
        }

        item {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    // Perform registration
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sign Up")
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
        }
    }
}
```

---

### 5. **Keyboard Visibility State Tracking**

```kotlin
@Composable
fun KeyboardVisibilityExample() {
    // Detect keyboard visibility
    val view = LocalView.current
    var isKeyboardVisible by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = ViewTreeObserver.OnPreDrawListener {
            val isVisible = ViewCompat.getRootWindowInsets(view)
                ?.isVisible(WindowInsetsCompat.Type.ime()) ?: false
            isKeyboardVisible = isVisible
            true
        }
        view.viewTreeObserver.addOnPreDrawListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnPreDrawListener(listener)
        }
    }

    Column {
        if (isKeyboardVisible) {
            Text("Keyboard is visible", color = Color.Green)
        } else {
            Text("Keyboard is hidden", color = Color.Red)
        }
    }
}
```

---

### 6. **Best Practices Checklist**

✅ **DO:**

- Use `imePadding()` on main scrollable container
- Set appropriate `keyboardType` for each field
- Use `ImeAction.Next` to navigate between fields
- Use `ImeAction.Done` on last field
- Clear focus when hiding keyboard
- Provide visual feedback for keyboard state

❌ **DON'T:**

- Leave keyboard covering important content
- Use generic `Text` keyboard type for email/phone
- Forget to set `singleLine = true` on single-line fields
- Use `focusRequester` without understanding composition lifecycle
- Assume keyboard state without checking

---

### 7. **Accessibility: Keyboard Navigation**

```kotlin
@Composable
fun AccessibleForm() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val passwordFocus = remember { FocusRequester() }
    val submitFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(24.dp)
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .focusProperties {
                    next = passwordFocus  // Set next focus target
                },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { passwordFocus.requestFocus() }
            )
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocus)
                .focusProperties {
                    previous = remember { FocusRequester() }  // Link to previous
                    next = submitFocus
                },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { submitFocus.requestFocus() }
            )
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { /* Submit */ },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(submitFocus)
        ) {
            Text("Sign In")
        }
    }
}
```

---

## 📋 **Quick Reference Table**

|Scenario|Solution|Code|
|---|---|---|
|Keyboard covers content|Use `imePadding()`|`Modifier.imePadding()`|
|Hide keyboard on action|Use controller|`keyboardController?.hide()`|
|Navigate between fields|Use ImeAction|`imeAction = ImeAction.Next`|
|Detect keyboard state|Use ViewTreeObserver|See example above|
|Clear focus|Use focusManager|`focusManager.clearFocus()`|
|Move focus direction|Use focusManager|`focusManager.moveFocus(FocusDirection.Down)`|

---

## 🎯 **Summary**

1. **Always use `imePadding()`** on scrollable containers to prevent keyboard overlap
2. **Set proper keyboard types** to improve UX and reduce typing errors
3. **Navigate between fields** using `ImeAction.Next` and focus management
4. **Hide keyboard when done** using `keyboardController?.hide()`
5. **Test on different screen sizes** - keyboard behavior varies by device
   
   # 🚀 Jetpack Compose: Complete Advanced UI Guide
## From Basics to Production-Ready (Fast Track)

---

# 📖 TABLE OF CONTENTS
1. [Core Concepts (Corrected)](#core-concepts)
2. [Layout System (Deep Dive)](#layout-system)
3. [State Management (Advanced)](#state-management)
4. [Keyboard & Input Handling](#keyboard-handling)
5. [Styling & Theme System](#styling-theme)
6. [Lists & Performance](#lists-performance)
7. [Animations & Motion](#animations)
8. [Navigation & Routing](#navigation)
9. [Advanced Patterns](#advanced-patterns)
10. [Production Checklist](#production-checklist)

---

# 🎯 PART 1: CORE CONCEPTS (CORRECTED)
<a name="core-concepts"></a>

## ✅ Correction 1: What `@Composable` Actually Is

### The Technical Reality

`@Composable` is a **compiler-level marker** that instructs the Compose compiler to:
- Track recomposition scope
- Generate slot table entries
- Manage composition memory

**Critical Facts:**
- Returns `Unit` (not UI)
- Executes **every recomposition** (design for it!)
- The UI tree is built by **function side effects**, not return values
- Can be called multiple times during composition

```kotlin
// This executes 3 times if state changes
@Composable
fun CounterScreen() {
    var count by remember { mutableStateOf(0) }
    
    // THIS RUNS ON EVERY RECOMPOSITION
    println("CounterScreen recomposed, count=$count")
    
    Column {
        Text("Count: $count")
        Button(onClick = { count++ }) {
            Text("Increment")
        }
    }
}

// OUTPUT:
// CounterScreen recomposed, count=0
// CounterScreen recomposed, count=1
// CounterScreen recomposed, count=2
// etc...
```

### Important Implications

```kotlin
// ❌ DON'T DO THIS - Runs on every recomposition
@Composable
fun BadExample() {
    var items by remember { mutableStateOf(emptyList<String>()) }
    
    // BAD: API call on every recomposition
    val data = fetchDataFromServer()  // SLOW!
    
    LazyColumn {
        items(data) { item ->
            Text(item)
        }
    }
}

// ✅ DO THIS - Run side effect only once
@Composable
fun GoodExample() {
    var items by remember { mutableStateOf(emptyList<String>()) }
    
    // GOOD: API call only on mount
    LaunchedEffect(Unit) {
        items = fetchDataFromServer()
    }
    
    LazyColumn {
        items(items) { item ->
            Text(item)
        }
    }
}
```

---

## ✅ Correction 2: Modifier Chain Order (CRITICAL!)

### Why Order Matters

Modifiers form a **linked list** executed left-to-right. Each modifier wraps the previous one.

```kotlin
// Think of it like function composition
// Modifier.A.B.C is really: C(B(A(content)))

Box(
    modifier = Modifier
        .background(Color.Red)      // Applied FOURTH
        .padding(16.dp)             // Applied THIRD
        .fillMaxWidth()             // Applied SECOND
        .height(100.dp)             // Applied FIRST
)

// Execution order: height → fillMaxWidth → padding → background
```

### Visual Examples

**Example 1: Size vs Padding**

```kotlin
// ❌ WRONG - Padding extends beyond box
Box(
    modifier = Modifier
        .size(100.dp)           // Box is 100x100
        .padding(16.dp)         // 16dp padding OUTSIDE the box
        .background(Color.Blue)
) {
    Text("Wrong")
}

// ✅ CORRECT - Padding inside box
Box(
    modifier = Modifier
        .padding(16.dp)         // Apply padding first
        .size(100.dp)           // Then constrain to 100x100
        .background(Color.Blue)
) {
    Text("Correct")
}
```

**Example 2: Background vs Padding**

```kotlin
// Results in different visual effects:

// Background BEFORE padding
Text(
    "A",
    modifier = Modifier
        .background(Color.Blue)
        .padding(16.dp)
)
// Result: Text with 16dp padding, blue background behind text only

// Background AFTER padding
Text(
    "A",
    modifier = Modifier
        .padding(16.dp)
        .background(Color.Blue)
)
// Result: Text with blue background including 16dp padded area
```

**Example 3: Click Effects Matter**

```kotlin
// ❌ WRONG - Click area doesn't include padding
Button(
    onClick = { },
    modifier = Modifier
        .size(100.dp)
        .padding(16.dp)  // Makes clickable area SMALLER
)

// ✅ CORRECT - Click area includes padding
Button(
    onClick = { },
    modifier = Modifier
        .padding(16.dp)
        .size(100.dp)  // Full 100dp is clickable
)
```

### Remember This Order

```
Size modifiers first → Position modifiers → Visual modifiers
.size() → .padding(), .offset() → .background(), .border()
```

---

## ✅ Correction 3: `remember` & Composition Identity

### How `remember` Actually Works

`remember` doesn't just "store values"—it ties them to **composition slot identity**.

```kotlin
@Composable
fun StatefulForm() {
    var email by remember { mutableStateOf("") }
    // ↑ This value is stored in a COMPOSITION SLOT
    // Slot is identified by:
    // 1. Composable's position in tree
    // 2. Optional key() parameter
    // 3. The order it's declared
}
```

### The Real Gotcha: Position-Based Identity

```kotlin
// ❌ PROBLEM: State lives at POSITION, not NAME

@Composable
fun Screen(showForm: Boolean) {
    if (showForm) {
        EmailForm()      // Position 1
    } else {
        NameForm()       // Also position 1!
    }
}

// When you toggle showForm:
// - EmailForm remembers values at position 1
// - NameForm gets position 1 too
// - RESULT: NameForm can see EmailForm's remembered state!

// ✅ SOLUTION: Use explicit keys

@Composable
fun FixedScreen(showForm: Boolean) {
    if (showForm) {
        key("email") {   // Tie to "email" key
            EmailForm()
        }
    } else {
        key("name") {    // Tie to "name" key
            NameForm()
        }
    }
}
```

### Composition Lifetime Visualization

```kotlin
@Composable
fun ScreenExample() {
    var items by remember { mutableStateOf(listOf(1, 2, 3)) }
    
    Column {
        items.forEach { item ->
            // Each item gets its own remember slot
            var expanded by remember { mutableStateOf(false) }
            ExpandableItem(item, expanded)
        }
    }
}

// Timeline:
// First compose: items=[1,2,3]
//   Slot 1: expanded=false (item=1)
//   Slot 2: expanded=false (item=2)
//   Slot 3: expanded=false (item=3)
//
// User expands item 2: expanded=true
//   Slot 1: expanded=false (item=1)
//   Slot 2: expanded=true  (item=2) ← Updated
//   Slot 3: expanded=false (item=3)
//
// Remove item 1 (items=[2,3]):
//   Slot 1: expanded=true  (item=2) ← WRONG! Now has old slot 2 state!
//   Slot 2: expanded=false (item=3)
//
// FIX: Use LazyColumn with keys!
```

---

## ✅ Correction 4: `LazyColumn` Gotchas

### Problem 1: No Intrinsic Size

```kotlin
// ❌ DOESN'T WORK - LazyColumn can't measure all children
Column {
    LazyColumn(
        modifier = Modifier.fillMaxHeight()  // Fills parent
    ) {
        items(1000) { Text("Item $it") }
    }
    // Parent doesn't know LazyColumn's height!
}

// ✅ WORKS - Give LazyColumn explicit size
Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(1000) { Text("Item $it") }
    }
}
```

### Problem 2: State Loss Without Keys

```kotlin
// ❌ BAD - State mixes on recompose/scroll
LazyColumn {
    items(100) { index ->
        var isSelected by remember { mutableStateOf(false) }
        ListItem(index, isSelected, onSelect = { isSelected = !isSelected })
    }
}

// When you scroll:
// - Item 5 leaves composition
// - Item 105 enters composition at same slot
// - Item 105 gets item 5's "isSelected" state!

// ✅ GOOD - Explicit keys preserve state
LazyColumn {
    items(
        count = 100,
        key = { index -> index }  // CRITICAL!
    ) { index ->
        var isSelected by remember { mutableStateOf(false) }
        ListItem(index, isSelected)
    }
}

// ✅ BEST - Use data object as key
data class User(val id: Int, val name: String)

LazyColumn {
    items(
        items = users,
        key = { user -> user.id }  // Use unique ID
    ) { user ->
        var isSelected by remember { mutableStateOf(false) }
        UserItem(user, isSelected)
    }
}
```

### Problem 3: LazyColumn Performance

```kotlin
// ❌ SLOW - Recomposes whole list on state change
@Composable
fun SlowList() {
    var selectedId by remember { mutableStateOf(-1) }
    
    LazyColumn {
        items(1000, key = { it }) { index ->
            // When selectedId changes, ALL items recompose!
            ListItem(index, isSelected = selectedId == index)
        }
    }
}

// ✅ FAST - Lift selection state to item level
@Composable
fun FastList() {
    LazyColumn {
        items(1000, key = { it }) { index ->
            // Each item manages its own state
            ListItemWithLocalState(index)
        }
    }
}

@Composable
fun ListItemWithLocalState(index: Int) {
    var isSelected by remember { mutableStateOf(false) }
    // Only THIS item recomposes when toggled
    ListItem(index, isSelected) { isSelected = !isSelected }
}
```

---

# 🎨 PART 2: LAYOUT SYSTEM (DEEP DIVE)
<a name="layout-system"></a>

## Understanding Compose Layout

Compose uses a **3-pass layout algorithm**:
1. **Measure** - Determine size of each component
2. **Place** - Position components based on parent layout
3. **Draw** - Render to canvas

```kotlin
// When you write:
Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Text("Title")
    Spacer(Modifier.height(16.dp))
    Text("Content")
}

// Internally:
// PASS 1 (MEASURE):
//   - Measure Text("Title") → ~30.dp high
//   - Measure Spacer → 16.dp high
//   - Measure Text("Content") → ~30.dp high
//   - Total: 76.dp high

// PASS 2 (PLACE):
//   - Place Text at y=0, centered x
//   - Place Spacer at y=30
//   - Place Text at y=46, centered x
//   - Column total: 76.dp

// PASS 3 (DRAW):
//   - Draw all elements
```

---

## Column Layout in Detail

```kotlin
@Composable
fun ColumnLayoutMastery() {
    // ARRANGEMENT: How children are distributed along vertical axis
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly,  // Distribute with equal space
        horizontalAlignment = Alignment.CenterHorizontally  // Align horizontally
    ) {
        Text("Item 1")
        Text("Item 2")
        Text("Item 3")
    }
}

// Arrangement.* options:
// - Top → Items at top
// - Center → Items centered
// - Bottom → Items at bottom
// - SpaceBetween → Space between items only
// - SpaceAround → Equal space around each item
// - SpaceEvenly → Equal space everywhere
// - spacedBy(16.dp) → Fixed 16dp between items

// Alignment.Horizontal options:
// - Start → Left (RTL: Right)
// - CenterHorizontally → Center
// - End → Right (RTL: Left)
```

---

## Row Layout in Detail

```kotlin
@Composable
fun RowLayoutMastery() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Left")
        Text("Center")
        Text("Right")
    }
}

// Weight Distribution in Row:
Row(modifier = Modifier.fillMaxWidth()) {
    Text("A", modifier = Modifier.weight(1f))    // 50%
    Text("B", modifier = Modifier.weight(1f))    // 50%
}

Row(modifier = Modifier.fillMaxWidth()) {
    Text("A", modifier = Modifier.weight(2f))    // 66%
    Text("B", modifier = Modifier.weight(1f))    // 33%
}

Row(modifier = Modifier.fillMaxWidth()) {
    Text("A", modifier = Modifier.weight(1f))    // Takes remaining space
    Text("B", modifier = Modifier.wrapContentWidth())  // Natural size
    Text("C", modifier = Modifier.wrapContentWidth())  // Natural size
}
```

---

## Box (Overlay/Stack Layout)

```kotlin
@Composable
fun BoxLayoutMastery() {
    // Box stacks children - last one on top
    Box(
        modifier = Modifier
            .size(200.dp)
            .background(Color.Blue),
        contentAlignment = Alignment.Center  // Where unaligned children go
    ) {
        // This is behind
        Image(
            painter = painterResource(R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        
        // This is on top
        Text(
            "Overlay",
            modifier = Modifier.align(Alignment.TopEnd),  // Override contentAlignment
            color = Color.White
        )
    }
}

// Alignment options in Box:
// TopStart, TopCenter, TopEnd
// CenterStart, Center, CenterEnd
// BottomStart, BottomCenter, BottomEnd
```

---

## Advanced: Custom Layout

```kotlin
@Composable
fun CustomLayoutExample() {
    Layout(
        content = {
            repeat(5) { index ->
                Text("Item $index", modifier = Modifier.padding(8.dp))
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) { measurables, constraints ->
        // MEASURE: Get size of each child
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }
        
        // PLACE: Calculate layout
        layout(constraints.maxWidth, 200) {
            var yPosition = 0
            placeables.forEach { placeable ->
                placeable.place(x = 0, y = yPosition)
                yPosition += placeable.height
            }
        }
    }
}
```

---

## Constraint Modifiers (Most Important!)

```kotlin
@Composable
fun ConstraintModifiers() {
    Column(modifier = Modifier.fillMaxSize()) {
        // SIZE CONSTRAINTS
        Box(modifier = Modifier.size(100.dp).background(Color.Red)) // 100x100
        Box(modifier = Modifier.width(100.dp).height(50.dp)) // 100x50
        Box(modifier = Modifier.fillMaxWidth()) // Match parent width
        Box(modifier = Modifier.fillMaxHeight()) // Match parent height
        Box(modifier = Modifier.fillMaxSize()) // Fill entire parent
        
        // WRAP & BOUND
        Box(modifier = Modifier.wrapContentSize()) // Size to content
        Box(modifier = Modifier.wrapContentWidth()) // Width to content
        Box(modifier = Modifier.wrapContentHeight()) // Height to content
        
        // SIZE RANGE
        Box(modifier = Modifier.sizeIn(minWidth = 50.dp, maxWidth = 200.dp))
        Box(modifier = Modifier.heightIn(min = 40.dp, max = 100.dp))
        
        // ASPECT RATIO
        Box(modifier = Modifier.aspectRatio(16f / 9f)) // 16:9 ratio
    }
}
```

---

# 💾 PART 3: STATE MANAGEMENT (ADVANCED)
<a name="state-management"></a>

## State Hierarchy

```kotlin
// LEVEL 1: Composition State (Stateless function)
@Composable
fun StatelessButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text)
    }
}

// LEVEL 2: Local State (remember)
@Composable
fun ClickCounter() {
    var count by remember { mutableStateOf(0) }
    Button(onClick = { count++ }) {
        Text("Clicks: $count")
    }
}

// LEVEL 3: Lifted State (state hoisting)
@Composable
fun ParentScreen() {
    var count by remember { mutableStateOf(0) }
    
    ChildCounter(
        count = count,
        onIncrement = { count++ }
    )
}

@Composable
fun ChildCounter(count: Int, onIncrement: () -> Unit) {
    Button(onClick = onIncrement) {
        Text("Clicks: $count")
    }
}

// LEVEL 4: ViewModel State (persistent)
@Composable
fun MVVMScreen(viewModel: MyViewModel = viewModel()) {
    val count by viewModel.count.collectAsState()
    
    Button(onClick = { viewModel.increment() }) {
        Text("Clicks: $count")
    }
}
```

---

## State Patterns

### Pattern 1: Derived State

```kotlin
@Composable
fun DerivedStateExample() {
    var count by remember { mutableStateOf(0) }
    
    // ❌ BAD - Recomputes on every recomposition
    val isEven = count % 2 == 0
    
    // ✅ GOOD - Only updates when count changes
    val isEvenDerived = remember(count) {
        count % 2 == 0
    }
    
    // ✅ BEST - For complex calculations
    val isEvenComputed = derivedStateOf {
        count % 2 == 0
    }
    
    Button(onClick = { count++ }) {
        Text("Count: $count, Even: ${isEvenComputed.value}")
    }
}
```

### Pattern 2: Complex State Objects

```kotlin
data class FormState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@Composable
fun FormScreen() {
    var formState by remember { 
        mutableStateOf(FormState())
    }
    
    OutlinedTextField(
        value = formState.email,
        onValueChange = { newEmail ->
            formState = formState.copy(email = newEmail)
        }
    )
    
    if (formState.isLoading) {
        CircularProgressIndicator()
    }
    
    formState.error?.let {
        Text(it, color = Color.Red)
    }
}
```

### Pattern 3: SnapshotStateList (Collections)

```kotlin
@Composable
fun TodoListApp() {
    // ✅ Use SnapshotStateList for lists
    val todos = remember { mutableStateListOf<String>() }
    
    LazyColumn {
        items(todos.size, key = { todos[it].hashCode() }) { index ->
            TodoItem(
                text = todos[index],
                onDelete = { todos.removeAt(index) },
                onEdit = { newText -> todos[index] = newText }
            )
        }
    }
    
    Button(onClick = {
        todos.add("New Todo ${todos.size + 1}")
    }) {
        Text("Add Todo")
    }
}
```

---

# 📱 PART 4: KEYBOARD & INPUT HANDLING
<a name="keyboard-handling"></a>

## Complete Keyboard System

### Section 4.1: Keyboard Types & Actions

```kotlin
@Composable
fun KeyboardComprehensive() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var zipCode by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    autoCorrect = false,
                    capitalization = KeyboardCapitalization.None
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true
            )
        }
        
        item {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone (+1234567890)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true
            )
        }
        
        item {
            OutlinedTextField(
                value = zipCode,
                onValueChange = { zipCode = it },
                label = { Text("Zip Code") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true
            )
        }
        
        item {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                prefix = { Text("$") }
            )
        }
        
        item {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    autoCorrect = false,
                    capitalization = KeyboardCapitalization.None
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        // Submit form here
                    }
                ),
                singleLine = true
            )
        }
        
        item {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit")
            }
        }
    }
}
```

### Section 4.2: Advanced Keyboard Control

```kotlin
@Composable
fun AdvancedKeyboardControl() {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val view = LocalView.current
    
    var isKeyboardVisible by remember { mutableStateOf(false) }
    var focusedFieldId by remember { mutableStateOf("") }
    
    // Detect keyboard visibility
    DisposableEffect(Unit) {
        val listener = ViewTreeObserver.OnPreDrawListener {
            val insets = ViewCompat.getRootWindowInsets(view)
            isKeyboardVisible = insets?.isVisible(WindowInsetsCompat.Type.ime()) ?: false
            true
        }
        view.viewTreeObserver.addOnPreDrawListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnPreDrawListener(listener)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = "",
            onValueChange = { },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    focusedFieldId = if (focusState.isFocused) "email" else ""
                },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        
        Spacer(Modifier.height(24.dp))
        
        if (isKeyboardVisible) {
            Text("Keyboard is visible", color = Color.Green, fontSize = 12.sp)
        }
        
        if (focusedFieldId.isNotEmpty()) {
            Text("Focused: $focusedFieldId", color = Color.Blue, fontSize = 12.sp)
        }
    }
}
```

### Section 4.3: Input Validation with Keyboard

```kotlin
@Composable
fun ValidatedFormScreen() {
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    
    val validateEmail = { value: String ->
        emailError = when {
            value.isEmpty() -> "Email cannot be empty"
            !value.contains("@") -> "Invalid email format"
            !value.contains(".") -> "Invalid email domain"
            else -> null
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = email,
                onValueChange = { newValue ->
                    email = newValue
                    validateEmail(newValue)
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                isError = emailError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        validateEmail(email)
                    }
                )
            )
        }
        
        if (emailError != null) {
            item {
                Text(
                    emailError!!,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
```

---

# 🎨 PART 5: STYLING & THEME SYSTEM
<a name="styling-theme"></a>

## Material 3 Design System

```kotlin
// Define custom theme
private val LightColors = lightColorScheme(
    primary = Color(0xFF6200EE),
    secondary = Color(0xFF03DAC6),
    tertiary = Color(0xFF03DAC6),
    surface = Color(0xFFFFFFFF),
    background = Color(0xFFFAFAFA),
    error = Color(0xFFB00020)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBB86FC),
    secondary = Color(0xFF03DAC6),
    tertiary = Color(0xFF03DAC6),
    surface = Color(0xFF121212),
    background = Color(0xFF121212),
    error = Color(0xFFCF6679)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(
            displayLarge = TextStyle(
                fontSize = 57.sp,
                lineHeight = 64.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.25).sp
            ),
            headlineSmall = TextStyle(
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold
            ),
            bodyMedium = TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal
            )
        ),
        shapes = Shapes(
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(16.dp)
        ),
        content = content
    )
}
```

### Using Theme

```kotlin
@Composable
fun ThemedScreen() {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
    ) {
        Text(
            "Themed Title",
            style = typography.headlineSmall,
            color = colors.primary
        )
        
        Button(
            onClick = { },
            shape = shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = Color.White
            )
        ) {
            Text("Themed Button")
        }
    }
}
```

---

# 📜 PART 6: LISTS & PERFORMANCE
<a name="lists-performance"></a>

## LazyColumn Performance Optimization

```kotlin
@Composable
fun OptimizedLazyList(items: List<String>) {
    // Store derived state to avoid unnecessary recompositions
    val listState = rememberLazyListState()
    
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = items.size,
            key = { index -> items[index] },  // CRITICAL for performance
            contentType = { "listItem" }       // Hint for reuse
        ) { index ->
            ListItem(items[index])
        }
    }
}

@Composable
fun ListItem(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(text, modifier = Modifier.padding(16.dp))
    }
}
```

### Infinite Scroll Example

```kotlin
@Composable
fun InfiniteScrollList(
    loadMore: suspend () -> List<String>
) {
    var items by remember { mutableStateOf(emptyList<String>()) }
    var isLoading by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    
    LaunchedEffect(Unit) {
        items = loadMore()
    }
    
    // Load more when reaching bottom
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && 
                    lastVisibleIndex >= items.size - 5 && 
                    !isLoading) {
                    isLoading = true
                    items = items + loadMore()
                    isLoading = false
                }
            }
    }
    
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(items.size, key = { items[it].hashCode() }) { index ->
            ListItem(items[index])
        }
        
        if (isLoading) {
            item {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}
```

### Grid Layout

```kotlin
@Composable
fun GridScreen() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),  // 2 columns
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(50) { index ->
            GridItem(index)
        }
    }
}

@Composable
fun GridItem(index: Int) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .background(Color(0xFF6200EE).copy(alpha = 0.3f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("Item $index", fontWeight = FontWeight.Bold)
        }
    }
}
```

---

# ⚡ PART 7: ANIMATIONS & MOTION
<a name="animations"></a>

## Basic Animations

```kotlin
@Composable
fun AnimationExamples() {
    var isExpanded by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(true) }
    
    // Animate size
    val animatedSize by animateDpAsState(
        targetValue = if (isExpanded) 200.dp else 100.dp,
        animationSpec = spring(dampingRatio = 0.7f)
    )
    
    // Animate color
    val animatedColor by animateColorAsState(
        targetValue = if (isExpanded) Color.Green else Color.Red,
        animationSpec = tween(durationMillis = 500)
    )
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box(
            modifier = Modifier
                .size(animatedSize)
                .background(animatedColor)
                .clickable { isExpanded = !isExpanded }
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Visibility animation
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally() + fadeIn(),
            exit = slideOutHorizontally() + fadeOut()
        ) {
            Text("Hello!")
        }
        
        Button(onClick = { isVisible = !isVisible }) {
            Text("Toggle Visibility")
        }
    }
}
```

## Complex Animations

```kotlin
@Composable
fun AdvancedAnimation() {
    var isExpanded by remember { mutableStateOf(false) }
    
    val transition = updateTransition(isExpanded, label = "expandTransition")
    
    val backgroundColor by transition.animateColor(label = "bgColor") { expanded ->
        if (expanded) Color.Green else Color.Red
    }
    
    val size by transition.animateDp(label = "size") { expanded ->
        if (expanded) 200.dp else 100.dp
    }
    
    val rotation by transition.animateFloat(label = "rotation") { expanded ->
        if (expanded) 360f else 0f
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box(
            modifier = Modifier
                .size(size)
                .background(backgroundColor)
                .rotate(rotation)
                .clickable { isExpanded = !isExpanded }
        )
    }
}
```

---

# 🗺️ PART 8: NAVIGATION & ROUTING
<a name="navigation"></a>

## Navigation Structure

```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Details : Screen("details/{id}") {
        fun createRoute(id: Int) = "details/$id"
    }
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToDetails = { id ->
                    navController.navigate(Screen.Details.createRoute(id))
                }
            )
        }
        
        composable(
            route = Screen.Details.route,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            DetailsScreen(id = id, onBack = { navController.popBackStack() })
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
```

---

# 🏗️ PART 9: ADVANCED PATTERNS
<a name="advanced-patterns"></a>

## Pattern 1: MVVM with Compose

```kotlin
class MyViewModel : ViewModel() {
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()
    
    fun loadData() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                val data = fetchData()
                _state.value = UiState.Success(data)
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class UiState {
    object Loading : UiState()
    data class Success(val data: String) : UiState()
    data class Error(val message: String) : UiState()
}

@Composable
fun ScreenWithViewModel(
    viewModel: MyViewModel = viewModel()
) {
    val uiState by viewModel.state.collectAsState()
    
    when (uiState) {
        is UiState.Loading -> {
            CircularProgressIndicator()
        }
        is UiState.Success -> {
            Text((uiState as UiState.Success).data)
        }
        is UiState.Error -> {
            Text("Error: ${(uiState as UiState.Error).message}", color = Color.Red)
        }
    }
}
```

## Pattern 2: Dependency Injection

```kotlin
// Use Hilt for DI
@HiltViewModel
class InjectedViewModel @Inject constructor(
    private val repository: UserRepository,
    private val analytics: Analytics
) : ViewModel() {
    // ViewModel logic
}

@Composable
fun ScreenWithHilt(
    viewModel: InjectedViewModel = hiltViewModel()
) {
    // Use viewModel
}
```

## Pattern 3: Effects & Side Effects

```kotlin
@Composable
fun SideEffectExamples() {
    val viewModel: MyViewModel = viewModel()
    
    // LaunchedEffect - Run once on composition
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }
    
    // DisposableEffect - Cleanup
    DisposableEffect(Unit) {
        val listener = object : Listener {
            override fun onEvent() { }
        }
        eventBus.register(listener)
        onDispose {
            eventBus.unregister(listener)
        }
    }
    
    // SideEffect - Every recomposition
    SideEffect {
        analytics.logScreenView("Screen")
    }
}
```

---

# ✅ PART 10: PRODUCTION CHECKLIST
<a name="production-checklist"></a>

## Pre-Launch Checklist

### Performance
- [ ] Use `key` in all LazyColumn/LazyRow
- [ ] Avoid state at top-level in lists
- [ ] Use `derivedStateOf` for computed values
- [ ] Profile with Layout Inspector
- [ ] No unnecessary recompositions
- [ ] Use `@Stable` for custom classes in compose

### Accessibility
- [ ] Add `contentDescription` to all Images
- [ ] Test with TalkBack
- [ ] Proper touch targets (48dp minimum)
- [ ] Color contrast meets WCAG AA
- [ ] Keyboard navigation works

### UI/UX
- [ ] Test on multiple screen sizes
- [ ] Keyboard doesn't cover content (use `imePadding()`)
- [ ] Smooth animations (not janky)
- [ ] Loading states visible
- [ ] Error states user-friendly
- [ ] Back button works correctly

### Testing
- [ ] Write Composable preview tests
- [ ] Unit test ViewModels
- [ ] Integration test navigation
- [ ] Test keyboard scenarios
- [ ] Test on real devices

### Code Quality
- [ ] No hardcoded strings (use resources)
- [ ] Consistent modifier ordering
- [ ] No trailing lambdas in lists
- [ ] ViewModels don't hold UI state
- [ ] Proper error handling
- [ ] Memory leak check

---

## Quick Reference: Common Patterns

```kotlin
// Pattern: State hoisting
@Composable
fun Parent() {
    var value by remember { mutableStateOf("") }
    Child(value = value, onValueChange = { value = it })
}

// Pattern: MutableList with remember
var items by remember { mutableStateOf(listOf<String>()) }
items = items + "new"  // Or use SnapshotStateList

// Pattern: Keyboard handling
OutlinedTextField(
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Next
    ),
    keyboardActions = KeyboardActions(
        onNext = { focusManager.moveFocus(FocusDirection.Down) }
    )
)

// Pattern: LazyColumn with keys
LazyColumn {
    items(items.size, key = { items[it].id }) { index ->
        ItemComposable(items[index])
    }
}

// Pattern: Animation
val animated by animateDpAsState(targetValue = if (expanded) 200.dp else 100.dp)
Box(modifier = Modifier.size(animated))
```

---

## 🎓 Learning Path (Fast Track)

**Week 1:**
- [ ] Master @Composable functions
- [ ] Understand Modifier chain order
- [ ] Layout system (Column, Row, Box)

**Week 2:**
- [ ] State management & remember
- [ ] Keyboard handling & input
- [ ] LazyColumn optimization

**Week 3:**
- [ ] Theming & styling
- [ ] Navigation
- [ ] MVVM pattern

**Week 4:**
- [ ] Animations
- [ ] Advanced patterns
- [ ] Testing & performance

---

**Remember:** Compose is declarative, not imperative. Think about WHAT to display, not HOW to display it.