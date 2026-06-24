# 06 — Project: Analog Clock

> **Phase 1 Capstone**: Apply everything — angles, sin/cos, radians, polar coordinates — to build a fully working analog clock. Real-time. No cheating with `Modifier.rotate()`.

---

## 🧠 What We're Building

A live analog clock where:
- The **second hand** completes one revolution every 60 seconds
- The **minute hand** completes one revolution every 60 minutes
- The **hour hand** completes one revolution every 12 hours
- All hands are drawn using `cos(θ)` and `sin(θ)` — pure polar math
- The clock face has tick marks drawn with the same technique

---

## 📐 Diagram

```
CLOCK MATH — mapping time to angles:

  Seconds: angle = (seconds / 60) × 2π
  Minutes: angle = (minutes / 60) × 2π + (seconds / 3600) × 2π
  Hours:   angle = (hours / 12) × 2π   + (minutes / 720) × 2π

CLOCK FACE — hands in polar coordinates:

        12 o'clock = -π/2 (top)
              │
              │ hour
              │  hand
     9 ───────0─────── 3
              │
              │
        6 o'clock = π/2 (bottom)

  At 3:00:
    hour angle = (3/12) × 2π = π/2 = pointing right
    BUT canvas 0° starts at the right (east), and 12 is at top (north)
    So offset by -π/2:

    hourAngle = (hours / 12) × 2π - π/2

  Hand tip coordinates:
    tipX = cx + length × cos(angle)
    tipY = cy + length × sin(angle)

TICK MARKS — 12 major + 60 minor:
  for each i in 0..59:
    angle = (i / 60) × 2π - π/2
    innerR = 0.85 × radius  (short ticks at 0.85)
    outerR = radius          (long ticks at r)
    startX = cx + innerR × cos(angle)
    startY = cy + innerR × sin(angle)
    endX   = cx + outerR × cos(angle)
    endY   = cy + outerR × sin(angle)
```

---

## 🎨 Compose Code

### Full Analog Clock Implementation

```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.*

@Composable
fun AnalogClockDemo() {
    // Real-time state — ticks every second
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            calendar = Calendar.getInstance()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(320.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.minDimension / 2f - 12f

            // ── Extract time components ──────────────────────────────────
            val hours   = calendar.get(Calendar.HOUR).toFloat()       // 0–11
            val minutes = calendar.get(Calendar.MINUTE).toFloat()     // 0–59
            val seconds = calendar.get(Calendar.SECOND).toFloat()     // 0–59
            val millis  = calendar.get(Calendar.MILLISECOND).toFloat()

            // ── Clock face ───────────────────────────────────────────────
            // Outer ring
            drawCircle(
                color = Color(0xFF1A1A2E),
                radius = radius,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFF4A9EFF),
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(3f)
            )

            // ── Tick marks ───────────────────────────────────────────────
            for (i in 0 until 60) {
                val tickAngle = (i.toFloat() / 60f) * (2 * PI).toFloat() - (PI / 2).toFloat()
                val isHourMark = i % 5 == 0

                val innerR = if (isHourMark) radius * 0.82f else radius * 0.90f
                val outerR = radius * 0.97f
                val tickWidth = if (isHourMark) 3f else 1f
                val tickColor = if (isHourMark) Color(0xFF4A9EFF) else Color(0xFF2A2A5A)

                drawLine(
                    color = tickColor,
                    start = Offset(cx + innerR * cos(tickAngle), cy + innerR * sin(tickAngle)),
                    end   = Offset(cx + outerR * cos(tickAngle), cy + outerR * sin(tickAngle)),
                    strokeWidth = tickWidth
                )
            }

            // ── Angle calculations ───────────────────────────────────────
            // Offset -π/2 so 0 = 12 o'clock (top), not 3 o'clock (right)
            val offset = (-PI / 2).toFloat()

            // Second hand: sweeps 0→2π in 60s (smooth with millis)
            val secondAngle = ((seconds + millis / 1000f) / 60f) * (2 * PI).toFloat() + offset

            // Minute hand: sweeps 0→2π in 60min (smooth with seconds)
            val minuteAngle = ((minutes + seconds / 60f) / 60f) * (2 * PI).toFloat() + offset

            // Hour hand: sweeps 0→2π in 12h (smooth with minutes)
            val hourAngle = ((hours + minutes / 60f) / 12f) * (2 * PI).toFloat() + offset

            // ── Draw hands using polar coordinates ───────────────────────
            // Hour hand — thick, short
            val hourLen = radius * 0.50f
            drawLine(
                color = Color.White,
                start = Offset(cx, cy),
                end   = Offset(cx + hourLen * cos(hourAngle), cy + hourLen * sin(hourAngle)),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )

            // Minute hand — medium, longer
            val minuteLen = radius * 0.70f
            drawLine(
                color = Color(0xFFCCCCCC),
                start = Offset(cx, cy),
                end   = Offset(cx + minuteLen * cos(minuteAngle), cy + minuteLen * sin(minuteAngle)),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )

            // Second hand — thin, longest, red accent
            val secondLen = radius * 0.85f
            val secondTailLen = radius * 0.15f
            drawLine(
                color = Color(0xFFFF4444),
                start = Offset(
                    cx - secondTailLen * cos(secondAngle),
                    cy - secondTailLen * sin(secondAngle)
                ),
                end   = Offset(cx + secondLen * cos(secondAngle), cy + secondLen * sin(secondAngle)),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )

            // ── Center hub ───────────────────────────────────────────────
            drawCircle(Color(0xFFFF4444), 8f, Offset(cx, cy))
            drawCircle(Color.White, 3f, Offset(cx, cy))
        }
    }
}
```

### Simplified version — teaching each hand separately

```kotlin
// Utility: compute clock hand endpoint given time fraction (0..1)
fun handOffset(
    fraction: Float,     // 0.0 = 12 o'clock, 0.25 = 3 o'clock, 0.5 = 6 o'clock
    length: Float,
    centerX: Float,
    centerY: Float
): Offset {
    // Start at top (-π/2), sweep clockwise
    val angle = fraction * 2 * PI.toFloat() - (PI / 2).toFloat()
    return Offset(
        x = centerX + length * cos(angle),
        y = centerY + length * sin(angle)
    )
}

// Usage:
// Second hand: fraction = seconds / 60f
// Minute hand: fraction = (minutes + seconds/60) / 60f
// Hour hand:   fraction = (hours + minutes/60) / 12f
```

### Bonus: Digital + Analog combined

```kotlin
@Composable
fun HybridClockDemo() {
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) { delay(100L); calendar = Calendar.getInstance() }
    }

    val h = calendar.get(Calendar.HOUR_OF_DAY)
    val m = calendar.get(Calendar.MINUTE)
    val s = calendar.get(Calendar.SECOND)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Digital display
        Text(
            text = "%02d:%02d:%02d".format(h, m, s),
            color = Color(0xFF4A9EFF),
            fontSize = 32.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(Modifier.height(32.dp))

        // Analog clock (same as above, condensed)
        Canvas(modifier = Modifier.size(240.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension / 2f - 8f
            val offset = (-PI / 2).toFloat()

            drawCircle(Color(0xFF1A1A2E), r, Offset(cx, cy))
            drawCircle(Color(0xFF4A9EFF), r, Offset(cx, cy), style = Stroke(2f))

            for (i in 0 until 12) {
                val a = (i.toFloat() / 12f) * (2 * PI).toFloat() + offset
                drawLine(Color(0xFF4A9EFF),
                    start = Offset(cx + r * 0.8f * cos(a), cy + r * 0.8f * sin(a)),
                    end   = Offset(cx + r * 0.95f * cos(a), cy + r * 0.95f * sin(a)),
                    strokeWidth = 3f)
            }

            val secFraction = s.toFloat() / 60f
            val minFraction = (m + s / 60f) / 60f
            val hrFraction  = (h % 12 + m / 60f) / 12f

            drawLine(Color.White, Offset(cx, cy),
                Offset(cx + r * 0.5f * cos(hrFraction * 2 * PI.toFloat() + offset),
                       cy + r * 0.5f * sin(hrFraction * 2 * PI.toFloat() + offset)),
                strokeWidth = 7f, cap = StrokeCap.Round)

            drawLine(Color(0xFFCCCCCC), Offset(cx, cy),
                Offset(cx + r * 0.7f * cos(minFraction * 2 * PI.toFloat() + offset),
                       cy + r * 0.7f * sin(minFraction * 2 * PI.toFloat() + offset)),
                strokeWidth = 4f, cap = StrokeCap.Round)

            drawLine(Color(0xFFFF4444), Offset(cx, cy),
                Offset(cx + r * 0.85f * cos(secFraction * 2 * PI.toFloat() + offset),
                       cy + r * 0.85f * sin(secFraction * 2 * PI.toFloat() + offset)),
                strokeWidth = 2f, cap = StrokeCap.Round)

            drawCircle(Color(0xFFFF4444), 6f, Offset(cx, cy))
        }
    }
}
```

---

## 🧪 Trace

**Draw the second hand at exactly 15 seconds, radius = 160px, center = (200, 200)**:

```
seconds = 15
fraction = 15 / 60 = 0.25

angle = 0.25 × 2π - π/2
      = 0.25 × 6.283 - 1.571
      = 1.571 - 1.571
      = 0 radians  ← pointing right (east = 3 o'clock)

tipX = 200 + 160 × cos(0) = 200 + 160 × 1.0 = 360
tipY = 200 + 160 × sin(0) = 200 + 160 × 0.0 = 200

At 15 seconds: hand points straight RIGHT to pixel (360, 200) ✓
```

**Verify**: 15 seconds = quarter of a minute = 3 o'clock position = pointing right. ✓

---

## 📋 Phase 1 Complete — What You Now Know

| Concept | You Can Now… |
|---------|-------------|
| Unit Circle | Map angle to (x, y) position |
| sin / cos | Drive oscillation and wave motion |
| Radians | Convert and use in all Kotlin math functions |
| Polar Coords | Place objects at (radius, angle) — orbits, spirals |
| Oscillation | Layer sin + cos for complex natural motion |
| **Analog Clock** | Build a real-time clock with pure trigonometry |

---

## 🔗 Next Phase

→ [[Phase 2 — Vectors/01 What is a Vector]] — *Understand direction + magnitude: the building blocks of forces, motion paths, and physics engines*
