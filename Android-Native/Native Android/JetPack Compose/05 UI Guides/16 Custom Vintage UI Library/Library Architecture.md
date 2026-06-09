# 🏗️ Library Architecture

## 📌 Purpose
When building a custom component library, you shouldn't just dump all composables into a single app module. It needs to be a dedicated Android Library module. This file covers how to structure a Jetpack Compose library, including the Gradle setup, package structure, and custom theming system (bypassing Material Theme).

## 🔧 Android Library Module Setup

To create a standalone library, you need an Android Library module.

```kotlin
// build.gradle.kts (library module)
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    `maven-publish` // Required later for publishing
}

android {
    namespace = "com.yourname.retroui"
    compileSdk = 35
    
    defaultConfig {
        minSdk = 26 // Canvas APIs and hardware acceleration work best 26+
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    // Notice: NO material3 dependency! We build our own visuals.
}
```

## 📂 File & Package Structure

A clean package structure makes your library easy to maintain.

```text
retroui/
├── src/main/kotlin/com/yourname/retroui/
│   ├── theme/
│   │   ├── VintageTheme.kt         ← CompositionLocal tokens
│   │   ├── VintageColors.kt
│   │   ├── VintageDimensions.kt
│   │   └── VintageTypography.kt
│   ├── components/
│   │   ├── VintageButton.kt
│   │   ├── VintageKnob.kt
│   │   ├── VintageMeter.kt
│   │   ├── VintageToggle.kt
│   │   ├── VintageSlider.kt
│   │   └── VintageLED.kt
│   ├── utils/
│   │   ├── GradientUtils.kt        ← reusable gradient brushes
│   │   ├── MathUtils.kt            ← polar coordinate helpers
│   │   └── PathUtils.kt            ← reusable path shapes
│   └── RetroUI.kt                  ← public API entry point
```

## 🎨 The VintageTheme System

Since we aren't using Material, we need our own `CompositionLocal` provider for theming.

```kotlin
// VintageColors.kt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class VintageColors(
    val metalHighlight: Color = Color(0xFFE8E8E8),
    val metalMid: Color = Color(0xFFA0A0A0),
    val metalShadow: Color = Color(0xFF404040),
    val woodWarm: Color = Color(0xFF8B5E3C),
    val woodDark: Color = Color(0xFF5C3D1E),
    val ledAmber: Color = Color(0xFFFFBF00),
    val ledGreen: Color = Color(0xFF39FF14),
    val ledRed: Color = Color(0xFFFF2200),
    val gaugeBackground: Color = Color(0xFFF5F0E0),
    val gaugeText: Color = Color(0xFF1A1A1A)
)

val LocalVintageColors = compositionLocalOf { VintageColors() }

object VintageTheme {
    val colors: VintageColors
        @Composable get() = LocalVintageColors.current
}

@Composable
fun VintageTheme(
    colors: VintageColors = VintageColors(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalVintageColors provides colors) {
        content()
    }
}
```

## 🖌️ Reusable Gradient Utilities

Vintage UIs rely heavily on complex gradients to simulate lighting on metallic surfaces. Centralizing these prevents duplication.

```kotlin
// GradientUtils.kt
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object VintageGradients {
    fun metalBevel(size: Size): Brush = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to Color(0xFFE8E8E8),
            0.15f to Color(0xFFD0D0D0),
            0.5f to Color(0xFFA0A0A0),
            0.85f to Color(0xFF707070),
            1.0f to Color(0xFF505050)
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )
    
    fun metalKnob(size: Size): Brush = Brush.radialGradient(
        colorStops = arrayOf(
            0.0f to Color(0xFFFFFFFF),
            0.2f to Color(0xFFD8D8D8),
            0.6f to Color(0xFF888888),
            1.0f to Color(0xFF404040)
        ),
        center = Offset(size.width * 0.35f, size.height * 0.35f), // Off-center highlight
        radius = size.width * 0.7f
    )
}
```

## 📐 MathUtils for Polar Coordinates

Knobs and meters require translating between angles (degrees/radians) and X/Y coordinates.

```kotlin
// MathUtils.kt
import androidx.compose.ui.geometry.Offset
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object VintageMath {
    fun angleToOffset(angleDeg: Float, radius: Float, center: Offset): Offset {
        val rad = Math.toRadians(angleDeg.toDouble())
        return Offset(
            x = center.x + (radius * cos(rad)).toFloat(),
            y = center.y + (radius * sin(rad)).toFloat()
        )
    }
    
    fun offsetToAngle(offset: Offset, center: Offset): Float {
        val dx = offset.x - center.x
        val dy = offset.y - center.y
        return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }
    
    fun valueToAngle(value: Float, min: Float, max: Float, startAngle: Float, endAngle: Float): Float {
        val fraction = (value - min) / (max - min)
        return startAngle + (fraction * (endAngle - startAngle))
    }
}
```

> [!TIP]
> **Why objects for utils?**
> Using `object` singletons for utility functions keeps the namespace clean and makes them easily discoverable via autocomplete (`VintageMath.`).

## ⚠️ Common Gotchas
- **Forgetting `consumerProguardFiles`**: If your library uses internal classes, ProGuard in the consuming app might strip them out. Always provide consumer rules.
- **Depending on Material**: If you want a truly independent library, do not import `androidx.compose.material` or `material3`. Rely strictly on `androidx.compose.foundation`.

## 💡 Interview Q&A
**Q: How does `CompositionLocal` work and why use it for custom themes?**
A: `CompositionLocal` allows data to flow down the composable tree implicitly. It's perfect for themes because you don't want to pass `colors` and `typography` as explicit parameters to every single component. `MaterialTheme` itself is just a wrapper around several `CompositionLocalProvider` calls.
