# TextButton - Jetpack Compose

## Theory & Interview Knowledge

### What is TextButton?

TextButton is one of the five fundamental button types in Jetpack Compose that provides the **lowest emphasis** in the button hierarchy. It appears as text without background or border, making it ideal for tertiary actions.

### Material Design 3 Principles

- **Visual Weight**: Lightest among all button variants
- **Emphasis Level**: Lowest - used for dismissive actions
- **Hierarchy Position**: Bottom of the button hierarchy
- **Use Cases**: Cancel actions, navigation links, less important secondary actions

### Key Characteristics

- No background fill
- No border/outline
- Text-only appearance
- Minimal visual impact
- Supports icons alongside text
- Follows Material 3 color scheme

## Core Implementation

### Basic TextButton

```kotlin
@Composable
fun BasicTextButton() {
    TextButton(onClick = { /* Handle click */ }) {
        Text("Text Button")
    }
}
```

### TextButton with Icon

```kotlin
@Composable
fun TextButtonWithIcon() {
    TextButton(onClick = { /* Handle click */ }) {
        Icon(
            Icons.Default.Edit,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text("Edit")
    }
}
```

### Custom Styling

```kotlin
@Composable
fun CustomTextButton() {
    TextButton(
        onClick = { /* Handle click */ },
        colors = ButtonDefaults.textButtonColors(
            contentColor = Color.Red
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text("Delete")
    }
}
```

## Advanced Patterns

### Loading State TextButton

```kotlin
@Composable
fun LoadingTextButton() {
    var isLoading by remember { mutableStateOf(false) }
    
    TextButton(
        onClick = { isLoading = !isLoading },
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
            Text("Loading...")
        } else {
            Text("Retry")
        }
    }
}
```

### Toggle TextButton

```kotlin
@Composable
fun ToggleTextButton() {
    var isSelected by remember { mutableStateOf(false) }
    
    TextButton(
        onClick = { isSelected = !isSelected },
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    ) {
        Icon(
            if (isSelected) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(if (isSelected) "Liked" else "Like")
    }
}
```

### Navigation TextButton

```kotlin
@Composable
fun NavigationTextButton(
    onNavigate: () -> Unit
) {
    TextButton(
        onClick = onNavigate,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text("View Details")
        Spacer(Modifier.size(4.dp))
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }
}
```

## Accessibility Implementation

### Proper Semantic Descriptions

```kotlin
@Composable
fun AccessibleTextButton() {
    TextButton(
        onClick = { /* Cancel action */ },
        modifier = Modifier.semantics {
            contentDescription = "Cancel the current operation"
            role = Role.Button
        }
    ) {
        Text("Cancel")
    }
}
```

### State Descriptions

```kotlin
@Composable
fun StatefulAccessibleTextButton() {
    var isBookmarked by remember { mutableStateOf(false) }
    
    TextButton(
        onClick = { isBookmarked = !isBookmarked },
        modifier = Modifier.semantics {
            contentDescription = if (isBookmarked) {
                "Remove bookmark"
            } else {
                "Add bookmark"
            }
            stateDescription = if (isBookmarked) "Bookmarked" else "Not bookmarked"
        }
    ) {
        Text(if (isBookmarked) "Bookmarked" else "Bookmark")
    }
}
```

### Minimum Touch Target

```kotlin
@Composable
fun MinimumTouchTargetTextButton() {
    TextButton(
        onClick = { },
        modifier = Modifier
            .minimumInteractiveComponentSize() // Ensures 48dp minimum
            .semantics {
                contentDescription = "Skip this step"
            }
    ) {
        Text("Skip")
    }
}
```

## Common Use Cases & Examples

### Dialog Actions

```kotlin
@Composable
fun DialogWithTextButtons(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Action") },
        text = { Text("Are you sure you want to proceed?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

### Menu Items

```kotlin
@Composable
fun MenuWithTextButtons() {
    Column {
        TextButton(
            onClick = { /* Profile action */ },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Profile")
            }
        }
        
        TextButton(
            onClick = { /* Settings action */ },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Settings")
            }
        }
    }
}
```

### Inline Actions

```kotlin
@Composable
fun InlineTextButtons() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Article Title",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Article preview text goes here...",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Row {
                TextButton(onClick = { /* Share */ }) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Share")
                }
                TextButton(onClick = { /* Save */ }) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null)
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Save")
                }
            }
        }
    }
}
```

## Styling & Customization

### Color Customization

```kotlin
@Composable
fun CustomColorTextButtons() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Primary text button
        TextButton(
            onClick = { },
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Primary Action")
        }
        
        // Error text button
        TextButton(
            onClick = { },
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Delete")
        }
        
        // Disabled text button
        TextButton(
            onClick = { },
            enabled = false
        ) {
            Text("Disabled")
        }
    }
}
```

### Size Variants

```kotlin
@Composable
fun TextButtonSizes() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Small text button
        TextButton(
            onClick = { },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("Small", fontSize = 12.sp)
        }
        
        // Regular text button (default)
        TextButton(onClick = { }) {
            Text("Regular")
        }
        
        // Large text button
        TextButton(
            onClick = { },
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("Large", fontSize = 16.sp)
        }
    }
}
```

## Interview Questions & Answers

### Q1: When should you use TextButton instead of other button variants?

**Answer**: TextButton should be used for:

- Dismissive actions (Cancel, Skip, Close)
- Navigation links with low emphasis
- Tertiary actions that shouldn't compete with primary actions
- Menu items in lists
- Inline actions within cards or content areas
- Actions that need minimal visual weight

### Q2: How does TextButton differ from Button in Material 3?

**Answer**:

- **TextButton**: No background, no border, lowest emphasis, text-only appearance
- **Button**: Filled background, highest emphasis, primary actions
- **Use Case**: TextButton for tertiary actions, Button for primary actions
- **Visual Hierarchy**: TextButton sits at the bottom, Button at the top

### Q3: How do you handle accessibility in TextButton?

**Answer**:

- Use `semantics` modifier with proper `contentDescription`
- Add `stateDescription` for stateful buttons
- Ensure minimum 48dp touch target with `minimumInteractiveComponentSize()`
- Provide clear, descriptive text that explains the action
- Use appropriate `Role.Button` for screen readers

### Q4: Can TextButton be used with icons? How?

**Answer**: Yes, TextButton supports icons:

```kotlin
TextButton(onClick = { }) {
    Icon(Icons.Default.Edit, contentDescription = null)
    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
    Text("Edit")
}
```

- Use `ButtonDefaults.IconSize` for consistent icon sizing
- Use `ButtonDefaults.IconSpacing` for proper spacing
- Icons should complement the text, not replace it

### Q5: How do you implement loading states in TextButton?

**Answer**: Use state management with conditional content:

```kotlin
var isLoading by remember { mutableStateOf(false) }
TextButton(enabled = !isLoading) {
    if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("Loading...")
    } else {
        Text("Submit")
    }
}
```

## Best Practices

### Do's ✅

- Use for dismissive actions (Cancel, Skip, Close)
- Place in dialog dismiss positions
- Use for navigation with low emphasis
- Provide clear, action-oriented text
- Use consistent color scheme
- Include proper accessibility descriptions

### Don'ts ❌

- Don't use for primary actions
- Don't use multiple TextButtons competing for attention
- Don't use without clear text labels
- Don't ignore minimum touch target requirements
- Don't use for destructive actions without proper styling

## Performance Considerations

- TextButton has minimal rendering overhead (no background/border)
- Use `remember` for click handlers to avoid recomposition
- Consider using `LazyColumn` for long lists of TextButtons
- Cache icon content when using with icons
- Prefer TextButton for frequently used actions due to lightweight nature

## Common Pitfalls

1. **Hierarchy Confusion**: Using TextButton for primary actions
2. **Accessibility Issues**: Missing contentDescription or touch targets
3. **Visual Clutter**: Too many TextButtons competing for attention
4. **Unclear Labels**: Generic text like "OK" instead of specific actions
5. **Color Misuse**: Not following Material 3 color guidelines

## Related Components

- [[Button]] - Primary filled button
- [[OutlinedButton]] - Secondary outlined button
- [[IconButton]] - Icon-only button
- [[FloatingActionButton]] - Primary floating action