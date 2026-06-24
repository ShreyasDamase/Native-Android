# ⚡ Animation Performance — What to Know

> [!IMPORTANT]
> Compose has 3 phases: **Composition → Layout → Drawing**. The phase where your animation runs determines its performance cost. Always animate at the cheapest phase possible.

---

## The 3 Phases and Animation Cost

```text
  ┌─────────────────────────────────────────────────────────────────┐
  │                 COMPOSE LAYOUT INSPECTOR PIPELINE               │
  │                                                                 │
  │     State Change (targetValue changes)                          │
  │                 │                                               │
  │                 ▼                                               │
  │     ┌───────────────────────┐                                   │
  │     │   Composition Phase   │ ──► Re-runs composable functions  │
  │     └───────────┬───────────┘     (Very Expensive!)             │
  │                 │                                               │
  │                 ▼                                               │
  │     ┌───────────────────────┐                                   │
  │     │     Layout Phase      │ ──► Measures & places children    │
  │     └───────────┬───────────┘     (Expensive!)                  │
  │                 │                                               │
  │                 ▼                                               │
  │     ┌───────────────────────┐                                   │
  │     │     Drawing Phase     │ ──► Draws canvas pixels           │
  │     └───────────────────────┘     (Cheap!)                      │
  │                                                                 │
  │  * Modifiers like graphicsLayer {} bypass Composition & Layout  │
  │    entirely, sending animating values straight to Drawing!      │
  └─────────────────────────────────────────────────────────────────┘
```

| Animation | Phase | Cost |
|-----------|-------|------|
| `animateContentSize` | Layout | 🔴 Expensive |
| `Modifier.padding(animated)` | Layout | 🔴 Expensive |
| `Modifier.offset(Dp)` | Layout | 🔴 Expensive |
| `Modifier.alpha(animated)` | Drawing | 🟡 OK |
| `graphicsLayer { alpha }` | Drawing | 🟢 Cheap |
| `graphicsLayer { scaleX }` | Drawing | 🟢 Cheap |
| `graphicsLayer { translationX }` | Drawing | 🟢 Cheap |
| `graphicsLayer { rotationZ }` | Drawing | 🟢 Cheap |
| Canvas `drawWithCache` | Drawing | 🟢 Cheap |

---

## The Golden Rule

> **Animate in `graphicsLayer` whenever possible.**
> It skips Composition and Layout entirely — only Drawing happens.

```kotlin
// ❌ Triggers layout every frame
val padding by animateDpAsState(if (selected) 16.dp else 0.dp, label = "p")
Modifier.padding(padding)

// ✅ Only Drawing phase — no layout
val scale by animateFloatAsState(if (selected) 1.05f else 1f, label = "s")
Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
```

---

## What CAN'T Be Done in graphicsLayer

Some animations MUST go through Layout because they actually change how other elements position:

| Situation | Must Use | Reason |
|-----------|----------|--------|
| Card expanding to show more content | `animateContentSize` | Other UI must move |
| Drawer sliding from edge | `AnchoredDraggable` | Changes layout bounds |
| List items reordering | `animateItem` | Layout needs to know positions |

For these, accept the layout cost — it's unavoidable. Optimize everything else.

---

## derivedStateOf — Prevent Over-Recomposition

When you derive animation inputs from frequently-changing state:

```kotlin
// ❌ Bad: every scroll position change causes recomposition of Button
val showButton by remember {
    derivedStateOf { scrollState.value > 100 }  // ← OK, but without derivedStateOf it's even worse
}

// ✅ Better: only recomposes when boolean changes (not on every pixel of scroll)
val showButton by remember {
    derivedStateOf { scrollState.value > 100 }
}

AnimatedVisibility(visible = showButton) { FABButton() }
```

`derivedStateOf` memoizes — only triggers recomposition when the derived value actually changes.

---

## `shouldAutoCancel` in Animatable

By default, `Animatable.animateTo()` cancels the previous animation when a new `animateTo` starts. This is usually what you want (smooth interruption).

If you want multiple animations to run simultaneously on the same `Animatable` (don't do this usually), you'd need separate `Animatable` instances.

---

## Avoiding Allocation in Animation Loops

```kotlin
// ❌ Bad: creates new objects every frame
drawWithContent {
    val brush = Brush.linearGradient(...)  // BAD: created each frame
    drawRect(brush)
}

// ✅ Good: cache expensive objects
drawWithCache {
    val brush = Brush.linearGradient(...)  // created once
    onDrawWithContent {
        drawContent()
        drawRect(brush)  // reuses cached brush
    }
}
```

For animated brush (shimmer), the brush must change every frame — in that case, minimize object creation:

```kotlin
// Acceptable for animated gradients
val offset by infiniteTransition.animateFloat(...)

Modifier.drawWithContent {
    drawContent()
    // Brush depends on animated offset — must recreate
    // But we avoid other allocations here
    drawRect(
        brush = Brush.linearGradient(
            colors = shimmerColors,  // ← pre-created list, not inline
            start = Offset(offset, 0f),
            end = Offset(offset + 200, 0f)
        )
    )
}

// Pre-create the colors list OUTSIDE the draw block
val shimmerColors = remember {
    listOf(
        Color.LightGray.copy(alpha = 0.3f),
        Color.LightGray.copy(alpha = 0.8f),
        Color.LightGray.copy(alpha = 0.3f)
    )
}
```

---

## Rendering in Overlay During Shared Element Transitions

During shared element transitions, elements render in an overlay (on top of everything). This has performance implications for complex composables. Keep shared elements visually simple.

---

## Animation Inspector (Debug Tool)

1. Run app in Debug mode in Android Studio
2. Open **Layout Inspector**
3. Check **Show Recomposition Counts**
4. Identify which composables recompose during animation

High recomposition counts during animation → you're probably not using `graphicsLayer` where you should.

---

## Profiling Checklist

- [ ] All visual-only animations (scale, rotate, translate, alpha) use `graphicsLayer`
- [ ] No `Modifier.offset(Dp)` in animations — use `graphicsLayer { translationX }`
- [ ] Infinite animations use `rememberInfiniteTransition`, not `while(true)` loops
- [ ] Canvas animations create expensive objects in `drawWithCache`, not `onDrawBehind`
- [ ] LazyList animations use `animateItem` with proper `key`
- [ ] All `animate*AsState` have `label` for debugging
- [ ] `derivedStateOf` used when deriving boolean from frequently-changing state

---

## 💡 Interview Q&A

**Q: Why is animating `padding` expensive but animating `scale` cheap?**
A: Padding changes the layout — when padding changes, Compose must recalculate the position of the composable and everything affected by it (Layout phase). Scale changes only the visual rendering of the composable without affecting its layout footprint (Drawing phase only).

**Q: What's the difference between `Modifier.offset { IntOffset }` and `Modifier.offset(Dp)`?**
A: The lambda form `offset { }` is measured and placed in the Layout phase but is more efficient for pixel-precise animations. The Dp form converts Dp to pixels and triggers a layout pass. For animations, prefer `graphicsLayer { translationX }` over both.

**Q: When is it acceptable to animate layout properties?**
A: When the animation MUST affect the layout of other composables — like expanding a card that pushes down content, or a drawer sliding in that shrinks the main content area. For purely visual effects (making something look bigger without pushing others), always use `graphicsLayer`.
