# 🔢 Digital Clock — Flip & Slide Effect

> [!NOTE]
> Build a digital clock where each digit slides up/down when it changes — like old airport departure boards or premium clock apps. This exercises `AnimatedContent`, `LaunchedEffect`, and coroutine timing.

---

## What We're Building

A live digital clock where:
1. Each digit **slides out upward** when it changes
2. New digit **slides in from below**
3. Direction-aware: when count goes UP (0→9), slide UP; when it wraps (9→0), slide DOWN
4. Colons blink or stay solid between hour:minute:second

---

## Concept: Slot Machine Digit

```text
  ┌─────────────────────────────────────────────────────────────────┐
  │                 SLOT MACHINE DIGIT SLIDE                        │
  │                                                                 │
  │     [ Frame 1 ]         [ Frame 2 ]         [ Frame 3 ]         │
  │    ┌───────────┐       ┌───────────┐       ┌───────────┐        │
  │    │     1     │       │   ┌───┐   │       │           │        │
  │    │           │       │   │ 2 │   │       │     2     │        │
  │    │           │       │   └───┘   │       │           │        │
  │    │           │       │   ┌───┐   │       │           │        │
  │    │           │       │   │ 1 │   │       │           │        │
  │    └───────────┘       └───────────┘       └───────────┘        │
  │     Old digit           Both visible        New digit           │
  │     settled             during slide        settled             │
  │                                                                 │
  │  * Old digit moves from 0 to -height, new from height to 0.    │
  └─────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────┐
  │                 3D MECHANICAL FLIP CLOCK CARD                   │
  │                                                                 │
  │              Top Half Flaps Down:                               │
  │              ┌──────────────────────────┐                       │
  │              │            2             │                       │
  │              ├ - - - - - - - - - - - - -┤  ◄── rotationX pivot  │
  │              │        1 ──► 2           │      (0° → -90°)      │
  │              └──────────────────────────┘                       │
  │                                                                 │
  │              Bottom Half Settles:                               │
  │              ┌──────────────────────────┐                       │
  │              │        1 ──► 2           │                       │
  │              ├ - - - - - - - - - - - - -┤  ◄── rotationX pivot  │
  │              │            2             │      (90° → 0°)       │
  │              └──────────────────────────┘                       │
  └─────────────────────────────────────────────────────────────────┘
```

---

## Step 1: Single Animated Digit

```kotlin
@Composable
fun AnimatedDigit(
    digit: Int,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.displayLarge
) {
    AnimatedContent(
        targetState = digit,
        transitionSpec = {
            // Determine scroll direction based on digit progression
            if (targetState > initialState) {
                // Going up (1 → 2 → 3 ...)
                slideInVertically { fullHeight -> fullHeight } + fadeIn() togetherWith
                slideOutVertically { fullHeight -> -fullHeight } + fadeOut()
            } else {
                // Going down / wrapping (9 → 0)
                slideInVertically { fullHeight -> -fullHeight } + fadeIn() togetherWith
                slideOutVertically { fullHeight -> fullHeight } + fadeOut()
            }
        },
        label = "digit_$digit"
    ) { currentDigit ->
        Text(
            text = currentDigit.toString(),
            style = textStyle,
            modifier = modifier
        )
    }
}
```

---

## Step 2: Two-Digit Group (e.g., "04", "59")

```kotlin
@Composable
fun DoubleDigit(
    value: Int,  // 0-59 or 0-23
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.displayLarge
) {
    val tens = value / 10
    val ones = value % 10

    Row(modifier = modifier) {
        AnimatedDigit(digit = tens, textStyle = textStyle)
        AnimatedDigit(digit = ones, textStyle = textStyle)
    }
}
```

---

## Step 3: Clock State (Updates Every Second)

```kotlin
data class ClockTime(
    val hours: Int,
    val minutes: Int,
    val seconds: Int
)

@Composable
fun rememberClockTime(): State<ClockTime> {
    val time = remember {
        mutableStateOf(
            Calendar.getInstance().let { cal ->
                ClockTime(
                    hours = cal.get(Calendar.HOUR_OF_DAY),
                    minutes = cal.get(Calendar.MINUTE),
                    seconds = cal.get(Calendar.SECOND)
                )
            }
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            val cal = Calendar.getInstance()
            time.value = ClockTime(
                hours = cal.get(Calendar.HOUR_OF_DAY),
                minutes = cal.get(Calendar.MINUTE),
                seconds = cal.get(Calendar.SECOND)
            )
        }
    }

    return time
}
```

---

## Step 4: Blinking Colon

```kotlin
@Composable
fun BlinkingColon(
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.displayLarge
) {
    val infiniteTransition = rememberInfiniteTransition(label = "colon")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "colonAlpha"
    )

    Text(
        text = ":",
        style = textStyle,
        modifier = modifier.graphicsLayer { this.alpha = alpha }
    )
}
```

---

## Step 5: Full Digital Clock

```kotlin
@Composable
fun DigitalClock(
    modifier: Modifier = Modifier,
    showSeconds: Boolean = true,
    use24Hour: Boolean = false
) {
    val time by rememberClockTime()
    
    val displayHours = if (use24Hour) time.hours else {
        val h = time.hours % 12
        if (h == 0) 12 else h
    }

    val digitStyle = MaterialTheme.typography.displayLarge.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Hours
        DoubleDigit(value = displayHours, textStyle = digitStyle)

        // Colon 1
        BlinkingColon(textStyle = digitStyle)

        // Minutes
        DoubleDigit(value = time.minutes, textStyle = digitStyle)

        // Optional: Seconds
        if (showSeconds) {
            BlinkingColon(textStyle = digitStyle)
            DoubleDigit(value = time.seconds, textStyle = digitStyle)
        }
        
        // AM/PM indicator (if 12-hour mode)
        if (!use24Hour) {
            Column(modifier = Modifier.padding(start = 4.dp)) {
                val isAm = time.hours < 12
                Text(
                    text = "AM",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isAm) MaterialTheme.colorScheme.primary else Color.Gray
                )
                Text(
                    text = "PM",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (!isAm) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
    }
}
```

---

## Variation: Flip Clock (Paper Flap)

For the actual flip-card effect (like a mechanical flip clock), use 3D Y-rotation:

```kotlin
@Composable
fun FlipDigit(currentDigit: Int, previousDigit: Int) {
    var flipped by remember { mutableStateOf(false) }
    
    LaunchedEffect(currentDigit) {
        flipped = false
        delay(50)
        flipped = true
    }
    
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 0f else -90f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "flip"
    )
    
    Box {
        // Bottom half — current digit
        Text(
            text = currentDigit.toString(),
            modifier = Modifier.graphicsLayer {
                cameraDistance = 12f * density
                rotationX = rotation.coerceAtLeast(0f)  // Only show when rotation ≥ 0
            }
        )
        // Top half — previous digit (flips away)
        Text(
            text = previousDigit.toString(),
            modifier = Modifier.graphicsLayer {
                cameraDistance = 12f * density
                rotationX = (rotation + 90f).coerceAtMost(0f)  // Leaves when < 0
            }
        )
    }
}
```

---

## Variation: Scramble / Matrix Effect

For a "cracking the code" reveal animation:

```kotlin
@Composable
fun ScrambleDigit(targetDigit: Int) {
    var displayDigit by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(targetDigit) {
        // Scramble through random digits before landing
        repeat(8) {
            displayDigit = (0..9).random()
            delay(50L)
        }
        displayDigit = targetDigit
    }
    
    AnimatedContent(
        targetState = displayDigit,
        transitionSpec = {
            fadeIn(tween(50)) togetherWith fadeOut(tween(50))
        },
        label = "scramble"
    ) { digit ->
        Text(text = digit.toString())
    }
}
```

---

## Key Concepts Learned

| Concept | Where Used |
|---------|-----------|
| `AnimatedContent` | Swapping digit with directional transition |
| `transitionSpec` | Direction-aware (targetState > initialState) |
| `slideInVertically` / `slideOutVertically` | The slot-machine motion |
| `rememberInfiniteTransition` | Blinking colon |
| `LaunchedEffect(Unit)` with `delay` | Clock tick loop |
| `graphicsLayer { rotationX }` | 3D flip card effect |
| `LaunchedEffect(targetDigit)` | Trigger animation on value change |

---

## 💡 Interview Q&A

**Q: Why does `AnimatedContent` use the lambda parameter and not the outer state?**
A: Inside the `content` lambda, the parameter represents the **snapshot of targetState for this particular content instance**. During the crossfade, both old and new content exist simultaneously. If you used the outer state, both would show the same (newest) value — breaking the animation.

**Q: How do you make the colon stop blinking?**
A: Replace `rememberInfiniteTransition` with `animate*AsState` driven by a state, or simply set `alpha = 1f` without animation.

---

**Next:** [[07 Animation/12 Recipes/01 Shimmer Effect|Shimmer Effect →]]
