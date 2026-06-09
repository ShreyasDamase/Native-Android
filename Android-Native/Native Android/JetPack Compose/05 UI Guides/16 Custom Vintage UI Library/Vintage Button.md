# 🎛️ VintageButton

## 📌 Purpose
The `VintageButton` represents a physical, mechanical button inspired by 1980s cassette decks and analog synthesizers. It uses 3D gradients and drops a shadow to look raised. Instead of a Material ripple, it physically depresses (translates downward) and inverts its lighting to simulate being pushed into a panel.

## 🔧 Function Signature
```kotlin
@Composable
fun VintageButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    enabled: Boolean = true,
    buttonColor: Color = Color(0xFF8B8B8B),
    width: Dp = 80.dp,
    height: Dp = 40.dp
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `onClick` | `() -> Unit` | — | Required. Callback invoked when the button is clicked. |
| `modifier` | `Modifier` | `Modifier` | Optional. Standard Compose modifier. |
| `label` | `String` | — | Required. The text displayed on the button. |
| `enabled` | `Boolean` | `true` | Optional. Whether the button is clickable. |
| `buttonColor` | `Color` | `Color(0xFF8B8B8B)` | Optional. Base color (mixes with the metallic sheen). |
| `width` | `Dp` | `80.dp` | Optional. Total width of the button. |
| `height` | `Dp` | `40.dp` | Optional. Total height of the button. |

## ✅ Basic Implementation
```kotlin
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VintageButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    enabled: Boolean = true,
    buttonColor: Color = Color(0xFF8B8B8B), // Useful if you want colored plastic buttons
    width: Dp = 80.dp,
    height: Dp = 40.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Simulate physical depression
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "pressOffset"
    )
    
    val textMeasurer = rememberTextMeasurer()
    
    Canvas(
        modifier = modifier
            .size(width, height)
            .offset(y = pressOffset)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disable default Material ripple
                enabled = enabled,
                onClick = onClick
            )
    ) {
        val w = size.width
        val h = size.height
        val cornerRadius = CornerRadius(8.dp.toPx())
        
        // 1. Shadow layer (only visible when NOT pressed)
        if (!isPressed) {
            drawIntoCanvas { canvas ->
                canvas.drawRoundRect(
                    left = 2f, top = 4f, right = w - 2f, bottom = h + 4f,
                    radiusX = cornerRadius.x, radiusY = cornerRadius.y,
                    paint = Paint().apply {
                        color = Color.Black.copy(alpha = 0.5f)
                        asFrameworkPaint().maskFilter =
                            BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
                    }
                )
            }
        }
        
        // 2. Button body — bevel gradient
        val bevelGradient = if (isPressed) {
            // Inverted bevel when pressed (light from bottom-right)
            Brush.linearGradient(
                colorStops = arrayOf(
                    0f to Color(0xFF505050),
                    0.5f to Color(0xFF909090),
                    1f to Color(0xFFD0D0D0)
                ),
                start = Offset.Zero,
                end = Offset(w, h)
            )
        } else {
            // Normal bevel (light from top-left)
            Brush.linearGradient(
                colorStops = arrayOf(
                    0f to Color(0xFFE0E0E0),
                    0.5f to Color(0xFFA0A0A0),
                    1f to Color(0xFF606060)
                ),
                start = Offset.Zero,
                end = Offset(w, h)
            )
        }
        
        drawRoundRect(brush = bevelGradient, cornerRadius = cornerRadius, size = size)
        
        // 3. Metallic sheen — Screen blend for extra realism
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.3f), Color.Transparent, Color.Black.copy(alpha = 0.1f)),
                start = Offset(0f, 0f), end = Offset(0f, h)
            ),
            cornerRadius = cornerRadius,
            size = size,
            blendMode = BlendMode.Screen
        )
        
        // 4. Label text (centered)
        val textLayoutResult = textMeasurer.measure(
            text = label, 
            style = TextStyle(fontSize = 12.sp, color = Color(0xFF1A1A1A))
        )
        
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                x = (w - textLayoutResult.size.width) / 2f,
                y = (h - textLayoutResult.size.height) / 2f
            )
        )
    }
}
```

## 🚀 Advanced Examples

### Transport Controls Panel
Grouping vintage buttons to create a media playback transport panel.

```kotlin
@Composable
fun TransportControls() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VintageButton(onClick = {}, label = "REW", width = 60.dp)
        VintageButton(onClick = {}, label = "PLAY", width = 80.dp, buttonColor = Color(0xFF4CAF50))
        VintageButton(onClick = {}, label = "FFWD", width = 60.dp)
        VintageButton(onClick = {}, label = "STOP", width = 60.dp)
    }
}
```

## ⚠️ Common Gotchas
- **Ripple effects:** If you forget to pass `indication = null` to `.clickable`, you'll get a Material ripple on top of your vintage 3D component, ruining the illusion.
- **State reads in draw phase:** Use `textMeasurer` properly. Don't create `TextStyle` or `Paint` objects inside the `Canvas` block endlessly; define them outside or let Compose optimize them.
- **Hardware Acceleration:** `BlurMaskFilter` requires hardware acceleration. If it's turned off (rare on modern devices, but possible in some window settings), shadows will render as solid boxes.

## 💡 Interview Q&A
**Q: Why use `MutableInteractionSource` instead of standard `clickable` state?**
A: `MutableInteractionSource` allows us to observe interaction events like `PressInteraction.Press`. Standard `clickable` just provides an `onClick` callback, which doesn't tell us *when* the button is actively being held down to trigger our 3D translation and lighting inversion.
