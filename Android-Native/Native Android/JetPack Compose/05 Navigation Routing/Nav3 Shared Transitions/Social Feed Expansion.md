# Social Feed Expansion

## 📌 Purpose
Social media feeds present a complex transition challenge: you have a list item containing multiple disparate elements (avatar, text, image) that all need to animate into a detailed view, while the container itself expands.

> [!TIP]
> Use a combination of `sharedBounds` for containers/text and `sharedElement` for images. Ensuring that clip shapes match between the start and end states is critical for a smooth morph.

## ✅ Full Working Example

### 1. Data Model
```kotlin
data class Post(
    val id: String,
    val authorName: String,
    val authorAvatarRes: Int,
    val textContent: String,
    val imageRes: Int? = null
)

val samplePosts = listOf(
    Post("p1", "Alice Smith", android.R.drawable.sym_def_app_icon, "Just had a great day at the park!", android.R.drawable.ic_menu_gallery),
    Post("p2", "Bob Jones", android.R.drawable.sym_contact_card, "Learning Jetpack Compose Shared Transitions. It is amazing!", null)
)
```

### 2. Nav3 App Setup
```kotlin
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.navigation3.*
import androidx.navigation3.ui.*
import kotlinx.serialization.Serializable

@Serializable data object FeedRoute : NavKey
@Serializable data class PostDetailRoute(val postId: String) : NavKey

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SocialFeedApp() {
    SharedTransitionLayout {
        val backStack = remember { mutableStateListOf<NavKey>(FeedRoute) }
        
        NavDisplay(
            backStack = backStack,
            entryProvider = entryProvider {
                entry<FeedRoute> {
                    FeedScreen(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                        onPostClick = { id -> backStack.add(PostDetailRoute(id)) }
                    )
                }
                entry<PostDetailRoute> { route ->
                    PostDetailScreen(
                        postId = route.postId,
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

### 3. Feed Screen (List of Posts)
```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FeedScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onPostClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(samplePosts, key = { it.id }) { post ->
            with(sharedTransitionScope) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "post-card-${post.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> tween(400) }
                        )
                        .clickable { onPostClick(post.id) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = post.authorAvatarRes),
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    // Clip MUST match between start and end states
                                    .clip(CircleShape)
                                    .sharedElement(
                                        state = rememberSharedContentState(key = "avatar-${post.id}"),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        boundsTransform = { _, _ -> tween(400) }
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = post.authorName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .skipToLookaheadSize()
                                    .sharedBounds(
                                        sharedContentState = rememberSharedContentState(key = "author-${post.id}"),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds,
                                        boundsTransform = { _, _ -> tween(400) }
                                    )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = post.textContent,
                            maxLines = 3,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        if (post.imageRes != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Image(
                                painter = painterResource(id = post.imageRes),
                                contentDescription = "Post Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .sharedElement(
                                        state = rememberSharedContentState(key = "post-image-${post.id}"),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        boundsTransform = { _, _ -> tween(400) }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
```

### 4. Post Detail Screen
```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PostDetailScreen(
    postId: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit
) {
    val post = samplePosts.first { it.id == postId }

    with(sharedTransitionScope) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "post-card-${post.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> tween(400) },
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300))
                )
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
        ) {
            IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = post.authorAvatarRes),
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp) // Larger avatar in detail view
                            .clip(CircleShape) // Matching clip shape
                            .sharedElement(
                                state = rememberSharedContentState(key = "avatar-${post.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> tween(400) }
                            )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .skipToLookaheadSize()
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "author-${post.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds,
                                boundsTransform = { _, _ -> tween(400) }
                            )
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = post.textContent,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                if (post.imageRes != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Image(
                        painter = painterResource(id = post.imageRes),
                        contentDescription = "Post Image",
                        contentScale = ContentScale.FillWidth, // Can transition from Crop safely
                        modifier = Modifier
                            .fillMaxWidth()
                            .sharedElement(
                                state = rememberSharedContentState(key = "post-image-${post.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> tween(400) }
                            )
                    )
                }
            }
        }
    }
}
```

## ⚠️ Common Gotchas

- **Circular to Circular Transitions:** When animating a circular avatar to a larger circular avatar, you must apply `.clip(CircleShape)` explicitly on *both* sides of the transition. If one side uses a rounded corner shape and the other uses a circle, the `sharedElement` bounds interpolation might look blocky or distorted.
- **Complex Hierarchies:** Too many shared elements (e.g., trying to share every single icon in a post's action bar) can cause performance drops and visual clutter. Stick to the anchor elements: Container (Card), Avatar, Title, and Main Media. Let the rest crossfade natively.
- **Missing Background Fade:** The `background(MaterialTheme.colorScheme.surface)` modifier inside the detail view is mandatory. The `sharedBounds` acts as a transparent morphing container by default; without a solid background that fades in (`enter = fadeIn(...)`), the list items underneath will be visible during the transition.
