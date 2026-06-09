# 🔘 SplitButton

## 📌 Purpose
`SplitButton` is a **Material 3 Expressive** component (introduced in M3 1.4.x). It creates a unified layout containing two distinct interaction areas: a primary action button on the left, and a secondary action (usually a dropdown chevron) on the right. 

> [!WARNING]
> This is an Experimental API and requires `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`.

## 🔧 Function Signature

```kotlin
@ExperimentalMaterial3ExpressiveApi
@Composable
fun SplitButtonLayout(
    leadingButton: @Composable () -> Unit,
    trailingButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = SplitButtonDefaults.Spacing
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| leadingButton | `@Composable () -> Unit` | — | Required. The primary action (usually a normal Button or FilledTonalButton). |
| trailingButton | `@Composable () -> Unit` | — | Required. The secondary action (usually an IconButton or an OutlinedButton with a dropdown arrow). |
| modifier | `Modifier` | `Modifier` | Optional. |
| spacing | `Dp` | `SplitButtonDefaults.Spacing` | Optional. Space between the two halves. |

## ✅ Basic Example

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SimpleSplitButton() {
    SplitButtonLayout(
        leadingButton = {
            Button(
                onClick = { /* Primary Save Action */ },
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 0.dp, bottomEnd = 0.dp)
            ) {
                Text("Save")
            }
        },
        trailingButton = {
            FilledIconButton(
                onClick = { /* Open Dropdown */ },
                shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "More save options")
            }
        }
    )
}
```

## 🚀 Advanced Examples

### 1. Save Button + Dropdown Options
A common pattern in content management systems: "Save Draft" vs "Save & Publish".

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SaveSplitButton() {
    var expanded by remember { mutableStateOf(false) }
    
    SplitButtonLayout(
        leadingButton = {
            Button(
                onClick = { println("Saved as Draft!") },
                // Custom shape to make them look connected
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 4.dp, bottomEnd = 4.dp)
            ) {
                Text("Save Draft")
            }
        },
        trailingButton = {
            Box {
                Button(
                    onClick = { expanded = true },
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 24.dp, bottomEnd = 24.dp)
                ) {
                    Icon(Icons.Default.ArrowDropDown, "Save Options")
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Save & Publish") },
                        onClick = { 
                            println("Published!")
                            expanded = false 
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export to PDF") },
                        onClick = { expanded = false }
                    )
                }
            }
        }
    )
}
```

## ⚠️ Common Gotchas
- **Shapes:** `SplitButtonLayout` just positions the two buttons. It does **not** automatically round the outside corners and flatten the inside corners. You have to pass specific `RoundedCornerShape` values to the `leadingButton` and `trailingButton` yourself to achieve the connected "split button" look.
- **Semantic grouping:** Make sure to group the component semantically if needed so screen readers understand the primary action is related to the dropdown options.

## 💡 Interview Q&A

**Q: Why use `SplitButtonLayout` instead of just a `Row` with two buttons?**
A: `SplitButtonLayout` is explicitly built to handle the visual alignment and specific sizing constraints defined by the Material 3 Expressive guidelines, ensuring the two halves align perfectly on their vertical centers and take up consistent height.
