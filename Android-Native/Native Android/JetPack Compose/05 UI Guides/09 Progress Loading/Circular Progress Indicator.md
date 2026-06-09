# CircularProgressIndicator

## 📌 Purpose
`CircularProgressIndicator` is a Material Design circular progress indicator. It visually shows the progress of an operation or simply that the application is busy. It comes in two flavors: **determinate** (where the progress is known and shown as a fraction) and **indeterminate** (where the progress is unknown and an animated spinning circle is shown). 

> [!NOTE]
> Use this for local loading states (like an image loading, or a form submitting), rather than global loading states where a `LinearProgressIndicator` at the top of the screen might be more appropriate.

## 🔧 Function Signatures

### Determinate
```kotlin
@Composable
fun CircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth,
    trackColor: Color = ProgressIndicatorDefaults.circularTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
    gapSize: Dp = ProgressIndicatorDefaults.CircularIndicatorTrackGapSize
)
```

### Indeterminate
```kotlin
@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth,
    trackColor: Color = ProgressIndicatorDefaults.circularTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.CircularIndeterminateStrokeCap
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `progress` | `() -> Float` | — | **Determinate only**. A lambda returning the current progress between 0.0 and 1.0. Passed as a lambda to prevent unnecessary recompositions. |
| `modifier` | `Modifier` | `Modifier` | Modifier to be applied to the indicator. |
| `color` | `Color` | `ProgressIndicatorDefaults.circularColor` | The color of the active progress arc. |
| `strokeWidth` | `Dp` | `ProgressIndicatorDefaults.CircularStrokeWidth` | The thickness of the progress and track arcs. |
| `trackColor` | `Color` | `ProgressIndicatorDefaults.circularTrackColor` | The color of the track behind the progress arc. |
| `strokeCap` | `StrokeCap` | `StrokeCap.Square` (determinate) / `StrokeCap.Square` (indeterminate) | The stroke cap style for the ends of the progress arc (e.g., `StrokeCap.Round`). |
| `gapSize` | `Dp` | `ProgressIndicatorDefaults.CircularIndicatorTrackGapSize` | **Determinate only** (Added in M3 1.3.0). The size of the gap between the filled progress arc and the empty track. |

> [!TIP]
> The `progress` parameter is a lambda `() -> Float`. This is a performance optimization in Compose that allows the indicator to update its drawing phase without triggering a full recomposition of the parent composable!

## ✅ Basic Example

```kotlin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BasicLoadingIndicator() {
    // Indeterminate spinning circle
    CircularProgressIndicator()
}
```

## 🚀 Advanced Examples

### 1. Determinate Progress Circle (File Download)
```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DownloadProgress() {
    var progress by remember { mutableFloatStateOf(0.1f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            progress = { progress },
            gapSize = 2.dp // Noticeable gap between progress and track
        )
        
        Button(onClick = { if (progress < 1f) progress += 0.1f }) {
            Text("Increase Progress")
        }
    }
}
```

### 2. Custom Color and Stroke
```kotlin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun StyledProgressIndicator() {
    CircularProgressIndicator(
        color = MaterialTheme.colorScheme.tertiary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeWidth = 6.dp,
        strokeCap = StrokeCap.Round
    )
}
```

### 3. Animated Progress from 0 to 1
```kotlin
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment

@Composable
fun AnimatedCircularProgress() {
    var isLoaded by remember { mutableStateOf(false) }
    
    val animatedProgress by animateFloatAsState(
        targetValue = if (isLoaded) 1f else 0f,
        animationSpec = tween(durationMillis = 1500),
        label = "progress_animation"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            progress = { animatedProgress }
        )
        
        Button(onClick = { isLoaded = !isLoaded }) {
            Text(if (isLoaded) "Reset" else "Load")
        }
    }
}
```

### 4. CircularProgressIndicator inside a Button
```kotlin
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingButton() {
    var isLoading by remember { mutableStateOf(false) }

    Button(
        onClick = { isLoading = true },
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text("Submit")
        }
    }
}
```

## ⚠️ Common Gotchas
- **Passing a static value to progress:** Prior to newer Compose versions, `progress` was a `Float`. Now it's a lambda `() -> Float`. If you have a state variable `progress`, pass it as `{ progress }` rather than just `progress` to avoid heavy recompositions.
- **Sizing:** `CircularProgressIndicator` uses a default size. To resize it, apply `Modifier.size(...)`. Don't try to change the size using padding or constraints if you want a precise diameter.
- **Color contrast:** Ensure your `trackColor` and `color` have enough contrast, especially in dark mode.

## 💡 Interview Q&A

**Q: Why does the determinate `CircularProgressIndicator` take a lambda for `progress` instead of a primitive `Float`?**
A: Performance. By taking a lambda `() -> Float`, the composable defers reading the progress state until the draw phase. As the state updates (e.g., during an animation), only the draw phase is executed, bypassing the composition and layout phases. This prevents unnecessary recomposition of the parent composable and ensures smooth 60fps animations.

**Q: How do you change the size of the CircularProgressIndicator?**
A: Use `Modifier.size(Dp)` on the `modifier` parameter.

**Q: What is `gapSize`?**
A: Added in Material3 1.3.0, it specifies the visual gap between the filled progress arc and the empty track arc in the determinate version, making it look cleaner and more aligned with the latest Material specifications.
