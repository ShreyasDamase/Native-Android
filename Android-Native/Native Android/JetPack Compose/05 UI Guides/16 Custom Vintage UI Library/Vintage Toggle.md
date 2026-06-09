# 🕹️ VintageToggle

## 📌 Purpose
The `VintageToggle` is a physical flip switch (lever) typically found on power panels, amplifiers, and industrial machinery. Unlike a smooth Android `Switch`, it has a distinct 3D lever that snaps between up (ON) and down (OFF) states with a satisfying bounce.

## 🔧 Function Signature
```kotlin
@Composable
fun VintageToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onColor: Color = Color(0xFFFFBF00),
    size: DpSize = DpSize(30.dp, 60.dp)
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `checked` | `Boolean` | — | Required. Whether the switch is in the ON position. |
| `onCheckedChange` | `(Boolean) -> Unit` | — | Required. Callback when flipped. |
| `modifier` | `Modifier` | `Modifier` | Optional. |
| `onColor` | `Color` | `Color(0xFFFFBF00)` | Optional. Color of the indicator LED inside the slot. |
| `size` | `DpSize` | `30.dp x 60.dp`| Optional. Total bounds of the toggle housing. |

## ✅ Basic Implementation

```kotlin
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Composable
fun VintageToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onColor: Color = Color(0xFFFFBF00),
    size: DpSize = DpSize(30.dp, 60.dp)
) {
    // 0f = ON (top), 1f = OFF (bottom)
    val leverOffset by animateFloatAsState(
        targetValue = if (checked) 0f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "leverSnap"
    )
    
    val interactionSource = remember { MutableInteractionSource() }
    
    Canvas(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = interactionSource,
                indication = null // physical switches don't ripple
            ) { onCheckedChange(!checked) }
    ) {
        val w = this.size.width
        val h = this.size.height
        val padding = 4.dp.toPx()
        val slotWidth = w - padding * 2
        val leverHeight = h * 0.45f
        val travelRange = h - padding * 2 - leverHeight
        
        // Interpolate lever Y position
        val leverY = padding + (leverOffset * travelRange)
        
        // 1. Slot housing (Outer bevel - raised)
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color(0xFF555555), Color(0xFF222222)),
                start = Offset.Zero, end = Offset(0f, h)
            ),
            cornerRadius = CornerRadius(w / 2f)
        )
        
        // 2. Inner slot shadow (Debossed effect)
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color.Black.copy(0.9f), Color(0xFF111111)),
                start = Offset(0f, 0f), end = Offset(0f, h * 0.3f)
            ),
            topLeft = Offset(padding, padding),
            size = Size(slotWidth, h - padding * 2),
            cornerRadius = CornerRadius(slotWidth / 2f)
        )
        
        // 3. Status LED glow at top (visible when ON/lever is up)
        if (checked) {
            val ledCenter = Offset(w / 2f, padding + 8.dp.toPx())
            drawIntoCanvas { canvas ->
                canvas.drawCircle(ledCenter, 6.dp.toPx(),
                    Paint().apply {
                        color = onColor.copy(alpha = 0.9f)
                        asFrameworkPaint().maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.OUTER)
                    }
                )
            }
            // LED Bulb
            drawCircle(onColor, 3.dp.toPx(), ledCenter)
        }
        
        // 4. The Lever (3D metallic pill)
        // Drop shadow from lever into the slot
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.6f),
            topLeft = Offset(padding + 2f, leverY + 4f),
            size = Size(slotWidth - 4f, leverHeight),
            cornerRadius = CornerRadius((slotWidth - 4f) / 2f)
        )
        
        // Lever body
        drawRoundRect(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0f to Color(0xFFE0E0E0),
                    0.2f to Color(0xFFFFFFFF),
                    0.5f to Color(0xFFB0B0B0),
                    1f to Color(0xFF606060)
                ),
                start = Offset(0f, leverY),
                end = Offset(0f, leverY + leverHeight)
            ),
            topLeft = Offset(padding + 2f, leverY),
            size = Size(slotWidth - 4f, leverHeight),
            cornerRadius = CornerRadius((slotWidth - 4f) / 2f)
        )
        
        // Horizontal grip lines on the lever
        val gripSpacing = leverHeight / 5
        for(i in 1..4) {
            val lineY = leverY + (gripSpacing * i)
            drawLine(
                color = Color.Black.copy(alpha = 0.3f),
                start = Offset(padding + 6f, lineY),
                end = Offset(w - padding - 6f, lineY),
                strokeWidth = 2f
            )
            // Highlight right below the dark line
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(padding + 6f, lineY + 2f),
                end = Offset(w - padding - 6f, lineY + 2f),
                strokeWidth = 2f
            )
        }
    }
}
```

## 🚀 Advanced Examples

### Power Switch with Labels
Combining the toggle with text to mimic standard hardware panels.

```kotlin
@Composable
fun MainPowerSwitch() {
    var powerOn by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
            Text("ON", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            Text("OFF", color = Color.Gray, fontSize = 12.sp)
        }
        VintageToggle(
            checked = powerOn,
            onCheckedChange = { powerOn = it },
            onColor = Color.Red
        )
    }
}
```

## ⚠️ Common Gotchas
- **Grip Lines Rendering:** When drawing small details like 2px grip lines over gradients, be careful with aliasing. Drawing a dark line followed immediately by a light line 1px below it creates a perfect 3D indentation illusion (emboss effect).
- **Hit box size:** Physical switches are often quite small (`30x60dp`). Ensure you are using `Modifier.minimumInteractiveComponentSize()` if you intend to use this in a high-accessibility environment, or wrap it in a larger Box.
