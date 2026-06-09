# Blur Effects

## 📌 Purpose
Blurring creates visual depth, shifts user focus, and enables modern UI patterns like glassmorphism (frosted glass). Compose provides component-level blur, but OS support dictates which APIs to use.

## 🔧 Function Signature
```kotlin
@Stable
fun Modifier.blur(
    radiusX: Dp,
    radiusY: Dp,
    edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Rectangle
): Modifier
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `radiusX` | `Dp` | — | Required. Blur radius along the X axis. |
| `radiusY` | `Dp` | — | Required. Blur radius along the Y axis. |
| `edgeTreatment` | `BlurredEdgeTreatment` | `Rectangle` | Optional. `Rectangle` clips to bounds; `Unbounded` lets blur bleed outside bounds. |

> [!WARNING] API Requirement
> `Modifier.blur()` and `RenderEffect` require **Android 12 (API 31+)**. On older APIs, they gracefully fallback to a no-op (no blur happens).

## ✅ Basic Example
```kotlin
Image(
    painter = painterResource(R.drawable.photo),
    contentDescription = null,
    modifier = Modifier
        .size(200.dp)
        .blur(radiusX = 16.dp, radiusY = 16.dp)
)
```

## 🚀 Advanced Examples

### 1. `Modifier.graphicsLayer` with RenderEffect (API 31+)
Allows chaining effects and setting explicit `TileMode`.

```kotlin
Box(
    modifier = Modifier
        .size(200.dp)
        .graphicsLayer {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                renderEffect = BlurEffect(
                    radiusX = 20f,
                    radiusY = 20f,
                    edgeTreatment = TileMode.Decal
                )
            }
        }
) {
    Text("Blurred content")
}
```

### 2. Canvas BlurMaskFilter (Legacy - All APIs)
Only blurs Canvas-drawn shapes, not composables.

```kotlin
Canvas(modifier = Modifier.size(100.dp)) {
    drawIntoCanvas { canvas ->
        canvas.drawRect(
            Rect(0f, 0f, size.width, size.height),
            Paint().apply {
                asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(20f, 0f, 0f, android.graphics.Color.argb(180, 0, 0, 0))
                }
            }
        )
    }
}
```

### 3. Glassmorphism Pattern
Creating a frosted glass card overlay.

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    Image(painter = painterResource(R.drawable.bg), contentDescription = null, modifier = Modifier.fillMaxSize())
    
    // Blurred copy of the content
    Box(
        modifier = Modifier
            .fillMaxSize()
            .blur(20.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
    )
    
    // Frosted glass card on top
    Card(
        modifier = Modifier.align(Alignment.Center).size(300.dp, 200.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text("Frosted Glass")
        }
    }
}
```

### 4. Haze Library (API < 31 Backdrop Blur)
For production apps needing backdrop blur on all devices.

```kotlin
// build.gradle: implementation("dev.chrisbanes.haze:haze:1.x.x")

val hazeState = rememberHazeState()

Box {
    LazyColumn(
        modifier = Modifier.haze(hazeState)
    ) { 
        items(50) { Text("Item $it") } 
    }
    
    BottomAppBar(
        modifier = Modifier.align(Alignment.BottomCenter).hazeChild(
            state = hazeState,
            style = HazeDefaults.style(backgroundColor = Color.White.copy(0.3f))
        )
    ) { 
        Text("Glass Bottom Bar") 
    }
}
```

## ⚠️ Common Gotchas
- **Component Blur vs Backdrop Blur:** `Modifier.blur()` blurs the composable itself. It does *not* blur the content behind it like an iOS frosted glass pane.
- **Performance:** Animating the blur radius is highly expensive. Instead, pre-blur an image and animate its `alpha` over the sharp image.

## 💡 Interview Q&A
**Q: How do you support blur on Android 10 (API 29)?**
A: `Modifier.blur` does not work below API 31. For older versions, you must use a library like `Haze` (which captures content and blurs it) or use `BlurMaskFilter` within a `Canvas` for basic shape shadows and blurs.
