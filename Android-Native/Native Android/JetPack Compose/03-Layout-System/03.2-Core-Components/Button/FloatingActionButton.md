# FloatingActionButton - Jetpack Compose

## Theory & Interview Knowledge

### What is FloatingActionButton (FAB)?

FloatingActionButton is a prominent, elevated button that floats above the content, representing the **primary action** in an application. It follows Material Design 3 principles for promoting the most important user action on a screen.

### Material Design 3 Principles

- **Primary Action**: Represents the main action users should take
- **Elevation**: Floats above content with shadow/elevation
- **Positioning**: Typically bottom-right corner of screen
- **Emphasis**: Highest visual emphasis among all buttons
- **Accessibility**: Large touch target for easy interaction
- **Single Instance**: Usually one FAB per screen

### Key Characteristics

- Circular shape (except ExtendedFAB)
- High elevation and shadow
- Large touch target (56dp standard)
- Contains icon and/or text
- Anchored to screen edge or specific components
- Animated entrance/exit transitions

### FAB Variants in Material 3

1. **FloatingActionButton** - Standard FAB (56dp)
2. **SmallFloatingActionButton** - Compact FAB (40dp)
3. **LargeFloatingActionButton** - Large FAB (96dp)
4. **ExtendedFloatingActionButton** - With text label

### When to Use FAB

- Creating new content (compose email, add item)
- Primary action that's used frequently
- Action that spans multiple screens
- Promoting a specific feature or capability

## Core Implementation

### Basic FloatingActionButton

```kotlin
@Composable
fun BasicFAB() {
    FloatingActionButton(
        onClick = { /* Handle primary action */ }
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Add new item"
        )
    }
}
```

### FAB with Custom Colors

```kotlin
@Composable
fun CustomColorFAB() {
    FloatingActionButton(
        onClick = { /* Handle action */ },
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary
    ) {
        Icon(
            Icons.Default.Edit,
            contentDescription = "Edit"
        )
    }
}
```

### FAB with Custom Elevation

```kotlin
@Composable
fun CustomElevationFAB() {
    FloatingActionButton(
        onClick = { /* Handle action */ },
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 12.dp,
            pressedElevation = 16.dp,
            hoveredElevation = 14.dp,
            focusedElevation = 14.dp
        )
    ) {
        Icon(
            Icons.Default.Star,
            contentDescription = "Favorite"
        )
    }
}
```

## FAB Size Variants

### SmallFloatingActionButton

```kotlin
@Composable
fun SmallFABExample() {
    SmallFloatingActionButton(
        onClick = { /* Handle secondary action */ }
    ) {
        Icon(
            Icons.Default.Edit,
            contentDescription = "Edit",
            modifier = Modifier.size(18.dp)
        )
    }
}
```

### LargeFloatingActionButton

```kotlin
@Composable
fun LargeFABExample() {
    LargeFloatingActionButton(
        onClick = { /* Handle primary action */ }
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Create new",
            modifier = Modifier.size(36.dp)
        )
    }
}
```

### All FAB Sizes Comparison

```kotlin
@Composable
fun FABSizeComparison() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Small FAB (40dp)
        SmallFloatingActionButton(onClick = { }) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit",
                modifier = Modifier.size(18.dp)
            )
        }
        
        // Standard FAB (56dp)
        FloatingActionButton(onClick = { }) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add"
            )
        }
        
        // Large FAB (96dp)
        LargeFloatingActionButton(onClick = { }) {
            Icon(
                Icons.Default.Create,
                contentDescription = "Create",
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
```

## ExtendedFloatingActionButton

### Basic Extended FAB

```kotlin
@Composable
fun BasicExtendedFAB() {
    ExtendedFloatingActionButton(
        onClick = { /* Handle action */ },
        icon = {
            Icon(
                Icons.Default.Add,
                contentDescription = null
            )
        },
        text = { Text("Create New") }
    )
}
```

### Text-Only Extended FAB

```kotlin
@Composable
fun TextOnlyExtendedFAB() {
    ExtendedFloatingActionButton(
        onClick = { /* Handle action */ },
        text = { Text("Get Started") }
    )
}
```

### Extended FAB with State

```kotlin
@Composable
fun StatefulExtendedFAB() {
    var isExpanded by remember { mutableStateOf(true) }
    
    ExtendedFloatingActionButton(
        onClick = { /* Handle action */ },
        expanded = isExpanded,
        icon = {
            Icon(
                Icons.Default.Add,
                contentDescription = null
            )
        },
        text = { Text("Create") }
    )
    
    // Control expansion based on scroll state
    // isExpanded = !scrollState.isScrollInProgress
}
```

## Advanced FAB Patterns

### Animated FAB with State Changes

```kotlin
@Composable
fun AnimatedStateFAB() {
    var fabState by remember { mutableStateOf(FABState.Add) }
    
    FloatingActionButton(
        onClick = {
            fabState = when (fabState) {
                FABState.Add -> FABState.Done
                FABState.Done -> FABState.Add
            }
        }
    ) {
        AnimatedContent(
            targetState = fabState,
            transitionSpec = {
                fadeIn(tween(300)) + scaleIn(tween(300)) with
                fadeOut(tween(300)) + scaleOut(tween(300))
            }
        ) { state ->
            when (state) {
                FABState.Add -> Icon(
                    Icons.Default.Add,
                    contentDescription = "Add item"
                )
                FABState.Done -> Icon(
                    Icons.Default.Check,
                    contentDescription = "Mark as done"
                )
            }
        }
    }
}

enum class FABState { Add, Done }
```

### Multi-Action FAB Menu

```kotlin
@Composable
fun MultiFABMenu() {
    var isExpanded by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Background overlay
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { isExpanded = false }
            )
        }
        
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            // Sub-actions
            AnimatedVisibility(
                visible = isExpanded,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 }
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it / 2 }
                ) + fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Camera FAB
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                "Camera",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        SmallFloatingActionButton(
                            onClick = { 
                                isExpanded = false
                                // Handle camera action
                            },
                            containerColor = MaterialTheme.colorScheme.secondary
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Camera")
                        }
                    }
                    
                    // Gallery FAB
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                "Gallery",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        SmallFloatingActionButton(
                            onClick = { 
                                isExpanded = false
                                // Handle gallery action
                            },
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                        }
                    }
                }
            }
            
            // Main FAB
            FloatingActionButton(
                onClick = { isExpanded = !isExpanded }
            ) {
                AnimatedContent(
                    targetState = isExpanded,
                    transitionSpec = {
                        fadeIn(tween(200)) + scaleIn(tween(200)) with
                        fadeOut(tween(200)) + scaleOut(tween(200))
                    }
                ) { expanded ->
                    if (expanded) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    } else {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            }
        }
    }
}
```

### FAB with Loading State

```kotlin
@Composable
fun LoadingFAB() {
    var isLoading by remember { mutableStateOf(false) }
    
    FloatingActionButton(
        onClick = { 
            isLoading = true
            // Simulate async operation
        },
        containerColor = if (isLoading) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.primary
        }
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Icon(
                Icons.Default.CloudUpload,
                contentDescription = "Upload"
            )
        }
    }
    
    // Auto-reset loading state
    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(3000)
            isLoading = false
        }
    }
}
```

## Scaffold Integration

### FAB with Scaffold

```kotlin
@Composable
fun ScaffoldWithFAB() {
    var itemCount by remember { mutableIntStateOf(0) }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { itemCount++ }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add item"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier.fillMaxSize()
        ) {
            items(itemCount) { index ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Item ${index + 1}",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
```

### Hide/Show FAB on Scroll

```kotlin
@Composable
fun ScrollAwareFAB() {
    val listState = rememberLazyListState()
    var fabVisible by remember { mutableStateOf(true) }
    
    // Hide FAB when scrolling down, show when scrolling up
    LaunchedEffect(listState) {
        snapshotFlow { 
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset 
        }.collect { (index, offset) ->
            fabVisible = index == 0 && offset == 0
        }
    }
    
    Scaffold(
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                FloatingActionButton(
                    onClick = { /* Add new item */ }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            contentPadding = paddingValues,
            modifier = Modifier.fillMaxSize()
        ) {
            items(100) { index ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Item $index",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
```

### Extended FAB with Scroll Behavior

```kotlin
@Composable
fun ExtendedFABWithScroll() {
    val listState = rememberLazyListState()
    val isScrollingUp by remember {
        derivedStateOf {
            listState.firstVisibleItemScrollOffset < 100
        }
    }
    
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* Compose action */ },
                expanded = isScrollingUp,
                icon = {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null
                    )
                },
                text = { Text("Compose") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            contentPadding = paddingValues,
            modifier = Modifier.fillMaxSize()
        ) {
            items(50) { index ->
                ListItem(
                    headlineContent = { Text("Email $index") },
                    supportingContent = { Text("Email preview text...") }
                )
            }
        }
    }
}
```

## Accessibility Implementation

### Proper Semantic Descriptions

```kotlin
@Composable
fun AccessibleFAB() {
    FloatingActionButton(
        onClick = { /* Create new email */ },
        modifier = Modifier.semantics {
            contentDescription = "Compose new email"
            role = Role.Button
        }
    ) {
        Icon(
            Icons.Default.Edit,
            contentDescription = null // Handled by semantics
        )
    }
}
```

### FAB with State Descriptions

```kotlin
@Composable
fun StatefulAccessibleFAB() {
    var isRecording by remember { mutableStateOf(false) }
    
    FloatingActionButton(
        onClick = { isRecording = !isRecording },
        modifier = Modifier.semantics {
            contentDescription = if (isRecording) {
                "Stop recording"
            } else {
                "Start recording"
            }
            stateDescription = if (isRecording) "Recording" else "Not recording"
        },
        containerColor = if (isRecording) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }
    ) {
        Icon(
            if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
            contentDescription = null
        )
    }
}
```

## Common Use Cases & Examples

### Email App FAB

```kotlin
@Composable
fun EmailAppFAB() {
    ExtendedFloatingActionButton(
        onClick = { /* Navigate to compose screen */ },
        icon = {
            Icon(
                Icons.Default.Edit,
                contentDescription = null
            )
        },
        text = { Text("Compose") },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
}
```

### Notes App FAB

```kotlin
@Composable
fun NotesAppFAB() {
    FloatingActionButton(
        onClick = { /* Create new note */ }
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Create new note"
        )
    }
}
```

### Camera App FAB

```kotlin
@Composable
fun CameraAppFAB() {
    LargeFloatingActionButton(
        onClick = { /* Take photo */ },
        containerColor = Color.White,
        contentColor = Color.Black
    ) {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = "Take photo",
            modifier = Modifier.size(36.dp)
        )
    }
}
```

### Shopping App FAB

```kotlin
@Composable
fun ShoppingAppFAB() {
    var itemsInCart by remember { mutableIntStateOf(0) }
    
    FloatingActionButton(
        onClick = { itemsInCart++ }
    ) {
        Badge(
            badgeContent = { 
                Text("$itemsInCart") 
            }
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = "Add to cart ($itemsInCart items)"
            )
        }
    }
}
```

## Styling & Customization

### Custom Shapes

```kotlin
@Composable
fun CustomShapeFAB() {
    FloatingActionButton(
        onClick = { },
        shape = RoundedCornerShape(16.dp) // Less rounded
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Add"
        )
    }
}
```

### Themed FAB Variants

```kotlin
@Composable
fun ThemedFABVariants() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Primary FAB
        FloatingActionButton(
            onClick = { },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Primary")
        }
        
        // Secondary FAB
        FloatingActionButton(
            onClick = { },
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Secondary")
        }
        
        // Tertiary FAB
        FloatingActionButton(
            onClick = { },
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary
        ) {
            Icon(Icons.Default.Star, contentDescription = "Tertiary")
        }
        
        // Surface FAB
        FloatingActionButton(
            onClick = { },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Surface")
        }
    }
}
```

## Interview Questions & Answers

### Q1: When should you use FloatingActionButton vs regular Button?

**Answer**: Use FAB when:

- Representing the primary action on a screen
- Action is used frequently and should be easily accessible
- Action spans multiple screens or contexts
- Need prominent visual emphasis
- Following Material Design patterns for specific app types (email compose, add content)

Use regular Button for:

- Actions within forms or dialogs
- Secondary or tertiary actions
- Actions that are contextual to specific content
- When you need multiple action buttons

### Q2: What are the different FAB sizes and when to use each?

**Answer**:

- **SmallFloatingActionButton (40dp)**: Secondary actions, compact spaces, sub-actions in multi-FAB patterns
- **FloatingActionButton (56dp)**: Standard primary action, most common use case
- **LargeFloatingActionButton (96dp)**: Very prominent primary actions, camera shutter buttons, when extra emphasis is needed
- **ExtendedFloatingActionButton**: When the action needs text clarification, during onboarding, or when icon alone isn't clear

### Q3: How do you implement a multi-action FAB menu?

**Answer**: Use state management with animated visibility:

```kotlin
var isExpanded by remember { mutableStateOf(false) }
// Show/hide sub-FABs based on isExpanded state
// Use AnimatedVisibility for smooth transitions
// Include background overlay for dismissing menu
// Use smaller FABs for sub-actions
```

### Q4: How do you handle FAB visibility during scrolling?

**Answer**: Monitor scroll state and animate FAB visibility:

```kotlin
val listState = rememberLazyListState()
var fabVisible by remember { mutableStateOf(true) }

LaunchedEffect(listState) {
    snapshotFlow { listState.firstVisibleItemScrollOffset }
        .collect { offset ->
            fabVisible = offset < 100 // Show when near top
        }
}
```

### Q5: What accessibility considerations are important for FAB?

**Answer**:

- Always provide clear `contentDescription`
- Use semantic descriptions for different states
- Ensure proper contrast ratios
- Consider users with motor impairments (large touch target is good)
- Provide alternative ways to access the action
- Use appropriate state descriptions for toggleable FABs

### Q6: How do you integrate FAB with Scaffold properly?

**Answer**:

```kotlin
Scaffold(
    floatingActionButton = { /* FAB content */ },
    floatingActionButtonPosition = FabPosition.End, // or Center
    content = { paddingValues -> /* Content with padding */ }
)
```

- Use paddingValues to avoid content overlap
- Choose appropriate position based on content layout
- Consider snackbar interactions

# FloatingActionButton Best Practices

## Do's ✅

### Design & UX

- **Use FAB for the primary action** - The most important action users should take on that screen
- **One FAB per screen** - Maintain visual hierarchy and avoid confusion
- **Place in bottom-right corner** - Follow Material Design conventions for consistency
- **Use clear, recognizable icons** - Icons should be universally understood (Add, Edit, etc.)
- **Maintain consistent behavior** - Same action type across similar screens
- **Consider the user journey** - FAB action should make sense in the current context

### Implementation

- **Provide proper accessibility** - Always include contentDescription and semantic roles
- **Handle different states** - Loading, disabled, pressed states should be clear
- **Use appropriate elevation** - Follow Material Design elevation guidelines
- **Implement smooth animations** - For state changes, show/hide, and transitions
- **Test on different screen sizes** - Ensure FAB doesn't interfere with content
- **Consider scroll behavior** - Hide/show or transform based on user interaction

### Code Quality

- **Use remember for state management** - Proper state handling with `remember` and `mutableStateOf`
- **Implement proper error handling** - Handle onClick failures gracefully
- **Follow naming conventions** - Clear, descriptive function and variable names
- **Use appropriate FAB variant** - Choose right size (Small/Standard/Large/Extended) for context
- **Leverage Scaffold integration** - Use built-in positioning and layout support

## Don'ts ❌

### Design & UX

- **Don't use multiple FABs** - Avoid visual clutter and decision paralysis
- **Don't use for secondary actions** - Reserve for primary actions only
- **Don't place randomly** - Avoid non-standard positions without good reason
- **Don't use unclear icons** - Avoid abstract or confusing iconography
- **Don't make it too small** - Maintain accessibility and touch target guidelines
- **Don't ignore context** - Action should be relevant to current screen content

### Implementation

- **Don't forget accessibility** - Never skip contentDescription or semantic properties
- **Don't hardcode colors** - Use theme colors and respect system preferences
- **Don't ignore touch feedback** - Provide clear pressed/hover states
- **Don't block content** - Ensure FAB doesn't cover important UI elements
- **Don't use without proper navigation** - FAB actions should have clear user flow
- **Don't ignore loading states** - Show progress for long-running operations

### Performance

- **Don't create unnecessary recompositions** - Use stable state and proper key usage
- **Don't animate excessively** - Keep animations smooth but not distracting
- **Don't ignore memory leaks** - Properly handle LaunchedEffect and coroutines
- **Don't over-engineer** - Keep implementations simple and maintainable

## Advanced Best Practices

### State Management

```kotlin
// ✅ Good: Stable state management
var isLoading by remember { mutableStateOf(false) }

// ❌ Bad: Unstable state
var isLoading = mutableStateOf(false) // Creates new state on recomposition
```

### Accessibility Excellence

```kotlin
// ✅ Good: Complete accessibility
FloatingActionButton(
    onClick = { },
    modifier = Modifier.semantics {
        contentDescription = "Create new email"
        role = Role.Button
        stateDescription = if (isEnabled) "Available" else "Disabled"
    }
) { /* content */ }

// ❌ Bad: Missing accessibility
FloatingActionButton(onClick = { }) {
    Icon(Icons.Default.Add, contentDescription = null) // No description
}
```

### Performance Optimization

```kotlin
// ✅ Good: Stable callbacks
val onClick = remember { { /* stable callback */ } }
FloatingActionButton(onClick = onClick) { /* content */ }

// ❌ Bad: Inline lambdas causing recomposition
FloatingActionButton(onClick = { viewModel.performAction() }) { /* content */ }
```

### Error Handling

```kotlin
// ✅ Good: Proper error handling
FloatingActionButton(
    onClick = {
        try {
            onAction()
        } catch (e: Exception) {
            showErrorMessage(e.message)
        }
    }
) { /* content */ }
```

### Responsive Design

```kotlin
// ✅ Good: Responsive FAB sizing
@Composable
fun ResponsiveFAB() {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    
    if (isTablet) {
        LargeFloatingActionButton(onClick = { }) { /* content */ }
    } else {
        FloatingActionButton(onClick = { }) { /* content */ }
    }
}
```

## Testing Best Practices

### Unit Testing

- Test FAB click handlers independently
- Verify state changes trigger correct recomposition
- Test accessibility properties are set correctly
- Validate error handling in click actions

### UI Testing

- Test FAB visibility in different scroll states
- Verify animations complete properly
- Test FAB doesn't interfere with other UI elements
- Validate touch targets meet accessibility guidelines

### Integration Testing

- Test FAB integration with Scaffold
- Verify navigation flows from FAB actions
- Test FAB behavior across different screen orientations
- Validate theme changes affect FAB appearance correctly

## Real-World Application Examples

### Email App Pattern

```kotlin
// Primary action: Compose new email
ExtendedFloatingActionButton(
    onClick = { navigateToCompose() },
    icon = { Icon(Icons.Default.Edit, contentDescription = null) },
    text = { Text("Compose") }
)
```

### Social Media Pattern

```kotlin
// Primary action: Create new post
FloatingActionButton(
    onClick = { showCreatePostDialog() }
) {
    Icon(Icons.Default.Add, contentDescription = "Create new post")
}
```

### Notes App Pattern

```kotlin
// Primary action: Add new note
FloatingActionButton(
    onClick =
```