# 03 — Normalize and Direction

> **Core Idea**: A **unit vector** has magnitude = 1. It carries only direction, no speed. It is the most useful vector in game development — it separates "where to go" from "how fast to go."

---

## 🧠 Concept

Suppose a homing missile needs to fly toward a target. You know:
- Missile position: (100, 200)
- Target position: (350, 500)

Direction vector = target − missile = (250, 300)

But that vector has magnitude ≈ 390. If you use it as velocity, the missile travels 390px per frame — instant teleport.

**Normalize** it: divide by magnitude → (0.64, 0.77). Magnitude = 1.
Now multiply by desired speed: `× 5f` → (3.2, 3.85) — travels 5px/frame toward target.

This is the formula: `velocity = normalize(target − self) × speed`

It appears in: homing bullets, follow-cam, look-at rotations, particle emission, AI chasing behavior.

---

## 📐 Diagram

```
NORMALIZING — collapsing any vector to length 1:

  v = (3, 4)          normalize(v) = (0.6, 0.8)
  magnitude = 5       magnitude = 1.0

  ─────────────────────────────────────────────
  │     *  ← v (3,4) — length 5               │
  │    ↗                                       │
  │   *  ← normalize(v) (0.6, 0.8) — length 1 │
  │  ↗                                         │
  │─*──────────────────────────────────────── │
  origin

  After normalizing: direction stays the same,
  length becomes exactly 1.

DIRECTION-TO-TARGET:

  missile (100, 200)        target (350, 500)
      *──────────────────────────→ *
      │←─── direction vector ───→│
      │     = (350-100, 500-200) │
      │     = (250, 300)         │
      │     magnitude = 390.5    │
      │                          │
      normalized = (0.640, 0.768)
      speed = 5px/frame
      velocity = (3.20, 3.84)

UNIT CIRCLE CONNECTION:
  Every normalized vector lives ON the unit circle.
  normalize(cos(θ), sin(θ)) = (cos(θ), sin(θ))  ← already normalized!
```

---

## 🔢 The Math

```kotlin
// Standard normalize implementation
fun Offset.normalize(): Offset {
    val m = sqrt(x * x + y * y)
    return if (m < 0.0001f) Offset.Zero else Offset(x / m, y / m)
}

// Direction from A to B (unit vector)
fun directionTo(from: Offset, to: Offset): Offset = (to - from).normalize()

// Move toward a target at given speed
fun moveToward(current: Offset, target: Offset, speed: Float): Offset {
    val direction = directionTo(current, target)
    val distance  = (target - current).magnitude()
    // Don't overshoot: min(speed, distance)
    val step = minOf(speed, distance)
    return Offset(current.x + direction.x * step, current.y + direction.y * step)
}
```

---

## 🎨 Compose Code

### Example 1: Homing dot — follows your finger / a moving target

```kotlin
@Composable
fun HomingDotDemo() {
    val target = remember { mutableStateOf(Offset(300f, 300f)) }
    var missile by remember { mutableStateOf(Offset(100f, 100f)) }

    // Animate target moving in a circle
    val infiniteTransition = rememberInfiniteTransition(label = "target")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "targetAngle"
    )

    // Move missile toward target
    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            val t = target.value
            val direction = directionTo(missile, t)
            val distance = (t - missile).magnitude()
            if (distance > 2f) {
                missile = Offset(
                    missile.x + direction.x * 4f,
                    missile.y + direction.y * 4f
                )
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Update target in circular path
        val newTarget = Offset(
            cx + 200f * cos(angle),
            cy + 200f * sin(angle)
        )
        target.value = newTarget

        // Target circle path
        drawCircle(Color(0xFF1A1A3A), 200f, Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(1f))

        // Direction line from missile to target
        drawLine(Color(0xFF2A2A4A), missile, newTarget, 1f)

        // Draw normalized direction arrow
        val dir = directionTo(missile, newTarget)
        drawArrow(missile, missile + dir * 40f, Color(0xFF4ECDC4), arrowSize = 12f)

        // Target
        drawCircle(Color(0xFFFFD700), 12f, newTarget)

        // Missile
        drawCircle(Color(0xFFFF6B6B), 16f, missile)
        drawCircle(Color(0x66FFFFFF), 6f, missile + Offset(-5f, -5f))
    }
}

// Helpers (reuse from previous file)
fun Offset.magnitude(): Float = sqrt(x * x + y * y)
fun Offset.normalize(): Offset {
    val m = magnitude()
    return if (m < 0.0001f) Offset.Zero else Offset(x / m, y / m)
}
fun directionTo(from: Offset, to: Offset): Offset = (to - from).normalize()
```

### Example 2: Repulsion field — particles flee from center

```kotlin
@Composable
fun RepulsionFieldDemo() {
    data class Particle(val pos: Offset, val vel: Offset)

    var particles by remember {
        mutableStateOf(
            (0 until 20).map {
                val angle = (it.toFloat() / 20f) * 2 * Math.PI.toFloat()
                val dist = 50f + it * 8f
                Particle(
                    pos = Offset(200f + dist * cos(angle), 400f + dist * sin(angle)),
                    vel = Offset.Zero
                )
            }
        )
    }

    val center = Offset(200f, 400f)

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            particles = particles.map { p ->
                // Direction AWAY from center (normalize, then negate would be toward)
                val awayDir = directionTo(center, p.pos)   // center → particle = away
                val distance = (p.pos - center).magnitude()
                val strength = (200f / (distance + 1f)).coerceAtMost(5f)

                val newVel = Offset(
                    p.vel.x * 0.92f + awayDir.x * strength,
                    p.vel.y * 0.92f + awayDir.y * strength
                )
                val newPos = p.pos + newVel
                Particle(newPos, newVel)
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        // Center repulsor
        drawCircle(Color(0xFFFF6B6B), 20f, center)
        for (i in 1..3) {
            drawCircle(Color(0x22FF6B6B), i * 40f, center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
        }

        // Particles
        particles.forEach { p ->
            drawCircle(Color(0xFF4A9EFF), 8f, p.pos)
        }
    }
}
```

### Example 3: Look-at rotation — arrow always points toward target

```kotlin
@Composable
fun LookAtDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "lookaT")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "lookatAngle"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        val turret = Offset(cx, cy)
        val target = Offset(
            cx + 200f * cos(angle),
            cy + 200f * sin(angle)
        )

        // Direction to target
        val dir = directionTo(turret, target)

        // Draw turret body
        drawCircle(Color(0xFF2A2A4A), 30f, turret)

        // Draw turret barrel — points toward target using direction vector
        val barrelEnd = turret + dir * 50f
        drawLine(Color(0xFF4A9EFF), turret, barrelEnd, strokeWidth = 6f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)

        // Target
        drawCircle(Color(0xFFFFD700), 14f, target)

        // Target orbit path
        drawCircle(Color(0xFF1A1A3A), 200f, Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
    }
}
```

---

## 🧪 Trace

**Missile at (100, 200), target at (350, 500), speed = 5px/frame**:

```
Step 1: Direction vector
  diff = (350 - 100, 500 - 200) = (250, 300)

Step 2: Magnitude
  |diff| = sqrt(250² + 300²) = sqrt(62500 + 90000) = sqrt(152500) ≈ 390.5

Step 3: Normalize
  dir = (250/390.5, 300/390.5) = (0.640, 0.768)
  Verify: sqrt(0.64² + 0.768²) = sqrt(0.410 + 0.590) = sqrt(1.0) = 1.0 ✓

Step 4: Scale by speed
  velocity = (0.640 × 5, 0.768 × 5) = (3.20, 3.84) px/frame

Step 5: New position (after 1 frame)
  new = (100 + 3.20, 200 + 3.84) = (103.20, 203.84)
```

Each frame, missile moves 5px closer to target.

---

## 🔗 Next

→ [[04 Project — Radar Screen]] — *Build a rotating radar sweep with blips using everything from Phase 2*
