# 04 — Project: Radar Screen

> **Phase 2 Capstone**: A military-style radar with a rotating sweep line, blip detection, and fading trails. This uses vectors for direction, polar coordinates for sweep, and dot product for detection.

---

## 🧠 What We're Building

- A circular radar face with range rings and tick marks
- A rotating green sweep line (uses polar coords from Phase 1)
- Blip objects at various positions — appear when sweep passes over them
- Fading trail behind the sweep (alpha decay)
- Detection logic: dot product to check if sweep has just passed a blip

---

## 📐 Diagram

```
RADAR SCREEN LAYOUT:

        ┌─────────────────────────────────────┐
        │              ┌───────┐              │
        │           ┌──┘       └──┐           │
        │         ┌─┘    rings    └─┐         │
        │        │  ───────────────  │        │
        │       │ │     ╱ sweep    │ │        │
        │       │ │    ╱  line     │ │        │
        │       │ │   ╱            │ │        │
        │       │ │  ╱         •   │ │        │
        │       │ │ 0             •│ │        │
        │       │ │            •   │ │        │
        │        │  ───────────────  │        │
        │         └─┐             ┌─┘         │
        │           └──┐       ┌──┘           │
        │              └───────┘              │
        └─────────────────────────────────────┘

SWEEP DETECTION LOGIC:

  Sweep angle = θ_sweep (rotating)
  Blip at polar (r, θ_blip)

  detection = abs(θ_blip - θ_sweep) < threshold
            OR
  Using dot product:
    sweepDir = (cos(θ_sweep), sin(θ_sweep))
    blipDir  = normalize(blip - center)
    dot = sweepDir · blipDir
    if dot > 0.98 → sweep is touching blip → show blip
```

---

## 🎨 Compose Code

### Full Radar Implementation

```kotlin
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.*

data class RadarBlip(
    val position: Offset,   // world position (relative to radar center)
    var alpha: Float = 0f,  // 0 = invisible, 1 = fully visible
    var label: String = ""
)

@Composable
fun RadarScreenDemo() {
    // Rotating sweep angle
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    // Blips — fixed positions in polar coords (radius, angle)
    val blips = remember {
        listOf(
            RadarBlip(Offset(120f, 60f),  label = "A"),
            RadarBlip(Offset(-90f, -40f), label = "B"),
            RadarBlip(Offset(30f, 130f),  label = "C"),
            RadarBlip(Offset(-140f, 70f), label = "D"),
            RadarBlip(Offset(80f, -100f), label = "E"),
        ).toMutableList()
    }

    // Fade blips each frame
    LaunchedEffect(sweepAngle) {
        blips.forEachIndexed { i, blip ->
            // Detection: dot product between sweep direction and blip direction
            val sweepDir = Offset(cos(sweepAngle), sin(sweepAngle))
            val blipDir  = blip.position.normalize()
            val dot = sweepDir.dot(blipDir)

            if (dot > 0.97f) {
                // Sweep just hit this blip — light it up
                blips[i] = blip.copy(alpha = 1f)
            } else {
                // Gradually fade
                blips[i] = blip.copy(alpha = (blip.alpha - 0.003f).coerceAtLeast(0f))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030A03)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(320.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radarRadius = size.minDimension / 2f - 10f

            // ── Radar background ────────────────────────────────────────
            drawCircle(Color(0xFF030A03), radarRadius, Offset(cx, cy))

            // ── Range rings ─────────────────────────────────────────────
            val ringColor = Color(0xFF004400)
            for (i in 1..4) {
                drawCircle(ringColor, radarRadius * (i / 4f), Offset(cx, cy),
                    style = Stroke(1f))
            }

            // ── Cross hairs ─────────────────────────────────────────────
            drawLine(ringColor, Offset(cx - radarRadius, cy), Offset(cx + radarRadius, cy), 1f)
            drawLine(ringColor, Offset(cx, cy - radarRadius), Offset(cx, cy + radarRadius), 1f)

            // ── Sweep trail (sector fill fading behind sweep) ────────────
            val trailSweep = 60f  // degrees of trail
            val brush = Brush.sweepGradient(
                colorStops = arrayOf(
                    0.0f to Color.Transparent,
                    0.7f to Color(0x2200FF00),
                    1.0f to Color(0x6600FF00)
                ),
                center = Offset(cx, cy)
            )
            // Rotate canvas to align gradient with sweep direction
            rotate(
                degrees = sweepAngle * (180f / PI.toFloat()) - trailSweep,
                pivot = Offset(cx, cy)
            ) {
                drawArc(
                    brush = brush,
                    startAngle = 0f,
                    sweepAngle = trailSweep,
                    useCenter = true,
                    topLeft = Offset(cx - radarRadius, cy - radarRadius),
                    size = androidx.compose.ui.geometry.Size(radarRadius * 2f, radarRadius * 2f)
                )
            }

            // ── Sweep line ───────────────────────────────────────────────
            val sweepEndX = cx + radarRadius * cos(sweepAngle)
            val sweepEndY = cy + radarRadius * sin(sweepAngle)
            drawLine(
                color = Color(0xFF00FF00),
                start = Offset(cx, cy),
                end = Offset(sweepEndX, sweepEndY),
                strokeWidth = 2f
            )

            // ── Blips ────────────────────────────────────────────────────
            blips.forEach { blip ->
                if (blip.alpha > 0f) {
                    val blipX = cx + blip.position.x
                    val blipY = cy + blip.position.y
                    // Outer glow
                    drawCircle(
                        color = Color(0xFF00FF00).copy(alpha = blip.alpha * 0.3f),
                        radius = 12f,
                        center = Offset(blipX, blipY)
                    )
                    // Core
                    drawCircle(
                        color = Color(0xFF00FF00).copy(alpha = blip.alpha),
                        radius = 5f,
                        center = Offset(blipX, blipY)
                    )
                }
            }

            // ── Tick marks on rim ────────────────────────────────────────
            for (i in 0 until 36) {
                val tickAngle = (i.toFloat() / 36f) * (2 * PI).toFloat()
                val inner = radarRadius * 0.93f
                drawLine(
                    color = Color(0xFF005500),
                    start = Offset(cx + inner * cos(tickAngle), cy + inner * sin(tickAngle)),
                    end   = Offset(cx + radarRadius * cos(tickAngle), cy + radarRadius * sin(tickAngle)),
                    strokeWidth = if (i % 9 == 0) 2f else 1f
                )
            }

            // ── Outer rim ────────────────────────────────────────────────
            drawCircle(Color(0xFF00AA00), radarRadius, Offset(cx, cy), style = Stroke(2f))

            // ── Center dot ───────────────────────────────────────────────
            drawCircle(Color(0xFF00FF00), 4f, Offset(cx, cy))
        }
    }
}

// Vector helpers
fun Offset.magnitude(): Float = sqrt(x * x + y * y)
fun Offset.normalize(): Offset {
    val m = magnitude()
    return if (m < 0.0001f) Offset.Zero else Offset(x / m, y / m)
}
fun Offset.dot(other: Offset): Float = x * other.x + y * other.y
```

---

## 🧪 Trace

**Is blip at position (120, 60) detected when sweep angle = 0.46 rad?**

```
Step 1: Blip polar direction
  blipDir = normalize(120, 60)
  |blip| = sqrt(120² + 60²) = sqrt(14400 + 3600) = sqrt(18000) ≈ 134.16
  blipDir = (120/134.16, 60/134.16) = (0.894, 0.447)

Step 2: Sweep direction
  sweepDir = (cos(0.46), sin(0.46)) = (0.895, 0.443)

Step 3: Dot product
  dot = 0.894 × 0.895 + 0.447 × 0.443
      = 0.800 + 0.198
      = 0.998

dot = 0.998 > 0.97 → DETECTED ✓ → blip lights up
```

---

## 📋 Phase 2 Complete — Vector Toolkit

| Concept | You Can Now… |
|---------|-------------|
| Vector = direction + magnitude | Represent forces, velocities, positions |
| Add / Subtract | Combine forces, find direction between points |
| Scale | Control speed independently from direction |
| Dot Product | Measure alignment, detect when sweep hits blip |
| Normalize | Pure direction — homing, look-at, targeting |
| **Radar** | Build full sensor sweep with detection logic |

---

## 🔗 Next Phase

→ [[Phase 3 — Coordinate Transforms/01 Translation]] — *How to move the entire coordinate system — the key to planetary orbits and hierarchical transforms*
