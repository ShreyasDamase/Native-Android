# Surface

## 📌 Purpose
`Surface` is the fundamental building block of Material Design layouts in Jetpack Compose. It is the lowest-level Material container component. 

Almost all other Material components (Card, Dialog, BottomSheet, NavigationBar) use `Surface` under the hood. It provides:
- A background color.
- A content color (automatically ensuring text is readable against the background).
- Shape clipping.
- Tonal and shadow elevation.

## 🔧 Function Signature

### Non-Clickable Surface
```kotlin
@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
)
```

### Clickable Surface
```kotlin
@Composable
fun Surface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `color` | `Color` | `surface` | Background color. |
| `contentColor` | `Color` | `contentColorFor(color)` | Automatically picks a high-contrast color (e.g. OnSurface) for text/icons placed inside. |
| `tonalElevation` | `Dp` | `0.dp` | Material 3 specific. Lightly tints the surface color with the primary color based on height. |
| `shadowElevation` | `Dp` | `0.dp` | Renders an actual drop shadow under the shape. |
| `shape` | `Shape` | `RectangleShape` | Clips content to this shape. |

## ✅ Basic Example
Usually wraps an entire screen or a distinct section of UI.

```kotlin
@Composable
fun BasicSurfaceScreen() {
    // A surface filling the screen provides the default background color
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Text("Hello World!", modifier = Modifier.padding(16.dp))
    }
}
```

## 🚀 Advanced Examples

### 1. Tonal Elevation Demo
In Material 3, changing elevation doesn't just add a shadow; it changes the *tint* of the background color.

```kotlin
@Composable
fun TonalElevationSurfaces() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Elevation 0.dp", Modifier.padding(8.dp))
        }
        Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Elevation 4.dp (Slight tint)", Modifier.padding(8.dp))
        }
        Surface(tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Elevation 8.dp (Stronger tint)", Modifier.padding(8.dp))
        }
    }
}
```

### 2. Custom Shape and Border
You can use Surface as a generic container when `Card` has too much opinionated styling.

```kotlin
@Composable
fun PillShapedSurface() {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        border = BorderStroke(2.dp, Color.Blue),
        color = Color.LightGray,
        shadowElevation = 4.dp,
        modifier = Modifier.padding(16.dp)
    ) {
        Text("Pill Button", modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
    }
}
```

## ⚠️ Common Gotchas

> [!WARNING] No Layout Logic
> Unlike `Box`, `Column`, or `Row`, `Surface` **does not** have inherent layout placement logic. It is just a colored canvas. If you put multiple items inside a `Surface` without wrapping them in a layout container, they will stack on top of each other exactly like a `Box`.

> [!TIP] `contentColorFor()`
> If you set a `Surface(color = MaterialTheme.colorScheme.primary)`, any `Text` placed inside it will automatically be tinted with `MaterialTheme.colorScheme.onPrimary`. You don't need to manually color your texts.

## 💡 Interview Q&A

**Q: When should I use `Box` versus `Surface`?**
A: Use `Box` when you purely need to position or stack UI elements. Use `Surface` when you need a Material visual layer (background color, shadow, tonal elevation, shape clipping, or touch feedback/ripple).

**Q: What is the difference between `tonalElevation` and `shadowElevation`?**
A: `shadowElevation` draws a dark drop-shadow around the perimeter of the shape. `tonalElevation` (introduced in M3) subtly shifts the *background color itself* toward the primary color to indicate depth, often without drawing any shadow at all (especially in dark mode).
