# 05 — Oscillation Patterns

> **Core Idea**: Real motion is never a single sine wave. It's sin + cos combined, scaled, and phase-shifted. Combine them and you get figure-8s, Lissajous curves, orbital wobble, and chaotic shimmer.

---

## 🧠 Concept

A single `sin(θ)` moves something up and down smoothly. That's boring and mechanical.

Real animation feels alive because objects move in **two dimensions simultaneously**, with slightly different rhythms.

When you combine:
- X = cos(aθ + phaseX)
- Y = sin(bθ + phaseY)

...and vary `a`, `b`, and the phases, you get **Lissajous figures** — every complex-looking oscillatory path in animation.

If a = b and phase difference = π/2, you get a **circle** (already seen in unit circle).
Change the ratios and you get ellipses, figure-8s, clover shapes.

---

## 📐 Diagram

```
LISSAJOUS FIGURES — different a:b ratios:

  a=1, b=1, Δphase=0       a=1, b=1, Δphase=π/2    a=1, b=2, Δphase=0
  ┌────────────┐            ┌────────────┐           ┌────────────┐
  │     .──.   │            │   .─────.  │           │     ∞      │
  │    /    \  │            │  /       \ │           │   ╭───╮    │
  │    \    /  │            │  \       / │           │  /     \   │
  │     '──'   │            │   '─────'  │           │  \     /   │
  └────────────┘            └────────────┘           └────────────┘
  Line diagonal             Circle                   Figure-8 (horizontal)

  a=1, b=3, Δphase=π/2     a=2, b=3, Δphase=π/4
  ┌────────────┐            ┌────────────┐
  │    ╭─╮     │            │  ╭──╮╭──╮  │
  │   / │ \    │            │ /    X    \ │
  │  │  │  │  │            │ \   / \   / │
  │   \ │ /    │            │  ╰──╯╰──╯  │
  │    ╰─╯     │            └────────────┘
  └────────────┘
  Triple loop               Complex curve

PENDULUM OSCILLATION:
  Simple pendulum (damped):
  
  amplitude ──▶ ██████
                  ███
                   █
                   █ ─── damps over time
                   ─
                   .  ← stops

  Damped sine:  A × e^(-kt) × sin(ωt)
  e^(-kt) = decay envelope shrinking amplitude
```

---

## 🔢 The Math

### Lissajous formula

```
x(t) = Ax × cos(a × t + δ)
y(t) = Ay × sin(b × t)
```

| Variable | Meaning |
|----------|---------|
| `Ax`, `Ay` | amplitude on each axis |
| `a`, `b` | frequency ratio (try 1:2, 2:3, 3:4) |
| `δ (delta)` | phase difference between x and y |

### Damped oscillation

```
y(t) = A × e^(-kt) × sin(ω × t)
```

| Variable | Meaning |
|----------|---------|
| `A` | initial amplitude |
| `k` | damping coefficient (higher = stops faster) |
| `ω` | angular frequency |
| `e^(-kt)` | exponential decay envelope |

---

## 🎨 Compose Code

### Example 1: Lissajous figure drawn as a path

```kotlin
import androidx.compose.ui.graphics.Path
import kotlin.math.*

@Composable
fun LissajousDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "lissajous")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val Ax = 200f   // amplitude X
        val Ay = 150f   // amplitude Y
        val a = 3f      // frequency ratio X — try 1, 2, 3, 4
        val b = 2f      // frequency ratio Y — try 1, 2, 3

        val path = Path()
        val steps = 600

        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * (2 * PI).toFloat()
            val x = cx + Ax * cos(a * t + phase)
            val y = cy + Ay * sin(b * t)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // Glow effect — draw twice with different opacity
        drawPath(
            path = path,
            color = Color(0x554A9EFF),
            style = androidx.compose.ui.graphics.drawscope.Stroke(8f)
        )
        drawPath(
            path = path,
            color = Color(0xFF4A9EFF),
            style = androidx.compose.ui.graphics.drawscope.Stroke(2f)
        )

        // Moving dot on the curve
        val dotT = phase  // use phase as current position
        val dotX = cx + Ax * cos(a * dotT + phase)
        val dotY = cy + Ay * sin(b * dotT)
        drawCircle(Color(0xFFFF6B6B), 10f, Offset(dotX, dotY))
    }
}
```

### Example 2: Damped oscillation (spring settling)

```kotlin
@Composable
fun DampedOscillationDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "damped")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4 * PI.toFloat(),  // 2 full cycles of damping
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val amplitude = 150f
        val dampingK = 0.5f       // damping strength
        val omega = 3f            // angular frequency

        // Draw the full damped wave path
        val path = Path()
        val steps = 500
        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * 4 * PI.toFloat()
            val decay = exp(-dampingK * t)
            val x = cx - size.width / 2f + (i.toFloat() / steps) * size.width
            val y = cy + amplitude * decay * sin(omega * t)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = Color(0xFF4ECDC4),
            style = androidx.compose.ui.graphics.drawscope.Stroke(2f)
        )

        // Draw envelope curves (decay boundary)
        val envPath1 = Path()
        val envPath2 = Path()
        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * 4 * PI.toFloat()
            val decay = exp(-dampingK * t)
            val x = cx - size.width / 2f + (i.toFloat() / steps) * size.width
            val env = amplitude * decay
            if (i == 0) {
                envPath1.moveTo(x, cy + env)
                envPath2.moveTo(x, cy - env)
            } else {
                envPath1.lineTo(x, cy + env)
                envPath2.lineTo(x, cy - env)
            }
        }

        drawPath(envPath1, Color(0x55FF6B6B), style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
        drawPath(envPath2, Color(0x55FF6B6B), style = androidx.compose.ui.graphics.drawscope.Stroke(1f))

        // Current moving dot
        val decay = exp(-dampingK * time)
        val dotX = cx - size.width / 2f + (time / (4 * PI.toFloat())) * size.width
        val dotY = cy + amplitude * decay * sin(omega * time)
        drawCircle(Color(0xFFFF6B6B), 10f, Offset(dotX, dotY))

        // Center line
        drawLine(Color(0xFF2A2A4A), Offset(0f, cy), Offset(size.width, cy), 1f)
    }
}
```

### Example 3: Pendulum with natural swing

```kotlin
@Composable
fun PendulumDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "pendulum")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val pivotX = size.width / 2f
        val pivotY = size.height * 0.25f
        val length = 220f
        val maxAngle = (PI / 4).toFloat()   // 45° swing

        // Pendulum angle: simple harmonic motion = A × cos(ωt)
        val angle = maxAngle * cos(time) - (PI / 2).toFloat()  // offset so 0° = down

        val bobX = pivotX + length * cos(angle)
        val bobY = pivotY + length * sin(angle)

        // String
        drawLine(
            color = Color(0xFF4A4A6A),
            start = Offset(pivotX, pivotY),
            end = Offset(bobX, bobY),
            strokeWidth = 2f
        )

        // Pivot
        drawCircle(Color(0xFF4A4A6A), 8f, Offset(pivotX, pivotY))

        // Bob (pendulum weight)
        drawCircle(Color(0xFFFF6B6B), 24f, Offset(bobX, bobY))
        drawCircle(Color(0x66FFFFFF), 10f, Offset(bobX - 8f, bobY - 8f))

        // Arc showing the swing path
        val arcRect = androidx.compose.ui.geometry.Rect(
            left = pivotX - length,
            top = pivotY - length,
            right = pivotX + length,
            bottom = pivotY + length
        )
        drawArc(
            color = Color(0x222A2A5A),
            startAngle = -135f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = androidx.compose.ui.graphics.drawscope.Stroke(1f)
        )
    }
}
```

---

## 🧪 Trace

**Lissajous at t = 0.5, a=2, b=3, Ax=200, Ay=150, phase=0, center=(400,400)**:

```
x = 400 + 200 × cos(2 × 0.5 + 0) = 400 + 200 × cos(1.0) = 400 + 200 × 0.5403 = 508.06
y = 400 + 150 × sin(3 × 0.5)     = 400 + 150 × sin(1.5) = 400 + 150 × 0.9975 = 549.63

Dot at pixel (508, 550)
```

**Damped oscillation at t = 2.0, k=0.5, A=150, ω=3**:

```
decay = e^(-0.5 × 2.0) = e^(-1.0) = 0.3679
y offset = 150 × 0.3679 × sin(3 × 2.0)
         = 150 × 0.3679 × sin(6.0)
         = 150 × 0.3679 × (-0.2794)
         = -15.42 px from center
```

Amplitude has shrunk from 150 to ~55px after one full revolution.

---

## 🔗 Next

→ [[06 Project — Analog Clock]] — *Apply Phase 1 math to build a fully functional analog clock*
