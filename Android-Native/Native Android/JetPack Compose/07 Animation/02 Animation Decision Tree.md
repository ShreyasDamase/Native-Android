# 🗺️ Decision Tree — Which Animation API to Use When

> [!NOTE]
> This is your GPS for Compose animations. Answer the questions, follow the path, arrive at the right API. No guessing.

---

## 🌳 The Main Decision Tree

```text
                  START: What is your animation goal?
                                   │
         ┌─────────────────────────┴─────────────────────────┐
         ▼                                                   ▼
  [ Animate Content Visibility ]                     [ Animate Properties/Values ]
         │                                                   │
         ├─► Show/Hide a composable                          ├─► Single value + state change
         │   └── AnimatedVisibility                              └── animate*AsState
         │                                                   │
         ├─► Swap one composable for another                 ├─► Multiple values + single state
         │   └── AnimatedContent                                 └── updateTransition
         │                                                   │
         ├─► Size changes smoothly                           ├─► Infinite Loop (shimmer/pulse)
         │   └── Modifier.animateContentSize                     └── rememberInfiniteTransition
         │                                                   │
         └─► Reorder list items                              ├─► Gesture-driven (drag/fling)
             └── Modifier.animateItem                            └── Animatable + Coroutine
                                                             │
                                                             └─► Custom drawing loop
                                                                 └── withFrameNanos + Animatable
```

---

## 📊 API Comparison Table

| Scenario | API | Level | When to Use |
|----------|-----|-------|-------------|
| Single value, state-triggered | `animate*AsState` | ⭐ Easy | Most common case |
| Show/hide composable | `AnimatedVisibility` | ⭐ Easy | Content appearing/disappearing |
| Swap composables | `AnimatedContent` | ⭐ Easy | Changing between UIs |
| Simple crossfade | `Crossfade` | ⭐ Easy | Tab changes, content swaps |
| Size change | `animateContentSize` | ⭐ Easy | Expanding cards, text |
| Multiple values, one state | `updateTransition` | ⭐⭐ Medium | Complex state-driven changes |
| Looping animation | `rememberInfiniteTransition` | ⭐⭐ Medium | Spinners, shimmer, pulse |
| Gesture-driven | `Animatable` | ⭐⭐⭐ Advanced | Drag + animation sync |
| Multi-step sequence | `Animatable` in coroutine | ⭐⭐⭐ Advanced | Step 1 → Step 2 → Step 3 |
| Canvas animation | `withFrameNanos` | ⭐⭐⭐⭐ Expert | Analog clock, particle system |
| Navigation transitions | `SharedTransitionLayout` | ⭐⭐⭐ Advanced | Screen-to-screen animations |

---

## 🔍 Scenarios & Correct API

### "I want to animate a button's background color when selected"
```
State change → single value → animateColorAsState ✅
```
```kotlin
val bgColor by animateColorAsState(
    targetValue = if (isSelected) Purple else Gray
)
```

### "I want a loading spinner that rotates forever"
```
Infinite loop → rememberInfiniteTransition ✅
```
```kotlin
val angle by rememberInfiniteTransition().animateFloat(
    initialValue = 0f, targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing))
)
```

### "I want a card to expand when clicked, with multiple properties changing"
```
State change → multiple values → updateTransition ✅
```
```kotlin
val transition = updateTransition(isExpanded, label = "card")
val height by transition.animateDp { if (it) 200.dp else 80.dp }
val alpha by transition.animateFloat { if (it) 1f else 0.5f }
```

### "I want an item to slide in from bottom when a list shows"
```
Show/Hide composable → AnimatedVisibility ✅
```
```kotlin
AnimatedVisibility(
    visible = isVisible,
    enter = slideInVertically { it } + fadeIn(),
    exit = slideOutVertically { it } + fadeOut()
) { ItemContent() }
```

### "I want a tab pill that follows my finger as I drag between tabs"
```
Gesture-driven → Animatable ✅
```
```kotlin
val offsetX = remember { Animatable(0f) }
// In drag gesture: offsetX.snapTo(newX)
// On release: offsetX.animateTo(targetX, spring())
```

### "I want a pulse effect on a notification badge"
```
Infinite loop → rememberInfiniteTransition ✅
```
```kotlin
val scale by rememberInfiniteTransition().animateFloat(
    initialValue = 1f, targetValue = 1.3f,
    animationSpec = infiniteRepeatable(
        tween(600), repeatMode = RepeatMode.Reverse
    )
)
```

### "I want a shimmer loading effect"
```
Infinite loop + gradient sweep → rememberInfiniteTransition ✅
```

### "I want a confetti burst when user achieves something"
```
Multiple independent animations → List of Animatable ✅
```

### "I want content to smoothly swap when I change tabs"
```
Swap composable → AnimatedContent ✅
```
```kotlin
AnimatedContent(targetState = selectedTab) { tab ->
    when (tab) {
        Tab.Home -> HomeScreen()
        Tab.Profile -> ProfileScreen()
    }
}
```

---

## ⚠️ Common Wrong Choices

| You might think... | Don't use | Use instead | Why |
|---------------------|-----------|-------------|-----|
| "I need the element to move" | `Modifier.offset(animatedDp)` | `graphicsLayer { translationX }` | offset() causes layout recalc |
| "I need a simple toggle" | `updateTransition` | `animate*AsState` | Overkill for single value |
| "I need it to bounce" | `tween(...)` | `spring(dampingRatio = Medium)` | tween can't bounce |
| "I need it to loop" | `while(true) { animateTo() }` | `rememberInfiniteTransition` | Coroutine loop is fragile |
| "I need gesture + animation" | `animate*AsState` | `Animatable` | Can't cancel/interrupt `*AsState` |

---

## 🧭 Quick Reference by Property

| Property to Animate | API |
|---------------------|-----|
| Float | `animateFloatAsState` |
| Dp | `animateDpAsState` |
| Color | `animateColorAsState` |
| Int | `animateIntAsState` |
| IntOffset | `animateIntOffsetAsState` |
| IntSize | `animateIntSizeAsState` |
| Offset | `animateOffsetAsState` |
| Rect | `animateRectAsState` |
| Size | `animateSizeAsState` |
| Custom type | `animateValueAsState` |
| Any of above, infinitely | `rememberInfiniteTransition.animate*` |
| Any of above, manually | `Animatable<T, V>` |

---

**Next steps:**
- [[07 Animation/03 High Level APIs/01 animate*AsState|animate\*AsState →]]
- [[07 Animation/04 Low Level APIs/01 Animatable|Animatable →]]
- [[07 Animation/05 Transitions/02 rememberInfiniteTransition|rememberInfiniteTransition →]]
