# 📐 Graphics Math for Jetpack Compose

> **Philosophy**: Forget memorizing formulas. Visualize them as movement.
> Every formula here is drawn before it is written.

---

## Why Math?

You can use `animateFloatAsState` without knowing math.
But you cannot build a **radar sweep**, a **solar system**, a **3D cube**, or a **Geodesic sphere** without it.

Math is the difference between:
- Copying an animation → **knowing** how to build one
- Using `EaseInOut` → **designing** your own easing curve
- Drawing a circle → **understanding** why it oscillates

---

## The 6-Phase Roadmap

```
Phase 1 ── Trigonometry Core
    │  Unit Circle, Sin/Cos, Radians, Polar Coords, Oscillation
    ▼
Phase 2 ── Vectors
    │  Direction, Magnitude, Dot Product, Normalization
    ▼
Phase 3 ── Coordinate Transforms
    │  Translation, Rotation, Scale
    ▼
Phase 4 ── Interpolation and Curves
    │  lerp, Bezier, Easing as Math
    ▼
Phase 5 ── Matrix Math
    │  Rotation Matrix, Transform Composition
    ▼
Phase 6 ── 3D and Advanced
       Projection, Normals, Geodesic Polyhedra, Icosphere
```

---

## Phase 1 — Trigonometry Core

| # | Topic | What You Build |
|---|-------|---------------|
| [[01 Unit Circle]] | The unit circle and angle as position | Animated dot on circle |
| [[02 Sine and Cosine]] | sin and cos as wave / oscillation | Bouncing ball |
| [[03 Radians vs Degrees]] | Converting, why radians exist | Rotation in Canvas |
| [[04 Polar Coordinates]] | (r, θ) → (x, y) mapping | Spiral path |
| [[05 Oscillation Patterns]] | Combining sin/cos for complex motion | Lissajous figure |
| [[06 Project — Analog Clock]] | Full clock with hour/min/sec hands | Analog Clock |

---

## Phase 2 — Vectors

| # | Topic | What You Build |
|---|-------|---------------|
| [[01 What is a Vector]] | Direction + Magnitude, not just a number | Arrow on Canvas |
| [[02 Vector Math]] | Add, subtract, scale, dot product | Force visualizer |
| [[03 Normalize and Direction]] | Unit vector, always length 1 | Bullet direction |
| [[04 Project — Radar Screen]] | Rotating sweep line, blip dots | Radar UI |

---

## Phase 3 — Coordinate Transforms

| # | Topic | What You Build |
|---|-------|---------------|
| [[01 Translation]] | Moving origin point | Object follow path |
| [[02 Rotation]] | Rotating around an arbitrary pivot | Spinning gear |
| [[03 Scale]] | Non-uniform scale, zoom | Pulsing circle |
| [[04 Project — Solar System]] | Orbiting planets with moons | Solar system |

---

## Phase 4 — Interpolation and Curves

| # | Topic | What You Build |
|---|-------|---------------|
| [[01 Linear Interpolation (lerp)]] | Blending two values | Color morph |
| [[02 Bezier Curves]] | Quadratic and cubic curves | Custom path |
| [[03 Easing Functions as Math]] | Mathematical easing from scratch | Custom easing |
| [[04 Project — Elastic Motion]] | Spring + bounce from pure math | Rubber band UI |

---

## Phase 5 — Matrix Math

| # | Topic | What You Build |
|---|-------|---------------|
| [[01 What is a Matrix]] | Grid of numbers, transform machine | Identity matrix |
| [[02 Rotation Matrix]] | 2D rotation via matrix | Rotating star |
| [[03 Composing Transforms]] | Multiply matrices = chain transforms | Orbiting moon |
| [[04 Project — 3D Cube Projection]] | Project 3D vertices to 2D screen | Spinning cube |

---

## Phase 6 — 3D and Advanced

| # | Topic | What You Build |
|---|-------|---------------|
| [[01 3D Projection Math]] | Perspective vs orthographic | Horizon line |
| [[02 Normals and Lighting]] | Surface normals, dot product lighting | Shaded sphere |
| [[03 Geodesic Polyhedron]] | Icosahedron subdivision | Wireframe dome |
| [[04 Project — Icosphere]] | Full geodesic icosphere | 3D Icosphere |

---

## 5 Milestone Projects

```
Project 1 ── Dot on Circle         (Phase 1 entry)
Project 2 ── Analog Clock          (Phase 1 capstone)
Project 3 ── Radar Screen          (Phase 2 capstone)
Project 4 ── Solar System          (Phase 3 capstone)
Project 5 ── Spinning 3D Cube      (Phase 5 capstone)
Project 6 ── Icosphere             (Phase 6 capstone)
```

---

## Compose Canvas Quick Reference

```kotlin
Canvas(modifier = Modifier.fillMaxSize()) {
    val cx = size.width / 2f   // center X
    val cy = size.height / 2f  // center Y
    val r  = 200f              // radius

    // Point on circle at angle θ
    val x = cx + r * cos(theta)
    val y = cy + r * sin(theta)

    drawCircle(color = Color.White, radius = 8f, center = Offset(x, y))
}
```

> `cos` → horizontal (X axis)
> `sin` → vertical (Y axis)
> angle is in **radians** in Kotlin `kotlin.math`

---

*Start here → [[01 Unit Circle]]*
