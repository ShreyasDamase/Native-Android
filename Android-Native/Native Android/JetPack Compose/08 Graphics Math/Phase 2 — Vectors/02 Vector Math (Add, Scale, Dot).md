# 02 — Vector Math: Add, Scale, Dot Product

> **Core Idea**: Three operations do 90% of all animation physics — Add (combine forces), Scale (change speed), Dot Product (measure alignment). Master these and physics simulations become mechanical.

---

## 🧠 Concept

### Addition — Combine two forces
A ball is pushed right AND falls down. Two forces, one motion.
```
velocity = Offset(5f, 0f)   // moving right
gravity  = Offset(0f, 2f)   // pulling down
result   = Offset(5f, 2f)   // moves diagonally
```

### Scaling — Change the speed
A particle moving at speed 5 should now move at speed 10.
Scale the velocity vector by 2. Direction unchanged, magnitude doubled.
```
velocity = Offset(3f, 4f)   // magnitude = 5
faster   = Offset(6f, 8f)   // magnitude = 10, same direction
```

### Dot Product — How aligned are two vectors?
Tells you: "Is A pointing in the same direction as B?"
- Result = +1 → completely aligned (same direction)
- Result = 0  → perpendicular (90° apart)
- Result = -1 → opposite directions

Used for: reflection angles, shadow calculations, surface lighting.

---

## 📐 Diagram

```
VECTOR ADDITION — tip-to-tail method:

  A = (4, 1)     B = (1, 3)     A + B = (5, 4)
  ┌─────────────────────────────────────────┐
  │         ╭───────────────── A+B ─────────│
  │     B  ╱                   ↗            │
  │       ╱                  ↗              │
  │      ╱                 ↗               │
  │     ╱               ↗    A             │
  │    *───────────────*─────────────────── │
  │   origin           A                   │
  └─────────────────────────────────────────┘

SCALING — stretch or shrink:

  v = (2, 3)     v×2 = (4, 6)    v×0.5 = (1, 1.5)
  ┌────────────────────────────────────────┐
  │         *  ← v×2                       │
  │        ↑                               │
  │       * ← v                            │
  │      ↑                                 │
  │     * ← v×0.5                          │
  │    ↑                                   │
  │───*──────────────────────────────────  │
  └────────────────────────────────────────┘

DOT PRODUCT — measuring angle between vectors:

  A·B = |A| × |B| × cos(angle between them)
  Or:  A·B = Ax×Bx + Ay×By

  cos(0°) = 1  → A·B = |A||B|   (same direction)
  cos(90°)= 0  → A·B = 0        (perpendicular)
  cos(180°)=-1 → A·B = -|A||B|  (opposite)

  A=(1,0), B=(0,1)    A=(1,0), B=(1,0)    A=(1,0), B=(-1,0)
  A·B = 0             A·B = 1              A·B = -1
  (perpendicular)     (same)               (opposite)
```

---

## 🔢 The Math

### Kotlin implementation

```kotlin
import kotlin.math.sqrt
import kotlin.math.abs
import androidx.compose.ui.geometry.Offset

// --- Vector Addition ---
// Compose Offset already supports + operator
val sum = offsetA + offsetB

// --- Vector Subtraction ---
// Direction FROM b TO a: use a - b
val direction = pointA - pointB

// --- Scalar Multiplication ---
// Compose Offset supports * with Float
val scaled = offset * 2.5f

// --- Magnitude ---
fun Offset.magnitude(): Float = sqrt(x * x + y * y)

// --- Normalize (length = 1, direction preserved) ---
fun Offset.normalize(): Offset {
    val m = magnitude()
    return if (m < 0.0001f) Offset.Zero else Offset(x / m, y / m)
}

// --- Dot Product ---
fun Offset.dot(other: Offset): Float = x * other.x + y * other.y

// --- Reflect vector across a normal ---
// Used for bouncing off surfaces
fun Offset.reflect(normal: Offset): Offset {
    val n = normal.normalize()
    val d = this.dot(n)
    return Offset(x - 2 * d * n.x, y - 2 * d * n.y)
}

// --- Distance between two points ---
fun distanceBetween(a: Offset, b: Offset): Float = (a - b).magnitude()

// --- Linear interpolation between two positions ---
fun lerpOffset(start: Offset, end: Offset, t: Float): Offset =
    Offset(start.x + (end.x - start.x) * t, start.y + (end.y - start.y) * t)
```

---

## 🎨 Compose Code

### Example 1: Force visualizer — add gravity + wind

```kotlin
@Composable
fun ForceVisualizerDemo() {
    val gravity   = Offset(0f, 120f)   // downward force
    val wind      = Offset(80f, 0f)    // rightward force
    val resultant = gravity + wind     // combined

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val origin = Offset(size.width / 2f, size.height / 2f)

        // Draw gravity arrow (blue)
        drawArrow(origin, origin + gravity, Color(0xFF4A9EFF))
        // Draw wind arrow (teal) — starts at tip of gravity
        drawArrow(origin + gravity, origin + gravity + wind, Color(0xFF4ECDC4))
        // Draw resultant (red) — from origin
        drawArrow(origin, origin + resultant, Color(0xFFFF6B6B))

        drawCircle(Color.White, 8f, origin)
    }
}
```

### Example 2: Dot product — alignment indicator

```kotlin
@Composable
fun DotProductDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "dot")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "angle"
    )

    val fixedVector = Offset(1f, 0f)  // pointing right
    val rotatingVector = Offset(cos(angle), sin(angle))
    val dotProduct = fixedVector.dot(rotatingVector)  // = cos(angle)

    // Interpret dot product
    val alignment = when {
        dotProduct > 0.7f  -> "Aligned ✓"
        dotProduct < -0.7f -> "Opposite ✗"
        else               -> "Perpendicular ~"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val len = 100f

            // Fixed vector (right)
            drawArrow(Offset(cx, cy), Offset(cx + len, cy), Color(0xFF4A9EFF))

            // Rotating vector
            val color = when {
                dotProduct > 0.7f  -> Color(0xFF4ECDC4)
                dotProduct < -0.7f -> Color(0xFFFF6B6B)
                else               -> Color(0xFFFFD700)
            }
            drawArrow(Offset(cx, cy),
                Offset(cx + len * rotatingVector.x, cy + len * rotatingVector.y), color)
        }

        Spacer(Modifier.height(16.dp))
        Text("Dot = %.2f".format(dotProduct), color = Color.White, fontSize = 18.sp)
        Text(alignment, color = Color(0xFF4ECDC4), fontSize = 16.sp)
    }
}
```

### Example 3: Billiard ball reflection using dot product

```kotlin
@Composable
fun BallReflectionDemo() {
    var position by remember { mutableStateOf(Offset(100f, 150f)) }
    var velocity by remember { mutableStateOf(Offset(3f, 2f)) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)

            var newPos = position + velocity
            var newVel = velocity

            // Wall normals for a rect canvas ~400×600
            val walls = listOf(
                Pair(Offset(1f, 0f), 20f),      // left wall,  normal = right
                Pair(Offset(-1f, 0f), 380f),    // right wall, normal = left
                Pair(Offset(0f, 1f), 20f),      // top wall,   normal = down
                Pair(Offset(0f, -1f), 580f)     // bottom wall,normal = up
            )

            for ((normal, boundary) in walls) {
                val crossed = when {
                    normal.x > 0 && newPos.x < boundary -> true
                    normal.x < 0 && newPos.x > boundary -> true
                    normal.y > 0 && newPos.y < boundary -> true
                    normal.y < 0 && newPos.y > boundary -> true
                    else -> false
                }
                if (crossed) {
                    // reflect(v, n) = v - 2(v·n)n
                    val dot = newVel.dot(normal)
                    newVel = Offset(newVel.x - 2 * dot * normal.x,
                                   newVel.y - 2 * dot * normal.y)
                }
            }

            position = newPos
            velocity = newVel
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        // Border
        drawRect(Color(0xFF2A2A4A),
            style = androidx.compose.ui.graphics.drawscope.Stroke(2f))

        // Velocity arrow
        drawArrow(position, position + velocity * 20f, Color(0xFF4ECDC4), arrowSize = 12f)

        // Ball
        drawCircle(Color(0xFFFF6B6B), 20f, position)
    }
}
```

---

## 🧪 Trace

**Dot product check — is a bullet heading toward an enemy?**

```
bullet direction: velocity = (4, 3) → normalize → (0.8, 0.6)
enemy direction:  toEnemy  = (2, 1) → normalize → (0.894, 0.447)

dot = 0.8 × 0.894 + 0.6 × 0.447
    = 0.715 + 0.268
    = 0.983

dot ≈ 0.98 → nearly aligned → bullet IS heading toward enemy ✓

(If dot < 0, bullet is moving away from enemy)
```

**Reflection of velocity (3, 2) off a floor (normal = (0, -1))**:
```
dot = (3)(0) + (2)(-1) = -2
reflected = (3 - 2×(-2)×0, 2 - 2×(-2)×(-1))
           = (3, 2 - 4) = (3, -2)

Ball bounces off floor, Y velocity flips ✓
```

---

## 🔗 Next

→ [[03 Normalize and Direction]] — *The unit vector — direction without speed — and why it powers targeting, aiming, and following*
