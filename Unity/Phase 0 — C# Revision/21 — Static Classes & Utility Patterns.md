# 21 — Static Classes & Utility Patterns
### 🟡 Block D — Modern C# (Unity 6 / C# 10+)

> [!NOTE]
> Static classes and utility patterns are the glue of a Unity project. `GameManager`, `AudioManager`, `InputHandler`, and `VFXManager` are all common static or singleton patterns. Knowing when to use each keeps your architecture clean.

---

## 21.1 — Static Classes

A static class cannot be instantiated. It's a container for utility methods and shared data.

```csharp
// Static class — no 'new' needed, ever
public static class MathUtils
{
    // All methods must be static
    public static float Remap(float value, float inMin, float inMax, float outMin, float outMax)
    {
        return outMin + (value - inMin) * (outMax - outMin) / (inMax - inMin);
    }
    
    public static bool IsApproximatelyZero(float value, float threshold = 0.001f)
        => Mathf.Abs(value) < threshold;
    
    public static float EaseInOut(float t) => t * t * (3f - 2f * t);
    
    public static float EaseIn(float t) => t * t;
    
    public static float EaseOut(float t) => 1f - (1f - t) * (1f - t);
    
    public static float Spring(float t, float frequency = 1f, float damping = 0.5f)
    {
        t = Mathf.Clamp01(t);
        t = (Mathf.Sin(t * Mathf.PI * frequency * (1f - (t / damping))) * (1f - t)) + t;
        return t;
    }
    
    public static Vector2 RandomInsideAnnulus(float innerRadius, float outerRadius)
    {
        float angle = Random.Range(0f, Mathf.PI * 2f);
        float distance = Random.Range(innerRadius, outerRadius);
        return new Vector2(Mathf.Cos(angle), Mathf.Sin(angle)) * distance;
    }
    
    public static int RandomSign() => Random.value > 0.5f ? 1 : -1;
}

// Usage — no instance needed
float normalized = MathUtils.Remap(health, 0f, maxHealth, 0f, 1f);
float smoothT = MathUtils.EaseInOut(t);
```

---

## 21.2 — Constants Class

```csharp
// A class (or static class) that holds all your magic strings and numbers
public static class GameTags
{
    public const string Player = "Player";
    public const string Enemy = "Enemy";
    public const string Ground = "Ground";
    public const string Hazard = "Hazard";
    public const string Collectible = "Collectible";
    public const string Checkpoint = "Checkpoint";
}

public static class GameLayers
{
    public const int Default = 0;
    public const int Player = 8;
    public const int Enemy = 9;
    public const int Ground = 10;
    public const int Trigger = 11;
    
    public static LayerMask PlayerMask => 1 << Player;
    public static LayerMask EnemyMask => 1 << Enemy;
    public static LayerMask GroundMask => 1 << Ground;
    public static LayerMask EnemyAndGround => (1 << Enemy) | (1 << Ground);
}

public static class AnimatorParams
{
    // Cache hashes instead of string lookups — faster AND no typo bugs
    public static readonly int SpeedX    = Animator.StringToHash("SpeedX");
    public static readonly int SpeedY    = Animator.StringToHash("SpeedY");
    public static readonly int IsGrounded = Animator.StringToHash("IsGrounded");
    public static readonly int IsAlive   = Animator.StringToHash("IsAlive");
    public static readonly int Hit       = Animator.StringToHash("Hit");
    public static readonly int Death     = Animator.StringToHash("Death");
    public static readonly int Jump      = Animator.StringToHash("Jump");
    public static readonly int Attack    = Animator.StringToHash("Attack");
}

// Usage
animator.SetFloat(AnimatorParams.SpeedX, velocity.x);  // hash lookup — faster than "SpeedX"
if (other.gameObject.CompareTag(GameTags.Enemy)) { }    // no typo risk
int layer = LayerMask.NameToLayer(GameLayers.Player.ToString()); // not ideal, but available
```

---

## 21.3 — Singleton Pattern

A Singleton is a class that ensures only ONE instance exists and provides global access to it.

```csharp
// The standard Unity Singleton pattern
public class GameManager : MonoBehaviour
{
    // Static property — one reference shared by everything
    public static GameManager Instance { get; private set; }
    
    [Header("Game State")]
    public int CurrentLevel = 1;
    public int Score = 0;
    public bool IsGameOver = false;
    
    void Awake()
    {
        // If an instance already exists and it's not this one, destroy this
        if (Instance != null && Instance != this)
        {
            Destroy(gameObject);
            return;
        }
        
        Instance = this;
        DontDestroyOnLoad(gameObject); // persist across scene loads
    }
    
    void OnDestroy()
    {
        if (Instance == this) Instance = null;
    }
    
    public void AddScore(int points)
    {
        Score += points;
        GameEvents.ScoreChanged(Score);
    }
    
    public void GameOver()
    {
        IsGameOver = true;
        GameEvents.PlayerDied();
    }
}

// Usage from ANY script
GameManager.Instance.AddScore(100);
int level = GameManager.Instance.CurrentLevel;
```

---

## 21.4 — Lazy Singleton (Thread-Safe)

```csharp
// Lazy initialization — only created when first accessed
public class AudioManager : MonoBehaviour
{
    private static AudioManager _instance;
    private static readonly object _lock = new object();
    
    public static AudioManager Instance
    {
        get
        {
            if (_instance == null)
            {
                lock (_lock)
                {
                    if (_instance == null)
                    {
                        // Find existing or create new
                        _instance = FindFirstObjectByType<AudioManager>();
                        
                        if (_instance == null)
                        {
                            GameObject go = new GameObject("AudioManager");
                            _instance = go.AddComponent<AudioManager>();
                            DontDestroyOnLoad(go);
                        }
                    }
                }
            }
            return _instance;
        }
    }
    
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
    
    public void PlaySFX(AudioClip clip, float volume = 1f) { /* ... */ }
    public void PlayMusic(AudioClip music) { /* ... */ }
}
```

---

## 21.5 — Service Locator (Better Than Singleton for Testability)

```csharp
// Service Locator — register services by interface type
public static class ServiceLocator
{
    private static readonly Dictionary<System.Type, object> services = new Dictionary<System.Type, object>();
    
    public static void Register<T>(T service) where T : class
    {
        services[typeof(T)] = service;
    }
    
    public static void Unregister<T>() where T : class
    {
        services.Remove(typeof(T));
    }
    
    public static T Get<T>() where T : class
    {
        if (services.TryGetValue(typeof(T), out object service))
            return (T)service;
        
        Debug.LogError($"Service {typeof(T).Name} not registered!");
        return null;
    }
    
    public static bool TryGet<T>(out T service) where T : class
    {
        if (services.TryGetValue(typeof(T), out object obj))
        {
            service = (T)obj;
            return true;
        }
        service = null;
        return false;
    }
}

// Register services at startup
public class ServiceBootstrapper : MonoBehaviour
{
    void Awake()
    {
        ServiceLocator.Register<IAudioManager>(GetComponent<AudioManager>());
        ServiceLocator.Register<IScoreManager>(GetComponent<ScoreManager>());
        ServiceLocator.Register<ISaveSystem>(GetComponent<SaveSystem>());
    }
    
    void OnDestroy()
    {
        ServiceLocator.Unregister<IAudioManager>();
        ServiceLocator.Unregister<IScoreManager>();
        ServiceLocator.Unregister<ISaveSystem>();
    }
}

// Usage — depends on interface, not concrete class
public class Player : MonoBehaviour
{
    private IAudioManager audio;
    
    void Awake()
    {
        audio = ServiceLocator.Get<IAudioManager>();
    }
    
    void OnHit()
    {
        audio.PlaySFX(hitSound);
    }
}
```

---

## 21.6 — Utility Methods — The Complete Toolkit

```csharp
public static class GameUtils
{
    // ===== Physics =====
    
    // Check if any objects are in a circle/sphere
    private static readonly Collider2D[] overlapBuffer2D = new Collider2D[32];
    
    public static int GetObjectsInRadius2D(Vector2 center, float radius, LayerMask mask, out Collider2D[] results)
    {
        int count = Physics2D.OverlapCircleNonAlloc(center, radius, overlapBuffer2D, mask);
        results = overlapBuffer2D;
        return count;
    }
    
    // ===== Angle Utilities =====
    
    // Get the angle of a direction vector in degrees
    public static float DirectionToAngle(Vector2 dir) => Mathf.Atan2(dir.y, dir.x) * Mathf.Rad2Deg;
    
    // Get a direction from an angle
    public static Vector2 AngleToDirection(float degrees)
    {
        float rad = degrees * Mathf.Deg2Rad;
        return new Vector2(Mathf.Cos(rad), Mathf.Sin(rad));
    }
    
    // Normalize angle to -180 to 180 range
    public static float NormalizeAngle(float angle)
    {
        while (angle > 180f) angle -= 360f;
        while (angle < -180f) angle += 360f;
        return angle;
    }
    
    // ===== Random Utilities =====
    
    public static T RandomElement<T>(this IList<T> list) => list[Random.Range(0, list.Count)];
    
    public static void Shuffle<T>(this IList<T> list)
    {
        for (int i = list.Count - 1; i > 0; i--)
        {
            int j = Random.Range(0, i + 1);
            (list[i], list[j]) = (list[j], list[i]); // tuple swap
        }
    }
    
    public static Vector2 RandomPointInRect(Rect rect)
        => new Vector2(Random.Range(rect.xMin, rect.xMax), Random.Range(rect.yMin, rect.yMax));
    
    // ===== Scene Utilities =====
    
    public static T FindRequiredInScene<T>(string errorContext = "") where T : Component
    {
        T found = Object.FindFirstObjectByType<T>();
        if (found == null)
            Debug.LogError($"[{errorContext}] Required component {typeof(T).Name} not found in scene!");
        return found;
    }
}
```

---

## 21.7 — 🎮 2D vs 3D: Utility Patterns

### 🎮 2D Utilities
```csharp
public static class Utils2D
{
    // Get 2D mouse world position
    public static Vector2 GetMouseWorldPosition(Camera cam = null)
    {
        cam ??= Camera.main;
        return cam.ScreenToWorldPoint(Input.mousePosition);
    }
    
    // Check if a point is inside a 2D bounds
    public static bool IsInBounds(Vector2 point, Vector2 center, Vector2 size)
    {
        return Mathf.Abs(point.x - center.x) < size.x * 0.5f &&
               Mathf.Abs(point.y - center.y) < size.y * 0.5f;
    }
    
    // Grid helpers
    public static Vector2Int WorldToGrid(Vector2 world, float cellSize)
        => new Vector2Int(Mathf.FloorToInt(world.x / cellSize), Mathf.FloorToInt(world.y / cellSize));
    
    public static Vector2 GridToWorld(Vector2Int grid, float cellSize)
        => new Vector2(grid.x * cellSize + cellSize * 0.5f, grid.y * cellSize + cellSize * 0.5f);
}
```

### 🎮 3D Utilities
```csharp
public static class Utils3D
{
    // Get 3D mouse hit position (raycast from camera to world)
    public static bool GetMouseWorldPosition3D(out Vector3 worldPos, LayerMask hitMask, Camera cam = null)
    {
        cam ??= Camera.main;
        Ray ray = cam.ScreenPointToRay(Input.mousePosition);
        
        if (Physics.Raycast(ray, out RaycastHit hit, 100f, hitMask))
        {
            worldPos = hit.point;
            return true;
        }
        
        worldPos = Vector3.zero;
        return false;
    }
    
    // Snap position to NavMesh
    public static bool SnapToNavMesh(Vector3 position, out Vector3 snapped, float searchRadius = 1f)
    {
        if (UnityEngine.AI.NavMesh.SamplePosition(position, out var hit, searchRadius, UnityEngine.AI.NavMesh.AllAreas))
        {
            snapped = hit.position;
            return true;
        }
        snapped = position;
        return false;
    }
    
    // Check line of sight between two points
    public static bool HasLineOfSight(Vector3 from, Vector3 to, LayerMask obstacleMask)
    {
        Vector3 dir = to - from;
        return !Physics.Raycast(from, dir.normalized, dir.magnitude, obstacleMask);
    }
}
```

---

## 📝 Summary

| Pattern | When to Use | Notes |
| :--- | :--- | :--- |
| Static class | Pure utility methods, no state | `MathUtils`, extension methods |
| Constants class | Magic strings/numbers | `GameTags`, `AnimatorParams` |
| Singleton (MonoBehaviour) | Managers that need Unity lifecycle | `GameManager`, `AudioManager` |
| Service Locator | Multiple systems, testable | Better decoupling than Singleton |
| `GameEvents` static | Cross-system communication | Zero references, fully decoupled |

**Previous:** [[20 — Extension Methods]] | **Next:** [[22 — Operator Overloading]]
