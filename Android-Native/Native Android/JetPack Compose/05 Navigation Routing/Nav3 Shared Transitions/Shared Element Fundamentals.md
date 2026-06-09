# Shared Element Fundamentals

## 📌 Purpose
Shared element transitions provide seamless visual continuity between screens by animating components that exist in both the initial and target states. In Jetpack Compose, this is achieved using `SharedTransitionLayout` and its specialized modifiers.

> [!NOTE]
> All shared transition APIs are currently experimental and require `@OptIn(ExperimentalSharedTransitionApi::class)`.

## 🔧 Core APIs & Function Signatures

### 1. `SharedTransitionLayout`
The root layout that provides the coordinate space and `SharedTransitionScope` needed for shared elements to animate across screens.

```kotlin
@ExperimentalSharedTransitionApi
@Composable
fun SharedTransitionLayout(
    modifier: Modifier = Modifier,
    content: @Composable SharedTransitionScope.() -> Unit
)
```

### 2. `Modifier.sharedElement()`
Used when the **exact same content** (like an image or icon) moves from one screen to another.

```kotlin
@ExperimentalSharedTransitionApi
fun Modifier.sharedElement(
    state: SharedContentState,
    animatedVisibilityScope: AnimatedVisibilityScope,
    boundsTransform: BoundsTransform = DefaultBoundsTransform,
    placeHolderSize: SharedTransitionScope.PlaceholderSize = SharedTransitionScope.PlaceholderSize.ContentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: OverlayClip = ParentClip
): Modifier
```

### 3. `Modifier.sharedBounds()`
Used when the **container** visually morphs, but the internal content differs (e.g., a small card expanding into a large detailed view).

```kotlin
@ExperimentalSharedTransitionApi
fun Modifier.sharedBounds(
    sharedContentState: SharedContentState,
    animatedVisibilityScope: AnimatedVisibilityScope,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    boundsTransform: BoundsTransform = DefaultBoundsTransform,
    resizeMode: SharedTransitionScope.ResizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds,
    placeHolderSize: SharedTransitionScope.PlaceholderSize = SharedTransitionScope.PlaceholderSize.contentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: OverlayClip = ParentClip
): Modifier
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `state` / `sharedContentState` | `SharedContentState` | — | Required. Holds the matching `key` that links the initial and target elements. Created via `rememberSharedContentState(key)`. |
| `animatedVisibilityScope` | `AnimatedVisibilityScope` | — | Required. Links the element to the screen's entry/exit transition. In Nav3, grab this using `LocalNavAnimatedContentScope.current`. |
| `boundsTransform` | `BoundsTransform` | `DefaultBoundsTransform` | Optional. Defines the spatial animation spec (e.g., arc paths, specific timing like `tween(400)`). |
| `resizeMode` | `ResizeMode` | `ScaleToBounds` | Optional. Only for `sharedBounds`. Controls if content scales graphically (`ScaleToBounds`) or physically remeasures (`RemeasureToBounds`). |
| `enter` / `exit` | `EnterTransition` / `ExitTransition` | `fadeIn()` / `fadeOut()` | Optional. For `sharedBounds`, how the differing internal contents crossfade. |
| `renderInOverlayDuringTransition`| `Boolean` | `true` | Optional. If true, the animating element is drawn in a top-level overlay so it isn't clipped by its parent's bounds. |
| `zIndexInOverlay` | `Float` | `0f` | Optional. Sort order within the overlay. |
| `clipInOverlayDuringTransition` | `OverlayClip` | `ParentClip` | Optional. Defines how the element is clipped during the animation. |

## ✅ Basic Example

```kotlin
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation3.*
import androidx.navigation3.ui.*

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppWithSharedTransitions() {
    SharedTransitionLayout {
        val backStack = remember { mutableStateListOf<NavKey>(ListRoute) }
        
        NavDisplay(
            backStack = backStack,
            entryProvider = entryProvider {
                entry<ListRoute> {
                    ListScreen(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                        onNavigate = { id -> backStack.add(DetailRoute(id)) }
                    )
                }
                entry<DetailRoute> { route ->
                    DetailScreen(
                        id = route.id,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
            },
            onBack = { backStack.removeLastOrNull() }
        )
    }
}

// Ensure the SAME KEY is used on both screens!
// List screen: `rememberSharedContentState(key = "image-${item.id}")`
// Detail screen: `rememberSharedContentState(key = "image-${id}")`
```

## 🚀 Advanced Modifiers & Mechanics

### `Modifier.skipToLookaheadSize()`
Prevents unwanted reflows (like text wrapping weirdly) during `sharedBounds` animations. It forces the layout to measure the element at its final target size *before* animating it.

```kotlin
Text(
    text = title,
    modifier = Modifier
        .skipToLookaheadSize() // MUST be placed BEFORE sharedBounds
        .sharedBounds(
            sharedContentState = rememberSharedContentState(key = "title-$id"),
            animatedVisibilityScope = animatedVisibilityScope
        )
)
```

### `ResizeMode` Deep Dive
When using `sharedBounds`, you must choose how the bounds morph:
- `ScaleToBounds` (Default): Measures the content at its final size, then applies a graphical scaling transform during the animation. **Best for Text** to avoid jittery reflows.
- `RemeasureToBounds`: Recalculates the layout constraints and remeasures the content every single frame. **Best for layouts where aspect ratios change** dramatically (e.g., Row morphing into a Column).

### `Modifier.renderInSharedTransitionScopeOverlay()`
Sometimes you have non-shared UI (like a bottom app bar or a sticky header) that you want to render on top of the transition overlay so it isn't obscured by expanding cards.

```kotlin
BottomAppBar(
    modifier = Modifier.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f)
)
```

### Custom `BoundsTransform`
You can define how the shared bounds travel through space.

```kotlin
val arcBoundsTransform = BoundsTransform { initialBounds, targetBounds ->
    keyframes {
        durationMillis = 500
        // Use an arc path or custom easing
    }
}

Modifier.sharedElement(
    state = rememberSharedContentState(key = "icon"),
    animatedVisibilityScope = animatedVisibilityScope,
    boundsTransform = arcBoundsTransform
)
```

## ⚠️ Common Gotchas

- **Mismatched Keys:** The string passed to `key` in `rememberSharedContentState(key)` must perfectly match between the start and end destinations.
- **Wrong Scope Source:** Always use `LocalNavAnimatedContentScope.current` inside `entryProvider`. Do not attempt to cast or use `this` from the `entry` lambda, it won't work in Nav3.
- **Modifier Ordering:** `skipToLookaheadSize()` must be applied *before* (above) `sharedBounds()` in the modifier chain to work correctly.
- **Forgetting `renderInOverlayDuringTransition`:** If your shared element seems to get cut off by its parent container (like a Card) during the animation, ensure `renderInOverlayDuringTransition = true` (it is by default, but verify parent clipping).

## 💡 Interview Q&A

**Q: What is the difference between `sharedElement` and `sharedBounds`?**
A: `sharedElement` is used when the exact same content is migrating (like an image). `sharedBounds` is used when the *container* morphs from one shape to another, but the internal contents of the container crossfade (e.g., a compact row turning into a detailed column).

**Q: Why might text look jittery during a shared bounds transition, and how do you fix it?**
A: Text jitters because its constraints are changing every frame, causing line breaks to reflow constantly. Fix it by using `ResizeMode.ScaleToBounds` on the `sharedBounds` modifier and adding `Modifier.skipToLookaheadSize()` right before it, which forces the text to measure at its final size and graphically scale instead.

**Q: How do you access `AnimatedVisibilityScope` inside Nav3?**
A: Nav3 injects it implicitly via a CompositionLocal. You access it by reading `LocalNavAnimatedContentScope.current` inside the `entry` DSL.
