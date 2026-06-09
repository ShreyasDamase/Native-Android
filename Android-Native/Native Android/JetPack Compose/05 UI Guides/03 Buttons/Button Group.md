# 🔘 ButtonGroup

## 📌 Purpose
`ButtonGroup` is a **Material 3 Expressive** component (introduced in M3 1.4.x) that arranges multiple related buttons horizontally. It effectively replaces the older `SegmentedButton` for many use cases. It supports displaying up to a maximum number of items, with overflowing items automatically moved into a dropdown menu.

> [!WARNING]
> This is an Experimental API and requires `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`.

## 🔧 Function Signature

```kotlin
@ExperimentalMaterial3ExpressiveApi
@Composable
fun ButtonGroup(
    overflowIndicator: @Composable (ButtonGroupMenuState) -> Unit,
    modifier: Modifier = Modifier,
    expandedRatio: Float = ButtonGroupDefaults.ExpandedRatio,
    horizontalArrangement: Arrangement.Horizontal = ButtonGroupDefaults.HorizontalArrangement,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable ButtonGroupScope.() -> Unit
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| overflowIndicator | `@Composable (ButtonGroupMenuState) -> Unit` | — | Required. The button (usually a dropdown chevron) that opens the menu for items that don't fit. |
| modifier | `Modifier` | `Modifier` | Optional. |
| expandedRatio | `Float` | `0.5f` | Optional. Defines how much space the group should take before collapsing items into the overflow. |
| horizontalArrangement | `Arrangement.Horizontal` | `Arrangement.spacedBy(8.dp)` | Optional. Spacing between the buttons. |
| verticalAlignment | `Alignment.Vertical` | `Alignment.Top` | Optional. |
| content | `@Composable ButtonGroupScope.() -> Unit` | — | Required. The buttons to display. |

## ✅ Basic Example

Inside the `content`, you use `ButtonGroupScope.buttonGroupItem()` to wrap each individual button.

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SimpleButtonGroup() {
    val options = listOf("Reply", "Reply All", "Forward")
    
    ButtonGroup(
        overflowIndicator = { state ->
            // A simple button that shows the dropdown when clicked
            IconButton(onClick = { state.expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
        }
    ) {
        options.forEach { option ->
            buttonGroupItem {
                Button(onClick = { }) {
                    Text(option)
                }
            }
        }
    }
}
```

## 🚀 Advanced Examples

### Migration from SegmentedButton
The older `SegmentedButton` forced buttons into a connected pill shape. `ButtonGroup` spaces them out dynamically. Here is a pattern for a filter selection.

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FilterButtonGroup() {
    val days = listOf("Today", "Yesterday", "Last 7 Days", "Last 30 Days", "All Time")
    var selectedDay by remember { mutableStateOf(days[0]) }

    ButtonGroup(
        modifier = Modifier.fillMaxWidth(),
        overflowIndicator = { state ->
            // Create a dropdown menu for overflowing items
            Box {
                OutlinedIconButton(onClick = { state.expanded = !state.expanded }) {
                    Icon(Icons.Default.MoreHoriz, "More filters")
                }
                
                DropdownMenu(
                    expanded = state.expanded,
                    onDismissRequest = { state.expanded = false }
                ) {
                    // Logic to show remaining days here
                    // (Note: full automatic overflow menu mapping requires manual state sync)
                    DropdownMenuItem(
                        text = { Text("More filters...") },
                        onClick = { state.expanded = false }
                    )
                }
            }
        }
    ) {
        days.forEach { day ->
            buttonGroupItem {
                val isSelected = day == selectedDay
                if (isSelected) {
                    FilledTonalButton(onClick = { selectedDay = day }) {
                        Text(day)
                    }
                } else {
                    OutlinedButton(onClick = { selectedDay = day }) {
                        Text(day)
                    }
                }
            }
        }
    }
}
```

## ⚠️ Common Gotchas
- **`ButtonGroupScope`**: You **must** wrap your buttons in the `buttonGroupItem { ... }` block inside the `ButtonGroup`. This allows the layout to measure and decide which items overflow.
- **Experimental Flag:** Because this is part of the Expressive API, its API surface might change. 
- **Overflow Menu Logic:** The `ButtonGroup` component calculates the space, but it does *not* automatically generate the Dropdown menu contents for the hidden items. You have to implement the `DropdownMenu` inside the `overflowIndicator` yourself.

## 💡 Interview Q&A

**Q: What is the main layout advantage of `ButtonGroup`?**
A: It provides built-in logic for responsive design. Instead of wrapping buttons to a new line (like a `FlowRow`), it keeps them on a single line and automatically collapses items that don't fit into the designated `overflowIndicator`.
