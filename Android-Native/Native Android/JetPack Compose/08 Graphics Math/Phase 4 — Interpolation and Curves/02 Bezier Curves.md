# 02 — Bezier Curves

> **Core Idea**: A Bezier curve is lerp applied recursively. Quadratic Bezier = lerp of two lerps. Cubic = lerp of three. Control points are magnets that "pull" the curve toward them without the curve actually touching them. Every smooth path in UI — from rounded corners to easing curves — is a Bezier.

---

## 🧠 Concept

### Quadratic Bezier (1 control point)
```
Three points: P0 (start), P1 (control), P2 (end)
Q(t) = lerp(lerp(P0, P1, t), lerp(P1, P2, t), t)
```

### Cubic Bezier (2 control points)
```
Four points: P0 (start), P1 (control 1), P2 (control 2), P3 (end)
B(t) = lerp(quadratic(P0, P1, P2, t), quadratic(P1, P2, P3, t), t)
```

The control points act like gravity — they pull the curve without being on it. This is how `CubicBezierEasing` works in Compose.

---

## 📐 Diagram

```
QUADRATIC BEZIER — lerp of lerps:

  P1 (control)
   *
  / \
 /   \
*─────* 
P0    P2

At t = 0.5:
  A = lerp(P0, P1, 0.5) → midpoint of P0─P1
  B = lerp(P1, P2, 0.5) → midpoint of P1─P2
  Q = lerp(A,  B,  0.5) → midpoint of A─B  ← the curve point

  P1
  * 
  |\ B
  | *─────────
  |A  \
  *────*────── P2
  P0   Q (on the curve)

CUBIC BEZIER — the industry standard:

  P0 ──────────────────────────── P3
     \                         /
      P1 (handle)   (handle) P2
       *               *

  The handles "pull" the curve away from the straight line.
  When handles are equal from each end → smooth symmetric curve.
  When handles differ → asymmetric: fast start, slow end (or vice versa)

EASING AS CUBIC BEZIER:
  ease-in-out = CubicBezier(0.42, 0, 0.58, 1.0)
  ease-in     = CubicBezier(0.42, 0, 1.0,  1.0)
  linear      = CubicBezier(0.0,  0, 1.0,  1.0)

  Y axis = animation progress (0→1)
  X axis = time (0→1)
  The curve describes HOW FAST progress moves over time.
```

---

## 🔢 The Math

```kotlin
import androidx.compose.ui.geometry.Offset

// Quadratic Bezier: P0 = start, P1 = control, P2 = end
fun quadraticBezier(p0: Offset, p1: Offset, p2: Offset, t: Float): Offset {
    val a = lerpOffset(p0, p1, t)  // lerp(start, control, t)
    val b = lerpOffset(p1, p2, t)  // lerp(control, end, t)
    return lerpOffset(a, b, t)     // lerp(a, b, t) = the curve point
}

// Cubic Bezier: P0 = start, P1 = control1, P2 = control2, P3 = end
fun cubicBezier(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val a = quadraticBezier(p0, p1, p2, t)
    val b = quadraticBezier(p1, p2, p3, t)
    return lerpOffset(a, b, t)
}

// Helper
fun lerp(start: Float, end: Float, t: Float): Float = start + (end - start) * t
fun lerpOffset(a: Offset, b: Offset, t: Float): Offset =
    Offset(lerp(a.x, b.x, t), lerp(a.y, b.y, t))

// Easing using Cubic Bezier (time-based, 1D):
// CubicBezierEasing in Compose already implements this:
val myEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)  // ease-in-out
```

---

## 🎨 Compose Code

### Example 1: Animated dot following a Bezier curve

```kotlin
import androidx.compose.ui.graphics.Path

@Composable
fun BezierPathDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "bezier")
    val t by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "t"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        // Define cubic bezier control points
        val p0 = Offset(80f, size.height * 0.7f)    // start
        val p1 = Offset(size.width * 0.3f, 80f)     // control 1
        val p2 = Offset(size.width * 0.7f, size.height - 80f)  // control 2
        val p3 = Offset(size.width - 80f, size.height * 0.3f)  // end

        // Draw the Bezier curve
        val path = Path().apply {
            moveTo(p0.x, p0.y)
            cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
        }
        drawPath(path, Color(0xFF2A2A5A), style = Stroke(width = 3f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round))

        // Draw control point handles
        drawLine(Color(0xFF333355), p0, p1, 1f)
        drawLine(Color(0xFF333355), p3, p2, 1f)

        // Draw control points
        listOf(p0, p3).forEach { drawCircle(Color.White, 8f, it) }
        listOf(p1, p2).forEach { drawCircle(Color(0xFF4ECDC4), 6f, it) }

        // Animated dot on the curve (computed using cubic bezier formula)
        val dotPos = cubicBezier(p0, p1, p2, p3, t)
        drawCircle(Color(0xFF4A9EFF).copy(alpha = 0.3f), 20f, dotPos)
        drawCircle(Color(0xFFFF6B6B), 10f, dotPos)
    }
}

fun quadraticBezier(p0: Offset, p1: Offset, p2: Offset, t: Float): Offset {
    val a = Offset(p0.x + (p1.x - p0.x) * t, p0.y + (p1.y - p0.y) * t)
    val b = Offset(p1.x + (p2.x - p1.x) * t, p1.y + (p2.y - p1.y) * t)
    return Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
}

fun cubicBezier(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val a = quadraticBezier(p0, p1, p2, t)
    val b = quadraticBezier(p1, p2, p3, t)
    return Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
}
```

### Example 2: Custom easing using CubicBezierEasing

```kotlin
@Composable
fun CustomEasingDemo() {
    // Custom easing curves
    val bounceEasing = CubicBezierEasing(0.68f, -0.55f, 0.265f, 1.55f)  // elastic-ish
    val sharpEasing  = CubicBezierEasing(0.9f, 0f, 0.1f, 1f)             // sharp in/out
    val smoothEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)           // classic ease

    val transition = rememberInfiniteTransition(label = "easings")

    val bounceVal by transition.animateFloat(0f, 300f,
        infiniteRepeatable(tween(2000, easing = bounceEasing), RepeatMode.Reverse), label = "b")
    val sharpVal by transition.animateFloat(0f, 300f,
        infiniteRepeatable(tween(2000, easing = sharpEasing), RepeatMode.Reverse), label = "s")
    val smoothVal by transition.animateFloat(0f, 300f,
        infiniteRepeatable(tween(2000, easing = smoothEasing), RepeatMode.Reverse), label = "sm")

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val startX = 60f

        // Three balls showing different easings
        drawCircle(Color(0xFFFF6B6B), 16f, Offset(startX + bounceVal, size.height * 0.3f))
        drawCircle(Color(0xFF4ECDC4), 16f, Offset(startX + sharpVal,  size.height * 0.5f))
        drawCircle(Color(0xFF4A9EFF), 16f, Offset(startX + smoothVal, size.height * 0.7f))

        // Track lines
        for (y in listOf(size.height * 0.3f, size.height * 0.5f, size.height * 0.7f)) {
            drawLine(Color(0xFF1A1A3A), Offset(startX, y), Offset(startX + 300f, y), 1f)
        }
    }
}
```

### Example 3: Drawing a smooth path through waypoints using Bezier

```kotlin
@Composable
fun WaypointPathDemo() {
    val waypoints = listOf(
        Offset(60f, 400f),
        Offset(150f, 150f),
        Offset(280f, 350f),
        Offset(380f, 100f),
        Offset(500f, 300f),
        Offset(600f, 180f),
    )

    val infiniteTransition = rememberInfiniteTransition(label = "waypath")
    val t by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (waypoints.size - 1).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        // Draw smooth path using quadratic bezier between each pair
        val path = Path()
        path.moveTo(waypoints[0].x, waypoints[0].y)

        for (i in 0 until waypoints.size - 1) {
            val current = waypoints[i]
            val next = waypoints[i + 1]
            val mid = Offset((current.x + next.x) / 2f, (current.y + next.y) / 2f)

            if (i == 0) path.lineTo(mid.x, mid.y)
            else path.quadraticBezierTo(current.x, current.y, mid.x, mid.y)
        }
        path.lineTo(waypoints.last().x, waypoints.last().y)

        drawPath(path, Color(0xFF2A2A5A), style = Stroke(2f))

        // Waypoint dots
        waypoints.forEach { drawCircle(Color(0xFF4ECDC4), 6f, it) }

        // Animated dot traveling path
        val segment = t.toInt().coerceIn(0, waypoints.size - 2)
        val segT = t - segment
        val pos = quadraticBezier(
            waypoints[segment],
            Offset((waypoints[segment].x + waypoints[segment+1].x) / 2f,
                   (waypoints[segment].y + waypoints[segment+1].y) / 2f),
            waypoints[(segment + 1).coerceAtMost(waypoints.size - 1)],
            segT
        )
        drawCircle(Color(0xFFFF6B6B), 12f, pos)
    }
}
```

---

## 🧪 Trace

**Quadratic Bezier at t=0.5: P0=(0,0), P1=(200, 300), P2=(400, 0)**:

```
Step 1: A = lerp(P0, P1, 0.5) = ((0+200)/2, (0+300)/2) = (100, 150)
Step 2: B = lerp(P1, P2, 0.5) = ((200+400)/2, (300+0)/2) = (300, 150)
Step 3: Q = lerp(A, B, 0.5)   = ((100+300)/2, (150+150)/2) = (200, 150)

Midpoint of this bezier = (200, 150) — notice it's BELOW the straight midpoint
of P0–P2 which would be (200, 0). The control point pulled it up to 150.
```

---

## 🔗 Next

→ [[03 Easing Functions as Math]] — *How easing functions are Bezier curves applied to time — and how to build your own from scratch*
