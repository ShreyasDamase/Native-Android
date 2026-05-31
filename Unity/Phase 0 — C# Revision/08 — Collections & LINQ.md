# 08 — Collections & LINQ
### 🟡 Block A — Language Foundations

> [!WARNING]
> LINQ is powerful, but **avoid it in hot paths** (Update, FixedUpdate, collision callbacks). LINQ methods allocate heap memory and can cause GC spikes in gameplay loops. Use them freely in editor utilities, initialization, and loading screens.

---

## 8.1 — Arrays

Fixed-size, fastest iteration, zero allocation per loop.

```csharp
// Declaration
int[] scores = new int[10];               // fixed size of 10
float[] weights = { 1f, 2f, 3f };         // initialized inline (size = 3)
string[] levels = new string[] { "Level1", "Level2" };

// Access
scores[0] = 100;              // set element at index 0
int first = scores[0];        // get element
int length = scores.Length;   // number of elements

// Multi-dimensional array
float[,] grid = new float[10, 10];  // 10x10 grid
grid[3, 5] = 1f;

// Jagged array (array of arrays — each row can have different length)
int[][] jaggedGrid = new int[5][];
jaggedGrid[0] = new int[3];
jaggedGrid[1] = new int[7];

// Fast iteration — no allocation
for (int i = 0; i < scores.Length; i++)
{
    scores[i] += 10;
}
```

---

## 8.2 — List`<T>`

Dynamic array — grows as needed. Most commonly used collection in Unity.

```csharp
// Creation — ALWAYS provide initial capacity if you know the approximate size
List<Enemy> enemies = new List<Enemy>();          // no initial capacity (will resize)
List<Enemy> enemies2 = new List<Enemy>(50);       // ✅ pre-allocated for 50 enemies

// Core operations
enemies.Add(newEnemy);                            // append to end
enemies.Insert(0, firstEnemy);                    // insert at index 0
enemies.Remove(deadEnemy);                        // remove by reference (O(n))
enemies.RemoveAt(3);                              // remove by index (O(n))
enemies.RemoveSwapBack(3);                        // custom: swap with last, then remove (O(1)) 
int count = enemies.Count;                        // current element count
bool hasEnemy = enemies.Contains(targetEnemy);    // O(n) search
enemies.Clear();                                  // remove all elements

// Sorting
enemies.Sort((a, b) => a.Health.CompareTo(b.Health)); // sort by health ascending

// Find (avoid in hot paths — allocates)
Enemy weakest = enemies.Find(e => e.Health < 20f);

// Index access
Enemy first = enemies[0];
```

### Removing Items During Iteration (Correct Way)

```csharp
// ❌ WRONG — modifying a list while iterating throws an exception
foreach (Enemy e in enemies)
{
    if (e.IsDead) enemies.Remove(e); // throws InvalidOperationException!
}

// ✅ CORRECT — iterate backwards when removing by index
for (int i = enemies.Count - 1; i >= 0; i--)
{
    if (enemies[i].IsDead)
        enemies.RemoveAt(i);
}

// ✅ ALTERNATIVE — collect indices, then remove
List<int> toRemove = new List<int>();
for (int i = 0; i < enemies.Count; i++)
    if (enemies[i].IsDead) toRemove.Add(i);

for (int i = toRemove.Count - 1; i >= 0; i--)
    enemies.RemoveAt(toRemove[i]);
```

---

## 8.3 — Dictionary`<K, V>`

Hash map for fast key-based lookups. O(1) average lookup.

```csharp
// Creation
Dictionary<int, Enemy> enemyByID = new Dictionary<int, Enemy>();
Dictionary<string, int> itemCount = new Dictionary<string, int>(50); // with capacity

// Adding / Updating
enemyByID[42] = someEnemy;               // add or update (overwrites if key exists)
enemyByID.Add(42, someEnemy);            // throws if key already exists!

// Safe lookup — ALWAYS use TryGetValue
if (enemyByID.TryGetValue(42, out Enemy found))
{
    found.TakeDamage(10f);
}

// Check existence
bool hasKey = enemyByID.ContainsKey(42);
bool hasVal = enemyByID.ContainsValue(someEnemy); // O(n) — avoid

// Removing
enemyByID.Remove(42);

// Iterating
foreach (KeyValuePair<int, Enemy> pair in enemyByID)
{
    Debug.Log($"ID: {pair.Key}, Enemy: {pair.Value.name}");
}

// Keys and Values collections
foreach (int id in enemyByID.Keys) { /* ... */ }
foreach (Enemy e in enemyByID.Values) { /* ... */ }
```

### Common Unity Dictionary Patterns

```csharp
public class TagToLayerCache : MonoBehaviour
{
    // Cache expensive lookups in a Dictionary
    private static Dictionary<string, int> layerCache = new Dictionary<string, int>(16);
    
    public static int GetLayer(string layerName)
    {
        if (!layerCache.TryGetValue(layerName, out int layer))
        {
            layer = LayerMask.NameToLayer(layerName);
            layerCache[layerName] = layer;
        }
        return layer;
    }
}

// Component registry pattern — fast access by ID
public class EnemyRegistry : MonoBehaviour
{
    private Dictionary<int, EnemyAI> registry = new Dictionary<int, EnemyAI>(100);
    
    public void Register(EnemyAI enemy)   => registry[enemy.GetInstanceID()] = enemy;
    public void Unregister(EnemyAI enemy) => registry.Remove(enemy.GetInstanceID());
    
    public bool TryGet(int id, out EnemyAI enemy) => registry.TryGetValue(id, out enemy);
}
```

---

## 8.4 — Queue`<T>` and Stack`<T>`

```csharp
// Queue — FIFO (first in, first out) — spawn queues, command queues
Queue<SpawnRequest> spawnQueue = new Queue<SpawnRequest>();

spawnQueue.Enqueue(new SpawnRequest { EnemyType = 1, Position = spawnPoint1 });
spawnQueue.Enqueue(new SpawnRequest { EnemyType = 2, Position = spawnPoint2 });

if (spawnQueue.Count > 0)
{
    SpawnRequest next = spawnQueue.Dequeue();  // removes and returns first
    SpawnRequest peek = spawnQueue.Peek();     // returns first WITHOUT removing
    SpawnEnemy(next);
}

// Stack — LIFO (last in, first out) — undo systems, state stacks
Stack<GameState> stateStack = new Stack<GameState>();

stateStack.Push(GameState.Playing);
stateStack.Push(GameState.Paused); // Paused is now on top

GameState current = stateStack.Peek();  // Paused (top, without removing)
GameState popped = stateStack.Pop();    // Paused (removed from top)
// Now top is Playing again
```

---

## 8.5 — HashSet`<T>`

```csharp
// HashSet — unique items, O(1) contains check
HashSet<string> unlockedAchievements = new HashSet<string>();

unlockedAchievements.Add("FIRST_KILL");
unlockedAchievements.Add("FIRST_KILL"); // duplicate — silently ignored

bool hasAchievement = unlockedAchievements.Contains("FIRST_KILL"); // O(1) — fast!

// Perfect for visited rooms in a procedural dungeon
HashSet<Vector2Int> visitedRooms = new HashSet<Vector2Int>();
visitedRooms.Add(new Vector2Int(2, 3));
bool visited = visitedRooms.Contains(new Vector2Int(2, 3)); // true
```

---

## 8.6 — LINQ — Powerful but Allocating

LINQ (Language Integrated Query) provides SQL-like operations on collections. **Fantastic for editor scripts, loading, and setup. Avoid in per-frame gameplay code.**

```csharp
using System.Linq;

List<Enemy> enemies = GetAllEnemies();

// --- Filtering ---
// Where — filter elements
IEnumerable<Enemy> alive = enemies.Where(e => e.Health > 0);
List<Enemy> aliveList = enemies.Where(e => e.Health > 0).ToList(); // allocates!

// --- Transforming ---
// Select — transform each element
IEnumerable<float> healthValues = enemies.Select(e => e.Health);
List<string> names = enemies.Select(e => e.gameObject.name).ToList();

// --- Aggregating ---
float totalHealth = enemies.Sum(e => e.Health);
float maxHealth = enemies.Max(e => e.Health);
float minHealth = enemies.Min(e => e.Health);
float avgHealth = enemies.Average(e => e.Health);
int aliveCount = enemies.Count(e => e.Health > 0);

// --- Finding ---
Enemy weakest = enemies.OrderBy(e => e.Health).FirstOrDefault();
Enemy strongest = enemies.OrderByDescending(e => e.Health).FirstOrDefault();
bool anyElite = enemies.Any(e => e.IsElite);
bool allDead = enemies.All(e => e.Health <= 0);

// --- Sorting ---
List<Enemy> sortedByHealth = enemies.OrderBy(e => e.Health).ToList();
List<Enemy> sortedDesc = enemies.OrderByDescending(e => e.Health).ToList();

// --- Safe LINQ in Unity (editor/loading only) ---
void SetupLevel(List<RoomData> rooms)
{
    var startRooms = rooms.Where(r => r.IsStartRoom).ToList();
    var sortedRooms = rooms.OrderBy(r => r.ID).ToList();
    // LINQ is fine here — called once on level load, not every frame
}
```

---

## 8.7 — LINQ vs Loop Performance

```csharp
// LINQ — clean but allocates (OK for editor/init, avoid in Update)
Enemy nearest = enemies.OrderBy(e => Vector3.Distance(transform.position, e.transform.position))
                       .FirstOrDefault(); // Allocates sorted collection

// Manual loop — more verbose but ZERO allocations
Enemy FindNearestEnemy()
{
    Enemy nearest = null;
    float nearestDist = float.MaxValue;
    
    for (int i = 0; i < enemies.Count; i++)
    {
        if (enemies[i] == null) continue;
        float dist = Vector3.Distance(transform.position, enemies[i].transform.position);
        if (dist < nearestDist)
        {
            nearestDist = dist;
            nearest = enemies[i];
        }
    }
    return nearest;
}
```

---

## 8.8 — 🎮 2D vs 3D: Collection Usage

### 🎮 2D — Tilemaps and Room Data
```csharp
// 2D dungeon rooms tracked as grid positions
Dictionary<Vector2Int, RoomData> rooms = new Dictionary<Vector2Int, RoomData>();
HashSet<Vector2Int> visited = new HashSet<Vector2Int>();

Vector2Int currentRoom = new Vector2Int(0, 0);
visited.Add(currentRoom);

// Find all adjacent unvisited rooms
List<Vector2Int> adjacent = new List<Vector2Int>
{
    currentRoom + Vector2Int.up,
    currentRoom + Vector2Int.down,
    currentRoom + Vector2Int.left,
    currentRoom + Vector2Int.right
};

List<Vector2Int> unvisited = adjacent.Where(r => !visited.Contains(r) && rooms.ContainsKey(r)).ToList();
```

### 🎮 3D — NavMesh Waypoints
```csharp
// 3D patrol points as a list of Vector3
List<Vector3> patrolPoints = new List<Vector3>();
Queue<Vector3> patrolQueue = new Queue<Vector3>(patrolPoints); // copy into queue

// 3D area coverage — track visited positions in world space
HashSet<Vector3Int> coveredAreas = new HashSet<Vector3Int>();

Vector3Int WorldToCell(Vector3 worldPos)
{
    return new Vector3Int(
        Mathf.RoundToInt(worldPos.x / cellSize),
        Mathf.RoundToInt(worldPos.y / cellSize),
        Mathf.RoundToInt(worldPos.z / cellSize)
    );
}
```

---

## 📝 Summary

| Collection | Best For | Avoid When |
| :--- | :--- | :--- |
| `Array` | Fixed-size, fast iteration | Need to resize |
| `List<T>` | Dynamic-size ordered data | Hot-path removal (use reverse loop) |
| `Dictionary<K,V>` | Fast key lookup | Value search (use List instead) |
| `Queue<T>` | FIFO ordered processing | Random access |
| `Stack<T>` | LIFO state/undo | Random access |
| `HashSet<T>` | Fast unique membership | Ordered data |
| LINQ | Editor, init, loading | `Update()`, `FixedUpdate()`, physics callbacks |

**Previous:** [[07 — Generics & Constraints]] | **Next:** [[09 — Delegates, Actions & Events]]
