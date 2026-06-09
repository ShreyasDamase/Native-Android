# Jetpack Compose Alignment & Arrangement Guide

## Core Imports

```kotlin
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
```

## Layout System Overview

Compose transforms state into UI elements via composition, layout, and drawing of elements. The layout system has two main goals: high performance and the ability to easily write custom layouts.

In the layout model, parents measure before their children, but are sized and placed after their children. This single-pass measurement achieves high performance.

## Standard Layout Components

### Column Layout

Use Column to place items vertically on the screen.

```kotlin
@Composable
fun VerticalLayout() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,      // How children are arranged vertically
        horizontalAlignment = Alignment.CenterHorizontally  // How children align horizontally
    ) {
        Text("First Item")
        Text("Second Item")
        Text("Third Item")
    }
}
```

#### Column Vertical Arrangements

```kotlin
// Spacing arrangements
Arrangement.Top           // Items at top
Arrangement.Bottom        // Items at bottom  
Arrangement.Center        // Items centered
Arrangement.SpaceEvenly   // Equal space between and around items
Arrangement.SpaceBetween  // Space between items, no space at edges
Arrangement.SpaceAround   // Equal space around items

// Fixed spacing
Arrangement.spacedBy(8.dp)  // Fixed space between items
```

#### Column Horizontal Alignments

```kotlin
Alignment.Start           // Left align (or right in RTL)
Alignment.End             // Right align (or left in RTL)  
Alignment.CenterHorizontally  // Center horizontally
```

### Row Layout

Use Row to place items horizontally on the screen.

```kotlin
@Composable
fun HorizontalLayout() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,     // How children are arranged horizontally
        verticalAlignment = Alignment.CenterVertically  // How children align vertically
    ) {
        Text("Left")
        Text("Center") 
        Text("Right")
    }
}
```

#### Row Horizontal Arrangements

```kotlin
// Spacing arrangements
Arrangement.Start         // Items at start (left in LTR)
Arrangement.End           // Items at end (right in LTR)
Arrangement.Center        // Items centered
Arrangement.SpaceEvenly   // Equal space between and around items
Arrangement.SpaceBetween  // Space between items, no space at edges
Arrangement.SpaceAround   // Equal space around items

// Fixed spacing
Arrangement.spacedBy(12.dp)  // Fixed space between items
```

#### Row Vertical Alignments

```kotlin
Alignment.Top             // Align to top
Alignment.Bottom          // Align to bottom
Alignment.CenterVertically    // Center vertically
```

### Box Layout

Use Box to put elements on top of another. Box also supports configuring specific alignment of the elements it contains.

```kotlin
@Composable
fun StackedLayout() {
    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center  // Default alignment for all children
    ) {
        // Background element
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Blue)
        )
        
        // Foreground elements with individual alignment
        Text(
            text = "Top Start",
            modifier = Modifier.align(Alignment.TopStart)
        )
        
        Text(
            text = "Center", 
            modifier = Modifier.align(Alignment.Center)
        )
        
        Text(
            text = "Bottom End",
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}
```

#### Box Alignment Options

```kotlin
// Corner alignments
Alignment.TopStart        Alignment.TopCenter        Alignment.TopEnd
Alignment.CenterStart     Alignment.Center           Alignment.CenterEnd  
Alignment.BottomStart     Alignment.BottomCenter     Alignment.BottomEnd
```

## Advanced Alignment Techniques

### Weight Modifier for Proportional Space

```kotlin
@Composable
fun WeightedLayout() {
    Row {
        Text(
            text = "Small",
            modifier = Modifier.weight(1f)  // Takes 1/4 of space
        )
        Text(
            text = "Large", 
            modifier = Modifier.weight(3f)  // Takes 3/4 of space
        )
    }
}
```

### Baseline Alignment

```kotlin
@Composable
fun BaselineAlignedRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Small Text",
            fontSize = 12.sp
        )
        Text(
            text = "LARGE TEXT",
            fontSize = 24.sp,
            modifier = Modifier.alignByBaseline()  // Align by text baseline
        )
    }
}
```

### Individual Child Alignment

```kotlin
@Composable
fun IndividualAlignment() {
    Column {
        Text(
            text = "Start Aligned",
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            text = "Center Aligned",
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = "End Aligned", 
            modifier = Modifier.align(Alignment.End)
        )
    }
}
```

## Responsive Layouts

### BoxWithConstraints

In order to know the constraints coming from the parent and design the layout accordingly, you can use BoxWithConstraints.

```kotlin
@Composable
fun ResponsiveLayout() {
    BoxWithConstraints {
        if (maxWidth < 600.dp) {
            // Mobile layout
            Column {
                Text("Mobile Layout")
                Text("Max width: $maxWidth")
            }
        } else {
            // Tablet/Desktop layout  
            Row {
                Text("Desktop Layout")
                Text("Max width: $maxWidth")
            }
        }
    }
}
```

### Constraint-Based Alignment

```kotlin
@Composable
fun ConstraintBasedLayout() {
    BoxWithConstraints {
        val isCompact = maxWidth < 400.dp
        
        Column(
            horizontalAlignment = if (isCompact) {
                Alignment.CenterHorizontally
            } else {
                Alignment.Start
            }
        ) {
            Text("Responsive alignment based on width")
            Text("Current width: $maxWidth")
        }
    }
}
```

## Flow Layouts (Latest Addition)

FlowRow and FlowColumn are composables that are similar to Row and Column, but differ in that items flow into the next line when the container runs out of space.

```kotlin
@Composable
fun FlowLayoutExample() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(10) { index ->
            Text(
                text = "Item $index",
                modifier = Modifier
                    .background(Color.LightGray)
                    .padding(8.dp)
            )
        }
    }
}

@Composable 
fun FlowColumnExample() {
    FlowColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        maxItemsInEachColumn = 3
    ) {
        repeat(10) { index ->
            Text(
                text = "Item $index",
                modifier = Modifier
                    .background(Color.LightBlue)
                    .padding(8.dp)
            )
        }
    }
}
```

## Custom Alignment Lines

The Compose layout model lets you use AlignmentLine to create custom alignment lines that can be used by parent layouts to align and position their children.

```kotlin
// Custom alignment line
val CustomAlignmentLine = HorizontalAlignmentLine(::min)

@Composable
fun CustomAlignmentExample() {
    Row {
        Text(
            text = "Aligned Text 1",
            modifier = Modifier.alignBy(CustomAlignmentLine)
        )
        Text(
            text = "Aligned Text 2", 
            modifier = Modifier.alignBy(CustomAlignmentLine)
        )
    }
}
```

## Practical Examples

### Navigation Bar Layout

```kotlin
@Composable
fun NavigationBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { }) {
            Icon(Icons.Default.Menu, contentDescription = "Menu")
        }
        
        Text("App Title")
        
        IconButton(onClick = { }) {
            Icon(Icons.Default.Search, contentDescription = "Search")
        }
    }
}
```

### Card Layout with Mixed Alignment

```kotlin
@Composable
fun UserCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "John Doe",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Online",
                    color = Color.Green
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "User bio goes here...",
                modifier = Modifier.align(Alignment.Start)
            )
        }
    }
}
```

### Centered Loading Screen

```kotlin
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Loading...")
        }
    }
}
```

## Performance Tips

### Efficient Arrangement Usage

```kotlin
// ✅ Good - Static arrangement
@Composable
fun EfficientLayout() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)  // Static spacing
    ) {
        // Content
    }
}

// ❌ Avoid - Dynamic arrangement in recomposition
@Composable  
fun InefficientLayout() {
    Column(
        verticalArrangement = Arrangement.spacedBy(
            if (someState) 8.dp else 16.dp  // Changes on every state change
        )
    ) {
        // Content
    }
}
```

### Remember Alignment Values

```kotlin
@Composable
fun RememberedAlignment(isExpanded: Boolean) {
    val alignment = remember(isExpanded) {
        if (isExpanded) Alignment.CenterHorizontally else Alignment.Start
    }
    
    Column(
        horizontalAlignment = alignment
    ) {
        // Content
    }
}
```

## Common Patterns

### Center Content with Max Width

```kotlin
@Composable
fun CenteredContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 400.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Content with max width but centered
        }
    }
}
```

### Bottom Sheet Style Layout

```kotlin
@Composable
fun BottomSheetLayout() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        MainContent()
        
        // Bottom sheet
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Bottom Sheet Content")
            }
        }
    }
}
```

## Best Practices

1. **Use appropriate layout for content structure**
    
    - Row for horizontal arrangements
    - Column for vertical arrangements
    - Box for stacking/overlaying
2. **Consider responsive design**
    
    - Use BoxWithConstraints for adaptive layouts
    - Test different screen sizes and orientations
3. **Leverage weight for flexible sizing**
    
    - Use weight() for proportional space distribution
    - Combine with fillMaxWidth/Height as needed
4. **Performance considerations**
    
    - Avoid complex calculations in arrangement/alignment parameters
    - Use remember for dynamic alignment values
5. **Accessibility**
    
    - Ensure proper reading order with logical layout structure
    - Test with TalkBack and other accessibility services