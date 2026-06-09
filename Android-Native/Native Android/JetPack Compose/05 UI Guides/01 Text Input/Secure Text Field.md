# 🔤 SecureTextField

## 📌 Purpose
`SecureTextField` and `OutlinedSecureTextField` (NEW in Material3 1.4.0) are fully styled Material components designed specifically for secure text input, such as passwords. They are built on top of `BasicSecureTextField` and bring the state-based API with native text obfuscation support, replacing the old `VisualTransformation` hack for passwords.

## 🔧 Function Signature

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
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
    keyboardOptions: KeyboardOptions = KeyboardOptions.SecureField,
    onKeyboardAction: KeyboardActionHandler? = null,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    textObfuscationMode: TextObfuscationMode = TextObfuscationMode.RevealLastTyped,
    textObfuscationCharacter: Char = '\u2022'
)
```
*(Signature for `OutlinedSecureTextField` is identical except for defaults like `OutlinedTextFieldDefaults.shape`)*

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| state | `TextFieldState` | — | Required. Holds the text field state. |
| modifier | `Modifier` | `Modifier` | Optional. |
| enabled | `Boolean` | `true` | Optional. |
| textStyle | `TextStyle` | `LocalTextStyle.current` | Optional. |
| label...supportingText | `@Composable` | `null` | Optional. Same UI decorators as regular `TextField`. |
| isError | `Boolean` | `false` | Optional. |
| inputTransformation | `InputTransformation?` | `null` | Optional. |
| keyboardOptions | `KeyboardOptions` | `KeyboardOptions.SecureField` | Optional. Defaults to Password type. |
| onKeyboardAction | `KeyboardActionHandler?` | `null` | Optional. |
| textObfuscationMode | `TextObfuscationMode` | `RevealLastTyped` | Optional. How to hide text (`RevealLastTyped`, `Hidden`, `Visible`). |
| textObfuscationCharacter | `Char` | `'\u2022'` | Optional. Character used for masking. |

## ✅ Basic Example

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimplePassword() {
    val state = rememberTextFieldState()
    
    SecureTextField(
        state = state,
        label = { Text("Password") }
    )
}
```

## 🚀 Advanced Examples

### 1. Password Field with Show/Hide Toggle
The modern, recommended way to build a togglable password field using the new `TextObfuscationMode`.
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TogglePassword() {
    val state = rememberTextFieldState()
    var isVisible by remember { mutableStateOf(false) }

    OutlinedSecureTextField(
        state = state,
        label = { Text("Password") },
        textObfuscationMode = if (isVisible) {
            TextObfuscationMode.Visible
        } else {
            TextObfuscationMode.RevealLastTyped
        },
        trailingIcon = {
            IconButton(onClick = { isVisible = !isVisible }) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (isVisible) "Hide password" else "Show password"
                )
            }
        }
    )
}
```

## ⚠️ Common Gotchas
- **Experimental API:** As of M3 1.4.0, this is still marked with `@ExperimentalMaterial3Api`. You must opt-in to use it.
- **Copying is disabled:** Users cannot copy the text out of a `SecureTextField` by design.

## 💡 Interview Q&A

**Q: Why use `SecureTextField` instead of `TextField` with `PasswordVisualTransformation`?**
A: `SecureTextField` uses `BasicSecureTextField` under the hood, which correctly disables clipboard access, informs the IME to disable learning/auto-complete, and efficiently manages character masking. `PasswordVisualTransformation` was a UI hack that didn't guarantee system-level security on the input.
