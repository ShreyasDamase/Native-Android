# NavigationDrawer

## 📌 Purpose
Drawers provide access to destinations and app functionality, typically hidden off-screen to save space. Material 3 offers three distinct drawer types:
1. `ModalNavigationDrawer`: Overlays the screen with a dark scrim. Standard for phones.
2. `PermanentNavigationDrawer`: Always visible on screen alongside content. Standard for large tablets/desktop.
3. `DismissibleNavigationDrawer`: Slides in and squishes content instead of overlaying it (no scrim). Standard for medium tablets.

## 🔧 Function Signatures

### 1. `ModalNavigationDrawer`
```kotlin
@Composable
fun ModalNavigationDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    scrimColor: Color = DrawerDefaults.scrimColor,
    content: @Composable () -> Unit
)
```

### 2. `NavigationDrawerItem`
```kotlin
@Composable
fun NavigationDrawerItem(
    label: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    shape: Shape = NavigationDrawerItemDefaults.ItemShape,
    colors: NavigationDrawerItemColors = NavigationDrawerItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `drawerContent` | `@Composable () -> Unit` | — | The actual drawer UI. You **MUST** wrap items inside a `ModalDrawerSheet` for standard M3 styling. |
| `drawerState` | `DrawerState` | `Closed` | Tracks if drawer is Open or Closed. Exposes `open()` and `close()` suspend functions. |
| `gesturesEnabled` | `Boolean` | `true` | If true, user can swipe from the left edge to open it. |
| `badge` | `@Composable` | `null` | Slot for trailing information (e.g. "99+" unread count). |

## ✅ Basic Example: Modal Drawer with Hamburger
This requires a `CoroutineScope` because opening/closing the drawer is an animated, suspendable action.

```kotlin
@Composable
fun BasicDrawer() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedItem by remember { mutableStateOf("Inbox") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet { // Provides standard M3 background, shape, and width
                Spacer(Modifier.height(16.dp))
                Text("Mail App", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                HorizontalDivider()
                
                NavigationDrawerItem(
                    label = { Text("Inbox") },
                    selected = selectedItem == "Inbox",
                    onClick = { 
                        selectedItem = "Inbox"
                        scope.launch { drawerState.close() } 
                    },
                    icon = { Icon(Icons.Default.Inbox, contentDescription = null) },
                    badge = { Text("24") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        // Main Screen Content
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Home") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Menu")
                        }
                    }
                )
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) { Text("Content for $selectedItem") }
        }
    }
}
```

## 🚀 Advanced Examples

### 1. Permanent Drawer on Tablet
Always visible, no state object needed to track open/closed, no scrim.

```kotlin
@Composable
fun TabletPermanentDrawer() {
    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(Modifier.width(240.dp)) {
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    label = { Text("Dashboard") },
                    selected = true,
                    onClick = { /* Navigate */ }
                )
            }
        }
    ) {
        // App Content sits to the right of the drawer
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Main Content Area")
        }
    }
}
```

### 2. Dismissible Drawer
Slides in and resizes the main content area alongside it.

```kotlin
@Composable
fun SplitScreenDismissibleDrawer() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    
    DismissibleNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DismissibleDrawerSheet {
                // items...
            }
        }
    ) {
        // Content gets pushed to the right when open
    }
}
```

## ⚠️ Common Gotchas

> [!IMPORTANT] Use DrawerSheets!
> Do not just put a `Column` directly into `drawerContent`. Always use `ModalDrawerSheet`, `PermanentDrawerSheet`, or `DismissibleDrawerSheet`. These wrappers provide the correct Material 3 width (usually 360dp max), background color, and rounded edge styling.

> [!CAUTION] Item Padding
> Notice the modifier on the item in the basic example: `Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)`. Material 3 drawer items should NOT touch the edges of the drawer. You must apply this padding manually.

> [!WARNING] Coroutines
> `drawerState.open()` is a `suspend fun`. You cannot call it directly in an `onClick` lambda without launching a coroutine (`scope.launch { ... }`).

## 💡 Interview Q&A

**Q: Where should the `Scaffold` go when using a `ModalNavigationDrawer`?**
A: The `Scaffold` should be placed **inside** the `content` block of the `ModalNavigationDrawer`. The drawer is the outermost parent so it can overlay the TopAppBar and FloatingActionButton.

**Q: What is the difference between `ModalNavigationDrawer` and `DismissibleNavigationDrawer`?**
A: `ModalNavigationDrawer` places a dimming scrim over the app content, preventing interaction with the background. `DismissibleNavigationDrawer` has no scrim; instead, it literally pushes the main layout to the side, allowing the user to view both the drawer and the reshaped content simultaneously.
