# 03 — Geodesic Polyhedron

> **Core Idea**: A geodesic polyhedron is a sphere approximated by triangles. Start with an icosahedron (20 equilateral triangles). Subdivide each triangle recursively. Push all new vertices to the surface of a sphere. Repeat. The higher the frequency, the rounder the result.

---

## 🧠 Concept

A perfect sphere has infinite triangles. We can't draw that. So we approximate it.

The best polyhedron base is the **icosahedron** — 20 equilateral triangles arranged symmetrically.

**Subdivision** (frequency n):
1. Take each triangle
2. Add n-1 new vertices along each edge
3. Split each triangle into n² smaller triangles
4. Push all new vertices to the sphere surface (normalize them to radius r)

Frequency 1 = icosahedron (20 faces)
Frequency 2 = 80 faces
Frequency 3 = 180 faces
Frequency n = 20n² faces

**Why normalize?** Because midpoints of a triangle edge inside a sphere fall INSIDE the sphere. Normalizing them (same as `normalize()` from Phase 2) pushes them back out to the surface.

---

## 📐 Diagram

```
ICOSAHEDRON — the base shape:

  12 vertices, 20 faces, 30 edges
  Every face is an equilateral triangle

  Top view:           Side view:
         *                  *
        /|\               / | \
       / | \             /  |  \
      *──────*          *───────*
     /|  |  |           \  |  /
    / |  |  |            \ | /
   *──────────*            *

  ICOSAHEDRON VERTEX POSITIONS:
  Using the golden ratio φ = (1 + √5) / 2 ≈ 1.618

  Vertices lie on 3 perpendicular rectangles of size (1 × φ):
  (0, ±1, ±φ), (±φ, 0, ±1), (±1, ±φ, 0)
  → 12 vertices total, normalized to sphere radius

SUBDIVISION — one triangle → four triangles:

  BEFORE:                      AFTER (frequency 2):
     A                           A
    / \                         / \
   /   \                       m0───m2
  /     \                     / \ / \
 B───────C                   B───m1───C

  m0 = midpoint(A, B) → normalize → push to sphere
  m1 = midpoint(B, C) → normalize → push to sphere
  m2 = midpoint(A, C) → normalize → push to sphere

  Result: 4 triangles from 1
  All 3 midpoint vertices ON the sphere surface

FREQUENCY COMPARISON:
  f=1: icosahedron     20 faces  ← exactly
  f=2: 80 faces        ← smooth enough for most uses
  f=3: 180 faces       ← very smooth
  f=4: 320 faces       ← diminishing returns
```

---

## 🔢 The Math

```kotlin
import kotlin.math.*

data class Vec3f(val x: Float, val y: Float, val z: Float) {
    fun normalize(): Vec3f {
        val m = sqrt(x*x + y*y + z*z)
        return if (m < 1e-6f) this else Vec3f(x/m, y/m, z/m)
    }
    fun scale(r: Float) = Vec3f(x*r, y*r, z*r)
    fun midpoint(other: Vec3f) = Vec3f(
        (x + other.x) / 2f,
        (y + other.y) / 2f,
        (z + other.z) / 2f
    ).normalize()  // ← normalize pushes to sphere!
}

data class Triangle(val a: Int, val b: Int, val c: Int)

// Golden ratio for icosahedron
val PHI = ((1f + sqrt(5f)) / 2f)

// Icosahedron: 12 base vertices (normalized)
val icoVertices: List<Vec3f> = run {
    val t = PHI
    listOf(
        Vec3f(-1f,  t, 0f), Vec3f( 1f,  t, 0f),
        Vec3f(-1f, -t, 0f), Vec3f( 1f, -t, 0f),
        Vec3f(0f, -1f,  t), Vec3f(0f,  1f,  t),
        Vec3f(0f, -1f, -t), Vec3f(0f,  1f, -t),
        Vec3f( t, 0f, -1f), Vec3f( t, 0f,  1f),
        Vec3f(-t, 0f, -1f), Vec3f(-t, 0f,  1f),
    ).map { it.normalize() }
}

// Icosahedron: 20 base triangles
val icoFaces: List<Triangle> = listOf(
    Triangle(0,11,5),  Triangle(0,5,1),   Triangle(0,1,7),
    Triangle(0,7,10),  Triangle(0,10,11), Triangle(1,5,9),
    Triangle(5,11,4),  Triangle(11,10,2), Triangle(10,7,6),
    Triangle(7,1,8),   Triangle(3,9,4),   Triangle(3,4,2),
    Triangle(3,2,6),   Triangle(3,6,8),   Triangle(3,8,9),
    Triangle(4,9,5),   Triangle(2,4,11),  Triangle(6,2,10),
    Triangle(8,6,7),   Triangle(9,8,1),
)

// Subdivide: split each triangle into 4 smaller ones
fun subdivide(vertices: MutableList<Vec3f>, faces: List<Triangle>, radius: Float): List<Triangle> {
    val newFaces = mutableListOf<Triangle>()
    val midpointCache = mutableMapOf<Long, Int>()

    fun getMidpoint(a: Int, b: Int): Int {
        val key = minOf(a, b).toLong() * 1000 + maxOf(a, b)
        return midpointCache.getOrPut(key) {
            val mid = vertices[a].midpoint(vertices[b]).scale(radius)
            vertices.add(mid)
            vertices.size - 1
        }
    }

    for (tri in faces) {
        val m0 = getMidpoint(tri.a, tri.b)
        val m1 = getMidpoint(tri.b, tri.c)
        val m2 = getMidpoint(tri.a, tri.c)

        newFaces.add(Triangle(tri.a, m0, m2))
        newFaces.add(Triangle(tri.b, m1, m0))
        newFaces.add(Triangle(tri.c, m2, m1))
        newFaces.add(Triangle(m0, m1, m2))
    }

    return newFaces
}

// Build a geodesic sphere with given frequency and radius
fun buildGeodesicSphere(frequency: Int, radius: Float): Pair<List<Vec3f>, List<Triangle>> {
    val vertices = icoVertices.map { it.scale(radius) }.toMutableList()
    var faces: List<Triangle> = icoFaces

    repeat(frequency - 1) {
        faces = subdivide(vertices, faces, radius)
    }

    return Pair(vertices, faces)
}
```

---

## 🎨 Compose Code

### Geodesic Polyhedron Wireframe

```kotlin
@Composable
fun GeodesicWireframeDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "geo")
    val rx by infiniteTransition.animateFloat(0f, (2*PI).toFloat(),
        infiniteRepeatable(tween(15000, easing = LinearEasing)), label = "grx")
    val ry by infiniteTransition.animateFloat(0f, (2*PI).toFloat(),
        infiniteRepeatable(tween(10000, easing = LinearEasing)), label = "gry")

    // Build geodesic sphere (frequency 2 = 80 faces)
    val (geoVerts, geoFaces) = remember { buildGeodesicSphere(frequency = 2, radius = 1f) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030310))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = minOf(size.width, size.height) * 0.35f

        // Rotate all vertices
        val rotated = geoVerts.map { v ->
            rotateVecY(rotateVecX(v, rx), ry)
        }

        // Project
        val projected = rotated.map { perspectiveProject(it, cx, cy, scale, 5f) }

        // Collect unique edges
        val edges = mutableSetOf<Pair<Int, Int>>()
        geoFaces.forEach { tri ->
            edges.add(minOf(tri.a, tri.b) to maxOf(tri.a, tri.b))
            edges.add(minOf(tri.b, tri.c) to maxOf(tri.b, tri.c))
            edges.add(minOf(tri.a, tri.c) to maxOf(tri.a, tri.c))
        }

        // Draw edges — color by Z depth
        edges.forEach { (a, b) ->
            val avgZ = (rotated[a].z + rotated[b].z) / 2f
            val alpha = ((avgZ + 1f) / 2f).coerceIn(0.1f, 1f)  // -1..1 → 0.1..1.0
            drawLine(
                color = Color(0xFF4A9EFF).copy(alpha = alpha * 0.8f),
                start = projected[a],
                end = projected[b],
                strokeWidth = 1.2f
            )
        }

        // Vertices
        geoVerts.indices.forEach { i ->
            val z = rotated[i].z
            if (z < 0) {  // only front hemisphere
                drawCircle(
                    Color(0xFF4ECDC4).copy(alpha = 0.6f),
                    2f,
                    projected[i]
                )
            }
        }
    }
}
```

---

## 🧪 Trace

**Subdivide one triangle: A=(0,1,0), B=(1,0,0), C=(0,0,1) on unit sphere**:

```
Step 1: Midpoint of A and B
  raw_m0 = ((0+1)/2, (1+0)/2, (0+0)/2) = (0.5, 0.5, 0.0)
  normalize → magnitude = sqrt(0.25+0.25+0) = sqrt(0.5) = 0.707
  m0 = (0.5/0.707, 0.5/0.707, 0) = (0.707, 0.707, 0.0)
  ✓ On unit sphere: 0.707² + 0.707² = 0.5 + 0.5 = 1.0 ✓

Step 2: Midpoint of B and C
  raw_m1 = (0.5, 0, 0.5)
  magnitude = sqrt(0.5) = 0.707
  m1 = (0.707, 0, 0.707) ✓ On unit sphere

Step 3: Midpoint of A and C
  raw_m2 = (0, 0.5, 0.5)
  m2 = (0, 0.707, 0.707) ✓ On unit sphere

4 new triangles: (A, m0, m2), (B, m1, m0), (C, m2, m1), (m0, m1, m2)
All 3 midpoints lie exactly on the unit sphere surface ✓
```

---

## 🔗 Next

→ [[04 Project — Icosphere]] — *Build the complete animated Icosphere with flat shading, lighting, and perspective*
