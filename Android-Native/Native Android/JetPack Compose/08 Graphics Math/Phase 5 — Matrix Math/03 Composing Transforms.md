# 03 — Composing Transforms

> **Core Idea**: Every complex movement is a chain of simple transforms. Multiply their matrices together and you get one matrix that does everything at once. This is how bones/joints work in character animation, and how OpenGL's model matrix works.

---

## 🧠 Concept

Chain: "Rotate, then translate" vs "Translate, then rotate" — these are different!

Example: Draw a clock hand (rod) that rotates around one end.
- **Wrong**: rotate around center → hand spins in place
- **Right**: translate so the pivot end is at the origin → rotate → translate back to world position

**TRS order** (standard in most engines):
1. **Scale** first (object space)
2. **Rotate** second (local orientation)
3. **Translate** last (world position)

Matrix multiplication: `M = T × R × S` (applied right to left: Scale → Rotate → Translate)

---

## 📐 Diagram

```
ORDER MATTERS — T×R vs R×T:

  Object: rectangle at origin, 40×10

  T then R (wrong pivot):          R then T (correct):
  ┌──────────────────────────┐     ┌─────────────────────────────┐
  │  1. Move to (100, 0)     │     │  1. Rotate 45°              │
  │     ┌────────┐           │     │        ╲  ╲                 │
  │     │ object │           │     │         ╲  ╲                │
  │     └────────┘           │     │  2. Move to (100, 0)        │
  │  2. Rotate 45°           │     │             ╲  ╲            │
  │     (rotates around      │     │              ╲  ╲           │
  │      world origin!)      │     │    correct! hand points up  │
  │          ╲               │     └─────────────────────────────┘
  │           ╲  ← rotated   │
  │            around (0,0)  │
  └──────────────────────────┘

PARENT-CHILD CHAIN:

  World
  └── Hip (T_hip × R_hip)
      └── Spine (parent × T_spine × R_spine)
          └── Shoulder (parent × T_shoulder × R_shoulder)
              └── Arm (parent × T_arm × R_arm)

  Each child's world matrix = parent world matrix × child local matrix
  This is exactly what translate() { translate() { ... }} does in Canvas
```

---

## 🔢 The Math

```kotlin
// Build full transform chain
// Remember: multiply right to left — S applied first, T applied last
fun buildObjectMatrix(
    worldX: Float, worldY: Float,    // translation
    angleRad: Float,                 // rotation
    scaleX: Float = 1f,
    scaleY: Float = 1f
): Matrix3 {
    val T = translationMatrix(worldX, worldY)
    val R = rotationMatrix(angleRad)
    val S = scaleMatrix(scaleX, scaleY)
    return T * R * S  // TRS order: scale first, translate last
}

// Bone/joint chain: each joint's world = parent world × local
fun childWorldMatrix(parentWorld: Matrix3, localX: Float, localY: Float, angle: Float): Matrix3 =
    parentWorld * translationMatrix(localX, localY) * rotationMatrix(angle)
```

---

## 🎨 Compose Code

### Example 1: Robotic arm — 3-joint chain

```kotlin
@Composable
fun RoboticArmDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "arm")
    val a1 by infiniteTransition.animateFloat(
        initialValue = -0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "joint1"
    )
    val a2 by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = -0.5f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "joint2"
    )
    val a3 by infiniteTransition.animateFloat(
        initialValue = -0.8f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "joint3"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cx = size.width / 2f
        val cy = size.height * 0.7f
        val origin = Offset.Zero

        // Base
        val baseMatrix = translationMatrix(cx, cy) * rotationMatrix(a1)
        val joint1Pos = baseMatrix.transform(origin)
        val arm1End   = baseMatrix.transform(Offset(0f, -80f))

        // Forearm
        val foreMatrix = baseMatrix * translationMatrix(0f, -80f) * rotationMatrix(a2)
        val joint2Pos = foreMatrix.transform(origin)
        val arm2End   = foreMatrix.transform(Offset(0f, -70f))

        // Hand
        val handMatrix = foreMatrix * translationMatrix(0f, -70f) * rotationMatrix(a3)
        val joint3Pos = handMatrix.transform(origin)
        val handEnd   = handMatrix.transform(Offset(0f, -50f))

        // Draw arms
        drawLine(Color(0xFF4A9EFF), joint1Pos, arm1End, 8f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(Color(0xFF4ECDC4), joint2Pos, arm2End, 6f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(Color(0xFFFFD700), joint3Pos, handEnd, 4f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)

        // Joints
        drawCircle(Color.White, 10f, joint1Pos)
        drawCircle(Color.White, 8f, joint2Pos)
        drawCircle(Color.White, 6f, joint3Pos)
        drawCircle(Color(0xFFFF6B6B), 6f, handEnd)  // end effector
    }
}
```

### Example 2: Transform order comparison — interactive

```kotlin
@Composable
fun TransformOrderDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "order")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "compAngle"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        val cy = size.height / 2f
        val rect = listOf(Offset(-40f, -15f), Offset(40f, -15f),
            Offset(40f, 15f), Offset(-40f, 15f))

        // LEFT: T then R (bad — rotates around world origin)
        val left = Offset(size.width * 0.25f, cy)
        val badMatrix = translationMatrix(left.x, left.y) * rotationMatrix(angle)
        val badPoints = rect.map { badMatrix.transform(it) }
        drawPolygon(badPoints, Color(0xFF2A1A1A), Color(0xFFFF6B6B))
        // This rotates correctly BUT if pivot should be at left edge, need different order

        // RIGHT: Place object, then rotate around its left edge
        val right = Offset(size.width * 0.65f, cy)
        // Pivot at left edge = translate object so left edge is at origin, rotate, translate to world
        val rightMatrix = translationMatrix(right.x, right.y) *
                          rotationMatrix(angle) *
                          translationMatrix(40f, 0f)  // offset so pivot is left edge
        val rightPoints = rect.map { rightMatrix.transform(it) }
        drawPolygon(rightPoints, Color(0xFF1A2A1A), Color(0xFF4ECDC4))

        // Pivot dot
        drawCircle(Color.White, 6f, left)
        drawCircle(Color.White, 6f, right)
        drawLine(Color(0xFF1A1A3A), Offset(size.width/2f, 0f),
            Offset(size.width/2f, size.height), 1f)
    }
}
```

---

## 🧪 Trace

**Robotic arm joint 1 at origin, rotate -30°, arm length 80. Joint 2 offset by (0, -80), rotate 20°**:

```
Base matrix = T(cx, cy) × R(-0.524)   [−30° = -π/6 rad]
  Apply to (0, -80):
    After R(-0.524):
      x' = 0×cos(-0.524) - (-80)×sin(-0.524) = 0 - (-80×(-0.5)) = -40
      y' = 0×sin(-0.524) + (-80)×cos(-0.524) = 0 + (-80×0.866) = -69.3
    arm1End in world = (cx - 40, cy - 69.3)

Forearm matrix = base × T(0,-80) × R(0.349)   [20° = π/9 rad]
  This chains: move to arm1End, then rotate 20° locally
```

---

## 🔗 Next

→ [[04 Project — 3D Cube Projection]] — *Project a 3D cube to 2D screen using rotation matrices and perspective projection*
