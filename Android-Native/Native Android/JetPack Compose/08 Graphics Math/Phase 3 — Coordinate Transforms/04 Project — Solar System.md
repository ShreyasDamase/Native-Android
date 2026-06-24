# 04 — Project: Solar System

> **Phase 3 Capstone**: Combine translation + rotation + scale to build an animated solar system — Sun, 3 planets with different orbital speeds, one planet with a moon, and asteroid belt particles.

---

## 🧠 What We're Building

- **Sun** at the center with glow effect
- **Mercury, Earth, Mars** orbiting at different radii and speeds
- **Earth's Moon** orbiting Earth (nested translate)
- **Asteroid belt** — ring of tiny particles
- **Scale glow** on Sun that pulses (sin wave)

**Every position is computed with the same formula**: `translate → rotate → draw`

---

## 📐 Diagram

```
SOLAR SYSTEM LAYOUT:

         ┌───────────────────────────────────────────────────────┐
         │                                                       │
         │              Asteroid belt                            │
         │         ┌─────────────────────┐                      │
         │      ┌──┘    Mars orbit       └──┐                   │
         │    ┌─┘   ┌───────────────┐       └─┐                 │
         │   │    ┌─┘  Earth orbit  └─┐       │                 │
         │   │   │  ┌─────────────┐   │       │                 │
         │   │   │  │ Mercury orb │   │       │                 │
         │   │   │  │    ☀ Sun   │   │       │                 │
         │   │   │  └─────────────┘   │       │                 │
         │   │   │  ● Mercury         │       │                 │
         │   │    └─┐            ┌─┘─ ● Earth │                 │
         │    └─┐   └───────────┘     └─┐    │                 │
         │      └──┐                 ┌──┘    │                 │
         │         └─────────────────┘ ● Mars│                 │
         │                                   │                 │
         └───────────────────────────────────────────────────────┘

NESTING HIERARCHY:
  World
  └── Sun (center)
      ├── Mercury (orbit R=80, period=3s)
      ├── Earth   (orbit R=140, period=6s)
      │   └── Moon (orbit R=28, period=1.5s)
      ├── Mars    (orbit R=200, period=10s)
      └── Asteroid belt (orbit R=240±20, 60 particles)
```

---

## 🎨 Compose Code

### Full Solar System

```kotlin
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*

// Planet data class
data class Planet(
    val name: String,
    val orbitRadius: Float,
    val planetRadius: Float,
    val color: Color,
    val orbitPeriodMs: Int,
    val hasMoon: Boolean = false
)

val solarPlanets = listOf(
    Planet("Mercury", 80f,  8f,  Color(0xFFB5B5B5), 3000),
    Planet("Venus",   115f, 11f, Color(0xFFE8C47A), 5000),
    Planet("Earth",   155f, 12f, Color(0xFF4A9EFF), 8000, hasMoon = true),
    Planet("Mars",    200f, 9f,  Color(0xFFFF5733), 12000),
)

@Composable
fun SolarSystemDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "solar")

    // One angle animator per planet
    val mercuryAngle by infiniteTransition.animateFloat(0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "mercury")
    val venusAngle by infiniteTransition.animateFloat(0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "venus")
    val earthAngle by infiniteTransition.animateFloat(0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "earth")
    val marsAngle by infiniteTransition.animateFloat(0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "mars")
    val moonAngle by infiniteTransition.animateFloat(0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "moon")
    val sunPulse by infiniteTransition.animateFloat(0.9f, 1.1f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sunPulse")

    val planetAngles = listOf(mercuryAngle, venusAngle, earthAngle, marsAngle)

    // Asteroid positions — fixed, just orbit at varying speeds
    val asteroidAngles = remember {
        (0 until 60).map { i ->
            (i.toFloat() / 60f) * (2 * PI).toFloat()
        }
    }
    val asteroidSpeed by infiniteTransition.animateFloat(0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(20000, easing = LinearEasing)), label = "asteroid")

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030310))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // ── Starfield background ─────────────────────────────────────────
        // (simple static stars)
        val starPositions = remember {
            (0 until 80).map {
                Offset(
                    (Math.random() * 400).toFloat(),
                    (Math.random() * 700).toFloat()
                )
            }
        }
        starPositions.forEach { star ->
            drawCircle(Color(0xFFFFFFFF).copy(alpha = 0.3f + (Math.random() * 0.4f).toFloat()),
                1f, star)
        }

        // ── Orbit rings ──────────────────────────────────────────────────
        solarPlanets.forEach { planet ->
            drawCircle(Color(0xFF111133), planet.orbitRadius, Offset(cx, cy), style = Stroke(1f))
        }
        // Asteroid belt ring
        drawCircle(Color(0xFF111122), 240f, Offset(cx, cy), style = Stroke(18f))

        // ── Sun ──────────────────────────────────────────────────────────
        translate(cx, cy) {
            // Outer corona glow
            for (i in 5 downTo 1) {
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = 0.04f * i),
                    radius = 34f * sunPulse + i * 12f,
                    center = Offset.Zero
                )
            }
            // Sun body
            drawCircle(Color(0xFFFFD700), 30f * sunPulse, Offset.Zero)
            drawCircle(Color(0xFFFFA500), 22f * sunPulse, Offset.Zero)
            drawCircle(Color(0xFFFFFF80), 12f * sunPulse, Offset.Zero)

            // ── Planets ──────────────────────────────────────────────────
            solarPlanets.forEachIndexed { i, planet ->
                val angle = planetAngles[i]
                val planetX = planet.orbitRadius * cos(angle)
                val planetY = planet.orbitRadius * sin(angle)

                translate(planetX, planetY) {
                    // Planet glow
                    drawCircle(planet.color.copy(alpha = 0.2f),
                        planet.planetRadius * 1.8f, Offset.Zero)
                    // Planet body
                    drawCircle(planet.color, planet.planetRadius, Offset.Zero)

                    // Highlight
                    drawCircle(Color.White.copy(alpha = 0.4f),
                        planet.planetRadius * 0.35f,
                        Offset(-planet.planetRadius * 0.3f, -planet.planetRadius * 0.3f))

                    // Earth Moon
                    if (planet.hasMoon) {
                        val moonR = 28f
                        val moonX = moonR * cos(moonAngle)
                        val moonY = moonR * sin(moonAngle)
                        // Moon orbit ring
                        drawCircle(Color(0xFF222244), moonR, Offset.Zero, style = Stroke(0.5f))
                        translate(moonX, moonY) {
                            drawCircle(Color(0xFFCCCCCC), 4f, Offset.Zero)
                        }
                    }
                }
            }

            // ── Asteroid belt ────────────────────────────────────────────
            asteroidAngles.forEachIndexed { i, baseAngle ->
                val a = baseAngle + asteroidSpeed + (i * 0.1f)
                val r = 230f + (i % 5) * 4f
                val ax = r * cos(a)
                val ay = r * sin(a)
                drawCircle(
                    Color(0xFF888888).copy(alpha = 0.6f),
                    1.5f + (i % 3) * 0.5f,
                    Offset(ax, ay)
                )
            }
        }
    }
}
```

---

## 🧪 Trace

**Earth position at earthAngle = π/3 (60°), orbitRadius = 155**:

```
earthX = 155 × cos(π/3) = 155 × 0.5 = 77.5
earthY = 155 × sin(π/3) = 155 × 0.866 = 134.2
Earth world position = center + (77.5, 134.2)

Moon position at moonAngle = π (180°), moonOrbit = 28:
  After translating to Earth center:
  moonX = 28 × cos(π) = 28 × (-1) = -28
  moonY = 28 × sin(π) = 28 × 0    = 0
  Moon local = (-28, 0) → directly left of Earth
  Moon world = center + (77.5 - 28, 134.2 + 0) = center + (49.5, 134.2)
```

---

## 📋 Phase 3 Complete — Transform Toolkit

| Concept | You Can Now… |
|---------|-------------|
| Translation | Move the origin — hierarchical animation |
| Rotation | Spin around any pivot with cos/sin |
| Scale | Resize, squash+stretch, zoom |
| **Solar System** | Compose full TRS hierarchy: sun→planet→moon |

---

## 🔗 Next Phase

→ [[Phase 4 — Interpolation and Curves/01 Linear Interpolation (lerp)]] — *The math of blending between two values — the foundation of all animation transitions*
