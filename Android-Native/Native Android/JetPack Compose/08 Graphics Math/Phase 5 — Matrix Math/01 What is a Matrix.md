# 01 — What is a Matrix

> **Core Idea**: A matrix is a grid of numbers that encodes a transformation. Multiplying a point by a matrix applies the transform. Multiplying two matrices chains two transforms. This is how 3D engines, OpenGL, and `graphicsLayer` work internally.

---

## 🧠 Concept

You've been doing transforms manually — `translate(x, y)`, then `rotate(angle)`, then `scale(s)`.

A matrix packages all three into one 3×3 grid (for 2D) or 4×4 grid (for 3D). You multiply a point by the matrix, and instantly get the transformed point.

**Why it matters**: Chain 10 transforms = multiply 10 matrices = ONE matrix. Apply to 10,000 vertices = 10,000 matrix multiplications. This is exactly what the GPU does.

In Compose: `graphicsLayer` uses a `Matrix` internally. `Canvas.setMatrix()` also uses it.

---

## 📐 Diagram

```
2D HOMOGENEOUS COORDINATES (3×3 matrix):

  Point as [x, y, 1] (the "1" enables translation)

  │ m00  m01  tx │   │ x │   │ m00×x + m01×y + tx │
  │ m10  m11  ty │ × │ y │ = │ m10×x + m11×y + ty │
  │  0    0    1 │   │ 1 │   │          1          │

IDENTITY MATRIX (does nothing):
  │ 1  0  0 │
  │ 0  1  0 │
  │ 0  0  1 │
  Apply to (3, 4): (1×3 + 0×4 + 0, 0×3 + 1×4 + 0) = (3, 4) ✓

TRANSLATION MATRIX (move by tx, ty):
  │ 1  0  tx │
  │ 0  1  ty │
  │ 0  0   1 │
  Apply (3,4) with tx=10, ty=20: (3+10, 4+20) = (13, 24) ✓

ROTATION MATRIX (rotate by angle θ):
  │ cos(θ)  -sin(θ)  0 │
  │ sin(θ)   cos(θ)  0 │
  │   0        0     1 │

SCALE MATRIX (scale by sx, sy):
  │ sx  0   0 │
  │  0  sy  0 │
  │  0  0   1 │

CHAINING — TRS order (applied right to left):
  M = Translation × Rotation × Scale
  point' = M × point
```

---

## 🔢 The Math

```kotlin
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

// 3×3 matrix for 2D homogeneous transforms
data class Matrix3(
    val m: FloatArray = floatArrayOf(
        1f, 0f, 0f,
        0f, 1f, 0f,
        0f, 0f, 1f
    )
) {
    operator fun get(row: Int, col: Int): Float = m[row * 3 + col]

    // Matrix × Matrix
    operator fun times(other: Matrix3): Matrix3 {
        val result = FloatArray(9)
        for (row in 0..2) {
            for (col in 0..2) {
                result[row * 3 + col] = (0..2).sumOf { k ->
                    (this[row, k] * other[k, col]).toDouble()
                }.toFloat()
            }
        }
        return Matrix3(result)
    }

    // Matrix × Point (homogeneous)
    fun transform(p: Offset): Offset {
        val x = m[0] * p.x + m[1] * p.y + m[2]
        val y = m[3] * p.x + m[4] * p.y + m[5]
        return Offset(x, y)
    }
}

// Factory functions
fun identityMatrix(): Matrix3 = Matrix3()

fun translationMatrix(tx: Float, ty: Float): Matrix3 = Matrix3(floatArrayOf(
    1f, 0f, tx,
    0f, 1f, ty,
    0f, 0f, 1f
))

fun rotationMatrix(angleRad: Float): Matrix3 {
    val c = cos(angleRad); val s = sin(angleRad)
    return Matrix3(floatArrayOf(
        c, -s, 0f,
        s,  c, 0f,
        0f, 0f, 1f
    ))
}

fun scaleMatrix(sx: Float, sy: Float): Matrix3 = Matrix3(floatArrayOf(
    sx, 0f, 0f,
    0f, sy, 0f,
    0f, 0f, 1f
))

// Chain: TRS
fun trsMatrix(tx: Float, ty: Float, angle: Float, sx: Float = 1f, sy: Float = 1f): Matrix3 =
    translationMatrix(tx, ty) * rotationMatrix(angle) * scaleMatrix(sx, sy)
```

---

## 🎨 Compose Code

### Example 1: Visualize identity vs transformed matrix

```kotlin
@Composable
fun MatrixTransformDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "matrix")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "angle"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // A square defined by 4 points (local coordinates, centered at origin)
        val square = listOf(
            Offset(-50f, -50f),
            Offset( 50f, -50f),
            Offset( 50f,  50f),
            Offset(-50f,  50f),
        )

        // LEFT: identity matrix — no transform
        val idMatrix = identityMatrix()
        val leftCenter = Offset(cx * 0.5f, cy)
        val identityPoints = square.map { p ->
            idMatrix.transform(p) + leftCenter
        }
        drawPolygon(identityPoints, Color(0xFF2A2A5A), Color(0xFF4A9EFF))

        // RIGHT: TRS matrix
        val trsM = trsMatrix(cx * 1.5f, cy, angle, sx = 1.2f, sy = 0.8f)
        val transformedPoints = square.map { p -> trsM.transform(p) }
        drawPolygon(transformedPoints, Color(0xFF2A1A1A), Color(0xFFFF6B6B))
    }
}

// Helper to draw a polygon
fun DrawScope.drawPolygon(points: List<Offset>, fill: Color, stroke: Color) {
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(path, fill)
    drawPath(path, stroke, style = Stroke(2f))
}
```

### Example 2: Matrix chain — orbit using pure matrix multiplication

```kotlin
@Composable
fun MatrixOrbitDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "matOrbit")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "orbitAngle"
    )
    val moonAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "moonAngle"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val origin = Offset.Zero

        // Planet: translate to orbit, then rotate (carries it around)
        val planetMatrix = translationMatrix(cx, cy) *  // move to screen center
                           rotationMatrix(angle) *       // orbit rotation
                           translationMatrix(150f, 0f)   // offset to orbit radius

        val planetPos = planetMatrix.transform(origin)

        // Moon: planet's matrix × moon orbit
        val moonMatrix = planetMatrix * rotationMatrix(moonAngle) * translationMatrix(45f, 0f)
        val moonPos = moonMatrix.transform(origin)

        // Draw
        drawCircle(Color(0xFFFFD700), 25f, Offset(cx, cy))   // sun
        drawCircle(Color(0xFF4A9EFF), 14f, planetPos)         // planet
        drawCircle(Color(0xFFCCCCCC), 6f, moonPos)            // moon
    }
}
```

---

## 🧪 Trace

**Apply TRS matrix to point (1, 0): tx=100, ty=50, angle=π/2, scale=2**:

```
Scale: (1×2, 0×2) = (2, 0)
Rotate 90°:
  x' = 2×cos(90°) - 0×sin(90°) = 0
  y' = 2×sin(90°) + 0×cos(90°) = 2
  After rotate: (0, 2)
Translate: (0+100, 2+50) = (100, 52)

Result: (100, 52)
```

---

## 🔗 Next

→ [[02 Rotation Matrix]] — *Deep dive into 2D and 3D rotation matrices — and why order of multiplication matters*
