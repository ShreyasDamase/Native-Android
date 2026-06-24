# 🔘 Animated Switch / Toggle

> [!NOTE]
> Build a custom animated switch from scratch — understanding every property. No library magic. By the end you'll know exactly how Android's `Switch` works internally.

---

## What We're Building

A toggle that:
1. Slides a thumb from left (OFF) to right (ON)
2. Background color animates between gray and your primary color
3. Thumb scale changes slightly when pressed (tactile feedback)
4. Spring physics — natural, bouncy feel

---

## Step 1 — Understand the Structure

```
[  OFF State  ]     [  ON State   ]
┌──────────────┐    ┌──────────────┐
│ ●            │    │            ● │
└──────────────┘    └──────────────┘
  Gray bg              Primary bg
  Thumb left           Thumb right
```

We need to animate:
- `thumbOffsetX` — how far right the thumb is
- `trackColor` — gray → primary
- `thumbScale` — slight press feedback

---

## Step 2 — The Mathematics

```text
  ┌─────────────────────────────────────────────────────────────────┐
  │                   SWITCH LAYOUT DIMENSIONS                      │
  │                                                                 │
  │     ◄──────────────────── trackWidth (52.dp) ───────────────────► │
  │     ┌──────────────────────────────────────────────────────────┐│
  │     │  ◄─ thumbPadding (3.dp)                                  ││
  │     │  ┌───────────────┐                          ▲            ││
  │  trackHeight           │                      thumbSize        ││
  │   (28.dp)   │  Thumb   │                       (22.dp)         ││
  │     │  │               │                          ▼            ││
  │     │  └───────────────┘                                       ││
  │     └──────────────────────────────────────────────────────────┘│
  │                                                                 │
  │   Thumb Offset X Travel Range:                                  │
  │     OFF State (Left):  thumbPadding = 3.dp                      │
  │     ON State (Right):  trackWidth - thumbSize - thumbPadding    │
  │                        52.dp - 22.dp - 3.dp = 27.dp             │
  └─────────────────────────────────────────────────────────────────┘
```

---

## Step 3 — The Code (From Scratch)

```kotlin
@Composable
fun AnimatedSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Dimensions
    val trackWidth = 52.dp
    val trackHeight = 28.dp
    val thumbSize = 22.dp
    val thumbPadding = 3.dp

    // Animated values
    val thumbOffsetX by animateDpAsState(
        targetValue = if (checked) {
            trackWidth - thumbSize - thumbPadding
        } else {
            thumbPadding
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "thumbOffset"
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(durationMillis = 200),
        label = "trackColor"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(durationMillis = 200),
        label = "thumbColor"
    )

    // Press feedback
    var isPressed by remember { mutableStateOf(false) }
    val thumbScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "thumbScale"
    )

    // Track (the container)
    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .background(trackColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null  // No ripple — we have custom animation
            ) {
                onCheckedChange(!checked)
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
    ) {
        // Thumb (the circle)
        Box(
            modifier = Modifier
                .offset(x = thumbOffsetX)
                .padding(vertical = thumbPadding)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .size(thumbSize)
                .background(thumbColor, CircleShape)
        )
    }
}
```

---

## Step 4 — Usage

```kotlin
var switchState by remember { mutableStateOf(false) }

Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
) {
    Text("Dark Mode")
    AnimatedSwitch(
        checked = switchState,
        onCheckedChange = { switchState = it }
    )
}
```

---

## Variations

### With Icon in Thumb
```kotlin
// Inside the thumb Box, add an icon
Box(
    modifier = Modifier
        .offset(x = thumbOffsetX)
        .padding(vertical = thumbPadding)
        .size(thumbSize)
        .background(thumbColor, CircleShape),
    contentAlignment = Alignment.Center
) {
    val iconAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        label = "iconAlpha"
    )
    Icon(
        imageVector = Icons.Default.Check,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .size(14.dp)
            .graphicsLayer { alpha = iconAlpha }
    )
}
```

### Thumb Width Stretch (Advanced)
The thumb can stretch wider during transition (like the pill in tab bars):

```kotlin
val thumbWidth by animateDpAsState(
    targetValue = if (isPressed || isTransitioning) 28.dp else thumbSize,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "thumbWidth"
)

// Instead of size(thumbSize), use:
Modifier.width(thumbWidth).height(thumbSize)
```

---

## Understanding What We Used

| Property | API Used | Why |
|----------|----------|-----|
| Thumb position | `animateDpAsState` | Dp value, simple state |
| Track color | `animateColorAsState` | Color value, state-driven |
| Thumb color | `animateColorAsState` | Color value, state-driven |
| Press scale | `animateFloatAsState` | Float, very fast spring |
| Spring physics | `spring(DampingRatioMediumBouncy)` | Natural bounce on toggle |

---

## Key Concepts Learned

1. **State drives everything** — `checked` is the single source of truth
2. **Multiple `animate*AsState` for multiple properties** — they all animate simultaneously
3. **Spring for positional** — thumb sliding feels physical
4. **Tween for color** — colors don't need to bounce
5. **Pointer input** — detecting press without consuming the click
6. **graphicsLayer for scale** — doesn't affect layout (thumb doesn't resize its space)

---

## 💡 Interview Q&A

**Q: Why use `spring()` for the thumb position but `tween()` for color?**
A: The thumb is a physical element that benefits from bouncy spring physics — it makes the toggle feel tactile. Colors don't need to bounce; a smooth `tween` looks cleaner.

**Q: Why use `clickable(indication = null)` and then custom pointer input?**
A: We want the tap to trigger `onCheckedChange` AND update `isPressed` for the scale animation. Using `clickable` for the main action and separate `pointerInput` for press state detection gives us both cleanly.

---

**Next:** [[07 Animation/12 Recipes/06 Tab Pill Stretch|Tab Pill Stretch Animation →]]
