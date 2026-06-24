# 01 — Translation

> **Core Idea**: Translation means moving the **origin point**. Instead of calculating every object's position relative to (0, 0), you temporarily move the center of the world to where you want — then draw as if at (0, 0). This unlocks hierarchical animation: moons orbit planets, planets orbit the sun.

---

## 🧠 Concept

Imagine you want to draw a moon orbiting a planet which is orbiting the sun.

**Without translation**: You must calculate the moon's world position manually.
```
moonWorldX = sunX + planetOrbitRadius × cos(planetAngle)
             + moonOrbitRadius × cos(moonAngle)
```
Messy. And if you add a third level (moon's satellite), it explodes in complexity.

**With translation**: You move the origin to the sun, draw the planet, move origin to the planet, draw the moon. Each object only knows its **local** position.

In Canvas: `translate(x, y)` moves the drawing origin. Everything drawn after it is relative to the new origin.

---

## 📐 Diagram

```
WITHOUT TRANSLATION — manual world coordinates:

  World (0,0)──────────────────────────────────────────
  │                                                    │
  │   Sun @ (300, 300)                                 │
  │       *                                            │
  │       │ planet orbit = 150px                       │
  │       │                                            │
  │       └───────────────────────────────* Planet     │
  │                                  @ (450, 300)      │
  │                                       │            │
  │                                       │ moon orbit │
  │                                       └─────────*  │
  │                                          Moon       │
  │                                      @ (510, 300)   │
  │                                                    │

WITH TRANSLATION — local coordinates:

  Step 1: translate to Sun center (300, 300)
    → origin is now at Sun
    Planet local position = (150, 0)
    draw planet at (150, 0)   ← simple!

  Step 2: translate to Planet center (origin now at Planet)
    Moon local position = (60, 0)
    draw moon at (60, 0)      ← simple!

  CANVAS translate() — additive!
    translate(300f, 300f)     ← origin at sun
    translate(150f, 0f)       ← origin at planet (300+150 = 450, 300)
    draw circle at (0, 0)     ← draws moon at (510, 300)
```

---

## 🔢 The Math

```kotlin
// Canvas DrawScope has built-in translate and rotate

// Manual translation (without DrawScope helpers):
fun translatePoint(point: Offset, translation: Offset): Offset =
    Offset(point.x + translation.x, point.y + translation.y)

// In Canvas:
Canvas(modifier) {
    translate(left = 300f, top = 300f) {
        // Everything inside is now relative to (300, 300)
        drawCircle(Color.Yellow, 30f, Offset.Zero)  // draws at world (300, 300)

        translate(left = 150f, top = 0f) {
            // Now relative to (450, 300)
            drawCircle(Color.Blue, 15f, Offset.Zero)  // draws at world (450, 300)
        }
    }
}
```

---

## 🎨 Compose Code

### Example 1: Nested translate — planet + moon

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

@Composable
fun PlanetMoonTranslateDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "solar")
    val planetAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "planet"
    )
    val moonAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "moon"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val planetOrbit = 180f
        val moonOrbit = 60f

        // Planet orbit ring
        drawCircle(Color(0xFF111133), planetOrbit, Offset(cx, cy), style = Stroke(1f))

        // ── Translate to Sun position ─────────────────────────────────
        translate(cx, cy) {
            // Draw Sun
            drawCircle(Color(0xFFFFD700), 28f, Offset.Zero)
            for (i in 1..3) {
                drawCircle(Color(0x22FFD700), i * 15f, Offset.Zero,
                    style = Stroke(3f))
            }

            // ── Translate to Planet position (relative to Sun) ────────
            val planetX = planetOrbit * cos(planetAngle)
            val planetY = planetOrbit * sin(planetAngle)
            translate(planetX, planetY) {
                // Moon orbit ring (relative to planet)
                drawCircle(Color(0xFF111133), moonOrbit, Offset.Zero, style = Stroke(1f))

                // Draw Planet
                drawCircle(Color(0xFF4A9EFF), 18f, Offset.Zero)

                // ── Translate to Moon position (relative to Planet) ───
                val moonX = moonOrbit * cos(moonAngle)
                val moonY = moonOrbit * sin(moonAngle)
                translate(moonX, moonY) {
                    // Draw Moon
                    drawCircle(Color(0xFFAAAAAA), 8f, Offset.Zero)
                }
            }
        }
    }
}
```

### Example 2: Translate vs manual position — side-by-side comparison

```kotlin
@Composable
fun TranslateVsManualDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "compare")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "angle"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val orbitR = 80f
        val moonR = 30f

        // LEFT SIDE: Manual calculation (verbose)
        val leftCenter = Offset(size.width * 0.25f, size.height / 2f)
        val planetX = leftCenter.x + orbitR * cos(angle)
        val planetY = leftCenter.y + orbitR * sin(angle)
        val moonX = planetX + moonR * cos(angle * 3f)
        val moonY = planetY + moonR * sin(angle * 3f)

        drawCircle(Color(0xFFFFD700), 18f, leftCenter)
        drawCircle(Color(0xFF4A9EFF), 12f, Offset(planetX, planetY))
        drawCircle(Color(0xFFAAAAAA), 6f, Offset(moonX, moonY))

        // RIGHT SIDE: Using translate (clean)
        val rightCenterX = size.width * 0.75f
        val rightCenterY = size.height / 2f

        translate(rightCenterX, rightCenterY) {
            // Sun
            drawCircle(Color(0xFFFFD700), 18f, Offset.Zero)

            // Planet (relative to sun)
            translate(orbitR * cos(angle), orbitR * sin(angle)) {
                drawCircle(Color(0xFF4A9EFF), 12f, Offset.Zero)

                // Moon (relative to planet)
                translate(moonR * cos(angle * 3f), moonR * sin(angle * 3f)) {
                    drawCircle(Color(0xFFAAAAAA), 6f, Offset.Zero)
                }
            }
        }

        // Divider
        drawLine(Color(0xFF2A2A4A), Offset(size.width / 2f, 0f),
            Offset(size.width / 2f, size.height), 1f)
    }
}
```

### Example 3: Moving object leaves a trail

```kotlin
@Composable
fun TrailDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "trail")
    val t by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "t"
    )

    // Trail: remember last N positions
    val trail = remember { ArrayDeque<Offset>(30) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val angle = t * (2 * Math.PI).toFloat()

        val pos = Offset(
            cx + 180f * cos(angle),
            cy + 80f * sin(angle * 2)  // figure-8 path
        )

        // Maintain trail
        if (trail.size >= 30) trail.removeFirst()
        trail.addLast(pos)

        // Draw trail
        trail.forEachIndexed { i, trailPos ->
            val alpha = i.toFloat() / trail.size
            drawCircle(
                color = Color(0xFF4A9EFF).copy(alpha = alpha * 0.6f),
                radius = 4f + 10f * alpha,
                center = trailPos
            )
        }

        // Ball
        drawCircle(Color(0xFFFF6B6B), 16f, pos)
    }
}
```

---

## 🧪 Trace

**Nested translation: Sun at world (300, 300), planet orbits at r=150, angle=π/4, moon orbits at r=60, angle=π/2**:

```
Step 1: translate to Sun
  world origin → (300, 300)

Step 2: planet position (local to Sun)
  planetLocalX = 150 × cos(π/4) = 150 × 0.707 = 106.1
  planetLocalY = 150 × sin(π/4) = 150 × 0.707 = 106.1
  Planet world = (300 + 106.1, 300 + 106.1) = (406, 406)

Step 3: translate to Planet
  world origin → (406, 406)

Step 4: moon position (local to Planet)
  moonLocalX = 60 × cos(π/2) = 60 × 0.0 = 0
  moonLocalY = 60 × sin(π/2) = 60 × 1.0 = 60
  Moon world = (406 + 0, 406 + 60) = (406, 466)
```

---

## 🔗 Next

→ [[02 Rotation]] — *How to rotate around any pivot point — the math that makes gears, wheels, and orbits work*
