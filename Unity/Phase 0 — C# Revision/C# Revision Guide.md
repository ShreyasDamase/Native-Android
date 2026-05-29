# 🏁 C# Revision Guide
### Focusing on Gaps, Memory & Garbage Collection Optimization

C# is similar to other managed OOP languages (like Java or Kotlin), but Unity developers must master C#-specific memory layout rules and compile-time features. Game development is highly sensitive to memory allocation; improper C# scripting leads to Garbage Collection (GC) spikes, which trigger frame-rate stutters.

---

## 📅 C# Revision Tiers

### ✅ Topics You Can Skip (Concepts match Kotlin/Java 1:1)
*   **Basic Control Flow**: `if/else`, loops (`for`, `foreach`, `while`), switch blocks.
*   **OOP Core**: Classes, interfaces, basic inheritance, accessibility modifiers (`public`, `private`, `protected`).
*   **Generics**: Using standard typed classes/methods (`List<T>`, `Dictionary<K,V>`).
*   **Async/Await Syntax**: Basic Task scheduling patterns (though Unity uses custom variants like `Awaitable` or Coroutines).

---

## 🔴 Tier 1 — Revise Immediately (Used Daily in Unity)

### 1. `class` (Reference Type) vs. `struct` (Value Type)
This is the most critical memory concept in Unity. Structs are value types; classes are reference types.

| Feature | `class` (Reference Type) | `struct` (Value Type) |
| :--- | :--- | :--- |
| **Allocation** | Managed Heap (requires GC cleaning) | Stack (instant cleanup) or inside the owning Object on Heap |
| **Passing Behavior** | By Reference (points to same object in memory) | By Value (makes an exact copy of all fields) |
| **Default Value** | `null` | All fields set to their default values (cannot be null) |
| **Inheritance** | Supports full inheritance (`class A : B`) | Cannot inherit from other structs/classes (implements interfaces only) |
| **Common Unity Examples**| `GameObject`, `MonoBehaviour`, `Transform`, `ScriptableObject` | `Vector2`, `Vector3`, `Quaternion`, `Color`, `RaycastHit2D` |

#### The Struct Modification Trap
In Unity, properties return *copies* of structs, not references. This is why you cannot modify struct properties directly:
```csharp
// WRONG: This will fail to compile. transform.position returns a copy of Vector3 (struct)
transform.position.x = 10f; 

// CORRECT: Copy the struct, modify the local copy, and re-assign it
Vector3 currentPosition = transform.position; // local copy on stack
currentPosition.x = 10f;                     // modify copy
transform.position = currentPosition;         // re-assign (writes changes back)
```

---

### 2. Properties (`get` / `set`)
Properties encapsulate fields. They behave like variables but compile to getter/setter methods.
```csharp
public class Player : MonoBehaviour
{
    // Auto-property with public read-only and private write access
    public float CurrentHealth { get; private set; } = 100f;

    // Expression-bodied property (calculates value on request, acts like a read-only variable)
    public bool IsDead => CurrentHealth <= 0f;

    // Property with backing field and input validation
    private int score;
    public int Score
    {
        get => score;
        set => score = Mathf.Max(0, value); // Prevents negative scores
    }
}
```

---

### 3. Parameter Modifiers: `out`, `ref`, and `in`
*   **`out` (Pass-by-reference, Write-only)**: The method *must* assign a value to this variable before returning. Often used for safe queries.
    ```csharp
    // Inside Update:
    if (TryGetComponent<Rigidbody2D>(out Rigidbody2D rb))
    {
        rb.gravityScale = 1.0f; // Safe to use; variable is declared inline
    }
    ```
*   **`ref` (Pass-by-reference, Read-Write)**: The variable must be initialized before passing. The method can read and modify it.
    ```csharp
    public void SwapFloats(ref float a, ref float b)
    {
        float temp = a;
        a = b;
        b = temp;
    }
    ```
*   **`in` (Pass-by-reference, Read-only)**: Passes a large struct by reference to avoid copying overhead, but prevents modification. Great for math optimization.
    ```csharp
    public float CalculateCustomDistance(in Vector3 targetPos)
    {
        // targetPos cannot be modified here. It is read-only.
        return (transform.position - targetPos).sqrMagnitude; 
    }
    ```

---

### 4. Events, Actions, and Delegates
Delegates are type-safe function pointers. Unity leverages them to build decoupled architectures.
*   `Delegate`: The base type definition.
*   `Action`: A built-in delegate returning `void` (can take parameters).
*   `Func`: A built-in delegate returning a value.
*   `event`: A modifier wrapping delegates that prevents external scripts from clearing or directly invoking the subscribers (adds safety).

```csharp
using System;
using UnityEngine;

public class PlayerHealth : MonoBehaviour
{
    // Best Practice: Use Action with 'event' modifier
    public static event Action<float> OnHealthChanged;

    private float health = 100f;

    public void TakeDamage(float amount)
    {
        health -= amount;
        // ?.Invoke safely checks if there are any subscribers before calling
        OnHealthChanged?.Invoke(health); 
    }
}
```

---

## 🟡 Tier 2 — Revise Within Your First Month

### 1. Modern C# Features (Unity 6 / C# 9 & 10 Support)
Modern Unity supports newer language constructs that simplify code:

*   **Pattern Matching & Switch Expressions**:
    ```csharp
    // Clean, readable matching instead of long switch statements
    string difficultyName = difficultyLevel switch
    {
        1 => "Easy",
        2 => "Medium",
        3 => "Hard",
        _ => "Unknown" // Default fallback case
    };
    ```
*   **Records (`record class` / `record struct`)**: Immutable data containers with built-in value-based equality checking.
    ```csharp
    // Great for passing game state packets or settings data
    public record struct PlayerStatSheet(float speed, int maxHealth, string faction);
    ```
*   **Init-Only Setters (`init`)**: Properties that can only be set during object initialization, keeping the object immutable thereafter.
    ```csharp
    public class WeaponConfig
    {
        public string WeaponName { get; init; }
        public float FireRate { get; init; }
    }
    // Usage: var gun = new WeaponConfig { WeaponName = "Pistol", FireRate = 0.5f };
    // gun.FireRate = 0.2f; // COMPILE ERROR!
    ```
*   **Null-Coalescing Assignment (`??=`)**: Assigns a value only if the target is currently null.
    ```csharp
    private List<Enemy> activeEnemies;
    public void RegisterEnemy(Enemy enemy)
    {
        activeEnemies ??= new List<Enemy>(); // Instantiate list only if null
        activeEnemies.Add(enemy);
    }
    ```

---

## 🧠 C# Memory Management & Garbage Collection (GC)

In game development, frame budget is razor-thin (e.g., 16.6ms for 60 FPS). High memory allocations on the managed heap trigger the Garbage Collector to run, stopping the main thread and causing noticeable stuttering ("GC spikes").

### 1. The Three Memory Spaces in Unity
```
┌────────────────────────────────────────────────────────────────────────┐
│                              SYSTEM RAM                                │
│                                                                        │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐  │
│  │    THE STACK     │    │   MANAGED HEAP   │    │   NATIVE HEAP    │  │
│  │                  │    │                  │    │                  │  │
│  │ ∙ Value types    │    │ ∙ Reference types│    │ ∙ Engine data    │  │
│  │   (structs, ints)│    │   (classes, lists│    │   (meshes, audio,│  │
│  │ ∙ Local variables│    │    strings, arrays)   │    textures, materials)│
│  │ ∙ Scope lifetime │    │ ∙ GC Managed     │    │ ∙ C++ Core       │  │
│  │   (instant clear)│    │   (slow cleanup) │    │   (Manual delete)│  │
│  └──────────────────┘    └──────────────────┘    └──────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```
1.  **The Stack**: Fast, contiguous memory. Holds local variables and parameters. Cleaned up immediately when the executing method exits. No GC overhead.
2.  **The Managed Heap**: Managed by Mono/IL2CPP. Reference types live here. Objects remain until the Garbage Collector runs, traces references, and deletes orphaned allocations.
3.  **The Native Heap**: The C++ core of Unity. Holds textures, meshes, audio data, and scene graphs. Accessing this requires crossing the "C# to C++ bridge", which has overhead.

---

### 2. Common Heap Allocation Pitfalls & Professional Fixes

#### Pitfall A: String Concatenation in Game Loops
Strings are immutable reference types. Modifying or concatenating strings creates a brand-new string object on the heap.
```csharp
// BAD: Allocates a new string on the heap every single frame!
void Update()
{
    scoreText.text = "Score: " + currentScore;
}
```
**Fix:** Cache the score value and only update the text when the score actually changes.
```csharp
private int cachedScore = -1;

public void UpdateScore(int newScore)
{
    if (newScore == cachedScore) return; // Prevent redundant allocations
    cachedScore = newScore;
    scoreText.text = $"Score: {newScore}"; // String interpolation (still allocates, but rarely)
}
```

#### Pitfall B: Box/Unboxing
Converting a value type (like an `int` or a custom `struct`) to a reference type (like `object` or an interface) places a wrapper box on the heap.
```csharp
// BAD: Int is boxed into an object parameter. Allocates heap memory.
Debug.LogFormat("Player reached checkpoint: {0}", 5);

// FIX: Convert value type to string beforehand, or use type-safe overloads
Debug.Log($"Player reached checkpoint: {5.ToString()}");
```

#### Pitfall C: Closures (Lambda Expressions capturing Local Variables)
If a lambda expression uses variables defined outside of its scope, the compiler generates a temporary class on the heap to store those variables (a "closure").
```csharp
// BAD: 'multiplier' is a local variable. The lambda captures it, allocating memory.
public void ApplyStatBuff(List<float> stats, float multiplier)
{
    stats.ForEach(s => s *= multiplier); 
}
```
**Fix:** Use standard loops (`for`/`foreach`) or write static local functions, which cannot capture outside variables and do not allocate memory.
```csharp
// GOOD: Standard loop compiles with zero allocations on stack.
public void ApplyStatBuffOptimized(List<float> stats, float multiplier)
{
    for (int i = 0; i < stats.Count; i++)
    {
        stats[i] *= multiplier;
    }
}
```

#### Pitfall D: Physics & Tag Comparisons
Certain queries return freshly instantiated arrays, and legacy string comparisons cause issues.
```csharp
// BAD: Allocates a new array of Colliders on the heap every execution.
Collider2D[] hits = Physics2D.OverlapCircleAll(point, radius);

// BAD: Comparing tags using string evaluation allocates memory.
if (collision.gameObject.tag == "Obstacle") { ... }
```
**Fix:** Use NonAlloc physics queries and `CompareTag()`.
```csharp
private Collider2D[] hitsBuffer = new Collider2D[10];

void CheckCollision()
{
    // Reuses the buffer, returns the hit count without allocating an array
    int count = Physics2D.OverlapCircleNonAlloc(point, radius, hitsBuffer);
    for (int i = 0; i < count; i++)
    {
        // CompareTag performs a native-side check with zero C# allocations
        if (hitsBuffer[i].gameObject.CompareTag("Obstacle")) 
        {
            // Process hit
        }
    }
}
```

#### Pitfall E: Collection Capacity Resizing
When lists or dictionaries exceed their size limit, they allocate a new larger internal array and copy elements over, leaving the old array to be garbage collected.
```csharp
// BAD: Resizes multiple times as it populates
List<int> scoreHistory = new List<int>();
```
**Fix:** Always define an initial capacity if you know the approximate final size.
```csharp
// GOOD: Allocates the required memory once, avoiding resizing operations
List<int> scoreHistory = new List<int>(100); 
```

---

## 📖 Recommended C# Learning Resources

*   [Microsoft C# Documentation](https://learn.microsoft.com/en-us/dotnet/csharp/) — Comprehensive language specs.
*   [Unity Performance Optimization Manual](https://docs.unity3d.com/6000.3/Documentation/Manual/best-practice-understanding-performance-in-unity.html) — Official documentation on performance best practices.
*   [C# for Unity — GameDev Academy](https://gamedevacademy.org/csharp-definitive-guide/) — Practical Unity coding patterns.
