# AnimationSpec — The Physics Engine

> [!NOTE]
> `AnimationSpec` is the **recipe** for HOW an animation moves. The same animation from A to B can feel bouncy, linear, elastic, or instant — all depending on the `AnimationSpec`. This is where the "feel" of your app comes from.

---

## The Big Picture

```
animate*AsState(
    targetValue = 1f,
    animationSpec = ???  ← This is where you choose the physics
)
```

Every animation API accepts an `animationSpec`. If you don't provide one, the default is `spring()`.

---

## AnimationSpec Types Overview

| Type | Metaphor | Key Trait |
|------|----------|-----------|
| `spring()` | Physical spring | Physics-based, no fixed duration |
| `tween()` | Stopwatch + curve | Fixed duration with easing |
| `keyframes {}` | Storyboard | Frame-by-frame checkpoints |
| `snap()` | Teleport | Zero duration, instant |
| `repeatable()` | Loop pedal | Loops a finite spec |
| `infiniteRepeatable()` | Infinite loop | Loops forever |

```text
  ┌─────────────────────────────────────────────────────────────────┐
  │                    SPRING DAMPING RATIOS                        │
  │                                                                 │
  │  HighBouncy (0.2f):   ~~∿∿∿∿───── (Many oscillations)           │
  │                                                                 │
  │  MediumBouncy (0.5f): ~~∿∿─────── (Few oscillations)            │
  │                                                                 │
  │  LowBouncy (0.75f):   ~∿───────── (Single small overshoot)      │
  │                                                                 │
  │  NoBouncy (1.0f):     ─────────── (Smooth settle, no bounce)    │
  └─────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────┐
  │                    SPEC TYPES CURVE PLOTS                       │
  │                                                                 │
  │  Spring:  ~~~~∿∿∿∿─────  (Physics-based bounce to target)       │
  │                                                                 │
  │  Tween:   ╱‾‾‾‾‾‾‾‾‾‾‾‾  (Decelerates smoothly via Easing)      │
  │                                                                 │
  │  Keyframe: ╱╲__╱‾‾‾‾‾‾‾  (Precise segment timeline values)      │
  │                                                                 │
  │  Snap:    |____________  (Instant jump, 0ms duration)           │
  └─────────────────────────────────────────────────────────────────┘
```

---

## 1. `spring()` — Physics-Based

The default. Simulates a **real spring** — naturally accelerates, may overshoot, then settles.

```kotlin
spring(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMedium,
    visibilityThreshold: T? = null   // Stop when within this threshold
)
```

### dampingRatio — Controls bounciness

| Constant | Value | Behavior |
|----------|-------|----------|
| `Spring.DampingRatioHighBouncy` | 0.2f | Very bouncy, many oscillations |
| `Spring.DampingRatioMediumBouncy` | 0.5f | Moderate bounce |
| `Spring.DampingRatioLowBouncy` | 0.75f | Slight single bounce |
| `Spring.DampingRatioNoBouncy` | 1.0f | No bounce — smooth settle |

> `dampingRatio < 1` → underdamped (bounces)
> `dampingRatio = 1` → critically damped (no bounce, fastest settle without overshoot)
> `dampingRatio > 1` → overdamped (no bounce, slower than critical)

### stiffness — Controls speed

| Constant | Value | Behavior |
|----------|-------|----------|
| `Spring.StiffnessHigh` | 10_000f | Very fast, snappy |
| `Spring.StiffnessMedium` | 4_000f | Default speed |
| `Spring.StiffnessMediumLow` | 400f | Relaxed |
| `Spring.StiffnessLow` | 200f | Slow, floaty |
| `Spring.StiffnessVeryLow` | 50f | Very slow, dreamy |

### Examples

```kotlin
// Default: no bounce, medium speed
spring()

// Bouncy button press
spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

// Slow, dreamy float
spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessVeryLow
)

// Very bouncy, fast (great for FAB)
spring(
    dampingRatio = Spring.DampingRatioHighBouncy,
    stiffness = Spring.StiffnessHigh
)
```

> [!IMPORTANT]
> Spring has **no fixed duration** — it's physics-based. The `dampingRatio` and `stiffness` determine how long it takes to settle. This means the same spring feels the same regardless of how large the value change is.

---

## 2. `tween()` — Time-Based

Goes from A to B in **exactly** the duration you specify, following an easing curve.

```kotlin
tween(
    durationMillis: Int = AnimationConstants.DefaultDurationMillis,  // 300ms
    delayMillis: Int = 0,       // Wait before starting
    easing: Easing = FastOutSlowIn  // The curve
)
```

### Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `durationMillis` | 300 | How long the animation takes |
| `delayMillis` | 0 | Wait before starting |
| `easing` | `FastOutSlowIn` | The acceleration curve |

### Examples

```kotlin
// Standard 300ms
tween()

// Slow 1 second fade
tween(durationMillis = 1000, easing = LinearEasing)

// Fast, responsive
tween(durationMillis = 150, easing = FastOutSlowIn)

// With delay (for staggered animations)
tween(durationMillis = 300, delayMillis = 200)

// Linear (constant speed — feels robotic)
tween(durationMillis = 500, easing = Linear)
```

### Common Easing Values

| Easing | Feel | Use For |
|--------|------|---------|
| `FastOutSlowIn` | Fast start, slow end | Most UI transitions (default) |
| `LinearOutSlowIn` | Linear start, slow end | Elements entering screen |
| `FastOutLinearIn` | Fast start, linear end | Elements leaving screen |
| `Linear` | Constant speed | Progress indicators, clocks |
| `EaseInOut` | Slow-fast-slow | Gentle, balanced |
| `EaseIn` | Slow start | Elements accelerating |
| `EaseOut` | Slow end | Elements decelerating |

---

## 3. `keyframes {}` — Frame-by-Frame Control

Defines **exact values at specific time points** during the animation.

```kotlin
keyframes {
    durationMillis = 1000      // Total duration
    
    // value at timepoint with `at` infix
    0.5f at 200 with LinearEasing    // At 200ms: value is 0.5, using Linear up to next keyframe
    0.8f at 600                       // At 600ms: value is 0.8
    1.2f at 800 with FastOutSlowIn   // At 800ms: overshoot to 1.2
    // At 1000ms (end): goes to targetValue (specified by the animation)
}
```

### Syntax

```kotlin
keyframes<Float> {
    durationMillis = 500
    delayMillis = 0
    
    // value at time [with easing_for_segment]
    0f at 0             // Start at 0
    0.8f at 200         // Jump to 0.8 at 200ms
    0.6f at 350         // Pull back to 0.6 at 350ms
    1f at 500           // Settle at 1f at 500ms
}
```

### Use Case: Custom Bounce

```kotlin
val scale by animateFloatAsState(
    targetValue = 1f,
    animationSpec = keyframes {
        durationMillis = 500
        1f at 0
        1.3f at 100 with FastOutSlowIn     // Overshoot
        0.9f at 250 with LinearEasing      // Under-shoot
        1.05f at 380                        // Small bounce
        1f at 500                           // Settle
    }
)
```

### When to use `keyframes`:
- When you need **precise timing** control
- When you want custom **overshoot/undershoot** patterns
- When `spring` isn't giving you the exact feel you need
- For **character animation** or specific brand motion

---

## 4. `snap()` — Instant Jump

Zero duration — value **instantly teleports** to target.

```kotlin
snap(
    delayMillis: Int = 0  // Optional delay before snapping
)
```

### When to use:
- Initial state (no animation on first render)
- Testing
- User prefers reduced motion (accessibility)
- Inside `Animatable.snapTo()` for gesture sync

```kotlin
// Disable animation completely
val alpha by animateFloatAsState(
    targetValue = if (isVisible) 1f else 0f,
    animationSpec = if (prefersReducedMotion) snap() else tween(300)
)
```

---

## 5. `repeatable()` — Finite Loop

Repeats an animation spec a **fixed number of times**.

```kotlin
repeatable(
    iterations: Int,                    // How many times to repeat
    animation: DurationBasedAnimationSpec<T>,  // The spec to repeat (tween or keyframes)
    repeatMode: RepeatMode = RepeatMode.Restart  // How to repeat
)
```

### repeatMode

| Mode | Behavior |
|------|----------|
| `RepeatMode.Restart` | Plays → restarts from beginning → plays → ... |
| `RepeatMode.Reverse` | Plays → reverses → plays forward → reverses → ... |

```kotlin
// 3 shakes
repeatable(
    iterations = 3,
    animation = tween(100),
    repeatMode = RepeatMode.Reverse
)

// 5 pulses
repeatable(
    iterations = 5,
    animation = tween(600),
    repeatMode = RepeatMode.Restart
)
```

> [!WARNING]
> `repeatable()` only works with **duration-based** specs (`tween`, `keyframes`). Not with `spring()` (which has no duration).

---

## 6. `infiniteRepeatable()` — Forever Loop

Same as `repeatable()` but loops **forever**. Used with `rememberInfiniteTransition`.

```kotlin
infiniteRepeatable(
    animation: DurationBasedAnimationSpec<T>,
    repeatMode: RepeatMode = RepeatMode.Restart,
    initialStartOffset: StartOffset = StartOffset(0)  // Offset start time
)
```

### initialStartOffset

```kotlin
infiniteRepeatable(
    animation = tween(1000),
    initialStartOffset = StartOffset(
        offsetMillis = 500,        // Start 500ms into the animation
        offsetType = StartOffsetType.Delay  // Delay or FastForward
    )
)
```

`StartOffsetType.Delay` — waits before starting
`StartOffsetType.FastForward` — starts as if it's already been running for `offsetMillis`

### Examples

```kotlin
// Spinning loader
val angle by rememberInfiniteTransition().animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
        animation = tween(1000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    )
)

// Pulse
val scale by rememberInfiniteTransition().animateFloat(
    initialValue = 1f,
    targetValue = 1.2f,
    animationSpec = infiniteRepeatable(
        animation = tween(600),
        repeatMode = RepeatMode.Reverse  // Grows then shrinks
    )
)
```

---

## Choosing the Right Spec

```
Is it gesture-driven?
  → Use spring() — natural velocity continuation

Do you need exact duration?
  → Use tween()

Do you need specific mid-points?
  → Use keyframes {}

Do you need it instant?
  → Use snap()

Does it loop a finite number of times?
  → Use repeatable()

Does it loop forever?
  → Use infiniteRepeatable()
```

---

## Comparison Table

| | `spring` | `tween` | `keyframes` | `snap` | `repeatable` |
|--|---------|---------|-------------|--------|--------------|
| Duration | Physics (variable) | Fixed | Fixed | 0 | Fixed × N |
| Bounce | ✅ (dampingRatio) | ❌ | Manual | ❌ | ❌ |
| Easing | N/A | ✅ | Per-segment | N/A | Inherits |
| Precise timing | ❌ | Duration only | ✅ | N/A | N/A |
| Gesture-friendly | ✅ | ❌ | ❌ | ✅ | ❌ |
| Loops | ❌ | ❌ | ❌ | ❌ | ✅ |

---

## 💡 Interview Q&A

**Q: Why does `spring()` not have a `durationMillis` parameter?**
A: Spring is physics-based — the duration is determined by the physics (stiffness + damping + distance), not a fixed time. This makes it feel natural because the same spring behaves consistently regardless of the value range.

**Q: What's the difference between `repeatable` and `infiniteRepeatable`?**
A: `repeatable(iterations = N)` loops N times then stops. `infiniteRepeatable` loops forever. Both only work with duration-based specs.

**Q: What's `RepeatMode.Reverse`?**
A: The animation plays forward, then plays backward (reverse), then forward again. Creates a ping-pong effect — great for pulse/breathing animations.

---

**Next:** [[07 Animation/07 Easing/02 Built-in Easings|Built-in Easing Functions →]]
