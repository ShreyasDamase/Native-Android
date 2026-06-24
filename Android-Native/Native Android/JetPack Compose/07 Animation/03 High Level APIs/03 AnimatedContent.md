# AnimatedContent — Swap Content with Animation

> [!NOTE]
> Use `AnimatedContent` when you want to **replace one composable with another** with an animated transition. Think: changing tabs, updating a counter, swapping screens.

---

## What is `AnimatedContent`?

It animates between different states of content. When the `targetState` changes, the old content exits and new content enters — both animating simultaneously.

```kotlin
var count by remember { mutableStateOf(0) }

AnimatedContent(targetState = count) { value ->
    Text("Count: $value", fontSize = 32.sp)
}
```

```text
  ┌─────────────────────────────────────────────────────────────────┐
  │                   SIMULTANEOUS CONTENT SWAP                     │
  │                                                                 │
  │  State Change: count = 0 ──► count = 1                          │
  │                                                                 │
  │  Outgoing Content (Count: 0)  ──► [ Exit Transition ] ──► Out    │
  │                                                                 │
  │  Incoming Content (Count: 1)  ──► [ Enter Transition ] ──► In   │
  │                                                                 │
  │  * Both occur simultaneously inside the container bounds *     │
  └─────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────┐
  │                 CONTAINER SIZETRANSFORM MATCHING                │
  │                                                                 │
  │  Initial Container Size          Target Container Size          │
  │  ┌──────────────┐                ┌───────────────────────────┐  │
  │  │  Count: 0    │   ──────────►  │        Count: 1000        │  │
  │  └──────────────┘                └───────────────────────────┘  │
  │                                                                 │
  │  Animate Width/Height together (Default spring)                 │
  │  Or custom (e.g. animate width first, then height using keyframes)│
  └─────────────────────────────────────────────────────────────────┘
```

Every time `count` changes, the old number flies out and the new one flies in.

---

## Full API Signature

```kotlin
@Composable
fun <S> AnimatedContent(
    targetState: S,                    // The state controlling which content shows
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentTransitionScope<S>.() -> ContentTransform
        = { fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith fadeOut(animationSpec = tween(90)) },
    contentAlignment: Alignment = Alignment.TopStart,  // How contents align during transition
    label: String = "AnimatedContent",
    contentKey: (targetState: S) -> Any? = { it },  // Key to distinguish states
    content: @Composable AnimatedContentScope.(targetState: S) -> Unit
)
```

---

## Parameters Explained

### `targetState: S`
The state that determines which content to show. Can be any type:
- `Boolean`, `Int`, `String`, `enum`, `sealed class`

### `transitionSpec`
A lambda that defines the `ContentTransform` — how content enters AND exits.

### `ContentTransform`
Created with the `togetherWith` infix function:
```kotlin
ContentTransform(enterTransition, exitTransition, targetContentZIndex, sizeTransform)

// Shorthand using infix
fadeIn() togetherWith fadeOut()
slideInVertically { it } togetherWith slideOutVertically { -it }
```

### `contentAlignment`
Where the incoming and outgoing content are aligned **during** the transition.

### `contentKey`
Used to determine if content should change. By default uses the `targetState` itself. Override if you want two different states to show the same content without animation.

---

## ContentTransform & transitionSpec

### Default behavior
```kotlin
// Default: fade in (slight delay) + fade out
fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith 
fadeOut(animationSpec = tween(90))
```

### Common patterns

```kotlin
// Slide up: new comes from bottom, old goes to top
transitionSpec = {
    slideInVertically { height -> height } + fadeIn() togetherWith
    slideOutVertically { height -> -height } + fadeOut()
}

// Counter up: new slides up when incrementing
transitionSpec = {
    if (targetState > initialState) {
        // Going up: slide new from bottom, old out to top
        slideInVertically { it } togetherWith slideOutVertically { -it }
    } else {
        // Going down: slide new from top, old out to bottom
        slideInVertically { -it } togetherWith slideOutVertically { it }
    }.using(SizeTransform(clip = false))
}
```

### Direction-aware counter example
```kotlin
var count by remember { mutableStateOf(0) }

AnimatedContent(
    targetState = count,
    transitionSpec = {
        if (targetState > initialState) {
            slideInVertically { it } + fadeIn() togetherWith
            slideOutVertically { -it } + fadeOut()
        } else {
            slideInVertically { -it } + fadeIn() togetherWith
            slideOutVertically { it } + fadeOut()
        }
    },
    label = "counter"
) { value ->
    Text("$value", style = MaterialTheme.typography.displayLarge)
}

Row {
    Button(onClick = { count-- }) { Text("-") }
    Button(onClick = { count++ }) { Text("+") }
}
```

---

## SizeTransform

Controls how the **container size changes** during the transition.

```kotlin
// Use SizeTransform to animate the container size
transitionSpec = {
    fadeIn() togetherWith fadeOut() using SizeTransform { initialSize, targetSize ->
        // Custom AnimationSpec for size change
        if (targetState > initialState) {
            // Growing: animate height first, then width
            keyframes {
                IntSize(targetSize.width, initialSize.height) at 150
                durationMillis = 300
            }
        } else {
            keyframes {
                IntSize(initialSize.width, targetSize.height) at 150
                durationMillis = 300
            }
        }
    }
}
```

`SizeTransform(clip: Boolean = true, sizeAnimationSpec: ...)`:
- `clip = true` — clips content to the current animated size (content doesn't overflow)
- `clip = false` — content can overflow the animated size (useful for expanding content)

---

## Using with Sealed Classes / Enums

```kotlin
sealed class Screen {
    object Home : Screen()
    object Profile : Screen()
    object Settings : Screen()
}

var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

AnimatedContent(
    targetState = currentScreen,
    transitionSpec = {
        fadeIn(tween(300)) togetherWith fadeOut(tween(300))
    },
    label = "screen"
) { screen ->
    when (screen) {
        is Screen.Home -> HomeContent()
        is Screen.Profile -> ProfileContent()
        is Screen.Settings -> SettingsContent()
    }
}
```

---

## AnimatedContentScope

Inside the `content` block, you have access to `AnimatedContentScope`, which exposes the underlying `transition`. You can use `Modifier.animateEnterExit()` for per-child animations.

```kotlin
AnimatedContent(targetState = isExpanded, label = "card") { expanded ->
    if (expanded) {
        Column {
            Text(
                "Title",
                modifier = Modifier.animateEnterExit(enter = slideInVertically { -it })
            )
            Text(
                "Details",
                modifier = Modifier.animateEnterExit(enter = slideInVertically { it })
            )
        }
    } else {
        Text("Summary")
    }
}
```

---

## Crossfade vs AnimatedContent

| | `Crossfade` | `AnimatedContent` |
|--|-------------|------------------|
| Transition type | Fade only | Any enter/exit transition |
| Direction-aware | ❌ | ✅ |
| Size animation | ❌ | ✅ with SizeTransform |
| Complexity | Simple | Full control |

```kotlin
// Crossfade - just a simple fade between states
Crossfade(targetState = selectedTab, label = "tab") { tab ->
    when (tab) {
        Tab.Home -> HomeContent()
        Tab.Profile -> ProfileContent()
    }
}
```

---

## Real-World Example: Tab Switcher

```kotlin
@Composable
fun TabContent(selectedTab: Tab) {
    AnimatedContent(
        targetState = selectedTab,
        transitionSpec = {
            // Direction-aware slide
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            slideInHorizontally { direction * it } + fadeIn(tween(300)) togetherWith
            slideOutHorizontally { -direction * it } + fadeOut(tween(200))
        },
        label = "tabContent"
    ) { tab ->
        when (tab) {
            Tab.Home -> HomeScreen()
            Tab.Calendar -> CalendarScreen()
            Tab.Profile -> ProfileScreen()
        }
    }
}
```

---

## 💡 Interview Q&A

**Q: What's the difference between `AnimatedVisibility` and `AnimatedContent`?**
A: `AnimatedVisibility` shows/hides a single composable. `AnimatedContent` swaps between different content states — both old and new content can animate simultaneously.

**Q: What is `ContentTransform`?**
A: The combination of `EnterTransition + ExitTransition (+ SizeTransform)` that defines how content transitions. Created with `enter togetherWith exit`.

**Q: What does `using SizeTransform(clip = false)` do?**
A: It allows the content to extend beyond its animated container bounds during transition — useful for expanding/collapsing content where you don't want clipping.

---

**Next:** [[07 Animation/03 High Level APIs/05 animateContentSize|animateContentSize →]]
