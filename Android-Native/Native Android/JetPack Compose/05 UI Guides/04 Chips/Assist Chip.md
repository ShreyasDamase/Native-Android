# 🍟 AssistChip

## 📌 Purpose
`AssistChip` represents smart or automated actions that can span multiple apps, such as opening a calendar event from a message. They are non-selectable actionable chips.

Material 3 offers two variants:
1. `AssistChip` (Outlined by default)
2. `ElevatedAssistChip` (Has shadow/elevation, no outline)

## 🔧 Function Signature

```kotlin
@Composable
fun AssistChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = AssistChipDefaults.shape,
    colors: ChipColors = AssistChipDefaults.assistChipColors(),
    elevation: ChipElevation? = AssistChipDefaults.assistChipElevation(),
    border: BorderStroke? = AssistChipDefaults.assistChipBorder(),
    interactionSource: MutableInteractionSource? = null
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| onClick | `() -> Unit` | — | Required. Callback when clicked. |
| label | `@Composable () -> Unit` | — | Required. The text inside the chip. |
| modifier | `Modifier` | `Modifier` | Optional. |
| enabled | `Boolean` | `true` | Optional. |
| leadingIcon | `@Composable (() -> Unit)?` | `null` | Optional. Icon before the text. |
| trailingIcon | `@Composable (() -> Unit)?` | `null` | Optional. Icon after the text. |
| shape | `Shape` | `AssistChipDefaults.shape` | Optional. |
| colors | `ChipColors` | `AssistChipDefaults.assistChipColors()`| Optional. |
| elevation | `ChipElevation?` | `AssistChipDefaults.assistChipElevation()`| Optional. |
| border | `BorderStroke?` | `AssistChipDefaults.assistChipBorder()` | Optional. |
| interactionSource | `MutableInteractionSource?`| `null` | Optional. |

## ✅ Basic Example

```kotlin
AssistChip(
    onClick = { /* Action */ },
    label = { Text("Add to Calendar") }
)
```

## 🚀 Advanced Examples

### 1. Smart Reply Chips (Row of Chips)
```kotlin
val replies = listOf("Yes, I'll be there!", "Sorry, I can't make it.", "Maybe later.")

LazyRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(horizontal = 16.dp)
) {
    items(replies) { reply ->
        AssistChip(
            onClick = { sendReply(reply) },
            label = { Text(reply) },
            leadingIcon = {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
    }
}
```

### 2. Action Chips in a Chat Interface (Elevated)
```kotlin
ElevatedAssistChip(
    onClick = { openMap() },
    label = { Text("Navigate to location") },
    leadingIcon = {
        Icon(Icons.Default.LocationOn, contentDescription = "Map")
    }
)
```

## ⚠️ Common Gotchas
- **Not Selectable:** `AssistChip` does not hold a selected/unselected state. If you need a toggleable chip, use `FilterChip`.
- **Icon Size:** If you put an `Icon` inside `leadingIcon` or `trailingIcon`, explicitly size it to `18.dp` (`Modifier.size(AssistChipDefaults.IconSize)`). Default icons (24dp) look too large inside chips.

## 💡 Interview Q&A

**Q: What is the semantic difference between `AssistChip` and `SuggestionChip`?**
A: `AssistChip` usually triggers an *action* or opens a different component/app (e.g., adding an event to a calendar, opening a map). `SuggestionChip` provides a *text suggestion* to fill an input field (e.g., auto-completing a search query).
