# 02 — Rotation

> **Core Idea**: Rotation is just the unit circle applied to every point. To rotate a point around a pivot, translate the pivot to the origin, rotate using cos/sin, then translate back. Canvas `rotate()` handles this automatically — but knowing the math lets you rotate around ANY point.

---

## 🧠 Concept

`Modifier.rotate(45f)` rotates a composable 45° around its **center**. Easy.

But what if you need to rotate a gear tooth around the gear center? Or a planet around the sun? Or a hand around a specific pivot?

You need **rotation around an arbitrary point**. The formula:

```
x' = cx + (x - cx) × cos(θ) - (y - cy) × sin(θ)
y' = cy + (x - cx) × sin(θ) + (y - cy) × cos(θ)
```

Where `(cx, cy)` is the pivot point.

In Canvas, `rotate(degrees, pivot)` does this exactly — but you need to understand it to control it.

---

## 📐 Diagram

```
ROTATION AROUND ORIGIN (0,0):

  BEFORE rotation                AFTER rotating 45°
  P = (4, 0)                     P' = (2.83, 2.83)

  y ▲                            y ▲
    │   P                          │     P'
  0 ├───────────▶ x              0 ├──────────▶ x
    0   4                           0  2.83

  x' = 4 × cos(45°) - 0 × sin(45°) = 4 × 0.707 = 2.83
  y' = 4 × sin(45°) + 0 × cos(45°) = 4 × 0.707 = 2.83

ROTATION AROUND ARBITRARY PIVOT (cx, cy):

  P = (300, 100)   pivot = (200, 200)   rotate 90°

  1. Translate to origin:
     px = 300 - 200 = 100
     py = 100 - 200 = -100

  2. Rotate 90° (cos=0, sin=1):
     x' = 100 × 0 - (-100) × 1 = 100
     y' = 100 × 1 + (-100) × 0 = 100

  3. Translate back:
     finalX = 200 + 100 = 300
     finalY = 200 + 100 = 300
     P' = (300, 300)

CANVAS rotate() PATTERN:
  ┌────────────────────────────────────┐
  │ rotate(degrees, pivot) {           │
  │   // canvas is now pre-rotated     │
  │   // draw as if at 0°             │
  │   drawRect(...)                    │
  │ }                                  │
  └────────────────────────────────────┘
```

---

## 🔢 The Math

```kotlin
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.geometry.Offset

// Rotate a point around a pivot
fun rotatePoint(
    point: Offset,
    pivot: Offset,
    angleRad: Float
): Offset {
    val dx = point.x - pivot.x
    val dy = point.y - pivot.y
    val cosA = cos(angleRad)
    val sinA = sin(angleRad)
    return Offset(
        x = pivot.x + dx * cosA - dy * sinA,
        y = pivot.y + dx * sinA + dy * cosA
    )
}

// Usage:
val point = Offset(300f, 100f)
val pivot = Offset(200f, 200f)
val rotated = rotatePoint(point, pivot, (PI / 4).toFloat())  // 45°
```

---

## 🎨 Compose Code

### Example 1: Spinning gear with teeth

```kotlin
import androidx.compose.ui.graphics.Path

@Composable
fun SpinningGearDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "gear")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val innerR = 70f
        val outerR = 100f
        val toothCount = 12

        // Rotate the entire gear using Canvas rotate()
        rotate(degrees = rotation, pivot = Offset(cx, cy)) {
            // Draw gear body
            drawCircle(Color(0xFF3A3A5A), innerR * 0.6f, Offset(cx, cy))
            drawCircle(Color(0xFF2A2A4A), innerR * 0.6f, Offset(cx, cy),
                style = Stroke(3f))

            // Draw teeth using rotatePoint
            val angleRad = rotation * (Math.PI / 180).toFloat()
            for (i in 0 until toothCount) {
                val toothAngle = (i.toFloat() / toothCount) * (2 * Math.PI).toFloat()

                // Tooth is a rectangle extending radially outward
                val innerPoint = Offset(
                    cx + innerR * cos(toothAngle),
                    cy + innerR * sin(toothAngle)
                )
                val outerPoint = Offset(
                    cx + outerR * cos(toothAngle),
                    cy + outerR * sin(toothAngle)
                )

                // Perpendicular offset for tooth width
                val perpAngle = toothAngle + (PI / 2).toFloat()
                val halfWidth = 8f
                val p1 = innerPoint + Offset(halfWidth * cos(perpAngle), halfWidth * sin(perpAngle))
                val p2 = innerPoint - Offset(halfWidth * cos(perpAngle), halfWidth * sin(perpAngle))
                val p3 = outerPoint - Offset(halfWidth * cos(perpAngle), halfWidth * sin(perpAngle))
                val p4 = outerPoint + Offset(halfWidth * cos(perpAngle), halfWidth * sin(perpAngle))

                val toothPath = Path().apply {
                    moveTo(p1.x, p1.y); lineTo(p2.x, p2.y)
                    lineTo(p3.x, p3.y); lineTo(p4.x, p4.y); close()
                }
                drawPath(toothPath, Color(0xFF4A9EFF))
            }

            // Gear ring
            drawCircle(Color(0xFF4A9EFF), innerR, Offset(cx, cy), style = Stroke(4f))
            // Center hub
            drawCircle(Color(0xFF1A1A3A), 15f, Offset(cx, cy))
            drawCircle(Color(0xFF4A9EFF), 15f, Offset(cx, cy), style = Stroke(3f))
        }
    }
}
```

### Example 2: Multiple gears meshing (opposite rotation)

```kotlin
@Composable
fun MeshingGearsDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "gears")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "gearRot"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        // Big gear: left center, 12 teeth
        val bigCenter = Offset(size.width * 0.38f, size.height / 2f)
        val bigR = 90f
        val bigTeeth = 12

        // Small gear: right of big gear, 6 teeth (half, rotates 2× faster, opposite)
        val smallCenter = Offset(size.width * 0.38f + bigR + 50f, size.height / 2f)
        val smallR = 50f
        val smallTeeth = 6

        fun drawGear(center: Offset, innerR: Float, toothCount: Int, deg: Float, color: Color) {
            rotate(degrees = deg, pivot = center) {
                drawCircle(color.copy(alpha = 0.3f), innerR * 0.6f, center)
                drawCircle(color, innerR, center, style = Stroke(4f))

                for (i in 0 until toothCount) {
                    val a = (i.toFloat() / toothCount) * (2 * Math.PI).toFloat()
                    val perpA = a + (PI / 2).toFloat()
                    val hw = if (innerR > 70f) 9f else 6f
                    val outerR = innerR + innerR * 0.3f

                    val ip = Offset(center.x + innerR * cos(a), center.y + innerR * sin(a))
                    val op = Offset(center.x + outerR * cos(a), center.y + outerR * sin(a))
                    val p1 = ip + Offset(hw * cos(perpA), hw * sin(perpA))
                    val p2 = ip - Offset(hw * cos(perpA), hw * sin(perpA))
                    val p3 = op - Offset(hw * cos(perpA), hw * sin(perpA))
                    val p4 = op + Offset(hw * cos(perpA), hw * sin(perpA))

                    drawPath(Path().apply {
                        moveTo(p1.x, p1.y); lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y); lineTo(p4.x, p4.y); close()
                    }, color)
                }
                drawCircle(Color(0xFF0D0D1A), 12f, center)
                drawCircle(color, 12f, center, style = Stroke(3f))
            }
        }

        // Big gear rotates clockwise (+)
        drawGear(bigCenter, bigR, bigTeeth, rotation, Color(0xFF4A9EFF))

        // Small gear rotates counter-clockwise (−) at 2× speed
        val smallRotation = -rotation * (bigTeeth.toFloat() / smallTeeth.toFloat())
        drawGear(smallCenter, smallR, smallTeeth, smallRotation, Color(0xFFFF6B6B))
    }
}
```

### Example 3: Rotate composable around a custom pivot with Modifier

```kotlin
@Composable
fun CustomPivotRotationDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "pivot")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "pivotAngle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),
        contentAlignment = Alignment.Center
    ) {
        // Pivot indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(Color.White, shape = androidx.compose.foundation.shape.CircleShape)
        )

        // Rotating arm — pivot is at its LEFT edge
        Box(
            modifier = Modifier
                .offset(x = 60.dp, y = 0.dp)  // shift right so pivot is at left
                .graphicsLayer {
                    rotationZ = angle
                    transformOrigin = TransformOrigin(0f, 0.5f)  // left-center pivot
                }
                .width(120.dp)
                .height(4.dp)
                .background(Color(0xFF4A9EFF))
        )

        // Ball at the tip
        Box(
            modifier = Modifier
                .offset(x = 120.dp, y = 0.dp)
                .graphicsLayer {
                    rotationZ = angle
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
                .size(16.dp)
                .background(Color(0xFFFF6B6B), shape = androidx.compose.foundation.shape.CircleShape)
        )
    }
}
```

---

## 🧪 Trace

**Rotate point P=(400, 200) by 90° around pivot=(300, 300)**:

```
Step 1: Translate P to local (pivot = origin)
  px = 400 - 300 = 100
  py = 200 - 300 = -100

Step 2: Apply rotation 90° (cos(90°)=0, sin(90°)=1)
  x' = 100 × cos(90°) - (-100) × sin(90°) = 0 - (-100) = 100
  y' = 100 × sin(90°) + (-100) × cos(90°) = 100 + 0     = 100

Step 3: Translate back
  finalX = 300 + 100 = 400
  finalY = 300 + 100 = 400
  P' = (400, 400)
```

Check: P was 141px from pivot (northeast). After 90°, P' is southeast of pivot — at (400, 400). Distance = sqrt((400-300)² + (400-300)²) = sqrt(20000) ≈ 141px. ✓

---

## 🔗 Next

→ [[03 Scale]] — *Uniform and non-uniform scaling, zoom effects, and how scale interacts with rotation*
