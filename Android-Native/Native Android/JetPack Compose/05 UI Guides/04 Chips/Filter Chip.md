# 🍟 FilterChip

## 📌 Purpose
`FilterChip` is used to filter content. It acts like a toggle button or checkbox, representing a binary state (selected or unselected). Multiple filter chips can be selected at the same time.

Material 3 offers two variants:
1. `FilterChip`
2. `ElevatedFilterChip`

## 🔧 Function Signature

```kotlin
@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = FilterChipDefaults.shape,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
    elevation: SelectableChipElevation? = FilterChipDefaults.filterChipElevation(),
    border: SelectableChipBorder? = FilterChipDefaults.filterChipBorder(),
    interactionSource: MutableInteractionSource? = null
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| selected | `Boolean` | — | Required. The current state of the chip. |
| onClick | `() -> Unit` | — | Required. Callback when toggled. |
| label | `@Composable () -> Unit` | — | Required. The text inside the chip. |
| modifier | `Modifier` | `Modifier` | Optional. |
| enabled | `Boolean` | `true` | Optional. |
| leadingIcon | `@Composable (() -> Unit)?` | `null` | Optional. Typically a checkmark when selected. |
| trailingIcon | `@Composable (() -> Unit)?` | `null` | Optional. Icon after the text. |
| shape | `Shape` | `FilterChipDefaults.shape` | Optional. |
| colors | `SelectableChipColors` | `FilterChipDefaults.*Colors()` | Optional. |
| elevation | `SelectableChipElevation?`| `FilterChipDefaults.*Elevation()` | Optional. |
| border | `SelectableChipBorder?` | `FilterChipDefaults.*Border()` | Optional. |

## ✅ Basic Example

```kotlin
var selected by remember { mutableStateOf(false) }

FilterChip(
    selected = selected,
    onClick = { selected = !selected },
    label = { Text("Vegan") }
)
```

## 🚀 Advanced Examples

### 1. Multi-Select Filter Row
A common e-commerce pattern for filtering categories.
```kotlin
val categories = listOf("Shoes", "Shirts", "Pants", "Hats")
val selectedCategories = remember { mutableStateListOf<String>() }

LazyRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(16.dp)
) {
    items(categories) { category ->
        val isSelected = selectedCategories.contains(category)
        
        FilterChip(
            selected = isSelected,
            onClick = {
                if (isSelected) selectedCategories.remove(category)
                else selectedCategories.add(category)
            },
            label = { Text(category) },
            leadingIcon = if (isSelected) {
                {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Selected",
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            } else null
        )
    }
}
```

### 2. Animated Icon on Selection
You can wrap the `leadingIcon` in an `AnimatedVisibility` for a smooth transition.
```kotlin
var isSelected by remember { mutableStateOf(false) }

FilterChip(
    selected = isSelected,
    onClick = { isSelected = !isSelected },
    label = { Text("Favorites Only") },
    leadingIcon = {
        AnimatedVisibility(
            visible = isSelected,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut()
        ) {
            Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
        }
    }
)
```

## ⚠️ Common Gotchas
- **Manual Checkmark:** Unlike checkboxes, `FilterChip` does **not** automatically show a checkmark when `selected = true`. You must manually provide the checkmark inside the `leadingIcon` slot if you want that visual behavior.
- **SegmentedButtons vs FilterChips:** `FilterChip` is for multiple-selection filters. If you are building a mutually exclusive single-selection group (like RadioButtons), use `ButtonGroup` or `SegmentedButton` instead.

## 💡 Interview Q&A

**Q: Why use `FilterChip` over a regular `Checkbox`?**
A: `FilterChip` is more visually compact and easier to place in horizontal scrolling lists (`LazyRow`). It is often used in search interfaces where vertical space is premium, whereas checkboxes are better suited for vertical forms.
