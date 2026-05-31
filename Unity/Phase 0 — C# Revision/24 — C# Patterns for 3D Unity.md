# 24 — C# Patterns for 3D Unity
### 🟡 Block E — 2D & 3D Unity-Specific C# Patterns

> [!IMPORTANT]
> 3D Unity introduces concepts that don't exist in 2D: **Quaternions for rotation**, **NavMesh for pathfinding**, **CharacterController for movement**, and **3D physics with normals and contact points**. This chapter is your complete 3D coding reference.

---

## 24.1 — The Biggest 3D vs 2D Difference: Rotation

In 2D, rotation is a single float (angle around Z). In 3D, rotation is a Quaternion — a 4D mathematical object.

```csharp
// ===== 2D Rotation =====
// Just set a Z angle — simple
transform.rotation = Quaternion.Euler(0f, 0f, 45f); // 45 degrees on Z

// ===== 3D Rotation =====
// NEVER directly set x, y, z, w on a Quaternion — it makes no intuitive sense
// Use helper methods instead

// Euler angles (degrees) → Quaternion
transform.rotation = Quaternion.Euler(30f, 90f, 0f); // pitch 30°, yaw 90°, roll 0°

// Look at a target
transform.LookAt(target.position);  // instantly face target
transform.LookAt(target.position, Vector3.up); // up = world up axis

// Create rotation from a direction
Vector3 direction = (target.position - transform.position).normalized;
Quaternion lookRot = Quaternion.LookRotation(direction, Vector3.up);
transform.rotation = lookRot;

// Smooth rotation toward a target direction
transform.rotation = Quaternion.RotateTowards(transform.rotation, targetRot, 360f * Time.deltaTime);

// Smooth rotation with Slerp (spherical interpolation)
transform.rotation = Quaternion.Slerp(transform.rotation, targetRot, rotationSpeed * Time.deltaTime);

// Get euler angles from a quaternion (be careful — gimbal lock exists)
Vector3 angles = transform.rotation.eulerAngles;
float yaw = angles.y; // rotation around Y axis (left/right)

// Combine rotations (order matters!)
Quaternion combined = rotationA * rotationB; // apply B first, then A

// Rotate a vector by a quaternion
Vector3 rotatedDirection = someRotation * Vector3.forward;

// Identity — no rotation
transform.rotation = Quaternion.identity;
```

---

## 24.2 — Complete 3D Character Controller

```csharp
[RequireComponent(typeof(CharacterController))]
[RequireComponent(typeof(Animator))]
public class ThirdPersonController : MonoBehaviour
{
    [Header("Movement")]
    [SerializeField, Range(1f, 20f)] private float walkSpeed = 5f;
    [SerializeField, Range(1f, 30f)] private float runSpeed = 10f;
    [SerializeField, Range(1f, 30f)] private float jumpHeight = 5f;
    [SerializeField, Range(0f, 20f)] private float gravity = -20f;
    [SerializeField, Range(1f, 20f)] private float rotationSpeed = 10f;
    
    [Header("Ground Detection")]
    [SerializeField] private Transform groundCheck;
    [SerializeField, Range(0.1f, 1f)] private float groundCheckRadius = 0.3f;
    [SerializeField] private LayerMask groundLayer;
    
    [Header("Camera")]
    [SerializeField] private Transform cameraTransform;
    
    private CharacterController cc;
    private Animator anim;
    
    private Vector3 velocity;      // includes gravity Y component
    private bool isGrounded;
    private Vector2 moveInput;
    
    private static readonly int HashSpeed   = Animator.StringToHash("Speed");
    private static readonly int HashJump    = Animator.StringToHash("Jump");
    private static readonly int HashGrounded = Animator.StringToHash("IsGrounded");
    
    void Awake()
    {
        cc   = GetComponent<CharacterController>();
        anim = GetComponent<Animator>();
    }
    
    void Update()
    {
        // Ground check
        isGrounded = Physics.CheckSphere(groundCheck.position, groundCheckRadius, groundLayer);
        
        if (isGrounded && velocity.y < 0f)
            velocity.y = -2f; // small downward force to stay grounded
        
        // Input
        moveInput = new Vector2(Input.GetAxisRaw("Horizontal"), Input.GetAxisRaw("Vertical"));
        bool sprint = Input.GetButton("Sprint");
        bool jump = Input.GetButtonDown("Jump");
        
        // Movement direction relative to camera
        Vector3 forward = cameraTransform.forward;
        Vector3 right = cameraTransform.right;
        forward.y = 0f;  // flatten to ground plane
        right.y = 0f;
        forward.Normalize();
        right.Normalize();
        
        Vector3 moveDir = (forward * moveInput.y + right * moveInput.x).normalized;
        float currentSpeed = sprint ? runSpeed : walkSpeed;
        
        // Apply horizontal movement
        cc.Move(moveDir * currentSpeed * Time.deltaTime);
        
        // Rotate character to face movement direction
        if (moveDir != Vector3.zero)
        {
            Quaternion targetRot = Quaternion.LookRotation(moveDir);
            transform.rotation = Quaternion.Slerp(transform.rotation, targetRot, rotationSpeed * Time.deltaTime);
        }
        
        // Jump
        if (jump && isGrounded)
        {
            // v² = 2*g*h → v = sqrt(2*g*h)
            velocity.y = Mathf.Sqrt(jumpHeight * -2f * gravity);
            anim.SetTrigger(HashJump);
        }
        
        // Apply gravity
        velocity.y += gravity * Time.deltaTime;
        cc.Move(velocity * Time.deltaTime);
        
        // Animation
        float speedRatio = moveDir.magnitude * (sprint ? 1f : 0.5f);
        anim.SetFloat(HashSpeed, speedRatio, 0.1f, Time.deltaTime); // damped set
        anim.SetBool(HashGrounded, isGrounded);
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

## 24.3 — 3D Physics Reference

```csharp
public class PhysicsReference3D : MonoBehaviour
{
    private Rigidbody rb;
    
    private readonly RaycastHit[] rayBuffer = new RaycastHit[16];
    private readonly Collider[] overlapBuffer = new Collider[32];
    
    void PhysicsMovement()
    {
        // Direct velocity control (kinematic-style)
        Vector3 moveVel = Vector3.forward * 5f;
        moveVel.y = rb.linearVelocity.y; // preserve gravity
        rb.linearVelocity = moveVel;
        
        // Forces
        rb.AddForce(Vector3.up * 10f, ForceMode.Impulse);  // instant push (jump)
        rb.AddForce(Vector3.forward * 5f, ForceMode.Force); // continuous force (wind)
        
        // Relative force (forward of the object, not world forward)
        rb.AddRelativeForce(Vector3.forward * 10f, ForceMode.Impulse);
        
        // Torque (rotation force)
        rb.AddTorque(Vector3.up * 10f, ForceMode.Impulse);
        
        // Move kinematic rigidbody
        rb.MovePosition(rb.position + Vector3.forward * Time.fixedDeltaTime);
        rb.MoveRotation(rb.rotation * Quaternion.Euler(0f, 10f * Time.fixedDeltaTime, 0f));
    }
    
    void RayCastExamples()
    {
        // Simple raycast
        if (Physics.Raycast(transform.position, Vector3.down, out RaycastHit hit, 10f))
        {
            Debug.Log($"Hit {hit.collider.name} at {hit.point}");
            Debug.Log($"Normal: {hit.normal}");
            Debug.Log($"Distance: {hit.distance}");
            Debug.DrawLine(transform.position, hit.point, Color.red);
        }
        
        // Ray from camera (for mouse picking)
        Ray ray = Camera.main.ScreenPointToRay(Input.mousePosition);
        if (Physics.Raycast(ray, out RaycastHit mouseHit, 100f))
        {
            Debug.Log($"Mouse clicked on: {mouseHit.collider.name}");
        }
        
        // SphereCast (cast a sphere, good for character ground detection)
        if (Physics.SphereCast(transform.position, 0.4f, Vector3.down, out RaycastHit sphereHit, 1f, LayerMask.GetMask("Ground")))
        {
            isGrounded = true;
            groundNormal = sphereHit.normal;
        }
        
        // CapsuleCast (for character hit detection)
        Vector3 top = transform.position + Vector3.up * 1.5f;
        Vector3 bottom = transform.position + Vector3.up * 0.5f;
        if (Physics.CapsuleCast(top, bottom, 0.4f, Vector3.forward, out RaycastHit capsuleHit, 2f))
        {
            Debug.Log($"Character hit: {capsuleHit.collider.name}");
        }
        
        // BoxCast
        if (Physics.BoxCast(transform.position, new Vector3(0.5f, 0.5f, 0.5f), Vector3.forward, out RaycastHit boxHit, transform.rotation, 5f))
        {
            Debug.Log($"Box hit: {boxHit.collider.name}");
        }
        
        // NonAlloc multiple hits
        int count = Physics.RaycastNonAlloc(transform.position, Vector3.forward, rayBuffer, 20f);
        for (int i = 0; i < count; i++)
            Debug.Log($"Hit {i}: {rayBuffer[i].collider.name}");
    }
    
    bool isGrounded;
    Vector3 groundNormal;
    
    void OverlapExamples()
    {
        // Check if any colliders overlap a sphere
        int count = Physics.OverlapSphereNonAlloc(transform.position, 5f, overlapBuffer);
        for (int i = 0; i < count; i++)
        {
            if (overlapBuffer[i].TryGetComponent<IDamageable>(out var target))
                target.TakeDamage(10f);
        }
        
        // Overlap box (useful for melee hit detection)
        int boxCount = Physics.OverlapBoxNonAlloc(
            transform.position + transform.forward,
            new Vector3(0.5f, 1f, 1f),
            overlapBuffer,
            transform.rotation
        );
    }
    
    void OnCollisionEnter(Collision collision)
    {
        // Get all contact points
        int contactCount = collision.contactCount;
        for (int i = 0; i < contactCount; i++)
        {
            ContactPoint contact = collision.GetContact(i);
            Debug.Log($"Contact: {contact.point}, normal: {contact.normal}");
        }
        
        float impactForce = collision.impulse.magnitude;
        Debug.Log($"Impact force: {impactForce}");
    }
}
```

---

## 24.4 — 3D Enemy AI with NavMesh

```csharp
using UnityEngine.AI;

[RequireComponent(typeof(NavMeshAgent))]
public class NavMeshEnemy : MonoBehaviour
{
    public enum State { Patrol, Chase, Attack, Investigate }
    
    [Header("Detection")]
    [SerializeField] private float sightRange = 15f;
    [SerializeField] private float attackRange = 2f;
    [SerializeField, Range(0f, 180f)] private float fieldOfView = 120f;
    [SerializeField] private LayerMask obstacleMask;
    
    [Header("Patrol")]
    [SerializeField] private Transform[] patrolPoints;
    [SerializeField] private float patrolWaitTime = 2f;
    
    private NavMeshAgent agent;
    private Animator anim;
    private Transform player;
    private State state = State.Patrol;
    
    private int currentPatrolIndex;
    private float waitTimer;
    private Vector3 lastKnownPlayerPos;
    
    private static readonly int HashSpeed   = Animator.StringToHash("Speed");
    private static readonly int HashAttack  = Animator.StringToHash("Attack");
    
    void Awake()
    {
        agent = GetComponent<NavMeshAgent>();
        anim = GetComponent<Animator>();
    }
    
    void Start()
    {
        player = FindFirstObjectByType<ThirdPersonController>()?.transform;
        if (patrolPoints.Length > 0)
            agent.SetDestination(patrolPoints[0].position);
    }
    
    void Update()
    {
        bool canSeePlayer = CanSeePlayer();
        
        // State transitions
        switch (state)
        {
            case State.Patrol:
                if (canSeePlayer) TransitionTo(State.Chase);
                break;
            case State.Chase:
                if (!canSeePlayer)
                {
                    lastKnownPlayerPos = player.position;
                    TransitionTo(State.Investigate);
                }
                else if (Vector3.Distance(transform.position, player.position) <= attackRange)
                    TransitionTo(State.Attack);
                break;
            case State.Attack:
                if (Vector3.Distance(transform.position, player.position) > attackRange)
                    TransitionTo(State.Chase);
                break;
            case State.Investigate:
                if (canSeePlayer) TransitionTo(State.Chase);
                else if (!agent.pathPending && agent.remainingDistance < 0.5f)
                    TransitionTo(State.Patrol);
                break;
        }
        
        // State execution
        switch (state)
        {
            case State.Patrol:    ExecutePatrol(); break;
            case State.Chase:     ExecuteChase(); break;
            case State.Attack:    ExecuteAttack(); break;
            case State.Investigate: ExecuteInvestigate(); break;
        }
        
        // Update animation
        anim.SetFloat(HashSpeed, agent.velocity.magnitude);
    }
    
    bool CanSeePlayer()
    {
        if (player == null) return false;
        
        Vector3 toPlayer = player.position - transform.position;
        float dist = toPlayer.magnitude;
        
        if (dist > sightRange) return false;
        
        // Field of view check
        float angle = Vector3.Angle(transform.forward, toPlayer);
        if (angle > fieldOfView * 0.5f) return false;
        
        // Line of sight (no obstacles)
        return !Physics.Raycast(transform.position + Vector3.up, toPlayer.normalized, dist, obstacleMask);
    }
    
    void ExecutePatrol()
    {
        if (!agent.pathPending && agent.remainingDistance < 0.5f)
        {
            waitTimer += Time.deltaTime;
            if (waitTimer >= patrolWaitTime)
            {
                waitTimer = 0f;
                currentPatrolIndex = (currentPatrolIndex + 1) % patrolPoints.Length;
                agent.SetDestination(patrolPoints[currentPatrolIndex].position);
            }
        }
    }
    
    void ExecuteChase()
    {
        agent.SetDestination(player.position);
    }
    
    void ExecuteAttack()
    {
        agent.ResetPath();
        transform.LookAt(player.position);
        anim.SetTrigger(HashAttack);
    }
    
    void ExecuteInvestigate()
    {
        agent.SetDestination(lastKnownPlayerPos);
    }
    
    void TransitionTo(State newState)
    {
        state = newState;
        if (newState == State.Attack) agent.ResetPath();
    }
    
    void OnDrawGizmosSelected()
    {
        // Sight range sphere
        Gizmos.color = new Color(1f, 1f, 0f, 0.3f);
        Gizmos.DrawSphere(transform.position, sightRange);
        
        // Attack range sphere
        Gizmos.color = new Color(1f, 0f, 0f, 0.3f);
        Gizmos.DrawSphere(transform.position, attackRange);
        
        // FOV lines
        Gizmos.color = Color.yellow;
        Quaternion leftFov = Quaternion.Euler(0f, -fieldOfView * 0.5f, 0f);
        Quaternion rightFov = Quaternion.Euler(0f, fieldOfView * 0.5f, 0f);
        Gizmos.DrawRay(transform.position, leftFov * transform.forward * sightRange);
        Gizmos.DrawRay(transform.position, rightFov * transform.forward * sightRange);
    }
}
```

---

## 24.5 — 3D Camera (Orbit / Third Person)

```csharp
public class OrbitCamera : MonoBehaviour
{
    [SerializeField] private Transform target;
    [SerializeField, Range(1f, 20f)] private float distance = 5f;
    [SerializeField, Range(0f, 30f)] private float mouseSensitivity = 5f;
    [SerializeField] private Vector2 pitchMinMax = new Vector2(-30f, 70f);
    [SerializeField, Range(0.01f, 1f)] private float cameraSmoothing = 0.1f;
    [SerializeField] private LayerMask collisionMask;
    
    private float yaw;
    private float pitch;
    private Vector3 smoothedPos;
    private Quaternion smoothedRot;
    
    void Start()
    {
        Vector3 angles = transform.eulerAngles;
        yaw = angles.y;
        pitch = angles.x;
        Cursor.lockState = CursorLockMode.Locked;
    }
    
    void LateUpdate()
    {
        yaw   += Input.GetAxis("Mouse X") * mouseSensitivity;
        pitch -= Input.GetAxis("Mouse Y") * mouseSensitivity;
        pitch  = Mathf.Clamp(pitch, pitchMinMax.x, pitchMinMax.y);
        
        Quaternion targetRot = Quaternion.Euler(pitch, yaw, 0f);
        
        // Camera collision — pull camera closer if something is in the way
        float actualDistance = distance;
        Vector3 desiredOffset = targetRot * Vector3.back * distance;
        
        if (Physics.SphereCast(target.position, 0.2f, desiredOffset.normalized, out RaycastHit hit, distance, collisionMask))
            actualDistance = Mathf.Max(1f, hit.distance - 0.2f);
        
        Vector3 targetPos = target.position + targetRot * Vector3.back * actualDistance;
        
        // Smooth
        transform.position = Vector3.SmoothDamp(transform.position, targetPos, ref smoothedPos, cameraSmoothing);
        transform.rotation = Quaternion.Slerp(transform.rotation, targetRot, Time.deltaTime / cameraSmoothing);
    }
}
```

---

## 24.6 — 3D Shooting with Raycasts (Hitscan)

```csharp
public class HitscanGun3D : MonoBehaviour
{
    [Header("Stats")]
    [SerializeField] private float damage = 35f;
    [SerializeField] private float fireRate = 0.15f;
    [SerializeField] private float range = 100f;
    [SerializeField] private int magazineSize = 30;
    
    [Header("References")]
    [SerializeField] private Transform muzzle;
    [SerializeField] private Camera playerCamera;
    [SerializeField] private LayerMask hitMask;
    [SerializeField] private GameObject bulletHolePrefab;
    [SerializeField] private ParticleSystem muzzleFlash;
    
    private float nextFireTime;
    private int currentAmmo;
    private bool isReloading;
    
    void Update()
    {
        if (Input.GetButton("Fire1") && CanFire()) Fire();
        if (Input.GetKeyDown(KeyCode.R) && !isReloading) StartCoroutine(Reload());
    }
    
    bool CanFire() => !isReloading && currentAmmo > 0 && Time.time >= nextFireTime;
    
    void Fire()
    {
        nextFireTime = Time.time + fireRate;
        currentAmmo--;
        
        // Effects
        muzzleFlash?.Play();
        
        // Hitscan — ray from camera center (crosshair)
        Ray ray = playerCamera.ViewportPointToRay(new Vector3(0.5f, 0.5f, 0f)); // center of screen
        
        if (Physics.Raycast(ray, out RaycastHit hit, range, hitMask))
        {
            // Damage
            if (hit.collider.TryGetComponent<IDamageable>(out IDamageable target))
                target.TakeDamage(damage);
            
            // Hit effect — spawn decal aligned to surface normal
            if (bulletHolePrefab != null)
            {
                Quaternion decalRot = Quaternion.LookRotation(-hit.normal); // face away from surface
                Instantiate(bulletHolePrefab, hit.point + hit.normal * 0.01f, decalRot);
            }
            
            // Debug visualization
            Debug.DrawLine(muzzle.position, hit.point, Color.yellow, 0.1f);
        }
        else
        {
            // Missed — draw ray to max range
            Debug.DrawRay(muzzle.position, ray.direction * range, Color.cyan, 0.1f);
        }
    }
    
    IEnumerator Reload()
    {
        isReloading = true;
        yield return new WaitForSeconds(1.5f);
        currentAmmo = magazineSize;
        isReloading = false;
    }
}
```

---

## 📝 Summary — 3D Quick Reference

| System | Key API | Notes |
| :--- | :--- | :--- |
| Rotation | `Quaternion.Slerp`, `RotateTowards`, `LookRotation` | Never set .x/.y/.z/.w directly |
| Movement | `CharacterController.Move()` or `Rigidbody.linearVelocity` | CC handles collision, Rb handles physics |
| Raycast | `Physics.Raycast(origin, dir, out hit, dist, mask)` | 3D — no "2D" suffix |
| Sphere cast | `Physics.SphereCast(origin, radius, dir, out hit, dist)` | Good for character ground check |
| NavMesh | `agent.SetDestination(pos)` | AI pathfinding |
| Overlap sphere | `Physics.OverlapSphereNonAlloc(center, r, buffer)` | NonAlloc is mandatory |
| Collision enter | `OnCollisionEnter(Collision)` | No 2D suffix! |
| Look at | `transform.LookAt(target.position)` | Instant — use Slerp for smooth |
| Euler → world | `transform.forward`, `transform.right`, `transform.up` | Local axes in world space |

**Previous:** [[23 — C# Patterns for 2D Unity]] | **Next:** [[25 — Math Utilities in C#]]
