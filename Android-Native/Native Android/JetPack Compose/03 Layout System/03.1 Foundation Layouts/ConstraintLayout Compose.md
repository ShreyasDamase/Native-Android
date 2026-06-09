# ConstraintLayout in Jetpack Compose Guide

## Overview

ConstraintLayout is a layout that allows you to place composables relative to other composables on the screen. It is an alternative to using multiple nested Row, Column, Box and other custom layout elements. ConstraintLayout is useful when implementing larger layouts with more complicated alignment requirements.

## Setup & Dependencies

Add the ConstraintLayout Compose dependency to your `build.gradle`:

```kotlin
implementation "androidx.constraintlayout:constraintlayout-compose:1.0.1"
```

### Core Imports

```kotlin
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.layoutId
```

## Three Ways to Create Constraints

There are 3 ways to create constraints in ConstraintLayout:

- Imbedded in Modifier
- ConstraintSet Composable
- JSON ConstraintSet

## Method 1: Embedded in Modifier (Inline DSL)

The most common approach where constraints are defined directly within the ConstraintLayout.

### Basic Structure

```kotlin
@Composable
fun BasicConstraintLayout() {
    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        // Create references for composables
        val (button, title, subtitle) = createRefs()
        
        // Define composables with constraints
        Text(
            text = "Title",
            modifier = Modifier.constrainAs(title) {
                top.linkTo(parent.top, 16.dp)
                centerHorizontallyTo(parent)
            }
        )
        
        Text(
            text = "Subtitle", 
            modifier = Modifier.constrainAs(subtitle) {
                top.linkTo(title.bottom, 8.dp)
                centerHorizontallyTo(parent)
            }
        )
        
        Button(
            onClick = { },
            modifier = Modifier.constrainAs(button) {
                top.linkTo(subtitle.bottom, 16.dp)
                centerHorizontallyTo(parent)
            }
        ) {
            Text("Click Me")
        }
    }
}
```

### Creating References

```kotlin
// Single reference
val button = createRef()

// Multiple references using destructuring
val (button, title, subtitle) = createRefs()

// Individual references
val button = createRef()
val title = createRef()
val subtitle = createRef()
```

### Guidelines

Guidelines are invisible reference lines that help position composables.

```kotlin
@Composable
fun GuidelineExample() {
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (button, title) = createRefs()
        
        // Create guidelines
        val startGuideline = createGuidelineFromStart(80.dp)
        val topGuideline = createGuidelineFromTop(0.3f) // 30% from top
        val endGuideline = createGuidelineFromEnd(40.dp)
        val bottomGuideline = createGuidelineFromBottom(0.2f) // 20% from bottom
        
        Text(
            text = "Welcome",
            modifier = Modifier.constrainAs(title) {
                top.linkTo(topGuideline)
                start.linkTo(startGuideline)
            }
        )
        
        Button(
            onClick = { },
            modifier = Modifier.constrainAs(button) {
                top.linkTo(title.bottom, 16.dp)
                start.linkTo(startGuideline)
                end.linkTo(endGuideline)
                width = Dimension.fillToConstraints
            }
        ) {
            Text("Login")
        }
    }
}
```

## Method 2: ConstraintSet Composable

For cases like these, you can use ConstraintLayout in a different way: Pass in a ConstraintSet as a parameter to ConstraintLayout. Assign references created in the ConstraintSet to composables using the layoutId modifier.

```kotlin
@Composable
fun ConstraintSetExample() {
    ConstraintLayout(
        constraintSet = ConstraintSet {
            val button = createRefFor("button")
            val title = createRefFor("title")
            val g1 = createGuidelineFromStart(80.dp)
            
            constrain(button) {
                top.linkTo(title.bottom, 16.dp)
                start.linkTo(g1)
            }
            
            constrain(title) {
                centerVerticallyTo(parent)
                start.linkTo(g1)
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Button(
            modifier = Modifier.layoutId("button"),
            onClick = { }
        ) {
            Text("Log In")
        }
        
        Text(
            modifier = Modifier.layoutId("title"),
            text = "Welcome Header",
            style = MaterialTheme.typography.headlineLarge
        )
    }
}
```

## Method 3: JSON ConstraintSet

Define constraints using JSON5 syntax for dynamic or external constraint definitions.

```kotlin
@Composable
fun JsonConstraintSetExample() {
    val constraintSetJson = """
        {
            Header: { exportAs: 'json example' },
            g1: { type: 'vGuideline', start: 80 },
            button: {
                top: ['title', 'bottom', 16],
                start: ['g1', 'start']
            },
            title: {
                centerVertically: 'parent',
                start: ['g1', 'start']
            }
        }
    """.trimIndent()
    
    ConstraintLayout(
        constraintSet = ConstraintSet(constraintSetJson),
        modifier = Modifier.fillMaxSize()
    ) {
        Button(
            modifier = Modifier.layoutId("button"),
            onClick = { }
        ) {
            Text("Log In")
        }
        
        Text(
            modifier = Modifier.layoutId("title"),
            text = "Welcome Header",
            style = MaterialTheme.typography.headlineLarge
        )
    }
}
```

## Constraint Types & Methods

### Vertical Constraints

```kotlin
// Link to vertical anchors
top.linkTo(anchor, margin)
bottom.linkTo(anchor, margin)  
baseline.linkTo(anchor, margin)

// Center vertically
centerVerticallyTo(parent)
centerVerticallyTo(otherWidget)

// Link between two vertical anchors with bias
linkTo(topAnchor, bottomAnchor, topMargin, bottomMargin, bias)
```

### Horizontal Constraints

```kotlin
// Link to horizontal anchors
start.linkTo(anchor, margin)
end.linkTo(anchor, margin)

// Center horizontally  
centerHorizontallyTo(parent)
centerHorizontallyTo(otherWidget)

// Link between two horizontal anchors with bias
linkTo(startAnchor, endAnchor, startMargin, endMargin, bias)
```

### Circular Constraints

```kotlin
// Position relative to another widget at specific angle and distance
circular(widget_id, angle, distance)
```

### Dimensions

```kotlin
// Set width/height behavior
width = Dimension.value(100.dp)        // Fixed size
width = Dimension.fillToConstraints    // Fill available space between constraints
width = Dimension.wrapContent          // Wrap content (default)
width = Dimension.preferredWrapContent // Prefer wrap content but can expand
width = Dimension.ratio("16:9")        // Aspect ratio

// Dimension with constraints
width = Dimension.preferredValue(200.dp) // Preferred size that can shrink/expand
```

## Practical Examples

### Login Screen Layout

```kotlin
@Composable
fun LoginScreen() {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val (logo, title, emailField, passwordField, loginButton, signupText) = createRefs()
        val centerGuideline = createGuidelineFromTop(0.3f)
        
        // Logo
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Logo",
            modifier = Modifier
                .size(80.dp)
                .constrainAs(logo) {
                    bottom.linkTo(centerGuideline, 32.dp)
                    centerHorizontallyTo(parent)
                },
            tint = MaterialTheme.colorScheme.primary
        )
        
        // Title
        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(centerGuideline, 16.dp)
                centerHorizontallyTo(parent)
            }
        )
        
        // Email Field
        OutlinedTextField(
            value = "",
            onValueChange = { },
            label = { Text("Email") },
            modifier = Modifier.constrainAs(emailField) {
                top.linkTo(title.bottom, 32.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }
        )
        
        // Password Field
        OutlinedTextField(
            value = "",
            onValueChange = { },
            label = { Text("Password") },
            modifier = Modifier.constrainAs(passwordField) {
                top.linkTo(emailField.bottom, 16.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }
        )
        
        // Login Button
        Button(
            onClick = { },
            modifier = Modifier.constrainAs(loginButton) {
                top.linkTo(passwordField.bottom, 24.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }
        ) {
            Text("Log In")
        }
        
        // Signup Text
        TextButton(
            onClick = { },
            modifier = Modifier.constrainAs(signupText) {
                top.linkTo(loginButton.bottom, 16.dp)
                centerHorizontallyTo(parent)
            }
        ) {
            Text("Don't have an account? Sign up")
        }
    }
}
```

### Profile Screen with Complex Layout

```kotlin
@Composable
fun ProfileScreen() {
    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val (coverImage, profileImage, name, bio, followButton, 
             followersCount, followingCount, postsCount) = createRefs()
        
        val coverImageBarrier = createBottomBarrier(coverImage)
        
        // Cover Image
        AsyncImage(
            model = "cover_image_url",
            contentDescription = "Cover Image",
            modifier = Modifier.constrainAs(coverImage) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
                height = Dimension.value(200.dp)
            },
            contentScale = ContentScale.Crop
        )
        
        // Profile Image (overlapping cover)
        AsyncImage(
            model = "profile_image_url",
            contentDescription = "Profile Image",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .border(4.dp, Color.White, CircleShape)
                .constrainAs(profileImage) {
                    top.linkTo(coverImage.bottom)
                    bottom.linkTo(coverImage.bottom)
                    start.linkTo(parent.start, 16.dp)
                }
        )
        
        // Follow Button
        Button(
            onClick = { },
            modifier = Modifier.constrainAs(followButton) {
                top.linkTo(coverImageBarrier, 16.dp)
                end.linkTo(parent.end, 16.dp)
            }
        ) {
            Text("Follow")
        }
        
        // Name
        Text(
            text = "John Doe",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.constrainAs(name) {
                top.linkTo(profileImage.bottom, 8.dp)
                start.linkTo(parent.start, 16.dp)
            }
        )
        
        // Bio
        Text(
            text = "Software Engineer | Android Developer | Jetpack Compose enthusiast",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.constrainAs(bio) {
                top.linkTo(name.bottom, 4.dp)
                start.linkTo(parent.start, 16.dp)
                end.linkTo(parent.end, 16.dp)
                width = Dimension.fillToConstraints
            }
        )
        
        // Stats Row
        Row(
            modifier = Modifier.constrainAs(postsCount) {
                top.linkTo(bio.bottom, 16.dp)
                start.linkTo(parent.start, 16.dp)
                end.linkTo(parent.end, 16.dp)
                width = Dimension.fillToConstraints
            },
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn("Posts", "142")
            StatColumn("Followers", "1.2k")  
            StatColumn("Following", "320")
        }
    }
}

@Composable
fun StatColumn(label: String, count: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

### Responsive Layout with Barriers

```kotlin
@Composable
fun ResponsiveLayout() {
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (image, title, description, price, addButton) = createRefs()
        
        // Create barrier to handle dynamic content
        val textBarrier = createEndBarrier(title, description)
        
        // Product Image
        AsyncImage(
            model = "product_image_url",
            contentDescription = "Product",
            modifier = Modifier
                .size(120.dp)
                .constrainAs(image) {
                    top.linkTo(parent.top, 16.dp)
                    start.linkTo(parent.start, 16.dp)
                }
        )
        
        // Title (can be long)
        Text(
            text = "Very Long Product Title That Might Wrap Multiple Lines",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(image.top)
                start.linkTo(image.end, 16.dp)
                end.linkTo(textBarrier, 16.dp)
                width = Dimension.fillToConstraints
            }
        )
        
        // Description (can be long)
        Text(
            text = "Product description that can also be quite long and span multiple lines",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.constrainAs(description) {
                top.linkTo(title.bottom, 8.dp)
                start.linkTo(image.end, 16.dp)
                end.linkTo(textBarrier, 16.dp)
                width = Dimension.fillToConstraints
            }
        )
        
        // Price
        Text(
            text = "$29.99",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.constrainAs(price) {
                top.linkTo(parent.top, 16.dp)
                end.linkTo(parent.end, 16.dp)
                start.linkTo(textBarrier)
            }
        )
        
        // Add Button
        Button(
            onClick = { },
            modifier = Modifier.constrainAs(addButton) {
                top.linkTo(price.bottom, 8.dp)
                end.linkTo(parent.end, 16.dp)
                start.linkTo(textBarrier)
                width = Dimension.fillToConstraints
            }
        ) {
            Text("Add to Cart")
        }
    }
}
```

## Advanced Features

### Barriers

Barriers create virtual boundaries based on multiple widgets.

```kotlin
@Composable
fun BarrierExample() {
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (text1, text2, text3, button) = createRefs()
        
        // Create barrier after the longest text
        val textBarrier = createEndBarrier(text1, text2, text3)
        
        Text("Short", modifier = Modifier.constrainAs(text1) {
            top.linkTo(parent.top)
            start.linkTo(parent.start)
        })
        
        Text("Medium length text", modifier = Modifier.constrainAs(text2) {
            top.linkTo(text1.bottom)
            start.linkTo(parent.start)
        })
        
        Text("Very very long text that extends", modifier = Modifier.constrainAs(text3) {
            top.linkTo(text2.bottom)
            start.linkTo(parent.start)
        })
        
        Button(
            onClick = { },
            modifier = Modifier.constrainAs(button) {
                start.linkTo(textBarrier, 16.dp)
                centerVerticallyTo(parent)
            }
        ) {
            Text("Action")
        }
    }
}
```

### Chains

Create chains of widgets that are linked together.

```kotlin
@Composable
fun ChainExample() {
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (button1, button2, button3) = createRefs()
        
        // Create horizontal chain
        createHorizontalChain(
            button1, button2, button3,
            chainStyle = ChainStyle.SpreadInside
        )
        
        Button(
            onClick = { },
            modifier = Modifier.constrainAs(button1) {
                centerVerticallyTo(parent)
            }
        ) {
            Text("Button 1")
        }
        
        Button(
            onClick = { },
            modifier = Modifier.constrainAs(button2) {
                centerVerticallyTo(parent)
            }
        ) {
            Text("Button 2")
        }
        
        Button(
            onClick = { },
            modifier = Modifier.constrainAs(button3) {
                centerVerticallyTo(parent)
            }
        ) {
            Text("Button 3")
        }
    }
}
```

### Chain Styles

```kotlin
// Chain style options
ChainStyle.Spread        // Default - spread evenly
ChainStyle.SpreadInside  // Spread with no margins at edges
ChainStyle.Packed        // Pack together in center
```

### Bias

Control positioning within available space using bias (0.0f to 1.0f).

```kotlin
@Composable
fun BiasExample() {
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val button = createRef()
        
        Button(
            onClick = { },
            modifier = Modifier.constrainAs(button) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                
                // Bias towards start (left) and top
                horizontalBias = 0.2f  // 20% from start
                verticalBias = 0.3f    // 30% from top
            }
        ) {
            Text("Biased Button")
        }
    }
}
```

## Performance Tips

### Use ConstraintSet for Dynamic Layouts

```kotlin
@Composable
fun DynamicConstraintLayout(isExpanded: Boolean) {
    val constraintSet = remember(isExpanded) {
        ConstraintSet {
            val content = createRefFor("content")
            
            constrain(content) {
                if (isExpanded) {
                    centerTo(parent)
                    width = Dimension.fillToConstraints
                } else {
                    top.linkTo(parent.top)
                    centerHorizontallyTo(parent)
                    width = Dimension.wrapContent
                }
            }
        }
    }
    
    ConstraintLayout(
        constraintSet = constraintSet,
        modifier = Modifier.fillMaxSize()
    ) {
        Card(
            modifier = Modifier.layoutId("content")
        ) {
            Text(
                text = if (isExpanded) "Expanded Content" else "Collapsed",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
```

### Reuse ConstraintSets

```kotlin
object AppConstraintSets {
    val loginConstraintSet = ConstraintSet {
        // Define login layout constraints
    }
    
    val profileConstraintSet = ConstraintSet {
        // Define profile layout constraints  
    }
}

@Composable
fun ReusableConstraintLayout() {
    ConstraintLayout(
        constraintSet = AppConstraintSets.loginConstraintSet,
        modifier = Modifier.fillMaxSize()
    ) {
        // Composables with layoutId
    }
}
```

## Best Practices

### 1. When to Use ConstraintLayout

- Complex layouts with multiple relationships between elements
- When you need precise positioning and sizing control
- Layouts that would require multiple nested Row/Column/Box combinations
- Responsive designs that adapt to different screen sizes

### 2. When NOT to Use ConstraintLayout

- Simple linear layouts (use Column/Row instead)
- Basic stacking (use Box instead)
- Lists or grids (use LazyColumn/LazyVerticalGrid)

### 3. Performance Guidelines

- Use `remember` for dynamic ConstraintSets
- Prefer inline DSL for static layouts
- Use JSON ConstraintSets for external/dynamic constraint definitions
- Avoid creating new ConstraintSets in recomposition-heavy code

### 4. Design Guidelines

- It's better to use start and end constraints, rather than left and right
- Use createRefs() to create constraint references for your composables
- Use a guideline if you need to position your composable relative to a specific place on the screen

### 5. Debugging Tips

- Use Layout Inspector in Android Studio
- Add background colors temporarily to see constraint boundaries
- Check for circular dependencies in constraints
- Verify that all constraints are properly defined

## Migration from View-based ConstraintLayout

### Common Equivalents

```kotlin
// View XML -> Compose
app:layout_constraintTop_toTopOf="parent"          -> top.linkTo(parent.top)
app:layout_constraintStart_toEndOf="@id/view"      -> start.linkTo(view.end)
app:layout_constraintWidth_percent="0.5"           -> width = Dimension.percent(0.5f)
app:layout_constraintHorizontal_bias="0.3"         -> horizontalBias = 0.3f
app:layout_constraintDimensionRatio="16:9"         -> width = Dimension.ratio("16:9")
```

ConstraintLayout in Compose provides powerful layout capabilities while maintaining the flexibility and performance benefits of the Compose UI toolkit. It's especially useful for complex layouts that require precise positioning and responsive behavior.