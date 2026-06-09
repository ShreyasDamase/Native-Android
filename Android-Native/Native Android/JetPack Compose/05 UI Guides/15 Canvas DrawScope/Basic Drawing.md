# Basic Drawing Primitives

## 📌 Purpose
`DrawScope` offers a wide variety of declarative functions to render fundamental shapes. All coordinates and sizes use exact pixel floats.

## 🔧 Function Signatures

```kotlin
Canvas(modifier = Modifier.size(200.dp)) {
    // Rectangles
    drawRect(color, topLeft, size, alpha, style, colorFilter, blendMode)
    drawRoundRect(color, topLeft, size, cornerRadius = CornerRadius(x, y), ...)
    
    // Circles & Ovals
    drawCircle(color, radius, center = Offset(x, y), ...)
    drawOval(color, topLeft, size, ...)
    
    // Lines & Points
    drawLine(color, start = Offset(x, y), end = Offset(x, y), strokeWidth, cap, pathEffect, ...)
    drawPoints(points = List<Offset>, pointMode: PointMode, color, strokeWidth, cap, pathEffect, ...)
    
    // Arcs
    drawArc(color, startAngle, sweepAngle, useCenter: Boolean, topLeft, size, ...)
    
    // Images
    drawImage(image: ImageBitmap, topLeft, alpha, style, colorFilter, blendMode)
    drawImage(image, srcOffset, srcSize, dstOffset, dstSize, ...) // Cropping/Scaling
}
```

## 📋 Key Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `topLeft` | `Offset` | `Offset.Zero` | Starting coordinate (top-left corner). |
| `size` | `Size` | `this.size` | Dimensions of the shape. |
| `center` | `Offset` | `this.center` | Center coordinate. |
| `startAngle` | `Float` | — | Used in `drawArc`. 0 is right (3 o'clock). 90 is bottom. |
| `useCenter` | `Boolean` | — | Used in `drawArc`. `true` = pie slice. `false` = curved arc. |
| `pointMode` | `PointMode` | — | `Points` (dots), `Lines` (pairs), `Polygon` (connected path). |

## ✅ Basic Example (Pie Chart Segment)
```kotlin
Canvas(modifier = Modifier.size(100.dp)) {
    drawArc(
        color = Color.Red,
        startAngle = -90f, // Start at 12 o'clock
        sweepAngle = 120f,
        useCenter = true,
        size = size
    )
}
```

## 🚀 Advanced Examples

### 1. Simple Clock Face
```kotlin
Canvas(modifier = Modifier.size(200.dp)) {
    // Clock Border
    drawCircle(color = Color.Black, style = Stroke(width = 4.dp.toPx()))
    
    // Hour hand
    drawLine(
        color = Color.Black,
        start = center,
        end = Offset(center.x, center.y - 50f),
        strokeWidth = 8f,
        cap = StrokeCap.Round
    )
    
    // Minute hand
    drawLine(
        color = Color.Gray,
        start = center,
        end = Offset(center.x + 60f, center.y),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )
    
    // Center pin
    drawCircle(color = Color.Red, radius = 6f)
}
```

### 2. Grid of Dots Pattern
```kotlin
Canvas(modifier = Modifier.fillMaxSize()) {
    val spacing = 40f
    val dotRadius = 4f
    
    for (x in 0..size.width.toInt() step spacing.toInt()) {
        for (y in 0..size.height.toInt() step spacing.toInt()) {
            drawCircle(
                color = Color.LightGray,
                radius = dotRadius,
                center = Offset(x.toFloat(), y.toFloat())
            )
        }
    }
}
```

## ⚠️ Common Gotchas
- **`startAngle` in `drawArc`**: 0 degrees points East (3 o'clock). To start from the top (12 o'clock), use `-90f` or `270f`.
- **`drawImage` vs `painterResource`**: `drawImage` requires an `ImageBitmap`. If you have a vector drawable or resource ID, it is often easier to use `paint` modifier or a standard `Image` composable unless you specifically need bitmap pixel manipulation.

## 💡 Interview Q&A
**Q: How do you draw only the outline of an arc instead of a solid pie slice?**
A: Call `drawArc()` with `useCenter = false` and set the `style` parameter to `Stroke(width = myWidth)`. Setting `useCenter = true` with a `Stroke` will draw the arc outline AND the straight lines connecting the ends to the center.
