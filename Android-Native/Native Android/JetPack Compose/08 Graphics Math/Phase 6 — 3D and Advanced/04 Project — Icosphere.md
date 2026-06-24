# 04 — Project: Icosphere

> **Phase 6 Capstone — Final Boss**: Build a fully shaded, rotating geodesic icosphere with Lambert lighting, painter's algorithm face sorting, perspective projection, and a rotating light source. Every line of math used in this file comes from the previous 23 chapters.

---

## 🧠 What We're Building

- Geodesic icosphere (frequency 2 = 80 triangles)
- Full 3D rotation (X and Y axes simultaneously)
- Perspective projection with depth feel
- Face normal calculation using cross product
- Lambert diffuse shading (dot product of normal and light)
- Painter's algorithm — draw farthest faces first
- Back-face culling — skip faces pointing away from camera
- Orbiting point light source
- Optional: smooth pulsing glow and frequency selector

---

## 📐 Diagram

```
COMPLETE PIPELINE:

  ┌─────────────────────────────────────────────────────┐
  │                  Icosphere Pipeline                  │
  │                                                     │
  │  1. GEOMETRY                                        │
  │     Icosahedron vertices (12 points)                │
  │     ↓ subdivide(frequency=2)                        │
  │     Icosphere vertices (42 points)                  │
  │     80 triangular faces                             │
  │                                                     │
  │  2. TRANSFORM                                       │
  │     rotateX(rx) → rotateY(ry) → 3D vertices        │
  │     Rotate face normals by same matrix              │
  │                                                     │
  │  3. CULL                                            │
  │     if face_normal.z > 0 → face is back-facing     │
  │     → skip drawing (back-face culling)             │
  │                                                     │
  │  4. SORT                                            │
  │     Sort faces by average Z (painter's algorithm)  │
  │     Draw farthest faces first                       │
  │                                                     │
  │  5. SHADE                                           │
  │     brightness = dot(face_normal, light_dir)        │
  │     ambient = 0.1                                   │
  │     color = base_color × (ambient + brightness)     │
  │                                                     │
  │  6. PROJECT                                         │
  │     perspective_project(vertex) → screen Offset     │
  │                                                     │
  │  7. DRAW                                            │
  │     drawPath(face_path, shaded_color)               │
  └─────────────────────────────────────────────────────┘

FACE NORMAL DIRECTION:
  If the normal points toward camera (z < 0 in view space)
  → face is front-facing → draw it ✓
  If normal points away (z > 0)
  → face is back-facing → skip ✓
```

---

## 🎨 Compose Code

### Complete Icosphere Composable

```kotlin
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*

// ─── Data ────────────────────────────────────────────────────────────────────

data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3)  = Vec3(x+o.x, y+o.y, z+o.z)
    operator fun minus(o: Vec3) = Vec3(x-o.x, y-o.y, z-o.z)
    operator fun times(s: Float)= Vec3(x*s, y*s, z*s)
    fun dot(o: Vec3)   = x*o.x + y*o.y + z*o.z
    fun cross(o: Vec3) = Vec3(y*o.z-z*o.y, z*o.x-x*o.z, x*o.y-y*o.x)
    fun magnitude()    = sqrt(x*x+y*y+z*z)
    fun normalize()    = magnitude().let { m -> if (m < 1e-6f) this else Vec3(x/m,y/m,z/m) }
    fun midpoint(o: Vec3) = Vec3((x+o.x)/2f,(y+o.y)/2f,(z+o.z)/2f).normalize()
}

data class Tri(val a: Int, val b: Int, val c: Int)

// ─── Icosphere Builder ────────────────────────────────────────────────────────

fun buildIcosphere(frequency: Int, radius: Float): Pair<MutableList<Vec3>, List<Tri>> {
    val φ = (1f + sqrt(5f)) / 2f
    val base = listOf(
        Vec3(-1f,φ,0f),Vec3(1f,φ,0f),Vec3(-1f,-φ,0f),Vec3(1f,-φ,0f),
        Vec3(0f,-1f,φ),Vec3(0f,1f,φ),Vec3(0f,-1f,-φ),Vec3(0f,1f,-φ),
        Vec3(φ,0f,-1f),Vec3(φ,0f,1f),Vec3(-φ,0f,-1f),Vec3(-φ,0f,1f)
    )
    val verts = base.map { it.normalize() * radius }.toMutableList()
    var faces: List<Tri> = listOf(
        Tri(0,11,5),Tri(0,5,1),Tri(0,1,7),Tri(0,7,10),Tri(0,10,11),
        Tri(1,5,9),Tri(5,11,4),Tri(11,10,2),Tri(10,7,6),Tri(7,1,8),
        Tri(3,9,4),Tri(3,4,2),Tri(3,2,6),Tri(3,6,8),Tri(3,8,9),
        Tri(4,9,5),Tri(2,4,11),Tri(6,2,10),Tri(8,6,7),Tri(9,8,1)
    )
    val cache = mutableMapOf<Long, Int>()
    fun mid(a: Int, b: Int): Int {
        val key = minOf(a,b).toLong()*10000 + maxOf(a,b)
        return cache.getOrPut(key) {
            verts.add(verts[a].midpoint(verts[b]) * radius)
            verts.size - 1
        }
    }
    repeat(frequency - 1) {
        faces = faces.flatMap { t ->
            val m0=mid(t.a,t.b); val m1=mid(t.b,t.c); val m2=mid(t.a,t.c)
            listOf(Tri(t.a,m0,m2),Tri(t.b,m1,m0),Tri(t.c,m2,m1),Tri(m0,m1,m2))
        }
    }
    return Pair(verts, faces)
}

// ─── Rotation Helpers ────────────────────────────────────────────────────────

fun Vec3.rotX(a: Float) = Vec3(x, y*cos(a)-z*sin(a), y*sin(a)+z*cos(a))
fun Vec3.rotY(a: Float) = Vec3(x*cos(a)+z*sin(a), y, -x*sin(a)+z*cos(a))
operator fun Vec3.times(s: Float) = Vec3(x*s, y*s, z*s)

// ─── Projection ──────────────────────────────────────────────────────────────

fun Vec3.project(cx: Float, cy: Float, scale: Float, fov: Float = 5f): Offset {
    val f = fov / (fov + z)
    return Offset(cx + x * scale * f, cy - y * scale * f)
}

// ─── Color Utility ───────────────────────────────────────────────────────────

fun Color.shade(b: Float) = Color(
    red=(red*b).coerceIn(0f,1f),
    green=(green*b).coerceIn(0f,1f),
    blue=(blue*b).coerceIn(0f,1f)
)

// ─── Main Composable ─────────────────────────────────────────────────────────

@Composable
fun IcosphereDemo() {
    // Build sphere once
    val (verts, faces) = remember { buildIcosphere(frequency = 2, radius = 1f) }

    val transition = rememberInfiniteTransition(label = "ico")
    val rx by transition.animateFloat(0f, (2*PI).toFloat(),
        infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "icoRx")
    val ry by transition.animateFloat(0f, (2*PI).toFloat(),
        infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "icoRy")
    val lightAngle by transition.animateFloat(0f, (2*PI).toFloat(),
        infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "icoLight")
    val glowPulse by transition.animateFloat(0.8f, 1.2f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow")

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030310))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = minOf(size.width, size.height) * 0.30f

        // Orbiting light direction
        val lightDir = Vec3(cos(lightAngle), 0.6f, sin(lightAngle)).normalize()

        // Rotate all vertices
        val rotated = verts.map { v -> v.rotX(rx).rotY(ry) }

        // Project to screen
        val projected = rotated.map { it.project(cx, cy, scale) }

        // Process each face
        data class FaceInfo(
            val tri: Tri, val avgZ: Float,
            val normal: Vec3, val brightness: Float
        )

        val faceInfos = faces.map { tri ->
            val va = rotated[tri.a]; val vb = rotated[tri.b]; val vc = rotated[tri.c]
            val avgZ = (va.z + vb.z + vc.z) / 3f
            val normal = (vb - va).cross(vc - va).normalize()
            val brightness = maxOf(0f, normal.dot(lightDir))
            FaceInfo(tri, avgZ, normal, brightness)
        }

        // Sort back-to-front (painter's algorithm)
        val sorted = faceInfos.sortedByDescending { it.avgZ }

        // Outer glow
        for (i in 4 downTo 1) {
            drawCircle(
                color = Color(0xFF4A9EFF).copy(alpha = 0.04f * i),
                radius = scale * glowPulse + i * 18f,
                center = Offset(cx, cy)
            )
        }

        // Draw faces
        sorted.forEach { fi ->
            // Back-face culling: skip if normal points away from camera
            if (fi.normal.z > 0.1f) return@forEach

            val ambient    = 0.12f
            val total      = (ambient + fi.brightness * (1f - ambient)).coerceIn(0f, 1f)
            val faceColor  = Color(0xFF4A9EFF).shade(total)
            val edgeColor  = Color(0xFF7BBFFF).copy(alpha = (total * 0.5f).coerceIn(0f, 1f))

            val pa = projected[fi.tri.a]
            val pb = projected[fi.tri.b]
            val pc = projected[fi.tri.c]

            val path = Path().apply {
                moveTo(pa.x, pa.y)
                lineTo(pb.x, pb.y)
                lineTo(pc.x, pc.y)
                close()
            }

            drawPath(path, faceColor)
            drawPath(path, edgeColor, style = Stroke(0.8f))
        }

        // Orbiting light indicator
        val lightIndicatorPos = Offset(
            cx + scale * 1.5f * cos(lightAngle),
            cy - scale * 0.6f
        )
        drawCircle(Color(0xFFFFFFAA).copy(alpha = 0.7f), 10f, lightIndicatorPos)
        for (i in 1..3) {
            drawCircle(Color(0xFFFFFFAA).copy(alpha = 0.1f * i),
                10f + i * 6f, lightIndicatorPos)
        }
    }
}
```

### Bonus: Frequency selector UI (see 1 vs 2 vs 3 subdivisions)

```kotlin
@Composable
fun IcosphereFrequencyDemo() {
    var frequency by remember { mutableStateOf(2) }

    val transition = rememberInfiniteTransition(label = "freq")
    val ry by transition.animateFloat(0f, (2*PI).toFloat(),
        infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "freqRy")
    val lightAngle by transition.animateFloat(0f, (2*PI).toFloat(),
        infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "freqLA")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030310)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val scale = minOf(size.width, size.height) * 0.28f

            val (verts, faces) = buildIcosphere(frequency, 1f)
            val lightDir = Vec3(cos(lightAngle), 0.7f, sin(lightAngle)).normalize()
            val rotated = verts.map { it.rotY(ry) }
            val projected = rotated.map { it.project(cx, cy, scale) }

            faces.map { tri ->
                val va=rotated[tri.a]; val vb=rotated[tri.b]; val vc=rotated[tri.c]
                val n = (vb-va).cross(vc-va).normalize()
                Triple(tri, (va.z+vb.z+vc.z)/3f, maxOf(0f, n.dot(lightDir)).also { if (n.z > 0.1f) return@also -1f })
            }.filter { it.third >= 0f }
            .sortedByDescending { it.second }
            .forEach { (tri, _, b) ->
                val total = 0.15f + b * 0.85f
                val pts = listOf(projected[tri.a], projected[tri.b], projected[tri.c])
                val path = Path().apply { moveTo(pts[0].x,pts[0].y); pts.drop(1).forEach { lineTo(it.x,it.y) }; close() }
                drawPath(path, Color(0xFF4A9EFF).shade(total))
                drawPath(path, Color(0xFF7BBFFF).copy(alpha=0.4f), style=Stroke(0.8f))
            }
        }

        // Frequency selector
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(1, 2, 3).forEach { f ->
                Button(
                    onClick = { frequency = f },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (f == frequency) Color(0xFF4A9EFF) else Color(0xFF2A2A4A)
                    )
                ) {
                    Text("f=$f  (${20*f*f} faces)")
                }
            }
        }
    }
}
```

---

## 🧪 Trace

**Full pipeline for one triangle face at rx=0.5, ry=1.0, lightDir=(0.5, 0.6, -0.6)**

```
Vertices (on unit sphere):
  A = icoVert[0] = (0, 0.850, 0.526)  (normalized)
  B = icoVert[11] = (-0.850, 0, 0.526)
  C = icoVert[5]  = (0, 0.526, 0.850)

After rotX(0.5):
  A' = (0, 0.850×cos(0.5)-0.526×sin(0.5), ...) ≈ (0, 0.744, 0.824)
  (apply similar for B', C')

Face normal (cross product B'-A' × C'-A') → normalize → n

dot(n, lightDir) → brightness → shade color → draw ✓
```

---

## 🏆 Phase 6 Complete — Full Graphics Math Vault

| Chapter | Concept |
|---------|---------|
| 3D Projection | Orthographic, Perspective, Isometric |
| Normals & Lighting | Lambert diffuse, back-face culling |
| Geodesic Polyhedron | Icosahedron + subdivision algorithm |
| **Icosphere** | Full pipeline: geometry → transform → cull → sort → shade → project → draw |

---

## 🗺️ You Now Know

```
Phase 1 — Trigonometry     ✅ Unit circle, sin/cos, radians, polar, oscillation
Phase 2 — Vectors          ✅ Direction, magnitude, dot product, normalize
Phase 3 — Transforms       ✅ Translate, rotate, scale, hierarchies
Phase 4 — Interpolation    ✅ lerp, Bezier, easing functions, spring physics
Phase 5 — Matrix Math      ✅ Matrix, rotation matrix, transform chains, 3D cube
Phase 6 — 3D Advanced      ✅ Projection, lighting, geodesic polyhedra, Icosphere
```

**You can now build anything you can draw.** 🎉

→ Return to [[Index]] for the full map.
