# 23 — C# Patterns for 2D Unity
### 🟡 Block E — 2D & 3D Unity-Specific C# Patterns

> [!IMPORTANT]
> This is your **practical coding reference for 2D games**. Every system you build in a 2D Unity game — player movement, physics, camera, tilemaps — uses the APIs and patterns here. Read this when you start your first 2D project.

---

## 23.1 — Vector2 vs Vector3 in 2D

```csharp
public class Player2D : MonoBehaviour
{
    // transform.position is ALWAYS Vector3 — even in 2D Unity games
    Vector3 pos3D = transform.position;    // x, y, z (z usually 0 in 2D)
    
    // Rigidbody2D uses Vector2
    Vector2 vel2D = rb.linearVelocity;    // x, y only
    
    // Casting between them
    Vector2 as2D = transform.position;    // implicit: drops Z
    Vector3 as3D = (Vector3)vel2D;        // implicit: adds Z=0
    
    // When to use Vector2 vs Vector3 in 2D:
    // Vector2: Rigidbody2D velocity, force direction, 2D distance
    // Vector3: transform.position, Instantiate position, camera position
}
```

---

## 23.2 — Complete 2D Platformer Controller

```csharp
[RequireComponent(typeof(Rigidbody2D))]
[RequireComponent(typeof(Animator))]
[RequireComponent(typeof(SpriteRenderer))]
public class PlatformerController : MonoBehaviour
{
    [Header("Movement")]
    [SerializeField, Range(1f, 20f)] private float moveSpeed = 8f;
    [SerializeField, Range(1f, 30f)] private float jumpForce = 16f;
    [SerializeField, Range(0f, 1f)]  private float coyoteTime = 0.15f;
    [SerializeField, Range(0f, 0.5f)] private float jumpBufferTime = 0.1f;
    
    [Header("Ground Detection")]
    [SerializeField] private Transform groundCheckPoint;
    [SerializeField, Range(0.05f, 0.5f)] private float groundCheckRadius = 0.15f;
    [SerializeField] private LayerMask groundLayer;
    
    [Header("Physics Tweaks")]
    [SerializeField, Range(1f, 10f)] private float fallGravityMultiplier = 2.5f;
    [SerializeField, Range(1f, 5f)]  private float lowJumpMultiplier = 2f;
    
    // Cached components
    private Rigidbody2D rb;
    private Animator anim;
    private SpriteRenderer sr;
    
    // State
    private bool isGrounded;
    private float coyoteTimeCounter;
    private float jumpBufferCounter;
    private float horizontalInput;
    private bool jumpPressed;
    
    // Animator hash cache
    private static readonly int HashSpeedX   = Animator.StringToHash("SpeedX");
    private static readonly int HashGrounded = Animator.StringToHash("IsGrounded");
    private static readonly int HashJump     = Animator.StringToHash("Jump");
    
    void Awake()
    {
        rb   = GetComponent<Rigidbody2D>();
        anim = GetComponent<Animator>();
        sr   = GetComponent<SpriteRenderer>();
    }
    
    void Update()
    {
        // Input — gather all input in Update
        horizontalInput = Input.GetAxisRaw("Horizontal");
        jumpPressed = Input.GetButtonDown("Jump");
        
        // Ground check
        isGrounded = Physics2D.OverlapCircle(groundCheckPoint.position, groundCheckRadius, groundLayer);
        
        // Coyote time: allow jumping a few frames after walking off a ledge
        if (isGrounded) coyoteTimeCounter = coyoteTime;
        else coyoteTimeCounter -= Time.deltaTime;
        
        // Jump buffer: allow jump input slightly before landing
        if (jumpPressed) jumpBufferCounter = jumpBufferTime;
        else jumpBufferCounter -= Time.deltaTime;
        
        // Attempt jump
        if (jumpBufferCounter > 0f && coyoteTimeCounter > 0f)
        {
            rb.linearVelocity = new Vector2(rb.linearVelocity.x, jumpForce);
            jumpBufferCounter = 0f;
            coyoteTimeCounter = 0f;
            anim.SetTrigger(HashJump);
        }
        
        // Cut jump height if button released early
        if (Input.GetButtonUp("Jump") && rb.linearVelocity.y > 0f)
            rb.linearVelocity = new Vector2(rb.linearVelocity.x, rb.linearVelocity.y * 0.5f);
        
        // Sprite flip
        if (horizontalInput > 0.1f) sr.flipX = false;
        else if (horizontalInput < -0.1f) sr.flipX = true;
        
        // Animation
        anim.SetFloat(HashSpeedX, Mathf.Abs(rb.linearVelocity.x));
        anim.SetBool(HashGrounded, isGrounded);
    }
    
    void FixedUpdate()
    {
        // Horizontal movement — preserve Y velocity for gravity
        rb.linearVelocity = new Vector2(horizontalInput * moveSpeed, rb.linearVelocity.y);
        
        // Better jump feel — increase gravity when falling
        if (rb.linearVelocity.y < 0f)
            rb.gravityScale = fallGravityMultiplier;
        else if (rb.linearVelocity.y > 0f && !Input.GetButton("Jump"))
            rb.gravityScale = lowJumpMultiplier;
        else
            rb.gravityScale = 1f;
    }
    
    void OnDrawGizmosSelected()
    {
        // Visualize ground check in Scene view
        if (groundCheckPoint != null)
        {
            Gizmos.color = isGrounded ? Color.green : Color.red;
            Gizmos.DrawWireSphere(groundCheckPoint.position, groundCheckRadius);
        }
    }
}
```

---

## 23.3 — 2D Physics — All the APIs You Need

```csharp
public class PhysicsReference2D : MonoBehaviour
{
    private Rigidbody2D rb;
    
    // Pre-allocated buffers (avoid allocation every call)
    private static readonly Collider2D[] overlapBuffer = new Collider2D[32];
    private static readonly RaycastHit2D[] raycastBuffer = new RaycastHit2D[16];
    private static readonly ContactPoint2D[] contactBuffer = new ContactPoint2D[8];
    
    // ===== Movement =====
    void MoveExamples()
    {
        // Set velocity directly (teleports instantly)
        rb.linearVelocity = new Vector2(5f, rb.linearVelocity.y);
        
        // Add a one-time impulse (jump, knockback)
        rb.AddForce(Vector2.up * 500f, ForceMode2D.Impulse);
        
        // Add continuous force (rocket, wind)
        rb.AddForce(Vector2.right * 10f, ForceMode2D.Force); // called in FixedUpdate
        
        // Move position (for kinematic bodies or animation)
        rb.MovePosition(rb.position + Vector2.right * Time.fixedDeltaTime * 5f);
        
        // Rotate
        rb.AddTorque(10f, ForceMode2D.Impulse); // spin
        rb.angularVelocity = 0f; // stop spinning instantly
        
        // Freeze rotation (prevent physics spinning)
        rb.constraints = RigidbodyConstraints2D.FreezeRotation;
    }
    
    // ===== Raycasts =====
    void RaycastExamples()
    {
        // Single raycast
        RaycastHit2D hit = Physics2D.Raycast(
            transform.position,      // origin
            Vector2.down,            // direction
            2f,                      // max distance
            LayerMask.GetMask("Ground") // layer mask
        );
        
        if (hit.collider != null)
        {
            Debug.Log($"Hit: {hit.collider.name} at {hit.point}");
            Debug.Log($"Normal: {hit.normal}");
            Debug.Log($"Distance: {hit.distance}");
        }
        
        // NonAlloc ray (multiple hits)
        int count = Physics2D.RaycastNonAlloc(transform.position, Vector2.right, raycastBuffer, 10f);
        for (int i = 0; i < count; i++)
            Debug.Log($"Hit {i}: {raycastBuffer[i].collider.name}");
        
        // BoxCast (cast a box shape)
        RaycastHit2D boxHit = Physics2D.BoxCast(
            transform.position,
            new Vector2(1f, 0.5f),  // box size
            0f,                     // angle
            Vector2.down,           // direction
            0.5f                    // distance
        );
        
        // CircleCast
        RaycastHit2D circleHit = Physics2D.CircleCast(transform.position, 0.5f, Vector2.right, 5f);
    }
    
    // ===== Overlap Checks =====
    void OverlapExamples()
    {
        // Single overlap (bool check)
        bool isGrounded = Physics2D.OverlapCircle(transform.position, 0.2f, LayerMask.GetMask("Ground"));
        
        // Single overlap object
        Collider2D hit = Physics2D.OverlapPoint(transform.position);
        
        // NonAlloc circle overlap
        int count = Physics2D.OverlapCircleNonAlloc(transform.position, 3f, overlapBuffer);
        for (int i = 0; i < count; i++)
        {
            if (overlapBuffer[i].TryGetComponent<IDamageable>(out var t))
                t.TakeDamage(10f);
        }
        
        // NonAlloc box overlap
        int boxCount = Physics2D.OverlapBoxNonAlloc(
            transform.position,
            new Vector2(3f, 1f),  // size
            0f,                   // angle
            overlapBuffer
        );
    }
    
    // ===== Collision Callbacks =====
    void OnCollisionEnter2D(Collision2D collision)
    {
        // Get contact points
        int contacts = collision.GetContacts(contactBuffer);
        for (int i = 0; i < contacts; i++)
        {
            Vector2 point = contactBuffer[i].point;
            Vector2 normal = contactBuffer[i].normal;
            Debug.Log($"Contact at {point}, normal: {normal}");
        }
        
        // Relative velocity (impact speed)
        float impactSpeed = collision.relativeVelocity.magnitude;
        
        // CompareTag (no allocation — always use this over .tag ==)
        if (collision.gameObject.CompareTag("Ground"))
            isGrounded = true;
    }
}
```

---

## 23.4 — 2D Camera Follow

```csharp
public class CameraFollow2D : MonoBehaviour
{
    [SerializeField] private Transform target;
    [SerializeField, Range(1f, 20f)] private float smoothSpeed = 10f;
    [SerializeField] private Vector2 offset = new Vector2(0f, 2f);
    
    [Header("Camera Bounds")]
    [SerializeField] private bool useBounds = true;
    [SerializeField] private Vector2 minBounds;
    [SerializeField] private Vector2 maxBounds;
    
    void LateUpdate()  // LateUpdate — after player has moved this frame
    {
        if (target == null) return;
        
        Vector3 targetPos = new Vector3(
            target.position.x + offset.x,
            target.position.y + offset.y,
            transform.position.z // preserve camera Z depth
        );
        
        // Clamp to world bounds
        if (useBounds)
        {
            targetPos.x = Mathf.Clamp(targetPos.x, minBounds.x, maxBounds.x);
            targetPos.y = Mathf.Clamp(targetPos.y, minBounds.y, maxBounds.y);
        }
        
        // Smooth follow
        transform.position = Vector3.Lerp(transform.position, targetPos, smoothSpeed * Time.deltaTime);
    }
    
    void OnDrawGizmosSelected()
    {
        if (!useBounds) return;
        Gizmos.color = Color.cyan;
        Vector2 size = maxBounds - minBounds;
        Vector2 center = (maxBounds + minBounds) * 0.5f;
        Gizmos.DrawWireCube(new Vector3(center.x, center.y, 0f), new Vector3(size.x, size.y, 0f));
    }
}
```

---

## 23.5 — 2D Enemy AI — Patrol + Chase

```csharp
public class Patrol2DEnemy : MonoBehaviour
{
    public enum State { Patrol, Chase, Attack, Return }
    
    [Header("Patrol")]
    [SerializeField] private Transform[] waypoints;
    [SerializeField] private float waitTime = 1f;
    
    [Header("Detection")]
    [SerializeField] private float detectionRange = 6f;
    [SerializeField] private float attackRange = 1.2f;
    [SerializeField] private LayerMask playerLayer;
    
    [Header("Stats")]
    [SerializeField] private float moveSpeed = 3f;
    [SerializeField] private float chaseSpeed = 5f;
    
    private State currentState = State.Patrol;
    private Rigidbody2D rb;
    private SpriteRenderer sr;
    private Transform player;
    private int waypointIndex;
    private float waitTimer;
    private bool isWaiting;
    
    void Awake()
    {
        rb = GetComponent<Rigidbody2D>();
        sr = GetComponent<SpriteRenderer>();
        player = FindFirstObjectByType<PlayerController2D>()?.transform;
    }
    
    void Update()
    {
        if (player == null) return;
        
        float distToPlayer = Vector2.Distance(transform.position, player.position);
        
        // State transitions
        switch (currentState)
        {
            case State.Patrol:
                if (distToPlayer < detectionRange) TransitionTo(State.Chase);
                break;
            case State.Chase:
                if (distToPlayer <= attackRange) TransitionTo(State.Attack);
                else if (distToPlayer > detectionRange * 1.5f) TransitionTo(State.Return);
                break;
            case State.Attack:
                if (distToPlayer > attackRange) TransitionTo(State.Chase);
                break;
        }
    }
    
    void FixedUpdate()
    {
        switch (currentState)
        {
            case State.Patrol:   HandlePatrol(); break;
            case State.Chase:    HandleChase(); break;
            case State.Attack:   HandleAttack(); break;
            case State.Return:   HandleReturn(); break;
        }
    }
    
    void HandlePatrol()
    {
        if (isWaiting)
        {
            waitTimer -= Time.fixedDeltaTime;
            if (waitTimer <= 0f) isWaiting = false;
            return;
        }
        
        Transform target = waypoints[waypointIndex];
        Vector2 dir = ((Vector2)target.position - rb.position).normalized;
        
        sr.flipX = dir.x < 0f; // face direction of movement
        rb.linearVelocity = dir * moveSpeed;
        
        if (Vector2.Distance(rb.position, target.position) < 0.1f)
        {
            waypointIndex = (waypointIndex + 1) % waypoints.Length;
            waitTimer = waitTime;
            isWaiting = true;
            rb.linearVelocity = Vector2.zero;
        }
    }
    
    void HandleChase()
    {
        Vector2 dir = ((Vector2)player.position - rb.position).normalized;
        sr.flipX = dir.x < 0f;
        rb.linearVelocity = dir * chaseSpeed;
    }
    
    void HandleAttack()
    {
        rb.linearVelocity = Vector2.zero;
        // Attack logic here
    }
    
    void HandleReturn()
    {
        Vector2 startPos = waypoints[0].position;
        Vector2 dir = (startPos - rb.position).normalized;
        rb.linearVelocity = dir * moveSpeed;
        
        if (Vector2.Distance(rb.position, startPos) < 0.1f)
            TransitionTo(State.Patrol);
    }
    
    void TransitionTo(State newState)
    {
        currentState = newState;
        if (newState == State.Return) rb.linearVelocity = Vector2.zero;
    }
}
```

---

## 23.6 — 2D Shooting System

```csharp
public class WeaponController2D : MonoBehaviour
{
    [SerializeField] private GameObject bulletPrefab;
    [SerializeField] private Transform muzzle;
    [SerializeField] private float fireRate = 0.2f;
    [SerializeField] private int magazineSize = 15;
    [SerializeField] private float reloadTime = 1.5f;
    
    private float nextFireTime;
    private int currentAmmo;
    private bool isReloading;
    
    // Events for UI
    public event System.Action<int, int> OnAmmoChanged; // current, max
    public event System.Action OnReloadStarted;
    public event System.Action OnReloadFinished;
    
    void Start()
    {
        currentAmmo = magazineSize;
        OnAmmoChanged?.Invoke(currentAmmo, magazineSize);
    }
    
    void Update()
    {
        // Point weapon toward mouse
        Vector2 mouseWorld = Camera.main.ScreenToWorldPoint(Input.mousePosition);
        Vector2 dir = mouseWorld - (Vector2)transform.position;
        float angle = Mathf.Atan2(dir.y, dir.x) * Mathf.Rad2Deg;
        transform.rotation = Quaternion.Euler(0f, 0f, angle);
        
        // Fire
        if (Input.GetButton("Fire1") && CanFire())
            Fire();
        
        // Reload
        if (Input.GetKeyDown(KeyCode.R) && !isReloading && currentAmmo < magazineSize)
            StartCoroutine(Reload());
    }
    
    bool CanFire() => !isReloading && currentAmmo > 0 && Time.time >= nextFireTime;
    
    void Fire()
    {
        nextFireTime = Time.time + fireRate;
        currentAmmo--;
        OnAmmoChanged?.Invoke(currentAmmo, magazineSize);
        
        // Instantiate bullet at muzzle (use pool in production!)
        Instantiate(bulletPrefab, muzzle.position, muzzle.rotation);
        
        // Muzzle flash, recoil, etc.
        
        if (currentAmmo <= 0)
            StartCoroutine(Reload());
    }
    
    IEnumerator Reload()
    {
        isReloading = true;
        OnReloadStarted?.Invoke();
        
        yield return new WaitForSeconds(reloadTime);
        
        currentAmmo = magazineSize;
        isReloading = false;
        OnAmmoChanged?.Invoke(currentAmmo, magazineSize);
        OnReloadFinished?.Invoke();
    }
}
```

---

## 23.7 — 2D Tilemap Interaction

```csharp
using UnityEngine.Tilemaps;

public class TilemapInteraction : MonoBehaviour
{
    [SerializeField] private Tilemap foregroundTilemap;
    [SerializeField] private TileBase destructibleTile;
    
    // Check if a position has a tile
    public bool HasTileAt(Vector2 worldPos)
    {
        Vector3Int cellPos = foregroundTilemap.WorldToCell(worldPos);
        return foregroundTilemap.HasTile(cellPos);
    }
    
    // Remove a tile (mining, destruction)
    public void DestroyTileAt(Vector2 worldPos)
    {
        Vector3Int cellPos = foregroundTilemap.WorldToCell(worldPos);
        if (foregroundTilemap.GetTile(cellPos) == destructibleTile)
        {
            foregroundTilemap.SetTile(cellPos, null); // remove tile
            SpawnBreakEffect(worldPos);
        }
    }
    
    // Get tile type at position
    public TileBase GetTileAt(Vector2 worldPos)
    {
        Vector3Int cellPos = foregroundTilemap.WorldToCell(worldPos);
        return foregroundTilemap.GetTile(cellPos);
    }
    
    // Scan neighbors in 4 directions
    static readonly Vector3Int[] neighbors = {
        Vector3Int.up, Vector3Int.down, Vector3Int.left, Vector3Int.right
    };
    
    public int CountSolidNeighbors(Vector3Int cell)
    {
        int count = 0;
        foreach (var offset in neighbors)
            if (foregroundTilemap.HasTile(cell + offset)) count++;
        return count;
    }
}
```

---

## 📝 Summary — 2D Quick Reference

| System | Key API | Notes |
| :--- | :--- | :--- |
| Movement | `rb.linearVelocity`, `rb.AddForce(ForceMode2D.Impulse)` | Use FixedUpdate for forces |
| Ground check | `Physics2D.OverlapCircle()` | Use NonAlloc in tight loops |
| Raycast | `Physics2D.Raycast()` | Use NonAlloc for multiple |
| Collision enter | `OnCollisionEnter2D(Collision2D)` | 2D suffix! |
| Trigger enter | `OnTriggerEnter2D(Collider2D)` | 2D suffix! |
| Gravity adjust | `rb.gravityScale` | Tweak in FixedUpdate |
| Sprite flip | `spriteRenderer.flipX` | For 2D direction |
| Mouse position | `Camera.main.ScreenToWorldPoint(Input.mousePosition)` | Cast to Vector2 |

**Previous:** [[22 — Operator Overloading]] | **Next:** [[24 — C# Patterns for 3D Unity]]
