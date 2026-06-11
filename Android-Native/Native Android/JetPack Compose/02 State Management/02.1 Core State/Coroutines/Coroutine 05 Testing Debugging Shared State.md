# Coroutine 05 Testing Debugging Shared State

This note covers shared mutable state, testing, debugging, and performance.

---

## Shared Mutable State

### Problem

- Coroutines can run on multiple threads.
- Multiple coroutines updating the same variable can cause race conditions.

Bad:

```kotlin
var counter = 0

coroutineScope {
    repeat(1000) {
        launch(Dispatchers.Default) {
            counter++
        }
    }
}
```

### `Mutex`

- Coroutine-friendly lock.
- Suspends instead of blocking a thread.

```kotlin
val mutex = Mutex()
var counter = 0

coroutineScope {
    repeat(1000) {
        launch(Dispatchers.Default) {
            mutex.withLock {
                counter++
            }
        }
    }
}
```

### Atomic types

- Good for simple thread-safe values.

```kotlin
val counter = AtomicInteger(0)

coroutineScope {
    repeat(1000) {
        launch(Dispatchers.Default) {
            counter.incrementAndGet()
        }
    }
}
```

### State confinement

- Keep state updates on one dispatcher.
- Android UI state is usually updated on Main.
- Prefer immutable data class copies.

```kotlin
_uiState.value = _uiState.value.copy(loading = true)
```

### Interview explanation

- "For shared state, I avoid uncontrolled mutation. I use immutable state, `Mutex`, atomic classes, or confine updates to one dispatcher."

---

## Testing Coroutines

### Main tools

- `runTest`
  - Creates a test coroutine scope.
  - Uses virtual time.
- `StandardTestDispatcher`
  - Controlled execution.
- `UnconfinedTestDispatcher`
  - Eager execution.
- `advanceUntilIdle()`
  - Runs pending coroutine work.
- `advanceTimeBy()`
  - Moves virtual time forward.

### Simple suspend test

```kotlin
@Test
fun loadUsers_returnsUsers() = runTest {
    val users = repository.getUsers()
    assertEquals(2, users.size)
}
```

### Testing delay

```kotlin
@Test
fun delay_isControlledByVirtualTime() = runTest {
    var done = false

    launch {
        delay(5000)
        done = true
    }

    assertFalse(done)
    advanceTimeBy(5000)
    assertTrue(done)
}
```

### Inject dispatchers

```kotlin
class UserRepository(
    private val api: UserApi,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun getUsers(): List<User> = withContext(ioDispatcher) {
        api.getUsers()
    }
}
```

### ViewModel test idea

```kotlin
@Test
fun loadUsers_updatesState() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    val repository = FakeUserRepository(dispatcher)
    val viewModel = UserViewModel(repository)

    viewModel.loadUsers()
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.loading)
    assertEquals(2, viewModel.uiState.value.users.size)
}
```

### Flow testing belongs here and Flow notes

- For Flow basics and operators, see [[../Android Jetpack Flow Study Notes]].
- For Compose collection, see [[../Flow with Compose]].
- Flow can be tested with `toList()` or Turbine.

```kotlin
@Test
fun flowEmitsValues() = runTest {
    val values = countFlow().toList()
    assertEquals(listOf(1, 2, 3), values)
}
```

### Interview explanation

- "I test coroutine code with `runTest`, inject dispatchers, use virtual time for delays, and use `advanceUntilIdle()` to finish pending work."

---

## Debugging

### Coroutine names

```kotlin
viewModelScope.launch(CoroutineName("LoadUsers")) {
    repository.getUsers()
}
```

### Log current thread

```kotlin
fun threadInfo(): String = Thread.currentThread().name
```

```kotlin
viewModelScope.launch {
    Log.d("Coroutine", "Started on ${threadInfo()}")

    withContext(Dispatchers.IO) {
        Log.d("Coroutine", "IO on ${threadInfo()}")
    }

    Log.d("Coroutine", "Back on ${threadInfo()}")
}
```

### Debug mode

```text
-Dkotlinx.coroutines.debug
```

### Interview explanation

- "For debugging, I use coroutine names, log thread names, and inspect cancellation/error paths."

---

## Performance Checklist

- Do not run CPU-heavy work on Main.
- Do not use `runBlocking` in Android UI.
- Do not launch unlimited coroutines in loops.
- Avoid `GlobalScope`.
- Cancel old jobs for repeated actions.
- Use `Dispatchers.IO` for blocking IO.
- Use `Dispatchers.Default` for CPU-heavy work.
- Use `limitedParallelism()` if too many tasks overload a resource.
