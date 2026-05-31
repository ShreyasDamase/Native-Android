# 02 — Control Flow & Pattern Matching
### 🟢 Block A — Language Foundations

> [!NOTE]
> Control flow in C# is nearly identical to Java/Kotlin — so this chapter is mostly a quick refresh. The key new additions are **switch expressions** and **pattern matching**, which are extremely popular in modern Unity game state code.

---

## 2.1 — `if / else if / else`

The foundation. Same as every language, but note C#'s `&&` (and), `||` (or), `!` (not).

```csharp
public class PlayerController : MonoBehaviour
{
    private float health = 100f;
    private bool isGrounded;
    private bool hasShield;

    void Update()
    {
        if (health <= 0f)
        {
            Die();
        }
        else if (health < 25f && !hasShield)
        {
            TriggerLowHealthEffect();
        }
        else
        {
            ResumeNormal();
        }
    }
}
```

---

## 2.2 — `switch` Statement (Classic)

Use when checking one variable against many possible specific values.

```csharp
public enum GameState { MainMenu, Playing, Paused, GameOver }

void HandleState(GameState state)
{
    switch (state)
    {
        case GameState.MainMenu:
            ShowMainMenu();
            break;
        case GameState.Playing:
            RunGameLoop();
            break;
        case GameState.Paused:
            ShowPauseMenu();
            break;
        case GameState.GameOver:
            ShowGameOver();
            break;
        default:
            Debug.LogWarning($"Unhandled state: {state}");
            break;
    }
}
```

---

## 2.3 — `switch` Expression (Modern — Preferred in Unity 6)

Switch expressions return a value directly. Much cleaner for data mappings like difficulty, item tiers, or damage types.

```csharp
// Classic switch for mapping a value
string GetDifficultyLabel(int difficulty)
{
    return difficulty switch
    {
        1 => "Easy",
        2 => "Normal",
        3 => "Hard",
        4 => "Nightmare",
        _ => "Unknown"   // _ is the default/fallback case
    };
}

// Used inline to set properties
float damageMultiplier = currentDifficulty switch
{
    1 => 0.75f,
    2 => 1.0f,
    3 => 1.5f,
    _ => 1.0f
};

// Pattern matching with switch expression — check type AND properties
float CalculateDamage(IDamageable target) => target switch
{
    Player p when p.HasShield => 0f,           // Player with shield takes no damage
    Player p                  => p.Defense * 0.5f,
    Enemy e                   => e.Armor,
    _                         => 10f            // fallback
};
```

---

## 2.4 — Pattern Matching with `is`

Pattern matching lets you check the type of an object AND cast it simultaneously.

```csharp
// Old style (verbose)
void OnTriggerEnter2D(Collider2D other)
{
    if (other.gameObject.GetComponent<Enemy>() != null)
    {
        Enemy e = other.gameObject.GetComponent<Enemy>(); // double GetComponent!
        e.TakeDamage(10f);
    }
}

// Modern pattern matching (single GetComponent, type-safe)
void OnTriggerEnter2D(Collider2D other)
{
    if (other.TryGetComponent<Enemy>(out Enemy enemy))
    {
        enemy.TakeDamage(10f);
    }
}

// 'is' pattern — check and cast in one line
void ProcessCollectable(MonoBehaviour mono)
{
    if (mono is Coin coin)
    {
        score += coin.Value;  // coin is already cast here
    }
    else if (mono is HealthPack pack)
    {
        health = Mathf.Min(health + pack.HealAmount, maxHealth);
    }
}
```

---

## 2.5 — Null Checks — The Right Way in Unity

Unity has its OWN null-handling that is different from standard C#. This is one of the most confusing things for beginners.

```csharp
// ✅ Safe null check — Unity's overloaded == operator
if (myObject == null) { }

// ⚠️ Unsafe for Unity objects — bypasses Unity's == override
// A destroyed GameObject reports as null with == but NOT with 'is null'
if (myObject is null) { }       // DANGEROUS for Unity objects
if (myObject is not null) { }   // DANGEROUS for Unity objects

// ✅ Null-coalescing — gives fallback value if null
string name = player?.name ?? "No Player";

// ✅ Null-conditional — safely chain properties, returns null instead of crashing
float? hp = player?.GetComponent<Health>()?.CurrentHP;
```

> [!WARNING]
> Never use `is null` or `is not null` on `GameObject`, `Component`, or `MonoBehaviour`. Unity overrides `==` to catch destroyed objects, but `is null` bypasses that override and won't detect them.

---

## 2.6 — Loops

### `for` Loop — Best for Array Iteration (Zero Allocation)
```csharp
// ✅ Preferred for performance — no IEnumerator allocation
int[] enemies = GetActiveEnemies();
for (int i = 0; i < enemies.Length; i++)
{
    enemies[i].TakeDamage(damageAmount);
}
```

### `foreach` Loop — Clean but Allocates on Arrays
```csharp
// ⚠️ foreach on arrays/lists allocates an IEnumerator object each time
// OK in editor code, avoid in hot paths (Update, FixedUpdate)
foreach (Enemy e in enemies)
{
    e.TakeDamage(damageAmount);
}

// ✅ foreach on List<T> does NOT allocate — List uses a struct enumerator
List<Enemy> enemyList = new List<Enemy>();
foreach (Enemy e in enemyList)  // Safe! No allocation.
{
    e.TakeDamage(damageAmount);
}
```

### `while` — Use for Time-Based or Condition-Based Loops
```csharp
// Classic while
while (wavesRemaining > 0)
{
    SpawnWave();
    wavesRemaining--;
}

// do-while — guaranteed to run at least once
do
{
    GenerateRoomLayout();
} while (!IsRoomValid());
```

---

## 2.7 — Ternary Operator

One-line if/else that produces a value.

```csharp
// Syntax: condition ? valueIfTrue : valueIfFalse
string statusText = player.IsAlive ? "Alive" : "Dead";

float moveSpeed = isRunning ? runSpeed : walkSpeed;

// Can be nested (but gets unreadable fast — prefer switch expression instead)
string tier = score > 1000 ? "Gold" : score > 500 ? "Silver" : "Bronze";
```

---

## 2.8 — `break`, `continue`, `return`

```csharp
void ScanForTargets(List<Enemy> enemies)
{
    foreach (Enemy enemy in enemies)
    {
        if (enemy == null)
            continue;   // skip this iteration, go to next enemy
        
        if (enemy.IsElite)
            break;      // stop the loop entirely — found our priority target

        if (!enemy.IsVisible)
            return;     // exit the ENTIRE method, not just the loop
        
        ProcessEnemy(enemy);
    }
}
```

---

## 2.9 — 🎮 2D vs 3D: Common Branching Patterns

### 🎮 2D — Ground Detection
```csharp
// 2D ground check using a raycast downward
bool IsGrounded()
{
    RaycastHit2D hit = Physics2D.Raycast(
        transform.position,
        Vector2.down,
        groundCheckDistance,
        groundLayerMask
    );
    return hit.collider != null;
}

void Update()
{
    bool grounded = IsGrounded();
    
    if (grounded && Input.GetButtonDown("Jump"))
    {
        rb.AddForce(Vector2.up * jumpForce, ForceMode2D.Impulse);
    }
}
```

### 🎮 3D — Ground Detection
```csharp
// 3D ground check using a sphere cast downward
bool IsGrounded()
{
    return Physics.SphereCast(
        transform.position,
        0.4f,                    // sphere radius
        Vector3.down,
        out RaycastHit hit,
        groundCheckDistance,
        groundLayerMask
    );
}

void Update()
{
    bool grounded = IsGrounded();
    
    if (grounded && Input.GetButtonDown("Jump"))
    {
        rb.AddForce(Vector3.up * jumpForce, ForceMode.Impulse);
    }
}
```

---

## 📝 Summary

| Concept | When to Use |
| :--- | :--- |
| `if/else` | Conditions with complex boolean logic |
| `switch` statement | Multiple discrete cases with side effects |
| `switch` expression | Mapping a value to a result (pure output) |
| `is` pattern | Safely check and cast type simultaneously |
| `for` loop | Array iteration with zero allocation |
| `foreach` on `List<T>` | Clean iteration on lists (safe, no allocation) |
| `foreach` on arrays | Avoid in hot paths — allocates |
| `?.` null-conditional | Safe property chaining when object might be null |
| `??` null-coalescing | Fallback value when null |

**Previous:** [[01 — Variables, Types & Memory]] | **Next:** [[03 — Methods, Parameters & Return Types]]
