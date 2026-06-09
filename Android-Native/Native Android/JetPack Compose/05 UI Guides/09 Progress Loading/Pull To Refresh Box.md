# PullToRefreshBox

## 📌 Purpose
`PullToRefreshBox` is the modern Material 3 standard for "Swipe-to-Refresh" functionality in Jetpack Compose. It acts as a container over scrollable content (like `LazyColumn`). When the user pulls down from the top, it reveals a refresh indicator and triggers a callback.

> [!NOTE]
> This component replaces the older accompanist swipe-to-refresh and earlier iteration `PullRefreshIndicator` modifier patterns. It is much easier to use as a simple Box wrapper!

## 🔧 Function Signature
```kotlin
@ExperimentalMaterial3Api
@Composable
fun PullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    contentAlignment: Alignment = Alignment.TopStart,
    indicator: @Composable BoxScope.() -> Unit = { 
        PullToRefreshDefaults.Indicator(state = state, isRefreshing = isRefreshing) 
    },
    enabled: Boolean = true,
    threshold: Dp = PullToRefreshDefaults.PositionalThreshold,
    content: @Composable BoxScope.() -> Unit
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `isRefreshing` | `Boolean` | — | Whether the refresh indicator should currently be showing its "refreshing" state. |
| `onRefresh` | `() -> Unit` | — | Callback fired when the pull distance exceeds the `threshold` and the user releases. |
| `modifier` | `Modifier` | `Modifier` | Modifier for the box container. |
| `state` | `PullToRefreshState` | `rememberPullToRefreshState()` | Holds the internal state of the pull (e.g., `distanceFraction`). |
| `contentAlignment` | `Alignment` | `Alignment.TopStart` | Alignment of the internal content. |
| `indicator` | `@Composable BoxScope.() -> Unit` | Default spinning indicator | The visual indicator component that pulls down from the top. |
| `enabled` | `Boolean` | `true` | Whether pull-to-refresh gestures are enabled. |
| `threshold` | `Dp` | `PullToRefreshDefaults.PositionalThreshold` | The distance the user must pull to trigger a refresh. |
| `content` | `@Composable BoxScope.() -> Unit` | — | The main scrollable content (e.g., `LazyColumn`). |

## 🔄 PullToRefreshState
The `state` object contains useful properties if you are building a custom indicator:
- `state.distanceFraction`: A `Float` representing how far the user has pulled relative to the `threshold`. (0.0 = start, 1.0 = threshold reached).

## ✅ Basic Example

```kotlin
@file:OptIn(ExperimentalMaterial3Api::class)

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BasicPullToRefresh() {
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            // Simulate network request
            coroutineScope.launch {
                delay(2000)
                isRefreshing = false
            }
        }
    ) {
        LazyColumn {
            items(20) { index ->
                ListItem(headlineContent = { Text("Item $index") })
            }
        }
    }
}
```

## 🚀 Advanced Examples

### 1. Integration with ViewModel & Coroutines
```kotlin
@file:OptIn(ExperimentalMaterial3Api::class)

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MyViewModel : ViewModel() {
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(1500) // Network call
            _isRefreshing.value = false
        }
    }
}

@Composable
fun ViewModelPullToRefresh(viewModel: MyViewModel = MyViewModel()) {
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshData() }
    ) {
        LazyColumn {
            items(10) { Text("Data loaded from VM $it") }
        }
    }
}
```

### 2. Custom Indicator Design
```kotlin
@file:OptIn(ExperimentalMaterial3Api::class)

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CustomIndicatorPullToRefresh() {
    var isRefreshing by remember { mutableStateOf(false) }
    val state = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            scope.launch { delay(2000); isRefreshing = false }
        },
        state = state,
        indicator = {
            // Custom Indicator UI
            val scale = (state.distanceFraction).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .scale(if (isRefreshing) 1f else scale)
                    .clip(CircleShape)
                    .background(Color.Red)
                    .padding(8.dp)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                        contentDescription = "Pull to refresh",
                        tint = Color.White
                    )
                }
            }
        }
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(5) { Text("Custom Refresh Item $it", Modifier.padding(16.dp)) }
        }
    }
}
```

## ⚠️ Common Gotchas
- **Missing Scrollable Content:** `PullToRefreshBox` relies on nested scrolling. The `content` inside it **must** be a scrollable container (like `LazyColumn`, `LazyVerticalGrid`, or `Column(Modifier.verticalScroll())`). If it's not scrollable, the gesture will not trigger!
- **State mismatch:** Ensure `isRefreshing` accurately reflects your data fetching state. If you set it to `false` too early, the indicator will snap back before the new data is painted.

## 💡 Interview Q&A

**Q: How does `PullToRefreshBox` intercept touch events?**
A: It relies on Compose's **Nested Scroll Connection**. When the user pulls down, if the inner scrollable list is at the very top (scroll offset is 0), the `PullToRefreshBox` intercepts the remaining scroll delta to drag the indicator down.

**Q: How do you change the distance required to trigger a refresh?**
A: Adjust the `threshold` parameter in `PullToRefreshBox`. By default, it uses `PullToRefreshDefaults.PositionalThreshold`.
