# 📖 Animation Glossary — Every Term You Must Know

> [!NOTE]
> Before you use any animation API, you need to speak the language. If you see a rotating circle and don't know it's called **"rotation around the Z-axis"**, you'll never find the right API. This glossary is your dictionary.

---

## 🔤 A — B

### Alpha
The **opacity/transparency** of a composable.
- Range: `0f` (fully transparent / invisible) → `1f` (fully opaque / visible)
- API: `Modifier.alpha(value)` or via `graphicsLayer { alpha = ... }`
- Example: A fade-in animation goes from alpha `0f` → `1f`

### Anchor
A **fixed position** that a draggable element snaps to.
- Like drawer states: OPEN, HALF, CLOSED are anchors
- API: `AnchoredDraggable`

### Animation
A **smooth transition of a value over time**, driven by physics or timing.
- Not a video — it's a live calculation at 60/120 fps

### AnimationSpec
The **recipe** that describes how an animation moves — its duration, physics, and curve.
- Think of it as: "HOW does this value change over time?"
- Types: `tween`, `spring`, `keyframes`, `snap`, `repeatable`

### AnimationVector
The internal representation of any animatable value as a vector of floats.
- `Float` → `AnimationVector1D`
- `Dp` → `AnimationVector1D`
- `Color` → `AnimationVector4D` (ARGB)
- `Offset` → `AnimationVector2D`

---

## 🔤 C

### Canvas
A **raw drawing surface** in Compose where you draw shapes, paths, and lines using `DrawScope`.
- Animations here are driven by `withFrameNanos` or `Animatable`

### Choreographer
Android's system mechanism that **syncs drawing to the display's refresh rate** (60Hz, 120Hz).
- Compose animations are internally tied to the Choreographer

### Clip
**Cutting off** parts of a composable so they don't show outside a defined boundary.
- `Modifier.clip(shape)` — e.g., clip a Box to a circle shape
- Used in transitions where content slides in/out

### Compositing
Combining multiple **rendered layers** into a single frame.
- `graphicsLayer` creates a new compositing layer

### ContentTransform
The pair of `EnterTransition + ExitTransition` used in `AnimatedContent`.
- Example: `slideInVertically() + fadeIn() with fadeOut()`

### Coroutine (Animation)
Animations using `Animatable.animateTo()` are **suspending** — they live in a coroutine and can be cancelled/awaited.

---

## 🔤 D

### Damping Ratio
Controls **how bouncy** a spring animation is.
- `DampingRatioHighBouncy` = lots of bounce (like a rubber ball)
- `DampingRatioNoBouncy` = no bounce (smooth landing)
- Range: `0f` (never stops) → `1f` (no oscillation)

| Constant | Value | Feel |
|----------|-------|------|
| `DampingRatioHighBouncy` | 0.2f | Very bouncy |
| `DampingRatioMediumBouncy` | 0.5f | Some bounce |
| `DampingRatioLowBouncy` | 0.75f | Slight bounce |
| `DampingRatioNoBouncy` | 1.0f | No bounce |

### Delay
How many **milliseconds to wait** before the animation starts.
- Used in `tween(delayMillis = 300)`
- Useful for **staggered animations** (each item starts slightly later)

### Duration
How many **milliseconds** the animation takes to complete.
- Used in `tween(durationMillis = 500)`
- Default for `tween` is `AnimationConstants.DefaultDurationMillis` = 300ms

---

## 🔤 E

### Easing
The **curve** that controls how an animation accelerates and decelerates.
- Without easing: animation moves at constant speed → feels robotic 🤖
- With easing: animation speeds up, slows down → feels natural 🌊

> [!TIP]
> Real-world objects don't move at constant speed. A ball thrown into the air slows down, stops, then accelerates down. Easing replicates this.

| Easing | Meaning |
|--------|---------|
| `FastOutSlowIn` | Fast start, slow end (most common for UI) |
| `LinearOutSlowIn` | Linear start, slow end (for entering) |
| `FastOutLinearIn` | Fast start, linear end (for exiting) |
| `Linear` | Constant speed (robotic) |
| `EaseIn` | Starts slow, ends fast |
| `EaseOut` | Starts fast, ends slow |
| `EaseInOut` | Slow-fast-slow |
| `EaseInBounce` | Bounces at start |
| `EaseOutBounce` | Bounces at end |
| `EaseInOutElastic` | Elastic snap both ways |

### Enter Transition
How a composable **appears** on screen — slide in, fade in, scale in, etc.
- Used in `AnimatedVisibility(enter = ...)`

### Exit Transition
How a composable **disappears** from screen.
- Used in `AnimatedVisibility(exit = ...)`

---

## 🔤 F

### Fade
**Changing alpha** from 0 to 1 (in) or 1 to 0 (out).
- `fadeIn()` / `fadeOut()` as enter/exit transitions
- `animateFloatAsState(0f → 1f)` applied to `Modifier.alpha()`

### Float (Animation target)
Most animation values are ultimately `Float` under the hood.
- Even `Color`, `Dp`, `Offset` are converted to float vectors internally

### FPS (Frames Per Second)
How many frames are rendered per second.
- Standard: 60 fps (1 frame every 16ms)
- High refresh: 120 fps (1 frame every 8ms)
- Compose animations target the device's refresh rate automatically

### Frame
**A single rendered snapshot** of the UI at a specific moment in time.
- At 60fps: 60 frames per second, each 16.67ms apart

### Fling
A gesture where the user **lifts their finger while dragging fast**, and the UI continues moving based on **velocity** before decelerating.
- Used in scrolling, pager swiping
- API: `VelocityTracker`, `DecayAnimation`

---

## 🔤 G — H

### graphicsLayer
A Compose modifier that applies **visual transformations without affecting layout**.
- `scaleX`, `scaleY`, `rotationZ`, `translationX`, `translationY`, `alpha`
- **Does NOT trigger recomposition** during animation → performance win 🚀

> [!IMPORTANT]
> Use `graphicsLayer` for scale, rotation, translation, alpha animations — NEVER trigger layout recalculation for these.

### Gesture
A **user touch input** — tap, drag, swipe, pinch.
- Gestures are the **input** that drives animations

---

## 🔤 I

### Infinite Animation
An animation that **loops forever**.
- API: `rememberInfiniteTransition()`
- Good for: loading spinners, pulse effects, shimmer

### Initial Value
The **starting point** of an animation.
- In `animateFloatAsState`, the initial value is the current state value before it changes

### Interpolation
The mathematical process of **calculating intermediate values** between start and end.
- At 50% through an animation: `interpolatedValue = start + (end - start) * 0.5`
- Easing modifies how the `0.5` (progress) is calculated

### isRunning
A property on `Animatable` that tells you if an animation is currently in progress.

---

## 🔤 K — L

### Keyframe
A **specific moment in time** in an animation where you define an exact value.
- Like a "checkpoint": "at 30% through, the value must be exactly 0.8f"
- API: `keyframes { }`

### Layer
A **compositing layer** created by `graphicsLayer`. Animations on a layer don't require layout recalculation.

---

## 🔤 M

### Morphing
**Changing the shape** of a composable over time.
- Example: Circle → Rounded Rectangle
- Done via animating `cornerRadius` in a `graphicsLayer` or via `GenericShape`

---

## 🔤 O

### Offset
The **position displacement** of a composable from its original position.
- `Offset(x, y)` — `x` is horizontal, `y` is vertical (positive Y = down on screen)
- `Modifier.offset(x, y)` — physical layout offset (triggers layout)
- `graphicsLayer { translationX = x; translationY = y }` — visual only (no layout)

> [!WARNING]
> `Modifier.offset()` causes layout recalculation every frame — expensive.
> `graphicsLayer { translationX }` skips layout — cheap ✅

### Oscillation
The **back-and-forth movement** of a spring animation before it settles.
- High bouncy spring = many oscillations
- No bouncy spring = zero oscillations

---

## 🔤 P

### Phase (Compose)
Compose has 3 phases: **Composition → Layout → Drawing**.
- Most animations happen at the Drawing phase (cheapest)
- `animateContentSize` happens at Layout phase (more expensive)

### Progress
The **completion percentage** of an animation, from `0.0` (start) to `1.0` (end).
- Easing transforms raw progress (linear 0→1) into a curved value

---

## 🔤 R

### Recomposition
The process of Compose **re-running your composable function** when its state changes.
- Animations should avoid triggering recomposition every frame

### Rotation
Rotating a composable:
- **X-axis**: Flip like a book page (top-to-bottom flip)
- **Y-axis**: Flip like a card (left-to-right flip)  
- **Z-axis**: Spin flat on screen (clockwise/counter-clockwise)
- API: `graphicsLayer { rotationX, rotationY, rotationZ }`

### RepeatMode
Controls how a repeating animation behaves:
- `RepeatMode.Restart` — plays from start every time (→ → →)
- `RepeatMode.Reverse` — plays forward then backward (→ ← → ←)

---

## 🔤 S

### Scale
Resizing a composable:
- `scaleX` — horizontal scale (1.0 = normal, 0.5 = half width, 2.0 = double)
- `scaleY` — vertical scale
- `scale` — uniform scale (same for both axes)
- Does NOT change layout size — only visual size

### Slide
Moving a composable **into or out of** the screen.
- `slideInVertically()`, `slideInHorizontally()` as enter transitions
- `slideOutVertically()`, `slideOutHorizontally()` as exit transitions

### Snap
An **instant** animation with zero duration — jumps to value immediately.
- API: `snap()` as AnimationSpec, or `Animatable.snapTo(value)`

### Spring
A physics-based animation that **simulates a physical spring**.
- Defined by: `dampingRatio` (bounciness) and `stiffness` (speed)
- Unlike `tween`, has no fixed duration — settles naturally

### Staggered
When items in a list each **start their animation with a slight delay** after the previous.
- Creates a cascade/wave effect
- Done by: `LaunchedEffect(index) { delay(index * 50L); ... }`

### Stiffness
Controls **how fast** a spring animation moves toward its target.
- High stiffness = faster, snappier
- Low stiffness = slower, floatier

| Constant | Value | Feel |
|----------|-------|------|
| `StiffnessHigh` | 10_000f | Very snappy |
| `StiffnessMedium` | 4_000f | Default |
| `StiffnessMediumLow` | 400f | Relaxed |
| `StiffnessLow` | 200f | Slow |
| `StiffnessVeryLow` | 50f | Very slow |

---

## 🔤 T

### Target Value
The **destination** of an animation — the value it's trying to reach.
- In `animateFloatAsState(targetValue = 1f)`, `1f` is the target

### Translation
**Moving** a composable visually without changing layout.
- `graphicsLayer { translationX = 100f; translationY = 50f }`
- Different from `Modifier.offset()` which affects layout

### Tween
A **time-based animation** that goes from A to B in a set duration with an easing curve.
- Short for "in-between" — calculates all frames between start and end

---

## 🔤 V

### Velocity
**How fast** a value is changing at a specific moment.
- Measured in `units/second` (e.g., `px/s`)
- Used in fling: the finger's velocity when released determines how far the fling goes
- API: `VelocityTracker`

### Vector (Animation)
Internally, all animated values are represented as float vectors:
- `AnimationVector1D` — a single float (Float, Dp, Int)
- `AnimationVector2D` — two floats (Offset, IntOffset)
- `AnimationVector4D` — four floats (Color = A, R, G, B)

---

## 🔤 Z

### Z-axis Rotation
Spinning a composable **flat on screen** (like a clock hand or loading spinner).
- `graphicsLayer { rotationZ = degrees }`
- 0° = pointing right, 90° = pointing down, 180° = pointing left, 270° = pointing up

---

## 🗺️ Visual Term Map

```
Screen
┌────────────────────────────────┐
│  ← translationX →              │
│  ↑ translationY                │
│         ┌───────┐              │
│         │   ⟳   │ ← rotationZ │
│         │ scale │              │
│         └───────┘              │
│              ↓                 │
│         alpha (0–1)            │
└────────────────────────────────┘

Spring:  ~~~~∿∿∿∿─────  (oscillates then settles)
Tween:   ╱‾‾‾‾‾‾‾‾‾‾   (follows easing curve)
Snap:    |____________   (instant jump)
```

---

## 🌍 Physics of Motion — How Real Things Move (and How to Code Them)

> [!IMPORTANT]
> This section answers: **"I can SEE how this thing moves — but what is it called, and how do I build it?"**
> If you can't name the motion, you'll never find the right API. Name it first. Then build it.

---

### The Core Problem

You see a leaf fall. You want to animate it. But you open Compose docs and see `spring()`, `tween()`, `dampingRatio`. You don't know where to start — because you don't know **what the leaf is doing, technically.**

This section maps **real-world physics vocabulary → animation vocabulary → Compose API**.

---

### 🔻 Free Fall (Gravity-Driven Descent)

**What it is physically:**
An object accelerates downward due to gravity (`9.8 m/s²`). With no air resistance, it gets faster every second. Speed increases linearly with time: `v = g × t`.

**What it looks like:**
- Starts slow (almost stationary)
- Gets faster and faster
- No slowdown — hits the ground at max speed

**Technical terms:** `free fall`, `gravitational acceleration`, `uniformly accelerated motion`

**In animation language:**
- **EaseIn** curve — starts slow, ends fast (matches gravity's acceleration profile)
- `translationY` increases faster over time

**Compose implementation:**
```kotlin
val fall by animateFloatAsState(
    targetValue = screenHeight,
    animationSpec = tween(
        durationMillis = 800,
        easing = EaseIn  // starts slow (top), ends fast (bottom) — just like gravity
    )
)
Modifier.graphicsLayer { translationY = fall }
```

---

### 🍃 Leaf Falling (Fluttering Descent)

**What it is physically:**
A leaf undergoes **gravity-driven descent** while simultaneously experiencing:
1. **Aerodynamic drag** — air resistance opposing downward motion (reduces terminal speed)
2. **Lateral drift** — horizontal displacement from air current
3. **Rotational motion** — leaf rotates around its center of mass due to asymmetric drag
4. **Oscillatory flutter** — leaf rocks side-to-side, alternating which face meets the air
5. **Terminal velocity** — falls at a constant final speed once drag equals gravity

**What it looks like:**
- Falls downward, but NOT straight
- Wobbles left-right while descending
- Rotates slowly, unpredictably
- Slows near the end (air resistance balances gravity)
- Unpredictable path — different every time

**Technical terms:** `aerodynamic flutter`, `oscillatory descent`, `rotational motion`, `terminal velocity`, `stochastic trajectory`

**In animation language:**
- **translationY** — primary downward movement (EaseOut curve, slows at end from drag)
- **translationX** — lateral oscillation (sine wave or randomized)
- **rotationZ** — rotation around Z-axis (slow, alternating)
- **Randomization** — each leaf has different amplitude and frequency

**Compose implementation:**
```kotlin
@Composable
fun FallingLeaf(screenHeight: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "leaf")

    // Primary: fall downward — slows at end (terminal velocity feel)
    val fallY by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = screenHeight,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowIn),
            repeatMode = RepeatMode.Restart
        ), label = "fallY"
    )

    // Secondary: drift left and right (oscillatory)
    val driftX by infiniteTransition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse  // oscillates back and forth
        ), label = "driftX"
    )

    // Tertiary: slow rotation (aerodynamic tumble)
    val rotation by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(20.dp)
            .graphicsLayer {
                translationY = fallY
                translationX = driftX
                rotationZ = rotation
            }
            .background(Color(0xFF4CAF50), RoundedCornerShape(50))
    )
}
```

> Key insight: `RepeatMode.Reverse` on `driftX` and `rotation` creates the oscillatory flutter. The drift oscillates at a different speed than the rotation — that mismatch creates the chaotic, natural look.

---

### 💧 Raindrop (Vertical Free Fall, High Terminal Velocity)

**What it is physically:**
Rain falls nearly vertically with very high terminal velocity (about 9 m/s). Very little horizontal drift (unless wind). Almost perfectly linear downward motion. Hits ground sharply.

**Technical terms:** `vertical free fall`, `high terminal velocity`, `near-linear descent`

**In animation language:**
- `translationY` animates at **constant speed** (LinearEasing)
- Minimal `translationX`
- Fast duration

**Compose:**
```kotlin
// Rain feels LINEAR — constant speed, no easing
tween(durationMillis = 300, easing = LinearEasing)
// translationY: top → bottom
// translationX: slight random offset per drop (wind)
```

---

### 🌊 River Flow (Laminar vs Turbulent Flow)

**What it is physically:**

**Laminar flow** (calm river): Water moves in parallel layers. Each particle follows a smooth, predictable path. No chaotic mixing. This is what a slow, deep river looks like.

**Turbulent flow** (rapids): Random, chaotic motion. Eddies, vortices, unpredictable velocities at every point. What fast water or waterfalls look like.

**Technical terms:**
- `laminar flow` — smooth, ordered, low Reynolds number
- `turbulent flow` — chaotic, eddies, high Reynolds number
- `Reynolds number` — predicts whether flow is laminar or turbulent

**In animation language:**
- **Laminar** → Smooth, constant speed, `LinearEasing`, `tween()`, predictable path
- **Turbulent** → Random offset changes every frame, `withFrameNanos`, randomized `translationX`/`translationY`, chaotic keyframes

**Compose (laminar — smooth scrolling list):**
```kotlin
// Smooth, predictable. LinearEasing = laminar
tween(durationMillis = 600, easing = LinearEasing)
```

**Compose (turbulent — particle bubbles):**
```kotlin
// Each particle has randomized deltas every frame
LaunchedEffect(Unit) {
    while (true) {
        withFrameNanos {
            x += (Random.nextFloat() - 0.5f) * turbulenceStrength
            y += (Random.nextFloat() - 0.5f) * turbulenceStrength
        }
    }
}
```

---

### 🌑 Meteoroid Entering Atmosphere (Ballistic + Rapid Deceleration)

**What it is physically:**
1. **Entry**: High velocity (~30,000 km/h), ballistic trajectory (straight line, gravity negligible over short arc)
2. **Atmospheric drag**: Friction with air molecules causes **extreme deceleration** — 100× faster than normal drag
3. **Heating**: Kinetic energy converts to heat (glow/trail)
4. **Deceleration**: Velocity drops exponentially — not linearly
5. **Result**: Either burns up (fades to zero) or slows to "dark flight" (falls like a stone)

**Technical terms:** `ballistic trajectory`, `aerodynamic drag`, `exponential deceleration`, `terminal velocity`, `ablation`

**In animation language:**
- Starts at very high translationX/Y velocity
- Decelerates exponentially — `EaseOut` or `FastOutSlowIn`
- Can use `animateDecay()` (exponential decay) for physics accuracy
- Fades in alpha as it burns (`alpha` decreasing while trailing)

**Compose:**
```kotlin
// Meteor: fast entry, exponential slowdown
val meteorX = remember { Animatable(0f) }
val meteorAlpha = remember { Animatable(1f) }

LaunchedEffect(Unit) {
    // Exponential decay — exactly like atmospheric drag
    launch { meteorX.animateDecay(initialVelocity = 2000f, animationSpec = exponentialDecay()) }
    // Fade out as it burns
    launch { meteorAlpha.animateTo(0f, tween(800, easing = FastOutLinearIn)) }
}
```

---

### 🏀 Bouncing Ball (Elastic Collision + Gravity)

**What it is physically:**
1. Ball falls under gravity (accelerates downward — `EaseIn`)
2. Hits surface — elastic collision — reverses velocity (very fast)
3. Rises, decelerating against gravity (`EaseOut`)
4. Each bounce loses energy (coefficient of restitution < 1)
5. Bounces get smaller and faster until ball comes to rest

**Technical terms:** `elastic collision`, `coefficient of restitution`, `kinetic energy loss`, `damped oscillation`

**In animation language:**
- `keyframes{}` to define each bounce arc precisely
- `translationY` oscillates with decreasing amplitude
- Each arc uses `EaseIn` (falling) then `EaseOut` (rising)

**Compose:**
```kotlin
val bounceY by animateFloatAsState(
    targetValue = 0f,
    animationSpec = keyframes {
        durationMillis = 1200
        0f at 0                           // Start: top
        200f at 200 with EaseIn           // Fall (accelerating down)
        0f at 400 with EaseOut            // First bounce (decelerating up)
        150f at 550 with EaseIn           // Second fall (smaller)
        0f at 700 with EaseOut            // Second bounce
        80f at 800 with EaseIn            // Third fall (smaller)
        0f at 900 with EaseOut            // Third bounce
        30f at 960 with EaseIn            // Almost stopped
        0f at 1000                         // Rest
    },
    label = "bounce"
)
Modifier.graphicsLayer { translationY = bounceY }
```

> This is what `EaseOutBounce` does automatically — but `keyframes{}` gives you precise control.

---

### 🔤 Newton's First Law in UI (Inertia / Fling)

**The law:** An object in motion stays in motion unless acted upon by a force.

**In UI:** When you fling a scrollable list and release your finger, **the list keeps moving** even though your finger is gone — then gradually decelerates (friction force).

**Technical terms:** `inertia`, `momentum`, `deceleration`, `friction`, `fling`

**In animation language:**
- Velocity at release → `initialVelocity` in Animatable
- Friction/air resistance → `exponentialDecay()` or `splineBasedDecay()`
- The object "wants to keep moving" → but decay slowly brings it to rest

**Compose:**
```kotlin
val offset = remember { Animatable(0f) }
val decay = rememberSplineBasedDecay<Float>()

// On gesture release:
scope.launch {
    offset.animateDecay(
        initialVelocity = velocityTracker.calculateVelocity().x,
        animationSpec = decay  // exponentially decelerates, like friction
    )
}
```

---

### 🌀 Pendulum (Simple Harmonic Motion)

**What it is physically:**
A pendulum swings back and forth. With no friction: oscillates forever at constant amplitude. With friction: oscillates with decreasing amplitude until it stops at center.

**Technical terms:** `simple harmonic motion (SHM)`, `period`, `amplitude`, `damped oscillation`, `restoring force`

- **Period** = time for one full swing
- **Amplitude** = how far it swings from center
- **Undamped** = oscillates forever (no friction)
- **Damped** = oscillates but loses energy each swing

**In animation language:**
- **Undamped pendulum** = `infiniteRepeatable(RepeatMode.Reverse)` — swings forever
- **Damped pendulum** = `spring(dampingRatio = 0.3f)` — oscillates then settles at center
- The `dampingRatio` IS the physical damping coefficient of a pendulum

**Compose:**
```kotlin
// Undamped pendulum — swings forever
val angle by infiniteTransition.animateFloat(
    initialValue = -30f,
    targetValue = 30f,
    animationSpec = infiniteRepeatable(
        animation = tween(1000, easing = EaseInOut),
        repeatMode = RepeatMode.Reverse  // like a pendulum
    ), label = "pendulum"
)

// Damped pendulum — oscillates then rests
val angle by animateFloatAsState(
    targetValue = 0f,  // resting position (center)
    animationSpec = spring(
        dampingRatio = 0.3f,   // low damping = many oscillations before rest
        stiffness = Spring.StiffnessLow
    ), label = "dampedPendulum"
)
```

---

### 💨 Smoke Rising (Turbulent Convection)

**What it is physically:**
Smoke rises due to **convection** (hot air is less dense, rises). As it rises, it loses heat and mixes with surrounding air → becomes **turbulent**. Path is chaotic: it billows, expands, and disperses.

**Technical terms:** `convection`, `buoyancy`, `turbulent diffusion`, `entrainment`, `dispersion`

**In animation language:**
- Primary upward `translationY` — steady rise (constant or EaseOut speed)
- Expanding `scale` as smoke spreads
- Decreasing `alpha` as it disperses
- Randomized `translationX` jitter for turbulence
- Multiple particles with staggered start offsets

**Compose:**
```kotlin
@Composable
fun SmokeParticle(index: Int) {
    val alpha = remember { Animatable(0.7f) }
    val scaleVal = remember { Animatable(0.3f) }
    val riseY = remember { Animatable(0f) }
    val driftX = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(index * 300L)  // stagger particles
        launch { alpha.animateTo(0f, tween(2000)) }           // fade out
        launch { scaleVal.animateTo(2f, tween(2000)) }         // expand
        launch { riseY.animateTo(-200f, tween(2000, easing = LinearEasing)) }  // rise
        launch {  // turbulent drift
            while (true) {
                driftX.animateTo(
                    (Random.nextFloat() - 0.5f) * 40f,
                    tween(400, easing = EaseInOut)
                )
            }
        }
    }

    Box(modifier = Modifier.graphicsLayer {
        alpha = alpha.value
        scaleX = scaleVal.value
        scaleY = scaleVal.value
        translationY = riseY.value
        translationX = driftX.value
    }.size(20.dp).background(Color.Gray, CircleShape))
}
```

---

### 🌐 Motion Vocabulary Master Table

| Real-World Motion | Physics Term | Key Properties | Compose Tool | AnimationSpec |
|-------------------|-------------|----------------|-------------|---------------|
| Ball dropped | Free fall | Constant acceleration ↓ | `translationY` | `tween(EaseIn)` |
| Leaf falling | Flutter descent | Gravity + drag + rotation + lateral oscillation | `translationY + X + rotationZ` | `tween + infiniteRepeatable(Reverse)` |
| Rain | Vertical linear fall | High terminal velocity, linear | `translationY` | `tween(LinearEasing)` |
| Bouncing ball | Elastic collision + gravity | Decreasing amplitude arcs | `translationY` | `keyframes{}` or `EaseOutBounce` |
| Pendulum swing | Simple harmonic motion | Oscillating at constant amplitude | `rotationZ` | `infiniteRepeatable(Reverse)` or `spring` |
| Spring pressed | Damped SHM | Oscillation + decay to rest | Any value | `spring(dampingRatio)` |
| River (calm) | Laminar flow | Smooth, predictable, constant | `translationX` | `tween(LinearEasing)` |
| River (rapid) | Turbulent flow | Chaotic, random, eddy motion | `translationX/Y` | `withFrameNanos + random` |
| Smoke rising | Convection + turbulence | Rise + expand + fade + jitter | Multiple `Animatable` | Multiple concurrent |
| Meteor entry | Ballistic + deceleration | Very fast → exponential slowdown | `translationX/Y` | `animateDecay(exponentialDecay)` |
| Door swinging | Rotational motion + pivot | Rotation around edge, not center | `rotationY + transformOrigin` | `spring` or `tween` |
| Rubber band snap | Elastic recoil | Overshoot past target, snap back | Any value | `spring(DampingRatioHighBouncy)` |
| Object slowing to stop | Deceleration / drag | Fast → slow → stop | `translationX` | `tween(EaseOut)` or `animateDecay` |
| Object accelerating | Acceleration | Slow → fast | `translationX` | `tween(EaseIn)` |
| Fling / scroll inertia | Newton's 1st law | Velocity carries on after release | `Animatable.animateDecay` | `splineBasedDecay` |
| Heartbeat / pulse | Rhythmic oscillation | Periodic scale change | `scale` | `infiniteRepeatable(Reverse)` |
| Feather falling | Very low terminal velocity | Slow, wide oscillation, gentle | Same as leaf | Slower duration, wider drift |
| Satellite orbit | Circular motion | Constant speed along circular path | `rotationZ` or `Offset` on circle | `infiniteRepeatable(LinearEasing)` |
| Shake / tremor | Rapid oscillation | Fast, small-amplitude alternating | `translationX` | `keyframes{}` rapid alternation |
| Rubber duck bobbing | Buoyancy oscillation | Gentle up-down at rest | `translationY` | `infiniteRepeatable(Reverse, EaseInOut)` |

---

### 🔬 The Physics → Code Mental Model

```
Step 1: OBSERVE the motion
        "This object falls fast, then bounces a few times, then rests"

Step 2: NAME the physics
        "Gravity-driven fall + elastic collision + damped oscillation"

Step 3: DECOMPOSE into axes
        "Y: falls (EaseIn), bounces (EaseOut×3)
         X: none
         Rotation: none"

Step 4: MAP to Compose tools
        "keyframes{} for multi-phase Y movement"
        "OR spring(dampingRatio=0.3) if I want natural settle"

Step 5: CODE it
        "animateFloatAsState(animationSpec = keyframes { ... })"
```

---

### 🧠 Key Physical Principles → Compose Constants

| Physical Concept | What it Controls | Compose Constant |
|-----------------|-----------------|-----------------|
| **High friction / heavy damping** | Less oscillation, smooth landing | `DampingRatioNoBouncy = 1.0f` |
| **Low friction / light damping** | More oscillations before rest | `DampingRatioHighBouncy = 0.2f` |
| **High stiffness** | Fast movement, snappy response | `StiffnessHigh = 10_000f` |
| **Low stiffness** | Slow movement, floaty response | `StiffnessVeryLow = 50f` |
| **Constant velocity** | No acceleration, no deceleration | `LinearEasing` |
| **Gravity (accelerating down)** | Starts slow, ends fast | `EaseIn` |
| **Air resistance (decelerating)** | Starts fast, ends slow | `EaseOut` |
| **Elastic collision** | Overshoot past target, spring back | `EaseOutBack`, `EaseOutElastic` |
| **Ball dropping multiple bounces** | Multiple arcs, each smaller | `EaseOutBounce` |
| **Inertia / fling** | Keeps moving after gesture | `animateDecay(exponentialDecay)` |

---

**Next:** [[07 Animation/02 Animation Decision Tree|🗺️ Decision Tree — Which API to Use When →]]
