# Nav3 Overview

## 📌 Purpose
Navigation 3 (Nav3) is a completely new navigation library built from the ground up specifically for Jetpack Compose. Unlike Nav2, which wrapped the old `NavController` imperative system, Nav3 is purely declarative and heavily relies on Compose state, specifically `SnapshotStateList`. It provides built-in support for multi-pane layouts, multiplatform capabilities (CMP), and explicit state management.

> [!NOTE]
> Nav3 is **not** backward compatible with Nav2. It introduces a paradigm shift where the developer owns the back stack, making testing and state manipulation much more predictable.

### Nav3 vs Nav2 Comparison Table
| Feature | Nav2 | Nav3 |
|---|---|---|
| Back stack | `NavController` (internal) | `SnapshotStateList` (you own it) |
| State model | Imperative | Declarative |
| Package | `androidx.navigation` | `androidx.navigation3` |
| Multi-pane | Hard workaround | Built-in `SceneStrategy` |
| Multiplatform | Android only | CMP (Android+iOS+Desktop+Web) |
| ViewModels | Automatic | `rememberViewModelStoreNavEntryDecorator()` |
| Version | 2.x | 1.1.2 |

## 🔧 Function Signature

The primary composable for Nav3 rendering is `NavDisplay`.

```kotlin
@Composable
public fun <T : Any> NavDisplay(
    backStack: List<T>,
    entryProvider: NavEntryProvider<T>,
    modifier: Modifier = Modifier,
    sceneStrategy: SceneStrategy<T> = SinglePaneSceneStrategy(),
    entryDecorators: List<NavEntryDecorator> = emptyList(),
    transitionSpec: AnimatedContentTransitionScope<T>.() -> ContentTransform = {
        fadeIn(animationSpec = tween(700)) togetherWith fadeOut(animationSpec = tween(700))
    },
    popTransitionSpec: AnimatedContentTransitionScope<T>.() -> ContentTransform = transitionSpec,
    predictivePopTransitionSpec: AnimatedContentTransitionScope<T>.() -> ContentTransform = popTransitionSpec,
    onBack: () -> Unit
)
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `backStack` | `List<T>` | — | Required. The developer-owned list of `NavKey` representing the back stack. Must be backed by a `SnapshotStateList`. |
| `entryProvider` | `NavEntryProvider<T>` | — | Required. DSL mapping `NavKey` types to Composable screen content. |
| `modifier` | `Modifier` | `Modifier` | Optional. Modifier applied to the root container of `NavDisplay`. |
| `sceneStrategy` | `SceneStrategy<T>` | `SinglePaneSceneStrategy()` | Optional. Strategy for controlling layout modes (e.g., single-pane vs multi-pane list-detail). |
| `entryDecorators` | `List<NavEntryDecorator>` | `emptyList()` | Optional. Adds capabilities like `rememberSaveableStateHolderNavEntryDecorator()` and `rememberViewModelStoreNavEntryDecorator()`. |
| `transitionSpec` | `AnimatedContentTransitionScope<T>.() -> ContentTransform` | default fade | Optional. Global transition for pushing new screens. |
| `popTransitionSpec` | `AnimatedContentTransitionScope<T>.() -> ContentTransform` | `transitionSpec` | Optional. Global transition for popping screens. |
| `predictivePopTransitionSpec` | `AnimatedContentTransitionScope<T>.() -> ContentTransform` | `popTransitionSpec` | Optional. Transition shown during predictive back gesture. |
| `onBack` | `() -> Unit` | — | Required. Callback triggered when the system back button is pressed. Usually just removes the last item from `backStack`. |

## ✅ Basic Example

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.navigation3.*
import androidx.navigation3.ui.*
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute : NavKey
@Serializable data class DetailRoute(val id: Int) : NavKey
@Serializable data class ProfileRoute(val userId: String) : NavKey

@Composable
fun MinimalNav3App() {
    // 1. You own the back stack
    val backStack = remember { mutableStateListOf<NavKey>(HomeRoute) }
    
    // 2. Define the mapping between keys and UI
    val entryProvider = entryProvider {
        entry<HomeRoute> { 
            HomeScreen(onNavigate = { id -> backStack.add(DetailRoute(id)) }) 
        }
        entry<DetailRoute> { route -> 
            DetailScreen(id = route.id, onBack = { backStack.removeLastOrNull() }) 
        }
        entry<ProfileRoute> { route -> 
            ProfileScreen(userId = route.userId, onBack = { backStack.removeLastOrNull() }) 
        }
    }
    
    // 3. Render the back stack
    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        onBack = { backStack.removeLastOrNull() }
    )
}

@Composable
fun HomeScreen(onNavigate: (Int) -> Unit) {
    Button(onClick = { onNavigate(42) }) { Text("Go to Detail 42") }
}

@Composable
fun DetailScreen(id: Int, onBack: () -> Unit) {
    Column {
        Text("Detail for ID: $id")
        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
fun ProfileScreen(userId: String, onBack: () -> Unit) { /* ... */ }
```

## 🚀 Advanced Examples

### Accessing AnimatedContentScope for Shared Transitions
In Nav3, `NavDisplay` provides an `AnimatedContentScope` implicitly to each entry. However, because you are inside the `entry` DSL, you need to use `LocalNavAnimatedContentScope.current` instead of `this@composable`.

```kotlin
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.*
import androidx.navigation3.*
import androidx.navigation3.ui.*

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppWithSharedTransitions() {
    SharedTransitionLayout {
        val backStack = remember { mutableStateListOf<NavKey>(HomeRoute) }
        
        NavDisplay(
            backStack = backStack,
            entryProvider = entryProvider {
                entry<HomeRoute> {
                    HomeScreen(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                        onNavigate = { backStack.add(DetailRoute(it)) }
                    )
                }
                // ...
            },
            onBack = { backStack.removeLastOrNull() }
        )
    }
}
```

## ⚠️ Common Gotchas

- **Forgetting Entry Decorators:** If you forget to pass `rememberSaveableStateHolderNavEntryDecorator()`, screen state (like scroll position) will be lost when navigating back and forth.
- **Modifying Back Stack incorrectly:** `backStack` is a standard `SnapshotStateList`. Modifying it updates the UI immediately. Ensure modifications happen on the main thread and don't do concurrent modification.
- **Using old Navigation Compose dependencies:** Make sure your `build.gradle.kts` uses `navigation3-runtime` and `navigation3-ui`, NOT `navigation-compose`.
- **Finding AnimatedVisibilityScope:** You cannot use `this` to get the `AnimatedVisibilityScope` in the `entry {}` block. You **must** use `LocalNavAnimatedContentScope.current`.

## 💡 Interview Q&A

**Q: How does the back stack model differ between Nav2 and Nav3?**
A: In Nav2, the back stack is internal to `NavController` and is mutated imperatively via `.navigate()` and `.popBackStack()`. In Nav3, the back stack is a declarative `SnapshotStateList` owned by the developer. Navigating is literally just `list.add()` and `list.removeLast()`.

**Q: Why do we need `NavEntryDecorator`?**
A: Nav3 strips out automatic bindings to Android-specific lifecycles to be multiplatform-friendly. `NavEntryDecorator` acts as a middleware plugin system to add back features like `ViewModel` scoping (`rememberViewModelStoreNavEntryDecorator()`) and state saving (`rememberSaveableStateHolderNavEntryDecorator()`).

**Q: How do you handle deep linking in Nav3?**
A: Because the back stack is developer-owned, deep linking is just initializing the `SnapshotStateList` with a pre-populated list of keys (e.g., `mutableStateListOf(HomeRoute, ProfileRoute(userId))`). You handle the incoming intent and parse it into your initial state list before passing it to `NavDisplay`.
