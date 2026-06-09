# DropdownMenu

## 📌 Purpose
`DropdownMenu` is a compact menu that appears as a popup anchored to another composable, usually an action button or an icon. It allows users to make a selection from a list of options. `DropdownMenuItem` is used to represent the individual actionable options within the menu.

## 🔧 Function Signature
```kotlin
@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    shape: Shape = MenuDefaults.shape,
    containerColor: Color = MenuDefaults.containerColor,
    tonalElevation: Dp = MenuDefaults.TonalElevation,
    shadowElevation: Dp = MenuDefaults.ShadowElevation,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
)
```

```kotlin
@Composable
fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null
)
```

## 📋 Props / Parameters

### `DropdownMenu`
| Parameter | Type | Default | Description |
|---|---|---|---|
| `expanded` | `Boolean` | — | Required. Whether the menu is currently visible. |
| `onDismissRequest` | `() -> Unit` | — | Required. Callback when the user taps outside the menu to dismiss it. |
| `modifier` | `Modifier` | `Modifier` | Optional. Modifier for the menu's container layout. |
| `offset` | `DpOffset` | `DpOffset(0.dp, 0.dp)` | Optional. Offset to position the menu precisely relative to its anchor. |
| `scrollState` | `ScrollState` | `rememberScrollState()` | Optional. The scroll state used if the menu items exceed max height. |
| `properties` | `PopupProperties` | `PopupProperties(focusable = true)` | Optional. Properties for underlying popup (e.g. dismissing on back press). |
| `shape` | `Shape` | `MenuDefaults.shape` | Optional. Shape of the menu's background. |
| `containerColor` | `Color` | `MenuDefaults.containerColor` | Optional. The background color of the menu. |
| `tonalElevation` | `Dp` | `MenuDefaults.TonalElevation` | Optional. Surface tonal elevation. |
| `shadowElevation` | `Dp` | `MenuDefaults.ShadowElevation` | Optional. Size of the drop shadow. |
| `border` | `BorderStroke?` | `null` | Optional. Border drawn around the menu container. |
| `content` | `@Composable ColumnScope.() -> Unit` | — | Required. The content of the menu, usually `DropdownMenuItem`s. |

### `DropdownMenuItem`
| Parameter | Type | Default | Description |
|---|---|---|---|
| `text` | `@Composable () -> Unit` | — | Required. The main text content of the item. |
| `onClick` | `() -> Unit` | — | Required. Callback when this item is clicked. |
| `modifier` | `Modifier` | `Modifier` | Optional. Layout modifier for this item. |
| `leadingIcon` | `@Composable (() -> Unit)?` | `null` | Optional. Icon placed before the text. |
| `trailingIcon` | `@Composable (() -> Unit)?` | `null` | Optional. Icon or text placed after the main text (e.g., for keyboard shortcuts). |
| `enabled` | `Boolean` | `true` | Optional. If false, item is grayed out and unclickable. |
| `colors` | `MenuItemColors` | `MenuDefaults.itemColors()` | Optional. Colors for the text and icons based on enabled state. |
| `contentPadding` | `PaddingValues` | `MenuDefaults.DropdownMenuItemContentPadding` | Optional. Padding inside the item's container. |
| `interactionSource` | `MutableInteractionSource?` | `null` | Optional. Custom interaction source to track clicks/hovers. |

## ✅ Basic Example
```kotlin
@Composable
fun SimpleOverflowMenu() {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Settings") },
                onClick = { /* Do something */ expanded = false }
            )
            DropdownMenuItem(
                text = { Text("Help") },
                onClick = { /* Do something */ expanded = false }
            )
        }
    }
}
```

## 🚀 Advanced Examples

### 1. Menu with Icons (`leadingIcon`)
```kotlin
@Composable
fun IconDropdownMenu() {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(onClick = { expanded = true }) { Text("Actions") }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = { expanded = false },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = { expanded = false },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
            )
        }
    }
}
```

### 2. Menu with Keyboard Shortcuts (`trailingIcon`)
```kotlin
@Composable
fun ShortcutDropdownMenu() {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) { Text("File") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Copy") },
                onClick = { expanded = false },
                trailingIcon = { Text("⌘C", color = Color.Gray) }
            )
            DropdownMenuItem(
                text = { Text("Paste") },
                onClick = { expanded = false },
                trailingIcon = { Text("⌘V", color = Color.Gray) }
            )
        }
    }
}
```

### 3. Menu with Dividers
```kotlin
@Composable
fun DividedDropdownMenu() {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Profile") }, onClick = { expanded = false })
            DropdownMenuItem(text = { Text("Settings") }, onClick = { expanded = false })
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Sign out", color = Color.Red) },
                onClick = { expanded = false }
            )
        }
    }
}
```

### 4. Nested / Cascading Menu
```kotlin
@Composable
fun NestedDropdownMenu() {
    var mainExpanded by remember { mutableStateOf(false) }
    var subExpanded by remember { mutableStateOf(false) }

    Box {
        Button(onClick = { mainExpanded = true }) { Text("Menu") }
        DropdownMenu(expanded = mainExpanded, onDismissRequest = { mainExpanded = false }) {
            DropdownMenuItem(text = { Text("Option 1") }, onClick = { mainExpanded = false })
            
            // Nested trigger
            Box {
                DropdownMenuItem(
                    text = { Text("More Options...") },
                    onClick = { subExpanded = true },
                    trailingIcon = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) }
                )
                
                DropdownMenu(
                    expanded = subExpanded,
                    onDismissRequest = { subExpanded = false },
                    offset = DpOffset(100.dp, 0.dp) // Offset to show next to main menu
                ) {
                    DropdownMenuItem(text = { Text("Sub Option A") }, onClick = { subExpanded = false; mainExpanded = false })
                    DropdownMenuItem(text = { Text("Sub Option B") }, onClick = { subExpanded = false; mainExpanded = false })
                }
            }
        }
    }
}
```

## ⚠️ Common Gotchas

> [!WARNING] The anchoring mechanism
> `DropdownMenu` anchors to its immediate **parent**, not specifically to the button that triggered it. You **must** wrap the trigger (like `IconButton`) and the `DropdownMenu` inside a `Box` or another container so they share the exact same origin coordinate system.

> [!CAUTION] Forgetting to close
> Always remember to set your `expanded` state back to `false` in **both** the `onDismissRequest` (when user clicks outside) and `onClick` of your `DropdownMenuItem`s.

> [!TIP] Tweak `offset`
> If the menu overlaps the button in a weird way, adjust `offset = DpOffset(x.dp, y.dp)` on the `DropdownMenu` to shift it horizontally or vertically.

## 💡 Interview Q&A

**Q: Why does the DropdownMenu show up at the top-left of the screen instead of under my button?**
A: Because the `DropdownMenu` isn't placed in the same layout boundary as the button. They must be grouped in a `Box` where the button defines the anchor points for the menu.

**Q: How do you implement a non-clickable section header inside a DropdownMenu?**
A: Since `content` is a `ColumnScope`, you can simply insert standard composables like `Text` or `Box` directly in the menu instead of using `DropdownMenuItem`. Apply padding to match the standard item design.
