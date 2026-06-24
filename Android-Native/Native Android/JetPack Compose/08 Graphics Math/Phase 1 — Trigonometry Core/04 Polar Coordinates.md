# 04 — Polar Coordinates

> **Core Idea**: Instead of describing a point as (x, y), describe it as **(radius, angle)**. This is polar coordinates. It makes circular paths, spirals, and orbits trivially easy to compute.

---

## 🧠 Concept

In the regular Cartesian system, you describe every point with two distances: how far right (x) and how far up (y).

In the **polar system**, you describe every point with:
- **r** — how far from the center (radius)
- **θ (theta)** — what direction (angle)

These two systems are just different languages for the same point. You convert between them using the unit circle formulas:

```
Polar → Cartesian:
  x = r × cos(θ)
  y = r × sin(θ)

Cartesian → Polar:
  r = sqrt(x² + y²)
  θ = atan2(y, x)
```

**Why it matters**: If you want something to move in a circle, you just increase θ. If you want a spiral, you increase both r and θ.

---

## 📐 Diagram

```
CARTESIAN vs POLAR — same point, two descriptions:

CARTESIAN:                    POLAR:
  y                             θ (angle)
  ▲                             ▲
  │       P(3, 4)               │     P(5, 53°)
  4 ──────*                     │  .──*
  │       │                     │ /  r=5
  │       │ y=4                 │/
  │       │                 ───0──────────▶
  0───────┼──────▶ x            0     53°
          3
          x=3

  Both describe the SAME point.

CONVERTING:
  r = sqrt(3² + 4²) = sqrt(9+16) = sqrt(25) = 5
  θ = atan2(4, 3) ≈ 53.1° ≈ 0.927 rad

SPIRAL — r grows as θ grows:
  ╭────────────────────────────────────╮
  │   .                                │
  │  . .   .                           │
  │ .   . . .                          │
  │.     *   .  ←─ r = k × θ          │
  │       .    .                       │
  │        .     .                     │
  │         .       .                  │
  ╰────────────────────────────────────╯
  At θ = 2π, r = k×2π (one full revolution outward)
```

---

## 🔢 The Math

### Archimedean Spiral

```
r = k × θ
```

Where `k` is a growth constant. Every full revolution (2π), radius increases by `2πk`.

```kotlin
val x = cx + (k * theta) * cos(theta)
val y = cy + (k * theta) * sin(theta)
```

### Rose curve (petals)

```
r = cos(n × θ)
```
Where `n` controls the number of petals.

```kotlin
val r = radius * cos(n * theta)
val x = cx + r * cos(theta)
val y = cy + r * sin(theta)
```

---

## 🎨 Compose Code

### Example 1: Dot orbiting at fixed radius (simple polar)

```kotlin
@Composable
fun PolarOrbitDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "orbit")
    val theta by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "theta"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = 180f

        // Orbit ring
        drawCircle(
            color = Color(0xFF1A1A3A),
            radius = r,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(1f)
        )

        // Polar → Cartesian conversion
        val dotX = cx + r * cos(theta)
        val dotY = cy + r * sin(theta)

        // Dashed line from center to dot
        drawLine(
            color = Color(0xFF2A2A5A),
            start = Offset(cx, cy),
            end = Offset(dotX, dotY),
            strokeWidth = 1f
        )

        // Planet dot
        drawCircle(
            color = Color(0xFF4A9EFF),
            radius = 16f,
            center = Offset(dotX, dotY)
        )

        // Center (sun)
        drawCircle(
            color = Color(0xFFFFD700),
            radius = 10f,
            center = Offset(cx, cy)
        )
    }
}
```

### Example 2: Archimedean spiral (r grows with θ)

```kotlin
import androidx.compose.ui.graphics.Path

@Composable
fun SpiralDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "spiral")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        val maxTheta = (4 * Math.PI).toFloat()  // 2 full rotations
        val k = 25f                              // growth rate per radian

        val currentTheta = maxTheta * progress
        val path = Path()
        val steps = (currentTheta / 0.05f).toInt().coerceAtLeast(1)

        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * currentTheta
            val r = k * t
            val x = cx + r * cos(t)
            val y = cy + r * sin(t)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = Color(0xFF4ECDC4),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )

        // Current tip of the spiral
        if (currentTheta > 0) {
            val tipX = cx + k * currentTheta * cos(currentTheta)
            val tipY = cy + k * currentTheta * sin(currentTheta)
            drawCircle(Color(0xFFFF6B6B), 8f, Offset(tipX, tipY))
        }
    }
}
```

### Example 3: Rose curve (r = cos(3θ) → 3 petals)

```kotlin
@Composable
fun RoseCurveDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "rose")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxRadius = 160f
        val n = 3f  // number of petals
        val maxTheta = (Math.PI).toFloat()  // rose with odd n uses 0..π

        val currentTheta = maxTheta * progress
        val path = Path()
        val steps = 500

        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * currentTheta
            // Rose curve formula: r = cos(n × θ)
            val r = maxRadius * cos(n * t)
            val x = cx + r * cos(t)
            val y = cy + r * sin(t)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = Color(0xFFFF6B6B),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
        drawCircle(Color.White, 4f, Offset(cx, cy))
    }
}
```

---

## 🧪 Trace

**Problem**: Planet orbiting at r=150, currently at θ = 2.5 rad. Canvas center = (500, 400).

```
x = 500 + 150 × cos(2.5)
  = 500 + 150 × (-0.8011)
  = 500 - 120.17
  = 379.83

y = 400 + 150 × sin(2.5)
  = 400 + 150 × 0.5985
  = 400 + 89.78
  = 489.78

Planet pixel: (380, 490)
```

**Spiral check**: At θ = 2π (one revolution), k = 25:
```
r = 25 × 6.283 = 157px from center
```

---

## 🔗 Next

→ [[05 Oscillation Patterns]] — *Combining sin + cos to create figure-8s, Lissajous curves, and complex orbits*
