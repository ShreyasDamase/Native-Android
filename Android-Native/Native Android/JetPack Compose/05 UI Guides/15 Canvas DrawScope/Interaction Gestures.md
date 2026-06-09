# Interaction and Gestures on Canvas

## 📌 Purpose
Because `Canvas` is fundamentally a `Spacer` with a draw modifier, it has no built-in click or drag semantics. You must use `Modifier.pointerInput` to detect precise touches, calculate hit areas manually, and handle complex gestures like rotary dragging.

## 🔧 Interaction APIs
- `detectTapGestures` (Tap, DoubleTap, LongPress, Press)
- `detectDragGestures` (Start, Drag, End, Cancel)
- `MutableInteractionSource` (Ripple and state tracking)

## 🚀 Advanced Examples

### 1. Rotary Knob Gesture (Drag to Rotate)
Converting 2D XY drag coordinates into polar angle math. This is the absolute core of vintage UI knob controls.

```kotlin
var currentAngle by remember { mutableStateOf(0f) }

Canvas(
    modifier = Modifier
        .size(150.dp)
        .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                
                val center = Offset(size.width / 2f, size.height / 2f)
                
                // Vector from center to previous position
                val prevVec = change.previousPosition - center
                // Vector from center to current position
                val currVec = change.position - center
                
                // atan2 returns radians. Convert to degrees.
                val prevAngle = atan2(prevVec.y, prevVec.x) * (180f / Math.PI.toFloat())
                val currAngle = atan2(currVec.y, currVec.x) * (180f / Math.PI.toFloat())
                
                var angleDelta = currAngle - prevAngle
                
                // Handle the 180 to -180 boundary crossing wrap-around
                if (angleDelta > 180f) angleDelta -= 360f
                if (angleDelta < -180f) angleDelta += 360f
                
                // Apply delta and constrain limits (e.g. -150 to 150 degrees)
                currentAngle = (currentAngle + angleDelta).coerceIn(-150f, 150f)
            }
        }
) {
    // Draw the knob rotated
    rotate(degrees = currentAngle + 90f) { // +90f offsets 0 degrees to point UP
        drawCircle(Color.DarkGray)
        drawLine(
            Color.White, 
            start = center, 
            end = Offset(center.x, 20f), 
            strokeWidth = 8f
        )
    }
}
```

### 2. Precise Hit Detection (Tap Areas)
Since a Canvas is just a rectangle, if you draw a circle, tapping the transparent corners of the rectangle will still trigger a tap. You must calculate distance manually.

```kotlin
var isActivated by remember { mutableStateOf(false) }

Canvas(
    modifier = Modifier
        .size(100.dp)
        .pointerInput(Unit) {
            detectTapGestures { offset ->
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f
                
                // Calculate distance from center using Pythagorean theorem
                val distFromCenter = (offset - center).getDistance()
                
                // Only trigger if tapped INSIDE the drawn circle
                if (distFromCenter <= radius) {
                    isActivated = !isActivated
                }
            }
        }
) {
    drawCircle(if (isActivated) Color.Green else Color.Red)
}
```

### 3. Press/Depress Animation (InteractionSource)
Simulating a mechanical button pressing down.
```kotlin
val interactionSource = remember { MutableInteractionSource() }
val isPressed by interactionSource.collectIsPressedAsState()

// Shrink the button slightly when pressed
val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f)

Canvas(
    modifier = Modifier
        .size(100.dp)
        .scale(scale)
        .clickable(
            interactionSource = interactionSource, 
            indication = null // Removes standard Material Ripple
        ) { 
            /* Perform action */ 
        }
) {
    drawCircle(Color.LightGray)
    // Draw inset shadow if pressed
    if (isPressed) {
        drawCircle(Color.Black.copy(alpha = 0.3f))
    }
}
```

## ⚠️ Common Gotchas
- **`change.consume()`**: Always call `consume()` inside drag gestures. If you don't, parent containers (like a `LazyColumn` or `ScrollableBox`) will steal the gesture, stopping your knob rotation and scrolling the screen instead.
- **Math.PI Type Inference**: `Math.PI` returns a Double. Compose uses Floats. Always cast to `toFloat()` to avoid type mismatch errors.

## 💡 Interview Q&A
**Q: How do you prevent a parent `ScrollState` from intercepting vertical drag events meant for a custom Canvas slider?**
A: Calling `change.consume()` inside `detectDragGestures` is usually enough. However, if the gesture starts in an ambiguous way, you may need to wrap your modifier in `pointerInteropFilter { true }` (legacy) or ensure `detectDragGestures` is configured correctly so Compose registers the gesture claim.
