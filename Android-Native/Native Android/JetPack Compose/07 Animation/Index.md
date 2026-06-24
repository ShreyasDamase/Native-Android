# 07 Animation — Master Index

> [!NOTE]
> This is your complete animation reference vault. Every topic has its own note with API tables, key concepts, use cases, and real examples. Learn the "WHY" before the "HOW".

```text
                  🚀 JETPACK COMPOSE ANIMATION VAULT MAP
                                    │
       ┌────────────────────────────┼────────────────────────────┐
       ▼                            ▼                            ▼
  [Foundations]               [APIs & Levels]             [Aesthetics & Specs]
  ├─ Mindset                  ├─ High-Level               ├─ AnimationSpec
  ├─ Glossary                 │  ├─ animate*AsState       │  ├─ spring/tween
  └─ Decision Tree            │  ├─ AnimatedVisibility    │  └─ keyframes/snap
                              │  └─ AnimatedContent       ├─ Easing Curves
                              ├─ Low-Level                └─ Modifier Layers
                              │  └─ Animatable               └─ graphicsLayer
                              └─ Transitions
                                 ├─ updateTransition
                                 └─ infiniteTransition
                                    │
                                    ▼
                      ┌─────────────┴─────────────┐
                      ▼                           ▼
              [Practical Recipes]          [Performance & Ref]
              ├─ Shimmer / Shine           ├─ 3 Phases Pipeline
              ├─ Animated Switch           ├─ Cheat Sheets
              ├─ Clocks (Analog/Digital)   └─ Common Mistakes
              └─ 3D Card Flip
```

---

## 🧠 Start Here — Foundations

- [[07 Animation/00 Animation Mindset|🎯 Animation Mindset — How to Think in Animations]]
- [[07 Animation/01 Animation Glossary|📖 Animation Glossary — Every Term You Must Know]]
- [[07 Animation/02 Animation Decision Tree|🗺️ Decision Tree — Which API to Use When]]

---

## ⚡ High-Level APIs — Simple & Declarative

> Use these first. They handle state transitions automatically.

- [[07 Animation/03 High Level APIs/01 animate*AsState|animate\*AsState — Animate Any Value]]
- [[07 Animation/03 High Level APIs/02 AnimatedVisibility|AnimatedVisibility — Show & Hide with Motion]]
- [[07 Animation/03 High Level APIs/03 AnimatedContent|AnimatedContent — Swap Content with Animation]]
- [[07 Animation/03 High Level APIs/04 Crossfade|Crossfade — Fade Between Composables]]
- [[07 Animation/03 High Level APIs/05 animateContentSize|animateContentSize — Smooth Size Changes]]

---

## 🔬 Low-Level APIs — Manual & Precise

> Use when you need full control — gesture sync, custom curves, multi-step animations.

- [[07 Animation/04 Low Level APIs/01 Animatable|Animatable — The Powerhouse]]
- [[07 Animation/04 Low Level APIs/02 Animation Interface|Animation Interface & AnimationVector]]
- [[07 Animation/04 Low Level APIs/03 Coroutine Animations|Coroutine-Driven Animations]]

---

## 🔄 Transition APIs — State-Based Groups

> Coordinate multiple animations from a single state change.

- [[07 Animation/05 Transitions/01 updateTransition|updateTransition — Multi-Property Transitions]]
- [[07 Animation/05 Transitions/02 rememberInfiniteTransition|rememberInfiniteTransition — Loop Forever]]
- [[07 Animation/05 Transitions/03 Transition Properties|Transition Properties & Child Animations]]

---

## 📐 AnimationSpec — The Physics Engine

> This is what makes animations feel natural. Understand this deeply.

- [[07 Animation/06 AnimationSpec/01 tween|tween — Time-Based Animation]]
- [[07 Animation/06 AnimationSpec/02 spring|spring — Physics-Based Animation]]
- [[07 Animation/06 AnimationSpec/03 keyframes|keyframes — Frame-by-Frame Control]]
- [[07 Animation/06 AnimationSpec/04 repeatable|repeatable & infiniteRepeatable — Looping]]
- [[07 Animation/06 AnimationSpec/05 snap|snap — Instant Jump]]
- [[07 Animation/06 AnimationSpec/06 AnimationSpec Comparison|AnimationSpec Comparison Table]]

---

## 〰️ Easing — The Shape of Motion

> Easing is the curve that defines how an animation accelerates and decelerates.

- [[07 Animation/07 Easing/01 Easing Overview|Easing Overview & Concepts]]
- [[07 Animation/07 Easing/02 Built-in Easings|Built-in Easing Functions — Full Reference]]
- [[07 Animation/07 Easing/03 CubicBezierEasing|CubicBezierEasing — Custom Curves]]

---

## 🤚 Gesture-Driven Animations

> Animations that react to touch — drags, swipes, flings.

- [[07 Animation/08 Gesture Animations/01 Gesture Basics|Gesture Basics — Touch Events in Compose]]
- [[07 Animation/08 Gesture Animations/02 detectDragGestures|detectDragGestures — Drag & Drop]]
- [[07 Animation/08 Gesture Animations/03 AnchoredDraggable|AnchoredDraggable — Snap to Anchors]]
- [[07 Animation/08 Gesture Animations/04 Fling Animations|Fling — Velocity & Natural Deceleration]]
- [[07 Animation/08 Gesture Animations/05 Animatable with Gestures|Animatable + Gestures — Full Control]]

---

## 🎬 Modifier Animations

> Apply animations directly to composables via modifiers.

- [[07 Animation/09 Modifier Animations/01 graphicsLayer|graphicsLayer — Scale, Rotate, Translate, Alpha]]
- [[07 Animation/09 Modifier Animations/02 animateItemPlacement|animateItem — LazyList Item Animations]]
- [[07 Animation/09 Modifier Animations/03 drawWithCache|drawWithCache — Canvas Modifier Animations]]

---

## 🎨 Canvas Animations

> Draw custom animations using Canvas, drawWithCache, and withFrameNanos.

- [[07 Animation/10 Canvas Animations/01 Canvas Animation Basics|Canvas Animation Basics]]
- [[07 Animation/10 Canvas Animations/02 withFrameNanos|withFrameNanos & withFrameMillis]]
- [[07 Animation/10 Canvas Animations/03 Custom Draw Loop|Custom Draw Loop Pattern]]
- [[07 Animation/10 Canvas Animations/04 Analog Clock|🕐 Analog Clock — Hands & Dial]]
- [[07 Animation/10 Canvas Animations/05 Digital Clock Effect|🔢 Digital Clock — Flip & Slide Effect]]

---

## 🌟 Shared Element Transitions (Compose 1.7+)

> Premium navigation animations — content flows between screens.

- [[07 Animation/11 Shared Elements/01 Overview|Shared Element Overview]]
- [[07 Animation/11 Shared Elements/02 sharedElement|sharedElement Modifier]]
- [[07 Animation/11 Shared Elements/03 sharedBounds|sharedBounds Modifier]]
- [[07 Animation/11 Shared Elements/04 SharedTransitionLayout|SharedTransitionLayout]]
- [[07 Animation/11 Shared Elements/05 AnimatedNavHost|AnimatedNavHost Integration]]

---

## ✨ Practical Animation Recipes

> Real-world patterns you'll actually use in your apps.

### Effects & Polish
- [[07 Animation/12 Recipes/01 Shimmer Effect|✨ Shimmer / Loading Skeleton]]
- [[07 Animation/12 Recipes/02 Shine Effect|💎 Shine / Gloss Sweep]]
- [[07 Animation/12 Recipes/03 Pulse Breathing|💓 Pulse / Breathing Effect]]
- [[07 Animation/12 Recipes/04 Confetti Burst|🎉 Confetti / Particle Burst]]

### UI Components
- [[07 Animation/12 Recipes/05 Animated Switch|🔘 Animated Switch / Toggle]]
- [[07 Animation/12 Recipes/06 Tab Pill Stretch|🔵 Tab Pill Stretch Animation]]
- [[07 Animation/12 Recipes/07 FAB Giggle Bounce|➕ FAB Spring Giggle Bounce]]
- [[07 Animation/12 Recipes/08 Morphing Shape|⬤ Morphing Shape (Circle to Pill)]]
- [[07 Animation/12 Recipes/09 Progress Ring|⭕ Animated Progress Ring]]

### Text & Numbers
- [[07 Animation/12 Recipes/10 Typewriter Effect|⌨️ Typewriter Text Effect]]
- [[07 Animation/12 Recipes/11 Count Up Number|🔢 Count-Up Number Animation]]
- [[07 Animation/12 Recipes/12 Flip Clock|🕐 Flip Clock / Number Flip]]

### Scroll & Parallax
- [[07 Animation/12 Recipes/13 Staggered List|📋 Staggered List Entrance]]
- [[07 Animation/12 Recipes/14 Parallax Effect|🏔️ Parallax Scroll Effect]]
- [[07 Animation/12 Recipes/15 Scroll Driven|📜 Scroll-Driven Animations]]

### Advanced
- [[07 Animation/12 Recipes/16 3D Card Flip|🃏 3D Card Flip]]
- [[07 Animation/12 Recipes/17 Lottie Integration|🎭 Lottie Animation Integration]]

---

## ⚡ Performance & Best Practices

- [[07 Animation/13 Performance/01 Animation Performance|Performance Overview]]
- [[07 Animation/13 Performance/02 Skip Recomposition|Avoiding Recomposition in Animations]]
- [[07 Animation/13 Performance/03 graphicsLayer vs Layout|graphicsLayer vs Layout Animations]]
- [[07 Animation/13 Performance/04 Profiling Animations|Profiling & Debugging Animations]]

---

## 📚 Quick Reference Tables

- [[07 Animation/14 Reference/API Cheat Sheet|📋 API Cheat Sheet]]
- [[07 Animation/14 Reference/AnimationSpec Quick Ref|⚙️ AnimationSpec Quick Reference]]
- [[07 Animation/14 Reference/Easing Cheat Sheet|〰️ Easing Cheat Sheet]]
- [[07 Animation/14 Reference/Common Mistakes|❌ Common Mistakes & Fixes]]

---

## 🔗 Official Resources

- [Jetpack Compose Animation Docs](https://developer.android.com/develop/ui/compose/animation/introduction)
- [Animation Quick Guide](https://developer.android.com/develop/ui/compose/animation/quick-guide)
- [Compose Animation Codelab](https://developer.android.com/codelabs/jetpack-compose-animation)
- [Animation Samples — GitHub](https://github.com/android/compose-samples)

---

*Start with [[07 Animation/00 Animation Mindset|Animation Mindset]] → then [[07 Animation/01 Animation Glossary|Glossary]] → then [[07 Animation/02 Animation Decision Tree|Decision Tree]]. That sequence will wire your brain correctly before touching any API.*