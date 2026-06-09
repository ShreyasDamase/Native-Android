# AlertDialog

## 📌 Purpose
`AlertDialog` is a structured popup that interrupts the user with urgent information, details, or a request for a decision (like a confirmation or permission prompt). Material Design enforces a specific layout: optional icon, title, text body, and action buttons (confirm/dismiss). 

If you need a completely custom, non-standard layout inside a dialog, `BasicAlertDialog` is available to provide just the dialog window behavior.

## 🔧 Function Signature

```kotlin
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties()
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `onDismissRequest` | `() -> Unit` | — | Required. Called when user taps outside the dialog or presses Back. |
| `confirmButton` | `@Composable () -> Unit` | — | Required. The primary action button (e.g., "OK", "Delete"). |
| `modifier` | `Modifier` | `Modifier` | Optional. Modifier applied to the dialog surface. |
| `dismissButton` | `@Composable (() -> Unit)?` | `null` | Optional. The secondary action button (e.g., "Cancel"). |
| `icon` | `@Composable (() -> Unit)?` | `null` | Optional. Icon placed at the top center. |
| `title` | `@Composable (() -> Unit)?` | `null` | Optional. Header text. |
| `text` | `@Composable (() -> Unit)?` | `null` | Optional. Main body text. |
| `properties` | `DialogProperties` | `DialogProperties()` | Optional. Behavior config (e.g., back-press dismiss). |

### `DialogProperties`
Key properties to know:
- `dismissOnBackPress: Boolean` (default `true`)
- `dismissOnClickOutside: Boolean` (default `true`)
- `usePlatformDefaultWidth: Boolean` (default `true`, restricts max width to Material guidelines. Set `false` for full-screen dialogs)
- `securePolicy: SecureFlagPolicy` (to block screenshots)

## ✅ Basic Example: Confirmation Dialog
```kotlin
@Composable
fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Item?") },
        text = { Text("Are you sure you want to delete this item? This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

## 🚀 Advanced Examples

### 1. Info Dialog with Icon
```kotlin
@Composable
fun SyncCompleteDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green) },
        title = { Text("Sync Complete") },
        text = { Text("All your files have been successfully backed up to the cloud.") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}
```

### 2. Dialog with TextField Inside (Rename Dialog)
For simple inputs inside the dialog body.
```kotlin
@Composable
fun RenameDialog(initialName: String, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Folder") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Folder Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onRename(text) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
```

### 3. BasicAlertDialog (Fully Custom Layout)
`BasicAlertDialog` (`@ExperimentalMaterial3Api`) has no predefined slots. You provide a single `content` block.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRatingDialog(onDismiss: () -> Unit) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Rate your experience", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Row {
                    repeat(5) { 
                        Icon(Icons.Default.Star, contentDescription = "Star", tint = Color.Yellow)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss) { Text("Submit") }
            }
        }
    }
}
```

## ⚠️ Common Gotchas

> [!WARNING] State Hoisting
> `AlertDialog` itself doesn't manage its visibility. You must wrap the dialog call in an `if (showDialog)` condition and pass a state mutator down to `onDismissRequest` to change `showDialog = false`.

> [!IMPORTANT] `usePlatformDefaultWidth`
> If you are trying to make a dialog that stretches edge-to-edge across the screen, it won't work by default because Material enforces a maximum width. You must set `DialogProperties(usePlatformDefaultWidth = false)` to remove these constraints.

> [!CAUTION] Handling Back Press
> If you set `dismissOnBackPress = false`, make sure you provide the user an explicit button to cancel or dismiss the dialog, otherwise they could become trapped.

## 💡 Interview Q&A

**Q: What is the difference between `AlertDialog` and `BasicAlertDialog`?**
A: `AlertDialog` forces you into the Material Design template with slots for `title`, `text`, `icon`, `confirmButton`, etc. `BasicAlertDialog` provides a blank slate window wrapper, allowing you to compose literally anything inside it without layout constraints.

**Q: How do you prevent a dialog from closing when the user taps outside of it?**
A: Use `properties = DialogProperties(dismissOnClickOutside = false)` within the dialog parameters.
