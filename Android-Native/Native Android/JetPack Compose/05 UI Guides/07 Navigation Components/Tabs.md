# Tabs

## 📌 Purpose
Tabs organize content across different screens, data sets, or interactions. 
- `TabRow`: Fixed width, distributes tabs equally. Best for 2-4 tabs.
- `ScrollableTabRow`: Horizontally scrollable. Best for 5+ tabs.
- `Tab`: A standard text/icon tab.
- `LeadingIconTab`: A specific layout where the icon is on the left of the text instead of above it.

## 🔧 Function Signatures

### `TabRow`
```kotlin
@Composable
fun TabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = TabRowDefaults.primaryContainerColor,
    contentColor: Color = TabRowDefaults.primaryContentColor,
    indicator: @Composable (tabPositions: List<TabPosition>) -> Unit = @Composable { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
        )
    },
    divider: @Composable () -> Unit = @Composable {
        HorizontalDivider()
    },
    tabs: @Composable () -> Unit
)
```

*(Note: `ScrollableTabRow` has identical parameters plus `edgePadding: Dp`)*

### `Tab`
```kotlin
@Composable
fun Tab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor,
    interactionSource: MutableInteractionSource? = null
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `selectedTabIndex` | `Int` | — | Required. The currently active tab index. Tells the `indicator` where to draw. |
| `indicator` | `@Composable` | M3 Default Indicator | Customizes the underline beneath the selected tab. |
| `tabs` | `@Composable` | — | Required. A block containing multiple `Tab` composables. |

## ✅ Basic Example: Fixed TabRow
```kotlin
@Composable
fun BasicTabs() {
    var state by remember { mutableIntStateOf(0) }
    val titles = listOf("Home", "Library", "Settings")

    Column {
        TabRow(selectedTabIndex = state) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = state == index,
                    onClick = { state = index },
                    text = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                )
            }
        }
        
        // Content switching based on state
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Displaying content for ${titles[state]}")
        }
    }
}
```

## 🚀 Advanced Examples

### 1. ScrollableTabRow (For Many Categories)
```kotlin
@Composable
fun ScrollableCategories() {
    var state by remember { mutableIntStateOf(0) }
    val categories = listOf("All", "Music", "Podcasts", "Live", "Gaming", "News", "Sports")

    ScrollableTabRow(
        selectedTabIndex = state,
        edgePadding = 16.dp // Padding at the start and end of the scrollable row
    ) {
        categories.forEachIndexed { index, category ->
            Tab(
                selected = state == index,
                onClick = { state = index },
                text = { Text(category) }
            )
        }
    }
}
```

### 2. Tabs with HorizontalPager Integration (Smooth Swiping)
The most common and powerful way to use tabs in Android.
*(Requires `androidx.compose.foundation.pager.HorizontalPager`)*

```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerTabs() {
    val titles = listOf("Chats", "Status", "Calls")
    val pagerState = rememberPagerState(pageCount = { titles.size })
    val coroutineScope = rememberCoroutineScope()

    Column {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        // Animate pager scroll when tab is clicked
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = { Text(title) }
                )
            }
        }
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Page: ${titles[page]}")
            }
        }
    }
}
```

### 3. Custom Animated Indicator
> [!WARNING] Deprecation Notice
> The old `Modifier.tabIndicatorOffset()` using `TabPosition` lists is frequently flagged or deprecated in favor of the new Material 3 `TabIndicatorScope`. Below is the modern way to do custom indicators if you are using newer M3 artifacts.

*(Assuming latest M3 `PrimaryTabRow` / `SecondaryTabRow` which provide `TabIndicatorScope`)*
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernIndicatorTabs() {
    var state by remember { mutableIntStateOf(0) }
    val titles = listOf("A", "B", "C")

    PrimaryTabRow(selectedTabIndex = state) {
        titles.forEachIndexed { index, title ->
            Tab(
                selected = state == index,
                onClick = { state = index },
                text = { Text(title) }
            )
        }
    }
}
```

## ⚠️ Common Gotchas

> [!CAUTION] Infinite Recomposition loop
> Never mutate your tab `state` variable directly inside the `tabs` composable tree unless it is within an `onClick` lambda.

> [!IMPORTANT] `LeadingIconTab` Strictness
> `LeadingIconTab` requires **both** `icon` and `text` parameters. If you only want one or the other, use standard `Tab`. Standard `Tab` stacks the icon *above* the text.

> [!TIP] Pager Syncing
> When syncing `TabRow` with `HorizontalPager`, always use `pagerState.currentPage` as the `selectedTabIndex`. Do not maintain a separate `selectedTab` integer state, or they will drift out of sync during swipe gestures.

## 💡 Interview Q&A

**Q: What is the difference between `TabRow` and `ScrollableTabRow`?**
A: `TabRow` divides the screen width equally among all tabs. If you have 6 tabs, they will squash together and look terrible. `ScrollableTabRow` sizes each tab based on its content width and allows the user to swipe horizontally to see overflowing tabs.
