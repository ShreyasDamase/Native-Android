# Typography-System - Material Design Typography

## Introduction

The Typography System in Jetpack Compose provides a structured approach to text styling throughout your app. It's based on Material Design's typography scale and ensures consistency across all text elements.

## What is Typography System?

Typography System is a collection of predefined text styles that:

- Maintains visual consistency across your app
- Follows Material Design guidelines
- Provides semantic meaning to text (headlines, body, labels, etc.)
- Makes your app look professional and polished

## Material 3 Typography Scale

### Typography Hierarchy

Material 3 defines 15 different text styles organized in categories:

#### **Display Styles** (Largest)

```kotlin
// For hero text, marketing headers
MaterialTheme.typography.displayLarge    // 57sp
MaterialTheme.typography.displayMedium   // 45sp  
MaterialTheme.typography.displaySmall    // 36sp
```

#### **Headline Styles**

```kotlin
// For section headers, page titles
MaterialTheme.typography.headlineLarge   // 32sp
MaterialTheme.typography.headlineMedium  // 28sp
MaterialTheme.typography.headlineSmall   // 24sp
```

#### **Title Styles**

```kotlin
// For card titles, dialog titles
MaterialTheme.typography.titleLarge      // 22sp
MaterialTheme.typography.titleMedium     // 16sp
MaterialTheme.typography.titleSmall      // 14sp
```

#### **Body Styles**

```kotlin
// For main content, paragraphs
MaterialTheme.typography.bodyLarge       // 16sp
MaterialTheme.typography.bodyMedium      // 14sp
MaterialTheme.typography.bodySmall       // 12sp
```

#### **Label Styles**

```kotlin
// For buttons, chips, captions
MaterialTheme.typography.labelLarge      // 14sp
MaterialTheme.typography.labelMedium     // 12sp
MaterialTheme.typography.labelSmall      // 11sp
```

## Using Typography in Practice

### Basic Usage

```kotlin
@Composable
fun TypographyExample() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Display Large",
            style = MaterialTheme.typography.displayLarge
        )
        
        Text(
            text = "Headline Medium", 
            style = MaterialTheme.typography.headlineMedium
        )
        
        Text(
            text = "Body Large text for main content",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Text(
            text = "Label Small",
            style = MaterialTheme.typography.labelSmall
        )
    }
}
```

### Real-World Example: Article Layout

```kotlin
@Composable
fun ArticleLayout() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Article title
        Text(
            text = "Breaking News: Typography System Explained",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Article subtitle
        Text(
            text = "A comprehensive guide to Material Design typography",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Article body
        Text(
            text = "The typography system in Material Design provides a structured approach to text styling. It ensures consistency and hierarchy throughout your application.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Caption
        Text(
            text = "Published 2 hours ago",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

## Customizing Typography

### Creating Custom Typography

```kotlin
// Define custom typography
val CustomTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 60.sp,
        lineHeight = 68.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    )
    // ... define other styles
)

// Apply custom typography to theme
@Composable
fun MyApp() {
    MaterialTheme(
        typography = CustomTypography
    ) {
        // Your app content
        MyAppContent()
    }
}
```

### Modifying Existing Typography

```kotlin
@Composable
fun ModifiedTypography() {
    // Use base style but modify specific properties
    Text(
        text = "Modified Headline",
        style = MaterialTheme.typography.headlineMedium.copy(
            color = Color.Red,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
    )
}
```

## Typography with Custom Fonts

### Using Custom Font Families

```kotlin
// Define custom font family (put fonts in res/font/)
val CustomFontFamily = FontFamily(
    Font(R.font.roboto_regular, FontWeight.Normal),
    Font(R.font.roboto_medium, FontWeight.Medium),
    Font(R.font.roboto_bold, FontWeight.Bold)
)

// Create typography with custom fonts
val CustomTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = CustomFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = CustomFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    )
)
```

### Google Fonts Integration

```kotlin
// Add to build.gradle (Module: app)
// implementation "androidx.compose.ui:ui-text-google-fonts:$compose_version"

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val Montserrat = GoogleFont("Montserrat")

val MontserratFontFamily = FontFamily(
    Font(googleFont = Montserrat, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = Montserrat, fontProvider = provider, weight = FontWeight.Bold)
)
```

## Best Practices for Typography

### 1. **Use Semantic Styles**

```kotlin
// ✅ Good - semantic meaning
Text("Article Title", style = MaterialTheme.typography.headlineMedium)
Text("Article body...", style = MaterialTheme.typography.bodyLarge)
Text("Published date", style = MaterialTheme.typography.labelSmall)

// ❌ Avoid - no semantic meaning
Text("Article Title", fontSize = 28.sp, fontWeight = FontWeight.Bold)
```

### 2. **Consistent Color Usage**

```kotlin
@Composable
fun ConsistentColorExample() {
    Column {
        // Primary text
        Text(
            "Main Content",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // Secondary text
        Text(
            "Supporting Text",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Disabled text
        Text(
            "Disabled Text",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
}
```

### 3. **Typography Hierarchy Example**

```kotlin
@Composable
fun TypographyHierarchy() {
    Column(modifier = Modifier.padding(16.dp)) {
        // Page title
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Section header
        Text(
            "Account",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Setting item
        Text(
            "Email notifications",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        // Setting description
        Text(
            "Receive notifications about account activity",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}
```

## Typography in Different Components

### 1. **Card with Typography**

```kotlin
@Composable
fun TypographyCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Card Title",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Card subtitle with additional information",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                "Main card content goes here. This could be a longer description.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
```

### 2. **Button Typography**

```kotlin
@Composable
fun ButtonTypography() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { }) {
            Text(
                "Primary Button",
                style = MaterialTheme.typography.labelLarge
            )
        }
        
        OutlinedButton(onClick = { }) {
            Text(
                "Outlined Button",
                style = MaterialTheme.typography.labelLarge
            )
        }
        
        TextButton(onClick = { }) {
            Text(
                "Text Button",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
```

### 3. **List Item Typography**

```kotlin
@Composable
fun ListItemTypography() {
    LazyColumn {
        items(10) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "List Item $index",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Supporting text for item $index",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Action",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
```

## Common Typography Mistakes

### ❌ **Mistake 1: Mixing typography scales**

```kotlin
// Don't mix different scales randomly
Text("Title", fontSize = 23.sp)  // Random size
Text("Subtitle", fontSize = 17.sp)  // Random size

// Use consistent typography scale
Text("Title", style = MaterialTheme.typography.titleLarge)
Text("Subtitle", style = MaterialTheme.typography.titleMedium)
```

### ❌ **Mistake 2: Not using semantic styles**

```kotlin
// Poor semantic meaning
Text("Button", fontSize = 14.sp, fontWeight = FontWeight.Medium)

// Good semantic meaning
Text("Button", style = MaterialTheme.typography.labelLarge)
```

### ❌ **Mistake 3: Overriding too many properties**

```kotlin
// Too many overrides - defeats the purpose
Text(
    "Title",
    style = MaterialTheme.typography.headlineMedium.copy(
        fontSize = 30.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 3.sp,
        lineHeight = 40.sp
    )
)

// Better to define custom style
val customTitleStyle = TextStyle(
    fontSize = 30.sp,
    fontWeight = FontWeight.ExtraBold,
    letterSpacing = 3.sp,
    lineHeight = 40.sp
)
```

## Accessibility Considerations

### 1. **Respect User Font Size**

```kotlin
// Typography automatically scales with user's font size preferences
// No additional code needed - just use typography styles
Text("Accessible Text", style = MaterialTheme.typography.bodyLarge)
```

### 2. **Minimum Text Size**

```kotlin
// Ensure text is readable - avoid going below 12sp
Text(
    "Small but readable",
    style = MaterialTheme.typography.labelSmall // 11sp minimum
)
```

### 3. **Sufficient Color Contrast**

```kotlin
@Composable
fun AccessibleText() {
    // Use theme colors for proper contrast
    Text(
        "High contrast text",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface // Automatically contrasts with surface
    )
}
```

## Typography Testing

### 1. **Different Screen Sizes**

```kotlin
// Test typography on different screen densities
// Typography scales automatically but verify readability
```

### 2. **Different Font Size Settings**

```kotlin
// Test with device font size settings:
// Settings > Display > Font size (Small, Default, Large, Largest)
```

### 3. **Dark Mode**

```kotlin
@Composable
fun DarkModeTypography() {
    // Typography colors automatically adapt to dark/light theme
    Text(
        "Auto-adapting text",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
}
```

## Quick Reference Guide

### When to Use Each Style

|Style|Use Case|Example|
|---|---|---|
|`displayLarge`|Hero text, splash screens|App name on launch screen|
|`displayMedium`|Large marketing headers|"Welcome to our app!"|
|`displaySmall`|Section dividers|"Featured Products"|
|`headlineLarge`|Page titles|"Settings", "Profile"|
|`headlineMedium`|Dialog titles|"Delete Account?"|
|`headlineSmall`|Card headers|"Recent Activity"|
|`titleLarge`|List section headers|"Today", "Yesterday"|
|`titleMedium`|Card titles|"John's Photo"|
|`titleSmall`|Chip labels|"Technology", "Sports"|
|`bodyLarge`|Main content|Article text, descriptions|
|`bodyMedium`|Secondary content|Subtitles, supporting text|
|`bodySmall`|Captions|Photo descriptions|
|`labelLarge`|Button text|"Save", "Cancel"|
|`labelMedium`|Tab labels|"Home", "Search", "Profile"|
|`labelSmall`|Timestamps|"2 min ago", "Updated"|

### Typography Hierarchy Template

```kotlin
@Composable
fun TypographyTemplate() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Page title
        Text("Page Title", style = MaterialTheme.typography.headlineMedium)
        
        // Section header
        Text("Section Header", style = MaterialTheme.typography.titleLarge)
        
        // Subsection
        Text("Subsection", style = MaterialTheme.typography.titleMedium)
        
        // Main content
        Text(
            "Main content text goes here. This is the primary text that users will read.",
            style = MaterialTheme.typography.bodyLarge
        )
        
        // Supporting text
        Text(
            "Supporting or secondary information",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Caption
        Text(
            "Caption or metadata",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Button
        Button(onClick = { }) {
            Text("Action Button", style = MaterialTheme.typography.labelLarge)
        }
    }
}
```

## Summary

The Typography System provides:

- **Consistency** across your entire app
- **Accessibility** with automatic scaling and contrast
- **Professional appearance** following Material Design
- **Semantic meaning** making code more maintainable
- **Easy customization** while maintaining structure

### Key Takeaways:

1. Always use `MaterialTheme.typography.*` instead of manual font sizes
2. Choose styles based on semantic meaning, not just visual appearance
3. Use theme colors with typography for proper contrast
4. Test typography at different font sizes and in dark mode
5. Create custom typography when needed, but maintain the hierarchy
6. Remember that typography automatically handles accessibility scaling

The typography system is the foundation of good text design in your app - master it early for better, more maintainable UI code!