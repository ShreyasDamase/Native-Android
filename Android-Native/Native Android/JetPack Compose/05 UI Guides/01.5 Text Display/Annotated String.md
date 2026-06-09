# AnnotatedString - Rich Text Formatting

## Introduction

`AnnotatedString` is a powerful class in Jetpack Compose that allows you to create rich text with multiple styles, colors, clickable sections, and even inline content within a single Text composable. Think of it as HTML for Compose text.

## What is AnnotatedString?

`AnnotatedString` lets you:

- Apply different styles to different parts of the same text
- Create clickable text sections
- Add different colors to text segments
- Include inline images or custom content
- Create rich text formatting like bold, italic, underline in one text block

## Basic AnnotatedString Creation

### Method 1: AnnotatedString.Builder

```kotlin
@Composable
fun BasicAnnotatedString() {
    val annotatedString = buildAnnotatedString {
        append("This is ")
        
        // Add styled text
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("bold")
        }
        
        append(" and this is ")
        
        withStyle(style = SpanStyle(color = Color.Red)) {
            append("red")
        }
        
        append(" text.")
    }
    
    Text(text = annotatedString)
}
```

### Method 2: Direct Construction

```kotlin
@Composable
fun DirectAnnotatedString() {
    val text = "Hello World"
    val annotatedString = AnnotatedString(
        text = text,
        spanStyles = listOf(
            AnnotatedString.Range(
                SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold),
                start = 0,
                end = 5
            ),
            AnnotatedString.Range(
                SpanStyle(color = Color.Blue, fontStyle = FontStyle.Italic),
                start = 6,
                end = 11
            )
        )
    )
    
    Text(text = annotatedString)
}
```

## SpanStyle vs ParagraphStyle

### SpanStyle - Character Level Styling

```kotlin
@Composable
fun SpanStyleExample() {
    val annotatedString = buildAnnotatedString {
        append("Normal text ")
        
        withStyle(SpanStyle(color = Color.Red)) {
            append("red ")
        }
        
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append("bold ")
        }
        
        withStyle(SpanStyle(fontSize = 24.sp)) {
            append("large ")
        }
        
        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
            append("underlined ")
        }
        
        withStyle(SpanStyle(background = Color.Yellow)) {
            append("highlighted")
        }
    }
    
    Text(annotatedString)
}
```

### ParagraphStyle - Paragraph Level Styling

```kotlin
@Composable
fun ParagraphStyleExample() {
    val annotatedString = buildAnnotatedString {
        withStyle(ParagraphStyle(textAlign = TextAlign.Center)) {
            append("This paragraph is centered.\n")
        }
        
        withStyle(ParagraphStyle(textAlign = TextAlign.End)) {
            append("This paragraph is right-aligned.\n")
        }
        
        withStyle(ParagraphStyle(lineHeight = 30.sp)) {
            append("This paragraph has large line spacing.")
        }
    }
    
    Text(
        text = annotatedString,
        modifier = Modifier.fillMaxWidth()
    )
}
```

## Advanced Styling Examples

### 1. **Multi-Style Text**

```kotlin
@Composable
fun MultiStyleText() {
    val annotatedString = buildAnnotatedString {
        // Title
        withStyle(
            SpanStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        ) {
            append("Breaking News\n\n")
        }
        
        // Subtitle
        withStyle(
            SpanStyle(
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic,
                color = Color.Gray
            )
        ) {
            append("Technology Update - ")
        }
        
        // Date
        withStyle(
            SpanStyle(
                fontSize = 14.sp,
                color = Color.Blue
            )
        ) {
            append("March 15, 2024\n\n")
        }
        
        // Body text
        withStyle(
            SpanStyle(
                fontSize = 16.sp,
                color = Color.Black
            )
        ) {
            append("The latest updates in ")
        }
        
        // Highlighted word
        withStyle(
            SpanStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red,
                background = Color.Yellow.copy(alpha = 0.3f)
            )
        ) {
            append("technology")
        }
        
        withStyle(SpanStyle(fontSize = 16.sp, color = Color.Black)) {
            append(" are revolutionizing the industry.")
        }
    }
    
    Text(
        text = annotatedString,
        modifier = Modifier.padding(16.dp)
    )
}
```

### 2. **Code Highlighting**

```kotlin
@Composable
fun CodeHighlighting() {
    val codeString = buildAnnotatedString {
        // Function keyword
        withStyle(SpanStyle(color = Color.Blue, fontWeight = FontWeight.Bold)) {
            append("fun ")
        }
        
        // Function name
        withStyle(SpanStyle(color = Color.Magenta)) {
            append("calculateSum")
        }
        
        // Parameters
        withStyle(SpanStyle(color = Color.Black)) {
            append("(")
        }
        
        withStyle(SpanStyle(color = Color.Green)) {
            append("a: Int, b: Int")
        }
        
        withStyle(SpanStyle(color = Color.Black)) {
            append("): ")
        }
        
        // Return type
        withStyle(SpanStyle(color = Color.Blue)) {
            append("Int ")
        }
        
        withStyle(SpanStyle(color = Color.Black)) {
            append("{\n    ")
        }
        
        // Return statement
        withStyle(SpanStyle(color = Color.Blue, fontWeight = FontWeight.Bold)) {
            append("return ")
        }
        
        withStyle(SpanStyle(color = Color.Black)) {
            append("a + b\n}")
        }
    }
    
    Text(
        text = codeString,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .background(Color.LightGray.copy(alpha = 0.3f))
            .padding(12.dp)
    )
}
```

## Clickable Text with AnnotatedString

### Basic Clickable Text

```kotlin
@Composable
fun ClickableText() {
    val annotatedString = buildAnnotatedString {
        append("By signing up, you agree to our ")
        
        // Add a tag to identify clickable text
        pushStringAnnotation(tag = "terms", annotation = "terms_of_service")
        withStyle(
            SpanStyle(
                color = Color.Blue,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append("Terms of Service")
        }
        pop()
        
        append(" and ")
        
        pushStringAnnotation(tag = "privacy", annotation = "privacy_policy")
        withStyle(
            SpanStyle(
                color = Color.Blue,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append("Privacy Policy")
        }
        pop()
        
        append(".")
    }
    
    ClickableText(
        text = annotatedString,
        onClick = { offset ->
            // Get annotations at click position
            annotatedString.getStringAnnotations(
                tag = "terms",
                start = offset,
                end = offset
            ).firstOrNull()?.let {
                // Handle terms click
                println("Terms of Service clicked")
            }
            
            annotatedString.getStringAnnotations(
                tag = "privacy",
                start = offset,
                end = offset
            ).firstOrNull()?.let {
                // Handle privacy policy click
                println("Privacy Policy clicked")
            }
        }
    )
}
```

### Advanced Clickable Text with URLs

```kotlin
@Composable
fun ClickableUrlText() {
    val context = LocalContext.current
    
    val annotatedString = buildAnnotatedString {
        append("Visit our website at ")
        
        pushStringAnnotation(
            tag = "URL",
            annotation = "https://example.com"
        )
        withStyle(
            SpanStyle(
                color = Color.Blue,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append("example.com")
        }
        pop()
        
        append(" for more information.")
    }
    
    ClickableText(
        text = annotatedString,
        onClick = { offset ->
            annotatedString.getStringAnnotations(
                tag = "URL",
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                // Open URL in browser
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                context.startActivity(intent)
            }
        }
    )
}
```

## Practical Examples

### 1. **Chat Message with Mentions**

```kotlin
@Composable
fun ChatMessage(message: String, mentions: List<String>) {
    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        
        mentions.forEach { mention ->
            val mentionIndex = message.indexOf("@$mention", lastIndex)
            if (mentionIndex != -1) {
                // Add text before mention
                append(message.substring(lastIndex, mentionIndex))
                
                // Add mention with styling
                pushStringAnnotation(tag = "mention", annotation = mention)
                withStyle(
                    SpanStyle(
                        color = Color.Blue,
                        fontWeight = FontWeight.Bold,
                        background = Color.Blue.copy(alpha = 0.1f)
                    )
                ) {
                    append("@$mention")
                }
                pop()
                
                lastIndex = mentionIndex + mention.length + 1
            }
        }
        
        // Add remaining text
        if (lastIndex < message.length) {
            append(message.substring(lastIndex))
        }
    }
    
    ClickableText(
        text = annotatedString,
        onClick = { offset ->
            annotatedString.getStringAnnotations(
                tag = "mention",
                start = offset,
                end = offset
            ).firstOrNull()?.let { mention ->
                println("Clicked on user: ${mention.item}")
            }
        }
    )
}

// Usage
ChatMessage(
    message = "Hey @john, did you see what @sarah posted?",
    mentions = listOf("john", "sarah")
)
```

### 2. **Rich Text Editor Preview**

```kotlin
@Composable
fun RichTextPreview() {
    val richText = buildAnnotatedString {
        // Heading
        withStyle(
            SpanStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        ) {
            append("Rich Text Example\n\n")
        }
        
        // Paragraph with various styles
        append("This text contains ")
        
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append("bold")
        }
        
        append(", ")
        
        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            append("italic")
        }
        
        append(", ")
        
        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
            append("underlined")
        }
        
        append(", and ")
        
        withStyle(
            SpanStyle(
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        ) {
            append("colored")
        }
        
        append(" text.\n\n")
        
        // Code block
        withStyle(
            SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = Color.LightGray.copy(alpha = 0.3f),
                color = Color.DarkGreen
            )
        ) {
            append("val code = \"This looks like code\"")
        }
        
        append("\n\nAnd this is ")
        
        withStyle(
            SpanStyle(
                fontSize = 12.sp,
                baselineShift = BaselineShift.Superscript
            )
        ) {
            append("superscript")
        }
        
        append(" and ")
        
        withStyle(
            SpanStyle(
                fontSize = 12.sp,
                baselineShift = BaselineShift.Subscript
            )
        ) {
            append("subscript")
        }
        
        append(" text.")
    }
    
    Text(
        text = richText,
        modifier = Modifier.padding(16.dp)
    )
}
```

### 3. **Search Result Highlighting**

```kotlin
@Composable
fun SearchResultText(
    fullText: String,
    searchQuery: String
) {
    val annotatedString = buildAnnotatedString {
        if (searchQuery.isBlank()) {
            append(fullText)
        } else {
            var lastIndex = 0
            var searchIndex = fullText.indexOf(searchQuery, ignoreCase = true)
            
            while (searchIndex != -1) {
                // Add text before match
                append(fullText.substring(lastIndex, searchIndex))
                
                // Add highlighted match
                withStyle(
                    SpanStyle(
                        background = Color.Yellow,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(fullText.substring(searchIndex, searchIndex + searchQuery.length))
                }
                
                lastIndex = searchIndex + searchQuery.length
                searchIndex = fullText.indexOf(searchQuery, lastIndex, ignoreCase = true)
            }
            
            // Add remaining text
            if (lastIndex < fullText.length) {
                append(fullText.substring(lastIndex))
            }
        }
    }
    
    Text(text = annotatedString)
}

// Usage
SearchResultText(
    fullText = "Jetpack Compose makes UI development easier",
    searchQuery = "Compose"
)
```

## Performance Considerations

### 1. **Efficient AnnotatedString Creation**

```kotlin
// ❌ Inefficient - creates new AnnotatedString on every recomposition
@Composable
fun InefficientAnnotatedText(name: String) {
    val annotatedString = buildAnnotatedString {
        append("Hello, ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(name)
        }
    }
    Text(annotatedString)
}

// ✅ Efficient - uses remember to cache result
@Composable
fun EfficientAnnotatedText(name: String) {
    val annotatedString = remember(name) {
        buildAnnotatedString {
            append("Hello, ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(name)
            }
        }
    }
    Text(annotatedString)
}
```

### 2. **Reusable AnnotatedString Builders**

```kotlin
// Create reusable functions for common patterns
fun createHighlightedText(
    normalText: String,
    highlightedText: String,
    highlightColor: Color = Color.Yellow
): AnnotatedString = buildAnnotatedString {
    append(normalText)
    withStyle(SpanStyle(background = highlightColor)) {
        append(highlightedText)
    }
}

@Composable
fun ReusableExample() {
    Text(createHighlightedText("Important: ", "This is highlighted"))
}
```

## Common Mistakes and Best Practices

### ❌ **Mistake 1: Forgetting to pop annotations**

```kotlin
// Wrong - missing pop()
buildAnnotatedString {
    pushStringAnnotation("tag", "value")
    withStyle(SpanStyle(color = Color.Red)) {
        append("Clickable")
    }
    // Missing pop() here
    append("Not clickable")
}

// Correct
buildAnnotatedString {
    pushStringAnnotation("tag", "value")
    withStyle(SpanStyle(color = Color.Red)) {
        append("Clickable")
    }
    pop() // Important!
    append("Not clickable")
}
```

### ❌ **Mistake 2: Not using remember for static content**

```kotlin
// Inefficient
@Composable
fun StaticAnnotatedText() {
    val text = buildAnnotatedString {
        append("This never changes")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append("Bold text")
        }
    }
    Text(text)
}

// Efficient
@Composable
fun StaticAnnotatedText() {
    val text = remember {
        buildAnnotatedString {
            append("This never changes")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append("Bold text")
            }
        }
    }
    Text(text)
}
```

### ✅ **Best Practice: Extract complex AnnotatedString creation**

```kotlin
// Good - separate function for complex logic
fun createFormattedMessage(
    userName: String,
    action: String,
    timestamp: String
): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
        append(userName)
    }
    append(" $action ")
    withStyle(SpanStyle(color = Color.Gray, fontSize = 12.sp)) {
        append(timestamp)
    }
}

@Composable
fun MessageText(userName: String, action: String, timestamp: String) {
    val message = remember(userName, action, timestamp) {
        createFormattedMessage(userName, action, timestamp)
    }
    Text(message)
}
```

## Testing AnnotatedString

```kotlin
@Test
fun testAnnotatedStringContent() {
    val annotatedString = buildAnnotatedString {
        append("Hello ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append("World")
        }
    }
    
    assertEquals("Hello World", annotatedString.text)
    
    // Test annotations
    val annotations = annotatedString.spanStyles
    assertTrue(annotations.isNotEmpty())
}
```

## Summary

AnnotatedString is essential for:

- **Rich text formatting** within single Text composables
- **Clickable text sections** for links and interactions
- **Search highlighting** and dynamic text styling
- **Code syntax highlighting** and technical documentation
- **Chat applications** with mentions and formatting

### Key Points:

1. Use `buildAnnotatedString {}` for complex formatting
2. `SpanStyle` for character-level styling, `ParagraphStyle` for paragraph-level
3. Always `pop()` after `pushStringAnnotation()`
4. Use `remember` for performance with static or expensive AnnotatedStrings
5. `ClickableText` for interactive text sections
6. Extract complex logic into separate functions for maintainability

AnnotatedString bridges the gap between simple text and complex rich text formatting in Compose!