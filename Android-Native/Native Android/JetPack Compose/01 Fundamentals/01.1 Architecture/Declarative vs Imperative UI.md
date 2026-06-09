# 🔄 Declarative vs. Imperative UI

## 📌 Purpose
Jetpack Compose is a **Declarative UI** framework. Understanding the paradigm shift from the old **Imperative** XML View system is the most critical step to mastering Compose.

## 🏛️ Imperative UI (The Old Way - XML & Views)
In an imperative system, you construct a UI tree, and when the state changes, you manually instruct the UI components how to update themselves.

### The Imperative Mental Model: "How to do it"
1. You inflate an XML file.
2. You use `findViewById` to get references to UI nodes (e.g., a `TextView`).
3. You manually mutate the internal state of that node (e.g., `textView.text = "New State"`).

> [!WARNING] The Problem with Imperative
> UI state is scattered. The `TextView` holds its own state. The `ViewModel` holds the business state. If they get out of sync (e.g., you forget to call `setText` when data changes), you get UI bugs, overlapping data, and crashes.

## 🎨 Declarative UI (The Compose Way)
In a declarative system, you describe what the UI should look like for a given state. You **never manually update** a UI component. Instead, you update the state, and the framework recreates the UI to reflect that new state.

### The Declarative Mental Model: "What it should look like"
The core philosophy of Compose is:
**UI = f(state)**

The User Interface is a function of the State.
1. State goes in.
2. The Composable function executes.
3. UI comes out.

> [!TIP]
> In Compose, UI components do not have getter or setter methods (no `setText()`, no `setVisibility()`). You pass the state as a parameter to the Composable.

## ✅ Code Comparison

### ❌ Imperative Approach (Android Views)
```kotlin
// Setup UI
val button = findViewById<Button>(R.id.btn_follow)

// State is maintained in the UI itself and manually updated
fun updateFollowState(isFollowing: Boolean) {
    if (isFollowing) {
        button.text = "Unfollow"
        button.setBackgroundColor(Color.GRAY)
    } else {
        button.text = "Follow"
        button.setBackgroundColor(Color.BLUE)
    }
}
```

### ✨ Declarative Approach (Jetpack Compose)
```kotlin
@Composable
fun FollowButton(isFollowing: Boolean, onToggle: () -> Unit) {
    // UI is entirely dependent on the 'isFollowing' state parameter
    Button(
        onClick = onToggle,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFollowing) Color.Gray else Color.Blue
        )
    ) {
        Text(if (isFollowing) "Unfollow" else "Follow")
    }
}
```

## 🧠 Single Source of Truth
Because Compose UI components don't hold their own state, we enforce a **Single Source of Truth**.
The state lives in one place (usually a `ViewModel` or a state holder). The Composable observes it. When the state changes, the Composable is automatically re-run (recomposed) with the new data.

## ⚠️ Common Gotchas
*   **Trying to find views:** Android developers new to Compose often look for ways to "get a reference" to a `Text` or `TextField` to read its value. You can't. You must push the state up and pass it down as parameters.
*   **Modifying state inside the Composable without `remember`:** If you try to use a standard Kotlin variable `var count = 0` inside a Composable, it will reset to `0` every time the function recomposes. You must use `remember` and `State`.

## 💡 Interview Q&A

**Q: Explain UI = f(state).**
A: It means the User Interface is a direct mathematical result of the current State. If the state changes, the function runs again to produce a new UI. The UI cannot be changed independently of the state.

**Q: Why is Declarative UI considered less error-prone than Imperative UI?**
A: In Imperative UI, you have to manually keep the UI and the data model in sync, often leading to bugs if a specific state transition is missed. In Declarative UI, the framework guarantees that the UI perfectly matches the current state, eliminating a whole category of synchronization bugs.
