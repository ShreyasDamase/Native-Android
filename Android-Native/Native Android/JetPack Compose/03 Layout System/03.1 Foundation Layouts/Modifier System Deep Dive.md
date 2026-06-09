# Jetpack Compose Modifier Properties Guide

## Core Import

```kotlin
import androidx.compose.ui.Modifier
```

## Essential Modifier Properties

### Layout & Sizing

- **`fillMaxWidth()`** - Fill available width
- **`fillMaxHeight()`** - Fill available height
- **`fillMaxSize()`** - Fill both width and height
- **`width(dp)`** - Set specific width
- **`height(dp)`** - Set specific height
- **`size(dp)`** - Set both width and height to same value
- **`size(width.dp, height.dp)`** - Set different width and height
- **`wrapContentWidth()`** - Wrap content width
- **`wrapContentHeight()`** - Wrap content height
- **`wrapContentSize()`** - Wrap both dimensions

### Padding & Margins

- **`padding(dp)`** - Equal padding on all sides
- **`padding(horizontal = dp, vertical = dp)`** - Different horizontal/vertical padding
- **`padding(start = dp, top = dp, end = dp, bottom = dp)`** - Individual side padding
- **`paddingFromBaseline(top = dp, bottom = dp)`** - Padding from text baseline

### Positioning & Alignment

- **`align(alignment)`** - Align within parent (BoxScope, ColumnScope, RowScope)
- **`weight(float)`** - Distribute space proportionally (in Row/Column)
- **`offset(x = dp, y = dp)`** - Offset position
- **`absoluteOffset(x = dp, y = dp)`** - Absolute offset (ignores RTL)

### Background & Appearance

- **`background(color)`** - Set background color
- **`background(color, shape)`** - Background with shape
- **`alpha(float)`** - Set transparency (0f to 1f)
- **`clip(shape)`** - Clip to shape
- **`border(width, color)`** - Add border
- **`border(width, color, shape)`** - Border with shape
- **`shadow(elevation, shape)`** - Add shadow

### Interaction

- **`clickable { }`** - Make clickable
- **`clickable(enabled = boolean) { }`** - Clickable with enable state
- **`selectable(selected, onClick)`** - Selectable behavior
- **`toggleable(value, onValueChange)`** - Toggle behavior
- **`focusable()`** - Make focusable
- **`focusTarget()`** - Focus target

### Scrolling

- **`verticalScroll(scrollState)`** - Enable vertical scrolling
- **`horizontalScroll(scrollState)`** - Enable horizontal scrolling
- **`scrollable(state, orientation)`** - Custom scrollable behavior

### Transformation

- **`rotate(degrees)`** - Rotate element
- **`scale(scale)`** - Scale element uniformly
- **`scale(scaleX, scaleY)`** - Scale with different X/Y factors
- **`graphicsLayer { }`** - Advanced graphics transformations

### Conditional & Utility

- **`then(modifier)`** - Conditionally apply modifier
- **`requiredWidth(dp)`** - Required width (overrides parent constraints)
- **`requiredHeight(dp)`** - Required height (overrides parent constraints)
- **`requiredSize(dp)`** - Required size
- **`aspectRatio(ratio)`** - Maintain aspect ratio

## Common Usage Examples

### Text with Material3

```kotlin
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape

Text(
    text = "Hello World",
    modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .background(
            color = Color.LightGray,
            shape = RoundedCornerShape(8.dp)
        )
        .clickable { /* Handle click */ }
        .padding(12.dp) // Inner padding after background
)
```

### Button-like Text

```kotlin
Text(
    text = "Click Me",
    modifier = Modifier
        .size(width = 120.dp, height = 48.dp)
        .background(
            color = Color.Blue,
            shape = RoundedCornerShape(24.dp)
        )
        .clickable { /* Handle click */ }
        .wrapContentSize() // Center text inside
)
```

### Responsive Text in Column

```kotlin
Column {
    Text(
        text = "Title",
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f) // Takes available space
            .padding(horizontal = 16.dp)
    )
    
    Text(
        text = "Subtitle",
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .alpha(0.7f)
    )
}
```

## Import Guidelines

### Core Modifier

```kotlin
import androidx.compose.ui.Modifier
```

### Common Additional Imports

```kotlin
// For padding, size, etc.
import androidx.compose.foundation.layout.*

// For background, clickable, etc.
import androidx.compose.foundation.*

// For colors
import androidx.compose.ui.graphics.Color

// For shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape

// For alignment
import androidx.compose.ui.Alignment

// For units
import androidx.compose.ui.unit.dp
```

## Best Practices

### 1. Order Matters

Apply modifiers in logical order:

```kotlin
Modifier
    .size(100.dp)           // 1. Size first
    .background(Color.Blue) // 2. Background
    .padding(8.dp)          // 3. Padding
    .clickable { }          // 4. Interactions
```

### 2. Conditional Modifiers

```kotlin
Modifier
    .fillMaxWidth()
    .then(
        if (isSelected) {
            Modifier.background(Color.Blue)
        } else {
            Modifier
        }
    )
```

### 3. Extract Common Modifiers

```kotlin
val cardModifier = Modifier
    .fillMaxWidth()
    .padding(8.dp)
    .background(
        color = Color.White,
        shape = RoundedCornerShape(8.dp)
    )
    .shadow(4.dp, RoundedCornerShape(8.dp))

Text(
    text = "Card Content",
    modifier = cardModifier.clickable { }
)
```

## Common Mistakes to Avoid

1. **Wrong Import**: Make sure you're importing from the right package
2. **Order Issues**: Background before padding vs padding before background gives different results
3. **Overriding Constraints**: Using `required*` modifiers when not needed
4. **Performance**: Avoid creating new modifier chains in recomposition-heavy areas

## Advanced Modifier Properties

### Semantics & Accessibility

- **`semantics { }`** - Add semantic information for accessibility
- **`clearAndSetSemantics { }`** - Clear inherited semantics and set new ones
- **`testTag(string)`** - Add test tag for UI testing

### Animation & Transition

- **`animateContentSize()`** - Animate size changes
- **`placeholder(visible, highlight)`** - Show placeholder while loading

### Layout Behavior

- **`layoutId(id)`** - Assign layout ID for ConstraintLayout
- **`onGloballyPositioned { }`** - Callback when layout is positioned
- **`onSizeChanged { }`** - Callback when size changes

### Drawing & Canvas

- **`drawBehind { }`** - Custom drawing behind content
- **`drawWithContent { }`** - Custom drawing with content
- **`paint(painter)`** - Apply custom painter

### Window & System

- **`windowInsetsPadding(insets)`** - Apply system window insets padding
- **`systemBarsPadding()`** - Add system bars padding
- **`navigationBarsPadding()`** - Add navigation bar padding
- **`statusBarsPadding()`** - Add status bar padding
- **`imePadding()`** - Add IME (keyboard) padding

### Nested Scroll

- **`nestedScroll(connection)`** - Handle nested scrolling behavior

## Modifier Scope-Specific Properties

### BoxScope Only

- **`matchParentSize()`** - Match parent size (only in BoxScope)

### LazyListScope

- **`animateItemPlacement()`** - Animate item reordering in lazy lists
- **`fillParentMaxWidth()`** - Fill parent's max width in lazy list items
- **`fillParentMaxHeight()`** - Fill parent's max height in lazy list items

## Performance Considerations

### Modifier Recomposition

```kotlin
// ❌ Bad - Creates new modifier on every recomposition
@Composable
fun BadExample(isSelected: Boolean) {
    Text(
        text = "Hello",
        modifier = Modifier
            .background(if (isSelected) Color.Blue else Color.Gray)
    )
}

// ✅ Good - Stable modifier reference
@Composable
fun GoodExample(isSelected: Boolean) {
    val backgroundColor = if (isSelected) Color.Blue else Color.Gray
    Text(
        text = "Hello", 
        modifier = Modifier.background(backgroundColor)
    )
}
```

### Remember Expensive Modifiers

```kotlin
@Composable
fun ExpensiveModifier() {
    val expensiveModifier = remember {
        Modifier
            .graphicsLayer {
                // Complex transformations
                rotationX = 45f
                rotationY = 45f
                cameraDistance = 12f
            }
    }
    
    Text("Hello", modifier = expensiveModifier)
}
```

## Custom Modifier Extensions

### Creating Custom Modifiers

```kotlin
fun Modifier.dashedBorder(
    width: Dp,
    color: Color,
    cornerRadius: Dp
) = this.drawBehind {
    val strokeWidth = width.toPx()
    val pathEffect = PathEffect.dashPathEffect(
        floatArrayOf(10f, 10f), 0f
    )
    // Drawing logic here
}

// Usage
Text(
    text = "Dashed Border",
    modifier = Modifier.dashedBorder(2.dp, Color.Blue, 8.dp)
)
```

### Conditional Modifier Extension

```kotlin
fun Modifier.conditional(
    condition: Boolean,
    modifier: Modifier.() -> Modifier
): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}

// Usage
Text(
    text = "Hello",
    modifier = Modifier
        .conditional(isHighlighted) {
            background(Color.Yellow)
        }
)
```

## Debugging Modifiers

### Visual Debugging

```kotlin
fun Modifier.debugBorder(color: Color = Color.Red) = this.border(1.dp, color)

// Usage during development
Text(
    text = "Debug me",
    modifier = Modifier
        .padding(16.dp)
        .debugBorder() // Remove in production
)
```

### Modifier Chain Inspection

```kotlin
// Use this to understand modifier chain
fun Modifier.logModifier(tag: String) = this.then(
    Modifier.layout { measurable, constraints ->
        println("$tag: constraints = $constraints")
        val placeable = measurable.measure(constraints)
        println("$tag: measured size = ${placeable.width} x ${placeable.height}")
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }
)
```

## Platform-Specific Modifiers

### Android-Specific

```kotlin
// Pointer input for custom gestures
.pointerInput(Unit) {
    detectTapGestures { offset ->
        // Handle tap
    }
}

// Magnifier for text selection
.magnifier(
    sourceCenter = { offset },
    magnifierCenter = { offset }
)
```

## IDE Tips

- Use **Ctrl+Space** (or Cmd+Space on Mac) after `Modifier.` to see available options
- Android Studio will show you the import path when you hover over properties
- Use **Alt+Enter** to auto-import missing dependencies
- Use **Ctrl+Q** (or F1 on Mac) to see quick documentation for modifier properties
- Enable "Parameter name hints" in IDE settings to see parameter names inline