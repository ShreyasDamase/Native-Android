# Badge

## 📌 Purpose
Badges (`Badge`) are small status descriptors for UI elements. They appear at the top right of an icon, typically to show notifications or status. 
They come in two types:
1. **Dot Badge:** A simple dot indicating unread status.
2. **Small/Large Badge:** A badge containing short text or numbers (like "3" or "99+").

You use the `Badge` composable *inside* a `BadgedBox` to attach it to an element.

## 🔧 Function Signatures

### Badge
```kotlin
@Composable
fun Badge(
    modifier: Modifier = Modifier,
    containerColor: Color = BadgeDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable (RowScope.() -> Unit)? = null
)
```

### BadgedBox
```kotlin
@Composable
fun BadgedBox(
    badge: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `badge` | `@Composable BoxScope.() -> Unit` | — | **BadgedBox only**. The `Badge` component goes here. |
| `containerColor` | `Color` | `MaterialTheme.colorScheme.error` | Background color of the badge (defaults to red/error). |
| `contentColor` | `Color` | `contentColorFor(containerColor)` | Text/Icon color inside the badge. |
| `content` | `@Composable (RowScope.() -> Unit)?` | `null` | **Badge only**. Leave `null` for a dot badge. Provide a `Text` for a count/text badge. |

## ✅ Basic Example

### 1. Notification Dot Badge
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun DotBadgeExample() {
    BadgedBox(
        badge = { Badge() } // No content = small dot
    ) {
        Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
    }
}
```

## 🚀 Advanced Examples

### 2. Message Count Badge
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun CountBadgeExample() {
    val unreadCount = 5

    BadgedBox(
        badge = {
            Badge {
                Text(unreadCount.toString())
            }
        }
    ) {
        Icon(Icons.Filled.MailOutline, contentDescription = "Mail")
    }
}
```

### 3. BadgedBox with NavigationBarItem
Badges are extremely common on bottom navigation bars.

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun BadgedNavigationBar() {
    NavigationBar {
        NavigationBarItem(
            icon = {
                BadgedBox(
                    badge = {
                        Badge { Text("99+") } // Max count pattern
                    }
                ) {
                    Icon(Icons.Filled.Home, contentDescription = "Home")
                }
            },
            label = { Text("Home") },
            selected = true,
            onClick = { /* ... */ }
        )
    }
}
```

### 4. Custom Colored Badge
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun CustomColorBadge() {
    BadgedBox(
        badge = {
            Badge(
                containerColor = Color.Green,
                contentColor = Color.Black
            ) {
                Text("NEW")
            }
        }
    ) {
        Icon(Icons.Filled.Person, contentDescription = "Profile")
    }
}
```

## ⚠️ Common Gotchas
- **Accessibility:** A `BadgedBox` does not inherently update the semantics of its child. If you have "5" unread messages, make sure the `contentDescription` of the `Icon` (or a `semantics` modifier) reflects this so screen readers announce "Mail, 5 unread messages" instead of just "Mail".
- **Clipping:** `BadgedBox` positions the badge slightly outside the bounds of the anchor content. If the `BadgedBox` is placed inside a tightly constrained container with `clipToBounds` or `clip`, the badge might get cut off.
- **Sizing:** The size of the badge is fixed by Material specifications (small for dots, larger for text). Don't try to manually resize it using `Modifier.size()`, as it will break the shape rounding.

## 💡 Interview Q&A

**Q: How do you create a "Dot" badge vs a "Number" badge?**
A: It depends entirely on the `content` parameter of the `Badge` composable. If you leave `content = null`, it renders as a small dot. If you provide a `Text` composable, it renders as a larger pill-shape containing the text.
