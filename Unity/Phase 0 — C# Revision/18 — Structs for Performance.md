# 18 — Structs for Performance
### 🔴 Block C — Memory Management & Performance

> [!NOTE]
> Structs are the professional C# developer's secret weapon against GC. When used correctly, they eliminate heap allocations entirely. Unity uses them everywhere — every `Vector2`, `Vector3`, `Quaternion`, `Color`, `RaycastHit` is a struct.

---

## 18.1 — Why Structs Beat Classes for Pure Data

```
CLASS:                              STRUCT:
┌─────────────────────────────┐    ┌─────────────────────────────┐
│ Object Header (16 bytes)    │    │ No header overhead           │
│ VTable pointer              │    │ No vtable                   │
│ GC reference tracking       │    │ No GC tracking              │
│ x: float (4 bytes)          │    │ x: float (4 bytes)          │
│ y: float (4 bytes)          │    │ y: float (4 bytes)          │
│ z: float (4 bytes)          │    │ z: float (4 bytes)          │
│                             │    │                             │
│ Lives on: HEAP              │    │ Lives on: STACK             │
│ GC cost: YES                │    │ GC cost: NONE               │
└─────────────────────────────┘    └─────────────────────────────┘
```

---

## 18.2 — Defining a Good Struct

```csharp
// ✅ Good struct — pure data, small, no mutable state, no reference types
public struct DamageEvent
{
    public readonly float Amount;
    public readonly DamageType Type;
    public readonly Vector2 HitPoint;
    public readonly bool IsCritical;
    public readonly float Knockback;
    
    // Structs CAN have constructors
    public DamageEvent(float amount, DamageType type, Vector2 hitPoint, bool isCritical = false, float knockback = 0f)
    {
        Amount = amount;
        Type = type;
        HitPoint = hitPoint;
        IsCritical = isCritical;
        Knockback = knockback;
    }
    
    // Structs CAN have methods (but these are called on copies unless passed by ref)
    public float GetEffectiveDamage(float resistance) => Amount * (1f - resistance);
    
    // Structs CAN have computed properties
    public bool IsLethal => Amount >= 999f;
}

// Usage — zero heap allocation!
void OnHit(Collider2D other)
{
    // Created on the stack
    DamageEvent dmg = new DamageEvent(25f, DamageType.Fire, (Vector2)other.transform.position, true);
    
    if (other.TryGetComponent<IDamageable>(out var target))
        target.TakeDamage(dmg); // passed as a copy — fast
}
```

---

## 18.3 — `readonly struct` — The Best Practice

Mark a struct as `readonly` to enforce immutability and enable compiler optimizations.

```csharp
// readonly struct — all fields must also be readonly
// The compiler can make defensive copies more efficient
public readonly struct MovementData
{
    public readonly Vector2 Velocity;
    public readonly float MoveSpeed;
    public readonly float JumpForce;
    public readonly bool IsGrounded;
    public readonly bool IsFacingRight;
    
    public MovementData(Vector2 velocity, float speed, float jump, bool grounded, bool right)
    {
        Velocity = velocity;
        MoveSpeed = speed;
        JumpForce = jump;
        IsGrounded = grounded;
        IsFacingRight = right;
    }
    
    // All methods on readonly struct are automatically readonly (no mutation possible)
    public float SpeedSqr => Velocity.sqrMagnitude;
    public bool IsMoving => Velocity.sqrMagnitude > 0.01f;
    public bool IsFalling => Velocity.y < -0.1f;
}

// Used as a snapshot of movement state — pass around without GC
void ProcessAnimation(in MovementData data)
{
    animator.SetBool("IsGrounded", data.IsGrounded);
    animator.SetFloat("SpeedX", Mathf.Abs(data.Velocity.x));
    animator.SetFloat("SpeedY", data.Velocity.y);
}
```

---

## 18.4 — Passing Structs Efficiently with `in`

Large structs (>16 bytes) should be passed with `in` to avoid copying.

```csharp
// How big is a struct? Count field bytes
// Vector3: 3 floats = 12 bytes — borderline, 'in' still helps
// Matrix4x4: 16 floats = 64 bytes — DEFINITELY use 'in'

// Your custom struct
public struct BulletParameters
{
    public Vector3 Origin;        // 12 bytes
    public Vector3 Direction;     // 12 bytes
    public float Speed;           // 4 bytes
    public float Damage;          // 4 bytes
    public float Lifetime;        // 4 bytes
    public LayerMask HitMask;     // 4 bytes
    // Total: 40 bytes — large enough to justify 'in'
}

// WITHOUT 'in' — copies 40 bytes every call
bool SimulateBullet(BulletParameters data) { /* ... */ return false; }

// WITH 'in' — passes a reference (4-8 bytes pointer), data is read-only
bool SimulateBullet(in BulletParameters data)
{
    // data cannot be modified — read-only reference
    Vector3 pos = data.Origin;
    Vector3 dir = data.Direction.normalized;
    
    return Physics.Raycast(pos, dir, data.Speed * data.Lifetime, data.HitMask);
}
```

---

## 18.5 — Structs in Arrays vs. Classes in Arrays

```csharp
// Struct array — CONTIGUOUS memory (cache friendly — very fast!)
// All data is stored inline in the array
SpawnPoint[] structArray = new SpawnPoint[100];
// Memory: [pos1.x|pos1.y|pos1.z|id1|pos2.x|pos2.y|...]
// CPU can prefetch this efficiently!

// Class array — POINTER array (cache UNfriendly — slower)
// Each array slot is a pointer to a different heap location
SpawnPointClass[] classArray = new SpawnPointClass[100];
// Memory: [ptr→heap1 | ptr→heap2 | ptr→heap3 | ...]
// Each access jumps to a different memory location!

// Benchmark: iterating 10,000 structs can be 5-10x faster than 10,000 classes

// Example — enemy AI data as struct array
public struct EnemyData
{
    public Vector3 Position;
    public float Health;
    public float Speed;
    public int TargetID;
}

EnemyData[] enemyData = new EnemyData[1000]; // 1000 enemies, one contiguous block

void UpdateAllEnemies()
{
    for (int i = 0; i < enemyData.Length; i++)
    {
        // Direct struct modification by index — modifies the ACTUAL struct in array
        enemyData[i].Position += Vector3.forward * enemyData[i].Speed * Time.deltaTime;
    }
}
```

---

## 18.6 — The Struct Modification Traps (Critical!)

```csharp
// TRAP 1: Modifying a struct stored in a class property
public class Container
{
    public Vector3 Position { get; set; } // property
}

Container container = new Container();

// ❌ WRONG — Position returns a COPY of the Vector3
container.Position.x = 5f; // modifies the copy, not the actual field — COMPILE ERROR

// ✅ CORRECT — copy, modify, re-assign
Vector3 pos = container.Position;
pos.x = 5f;
container.Position = pos;


// TRAP 2: Struct method modifying fields (requires ref or must re-assign)
public struct Timer
{
    public float Elapsed;
    public float Duration;
    
    // This method seems like it modifies the struct...
    public void Tick(float deltaTime)
    {
        Elapsed += deltaTime; // modifies THIS copy
    }
    
    public bool IsDone => Elapsed >= Duration;
}

Timer t = new Timer { Duration = 5f };

// ❌ WRONG — .Tick() modifies a COPY of t
t.Tick(Time.deltaTime); // t.Elapsed is still 0!

// This is because method calls on value types work on a copy UNLESS...

// ✅ CORRECT Option A — call on a ref
ref Timer tRef = ref t;
tRef.Tick(Time.deltaTime); // modifies the actual t

// ✅ CORRECT Option B — reassign
t.Elapsed += Time.deltaTime; // directly modify the field

// ✅ CORRECT Option C — mark the struct as readonly and make methods return new values
public readonly struct ImmutableTimer
{
    public readonly float Elapsed;
    public readonly float Duration;
    
    public ImmutableTimer(float duration) { Duration = duration; Elapsed = 0f; }
    private ImmutableTimer(float elapsed, float duration) { Elapsed = elapsed; Duration = duration; }
    
    public ImmutableTimer Tick(float deltaTime) => new ImmutableTimer(Elapsed + deltaTime, Duration);
    public bool IsDone => Elapsed >= Duration;
}

ImmutableTimer timer = new ImmutableTimer(5f);
timer = timer.Tick(Time.deltaTime); // reassign — creates new struct on stack
```

---

## 18.7 — When to Use Struct vs Class

```csharp
// ✅ USE STRUCT FOR:

// 1. Math/geometry data
public struct Circle { public Vector2 Center; public float Radius; }
public struct AABB { public Vector2 Min, Max; }
public struct Ray2D { public Vector2 Origin; public Vector2 Direction; }

// 2. Event payload data (pass between systems without allocation)
public struct HitInfo
{
    public readonly IDamageable Target;
    public readonly float Damage;
    public readonly bool IsCritical;
    public readonly Vector2 HitPoint;
}

// 3. Configuration snapshots (read once, pass around)
public struct PhysicsConfig
{
    public float GravityScale;
    public float Drag;
    public float Mass;
    public bool FreezeRotation;
}

// 4. Return multiple values cleanly
public struct GroundCheckResult
{
    public readonly bool IsGrounded;
    public readonly Vector2 Normal;
    public readonly float SlopeAngle;
    public readonly Collider2D HitCollider;
}

GroundCheckResult CheckGround()
{
    RaycastHit2D hit = Physics2D.Raycast(transform.position, Vector2.down, 0.5f, groundLayer);
    if (!hit) return default; // all zeros/null
    
    return new GroundCheckResult
    {
        IsGrounded = true,
        Normal = hit.normal,
        SlopeAngle = Vector2.Angle(hit.normal, Vector2.up),
        HitCollider = hit.collider
    };
}

// ❌ DON'T USE STRUCT FOR:
// 1. Anything larger than ~64 bytes
// 2. Anything that needs to be null (identity checks like "does enemy exist?")
// 3. MonoBehaviour subclasses (Unity requires class)
// 4. Objects with complex mutable state
// 5. Objects stored in a List<T> that get modified frequently (copies in/out)
```

---

## 18.8 — 🎮 2D vs 3D: Struct Use Cases

### 🎮 2D Useful Structs
```csharp
// 2D contact info struct — returned from physics checks
public struct Contact2DInfo
{
    public readonly bool HasContact;
    public readonly Vector2 Point;
    public readonly Vector2 Normal;
    public readonly Collider2D Collider;
    public readonly string Tag;
    
    public static Contact2DInfo None => default;
}

// 2D tile data
public struct TileData
{
    public readonly Vector2Int GridPosition;
    public readonly bool IsWalkable;
    public readonly bool IsWater;
    public readonly int MovementCost;
}

// 2D input snapshot (take once per frame, pass to movement)
public struct InputSnapshot2D
{
    public readonly float Horizontal;
    public readonly bool JumpPressed;
    public readonly bool JumpHeld;
    public readonly bool RunHeld;
    public readonly bool AttackPressed;
    
    public static InputSnapshot2D Capture()
    {
        return new InputSnapshot2D
        {
            Horizontal = Input.GetAxisRaw("Horizontal"),
            JumpPressed = Input.GetButtonDown("Jump"),
            JumpHeld = Input.GetButton("Jump"),
            RunHeld = Input.GetButton("Run"),
            AttackPressed = Input.GetButtonDown("Fire1")
        };
    }
    
    public bool IsMoving => Mathf.Abs(Horizontal) > 0.1f;
}
```

### 🎮 3D Useful Structs
```csharp
// 3D raycast result struct
public struct RaycastResult3D
{
    public readonly bool Hit;
    public readonly Vector3 Point;
    public readonly Vector3 Normal;
    public readonly Collider Collider;
    public readonly float Distance;
    public readonly GameObject GameObject;
    
    public static RaycastResult3D None => default;
    
    public static RaycastResult3D From(RaycastHit hit, bool didHit)
    {
        if (!didHit) return None;
        return new RaycastResult3D
        {
            Hit = true,
            Point = hit.point,
            Normal = hit.normal,
            Collider = hit.collider,
            Distance = hit.distance,
            GameObject = hit.collider.gameObject
        };
    }
}

// 3D input snapshot
public struct InputSnapshot3D
{
    public readonly Vector2 MoveInput;       // WASD normalized
    public readonly Vector2 LookInput;       // Mouse delta
    public readonly bool JumpPressed;
    public readonly bool SprintHeld;
    public readonly bool CrouchHeld;
    public readonly bool AimHeld;
    public readonly bool FirePressed;
    
    public Vector3 MoveDirection3D => new Vector3(MoveInput.x, 0f, MoveInput.y);
}
```

---

## 📝 Summary

| Rule | Detail |
| :--- | :--- |
| Use struct for | Small (< 64 bytes), pure data, no identity, no null needed |
| Use class for | MonoBehaviour, identity, inheritance, mutable complex state |
| `readonly struct` | All fields readonly — best practice for immutable data |
| `in` parameter | Pass large structs by reference without copying |
| Struct in array | Contiguous memory — 5-10x faster to iterate |
| Struct modification trap | Never modify a struct field through a property/return value |
| Return from method | Returns a COPY — always re-assign if needed |

**Previous:** [[17 — Object Pooling in C#]] | **Next:** [[19 — Modern C# Features]]
