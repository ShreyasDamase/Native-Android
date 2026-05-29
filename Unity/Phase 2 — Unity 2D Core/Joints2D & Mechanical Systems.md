# ⛓️ Joints2D & Mechanical Systems
### HingeJoint2D, SpringJoint2D, DistanceJoint2D, SliderJoint2D & Ragdolls

Joints connect two Rigidbody2D objects (or one to a fixed world anchor point) and enforce a **constraint** between them. They are the building blocks of mechanical systems: doors, pendulums, rope bridges, ragdolls, vehicle suspensions, and crane arms.

---

## 1. Joint Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────────┐
│                    UNITY 2D JOINTS HIERARCHY                             │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  DistanceJoint2D   — Fixed rope length between two bodies                │
│  HingeJoint2D      — Rotational pivot (door, wheel axle, pendulum)       │
│  SpringJoint2D     — Elastic spring force between two bodies             │
│  SliderJoint2D     — Constrained linear slide along an axis (piston)     │
│  FixedJoint2D      — Locks two bodies as one rigid unit                  │
│  RelativeJoint2D   — Maintains relative position/angle (no anchors)     │
│  TargetJoint2D     — Pulls body toward a world-space target (dragging)   │
│  WheelJoint2D      — Motor + spring wheel simulation (vehicles)          │
│  FrictionJoint2D   — Resists linear and angular motion (damping plates)  │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### Shared Joint Properties (All Joints)

```csharp
joint.connectedBody      // The Rigidbody2D this joint connects to (null = world anchor)
joint.autoConfigureConnectedAnchor // Auto-calculate anchor on the connected body
joint.enableCollision    // Allow the two connected bodies to collide with each other
joint.breakForce         // Force magnitude that destroys the joint (Infinity = unbreakable)
joint.breakTorque        // Torque magnitude that destroys the joint
joint.reactionForce      // Read-only: Current force the joint is exerting (use to detect stress)
joint.reactionTorque     // Read-only: Current torque the joint is exerting
```

### Detecting Joint Breaks

```csharp
// OnJointBreak2D is called on the GameObject when the joint exceeds breakForce
private void OnJointBreak2D(Joint2D brokenJoint)
{
    Debug.Log($"Joint broke! Type: {brokenJoint.GetType().Name}");
    // Spawn debris, play break sound, trigger camera shake, etc.
    PlayBreakEffect();
}
```

---

## 2. HingeJoint2D (Pivot / Rotation)

A hinge allows one body to rotate around a pivot anchor point. It can be configured as a free-spinning pivot (door), a motorized rotator (windmill, wheel), or a limited-angle swing (character arm, crane).

### Use Cases
- Swinging doors, gates, drawbridges
- Pendulums, clock arms
- Wheel axles, spinning platforms
- Character limb joints (ragdoll)

```csharp
[RequireComponent(typeof(HingeJoint2D))]
public class HingeDoorController : MonoBehaviour
{
    private HingeJoint2D hinge;

    [SerializeField] private float motorSpeed = 120f;  // degrees per second
    [SerializeField] private float maxMotorTorque = 200f;
    [SerializeField] private float minAngle = -85f;
    [SerializeField] private float maxAngle = 85f;
    [SerializeField] private bool useAngleLimits = true;

    private void Awake()
    {
        hinge = GetComponent<HingeJoint2D>();

        // ─── Motor: actively drives rotation ─────────────────────────────
        hinge.useMotor = false; // Only enable when you want powered rotation
        var motor = hinge.motor;
        motor.motorSpeed = motorSpeed;
        motor.maxMotorTorque = maxMotorTorque;
        hinge.motor = motor;

        // ─── Angle Limits ────────────────────────────────────────────────
        hinge.useLimits = useAngleLimits;
        var limits = hinge.limits;
        limits.min = minAngle;
        limits.max = maxAngle;
        hinge.limits = limits;
    }

    // Open door: activate motor
    public void OpenDoor()
    {
        hinge.useMotor = true;
        var motor = hinge.motor;
        motor.motorSpeed = Mathf.Abs(motorSpeed); // Positive = counter-clockwise
        hinge.motor = motor;
    }

    // Close door: reverse motor direction
    public void CloseDoor()
    {
        hinge.useMotor = true;
        var motor = hinge.motor;
        motor.motorSpeed = -Mathf.Abs(motorSpeed);
        hinge.motor = motor;
    }

    // Lock door in place: disable motor
    public void LockDoor() => hinge.useMotor = false;

    // Read joint angle and angular velocity
    private void Update()
    {
        float currentAngle = hinge.jointAngle;          // Current rotation in degrees
        float currentSpeed = hinge.jointSpeed;          // Current angular velocity (deg/s)
        Debug.Log($"Door angle: {currentAngle:F1}°, Speed: {currentSpeed:F1}°/s");
    }
}
```

### Pendulum with Physics

```csharp
// Setup a physics-accurate pendulum: set the HingeJoint2D anchor at the top
// Let gravity drive the swing naturally — don't use a motor
public class Pendulum : MonoBehaviour
{
    [SerializeField] private float initialAngle = 60f; // Starting angle offset

    private void Start()
    {
        // Give it an initial angular push
        GetComponent<Rigidbody2D>().angularVelocity = initialAngle;
    }
}
```

---

## 3. SpringJoint2D (Elastic Connection)

A spring pulls two bodies toward each other with a force proportional to how far they are stretched beyond the rest distance. It can also push them apart if compressed.

```
           |  RestLength
    [Body A] ~~~~~~~~~~~ [Body B]
             ← spring →
```

### Key Properties

```csharp
joint.distance            // Natural rest length of the spring
joint.dampingRatio        // 0 = oscillates forever, 1 = critically damped (no oscillation)
joint.frequency           // Spring oscillation frequency in Hz (higher = stiffer spring)
joint.autoConfigureDistance // Auto-set rest length to current separation
```

### Practical: Elastic Rope / Bungee

```csharp
[RequireComponent(typeof(SpringJoint2D))]
public class BungeeController : MonoBehaviour
{
    private SpringJoint2D spring;

    [SerializeField] private float stiffness = 12f;       // Hz — spring oscillation frequency
    [SerializeField] private float damping = 0.3f;         // 0 = bouncy, 1 = no bounce
    [SerializeField] private float restLength = 3f;

    private void Awake()
    {
        spring = GetComponent<SpringJoint2D>();
        spring.frequency = stiffness;
        spring.dampingRatio = damping;
        spring.distance = restLength;
        spring.autoConfigureDistance = false;
    }

    // Stretch or compress the rest length dynamically
    public void SetRestLength(float newLength) => spring.distance = newLength;

    // Read how much the spring is currently stressed
    public float GetCurrentStress() => spring.reactionForce.magnitude;
}
```

### Spring Suspension (Vehicle / Platform)

```csharp
// Vehicle body connected via SpringJoint2D to each wheel axle
// Wheel axle is a separate child Rigidbody2D without gravity that follows terrain via FixedUpdate
public class VehicleSuspension : MonoBehaviour
{
    [SerializeField] private SpringJoint2D[] suspensionJoints; // One per wheel

    [SerializeField] private float suspensionFrequency = 8f;   // Stiffness
    [SerializeField] private float suspensionDamping = 0.5f;   // Comfort level

    private void Start()
    {
        foreach (var joint in suspensionJoints)
        {
            joint.frequency = suspensionFrequency;
            joint.dampingRatio = suspensionDamping;
        }
    }
}
```

---

## 4. DistanceJoint2D (Fixed-Length Rope / Tether)

A DistanceJoint2D enforces a maximum distance between two bodies. Unlike SpringJoint2D, it does not push bodies apart — it only prevents them from going further than the specified distance (like a rope, not a spring).

```csharp
[RequireComponent(typeof(DistanceJoint2D))]
public class GrapplingHook : MonoBehaviour
{
    private DistanceJoint2D distanceJoint;
    private Rigidbody2D rb;
    private LineRenderer ropeRenderer;

    private void Awake()
    {
        rb = GetComponent<Rigidbody2D>();
        distanceJoint = GetComponent<DistanceJoint2D>();
        ropeRenderer = GetComponent<LineRenderer>();

        distanceJoint.enabled = false; // Start disconnected
    }

    // Fire grapple at a world position
    public void AttachGrapple(Vector2 anchorWorldPosition)
    {
        distanceJoint.enabled = true;
        distanceJoint.connectedAnchor = anchorWorldPosition; // Attach to world point (no Rigidbody)
        distanceJoint.connectedBody = null;
        distanceJoint.autoConfigureDistance = false;

        // Rope length = distance from player to attachment point
        distanceJoint.distance = Vector2.Distance(rb.position, anchorWorldPosition);

        // Allow swinging in closer (pull-toward behavior)
        distanceJoint.maxDistanceOnly = true;

        ropeRenderer.enabled = true;
    }

    // Reel in: shorten the rope
    public void ReelIn(float amount)
    {
        distanceJoint.distance = Mathf.Max(0.5f, distanceJoint.distance - amount * Time.deltaTime);
    }

    // Release grapple
    public void Release()
    {
        distanceJoint.enabled = false;
        ropeRenderer.enabled = false;
    }

    private void LateUpdate()
    {
        if (!distanceJoint.enabled) return;
        ropeRenderer.SetPosition(0, transform.position);
        ropeRenderer.SetPosition(1, distanceJoint.connectedAnchor);
    }
}
```

---

## 5. SliderJoint2D (Linear Slide / Piston)

A SliderJoint2D constrains movement to a single axis (like a rail or piston). The body can only slide along that axis, not rotate or move perpendicular.

```csharp
[RequireComponent(typeof(SliderJoint2D))]
public class LinearPiston : MonoBehaviour
{
    private SliderJoint2D slider;

    [SerializeField] private float motorSpeed = 5f;         // Units per second
    [SerializeField] private float maxMotorForce = 1000f;
    [SerializeField] private float minTranslation = -3f;    // Minimum slide position
    [SerializeField] private float maxTranslation = 3f;     // Maximum slide position

    private void Awake()
    {
        slider = GetComponent<SliderJoint2D>();

        // The angle (in degrees) defines the slide axis direction
        slider.angle = 90f; // 0 = horizontal, 90 = vertical

        // Set translation limits
        slider.useLimits = true;
        var limits = new JointTranslationLimits2D();
        limits.min = minTranslation;
        limits.max = maxTranslation;
        slider.limits = limits;

        // Configure motor
        slider.useMotor = true;
        var motor = new JointMotor2D();
        motor.motorSpeed = motorSpeed;
        motor.maxMotorTorque = maxMotorForce;
        slider.motor = motor;
    }

    // Read current position along the slider axis
    private void Update()
    {
        float currentPosition = slider.jointTranslation;  // Current displacement from anchor
        float currentSpeed = slider.jointSpeed;           // Speed along the axis
    }

    // Extend piston outward
    public void Extend()
    {
        var motor = slider.motor;
        motor.motorSpeed = Mathf.Abs(motorSpeed);
        slider.motor = motor;
    }

    // Retract piston inward
    public void Retract()
    {
        var motor = slider.motor;
        motor.motorSpeed = -Mathf.Abs(motorSpeed);
        slider.motor = motor;
    }
}
```

---

## 6. WheelJoint2D (Vehicle Wheels)

`WheelJoint2D` combines a suspension spring (linear) with a motor (angular). It is the correct joint for wheel-based vehicles.

```csharp
[RequireComponent(typeof(WheelJoint2D))]
public class WheelController : MonoBehaviour
{
    private WheelJoint2D wheel;

    [SerializeField] private float topSpeed = 15f;           // Max motor speed (deg/s)
    [SerializeField] private float motorTorque = 800f;
    [SerializeField] private float suspensionFrequency = 6f;
    [SerializeField] private float suspensionDamping = 0.4f;

    private void Awake()
    {
        wheel = GetComponent<WheelJoint2D>();

        // Suspension spring settings
        var suspension = wheel.suspension;
        suspension.frequency = suspensionFrequency;
        suspension.dampingRatio = suspensionDamping;
        wheel.suspension = suspension;

        // Motor settings
        var motor = wheel.motor;
        motor.maxMotorTorque = motorTorque;
        wheel.motor = motor;
        wheel.useMotor = true;
    }

    public void Drive(float inputAxis)
    {
        // inputAxis: -1 (reverse), 0 (brake), +1 (forward)
        var motor = wheel.motor;
        motor.motorSpeed = inputAxis * topSpeed;
        wheel.motor = motor;
    }

    // Read wheel contact stress (for sound and particle effects)
    public float GetWheelStress() => wheel.reactionForce.magnitude;
}
```

---

## 7. TargetJoint2D (Mouse / Touch Drag)

Pulls a Rigidbody2D toward a continuously updating world-space target position. Perfect for dragging physics objects with the mouse or a touch finger.

```csharp
public class PhysicsDragger : MonoBehaviour
{
    private TargetJoint2D targetJoint;
    private Camera mainCamera;

    [SerializeField] private float springFrequency = 15f;
    [SerializeField] private float springDamping = 0.8f;

    private void Awake() => mainCamera = Camera.main;

    private void OnMouseDown()
    {
        // Add a TargetJoint2D dynamically when the user clicks
        targetJoint = gameObject.AddComponent<TargetJoint2D>();
        targetJoint.frequency = springFrequency;
        targetJoint.dampingRatio = springDamping;

        // Anchor to the exact click point on the body
        targetJoint.anchor = transform.InverseTransformPoint(GetMouseWorldPos());
    }

    private void OnMouseDrag()
    {
        if (targetJoint)
            targetJoint.target = GetMouseWorldPos();
    }

    private void OnMouseUp()
    {
        Destroy(targetJoint);
    }

    private Vector2 GetMouseWorldPos()
    {
        Vector3 pos = mainCamera.ScreenToWorldPoint(Input.mousePosition);
        pos.z = 0;
        return pos;
    }
}
```

---

## 8. Ragdoll System (Chained Joints)

A 2D ragdoll is a hierarchy of Rigidbody2D + Collider2D objects connected with HingeJoint2Ds, each with angle limits that match anatomical joint ranges.

```
       [Head]
         |  ← HingeJoint2D (limited neck rotation)
      [Torso]
      /     \
 [L.Arm]  [R.Arm]   ← HingeJoint2D per shoulder/elbow
 [L.Leg]  [R.Leg]   ← HingeJoint2D per hip/knee
```

```csharp
// Ragdoll controller — toggles between animated and physics-driven states
public class RagdollController : MonoBehaviour
{
    [SerializeField] private Animator animator;
    [SerializeField] private Rigidbody2D[] ragdollBodies;     // All body parts
    [SerializeField] private Collider2D[] ragdollColliders;

    private bool isRagdolling;

    private void Awake()
    {
        SetRagdollActive(false); // Start in animated state
    }

    public void ActivateRagdoll(Vector2 impactForce)
    {
        SetRagdollActive(true);

        // Apply the impact force to the torso (first/root body)
        ragdollBodies[0].AddForce(impactForce, ForceMode2D.Impulse);
    }

    private void SetRagdollActive(bool active)
    {
        isRagdolling = active;
        animator.enabled = !active; // Disable animator when ragdolling

        foreach (var rb in ragdollBodies)
        {
            rb.bodyType = active ? RigidbodyType2D.Dynamic : RigidbodyType2D.Kinematic;
            rb.simulated = active;
        }

        foreach (var col in ragdollColliders)
            col.enabled = active;
    }

    public void RecoverFromRagdoll()
    {
        SetRagdollActive(false);
        // Trigger a "get up" animation
        animator.SetTrigger("GetUp");
    }
}
```

---

## 9. Joint Quick Reference

```
┌──────────────────┬────────────────────────────────────┬────────────────────────────┐
│ Joint            │ Best For                           │ Key Property               │
├──────────────────┼────────────────────────────────────┼────────────────────────────┤
│ HingeJoint2D     │ Doors, pendulums, wheels, ragdolls │ useMotor, useLimits, angle │
│ SpringJoint2D    │ Elastic connections, bungee         │ frequency, dampingRatio    │
│ DistanceJoint2D  │ Rope tether, grappling hook         │ distance, maxDistanceOnly  │
│ SliderJoint2D    │ Pistons, elevators, sliders         │ angle, useLimits, motor    │
│ FixedJoint2D     │ Weld two bodies together            │ dampingRatio, frequency    │
│ WheelJoint2D     │ Wheeled vehicles                   │ suspension, motor          │
│ TargetJoint2D    │ Mouse/touch drag                   │ target, anchor             │
│ RelativeJoint2D  │ Keep offset without anchors         │ linearOffset, angularOffset│
│ FrictionJoint2D  │ Dampen movement (sticky surfaces)  │ maxForce, maxTorque        │
└──────────────────┴────────────────────────────────────┴────────────────────────────┘
```
