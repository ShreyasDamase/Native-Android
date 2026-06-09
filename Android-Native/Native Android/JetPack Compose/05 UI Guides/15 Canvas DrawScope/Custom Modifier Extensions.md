# Custom Modifier Extensions

## 📌 Purpose
Instead of placing a `Canvas` composable everywhere, you can encapsulate complex drawing logic into reusable `Modifier` extension functions. This is how Compose's own `Modifier.background()` and `Modifier.border()` are implemented internally.

## 🔧 Basic Structure
```kotlin
fun Modifier.myCustomDrawModifier(color: Color): Modifier = this.drawBehind {
    drawRect(color = color)
}
```
*Note: Use `Modifier.composed { ... }` ONLY if your modifier needs to use `remember` or access Composable scope (like `animateFloatAsState`). Otherwise, use standard functional extensions.*

## 🚀 Advanced Examples

### 1. Reusable Gradient Border Modifier
Compose's native `Modifier.border()` doesn't allow gradients with corner radiuses easily. Let's build one.
```kotlin
fun Modifier.gradientBorder(
    colors: List<Color>,
    width: Dp,
    cornerRadius: Dp
): Modifier = this.drawWithCache {
    val strokeWidthPx = width.toPx()
    val radiusPx = cornerRadius.toPx()
    
    val brush = Brush.linearGradient(colors)
    
    onDrawBehind {
        drawRoundRect(
            brush = brush,
            size = size,
            cornerRadius = CornerRadius(radiusPx),
            style = Stroke(width = strokeWidthPx)
        )
    }
}

// Usage:
Box(modifier = Modifier
    .size(100.dp)
    .gradientBorder(listOf(Color.Red, Color.Blue), 4.dp, 16.dp)
)
```

### 2. State-Aware Modifier (using Modifier.composed)
If the modifier needs an internal animation, use `composed`.
```kotlin
fun Modifier.neonGlow(color: Color, isPulsing: Boolean = false): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
    )
    
    this.drawBehind {
        val currentAlpha = if (isPulsing) alpha else 1f
        
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                this.color = color.copy(alpha = currentAlpha)
                asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(
                    30f, 
                    android.graphics.BlurMaskFilter.Blur.OUTER
                )
            }
            canvas.drawRect(Rect(0f, 0f, size.width, size.height), paint)
        }
    }
}
```

### 3. Custom Indication (Replacing the Ripple)
A vintage UI element might need a flat darken effect on press, rather than a material ripple.
```kotlin
class VintageClickIndication : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        val isPressed by interactionSource.collectIsPressedAsState()
        
        return object : IndicationInstance {
            override fun ContentDrawScope.drawIndication() {
                // 1. Draw the component itself
                drawContent()
                
                // 2. Draw overlay if pressed
                if (isPressed) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.2f),
                        size = size
                    )
                }
            }
        }
    }
}

// Usage:
Box(
    modifier = Modifier
        .size(100.dp)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = VintageClickIndication()
        ) { }
)
```

## ⚠️ Common Gotchas
- **`Modifier.composed` overhead:** `composed` creates a state object every time the modifier is applied to an element in the tree. Do not use it unless you absolutely need `@Composable` context (like `remember`).
- **Chaining Order:** Draw modifiers execute in the order they are chained. `Modifier.background(Red).drawBehind { drawRect(Blue) }` will result in Blue being drawn under Red, making Blue invisible.

## 💡 Interview Q&A
**Q: How does `Modifier.drawWithContent` differ from `Modifier.drawBehind`?**
A: `drawBehind` strictly executes *before* the component's internal content is rendered. `drawWithContent` gives you the `drawContent()` method, allowing you to choose exactly when the component renders, enabling you to draw on top of it, underneath it, or apply a layer effect around it.
