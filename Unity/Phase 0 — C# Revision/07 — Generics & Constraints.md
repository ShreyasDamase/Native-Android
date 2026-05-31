# 07 — Generics & Constraints
### 🟡 Block A — Language Foundations

> [!NOTE]
> Generics let you write **type-safe, reusable code**. In Unity, you use generics every time you call `GetComponent<T>()`, `List<T>`, `Dictionary<K,V>`, or write an object pool. Understanding how to write your own generic classes and methods unlocks major architectural patterns.

---

## 7.1 — What Are Generics?

Generics are a placeholder for a type that gets filled in when you use the class or method.

```csharp
// WITHOUT generics — you'd need a separate method for each type
public int FindIndex_Int(List<int> list, int value) { /* ... */ }
public int FindIndex_String(List<string> list, string value) { /* ... */ }
public int FindIndex_Enemy(List<Enemy> list, Enemy value) { /* ... */ }

// WITH generics — one method works for ALL types
public int FindIndex<T>(List<T> list, T value)
{
    for (int i = 0; i < list.Count; i++)
        if (list[i].Equals(value)) return i;
    return -1;
}

// Usage — T is inferred from the argument
int idx = FindIndex(enemies, targetEnemy);    // T = Enemy
int idx2 = FindIndex(scores, 500);            // T = int
```

---

## 7.2 — Generic Classes

```csharp
// A generic wrapper that tracks both a value and when it was last changed
public class TrackedValue<T>
{
    public T Value { get; private set; }
    public float LastChangedTime { get; private set; }
    
    public TrackedValue(T initialValue)
    {
        Value = initialValue;
        LastChangedTime = Time.time;
    }
    
    public void Set(T newValue)
    {
        Value = newValue;
        LastChangedTime = Time.time;
    }
    
    public float TimeSinceLastChange => Time.time - LastChangedTime;
}

// Usage
TrackedValue<float> playerHealth = new TrackedValue<float>(100f);
TrackedValue<GameState> gameState = new TrackedValue<GameState>(GameState.MainMenu);

playerHealth.Set(75f);
Debug.Log($"Health changed {playerHealth.TimeSinceLastChange:F1}s ago");
```

---

## 7.3 — Generic Methods

You can make individual methods generic even inside non-generic classes.

```csharp
public class SceneHelper : MonoBehaviour
{
    // Generic method to find a component safely
    public static T FindRequired<T>() where T : Component
    {
        T result = FindFirstObjectByType<T>();
        if (result == null)
            Debug.LogError($"Required component {typeof(T).Name} not found in scene!");
        return result;
    }
    
    // Generic method with multiple type parameters
    public static void Swap<T>(ref T a, ref T b)
    {
        T temp = a;
        a = b;
        b = temp;
    }
    
    // Generic factory method
    public static T SpawnPrefab<T>(GameObject prefab, Vector3 position) where T : Component
    {
        GameObject obj = Instantiate(prefab, position, Quaternion.identity);
        return obj.GetComponent<T>();
    }
}

// Usage
PlayerController player = SceneHelper.FindRequired<PlayerController>();
Enemy enemy = SceneHelper.SpawnPrefab<Enemy>(enemyPrefab, spawnPoint.position);
```

---

## 7.4 — Generic Constraints (`where`)

Constraints restrict what types can be used with a generic. This lets you call methods on `T` that require a specific type.

```csharp
// No constraint — T can be ANYTHING, but you can only use object methods
public T CreateDefault<T>() { return default(T); }

// 'where T : class' — T must be a reference type
public T FindOrNull<T>() where T : class { return null; }

// 'where T : struct' — T must be a value type
public T ClampStruct<T>() where T : struct { /* ... */ return default; }

// 'where T : Component' — T must be a Unity Component (allows GetComponent)
public T GetOrAdd<T>(GameObject obj) where T : Component
{
    T component = obj.GetComponent<T>();
    if (component == null)
        component = obj.AddComponent<T>();
    return component;
}

// 'where T : MonoBehaviour' — T must be a MonoBehaviour
public T SpawnAndInit<T>(GameObject prefab, Vector3 pos) where T : MonoBehaviour
{
    return Instantiate(prefab, pos, Quaternion.identity).GetComponent<T>();
}

// 'where T : new()' — T must have a parameterless constructor
public T CreateInstance<T>() where T : new()
{
    return new T();
}

// Multiple constraints
public T CreateAndRegister<T>(Vector3 position) 
    where T : MonoBehaviour, IDamageable, new()
{
    T instance = SpawnAndInit<T>(prefab, position);
    RegisterDamageable(instance);
    return instance;
}
```

---

## 7.5 — Generic Object Pool (Most Important Generic Pattern in Unity)

The most practical use of generics in Unity is a reusable object pool.

```csharp
// Generic pool that works for bullets, particles, enemies — anything PooledBehaviour
public class ObjectPool<T> where T : MonoBehaviour, IPoolable
{
    private readonly T prefab;
    private readonly Transform parent;
    private readonly Queue<T> available = new Queue<T>();
    
    public int TotalCreated { get; private set; }
    
    public ObjectPool(T prefab, Transform parent, int initialSize = 10)
    {
        this.prefab = prefab;
        this.parent = parent;
        
        for (int i = 0; i < initialSize; i++)
            CreateNew();
    }
    
    private T CreateNew()
    {
        T instance = Object.Instantiate(prefab, parent);
        instance.gameObject.SetActive(false);
        available.Enqueue(instance);
        TotalCreated++;
        return instance;
    }
    
    public T Get(Vector3 position, Quaternion rotation)
    {
        T item = available.Count > 0 ? available.Dequeue() : CreateNew();
        item.transform.SetPositionAndRotation(position, rotation);
        item.gameObject.SetActive(true);
        item.OnSpawn();
        return item;
    }
    
    public void Return(T item)
    {
        item.OnDespawn();
        item.gameObject.SetActive(false);
        available.Enqueue(item);
    }
}

// Usage
public class BulletSpawner : MonoBehaviour
{
    [SerializeField] private Bullet bulletPrefab;
    private ObjectPool<Bullet> bulletPool;
    
    void Awake()
    {
        bulletPool = new ObjectPool<Bullet>(bulletPrefab, transform, 30);
    }
    
    public void FireBullet(Vector3 pos, Quaternion rot)
    {
        Bullet b = bulletPool.Get(pos, rot);
        b.Initialize(bulletPool); // Give bullet reference to pool so it can return itself
    }
}
```

---

## 7.6 — Generic Event System

```csharp
// Strongly-typed event channel using generics
public class EventChannel<T> : ScriptableObject
{
    private readonly List<System.Action<T>> listeners = new List<System.Action<T>>();
    
    public void Subscribe(System.Action<T> listener) => listeners.Add(listener);
    public void Unsubscribe(System.Action<T> listener) => listeners.Remove(listener);
    
    public void Raise(T data)
    {
        // Iterate backwards in case listeners remove themselves during invocation
        for (int i = listeners.Count - 1; i >= 0; i--)
            listeners[i]?.Invoke(data);
    }
}

// Define specific event channels as ScriptableObjects
[CreateAssetMenu] public class FloatEventChannel : EventChannel<float> { }
[CreateAssetMenu] public class IntEventChannel : EventChannel<int> { }
[CreateAssetMenu] public class Vector3EventChannel : EventChannel<Vector3> { }

// Usage: PlayerHealth fires an event, UI listens
public class PlayerHealth : MonoBehaviour
{
    [SerializeField] private FloatEventChannel onHealthChanged;
    
    public void TakeDamage(float amount)
    {
        health -= amount;
        onHealthChanged.Raise(health);
    }
}

public class HealthUI : MonoBehaviour
{
    [SerializeField] private FloatEventChannel onHealthChanged;
    
    void OnEnable() => onHealthChanged.Subscribe(UpdateUI);
    void OnDisable() => onHealthChanged.Unsubscribe(UpdateUI);
    
    void UpdateUI(float newHealth) { /* update health bar */ }
}
```

---

## 7.7 — Built-In Generic Types You Use Daily

```csharp
// List<T> — dynamic array (most common collection)
List<Enemy> enemies = new List<Enemy>(20);  // initial capacity to avoid resizing
enemies.Add(newEnemy);
enemies.Remove(deadEnemy);
enemies.Count;           // current count
enemies[0];              // index access

// Dictionary<K, V> — key-value lookup
Dictionary<int, Enemy> enemyByID = new Dictionary<int, Enemy>();
enemyByID[enemy.ID] = enemy;               // add/update
if (enemyByID.TryGetValue(42, out Enemy e)) // safe lookup
    e.TakeDamage(10f);

// Queue<T> — first-in, first-out
Queue<SpawnRequest> spawnQueue = new Queue<SpawnRequest>();
spawnQueue.Enqueue(newRequest);
SpawnRequest next = spawnQueue.Dequeue();

// Stack<T> — last-in, first-out
Stack<GameState> stateStack = new Stack<GameState>();
stateStack.Push(GameState.Paused);
GameState top = stateStack.Pop();

// HashSet<T> — unique items, fast contains check
HashSet<string> unlockedLevels = new HashSet<string>();
unlockedLevels.Add("Level_01");
bool hasLevel = unlockedLevels.Contains("Level_01");
```

---

## 📝 Summary

| Concept | Syntax | Use Case |
| :--- | :--- | :--- |
| Generic method | `public T Foo<T>()` | Type-safe, reusable utility |
| Generic class | `public class Pool<T>` | Data containers, pools, event channels |
| `where T : Component` | Constraint | Access Unity component methods on T |
| `where T : new()` | Constraint | Call `new T()` inside generic |
| `List<T>` | `new List<T>(capacity)` | Most common collection — always pass capacity |
| `Dictionary<K,V>` | `TryGetValue(key, out val)` | Fast lookups by key |
| `Queue<T>` | FIFO | Spawn queues, command queues |
| `HashSet<T>` | `.Contains()` O(1) | Fast membership checks |

**Previous:** [[06 — Properties & Encapsulation]] | **Next:** [[08 — Collections & LINQ]]
