# 00G — Camera, Rendering & Prefabs
### 🔶 Block 0 — Unity Engine Fundamentals

> [!NOTE]
> This file covers the three remaining engine fundamentals: **Camera** (what the player sees), **Rendering** (how things look), and **Prefabs** (reusable objects). These are things you'll interact with in literally every Unity project.

---

## 00G.1 — The Camera Component

The **Camera** component is what renders the scene to the screen. Without a Camera, the player sees nothing. Unity scenes always need at least one Camera.

### Camera Inspector Fields (2D)

```
Camera
├── Clear Flags      [Solid Color ▼]    ← What to show where nothing is drawn
├── Background       [■ Black]           ← Background color (when Clear Flags=Solid Color)
├── Culling Mask     [Everything ▼]     ← Which layers this camera renders
├── Projection       [Orthographic ▼]   ← Orthographic=2D flat, Perspective=3D depth
├── Size             [5]                 ← Orthographic size (half the height in world units)
├── Near Clip Plane  [0.3]              ← Objects closer than this aren't rendered
├── Far Clip Plane   [1000]             ← Objects farther than this aren't rendered
├── Viewport Rect    X[0] Y[0] W[1] H[1] ← Which part of the screen this camera fills
└── Depth            [-1]               ← Render order (higher = rendered on top)
```

### Camera Inspector Fields (3D)

```
Camera (Perspective projection)
├── Clear Flags      [Skybox ▼]         ← Renders skybox where nothing is drawn
├── Culling Mask     [Everything ▼]
├── Projection       [Perspective ▼]
├── Field of View    [60]               ← Horizontal angle of view (60-90 is typical)
├── Physical Camera  [□]               ← Enable for real camera lens simulation
├── Near Clip Plane  [0.3]
└── Far Clip Plane   [1000]
```

---

## 00G.2 — Orthographic vs Perspective

```
ORTHOGRAPHIC (2D games):            PERSPECTIVE (3D games):
────────────────────────────        ────────────────────────────
Objects don't get smaller           Objects get smaller with distance
No depth/distance illusion          Realistic depth perception
Used for: 2D platformers,          Used for: 3D games, FPS, TPS,
          top-down RPGs,                     3D puzzles
          strategy games

         Camera                              Camera
          │                                  /|\
          ▼                                 / | \
    ┌──────────┐                           /  |  \
    │  object  │ ← same size              /   │   \
    └──────────┘   regardless           far objects appear smaller
    any distance

"Size" field controls zoom          "Field of View" controls zoom
Higher size = more visible area     Higher FOV = wider, fisheye look
```

```csharp
// Access the main camera (the one with "MainCamera" tag)
Camera mainCam = Camera.main;

// Properties
float orthoSize = mainCam.orthographicSize; // 2D zoom level
float fov = mainCam.fieldOfView;            // 3D field of view
bool isOrtho = mainCam.orthographic;        // true if orthographic

// Change projection at runtime
mainCam.orthographic = true;
mainCam.orthographicSize = 5f;  // 2D — shows 5 units above center

mainCam.orthographic = false;
mainCam.fieldOfView = 70f;      // 3D — 70 degree FOV

// CONVERTING POSITIONS
// Screen space: pixel coordinates (0,0 = bottom-left, Screen.width/height = top-right)
// World space: actual game world coordinates

// Screen point → World point (for mouse position in 2D)
Vector3 mouseScreen = Input.mousePosition;  // pixel position
Vector3 mouseWorld = mainCam.ScreenToWorldPoint(mouseScreen); // world position

// For 2D, you often cast to Vector2 and ignore Z:
Vector2 mouseWorld2D = mainCam.ScreenToWorldPoint(Input.mousePosition);

// World point → Screen point (for UI overlays over 3D objects)
Vector3 screenPos = mainCam.WorldToScreenPoint(enemy.transform.position);
// Now screenPos.x, screenPos.y are pixel coords for where enemy appears on screen

// Screen point → Ray (for 3D mouse raycasting)
Ray ray = mainCam.ScreenPointToRay(Input.mousePosition);
if (Physics.Raycast(ray, out RaycastHit hit, 100f))
{
    Debug.Log($"Clicked on: {hit.collider.name}");
}
```

---

## 00G.3 — Camera.main — How It Works

`Camera.main` finds the first Camera in the scene tagged **"MainCamera"**.

```csharp
// ❌ SLOW — Camera.main does a FindObjectWithTag search every call!
void Update()
{
    Vector3 pos = Camera.main.ScreenToWorldPoint(Input.mousePosition); // slow!
}

// ✅ FAST — cache it once in Awake
private Camera cam;
void Awake() => cam = Camera.main;

void Update()
{
    Vector3 pos = cam.ScreenToWorldPoint(Input.mousePosition); // fast!
}
```

---

## 00G.4 — Culling Mask — What the Camera Sees

The **Culling Mask** determines which layers the camera renders. You can have multiple cameras rendering different layers (e.g., one for game world, one for UI).

```csharp
// Camera renders only the "UI" layer (for a dedicated UI camera)
Camera uiCamera = GetComponent<Camera>();
uiCamera.cullingMask = LayerMask.GetMask("UI");

// Camera renders everything EXCEPT the UI layer
uiCamera.cullingMask = ~LayerMask.GetMask("UI"); // ~ = bitwise NOT

// Multiple layers
uiCamera.cullingMask = LayerMask.GetMask("Default", "Enemy", "Player");
```

---

## 00G.5 — Multiple Cameras and Depth

You can have multiple cameras. `Depth` controls render order — higher depth = rendered on top (like layers in Photoshop).

```csharp
// Common setup:
// Camera 1: depth=-1, cullingMask=game world → renders game
// Camera 2: depth=0, cullingMask=UI, Clear Flags=Don't Clear → overlays UI on top

// This is the old approach — Unity now recommends using a single camera with Canvas in World Space
// or using the Universal Render Pipeline (URP) camera stacking

Camera worldCamera = GameObject.Find("WorldCamera").GetComponent<Camera>();
Camera uiCamera = GameObject.Find("UICamera").GetComponent<Camera>();

worldCamera.depth = -1;                    // renders first
uiCamera.depth = 0;                        // renders on top
uiCamera.clearFlags = CameraClearFlags.Depth; // don't clear color, just depth
```

---

## 00G.6 — Prefabs — Reusable GameObjects

A **Prefab** is a saved "template" of a GameObject with all its components, settings, and children. When you need many copies of the same thing (bullets, enemies, coins, effects), you use a Prefab.

```
Think of a Prefab as a BLUEPRINT and instances as BUILDINGS made from it.

Prefab (in Project window):     Instance (in Scene/Hierarchy):
   Enemy.prefab                     Enemy (Clone)
   ├── SpriteRenderer               ├── SpriteRenderer     ← linked to prefab
   ├── Rigidbody2D                  ├── Rigidbody2D
   ├── BoxCollider2D                ├── BoxCollider2D
   └── EnemyAI                      └── EnemyAI

Changes to the Prefab → automatically update ALL instances!
You can override individual instance values without affecting the prefab.
```

### Creating Prefabs

```
1. Set up a GameObject in the scene exactly as you want
2. Drag it from the Hierarchy → to the Project window (Assets/Prefabs/)
3. The GameObject in your scene is now an INSTANCE of that prefab
4. Delete the scene instance — you have your prefab template
```

### Using Prefabs in Code

```csharp
public class EnemySpawner : MonoBehaviour
{
    // Drag the prefab here in the Inspector
    [SerializeField] private GameObject enemyPrefab;
    [SerializeField] private Transform[] spawnPoints;
    
    void SpawnEnemy()
    {
        // Instantiate — creates a new instance from the prefab
        Vector3 spawnPos = spawnPoints[Random.Range(0, spawnPoints.Length)].position;
        Quaternion spawnRot = Quaternion.identity; // no rotation
        
        // Signature: Instantiate(prefab, position, rotation)
        GameObject newEnemy = Instantiate(enemyPrefab, spawnPos, spawnRot);
        newEnemy.name = "Enemy_" + Time.time; // optional: rename the instance
        
        // Instantiate with parent:
        GameObject enemyAsChild = Instantiate(enemyPrefab, spawnPos, spawnRot, transform);
        // The new enemy is now a child of this spawner GameObject
    }
    
    void SpawnWithSetup()
    {
        GameObject enemy = Instantiate(enemyPrefab, Vector3.zero, Quaternion.identity);
        
        // Configure the instance AFTER spawning — access its components
        Enemy enemyScript = enemy.GetComponent<Enemy>();
        enemyScript.SetDifficulty(currentWave);
        
        Rigidbody2D rb = enemy.GetComponent<Rigidbody2D>();
        rb.AddForce(Vector2.right * 5f, ForceMode2D.Impulse);
    }
    
    // Typed Instantiate (Unity returns the correct type directly)
    void SpawnTyped()
    {
        // If your prefab has an Enemy component at the root:
        Enemy enemyInstance = Instantiate(enemyPrefab, Vector3.zero, Quaternion.identity)
                                .GetComponent<Enemy>();
        
        // Or keep a typed prefab reference:
        // [SerializeField] private Enemy typedEnemyPrefab;
        // Enemy inst = Instantiate(typedEnemyPrefab, pos, rot);
    }
}
```

---

## 00G.7 — Destroying Objects

```csharp
// Destroy a GameObject
Destroy(gameObject);           // destroys THIS object at end of current frame
Destroy(otherObject);          // destroys another object
Destroy(gameObject, 2f);       // destroys after 2 second delay
Destroy(GetComponent<Script>()); // removes just a component, not the whole object

// IMPORTANT: Destroy is NOT instant!
// The object still exists until end of frame — don't use it after calling Destroy
Destroy(enemy.gameObject);
Debug.Log(enemy.Health); // WORKS this frame — enemy still exists
// End of frame: enemy is actually destroyed

// DestroyImmediate — instant destruction (ONLY use in Editor scripts!)
DestroyImmediate(gameObject); // NEVER in game code — can break physics

// DontDestroyOnLoad — survive scene transitions
DontDestroyOnLoad(gameObject); // this object persists when a new scene loads
// Used for: GameManager, AudioManager, persistent data

// SetActive — hide/disable without destroying
gameObject.SetActive(false); // hidden, all components disabled, coroutines stop
gameObject.SetActive(true);  // shown, components re-enable, OnEnable fires again
```

---

## 00G.8 — SpriteRenderer — Making Things Visible in 2D

The **SpriteRenderer** component draws a sprite (2D image) on screen.

```
SpriteRenderer
├── Sprite          [None ▼]     ← The image file to display
├── Color           [■ White]    ← Tint color (white = no tint)
├── Flip            [□ X] [□ Y]  ← Mirror the sprite
├── Material        [Sprites-Default] ← Shader (usually leave default)
├── Order in Layer  [0]          ← Draw order within same Sorting Layer (higher = on top)
└── Sorting Layer   [Default ▼]  ← Broad draw category (Background, Default, Foreground)
```

```csharp
SpriteRenderer sr = GetComponent<SpriteRenderer>();

// Change the sprite image at runtime
sr.sprite = newSprite;

// Change color / transparency
sr.color = Color.red;                  // make red
sr.color = new Color(1f, 1f, 1f, 0.5f); // 50% transparent
sr.color = Color.white;                // reset to normal

// Fade out
sr.color = new Color(sr.color.r, sr.color.g, sr.color.b, 0.5f); // just change alpha

// Flip
sr.flipX = true;  // mirror horizontally (face left)
sr.flipX = false; // face right

// Draw order — higher sortingOrder = drawn on top
sr.sortingOrder = 5;          // drawn above 0-4
sr.sortingLayerName = "Player"; // assign to a different sorting layer

// Enable/disable visibility
sr.enabled = false; // invisible (still exists, collision still works!)
sr.enabled = true;
```

---

## 00G.9 — Common Rendering Components

| Component | Purpose |
| :--- | :--- |
| `SpriteRenderer` | Draw 2D images (sprites) |
| `MeshRenderer` | Draw 3D meshes |
| `SkinnedMeshRenderer` | Draw animated 3D character meshes |
| `LineRenderer` | Draw lines (lasers, paths, ropes) |
| `TrailRenderer` | Draw a trail behind a moving object |
| `ParticleSystem` | Emit particles (fire, sparks, dust) |
| `Canvas + UI` | Draw UI elements (buttons, text, images) |

```csharp
// Line Renderer — draw a laser between two points
LineRenderer lr = GetComponent<LineRenderer>();
lr.positionCount = 2;              // two points = one line
lr.SetPosition(0, startPoint);    // start
lr.SetPosition(1, endPoint);      // end
lr.startWidth = 0.05f;            // thin laser
lr.endWidth = 0.05f;
lr.material.color = Color.red;

// Trail Renderer — leave a trail behind moving objects
TrailRenderer tr = GetComponent<TrailRenderer>();
tr.time = 0.5f;      // how long the trail lasts
tr.startWidth = 0.2f;
tr.endWidth = 0f;    // tapers to nothing
```

---

## 00G.10 — The Complete Scene Setup Checklist

When starting any new Unity project from scratch:

```
□ 1. Create scene structure in Hierarchy:
      Environment/ → visual/collision objects
      Characters/  → player, enemies
      UI/          → Canvas with UI elements
      Systems/     → GameManager, AudioManager (empty GameObjects with scripts)
      Camera/      → Main Camera

□ 2. Layer setup (Edit → Project Settings → Tags and Layers):
      Layer 6: Player
      Layer 7: Enemy
      Layer 8: Ground
      Layer 9: Bullet
      Layer 10: Trigger

□ 3. Physics Matrix (Project Settings → Physics 2D):
      Configure which layers collide with which

□ 4. Camera settings:
      2D: Orthographic, Size=5, Clear=Solid Color
      3D: Perspective, FOV=60, Clear=Skybox

□ 5. First script structure:
      GameManager.cs (singleton, scene management, score)
      PlayerController.cs (input, movement)
      PlayerHealth.cs (damage, death)

□ 6. Prefabs folder in Assets:
      Assets/Prefabs/Characters/
      Assets/Prefabs/Projectiles/
      Assets/Prefabs/VFX/
      Assets/Prefabs/UI/
```

---

## 📝 Summary — Quick Reference

### Camera
| Property/Method | What It Does |
| :--- | :--- |
| `Camera.main` | Get the main camera (cache it!) |
| `orthographic` | true=2D flat, false=3D perspective |
| `orthographicSize` | 2D zoom (half height in world units) |
| `fieldOfView` | 3D zoom (degrees, 60 is typical) |
| `ScreenToWorldPoint(v)` | Convert screen pixels → world coords |
| `WorldToScreenPoint(v)` | Convert world coords → screen pixels |
| `ScreenPointToRay(v)` | Get a ray from screen point into world |

### Prefabs
| Action | Code |
| :--- | :--- |
| Spawn a prefab | `Instantiate(prefab, pos, rot)` |
| Spawn with parent | `Instantiate(prefab, pos, rot, parent)` |
| Remove object | `Destroy(gameObject)` |
| Remove after delay | `Destroy(gameObject, 2f)` |
| Persist scene loads | `DontDestroyOnLoad(gameObject)` |

### SpriteRenderer (2D)
| Property | What It Does |
| :--- | :--- |
| `sprite` | The displayed image |
| `color` | Tint/transparency (alpha) |
| `flipX/flipY` | Mirror the sprite |
| `sortingOrder` | Draw order (higher = on top) |
| `enabled` | Show/hide (collision still active) |

**Previous:** [[00F — Rigidbody3D & Physics3D]] | **Next:** [[01 — Variables, Types & Memory]]

---

*Block 0 complete. You now understand how Unity works as an engine. Start [[01 — Variables, Types & Memory]] to learn the C# language that powers all your scripts.*
