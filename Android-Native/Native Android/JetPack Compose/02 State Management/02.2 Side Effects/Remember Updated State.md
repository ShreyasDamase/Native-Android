# 5. `rememberUpdatedState`

## What is it?

Captures the **latest value** of a parameter inside a long-running effect **without restarting** the effect.

## The Problem it Solves

kotlin

```kotlin
// ❌ PROBLEM — onTimeout might be stale inside LaunchedEffect
LaunchedEffect(Unit) {
    delay(5000)
    onTimeout() // this might be old version of onTimeout
}
```

kotlin

```kotlin
// ✅ SOLUTION — always gets latest onTimeout
val currentOnTimeout by rememberUpdatedState(onTimeout)

LaunchedEffect(Unit) {
    delay(5000)
    currentOnTimeout() // always latest version
}
```

## Example

**Splash screen with callback:**

kotlin

```kotlin
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val currentOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        delay(3000)
        currentOnFinished() // uses latest onFinished even if it changed
    }
}
```

## When to use it

When you have a `LaunchedEffect(Unit)` (runs once) but it uses a **lambda or value that might change** during the delay.