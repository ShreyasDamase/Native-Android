# Canvas Performance

## 📌 Purpose
While Canvas drawing is incredibly fast, doing it wrong can cause memory churn (allocating objects every frame) and excessive recomposition (invalidating layout trees unnecessarily). Following strict performance rules is mandatory for smooth 60/120fps animations.

## 🥇 The Golden Rules of Canvas

### 1. Never Allocate Objects in Draw Scope
The drawing lambda is called *every single frame* during animations.
```kotlin
// 🚫 BAD: Creates a new object 60 times a second
Canvas(Modifier.size(100.dp)) {
    val path = Path() 
    val brush = Brush.linearGradient(...)
    drawPath(path, brush)
}

// ✅ GOOD: Uses remember
val path = remember { Path() }
val brush = remember { Brush.linearGradient(...) }
Canvas(Modifier.size(100.dp)) {
    drawPath(path, brush)
}
```

### 2. Use `drawWithCache` for Size-Dependent Objects
`remember` cannot access `size`. If your gradient needs to match the canvas width, use `drawWithCache`.
```kotlin
Modifier.drawWithCache {
    // This block ONLY runs when component size changes
    val brush = Brush.linearGradient(
        colors = listOf(Color.Red, Color.Blue),
        end = Offset(size.width, size.height) // Depends on size
    )
    
    // This block runs every frame
    onDrawBehind {
        drawRect(brush)
    }
}
```

### 3. Read Animated State Inside the Draw Lambda
State reads trigger phase invalidation. If you read state outside the lambda, you trigger Recomposition. If you read inside the lambda, you only trigger the Draw Phase.
```kotlin
// 🚫 BAD: Recomposes the parent layout every frame
val animatedAngle by animateFloatAsState(target)
Canvas(Modifier.size(100.dp)) {
    rotate(animatedAngle) { ... }
}

// ✅ GOOD: Only invalidates Draw Phase
val animatedAngle by animateFloatAsState(target)
Canvas(Modifier.size(100.dp)) {
    // animatedAngle is READ inside the Canvas lambda block
    rotate(animatedAngle) { ... }
}
```

## 🧠 Advanced Performance Tactics

### Manual Invalidation Control
If you are writing a custom low-level `DrawModifierNode`, you can set `shouldAutoInvalidate = false` to stop Compose from redrawing when parameters change, giving you manual control by calling `invalidateDraw()`.

### Native Canvas Interop Overhead
Calling `drawIntoCanvas { ... }` to access `android.graphics.Canvas` forces the Compose renderer to synchronize with the classic Android View system's render node. Doing this heavily, especially with `saveLayer` and `BlurMaskFilter`, can drop frames on older devices. Only use it when Compose's native `DrawScope` doesn't support the feature (like custom shadows on paths).

## 💡 Interview Q&A
**Q: Explain the Compose rendering phases and how `Canvas` optimizes them.**
A: The three phases are:
1. **Composition:** What to show (building the tree).
2. **Layout:** Where to place it (measuring x,y,width,height).
3. **Drawing:** Rendering the pixels.
When you animate a state value (like an angle float) and read it *only* inside a `Canvas` lambda, Compose completely skips Phase 1 and Phase 2. It immediately goes to Phase 3 and just repaints the pixels. This makes it vastly cheaper than animating `Modifier.padding()` or `Modifier.offset()`, which force Layout recalculations.
