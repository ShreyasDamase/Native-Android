# 🔘 FloatingActionButton (FAB)

## 📌 Purpose
The `FloatingActionButton` (FAB) represents the primary action of a screen. It usually floats over all other content in the bottom right corner. Material 3 provides 4 variants:
1. **FloatingActionButton** - Standard size (56dp).
2. **SmallFloatingActionButton** - Compact size (40dp).
3. **LargeFloatingActionButton** - Prominent size (96dp).
4. **ExtendedFloatingActionButton** - Pill-shaped, contains both an icon and a text label. Can collapse/expand.

## 🔧 Function Signatures

### 1. Standard FAB
```kotlin
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.shape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
)
```
*(Small and Large variants have identical signatures, differing only in default shape/size).*

### 2. Extended FAB
```kotlin
@Composable
fun ExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    shape: Shape = FloatingActionButtonDefaults.extendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| onClick | `() -> Unit` | — | Required. |
| modifier | `Modifier` | `Modifier` | Optional. |
| shape | `Shape` | `*Defaults.shape` | Optional. M3 defaults to rounded rectangles, not circles! |
| containerColor | `Color` | `*Defaults.containerColor` | Optional. Background color. |
| contentColor | `Color` | `contentColorFor(...)` | Optional. Icon/Text color. |
| elevation | `FloatingActionButtonElevation` | `*Defaults.elevation()` | Optional. Handles shadows. |
| interactionSource | `MutableInteractionSource?`| `null` | Optional. |
| content | `@Composable () -> Unit` | — | Required (Standard). Usually an `Icon`. |
| text | `@Composable () -> Unit` | — | Required (Extended). Text label. |
| icon | `@Composable () -> Unit` | — | Required (Extended). Icon. |
| expanded | `Boolean` | `true` | Required (Extended). Whether text is shown alongside icon. |

### FloatingActionButtonElevation Props
- `defaultElevation`
- `pressedElevation`
- `focusedElevation`
- `hoveredElevation`

## ✅ Basic Example

```kotlin
FloatingActionButton(onClick = { /* Add item */ }) {
    Icon(Icons.Filled.Add, "Add Item")
}
```

## 🚀 Advanced Examples

### 1. Standard FAB inside a Scaffold
This is the correct way to position a FAB.
```kotlin
Scaffold(
    floatingActionButton = {
        FloatingActionButton(onClick = { }) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
    }
) { padding ->
    // Screen content
}
```

### 2. Extended FAB that Collapses on Scroll
A very common pattern where the FAB shrinks to icon-only when scrolling down, and expands when scrolling up.
```kotlin
@Composable
fun ScrollAwareFab() {
    val listState = rememberLazyListState()
    
    // Returns true if the user is scrolling down
    val isExpanded by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 || listState.canScrollBackward.not()
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Compose") },
                icon = { Icon(Icons.Filled.Create, contentDescription = null) },
                onClick = { },
                expanded = isExpanded // Animates automatically!
            )
        }
    ) { padding ->
        LazyColumn(state = listState, contentPadding = padding) {
            items(50) { Text("Item $it", modifier = Modifier.padding(16.dp)) }
        }
    }
}
```

### 3. Animated FAB Appearance
Fading and scaling the FAB in/out based on state.
```kotlin
var showFab by remember { mutableStateOf(true) }

Scaffold(
    floatingActionButton = {
        AnimatedVisibility(
            visible = showFab,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            FloatingActionButton(onClick = { }) {
                Icon(Icons.Default.Add, "Add")
            }
        }
    }
) {
    // Content
}
```

## ⚠️ Common Gotchas
- **M3 Shapes:** In Material Design 3, FABs are no longer circles by default. They are rounded rectangles (`RoundedCornerShape(16.dp)`). If you absolutely need a circle, explicitly pass `shape = CircleShape`.
- **Scaffold Padding:** Never put `modifier = Modifier.padding(16.dp)` on a FAB that is assigned to the `Scaffold(floatingActionButton = { ... })`. The Scaffold already applies the correct standard padding to the bottom right corner.
- **Multiple FABs:** Avoid putting multiple primary FABs on screen. If you need a "Speed Dial" pattern (one FAB expands into many), you have to build that manually or use a third-party library, as Compose doesn't have an official `SpeedDialFab` yet.

## 💡 Interview Q&A

**Q: How does `ExtendedFloatingActionButton` handle the `expanded` parameter?**
A: When `expanded` changes from `true` to `false`, the component automatically animates the width to collapse the text, leaving only the icon. You don't need to wrap it in an `AnimatedVisibility`.
