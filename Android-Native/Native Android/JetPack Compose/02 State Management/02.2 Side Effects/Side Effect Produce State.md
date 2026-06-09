# Side Effects in Jetpack Compose — Deep Dive

---

# 1. `LaunchedEffect`

## What is it?

A composable that launches a **coroutine** tied to the composable's lifecycle. When the composable leaves the screen the coroutine is **automatically cancelled**.

## Key Parameter — `key`

The key controls **when the effect re-runs**:

kotlin

```kotlin
LaunchedEffect(Unit)        // runs ONCE, never re-runs
LaunchedEffect(true)        // runs ONCE, never re-runs
LaunchedEffect(userId)      // re-runs when userId changes
LaunchedEffect(key1, key2)  // re-runs when either changes
```

## Examples

**Load data on screen open:**

kotlin

```kotlin
LaunchedEffect(Unit) {
    viewModel.getUserProfile()
}
```

**Re-run when value changes:**

kotlin

```kotlin
LaunchedEffect(searchQuery) {
    viewModel.searchBooks(searchQuery) // re-runs on every new query
}
```

**Navigate after login:**

kotlin

```kotlin
LaunchedEffect(loginSuccess) {
    if (loginSuccess) {
        navController.navigate("home")
    }
}
```

**Show snackbar:**

kotlin

```kotlin
LaunchedEffect(errorMessage) {
    if (errorMessage != null) {
        snackbarHostState.showSnackbar(errorMessage)
    }
}
```

**Delay / Timer:**

kotlin

````kotlin
LaunchedEffect(Unit) {
    delay(3000)
    navController.navigate("home") // auto navigate after 3 seconds
}
```

## What happens when key changes?
```
key = "A" → coroutine starts
key changes to "B" → old coroutine CANCELLED → new coroutine starts
composable leaves screen → coroutine CANCELLED
````

---

# 2. `DisposableEffect`

## What is it?

Like `LaunchedEffect` but for **non-coroutine** work that needs **cleanup** when the composable leaves the screen. It has a mandatory `onDispose` block.

## Structure

kotlin

```kotlin
DisposableEffect(key) {
    // SETUP — runs when composable enters

    onDispose {
        // CLEANUP — runs when composable leaves
    }
}
```

## Examples

**Register/Unregister a listener:**

kotlin

```kotlin
DisposableEffect(Unit) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            viewModel.refresh()
        }
    }
    lifecycle.addObserver(observer)

    onDispose {
        lifecycle.removeObserver(observer) // cleanup
    }
}
```

**Location updates:**

kotlin

```kotlin
DisposableEffect(Unit) {
    val locationManager = context.getSystemService(LocationManager::class.java)
    val listener = LocationListener { location ->
        viewModel.updateLocation(location)
    }
    locationManager.requestUpdates(listener)

    onDispose {
        locationManager.removeUpdates(listener) // stop when screen leaves
    }
}
```

**Analytics screen tracking:**

kotlin

````kotlin
DisposableEffect(Unit) {
    analytics.trackScreenEnter("ProfileScreen")

    onDispose {
        analytics.trackScreenExit("ProfileScreen")
    }
}
```

## Key Rule
**Always clean up in `onDispose`** — if you register something, unregister it. If you start something, stop it.

---

# 3. `SideEffect`

## What is it?
Runs on **every recomposition**. Used to **sync Compose state with non-Compose code**. It is NOT a coroutine — it runs synchronously.

## When does it run?
```
Screen opens → runs
User types something → recomposition → runs again
State changes → recomposition → runs again
````

## Examples

**Update system UI (status bar color):**

kotlin

```kotlin
val systemUiController = rememberSystemUiController()
val darkTheme = isSystemInDarkTheme()

SideEffect {
    systemUiController.setStatusBarColor(
        color = if (darkTheme) Color.Black else Color.White
    )
}
```

**Pass Compose state to non-Compose code:**

kotlin

```kotlin
SideEffect {
    // keep external non-compose analytics in sync
    FirebaseAnalytics.setUserProperty("theme", currentTheme)
}
```

## Important

Do NOT do heavy work or API calls here — it runs on **every recomposition** which can be very frequent.