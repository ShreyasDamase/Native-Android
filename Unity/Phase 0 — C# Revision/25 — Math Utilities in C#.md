# 25 — Math Utilities in C#
### 🟡 Block E — 2D & 3D Unity-Specific C# Patterns

> [!NOTE]
> This is your **complete math cheat sheet for Unity game development**. Every common calculation — distance, angles, lerping, dot products, cross products — with plain-English explanations and game dev use cases. Bookmark this and refer to it constantly.

---

## 25.1 — `Mathf` — Unity's Math Library

```csharp
// ===== Basic Operations =====
Mathf.Abs(-5f)               // 5f — absolute value
Mathf.Sign(-5f)              // -1f — sign of a number (-1, 0, 1)
Mathf.Min(3f, 7f)            // 3f — minimum of two values
Mathf.Max(3f, 7f)            // 7f — maximum of two values
Mathf.Clamp(15f, 0f, 10f)   // 10f — clamp to [0, 10]
Mathf.Clamp01(1.5f)          // 1f — clamp to [0, 1]
Mathf.Sqrt(16f)              // 4f — square root
Mathf.Pow(2f, 10f)           // 1024f — 2 to the power of 10
Mathf.Log(100f, 10f)         // 2f — log base 10 of 100

// ===== Rounding =====
Mathf.Floor(3.7f)            // 3f — round down
Mathf.Ceil(3.2f)             // 4f — round up
Mathf.Round(3.5f)            // 4f — round to nearest (ties go up)
Mathf.FloorToInt(3.7f)       // 3 (int)
Mathf.CeilToInt(3.2f)        // 4 (int)
Mathf.RoundToInt(3.5f)       // 4 (int)

// ===== Trig =====
Mathf.Sin(Mathf.PI * 0.5f)  // 1f — sine of 90 degrees
Mathf.Cos(0f)                // 1f — cosine of 0 degrees
Mathf.Atan2(1f, 1f)          // π/4 — angle of direction (1,1) in radians

// ===== Constants =====
Mathf.PI           // 3.14159...
Mathf.Deg2Rad      // 0.01745... — multiply to convert degrees to radians
Mathf.Rad2Deg      // 57.2957... — multiply to convert radians to degrees
Mathf.Infinity     // float.MaxValue alias
float.Epsilon      // smallest non-zero float
```

---

## 25.2 — Lerp (Linear Interpolation) — The Most Used Math in Games

Lerp smoothly blends between two values based on a `t` parameter from 0 to 1.

```
Lerp(a, b, t):  a ──────────────── b
                0                   1
                        t=0.5 gives midpoint
```

```csharp
// ===== Float Lerp =====
float slow = Mathf.Lerp(0f, 100f, 0f);   // 0f (start)
float half = Mathf.Lerp(0f, 100f, 0.5f); // 50f (midpoint)
float end  = Mathf.Lerp(0f, 100f, 1f);   // 100f (end)

// Health bar filling animation
float displayedHealth = Mathf.Lerp(displayedHealth, targetHealth, 5f * Time.deltaTime);

// ===== Vector Lerp =====
Vector3 current = Vector3.Lerp(startPos, endPos, t);

// Smooth follow
transform.position = Vector3.Lerp(transform.position, target.position, 10f * Time.deltaTime);

// ===== Quaternion Lerp (Spherical) =====
// For rotations, always use Slerp (Spherical Lerp) — Lerp gives wrong results for rotation
transform.rotation = Quaternion.Slerp(startRot, endRot, t);
transform.rotation = Quaternion.Slerp(transform.rotation, targetRot, 10f * Time.deltaTime);

// ===== Color Lerp =====
Color blended = Color.Lerp(Color.red, Color.green, healthPercent);
spriteRenderer.color = Color.Lerp(spriteRenderer.color, targetColor, 5f * Time.deltaTime);

// ===== Lerp vs MoveTowards =====
// Lerp: gets closer but never exactly reaches destination (exponential decay)
pos = Mathf.Lerp(pos, target, speed * Time.deltaTime);

// MoveTowards: moves at a CONSTANT speed and stops exactly at destination
pos = Mathf.MoveTowards(pos, target, speed * Time.deltaTime);
// Vector3 version:
transform.position = Vector3.MoveTowards(transform.position, target.position, speed * Time.deltaTime);

// ===== LerpUnclamped (t can be outside 0-1) =====
// Use for overshoot/spring effects
float overshoot = Mathf.LerpUnclamped(0f, 100f, 1.2f); // 120f
```

---

## 25.3 — Easing Functions

```csharp
// SmoothStep — built-in ease in/out (S-curve)
float t = Mathf.SmoothStep(0f, 1f, rawT); // smooth 0→1 transition

// Manual easing functions (use in LerpUnclamped for advanced effects)
public static class Ease
{
    public static float InQuad(float t)    => t * t;
    public static float OutQuad(float t)   => 1f - (1f - t) * (1f - t);
    public static float InOutQuad(float t) => t < 0.5f ? 2f * t * t : 1f - Mathf.Pow(-2f * t + 2f, 2f) / 2f;
    
    public static float InCubic(float t)   => t * t * t;
    public static float OutCubic(float t)  => 1f - Mathf.Pow(1f - t, 3f);
    
    // Elastic — overshoots then settles
    public static float OutElastic(float t)
    {
        const float c4 = (2f * Mathf.PI) / 3f;
        return t == 0f ? 0f : t == 1f ? 1f :
            Mathf.Pow(2f, -10f * t) * Mathf.Sin((t * 10f - 0.75f) * c4) + 1f;
    }
    
    // Bounce — like dropping a ball
    public static float OutBounce(float t)
    {
        const float n1 = 7.5625f;
        const float d1 = 2.75f;
        if (t < 1f / d1) return n1 * t * t;
        if (t < 2f / d1) return n1 * (t -= 1.5f / d1) * t + 0.75f;
        if (t < 2.5f / d1) return n1 * (t -= 2.25f / d1) * t + 0.9375f;
        return n1 * (t -= 2.625f / d1) * t + 0.984375f;
    }
    
    // Back — overshoots slightly then comes back
    public static float OutBack(float t)
    {
        const float c1 = 1.70158f;
        const float c3 = c1 + 1f;
        return 1f + c3 * Mathf.Pow(t - 1f, 3f) + c1 * Mathf.Pow(t - 1f, 2f);
    }
}

// Usage — apply easing to any lerp
float easedT = Ease.OutElastic(rawT);
transform.position = Vector3.Lerp(startPos, endPos, easedT);
```

---

## 25.4 — Dot Product — "How Similar Are Two Directions?"

The dot product of two **normalized** vectors returns how aligned they are:
- `1.0` = same direction
- `0.0` = perpendicular
- `-1.0` = opposite direction

```csharp
// Dot product formula: a · b = ax*bx + ay*by + az*bz (equals cos(angle) for unit vectors)

// ===== Enemy FOV Detection =====
// Is the player in front of the enemy?
Vector3 toPlayer = (player.position - enemy.position).normalized;
float dot = Vector3.Dot(enemy.transform.forward, toPlayer);
bool playerAhead = dot > 0.5f;  // within 60° cone (cos(60°) = 0.5)
bool playerBehind = dot < 0f;   // behind (dot negative = angle > 90°)

// ===== Landing Detection =====
// Is the slope gentle enough to land on?
Vector3 surfaceNormal = hit.normal;
float slopeAlignment = Vector3.Dot(surfaceNormal, Vector3.up);
bool canLand = slopeAlignment > 0.7f; // less than ~45 degree slope

// ===== Same Direction Check =====
float moveDot = Vector2.Dot(rb.linearVelocity.normalized, inputDirection);
bool movingForward = moveDot > 0.3f;

// ===== Projected Speed (how fast along a direction) =====
// How fast is the player moving forward?
float forwardSpeed = Vector3.Dot(rb.linearVelocity, transform.forward);
float rightSpeed = Vector3.Dot(rb.linearVelocity, transform.right);

// ===== Damage Based on Hit Angle =====
// More damage if hit head-on, less if glancing blow
float impactDot = Mathf.Abs(Vector3.Dot(impactDirection, surfaceNormal));
float finalDamage = baseDamage * impactDot;
```

---

## 25.5 — Distance & Direction

```csharp
// ===== Distance =====
// Standard distance (uses square root — slower)
float dist = Vector3.Distance(a, b);               // sqrt((b-a)²)

// Squared distance (NO square root — faster for comparison!)
float distSq = (b - a).sqrMagnitude;
float rangeChecking = 5f * 5f;  // compare against squared value

if (distSq < rangeChecking)     // "within 5 units" — no sqrt needed
{
    // This runs 20-30% faster than comparing Distance
}

// ===== Direction =====
Vector3 fromAtoB = (b - a).normalized; // unit vector pointing from a to b

// ===== Practical Examples =====
void CheckIfInRange(Transform target, float range)
{
    // Cheap distance check — no sqrt
    float rangeSq = range * range;
    if ((target.position - transform.position).sqrMagnitude < rangeSq)
    {
        // In range
    }
}

// 2D angle to a target
float AngleTo2D(Transform target)
{
    Vector2 dir = ((Vector2)target.position - (Vector2)transform.position).normalized;
    return Mathf.Atan2(dir.y, dir.x) * Mathf.Rad2Deg;
}

// Signed angle (positive = left, negative = right)
float SignedAngleTo(Vector3 target)
{
    Vector3 dir = target - transform.position;
    return Vector3.SignedAngle(transform.forward, dir, Vector3.up);
}
```

---

## 25.6 — Cross Product (3D Only)

The cross product of two vectors gives a vector **perpendicular to both**. Used to find normals, determine left/right, and calculate rotation axes.

```csharp
// Cross product: a × b = vector perpendicular to both a and b
// The direction follows the right-hand rule

// Find the "up" direction for a surface
Vector3 surfaceNormal = Vector3.Cross(tangent, bitangent).normalized;

// Is the player to the LEFT or RIGHT of the enemy?
Vector3 toPlayer = (player.position - transform.position).normalized;
Vector3 cross = Vector3.Cross(transform.forward, toPlayer);
float side = cross.y; // positive = player is to the RIGHT, negative = LEFT

// Generate a perpendicular vector in 2D
// (rotate 90 degrees)
Vector2 perpendicular = new Vector2(-direction.y, direction.x);  // rotate left 90°
Vector2 perpendicularR = new Vector2(direction.y, -direction.x); // rotate right 90°

// Wall normal in 2D (which way to push character off wall)
Vector2 wallNormal = new Vector2(-wallDirection.y, wallDirection.x);
```

---

## 25.7 — Random Numbers

```csharp
// ===== Unity's Random =====
float r1 = Random.value;               // 0.0 to 1.0 (inclusive)
float r2 = Random.Range(0f, 10f);      // 0.0 to 10.0 (inclusive of both)
int r3   = Random.Range(0, 10);        // 0 to 9 (MAX IS EXCLUSIVE for int!)

// Random boolean
bool coin = Random.value > 0.5f;

// Random chance
bool critHit = Random.value < critChance; // critChance = 0.2f = 20% chance

// Random inside circle (2D)
Vector2 inCircle = Random.insideUnitCircle * radius;

// Random on circle circumference (2D)
Vector2 onCircle = Random.insideUnitCircle.normalized * radius;

// Random inside sphere (3D)
Vector3 inSphere = Random.insideSphere * radius; // NOT exposed directly — use:
Vector3 inSphereManual = Random.onUnitSphere * Random.value * radius;

// Random direction (3D)
Vector3 randomDir = Random.onUnitSphere;

// Seeded random (for reproducible generation)
Random.InitState(42); // seed — same seed always gives same sequence

// Weighted random selection
float[] weights = { 1f, 2f, 5f };  // third item is 5x more likely
ItemType RandomWeighted(float[] w)
{
    float total = 0f;
    foreach (float wt in w) total += wt;
    
    float rand = Random.Range(0f, total);
    float cumulative = 0f;
    
    for (int i = 0; i < w.Length; i++)
    {
        cumulative += w[i];
        if (rand < cumulative) return (ItemType)i;
    }
    
    return (ItemType)(w.Length - 1);
}
```

---

## 25.8 — Remap & Range Calculations

```csharp
// Remap — convert a value from one range to another
// Usage: health percentage → health bar width
public static float Remap(float value, float inMin, float inMax, float outMin, float outMax)
{
    return outMin + (value - inMin) * (outMax - outMin) / (inMax - inMin);
}

// Examples
float barWidth = Remap(currentHP, 0f, maxHP, 0f, 200f);         // HP → bar pixels
float soundVol = Remap(distToSource, 0f, maxRange, 1f, 0f);     // dist → volume
float pitchVal = Remap(playerSpeed, 0f, maxSpeed, 0.8f, 1.2f);  // speed → engine pitch

// Normalize — special case of remap (output 0-1)
float normalized = Mathf.InverseLerp(minValue, maxValue, currentValue);
// InverseLerp(0, 100, 75) = 0.75 — tells you WHERE 75 falls between 0 and 100

// Ping Pong — oscillates between 0 and length
// Value goes 0→1→0→1→0 as t increases
float pingPong = Mathf.PingPong(Time.time * speed, 1f);
transform.localScale = Vector3.one * Mathf.Lerp(minScale, maxScale, pingPong);

// Repeat — wraps value in range (like modulo for floats)
float looped = Mathf.Repeat(Time.time * speed, 1f); // always 0 to 1
```

---

## 25.9 — 🎮 2D vs 3D: Math Differences

### 🎮 2D Math Patterns
```csharp
public class MathHelper2D
{
    // Get angle from a 2D direction vector (in degrees)
    public static float DirectionToAngle(Vector2 direction)
        => Mathf.Atan2(direction.y, direction.x) * Mathf.Rad2Deg;
    
    // Get direction from an angle (in degrees)
    public static Vector2 AngleToDirection(float angleDeg)
    {
        float rad = angleDeg * Mathf.Deg2Rad;
        return new Vector2(Mathf.Cos(rad), Mathf.Sin(rad));
    }
    
    // Rotate a 2D vector by degrees (no Quaternion needed in 2D!)
    public static Vector2 RotateVector(Vector2 v, float degrees)
    {
        float rad = degrees * Mathf.Deg2Rad;
        float cos = Mathf.Cos(rad);
        float sin = Mathf.Sin(rad);
        return new Vector2(cos * v.x - sin * v.y, sin * v.x + cos * v.y);
    }
    
    // Is point inside circle?
    public static bool PointInCircle(Vector2 point, Vector2 center, float radius)
        => (point - center).sqrMagnitude <= radius * radius;
    
    // Is point inside rect?
    public static bool PointInRect(Vector2 point, Vector2 rectCenter, Vector2 rectSize)
    {
        Vector2 half = rectSize * 0.5f;
        return point.x >= rectCenter.x - half.x && point.x <= rectCenter.x + half.x &&
               point.y >= rectCenter.y - half.y && point.y <= rectCenter.y + half.y;
    }
    
    // Reflect a direction off a surface (for bouncing projectiles)
    public static Vector2 Reflect2D(Vector2 direction, Vector2 normal)
        => direction - 2f * Vector2.Dot(direction, normal) * normal;
    
    // Get orbit position (circular path)
    public static Vector2 OrbitPoint(Vector2 center, float radius, float angleDeg)
    {
        float rad = angleDeg * Mathf.Deg2Rad;
        return center + new Vector2(Mathf.Cos(rad), Mathf.Sin(rad)) * radius;
    }
}
```

### 🎮 3D Math Patterns
```csharp
public class MathHelper3D
{
    // Project movement onto ground plane (remove Y)
    public static Vector3 FlattenToGround(Vector3 v) => new Vector3(v.x, 0f, v.z);
    
    // Get the slope angle of a surface (0 = flat, 90 = wall)
    public static float GetSlopeAngle(Vector3 normal)
        => Vector3.Angle(normal, Vector3.up);
    
    // Is the slope walkable?
    public static bool IsWalkableSlope(Vector3 normal, float maxAngle = 45f)
        => GetSlopeAngle(normal) <= maxAngle;
    
    // Get movement direction relative to camera (for third-person)
    public static Vector3 GetCameraRelativeDirection(Vector2 input, Transform camera)
    {
        Vector3 forward = camera.forward;
        Vector3 right = camera.right;
        forward.y = 0f;
        right.y = 0f;
        forward.Normalize();
        right.Normalize();
        return (forward * input.y + right * input.x).normalized;
    }
    
    // Closest point on a line to another point
    public static Vector3 ClosestPointOnLine(Vector3 lineStart, Vector3 lineEnd, Vector3 point)
    {
        Vector3 lineDir = (lineEnd - lineStart).normalized;
        float t = Vector3.Dot(point - lineStart, lineDir);
        t = Mathf.Clamp(t, 0f, Vector3.Distance(lineStart, lineEnd));
        return lineStart + lineDir * t;
    }
    
    // Predict where a moving target will be after 'time' seconds
    public static Vector3 PredictPosition(Vector3 position, Vector3 velocity, float time)
        => position + velocity * time;
    
    // Lead-target calculation for projectiles
    public static Vector3 CalculateLeadTarget(Vector3 shooterPos, Vector3 targetPos, Vector3 targetVel, float projectileSpeed)
    {
        Vector3 toTarget = targetPos - shooterPos;
        float dist = toTarget.magnitude;
        float travelTime = dist / projectileSpeed;
        return targetPos + targetVel * travelTime; // simple first-order prediction
    }
}
```

---

## 25.10 — Sine Wave Patterns (Animation & Procedural)

```csharp
// Sine wave oscillation — back and forth smoothly
public class SineWavePatterns : MonoBehaviour
{
    [SerializeField] private float frequency = 1f;  // cycles per second
    [SerializeField] private float amplitude = 1f;  // peak displacement
    
    Vector3 startPos;
    
    void Start() => startPos = transform.position;
    
    void Update()
    {
        // Basic oscillation
        float wave = Mathf.Sin(Time.time * frequency * Mathf.PI * 2f);  // -1 to 1
        float wave01 = (wave + 1f) * 0.5f;                               // 0 to 1
        
        // Float up and down
        float offsetY = wave * amplitude;
        transform.position = startPos + Vector3.up * offsetY;
        
        // Pulsing scale
        float scale = 1f + wave * 0.2f;
        transform.localScale = Vector3.one * scale;
        
        // Breathing effect (sin for smooth, always positive)
        float breath = Mathf.Abs(Mathf.Sin(Time.time * 0.5f));
        
        // Circular orbit (combine sin and cos)
        float orbitRadius = 3f;
        float orbitX = Mathf.Cos(Time.time * frequency) * orbitRadius;
        float orbitY = Mathf.Sin(Time.time * frequency) * orbitRadius;
        transform.position = startPos + new Vector3(orbitX, orbitY, 0f);
        
        // Figure-8 (Lissajous)
        float figure8X = Mathf.Sin(Time.time) * orbitRadius;
        float figure8Y = Mathf.Sin(Time.time * 2f) * orbitRadius * 0.5f;
        transform.position = startPos + new Vector3(figure8X, figure8Y, 0f);
    }
}
```

---

## 📝 Math Quick Reference Card

| Operation | Code | Use Case |
| :--- | :--- | :--- |
| Lerp | `Mathf.Lerp(a, b, t)` | Smooth transitions |
| Smooth lerp | `Vector3.Lerp(a, b, speed * dt)` | Follow, spring |
| Move at speed | `Vector3.MoveTowards(a, b, speed * dt)` | Constant movement |
| Slerp (rotation) | `Quaternion.Slerp(a, b, t)` | Smooth rotation |
| Clamp | `Mathf.Clamp(v, min, max)` | Health, speed limits |
| Normalize 0-1 | `Mathf.InverseLerp(min, max, v)` | Percentage |
| Remap | Custom or extension method | Any range conversion |
| Dot product | `Vector3.Dot(a, b)` | FOV, alignment |
| Cross product | `Vector3.Cross(a, b)` | Normals, left/right |
| Distance | `(b-a).sqrMagnitude < r*r` | Fast range check |
| Angle | `Vector3.Angle(a, b)` | Slope, FOV |
| Signed angle | `Vector3.SignedAngle(a, b, up)` | Left vs right |
| Random range | `Random.Range(min, max)` | Float: inclusive, Int: max exclusive |
| Sine wave | `Mathf.Sin(Time.time * freq)` | Oscillation, animation |
| PingPong | `Mathf.PingPong(t, 1f)` | Back-and-forth |

**Previous:** [[24 — C# Patterns for 3D Unity]] | **You've completed the C# learning vault! 🎉**

---

*Start building now → [[00 — Index & Learning Path|Return to Index]]*
