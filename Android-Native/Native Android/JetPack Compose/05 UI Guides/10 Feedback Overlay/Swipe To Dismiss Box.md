# SwipeToDismissBox

## 📌 Purpose
`SwipeToDismissBox` is a Material 3 component that allows users to swipe a UI element horizontally to reveal background actions or delete the item. This is commonly seen in email apps (swipe to archive/delete) and notification centers.

> [!NOTE]
> Added in M3 as `SwipeToDismissBox`. It replaces the older `SwipeToDismiss` component from accompanist and earlier Compose versions.

## 🔧 Function Signature
```kotlin
@ExperimentalMaterial3Api
@Composable
fun SwipeToDismissBox(
    state: SwipeToDismissBoxState,
    backgroundContent: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    enableDismissFromStartToEnd: Boolean = true,
    enableDismissFromEndToStart: Boolean = true,
    gesturesEnabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `state` | `SwipeToDismissBoxState` | — | The state managing the swipe progress and anchors. |
| `backgroundContent` | `@Composable RowScope.() -> Unit` | — | The UI revealed *behind* the item as it's swiped. |
| `modifier` | `Modifier` | `Modifier` | Modifier for the container. |
| `enableDismissFromStartToEnd` | `Boolean` | `true` | Allows swiping Right (LTR). |
| `enableDismissFromEndToStart` | `Boolean` | `true` | Allows swiping Left (LTR). |
| `gesturesEnabled` | `Boolean` | `true` | Toggles whether swipe gestures are active. |
| `content` | `@Composable RowScope.() -> Unit` | — | The foreground item (the thing being swiped). |

## 🔄 SwipeToDismissBoxState
You create the state using `rememberSwipeToDismissBoxState()`.
```kotlin
val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { dismissValue ->
        when (dismissValue) {
            SwipeToDismissBoxValue.StartToEnd -> { /* Swiped right! Return true to dismiss */ true }
            SwipeToDismissBoxValue.EndToStart -> { /* Swiped left! Return true to dismiss */ true }
            SwipeToDismissBoxValue.Settled -> false
        }
    },
    positionalThreshold = { totalDistance -> totalDistance * 0.5f } // Need to swipe 50% to trigger
)
```
- `dismissState.dismissDirection`: Returns the current swipe direction (or `null`). Use this to change the background color/icon dynamically!
- `dismissState.progress`: Float between 0 and 1 indicating how far the swipe has traveled.

## ✅ Basic Example

### 1. Delete on Swipe (One Direction)
```kotlin
@file:OptIn(ExperimentalMaterial3Api::class)

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BasicSwipeToDismiss(onRemove: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onRemove()
                return@rememberSwipeToDismissBoxState true
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false, // Disable right swipe
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Red)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        }
    ) {
        // Foreground Content
        Card(Modifier.fillMaxWidth()) {
            Text("Swipe me left to delete!", Modifier.padding(16.dp))
        }
    }
}
```

## 🚀 Advanced Examples

### 2. Multi-Directional Swipe (Archive/Delete)
Change background color and icon based on `dismissDirection`.

```kotlin
@file:OptIn(ExperimentalMaterial3Api::class)

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MultiDirectionSwipeToDismiss() {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { /* Archive Logic */ true }
                SwipeToDismissBoxValue.EndToStart -> { /* Delete Logic */ true }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Color.Green // Archive
                SwipeToDismissBoxValue.EndToStart -> Color.Red   // Delete
                else -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Archive
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                else -> null
            }

            Box(
                Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = Color.White)
                }
            }
        }
    ) {
        ListItem(headlineContent = { Text("Email from Boss") }, supportingContent = { Text("Read this ASAP") })
    }
}
```

### 3. SwipeToDismissBox in LazyColumn with Animated Removal
To animate the height shrinking when an item is deleted, wrap `SwipeToDismissBox` in an `AnimatedVisibility`.

```kotlin
@file:OptIn(ExperimentalMaterial3Api::class)

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun SwipeListExample() {
    var items by remember { mutableStateOf(listOf("Apple", "Banana", "Cherry")) }

    LazyColumn(Modifier.fillMaxSize()) {
        items(items, key = { it }) { fruit ->
            var isDismissed by remember { mutableStateOf(false) }
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    if (it != SwipeToDismissBoxValue.Settled) {
                        isDismissed = true
                        true
                    } else false
                }
            )

            // Optional: Launch effect to actually remove from list after animation completes
            LaunchedEffect(isDismissed) {
                if (isDismissed) {
                    kotlinx.coroutines.delay(300) // Match animation duration
                    items = items.filter { it != fruit }
                }
            }

            AnimatedVisibility(
                visible = !isDismissed,
                exit = shrinkVertically(tween(300)) + fadeOut()
            ) {
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = { /* ... */ }
                ) {
                    ListItem(headlineContent = { Text(fruit) })
                }
            }
        }
    }
}
```

## ⚠️ Common Gotchas
- **State Keys in LazyColumn:** When using `SwipeToDismissBox` inside a `LazyColumn`, you **must** provide a unique `key` to the `items` builder. If you don't, Compose will reuse the dismissed state for the next item that scrolls into view!
- **State Confirm Change:** `confirmValueChange` must return `true` if you want the item to slide off the screen. If you return `false`, the item will snap back to the center.

## 💡 Interview Q&A

**Q: How do you change the background color depending on which way the user is swiping?**
A: Inside the `backgroundContent` lambda, read `dismissState.dismissDirection`. It will be `StartToEnd`, `EndToStart`, or `Settled`. Use a `when` statement to assign different colors or icons.

**Q: Why doesn't `SwipeToDismissBox` delete the item from my list automatically?**
A: Compose state is hoisted. `SwipeToDismissBox` only handles the *visual* swipe state. Your `confirmValueChange` lambda must trigger a callback or update a state variable in your ViewModel to actually remove the underlying data object from your list.
