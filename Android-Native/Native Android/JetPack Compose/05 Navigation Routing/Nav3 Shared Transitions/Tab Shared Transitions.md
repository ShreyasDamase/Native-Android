# Tab Shared Transitions

## 📌 Purpose
Tabs or bottom navigation bars frequently require elements to persist seamlessly or animate uniquely when switching content. Instead of using a full `NavDisplay` back stack push, tabs often use `AnimatedContent` for peer-level screen switching.

> [!IMPORTANT]
> When using `AnimatedContent` directly (without Nav3's `NavDisplay`), the `AnimatedVisibilityScope` must be grabbed from the `AnimatedContent` lambda receiver (`this`), NOT `LocalNavAnimatedContentScope.current`.

There are two primary scenarios for tabs with shared transitions:

## 🚀 Scenario A: Persistent Elements Across Tabs
Some UI elements, like a "Mini Player" in a music app, should remain completely stable on the screen while the tabs underneath crossfade or slide.

```kotlin
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class AppTab { Home, Search, Profile }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PersistentPlayerApp() {
    var selectedTab by remember { mutableStateOf(AppTab.Home) }

    SharedTransitionLayout {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Tab Content
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { tab ->
                when (tab) {
                    AppTab.Home -> Box(Modifier.fillMaxSize().background(Color.Red))
                    AppTab.Search -> Box(Modifier.fillMaxSize().background(Color.Blue))
                    AppTab.Profile -> Box(Modifier.fillMaxSize().background(Color.Green))
                }
            }

            // Floating Mini Player that stays STABLE across tab changes
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    // Render above the AnimatedContent transitions
                    .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Now Playing: Song Title", modifier = Modifier.padding(16.dp))
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                AppTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.name) }
                    )
                }
            }
        }
    }
}
```

## 🚀 Scenario B: Tab Switching with Content Animation
Sometimes you want an element from Tab A to animate smoothly into its corresponding location in Tab B.

```kotlin
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTabContentApp() {
    var selectedTab by remember { mutableStateOf(AppTab.Home) }

    SharedTransitionLayout {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 1. AnimatedContent provides its own AnimatedVisibilityScope!
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    // Determine slide direction based on tab index
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (fadeIn() + slideInHorizontally { it * direction }) togetherWith
                    (fadeOut() + slideOutHorizontally { -it * direction })
                }
            ) { tab ->
                // 'this' is the AnimatedVisibilityScope
                val animatedVisibilityScope = this 
                
                when (tab) {
                    AppTab.Home -> HomeTabContent(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                    AppTab.Search -> SearchTabContent(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                    AppTab.Profile -> Box(Modifier.fillMaxSize())
                }
            }

            TabRow(selectedTabIndex = selectedTab.ordinal) {
                AppTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.name) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeTabContent(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    with(sharedTransitionScope) {
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            // A persistent logo that exists on both Home and Search tabs
            Text(
                text = "GLOBAL LOGO",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .sharedElement(
                        state = rememberSharedContentState(key = "global-logo"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SearchTabContent(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    with(sharedTransitionScope) {
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            // The logo moves to the center in the Search tab
            Text(
                text = "GLOBAL LOGO",
                modifier = Modifier
                    .align(Alignment.Center)
                    .sharedElement(
                        state = rememberSharedContentState(key = "global-logo"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
            )
        }
    }
}
```

## ⚠️ Common Gotchas

- **Using the wrong AnimatedVisibilityScope:** If you are inside `AnimatedContent`, do NOT use `LocalNavAnimatedContentScope.current`. You must capture the `this` receiver from the `AnimatedContent` trailing lambda. Using the wrong scope means the shared elements won't sync with the slide/fade animations.
- **Z-Index over TabRows:** Expanding elements in tabs might get clipped by or hidden behind `TabRow` or `BottomAppBar`. Ensure your shared items use `renderInOverlayDuringTransition = true` (which is default on `sharedElement` and `sharedBounds`).

## 💡 Interview Q&A

**Q: Why use `AnimatedContent` for tabs instead of `NavDisplay`?**
A: Tabs represent peer-level destinations (top-level navigation). Usually, switching a tab shouldn't add to the deep navigation back stack (pressing back shouldn't cycle through previously clicked tabs). `AnimatedContent` is perfect for state-based peer switching, whereas `NavDisplay` is designed for stack-based hierarchical navigation.
