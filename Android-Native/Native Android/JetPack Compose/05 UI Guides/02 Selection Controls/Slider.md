# ✅ Slider

## 📌 Purpose
`Slider` allows users to make selections from a range of values. It is typically used for adjusting settings such as volume, brightness, or applying image filters. It can be continuous (smooth sliding) or discrete (snapping to specific step values).

## 🔧 Function Signature

Material 3 `Slider` (Note: M3 introduced custom `thumb` and `track` parameters).

```kotlin
@Composable
@ExperimentalMaterial3Api
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    thumb: @Composable (SliderState) -> Unit = {
        SliderDefaults.Thumb(
            interactionSource = interactionSource,
            colors = colors,
            enabled = enabled
        )
    },
    track: @Composable (SliderState) -> Unit = { sliderState ->
        SliderDefaults.Track(
            colors = colors,
            enabled = enabled,
            sliderState = sliderState
        )
    }
)
```

*(There is also a simpler stable version without `thumb` and `track` composables).*

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| value | `Float` | — | Required. The current value of the slider. |
| onValueChange | `(Float) -> Unit` | — | Required. Callback triggered as the user drags the slider. |
| modifier | `Modifier` | `Modifier` | Optional. |
| enabled | `Boolean` | `true` | Optional. |
| valueRange | `ClosedFloatingPointRange<Float>` | `0f..1f` | Optional. The range of values the slider can take. |
| steps | `Int` | `0` | Optional. Number of discrete steps *between* the min and max. 0 means continuous. |
| onValueChangeFinished | `(() -> Unit)?` | `null` | Optional. Callback triggered only when the user *releases* the drag. |
| colors | `SliderColors` | `SliderDefaults.colors()` | Optional. |
| interactionSource | `MutableInteractionSource` | `new instance` | Optional. |
| thumb | `@Composable` | Default thumb | Optional. Custom UI for the thumb. |
| track | `@Composable` | Default track | Optional. Custom UI for the track. |

## ✅ Basic Example

```kotlin
var sliderPosition by remember { mutableFloatStateOf(0.5f) }

Slider(
    value = sliderPosition,
    onValueChange = { sliderPosition = it }
)
```

## 🚀 Advanced Examples

### 1. Discrete Slider with Steps
This slider goes from 0 to 100, but only allows picking values in increments of 10. (10 increments = 9 steps in between).
```kotlin
var sliderPosition by remember { mutableFloatStateOf(0f) }

Column(Modifier.padding(16.dp)) {
    Text(text = "Volume: ${sliderPosition.toInt()}")
    Slider(
        value = sliderPosition,
        onValueChange = { sliderPosition = it },
        valueRange = 0f..100f,
        steps = 9 // (100 - 0) / 10 - 1 = 9 discrete steps between min and max
    )
}
```

### 2. Slider with `onValueChangeFinished`
Useful when `onValueChange` updates a UI rapidly, but you only want to send the final value to a database or API when the user lets go.
```kotlin
var tempValue by remember { mutableFloatStateOf(50f) }

Slider(
    value = tempValue,
    onValueChange = { tempValue = it }, // Update UI instantly
    valueRange = 0f..100f,
    onValueChangeFinished = {
        // e.g., viewModel.saveVolumeLevel(tempValue)
        println("User released thumb at $tempValue")
    }
)
```

### 3. Custom Thumb and Track (Material 3)
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSlider() {
    var sliderPosition by remember { mutableFloatStateOf(5f) }
    val interactionSource = remember { MutableInteractionSource() }

    Slider(
        value = sliderPosition,
        onValueChange = { sliderPosition = it },
        valueRange = 0f..10f,
        interactionSource = interactionSource,
        thumb = {
            // Custom Square Thumb
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.Red, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sliderPosition.toInt().toString(),
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        },
        track = { sliderState ->
            // Custom Track
            SliderDefaults.Track(
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Red,
                    inactiveTrackColor = Color.LightGray
                ),
                sliderState = sliderState,
                modifier = Modifier.height(8.dp) // Thicker track
            )
        }
    )
}
```

## ⚠️ Common Gotchas
- **`steps` Math:** The `steps` parameter is confusing. It represents the number of ticks *between* the start and end values.
  Formula: `steps = ((max - min) / increment) - 1`. 
  Example: Range 0 to 100, increment by 10. `((100 - 0) / 10) - 1 = 9`.
- **State Trashing:** Doing heavy work inside `onValueChange` will cause UI stuttering because it's called constantly during a drag. Only update the local `mutableFloatStateOf` inside `onValueChange`, and do the heavy lifting in `onValueChangeFinished`.

## 💡 Interview Q&A

**Q: How do you prevent excessive API calls while a user is dragging a slider?**
A: Use the `onValueChange` callback only to update the local Compose UI state so the thumb moves smoothly. Use the `onValueChangeFinished` callback to execute the API call or database write once the user lifts their finger.
