Here's the full `Text.md` note **formatted perfectly for Obsidian**, following best practices for readability, linking, and compatibility with Obsidian features like **Graph View**, **Dataview**, and **Hover Preview**.

---

## ✅ Copy-paste this into `Text.md` inside your Obsidian vault:




> Jetpack Compose’s `Text` composable is used to **display read-only text** in the UI.  
> It is one of the most fundamental building blocks of any Android layout.

---

## 🔧 Basic Syntax

```kotlin
Text(text = "Hello World!")
````

---

## 🧠 Key Parameters

|Parameter|Type|Description|
|---|---|---|
|`text`|`String`|The actual string to display|
|`modifier`|`Modifier`|Layout control (padding, width, etc.)|
|`style`|`TextStyle`|Typography style (font size, color, etc.)|
|`color`|`Color`|Sets text color|
|`fontSize`|`TextUnit`|Font size (e.g., `20.sp`)|
|`fontWeight`|`FontWeight`|Weight (e.g., `FontWeight.Bold`)|
|`textAlign`|`TextAlign`|Aligns text (start, center, end, justify)|
|`maxLines`|`Int`|Limits the number of visible lines|
|`overflow`|`TextOverflow`|What to do with overflow (e.g., `Ellipsis`, `Clip`)|

---

## 🎨 Styling Example

```kotlin
Text(
    text = "Styled Text",
    color = Color.Red,
    fontSize = 20.sp,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis
)
```

---

## 📎 Snippet Example

```kotlin
@Composable
fun Greeting(name: String) {
    Text(
        text = "Hello $name!",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(8.dp)
    )
}
```

---

## 🚫 Common Pitfall

> ⚠️ `Text` does **not support mixed formatting (bold/italic)** inline by default.  
> Use `AnnotatedString` and `SpanStyle`:

```kotlin
Text(
    buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("Bold ")
        }
        append("Normal")
    }
)
```

---

## 🔁 Related Components

 
    

---

## 🧭 Use Cases

- Static text and labels
    
- Dynamic UI content
    
- Form instructions
    
- Titles and subtitles
    
- Error or success messages
    

---

## 📚 Documentation

- [🔗 Official Android Docs](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#Text\(kotlin.String,androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Color,androidx.compose.ui.text.TextStyle,...\))


---

## 🔗 Backlinks

- Part of: [[Core components]]
    
- See also: [[Typography]], [[Native Android/JetPack Compose/Core componnets/Core components/Scaffold]], [[Native Android/JetPack Compose/Core componnets/Core components/Button]], [[Native Android/JetPack Compose/Core componnets/Core components/Image]]
    



---

## ✅ Features Included:
| Feature              | Why it's useful in Obsidian           |
| -------------------- | ------------------------------------- |
| `[[internal links]]` | Creates graph view structure          |
| `#tags`              | Used for graph coloring               |
| Tables & Snippets    | Clean, readable developer references  |
| 🔗 Web links         | Connect to Android documentation      |
| Markdown headers     | Searchable and collapsible formatting |


