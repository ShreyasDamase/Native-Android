# 📋 API Cheat Sheet — Complete Quick Reference

> [!TIP]
> Keep this open while coding. Every animation API, parameter, and common pattern in one place.

```text
  ┌─────────────────────────────────────────────────────────────────┐
  │                 API CATEGORY CHEAT SHEET MAP                    │
  │                                                                 │
  │  ┌───────────────────────┐       ┌───────────────────────────┐  │
  │  │     Simple Values     │ ◄───► │   Show / Hide Composable  │  │
  │  │    animate*AsState    │       │     AnimatedVisibility    │  │
  │  └───────────┬───────────┘       └───────────────────────────┘  │
  │              │                                 ▲                │
  │              ▼                                 │                │
  │  ┌───────────────────────┐                     ▼                │
  │  │    Manual Control     │       ┌───────────────────────────┐  │
  │  │      Animatable       │ ◄───► │      Swap Composable      │  │
  │  └───────────┬───────────┘       │      AnimatedContent      │  │
  │              │                   └───────────────────────────┘  │
  │              ▼                                                  │
  │  ┌───────────────────────┐                                      │
  │  │   Group Coordinator   │ ◄───► rememberInfiniteTransition     │
  │  │   updateTransition    │                                      │
  │  └───────────────────────┘                                      │
  └─────────────────────────────────────────────────────────────────┘
```

---

## animate*AsState — Quick Reference

```kotlin
// Float
val f by animateFloatAsState(targetValue, animationSpec, label = "name")

// Dp  
val d by animateDpAsState(targetValue, animationSpec, label = "name")

// Color
val c by animateColorAsState(targetValue, animationSpec, label = "name")

// Int
val i by animateIntAsState(targetValue, animationSpec, label = "name")

// Offset (2D float)
val o by animateOffsetAsState(targetValue, animationSpec, label = "name")

// IntOffset (2D int, pixels)
val io by animateIntOffsetAsState(targetValue, animationSpec, label = "name")

// Size
val s by animateSizeAsState(targetValue, animationSpec, label = "name")

// IntSize
val is by animateIntSizeAsState(targetValue, animationSpec, label = "name")

// Rect
val r by animateRectAsState(targetValue, animationSpec, label = "name")

// Any custom type
val v by animateValueAsState(targetValue, typeConverter = MyConverter, label = "name")
```

**Common parameters:**
- `targetValue` — destination
- `animationSpec` — HOW (default: `spring()`)
- `label` — debug name (always set!)
- `finishedListener` — callback when done

---

## AnimatedVisibility — Quick Reference

```kotlin
AnimatedVisibility(
    visible = booleanState,
    enter = fadeIn() + slideInVertically { it },     // Appears
    exit = fadeOut() + slideOutVertically { it },    // Disappears
    label = "myVisibility"
) {
    YourContent()
}
```

**Enter transitions:** `fadeIn`, `slideIn[Horizontally/Vertically]`, `expandIn/Horizontally/Vertically`, `scaleIn`
**Exit transitions:** `fadeOut`, `slideOut[Horizontally/Vertically]`, `shrinkOut/Horizontally/Vertically`, `scaleOut`
**Combine with:** `+` operator

---

## AnimatedContent — Quick Reference

```kotlin
AnimatedContent(
    targetState = state,
    transitionSpec = {
        // initialState → targetState available here
        fadeIn() togetherWith fadeOut()
    },
    label = "myContent"
) { currentState ->  // ← use THIS parameter, not outer state!
    when (currentState) { ... }
}
```

**ContentTransform:** `enterTransition togetherWith exitTransition [using SizeTransform(...)]`

---

## Crossfade — Quick Reference

```kotlin
Crossfade(
    targetState = state,
    animationSpec = tween(300),
    label = "crossfade"
) { current ->
    when (current) { ... }
}
```

---

## Animatable — Quick Reference

```kotlin
val anim = remember { Animatable(initialValue = 0f) }
val scope = rememberCoroutineScope()

// Animate smoothly (suspends)
scope.launch { anim.animateTo(targetValue, spring()) }

// Instant jump (suspends briefly)
scope.launch { anim.snapTo(newValue) }

// Stop
anim.stop()

// Set bounds
anim.updateBounds(lowerBound = 0f, upperBound = 100f)

// Decay (fling)
scope.launch { anim.animateDecay(initialVelocity, exponentialDecay()) }

// Properties
anim.value        // current
anim.velocity     // current velocity  
anim.targetValue  // where it's heading
anim.isRunning    // true if animating
```

---

## updateTransition — Quick Reference

```kotlin
val transition = updateTransition(targetState = state, label = "myTransition")

val scale by transition.animateFloat(label = "scale") { state ->
    when (state) {
        MyState.A -> 1f
        MyState.B -> 1.5f
    }
}

// Also: animateDp, animateColor, animateInt, animateOffset, animateSize, etc.
// Direction-aware:
val value by transition.animateFloat(
    transitionSpec = {
        if (isTransitioningTo(MyState.B)) spring() else tween()
    },
    label = "value"
) { ... }
```

---

## rememberInfiniteTransition — Quick Reference

```kotlin
val infinite = rememberInfiniteTransition(label = "myInfinite")

val f by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = tween(1000),
        repeatMode = RepeatMode.Reverse
    ),
    label = "value"
)

// Also: animateColor, animateValue
```

---

## AnimationSpec — Quick Reference

```kotlin
// Spring (physics, no fixed duration)
spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,  // 0.2, 0.5, 0.75, 1.0
    stiffness = Spring.StiffnessMedium               // 50, 200, 400, 4000, 10000
)

// Tween (time-based with easing)
tween(
    durationMillis = 300,
    delayMillis = 0,
    easing = FastOutSlowInEasing
)

// Keyframes (precise frame timing)
keyframes {
    durationMillis = 1000
    0.5f at 300 with LinearEasing
    1.2f at 700
    1f at 1000
}

// Snap (instant)
snap(delayMillis = 0)

// Repeating (finite)
repeatable(
    iterations = 3,
    animation = tween(300),
    repeatMode = RepeatMode.Reverse
)

// Infinite
infiniteRepeatable(
    animation = tween(1000),
    repeatMode = RepeatMode.Restart,
    initialStartOffset = StartOffset(0)
)
```

---

## Spring Constants — Quick Reference

### DampingRatio
| Constant | Value | Feel |
|----------|-------|------|
| `DampingRatioHighBouncy` | 0.2f | Very bouncy |
| `DampingRatioMediumBouncy` | 0.5f | Moderate bounce |
| `DampingRatioLowBouncy` | 0.75f | Slight bounce |
| `DampingRatioNoBouncy` | 1.0f | No bounce |

### Stiffness
| Constant | Value | Feel |
|----------|-------|------|
| `StiffnessHigh` | 10_000f | Very snappy |
| `StiffnessMedium` | 4_000f | Default |
| `StiffnessMediumLow` | 400f | Relaxed |
| `StiffnessLow` | 200f | Slow |
| `StiffnessVeryLow` | 50f | Very slow |

---

## Easing — Quick Reference

| Easing | Use |
|--------|-----|
| `FastOutSlowInEasing` | Default (UI transitions) |
| `LinearOutSlowInEasing` | Enter transitions |
| `FastOutLinearInEasing` | Exit transitions |
| `LinearEasing` | Spinners, clocks |
| `EaseOutBack` | Popups, dialogs (overshoot) |
| `EaseOutBounce` | Ball bounce effect |
| `EaseOutElastic` | Button springs |
| `EaseInOut` | Balanced transitions |

---

## graphicsLayer — Quick Reference

```kotlin
Modifier.graphicsLayer {
    alpha = 0.8f                    // 0-1
    scaleX = 1.2f                   // scale
    scaleY = 1.2f
    rotationX = 0f                  // 3D flip
    rotationY = 0f                  // 3D card flip
    rotationZ = 45f                 // 2D rotation
    translationX = 100f             // pixels, no layout
    translationY = 50f
    transformOrigin = TransformOrigin(0.5f, 0.5f)  // pivot
    shadowElevation = 8f
    clip = true
    shape = RoundedCornerShape(12.dp)
}
```

---

## Enter/Exit Transition Parameters

```kotlin
fadeIn(
    animationSpec: FiniteAnimationSpec<Float> = spring(),
    initialAlpha: Float = 0f  // start from
)

fadeOut(
    animationSpec: FiniteAnimationSpec<Float> = spring(),
    targetAlpha: Float = 0f  // end at
)

slideInVertically(
    animationSpec = spring(),
    initialOffsetY: (fullHeight: Int) -> Int = { -it }  // neg = from top, pos = from bottom
)

slideInHorizontally(
    animationSpec = spring(),
    initialOffsetX: (fullWidth: Int) -> Int = { -it }   // neg = from left, pos = from right
)

scaleIn(
    animationSpec = spring(),
    initialScale: Float = 0f,
    transformOrigin: TransformOrigin = TransformOrigin.Center
)

expandVertically(
    animationSpec = spring(),
    expandFrom: Alignment.Vertical = Alignment.Bottom,
    clip: Boolean = true
)
```

---

## animateContentSize — Quick Reference

```kotlin
Modifier.animateContentSize(
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    finishedListener: ((IntSize, IntSize) -> Unit)? = null
)
```

Apply to parent when content size changes:
```kotlin
Column(
    modifier = Modifier.animateContentSize()
) {
    Text(if (expanded) longText else shortText)
}
```

---

## animateItem — Quick Reference (LazyList)

```kotlin
LazyColumn {
    items(list, key = { it.id }) { item ->  // KEY IS REQUIRED
        ItemContent(
            modifier = Modifier.animateItem(
                fadeInSpec = spring(),
                placementSpec = spring(),
                fadeOutSpec = spring()
            )
        )
    }
}
```

---

## Performance Rules

| ✅ DO (Cheap) | ❌ AVOID (Expensive) |
|--------------|---------------------|
| `graphicsLayer { alpha }` | `Modifier.alpha(animated)` |
| `graphicsLayer { translationX }` | `Modifier.offset(animated.dp)` |
| `graphicsLayer { scaleX }` | Resizing with layout |
| `drawWithCache` for Canvas | Creating Brush in `onDrawBehind` |
| `rememberInfiniteTransition` | `while(true)` loop |
| `animateItem` for lists | `AnimatedVisibility` per item |

---

## Common Patterns (One-liners)

```kotlin
// Press scale feedback
val scale by animateFloatAsState(if (pressed) 0.93f else 1f, spring(StiffnessHigh), "scale")
Modifier.graphicsLayer { scaleX = scale; scaleY = scale }

// Fade in/out
val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(300), "alpha")
Modifier.graphicsLayer { this.alpha = alpha }

// Spinner
val angle by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing)), "spin")
Modifier.graphicsLayer { rotationZ = angle }

// Pulse
val scale by infiniteTransition.animateFloat(1f, 1.2f, infiniteRepeatable(tween(600), RepeatMode.Reverse), "pulse")
Modifier.graphicsLayer { scaleX = scale; scaleY = scale }

// Color on select
val color by animateColorAsState(if (selected) Primary else Gray, tween(200), "selColor")
Modifier.background(color)
```
