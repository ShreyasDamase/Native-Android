# 📐 Complete Physics & Math Formula Reference
### Every Formula a Game Developer Needs — With Full Explanation & Unity Use Cases

This is the master formula reference. Every formula is explained in three parts:
1. **What it means** — the concept in plain English
2. **The math** — the actual formula
3. **Game use case** — exactly when and how you use it in Unity

---

## PART 1: NEWTON'S LAWS & KINEMATICS

---

### 1.1 Newton's Second Law — The Engine of All Physics
$$\boxed{F = m \cdot a}$$

**What it means:** Force equals mass times acceleration. If you push a heavier object with the same force, it accelerates less. If you apply a bigger force to the same object, it accelerates more.

**Why this matters in games:**
- `rb.AddForce(F)` applies this formula internally: Unity divides the force by `rb.mass` to get acceleration
- A bullet has tiny mass → same force = huge acceleration
- A ship has large mass → same engine force = slow acceleration

```csharp
// Equivalent of F = m*a — Unity does this automatically inside the physics engine
// But you can verify it:
float acceleration = force / rb.mass;        // a = F/m
float forceNeeded  = rb.mass * targetAccel;  // F = m*a

// Custom force needed to reach a target velocity in one fixedUpdate step:
float impulseNeeded = rb.mass * (targetVelocity - rb.linearVelocity.magnitude);
rb.AddForce(transform.up * impulseNeeded, ForceMode2D.Impulse);
```

**Game use case:** Tuning thruster forces on your lander. If the ship feels sluggish, either reduce `rb.mass` or increase your `thrustForce`. F = m·a tells you exactly the ratio to hit.

---

### 1.2 Kinematic Equations — Motion Without Forces

These 4 equations describe motion when acceleration is **constant** (like a falling object or a bullet in zero air resistance).

```
Variables:
  s  = displacement (distance traveled)
  u  = initial velocity
  v  = final velocity
  a  = constant acceleration
  t  = time elapsed

The 4 Kinematic Equations:
  v  = u + a·t                      [1] Find final velocity
  s  = u·t + ½·a·t²                 [2] Find displacement from time
  v² = u² + 2·a·s                   [3] Find velocity from distance (no time needed)
  s  = ½·(u + v)·t                  [4] Find displacement from average velocity
```

**What they mean:**
- Equation [1]: "How fast will it be going?" — velocity grows linearly with time under constant acceleration
- Equation [2]: "Where will it be?" — the parabolic arc of any thrown/falling object
- Equation [3]: **The most useful** — "How fast will the bullet be going when it travels X meters?" — no time variable needed
- Equation [4]: Average of start/end velocity × time = total distance

```csharp
// [1] Final velocity of a falling object after 3 seconds:
float g = Mathf.Abs(Physics2D.gravity.y); // 9.81
float v = 0 + g * 3f;   // v = 29.43 m/s downward

// [2] How far does it fall in 3 seconds (free fall from rest):
float s = 0 * 3f + 0.5f * g * (3f * 3f);  // s = 44.1 meters

// [3] How fast is the bullet going after traveling 50m with a = -2 (air resistance)?
// v² = u² + 2·a·s  →  v = sqrt(u² + 2·a·s)
float bulletInitialSpeed = 100f;
float airResistance = -2f; // deceleration
float distanceTraveled = 50f;
float finalSpeed = Mathf.Sqrt(bulletInitialSpeed * bulletInitialSpeed + 2f * airResistance * distanceTraveled);
// finalSpeed = sqrt(10000 - 200) = sqrt(9800) ≈ 98.99 m/s

// [3] Minimum launch speed needed to reach a height H from rest:
// 0 = u² - 2·g·H  →  u = sqrt(2·g·H)
float heightToReach = 5f;
float minLaunchSpeed = Mathf.Sqrt(2f * g * heightToReach);  // ≈ 9.9 m/s
```

**Game use case:**
- **Jump design**: Use [3] to calculate exactly how fast to launch a character to reach a desired jump height: `v = sqrt(2·g·H)`
- **Projectile travel**: Use [2] for trajectory preview arc without simulating every step
- **Stopping distance**: Use [3] to calculate how much braking distance a vehicle needs: `s = v²/(2·a)`

---

### 1.3 Impulse-Momentum Theorem
$$\boxed{J = \Delta p = m \cdot \Delta v = F \cdot \Delta t}$$

**What it means:** An impulse (J) is a force applied over a very short time. It directly changes momentum (mass × velocity). `ForceMode2D.Impulse` in Unity applies this.

**Why the distinction matters:**
- `ForceMode2D.Force` → `F = m·a` → applied each FixedUpdate (continuous)
- `ForceMode2D.Impulse` → `J = m·Δv` → immediate velocity change (one-shot)

```csharp
// How much impulse needed to launch a 2kg object upward at 8 m/s from rest?
// J = m * deltaV = 2 * (8 - 0) = 16 N·s
float mass = rb.mass;          // e.g. 2 kg
float targetDeltaV = 8f;       // desired velocity change
float impulseNeeded = mass * targetDeltaV;
rb.AddForce(Vector2.up * impulseNeeded, ForceMode2D.Impulse);

// Explosion knockback: apply impulse scaled by inverse distance (closer = stronger)
void ApplyExplosionKnockback(Vector2 center, float radius, float maxImpulse)
{
    var colliders = Physics2D.OverlapCircleAll(center, radius);
    foreach (var col in colliders)
    {
        if (!col.attachedRigidbody) continue;
        float distance = Vector2.Distance(center, col.transform.position);
        float falloff = 1f - (distance / radius); // Linear falloff: 1 at center, 0 at edge
        Vector2 direction = ((Vector2)col.transform.position - center).normalized;
        col.attachedRigidbody.AddForce(direction * maxImpulse * falloff, ForceMode2D.Impulse);
    }
}
```

**Game use case:** Jump, punch knockback, rocket thrust bursts, explosion radial force, bullet deflection.

---

### 1.4 Work & Energy
$$\boxed{W = F \cdot d \cdot \cos\theta}$$
$$\boxed{KE = \frac{1}{2} m v^2}$$
$$\boxed{PE = m \cdot g \cdot h}$$
$$\boxed{E_{total} = KE + PE = \text{constant (no friction)}}$$

**What they mean:**
- Work = force × distance in the direction of motion
- Kinetic Energy = the energy stored in motion
- Potential Energy = the energy stored in height (gravitational)
- Conservation of Energy: without friction/drag, KE + PE stays constant → a ball falling converts PE to KE

**Game use case — checking physics integrity:**

```csharp
// Verify your physics simulation conserves energy (useful for debugging)
float GetTotalEnergy(Rigidbody2D rb)
{
    float ke = 0.5f * rb.mass * rb.linearVelocity.sqrMagnitude;  // ½mv²
    float pe = rb.mass * Mathf.Abs(Physics2D.gravity.y) * rb.position.y; // mgh
    return ke + pe;
}

// Use kinematic formula for precise jump height from launch speed:
// At peak: KE = 0, so all initial KE converts to PE
// ½mv² = mgh  →  h = v²/(2g)
float JumpHeightFromSpeed(float launchSpeed)
{
    return (launchSpeed * launchSpeed) / (2f * Mathf.Abs(Physics2D.gravity.y));
}

// And reverse: what speed do I need for a given jump height?
float SpeedForJumpHeight(float height)
{
    return Mathf.Sqrt(2f * Mathf.Abs(Physics2D.gravity.y) * height);
}
```

---

## PART 2: FRICTION, DRAG & RESISTANCE

---

### 2.1 Friction Force (Static vs Kinetic)
$$\boxed{f_s \leq \mu_s \cdot N}$$    (Static — before sliding starts)
$$\boxed{f_k = \mu_k \cdot N}$$       (Kinetic — while sliding)

**What it means:**
- **N** = Normal force (force perpendicular to the surface = `mass × g` on flat ground)
- **μs** = Coefficient of static friction (how hard to START sliding)
- **μk** = Coefficient of kinetic friction (resistance WHILE sliding)
- Static friction is always ≥ kinetic friction — objects resist starting to slide more than they resist continuing to slide
- When applied force < `μs × N` → object stays still. When it exceeds it → it starts sliding.

```
Friction Coefficients (approximate values):
─────────────────────────────────────────
Ice on ice:          μs ≈ 0.1,  μk ≈ 0.03
Wood on wood:        μs ≈ 0.4,  μk ≈ 0.2
Rubber on concrete:  μs ≈ 0.8,  μk ≈ 0.6
Metal on metal:      μs ≈ 0.6,  μk ≈ 0.4
─────────────────────────────────────────
```

```csharp
// Manual friction simulation (Unity handles automatically via PhysicsMaterial2D)
// Useful when you want CUSTOM friction logic (e.g., ice platform changes friction at runtime)
public class CustomFriction : MonoBehaviour
{
    [SerializeField] private float staticFrictionCoeff  = 0.5f;  // μs
    [SerializeField] private float kineticFrictionCoeff = 0.3f;  // μk
    private Rigidbody2D rb;

    private void FixedUpdate()
    {
        if (!IsGrounded()) return;

        float normalForce = rb.mass * Mathf.Abs(Physics2D.gravity.y); // N = mg on flat ground
        float currentSpeed = rb.linearVelocity.magnitude;

        if (currentSpeed < 0.01f)
        {
            // Apply static friction — prevent movement unless force overcomes μs × N
            rb.linearVelocity = Vector2.zero;
        }
        else
        {
            // Apply kinetic friction — opposes direction of motion
            float frictionMagnitude = kineticFrictionCoeff * normalForce;
            Vector2 frictionForce = -rb.linearVelocity.normalized * frictionMagnitude;
            rb.AddForce(frictionForce, ForceMode2D.Force);
        }
    }
}
```

**Game use case:** Ice levels (near-zero μk), sticky platforms, climbing mechanics (high μs), landing deceleration, sliding puzzles.

---

### 2.2 Air Drag / Fluid Resistance
$$\boxed{F_{drag} = \frac{1}{2} \cdot \rho \cdot v^2 \cdot C_d \cdot A}$$

**What it means:**
- `ρ` = fluid density (air ≈ 1.225 kg/m³)
- `v²` = velocity squared — drag grows with the **square** of speed (double speed = 4× drag)
- `Cd` = drag coefficient (how aerodynamic the shape is — 0.05 for a streamlined rocket, 1.2 for a flat plate)
- `A` = cross-sectional area facing the flow

**Why v² matters:** At low speeds, drag is negligible. At high speeds, drag dominates. This creates **terminal velocity**.

**Terminal velocity** — when drag equals gravity:
$$v_{terminal} = \sqrt{\frac{2mg}{\rho C_d A}}$$

```csharp
// Realistic quadratic air drag simulation
public class AerodynamicDrag : MonoBehaviour
{
    [SerializeField] private float dragCoefficient = 0.47f;   // Cd: sphere ≈ 0.47
    [SerializeField] private float crossSectionArea = 0.1f;   // m² facing airflow
    [SerializeField] private float airDensity = 1.225f;       // kg/m³ (sea level air)

    private Rigidbody2D rb;

    private void Awake() => rb = GetComponent<Rigidbody2D>();

    private void FixedUpdate()
    {
        float speed = rb.linearVelocity.magnitude;
        if (speed < 0.001f) return;

        // F_drag = ½ * ρ * v² * Cd * A
        float dragMagnitude = 0.5f * airDensity * (speed * speed) * dragCoefficient * crossSectionArea;

        // Drag opposes direction of motion
        Vector2 dragForce = -rb.linearVelocity.normalized * dragMagnitude;
        rb.AddForce(dragForce, ForceMode2D.Force);
    }

    // Calculate terminal velocity for this object
    public float GetTerminalVelocity()
    {
        float weight = rb.mass * Mathf.Abs(Physics2D.gravity.y);
        return Mathf.Sqrt((2f * weight) / (airDensity * dragCoefficient * crossSectionArea));
    }
}
```

> [!NOTE]
> Unity's built-in `rb.drag` uses **linear drag** (F_drag = -drag × v), which is simpler but less realistic. For accurate projectile flight arcs, implement quadratic drag manually.

**Game use case:** Cannonball trajectories with realistic falloff, rocket vs bullet behavior difference, feather vs stone falling speed, wind resistance on vehicles.

---

### 2.3 Rolling Resistance
$$\boxed{F_{roll} = C_{rr} \cdot N}$$

**What it means:** Rolling resistance is much weaker than sliding friction. `Crr` (rolling resistance coefficient) is typically 0.001–0.01 for wheels, vs 0.3–0.8 for kinetic friction.

```csharp
// Apply rolling resistance to a wheel-based vehicle
void ApplyRollingResistance(Rigidbody2D rb, float rollingResistanceCoeff = 0.02f)
{
    float normalForce = rb.mass * Mathf.Abs(Physics2D.gravity.y);
    float rollForce = rollingResistanceCoeff * normalForce;

    // Oppose the direction of travel
    if (rb.linearVelocity.magnitude > 0.01f)
        rb.AddForce(-rb.linearVelocity.normalized * rollForce, ForceMode2D.Force);
}
```

---

## PART 3: ROTATIONAL DYNAMICS

---

### 3.1 Angular Kinematic Equations

Exactly parallel to linear kinematic equations but for rotation:

```
Variables:
  θ  = angular displacement (radians)
  ω₀ = initial angular velocity (rad/s)
  ω  = final angular velocity (rad/s)
  α  = angular acceleration (rad/s²)
  t  = time

Angular Kinematic Equations:
  ω  = ω₀ + α·t
  θ  = ω₀·t + ½·α·t²
  ω² = ω₀² + 2·α·θ
  θ  = ½·(ω₀ + ω)·t
```

```csharp
// How long to spin up a wheel from rest to 120 RPM with angular acceleration 10 rad/s²?
float targetRPM = 120f;
float targetOmega = targetRPM * 2f * Mathf.PI / 60f;  // Convert RPM → rad/s = 4π rad/s ≈ 12.57 rad/s
float alpha = 10f;   // Angular acceleration (rad/s²)
float time = targetOmega / alpha;  // ω = ω₀ + α·t → t = (ω - 0) / α = 1.257 seconds

// Unity: rb.angularVelocity is in DEGREES/second, not radians
// Convert: omega_deg = omega_rad * Mathf.Rad2Deg
float currentOmegaDeg = rb.angularVelocity;  // degrees/second
float currentOmegaRad = currentOmegaDeg * Mathf.Deg2Rad;
```

---

### 3.2 Centripetal Force & Acceleration (Circular Motion)
$$\boxed{F_c = \frac{m \cdot v^2}{r}}$$
$$\boxed{a_c = \frac{v^2}{r} = \omega^2 \cdot r}$$

**What it means:** To keep an object moving in a circle, you need a continuous inward force (centripetal force). The faster it goes or the tighter the circle, the larger this force must be.

**This is NOT a new type of force** — it's just whatever force is causing the circular path:
- Earth orbiting the Sun → gravity provides `Fc`
- Car going around a corner → friction provides `Fc`
- Ball on a string → tension provides `Fc`

```csharp
// How fast can a vehicle take a corner of radius r before sliding out?
// At the limit: Friction force = Centripetal force
// μ·m·g = m·v²/r  →  v_max = sqrt(μ·g·r)
float MaxCornerSpeed(float radius, float frictionCoeff)
{
    float g = Mathf.Abs(Physics2D.gravity.y);
    return Mathf.Sqrt(frictionCoeff * g * radius);
}

// Planet orbital velocity: At stable orbit, gravity = centripetal force
// G·M/r² = v²/r  →  v = sqrt(G·M/r)
float OrbitalSpeed(float planetMass, float orbitRadius, float G = 50f)
{
    return Mathf.Sqrt(G * planetMass / orbitRadius);
}

// Keep an object in circular orbit around a pivot (no joint needed)
public class CircularOrbit : MonoBehaviour
{
    [SerializeField] private Transform center;
    [SerializeField] private float orbitSpeed = 3f; // m/s tangential speed

    private Rigidbody2D rb;

    private void Awake() => rb = GetComponent<Rigidbody2D>();

    private void FixedUpdate()
    {
        Vector2 toCenter = (Vector2)center.position - rb.position;
        float radius = toCenter.magnitude;
        if (radius < 0.001f) return;

        // F_centripetal = m * v² / r, directed toward center
        float centripetalForceMag = rb.mass * (orbitSpeed * orbitSpeed) / radius;
        rb.AddForce(toCenter.normalized * centripetalForceMag, ForceMode2D.Force);
    }
}
```

**Game use case:** Planet orbits, spinning platforms, ball-on-rope physics, tire friction limits for racing games, calculating minimum curve radius for paths.

---

### 3.3 Angular Momentum & Conservation
$$\boxed{L = I \cdot \omega}$$
$$\boxed{L_{before} = L_{after} \text{ (if no external torque)}}$$

**What it means:** Angular momentum is conserved when no external torque acts. If a spinning object changes its moment of inertia (e.g., a figure skater pulling arms in), its angular velocity must change to compensate.

$$I_1 \omega_1 = I_2 \omega_2$$

```csharp
// Simulate a figure skater spinning up by "pulling in mass"
// When radius decreases, angular velocity must increase to conserve momentum
public class SpinAccelerator : MonoBehaviour
{
    private Rigidbody2D rb;

    [SerializeField] private float extendedRadius = 1.5f;   // Arms out
    [SerializeField] private float contractedRadius = 0.5f; // Arms in

    private void Awake() => rb = GetComponent<Rigidbody2D>();

    public void ContractArms()
    {
        // I1 * ω1 = I2 * ω2  →  ω2 = ω1 * (I1 / I2) = ω1 * (r1² / r2²)
        // (Using point-mass approximation: I = m*r²)
        float currentOmega = rb.angularVelocity;
        float r1sq = extendedRadius * extendedRadius;
        float r2sq = contractedRadius * contractedRadius;
        rb.angularVelocity = currentOmega * (r1sq / r2sq);
    }
}
```

---

## PART 4: WAVES, OSCILLATION & RESONANCE

---

### 4.1 Simple Harmonic Motion (SHM)
$$\boxed{x(t) = A \cdot \cos(\omega t + \phi)}$$
$$\boxed{\omega = \sqrt{\frac{k}{m}}}$$    (Spring-mass system)
$$\boxed{\omega = \sqrt{\frac{g}{L}}}$$    (Pendulum)
$$\boxed{T = \frac{2\pi}{\omega}}$$        (Period — time for one full oscillation)

**What it means:**
- Any spring-mass system oscillates with frequency `ω` (omega, in rad/s)
- `A` = amplitude (maximum displacement from center)
- `φ` = phase offset (initial position in the cycle)
- The period T tells you how many seconds one full bounce takes

```csharp
// Design a spring platform that bounces the player with a specific frequency
// Target: platform oscillates with period T = 1.5 seconds
// ω = 2π/T  →  ω = 4.19 rad/s
// k = ω² * m (for mass m = 1 as reference)
float targetPeriod = 1.5f;
float omega = 2f * Mathf.PI / targetPeriod;  // Angular frequency
float springConstant = omega * omega;          // k = ω² (for m=1)

// Animate a UI element or floating object with SHM (no physics needed)
public class SHMFloat : MonoBehaviour
{
    [SerializeField] private float amplitude = 0.3f;   // A: how far up/down
    [SerializeField] private float frequency = 1.2f;   // Hz: cycles per second
    [SerializeField] private float phaseOffset = 0f;   // φ: start position in cycle

    private Vector3 startPosition;

    private void Start() => startPosition = transform.localPosition;

    private void Update()
    {
        float omega = 2f * Mathf.PI * frequency;
        float displacement = amplitude * Mathf.Cos(omega * Time.time + phaseOffset);
        transform.localPosition = startPosition + Vector3.up * displacement;
    }
}
```

**Game use case:** Bobbing pickups/coins, pendulum traps, oscillating platforms, breathing animation on idle characters, screen breathing effect.

---

### 4.2 Pendulum Period Formula
$$\boxed{T = 2\pi\sqrt{\frac{L}{g}}}$$

**What it means:** A pendulum of length L swings with period T regardless of how heavy the bob is (for small angles). Double the length → period increases by √2 ≈ 1.41×.

```csharp
// Calculate how long a pendulum swing takes based on rope length
// Use this to sync your animation timing with the physics simulation
float PendulumPeriod(float ropeLength)
{
    float g = Mathf.Abs(Physics2D.gravity.y);
    return 2f * Mathf.PI * Mathf.Sqrt(ropeLength / g);
}

// Example: A 2-meter rope pendulum
float period = PendulumPeriod(2f);  // ≈ 2.84 seconds per swing

// What rope length gives a 1-second swing period?
// T = 2π√(L/g)  →  L = g·(T/2π)²
float RopeLengthForPeriod(float targetPeriod)
{
    float g = Mathf.Abs(Physics2D.gravity.y);
    float ratio = targetPeriod / (2f * Mathf.PI);
    return g * ratio * ratio;
}
```

---

## PART 5: MOMENTUM & COLLISIONS

---

### 5.1 Conservation of Momentum
$$\boxed{m_1 v_1 + m_2 v_2 = m_1 v_1' + m_2 v_2'}$$

**What it means:** In a closed system (no external forces), the total momentum before and after a collision is identical. Unity handles this automatically in physics, but understanding it helps you design meaningful mass ratios.

```
Examples:
  A 1kg bullet at 300 m/s hits a 10kg stationary block:
  Before: p = 1×300 + 10×0 = 300 kg·m/s
  After (perfectly inelastic — they stick): v' = 300 / (1+10) = 27.3 m/s

  A 2kg ball hits a 2kg ball at rest (elastic, 1D):
  They exchange velocities! v1' = 0, v2' = original v1
```

```csharp
// 2D Vector collision response (full formula)
// Elastic collision between two equal-mass objects
// They exchange velocity components along the collision normal
void Elastic2DCollision(Rigidbody2D a, Rigidbody2D b, Vector2 collisionNormal)
{
    // Project velocities onto collision normal
    float a_normal = Vector2.Dot(a.linearVelocity, collisionNormal);
    float b_normal = Vector2.Dot(b.linearVelocity, collisionNormal);

    float ma = a.mass, mb = b.mass;

    // 1D elastic collision formulas applied to the normal component
    float a_new_normal = ((ma - mb) * a_normal + 2 * mb * b_normal) / (ma + mb);
    float b_new_normal = ((mb - ma) * b_normal + 2 * ma * a_normal) / (ma + mb);

    // Reconstruct full 2D velocities
    a.linearVelocity += collisionNormal * (a_new_normal - a_normal);
    b.linearVelocity += collisionNormal * (b_new_normal - b_normal);
}
```

---

### 5.2 Impulse Resolution (Professional Collision Response)

This is the formula game engines use internally. Knowing it lets you write custom collision responses:

$$\boxed{j = \frac{-(1+e)(v_{rel} \cdot \hat{n})}{\frac{1}{m_A} + \frac{1}{m_B}}}$$

Where `j` = impulse scalar, `e` = coefficient of restitution, `v_rel` = relative velocity, `n̂` = collision normal.

```csharp
// Full impulse resolution — apply this to both bodies in OnCollisionEnter2D
// for precise custom collision feel beyond what PhysicsMaterial2D provides
public static void ResolveCollision(Rigidbody2D a, Rigidbody2D b,
                                     Vector2 normal, float restitution = 0.6f)
{
    Vector2 relativeVelocity = a.linearVelocity - b.linearVelocity;
    float velAlongNormal = Vector2.Dot(relativeVelocity, normal);

    // Don't resolve if objects are separating
    if (velAlongNormal > 0) return;

    float invMassA = a.bodyType == RigidbodyType2D.Static ? 0f : 1f / a.mass;
    float invMassB = b.bodyType == RigidbodyType2D.Static ? 0f : 1f / b.mass;

    // j = -(1 + e) * v_rel·n / (1/mA + 1/mB)
    float j = -(1f + restitution) * velAlongNormal / (invMassA + invMassB);

    Vector2 impulse = j * normal;
    if (a.bodyType != RigidbodyType2D.Static) a.AddForce( impulse, ForceMode2D.Impulse);
    if (b.bodyType != RigidbodyType2D.Static) b.AddForce(-impulse, ForceMode2D.Impulse);
}
```

---

## PART 6: VECTOR GEOMETRY FOR GAMES

---

### 6.1 Vector Projection
$$\boxed{\text{proj}_{\hat{b}} \vec{a} = (\vec{a} \cdot \hat{b}) \cdot \hat{b}}$$

**What it means:** Project vector A onto the direction of B. Returns the component of A that lies along B's direction. Essential for separating velocity into "along slope" and "perpendicular to slope" components.

```csharp
// Decompose player velocity into slope-parallel and slope-perpendicular components
// This gives you smooth slope movement without jitter
void MoveOnSlope(Vector2 slopeNormal, float moveSpeed)
{
    // Tangent = direction along the slope surface (perpendicular to normal)
    Vector2 slopeTangent = new Vector2(slopeNormal.y, -slopeNormal.x);

    // Project movement input onto slope tangent
    float inputAlongSlope = Vector2.Dot(Vector2.right * moveSpeed, slopeTangent);
    rb.linearVelocity = slopeTangent * inputAlongSlope;
}

// Reflect a velocity off a surface (perfect billiard ball bounce)
// Reflection formula: v' = v - 2(v·n̂)n̂
Vector2 ReflectVelocity(Vector2 velocity, Vector2 surfaceNormal)
{
    return Vector2.Reflect(velocity, surfaceNormal); // Unity built-in
    // Manual: return velocity - 2 * Vector2.Dot(velocity, surfaceNormal) * surfaceNormal;
}
```

**Game use case:** Slope movement, ricocheting bullets, laser reflections, calculating velocity component for landing detection, decomposing gravity on inclined surfaces.

---

### 6.2 Signed Angle (Clockwise vs Counter-Clockwise)
$$\boxed{\theta_{signed} = \text{atan2}(\vec{a} \times \vec{b},\ \vec{a} \cdot \vec{b})}$$

**What it means:** Regular `Vector2.Angle` always returns 0–180°. `SignedAngle` tells you if B is to the LEFT (+) or RIGHT (−) of A.

```csharp
// Get signed angle from A to B (-180 to +180)
float signedAngle = Vector2.SignedAngle(fromDir, toDir);

// Steer a ship: positive = turn counter-clockwise, negative = turn clockwise
float steer = Vector2.SignedAngle(transform.up, directionToTarget);
rb.AddTorque(-steer * steeringForce * Time.fixedDeltaTime);

// Cap steering angle (don't spin more than maxTurnRate per second)
float maxTorque = rb.inertia * maxTurnRate;
rb.AddTorque(Mathf.Clamp(-steer, -maxTorque, maxTorque));
```

---

### 6.3 Barycentric Coordinates (Point Inside Triangle)

**What it means:** Any point inside a triangle can be expressed as a weighted average of the 3 vertices. Used for interpolating values (color, UV, damage) across a triangular region.

```csharp
// Returns barycentric coordinates (u, v, w) where u+v+w = 1
// If all are >= 0, the point is inside the triangle
public static Vector3 Barycentric(Vector2 p, Vector2 a, Vector2 b, Vector2 c)
{
    Vector2 v0 = b - a, v1 = c - a, v2 = p - a;
    float d00 = Vector2.Dot(v0, v0);
    float d01 = Vector2.Dot(v0, v1);
    float d11 = Vector2.Dot(v1, v1);
    float d20 = Vector2.Dot(v2, v0);
    float d21 = Vector2.Dot(v2, v1);
    float denom = d00 * d11 - d01 * d01;
    float v = (d11 * d20 - d01 * d21) / denom;
    float w = (d00 * d21 - d01 * d20) / denom;
    float u = 1f - v - w;
    return new Vector3(u, v, w);
}

bool IsInsideTriangle(Vector2 p, Vector2 a, Vector2 b, Vector2 c)
{
    Vector3 bary = Barycentric(p, a, b, c);
    return bary.x >= 0 && bary.y >= 0 && bary.z >= 0;
}
```

---

## PART 7: WAVE & SIGNAL MATH

---

### 7.1 Sine Wave Fundamentals
$$\boxed{y(t) = A \cdot \sin(2\pi f \cdot t + \phi)}$$

**Variables:**
- `A` = amplitude (height of the wave)
- `f` = frequency in Hz (cycles per second)
- `t` = time
- `φ` = phase offset in radians

```csharp
// Comprehensive wave toolkit for game effects
public static class WaveMath
{
    // Basic sine wave oscillation (returns value in [-A, +A])
    public static float Sine(float amplitude, float frequencyHz, float time, float phaseRad = 0f)
        => amplitude * Mathf.Sin(2f * Mathf.PI * frequencyHz * time + phaseRad);

    // Sawtooth wave: rises linearly from -A to +A, then resets
    // Use for: count-up timers, scrolling backgrounds, repeating mechanical motion
    public static float Sawtooth(float amplitude, float frequencyHz, float time)
        => amplitude * (2f * Mathf.Repeat(time * frequencyHz, 1f) - 1f);

    // Triangle wave: linear up then linear down (like sine but with sharp corners)
    // Use for: ping-pong animations, simple oscillators without soft acceleration
    public static float Triangle(float amplitude, float frequencyHz, float time)
        => amplitude * (1f - 4f * Mathf.Abs(Mathf.Repeat(time * frequencyHz + 0.25f, 1f) - 0.5f));

    // Square wave: snaps between +A and -A
    // Use for: on/off blinking effects, retro sound generation, binary oscillators
    public static float Square(float amplitude, float frequencyHz, float time)
        => amplitude * Mathf.Sign(Mathf.Sin(2f * Mathf.PI * frequencyHz * time));

    // Damped sine: oscillation that decays over time (like a plucked string)
    // Use for: spring rebound, impact vibration, UI bounce
    public static float DampedSine(float amplitude, float frequencyHz, float decayRate, float time)
        => amplitude * Mathf.Exp(-decayRate * time) * Mathf.Sin(2f * Mathf.PI * frequencyHz * time);
}

// Usage examples:
void Update()
{
    // Hovering item that bobs up and down at 1 Hz with 0.2 meter amplitude
    float bob = WaveMath.Sine(0.2f, 1f, Time.time);
    transform.position = startPos + Vector3.up * bob;

    // Rotate a wheel at constant speed (sawtooth angle)
    float rotationAngle = WaveMath.Sawtooth(360f, 0.5f, Time.time); // 0.5 Hz = 1 rotation per 2 seconds
    wheel.localRotation = Quaternion.Euler(0, 0, rotationAngle);

    // Pulsating light radius (triangle wave is gentler than sine for visual effects)
    light.pointLightOuterRadius = 3f + WaveMath.Triangle(0.5f, 2f, Time.time);
}
```

---

## PART 8: NUMERIC STABILITY & INTEGRATION

---

### 8.1 Euler vs RK4 Integration Comparison

```
Integration Methods (from worst to best stability):
──────────────────────────────────────────────────────────────────
Explicit Euler:   x += v*dt; v += a*dt          [Used by Unity physics]
  → Fast but drifts at large dt. Fine for most games.

Symplectic Euler: v += a*dt; x += v*dt          [Update v THEN x]
  → Energy-conserving. Better for springs/pendulums.

Verlet:           x_new = 2x - x_prev + a*dt²   [No explicit velocity]
  → Excellent for constraint systems (ropes, cloth).

RK4 (Runge-Kutta 4th order): 4 sub-steps        [Most accurate]
  → Expensive but necessary for orbital/high-precision simulations.
──────────────────────────────────────────────────────────────────
```

```csharp
// Symplectic Euler (better than standard Unity Euler for spring systems)
public class SymplecticEulerSpring : MonoBehaviour
{
    private Vector2 position;
    private Vector2 velocity;
    private Vector2 equilibrium;

    [SerializeField] private float k = 50f;  // Spring stiffness
    [SerializeField] private float mass = 1f;

    private void FixedUpdate()
    {
        Vector2 displacement = position - equilibrium;
        Vector2 acceleration = -(k / mass) * displacement;

        // KEY: Update velocity FIRST, then position (symplectic, not explicit Euler)
        velocity  += acceleration * Time.fixedDeltaTime;
        position  += velocity     * Time.fixedDeltaTime;

        transform.position = position;
    }
}
```

---

## QUICK FORMULA LOOKUP TABLE

```
┌──────────────────────────────────┬────────────────────────────────────────────────────┐
│ FORMULA                          │ GAME USE CASE                                      │
├──────────────────────────────────┼────────────────────────────────────────────────────┤
│ F = m·a                          │ Force needed for target acceleration                │
│ v = u + at                       │ Velocity of falling/accelerating object             │
│ s = ut + ½at²                    │ Position of projectile at time t (arc preview)      │
│ v² = u² + 2as                    │ Jump launch speed for target height (h=v²/2g)       │
│ J = m·Δv                         │ Impulse for jump/knockback (ForceMode2D.Impulse)    │
│ KE = ½mv²                        │ Energy of a moving object                          │
│ PE = mgh                         │ Gravitational potential (height-based energy)       │
│ f = μN                           │ Friction force (μ = 0.01 ice, 0.8 rubber)          │
│ F_drag = ½ρv²CdA                 │ Realistic air resistance for projectiles            │
│ v_terminal = √(2mg/ρCdA)         │ Max falling speed (terminal velocity)               │
│ τ = I·α                          │ Torque needed to spin object                       │
│ I = ½mr² (disk)                  │ Moment of inertia of circle/wheel                  │
│ F_c = mv²/r                      │ Centripetal force for circular orbit                │
│ v_orbit = √(GM/r)                │ Stable orbital speed around a planet               │
│ v_max_corner = √(μgr)            │ Max speed to take a corner without sliding          │
│ F = -kx (Hooke's)                │ Spring restoring force                             │
│ ω = √(k/m)                       │ Spring oscillation frequency                       │
│ T = 2π√(L/g)                     │ Pendulum period from rope length                   │
│ T = 2π/ω                         │ Oscillation period from angular frequency           │
│ F = Gm₁m₂/r²                    │ Gravitational pull between two masses               │
│ I₁ω₁ = I₂ω₂                     │ Conservation of angular momentum                   │
│ p = mv (momentum)                │ Total momentum in collision (conserved)             │
│ j = -(1+e)v_rel·n / Σ(1/m)      │ Impulse collision resolution                       │
│ proj = (a·b̂)·b̂                  │ Velocity along slope / surface component           │
│ v' = v - 2(v·n̂)n̂               │ Velocity reflection (mirror bounce off surface)     │
│ y = A·sin(2πft + φ)              │ Oscillating effects (bob, pulse, vibration)         │
└──────────────────────────────────┴────────────────────────────────────────────────────┘
```
