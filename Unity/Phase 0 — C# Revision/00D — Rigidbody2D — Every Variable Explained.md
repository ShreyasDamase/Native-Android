# 00D — Rigidbody2D — Every Variable Explained
### 🔶 Block 0 — Unity Engine Fundamentals

> [!IMPORTANT]
> **This is the file you asked for.** `Rigidbody2D` is the component that makes 2D physics work — gravity, forces, velocity, collision response. Every field in the Inspector is explained here with exactly what it does, when to change it, and what values are typical.

---

## 00D.1 — What is Rigidbody2D?

A **Rigidbody2D** is a component that makes a GameObject obey **2D physics laws**. Without it, a GameObject just sits in space and doesn't move or respond to physics at all.

When you add Rigidbody2D to a GameObject, Unity's physics engine takes over:
- Gravity pulls it down every frame
- Forces (like jump force) accelerate it
- Colliders cause it to bounce, stop, or slide against other objects
- Drag slows it down over time

```
WITHOUT Rigidbody2D                  WITH Rigidbody2D
─────────────────────────            ─────────────────────────
Object just floats at (0,0)          Object falls under gravity
You must manually move it            Physics moves it for you
Doesn't respond to forces            Responds to AddForce()
Colliders do nothing                 Colliders cause bounces/stops
```

**How to add it:** Select a GameObject → Inspector → Add Component → Rigidbody 2D

---

## 00D.2 — Every Inspector Field Explained

When you add Rigidbody2D to a GameObject and look at it in the Inspector, you see this:

```
Rigidbody 2D
├── Body Type           [Dynamic      ▼]
├── Material            [None (Physics Material 2D)]
├── Simulated           [✓]
├── Use Auto Mass       [□]
├── Mass                [1]
├── Linear Drag         [0]
├── Angular Drag        [0.05]
├── Gravity Scale       [1]
├── Collision Detection [Discrete     ▼]
├── Sleeping Mode       [Start Awake  ▼]
└── Constraints
    ├── Freeze Position [□ X] [□ Y]
    └── Freeze Rotation [□ Z]
```

Let's go through every single one:

---

### `Body Type` — The Most Important Field

Controls the fundamental physics behavior of this object.

```
Body Type Options:
┌────────────────────────────────────────────────────────────────────────┐
│ DYNAMIC (default)                                                      │
│   - Fully simulated — gravity, forces, collision response, velocity    │
│   - Use for: player, enemies, projectiles, physics objects             │
│   - Most expensive (most features)                                     │
├────────────────────────────────────────────────────────────────────────┤
│ KINEMATIC                                                              │
│   - You control movement via code (MovePosition/MoveRotation)          │
│   - NOT affected by gravity or forces from other objects               │
│   - Still DETECTS collisions with Dynamic bodies                       │
│   - Use for: moving platforms, doors, elevators, camera rails          │
│   - Medium cost                                                        │
├────────────────────────────────────────────────────────────────────────┤
│ STATIC                                                                 │
│   - NEVER moves. Eternal immovable obstacle.                           │
│   - Use for: ground, walls, static platforms                           │
│   - Cheapest (no simulation)                                           │
│   - For things that NEVER move, prefer using a Collider WITHOUT a      │
│     Rigidbody at all — even cheaper                                    │
└────────────────────────────────────────────────────────────────────────┘
```

```csharp
// Change body type in code
rb.bodyType = RigidbodyType2D.Dynamic;
rb.bodyType = RigidbodyType2D.Kinematic;
rb.bodyType = RigidbodyType2D.Static;

// Common pattern: switch to kinematic during cutscene, then back
void EnterCutscene()
{
    rb.bodyType = RigidbodyType2D.Kinematic;
    rb.linearVelocity = Vector2.zero;
}

void ExitCutscene()
{
    rb.bodyType = RigidbodyType2D.Dynamic;
}
```

---

### `Material` — Bounciness and Friction

A **Physics Material 2D** asset that controls how surfaces interact on contact.

```csharp
// Physics Material 2D has two properties:
// - Friction (0 = ice, 1 = rubber, typical = 0.4)
// - Bounciness (0 = no bounce, 1 = perfect elastic bounce)

// Set material via code
PhysicsMaterial2D iceMat = new PhysicsMaterial2D();
iceMat.friction = 0f;
iceMat.bounciness = 0f;
rb.sharedMaterial = iceMat;

// Common setups:
// Player: friction=0.4, bounciness=0 (realistic)
// Ball: friction=0.3, bounciness=0.7 (bouncy)
// Ice platform: apply on the platform's collider, friction=0, bounciness=0
// Bouncy pad: bounciness=1.5 (super bounce), friction=0
```

---

### `Simulated` — On/Off Switch for Physics

When unchecked, the Rigidbody is **completely removed from the physics simulation** — no gravity, no collision, nothing. The object becomes "physics-invisible".

```csharp
rb.simulated = false; // completely disable all physics on this object
rb.simulated = true;  // re-enable

// Use case: a "ghost" item that's currently being dragged or out of bounds
// Use case: pausing physics on inactive pool objects
```

---

### `Mass` — How Heavy It Is

Mass affects how the object responds to forces and collisions. Higher mass = harder to push around.

```
Mass = 1    → Normal object (player, typical enemy)
Mass = 5    → Heavy (boulder, tank)
Mass = 0.1  → Very light (feather, paper, leaf)
```

```csharp
rb.mass = 1f;  // default

// Physics formula: Force = Mass × Acceleration
// So if you want to accelerate a heavier object to the same speed, you need more force

// Impact between objects — heavier object pushes lighter one more
// Mass ratio = 10:1 → lighter object bounces far more

// Typical game values:
// Player:      1f
// Enemy:       2f
// Boss:        10f
// Projectile:  0.01f
// Crate:       3f
```

---

### `Linear Drag` — Air Resistance on Movement

Drag slows down LINEAR (movement) velocity over time. Think of it as air resistance or friction from the environment.

```
Linear Drag = 0    → No drag. Object keeps moving forever in space. (projectiles)
Linear Drag = 1    → Moderate drag. Slows down noticeably in air.
Linear Drag = 5    → Heavy drag. Stops very quickly.
Linear Drag = 10+  → Extremely heavy drag. Nearly stops immediately.
```

```csharp
rb.linearDamping = 0f;    // no drag — bullet, space game
rb.linearDamping = 0.5f;  // some drag — player in air
rb.linearDamping = 3f;    // heavy drag — player on ground (combined with friction)

// Common pattern: more drag when grounded, less when airborne
void Update()
{
    rb.linearDamping = isGrounded ? 5f : 0.5f;
}

// Why this matters:
// Without drag: player slides forever after letting go of input
// With drag: player decelerates naturally when no input applied
// Alternative: zero out velocity when no input — direct control
```

---

### `Angular Drag` — Air Resistance on Rotation

Same as Linear Drag but for ROTATION. Slows down spinning.

```
Angular Drag = 0     → No rotational drag. Spins forever.
Angular Drag = 0.05  → Default. Slight rotational drag.
Angular Drag = 5     → High rotational drag. Stops spinning quickly.
Angular Drag = Inf   → Effectively freeze rotation.
```

```csharp
rb.angularDamping = 0.05f; // default

// For player characters, you usually want to FREEZE rotation entirely instead
// Use Constraints.FreezeRotation (see below)
```

---

### `Gravity Scale` — How Much Gravity Affects This Object

`1.0` = normal gravity. `0.0` = no gravity. `2.0` = twice as heavy.

```
Gravity Scale = 0     → Floats. (top-down RPG, space game, bird)
Gravity Scale = 0.5   → Light gravity. (slow floaty platformer)
Gravity Scale = 1     → Normal gravity. (realistic platformer)
Gravity Scale = 2     → Heavy. (fast, heavy feel)
Gravity Scale = 3     → Very heavy. (action games, fast fall)
Gravity Scale = -1    → Upside-down gravity! (gravity-flip puzzles)
```

```csharp
rb.gravityScale = 1f;  // normal
rb.gravityScale = 0f;  // no gravity — top-down game or flying
rb.gravityScale = 3f;  // heavy — feels snappy and responsive

// Tweak in FixedUpdate for better jump feel:
if (rb.linearVelocity.y < 0f)
    rb.gravityScale = fallGravityMultiplier; // fall faster (e.g. 2.5)
else if (rb.linearVelocity.y > 0f && !Input.GetButton("Jump"))
    rb.gravityScale = lowJumpMultiplier;     // cut jump short (e.g. 2)
else
    rb.gravityScale = 1f;                   // normal on the way up
```

---

### `Collision Detection` — How Collisions Are Detected

Determines whether fast-moving objects can pass through thin walls.

```
Discrete (default):
  - Checks position AFTER the physics step
  - Fast objects can pass through thin walls (called "tunneling")
  - Cheapest
  - Fine for slow/medium speed objects

Continuous:
  - Checks the PATH between old and new position
  - Fast objects (bullets, fast players) correctly hit thin walls
  - More expensive
  - Use for: projectiles, fast enemies
```

```csharp
rb.collisionDetectionMode = CollisionDetectionMode2D.Discrete;
rb.collisionDetectionMode = CollisionDetectionMode2D.Continuous;

// Rule of thumb:
// Player:     Discrete (usually fine)
// Bullet:     Continuous (tunneling is very visible with bullets)
// Normal enemy: Discrete
// Fast enemy: Continuous
```

---

### `Sleeping Mode` — Performance Optimization

When an object is completely still, Unity can "put it to sleep" to save CPU. A sleeping object doesn't update physics.

```
Start Awake (default): Object begins active, goes to sleep when still
Start Asleep:          Object begins asleep — wakes up when a force hits it
Never Sleep:           Always active — use for player or objects that need constant updates
```

```csharp
rb.sleepMode = RigidbodySleepMode2D.StartAwake;  // default
rb.sleepMode = RigidbodySleepMode2D.NeverSleep;  // player — always needs to be active

// Manually wake a sleeping object
rb.WakeUp();

// Force an object to sleep
rb.Sleep();
bool isSleeping = rb.IsSleeping();
```

---

### `Constraints` — Locking Axes

Prevent the physics engine from moving or rotating the object on specific axes.

```
Freeze Position X: Object cannot move left/right
Freeze Position Y: Object cannot move up/down (disable gravity essentially)
Freeze Rotation Z: Object cannot spin (VERY COMMON for player characters)
```

```csharp
// Freeze rotation — player characters should NEVER spin from physics
rb.constraints = RigidbodyConstraints2D.FreezeRotation;

// Freeze specific position axes
rb.constraints = RigidbodyConstraints2D.FreezePositionX; // lock X movement
rb.constraints = RigidbodyConstraints2D.FreezePositionY; // lock Y movement (floating platform)

// Combine constraints with | operator
rb.constraints = RigidbodyConstraints2D.FreezePositionX | RigidbodyConstraints2D.FreezeRotation;

// Freeze everything
rb.constraints = RigidbodyConstraints2D.FreezeAll;

// No constraints (fully dynamic)
rb.constraints = RigidbodyConstraints2D.None;
```

---

## 00D.3 — All Rigidbody2D Properties You Read in Code

```csharp
// Reading current physical state
Vector2 vel = rb.linearVelocity;       // current velocity (direction + speed)
float speed = rb.linearVelocity.magnitude; // how fast it's moving
float angVel = rb.angularVelocity;     // rotation speed in degrees/second
Vector2 pos = rb.position;             // physics position (slightly different from transform.position)
float rot = rb.rotation;               // physics rotation in degrees

// Position and velocity checks
bool isMovingRight = rb.linearVelocity.x > 0.1f;
bool isMovingLeft  = rb.linearVelocity.x < -0.1f;
bool isFalling     = rb.linearVelocity.y < -0.5f;
bool isRising      = rb.linearVelocity.y > 0.5f;
bool isStationary  = rb.linearVelocity.sqrMagnitude < 0.01f;
```

---

## 00D.4 — All Methods for Moving with Rigidbody2D

```csharp
// ===== SETTING VELOCITY DIRECTLY (most common for platformers) =====
// Set full velocity
rb.linearVelocity = new Vector2(5f, rb.linearVelocity.y); // move right, keep gravity

// Set just X velocity (preserve Y for gravity)
rb.linearVelocity = new Vector2(moveInput * speed, rb.linearVelocity.y);

// Set just Y velocity (used for jump)
rb.linearVelocity = new Vector2(rb.linearVelocity.x, jumpSpeed);

// Zero out all velocity (full stop)
rb.linearVelocity = Vector2.zero;

// ===== ADDING FORCE (physics-based movement) =====
// ForceMode2D.Force — continuous force (multiply by Time.fixedDeltaTime mentally)
// Applied over time — use for rockets, wind, sustained thrust
rb.AddForce(Vector2.up * 10f, ForceMode2D.Force);

// ForceMode2D.Impulse — instant hit of force
// Applied instantly — use for jump, explosion, knockback, bullets
rb.AddForce(Vector2.up * 10f, ForceMode2D.Impulse);

// Force relative to the object's local axes
rb.AddRelativeForce(Vector2.up * 10f, ForceMode2D.Impulse); // push in local up

// Force at a specific point (creates torque/spin)
rb.AddForceAtPosition(force, contactPoint, ForceMode2D.Impulse);

// ===== TORQUE (rotational force) =====
rb.AddTorque(10f, ForceMode2D.Impulse); // spin clockwise
rb.angularVelocity = 0f;               // stop spinning

// ===== KINEMATIC MOVEMENT (Kinematic body type only) =====
// Moving Kinematic objects — use these instead of transform.position
rb.MovePosition(rb.position + Vector2.right * speed * Time.fixedDeltaTime);
rb.MoveRotation(rb.rotation + 90f * Time.fixedDeltaTime);
```

---

## 00D.5 — Complete Setup: Player Rigidbody2D

```csharp
// How to set up a player's Rigidbody2D correctly — both in Inspector and code

public class PlayerSetup : MonoBehaviour
{
    private Rigidbody2D rb;
    
    void Awake()
    {
        rb = GetComponent<Rigidbody2D>();
        
        // Inspector settings you should also set from code if spawning dynamically:
        rb.bodyType = RigidbodyType2D.Dynamic;
        rb.mass = 1f;
        rb.linearDamping = 0f;          // control drag via code, not this
        rb.angularDamping = 0f;         // we'll freeze rotation instead
        rb.gravityScale = 3f;           // heavier gravity = more responsive feel
        rb.collisionDetectionMode = CollisionDetectionMode2D.Discrete;
        rb.sleepMode = RigidbodySleepMode2D.NeverSleep;
        
        // CRITICAL for player: freeze rotation so physics can't knock player sideways
        rb.constraints = RigidbodyConstraints2D.FreezeRotation;
    }
}
```

---

## 00D.6 — Common Inspector Settings by Object Type

| Object | Body Type | Mass | Linear Drag | Gravity Scale | Constraints |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Platform 2D Player | Dynamic | 1 | 0 | 2-3 | Freeze Rotation |
| Top-down Player | Dynamic | 1 | 3-5 | 0 | Freeze Rotation |
| Enemy | Dynamic | 2 | 0 | 1 | Freeze Rotation |
| Bullet | Dynamic | 0.01 | 0 | 0 | Freeze Rotation |
| Crate/Box | Dynamic | 5 | 0 | 1 | None |
| Moving Platform | Kinematic | — | — | — | Freeze All |
| Ground/Walls | Static (or no RB) | — | — | — | — |
| Bird/Flying enemy | Dynamic | 1 | 1 | 0 | Freeze Rotation |
| Bouncy Ball | Dynamic | 1 | 0 | 1 | None |

---

## 00D.7 — Reading Physics Correctly in Code

```csharp
public class Player2DPhysics : MonoBehaviour
{
    [SerializeField] private float moveSpeed = 8f;
    [SerializeField] private float jumpForce = 15f;
    [SerializeField] private float fallGravityScale = 3f;
    [SerializeField] private float groundCheckRadius = 0.15f;
    [SerializeField] private Transform groundCheck;
    [SerializeField] private LayerMask groundLayer;
    
    private Rigidbody2D rb;
    private bool isGrounded;
    private float horizontalInput;
    private bool jumpQueued;
    
    void Awake()
    {
        rb = GetComponent<Rigidbody2D>();
        rb.constraints = RigidbodyConstraints2D.FreezeRotation;
    }
    
    void Update()
    {
        // Read input in Update
        horizontalInput = Input.GetAxisRaw("Horizontal");
        
        if (Input.GetButtonDown("Jump") && isGrounded)
            jumpQueued = true;
        
        // Ground check — using a small circle at feet
        isGrounded = Physics2D.OverlapCircle(groundCheck.position, groundCheckRadius, groundLayer);
        
        // Adjust gravity for better feel
        rb.gravityScale = rb.linearVelocity.y < 0f ? fallGravityScale : 1f;
    }
    
    void FixedUpdate()
    {
        // Apply movement — preserve Y velocity (gravity!)
        rb.linearVelocity = new Vector2(horizontalInput * moveSpeed, rb.linearVelocity.y);
        
        // Apply jump
        if (jumpQueued)
        {
            rb.linearVelocity = new Vector2(rb.linearVelocity.x, 0f); // clear downward velocity first
            rb.AddForce(Vector2.up * jumpForce, ForceMode2D.Impulse);
            jumpQueued = false;
        }
    }
    
    void OnDrawGizmosSelected()
    {
        if (groundCheck != null)
        {
            Gizmos.color = isGrounded ? Color.green : Color.red;
            Gizmos.DrawWireSphere(groundCheck.position, groundCheckRadius);
        }
    }
}
```

---

## 📝 Summary — Rigidbody2D Inspector Reference

| Field | What It Does | Typical Value |
| :--- | :--- | :--- |
| **Body Type** | Dynamic=physics, Kinematic=manual, Static=immovable | Dynamic for player |
| **Simulated** | Turn physics on/off completely | ✓ always |
| **Mass** | Weight — affects force response and collisions | 1 (player), 5 (crate) |
| **Linear Drag** | Slows movement over time | 0 (platformer), 3-5 (top-down) |
| **Angular Drag** | Slows rotation over time | 0 (with freeze rotation) |
| **Gravity Scale** | How much gravity pulls this object | 2-3 (player), 0 (top-down) |
| **Collision Detection** | Discrete=normal, Continuous=fast objects | Continuous for bullets |
| **Sleeping Mode** | Never Sleep for player, Start Awake for rest | NeverSleep for player |
| **Freeze Rotation Z** | Prevents physics from spinning the object | ✓ ALWAYS for characters |

**Previous:** [[00C — Transform Deep Dive]] | **Next:** [[00E — Colliders & Triggers 2D]]
