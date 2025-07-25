# ElevatedButton

## Theory & Interview Knowledge

### Purpose & Design Philosophy

- **Secondary Action Emphasis**: More prominent than outlined buttons, less than filled
- **Physical Metaphor**: Uses shadow/elevation to create depth perception
- **Light Theme Optimization**: Works particularly well in light themes where shadows are visible
- **Accessibility**: Shadow provides additional visual cue beyond color

### When to Use ElevatedButton

✅ **Good Use Cases:**

- Important secondary actions that need prominence
- Actions that benefit from appearing "raised" above content
- Alternatives to primary actions in choice scenarios
- Cards or surfaces where subtle elevation helps hierarchy

❌ **Avoid When:**

- Primary actions (use Button instead)
- Dark themes where elevation is less visible
- Destructive actions (use appropriate colors with other variants)
- Space-constrained layouts (elevation increases visual footprint)

### Technical Implementation

```kotlin
@Composable
fun ElevatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.elevatedShape,
    colors: ButtonColors = ButtonDefaults.elevatedButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.elevatedButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
)
```

### Key Properties Explained

**Elevation System:**

```kotlin
// Default elevation values
ButtonDefaults.elevatedButtonElevation(
    defaultElevation = 1.dp,      // Rest state
    pressedElevation = 1.dp,      // When pressed
    focusedElevation = 1.dp,      // When focused
    hoveredElevation = 3.dp,      // On hover (desktop)
    disabledElevation = 0.dp      // When disabled
)
```

**Color System:**

```kotlin
// Default colors adapt to theme
ButtonDefaults.elevatedButtonColors(
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.primary,
    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
)
```

## Practical Examples

### Basic Implementation

```kotlin
@Composable
fun BasicElevatedButtonExample() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Standard elevated button
        ElevatedButton(onClick = { /* Handle click */ }) {
            Text("Save Draft")
        }
        
        // With icon
        ElevatedButton(onClick = { /* Handle click */ }) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Add to Favorites")
        }
        
        // Disabled state
        ElevatedButton(
            onClick = { /* Handle click */ },
            enabled = false
        ) {
            Text("Unavailable")
        }
    }
}
```

### Custom Elevation

```kotlin
@Composable
fun CustomElevationExample() {
    ElevatedButton(
        onClick = { /* Handle click */ },
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 6.dp
        )
    ) {
        Text("High Elevation")
    }
}
```

### Color Customization

```kotlin
@Composable
fun CustomColorElevatedButton() {
    ElevatedButton(
        onClick = { /* Handle click */ },
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Icon(Icons.Default.Settings, contentDescription = null)
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text("Settings")
    }
}
```

### Loading State Pattern

```kotlin
@Composable
fun LoadingElevatedButton() {
    var isLoading by remember { mutableStateOf(false) }
    
    ElevatedButton(
        onClick = {
            if (!isLoading) {
                isLoading = true
            }
        },
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
            Text("Processing...")
        } else {
            Text("Submit Form")
        }
    }
    
    // Reset loading state
    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(2000)
            isLoading = false
        }
    }
}
```

## Interview Questions & Answers

**Q: Why use ElevatedButton instead of Button with custom elevation?**

- **Semantic Clarity**: ElevatedButton has specific design intent
- **Theme Integration**: Automatically adapts colors for elevated surfaces
- **Accessibility**: Built-in elevation behavior for different states
- **Consistency**: Follows Material Design elevation patterns

**Q: How does elevation affect performance?**

- **Shadow Rendering**: Additional GPU work for shadow effects
- **Overdraw**: Elevated elements may cause more overdraw
- **Animation**: Elevation changes trigger smooth animations
- **Memory**: Minimal impact, mostly rendering overhead

**Q: When should you modify default elevation values?**

- **Brand Requirements**: Custom elevation fits brand language
- **Hierarchy Needs**: Multiple elevation levels needed
- **Platform Differences**: Desktop vs mobile elevation preferences
- **Content Context**: Cards, dialogs may need different elevations

**Q: How do you test elevated buttons?**

```kotlin
@Test
fun elevatedButtonClickTest() {
    composeTestRule.setContent {
        var clicked by remember { mutableStateOf(false) }
        ElevatedButton(onClick = { clicked = true }) {
            Text("Test Button")
        }
    }
    
    composeTestRule
        .onNodeWithText("Test Button")
        .performClick()
        .assertExists()
}
```

## Best Practices

### Do's

- Use for secondary actions that need prominence
- Maintain consistent elevation values across similar actions
- Consider light/dark theme elevation visibility
- Test shadow visibility on different backgrounds

### Don'ts

- Don't use excessive elevation (>8dp typically)
- Don't mix elevation and tonal variants inconsistently
- Don't rely solely on elevation for critical distinctions
- Don't forget disabled state elevation (should be 0dp)

### Accessibility Considerations

```kotlin
@Composable
fun AccessibleElevatedButton() {
    ElevatedButton(
        onClick = { /* Handle action */ },
        modifier = Modifier.semantics {
            contentDescription = "Save your draft document"
            role = Role.Button
        }
    ) {
        Icon(Icons.Default.Save, contentDescription = null)
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text("Save Draft")
    }
}
```

### Common Patterns

```kotlin
@Composable
fun ElevatedButtonShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Standard secondary action
        ElevatedButton(onClick = { }) {
            Text("View Details")
        }
        
        // With semantic icon
        ElevatedButton(onClick = { }) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Download")
        }
        
        // Success state
        ElevatedButton(
            onClick = { },
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Completed")
        }
    }
}
```