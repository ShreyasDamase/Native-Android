# Profile Card Expansion

## 📌 Purpose
The Profile Card pattern demonstrates animating a compact UI component (often floating or embedded) into a full-screen view. This is commonly used in messaging apps or sidebars where tapping a small circular avatar card blooms into a complete user profile.

> [!NOTE]
> This pattern emphasizes spatial growth. Using `sharedBounds` on the card and `ScaleToBounds` on the text ensures the profile info scales gracefully as the card expands to fill the device screen.

## ✅ Full Working Example

### 1. Data Model
```kotlin
data class User(
    val id: String,
    val name: String,
    val bio: String,
    val followers: Int,
    val following: Int,
    val photoRes: Int
)

val sampleUser = User(
    id = "u1",
    name = "Elena Rostova",
    bio = "Android Developer | Tech Writer | Coffee Enthusiast",
    followers = 1420,
    following = 350,
    photoRes = android.R.drawable.sym_def_app_icon
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

@Serializable data object MainScreenRoute : NavKey
@Serializable data class ProfileRoute(val userId: String) : NavKey

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileApp() {
    SharedTransitionLayout {
        val backStack = remember { mutableStateListOf<NavKey>(MainScreenRoute) }
        
        NavDisplay(
            backStack = backStack,
            entryProvider = entryProvider {
                entry<MainScreenRoute> {
                    MainScreen(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                        onProfileClick = { backStack.add(ProfileRoute(sampleUser.id)) }
                    )
                }
                entry<ProfileRoute> { route ->
                    ProfileScreen(
                        userId = route.userId,
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

### 3. Main Screen (Compact Profile Card)
```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
fun MainScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onProfileClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        with(sharedTransitionScope) {
            // A small floating profile card at the top right
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "profile-card-${sampleUser.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> tween(500) }
                    )
                    .clickable { onProfileClick() },
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = sampleUser.photoRes),
                        contentDescription = "Profile Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .sharedElement(
                                state = rememberSharedContentState(key = "profile-photo-${sampleUser.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> tween(500) }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = sampleUser.name,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .skipToLookaheadSize()
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "profile-name-${sampleUser.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds,
                                boundsTransform = { _, _ -> tween(500) }
                            )
                    )
                }
            }
        }
    }
}
```

### 4. Full Profile Screen
```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    userId: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit
) {
    val user = sampleUser // Fetch user by ID in real app

    with(sharedTransitionScope) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "profile-card-${user.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    enter = fadeIn(tween(400)),
                    exit = fadeOut(tween(400)),
                    boundsTransform = { _, _ -> tween(500) }
                )
                // The expanded card background matches the surface color
                .background(MaterialTheme.colorScheme.surface)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close Profile")
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = user.photoRes),
                    contentDescription = "Profile Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp) // Much larger photo in detail
                        .clip(CircleShape)
                        .sharedElement(
                            state = rememberSharedContentState(key = "profile-photo-${user.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> tween(500) }
                        )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier
                        .skipToLookaheadSize()
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "profile-name-${user.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds,
                            boundsTransform = { _, _ -> tween(500) }
                        )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Non-shared elements just crossfade natively
                Text(
                    text = user.bio,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${user.followers}", style = MaterialTheme.typography.titleLarge)
                        Text("Followers", style = MaterialTheme.typography.labelMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${user.following}", style = MaterialTheme.typography.titleLarge)
                        Text("Following", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
```

## ⚠️ Common Gotchas

- **Corner Radius Morphing:** Notice how the compact card uses `RoundedCornerShape(32.dp)`, while the full screen acts as a rect. `sharedBounds` naturally handles clipping corner radii between the start bounds and end bounds if you apply the shape on the surface, but sometimes it can look a bit rigid. For the best visual effect, ensure `renderInOverlayDuringTransition = true` (default).
- **ScaleToBounds is critical:** The font size jump from `labelLarge` to `headlineLarge` is extreme. If you don't use `ScaleToBounds` + `skipToLookaheadSize()`, the text will snap to the large size immediately and then awkwardly translate across the screen.

## 💡 Interview Q&A

**Q: Can I use `sharedBounds` without an explicit `sharedElement` inside it?**
A: Yes! You can use `sharedBounds` purely to expand a container smoothly, while all the internal elements just crossfade (`fadeIn`/`fadeOut`) normally. You only need `sharedElement` for things you specifically want to translate and scale across the screen, like the profile photo.
