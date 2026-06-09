# TopAppBar

## 📌 Purpose
The `TopAppBar` displays information and actions relating to the current screen. It is typically placed at the top of the screen within a `Scaffold`'s `topBar` slot.

Material 3 provides four variants (all currently `@ExperimentalMaterial3Api`):
1. **TopAppBar**: Simple, single-line bar with a start-aligned title.
2. **CenterAlignedTopAppBar**: Simple, single-line bar with a centered title.
3. **MediumTopAppBar**: Starts as two lines (title on second line). On scroll, collapses to a single line.
4. **LargeTopAppBar**: Starts very tall (large title text). On scroll, smoothly scales and collapses to a single line.

## 🔧 Function Signatures

### 1. TopAppBar (and CenterAlignedTopAppBar)
```kotlin
@ExperimentalMaterial3Api
@Composable
fun TopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null
)
```

### 2. LargeTopAppBar (and MediumTopAppBar)
```kotlin
@ExperimentalMaterial3Api
@Composable
fun LargeTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = TopAppBarDefaults.LargeAppBarExpandedHeight,
    collapsedHeight: Dp = TopAppBarDefaults.LargeAppBarCollapsedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.largeTopAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null
)
```

## 📋 Shared Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `title` | `@Composable` | — | The title to be displayed (usually a `Text`). |
| `navigationIcon` | `@Composable` | `{}` | The icon displayed at the start (usually a back arrow or hamburger menu). |
| `actions` | `@Composable RowScope` | `{}` | Icons displayed at the end. |
| `colors` | `TopAppBarColors` | Defaults | Controls container and content colors. `TopAppBarDefaults` provides builders for these. |
| `scrollBehavior` | `TopAppBarScrollBehavior?`| `null` | Controls how the app bar responds to scrolling content below it. |
| `expandedHeight` | `Dp` | varies | The height of the bar when fully expanded. |
| `collapsedHeight` | `Dp` | varies | **Medium/Large only.** The height when collapsed. |

## 🔄 Scroll Behavior Options
You obtain a `ScrollBehavior` using `TopAppBarDefaults`:
- `pinnedScrollBehavior()`: App bar stays pinned at the top.
- `enterAlwaysScrollBehavior()`: App bar instantly scrolls off-screen when scrolling down, and instantly appears when scrolling up.
- `exitUntilCollapsedScrollBehavior()`: App bar collapses to its `collapsedHeight` when scrolling down, but the collapsed version remains pinned.

> [!IMPORTANT]
> To make `scrollBehavior` work, you **must** attach its `nestedScrollConnection` to your `Scaffold`'s modifier!

## ✅ Basic Example

### 1. Simple TopAppBar with Back Button
```kotlin
@file:OptIn(ExperimentalMaterial3Api::class)

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun SimpleAppBar() {
    TopAppBar(
        title = { Text("Profile") },
        navigationIcon = {
            IconButton(onClick = { /* Go Back */ }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        },
        actions = {
            IconButton(onClick = { /* Favorite */ }) {
                Icon(Icons.Filled.Favorite, "Favorite")
            }
        }
    )
}
```

## 🚀 Advanced Examples

### 2. CenterAlignedTopAppBar
```kotlin
@file:OptIn(ExperimentalMaterial3Api::class)

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun CenteredAppBar() {
    CenterAlignedTopAppBar(
        title = { Text("My App") },
        navigationIcon = {
            IconButton(onClick = { /* Open Drawer */ }) {
                Icon(Icons.Default.Menu, "Menu")
            }
        }
    )
}
```

### 3. LargeTopAppBar with Scroll Behavior
This is the modern standard for Android apps.

```kotlin
@file:OptIn(ExperimentalMaterial3Api::class)

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

@Composable
fun LargeCollapsingAppBar() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        // MUST ATTACH NESTED SCROLL CONNECTION HERE
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface // Color changes when collapsed!
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            items(50) { index ->
                ListItem(headlineContent = { Text("Setting item $index") })
            }
        }
    }
}
```

## ⚠️ Common Gotchas
- **Nested Scroll Connection:** If you pass a `scrollBehavior` to the `TopAppBar` but forget to add `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)` to the parent `Scaffold`, the App Bar will NOT collapse when the list scrolls!
- **State remembrance:** Use `rememberTopAppBarState()` inside the scroll behavior to ensure scroll state survives recompositions.
- **Color changes:** Material 3 AppBars dynamically change color when content scrolls underneath them (controlled by `scrolledContainerColor`). If you want a solid color that never changes, set both `containerColor` and `scrolledContainerColor` to the same value.

## 💡 Interview Q&A

**Q: What is the difference between `MediumTopAppBar` and `LargeTopAppBar`?**
A: Both collapse down to a single line on scroll. The difference is their initial expanded height and text sizing. `LargeTopAppBar` has a much larger, prominent title font and takes up more vertical space initially compared to `MediumTopAppBar`.

**Q: How do you make an App Bar disappear completely when the user scrolls down, but reappear instantly when they scroll up?**
A: Use `TopAppBarDefaults.enterAlwaysScrollBehavior()` and attach it to both the `TopAppBar` and the `Scaffold`'s nested scroll modifier.
