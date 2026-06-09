# TimePicker & TimeInput

## 📌 Purpose
Material 3 introduced native Jetpack Compose time pickers.
- `TimePicker`: Provides the visual clock-face UI to drag and drop hours/minutes.
- `TimeInput`: Provides text entry fields to type in HH:MM.

> [!NOTE] Experimental API
> These components are still under `@ExperimentalMaterial3Api`.
> Also note: There is **NO** built-in `TimePickerDialog`. You must wrap the `TimePicker` inside an `AlertDialog` or `BasicAlertDialog`.

## 🔧 Function Signatures & State

### 1. `TimePickerState`
```kotlin
@ExperimentalMaterial3Api
@Composable
fun rememberTimePickerState(
    initialHour: Int = 0,
    initialMinute: Int = 0,
    is24Hour: Boolean = false
): TimePickerState
```

### 2. `TimePicker`
```kotlin
@ExperimentalMaterial3Api
@Composable
fun TimePicker(
    state: TimePickerState,
    modifier: Modifier = Modifier,
    colors: TimePickerColors = TimePickerDefaults.colors(),
    layoutType: TimePickerLayoutType = TimePickerDefaults.layoutType()
)
```

### 3. `TimeInput`
```kotlin
@ExperimentalMaterial3Api
@Composable
fun TimeInput(
    state: TimePickerState,
    modifier: Modifier = Modifier,
    colors: TimePickerColors = TimePickerDefaults.colors()
)
```

## 📋 Props / Parameters Overview

| Parameter | Type | Description |
|---|---|---|
| `state` | `TimePickerState` | Required. Holds the current `hour` and `minute` selections. |
| `layoutType` | `TimePickerLayoutType` | Optional. Determines the orientation. `Vertical` (clock under numbers) or `Horizontal` (clock beside numbers). Default auto-adapts to screen size. |
| `is24Hour` | `Boolean` | Used in state creation. Toggles between AM/PM picker and 0-23 picker. |

## ✅ Basic Example: TimePicker inside AlertDialog
Because Compose lacks a `TimePickerDialog`, we build our own using `AlertDialog`.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardTimePickerDialog() {
    var showDialog by remember { mutableStateOf(false) }
    val timeState = rememberTimePickerState(initialHour = 10, initialMinute = 30)

    Button(onClick = { showDialog = true }) {
        Text("Pick Time")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select time") },
            text = {
                // The clock UI
                TimePicker(state = timeState)
            },
            confirmButton = {
                TextButton(onClick = {
                    val h = timeState.hour
                    val m = timeState.minute
                    println("Selected Time: $h:$m")
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
```

## 🚀 Advanced Examples

### 1. Toggle between Clock (TimePicker) and Input (TimeInput)
A very common pattern is providing an icon button in the dialog to let the user switch between the dial and keyboard typing.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedTimePickerDialog() {
    var showDialog by remember { mutableStateOf(false) }
    var showingInput by remember { mutableStateOf(false) }
    val timeState = rememberTimePickerState(is24Hour = false)

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select time") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (showingInput) {
                        TimeInput(state = timeState)
                    } else {
                        TimePicker(state = timeState)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            },
            // Adding a bottom-left button in AlertDialog is tricky natively,
            // Often we put it in the dismiss/confirm area, or build a custom BasicAlertDialog.
            icon = {
                IconButton(onClick = { showingInput = !showingInput }) {
                    val icon = if (showingInput) Icons.Default.Schedule else Icons.Default.Keyboard
                    Icon(icon, contentDescription = "Toggle Input Mode")
                }
            }
        )
    }
}
```

### 2. 24-Hour Format Support
If your app defaults to European/Military time, set `is24Hour = true`.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilitaryTimePicker() {
    val timeState = rememberTimePickerState(
        initialHour = 14, 
        initialMinute = 0, 
        is24Hour = true
    )
    
    // Will not show AM/PM toggle buttons
    TimePicker(state = timeState)
}
```

## ⚠️ Common Gotchas

> [!WARNING] No Built-in Dialog
> As mentioned, unlike `DatePickerDialog`, there is no official `TimePickerDialog`. Trying to find it will waste your time. You must manually wrap `TimePicker` in an `AlertDialog` or a custom `Dialog`.

> [!CAUTION] State Extraction
> `TimePickerState.hour` is **always returned in 24-hour format** (0-23) regardless of whether `is24Hour` is true or false. You don't have to manually figure out AM/PM logic to get the absolute hour.

> [!TIP] Responsive Layout
> `TimePickerDefaults.layoutType()` automatically returns `Vertical` on portrait phones and `Horizontal` on landscape phones. You rarely need to override this unless embedding the picker in a tight layout.

## 💡 Interview Q&A

**Q: Does `TimePickerState.hour` return `1` or `13` if the user selects 1 PM on a 12-hour clock?**
A: It returns `13`. `TimePickerState` always tracks time in 24-hour format internally.

**Q: How do you allow a user to type the time using the keyboard instead of dragging the clock hands?**
A: You use the `TimeInput` composable instead of `TimePicker`, passing the exact same `TimePickerState` to it.
