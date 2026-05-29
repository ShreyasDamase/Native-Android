# 📐 Advanced Game Math
### Bezier Curves, Splines, Matrices, Noise, Easing Functions & Intersection Tests

When building a large physics-based game, raw vector math is not enough. This guide covers the advanced mathematical toolkit used in professional-grade gameplay and simulation code.

---

## 1. Bezier Curves

A Bezier curve is a smooth path defined by **control points**. It is used for curved movement paths, bullet trajectories, UI animations, and procedural generation.

### Linear Bezier (2 points — same as Lerp)
$$B(t) = (1-t) \cdot P_0 + t \cdot P_1$$

### Quadratic Bezier (3 points — one curve)
$$B(t) = (1-t)^2 \cdot P_0 + 2(1-t)t \cdot P_1 + t^2 \cdot P_2$$

### Cubic Bezier (4 points — two smooth curves)
$$B(t) = (1-t)^3 P_0 + 3(1-t)^2 t P_1 + 3(1-t)t^2 P_2 + t^3 P_3$$

```csharp
public static class BezierMath
{
    // Quadratic Bezier: 3 control points
    // t = 0.0 → start, t = 1.0 → end, t = 0.5 → midpoint on curve
    public static Vector2 Quadratic(Vector2 p0, Vector2 p1, Vector2 p2, float t)
    {
        float u = 1f - t;
        return u * u * p0 + 2f * u * t * p1 + t * t * p2;
    }

    // Cubic Bezier: 4 control points (used in CSS, Adobe Illustrator, Unity animation)
    public static Vector2 Cubic(Vector2 p0, Vector2 p1, Vector2 p2, Vector2 p3, float t)
    {
        float u = 1f - t;
        return u * u * u * p0
             + 3f * u * u * t * p1
             + 3f * u * t * t * p2
             + t * t * t * p3;
    }

    // First derivative of cubic Bezier — gives the TANGENT direction at parameter t
    // Used to orient a projectile or enemy along the curve path
    public static Vector2 CubicTangent(Vector2 p0, Vector2 p1, Vector2 p2, Vector2 p3, float t)
    {
        float u = 1f - t;
        return 3f * u * u * (p1 - p0)
             + 6f * u * t  * (p2 - p1)
             + 3f * t * t  * (p3 - p2);
    }
}

// Move an object along a cubic Bezier path
public class BezierMover : MonoBehaviour
{
    [SerializeField] private Transform p0, p1, p2, p3; // Control point transforms
    [SerializeField] private float duration = 3f;
    [SerializeField] private AnimationCurve speedCurve = AnimationCurve.EaseInOut(0, 0, 1, 1);

    private float elapsed;

    private void Update()
    {
        elapsed += Time.deltaTime;
        float t = Mathf.Clamp01(elapsed / duration);

        // Optionally remap t through a speed curve for easing
        float easedT = speedCurve.Evaluate(t);

        Vector2 pos = BezierMath.Cubic(p0.position, p1.position, p2.position, p3.position, easedT);
        Vector2 tangent = BezierMath.CubicTangent(p0.position, p1.position, p2.position, p3.position, easedT);

        transform.position = pos;

        // Align sprite rotation to travel direction
        if (tangent.sqrMagnitude > 0.001f)
        {
            float angle = Mathf.Atan2(tangent.y, tangent.x) * Mathf.Rad2Deg;
            transform.rotation = Quaternion.Euler(0, 0, angle);
        }
    }
}
```

---

## 2. Catmull-Rom Splines (Smooth Path Through All Points)

Unlike Bezier curves, Catmull-Rom splines pass **through** all control points. Perfect for camera paths, patrol routes, or procedural roads.

```csharp
// Catmull-Rom interpolation between p1 and p2
// p0 and p3 are the neighboring points used only to compute curve shape
public static Vector2 CatmullRom(Vector2 p0, Vector2 p1, Vector2 p2, Vector2 p3, float t)
{
    float t2 = t * t;
    float t3 = t2 * t;

    return 0.5f * (
        (2f * p1) +
        (-p0 + p2) * t +
        (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2 +
        (-p0 + 3f * p1 - 3f * p2 + p3) * t3
    );
}

// Walk along a list of waypoints using Catmull-Rom
public static Vector2 EvaluateSpline(Vector2[] waypoints, float globalT)
{
    int count = waypoints.Length;
    float scaledT = globalT * (count - 1);
    int segment = Mathf.Clamp((int)scaledT, 0, count - 2);
    float localT = scaledT - segment;

    Vector2 p0 = waypoints[Mathf.Max(segment - 1, 0)];
    Vector2 p1 = waypoints[segment];
    Vector2 p2 = waypoints[Mathf.Min(segment + 1, count - 1)];
    Vector2 p3 = waypoints[Mathf.Min(segment + 2, count - 1)];

    return CatmullRom(p0, p1, p2, p3, localT);
}
```

---

## 3. Matrix Transformations (2D TRS Matrix)

A **TRS matrix** (Translation × Rotation × Scale) is the mathematical engine behind `Transform`. Understanding matrices helps you manually compute world positions of bones, weapons, UI anchors, and camera viewports.

### 2D Homogeneous Transformation Matrix

```
[cos θ  -sin θ  tx]
[sin θ   cos θ  ty]
[0       0       1]
```

Where `tx/ty` = translation, `θ` = rotation, and scaling is applied separately.

```csharp
// Manually build a 2D TRS Matrix3x3 and apply it to a point
public struct Matrix3x3
{
    public float m00, m01, m02;
    public float m10, m11, m12;
    public float m20, m21, m22;

    // Build a 2D Transform matrix: Translate → Rotate → Scale
    public static Matrix3x3 TRS(Vector2 translation, float angleDegrees, Vector2 scale)
    {
        float rad = angleDegrees * Mathf.Deg2Rad;
        float cos = Mathf.Cos(rad);
        float sin = Mathf.Sin(rad);

        return new Matrix3x3
        {
            m00 = cos * scale.x,  m01 = -sin * scale.y,  m02 = translation.x,
            m10 = sin * scale.x,  m11 =  cos * scale.y,  m12 = translation.y,
            m20 = 0,              m21 = 0,                m22 = 1
        };
    }

    // Transform a 2D point through this matrix
    public Vector2 MultiplyPoint(Vector2 point)
    {
        return new Vector2(
            m00 * point.x + m01 * point.y + m02,
            m10 * point.x + m11 * point.y + m12
        );
    }

    // Matrix multiplication — chain parent and child transforms
    public static Matrix3x3 operator *(Matrix3x3 a, Matrix3x3 b)
    {
        return new Matrix3x3
        {
            m00 = a.m00 * b.m00 + a.m01 * b.m10,
            m01 = a.m00 * b.m01 + a.m01 * b.m11,
            m02 = a.m00 * b.m02 + a.m01 * b.m12 + a.m02,

            m10 = a.m10 * b.m00 + a.m11 * b.m10,
            m11 = a.m10 * b.m01 + a.m11 * b.m11,
            m12 = a.m10 * b.m02 + a.m11 * b.m12 + a.m12,

            m20 = 0, m21 = 0, m22 = 1
        };
    }
}
```

---

## 4. Noise Functions (Procedural Generation)

Noise generates smooth, natural-looking random values — essential for terrain, animations, particle jitter, and camera shake.

### Perlin Noise vs. Value Noise

```
Value Noise:  Interpolate between random grid values. Fast, blocky result.
Perlin Noise: Interpolate between random GRADIENT vectors. Smoother, more organic.
Simplex Noise: Improved Perlin — fewer visual artifacts, faster in N dimensions.
```

### Unity Built-in: `Mathf.PerlinNoise(x, y)`

```csharp
// Generate a 1D noise wave (camera shake, procedural animation)
public class NoiseExamples : MonoBehaviour
{
    [SerializeField] private float noiseScale = 1.5f;
    [SerializeField] private float noiseSpeed = 2f;

    // Perlin noise returns a value in [0, 1] — often remapped to [-1, 1]
    public float GetNoise1D(float time)
    {
        float raw = Mathf.PerlinNoise(time * noiseSpeed, 0f);
        return raw * 2f - 1f; // Remap [0,1] → [-1, 1]
    }

    // 2D noise: Procedural terrain height or texture variation
    public float GetTerrainHeight(float worldX, float worldZ)
    {
        float nx = worldX / noiseScale;
        float nz = worldZ / noiseScale;
        return Mathf.PerlinNoise(nx, nz) * 10f; // Scale to world height
    }

    // Layered (Fractal / Octave) Noise for natural-looking terrain
    // Each 'octave' adds finer detail at higher frequency and lower amplitude
    public float FractalNoise(float x, float y, int octaves = 4, float persistence = 0.5f, float lacunarity = 2f)
    {
        float result = 0f;
        float amplitude = 1f;
        float frequency = 1f;
        float maxValue = 0f;

        for (int i = 0; i < octaves; i++)
        {
            result   += Mathf.PerlinNoise(x * frequency, y * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence; // Each octave contributes less
            frequency *= lacunarity;  // Each octave is at higher resolution
        }

        return result / maxValue; // Normalize to [0, 1]
    }
}
```

### Camera Shake Using Noise

```csharp
public class CameraShake : MonoBehaviour
{
    private float shakeIntensity;
    private float shakeDuration;
    private float shakeTimer;
    private float noiseOffsetX;
    private float noiseOffsetY;

    public void Shake(float intensity, float duration)
    {
        shakeIntensity = intensity;
        shakeDuration = duration;
        shakeTimer = 0f;
        // Random noise sample offset per shake to avoid repetition
        noiseOffsetX = Random.Range(0f, 1000f);
        noiseOffsetY = Random.Range(0f, 1000f);
    }

    private void LateUpdate()
    {
        if (shakeTimer >= shakeDuration) return;

        shakeTimer += Time.deltaTime;
        float progress = shakeTimer / shakeDuration;

        // Perlin noise gives smooth shake. Lerp intensity to zero as timer ends.
        float currentIntensity = shakeIntensity * (1f - progress);
        float t = shakeTimer * 5f; // Noise speed

        float offsetX = (Mathf.PerlinNoise(noiseOffsetX + t, 0f) * 2f - 1f) * currentIntensity;
        float offsetY = (Mathf.PerlinNoise(noiseOffsetY + t, 0f) * 2f - 1f) * currentIntensity;

        transform.localPosition = new Vector3(offsetX, offsetY, transform.localPosition.z);
    }
}
```

---

## 5. Easing Functions

Easing functions remap a linear `t` value to a more natural curve. They are the backbone of professional UI animations, object movement, and physics feel.

```csharp
public static class Easing
{
    // ─── IN: slow start, fast end ───
    public static float EaseInQuad(float t)   => t * t;
    public static float EaseInCubic(float t)  => t * t * t;
    public static float EaseInQuart(float t)  => t * t * t * t;
    public static float EaseInExpo(float t)   => t == 0 ? 0 : Mathf.Pow(2, 10 * t - 10);
    public static float EaseInCirc(float t)   => 1 - Mathf.Sqrt(1 - t * t);

    // ─── OUT: fast start, slow end ───
    public static float EaseOutQuad(float t)  => 1 - (1 - t) * (1 - t);
    public static float EaseOutCubic(float t) { t--; return t * t * t + 1; }
    public static float EaseOutExpo(float t)  => t == 1 ? 1 : 1 - Mathf.Pow(2, -10 * t);
    public static float EaseOutBounce(float t)
    {
        const float n1 = 7.5625f, d1 = 2.75f;
        if (t < 1 / d1)        return n1 * t * t;
        else if (t < 2 / d1) { t -= 1.5f / d1;   return n1 * t * t + 0.75f; }
        else if (t < 2.5 / d1){ t -= 2.25f / d1; return n1 * t * t + 0.9375f; }
        else                  { t -= 2.625f / d1; return n1 * t * t + 0.984375f; }
    }

    // ─── IN-OUT: slow start, fast middle, slow end ───
    public static float EaseInOutCubic(float t)
        => t < 0.5f ? 4 * t * t * t : 1 - Mathf.Pow(-2 * t + 2, 3) / 2;

    public static float EaseInOutExpo(float t)
    {
        if (t == 0 || t == 1) return t;
        return t < 0.5f
            ? Mathf.Pow(2, 20 * t - 10) / 2
            : (2 - Mathf.Pow(2, -20 * t + 10)) / 2;
    }

    // Elastic: overshoot and spring back
    public static float EaseOutElastic(float t)
    {
        const float c4 = (2 * Mathf.PI) / 3f;
        if (t == 0 || t == 1) return t;
        return Mathf.Pow(2, -10 * t) * Mathf.Sin((t * 10 - 0.75f) * c4) + 1;
    }

    // Back: overshoot past target, then settle
    public static float EaseOutBack(float t)
    {
        const float c1 = 1.70158f, c3 = c1 + 1;
        return 1 + c3 * Mathf.Pow(t - 1, 3) + c1 * Mathf.Pow(t - 1, 2);
    }
}
```

---

## 6. Line / Ray Intersection Mathematics

### Line Segment Intersection Test

```csharp
// Returns true if segment AB intersects segment CD, and outputs the intersection point
public static bool LineSegmentIntersect(Vector2 a, Vector2 b, Vector2 c, Vector2 d, out Vector2 intersection)
{
    intersection = Vector2.zero;

    Vector2 ab = b - a;
    Vector2 cd = d - c;
    float denominator = ab.x * cd.y - ab.y * cd.x;

    // Parallel lines (denominator = 0) never intersect
    if (Mathf.Abs(denominator) < 0.0001f) return false;

    Vector2 ac = c - a;
    float t = (ac.x * cd.y - ac.y * cd.x) / denominator;
    float u = (ac.x * ab.y - ac.y * ab.x) / denominator;

    // t and u must both be in [0, 1] for segments (not full lines) to intersect
    if (t < 0 || t > 1 || u < 0 || u > 1) return false;

    intersection = a + t * ab;
    return true;
}
```

### Circle Intersection Test

```csharp
// Returns true if a ray hits a circle
public static bool RayHitsCircle(Vector2 rayOrigin, Vector2 rayDir, Vector2 circleCenter, float radius, out float distance)
{
    distance = 0f;
    Vector2 oc = rayOrigin - circleCenter;
    float b = Vector2.Dot(oc, rayDir);
    float c = Vector2.Dot(oc, oc) - radius * radius;
    float discriminant = b * b - c;

    if (discriminant < 0) return false; // Ray misses circle

    distance = -b - Mathf.Sqrt(discriminant); // Distance to nearest intersection
    return distance >= 0;
}
```

---

## 7. Quaternion Slerp & Rotation Utilities

### Why Quaternions, Not Euler Angles?

Euler angles suffer from **Gimbal Lock** (when two rotation axes align, one degree of freedom is lost). Quaternions represent rotations in 4D space and avoid this entirely.

```csharp
// Smoothly rotate toward a target direction using Slerp
public class RotateTowardTarget : MonoBehaviour
{
    [SerializeField] private Transform target;
    [SerializeField] private float rotationSpeed = 180f; // degrees per second

    private void Update()
    {
        Vector2 dir = ((Vector2)target.position - (Vector2)transform.position).normalized;
        float targetAngle = Mathf.Atan2(dir.y, dir.x) * Mathf.Rad2Deg - 90f;

        Quaternion targetRotation = Quaternion.Euler(0, 0, targetAngle);

        // RotateTowards prevents over-rotation by clamping to maxDegreesDelta per frame
        transform.rotation = Quaternion.RotateTowards(
            transform.rotation,
            targetRotation,
            rotationSpeed * Time.deltaTime
        );
    }
}
```

---

## 8. AABB vs. Circle Collision — Manual Detection

```csharp
// Axis-Aligned Bounding Box vs. Circle intersection
public static bool AABBvsCircle(Bounds box, Vector2 circleCenter, float radius)
{
    // Find the closest point on the AABB to the circle center
    float closestX = Mathf.Clamp(circleCenter.x, box.min.x, box.max.x);
    float closestY = Mathf.Clamp(circleCenter.y, box.min.y, box.max.y);

    // Check if the closest point is within the circle radius
    float dx = circleCenter.x - closestX;
    float dy = circleCenter.y - closestY;
    return (dx * dx + dy * dy) <= (radius * radius); // Squared comparison: no sqrt
}

// Check if a point is inside a convex polygon (Winding Number algorithm)
public static bool PointInConvexPolygon(Vector2 point, Vector2[] vertices)
{
    int count = vertices.Length;
    int winding = 0;

    for (int i = 0; i < count; i++)
    {
        Vector2 v1 = vertices[i];
        Vector2 v2 = vertices[(i + 1) % count];

        if (v1.y <= point.y)
        {
            if (v2.y > point.y)
            {
                Vector3 cross = Vector3.Cross(v2 - v1, point - v1);
                if (cross.z > 0) winding++;
            }
        }
        else
        {
            if (v2.y <= point.y)
            {
                Vector3 cross = Vector3.Cross(v2 - v1, point - v1);
                if (cross.z < 0) winding--;
            }
        }
    }

    return winding != 0;
}
```
