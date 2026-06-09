# 🔤 BasicSecureTextField

## 📌 Purpose
`BasicSecureTextField` (from `androidx.compose.foundation.text`) is the foundation-level, unstyled text input for secure content like passwords and PINs. It is optimized to obscure text and disable features that might compromise security (like copy/paste or learning dictionaries in the IME).

## 🔧 Function Signature

```kotlin
@Composable
fun BasicSecureTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    inputTransformation: InputTransformation? = null,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.SecureField,
    onKeyboardAction: KeyboardActionHandler? = null,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    decorator: TextFieldDecorator? = null,
    scrollState: ScrollState = rememberScrollState(),
    textObfuscationMode: TextObfuscationMode = TextObfuscationMode.RevealLastTyped,
    textObfuscationCharacter: Char = '\u2022' // Bullet character
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| state | `TextFieldState` | — | Required. Holds the text field state. |
| modifier | `Modifier` | `Modifier` | Optional. |
| enabled | `Boolean` | `true` | Optional. |
| inputTransformation | `InputTransformation?` | `null` | Optional. Filters/transforms input. |
| textStyle | `TextStyle` | `TextStyle.Default` | Optional. Text style. |
| keyboardOptions | `KeyboardOptions` | `KeyboardOptions.SecureField` | Optional. IME configuration. Defaults to password type. |
| onKeyboardAction | `KeyboardActionHandler?` | `null` | Optional. Callback for keyboard events. |
| onTextLayout | `Function?` | `null` | Optional. |
| interactionSource | `MutableInteractionSource?` | `null` | Optional. |
| cursorBrush | `Brush` | `SolidColor(Color.Black)` | Optional. Cursor color. |
| decorator | `TextFieldDecorator?` | `null` | Optional. Wrap the text field with custom UI. |
| scrollState | `ScrollState` | `rememberScrollState()` | Optional. |
| textObfuscationMode | `TextObfuscationMode` | `TextObfuscationMode.RevealLastTyped` | Optional. How to hide text (`RevealLastTyped`, `Hidden`, `Visible`). |
| textObfuscationCharacter | `Char` | `'\u2022'` (•) | Optional. The character to display instead of the real text. |

## ✅ Basic Example

```kotlin
val state = rememberTextFieldState()

BasicSecureTextField(
    state = state,
    textStyle = TextStyle(fontSize = 24.sp, letterSpacing = 4.sp)
)
```

## 🚀 Advanced Examples

### 1. Custom PIN Entry Field
Creating a secure PIN entry field using `BasicSecureTextField` with a custom text styling and custom obfuscation character.
```kotlin
val state = rememberTextFieldState()

BasicSecureTextField(
    state = state,
    textObfuscationMode = TextObfuscationMode.Hidden,
    textObfuscationCharacter = '*',
    inputTransformation = InputTransformation {
        // Limit to 4 digits
        if (!asCharSequence().all { it.isDigit() } || length > 4) {
            revertAllChanges()
        }
    },
    decorator = TextFieldDecorator { innerTextField ->
        Box(
            modifier = Modifier
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            if (state.text.isEmpty()) {
                Text("****", color = Color.LightGray, letterSpacing = 8.sp)
            }
            innerTextField()
        }
    }
)
```

## ⚠️ Common Gotchas
- **No `readOnly` parameter:** Secure fields usually don't need a read-only mode, so it's not present. If you need to disable input, use `enabled = false`.
- **Keyboard type:** By default it uses `KeyboardOptions.SecureField` which maps to `KeyboardType.Password`. If you want a numeric PIN, you must override `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)`.
- **Copy/Paste:** Copying text from a `BasicSecureTextField` is disabled by the framework for security reasons.

## 💡 Interview Q&A

**Q: What are the `TextObfuscationMode` options?**
A: 
- `RevealLastTyped`: The default. Briefly shows the last character typed before turning it into a dot.
- `Hidden`: Immediately turns everything into a dot.
- `Visible`: Shows the text completely (useful for "show password" toggle buttons).
