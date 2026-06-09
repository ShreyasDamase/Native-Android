# 🎭 Composition vs. Recomposition

## 📌 Purpose
Understanding the difference between Initial Composition and Recomposition—and how Compose optimizes the latter—is the key to writing efficient Jetpack Compose applications.

## 🏗️ Initial Composition
**Initial Composition** is the process that happens the very first time your `@Composable` function is executed.
*   Compose creates the UI tree from scratch.
*   It allocates memory, initializes variables in the Slot Table, and runs all `remember` blocks.
*   It evaluates all `if/else` branches based on the initial state.

## ♻️ Recomposition
**Recomposition** is the process of updating the UI tree when the underlying State changes.
*   When a `State<T>` object changes, Compose identifies all Composable functions that *read* that state.
*   Compose re-executes **only those specific functions** with the new data.
*   It updates the existing nodes in the UI tree instead of throwing them away and rebuilding them.

> [!NOTE] 
> Recomposition is **Optimistic**. Compose expects recomposition to finish quickly. If the state changes again before the current recomposition finishes, Compose will cancel the ongoing recomposition and start over with the newest state.

## 🧠 Smart Recomposition & Skipping

Compose is designed to be highly optimized. It doesn't rebuild the whole screen; it practices **Smart Recomposition**.

When a parent Composable recomposes, Compose evaluates its child Composables.
If the parameters passed to a child Composable **have not changed**, Compose will **SKIP** recomposing that child.

### Example of Skipping
```kotlin
@Composable
fun ProfileScreen(user: User) {
    Column {
        // If user.name changes, this recomposes
        Header(name = user.name) 
        
        // If user.name changes, but the avatar URL stays the same,
        // Compose SKIPS this function entirely!
        Avatar(url = user.avatarUrl) 
    }
}
```

## ⚖️ Stability: The Key to Skipping

Compose can only skip a Composable if it can guarantee that its inputs haven't changed. To do this, Compose relies on the concept of **Stability**.

### Stable Types (Can be skipped)
Compose knows these types cannot change without Compose knowing about it.
*   Primitives (`Int`, `Float`, `Boolean`)
*   `String`
*   All functional types (lambdas)
*   Data classes containing only Stable properties
*   MutableState (`State<T>`)

### Unstable Types (Cannot be skipped)
Compose cannot track changes inside these types, so if an Unstable type is passed to a Composable, **it will NEVER be skipped** during recomposition.
*   Standard collections (`List`, `Set`, `Map`) - *Because a `List` might be a `MutableList` under the hood, Compose assumes it's unstable.*
*   Classes with `var` properties (that aren't `MutableState`).
*   Classes from external modules (unless annotated).

> [!CAUTION] 
> Passing a standard `List` to a Composable is the #1 cause of performance issues in Compose, because it forces the Composable to recompose every time its parent recomposes, even if the list contents are identical! Use `kotlinx.collections.immutable.ImmutableList` instead.

## ⚠️ Common Gotchas
*   **Unstable Parameters:** Wondering why your list items are recomposing constantly? Check your Layout Inspector. You are likely passing an unstable object (like a normal `List` or a domain model with a `var`) to your row Composable.
*   **Inline Lambdas with Unstable Captures:** If you pass a lambda to a child Composable, and that lambda references an unstable variable from the parent, the lambda itself becomes unstable, forcing the child to recompose.

## 💡 Interview Q&A

**Q: What is the difference between Composition and Recomposition?**
A: Composition is the initial creation of the UI tree. Recomposition is the process of updating specific parts of that tree when state changes, while leaving the rest untouched.

**Q: What does it mean that recomposition is optimistic?**
A: It means Compose assumes it will finish before state changes again. If state does change mid-recomposition, it cancels the current pass and restarts, ensuring the UI always reflects the absolute latest state without wasting time finishing an outdated render.

**Q: How does Compose decide whether to skip a Composable during recomposition?**
A: It checks the equality of the input parameters. If all inputs are "Stable" and equal to their previous values (`a == b`), Compose skips executing the function. If any input is "Unstable", Compose is forced to recompose it just in case.
