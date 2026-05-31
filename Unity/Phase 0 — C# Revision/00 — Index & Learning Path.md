# 🗂️ C# for Unity — Complete Learning Index
### From Zero to Professional Game Developer (2D & 3D Covered Separately)

This folder is your complete C# language reference, written **entirely from a Unity game development perspective**. Every concept has code examples using Unity APIs. Topics are split clearly by **2D** and **3D** where the Unity usage differs.

> [!IMPORTANT]
> **Study Order**: Start with Block 0 if you've never opened Unity before. It explains the engine itself — what GameObjects are, how Rigidbody2D works, what every variable in the Inspector means. Then move through Blocks A–E for C# language mastery.

---

## 📐 How to Use This Vault

| Symbol | Meaning |
| :--- | :--- |
| 🟢 | Beginner — you must know this before writing any Unity script |
| 🟡 | Intermediate — needed for real game features |
| 🔴 | Advanced — needed for professional, optimized code |
| 🎮2D | Concepts with a **2D-specific** Unity section |
| 🎮3D | Concepts with a **3D-specific** Unity section |

---

## 📚 Topic Index — Ordered Learning Path

### 🔶 Block 0 — Unity Engine Fundamentals (Read First — Even Before C#)
> These files explain how Unity itself works. If you don't understand what a GameObject is, or why Rigidbody2D has a `gravityScale` field, start here. No Unity experience assumed.

| # | Topic File | Level | Covers |
| :-- | :--- | :--- | :--- |
| 00A | [[00A — How Unity Works]] | 🟢 | GameObjects, Components, Scenes, Inspector, Project window, Play mode |
| 00B | [[00B — MonoBehaviour & Script Lifecycle]] | 🟢 | How scripts attach to objects, ALL lifecycle methods with timing diagrams |
| 00C | [[00C — Transform Deep Dive]] | 🟢 | position, rotation, scale, parent/child, world vs local space, SetParent |
| 00D | [[00D — Rigidbody2D — Every Variable Explained]] | 🟢 | Mass, drag, gravity, body type, constraints, forces, velocity — all of it |
| 00E | [[00E — Colliders & Triggers 2D]] | 🟢 | BoxCollider2D, CircleCollider2D, layers, physics materials, trigger vs collision |
| 00F | [[00F — Rigidbody3D & Physics3D]] | 🟢 | 3D equivalent — Rigidbody, CharacterController, all 3D physics variables |
| 00G | [[00G — Camera, Rendering & Prefabs]] | 🟢 | Camera setup, Instantiate, Prefabs, scene hierarchy, object lifetime |

---

### 🔷 Block A — Language Foundations (Start Here for C#)

| # | Topic File | Level | Covers |
| :-- | :--- | :--- | :--- |
| 01 | [[01 — Variables, Types & Memory]] | 🟢 | value types, reference types, stack vs heap, nullable types |
| 02 | [[02 — Control Flow & Pattern Matching]] | 🟢 | if/else, switch expressions, pattern matching, ternary, null checks |
| 03 | [[03 — Methods, Parameters & Return Types]] | 🟢 | ref, out, in, params, default values, expression bodies |
| 04 | [[04 — Classes & Structs]] | 🟢 | class vs struct, constructors, static, readonly, partial |
| 05 | [[05 — OOP — Inheritance & Polymorphism]] | 🟡 | base, override, virtual, abstract, sealed, interfaces |
| 06 | [[06 — Properties & Encapsulation]] | 🟢 | get/set, auto-properties, init, expression-bodied, backing fields |
| 07 | [[07 — Generics & Constraints]] | 🟡 | List\<T\>, Dictionary\<K,V\>, where T: constraints, generic methods |
| 08 | [[08 — Collections & LINQ]] | 🟡 | arrays, List, Dictionary, Queue, Stack, LINQ (when to avoid in Unity) |

---

### 🔷 Block B — Unity-Critical C# Features

| # | Topic File | Level | Covers |
| :-- | :--- | :--- | :--- |
| 09 | [[09 — Delegates, Actions & Events]] | 🟡 | delegate, Action, Func, event modifier, UnityEvent, EventBus pattern |
| 10 | [[10 — Interfaces in Unity]] | 🟡 | IDamageable, IInteractable, GetComponent\<Interface\>(), interface patterns |
| 11 | [[11 — Enums & Flags]] | 🟢 | enum, enum with values, [Flags] bitmask, state machines via enum |
| 12 | [[12 — Coroutines & IEnumerator]] | 🟡 | yield return, WaitForSeconds, StartCoroutine, StopCoroutine, pitfalls |
| 13 | [[13 — Async Await & Tasks]] | 🔴 | Task, async/await, CancellationToken, Unity Awaitable (Unity 6) |
| 14 | [[14 — Attributes & Reflection]] | 🟡 | [SerializeField], [Header], [Range], [RequireComponent], custom attributes |

---

### 🔷 Block C — Memory Management & Performance

| # | Topic File | Level | Covers |
| :-- | :--- | :--- | :--- |
| 15 | [[15 — Memory Management & GC]] | 🔴 | Stack, Managed Heap, Native Heap, GC spikes, profiling |
| 16 | [[16 — String Handling & StringBuilder]] | 🟡 | string immutability, interpolation, StringBuilder, TextMeshPro caching |
| 17 | [[17 — Object Pooling in C#]] | 🔴 | Pool\<T\>, custom pool, IObjectPool, pooled bullets/particles |
| 18 | [[18 — Structs for Performance]] | 🔴 | when to use struct, readonly struct, in parameters, struct pitfalls |

---

### 🔷 Block D — Modern C# (Unity 6 / C# 10+)

| # | Topic File | Level | Covers |
| :-- | :--- | :--- | :--- |
| 19 | [[19 — Modern C# Features]] | 🟡 | records, init-only, null-coalescing, tuple deconstruction, using declarations |
| 20 | [[20 — Extension Methods]] | 🟡 | extending Vector3, Transform, GameObject; utility libraries |
| 21 | [[21 — Static Classes & Utility Patterns]] | 🟡 | static helpers, GameManager, ServiceLocator, Constants |
| 22 | [[22 — Operator Overloading]] | 🔴 | custom Vector operations, comparison operators, Unity use cases |

---

### 🔷 Block E — 2D vs 3D Unity-Specific C# Patterns

| # | Topic File | Level | Covers |
| :-- | :--- | :--- | :--- |
| 23 | [[23 — C# Patterns for 2D Unity]] | 🟡 | Vector2, Rigidbody2D, Physics2D, Collider2D, Input in 2D |
| 24 | [[24 — C# Patterns for 3D Unity]] | 🟡 | Vector3, Quaternion, Rigidbody, Physics, NavMesh in 3D |
| 25 | [[25 — Math Utilities in C#]] | 🟡 | Mathf, Vector math, Lerp/Slerp, distance, angles, cross/dot |

---

### 🔶 Block F — Architecture & Best Practices (For Android/App Devs)
> Coming from Android (MVVM/Clean Architecture)? Unity's component system will feel weird. These files bridge the gap between app architecture and game architecture.

| # | Topic File | Level | Covers |
| :-- | :--- | :--- | :--- |
| 26 | [[26 — Unity Folder Structure & Project Organization]] | 🟢 | Standard folder layout, namespaces, assembly definitions (asmdef) |
| 27 | [[27 — Architecture — MVC, MVP & MVVM in Unity]] | 🔴 | How to adapt Clean Architecture to Unity, why strict MVVM is hard, MVP approach |
| 28 | [[28 — The ScriptableObject Architecture]] | 🔴 | Unity's unique data-driven architecture, avoiding singletons, event channels |
| 29 | [[29 — Dependency Injection & Service Locator]] | 🔴 | VContainer/Zenject basics, decoupling systems, Service Locator pattern |
| 30 | [[30 — Clean Code & SOLID in Unity]] | 🔴 | SOLID principles applied to MonoBehaviours, avoiding god classes |

---

### 🔶 Block G — Game Algorithms & DSA (Clash of Clans Architecture)
> You know standard DSA (Arrays, Trees, Graphs), but how are they used in games? This block applies DSA to build complex strategy games like Clash of Clans.

| # | Topic File | Level | Covers |
| :-- | :--- | :--- | :--- |
| 31 | [[31 — Grid Systems & Isometric Math]] | 🟡 | 2D Arrays, building placement, snapping to grids, isometric conversion |
| 32 | [[32 — Pathfinding (A-Star & Flow Fields)]] | 🔴 | Graph Traversal, A* Algorithm for unit routing, Flow Fields for massive swarms |
| 33 | [[33 — Spatial Partitioning (Quadtrees)]] | 🔴 | Quadtrees, Spatial Hashing, O(log n) target acquisition (finding nearest building) |
| 34 | [[34 — AI State Machines (FSM)]] | 🟡 | Finite State Machines (FSM) for troop logic (Search -> Move -> Attack) |
| 35 | [[35 — Offline Timers & Server Auth]] | 🔴 | Unix timestamps, deterministic simulations, anti-cheat, Client-Server model |

---

## 🗺️ Visual Learning Map

```
START HERE (never used Unity?)
    │
    ▼
[Block 0] How Unity Works → Script Lifecycle → Transform → Rigidbody2D → Colliders → Rigidbody3D → Camera/Prefabs
    │
    ▼
[Block A] Variables → Control Flow → Methods → Classes → OOP → Properties → Generics → Collections
    │
    ▼
[Block B] Delegates → Interfaces → Enums → Coroutines → Async → Attributes
    │
    ▼
[Block C] Memory & GC → Strings → Object Pooling → Structs
    │
    ▼
[Block D] Modern C# → Extensions → Static Patterns → Operators
    │
    ▼
[Block E] 🎮2D Patterns  /  🎮3D Patterns  →  Math Utils
    │
    ▼
[Block F] Folder Structure → MVC/MVP/MVVM → ScriptableObjects → DI → SOLID
    │
    ▼
[Block G] Grids → A* Pathfinding → Quadtrees → AI FSM → Multiplayer Backends (CoC Build)
    │
    ▼
WRITE GAMES ✅
```

---

## 🔗 Quick Reference Cheat Sheets

- [[00 — Index & Learning Path#🚨 Unity Gotchas — C# Surprises That Bite Beginners|Unity C# Gotchas]] (below)
- [[Unity Cheat Sheet|Unity API Cheat Sheet]]
- [[C# Revision Guide|Original C# Revision Guide]] (memory & GC deep-dive)

---

## 🚨 Unity Gotchas — C# Surprises That Bite Beginners

These are the most common C# mistakes Unity beginners make. Read this list before starting.

### 1. You Cannot Modify Struct Properties Directly
```csharp
// ❌ WRONG — transform.position returns a copy of Vector3 (it's a struct)
transform.position.x = 5f; // Compile error!

// ✅ CORRECT — copy → modify → re-assign
Vector3 pos = transform.position;
pos.x = 5f;
transform.position = pos;
```

### 2. `null` Check on Destroyed GameObjects Always Fails Weirdly
```csharp
// Unity overrides the == operator, so destroyed objects appear null in ==
if (myObject == null) { }    // ✅ Safe — Unity's overloaded ==
if (myObject is null) { }    // ⚠️ Dangerous — bypasses Unity's override, may not catch destroyed objects
```

### 3. `Start()` and `Awake()` Order is NOT Guaranteed Across Scripts
```csharp
// If ScriptA.Start() reads ScriptB's data, ScriptB.Awake() must set that data
// Use Awake() for self-initialization, Start() for referencing other scripts
void Awake() { health = 100f; }              // initialize self here
void Start()  { ui.SetHealth(health); }     // reference others here
```

### 4. Never Call `GetComponent<T>()` in `Update()`
```csharp
// ❌ BAD — searches the entire component list every frame
void Update() { GetComponent<Rigidbody2D>().AddForce(Vector2.up); }

// ✅ GOOD — cache once in Awake()
private Rigidbody2D rb;
void Awake() { rb = GetComponent<Rigidbody2D>(); }
void Update() { rb.AddForce(Vector2.up); }
```

### 5. `foreach` on Arrays Allocates Memory; `for` Does Not
```csharp
// ❌ foreach on arrays allocates an IEnumerator object (heap)
foreach (var enemy in enemies) { }

// ✅ for loop — pure stack operations, zero allocations
for (int i = 0; i < enemies.Length; i++) { }
```

### 6. `Destroy()` Does Not Happen Immediately
```csharp
Destroy(gameObject);
// gameObject still exists this frame — don't trust it after this line
// Use Destroy(gameObject, 0f) or set a flag like 'isDestroyed'
```

### 7. Coroutines Stop When the GameObject is Disabled
```csharp
// If you disable the GameObject, ALL coroutines on it stop silently
// Use a manager object that is never disabled to run persistent coroutines
```

---

## 📅 Suggested Study Schedule

| Time | Topics | Goal |
| :--- | :--- | :--- |
| **Day 1–2** | **Block 0** (00A–00G) | Understand the engine: GameObjects, components, Rigidbody, lifecycle |
| Week 1 | 01–04 (Block A foundations) | Understand memory model and class vs struct |
| Week 2 | 05–08 (OOP + Collections) | Write proper inheritance and use collections safely |
| Week 3 | 09–12 (Events + Coroutines) | Build decoupled systems with events and timers |
| Week 4 | 13–15 (Async + Memory) | Profile your game and eliminate GC spikes |
| Week 5 | 16–18 (Strings + Pools + Structs) | Write production-quality, zero-allocation code |
| Week 6 | 19–22 (Modern C#) | Write idiomatic, clean modern C# |
| Week 7 | 23–25 (2D & 3D patterns) | Apply everything to your actual game |
| Week 8 | 26–30 (Block F - Architecture) | Structure your game code like a professional app |
| Week 9+ | 31–35 (Block G - Algorithms) | Build Clash of Clans systems (Grids, Pathfinding, State Machines) |

---

*Last updated: Phase 0 — C# Learning Vault — Block G Added*
