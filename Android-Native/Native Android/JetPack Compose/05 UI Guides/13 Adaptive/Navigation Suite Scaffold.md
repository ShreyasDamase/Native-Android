# NavigationSuiteScaffold

## 📌 Purpose
`NavigationSuiteScaffold` is a powerful, adaptive container that automatically switches between different navigation UI components based on the device's screen size (phone, tablet, desktop).

Instead of writing `if/else` logic to show a `NavigationBar` on phones and a `NavigationRail` on tablets, `NavigationSuiteScaffold` does it automatically using window size classes!

> [!NOTE]
> **Dependency:** This requires a separate library dependency:
> `implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.3.0")`

## 🔧 Function Signature
```kotlin
@Composable
fun NavigationSuiteScaffold(
    navigationSuiteItems: NavigationSuiteScope.() -> Unit,
    modifier: Modifier = Modifier,
    layoutType: NavigationSuiteType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
        WindowAdaptiveInfoDefault.currentWindowAdaptiveInfo()
    ),
    containerColor: Color = NavigationSuiteScaffoldDefaults.containerColor,
    contentColor: Color = NavigationSuiteScaffoldDefaults.contentColor,
    content: @Composable () -> Unit
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `navigationSuiteItems` | `NavigationSuiteScope.() -> Unit` | — | Builder block where you define your navigation items using `item()`. |
| `layoutType` | `NavigationSuiteType` | Auto-calculated | Forces a specific layout, or defaults to auto-detecting based on screen size. |
| `content` | `@Composable () -> Unit` | — | The main screen content (typically a `NavHost`). |

### NavigationSuiteType Values:
- `NavigationSuiteType.NavigationBar`: Bottom bar (used for compact screens / phones).
- `NavigationSuiteType.NavigationRail`: Side rail (used for medium screens / landscape phones / small tablets).
- `NavigationSuiteType.NavigationDrawer`: Persistent side drawer (used for expanded screens / desktops / large tablets).

## 🎛️ NavigationSuiteScope.item()
Inside `navigationSuiteItems`, you call `item()` for each tab.
```kotlin
fun item(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    badge: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    alwaysShowLabel: Boolean = true,
    colors: NavigationSuiteItemColors = NavigationSuiteDefaults.itemColors(),
    interactionSource: MutableInteractionSource? = null
)
```

## ✅ Basic Example

### 1. Full Adaptive Navigation
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*

@Composable
fun AdaptiveAppNavigation() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Home", "Settings")
    val icons = listOf(Icons.Filled.Home, Icons.Filled.Settings)

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            items.forEachIndexed { index, item ->
                item(
                    icon = { Icon(icons[index], contentDescription = item) },
                    label = { Text(item) },
                    selected = selectedItem == index,
                    onClick = { selectedItem = index }
                )
            }
        }
    ) {
        // Main Screen Content goes here!
        // This takes up the remaining space next to/above the navigation.
        Text("Currently on tab: ${items[selectedItem]}")
    }
}
```

## 🚀 Advanced Examples

### 2. Integration with NavHost
Typically, `selected` and `onClick` are driven by a `NavController`.

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun NavHostAdaptiveApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            item(
                icon = { Icon(Icons.Filled.Home, null) },
                label = { Text("Home") },
                selected = currentRoute == "home",
                onClick = { navController.navigate("home") }
            )
            item(
                icon = { Icon(Icons.Filled.Email, null) },
                label = { Text("Messages") },
                selected = currentRoute == "messages",
                onClick = { navController.navigate("messages") }
            )
        }
    ) {
        NavHost(navController, startDestination = "home") {
            composable("home") { Text("Home Screen") }
            composable("messages") { Text("Messages Screen") }
        }
    }
}
```

### 3. Override layoutType Manually
If you want to force a `NavigationRail` regardless of screen size:

```kotlin
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable

@Composable
fun ForcedRailNavigation() {
    NavigationSuiteScaffold(
        layoutType = NavigationSuiteType.NavigationRail,
        navigationSuiteItems = { /* items */ }
    ) {
        // Content
    }
}
```

## ⚠️ Common Gotchas
- **Dependency:** It is NOT part of the standard `androidx.compose.material3` library. You must add the `material3-adaptive-navigation-suite` artifact.
- **Scaffold Usage:** `NavigationSuiteScaffold` wraps your entire app. You typically place standard `Scaffold`s (which have `TopAppBar`s and `FloatingActionButton`s) *inside* the `content` block of the `NavigationSuiteScaffold`.

## 💡 Interview Q&A

**Q: How does `NavigationSuiteScaffold` know when to switch from a bottom bar to a side rail?**
A: It relies on `WindowAdaptiveInfo`. By default, it checks the screen's window size class. If the width is "Compact" (e.g., portrait phone), it uses a bottom `NavigationBar`. If "Medium" (landscape phone, foldable), it uses a `NavigationRail`. If "Expanded" (tablet/desktop), it uses a `NavigationDrawer`.

**Q: Can I use `Badge` inside a `NavigationSuiteScaffold` item?**
A: Yes! The `item()` builder has a `badge: @Composable (() -> Unit)?` parameter. You can pass the standard Material 3 `Badge` composable here just like you would on a standard `NavigationBarItem`.
