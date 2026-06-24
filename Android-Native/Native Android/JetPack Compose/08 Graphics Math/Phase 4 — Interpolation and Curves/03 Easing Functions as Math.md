# 03 — Easing Functions as Math

> **Core Idea**: Easing functions are mathematical curves applied to the time parameter `t`. They transform linear time (0→1) into accelerated, decelerated, bouncy, or elastic time. You can write any easing as a pure math function.

---

## 🧠 Concept

Linear animation feels robotic. Real objects:
- Start slow, build speed (ease-in)
- Start fast, slow down (ease-out)
- Do both (ease-in-out)
- Overshoot and snap back (back/elastic)
- Bounce off the floor (bounce)

All of these are just a function `f(t)` where input is linear time (0→1) and output is the modified progress.

In Compose: `tween(easing = ...)` does this. But you can also apply any easing manually using the math function directly.

---

## 📐 Diagram

```
EASING CURVES — f(t) = progress:

Y (progress)
│ 1.0                    linear: f(t) = t
│              .─────────────  ease-out: fast → slow
│          .───       .───────  ease-in:  slow → fast
│       .─      .──────
│    .─    .─────
│  .─  .─────
│.─.───────────────────────────────▶ X (time t)
0                              1.0

EASE-IN-OUT:
Y
│  1.0        .───────────
│         .──
│       .─                 ← slow middle
│      .
│    .─
│ .──
│.───────────────────────────▶ t
0                         1.0

BOUNCE:
Y
│ 1.0 ────────────────.───.─
│               .─────  .─ .
│           .───       .
│       .─── ←bounces
│   .───
│.──────────────────────────▶ t
0                         1.0

ELASTIC (overshoot):
Y
│  1.3  ─ .
│ 1.0  ─.─.─────────────────
│     .
│   .
│ .─────────────────────────▶ t
│0                         1.0
```

---

## 🔢 The Math — Easing Function Implementations

```kotlin
// These are all pure functions: f(t: Float): Float, where t in 0..1

// ── Linear ──────────────────────────────────────────────────────────────
fun linear(t: Float) = t

// ── Ease In (quadratic) ─────────────────────────────────────────────────
fun easeIn(t: Float) = t * t

// ── Ease Out (quadratic) ────────────────────────────────────────────────
fun easeOut(t: Float) = t * (2 - t)

// ── Ease In Out (cubic) ─────────────────────────────────────────────────
fun easeInOut(t: Float) =
    if (t < 0.5f) 2 * t * t
    else -1 + (4 - 2 * t) * t

// ── Ease In Cubic ───────────────────────────────────────────────────────
fun easeInCubic(t: Float) = t * t * t

// ── Ease Out Cubic ──────────────────────────────────────────────────────
fun easeOutCubic(t: Float) = 1 - (1 - t).pow(3)

// ── Elastic ease out (overshoot and settle) ─────────────────────────────
fun easeOutElastic(t: Float): Float {
    if (t == 0f || t == 1f) return t
    val c4 = (2 * Math.PI / 3).toFloat()
    return (2f).pow(-10 * t) * sin((t * 10 - 0.75f) * c4) + 1
}

// ── Back ease out (overshoot) ───────────────────────────────────────────
fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1
    return 1 + c3 * (t - 1).pow(3) + c1 * (t - 1).pow(2)
}

// ── Bounce ease out ─────────────────────────────────────────────────────
fun easeOutBounce(t: Float): Float {
    val n1 = 7.5625f
    val d1 = 2.75f
    return when {
        t < 1f / d1    -> n1 * t * t
        t < 2f / d1    -> n1 * (t - 1.5f / d1).let { it * it } + 0.75f
        t < 2.5f / d1  -> n1 * (t - 2.25f / d1).let { it * it } + 0.9375f
        else            -> n1 * (t - 2.625f / d1).let { it * it } + 0.984375f
    }
}

// ── Compose integration ─────────────────────────────────────────────────
// Wrap any easing function as a Compose Easing:
val myEasing = Easing { fraction -> easeOutElastic(fraction) }

// Use in tween:
animationSpec = tween(durationMillis = 1000, easing = myEasing)
```

---

## 🎨 Compose Code

### Example 1: Side-by-side comparison of all easing types

```kotlin
@Composable
fun EasingComparisonDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "easing")
    val t by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rawT"
    )

    data class EasingEntry(val name: String, val fn: (Float) -> Float, val color: Color)

    val easings = listOf(
        EasingEntry("Linear",    { it },                            Color(0xFF888888)),
        EasingEntry("EaseIn",    { it * it },                       Color(0xFF4A9EFF)),
        EasingEntry("EaseOut",   { it * (2 - it) },                Color(0xFF4ECDC4)),
        EasingEntry("EaseInOut", { if (it < 0.5f) 2*it*it else -1+(4-2*it)*it }, Color(0xFFFFD700)),
        EasingEntry("Elastic",   ::easeOutElastic,                  Color(0xFFFF6B6B)),
        EasingEntry("Bounce",    ::easeOutBounce,                   Color(0xFFFF9F43)),
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val trackStart = 80f
        val trackEnd = size.width - 80f
        val trackLength = trackEnd - trackStart
        val rowHeight = size.height / (easings.size + 1)

        easings.forEachIndexed { i, entry ->
            val y = rowHeight * (i + 1)
            val easedT = entry.fn(t).coerceIn(-0.5f, 1.5f)  // clamp for elastic/bounce
            val ballX = trackStart + trackLength * easedT

            // Track
            drawLine(Color(0xFF1A1A3A), Offset(trackStart, y), Offset(trackEnd, y), 2f)
            drawCircle(Color(0xFF2A2A3A), 6f, Offset(trackStart, y))
            drawCircle(Color(0xFF2A2A3A), 6f, Offset(trackEnd, y))

            // Ball
            drawCircle(entry.color, 14f, Offset(ballX.coerceIn(trackStart, trackEnd), y))
        }
    }
}
```

### Example 2: Visualize easing curve as a graph

```kotlin
@Composable
fun EasingCurveGraphDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "graph")
    val currentT by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cursor"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val graphLeft = 60f
        val graphTop  = 60f
        val graphSize = minOf(size.width, size.height) * 0.7f

        // Graph border
        drawRect(Color(0xFF2A2A4A),
            Offset(graphLeft, graphTop),
            androidx.compose.ui.geometry.Size(graphSize, graphSize),
            style = Stroke(1f))

        // Draw easeOutElastic curve
        val path = Path()
        val steps = 200
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val y = easeOutElastic(t)
            val px = graphLeft + t * graphSize
            val py = graphTop + graphSize - y * graphSize  // flip Y
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(path, Color(0xFF4A9EFF), style = Stroke(2f))

        // Moving cursor on X axis
        val cursorX = graphLeft + currentT * graphSize
        val easedY = easeOutElastic(currentT)
        val cursorY = graphTop + graphSize - easedY * graphSize

        // Horizontal / vertical guide lines
        drawLine(Color(0xFF2A2A4A), Offset(cursorX, graphTop),
            Offset(cursorX, graphTop + graphSize), 1f)
        drawLine(Color(0xFF2A2A4A), Offset(graphLeft, cursorY),
            Offset(graphLeft + graphSize, cursorY), 1f)

        // Dot on curve
        drawCircle(Color(0xFFFF6B6B), 8f, Offset(cursorX, cursorY))
    }
}
```

### Example 3: Apply custom easing to composable animation

```kotlin
@Composable
fun CustomEasingAnimationDemo() {
    var triggered by remember { mutableStateOf(false) }

    // Custom easing from math function
    val bounceCompose = Easing { t -> easeOutBounce(t) }
    val elasticCompose = Easing { t -> easeOutElastic(t) }

    val offsetY by animateFloatAsState(
        targetValue = if (triggered) 0f else -300f,
        animationSpec = tween(1200, easing = bounceCompose),
        label = "bounceY"
    )

    val scale by animateFloatAsState(
        targetValue = if (triggered) 1f else 0f,
        animationSpec = tween(800, easing = elasticCompose),
        label = "elasticScale"
    )

    LaunchedEffect(Unit) {
        delay(500L)
        triggered = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp, 60.dp)
                .graphicsLayer {
                    translationY = offsetY
                    scaleX = scale
                    scaleY = scale
                }
                .background(Color(0xFF4A9EFF),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Hello!", color = Color.White, fontSize = 20.sp)
        }
    }
}

// Reuse easing functions from 🔢 section above
fun Float.pow(n: Int): Float {
    var result = 1f
    repeat(n) { result *= this }
    return result
}
```

---

## 🧪 Trace

**easeOutBounce at t = 0.3**:

```
n1 = 7.5625, d1 = 2.75
1/d1 = 0.364, 2/d1 = 0.727

t = 0.3 < 0.364 → first case: n1 × t² = 7.5625 × 0.09 = 0.681

Progress = 0.681 (68% of the way)
at 30% of time, object is 68% done → fast start

t = 0.5 (between 0.364 and 0.727):
  n1 × (0.5 - 1.5/2.75)² + 0.75
  = 7.5625 × (0.5 - 0.545)² + 0.75
  = 7.5625 × 0.002 + 0.75 = 0.765

t = 0.7 → just below 2/d1 = 0.727:
  similar → result ≈ 0.92 → mostly done, slight bounce starting
```

---

## 🔗 Next

→ [[04 Project — Elastic Motion]] — *Build spring and elastic animations from pure math functions*
