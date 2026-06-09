# Blend Modes

## 📌 Purpose
`BlendMode` defines how source pixels (the thing you are currently drawing) interact with destination pixels (the canvas content already rendered below it). This is critical for lighting, shadows, compositing masks, and visual filters.

## 📋 Comprehensive BlendMode Table

| BlendMode | Mathematical Concept | Visual Result | Vintage UI Use Case |
|---|---|---|---|
| **Clear** | Dest = 0 | Erases all pixels | Punch-through holes |
| **Src** | Dest = Src | Replaces | Overriding content |
| **Dst** | Dest = Dst | No change | Keep original |
| **SrcOver** | Normal Alpha Blend | Standard | Default Drawing |
| **DstOver** | Behind Src | Background | Underlays |
| **SrcIn** | Src * Dst Alpha | Masked by shape | Tinting icons/shapes |
| **DstIn** | Dst * Src Alpha | Mask effect | Cropping |
| **SrcOut** | Src * (1 - Dst Alpha) | Inverse mask | Drawing outside bounds |
| **DstOut** | Dst * (1 - Src Alpha) | Erase by shape | Subtracting geometry |
| **SrcAtop** | Src atop Dst | Overlay | Coloring existing pixels |
| **Plus** | Src + Dst | Adds colors | **LED/Neon glow blooming** |
| **Modulate** | Src * Dst | Multiplies colors | Darkening, shadowing |
| **Screen** | 1 - (1-Src)(1-Dst) | Brightens colors | **Metallic edge shine** |
| **Overlay** | Screen/Multiply mix | Boosts contrast | Vintage photo filters |
| **Darken** | min(Src, Dst) | Keeps darkest | Shadow overlays |
| **Lighten** | max(Src, Dst) | Keeps lightest | Highlight overlays |
| **ColorDodge** | Brighten by Src | High contrast light | Extreme highlights |
| **ColorBurn** | Darken by Src | Deep contrast dark | Burnt paper edges |
| **HardLight** | Strong Overlay | Harsh highlight | Plastic reflections |
| **SoftLight** | Soft Overlay | Soft glow | Subsurface scattering |
| **Difference** | \|Src - Dst\| | Inverts overlaps | Psychedelic effects |
| **Multiply** | Src * Dst | Darkens | **Contact shadows** |
| **Color** | Hue+Sat from Src, Lum from Dst | Color filter | Monochromatic tinting |

## 🚀 Advanced Examples

### 1. Metallic Shine (Screen Blend)
Use `Screen` mode to simulate specular reflection on a metal surface.
```kotlin
Canvas(modifier = Modifier.size(200.dp)) {
    // 1. Draw base metal (Destination)
    drawRect(Color(0xFF888888)) 
    
    // 2. Draw light glare (Source)
    val highlightBrush = Brush.linearGradient(
        colors = listOf(Color.Transparent, Color.White, Color.Transparent),
        start = Offset(0f, 0f),
        end = Offset(size.width, size.height)
    )
    
    drawRect(
        brush = highlightBrush,
        blendMode = BlendMode.Screen // Blends purely as light
    )
}
```

### 2. Deep Depth Shadow (Multiply)
Use `Multiply` to overlay shadows that react naturally to the colors beneath them, rather than just laying gray pixels over them.
```kotlin
Canvas(modifier = Modifier.size(100.dp)) {
    // Background color
    drawRect(Color.Cyan)
    
    // Shadow layer
    val shadowBrush = Brush.radialGradient(
        colors = listOf(Color.Black.copy(0.5f), Color.Transparent),
        radius = 50f
    )
    
    drawRect(
        brush = shadowBrush,
        blendMode = BlendMode.Multiply // Creates a richer, deeply saturated shadow
    )
}
```

### 3. LED Glow Effect (Plus)
`Plus` physically adds the RGB values together. Red (255,0,0) + Green (0,255,0) = Yellow (255,255,0).
```kotlin
Canvas(modifier = Modifier.fillMaxSize().background(Color.Black)) {
    val radius = 60f
    // Draw Red Light
    drawCircle(color = Color.Red, radius = radius, center = Offset(100f, 100f))
    
    // Draw Blue Light overlapping, using Plus
    drawCircle(
        color = Color.Blue, 
        radius = radius, 
        center = Offset(140f, 100f),
        blendMode = BlendMode.Plus 
    ) // The overlap area will be bright Magenta
}
```

## ⚠️ Common Gotchas
- **Alpha requirement:** Many BlendModes (like `SrcIn`, `DstOut`) rely entirely on the *Alpha* channel of the destination. If your destination is just an opaque rectangle, `SrcIn` will just look exactly like `SrcOver`.
- **Offscreen Buffers:** BlendModes interact with *everything* drawn on the canvas behind them. To limit a blend mode to specific shapes, wrap them in `drawContext.canvas.saveLayer()`.

## 💡 Interview Q&A
**Q: How does `BlendMode.Screen` differ from just drawing a white rectangle with 50% opacity?**
A: A 50% opacity white rectangle washes out the colors underneath, making blacks look gray. `BlendMode.Screen` uses a multiplicative inverse formula that guarantees black pixels are unaffected (0 light added), while white pixels blow out to pure white, accurately simulating real-world additive lighting.
