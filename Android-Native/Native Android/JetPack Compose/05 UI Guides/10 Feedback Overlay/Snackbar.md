# Snackbar

## 📌 Purpose
A `Snackbar` provides brief messages about app processes at the bottom of the screen. They can include an action (like "Undo" or "Retry") and an optional dismiss button. 

In Jetpack Compose, you typically do not use the `Snackbar` composable directly. Instead, you use a `SnackbarHost` paired with a `SnackbarHostState`, and trigger messages via coroutines.

> [!NOTE]
> `Snackbar` is transient and should not be used for critical error messages or alerts that require immediate user input (use a dialog instead).

## 🔧 Function Signatures

### 1. SnackbarHost
```kotlin
@Composable
fun SnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (SnackbarData) -> Unit = { Snackbar(it) }
)
```

### 2. Snackbar (Direct Composable)
```kotlin
@Composable
fun Snackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    actionOnNewLine: Boolean = false,
    shape: Shape = SnackbarDefaults.shape,
    containerColor: Color = SnackbarDefaults.color,
    contentColor: Color = SnackbarDefaults.contentColor,
    actionColor: Color = SnackbarDefaults.actionColor,
    actionContentColor: Color = SnackbarDefaults.actionContentColor,
    dismissActionContentColor: Color = SnackbarDefaults.dismissActionContentColor
)
```

### 3. showSnackbar (Suspend Function)
```kotlin
suspend fun SnackbarHostState.showSnackbar(
    message: String,
    actionLabel: String? = null,
    withDismissAction: Boolean = false,
    duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Indefinite
): SnackbarResult
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `hostState` | `SnackbarHostState` | — | The state object that manages the queue of snackbars. |
| `snackbar` | `@Composable (SnackbarData) -> Unit` | `Snackbar(it)` | The visual representation of the snackbar. Override this for custom designs. |
| `message` | `String` | — | The text to display. |
| `actionLabel` | `String?` | `null` | The text for the action button (e.g., "UNDO"). |
| `withDismissAction` | `Boolean` | `false` | Whether to show an 'X' button to dismiss the snackbar. |
| `duration` | `SnackbarDuration` | `Short` / `Indefinite` | How long to show it (`Short` = 4s, `Long` = 10s, `Indefinite`). |

## ✅ Basic Example

```kotlin
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@Composable
fun BasicSnackbar() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Show snackbar") },
                icon = { Icon(Icons.Filled.Info, contentDescription = "") },
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Hello from Snackbar!")
                    }
                }
            )
        }
    ) { innerPadding ->
        // content using innerPadding
        Text("Main Content", Modifier.padding(innerPadding))
    }
}
```

## 🚀 Advanced Examples

### 1. Snackbar with Action ("Undo")
```kotlin
@Composable
fun ActionSnackbar(snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
    Button(onClick = {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Message deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    // Restore the message
                }
                SnackbarResult.Dismissed -> {
                    // Message permanently deleted
                }
            }
        }
    }) {
        Text("Delete Message")
    }
}
```

### 2. Snackbar with Dismiss Button
```kotlin
@Composable
fun DismissibleSnackbar(snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
    Button(onClick = {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = "Task finished successfully.",
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
        }
    }) {
        Text("Show Task Status")
    }
}
```

### 3. Custom Styled Snackbar (Error State)
You can customize the appearance by passing a custom `snackbar` lambda to the `SnackbarHost`.

```kotlin
@Composable
fun CustomErrorSnackbarHost(snackbarHostState: SnackbarHostState) {
    SnackbarHost(hostState = snackbarHostState) { snackbarData ->
        Snackbar(
            snackbarData = snackbarData,
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            actionColor = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
```

## ⚠️ Common Gotchas
- **Coroutine Scope:** `showSnackbar` is a `suspend` function. You **must** call it from a Coroutine (usually launched from a `rememberCoroutineScope()`).
- **Scaffold integration:** If you put the `SnackbarHost` directly in a Box without using `Scaffold`, it won't automatically shift Floating Action Buttons or Navigation Bars out of the way. Always use `Scaffold`'s `snackbarHost` slot!
- **Queueing:** `showSnackbar` blocks the coroutine until the snackbar is dismissed. If you fire three `showSnackbar` calls sequentially in the *same* coroutine, they will queue up and show one after another.

## 💡 Interview Q&A

**Q: How do you know if the user clicked the action button on a Snackbar?**
A: `showSnackbar` returns a `SnackbarResult` enum. You can check if `result == SnackbarResult.ActionPerformed` to execute the action, or `SnackbarResult.Dismissed` if it timed out or was swiped away.

**Q: Why does `showSnackbar` have to be a suspend function?**
A: Because snackbars inherently involve time. The function suspends the coroutine until the snackbar finishes its duration or is interacted with. This makes it trivial to handle sequential messages or wait for a user's action before continuing logic.
