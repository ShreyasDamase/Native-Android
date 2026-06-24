# rememberInfiniteTransition — Loop Forever

> [!NOTE]
> Use `rememberInfiniteTransition` when you need an animation that **runs forever** — loading spinners, pulse effects, shimmer, breathing animations. This API is tailor-made for this exact use case.

---

## What is `rememberInfiniteTransition`?

A coroutine-driven transition that loops its child animations forever. Unlike `animate*AsState`, it doesn't need a state trigger — it just runs continuously from the moment the composable enters composition.

```kotlin
val infiniteTransition = rememberInfiniteTransition(label = "infinite")

val angle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
        animation = tween(1000, easing = LinearEasing)
    ),
    label = "spinnerAngle"
)

Icon(
    Icons.Default.Refresh,
    contentDescription = null,
    modifier = Modifier.graphicsLayer { rotationZ = angle }
)
```

---

## Full API Reference

### `rememberInfiniteTransition()`
```kotlin
@Composable
fun rememberInfiniteTransition(
    label: String = "InfiniteTransition"
): InfiniteTransition
```

Returns an `InfiniteTransition` object you use to define child animations.

### Child Animation Methods

#### `animateFloat`
```kotlin
@Composable
fun InfiniteTransition.animateFloat(
    initialValue: Float,
    targetValue: Float,
    animationSpec: InfiniteRepeatableSpec<Float>,
    label: String = "FloatAnimation"
): State<Float>
```

#### `animateValue`
```kotlin
@Composable
fun <T, V : AnimationVector> InfiniteTransition.animateValue(
    initialValue: T,
    targetValue: T,
    typeConverter: TwoWayConverter<T, V>,
    animationSpec: InfiniteRepeatableSpec<T>,
    label: String = "ValueAnimation"
): State<T>
```

#### `animateColor`
```kotlin
@Composable
fun InfiniteTransition.animateColor(
    initialValue: Color,
    targetValue: Color,
    animationSpec: InfiniteRepeatableSpec<Color>,
    label: String = "ColorAnimation"
): State<Color>
```

---

## Parameters

### `initialValue`
The **starting point** of each loop iteration.
- For spinner: `0f`
- For pulse: `1f` (normal size)

### `targetValue`
The **end point** of each loop iteration.
- For spinner: `360f`
- For pulse: `1.3f` (30% bigger)

### `animationSpec: InfiniteRepeatableSpec`
Must be `infiniteRepeatable(...)` — wraps a duration-based spec (tween or keyframes).

```kotlin
infiniteRepeatable(
    animation = tween(
        durationMillis = 1000,
        easing = LinearEasing
    ),
    repeatMode = RepeatMode.Restart  // or RepeatMode.Reverse
)
```

---

## RepeatMode in Infinite Animations

### `RepeatMode.Restart`
```
0° → 360° → 0° → 360° → ...
```
Snaps back to start, then plays again. Good for: spinners, progress.

### `RepeatMode.Reverse`
```
1f → 1.3f → 1f → 1.3f → 1f → ...
```
Plays forward then backward smoothly. Good for: pulse, breathing, shimmer.

```text
  ┌─────────────────────────────────────────────────────────────────┐
  │                    REPEATMODE WAVEFORMS                         │
  │                                                                 │
  │  RepeatMode.Restart:                                            │
  │  Value                                                          │
  │    ▲                                                            │
  │  Max │     ╱│    ╱│    ╱│                                       │
  │      │   ╱  │  ╱  │  ╱  │ (Instant snap back to Min)            │
  │  Min └──╱───┴─╱───┴─╱───┴──────────► Time                       │
  │                                                                 │
  │  RepeatMode.Reverse:                                            │
  │  Value                                                          │
  │    ▲                                                            │
  │  Max │     ╱╲    ╱╲    ╱╲                                       │
  │      │   ╱    ╲╱    ╲╱    ╲ (Smooth back-and-forth)             │
  │  Min └──╱──────────────────────────► Time                       │
  └─────────────────────────────────────────────────────────────────┘
```

---

## Real-World Examples

### Spinning Loader
```kotlin
@Composable
fun SpinningLoader() {
    val infiniteTransition = rememberInfiniteTransition(label = "loader")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    CircularProgressIndicator(
        modifier = Modifier.graphicsLayer { rotationZ = angle }
    )
}
```

### Pulse / Breathing Effect
```kotlin
@Composable
fun PulseBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                easing = FastOutSlowIn
            ),
            repeatMode = RepeatMode.Reverse  // ← Makes it go back and forth
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .size(12.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(Color.Red, CircleShape)
    )
}
```

### Color Breathing (Status Indicator)
```kotlin
@Composable
fun StatusIndicator(isOnline: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "status")
    val color by infiniteTransition.animateColor(
        initialValue = Color.Green,
        targetValue = Color.Green.copy(alpha = 0.3f),
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statusColor"
    )

    Box(
        modifier = Modifier
            .size(10.dp)
            .background(if (isOnline) color else Color.Gray, CircleShape)
    )
}
```

### Shimmer Loading Effect
```kotlin
@Composable
fun ShimmerBox() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f,    // Start off-screen left
        targetValue = 300f,      // End off-screen right
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.White.copy(alpha = 0.8f),
            Color.LightGray.copy(alpha = 0.6f)
        ),
        start = Offset(shimmerOffset - 100, 0f),
        end = Offset(shimmerOffset + 100, 0f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .background(shimmerBrush)
    )
}
```

### Wave Animation (Multiple offsets)
```kotlin
@Composable
fun WaveIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    // Three dots with staggered delays
    val offsets = (0..2).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -20f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 400,
                    easing = FastOutSlowIn
                ),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(index * 150)  // Stagger!
            ),
            label = "dot$index"
        )
    }

    Row {
        offsets.forEach { offset ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .graphicsLayer { translationY = offset.value }
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(horizontal = 4.dp)
            )
        }
    }
}
```

---

## Multiple Animations from One Transition

One `rememberInfiniteTransition` can drive multiple animations — they're all synchronized:

```kotlin
val transition = rememberInfiniteTransition(label = "glitch")

val offsetX by transition.animateFloat(...)
val alpha by transition.animateFloat(...)
val color by transition.animateColor(...)
```

All run from the same transition — sharing the same timeline.

---

## Stopping Infinite Animations

`rememberInfiniteTransition` animations **stop automatically** when the composable leaves composition (e.g., navigates away). You don't need to manage lifecycle.

---

## 💡 Interview Q&A

**Q: Why use `rememberInfiniteTransition` instead of a `while(true)` loop in a coroutine?**
A: `rememberInfiniteTransition` is lifecycle-aware — it stops automatically when the composable exits. A coroutine `while(true)` loop is harder to cancel, can leak, and needs careful management with `DisposableEffect` or `LaunchedEffect`.

**Q: Can `spring()` be used with `infiniteRepeatable`?**
A: No — `infiniteRepeatable` requires a duration-based spec (`tween` or `keyframes`). Spring has no fixed duration, so it can't loop predictably.

**Q: What does `StartOffset` do in `infiniteRepeatable`?**
A: It lets you stagger the start of each animation in a group. `StartOffset(300)` starts the animation 300ms into the cycle — useful for creating wave/cascade effects with multiple elements.

---

**Next:** [[07 Animation/06 AnimationSpec/06 AnimationSpec Comparison|AnimationSpec Comparison →]]
