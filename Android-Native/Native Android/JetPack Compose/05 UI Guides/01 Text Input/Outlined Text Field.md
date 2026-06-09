# 🔤 OutlinedTextField

## 📌 Purpose
`OutlinedTextField` is the Material Design component for text input with an outlined style. It has the exact same API signature as `TextField` but uses a different default shape (outlined) and colors. Use it when you need a text field with less visual emphasis than the filled `TextField`, or to create a distinct boundary on complex screens.

## 🔧 Function Signature

### 1. State-Based (NEW in M3 1.4.0)
```kotlin
@Composable
fun OutlinedTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
)
```

### 2. Value-Based (Legacy but widely used)
```kotlin
@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
)
```

## 📋 Props / Parameters

### State-Based Props

| Parameter | Type | Default | Description |
|---|---|---|---|
| state | `TextFieldState` | — | Required. Holds the state of the text field. |
| modifier | `Modifier` | `Modifier` | Optional. Modifier to be applied to the text field. |
| enabled | `Boolean` | `true` | Optional. Controls the enabled state of the text field. |
| readOnly | `Boolean` | `false` | Optional. Controls the editable state of the text field. |
| textStyle | `TextStyle` | `LocalTextStyle.current` | Optional. Style to be applied to the text. |
| label | `@Composable (() -> Unit)?` | `null` | Optional. Label to be displayed over the outline boundary. |
| placeholder | `@Composable (() -> Unit)?` | `null` | Optional. Placeholder to be displayed when the text field is empty. |
| leadingIcon | `@Composable (() -> Unit)?` | `null` | Optional. Leading icon to be displayed at the beginning. |
| trailingIcon | `@Composable (() -> Unit)?` | `null` | Optional. Trailing icon to be displayed at the end. |
| prefix | `@Composable (() -> Unit)?` | `null` | Optional. Prefix text or content to display before the input. |
| suffix | `@Composable (() -> Unit)?` | `null` | Optional. Suffix text or content to display after the input. |
| supportingText | `@Composable (() -> Unit)?` | `null` | Optional. Supporting text to be displayed below the text field. |
| isError | `Boolean` | `false` | Optional. Indicates if the text field's current value is in an error state. |
| inputTransformation | `InputTransformation?` | `null` | Optional. Filters or transforms input before it's applied to the state. |
| outputTransformation | `OutputTransformation?` | `null` | Optional. Transforms how the text is rendered. |
| keyboardOptions | `KeyboardOptions` | `KeyboardOptions.Default` | Optional. Software keyboard options. |
| onKeyboardAction | `KeyboardActionHandler?` | `null` | Optional. Callback when a software keyboard action is triggered. |
| lineLimits | `TextFieldLineLimits` | `TextFieldLineLimits.Default` | Optional. Controls line limits. |
| interactionSource | `MutableInteractionSource?` | `null` | Optional. Interaction source. |
| shape | `Shape` | `OutlinedTextFieldDefaults.shape` | Optional. Shape of the text field container. |
| colors | `TextFieldColors` | `OutlinedTextFieldDefaults.colors()` | Optional. Colors of the text field. |

### Value-Based Differences
| Parameter | Type | Default | Description |
|---|---|---|---|
| value | `String` | — | Required. The current text value. |
| onValueChange | `(String) -> Unit` | — | Required. Callback when the text changes. |
| ⚠️ DEPRECATED visualTransformation | `VisualTransformation` | `VisualTransformation.None` | Use `outputTransformation` in state-based. |
| ⚠️ DEPRECATED keyboardActions | `KeyboardActions` | `KeyboardActions.Default` | Use `onKeyboardAction` in state-based. |
| ⚠️ DEPRECATED singleLine | `Boolean` | `false` | Use `lineLimits` in state-based. |
| maxLines | `Int` | `Int.MAX_VALUE` | Use `lineLimits` in state-based. |
| minLines | `Int` | `1` | Use `lineLimits` in state-based. |

> [!NOTE]
> The primary visual difference from `TextField` is that the `label` rests _on the border_ of the `OutlinedTextField`, interrupting the outline line, whereas in a regular `TextField` it sits inside the filled background.

## ✅ Basic Example

### New State-Based
```kotlin
val state = rememberTextFieldState()

OutlinedTextField(
    state = state,
    label = { Text("Search") }
)
```

## 🚀 Advanced Examples

### 1. Currency Field with Prefix/Suffix
Using the legacy value-based API for quick binding to local state.
```kotlin
var amount by remember { mutableStateOf("") }

OutlinedTextField(
    value = amount,
    onValueChange = { amount = it },
    label = { Text("Amount") },
    prefix = { Text("$") },
    suffix = { Text(".00") },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
)
```

### 2. Error State OutlinedTextField
```kotlin
var username by remember { mutableStateOf("") }
val isError = username.length in 1..3

OutlinedTextField(
    value = username,
    onValueChange = { username = it },
    label = { Text("Username") },
    isError = isError,
    supportingText = {
        if (isError) {
            Text("Username must be at least 4 characters")
        }
    },
    trailingIcon = {
        if (isError) {
            Icon(Icons.Filled.Error, "error", tint = MaterialTheme.colorScheme.error)
        }
    }
)
```

## ⚠️ Common Gotchas
- **Label Cutoff:** If you apply a custom border or shape, ensure you are using `OutlinedTextFieldDefaults.colors()` and not `TextFieldDefaults.colors()`, as the default colors handle the cutout for the label.
- **Height issues with Prefix/Suffix:** Prefixes and suffixes are vertically aligned to the text baseline. Make sure their typography matches the `textStyle` to avoid misalignment.

## 💡 Interview Q&A

**Q: When would you choose `OutlinedTextField` over `TextField`?**
A: `OutlinedTextField` provides less visual emphasis because it uses a stroke rather than a solid background fill. It is typically used in dense forms or when you don't want the text fields to dominate the UI visually.
