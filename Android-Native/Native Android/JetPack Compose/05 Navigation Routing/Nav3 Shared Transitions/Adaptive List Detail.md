# Adaptive List-Detail Transition

## 📌 Purpose
On phones, a list and its detail view are separate screens (single-pane). On tablets or foldables, they are displayed side-by-side (multi-pane). Nav3 handles this natively via `SceneStrategy`. However, integrating `SharedElement` transitions into an adaptive layout requires careful state checking, as elements shouldn't try to morph when both the source and destination are visible simultaneously on a wide screen.

> [!IMPORTANT]
> If a shared element exists on both sides of a visible multi-pane layout, the framework will throw an error or behave unpredictably. You must disable the `sharedElement` modifier on wide screens.

## ✅ Full Working Example

### Nav3 Setup with SceneStrategy

```kotlin
import androidx.compose.animation.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.navigation3.*
import androidx.navigation3.ui.*
import androidx.window.core.layout.WindowWidthSizeClass
import kotlinx.serialization.Serializable

@Serializable data object ListRoute : NavKey
@Serializable data class DetailRoute(val id: Int) : NavKey

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AdaptiveSharedTransitionApp() {
    SharedTransitionLayout {
        val backStack = remember { mutableStateListOf<NavKey>(ListRoute) }
        
        NavDisplay(
            backStack = backStack,
            // Automatically switches between single and multi-pane based on window size
            sceneStrategy = rememberListDetailSceneStrategy(),
            
            entryProvider = entryProvider {
                // Mark this entry as the List Pane
                entry<ListRoute>(
                    metadata = mapOf(ListDetailSceneStrategy.listPane to true)
                ) {
                    ListScreen(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                        onItemClick = { id -> backStack.add(DetailRoute(id)) }
                    )
                }
                
                // Mark this entry as the Detail Pane
                entry<DetailRoute>(
                    metadata = mapOf(ListDetailSceneStrategy.detailPane to true)
                ) { route ->
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
```

### Safely Applying Modifiers

In your `ListScreen` and `DetailScreen`, check the window size class before applying shared modifiers.

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ListScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onItemClick: (Int) -> Unit
) {
    // Determine if we are on a compact screen (phone)
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isCompact = windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

    // ... inside your lazy list item:
    val product = sampleProducts.first()

    with(sharedTransitionScope) {
        
        // 1. Conditionally apply sharedElement
        val imageModifier = if (isCompact) {
            Modifier.sharedElement(
                state = rememberSharedContentState(key = "image-${product.id}"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        } else {
            Modifier // Do nothing on wide screens
        }

        Image(
            painter = painterResource(id = product.imageRes),
            contentDescription = null,
            modifier = Modifier.size(80.dp).then(imageModifier)
        )
        
        // 2. Conditionally apply sharedBounds
        val titleModifier = if (isCompact) {
            Modifier
                .skipToLookaheadSize()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "title-${product.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds
                )
        } else {
            Modifier // Do nothing on wide screens
        }

        Text(
            text = product.name,
            modifier = titleModifier
        )
    }
}
```

## ⚠️ Common Gotchas

- **Shared State Collisions:** If `isCompact` is false (tablet mode), both the list and detail panes render at the same time. If they both register a `sharedElement` with the key `"image-123"`, Compose doesn't know which one is the "source of truth", breaking the UI. The conditional check avoids this entirely.
- **rememberListDetailSceneStrategy Behavior:** This built-in strategy expects you to provide exactly one list pane and one detail pane in the metadata. If it detects a wide screen, it automatically places them side-by-side. If it detects a compact screen, it behaves like `SinglePaneSceneStrategy` and animates them pushing on top of each other.

## 💡 Interview Q&A

**Q: Why do we have to conditionally disable `sharedElement` on tablets?**
A: `SharedElement` works by capturing a UI component from the exiting screen and animating it to the position of the entering screen. On a tablet using `ListDetailSceneStrategy`, there is no exiting screen—both the list and the detail are rendered side-by-side simultaneously. Since the elements never leave the screen, a transition between them doesn't make logical or visual sense.
