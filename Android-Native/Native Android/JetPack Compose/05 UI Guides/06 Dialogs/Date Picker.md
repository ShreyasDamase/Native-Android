# DatePicker & DateRangePicker

## 📌 Purpose
Material 3 brings fully native, state-driven date pickers to Jetpack Compose.
- `DatePicker`: Selects a single date.
- `DateRangePicker`: Selects a start and end date.
- `DatePickerDialog`: A wrapper container to display the pickers as a modal dialog.

> [!NOTE] Experimental API
> These components are still under `@ExperimentalMaterial3Api`.

## 🔧 Function Signatures & States

### 1. `DatePickerState`
```kotlin
@ExperimentalMaterial3Api
@Composable
fun rememberDatePickerState(
    initialSelectedDateMillis: Long? = null,
    initialDisplayedMonthMillis: Long? = null,
    yearRange: IntRange = DatePickerDefaults.YearRange,
    initialDisplayMode: DisplayMode = DisplayMode.Picker, // .Picker (calendar) or .Input (text fields)
    selectableDates: SelectableDates = DatePickerDefaults.AllDates
): DatePickerState
```

### 2. `DatePicker`
```kotlin
@ExperimentalMaterial3Api
@Composable
fun DatePicker(
    state: DatePickerState,
    modifier: Modifier = Modifier,
    dateFormatter: DatePickerFormatter = remember { DatePickerDefaults.dateFormatter() },
    title: (@Composable () -> Unit)? = { DatePickerDefaults.DatePickerTitle(...) },
    headline: (@Composable () -> Unit)? = { DatePickerDefaults.DatePickerHeadline(...) },
    showModeToggle: Boolean = true,
    colors: DatePickerColors = DatePickerDefaults.colors()
)
```

### 3. `DatePickerDialog`
```kotlin
@ExperimentalMaterial3Api
@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    shape: Shape = DatePickerDefaults.shape,
    tonalElevation: Dp = DatePickerDefaults.TonalElevation,
    colors: DatePickerColors = DatePickerDefaults.colors(),
    properties: DialogProperties = DialogProperties(),
    content: @Composable ColumnScope.() -> Unit
)
```

## 📋 Props / Parameters Overview

| Parameter | Type | Description |
|---|---|---|
| `state` | `DatePickerState` | Required. The state object holding selected milliseconds and UI mode. |
| `title` | `@Composable` | Optional. Top-level title (e.g. "Select Date"). |
| `headline` | `@Composable` | Optional. Displays the currently selected date. |
| `showModeToggle` | `Boolean` | Optional. Whether to show the button that swaps Calendar view to Text Input view. |
| `selectableDates` | `SelectableDates` | Used in state creation. An interface to block out certain dates (e.g., past dates). |

## ✅ Basic Example: DatePickerDialog
Since `DatePicker` is just a UI layout, you almost always put it inside `DatePickerDialog`.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDatePickerDialog() {
    var showDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Button(onClick = { showDialog = true }) {
        Text("Show Date Picker")
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Read selected date: datePickerState.selectedDateMillis
                        showDialog = false
                    },
                    enabled = datePickerState.selectedDateMillis != null // Force selection
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
```

## 🚀 Advanced Examples

### 1. DateRangePicker for Booking
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDateRangePicker() {
    val state = rememberDateRangePickerState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        DateRangePicker(
            state = state,
            modifier = Modifier.weight(1f),
            title = { Text(text = "Select Trip Dates", modifier = Modifier.padding(16.dp)) },
            headline = {
                DateRangePickerDefaults.DateRangePickerHeadline(
                    selectedStartDateMillis = state.selectedStartDateMillis,
                    selectedEndDateMillis = state.selectedEndDateMillis,
                    displayMode = state.displayMode,
                    dateFormatter = remember { DatePickerDefaults.dateFormatter() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        )
        
        Button(
            onClick = {
                val start = state.selectedStartDateMillis
                val end = state.selectedEndDateMillis
                // Proceed with booking
            },
            modifier = Modifier.padding(16.dp),
            enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null
        ) {
            Text("Confirm Dates")
        }
    }
}
```

### 2. Disabling Past Dates (Custom Validator)
Pass a `SelectableDates` implementation into your `rememberDatePickerState` to gray-out unavailable dates.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutureOnlyDatePicker() {
    val currentMillis = System.currentTimeMillis()
    
    val state = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // Disable dates before today
                return utcTimeMillis >= currentMillis - 86400000 // approx minus 1 day offset
            }
            override fun isSelectableYear(year: Int): Boolean {
                return year >= 2024
            }
        }
    )

    DatePicker(state = state)
}
```

### 3. Embedded DatePicker in a BottomSheet
You don't *have* to use `DatePickerDialog`. `DatePicker` is just a composable.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetDatePicker() {
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState()

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            DatePicker(state = dateState)
            Button(
                onClick = { showSheet = false },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("Select")
            }
        }
    }
}
```

## ⚠️ Common Gotchas

> [!CAUTION] UTC vs Local Time
> The milliseconds returned by `selectedDateMillis` are **UTC midnights**. When you format these to strings for display using `java.text.SimpleDateFormat` or `java.time`, make sure to treat them as UTC or apply timezone offsets correctly, otherwise the user might see the *previous day's date* due to timezone shifts.

> [!TIP] Missing DatePickerDialog
> `DatePickerDialog` is purely a container. It doesn't contain a date picker by default. You MUST place `<DatePicker>` inside its `content` block.

> [!WARNING] No built-in null-check for Confirm
> The `DatePickerDialog` confirm button doesn't automatically disable itself if no date is picked. You must manually add `enabled = state.selectedDateMillis != null` to your Confirm button.

## 💡 Interview Q&A

**Q: How do you extract the selected date from `DatePicker`?**
A: You access `datePickerState.selectedDateMillis`, which returns a `Long?` representing the UTC epoch milliseconds of the chosen date.

**Q: What is the difference between `DisplayMode.Picker` and `DisplayMode.Input`?**
A: `Picker` shows the visual calendar grid for clicking on days. `Input` shows a text field layout where the user types in the date manually (e.g. MM/DD/YYYY).
