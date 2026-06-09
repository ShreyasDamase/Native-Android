# Image Gallery Transition

## 📌 Purpose
The Image Gallery pattern handles transitioning from a dense grid of images to a single, fullscreen immersive image viewer. The main challenge here is dealing with `ContentScale` changes: grid items typically use `ContentScale.Crop` (to fit perfect squares), while the fullscreen view uses `ContentScale.Fit` (to show the entire image without distortion).

> [!NOTE]
> The Compose `sharedElement` modifier gracefully handles the transformation between different `ContentScale` types automatically.

## ✅ Full Working Example

### 1. Data Model
```kotlin
data class GalleryImage(val id: String, val resId: Int)

val sampleGallery = listOf(
    GalleryImage("img1", android.R.drawable.ic_menu_camera),
    GalleryImage("img2", android.R.drawable.ic_menu_gallery),
    GalleryImage("img3", android.R.drawable.ic_menu_mapmode),
    GalleryImage("img4", android.R.drawable.ic_menu_manage)
)
```

### 2. Nav3 App Setup

```kotlin
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation3.*
import androidx.navigation3.ui.*
import kotlinx.serialization.Serializable

@Serializable data object GalleryRoute : NavKey
@Serializable data class FullscreenRoute(val imageId: String) : NavKey

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ImageGalleryApp() {
    SharedTransitionLayout {
        val backStack = remember { mutableStateListOf<NavKey>(GalleryRoute) }
        
        NavDisplay(
            backStack = backStack,
            entryProvider = entryProvider {
                entry<GalleryRoute> {
                    GalleryScreen(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                        onImageClick = { id -> backStack.add(FullscreenRoute(id)) }
                    )
                }
                entry<FullscreenRoute> { route ->
                    FullscreenImageScreen(
                        imageId = route.imageId,
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

### 3. Gallery Grid Screen

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onImageClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(sampleGallery, key = { it.id }) { image ->
            with(sharedTransitionScope) {
                Image(
                    painter = painterResource(id = image.resId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop, // Crop to fit the square grid cell
                    modifier = Modifier
                        .aspectRatio(1f)
                        .sharedElement(
                            state = rememberSharedContentState(key = "gallery-${image.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> tween(400) }
                        )
                        .clickable { onImageClick(image.id) }
                )
            }
        }
    }
}
```

### 4. Fullscreen Image Screen

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FullscreenImageScreen(
    imageId: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit
) {
    val image = sampleGallery.first { it.id == imageId }

    with(sharedTransitionScope) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Fade in the black background during transition
                .background(Color.Black)
        ) {
            Image(
                painter = painterResource(id = image.resId),
                contentDescription = null,
                contentScale = ContentScale.Fit, // Fit to screen without cropping
                modifier = Modifier
                    .fillMaxSize()
                    .sharedElement(
                        state = rememberSharedContentState(key = "gallery-${image.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> tween(400) }
                    )
            )

            // Close button overlay (not part of the shared element)
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    // Render UI on top of transition if needed
                    .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}
```

## ⚠️ Common Gotchas

- **Black Background Fade:** Ensure that the `Box` wrapping the fullscreen image has a `background(Color.Black)`. Because of `NavDisplay`'s default crossfade transitions, the black background will naturally fade in behind the expanding image, creating a polished cinematic effect.
- **Aspect Ratio Jitter:** If you use `sharedBounds` instead of `sharedElement` for the image, the `ContentScale` shift might look blocky or jarring because `sharedBounds` interpolates containers, not pixels. `sharedElement` natively understands pixel rendering bounds and handles `ContentScale.Crop` to `Fit` morphs flawlessly.
- **Overscroll clipping:** When jumping from a deeply scrolled `LazyVerticalGrid`, the grid item might be partially clipped by the screen edges. `sharedElement`'s default `renderInOverlayDuringTransition = true` ensures the image pops out of the grid constraints and animates smoothly over the UI.

## 💡 Interview Q&A

**Q: How does Compose handle the transition between `ContentScale.Crop` in the grid and `ContentScale.Fit` in the fullscreen view?**
A: When using `Modifier.sharedElement()`, Compose captures the rendering coordinates and constraints of the image at both the start and end states. It applies a continuous transformation matrix to the underlying graphical bounds during the transition, smoothly interpolating the clipping bounds from a cropped square to the image's intrinsic aspect ratio.
