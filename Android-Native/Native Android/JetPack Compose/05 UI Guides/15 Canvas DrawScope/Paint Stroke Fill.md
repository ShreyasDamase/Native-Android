# Paint, Stroke, and Fill

## 📌 Purpose
Every drawing operation in Compose allows you to specify a `DrawStyle` (`Fill` or `Stroke`). The `Stroke` style unlocks powerful visual features like custom line caps, dashing patterns (`PathEffect`), and corner joins. Additionally, `BlurMaskFilter` can be applied to paint objects to create drop shadows and glows.

## 🔧 DrawStyle Signatures

```kotlin
// Solid fill (Default)
val fillStyle = Fill

// Outline stroke
val stroke = Stroke(
    width: Float = 0.0f,
    miter: Float = Stroke.DefaultMiter,
    cap: StrokeCap = StrokeCap.Butt,
    join: StrokeJoin = StrokeJoin.Miter,
    pathEffect: PathEffect? = null
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `width` | `Float` | `0f` | Thickness of the line in pixels. |
| `cap` | `StrokeCap` | `Butt` | End of the line: `Butt` (flat), `Round`, `Square` (flat but extends). |
| `join` | `StrokeJoin`| `Miter` | Corner connections: `Miter` (sharp), `Round`, `Bevel` (cut off). |
| `miter` | `Float` | `4f` | Limit for miter join length before falling back to bevel. |
| `pathEffect`| `PathEffect?`| `null` | Applies dashing, stamping, or corner rounding. |

## 🚀 Advanced Examples

### 1. Dashed Lines & PathEffects
`PathEffect.dashPathEffect` takes an array of floats `[drawLength, spaceLength, drawLength, ...]`.
```kotlin
Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
    drawLine(
        color = Color.Gray,
        start = Offset(0f, center.y),
        end = Offset(size.width, center.y),
        strokeWidth = 4f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), phase = 0f)
    )
}
```

### 2. Rounded Corners on a Custom Polygon
Using `cornerPathEffect` avoids manually calculating bezier curves for sharp corners.
```kotlin
Canvas(modifier = Modifier.size(100.dp)) {
    val path = Path().apply {
        moveTo(50f, 0f)
        lineTo(100f, 100f)
        lineTo(0f, 100f)
        close()
    }
    
    drawPath(
        path = path,
        color = Color.Blue,
        style = Stroke(
            width = 4f,
            pathEffect = PathEffect.cornerPathEffect(radius = 16f) // Rounds the sharp triangle corners
        )
    )
}
```

### 3. Glows & Shadows (BlurMaskFilter)
To apply a `BlurMaskFilter`, you must drop down to the underlying `android.graphics.Paint` using `drawIntoCanvas`.

**Blur Types:**
- `NORMAL`: Blurs inside and outside (standard blur).
- `SOLID`: Blurs outside, keeps inside solid.
- `OUTER`: Blurs outside, clears inside (perfect for halos/glows).
- `INNER`: Blurs inside, clears outside.

```kotlin
Canvas(modifier = Modifier.size(150.dp)) {
    drawIntoCanvas { canvas ->
        // Create Neon Glow
        val paint = Paint().apply {
            color = Color.Cyan
            asFrameworkPaint().apply {
                maskFilter = android.graphics.BlurMaskFilter(30f, android.graphics.BlurMaskFilter.Blur.OUTER)
            }
        }
        
        // Draw the glow layer
        canvas.drawCircle(center, 50f, paint)
        
        // Draw the solid core on top
        drawCircle(color = Color.White, radius = 50f, center = center)
    }
}
```

## ⚠️ Common Gotchas
- **Stroke Width Expansion:** When drawing a `Stroke`, the line expands evenly outward and inward from the coordinates. E.g., drawing a border around a rectangle with `Stroke(10f)` means 5px of the border is *inside* the rect, and 5px is *outside*.
- **Hardware Acceleration:** Some complex `PathEffect` chains might cause performance drops or visual glitches depending on OEM hardware acceleration implementations.

## 💡 Interview Q&A
**Q: How do you create an animated dotted line ("marching ants" effect)?**
A: Use a `PathEffect.dashPathEffect` and animate the `phase` parameter over time using `infiniteTransition.animateFloat()`. This shifts the dash pattern along the path continuously.
