# 🎚️ VintageMeter

## 📌 Purpose
The `VintageMeter` simulates an analog VU (Volume Unit) or level meter gauge. It draws heavy inspiration from classic scientific instruments like the Western Electric Percent Distortion meter. It uses spring animations to mimic the realistic physical inertia of a metal needle bouncing against its limits.

## 🔧 Function Signature
```kotlin
@Composable
fun VintageMeter(
    value: Float,
    modifier: Modifier = Modifier,
    minValue: Float = -20f,
    maxValue: Float = 3f,
    unit: String = "VU",
    startAngle: Float = -130f,   
    endAngle: Float = -50f,      
    dangerThreshold: Float = 0f, 
    warningThreshold: Float = -6f,
    size: DpSize = DpSize(200.dp, 120.dp)
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `value` | `Float` | — | Required. Current value to display. |
| `modifier` | `Modifier` | `Modifier` | Optional. |
| `minValue` | `Float` | `-20f` | Optional. Minimum bound. |
| `maxValue` | `Float` | `3f` | Optional. Maximum bound. |
| `unit` | `String` | `"VU"` | Optional. Text label inside the gauge. |
| `startAngle` | `Float` | `-130f` | Optional. Needle angle at `minValue`. (0 is right) |
| `endAngle` | `Float` | `-50f` | Optional. Needle angle at `maxValue`. |
| `dangerThreshold` | `Float` | `0f` | Optional. Value where the red zone starts. |
| `warningThreshold` | `Float` | `-6f` | Optional. Value where the yellow zone starts. |
| `size` | `DpSize` | `200.dp x 120.dp`| Optional. Dimensions of the meter. |

## ✅ Basic Implementation
*Requires `VintageMath` from [[01-Library-Architecture]].*

```kotlin
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.retroui.utils.VintageMath

@Composable
fun VintageMeter(
    value: Float,
    modifier: Modifier = Modifier,
    minValue: Float = -20f,
    maxValue: Float = 3f,
    unit: String = "VU",
    startAngle: Float = -130f,   
    endAngle: Float = -50f,      
    dangerThreshold: Float = 0f, 
    warningThreshold: Float = -6f,
    size: DpSize = DpSize(200.dp, 120.dp)
) {
    // We add 90f because 0 is right, but our needle is drawn pointing straight UP
    // so we need to offset the coordinate system rotation.
    val targetAngle = VintageMath.valueToAngle(value, minValue, maxValue, startAngle + 90f, endAngle + 90f)
    
    val needleAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = spring(
            dampingRatio = 0.5f,  // Low damping = slight bounce, like a real physical needle
            stiffness = Spring.StiffnessMedium
        ),
        label = "needleBounce"
    )
    
    val textMeasurer = rememberTextMeasurer()
    
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val pivotX = w / 2f
        val pivotY = h * 0.88f  // pivot near bottom
        val pivot = Offset(pivotX, pivotY)
        val gaugeRadius = minOf(w, h * 1.5f) * 0.75f
        
        // 1. Dark Frame
        val frameGradient = Brush.linearGradient(
            listOf(Color(0xFF555555), Color(0xFF222222)),
            start = Offset.Zero, end = Offset(0f, h)
        )
        drawRoundRect(frameGradient, cornerRadius = CornerRadius(8.dp.toPx()))
        
        // 2. Gauge face (cream inset)
        val faceInset = 8.dp.toPx()
        drawRoundRect(
            color = Color(0xFFF5F0E0),
            topLeft = Offset(faceInset, faceInset),
            size = Size(w - faceInset * 2, h - faceInset * 2),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        
        // Inner shadow on the face
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color.Black.copy(0.3f), Color.Transparent),
                start = Offset(0f, 0f), end = Offset(0f, faceInset * 3)
            ),
            topLeft = Offset(faceInset, faceInset),
            size = Size(w - faceInset * 2, h - faceInset * 2),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        
        // 3. Color zone arcs
        val arcRectTopLeft = Offset(pivotX - gaugeRadius, pivotY - gaugeRadius)
        val arcSize = Size(gaugeRadius * 2, gaugeRadius * 2)
        val arcWidth = 12.dp.toPx()
        
        fun drawZoneArc(startVal: Float, endVal: Float, color: Color) {
            val sAngle = VintageMath.valueToAngle(startVal, minValue, maxValue, startAngle, endAngle)
            val eAngle = VintageMath.valueToAngle(endVal, minValue, maxValue, startAngle, endAngle)
            drawArc(
                color = color.copy(alpha = 0.4f),
                startAngle = sAngle,
                sweepAngle = eAngle - sAngle,
                useCenter = false,
                topLeft = arcRectTopLeft,
                size = arcSize,
                style = Stroke(width = arcWidth)
            )
        }
        
        drawZoneArc(minValue, warningThreshold, Color(0xFF4CAF50)) // Safe (Green)
        drawZoneArc(warningThreshold, dangerThreshold, Color(0xFFFFEB3B)) // Warning (Yellow)
        drawZoneArc(dangerThreshold, maxValue, Color(0xFFF44336)) // Danger (Red)
        
        // 4. Tick marks and Labels
        val majorTicks = listOf(minValue, -15f, -10f, -7f, -5f, -3f, 0f, maxValue)
        
        for (v in (minValue.toInt()..maxValue.toInt())) {
            val tickVal = v.toFloat()
            val tAngle = VintageMath.valueToAngle(tickVal, minValue, maxValue, startAngle, endAngle)
            val isMajor = majorTicks.contains(tickVal)
            
            val innerR = if (isMajor) gaugeRadius - 16.dp.toPx() else gaugeRadius - 8.dp.toPx()
            val outerR = gaugeRadius + 2.dp.toPx()
            
            val startPt = VintageMath.angleToOffset(tAngle, innerR, pivot)
            val endPt = VintageMath.angleToOffset(tAngle, outerR, pivot)
            
            val color = if (tickVal >= dangerThreshold) Color.Red else Color.Black
            drawLine(color, startPt, endPt, strokeWidth = if(isMajor) 3f else 1.5f)
            
            // Labels for major ticks
            if (isMajor) {
                val labelText = tickVal.toInt().toString()
                val textLayout = textMeasurer.measure(
                    text = labelText, 
                    style = TextStyle(fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
                )
                // Offset label slightly inside the arc
                val labelPt = VintageMath.angleToOffset(tAngle, innerR - 12.dp.toPx(), pivot)
                drawText(
                    textLayout,
                    topLeft = Offset(labelPt.x - textLayout.size.width / 2f, labelPt.y - textLayout.size.height / 2f)
                )
            }
        }
        
        // Draw Unit Label
        val unitLayout = textMeasurer.measure(unit, TextStyle(fontSize = 14.sp, color = Color.Black.copy(0.6f)))
        drawText(unitLayout, topLeft = Offset(pivotX - unitLayout.size.width / 2f, pivotY - gaugeRadius * 0.4f))
        
        // 5. Needle
        rotate(needleAngle, pivot) {
            val needlePath = Path().apply {
                moveTo(pivotX, pivotY)                          // base center
                lineTo(pivotX - 3f, pivotY - gaugeRadius * 0.1f) // flare left
                lineTo(pivotX, pivotY - gaugeRadius * 0.95f)    // needle tip
                lineTo(pivotX + 3f, pivotY - gaugeRadius * 0.1f) // flare right
                close()
            }
            // Shadow
            drawPath(needlePath, Color.Black.copy(0.3f)) 
            // Needle body
            drawPath(needlePath, color = Color(0xFF1A1A1A))
        }
        
        // 6. Pivot dome
        drawIntoCanvas { canvas ->
            canvas.drawCircle(pivot, 10.dp.toPx(),
                Paint().apply {
                    color = Color.Black.copy(0.5f)
                    asFrameworkPaint().maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
                }
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFDDDDDD), Color(0xFF888888), Color(0xFF333333)),
                center = Offset(pivotX - 3f, pivotY - 3f), radius = 12.dp.toPx()
            ),
            radius = 10.dp.toPx(),
            center = pivot
        )
    }
}
```

## 🚀 Advanced Examples

### Dual Stereo Output Meters
```kotlin
@Composable
fun StereoMeters() {
    var leftLevel by remember { mutableFloatStateOf(-20f) }
    var rightLevel by remember { mutableFloatStateOf(-20f) }

    // Simulating audio peaks
    LaunchedEffect(Unit) {
        while(true) {
            leftLevel = (-20f..3f).random().toFloat()
            rightLevel = leftLevel + (-2f..2f).random().toFloat()
            delay(150) // Update very fast
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        VintageMeter(value = leftLevel, unit = "L VU", size = DpSize(150.dp, 90.dp))
        VintageMeter(value = rightLevel, unit = "R VU", size = DpSize(150.dp, 90.dp))
    }
}
```

## ⚠️ Common Gotchas
- **Needle Rotation Origin:** By default, `rotate()` in Canvas rotates around the exact center of the entire canvas. To make a needle sweep properly, you MUST pass a custom `pivot` offset (the base of the needle) to the `rotate(degrees, pivot)` block.
- **Spring Animations:** Analog needles don't move linearly. A `spring` animation with `dampingRatio = 0.5f` gives it that satisfying analog flutter when the value spikes. Using `tween()` will look robotic and fake.

## 💡 Interview Q&A
**Q: How do you handle mapping a custom data range (e.g., -20 to +3) to specific angles on the canvas?**
A: We use linear interpolation. First, we find the "fraction" of the value within the min/max range: `(value - min) / (max - min)`. Then we apply that fraction to the angle range: `startAngle + (fraction * (endAngle - startAngle))`.
