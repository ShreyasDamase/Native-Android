# Animations with Canvas

## 📌 Purpose
Animations in Canvas are extremely performant because reading Compose `State` inside the `DrawScope` only invalidates the *Draw Phase*, completely bypassing the Composition and Layout phases.

## 🔧 Core Animation APIs

1. **`animateFloatAsState`**: Fire-and-forget state-driven animation.
2. **`Animatable`**: Coroutine-driven animation with control over start/stop/velocity.
3. **`rememberInfiniteTransition`**: Continuous looping animations (pulsing, spinning, shimmering).

## 🚀 Advanced Examples

### 1. Animated Analog Meter Sweep
Using `animateFloatAsState` with a bouncy spring specification for realism.
```kotlin
var isMax by remember { mutableStateOf(false) }
val sweepAngle by animateFloatAsState(
    targetValue = if (isMax) 180f else 0f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
)

Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Canvas(modifier = Modifier.size(200.dp, 100.dp)) {
        // Draw meter background...
        
        // Draw Needle
        rotate(degrees = sweepAngle - 90f, pivot = Offset(center.x, size.height)) {
            drawLine(
                color = Color.Red,
                start = Offset(center.x, size.height),
                end = Offset(center.x, 20f),
                strokeWidth = 4f
            )
        }
    }
    
    Button(onClick = { isMax = !isMax }) { Text("Toggle Throttle") }
}
```

### 2. Pulsing LED Glow (Infinite Transition)
```kotlin
val infiniteTransition = rememberInfiniteTransition()
val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.2f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
        animation = tween(800, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse // Pulses in and out
    )
)

Canvas(modifier = Modifier.size(50.dp)) {
    // Outer glow
    drawCircle(
        color = Color.Red.copy(alpha = glowAlpha),
        radius = size.width / 2f
    )
    // Solid core
    drawCircle(
        color = Color.Red,
        radius = size.width / 4f
    )
}
```

### 3. Shimmer Loading Effect (drawWithCache + infiniteTransition)
```kotlin
val infiniteTransition = rememberInfiniteTransition()
val shimmerOffset by infiniteTransition.animateFloat(
    initialValue = -1f,
    targetValue = 2f,
    animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Restart)
)

Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(100.dp)
        .drawWithCache {
            val gradient = Brush.linearGradient(
                colors = listOf(
                    Color.LightGray.copy(alpha = 0.3f),
                    Color.White.copy(alpha = 0.8f),
                    Color.LightGray.copy(alpha = 0.3f)
                ),
                start = Offset(size.width * shimmerOffset, 0f),
                end = Offset(size.width * shimmerOffset + size.width * 0.5f, size.height)
            )
            
            onDrawBehind {
                drawRect(gradient)
            }
        }
)
```

## ⚠️ Common Gotchas
- **Extracting State Readings:** Do not read the animated state *outside* the `Canvas` block or `onDrawBehind` block. If you do, it will trigger recomposition of the whole component every frame, killing performance.
  
  *Bad:* `val myColor = if(state) Color.Red else Color.Blue; Canvas { draw(myColor) }`
  *Good:* `val animatedColor by animateColor...; Canvas { draw(animatedColor) }`

## 💡 Interview Q&A
**Q: Why is `Canvas` animation cheaper than animating the `Modifier.size()` of a Box?**
A: Animating `Modifier.size()` forces Compose to recalculate the Layout constraints of the Box and potentially all its parent/sibling components, triggering the Layout Phase 60 times a second. Canvas animations only read state in the Draw Phase, meaning Compose just repaints the existing pixels within the already-determined bounds.
