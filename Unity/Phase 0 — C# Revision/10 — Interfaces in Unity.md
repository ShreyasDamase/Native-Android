# 10 — Interfaces in Unity
### 🟡 Block B — Unity-Critical C# Features

> [!NOTE]
> Interfaces are one of Unity's most powerful architectural tools. `GetComponent<IDamageable>()` lets a bullet damage anything — an enemy, a barrel, a wall — without knowing the specific type. Design systems around interfaces to make them modular and extensible.

---

## 10.1 — Defining Interfaces

```csharp
// Interface naming convention: always prefix with 'I'
public interface IDamageable
{
    // Properties in interfaces
    float CurrentHealth { get; }
    float MaxHealth { get; }
    bool IsAlive { get; }
    
    // Methods in interfaces — no body
    void TakeDamage(float amount);
    void Heal(float amount);
}

public interface IKillable
{
    void Die();
    event System.Action OnDied;
}

public interface IInteractable
{
    void Interact(PlayerController player);
    string GetInteractPrompt();
    bool CanInteract { get; }
}

public interface IPoolable
{
    void OnSpawn();
    void OnDespawn();
}

public interface IMovable
{
    void MoveTo(Vector3 destination);
    void Stop();
    float MoveSpeed { get; set; }
}
```

---

## 10.2 — Implementing Interfaces

A class can implement **multiple** interfaces (unlike single inheritance).

```csharp
// Enemy implements IDamageable, IKillable, and IMovable
public class Enemy : MonoBehaviour, IDamageable, IKillable, IMovable
{
    [SerializeField] private float maxHealth = 100f;
    private float currentHealth;
    private Rigidbody2D rb;
    
    // IDamageable
    public float CurrentHealth => currentHealth;
    public float MaxHealth => maxHealth;
    public bool IsAlive => currentHealth > 0f;
    
    public void TakeDamage(float amount)
    {
        currentHealth -= amount;
        PlayHitEffect();
        if (currentHealth <= 0f) Die();
    }
    
    public void Heal(float amount)
    {
        currentHealth = Mathf.Min(currentHealth + amount, maxHealth);
    }
    
    // IKillable
    public event System.Action OnDied;
    
    public void Die()
    {
        OnDied?.Invoke();
        DropLoot();
        Destroy(gameObject, 0.5f);
    }
    
    // IMovable
    public float MoveSpeed { get; set; } = 3f;
    
    public void MoveTo(Vector3 destination)
    {
        Vector2 dir = (destination - transform.position).normalized;
        rb.linearVelocity = dir * MoveSpeed;
    }
    
    public void Stop() => rb.linearVelocity = Vector2.zero;
    
    void Awake()
    {
        rb = GetComponent<Rigidbody2D>();
        currentHealth = maxHealth;
    }
}
```

---

## 10.3 — Using Interfaces with GetComponent

This is the key advantage of interfaces in Unity — `GetComponent<T>()` works with interface types.

```csharp
// Bullet doesn't need to know what it hits — just IDamageable
public class Bullet : MonoBehaviour, IPoolable
{
    [SerializeField] private float damage = 25f;
    [SerializeField] private LayerMask hitMask;
    private ObjectPool<Bullet> pool;
    
    void OnTriggerEnter2D(Collider2D other)
    {
        // Does this object support being damaged?
        if (other.TryGetComponent<IDamageable>(out IDamageable target))
        {
            target.TakeDamage(damage);
        }
        
        // Return to pool
        pool?.Return(this);
    }
    
    // IPoolable
    public void OnSpawn() { gameObject.SetActive(true); }
    public void OnDespawn() { gameObject.SetActive(false); }
    
    public void Initialize(ObjectPool<Bullet> ownerPool) => pool = ownerPool;
}

// Barrel — also damageable, but different behavior
public class Barrel : MonoBehaviour, IDamageable, IKillable
{
    private float health = 50f;
    
    public float CurrentHealth => health;
    public float MaxHealth => 50f;
    public bool IsAlive => health > 0f;
    
    public event System.Action OnDied;
    
    public void TakeDamage(float amount)
    {
        health -= amount;
        if (health <= 0f) Die();
    }
    
    public void Heal(float amount) { /* barrels don't heal */ }
    
    public void Die()
    {
        OnDied?.Invoke();
        Explode();  // barrel-specific behavior
        Destroy(gameObject);
    }
    
    void Explode()
    {
        // Deal area damage in radius
        Collider2D[] hits = Physics2D.OverlapCircleAll(transform.position, 3f);
        foreach (Collider2D hit in hits)
        {
            if (hit.TryGetComponent<IDamageable>(out IDamageable d))
                d.TakeDamage(50f);
        }
    }
}
```

---

## 10.4 — Interface-Driven Interaction System

```csharp
// Interaction system works with ANY interactable — chest, NPC, door, lever...
public class InteractionSystem : MonoBehaviour
{
    [SerializeField] private float interactRange = 2f;
    [SerializeField] private LayerMask interactableMask;
    
    private IInteractable currentInteractable;
    
    void Update()
    {
        ScanForInteractable();
        
        if (currentInteractable != null && Input.GetKeyDown(KeyCode.E))
        {
            if (currentInteractable.CanInteract)
                currentInteractable.Interact(GetComponent<PlayerController>());
        }
    }
    
    void ScanForInteractable()
    {
        Collider2D closest = Physics2D.OverlapCircle(transform.position, interactRange, interactableMask);
        
        if (closest != null && closest.TryGetComponent<IInteractable>(out IInteractable found))
        {
            currentInteractable = found;
            ShowPrompt(found.GetInteractPrompt());
        }
        else
        {
            currentInteractable = null;
            HidePrompt();
        }
    }
}

// Chest — implements IInteractable
public class Chest : MonoBehaviour, IInteractable
{
    [SerializeField] private ItemData[] loot;
    private bool opened = false;
    
    public bool CanInteract => !opened;
    
    public void Interact(PlayerController player)
    {
        opened = true;
        player.AddItems(loot);
        PlayOpenAnimation();
    }
    
    public string GetInteractPrompt() => opened ? "" : "Press E to open chest";
}

// NPC — also IInteractable, different behavior
public class NPC : MonoBehaviour, IInteractable
{
    [SerializeField] private DialogueData dialogue;
    
    public bool CanInteract => true; // always interactable
    
    public void Interact(PlayerController player)
    {
        DialogueManager.Instance.StartDialogue(dialogue, player);
    }
    
    public string GetInteractPrompt() => $"Talk to {gameObject.name}";
}
```

---

## 10.5 — Default Interface Methods (C# 8+)

Interfaces can now have default implementations. Use sparingly — can cause confusion.

```csharp
public interface IDamageable
{
    float CurrentHealth { get; }
    void TakeDamage(float amount);
    
    // Default implementation — classes get this for free unless they override
    public bool IsAlive => CurrentHealth > 0f;
    
    public void TakePercentDamage(float percent)
    {
        TakeDamage(CurrentHealth * percent);
    }
}
```

---

## 10.6 — Interface Segregation (Good Design)

Don't create fat interfaces. Split them into small, focused ones.

```csharp
// ❌ BAD — bloated interface — not everything can do all of this
public interface IGameObject
{
    void TakeDamage(float amount);
    void Heal(float amount);
    void Move(Vector3 destination);
    void PlayAnimation(string name);
    void SaveState();
    void LoadState();
}

// ✅ GOOD — small, focused interfaces
public interface IDamageable { void TakeDamage(float amount); }
public interface IHealable   { void Heal(float amount); }
public interface IMovable    { void MoveTo(Vector3 destination); }
public interface ISaveable   { void Save(); void Load(); }

// A class implements only what it needs
public class Player : MonoBehaviour, IDamageable, IHealable, IMovable, ISaveable { }
public class StaticBarrel : MonoBehaviour, IDamageable { } // only damageable
public class Checkpoint : MonoBehaviour, ISaveable { }     // only saveable
```

---

## 10.7 — 🎮 2D vs 3D: Interface Patterns

### 🎮 2D — Platformer Interfaces
```csharp
public interface IJumpable
{
    void Jump(float force);
    bool IsGrounded { get; }
    int JumpsRemaining { get; }  // for double jump
}

public interface IWallClimbable
{
    bool IsOnWall { get; }
    void WallJump(Vector2 direction, float force);
}

public interface ICollectible
{
    void Collect(Player2D player);
    int PointValue { get; }
}

// Coin implements ICollectible
public class Coin : MonoBehaviour, ICollectible
{
    [SerializeField] private int value = 10;
    public int PointValue => value;
    
    public void Collect(Player2D player)
    {
        player.AddCoins(value);
        PlayCollectFX();
        Destroy(gameObject);
    }
}
```

### 🎮 3D — FPS/TPS Interfaces
```csharp
public interface IShootable
{
    void OnBulletHit(Vector3 hitPoint, Vector3 hitNormal, float damage);
}

public interface IAimable
{
    void AimAt(Vector3 targetPosition);
    float AimAccuracy { get; }
}

public interface ILootable
{
    void OpenLootUI(PlayerController3D player);
    bool HasLoot { get; }
    List<ItemData> GetLoot();
}

// 3D damageable enemy
public class Enemy3D : MonoBehaviour, IDamageable, IShootable
{
    public float CurrentHealth { get; private set; } = 100f;
    public float MaxHealth => 100f;
    public bool IsAlive => CurrentHealth > 0f;
    
    public void TakeDamage(float amount) => CurrentHealth -= amount;
    
    public void OnBulletHit(Vector3 hitPoint, Vector3 hitNormal, float damage)
    {
        // Spawn hit decal in 3D
        SpawnHitDecal(hitPoint, hitNormal);
        TakeDamage(damage);
    }
}
```

---

## 📝 Summary

| Pattern | Code | Use Case |
| :--- | :--- | :--- |
| Define interface | `public interface IFoo { void Bar(); }` | Define a capability contract |
| Implement interface | `class A : IFoo { public void Bar() {} }` | Fulfill the contract |
| Query by interface | `GetComponent<IDamageable>()` | Treat different types the same way |
| Multiple interfaces | `class A : IFoo, IBar, IBaz {}` | Compose behaviors without multiple inheritance |
| Default impl | `interface IFoo { void Bar() { } }` | Optional default — use sparingly |

**Previous:** [[09 — Delegates, Actions & Events]] | **Next:** [[11 — Enums & Flags]]
