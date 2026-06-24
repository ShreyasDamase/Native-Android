# animate*AsState — Animate Any Value

> [!NOTE]
> This is the **simplest** and most common animation API in Compose. If you have a value that changes based on state, use this. It's declarative, automatic, and handles cancellation for you.

---

## What is `animate*AsState`?

It's a family of functions that return a `State<T>` whose value **animates smoothly** whenever the `targetValue` changes.

You just say: **"I want this value to equal X"** — and Compose smoothly animates to X from whatever it was before.

```kotlin
// Simple example: animate button size on press
var isPressed by remember { mutableStateOf(false) }

val size by animateDpAsState(
    targetValue = if (isPressed) 120.dp else 80.dp,
    label = "buttonSize"
)
```

```text
  ┌─────────────────────────────────────────────────────────────────┐
  │                        DATA FLOW                                │
  │                                                                 │
  │  State variable (e.g., isPressed) ──► Re-evaluates targetValue  │
  │                                                │                │
  │                                                ▼                │
  │  animate*AsState(targetValue)  ◄───────────────┘                │
  │         │                                                       │
  │         ▼                                                       │
  │  Returns State<T> (Value ticks 80.dp -> ... -> 120.dp)          │
  │         │                                                       │
  │         ▼                                                       │
  │  UI reads value via delegate 'by' ──► Triggers Redraw           │
  └─────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────┐
  │                   INTERRUPTION BEHAVIOR                         │
  │                                                                 │
  │  Target 100f  ───────────────────────┐                          │
  │                                      │ (Interrupt: Target       │
  │                                      │  changes to 0f mid-way)  │
  │  Value 50f   ─────────╱╲             │                          │
  │                       │  ╲           ▼                          │
  │  Value 0f     ────────┴───┴──────────┴────────────────► Time    │
  │              Starts   Reverses smoothly from current            │
  │                       value (50f) - no frame jump               │
  └─────────────────────────────────────────────────────────────────┘
```

---

## The Full Family

| Function | Animates | Type |
|----------|----------|------|
| `animateFloatAsState` | Float values | `Float` |
| `animateDpAsState` | Density-independent pixels | `Dp` |
| `animateColorAsState` | Colors | `Color` |
| `animateIntAsState` | Integer values | `Int` |
| `animateIntOffsetAsState` | Integer 2D position | `IntOffset` |
| `animateIntSizeAsState` | Integer 2D size | `IntSize` |
| `animateOffsetAsState` | Float 2D position | `Offset` |
| `animateRectAsState` | Rectangular region | `Rect` |
| `animateSizeAsState` | Float 2D size | `Size` |
| `animateValueAsState` | **Any custom type** | `T` |

---

## Full API Signature

```kotlin
@Composable
fun animateFloatAsState(
    targetValue: Float,                              // The destination value
    animationSpec: AnimationSpec<Float> = spring(),  // HOW it animates
    visibilityThreshold: Float = 0.01f,             // Stop threshold
    label: String = "FloatAnimation",               // Debug label
    finishedListener: ((Float) -> Unit)? = null     // Called when done
): State<Float>
```

> The same parameters apply to all `animate*AsState` functions, just with different types.

---

## Parameters Explained

### `targetValue`
The **destination** — the value you want to reach.
- When this changes, the animation starts from the **current animated value** (not the old target)
- This means if you reverse mid-animation, it smoothly reverses from wherever it currently is

```kotlin
val alpha by animateFloatAsState(
    targetValue = if (isVisible) 1f else 0f  // ← this is the target
)
```

### `animationSpec`
**HOW** the animation moves — the physics/timing.
- Default: `spring()` (slightly bouncy, natural feel)
- Options: `tween(...)`, `spring(...)`, `snap()`, `keyframes { }`, `repeatable(...)`

```kotlin
val scale by animateFloatAsState(
    targetValue = if (isSelected) 1.2f else 1f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
)
```

### `visibilityThreshold`
The value difference below which the animation is considered **finished**.
- For Float: `0.01f` (stops when within 0.01 of target)
- For Dp: `1.dp` (stops when within 1dp)
- Usually you never need to change this

### `label`
A **string for debugging** — shows up in the Animation Inspector in Android Studio.
- Always provide a meaningful label!
- Helps you identify which animation is which in the inspector

### `finishedListener`
A callback called **when the animation completes**.
- Called with the final value
- NOT called if the animation is interrupted by a new target

```kotlin
val offset by animateDpAsState(
    targetValue = if (isOpen) 0.dp else (-200).dp,
    finishedListener = { finalValue ->
        if (finalValue == (-200).dp) {
            // Drawer fully closed, dispose something
        }
    }
)
```

---

## How to Use the Animated Value

The returned `State<T>` is read by using Kotlin's `by` delegate — Compose automatically tracks it and redraws when it changes.

```kotlin
val backgroundColor by animateColorAsState(
    targetValue = if (isDark) DarkBackground else LightBackground,
    label = "bgColor"
)

// Use it directly in the modifier
Box(modifier = Modifier.background(backgroundColor))
```

> [!IMPORTANT]
> `animate*AsState` returns `State<T>`, not `T`. Using `by` unwraps it automatically. Without `by`, you'd write `.value` everywhere: `backgroundColor.value`.

---

## Complete Real Example

```kotlin
@Composable
fun AnimatedCard(isSelected: Boolean) {
    // Animate multiple properties simultaneously
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 16.dp else 4.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "cardElevation"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
        animationSpec = tween(durationMillis = 300),
        label = "borderColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    Card(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        // content...
    }
}
```

---

## When to Use animate*AsState vs Alternatives

| Scenario | Use |
|----------|-----|
| 1 value, triggered by state | `animate*AsState` ✅ |
| 3+ values, same state trigger | `updateTransition` ✅ |
| Gesture-controlled | `Animatable` ✅ |
| Infinite loop | `rememberInfiniteTransition` ✅ |
| Want to wait for animation to finish | `Animatable.animateTo()` ✅ |

---

## Common Mistakes

### ❌ Not providing a label
```kotlin
// Bad - hard to debug
val x by animateFloatAsState(targetValue = 1f)

// Good
val x by animateFloatAsState(targetValue = 1f, label = "scaleX")
```

### ❌ Using for gesture-driven animation
```kotlin
// Bad - can't cancel mid-animation during gesture
val x by animateFloatAsState(targetValue = dragX) // laggy!

// Good
val x = remember { Animatable(0f) }
// Use x.snapTo(dragX) during drag
```

### ❌ Animating layout-affecting properties unnecessarily
```kotlin
// Bad - triggers layout every frame
val padding by animateDpAsState(targetValue = if (selected) 16.dp else 0.dp)
Modifier.padding(padding)

// Better for visual-only - use graphicsLayer scale
val scale by animateFloatAsState(targetValue = if (selected) 1.1f else 1f)
Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
```

---

## animateValueAsState — Custom Types

Use this when you need to animate a type not in the standard list.

```kotlin
data class MyData(val width: Float, val height: Float)

// Define how MyData converts to animation vector
val MyDataConverter = object : TwoWayConverter<MyData, AnimationVector2D> {
    override val convertToVector: (MyData) -> AnimationVector2D = {
        AnimationVector2D(it.width, it.height)
    }
    override val convertFromVector: (AnimationVector2D) -> MyData = {
        MyData(it.v1, it.v2)
    }
}

@Composable
fun AnimatedMyData() {
    var target by remember { mutableStateOf(MyData(100f, 100f)) }
    val animated by animateValueAsState(
        targetValue = target,
        typeConverter = MyDataConverter,
        label = "myData"
    )
}
```

---

## 💡 Interview Q&A

**Q: What happens if `targetValue` changes while the animation is already running?**
A: Compose seamlessly starts animating from the **current animated value** (not the original start or old target) toward the new target. This prevents jarring jumps mid-animation.

**Q: What's the default `animationSpec` for `animate*AsState`?**
A: A `spring()` with default damping ratio and stiffness — which gives a natural, slightly bouncy feel.

**Q: Can `animate*AsState` animate gesture positions?**
A: Technically yes, but it's not ideal because you can't cancel or interrupt it mid-gesture. Use `Animatable` for gesture-driven animations instead.

---

**Next:** [[07 Animation/03 High Level APIs/02 AnimatedVisibility|AnimatedVisibility →]]
