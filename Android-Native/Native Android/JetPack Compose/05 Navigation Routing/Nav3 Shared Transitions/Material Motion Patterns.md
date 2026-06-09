# Material Motion Patterns

## 📌 Purpose
Material Design 3 defines strict motion patterns to help users understand the spatial relationships between screens. These patterns can be directly translated into Compose `ContentTransform` definitions and used globally in Nav3's `NavDisplay`.

> [!TIP]
> Keep these definitions in an `AnimationSpecs.kt` file and reuse them across your app to maintain a consistent feel.

## 🔧 The 6 Material Motion Patterns

### 1. FadeThrough
**Use case:** Navigating between completely unrelated screens (e.g., Bottom Navigation tabs).
**Effect:** Exiting screen fades out and scales down slightly. Entering screen fades in and scales up.

```kotlin
import androidx.compose.animation.*
import androidx.compose.animation.core.tween

val fadeThroughSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
    fadeIn(tween(300, delayMillis = 150)) +
    scaleIn(initialScale = 0.92f, animationSpec = tween(300, delayMillis = 150)) togetherWith
    fadeOut(tween(150))
}
```

### 2. SharedAxisX (Horizontal)
**Use case:** Navigation with a clear left/right relationship (e.g., Wizard steps, horizontal pagers).
**Effect:** Screens slide horizontally while crossfading.

```kotlin
val sharedAxisXForward: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
    fadeIn(tween(300)) + slideInHorizontally { it / 3 } togetherWith
    fadeOut(tween(300)) + slideOutHorizontally { -it / 3 }
}

val sharedAxisXBack: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
    fadeIn(tween(300)) + slideInHorizontally { -it / 3 } togetherWith
    fadeOut(tween(300)) + slideOutHorizontally { it / 3 }
}
```

### 3. SharedAxisY (Vertical)
**Use case:** Up/down navigation relationships (e.g., showing/hiding a persistent filter sheet, stepping through vertical forms).

```kotlin
val sharedAxisYUp: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
    fadeIn(tween(300)) + slideInVertically { it / 3 } togetherWith
    fadeOut(tween(300)) + slideOutVertically { -it / 3 }
}

val sharedAxisYDown: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
    fadeIn(tween(300)) + slideInVertically { -it / 3 } togetherWith
    fadeOut(tween(300)) + slideOutVertically { it / 3 }
}
```

### 4. SharedAxisZ (Depth)
**Use case:** Parent-to-child drill-down navigation (e.g., Settings menu -> Sub-setting menu).
**Effect:** The new screen scales up from the center, giving the illusion of moving deeper into the app.

```kotlin
val sharedAxisZIn: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
    fadeIn(tween(300)) + scaleIn(initialScale = 0.8f) togetherWith
    fadeOut(tween(300)) + scaleOut(targetScale = 1.1f)
}

val sharedAxisZOut: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
    fadeIn(tween(300)) + scaleIn(initialScale = 1.1f) togetherWith
    fadeOut(tween(300)) + scaleOut(targetScale = 0.8f)
}
```

### 5. ContainerTransform (Shared Element Bounds)
**Use case:** Expanding a Card into a Detail View.
**Implementation:** This relies on `Modifier.sharedBounds()`. It is NOT a `ContentTransform` lambda. Refer to `02-Card-to-Detail.md` for full implementation details.

### 6. ElevationScale
**Use case:** A Floating Action Button (FAB) or dialog expanding to take over the screen.
**Effect:** A simple zoom in from the center with a background fade.

```kotlin
val elevationScaleIn: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
    fadeIn(tween(300)) + scaleIn(initialScale = 0.92f) togetherWith
    fadeOut(tween(300))
}

val elevationScaleOut: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
    fadeIn(tween(300)) togetherWith
    fadeOut(tween(300)) + scaleOut(targetScale = 0.92f)
}
```

## 📋 Decision Guide: Which Pattern?

| Scenario | Recommended Pattern |
|---|---|
| User clicks a Bottom Nav item | **FadeThrough** |
| User clicks a Card with an image | **ContainerTransform** (`sharedBounds`) |
| User clicks "Next" in a setup wizard | **SharedAxisX** |
| User clicks a Setting item to go deeper | **SharedAxisZ** |
| User clicks a FAB to create a new post | **ElevationScale** |

## ⚠️ Common Gotchas
- **Overshooting:** When using `slideInHorizontally { it / 3 }`, notice we divide the width by 3. This is intentional Material Design behavior. Fully sliding the screen (`it`) looks too aggressive for modern apps.
- **Delays:** In `FadeThrough`, the entering animation uses `delayMillis = 150`. This ensures the old screen has time to fade out before the new screen starts fading in, preventing a messy double-exposure effect.
