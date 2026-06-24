# 04 — Project: Elastic Motion

> **Phase 4 Capstone**: Build spring physics, elastic bounce, and rubber-band UI using pure math — no `spring()` AnimationSpec. Just `sin()`, `exp()`, and easing functions.

---

## 🧠 What We're Building

- **Spring slider** — drag and release, springs back using damped oscillation
- **Elastic card** — stretches beyond target then snaps back
- **Rubber band pull** — overscroll effect with exponential decay
- **Bouncy balls** — each with different damping ratios

---

## 📐 Diagram

```
SPRING PHYSICS — damped oscillation:

  displacement
  ▲
  │  released here
  │ *
  │  \  overshoot
  │   \   *
  │    \ /  undershoot
  │     *     *
  │            *
  │─────────────────────────────────▶ time
  0      settles here (target)

  x(t) = A × e^(-kt) × sin(ωt + φ) + target

  A = initial displacement
  k = damping (0 = no damping, ∞ = overdamped)
  ω = angular frequency (how fast it oscillates)
  φ = phase offset

RUBBER BAND — exponential distance decay:

  User pulls distance d beyond boundary:
  visualOffset = d × (1 - e^(-d/resistance))
  
  d=0:   offset = 0          (at boundary)
  d=50:  offset = 50×(1-0.6) = 20
  d=100: offset = 100×(1-0.37)= 63
  d=200: offset = 200×(1-0.13)= 174

  Starts linear, progressively harder to pull → rubber band feel
```

---

## 🎨 Compose Code

### Example 1: Spring settle animation from pure math

```kotlin
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun SpringSettleDemo() {
    // Spring parameters
    val stiffness   = 200f    // ω² — how stiff the spring is
    val damping     = 20f     // 2 × k — damping coefficient
    val mass        = 1f

    var springX     by remember { mutableStateOf(100f) }
    var velocity    by remember { mutableStateOf(0f) }
    val targetX     = 300f

    // Tap button to flick
    var isAnimating by remember { mutableStateOf(false) }

    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            springX  = 50f
            velocity = 500f
            val dt   = 0.016f  // 16ms per frame

            repeat(300) {  // max 5 seconds
                delay(16L)
                // Spring force: F = -k × displacement - damping × velocity
                val displacement = springX - targetX
                val force = -stiffness * displacement - damping * velocity
                val acceleration = force / mass

                velocity += acceleration * dt
                springX  += velocity * dt

                // Stop if settled
                if (abs(velocity) < 0.5f && abs(springX - targetX) < 0.5f) {
                    springX = targetX
                    velocity = 0f
                    return@repeat
                }
            }
            isAnimating = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
        ) {
            // Track
            drawLine(Color(0xFF2A2A4A), Offset(50f, size.height/2f),
                Offset(size.width - 50f, size.height/2f), 2f)
            // Target marker
            drawCircle(Color(0xFF2A2A5A), 20f, Offset(targetX, size.height/2f),
                style = Stroke(2f))
            // Spring ball
            drawCircle(Color(0xFFFF6B6B).copy(alpha = 0.3f), 30f,
                Offset(springX, size.height/2f))
            drawCircle(Color(0xFFFF6B6B), 20f, Offset(springX, size.height/2f))
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = { isAnimating = true }) {
            Text("Release Spring")
        }
    }
}
```

### Example 2: Bouncy balls — different damping ratios

```kotlin
@Composable
fun BouncyBallsDemoMath() {
    data class Ball(
        val color: Color,
        val dampingRatio: Float,     // 0 = no damping (infinite bounce), 1 = critical
        val label: String
    )

    val balls = listOf(
        Ball(Color(0xFFFF6B6B), 0.1f,  "NoBouncy"),
        Ball(Color(0xFFFFD700), 0.3f,  "HighBouncy"),
        Ball(Color(0xFF4ECDC4), 0.6f,  "MedBouncy"),
        Ball(Color(0xFF4A9EFF), 0.9f,  "LowBouncy"),
    )

    // Animate using Compose's built-in spring for comparison
    var trigger by remember { mutableStateOf(false) }
    val transitions = balls.map { ball ->
        animateFloatAsState(
            targetValue = if (trigger) 0f else 1f,
            animationSpec = spring(
                dampingRatio = ball.dampingRatio,
                stiffness = Spring.StiffnessMedium
            ),
            label = ball.label
        )
    }

    LaunchedEffect(Unit) {
        delay(500L); trigger = true
        delay(3000L); trigger = false
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
            .clickable { trigger = !trigger }
    ) {
        val floorY = size.height * 0.75f
        val colWidth = size.width / (balls.size + 1)

        // Floor
        drawLine(Color(0xFF2A2A4A), Offset(0f, floorY), Offset(size.width, floorY), 2f)

        balls.forEachIndexed { i, ball ->
            val t = transitions[i].value  // 0 = floor, 1 = top
            val ballX = colWidth * (i + 1)
            val ballY = floorY - (floorY * 0.7f) * (1f - t)

            drawCircle(ball.color.copy(alpha = 0.2f), 24f, Offset(ballX, ballY))
            drawCircle(ball.color, 16f, Offset(ballX, ballY))

            // Shadow
            val shadowScale = t * 0.8f + 0.2f
            drawOval(Color(0x44000000),
                Offset(ballX - 20f * shadowScale, floorY - 6f),
                androidx.compose.ui.geometry.Size(40f * shadowScale, 10f))
        }
    }
}
```

### Example 3: Rubber band overscroll

```kotlin
@Composable
fun RubberBandDemo() {
    var dragOffset by remember { mutableStateOf(0f) }
    var isSettling by remember { mutableStateOf(false) }
    val resistance = 300f  // higher = harder to pull

    // Rubber band formula: visual = raw × (1 - e^(-raw/resistance))
    fun rubberBand(raw: Float): Float {
        return raw * (1f - exp(-abs(raw) / resistance)) * sign(raw)
    }

    // Settle back after release
    LaunchedEffect(isSettling) {
        if (isSettling) {
            var vel = 0f
            while (abs(dragOffset) > 0.5f || abs(vel) > 0.5f) {
                delay(16L)
                val spring = -dragOffset * 15f
                val damp   = -vel * 5f
                vel = (vel + (spring + damp) * 0.016f)
                dragOffset += vel * 0.016f
            }
            dragOffset = 0f
            isSettling = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val visualOffset = rubberBand(dragOffset)

        Box(
            modifier = Modifier
                .offset(y = visualOffset.dp * 0.3f)
                .size(280.dp, 160.dp)
                .background(
                    Color(0xFF1A1A3A),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                )
                .border(
                    2.dp,
                    Color(0xFF4A9EFF).copy(alpha = (abs(visualOffset) / 100f).coerceIn(0f, 1f)),
                    androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { isSettling = true },
                        onDrag = { _, delta ->
                            dragOffset += delta.y
                            dragOffset = dragOffset.coerceIn(-200f, 200f)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("↕ Drag me", color = Color.White, fontSize = 18.sp)
                Text(
                    "pull: %.0fpx → visual: %.0fpx".format(dragOffset, visualOffset),
                    color = Color(0xFF4A9EFF),
                    fontSize = 12.sp
                )
            }
        }
    }
}
```

---

## 🧪 Trace

**Spring physics one step: position=50, target=300, velocity=500, stiffness=200, damping=20, dt=0.016**:

```
displacement = 50 - 300 = -250
force        = -200 × (-250) - 20 × 500 = 50000 - 10000 = 40000
acceleration = 40000 / 1 = 40000
velocity     = 500 + 40000 × 0.016 = 500 + 640 = 1140
position     = 50 + 1140 × 0.016 = 50 + 18.24 = 68.24

Next frame: spring is pulling the ball toward 300, velocity increased.
```

**Rubber band at raw pull = 100, resistance = 300**:
```
visual = 100 × (1 - e^(-100/300))
       = 100 × (1 - e^(-0.333))
       = 100 × (1 - 0.717)
       = 100 × 0.283
       = 28.3px

User pulled 100px but only sees 28px movement → resistance feel ✓
```

---

## 📋 Phase 4 Complete

| Concept | You Can Now… |
|---------|-------------|
| lerp | Blend any two values over time |
| Bezier Curves | Draw and follow smooth curved paths |
| Easing as Math | Write any easing from pure math functions |
| **Elastic Motion** | Build spring physics, bounce, rubber-band from scratch |

---

## 🔗 Next Phase

→ [[Phase 5 — Matrix Math/01 What is a Matrix]] — *A 2D array that encodes any transform — the unified representation of translate + rotate + scale*
