# 01 — What is a Vector

> **Core Idea**: A vector is not just a number — it's a number with a **direction**. It describes "how much AND which way." This is the foundation of every force, velocity, and path calculation in animation.

---

## 🧠 Concept

A **scalar** is a single number: `5.0`. It describes magnitude only.
A **vector** is a pair (or triple) of numbers: `(3, 4)`. It describes both magnitude and direction.

In 2D graphics, a vector is always `(x, y)`:
- **x** = how far right (positive) or left (negative)
- **y** = how far down (positive) or up (negative) in Canvas coordinates

Vectors can represent:
- **Position** — where is the object? `Offset(200f, 300f)`
- **Velocity** — how fast and which way? `velocity = Offset(2f, -5f)` (moving right and up)
- **Direction** — which way without speed? `Offset(1f, 0f)` = pointing right
- **Force** — push in which direction? `gravity = Offset(0f, 9.8f)` = downward

---

## 📐 Diagram

```
VECTOR as an arrow:

  y
  ▲
  │
  │      B(5, 4)
4 │        *
  │      ↗
  │    ↗  ← vector (5, 4) — an arrow from origin to B
  │  ↗
  │↗
──0────────────────▶ x
  0    5

VECTOR COMPONENTS:
  │←────── x = 5 ──────→│
  ┌─────────────────────┐
  │                     *  ← tip B(5,4)
  │                   ↗
4 │                 ↗
  │               ↗
  │             ↗
  │           ↗    ↑
  │         ↗      │ y = 4
  │       ↗        │
  │     ↗          ↓
  *───────────────────  ← origin A(0,0)

MAGNITUDE (length of arrow):
  |v| = sqrt(x² + y²) = sqrt(5² + 4²) = sqrt(41) ≈ 6.40

DIRECTION (angle):
  θ = atan2(y, x) = atan2(4, 5) ≈ 38.7°

SAME VECTOR, DIFFERENT POSITIONS:
  A vector has no fixed position. The arrow (3,2) means
  "move 3 right, 2 down" regardless of where it starts.

  Start (0,0) → end (3,2)    ← same vector
  Start (5,1) → end (8,3)    ← same vector
  Start (100,50)→end(103,52) ← same vector
```

---

## 🔢 The Math

### Vector operations

| Operation | Formula | Meaning |
|-----------|---------|---------|
| **Magnitude** | `√(x² + y²)` | Length of the vector |
| **Direction** | `atan2(y, x)` | Angle in radians |
| **Add** | `(ax+bx, ay+by)` | Combine two forces |
| **Subtract** | `(ax-bx, ay-by)` | Direction from B to A |
| **Scale** | `(x×k, y×k)` | Make faster/slower |
| **Normalize** | `(x/|v|, y/|v|)` | Direction only, length=1 |

```kotlin
import kotlin.math.sqrt
import kotlin.math.atan2
import androidx.compose.ui.geometry.Offset

// Magnitude
fun Offset.magnitude(): Float = sqrt(x * x + y * y)

// Normalize (unit vector)
fun Offset.normalize(): Offset {
    val mag = magnitude()
    return if (mag == 0f) Offset.Zero else Offset(x / mag, y / mag)
}

// Scale
operator fun Offset.times(scalar: Float): Offset = Offset(x * scalar, y * scalar)

// Dot product (how aligned are two vectors?)
fun Offset.dot(other: Offset): Float = x * other.x + y * other.y

// Angle from vector
fun Offset.angle(): Float = atan2(y, x)  // returns radians
```

---

## 🎨 Compose Code

### Example 1: Draw a vector as an arrow on Canvas

```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.*

// Utility: draw an arrow from start to end
fun DrawScope.drawArrow(
    from: Offset,
    to: Offset,
    color: Color,
    strokeWidth: Float = 3f,
    arrowSize: Float = 20f
) {
    drawLine(color = color, start = from, end = to, strokeWidth = strokeWidth)

    val angle = atan2(to.y - from.y, to.x - from.x)
    val arrowPath = Path().apply {
        moveTo(to.x, to.y)
        lineTo(
            to.x - arrowSize * cos(angle - PI.toFloat() / 6f),
            to.y - arrowSize * sin(angle - PI.toFloat() / 6f)
        )
        lineTo(
            to.x - arrowSize * cos(angle + PI.toFloat() / 6f),
            to.y - arrowSize * sin(angle + PI.toFloat() / 6f)
        )
        close()
    }
    drawPath(arrowPath, color)
}

@Composable
fun VectorArrowDemo() {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val origin = Offset(size.width / 2f, size.height / 2f)

        // Draw coordinate axes
        drawArrow(Offset(origin.x - 200f, origin.y), Offset(origin.x + 200f, origin.y),
            Color(0xFF2A2A4A), 1f, 12f)
        drawArrow(Offset(origin.x, origin.y + 200f), Offset(origin.x, origin.y - 200f),
            Color(0xFF2A2A4A), 1f, 12f)

        // Vector A = (150, -80) — right and up
        val vectorA = Offset(150f, -80f)
        drawArrow(origin, origin + vectorA, Color(0xFF4A9EFF))

        // Vector B = (-60, 120) — left and down
        val vectorB = Offset(-60f, 120f)
        drawArrow(origin, origin + vectorB, Color(0xFFFF6B6B))

        // Sum vector (A + B)
        val sumVector = Offset(vectorA.x + vectorB.x, vectorA.y + vectorB.y)
        drawArrow(origin, origin + sumVector, Color(0xFF4ECDC4))

        // Dot at origin
        drawCircle(Color.White, 6f, origin)
    }
}
```

### Example 2: Particle following a velocity vector

```kotlin
@Composable
fun VelocityParticleDemo() {
    // Mutable state for position and velocity
    var position by remember { mutableStateOf(Offset(200f, 200f)) }
    var velocity by remember { mutableStateOf(Offset(3f, 1.5f)) }

    // Physics loop
    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)  // ~60fps

            // Update position: pos = pos + velocity
            val newX = position.x + velocity.x
            val newY = position.y + velocity.y

            // Bounce off walls (not knowing canvas size yet — approximate)
            val vx = if (newX < 20f || newX > 380f) -velocity.x else velocity.x
            val vy = if (newY < 20f || newY > 580f) -velocity.y else velocity.y

            position = Offset(newX.coerceIn(20f, 380f), newY.coerceIn(20f, 580f))
            velocity = Offset(vx, vy)
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        // Draw velocity arrow
        val velScale = 20f
        drawArrow(
            from = position,
            to = position + Offset(velocity.x * velScale, velocity.y * velScale),
            color = Color(0xFF4ECDC4),
            arrowSize = 15f
        )

        // Draw particle
        drawCircle(Color(0xFFFF6B6B), 16f, position)
        drawCircle(Color(0x66FFFFFF), 6f, position + Offset(-5f, -5f))
    }
}
```

### Example 3: Force field — gravity pulling a ball

```kotlin
@Composable
fun GravityVectorDemo() {
    val gravity = Offset(0f, 0.4f)   // gravity vector: downward

    var position by remember { mutableStateOf(Offset(200f, 100f)) }
    var velocity by remember { mutableStateOf(Offset(2f, 0f)) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            // Apply gravity to velocity: v = v + gravity
            val newVelocity = Offset(velocity.x + gravity.x, velocity.y + gravity.y)
            // Apply velocity to position: pos = pos + velocity
            var newPosition = Offset(position.x + newVelocity.x, position.y + newVelocity.y)

            var finalVelocity = newVelocity
            // Floor bounce: reverse Y velocity and lose some energy
            if (newPosition.y > 500f) {
                newPosition = Offset(newPosition.x, 500f)
                finalVelocity = Offset(finalVelocity.x, -finalVelocity.y * 0.7f)
            }
            // Side walls: wrap
            if (newPosition.x > 400f) newPosition = Offset(0f, newPosition.y)

            position = newPosition
            velocity = finalVelocity
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        // Draw floor
        drawLine(Color(0xFF2A2A4A), Offset(0f, 500f), Offset(size.width, 500f), 2f)

        // Gravity arrow at ball position
        drawArrow(position, position + Offset(gravity.x * 80, gravity.y * 80),
            Color(0xFF4ECDC4), arrowSize = 12f)

        // Velocity arrow
        drawArrow(position, position + velocity * 5f,
            Color(0xFF4A9EFF), arrowSize = 12f)

        // Ball
        drawCircle(Color(0xFFFF6B6B), 18f, position)
    }
}
```

---

## 🧪 Trace

**Vector A = (3, 4)**:

```
Magnitude = sqrt(3² + 4²) = sqrt(9 + 16) = sqrt(25) = 5.0
Direction = atan2(4, 3) = 53.13° = 0.9273 rad

Normalized = (3/5, 4/5) = (0.6, 0.8)
  Verify: sqrt(0.6² + 0.8²) = sqrt(0.36 + 0.64) = sqrt(1.0) = 1.0 ✓

Scaled by 3 = (3×3, 4×3) = (9, 12)
  Magnitude = sqrt(81 + 144) = sqrt(225) = 15 (= 5 × 3) ✓
```

---

## 🔗 Next

→ [[02 Vector Math (Add, Scale, Dot)]] — *The full set of vector operations and when each one is useful in animation*
