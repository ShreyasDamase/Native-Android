# BottomSheetScaffold

## 📌 Purpose
`BottomSheetScaffold` integrates a standard screen scaffold with a **persistent** bottom sheet. 

Unlike `ModalBottomSheet` which blocks the screen, a persistent bottom sheet coexists with the main UI (like the location details sheet in Google Maps or the "Now Playing" bar in a music app). Users can interact with the background content while the sheet is partially or fully expanded.

> [!NOTE] Experimental API
> This component requires `@OptIn(ExperimentalMaterial3Api::class)`.

## 🔧 Function Signature

```kotlin
@ExperimentalMaterial3Api
@Composable
fun BottomSheetScaffold(
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    sheetPeekHeight: Dp = BottomSheetDefaults.SheetPeekHeight, // Default is 56.dp
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetShape: Shape = BottomSheetDefaults.ExpandedShape,
    sheetContainerColor: Color = BottomSheetDefaults.ContainerColor,
    sheetContentColor: Color = contentColorFor(sheetContainerColor),
    sheetTonalElevation: Dp = BottomSheetDefaults.Elevation,
    sheetShadowElevation: Dp = BottomSheetDefaults.Elevation,
    sheetDragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    sheetSwipeEnabled: Boolean = true,
    topBar: @Composable (() -> Unit)? = null,
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable (PaddingValues) -> Unit
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `sheetContent` | `@Composable ColumnScope.() -> Unit` | — | Required. The UI inside the bottom sheet. |
| `scaffoldState` | `BottomSheetScaffoldState` | `remember...()` | Holds the `bottomSheetState` and `snackbarHostState`. |
| `sheetPeekHeight` | `Dp` | `56.dp` | How much of the sheet is visible when it is "collapsed". Set to `0.dp` if you want it completely hidden. |
| `sheetSwipeEnabled` | `Boolean` | `true` | If user can drag the sheet. |
| `content` | `@Composable (PaddingValues) -> Unit` | — | Required. The main background UI. |

## ✅ Basic Example: Google Maps Pattern
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapWithBottomSheet() {
    val scaffoldState = rememberBottomSheetScaffoldState()
    
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 64.dp,
        sheetContent = {
            // The Sheet
            Column(
                Modifier.fillMaxWidth().height(300.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Location Details", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Text("Swipe up to see more info, reviews, and photos.")
            }
        },
        topBar = {
            TopAppBar(title = { Text("Map View") })
        }
    ) { innerPadding ->
        // The Main Background Content
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text("Interactive Map goes here")
        }
    }
}
```

## 🚀 Advanced Examples

### Programmatic Expand/Collapse
Using a button in the main layout to manipulate the sheet.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgrammaticBottomSheet() {
    val scaffoldState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp, // Fully hidden when collapsed
        sheetContent = {
            Column(Modifier.fillMaxWidth().height(200.dp).padding(16.dp)) {
                Text("Hidden Controls")
                Button(onClick = { scope.launch { scaffoldState.bottomSheetState.partialExpand() } }) {
                    Text("Close")
                }
            }
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize()) {
            Button(onClick = { 
                scope.launch { scaffoldState.bottomSheetState.expand() } 
            }) {
                Text("Open Controls Panel")
            }
        }
    }
}
```

## ⚠️ Common Gotchas

> [!WARNING] The `innerPadding` mapping
> In `BottomSheetScaffold`, the `PaddingValues` passed to the `content` block account for the `topBar` and the `sheetPeekHeight`. If you do not apply `Modifier.padding(innerPadding)` to your main content, your content will draw *underneath* the partially exposed bottom sheet.

> [!CAUTION] Overlapping scrollables
> If your `sheetContent` contains a `LazyColumn` or `Scrollable`, the sheet's state machine will automatically intercept upward scrolls. It expands the sheet first, and only once fully expanded does the internal list begin to scroll.

## 💡 Interview Q&A

**Q: How do you hide the bottom sheet completely in `BottomSheetScaffold`?**
A: By default, it never fully hides; it collapses to its `sheetPeekHeight` (56dp). To hide it completely, set `sheetPeekHeight = 0.dp`.

**Q: How do I access the underlying state of the sheet to check if it's expanded?**
A: Use `scaffoldState.bottomSheetState.currentValue`. It will be either `SheetValue.PartiallyExpanded` or `SheetValue.Expanded`.
