# 02 — Rotation Matrix

> **Core Idea**: A rotation matrix transforms every point of an object by the same angle, simultaneously, with a single matrix multiply. Extend it to 3D and you can rotate around any of the three axes — which is how every 3D engine works.

---

## 🧠 Concept

The 2D rotation matrix is derived directly from the unit circle:

```
Rotate point (x, y) by angle θ:
  x' = x × cos(θ) - y × sin(θ)
  y' = x × sin(θ) + y × cos(θ)
```

Written as a matrix-vector product:
```
│ cos(θ)  -sin(θ) │   │ x │   │ x' │
│ sin(θ)   cos(θ) │ × │ y │ = │ y' │
```

For 3D, there is a separate rotation matrix for each axis (X, Y, Z), and you combine them.

---

## 📐 Diagram

```
2D ROTATION MATRIX DERIVATION:

  Unit circle: point at angle θ = (cos θ, sin θ)
  Rotating vector (1, 0) by θ gives (cos θ, sin θ)  → first column
  Rotating vector (0, 1) by θ gives (-sin θ, cos θ) → second column

  R(θ) = [ cos θ  -sin θ ]
          [ sin θ   cos θ ]

3D ROTATION MATRICES:

  Around Z axis (2D rotation):         Around X axis:
  │ cos θ  -sin θ  0 │                │ 1    0      0  │
  │ sin θ   cos θ  0 │                │ 0   cos θ -sin θ │
  │  0       0     1 │                │ 0   sin θ  cos θ │

  Around Y axis:
  │  cos θ   0  sin θ │
  │   0      1    0   │
  │ -sin θ   0  cos θ │

ROTATION ORDER MATTERS (non-commutative):
  R_x × R_y ≠ R_y × R_x

  Rotating a cube 90° around X then 90° around Y
  gives a different result than Y then X.

  Convention: YXZ, XYZ, ZYX — must be consistent!
  Compose uses: rotationX, rotationY, rotationZ in graphicsLayer (ZYX applied)
```

---

## 🔢 The Math

```kotlin
import kotlin.math.*
import androidx.compose.ui.geometry.Offset

// 2D rotation
fun rotate2D(point: Offset, angleRad: Float, pivot: Offset = Offset.Zero): Offset {
    val dx = point.x - pivot.x
    val dy = point.y - pivot.y
    return Offset(
        pivot.x + dx * cos(angleRad) - dy * sin(angleRad),
        pivot.y + dx * sin(angleRad) + dy * cos(angleRad)
    )
}

// 3D point
data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x+o.x, y+o.y, z+o.z)
}

// 3D rotation matrices
fun rotateX(p: Vec3, a: Float) = Vec3(p.x, p.y*cos(a)-p.z*sin(a), p.y*sin(a)+p.z*cos(a))
fun rotateY(p: Vec3, a: Float) = Vec3(p.x*cos(a)+p.z*sin(a), p.y, -p.x*sin(a)+p.z*cos(a))
fun rotateZ(p: Vec3, a: Float) = Vec3(p.x*cos(a)-p.y*sin(a), p.x*sin(a)+p.y*cos(a), p.z)

// Apply all three rotations
fun rotateXYZ(p: Vec3, rx: Float, ry: Float, rz: Float): Vec3 =
    rotateZ(rotateY(rotateX(p, rx), ry), rz)
```

---

## 🎨 Compose Code

### Example 1: Rotating star polygon

```kotlin
@Composable
fun RotatingStarDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "star")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "starAngle"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = 120f
        val innerR = 50f
        val points = 5

        // Generate star points, then rotate them using rotation matrix
        val starPoints = (0 until points * 2).map { i ->
            val baseAngle = (i.toFloat() / (points * 2)) * (2 * Math.PI).toFloat()
            val r = if (i % 2 == 0) outerR else innerR
            val rawPoint = Offset(r * cos(baseAngle), r * sin(baseAngle))
            // Apply rotation matrix around origin, then offset to center
            rotate2D(rawPoint, angle) + Offset(cx, cy)
        }

        val starPath = Path().apply {
            moveTo(starPoints[0].x, starPoints[0].y)
            starPoints.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }

        drawPath(starPath, Color(0xFF2A2A5A))
        drawPath(starPath, Color(0xFFFFD700), style = Stroke(3f))
        drawCircle(Color.White, 6f, Offset(cx, cy))
    }
}
```

### Example 2: 3D axes rotating in 2D projection

```kotlin
@Composable
fun AxesRotationDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "axes")
    val rx by infiniteTransition.animateFloat(0f, (2*PI).toFloat(),
        infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "rx")
    val ry by infiniteTransition.animateFloat(0f, (2*PI).toFloat(),
        infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "ry")
    val rz by infiniteTransition.animateFloat(0f, (2*PI).toFloat(),
        infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "rz")

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        fun project(v: Vec3): Offset {
            val rotated = rotateXYZ(v, rx, ry, rz)
            return Offset(cx + rotated.x, cy - rotated.y)  // flip Y for screen
        }

        val origin = Vec3(0f, 0f, 0f)
        val xAxis  = Vec3(120f, 0f, 0f)
        val yAxis  = Vec3(0f, 120f, 0f)
        val zAxis  = Vec3(0f, 0f, 120f)

        val o = project(origin)
        drawArrow(o, project(xAxis), Color(0xFFFF4444), arrowSize = 15f)  // X = red
        drawArrow(o, project(yAxis), Color(0xFF44FF44), arrowSize = 15f)  // Y = green
        drawArrow(o, project(zAxis), Color(0xFF4444FF), arrowSize = 15f)  // Z = blue
        drawCircle(Color.White, 6f, o)
    }
}
```

---

## 🧪 Trace

**Rotate point (3, 0) by 90° using rotation matrix**:

```
cos(90°) = 0, sin(90°) = 1

x' = 3 × 0  - 0 × 1 = 0
y' = 3 × 1  + 0 × 0 = 3

Result: (0, 3) — point moved from 3 o'clock to 12 o'clock ✓
```

---

## 🔗 Next

→ [[03 Composing Transforms]] — *Chain multiple matrices and understand why order changes the result*
