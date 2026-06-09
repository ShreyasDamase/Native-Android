# Canvas & DrawScope Overview

## 📌 Purpose
`Canvas` is a core UI component in Jetpack Compose that provides direct access to 2D drawing primitives. It acts as a wrapper around the `DrawScope` interface, which defines an entirely declarative, state-aware DSL for drawing lines, shapes, text, and images.

Use Canvas for custom charts, vintage UI controls (knobs, meters), complex animations, and any component that cannot be easily assembled using standard rows, columns, and boxes.

## 🗺️ Coordinate System
- **Origin (0,0)**: Top-Left corner of the Canvas.
- **X-axis**: Increases towards the Right.
- **Y-axis**: Increases towards the Bottom.
- Dimensions are measured in **Pixels** (`Float`), not `Dp`. You must use `.toPx()` if mapping from Compose `Dp`.

## 🧩 The DrawScope Environment
When you open a `Canvas { ... }` block, you are inside `DrawScope`. 
Key properties available automatically:
- `size: Size` — Dimensions of the canvas area.
- `center: Offset` — Center point coordinates.
- `layoutDirection: LayoutDirection` — LTR or RTL support.
- `drawContext: DrawContext` — Access to underlying native canvas and transform manipulation.

## 🔗 Related Components
- `Canvas`: A standalone composable that reserves layout space and draws.
- `Modifier.drawBehind`: Draws *behind* an existing composable.
- `Modifier.drawWithContent`: Draws *above, below, or modifies* an existing composable.

## 📚 Deep Dive Topics

Explore the detailed Canvas functionality in the following guides:

1. [[01-Basic-Drawing]] - Primitives (Rects, Circles, Lines, Arcs)
2. [[02-Paint-Stroke-Fill]] - Styles, Caps, Joins, PathEffects, BlurMaskFilters
3. [[03-Gradients-Brushes]] - Linear, Radial, Sweep, and Bevel recipes
4. [[04-Path-Building]] - Custom complex shapes, Beziers, Path Operations
5. [[05-Transformations]] - Translate, Scale, Rotate, Inset
6. [[06-Text-On-Canvas]] - TextMeasurer API, Text along paths
7. [[07-Clipping-Layers]] - clipRect, clipPath, saveLayer, Native Canvas interop
8. [[08-BlendModes]] - All 29 compositing modes
9. [[09-Animations-With-Canvas]] - Animatable, animateFloatAsState, infiniteTransition
10. [[10-Interaction-Gestures]] - Detect taps, rotary drag gestures
11. [[11-Custom-Modifier-Extensions]] - Creating reusable custom DrawModifiers
12. [[12-Performance]] - State reads, invalidation bounds, drawWithCache best practices
