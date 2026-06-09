# 🔘 IconButtons

## 📌 Purpose
Icon Buttons are used for actions represented exclusively by an icon, without a text label. Material 3 provides 8 variants. 
Four of these are standard **actions**, and four are **toggleable** (can be turned on/off).

Standard Variants:
1. **IconButton**: Standard, no background. Low emphasis.
2. **FilledIconButton**: Solid color background. High emphasis.
3. **FilledTonalIconButton**: Tonal background. Medium-high emphasis.
4. **OutlinedIconButton**: Outlined border. Medium emphasis.

Toggleable Variants (use `onCheckedChange` instead of `onClick`):
5. **IconToggleButton**
6. **FilledIconToggleButton**
7. **FilledTonalIconToggleButton**
8. **OutlinedIconToggleButton**

By default, IconButtons constrain their size to `40.dp`.

## 🔧 Function Signatures

### Standard (e.g. IconButton)
```kotlin
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
)
```

### Toggleable (e.g. IconToggleButton)
```kotlin
@Composable
fun IconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
)
```
*(The filled/tonal/outlined variants add `shape: Shape` to the signature).*

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| onClick | `() -> Unit` | — | Required (Standard). Action on click. |
| checked | `Boolean` | — | Required (Toggle). Current state. |
| onCheckedChange | `(Boolean) -> Unit` | — | Required (Toggle). Action on toggle. |
| modifier | `Modifier` | `Modifier` | Optional. |
| enabled | `Boolean` | `true` | Optional. |
| shape | `Shape` | e.g. `CircleShape` | Optional (Only for bordered/filled variants). |
| colors | `*Colors` | `IconButtonDefaults.*Colors()` | Optional. |
| interactionSource | `MutableInteractionSource?`| `null` | Optional. |
| content | `@Composable () -> Unit` | — | Required. Usually an `Icon`. |

## ✅ Basic Example

```kotlin
IconButton(onClick = { /* Do something */ }) {
    Icon(Icons.Filled.Search, contentDescription = "Search")
}
```

## 🚀 Advanced Examples

### 1. IconButton in TopAppBar
The most common place for an unstyled `IconButton` is inside app bars.
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBarWithIcons() {
    TopAppBar(
        title = { Text("Home") },
        actions = {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Search, "Search")
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.MoreVert, "More options")
            }
        }
    )
}
```

### 2. FilledIconButton as Primary Action
```kotlin
FilledIconButton(
    onClick = { playMusic() },
    modifier = Modifier.size(56.dp) // Making it larger
) {
    Icon(
        imageVector = Icons.Filled.PlayArrow,
        contentDescription = "Play",
        modifier = Modifier.size(32.dp)
    )
}
```

### 3. IconToggleButton for Like/Unlike
Changing the icon based on the state.
```kotlin
var isLiked by remember { mutableStateOf(false) }

IconToggleButton(
    checked = isLiked,
    onCheckedChange = { isLiked = it }
) {
    val icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
    val tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
    
    Icon(icon, contentDescription = "Like", tint = tint)
}
```

### 4. FilledIconToggleButton for Favorite Star
This variant changes its background color when selected.
```kotlin
var isFavorite by remember { mutableStateOf(false) }

FilledIconToggleButton(
    checked = isFavorite,
    onCheckedChange = { isFavorite = it }
) {
    Icon(
        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
        contentDescription = "Favorite"
    )
}
```

## ⚠️ Common Gotchas
- **Accessibility:** `IconButton` components do not have text labels. You **must** provide a meaningful `contentDescription` inside the `Icon` for screen readers.
- **Click targets:** Material 3 `IconButton` handles minimum touch target sizes automatically (`48.dp` minimum touch area, even though the visual size defaults to `40.dp`). Be careful not to wrap it in a `Box(Modifier.clickable)` which might break this handling.
- **Tinting:** `IconButtons` dictate the default tint color for the `Icon` inside them via `LocalContentColor`. You usually don't need to specify `tint = ...` on the `Icon` unless you are overriding it (like the red heart example).

## 💡 Interview Q&A

**Q: When would you use a `FilledIconToggleButton` over an `IconToggleButton`?**
A: You use `IconToggleButton` when the state change is represented purely by the icon changing (e.g. an empty star becoming a filled star). You use `FilledIconToggleButton` when the button represents a standalone filter or chip that toggles on and off, where a background color change clearly indicates the "active" state.
