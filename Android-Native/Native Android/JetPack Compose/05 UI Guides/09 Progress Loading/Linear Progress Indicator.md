# LinearProgressIndicator

## 📌 Purpose
`LinearProgressIndicator` is a Material Design linear progress bar. It spans horizontally to show either the exact progress of an operation (determinate) or an ongoing, unquantifiable background task (indeterminate).

> [!TIP]
> This is commonly used at the very top of a screen, under a `[[TopAppBar]]`, or within list items to show file upload/download statuses.

## 🔧 Function Signatures

### Determinate
```kotlin
@Composable
fun LinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
    gapSize: Dp = ProgressIndicatorDefaults.LinearIndicatorTrackGapSize,
    drawStopIndicator: DrawScope.() -> Unit = { 
        ProgressIndicatorDefaults.drawStopIndicator(
            drawScope = this,
            stopSize = ProgressIndicatorDefaults.LinearTrackStopIndicatorSize,
            color = color,
            strokeCap = strokeCap
        ) 
    }
)
```

### Indeterminate
```kotlin
@Composable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.LinearStrokeCap
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `progress` | `() -> Float` | — | **Determinate only**. A lambda returning progress between 0.0 and 1.0. |
| `modifier` | `Modifier` | `Modifier` | Modifier to apply to the indicator. |
| `color` | `Color` | `ProgressIndicatorDefaults.linearColor` | The color of the active progress segment. |
| `trackColor` | `Color` | `ProgressIndicatorDefaults.linearTrackColor` | The background color of the track. |
| `strokeCap` | `StrokeCap` | `ProgressIndicatorDefaults.LinearStrokeCap` | Stroke cap for the ends of the progress line (e.g., `StrokeCap.Round`). |
| `gapSize` | `Dp` | `ProgressIndicatorDefaults.LinearIndicatorTrackGapSize` | **Determinate only**. Size of the gap between the active progress and the remaining track. |
| `drawStopIndicator` | `DrawScope.() -> Unit` | Default indicator | **Determinate only**. Lambda to draw the visual "stop" or endpoint indicator at 100%. |

## ✅ Basic Example

```kotlin
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BasicLinearLoading() {
    // Indeterminate loading
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
}
```

## 🚀 Advanced Examples

### 1. Upload Progress Bar (Determinate)
```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun UploadProgressBar() {
    var progress by remember { mutableFloatStateOf(0.3f) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Uploading file... ${(progress * 100).toInt()}%")
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            gapSize = 4.dp // Adds a distinct gap before the track
        )
    }
}
```

### 2. Top-of-Screen Loading Indicator
```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopLoadingScreen(isLoading: Boolean) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("My App") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // Screen content...
        }
    }
}
```

### 3. Custom Styled Linear Progress
```kotlin
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun StyledLinearIndicator() {
    LinearProgressIndicator(
        progress = { 0.75f },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp), // Makes it thicker
        color = Color.Green,
        trackColor = Color.LightGray,
        strokeCap = StrokeCap.Round
    )
}
```

### 4. Animated Progress Update
```kotlin
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedLinearProgress() {
    var targetProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(1000),
        label = "linear_progress"
    )

    Column(modifier = Modifier.padding(16.dp)) {
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { targetProgress = (targetProgress + 0.25f).coerceAtMost(1f) }) {
            Text("Step Forward")
        }
    }
}
```

### 5. LinearProgressIndicator in a Card
```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingCard() {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Processing Data", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
```

## ⚠️ Common Gotchas
- **Default Width:** By default, it takes up minimal intrinsic width unless you specify `Modifier.fillMaxWidth()`. Always specify width constraints!
- **Animation recompositions:** Always use the `progress: () -> Float` overload when animating to avoid causing recompositions of the entire hierarchy during the animation.
- **Height customization:** Use `Modifier.height(Dp)` to make it thicker. Note that applying a very large height with `StrokeCap.Round` might look weird at the edges.

## 💡 Interview Q&A

**Q: How does the new `drawStopIndicator` parameter work?**
A: Introduced in modern Material 3 iterations, `drawStopIndicator` provides a DrawScope block where you can paint an element at the 100% mark (the end of the track). By default, it renders a small circle or line (depending on stroke cap) indicating where the progress will end, giving better visual context to the user.

**Q: When would you use `LinearProgressIndicator` vs `[[CircularProgressIndicator]]`?**
A: Use a linear indicator when space is tight vertically but wide horizontally (like underneath an app bar, or inside a list item tracking a download). Use a circular indicator for localized states like inside a button or centered on an empty screen.
