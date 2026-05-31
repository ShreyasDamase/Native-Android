# 00F — Rigidbody3D & Physics3D
### 🔶 Block 0 — Unity Engine Fundamentals

> [!NOTE]
> 3D physics in Unity works almost the same as 2D but with a **third axis (Z)** and different component names — `Rigidbody` instead of `Rigidbody2D`, `Collider` instead of `Collider2D`. The biggest new challenge in 3D is **rotation** — you use `Quaternion` instead of a simple float angle.

---

## 00F.1 — Rigidbody (3D) vs Rigidbody2D — The Differences

| Property | Rigidbody2D (2D) | Rigidbody (3D) |
| :--- | :--- | :--- |
| Namespace | `UnityEngine` | `UnityEngine` |
| Movement | X and Y only | X, Y, and Z |
| Rotation | Z axis only (single float) | X, Y, Z axes (Quaternion) |
| Gravity | `gravityScale` multiplier | Uses `Physics.gravity` Vector3 |
| Constraints | FreezePositionX/Y, FreezeRotationZ | FreezePositionX/Y/Z, FreezeRotationX/Y/Z |
| Velocity | `linearVelocity` (Vector2) | `linearVelocity` (Vector3) |
| Force mode | `ForceMode2D.Impulse/Force` | `ForceMode.Impulse/Force/Acceleration/VelocityChange` |

---

## 00F.2 — Rigidbody (3D) — Every Inspector Field

```
Rigidbody
├── Mass                    [1]
├── Drag                    [0]          ← same as Linear Drag in 2D
├── Angular Drag            [0.05]
├── Use Gravity             [✓]          ← replaces gravityScale — simple on/off
├── Is Kinematic            [□]          ← replaces Body Type in 2D
├── Interpolate             [None ▼]     ← 3D exclusive — smoother movement
├── Collision Detection     [Discrete ▼]
└── Constraints
    ├── Freeze Position     [□ X] [□ Y] [□ Z]
    └── Freeze Rotation     [□ X] [□ Y] [□ Z]
```

### `Mass`, `Drag`, `Angular Drag`, `Collision Detection`
Same behavior as 2D equivalents (see file 00D). Read those explanations.

### `Use Gravity` (3D replaces `gravityScale`)
In 3D, gravity is a **global setting** in `Physics.gravity` (default: `(0, -9.81, 0)`).

```csharp
// Toggle gravity on/off
rb.useGravity = true;   // default — gravity applies
rb.useGravity = false;  // no gravity — flying, swimming, space

// Change global gravity direction (affects ALL rigidbodies)
Physics.gravity = new Vector3(0f, -20f, 0f);  // stronger gravity
Physics.gravity = new Vector3(0f, -9.81f, 0f); // Earth-like (default)
Physics.gravity = Vector3.zero;               // zero gravity — space game
Physics.gravity = new Vector3(0f, 0f, -9.81f); // gravity pulls toward screen!
```

### `Is Kinematic` (3D replaces Body Type dropdown)
In 3D there's no dropdown — just a boolean.

```csharp
rb.isKinematic = false; // Dynamic physics (default)
rb.isKinematic = true;  // Kinematic — manual movement only, no physics forces
```

### `Interpolate` — Smooth Rendering (3D Exclusive)

Physics runs at a fixed rate (FixedUpdate). Rendering runs at the display's frame rate. If they're different rates, objects can appear to stutter.

```
Interpolate = None:        No smoothing. Position snaps to physics position.
Interpolate = Interpolate: Smooths based on last two physics positions. ← USE THIS for player
Interpolate = Extrapolate: Predicts future position. Can cause visual artifacts.
```

```csharp
rb.interpolation = RigidbodyInterpolation.Interpolate;  // smooth player movement
rb.interpolation = RigidbodyInterpolation.None;          // no smoothing (default)
```

---

## 00F.3 — Moving with Rigidbody (3D)

```csharp
public class Physics3DMovement : MonoBehaviour
{
    [SerializeField] private float moveSpeed = 5f;
    [SerializeField] private float jumpForce = 8f;
    [SerializeField] private float rotationSpeed = 10f;
    
    private Rigidbody rb;
    private bool isGrounded;
    private Vector3 moveInput;
    
    void Awake()
    {
        rb = GetComponent<Rigidbody>();
        rb.freezeRotation = true; // equivalent to FreezeRotation in constraints
    }
    
    void Update()
    {
        // Read input
        float h = Input.GetAxisRaw("Horizontal"); // left/right (A/D)
        float v = Input.GetAxisRaw("Vertical");   // forward/back (W/S)
        moveInput = new Vector3(h, 0f, v).normalized;
        
        if (Input.GetButtonDown("Jump") && isGrounded)
            jumpQueued = true;
    }
    
    void FixedUpdate()
    {
        // ===== OPTION 1: Velocity-based (snappy, direct control) =====
        Vector3 targetVel = moveInput * moveSpeed;
        targetVel.y = rb.linearVelocity.y; // preserve gravity
        rb.linearVelocity = targetVel;
        
        // ===== OPTION 2: Force-based (realistic, floaty) =====
        rb.AddForce(moveInput * moveSpeed, ForceMode.Force);
        
        // ===== OPTION 3: Acceleration toward target velocity =====
        Vector3 desiredVelocity = moveInput * moveSpeed;
        desiredVelocity.y = rb.linearVelocity.y;
        Vector3 velocityChange = desiredVelocity - rb.linearVelocity;
        velocityChange.y = 0f; // don't interfere with gravity
        rb.AddForce(velocityChange, ForceMode.VelocityChange); // instant velocity change
        
        // ===== JUMP =====
        if (jumpQueued)
        {
            rb.AddForce(Vector3.up * jumpForce, ForceMode.Impulse);
            jumpQueued = false;
        }
        
        // ===== SMOOTH ROTATION =====
        if (moveInput != Vector3.zero)
        {
            Quaternion targetRot = Quaternion.LookRotation(moveInput);
            transform.rotation = Quaternion.Slerp(transform.rotation, targetRot, rotationSpeed * Time.fixedDeltaTime);
        }
        
        // ===== GROUND CHECK =====
        isGrounded = Physics.CheckSphere(
            transform.position + Vector3.down * 0.9f, // slightly below center
            0.3f,                                      // check radius
            LayerMask.GetMask("Ground")
        );
    }
    
    private bool jumpQueued;
}
```

---

## 00F.4 — 3D Colliders — Every Type

### `BoxCollider` — Rectangle Box (3D)
```csharp
BoxCollider box = GetComponent<BoxCollider>();
box.size = new Vector3(1f, 2f, 0.5f); // width, height, depth
box.center = Vector3.zero;
```

### `SphereCollider` — Sphere Shape
```csharp
SphereCollider sphere = GetComponent<SphereCollider>();
sphere.radius = 0.5f;
sphere.center = Vector3.zero;
```

### `CapsuleCollider` — Capsule (Pill)
```csharp
CapsuleCollider cap = GetComponent<CapsuleCollider>();
cap.radius = 0.4f;   // width of the capsule halves
cap.height = 2f;     // total height
cap.direction = 1;   // 0=X, 1=Y, 2=Z axis alignment
```

### `MeshCollider` — Exact Mesh Shape
```csharp
MeshCollider mesh = GetComponent<MeshCollider>();
mesh.sharedMesh = myMesh;       // uses the mesh itself as collision shape
mesh.convex = true;             // must be convex for collision with Rigidbody
```

### `TerrainCollider` — Unity Terrain
Automatically created with Unity's Terrain component — covers the entire terrain heightmap.

---

## 00F.5 — 3D Physics Queries

```csharp
public class PhysicsQueries3D : MonoBehaviour
{
    [SerializeField] private LayerMask groundLayer;
    [SerializeField] private LayerMask enemyLayer;
    
    private RaycastHit[] rayBuffer = new RaycastHit[16];
    private Collider[] overlapBuffer = new Collider[32];
    
    void QueryExamples()
    {
        // ===== RAYCAST =====
        // Cast a ray from a point in a direction
        if (Physics.Raycast(transform.position, Vector3.down, out RaycastHit hit, 2f, groundLayer))
        {
            Debug.Log($"Ground at: {hit.point}");
            Debug.Log($"Normal: {hit.normal}");
            Debug.Log($"Distance: {hit.distance}");
            Debug.Log($"Object: {hit.collider.name}");
        }
        
        // Ray from camera through mouse (for clicking objects)
        Ray mouseRay = Camera.main.ScreenPointToRay(Input.mousePosition);
        if (Physics.Raycast(mouseRay, out RaycastHit mouseHit, 100f))
        {
            Debug.Log($"Clicked on: {mouseHit.collider.name}");
        }
        
        // NonAlloc multiple hits
        int hitCount = Physics.RaycastNonAlloc(transform.position, Vector3.forward, rayBuffer, 20f, enemyLayer);
        for (int i = 0; i < hitCount; i++)
            Debug.Log($"Enemy: {rayBuffer[i].collider.name}");
        
        // ===== SPHERE CAST (sweep a sphere through space) =====
        // Great for character ground detection — catches edges better than raycast
        if (Physics.SphereCast(transform.position, 0.4f, Vector3.down, out RaycastHit sphereHit, 1.1f, groundLayer))
        {
            isGrounded = true;
            groundNormal = sphereHit.normal;
            slopeAngle = Vector3.Angle(groundNormal, Vector3.up);
        }
        
        // ===== BOX CAST (sweep a box) =====
        if (Physics.BoxCast(transform.position, new Vector3(0.4f, 0.1f, 0.4f), Vector3.down,
            out RaycastHit boxHit, transform.rotation, 1f, groundLayer))
        {
            Debug.Log("Box hit ground");
        }
        
        // ===== CAPSULE CAST (sweep a capsule — for character movement) =====
        Vector3 top = transform.position + Vector3.up * 1.5f;
        Vector3 bottom = transform.position + Vector3.up * 0.5f;
        if (Physics.CapsuleCast(top, bottom, 0.4f, Vector3.forward, out RaycastHit capsuleHit, 2f))
        {
            Debug.Log($"Character would hit: {capsuleHit.collider.name}");
        }
        
        // ===== OVERLAP SPHERE (find all objects in radius) =====
        int overlapCount = Physics.OverlapSphereNonAlloc(transform.position, 5f, overlapBuffer, enemyLayer);
        for (int i = 0; i < overlapCount; i++)
        {
            if (overlapBuffer[i].TryGetComponent<IDamageable>(out IDamageable target))
                target.TakeDamage(10f);
        }
        
        // ===== OVERLAP BOX =====
        int boxCount = Physics.OverlapBoxNonAlloc(
            transform.position + transform.forward,
            new Vector3(1f, 1f, 2f),   // half extents
            overlapBuffer,
            transform.rotation,
            enemyLayer
        );
        
        // ===== CHECK SPHERE (boolean only) =====
        bool nearEnemy = Physics.CheckSphere(transform.position, 2f, enemyLayer);
        bool isGroundedSimple = Physics.CheckSphere(feetPos, 0.3f, groundLayer);
    }
    
    private bool isGrounded;
    private Vector3 groundNormal;
    private float slopeAngle;
    private Vector3 feetPos => transform.position + Vector3.down * 0.9f;
}
```

---

## 00F.6 — CharacterController — The Alternative to Rigidbody in 3D

For 3D games, many developers use `CharacterController` instead of `Rigidbody`. It gives you **direct movement control** without fighting against physics forces.

```
CharacterController vs Rigidbody (3D):

CharacterController:                    Rigidbody:
─────────────────────────────────       ─────────────────────────────────
You control movement directly           Physics engine controls movement
Built-in slope handling                 Manual slope detection
Built-in step-over logic                Manual step handling
No bouncing or sliding from physics     Can bounce/slide from physics
Very predictable, game-feel control     More realistic, harder to tune
Used by: FPS games, action RPGs         Used by: physics puzzles, pinball
```

```csharp
[RequireComponent(typeof(CharacterController))]
public class FPSController : MonoBehaviour
{
    [Header("Movement")]
    [SerializeField] private float walkSpeed = 5f;
    [SerializeField] private float runSpeed = 10f;
    [SerializeField] private float jumpHeight = 2f;
    [SerializeField] private float gravity = -20f;
    
    [Header("Mouse Look")]
    [SerializeField] private float mouseSensitivity = 2f;
    [SerializeField] private float maxPitchAngle = 80f;
    [SerializeField] private Transform cameraTransform;
    
    private CharacterController cc;
    private Vector3 velocity;        // tracks Y velocity (gravity accumulation)
    private float pitch = 0f;        // camera up/down angle
    
    void Awake()
    {
        cc = GetComponent<CharacterController>();
        Cursor.lockState = CursorLockMode.Locked; // lock mouse to window
        Cursor.visible = false;
    }
    
    void Update()
    {
        // ===== MOUSE LOOK =====
        float mouseX = Input.GetAxis("Mouse X") * mouseSensitivity;
        float mouseY = Input.GetAxis("Mouse Y") * mouseSensitivity;
        
        transform.Rotate(Vector3.up * mouseX);    // rotate body left/right (yaw)
        
        pitch -= mouseY;
        pitch = Mathf.Clamp(pitch, -maxPitchAngle, maxPitchAngle);
        cameraTransform.localRotation = Quaternion.Euler(pitch, 0f, 0f); // camera up/down (pitch)
        
        // ===== MOVEMENT =====
        float h = Input.GetAxisRaw("Horizontal");
        float v = Input.GetAxisRaw("Vertical");
        bool run = Input.GetButton("Sprint");
        
        Vector3 moveDir = transform.right * h + transform.forward * v;
        moveDir = Vector3.ClampMagnitude(moveDir, 1f); // prevent diagonal speed boost
        float speed = run ? runSpeed : walkSpeed;
        
        // ===== GRAVITY & JUMP =====
        bool grounded = cc.isGrounded; // CharacterController has built-in ground check!
        
        if (grounded && velocity.y < 0f)
            velocity.y = -2f; // small downward force to stick to ground
        
        if (Input.GetButtonDown("Jump") && grounded)
            velocity.y = Mathf.Sqrt(jumpHeight * -2f * gravity); // physics formula
        
        velocity.y += gravity * Time.deltaTime; // accumulate gravity
        
        // ===== APPLY MOVEMENT =====
        // Move combines horizontal movement + vertical gravity
        cc.Move((moveDir * speed + velocity) * Time.deltaTime);
    }
}
```

---

## 00F.7 — CharacterController Inspector Fields

```
CharacterController
├── Slope Limit     [45]     ← Max slope angle character can walk up (degrees)
├── Step Offset     [0.3]    ← How high a step the character can auto-climb (units)
├── Skin Width      [0.08]   ← Thin buffer zone to prevent clipping (usually leave default)
├── Min Move Distance [0]    ← Minimum distance before movement is registered
├── Center         X[0] Y[0] Z[0]  ← Offset the collision capsule center
├── Radius          [0.5]    ← Width of the character capsule (half-width)
└── Height          [2]      ← Total height of the character capsule
```

```csharp
CharacterController cc = GetComponent<CharacterController>();

// Key properties:
bool grounded = cc.isGrounded;     // built-in ground detection — very reliable
float radius = cc.radius;          // capsule radius
float height = cc.height;          // capsule height

cc.Move(velocity * Time.deltaTime); // the main movement method
// Move handles collision resolution, slopes, and steps automatically!

// No forces, no AddForce — you control everything via cc.Move()
```

---

## 00F.8 — 3D Collision Callbacks

```csharp
public class PhysicsObject3D : MonoBehaviour
{
    // 3D collision/trigger callbacks — NO "2D" suffix!
    
    void OnCollisionEnter(Collision collision)
    {
        // physical collision started
        float impactSpeed = collision.relativeVelocity.magnitude;
        ContactPoint contact = collision.GetContact(0);
        Vector3 point = contact.point;
        Vector3 normal = contact.normal;
        
        if (collision.gameObject.CompareTag("Ground"))
            Debug.Log($"Landed! Impact speed: {impactSpeed}");
    }
    
    void OnCollisionStay(Collision collision) { }
    
    void OnCollisionExit(Collision collision)
    {
        Debug.Log($"Separated from {collision.gameObject.name}");
    }
    
    void OnTriggerEnter(Collider other) // no "2D" — this is 3D
    {
        if (other.CompareTag("Checkpoint"))
            SaveCheckpoint(other.transform.position);
    }
    
    void OnTriggerStay(Collider other) { }
    void OnTriggerExit(Collider other) { }
    
    void SaveCheckpoint(Vector3 pos) { }
}
```

---

## 📝 Summary — 3D Physics Quick Reference

| Feature | 2D | 3D |
| :--- | :--- | :--- |
| Rigidbody component | `Rigidbody2D` | `Rigidbody` |
| Velocity | `linearVelocity` (Vector2) | `linearVelocity` (Vector3) |
| Add force | `AddForce(v2, ForceMode2D)` | `AddForce(v3, ForceMode)` |
| Gravity control | `rb.gravityScale = 2f` | `rb.useGravity = true/false` |
| Is Kinematic | `rb.bodyType = Kinematic` | `rb.isKinematic = true` |
| Freeze rotation | `FreezeRotationZ` | `FreezeRotationX/Y/Z` |
| Collision callback | `OnCollisionEnter2D(Collision2D)` | `OnCollisionEnter(Collision)` |
| Trigger callback | `OnTriggerEnter2D(Collider2D)` | `OnTriggerEnter(Collider)` |
| Raycast | `Physics2D.Raycast()` | `Physics.Raycast()` |
| Overlap sphere | `Physics2D.OverlapCircle()` | `Physics.OverlapSphere()` |
| Ground check | `OverlapCircle` at feet | `CheckSphere` or `cc.isGrounded` |
| Movement alternative | Rigidbody2D always | `CharacterController` (3D only) |

**Previous:** [[00E — Colliders & Triggers 2D]] | **Next:** [[00G — Camera, Rendering & Prefabs]]
