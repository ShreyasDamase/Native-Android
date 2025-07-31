![[Pasted image 20250727231351.png]]

 # Android Jetpack Compose - Accessing Assets and Resources Guide

## 1. How to access string name from string which is inside res folder

```xml
<resources>  
    <string name="app_name">jetpackComposeCourse</string>  
</resources>
```

```kotlin
Text(text = stringResource(R.string.app_name))
// R stands for resource folder which is inside package so it import always be import packagename.R
```

## 2. How to access Color Resources from XML

```xml
<?xml version="1.0" encoding="utf-8"?>  
<resources>  
    <color name="purple_200">#FFBB86FC</color>  
    <color name="purple_500">#FF6200EE</color>  
    <color name="purple_700">#FF3700B3</color>  
    <color name="teal_200">#FF03DAC5</color>  
    <color name="teal_700">#FF018786</color>  
    <color name="black">#FF000000</color>  
    <color name="white">#FFFFFFFF</color>  
</resources>
```

```kotlin
Text(
    text = stringResource(R.string.app_name), 
    color = colorResource(R.color.purple_200)
)
```

## 3. How to access Image using painterResource API

```kotlin
Image(
    painter = painterResource(R.drawable.ic_launcher_foreground),
    contentDescription = null
)
// Go to res folder -> drawable -> and find ic_launcher_foreground image
// Don't forget to add contentDescription or assign it null for accessibility
```

## 4. How to access Dimensions (dimens.xml)

```xml
<!-- res/values/dimens.xml -->
<resources>
    <dimen name="small_padding">8dp</dimen>
    <dimen name="medium_padding">16dp</dimen>
    <dimen name="large_padding">24dp</dimen>
    <dimen name="text_size_large">18sp</dimen>
</resources>
```

```kotlin
import androidx.compose.ui.res.dimensionResource

Box(
    modifier = Modifier.padding(dimensionResource(R.dimen.medium_padding))
) {
    Text(
        text = "Hello World",
        fontSize = dimensionResource(R.dimen.text_size_large).value.sp
    )
}
```

## 5. How to access Arrays from resources

```xml
<!-- res/values/arrays.xml -->
<resources>
    <string-array name="planets_array">
        <item>Mercury</item>
        <item>Venus</item>
        <item>Earth</item>
        <item>Mars</item>
    </string-array>
</resources>
```

```kotlin
val planets = stringArrayResource(R.array.planets_array)
LazyColumn {
    items(planets) { planet ->
        Text(text = planet)
    }
}
```

## 6. How to access Raw Resources (files in res/raw/)

```kotlin
import androidx.compose.ui.platform.LocalContext

@Composable
fun ReadRawFile() {
    val context = LocalContext.current
    val rawText = remember {
        context.resources.openRawResource(R.raw.sample_file)
            .bufferedReader()
            .use { it.readText() }
    }
    Text(text = rawText)
}
```

## 7. How to access Assets folder files

```kotlin
import androidx.compose.ui.platform.LocalContext

@Composable
fun ReadAssetFile() {
    val context = LocalContext.current
    val assetText = remember {
        context.assets.open("sample.txt")
            .bufferedReader()
            .use { it.readText() }
    }
    Text(text = assetText)
}
```

## 8. Vector Drawables and Icons

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite

// Using Material Icons
Icon(
    imageVector = Icons.Default.Favorite,
    contentDescription = "Favorite"
)

// Using custom vector drawable
Icon(
    painter = painterResource(R.drawable.ic_custom_vector),
    contentDescription = "Custom Icon"
)
```

## 9. Accessing Boolean and Integer Resources

```xml
<!-- res/values/booleans.xml -->
<resources>
    <bool name="is_tablet">false</bool>
</resources>

<!-- res/values/integers.xml -->
<resources>
    <integer name="max_items">10</integer>
</resources>
```

```kotlin
val isTablet = booleanResource(R.bool.is_tablet)
val maxItems = integerResource(R.integer.max_items)

if (isTablet) {
    // Tablet layout
} else {
    // Phone layout
}
```

## 10. Theming in Jetpack Compose

### Creating a Custom Theme

```kotlin
// ui/theme/Color.kt
val Purple200 = Color(0xFFBB86FC)
val Purple500 = Color(0xFF6200EE)
val Purple700 = Color(0xFF3700B3)
val Teal200 = Color(0xFF03DAC5)

// Dark Theme Colors
val DarkColors = darkColors(
    primary = Purple200,
    primaryVariant = Purple700,
    secondary = Teal200
)

// Light Theme Colors
val LightColors = lightColors(
    primary = Purple500,
    primaryVariant = Purple700,
    secondary = Teal200
)
```

```kotlin
// ui/theme/Theme.kt
@Composable
fun MyAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColors
    } else {
        LightColors
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
```

### Using Theme Colors

```kotlin
@Composable
fun ThemedComponent() {
    Card(
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface
    ) {
        Text(
            text = "Themed Text",
            color = MaterialTheme.colors.primary,
            style = MaterialTheme.typography.h6
        )
    }
}
```

### Custom Typography

```kotlin
// ui/theme/Type.kt
val Typography = Typography(
    body1 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    h1 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp
    )
)
```

### Custom Shapes

```kotlin
// ui/theme/Shape.kt
val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp)
)
```

### Accessing Theme Values

```kotlin
@Composable
fun AccessThemeValues() {
    // Access colors
    val primaryColor = MaterialTheme.colors.primary
    val backgroundColor = MaterialTheme.colors.background
    
    // Access typography
    val headlineStyle = MaterialTheme.typography.h1
    val bodyStyle = MaterialTheme.typography.body1
    
    // Access shapes
    val smallShape = MaterialTheme.shapes.small
    val mediumShape = MaterialTheme.shapes.medium
    
    // Use in composables
    Text(
        text = "Styled Text",
        color = primaryColor,
        style = headlineStyle
    )
}
```

## Important Notes:

1. **Always import the correct R class**: `import your.package.name.R`
2. **Use `LocalContext.current`** when you need context in Composables
3. **Remember to add contentDescription** for accessibility
4. **Use `remember`** for expensive operations like file reading
5. **Theme your app consistently** using MaterialTheme
6. **Test both light and dark themes** for better user experience