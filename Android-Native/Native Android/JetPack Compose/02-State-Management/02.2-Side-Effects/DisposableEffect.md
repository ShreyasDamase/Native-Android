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

```kotlin
DisposableEffect(Unit) {
    analytics.trackScreenEnter("ProfileScreen")

    onDispose {
        analytics.trackScreenExit("ProfileScreen")
    }
}
```

## Key Rule

**Always clean up in `onDispose`** — if you register something, unregister it. If you start something, stop it.