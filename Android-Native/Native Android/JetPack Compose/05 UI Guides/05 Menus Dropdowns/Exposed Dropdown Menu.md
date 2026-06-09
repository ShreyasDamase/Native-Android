# ExposedDropdownMenu

## 📌 Purpose
`ExposedDropdownMenu` (also known as a Spinner in classic Android) represents a selection field with an attached dropdown menu. The "exposed" part means the currently selected value (or typed text) is visible in a TextField, and tapping it reveals the list of choices.

> [!NOTE] Experimental API
> This component frequently lives under `@ExperimentalMaterial3Api` because its inner workings for text anchoring are still being refined.

## 🔧 Function Signature
To use it, you need to wrap your items inside an `ExposedDropdownMenuBox`, which sets up the layout anchoring scope.

```kotlin
@ExperimentalMaterial3Api
@Composable
fun ExposedDropdownMenuBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ExposedDropdownMenuBoxScope.() -> Unit
)
```

Inside the scope, you configure a `TextField` (using `Modifier.menuAnchor()`) and the `ExposedDropdownMenu`:

```kotlin
@Composable
fun ExposedDropdownMenuBoxScope.ExposedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit
)
```

## 📋 Props / Parameters

### `ExposedDropdownMenuBox`
| Parameter | Type | Default | Description |
|---|---|---|---|
| `expanded` | `Boolean` | — | Required. Whether the menu is expanded. |
| `onExpandedChange` | `(Boolean) -> Unit` | — | Required. Callback when the user clicks the box to expand/collapse. |
| `modifier` | `Modifier` | `Modifier` | Optional. Modifier for the box. |
| `content` | `ExposedDropdownMenuBoxScope.() -> Unit` | — | Required. Must contain the TextField anchor and the Menu. |

### `ExposedDropdownMenuBoxScope` Methods
| Function / Modifier | Description |
|---|---|
| `Modifier.menuAnchor(type: MenuAnchorType, enabled: Boolean)` | **CRITICAL:** Attached to the `TextField` to anchor the menu to it. |
| `ExposedDropdownMenuDefaults.TrailingIcon(expanded)` | Standard rotation arrow indicating expanded/collapsed state. |

### `MenuAnchorType` Enum
| Value | Description |
|---|---|
| `PrimaryNotEditable` | Use when `TextField` has `readOnly = true`. Standard selection dropdown. |
| `PrimaryEditable` | Use when `TextField` allows typing (searchable dropdown). |
| `SecondaryEditable` | Alternative selection logic when editing is secondary to selection. |

## ✅ Basic Example: Non-Editable Dropdown (Standard)
This is the standard drop-in replacement for a classic Android Spinner.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleExposedDropdown() {
    val options = listOf("Option 1", "Option 2", "Option 3")
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(options[0]) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        // The Anchor
        TextField(
            value = selected,
            onValueChange = {}, // Read-only, no-op
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        
        // The Menu
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        selected = option
                        expanded = false
                    }
                )
            }
        }
    }
}
```

## 🚀 Advanced Examples

### 1. Searchable / Editable Dropdown
Allows the user to type to filter the dropdown, or pick from the list. Notice `readOnly` is false and we use `MenuAnchorType.PrimaryEditable`.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableExposedDropdown() {
    val allOptions = listOf("Apple", "Banana", "Cherry", "Date", "Elderberry")
    var expanded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    
    // Filter options based on text input
    val filteredOptions = allOptions.filter { it.contains(text, ignoreCase = true) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { 
                text = it
                expanded = true // Keep open while typing
            },
            label = { Text("Search Fruits") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable)
        )
        
        // Only show if there are options or if they typed something
        if (filteredOptions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filteredOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            text = option
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
```

## ⚠️ Common Gotchas

> [!IMPORTANT] Modifier.menuAnchor() is Mandatory
> If you forget to attach `Modifier.menuAnchor()` to the `TextField` inside the `ExposedDropdownMenuBox`, the dropdown will open in a bizarre location (often the top left corner of the screen).

> [!WARNING] Matching MenuAnchorType
> If your `TextField` is `readOnly = true`, you **must** use `MenuAnchorType.PrimaryNotEditable`. Using an editable type on a read-only field causes focus and keyboard issues.

> [!NOTE] Scope containment
> `ExposedDropdownMenu` relies heavily on `ExposedDropdownMenuBoxScope`. You cannot place the dropdown completely outside the box composable without passing the scope context explicitly.

> [!TIP] Width matching
> By default, `ExposedDropdownMenu` automatically matches the exact width of its anchored `TextField`. You rarely need to override its width.

## 💡 Interview Q&A

**Q: How do you create a Spinner-like dropdown in Jetpack Compose?**
A: Use `ExposedDropdownMenuBox` containing a `TextField` with `readOnly=true` and an `ExposedDropdownMenu` containing `DropdownMenuItem`s.

**Q: What is the purpose of `ExposedDropdownMenuDefaults.TrailingIcon`?**
A: It provides the standard expanding/collapsing chevron icon (down arrow when closed, up arrow when open) and handles the rotation animation automatically based on the `expanded` boolean.

**Q: My ExposedDropdownMenu doesn't scroll when I have 50 items. Why?**
A: It should scroll automatically because `scrollState` defaults to `rememberScrollState()`. However, if your dropdown goes offscreen, make sure it is not constrained tightly by a parent column without scroll support. The menu popup usually manages its own height though.
