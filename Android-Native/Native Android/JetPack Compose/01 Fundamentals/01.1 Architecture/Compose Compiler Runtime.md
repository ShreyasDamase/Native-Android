# 🛠️ Compose Compiler & Runtime

## 📌 Purpose
Jetpack Compose is entirely built around the Kotlin Compiler Plugin and the Compose Runtime. Understanding how `@Composable` functions are transformed and how the runtime tracks state is essential for writing performant Compose code and avoiding obscure bugs.

## 🧠 The Compose Compiler
The Compose Compiler is a Kotlin compiler plugin. It modifies the Abstract Syntax Tree (AST) of your code during compilation. When you annotate a function with `@Composable`, it isn't just a marker; it actively changes the function signature and its contents.

### 🔧 Function Transformation
When you write:
```kotlin
@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name")
}
```

The compiler transforms it into something resembling this (simplified):
```kotlin
fun Greeting(name: String, $composer: Composer, $changed: Int) {
    $composer.startRestartGroup(123456) // generated group ID
    
    // Check if we need to recompose
    if ($changed != 0 || !$composer.skipping) {
        Text(text = "Hello $name", $composer, ...)
    } else {
        $composer.skipToGroupEnd()
    }
    
    $composer.endRestartGroup()?.updateScope { nextComposer, _ ->
        Greeting(name, nextComposer, $changed)
    }
}
```

> [!NOTE] 
> The hidden `$composer` parameter is the backbone of Compose. It connects the function to the Compose Runtime's state management tree.

## ⚙️ The Compose Runtime & The Slot Table
The **Compose Runtime** is completely independent of Android UI. It is just a general-purpose tree management system.
To manage UI elements and states, the runtime uses a data structure called the **Slot Table** (or Gap Buffer).

### What is the Slot Table?
The Slot Table is an array-based data structure (similar to an array-backed gap buffer) that stores information about the composition. It tracks:
1. The type of Composables called.
2. The parameters passed to them.
3. The internal state (`remember` calls) of the composables.

> [!IMPORTANT]
> The Slot Table allows Compose to look up previously generated UI nodes and states in `O(1)` time based on their position in the tree.

### How `remember` works with the Slot Table
When you call `remember { ... }`, the compiler tells the `Composer` to store the value in the Slot Table at the current execution position. On the next recomposition, the runtime checks the Slot Table at that position and returns the stored value instead of evaluating the lambda again.

## 📍 Positional Memoization
Compose uses **Positional Memoization** to track state and Composables.

In normal functional programming, memoization is based on function arguments (e.g., caching the result of `fibonacci(5)`). In Compose, memoization is based on **the position of the function call in the source code**.

### Example of Positional Memoization
```kotlin
@Composable
fun Counter() {
    // Stored in the Slot Table at position "A"
    var count by remember { mutableStateOf(0) } 
    
    Button(onClick = { count++ }) {
        Text("Click me")
    }
}

@Composable
fun TwoCounters() {
    Counter() // Execution position 1
    Counter() // Execution position 2
}
```
Even though `Counter()` is the same function, because it is called from two different positions in `TwoCounters`, the Compose Compiler assigns them unique execution positions. The Runtime allocates separate slots in the Slot Table for each, resulting in two independent states!

> [!WARNING]
> If you use loops, Compose might lose track of positional identity if the list order changes. Always use the `key` composable or the `key` parameter in `LazyColumn` to manually assign an identity!

## 💡 Interview Q&A

**Q: What does the `@Composable` annotation actually do?**
A: It is not just an annotation for the IDE; it triggers a Kotlin Compiler plugin to rewrite the function signature, injecting a hidden `Composer` parameter and adding state-tracking code (like groups and restart scopes).

**Q: What happens if you call a Composable function from a normal Kotlin function?**
A: It won't compile because the normal function doesn't have the hidden `Composer` parameter required to pass down to the Composable function.

**Q: How does `remember` store data?**
A: `remember` stores data in the Compose Runtime's Slot Table based on the execution position of the function call (Positional Memoization).

**Q: What is a Gap Buffer / Slot Table?**
A: An array-backed data structure used by the Compose Runtime to store UI nodes, parameters, and states. It is optimized for fast insertions and deletions at the "gap", which corresponds to the current cursor position during composition.
