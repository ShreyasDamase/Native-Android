# LazyColumn & LazyRow - Complete Beginner's Guide

## 🎯 What are LazyColumn and LazyRow?

**LazyColumn** and **LazyRow** are Jetpack Compose's efficient scrollable list components. They are called "lazy" because they only compose and render items that are currently visible on screen, making them perfect for large datasets.

### Key Differences from Column/Row:

- **Column/Row**: Compose ALL items at once (memory intensive for large lists)
- **LazyColumn/LazyRow**: Compose only VISIBLE items (memory efficient)

## 🧠 Core Theory - Why "Lazy"?

### The Problem with Regular Column/Row

```kotlin
// ❌ BAD - This creates 10,000 Text composables in memory!
Column {
    repeat(10000) { index ->
        Text("Item $index")
    }
}
// Result: App crashes or becomes very slow
```

### The Solution - Lazy Composables

```kotlin
// ✅ GOOD - Only creates visible Text composables!
LazyColumn {
    items(10000) { index ->
        Text("Item $index")
    }
}
// Result: Smooth scrolling, low memory usage
```

### How Lazy Loading Works:

1. **Viewport**: Only items in the visible area are composed
2. **Recycling**: As you scroll, off-screen items are destroyed
3. **Composition**: New items entering the screen are created
4. **Memory Efficient**: Constant memory usage regardless of list size

## 📋 Basic Syntax and Structure

### LazyColumn Structure

```kotlin
LazyColumn(
    modifier = Modifier,
    state = rememberLazyListState(),
    contentPadding = PaddingValues(),
    reverseLayout = false,
    verticalArrangement = Arrangement.Top,
    horizontalAlignment = Alignment.Start,
    flingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled = true
) {
    // Content goes here using DSL
    item { /* Single item */ }
    items(list) { item -> /* Multiple items */ }
    itemsIndexed(list) { index, item -> /* Items with index */ }
}
```

### LazyRow Structure

```kotlin
LazyRow(
    modifier = Modifier,
    state = rememberLazyListState(),
    contentPadding = PaddingValues(),
    reverseLayout = false,
    horizontalArrangement = Arrangement.Start,
    verticalAlignment = Alignment.Top,
    flingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled = true
) {
    // Same content DSL as LazyColumn
    item { /* Single item */ }
    items(list) { item -> /* Multiple items */ }
}
```

## 🔤 LazyColumn - Vertical Scrolling Lists

### Basic LazyColumn

```kotlin
@Composable
fun BasicLazyColumn() {
    LazyColumn {
        items(100) { index ->
            Text(
                text = "Item $index",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
```

### LazyColumn with Different Content Types

```kotlin
@Composable
fun MixedContentLazyColumn() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Single header item
        item {
            Text(
                text = "Header",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray)
                    .padding(16.dp)
            )
        }
        
        // List of items from data
        val itemsList = (1..50).toList()
        items(itemsList) { number ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "Card Item $number",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // Another single item
        item {
            Text(
                text = "Footer",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.LightGray)
                    .padding(16.dp)
            )
        }
    }
}
```

## ↔️ LazyRow - Horizontal Scrolling Lists

### Basic LazyRow

```kotlin
@Composable
fun BasicLazyRow() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(20) { index ->
            Card(
                modifier = Modifier
                    .width(120.dp)
                    .height(80.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Blue
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Item $index",
                        color = Color.White
                    )
                }
            }
        }
    }
}
```

### Horizontal Image Gallery

```kotlin
@Composable
fun ImageGallery() {
    val images = listOf("🌅", "🏔️", "🌊", "🌸", "🌟", "🦋", "🌈", "🍎")
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(images) { emoji ->
            Card(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.LightBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 40.sp
                    )
                }
            }
        }
    }
}
```

## 📊 Working with Data Lists

### Using Data Classes

```kotlin
data class Person(
    val id: Int,
    val name: String,
    val age: Int,
    val profession: String
)

@Composable
fun PersonList() {
    val people = listOf(
        Person(1, "Alice", 25, "Developer"),
        Person(2, "Bob", 30, "Designer"),
        Person(3, "Charlie", 28, "Manager"),
        Person(4, "Diana", 32, "Analyst")
    )
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(people) { person ->
            PersonCard(person = person)
        }
    }
}

@Composable
fun PersonCard(person: Person) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Age: ${person.age}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Profession: ${person.profession}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}
```

### Using itemsIndexed for Index Access

```kotlin
@Composable
fun IndexedList() {
    val fruits = listOf("Apple", "Banana", "Cherry", "Date", "Elderberry")
    
    LazyColumn {
        itemsIndexed(fruits) { index, fruit ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "${index + 1}. $fruit")
                Text(
                    text = if (index % 2 == 0) "Even" else "Odd",
                    color = if (index % 2 == 0) Color.Blue else Color.Red
                )
            }
        }
    }
}
```

## ⚙️ LazyListState - Controlling Scroll Behavior

### Basic State Management

```kotlin
@Composable
fun ScrollControlExample() {
    val listState = rememberLazyListState()
    
    Column {
        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    // Scroll to top
                    listState.animateScrollToItem(0)
                }
            ) {
                Text("Scroll to Top")
            }
            
            Button(
                onClick = {
                    // Scroll to bottom
                    listState.animateScrollToItem(99)
                }
            ) {
                Text("Scroll to Bottom")
            }
        }
        
        // The lazy column
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(100) { index ->
                Text(
                    text = "Item $index",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
```

### Advanced State Usage - Show/Hide FAB Based on Scroll

```kotlin
@Composable
fun ScrollAwareFAB() {
    val listState = rememberLazyListState()
    
    // Check if we're scrolled past first item
    val showFAB by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(100) { index ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Scroll Item $index",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        
        // FAB that appears when scrolled
        AnimatedVisibility(
            visible = showFAB,
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            FloatingActionButton(
                onClick = {
                    listState.animateScrollToItem(0)
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Scroll to top"
                )
            }
        }
    }
}
```

## 🎨 Styling and Layout Options

### Content Padding and Spacing

```kotlin
@Composable
fun StyledLazyColumn() {
    LazyColumn(
        // Padding around entire content
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = 16.dp,
            start = 8.dp,
            end = 8.dp
        ),
        // Space between items
        verticalArrangement = Arrangement.spacedBy(12.dp),
        // Horizontal alignment of items
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(20) { index ->
            Card(
                modifier = Modifier
                    .width(200.dp)
                    .height(80.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Centered Item $index")
                }
            }
        }
    }
}
```

### Reverse Layout

```kotlin
@Composable
fun ReversedLazyColumn() {
    // Items appear from bottom to top (like chat messages)
    LazyColumn(
        reverseLayout = true,
        modifier = Modifier.fillMaxSize()
    ) {
        items(50) { index ->
            Text(
                text = "Message ${50 - index}",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (index % 2 == 0) Color.LightBlue else Color.LightGray
                    )
                    .padding(16.dp)
            )
        }
    }
}
```

## ❌ Common Beginner Mistakes & Solutions

### Mistake 1: Using Column/Row for Large Lists

```kotlin
// ❌ NEVER DO THIS - Will crash with large datasets!
Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
    repeat(1000) { index ->
        Text("Item $index")  // Creates 1000 composables in memory!
    }
}

// ✅ DO THIS - Efficient memory usage
LazyColumn {
    items(1000) { index ->
        Text("Item $index")  // Only visible items in memory
    }
}
```

### Mistake 2: Nested Scrolling Without Proper Configuration

```kotlin
// ❌ PROBLEM - Scrolling conflicts
LazyColumn {
    item {
        LazyRow {  // Nested lazy composable
            items(20) { Text("Item $it") }
        }
    }
}

// ✅ SOLUTION - Specify height or use nestedScroll
LazyColumn {
    item {
        LazyRow(
            modifier = Modifier.height(100.dp)  // Fixed height
        ) {
            items(20) { Text("Item $it") }
        }
    }
}
```

### Mistake 3: Forgetting Keys for Dynamic Lists

```kotlin
// ❌ BAD - No keys, inefficient recomposition
LazyColumn {
    items(dynamicList) { item ->
        ItemCard(item)
    }
}

// ✅ GOOD - Using keys for better performance
LazyColumn {
    items(
        items = dynamicList,
        key = { item -> item.id }  // Use unique identifier
    ) { item ->
        ItemCard(item)
    }
}
```

### Mistake 4: Incorrect Sizing in LazyRow

```kotlin
// ❌ PROBLEM - Items too small or inconsistent
LazyRow {
    items(10) { index ->
        Text("Item $index")  // May be too narrow
    }
}

// ✅ SOLUTION - Specify proper dimensions
LazyRow {
    items(10) { index ->
        Text(
            text = "Item $index",
            modifier = Modifier
                .width(120.dp)  // Fixed width
                .padding(8.dp)
        )
    }
}
```

## 🔧 Performance Optimization Tips

### 1. Use Keys for Dynamic Content

```kotlin
data class TodoItem(val id: Int, val text: String, val completed: Boolean)

@Composable
fun TodoList(todos: List<TodoItem>) {
    LazyColumn {
        items(
            items = todos,
            key = { todo -> todo.id }  // Crucial for reordering/updates
        ) { todo ->
            TodoItemCard(todo)
        }
    }
}
```

### 2. Avoid Heavy Operations in Item Content

```kotlin
// ❌ BAD - Heavy computation in item
LazyColumn {
    items(largeList) { item ->
        val expensiveResult = remember { performExpensiveCalculation(item) }
        Text(expensiveResult)
    }
}

// ✅ BETTER - Pre-compute or use coroutines
@Composable
fun OptimizedList(preprocessedList: List<ProcessedItem>) {
    LazyColumn {
        items(preprocessedList) { item ->
            Text(item.displayText)  // Already processed
        }
    }
}
```

### 3. Use Appropriate Content Padding

```kotlin
// ✅ Better for performance - padding on container
LazyColumn(
    contentPadding = PaddingValues(16.dp)
) {
    items(list) { item ->
        Text(item.text)
    }
}

// Rather than padding on each item (less efficient)
LazyColumn {
    items(list) { item ->
        Text(
            text = item.text,
            modifier = Modifier.padding(16.dp)  // Repeated for each item
        )
    }
}
```

## 🧪 Real-World Examples

### 1. News Feed with Mixed Content

```kotlin
@Composable
fun NewsFeed() {
    val articles = remember { generateNewsArticles() }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp) // Account for bottom nav
    ) {
        // Header
        item {
            Text(
                text = "Latest News",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
        
        // Articles
        items(
            items = articles,
            key = { article -> article.id }
        ) { article ->
            NewsArticleCard(article)
        }
        
        // Load more button
        item {
            Button(
                onClick = { /* Load more */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Load More Articles")
            }
        }
    }
}

@Composable
fun NewsArticleCard(article: NewsArticle) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = article.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = article.publishTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}
```

### 2. Shopping Categories (Nested LazyRow in LazyColumn)

```kotlin
@Composable
fun ShoppingCategories() {
    val categories = remember { generateShoppingCategories() }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(categories) { category ->
            CategorySection(category)
        }
    }
}

@Composable
fun CategorySection(category: ShoppingCategory) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Category title
        Text(
            text = category.name,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        
        // Horizontal products list
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(200.dp)  // Important: Fixed height!
        ) {
            items(category.products) { product ->
                ProductCard(product)
            }
        }
    }
}

@Composable
fun ProductCard(product: Product) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .fillMaxHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.LightGray, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("📱") // Placeholder for image
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$${product.price}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Green
            )
        }
    }
}
```

## 📚 Summary & Best Practices

### ✅ Do's:

1. **Use LazyColumn/LazyRow** for lists with more than ~20 items
2. **Provide keys** for dynamic content that can change order
3. **Use contentPadding** instead of padding individual items when possible
4. **Specify fixed dimensions** for LazyRow items
5. **Use LazyListState** to control scroll behavior
6. **Test with large datasets** to ensure performance
7. **Use appropriate arrangements** (spacedBy, SpaceEvenly, etc.)

### ❌ Don'ts:

1. **Don't use Column/Row** with verticalScroll for large lists
2. **Don't nest scrollable composables** without proper height constraints
3. **Don't perform heavy computations** inside item composables
4. **Don't forget accessibility** - provide content descriptions
5. **Don't use LazyColumn/LazyRow** for small, static lists (< 10 items)

### 🎯 Key Takeaways:

- **Lazy = Efficient**: Only visible items are composed
- **State Management**: Use LazyListState for scroll control
- **Performance**: Keys are crucial for dynamic content
- **Sizing**: LazyRow needs explicit item widths
- **Nesting**: Possible but requires careful height management

This comprehensive guide covers everything you need to know about LazyColumn and LazyRow. Practice with these examples and you'll have a solid foundation for building efficient, scrollable lists in Jetpack Compose!


# LazyColumn & LazyRow - Complete Advanced Developer Guide

## 🚀 Advanced Performance Optimization

### 1. Understanding Composition Phases and Optimization

#### Layout Phase Optimization

```kotlin
// ❌ Triggers recomposition on every scroll
@Composable
fun IneffientList(items: List<Item>) {
    val scrollState = rememberLazyListState()
    val isScrolling = scrollState.isScrollInProgress // This triggers recomposition!
    
    LazyColumn(state = scrollState) {
        items(items) { item ->
            ItemCard(item, isHighlighted = isScrolling) // Unnecessary recomposition
        }
    }
}

// ✅ Optimized - Isolate state reads
@Composable
fun OptimizedList(items: List<Item>) {
    val scrollState = rememberLazyListState()
    
    LazyColumn(state = scrollState) {
        items(items, key = { it.id }) { item ->
            ItemCard(item) // Stable, won't recompose unless item changes
        }
    }
}

// Separate composable for scroll-dependent UI
@Composable
fun ScrollDependentHeader(scrollState: LazyListState) {
    val isScrolling by remember { derivedStateOf { scrollState.isScrollInProgress } }
    // Only this composable recomposes on scroll
}
```

#### Advanced Key Strategies

```kotlin
data class Message(
    val id: String,
    val content: String,
    val timestamp: Long,
    val isEdited: Boolean
)

@Composable
fun ChatMessages(messages: List<Message>) {
    LazyColumn {
        items(
            items = messages,
            // Complex key that considers edit state
            key = { message -> "${message.id}_${message.isEdited}" }
        ) { message ->
            MessageBubble(message)
        }
    }
}

// For frequently changing content, use contentType for better recycling
@Composable
fun MixedContentFeed(feedItems: List<FeedItem>) {
    LazyColumn {
        items(
            items = feedItems,
            key = { it.id },
            contentType = { feedItem ->
                when (feedItem) {
                    is TextPost -> "text"
                    is ImagePost -> "image"
                    is VideoPost -> "video"
                    is AdPost -> "ad"
                }
            }
        ) { item ->
            when (item) {
                is TextPost -> TextPostCard(item)
                is ImagePost -> ImagePostCard(item)
                is VideoPost -> VideoPostCard(item)
                is AdPost -> AdCard(item)
            }
        }
    }
}
```

### 2. Memory Management and Resource Optimization

#### Image Loading Optimization

```kotlin
@Composable
fun OptimizedImageList(imageUrls: List<String>) {
    LazyColumn {
        items(imageUrls) { url ->
            // Use placeholder while loading, proper sizing
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .size(Size.ORIGINAL) // Important: don't over-size
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f) // Maintain aspect ratio
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                placeholder = ColorPainter(Color.Gray.copy(alpha = 0.3f))
            )
        }
    }
}
```

#### Large Dataset Handling with Paging

```kotlin
// Using Paging 3 with LazyColumn
@Composable
fun PaginatedList(
    pagingData: Flow<PagingData<Article>>
) {
    val articles = pagingData.collectAsLazyPagingItems()
    
    LazyColumn {
        items(
            count = articles.itemCount,
            key = articles.itemKey { it.id }
        ) { index ->
            val article = articles[index]
            if (article != null) {
                ArticleCard(article)
            } else {
                // Placeholder while loading
                ArticleCardPlaceholder()
            }
        }
        
        // Handle loading states
        when (val loadState = articles.loadState.append) {
            is LoadState.Loading -> {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is LoadState.Error -> {
                item {
                    RetryButton { articles.retry() }
                }
            }
            else -> {}
        }
    }
}
```

## 🏗️ Complex Layout Patterns

### 1. Staggered Grid Layout (Pinterest Style)

```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StaggeredGrid(items: List<GridItem>) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(
            items = items,
            key = { it.id }
        ) { item ->
            StaggeredGridItemCard(
                item = item,
                modifier = Modifier.animateItemPlacement()
            )
        }
    }
}

@Composable
fun StaggeredGridItemCard(
    item: GridItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Dynamic height based on content
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(item.aspectRatio),
                contentScale = ContentScale.Crop
            )
            
            Text(
                text = item.title,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

### 2. Sticky Headers Implementation

```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupedListWithStickyHeaders(groupedData: Map<String, List<Contact>>) {
    LazyColumn {
        groupedData.forEach { (initial, contacts) ->
            stickyHeader {
                StickyHeaderItem(initial)
            }
            
            items(
                items = contacts,
                key = { contact -> contact.id }
            ) { contact ->
                ContactItem(contact)
            }
        }
    }
}

@Composable
fun StickyHeaderItem(initial: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 4.dp
    ) {
        Text(
            text = initial,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
```

### 3. Nested Scrolling with Complex Layouts

```kotlin
@Composable
fun ComplexNestedScrolling() {
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Handle nested scroll behavior
                return Offset.Zero
            }
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        item {
            // Collapsible header with nested horizontal scroll
            CollapsibleHeader()
        }
        
        item {
            // Horizontal category selector
            CategoryRow()
        }
        
        // Main content with proper nested scroll handling
        items(mainContentItems) { item ->
            when (item) {
                is CarouselItem -> {
                    HorizontalCarousel(
                        items = item.carouselItems,
                        modifier = Modifier.height(200.dp)
                    )
                }
                is GridItem -> {
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(2),
                        modifier = Modifier.height(400.dp)
                    ) {
                        items(item.gridItems) { gridItem ->
                            GridItemCard(gridItem)
                        }
                    }
                }
                else -> RegularListItem(item)
            }
        }
    }
}
```

## 🎯 Advanced State Management

### 1. Complex Scroll State Management

```kotlin
class AdvancedScrollState(
    private val listState: LazyListState
) {
    val isScrollingUp: Boolean by derivedStateOf {
        listState.firstVisibleItemScrollOffset < previousScrollOffset
    }
    
    private var previousScrollOffset by mutableStateOf(0)
    
    val currentSection: String by derivedStateOf {
        when (listState.firstVisibleItemIndex) {
            in 0..9 -> "Recent"
            in 10..29 -> "Popular"
            in 30..49 -> "Trending"
            else -> "Archive"
        }
    }
    
    val scrollProgress: Float by derivedStateOf {
        val totalItems = listState.layoutInfo.totalItemsCount
        if (totalItems == 0) 0f
        else listState.firstVisibleItemIndex.toFloat() / totalItems.toFloat()
    }
    
    init {
        // Update previous offset
        snapshotFlow { listState.firstVisibleItemScrollOffset }
            .collect { offset ->
                previousScrollOffset = offset
            }
    }
    
    suspend fun animateToSection(section: String) {
        val targetIndex = when (section) {
            "Recent" -> 0
            "Popular" -> 10
            "Trending" -> 30
            "Archive" -> 50
            else -> 0
        }
        listState.animateScrollToItem(targetIndex)
    }
}

@Composable
fun rememberAdvancedScrollState(
    listState: LazyListState = rememberLazyListState()
): AdvancedScrollState {
    val scope = rememberCoroutineScope()
    return remember(listState) { 
        AdvancedScrollState(listState).apply {
            scope.launch {
                // Initialize state tracking
            }
        }
    }
}
```

### 2. Multi-Selection State Management

```kotlin
@Stable
class MultiSelectionState<T>(
    private val keySelector: (T) -> Any = { it.toString() }
) {
    private val _selectedItems = mutableStateMapOf<Any, T>()
    val selectedItems: Map<Any, T> = _selectedItems
    
    val selectedCount: Int by derivedStateOf { _selectedItems.size }
    val isSelectionMode: Boolean by derivedStateOf { _selectedItems.isNotEmpty() }
    
    fun toggleSelection(item: T) {
        val key = keySelector(item)
        if (_selectedItems.containsKey(key)) {
            _selectedItems.remove(key)
        } else {
            _selectedItems[key] = item
        }
    }
    
    fun isSelected(item: T): Boolean {
        return _selectedItems.containsKey(keySelector(item))
    }
    
    fun clearSelection() {
        _selectedItems.clear()
    }
    
    fun selectAll(items: List<T>) {
        items.forEach { item ->
            _selectedItems[keySelector(item)] = item
        }
    }
}

@Composable
fun <T> rememberMultiSelectionState(
    keySelector: (T) -> Any = { it.toString() }
): MultiSelectionState<T> {
    return remember { MultiSelectionState(keySelector) }
}

// Usage in LazyColumn
@Composable
fun SelectableList(items: List<Contact>) {
    val selectionState = rememberMultiSelectionState<Contact> { it.id }
    
    Column {
        // Selection toolbar
        AnimatedVisibility(
            visible = selectionState.isSelectionMode,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            SelectionToolbar(
                selectedCount = selectionState.selectedCount,
                onClearSelection = { selectionState.clearSelection() },
                onSelectAll = { selectionState.selectAll(items) }
            )
        }
        
        LazyColumn {
            items(
                items = items,
                key = { it.id }
            ) { contact ->
                SelectableContactItem(
                    contact = contact,
                    isSelected = selectionState.isSelected(contact),
                    onToggleSelection = { selectionState.toggleSelection(contact) }
                )
            }
        }
    }
}
```

## 🎨 Advanced Animations and Transitions

### 1. Item Animations with AnimateItemPlacement

```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimatedList(
    items: List<AnimatedItem>,
    onDeleteItem: (AnimatedItem) -> Unit,
    onMoveItem: (Int, Int) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = items,
            key = { it.id }
        ) { item ->
            SwipeToDeleteCard(
                item = item,
                onDelete = { onDeleteItem(item) },
                modifier = Modifier
                    .animateItemPlacement(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing
                        )
                    )
                    .fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteCard(
    item: AnimatedItem,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberDismissState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == DismissValue.DismissedToStart) {
                onDelete()
                true
            } else false
        }
    )
    
    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.EndToStart),
        background = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    DismissValue.Default -> Color.Transparent
                    else -> Color.Red
                }
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        dismissContent = {
            ItemCard(
                item = item,
                modifier = modifier
            )
        }
    )
}
```

### 2. Complex Reveal Animations

```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RevealAnimationList(items: List<Item>) {
    LazyColumn {
        itemsIndexed(
            items = items,
            key = { _, item -> item.id }
        ) { index, item ->
            val animationDelay = (index * 50).coerceAtMost(500)
            
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = animationDelay,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = animationDelay
                    )
                ),
                modifier = Modifier.animateItemPlacement()
            ) {
                ItemCard(item)
            }
        }
    }
}
```

## 🔧 Performance Profiling and Optimization

### 1. Composition Tracking

```kotlin
@Composable
fun ProfiledLazyColumn(items: List<Item>) {
    // Track composition count in debug builds
    if (BuildConfig.DEBUG) {
        val compositionCount = remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            compositionCount.value++
            Log.d("Composition", "LazyColumn composed ${compositionCount.value} times")
        }
    }
    
    LazyColumn {
        items(
            items = items,
            key = { it.id }
        ) { item ->
            ProfiledItemCard(item)
        }
    }
}

@Composable
fun ProfiledItemCard(item: Item) {
    // Composition tracking for individual items
    if (BuildConfig.DEBUG) {
        val recompositionCount = remember { mutableStateOf(0) }
        LaunchedEffect(item) {
            recompositionCount.value++
            Log.d("Recomposition", "Item ${item.id} recomposed ${recompositionCount.value} times")
        }
    }
    
    // Stable item implementation
    ItemCard(item)
}
```

### 2. Memory Usage Optimization

```kotlin
@Composable
fun MemoryOptimizedList(
    items: List<LargeDataItem>,
    onItemVisible: (String) -> Unit = {},
    onItemHidden: (String) -> Unit = {}
) {
    val listState = rememberLazyListState()
    
    // Track visible items for memory management
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .distinctUntilChanged()
            .collect { visibleItems ->
                visibleItems.forEach { itemInfo ->
                    val item = items.getOrNull(itemInfo.index)
                    item?.let { onItemVisible(it.id) }
                }
            }
    }
    
    LazyColumn(state = listState) {
        items(
            items = items,
            key = { it.id },
            contentType = { it.type }
        ) { item ->
            // Use placeholder for off-screen heavy content
            LazyLoadedItemCard(item)
        }
    }
}

@Composable
fun LazyLoadedItemCard(item: LargeDataItem) {
    var isLoaded by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // Simulate async loading
        delay(100)
        isLoaded = true
    }
    
    if (isLoaded) {
        HeavyItemCard(item)
    } else {
        ItemPlaceholder()
    }
}
```

## 🧠 Advanced Architecture Patterns

### 1. Repository Pattern with LazyColumn

```kotlin
interface ItemRepository {
    fun getItems(): Flow<List<Item>>
    suspend fun refreshItems()
    fun searchItems(query: String): Flow<List<Item>>
}

class ItemRepositoryImpl : ItemRepository {
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    
    override fun getItems(): Flow<List<Item>> = _items
    
    override suspend fun refreshItems() {
        // Fetch from API
        val newItems = apiService.getItems()
        _items.value = newItems
    }
    
    override fun searchItems(query: String): Flow<List<Item>> {
        return _items.map { items ->
            items.filter { it.name.contains(query, ignoreCase = true) }
        }
    }
}

@Composable
fun RepositoryBasedList(
    repository: ItemRepository,
    searchQuery: String = ""
) {
    val items by if (searchQuery.isEmpty()) {
        repository.getItems().collectAsState(initial = emptyList())
    } else {
        repository.searchItems(searchQuery).collectAsState(initial = emptyList())
    }
    
    LazyColumn {
        items(
            items = items,
            key = { it.id }
        ) { item ->
            ItemCard(item)
        }
    }
}
```

### 2. ViewModel Integration with Complex State

```kotlin
class ListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ListUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items = _items.asStateFlow()
    
    fun onAction(action: ListAction) {
        when (action) {
            is ListAction.LoadItems -> loadItems()
            is ListAction.SearchItems -> searchItems(action.query)
            is ListAction.RefreshItems -> refreshItems()
            is ListAction.SelectItem -> selectItem(action.item)
        }
    }
    
    private fun loadItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val items = repository.getItems()
                _items.value = items
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}

data class ListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedItems: Set<String> = emptySet()
)

sealed class ListAction {
    object LoadItems : ListAction()
    object RefreshItems : ListAction()
    data class SearchItems(val query: String) : ListAction()
    data class SelectItem(val item: Item) : ListAction()
}

@Composable
fun ViewModelBasedList(
    viewModel: ListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val items by viewModel.items.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.onAction(ListAction.LoadItems)
    }
    
    when {
        uiState.isLoading -> {
            LoadingIndicator()
        }
        uiState.error != null -> {
            ErrorMessage(
                message = uiState.error,
                onRetry = { viewModel.onAction(ListAction.RefreshItems) }
            )
        }
        else -> {
            LazyColumn {
                items(
                    items = items,
                    key = { it.id }
                ) { item ->
                    ItemCard(
                        item = item,
                        isSelected = item.id in uiState.selectedItems,
                        onClick = { viewModel.onAction(ListAction.SelectItem(item)) }
                    )
                }
            }
        }
    }
}
```

## 🎯 Testing Advanced LazyColumn Components

### 1. Unit Testing List Logic

```kotlin
@Test
fun `selection state should toggle items correctly`() {
    val selectionState = MultiSelectionState<TestItem> { it.id }
    val testItem = TestItem("1", "Test")
    
    // Initial state
    assertThat(selectionState.isSelected(testItem)).isFalse()
    assertThat(selectionState.selectedCount).isEqualTo(0)
    
    // Select item
    selectionState.toggleSelection(testItem)
    assertThat(selectionState.isSelected(testItem)).isTrue()
    assertThat(selectionState.selectedCount).isEqualTo(1)
    
    // Deselect item
    selectionState.toggleSelection(testItem)
    assertThat(selectionState.isSelected(testItem)).isFalse()
    assertThat(selectionState.selectedCount).isEqualTo(0)
}
```

### 2. UI Testing with Compose Test

```kotlin
@Test
fun lazyColumn_displays_all_items() {
    val testItems = (1..10).map { TestItem(it.toString(), "Item $it") }
    
    composeTestRule.setContent {
        LazyColumn {
            items(testItems) { item ->
                Text(
                    text = item.name,
                    modifier = Modifier.testTag("item_${item.id}")
                )
            }
        }
    }
    
    // Verify first and last items are displayed
    composeTestRule.onNodeWithTag("item_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("item_10").assertIsDisplayed()
}

@Test
fun lazyColumn_scroll_behavior() {
    val testItems = (1..100).map { TestItem(it.toString(), "Item $it") }
    
    composeTestRule.setContent {
        LazyColumn(
            modifier = Modifier.testTag("lazy_column")
        ) {
            items(testItems) { item ->
                Text(
                    text = item.name,
                    modifier = Modifier
                        .testTag("item_${item.id}")
                        .height(50.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
    
    // Initially, item 50 should not be visible
    composeTestRule.onNodeWithTag("item_50").assertDoesNotExist()
    
    // Scroll to item 50
    composeTestRule.onNodeWithTag("lazy_column")
        .performScrollToNode(hasTestTag("item_50"))
    
    // Now item 50 should be visible
    composeTestRule.onNodeWithTag("item_50").assertIsDisplayed()
}
```

## 🚀 Performance Best Practices Summary

### Critical Optimizations:

1. **Use Keys Wisely**: Always provide unique, stable keys for dynamic content
2. **Minimize Recomposition**: Use `derivedStateOf` and isolate state reads
3. **ContentType**: Use for better view recycling with different item types
4. **Image Optimization**: Proper sizing and caching strategies
5. **Memory Management**: Monitor and optimize for large datasets

### Advanced Techniques:

1. **Custom Scroll Connections**: For complex nested scroll behavior
2. **State Management**: Sophisticated selection and scroll state handling
3. **Animation Integration**: Smooth transitions and reveals
4. **Testing**: Comprehensive testing strategies for complex lists
5. **Architecture**: Clean separation of concerns with repositories and ViewModels

This completes the advanced guide for LazyColumn and LazyRow. The techniques covered here will help you build highly performant, scalable list UIs in Jetpack Compose.