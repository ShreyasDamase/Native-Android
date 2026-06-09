# 🔤 BasicTextField

## 📌 Purpose
`BasicTextField` (from `androidx.compose.foundation.text`) is the lowest-level composable for text input in Jetpack Compose. It provides the core typing functionality, cursor management, and keyboard connection but **zero visual styling**. It has no border, no background, and no Material padding. Use it when you need to build a completely custom text input component from scratch that doesn't fit Material Design constraints.

## 🔧 Function Signature

### State-Based (NEW in Compose Foundation 1.7.0)
```kotlin
@Composable
fun BasicTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    inputTransformation: InputTransformation? = null,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    outputTransformation: OutputTransformation? = null,
    decorator: TextFieldDecorator? = null,
    scrollState: ScrollState = rememberScrollState()
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| state | `TextFieldState` | — | Required. Holds the text field state. |
| modifier | `Modifier` | `Modifier` | Optional. |
| enabled | `Boolean` | `true` | Optional. Controls if text is editable and focusable. |
| readOnly | `Boolean` | `false` | Optional. Text cannot be edited but remains focusable. |
| inputTransformation | `InputTransformation?` | `null` | Optional. Filters/transforms input before state changes. |
| textStyle | `TextStyle` | `TextStyle.Default` | Optional. Base styling for text. |
| keyboardOptions | `KeyboardOptions` | `KeyboardOptions.Default` | Optional. IME configuration. |
| onKeyboardAction | `KeyboardActionHandler?` | `null` | Optional. Hardware/Software keyboard action callbacks. |
| lineLimits | `TextFieldLineLimits` | `TextFieldLineLimits.Default` | Optional. Controls line limits. |
| onTextLayout | `Function?` | `null` | Optional. Callback for text layout computation results. |
| interactionSource | `MutableInteractionSource?` | `null` | Optional. Handles interaction states. |
| cursorBrush | `Brush` | `SolidColor(Color.Black)` | Optional. Brush used to paint the cursor. |
| outputTransformation | `OutputTransformation?` | `null` | Optional. Visual transformation. |
| decorator | `TextFieldDecorator?` | `null` | Optional. Wrap the internal text field with custom UI (e.g., placeholder, underline). |
| scrollState | `ScrollState` | `rememberScrollState()` | Optional. Controls scrolling when text overflows. |

> [!NOTE]
> `TextFieldDecorator` is the modern replacement for the `decorationBox` in the old value-based API. It allows you to place custom UI around the raw text field.

## ✅ Basic Example

```kotlin
val state = rememberTextFieldState()

BasicTextField(
    state = state,
    textStyle = TextStyle(fontSize = 18.sp, color = Color.Black)
)
// Result: Invisible text box until you start typing. No borders, no background.
```

## 🚀 Advanced Examples

### 1. Completely Custom Styled TextField (Underline Only)
Building a clean, minimalist text field that just has an underline and a placeholder.
```kotlin
val state = rememberTextFieldState()
val isFocused by interactionSource.collectIsFocusedAsState()

BasicTextField(
    state = state,
    modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    textStyle = TextStyle(fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface),
    interactionSource = interactionSource,
    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
    decorator = TextFieldDecorator { innerTextField ->
        Column {
            Box(Modifier.fillMaxWidth()) {
                if (state.text.isEmpty()) {
                    Text("Enter text here...", color = Color.Gray, fontSize = 18.sp)
                }
                // The actual text input area
                innerTextField()
            }
            
            // Custom Underline
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(
                thickness = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.LightGray
            )
        }
    }
)
```

### 2. Custom Rounded Search Bar Background
```kotlin
val state = rememberTextFieldState()

BasicTextField(
    state = state,
    modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(Color.LightGray.copy(alpha = 0.3f))
        .padding(horizontal = 16.dp, vertical = 12.dp),
    decorator = TextFieldDecorator { innerTextField ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (state.text.isEmpty()) {
                    Text("Search...", color = Color.Gray)
                }
                innerTextField()
            }
        }
    }
)
```

## ⚠️ Common Gotchas
- **Forgetting `innerTextField()`:** Inside `TextFieldDecorator` (or `decorationBox`), you **must** call the lambda parameter (`innerTextField()`), otherwise the text field won't render at all.
- **Focus Management:** `BasicTextField` does not visually show focus by default. You need to read `InteractionSource` to draw your own focus indicators.
- **Hardware keyboards:** Ensure you handle `onKeyboardAction` if you want pressing "Enter" on a hardware keyboard to perform an action rather than adding a newline.

## 💡 Interview Q&A

**Q: When should I use `BasicTextField` instead of `TextField`?**
A: Use `BasicTextField` when the Material Design specs don't fit your requirements. If you find yourself fighting `TextFieldDefaults` to remove padding, borders, or colors, it's better to drop down to `BasicTextField` and build your custom UI using `decorator`.
