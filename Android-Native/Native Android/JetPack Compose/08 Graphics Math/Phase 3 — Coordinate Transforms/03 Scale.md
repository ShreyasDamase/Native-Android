# 03 — Scale

> **Core Idea**: Scale multiplies every coordinate by a factor. Uniform scale (same X and Y) just resizes. Non-uniform scale stretches. Scale + rotation + translation together form the **TRS** transform — the universal description of every object in 2D and 3D graphics.

---

## 🧠 Concept

Scaling multiplies the distance of every point from the origin:

```
x' = x × scaleX
y' = y × scaleY
```

If `scaleX = scaleY = 2`, everything doubles in size. If `scaleX = 2, scaleY = 1`, it stretches horizontally only.

**Pivot matters**: Like rotation, scale happens around a pivot. Default = top-left (0,0) in most systems, but `graphicsLayer` uses center by default.

**Why TRS order matters**: Translate → Rotate → Scale (TRS) gives different results than Scale → Rotate → Translate (SRT). In Compose `graphicsLayer`, it's TRS.

---

## 📐 Diagram

```
UNIFORM SCALE:

  Original       × 2.0              × 0.5
  ┌────┐         ┌────────┐         ┌──┐
  │    │   →     │        │   →     │  │
  │    │         │        │         └──┘
  └────┘         └────────┘
  (2×2)          (4×4)              (1×1)

NON-UNIFORM SCALE (scaleX ≠ scaleY):

  Original       scaleX=2, scaleY=1    scaleX=1, scaleY=2
  ┌──┐           ┌────┐                ┌──┐
  │  │    →      │    │        →       │  │
  │  │           └────┘                │  │
  └──┘           (stretched wide)      │  │
                                       └──┘
                                  (stretched tall)

SCALE PIVOT:

  Pivot = center:                 Pivot = top-left:
  ┌──────┐          ┌──────────┐  ┌──────┐  ┌────────────┐
  │  ██  │  ×2 →   │    ██    │  │ ██   │  │ ████       │
  └──────┘          └──────────┘  └──────┘  └────────────┘
  (grows outward)                 (grows right+down)

TRS ORDER — matters!

  T then S:  translate to (100,0), then scale ×2
    → object is at 100px, scaled ×2 from its new position

  S then T:  scale ×2, then translate to (100,0)
    → object is scaled first, then moved 100px
    → translation distance is NOT scaled
```

---

## 🔢 The Math

```kotlin
// Manual scale around a pivot
fun scalePoint(point: Offset, pivot: Offset, scaleX: Float, scaleY: Float): Offset =
    Offset(
        x = pivot.x + (point.x - pivot.x) * scaleX,
        y = pivot.y + (point.y - pivot.y) * scaleY
    )

// In graphicsLayer (composable level):
Modifier.graphicsLayer {
    scaleX = 1.5f
    scaleY = 1.5f
    transformOrigin = TransformOrigin.Center  // default
}

// In Canvas DrawScope:
scale(scaleX = 2f, scaleY = 1f, pivot = Offset(cx, cy)) {
    drawRect(...)
}
```

---

## 🎨 Compose Code

### Example 1: Pulsing circle with animated scale

```kotlin
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun PulsingScaleDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadius = 100f

        // Scale the radius directly (equivalent to Canvas scale)
        val scaledRadius = baseRadius * scale

        // Outer glow rings (scale independently)
        for (i in 3 downTo 1) {
            drawCircle(
                color = Color(0xFF4A9EFF).copy(alpha = 0.08f * i),
                radius = scaledRadius + i * 30f * scale,
                center = Offset(cx, cy)
            )
        }

        drawCircle(Color(0xFF4A9EFF), scaledRadius, Offset(cx, cy))
    }
}
```

### Example 2: Squash and stretch (non-uniform scale for animation life)

```kotlin
@Composable
fun SquashStretchBallDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "squash")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "time"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val floorY = size.height * 0.75f
        val baseRadius = 40f
        val bounceHeight = 200f

        // Ball height using abs(sin) like in the sine note
        val t = kotlin.math.abs(kotlin.math.sin(time))
        val ballY = floorY - bounceHeight * t

        // Squash and stretch:
        // Near floor (t ≈ 0): wide and flat (scaleX > 1, scaleY < 1)
        // At peak (t ≈ 1): tall and thin (scaleX < 1, scaleY > 1)
        val squashFactor = 1f - t  // 1 at floor, 0 at peak
        val scaleX = 1f + squashFactor * 0.6f   // max 1.6 at floor
        val scaleY = 1f - squashFactor * 0.4f   // min 0.6 at floor

        // Floor shadow
        val shadowAlpha = 0.5f - t * 0.4f
        drawOval(
            color = Color(0xFF000000).copy(alpha = shadowAlpha),
            topLeft = Offset(cx - baseRadius * scaleX, floorY - 6f),
            size = androidx.compose.ui.geometry.Size(baseRadius * scaleX * 2f, 12f)
        )

        // Ball (scaled using Canvas scale)
        scale(scaleX, scaleY, Offset(cx, ballY)) {
            drawCircle(Color(0xFFFF6B6B), baseRadius, Offset(cx, ballY))
            drawCircle(Color(0x66FFFFFF), baseRadius * 0.35f,
                Offset(cx - baseRadius * 0.3f, ballY - baseRadius * 0.3f))
        }

        // Floor
        drawLine(Color(0xFF2A2A4A), Offset(0f, floorY), Offset(size.width, floorY), 2f)
    }
}
```

### Example 3: Zoom map — pinch-to-zoom simulation

```kotlin
@Composable
fun ZoomMapDemo() {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val infiniteTransition = rememberInfiniteTransition(label = "zoom")
    val autoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "autoScale"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Apply scale around center
        scale(autoScale, autoScale, Offset(cx, cy)) {
            // Draw a "map" — grid of points
            val gridSpacing = 60f
            for (x in -5..5) {
                for (y in -5..5) {
                    val px = cx + x * gridSpacing
                    val py = cy + y * gridSpacing
                    drawCircle(Color(0xFF2A2A4A), 3f, Offset(px, py))
                }
            }

            // Grid lines
            for (i in -5..5) {
                drawLine(Color(0xFF1A1A3A), Offset(cx + i * gridSpacing, cy - 300f),
                    Offset(cx + i * gridSpacing, cy + 300f), 1f)
                drawLine(Color(0xFF1A1A3A), Offset(cx - 300f, cy + i * gridSpacing),
                    Offset(cx + 300f, cy + i * gridSpacing), 1f)
            }

            // Featured point at center
            drawCircle(Color(0xFFFF6B6B), 12f, Offset(cx, cy))
            drawCircle(Color(0xFFFF6B6B), 24f, Offset(cx, cy),
                style = Stroke(2f))
        }

        // Scale indicator
        val scalePct = (autoScale * 100).toInt()
    }
}
```

---

## 🧪 Trace

**Scale point P=(300, 200) by (2.0, 1.5) around pivot (200, 200)**:

```
Step 1: Translate to pivot
  px = 300 - 200 = 100
  py = 200 - 200 = 0

Step 2: Apply scale
  x' = 100 × 2.0 = 200
  y' = 0   × 1.5 = 0

Step 3: Translate back
  finalX = 200 + 200 = 400
  finalY = 200 + 0   = 200
  P' = (400, 200)
```

**Squash stretch at t=0 (floor contact)**:
```
squashFactor = 1 - 0 = 1
scaleX = 1 + 1 × 0.6 = 1.6  ← 60% wider
scaleY = 1 - 1 × 0.4 = 0.6  ← 40% shorter
→ ball squashes into a wide flat oval on impact ✓
```

---

## 🔗 Next

→ [[04 Project — Solar System]] — *Combine translation + rotation + scale to build an animated solar system with multiple planets and moons*
