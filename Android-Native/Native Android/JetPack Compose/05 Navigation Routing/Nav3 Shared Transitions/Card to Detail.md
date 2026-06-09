# Card to Detail Transition

## 📌 Purpose
The "Card to Detail" transition (also known as Container Transform in Material Design) is the most common shared element pattern. It occurs when a user taps a compact card in a list, and that card expands to fill the screen as a detailed view, while the internal contents crossfade seamlessly.

> [!TIP]
> This pattern relies heavily on `sharedBounds` for the container morphing, and `sharedElement` for persistent content like the main image.

## ✅ Full Working Example

### 1. Data Model

```kotlin
data class Product(
    val id: Int,
    val name: String,
    val description: String,
    val imageRes: Int
)

val sampleProducts = listOf(
    Product(1, "Wireless Headphones", "High fidelity audio with noise cancellation.", android.R.drawable.ic_media_play),
    Product(2, "Smart Watch", "Track your fitness and receive notifications on the go.", android.R.drawable.ic_menu_compass),
    Product(3, "Mechanical Keyboard", "Clicky switches with RGB backlighting.", android.R.drawable.ic_menu_edit)
)
```

### 2. Nav3 App Setup

```kotlin
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.navigation3.*
import androidx.navigation3.ui.*
import kotlinx.serialization.Serializable

@Serializable data object ProductListRoute : NavKey
@Serializable data class ProductDetailRoute(val id: Int) : NavKey

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CardToDetailApp() {
    SharedTransitionLayout {
        val backStack = remember { mutableStateListOf<NavKey>(ProductListRoute) }
        
        NavDisplay(
            backStack = backStack,
            entryProvider = entryProvider {
                entry<ProductListRoute> {
                    ProductListScreen(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                        onProductClick = { id -> backStack.add(ProductDetailRoute(id)) }
                    )
                }
                entry<ProductDetailRoute> { route ->
                    ProductDetailScreen(
                        productId = route.id,
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

### 3. Home Screen (List of Cards)

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProductListScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onProductClick: (Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(sampleProducts, key = { it.id }) { product ->
            with(sharedTransitionScope) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "card-${product.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> tween(400, easing = FastOutSlowInEasing) }
                        )
                        .clickable { onProductClick(product.id) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Image(
                            painter = painterResource(id = product.imageRes),
                            contentDescription = product.name,
                            modifier = Modifier
                                .size(80.dp)
                                .sharedElement(
                                    state = rememberSharedContentState(key = "image-${product.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    boundsTransform = { _, _ -> tween(400, easing = FastOutSlowInEasing) }
                                )
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .skipToLookaheadSize()
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = "title-${product.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds,
                                    boundsTransform = { _, _ -> tween(400, easing = FastOutSlowInEasing) }
                                )
                        )
                    }
                }
            }
        }
    }
}
```

### 4. Detail Screen

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Alignment

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProductDetailScreen(
    productId: Int,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit
) {
    val product = sampleProducts.first { it.id == productId }

    with(sharedTransitionScope) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "card-${product.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    enter = fadeIn(tween(300, delayMillis = 200)),
                    exit = fadeOut(tween(300)),
                    boundsTransform = { _, _ -> tween(400, easing = FastOutSlowInEasing) }
                )
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                Image(
                    painter = painterResource(id = product.imageRes),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedElement(
                            state = rememberSharedContentState(key = "image-${product.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> tween(400, easing = FastOutSlowInEasing) }
                        )
                )
                
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }

            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier
                        .skipToLookaheadSize()
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "title-${product.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds,
                            boundsTransform = { _, _ -> tween(400, easing = FastOutSlowInEasing) }
                        )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Description fades in (it is not a shared element)
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
```

## ⚠️ Common Gotchas

1. **Consistent BoundsTransform:** You should use the exact same `BoundsTransform` (like `tween(400, easing = FastOutSlowInEasing)`) for all `sharedBounds` and `sharedElement` modifiers that animate together. Otherwise, the image might arrive at its destination faster than the card container.
2. **Detail Background Fade:** The detail screen must explicitly declare `background(MaterialTheme.colorScheme.surface)` *inside* the container that has `sharedBounds`. Use the `enter` and `exit` parameters on `sharedBounds` to create a smooth crossfade of the background content (`fadeIn(tween(300, delayMillis = 200))`).
3. **Text Reflows:** Using `skipToLookaheadSize()` + `ScaleToBounds` on the title text is critical; otherwise, the text might visually jump or wrap awkwardly during the transition.

## 💡 Interview Q&A

**Q: Why do we use `sharedBounds` on the Card and not `sharedElement`?**
A: `sharedElement` is for identical UI trees. The compact card has a row layout, whereas the detail screen has a column layout. They are fundamentally different visual structures. `sharedBounds` morphs the *container's physical boundaries* while crossfading the non-shared UI elements inside.

**Q: Where should the `modifier.clickable` be placed relative to `sharedBounds`?**
A: `clickable` should generally be chained *after* `sharedBounds`. The shared bounds define the coordinate space of the transitioning entity; the click listener applies to the entity.
