# Scaffold

## 📌 Purpose
`Scaffold` is the ultimate architectural root layout for a Material Design screen. 
It provides dedicated slots for top-level UI components so they align perfectly according to Material guidelines: `TopAppBar`, `BottomAppBar`/`NavigationBar`, `FloatingActionButton`, and `Snackbar`.

## 🔧 Function Signature

```kotlin
@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `topBar` | `@Composable () -> Unit` | `{}` | Slot for `TopAppBar` or `CenterAlignedTopAppBar`. |
| `bottomBar` | `@Composable () -> Unit` | `{}` | Slot for `NavigationBar` or `BottomAppBar`. |
| `snackbarHost` | `@Composable () -> Unit` | `{}` | Slot for `SnackbarHost(hostState)`. Manages toast-like messages. |
| `floatingActionButton` | `@Composable () -> Unit` | `{}` | Slot for `FloatingActionButton`. |
| `floatingActionButtonPosition` | `FabPosition` | `.End` | `End` (bottom right), `Center` (bottom center), `EndOverlay`, `CenterOverlay`. |
| `content` | `@Composable (PaddingValues) -> Unit` | — | Required. Main screen UI. **Must apply PaddingValues!** |

## ✅ Basic Example: The Full Scaffold
A screen utilizing almost all the slots.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("App Name") })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, null) })
                NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.Person, null) })
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scope.launch {
                    snackbarHostState.showSnackbar("FAB Clicked")
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        // CRITICAL: Apply innerPadding
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        ) {
            items(20) {
                Text("List item $it", modifier = Modifier.padding(16.dp))
            }
        }
    }
}
```

## 🚀 Advanced Examples

### Nested Scaffolds
It is common to have an outer Scaffold at the `NavHost` level for bottom navigation, and inner Scaffolds for individual screens that need a custom TopAppBar.

```kotlin
@Composable
fun OuterScaffold() {
    Scaffold(
        bottomBar = { NavigationBar { /* ... */ } }
    ) { outerPadding ->
        // This box simulates a NavHost
        Box(Modifier.padding(outerPadding)) {
            InnerScreenScaffold()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InnerScreenScaffold() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Profile Details") }) },
        floatingActionButton = { FloatingActionButton(onClick = {}) { Icon(Icons.Default.Edit, null) } }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            Text("Inner content")
        }
    }
}
```

## ⚠️ Common Gotchas

> [!CAUTION] Ignoring PaddingValues
> The `content` lambda provides `PaddingValues`. This padding represents the height of the `topBar` and `bottomBar`. **If you do not apply this padding to your content via `Modifier.padding()` or `contentPadding`, your UI will draw underneath the app bars.**

> [!WARNING] Snackbar Visibility
> If you trigger a Snackbar via `SnackbarHostState` but forget to provide `snackbarHost = { SnackbarHost(snackbarHostState) }` in your Scaffold, the Snackbar will never appear on screen.

> [!TIP] WindowInsets Consumption
> If your app is drawing Edge-to-Edge, `Scaffold` automatically applies system window insets (status bar, nav bar) to the `PaddingValues`.

## 💡 Interview Q&A

**Q: Why does the FAB move up when a Snackbar appears?**
A: `Scaffold` has built-in logic to coordinate its slots. When it detects a `Snackbar` entering the UI, it automatically translates the `floatingActionButton` upwards so it isn't obscured by the message.

**Q: Can I use `Modifier.consumeWindowInsets(innerPadding)` instead of `Modifier.padding(innerPadding)`?**
A: Yes! If your content handles its own insets (like a custom full-bleed layout), `consumeWindowInsets` tells the framework "I acknowledge this padding, but don't force it on me, just subtract it from the remaining inset tracking."
