# AnimatedVisibility — Show & Hide with Motion

> [!NOTE]
> Use `AnimatedVisibility` when a composable needs to **appear or disappear** on screen. It automatically handles the enter and exit transitions so you don't have to manage alpha or offset manually.

---

## What is `AnimatedVisibility`?

A composable wrapper that **shows or hides its child** with an animated transition.

```kotlin
var isVisible by remember { mutableStateOf(false) }

AnimatedVisibility(visible = isVisible) {
    Text("I animate in and out!")
}
```

```text
  ┌─────────────────────────────────────────────────────────────────┐
  │                    ENTER/EXIT STATE FLOW                        │
  │                                                                 │
  │  [PreEnter]   ──►  [Visible]   ──►  [PostExit]                  │
  │  (Off-screen/      (On screen,      (Off-screen/                │
  │   Alpha 0f)         fully interactive) Alpha 0f)                │
  │                                                                 │
  │  ◄───────────────► ◄─────────────────► ◄──────────────────────► │
  │    Enter transition    Visible state    Exit transition         │
  │    (Layout active)    (Layout active)  (Layout active, then     │
  │                                         removed from tree)      │
  └─────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────┐
  │               ALPHA vs ANIMATEDVISIBILITY                       │
  │                                                                 │
  │  With Modifier.alpha(0f):                                       │
  │  ┌───────────────────────────────────────────────────────────┐  │
  │  │ Composable is INVISIBLE but STILL measured & in tree     │  │
  │  └───────────────────────────────────────────────────────────┘  │
  │                                                                 │
  │  With AnimatedVisibility(visible = false):                      │
  │  ┌───────────────────────────────────────────────────────────┐  │
  │  │ Composable is COMPLETELY REMOVED from composition tree    │  │
  │  └───────────────────────────────────────────────────────────┘  │
  └─────────────────────────────────────────────────────────────────┘
```

When `visible` flips `false → true`: **enter transition** plays.
When `visible` flips `true → false`: **exit transition** plays. The composable is **not removed from the tree until the exit animation finishes**.

---

## Full API Signature

```kotlin
@Composable
fun AnimatedVisibility(
    visible: Boolean,                           // Show or hide
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandVertically(),  // How it appears
    exit: ExitTransition = fadeOut() + shrinkVertically(),   // How it disappears
    label: String = "AnimatedVisibility",       // Debug label
    content: @Composable AnimatedVisibilityScope.() -> Unit  // Your UI
)
```

---

## Parameters Explained

### `visible: Boolean`
The boolean that controls visibility.
- `true` → plays enter transition
- `false` → plays exit transition, then removes composable from layout

### `enter: EnterTransition`
How the content **enters** the screen. Default: `fadeIn() + expandVertically()`

### `exit: ExitTransition`
How the content **exits** the screen. Default: `fadeOut() + shrinkVertically()`

### Combining transitions with `+`
You can combine multiple transitions:
```kotlin
enter = slideInVertically() + fadeIn() + scaleIn()
exit = slideOutVertically() + fadeOut() + scaleOut()
```

---

## Enter Transitions — Full Reference

| Function | What it does | Key Parameters |
|----------|-------------|----------------|
| `fadeIn()` | Fades from transparent to opaque | `initialAlpha`, `animationSpec` |
| `expandVertically()` | Expands from 0 height | `expandFrom` (Top/Bottom), `clip`, `animationSpec` |
| `expandHorizontally()` | Expands from 0 width | `expandFrom` (Start/End), `clip`, `animationSpec` |
| `expandIn()` | Expands from corner/center | `expandFrom` (Alignment), `initialSize`, `clip`, `animationSpec` |
| `slideInVertically()` | Slides in from top or bottom | `initialOffsetY` (lambda), `animationSpec` |
| `slideInHorizontally()` | Slides in from left or right | `initialOffsetX` (lambda), `animationSpec` |
| `slideIn()` | Slides in from any direction | `initialOffset` (lambda), `animationSpec` |
| `scaleIn()` | Scales from 0 to 1 | `initialScale`, `transformOrigin`, `animationSpec` |

### Enter Transition Examples

```kotlin
// Slide up from bottom
enter = slideInVertically { fullHeight -> fullHeight } + fadeIn()

// Slide down from top
enter = slideInVertically { fullHeight -> -fullHeight } + fadeIn()

// Slide in from right
enter = slideInHorizontally { fullWidth -> fullWidth }

// Scale up from center
enter = scaleIn(initialScale = 0.5f) + fadeIn()

// Expand from top with clip
enter = expandVertically(expandFrom = Alignment.Top)

// No animation (instant)
enter = EnterTransition.None
```

> [!TIP]
> The lambda in `slideInVertically { fullHeight -> fullHeight }` receives the full height of the composable and returns the offset pixels. Returning `fullHeight` means "start off-screen below". Returning `-fullHeight` means "start off-screen above".

---

## Exit Transitions — Full Reference

| Function | What it does | Key Parameters |
|----------|-------------|----------------|
| `fadeOut()` | Fades from opaque to transparent | `targetAlpha`, `animationSpec` |
| `shrinkVertically()` | Shrinks to 0 height | `shrinkTowards` (Top/Bottom), `clip`, `animationSpec` |
| `shrinkHorizontally()` | Shrinks to 0 width | `shrinkTowards` (Start/End), `clip`, `animationSpec` |
| `shrinkOut()` | Shrinks to a corner/center | `shrinkTowards` (Alignment), `targetSize`, `clip`, `animationSpec` |
| `slideOutVertically()` | Slides out to top or bottom | `targetOffsetY` (lambda), `animationSpec` |
| `slideOutHorizontally()` | Slides out to left or right | `targetOffsetX` (lambda), `animationSpec` |
| `slideOut()` | Slides out in any direction | `targetOffset` (lambda), `animationSpec` |
| `scaleOut()` | Scales from 1 to 0 | `targetScale`, `transformOrigin`, `animationSpec` |

```kotlin
// Slide out to left
exit = slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut()

// Shrink and fade
exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()

// Scale down from center
exit = scaleOut(targetScale = 0f) + fadeOut()

// No animation
exit = ExitTransition.None
```

---

## AnimatedVisibilityScope

The `content` block gives you access to `AnimatedVisibilityScope`, which provides:

### `transition`
Access the underlying `Transition` for custom child animations:

```kotlin
AnimatedVisibility(visible = isVisible) {
    // 'this' is AnimatedVisibilityScope
    val color by transition.animateColor(label = "color") { state ->
        when (state) {
            EnterExitState.PreEnter -> Color.Red
            EnterExitState.Visible -> Color.Green
            EnterExitState.PostExit -> Color.Blue
        }
    }
    Box(modifier = Modifier.background(color))
}
```

### `Modifier.animateEnterExit()`
Apply **custom enter/exit** to specific children inside the animated visibility:

```kotlin
AnimatedVisibility(visible = isVisible, enter = fadeIn(), exit = fadeOut()) {
    Column {
        Text("Fades with parent")

        Text(
            "Slides separately",
            modifier = Modifier.animateEnterExit(
                enter = slideInHorizontally { -it },
                exit = slideOutHorizontally { -it }
            )
        )
    }
}
```

---

## Using in LazyList

```kotlin
items(list) { item ->
    AnimatedVisibility(
        visible = item.isVisible,
        enter = slideInHorizontally() + fadeIn(),
        exit = slideOutHorizontally() + fadeOut()
    ) {
        ListItem(item = item)
    }
}
```

> [!WARNING]
> For animated item REORDERING in LazyList, use `Modifier.animateItem()` instead of `AnimatedVisibility`. `AnimatedVisibility` is only for show/hide, not reordering.

---

## Staggered List Entrance Pattern

```kotlin
@Composable
fun StaggeredList(items: List<String>) {
    val visibleItems = remember { mutableStateListOf<Boolean>() }

    LaunchedEffect(items) {
        items.indices.forEach { i ->
            delay(i * 80L)  // 80ms stagger
            visibleItems.add(true)
        }
    }

    Column {
        items.forEachIndexed { index, item ->
            AnimatedVisibility(
                visible = visibleItems.getOrNull(index) == true,
                enter = slideInVertically { it } + fadeIn(
                    animationSpec = tween(durationMillis = 300)
                )
            ) {
                Text(item)
            }
        }
    }
}
```

---

## Comparison with MutableState Alpha

```kotlin
// Without AnimatedVisibility (manual approach — avoid)
var alpha by animateFloatAsState(if (isVisible) 1f else 0f)
Box(modifier = Modifier.alpha(alpha)) {
    // ⚠️ Still in composition even when invisible!
    HeavyContent()
}

// With AnimatedVisibility (correct approach)
AnimatedVisibility(visible = isVisible) {
    // ✅ Removed from composition after exit animation finishes
    HeavyContent()
}
```

---

## Common Patterns

### Dialog / Bottom Sheet Reveal
```kotlin
AnimatedVisibility(
    visible = showDialog,
    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
) {
    BottomSheetContent()
}
```

### Toast / Snackbar
```kotlin
AnimatedVisibility(
    visible = showToast,
    enter = slideInVertically { -it } + fadeIn(),
    exit = slideOutVertically { -it } + fadeOut()
) {
    ToastBanner()
}
```

### FAB Menu
```kotlin
fabMenuItems.forEachIndexed { index, item ->
    AnimatedVisibility(
        visible = isFabExpanded,
        enter = slideInVertically { it * (index + 1) } + fadeIn(
            animationSpec = tween(delayMillis = index * 50)
        ),
        exit = slideOutVertically { it * (index + 1) } + fadeOut()
    ) {
        FabMenuItem(item)
    }
}
```

---

## 💡 Interview Q&A

**Q: What happens to the composable while it's animating out?**
A: It stays in the composition tree until the exit animation finishes. After that, it's removed and recomposition stops for it.

**Q: Can I use different enter and exit animations?**
A: Yes — `enter` and `exit` are completely independent.

**Q: What's the difference between `EnterTransition.None` and setting `visible = true` without animation?**
A: `EnterTransition.None` skips the animation but still uses the `AnimatedVisibility` wrapper (so exit still animates). Removing `AnimatedVisibility` entirely means no animation at all.

---

**Next:** [[07 Animation/03 High Level APIs/03 AnimatedContent|AnimatedContent →]]
