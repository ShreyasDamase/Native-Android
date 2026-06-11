# Coroutine 00 Setup Basics Suspend

This note covers setup, first coroutine, coroutine meaning, blocking vs suspending, and `suspend` functions.

---

## Setup

### Dependencies

- `kotlinx-coroutines-core`
  - Core coroutine APIs.
  - Provides `launch`, `async`, `delay`, `coroutineScope`, Flow, Channel, etc.
- `kotlinx-coroutines-android`
  - Android integration.
  - Provides `Dispatchers.Main`.
- `lifecycle-viewmodel-ktx`
  - Provides `viewModelScope`.
- `lifecycle-runtime-ktx`
  - Provides `lifecycleScope`.
- `kotlinx-coroutines-test`
  - Used for coroutine unit tests.

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
```

### First coroutine

```kotlin
viewModelScope.launch {
    delay(1000)
    _uiText.value = "Loaded"
}
```

### Interview explanation

- Coroutines need library support from `kotlinx.coroutines`.
- Android apps need `kotlinx-coroutines-android` for `Dispatchers.Main`.
- `viewModelScope.launch { }` is a common first coroutine in Android.

---

## What Is a Coroutine?

### Definition

- A coroutine is a lightweight asynchronous task.
- It can suspend and resume.
- It does not block the underlying thread when suspended.
- It runs on real threads through dispatchers.
- Many coroutines can share a small number of threads.

### Coroutine is not a thread

- Thread:
  - OS-level execution resource.
  - Expensive compared to coroutine.
  - Blocking a thread wastes that resource.
- Coroutine:
  - Kotlin-level async task.
  - Lightweight.
  - Can suspend and release the thread.

### Real-life example

- Thread blocking:
  - A waiter takes an order and stands still until food is ready.
  - Other customers are ignored.
- Coroutine suspending:
  - A waiter takes an order and lets the kitchen cook.
  - The waiter serves other tables.
  - The waiter comes back when food is ready.

### Android example

- Network call waits for server response.
- Main thread stays free for UI drawing and touch events.
- Coroutine resumes when response arrives.

### Interview explanation

- "A coroutine is a lightweight async task. It can suspend without blocking a thread, so Android can keep the UI responsive during network, database, or long-running work."

---

## Blocking vs Suspending

### Blocking

- Blocks the whole thread.
- Main thread blocking causes UI freeze.
- Long main-thread blocking can cause ANR.

```kotlin
fun badExample() {
    Thread.sleep(5000)
    showUser()
}
```

### Suspending

- Pauses only the coroutine.
- Frees the thread for other work.
- Coroutine resumes later from the same point.

```kotlin
suspend fun goodExample() {
    delay(5000)
    showUser()
}
```

### `Thread.sleep()` vs `delay()`

| Topic | `Thread.sleep()` | `delay()` |
|---|---|---|
| Behavior | Blocks thread | Suspends coroutine |
| Thread usage | Thread is occupied | Thread is released |
| Android UI | Can freeze UI | Does not freeze by itself |
| Cancellation | Not coroutine-friendly | Cancellation-friendly |

### Interview explanation

- "`Thread.sleep()` blocks the thread. `delay()` suspends the coroutine and lets the thread do other work."

---

## The `suspend` Keyword

### Meaning

- `suspend` marks a function that can pause and resume.
- A suspend function can call other suspend functions.
- A normal function cannot directly call a suspend function.
- It must be called from another suspend function or from a coroutine builder.

```kotlin
suspend fun getUser(): User {
    delay(1000)
    return api.getUser()
}
```

### What `suspend` does not mean

- It does not automatically create a background thread.
- It does not automatically make work parallel.
- It does not automatically handle exceptions.
- It only allows suspension.

### Bad CPU example

```kotlin
suspend fun badCpuWork(): Int {
    var sum = 0
    for (i in 1..100_000_000) {
        sum += i
    }
    return sum
}
```

- This is suspend, but still CPU-heavy.
- If called on Main, it can freeze UI.

### Correct CPU example

```kotlin
suspend fun goodCpuWork(): Int = withContext(Dispatchers.Default) {
    var sum = 0
    for (i in 1..100_000_000) {
        sum += i
    }
    sum
}
```

### Interview explanation

- "`suspend` means the function can suspend and resume. It does not mean the function automatically runs on a background thread."
