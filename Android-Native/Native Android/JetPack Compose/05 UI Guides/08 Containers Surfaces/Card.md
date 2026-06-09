# Card

## 📌 Purpose
Cards are surfaces that display content and actions on a single topic. They provide clear boundaries, shape, and elevation.
Material 3 provides three main card variants:
1. `Card` (Filled / Default): Uses a filled surface color.
2. `ElevatedCard`: Uses shadow elevation to stand out from the background.
3. `OutlinedCard`: Has a stroke border, no shadow.

Each variant has a non-clickable and a clickable (`onClick`) overload.

## 🔧 Function Signatures

### Non-Clickable Card (Base)
```kotlin
@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
)
```

### Clickable Card (Overload)
```kotlin
@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit
)
```

*(Note: `ElevatedCard` and `OutlinedCard` have identical signatures but different default `colors`, `elevation`, and `border`).*

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `colors` | `CardColors` | `CardDefaults.cardColors()` | Defines container, content, and disabled colors. |
| `elevation` | `CardElevation` | `CardDefaults.cardElevation()` | Handles shadow/tonal elevation dynamically based on state (pressed, hovered, focused). |
| `shape` | `Shape` | `CardDefaults.shape` | Default is 12dp rounded corners. |
| `border` | `BorderStroke?` | `null` | Stroke drawn around the edge. Required for OutlinedCard. |
| `content` | `@Composable ColumnScope.() -> Unit` | — | Required. Lays out children vertically by default. |

## ✅ Basic Example: Info Card
```kotlin
@Composable
fun BasicCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Text(
            text = "Jetpack Compose",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Modern native UI toolkit.",
            modifier = Modifier.padding(start = 16.dp, bottom = 16.dp, end = 16.dp)
        )
    }
}
```

## 🚀 Advanced Examples

### 1. Clickable Elevated Card
Using the clickable overload automatically gives you standard Material ripple effects.

```kotlin
@Composable
fun ClickableProductCard(onProductClick: () -> Unit) {
    ElevatedCard(
        onClick = onProductClick,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Row(Modifier.padding(16.dp)) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null)
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Premium Subscription", fontWeight = FontWeight.Bold)
                Text("$9.99 / month")
            }
        }
    }
}
```

### 2. Outlined Card with Image
Cards clip their content to their shape automatically.

```kotlin
@Composable
fun ImageOutlinedCard() {
    OutlinedCard(
        modifier = Modifier.size(width = 240.dp, height = 200.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        // Image will be automatically rounded by the Card's shape
        Image(
            painter = painterResource(id = R.drawable.sample_header),
            contentDescription = "Header",
            contentScale = ContentScale.Crop,
            modifier = Modifier.height(120.dp).fillMaxWidth()
        )
        Text(
            text = "Title Text",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium
        )
    }
}
```

## ⚠️ Common Gotchas

> [!WARNING] ColumnScope Content
> The `content` block of a Card is an implicit `ColumnScope`. You do NOT need to declare a `<Column>` inside a Card if you just want items stacked vertically.

> [!CAUTION] Modifier.clickable vs onClick parameter
> **Do not** use `Modifier.clickable` on a Card if you intend for the whole card to be a button. Use the explicitly provided `onClick: () -> Unit` constructor overload. The constructor manages the specific Material 3 elevation shifts and ripple clip paths much better than the raw modifier.

## 💡 Interview Q&A

**Q: What is the primary difference between `Card` and `ElevatedCard` in Material 3?**
A: In M3, the default `Card` uses a filled tonal color difference (surface variant color) to distinguish itself from the background, and has *zero* drop shadow by default. `ElevatedCard` uses the base surface color but adds a drop shadow (`elevation`) to create depth.
