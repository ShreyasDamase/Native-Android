# SegmentedButton

## 📌 Purpose
Segmented buttons allow users to select options, switch views, or sort elements. They are essentially a group of connected buttons.

> [!WARNING]
> ⚠️ **DEPRECATED** in Material 3 Expressive.
> While `SegmentedButton` and `SegmentedButtonRow` still exist and function, the Material 3 guidelines have shifted towards using the new `ButtonGroup` API for this pattern. However, you will still encounter `SegmentedButton` in existing codebases.

They come in two flavors:
1. **SingleChoice**: Acts like Radio Buttons (only one can be selected).
2. **MultiChoice**: Acts like Checkboxes (multiple can be selected).

## 🔧 Function Signatures

### Row Containers
```kotlin
@ExperimentalMaterial3Api
@Composable
fun SingleChoiceSegmentedButtonRow(
    modifier: Modifier = Modifier,
    space: Dp = SegmentedButtonDefaults.BorderWidth,
    content: @Composable SingleChoiceSegmentedButtonRowScope.() -> Unit
)

@ExperimentalMaterial3Api
@Composable
fun MultiChoiceSegmentedButtonRow(
    modifier: Modifier = Modifier,
    space: Dp = SegmentedButtonDefaults.BorderWidth,
    content: @Composable MultiChoiceSegmentedButtonRowScope.() -> Unit
)
```

### SegmentedButton
```kotlin
@ExperimentalMaterial3Api
@Composable
fun SegmentedButton(
    selected: Boolean, // or 'checked' for MultiChoice
    onClick: () -> Unit, // or 'onCheckedChange' for MultiChoice
    shape: Shape, // REQUIRED!
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SegmentedButtonColors = SegmentedButtonDefaults.colors(),
    border: BorderStroke = SegmentedButtonDefaults.borderStroke(colors.borderColor),
    icon: @Composable () -> Unit = { SegmentedButtonDefaults.Icon(selected) },
    label: @Composable () -> Unit
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `selected` / `checked` | `Boolean` | — | Whether this specific segment is active. |
| `onClick` / `onCheckedChange`| `Callback` | — | Triggered when the segment is tapped. |
| `shape` | `Shape` | — | **Required**. You must calculate this using `SegmentedButtonDefaults.itemShape(index, count)` to ensure only the outer corners are rounded! |
| `icon` | `@Composable` | Checkmark | An icon. Defaults to an animated checkmark when selected. |
| `label` | `@Composable` | — | The text label for the button. |

## ✅ Basic Example

### 1. View Mode Toggle (Single Choice)
```kotlin
@file:OptIn(ExperimentalMaterial3Api::class)

import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun ViewModeToggle() {
    val options = listOf("List", "Grid", "Map")
    var selectedIndex by remember { mutableIntStateOf(0) }

    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                selected = index == selectedIndex,
                onClick = { selectedIndex = index },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(label) }
            )
        }
    }
}
```

## 🚀 Advanced Examples

### 2. Day of Week Filter (Multi Choice)
```kotlin
@file:OptIn(ExperimentalMaterial3Api::class)

import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun DayMultiSelect() {
    val days = listOf("M", "T", "W", "T", "F")
    // Keep track of multiple selected indices
    val selectedIndices = remember { mutableStateListOf<Int>() }

    MultiChoiceSegmentedButtonRow {
        days.forEachIndexed { index, day ->
            SegmentedButton(
                checked = selectedIndices.contains(index),
                onCheckedChange = {
                    if (selectedIndices.contains(index)) {
                        selectedIndices.remove(index)
                    } else {
                        selectedIndices.add(index)
                    }
                },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = days.size),
                label = { Text(day) }
            )
        }
    }
}
```

## ⚠️ Common Gotchas
- **Forgetting `shape`:** The `shape` parameter is explicitly required on `SegmentedButton`. If you pass a generic `RoundedCornerShape`, every segment will have rounded corners on all sides (looking like standard buttons stuck together). You **must** use `SegmentedButtonDefaults.itemShape(index, count)` so it knows to round the left side for the first item, right side for the last item, and square off the middle items.
- **Row Scopes:** You cannot put a `SegmentedButton` inside a standard `Row`. It must be inside `SingleChoiceSegmentedButtonRow` or `MultiChoiceSegmentedButtonRow` scopes.

## 💡 Interview Q&A

**Q: If `SegmentedButton` is being deprecated, what should we use instead?**
A: Material 3 Expressive introduces `ButtonGroup`. You wrap items in a `ButtonGroup` and use `buttonGroupItem()` modifiers or specific button group APIs. It provides better accessibility and more flexible layouts.

**Q: How does `SegmentedButton` handle icons when selected?**
A: By default, `SegmentedButtonDefaults.Icon(selected)` is used. This automatically crossfades and reveals a checkmark icon when the button's `selected` state becomes true. You can override the `icon` parameter to provide your own persistent icons or disable the checkmark.
