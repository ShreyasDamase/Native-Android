# Text on Canvas

## 📌 Purpose
Drawing text on a Canvas requires complex measurements (font metrics, kerning, line-wrapping). Compose handles this via the `TextMeasurer` API. This is critical for drawing labels on custom meters, gauges, charts, and watermarks.

## 🔧 API Breakdown

### 1. Initialization
You must create and remember a `TextMeasurer` outside the drawing phase.
```kotlin
val textMeasurer = rememberTextMeasurer()
```

### 2. Measuring Text
Pre-measure text if you need to know its exact width/height *before* drawing (e.g., to center it).
```kotlin
val textLayoutResult: TextLayoutResult = textMeasurer.measure(
    text = "Vintage Audio",
    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
    constraints = Constraints(), // optional
    maxLines = 1
)
```

### 3. Drawing Text
```kotlin
// Draw pre-measured text
drawText(
    textLayoutResult = textLayoutResult,
    topLeft = Offset(x, y),
    color = Color.Black
)

// Draw directly (measures and draws in one call)
drawText(
    textMeasurer = textMeasurer,
    text = "Direct Draw",
    topLeft = Offset(x, y),
    style = TextStyle(color = Color.Red)
)
```

## 🚀 Advanced Examples

### 1. Perfectly Centered Text
Because text coordinates are `topLeft`, you must subtract half the text bounds to center it.
```kotlin
val textMeasurer = rememberTextMeasurer()

Canvas(modifier = Modifier.size(200.dp)) {
    val textResult = textMeasurer.measure(
        text = "Center",
        style = TextStyle(fontSize = 24.sp)
    )
    
    drawText(
        textLayoutResult = textResult,
        topLeft = Offset(
            x = center.x - textResult.size.width / 2f,
            y = center.y - textResult.size.height / 2f
        ),
        color = Color.Black
    )
}
```

### 2. Circular Meter Labels (Text Along an Arc)
Drawing vintage gauge numbers using polar coordinates and rotation.
```kotlin
val textMeasurer = rememberTextMeasurer()
val labels = listOf("0", "2", "4", "6", "8", "10")

Canvas(modifier = Modifier.size(200.dp)) {
    val labelRadius = size.width / 2f - 30f
    val startAngle = 180f // left
    val angleStep = 180f / (labels.size - 1)
    
    for ((i, label) in labels.withIndex()) {
        val angle = startAngle + (i * angleStep)
        val angleRad = Math.toRadians(angle.toDouble()).toFloat()
        
        // Polar to Cartesian
        val x = center.x + (labelRadius * cos(angleRad))
        val y = center.y + (labelRadius * sin(angleRad))
        
        val textResult = textMeasurer.measure(text = label, style = TextStyle(fontSize = 12.sp))
        
        // Rotate the canvas around the text's coordinate so the text points inward
        rotate(degrees = angle + 90f, pivot = Offset(x, y)) {
            drawText(
                textLayoutResult = textResult,
                topLeft = Offset(
                    x = x - textResult.size.width / 2f,
                    y = y - textResult.size.height / 2f
                ),
                color = Color.Gray
            )
        }
    }
}
```

## ⚠️ Common Gotchas
- **Creation in Draw:** Never call `rememberTextMeasurer()` inside the `Canvas` lambda. The lambda is the draw phase, and you cannot use Composable functions (`remember`) there.
- **TopLeft vs Baseline:** `drawText` places text using the top-left bounding box, *not* the text baseline. Be careful when trying to align canvas text with standard UI elements.

## 💡 Interview Q&A
**Q: How did we draw text on Canvas before `TextMeasurer` was introduced in Compose 1.3?**
A: We had to use `drawIntoCanvas` to access the native Android Canvas (`nativeCanvas`) and use the legacy `android.graphics.Paint` and `nativeCanvas.drawText()` APIs. `TextMeasurer` makes this completely obsolete and cross-platform compatible.
