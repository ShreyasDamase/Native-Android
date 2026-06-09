# NavDisplay Custom Transitions

## 📌 Purpose
While shared elements focus on specific UI components, screen transitions define how the overall page enters and exits. Nav3 allows you to define these transitions globally (for all routes) or per-destination using metadata.

> [!NOTE]
> All transitions in Nav3 are built using `AnimatedContentTransitionScope<*>.() -> ContentTransform`. You combine an `EnterTransition` and an `ExitTransition` using the `togetherWith` infix function.

## 🚀 Global Transitions
You configure default animations directly on the `NavDisplay` composable.

```kotlin
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation3.NavDisplay
import androidx.navigation3.ui.entryProvider

@Composable
fun AppWithGlobalTransitions(backStack: List<Any>, onBack: () -> Unit) {
    NavDisplay(
        backStack = backStack,
        // Forward navigation: new screen slides in from right, old screen slides out left
        transitionSpec = {
            fadeIn(tween(300)) + slideInHorizontally { it } togetherWith
            fadeOut(tween(300)) + slideOutHorizontally { -it }
        },
        // Back navigation: old screen slides in from left, current screen slides out right
        popTransitionSpec = {
            fadeIn(tween(300)) + slideInHorizontally { -it } togetherWith
            fadeOut(tween(300)) + slideOutHorizontally { it }
        },
        // Predictive back gesture preview
        predictivePopTransitionSpec = {
            fadeIn(tween(300)) togetherWith fadeOut(tween(300))
        },
        entryProvider = entryProvider { /* ... */ },
        onBack = onBack
    )
}
```

## 🎯 Per-Destination Transition Overrides
Sometimes specific screens (like a Dialog-like screen or a Bottom Sheet replacement) need unique animations. Nav3 allows you to attach metadata to an `entry` using standard keys.

```kotlin
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.navigation3.ui.entryProvider
import androidx.navigation3.NavDisplay

val customEntryProvider = entryProvider {
    // Standard screen
    entry<HomeRoute> { HomeScreen() }

    // Screen with overridden transitions
    entry<DialogLikeRoute>(
        metadata = mapOf(
            NavDisplay.TransitionKey to ContentTransform(
                targetContentEnter = fadeIn(tween(200)) + scaleIn(initialScale = 0.8f),
                initialContentExit = fadeOut(tween(200)) + scaleOut(targetScale = 0.8f)
            ),
            NavDisplay.PopTransitionKey to ContentTransform(
                targetContentEnter = fadeIn(tween(200)) + scaleIn(initialScale = 0.8f),
                initialContentExit = fadeOut(tween(200)) + scaleOut(targetScale = 0.8f)
            )
        )
    ) { 
        DialogLikeScreen() 
    }
}
```

## 🔧 ContentTransform & SizeTransform
When screens have different heights (e.g., bottom sheets), animating the container bounds can be jarring. You can compose `using SizeTransform` to smoothly interpolate the bounds.

```kotlin
val sizeTransformSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
    (fadeIn() togetherWith fadeOut()).using(
        SizeTransform(clip = false) { initialSize, targetSize ->
            tween(durationMillis = 300)
        }
    )
}
```

## ⚠️ Common Gotchas

- **Directionality:** In `slideInHorizontally`, the lambda receives the full width of the container (`it`). Returning `it` means start at the right edge. Returning `-it` means start at the left edge. Make sure your `popTransitionSpec` does the exact inverse of your `transitionSpec` to create a natural flow.
- **Z-Index:** By default, the entering screen in `AnimatedContent` is drawn *on top* of the exiting screen. If you want the exiting screen to render on top (e.g., sliding a drawer shut), you might need to manipulate the `zIndex` modifier directly on the screen's root composable based on navigation state.

## 💡 Interview Q&A

**Q: How does Nav3 route specific transitions differently from Nav2?**
A: In Nav2, you defined transitions in the `composable` route DSL directly (`enterTransition = { ... }`). In Nav3, transitions are decoupled into the `metadata` map using `NavDisplay.TransitionKey`. This keeps the API multiplatform-friendly and abstracts away Android-specific animation dependencies from the core routing logic.
