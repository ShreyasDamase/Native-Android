# 00B — MonoBehaviour & Script Lifecycle
### 🔶 Block 0 — Unity Engine Fundamentals

> [!IMPORTANT]
> This is the most important file in the entire vault for a beginner. Unity does NOT run your code the way a console app does (`Main()` → lines run top to bottom → done). Instead, Unity **calls specific methods in your script at specific moments**. These moments are called the **lifecycle**. If you don't understand this, nothing you write will work correctly.

---

## 00B.1 — What is MonoBehaviour?

`MonoBehaviour` is the **base class** that every Unity script inherits from. When your class inherits from `MonoBehaviour`, Unity recognizes it as a Component that can be attached to a GameObject.

```csharp
// This is the MINIMUM valid Unity script
// It inherits from MonoBehaviour — that's what makes it a Unity component
public class MyScript : MonoBehaviour
{
    // Unity will call these special methods automatically
    // You NEVER call them manually — Unity does it for you
}
```

Without `MonoBehaviour`:
```csharp
// This is a regular C# class — NOT a Unity component
// You cannot attach this to a GameObject in the Inspector
// It cannot have Update(), Start(), Awake(), etc. called by Unity
public class RegularClass
{
    public void DoSomething() { }
}

// You CAN still use regular classes in Unity — just as helper classes
// that are created and managed BY your MonoBehaviour scripts
```

---

## 00B.2 — The Complete Lifecycle — Every Method Unity Calls

This is the **complete diagram** of when Unity calls each method:

```
GAME STARTS / OBJECT CREATED
         │
         ▼
    ┌─────────────────────────────────────────────────────────────────────────┐
    │  INITIALIZATION PHASE (runs once, when object first becomes active)     │
    │                                                                         │
    │   Awake()          ← FIRST. Called even if script component disabled.   │
    │      │                Set up YOUR OWN data. Don't touch other scripts.  │
    │      │                                                                  │
    │   OnEnable()       ← Called when object/script becomes active/enabled.  │
    │      │                Also called the first time after Awake.           │
    │      │                                                                  │
    │   Start()          ← Called before first frame. Script must be enabled. │
    │                       Safe to reference other scripts set up in Awake.  │
    └─────────────────────────────────────────────────────────────────────────┘
         │
         ▼
    ┌─────────────────────────────────────────────────────────────────────────┐
    │  GAME LOOP (runs every frame / every physics step — forever until done) │
    │                                                                         │
    │   FixedUpdate()    ← Physics step. Fixed timestep (~50x/sec default).   │
    │      │                Use for Rigidbody forces, physics movement.        │
    │      │                NOT tied to frame rate.                           │
    │      │                                                                  │
    │   Update()         ← Every frame. Rate = frame rate (30, 60, 144 FPS). │
    │      │                Input reading, game logic, most gameplay code.    │
    │      │                                                                  │
    │   LateUpdate()     ← After ALL Update()s have run this frame.          │
    │                       Camera follow, look-at logic, final state reads.  │
    └─────────────────────────────────────────────────────────────────────────┘
         │
         ▼
    ┌─────────────────────────────────────────────────────────────────────────┐
    │  EVENTS (called when specific things happen — not every frame)          │
    │                                                                         │
    │   OnTriggerEnter/Stay/Exit (2D variants have "2D" suffix)               │
    │   OnCollisionEnter/Stay/Exit (2D variants have "2D" suffix)             │
    │   OnMouseDown / OnMouseEnter / OnMouseUp                                │
    │   OnBecameVisible / OnBecameInvisible                                   │
    └─────────────────────────────────────────────────────────────────────────┘
         │
         ▼
    ┌─────────────────────────────────────────────────────────────────────────┐
    │  TEARDOWN (runs when the object is destroyed or disabled)               │
    │                                                                         │
    │   OnDisable()      ← Called when script/object is disabled.            │
    │      │                Mirror of OnEnable — clean up subscriptions.     │
    │      │                                                                  │
    │   OnDestroy()      ← Called just before object is destroyed.           │
    │                       Final cleanup — unsubscribe events, release GPU.  │
    └─────────────────────────────────────────────────────────────────────────┘
         │
         ▼
    GAME ENDS → OnApplicationQuit() called on ALL objects
```

---

## 00B.3 — `Awake()` — The Constructor of Unity

**When:** Called once, immediately when the GameObject is created or the scene loads — even if the script component is **disabled**.
**Purpose:** Initialize YOUR OWN data. Cache components. Set up internal state.
**Rule:** Never reference other scripts in `Awake()` — they may not have run their `Awake()` yet.

```csharp
public class PlayerHealth : MonoBehaviour
{
    [SerializeField] private float maxHealth = 100f;
    
    private float currentHealth;
    private Rigidbody2D rb;
    private Animator animator;
    private SpriteRenderer sr;
    
    void Awake()
    {
        // ✅ CORRECT uses of Awake:
        
        // 1. Cache your own components (GetComponent is allowed — same object)
        rb = GetComponent<Rigidbody2D>();
        animator = GetComponent<Animator>();
        sr = GetComponent<SpriteRenderer>();
        
        // 2. Initialize your own data
        currentHealth = maxHealth;
        
        // 3. Set up internal state
        isAlive = true;
        
        // ❌ WRONG: Don't reference other scripts — they may not be ready
        // GameManager.Instance.RegisterPlayer(this); // GameManager.Awake might not have run!
    }
    
    private bool isAlive;
}
```

---

## 00B.4 — `Start()` — Post-Initialization Setup

**When:** Called once, before the **first Update()**, but AFTER all Awake() calls in the scene.
**Purpose:** Reference other scripts, subscribe to events, do setup that requires other scripts to be initialized.

```csharp
public class PlayerController : MonoBehaviour
{
    private PlayerHealth healthSystem;
    private Rigidbody2D rb;
    private GameManager gameManager;
    private UIManager uiManager;
    
    void Awake()
    {
        // Self-initialization only
        rb = GetComponent<Rigidbody2D>();
    }
    
    void Start()
    {
        // ✅ CORRECT uses of Start:
        
        // 1. Get components from OTHER objects (they've all run Awake by now)
        healthSystem = GetComponent<PlayerHealth>(); // same object is fine
        gameManager = FindFirstObjectByType<GameManager>(); // find in scene
        uiManager = FindFirstObjectByType<UIManager>();
        
        // 2. Subscribe to events
        healthSystem.OnHealthChanged += OnHealthChanged;
        gameManager.OnLevelComplete += HandleLevelComplete;
        
        // 3. Set initial UI state
        uiManager.SetMaxHealth(healthSystem.MaxHealth);
        uiManager.SetHealth(healthSystem.CurrentHealth);
        
        // 4. Register with managers
        gameManager.RegisterPlayer(this);
        
        Debug.Log("Player initialized!");
    }
    
    void OnHealthChanged(float newHealth) { }
    void HandleLevelComplete() { }
}
```

---

## 00B.5 — `Update()` — The Game Loop

**When:** Called every single frame. If your game runs at 60 FPS, `Update()` runs 60 times per second.
**Purpose:** Input reading, movement logic, state changes, timers, most gameplay code.
**Critical:** `Time.deltaTime` — ALWAYS multiply movement/timers by this. Explained below.

```csharp
public class PlayerController : MonoBehaviour
{
    [SerializeField] private float moveSpeed = 5f;
    [SerializeField] private float rotateSpeed = 90f;
    
    private Rigidbody2D rb;
    
    void Awake() => rb = GetComponent<Rigidbody2D>();
    
    void Update()
    {
        // ====== INPUT ======
        // Read input every frame — always in Update
        float horizontal = Input.GetAxisRaw("Horizontal"); // -1, 0, or 1
        float vertical = Input.GetAxisRaw("Vertical");
        bool jumpPressed = Input.GetButtonDown("Jump");    // true for ONE frame only
        bool fireHeld = Input.GetButton("Fire1");          // true every frame while held
        bool reloadTapped = Input.GetKeyDown(KeyCode.R);   // specific key, one frame
        
        // ====== TIMER ======
        // Time.deltaTime = seconds elapsed since last frame
        // At 60 FPS: deltaTime ≈ 0.0167 seconds
        // At 30 FPS: deltaTime ≈ 0.0333 seconds
        // Multiplying by deltaTime makes movement FRAME-RATE INDEPENDENT
        
        timer += Time.deltaTime; // timer counts UP at 1 second per real second, regardless of FPS
        
        if (timer >= cooldown)
        {
            timer = 0f;
            Fire();
        }
        
        // ====== NON-PHYSICS MOVEMENT ======
        // Direct transform movement — OK for non-physics objects
        transform.Translate(Vector2.right * horizontal * moveSpeed * Time.deltaTime);
        
        // ====== ROTATION ======
        transform.Rotate(0f, 0f, rotateSpeed * Time.deltaTime); // spin object
        
        // ====== INPUT-TRIGGERED EVENTS ======
        if (jumpPressed && isGrounded)
        {
            rb.AddForce(Vector2.up * jumpForce, ForceMode2D.Impulse);
        }
    }
    
    private float timer = 0f;
    private float cooldown = 0.5f;
    private bool isGrounded;
    private float jumpForce = 10f;
    
    void Fire() { }
}
```

### Why `Time.deltaTime` Matters

```csharp
// WITHOUT Time.deltaTime — speed depends on frame rate!
void Update()
{
    transform.position += Vector3.right * 5f;
    // At 60 FPS: moves 5 * 60 = 300 units/second
    // At 30 FPS: moves 5 * 30 = 150 units/second  ← HALF SPEED on slow machines!
}

// WITH Time.deltaTime — frame-rate independent!
void Update()
{
    transform.position += Vector3.right * 5f * Time.deltaTime;
    // At 60 FPS: moves 5 * (1/60) * 60 = 5 units/second
    // At 30 FPS: moves 5 * (1/30) * 30 = 5 units/second  ← SAME SPEED!
    // At 144 FPS: moves 5 * (1/144) * 144 = 5 units/second ← SAME SPEED!
}
```

---

## 00B.6 — `FixedUpdate()` — Physics-Safe Updates

**When:** Called at a **fixed time interval** (default: 50 times per second = every 0.02 seconds). NOT tied to frame rate.
**Purpose:** Anything that interacts with the physics engine. Rigidbody forces, velocity changes, physics movement.
**Rule:** If you're using `Rigidbody` or `Rigidbody2D`, put that code here.

```csharp
public class PhysicsPlayer : MonoBehaviour
{
    [SerializeField] private float moveSpeed = 5f;
    [SerializeField] private float jumpForce = 12f;
    
    private Rigidbody2D rb;
    private float horizontalInput;  // read in Update, used in FixedUpdate
    private bool jumpQueued;        // queued in Update, consumed in FixedUpdate
    
    void Awake() => rb = GetComponent<Rigidbody2D>();
    
    void Update()
    {
        // READ input in Update (correct — input polling is frame-based)
        horizontalInput = Input.GetAxisRaw("Horizontal");
        
        // QUEUE jump request — don't apply force here!
        if (Input.GetButtonDown("Jump"))
            jumpQueued = true;
    }
    
    void FixedUpdate()
    {
        // APPLY physics in FixedUpdate
        
        // Horizontal movement — preserve vertical velocity (gravity!)
        rb.linearVelocity = new Vector2(horizontalInput * moveSpeed, rb.linearVelocity.y);
        
        // Apply queued jump
        if (jumpQueued)
        {
            rb.AddForce(Vector2.up * jumpForce, ForceMode2D.Impulse);
            jumpQueued = false; // consume the request
        }
        
        // Note: Time.fixedDeltaTime is the FixedUpdate interval (usually 0.02)
        // For forces in FixedUpdate, you typically DON'T need to multiply by it
        // because ForceMode.Force already accounts for the fixed timestep
    }
}
```

### Update vs FixedUpdate — The Golden Rule

```
READ INPUT     → Update()
MOVE WITH PHYSICS (Rigidbody) → FixedUpdate()
MOVE WITHOUT PHYSICS (transform) → Update() × Time.deltaTime
CAMERA FOLLOW  → LateUpdate()
```

---

## 00B.7 — `LateUpdate()` — After Everything Moves

**When:** Called every frame, AFTER all `Update()` calls have completed.
**Purpose:** Camera follow (the player has already moved this frame), look-at targets, final adjustments that should react to the current frame's state.

```csharp
public class CameraFollow : MonoBehaviour
{
    [SerializeField] private Transform player;
    [SerializeField] private float smoothSpeed = 10f;
    [SerializeField] private Vector3 offset = new Vector3(0f, 2f, -10f);
    
    // LateUpdate — player has already moved this frame, so camera is never one frame behind
    void LateUpdate()
    {
        if (player == null) return;
        
        Vector3 targetPos = player.position + offset;
        transform.position = Vector3.Lerp(transform.position, targetPos, smoothSpeed * Time.deltaTime);
    }
}
```

---

## 00B.8 — `OnEnable()` and `OnDisable()` — Subscribe/Unsubscribe Pattern

**When:** `OnEnable()` when object is activated. `OnDisable()` when deactivated.
**Purpose:** Subscribe to events when active, unsubscribe when inactive. This is the **correct pattern** to avoid memory leaks.

```csharp
public class EnemyHealthBar : MonoBehaviour
{
    [SerializeField] private EnemyHealth enemyHealth;
    
    void OnEnable()
    {
        // Subscribe to events WHEN this object becomes active
        // This also runs on first activation (after Awake, before Start if script was enabled)
        enemyHealth.OnHealthChanged += UpdateHealthBar;
        enemyHealth.OnDied += HandleDeath;
    }
    
    void OnDisable()
    {
        // Unsubscribe when disabled — CRITICAL to prevent memory leaks and null reference errors
        // If you forget this, the event will try to call methods on a disabled/destroyed object
        enemyHealth.OnHealthChanged -= UpdateHealthBar;
        enemyHealth.OnDied -= HandleDeath;
    }
    
    void UpdateHealthBar(float newHealth) { /* update UI */ }
    void HandleDeath() { /* hide health bar */ }
}
```

---

## 00B.9 — `OnDestroy()` — Final Cleanup

**When:** Called just before the GameObject or script is destroyed.
**Purpose:** Final cleanup — cancel async tasks, release GPU resources, unregister from global managers.

```csharp
public class Player : MonoBehaviour
{
    private System.Threading.CancellationTokenSource cts;
    private Coroutine activeCoroutine;
    
    void Start()
    {
        cts = new System.Threading.CancellationTokenSource();
        // Start some async work...
    }
    
    void OnDestroy()
    {
        // Cancel any ongoing async tasks — prevents errors after destruction
        cts?.Cancel();
        cts?.Dispose();
        
        // Unsubscribe from any events (belt-and-suspenders with OnDisable)
        if (GameManager.Instance != null)
            GameManager.Instance.OnLevelComplete -= HandleLevelComplete;
        
        Debug.Log($"{name} destroyed and cleaned up.");
    }
    
    void HandleLevelComplete() { }
}
```

---

## 00B.10 — Physics Lifecycle Methods

These are called by the physics engine, not the main loop:

```csharp
public class Character : MonoBehaviour
{
    // ===== TRIGGER EVENTS (the other collider has "Is Trigger" checked) =====
    
    void OnTriggerEnter2D(Collider2D other)
    {
        // Called ONCE when another collider first enters this trigger
        // 'other' = the collider that entered
        if (other.CompareTag("Coin"))
        {
            CollectCoin(other.gameObject);
        }
    }
    
    void OnTriggerStay2D(Collider2D other)
    {
        // Called EVERY PHYSICS STEP while another collider overlaps this trigger
        // Use sparingly — runs a lot!
        if (other.CompareTag("HealZone"))
        {
            Heal(10f * Time.fixedDeltaTime);
        }
    }
    
    void OnTriggerExit2D(Collider2D other)
    {
        // Called ONCE when a collider leaves this trigger
        if (other.CompareTag("HealZone"))
        {
            Debug.Log("Left heal zone");
        }
    }
    
    // ===== COLLISION EVENTS (actual physical collision, not trigger) =====
    
    void OnCollisionEnter2D(Collision2D collision)
    {
        // Called ONCE when this object physically collides with another
        // 'collision' contains hit point, normal, relative velocity, etc.
        
        float impactSpeed = collision.relativeVelocity.magnitude;
        
        if (collision.gameObject.CompareTag("Ground"))
        {
            isGrounded = true;
        }
        
        if (impactSpeed > 10f)
        {
            TakeDamage(impactSpeed * 2f); // fall damage!
        }
    }
    
    void OnCollisionStay2D(Collision2D collision) { }  // while in contact
    void OnCollisionExit2D(Collision2D collision)
    {
        if (collision.gameObject.CompareTag("Ground"))
            isGrounded = false;
    }
    
    private bool isGrounded;
    void Heal(float amount) { }
    void TakeDamage(float amount) { }
    void CollectCoin(GameObject coin) { }
}
```

### Trigger vs Collision — The Difference

```
COLLIDER (Is Trigger = OFF)          COLLIDER (Is Trigger = ON)
─────────────────────────────        ─────────────────────────────
Physical object — stops things       Ghost zone — things pass through
OnCollisionEnter/Stay/Exit           OnTriggerEnter/Stay/Exit
Needs Rigidbody to receive events    Only needs Collider

USE FOR:                             USE FOR:
  floors, walls, platforms             pickup zones, damage areas,
  bullet impacts                       detection zones, checkpoints,
  physical interactions                camera trigger zones
```

---

## 00B.11 — `Gizmos` — Drawing Debug Shapes in Scene View

```csharp
// OnDrawGizmos — always visible in Scene view
void OnDrawGizmos()
{
    Gizmos.color = Color.yellow;
    Gizmos.DrawWireSphere(transform.position, detectionRange); // always shown
}

// OnDrawGizmosSelected — only visible when object is selected
void OnDrawGizmosSelected()
{
    Gizmos.color = Color.red;
    Gizmos.DrawWireSphere(transform.position, attackRange);   // shown when selected
    
    Gizmos.color = Color.green;
    Gizmos.DrawLine(transform.position, transform.position + transform.forward * 5f);
    
    // Draw custom arrow for patrol direction
    Gizmos.DrawWireCube(patrolTarget.position, Vector3.one * 0.5f);
}
```

---

## 00B.12 — A Complete Script Template

This is the professional template for every MonoBehaviour you write:

```csharp
using System.Collections;
using UnityEngine;

/// <summary>
/// Brief description of what this script does.
/// Attach to: [GameObject name/type].
/// Requires: [List of required components].
/// </summary>
[RequireComponent(typeof(Rigidbody2D))]
[DisallowMultipleComponent]
public class PlayerController : MonoBehaviour
{
    // ===== INSPECTOR FIELDS =====
    [Header("Movement")]
    [SerializeField, Range(1f, 20f)] private float moveSpeed = 8f;
    [SerializeField, Range(1f, 30f)] private float jumpForce = 12f;
    
    [Header("References")]
    [SerializeField] private Transform groundCheck;
    [SerializeField] private LayerMask groundLayer;
    
    // ===== CACHED COMPONENTS =====
    private Rigidbody2D rb;
    private Animator anim;
    private SpriteRenderer sr;
    
    // ===== PRIVATE STATE =====
    private bool isGrounded;
    private float horizontalInput;
    
    // ===== EVENTS =====
    public event System.Action<float> OnHealthChanged;
    
    // ===== PUBLIC PROPERTIES =====
    public bool IsGrounded => isGrounded;
    
    // ===== LIFECYCLE =====
    
    void Awake()
    {
        // Cache components — self-setup only
        rb = GetComponent<Rigidbody2D>();
        anim = GetComponent<Animator>();
        sr = GetComponent<SpriteRenderer>();
    }
    
    void OnEnable()
    {
        // Subscribe to events
    }
    
    void Start()
    {
        // Reference other objects — they're all initialized by now
    }
    
    void Update()
    {
        // Input and non-physics logic
        horizontalInput = Input.GetAxisRaw("Horizontal");
        
        isGrounded = Physics2D.OverlapCircle(groundCheck.position, 0.15f, groundLayer);
    }
    
    void FixedUpdate()
    {
        // Physics — Rigidbody forces and velocity
        rb.linearVelocity = new Vector2(horizontalInput * moveSpeed, rb.linearVelocity.y);
    }
    
    void LateUpdate()
    {
        // Post-frame updates (nothing for player, but camera would go here)
    }
    
    void OnDisable()
    {
        // Unsubscribe from events
    }
    
    void OnDestroy()
    {
        // Final cleanup
    }
    
    // ===== PHYSICS CALLBACKS =====
    
    void OnCollisionEnter2D(Collision2D collision) { }
    void OnTriggerEnter2D(Collider2D other) { }
    
    // ===== PRIVATE METHODS =====
    
    void Jump()
    {
        rb.AddForce(Vector2.up * jumpForce, ForceMode2D.Impulse);
    }
    
    // ===== GIZMOS =====
    
    void OnDrawGizmosSelected()
    {
        if (groundCheck != null)
            Gizmos.DrawWireSphere(groundCheck.position, 0.15f);
    }
}
```

---

## 📝 Summary — Lifecycle Quick Reference

| Method | Runs | Use For |
| :--- | :--- | :--- |
| `Awake()` | Once on creation | Cache own components, init own data |
| `OnEnable()` | On each enable | Subscribe to events |
| `Start()` | Once before first frame | Reference other scripts, final setup |
| `Update()` | Every frame | Input, game logic, timers |
| `FixedUpdate()` | Every physics step | Rigidbody forces and velocity |
| `LateUpdate()` | Every frame (after Update) | Camera follow, look-at |
| `OnDisable()` | On each disable | Unsubscribe from events |
| `OnDestroy()` | Once on destruction | Cancel tasks, final cleanup |
| `OnTriggerEnter2D` | On trigger entry | Pickups, zones, detection |
| `OnCollisionEnter2D` | On physical collision | Damage, landing, impact |
| `OnDrawGizmosSelected` | In Editor only | Visualize ranges, paths |

**Previous:** [[00A — How Unity Works]] | **Next:** [[00C — Transform Deep Dive]]
