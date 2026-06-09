# ✅ Switch

## 📌 Purpose
`Switch` toggles the state of a single setting on or off. It is the preferred way to adjust settings on mobile (e.g., Airplane Mode, Dark Mode). In Material 3, the switch thumb is significantly larger, and you can easily add an icon inside the thumb to indicate the current state.

## 🔧 Function Signature

```kotlin
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| checked | `Boolean` | — | Required. Whether the switch is currently checked (ON). |
| onCheckedChange | `((Boolean) -> Unit)?` | — | Required. Callback when the state changes. If `null`, it becomes visual-only. |
| modifier | `Modifier` | `Modifier` | Optional. |
| thumbContent | `@Composable (() -> Unit)?` | `null` | Optional. (NEW in M3 1.1.0). Icon or content to place *inside* the moving thumb. |
| enabled | `Boolean` | `true` | Optional. Controls if the switch is interactable. |
| colors | `SwitchColors` | `SwitchDefaults.colors()` | Optional. Customizes colors of thumb, track, and border. |
| interactionSource | `MutableInteractionSource?` | `null` | Optional. |

### SwitchColors
Created via `SwitchDefaults.colors(...)`:
- `checkedThumbColor`, `checkedTrackColor`, `checkedBorderColor`, `checkedIconColor`
- `uncheckedThumbColor`, `uncheckedTrackColor`, `uncheckedBorderColor`, `uncheckedIconColor`
- Plus disabled variants for all the above.

## ✅ Basic Example

```kotlin
var isChecked by remember { mutableStateOf(true) }

Switch(
    checked = isChecked,
    onCheckedChange = { isChecked = it }
)
```

## 🚀 Advanced Examples

### 1. Switch with `thumbContent` (Icon inside Thumb)
Material 3 allows placing an icon inside the thumb to make the state clearer.
```kotlin
var isChecked by remember { mutableStateOf(false) }

Switch(
    checked = isChecked,
    onCheckedChange = { isChecked = it },
    thumbContent = if (isChecked) {
        {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        }
    } else {
        {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        }
    }
)
```

### 2. Settings Page Pattern (Row + Label)
Like checkboxes, switches should be paired with a text label and wrapped in a toggleable Row.
```kotlin
var notificationsEnabled by remember { mutableStateOf(true) }

Row(
    modifier = Modifier
        .fillMaxWidth()
        .toggleable(
            value = notificationsEnabled,
            onValueChange = { notificationsEnabled = it },
            role = Role.Switch
        )
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
) {
    Column {
        Text("Push Notifications", style = MaterialTheme.typography.bodyLarge)
        Text("Receive daily alerts", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
    }
    Switch(
        checked = notificationsEnabled,
        onCheckedChange = null // Handled by Row toggleable
    )
}
```

### 3. Custom Colors
```kotlin
var isChecked by remember { mutableStateOf(true) }

Switch(
    checked = isChecked,
    onCheckedChange = { isChecked = it },
    colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = Color.Green,
        uncheckedThumbColor = Color.Gray,
        uncheckedTrackColor = Color.LightGray
    )
)
```

## ⚠️ Common Gotchas
- **Icon Size:** When using `thumbContent`, make sure to apply `Modifier.size(SwitchDefaults.IconSize)` to your `Icon`. If you don't, the icon might be too large and clip out of the thumb boundaries.
- **Row Clicking:** Same as checkboxes: make sure the entire row is clickable by applying `Modifier.toggleable` to the parent layout to increase the touch target.

## 💡 Interview Q&A

**Q: When should you use a `Switch` versus a `Checkbox`?**
A: Use a `Switch` for settings that take effect immediately when toggled (like turning on Wi-Fi). Use a `Checkbox` for selections that require a secondary action to apply (like checking items in a list before hitting "Delete" or submitting a form).
