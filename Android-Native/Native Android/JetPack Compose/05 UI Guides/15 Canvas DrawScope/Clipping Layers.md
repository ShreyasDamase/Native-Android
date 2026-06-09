# Clipping and Layers

## 📌 Purpose
Clipping defines *where* drawing operations are allowed to render on the canvas. Layers (`saveLayer`) allow you to group drawing operations into an offscreen buffer, enabling you to apply transparency, blending, or masking to an entire group of shapes at once.

## 🔧 APIs

### Clipping Functions
```kotlin
// Restrict drawing to a rectangle
clipRect(left, top, right, bottom, clipOp = ClipOp.Intersect) { ... }

// Restrict drawing to a complex path
clipPath(path: Path, clipOp = ClipOp.Intersect) { ... }

// ClipOp:
// ClipOp.Intersect = keep inside the clip bounds
// ClipOp.Difference = keep outside the clip bounds (punch a hole)
```

### Layer Composition
```kotlin
// Allocates an offscreen buffer
drawContext.canvas.saveLayer(bounds: Rect, paint: Paint)

// Compose extension method
drawIntoCanvas { canvas ->
    canvas.saveLayer(bounds, paint)
    // draw stuff...
    canvas.restore()
}
```

## 🚀 Advanced Examples

### 1. Circular Avatar Clip
Clipping an image to a circle.
```kotlin
Canvas(modifier = Modifier.size(100.dp)) {
    val imagePath = Path().apply {
        addOval(Rect(0f, 0f, size.width, size.height))
    }
    
    clipPath(path = imagePath) {
        // This image will only draw inside the circle path
        drawImage(
            image = myImageBitmap,
            dstSize = IntSize(size.width.toInt(), size.height.toInt())
        )
    }
}
```

### 2. Reveal Animation (Wiper effect)
Using `clipRect` to reveal content progressively.
```kotlin
val revealProgress by animateFloatAsState(targetValue = 1f, tween(1000))

Canvas(modifier = Modifier.size(200.dp)) {
    // Only the left percentage of the canvas is drawn
    clipRect(right = size.width * revealProgress) {
        drawRect(Color.Blue)
        drawText(textMeasurer, "Revealed Text", topLeft = center)
    }
}
```

### 3. Transparent Punch-Through (saveLayer)
To cut a transparent hole into a solid shape, you cannot just draw a transparent circle on top. You must render to a layer, then use `BlendMode.Clear`.
```kotlin
Canvas(modifier = Modifier.fillMaxSize()) {
    // We MUST use drawIntoCanvas for saveLayer
    drawIntoCanvas { canvas ->
        // Create an offscreen layer
        canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
        
        // 1. Draw solid background
        drawRect(Color.Black.copy(alpha = 0.8f))
        
        // 2. Punch a hole using BlendMode.Clear
        drawCircle(
            color = Color.Transparent, 
            radius = 100f, 
            center = center,
            blendMode = BlendMode.Clear // Erases pixels in the offscreen buffer!
        )
        
        // 3. Render the buffer to the screen
        canvas.restore()
    }
}
```

## ⚠️ Common Gotchas
- **`saveLayer` Performance:** `saveLayer` is very expensive. It allocates a bitmap in GPU memory the size of the bounds provided. Always provide the smallest `bounds` `Rect` possible instead of the full screen.
- **Hardware Acceleration limitations:** `ClipOp.Difference` may behave inconsistently on very old Android versions due to hardware acceleration bugs, but works perfectly on modern devices.

## 💡 Interview Q&A
**Q: Why doesn't `BlendMode.Clear` work if I just call `drawCircle(..., blendMode = BlendMode.Clear)` directly on the canvas without `saveLayer`?**
A: If you don't use `saveLayer`, `BlendMode.Clear` will erase the pixels straight down to the window background (resulting in a black or white square depending on the OS theme), because it clears *everything* rendered so far. `saveLayer` isolates the operation so you only erase the background *you* drew in that layer, revealing the Compose UI underneath.
