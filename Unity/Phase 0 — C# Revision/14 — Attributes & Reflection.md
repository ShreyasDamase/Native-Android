# 14 — Attributes & Reflection
### 🟡 Block B — Unity-Critical C# Features

> [!NOTE]
> Attributes are **metadata decorators** you put above classes, fields, or methods. In Unity, attributes control Inspector visibility, add editor tools, enforce component requirements, and more. You'll use them in every single script you write.

---

## 14.1 — What Are Attributes?

Attributes are square-bracket decorators `[AttributeName]` placed above code elements. They add metadata that tools (Unity Editor, compiler) read to change behavior.

```csharp
// Syntax: [AttributeName] or [AttributeName(parameters)]
[RequireComponent(typeof(Rigidbody2D))]  // Attribute with parameter
[DisallowMultipleComponent]              // Attribute with no parameters
public class PlayerController : MonoBehaviour
{
    [SerializeField]                     // Field attribute
    private float speed = 5f;
    
    [Header("Combat Settings")]         // Group header in Inspector
    [Range(0f, 100f)]                   // Clamp slider in Inspector
    [SerializeField] private float damage = 25f;
    
    [Tooltip("Time in seconds between attacks")]  // Hover tooltip
    [SerializeField] private float attackRate = 0.5f;
    
    [ContextMenu("Reset Stats")]         // Right-click method in Inspector
    void ResetStats()
    {
        speed = 5f;
        damage = 25f;
    }
}
```

---

## 14.2 — The 10 Most Important Unity Attributes

### `[SerializeField]` — Make Private Fields Show in Inspector
```csharp
public class Enemy : MonoBehaviour
{
    // Private field — hidden from Inspector by default
    private float speed = 3f;  // HIDDEN
    
    // Add [SerializeField] — now visible AND editable in Inspector
    // BEST PRACTICE: use private + SerializeField instead of public fields
    [SerializeField] private float moveSpeed = 3f;  // VISIBLE
    [SerializeField] private float maxHealth = 100f; // VISIBLE
    [SerializeField] private GameObject bulletPrefab; // VISIBLE
    
    // Public field — also visible, but AVOID — breaks encapsulation
    public float badPractice = 5f; // visible but anyone can change it
}
```

### `[Header]` — Group Fields with a Bold Label
```csharp
public class PlayerController : MonoBehaviour
{
    [Header("Movement")]
    [SerializeField] private float moveSpeed = 5f;
    [SerializeField] private float runSpeed = 10f;
    
    [Header("Jump")]
    [SerializeField] private float jumpForce = 12f;
    [SerializeField] private int maxJumpCount = 2;
    [SerializeField] private LayerMask groundLayer;
    
    [Header("Combat")]
    [SerializeField] private float damage = 25f;
    [SerializeField] private float attackRange = 1.5f;
    [SerializeField] private float attackCooldown = 0.5f;
    
    [Header("References")]
    [SerializeField] private Transform weaponMount;
    [SerializeField] private ParticleSystem dustParticles;
    [SerializeField] private Animator animator;
}
```

### `[Range]` — Slider for Numeric Values
```csharp
[SerializeField, Range(0f, 100f)] private float health = 100f;
[SerializeField, Range(1f, 50f)]  private float moveSpeed = 5f;
[SerializeField, Range(0, 10)]    private int bulletCount = 3;
[SerializeField, Range(0f, 1f)]   private float critChance = 0.15f;
```

### `[Tooltip]` — Hover Description
```csharp
[Tooltip("The speed in units per second. Affected by Time.timeScale.")]
[SerializeField] private float moveSpeed = 5f;

[Tooltip("Set this to the Ground layer in the Layer dropdown.")]
[SerializeField] private LayerMask groundLayer;
```

### `[RequireComponent]` — Auto-Add Dependencies
```csharp
// Unity will automatically add Rigidbody2D when this script is added
// AND prevent you from removing Rigidbody2D while this script exists
[RequireComponent(typeof(Rigidbody2D))]
[RequireComponent(typeof(Animator))]
[RequireComponent(typeof(AudioSource))]
public class PlayerController2D : MonoBehaviour
{
    private Rigidbody2D rb;      // guaranteed to exist!
    private Animator animator;   // guaranteed to exist!
    
    void Awake()
    {
        // Safe to call without null check — RequireComponent guarantees it
        rb = GetComponent<Rigidbody2D>();
        animator = GetComponent<Animator>();
    }
}
```

### `[DisallowMultipleComponent]` — Enforce One Instance
```csharp
// Prevents adding two of this script to the same GameObject
[DisallowMultipleComponent]
public class PlayerHealth : MonoBehaviour { }
```

### `[HideInInspector]` — Hide Public Field from Inspector
```csharp
// Public (accessible by other scripts) but hidden from Inspector
[HideInInspector] public bool isGrounded;
[HideInInspector] public int currentWave;
```

### `[System.Serializable]` — Serialize Custom Classes
```csharp
// Makes a NON-MonoBehaviour class visible in the Inspector
[System.Serializable]
public class WeaponStats
{
    public string Name;
    [Range(1f, 100f)] public float Damage = 10f;
    [Range(0.1f, 5f)] public float FireRate = 1f;
    public float Range = 20f;
    public bool IsAutomatic = false;
    public AudioClip FireSound;
    public GameObject BulletPrefab;
}

// Now WeaponStats shows as an expandable object in the Inspector
public class Gun : MonoBehaviour
{
    [SerializeField] private WeaponStats stats; // Inspectable!
    [SerializeField] private List<WeaponStats> upgradeChain; // List works too!
}
```

### `[ContextMenu]` — Add Right-Click Methods to Inspector
```csharp
public class LevelGenerator : MonoBehaviour
{
    [ContextMenu("Generate Level")]
    void GenerateLevel()
    {
        // Runs in Editor mode when you right-click the script component
        ClearExistingLevel();
        PlaceRooms();
        ConnectRooms();
        Debug.Log("Level generated!");
    }
    
    [ContextMenu("Clear Level")]
    void ClearLevel()
    {
        ClearExistingLevel();
    }
}
```

### `[CreateAssetMenu]` — Add ScriptableObjects to Create Menu
```csharp
// Right-click in Project window → Create → Game/EnemyData
[CreateAssetMenu(fileName = "NewEnemyData", menuName = "Game/Enemy Data", order = 1)]
public class EnemyData : ScriptableObject
{
    public string EnemyName;
    [Range(10f, 500f)] public float MaxHealth = 100f;
    [Range(1f, 20f)] public float MoveSpeed = 3f;
    public int ScoreValue = 50;
    public Sprite Sprite;
    public RuntimeAnimatorController AnimatorController;
}
```

---

## 14.3 — Editor-Only Attributes

```csharp
// [ExecuteInEditMode] — script runs in the Editor without pressing Play
// ⚠️ Careful — Update() runs constantly in Editor
[ExecuteInEditMode]
public class GridDrawer : MonoBehaviour
{
    [SerializeField] private int gridWidth = 10;
    [SerializeField] private int gridHeight = 10;
    
    void OnDrawGizmos()
    {
        // Draw grid lines in Scene view
        Gizmos.color = Color.cyan;
        for (int x = 0; x <= gridWidth; x++)
            Gizmos.DrawLine(new Vector3(x, 0, 0), new Vector3(x, gridHeight, 0));
        for (int y = 0; y <= gridHeight; y++)
            Gizmos.DrawLine(new Vector3(0, y, 0), new Vector3(gridWidth, y, 0));
    }
}

// [ExecuteAlways] — runs in Edit mode AND Play mode (Unity 2019.3+)
[ExecuteAlways]
public class LightingPreview : MonoBehaviour { }

// Conditional compilation — only compile in Editor builds
#if UNITY_EDITOR
using UnityEditor;

// This code doesn't exist in your final game build
[CustomEditor(typeof(EnemyAI))]
public class EnemyAIEditor : Editor
{
    public override void OnInspectorGUI()
    {
        base.OnInspectorGUI();
        
        if (GUILayout.Button("Force Alert State"))
        {
            ((EnemyAI)target).SetAlert(true);
        }
    }
}
#endif
```

---

## 14.4 — Custom Attributes

You can create your own attributes for validation or tooling.

```csharp
// Define a custom attribute
[System.AttributeUsage(System.AttributeTargets.Field)]
public class MinValueAttribute : System.Attribute
{
    public float MinValue { get; }
    public MinValueAttribute(float minValue) => MinValue = minValue;
}

// Use the custom attribute
public class EnemyStats : MonoBehaviour
{
    [MinValue(0f)] private float health = 100f;
    [MinValue(0.1f)] private float speed = 3f;
}
```

---

## 14.5 — Reflection (Reading Attributes at Runtime)

Reflection lets you inspect and interact with types at runtime. Useful for serialization, debug tools, and editor scripts. **Avoid in gameplay hot paths — it's slow.**

```csharp
using System.Reflection;

// Get all fields of a type at runtime
public class DebugInspector : MonoBehaviour
{
    void PrintAllFields(object target)
    {
        System.Type type = target.GetType();
        FieldInfo[] fields = type.GetFields(BindingFlags.Public | BindingFlags.NonPublic | BindingFlags.Instance);
        
        foreach (FieldInfo field in fields)
        {
            object value = field.GetValue(target);
            Debug.Log($"{field.Name} ({field.FieldType.Name}): {value}");
        }
    }
    
    // Check if a field has a specific attribute
    bool HasAttribute<TAttr>(FieldInfo field) where TAttr : System.Attribute
    {
        return field.GetCustomAttribute<TAttr>() != null;
    }
    
    // Get all fields with [SerializeField] attribute
    FieldInfo[] GetSerializedFields(System.Type type)
    {
        return type.GetFields(BindingFlags.NonPublic | BindingFlags.Instance)
                   .Where(f => f.GetCustomAttribute<SerializeField>() != null)
                   .ToArray();
    }
    
    // Call a method by name (editor tooling, not gameplay)
    void CallMethodByName(MonoBehaviour target, string methodName)
    {
        MethodInfo method = target.GetType().GetMethod(methodName, 
            BindingFlags.Public | BindingFlags.NonPublic | BindingFlags.Instance);
        method?.Invoke(target, null);
    }
}
```

---

## 14.6 — `[Serializable]` Deep Dive — Nested Data in Inspector

```csharp
[System.Serializable]
public class AudioConfig
{
    public AudioClip Clip;
    [Range(0f, 1f)] public float Volume = 1f;
    [Range(0.5f, 2f)] public float Pitch = 1f;
    public bool Loop = false;
}

[System.Serializable]
public class WaveData
{
    public string WaveName;
    public int EnemyCount;
    public float SpawnInterval = 1f;
    public GameObject EnemyPrefab;
    public AudioConfig BattleMusic;  // nested serializable!
}

// Full game config — all editable in Inspector
public class GameConfig : MonoBehaviour
{
    [Header("Gameplay")]
    [SerializeField] private WaveData[] waves;     // array of complex objects
    [SerializeField] private List<WaveData> extraWaves; // list works too
    
    [Header("Audio")]
    [SerializeField] private AudioConfig mainMenuMusic;
    [SerializeField] private AudioConfig gameOverStinger;
}
```

---

## 14.7 — 🎮 2D vs 3D: Attribute Differences

### 🎮 2D Common Attribute Patterns
```csharp
[RequireComponent(typeof(Rigidbody2D))]
[RequireComponent(typeof(Collider2D))]
[DisallowMultipleComponent]
public class Character2D : MonoBehaviour
{
    [Header("Ground Detection — 2D")]
    [SerializeField] private Transform groundCheckPoint;
    [SerializeField, Range(0.05f, 0.5f)] private float groundCheckRadius = 0.2f;
    [SerializeField] private LayerMask groundLayer;
    
    [Header("2D Physics")]
    [SerializeField, Range(0f, 10f)] private float gravityScale = 3f;
    [SerializeField] private bool constrainRotation = true;
    
    void Awake()
    {
        var rb = GetComponent<Rigidbody2D>();
        rb.gravityScale = gravityScale;
        rb.constraints = constrainRotation 
            ? RigidbodyConstraints2D.FreezeRotation 
            : RigidbodyConstraints2D.None;
    }
    
    // Editor visualization
    void OnDrawGizmosSelected()
    {
        Gizmos.color = Color.green;
        if (groundCheckPoint != null)
            Gizmos.DrawWireSphere(groundCheckPoint.position, groundCheckRadius);
    }
}
```

### 🎮 3D Common Attribute Patterns
```csharp
[RequireComponent(typeof(CharacterController))]
[RequireComponent(typeof(Animator))]
[DisallowMultipleComponent]
public class Character3D : MonoBehaviour
{
    [Header("Movement — 3D")]
    [SerializeField, Range(1f, 20f)] private float walkSpeed = 5f;
    [SerializeField, Range(1f, 30f)] private float runSpeed = 10f;
    [SerializeField, Range(0f, 90f)] private float slopeLimit = 45f;
    
    [Header("Ground Detection — 3D")]
    [SerializeField, Range(0f, 1f)] private float groundCheckDistance = 0.3f;
    [SerializeField, Range(0.1f, 1f)] private float groundSphereRadius = 0.4f;
    [SerializeField] private LayerMask groundLayer;
    
    [Header("3D Camera")]
    [Tooltip("Assign the CinemachineVirtualCamera or Camera Transform")]
    [SerializeField] private Transform cameraTransform;
    
    // 3D-specific gizmos — draw sphere cast visualization
    void OnDrawGizmosSelected()
    {
        Gizmos.color = Color.yellow;
        Gizmos.DrawWireSphere(transform.position + Vector3.down * groundCheckDistance, groundSphereRadius);
    }
}
```

---

## 📝 Summary

| Attribute | Where | Effect |
| :--- | :--- | :--- |
| `[SerializeField]` | Private field | Shows in Inspector |
| `[Header("text")]` | Field | Bold label in Inspector |
| `[Range(min,max)]` | Numeric field | Slider in Inspector |
| `[Tooltip("text")]` | Field | Hover description |
| `[HideInInspector]` | Public field | Hidden from Inspector |
| `[RequireComponent(typeof(T))]` | Class | Auto-adds T, prevents removal |
| `[DisallowMultipleComponent]` | Class | Only one instance allowed |
| `[System.Serializable]` | Non-MB class | Makes it editable in Inspector |
| `[CreateAssetMenu]` | ScriptableObject | Adds right-click Create menu entry |
| `[ContextMenu("name")]` | Method | Adds right-click method in Inspector |
| `[ExecuteAlways]` | Class | Runs in Edit mode |

**Previous:** [[13 — Async Await & Tasks]] | **Next:** [[15 — Memory Management & GC]]
