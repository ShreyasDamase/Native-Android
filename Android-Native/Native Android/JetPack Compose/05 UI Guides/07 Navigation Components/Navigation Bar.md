# NavigationBar

## 📌 Purpose
`NavigationBar` is the Material 3 successor to the classic `BottomNavigationView`. It provides access to 3-5 primary destinations at the bottom of the screen. Individual destinations are represented by `NavigationBarItem`s.

## 🔧 Function Signature

### `NavigationBar`
```kotlin
@Composable
fun NavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = NavigationBarDefaults.containerColor,
    contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
    tonalElevation: Dp = NavigationBarDefaults.Elevation,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
    content: @Composable RowScope.() -> Unit
)
```

### `NavigationBarItem`
```kotlin
@Composable
fun RowScope.NavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
    colors: NavigationBarItemColors = NavigationBarItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null
)
```

## 📋 Props / Parameters

### `NavigationBarItem`
| Parameter | Type | Default | Description |
|---|---|---|---|
| `selected` | `Boolean` | — | Required. Whether this item is the currently selected destination. |
| `onClick` | `() -> Unit` | — | Required. Callback when the user taps this item. |
| `icon` | `@Composable () -> Unit` | — | Required. The icon composable (often switches based on `selected` state). |
| `modifier` | `Modifier` | `Modifier` | Optional. Layout modifier for the item. |
| `enabled` | `Boolean` | `true` | Optional. If false, item cannot be clicked. |
| `label` | `@Composable (() -> Unit)?` | `null` | Optional. The text label displayed below the icon. |
| `alwaysShowLabel` | `Boolean` | `true` | Optional. If `false`, the label is only visible when `selected == true`. |
| `colors` | `NavigationBarItemColors` | `NavigationBarItemDefaults.colors()` | Optional. Customizes indicator, icon, and text colors across states. |
| `interactionSource` | `MutableInteractionSource?`| `null` | Optional. |

## ✅ Basic Example: 3-Item Navigation
The most standard implementation, usually placed in the `bottomBar` slot of a `Scaffold`.

```kotlin
@Composable
fun SimpleBottomNav() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Home", "Search", "Profile")
    val icons = listOf(Icons.Filled.Home, Icons.Filled.Search, Icons.Filled.Person)

    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = item) },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = { selectedItem = index }
            )
        }
    }
}
```

## 🚀 Advanced Examples

### 1. NavigationBar with Scaffold and NavHost integration
The canonical pattern in Jetpack Compose architecture.

```kotlin
@Composable
fun AppNavigation(navController: NavHostController) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                listOf(Screen.Home, Screen.Profile).forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Home.route, Modifier.padding(innerPadding)) {
            // ... destinations
        }
    }
}
```

### 2. NavigationBar with Badges
Using the `BadgedBox` component to show notification counts.

```kotlin
@Composable
fun BadgedBottomNav() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val unreadMessages = 3

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Home") },
            selected = selectedItem == 0,
            onClick = { selectedItem = 0 }
        )
        NavigationBarItem(
            icon = {
                BadgedBox(
                    badge = {
                        if (unreadMessages > 0) {
                            Badge { Text(unreadMessages.toString()) }
                        }
                    }
                ) {
                    Icon(Icons.Default.Email, contentDescription = null)
                }
            },
            label = { Text("Messages") },
            selected = selectedItem == 1,
            onClick = { selectedItem = 1 }
        )
    }
}
```

### 3. Icon Animation on Select
Changing icons between outlined and filled states based on selection.

```kotlin
@Composable
fun AnimatedIconBottomNav() {
    var selectedItem by remember { mutableIntStateOf(0) }

    NavigationBar {
        NavigationBarItem(
            icon = {
                Crossfade(targetState = selectedItem == 0) { isSelected ->
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorites"
                    )
                }
            },
            label = { Text("Favorites") },
            selected = selectedItem == 0,
            onClick = { selectedItem = 0 }
        )
    }
}
```

## ⚠️ Common Gotchas

> [!WARNING] RowScope Constraint
> `NavigationBarItem` is an extension on `RowScope`. You **cannot** use it outside of the `NavigationBar` (or a `Row`). It relies on `RowScope.weight` internally to distribute items evenly.

> [!CAUTION] Overcrowding
> Material guidelines state `NavigationBar` should hold exactly 3 to 5 items. If you need 6 or more, consider adding a "More" drawer or restructuring your app architecture.

> [!TIP] Scaffold Padding
> When placing `NavigationBar` inside `Scaffold(bottomBar = {...})`, you MUST consume the `innerPadding` passed to the Scaffold's content block, or your bottom content will get hidden behind the NavigationBar.

## 💡 Interview Q&A

**Q: What happens if `alwaysShowLabel` is set to `false`?**
A: The label text is hidden on unselected items, and a smooth animation reveals the text and shifts the icon upwards only when the user taps that specific item.

**Q: How do you change the pill-shaped indicator color behind the selected icon?**
A: Pass a custom `NavigationBarItemColors` using `NavigationBarItemDefaults.colors(indicatorColor = YourColor)` into the `colors` parameter of `NavigationBarItem`.
