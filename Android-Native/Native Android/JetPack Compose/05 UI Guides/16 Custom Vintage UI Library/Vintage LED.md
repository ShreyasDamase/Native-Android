# 🚨 VintageLED

## 📌 Purpose
The `VintageLED` represents a physical glass diode indicator. Unlike a simple colored circle, a physical LED has a plastic/glass dome that refracts light, an unlit baseline color, a specular highlight reflecting room lighting, and an outer glow when powered on.

## 🔧 Function Signature
```kotlin
@Composable
fun VintageLED(
    isOn: Boolean,
    modifier: Modifier = Modifier,
    color: LEDColor = LEDColor.AMBER,
    size: Dp = 16.dp,
    blink: Boolean = false,
    blinkDurationMs: Int = 800
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `isOn` | `Boolean` | — | Required. Whether the LED is currently powered on. |
| `modifier` | `Modifier` | `Modifier` | Optional. |
| `color` | `LEDColor` | `AMBER` | Optional. Enum containing lit and dim state colors. |
| `size` | `Dp` | `16.dp` | Optional. Diameter of the LED. |
| `blink` | `Boolean` | `false` | Optional. If true and `isOn` is true, the LED will flash. |
| `blinkDurationMs`| `Int` | `800` | Optional. Speed of the flash cycle. |

## ✅ Basic Implementation

First, we define an Enum to encapsulate the "ON" vs "OFF" colors cleanly:

```kotlin
import androidx.compose.ui.graphics.Color

enum class LEDColor(val lit: Color, val dim: Color) {
    AMBER(Color(0xFFFFBF00), Color(0xFF4A3800)),
    GREEN(Color(0xFF39FF14), Color(0xFF0A3000)),
    RED(Color(0xFFFF2200), Color(0xFF3A0000)),
    BLUE(Color(0xFF00BFFF), Color(0xFF001A3A))
}
```

Then, the implementation:

```kotlin
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun VintageLED(
    isOn: Boolean,
    modifier: Modifier = Modifier,
    color: LEDColor = LEDColor.AMBER,
    size: Dp = 16.dp,
    blink: Boolean = false,
    blinkDurationMs: Int = 800
) {
    // Handle blinking logic via InfiniteTransition
    val infiniteTransition = rememberInfiniteTransition(label = "blinkTransition")
    
    val blinkAlpha by if (blink && isOn) {
        infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 0.1f,
            animationSpec = infiniteRepeatable(
                tween(blinkDurationMs, easing = LinearEasing),
                RepeatMode.Reverse
            ),
            label = "blinkAlpha"
        )
    } else {
        // If not blinking, just return static values based on isOn state
        androidx.compose.runtime.remember(isOn) { 
            androidx.compose.runtime.mutableFloatStateOf(if (isOn) 1f else 0.15f) 
        }
    }
    
    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.width / 2f
        val center = Offset(radius, radius)
        val litColor = color.lit
        val currentColor = if (isOn) litColor.copy(alpha = blinkAlpha) else color.dim
        
        // 1. Glow effect (BlurMaskFilter.Blur.OUTER paints ONLY outside the bounds)
        if (isOn) {
            drawIntoCanvas { canvas ->
                canvas.drawCircle(center, radius,
                    Paint().apply {
                        this.color = litColor.copy(alpha = 0.7f * blinkAlpha)
                        asFrameworkPaint().maskFilter = BlurMaskFilter(radius * 1.5f, BlurMaskFilter.Blur.OUTER)
                    }
                )
            }
        }
        
        // 2. Outer bezel (black ring holding the LED)
        drawCircle(color = Color(0xFF1A1A1A), radius = radius, center = center)
        
        // 3. LED Dome (Radial gradient simulating a 3D sphere)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to if (isOn) Color.White.copy(alpha = 0.9f * blinkAlpha) else Color(0xFF555555),
                    0.3f to currentColor.copy(alpha = 0.9f),
                    0.8f to currentColor.copy(alpha = 0.5f),
                    1f to color.dim
                ),
                // Shift gradient center up and left for top-down lighting
                center = Offset(center.x - radius * 0.2f, center.y - radius * 0.2f),
                radius = radius * 0.9f
            ),
            radius = radius * 0.85f,
            center = center
        )
        
        // 4. Specular highlight (Glass reflection)
        drawOval(
            color = Color.White.copy(alpha = if (isOn) 0.6f * blinkAlpha else 0.3f),
            topLeft = Offset(center.x - radius * 0.35f, center.y - radius * 0.5f),
            size = Size(radius * 0.35f, radius * 0.25f)
        )
    }
}
```

## 🚀 Advanced Examples

### System Status Panel
```kotlin
@Composable
fun ServerStatusPanel() {
    var isConnected by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            VintageLED(isOn = isConnected, color = LEDColor.GREEN)
            Text("LINK", fontSize = 10.sp, color = Color.Gray)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            VintageLED(isOn = hasError, blink = true, color = LEDColor.RED)
            Text("ERR", fontSize = 10.sp, color = Color.Gray)
        }
    }
}
```

## ⚠️ Common Gotchas
- **`BlurMaskFilter` Clipping:** Compose Canvases clip to their bounds by default. If your LED is exactly `16.dp` and your glow extends outside that, the glow will be cut off perfectly flat at the edges. To fix this, either make the overall Canvas larger than the LED radius, or ensure the parent container doesn't enforce strict clipping.
- **Hardware Acceleration:** The glow effect (`BlurMaskFilter`) requires hardware acceleration on Android.

## 💡 Interview Q&A
**Q: How do you animate an infinite blinking effect efficiently in Compose?**
A: By using `rememberInfiniteTransition()`. This creates an animation loop that runs outside of the standard recomposition cycle. We then read the `blinkAlpha` state and apply it directly in the `Canvas` drawing phase, minimizing structural recompositions.
