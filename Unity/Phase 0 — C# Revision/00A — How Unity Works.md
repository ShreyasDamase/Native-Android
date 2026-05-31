# 00A — How Unity Works
### 🔶 Block 0 — Unity Engine Fundamentals

> [!IMPORTANT]
> **Read this first — even before any C#.** This file explains what Unity actually is, how it's structured, and the mental model you need before any code makes sense. Every single thing you do in Unity goes through the concepts on this page.

---

## 00A.1 — What Is Unity?

Unity is a **game engine**. That means it gives you:

1. A **renderer** — draws pixels to the screen
2. A **physics engine** — simulates gravity, collisions, forces
3. An **audio engine** — plays sounds, 3D spatial audio
4. An **input system** — reads keyboard, mouse, gamepad, touch
5. A **scripting runtime** — runs your C# code every frame
6. An **editor** — the visual tool you open on your computer

You write **C# scripts** that plug into Unity's systems. Unity calls your code at specific moments (called the **lifecycle** — see file `00B`).

```
YOUR C# SCRIPT
      │
      │ Unity calls your methods (Update, Start, etc.)
      ▼
UNITY ENGINE
   ├── Physics    ← Rigidbody, forces, collisions
   ├── Renderer   ← SpriteRenderer, MeshRenderer, Camera
   ├── Audio      ← AudioSource, AudioListener
   ├── Input      ← Input.GetKey(), InputSystem
   └── Scene      ← All GameObjects in the current level
```

---

## 00A.2 — The Editor Windows — What Each One Is

When you open Unity, you see several panels. Here's what every one does:

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────┐  ┌──────────────────────────────────┐  ┌───────────────────┐│
│  │  HIERARCHY  │  │          SCENE VIEW              │  │    INSPECTOR      ││
│  │             │  │                                  │  │                   ││
│  │ ▼ Scene     │  │   [visual 2D/3D viewport]        │  │ GameObject: Player││
│  │   Player    │  │   drag objects here              │  │ ───────────────── ││
│  │   Enemy     │  │   click to select                │  │ ▼ Transform       ││
│  │   ▼ UI      │  │   use tools to move/scale/rotate │  │   Position X 0    ││
│  │     Canvas  │  │                                  │  │   Position Y 0    ││
│  │     Button  │  │                                  │  │ ▼ Rigidbody2D     ││
│  │   Camera    │  │                                  │  │   Mass: 1         ││
│  └─────────────┘  └──────────────────────────────────┘  │   Gravity: 1      ││
│                                                          └───────────────────┘│
│  ┌─────────────────────────────────────┐  ┌──────────────────────────────────┐│
│  │           PROJECT WINDOW            │  │         GAME VIEW                ││
│  │                                     │  │                                  ││
│  │  Assets/                            │  │  [what the player SEES]          ││
│  │    Scripts/                         │  │  Press Play to test here         ││
│  │      PlayerController.cs            │  │                                  ││
│  │    Sprites/                         │  │                                  ││
│  │    Prefabs/                         │  │                                  ││
│  └─────────────────────────────────────┘  └──────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────┘
```

| Window | Purpose |
| :--- | :--- |
| **Hierarchy** | List of all objects in the current scene. Like a folder tree |
| **Scene View** | Visual editor — drag, rotate, place objects |
| **Game View** | What the camera sees — what the player will see |
| **Inspector** | Shows all properties of the selected object. Where you tweak values |
| **Project** | All your files — scripts, images, sounds, prefabs |
| **Console** | Shows `Debug.Log()` output, warnings, and errors |

---

## 00A.3 — GameObjects — The Fundamental Building Block

**Everything in Unity is a GameObject.** The player, the enemy, the camera, the background, the UI button, the invisible trigger zone — all GameObjects.

A **GameObject** by itself is just a container. It has:
- A **name** (e.g., "Player")
- A **tag** (e.g., "Player" or "Enemy")
- A **layer** (controls what it collides with and what the camera sees)
- A list of **Components** (the actual behavior and data)

```
GameObject: "Player"
│
├── Transform        ← ALWAYS present. Controls position, rotation, scale
├── SpriteRenderer   ← Draws the player's sprite/image
├── Rigidbody2D      ← Makes physics work (gravity, forces, collisions)
├── BoxCollider2D    ← Defines the shape for collision detection
└── PlayerController ← YOUR C# SCRIPT — the custom behavior you write
```

Think of a GameObject like an **empty box**. Components are the things you put inside the box to give it meaning.

---

## 00A.4 — Components — What They Are and How They Work

A **Component** is a piece of functionality attached to a GameObject. Unity provides dozens of built-in components. You write custom components (called **MonoBehaviours**) in C#.

```csharp
// Your script IS a Component — it inherits from MonoBehaviour
public class PlayerController : MonoBehaviour   // ← This IS a Component
{
    // Fields here appear in the Inspector
    [SerializeField] private float speed = 5f;
    
    // Unity calls this every frame
    void Update()
    {
        // Move the player
    }
}
```

**How to add a Component:**
1. Click a GameObject in the Hierarchy
2. In the Inspector → click **Add Component**
3. Search and add (e.g., "Rigidbody 2D")

**From code:**
```csharp
// Add a component at runtime
Rigidbody2D rb = gameObject.AddComponent<Rigidbody2D>();

// Get a component that's already on the GameObject
Rigidbody2D rb = GetComponent<Rigidbody2D>();

// Get a component from a different GameObject
Rigidbody2D otherRb = otherObject.GetComponent<Rigidbody2D>();

// Safe version — returns true/false instead of crashing
if (TryGetComponent<Rigidbody2D>(out Rigidbody2D rb))
{
    rb.AddForce(Vector2.up * 5f);
}
```

---

## 00A.5 — The Scene — Your Game World

A **Scene** is one "level" or "screen" in your game. Think of it like a room that holds GameObjects.

```
Scene: "Level_01"
├── Environment
│   ├── Ground (Tilemap)
│   ├── Background
│   └── Platforms
├── Characters
│   ├── Player
│   └── Enemy_01, Enemy_02
├── UI
│   ├── Canvas
│   │   ├── HealthBar
│   │   ├── ScoreText
│   │   └── PauseMenu
└── Systems
    ├── GameManager
    ├── AudioManager
    └── Main Camera
```

**Loading Scenes in code:**
```csharp
using UnityEngine.SceneManagement;

// Load a scene by name
SceneManager.LoadScene("Level_02");

// Load a scene by index (order in Build Settings)
SceneManager.LoadScene(1);

// Load additively (don't unload current scene — useful for UI scenes)
SceneManager.LoadScene("UIOverlay", LoadSceneMode.Additive);

// Load asynchronously (no freeze/stutter)
IEnumerator LoadSceneAsync(string sceneName)
{
    AsyncOperation op = SceneManager.LoadSceneAsync(sceneName);
    while (!op.isDone)
    {
        float progress = op.progress; // 0.0 to 0.9 (0.9 = 90% = loading done)
        loadingBar.value = progress;
        yield return null;
    }
}

// Get current scene name
string currentScene = SceneManager.GetActiveScene().name;
```

---

## 00A.6 — The Inspector — Reading and Setting Values

The **Inspector** shows every public field and `[SerializeField]` field of every component on the selected GameObject. This is where you **configure** your GameObjects without writing code for every value.

```csharp
public class EnemyConfig : MonoBehaviour
{
    // PUBLIC — visible in Inspector (avoid — breaks encapsulation)
    public float maxHealth = 100f;
    
    // [SerializeField] PRIVATE — visible in Inspector, private in code (BEST PRACTICE)
    [SerializeField] private float moveSpeed = 3f;
    [SerializeField] private float attackRange = 1.5f;
    [SerializeField] private int scoreValue = 50;
    
    // Reference to other GameObjects or components — drag & drop in Inspector
    [SerializeField] private Transform spawnPoint;
    [SerializeField] private GameObject bulletPrefab;
    [SerializeField] private AudioClip deathSound;
    [SerializeField] private SpriteRenderer spriteRenderer;
    
    // Arrays and Lists — expandable in Inspector
    [SerializeField] private Transform[] patrolPoints;
    [SerializeField] private List<AudioClip> footstepSounds;
    
    // PRIVATE, no attribute — HIDDEN from Inspector (internal state)
    private float currentHealth;
    private bool isAlive = true;
}
```

**How the Inspector workflow works:**
1. You write the script with `[SerializeField]` fields
2. Unity compiles it — the Inspector shows the new fields
3. You set the values in the Inspector (drag GameObjects, type numbers, etc.)
4. When the game runs, those values are what your script gets
5. **You never need to hardcode "Player" string references** — just drag the object

---

## 00A.7 — Play Mode vs Edit Mode

Unity has two modes:

| Mode | What Happens |
| :--- | :--- |
| **Edit Mode** | You design the game. Move objects, adjust values, write scripts |
| **Play Mode** | The game runs. Your scripts execute. Physics simulates |

> [!WARNING]
> **Changes made in Play Mode are LOST when you stop playing.** If you change a value in the Inspector during Play Mode to test something, it resets when you stop. To keep changes, stop Play Mode first, then change the values.

```csharp
// Check which mode you're in (useful for editor-only code)
#if UNITY_EDITOR
using UnityEditor;

bool isPlaying = EditorApplication.isPlaying;
#endif
```

---

## 00A.8 — How Scripts Talk to Each Other

This is the most important practical concept. Scripts don't live in isolation — they need to talk to each other constantly.

```csharp
// METHOD 1: Direct reference (most common — drag in Inspector)
public class Gun : MonoBehaviour
{
    [SerializeField] private PlayerController player;  // drag player in Inspector
    
    void Fire()
    {
        // Access player's data directly via the reference
        if (player.CurrentAmmo > 0)
            SpawnBullet();
    }
}

// METHOD 2: GetComponent (find on same or other GameObjects)
public class HealthSystem : MonoBehaviour
{
    private PlayerController playerController;
    private Animator animator;
    
    void Awake()
    {
        // Get components ON THIS SAME GameObject
        playerController = GetComponent<PlayerController>();
        animator = GetComponent<Animator>();
    }
    
    // Get component from a DIFFERENT object (from a collision, etc.)
    void OnTriggerEnter2D(Collider2D other)
    {
        // Find the Enemy script on the other object
        if (other.TryGetComponent<Enemy>(out Enemy enemy))
        {
            TakeDamage(enemy.damage);
        }
    }
}

// METHOD 3: Find in scene (expensive — avoid in Update)
void Start()
{
    // Find by type — looks through ENTIRE scene
    GameManager gm = FindFirstObjectByType<GameManager>(); // Unity 2023+
    
    // Find by name — fragile (name changes break it)
    GameObject player = GameObject.Find("Player"); // avoid if possible
    
    // Find by tag — slightly better
    GameObject playerByTag = GameObject.FindWithTag("Player");
}

// METHOD 4: Static/Singleton (for managers — see file 21)
GameManager.Instance.AddScore(100); // access from anywhere
```

---

## 00A.9 — Tags and Layers

**Tags** are string labels on GameObjects used to identify them in code.
**Layers** are numeric categories used for physics filtering and camera culling.

```csharp
// Setting tags in Inspector: click the "Tag" dropdown at the top of Inspector
// or right-click empty space in Hierarchy → Create Empty → then set tag

// Checking tags (ALWAYS use CompareTag, never == for tags)
void OnCollisionEnter2D(Collision2D other)
{
    // ✅ CORRECT — CompareTag is fast, no string allocation
    if (other.gameObject.CompareTag("Ground"))
    {
        isGrounded = true;
    }
    
    if (other.gameObject.CompareTag("Enemy"))
    {
        TakeDamage(10f);
    }
    
    // ❌ WRONG — allocates a new string every call
    if (other.gameObject.tag == "Enemy") { }
}

// Layers — used for physics and cameras
// Set in Inspector: click "Layer" dropdown at top of Inspector
// Also set in: Edit → Project Settings → Physics 2D → Layer Collision Matrix

// In code — check layer
bool isGroundLayer = other.gameObject.layer == LayerMask.NameToLayer("Ground");

// LayerMask — pass to Physics queries to filter collisions
[SerializeField] private LayerMask groundLayer;
[SerializeField] private LayerMask enemyLayer;

bool hit = Physics2D.Raycast(pos, Vector2.down, 1f, groundLayer);
// Only hits objects on the Ground layer — ignores everything else
```

---

## 00A.10 — The Asset Pipeline (Project Window)

Everything in the **Project window** is an **Asset**. Assets live in the `Assets/` folder of your project.

```
Assets/
├── Scenes/               ← Your .unity scene files
├── Scripts/              ← All .cs files
│   ├── Player/
│   ├── Enemy/
│   └── UI/
├── Sprites/              ← Images (.png, .jpg, .psd)
├── Audio/                ← Sound files (.mp3, .wav, .ogg)
├── Prefabs/              ← Reusable pre-configured GameObjects (.prefab)
├── Animations/           ← Animation clips (.anim), controllers (.controller)
├── Materials/            ← Visual materials (.mat)
└── Fonts/                ← Text fonts
```

**Key asset types:**
| Asset Type | Extension | Purpose |
| :--- | :--- | :--- |
| Scene | `.unity` | A level/screen |
| Script | `.cs` | C# code |
| Prefab | `.prefab` | A reusable, pre-configured GameObject |
| Sprite | `.png` | 2D image |
| AnimationClip | `.anim` | One animation (walk, jump, etc.) |
| AnimatorController | `.controller` | State machine connecting animations |
| AudioClip | `.mp3`/`.wav` | Sound effect or music |
| Material | `.mat` | Visual shader/texture settings |
| ScriptableObject | `.asset` | Data container (see Block B) |

---

## 📝 Summary — The Unity Mental Model

```
SCENE
  └── contains GameObjects
        └── each has Components
              ├── Transform (always) → position/rotation/scale
              ├── Renderer → makes it visible
              ├── Collider → makes it physical
              ├── Rigidbody → makes it move with physics
              └── YOUR SCRIPT → the custom behavior

You write C# scripts that:
  1. Inherit from MonoBehaviour (to become a Component)
  2. Use [SerializeField] to expose data to the Inspector
  3. Override lifecycle methods (Awake, Start, Update) to run code
  4. Use GetComponent<T>() to talk to other Components
  5. Use Tags and Layers to categorize and filter objects
```

**Next:** [[00B — MonoBehaviour & Script Lifecycle]]
