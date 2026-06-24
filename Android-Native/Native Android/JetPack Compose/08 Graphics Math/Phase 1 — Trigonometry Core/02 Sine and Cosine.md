# 02 — Sine and Cosine

> **Core Idea**: sin and cos are not exam functions. They are **wave generators**. Give them a continuously increasing angle and they output smooth, infinite oscillation.

---

## 🧠 Concept

From the unit circle, you already know:
- `cos(θ)` = how far right the point is (X component)
- `sin(θ)` = how far down the point is (Y component)

Now imagine **unrolling** the circle into a flat line. As θ marches from 0 → 2π, plot each value of sin(θ) over time.

You get a wave. This is a **sine wave**.

This is how every oscillation in animation works — bouncing, breathing, pulsing, swinging.

---

## 📐 Diagram

```
SINE WAVE — sin(θ) as θ increases:

  1.0 ┤        .***.
      │      .*     *.
  0.5 ┤    .*         *.
      │   *             *
  0.0 ┼──*───────────────*───────────────*──▶ θ
      │ 0  π/2   π   3π/2   2π   5π/2   3π
 -0.5 ┤              *             *
      │             * *           * *
 -1.0 ┤               ***.     .***
      │                   *.*.*

COSINE WAVE — cos(θ) as θ increases:

  1.0 ┤*                               *
      │ *                             *
  0.5 ┤  *                           *
      │   *                         *
  0.0 ┼────*─────────*─────────*────▶ θ
      │     *       * *       *
 -0.5 ┤      *     *   *     *
      │       *   *     *   *
 -1.0 ┤        *.*       *.*

KEY RELATIONSHIP:
  cos(θ) = sin(θ + π/2)   ← cos is just sin shifted left by 90°

  When sin = 0  → cos = ±1   (extremes)
  When sin = ±1 → cos = 0    (at zero)
  They are always 90° out of phase.
```

---

## 🔢 The Math

### Amplitude, Frequency, Phase, Offset

You never use raw `sin(θ)` in real code. You always shape it:

```
value = amplitude × sin(frequency × θ + phase) + offset
```

| Parameter | What it does | Example |
|-----------|-------------|---------|
| **amplitude** | Height of the wave (how far it swings) | `100f` → swings ±100px |
| **frequency** | How fast it oscillates | `2f` → twice as fast |
| **phase** | Starting position of the wave | `π/2` → start at peak |
| **offset** | Vertical shift (baseline) | `400f` → centered at y=400 |

### Practical form used in Compose:

```kotlin
val y = centerY + amplitude * sin(frequency * angle + phase)
val x = centerX + amplitude * cos(frequency * angle + phase)
```

---

## 🎨 Compose Code

### Example 1: Bouncing ball using sin

```kotlin
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
import kotlin.math.abs

@Composable
fun BouncingBallDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
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
        val floorY = size.height * 0.8f
        val amplitude = 200f
        val ballRadius = 24f

        // Ball position: sin oscillates -1 to 1
        // abs() makes it bounce off the floor (only upper half of wave)
        val ballY = floorY - amplitude * abs(sin(angle))

        // Shadow on floor — scales based on how high the ball is
        val distanceFromFloor = floorY - ballY
        val shadowScale = 1f - (distanceFromFloor / amplitude) * 0.7f
        drawOval(
            color = Color(0x44000000),
            topLeft = Offset(cx - 30f * shadowScale, floorY - 5f),
            size = androidx.compose.ui.geometry.Size(60f * shadowScale, 10f)
        )

        // Floor line
        drawLine(
            color = Color(0xFF2A2A4A),
            start = Offset(0f, floorY),
            end = Offset(size.width, floorY),
            strokeWidth = 2f
        )

        // Ball
        drawCircle(
            color = Color(0xFFFF6B6B),
            radius = ballRadius,
            center = Offset(cx, ballY)
        )

        // Highlight on ball
        drawCircle(
            color = Color(0x66FFFFFF),
            radius = ballRadius * 0.35f,
            center = Offset(cx - ballRadius * 0.3f, ballY - ballRadius * 0.3f)
        )
    }
}
```

### Example 2: Sine wave drawn on Canvas

```kotlin
import androidx.compose.ui.graphics.Path

@Composable
fun SineWaveDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cy = size.height / 2f
        val amplitude = 80f
        val frequency = 2f  // how many full waves fit on screen

        val path = Path()
        val steps = size.width.toInt()

        // Draw the wave pixel by pixel
        for (i in 0..steps) {
            val x = i.toFloat()
            // Map screen x to angle, add offset to animate scrolling
            val theta = (x / size.width) * (2 * Math.PI) * frequency + offset
            val y = cy + amplitude * sin(theta).toFloat()

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = Color(0xFF4A9EFF),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )

        // Second wave with phase offset — cos(θ) = sin(θ + π/2)
        val path2 = Path()
        for (i in 0..steps) {
            val x = i.toFloat()
            val theta = (x / size.width) * (2 * Math.PI) * frequency + offset
            val y = cy + amplitude * kotlin.math.cos(theta).toFloat()

            if (i == 0) path2.moveTo(x, y) else path2.lineTo(x, y)
        }

        drawPath(
            path = path2,
            color = Color(0xFFFF6B6B),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )
    }
}
```

### Example 3: Breathing / pulsing effect

```kotlin
@Composable
fun BreathingCircleDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breath"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // sin goes -1 to 1, map to radius 100..200
        val baseRadius = 150f
        val amplitude = 50f
        val radius = baseRadius + amplitude * sin(angle)

        // Outer glow rings
        for (i in 3 downTo 1) {
            drawCircle(
                color = Color(0xFF4A9EFF).copy(alpha = 0.1f * i),
                radius = radius + i * 20f,
                center = Offset(cx, cy)
            )
        }

        // Main circle
        drawCircle(
            color = Color(0xFF4A9EFF),
            radius = radius,
            center = Offset(cx, cy)
        )
    }
}
```

---

## 🧪 Trace

**Setup**: amplitude = 100, frequency = 1, offset = 0, centerY = 400

```
θ = 0        → sin(0) = 0.0    → y = 400 + 100 × 0.0    = 400  (center)
θ = π/2      → sin(1.57) = 1.0 → y = 400 + 100 × 1.0    = 500  (bottom)
θ = π        → sin(3.14) = 0.0 → y = 400 + 100 × 0.0    = 400  (center)
θ = 3π/2     → sin(4.71) = -1.0→ y = 400 + 100 × (-1.0) = 300  (top)
θ = 2π       → sin(6.28) = 0.0 → y = 400 + 100 × 0.0    = 400  (center)

One full loop: 400 → 500 → 400 → 300 → 400
```

**In Compose Canvas**: Y axis points **downward**. So sin = 1 moves the object toward the bottom of the screen, not the top.

---

## 🔗 Next

→ [[03 Radians vs Degrees]] — *Why you pass radians to `sin()` and `cos()` in Kotlin, and how to convert*
