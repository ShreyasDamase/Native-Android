# BottomNav Transitions

## 📌 Purpose
Bottom navigation shifts between top-level peer destinations. The recommended Material Design motion pattern for these transitions is **FadeThrough**. 

> [!NOTE]
> Unlike standard slide animations, FadeThrough drops the opacity of the exiting screen and slightly scales down, while the entering screen fades in and scales up. This implies no spatial relationship between the screens.

## ✅ Full Working Example

### 1. Route Definitions
```kotlin
import kotlinx.serialization.Serializable
import androidx.navigation3.NavKey

@Serializable data object HomeRoute : NavKey
@Serializable data object DashboardRoute : NavKey
@Serializable data object NotificationsRoute : NavKey
```

### 2. Scaffold and BottomBar Setup
To keep the Bottom Navigation Bar stable while the content above it animates, use a standard `Scaffold`. The `NavDisplay` goes inside the `Scaffold`'s content area.

```kotlin
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.*
import androidx.navigation3.ui.*

@Composable
fun BottomNavApp() {
    // Top-level state for bottom nav selection
    // Note: Instead of a deep back stack, we just replace the root element
    val backStack = remember { mutableStateListOf<NavKey>(HomeRoute) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute = backStack.lastOrNull()

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = currentRoute == HomeRoute,
                    onClick = {
                        // Clear stack and set new root to avoid back stack bloat
                        backStack.clear()
                        backStack.add(HomeRoute)
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = currentRoute == DashboardRoute,
                    onClick = {
                        backStack.clear()
                        backStack.add(DashboardRoute)
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Alerts") },
                    label = { Text("Alerts") },
                    selected = currentRoute == NotificationsRoute,
                    onClick = {
                        backStack.clear()
                        backStack.add(NotificationsRoute)
                    }
                )
            }
        }
    ) { innerPadding ->
        
        // NavDisplay handles the content routing
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(innerPadding),
            
            // Apply FadeThrough transition globally
            transitionSpec = {
                fadeIn(tween(300, delayMillis = 150)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(300, delayMillis = 150)) togetherWith
                fadeOut(tween(150))
            },
            popTransitionSpec = {
                fadeIn(tween(300, delayMillis = 150)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(300, delayMillis = 150)) togetherWith
                fadeOut(tween(150))
            },
            
            entryProvider = entryProvider {
                entry<HomeRoute> { 
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Home Screen") }
                }
                entry<DashboardRoute> { 
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Dashboard Screen") }
                }
                entry<NotificationsRoute> { 
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Notifications Screen") }
                }
            },
            onBack = { backStack.removeLastOrNull() }
        )
    }
}
```

## ⚠️ Common Gotchas

- **Back Stack Bloat:** When switching bottom tabs, you usually don't want to add to a massive back stack (`A -> B -> C -> A -> B`). It's best practice to clear the stack or manipulate it so the selected tab becomes the new root (`backStack.clear(); backStack.add(NewRoute)`).
- **Scaffold Padding:** Always pass the `innerPadding` from the `Scaffold` to the `NavDisplay` modifier. Otherwise, your content will render underneath the `NavigationBar`.
- **Transitions and Scaffold:** Do NOT put the `Scaffold` inside the `NavDisplay` entry. If you do, the Bottom Nav Bar itself will animate and crossfade out/in on every click, which looks terrible. The `Scaffold` wraps `NavDisplay`.

## 💡 Interview Q&A

**Q: Why do we use FadeThrough instead of Slide for bottom navigation?**
A: Slide animations imply a spatial relationship (e.g., sliding left means moving deeper, sliding right means going back). Bottom navigation tabs are peers with no inherent spatial hierarchy. FadeThrough correctly communicates that the user is completely swapping contexts without a deep navigation relationship.
