# 06 — Properties & Encapsulation
### 🟢 Block A — Language Foundations

> [!NOTE]
> Properties in Unity give you controlled access to data. They let the Inspector expose fields while keeping internal logic safe. Understanding them is needed for ScriptableObjects, data classes, and any system where you want to react when a value changes.

---

## 6.1 — Fields vs. Properties

```csharp
// FIELD — raw data storage, no logic
public float health; // ⚠️ public field — anyone can set it to -999f

// PROPERTY — controlled access with optional logic
public float Health
{
    get { return health; }   // read
    set { health = Mathf.Clamp(value, 0f, maxHealth); } // write with validation
}
```

---

## 6.2 — Auto-Properties

When you don't need a backing field or logic, use auto-properties. The compiler generates the hidden field for you.

```csharp
public class PlayerStats : MonoBehaviour
{
    // Full read/write auto-property (rarely used public/public in Unity)
    public float Speed { get; set; }
    
    // Public read, private write — most common pattern in Unity
    // Inspector can't set these — use [SerializeField] fields for Inspector access
    public float CurrentHealth { get; private set; }
    public bool IsAlive { get; private set; }
    public int Score { get; private set; }
    
    // Public read, protected write — subclasses can change, external code cannot
    public float MaxHealth { get; protected set; }
    
    void TakeDamage(float amount)
    {
        CurrentHealth -= amount;           // ✅ set inside class (private set)
        IsAlive = CurrentHealth > 0f;      // ✅
    }
}

// External code
PlayerStats stats = GetComponent<PlayerStats>();
float hp = stats.CurrentHealth;  // ✅ READ is public
stats.CurrentHealth = 99f;       // ❌ COMPILE ERROR — private set
```

---

## 6.3 — Properties with Backing Fields and Validation

```csharp
public class PlayerHealth : MonoBehaviour
{
    [SerializeField] private float _maxHealth = 100f;
    private float _currentHealth;
    
    // Property with validation — clamp health between 0 and max
    public float CurrentHealth
    {
        get => _currentHealth;
        private set
        {
            _currentHealth = Mathf.Clamp(value, 0f, _maxHealth);
            OnHealthChanged?.Invoke(_currentHealth); // notify UI, effects etc.
            
            if (_currentHealth <= 0f)
                Die();
        }
    }
    
    public event System.Action<float> OnHealthChanged;
    
    // MaxHealth property — update current health if max is reduced
    public float MaxHealth
    {
        get => _maxHealth;
        set
        {
            _maxHealth = Mathf.Max(1f, value); // max must be at least 1
            // Clamp current health to new max
            CurrentHealth = Mathf.Min(_currentHealth, _maxHealth);
        }
    }
    
    public float HealthPercent => _currentHealth / _maxHealth; // computed, no backing field
    
    void Awake()
    {
        CurrentHealth = _maxHealth; // triggers the setter validation on init
    }
    
    public void Heal(float amount) => CurrentHealth += amount; // triggers setter, auto-clamped
    public void TakeDamage(float amount) => CurrentHealth -= amount; // triggers setter
}
```

---

## 6.4 — Expression-Bodied Properties

For simple computed properties, use the `=>` shorthand.

```csharp
public class PlayerController : MonoBehaviour
{
    private Rigidbody2D rb;
    private float health;
    private float maxHealth = 100f;
    
    // Computed properties — calculated on demand, no backing field
    public bool IsAlive => health > 0f;
    public bool IsFullHealth => health >= maxHealth;
    public float HealthPercent => health / maxHealth;
    public bool IsMoving => rb.linearVelocity.sqrMagnitude > 0.01f;
    public Vector2 Velocity => rb.linearVelocity;
    
    // Computed from child transform
    public Vector3 WeaponPosition => weaponMount.position;
    
    // String computed properties for UI
    public string HealthText => $"{Mathf.CeilToInt(health)}/{maxHealth}";
}
```

---

## 6.5 — `init` Only Setters (C# 9 / Unity 2021+)

`init` properties can only be set during object initialization. After that, they're immutable like `readonly`.

```csharp
// Immutable weapon configuration
public class WeaponConfig
{
    public string Name { get; init; }
    public float Damage { get; init; }
    public float FireRate { get; init; }
    public float Range { get; init; }
    public bool IsAutomatic { get; init; }
}

// Must be set at creation — cannot change after
WeaponConfig pistol = new WeaponConfig
{
    Name = "Pistol",
    Damage = 25f,
    FireRate = 2f,
    Range = 50f,
    IsAutomatic = false
};

// pistol.Damage = 30f; // ❌ COMPILE ERROR — init-only

// Great for ScriptableObject wrappers or config structs
public record struct EnemyStatSheet(float speed, int maxHealth, string faction);
```

---

## 6.6 — Properties for the Inspector (`[SerializeField]`)

> [!IMPORTANT]
> Unity's Inspector cannot show **properties** — it only shows **fields**. But you can expose a private field via `[SerializeField]` while keeping a property for code access. This is the professional pattern.

```csharp
public class EnemyAI : MonoBehaviour
{
    // Inspector-visible private fields (use [SerializeField])
    [Header("Stats")]
    [SerializeField, Range(1f, 20f)] private float _moveSpeed = 5f;
    [SerializeField, Range(10f, 500f)] private float _maxHealth = 100f;
    [SerializeField] private float _detectionRange = 8f;
    
    // Properties provide safe code access
    // External scripts read these, but only this class writes them
    public float MoveSpeed => _moveSpeed;
    public float MaxHealth => _maxHealth;
    public float DetectionRange => _detectionRange;
    
    // Runtime state — not inspector-visible, but code-accessible
    public float CurrentHealth { get; private set; }
    public bool IsAggro { get; private set; }
    
    void Start()
    {
        CurrentHealth = _maxHealth;
    }
}
```

---

## 6.7 — Static Properties

```csharp
public class GameManager : MonoBehaviour
{
    // Singleton exposed via static property
    private static GameManager _instance;
    public static GameManager Instance
    {
        get
        {
            if (_instance == null)
                Debug.LogError("GameManager is null! Did you forget to add it to the scene?");
            return _instance;
        }
    }
    
    // Static state properties
    public static bool IsGamePaused { get; private set; }
    public static int CurrentLevel { get; private set; } = 1;
    
    void Awake()
    {
        if (_instance != null && _instance != this)
        {
            Destroy(gameObject);
            return;
        }
        _instance = this;
        DontDestroyOnLoad(gameObject);
    }
    
    public static void SetPaused(bool paused)
    {
        IsGamePaused = paused;
        Time.timeScale = paused ? 0f : 1f;
    }
}
```

---

## 6.8 — 🎮 2D vs 3D: Useful Computed Properties

### 🎮 2D Useful Properties
```csharp
public class Player2D : MonoBehaviour
{
    private Rigidbody2D rb;
    private SpriteRenderer spriteRenderer;
    
    // Movement state
    public bool IsMovingRight => rb.linearVelocity.x > 0.1f;
    public bool IsMovingLeft => rb.linearVelocity.x < -0.1f;
    public bool IsFalling => rb.linearVelocity.y < -0.1f;
    public bool IsRising => rb.linearVelocity.y > 0.1f;
    
    // Screen position for UI elements
    public Vector2 ScreenPosition => Camera.main.WorldToScreenPoint(transform.position);
    
    // Direction the sprite is facing
    public float FacingDirection => spriteRenderer.flipX ? -1f : 1f;
    
    // 2D distance to any point
    public float DistanceTo(Vector2 point) => Vector2.Distance(transform.position, point);
}
```

### 🎮 3D Useful Properties
```csharp
public class Player3D : MonoBehaviour
{
    private Rigidbody rb;
    private CharacterController cc;
    
    // 3D movement state
    public bool IsMoving => rb.linearVelocity.magnitude > 0.1f;
    public bool IsFalling => rb.linearVelocity.y < -0.5f;
    
    // Forward/right relative movement (used for strafing animation)
    public float ForwardSpeed => Vector3.Dot(rb.linearVelocity, transform.forward);
    public float RightSpeed => Vector3.Dot(rb.linearVelocity, transform.right);
    
    // 3D distance to point (ignoring Y — useful for AI detection on flat ground)
    public float FlatDistanceTo(Vector3 point)
    {
        Vector3 flat = point - transform.position;
        flat.y = 0f;
        return flat.magnitude;
    }
    
    // Height above ground (for jump/fall state)
    public float HeightAboveGround
    {
        get
        {
            Physics.Raycast(transform.position, Vector3.down, out RaycastHit hit, 10f);
            return hit.collider != null ? hit.distance : float.MaxValue;
        }
    }
}
```

---

## 📝 Summary

| Pattern | Syntax | Use Case |
| :--- | :--- | :--- |
| Auto-property | `public float X { get; set; }` | Simple read/write with no logic |
| Private setter | `public float X { get; private set; }` | External read-only, internal write |
| Backing field | `private float _x; public float X { get => _x; set => _x = ... }` | Need validation or side effects |
| Expression-bodied | `public bool IsAlive => health > 0f;` | Computed, read-only, no backing field |
| `init` only | `public string Name { get; init; }` | Set once at creation, then immutable |
| `[SerializeField]` | `[SerializeField] private float _speed;` | Inspector-visible but code-encapsulated |

**Previous:** [[05 — OOP — Inheritance & Polymorphism]] | **Next:** [[07 — Generics & Constraints]]
