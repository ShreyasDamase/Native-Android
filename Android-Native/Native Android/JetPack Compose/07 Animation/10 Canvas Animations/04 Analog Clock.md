# 🕐 Analog Clock — Hands & Dial

> [!NOTE]
> One of the best exercises to master Canvas + animation + coroutines in Compose. We draw an analog clock with smooth-moving hands using real system time.

---

## Concepts Used
- `Canvas` + `DrawScope` — draw shapes, arcs, lines
- `withFrameNanos` or `LaunchedEffect(Unit)` — update every frame
- `rotate()` — transform draw calls
- Basic trigonometry — angle calculation from time

---

## The Math

Converting time to angle:

```
Seconds hand:  angle = (seconds / 60) × 360°
Minutes hand:  angle = ((minutes + seconds/60) / 60) × 360°
Hours hand:    angle = ((hours % 12 + minutes/60) / 12) × 360°

Note: 0° = 12 o'clock position (top)
In DrawScope, rotate(degrees) starts from top (12 o'clock) ✅
```

```text
  ┌─────────────────────────────────────────────────────────────────┐
  │                    CLOCK FACE COORDINATES                       │
  │                                                                 │
  │                     12 (0° / 360°)                              │
  │                        │                                        │
  │             11         │         1                              │
  │                 ╲      │      ╱                                 │
  │           10      ╲    │    ╱      2                            │
  │                     ╲  │  ╱                                     │
  │         9 (270°) ───── pivot ───── 3 (90°)                      │
  │                     ╱  │  ╲                                     │
  │            8      ╱    │    ╲      4                            │
  │                 ╱      │      ╲                                 │
  │             7          │         5                              │
  │                        │                                        │
  │                        6 (180°)                                 │
  │                                                                 │
  │   Hour Hand Length:    50% of radius (Thickest)                 │
  │   Minute Hand Length:  75% of radius (Medium)                   │
  │   Second Hand Length:  85% of radius (Thinnest)                 │
  └─────────────────────────────────────────────────────────────────┘
```

---

## Step 1: Time State (Updates Every Second for Hands)

```kotlin
@Composable
fun rememberCurrentTime(): State<Calendar> {
    val timeState = remember { mutableStateOf(Calendar.getInstance()) }

    LaunchedEffect(Unit) {
        while (true) {
            timeState.value = Calendar.getInstance()
            delay(16L)  // ~60fps for smooth second hand
        }
    }

    return timeState
}
```

---

## Step 2: Draw the Clock Face

```kotlin
@Composable
fun AnalogClock(
    modifier: Modifier = Modifier,
    dialColor: Color = Color.DarkGray,
    handColor: Color = Color.White,
    accentColor: Color = Color.Red,
    secondHandColor: Color = Color.Red
) {
    val time by rememberCurrentTime()

    val hours = time.get(Calendar.HOUR)       // 0-11
    val minutes = time.get(Calendar.MINUTE)   // 0-59
    val seconds = time.get(Calendar.SECOND)   // 0-59
    val millis = time.get(Calendar.MILLISECOND) // 0-999 (for smooth second hand)

    Canvas(modifier = modifier.size(200.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 8.dp.toPx()

        // ── Dial Background ──────────────────────────────────────────────
        drawCircle(
            color = dialColor,
            radius = radius,
            center = center
        )

        // ── Outer Ring ───────────────────────────────────────────────────
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // ── Hour Tick Marks (12 marks) ───────────────────────────────────
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30).toDouble())
            val outerX = center.x + radius * sin(angle).toFloat()
            val outerY = center.y - radius * cos(angle).toFloat()
            val innerX = center.x + (radius - 12.dp.toPx()) * sin(angle).toFloat()
            val innerY = center.y - (radius - 12.dp.toPx()) * cos(angle).toFloat()

            drawLine(
                color = Color.White,
                start = Offset(outerX, outerY),
                end = Offset(innerX, innerY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // ── Minute Tick Marks (60 marks, skip hour marks) ────────────────
        for (i in 0 until 60) {
            if (i % 5 == 0) continue  // Skip — already drew hour marks
            val angle = Math.toRadians((i * 6).toDouble())
            val outerX = center.x + radius * sin(angle).toFloat()
            val outerY = center.y - radius * cos(angle).toFloat()
            val innerX = center.x + (radius - 6.dp.toPx()) * sin(angle).toFloat()
            val innerY = center.y - (radius - 6.dp.toPx()) * cos(angle).toFloat()

            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(outerX, outerY),
                end = Offset(innerX, innerY),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // ── Hours Hand ────────────────────────────────────────────────────
        val hourAngle = ((hours + minutes / 60f) / 12f) * 360f
        rotate(degrees = hourAngle, pivot = center) {
            drawLine(
                color = handColor,
                start = center,
                end = Offset(center.x, center.y - radius * 0.5f),  // 50% of radius length
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // ── Minutes Hand ──────────────────────────────────────────────────
        val minuteAngle = ((minutes + seconds / 60f) / 60f) * 360f
        rotate(degrees = minuteAngle, pivot = center) {
            drawLine(
                color = handColor,
                start = center,
                end = Offset(center.x, center.y - radius * 0.75f),  // 75% of radius
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // ── Seconds Hand (smooth with millisecond precision) ──────────────
        val secondAngle = ((seconds + millis / 1000f) / 60f) * 360f
        rotate(degrees = secondAngle, pivot = center) {
            // Tail (goes backward)
            drawLine(
                color = secondHandColor,
                start = Offset(center.x, center.y + radius * 0.2f),
                end = Offset(center.x, center.y - radius * 0.85f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // ── Center Dot ────────────────────────────────────────────────────
        drawCircle(
            color = secondHandColor,
            radius = 5.dp.toPx(),
            center = center
        )
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = center
        )
    }
}
```

---

## Step 3: Usage

```kotlin
@Composable
fun ClockScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnalogClock(
            modifier = Modifier.size(250.dp),
            dialColor = MaterialTheme.colorScheme.surface,
            handColor = MaterialTheme.colorScheme.onSurface,
            secondHandColor = MaterialTheme.colorScheme.primary
        )
    }
}
```

---

## Making It Truly Smooth (withFrameNanos)

For a perfectly smooth seconds hand (no 1-second jumps), use `withFrameNanos`:

```kotlin
@Composable
fun rememberSmoothTime(): State<Long> {
    val timeMs = remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { _ ->
                timeMs.longValue = System.currentTimeMillis()
            }
        }
    }

    return timeMs
}

// Then in your Canvas composable:
val timeMs by rememberSmoothTime()
val calendar = remember { Calendar.getInstance() }

Canvas(modifier = modifier) {
    calendar.timeInMillis = timeMs
    val seconds = calendar.get(Calendar.SECOND)
    val millis = calendar.get(Calendar.MILLISECOND)
    val secondAngle = ((seconds + millis / 1000f) / 60f) * 360f
    // ...draw hands
}
```

---

## Adding Spring to the Second Hand

For a "ticking" spring effect (like luxury watches):

```kotlin
// Target angle from time
val targetSecondAngle = ((seconds) / 60f) * 360f

// Animate with spring
val secondAngle by animateFloatAsState(
    targetValue = targetSecondAngle,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessHigh
    ),
    label = "secondHand"
)
```

---

## Key Concepts Learned

| Concept | Used For |
|---------|---------|
| `Canvas` + `DrawScope` | Raw drawing: circles, lines, arcs |
| `rotate(degrees, pivot)` | Rotating draw calls around clock center |
| `LaunchedEffect(Unit)` | Continuous update loop |
| `delay(16L)` | ~60fps refresh |
| `Math.toRadians()` | Converting degrees to radians for sin/cos |
| `sin()`, `cos()` | Placing tick marks on circle circumference |
| `StrokeCap.Round` | Rounded hand/tick ends |
| `withFrameNanos` | Frame-accurate timing |

---

## 💡 Why This Exercise Is Valuable

1. You learn **Canvas drawing** — the foundation of custom UI
2. You learn **update loops** — how to keep state synchronized with time
3. You learn **coordinate math** — sin/cos for circular layouts
4. You understand **why `rotate()` works around a pivot** — the transform origin concept
5. You see how `LaunchedEffect(Unit)` differs from `rememberInfiniteTransition`

---

**Next:** [[07 Animation/10 Canvas Animations/05 Digital Clock Effect|Digital Clock Flip Effect →]]
