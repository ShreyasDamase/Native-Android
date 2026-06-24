# Animatable — The Powerhouse

> [!IMPORTANT]
> `Animatable` is the **lowest-level coroutine-based animation primitive** in Compose. Every high-level API (`animate*AsState`, `updateTransition`) is built on top of it. Learn this deeply — it gives you total control.

---

## What is `Animatable`?

A value holder that can animate to a target using coroutines. Unlike `animate*AsState`, you control it manually — you call `animateTo()`, `snapTo()`, `stop()` yourself inside a coroutine.

```kotlin
val offsetX = remember { Animatable(0f) }

// In a coroutine:
offsetX.animateTo(targetValue = 200f, animationSpec = spring())
```

```text
  ┌─────────────────────────────────────────────────────────────────┐
  │                   GESTURE TIMELINE STATE MACHINE                │
  │                                                                 │
  │  [Pointer Down] ──► Call anim.stop()                            │
  │                     (Halts running animation instantly)         │
  │                           │                                     │
  │                           ▼                                     │
  │  [Dragging]     ──► Call anim.snapTo(newPosition)               │
  │                     (Updates position per frame, no delay)      │
  │                           │                                     │
  │                           ▼                                     │
  │  [Pointer Up]   ──► Call anim.animateTo(target, spring())       │
  │                     (Smooth settle using release velocity)      │
  └─────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────┐
  │                       BOUNDARY CLAMPING                         │
  │                                                                 │
  │  Value                                                          │
  │    ▲                                                            │
  │    │  (Animate target = 300f)                                   │
  │    │                                                            │
  │    │                 ┌────────────────────── (upperBound = 200f)│
  │    │               ╱ │                                          │
  │    │             ╱   │                                          │
  │  0 └───────────╱─────┴────────────────────────► Time            │
  │                Value hits bound -> animateTo() halts & returns  │
  │                with endReason = BoundReached.                   │
  └─────────────────────────────────────────────────────────────────┘
```

---

## Creating an Animatable

```kotlin
// Float (most common)
val scale = remember { Animatable(initialValue = 1f) }

// Dp
val width = remember { Animatable(initialValue = 100.dp, typeConverter = Dp.VectorConverter) }

// Color
val color = remember { Animatable(initialValue = Color.Red, typeConverter = Color.VectorConverter) }

// Offset
val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
```

> [!NOTE]
> For `Float`, no `typeConverter` needed. For other types, provide the appropriate `TwoWayConverter`.

---

## Full API Reference

### Constructor
```kotlin
class Animatable<T, V : AnimationVector>(
    initialValue: T,
    val typeConverter: TwoWayConverter<T, V>,
    val visibilityThreshold: T? = null,
    val label: String = "Animatable"
)
```

### Methods

#### `animateTo()` — Smoothly animate to a target
```kotlin
suspend fun animateTo(
    targetValue: T,
    animationSpec: AnimationSpec<T> = defaultSpringSpec,
    initialVelocity: T = velocity,   // Starting velocity (e.g., from gesture)
    block: (Animatable<T, V>.() -> Unit)? = null  // Called each frame
): AnimationResult<T, V>
```

- **Suspends** until animation completes OR is cancelled
- Returns `AnimationResult` with `endReason` and `endState`

```kotlin
launch {
    val result = offset.animateTo(
        targetValue = 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    // result.endReason: Finished | BoundReached | Cancelled
}
```

#### `snapTo()` — Instantly jump to a value
```kotlin
suspend fun snapTo(targetValue: T)
```

- **Does not animate** — teleports the value instantly
- Also suspends briefly (to synchronize with frame timing)
- Use during **active gesture** when you want immediate response

```kotlin
launch {
    // During drag: snap instantly
    offset.snapTo(dragDelta)
    
    // On release: animate smoothly to final position
    offset.animateTo(0f, spring())
}
```

#### `stop()` — Cancel the current animation
```kotlin
fun stop()
```

- Stops the current animation at the current value
- Does NOT suspend
- Velocity is preserved after stop

#### `updateBounds()` — Set clamping limits
```kotlin
fun updateBounds(lowerBound: T? = null, upperBound: T? = null)
```

- The animation cannot go below `lowerBound` or above `upperBound`
- If animation hits a bound, `animateTo()` returns with `endReason = BoundReached`

```kotlin
offset.updateBounds(lowerBound = -200f, upperBound = 200f)
```

---

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `value` | `T` | Current animated value |
| `velocity` | `T` | Current velocity of the animation |
| `targetValue` | `T` | The target value being animated to |
| `isRunning` | `Boolean` | `true` if animation is currently playing |
| `lowerBound` | `T?` | Lower clamping bound |
| `upperBound` | `T?` | Upper clamping bound |
| `typeConverter` | `TwoWayConverter<T, V>` | Converts between T and AnimationVector |

---

## AnimationResult

What `animateTo()` returns:

```kotlin
data class AnimationResult<T, V>(
    val endState: AnimationState<T, V>,  // Final value, velocity, etc.
    val endReason: AnimationEndReason    // Why it ended
)

enum class AnimationEndReason {
    Finished,     // Reached target value normally
    BoundReached, // Hit a lowerBound or upperBound
    Cancelled     // Was cancelled by stop() or a new animateTo()
}
```

---

## The Coroutine Connection

`Animatable` is designed to work with **coroutines**:

```kotlin
val coroutineScope = rememberCoroutineScope()

Button(onClick = {
    coroutineScope.launch {
        scale.animateTo(1.5f, spring())  // grow
        scale.animateTo(1f, spring())    // shrink back
    }
}) {
    Text("Bounce!")
}
```

> [!IMPORTANT]
> **Cancellation**: If you call `animateTo()` while another is running, the first is **cancelled** and the second starts from the current value+velocity. This is how you get smooth interruption.

---

## Multi-Step Animation

```kotlin
launch {
    // Step 1: Move right
    offsetX.animateTo(200f, tween(300))
    // Step 2: Move down (only starts after step 1 finishes)
    offsetY.animateTo(200f, tween(300))
    // Step 3: Return to origin
    offsetX.animateTo(0f, tween(500))
    offsetY.animateTo(0f, tween(500))
}
```

---

## Gesture-Driven Pattern (The Key Pattern)

This is the pattern behind your FloatingTabBar pill:

```kotlin
val offset = remember { Animatable(0f) }
val scope = rememberCoroutineScope()

Box(
    modifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = {
                scope.launch { offset.stop() } // Cancel any ongoing animation
            },
            onDrag = { change, dragAmount ->
                change.consume()
                scope.launch {
                    offset.snapTo(offset.value + dragAmount.x) // Instant during drag
                }
            },
            onDragEnd = {
                scope.launch {
                    offset.animateTo(
                        targetValue = 0f,  // Snap back home
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        ),
                        initialVelocity = offset.velocity  // Use gesture velocity
                    )
                }
            }
        )
    }
) {
    // Use offset.value in your composable
    Box(modifier = Modifier.offset { IntOffset(offset.value.roundToInt(), 0) })
}
```

---

## Animatable vs animate*AsState

| | `animate*AsState` | `Animatable` |
|--|------------------|--------------|
| Control | Automatic | Manual |
| Cancellation | Auto on recompose | Manual via `stop()` |
| Multi-step | ❌ | ✅ (sequential coroutines) |
| Gesture sync | ❌ (laggy) | ✅ (snapTo/animateTo) |
| Initial velocity | ❌ | ✅ |
| Await completion | ❌ | ✅ (suspend) |

---

## Decay Animation (Fling)

After a gesture, use decay to simulate natural deceleration:

```kotlin
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.splineBasedDecay

val decay = rememberSplineBasedDecay<Float>()

onDragEnd = {
    scope.launch {
        offset.animateDecay(
            initialVelocity = velocityTracker.calculateVelocity().x,
            animationSpec = decay
        )
    }
}
```

---

## Complete Example: Spring Button

```kotlin
@Composable
fun SpringButton() {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clickable {
                scope.launch {
                    scale.animateTo(
                        targetValue = 0.85f,
                        animationSpec = spring(stiffness = Spring.StiffnessHigh)
                    )
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
    ) {
        Text("Press Me!")
    }
}
```

---

## 💡 Interview Q&A

**Q: What is `Animatable` and when would you use it?**
A: It's a coroutine-based animation primitive that gives you full manual control. Use it when you need gesture-driven animations, multi-step sequences, or want to use the animation's velocity.

**Q: What's the difference between `snapTo()` and `animateTo()`?**
A: `snapTo()` instantly sets the value with no animation. `animateTo()` smoothly transitions using an AnimationSpec. During gesture drag, use `snapTo()` for immediate response; on release, use `animateTo()` with spring for a natural settle.

**Q: What happens if you call `animateTo()` while another `animateTo()` is running?**
A: The first animation is cancelled and the second starts from the current value with preserved velocity. This enables smooth interruption — the animation direction can change mid-flight.

---

**Next:** [[07 Animation/05 Transitions/01 updateTransition|updateTransition →]]
