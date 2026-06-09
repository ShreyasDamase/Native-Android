# BottomAppBar

## 📌 Purpose
`BottomAppBar` provides access to bottom-anchored actions and (optionally) a Floating Action Button (FAB). 

**Important Distinction:**
- Use `NavigationBar` for *top-level destination switching* (Home, Search, Profile).
- Use `BottomAppBar` for *contextual screen actions* (Edit, Share, Delete, Create).

## 🔧 Function Signature

```kotlin
@Composable
fun BottomAppBar(
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    floatingActionButton: @Composable (() -> Unit)? = null,
    containerColor: Color = BottomAppBarDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomAppBarDefaults.ContainerElevation,
    contentPadding: PaddingValues = BottomAppBarDefaults.ContentPadding,
    windowInsets: WindowInsets = BottomAppBarDefaults.windowInsets,
    scrollBehavior: BottomAppBarScrollBehavior? = null
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `actions` | `@Composable RowScope.() -> Unit` | — | Required. The action icons (typically `IconButton`s) aligned to the start/left. |
| `modifier` | `Modifier` | `Modifier` | Optional modifier. |
| `floatingActionButton` | `@Composable (() -> Unit)?` | `null` | Optional FAB slot. Automatically aligned to the end/right. |
| `scrollBehavior` | `BottomAppBarScrollBehavior?`| `null` | Used to hide the app bar when scrolling down. |

## ✅ Basic Example
Usually placed in the `bottomBar` slot of a `Scaffold`.

```kotlin
@Composable
fun SimpleBottomAppBar() {
    Scaffold(
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { /* do something */ }) {
                        Icon(Icons.Filled.Check, contentDescription = "Mark done")
                    }
                    IconButton(onClick = { /* do something */ }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { /* do something */ }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { /* Create new */ },
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
                        Icon(Icons.Filled.Add, "Add")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Text("Main Content")
        }
    }
}
```

## 🚀 Advanced Examples

### Hiding BottomAppBar on Scroll
Similar to `TopAppBar` scroll behaviors, `BottomAppBar` can react to list scrolling.
This requires attaching a `NestedScrollConnection` to your `Scaffold`.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollingBottomAppBar() {
    val scrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { /* ... */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { }) {
                        Icon(Icons.Default.Add, "Add")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {
            items(50) { index ->
                Text("List item $index", modifier = Modifier.padding(16.dp))
            }
        }
    }
}
```

## ⚠️ Common Gotchas

> [!WARNING] FAB Placement
> If you define a `floatingActionButton` inside the `BottomAppBar`, you generally **do not** use the `floatingActionButton` slot of the `Scaffold` itself. The `BottomAppBar` manages the FAB's specific overlapping layout and elevation automatically.

> [!TIP] Alignment
> Because the `actions` block is a `RowScope`, you can push the FAB further away or create gaps between icons by inserting a `Spacer(Modifier.weight(1f))` between your `IconButton`s.

## 💡 Interview Q&A

**Q: Why does the FAB in `BottomAppBar` look different from a standard FAB?**
A: Material 3 specifies a distinct elevation and color mapping (`BottomAppBarDefaults.bottomAppBarFabColor` and `bottomAppBarFabElevation`) to ensure the FAB blends correctly with the tonal elevation of the BottomAppBar container.

**Q: Should I use `BottomAppBar` for navigating between my Home and Profile screens?**
A: No. Use `NavigationBar` for top-level routing. `BottomAppBar` is strictly for localized screen actions (e.g., editing the current document, sharing the current photo).
