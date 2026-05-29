# 💡 2D Lighting & Universal Render Pipeline (URP)
### Point Lights, Global Light, Shadow Casters, Normal Maps & Light-Based Gameplay

2D Lighting transforms a flat sprite game into a rich, atmospheric world. Under Unity's Universal Render Pipeline (URP), you get hardware-accelerated real-time 2D lights that interact with sprite normal maps, cast dynamic shadows, and can be scripted for gameplay mechanics.

---

## 1. Setting Up URP for 2D Lighting

### Requirements
- Project must use **Universal Render Pipeline** (URP)
- The **2D Renderer** must be selected in the URP Renderer Asset (not Forward Renderer)
- Sprites need a **lit material** (the `Sprite-Lit-Default` material, or a custom URP 2D Lit shader)

### Quick Setup Checklist

```
1. Install URP via Package Manager (Packages → Unity Registry → Universal RP)
2. Create URP Asset: Assets → Create → Rendering → URP Asset
3. Assign URP Asset: Project Settings → Graphics → Scriptable Render Pipeline Settings
4. Create 2D Renderer: Assets → Create → Rendering → URP 2D Renderer Data
5. Assign 2D Renderer in the URP Asset → Renderer List
6. Switch sprite materials from "Sprites-Default" to "Sprites-Lit-Default"
```

---

## 2. Light Types Reference

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        URP 2D LIGHT TYPES                                │
├──────────────────────────────────────────────────────────────────────────┤
│ Global Light 2D    │ Ambient fill light for the whole scene               │
│ Point Light 2D     │ Circle of light radiating from a point (torch, lamp) │
│ Spot Light 2D      │ Cone of light in a direction (flashlight, spotlight) │
│ Freeform Light 2D  │ Custom polygon shape of light                        │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Global Light 2D (Ambient / Darkness Level)

The `Global Light 2D` sets the baseline ambient light for the entire scene. Use it to simulate time of day, indoor/outdoor brightness, or dungeon darkness.

```csharp
using UnityEngine.Rendering.Universal;

public class DayNightCycle : MonoBehaviour
{
    [SerializeField] private Light2D globalLight;
    [SerializeField] private Gradient dayNightGradient; // Configure in Inspector
    [SerializeField] private float cycleDuration = 120f; // Full day in seconds

    private float timeOfDay; // 0 = midnight, 0.5 = noon, 1 = midnight again

    private void Update()
    {
        timeOfDay = (timeOfDay + Time.deltaTime / cycleDuration) % 1f;

        // Sample gradient to get current sky color
        globalLight.color = dayNightGradient.Evaluate(timeOfDay);

        // Scale brightness (1 = full day, 0 = pitch black night)
        globalLight.intensity = Mathf.Sin(timeOfDay * Mathf.PI); // Peaks at noon
    }
}
```

---

## 4. Point Light 2D (Dynamic Light Sources)

### Inspector Key Properties

```
Light Type:     Point
Radius (Inner): The fully-lit zone radius
Radius (Outer): Where the light fully fades to black
Intensity:      Brightness multiplier (0 = off, 1 = normal, >1 = bloom)
Color:          Light tint color
Light Order:    Rendering order within the same Sorting Layer
Target Sorting Layers: Which sorting layers this light affects
```

### Flickering Torch / Fire Light

```csharp
using UnityEngine.Rendering.Universal;

public class FlickeringLight : MonoBehaviour
{
    private Light2D light2D;

    [Header("Flicker Settings")]
    [SerializeField] private float baseIntensity = 1.2f;
    [SerializeField] private float flickerSpeed = 8f;         // Noise frequency
    [SerializeField] private float flickerAmount = 0.35f;     // How much it varies

    [Header("Radius Wobble")]
    [SerializeField] private float baseOuterRadius = 3f;
    [SerializeField] private float radiusVariation = 0.3f;

    private float noiseOffset;

    private void Awake()
    {
        light2D = GetComponent<Light2D>();
        noiseOffset = Random.Range(0f, 1000f); // Unique per light
    }

    private void Update()
    {
        float t = Time.time * flickerSpeed + noiseOffset;

        // Sample noise for smooth flicker (not random jitter)
        float noise = Mathf.PerlinNoise(t, 0f);

        // Map [0,1] to a meaningful range around base values
        light2D.intensity = baseIntensity + (noise * 2f - 1f) * flickerAmount;
        light2D.pointLightOuterRadius = baseOuterRadius + (noise * 2f - 1f) * radiusVariation;
    }
}
```

### Light as a Gameplay Mechanic (Detection Range)

```csharp
// Enemy can only detect the player if the player is within the light's range
public class LightDetectionEnemy : MonoBehaviour
{
    [SerializeField] private Light2D visionLight;       // The spot light cone the enemy "sees" with
    [SerializeField] private Transform player;
    [SerializeField] private LayerMask obstacleMask;

    private bool CanSeePlayer()
    {
        Vector2 toPlayer = (Vector2)player.position - (Vector2)transform.position;
        float distance = toPlayer.magnitude;

        // Check if player is within light radius
        if (distance > visionLight.pointLightOuterRadius) return false;

        // Check line of sight (obstacles block vision even within light)
        var hit = Physics2D.Raycast(transform.position, toPlayer.normalized, distance, obstacleMask);
        return hit.collider == null;
    }
}
```

---

## 5. Spot Light 2D (Flashlight / Cone Vision)

```csharp
using UnityEngine.Rendering.Universal;

public class PlayerFlashlight : MonoBehaviour
{
    private Light2D spotLight;

    [SerializeField] private float batteryLife = 30f;          // Seconds
    [SerializeField] private float lowBatteryThreshold = 5f;   // Start flickering
    [SerializeField] private float maxIntensity = 1.5f;

    private float batteryRemaining;
    private bool isOn = true;

    private void Awake()
    {
        spotLight = GetComponent<Light2D>();
        batteryRemaining = batteryLife;
    }

    private void Update()
    {
        if (!isOn) return;

        batteryRemaining -= Time.deltaTime;

        if (batteryRemaining <= 0)
        {
            TurnOff();
            return;
        }

        // Fade light as battery drains
        float charge = Mathf.Clamp01(batteryRemaining / batteryLife);
        spotLight.intensity = maxIntensity * charge;

        // Flicker effect when battery is critically low
        if (batteryRemaining < lowBatteryThreshold)
        {
            float flicker = Mathf.PerlinNoise(Time.time * 15f, 0f);
            spotLight.intensity *= Mathf.Lerp(0.2f, 1f, flicker);
        }

        // Rotate flashlight to follow mouse cursor
        Vector3 mouseWorldPos = Camera.main.ScreenToWorldPoint(Input.mousePosition);
        Vector2 direction = (Vector2)(mouseWorldPos - transform.position);
        float angle = Mathf.Atan2(direction.y, direction.x) * Mathf.Rad2Deg;
        transform.rotation = Quaternion.Euler(0, 0, angle);
    }

    public void TurnOff()
    {
        isOn = false;
        spotLight.enabled = false;
    }

    public void Recharge(float amount)
    {
        batteryRemaining = Mathf.Min(batteryRemaining + amount, batteryLife);
        if (!isOn) { isOn = true; spotLight.enabled = true; }
    }
}
```

---

## 6. Shadow Caster 2D (Dynamic Shadows)

`Shadow Caster 2D` is a component that makes a sprite cast shadows from nearby lights.

### Setup Steps

1. Add `Shadow Caster 2D` component to any sprite GameObject
2. Enable **Cast Shadows** on any `Light2D` that should generate shadows
3. Optionally enable **Self Shadows** on the Shadow Caster to receive its own cast shadow

### Scripting Shadow Casters

```csharp
using UnityEngine.Rendering.Universal;

public class DynamicShadowController : MonoBehaviour
{
    private ShadowCaster2D shadowCaster;

    [SerializeField] private bool selfShadows = false;

    private void Awake()
    {
        shadowCaster = GetComponent<ShadowCaster2D>();
        shadowCaster.selfShadows = selfShadows;
    }

    // Toggle shadows dynamically (e.g., disable for ghost/transparent objects)
    public void EnableShadows(bool enable) => shadowCaster.enabled = enable;
}
```

---

## 7. Normal Maps for 2D Sprites (Depth from Flat Sprites)

A **Normal Map** encodes surface orientation per-pixel in RGB channels. When a 2D light passes over a normal-mapped sprite, bumps and grooves react to light direction — giving 3D depth to flat art.

```
Without Normal Map:  Sprite looks uniformly lit (flat)
With Normal Map:      Bumps highlight toward light, cast micro-shadows away from it
```

### Setup
1. Import your normal map texture (named `MySprite_Normal.png`)
2. In the Texture Importer, set **Texture Type** = `Normal map`
3. Assign it to the sprite's `Sprite-Lit-Default` material in the `Normal Map` slot
4. The light now interacts with the surface topology

### In Code (Runtime Normal Map Switching)

```csharp
public class NormalMapController : MonoBehaviour
{
    private SpriteRenderer spriteRenderer;
    [SerializeField] private Texture2D normalMap;

    private static readonly int NormalMapID = Shader.PropertyToID("_NormalMap");

    private void Awake()
    {
        spriteRenderer = GetComponent<SpriteRenderer>();

        // Create a per-instance material (don't modify shared material!)
        var mat = spriteRenderer.material;
        mat.SetTexture(NormalMapID, normalMap);
    }
}
```

---

## 8. Light Target Sorting Layers

By default, a light affects ALL sorting layers. For a layered scene (background, midground, foreground), you often want different lights to only affect specific layers:

```csharp
using UnityEngine.Rendering.Universal;

public class LightLayerController : MonoBehaviour
{
    [SerializeField] private Light2D ambientLight;    // Affects all layers
    [SerializeField] private Light2D dungeonLight;    // Affects only dungeon/player layer

    private void Start()
    {
        // Programmatically add a sorting layer to the light's target list
        // Sorting layer must exist in Tags & Layers settings
        dungeonLight.lightTargetingLayers = LayerMask.GetMask("Player", "Enemies", "Ground");
    }
}
```

---

## 9. Optimization Rules for 2D Lighting

```
┌──────────────────────────────────────────────────────────────────────────┐
│                    2D LIGHTING PERFORMANCE RULES                         │
├──────────────────────────────────────────────────────────────────────────┤
│ ✅ Limit Point Lights per scene (each adds a render pass)                │
│ ✅ Set Target Sorting Layers to only layers that NEED lighting           │
│ ✅ Use "Volume Only" shadow quality for distant/background lights        │
│ ✅ Bake static lights where possible (Light 2D does not support baking   │
│      natively — use sprite-based light maps as workaround)              │
│ ✅ Disable ShadowCaster2D on small/distant GameObjects                  │
│ ✅ Reduce inner/outer radius to the minimum needed                       │
│ ✅ Pool and reuse light GameObjects for explosion/bullet flash effects   │
│ ❌ Don't use shadows on every object — limit to hero geometry            │
│ ❌ Don't enable self-shadows unless you specifically need them           │
│ ❌ Don't change Light2D.color every frame without caching the component  │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 10. Explosive Flash Light Effect (Pooled)

```csharp
// A bright flash of light that appears briefly on explosion, then fades
// Reuse from an object pool rather than Instantiate/Destroy
using UnityEngine.Rendering.Universal;

public class ExplosionFlash : MonoBehaviour
{
    private Light2D light2D;

    [SerializeField] private float peakIntensity = 5f;
    [SerializeField] private float peakRadius = 6f;
    [SerializeField] private float fadeDuration = 0.3f;

    private void Awake() => light2D = GetComponent<Light2D>();

    private void OnEnable()
    {
        // Reset to peak immediately
        light2D.intensity = peakIntensity;
        light2D.pointLightOuterRadius = peakRadius;
        StartCoroutine(FadeOut());
    }

    private IEnumerator FadeOut()
    {
        float elapsed = 0f;

        while (elapsed < fadeDuration)
        {
            elapsed += Time.deltaTime;
            float t = elapsed / fadeDuration;

            light2D.intensity = Mathf.Lerp(peakIntensity, 0f, t);
            light2D.pointLightOuterRadius = Mathf.Lerp(peakRadius, 0.5f, t);

            yield return null;
        }

        // Return to pool
        gameObject.SetActive(false);
    }
}
```
