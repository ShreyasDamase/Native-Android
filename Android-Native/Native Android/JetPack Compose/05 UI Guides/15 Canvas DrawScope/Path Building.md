# Path Building

## 📌 Purpose
When basic geometric primitives aren't enough, `Path` lets you construct entirely custom vector shapes. Paths can consist of straight lines, arcs, bezier curves, and combined operations.

## 🔧 Path DSL
```kotlin
val path = Path().apply {
    // 1. Position cursor
    moveTo(x, y) 
    
    // 2. Draw lines
    lineTo(x, y) // absolute
    relativeLineTo(dx, dy) // relative to current position
    
    // 3. Curves
    quadraticBezierTo(controlX, controlY, endX, endY)
    cubicTo(cx1, cy1, cx2, cy2, endX, endY)
    
    // 4. Arcs
    arcTo(rect, startAngleDegrees, sweepAngleDegrees, forceMoveTo)
    
    // 5. Appending complete shapes
    addOval(rect)
    addRoundRect(roundRect)
    
    // 6. Finish
    close() // Draws line back to the last moveTo()
}

// Render the path
drawPath(path, color, alpha, style, colorFilter, blendMode)
```

## 🚀 Advanced Examples

### 1. Drawing a Custom Needle for an Analog Meter
A sharp diamond-like needle commonly used in vintage gauges.
```kotlin
Canvas(modifier = Modifier.size(20.dp, 100.dp)) {
    val needlePath = Path().apply {
        moveTo(size.width / 2f, 0f) // Sharp Top point
        lineTo(size.width, size.height * 0.8f) // Right base
        lineTo(size.width / 2f, size.height) // Bottom blunt center
        lineTo(0f, size.height * 0.8f) // Left base
        close()
    }
    
    drawPath(path = needlePath, color = Color.Red)
}
```

### 2. Path Operations (Boolean Math)
You can combine two paths using Union, Intersect, Difference, etc.
```kotlin
Canvas(modifier = Modifier.size(200.dp)) {
    val pathA = Path().apply { addCircle(center, 50f) }
    val pathB = Path().apply { addCircle(Offset(center.x + 40f, center.y), 50f) }
    
    // Create a crescent moon shape by subtracting Path B from Path A
    val combinedPath = Path.combine(
        operation = PathOperation.Difference,
        path1 = pathA,
        path2 = pathB
    )
    
    drawPath(path = combinedPath, color = Color.Yellow)
}
```

### 3. Speech Bubble with a Tail
Combining a rounded rectangle with custom lines.
```kotlin
Canvas(modifier = Modifier.size(200.dp, 150.dp)) {
    val bubblePath = Path().apply {
        val rect = Rect(0f, 0f, size.width, size.height - 30f)
        addRoundRect(RoundRect(rect, cornerRadius = CornerRadius(20f)))
        
        // Add the triangle tail at the bottom
        moveTo(size.width / 2f - 20f, size.height - 30f)
        lineTo(size.width / 2f, size.height)
        lineTo(size.width / 2f + 20f, size.height - 30f)
    }
    
    drawPath(path = bubblePath, color = Color.LightGray)
}
```

### 4. Quadratic Bezier Wave
```kotlin
Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
    val wavePath = Path().apply {
        moveTo(0f, size.height / 2f)
        // curve down, then up
        quadraticBezierTo(
            size.width / 4f, size.height, // Control point
            size.width / 2f, size.height / 2f // End point
        )
        // curve up, then down
        quadraticBezierTo(
            size.width * 0.75f, 0f, // Control point
            size.width, size.height / 2f // End point
        )
    }
    
    drawPath(
        path = wavePath, 
        color = Color.Cyan, 
        style = Stroke(width = 5f)
    )
}
```

## ⚠️ Common Gotchas
- **`forceMoveTo` in `arcTo`**: If `forceMoveTo` is `true`, a new sub-path is started at the arc's beginning. If `false`, a straight line is drawn from your current cursor position to the start of the arc.
- **Object Allocation:** Paths allocate memory. Never declare `val path = Path()` directly inside a fast-updating animation loop block or `onDrawBehind`. Cache it.

## 💡 Interview Q&A
**Q: What's the difference between a Quadratic Bezier and a Cubic Bezier?**
A: A Quadratic Bezier curve (`quadraticBezierTo`) has exactly one control point that pulls the curve. A Cubic Bezier (`cubicTo`) has two control points, allowing for "S" shapes and more complex inflection points within a single curve segment.
