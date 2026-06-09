# WideNavigationRail

## 📌 Purpose
`WideNavigationRail` is part of the new **Material 3 Expressive** design language (`@OptIn(ExperimentalMaterial3ExpressiveApi::class)`). It is an advanced version of `NavigationRail` designed for large screens (tablets, desktops, foldables). 

Unlike the standard `NavigationRail` which is fixed-width (usually showing just icons), the `WideNavigationRail` supports an **expanded state** where it widens to reveal both icons and text labels side-by-side, similar to a mini-drawer.

## 🔧 Function Signatures

### `WideNavigationRail`
```kotlin
@ExperimentalMaterial3ExpressiveApi
@Composable
fun WideNavigationRail(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    containerColor: Color = WideNavigationRailDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    header: @Composable (ColumnScope.() -> Unit)? = null,
    windowInsets: WindowInsets = WideNavigationRailDefaults.windowInsets,
    arrangement: Arrangement.Vertical = WideNavigationRailDefaults.Arrangement,
    content: @Composable ColumnScope.() -> Unit
)
```

### `WideNavigationRailItem`
```kotlin
@ExperimentalMaterial3ExpressiveApi
@Composable
fun WideNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    railExpanded: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: NavigationItemColors = WideNavigationRailItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null
)
```

## 📋 Props / Parameters

| Parameter | Type | Description |
|---|---|---|
| `expanded` / `railExpanded` | `Boolean` | **Crucial.** Tells the rail and its items whether to render in compact mode (icon only) or expanded mode (icon + label side-by-side). |
| `header` | `@Composable` | Usually a FAB or logo. Adapts nicely when expanded. |
| `arrangement` | `Arrangement.Vertical` | Defines how items are spaced. Standard rail defaults to center; wide rail defaults to top alignment. |

## ✅ Basic Example: Expandable Rail
Notice how we must track the `expanded` state and pass it down to both the Rail and the Items.

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AdaptiveWideRail() {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableIntStateOf(0) }

    Row(modifier = Modifier.fillMaxSize()) {
        WideNavigationRail(
            expanded = isExpanded,
            header = {
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(Icons.Default.Menu, contentDescription = "Toggle Menu")
                }
            }
        ) {
            WideNavigationRailItem(
                selected = selectedItem == 0,
                onClick = { selectedItem = 0 },
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text("Dashboard") },
                railExpanded = isExpanded
            )
            WideNavigationRailItem(
                selected = selectedItem == 1,
                onClick = { selectedItem = 1 },
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text("Settings") },
                railExpanded = isExpanded
            )
        }

        // Main Screen Content
        Box(Modifier.fillMaxSize().background(Color.LightGray)) {
            Text("Content Area", Modifier.align(Alignment.Center))
        }
    }
}
```

## 🚀 Advanced Examples

### ModalWideNavigationRail
This variant is similar to a Modal Drawer. It acts as a standard narrow rail when collapsed, but when expanded, it overlays the main content and casts a shadow, rather than pushing the content to the right.

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModalWideRailExample() {
    val state = rememberWideNavigationRailState()
    val scope = rememberCoroutineScope()
    
    // The Modal version provides a built-in state mechanism instead of a raw boolean
    ModalWideNavigationRail(
        state = state,
        hideOnCollapse = false, // If true, it disappears entirely when collapsed. If false, it acts as a narrow rail.
        header = {
            FloatingActionButton(onClick = { scope.launch { state.expand() } }) {
                Icon(Icons.Default.Add, "Expand")
            }
        }
    ) {
        WideNavigationRailItem(
            selected = true,
            onClick = { scope.launch { state.collapse() } },
            icon = { Icon(Icons.Default.Person, null) },
            label = { Text("Profile") },
            railExpanded = state.currentValue == WideNavigationRailValue.Expanded
        )
    }
}
```

## ⚠️ Common Gotchas

> [!WARNING] Experimental API Flag
> You cannot use this component without annotating your functions (or the whole file) with `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`. It is part of the newest Material evolution and subject to API changes.

> [!CAUTION] Passing `railExpanded`
> Unlike some Compose components where parents implicitly pass state to children, `WideNavigationRailItem` **requires** you to manually pass `railExpanded`. If you forget, the item won't animate its label into view when the parent expands.

> [!TIP] When to use WideRail vs Drawer
> - **PermanentDrawer**: Good for complex apps with hierarchies and deep nested folders.
> - **WideNavigationRail**: Good for modern apps that want a clean, slim rail by default, but want to offer labels for users who need accessibility or clarity without switching to a full heavy drawer.

## 💡 Interview Q&A

**Q: What is the primary visual difference between a `NavigationRailItem` and an expanded `WideNavigationRailItem`?**
A: `NavigationRailItem` places the text label *below* the icon vertically. An expanded `WideNavigationRailItem` places the text label horizontally *next to* the icon.
