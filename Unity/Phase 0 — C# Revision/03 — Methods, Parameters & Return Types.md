# 03 — Methods, Parameters & Return Types
### 🟢 Block A — Language Foundations

> [!NOTE]
> Methods in Unity do a lot more than in typical apps — they respond to physics, input, lifecycle events, and animation frames. Understanding **parameter modifiers** (`ref`, `out`, `in`) is critical because Unity's APIs use them heavily for performance.

---

## 3.1 — Anatomy of a Method

```csharp
// [access modifier] [return type] [name]([parameters])
public float CalculateDamage(float baseDamage, float critMultiplier)
{
    return baseDamage * critMultiplier;
}

// Void method — returns nothing
private void ApplyKnockback(Vector2 direction, float force)
{
    rb.AddForce(direction * force, ForceMode2D.Impulse);
}

// Expression-bodied method (=> syntax) — single-expression shortcut
public bool IsAlive() => health > 0f;
public float GetSpeedSq() => rb.linearVelocity.sqrMagnitude;
```

---

## 3.2 — Parameter Modifiers: `ref`, `out`, `in`

These three modifiers change how values are passed between methods. Understanding them is key to reading Unity's API.

### `out` — The Method Will Assign a Value (Write-Only)

`out` parameters must be assigned inside the method before it returns. The variable does NOT need to be initialized before calling.

```csharp
// Unity uses 'out' heavily for safe, allocation-free queries
void HandlePhysicsQuery()
{
    // Declare inline — no need to initialize raycastHit beforehand
    if (Physics2D.Raycast(transform.position, Vector2.down, 1f) is { collider: not null } hit2d)
    {
        Debug.Log(hit2d.normal);
    }
    
    // TryGetComponent — the most common 'out' pattern you'll write daily
    if (TryGetComponent<Rigidbody2D>(out Rigidbody2D rb))
    {
        rb.gravityScale = 2f;
    }
    
    // TryGetValue on Dictionary — safe key lookup without throwing
    Dictionary<string, int> inventory = new Dictionary<string, int>();
    if (inventory.TryGetValue("sword", out int count))
    {
        Debug.Log($"Sword count: {count}");
    }
}

// Writing your own method with 'out'
bool TryGetNearestEnemy(out Enemy nearestEnemy)
{
    nearestEnemy = null; // MUST assign before any return path
    
    if (allEnemies.Count == 0) return false;
    
    nearestEnemy = allEnemies
        .OrderBy(e => Vector2.Distance(transform.position, e.transform.position))
        .First();
    return true;
}
```

### `ref` — Pass-by-Reference (Read AND Write)

The variable MUST be initialized before passing. The method can read and modify the original.

```csharp
// Swap two floats without creating new variables
public static void Swap(ref float a, ref float b)
{
    float temp = a;
    a = b;
    b = temp;
}

// Usage
float speed = 5f;
float maxSpeed = 10f;
Swap(ref speed, ref maxSpeed);
// speed is now 10f, maxSpeed is now 5f

// Useful for modifying a value in-place from a helper method
public void ClampHealth(ref float currentHP, float maxHP)
{
    currentHP = Mathf.Clamp(currentHP, 0f, maxHP);
}
```

### `in` — Pass-by-Reference, Read-Only (Performance Optimization)

Passes a struct by reference (avoiding copy) but the method cannot modify it. Used to pass large structs like `Matrix4x4` or custom data without copying.

```csharp
// Without 'in': Vector3 is copied (12 bytes) for every call
public float GetDistance(Vector3 target) 
{
    return (transform.position - target).magnitude;
}

// With 'in': Vector3 is passed by reference, no copy, read-only
public float GetDistanceFast(in Vector3 target) 
{
    return (transform.position - target).magnitude;
    // target is read-only — cannot be modified here
}

// Most useful with large custom structs
public struct BulletData
{
    public Vector3 Origin, Direction;
    public float Speed, Damage, Range;
    public LayerMask HitMask;
}

public bool ProcessBullet(in BulletData data)
{
    // data is 40+ bytes — passing 'in' avoids copying all of that
    return Physics.Raycast(data.Origin, data.Direction, data.Range, data.HitMask);
}
```

---

## 3.3 — Default Parameter Values

```csharp
// Parameters with defaults must come AFTER required parameters
public void SpawnEnemy(Vector3 position, int health = 100, float speed = 3f, bool isElite = false)
{
    // position is required, others are optional
}

// Call with only required argument
SpawnEnemy(new Vector3(5f, 0f, 0f));

// Call with some optional arguments using named parameters
SpawnEnemy(new Vector3(5f, 0f, 0f), isElite: true); // skip health and speed
SpawnEnemy(new Vector3(5f, 0f, 0f), 200, isElite: true); // skip only speed
```

---

## 3.4 — `params` — Variable Number of Arguments

```csharp
// Accept any number of arguments as an array
public void TakeDamageFrom(params string[] sources)
{
    foreach (string source in sources)
    {
        Debug.Log($"Damaged by: {source}");
    }
}

// Call with any number of arguments
TakeDamageFrom("Fire");
TakeDamageFrom("Fire", "Poison");
TakeDamageFrom("Fire", "Poison", "Explosion", "Fall");
```

---

## 3.5 — Return Types

```csharp
// void — returns nothing
void Die() { Destroy(gameObject); }

// Single value return
float GetHPPercent() => health / maxHealth;
bool IsFullHealth() => health >= maxHealth;

// Tuple — return multiple values without creating a class
(float damage, bool isCritical) CalculateAttack()
{
    float dmg = baseDamage * Random.Range(0.8f, 1.2f);
    bool crit = Random.value < critChance;
    return (crit ? dmg * 2f : dmg, crit);
}

// Usage — tuple deconstruction
var (finalDamage, wasCrit) = CalculateAttack();
if (wasCrit) Debug.Log("Critical Hit!");
target.TakeDamage(finalDamage);
```

---

## 3.6 — Static Methods

Static methods belong to the class itself, not to an instance. Use them for utility functions, math helpers, or factory methods.

```csharp
public class MathUtils
{
    // Static method — call without creating a MathUtils object
    public static float RemapValue(float value, float fromMin, float fromMax, float toMin, float toMax)
    {
        return toMin + (value - fromMin) / (fromMax - fromMin) * (toMax - toMin);
    }
    
    public static bool IsApproximatelyZero(float value, float tolerance = 0.001f)
    {
        return Mathf.Abs(value) < tolerance;
    }
}

// Usage
float remapped = MathUtils.RemapValue(0.5f, 0f, 1f, -1f, 1f); // = 0f
```

---

## 3.7 — Method Overloading

Same method name, different parameter signatures.

```csharp
public class AudioManager : MonoBehaviour
{
    // Different ways to play a sound
    public void PlaySound(AudioClip clip) { PlaySound(clip, 1f, 1f); }
    public void PlaySound(AudioClip clip, float volume) { PlaySound(clip, volume, 1f); }
    public void PlaySound(AudioClip clip, float volume, float pitch)
    {
        source.pitch = pitch;
        source.PlayOneShot(clip, volume);
    }
}
```

---

## 3.8 — Unity Lifecycle Methods (Special Methods)

These are called by the Unity engine on specific events. They must match the exact signature.

```csharp
public class EnemyAI : MonoBehaviour
{
    // Called ONCE when the object is created (before any frame)
    // Use for self-initialization — cache components here
    void Awake()
    {
        rb = GetComponent<Rigidbody2D>();
        animator = GetComponent<Animator>();
    }

    // Called ONCE on the first frame (after ALL Awake() methods have run)
    // Use to reference OTHER scripts (they've had their Awake() called by now)
    void Start()
    {
        player = FindFirstObjectByType<PlayerController>();
    }

    // Called EVERY FRAME — for input, logic, non-physics movement
    void Update()
    {
        HandleMovement();
    }

    // Called EVERY PHYSICS STEP (fixed timestep, default 50/sec)
    // Use for all Rigidbody operations (forces, velocities)
    void FixedUpdate()
    {
        ApplyForces();
    }

    // Called after Update — use for camera follow, secondary reactions
    void LateUpdate()
    {
        UpdateHealthBarPosition();
    }

    // Called when this object is destroyed (cleanup)
    void OnDestroy()
    {
        // Unsubscribe from events to avoid memory leaks!
        GameEvents.OnPlayerDied -= HandlePlayerDied;
    }

    // Called when the object becomes active/inactive
    void OnEnable()  { /* subscribe to events */ }
    void OnDisable() { /* unsubscribe from events */ }
}
```

---

## 3.9 — 🎮 2D vs 3D: Physics Callbacks

### 🎮 2D Collision & Trigger Methods
```csharp
// 2D Physics callbacks — use 'Collider2D' and 'Collision2D'
void OnCollisionEnter2D(Collision2D collision)
{
    if (collision.gameObject.CompareTag("Ground"))
        isGrounded = true;
}

void OnCollisionExit2D(Collision2D collision)
{
    if (collision.gameObject.CompareTag("Ground"))
        isGrounded = false;
}

void OnTriggerEnter2D(Collider2D other)
{
    if (other.TryGetComponent<Coin>(out Coin coin))
        CollectCoin(coin);
}

void OnTriggerExit2D(Collider2D other) { }
void OnTriggerStay2D(Collider2D other) { }  // expensive — avoid if possible
```

### 🎮 3D Collision & Trigger Methods
```csharp
// 3D Physics callbacks — use 'Collider' and 'Collision' (no "2D" suffix!)
void OnCollisionEnter(Collision collision)
{
    if (collision.gameObject.CompareTag("Ground"))
    {
        // Get contact normal for landing detection
        Vector3 normal = collision.contacts[0].normal;
        if (Vector3.Dot(normal, Vector3.up) > 0.7f)
            isGrounded = true;
    }
}

void OnTriggerEnter(Collider other)
{
    if (other.TryGetComponent<HealthPack>(out HealthPack pack))
        Heal(pack.Amount);
}
```

---

## 📝 Summary

| Modifier | Caller must initialize | Method can read | Method can modify | Key use case |
| :--- | :--- | :--- | :--- | :--- |
| (none) | N/A | ✅ | Modifies copy only | Normal passing |
| `ref` | ✅ Yes | ✅ | ✅ Yes | Swap, in-place modify |
| `out` | ❌ No | ❌ | ✅ Yes (required) | TryGet patterns |
| `in` | ✅ Yes | ✅ | ❌ No | Large struct performance |

**Previous:** [[02 — Control Flow & Pattern Matching]] | **Next:** [[04 — Classes & Structs]]
