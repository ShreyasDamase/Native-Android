# SelectionContainer - Complete Beginner's Guide

## What is SelectionContainer?

**SelectionContainer** is a Jetpack Compose composable that enables text selection functionality. By default, `Text` composables in Compose are **NOT selectable** - users cannot highlight, copy, or interact with the text. SelectionContainer changes this behavior.

## Key Features

- ✅ **Text Selection**: Touch and drag to select text
- ✅ **Copy Functionality**: Long press to show context menu with copy option
- ✅ **Selection Handles**: Visual handles for precise text selection
- ✅ **Keyboard Shortcuts**: Ctrl+A (select all), Ctrl+C (copy) on supported platforms
- ✅ **Accessibility Support**: Screen readers can interact with selected text
- ✅ **Cross-Text Selection**: Select text across multiple Text composables

## Basic Usage

### Simple SelectionContainer

```kotlin
@Composable
fun BasicSelectableText() {
    SelectionContainer {
        Text("This text can be selected and copied!")
    }
}
```

### Multiple Text Elements

```kotlin
@Composable
fun MultipleSelectableTexts() {
    SelectionContainer {
        Column {
            Text("First paragraph - selectable")
            Text("Second paragraph - also selectable")
            Text("Users can select across both texts!")
        }
    }
}
```

## ⚠️ Common Beginner Mistakes

### ❌ Mistake 1: Wrapping Each Text Individually

```kotlin
// DON'T DO THIS - Inefficient and limits cross-text selection
Column {
    SelectionContainer {
        Text("First text")
    }
    SelectionContainer {
        Text("Second text")  // Can't select across both texts
    }
}
```

### ✅ Correct Way: Wrap the Container

```kotlin
// DO THIS - Efficient and allows cross-text selection
SelectionContainer {
    Column {
        Text("First text")
        Text("Second text")  // Can select across both texts
    }
}
```

### ❌ Mistake 2: Forgetting Non-Selectable Areas

```kotlin
// This makes EVERYTHING selectable (including UI elements)
SelectionContainer {
    Column {
        Button(onClick = {}) { Text("Click me") }  // Button text is selectable!
        Text("Article content")
    }
}
```

### ✅ Correct Way: Use DisableSelection

```kotlin
SelectionContainer {
    Column {
        DisableSelection {
            Button(onClick = {}) { Text("Click me") }  // Button text NOT selectable
        }
        Text("Article content")  // Only this is selectable
    }
}
```

## Working with Different Content Types

### With AnnotatedString (Styled Text)

```kotlin
@Composable
fun SelectableStyledText() {
    val styledText = buildAnnotatedString {
        append("This is ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append("bold")
        }
        append(" and this is ")
        withStyle(SpanStyle(color = Color.Red)) {
            append("red")
        }
        append(" text. All selectable!")
    }
    
    SelectionContainer {
        Text(text = styledText)
    }
}
```

### Long Text with Scrolling

```kotlin
@Composable
fun SelectableLongText() {
    SelectionContainer {
        LazyColumn(
            modifier = Modifier.padding(16.dp)
        ) {
            item {
                Text(
                    text = """
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit...
                        [Long text content here]
                    """.trimIndent()
                )
            }
        }
    }
}
```

## DisableSelection - Excluding Specific Areas

### Basic Usage

```kotlin
@Composable
fun MixedSelectableContent() {
    SelectionContainer {
        Column {
            Text("This CAN be selected")
            
            DisableSelection {
                Text("This CANNOT be selected")
            }
            
            Text("This CAN be selected again")
        }
    }
}
```

### Practical Example: Article Layout

```kotlin
@Composable
fun ArticleWithSelectableContent() {
    Column {
        // Non-selectable header (UI element)
        DisableSelection {
            TopAppBar(
                title = { Text("Article Title") },
                backgroundColor = Color.Blue
            )
        }
        
        // Selectable article content
        SelectionContainer {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    Text(
                        text = "Article Headline",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                item {
                    Text(
                        text = """
                            This is the main article content that users can select 
                            and copy. Multiple paragraphs can be selected together.
                            
                            Users can drag across paragraphs to select large blocks 
                            of text for copying.
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        
        // Non-selectable footer (UI element)
        DisableSelection {
            BottomAppBar {
                Text("Footer - Not Selectable")
            }
        }
    }
}
```

## Advanced Examples

### Chat Message with Selective Selection

```kotlin
@Composable
fun SelectableChatMessage(
    senderName: String,
    message: String,
    timestamp: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Non-selectable metadata
            DisableSelection {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = senderName,
                        fontWeight = FontWeight.Bold,
                        color = Color.Blue
                    )
                    Text(
                        text = timestamp,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Selectable message content only
            SelectionContainer {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

// Usage
@Composable
fun ChatScreen() {
    LazyColumn {
        items(messages) { message ->
            SelectableChatMessage(
                senderName = message.sender,
                message = message.content,
                timestamp = message.time
            )
        }
    }
}
```

### Code Block with Selection

```kotlin
@Composable
fun SelectableCodeBlock(code: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        SelectionContainer {
            Text(
                text = code,
                modifier = Modifier.padding(16.dp),
                fontFamily = FontFamily.Monospace,
                color = Color.Green,
                fontSize = 14.sp
            )
        }
    }
}
```

## 🎯 Best Practices

### 1. **Scope SelectionContainer Appropriately**

- Wrap content areas, not entire screens
- Keep UI elements (buttons, toolbars) outside SelectionContainer
- Use one SelectionContainer per content section

### 2. **Performance Considerations**

```kotlin
// ✅ Good - Single SelectionContainer for related content
SelectionContainer {
    LazyColumn {
        items(articles) { article ->
            Text(article.content)
        }
    }
}

// ❌ Bad - Multiple SelectionContainers hurt performance
LazyColumn {
    items(articles) { article ->
        SelectionContainer {
            Text(article.content)  // Creates many SelectionContainers
        }
    }
}
```

### 3. **User Experience**

- Always exclude interactive UI elements using DisableSelection
- Test selection on different screen sizes
- Ensure selected text is easily readable
- Provide visual feedback when text is selected

### 4. **Accessibility**

- SelectionContainer automatically provides accessibility support
- Screen readers can announce selected text
- Keyboard navigation works automatically

## 🚫 What NOT to Do

1. **Don't wrap buttons/interactive elements** without DisableSelection
2. **Don't create nested SelectionContainers** - they conflict
3. **Don't forget to test** cross-text selection functionality
4. **Don't make everything selectable** - only content should be selectable

## 📱 Testing Your Implementation

### Manual Testing Checklist

- [ ] Long press shows selection handles
- [ ] Drag to select works smoothly
- [ ] Context menu appears with copy option
- [ ] Copy functionality works
- [ ] Selection across multiple texts works
- [ ] UI elements are not selectable (when using DisableSelection)
- [ ] Selection handles are visible and functional

### Code Testing

```kotlin
@Preview
@Composable
fun TestSelectableContent() {
    SelectionContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Test paragraph 1 - should be selectable")
            Text("Test paragraph 2 - should also be selectable")
            
            DisableSelection {
                Button(onClick = {}) {
                    Text("This button text should NOT be selectable")
                }
            }
        }
    }
}
```

## Summary

SelectionContainer is essential for creating user-friendly apps where users need to copy text content. Remember:

- **Wrap content areas**, not entire UIs
- **Use DisableSelection** for UI elements
- **Test selection behavior** thoroughly
- **Keep it simple** - one SelectionContainer per content section

This makes your app more professional and user-friendly by allowing users to easily copy important information like addresses, phone numbers, article content, etc.