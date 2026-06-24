# 🎯 Animation Mindset — How to Think in Animations

> [!IMPORTANT]
> Before you write a single line of animation code, you need to understand how Compose thinks about animation. This note wires your brain correctly.

---

## The Core Mental Model

In traditional Android (Views), you told the UI **what to do**:
```kotlin
// Imperative — you drive it manually
ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
    duration = 300
    start()
}
```

In Compose, you tell the UI **what it should look like** for a given state. The animation is just the **journey between two states**:
```kotlin
// Declarative — state drives it
val alpha by animateFloatAsState(targetValue = if (isVisible) 1f else 0f)
Box(modifier = Modifier.alpha(alpha))
```

> [!TIP]
> **Think of animation as: "From State A → to State B, smoothly."**
> You never control the frames manually. You control the *states*, and Compose figures out the motion.

```text
  ┌───────────────────────────────────────────────────────────────┐
  │              DECLARATIVE STATE-DRIVEN FLOW                    │
  │                                                               │
  │  [User Interaction] ──► State Change (e.g., isVisible = true) │
  │                                     │                         │
  │                                     ▼                         │
  │                         Target Value = 1f                     │
  │                                     │                         │
  │                                     ▼                         │
  │                           [ Animation Spec ]                  │
  │                                     │                         │
  │                                     ▼                         │
  │                     Interpolated value (0.1f → 0.9f)          │
  │                                     │                         │
  │                                     ▼                         │
  │                     Drawing Phase (Re-renders frame)          │
  └───────────────────────────────────────────────────────────────┘
```

---

## The Three Questions to Ask Every Time

Before coding any animation, ask:

| Question | Example |
|----------|---------|
| **What changes?** | Position, size, color, alpha, rotation |
| **What triggers it?** | A click, a state change, an infinite loop |
| **How fast / physical?** | Bouncy spring? Smooth tween? Instant snap? |

---

## Animations vs Recomposition

This is the most important distinction:

```
State Changes → Recomposition (UI tree rebuilt)
           ↓
Animation APIs → NO Recomposition during animation
                 (they use separate drawing phase)
```

> [!WARNING]
> Animating `size` or `padding` with `Modifier.padding(animatedDp)` **causes recomposition** on every frame — expensive!
>
> Animating `scale` or `alpha` via `graphicsLayer` **bypasses recomposition** — cheap! ✅

---

## The 4 Levels of Animation APIs

Think of it as a tool belt — you pick the right tool:

```
Level 1 (Simplest):   animate*AsState, AnimatedVisibility, AnimatedContent
Level 2 (Groups):     updateTransition, rememberInfiniteTransition  
Level 3 (Manual):     Animatable (coroutine-driven)
Level 4 (Raw):        withFrameNanos, Canvas draw loops
```

Go as **low-level as you need**, but **no lower**. Start from Level 1 and move down only when you hit a limitation.

---

## State is the Source of Truth

```kotlin
// The state
var selected by remember { mutableStateOf(false) }

// The animation is just "track this state, smoothly"
val backgroundColor by animateColorAsState(
    targetValue = if (selected) Purple else Gray
)

// The UI reflects the animated value
Box(modifier = Modifier.background(backgroundColor))
```

The **state** (`selected`) is the truth. The **animation** is the smooth journey to reflect it. The **UI** draws whatever the animation says right now.

---

## Why Guilt About Copy-Pasting?

You copied animation code → it worked → but you didn't know WHY.

That means:
- You can't debug when it breaks 🐛
- You can't modify it to fit your needs 🔧
- You can't create new animations from scratch 🚫

After these notes, you'll understand:
- What every parameter does
- Why certain patterns exist
- How to build any animation from scratch

**Let's begin.** → [[07 Animation/01 Animation Glossary|Next: Animation Glossary →]]
