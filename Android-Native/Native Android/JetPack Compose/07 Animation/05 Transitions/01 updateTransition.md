# updateTransition — Multi-Property Transitions

> [!NOTE]
> Use `updateTransition` when you need **multiple properties to animate together from a single state change**. It coordinates all child animations under one umbrella and gives you access to the underlying `Transition` for debugging.

---

## What is `updateTransition`?

A way to tie multiple animations to a **single state variable**. When the state changes, all child animations start simultaneously, each with their own spec.

Think of it as: "I have ONE state (e.g., isExpanded), and I want FIVE things to animate when it changes."

```kotlin
val transition = updateTransition(targetState = isExpanded, label = "card")

val height by transition.animateDp(label = "height") { expanded ->
    if (expanded) 200.dp else 80.dp
}
val alpha by transition.animateFloat(label = "alpha") { expanded ->
    if (expanded) 1f else 0.5f
}
val backgroundColor by transition.animateColor(label = "bg") { expanded ->
    if (expanded) MaterialTheme.colorScheme.primary else Color.Gray
}
```

```text
  ┌─────────────────────────────────────────────────────────────────┐
  │              COORDINATING MULTIPLE CHILD PROPERTIES             │
  │                                                                 │
  │                     ┌──────► height (animateDp)                 │
  │                     ├──────► alpha (animateFloat)               │
  │  State: isExpanded ─┼──────► backgroundColor (animateColor)     │
  │                     └──────► iconRotation (animateFloat)        │
  │                                                                 │
  │  * All child properties animate on a synchronized timeline      │
  │    under a single transition umbrella.                          │
  └─────────────────────────────────────────────────────────────────┘
```

---

## Full API Signature

```kotlin
@Composable
fun <S> updateTransition(
    targetState: S,
    label: String? = null
): Transition<S>
```

Returns a `Transition<S>` object that you use to define child animations.

---

## Child Animation Methods

All child animations are extension functions on `Transition<S>`:

```kotlin
// Animate Float
@Composable
fun <S> Transition<S>.animateFloat(
    transitionSpec: @Composable Transition.Segment<S>.() -> AnimationSpec<Float> = { spring() },
    label: String = "FloatAnimation",
    targetValueByState: @Composable (state: S) -> Float
): State<Float>

// Animate Dp, Color, Int, IntOffset, IntSize, Offset, Rect, Size similarly
```

### All available child animators:
- `animateFloat`
- `animateDp`
- `animateColor`
- `animateInt`
- `animateIntOffset`
- `animateIntSize`
- `animateOffset`
- `animateRect`
- `animateSize`
- `animateValue` (custom types)

---

## Transition.Segment — Direction-Aware Specs

Inside `transitionSpec`, you get a `Transition.Segment<S>` which has:
- `initialState` — where it was
- `targetState` — where it's going

Use `isTransitioningTo` to make direction-aware animation specs:

```kotlin
val height by transition.animateDp(
    transitionSpec = {
        if (initialState == CardState.Collapsed && targetState == CardState.Expanded) {
            // Expanding: slow spring
            spring(stiffness = Spring.StiffnessLow)
        } else {
            // Collapsing: fast tween
            tween(200)
        }
    },
    label = "height"
) { state ->
    when (state) {
        CardState.Collapsed -> 80.dp
        CardState.Expanded -> 200.dp
    }
}
```

---

## Transition Properties

The `Transition<S>` object provides:

| Property | Type | Description |
|----------|------|-------------|
| `currentState` | `S` | The state transition is currently at |
| `targetState` | `S` | The state it's animating toward |
| `isRunning` | `Boolean` | True if any child animation is running |
| `segment` | `Transition.Segment<S>` | Current `initialState → targetState` segment |

---

## Complete Example: Expandable Card

```kotlin
enum class CardState { Collapsed, Expanded }

@Composable
fun ExpandableCard() {
    var cardState by remember { mutableStateOf(CardState.Collapsed) }

    val transition = updateTransition(targetState = cardState, label = "expandCard")

    val cardHeight by transition.animateDp(
        transitionSpec = { spring(stiffness = Spring.StiffnessLow) },
        label = "cardHeight"
    ) { state ->
        when (state) {
            CardState.Collapsed -> 80.dp
            CardState.Expanded -> 250.dp
        }
    }

    val contentAlpha by transition.animateFloat(
        transitionSpec = {
            if (isTransitioningTo(CardState.Expanded)) {
                tween(durationMillis = 300, delayMillis = 100)  // Delay appearance
            } else {
                tween(durationMillis = 150)  // Quick disappear
            }
        },
        label = "contentAlpha"
    ) { state ->
        when (state) {
            CardState.Collapsed -> 0f
            CardState.Expanded -> 1f
        }
    }

    val borderColor by transition.animateColor(
        transitionSpec = { tween(300) },
        label = "borderColor"
    ) { state ->
        when (state) {
            CardState.Collapsed -> Color.Gray
            CardState.Expanded -> MaterialTheme.colorScheme.primary
        }
    }

    val iconRotation by transition.animateFloat(
        transitionSpec = { spring() },
        label = "iconRotation"
    ) { state ->
        when (state) {
            CardState.Collapsed -> 0f
            CardState.Expanded -> 180f
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable {
                cardState = if (cardState == CardState.Collapsed)
                    CardState.Expanded else CardState.Collapsed
            }
    ) {
        Column {
            Row(modifier = Modifier.padding(16.dp)) {
                Text("Card Title", modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer { rotationZ = iconRotation }
                )
            }

            // Extra content only visible when expanded
            Column(modifier = Modifier.alpha(contentAlpha)) {
                Text("Hidden content...")
                Text("More details...")
            }
        }
    }
}
```

---

## Transition + AnimatedVisibility

`AnimatedVisibility` can also be used as a child of `updateTransition`:

```kotlin
@Composable
fun <S> Transition<S>.AnimatedVisibility(
    visible: @Composable (targetState: S) -> Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    content: @Composable AnimatedVisibilityScope.() -> Unit
)

// Usage:
transition.AnimatedVisibility(
    visible = { state -> state == CardState.Expanded }
) {
    ExtraContent()
}
```

---

## Transition + AnimatedContent

```kotlin
transition.AnimatedContent(
    transitionSpec = { fadeIn() togetherWith fadeOut() }
) { state ->
    when (state) {
        CardState.Collapsed -> CollapsedContent()
        CardState.Expanded -> ExpandedContent()
    }
}
```

---

## `updateTransition` vs `animate*AsState`

| | `animate*AsState` | `updateTransition` |
|--|------------------|-------------------|
| # of values | 1 | Many |
| State trigger | State change | State change |
| Direction-aware spec | ❌ | ✅ |
| isRunning check | ❌ | ✅ |
| Debug in inspector | ❌ | ✅ |

---

## 💡 Interview Q&A

**Q: When should I use `updateTransition` instead of multiple `animate*AsState`?**
A: When you have 3+ properties all driven by the same state, `updateTransition` groups them logically and is easier to debug. It also lets you use direction-aware specs (different animation for expanding vs collapsing).

**Q: What is `Transition.Segment`?**
A: It represents the current `initialState → targetState` pair, giving you access to both states inside `transitionSpec`. This lets you apply different animations based on the direction of change.

**Q: Can you check if all animations in a transition have finished?**
A: Yes — `transition.isRunning` is `false` when all child animations have settled.

---

**Next:** [[07 Animation/09 Modifier Animations/01 graphicsLayer|graphicsLayer →]]
