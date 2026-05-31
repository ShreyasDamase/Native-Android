# 15 — Memory Management & GC
### 🔴 Block C — Memory Management & Performance

> [!IMPORTANT]
> GC spikes are the #1 cause of frame rate drops in Unity games. A 60 FPS game has 16.6ms per frame. The Garbage Collector can steal 5–50ms when it runs. This chapter teaches you how to **write code that never triggers the GC** during gameplay.

---

## 15.1 — The Three Memory Spaces (Deep Dive)

```
EVERY FRAME your game has a BUDGET:
 60 FPS = 16.6 ms per frame
 30 FPS = 33.3 ms per frame

If GC runs during a frame: +5 to +50ms added → STUTTER
```

```csharp
// THE STACK — fast, automatic, zero GC
void Update()
{
    float speed = 5f;              // stack — destroyed when Update() ends
    Vector3 pos = transform.position; // struct — stack copy
    int count = enemies.Count;     // stack — int
    
    // All these variables vanish instantly when Update() returns
    // NO garbage collector involvement
}

// THE MANAGED HEAP — slow cleanup, triggers GC
void BadUpdate()
{
    // Every 'new' on a class = heap allocation = future GC work
    string text = "Score: " + score;          // new string object!
    List<int> temp = new List<int>();          // new List object!
    Collider2D[] hits = Physics2D.OverlapCircleAll(pos, r); // new array!
    Enemy e = new Enemy();                     // new Enemy object!
}
```

---

## 15.2 — The Garbage Collector — How It Works

```
STEP 1: You allocate objects on the heap:
        Enemy e1 = new Enemy();
        string s1 = "Score: 100";

STEP 2: References get removed:
        e1 = null;
        s1 = null;
        // But the objects STILL EXIST on the heap!
        
STEP 3: GC runs (Unity decides when):
        ├── PAUSES the main thread ←── this is the stutter!
        ├── Scans all heap objects
        ├── Finds objects with no references
        ├── Frees their memory
        └── RESUMES the main thread

The problem: you have no control WHEN the GC runs.
The solution: allocate NOTHING during gameplay.
```

---

## 15.3 — The 8 Biggest Allocation Pitfalls

### Pitfall 1 — String Concatenation in Loops
```csharp
// ❌ WRONG — creates a new string on the heap EVERY FRAME
void Update()
{
    hpText.text = "HP: " + currentHP;        // allocates!
    scoreText.text = "Score: " + score;      // allocates!
    timerText.text = "Time: " + time + "s";  // allocates TWO strings!
}

// ✅ CORRECT — only update when value changes
private int cachedHP = -1;
private int cachedScore = -1;

public void OnHPChanged(int newHP)
{
    if (newHP == cachedHP) return;
    cachedHP = newHP;
    hpText.text = $"HP: {newHP}";  // string interpolation still allocates, but RARELY
}

// ✅ BEST — StringBuilder for frequent string building
private System.Text.StringBuilder sb = new System.Text.StringBuilder(64);

public string BuildStatusText(string name, int hp, int score)
{
    sb.Clear();
    sb.Append("Player: ");
    sb.Append(name);
    sb.Append(" | HP: ");
    sb.Append(hp);
    sb.Append(" | Score: ");
    sb.Append(score);
    return sb.ToString(); // one allocation at the end
}
```

### Pitfall 2 — `new WaitForSeconds` Every Coroutine
```csharp
// ❌ WRONG — new object on heap every time this coroutine runs
IEnumerator SpawnLoop()
{
    while (true)
    {
        SpawnEnemy();
        yield return new WaitForSeconds(3f); // allocates every loop!
    }
}

// ✅ CORRECT — create once, reuse forever
private readonly WaitForSeconds wait3s = new WaitForSeconds(3f);
private readonly WaitForSeconds wait1s = new WaitForSeconds(1f);
private readonly WaitForFixedUpdate waitFixed = new WaitForFixedUpdate();
private readonly WaitForEndOfFrame waitEOF = new WaitForEndOfFrame();

IEnumerator SpawnLoopOptimized()
{
    while (true)
    {
        SpawnEnemy();
        yield return wait3s; // reuses cached object — zero allocation!
    }
}
```

### Pitfall 3 — Physics Overlap Queries That Return Arrays
```csharp
// ❌ WRONG — allocates a new array on the heap every call
void CheckEnemiesInRange()
{
    Collider2D[] hits = Physics2D.OverlapCircleAll(transform.position, 5f); // new array!
    foreach (var hit in hits) { /* ... */ }
}

// ✅ CORRECT — pre-allocated buffer, reused every call
private Collider2D[] overlapBuffer = new Collider2D[32]; // pre-allocate once

void CheckEnemiesInRange()
{
    int count = Physics2D.OverlapCircleNonAlloc(transform.position, 5f, overlapBuffer);
    for (int i = 0; i < count; i++) // only iterate actual hits
    {
        if (overlapBuffer[i].TryGetComponent<IDamageable>(out var target))
            target.TakeDamage(10f);
    }
}

// 3D equivalent
private RaycastHit[] raycastBuffer = new RaycastHit[16];

void RaycastScan()
{
    int count = Physics.RaycastNonAlloc(transform.position, Vector3.forward, raycastBuffer, 20f);
    for (int i = 0; i < count; i++)
    {
        Debug.Log($"Hit: {raycastBuffer[i].collider.name}");
    }
}
```

### Pitfall 4 — `foreach` on Arrays
```csharp
// ❌ WRONG — foreach on a raw array allocates an IEnumerator
Enemy[] enemyArray = GetEnemies();
foreach (Enemy e in enemyArray) { } // allocates!

// ✅ CORRECT — use for with index
for (int i = 0; i < enemyArray.Length; i++)
{
    enemyArray[i].TakeDamage(10f);
}

// NOTE: foreach on List<T> is SAFE — no allocation
List<Enemy> enemyList = GetEnemyList();
foreach (Enemy e in enemyList) { } // safe — List's struct enumerator
```

### Pitfall 5 — Closures Capturing Local Variables
```csharp
// ❌ WRONG — lambda captures 'multiplier', forcing heap allocation of closure class
void ApplyBuff(List<float> stats, float multiplier)
{
    stats.ForEach(s => s *= multiplier); // closure allocates!
}

// ✅ CORRECT — explicit loop, no closure
void ApplyBuff(List<float> stats, float multiplier)
{
    for (int i = 0; i < stats.Count; i++)
        stats[i] *= multiplier;
}
```

### Pitfall 6 — Boxing (Value Types Cast to `object`)
```csharp
// ❌ WRONG — int gets boxed into an object on the heap
Debug.Log("Score: " + score);          // score (int) boxes into object!
Debug.LogFormat("Score: {0}", score);  // score boxes into params object[]!

// ✅ CORRECT — convert to string explicitly, no boxing
Debug.Log("Score: " + score.ToString()); // ToString() avoids boxing
Debug.Log($"Score: {score}");            // string interpolation also converts cleanly

// ❌ WRONG — non-generic collections box everything
System.Collections.ArrayList oldList = new System.Collections.ArrayList();
oldList.Add(42); // int is boxed!

// ✅ CORRECT — use generic collections (never box)
List<int> typedList = new List<int>();
typedList.Add(42); // no boxing!
```

### Pitfall 7 — `GetComponent` in Update
```csharp
// ❌ WRONG — traverses component list EVERY frame
void Update()
{
    GetComponent<Rigidbody2D>().AddForce(Vector2.up * speed);
}

// ✅ CORRECT — cache in Awake
private Rigidbody2D rb;
void Awake() => rb = GetComponent<Rigidbody2D>();
void Update() => rb.AddForce(Vector2.up * speed);
```

### Pitfall 8 — `CompareTag` vs Tag String Comparison
```csharp
// ❌ WRONG — string comparison allocates
if (gameObject.tag == "Player") { }

// ✅ CORRECT — CompareTag uses native-side comparison, no C# allocation
if (gameObject.CompareTag("Player")) { }
if (collision.gameObject.CompareTag("Enemy")) { }
```

---

## 15.4 — Object Pooling (The #1 Allocation Fix)

Instead of `Instantiate` (allocates) and `Destroy` (fragments memory), pool objects.

```csharp
// Simple bullet pool
public class BulletPool : MonoBehaviour
{
    [SerializeField] private GameObject bulletPrefab;
    [SerializeField] private int initialPoolSize = 30;
    
    private Queue<GameObject> pool = new Queue<GameObject>();
    
    void Awake()
    {
        // Pre-warm the pool — allocate all at start, not during gameplay
        for (int i = 0; i < initialPoolSize; i++)
        {
            GameObject b = Instantiate(bulletPrefab, transform);
            b.SetActive(false);
            pool.Enqueue(b);
        }
    }
    
    public GameObject GetBullet(Vector3 position, Quaternion rotation)
    {
        if (pool.Count == 0)
        {
            // Pool empty — expand (only happens rarely)
            GameObject extra = Instantiate(bulletPrefab, transform);
            extra.SetActive(false);
            pool.Enqueue(extra);
        }
        
        GameObject bullet = pool.Dequeue();
        bullet.transform.SetPositionAndRotation(position, rotation);
        bullet.SetActive(true);
        return bullet;
    }
    
    public void ReturnBullet(GameObject bullet)
    {
        bullet.SetActive(false);
        pool.Enqueue(bullet);
    }
}
```

---

## 15.5 — The Unity Memory Profiler

```
To find allocations:
1. Window → Analysis → Profiler (Ctrl+7)
2. Play the game
3. Click the GC Alloc column
4. Sort by allocation size
5. Click a spike — see the exact call that caused it

Allocation budget per frame: AIM FOR 0 BYTES during gameplay
During loading screens: unlimited — GC.Collect() is fine
```

---

## 15.6 — Native Resources — Destroy Manually!

Unity assets that live on the **native heap** must be explicitly destroyed.

```csharp
// Native resources that MUST be destroyed when done
void OnDestroy()
{
    // Destroy runtime-created assets
    if (runtimeTexture != null)
        Destroy(runtimeTexture);   // or DestroyImmediate in Editor
    
    if (runtimeMaterial != null)
        Destroy(runtimeMaterial);
    
    if (runtimeMesh != null)
        Destroy(runtimeMesh);
    
    if (runtimeRenderTexture != null)
    {
        runtimeRenderTexture.Release(); // RenderTexture needs Release() first!
        Destroy(runtimeRenderTexture);
    }
}

// Compute buffers (GPU memory — ALWAYS release!)
ComputeBuffer gpuBuffer;

void CreateBuffer()
{
    gpuBuffer = new ComputeBuffer(1024, sizeof(float));
}

void OnDestroy()
{
    gpuBuffer?.Release(); // GPU memory — not collected by GC!
}
```

---

## 15.7 — `Dispose` Pattern for Managed Resources

```csharp
// Classes implementing IDisposable should be used with 'using'
// 'using' automatically calls Dispose() when the block exits

// File I/O
using (System.IO.StreamReader reader = System.IO.File.OpenText(path))
{
    string content = reader.ReadToEnd();
} // reader.Dispose() called automatically here

// UnityWebRequest
using (UnityWebRequest req = UnityWebRequest.Get(url))
{
    yield return req.SendWebRequest();
    ProcessResult(req.downloadHandler.text);
} // req.Dispose() called automatically

// Manually implementing IDisposable in your own class
public class GameRecorder : System.IDisposable
{
    private System.IO.FileStream fileStream;
    private bool disposed = false;
    
    public GameRecorder(string path)
    {
        fileStream = System.IO.File.Open(path, System.IO.FileMode.Create);
    }
    
    public void Dispose()
    {
        if (!disposed)
        {
            fileStream?.Close();
            fileStream?.Dispose();
            disposed = true;
        }
    }
}
```

---

## 15.8 — Quick Reference: Allocation Table

| Code | Allocates? | Fix |
| :--- | :---: | :--- |
| `new MyClass()` | ✅ | Pool it |
| `new WaitForSeconds(t)` | ✅ | Cache as `readonly` field |
| `new int[n]` | ✅ | Pre-allocate as field |
| `"a" + "b"` | ✅ | Use StringBuilder or cache |
| `string.Format(...)` | ✅ | Cache result |
| `Physics2D.OverlapCircleAll()` | ✅ | Use NonAlloc version |
| `gameObject.tag == "X"` | ✅ | Use `CompareTag()` |
| `(object)myInt` | ✅ | Avoid object/non-generic casts |
| `foreach` on array | ✅ | Use `for` loop |
| `foreach` on `List<T>` | ❌ | Safe! |
| `int`, `float`, `bool` (local) | ❌ | Stack — always free |
| `Vector2`, `Vector3` (local) | ❌ | Struct — stack |
| `GetComponent<T>()` (Awake) | ❌ | Cache once, safe |
| `GetComponent<T>()` (Update) | ❌ | No alloc, but SLOW — still cache |

**Previous:** [[14 — Attributes & Reflection]] | **Next:** [[16 — String Handling & StringBuilder]]
