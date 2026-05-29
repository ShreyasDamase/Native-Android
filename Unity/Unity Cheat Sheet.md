# ⚡ Unity Cheat Sheet
A quick-reference guide for common Unity APIs, C# patterns, and editor lifecycles.

---

## 📐 Vector Mathematics (Vector2 & Vector3)

```csharp
Vector2.up                  // (0, 1)    | Vector2.down      (0, -1)
Vector2.right               // (1, 0)    | Vector2.left      (-1, 0)
Vector2.zero                // (0, 0)    | Vector2.one       (1, 1)

// Direction from position A to position B
Vector3 direction = (positionB - positionA).normalized;

// Distance between two points
float dist = Vector3.Distance(posA, posB); 
float distSqr = (posB - posA).sqrMagnitude; // Optimized: Use for range checks

// Dot Product alignment checks (1 = same direction | 0 = perpendicular | -1 = opposite)
float dotResult = Vector3.Dot(vectorA, vectorB);

// 2D Rotation Angle from Vector Direction
float angle = Mathf.Atan2(direction.y, direction.x) * Mathf.Rad2Deg;
transform.rotation = Quaternion.Euler(0f, 0f, angle - 90f); // Offset 90 degrees if sprite faces up
```

---

## 🔭 Transform System

```csharp
transform.position          // Position in world coordinates
transform.localPosition     // Position relative to parent transform
transform.rotation          // Rotation in world space (Quaternion)
transform.localEulerAngles  // Rotation relative to parent in degrees (Vector3)
transform.localScale        // Local scale scale multiplier

transform.up                // Local y-axis pointing up (green arrow)
transform.right             // Local x-axis pointing right (red arrow)

// Parenting
transform.SetParent(newParent, worldPositionStays: true); // Attach to parent
transform.SetParent(null); // Detach back to scene root
```

---

## ⚙️ Physics 2D & Queries

```csharp
Rigidbody2D rb = GetComponent<Rigidbody2D>();

// Velocity Modification (Unity 6+)
rb.linearVelocity = new Vector2(speedX, speedY); // Replaces obsolete rb.velocity
rb.angularVelocity = spinSpeed;

// Applying Forces
rb.AddForce(Vector2.up * thrust, ForceMode2D.Force);       // Continuous force
rb.AddForce(Vector2.up * jumpForce, ForceMode2D.Impulse);   // Instant force push

// Physics Queries
int enemyLayer = LayerMask.GetMask("Enemies");
RaycastHit2D hit = Physics2D.Raycast(origin, Vector2.down, 1.5f, enemyLayer);
if (hit.collider != null)
{
    Vector2 hitPoint = hit.point;
}

// Non-Allocating Overlap Check
Collider2D[] results = new Collider2D[5];
int count = Physics2D.OverlapCircleNonAlloc(transform.position, 3.0f, results, enemyLayer);
```

---

## 🕹️ The New Input System

```csharp
using UnityEngine.InputSystem;

// 1. Direct Polling (Unity 6 / Input System v1.7+)
Vector2 move = InputSystem.actions["Move"].ReadValue<Vector2>();
bool isPressed = InputSystem.actions["Fire"].IsPressed();
bool tapped = InputSystem.actions["Jump"].WasPressedThisFrame();

// 2. Action Callback Subscriptions (Generated C# Wrapper)
private PlayerControls controls;
void Awake() => controls = new PlayerControls();
void OnEnable()
{
    controls.Enable();
    controls.Player.Jump.performed += PerformJump;
}
void OnDisable()
{
    controls.Player.Jump.performed -= PerformJump;
    controls.Disable();
}
void PerformJump(InputAction.CallbackContext ctx) => rb.AddForce(Vector2.up * 5f, ForceMode2D.Impulse);
```

---

## 🛠️ Component Access & Safety

```csharp
// Standard Search (returns null if component not found)
MyScript script = GetComponent<MyScript>();

// Search Hierarchy
MyScript childScript = GetComponentInChildren<MyScript>();
MyScript parentScript = GetComponentInParent<MyScript>();

// High-Performance Safe Query (recommended)
if (TryGetComponent<Rigidbody2D>(out var rb))
{
    rb.gravityScale = 2f;
}
```

---

## 🧮 Mathf Utilities

```csharp
Mathf.Clamp(value, min, max); // Limits value between min and max bounds
Mathf.Clamp01(value);         // Limits value between 0.0 and 1.0

Mathf.Repeat(time, duration); // Loops value like a clock (0 to duration)
Mathf.PingPong(time, length); // Bounces value back and forth (0 to length)

// Framerate-Independent Lerp
float t = 1f - Mathf.Exp(-decaySpeed * Time.deltaTime);
transform.position = Vector3.Lerp(transform.position, targetPos, t);
```

---

## 📦 Object Pooling (`UnityEngine.Pool`)

```csharp
using UnityEngine.Pool;

// Declare the pool reference
private IObjectPool<GameObject> bulletPool;

// Initialize
bulletPool = new ObjectPool<GameObject>(
    createFunc: () => Instantiate(bulletPrefab),
    actionOnGet: (obj) => obj.SetActive(true),
    actionOnRelease: (obj) => obj.SetActive(false),
    actionOnDestroy: (obj) => Destroy(obj),
    maxSize: 50
);

// Fetch & Release
GameObject bullet = bulletPool.Get();
bulletPool.Release(bullet);
```

---

## 🕒 MonoBehaviour Lifecycle Sequence

```
1. OnValidate()       → Runs inside Editor when component variables are updated
2. Awake()            → Called once when script is loaded (runs even if disabled)
3. OnEnable()         → Called whenever object is activated
4. Start()            → Called once before first frame update (runs only if enabled)
5. FixedUpdate()      → Physics step loop (reliable timestep e.g., 0.02s)
6. Update()           → Gameplay step loop (variable graphic timestep)
7. LateUpdate()       → Camera positioning step loop (runs after Update completes)
8. OnDisable()        → Called whenever object is deactivated
9. OnDestroy()        → Called when object is permanently deleted
```

---

## ⏳ Coroutines

```csharp
// Definition
IEnumerator CountRoutine(float duration)
{
    yield return new WaitForSeconds(duration); // Delay execution
    DoAction();
}

// Start
Coroutine loop = StartCoroutine(CountRoutine(2.5f));

// Stop
StopCoroutine(loop);
StopAllCoroutines(); // Stops all routines running on this script instance
```

---

## 🎥 Camera conversions

```csharp
// Convert mouse pixels position to world space
Vector3 worldPos = Camera.main.ScreenToWorldPoint(Input.mousePosition);
worldPos.z = 0f; // Anchor to 2D plane

// Convert world point to normalized viewport bounds (0,0 to 1,1)
Vector3 viewportPos = Camera.main.WorldToViewportPoint(transform.position);
```

---

## 📝 TextMeshPro (TMP) Rich Text Tags

```xml
Score: <color=yellow>100</color>
Status: <b>BOLD</b> and <i>ITALICS</i>
Scale: <size=150%>SUPER COMBOS!</size>
Spacing: <pos=50%>Offset text
```

---

## 🪲 Debugging & Editor Gizmos

```csharp
// Write to Console
Debug.Log("Standard log message.");
Debug.LogWarning("Potential problem check.");
Debug.LogError("Error or failed validation.");

// Freeze play execution in editor on trigger
Debug.Break(); 

// Draw visual vectors in scene for debugging
Debug.DrawLine(posA, posB, Color.yellow);
Debug.DrawRay(origin, direction * length, Color.red);

// Draw Editor Icons / Shapes (visible only in Scene View)
private void OnDrawGizmos()
{
    Gizmos.color = Color.cyan;
    Gizmos.DrawWireSphere(transform.position, 2.5f);
}
```
