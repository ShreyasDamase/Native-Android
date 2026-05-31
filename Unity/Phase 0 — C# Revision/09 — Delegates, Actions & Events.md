# 09 — Delegates, Actions & Events
### 🟡 Block B — Unity-Critical C# Features

> [!IMPORTANT]
> This is the **most important chapter for building decoupled game systems**. Delegates and events let different scripts communicate without knowing about each other. A health bar that updates when the player takes damage, a UI manager that reacts to score changes, enemy AI reacting to player death — all done through events.

---

## 9.1 — Delegates (The Foundation)

A delegate is a **type-safe function pointer**. It holds a reference to a method (or methods) with a specific signature.

```csharp
// Step 1: Define a delegate TYPE (method signature)
public delegate void PlayerDiedDelegate();
public delegate void DamageDelegate(float amount, DamageType type);
public delegate float StatModifierDelegate(float baseValue);

// Step 2: Declare a variable of that delegate type
public DamageDelegate OnDamageTaken;

// Step 3: Assign a method to it
OnDamageTaken = PlayHitAnimation;  // method signature must match exactly

// Step 4: Invoke it (calls the method)
OnDamageTaken?.Invoke(25f, DamageType.Fire);  // ?. prevents crash if null
```

---

## 9.2 — `Action` and `Func` — Pre-Built Delegates

Don't define custom delegates unless you need named types for clarity. Use `Action` and `Func` instead.

```csharp
// Action — void return type
Action doSomething;                    // void Foo()
Action<float> onDamage;               // void Foo(float amount)
Action<float, bool> onHit;            // void Foo(float damage, bool isCritical)
Action<int, int, string> onScoreUp;   // void Foo(int old, int new, string reason)

// Func — has a return type (last type parameter is the return type)
Func<float> getHealth;                // float Foo()
Func<float, float> calculateDamage;  // float Foo(float input)
Func<Vector3, bool> isInRange;        // bool Foo(Vector3 pos)

// Using them
Action<float> onHealthChanged = null;
onHealthChanged += UpdateHealthBar;    // subscribe a method
onHealthChanged += PlayLowHealthFX;   // add another — both will be called
onHealthChanged -= UpdateHealthBar;   // unsubscribe
onHealthChanged?.Invoke(75f);         // invoke — calls all subscribed methods
```

---

## 9.3 — `event` Keyword (Safety Layer)

Add `event` to an Action/delegate field to prevent external scripts from:
- **Clearing** all subscribers (`OnDamage = null`)
- **Directly invoking** it (`OnDamage(10f)`)
- Only the class that declares it can clear or invoke it.

```csharp
public class PlayerHealth : MonoBehaviour
{
    // Without 'event' — anyone can invoke it or clear it! Dangerous.
    public Action<float> OnHealthChanged_UNSAFE;
    
    // With 'event' — external scripts can only subscribe/unsubscribe
    public event Action<float> OnHealthChanged;
    public static event Action OnPlayerDied;
    
    private float health = 100f;
    
    public void TakeDamage(float amount)
    {
        health = Mathf.Max(0f, health - amount);
        OnHealthChanged?.Invoke(health);   // Only PlayerHealth can invoke this
        
        if (health <= 0f)
            OnPlayerDied?.Invoke();
    }
}

// External script — can only += or -=
public class HealthUI : MonoBehaviour
{
    private PlayerHealth player;
    
    void OnEnable()
    {
        player = FindFirstObjectByType<PlayerHealth>();
        player.OnHealthChanged += UpdateHealthBar;  // ✅ subscribe
        PlayerHealth.OnPlayerDied += ShowDeathScreen; // ✅ subscribe
    }
    
    void OnDisable()
    {
        player.OnHealthChanged -= UpdateHealthBar;  // ✅ unsubscribe
        PlayerHealth.OnPlayerDied -= ShowDeathScreen;
    }
    
    void UpdateHealthBar(float newHealth) { /* update slider */ }
    void ShowDeathScreen() { /* show game over UI */ }
    
    // External scripts CANNOT do these:
    // player.OnHealthChanged = null;       ❌ compile error — event prevents this
    // player.OnHealthChanged.Invoke(99f);  ❌ compile error
}
```

---

## 9.4 — Lambda Expressions with Events

```csharp
public class ScoreManager : MonoBehaviour
{
    public event Action<int> OnScoreChanged;
    
    private int score;
    public int Score
    {
        get => score;
        set
        {
            score = value;
            OnScoreChanged?.Invoke(score);
        }
    }
}

// Subscribe with a lambda (inline anonymous method)
void Start()
{
    scoreManager.OnScoreChanged += (newScore) => 
    {
        scoreText.text = $"Score: {newScore}";
    };
    
    // Shorter lambda for single expression
    scoreManager.OnScoreChanged += newScore => scoreText.text = $"Score: {newScore}";
}
```

> [!WARNING]
> **Lambda subscriptions cannot be unsubscribed** easily! If you subscribe with a lambda, you cannot unsubscribe later. Always use named methods if you need to unsubscribe in `OnDisable`. Lambda subscriptions that aren't unsubscribed cause **memory leaks**.

```csharp
// ❌ MEMORY LEAK — cannot unsubscribe this lambda later
void OnEnable()
{
    player.OnHealthChanged += (hp) => UpdateUI(hp); // anonymous — can't remove
}

// ✅ CORRECT — store the method reference and unsubscribe
void OnEnable()  => player.OnHealthChanged += UpdateUI;
void OnDisable() => player.OnHealthChanged -= UpdateUI;
void UpdateUI(float hp) { /* ... */ }
```

---

## 9.5 — Static Event Bus (Decoupled Global Events)

The most powerful pattern in Unity — a global event system where nothing needs a reference to anything.

```csharp
// Central event hub — static class
public static class GameEvents
{
    // Player events
    public static event Action OnPlayerDied;
    public static event Action<int> OnScoreChanged;
    public static event Action<float, float> OnPlayerHealthChanged; // current, max
    
    // Game flow events
    public static event Action<int> OnLevelStarted;
    public static event Action OnGamePaused;
    public static event Action OnGameResumed;
    
    // Enemy events
    public static event Action<Enemy> OnEnemyKilled;
    public static event Action<int> OnWaveCompleted;
    
    // Invokers — only called from the relevant system scripts
    public static void PlayerDied() => OnPlayerDied?.Invoke();
    public static void ScoreChanged(int score) => OnScoreChanged?.Invoke(score);
    public static void PlayerHealthChanged(float current, float max) => OnPlayerHealthChanged?.Invoke(current, max);
    public static void EnemyKilled(Enemy e) => OnEnemyKilled?.Invoke(e);
    public static void WaveCompleted(int wave) => OnWaveCompleted?.Invoke(wave);
}

// Script A: Fires the event — doesn't know who's listening
public class Player : MonoBehaviour
{
    void Die()
    {
        GameEvents.PlayerDied();        // Broadcast — zero dependencies!
        GameEvents.PlayerHealthChanged(0f, maxHealth);
    }
    
    void AddScore(int points)
    {
        score += points;
        GameEvents.ScoreChanged(score);
    }
}

// Script B: Listens — doesn't know who fired it
public class GameUI : MonoBehaviour
{
    void OnEnable()
    {
        GameEvents.OnPlayerDied += ShowDeathScreen;
        GameEvents.OnScoreChanged += UpdateScoreText;
        GameEvents.OnPlayerHealthChanged += UpdateHealthBar;
    }
    
    void OnDisable()
    {
        GameEvents.OnPlayerDied -= ShowDeathScreen;
        GameEvents.OnScoreChanged -= UpdateScoreText;
        GameEvents.OnPlayerHealthChanged -= UpdateHealthBar;
    }
    
    void ShowDeathScreen() { /* ... */ }
    void UpdateScoreText(int score) { scoreText.text = $"Score: {score}"; }
    void UpdateHealthBar(float current, float max) { /* ... */ }
}
```

---

## 9.6 — UnityEvent (Inspector-Assignable Events)

`UnityEvent` can be assigned in the Inspector (drag-and-drop). Use for designer-friendly event wiring.

```csharp
using UnityEngine.Events;

public class Button3D : MonoBehaviour
{
    // Inspector-assignable — drag any method from the Inspector
    [SerializeField] private UnityEvent onClicked;
    [SerializeField] private UnityEvent<float> onValueChanged; // typed UnityEvent
    
    void OnMouseDown()
    {
        onClicked?.Invoke();       // calls whatever was assigned in Inspector
    }
    
    public void SetValue(float val)
    {
        onValueChanged?.Invoke(val);
    }
}
```

---

## 9.7 — 🎮 2D vs 3D: Event Patterns

### 🎮 2D — Platformer Events
```csharp
public static class PlatformerEvents
{
    // 2D-specific events
    public static event Action OnPlayerLanded;
    public static event Action<Vector2> OnPlayerJumped;
    public static event Action<Collider2D> OnHazardTouched;
    public static event Action<int> OnCheckpointReached;
    
    public static void PlayerLanded() => OnPlayerLanded?.Invoke();
    public static void PlayerJumped(Vector2 velocity) => OnPlayerJumped?.Invoke(velocity);
}

public class Player2D : MonoBehaviour
{
    private bool wasGrounded;
    private Rigidbody2D rb;
    
    void Update()
    {
        bool isGrounded = CheckGrounded();
        
        if (!wasGrounded && isGrounded && rb.linearVelocity.y <= 0f)
            PlatformerEvents.PlayerLanded();
        
        wasGrounded = isGrounded;
    }
}
```

### 🎮 3D — Shooter Events
```csharp
public static class ShooterEvents
{
    // 3D-specific events
    public static event Action<Vector3, Vector3> OnBulletImpact; // position, normal
    public static event Action<Transform, int> OnEnemyHit;       // enemy transform, damage
    public static event Action<NavMeshAgent> OnEnemyReachedBase;
    
    public static void BulletImpact(Vector3 pos, Vector3 normal)
        => OnBulletImpact?.Invoke(pos, normal);
}

public class Bullet3D : MonoBehaviour
{
    void OnCollisionEnter(Collision collision)
    {
        ContactPoint contact = collision.contacts[0];
        ShooterEvents.BulletImpact(contact.point, contact.normal);
        
        if (collision.gameObject.TryGetComponent<IDamageable>(out var target))
            target.TakeDamage(damage);
        
        Destroy(gameObject);
    }
}
```

---

## 📝 Summary

| Concept | When to Use |
| :--- | :--- |
| `delegate` | Define a custom named function pointer type |
| `Action` | Pre-built delegate, void return, 0-16 params |
| `Func<T>` | Pre-built delegate, returns T |
| `event` | Add safety — prevent external invoke/clear |
| Lambda `+=` | Quick subscribe when you don't need to unsubscribe |
| Named method `+=/-=` | When you must unsubscribe (always use in MonoBehaviour) |
| Static event bus | Global events — zero coupling between systems |
| `UnityEvent` | Designer-assignable events via Inspector |

**Previous:** [[08 — Collections & LINQ]] | **Next:** [[10 — Interfaces in Unity]]
