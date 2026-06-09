# Transformations

## 📌 Purpose
`DrawScope` transformations modify the coordinate space before drawing occurs. Instead of calculating complex math for every shape vertex, you simply rotate, scale, or translate the entire "paper" (canvas), draw your shape at `(0,0)`, and then restore the paper.

## 🔧 Transform Functions
```kotlin
// Rotates coordinate space around a pivot point
rotate(degrees: Float, pivot: Offset = center) { ... }

// Scales coordinate space
scale(scaleX: Float, scaleY: Float, pivot: Offset = center) { ... }
scale(scale: Float, pivot: Offset = center) { ... } // uniform

// Translates (moves) origin point
translate(left: Float = 0f, top: Float = 0f) { ... }

// Shrinks coordinate space inwards from boundaries
inset(inset: Float) { ... }
inset(left, top, right, bottom) { ... }

// Batch multiple transforms efficiently
withTransform({
    translate(left = 50f, top = 50f)
    rotate(degrees = 45f)
    scale(scaleX = 2f, scaleY = 2f)
}) {
    drawRect(...)
}
```

## 🚀 Advanced Examples

### 1. Radial Tick Marks (Meters / Clocks)
Math to place ticks circularly is hard. Rotating the canvas is easy.
```kotlin
Canvas(modifier = Modifier.size(200.dp)) {
    val radius = size.width / 2f
    val tickLength = 15f
    
    // Draw 12 tick marks around a circle
    for (i in 0 until 12) {
        val angle = i * 30f // 360 degrees / 12 marks
        
        rotate(degrees = angle, pivot = center) {
            // Because we rotated the canvas, we just draw a straight line straight UP
            drawLine(
                color = Color.Black,
                start = Offset(center.x, center.y - radius), 
                end = Offset(center.x, center.y - radius + tickLength),
                strokeWidth = 4f
            )
        }
    }
}
```

### 2. Animating a Needle Sweep
```kotlin
val targetAngle = 45f
val currentAngle by animateFloatAsState(
    targetValue = targetAngle,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
)

Canvas(modifier = Modifier.size(150.dp)) {
    rotate(degrees = currentAngle, pivot = Offset(center.x, size.height)) {
        // Draw needle pointing straight UP. Transform handles the angle.
        drawLine(
            color = Color.Red,
            start = Offset(center.x, size.height), // Pivot at bottom
            end = Offset(center.x, 0f), // Tip at top
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )
    }
}
```

### 3. Inset for Borders
Drawing borders without clipping the edges.
```kotlin
Canvas(modifier = Modifier.size(100.dp)) {
    val strokeWidth = 10f
    
    // If we draw exactly on the bounds, half the stroke is cut off.
    // Inset the canvas by half the stroke width to fix this.
    inset(strokeWidth / 2f) {
        drawRect(
            color = Color.Blue,
            style = Stroke(width = strokeWidth)
        )
    }
}
```

## ⚠️ Common Gotchas
- **Order Matters:** Transforms applied via `withTransform` are applied in the order they are declared. Translating then rotating gives a completely different result than rotating then translating.
- **Pivot Point:** The default pivot for `rotate` and `scale` is `center`. If you are drawing an analog clock hand, you likely want the pivot to be the bottom-center of the hand, not the absolute center.

## 💡 Interview Q&A
**Q: Why use `withTransform` instead of nesting `rotate { translate { scale { ... } } }`?**
A: Nesting transform blocks creates multiple closures, saves and restores the canvas state multiple times, and recalculates matrices repeatedly. `withTransform` computes a single transform matrix internally and applies it once, which is much more efficient.
