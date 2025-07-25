# FilledTonalButton

## Theory & Interview Knowledge

### Purpose & Design Philosophy

- **Middle Ground**: Between filled and outlined buttons in visual hierarchy
- **Tonal Approach**: Uses color containers instead of elevation for emphasis
- **Dark Theme Friendly**: Works better than elevation in dark themes
- **Subtle Emphasis**: Provides background fill without high contrast

### When to Use FilledTonalButton

✅ **Good Use Cases:**

- Secondary actions that need background emphasis
- Dark themes where elevation is less effective
- Multiple secondary actions that need similar treatment
- Complementary actions alongside primary buttons
- Filter buttons, category selectors

❌ **Avoid When:**

- Primary actions (use Button instead)
- Actions needing maximum prominence
- Single isolated actions (consider other variants)
- When brand colors don't support tonal containers

### Technical Implementation

```kotlin
@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
)
```

### Color System Deep Dive

**Material 3 Color Mapping:**

```kotlin
ButtonDefaults.filledTonalButtonColors(
    containerColor = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
)
```

**Container vs Surface Colors:**

- **Container Colors**: Provide subtle background tint
- **onContainer Colors**: Ensure proper contrast for content
- **Tonal Variants**: Primary, Secondary, Tertiary containers available

## Practical Examples

### Basic Implementation

```kotlin
@Composable
fun BasicFilledTonalExample() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Standard tonal button
        FilledTonalButton(onClick = { /* Handle click */ }) {
            Text("Edit Profile")
        }
        
        // With icon
        FilledTonalButton(onClick = { /* Handle click */ }) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Settings")
        }
        
        // Disabled state
        FilledTonalButton(
            onClick = { /* Handle click */ },
            enabled = false
        ) {
            Text("Coming Soon")
        }
    }
}
```

### Color Container Variants

```kotlin
@Composable
fun TonalButtonColorVariants() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Secondary container (default)
        FilledTonalButton(onClick = { }) {
            Text("Secondary")
        }
        
        // Primary container
        FilledTonalButton(
            onClick = { },
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Text("Primary Tonal")
        }
        
        // Tertiary container
        FilledTonalButton(
            onClick = { },
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        ) {
            Text("Tertiary Tonal")
        }
        
        // Error container
        FilledTonalButton(
            onClick = { },
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text("Error Tonal")
        }
    }
}
```

### Toggle Button Pattern

```kotlin
@Composable
fun TonalToggleButtons() {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Active", "Complete", "Pending")
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(filters) { filter ->
            val isSelected = selectedFilter == filter
            
            FilledTonalButton(
                onClick = { selectedFilter = filter },
                colors = if (isSelected) {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    ButtonDefaults.filledTonalButtonColors()
                }
            ) {
                Text(filter)
            }
        }
    }
}
```

### Custom Shape Examples

```kotlin
@Composable
fun TonalButtonShapes() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Default rounded
        FilledTonalButton(onClick = { }) {
            Text("Default")
        }
        
        // Pill shape
        FilledTonalButton(
            onClick = { },
            shape = RoundedCornerShape(50)
        ) {
            Text("Pill Shape")
        }
        
        // Custom corners
        FilledTonalButton(
            onClick = { },
            shape = RoundedCornerShape(
                topStart = 16.dp,
                bottomEnd = 16.dp
            )
        ) {
            Text("Custom Corners")
        }
        
        // Cut corners
        FilledTonalButton(
            onClick = { },
            shape = CutCornerShape(8.dp)
        ) {
            Text("Cut Corners")
        }
    }
}
```

### Loading State with Tonal Style

```kotlin
@Composable  
fun LoadingTonalButton() {
    var isProcessing by remember { mutableStateOf(false) }
    
    FilledTonalButton(
        onClick = {
            if (!isProcessing) {
                isProcessing = true
            }
        },
        enabled = !isProcessing,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isProcessing) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        AnimatedContent(
            targetState = isProcessing,
            transitionSpec = { fadeIn() with fadeOut() }
        ) { loading ->
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Processing...")
                }
            } else {
                Text("Process Data")
            }
        }
    }
    
    // Auto-reset
    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            delay(3000)
            isProcessing = false
        }
    }
}
```

## Interview Questions & Answers

**Q: What's the difference between FilledTonalButton and ElevatedButton?**

- **Visual Approach**: Tonal uses color containers, Elevated uses shadows
- **Theme Adaptation**: Tonal works better in dark themes
- **Accessibility**: Tonal relies on color contrast, Elevated on depth perception
- **Performance**: Tonal has less rendering overhead than elevation

**Q: When would you choose tonal over filled buttons?**

- **Secondary Actions**: When you need background emphasis but not primary-level attention
- **Multiple Options**: When presenting several similar-weight choices
- **Dark Themes**: Where tonal containers are more visible than elevation
- **Brand Consistency**: When filled buttons are reserved for CTAs

**Q: How do container colors work in Material 3?**

```kotlin
// Container colors provide semantic meaning
primaryContainer      // For primary-related tonal buttons
secondaryContainer    // For secondary actions (default)
tertiaryContainer     // For supporting actions
errorContainer        // For error-related actions
surfaceVariant        // For neutral actions
```

**Q: How do you test tonal button interactions?**

```kotlin
@Test
fun tonalButtonStateTest() {
    var clicked by mutableStateOf(false)
    
    composeTestRule.setContent {
        FilledTonalButton(
            onClick = { clicked = true }
        ) {
            Text("Tonal Button")
        }
    }
    
    composeTestRule
        .onNodeWithText("Tonal Button")
        .performClick()
    
    assert(clicked)
}
```

## Best Practices

### Design Guidelines

```kotlin
@Composable
fun TonalButtonBestPractices() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // ✅ Good: Consistent container usage
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { }) { Text("Primary Action") }
            FilledTonalButton(onClick = { }) { Text("Secondary") }
            OutlinedButton(onClick = { }) { Text("Alternative") }
        }
        
        // ✅ Good: Filter/category pattern
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(listOf("Design", "Development", "Marketing")) { category ->
                FilledTonalButton(
                    onClick = { },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(category, fontSize = 12.sp)
                }
            }
        }
        
        // ✅ Good: Settings/configuration actions
        FilledTonalButton(
            onClick = { },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Open Settings")
        }
    }
}
```

### Accessibility Implementation

```kotlin
@Composable
fun AccessibleTonalButton() {
    var isSelected by remember { mutableStateOf(false) }
    
    FilledTonalButton(
        onClick = { isSelected = !isSelected },
        modifier = Modifier.semantics {
            contentDescription = "Filter by favorite items"
            stateDescription = if (isSelected) "Selected" else "Not selected"
            role = Role.Button
            toggleableState = ToggleableState(isSelected)
        },
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Icon(
            if (isSelected) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = null
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text("Favorites")
    }
}
```

### Common Patterns

- **Filter Chips**: Small tonal buttons for filtering content
- **Category Selectors**: Multiple options with tonal feedback
- **Secondary CTAs**: Actions that support primary workflows
- **Feature Toggles**: On/off states with visual feedback
- **Navigation Aids**: Section switchers, view toggles

### Performance Notes

- Lower rendering cost than elevated buttons
- Efficient color animations built-in
- Container colors automatically adapt to theme changes
- Minimal memory footprint compared to complex visual effects