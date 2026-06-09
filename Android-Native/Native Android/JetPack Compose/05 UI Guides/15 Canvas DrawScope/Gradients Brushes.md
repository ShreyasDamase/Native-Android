# Gradients & Brushes

## 📌 Purpose
`Brush` defines how colors are painted into a shape. Beyond solid colors, gradients are critical for rendering 3D volume, shadows, metallic surfaces, and lighting (especially for skeuomorphic/vintage UI).

## 🔧 Brush Signatures

```kotlin
// Linear
Brush.linearGradient(
    colorStops: Array<Pair<Float, Color>>,
    start: Offset,
    end: Offset,
    tileMode: TileMode = TileMode.Clamp
)

// Radial
Brush.radialGradient(
    colorStops: Array<Pair<Float, Color>>,
    center: Offset,
    radius: Float,
    tileMode: TileMode = TileMode.Clamp
)

// Sweep (Angular/Conic)
Brush.sweepGradient(
    colorStops: Array<Pair<Float, Color>>,
    center: Offset
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `colorStops` | `Array` | — | Pairs of `Float` (0.0 to 1.0 position) and `Color`. |
| `start` / `end` | `Offset` | `(0,0)` to `Infinity` | Linear gradient direction/length. |
| `center` | `Offset` | Center of area | Origin point for radial/sweep gradients. |
| `radius` | `Float` | Inf | How far the radial gradient extends. |
| `tileMode` | `TileMode` | `Clamp` | `Clamp` (extend edge), `Repeated` (loop), `Mirror` (bounce). |

## 🚀 Advanced Examples (Vintage UI Focus)

### 1. The Classic Bevel Effect (3D Button)
A linear gradient from top-left (light) to bottom-right (dark) creates the illusion of raised geometry.
```kotlin
Canvas(modifier = Modifier.size(150.dp)) {
    val bevelGradient = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to Color(0xFFE8E8E8), // Top-left highlight
            0.5f to Color(0xFFA0A0A0), // Midtone
            1.0f to Color(0xFF505050)  // Bottom-right shadow
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )
    
    drawRoundRect(
        brush = bevelGradient,
        cornerRadius = CornerRadius(16f)
    )
}
```

### 2. Metallic Radial Knob
By offsetting the center of a radial gradient, you simulate a specular light reflection.
```kotlin
Canvas(modifier = Modifier.size(100.dp)) {
    val metallicKnob = Brush.radialGradient(
        colorStops = arrayOf(
            0.0f to Color(0xFFFFFFFF),  // Hot bright specular highlight
            0.2f to Color(0xFFD8D8D8),  // Light metal
            0.6f to Color(0xFF888888),  // Mid gray
            1.0f to Color(0xFF404040)   // Dark rim shadow
        ),
        center = Offset(size.width * 0.35f, size.height * 0.35f), // Offset top-left
        radius = size.width * 0.7f
    )
    
    drawCircle(brush = metallicKnob)
}
```

### 3. Sweep Gradient for Dials & Color Wheels
Sweep gradient rotates colors around a center point (also known as a conic gradient).
```kotlin
Canvas(modifier = Modifier.size(150.dp)) {
    val sweep = Brush.sweepGradient(
        colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
        center = center
    )
    
    drawCircle(
        brush = sweep,
        style = Stroke(width = 30f) // Creates a ring instead of a full circle
    )
}
```

### 4. Brushed Metal Texture Simulation
Using many color stops to simulate anisotropic reflections (hairline scratches).
```kotlin
Canvas(modifier = Modifier.fillMaxSize()) {
    val brushedMetal = Brush.linearGradient(
        colorStops = (0..20).map { i ->
            (i / 20f) to if (i % 2 == 0) Color(0xFFBBBBBB) else Color(0xFF999999)
        }.toTypedArray(),
        start = Offset.Zero,
        end = Offset(0f, size.height) // Vertical stripes
    )
    drawRect(brush = brushedMetal)
}
```

## ⚠️ Common Gotchas
- **Infinity Defaults:** If you don't explicitly set `end` on a `linearGradient`, it defaults to `Offset.Infinite`, meaning your gradient will look like a solid color because the "end" is millions of pixels away. Always provide `start` and `end`.
- **Caching:** Creating `Brush` instances inside `onDraw` is bad for performance. Pre-calculate them using `remember` or `drawWithCache`.

## 💡 Interview Q&A
**Q: How do you create a repeating striped background using gradients?**
A: Use a `Brush.linearGradient` with hard color stops (e.g., `0.0 to Red, 0.5 to Red, 0.5 to White, 1.0 to White`), and set the `tileMode` to `TileMode.Repeated`. Set the `start` and `end` coordinates very close to each other (e.g., `0f` to `20f`) to define the width of a single stripe pattern.
