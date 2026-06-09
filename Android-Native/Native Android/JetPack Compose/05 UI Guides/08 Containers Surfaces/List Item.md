# ListItem

## 📌 Purpose
`ListItem` is a highly structured layout component designed specifically for rows inside `LazyColumn` or scrollable lists. It enforces Material 3 list item standards by providing slots for leading icons (avatars), trailing icons (checkboxes/switches), a main headline, and supporting text.

## 🔧 Function Signature

```kotlin
@Composable
fun ListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    colors: ListItemColors = ListItemDefaults.colors(),
    tonalElevation: Dp = ListItemDefaults.Elevation,
    shadowElevation: Dp = ListItemDefaults.Elevation
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `headlineContent` | `@Composable () -> Unit` | — | Required. The primary, largest text of the row. |
| `overlineContent` | `@Composable (() -> Unit)?` | `null` | Optional text placed *above* the headline. |
| `supportingContent` | `@Composable (() -> Unit)?` | `null` | Optional text placed *below* the headline. |
| `leadingContent` | `@Composable (() -> Unit)?` | `null` | Slot for an icon, image, avatar, or checkbox on the far left. |
| `trailingContent` | `@Composable (() -> Unit)?` | `null` | Slot for an icon, switch, or metadata (like time) on the far right. |

## ✅ Basic Example: 1-Line Item
Just an icon and text.

```kotlin
@Composable
fun SimpleListItem() {
    ListItem(
        headlineContent = { Text("Wi-Fi") },
        leadingContent = { Icon(Icons.Default.Wifi, contentDescription = null) }
    )
}
```

## 🚀 Advanced Examples

### 1. 3-Line List Item (The works)
Avatar + Overline + Headline + Supporting Text + Trailing Switch

```kotlin
@Composable
fun ComplexListItem() {
    var checked by remember { mutableStateOf(false) }

    ListItem(
        overlineContent = { Text("System Settings") },
        headlineContent = { Text("Background Sync") },
        supportingContent = { Text("Allow app to download data while in the background to improve performance.") },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = { checked = it }
            )
        }
    )
}
```

### 2. In a LazyColumn with Dividers
The canonical way to build a settings menu.

```kotlin
@Composable
fun SettingsList() {
    LazyColumn {
        item {
            ListItem(
                headlineContent = { Text("Account") },
                supportingContent = { Text("john.doe@example.com") },
                leadingContent = { Icon(Icons.Default.Person, null) }
            )
            HorizontalDivider()
        }
        item {
            ListItem(
                headlineContent = { Text("Notifications") },
                leadingContent = { Icon(Icons.Default.Notifications, null) }
            )
            HorizontalDivider()
        }
    }
}
```

### 3. Selectable/Clickable List Item
`ListItem` itself has no `onClick` parameter. You must apply `Modifier.clickable` or wrap it.

```kotlin
@Composable
fun ClickableListItem(onItemClick: () -> Unit) {
    ListItem(
        headlineContent = { Text("Tap Me") },
        modifier = Modifier.clickable { onItemClick() }
    )
}
```

## ⚠️ Common Gotchas

> [!WARNING] Not Clickable By Default
> Unlike `Card` or `Surface`, `ListItem` does not have an `onClick` constructor overload. You must use `Modifier.clickable()`.

> [!TIP] Height is Automatic
> Do not attempt to force a specific `.height()` on a `ListItem`. It automatically sizes itself to 1-line (56dp), 2-line (72dp), or 3-line (88dp+) based purely on which text slots (`headlineContent`, `supportingContent`, `overlineContent`) you populate.

## 💡 Interview Q&A

**Q: In a list of settings where each row has a Switch, should I make the entire `ListItem` clickable or just the `Switch`?**
A: Material guidelines suggest the entire row should be clickable to toggle the switch, as it creates a much larger and more accessible touch target.

**Q: How does `ListItem` determine if it should be formatted as a 1-line or 2-line item?**
A: It evaluates the presence of the `supportingContent` and `overlineContent` composables. If either is provided, it automatically shifts from a 1-line layout to a multi-line layout and adjusts vertical padding accordingly.
