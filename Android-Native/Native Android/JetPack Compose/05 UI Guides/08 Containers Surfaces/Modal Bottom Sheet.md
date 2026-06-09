# ModalBottomSheet

## 📌 Purpose
`ModalBottomSheet` is a surface anchored to the bottom of the screen that presents supplemental content. Being "Modal" means it blocks interaction with the rest of the screen (using a dark scrim) until dismissed.

> [!NOTE] Experimental API
> This component currently requires `@OptIn(ExperimentalMaterial3Api::class)`.

## 🔧 Function Signature

```kotlin
@ExperimentalMaterial3Api
@Composable
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomSheetDefaults.Elevation,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    windowInsets: WindowInsets = BottomSheetDefaults.windowInsets,
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    content: @Composable ColumnScope.() -> Unit
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `onDismissRequest` | `() -> Unit` | — | Required. Called when user taps the scrim or swipes the sheet down. |
| `sheetState` | `SheetState` | `rememberModalBottomSheetState()` | Tracks internal state (`Hidden`, `Expanded`, `PartiallyExpanded`). |
| `dragHandle` | `@Composable` | `BottomSheetDefaults.DragHandle()` | The small pill at the top of the sheet indicating it can be swiped. |
| `scrimColor` | `Color` | `BottomSheetDefaults.ScrimColor` | Color overlaid on the rest of the app. |
| `content` | `@Composable ColumnScope.() -> Unit` | — | Required. The UI inside the sheet. |

### `SheetState` Construction
```kotlin
val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = false, // If true, sheet opens fully and ignores the half-way peek state
    confirmValueChange = { /* return false to veto a state change */ }
)
```

## ✅ Basic Example
Like Dialogs, ModalBottomSheets are typically conditionally rendered based on a state boolean.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicBottomSheetExample() {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { showSheet = true }) {
                Text("Show Bottom Sheet")
            }
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState
            ) {
                // Sheet content
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Settings", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        // Correctly hiding before removing from composition
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) { showSheet = false }
                        }
                    }) {
                        Text("Close Sheet")
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
```

## 🚀 Advanced Examples

### 1. Action Sheet (List of Options)
A common pattern for replacing popups or menus.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSheetExample(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Optional: remove the drag handle for a cleaner look if content is small
        dragHandle = null 
    ) {
        Column(Modifier.padding(bottom = 32.dp)) {
            ListItem(
                headlineContent = { Text("Share") },
                leadingContent = { Icon(Icons.Default.Share, null) },
                modifier = Modifier.clickable { /* Handle click */ }
            )
            ListItem(
                headlineContent = { Text("Copy Link") },
                leadingContent = { Icon(Icons.Default.Link, null) },
                modifier = Modifier.clickable { /* Handle click */ }
            )
            ListItem(
                headlineContent = { Text("Delete") },
                leadingContent = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                modifier = Modifier.clickable { /* Handle click */ }
            )
        }
    }
}
```

### 2. Scrollable Content and Peek Height
If your content is taller than the screen, `ModalBottomSheet` automatically handles internal scrolling. If `skipPartiallyExpanded = false` (the default), it will first open to half-screen, and a second upward swipe will expand it fully.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollableBottomSheet() {
    var show by remember { mutableStateOf(false) }
    
    // By default, it will peek. If we wanted it to open full-screen immediately, 
    // we would set skipPartiallyExpanded = true
    val state = rememberModalBottomSheetState()

    if (show) {
        ModalBottomSheet(onDismissRequest = { show = false }, sheetState = state) {
            LazyColumn {
                items(50) { index ->
                    ListItem(headlineContent = { Text("Item $index") })
                }
            }
        }
    }
}
```

## ⚠️ Common Gotchas

> [!WARNING] The Disappearing Animation Bug
> If you close the sheet programmatically by simply setting `showSheet = false`, it will instantly vanish without animating down. You MUST call `sheetState.hide()` inside a coroutine, and ONLY set `showSheet = false` after `hide()` completes.
```kotlin
scope.launch { 
    sheetState.hide() 
}.invokeOnCompletion { 
    if (!sheetState.isVisible) showSheet = false 
}
```

> [!IMPORTANT] Window Insets
> By default, `ModalBottomSheet` extends behind the system navigation bar (drawing the background color) but pads its content. This is usually what you want.

## 💡 Interview Q&A

**Q: What is the difference between `ModalBottomSheet` and `BottomSheetScaffold`?**
A: `ModalBottomSheet` acts like a dialog—it overlays the app, darkens the background, and prevents interaction with the underlying screen until closed. `BottomSheetScaffold` is a persistent sheet that coexists with the main content (like Google Maps), allowing the user to interact with the map while the sheet is visible.

**Q: How do you force a ModalBottomSheet to open fully instead of stopping halfway?**
A: Initialize your state with `rememberModalBottomSheetState(skipPartiallyExpanded = true)`.
