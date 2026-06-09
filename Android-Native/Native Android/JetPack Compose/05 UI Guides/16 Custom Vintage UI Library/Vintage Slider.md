# 🎚️ VintageSlider

## 📌 Purpose
The `VintageSlider` replicates a studio mixing board fader. It uses a deep grooved track, a colored active fill line, and a tactile, 3D metallic thumb that supports horizontal dragging.

## 🔧 Function Signature
```kotlin
@Composable
fun VintageSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    width: Dp = 200.dp,
    height: Dp = 40.dp,
    trackColor: Color = Color(0xFF4CAF50),
    showValueBubble: Boolean = true
)
```

## ✅ Basic Implementation

```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun VintageSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    width: Dp = 200.dp,
    height: Dp = 40.dp,
    trackColor: Color = Color(0xFF4CAF50),
    showValueBubble: Boolean = true
) {
    var dragStartPx by remember { mutableFloatStateOf(0f) }
    var dragStartValue by remember { mutableFloatStateOf(value) }
    
    val textMeasurer = rememberTextMeasurer()
    
    Canvas(
        modifier = modifier
            .size(width, height)
            .pointerInput(valueRange) {
                detectDragGestures(
                    onDragStart = { 
                        dragStartPx = it.x
                        dragStartValue = value 
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val thumbTravelArea = size.width - size.height // Reserve space for thumb
                        val dx = change.position.x - dragStartPx
                        val valueDelta = (dx / thumbTravelArea) * (valueRange.endInclusive - valueRange.start)
                        val newValue = (dragStartValue + valueDelta).coerceIn(valueRange.start, valueRange.endInclusive)
                        onValueChange(newValue)
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        
        val trackHeight = 12.dp.toPx()
        val trackY = (h - trackHeight) / 2f
        val trackCorner = CornerRadius(trackHeight / 2f)
        
        val thumbWidth = h * 0.6f
        val thumbHeight = h
        val travelArea = w - thumbWidth
        val valueFraction = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
        val thumbX = (valueFraction * travelArea)
        
        // 1. Groove (Outer Track Base)
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF222222), Color(0xFF555555))
            ),
            topLeft = Offset(0f, trackY),
            size = Size(w, trackHeight),
            cornerRadius = trackCorner
        )
        
        // 2. Active Track Fill
        val activeWidth = thumbX + (thumbWidth / 2f)
        drawRoundRect(
            color = trackColor.copy(alpha = 0.8f),
            topLeft = Offset(0f, trackY),
            size = Size(activeWidth, trackHeight),
            cornerRadius = trackCorner
        )
        
        // Inner shadow to make track look deep
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color.Black.copy(0.6f), Color.Transparent)
            ),
            topLeft = Offset(0f, trackY),
            size = Size(w, trackHeight * 0.5f),
            cornerRadius = trackCorner
        )
        
        // 3. Thumb Shadow
        drawIntoCanvas { canvas ->
            canvas.drawRoundRect(
                left = thumbX - 2f, top = 2f, right = thumbX + thumbWidth + 4f, bottom = thumbHeight + 4f,
                radiusX = 6.dp.toPx(), radiusY = 6.dp.toPx(),
                paint = Paint().apply {
                    color = Color.Black.copy(alpha = 0.5f)
                    asFrameworkPaint().maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
                }
            )
        }
        
        // 4. Thumb Body (Metallic Capsule)
        drawRoundRect(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0f to Color(0xFFE0E0E0),
                    0.2f to Color(0xFFFFFFFF),
                    0.5f to Color(0xFFA0A0A0),
                    1f to Color(0xFF505050)
                ),
                start = Offset(thumbX, 0f), end = Offset(thumbX + thumbWidth, 0f)
            ),
            topLeft = Offset(thumbX, 0f),
            size = Size(thumbWidth, thumbHeight),
            cornerRadius = CornerRadius(6.dp.toPx())
        )
        
        // Thumb Indentation Line (middle marker)
        drawLine(
            color = Color.Black.copy(alpha = 0.5f),
            start = Offset(thumbX + thumbWidth / 2f, h * 0.2f),
            end = Offset(thumbX + thumbWidth / 2f, h * 0.8f),
            strokeWidth = 3f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(thumbX + thumbWidth / 2f + 1f, h * 0.2f),
            end = Offset(thumbX + thumbWidth / 2f + 1f, h * 0.8f),
            strokeWidth = 1.5f
        )
        
        // 5. Value Text Bubble
        if (showValueBubble) {
            val percentage = (valueFraction * 100).roundToInt()
            val textLayout = textMeasurer.measure("$percentage%", TextStyle(fontSize = 10.sp, color = Color.White))
            drawText(
                textLayout,
                topLeft = Offset(
                    x = thumbX + (thumbWidth / 2f) - (textLayout.size.width / 2f),
                    y = -textLayout.size.height - 4.dp.toPx()
                )
            )
        }
    }
}
```

## 🚀 Advanced Examples

### Graphic Equalizer
You can rotate the slider to act as an EQ band fader.

```kotlin
@Composable
fun Equalizer() {
    var band1 by remember { mutableFloatStateOf(0.5f) }
    
    // Rotating a horizontal slider -90 degrees makes it vertical
    VintageSlider(
        value = band1,
        onValueChange = { band1 = it },
        modifier = Modifier
            .rotate(-90f)
            .padding(32.dp),
        width = 150.dp,
        height = 30.dp
    )
}
```

## ⚠️ Common Gotchas
- **Drag area limits:** The thumb shouldn't slide *past* the track. In `detectDragGestures`, we calculate `travelArea = w - thumbWidth` to ensure the center of the thumb respects its own bounds, mapping `0%` and `100%` perfectly to the edges.
- **Hit Detection:** To make the slider easier to grab on touch screens, wrap it in a `Box` that extends beyond the Canvas size.
