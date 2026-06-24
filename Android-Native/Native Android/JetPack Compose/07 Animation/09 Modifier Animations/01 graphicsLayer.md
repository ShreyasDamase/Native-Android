# graphicsLayer — Scale, Rotate, Translate, Alpha

> [!IMPORTANT]
> `graphicsLayer` is the **most performance-critical modifier** in Compose animation. Understanding it is the difference between an animation that runs at 60fps vs one that stutters.

---

## What is `graphicsLayer`?

A modifier that applies **visual transformations** at the Drawing phase — completely bypassing Layout and Composition.

```
Normal animation:  Composition → Layout → Drawing  (slow if animated)
graphicsLayer:     Composition → Layout → [Drawing only] → Drawing  ✅ Fast!
```

```kotlin
Box(
    modifier = Modifier
        .size(100.dp)
        .graphicsLayer {
            scaleX = 1.5f
            scaleY = 1.5f
            rotationZ = 45f
            alpha = 0.8f
        }
)
```

---

## Full Property Reference

```kotlin
Modifier.graphicsLayer {
    // ── Visual Properties ──────────────────────────────
    alpha: Float = 1f              // 0f = invisible, 1f = opaque
    
    scaleX: Float = 1f             // horizontal scale (1f = normal, 2f = double width)
    scaleY: Float = 1f             // vertical scale
    
    rotationX: Float = 0f          // flip top-to-bottom (degrees) [3D perspective]
    rotationY: Float = 0f          // flip left-to-right (degrees) [3D perspective]
    rotationZ: Float = 0f          // spin flat on screen (degrees) [2D rotation]
    
    translationX: Float = 0f       // horizontal shift (pixels, no layout change)
    translationY: Float = 0f       // vertical shift (pixels, no layout change)
    
    shadowElevation: Float = 0f    // drop shadow depth (pixels)
    ambientShadowColor: Color = DefaultShadowColor
    spotShadowColor: Color = DefaultShadowColor
    
    // ── Transform Origin ────────────────────────────────
    transformOrigin: TransformOrigin = TransformOrigin.Center
    // Pivot point for scale/rotation:
    // (0f, 0f) = top-left
    // (0.5f, 0.5f) = center (default)
    // (1f, 1f) = bottom-right
    // (0f, 1f) = bottom-left
    
    // ── Clipping ────────────────────────────────────────
    shape: Shape = RectangleShape  // The clip shape
    clip: Boolean = false          // Whether to clip content to shape
    
    // ── Compositing ─────────────────────────────────────
    renderEffect: RenderEffect? = null  // blur, etc. (Android 12+)
    cameraDistance: Float = ...         // perspective depth for rotationX/Y
    compositingStrategy: CompositingStrategy = CompositingStrategy.Auto
}
```

```text
  ┌─────────────────────────────────────────────────────────────────┐
  │                    3D ROTATION AXES                             │
  │                                                                 │
  │              ▲ Y-axis (rotationY: flips left-to-right)          │
  │              │                                                  │
  │              │    ╭─╮                                           │
  │              │    │⟳│                                           │
  │              │    ╰─╯                                           │
  │              │                                                  │
  │              │          ╭─╮                                     │
  │              └──────────┼─┼────────► X-axis (rotationX: flips   │
  │             ╱           │⟳│                 top-to-bottom)      │
  │            ╱            ╰─╯                                     │
  │           ▼ Z-axis (rotationZ: flat spin on screen)             │
  │         ╭─╮                                                     │
  │         │⟳│                                                     │
  │         ╰─╯                                                     │
  └─────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────┐
  │                    TRANSFORMORIGIN PIVOTS                       │
  │                                                                 │
  │        (0f, 0f)              (0.5f, 0f)             (1f, 0f)    │
  │        [Top-Left]           [Top-Center]          [Top-Right]   │
  │            ┌─────────────────────┬─────────────────────┐        │
  │            │                     │                     │        │
  │            │                     │                     │        │
  │  (0f, 0.5f)├                     ┼ [Center]            ┤(1f,0.5f)│
  │  [Left-Ctr]│                  (0.5f, 0.5f)             │[Rt-Ctr] │
  │            │                     │                     │        │
  │            │                     │                     │        │
  │            └─────────────────────┴─────────────────────┘        │
  │        [Bottom-Left]        [Bottom-Ctr]        [Bottom-Right]  │
  │        (0f, 1f)              (0.5f, 1f)             (1f, 1f)    │
  └─────────────────────────────────────────────────────────────────┘
```

---

## Transform Origin — The Pivot Point

By default, all transforms (scale, rotation) happen around the **center** of the composable.

```kotlin
// Rotate around top-left (like a door hinge on the left)
Modifier.graphicsLayer {
    transformOrigin = TransformOrigin(0f, 0.5f)  // left edge, center height
    rotationY = angle
}

// Scale from bottom-right corner
Modifier.graphicsLayer {
    transformOrigin = TransformOrigin(1f, 1f)  // bottom-right
    scaleX = scale
    scaleY = scale
}

// Scale from top-center (like a dropdown opening)
Modifier.graphicsLayer {
    transformOrigin = TransformOrigin(0.5f, 0f)  // top center
    scaleY = scale
}
```

### TransformOrigin Values
```kotlin
TransformOrigin(pivotFractionX: Float, pivotFractionY: Float)
// X: 0f = left edge, 0.5f = center, 1f = right edge
// Y: 0f = top edge, 0.5f = center, 1f = bottom edge
```

---

## CompositingStrategy

Controls how the layer is composited with the rest of the UI:

| Strategy | Use | Effect |
|----------|-----|--------|
| `Auto` | Default | Compose decides |
| `Offscreen` | Masking, complex blending | Forces separate layer (slower but correct blending) |
| `ModulateAlpha` | Alpha-only animation | Most efficient for alpha changes |

```kotlin
// For masking (compositing correctly)
Modifier.graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
    // Now blendMode works correctly for masking
}

// For fast alpha animation
Modifier.graphicsLayer {
    compositingStrategy = CompositingStrategy.ModulateAlpha
    alpha = animatedAlpha
}
```

---

## Animating with graphicsLayer

### Float animation → graphicsLayer (the golden pattern)

```kotlin
val scale by animateFloatAsState(
    targetValue = if (isSelected) 1.2f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "scale"
)

Box(
    modifier = Modifier
        .size(80.dp)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
)
```

### Multiple properties at once

```kotlin
val transition = updateTransition(targetState = isExpanded, label = "card")
val scale by transition.animateFloat(label = "scale") { if (it) 1.05f else 1f }
val alpha by transition.animateFloat(label = "alpha") { if (it) 1f else 0.7f }
val rotation by transition.animateFloat(label = "rotation") { if (it) 5f else 0f }

Box(
    modifier = Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
        rotationZ = rotation
    }
)
```

---

## Common Animation Patterns

### 1. Press Scale (Button Feedback)
```kotlin
var pressed by remember { mutableStateOf(false) }
val scale by animateFloatAsState(
    targetValue = if (pressed) 0.93f else 1f,
    animationSpec = spring(stiffness = Spring.StiffnessHigh),
    label = "pressScale"
)

Box(
    modifier = Modifier
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                }
            )
        }
)
```

### 2. Rotation Spinner
```kotlin
val angle by rememberInfiniteTransition().animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
    label = "spinAngle"
)

Icon(
    Icons.Default.Refresh,
    contentDescription = null,
    modifier = Modifier.graphicsLayer { rotationZ = angle }
)
```

### 3. 3D Card Flip
```kotlin
var flipped by remember { mutableStateOf(false) }
val rotation by animateFloatAsState(
    targetValue = if (flipped) 180f else 0f,
    animationSpec = tween(600, easing = FastOutSlowInEasing),
    label = "cardFlip"
)

Box(
    modifier = Modifier
        .graphicsLayer {
            rotationY = rotation
            cameraDistance = 12f * density  // Perspective depth
        }
        .clickable { flipped = !flipped }
) {
    if (rotation <= 90f) {
        // Front side
        FrontContent()
    } else {
        // Back side (mirrored)
        Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
            BackContent()
        }
    }
}
```

### 4. Fade + Slide In
```kotlin
val alpha by animateFloatAsState(if (visible) 1f else 0f, label = "alpha")
val slideY by animateFloatAsState(if (visible) 0f else 40f, label = "slideY")

Box(
    modifier = Modifier.graphicsLayer {
        this.alpha = alpha
        translationY = slideY
    }
)
```

### 5. Parallax Effect
```kotlin
@Composable
fun ParallaxItem(scrollOffset: Float) {
    Box(
        modifier = Modifier.graphicsLayer {
            translationY = scrollOffset * 0.5f  // moves at half speed
        }
    ) {
        BackgroundImage()
    }
}
```

---

## graphicsLayer vs Modifier.offset

| | `graphicsLayer { translationX }` | `Modifier.offset(x.dp)` |
|--|----------------------------------|-------------------------|
| Triggers layout | ❌ NO (cheap) | ✅ YES (expensive) |
| Triggers recomposition | ❌ NO | ❌ NO |
| Affects other composables | ❌ NO | ✅ YES (moves others) |
| Uses pixels | ✅ Px | ❌ Dp |
| Animation use | ✅ Preferred | ⚠️ Only if you need layout |

> [!WARNING]
> Use `graphicsLayer { translationX = px }` for animations. Use `Modifier.offset(dp)` only when you need the shift to affect the layout of sibling composables.

---

## graphicsLayer vs Modifier.alpha

| | `graphicsLayer { alpha }` | `Modifier.alpha()` |
|--|---------------------------|---------------------|
| Performance | ✅ Better | ✅ Similar |
| Combines with other transforms | ✅ Yes (single layer) | Limited |
| Compositing control | ✅ Yes | ❌ No |

Both are fine for simple alpha. `graphicsLayer` is preferred when you're also doing scale/rotation, to avoid multiple layers.

---

## renderEffect — Blur (Android 12+)

Apply visual effects like blur using `renderEffect`:

```kotlin
val blurRadius = 10f

Modifier.graphicsLayer {
    renderEffect = BlurEffect(
        radiusX = blurRadius,
        radiusY = blurRadius,
        edgeTreatment = TileMode.Clamp
    )
}
```

> [!NOTE]
> `renderEffect` requires Android 12 (API 31+). Check API level before using.

---

## Performance Notes

> [!IMPORTANT]
> `graphicsLayer` creates a **separate render layer**. This is fast for animations but consumes extra memory. Avoid using it on every item in a large list.

- **DO**: Use on interactive elements, animated components
- **AVOID**: Using on every item in LazyColumn (unless necessary)
- **USE**: `Modifier.graphicsLayer(alpha = alpha)` shorthand for simple single-property changes

---

## 💡 Interview Q&A

**Q: Why is `graphicsLayer` preferred over `Modifier.offset` for animations?**
A: `graphicsLayer` operates at the Drawing phase — it doesn't affect layout, so other composables don't move, and no layout recalculation happens. `Modifier.offset` triggers layout recalculation on every frame, which is significantly more expensive.

**Q: What is `transformOrigin` and why does it matter?**
A: It's the pivot point for scale and rotation. By default, transforms happen around the center. Changing it lets you rotate around an edge (like a door hinge) or scale from a corner.

**Q: What does `CompositingStrategy.Offscreen` do?**
A: It forces the composable to render into an off-screen buffer before compositing with the rest. This is needed for correct masking and complex blend modes, but it's slower than the default.

---

**Next:** [[07 Animation/12 Recipes/05 Animated Switch|Animated Switch Recipe →]]
