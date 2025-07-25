# OutlinedButton

## Theory & Interview Knowledge

### Purpose & Design Philosophy

- **Boundary Emphasis**: Uses border to define button area without background
- **Alternative Actions**: Represents choices alternative to primary actions
- **Visual Clarity**: Clear button boundaries without adding visual weight
- **Minimal Design**: Clean appearance that doesn't compete with primary actions

### When to Use OutlinedButton

✅ **Good Use Cases:**

- Alternative actions (Cancel, Back, Skip)
- Secondary choices in decision scenarios
- Actions that need clear boundaries but low emphasis
- Multiple similar-weight options
- Dialog negative actions (Cancel, Dismiss)

❌ **Avoid When:**

- Primary actions (use Button instead)
- Single prominent actions on screen
- Actions requiring immediate attention
- Very small sizes (border may become unclear)

### Technical Implementation

```kotlin
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
)
```

### Border System Deep Dive

**Default Border:**

```kotlin
ButtonDefaults.outlinedButtonBorder = BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.outline
)
```

**Color System:**

```kotlin
ButtonDefaults.outlinedButtonColors(
    containerColor = Color.Transparent,            // No background
    contentColor = MaterialTheme.colorScheme.primary,
    disabledContainerColor = Color.Transparent,
    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
)
```

## Practical Examples

### Basic Implementation

```kotlin
@Composable
fun BasicOutlinedButtonExample() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Standard outlined button
        OutlinedButton(onClick = { /* Handle click */ }) {
            Text("Cancel")
        }
        
        // With icon
        OutlinedButton(onClick = { /* Handle click */ }) {
            Icon(
                Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Download")
        }
        
        // Disabled state
        OutlinedButton(
            onClick = { /* Handle click */ },
            enabled = false
        ) {
            Text("Unavailable")
        }
    }
}
```

### Custom Border Styles

```kotlin
@Composable
fun CustomBorderExamples() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Thick border
        OutlinedButton(
            onClick = { },
            border = BorderStroke(2.dp, Color.Blue)
        ) {
            Text("Thick Border")
        }
        
        // Dashed border (custom implementation)
        OutlinedButton(
            onClick = { },
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        Color.Transparent,
                        MaterialTheme.colorScheme.primary
                    )
                )
            )
        ) {
            Text("Gradient Border")
        }
        
        // Colored border with matching content
        OutlinedButton(
            onClick = { },
            border = BorderStroke(1.dp, Color.Green),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.Green
            )
        ) {
            Text("Green Theme")
        }
        
        // No border (transparent)
        OutlinedButton(
            onClick = { },
            border = null
        ) {
            Text("No Border")
        }
    }
}
```

### Dialog Action Pattern

```kotlin
@Composable
fun DialogActionPattern() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Confirm Action",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Text(
                "Are you sure you want to proceed?",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Standard dialog button layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // Secondary action - outlined
                OutlinedButton(
                    onClick = { /* Dismiss */ }
                ) {
                    Text("Cancel")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Primary action - filled
                Button(
                    onClick = { /* Confirm */ }
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}
```

### Choice Selection Pattern

```kotlin
@Composable
fun ChoiceSelectionExample() {
    var selectedChoice by remember { mutableStateOf<String?>(null) }
```