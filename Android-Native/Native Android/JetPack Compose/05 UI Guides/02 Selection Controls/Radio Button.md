# ✅ RadioButton

## 📌 Purpose
`RadioButton` is a selection control used when the user needs to select exactly **one** option from a mutually exclusive list. Unlike checkboxes, radio buttons cannot be unselected by clicking them again; another option in the group must be selected to deselect the current one.

## 🔧 Function Signature

```kotlin
@Composable
fun RadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: RadioButtonColors = RadioButtonDefaults.colors(),
    interactionSource: MutableInteractionSource? = null
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| selected | `Boolean` | — | Required. Whether this radio button is currently selected. |
| onClick | `(() -> Unit)?` | — | Required. Callback invoked when the radio button is clicked. If `null`, it becomes visual-only. |
| modifier | `Modifier` | `Modifier` | Optional. |
| enabled | `Boolean` | `true` | Optional. Whether the radio button is enabled and responds to clicks. |
| colors | `RadioButtonColors` | `RadioButtonDefaults.colors()` | Optional. Customizes the colors of the radio button. |
| interactionSource | `MutableInteractionSource?` | `null` | Optional. |

### RadioButtonColors
Created via `RadioButtonDefaults.colors(...)`:
- `selectedColor`
- `unselectedColor`
- `disabledSelectedColor`
- `disabledUnselectedColor`

## ✅ Basic Example

```kotlin
var selected by remember { mutableStateOf(false) }

RadioButton(
    selected = selected,
    onClick = { selected = true } // Note: Radio buttons usually don't toggle off
)
```

## 🚀 Advanced Examples

### 1. RadioGroup Pattern (Standard Usage)
Radio buttons are almost never used alone. They are used in groups. This example shows how to build a standard RadioGroup with labels.
```kotlin
val options = listOf("Option 1", "Option 2", "Option 3")
var selectedOption by remember { mutableStateOf(options[0]) }

Column(Modifier.selectableGroup()) { // selectableGroup for accessibility
    options.forEach { text ->
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                // Use selectable instead of clickable
                .selectable(
                    selected = (text == selectedOption),
                    onClick = { selectedOption = text },
                    role = Role.RadioButton
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = (text == selectedOption),
                onClick = null // null because the Row handles the click
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
```

### 2. RadioButton in a Card (Visual Selection)
For more visual flair, you can place radio buttons inside selectable cards.
```kotlin
val plans = listOf("Basic" to "$10/mo", "Pro" to "$20/mo", "Ultra" to "$30/mo")
var selectedPlan by remember { mutableStateOf(plans[0].first) }

Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    plans.forEach { (planName, price) ->
        val isSelected = planName == selectedPlan
        
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
            ),
            onClick = { selectedPlan = planName }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = null
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(planName, fontWeight = FontWeight.Bold)
                    Text(price, color = Color.Gray)
                }
            }
        }
    }
}
```

## ⚠️ Common Gotchas
- **Toggle behavior:** Do NOT try to implement toggle behavior (e.g. `onClick = { selected = !selected }`) on a RadioButton. This breaks standard UI expectations. Use a Checkbox or Switch if an item can be toggled on/off independently.
- **Interaction handling:** Just like Checkbox, wrap `RadioButton` and its label in a `Row`. Use `Modifier.selectable` on the `Row` and set the `RadioButton`'s `onClick` to `null`.
- **`Modifier.selectableGroup()`:** Always apply this modifier to the `Column` or `Row` that contains your list of radio buttons. This tells screen readers that the items inside are part of a single radio group.

## 💡 Interview Q&A

**Q: What is the difference between `Modifier.toggleable` and `Modifier.selectable`?**
A: `toggleable` is for independent binary states (like a Checkbox or Switch) where interacting flips the state back and forth. `selectable` is used for mutually exclusive options (like a RadioButton or Tabs) where interacting selects the item, and clicking an already-selected item usually does nothing.
