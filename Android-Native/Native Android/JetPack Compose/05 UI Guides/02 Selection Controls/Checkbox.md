# ✅ Checkbox

## 📌 Purpose
`Checkbox` is a selection control that allows the user to select one or more items from a set. It comes in two variants:
1. `Checkbox` - standard binary state (Checked / Unchecked).
2. `TriStateCheckbox` - three states (Checked / Unchecked / Indeterminate). Typically used for a "Select All" parent checkbox where only *some* children are selected.

## 🔧 Function Signature

### 1. Checkbox
```kotlin
@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource? = null
)
```

### 2. TriStateCheckbox
```kotlin
@Composable
fun TriStateCheckbox(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource? = null
)
```

## 📋 Props / Parameters

### Checkbox
| Parameter | Type | Default | Description |
|---|---|---|---|
| checked | `Boolean` | — | Required. Whether the checkbox is checked. |
| onCheckedChange | `((Boolean) -> Unit)?` | — | Required. Callback when toggled. If `null`, checkbox becomes visual-only (cannot be clicked to toggle). |
| modifier | `Modifier` | `Modifier` | Optional. |
| enabled | `Boolean` | `true` | Optional. Whether the checkbox responds to user interaction. |
| colors | `CheckboxColors` | `CheckboxDefaults.colors()` | Optional. Customizes colors. |
| interactionSource | `MutableInteractionSource?` | `null` | Optional. |

### TriStateCheckbox
| Parameter | Type | Default | Description |
|---|---|---|---|
| state | `ToggleableState` | — | Required. Can be `On`, `Off`, or `Indeterminate`. |
| onClick | `(() -> Unit)?` | — | Required. Callback when clicked. You define how state transitions. |

### CheckboxColors
Created via `CheckboxDefaults.colors(...)`:
- `checkedColor`
- `uncheckedColor`
- `checkmarkColor`
- `disabledCheckedColor`
- `disabledUncheckedColor`
- `disabledIndeterminateColor`

## ✅ Basic Example

```kotlin
var isChecked by remember { mutableStateOf(false) }

Checkbox(
    checked = isChecked,
    onCheckedChange = { isChecked = it }
)
```

## 🚀 Advanced Examples

### 1. Checkbox with Label (Clickable Row)
Checkboxes don't have built-in labels. You must combine them with a `Text` in a `Row` and make the *Row* toggleable for better UX.
```kotlin
var isChecked by remember { mutableStateOf(false) }

Row(
    Modifier
        .fillMaxWidth()
        .toggleable(
            value = isChecked,
            onValueChange = { isChecked = it },
            role = Role.Checkbox
        )
        .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Checkbox(
        checked = isChecked,
        onCheckedChange = null // null because the Row handles the click
    )
    Spacer(Modifier.width(16.dp))
    Text("Accept Terms and Conditions")
}
```

### 2. TriStateCheckbox ("Select All" Pattern)
```kotlin
val items = listOf("Apple", "Banana", "Cherry")
val checkedStates = remember { mutableStateListOf(false, false, false) }

// Calculate parent state based on children
val parentState = when {
    checkedStates.all { it } -> ToggleableState.On
    checkedStates.none { it } -> ToggleableState.Off
    else -> ToggleableState.Indeterminate
}

Column(Modifier.padding(16.dp)) {
    // Parent
    Row(verticalAlignment = Alignment.CenterVertically) {
        TriStateCheckbox(
            state = parentState,
            onClick = {
                // If currently On or Indeterminate, turn everything Off. Else turn everything On.
                val newState = parentState != ToggleableState.On
                checkedStates.indices.forEach { checkedStates[it] = newState }
            }
        )
        Text("Select All", fontWeight = FontWeight.Bold)
    }

    // Children
    items.forEachIndexed { index, item ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 32.dp)
        ) {
            Checkbox(
                checked = checkedStates[index],
                onCheckedChange = { checkedStates[index] = it }
            )
            Text(item)
        }
    }
}
```

### 3. Custom Colored Checkbox
```kotlin
Checkbox(
    checked = true,
    onCheckedChange = {},
    colors = CheckboxDefaults.colors(
        checkedColor = Color.Magenta,
        checkmarkColor = Color.White,
        uncheckedColor = Color.LightGray
    )
)
```

## ⚠️ Common Gotchas
- **Small touch targets:** Standalone `Checkbox` composables have a small touch target. It is highly recommended to wrap them in a `Row` with a label and apply `Modifier.toggleable` to the row to increase the tap area.
- **Using `clickable` instead of `toggleable`:** When wrapping a Checkbox in a Row, use `Modifier.toggleable` rather than `Modifier.clickable`. `toggleable` provides proper semantic accessibility for screen readers indicating the ON/OFF state.
- **Null `onCheckedChange`:** Passing `null` removes the ripple and interaction from the checkbox. This is ideal when the parent `Row` handles the interaction.

## 💡 Interview Q&A

**Q: How does `TriStateCheckbox` know which state to transition to when clicked?**
A: It doesn't! Unlike a standard `Checkbox` that passes you the *new* boolean value in `onCheckedChange`, `TriStateCheckbox` only provides an `onClick` callback with no parameters. It is completely up to your business logic to determine what state follows `Indeterminate` (usually it transitions to `On`).
