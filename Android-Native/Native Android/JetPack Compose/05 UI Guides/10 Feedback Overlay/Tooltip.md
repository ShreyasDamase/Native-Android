# Tooltip

## 📌 Purpose
Tooltips display informative text when users hover over, focus on, or long-press an element. Compose provides two main varieties:
- `PlainTooltip`: Simple, single-line text providing a brief description (like an icon's label).
- `RichTooltip`: A more complex tooltip that can contain a title, multi-line text, and an action button.

> [!NOTE]
> Tooltips are currently part of the Experimental Material 3 API.

## 🔧 Function Signatures

### TooltipBox (The Container)
```kotlin
@ExperimentalMaterial3Api
@Composable
fun TooltipBox(
    positionProvider: PopupPositionProvider,
    tooltip: @Composable TooltipScope.() -> Unit,
    state: TooltipState,
    modifier: Modifier = Modifier,
    focusable: Boolean = true,
    enableUserInput: Boolean = true,
    content: @Composable BoxScope.() -> Unit
)
```

### PlainTooltip
```kotlin
@ExperimentalMaterial3Api
@Composable
fun TooltipScope.PlainTooltip(
    modifier: Modifier = Modifier,
    caretSize: DpSize = TooltipDefaults.caretSize,
    shape: Shape = TooltipDefaults.plainTooltipContainerShape,
    contentColor: Color = TooltipDefaults.plainTooltipContentColor,
    containerColor: Color = TooltipDefaults.plainTooltipContainerColor,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit
)
```

### RichTooltip
```kotlin
@ExperimentalMaterial3Api
@Composable
fun TooltipScope.RichTooltip(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    caretSize: DpSize = TooltipDefaults.caretSize,
    shape: Shape = TooltipDefaults.richTooltipContainerShape,
    colors: RichTooltipColors = TooltipDefaults.richTooltipColors(),
    tonalElevation: Dp = TooltipDefaults.RichTooltipTonalElevation,
    shadowElevation: Dp = TooltipDefaults.RichTooltipShadowElevation,
    text: @Composable () -> Unit
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `positionProvider` | `PopupPositionProvider` | — | Determines where the tooltip shows. Use `TooltipDefaults.rememberPlainTooltipPositionProvider()` or `rememberRichTooltipPositionProvider()`. |
| `tooltip` | `@Composable` | — | The actual tooltip component (`PlainTooltip` or `RichTooltip`). |
| `state` | `TooltipState` | — | Holds visibility state. Created via `rememberTooltipState()`. |
| `focusable` | `Boolean` | `true` | Whether the tooltip can receive focus. |
| `enableUserInput` | `Boolean` | `true` | Whether long-press triggers the tooltip automatically. |
| `content` | `@Composable` | — | The UI element (like a button) the tooltip is attached to. |

## 🔄 TooltipState
`rememberTooltipState(isPersistent = false)`
- `isPersistent`: If `false` (default for plain), tooltip dismisses on touch outside. If `true` (standard for rich), it stays visible until explicitly dismissed.
- `state.show()` and `state.dismiss()` are suspend functions to manually control visibility.

## ✅ Basic Example

### 1. PlainTooltip on an IconButton
```kotlin
@file:OptIn(ExperimentalMaterial3Api::class)

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun BasicPlainTooltip() {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text("Add to favorites")
            }
        },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = { /* do something */ }) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Favorite"
            )
        }
    }
}
```

## 🚀 Advanced Examples

### 2. RichTooltip with Title and Action
```kotlin
@file:OptIn(ExperimentalMaterial3Api::class)

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AdvancedRichTooltip() {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        tooltip = {
            RichTooltip(
                title = { Text("Network Syncing") },
                action = {
                    TextButton(onClick = { scope.launch { tooltipState.dismiss() } }) {
                        Text("Got it")
                    }
                }
            ) {
                Text("Your data is currently syncing in the background. Please wait a moment.")
            }
        },
        state = tooltipState
    ) {
        IconButton(onClick = { scope.launch { tooltipState.show() } }) {
            Icon(Icons.Filled.Info, contentDescription = "Info")
        }
    }
}
```

### 3. Programmatically Showing a Tooltip
Sometimes you want to show a tooltip without a long press (e.g., as part of an onboarding tutorial).

```kotlin
@file:OptIn(ExperimentalMaterial3Api::class)

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ProgrammaticTooltip() {
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()

    Column {
        Button(onClick = { scope.launch { tooltipState.show() } }) {
            Text("Show tooltip below")
        }
        
        Spacer(Modifier.height(16.dp))

        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text("I was shown programmatically!") } },
            state = tooltipState
        ) {
            Text("Target Element", modifier = Modifier.padding(16.dp))
        }
    }
}
```

## ⚠️ Common Gotchas
- **Placement issues:** If the `TooltipBox` is inside a deeply nested row or column with clipping, the tooltip might get cut off. `TooltipBox` uses a `Popup` under the hood, but `positionProvider` handles screen bounds.
- **isPersistent:** Remember to set `isPersistent = true` for `RichTooltip`s that contain clickable actions (like buttons or links). If it's false, the user clicking the tooltip to press the button will accidentally dismiss the tooltip instead!
- **Suspension:** `tooltipState.show()` suspends until the tooltip is dismissed.

## 💡 Interview Q&A

**Q: How does a user naturally trigger a `PlainTooltip` in an Android app?**
A: By long-pressing the target element (the `content` of the `TooltipBox`). If they are using a mouse (e.g., on a Chromebook), hovering will also trigger it.

**Q: What is the difference between `rememberPlainTooltipPositionProvider()` and `rememberRichTooltipPositionProvider()`?**
A: The plain position provider typically places the tooltip directly above or below the element with a small offset. The rich position provider has different spacing rules to accommodate larger boxes and ensures the rich tooltip stays within screen bounds without obscuring the target entirely.
