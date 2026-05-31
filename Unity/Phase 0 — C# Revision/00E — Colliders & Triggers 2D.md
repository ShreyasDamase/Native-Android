# 00E — Colliders & Triggers 2D
### 🔶 Block 0 — Unity Engine Fundamentals

> [!NOTE]
> A **Rigidbody2D** alone doesn't make things collide — it only adds gravity and forces. You need a **Collider2D** to define the SHAPE of an object for collision detection. This file covers all 2D collider types, triggers, layers, and physics materials.

---

## 00E.1 — What is a Collider?

A **Collider** defines the **physical shape** of a GameObject for the physics engine. It's invisible in the game — it's just math telling Unity "this object occupies this space."

```
COLLIDER = The Shape     RIGIDBODY = The Physics Behavior
    ↓                           ↓
"I am a box, 1×1 units"   "I have mass, gravity, velocity"
```

- Rigidbody WITHOUT a Collider → object falls through everything (no shape)
- Collider WITHOUT a Rigidbody → static obstacle (wall, ground) — nothing more
- Collider + Rigidbody → fully physical object (player, enemy, box)

---

## 00E.2 — The 2D Collider Types

### `BoxCollider2D` — Rectangle Shape
The most common. Use for: players, enemies, platforms, crates, boxes.

```
Inspector:
BoxCollider2D
├── Material         [None]   ← Physics material (friction/bounciness)
├── Is Trigger       [□]      ← If checked, becomes a trigger zone (no physical collision)
├── Used By Effector [□]      ← For special physics effectors (platform effectors)
├── Offset           X [0] Y [0]   ← Move the collider center without moving the sprite
├── Size             X [1] Y [1]   ← Width and Height in units
└── Edge Radius      [0]           ← Round the corners (0 = sharp, 0.1 = slightly rounded)
```

```csharp
BoxCollider2D box = GetComponent<BoxCollider2D>();

// Read properties
Vector2 size = box.size;     // width and height
Vector2 offset = box.offset; // center offset from transform.position

// Modify properties
box.size = new Vector2(1.5f, 2f);      // resize
box.offset = new Vector2(0f, 0.5f);   // shift collider up (e.g., for head detection)
box.isTrigger = false;                 // solid collision

// Check if a point is inside the collider
bool isInside = box.OverlapPoint(somePoint);
```

### `CircleCollider2D` — Circle Shape
Use for: balls, coins, round enemies, projectiles, pickup ranges.

```
Inspector:
CircleCollider2D
├── Material         [None]
├── Is Trigger       [□]
├── Offset           X [0] Y [0]
└── Radius           [0.5]   ← Half the width in units
```

```csharp
CircleCollider2D circle = GetComponent<CircleCollider2D>();
circle.radius = 0.5f;
circle.offset = Vector2.zero;
```

### `CapsuleCollider2D` — Capsule Shape
Use for: human-shaped characters, vertical/horizontal pills. Best for players — avoids corner catching.

```
Inspector:
CapsuleCollider2D
├── Material         [None]
├── Is Trigger       [□]
├── Offset           X [0] Y [0]
├── Size             X [0.5] Y [1]   ← Width and Height
└── Direction        [Vertical ▼]    ← Vertical = standing capsule
```

```csharp
CapsuleCollider2D capsule = GetComponent<CapsuleCollider2D>();
capsule.size = new Vector2(0.5f, 1.8f); // slim player height
capsule.direction = CapsuleDirection2D.Vertical;
```

### `PolygonCollider2D` — Custom Shape
Use for: irregular shapes, drawn sprites, complex terrain chunks.

```csharp
// Usually auto-generated from sprite shape in Inspector
// Can edit points manually in Scene view with Edit Collider button
PolygonCollider2D poly = GetComponent<PolygonCollider2D>();
Vector2[] points = poly.GetPath(0); // get the polygon vertices
```

### `EdgeCollider2D` — Line Shape
Use for: terrain outlines, lines, ropes (one-sided — only detects from one direction).

```csharp
EdgeCollider2D edge = GetComponent<EdgeCollider2D>();
edge.SetPoints(new Vector2[]
{
    new Vector2(-5f, 0f),
    new Vector2(0f, 1f),
    new Vector2(5f, 0f)
}); // creates a line path
```

### `CompositeCollider2D` — Combined Shape
Merges multiple Tilemap colliders or Box/Polygon colliders into one efficient shape. Used automatically with Tilemaps.

---

## 00E.3 — Trigger vs Collision — Deep Explanation

This is the most important distinction in 2D physics.

```
COLLISION (Is Trigger = OFF):              TRIGGER (Is Trigger = ON):
────────────────────────────               ────────────────────────────
Physical solid object.                     Ghost zone — overlap only.
Objects CANNOT pass through.               Objects CAN pass through.
Creates physical contact forces.           No physical forces.
Callbacks: OnCollisionEnter/Stay/Exit2D    Callbacks: OnTriggerEnter/Stay/Exit2D
Needs Rigidbody to receive callbacks.      Any collider can be a trigger.

USE FOR:                                   USE FOR:
  Ground, walls, platforms                   Damage zones, lava
  Bullet impacts on walls                    Pickup areas (coins, power-ups)
  Player landing                             Detection zones (enemy sight)
  Physical crates                            Level completion zones
                                             Checkpoint triggers
                                             Area-of-effect (bomb blast radius)
```

```csharp
public class DamageZone : MonoBehaviour
{
    [SerializeField] private float damagePerSecond = 10f;
    
    // This object has a Collider2D with IsTrigger = TRUE
    // → objects pass through but OnTrigger events fire
    
    void OnTriggerEnter2D(Collider2D other)
    {
        // Called ONCE when something enters this zone
        Debug.Log($"{other.name} entered lava!");
        
        if (other.TryGetComponent<PlayerHealth>(out PlayerHealth health))
        {
            health.SetInLava(true);
        }
    }
    
    void OnTriggerStay2D(Collider2D other)
    {
        // Called EVERY PHYSICS STEP while something is inside
        if (other.TryGetComponent<PlayerHealth>(out PlayerHealth health))
        {
            health.TakeDamage(damagePerSecond * Time.fixedDeltaTime);
        }
    }
    
    void OnTriggerExit2D(Collider2D other)
    {
        // Called ONCE when something leaves
        if (other.TryGetComponent<PlayerHealth>(out PlayerHealth health))
        {
            health.SetInLava(false);
        }
    }
}

public class Wall : MonoBehaviour
{
    // This object has a Collider2D with IsTrigger = FALSE
    // → physical solid object
    
    void OnCollisionEnter2D(Collision2D collision)
    {
        // 'collision' has more info than 'other' in triggers:
        float impactSpeed = collision.relativeVelocity.magnitude;
        Vector2 contactPoint = collision.GetContact(0).point;
        Vector2 contactNormal = collision.GetContact(0).normal;
        
        Debug.Log($"Hit at speed: {impactSpeed}");
    }
}
```

---

## 00E.4 — Layers and Layer Masks — Controlling What Collides

**Layers** are numbered categories (0–31) assigned to GameObjects. The **Layer Collision Matrix** in Project Settings controls which layers can collide with each other.

```
Project Settings → Physics 2D → Layer Collision Matrix

             | Default | Player | Enemy | Ground | Bullet | Trigger |
Default      |   ✓     |   ✓    |  ✓   |   ✓    |   ✓   |         |
Player       |   ✓     |        |  ✓   |   ✓    |        |   ✓    |
Enemy        |   ✓     |   ✓    |      |   ✓    |   ✓   |         |
Ground       |   ✓     |   ✓    |  ✓   |        |        |         |
Bullet       |   ✓     |        |  ✓   |   ✓    |        |         |
Trigger      |         |   ✓    |      |        |        |         |

✓ = these layers detect collision with each other
Empty = they ignore each other (no collision callbacks, no physics)
```

**Setting layers in Inspector:** Top of Inspector → "Layer" dropdown → select or create layer.

```csharp
// Check an object's layer
int layer = gameObject.layer;
string layerName = LayerMask.LayerToName(layer);

// Set an object's layer
gameObject.layer = LayerMask.NameToLayer("Enemy");
gameObject.layer = 9; // by number (avoid — use names)

// LayerMask for physics queries
[SerializeField] private LayerMask groundLayer;     // set in Inspector
[SerializeField] private LayerMask enemyLayer;

// Use in raycast/overlap to filter what you detect
bool isGrounded = Physics2D.OverlapCircle(groundCheck.position, 0.15f, groundLayer);
// Only detects overlaps with objects on the "Ground" layer

// Build a LayerMask in code
LayerMask mask = LayerMask.GetMask("Ground", "Platform"); // combine multiple layers
LayerMask groundOnly = 1 << LayerMask.NameToLayer("Ground"); // bit shift method

// Check if an object is on a specific layer
bool isOnGroundLayer = ((groundLayer.value & (1 << gameObject.layer)) != 0);
```

---

## 00E.5 — Physics Queries (How to Ask "What's Here?")

These let you detect objects in the world without waiting for collisions.

```csharp
public class PhysicsQueries : MonoBehaviour
{
    [SerializeField] private LayerMask groundLayer;
    [SerializeField] private LayerMask enemyLayer;
    
    private Collider2D[] overlapBuffer = new Collider2D[16]; // pre-allocated
    private RaycastHit2D[] rayBuffer = new RaycastHit2D[8];
    
    void QueryExamples()
    {
        // ===== POINT QUERIES =====
        // Is there anything at exactly this point?
        Collider2D hit = Physics2D.OverlapPoint(transform.position);
        
        // Is there anything at this point on specific layers?
        Collider2D groundHit = Physics2D.OverlapPoint(transform.position, groundLayer);
        
        // ===== CIRCLE QUERIES =====
        // Boolean check — is there anything in this circle?
        bool isGrounded = Physics2D.OverlapCircle(transform.position, 0.2f, groundLayer);
        
        // Get ONE collider in a circle
        Collider2D nearestEnemy = Physics2D.OverlapCircle(transform.position, 5f, enemyLayer);
        
        // Get ALL colliders in a circle (NonAlloc = no heap allocation)
        int count = Physics2D.OverlapCircleNonAlloc(transform.position, 5f, overlapBuffer, enemyLayer);
        for (int i = 0; i < count; i++)
        {
            if (overlapBuffer[i].TryGetComponent<Enemy>(out Enemy e))
                e.Alert();
        }
        
        // ===== BOX QUERIES =====
        // Check a rectangular area
        bool hasEnemy = Physics2D.OverlapBox(
            transform.position,     // center
            new Vector2(3f, 1f),    // size (width, height)
            0f,                     // angle
            enemyLayer
        );
        
        // ===== RAYCASTS =====
        // Cast a ray from a point in a direction, returns first hit
        RaycastHit2D rayHit = Physics2D.Raycast(
            transform.position,     // start point
            Vector2.down,           // direction
            2f,                     // max distance
            groundLayer             // only hit ground layer
        );
        
        if (rayHit.collider != null) // null = didn't hit anything
        {
            Debug.Log($"Ground below at: {rayHit.point}");
            Debug.Log($"Surface normal: {rayHit.normal}");
            Debug.Log($"Distance: {rayHit.distance}");
        }
        
        // Debug — draw ray in Scene view to visualize it
        Debug.DrawRay(transform.position, Vector2.down * 2f, Color.red);
        
        // ===== LINECAST =====
        // Check if there's a clear line between two specific points
        RaycastHit2D lineHit = Physics2D.Linecast(transform.position, targetPosition, groundLayer);
        bool hasLineOfSight = lineHit.collider == null; // null = nothing blocking
        
        // ===== SHAPECAST (BoxCast, CapsuleCast, CircleCast) =====
        // Move a shape in a direction, find first object it hits
        RaycastHit2D boxHit = Physics2D.BoxCast(
            transform.position,
            new Vector2(0.9f, 0.1f), // box size (slightly smaller than collider to avoid false hits)
            0f,                       // angle
            Vector2.down,             // direction
            0.5f,                     // distance
            groundLayer
        );
        bool isGroundedBox = boxHit.collider != null;
    }
}
```

---

## 00E.6 — Physics Material 2D — Friction and Bounciness

A **Physics Material 2D** is an asset you create (right-click in Project → Create → 2D → Physics Material 2D) and assign to a Collider.

```
PhysicsMaterial2D
├── Friction      [0.4]   ← 0 = frictionless ice, 1 = very grippy rubber
└── Bounciness    [0]     ← 0 = no bounce, 1 = perfect elastic bounce
```

```csharp
// Create a physics material in code (for runtime configuration)
PhysicsMaterial2D iceMaterial = new PhysicsMaterial2D();
iceMaterial.friction = 0f;
iceMaterial.bounciness = 0f;

// Apply to a specific collider
BoxCollider2D box = GetComponent<BoxCollider2D>();
box.sharedMaterial = iceMaterial;

// Or apply to the Rigidbody (affects all colliders on this body)
rb.sharedMaterial = iceMaterial;

// Reset material
box.sharedMaterial = null; // use default (no material)

// Common presets to create in Project:
// "Player_Material":    friction=0.0, bounciness=0.0  (no wall-sticking)
// "Ground_Material":    friction=0.4, bounciness=0.0  (normal ground)
// "Ice_Material":       friction=0.0, bounciness=0.0  (slippery)
// "Bouncy_Material":    friction=0.0, bounciness=0.8  (trampoline)
// "Sticky_Material":    friction=1.0, bounciness=0.0  (wall-climb)
```

---

## 00E.7 — Common Collider Setups by Object Type

### Player (Platformer 2D)
```csharp
// Recommended: CapsuleCollider2D + BoxCollider2D combo
// CapsuleCollider2D: main body (avoids corner catching on walls)
// BoxCollider2D (isTrigger=true): for specific hitbox areas

[RequireComponent(typeof(Rigidbody2D))]
[RequireComponent(typeof(CapsuleCollider2D))]
public class Player2DSetup : MonoBehaviour
{
    void Awake()
    {
        CapsuleCollider2D cap = GetComponent<CapsuleCollider2D>();
        cap.size = new Vector2(0.5f, 1.8f);       // slim, tall capsule
        cap.direction = CapsuleDirection2D.Vertical;
        cap.offset = new Vector2(0f, 0f);
        cap.isTrigger = false;                     // physical collision
        
        // Apply zero-friction material so player doesn't stick to walls
        PhysicsMaterial2D noFriction = new PhysicsMaterial2D { friction = 0f };
        cap.sharedMaterial = noFriction;
    }
}
```

### Ground/Platform (Static)
```csharp
// No Rigidbody needed for ground — just add a BoxCollider2D
// In Inspector: Add Component → Box Collider 2D → set Size to match sprite
// Set the object's layer to "Ground"

// For a tilemap: Add TilemapCollider2D and CompositeCollider2D to the Tilemap GameObject
// TilemapCollider2D auto-generates shapes from tile data
// CompositeCollider2D merges them into one efficient shape
```

### Coin/Pickup (Trigger)
```csharp
[RequireComponent(typeof(CircleCollider2D))]
public class Coin : MonoBehaviour
{
    void Awake()
    {
        CircleCollider2D col = GetComponent<CircleCollider2D>();
        col.radius = 0.4f;
        col.isTrigger = true; // player passes through, but we detect the overlap
    }
    
    void OnTriggerEnter2D(Collider2D other)
    {
        if (other.CompareTag("Player"))
        {
            GameManager.Instance.AddScore(10);
            Destroy(gameObject); // or return to pool
        }
    }
}
```

### Enemy Detection Zone
```csharp
// Enemy has a large trigger circle for "detection"
// and a small collision box for physical body

public class Enemy : MonoBehaviour
{
    [Header("Colliders")]
    [SerializeField] private BoxCollider2D bodyCollider;        // physical body
    [SerializeField] private CircleCollider2D detectionZone;    // trigger zone
    
    void Awake()
    {
        bodyCollider.isTrigger = false;    // physical
        bodyCollider.size = new Vector2(0.8f, 1.6f);
        
        detectionZone.isTrigger = true;    // trigger zone
        detectionZone.radius = 5f;         // 5 unit detection radius
    }
    
    void OnTriggerEnter2D(Collider2D other)
    {
        if (other.CompareTag("Player"))
            StartChasing();
    }
    
    void OnTriggerExit2D(Collider2D other)
    {
        if (other.CompareTag("Player"))
            StopChasing();
    }
    
    void StartChasing() { }
    void StopChasing() { }
}
```

---

## 00E.8 — One-Way Platforms (PlatformEffector2D)

Let the player jump UP through platforms but land ON them from above.

```csharp
// In Inspector:
// 1. Add BoxCollider2D to platform
// 2. Check "Used By Effector"
// 3. Add PlatformEffector2D component
// 4. Set "Surface Arc" to 180 (only collide from the top half)

// In code:
PlatformEffector2D effector = GetComponent<PlatformEffector2D>();
effector.useOneWay = true;
effector.surfaceArc = 180f; // top 180 degrees are solid

// Drop through platform:
IEnumerator DropThrough(BoxCollider2D platformCollider)
{
    platformCollider.enabled = false;
    yield return new WaitForSeconds(0.5f);
    platformCollider.enabled = true;
}
```

---

## 📝 Summary — 2D Collider Quick Reference

| Collider Type | Shape | Best For |
| :--- | :--- | :--- |
| `BoxCollider2D` | Rectangle | Platforms, buildings, default choice |
| `CircleCollider2D` | Circle | Balls, coins, circular enemies |
| `CapsuleCollider2D` | Capsule (pill) | Player characters (avoids edge snag) |
| `PolygonCollider2D` | Custom polygon | Irregular shapes, traced sprites |
| `EdgeCollider2D` | Line/path | Terrain outlines, ropes, rails |
| `CompositeCollider2D` | Merged shape | Tilemaps, merged platforms |

| Is Trigger | What It Does | Callbacks |
| :--- | :--- | :--- |
| `false` (default) | Physical solid — blocks movement | `OnCollisionEnter/Stay/Exit2D` |
| `true` | Ghost zone — detects overlap only | `OnTriggerEnter/Stay/Exit2D` |

**Previous:** [[00D — Rigidbody2D — Every Variable Explained]] | **Next:** [[00F — Rigidbody3D & Physics3D]]
