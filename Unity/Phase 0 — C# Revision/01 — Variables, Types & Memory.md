# 01 — Variables, Types & Memory
### 🟢 Block A — Language Foundations

> [!NOTE]
> This is the most critical chapter. Everything in Unity — positions, health, scores, GameObjects — is a variable. Understanding **where** data lives in memory explains why Unity's API behaves the way it does.

---

## 1.1 — The Two Kinds of Types

C# types fall into **two fundamental categories**. This distinction controls everything about how data is stored and shared.

| Category | Examples | Lives On | Passed By | Can be `null`? |
| :--- | :--- | :--- | :--- | :--- |
| **Value Type** | `int`, `float`, `bool`, `Vector2`, `Vector3`, `Quaternion`, `Color`, `struct` | Stack | Copy | ❌ No |
| **Reference Type** | `class`, `string`, `array`, `List<T>`, `GameObject`, `MonoBehaviour` | Heap | Reference | ✅ Yes |

### Why This Matters in Unity

```csharp
// --- VALUE TYPE EXAMPLE ---
Vector3 a = new Vector3(1, 0, 0);
Vector3 b = a;          // b is a COPY — independent from a
b.x = 99f;
Debug.Log(a.x);         // Still 1 — a was not changed

// --- REFERENCE TYPE EXAMPLE ---
List<int> listA = new List<int>() { 1, 2, 3 };
List<int> listB = listA;    // listB POINTS to the same object
listB.Add(99);
Debug.Log(listA.Count);     // 4 — listA was changed because both point to same object
```

---

## 1.2 — The Three Memory Spaces

```
┌────────────────────────────────────────────────────────────────────────┐
│                              SYSTEM RAM                                │
│                                                                        │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐  │
│  │    THE STACK     │    │   MANAGED HEAP   │    │   NATIVE HEAP    │  │
│  │                  │    │                  │    │                  │  │
│  │  int, float,     │    │  classes, lists, │    │  Textures, Meshes│  │
│  │  bool, Vector2,  │    │  strings, arrays,│    │  Audio clips,    │  │
│  │  Vector3,        │    │  GameObjects     │    │  Materials       │  │
│  │  Quaternion      │    │                  │    │  (C++ engine)    │  │
│  │                  │    │  GC cleans this  │    │                  │  │
│  │  Auto-cleared    │    │  (can cause lag) │    │  You must Destroy │  │
│  └──────────────────┘    └──────────────────┘    └──────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

- **Stack**: Fast. Cleaned instantly when the method ends. Zero GC overhead.
- **Managed Heap**: Where all your `new MyClass()` objects live. The Garbage Collector (GC) periodically frees unused memory — but this causes **frame stutters** (GC spikes).
- **Native Heap**: The C++ engine side. You don't allocate here directly, but `Destroy()`, `Instantiate()`, loading assets — all touch this.

---

## 1.3 — Built-In Value Types (Primitives)

```csharp
// Integer types
int score = 0;           // 32-bit signed integer (-2.1B to +2.1B)
long bigNumber = 0L;     // 64-bit signed integer (for huge IDs)
short smallInt = 0;      // 16-bit (used in tight memory situations)
byte flags = 0;          // 0-255 (pixel color channels, bitmasks)

// Floating-point types
float speed = 5.5f;      // 32-bit float — use 'f' suffix, this is Unity's standard
double precise = 5.5;    // 64-bit double — rarely used in Unity (slower on mobile)

// Other primitives
bool isGrounded = false; // true / false
char letter = 'A';       // single Unicode character

// Unity-specific value types (structs)
Vector2 pos2D = new Vector2(1f, 0f);     // x, y
Vector3 pos3D = new Vector3(1f, 0f, 0f); // x, y, z
Quaternion rot = Quaternion.identity;    // rotation (never modify x,y,z,w directly!)
Color red = Color.red;                   // r, g, b, a (0f to 1f each)
```

> [!TIP]
> Always use `float` (not `double`) in Unity. All Unity APIs (`Vector3`, `Mathf`, `Time.deltaTime`) use `float`. Mixing doubles forces conversions and can cause subtle bugs.

---

## 1.4 — Reference Types You'll Use Daily

```csharp
// String — immutable reference type (every change creates a new object)
string playerName = "Hero";
string greeting = "Hello, " + playerName; // allocates a new string on the heap!

// Arrays — fixed-size, reference type
int[] enemyCounts = new int[10];          // size fixed at creation
float[] weights = { 1f, 2f, 3f };        // initialization shorthand

// Classes (reference types)
GameObject player;        // null until assigned
Transform myTransform;    // null until assigned
Rigidbody2D rb;           // null until assigned via GetComponent or Inspector

// Collections
List<int> scores = new List<int>(20);     // dynamic size, pass initial capacity!
Dictionary<string, int> items = new Dictionary<string, int>();
```

---

## 1.5 — Nullable Types (`?`)

Value types normally cannot be `null`. But sometimes you need "no value" to be a valid state (e.g., "has the player picked a class yet?").

```csharp
// Non-nullable int — must always have a value
int level = 0;

// Nullable int — can represent "no value"
int? selectedLevel = null;

// Check before using
if (selectedLevel.HasValue)
{
    Debug.Log($"Selected level: {selectedLevel.Value}");
}

// Null-coalescing: use a fallback if null
int levelToLoad = selectedLevel ?? 1; // use selectedLevel if set, else 1

// Null-conditional: don't crash if null
string name = player?.name ?? "Unknown"; // safe chain
```

---

## 1.6 — Constants and Read-Only

```csharp
public class GameConfig : MonoBehaviour
{
    // const: compile-time constant — baked into IL, no memory at runtime
    // Use for mathematical constants or values that NEVER change across builds
    private const float GRAVITY_SCALE = 2.5f;
    private const int MAX_PLAYERS = 4;

    // readonly: runtime constant — set once in constructor or field initializer
    // Use when the value is set once but might differ between instances
    private readonly string playerID;

    // static readonly: shared across all instances, set once
    public static readonly Vector3 WorldUp = Vector3.up;

    void Awake()
    {
        playerID = System.Guid.NewGuid().ToString(); // set once, never changes
    }
}
```

---

## 1.7 — Variable Scope

```csharp
public class EnemyAI : MonoBehaviour
{
    // CLASS SCOPE — accessible by all methods in this class
    private float speed = 3f;
    private bool isChasing;

    void Update()
    {
        // METHOD SCOPE — only visible inside Update()
        float distanceToPlayer = Vector3.Distance(transform.position, target.position);

        if (distanceToPlayer < 5f)
        {
            // BLOCK SCOPE — only visible inside this if block
            Vector3 direction = (target.position - transform.position).normalized;
            transform.position += direction * speed * Time.deltaTime;
        }

        // distanceToPlayer is accessible here — still in Update() scope
        // direction is NOT accessible here — out of the if block scope
    }
}
```

---

## 1.8 — 🎮 2D vs 3D: Which Variables You Use

### 🎮 In 2D Projects

```csharp
// Position — uses Vector2 for logic, but transform uses Vector3
Vector2 moveInput = new Vector2(Input.GetAxisRaw("Horizontal"), 0f);
Rigidbody2D rb2d = GetComponent<Rigidbody2D>();
rb2d.linearVelocity = moveInput * speed;

// Physics hit data — all 2D structs
RaycastHit2D hit = Physics2D.Raycast(transform.position, Vector2.down, 1f);
Collider2D[] nearby = new Collider2D[10]; // pre-allocated buffer
```

### 🎮 In 3D Projects

```csharp
// Position — uses Vector3
Vector3 moveInput = new Vector3(Input.GetAxisRaw("Horizontal"), 0f, Input.GetAxisRaw("Vertical"));
Rigidbody rb3d = GetComponent<Rigidbody>();
rb3d.linearVelocity = moveInput * speed;

// Physics hit data — 3D structs
RaycastHit hit3d;
bool didHit = Physics.Raycast(transform.position, Vector3.down, out hit3d, 1f);
```

---

## 📝 Summary

| Concept | Key Rule |
| :--- | :--- |
| Value type | Copied when assigned. On the stack. No GC. |
| Reference type | Shared when assigned. On the heap. GC cleans up. |
| `const` | Compile-time, never changes |
| `readonly` | Runtime constant, set once |
| `nullable T?` | Value types that can also be null |
| Vector2/3 | Structs (value types!) — always copy when modifying components |

**Next:** [[02 — Control Flow & Pattern Matching]]
