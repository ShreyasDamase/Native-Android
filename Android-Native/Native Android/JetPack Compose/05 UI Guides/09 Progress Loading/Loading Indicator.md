# LoadingIndicator & ContainedLoadingIndicator

## 📌 Purpose
`LoadingIndicator` is a new component in the Material 3 Expressive APIs (`@ExperimentalMaterial3ExpressiveApi`). It acts as a modern replacement for the traditional circular spinner. 

Instead of a spinning arc, it uses **morphing polygons**. The shape continuously morphs (e.g., from a circle to a flower shape, to a star) to indicate indeterminate loading. The `ContainedLoadingIndicator` provides a determinate version housed within a colored container, often morphing its shape to reflect progress.

> [!NOTE]
> Added in Material3 1.4.0. Requires the graphics-shapes library to define `RoundedPolygon` instances.

## 🔧 Function Signatures

### Indeterminate LoadingIndicator
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    polygons: List<RoundedPolygon> = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons
)
```

### Determinate ContainedLoadingIndicator
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContainedLoadingIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    containerShape: Shape = LoadingIndicatorDefaults.ContainerShape,
    polygons: List<RoundedPolygon> = LoadingIndicatorDefaults.DeterminateIndicatorPolygons
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `progress` | `() -> Float` | — | **Contained only**. Lambda returning progress from 0f to 1f. |
| `modifier` | `Modifier` | `Modifier` | Component modifier. |
| `color` / `indicatorColor` | `Color` | `colorScheme.primary` | Color of the morphing polygon. |
| `containerColor` | `Color` | `colorScheme.surfaceVariant` | **Contained only**. Color of the background container. |
| `containerShape` | `Shape` | `LoadingIndicatorDefaults.ContainerShape` | **Contained only**. Shape of the container. |
| `polygons` | `List<RoundedPolygon>` | `LoadingIndicatorDefaults.*Polygons` | A list of `RoundedPolygon` shapes that the indicator will smoothly morph between. |

## 📐 What is RoundedPolygon?
`RoundedPolygon` comes from the `androidx.graphics:graphics-shapes` library. It allows you to create complex polygons (like stars, clovers, squicles) with rounded corners. `LoadingIndicator` takes a list of these and uses `Morph` animations to transition between them seamlessly.

## ✅ Basic Example

```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable

@Composable
fun AppLoadingSpinner() {
    // A morphing shape indicator
    LoadingIndicator()
}
```

## 🚀 Advanced Examples

### 1. App Launch Loading Screen
```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LaunchScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LoadingIndicator(
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("Loading Expressive UI...", style = MaterialTheme.typography.titleLarge)
        }
    }
}
```

### 2. ContainedLoadingIndicator in a Card
```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DownloadCard() {
    var progress by remember { mutableFloatStateOf(0.2f) }

    Card(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ContainedLoadingIndicator(
                progress = { progress },
                indicatorColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
            
            Column {
                Text("Downloading Assets")
                Text("${(progress * 100).toInt()}% complete")
            }
        }
        
        Button(
            onClick = { progress = (progress + 0.1f).coerceAtMost(1f) },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Step Progress")
        }
    }
}
```

## ⚠️ Common Gotchas
- **Missing Dependency:** To customize shapes, you must include `implementation("androidx.graphics:graphics-shapes:1.0.1")` (or latest) in your Gradle file.
- **Sizing:** `LoadingIndicator` relies heavily on internal Canvas drawing bounds. Ensure you assign it an explicit size via `Modifier.size()` if you deviate from the default.
- **Expressive API Opt-In:** Will cause compile errors if `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` is forgotten.

## 💡 Interview Q&A

**Q: How does `LoadingIndicator` differ from `[[CircularProgressIndicator]]`?**
A: `CircularProgressIndicator` rotates a stroked arc to convey loading. `LoadingIndicator` fills a solid geometric shape (a `RoundedPolygon`) and continuously morphs its vertices to transition into other shapes, providing a modern, playful, Material You expressive aesthetic.

**Q: Can I create my own custom morphing sequence?**
A: Yes! You can provide a custom `List<RoundedPolygon>` to the `polygons` parameter. The indicator will automatically handle the interpolation (morphing) between shape N and shape N+1 in the list, looping back to 0.
