# IconButton - Jetpack Compose

## Theory & Interview Knowledge

### What is IconButton?

IconButton is a specialized button component in Jetpack Compose designed specifically for icon-only interactions. It provides a circular touch target around an icon, following Material Design guidelines for icon-based actions.

### Material Design 3 Principles

- **Purpose**: Actions that can be clearly represented by icons alone
- **Touch Target**: 48dp minimum touch area regardless of icon size
- **Visual Weight**: Minimal - no background or border by default
- **Emphasis**: Low to medium depending on context
- **Space Efficiency**: Compact representation of actions

### Key Characteristics

- Circular ripple effect on interaction
- Standard 48dp touch target
- No background by default
- Supports Material icons and custom icons
- Can be used in app bars, toolbars, and content areas
- Automatic content color handling

### IconButton Variants

Jetpack Compose Material 3 provides several IconButton variants:

1. **IconButton** - Standard icon button
2. **IconToggleButton** - Toggle between two states
3. **FilledIconButton** - With filled background
4. **FilledTonalIconButton** - With tonal background
5. **OutlinedIconButton** - With border outline

## Core Implementation

### Basic IconButton

```kotlin
@Composable
fun BasicIconButton() {
    IconButton(onClick = { /* Handle click */ }) {
        Icon(
            Icons.Default.Favorite,
            contentDescription = "Add to favorites"
        )
    }
}
```

### IconButton with Custom Size

```kotlin
@Composable
fun CustomSizeIconButton() {
    IconButton(
        onClick = { /* Handle click */ },
        modifier = Modifier.size(32.dp) // Custom button size
    ) {
        Icon(
            Icons.Default.Close,
            contentDescription = "Close",
            modifier = Modifier.size(16.dp) // Custom icon size
        )
    }
}
```

### IconButton with Colors

```kotlin
@Composable
fun ColoredIconButton() {
    IconButton(onClick = { /* Handle click */ }) {
        Icon(
            Icons.Default.Delete,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.error
        )
    }
}
```

## IconButton Variants

### FilledIconButton

```kotlin
@Composable
fun FilledIconButtonExample() {
    FilledIconButton(
        onClick = { /* Handle click */ },
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Add item"
        )
    }
}
```

### FilledTonalIconButton

```kotlin
@Composable
fun FilledTonalIconButtonExample() {
    FilledTonalIconButton(
        onClick = { /* Handle click */ }
    ) {
        Icon(
            Icons.Default.Settings,
            contentDescription = "Settings"
        )
    }
}
```

### OutlinedIconButton

```kotlin
@Composable
fun OutlinedIconButtonExample() {
    OutlinedIconButton(
        onClick = { /* Handle click */ },
        border = BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.outline
        )
    ) {
        Icon(
            Icons.Default.Edit,
            contentDescription = "Edit"
        )
    }
}
```

### IconToggleButton

```kotlin
@Composable
fun IconToggleButtonExample() {
    var isChecked by remember { mutableStateOf(false) }
    
    IconToggleButton(
        checked = isChecked,
        onCheckedChange = { isChecked = it }
    ) {
        Icon(
            if (isChecked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isChecked) "Remove from favorites" else "Add to favorites",
            tint = if (isChecked) Color.Red else MaterialTheme.colorScheme.onSurface
        )
    }
}
```

## Advanced Patterns

### Multi-State IconButton

```kotlin
enum class PlayState { Playing, Paused, Stopped }

@Composable
fun MediaControlIconButton() {
    var playState by remember { mutableStateOf(PlayState.Stopped) }
    
    IconButton(
        onClick = {
            playState = when (playState) {
                PlayState.Stopped -> PlayState.Playing
                PlayState.Playing -> PlayState.Paused
                PlayState.Paused -> PlayState.Playing
            }
        }
    ) {
        val (icon, description) = when (playState) {
            PlayState.Stopped -> Icons.Default.PlayArrow to "Play"
            PlayState.Playing -> Icons.Default.Pause to "Pause"
            PlayState.Paused -> Icons.Default.PlayArrow to "Resume"
        }
        
        Icon(
            icon,
            contentDescription = description
        )
    }
}
```

### Loading IconButton

```kotlin
@Composable
fun LoadingIconButton() {
    var isLoading by remember { mutableStateOf(false) }
    
    IconButton(
        onClick = { 
            isLoading = true
            // Simulate async operation
        },
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Refresh"
            )
        }
    }
    
    // Auto-reset loading state
    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(2000)
            isLoading = false
        }
    }
}
```

### Animated IconButton

```kotlin
@Composable
fun AnimatedIconButton() {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300)
    )
    
    IconButton(
        onClick = { isExpanded = !isExpanded }
    ) {
        Icon(
            Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            modifier = Modifier.rotate(rotationAngle)
        )
    }
}
```

## Accessibility Implementation

### Proper Content Descriptions

```kotlin
@Composable
fun AccessibleIconButton() {
    var isMuted by remember { mutableStateOf(false) }
    
    IconButton(
        onClick = { isMuted = !isMuted },
        modifier = Modifier.semantics {
            contentDescription = if (isMuted) {
                "Unmute audio"
            } else {
                "Mute audio"
            }
            stateDescription = if (isMuted) "Muted" else "Unmuted"
        }
    ) {
        Icon(
            if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
            contentDescription = null // Handled by semantics
        )
    }
}
```

### Custom Semantics Actions

```kotlin
@Composable
fun CustomSemanticsIconButton() {
    var count by remember { mutableIntStateOf(0) }
    
    IconButton(
        onClick = { count++ },
        modifier = Modifier.semantics {
            contentDescription = "Like button, currently $count likes"
            customActions = listOf(
                CustomAccessibilityAction("Reset likes") {
                    count = 0
                    true
                }
            )
        }
    ) {
        Badge(
            badgeContent = { Text("$count") }
        ) {
            Icon(
                Icons.Default.ThumbUp,
                contentDescription = null
            )
        }
    }
}
```

## Common Use Cases & Examples

### App Bar Actions

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBarWithIconButtons() {
    TopAppBar(
        title = { Text("My App") },
        actions = {
            IconButton(onClick = { /* Search */ }) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search"
                )
            }
            IconButton(onClick = { /* More options */ }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More options"
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = { /* Navigate back */ }) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Navigate back"
                )
            }
        }
    )
}
```

### Media Controls

```kotlin
@Composable
fun MediaControlBar() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { /* Previous track */ }) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Previous track"
            )
        }
        
        FilledIconButton(
            onClick = { /* Play/Pause */ },
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                modifier = Modifier.size(32.dp)
            )
        }
        
        IconButton(onClick = { /* Next track */ }) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Next track"
            )
        }
    }
}
```

### Social Actions

```kotlin
@Composable
fun SocialActionBar() {
    var isLiked by remember { mutableStateOf(false) }
    var isBookmarked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableIntStateOf(42) }
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Like button
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconToggleButton(
                checked = isLiked,
                onCheckedChange = { 
                    isLiked = it
                    likeCount += if (it) 1 else -1
                }
            ) {
                Icon(
                    if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isLiked) "Unlike" else "Like",
                    tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface
                )
            }
            Text("$likeCount")
        }
        
        // Share button
        IconButton(onClick = { /* Share */ }) {
            Icon(
                Icons.Default.Share,
                contentDescription = "Share"
            )
        }
        
        // Bookmark button
        IconToggleButton(
            checked = isBookmarked,
            onCheckedChange = { isBookmarked = it }
        ) {
            Icon(
                if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark",
                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
```

### Form Actions

```kotlin
@Composable
fun FormWithIconButtons() {
    var text by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Text field with clear button
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Search") },
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(onClick = { text = "" }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear text"
                        )
                    }
                }
            }
        )
        
        // Password field with visibility toggle
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Password") },
            visualTransformation = if (isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                    )
                }
            }
        )
    }
}
```

## Styling & Customization

### Size Variations

```kotlin
@Composable
fun IconButtonSizes() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Small icon button
        IconButton(
            onClick = { },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add",
                modifier = Modifier.size(16.dp)
            )
        }
        
        // Standard icon button (48dp)
        IconButton(onClick = { }) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add"
            )
        }
        
        // Large icon button
        IconButton(
            onClick = { },
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
```

### Custom Colors and Themes

```kotlin
@Composable
fun ThemedIconButtons() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Primary themed
        FilledIconButton(
            onClick = { },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Default.Star, contentDescription = "Primary")
        }
        
        // Secondary themed
        FilledTonalIconButton(
            onClick = { },
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(Icons.Default.Favorite, contentDescription = "Secondary")
        }
        
        // Error themed
        FilledIconButton(
            onClick = { },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}
```

## Interview Questions & Answers

### Q1: When should you use IconButton instead of Button?

**Answer**: Use IconButton when:

- The action can be clearly understood from an icon alone
- You need to save space in the UI
- Building toolbars, app bars, or action rows
- The icon is universally recognizable (save, delete, share, etc.)
- You need consistent 48dp touch targets for accessibility

### Q2: What's the difference between IconButton and FloatingActionButton?

**Answer**:

- **IconButton**: Standard touch target, no elevation, used for secondary actions, can be grouped
- **FloatingActionButton**: Elevated, prominent, used for primary actions, typically single instance
- **Use Case**: IconButton for toolbar actions, FAB for main screen actions
- **Visual Hierarchy**: FAB has higher emphasis and visual weight

### Q3: How do you ensure accessibility in IconButton?

**Answer**:

- Always provide `contentDescription` for the Icon
- Use minimum 48dp touch target (default behavior)
- Add semantic descriptions for complex states
- Use `stateDescription` for toggle buttons
- Ensure sufficient color contrast
- Test with screen readers

### Q4: What are the variants of IconButton in Material 3?

**Answer**:

- **IconButton**: Standard, no background
- **FilledIconButton**: With filled background
- **FilledTonalIconButton**: With tonal background
- **OutlinedIconButton**: With border outline
- **IconToggleButton**: For toggle states

### Q5: How do you handle loading states in IconButton?

**Answer**: Replace icon content conditionally:

```kotlin
IconButton(enabled = !isLoading) {
    if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
    } else {
        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
    }
}
```

### Q6: How do you create an animated IconButton?

**Answer**: Use animation APIs with state changes:

```kotlin
val rotationAngle by animateFloatAsState(
    targetValue = if (isExpanded) 180f else 0f
)
IconButton(onClick = { isExpanded = !isExpanded }) {
    Icon(
        Icons.Default.ExpandMore,
        contentDescription = "Toggle",
        modifier = Modifier.rotate(rotationAngle)
    )
}
```

## Best Practices

### Do's ✅

- Always provide clear `contentDescription` for accessibility
- Use universally recognizable icons
- Maintain consistent icon sizes within the same context
- Follow Material Design icon guidelines
- Use appropriate IconButton variant for visual hierarchy
- Group related icon actions together
- Provide feedback for state changes (visual/haptic)

### Don'ts ❌

- Don't use obscure icons without additional context
- Don't make icons too small (minimum 24dp for icons)
- Don't ignore accessibility requirements
- Don't use too many icon buttons in a single row
- Don't use IconButton for primary actions (use FAB instead)
- Don't forget to handle loading/disabled states
- Don't use inconsistent icon styles in the same UI

## Performance Considerations

- IconButton has minimal rendering overhead
- Vector icons (Material Icons) scale without quality loss
- Use `remember` for click handlers to avoid recomposition
- Consider using `LazyRow` for many icon buttons
- Cache icon content when using custom icons
- Prefer Material Icons for better performance

## Common Pitfalls

1. **Missing Accessibility**: Not providing contentDescription
2. **Inconsistent Sizing**: Using different icon sizes in same context
3. **Poor Icon Choice**: Using unclear or non-standard icons
4. **Touch Target Issues**: Making buttons too small
5. **State Management**: Not handling loading/disabled states properly
6. **Visual Hierarchy**: Using wrong IconButton variant for emphasis level

## Testing Considerations

### Unit Testing

```kotlin
@Test
fun iconButton_click_triggersCallback() {
    var clicked = false
    composeTestRule.setContent {
        IconButton(onClick = { clicked = true }) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }
    
    composeTestRule.onNodeWithContentDescription("Add")
        .performClick()
    
    assertTrue(clicked)
}
```

### Accessibility Testing

```kotlin
@Test
fun iconButton_hasProperContentDescription() {
    composeTestRule.setContent {
        IconButton(onClick = { }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete item")
        }
    }
    
    composeTestRule.onNodeWithContentDescription("Delete item")
        .assertExists()
        .assertHasClickAction()
}
```

## Integration with Other Components

### With Badge

```kotlin
@Composable
fun IconButtonWithBadge() {
    var notificationCount by remember { mutableIntStateOf(3) }
    
    IconButton(onClick = { notificationCount = 0 }) {
        Badge(
            badgeContent = { 
                Text("$notificationCount") 
            }
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "Notifications ($notificationCount unread)"
            )
        }
    }
}
```

### With Tooltip

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconButtonWithTooltip() {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text("Add new item")
            }
        },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = { /* Add item */ }) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add new item"
            )
        }
    }
}
```

### In Lists

```kotlin
@Composable
fun ListItemWithIconButtons() {
    LazyColumn {
        items(10) { index ->
            ListItem(
                headlineContent = { Text("Item $index") },
                supportingContent = { Text("Supporting text") },
                trailingContent = {
                    Row {
                        IconButton(onClick = { /* Edit */ }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit item $index"
                            )
                        }
                        IconButton(onClick = { /* Delete */ }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete item $index",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    }
}
```

## Custom IconButton Implementations

### Expandable IconButton

```kotlin
@Composable
fun ExpandableIconButton(
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300)
    )
    
    IconButton(
        onClick = { onExpandChange(!isExpanded) },
        modifier = modifier
    ) {
        Icon(
            Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            modifier = Modifier.rotate(rotationAngle)
        )
    }
}
```

### Counter IconButton

```kotlin
@Composable
fun CounterIconButton(
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onDecrement,
            enabled = count > 0
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = "Decrease"
            )
        }
        
        Text(
            text = count.toString(),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        
        IconButton(onClick = onIncrement) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Increase"
            )
        }
    }
}
```

### Confirmation IconButton

```kotlin
@Composable
fun ConfirmationIconButton(
    onConfirm: () -> Unit,
    confirmationRequired: Boolean = true,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    var showConfirmation by remember { mutableStateOf(false) }
    
    if (showConfirmation && confirmationRequired) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Confirm Action") },
            text = { Text("Are you sure you want to proceed?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm()
                        showConfirmation = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    IconButton(
        onClick = {
            if (confirmationRequired) {
                showConfirmation = true
            } else {
                onConfirm()
            }
        },
        modifier = modifier
    ) {
        Icon(
            icon,
            contentDescription = contentDescription
        )
    }
}
```

## Related Components

- [[TextButton]] - Text-only button with low emphasis
- [[FloatingActionButton]] - Primary floating action button
- [[Button]] - Primary filled button
- [[TopAppBar]] - Contains navigation and action IconButtons
- [[NavigationBar]] - Uses IconButtons for navigation items