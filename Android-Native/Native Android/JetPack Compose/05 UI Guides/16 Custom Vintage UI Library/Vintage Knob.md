# 🎛️ VintageKnob

## 📌 Purpose
The `VintageKnob` represents a rotary dial, heavily used in analog audio equipment (like a guitar amplifier or mixing console). It translates drag gestures into rotary motion, features 3D radial gradients, and displays precise tick marks around its perimeter.

## 🔧 Function Signature
```kotlin
@Composable
fun VintageKnob(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    startAngle: Float = -135f,
    endAngle: Float = 135f,
    size: Dp = 80.dp,
    label: String? = null,
    showTicks: Boolean = true,
    tickCount: Int = 11,
    arcColor: Color = Color(0xFFFFBF00)
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `value` | `Float` | — | Required. Current value of the knob. |
| `onValueChange` | `(Float) -> Unit` | — | Required. Callback when knob is turned. |
| `modifier` | `Modifier` | `Modifier` | Optional. Compose modifier. |
| `valueRange` | `ClosedFloatingPointRange<Float>` | `0f..1f` | Optional. Min and max values. |
| `startAngle` | `Float` | `-135f` | Optional. Angle for min value (0 is right, -90 is top). |
| `endAngle` | `Float` | `135f` | Optional. Angle for max value. |
| `size` | `Dp` | `80.dp` | Optional. Width and height of the knob. |
| `label` | `String?` | `null` | Optional. Text displayed below the knob. |
| `showTicks` | `Boolean` | `true` | Optional. Whether to draw outer tick marks. |
| `tickCount` | `Int` | `11` | Optional. Number of tick marks. |
| `arcColor` | `Color` | `Color(0xFFFFBF00)` | Optional. Color of the active sweep arc. |

## ✅ Basic Implementation
*Requires `VintageMath` from [[01-Library-Architecture]].*

```kotlin
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.retroui.utils.VintageMath // Ensure this is implemented

@Composable
fun VintageKnob(
    value: Float,                           
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    startAngle: Float = -135f,              
    endAngle: Float = 135f,                 
    size: Dp = 80.dp,
    label: String? = null,
    showTicks: Boolean = true,
    tickCount: Int = 11,
    arcColor: Color = Color(0xFFFFBF00)     
) {
    // Map value to angle
    val targetAngle = remember(value, valueRange, startAngle, endAngle) {
        VintageMath.valueToAngle(
            value, valueRange.start, valueRange.endInclusive, startAngle, endAngle
        )
    }
    
    val animatedAngle by animateFloatAsState(targetAngle, spring(), label = "knobAngle")
    
    var dragStartAngle by remember { mutableFloatStateOf(0f) }
    var dragStartValue by remember { mutableFloatStateOf(value) }
    
    val textMeasurer = rememberTextMeasurer()
    
    Canvas(
        modifier = modifier
            .size(size)
            .pointerInput(valueRange, startAngle, endAngle) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStartAngle = VintageMath.offsetToAngle(offset, Offset(size.toPx()/2f, size.toPx()/2f))
                        dragStartValue = value
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val currentAngle = VintageMath.offsetToAngle(change.position, Offset(this.size.width/2f, this.size.height/2f))
                        var angleDelta = currentAngle - dragStartAngle
                        
                        // Handle crossing the 180/-180 degree boundary (atan2 wrapping)
                        if (angleDelta > 180f) angleDelta -= 360f
                        if (angleDelta < -180f) angleDelta += 360f
                        
                        val totalRange = endAngle - startAngle
                        val valueDelta = (angleDelta / totalRange) * (valueRange.endInclusive - valueRange.start)
                        val newValue = (dragStartValue + valueDelta).coerceIn(valueRange.start, valueRange.endInclusive)
                        onValueChange(newValue)
                    }
                )
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.width / 2f
        val knobRadius = outerRadius * 0.75f
        val tickInnerRadius = outerRadius * 0.82f
        val tickOuterRadius = outerRadius * 0.95f
        
        // 1. Value sweep arc (lights up as you turn)
        val sweepAngle = animatedAngle - startAngle
        drawArc(
            color = arcColor,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
            size = Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // 2. Tick marks around the perimeter
        if (showTicks) {
            for (i in 0 until tickCount) {
                val fraction = i.toFloat() / (tickCount - 1)
                val tickAngle = startAngle + (fraction * (endAngle - startAngle))
                val isMajor = i == 0 || i == tickCount - 1 || i == tickCount / 2
                val innerR = if (isMajor) tickInnerRadius * 0.92f else tickInnerRadius
                val outerR = tickOuterRadius
                
                val isActive = fraction <= (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
                val tickColor = if (isActive) arcColor else Color.Gray
                
                val startPt = VintageMath.angleToOffset(tickAngle, innerR, center)
                val endPt = VintageMath.angleToOffset(tickAngle, outerR, center)
                drawLine(tickColor, startPt, endPt, if (isMajor) 2.5f else 1.5f, StrokeCap.Round)
            }
        }
        
        // 3. Outer ring shadow (drop shadow under the knob)
        drawIntoCanvas { canvas ->
            canvas.drawCircle(center, knobRadius + 4f,
                Paint().apply {
                    color = Color.Black.copy(alpha = 0.6f)
                    asFrameworkPaint().maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
                }
            )
        }
        
        // 4. Outer dark base ring
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF555555), Color(0xFF222222)),
                center = center, radius = knobRadius + 4f
            ),
            radius = knobRadius,
            center = center
        )
        
        // 5. Inner dome — metallic radial gradient with off-center light source
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color(0xFFFFFFFF),
                    0.2f to Color(0xFFDDDDDD),
                    0.6f to Color(0xFF888888),
                    1f to Color(0xFF3A3A3A)
                ),
                center = Offset(center.x - knobRadius * 0.25f, center.y - knobRadius * 0.25f),
                radius = knobRadius
            ),
            radius = knobRadius * 0.9f,
            center = center
        )
        
        // 6. Indicator line & dot
        rotate(animatedAngle, center) {
            // Line pointing outward
            drawLine(
                color = Color(0xFF1A1A1A),
                start = center,
                end = Offset(center.x, center.y - knobRadius * 0.65f), // Top is -Y in Compose
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
            // Glowing dot at the end
            drawCircle(Color(0xFFFFBF00), 3.dp.toPx(), Offset(center.x, center.y - knobRadius * 0.65f))
        }
        
        // 7. Label text
        if (label != null) {
            val textLayout = textMeasurer.measure(label, TextStyle(fontSize = 10.sp, color = Color(0xFFCCCCCC)))
            drawText(
                textLayout, 
                topLeft = Offset(
                    x = center.x - textLayout.size.width / 2f,
                    y = center.y + knobRadius + 8.dp.toPx()
                )
            )
        }
    }
}
```

## 🚀 Advanced Examples

### Mixing Board (Row of Knobs)
```kotlin
@Composable
fun EQPanel() {
    var bass by remember { mutableFloatStateOf(0.5f) }
    var mid by remember { mutableFloatStateOf(0.5f) }
    var treble by remember { mutableFloatStateOf(0.5f) }

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        VintageKnob(value = bass, onValueChange = { bass = it }, label = "LOW")
        VintageKnob(value = mid, onValueChange = { mid = it }, label = "MID")
        VintageKnob(value = treble, onValueChange = { treble = it }, label = "HIGH")
    }
}
```

## ⚠️ Common Gotchas
- **Math angle wrapping:** `atan2` returns values between `-180` and `180`. When dragging across the left side of the circle, the delta will suddenly flip (e.g., from `179` to `-179`). The delta compensation `if (angleDelta > 180f)` logic is crucial to prevent the knob from spinning wildly.
- **Compose Coordinate System:** In Canvas, `0` degrees is directly to the **Right**, and `90` degrees is **Down**. But standard UI often thinks of `0` as top. `VintageMath` correctly handles standard trigonometry, but visual design must accommodate Compose's downward Y-axis.

## 💡 Interview Q&A
**Q: How do you make a circular gesture feel natural to the user?**
A: Instead of tracking X/Y distance, we calculate the angle of the user's touch point relative to the center of the knob using `atan2`. As they move their finger, we calculate the delta angle and apply that proportionally to the value range.
