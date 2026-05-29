# ⚙️ 2D Physics
### Rigidbody2D, Forces, Collisions & Triggers

Unity's 2D physics engine (Box2D under the hood) runs simulation algorithms for gravity, drag, velocity, and collisions. Understanding how to script, configure, and optimize physics queries is essential for fluid gameplay.

---

## 1. Rigidbody2D Body Types

Every physics-controlled GameObject requires a `Rigidbody2D` component. Choose the correct body type to save CPU cycles and prevent collision bugs:

| Body Type | Description | Movement Method | Collides With | Trigger Detection With |
| :--- | :--- | :--- | :--- | :--- |
| **Dynamic** | Full physics simulation (affected by gravity, forces, torque, and mass). | `AddForce` / `linearVelocity` | Dynamic, Kinematic, Static | Dynamic, Kinematic, Static |
| **Kinematic**| Ignores forces and gravity. Moved explicitly via code/scripts. | `MovePosition` / `linearVelocity` | Dynamic *only* (unless configured in settings) | Dynamic, Kinematic, Static |
| **Static** | Rigid, immovable walls or terrain. Zero memory movement overhead. | Cannot be moved. | Dynamic | Dynamic, Kinematic |

> [!CAUTION]
> **Static Movement Warning:** Never move a GameObject marked as **Static** at runtime. Doing so forces the physics engine to rebuild its internal spatial partitions (AABB trees), which causes significant CPU lag. If a static-looking wall needs to move (like a sliding door), configure it as **Kinematic** instead.

---

## 2. Collision Compatibility Matrix
How colliders interact based on their Rigidbody configurations:

```
                  ┌───────────────────────────────┐
                  │          COLLIDER B           │
                  │   Dynamic  Kinematic  Static  │
   ┌──────────────┼───────────────────────────────┤
   │   Dynamic    │     Yes        Yes      Yes   │
   │  Kinematic   │     Yes         No       No   │
   │    Static    │     Yes         No       No   │
   └──────────────┴───────────────────────────────┘
   (Note: Kinematic-Kinematic collisions can be enabled in Project Settings if needed)
```

---

## 3. Force vs. Velocity Scripting

### A. Applying Forces (Realistic Physics)
Forces act gradually, accelerating objects over time based on their mass ($F = ma$).
```csharp
// 1. Continuous Force (e.g. constant engine thrust, wind pushing)
rb.AddForce(transform.up * thrustForce, ForceMode2D.Force);

// 2. Instant Force (e.g. jumping, explosions, firing a projectile)
rb.AddForce(Vector2.up * jumpForce, ForceMode2D.Impulse);
```

### B. Velocity Modification (Arcade Precision)
Overriding velocity directly bypasses the force acceleration step. It is preferred for precise, responsive controls (e.g., character movement).
*   *Note for Unity 6+:* `Rigidbody2D.velocity` has been deprecated in favor of `Rigidbody2D.linearVelocity`.
```csharp
// Direct horizontal movement override while preserving current gravity fall speed
rb.linearVelocity = new Vector2(horizontalInput * moveSpeed, rb.linearVelocity.y);
```

---

## 4. Collision vs. Trigger Detection

Colliders define physical boundaries. The **Is Trigger** checkbox determines if the boundary behaves as a solid surface or a ghost sensor zone:

```
Is Trigger = FALSE (Physical Collision)     Is Trigger = TRUE (Sensor Overlap)
        ┌──────┐  Solid hit                       ┌──────┐  Ghost overlap
        │      │ ◄────────── Rigid                │      │ ◄────────── Ghost
        └──────┘                                  └──────┘
      Physical bounce                          Triggers code event
      No penetration                           Objects pass through
```

### Event Script Callbacks
Both modes support three timing states: `Enter` (first touch), `Stay` (ongoing overlap), and `Exit` (separated).

```csharp
// SOLID COLLISIONS
private void OnCollisionEnter2D(Collision2D collision)
{
    // collision contains detailed context: contact points, impact velocity, relative mass
    Vector2 impactNormal = collision.GetContact(0).normal;
    float relativeVelocity = collision.relativeVelocity.magnitude;
    
    if (collision.gameObject.CompareTag("Obstacle") && relativeVelocity > 5f)
    {
        TakeDamage(relativeVelocity);
    }
}

// SENSOR TRIGGERS
private void OnTriggerEnter2D(Collider2D other)
{
    // other only contains the overlapping collider component; no contact vectors
    if (other.CompareTag("Collectible"))
    {
        CollectItem(other.gameObject);
    }
}
```

---

## 5. The FixedUpdate execution Rule

Unity evaluates updates at variable intervals (dependent on graphic workload), whereas physics loops run at a rigid, fixed timestep (default: 0.02s / 50 Hz). 
*   **The Rule:** Read player input in `Update()` (so you never miss input frames). Process physics modifications (applying force/altering velocity) in `FixedUpdate()`.

```csharp
private float horizontalInput;
private bool jumpRequested;

void Update()
{
    // 1. Read input every frame
    horizontalInput = Input.GetAxisRaw("Horizontal");
    if (Input.GetButtonDown("Jump"))
    {
        jumpRequested = true; 
    }
}

void FixedUpdate()
{
    // 2. Execute physics operations
    rb.linearVelocity = new Vector2(horizontalInput * speed, rb.linearVelocity.y);

    if (jumpRequested)
    {
        rb.AddForce(Vector2.up * jumpForce, ForceMode2D.Impulse);
        jumpRequested = false; // Reset input flag
    }
}
```

---

## 6. Physics Stability & Optimizations

### A. Tunnelling & Collision Detection Modes
When objects move very fast, they can "tunnel" through walls because they are at position X on frame 1, and position Y (already past the wall) on frame 2.
*   **Discrete (Default):** Checks intersections at fixed intervals. Fast, but fast-moving objects can pass through thin colliders.
*   **Continuous:** Traces a swept volume between frames to ensure collisions are detected. Use this for high-speed targets (like player characters, bullets, or landing ships).

### B. Rigidbody Jitter & Interpolation
Physics updates run at a different frequency than visual frame renders, which can cause objects to appear to shake or micro-stutter when the camera follows them.
*   **Interpolate:** Predicts the object's position based on its past frame velocities. **Highly recommended** for player-controlled characters.
*   **Extrapolate:** Predicts position based on future expected velocities. Use with caution.

---

## 7. Physics Queries (Raycasts, Casts & Overlaps)

Instead of using colliders as sensors, you can cast mathematical queries through space:

```csharp
[SerializeField] private LayerMask groundLayer;
private Collider2D[] queryResults = new Collider2D[5];

bool IsGrounded()
{
    // Cast a ray downward to check for ground
    Vector2 origin = transform.position;
    Vector2 direction = Vector2.down;
    float distance = 1.1f;

    RaycastHit2D hit = Physics2D.Raycast(origin, direction, distance, groundLayer);
    
    // Debug draw visual representation in editor
    Debug.DrawRay(origin, direction * distance, hit.collider != null ? Color.green : Color.red);
    
    return hit.collider != null;
}

// Optimized NonAlloc Query (prevents Garbage Collection allocations)
void DetectEnemiesAround()
{
    Vector2 center = transform.position;
    float searchRadius = 3f;
    int enemyLayerMask = LayerMask.GetMask("Enemies");

    int numFound = Physics2D.OverlapCircleNonAlloc(center, searchRadius, queryResults, enemyLayerMask);
    for (int i = 0; i < numFound; i++)
    {
        // Process queryResults[i]
        Debug.Log($"Enemy detected: {queryResults[i].name}");
    }
}
```

---

## 8. 2D Effectors

Effectors add complex behavioral physics to standard colliders without requiring custom code:

*   **PlatformEffector2D:** Creates one-way platforms (player can jump through the bottom and land on the top) and side friction overrides.
*   **AreaEffector2D:** Applies directional force within a volume (used to create wind zones, water currents, or magnetic fields).
*   **SurfaceEffector2D:** Applies tangential force along collider edges (ideal for creating conveyor belts or speed boost pads).
*   **BuoyancyEffector2D:** Simulates liquid density and surface waves, allowing objects to float or sink based on their mass.
