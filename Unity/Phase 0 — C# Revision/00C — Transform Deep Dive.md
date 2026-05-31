# 00C — Transform Deep Dive
### 🔶 Block 0 — Unity Engine Fundamentals

> [!IMPORTANT]
> `Transform` is the **only component that every GameObject always has** — you cannot remove it. It controls where an object is, which direction it's facing, and how big it is. Understanding Transform is essential before understanding ANY movement code.

---

## 00C.1 — What is the Transform Component?

Every GameObject has exactly one `Transform` component. It stores three things:

```
Transform
├── Position   → Where is this object in the world?  (X, Y, Z coordinates)
├── Rotation   → Which direction is it facing?        (as Euler angles X, Y, Z in degrees)
└── Scale      → How big/small is it?                 (multiplier: 1 = normal, 2 = double size)
```

In the **Inspector**, you see the Transform at the top of every GameObject:

```
Transform
  Position    X [ 0 ]    Y [ 0 ]    Z [ 0 ]
  Rotation    X [ 0 ]    Y [ 0 ]    Z [ 0 ]
  Scale       X [ 1 ]    Y [ 1 ]    Z [ 1 ]
```

In **code**, you access the Transform through `transform` (lowercase — it's a built-in property of MonoBehaviour):

```csharp
// Access your own Transform — lowercase 'transform'
Vector3 myPos = transform.position;
Quaternion myRot = transform.rotation;
Vector3 myScale = transform.localScale;

// Access another object's Transform through its component
Transform enemyTransform = enemyGameObject.transform;
Vector3 enemyPos = enemyTransform.position;
```

---

## 00C.2 — Position — Where Things Are

```csharp
// ===== READING POSITION =====
Vector3 worldPos = transform.position;      // position in WORLD space (absolute)
Vector3 localPos = transform.localPosition; // position RELATIVE to parent

// Get individual axes
float x = transform.position.x;
float y = transform.position.y;
float z = transform.position.z; // in 2D games, usually 0

// ===== SETTING POSITION =====
// ❌ WRONG — Vector3 is a struct; you can't set individual components through a property
transform.position.x = 5f; // COMPILE ERROR

// ✅ CORRECT — always set the whole vector
transform.position = new Vector3(5f, 0f, 0f);   // set to absolute position
transform.localPosition = new Vector3(0f, 1f, 0f); // set relative to parent

// ===== CHANGING POSITION =====
// Move by adding a vector (frame-rate dependent without deltaTime — bad!)
transform.position += new Vector3(1f, 0f, 0f); // move 1 unit right

// Frame-rate INDEPENDENT movement (correct way)
float speed = 5f;
transform.position += Vector3.right * speed * Time.deltaTime;

// Translate — moves in the object's LOCAL direction by default
transform.Translate(Vector3.forward * speed * Time.deltaTime);          // moves in local forward
transform.Translate(Vector3.forward * speed * Time.deltaTime, Space.World); // moves in world forward

// Move toward a target at a constant speed (stops exactly when reached)
transform.position = Vector3.MoveTowards(transform.position, target, speed * Time.deltaTime);

// Lerp toward a target (gets close but never exactly reaches — exponential decay)
transform.position = Vector3.Lerp(transform.position, target, 5f * Time.deltaTime);

// ===== COMMON PATTERNS =====
// Spawn something at my position
Instantiate(prefab, transform.position, Quaternion.identity);

// Spawn offset from my position
Vector3 spawnPos = transform.position + Vector3.up * 2f; // 2 units above me
Instantiate(prefab, spawnPos, Quaternion.identity);

// Teleport (instant, no physics) — for respawning
transform.position = respawnPoint.position;

// ===== DISTANCE =====
float dist = Vector3.Distance(transform.position, target.position);
float distSq = (target.position - transform.position).sqrMagnitude; // faster — no sqrt
```

---

## 00C.3 — World Space vs Local Space

This is one of the trickiest concepts for beginners.

```
WORLD SPACE                          LOCAL SPACE
The entire game world.               Relative to the parent object.
Origin is (0, 0, 0) always.         Origin is the parent's position.

transform.position                   transform.localPosition
transform.rotation                   transform.localRotation
transform.lossyScale                 transform.localScale
```

**Visual Example:**
```
WORLD:
(0,0,0) ──────────────────────── World X+ →

    Parent at (5, 0, 0)
        └── Child at (1, 0, 0) LOCAL → WORLD position = (6, 0, 0)
              (parent X + child local X = 5 + 1 = 6)

    If parent ROTATES 90°, child also rotates in world space
    but its LOCAL position stays (1, 0, 0)
```

```csharp
// Example: A gun attached to a player
public class Gun : MonoBehaviour
{
    // This gun is a CHILD of the Player GameObject
    
    void Start()
    {
        // localPosition = position relative to Player
        // Player is at world (5, 2, 0), gun offset is (0.5, 0, 0)
        Debug.Log(transform.localPosition); // (0.5, 0, 0) — relative to player
        Debug.Log(transform.position);      // (5.5, 2, 0) — in world space
        
        // When the player moves, the gun moves WITH it automatically
        // because it's a child in the hierarchy
    }
}

// Transform conversion methods
Vector3 worldPoint = new Vector3(5f, 2f, 0f);

// Convert world position → local position (relative to this transform)
Vector3 localPoint = transform.InverseTransformPoint(worldPoint);

// Convert local position → world position
Vector3 backToWorld = transform.TransformPoint(localPoint);

// Convert world direction → local direction
Vector3 worldDir = Vector3.right;
Vector3 localDir = transform.InverseTransformDirection(worldDir);

// Convert local direction → world direction
Vector3 backToWorldDir = transform.TransformDirection(localDir);
```

---

## 00C.4 — Rotation — How Things Face

Rotation in Unity is stored as a **Quaternion** (4D math) internally, but you interact with it through **Euler angles** (degrees around X, Y, Z axes).

```csharp
// ===== READING ROTATION =====
Quaternion rot = transform.rotation;        // raw quaternion (world space)
Vector3 euler = transform.eulerAngles;      // as degrees (world space)
Vector3 localEuler = transform.localEulerAngles; // as degrees (local space)

float currentAngleZ = transform.eulerAngles.z; // Z rotation in degrees (2D)

// ===== SETTING ROTATION =====
// Set via Euler angles (degrees)
transform.rotation = Quaternion.Euler(0f, 90f, 0f); // face right in 3D

// 2D rotation — only Z matters
transform.rotation = Quaternion.Euler(0f, 0f, 45f); // 45 degrees

// Identity — no rotation (facing default direction)
transform.rotation = Quaternion.identity;

// ===== ROTATING =====
// Rotate by angle each frame (frame-rate dependent — needs deltaTime)
transform.Rotate(0f, 0f, 90f * Time.deltaTime); // spin 90 degrees/second around Z

// Rotate around a specific axis
transform.RotateAround(pivot.position, Vector3.up, 45f * Time.deltaTime);

// ===== LOOK AT TARGET =====
// Make object face a target instantly (3D)
transform.LookAt(target.position);
transform.LookAt(target.position, Vector3.up); // with explicit up vector

// 2D look-at (rotate around Z to face a 2D direction)
Vector2 direction = ((Vector2)target.position - (Vector2)transform.position).normalized;
float angle = Mathf.Atan2(direction.y, direction.x) * Mathf.Rad2Deg;
transform.rotation = Quaternion.Euler(0f, 0f, angle);

// Smooth rotation toward a target (Slerp = spherical interpolation for rotations)
Quaternion targetRot = Quaternion.LookRotation(direction);
transform.rotation = Quaternion.Slerp(transform.rotation, targetRot, 10f * Time.deltaTime);
transform.rotation = Quaternion.RotateTowards(transform.rotation, targetRot, 360f * Time.deltaTime);

// ===== DIRECTION VECTORS FROM ROTATION =====
Vector3 forward = transform.forward;   // direction the object is "facing" (local Z+)
Vector3 right   = transform.right;     // local X+ direction
Vector3 up      = transform.up;        // local Y+ direction

// These update automatically as you rotate the object!
// Used for movement relative to the object's facing direction
rb.AddForce(transform.forward * thrustForce); // move in the direction you're facing
```

---

## 00C.5 — Scale

```csharp
// ===== READING SCALE =====
Vector3 localScale = transform.localScale;    // set in Inspector — relative to parent
Vector3 worldScale  = transform.lossyScale;   // actual size in world (read-only, accounts for parent scale)

// ===== SETTING SCALE =====
transform.localScale = new Vector3(2f, 2f, 1f); // twice as wide and tall in 2D

// Flip horizontally (mirror a sprite in 2D)
transform.localScale = new Vector3(-1f, 1f, 1f); // flip X axis
transform.localScale = new Vector3(1f, 1f, 1f);  // unflip

// Uniform scale
float size = 1.5f;
transform.localScale = Vector3.one * size; // shorthand for (size, size, size)

// Animated scale (pulsing, "juice" effect)
void Update()
{
    float pulse = 1f + Mathf.Sin(Time.time * 3f) * 0.05f; // subtle pulse
    transform.localScale = Vector3.one * pulse;
}

// ⚠️ IMPORTANT: Avoid non-uniform scale on parent objects with physics children
// It causes the physics engine to behave incorrectly
// Use uniform scale where possible, or adjust child positions instead
```

---

## 00C.6 — Parent-Child Hierarchy

Unity's hierarchy window shows parent-child relationships. Child objects **inherit** the parent's position, rotation, and scale.

```
Hierarchy:
▼ Player              ← Parent
    ├── Sprite        ← Child — moves/rotates WITH player
    ├── WeaponMount   ← Child — mount point for weapons
    │   └── Gun       ← Grandchild
    └── GroundCheck   ← Child — invisible point used for ground detection
```

```csharp
// ===== PARENTING IN CODE =====
// Set this object's parent
transform.SetParent(parentTransform);               // world position changes
transform.SetParent(parentTransform, worldPositionStays: true);  // keeps world pos
transform.SetParent(parentTransform, worldPositionStays: false); // snaps to parent origin
transform.SetParent(null); // unparent — become a root object in the scene

// Get the parent
Transform parent = transform.parent;
bool hasParent = transform.parent != null;

// ===== NAVIGATING THE HIERARCHY =====
// Get a child by index
Transform firstChild = transform.GetChild(0);
int childCount = transform.childCount; // number of direct children

// Iterate over direct children
for (int i = 0; i < transform.childCount; i++)
{
    Transform child = transform.GetChild(i);
    Debug.Log(child.name);
}

// Find a child by name (slow — searches entire subtree)
Transform gunMount = transform.Find("WeaponMount/Gun"); // path-based search
Transform gunPoint = transform.Find("MuzzlePoint");     // direct child search

// Get all components in children
Renderer[] allRenderers = GetComponentsInChildren<Renderer>();
Collider2D[] allColliders = GetComponentsInChildren<Collider2D>();

// Destroy all children (e.g., clearing a container)
for (int i = transform.childCount - 1; i >= 0; i--)
{
    Destroy(transform.GetChild(i).gameObject);
}

// ===== PRACTICAL EXAMPLES =====

// Weapon pickup — attach to player's hand
public class WeaponPickup : MonoBehaviour
{
    [SerializeField] private Transform playerHandTransform;
    
    void OnTriggerEnter2D(Collider2D other)
    {
        if (other.CompareTag("Player"))
        {
            // Move this weapon to the player's hand
            transform.SetParent(playerHandTransform, worldPositionStays: false);
            transform.localPosition = Vector3.zero;  // snap to hand center
            transform.localRotation = Quaternion.identity;
        }
    }
}

// Container that holds spawned enemies — easy to find and count them
public class EnemyContainer : MonoBehaviour
{
    public int ActiveEnemyCount => transform.childCount;
    
    public void SpawnEnemy(GameObject prefab, Vector3 pos)
    {
        GameObject enemy = Instantiate(prefab, pos, Quaternion.identity, transform);
        // The 4th argument to Instantiate is the parent — enemy is now a child
    }
    
    public void DestroyAll()
    {
        for (int i = transform.childCount - 1; i >= 0; i--)
            Destroy(transform.GetChild(i).gameObject);
    }
}
```

---

## 00C.7 — Useful Transform Properties

```csharp
// The GameObject this Transform belongs to
GameObject go = transform.gameObject;

// The Transform's component list (shortcut)
Transform t = GetComponent<Transform>(); // same as 'transform'

// World position shortcuts (as Vector2 for 2D games)
Vector2 pos2D = transform.position; // implicit cast — drops Z

// Distance utilities
float dist = Vector3.Distance(transform.position, other.position);
float distFlat = Vector2.Distance(transform.position, other.position); // 2D, ignores Z

// Check if a position is in front of this object
Vector3 toTarget = target.position - transform.position;
bool inFront = Vector3.Dot(transform.forward, toTarget) > 0f;

// Get the angle to a target (2D)
float angleToTarget = Vector2.SignedAngle(
    transform.right,
    (Vector2)(target.position - transform.position)
);
```

---

## 00C.8 — Common Beginner Mistakes with Transform

```csharp
// ❌ MISTAKE 1: Modifying position component directly (won't compile)
transform.position.x = 5f; // ERROR — Vector3 is a struct, property returns copy

// ✅ FIX: Replace the whole vector
Vector3 pos = transform.position;
pos.x = 5f;
transform.position = pos;
// OR in Unity 6:
transform.SetPositionAndRotation(new Vector3(5f, transform.position.y, transform.position.z), transform.rotation);

// ❌ MISTAKE 2: Using transform.position for physics objects
// When you have a Rigidbody, setting transform.position directly BYPASSES physics
// It teleports the object, potentially through walls!
void Update()
{
    transform.position = new Vector3(5f, 0f, 0f); // ❌ teleports, ignores physics
}

// ✅ FIX: Use Rigidbody for physics objects
void FixedUpdate()
{
    rb.MovePosition(rb.position + Vector2.right * speed * Time.fixedDeltaTime); // ✅
}

// ❌ MISTAKE 3: Forgetting localPosition vs position
// When parented, setting transform.position sets WORLD position
// transform.localPosition = (0,0,0) means "at the parent's center"
// transform.position = (0,0,0) means "at the world origin"

// ❌ MISTAKE 4: Using transform.Rotate without Time.deltaTime
void Update()
{
    transform.Rotate(0f, 0f, 90f); // spins at 90° PER FRAME → 5400°/second at 60FPS!
}
// ✅ FIX:
void Update()
{
    transform.Rotate(0f, 0f, 90f * Time.deltaTime); // 90° per SECOND — frame-rate independent
}
```

---

## 📝 Summary — Transform Quick Reference

| Property/Method | What It Does |
| :--- | :--- |
| `transform.position` | World position (X, Y, Z) |
| `transform.localPosition` | Position relative to parent |
| `transform.rotation` | World rotation (Quaternion) |
| `transform.eulerAngles` | World rotation as degrees (X, Y, Z) |
| `transform.localScale` | Scale relative to parent |
| `transform.forward` | Direction this object is "looking" |
| `transform.right` | Direction to the object's right |
| `transform.up` | Direction above the object |
| `transform.parent` | The parent Transform (null = root) |
| `transform.childCount` | Number of direct children |
| `transform.GetChild(i)` | Get child by index |
| `transform.SetParent(t)` | Set parent |
| `transform.Translate(v)` | Move by vector |
| `transform.Rotate(x,y,z)` | Rotate by Euler angles |
| `transform.LookAt(t)` | Face a target (3D) |
| `TransformPoint(v)` | Local → World position |
| `InverseTransformPoint(v)` | World → Local position |

**Previous:** [[00B — MonoBehaviour & Script Lifecycle]] | **Next:** [[00D — Rigidbody2D — Every Variable Explained]]
