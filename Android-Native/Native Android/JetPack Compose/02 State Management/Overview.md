# 02 State Management Overview

## 📌 Purpose
State in an application is any value that can change over time. In Jetpack Compose, state is what drives the UI. When state changes, Compose automatically re-executes (recomposes) the parts of the UI that depend on that state. Understanding how to manage state correctly is critical for building performant and bug-free Android apps.

> [!NOTE]
> Jetpack Compose is a declarative UI framework. You don't update the UI manually (e.g., `textView.text = "Hello"`). Instead, you update the state, and the UI automatically updates to reflect it.

## 🔄 Unidirectional Data Flow (UDF)
Unidirectional Data Flow (UDF) is a design pattern where **state flows down** and **events flow up**.

In UDF:
- **State** is passed down from parent composables or ViewModels to child composables.
- **Events** (like button clicks) are passed up from child composables to parents via lambda functions.

> [!TIP]
> UDF ensures that there is a single source of truth for your state. This makes state easier to track, debug, and test, while reducing the likelihood of inconsistent UI states.

### UDF Example

```kotlin
@Composable
fun CounterScreen(viewModel: CounterViewModel = viewModel()) {
    // 1. State is observed from the ViewModel (Source of Truth)
    val count by viewModel.count.collectAsStateWithLifecycle()

    // 2. State flows down to the UI
    // 3. Events flow up to the ViewModel
    CounterContent(
        count = count,
        onIncrement = { viewModel.increment() } // Event flowing up
    )
}

@Composable
fun CounterContent(count: Int, onIncrement: () -> Unit) {
    Button(onClick = onIncrement) {
        Text("Count is: $count") // UI reacts to state flowing down
    }
}
```

## ⚖️ Stateful vs Stateless Composables

### Stateful Composables
A composable is **stateful** if it creates, holds, and modifies its own state internally using `remember` and `mutableStateOf`.

**Pros:** Self-contained, easy to use in simple scenarios.
**Cons:** Less reusable, harder to test, and tightly couples UI with logic.

```kotlin
@Composable
fun StatefulCounter() {
    // This composable holds its own state.
    var count by remember { mutableIntStateOf(0) }

    Button(onClick = { count++ }) {
        Text("Count: $count")
    }
}
```

### Stateless Composables
A composable is **stateless** if it doesn't hold any state of its own. It receives its state via parameters and communicates changes via event callbacks.

**Pros:** Highly reusable, easy to test, and purely presents data.
**Cons:** Requires you to manage state elsewhere (usually hoisted to a parent or ViewModel).

```kotlin
@Composable
fun StatelessCounter(
    count: Int,                // State flows down
    onIncrement: () -> Unit    // Event flows up
) {
    Button(onClick = onIncrement) {
        Text("Count: $count")
    }
}
```

> [!IMPORTANT]
> **Best Practice:** You should strive to make your core UI components (like buttons, cards, list items) stateless. Stateful composables should mostly be screen-level components that bridge ViewModels and stateless UI components.

## ⚠️ Common Gotchas

1. **Forgetting `remember`**: If you use `mutableStateOf` without `remember`, the state will be reset to its initial value on every recomposition.
2. **Mutating Objects**: If you put a standard `ArrayList` or a regular data class inside a `mutableStateOf` and modify its properties, Compose *will not* detect the change. You must either create a new copy of the object or use a Compose-aware collection like `mutableStateListOf`.
3. **Multiple Sources of Truth**: Passing state into a composable and then copying it into an internal `remember` block creates two competing sources of truth, leading to bugs. Always rely on the hoisted state.

## 💡 Interview Q&A

**Q: What triggers a recomposition in Jetpack Compose?**
A: Recomposition is triggered when the `State` object that a Composable function reads is updated. Compose tracks these reads automatically and only recomposes the functions that read the changed state.

**Q: What is Unidirectional Data Flow (UDF) and why is it useful in Compose?**
A: UDF is a pattern where state flows down from a source of truth, and events flow up from the UI to modify that state. It makes the application predictable, easier to test, and reduces bugs caused by inconsistent state across different parts of the UI.

**Q: What is the difference between a stateful and a stateless composable?**
A: A stateful composable creates and maintains its own internal state, making it less reusable. A stateless composable receives all its state via parameters and reports user interactions via callbacks, making it highly reusable and testable.
