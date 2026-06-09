# 🍟 InputChip

## 📌 Purpose
`InputChip` represents a complex piece of information in compact form, such as an entity (person, place, or thing) or text. They are commonly used for tags or email recipients. They are unique because they support an `avatar` and a `trailingIcon` (usually an "X" to delete the chip), and they can be selectable.

## 🔧 Function Signature

```kotlin
@Composable
fun InputChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    avatar: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = InputChipDefaults.shape,
    colors: SelectableChipColors = InputChipDefaults.inputChipColors(),
    elevation: SelectableChipElevation? = InputChipDefaults.inputChipElevation(),
    border: SelectableChipBorder? = InputChipDefaults.inputChipBorder(),
    interactionSource: MutableInteractionSource? = null
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| selected | `Boolean` | — | Required. |
| onClick | `() -> Unit` | — | Required. |
| label | `@Composable () -> Unit` | — | Required. |
| modifier | `Modifier` | `Modifier` | Optional. |
| enabled | `Boolean` | `true` | Optional. |
| avatar | `@Composable (() -> Unit)?` | `null` | Optional. Profile photo or image. Renders before `leadingIcon`. |
| trailingIcon | `@Composable (() -> Unit)?` | `null` | Optional. Usually a remove/close button. |
| leadingIcon | `@Composable (() -> Unit)?` | `null` | Optional. Used if no avatar is provided. |

## ✅ Basic Example

```kotlin
InputChip(
    selected = false,
    onClick = { },
    label = { Text("Compose") },
    trailingIcon = {
        Icon(Icons.Default.Close, contentDescription = "Remove tag")
    }
)
```

## 🚀 Advanced Examples

### 1. Email Recipient Chip with Avatar
```kotlin
var isSelected by remember { mutableStateOf(false) }

InputChip(
    selected = isSelected,
    onClick = { isSelected = !isSelected },
    label = { Text("Jane Doe") },
    avatar = {
        // Simple avatar placeholder
        Box(
            modifier = Modifier
                .size(InputChipDefaults.AvatarSize)
                .background(Color.Blue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("J", color = Color.White, fontSize = 12.sp)
        }
    },
    trailingIcon = {
        IconButton(
            onClick = { /* Remove recipient */ },
            modifier = Modifier.size(InputChipDefaults.AvatarSize)
        ) {
            Icon(Icons.Default.Clear, contentDescription = "Remove")
        }
    }
)
```

### 2. Tag Input Field with FlowRow
Showing chips wrapping inside an input area.
```kotlin
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagInput() {
    val tags = remember { mutableStateListOf("Android", "Kotlin", "Compose") }
    
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            InputChip(
                selected = false,
                onClick = { }, // Optionally select for editing
                label = { Text(tag) },
                trailingIcon = {
                    Icon(
                        Icons.Default.Close, 
                        contentDescription = "Remove",
                        modifier = Modifier.clickable { tags.remove(tag) }
                    )
                }
            )
        }
        
        // Pseudo Text Input
        BasicTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.align(Alignment.CenterVertically),
            decorationBox = { Text("Add tag...", color = Color.Gray) }
        )
    }
}
```

## ⚠️ Common Gotchas
- **Click Actions:** `InputChip` has an `onClick` for the whole chip, but often you want clicking the "X" (trailing icon) to delete it. You must apply `Modifier.clickable { ... }` or use an `IconButton` specifically on the `trailingIcon` composable to capture that discrete deletion action.
- **Avatar vs Leading Icon:** They occupy similar visual space. Use `avatar` for images (it has specific padding/clipping rules) and `leadingIcon` for vector icons.

## 💡 Interview Q&A

**Q: What makes `InputChip` unique among the other Chip types?**
A: It is the only chip specifically designed to hold an `avatar` and is meant to represent dynamic, user-generated entities (like tags or contacts) that can be deleted via a `trailingIcon`.
