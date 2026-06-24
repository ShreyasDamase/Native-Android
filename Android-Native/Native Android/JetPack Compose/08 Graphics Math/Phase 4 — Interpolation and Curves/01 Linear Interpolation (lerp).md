# 01 — Linear Interpolation (lerp)

> **Core Idea**: lerp(a, b, t) gives you any point between `a` and `b`. At t=0 you get `a`, at t=1 you get `b`, at t=0.5 you get the midpoint. This is the mathematical foundation of every animation transition, color blend, and smooth camera follow.

---

## 🧠 Concept

`lerp` = **L**inear int**erp**olation.

You have a start value and an end value. You want to smoothly travel between them over time.

```
lerp(start, end, t) = start + (end - start) × t
                    = start × (1 - t) + end × t
```

Where `t` is the progress, from 0.0 to 1.0.

**Important**: `t` doesn't have to go linearly. If you feed a curved `t` (easing), the motion will accelerate/decelerate smoothly. Lerp itself is linear — the easing comes from how you drive `t`.

---

## 📐 Diagram

```
LINEAR INTERPOLATION — t from 0 to 1:

  start = 100        end = 500
  ├──────────────────────────────────────────────┤
  │         │         │         │         │      │
  0        0.25      0.5       0.75      1.0
  100      200       300       400       500

  lerp(100, 500, 0.0)  = 100  (at start)
  lerp(100, 500, 0.25) = 200  (quarter way)
  lerp(100, 500, 0.5)  = 300  (midpoint)
  lerp(100, 500, 0.75) = 400  (three-quarters)
  lerp(100, 500, 1.0)  = 500  (at end)

2D lerp (between two points):

  A (100, 100)                        B (400, 300)
       *──────────────────────────────────* 
       │←───── t=0.5 midpoint ──────────→│
                  * (250, 200)

COLOR lerp (red → blue, t=0.5):

  Red (255, 0, 0) ─────────────────── Blue (0, 0, 255)
  R: 255, G: 0, B: 0       R: 128, G: 0, B: 128       R: 0, G: 0, B: 255
  ─────────────────────────────────────────────────────

SMOOTH FOLLOW — exponential lerp (damp):
  Every frame: position = lerp(position, target, 0.1f)
  
  target = 500
  frame 1: pos = lerp(100, 500, 0.1) = 140
  frame 2: pos = lerp(140, 500, 0.1) = 176
  frame 3: pos = lerp(176, 500, 0.1) = 208.4
  → Never quite reaches target but gets arbitrarily close (exponential decay)
```

---

## 🔢 The Math

```kotlin
// Basic lerp
fun lerp(start: Float, end: Float, t: Float): Float =
    start + (end - start) * t

// Clamped lerp (t stays in 0..1)
fun lerpClamped(start: Float, end: Float, t: Float): Float =
    start + (end - start) * t.coerceIn(0f, 1f)

// 2D lerp
fun lerpOffset(start: Offset, end: Offset, t: Float): Offset =
    Offset(lerp(start.x, end.x, t), lerp(start.y, end.y, t))

// Color lerp
fun lerpColor(from: Color, to: Color, t: Float): Color = Color(
    red   = lerp(from.red, to.red, t),
    green = lerp(from.green, to.green, t),
    blue  = lerp(from.blue, to.blue, t),
    alpha = lerp(from.alpha, to.alpha, t)
)

// Smooth follow (call every frame): position = lerp(position, target, speed)
// speed ≈ 0.05 = slow follow, 0.2 = fast follow, 1.0 = instant
```

---

## 🎨 Compose Code

### Example 1: Lerp-driven color transition

```kotlin
@Composable
fun ColorLerpDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "color")
    val t by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "t"
    )

    // Lerp between two colors manually using math
    val fromColor = Color(0xFFFF6B6B)  // red
    val toColor   = Color(0xFF4A9EFF)  // blue

    val lerpedColor = Color(
        red   = fromColor.red   + (toColor.red   - fromColor.red)   * t,
        green = fromColor.green + (toColor.green - fromColor.green) * t,
        blue  = fromColor.blue  + (toColor.blue  - fromColor.blue)  * t,
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        drawCircle(lerpedColor.copy(alpha = 0.2f), 120f, Offset(cx, cy))
        drawCircle(lerpedColor, 60f, Offset(cx, cy))

        // Show lerp progress bar
        val barWidth = 300f
        val barX = cx - barWidth / 2f
        val barY = cy + 120f
        drawRect(Color(0xFF2A2A4A), Offset(barX, barY),
            androidx.compose.ui.geometry.Size(barWidth, 8f))
        drawRect(lerpedColor, Offset(barX, barY),
            androidx.compose.ui.geometry.Size(barWidth * t, 8f))
        drawCircle(lerpedColor, 10f, Offset(barX + barWidth * t, barY + 4f))
    }
}
```

### Example 2: Smooth camera follow using exponential lerp

```kotlin
@Composable
fun SmoothFollowDemo() {
    // Target moves in a figure-8 path
    val infiniteTransition = rememberInfiniteTransition(label = "target")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "figureEight"
    )

    // Camera position — smoothly lerps toward target
    var cameraPos by remember { mutableStateOf(Offset(200f, 400f)) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Target follows figure-8
        val targetPos = Offset(
            cx + 180f * cos(time),
            cy + 80f * sin(time * 2)
        )

        // Smooth follow: lerp 10% toward target each frame
        cameraPos = Offset(
            lerp(cameraPos.x, targetPos.x, 0.08f),
            lerp(cameraPos.y, targetPos.y, 0.08f)
        )

        // Target (fast)
        drawCircle(Color(0xFFFFD700).copy(alpha = 0.3f), 20f, targetPos)
        drawCircle(Color(0xFFFFD700), 10f, targetPos)

        // Camera follower (slow, smooth)
        drawCircle(Color(0xFF4A9EFF).copy(alpha = 0.3f), 30f, cameraPos)
        drawCircle(Color(0xFF4A9EFF), 14f, cameraPos)

        // Line connecting them
        drawLine(Color(0xFF2A2A4A), cameraPos, targetPos, 1f)

        // Figure-8 path hint
        val path = androidx.compose.ui.graphics.Path()
        for (i in 0..200) {
            val t2 = (i.toFloat() / 200f) * (2 * Math.PI).toFloat()
            val px = cx + 180f * cos(t2)
            val py = cy + 80f * sin(t2 * 2)
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(path, Color(0xFF1A1A3A),
            style = Stroke(1f))
    }
}

fun lerp(start: Float, end: Float, t: Float): Float = start + (end - start) * t
```

### Example 3: Lerp-based loading bar

```kotlin
@Composable
fun LerpLoadingBarDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "load")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    // Visual position lerped: left edge → right edge
    val barStart = 60f
    val barEnd   = 340f
    val currentX = lerp(barStart, barEnd, progress)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(400.dp, 80.dp)) {
            // Track
            drawRoundRect(Color(0xFF2A2A4A),
                Offset(barStart, size.height / 2f - 6f),
                androidx.compose.ui.geometry.Size(barEnd - barStart, 12f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f))

            // Fill (lerped width)
            drawRoundRect(Color(0xFF4A9EFF),
                Offset(barStart, size.height / 2f - 6f),
                androidx.compose.ui.geometry.Size(currentX - barStart, 12f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f))

            // Knob
            drawCircle(Color.White, 10f, Offset(currentX, size.height / 2f))
        }

        Text(
            text = "${(progress * 100).toInt()}%",
            color = Color(0xFF4A9EFF),
            fontSize = 24.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
```

---

## 🧪 Trace

**Smooth follow — 3 frames with speed 0.1**:

```
Initial position = 100, target = 500

Frame 1: pos = lerp(100, 500, 0.1) = 100 + 400 × 0.1 = 140.0
Frame 2: pos = lerp(140, 500, 0.1) = 140 + 360 × 0.1 = 176.0
Frame 3: pos = lerp(176, 500, 0.1) = 176 + 324 × 0.1 = 208.4
Frame 4: pos = lerp(208.4, 500, 0.1) = 208.4 + 291.6 × 0.1 = 237.6

After 10 frames: ~65% of the way there.
After 20 frames: ~87% of the way there.
Never exactly 500 — exponentially decaying gap. Feels natural!
```

---

## 🔗 Next

→ [[02 Bezier Curves]] — *Control points, quadratic vs cubic, and how every "smooth curve" in UI uses Bezier math*
