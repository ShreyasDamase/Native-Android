# 04 — Classes & Structs
### 🟢 Block A — Language Foundations

> [!IMPORTANT]
> This is the **single most important chapter for Unity**. The class vs. struct distinction controls memory allocation, GC pressure, and the behavior of every Vector, Quaternion, and custom data object you use. Get this wrong and your game stutters.

---

## 4.1 — Classes (Reference Types)

A `class` is a blueprint. Every time you create an instance with `new`, it's allocated on the **managed heap**. Multiple variables can point to the same instance.

```csharp
// MonoBehaviour is a class — Unity attaches it to GameObjects
public class PlayerController : MonoBehaviour
{
    // Fields — data this class owns
    private float health = 100f;
    private float maxHealth = 100f;
    private Rigidbody2D rb;          // Reference to another class instance
    private Animator animator;       // Reference to another class instance
    
    // Constructor-equivalent in Unity: Awake()
    void Awake()
    {
        // Cache component references (expensive GetComponent call done once)
        rb = GetComponent<Rigidbody2D>();
        animator = GetComponent<Animator>();
    }
}
```

### Classes Are Shared References

```csharp
// Class reference behavior
PlayerController playerA = FindFirstObjectByType<PlayerController>();
PlayerController playerB = playerA;  // Both point to SAME object

playerB.health = 50f;
Debug.Log(playerA.health); // 50f — because playerA and playerB are the SAME object
```

---

## 4.2 — Structs (Value Types)

A `struct` is a value type — it lives on the stack and is **copied** every time it's assigned. All of Unity's math types are structs.

```csharp
// Example: Vector3 is a struct (simplified version of what Unity defines)
public struct Vector3
{
    public float x, y, z;
    
    public Vector3(float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    // Structs can have methods
    public float Magnitude() => Mathf.Sqrt(x * x + y * y + z * z);
}
```

### The Struct Copy Trap (Critical!)

```csharp
// Struct copy behavior
Vector3 posA = new Vector3(1f, 0f, 0f);
Vector3 posB = posA;   // posB is a COPY — independent from posA

posB.x = 99f;
Debug.Log(posA.x); // Still 1f — posA was NOT changed

// This is why you CANNOT modify struct properties of Unity components directly:
// transform.position returns a COPY of the Vector3

// ❌ WRONG — modifies a temporary copy, not the actual transform position
transform.position.x = 5f; // COMPILER ERROR in newer C#

// ✅ CORRECT — copy → modify → re-assign
Vector3 pos = transform.position;    // get a copy
pos.x = 5f;                         // modify the copy
transform.position = pos;           // write back to the property
```

---

## 4.3 — When to Use Class vs. Struct

| Use `class` when... | Use `struct` when... |
| :--- | :--- |
| The object has identity (a specific Enemy, a Player) | It's just pure data (position, color, stats) |
| You need inheritance | No inheritance needed |
| It will be stored in a `List<T>` and modified frequently | It's small (< 16 bytes ideally) and read-often |
| It needs `null` to represent "no object" | It should always have a valid value |
| It's a MonoBehaviour/ScriptableObject | It's a math type, config, or event payload |

```csharp
// ✅ Use class — has identity, behavior, lifecycle
public class Enemy : MonoBehaviour
{
    public int ID;
    public float Health;
    public void TakeDamage(float amount) { Health -= amount; }
}

// ✅ Use struct — pure data, no identity, small size, used as parameter
public struct SpawnRequest
{
    public Vector3 Position;
    public int EnemyTypeID;
    public float DelaySeconds;
}
```

---

## 4.4 — Constructors

```csharp
public class Bullet
{
    public Vector3 Origin;
    public Vector3 Direction;
    public float Speed;
    public float Damage;

    // Default constructor
    public Bullet()
    {
        Speed = 20f;
        Damage = 10f;
    }
    
    // Parameterized constructor
    public Bullet(Vector3 origin, Vector3 direction, float speed = 20f, float damage = 10f)
    {
        Origin = origin;
        Direction = direction.normalized;
        Speed = speed;
        Damage = damage;
    }
    
    // Constructor chaining — call another constructor
    public Bullet(Vector3 origin, Vector3 direction) 
        : this(origin, direction, 20f, 10f) { }
}
```

> [!NOTE]
> Unity `MonoBehaviour` classes **cannot have constructors**. Unity creates them internally. Use `Awake()` for initialization instead.

---

## 4.5 — `static` Members

Static members belong to the **class itself**, not to any instance. All instances share the same static value.

```csharp
public class GameManager : MonoBehaviour
{
    // Static singleton reference — one shared instance for all
    public static GameManager Instance { get; private set; }
    
    // Static counter shared across ALL Enemy instances
    public static int TotalEnemiesAlive = 0;
    
    void Awake()
    {
        // Singleton pattern
        if (Instance != null && Instance != this)
        {
            Destroy(gameObject);
            return;
        }
        Instance = this;
        DontDestroyOnLoad(gameObject);
    }
    
    // Static method — called without needing an instance
    public static void QuitGame()
    {
        #if UNITY_EDITOR
            UnityEditor.EditorApplication.isPlaying = false;
        #else
            Application.Quit();
        #endif
    }
}

// Usage
GameManager.QuitGame();           // static method — no instance needed
Debug.Log(Enemy.TotalEnemiesAlive); // static field
```

---

## 4.6 — `readonly` and `const`

```csharp
public class PhysicsConstants : MonoBehaviour
{
    // const — baked at compile time, zero runtime memory
    private const float GRAVITY = 9.81f;
    private const int MAX_COLLIDERS = 32;
    
    // readonly — set once (in constructor or field init), then immutable
    private readonly string playerUID;
    private readonly Rigidbody2D rb;
    
    void Awake()
    {
        playerUID = System.Guid.NewGuid().ToString();
        rb = GetComponent<Rigidbody2D>();
        // rb = ... // Can't reassign rb after Awake() — it's readonly
    }
}
```

---

## 4.7 — `partial` Classes

Splits a class definition across multiple files. Unity uses this for generated code.

```csharp
// File: PlayerController.cs — your gameplay code
public partial class PlayerController : MonoBehaviour
{
    private float health = 100f;
    
    void HandleMovement() { /* ... */ }
}

// File: PlayerController.InputActions.cs — auto-generated input code
public partial class PlayerController : MonoBehaviour, PlayerInputActions.IPlayerActions
{
    private PlayerInputActions inputActions;
    
    public void OnMove(InputAction.CallbackContext context) { /* ... */ }
}
```

---

## 4.8 — Nested Classes

```csharp
public class WaveManager : MonoBehaviour
{
    // Nested class — scoped to WaveManager, represents data about a wave
    [System.Serializable]
    public class Wave
    {
        public string WaveName;
        public int EnemyCount;
        public float SpawnInterval;
        public GameObject EnemyPrefab;
    }
    
    [SerializeField] private List<Wave> waves;
    
    void SpawnWave(Wave wave)
    {
        StartCoroutine(SpawnEnemiesRoutine(wave));
    }
}
```

---

## 4.9 — 🎮 2D vs 3D: Class Structure Differences

### 🎮 2D Player Controller Class Structure
```csharp
public class PlayerController2D : MonoBehaviour
{
    [Header("Movement")]
    [SerializeField] private float moveSpeed = 5f;
    [SerializeField] private float jumpForce = 12f;
    
    // 2D-specific components
    private Rigidbody2D rb;
    private BoxCollider2D groundCheck;
    
    [Header("Ground Detection")]
    [SerializeField] private LayerMask groundLayer;
    [SerializeField] private Transform groundCheckPoint;
    [SerializeField] private float groundCheckRadius = 0.2f;
    
    private bool isGrounded;
    private float horizontalInput;
    
    void Awake()
    {
        rb = GetComponent<Rigidbody2D>();
    }
    
    void Update()
    {
        horizontalInput = Input.GetAxisRaw("Horizontal");
        isGrounded = Physics2D.OverlapCircle(groundCheckPoint.position, groundCheckRadius, groundLayer);
        
        if (isGrounded && Input.GetButtonDown("Jump"))
            rb.AddForce(Vector2.up * jumpForce, ForceMode2D.Impulse);
    }
    
    void FixedUpdate()
    {
        rb.linearVelocity = new Vector2(horizontalInput * moveSpeed, rb.linearVelocity.y);
    }
}
```

### 🎮 3D Player Controller Class Structure
```csharp
public class PlayerController3D : MonoBehaviour
{
    [Header("Movement")]
    [SerializeField] private float moveSpeed = 5f;
    [SerializeField] private float jumpForce = 7f;
    [SerializeField] private float rotationSpeed = 720f;
    
    // 3D-specific components
    private Rigidbody rb;
    private CapsuleCollider capsule;
    
    [Header("Ground Detection")]
    [SerializeField] private LayerMask groundLayer;
    [SerializeField] private float groundCheckDistance = 0.3f;
    
    private bool isGrounded;
    private Vector3 moveInput;
    
    void Awake()
    {
        rb = GetComponent<Rigidbody>();
        capsule = GetComponent<CapsuleCollider>();
    }
    
    void Update()
    {
        // 3D requires 3-axis input
        float h = Input.GetAxisRaw("Horizontal");
        float v = Input.GetAxisRaw("Vertical");
        moveInput = new Vector3(h, 0f, v).normalized;
        
        // 3D ground check uses SphereCast
        isGrounded = Physics.SphereCast(transform.position, 0.3f, Vector3.down, out _, groundCheckDistance, groundLayer);
        
        if (isGrounded && Input.GetButtonDown("Jump"))
            rb.AddForce(Vector3.up * jumpForce, ForceMode.Impulse);
            
        // Rotate to face movement direction (3D only)
        if (moveInput != Vector3.zero)
        {
            Quaternion targetRot = Quaternion.LookRotation(moveInput);
            transform.rotation = Quaternion.RotateTowards(transform.rotation, targetRot, rotationSpeed * Time.deltaTime);
        }
    }
    
    void FixedUpdate()
    {
        // Preserve Y velocity (gravity), only override XZ
        Vector3 velocity = moveInput * moveSpeed;
        velocity.y = rb.linearVelocity.y;
        rb.linearVelocity = velocity;
    }
}
```

---

## 📝 Summary

| Concept | Key Rule |
| :--- | :--- |
| `class` | Reference type — heap — shared pointer — can be null |
| `struct` | Value type — stack — copied on assign — cannot be null |
| `static` | Belongs to the class, shared by all instances |
| `const` | Compile-time constant, zero runtime memory |
| `readonly` | Runtime constant, set once in constructor |
| Struct trap | Always copy → modify → re-assign for Unity structs |
| `MonoBehaviour` | Never use constructors — use `Awake()` instead |

**Previous:** [[03 — Methods, Parameters & Return Types]] | **Next:** [[05 — OOP — Inheritance & Polymorphism]]
