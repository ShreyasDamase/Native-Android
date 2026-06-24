# 01 — Unit Circle

> **Core Idea**: A circle with radius 1. Every point on it has coordinates (cos θ, sin θ). That's the entire secret of animation math.

---

## 🧠 Concept

You have a circle. Radius = 1. Center at origin (0, 0).

Now pick any angle — call it **θ (theta)**. Draw a line from the center to the edge of the circle at that angle.

The point where that line touches the circle is:

```
x = cos(θ)
y = sin(θ)
```

That's it. That's the unit circle. Everything in animation math builds from this.

---

## 📐 Diagram

```
             90° (π/2)
                │
        ┌───────┼───────┐
        │       │       │
        │   .───┤───.   │
        │ .     │     . │
  180°──┤.      │      .│──0° / 360°
(π)     │.      0      .│      (2π)
        │ .     │     . │
        │   '───┤───'   │
        │       │       │
        └───────┼───────┘
                │
            270° (3π/2)

At angle θ = 45° (π/4):
  ┌──────────────────────────────┐
  │         *  ← point           │
  │        /|                    │
  │       / │                    │
  │      /  │ sin(45°) = 0.707  │
  │     /   │                    │
  │    /    │                    │
  │───/─────┼──────────────────  │
  │  0   cos(45°) = 0.707        │
  └──────────────────────────────┘

  x = cos(45°) = 0.707
  y = sin(45°) = 0.707
```

---

## 🔢 The Math

| Angle (°) | Angle (rad) | cos(θ) = x | sin(θ) = y |
|-----------|-------------|------------|------------|
| 0°        | 0           | 1.0        | 0.0        |
| 30°       | π/6         | 0.866      | 0.5        |
| 45°       | π/4         | 0.707      | 0.707      |
| 60°       | π/3         | 0.5        | 0.866      |
| 90°       | π/2         | 0.0        | 1.0        |
| 180°      | π           | -1.0       | 0.0        |
| 270°      | 3π/2        | 0.0        | -1.0       |
| 360°      | 2π          | 1.0        | 0.0        |

**Key insight**: As angle goes 0 → 2π (full rotation), the point traces the circle.

---

## 🎨 Compose Code

### Example 1: Dot moving on a unit circle

```kotlin
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun UnitCircleDemo() {
    // Animate angle from 0 to 2π continuously
    val infiniteTransition = rememberInfiniteTransition(label = "circle")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = 200f

        // Draw the unit circle outline
        drawCircle(
            color = Color(0xFF2A2A4A),
            radius = radius,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )

        // Draw crosshair axes
        drawLine(Color(0xFF2A2A4A), Offset(cx - radius - 20, cy), Offset(cx + radius + 20, cy))
        drawLine(Color(0xFF2A2A4A), Offset(cx, cy - radius - 20), Offset(cx, cy + radius + 20))

        // Calculate dot position using unit circle formula
        val dotX = cx + radius * cos(angle)   // cos → X axis
        val dotY = cy + radius * sin(angle)   // sin → Y axis

        // Draw the radius line
        drawLine(
            color = Color(0xFF4A9EFF),
            start = Offset(cx, cy),
            end = Offset(dotX, dotY),
            strokeWidth = 2f
        )

        // Draw the dot on the circle
        drawCircle(
            color = Color(0xFFFF6B6B),
            radius = 12f,
            center = Offset(dotX, dotY)
        )

        // Draw center point
        drawCircle(
            color = Color.White,
            radius = 4f,
            center = Offset(cx, cy)
        )
    }
}
```

### Example 2: Visualize cos and sin components separately

```kotlin
@Composable
fun CosSinComponentsDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "components")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "angle"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = 150f

        val dotX = cx + radius * cos(angle)
        val dotY = cy + radius * sin(angle)

        // Draw circle
        drawCircle(
            color = Color(0xFF333355),
            radius = radius,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(2f)
        )

        // cos component: horizontal dashed projection to X axis
        drawLine(
            color = Color(0xFF4ECDC4),   // teal = cos/X
            start = Offset(dotX, dotY),
            end = Offset(dotX, cy),
            strokeWidth = 2f
        )

        // sin component: vertical dashed projection to Y axis
        drawLine(
            color = Color(0xFFFF6B6B),   // red = sin/Y
            start = Offset(dotX, dotY),
            end = Offset(cx, dotY),
            strokeWidth = 2f
        )

        // Moving dot
        drawCircle(Color.White, 10f, Offset(dotX, dotY))

        // Labels hint via dot on axes
        drawCircle(Color(0xFF4ECDC4), 6f, Offset(dotX, cy))  // cos on X axis
        drawCircle(Color(0xFFFF6B6B), 6f, Offset(cx, dotY))  // sin on Y axis
    }
}
```

---

## 🧪 Trace

**Walk through**: angle = 1.0 radian, radius = 200px, center = (400, 400)

```
θ = 1.0 rad

cos(1.0) = 0.5403
sin(1.0) = 0.8415

x = 400 + 200 × 0.5403 = 400 + 108.06 = 508.06
y = 400 + 200 × 0.8415 = 400 + 168.30 = 568.30

Dot appears at pixel (508, 568)
```

**Notice**: As θ increases from 0 → 2π, the dot circles back to (600, 400) → (400, 600) → (200, 400) → (400, 200) → (600, 400).

---

## 🔗 Next

→ [[02 Sine and Cosine]] — *Why sin gives you waves and cos gives you horizontal motion*
