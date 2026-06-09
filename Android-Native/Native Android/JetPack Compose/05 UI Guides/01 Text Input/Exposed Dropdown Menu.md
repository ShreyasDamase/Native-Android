# 🔤 ExposedDropdownMenu

## 📌 Purpose
`ExposedDropdownMenu` (often just called a Dropdown) is a Material 3 component for selecting a value from a predefined list. It consists of an anchor (usually a `TextField`) that displays the current selection, and a popup menu that overlays the screen. 

Building a dropdown in Compose can be notoriously tricky, so this guide provides step-by-step examples.

## 🔧 Function Signatures

### 1. The Container (`ExposedDropdownMenuBox`)
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

### 2. The Anchor Modifier (`menuAnchor`)
Used inside the `ExposedDropdownMenuBoxScope` on the `TextField`.
```kotlin
fun Modifier.menuAnchor(
    type: MenuAnchorType = MenuAnchorType.PrimaryNotEditable,
    enabled: Boolean = true
): Modifier
```

### 3. The Menu (`ExposedDropdownMenu`)
Used inside the `ExposedDropdownMenuBoxScope`.
```kotlin
@Composable
fun ExposedDropdownMenuBoxScope.ExposedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    matchTextFieldWidth: Boolean = true,
    shape: Shape = MenuDefaults.shape,
    containerColor: Color = MenuDefaults.containerColor,
    tonalElevation: Dp = MenuDefaults.TonalElevation,
    shadowElevation: Dp = MenuDefaults.ShadowElevation,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
)
```

## 📋 Props / Parameters (ExposedDropdownMenuBox)

| Parameter | Type | Default | Description |
|---|---|---|---|
| expanded | `Boolean` | — | Required. Whether the popup menu is currently shown. |
| onExpandedChange | `(Boolean) -> Unit` | — | Required. Called when the user clicks the anchor field. |
| modifier | `Modifier` | `Modifier` | Optional. |
| content | `@Composable` | — | Required. The anchor `TextField` and the `ExposedDropdownMenu`. |

## ✅ Basic Example: Step-by-Step Dropdown

1. Create state for `expanded` and `selectedOption`.
2. Wrap everything in `ExposedDropdownMenuBox`.
3. Add a `TextField` (or `OutlinedTextField`) with `Modifier.menuAnchor()`. Make it `readOnly`.
4. Add the `ExposedDropdownMenu`.
5. Iterate through options to create `DropdownMenuItem`s.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDropdown() {
    val options = listOf("Option 1", "Option 2", "Option 3")
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf(options[0]) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        // The Anchor
        OutlinedTextField(
            // Important: Dropdown is usually read-only
            readOnly = true,
            value = selectedOptionText,
            onValueChange = {},
            label = { Text("Label") },
            trailingIcon = {
                // Automates the rotation of the chevron arrow
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                // IMPORTANT: Must apply menuAnchor to make it clickable and position the menu
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )

        // The Menu
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        selectedOptionText = selectionOption
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
```

## 🚀 Advanced Examples

### 1. Searchable / Editable Dropdown
Allows the user to type in the field to filter the options.
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableDropdown() {
    val options = listOf("Apple", "Banana", "Cherry", "Date", "Elderberry")
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    
    // Filter options based on query
    val filteredOptions = options.filter { it.contains(query, ignoreCase = true) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { 
                query = it
                expanded = true // Keep open while typing
            },
            modifier = Modifier
                .fillMaxWidth()
                // Use PrimaryEditable since we allow typing
                .menuAnchor(MenuAnchorType.PrimaryEditable),
            label = { Text("Search Fruit") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        if (filteredOptions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filteredOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            query = option
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
```

### 2. Multi-Select Dropdown
Selecting multiple options without closing the menu.
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectDropdown() {
    val options = listOf("Red", "Green", "Blue", "Yellow")
    var expanded by remember { mutableStateOf(false) }
    var selectedOptions by remember { mutableStateOf(setOf<String>()) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedOptions.joinToString(", ").ifEmpty { "Select colors" },
            onValueChange = {},
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                val isSelected = selectedOptions.contains(option)
                DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Text(option)
                        }
                    },
                    onClick = {
                        selectedOptions = if (isSelected) {
                            selectedOptions - option
                        } else {
                            selectedOptions + option
                        }
                        // Do NOT set expanded = false here to allow multi-select
                    }
                )
            }
        }
    }
}
```

## ⚠️ Common Gotchas
- **Forgetting `Modifier.menuAnchor()`:** If you forget this modifier on the `TextField`, tapping the text field will not open the menu, and the menu might appear in the wrong position on the screen.
- **Wrong `MenuAnchorType`:** If the field is `readOnly = true`, use `MenuAnchorType.PrimaryNotEditable`. If it's an editable search field, use `MenuAnchorType.PrimaryEditable`. This affects keyboard handling and focus.
- **Menu not dismissing:** Make sure you call `expanded = false` inside `onDismissRequest` of the `ExposedDropdownMenu` and in the `onClick` of your `DropdownMenuItem`s.
- **Width mismatch:** By default, `ExposedDropdownMenu` matches the width of the anchor `TextField`. If you don't want this, set `matchTextFieldWidth = false` in `ExposedDropdownMenu`.

## 💡 Interview Q&A

**Q: How does `ExposedDropdownMenuBox` know where to show the popup?**
A: The `Modifier.menuAnchor()` applied to the anchor child passes layout coordinates up to the `ExposedDropdownMenuBoxScope`, which then positions the `ExposedDropdownMenu` popup relative to that anchor.
