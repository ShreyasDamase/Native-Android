# remember-mutableStateOf Complete Guide

## Overview

`remember` and `mutableStateOf` are fundamental state management APIs in Jetpack Compose that enable reactive UI updates. They work together to create and maintain state that survives recomposition.

## Core Concepts

### What is `remember`?

- **Purpose**: Stores values across recompositions
- **Lifecycle**: Tied to the composition's lifecycle
- **Memory**: Prevents recreation of expensive objects

### What is `mutableStateOf`?

- **Purpose**: Creates observable state that triggers recomposition
- **Type**: Returns `MutableState<T>`
- **Reactivity**: UI automatically updates when value changes

## Primitive State Types

Compose provides optimized state primitives for common data types that avoid boxing overhead and improve performance.

### Theory Behind Primitive States

**Why Primitive States Matter:**

- **Performance**: Avoid boxing/unboxing of primitive values
- **Memory**: Reduced memory allocation and garbage collection
- **Type Safety**: Compile-time type checking for numeric operations
- **Optimized Recomposition**: Better performance for frequently changing numeric values

**Available Primitive States:**

- `mutableIntStateOf()` - for `Int` values
- `mutableLongStateOf()` - for `Long` values
- `mutableFloatStateOf()` - for `Float` values
- `mutableDoubleStateOf()` - for `Double` values

### Performance Comparison

```kotlin
// ❌ Boxing overhead with generic state
var count by remember { mutableStateOf(0) } // Int gets boxed

// ✅ No boxing with primitive state
var count by remember { mutableIntStateOf(0) } // Direct Int storage
```

## Basic Usage

### Simple State Declaration

```kotlin
@Composable
fun CounterExample() {
    // Basic remember + mutableStateOf pattern
    var count by remember { mutableStateOf(0) }
    
    Column {
        Text("Count: $count")
        Button(onClick = { count++ }) {
            Text("Increment")
        }
    }
}
```

### Without Delegate Property

```kotlin
@Composable
fun CounterWithoutDelegate() {
    val countState = remember { mutableStateOf(0) }
    
    Column {
        Text("Count: ${countState.value}")
        Button(onClick = { countState.value++ }) {
            Text("Increment")
        }
    }
}
```

## Primitive State Types Examples

### mutableIntStateOf

```kotlin
@Composable
fun IntStateExample() {
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    
    Column {
        Text("Score: $score")
        Text("Lives: $lives")
        
        Row {
            Button(onClick = { score += 10 }) {
                Text("Add Points")
            }
            Button(
                onClick = { lives-- },
                enabled = lives > 0
            ) {
                Text("Lose Life")
            }
        }
        
        if (lives == 0) {
            Text(
                "Game Over! Final Score: $score",
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
```

### mutableFloatStateOf

```kotlin
@Composable
fun SliderExample() {
    var volume by remember { mutableFloatStateOf(0.5f) }
    var brightness by remember { mutableFloatStateOf(1.0f) }
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Volume: ${(volume * 100).roundToInt()}%")
        Slider(
            value = volume,
            onValueChange = { volume = it },
            valueRange = 0f..1f
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Brightness: ${(brightness * 100).roundToInt()}%")
        Slider(
            value = brightness,
            onValueChange = { brightness = it },
            valueRange = 0f..1f
        )
        
        // Visual feedback
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(
                    Color.Yellow.copy(alpha = brightness),
                    RoundedCornerShape(8.dp)
                )
        ) {
            Text(
                "Brightness Preview",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
```

### mutableDoubleStateOf

```kotlin
@Composable
fun PrecisionCalculator() {
    var input1 by remember { mutableDoubleStateOf(0.0) }
    var input2 by remember { mutableDoubleStateOf(0.0) }
    
    val result by remember(input1, input2) {
        mutableDoubleStateOf(input1 * input2)
    }
    
    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = input1.toString(),
            onValueChange = { 
                input1 = it.toDoubleOrNull() ?: 0.0
            },
            label = { Text("First Number") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            )
        )
        
        TextField(
            value = input2.toString(),
            onValueChange = { 
                input2 = it.toDoubleOrNull() ?: 0.0
            },
            label = { Text("Second Number") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            )
        )
        
        Text(
            "Result: ${"%.6f".format(result)}",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
```

### mutableLongStateOf

```kotlin
@Composable
fun TimerExample() {
    var startTime by remember { mutableLongStateOf(0L) }
    var currentTime by remember { mutableLongStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    
    val elapsedTime = currentTime - startTime
    
    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (isRunning) {
                currentTime = System.currentTimeMillis()
                delay(10) // Update every 10ms
            }
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = formatTime(elapsedTime),
            style = MaterialTheme.typography.displayLarge,
            fontFamily = FontFamily.Monospace
        )
        
        Row {
            Button(
                onClick = {
                    if (!isRunning) {
                        startTime = System.currentTimeMillis()
                        currentTime = startTime
                    }
                    isRunning = !isRunning
                }
            ) {
                Text(if (isRunning) "Stop" else "Start")
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = {
                    isRunning = false
                    startTime = 0L
                    currentTime = 0L
                }
            ) {
                Text("Reset")
            }
        }
    }
}

fun formatTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / (1000 * 60)) % 60
    val millis = (milliseconds % 1000) / 10
    return "%02d:%02d.%02d".format(minutes, seconds, millis)
}
```

## Comparative Performance Example

```kotlin
@Composable
fun PerformanceComparison() {
    // Generic state (boxing overhead)
    var genericCounter by remember { mutableStateOf(0) }
    
    // Primitive state (no boxing)
    var primitiveCounter by remember { mutableIntStateOf(0) }
    
    var iterations by remember { mutableIntStateOf(100000) }
    
    Column {
        Text("Performance Test")
        
        TextField(
            value = iterations.toString(),
            onValueChange = { iterations = it.toIntOrNull() ?: 100000 },
            label = { Text("Iterations") }
        )
        
        Row {
            Button(
                onClick = {
                    // This creates boxing overhead
                    repeat(iterations) {
                        genericCounter++
                    }
                }
            ) {
                Text("Generic: $genericCounter")
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = {
                    // This is more efficient
                    repeat(iterations) {
                        primitiveCounter++
                    }
                }
            ) {
                Text("Primitive: $primitiveCounter")
            }
        }
    }
}
```

## Different Data Types

### String State

```kotlin
@Composable
fun TextInputExample() {
    var text by remember { mutableStateOf("") }
    
    Column {
        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Enter text") }
        )
        Text("You typed: $text")
    }
}
```

### Boolean State

```kotlin
@Composable
fun ToggleExample() {
    var isChecked by remember { mutableStateOf(false) }
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { isChecked = it }
        )
        Text(if (isChecked) "Checked" else "Unchecked")
    }
}
```

### List State

```kotlin
@Composable
fun TodoListExample() {
    var items by remember { mutableStateOf(listOf<String>()) }
    var newItem by remember { mutableStateOf("") }
    
    Column {
        Row {
            TextField(
                value = newItem,
                onValueChange = { newItem = it },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (newItem.isNotBlank()) {
                        items = items + newItem
                        newItem = ""
                    }
                }
            ) {
                Text("Add")
            }
        }
        
        LazyColumn {
            items(items) { item ->
                Text(
                    text = item,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
```

### Data Class State

```kotlin
data class User(
    val name: String = "",
    val email: String = "",
    val age: Int = 0
)

@Composable
fun UserFormExample() {
    var user by remember { mutableStateOf(User()) }
    
    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = user.name,
            onValueChange = { user = user.copy(name = it) },
            label = { Text("Name") }
        )
        
        TextField(
            value = user.email,
            onValueChange = { user = user.copy(email = it) },
            label = { Text("Email") }
        )
        
        TextField(
            value = user.age.toString(),
            onValueChange = { 
                user = user.copy(age = it.toIntOrNull() ?: 0)
            },
            label = { Text("Age") }
        )
        
        Text("User: ${user.name}, ${user.email}, ${user.age}")
    }
}
```

## Advanced Primitive State Patterns

### Animation with Primitive States

```kotlin
@Composable
fun AnimatedCounter() {
    var targetValue by remember { mutableIntStateOf(0) }
    
    // Animate between integer values
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis = 1000)
    )
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = animatedValue.toString(),
            style = MaterialTheme.typography.displayLarge
        )
        
        Row {
            Button(onClick = { targetValue -= 10 }) {
                Text("-10")
            }
            Button(onClick = { targetValue += 10 }) {
                Text("+10")
            }
        }
        
        Button(onClick = { targetValue = 0 }) {
            Text("Reset")
        }
    }
}
```

### Progress Tracking with Float State

```kotlin
@Composable
fun DownloadProgress() {
    var progress by remember { mutableFloatStateOf(0f) }
    var isDownloading by remember { mutableStateOf(false) }
    
    LaunchedEffect(isDownloading) {
        if (isDownloading) {
            while (progress < 1f && isDownloading) {
                delay(50)
                progress += 0.01f
            }
            if (progress >= 1f) {
                isDownloading = false
            }
        }
    }
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Download Progress: ${(progress * 100).roundToInt()}%")
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row {
            Button(
                onClick = { 
                    isDownloading = !isDownloading
                },
                enabled = progress < 1f
            ) {
                Text(if (isDownloading) "Pause" else "Start")
            }
            
            Button(
                onClick = { 
                    progress = 0f
                    isDownloading = false
                }
            ) {
                Text("Reset")
            }
        }
    }
}
```

### Memory Usage Tracker with Long State

```kotlin
@Composable
fun MemoryUsageTracker() {
    var usedMemory by remember { mutableLongStateOf(0L) }
    var maxMemory by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(Unit) {
        while (true) {
            val runtime = Runtime.getRuntime()
            maxMemory = runtime.maxMemory()
            usedMemory = runtime.totalMemory() - runtime.freeMemory()
            delay(1000) // Update every second
        }
    }
    
    val usagePercentage = if (maxMemory > 0) {
        (usedMemory.toFloat() / maxMemory.toFloat())
    } else 0f
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Memory Usage",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Text("Used: ${formatBytes(usedMemory)}")
        Text("Max: ${formatBytes(maxMemory)}")
        Text("Percentage: ${(usagePercentage * 100).roundToInt()}%")
        
        LinearProgressIndicator(
            progress = usagePercentage,
            modifier = Modifier.fillMaxWidth(),
            color = when {
                usagePercentage > 0.9f -> Color.Red
                usagePercentage > 0.7f -> Color.Yellow
                else -> Color.Green
            }
        )
        
        Button(
            onClick = { System.gc() }
        ) {
            Text("Run Garbage Collector")
        }
    }
}

fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024 * 1024)
    return "${mb}MB"
}
```

## When to Use Each Primitive State

### Decision Guide

```kotlin
@Composable
fun StateSelectionGuide() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "State Selection Guide",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Use mutableIntStateOf for:
        Card(modifier = Modifier.padding(vertical = 4.dp)) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("mutableIntStateOf - Use for:")
                Text("• Counters, scores, indices")
                Text("• UI measurements (dp, px)")
                Text("• Discrete numeric values")
                
                var counter by remember { mutableIntStateOf(0) }
                Row {
                    Text("Example: $counter")
                    Button(onClick = { counter++ }) { Text("+") }
                }
            }
        }
        
        // Use mutableFloatStateOf for:
        Card(modifier = Modifier.padding(vertical = 4.dp)) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("mutableFloatStateOf - Use for:")
                Text("• Progress values (0.0 to 1.0)")
                Text("• Animation values")
                Text("• Percentages, ratios")
                
                var progress by remember { mutableFloatStateOf(0.5f) }
                Column {
                    Text("Example: ${(progress * 100).roundToInt()}%")
                    Slider(
                        value = progress,
                        onValueChange = { progress = it }
                    )
                }
            }
        }
        
        // Use mutableDoubleStateOf for:
        Card(modifier = Modifier.padding(vertical = 4.dp)) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("mutableDoubleStateOf - Use for:")
                Text("• High precision calculations")
                Text("• Scientific computations")
                Text("• Financial calculations")
                
                var value by remember { mutableDoubleStateOf(3.14159) }
                Text("Example: ${"%.5f".format(value)}")
            }
        }
        
        // Use mutableLongStateOf for:
        Card(modifier = Modifier.padding(vertical = 4.dp)) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("mutableLongStateOf - Use for:")
                Text("• Timestamps, durations")
                Text("• Large numbers, IDs")
                Text("• Memory sizes, file sizes")
                
                var timestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
                Text("Example: $timestamp")
                Button(onClick = { timestamp = System.currentTimeMillis() }) {
                    Text("Update")
                }
            }
        }
    }
}
```

### remember with Key

```kotlin
@Composable
fun UserProfile(userId: String) {
    // Recompute when userId changes
    var userData by remember(userId) { 
        mutableStateOf(loadUserData(userId)) 
    }
    
    Column {
        Text("User ID: $userId")
        Text("Data: $userData")
        Button(
            onClick = { userData = loadUserData(userId) }
        ) {
            Text("Refresh")
        }
    }
}

fun loadUserData(id: String): String = "Data for user $id"
```

### Multiple Keys

```kotlin
@Composable
fun FilteredList(
    items: List<String>,
    filter: String,
    sortAscending: Boolean
) {
    val filteredAndSorted by remember(items, filter, sortAscending) {
        mutableStateOf(
            items
                .filter { it.contains(filter, ignoreCase = true) }
                .let { if (sortAscending) it.sorted() else it.sortedDescending() }
        )
    }
    
    LazyColumn {
        items(filteredAndSorted) { item ->
            Text(item)
        }
    }
}
```

### Expensive Computation

```kotlin
@Composable
fun ExpensiveCalculation(input: Int) {
    val result by remember(input) {
        mutableStateOf(performExpensiveCalculation(input))
    }
    
    Text("Result for $input: $result")
}

fun performExpensiveCalculation(input: Int): Int {
    // Simulate expensive operation
    Thread.sleep(100)
    return input * input
}
```

## State with Side Effects

### remember with LaunchedEffect

```kotlin
@Composable
fun TimerExample() {
    var seconds by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            seconds++
        }
    }
    
    Text("Seconds: $seconds")
}
```

### remember with DisposableEffect

```kotlin
@Composable
fun LocationTracker() {
    var location by remember { mutableStateOf("Unknown") }
    
    DisposableEffect(Unit) {
        val callback = LocationCallback { newLocation ->
            location = newLocation
        }
        
        // Start location updates
        startLocationUpdates(callback)
        
        onDispose {
            stopLocationUpdates(callback)
        }
    }
    
    Text("Current location: $location")
}
```

## Best Practices

### Use Stable Data Classes

```kotlin
// ✅ Good - Immutable data class
@Stable
data class UserState(
    val name: String = "",
    val email: String = "",
    val isLoading: Boolean = false
)

@Composable
fun UserProfile() {
    var userState by remember { mutableStateOf(UserState()) }
    
    // Update immutably
    Button(
        onClick = {
            userState = userState.copy(isLoading = true)
        }
    ) {
        Text("Load")
    }
}
```

### Avoid Mutable Collections

```kotlin
// ❌ Bad - Mutable list
@Composable
fun BadExample() {
    val items = remember { mutableStateOf(mutableListOf<String>()) }
    
    // This won't trigger recomposition
    Button(onClick = { items.value.add("New item") }) {
        Text("Add")
    }
}

// ✅ Good - Immutable updates
@Composable
fun GoodExample() {
    var items by remember { mutableStateOf(listOf<String>()) }
    
    // This triggers recomposition
    Button(onClick = { items = items + "New item" }) {
        Text("Add")
    }
}
```

### State Hoisting Pattern

```kotlin
// ✅ Stateless composable
@Composable
fun Counter(
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column {
        Text("Count: $count")
        Row {
            Button(onClick = onIncrement) {
                Text("+")
            }
            Button(onClick = onDecrement) {
                Text("-")
            }
        }
    }
}

// ✅ Stateful wrapper
@Composable
fun StatefulCounter() {
    var count by remember { mutableStateOf(0) }
    
    Counter(
        count = count,
        onIncrement = { count++ },
        onDecrement = { count-- }
    )
}
```

## Performance Considerations

### Use Keys Wisely

```kotlin
@Composable
fun UserList(users: List<User>) {
    LazyColumn {
        items(
            items = users,
            key = { user -> user.id } // Stable key for performance
        ) { user ->
            UserItem(user = user)
        }
    }
}

@Composable
fun UserItem(user: User) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column {
            Text(user.name)
            if (isExpanded) {
                Text(user.email)
                Text(user.details)
            }
        }
    }
}
```

### Minimize State Scope

```kotlin
// ✅ Good - State close to where it's used
@Composable
fun LoginForm() {
    Column {
        EmailInput()
        PasswordInput()
        LoginButton()
    }
}

@Composable
private fun EmailInput() {
    var email by remember { mutableStateOf("") }
    
    TextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") }
    )
}
```

## Common Patterns

### Form Validation

```kotlin
@Composable
fun ValidatedForm() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    val isEmailValid by remember(email) {
        mutableStateOf(email.contains("@"))
    }
    
    val isPasswordValid by remember(password) {
        mutableStateOf(password.length >= 8)
    }
    
    val isFormValid = isEmailValid && isPasswordValid
    
    Column {
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            isError = email.isNotEmpty() && !isEmailValid
        )
        
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            isError = password.isNotEmpty() && !isPasswordValid
        )
        
        Button(
            onClick = { /* Submit form */ },
            enabled = isFormValid
        ) {
            Text("Submit")
        }
    }
}
```

### Search with Debouncing

```kotlin
@Composable
fun SearchExample() {
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<String>()) }
    
    LaunchedEffect(query) {
        delay(300) // Debounce
        if (query.isNotEmpty()) {
            searchResults = performSearch(query)
        } else {
            searchResults = emptyList()
        }
    }
    
    Column {
        TextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search") }
        )
        
        LazyColumn {
            items(searchResults) { result ->
                Text(result)
            }
        }
    }
}

fun performSearch(query: String): List<String> {
    // Mock search implementation
    return listOf("Result 1", "Result 2", "Result 3")
        .filter { it.contains(query, ignoreCase = true) }
}
```

## Testing State

```kotlin
@Test
fun testCounterState() {
    composeTestRule.setContent {
        CounterExample()
    }
    
    // Initial state
    composeTestRule.onNodeWithText("Count: 0").assertExists()
    
    // Click increment
    composeTestRule.onNodeWithText("Increment").performClick()
    
    // Verify state change
    composeTestRule.onNodeWithText("Count: 1").assertExists()
}
```

## Key Takeaways

1. **Always use `remember`** when creating any mutable state in composables
2. **Choose primitive states** for performance when working with Int, Float, Double, Long
3. **Use delegation** (`by remember`) for cleaner syntax
4. **Update immutably** to trigger recomposition
5. **Hoist state** when multiple composables need access
6. **Use stable keys** for performance optimization
7. **Keep state minimal** and close to where it's used
8. **Prefer immutable data structures** over mutable ones

## Performance Best Practices for Primitive States

### Memory Efficiency

```kotlin
// ✅ Good - Use primitive states for numeric values
var score by remember { mutableIntStateOf(0) }        // No boxing
var progress by remember { mutableFloatStateOf(0f) }  // No boxing
var timestamp by remember { mutableLongStateOf(0L) }  // No boxing

// ❌ Avoid - Generic state for primitives causes boxing
var score by remember { mutableStateOf(0) }           // Boxing overhead
var progress by remember { mutableStateOf(0f) }       // Boxing overhead
var timestamp by remember { mutableStateOf(0L) }      // Boxing overhead
```

### Recomposition Optimization

```kotlin
@Composable
fun OptimizedCounter() {
    // Fast path for primitive operations
    var count by remember { mutableIntStateOf(0) }
    
    // This is optimized for integer operations
    Button(onClick = { count += 1 }) {
        Text("Count: $count")
    }
}
```

### Combining Primitive and Generic States

```kotlin
@Composable
fun MixedStateExample() {
    // Use primitive states for numeric values
    var score by remember { mutableIntStateOf(0) }
    var multiplier by remember { mutableFloatStateOf(1.0f) }
    
    // Use generic state for complex objects
    var player by remember { mutableStateOf(Player("Unknown")) }
    
    // Computed property using both
    val finalScore by remember(score, multiplier) {
        mutableIntStateOf((score * multiplier).roundToInt())
    }
    
    Column {
        Text("Player: ${player.name}")
        Text("Score: $score")
        Text("Multiplier: ${multiplier}x")
        Text("Final Score: $finalScore")
    }
}

data class Player(val name: String)
```

---

## 🚀 Mastery Deep Dive (Added 2026)

> [!NOTE]
> The following deep dive notes were generated to provide mastery-level understanding, complementing the original notes above.

# remember & mutableStateOf — Deep Reference

> [!NOTE]
> `States.md` covers the fundamentals. This file goes deeper: `@Stable`/`@Immutable`, `remember` key mechanics, `rememberSaveable` Saver patterns, when NOT to use `remember`, and smart recomposition skipping.

---

## 🧠 Mental Model — Read This First

**`remember` is a numbered locker room.**

- Every `remember` call in your composable gets its own locker (slot), numbered by **position in the code**, not by variable name.
- First composition: Compose OPENS each locker and puts the computed value inside.
- Every recomposition: Compose READS each locker — it does NOT recompute. The lambda inside `remember { }` never runs again.
- When a `remember(key)` detects the key changed: it EMPTIES the locker and recomputes the value.

This is why `remember` calls MUST always be in the same order — if you conditionally skip one, every subsequent locker number shifts, and you end up reading the wrong value.

---

## 🔬 `remember(key)` — Deep Dive into Key Mechanics

### Single Key

```kotlin
@Composable
fun UserProfile(userId: String) {
    // Every time userId changes → lambda runs again → loadUser() called fresh
    // Same userId → lambda skipped → cached value returned
    val userProfile by remember(userId) {
        mutableStateOf(loadUser(userId))
    }
}
```

**Internally, Compose stores:**
```
Slot[n] = {
    keys: ["user-123"],           // current keys
    value: MutableState<User>     // the remembered value
}
```

On recomposition with `userId = "user-456"`:
- Compose checks: `["user-456"] != ["user-123"]` → keys changed
- Throws away the old `MutableState<User>`
- Runs the lambda: `mutableStateOf(loadUser("user-456"))`
- Stores new value in the same slot

### Multiple Keys — ANY key change resets

```kotlin
@Composable
fun FilteredSortedList(
    items: List<Product>,
    filter: String,
    sortOrder: SortOrder
) {
    // Recomputes if items, filter, OR sortOrder changes
    val processed by remember(items, filter, sortOrder) {
        mutableStateOf(
            items
                .filter { it.name.contains(filter, ignoreCase = true) }
                .sortedWith(sortOrder.comparator)
        )
    }

    LazyColumn {
        items(processed) { product -> ProductRow(product) }
    }
}
```

### `remember` for expensive objects (not just state)

```kotlin
// remember is not only for MutableState — it caches ANY expensive object
@Composable
fun RegexValidatedInput(pattern: String, input: String) {
    // Regex compilation is expensive — cache it, recompile only when pattern changes
    val regex = remember(pattern) { Regex(pattern) }
    val isValid = regex.matches(input)

    OutlinedTextField(
        value = input,
        onValueChange = { /* hoisted */ },
        isError = !isValid
    )
}

@Composable
fun AnimatedGradient() {
    // Shader creation is expensive — cache across recompositions
    val shader = remember {
        LinearGradientShader(
            from = Offset.Zero,
            to = Offset(100f, 100f),
            colors = listOf(Color.Blue, Color.Purple)
        )
    }
}
```

---

## 🏷️ `@Stable` and `@Immutable` — Teaching Compose to Skip

### The Skipping Problem

Compose can **skip** recomposing a composable if its parameters haven't changed. But it only skips if it can PROVE the parameters are stable (won't change in a way Compose can't detect).

**Compose considers a type stable if:**
1. It's a primitive (`Int`, `String`, `Boolean`, etc.)
2. It's annotated with `@Stable` or `@Immutable`
3. It's a Kotlin `data class` where ALL fields are stable types
4. It's a Compose `State<T>` type

**If a parameter type is NOT stable, Compose always recomposes the child — even if the value didn't actually change.**

```kotlin
// Regular class — Compose treats as UNSTABLE (cannot verify it won't change)
class User(val name: String, val age: Int)

// Compose CANNOT skip recomposing UserCard when User is passed as unstable
@Composable
fun UserCard(user: User) {   // 'user' is unstable → always recomposes
    Text(user.name)
    Text(user.age.toString())
}
```

### `@Immutable` — The Strong Guarantee

```kotlin
// @Immutable: you promise ALL properties will NEVER change after construction
@Immutable
data class User(val name: String, val age: Int)
// Now Compose knows: if the User reference is the same, the data is identical
// → Can skip recomposing composables that receive this User if reference hasn't changed

@Composable
fun UserCard(user: User) {   // 'user' is now stable (@Immutable) → Compose CAN skip this
    Text(user.name)
}
```

### `@Stable` — The Softer Guarantee

```kotlin
// @Stable: you promise that if == returns true, all public properties are equal
// AND: changes to properties are always notified via snapshot system
@Stable
class UserState(initialName: String) {
    var name by mutableStateOf(initialName)   // changes are observable via snapshot ✅
}
// Compose can now skip recompositions where the UserState reference is the same
// AND none of its observable properties changed
```

### Practical Example: The Performance Difference

```kotlin
// Without @Immutable — always recomposes
data class ProductItem(val id: String, val name: String, val price: Double)

@Composable
fun ProductList(products: List<ProductItem>) {
    LazyColumn {
        items(products) { product ->
            ProductRow(product)  // recomposes every time parent recomposes — expensive!
        }
    }
}

// With @Immutable — Compose can skip ProductRow if ProductItem didn't change
@Immutable
data class ProductItem(val id: String, val name: String, val price: Double)

@Composable
fun ProductRow(product: ProductItem) {  // CAN be skipped now ✅
    Row { Text(product.name); Text("$${product.price}") }
}
```

> [!TIP]
> For the Compose compiler plugin (Compose Compiler Metrics) to verify your stability, you can run: `./gradlew assembleRelease -PcomposeCompilerReports=true`. It generates a report showing which composables are "skippable" and which aren't due to unstable parameters.

---

## 🚫 When NOT to Use `remember`

Not every value needs `remember`. Using it unnecessarily adds overhead to the Slot Table.

### Don't use `remember` for derived values that recompute on every recomposition anyway

```kotlin
// ❌ Unnecessary — fullName changes every time firstName or lastName changes
// derivedStateOf would be better, or just compute directly
val fullName by remember(firstName, lastName) {
    mutableStateOf("$firstName $lastName")
}

// ✅ Just compute directly — the composable already recomposes when these change
val fullName = "$firstName $lastName"   // no remember needed!
```

### Don't use `remember` when the value doesn't need to survive recomposition

```kotlin
// ❌ Overkill — this formatting changes on every recomposition anyway
val formattedDate by remember(timestamp) {
    mutableStateOf(DateFormat.getDateInstance().format(timestamp))
}

// ✅ Just compute inline
val formattedDate = DateFormat.getDateInstance().format(timestamp)
```

### DO use `remember` for:

```kotlin
// ✅ Expensive object creation that doesn't depend on changing params
val paint = remember { Paint().apply { color = Color.Red } }

// ✅ Observable state that the user can modify
var searchQuery by remember { mutableStateOf("") }

// ✅ Objects that are expensive per-instance (Regex, Coroutine, etc.)
val animatable = remember { Animatable(0f) }

// ✅ State holders / objects with internal state
val scrollState = rememberScrollState()
val lazyListState = rememberLazyListState()
```

---

## 🗄️ `rememberSaveable` — Saver Patterns for Complex Types

### Pattern 1: `mapSaver` — Cleanest for data classes

```kotlin
data class FilterState(
    val query: String = "",
    val priceMin: Int = 0,
    val priceMax: Int = 10000,
    val categoriesSelected: Set<String> = emptySet()
)

val FilterStateSaver = run {
    // mapSaver: converts to/from Map<String, Any?>
    val queryKey = "query"
    val priceMinKey = "priceMin"
    val priceMaxKey = "priceMax"
    val categoriesKey = "categories"

    mapSaver(
        save = { state ->
            mapOf(
                queryKey    to state.query,
                priceMinKey to state.priceMin,
                priceMaxKey to state.priceMax,
                categoriesKey to state.categoriesSelected.toList()  // Set → List (Bundle-safe)
            )
        },
        restore = { map ->
            @Suppress("UNCHECKED_CAST")
            FilterState(
                query              = map[queryKey] as String,
                priceMin           = map[priceMinKey] as Int,
                priceMax           = map[priceMaxKey] as Int,
                categoriesSelected = (map[categoriesKey] as List<String>).toSet()
            )
        }
    )
}

@Composable
fun FilterScreen() {
    var filterState by rememberSaveable(stateSaver = FilterStateSaver) {
        mutableStateOf(FilterState())
    }
}
```

### Pattern 2: `listSaver` — For ordered sequences

```kotlin
data class Point(val x: Float, val y: Float)

val PointSaver = listSaver<Point, Float>(
    save = { point -> listOf(point.x, point.y) },
    restore = { list -> Point(list[0], list[1]) }
)

@Composable
fun DrawingCanvas() {
    var touchPoint by rememberSaveable(stateSaver = PointSaver) {
        mutableStateOf(Point(0f, 0f))
    }
}
```

### Pattern 3: `Parcelable` — Auto-save without a Saver

```kotlin
@Parcelize
data class CartItem(
    val productId: String,
    val quantity: Int,
    val price: Double
) : Parcelable   // ← @Parcelize from kotlin-parcelize plugin

@Composable
fun CartItemEditor() {
    // Parcelable is auto-saveable — no custom Saver needed!
    var selectedItem by rememberSaveable {
        mutableStateOf(CartItem("prod_1", 1, 29.99))
    }
}
```

> [!TIP]
> For most cases, making your UI state data classes `@Parcelize` is the simplest approach. It's faster to write than a custom Saver and handles nested Parcelable objects automatically.

---

## 🔄 Primitive State Types — Full Examples

### `mutableIntStateOf` — Counters, indices, scores

```kotlin
@Composable
fun QuantitySelector(
    min: Int = 1,
    max: Int = 99,
    onQuantityChange: (Int) -> Unit
) {
    var quantity by remember { mutableIntStateOf(min) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { if (quantity > min) { quantity--; onQuantityChange(quantity) } },
            enabled = quantity > min
        ) {
            Icon(Icons.Default.Remove, "Decrease")
        }

        Text(
            text = quantity.toString(),
            modifier = Modifier.widthIn(min = 40.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )

        IconButton(
            onClick = { if (quantity < max) { quantity++; onQuantityChange(quantity) } },
            enabled = quantity < max
        ) {
            Icon(Icons.Default.Add, "Increase")
        }
    }
}
```

### `mutableFloatStateOf` — Progress, sliders, animations

```kotlin
@Composable
fun VolumeControl() {
    var volume by remember { mutableFloatStateOf(0.7f) }

    Column {
        Icon(
            imageVector = when {
                volume == 0f -> Icons.Default.VolumeOff
                volume < 0.5f -> Icons.Default.VolumeDown
                else -> Icons.Default.VolumeUp
            },
            contentDescription = "Volume: ${(volume * 100).roundToInt()}%"
        )
        Slider(
            value = volume,
            onValueChange = { volume = it },
            valueRange = 0f..1f,
            steps = 9  // 10% increments
        )
        Text("${(volume * 100).roundToInt()}%")
    }
}
```

### `mutableLongStateOf` — Timestamps, stopwatch

```kotlin
@Composable
fun Stopwatch() {
    var startTimeMs by remember { mutableLongStateOf(0L) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            startTimeMs = System.currentTimeMillis() - elapsedMs
            while (isRunning) {
                delay(10)   // update every 10ms
                elapsedMs = System.currentTimeMillis() - startTimeMs
            }
        }
    }

    val minutes = (elapsedMs / 60_000)
    val seconds = (elapsedMs / 1_000) % 60
    val centiseconds = (elapsedMs / 10) % 100

    Text(
        text = "%02d:%02d.%02d".format(minutes, seconds, centiseconds),
        style = MaterialTheme.typography.displayLarge,
        fontFamily = FontFamily.Monospace
    )
}
```

---

## 🧩 State for Complex Forms — Data Class Pattern

```kotlin
data class RegistrationForm(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val acceptedTerms: Boolean = false
) {
    val emailError: String? get() = when {
        email.isEmpty() -> null   // don't show error on empty
        !email.contains("@") -> "Invalid email format"
        else -> null
    }
    val passwordError: String? get() = when {
        password.isEmpty() -> null
        password.length < 8 -> "Password must be at least 8 characters"
        else -> null
    }
    val confirmError: String? get() = when {
        confirmPassword.isEmpty() -> null
        confirmPassword != password -> "Passwords don't match"
        else -> null
    }
    val isValid get() = email.isNotEmpty() && password.isNotEmpty() &&
                        emailError == null && passwordError == null &&
                        confirmError == null && acceptedTerms
}

@Composable
fun RegistrationScreen(onRegister: (String, String) -> Unit) {
    // One mutableStateOf for the whole form — clean and atomic updates
    var form by remember { mutableStateOf(RegistrationForm()) }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = form.email,
            onValueChange = { form = form.copy(email = it) },  // copy() creates new instance → recomposition
            label = { Text("Email") },
            isError = form.emailError != null,
            supportingText = form.emailError?.let { { Text(it) } }
        )
        OutlinedTextField(
            value = form.password,
            onValueChange = { form = form.copy(password = it) },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            isError = form.passwordError != null,
            supportingText = form.passwordError?.let { { Text(it) } }
        )
        OutlinedTextField(
            value = form.confirmPassword,
            onValueChange = { form = form.copy(confirmPassword = it) },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            isError = form.confirmError != null,
            supportingText = form.confirmError?.let { { Text(it) } }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = form.acceptedTerms,
                onCheckedChange = { form = form.copy(acceptedTerms = it) }
            )
            Text("I accept the Terms and Conditions")
        }
        Button(
            onClick = { onRegister(form.email, form.password) },
            enabled = form.isValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Account")
        }
    }
}
```

---

## 🔗 Connections

- **Foundation**: [[States]] — Snapshot System, Slot Table, `by` delegation basics
- **Derived state**: [[Derived State Of Computed State]] — when to compute state from other state
- **Hoisting**: [[State Hoisting Patterns]] — when to move this state up to parent or ViewModel
- **Coroutines in Compose**: `LaunchedEffect` and `rememberCoroutineScope` are built on the same `remember` machinery

---

## 💬 Interview Master Q&A

**Q: What does `remember(key) { }` do when the key changes?**
> `remember` stores the value in Compose's Slot Table associated with a call site position. When using `remember(key)`, Compose also stores the key alongside the value. On each recomposition, it compares the current key to the stored key using structural equality. If they differ, Compose invalidates the slot, runs the lambda to compute a new value, and stores both the new value and new key. This is the mechanism for resetting state when a parent-provided identifier changes — like loading a new user's data when `userId` changes.

**Q: What are `@Stable` and `@Immutable` and why do they matter?**
> These annotations are hints to the Compose compiler about how types behave. `@Immutable` is a strong promise that all public properties will never change after construction — Compose can safely skip recomposing a composable that receives an `@Immutable` parameter if its reference hasn't changed. `@Stable` is a weaker promise: if `a == b` then all public properties of `a` and `b` are also equal, and any mutations will be observable via the Snapshot System. Without these annotations on non-primitive types, Compose assumes the type is unstable and always recomposes, even when unnecessary — which can significantly hurt performance on large lists.

**Q: When would you NOT use `remember`?**
> I don't use `remember` when the value is derived directly from state or parameters that already cause recomposition, and the computation is cheap. For example, `val fullName = "$firstName $lastName"` doesn't need `remember` — the composable already recomposes when firstName or lastName change, and string concatenation is trivial. `remember` has a real cost: it allocates a slot in the Slot Table and the stored value occupies memory. Reserve it for expensive computations, observable state (`mutableStateOf`), and objects that are costly to create (Regex, Animatable, scroll state).

**Q: How does a custom `Saver` work in `rememberSaveable`?**
> A `Saver<T, Saveable>` is an object with two functions: `save(T)` which converts your custom type into a Bundle-compatible type (String, Int, Bundle, List, etc.), and `restore(Saveable)` which converts it back. When `rememberSaveable` detects a configuration change or process death, it calls `save()` to serialize your state into the Android Bundle via `onSaveInstanceState`. When the Activity/Fragment is recreated, it calls `restore()` to reconstruct your object from the Bundle. The `mapSaver` and `listSaver` helper functions make this pattern even cleaner for common cases.