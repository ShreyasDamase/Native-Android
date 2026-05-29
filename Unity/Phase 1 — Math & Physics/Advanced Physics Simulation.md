# ⚙️ Advanced Physics Simulation
### Angular Mechanics, Springs, Constraints, Verlet Integration & Collision Response Math

This guide covers the underlying mathematics that power realistic 2D game physics. Whether you are building a lunar lander, a ragdoll, or a complex rope system, these principles are what every simulation is built on.

---

## 1. Angular Mechanics (Rotation, Torque & Moment of Inertia)

### Core Concepts

Linear and angular physics are parallel systems. Every linear concept has an angular equivalent:

```
LINEAR                          ANGULAR
──────────────────────────────────────────────────────
Position    (Vector2)           Angle       (float, radians)
Velocity    (Vector2)           Angular Velocity (float, rad/s)
Force       (Vector2)           Torque      (float, N·m)
Mass        (float, kg)         Moment of Inertia (float, kg·m²)
F = m·a                         τ = I·α   (Torque = Inertia × AngularAccel)
```

### Moment of Inertia

The moment of inertia `I` is the rotational resistance of an object — how hard it is to spin.
For simple 2D shapes:

```
Rectangle:   I = (1/12) × m × (w² + h²)
Circle/Disk: I = (1/2)  × m × r²
Point Mass:  I = m × r²    (r = distance from pivot)
```

In Unity, `Rigidbody2D` automatically calculates `inertia` from the attached collider shape.

### Torque in Unity

```csharp
[RequireComponent(typeof(Rigidbody2D))]
public class RotatingThrusters : MonoBehaviour
{
    private Rigidbody2D rb;

    [SerializeField] private float thrustForce = 15f;
    [SerializeField] private float steerTorque = 8f;

    // How many radians per second the object is rotating
    public float AngularVelocityRadians => rb.angularVelocity * Mathf.Deg2Rad;

    private void Awake() => rb = GetComponent<Rigidbody2D>();

    private void FixedUpdate()
    {
        // Apply thrust along the ship's local up axis
        rb.AddForce(transform.up * thrustForce, ForceMode2D.Force);

        // Apply torque (positive = counter-clockwise in Unity 2D)
        float steerInput = Input.GetAxisRaw("Horizontal");
        rb.AddTorque(-steerInput * steerTorque);

        // Dampen spin to feel controllable (simulates attitude thrusters)
        rb.angularVelocity *= 0.98f;
    }
}
```

### Force Applied at an Offset Point (Torque from Off-Center Force)

When a force is not applied at the center of mass, it generates BOTH linear acceleration AND rotational torque. Unity handles this automatically via `AddForceAtPosition`.

```
            ┌─────────────────────┐
            │     Spaceship       │
            │        ●            │  ← Center of Mass
            │                     │
            └──────────────────○──┘
                               ↑
                         Engine Force
            (This creates BOTH forward velocity AND rotation)
```

```csharp
// Apply force at a specific world point — not the center of mass
// This automatically distributes to linear + angular acceleration
public void FireOffCenterThruster(Vector2 engineWorldPos, Vector2 thrustDir, float magnitude)
{
    rb.AddForceAtPosition(thrustDir.normalized * magnitude, engineWorldPos, ForceMode2D.Force);
}
```

---

## 2. Springs & Oscillation (Hooke's Law)

### Hooke's Law

A spring exerts a restoring force proportional to its displacement from equilibrium:
$$F_{spring} = -k \cdot x$$

Where:
- `k` = spring stiffness constant (N/m)
- `x` = displacement from rest length
- The negative sign means the force always points back toward equilibrium

### Critically Damped Spring (Professional Camera / UI Smoothing)

A damped spring avoids oscillation while still reaching the target rapidly. The formula requires a **damping coefficient** `c`:
$$F = -k \cdot x - c \cdot v$$

For **critical damping** (reaches target without overshoot), use: $c = 2\sqrt{k \cdot m}$

```csharp
// A spring-damper system that smoothly drives a value toward a target
// This is the same system used inside Unity's SmoothDamp internally
public class SpringDamper
{
    private float velocity;

    // stiffness: How fast to accelerate toward target (higher = stiffer)
    // damping: How quickly oscillations die out (1.0 = critically damped, no overshooting)
    public float Update(float current, float target, float stiffness, float damping, float dt)
    {
        float displacement = current - target;
        float springForce = -stiffness * displacement;
        float dampingForce = -damping * velocity;

        float acceleration = springForce + dampingForce; // Assumes mass = 1
        velocity += acceleration * dt;
        return current + velocity * dt;
    }
}
```

```csharp
// Usage: Springy camera that follows a target
public class SpringCamera : MonoBehaviour
{
    [SerializeField] private Transform target;
    [SerializeField] private float stiffness = 50f;
    [SerializeField] private float damping = 8f;    // ~2*sqrt(stiffness) for critical damping

    private SpringDamper springX = new SpringDamper();
    private SpringDamper springY = new SpringDamper();
    private Vector3 pos;

    private void LateUpdate()
    {
        pos = transform.position;
        pos.x = springX.Update(pos.x, target.position.x, stiffness, damping, Time.deltaTime);
        pos.y = springY.Update(pos.y, target.position.y, stiffness, damping, Time.deltaTime);
        transform.position = pos;
    }
}
```

### Rope / Chain Simulation (Distance Constraints)

A rope is modeled as a series of linked point masses. Each pair of adjacent points has a rest length. Each frame, we enforce the constraint by pushing points apart (or pulling them together) to maintain that length.

```csharp
[System.Serializable]
public struct RopePoint
{
    public Vector2 position;
    public Vector2 previousPosition; // Used for Verlet integration (see Section 4)
    public bool isPinned;            // If true, this point cannot move
}

// Solve the distance constraint between two rope points
public static void SolveDistanceConstraint(ref RopePoint a, ref RopePoint b, float restLength)
{
    Vector2 delta = b.position - a.position;
    float currentLength = delta.magnitude;

    if (currentLength < 0.0001f) return; // Avoid divide-by-zero

    float error = (currentLength - restLength) / currentLength;
    Vector2 correction = delta * 0.5f * error;

    if (!a.isPinned) a.position += correction;
    if (!b.isPinned) b.position -= correction;
}
```

---

## 3. Collision Response Mathematics

### Elastic vs. Inelastic Collisions

```
ELASTIC COLLISION:      Kinetic energy is fully conserved. (Perfect billiard balls)
INELASTIC COLLISION:    Some kinetic energy converts to heat/deformation. (Real objects)
PERFECTLY INELASTIC:    Objects stick together after collision.
```

### 1D Collision Response Formula

For two objects colliding on a 1D line, post-collision velocities are:
$$v_1' = \frac{(m_1 - e \cdot m_2)v_1 + (1 + e) m_2 v_2}{m_1 + m_2}$$
$$v_2' = \frac{(m_2 - e \cdot m_1)v_2 + (1 + e) m_1 v_1}{m_1 + m_2}$$

Where `e` is the **coefficient of restitution** (0 = perfectly inelastic, 1 = perfectly elastic).

### Implementing a Custom Collision Response

Unity's physics handles this automatically, but knowing the math lets you tune `PhysicsMaterial2D`:

```csharp
// Configure Physics Material programmatically for precise tuning
public class BounceController : MonoBehaviour
{
    [Range(0f, 1f)]
    [SerializeField] private float restitution = 0.6f; // Bounciness coefficient 'e'
    [SerializeField] private float friction = 0.3f;

    private void Start()
    {
        var col = GetComponent<Collider2D>();
        var mat = new PhysicsMaterial2D("Dynamic")
        {
            bounciness = restitution,
            friction = friction
        };
        col.sharedMaterial = mat;
    }

    // Listen to collisions for custom response events
    private void OnCollisionEnter2D(Collision2D col)
    {
        Vector2 impactVelocity = col.relativeVelocity;
        float impactSpeed = impactVelocity.magnitude;

        // Impact Normal: direction the surface pushes back
        Vector2 surfaceNormal = col.GetContact(0).normal;

        // Reflect incoming velocity across the surface normal (mirror bounce)
        Vector2 reflectedVelocity = Vector2.Reflect(-impactVelocity.normalized, surfaceNormal);

        Debug.Log($"Impact speed: {impactSpeed:F2} m/s | Bounce direction: {reflectedVelocity}");
    }
}
```

### Separating Axis Theorem (SAT) — Concept for Custom Colliders

SAT proves two convex shapes are NOT colliding if there exists any axis along which their projections do not overlap.

```
  ┌───────┐          ┌───────┐
  │   A   │          │   B   │
  └───────┘          └───────┘
                   ← Gap on X axis →
      A_proj: [0, 4]   B_proj: [6, 10]  → NO OVERLAP → NOT COLLIDING

  ┌───────┐
  │   A   ├───────────┤ B │
  └───────┘           └───┘
      A_proj: [0, 6]   B_proj: [4, 8]  → OVERLAP → COLLIDING
```

```csharp
// Simplified 2D SAT overlap test for two AABBs (Axis-Aligned Bounding Boxes)
public static bool AABBOverlap(Bounds a, Bounds b)
{
    // Check separation on X axis
    if (a.max.x < b.min.x || b.max.x < a.min.x) return false;
    // Check separation on Y axis
    if (a.max.y < b.min.y || b.max.y < a.min.y) return false;
    // No separating axis found — shapes ARE overlapping
    return true;
}

// Calculate Minimum Translation Vector (MTV) — how much to push objects apart
public static Vector2 CalculateMTV(Bounds a, Bounds b)
{
    float overlapX = Mathf.Min(a.max.x, b.max.x) - Mathf.Max(a.min.x, b.min.x);
    float overlapY = Mathf.Min(a.max.y, b.max.y) - Mathf.Max(a.min.y, b.min.y);

    // Return the smallest overlap axis as the push direction
    if (overlapX < overlapY)
        return new Vector2(a.center.x < b.center.x ? -overlapX : overlapX, 0);
    else
        return new Vector2(0, a.center.y < b.center.y ? -overlapY : overlapY);
}
```

---

## 4. Verlet Integration (Physics-Accurate Simulation)

Verlet integration is a numerical method for solving Newton's equations of motion. It is more stable than Euler integration and is used in cloth, rope, and soft-body simulations.

### Euler Integration (Standard — Less Stable)
$$\text{velocity} = \text{velocity} + \text{acceleration} \times dt$$
$$\text{position} = \text{position} + \text{velocity} \times dt$$

**Problem:** Velocity errors accumulate over time, causing energy gain or loss (drift).

### Verlet Integration (Physics-Stable)

Instead of storing velocity explicitly, it derives velocity from the difference between the **current and previous position**:
$$\text{position}_{new} = 2 \times \text{position}_{current} - \text{position}_{prev} + \text{acceleration} \times dt^2$$

```csharp
public class VerletParticle
{
    public Vector2 Position;
    public Vector2 PreviousPosition;
    public bool IsPinned;

    public VerletParticle(Vector2 startPos)
    {
        Position = startPos;
        PreviousPosition = startPos;
    }

    // Integrate acceleration and gravity — no explicit velocity needed
    public void Integrate(float dt, Vector2 gravity)
    {
        if (IsPinned) return;

        // Implicit velocity = Position - PreviousPosition
        Vector2 implicitVelocity = Position - PreviousPosition;

        // Save current for next frame
        PreviousPosition = Position;

        // Verlet formula — position + velocity + acceleration contribution
        Position += implicitVelocity + gravity * (dt * dt);
    }
}

// Full Verlet rope simulation loop
public class VerletRope : MonoBehaviour
{
    [SerializeField] private int segmentCount = 12;
    [SerializeField] private float segmentLength = 0.4f;
    [SerializeField] private int constraintIterations = 8; // More = stiffer rope

    private VerletParticle[] particles;
    private LineRenderer lineRenderer;

    private void Awake()
    {
        lineRenderer = GetComponent<LineRenderer>();
        particles = new VerletParticle[segmentCount];

        for (int i = 0; i < segmentCount; i++)
        {
            particles[i] = new VerletParticle(transform.position + Vector3.down * (i * segmentLength));
        }

        // Pin the top of the rope to the anchor
        particles[0].IsPinned = true;
    }

    private void FixedUpdate()
    {
        Vector2 gravity = new Vector2(0, Physics2D.gravity.y);

        // Step 1: Integrate all particles
        foreach (var p in particles)
            p.Integrate(Time.fixedDeltaTime, gravity);

        // Step 2: Solve distance constraints (run multiple iterations for stability)
        for (int iter = 0; iter < constraintIterations; iter++)
        {
            for (int i = 0; i < particles.Length - 1; i++)
            {
                SolveDistanceConstraint(particles[i], particles[i + 1], segmentLength);
            }
        }

        // Step 3: Update visual renderer
        lineRenderer.positionCount = particles.Length;
        for (int i = 0; i < particles.Length; i++)
            lineRenderer.SetPosition(i, particles[i].Position);
    }

    private void SolveDistanceConstraint(VerletParticle a, VerletParticle b, float restLen)
    {
        Vector2 delta = b.Position - a.Position;
        float len = delta.magnitude;
        if (len < 0.0001f) return;

        float error = (len - restLen) / len;
        Vector2 correction = delta * 0.5f * error;

        if (!a.IsPinned) a.Position += correction;
        if (!b.IsPinned) b.Position -= correction;
    }
}
```

---

## 5. Buoyancy & Fluid Dynamics (Archimedes' Principle)

$$F_{buoyancy} = \rho_{fluid} \times V_{submerged} \times g$$

Where:
- `ρ` = fluid density (water ≈ 1000 kg/m³, but scale for your game world)
- `V` = volume of submerged portion
- `g` = gravitational acceleration magnitude

```csharp
// 2D Buoyancy: calculates submerged area and applies upward lift force
public class BuoyancyObject : MonoBehaviour
{
    [SerializeField] private float fluidDensity = 1.5f;  // Tunable: 1.0 = neutral buoyancy
    [SerializeField] private float dragInFluid = 2.5f;   // Horizontal water resistance
    [SerializeField] private float angularDragInFluid = 1.5f;

    private Rigidbody2D rb;
    private Collider2D col;

    // Set this from a water trigger zone script
    [HideInInspector] public bool isSubmerged;
    [HideInInspector] public float waterSurfaceY; // World Y of the water surface

    private void Awake()
    {
        rb = GetComponent<Rigidbody2D>();
        col = GetComponent<Collider2D>();
    }

    private void FixedUpdate()
    {
        if (!isSubmerged) return;

        // Calculate how deep the object's center is below water surface
        float submergedDepth = Mathf.Clamp(waterSurfaceY - col.bounds.min.y, 0, col.bounds.size.y);
        float submergedFraction = submergedDepth / col.bounds.size.y;

        // Approximate submerged volume using bounding box (2D = area approximation)
        float submergedArea = col.bounds.size.x * submergedDepth;

        // Archimedes upthrust force
        float buoyancyForce = fluidDensity * Mathf.Abs(Physics2D.gravity.y) * submergedArea;
        rb.AddForce(Vector2.up * buoyancyForce);

        // Apply water drag forces to dampen motion
        rb.linearVelocity *= (1f - dragInFluid * submergedFraction * Time.fixedDeltaTime);
        rb.angularVelocity *= (1f - angularDragInFluid * submergedFraction * Time.fixedDeltaTime);
    }
}
```

---

## 6. Projectile Motion (Ballistics Calculations)

### Key Equations

For a projectile launched with initial velocity `v₀` at angle `θ` from horizontal:

```
x(t) = v₀ · cos(θ) · t
y(t) = v₀ · sin(θ) · t  -  ½ · g · t²

Time of flight:    t = (2 · v₀ · sin(θ)) / g
Maximum range:     R = (v₀² · sin(2θ)) / g
Maximum height:    H = (v₀ · sin(θ))² / (2 · g)
Optimal angle:     θ = 45° (maximizes range on flat ground)
```

### Launch Angle Calculator (Hit a specific target)

```csharp
// Calculate the angle(s) needed to hit a target at (dx, dy) with speed v
// Returns true if the target is reachable, and fills low/highAngle in radians
public static bool CalculateLaunchAngles(float dx, float dy, float speed, float gravity,
                                          out float lowAngle, out float highAngle)
{
    lowAngle = highAngle = 0f;
    float v2 = speed * speed;
    float g = gravity;
    float discriminant = v2 * v2 - g * (g * dx * dx + 2 * dy * v2);

    if (discriminant < 0) return false; // Out of range — no solution exists

    float sqrtDisc = Mathf.Sqrt(discriminant);
    // Two solutions: low trajectory (fast, flat) and high trajectory (lobbed arc)
    lowAngle  = Mathf.Atan2(v2 - sqrtDisc, g * dx);
    highAngle = Mathf.Atan2(v2 + sqrtDisc, g * dx);
    return true;
}

// Predict where a projectile will be at time t (for AI aiming at moving targets)
public static Vector2 PredictPosition(Vector2 startPos, Vector2 initialVelocity, float t)
{
    float grav = Physics2D.gravity.y;
    return new Vector2(
        startPos.x + initialVelocity.x * t,
        startPos.y + initialVelocity.y * t + 0.5f * grav * t * t
    );
}
```

---

## 7. Gravitational Attraction & Orbital Mechanics

### Newton's Law of Universal Gravitation
$$F = G \cdot \frac{m_1 \cdot m_2}{r^2}$$

```csharp
// Attract any Rigidbody2D toward a gravitational body (planet, black hole, etc.)
public class GravityAttractor : MonoBehaviour
{
    [SerializeField] private float gravitationalConstant = 50f; // Scaled for game world
    [SerializeField] private float influenceRadius = 20f;

    private void FixedUpdate()
    {
        // Attract all dynamic rigidbodies in range
        Collider2D[] nearbyObjects = Physics2D.OverlapCircleAll(transform.position, influenceRadius);

        foreach (var col in nearbyObjects)
        {
            if (!col.attachedRigidbody || col.attachedRigidbody.bodyType != RigidbodyType2D.Dynamic)
                continue;

            Rigidbody2D target = col.attachedRigidbody;
            Vector2 direction = (Vector2)transform.position - target.position;
            float distanceSq = direction.sqrMagnitude;

            if (distanceSq < 0.01f) continue; // Avoid singularity at center

            // F = G * m1 * m2 / r² — simplified: F = G * targetMass / r²
            float forceMagnitude = gravitationalConstant * target.mass / distanceSq;
            target.AddForce(direction.normalized * forceMagnitude, ForceMode2D.Force);
        }
    }
}
```

---

## 8. Key Physics Constants Reference

```
┌──────────────────────────────────────────────────────────────────────────┐
│                    UNITY PHYSICS 2D QUICK REFERENCE                      │
├───────────────────────────┬──────────────────────────────────────────────┤
│ Physics2D.gravity         │ Default: (0, -9.81f) — world gravity vector  │
│ ForceMode2D.Force         │ Continuous force (per fixed update)           │
│ ForceMode2D.Impulse       │ Instant velocity change (one-time)            │
│ rb.linearVelocity         │ Current velocity vector (Unity 6+)            │
│ rb.angularVelocity        │ Current spin speed in degrees/second          │
│ rb.mass                   │ Object mass in kg (affects force response)    │
│ rb.gravityScale           │ Multiplier for gravity (0 = zero-G)           │
│ rb.drag                   │ Linear air resistance coefficient             │
│ rb.angularDrag            │ Rotational friction coefficient                │
│ rb.inertia                │ Rotational mass (auto-calculated by collider) │
│ rb.centerOfMass           │ Local center-of-mass offset                   │
│ rb.AddForceAtPosition()   │ Force + torque from off-center application    │
│ rb.AddTorque()            │ Pure rotation force                           │
│ rb.MovePosition()         │ Kinematic teleport with collision resolution  │
│ rb.MoveRotation()         │ Kinematic rotate with collision resolution    │
│ rb.Sleep() / WakeUp()     │ Manual physics sleep control                  │
│ Time.fixedDeltaTime       │ Fixed physics timestep (default 0.02s)        │
└───────────────────────────┴──────────────────────────────────────────────┘
```
