# Complete Guide to Buttons in Jetpack Compose

## Table of Contents

 

---

## Chapter 1: Button Fundamentals

### 1.1 Basic Button Structure

The `Button` composable is the primary interactive element in Jetpack Compose for triggering actions.

```kotlin
Button(
    onClick = { /* Action */ }
) {
    Text("Button Text")
}
```

### 1.2 Key Components

- **onClick**: Lambda function triggered when button is pressed
- **modifier**: Styling and layout modifications
- **enabled**: Boolean to enable/disable the button
- **shape**: Button shape (rounded corners, etc.)
- **colors**: Color scheme for different states
- **elevation**: Shadow and depth effects
- **border**: Border styling
- **contentPadding**: Internal padding for content

### 1.3 Import Statements

```kotlin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
```

---

## Chapter 2: Button Types and Variants

### 2.1 Standard Button Types

#### Filled Button (Default)

```kotlin
Button(onClick = { }) {
    Text("Filled Button")
}
```

#### Elevated Button

```kotlin
ElevatedButton(onClick = { }) {
    Text("Elevated Button")
}
```

#### Filled Tonal Button

```kotlin
FilledTonalButton(onClick = { }) {
    Text("Tonal Button")
}
```

#### Outlined Button

```kotlin
OutlinedButton(onClick = { }) {
    Text("Outlined Button")
}
```

#### Text Button

```kotlin
TextButton(onClick = { }) {
    Text("Text Button")
}
```

### 2.2 Button Comparison Table

| Button Type       | Background  | Elevation | Border | Use Case             |
| ----------------- | ----------- | --------- | ------ | -------------------- |
| Button            | Filled      | Low       | None   | Primary actions      |
| ElevatedButton    | Filled      | High      | None   | Important actions    |
| FilledTonalButton | Tonal       | None      | None   | Secondary actions    |
| OutlinedButton    | Transparent | None      | Yes    | Alternative actions  |
| TextButton        | Transparent | None      | None   | Low emphasis actions |

---

## Chapter 3: Button Styling and Colors

### 3.1 Basic Color Customization

```kotlin
Button(
    onClick = { },
    colors = ButtonDefaults.buttonColors(
        containerColor = Color.Blue,
        contentColor = Color.White
    )
) {
    Text("Colored Button")
}
```

### 3.2 Complete Color Configuration

```kotlin
Button(
    onClick = { },
    colors = ButtonDefaults.buttonColors(
        containerColor = Color.Cyan,         // Background color
        contentColor = Color.Black,          // Text/icon color
        disabledContainerColor = Color.Gray, // Background when disabled
        disabledContentColor = Color.LightGray // Text/icon when disabled
    )
) {
    Text("Fully Styled Button")
}
```

### 3.3 Material Theme Colors

```kotlin
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
) {
    Text("Theme Button")
}
```

### 3.4 Custom Color Definitions

```kotlin
// In Color.kt or theme file
val CustomBlue = Color(0xFF2196F3)
val CustomRed = Color(0xFFE91E63)
val CustomGreen = Color(0xFF4CAF50)

// Usage
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = CustomBlue
    )
) {
    Text("Custom Color")
}
```

---

## Chapter 4: Button States and Interactions

### 4.1 Enabled/Disabled States

```kotlin
var isEnabled by remember { mutableStateOf(true) }

Button(
    onClick = { },
    enabled = isEnabled
) {
    Text(if (isEnabled) "Enabled" else "Disabled")
}
```

### 4.2 State Management

```kotlin
var buttonState by remember { mutableStateOf("Normal") }

Button(
    onClick = { 
        buttonState = "Clicked"
        // Reset after delay
        kotlinx.coroutines.GlobalScope.launch {
            kotlinx.coroutines.delay(1000)
            buttonState = "Normal"
        }
    }
) {
    Text(buttonState)
}
```

### 4.3 Loading State Button

```kotlin
var isLoading by remember { mutableStateOf(false) }

Button(
    onClick = { 
        isLoading = true
        // Simulate network call
    },
    enabled = !isLoading
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = MaterialTheme.colorScheme.onPrimary
        )
    } else {
        Text("Submit")
    }
}
```

---

## Chapter 5: Button Content and Layout

### 5.1 Text Content

```kotlin
Button(onClick = { }) {
    Text(
        text = "Button Text",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
}
```

### 5.2 Icon and Text Combination

```kotlin
Button(onClick = { }) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Text("Add Item")
    }
}
```

### 5.3 Icon-Only Button

```kotlin
Button(
    onClick = { },
    modifier = Modifier.size(56.dp),
    contentPadding = PaddingValues(0.dp)
) {
    Icon(
        imageVector = Icons.Default.Favorite,
        contentDescription = "Favorite"
    )
}
```

### 5.4 Content Padding

```kotlin
Button(
    onClick = { },
    contentPadding = PaddingValues(
        horizontal = 24.dp,
        vertical = 12.dp
    )
) {
    Text("Custom Padding")
}
```

---

## Chapter 6: Advanced Button Customization

### 6.1 Custom Shapes

```kotlin
Button(
    onClick = { },
    shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 4.dp,
        bottomStart = 4.dp,
        bottomEnd = 16.dp
    )
) {
    Text("Custom Shape")
}
```

### 6.2 Gradient Background

```kotlin
Box(
    modifier = Modifier
        .background(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Red, Color.Orange, Color.Yellow)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        .clickable { /* Action */ }
        .padding(horizontal = 24.dp, vertical = 12.dp)
) {
    Text("Gradient Button", color = Color.White)
}
```

### 6.3 Elevation and Shadow

```kotlin
ElevatedButton(
    onClick = { },
    elevation = ButtonDefaults.elevatedButtonElevation(
        defaultElevation = 8.dp,
        pressedElevation = 4.dp,
        disabledElevation = 0.dp
    )
) {
    Text("Elevated Button")
}
```

### 6.4 Border Styling

```kotlin
OutlinedButton(
    onClick = { },
    border = BorderStroke(
        width = 2.dp,
        color = Color.Red
    )
) {
    Text("Custom Border")
}
```

---

## Chapter 7: Accessibility and Best Practices

### 7.1 Content Description

```kotlin
Button(
    onClick = { },
    modifier = Modifier.semantics {
        contentDescription = "Add new item to the list"
    }
) {
    Icon(Icons.Default.Add, contentDescription = null)
}
```

### 7.2 Minimum Touch Target

```kotlin
Button(
    onClick = { },
    modifier = Modifier
        .heightIn(min = 48.dp) // Minimum recommended touch target
        .widthIn(min = 48.dp)
) {
    Text("Accessible")
}
```

### 7.3 High Contrast Support

```kotlin
Button(
    onClick = { },
    colors = ButtonDefaults.buttonColors(
        containerColor = if (isSystemInDarkTheme()) 
            Color.White else Color.Black,
        contentColor = if (isSystemInDarkTheme()) 
            Color.Black else Color.White
    )
) {
    Text("High Contrast")
}
```

---

## Chapter 8: Animation and Dynamic Effects

### 8.1 Color Animation

```kotlin
var isPressed by remember { mutableStateOf(false) }
val buttonColor by animateColorAsState(
    targetValue = if (isPressed) Color.Green else Color.Blue,
    animationSpec = tween(300),
    label = "button color"
)

Button(
    onClick = { isPressed = !isPressed },
    colors = ButtonDefaults.buttonColors(
        containerColor = buttonColor
    )
) {
    Text("Animated Color")
}
```

### 8.2 Size Animation

```kotlin
var isExpanded by remember { mutableStateOf(false) }
val buttonWidth by animateDpAsState(
    targetValue = if (isExpanded) 200.dp else 100.dp,
    label = "button width"
)

Button(
    onClick = { isExpanded = !isExpanded },
    modifier = Modifier.width(buttonWidth)
) {
    Text("Expand")
}
```

### 8.3 Press Animation

```kotlin
val interactionSource = remember { MutableInteractionSource() }
val isPressed by interactionSource.collectIsPressedAsState()
val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.95f else 1f,
    label = "button scale"
)

Button(
    onClick = { },
    modifier = Modifier.scale(scale),
    interactionSource = interactionSource
) {
    Text("Press Animation")
}
```

---

## Chapter 9: Custom Button Components

### 9.1 Reusable Custom Button

```kotlin
@Composable
fun CustomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(text)
        }
    }
}
```

### 9.2 Floating Action Button Style

```kotlin
@Composable
fun CustomFAB(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(56.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}
```

### 9.3 Toggle Button

```kotlin
@Composable
fun ToggleButton(
    text: String,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { onToggle(!isSelected) },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.outline,
            contentColor = if (isSelected) 
                MaterialTheme.colorScheme.onPrimary 
            else 
                MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(text)
    }
}
```

---

## Chapter 10: Performance Optimization

### 10.1 Stable Parameters

```kotlin
// ❌ Avoid creating new objects in composition
Button(
    onClick = { },
    colors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFF2196F3) // Creates new Color each time
    )
) { }

// ✅ Use stable references
val buttonColor = Color(0xFF2196F3) // Stable reference
Button(
    onClick = { },
    colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
) { }
```

### 10.2 Memoized Click Handlers

```kotlin
@Composable
fun ButtonList(items: List<String>, onItemClick: (String) -> Unit) {
    LazyColumn {
        items(items) { item ->
            val clickHandler = remember(item) {
                { onItemClick(item) }
            }
            Button(onClick = clickHandler) {
                Text(item)
            }
        }
    }
}
```

### 10.3 Avoiding Recomposition

```kotlin
@Composable
fun OptimizedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use derivedStateOf for computed values
    val buttonText by remember {
        derivedStateOf { text.uppercase() }
    }
    
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(buttonText)
    }
}
```

---

## Quick Reference Cheat Sheet

### Essential Button Properties

```kotlin
Button(
    onClick = { },                    // Required: Click handler
    modifier = Modifier,              // Layout modifications
    enabled = true,                   // Enable/disable state
    shape = ButtonDefaults.shape,     // Button shape
    colors = ButtonDefaults.buttonColors(), // Color scheme
    elevation = ButtonDefaults.buttonElevation(), // Shadow
    border = null,                    // Border styling
    contentPadding = ButtonDefaults.ContentPadding, // Internal padding
    interactionSource = remember { MutableInteractionSource() }
) {
    // Button content (Text, Icon, etc.)
}
```

### Common Color Schemes

```kotlin
// Primary button
colors = ButtonDefaults.buttonColors()

// Secondary button  
colors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.secondary
)

// Error button
colors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.error
)

// Custom colors
colors = ButtonDefaults.buttonColors(
    containerColor = Color.Blue,
    contentColor = Color.White,
    disabledContainerColor = Color.Gray,
    disabledContentColor = Color.LightGray
)
```

---

## Tags

 
---

_Last Updated: July 2025_ _Compatibility: Jetpack Compose 1.5+_