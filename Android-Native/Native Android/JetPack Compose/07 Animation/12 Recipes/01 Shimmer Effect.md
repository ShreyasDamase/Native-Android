# ✨ Shimmer / Loading Skeleton

> [!NOTE]
> The shimmer effect is the go-to loading state in modern apps. A moving highlight sweeps across a gray placeholder, suggesting content is loading. Learn to build it from scratch using `rememberInfiniteTransition` + `drawWithCache` + `ShaderBrush`.

---

## The Concept

```text
  ┌─────────────────────────────────────────────────────────────────┐
  │                 LINEAR GRADIENT SHIMMER SWEEP                   │
  │                                                                 │
  │  translateX = -shimmerWidth (Start Off-Screen Left)             │
  │  ╔═══════════╗                                                  │
  │  ║ Highlight ║ ┌─────────────────────────────────────────────┐  │
  │  ╚═══════════╝ │           Grey Placeholder Component        │  │
  │                └─────────────────────────────────────────────┘  │
  │                                                                 │
  │  translateX = width / 2 (Sweeping Through Midpoint)             │
  │                ┌─────────────────────────────────────────────┐  │
  │                │         Grey ╔═══════════╗ Grey             │  │
  │                │         Component Highlight ║ Component     │  │
  │                └──────────────╚═══════════╝──────────────────┘  │
  │                                                                 │
  │  translateX = width + shimmerWidth (End Off-Screen Right)       │
  │                ┌─────────────────────────────────────────────┐  │
  │                │           Grey Placeholder Component        │  │
  │                └─────────────────────────────────────────────┘  │
  │                                                  ╔═══════════╗  │
  │                                                  ║ Highlight ║  │
  │                                                  ╚═══════════╝  │
  └─────────────────────────────────────────────────────────────────┘
```

A bright gradient "window" sweeps continuously from left to right over a gray background. Implemented with:

1. `rememberInfiniteTransition` — provides the moving X offset
2. `Brush.linearGradient` — creates the sweep gradient
3. `drawWithCache` — efficient rendering without allocation per frame

---

## Method 1: Using `drawWithCache` (Most Performant)

```kotlin
@Composable
fun Modifier.shimmerEffect(
    shimmerColor: Color = Color.White,
    baseColor: Color = Color.LightGray.copy(alpha = 0.4f)
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")

    val shimmerTranslateX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,  // We use normalized 0→1, convert in drawWithCache
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )

    return this.drawWithCache {
        // This block runs once (or when size changes) — no allocation per frame
        val width = size.width
        val shimmerWidth = width * 0.6f  // Shimmer highlight width = 60% of total

        onDrawBehind {
            val start = (width + shimmerWidth) * shimmerTranslateX - shimmerWidth
            val end = start + shimmerWidth

            val brush = Brush.linearGradient(
                colors = listOf(
                    baseColor,
                    shimmerColor.copy(alpha = 0.8f),
                    baseColor
                ),
                start = Offset(start, 0f),
                end = Offset(end, 0f)
            )

            drawRect(brush = brush)
        }
    }
}
```

> [!WARNING]
> Do NOT create `Brush.linearGradient` inside `onDrawBehind` — that runs 60 times/second. Create it with `drawWithCache` for caching, then use it inside `onDrawBehind`.

Wait — actually in this pattern, we need to update the Brush EVERY frame because `shimmerTranslateX` changes. The correct pattern uses `drawWithCache` for the shape, and dynamic brush inside:

```kotlin
// Corrected — brush must be inside onDrawBehind since it uses animating values
fun Modifier.shimmerEffect(
    shimmerColors: List<Color> = listOf(
        Color.LightGray.copy(alpha = 0.3f),
        Color.LightGray.copy(alpha = 0.8f),
        Color.LightGray.copy(alpha = 0.3f)
    )
): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -300f,
        targetValue = 300f,   // Pixels — using absolute values
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "shimmerAnim"
    )

    return this.background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateX - 100, 0f),
            end = Offset(translateX + 100, 0f)
        )
    )
}
```

---

## Method 2: Size-Aware Shimmer (Best for Variable Width)

```kotlin
@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    var componentSize by remember { mutableStateOf(IntSize.Zero) }
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val animatedProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Box(
        modifier = modifier
            .onSizeChanged { componentSize = it }
            .drawWithContent {
                drawContent()

                val width = componentSize.width.toFloat()
                val shimmerStart = width * animatedProgress - width * 0.4f
                val shimmerEnd = shimmerStart + width * 0.4f

                drawRect(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.5f to Color.White.copy(alpha = 0.5f),
                            1f to Color.Transparent
                        ),
                        start = Offset(shimmerStart, 0f),
                        end = Offset(shimmerEnd, 0f)
                    )
                )
            }
    )
}
```

---

## Full Loading Skeleton Screen

```kotlin
@Composable
fun PostCardSkeleton() {
    val shimmerBrush = rememberShimmerBrush()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar circle skeleton
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(shimmerBrush)
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Name line skeleton
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(shimmerBrush)
                )
                // Subtitle line skeleton
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(shimmerBrush)
                )
            }
        }

        // Image placeholder skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(shimmerBrush)
        )

        // Text lines
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (index == 2) 0.6f else 1f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

@Composable
fun rememberShimmerBrush(): Brush {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.4f),
        Color.LightGray.copy(alpha = 0.8f),
        Color.LightGray.copy(alpha = 0.4f)
    )

    val transition = rememberInfiniteTransition(label = "shimmerBrush")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation - 100f, 0f),
        end = Offset(translateAnimation + 100f, 0f)
    )
}
```

---

## Dark Mode Shimmer

```kotlin
@Composable
fun rememberShimmerBrush(
    isDarkMode: Boolean = isSystemInDarkTheme()
): Brush {
    val colors = if (isDarkMode) {
        listOf(
            Color(0xFF2A2A2A),
            Color(0xFF3D3D3D),
            Color(0xFF2A2A2A)
        )
    } else {
        listOf(
            Color(0xFFE8E8E8),
            Color(0xFFF5F5F5),
            Color(0xFFE8E8E8)
        )
    }
    // ...rest same as above
}
```

---

## Key Concepts Learned

| Concept | How Used |
|---------|---------|
| `rememberInfiniteTransition` | Drives the continuous sweep |
| `Brush.linearGradient` | Creates the gradient highlight |
| `RepeatMode.Restart` | Resets and sweeps again |
| `LinearEasing` | Constant speed sweep (no easing) |
| `drawWithCache` | Caches expensive objects |
| `onDrawBehind` | Draws behind content |
| `Modifier.background(brush)` | Applies gradient as background |

---

## When Shimmer Appears

```kotlin
// With data loading pattern
@Composable
fun ContentOrSkeleton(isLoading: Boolean, content: @Composable () -> Unit) {
    AnimatedContent(
        targetState = isLoading,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        label = "loadingState"
    ) { loading ->
        if (loading) {
            PostCardSkeleton()
        } else {
            content()
        }
    }
}
```

---

## 💡 Interview Q&A

**Q: Why use `LinearEasing` for shimmer?**
A: The shimmer should sweep at a constant speed — like a spotlight moving across a surface. Easing would make it slow down and speed up, which looks unnatural for this effect.

**Q: Why `RepeatMode.Restart` instead of `Reverse` for shimmer?**
A: Shimmer moves consistently in ONE direction (left to right). `Reverse` would make it sweep right-to-left every other cycle, which looks wrong. `Restart` always sweeps left-to-right.

---

**Next:** [[07 Animation/12 Recipes/02 Shine Effect|Shine Effect →]]
