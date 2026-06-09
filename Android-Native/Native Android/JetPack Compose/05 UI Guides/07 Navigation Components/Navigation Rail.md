# NavigationRail

## 📌 Purpose
`NavigationRail` is a vertical navigation component tailored for larger screens (tablets, foldables, and desktop). While phones use the horizontal `NavigationBar` at the bottom, tablets shift those destinations to a side rail on the left or right to make better use of widescreen layouts.

## 🔧 Function Signature

### `NavigationRail`
```kotlin
@Composable
fun NavigationRail(
    modifier: Modifier = Modifier,
    containerColor: Color = NavigationRailDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    header: @Composable (ColumnScope.() -> Unit)? = null,
    windowInsets: WindowInsets = NavigationRailDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit
)
```

### `NavigationRailItem`
```kotlin
@Composable
fun NavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
    colors: NavigationRailItemColors = NavigationRailItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null
)
```

## 📋 Props / Parameters

### `NavigationRail`
| Parameter | Type | Default | Description |
|---|---|---|---|
| `header` | `@Composable (ColumnScope.() -> Unit)?` | `null` | Optional slot at the very top of the rail, perfect for an app logo or a Floating Action Button (FAB). |
| `containerColor` | `Color` | `NavigationRailDefaults.ContainerColor` | Background color of the rail. |
| `content` | `@Composable ColumnScope.() -> Unit` | — | Required. Holds the `NavigationRailItem`s. Usually arranged vertically with `Spacer`s if needed. |

### `NavigationRailItem`
(Identical properties to `NavigationBarItem`, but optimized for vertical stacking).

| Parameter | Type | Default | Description |
|---|---|---|---|
| `selected` | `Boolean` | — | Required. |
| `onClick` | `() -> Unit` | — | Required. |
| `icon` | `@Composable () -> Unit` | — | Required. |
| `label` | `@Composable (() -> Unit)?` | `null` | Optional text below the icon. |

## ✅ Basic Example: Tablet Layout
Normally placed on the leading edge of a `Row` alongside the main screen content.

```kotlin
@Composable
fun BasicNavigationRail() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Home", "Search", "Settings")
    val icons = listOf(Icons.Filled.Home, Icons.Filled.Search, Icons.Filled.Settings)

    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail {
            items.forEachIndexed { index, item ->
                NavigationRailItem(
                    icon = { Icon(icons[index], contentDescription = item) },
                    label = { Text(item) },
                    selected = selectedItem == index,
                    onClick = { selectedItem = index }
                )
            }
        }
        
        // Main Screen Content
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Selected: ${items[selectedItem]}")
        }
    }
}
```

## 🚀 Advanced Examples

### 1. NavigationRail with FAB in Header
Material guidelines often place the primary creation action at the top of the rail.

```kotlin
@Composable
fun HeaderNavigationRail() {
    NavigationRail(
        header = {
            FloatingActionButton(
                onClick = { /* Add new item */ },
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
            Spacer(Modifier.height(8.dp)) // Optional spacing
        }
    ) {
        NavigationRailItem(
            icon = { Icon(Icons.Default.Inbox, contentDescription = null) },
            label = { Text("Inbox") },
            selected = true,
            onClick = { }
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Send, contentDescription = null) },
            label = { Text("Sent") },
            selected = false,
            onClick = { }
        )
    }
}
```

### 2. Adaptive Navigation (Combining Rail + Bar)
The standard modern Android architectural pattern using WindowSizeClasses.

```kotlin
@Composable
fun AdaptiveNavigationLayout(windowSizeClass: WindowSizeClass) {
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    
    Scaffold(
        bottomBar = {
            if (isCompact) {
                NavigationBar { /* Phone bottom nav */ }
            }
        }
    ) { padding ->
        Row(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (!isCompact) {
                NavigationRail { /* Tablet side nav */ }
            }
            // Main content
            Box(Modifier.weight(1f)) {
                // Content goes here
            }
        }
    }
}
```

## ⚠️ Common Gotchas

> [!WARNING] Vertical Alignment
> By default, `NavigationRail` centers its content vertically. If you want items to align to the top (right beneath the header), you must put a `Spacer(Modifier.weight(1f))` at the *end* of your items. If you want them at the bottom, put the spacer at the *top*.

> [!CAUTION] `Scaffold` Compatibility
> Unlike `NavigationBar` which has a dedicated `bottomBar` slot in `Scaffold`, `NavigationRail` **does not** have a dedicated slot in the standard Compose `Scaffold`. You must manually construct a `Row` and place the Rail alongside your content.

## 💡 Interview Q&A

**Q: When should an app use `NavigationRail` vs `NavigationDrawer` on a tablet?**
A: `NavigationRail` is best for 3-7 top-level destinations that need constant, quick switching (like email folders). `NavigationDrawer` is better when you have a massive hierarchy of destinations (10+ items, sub-headers, profile switchers) because it has more horizontal space for text.

**Q: How do you position the rail items at the bottom of the screen?**
A: Since the rail content is a `ColumnScope`, insert `Spacer(modifier = Modifier.weight(1f))` as the very first item inside the `NavigationRail` content block. This pushes all subsequent `NavigationRailItem`s to the bottom.
