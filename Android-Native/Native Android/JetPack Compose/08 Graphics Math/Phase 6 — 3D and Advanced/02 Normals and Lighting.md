# 02 — Normals and Lighting

> **Core Idea**: A surface normal is a unit vector perpendicular to a surface. The dot product between the normal and the light direction tells you how bright the surface should be. This is how all 3D shading works — from flat shading to Phong.

---

## 🧠 Concept

Hold a piece of paper flat. Shine a torch directly at it. Maximum brightness.
Tilt the paper 90° — the torch now grazes across the surface. Near zero brightness.

**Normal**: The vector pointing straight out from the paper's surface.
**Light direction**: Vector pointing toward the light source (normalized).
**Brightness**: `dot(normal, lightDir)` — 1 = fully lit, 0 = edge-on, negative = back-facing.

```
brightness = max(0, dot(surfaceNormal, lightDirection))
```

This is **Lambert's diffuse lighting** — the basis of all real-time lighting.

---

## 📐 Diagram

```
SURFACE NORMAL:

  Surface ─────────────────────────
                │ ← normal (perpendicular)
                │
                N⃗

  Light at 0° (head on):     Light at 45°:      Light at 90°:
         L⃗                      L⃗                    L⃗ →
         │                       ╲                   ─────
  ───────┼─── surface        ─────╲────        ─────────────
         N⃗ = L⃗               N⃗  45° L⃗             N⃗ ⊥ L⃗
  dot = 1.0 (max bright)   dot = 0.707          dot = 0 (dark)

CUBE FACE NORMALS (before rotation):
  Front:  (0,  0, -1)    Back:  (0,  0,  1)
  Left:   (-1, 0,  0)    Right: (1,  0,  0)
  Top:    (0,  1,  0)    Bottom:(0, -1,  0)

FLAT SHADING PROCESS:
  1. Define face normal in object space
  2. Rotate normal by same matrix as geometry
  3. dot(rotated_normal, light_dir) = brightness
  4. brightness × face_color = shaded color
```

---

## 🔢 The Math

```kotlin
data class Vec3f(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3f)  = Vec3f(x+o.x, y+o.y, z+o.z)
    operator fun minus(o: Vec3f) = Vec3f(x-o.x, y-o.y, z-o.z)
    operator fun times(s: Float) = Vec3f(x*s, y*s, z*s)

    fun dot(o: Vec3f) = x*o.x + y*o.y + z*o.z
    fun magnitude() = sqrt(x*x + y*y + z*z)
    fun normalize(): Vec3f {
        val m = magnitude()
        return if (m < 0.0001f) Vec3f(0f,0f,1f) else Vec3f(x/m, y/m, z/m)
    }

    // Cross product (gives normal of a triangle/face)
    fun cross(o: Vec3f) = Vec3f(
        y*o.z - z*o.y,
        z*o.x - x*o.z,
        x*o.y - y*o.x
    )
}

// Lambert diffuse: how bright is a face given its normal and light direction?
fun lambertBrightness(normal: Vec3f, lightDir: Vec3f): Float =
    maxOf(0f, normal.normalize().dot(lightDir.normalize()))

// Compute face normal from 3 vertices (cross product of two edges)
fun faceNormal(v0: Vec3f, v1: Vec3f, v2: Vec3f): Vec3f =
    (v1 - v0).cross(v2 - v0).normalize()

// Apply brightness to a Color
fun Color.withBrightness(b: Float): Color = Color(
    red   = (red   * b).coerceIn(0f, 1f),
    green = (green * b).coerceIn(0f, 1f),
    blue  = (blue  * b).coerceIn(0f, 1f),
    alpha = alpha
)
```

---

## 🎨 Compose Code

### Example 1: Flat-shaded rotating cube with lighting

```kotlin
// Face normals in object space (before rotation)
val cubeNormals = listOf(
    Vec3f( 0f, -1f,  0f),  // bottom
    Vec3f( 0f,  1f,  0f),  // top
    Vec3f( 0f,  0f, -1f),  // front
    Vec3f( 0f,  0f,  1f),  // back
    Vec3f( 1f,  0f,  0f),  // right
    Vec3f(-1f,  0f,  0f),  // left
)

val baseFaceColors = listOf(
    Color(0xFF4A9EFF), Color(0xFF4ECDC4), Color(0xFFFF6B6B),
    Color(0xFFFFD700), Color(0xFFFF9F43), Color(0xFFAA8EC8)
)

@Composable
fun ShadedCubeDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "shaded")
    val rx by infiniteTransition.animateFloat(0f, (2*PI).toFloat(),
        infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "srx")
    val ry by infiniteTransition.animateFloat(0f, (2*PI).toFloat(),
        infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "sry")

    // Light direction — fixed in world space (upper-right, toward camera)
    val lightDir = Vec3f(0.5f, 1f, -1f).normalize()

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030308))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = minOf(size.width, size.height) * 0.22f

        // Rotate all vertices
        val rotated = cubeVertices.map { v ->
            rotateVecY(rotateVecX(Vec3f(v.x, v.y, v.z), rx), ry)
        }

        // Rotate normals by same rotation
        val rotatedNormals = cubeNormals.map { n ->
            rotateVecY(rotateVecX(n, rx), ry)
        }

        // Project vertices
        val projected = rotated.map { perspectiveProject(it, cx, cy, scale) }

        // Collect faces with depth and shading
        data class FaceData(
            val faceIdx: Int, val verts: IntArray,
            val avgZ: Float, val brightness: Float
        )

        val faceDataList = cubeFaces.mapIndexed { i, verts ->
            val avgZ = verts.sumOf { rotated[it].z.toDouble() }.toFloat() / verts.size
            val brightness = lambertBrightness(rotatedNormals[i], lightDir)
            FaceData(i, verts, avgZ, brightness)
        }.sortedByDescending { it.avgZ }  // painter's algorithm

        // Draw each face with shading
        faceDataList.forEach { fd ->
            // Skip back-facing faces (brightness ≈ 0)
            if (fd.brightness < 0.01f) return@forEach

            val pts = fd.verts.map { projected[it] }
            val facePath = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                pts.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }

            // Ambient + diffuse lighting
            val ambient = 0.15f
            val totalBrightness = ambient + fd.brightness * (1f - ambient)

            drawPath(facePath, baseFaceColors[fd.faceIdx].withBrightness(totalBrightness))
            drawPath(facePath, Color.White.copy(alpha = 0.1f), style = Stroke(1f))
        }
    }
}
```

### Example 2: Sphere lighting approximation with dot product

```kotlin
@Composable
fun SphereShadeDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "sphere")
    val lightAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "lightAng"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val sphereR = 140f

        // Rotating light direction
        val lightDir = Vec3f(cos(lightAngle), 0.5f, sin(lightAngle)).normalize()

        // Draw sphere as grid of small circles — fake sphere shading
        val gridSize = 18
        for (latI in 0 until gridSize) {
            for (lonI in 0 until gridSize * 2) {
                val lat = ((latI.toFloat() / gridSize) - 0.5f) * PI.toFloat()       // -π/2 to π/2
                val lon = (lonI.toFloat() / (gridSize * 2)) * (2 * PI).toFloat()    // 0 to 2π

                // Sphere point in 3D
                val nx = cos(lat) * cos(lon)
                val ny = sin(lat)
                val nz = cos(lat) * sin(lon)
                val normal = Vec3f(nx, ny, nz)

                // Project to 2D (simple orthographic for sphere)
                val screenX = cx + nx * sphereR
                val screenY = cy - ny * sphereR

                // Only draw front hemisphere
                if (nz > 0) continue

                // Brightness
                val brightness = maxOf(0f, normal.dot(lightDir))
                val ambient = 0.1f
                val total = ambient + brightness * 0.9f

                drawCircle(
                    color = Color(0xFF4A9EFF).withBrightness(total),
                    radius = 8f,
                    center = Offset(screenX, screenY)
                )
            }
        }

        // Light indicator
        val lightX = cx + 200f * cos(lightAngle)
        val lightY = cy - 200f * 0.5f
        drawCircle(Color(0xFFFFFFAA), 12f, Offset(lightX, lightY))
        drawLine(Color(0xFF333333), Offset(cx, cy), Offset(lightX, lightY), 1f)
    }
}
```

---

## 🧪 Trace

**Front face normal = (0, 0, -1). Light from upper right-front = (0.5, 1, -1) normalized.**

```
Normalize light (0.5, 1, -1):
  magnitude = sqrt(0.25 + 1 + 1) = sqrt(2.25) = 1.5
  lightDir = (0.5/1.5, 1/1.5, -1/1.5) = (0.333, 0.667, -0.667)

dot = (0)(0.333) + (0)(0.667) + (-1)(-0.667) = 0 + 0 + 0.667 = 0.667

brightness = 0.667  → front face is 66.7% bright ✓
(makes sense — light is angled, not head-on)
```

---

## 🔗 Next

→ [[03 Geodesic Polyhedron]] — *Subdivision of an icosahedron into a sphere-like mesh — the most complex topic in this vault*
