# 19 — Modern C# Features
### 🟡 Block D — Modern C# (Unity 6 / C# 10+)

> [!NOTE]
> Unity 6 supports C# 9 and 10 features. These are language improvements that make code significantly cleaner and safer. Learn to recognize these patterns in documentation and tutorials — they're everywhere in modern Unity code.

---

## 19.1 — Records (`record class` / `record struct`)

Records are concise data containers with automatic equality comparison, `ToString()`, and immutability support.

```csharp
// record class — reference type, immutable by design
public record class PlayerProfile
{
    public string Name { get; init; }
    public int Level { get; init; }
    public float TotalPlaytimeHours { get; init; }
}

// record struct — value type, zero allocation
public record struct SpawnConfig
{
    public Vector3 Position { get; init; }
    public Quaternion Rotation { get; init; }
    public int EnemyTypeID { get; init; }
    public float SpawnDelay { get; init; }
}

// Positional record — even more compact
public record struct DamageInfo(float Amount, DamageType Type, Vector2 HitPoint, bool IsCritical);

// Usage
var config1 = new SpawnConfig { Position = Vector3.zero, EnemyTypeID = 1, SpawnDelay = 0.5f };

// 'with' expression — create a copy with some fields changed
var config2 = config1 with { EnemyTypeID = 2, Position = new Vector3(5f, 0f, 0f) };

// Records have built-in value equality
var profile1 = new PlayerProfile { Name = "Hero", Level = 10 };
var profile2 = new PlayerProfile { Name = "Hero", Level = 10 };
Debug.Log(profile1 == profile2); // true — records compare by VALUE, not reference!

// This is different from classes where == compares reference by default
```

---

## 19.2 — `init` Only Setters

Properties that can only be set during object initialization — enforced by the compiler.

```csharp
public class WeaponConfig
{
    // Can only be set via object initializer — immutable after that
    public string Name { get; init; }
    public float Damage { get; init; }
    public float FireRate { get; init; }
    public int MagazineSize { get; init; }
    public bool IsAutomatic { get; init; }
    
    // Can still have mutable properties alongside init
    public int CurrentAmmo { get; set; }
}

// Create with object initializer — the ONLY time you can set init properties
WeaponConfig pistol = new WeaponConfig
{
    Name = "Pistol",
    Damage = 25f,
    FireRate = 2f,
    MagazineSize = 15,
    IsAutomatic = false,
    CurrentAmmo = 15
};

// Later...
pistol.CurrentAmmo = 10;  // ✅ mutable property — fine
pistol.Damage = 30f;       // ❌ COMPILE ERROR — init only!
```

---

## 19.3 — `??` Null-Coalescing and `??=` Null-Coalescing Assignment

```csharp
// ?? — return left side if not null, otherwise right side (fallback)
string playerName = savedName ?? "Unknown Player";  // fallback if null
Transform target = nearestEnemy?.transform ?? defaultTarget;
float volume = PlayerPrefs.HasKey("Volume") ? PlayerPrefs.GetFloat("Volume") : 0.8f;

// Chaining ?? for multiple fallbacks
AudioClip sound = hitSound ?? defaultHitSound ?? silentClip;

// ??= — assign ONLY if the left side is currently null
private List<Enemy> activeEnemies;

public void RegisterEnemy(Enemy enemy)
{
    activeEnemies ??= new List<Enemy>(); // create list only if null
    activeEnemies.Add(enemy);
}

// Equivalent to:
// if (activeEnemies == null) activeEnemies = new List<Enemy>();

// Used in lazy initialization
private Camera _mainCamera;
public Camera MainCamera => _mainCamera ??= Camera.main; // cache on first access
```

---

## 19.4 — Pattern Matching Enhancements

```csharp
// Type patterns
void ProcessPickup(MonoBehaviour pickup)
{
    switch (pickup)
    {
        case Coin coin:
            score += coin.Value;
            break;
        case HealthPack pack when pack.HealAmount > 50f:
            health = maxHealth; // big pack — full heal
            break;
        case HealthPack pack:
            health = Mathf.Min(health + pack.HealAmount, maxHealth);
            break;
        case WeaponPickup weapon when !HasWeapon(weapon.WeaponType):
            EquipWeapon(weapon);
            break;
        case null:
            Debug.LogError("Null pickup!");
            break;
    }
}

// Property patterns — check properties inside the match
void OnTriggerEnter2D(Collider2D other)
{
    if (other is { gameObject: { CompareTag("Enemy"): true } })  // property pattern
    {
        TakeDamage(10f);
    }
    
    // Simpler: check multiple properties at once
    if (other.TryGetComponent<Enemy>(out Enemy enemy) && enemy is { IsAlive: true, IsAggro: false })
    {
        enemy.Alert();
    }
}

// Relational patterns (C# 9)
string GetHealthStatus(float hp) => hp switch
{
    > 75f  => "Healthy",
    > 50f  => "Wounded",
    > 25f  => "Critical",
    > 0f   => "Near Death",
    _      => "Dead"
};

// Logical patterns with 'and', 'or', 'not'
bool IsInDangerZone(float hp, bool hasShield) => hp is > 0f and < 25f && !hasShield;

bool IsDamagingCollision(string tag) => tag is "Enemy" or "Hazard" or "Projectile";
```

---

## 19.5 — Tuple Deconstruction

```csharp
// Returning multiple values cleanly
(float damage, bool isCritical) CalculateHit(float baseDamage, float critChance)
{
    bool crit = Random.value < critChance;
    return (crit ? baseDamage * 2f : baseDamage, crit);
}

// Deconstruct on use
var (finalDamage, wasCrit) = CalculateHit(25f, 0.2f);
if (wasCrit) ShowCritEffect();
target.TakeDamage(finalDamage);

// Discard parts you don't need with _
var (dmg, _) = CalculateHit(25f, 0.2f); // ignore crit bool

// Swap two values elegantly
(a, b) = (b, a); // zero temp variable needed!

// Deconstruct class into tuple
// Define Deconstruct method on your class
public class SpawnPoint : MonoBehaviour
{
    public Vector3 Position;
    public Quaternion Rotation;
    
    public void Deconstruct(out Vector3 position, out Quaternion rotation)
    {
        position = Position;
        rotation = Rotation;
    }
}

// Then deconstruct it naturally
var (pos, rot) = spawnPoint;
Instantiate(enemyPrefab, pos, rot);
```

---

## 19.6 — `using` Declarations (Scoped Disposal)

```csharp
// Old way — need a block
using (var writer = new System.IO.StreamWriter(path))
{
    writer.Write(data);
} // writer.Dispose() called here

// New way (C# 8+) — disposed at end of enclosing scope
void SaveData(string path, string data)
{
    using var writer = new System.IO.StreamWriter(path); // no braces needed
    writer.Write(data);
} // writer.Dispose() called here automatically when method exits
```

---

## 19.7 — Index and Range Operators

```csharp
int[] scores = { 10, 20, 30, 40, 50 };

// Index from end with ^ operator
int last = scores[^1];        // 50 (last element)
int secondLast = scores[^2];  // 40

// Range with .. operator
int[] first3 = scores[..3];   // { 10, 20, 30 } (indices 0,1,2)
int[] last2 = scores[^2..];   // { 40, 50 }
int[] middle = scores[1..^1]; // { 20, 30, 40 } (skip first and last)
int[] copy = scores[..];      // full copy

// Useful for Unity
string[] levelNames = { "Tutorial", "Forest", "Desert", "Cave", "Castle", "Boss" };
string[] mainLevels = levelNames[1..^1]; // skip Tutorial and Boss
string finalBoss = levelNames[^1];       // "Boss"

// Works on Span<T> too (zero-copy slicing)
Span<float> healthValues = stackalloc float[10];
Span<float> firstHalf = healthValues[..5];
Span<float> secondHalf = healthValues[5..];
```

---

## 19.8 — `stackalloc` and `Span<T>` (Zero-Allocation Buffers)

```csharp
// stackalloc — allocate on the stack, not the heap
// Great for temporary arrays that don't leave the method
void ProcessNearbyColliders(Vector2 center, float radius)
{
    // Traditional: allocates on heap
    // Collider2D[] buffer = new Collider2D[16]; // heap!
    
    // Span<T> with NonAlloc — uses a provided buffer
    Collider2D[] stackBuffer = new Collider2D[16]; // still heap, but pre-allocated as field
    int count = Physics2D.OverlapCircleNonAlloc(center, radius, stackBuffer);
    
    Span<Collider2D> hits = stackBuffer.AsSpan(0, count); // zero-copy slice
    
    foreach (ref readonly Collider2D hit in hits) // iterate without copy
    {
        if (hit.TryGetComponent<IDamageable>(out var target))
            target.TakeDamage(10f);
    }
}

// stackalloc for pure value types (truly zero allocation)
void SumWeights()
{
    Span<float> weights = stackalloc float[8]; // on the STACK — zero heap!
    weights[0] = 1.0f;
    weights[1] = 0.5f;
    // ... fill in weights
    
    float total = 0f;
    foreach (float w in weights) total += w;
    Debug.Log($"Total weight: {total}");
    // weights are automatically freed when this method exits
}
```

---

## 19.9 — Local Functions

```csharp
// Local functions — defined inside a method, scoped to that method
// Better than lambda: can be recursive, cannot capture by accident
public float CalculateDamage(DamageData data)
{
    float damage = ApplyResistance(data.BaseDamage, data.Type);
    float final = ApplyCrit(damage, data.CritChance);
    return Mathf.Max(1f, final);
    
    // LOCAL FUNCTIONS — only accessible within CalculateDamage
    float ApplyResistance(float amount, DamageType type)
    {
        float resist = GetResistance(type);
        return amount * (1f - resist);
    }
    
    float ApplyCrit(float amount, float chance)
    {
        if (Random.value < chance)
        {
            ShowCritEffect();
            return amount * 2f;
        }
        return amount;
    }
}

// static local function — cannot capture any outer variables (guaranteed no closure allocation)
public void ProcessEnemies(List<Enemy> enemies, float deltaTime)
{
    for (int i = 0; i < enemies.Count; i++)
        UpdateEnemy(enemies[i], deltaTime);
    
    static void UpdateEnemy(Enemy e, float dt) // static — can't close over anything
    {
        e.Position += e.Velocity * dt;
    }
}
```

---

## 19.10 — `CallerMemberName` and Debugging Attributes

```csharp
// [CallerMemberName] fills in the calling method's name automatically
public class DebugLogger
{
    public static void Log(string message, 
        [System.Runtime.CompilerServices.CallerMemberName] string callerName = "")
    {
        Debug.Log($"[{callerName}] {message}");
    }
}

// Usage
public class Player : MonoBehaviour
{
    void Update()
    {
        DebugLogger.Log("Player updated"); // prints "[Update] Player updated"
    }
    
    void OnDamaged()
    {
        DebugLogger.Log("Taking damage"); // prints "[OnDamaged] Taking damage"
    }
}
```

---

## 📝 Summary

| Feature | C# Version | Key Benefit |
| :--- | :--- | :--- |
| `record` / `record struct` | C# 9 | Immutable data containers with equality |
| `init` properties | C# 9 | Set-once, then immutable |
| `??=` assignment | C# 8 | Lazy initialization in one line |
| Pattern matching `switch` | C# 8-9 | Clean type-dispatch without casting |
| Property patterns | C# 8 | Match on object properties |
| Relational patterns `> < >=` | C# 9 | Range checks in patterns |
| Tuples `(a, b)` | C# 7 | Return multiple values cleanly |
| `using var` declaration | C# 8 | Auto-dispose without braces |
| `[^1]` index | C# 8 | Last element index |
| `[1..^1]` range | C# 8 | Array slicing |
| `Span<T>` | C# 7.2 | Zero-copy array views |
| Local functions | C# 7 | Scoped helpers, no closure risk |
| `static` local functions | C# 8 | Guaranteed zero capture |

**Previous:** [[18 — Structs for Performance]] | **Next:** [[20 — Extension Methods]]
