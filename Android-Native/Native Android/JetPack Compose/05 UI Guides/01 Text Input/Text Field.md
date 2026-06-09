# 🔤 TextField

## 📌 Purpose
`TextField` is the fundamental Material Design component for text input. It allows users to enter and edit text. Use the filled `TextField` for emphasis or `OutlinedTextField` for lower emphasis. Material 3 (1.4.0) introduced a new state-based API that replaces the legacy value-based API.

## 🔧 Function Signature

### 1. State-Based (NEW in M3 1.4.0)
```kotlin
@Composable
fun TextField(
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
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors()
)
```

### 2. Value-Based (Legacy but widely used)
```kotlin
@Composable
fun TextField(
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
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors()
)
```

## 📋 Props / Parameters

### State-Based Props

| Parameter | Type | Default | Description |
|---|---|---|---|
| state | `TextFieldState` | — | Required. Holds the state of the text field, including the text, selection, and cursor position. |
| modifier | `Modifier` | `Modifier` | Optional. Modifier to be applied to the text field. |
| enabled | `Boolean` | `true` | Optional. Controls the enabled state of the text field. |
| readOnly | `Boolean` | `false` | Optional. Controls the editable state of the text field. |
| textStyle | `TextStyle` | `LocalTextStyle.current` | Optional. Style to be applied to the text. |
| label | `@Composable (() -> Unit)?` | `null` | Optional. Label to be displayed inside the text field container. |
| placeholder | `@Composable (() -> Unit)?` | `null` | Optional. Placeholder to be displayed when the text field is empty. |
| leadingIcon | `@Composable (() -> Unit)?` | `null` | Optional. Leading icon to be displayed at the beginning of the text field. |
| trailingIcon | `@Composable (() -> Unit)?` | `null` | Optional. Trailing icon to be displayed at the end of the text field. |
| prefix | `@Composable (() -> Unit)?` | `null` | Optional. Prefix text or content to display before the input. |
| suffix | `@Composable (() -> Unit)?` | `null` | Optional. Suffix text or content to display after the input. |
| supportingText | `@Composable (() -> Unit)?` | `null` | Optional. Supporting text to be displayed below the text field. |
| isError | `Boolean` | `false` | Optional. Indicates if the text field's current value is in an error state. |
| inputTransformation | `InputTransformation?` | `null` | Optional. Filters or transforms input before it's applied to the state (e.g. max length, allowed chars). |
| outputTransformation | `OutputTransformation?` | `null` | Optional. Transforms how the text is rendered without altering the underlying state (e.g. formatting a phone number). |
| keyboardOptions | `KeyboardOptions` | `KeyboardOptions.Default` | Optional. Software keyboard options (e.g. keyboard type, IME action). |
| onKeyboardAction | `KeyboardActionHandler?` | `null` | Optional. Callback when a software keyboard action is triggered. |
| lineLimits | `TextFieldLineLimits` | `TextFieldLineLimits.Default` | Optional. Controls line limits (`SingleLine`, `MultiLine`, etc.). |
| interactionSource | `MutableInteractionSource?` | `null` | Optional. Interaction source for managing focus, press, and drag state. |
| shape | `Shape` | `TextFieldDefaults.shape` | Optional. Shape of the text field container. |
| colors | `TextFieldColors` | `TextFieldDefaults.colors()` | Optional. Colors of the text field. |

### Value-Based Differences
| Parameter | Type | Default | Description |
|---|---|---|---|
| value | `String` | — | Required. The current text value. |
| onValueChange | `(String) -> Unit` | — | Required. Callback when the text changes. |
| ⚠️ DEPRECATED visualTransformation | `VisualTransformation` | `VisualTransformation.None` | Use `outputTransformation` in state-based. Transforms visual representation (e.g. Password). |
| ⚠️ DEPRECATED keyboardActions | `KeyboardActions` | `KeyboardActions.Default` | Use `onKeyboardAction` in state-based. Defines actions for IME events. |
| ⚠️ DEPRECATED singleLine | `Boolean` | `false` | Use `lineLimits` in state-based. |
| maxLines | `Int` | `Int.MAX_VALUE` | Use `lineLimits` in state-based. Maximum number of visible lines. |
| minLines | `Int` | `1` | Use `lineLimits` in state-based. Minimum number of visible lines. |

> [!NOTE] 
> `TextFieldState` manages the text input lifecycle internally, preventing the classic "cursor jump" issues that plagued the older value-based `TextField` when asynchronous state updates arrived late.

## ✅ Basic Example

### Legacy Value-Based
```kotlin
var text by remember { mutableStateOf("") }

TextField(
    value = text,
    onValueChange = { text = it },
    label = { Text("Enter your name") }
)
```

### New State-Based
```kotlin
val state = rememberTextFieldState()

TextField(
    state = state,
    label = { Text("Enter your name") }
)

// Reading text: state.text.toString()
```

## 🚀 Advanced Examples

### 1. Email Field with Validation
```kotlin
var email by remember { mutableStateOf("") }
val isError = !email.contains("@") && email.isNotEmpty()

TextField(
    value = email,
    onValueChange = { email = it },
    label = { Text("Email Address") },
    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
    isError = isError,
    supportingText = {
        if (isError) {
            Text("Please enter a valid email address", color = MaterialTheme.colorScheme.error)
        } else {
            Text("We will not share your email.")
        }
    },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
)
```

### 2. Password Field with Visibility Toggle
```kotlin
var password by remember { mutableStateOf("") }
var passwordVisible by remember { mutableStateOf(false) }

TextField(
    value = password,
    onValueChange = { password = it },
    label = { Text("Password") },
    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    trailingIcon = {
        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
        IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(image, "Toggle password visibility")
        }
    }
)
```

### 3. State-Based TextField with InputTransformation
Filters input to only allow a maximum of 10 characters.
```kotlin
val state = rememberTextFieldState()

TextField(
    state = state,
    label = { Text("Username (Max 10 chars)") },
    inputTransformation = InputTransformation {
        if (length > 10) {
            revertAllChanges()
        }
    },
    lineLimits = TextFieldLineLimits.SingleLine
)
```

### 4. Custom Styled TextField
```kotlin
TextField(
    value = text,
    onValueChange = { text = it },
    shape = RoundedCornerShape(12.dp),
    colors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
    )
)
```

## ⚠️ Common Gotchas
- **Cursor Jumps:** Using the legacy `value` based API with an asynchronous state flow (like a Redux store or slow ViewModel updates) can cause the cursor to jump to the end of the text. Use the new `TextFieldState` API to avoid this.
- **`VisualTransformation` Crash:** Ensure that `OffsetMapping` used in custom visual transformations perfectly maps back and forth between original and transformed text lengths, otherwise it will crash.
- **Scroll in Scroll:** Placing a multiline `TextField` inside a scrollable column without setting `Modifier.heightIn(max=...)` can cause nested scrolling issues.

## 💡 Interview Q&A

**Q: What is the difference between `inputTransformation` and `outputTransformation`?**
A: `inputTransformation` intercepts and filters what actually gets written into the `TextFieldState` (e.g. limiting length or stripping illegal characters). `outputTransformation` only changes how the text is rendered visually on screen without changing the underlying state (e.g. adding formatting spaces to a credit card number).

**Q: Why was `TextFieldState` introduced?**
A: The value-based API decoupled the UI state from Compose's text field buffer, causing desync issues like cursor jumping when the user typed faster than the state could propagate back from a ViewModel. `TextFieldState` solves this by owning the buffer and exposing changes synchronously while still allowing observation.
