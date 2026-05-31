# 20 — Extension Methods
### 🟡 Block D — Modern C# (Unity 6 / C# 10+)

> [!NOTE]
> Extension methods let you **add new methods to existing types** without modifying them. This is how you add `.Remap()` to `float`, `.WithX()` to `Vector3`, or `.IsInLayerMask()` to `GameObject`. Professional Unity projects almost always have a `Extensions.cs` file full of these.

---

## 20.1 — How Extension Methods Work

```csharp
// Extension methods are defined in STATIC CLASSES
// The first parameter is 'this TypeName name' — this marks it as an extension

public static class FloatExtensions
{
    // Add a Remap method to float
    public static float Remap(this float value, float from1, float to1, float from2, float to2)
    {
        return from2 + (value - from1) * (to2 - from2) / (to1 - from1);
    }
}

// Now you can call it on any float variable:
float healthPercent = 0.7f;
float barWidth = healthPercent.Remap(0f, 1f, 0f, 200f); // 140f

// Without extension method, you'd write:
float barWidth2 = FloatExtensions.Remap(healthPercent, 0f, 1f, 0f, 200f); // same thing

// KEY RULE: The file must be in a namespace that the caller can see (or no namespace)
```

---

## 20.2 — Float Extensions (Must-Have)

```csharp
public static class FloatExtensions
{
    // Remap a value from one range to another
    // Usage: speed.Remap(0f, maxSpeed, 0f, 1f) — normalize to 0-1
    public static float Remap(this float value, float inMin, float inMax, float outMin, float outMax)
    {
        return outMin + (value - inMin) * (outMax - outMin) / (inMax - inMin);
    }
    
    // Clamp to 0-1 range
    // Usage: damage.Clamp01()
    public static float Clamp01(this float value) => Mathf.Clamp01(value);
    
    // Clamp between min and max
    // Usage: speed.Clamp(0f, maxSpeed)
    public static float Clamp(this float value, float min, float max) => Mathf.Clamp(value, min, max);
    
    // Is approximately equal (floating point comparison)
    // Usage: if (speed.Approximately(0f)) { ... }
    public static bool Approximately(this float a, float b) => Mathf.Approximately(a, b);
    
    // Check if between two values
    // Usage: if (health.Between(0f, 25f)) ShowDangerEffect();
    public static bool Between(this float value, float min, float max) => value >= min && value <= max;
    
    // Round to nearest value
    // Usage: angle.RoundTo(45f) — snap to 45-degree increments
    public static float RoundTo(this float value, float step) => Mathf.Round(value / step) * step;
    
    // Convert to an int (ceiling, floor, round)
    public static int CeilToInt(this float value) => Mathf.CeilToInt(value);
    public static int FloorToInt(this float value) => Mathf.FloorToInt(value);
    public static int RoundToInt(this float value) => Mathf.RoundToInt(value);
    
    // Absolute value
    public static float Abs(this float value) => Mathf.Abs(value);
    
    // Ease-in-out (smoothstep) — useful for lerp animations
    public static float SmoothStep(this float t) => t * t * (3f - 2f * t);
    
    // Convert between degrees and radians
    public static float ToRadians(this float degrees) => degrees * Mathf.Deg2Rad;
    public static float ToDegrees(this float radians) => radians * Mathf.Rad2Deg;
}
```

---

## 20.3 — Vector2 / Vector3 Extensions (Most Used)

```csharp
public static class VectorExtensions
{
    // ===== Vector2 Extensions =====
    
    // Create Vector2 with specific component changed (avoids the copy-modify-assign boilerplate)
    public static Vector2 WithX(this Vector2 v, float x) => new Vector2(x, v.y);
    public static Vector2 WithY(this Vector2 v, float y) => new Vector2(v.x, y);
    
    // Rotate a 2D vector by degrees
    // Usage: direction.Rotate(45f) — rotate 45 degrees
    public static Vector2 Rotate(this Vector2 v, float degrees)
    {
        float rad = degrees * Mathf.Deg2Rad;
        float cos = Mathf.Cos(rad);
        float sin = Mathf.Sin(rad);
        return new Vector2(cos * v.x - sin * v.y, sin * v.x + cos * v.y);
    }
    
    // Check if roughly zero
    public static bool IsNearlyZero(this Vector2 v, float threshold = 0.001f)
        => v.sqrMagnitude < threshold * threshold;
    
    // Clamp magnitude
    public static Vector2 ClampMagnitude(this Vector2 v, float max)
        => Vector2.ClampMagnitude(v, max);
    
    // ===== Vector3 Extensions =====
    
    // Create Vector3 with specific component changed
    public static Vector3 WithX(this Vector3 v, float x) => new Vector3(x, v.y, v.z);
    public static Vector3 WithY(this Vector3 v, float y) => new Vector3(v.x, y, v.z);
    public static Vector3 WithZ(this Vector3 v, float z) => new Vector3(v.x, v.y, z);
    
    // Flatten to XZ plane (3D floor projection)
    // Usage: velocity.Flat() — ignore Y velocity for ground speed calculation
    public static Vector3 Flat(this Vector3 v) => new Vector3(v.x, 0f, v.z);
    
    // Direction to another point
    public static Vector3 DirectionTo(this Vector3 from, Vector3 to) => (to - from).normalized;
    
    // Distance to another point
    public static float DistanceTo(this Vector3 from, Vector3 to) => Vector3.Distance(from, to);
    
    // Flat distance (ignore Y — for AI detection on terrain)
    public static float FlatDistanceTo(this Vector3 from, Vector3 to)
    {
        Vector3 diff = to - from;
        diff.y = 0f;
        return diff.magnitude;
    }
    
    // Check if nearly zero
    public static bool IsNearlyZero(this Vector3 v, float threshold = 0.001f)
        => v.sqrMagnitude < threshold * threshold;
    
    // Convert Vector3 to Vector2 (drop Z)
    public static Vector2 ToVector2(this Vector3 v) => new Vector2(v.x, v.y);
    
    // Convert Vector2 to Vector3 (add Z = 0)
    public static Vector3 ToVector3(this Vector2 v, float z = 0f) => new Vector3(v.x, v.y, z);
}
```

---

## 20.4 — Transform Extensions

```csharp
public static class TransformExtensions
{
    // Reset position, rotation, scale to defaults
    public static void Reset(this Transform t)
    {
        t.localPosition = Vector3.zero;
        t.localRotation = Quaternion.identity;
        t.localScale = Vector3.one;
    }
    
    // Set position X only (avoids copy-modify-assign)
    public static void SetX(this Transform t, float x)
    {
        Vector3 pos = t.position;
        pos.x = x;
        t.position = pos;
    }
    
    public static void SetY(this Transform t, float y)
    {
        Vector3 pos = t.position;
        pos.y = y;
        t.position = pos;
    }
    
    public static void SetZ(this Transform t, float z)
    {
        Vector3 pos = t.position;
        pos.z = z;
        t.position = pos;
    }
    
    // Get all direct children as list
    public static List<Transform> GetChildren(this Transform parent)
    {
        List<Transform> children = new List<Transform>(parent.childCount);
        for (int i = 0; i < parent.childCount; i++)
            children.Add(parent.GetChild(i));
        return children;
    }
    
    // Destroy all children
    public static void DestroyAllChildren(this Transform parent)
    {
        for (int i = parent.childCount - 1; i >= 0; i--)
            Object.Destroy(parent.GetChild(i).gameObject);
    }
    
    // Find child by name (including inactive)
    public static Transform FindDeep(this Transform parent, string name)
    {
        foreach (Transform child in parent)
        {
            if (child.name == name) return child;
            Transform found = child.FindDeep(name);
            if (found != null) return found;
        }
        return null;
    }
    
    // Look at target in 2D (only rotate around Z)
    public static void LookAt2D(this Transform t, Vector2 target)
    {
        Vector2 dir = (target - (Vector2)t.position).normalized;
        float angle = Mathf.Atan2(dir.y, dir.x) * Mathf.Rad2Deg;
        t.rotation = Quaternion.Euler(0f, 0f, angle);
    }
}
```

---

## 20.5 — GameObject Extensions

```csharp
public static class GameObjectExtensions
{
    // GetOrAddComponent — get existing or add if missing
    public static T GetOrAddComponent<T>(this GameObject go) where T : Component
    {
        return go.TryGetComponent<T>(out T comp) ? comp : go.AddComponent<T>();
    }
    
    // Check if in layer mask
    // Usage: if (other.gameObject.IsInLayerMask(groundLayer)) { }
    public static bool IsInLayerMask(this GameObject go, LayerMask mask)
    {
        return (mask.value & (1 << go.layer)) != 0;
    }
    
    // Safe SetActive that checks if already in that state
    public static void SetActiveOptimized(this GameObject go, bool active)
    {
        if (go.activeSelf != active) go.SetActive(active);
    }
    
    // Get all components implementing an interface
    public static List<T> GetComponentsWithInterface<T>(this GameObject go) where T : class
    {
        List<T> results = new List<T>();
        Component[] components = go.GetComponents<Component>();
        foreach (Component comp in components)
        {
            if (comp is T match)
                results.Add(match);
        }
        return results;
    }
    
    // Set layer on object and all its children
    public static void SetLayerRecursively(this GameObject go, int layer)
    {
        go.layer = layer;
        for (int i = 0; i < go.transform.childCount; i++)
            go.transform.GetChild(i).gameObject.SetLayerRecursively(layer);
    }
}
```

---

## 20.6 — Rigidbody Extensions

```csharp
public static class RigidbodyExtensions
{
    // ===== Rigidbody2D =====
    
    // Change only horizontal velocity (preserve vertical for gravity)
    public static void SetVelocityX(this Rigidbody2D rb, float x)
    {
        rb.linearVelocity = new Vector2(x, rb.linearVelocity.y);
    }
    
    public static void SetVelocityY(this Rigidbody2D rb, float y)
    {
        rb.linearVelocity = new Vector2(rb.linearVelocity.x, y);
    }
    
    // Check if moving in a direction
    public static bool IsMovingRight(this Rigidbody2D rb) => rb.linearVelocity.x > 0.1f;
    public static bool IsMovingLeft(this Rigidbody2D rb) => rb.linearVelocity.x < -0.1f;
    public static bool IsFalling(this Rigidbody2D rb) => rb.linearVelocity.y < -0.1f;
    
    // ===== Rigidbody (3D) =====
    
    // Preserve Y velocity (gravity) when setting horizontal movement
    public static void SetHorizontalVelocity(this Rigidbody rb, Vector3 horizontal)
    {
        rb.linearVelocity = new Vector3(horizontal.x, rb.linearVelocity.y, horizontal.z);
    }
    
    public static bool IsFalling(this Rigidbody rb) => rb.linearVelocity.y < -0.5f;
    
    // Add force in a specific direction relative to the object
    public static void AddRelativeImpulse(this Rigidbody rb, Vector3 localForce)
    {
        rb.AddForce(rb.transform.TransformDirection(localForce), ForceMode.Impulse);
    }
}
```

---

## 20.7 — Color Extensions

```csharp
public static class ColorExtensions
{
    // Change alpha without creating a whole new Color
    public static Color WithAlpha(this Color c, float alpha) => new Color(c.r, c.g, c.b, alpha);
    
    // Lerp only the alpha
    public static Color LerpAlpha(this Color c, float targetAlpha, float t)
        => c.WithAlpha(Mathf.Lerp(c.a, targetAlpha, t));
    
    // Make color transparent
    public static Color Transparent(this Color c) => c.WithAlpha(0f);
    
    // Make color opaque
    public static Color Opaque(this Color c) => c.WithAlpha(1f);
    
    // Mix with another color
    public static Color Mix(this Color a, Color b, float t = 0.5f) => Color.Lerp(a, b, t);
    
    // To hex string (#RRGGBB)
    public static string ToHex(this Color c) => ColorUtility.ToHtmlStringRGB(c);
    
    // Adjust brightness
    public static Color Brighten(this Color c, float amount) 
        => new Color(Mathf.Clamp01(c.r + amount), Mathf.Clamp01(c.g + amount), Mathf.Clamp01(c.b + amount), c.a);
}

// Usage
spriteRenderer.color = spriteRenderer.color.WithAlpha(0.5f);    // make semi-transparent
healthBarColor = Color.green.Mix(Color.red, 1f - healthPercent); // blend based on health
```

---

## 20.8 — 🎮 2D vs 3D: Practical Extension Usage

### 🎮 2D Player using Extensions
```csharp
public class Player2D : MonoBehaviour
{
    private Rigidbody2D rb;
    private Transform enemy;
    
    void Update()
    {
        // Using Transform extension — no copy-modify-assign!
        if (shouldFlip)
            transform.SetX(-transform.position.x);
        
        // Using Rigidbody2D extension — clean velocity modification
        float inputX = Input.GetAxisRaw("Horizontal");
        rb.SetVelocityX(inputX * moveSpeed); // preserves gravity Y velocity!
        
        // Using Vector2 extension — rotate aim direction
        Vector2 aimDir = (Vector2)(enemy.position - transform.position).normalized;
        Vector2 predictedPos = aimDir.Rotate(15f); // lead the target
        
        // Using float extension
        float distToEnemy = Vector2.Distance(transform.position, enemy.position);
        bool inRange = distToEnemy.Between(0f, attackRange);
        float normalizedDist = distToEnemy.Remap(0f, detectionRange, 1f, 0f).Clamp01();
    }
}
```

### 🎮 3D Player using Extensions
```csharp
public class Player3D : MonoBehaviour
{
    private Rigidbody rb;
    private Transform target;
    
    void Update()
    {
        Vector3 moveInput = new Vector3(Input.GetAxisRaw("Horizontal"), 0f, Input.GetAxisRaw("Vertical"));
        
        // Using Rigidbody extension — preserve gravity Y velocity
        rb.SetHorizontalVelocity(moveInput.normalized * moveSpeed);
        
        // Using Vector3 extension — flat distance ignoring terrain height
        float flatDist = transform.position.FlatDistanceTo(target.position);
        bool canSee = flatDist.Between(0f, sightRange);
        
        // Using Vector3 extension — direction to target
        Vector3 dirToTarget = transform.position.DirectionTo(target.position);
        
        // Using float extension
        float speedRatio = rb.linearVelocity.Flat().magnitude.Remap(0f, runSpeed, 0f, 1f).Clamp01();
        animator.SetFloat("SpeedRatio", speedRatio);
    }
}
```

---

## 📝 Summary

| Extension Type | Key Methods | Use Case |
| :--- | :--- | :--- |
| `float` | `Remap`, `Between`, `Clamp`, `SmoothStep` | Math utilities |
| `Vector2` | `WithX/Y`, `Rotate`, `IsNearlyZero` | 2D position/direction |
| `Vector3` | `WithX/Y/Z`, `Flat`, `DirectionTo`, `FlatDistanceTo` | 3D position/direction |
| `Transform` | `SetX/Y/Z`, `GetChildren`, `LookAt2D`, `DestroyAllChildren` | Scene hierarchy |
| `GameObject` | `GetOrAddComponent`, `IsInLayerMask`, `SetLayerRecursively` | Object queries |
| `Rigidbody2D` | `SetVelocityX/Y`, `IsFalling` | 2D physics |
| `Rigidbody` | `SetHorizontalVelocity`, `AddRelativeImpulse` | 3D physics |
| `Color` | `WithAlpha`, `Mix`, `Brighten` | Visual effects |

**Previous:** [[19 — Modern C# Features]] | **Next:** [[21 — Static Classes & Utility Patterns]]
