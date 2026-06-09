# 🔤 SearchBar

## 📌 Purpose
`SearchBar` is a Material 3 component used for entering search queries. It comes in two variants:
1. **SearchBar**: Typically used at the top of a screen. When expanded, it can grow to fill the screen (or container), showing search history or suggestions.
2. **DockedSearchBar**: Stays anchored to its location even when expanded, useful for side panels or larger screens where full-screen takeover isn't desired.

## 🔧 Function Signature

### 1. SearchBar (Material 3)
```kotlin
@ExperimentalMaterial3Api
@Composable
fun SearchBar(
    inputField: @Composable () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = SearchBarDefaults.inputFieldShape,
    colors: SearchBarColors = SearchBarDefaults.colors(),
    tonalElevation: Dp = SearchBarDefaults.TonalElevation,
    shadowElevation: Dp = SearchBarDefaults.ShadowElevation,
    windowInsets: WindowInsets = SearchBarDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit
)
```

### 2. DockedSearchBar (Material 3)
```kotlin
@ExperimentalMaterial3Api
@Composable
fun DockedSearchBar(
    inputField: @Composable () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = SearchBarDefaults.dockedShape,
    colors: SearchBarColors = SearchBarDefaults.colors(),
    tonalElevation: Dp = SearchBarDefaults.TonalElevation,
    shadowElevation: Dp = SearchBarDefaults.ShadowElevation,
    content: @Composable ColumnScope.() -> Unit
)
```

### 3. SearchBarDefaults.InputField
```kotlin
@ExperimentalMaterial3Api
@Composable
fun InputField(
    state: TextFieldState,
    onSearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    colors: TextFieldColors = SearchBarDefaults.inputFieldColors(),
    interactionSource: MutableInteractionSource? = null,
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| inputField | `@Composable` | — | Required. The text field at the top of the search bar, usually `SearchBarDefaults.InputField`. |
| expanded | `Boolean` | — | Required. Whether the search bar's content area (suggestions) is shown. |
| onExpandedChange | `(Boolean) -> Unit` | — | Required. Callback when the user focuses/unfocuses or dismisses the search bar. |
| modifier | `Modifier` | `Modifier` | Optional. |
| shape | `Shape` | `SearchBarDefaults.*` | Optional. Shape of the search bar container. |
| colors | `SearchBarColors` | `SearchBarDefaults.colors()` | Optional. |
| tonalElevation | `Dp` | `6.dp` | Optional. |
| shadowElevation | `Dp` | `0.dp` | Optional. |
| windowInsets | `WindowInsets` | `SearchBarDefaults.windowInsets` | Optional. `SearchBar` only. Adjusts for status bar. |
| content | `@Composable ColumnScope.() -> Unit` | — | Required. The content shown when `expanded` is true (e.g., search history list). |

## ✅ Basic Example

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSearchBar() {
    val state = rememberTextFieldState()
    var expanded by remember { mutableStateOf(false) }

    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                state = state,
                onSearch = { expanded = false },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                placeholder = { Text("Search...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )
        },
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        // Content shown when expanded
        Text("No recent searches", modifier = Modifier.padding(16.dp))
    }
}
```

## 🚀 Advanced Examples

### 1. SearchBar with Suggestions and History
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySearchBar() {
    val state = rememberTextFieldState()
    var expanded by remember { mutableStateOf(false) }
    val searchHistory = listOf("Jetpack Compose", "Material 3", "Kotlin Coroutines")

    SearchBar(
        modifier = Modifier.fillMaxWidth(),
        inputField = {
            SearchBarDefaults.InputField(
                state = state,
                onSearch = { 
                    expanded = false 
                    // Perform search
                },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                placeholder = { Text("Search docs") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (expanded) {
                        IconButton(onClick = { 
                            if (state.text.isNotEmpty()) {
                                state.edit { replace(0, length, "") }
                            } else {
                                expanded = false
                            }
                        }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                }
            )
        },
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(searchHistory) { historyItem ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            state.edit { replace(0, length, historyItem) }
                            expanded = false
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, null, modifier = Modifier.padding(end = 16.dp))
                    Text(historyItem)
                }
            }
        }
    }
}
```

### 2. DockedSearchBar
Used in tablet layouts or when you don't want the search to take over the screen.
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDockedSearchBar() {
    val state = rememberTextFieldState()
    var expanded by remember { mutableStateOf(false) }

    DockedSearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                state = state,
                onSearch = { expanded = false },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                placeholder = { Text("Search locally...") }
            )
        },
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        Text("Suggestions go here", modifier = Modifier.padding(16.dp))
    }
}
```

## ⚠️ Common Gotchas
- **State synchronization:** The `expanded` state needs to be carefully managed. Ensure your `trailingIcon` (like a close button) correctly resets the text and closes the expansion state.
- **Back handler handling:** By default, `SearchBar` doesn't handle the system back button. You usually want to intercept the back button to collapse the search bar instead of popping the fragment/activity.
- **Scaffold Padding:** `SearchBar` (full width) handles `WindowInsets` itself, meaning it will draw behind the status bar. Do not apply top `Scaffold` padding directly to the `SearchBar` if you want it to behave like a top app bar.

## 💡 Interview Q&A

**Q: What is the main difference between `SearchBar` and `DockedSearchBar`?**
A: `SearchBar` typically expands to cover the entire width and often the height (depending on layout), making it feel like a distinct screen or mode. `DockedSearchBar` drops down a menu anchored below the input field and does not stretch or take over the screen.
