# Kotlin Coroutines & Flow in Android

Coroutines provide a modern, lightweight, and non-blocking concurrency model for Android development. Instead of managing threads directly or dealing with nested callback structures ("callback hell"), coroutines allow you to write asynchronous code sequentially.

---

## 1. Concurrency Basics

### Blocking vs. Suspending
*   **Blocking a Thread**: Stops the entire thread from executing. If the main thread (UI thread) is blocked for more than 5 seconds, it triggers an **ANR (App Not Responding)** dialog.
*   **Suspending a Coroutine**: Pauses the execution of the coroutine without blocking the underlying thread. The thread is free to execute other coroutines or handle UI renders.

```kotlin
// Example of a suspending function
suspend fun fetchUserData(): User {
    // delay is a suspending function; it yields the thread to other operations
    delay(2000L) 
    return User("shreyas")
}
```

---

## 2. Dispatchers

Kotlin Coroutines use **Dispatchers** to determine which thread or thread pool executes a coroutine.

| Dispatcher | Thread Pool Type | Ideal Use Case |
| :--- | :--- | :--- |
| `Dispatchers.Main` | Main/UI Thread | UI interaction, light calculations, calling suspend functions. |
| `Dispatchers.IO` | Elastic thread pool (shared with disk/net) | Reading/writing database (Room), network calls (Retrofit), file I/O. |
| `Dispatchers.Default` | Fixed thread pool (number of CPU cores) | Heavy computations, sorting large lists, parsing complex JSON, image rendering. |

### Thread Switching with `withContext`
You can switch dispatchers inside a coroutine using `withContext`. It is a suspending function that executes the block on the designated dispatcher and returns the result safely.

```kotlin
class UserRepository {
    suspend fun loadUserData(): User = withContext(Dispatchers.IO) {
        // Runs on an IO thread
        database.getUser()
    }
}
```

---

## 3. Android Coroutine Scopes

In Android, coroutines must be tied to a lifecycle scope to prevent **memory leaks** when screens are closed or destroyed.

### A. `viewModelScope`
*   **Scope**: Bound to the ViewModel's lifecycle.
*   **Cancellation**: Automatically cancels all launched coroutines when the ViewModel is cleared (user exits screen).
```kotlin
class MyViewModel : ViewModel() {
    init {
        viewModelScope.launch {
            val data = repository.loadUserData()
            // Update UI state...
        }
    }
}
```

### B. `lifecycleScope`
*   **Scope**: Bound to a LifecycleOwner (Activity or Fragment).
*   **Cancellation**: Automatically cancels when the Activity/Fragment is destroyed.
```kotlin
class MyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            // Task that runs as long as the Activity is alive
        }
    }
}
```

### C. `rememberCoroutineScope()`
*   **Scope**: Bound to the Composable composition lifecycle.
*   **Use Case**: Launching coroutines in response to user gestures/events directly inside UI composables (e.g. scrolling a list on button click).
```kotlin
@Composable
fun MyScreen() {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    Button(onClick = {
        coroutineScope.launch {
            listState.animateScrollToItem(index = 0)
        }
    }) {
        Text("Scroll to top")
    }
}
```

---

## 4. Coroutine Builders

### `launch` vs `async`
*   **`launch`**: Starts a new coroutine that does not return a result ("fire and forget"). Returns a `Job` object, which can be used to monitor or cancel the coroutine.
*   **`async`**: Starts a new coroutine that calculates a value and returns a `Deferred<T>` (a future result). Use `await()` to pause execution until the result is returned.

```kotlin
// Launch usage
val job: Job = viewModelScope.launch {
    repository.saveData()
}

// Async usage (Parallel execution)
viewModelScope.launch {
    val deferredUser: Deferred<User> = async { repository.fetchUser() }
    val deferredOrders: Deferred<List<Order>> = async { repository.fetchOrders() }
    
    // Await both parallel results
    val user = deferredUser.await()
    val orders = deferredOrders.await()
}
```

---

## 5. Exception Handling

Uncaught exceptions in coroutines propagate up and will crash your application.

### A. Using `try-catch`
The simplest way to handle errors is inside the coroutine block:
```kotlin
viewModelScope.launch {
    try {
        val result = repository.fetchRemoteData()
    } catch (e: HttpException) {
        // Handle API error
    }
}
```

### B. `CoroutineExceptionHandler`
A global handler for uncaught exceptions. Define it and pass it to the launcher context.
```kotlin
val handler = CoroutineExceptionHandler { _, exception ->
    Log.e("CoroutineError", "Caught exception: ${exception.localizedMessage}")
}

// Pass handler into scope
viewModelScope.launch(handler) {
    throw RuntimeException("Something failed!")
}
```

### C. `SupervisorJob`
By default, if a child coroutine fails, it cancels its parent, which in turn cancels all other child coroutines. A `SupervisorJob` prevents this propagation. If one child fails, the others continue running.

```kotlin
// Creating a custom scope with SupervisorJob
val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

fun executeTasks() {
    scope.launch {
        throw RuntimeException("Task A failed!") // Will NOT cancel Task B
    }
    scope.launch {
        // Task B runs successfully
    }
}
```

---

## 6. Kotlin Flows

A **Flow** is a cold, asynchronous stream of values that emits data sequentially and finishes.

### Cold Flows
A cold flow does not emit values until it is collected (`collect`).

```kotlin
fun getNumbersFlow(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(1000L) // Simulate network/delay
        emit(i)     // Emit next number
    }
}

// Collection
viewModelScope.launch {
    getNumbersFlow().collect { value ->
        println(value) // Prints 1, then 2, then 3
    }
}
```

---

## 7. StateFlow vs. SharedFlow (Hot Flows)

Hot flows emit values immediately regardless of whether anyone is collecting.

### A. `StateFlow`
*   **Description**: Represents a state-holding observable flow that emits the current and new state updates.
*   **Key Traits**: Always holds a single value, requires an initial value, replays the latest value to new subscribers, and filters out identical updates (conflates values).
*   **Ideal Use Case**: Storing UI State in ViewModels.

```kotlin
class MyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun updateData(newData: String) {
        _uiState.value = UiState.Success(newData)
    }
}
```

### B. `SharedFlow`
*   **Description**: A highly configurable hot event stream.
*   **Key Traits**: Does not hold state, has no initial value, and can emit events multiple times (does not filter duplicates).
*   **Ideal Use Case**: One-time events (e.g. showing a SnackBar, displaying a Toast, navigating to another screen).

```kotlin
class MyViewModel : ViewModel() {
    private val _navigationEvents = MutableSharedFlow<Screen>()
    val navigationEvents: SharedFlow<Screen> = _navigationEvents.asSharedFlow()

    fun onButtonClicked() {
        viewModelScope.launch {
            _navigationEvents.emit(Screen.Profile)
        }
    }
}
```

---

## 8. Collecting Flows in Compose (Safely!)

In Jetpack Compose, you must convert flow streams into Composable `State`.

### The WRONG Way: `collectAsState()`
```kotlin
// Inside Composable
val uiState by viewModel.uiState.collectAsState()
```
> [!WARNING]
> `collectAsState()` remains active even when the application goes to the background (e.g. screen locked or home button pressed). This causes background resource consumption and can lead to battery drain or crashes.

### The RIGHT Way: `collectAsStateWithLifecycle()`
This function uses lifecycle-aware observation, halting flow collection when the lifecycle falls below `STARTED` (app goes background) and resuming when it restarts.

#### Add dependency to `build.gradle.kts`
```kotlin
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
```

#### Code Implementation
```kotlin
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MyScreen(viewModel: MyViewModel) {
    // Safely collect StateFlow
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        UiState.Loading -> CircularProgressIndicator()
        is UiState.Success -> Text(text = state.data)
    }
}
```
