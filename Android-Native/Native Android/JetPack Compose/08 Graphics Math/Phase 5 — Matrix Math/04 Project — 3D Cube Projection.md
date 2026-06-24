# 04 — Project: 3D Cube Projection

> **Phase 5 Capstone**: Rotate a cube's 8 vertices in 3D using rotation matrices, project them onto a 2D screen using perspective math, and draw the 12 edges. Pure math — no OpenGL or 3D engine.

---

## 🧠 What We're Building

- 8 vertices of a unit cube in 3D space
- Rotation around X, Y, Z axes simultaneously
- Orthographic and Perspective projection to 2D
- Edge drawing connecting correct vertex pairs
- Optional: face coloring by Z depth

---

## 📐 Diagram

```
CUBE — 8 vertices labeled:

       7─────────6
      /│        /│
     / │       / │
    4─────────5  │
    │  │      │  │
    │  3──────│──2
    │ /       │ /
    │/        │/
    0─────────1

VERTEX COORDINATES (±1 cube):
  0: (-1, -1, -1)    4: (-1,  1, -1)
  1: ( 1, -1, -1)    5: ( 1,  1, -1)
  2: ( 1, -1,  1)    6: ( 1,  1,  1)
  3: (-1, -1,  1)    7: (-1,  1,  1)

12 EDGES (vertex pairs):
  Bottom: 0-1, 1-2, 2-3, 3-0
  Top:    4-5, 5-6, 6-7, 7-4
  Pillars:0-4, 1-5, 2-6, 3-7

PERSPECTIVE PROJECTION:
  3D point (x, y, z) → 2D screen (sx, sy)

  Orthographic (no depth):
    sx = x, sy = y (just ignore z)

  Perspective (depth scaling):
    fov = field of view factor (e.g. 3.0)
    scale = fov / (fov + z)
    sx = x × scale
    sy = y × scale
    → things farther (larger z) appear smaller ✓
```

---

## 🎨 Compose Code

### Full Spinning 3D Cube

```kotlin
import kotlin.math.*
import androidx.compose.ui.geometry.Offset

// 3D vertex
data class Vec3f(val x: Float, val y: Float, val z: Float)

// Cube vertices (unit cube, ±1)
val cubeVertices = listOf(
    Vec3f(-1f, -1f, -1f), // 0
    Vec3f( 1f, -1f, -1f), // 1
    Vec3f( 1f, -1f,  1f), // 2
    Vec3f(-1f, -1f,  1f), // 3
    Vec3f(-1f,  1f, -1f), // 4
    Vec3f( 1f,  1f, -1f), // 5
    Vec3f( 1f,  1f,  1f), // 6
    Vec3f(-1f,  1f,  1f), // 7
)

// 12 edges (pairs of vertex indices)
val cubeEdges = listOf(
    0 to 1, 1 to 2, 2 to 3, 3 to 0,  // bottom face
    4 to 5, 5 to 6, 6 to 7, 7 to 4,  // top face
    0 to 4, 1 to 5, 2 to 6, 3 to 7   // vertical pillars
)

// 6 faces (groups of 4 vertices for depth sorting)
val cubeFaces = listOf(
    intArrayOf(0, 1, 2, 3),  // bottom
    intArrayOf(4, 5, 6, 7),  // top
    intArrayOf(0, 1, 5, 4),  // front
    intArrayOf(2, 3, 7, 6),  // back
    intArrayOf(1, 2, 6, 5),  // right
    intArrayOf(3, 0, 4, 7),  // left
)

val faceColors = listOf(
    Color(0xFF4A9EFF), Color(0xFF4ECDC4), Color(0xFFFF6B6B),
    Color(0xFFFFD700), Color(0xFFFF9F43), Color(0xFFAA8EC8)
)

// 3D Rotation functions
fun rotateVecX(v: Vec3f, a: Float) = Vec3f(v.x, v.y*cos(a)-v.z*sin(a), v.y*sin(a)+v.z*cos(a))
fun rotateVecY(v: Vec3f, a: Float) = Vec3f(v.x*cos(a)+v.z*sin(a), v.y, -v.x*sin(a)+v.z*cos(a))
fun rotateVecZ(v: Vec3f, a: Float) = Vec3f(v.x*cos(a)-v.y*sin(a), v.x*sin(a)+v.y*cos(a), v.z)

// Perspective projection
fun project(v: Vec3f, cx: Float, cy: Float, scale: Float, fov: Float = 4f): Offset {
    val depth = fov / (fov + v.z)
    return Offset(
        cx + v.x * scale * depth,
        cy - v.y * scale * depth  // flip Y for screen coords
    )
}

@Composable
fun SpinningCubeDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "cube")
    val rx by infiniteTransition.animateFloat(0f, (2*PI).toFloat(),
        infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "rx")
    val ry by infiniteTransition.animateFloat(0f, (2*PI).toFloat(),
        infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "ry")
    val rz by infiniteTransition.animateFloat(0f, (2*PI).toFloat() / 4f,
        infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "rz")

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = minOf(size.width, size.height) * 0.25f

        // Transform all vertices
        val rotated = cubeVertices.map { v ->
            rotateVecZ(rotateVecY(rotateVecX(v, rx), ry), rz)
        }

        // Project to 2D
        val projected = rotated.map { project(it, cx, cy, scale) }

        // Draw faces (sorted by average Z depth — painter's algorithm)
        val facesWithDepth = cubeFaces.mapIndexed { i, face ->
            val avgZ = face.sumOf { rotated[it].z.toDouble() }.toFloat() / face.size
            Triple(i, face, avgZ)
        }.sortedByDescending { it.third }  // draw farthest first

        facesWithDepth.forEach { (faceIdx, face, _) ->
            val pts = face.map { projected[it] }
            val facePath = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                pts.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            drawPath(facePath, faceColors[faceIdx].copy(alpha = 0.3f))
            drawPath(facePath, faceColors[faceIdx], style = Stroke(2f))
        }

        // Draw edges (wireframe on top)
        cubeEdges.forEach { (a, b) ->
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = projected[a],
                end = projected[b],
                strokeWidth = 1f
            )
        }

        // Draw vertices
        projected.forEach { p ->
            drawCircle(Color.White, 4f, p)
        }
    }
}
```

### Orthographic vs Perspective — side by side

```kotlin
@Composable
fun ProjectionComparisonDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "proj")
    val ry by infiniteTransition.animateFloat(0f, (2*PI).toFloat(),
        infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "projRy")

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510))
    ) {
        val scale = 60f

        // Rotate all vertices
        val rotated = cubeVertices.map { rotateVecY(it, ry) }

        // LEFT: Orthographic (just ignore Z)
        val leftCx = size.width * 0.25f
        val cy = size.height / 2f
        val orthoPts = rotated.map { Offset(leftCx + it.x * scale, cy - it.y * scale) }

        // RIGHT: Perspective
        val rightCx = size.width * 0.75f
        val perspPts = rotated.map { project(it, rightCx, cy, scale, fov = 4f) }

        // Draw both
        cubeEdges.forEach { (a, b) ->
            drawLine(Color(0xFF4A9EFF), orthoPts[a], orthoPts[b], 2f)
            drawLine(Color(0xFFFF6B6B), perspPts[a], perspPts[b], 2f)
        }

        // Divider + labels
        drawLine(Color(0xFF2A2A4A), Offset(size.width/2f, 0f),
            Offset(size.width/2f, size.height), 1f)
    }
}
```

---

## 🧪 Trace

**Project vertex (1, 1, 1) after rotating 45° around Y, fov=4, scale=120, center=(400,400)**:

```
Step 1: rotateY (1,1,1) by 45° (π/4)
  cos(45°) = 0.707, sin(45°) = 0.707
  x' = 1×0.707 + 1×0.707 = 1.414
  y' = 1 (unchanged)
  z' = -1×0.707 + 1×0.707 = 0

Step 2: Perspective projection (z'=0 → no depth distortion)
  depth = 4 / (4 + 0) = 1.0
  sx = 400 + 1.414 × 120 × 1.0 = 400 + 169.7 = 570
  sy = 400 - 1 × 120 × 1.0 = 400 - 120 = 280

Vertex at screen (570, 280)
```

---

## 📋 Phase 5 Complete

| Concept | You Can Now… |
|---------|-------------|
| Matrix | Represent any transform as a number grid |
| Rotation Matrix | Rotate in 2D and 3D |
| Composing Transforms | Chain matrices for bone/joint hierarchies |
| **3D Cube** | Project 3D geometry to screen with perspective |

---

## 🔗 Next Phase

→ [[Phase 6 — 3D and Advanced/01 3D Projection Math]] — *Perspective vs orthographic in depth, frustum, near/far plane, field of view*
