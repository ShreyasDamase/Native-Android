# WavyProgressIndicator

## 📌 Purpose
`WavyProgressIndicator` is part of the Material 3 Expressive APIs (`@ExperimentalMaterial3ExpressiveApi`). It provides a highly stylized, whimsical alternative to traditional progress bars by rendering progress as a sine-like wave. 

As the progress increases, the wave's amplitude increases, visually communicating the intensity or nearing completion of a task. It's available in both linear (`LinearWavyProgressIndicator`) and circular (`CircularWavyProgressIndicator`) forms.

> [!IMPORTANT]
> This component was added in **Material3 1.4.0** and requires opting into `@ExperimentalMaterial3ExpressiveApi`.

## 🔧 Function Signatures

### LinearWavyProgressIndicator (Determinate)
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LinearWavyProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    stroke: Stroke = ProgressIndicatorDefaults.WavyStroke,
    trackStroke: Stroke = ProgressIndicatorDefaults.WavyTrackStroke,
    gapSize: Dp = ProgressIndicatorDefaults.LinearIndicatorTrackGapSize,
    stopSize: Dp = ProgressIndicatorDefaults.LinearTrackStopIndicatorSize,
    amplitude: (progress: Float) -> Float = WavyProgressIndicatorDefaults.indicatorAmplitude,
    wavelength: Dp = WavyProgressIndicatorDefaults.LinearWavelength,
    waveSpeed: Dp = WavyProgressIndicatorDefaults.LinearWaveSpeed
)
```

### CircularWavyProgressIndicator (Determinate)
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CircularWavyProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
    trackColor: Color = ProgressIndicatorDefaults.circularTrackColor,
    stroke: Stroke = ProgressIndicatorDefaults.WavyStroke,
    trackStroke: Stroke = ProgressIndicatorDefaults.WavyTrackStroke,
    gapSize: Dp = ProgressIndicatorDefaults.CircularIndicatorTrackGapSize,
    amplitude: (progress: Float) -> Float = WavyProgressIndicatorDefaults.indicatorAmplitude,
    wavelength: Dp = WavyProgressIndicatorDefaults.CircularWavelength,
    waveSpeed: Dp = WavyProgressIndicatorDefaults.CircularWaveSpeed
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `progress` | `() -> Float` | — | A lambda returning progress between 0.0 and 1.0. |
| `modifier` | `Modifier` | `Modifier` | Modifier for the indicator. |
| `color` | `Color` | `ProgressIndicatorDefaults.*Color` | Color of the active wavy progress. |
| `trackColor` | `Color` | `ProgressIndicatorDefaults.*TrackColor` | Color of the track underneath. |
| `stroke` | `Stroke` | `ProgressIndicatorDefaults.WavyStroke` | Allows customizing brush, width, cap, join, and pathEffect for the active wave. |
| `trackStroke` | `Stroke` | `ProgressIndicatorDefaults.WavyTrackStroke` | Stroke configuration for the track. |
| `gapSize` | `Dp` | `...GapSize` | Spacing between the end of the wave and the rest of the track. |
| `stopSize` | `Dp` | `...StopIndicatorSize` | (Linear only) Size of the stop indicator at 100%. |
| `amplitude` | `(Float) -> Float` | `WavyProgressIndicatorDefaults.indicatorAmplitude` | A function that dictates the height of the wave based on current progress. |
| `wavelength` | `Dp` | `...Wavelength` | Horizontal distance between the peaks of the wave. |
| `waveSpeed` | `Dp` | `...WaveSpeed` | The speed at which the wave animates. |

## ✅ Basic Example

```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BasicWavyProgress() {
    LinearWavyProgressIndicator(
        progress = { 0.4f },
        modifier = Modifier.fillMaxWidth()
    )
}
```

## 🚀 Advanced Examples

### 1. Media Upload with Wavy Progress
```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun MediaUploadWavy() {
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1500)
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Uploading Video...")
        Spacer(Modifier.height(16.dp))
        
        LinearWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            // Custom stroke definition
            stroke = Stroke(width = 8f, cap = StrokeCap.Round),
            wavelength = 24.dp,
            amplitude = { p -> p * 15f } // Wave gets very tall as progress nears 1f!
        )
        
        Spacer(Modifier.height(16.dp))
        Button(onClick = { progress = (progress + 0.2f).coerceAtMost(1f) }) {
            Text("Simulate Progress")
        }
    }
}
```

### 2. Comparison: Regular vs Wavy
```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProgressComparison() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Standard Linear Progress:")
        LinearProgressIndicator(
            progress = { 0.6f },
            modifier = Modifier.fillMaxWidth()
        )
        
        Text("Wavy Linear Progress:")
        LinearWavyProgressIndicator(
            progress = { 0.6f },
            modifier = Modifier.fillMaxWidth()
        )
        
        Text("Circular Wavy Progress:")
        CircularWavyProgressIndicator(
            progress = { 0.6f }
        )
    }
}
```

## ⚠️ Common Gotchas
- **Requires Opt-In:** Because it's part of the M3 Expressive API, you must annotate your composable or file with `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`.
- **Vertical Spacing:** A `LinearWavyProgressIndicator` can grow in height vertically due to its `amplitude`. Make sure you give it enough vertical spacing/padding so it doesn't clip into elements above or below it as the wave grows.
- **Stroke Parameters:** It takes a `Stroke` object rather than just a `strokeWidth` Dp. This allows complex definitions (like dashing via `pathEffect`), but requires you to convert Dp to Px or use float values for stroke thickness.

## 💡 Interview Q&A

**Q: How does `amplitude: (progress: Float) -> Float` work?**
A: It maps the current progress (0.0 to 1.0) to a wave amplitude value. By default (`WavyProgressIndicatorDefaults.indicatorAmplitude`), the wave is almost flat when progress is 0, and becomes a full-height sine wave as progress approaches 1.0. This adds a psychological sense of momentum and completion.

**Q: What is `wavelength` and `waveSpeed`?**
A: `wavelength` determines the horizontal distance between two peaks of the wave. A smaller wavelength means a tighter, more squiggly line. `waveSpeed` defines how fast the wave appears to travel horizontally along the path.
