# 01 — 3D Projection Math

> **Core Idea**: Projection is the mathematics of flattening 3D space onto a 2D surface — exactly what your eye and a camera do. Two models: Orthographic (no depth illusion) and Perspective (near = big, far = small).

---

## 🧠 Concept

The eye sees perspective. Parallel train tracks appear to converge at the horizon. That convergence is perspective projection.

**Orthographic**: All parallel lines remain parallel. No depth illusion. Used in technical drawings, isometric games, CAD.

**Perspective**: Points closer to camera appear larger. Controlled by **Field of View (FOV)** — how wide the camera angle is.

---

## 📐 Diagram

```
ORTHOGRAPHIC PROJECTION:
  3D                         2D (top view)
  ─────────────────          ─────────────
  A ────────────────────────────────────── A'
  B ────────────────────────────────────── B'
  C ────────────────────────────────────── C'
  (all lines parallel, no depth effect)

PERSPECTIVE PROJECTION — frustum:

  Camera
    ╱─────────────────────────────────────
   ╱   near plane                far plane
  ╱           ┌──────────────────────┐
 * ──────────→│  Viewing frustum     │──→
  ╲           └──────────────────────┘
   ╲
    ╲─────────────────────────────────────

  FORMULA (simplified pinhole camera):
    Given 3D point (x, y, z):
    fov_factor = focal_length / (focal_length + z)
    screen_x = center_x + x × fov_factor
    screen_y = center_y - y × fov_factor

  FOV EFFECT:
    small focal_length (wide angle):  objects distort near edges
    large focal_length (telephoto):   flatter, less perspective

ISOMETRIC PROJECTION (special orthographic):
  Rotate 45° around Y, then 35.26° around X
  Result: cube appears equal on all 3 visible sides
  Used in games like Monument Valley, Clash of Clans
  
  3D: x=1, y=1, z=0
  iso_x = (x - z) × cos(30°)
  iso_y = (x + z) × sin(30°) - y
```

---

## 🔢 The Math

```kotlin
data class Vec3f(val x: Float, val y: Float, val z: Float)

// Perspective projection
fun perspectiveProject(
    v: Vec3f,
    centerX: Float,
    centerY: Float,
    scale: Float,
    focalLength: Float = 5f  // larger = less perspective distortion
): Offset {
    val factor = focalLength / (focalLength + v.z)
    return Offset(
        centerX + v.x * scale * factor,
        centerY - v.y * scale * factor  // flip Y
    )
}

// Orthographic projection
fun orthoProject(v: Vec3f, centerX: Float, centerY: Float, scale: Float): Offset =
    Offset(centerX + v.x * scale, centerY - v.y * scale)

// Isometric projection
fun isoProject(v: Vec3f, centerX: Float, centerY: Float, scale: Float): Offset {
    val isoX = (v.x - v.z) * cos((PI / 6).toFloat())  // 30°
    val isoY = (v.x + v.z) * sin((PI / 6).toFloat()) - v.y
    return Offset(centerX + isoX * scale, centerY + isoY * scale)
}
```

---

## 🎨 Compose Code

### Example 1: Three projection modes compared

```kotlin
@Composable
fun ProjectionModesDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "modes")
    val ry by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "ryModes"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510))
    ) {
        val scale = 45f
        val rotated = cubeVertices.map { rotateVecY(it, ry) }

        val positions = listOf(
            Triple("Ortho", size.width * 0.17f, size.height / 2f),
            Triple("Persp", size.width * 0.50f, size.height / 2f),
            Triple("Iso",   size.width * 0.83f, size.height / 2f),
        )

        positions.forEach { (mode, cx, cy) ->
            val pts = rotated.map { v ->
                when (mode) {
                    "Ortho" -> orthoProject(v, cx, cy, scale)
                    "Persp" -> perspectiveProject(v, cx, cy, scale, 4f)
                    else    -> isoProject(v, cx, cy, scale)
                }
            }

            val color = when (mode) {
                "Ortho" -> Color(0xFF4A9EFF)
                "Persp" -> Color(0xFFFF6B6B)
                else    -> Color(0xFF4ECDC4)
            }

            cubeEdges.forEach { (a, b) ->
                drawLine(color, pts[a], pts[b], 2f)
            }
        }

        // Dividers
        listOf(size.width * 0.33f, size.width * 0.66f).forEach { x ->
            drawLine(Color(0xFF1A1A3A), Offset(x, 0f), Offset(x, size.height), 1f)
        }
    }
}
```

### Example 2: Perspective depth — objects at different Z distances

```kotlin
@Composable
fun PerspectiveDepthDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "depth")
    val z by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "zDepth"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = 80f
        val fov = 5f

        // Several objects at fixed Z positions
        listOf(0f, 2f, 4f, 6f, 8f).forEachIndexed { i, zPos ->
            val factor = fov / (fov + zPos)
            val radius = 40f * factor
            val alpha = 1f - zPos / 10f
            drawCircle(
                color = Color(0xFF4A9EFF).copy(alpha = alpha.coerceAtLeast(0f)),
                radius = radius,
                center = Offset(cx + (i - 2) * 80f * factor, cy)
            )
        }

        // Moving sphere
        val movingFactor = fov / (fov + z.coerceAtLeast(-fov + 0.1f))
        drawCircle(
            color = Color(0xFFFF6B6B),
            radius = (40f * movingFactor).coerceAtLeast(2f),
            center = Offset(cx, cy - 80f)
        )
    }
}
```

---

## 🧪 Trace

**Perspective project point (2, 1, 3), focalLength=5, scale=100, center=(300, 300)**:

```
factor = 5 / (5 + 3) = 5/8 = 0.625

sx = 300 + 2 × 100 × 0.625 = 300 + 125 = 425
sy = 300 - 1 × 100 × 0.625 = 300 - 62.5 = 237.5

Screen: (425, 238)

Same point at z=0 (no depth):
factor = 5/5 = 1.0
sx = 300 + 200 = 500, sy = 300 - 100 = 200

Objects get closer together AND smaller as Z increases ✓
```

---

## 🔗 Next

→ [[02 Normals and Lighting]] — *Surface normals, dot product lighting, and how flat-shaded 3D works*
