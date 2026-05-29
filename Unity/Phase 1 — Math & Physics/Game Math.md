# 📐 Game Math
### Vectors, Dot Products, Rotations & Lerping

You do not need academic calculus to build games. Instead, you need **linear algebra intuition**. These six concepts form the foundation of gameplay scripting.

---

## 1. Vectors (Vector2 & Vector3)

Vectors represent both **Positions** (where an object is relative to the origin) and **Directions** (an offset in space).

```
Vector Position:  A point in space. E.g., Player at (3, 2).
Vector Direction: An offset. E.g., Move +1 on x-axis and +2 on y-axis -> (1, 2).
```

### Vector Operations

#### A. Vector Addition (`A + B`)
Adds offsets. Used for moving a position by a direction/velocity.
```
  Position A (3, 1) + Offset B (1, 2) = Result (4, 3)

       y
       │              (4,3) Result
       │             /▲
       │            / │
       │  B(1,2)   /  │
       │   ┌──────/   │
       │   │     /    │
       │   ▼    /     │
       │  (3,1)───────┘
       │  A
       └────────────────────────► x
```

#### B. Vector Subtraction (`B - A`): The "Direction Vector"
Subtracting position `A` from position `B` yields a vector pointing **from A to B**. This is the single most common operation in enemy AI and projectile code.
```csharp
// Get the vector pointing from the Enemy directly to the Player
Vector2 vectorToPlayer = player.position - enemy.position;
```
```
        y
        │            B (Player Position)
        │            ▲
        │           /
        │          /  vectorToPlayer = B - A
        │         /
        │        A (Enemy Position)
        └────────────────────────► x
```

#### C. Normalization (`.normalized`)
Normalization scales a vector's length (magnitude) to exactly `1.0` while maintaining its direction. 
*   **Why it is critical**: If you add up diagonal inputs (e.g., pressing `W` and `D`), the input vector magnitude is $\sqrt{1^2 + 1^2} \approx 1.41$. Without normalization, the player would walk 41% faster diagonally!
```csharp
// Ensure movement speed is uniform in all directions
Vector2 moveDirection = rawInput.normalized; // Length is now 1.0f
rb.linearVelocity = moveDirection * movementSpeed;
```

---

## 2. Dot Product (`Vector2.Dot`)

The Dot Product takes two vectors and outputs a single float value. It measures how aligned the vectors are.
$$\vec{A} \cdot \vec{B} = |\vec{A}| |\vec{B}| \cos(\theta)$$
If both vectors are normalized (magnitude of 1), the Dot Product returns exactly $\cos(\theta)$, which ranges from `-1.0` to `1.0`:

```
   Dot(A, B) = 1.0        Dot(A, B) = 0.0        Dot(A, B) = -1.0
      Same Dir             Perpendicular             Opposite
       ▲   ▲                     ▲                      ▲
       │   │                     │                      │
       │   │                 └───┴───►                  │
       A   B                   B     A                  A   ▼ B
```

### Practical Gameplay Use Cases

#### A. Uprightness Check (Lander Game)
Determine if a landing ship is tilted too far away from the world's vertical axis.
```csharp
// Vector2.up is world up (0,1). transform.up is the lander's local head orientation
float alignment = Vector2.Dot(Vector2.up, transform.up);

// alignment = 1.0 (perfectly vertical), alignment = 0.0 (horizontal tilt)
bool isUpright = alignment > 0.9f; // True if tilted less than ~25 degrees
```

#### B. Facing / Field of View (FOV) Check
Determine if an enemy is facing the player.
```csharp
Vector2 toPlayer = (player.position - enemy.position).normalized;
float facingAlignment = Vector2.Dot(enemy.transform.up, toPlayer);

// If alignment is > 0.707, player is within the enemy's forward 90-degree field of view
if (facingAlignment > 0.707f)
{
    SpotPlayer();
}
```

---

## 3. Magnitude, Distance, and `sqrMagnitude`

*   **Magnitude (`Vector2.magnitude`)**: The length of the vector, calculated using the Pythagorean theorem: $\sqrt{x^2 + y^2}$.
*   **Distance (`Vector2.Distance(A, B)`)**: Shorthand for `(B - A).magnitude`.

> [!IMPORTANT]
> **Performance Trick: Use `sqrMagnitude` for comparisons**
> Calculating square roots ($\sqrt{}$) is a CPU-intensive operation. In update loops, avoid `Distance()` or `magnitude`. Instead, compare the squared distances using `sqrMagnitude`.

```csharp
// UNOPTIMIZED: Performs a slow square root check every frame
if (Vector2.Distance(transform.position, player.position) < attackRange) { ... }

// OPTIMIZED: Zero square root calculations
float rangeSquared = attackRange * attackRange;
Vector2 offset = player.position - transform.position;
if (offset.sqrMagnitude < rangeSquared)
{
    AttackPlayer();
}
```

---

## 4. Lerp (Linear Interpolation)

Lerp slides smoothly between a starting value `A` and an ending value `B` by an interpolation factor `t` (clamped between `0.0` and `1.0`).
$$\text{Lerp}(A, B, t) = A + t(B - A)$$
*   `t = 0.0` returns `A`.
*   `t = 1.0` returns `B`.
*   `t = 0.5` returns the exact midpoint.

### The Framerate-Independent Lerp Bug & Fix
Most developers write camera or object smoothing incorrectly:
```csharp
// WRONG: Frame-rate dependent! 
// A high FPS machine will Lerp faster/further than a low FPS machine.
transform.position = Vector3.Lerp(transform.position, targetPosition, smoothingSpeed * Time.deltaTime);
```
**The Professional Fix:** Use an exponential decay formula to guarantee the same speed regardless of frame rate:
$$\text{Current} = \text{Lerp}(\text{Current}, \text{Target}, 1 - e^{-\text{decaySpeed} \times \Delta t})$$
```csharp
// CORRECT: Framerate-independent smoothing
float t = 1f - Mathf.Exp(-decaySpeed * Time.deltaTime);
transform.position = Vector3.Lerp(transform.position, targetPosition, t);
```

---

## 5. Trigonometry & Rotations in 2D

Unity stores rotations as **Quaternions** (complex 4D vectors that prevent Gimbal Lock), but C# scripting relies on Euler conversions.

### Rotating a 2D Sprite to Face a Target
To rotate a 2D sprite (e.g., a homing missile or turret) towards a target, you must calculate the angle of the direction vector relative to the coordinate axes using `Mathf.Atan2(y, x)`.

```csharp
void FaceTarget(Vector3 targetPosition)
{
    // 1. Get the direction vector
    Vector2 direction = (targetPosition - transform.position).normalized;

    // 2. Atan2 returns radians. Multiply by Rad2Deg to convert to degrees (-180 to 180)
    float angle = Mathf.Atan2(direction.y, direction.x) * Mathf.Rad2Deg;

    // 3. Set the rotation around the Z axis (2D rotation)
    // Offset by -90 if your default sprite graphic points Upward (Y) instead of Right (X)
    transform.rotation = Quaternion.Euler(0, 0, angle - 90f);
}
```

---

## 6. Cross Product (`Vector3.Cross` / 2D Perp-Dot Product)

The 3D Cross Product takes two vectors and returns a third vector perpendicular to both. 
In 2D space, the cross product is simplified to the **Perp-Dot Product** (or the Z component of the 3D cross product). It determines if a target vector lies to the **left** or **right** of an object's forward direction.

```
       Target (Left)                     Target (Right)
         ▲                                     ▲
          \  Direction                          \  Direction
           \                                     \
            \                                     \
    ═════════■ transform.up               ═════════■ transform.up
    Cross.z is Positive (> 0)             Cross.z is Negative (< 0)
```

```csharp
// Determine if we need to rotate clockwise or counter-clockwise to face a target
Vector3 cross = Vector3.Cross(transform.up, directionToTarget);

if (cross.z > 0f)
{
    // Target is to the left; apply positive torque (counter-clockwise)
    rb.AddTorque(steerForce);
}
else
{
    // Target is to the right; apply negative torque (clockwise)
    rb.AddTorque(-steerForce);
}
```

---

## 7. Coordinate Space conversions
Unity uses distinct coordinate contexts:

```
┌─────────────────┐                     ┌─────────────────┐
│  SCREEN SPACE   │  Camera Screen      │   WORLD SPACE   │
│                 ├────────────────────►│                 │
│ ∙ Raw pixels    │  To World Point     │ ∙ Global origin │
│ ∙ Mouse position│                     │ ∙ Physics space │
└─────────────────┘                     └─────────────────┘
         ▲                                       ▲
         │ Convert                               │ Convert
         │                                       │
┌────────┴────────┐                     ┌────────┴────────┐
│ VIEWPORT SPACE  │                     │   LOCAL SPACE   │
│                 │                     │                 │
│ ∙ 0.0 to 1.0    │                     │ ∙ Parent relative│
│ ∙ Camera bound  │                     │ ∙ Pivot relative│
└─────────────────┘                     └─────────────────┘
```

*   **World Space**: The global sandbox environment.
*   **Local Space**: Relative to the parent's position, scale, and rotation.
*   **Screen Space**: Measured in pixels representing the active display window. (Mouse position uses this).
*   **Viewport Space**: Normalized coordinates across the camera boundary. `(0,0)` is bottom-left, `(1,1)` is top-right.

```csharp
// Convert Screen position (mouse click) to World coordinates for a 2D check
Vector3 mouseScreenPos = Input.mousePosition;
Vector3 mouseWorldPos = Camera.main.ScreenToWorldPoint(mouseScreenPos);
// Reset Z coordinate to 0 in 2D
mouseWorldPos.z = 0f;

// Convert local offset to world position
Vector3 spawnPosition = transform.TransformPoint(new Vector3(0, -2f, 0)); // 2 units below local coordinates
```

---

## 🎓 Recommended Visual Resources

*   **Freya Holmér - Math for Game Devs**: Excellent multi-part video course on vector algebra and trigonometry.
*   **3Blue1Brown - Essence of Linear Algebra**: Deep visual intuition for vectors, dot products, and transformations.
