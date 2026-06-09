## Overview
[[TEXT-Component]]
[[Typography-System]]
[[AnnotatedString]]
[[SelectionContainer]]

# Overview - Text in Jetpack Compose

## What is Text in Jetpack Compose?

Text is the fundamental composable for displaying text content in Jetpack Compose applications. It's similar to TextView in traditional Android development but designed for the declarative UI paradigm.

## Key Concepts

### 1. **Declarative Nature**

- You describe **what** the text should look like, not **how** to create it
- Text automatically handles layout, rendering, and updates

### 2. **Composable Function**

```kotlin
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    // ... many more parameters
)
```

### 3. **Core Parameters**

- **text**: The string content to display
- **modifier**: Layout and behavior modifications
- **style**: Typography styling (TextStyle)
- **color**: Text color
- **fontSize**: Size in sp units
- **fontWeight**: Bold, normal, light, etc.
- **textAlign**: Center, start, end alignment

## Basic Usage Examples

### Simple Text

```kotlin
@Composable
fun SimpleText() {
    Text("Hello, World!")
}
```

### Styled Text

```kotlin
@Composable
fun StyledText() {
    Text(
        text = "Styled Text",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Blue,
        modifier = Modifier.padding(16.dp)
    )
}
```

### Text with Background

```kotlin
@Composable
fun TextWithBackground() {
    Text(
        text = "Background Text",
        modifier = Modifier
            .background(Color.Yellow)
            .padding(8.dp),
        color = Color.Black
    )
}
```

## Important Units

### TextUnit Types

- **sp (Scalable Pixels)**: For font sizes - respects user's font size settings
- **dp (Density-independent Pixels)**: For spacing, margins, padding
- **em**: Relative to the font size

```kotlin
// Correct usage
fontSize = 16.sp  // ✅ Use sp for font size
padding(8.dp)     // ✅ Use dp for spacing

// Incorrect usage
fontSize = 16.dp  // ❌ Don't use dp for font size
```

## Common Beginner Mistakes to Avoid

### 1. **Wrong Unit Types**

```kotlin
// ❌ Wrong
Text("Hello", fontSize = 16.dp)

// ✅ Correct
Text("Hello", fontSize = 16.sp)
```

### 2. **Modifier Order Matters**

```kotlin
// ❌ Background won't be rounded
Text(
    "Hello",
    modifier = Modifier
        .background(Color.Red)
        .clip(RoundedCornerShape(8.dp))
)

// ✅ Correct order
Text(
    "Hello",
    modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(Color.Red)
        .padding(8.dp)
)
```

### 3. **Using String Templates Unnecessarily**

```kotlin
// ❌ Unnecessary string template
Text("$staticString")

// ✅ Direct string
Text(staticString)

// ✅ Correct usage of template
Text("Hello, $userName!")
```

## Text Layout in Containers

### In Column (Vertical Stack)

```kotlin
Column {
    Text("First line")
    Text("Second line")
    Text("Third line")
}
```

### In Row (Horizontal Layout)

```kotlin
Row {
    Text("Label: ")
    Text("Value", fontWeight = FontWeight.Bold)
}
```

### Centered in Box

```kotlin
Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
) {
    Text("Centered Text")
}
```

## Text Alignment Options

```kotlin
// Within the composable
Text(
    text = "Centered",
    modifier = Modifier.fillMaxWidth(),
    textAlign = TextAlign.Center
)

// Available options:
// TextAlign.Start, TextAlign.End, TextAlign.Center
// TextAlign.Justify, TextAlign.Left, TextAlign.Right
```

## Performance Tips

### 1. **Avoid Frequent Recomposition**

```kotlin
// ❌ Creates new TextStyle every recomposition
Text(
    "Hello",
    style = TextStyle(fontSize = 16.sp, color = Color.Red)
)

// ✅ Define style outside composable
val titleStyle = TextStyle(fontSize = 16.sp, color = Color.Red)

@Composable
fun MyText() {
    Text("Hello", style = titleStyle)
}
```

### 2. **Use Material Theme Styles**

```kotlin
// ✅ Use predefined theme styles
Text("Title", style = MaterialTheme.typography.headlineMedium)
Text("Body", style = MaterialTheme.typography.bodyLarge)
```

## Next Steps

After mastering basic Text usage, explore:

1. **Typography System** - Consistent text styles across your app
2. **AnnotatedString** - Rich text with multiple styles
3. **SelectionContainer** - Making text selectable and copyable
4. **Text fields** - For user input

## Quick Reference

|Property|Purpose|Example|
|---|---|---|
|`text`|Content to display|`"Hello World"`|
|`fontSize`|Text size|`16.sp`|
|`fontWeight`|Text weight|`FontWeight.Bold`|
|`color`|Text color|`Color.Red`|
|`textAlign`|Alignment|`TextAlign.Center`|
|`maxLines`|Line limit|`maxLines = 2`|
|`overflow`|Overflow behavior|`TextOverflow.Ellipsis`|

Remember: Start simple, then add complexity as needed!