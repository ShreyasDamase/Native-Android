# ✅ RangeSlider

## 📌 Purpose
`RangeSlider` is a variation of the Slider that provides **two thumbs** instead of one. It allows the user to select a range of values (a minimum and a maximum) rather than a single value. It is commonly used for price filters, age ranges, or date range selections.

## 🔧 Function Signature

```kotlin
@Composable
@ExperimentalMaterial3Api
fun RangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    startInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    endInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    startThumb: @Composable (RangeSliderState) -> Unit = { ... },
    endThumb: @Composable (RangeSliderState) -> Unit = { ... },
    track: @Composable (RangeSliderState) -> Unit = { ... }
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| value | `ClosedFloatingPointRange<Float>` | — | Required. The current selected range (e.g., `10f..50f`). |
| onValueChange | `(ClosedFloatingPointRange<Float>) -> Unit` | — | Required. Callback triggered as the user drags either thumb. |
| modifier | `Modifier` | `Modifier` | Optional. |
| enabled | `Boolean` | `true` | Optional. |
| valueRange | `ClosedFloatingPointRange<Float>` | `0f..1f` | Optional. The total possible range of the slider. |
| steps | `Int` | `0` | Optional. Number of discrete steps between min and max. |
| onValueChangeFinished | `(() -> Unit)?` | `null` | Optional. Callback when dragging stops. |
| colors | `SliderColors` | `SliderDefaults.colors()` | Optional. |

## ✅ Basic Example

```kotlin
var sliderPosition by remember { mutableStateOf(0.2f..0.8f) }

RangeSlider(
    value = sliderPosition,
    onValueChange = { sliderPosition = it }
)
```

## 🚀 Advanced Examples

### 1. Basic Price Range Slider
Displaying the current selected range as text.
```kotlin
var priceRange by remember { mutableStateOf(20f..80f) }

Column(Modifier.padding(16.dp)) {
    Text(text = "Price: $${priceRange.start.toInt()} - $${priceRange.endInclusive.toInt()}")
    
    RangeSlider(
        value = priceRange,
        onValueChange = { priceRange = it },
        valueRange = 0f..100f,
        steps = 99 // Allow integer steps
    )
}
```

### 2. Custom Styled Range Slider
```kotlin
var ageRange by remember { mutableStateOf(18f..35f) }

Column(Modifier.padding(16.dp)) {
    Text("Target Age: ${ageRange.start.toInt()} to ${ageRange.endInclusive.toInt()}")
    
    RangeSlider(
        value = ageRange,
        onValueChange = { ageRange = it },
        valueRange = 18f..65f,
        colors = SliderDefaults.colors(
            activeTrackColor = MaterialTheme.colorScheme.tertiary,
            thumbColor = MaterialTheme.colorScheme.tertiary,
            inactiveTrackColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    )
}
```

## ⚠️ Common Gotchas
- **State Type:** Make sure you use `mutableStateOf(start..end)` and not `mutableFloatStateOf`. The state holds a `ClosedFloatingPointRange<Float>`, which is created using the `..` operator.
- **Overlapping Thumbs:** `RangeSlider` automatically prevents the start thumb from crossing the end thumb. You don't need to write validation logic inside `onValueChange` for this.

## 💡 Interview Q&A

**Q: How is the state represented for a `RangeSlider` compared to a regular `Slider`?**
A: A regular `Slider` uses a single `Float` to represent its position. A `RangeSlider` requires a `ClosedFloatingPointRange<Float>` (like `10f..50f`), representing both the starting thumb (`start`) and the ending thumb (`endInclusive`).
