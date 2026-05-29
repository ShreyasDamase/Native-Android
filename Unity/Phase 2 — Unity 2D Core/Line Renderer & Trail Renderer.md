# ✏️ Line Renderer & Trail Renderer
### Laser Beams, Trajectories, Rope Visuals, Sword Trails & Particle Paths

`LineRenderer` and `TrailRenderer` are two of the most-used components in physics games. They draw world-space polylines and motion trails without requiring meshes or sprites.

---

## 1. LineRenderer — Core Concepts

A `LineRenderer` draws a sequence of connected line segments through a set of world-space points. Each frame you can set these points to draw any shape: ropes, trajectories, laser beams, debug paths, or electrical arcs.

### Inspector Settings

```
LineRenderer Component:
├── Positions: [List of Vector3 world points]
├── Width Curve: Width of the line along its length (AnimationCurve)
├── Color Gradient: Color from start → end
├── Material: The material drawn on the line (use Sprites-Default or custom unlit)
├── Use World Space: ON = positions in world space, OFF = local to Transform
├── Loop: Connect the last point back to the first point (for closed shapes)
├── Alignment: View = always faces camera (billboard), Local = faces line direction
└── Texture Mode: Stretch / Tile / Repeat Along Direction
```

### Basic Setup in Code

```csharp
[RequireComponent(typeof(LineRenderer))]
public class LineRendererSetup : MonoBehaviour
{
    private LineRenderer lr;

    private void Awake()
    {
        lr = GetComponent<LineRenderer>();

        // ─── Configure once (do this in Awake, not Update) ────────────────
        lr.useWorldSpace = true;         // Points are in world space
        lr.startWidth = 0.05f;
        lr.endWidth = 0.05f;
        lr.positionCount = 2;            // How many points this line has
        lr.loop = false;
    }

    // Update the line to span from A to B every frame
    public void DrawLine(Vector3 from, Vector3 to)
    {
        lr.SetPosition(0, from);
        lr.SetPosition(1, to);
    }

    // Draw a multi-point path
    public void DrawPath(Vector3[] points)
    {
        lr.positionCount = points.Length;
        lr.SetPositions(points); // More efficient than calling SetPosition() in a loop
    }
}
```

### Efficient: Update Only Changed Points

```csharp
// Only change positions — do NOT recreate the LineRenderer or set positionCount every frame
// positionCount is expensive to change; set it ONCE and then only call SetPosition()
private void Update()
{
    // CORRECT: update positions without changing count
    lr.SetPosition(0, startTransform.position);
    lr.SetPosition(1, endTransform.position);

    // WRONG: changing positionCount every frame causes garbage
    // lr.positionCount = 2; ← DON'T do this in Update
}
```

---

## 2. Laser Beam with Raycast

```csharp
[RequireComponent(typeof(LineRenderer))]
public class LaserBeam : MonoBehaviour
{
    private LineRenderer lr;

    [SerializeField] private float maxRange = 50f;
    [SerializeField] private LayerMask hitLayers;
    [SerializeField] private GameObject hitSparkPrefab;

    private GameObject activeHitSpark;

    private void Awake()
    {
        lr = GetComponent<LineRenderer>();
        lr.positionCount = 2;

        // Create a hit spark effect (pooled in production)
        activeHitSpark = Instantiate(hitSparkPrefab);
        activeHitSpark.SetActive(false);
    }

    private void Update()
    {
        FireLaser();
    }

    private void FireLaser()
    {
        Vector2 origin = transform.position;
        Vector2 direction = transform.right; // Assumes laser points along local X axis

        lr.SetPosition(0, origin);

        RaycastHit2D hit = Physics2D.Raycast(origin, direction, maxRange, hitLayers);

        if (hit.collider != null)
        {
            // Hit something — draw to hit point
            lr.SetPosition(1, hit.point);

            // Show hit spark at impact point, aligned to surface normal
            activeHitSpark.SetActive(true);
            activeHitSpark.transform.position = hit.point;
            activeHitSpark.transform.up = hit.normal;

            // Apply damage, etc.
            hit.collider.GetComponent<IDamageable>()?.TakeDamage(1f * Time.deltaTime);
        }
        else
        {
            // No hit — draw to maximum range
            lr.SetPosition(1, origin + direction * maxRange);
            activeHitSpark.SetActive(false);
        }
    }

    // Enable / disable the laser
    public void SetActive(bool active)
    {
        lr.enabled = active;
        if (!active) activeHitSpark.SetActive(false);
    }
}
```

---

## 3. Rope Rendering (LineRenderer + Verlet Simulation)

Connect your Verlet rope simulation (from Advanced Physics notes) to a LineRenderer for realistic rope visuals.

```csharp
[RequireComponent(typeof(LineRenderer))]
public class RopeRenderer : MonoBehaviour
{
    private LineRenderer lr;
    [SerializeField] private VerletRope ropeSimulation; // Reference to rope physics script

    [Header("Visual Style")]
    [SerializeField] private float ropeWidth = 0.08f;
    [SerializeField] private Color ropeColor = new Color(0.6f, 0.4f, 0.2f);

    private void Awake()
    {
        lr = GetComponent<LineRenderer>();
        lr.startWidth = ropeWidth;
        lr.endWidth = ropeWidth;

        // Set a simple gradient
        var gradient = new Gradient();
        gradient.SetKeys(
            new GradientColorKey[] { new GradientColorKey(ropeColor, 0f), new GradientColorKey(ropeColor, 1f) },
            new GradientAlphaKey[] { new GradientAlphaKey(1f, 0f), new GradientAlphaKey(1f, 1f) }
        );
        lr.colorGradient = gradient;
    }

    // Called after VerletRope updates its particle positions in FixedUpdate
    private void LateUpdate()
    {
        var particles = ropeSimulation.GetParticlePositions();
        lr.positionCount = particles.Length;
        for (int i = 0; i < particles.Length; i++)
            lr.SetPosition(i, particles[i]);
    }
}
```

---

## 4. Projectile Trajectory Prediction

Show a dotted arc preview before launching a projectile (grenade, cannonball, grapple hook).

```csharp
[RequireComponent(typeof(LineRenderer))]
public class TrajectoryPredictor : MonoBehaviour
{
    private LineRenderer lr;

    [SerializeField] private int pointCount = 30;       // Resolution of the preview arc
    [SerializeField] private float timeStep = 0.1f;     // Time between prediction points

    private void Awake()
    {
        lr = GetComponent<LineRenderer>();
        lr.positionCount = pointCount;

        // Dotted line effect via texture tiling
        lr.textureMode = LineTextureMode.Tile;
    }

    // Call every frame while the player is aiming
    public void UpdateTrajectory(Vector2 launchPosition, Vector2 launchVelocity)
    {
        float gravity = Physics2D.gravity.y;

        for (int i = 0; i < pointCount; i++)
        {
            float t = i * timeStep;

            // Kinematic projectile motion (no drag)
            float x = launchPosition.x + launchVelocity.x * t;
            float y = launchPosition.y + launchVelocity.y * t + 0.5f * gravity * t * t;

            lr.SetPosition(i, new Vector3(x, y, 0));

            // Stop drawing if the arc hits the ground
            if (i > 0 && y < -100f) // Replace -100 with your ground Y
            {
                lr.positionCount = i;
                break;
            }
        }
    }

    // Also supports drawing with physics scene simulation (more accurate, includes obstacles)
    public void UpdateTrajectoryWithSimulation(Vector2 launchPos, Vector2 launchVelocity, LayerMask obstacleMask)
    {
        var points = new Vector3[pointCount];
        Vector2 pos = launchPos;
        Vector2 vel = launchVelocity;
        float dt = timeStep;
        float grav = Physics2D.gravity.y;

        for (int i = 0; i < pointCount; i++)
        {
            points[i] = new Vector3(pos.x, pos.y, 0);

            // Check for obstacle at this step
            var hit = Physics2D.Raycast(pos, vel.normalized, vel.magnitude * dt, obstacleMask);
            if (hit.collider != null)
            {
                // Bounce or stop the preview at hit point
                points[i] = hit.point;
                lr.positionCount = i + 1;
                lr.SetPositions(points);
                return;
            }

            // Integrate velocity and apply gravity
            vel.y += grav * dt;
            pos += vel * dt;
        }

        lr.positionCount = pointCount;
        lr.SetPositions(points);
    }

    public void ShowTrajectory(bool show) => lr.enabled = show;
}
```

---

## 5. Electrical Arc Effect (LineRenderer Noise)

```csharp
[RequireComponent(typeof(LineRenderer))]
public class ElectricalArc : MonoBehaviour
{
    private LineRenderer lr;

    [SerializeField] private Transform startPoint;
    [SerializeField] private Transform endPoint;
    [SerializeField] private int segmentCount = 12;
    [SerializeField] private float arcWidth = 0.5f;         // Max perpendicular offset
    [SerializeField] private float animationSpeed = 20f;    // How fast the arc shifts

    private float noiseOffset;

    private void Awake()
    {
        lr = GetComponent<LineRenderer>();
        lr.positionCount = segmentCount;
    }

    private void Update()
    {
        noiseOffset += Time.deltaTime * animationSpeed;
        GenerateArc();
    }

    private void GenerateArc()
    {
        Vector3 start = startPoint.position;
        Vector3 end = endPoint.position;
        Vector3 direction = end - start;
        // Perpendicular axis (for 2D, this is Z-up × direction)
        Vector3 perpendicular = new Vector3(-direction.y, direction.x, 0).normalized;

        for (int i = 0; i < segmentCount; i++)
        {
            float t = (float)i / (segmentCount - 1);
            Vector3 basePoint = Vector3.Lerp(start, end, t);

            // Apply Perlin noise perpendicular offset (sin wave to keep start/end clean)
            float envelope = Mathf.Sin(t * Mathf.PI); // 0 at endpoints, 1 at center
            float noiseVal = Mathf.PerlinNoise(t * 3f + noiseOffset, 0f) * 2f - 1f;
            basePoint += perpendicular * noiseVal * arcWidth * envelope;

            lr.SetPosition(i, basePoint);
        }
    }
}
```

---

## 6. TrailRenderer — Motion Trails

A `TrailRenderer` automatically records the path of a moving object and displays a fading tail.

### Key Properties

```csharp
trail.time           // How long (seconds) before the trail fades out
trail.startWidth     // Width at the leading edge
trail.endWidth       // Width at the fading tail (usually 0)
trail.minVertexDistance // Minimum distance the object must move before a new trail vertex is added
trail.autodestruct   // Destroy the GameObject when trail has fully faded
trail.emitting       // Toggle trail on/off (false stops emitting but keeps existing trail)
```

### Sword Slash / Weapon Trail

```csharp
[RequireComponent(typeof(TrailRenderer))]
public class WeaponTrail : MonoBehaviour
{
    private TrailRenderer trail;

    [SerializeField] private float trailDuration = 0.15f;
    [SerializeField] private float startWidth = 0.3f;

    private void Awake()
    {
        trail = GetComponent<TrailRenderer>();
        trail.time = trailDuration;
        trail.startWidth = startWidth;
        trail.endWidth = 0f;
        trail.emitting = false; // Off by default
    }

    // Called when attack animation starts
    public void StartTrail() => trail.emitting = true;

    // Called when attack animation ends — trail fades out naturally
    public void StopTrail() => trail.emitting = false;

    // Clear the trail instantly (between combo hits)
    public void ClearTrail() => trail.Clear();
}
```

### Bullet/Projectile Trail

```csharp
// Place a TrailRenderer on a bullet prefab
// The trail will automatically follow its movement and fade based on 'time'
// Configure in inspector:
//   time = 0.08f (very short for a bullet streak)
//   startWidth = 0.05f, endWidth = 0f
//   Color: bright white → transparent

// Key trick: Disable trail when bullet returns to pool!
public class PooledBulletWithTrail : MonoBehaviour
{
    private TrailRenderer trail;

    private void Awake() => trail = GetComponent<TrailRenderer>();

    private void OnEnable()
    {
        // Must clear the trail when re-used from pool!
        // Otherwise the trail draws from the previous position to the new spawn
        trail.Clear();
        trail.emitting = true;
    }

    private void OnDisable()
    {
        trail.emitting = false;
    }
}
```

---

## 7. Width Curves & Color Gradients (Custom Line Appearance)

```csharp
public class StyledLineRenderer : MonoBehaviour
{
    private LineRenderer lr;

    private void Awake()
    {
        lr = GetComponent<LineRenderer>();

        // ─── Custom Width Curve (tapered line) ──────────────────────────
        AnimationCurve widthCurve = new AnimationCurve();
        widthCurve.AddKey(0.0f, 0.1f);   // Start narrow
        widthCurve.AddKey(0.3f, 0.25f);  // Bulge in the middle
        widthCurve.AddKey(1.0f, 0.0f);   // Taper to nothing at end
        lr.widthCurve = widthCurve;
        lr.widthMultiplier = 1f;

        // ─── Custom Color Gradient ────────────────────────────────────────
        Gradient gradient = new Gradient();
        gradient.SetKeys(
            new GradientColorKey[]
            {
                new GradientColorKey(Color.cyan, 0.0f),
                new GradientColorKey(Color.blue, 0.5f),
                new GradientColorKey(Color.white, 1.0f)
            },
            new GradientAlphaKey[]
            {
                new GradientAlphaKey(1.0f, 0.0f),
                new GradientAlphaKey(0.5f, 0.8f),
                new GradientAlphaKey(0.0f, 1.0f)
            }
        );
        lr.colorGradient = gradient;
    }
}
```

---

## 8. Performance Rules

```
┌──────────────────────────────────────────────────────────────────────────┐
│               LINE / TRAIL RENDERER PERFORMANCE RULES                    │
├──────────────────────────────────────────────────────────────────────────┤
│ ✅ Set positionCount ONCE in Awake, not in Update                        │
│ ✅ Use SetPositions(array) instead of looped SetPosition() calls          │
│ ✅ Use lr.Simplify(tolerance) to reduce points on long paths              │
│ ✅ Set minVertexDistance on TrailRenderer to prevent dense geometry       │
│ ✅ Disable lr.enabled rather than destroying the component                │
│ ✅ Pool LineRenderer GameObjects — don't Instantiate/Destroy them         │
│ ✅ Call trail.Clear() before reusing a pooled object with TrailRenderer  │
│ ❌ Don't change positionCount every frame (rebuilds mesh)                │
│ ❌ Don't use worldPositionStays=true on SetParent if using world space   │
│ ❌ Avoid hundreds of LineRenderers simultaneously — batch into one       │
└──────────────────────────────────────────────────────────────────────────┘
```
