# 〰️ Built-in Easing Functions — Complete Reference

> [!NOTE]
> Easing is the **curve** that shapes HOW an animation moves — it transforms linear progress (0→1) into a more natural, curved path. Without easing, animation feels mechanical. With the right easing, it feels alive.

---

## The Core Concept

```
Linear progress:    0 ──────────── 0.5 ──────────── 1.0  (constant speed)
With FastOutSlowIn: 0 ──────── 0.7 ──── 0.85 ─── 1.0   (fast start, slow end)
```

Easing is a **function**: takes progress `0.0 → 1.0`, returns modified progress `0.0 → 1.0`.

---

## Material Design 3 Easings (Core 4)

These are the primary easings recommended by Material Design:

| Easing | Use For | Feel |
|--------|---------|------|
| `FastOutSlowIn` | Most UI transitions (default) | Fast departure, slow arrival |
| `LinearOutSlowIn` | Elements ENTERING screen | Linear start, decelerate to rest |
| `FastOutLinearIn` | Elements LEAVING screen | Accelerate out, linear exit |
| `Linear` | Loaders, progress, clocks | Constant speed (no curve) |

```kotlin
tween(durationMillis = 300, easing = FastOutSlowInEasing)
tween(durationMillis = 400, easing = LinearOutSlowInEasing)  // for enter transitions
tween(durationMillis = 200, easing = FastOutLinearInEasing)  // for exit transitions
tween(durationMillis = 1000, easing = LinearEasing)          // spinner rotation
```

> [!TIP]
> **Material Rule**: Use `FastOutSlowIn` for elements staying on screen, `LinearOutSlowIn` for elements entering, `FastOutLinearIn` for elements exiting.

---

## Extended Easing Library

These are available in `androidx.compose.animation.core`:

### 🔵 EaseIn Variants — Slow Start, Fast End

| Easing | Curve Shape |
|--------|------------|
| `EaseIn` | Gradual acceleration |
| `EaseInQuad` | Quadratic — mild acceleration |
| `EaseInCubic` | Cubic — stronger acceleration |
| `EaseInQuart` | Quartic — very strong |
| `EaseInQuint` | Quintic — very strong |
| `EaseInSine` | Sinusoidal — gentle |
| `EaseInCirc` | Circular — sharp |
| `EaseInExpo` | Exponential — extreme |
| `EaseInBack` | Pulls back slightly before starting |
| `EaseInElastic` | Elastic snap at beginning |
| `EaseInBounce` | Bounces at the beginning |

### 🔴 EaseOut Variants — Fast Start, Slow End

| Easing | Best For |
|--------|---------|
| `EaseOut` | Standard deceleration |
| `EaseOutQuad` | Cards settling into place |
| `EaseOutCubic` | Natural landing |
| `EaseOutQuart` | Snappy arrival |
| `EaseOutQuint` | Very snappy arrival |
| `EaseOutSine` | Smooth exit |
| `EaseOutCirc` | Sharp deceleration |
| `EaseOutExpo` | Dramatic deceleration |
| `EaseOutBack` | Overshoots then settles back ← 🌟 Great for popups/dialogs |
| `EaseOutElastic` | Elastic bounce at end ← 🌟 Great for buttons, FABs |
| `EaseOutBounce` | Multiple bounces at end (ball drop effect) |

### 🟢 EaseInOut Variants — Slow Both Ends

| Easing | Best For |
|--------|---------|
| `EaseInOut` | Page transitions |
| `EaseInOutQuad` | Card expansions |
| `EaseInOutCubic` | Smooth state changes |
| `EaseInOutQuart` | Modal presentations |
| `EaseInOutQuint` | Premium feel |
| `EaseInOutSine` | Very smooth, gentle |
| `EaseInOutCirc` | Sharp edges |
| `EaseInOutExpo` | Dramatic |
| `EaseInOutBack` | Bounces slightly at both ends |
| `EaseInOutElastic` | Elastic both ways (dramatic) |
| `EaseInOutBounce` | Bounces both ends |

---

## Visual Reference Table

```text
  ┌─────────────────────────────────────────────────────────────────┐
  │                     VISUAL EASING CURVES                        │
  │                                                                 │
  │  Linear:                    EaseIn:                             │
  │  1.0 ┼      ╱               1.0 ┼         ╭‾                    │
  │      │    ╱                     │       ╱                       │
  │      │  ╱                       │     ╱                         │
  │  0.0 ┴╱──────► Time         0.0 ┴───╱────────► Time             │
  │                                                                 │
  │  EaseOut:                   EaseInOut:                          │
  │  1.0 ┼    ╱‾‾               1.0 ┼       ╭‾‾                     │
  │      │  ╱                       │     ╱                         │
  │      │╱                         │   ╱                           │
  │  0.0 ┴───────► Time         0.0 ┴──╯─────────► Time             │
  │                                                                 │
  │  EaseOutBack (Overshoot):   EaseOutBounce:                      │
  │  1.2 ┼      ╭─╮             1.0 ┼    ╱╲    ╭╮                   │
  │  1.0 ┼    ╱   ╰──               │  ╱    ╲╱  ╰──                 │
  │      │  ╱                       │╱                              │
  │  0.0 ┴─╱─────┴───► Time     0.0 ┴──────────────► Time           │
  └─────────────────────────────────────────────────────────────────┘
```

---

## CubicBezierEasing — Custom Curves

Create any custom easing curve using a cubic bezier with two control points.

```kotlin
CubicBezierEasing(
    a: Float,  // x1 (0.0 → 1.0)
    b: Float,  // y1 (can go outside 0-1 for overshoot)
    c: Float,  // x2 (0.0 → 1.0)
    d: Float   // y2 (can go outside 0-1 for overshoot)
)
```

> [!TIP]
> Use [cubic-bezier.com](https://cubic-bezier.com) to visually design your curve, then paste the four values.

### Common Custom Beziers

```kotlin
// Material Design M3 "Emphasized" easing
val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

// Material Design M3 "Emphasized Accelerate"
val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

// Material Design M3 "Emphasized Decelerate"
val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

// Elastic-feel
val ElasticOut = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

// Back overshoot
val BackOut = CubicBezierEasing(0.34f, 1.4f, 0.64f, 1f)

// Standard iOS/macOS feel
val AppleLike = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
```

---

## Custom Easing Function

Implement the `Easing` functional interface for full custom control:

```kotlin
val MyEasing = Easing { fraction ->
    // fraction is 0.0 → 1.0 (linear progress)
    // return your modified progress

    // Example: quadratic ease-in
    fraction * fraction

    // Example: bounce end (simplified)
    if (fraction < 0.9f) {
        (fraction / 0.9f) * 1.1f
    } else {
        1.1f - (fraction - 0.9f) / 0.1f * 0.1f
    }
}

tween(durationMillis = 500, easing = MyEasing)
```

---

## Easing in Practice: Decision Guide

```
"I want a natural UI transition"         → FastOutSlowIn (default)
"Something is entering from outside"     → LinearOutSlowIn
"Something is leaving to outside"        → FastOutLinearIn
"A loading spinner / constant motion"    → LinearEasing
"A popup that feels springy"             → EaseOutBack
"A button that bounces when pressed"     → EaseOutElastic (or use spring())
"Ball landing on ground"                 → EaseOutBounce
"Premium, smooth page swipe"             → EaseInOutCubic
"Custom brand feel"                      → CubicBezierEasing(...)
```

---

## With AnimationSpec

```kotlin
// Easing is ONLY used with tween() and keyframes{}
// Spring() does NOT use easing (it's physics-based)

// ✅ With tween
animationSpec = tween(
    durationMillis = 400,
    easing = EaseOutBack  // ← here
)

// ✅ With keyframes (per-segment easing)
animationSpec = keyframes {
    durationMillis = 600
    0.5f at 200 with EaseOutElastic  // ← easing for this segment
    1f at 600
}

// ❌ spring() doesn't have easing — it uses dampingRatio + stiffness
animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
```

---

## 💡 Interview Q&A

**Q: What is easing in the context of animation?**
A: Easing is a function that transforms linear time progress (0→1) into a curved value. It determines how an animation accelerates and decelerates, making motion feel natural rather than mechanical.

**Q: What's the difference between `FastOutSlowIn` and `EaseOut`?**
A: They're similar but `FastOutSlowIn` is tuned for Material Design's specific aesthetic. Both decelerate toward the end but have slightly different curves. `FastOutSlowIn` is the default in Compose's `tween()`.

**Q: Can you use easing with spring()?**
A: No — spring animations are physics-based and use `dampingRatio`/`stiffness` to determine their curve. Easing is only applicable to `tween()` and `keyframes{}`.

**Q: What are good resources for designing custom easing?**
A: 
- [cubic-bezier.com](https://cubic-bezier.com) — Interactive bezier designer
- [easings.net](https://easings.net) — Visual gallery of all easing types
- [Material Design Motion](https://m3.material.io/styles/motion/easing-and-duration/tokens-specs) — MD3 specifications

---

**Next:** [[07 Animation/09 Modifier Animations/01 graphicsLayer|graphicsLayer — Scale, Rotate, Translate, Alpha →]]
