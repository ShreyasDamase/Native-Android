# 🔘 Button (All Variants)

## 📌 Purpose
Buttons allow users to take actions. Material 3 provides 5 variants of standard buttons to express different levels of emphasis:
1. **Button** (Filled) - High emphasis. The primary action on a screen.
2. **ElevatedButton** - High emphasis, but used when a surface needs to stand out visually (e.g. over a scrolling list).
3. **FilledTonalButton** - Medium-high emphasis. Used for important, but not primary, actions. Less visually distracting than a solid filled button.
4. **OutlinedButton** - Medium emphasis. Often paired with a filled button as the alternative/secondary action (e.g. "Cancel").
5. **TextButton** - Low emphasis. Used in dialogs or cards for optional actions.

## 🔧 Function Signature (Shared)

All 5 button variants share the exact same signature, varying only in the default `colors`, `elevation`, and `border` provided by their respective defaults.

```kotlin
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
)
```
*(Replace `Button` with `ElevatedButton`, `FilledTonalButton`, `OutlinedButton`, or `TextButton`)*

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| onClick | `() -> Unit` | — | Required. Callback when button is clicked. |
| modifier | `Modifier` | `Modifier` | Optional. |
| enabled | `Boolean` | `true` | Optional. When false, the button is grayed out and unclickable. |
| shape | `Shape` | `ButtonDefaults.shape` | Optional. Shape of the button (usually `RoundedCornerShape(50)`). |
| colors | `ButtonColors` | `*Defaults.colors()` | Optional. Background and content colors. |
| elevation | `ButtonElevation?` | `*Defaults.elevation()` | Optional. Shadow depth. |
| border | `BorderStroke?` | `null` / Outlined defaults | Optional. Outline around the button. |
| contentPadding | `PaddingValues` | `ButtonDefaults.ContentPadding` | Optional. Internal padding. |
| interactionSource | `MutableInteractionSource?`| `null` | Optional. |
| content | `@Composable RowScope.() -> Unit` | — | Required. The content inside the button (usually a `Text` or `Icon + Text`). |

### ButtonColors Props
- `containerColor`: Background color
- `contentColor`: Text/Icon color
- `disabledContainerColor`: Background when `enabled = false`
- `disabledContentColor`: Text/Icon when `enabled = false`

### ButtonElevation Props
- `defaultElevation`
- `pressedElevation`
- `focusedElevation`
- `hoveredElevation`
- `disabledElevation`

## ✅ Basic Example

```kotlin
Button(onClick = { /* Do something */ }) {
    Text("Submit")
}
```

## 🚀 Advanced Examples

### 1. All 5 Variants Side by Side
```kotlin
Column(
    modifier = Modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    Button(onClick = {}) { Text("Filled Button (Primary)") }
    
    ElevatedButton(onClick = {}) { Text("Elevated Button") }
    
    FilledTonalButton(onClick = {}) { Text("Filled Tonal Button") }
    
    OutlinedButton(onClick = {}) { Text("Outlined Button (Secondary)") }
    
    TextButton(onClick = {}) { Text("Text Button (Tertiary)") }
}
```

### 2. Button with Icon
Using the `RowScope` provided by the `content` lambda.
```kotlin
Button(onClick = { }) {
    Icon(
        imageVector = Icons.Filled.Add,
        contentDescription = "Add",
        modifier = Modifier.size(ButtonDefaults.IconSize)
    )
    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
    Text("Add to Cart")
}
```

### 3. Loading Button State
A pattern for showing a spinner inside the button while work is happening.
```kotlin
var isLoading by remember { mutableStateOf(false) }

Button(
    onClick = { isLoading = true },
    enabled = !isLoading,
    modifier = Modifier.fillMaxWidth()
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp
        )
        Spacer(Modifier.width(8.dp))
        Text("Loading...")
    } else {
        Text("Login")
    }
}
```

### 4. Custom Colored Button
```kotlin
Button(
    onClick = { },
    colors = ButtonDefaults.buttonColors(
        containerColor = Color.Red,
        contentColor = Color.White
    )
) {
    Text("Delete Account")
}
```

## ⚠️ Common Gotchas
- **Text Caps:** Unlike older Android View buttons, Material Compose buttons do **not** capitalize text by default. If you want ALL CAPS, you must enforce it on the string yourself: `Text("submit".uppercase())`.
- **TextButton Padding:** `TextButton` has different default padding than standard buttons. If you try to align a `TextButton` flush with the edge of the screen, you'll notice a gap because the padding is built into the button. You'll need to override `contentPadding`.

## 💡 Interview Q&A

**Q: What is the difference between `Button` and `ElevatedButton`?**
A: `Button` relies on color contrast to distinguish itself from the background (it has 0dp default elevation). `ElevatedButton` uses a shadow (elevation) to stand out, which is useful on noisy backgrounds or scrolling lists where color contrast isn't enough.
