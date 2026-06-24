# 03 — Radians vs Degrees

> **Core Idea**: Degrees are for humans. Radians are for math. Kotlin's `sin()` and `cos()` speak radians. You must convert — or better, think in radians from the start.

---

## 🧠 Concept

You learned angles in degrees: 0°, 90°, 180°, 360°. That's fine for everyday life.

But every math library — Kotlin, C, JavaScript — expects **radians** in trigonometric functions.

Why? Because radians are the **natural unit** of angle. They are defined by the circle itself.

**Definition**: 1 radian is the angle where the arc length equals the radius.

```
If radius = r, and you walk an arc of length r along the circle,
the angle you swept is exactly 1 radian.
```

A full circle (360°) has a circumference of `2πr`. Divide by radius `r` → `2π` radians.

So: **360° = 2π radians**

---

## 📐 Diagram

```
DEGREES vs RADIANS — same circle, two languages:

          90° = π/2
             │
    ┌────────┼────────┐
    │        │        │
    │  135°  │  45°   │
    │  3π/4  │  π/4   │
    │        │        │
180°┼────────0────────┼── 0° = 0 (or 2π)
(π) │        │        │
    │  225°  │  315°  │
    │  5π/4  │  7π/4  │
    │        │        │
    └────────┼────────┘
             │
          270° = 3π/2

CONVERSION RULES:
┌─────────────────────────────────────────┐
│  degrees → radians:  θ_rad = θ_deg × π/180   │
│  radians → degrees:  θ_deg = θ_rad × 180/π   │
└─────────────────────────────────────────┘

COMMON ANGLES:
  0°   = 0
  30°  = π/6  ≈ 0.524
  45°  = π/4  ≈ 0.785
  60°  = π/3  ≈ 1.047
  90°  = π/2  ≈ 1.571
  120° = 2π/3 ≈ 2.094
  180° = π    ≈ 3.142
  270° = 3π/2 ≈ 4.712
  360° = 2π   ≈ 6.283
```

---

## 🔢 The Math

### Conversion formulas

```kotlin
import kotlin.math.PI

// Degrees → Radians
fun degreesToRadians(degrees: Float): Float = (degrees * PI / 180).toFloat()

// Radians → Degrees
fun radiansToDegrees(radians: Float): Float = (radians * 180 / PI).toFloat()

// Kotlin shorthand (Double extension)
val rad = 45.0 * (PI / 180)   // 0.7854
val deg = PI / 4 * (180 / PI) // 45.0
```

### In Compose Canvas: rotation uses degrees

```kotlin
// Modifier.rotate() → degrees
Modifier.rotate(45f)   // ← degrees ✓

// Canvas rotate() → degrees too
rotate(degrees = 45f) {
    // draw something rotated
}

// But sin() and cos() → radians
val angle = 45f * (PI / 180).toFloat()  // must convert
val x = cos(angle)
val y = sin(angle)
```

> ⚠️ **Trap**: `Modifier.rotate()` and `Canvas.rotate()` use **degrees**. But `sin()`, `cos()`, `atan2()` all use **radians**. Keep them separate.

---

## 🎨 Compose Code

### Example 1: Dial showing angle in both units

```kotlin
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun AngleDualDialDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "dial")
    val angleDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val angleRad = angleDeg * (PI / 180).toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(
            modifier = Modifier.size(280.dp)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension / 2f - 20f

            // Circle
            drawCircle(
                color = Color(0xFF1A1A3A),
                radius = r,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFF2A2A5A),
                radius = r,
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(2f)
            )

            // Tick marks every 30°
            for (deg in 0 until 360 step 30) {
                val rad = deg * (PI / 180).toFloat()
                val inner = r * 0.85f
                val outer = r * 0.95f
                drawLine(
                    color = Color(0xFF4A4A7A),
                    start = Offset(cx + inner * cos(rad), cy + inner * sin(rad)),
                    end = Offset(cx + outer * cos(rad), cy + outer * sin(rad)),
                    strokeWidth = 2f
                )
            }

            // Angle needle
            val needleX = cx + r * 0.75f * cos(angleRad)
            val needleY = cy + r * 0.75f * sin(angleRad)
            drawLine(
                color = Color(0xFF4A9EFF),
                start = Offset(cx, cy),
                end = Offset(needleX, needleY),
                strokeWidth = 3f
            )

            // Dot at tip
            drawCircle(Color(0xFFFF6B6B), 10f, Offset(needleX, needleY))
            drawCircle(Color.White, 5f, Offset(cx, cy))
        }

        Spacer(Modifier.height(24.dp))

        // Display both units
        Text(
            text = "%.1f°  =  %.3f rad".format(angleDeg, angleRad),
            color = Color(0xFF4A9EFF),
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
```

### Example 2: Rotating a square — Modifier vs Canvas

```kotlin
@Composable
fun RotationComparisonDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val degrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "degrees"
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Method 1: Modifier.rotate() — uses degrees directly
        Box(
            modifier = Modifier
                .size(80.dp)
                .rotate(degrees)                          // ← degrees ✓
                .background(Color(0xFF4A9EFF))
        )

        // Method 2: Canvas rotate() — also uses degrees
        Canvas(modifier = Modifier.size(100.dp)) {
            rotate(degrees = degrees) {                   // ← degrees ✓
                drawRect(
                    color = Color(0xFFFF6B6B),
                    topLeft = Offset(size.width / 4f, size.height / 4f),
                    size = androidx.compose.ui.geometry.Size(size.width / 2f, size.height / 2f)
                )
            }
        }

        // Method 3: Manual sin/cos — must use radians
        Canvas(modifier = Modifier.size(100.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val rad = degrees * (PI / 180).toFloat()    // ← convert to radians ✓
            val tipX = cx + 40f * cos(rad)
            val tipY = cy + 40f * sin(rad)
            drawLine(
                color = Color(0xFF4ECDC4),
                start = Offset(cx, cy),
                end = Offset(tipX, tipY),
                strokeWidth = 4f
            )
            drawCircle(Color(0xFF4ECDC4), 8f, Offset(tipX, tipY))
            drawCircle(Color.White, 4f, Offset(cx, cy))
        }
    }
}
```

---

## 🧪 Trace

**Convert 120° to radians, then find the canvas point at radius 150px from center (300, 300)**:

```
Step 1: Convert
  radians = 120 × (π / 180)
          = 120 × 0.01745
          = 2.094 rad

Step 2: Compute position
  x = 300 + 150 × cos(2.094)
    = 300 + 150 × (-0.5)
    = 300 - 75
    = 225

  y = 300 + 150 × sin(2.094)
    = 300 + 150 × 0.866
    = 300 + 129.9
    = 429.9

Result: pixel (225, 430) — upper-left quadrant of the circle
(Y is flipped in Canvas, so sin positive = downward on screen)
```

---

## 🔗 Next

→ [[04 Polar Coordinates]] — *How (radius, angle) describes any point — and how to use it for spiral paths*
