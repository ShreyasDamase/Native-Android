# ❌ Common Mistakes & Fixes

> [!IMPORTANT]
> These are the mistakes almost every developer makes when starting with Compose animations. Learn these BEFORE you code, not after 2 hours of debugging.

---

## Mistake 1: Using `animate*AsState` for Gesture-Driven Animation

### ❌ Wrong
```kotlin
var dragX by remember { mutableFloatStateOf(0f) }

// BAD: drag position goes through animate*AsState
val animatedX by animateFloatAsState(targetValue = dragX, label = "drag")

Box(
    modifier = Modifier
        .offset { IntOffset(animatedX.roundToInt(), 0) }
        .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                dragX += dragAmount.x
            }
        }
)
```

**Problem**: `animate*AsState` adds lag and can't be interrupted cleanly. During fast gestures, the animation is always playing catch-up.

### ✅ Fix
```kotlin
val offset = remember { Animatable(0f) }
val scope = rememberCoroutineScope()

Box(
    modifier = Modifier
        .offset { IntOffset(offset.value.roundToInt(), 0) }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { scope.launch { offset.stop() } },
                onDrag = { change, dragAmount ->
                    scope.launch { offset.snapTo(offset.value + dragAmount.x) }
                },
                onDragEnd = {
                    scope.launch { offset.animateTo(0f, spring()) }
                }
            )
        }
)
```

---

## Mistake 2: Animating with `Modifier.offset(dp)` Instead of `graphicsLayer`

### ❌ Wrong
```kotlin
val offsetY by animateDpAsState(if (isDown) 20.dp else 0.dp, label = "offset")
Box(modifier = Modifier.offset(y = offsetY))  // ← triggers layout every frame!
```

**Problem**: `Modifier.offset(Dp)` triggers Layout recalculation on every frame. At 60fps = 60 layout passes per second. Expensive and affects surrounding composables.

### ✅ Fix
```kotlin
val translationY by animateFloatAsState(if (isDown) 20f.dpToPx() else 0f, label = "trans")
Box(modifier = Modifier.graphicsLayer { this.translationY = translationY })
// OR use pixel offset for pixel-precise control:
val offsetPx by animateIntOffsetAsState(if (isDown) IntOffset(0, dpToPx(20)) else IntOffset.Zero, label = "offset")
Box(modifier = Modifier.offset { offsetPx })  // Modifier.offset { IntOffset } is ok
```

> [!NOTE]
> `Modifier.offset { IntOffset }` (the lambda form) is different from `Modifier.offset(Dp)`. The lambda form runs in the Layout phase but is still more efficient than the Dp form for animations.

```text
  ┌─────────────────────────────────────────────────────────────────┐
  │                 MISTAKE 2: PIPELINE PHASES COST                 │
  │                                                                 │
  │  ❌ Modifier.offset(Dp):                                        │
  │  [ Composition ] ──► [ Layout Phase ] ──► [ Drawing Phase ]     │
  │                         (Calculates size/position every frame!) │
  │                                                                 │
  │  ✅ Modifier.graphicsLayer / offset { }:                        │
  │  [ Composition ] ────────────────────────► [ Drawing Phase ]     │
  │                                             (Bypasses Layout!)  │
  └─────────────────────────────────────────────────────────────────┘
```

---

## Mistake 3: Forgetting the Label Parameter

### ❌ Wrong
```kotlin
val scale by animateFloatAsState(targetValue = if (selected) 1.2f else 1f)
```

**Problem**: No label → unnamed animation in Android Studio's Animation Inspector. Impossible to debug when you have 10+ animations.

### ✅ Fix
```kotlin
val scale by animateFloatAsState(
    targetValue = if (selected) 1.2f else 1f,
    label = "cardScale"  // ← always provide
)
```

---

## Mistake 4: Using `delayMillis` in `tween` with `infiniteRepeatable`

### ❌ Wrong
```kotlin
infiniteRepeatable(
    animation = tween(durationMillis = 1000, delayMillis = 300)  // ← delay every cycle!
)
```

**Problem**: `delayMillis` in `tween()` applies to EVERY iteration, not just the first. A 300ms delay on each of 60 infinite cycles = very unexpected pauses.

### ✅ Fix
```kotlin
infiniteRepeatable(
    animation = tween(durationMillis = 1000),
    initialStartOffset = StartOffset(300)  // ← delay only before FIRST iteration
)
```

---

## Mistake 5: Using the Outer State Inside `AnimatedContent`

### ❌ Wrong
```kotlin
var selectedTab by remember { mutableStateOf(Tab.Home) }

AnimatedContent(targetState = selectedTab) { _ ->
    // BAD: using outer selectedTab, not the lambda parameter
    when (selectedTab) {
        Tab.Home -> HomeScreen()
        Tab.Profile -> ProfileScreen()
    }
}
```

**Problem**: During the crossfade transition, both old and new content exist simultaneously. Both will read the current (newest) `selectedTab` from the outer scope, meaning both show the same content — breaking the animation.

### ✅ Fix
```kotlin
AnimatedContent(targetState = selectedTab) { tab ->  // use `tab`!
    when (tab) {
        Tab.Home -> HomeScreen()
        Tab.Profile -> ProfileScreen()
    }
}
```

```text
  ┌─────────────────────────────────────────────────────────────────┐
  │                 MISTAKE 5: OUTGOING/INCOMING STATE              │
  │                                                                 │
  │  ❌ Reading Outer selectedTab:                                  │
  │  Outgoing Content (reads outer state = Profile) ──► Profile     │
  │                                                      (Jump!)    │
  │  Incoming Content (reads outer state = Profile) ──► Profile     │
  │                                                                 │
  │  ✅ Reading Lambda Parameter 'tab':                              │
  │  Outgoing Content (reads parameter tab = Home)    ──► Home      │
  │                                                      (Smooth    │
  │  Incoming Content (reads parameter tab = Profile) ──► Profile   │
  │                                                      Crossfade) │
  └─────────────────────────────────────────────────────────────────┘
```

---

## Mistake 6: Creating Expensive Objects Inside `onDrawBehind`

### ❌ Wrong
```kotlin
Modifier.drawWithCache {
    onDrawBehind {
        // BAD: Brush created on EVERY frame (60 times/second!)
        val brush = Brush.linearGradient(colors = listOf(Color.Red, Color.Blue))
        drawRect(brush)
    }
}
```

**Problem**: `onDrawBehind` runs 60 times per second. Creating `Brush`, `Path`, or `Paint` objects here causes heavy GC pressure and performance degradation.

### ✅ Fix
```kotlin
Modifier.drawWithCache {
    // GOOD: Brush created ONCE (or when size changes)
    val brush = Brush.linearGradient(
        colors = listOf(Color.Red, Color.Blue),
        start = Offset(0f, 0f),
        end = Offset(size.width, 0f)
    )
    
    onDrawBehind {
        drawRect(brush)  // No allocation here — just use the cached brush
    }
}
```

**Exception**: If the brush depends on an animated value (like shimmer offset), it MUST be inside `onDrawBehind` because it changes every frame. In that case, keep allocations minimal.

---

## Mistake 7: Not Providing `key` for `animateItem` in LazyList

### ❌ Wrong
```kotlin
LazyColumn {
    items(myList) { item ->
        ItemContent(
            modifier = Modifier.animateItem()  // animates but wrong items!
        )
    }
}
```

**Problem**: Without `key`, Compose can't identify which item is which across recompositions. Additions and removals animate the wrong items.

### ✅ Fix
```kotlin
LazyColumn {
    items(
        items = myList,
        key = { item -> item.id }  // ← REQUIRED, must be stable and unique
    ) { item ->
        ItemContent(
            modifier = Modifier.animateItem()
        )
    }
}
```

---

## Mistake 8: Calling `animateTo` Outside a Coroutine

### ❌ Wrong
```kotlin
val anim = remember { Animatable(0f) }

// In a non-suspend context:
Button(onClick = {
    anim.animateTo(1f)  // ← ERROR: suspend function called in non-suspend context
})
```

**Problem**: `animateTo` is a `suspend` function — it can only be called from a coroutine.

### ✅ Fix
```kotlin
val scope = rememberCoroutineScope()

Button(onClick = {
    scope.launch {
        anim.animateTo(1f)  // ← OK: inside coroutine scope
    }
})
```

---

## Mistake 9: Using `while(true)` Loop for Infinite Animations

### ❌ Wrong
```kotlin
LaunchedEffect(Unit) {
    while (true) {
        angle.animateTo(360f, tween(1000))
        angle.snapTo(0f)
    }
}
```

**Problem**: Technically works but is fragile. If the composable recomposes while animating, the `LaunchedEffect` restarts and the animation flickers. Also hard to manage cancellation.

### ✅ Fix
```kotlin
val infiniteTransition = rememberInfiniteTransition(label = "spinner")
val angle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
    label = "angle"
)
```

---

## Mistake 10: Animating `spring()` with `repeatable`

### ❌ Wrong
```kotlin
repeatable(
    iterations = 3,
    animation = spring()  // ← ERROR: spring is not DurationBasedAnimationSpec
)
```

**Problem**: `repeatable` and `infiniteRepeatable` only work with **duration-based** specs (`tween`, `keyframes`). Spring has no fixed duration.

### ✅ Fix
```kotlin
repeatable(
    iterations = 3,
    animation = tween(500),  // ← OK
    repeatMode = RepeatMode.Reverse
)
```

---

## Mistake 11: Forgetting to `consume()` Pointer Events

### ❌ Wrong
```kotlin
detectDragGestures { change, dragAmount ->
    // Not consuming — parent or sibling may also react to this event!
    offset.snapTo(offset.value + dragAmount.x)
}
```

### ✅ Fix
```kotlin
detectDragGestures { change, dragAmount ->
    change.consume()  // ← prevents other gesture handlers from receiving this event
    scope.launch { offset.snapTo(offset.value + dragAmount.x) }
}
```

---

## Mistake 12: Not Cancelling Running Animation Before Gesture

### ❌ Wrong
```kotlin
detectDragGestures(
    onDrag = { change, dragAmount ->
        scope.launch { offset.snapTo(offset.value + dragAmount.x) }
    }
    // No onDragStart to stop existing animation!
)
```

**Problem**: If a spring-back animation is running and user grabs the element again, the snap and animation fight each other.

### ✅ Fix
```kotlin
detectDragGestures(
    onDragStart = {
        scope.launch { offset.stop() }  // ← Cancel running animation first
    },
    onDrag = { change, dragAmount ->
        change.consume()
        scope.launch { offset.snapTo(offset.value + dragAmount.x) }
    },
    onDragEnd = {
        scope.launch { offset.animateTo(0f, spring()) }
    }
)
```
