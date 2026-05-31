# 30 — Clean Code & SOLID in Unity
### 🔶 Block F — Architecture & Best Practices

> [!NOTE]
> The SOLID principles apply to Unity just as much as they apply to Android development. However, because Unity heavily encourages Component-based design, the way you implement SOLID looks slightly different. This file translates SOLID into Unity terms.

---

## 30.1 — Single Responsibility Principle (SRP)

**Concept:** A class should have one, and only one, reason to change.

### ❌ The Unity Anti-Pattern: The God Script
Beginners put everything on `Player.cs`.
```csharp
public class Player : MonoBehaviour
{
    void Update()
    {
        // 1. Input processing
        float move = Input.GetAxis("Horizontal");
        
        // 2. Physics movement
        GetComponent<Rigidbody2D>().linearVelocity = new Vector2(move, 0);
        
        // 3. Audio logic
        if (move != 0) GetComponent<AudioSource>().Play();
        
        // 4. Data/Health logic
        if (transform.position.y < -10) Die();
    }
}
```

### ✅ The Clean Way: Component Separation
Break the GameObject into many small, single-purpose MonoBehaviours.

- `PlayerInput.cs` (Only reads keyboard/gamepad and fires C# events).
- `PlayerMovement.cs` (Listens to input events and applies Rigidbody forces).
- `PlayerAudio.cs` (Listens to movement events and plays clips).
- `PlayerHealth.cs` (Manages HP, totally ignorant of input/movement).

**Benefit:** You can drag the `PlayerHealth.cs` component onto an Enemy GameObject, and it works perfectly! You can't do that with a God Script.

---

## 30.2 — Open/Closed Principle (OCP)

**Concept:** Classes should be open for extension, but closed for modification.

### ❌ The Unity Anti-Pattern: Huge Switch Statements
```csharp
public class WeaponManager : MonoBehaviour
{
    public void Attack(string weaponType)
    {
        if (weaponType == "Sword") { /* melee logic */ }
        else if (weaponType == "Bow") { /* projectile logic */ }
        else if (weaponType == "Magic") { /* AoE logic */ }
        // If we add a new weapon, we have to modify this file!
    }
}
```

### ✅ The Clean Way: Interfaces or ScriptableObjects
**Using Interfaces:**
```csharp
public interface IWeapon
{
    void Attack();
}

public class Sword : MonoBehaviour, IWeapon { public void Attack() { } }
public class Bow : MonoBehaviour, IWeapon { public void Attack() { } }

public class WeaponManager : MonoBehaviour
{
    private IWeapon currentWeapon;
    
    public void Attack()
    {
        currentWeapon.Attack(); // No switch statements!
    }
}
```

**Using ScriptableObjects (Strategy Pattern):**
Create an abstract `WeaponSO : ScriptableObject`, and derive `SwordSO` and `BowSO`. The `WeaponManager` just holds a reference to the abstract `WeaponSO` and calls `weapon.Attack()`.

---

## 30.3 — Liskov Substitution Principle (LSP)

**Concept:** Derived classes must be substitutable for their base classes.

### ❌ The Unity Anti-Pattern: Forcing Inheritance
```csharp
public class Enemy : MonoBehaviour
{
    public virtual void Move() { transform.Translate(Vector3.forward); }
}

public class TurretEnemy : Enemy
{
    public override void Move()
    {
        throw new NotImplementedException("Turrets can't move!"); 
        // ❌ Violation! A Turret is NOT a substitute for an Enemy if it crashes on Move().
    }
}
```

### ✅ The Clean Way: Favor Composition over Inheritance
In Unity, **Composition (Components)** is almost always better than deep class inheritance hierarchies.

Instead of `Enemy` -> `TurretEnemy`, create small components:
- `Health.cs`
- `Shooter.cs`
- `Mover.cs`

Put `Health` and `Shooter` on the Turret. Put `Health`, `Shooter`, and `Mover` on a standard enemy. No inheritance needed, no LSP violations.

---

## 30.4 — Interface Segregation Principle (ISP)

**Concept:** Don't force clients to depend on methods they don't use.

### ❌ The Unity Anti-Pattern: Fat Interfaces
```csharp
public interface IEntity
{
    void TakeDamage();
    void Heal();
    void Move();
    void Attack();
}

// A wooden crate can take damage, but it can't move or attack!
public class Crate : MonoBehaviour, IEntity
{
    public void TakeDamage() { Destroy(gameObject); }
    public void Heal() { } // Empty, useless
    public void Move() { } // Empty, useless
    public void Attack() { } // Empty, useless
}
```

### ✅ The Clean Way: Small, Specific Interfaces
```csharp
public interface IDamageable { void TakeDamage(int amount); }
public interface IMovable { void Move(Vector3 direction); }

public class Crate : MonoBehaviour, IDamageable
{
    public void TakeDamage(int amount) { Destroy(gameObject); }
}

// Now raycasts can just check for IDamageable!
if (hit.collider.TryGetComponent(out IDamageable target))
{
    target.TakeDamage(10);
}
```

---

## 30.5 — Dependency Inversion Principle (DIP)

**Concept:** High-level modules should not depend on low-level modules. Both should depend on abstractions (interfaces).

### ❌ The Unity Anti-Pattern: Concrete Dependencies
```csharp
public class Player : MonoBehaviour
{
    // High-level Player depends on low-level specific classes.
    // If we want to change from XML to JSON saving, we have to rewrite Player.cs!
    private XmlSaveSystem saveSystem;
    
    void Start()
    {
        saveSystem = new XmlSaveSystem();
    }
}
```

### ✅ The Clean Way: Depend on Abstractions
```csharp
public interface ISaveSystem
{
    void Save(PlayerData data);
}

public class Player : MonoBehaviour
{
    // Depends on the interface, not the implementation.
    private ISaveSystem saveSystem;
    
    // Injected via VContainer/Zenject (See File 29)
    [VContainer.Inject]
    public void Construct(ISaveSystem saveService)
    {
        this.saveSystem = saveService;
    }
}
```
Now you can swap `XmlSaveSystem` for `JsonSaveSystem` or `CloudSaveSystem` without changing a single line of `Player.cs`.

---

## 📝 Final Thoughts on Unity Architecture

1. **Avoid God Scripts:** Break your GameObjects into many small MonoBehaviours.
2. **Prefer Composition:** Add components instead of using `virtual`/`override` inheritance trees.
3. **Use Interfaces:** `IDamageable` and `IInteractable` are the backbones of clean Unity physics interactions.
4. **Decouple Data:** Use ScriptableObjects to hold stats and broadcast events.
5. **Decouple Logic:** Use MVP for UI, and DI (Service Locator / VContainer) for managers.

**You have reached the end of the Unity C# Vault.** 
You are now armed with not just the language syntax, but the architectural knowledge to build professional, scalable, memory-efficient games.

*Return to Index → [[00 — Index & Learning Path]]*
