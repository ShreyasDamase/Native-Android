# Draw Modifiers

## 📌 Purpose
Draw modifiers allow you to execute custom Canvas drawing commands relative to a composable's content. They bridge the gap between standard Compose layouts and custom `DrawScope` rendering.

## 🔧 Function Signatures

### Modifier.drawBehind
Draws custom content *behind* the composable.
```kotlin
fun Modifier.drawBehind(
    onDraw: DrawScope.() -> Unit
): Modifier
```

### Modifier.drawWithContent
Gives explicit control over *when* the composable's content is drawn.
```kotlin
fun Modifier.drawWithContent(
    onDraw: ContentDrawScope.() -> Unit
): Modifier
```

### Modifier.drawWithCache
Allows caching expensive objects (paths, brushes, text layouts) that are sized-dependent, re-creating them only when the component's size changes.
```kotlin
fun Modifier.drawWithCache(
    onBuildDrawCache: CacheDrawScope.() -> DrawResult
): Modifier
```

## 📋 Props / Parameters
These modifiers take lambda parameters that run within specialized scopes:
- `DrawScope`: Standard canvas drawing capabilities (`size`, `center`, `drawRect`, etc).
- `ContentDrawScope`: Inherits `DrawScope`, adds `drawContent()` to trigger the component's render.
- `CacheDrawScope`: Inherits `DrawScope`, adds `onDrawBehind` and `onDrawWithContent` to return a `DrawResult`.

## ✅ Basic Examples

### Custom Background (drawBehind)
```kotlin
Text(
    text = "Highlighted Text",
    modifier = Modifier.drawBehind {
        drawCircle(color = Color.Yellow, radius = size.width / 2f)
    }
)
```

### Foreground Overlay (drawWithContent)
```kotlin
Image(
    painter = painterResource(id = R.drawable.image),
    contentDescription = null,
    modifier = Modifier.drawWithContent {
        drawContent() // Draw the image first
        drawRect(color = Color.Black.copy(alpha = 0.3f)) // Draw tint on top
    }
)
```

## 🚀 Advanced Examples

### 1. Custom Text Underline
Draws an underline that sits exactly below the text bounds.
```kotlin
Text(
    text = "Custom Underline",
    modifier = Modifier
        .padding(bottom = 4.dp)
        .drawBehind {
            val strokeWidth = 2.dp.toPx()
            val y = size.height - strokeWidth / 2
            drawLine(
                color = Color.Red,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidth
            )
        }
)
```

### 2. Animated Shimmer Effect (drawWithCache)
Using `drawWithCache` is critical for animations to avoid recreating the gradient `Brush` every frame.
```kotlin
val infiniteTransition = rememberInfiniteTransition()
val shimmerOffset by infiniteTransition.animateFloat(
    initialValue = -1f,
    targetValue = 2f,
    animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart)
)

Box(
    modifier = Modifier
        .size(200.dp, 50.dp)
        .drawWithCache {
            // Re-runs only if size changes
            val gradient = Brush.linearGradient(
                colors = listOf(Color.LightGray, Color.White, Color.LightGray),
                start = Offset(shimmerOffset * size.width, 0f),
                end = Offset(shimmerOffset * size.width + size.width, 0f)
            )
            
            // Re-runs every frame (as shimmerOffset changes)
            onDrawBehind {
                drawRect(gradient)
            }
        }
)
```

### 3. Gradient Border
```kotlin
Box(
    modifier = Modifier
        .size(100.dp)
        .drawWithCache {
            val brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
            onDrawBehind {
                drawRoundRect(
                    brush = brush,
                    style = Stroke(width = 4.dp.toPx()),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            }
        }
)
```

## ⚠️ Common Gotchas
- **State Reads:** Reading mutable state (like an animated float) inside `drawBehind` or `onDrawBehind` only invalidates the *draw phase*, making it highly performant.
- **Recomposition Loop:** Do not create large objects (like `Path` or `Brush`) inside `drawBehind`. If size depends on them, use `drawWithCache`.
- **`clip` vs `clipToBounds`**: `Modifier.clip(Shape)` clips to a specific shape, while `Modifier.clipToBounds()` clips exactly to the component's rectangular boundaries.

## 💡 Interview Q&A
**Q: When should you use `drawWithCache` instead of `drawBehind`?**
A: Use `drawWithCache` whenever your drawing logic requires creating objects that depend on the component's size (e.g., a `Brush.linearGradient` or a `Path`). `drawWithCache` ensures these objects are only created when the size changes, avoiding memory allocation during every frame of an animation.
