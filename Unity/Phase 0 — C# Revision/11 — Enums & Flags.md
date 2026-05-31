# 11 — Enums & Flags
### 🟢 Block B — Unity-Critical C# Features

> [!NOTE]
> Enums are used everywhere in Unity — game states, damage types, item rarities, enemy types, animation states. Understanding `[Flags]` bitmasks lets you efficiently represent combinations like "this object can be hit by Fire AND Ice but not Lightning."

---

## 11.1 — Basic Enums

An enum is a named set of integer constants. Use them instead of "magic numbers" for state.

```csharp
// Basic enum declaration
public enum GameState
{
    MainMenu,    // = 0 (auto-assigned from 0 by default)
    Playing,     // = 1
    Paused,      // = 2
    GameOver,    // = 3
    Loading      // = 4
}

// Enum with explicit values
public enum DamageType
{
    Physical = 0,
    Fire     = 1,
    Ice      = 2,
    Lightning = 3,
    Poison   = 4
}

// Enum with underlying type specified (default is int)
public enum Priority : byte  // saves memory when storing many
{
    Low    = 0,
    Medium = 1,
    High   = 2,
    Critical = 3
}

// Using enums
GameState state = GameState.Playing;
DamageType dmgType = DamageType.Fire;

if (state == GameState.Playing)
{
    RunGameLoop();
}
```

---

## 11.2 — Enums in Switch Expressions

```csharp
// Clean mapping with switch expression
float GetDamageMultiplier(DamageType type) => type switch
{
    DamageType.Physical => 1.0f,
    DamageType.Fire     => 1.5f,
    DamageType.Ice      => 0.8f,
    DamageType.Lightning => 1.2f,
    DamageType.Poison   => 0.6f,
    _ => 1.0f
};

// Game state machine via switch
void Update()
{
    switch (currentState)
    {
        case GameState.Playing:
            UpdateGameplay();
            break;
        case GameState.Paused:
            UpdatePauseMenu();
            break;
        case GameState.GameOver:
            UpdateDeathScreen();
            break;
    }
}
```

---

## 11.3 — Enum as State Machine State

```csharp
public class EnemyAI : MonoBehaviour
{
    // Enemy states
    public enum EnemyState
    {
        Idle,
        Patrol,
        Chase,
        Attack,
        Stunned,
        Dead
    }
    
    private EnemyState currentState = EnemyState.Idle;
    private EnemyState previousState;
    
    void Update()
    {
        previousState = currentState;
        
        switch (currentState)
        {
            case EnemyState.Idle:
                HandleIdle();
                break;
            case EnemyState.Patrol:
                HandlePatrol();
                break;
            case EnemyState.Chase:
                HandleChase();
                break;
            case EnemyState.Attack:
                HandleAttack();
                break;
            case EnemyState.Stunned:
                HandleStunned();
                break;
            case EnemyState.Dead:
                return; // nothing to do
        }
        
        // Log transitions
        if (currentState != previousState)
            Debug.Log($"Enemy: {previousState} → {currentState}");
    }
    
    void TransitionTo(EnemyState newState)
    {
        OnExitState(currentState);
        currentState = newState;
        OnEnterState(currentState);
    }
    
    void OnEnterState(EnemyState state)
    {
        switch (state)
        {
            case EnemyState.Chase:
                animator.SetBool("IsRunning", true);
                break;
            case EnemyState.Dead:
                rb.simulated = false;
                animator.SetTrigger("Death");
                break;
        }
    }
    
    void OnExitState(EnemyState state)
    {
        switch (state)
        {
            case EnemyState.Chase:
                animator.SetBool("IsRunning", false);
                break;
        }
    }
    
    void HandleIdle()
    {
        if (CanSeePlayer()) TransitionTo(EnemyState.Chase);
        else if (ShouldStartPatrol()) TransitionTo(EnemyState.Patrol);
    }
}
```

---

## 11.4 — `[Flags]` Enums — Bitmasks

`[Flags]` enums use powers of 2 as values, so multiple flags can be combined into one integer using bitwise operators. This is how Unity's `LayerMask` works.

```csharp
// [Flags] attribute — MUST use powers of 2 as values!
[System.Flags]
public enum DamageResistance
{
    None      = 0,          // 0000
    Fire      = 1 << 0,     // 0001 = 1
    Ice       = 1 << 1,     // 0010 = 2
    Lightning = 1 << 2,     // 0100 = 4
    Poison    = 1 << 3,     // 1000 = 8
    Physical  = 1 << 4,     // 0001 0000 = 16
    
    // Combinations (optional convenience)
    Elemental = Fire | Ice | Lightning | Poison,
    All       = Fire | Ice | Lightning | Poison | Physical
}

public class Golem : MonoBehaviour
{
    // Inspector shows checkboxes for each flag when [Flags] is used!
    [SerializeField] private DamageResistance resistances = DamageResistance.Fire | DamageResistance.Ice;
    
    public void TakeDamage(float amount, DamageType damageType)
    {
        DamageResistance asFlag = (DamageResistance)(1 << (int)damageType);
        
        // Check if resistant using bitwise AND
        if ((resistances & asFlag) != 0)
        {
            amount *= 0.25f; // 75% reduction for resistant types
            Debug.Log("Golem is resistant to this damage type!");
        }
        
        health -= amount;
    }
    
    // Add a resistance at runtime
    void AddResistance(DamageResistance flag)   => resistances |= flag;   // bitwise OR
    
    // Remove a resistance at runtime
    void RemoveResistance(DamageResistance flag) => resistances &= ~flag; // AND with NOT
    
    // Check a specific resistance
    bool HasResistance(DamageResistance flag) => (resistances & flag) != 0;
    
    // Toggle a resistance
    void ToggleResistance(DamageResistance flag) => resistances ^= flag;  // XOR
}
```

---

## 11.5 — LayerMask (Unity's Built-In Flags Enum)

Unity's `LayerMask` is essentially a `[Flags]` enum with 32 layers.

```csharp
public class PlayerController : MonoBehaviour
{
    // Inspector exposes a layer selector
    [SerializeField] private LayerMask groundLayer;
    [SerializeField] private LayerMask enemyLayer;
    
    // Combine masks with bitwise OR
    LayerMask hitMask = groundLayer | enemyLayer;
    
    // Check ground with specific layer mask
    bool IsGrounded()
    {
        return Physics2D.OverlapCircle(groundCheck.position, 0.2f, groundLayer);
    }
    
    // Raycast that only hits enemies
    void ShootRay()
    {
        RaycastHit2D hit = Physics2D.Raycast(transform.position, Vector2.right, 10f, enemyLayer);
        if (hit.collider != null)
            hit.collider.GetComponent<IDamageable>()?.TakeDamage(damage);
    }
    
    // Create LayerMask from layer names in code
    LayerMask CreateMaskFromNames(params string[] layerNames)
    {
        int mask = 0;
        foreach (string name in layerNames)
            mask |= 1 << LayerMask.NameToLayer(name);
        return mask;
    }
}
```

---

## 11.6 — Enum Utilities

```csharp
// Convert enum to/from string
GameState state = GameState.Playing;
string stateName = state.ToString();          // "Playing"
GameState parsed = System.Enum.Parse<GameState>("Playing"); // GameState.Playing

// Convert enum to int and back
int stateInt = (int)state;                    // 1 (Playing)
GameState fromInt = (GameState)1;             // GameState.Playing

// Get all values of an enum
GameState[] allStates = (GameState[])System.Enum.GetValues(typeof(GameState));

// Check if a value is valid
bool isValid = System.Enum.IsDefined(typeof(GameState), someInt);

// Random enum value (useful for testing)
GameState randomState = allStates[Random.Range(0, allStates.Length)];
```

---

## 11.7 — Enums in ScriptableObjects (Common Pattern)

```csharp
// Item rarity shown in Inspector
public enum ItemRarity
{
    Common,
    Uncommon,
    Rare,
    Epic,
    Legendary
}

[CreateAssetMenu(menuName = "Game/Item")]
public class ItemData : ScriptableObject
{
    public string ItemName;
    public ItemRarity Rarity;
    [TextArea] public string Description;
    public Sprite Icon;
    public float DropChance;
    
    public Color GetRarityColor() => Rarity switch
    {
        ItemRarity.Common    => Color.white,
        ItemRarity.Uncommon  => Color.green,
        ItemRarity.Rare      => Color.blue,
        ItemRarity.Epic      => new Color(0.5f, 0f, 0.5f), // purple
        ItemRarity.Legendary => Color.yellow,
        _ => Color.white
    };
}
```

---

## 11.8 — 🎮 2D vs 3D: Enum Usage Differences

### 🎮 2D — Platformer States
```csharp
public enum PlayerState2D
{
    Idle,
    Running,
    Jumping,
    Falling,
    WallSliding,
    WallJumping,
    Crouching,
    Attacking,
    Hurt,
    Dead
}

// Movement direction as enum (common in 2D)
public enum FacingDirection { Left = -1, Right = 1 }

public class Player2D : MonoBehaviour
{
    private PlayerState2D state = PlayerState2D.Idle;
    private FacingDirection facing = FacingDirection.Right;
    
    void FlipSprite()
    {
        facing = facing == FacingDirection.Right ? FacingDirection.Left : FacingDirection.Right;
        spriteRenderer.flipX = facing == FacingDirection.Left;
    }
}
```

### 🎮 3D — Locomotion States
```csharp
public enum PlayerState3D
{
    Idle,
    Walking,
    Running,
    Sprinting,
    Jumping,
    Falling,
    Landing,
    Crouching,
    Sliding,
    Climbing,
    Swimming,
    Attacking,
    Aiming,
    Dead
}

// 3D specific: movement mode
public enum MovementMode
{
    Ground,
    Air,
    Water,
    Climbing
}

[System.Flags]
public enum PlayerAbility
{
    None        = 0,
    DoubleJump  = 1 << 0,
    WallRun     = 1 << 1,
    Dash        = 1 << 2,
    Glide       = 1 << 3,
    Swim        = 1 << 4
}
```

---

## 📝 Summary

| Concept | Key Rule |
| :--- | :--- |
| `enum` | Named integers — use for states, types, categories |
| `switch` on enum | Exhaustive pattern matching — compiler warns on missing cases |
| `[Flags]` | Combine multiple values — values MUST be powers of 2 |
| Bitwise `\|` | Combine flags: `Fire \| Ice` |
| Bitwise `&` | Check if flag is set: `(flags & Fire) != 0` |
| Bitwise `&= ~` | Remove a flag: `flags &= ~Fire` |
| `LayerMask` | Unity's built-in [Flags] — use in all physics queries |

**Previous:** [[10 — Interfaces in Unity]] | **Next:** [[12 — Coroutines & IEnumerator]]
