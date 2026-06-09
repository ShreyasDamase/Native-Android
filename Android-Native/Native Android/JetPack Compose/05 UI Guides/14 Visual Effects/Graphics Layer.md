# GraphicsLayer

## 📌 Purpose
`Modifier.graphicsLayer` is the primary way to apply hardware-accelerated visual effects, transformations, and compositing strategies. It runs in the drawing phase, meaning changes here do not trigger recomposition or layout passes, making it highly performant for animations.

## 🔧 Function Signature
```kotlin
@Stable
fun Modifier.graphicsLayer(
    scaleX: Float = 1f,
    scaleY: Float = 1f,
    alpha: Float = 1f,
    translationX: Float = 0f,
    translationY: Float = 0f,
    shadowElevation: Float = 0f,
    rotationX: Float = 0f,
    rotationY: Float = 0f,
    rotationZ: Float = 0f,
    cameraDistance: Float = DefaultCameraDistance,
    transformOrigin: TransformOrigin = TransformOrigin.Center,
    shape: Shape = RectangleShape,
    clip: Boolean = false,
    renderEffect: RenderEffect? = null,
    ambientShadowColor: Color = DefaultShadowColor,
    spotShadowColor: Color = DefaultShadowColor,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Auto
): Modifier
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `scaleX` / `scaleY` | `Float` | `1f` | Scales the content horizontally/vertically. |
| `alpha` | `Float` | `1f` | Opacity of the layer (0f to 1f). |
| `translationX` / `Y` | `Float` | `0f` | Moves the content by pixels (not dp). |
| `shadowElevation` | `Float` | `0f` | Adds a drop shadow. Requires `shape` to be set. |
| `rotationX` / `Y` / `Z` | `Float` | `0f` | Rotates the content in 3D space (X/Y) or 2D (Z). |
| `cameraDistance` | `Float` | `8.0f` | Distance of the virtual 3D camera. |
| `transformOrigin` | `TransformOrigin`| `Center` | Pivot point for scaling and rotation. |
| `shape` | `Shape` | `Rectangle` | Defines the clipping and shadow bounds. |
| `clip` | `Boolean` | `false` | If true, clips content to the `shape`. |
| `renderEffect` | `RenderEffect?` | `null` | API 31+. Applies blur, color filters, etc. |
| `ambientShadowColor` | `Color` | `Black` | Color of ambient (diffuse) shadow. |
| `spotShadowColor` | `Color` | `Black` | Color of the directional spot shadow. |
| `compositingStrategy`| `CompositingStrategy` | `Auto` | Determines offscreen buffer usage. |

> [!NOTE] CompositingStrategy
> - **Auto:** Compose decides when to use an offscreen buffer.
> - **Offscreen:** Always renders to a buffer first (critical for `alpha` + overlapping children).
> - **ModulateAlpha:** Applies alpha without a buffer (efficient but limited).

## ✅ Basic Example
```kotlin
Box(
    modifier = Modifier
        .size(100.dp)
        .graphicsLayer {
            alpha = 0.5f
            scaleX = 1.2f
            scaleY = 1.2f
            rotationZ = 45f
        }
        .background(Color.Blue)
)
```

## 🚀 Advanced Examples

### 1. 3D Card Flip Animation
```kotlin
var isFlipped by remember { mutableStateOf(false) }
val rotation by animateFloatAsState(targetValue = if (isFlipped) 180f else 0f)

Card(
    modifier = Modifier
        .size(200.dp, 300.dp)
        .graphicsLayer {
            rotationY = rotation
            cameraDistance = 12f * density // Prevents distortion during 3D flip
        }
        .clickable { isFlipped = !isFlipped }
) {
    if (rotation <= 90f) {
        Text("Front")
    } else {
        Text("Back", modifier = Modifier.graphicsLayer { rotationY = 180f }) // Un-flip content
    }
}
```

### 2. Parallax Effect on Scroll
```kotlin
val scrollState = rememberScrollState()

Column(modifier = Modifier.verticalScroll(scrollState)) {
    Image(
        painter = painterResource(R.drawable.header),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .graphicsLayer {
                translationY = scrollState.value * 0.5f // Move at half speed
                alpha = 1f - (scrollState.value / 600f).coerceIn(0f, 1f)
            },
        contentScale = ContentScale.Crop
    )
    // Scrollable content...
}
```

### 3. RenderEffect Chain (API 31+)
```kotlin
Modifier.graphicsLayer {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val blur = BlurEffect(20f, 20f)
        val colorFilter = ColorFilterEffect(ColorFilter.tint(Color.Red, BlendMode.Screen))
        renderEffect = blur.then(colorFilter)
    }
}
```

## ⚠️ Common Gotchas
- **Clipping vs Transformation:** `graphicsLayer` transforms the *drawn pixels*, not the layout bounds. The component still occupies its original layout space.
- **Translation uses Pixels:** `translationX/Y` take plain `Float` pixels. Use `with(LocalDensity.current) { 10.dp.toPx() }` to translate by dp.

## 💡 Interview Q&A
**Q: Why use `Modifier.graphicsLayer` instead of `Modifier.scale()` or `Modifier.alpha()`?**
A: `Modifier.scale` and `Modifier.alpha` are actually convenience functions that call `graphicsLayer` internally. Using a single `graphicsLayer` block is more performant if you are animating multiple properties (like scale, alpha, and rotation together) because it avoids instantiating multiple modifier objects and groups the transformations in a single render pass.
