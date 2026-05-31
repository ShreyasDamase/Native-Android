# 05 — OOP — Inheritance & Polymorphism
### 🟡 Block A — Language Foundations

> [!NOTE]
> Inheritance in Unity is used to create hierarchies of enemies, weapons, items, and UI elements. Understanding `virtual`, `override`, and `abstract` lets you build clean, extensible game systems that don't duplicate code.

---

## 5.1 — Inheritance Basics

A class can `inherit` from a base class, gaining all its fields and methods.

```csharp
// BASE CLASS — shared behavior for all characters
public class Character : MonoBehaviour
{
    [SerializeField] protected float maxHealth = 100f;
    protected float currentHealth;
    protected Animator animator;
    
    protected virtual void Awake()
    {
        currentHealth = maxHealth;
        animator = GetComponent<Animator>();
    }
    
    public virtual void TakeDamage(float amount)
    {
        currentHealth -= amount;
        animator.SetTrigger("Hit");
        
        if (currentHealth <= 0f)
            Die();
    }
    
    protected virtual void Die()
    {
        animator.SetTrigger("Death");
        Destroy(gameObject, 2f);
    }
}
```

```csharp
// DERIVED CLASS — Player adds player-specific behavior
public class Player : Character
{
    private float stamina = 100f;
    
    protected override void Awake()
    {
        base.Awake();   // Call parent Awake first!
        // Player-specific initialization
        LoadSavedData();
    }
    
    // Override TakeDamage — player has armor reduction
    public override void TakeDamage(float amount)
    {
        float reducedDamage = amount * (1f - GetArmorReduction());
        base.TakeDamage(reducedDamage); // Call parent TakeDamage with new amount
    }
    
    // Override Die — player respawns instead of being destroyed
    protected override void Die()
    {
        animator.SetTrigger("Death");
        StartCoroutine(RespawnRoutine());
        // Note: we do NOT call base.Die() because we don't want Destroy
    }
    
    private float GetArmorReduction() => 0.2f; // 20% damage reduction
}
```

```csharp
// ANOTHER DERIVED CLASS — Enemy has AI-specific behavior
public class Enemy : Character
{
    [SerializeField] private int scoreValue = 50;
    
    protected override void Die()
    {
        base.Die();   // Call parent die (plays animation, destroys)
        GameManager.Instance.AddScore(scoreValue);
        DropLoot();
    }
}
```

---

## 5.2 — `virtual`, `override`, `abstract`, `sealed`

| Keyword | Where It Goes | What It Means |
| :--- | :--- | :--- |
| `virtual` | Base class method | "Subclasses CAN override this, but don't have to" |
| `override` | Derived class method | "I am replacing the base class version" |
| `abstract` | Base class method | "Subclasses MUST override this — no default implementation" |
| `sealed` | Derived class or method | "No further overriding allowed after this" |
| `base` | Derived class | "Call the parent class's version of this method" |

```csharp
// Abstract class — cannot be instantiated directly
public abstract class Weapon : MonoBehaviour
{
    [SerializeField] protected float damage = 10f;
    [SerializeField] protected float fireRate = 1f;
    
    private float nextFireTime;
    
    // Abstract method — MUST be implemented by subclasses
    public abstract void Fire();
    
    // Virtual method — has a default implementation but CAN be overridden
    public virtual bool CanFire()
    {
        return Time.time >= nextFireTime;
    }
    
    // Non-virtual method — cannot be overridden (sealed by default in non-abstract)
    protected void ResetCooldown()
    {
        nextFireTime = Time.time + (1f / fireRate);
    }
}

// Concrete implementation — MustFire because Fire() is abstract
public class Pistol : Weapon
{
    [SerializeField] private GameObject bulletPrefab;
    [SerializeField] private Transform muzzle;
    
    public override void Fire()
    {
        if (!CanFire()) return;
        
        Instantiate(bulletPrefab, muzzle.position, muzzle.rotation);
        ResetCooldown();
    }
}

public class Shotgun : Weapon
{
    [SerializeField] private int pelletCount = 8;
    [SerializeField] private float spread = 15f;
    
    public override void Fire()
    {
        if (!CanFire()) return;
        
        for (int i = 0; i < pelletCount; i++)
        {
            float angle = Random.Range(-spread, spread);
            Quaternion pelletRotation = muzzle.rotation * Quaternion.Euler(0f, angle, 0f);
            Instantiate(bulletPrefab, muzzle.position, pelletRotation);
        }
        ResetCooldown();
    }
    
    // Override CanFire to add ammo check
    public override bool CanFire()
    {
        return base.CanFire() && currentAmmo > 0;
    }
}
```

---

## 5.3 — Interfaces (Contracts)

An interface defines **what a class can do**, not how it does it. A class can implement multiple interfaces (unlike inheritance — only one base class).

```csharp
// Define capability interfaces
public interface IDamageable
{
    void TakeDamage(float amount);
    float CurrentHealth { get; }
}

public interface IInteractable
{
    void Interact(PlayerController player);
    string GetPromptText();
}

public interface IPoolable
{
    void OnSpawn();
    void OnDespawn();
}
```

```csharp
// A class implementing multiple interfaces
public class Chest : MonoBehaviour, IInteractable, IDamageable
{
    private float health = 50f;
    private bool isOpen = false;
    
    // IInteractable implementation
    public void Interact(PlayerController player)
    {
        if (!isOpen)
        {
            OpenChest(player);
            isOpen = true;
        }
    }
    
    public string GetPromptText() => isOpen ? "" : "Press E to open";
    
    // IDamageable implementation
    public void TakeDamage(float amount)
    {
        health -= amount;
        if (health <= 0f) Destroy(gameObject);
    }
    
    public float CurrentHealth => health;
}
```

```csharp
// Power of interfaces — the bullet doesn't need to know what it hit
public class Bullet : MonoBehaviour, IPoolable
{
    [SerializeField] private float damage = 10f;
    
    void OnTriggerEnter2D(Collider2D other)
    {
        // Check if the hit object implements IDamageable — don't care what TYPE it is
        if (other.TryGetComponent<IDamageable>(out IDamageable target))
        {
            target.TakeDamage(damage);
        }
        
        // Return to pool
        OnDespawn();
    }
    
    public void OnSpawn() { gameObject.SetActive(true); }
    public void OnDespawn() { gameObject.SetActive(false); }
}
```

---

## 5.4 — The `is` and `as` Operators

```csharp
void ProcessObject(MonoBehaviour obj)
{
    // 'is' — check if obj implements/inherits a type
    if (obj is IDamageable)
    {
        Debug.Log("This object can be damaged");
    }
    
    // 'is' with pattern matching — check AND cast in one step (preferred)
    if (obj is Enemy enemy)
    {
        enemy.Alert(); // 'enemy' is cast, safe to use
    }
    
    // 'as' — returns null if the cast fails (doesn't throw)
    IDamageable damageable = obj as IDamageable;
    if (damageable != null)
    {
        damageable.TakeDamage(10f);
    }
    
    // GetComponent<Interface>() — Unity's way to query by interface
    IDamageable comp = GetComponent<IDamageable>(); // works on any component!
}
```

---

## 5.5 — Polymorphism in Practice

Polymorphism means "different types, same interface." One list can hold many different types that share a common base.

```csharp
public class EnemySpawner : MonoBehaviour
{
    // ONE list holds Goblins, Orcs, Bosses — all derived from Enemy
    private List<Enemy> allEnemies = new List<Enemy>();
    
    void DamageAllEnemies(float amount)
    {
        // TakeDamage() calls the correct OVERRIDE for each specific type
        foreach (Enemy e in allEnemies)
        {
            e.TakeDamage(amount); // Goblin, Orc, and Boss each handle this differently
        }
    }
}

// Same power with interfaces
public class InteractionSystem : MonoBehaviour
{
    // Detects ALL interactables regardless of their type
    void ScanForInteractables()
    {
        Collider2D[] hits = Physics2D.OverlapCircleAll(transform.position, 2f);
        
        foreach (Collider2D hit in hits)
        {
            if (hit.TryGetComponent<IInteractable>(out IInteractable item))
            {
                ShowPrompt(item.GetPromptText()); // Works for Chest, NPC, Door, all!
            }
        }
    }
}
```

---

## 5.6 — 🎮 2D vs 3D: Enemy Hierarchies

### 🎮 2D Enemy Base
```csharp
public abstract class Enemy2D : MonoBehaviour
{
    protected Rigidbody2D rb;
    protected Animator animator;
    protected Transform player;
    
    [SerializeField] protected float health = 100f;
    [SerializeField] protected float moveSpeed = 2f;
    [SerializeField] protected float detectionRange = 8f;
    
    protected virtual void Awake()
    {
        rb = GetComponent<Rigidbody2D>();
        animator = GetComponent<Animator>();
    }
    
    protected virtual void Start()
    {
        player = FindFirstObjectByType<PlayerController2D>().transform;
    }
    
    // Abstract — each enemy moves differently
    protected abstract void Move();
    
    protected bool CanSeePlayer()
    {
        float dist = Vector2.Distance(transform.position, player.position);
        return dist < detectionRange;
    }
    
    void FixedUpdate() { if (CanSeePlayer()) Move(); }
}

// Specific 2D enemy
public class Slime2D : Enemy2D
{
    protected override void Move()
    {
        Vector2 dir = (player.position - transform.position).normalized;
        rb.linearVelocity = dir * moveSpeed;
    }
}
```

### 🎮 3D Enemy Base
```csharp
public abstract class Enemy3D : MonoBehaviour
{
    protected Rigidbody rb;
    protected NavMeshAgent agent;  // 3D uses NavMesh for pathfinding
    protected Transform player;
    
    [SerializeField] protected float health = 100f;
    [SerializeField] protected float detectionRange = 10f;
    
    protected virtual void Awake()
    {
        rb = GetComponent<Rigidbody>();
        agent = GetComponent<NavMeshAgent>();
    }
    
    protected virtual void Start()
    {
        player = FindFirstObjectByType<PlayerController3D>().transform;
    }
    
    protected abstract void Move();
    
    protected bool CanSeePlayer()
    {
        float dist = Vector3.Distance(transform.position, player.position);
        if (dist > detectionRange) return false;
        
        // 3D line-of-sight check (important in 3D, less common in 2D)
        Vector3 dir = (player.position - transform.position).normalized;
        return !Physics.Raycast(transform.position, dir, dist, obstacleMask);
    }
    
    void Update() { if (CanSeePlayer()) Move(); }
}

// Specific 3D enemy — uses NavMesh pathfinding
public class Zombie3D : Enemy3D
{
    protected override void Move()
    {
        agent.SetDestination(player.position); // NavMesh handles pathfinding!
    }
}
```

---

## 📝 Summary

| Keyword | Meaning |
| :--- | :--- |
| `virtual` | Can be overridden (has default implementation) |
| `override` | Replaces the base class version |
| `abstract` | Must be overridden (no default implementation) |
| `sealed` | Cannot be further overridden |
| `base` | Calls the parent class's version |
| `interface` | Defines capability — allows multiple "types" |
| `is` pattern | Safe type check + cast in one step |
| `as` | Soft cast — returns null instead of throwing |

**Previous:** [[04 — Classes & Structs]] | **Next:** [[06 — Properties & Encapsulation]]
