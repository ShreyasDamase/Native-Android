# 28 — The ScriptableObject Architecture
### 🔶 Block F — Architecture & Best Practices

> [!IMPORTANT]
> This is a Unity-exclusive architectural pattern popularized by Ryan Hipple (Schell Games). Android doesn't have an exact equivalent. **ScriptableObjects (SOs)** are data containers that live as *Asset files* in your project folder, completely independent of the Scene hierarchy.

---

## 28.1 — What is a ScriptableObject?

If `MonoBehaviour` is a script that attaches to a GameObject in a Scene, `ScriptableObject` is a script that lives as a file in your `Assets/` folder.

**Why are they powerful?**
1. They exist outside of Scenes. They don't get destroyed when a Scene reloads.
2. Multiple GameObjects can reference the same ScriptableObject.
3. They can hold data *and* events.

---

## 28.2 — The Standard Use Case: Data Containers

Instead of putting enemy stats directly in the `Enemy.cs` MonoBehaviour, we put them in a ScriptableObject. This allows us to create 50 different enemy types without writing 50 different scripts.

### 1. Define the ScriptableObject
```csharp
using UnityEngine;

// This attribute allows you to right-click in the Project window and create this asset!
[CreateAssetMenu(fileName = "NewEnemyStats", menuName = "Game Data/Enemy Stats")]
public class EnemyStatsSO : ScriptableObject
{
    public string enemyName;
    public int maxHealth;
    public float moveSpeed;
    public int damage;
    public Sprite artwork;
}
```

### 2. Create the Assets in the Editor
1. Right-click in the Project window → **Create** → **Game Data** → **Enemy Stats**.
2. Name it `Goblin_Stats`. Set Health to 50, Speed to 3.
3. Create another one. Name it `Orc_Stats`. Set Health to 200, Speed to 1.5.

### 3. Use it in the MonoBehaviour
```csharp
public class Enemy : MonoBehaviour
{
    // Drag the Goblin_Stats or Orc_Stats asset here in the Inspector!
    [SerializeField] private EnemyStatsSO stats;
    
    private int currentHealth;
    
    void Start()
    {
        // Read from the shared asset
        currentHealth = stats.maxHealth;
        Debug.Log($"Spawned {stats.enemyName} with {currentHealth} HP.");
    }
}
```

**Why this is better than JSON:** The Inspector natively supports it. You can drag and drop Sprites, AudioClips, and Prefabs into ScriptableObjects. JSON can't hold object references easily.

---

## 28.3 — Advanced Pattern: Shared Variables

You can use ScriptableObjects to share variables between completely disconnected systems, replacing Singletons!

### The Problem:
`PlayerHealth.cs` takes damage. `UIHealthBar.cs` needs to update.
How do they talk?
- `PlayerHealth` finds the UI? (Tight coupling, bad).
- `UIHealthBar` finds the Player? (What if the player hasn't spawned yet?)
- Use a `Singleton GameManager`? (Creates a global dependency bottleneck).

### The SO Solution: Shared Variables
Create a ScriptableObject that just holds a float and an event.

```csharp
[CreateAssetMenu(menuName = "Variables/Float Variable")]
public class FloatVariableSO : ScriptableObject
{
    [SerializeField] private float value;
    
    // An event that fires whenever the value changes
    public event System.Action<float> OnValueChanged;

    public float Value
    {
        get => value;
        set
        {
            this.value = value;
            OnValueChanged?.Invoke(this.value); // Notify listeners!
        }
    }
}
```

### How to use it:
1. Create a `FloatVariableSO` asset in your project. Name it `PlayerHP_Variable`.
2. In `PlayerHealth.cs`:
```csharp
public class PlayerHealth : MonoBehaviour
{
    // Drag PlayerHP_Variable here
    [SerializeField] private FloatVariableSO playerHP;
    
    void TakeDamage(float amount)
    {
        // Modifying this asset automatically fires the event
        playerHP.Value -= amount;
    }
}
```

3. In `UIHealthBar.cs`:
```csharp
public class UIHealthBar : MonoBehaviour
{
    // Drag the EXACT SAME PlayerHP_Variable here
    [SerializeField] private FloatVariableSO playerHP;
    [SerializeField] private UnityEngine.UI.Image fillBar;
    
    void OnEnable()
    {
        // Listen to the asset's event
        playerHP.OnValueChanged += UpdateUI;
        UpdateUI(playerHP.Value); // Initial setup
    }
    
    void OnDisable()
    {
        playerHP.OnValueChanged -= UpdateUI;
    }
    
    void UpdateUI(float hp)
    {
        fillBar.fillAmount = hp / 100f;
    }
}
```

**The Magic:** The Player and the UI completely ignore each other. They both just point to the `PlayerHP_Variable` file in your project. This is extreme decoupling.

> [!WARNING]
> In the Unity Editor, ScriptableObject values persist when you exit Play Mode. If your player dies and HP hits 0, the SO will stay at 0 next time you hit Play! You must manually reset it in `Awake()` or use a separate `RuntimeValue` field. (This persistence does *not* happen in a built game).

---

## 28.4 — Advanced Pattern: The Event Channel

Instead of a `GameManager` singleton handling global events (like `LevelCompleted` or `BossSpawned`), use an Event Channel ScriptableObject.

```csharp
[CreateAssetMenu(menuName = "Events/Void Event Channel")]
public class VoidEventChannelSO : ScriptableObject
{
    public event System.Action OnEventRaised;
    
    public void RaiseEvent()
    {
        OnEventRaised?.Invoke();
    }
}
```

1. Create asset: `OnBossKilled_Event`.
2. The Boss script takes a reference to it and calls `.RaiseEvent()` when it dies.
3. The Achievement System, Audio System, and UI System all take references to `OnBossKilled_Event` and subscribe to `.OnEventRaised`.
4. Result: Zero Singletons. Zero tight coupling.

---

## 📝 Summary

| Concept | Explanation |
| :--- | :--- |
| **ScriptableObject (SO)** | A script that lives as a data file in your Project window. |
| **Data Container** | Use SOs to define stats, items, and configs. Replaces large arrays of data. |
| **Shared Variables** | SOs holding a single value + an Action event. Decouples systems completely. |
| **Event Channels** | SOs holding only an event. Replaces global Singletons for cross-system communication. |

**Next:** [[29 — Dependency Injection & Service Locator]]
