# Jetpack Compose Column, Row & Box Layouts Guide

## Core Imports

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
```

## Layout System Fundamentals

The Compose layout system follows a single-pass measurement where:

1. **Parents measure before their children**
2. **Parents are sized and placed after their children**
3. Each node measures itself, then measures children recursively
4. Leaf nodes are sized and placed first, with resolved sizes passed back up the tree

This approach achieves high performance by measuring children only once.

## Column Layout

Column places items vertically on the screen, similar to a LinearLayout with vertical orientation.

### Basic Column Structure

```kotlin
@Composable
fun BasicColumn() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,      // How children are spaced vertically
        horizontalAlignment = Alignment.CenterHorizontally  // How children align horizontally
    ) {
        Text("First Item")
        Text("Second Item") 
        Text("Third Item")
    }
}
```

### Column Parameters

```kotlin
Column(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
)
```

### Vertical Arrangements

```kotlin
// Basic positioning
Arrangement.Top           // Items at top (default)
Arrangement.Bottom        // Items at bottom
Arrangement.Center        // Items centered vertically

// Space distribution
Arrangement.SpaceEvenly   // Equal space between and around items
Arrangement.SpaceBetween  // Space between items, no space at edges  
Arrangement.SpaceAround   // Equal space around each item

// Fixed spacing
Arrangement.spacedBy(8.dp)              // Fixed space between items
Arrangement.spacedBy(8.dp, Alignment.CenterVertically)  // Fixed space with alignment
```

### Horizontal Alignments

```kotlin
Alignment.Start              // Left align (or right in RTL)
Alignment.End                // Right align (or left in RTL)  
Alignment.CenterHorizontally // Center horizontally (default)
```

### Column Examples

#### Simple Vertical List

```kotlin
@Composable
fun VerticalList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Item 1", style = MaterialTheme.typography.headlineSmall)
        Text("Item 2", style = MaterialTheme.typography.bodyLarge)
        Text("Item 3", style = MaterialTheme.typography.bodyMedium)
    }
}
```

#### Form Layout

```kotlin
@Composable
fun FormLayout() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = "",
            onValueChange = { },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        
        TextField(
            value = "",
            onValueChange = { },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
    }
}
```

#### Weight Distribution in Column

```kotlin
@Composable
fun WeightedColumn() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)  // Takes 1/4 of available space
                .background(Color.Red)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f)  // Takes 2/4 of available space
                .background(Color.Green)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)  // Takes 1/4 of available space
                .background(Color.Blue)
        )
    }
}
```

## Row Layout

Row places items horizontally on the screen, similar to a LinearLayout with horizontal orientation.

### Basic Row Structure

```kotlin
@Composable
fun BasicRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,     // How children are spaced horizontally
        verticalAlignment = Alignment.CenterVertically  // How children align vertically
    ) {
        Text("Left")
        Text("Center")
        Text("Right")
    }
}
```

### Row Parameters

```kotlin
Row(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit
)
```

### Horizontal Arrangements

```kotlin
// Basic positioning
Arrangement.Start         // Items at start (left in LTR) - default
Arrangement.End           // Items at end (right in LTR)
Arrangement.Center        // Items centered horizontally

// Space distribution  
Arrangement.SpaceEvenly   // Equal space between and around items
Arrangement.SpaceBetween  // Space between items, no space at edges
Arrangement.SpaceAround   // Equal space around each item

// Fixed spacing
Arrangement.spacedBy(12.dp)               // Fixed space between items
Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)  // Fixed space with alignment
```

### Vertical Alignments

```kotlin
Alignment.Top             // Align to top (default)
Alignment.Bottom          // Align to bottom
Alignment.CenterVertically    // Center vertically
```

### Row Examples

#### Navigation Bar

```kotlin
@Composable
fun NavigationBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { }) {
            Icon(Icons.Default.Menu, contentDescription = "Menu")
        }
        
        Text(
            text = "App Title",
            style = MaterialTheme.typography.headlineSmall
        )
        
        IconButton(onClick = { }) {
            Icon(Icons.Default.Search, contentDescription = "Search")
        }
    }
}
```

#### User Profile Row

```kotlin
@Composable
fun UserProfileRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = "profile_url",
            contentDescription = "Profile",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        
        Column(
            modifier = Modifier.weight(1f)  // Takes remaining space
        ) {
            Text(
                text = "John Doe",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Software Engineer",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Button(onClick = { }) {
            Text("Follow")
        }
    }
}
```

#### Weight Distribution in Row

```kotlin
@Composable
fun WeightedRow() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .weight(1f)      // Takes 1/6 of available space
                .height(100.dp)
                .background(Color.Red)
        )
        
        Box(
            modifier = Modifier
                .weight(3f)      // Takes 3/6 of available space
                .height(100.dp)
                .background(Color.Green)
        )
        
        Box(
            modifier = Modifier
                .weight(2f)      // Takes 2/6 of available space
                .height(100.dp)
                .background(Color.Blue)
        )
    }
}
```

## Box Layout

Box puts elements on top of one another (stacking/overlaying). It supports configuring specific alignment of the elements it contains.

### Basic Box Structure

```kotlin
@Composable
fun BasicBox() {
    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center  // Default alignment for all children
    ) {
        // Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray)
        )
        
        // Foreground content
        Text("Centered Text")
    }
}
```

### Box Parameters

```kotlin
Box(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    content: @Composable BoxScope.() -> Unit
)
```

### Box Alignment Options

```kotlin
// Corner alignments
Alignment.TopStart        Alignment.TopCenter        Alignment.TopEnd
Alignment.CenterStart     Alignment.Center           Alignment.CenterEnd  
Alignment.BottomStart     Alignment.BottomCenter     Alignment.BottomEnd
```

### Box Examples

#### Overlay Content

```kotlin
@Composable
fun OverlayBox() {
    Box(modifier = Modifier.size(300.dp)) {
        // Background image
        AsyncImage(
            model = "background_image_url",
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Overlay gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )
        
        // Overlay text
        Text(
            text = "Overlay Text",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.BottomStart)
                .padding(16.dp)
        )
        
        // Top-right badge
        Badge(
            modifier = Modifier.align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Text("NEW")
        }
    }
}
```

#### Card with Badge

```kotlin
@Composable
fun CardWithBadge() {
    Box {
        Card(
            modifier = Modifier
                .width(200.dp)
                .height(120.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Product Title",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "$29.99",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Badge positioned at top-right corner
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 8.dp, y = (-8).dp),
            color = Color.Red,
            shape = CircleShape
        ) {
            Text(
                text = "50%",
                color = Color.White,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
```

#### Loading Overlay

```kotlin
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    content: @Composable () -> Unit
) {
    Box {
        content()
        
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) { }, // Prevent clicks
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text(
                        text = "Loading...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
```

## Flow Layouts (Latest Addition)

FlowRow and FlowColumn are similar to Row and Column, but items flow into the next line when the container runs out of space.

### FlowRow

```kotlin
@Composable
fun ChipFlowRow() {
    FlowRow(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val chips = listOf("Price: High to Low", "Avg rating: 4+", "Free breakfast", 
                          "Free cancellation", "£50 pn", "Pool", "Gym", "WiFi")
        
        chips.forEach { chip ->
            FilterChip(
                onClick = { },
                label = { Text(chip) },
                selected = false
            )
        }
    }
}
```

### FlowColumn

```kotlin
@Composable
fun VerticalFlowLayout() {
    FlowColumn(
        modifier = Modifier
            .fillMaxHeight()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        maxItemsInEachColumn = 3
    ) {
        repeat(10) { index ->
            Card(
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = "Item $index",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
```

### FlowRow Grid with Weights

```kotlin
@Composable
fun FlowRowGrid() {
    val rows = 3
    val columns = 3
    
    FlowRow(
        modifier = Modifier.padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        maxItemsInEachRow = rows
    ) {
        val itemModifier = Modifier
            .padding(4.dp)
            .height(80.dp)
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary)
            
        repeat(rows * columns) {
            Spacer(modifier = itemModifier)
        }
    }
}
```

## Advanced Layout Techniques

### Custom Alignment

```kotlin
@Composable
fun CustomAlignmentExample() {
    Column {
        Row {
            Text(
                text = "Baseline 1",
                fontSize = 16.sp,
                modifier = Modifier.alignByBaseline()
            )
            Text(
                text = "BASELINE 2",
                fontSize = 24.sp,
                modifier = Modifier.alignByBaseline()
            )
        }
    }
}
```

### Nested Layouts

```kotlin
@Composable
fun NestedLayoutExample() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Header", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = { }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
        
        // Content Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Main content
            LazyColumn {
                items(20) { index ->
                    ListItem(
                        headlineContent = { Text("Item $index") }
                    )
                }
            }
            
            // Floating Action Button
            FloatingActionButton(
                onClick = { },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
        
        // Bottom Navigation Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(3) { index ->
                TextButton(onClick = { }) {
                    Text("Tab $index")
                }
            }
        }
    }
}
```

## Performance Best Practices

### Avoid Recomposition in Layout Parameters

```kotlin
// ❌ Bad - Creates new arrangement on every recomposition
@Composable
fun BadLayout(isExpanded: Boolean) {
    Column(
        verticalArrangement = if (isExpanded) Arrangement.SpaceEvenly else Arrangement.Top
    ) {
        // Content
    }
}

// ✅ Good - Remember the arrangement
@Composable
fun GoodLayout(isExpanded: Boolean) {
    val arrangement = remember(isExpanded) {
        if (isExpanded) Arrangement.SpaceEvenly else Arrangement.Top
    }
    
    Column(
        verticalArrangement = arrangement
    ) {
        // Content
    }
}
```

### Efficient Modifier Chains

```kotlin
// ✅ Good - Stable modifier reference
@Composable
fun EfficientModifiers() {
    val baseModifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    
    Column(modifier = baseModifier) {
        Text("Item 1", modifier = baseModifier)
        Text("Item 2", modifier = baseModifier)
    }
}
```

### Layout-Specific Weight Usage

```kotlin
// ✅ Proper weight usage
@Composable
fun ProperWeights() {
    Row {
        // This will take proportional space
        Text(
            text = "Flexible",
            modifier = Modifier.weight(1f)
        )
        
        // This will take only needed space
        Button(onClick = { }) {
            Text("Fixed")
        }
    }
}
```

## Common Layout Patterns

### Three-Column Layout

```kotlin
@Composable
fun ThreeColumnLayout() {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Left sidebar
        Column(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text("Sidebar")
        }
        
        // Main content
        Column(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
        ) {
            Text("Main Content")
        }
        
        // Right panel
        Column(
            modifier = Modifier
                .weight(0.2f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text("Panel")
        }
    }
}
```

### Master-Detail Layout

```kotlin
@Composable
fun MasterDetailLayout() {
    Row(modifier = Modifier.fillMaxSize()) {
        // Master list
        LazyColumn(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
        ) {
            items(20) { index ->
                ListItem(
                    headlineContent = { Text("Item $index") },
                    modifier = Modifier.clickable { }
                )
            }
        }
        
        // Detail view
        Box(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("Detail View")
        }
    }
}
```

## Debugging Layout Issues

### Visual Layout Debugging

```kotlin
fun Modifier.debugBorder(color: Color = Color.Red) = 
    this.border(1.dp, color)

@Composable
fun DebugLayout() {
    Column(
        modifier = Modifier.debugBorder(Color.Blue)
    ) {
        Row(
            modifier = Modifier.debugBorder(Color.Green)
        ) {
            Text(
                text = "Debug Text",
                modifier = Modifier.debugBorder(Color.Red)
            )
        }
    }
}
```

### Layout Inspector Tips

- Use Android Studio's Layout Inspector to visualize the layout tree
- Check for unnecessary nesting of layouts
- Verify that weight distributions add up correctly
- Ensure proper alignment and arrangement settings

## Common Mistakes to Avoid

1. **Over-nesting layouts** - Avoid unnecessary Column/Row nesting
2. **Incorrect weight usage** - Weights only work within Row/Column parents
3. **Forgetting fillMaxWidth/Height** - Child elements might not expand as expected
4. **Mixing fixed and flexible sizing** - Be careful with weight + fixed size combinations
5. **Performance issues** - Avoid creating new modifiers in recomposition-heavy code