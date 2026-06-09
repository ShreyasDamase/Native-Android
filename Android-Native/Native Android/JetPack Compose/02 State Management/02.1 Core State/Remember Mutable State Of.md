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