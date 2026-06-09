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

```kotlin
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
```