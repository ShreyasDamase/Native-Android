# Color Effects

## 📌 Purpose
Color effects allow pixel-level manipulation of rendering via `ColorMatrix`, `ColorFilter`, and `BlendMode`. They are essential for vintage photo filters, overlays, screen blending, and dynamic tinting.

## 🔧 APIs

### ColorFilter
```kotlin
// Tint overlay
ColorFilter.tint(color: Color, blendMode: BlendMode = BlendMode.SrcIn)

// Matrix transformation
ColorFilter.colorMatrix(colorMatrix: ColorMatrix)

// Lighting (multiply & add)
ColorFilter.lighting(multiply: Color, add: Color)
```

### ColorMatrix
A 5x4 matrix representing RGBA transformations.
```kotlin
val colorMatrix = ColorMatrix()
colorMatrix.setToSaturation(0f) // Quick grayscale
colorMatrix.setToScale(redScale, greenScale, blueScale, alphaScale)
```

## 📋 BlendMode Reference Table

| BlendMode | Visual Result | Vintage / Common Use |
|---|---|---|
| `Clear` | Erases pixels | Cutouts / Punch-through |
| `SrcOver` | Standard composite | Default drawing |
| `SrcIn` | Masked by target shape | Tinting icons |
| `DstOut` | Erase by shape | Reverse masking |
| `Plus` | Adds colors | LED/Neon Glow effects |
| `Screen` | Brightens | **Metallic shine, highlights** |
| `Multiply` | Darkens | **Deep shadow layers** |
| `Overlay` | Boosts contrast | **Vintage photo effect** |
| `Hardlight` | Strong contrast | Harsh highlight |
| `Softlight` | Soft contrast | Soft glow / atmosphere |

## ✅ Basic Example (Grayscale Image)

```kotlin
val grayscale = ColorMatrix().apply { setToSaturation(0f) }

Image(
    painter = painterResource(id = R.drawable.photo),
    contentDescription = "Grayscale Photo",
    colorFilter = ColorFilter.colorMatrix(grayscale)
)
```

## 🚀 Advanced Examples

### 1. Custom Matrix Presets (Vintage/Sepia)
```kotlin
// Sepia
val sepia = ColorMatrix(floatArrayOf(
    0.393f, 0.769f, 0.189f, 0f, 0f,
    0.349f, 0.686f, 0.168f, 0f, 0f,
    0.272f, 0.534f, 0.131f, 0f, 0f,
    0f,     0f,     0f,     1f, 0f
))

// Invert / Negative
val invert = ColorMatrix(floatArrayOf(
    -1f, 0f,  0f,  0f, 255f,
     0f, -1f, 0f,  0f, 255f,
     0f, 0f,  -1f, 0f, 255f,
     0f, 0f,  0f,  1f, 0f
))

// Warm / Night mode
val warmTone = ColorMatrix(floatArrayOf(
    1.2f, 0f,   0f,   0f, 0f,
    0f,   1f,   0f,   0f, 0f,
    0f,   0f,   0.8f, 0f, 0f,
    0f,   0f,   0f,   1f, 0f
))

// Usage:
Modifier.graphicsLayer { colorFilter = ColorFilter.colorMatrix(sepia) }
```

### 2. Animated Saturation
```kotlin
val isGrayscale by remember { mutableStateOf(false) }
val saturation by animateFloatAsState(targetValue = if (isGrayscale) 0f else 1f)

Image(
    painter = painterResource(R.drawable.photo),
    contentDescription = null,
    modifier = Modifier.clickable { isGrayscale = !isGrayscale },
    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) })
)
```

### 3. Screen Blend for Glow Effect
Using `BlendMode.Screen` drops the dark pixels and adds the light pixels, creating a glowing or metallic reflection effect.
```kotlin
Box(modifier = Modifier.size(200.dp)) {
    Image(painterResource(R.drawable.base_metal), null)
    
    // Highlight overlay
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gradient = Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.5f), Color.Transparent)
        )
        drawRect(
            brush = gradient,
            blendMode = BlendMode.Screen
        )
    }
}
```

## ⚠️ Common Gotchas
- **Performance:** `ColorFilter` is hardware accelerated and very fast, but using complex `BlendMode` operations with large overlapping areas or offscreen buffers can cause overdraw.
- **Src vs Dst:** In BlendModes, `Src` is the thing you are currently drawing. `Dst` is the canvas/pixels already rendered below it.

## 💡 Interview Q&A
**Q: How would you tint an icon to be solid red?**
A: Use `ColorFilter.tint(Color.Red, BlendMode.SrcIn)`. The `SrcIn` mode ensures the red color (source) only draws where the icon's non-transparent pixels (destination) exist.
