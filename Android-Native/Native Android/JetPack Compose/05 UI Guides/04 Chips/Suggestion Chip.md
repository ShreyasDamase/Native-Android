# 🍟 SuggestionChip

## 📌 Purpose
`SuggestionChip` helps narrow user intent by presenting dynamically generated suggestions. For example, suggesting responses in a messaging app, or offering search autocomplete terms. They are strictly action-based (not selectable).

Material 3 offers two variants:
1. `SuggestionChip`
2. `ElevatedSuggestionChip`

## 🔧 Function Signature

```kotlin
@Composable
fun SuggestionChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null, // NOTE: only "icon", no trailing/leading
    shape: Shape = SuggestionChipDefaults.shape,
    colors: ChipColors = SuggestionChipDefaults.suggestionChipColors(),
    elevation: ChipElevation? = SuggestionChipDefaults.suggestionChipElevation(),
    border: BorderStroke? = SuggestionChipDefaults.suggestionChipBorder(),
    interactionSource: MutableInteractionSource? = null
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| onClick | `() -> Unit` | — | Required. |
| label | `@Composable () -> Unit` | — | Required. |
| modifier | `Modifier` | `Modifier` | Optional. |
| enabled | `Boolean` | `true` | Optional. |
| icon | `@Composable (() -> Unit)?` | `null` | Optional. Leading icon only. No trailing icon parameter exists. |
| shape... | | | Customization parameters. |

## ✅ Basic Example

```kotlin
SuggestionChip(
    onClick = { appendSearchQuery("Compose") },
    label = { Text("Compose") }
)
```

## 🚀 Advanced Examples

### 1. Search Suggestion Chips
```kotlin
val searchHistory = listOf("Jetpack Compose", "Material 3", "Kotlin Flows")

LazyRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(16.dp)
) {
    items(searchHistory) { query ->
        SuggestionChip(
            onClick = { performSearch(query) },
            label = { Text(query) },
            icon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
    }
}
```

### 2. AI Prompt Suggestions (Elevated)
```kotlin
val prompts = listOf("Summarize this text", "Translate to Spanish", "Fix grammar")

Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Suggestions", style = MaterialTheme.typography.titleMedium)
    
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        prompts.forEach { prompt ->
            ElevatedSuggestionChip(
                onClick = { fillTextField(prompt) },
                label = { Text(prompt) },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color.Magenta) }
            )
        }
    }
}
```

## ⚠️ Common Gotchas
- **No Trailing Icon:** The `SuggestionChip` API explicitly removes the `trailingIcon` parameter because suggestions are not meant to be modified or deleted in-place. They are transient.
- **Similar to AssistChip:** It looks almost identical to an `AssistChip`. The choice between them is purely semantic. Use `AssistChip` for taking actions (opening maps, saving files). Use `SuggestionChip` for generating text or refining inputs.

## 💡 Interview Q&A

**Q: Can a `SuggestionChip` be selected?**
A: No. Unlike `FilterChip` and `InputChip`, a `SuggestionChip` is stateless. It only fires an `onClick` event and does not hold a boolean `selected` state.
