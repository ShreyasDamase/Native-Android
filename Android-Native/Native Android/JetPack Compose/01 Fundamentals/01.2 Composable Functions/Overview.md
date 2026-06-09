# 🧩 Composable Functions Overview

## 📌 Purpose
The `@Composable` annotation is the fundamental building block of Jetpack Compose. It marks a function as a node in the UI tree. But a Composable function is not a normal Kotlin function; it operates under a strict set of rules enforced by the Compose Compiler.

## 🔧 Function Signature
```kotlin
@Composable
fun ComponentName(
    param1: Type,
    param2: Type = default,
)
```
*   Must be annotated with `@Composable`.
*   Usually returns `Unit` (they emit UI, they don't return objects).
*   Name must be `PascalCase` (Capitalized) if it emits UI (e.g., `ProfileImage`), or `camelCase` if it returns a value (e.g., `rememberState()`).

## 🧬 Core Characteristics of a Composable

To be efficient and predictable, Composable functions must adhere to these traits:

### 1. Fast & Lightweight
Composables can be called frequently—sometimes on every single frame of an animation (60 or 120 times per second). They should not do heavy computations.

### 2. Idempotent
Calling a Composable multiple times with the same arguments must produce the exact same result (the same UI nodes).
*   `f(x) = y`
*   Calling it 100 times should still result in `y` without changing the app's state.

### 3. Side-Effect Free
A Composable function should not have side effects. A side effect is any change to the state of the app that escapes the scope of the function.
*   ❌ Writing to `SharedPreferences` inside the Composable.
*   ❌ Modifying a global variable.
*   ❌ Firing a network request.
If you need side effects, you MUST use Compose's Effect API (e.g., `LaunchedEffect`, `DisposableEffect`).

### 4. Positionally Aware
Thanks to the Compose Compiler, Composables are aware of where they are called from in the source code. This allows them to store and retrieve state (`remember`) securely across recompositions.

### 5. Can Execute in Any Order
Compose can execute sibling composables in parallel or in an unpredictable order to optimize rendering.
```kotlin
@Composable
fun TabRow() {
    Tab1()
    Tab2()
    Tab3()
}
// Do not assume Tab1 finishes before Tab2 starts!
```

## ✅ Basic Example
```kotlin
@Composable
fun GreetingCard(name: String, modifier: Modifier = Modifier) {
    // Emits a UI node (Card)
    Card(modifier = modifier.padding(8.dp)) {
        // Emits a child UI node (Text)
        Text(
            text = "Hello, $name!",
            modifier = Modifier.padding(16.dp)
        )
    }
}
```

## ⚠️ Common Gotchas
*   **Returning Views:** Beginners often try to make a `@Composable` return a `View` or a Compose node. Composables emit UI to the composer; they do not return UI objects. Return `Unit`.
*   **Heavy Logic:** Putting sorting algorithms or database reads directly inside the Composable. This blocks the main thread and ruins UI performance during recomposition.

## 💡 Interview Q&A

**Q: Can a Composable function return a value?**
A: Yes. While Composables that emit UI return `Unit` by convention, helper Composables can return values. For example, `remember { ... }` or `stringResource(...)` are Composable functions that return objects/primitives.

**Q: Why must Composables be side-effect free?**
A: Because Composables can run multiple times (recomposition), in parallel, or be skipped entirely. If a Composable writes to a database, that database write might happen 100 times unexpectedly, or not happen at all when you thought it would.

**Q: What happens if you forget the `@Composable` annotation on a function that calls `Text()`?**
A: The code will not compile. The Kotlin compiler will throw an error saying that `@Composable` invocations can only happen from the context of a `@Composable` function.
