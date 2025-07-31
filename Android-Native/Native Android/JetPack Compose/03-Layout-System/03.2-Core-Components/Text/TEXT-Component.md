# TEXT-Component - Complete Guide

## Introduction

The `Text` composable is the foundation of text display in Jetpack Compose. It replaces the traditional `TextView` with a more flexible, declarative approach.

## Basic Text Composable

### Function Signature

```kotlin
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
)
```

## Essential Properties

### 1. **Text Content**

```kotlin
// Basic string
Text("Hello, World!")

// String variable
val greeting = "Welcome!"
Text(greeting)

// String template
val name = "John"
Text("Hello, $name!")

// Multi-line string
Text("""
    Line 1
    Line 2
    Line 3
""".trimIndent())
```

### 2. **Font Size**

```kotlin
// Always use .sp for font sizes
Text("Small", fontSize = 12.sp)
Text("Medium", fontSize = 16.sp)
Text("Large", fontSize = 24.sp)
Text("Extra Large", fontSize = 32.sp)

// ❌ NEVER use .dp for font size
// Text("Wrong", fontSize = 16.dp) // This will cause issues
```

### 3. **Font Weight**

```kotlin
Text("Thin", fontWeight = FontWeight.Thin)        // 100
Text("Light", fontWeight = FontWeight.Light)      // 300
Text("Normal", fontWeight = FontWeight.Normal)    // 400
Text("Medium", fontWeight = FontWeight.Medium)    // 500
Text("Bold", fontWeight = FontWeight.Bold)        // 700
Text("Black", fontWeight = FontWeight.Black)      // 900

// Custom weight
Text("Custom", fontWeight = FontWeight(600))
```

### 4. **Font Style**

```kotlin
Text("Normal Text", fontStyle = FontStyle.Normal)
Text("Italic Text", fontStyle = FontStyle.Italic)
```

### 5. **Colors**

```kotlin
// Predefined colors
Text("Red Text", color = Color.Red)
Text("Blue Text", color = Color.Blue)

// Custom colors
Text("Custom", color = Color(0xFF6200EE))
Text("RGB", color = Color(red = 255, green = 0, blue = 0))

// Theme colors
Text("Primary", color = MaterialTheme.colorScheme.primary)
Text("Secondary", color = MaterialTheme.colorScheme.secondary)
```

## Advanced Properties

### 1. **Text Alignment**

```kotlin
// Must use fillMaxWidth() to see alignment effect
Text(
    text = "Left Aligned",
    modifier = Modifier.fillMaxWidth(),
    textAlign = TextAlign.Start
)

Text(
    text = "Center Aligned",
    modifier = Modifier.fillMaxWidth(),
    textAlign = TextAlign.Center
)

Text(
    text = "Right Aligned",
    modifier = Modifier.fillMaxWidth(),
    textAlign = TextAlign.End
)

Text(
    text = "Justified text will spread words evenly across the width",
    modifier = Modifier.fillMaxWidth(),
    textAlign = TextAlign.Justify
)
```

### 2. **Line Management**

```kotlin
// Limit number of lines
Text(
    text = "This is a very long text that might wrap to multiple lines",
    maxLines = 2
)

// Set minimum lines
Text(
    text = "Short",
    minLines = 3  // Will create 3 lines even for short text
)

// Handle overflow
Text(
    text = "This text is too long and will be clipped",
    maxLines = 1,
    overflow = TextOverflow.Ellipsis  // Shows "..."
)

// Other overflow options
Text("Long text", overflow = TextOverflow.Clip)     // Hard cut
Text("Long text", overflow = TextOverflow.Visible)  // Extends beyond bounds
```

### 3. **Letter and Line Spacing**

```kotlin
// Letter spacing
Text(
    text = "S P A C E D",
    letterSpacing = 4.sp
)

// Line height
Text(
    text = "Line 1\nLine 2\nLine 3",
    lineHeight = 24.sp
)
```

### 4. **Text Decoration**

```kotlin
Text(
    text = "Underlined Text",
    textDecoration = TextDecoration.Underline
)

Text(
    text = "Line Through Text",
    textDecoration = TextDecoration.LineThrough
)

// Combine decorations
Text(
    text = "Both Decorations",
    textDecoration = TextDecoration.Underline + TextDecoration.LineThrough
)
```

### 5. **Font Families**

```kotlin
Text("Default Font", fontFamily = FontFamily.Default)
Text("Serif Font", fontFamily = FontFamily.Serif)
Text("Sans Serif", fontFamily = FontFamily.SansSerif)
Text("Monospace", fontFamily = FontFamily.Monospace)
Text("Cursive", fontFamily = FontFamily.Cursive)
```

## Working with TextStyle

### Creating Custom Styles

```kotlin
// Define reusable styles
val titleStyle = TextStyle(
    fontSize = 24.sp,
    fontWeight = FontWeight.Bold,
    color = Color.Black
)

val subtitleStyle = TextStyle(
    fontSize = 16.sp,
    fontWeight = FontWeight.Medium,
    color = Color.Gray
)

// Use the styles
@Composable
fun StyledTexts() {
    Column {
        Text("Main Title", style = titleStyle)
        Text("Subtitle", style = subtitleStyle)
    }
}
```

### Combining Styles

```kotlin
// Base style
val baseStyle = TextStyle(
    fontSize = 16.sp,
    fontFamily = FontFamily.SansSerif
)

// Override specific properties
Text(
    text = "Bold Red Text",
    style = baseStyle.copy(
        fontWeight = FontWeight.Bold,
        color = Color.Red
    )
)
```

## Practical Examples

### 1. **Profile Card Text Elements**

```kotlin
@Composable
fun ProfileCard() {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // Name
        Text(
            text = "John Doe",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        )
        
        // Title
        Text(
            text = "Software Developer",
            style = TextStyle(
                fontSize = 16.sp,
                color = Color.Gray,
                fontStyle = FontStyle.Italic
            ),
            modifier = Modifier.padding(top = 4.dp)
        )
        
        // Description
        Text(
            text = "Passionate about creating beautiful and functional user interfaces.",
            style = TextStyle(fontSize = 14.sp),
            modifier = Modifier.padding(top = 8.dp),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
```

### 2. **Label-Value Pairs**

```kotlin
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        Text(
            text = value,
            fontWeight = FontWeight.Bold
        )
    }
}

// Usage
InfoRow("Age", "25")
InfoRow("Location", "New York")
```

### 3. **Error and Success Messages**

```kotlin
@Composable
fun StatusMessage(message: String, isError: Boolean = false) {
    Text(
        text = message,
        color = if (isError) Color.Red else Color.Green,
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isError) Color.Red.copy(alpha = 0.1f) else Color.Green.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(12.dp)
    )
}

// Usage
StatusMessage("Operation successful!", isError = false)
StatusMessage("Something went wrong!", isError = true)
```

## Common Beginner Mistakes

### ❌ **Mistake 1: Using dp for font size**

```kotlin
// Wrong
Text("Hello", fontSize = 16.dp)

// Correct
Text("Hello", fontSize = 16.sp)
```

### ❌ **Mistake 2: Not using fillMaxWidth() with textAlign**

```kotlin
// Won't show alignment effect
Text("Centered", textAlign = TextAlign.Center)

// Correct
Text(
    text = "Centered",
    textAlign = TextAlign.Center,
    modifier = Modifier.fillMaxWidth()
)
```

### ❌ **Mistake 3: Creating TextStyle in composable body**

```kotlin
// Inefficient - creates new style on every recomposition
@Composable
fun BadExample() {
    Text(
        "Hello",
        style = TextStyle(fontSize = 16.sp, color = Color.Red)
    )
}

// Good - define outside
val textStyle = TextStyle(fontSize = 16.sp, color = Color.Red)

@Composable
fun GoodExample() {
    Text("Hello", style = textStyle)
}
```

### ❌ **Mistake 4: Wrong modifier order**

```kotlin
// Background won't be rounded
Text(
    "Hello",
    modifier = Modifier
        .background(Color.Yellow)
        .clip(RoundedCornerShape(8.dp))
        .padding(8.dp)
)

// Correct order
Text(
    "Hello",
    modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(Color.Yellow)
        .padding(8.dp)
)
```

## Performance Tips

1. **Define styles outside composables** to avoid recreation
2. **Use Material Theme typography** when possible
3. **Avoid complex string operations** in composable body
4. **Use remember for computed strings** if expensive

## Testing Text Components

```kotlin
@Test
fun testTextContent() {
    composeTestRule.setContent {
        Text("Hello World")
    }
    
    composeTestRule
        .onNodeWithText("Hello World")
        .assertIsDisplayed()
}
```

## Summary

The Text composable is powerful and flexible. Key points:

- Always use `.sp` for font sizes
- Use `fillMaxWidth()` with `textAlign` for alignment
- Define styles outside composables for performance
- Use modifier order: clip → background → padding
- Handle overflow with `maxLines` and `TextOverflow.Ellipsis`
- Leverage Material Theme typography for consistency