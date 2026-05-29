# 🎬 Animation Deep Dive
### AnimationCurves in Code, Mecanim State Machine, Animation Events, IK & Runtime Control

Animation is more than dragging clips into an Animator. This guide covers code-driven animation control, runtime state machine manipulation, animation events, and inverse kinematics — all critical for responsive game feel.

---

## 1. Animator Parameter Control (Fast Patterns)

### Hash IDs Over String Names

Every call to `animator.SetFloat("speed", val)` internally hashes the string to an integer ID at runtime. Caching the hash in `Awake()` eliminates this overhead:

```csharp
public class PlayerAnimator : MonoBehaviour
{
    private Animator animator;
    private Rigidbody2D rb;

    // ─── Cache hashes ONCE in Awake ─────────────────────────────────────
    private static readonly int HashSpeed      = Animator.StringToHash("speed");
    private static readonly int HashIsGrounded = Animator.StringToHash("isGrounded");
    private static readonly int HashJump       = Animator.StringToHash("jump");
    private static readonly int HashHurt       = Animator.StringToHash("hurt");
    private static readonly int HashDeath      = Animator.StringToHash("death");

    private void Awake()
    {
        animator = GetComponent<Animator>();
        rb = GetComponent<Rigidbody2D>();
    }

    private void Update()
    {
        // Use hashed versions — zero string allocation
        animator.SetFloat(HashSpeed, Mathf.Abs(rb.linearVelocity.x));
        animator.SetBool(HashIsGrounded, IsGrounded());
    }

    public void TriggerJump()    => animator.SetTrigger(HashJump);
    public void TriggerHurt()    => animator.SetTrigger(HashHurt);
    public void TriggerDeath()   => animator.SetBool(HashDeath, true);
}
```

---

## 2. Animator.CrossFade vs. Animator.Play

### `Animator.Play` — Instant, hard cut to a state

```csharp
// Jump directly to a state by name hash, on layer 0, at time 0.0 (start)
animator.Play(HashJump, 0, 0f);

// Jump to a state at 50% through the animation
animator.Play(HashHurt, 0, 0.5f);
```

### `Animator.CrossFadeInFixedTime` — Smooth blend between states

```csharp
// Blend to the "Walk" state over 0.15 seconds (fixed time, ignores timeScale)
animator.CrossFadeInFixedTime("Walk", 0.15f);

// Blend using hash (preferred)
animator.CrossFadeInFixedTime(HashSpeed, 0.15f, 0);
```

### Checking Current State

```csharp
// Get the state info for the base layer (layer 0)
AnimatorStateInfo stateInfo = animator.GetCurrentAnimatorStateInfo(0);

// Check if currently in the Jump state
if (stateInfo.IsName("Jump")) { ... }

// Check normalized time (0 = start, 1 = end of clip)
if (stateInfo.normalizedTime >= 0.9f) { ... } // Near end of animation

// Check if any transition is currently blending
if (animator.IsInTransition(0)) { ... }
```

---

## 3. Animation Events (Code Hooks Inside Clips)

Animation events are markers embedded in an `.anim` clip that call a C# method on the same GameObject at a precise frame. Essential for:
- Footstep sound triggers (exact frame foot lands)
- Weapon hitbox activation (during swing)
- VFX spawning (at moment of impact)
- Combo window opening (after attack frames)

### How to Add (in Editor)
1. Open the `.anim` clip in the **Animation Window**
2. Move the playhead to the desired frame
3. Click **Add Event** → set the Function name → click **Apply**

### The Receiving Script (on same GameObject)

```csharp
public class PlayerAnimationEvents : MonoBehaviour
{
    [SerializeField] private AudioSource footstepAudio;
    [SerializeField] private GameObject hitboxObject;
    [SerializeField] private ParticleSystem swordSlashVFX;

    // Called by animation event on the walking clip at foot-strike frames
    public void OnFootstep()
    {
        footstepAudio.pitch = Random.Range(0.9f, 1.1f);
        footstepAudio.Play();
    }

    // Called by animation event at the start of sword swing's active frames
    public void EnableHitbox()
    {
        hitboxObject.SetActive(true);
    }

    // Called by animation event at the end of the active hitbox frames
    public void DisableHitbox()
    {
        hitboxObject.SetActive(false);
    }

    // Called by animation event at the moment of sword connecting
    public void SpawnSlashVFX()
    {
        swordSlashVFX.Play();
    }

    // Called by animation event — passes a float value from the clip editor
    public void SetMoveSpeedMultiplier(float multiplier)
    {
        // Use the clip-defined value to scale speed during wind-up or recovery
        Debug.Log($"Speed multiplier from clip: {multiplier}");
    }
}
```

---

## 4. AnimationCurves in Code

`AnimationCurve` is a powerful serializable curve asset you can define in the Inspector and evaluate in code. It maps a `float` input (usually time `t` from 0 to 1) to an output `float`.

### Why Use It Over Mathf.Lerp?

- Fully designer-tunable — artists modify the feel without touching code
- Handles easing, overshoots, bounces, custom shapes
- Evaluates in O(log n) time — very fast

```csharp
public class JumpController : MonoBehaviour
{
    private Rigidbody2D rb;

    // ─── Configure in Inspector: draws the jump arc shape ────────────────
    [SerializeField] private AnimationCurve jumpCurve = AnimationCurve.EaseInOut(0, 0, 1, 1);
    [SerializeField] private float jumpHeight = 5f;
    [SerializeField] private float jumpDuration = 0.4f;

    private bool isJumping;
    private float jumpTimer;
    private float previousHeight;

    private void Awake() => rb = GetComponent<Rigidbody2D>();

    public void StartJump()
    {
        isJumping = true;
        jumpTimer = 0f;
        previousHeight = transform.position.y;
        rb.gravityScale = 0f; // Disable gravity during curve-driven jump
    }

    private void FixedUpdate()
    {
        if (!isJumping) return;

        jumpTimer += Time.fixedDeltaTime;
        float t = Mathf.Clamp01(jumpTimer / jumpDuration);

        // Evaluate the curve to get current height fraction (0 to 1 and back)
        float heightFraction = jumpCurve.Evaluate(t);
        float targetHeight = previousHeight + jumpHeight * heightFraction;

        // Move to target height via velocity (keeps physics integration accurate)
        float dy = (targetHeight - rb.position.y) / Time.fixedDeltaTime;
        rb.linearVelocity = new Vector2(rb.linearVelocity.x, dy);

        if (t >= 1f)
        {
            isJumping = false;
            rb.gravityScale = 1f;
        }
    }
}
```

### Damage Falloff Curve

```csharp
[SerializeField] private AnimationCurve damageFalloffCurve; // x=distance (0-1), y=damage multiplier (0-1)
[SerializeField] private float explosionRadius = 5f;
[SerializeField] private float maxDamage = 100f;

// Apply explosion damage with designer-tunable falloff curve
void ApplyExplosionDamage(Vector2 explosionCenter)
{
    var nearby = Physics2D.OverlapCircleAll(explosionCenter, explosionRadius);
    foreach (var col in nearby)
    {
        float distance = Vector2.Distance(explosionCenter, col.transform.position);
        float normalizedDist = distance / explosionRadius;          // 0 = center, 1 = edge
        float damageMultiplier = damageFalloffCurve.Evaluate(normalizedDist);
        float damage = maxDamage * damageMultiplier;

        col.GetComponent<IDamageable>()?.TakeDamage(damage);
    }
}
```

---

## 5. Sub-State Machines & Animator Layers

### Animator Layers (Blending)

Layers allow separate animations to run simultaneously on different body parts:

```
Layer 0 (Weight: 1.0, Additive: OFF) — Full body: Idle, Walk, Run
Layer 1 (Weight: 1.0, Additive: OFF) — Upper body: Attack, Aim, Reload
Layer 2 (Weight: 0.5, Additive: ON)  — Face: Expression blend
```

```csharp
// Fade a layer in/out smoothly
private IEnumerator SetLayerWeightSmooth(int layerIndex, float targetWeight, float duration)
{
    float startWeight = animator.GetLayerWeight(layerIndex);
    float elapsed = 0f;

    while (elapsed < duration)
    {
        elapsed += Time.deltaTime;
        float weight = Mathf.Lerp(startWeight, targetWeight, elapsed / duration);
        animator.SetLayerWeight(layerIndex, weight);
        yield return null;
    }

    animator.SetLayerWeight(layerIndex, targetWeight);
}

// Usage: fade in attack layer when entering combat
// StartCoroutine(SetLayerWeightSmooth(1, 1f, 0.2f));
```

### Avatar Masks (Layer Isolation)

In the Animator, each layer can have an **Avatar Mask** that restricts which body parts that layer controls. For 2D sprite-based characters, you typically use transform masks (selecting which child transforms the layer drives).

---

## 6. Animator Override Controller (Runtime Skin Swapping)

Reuses an existing state machine but replaces the clip assets at runtime. Perfect for weapon skins, character variants, or seasonal outfits.

```csharp
public class AnimatorSkinSwapper : MonoBehaviour
{
    private Animator animator;
    private AnimatorOverrideController overrideController;

    // Pre-assign in inspector: pairs of (original clip, replacement clip)
    [SerializeField] private AnimationClip baseIdleClip;
    [SerializeField] private AnimationClip baseWalkClip;

    private void Awake()
    {
        animator = GetComponent<Animator>();

        // Create override controller from the existing runtime controller
        overrideController = new AnimatorOverrideController(animator.runtimeAnimatorController);
        animator.runtimeAnimatorController = overrideController;
    }

    // Call this to swap to a new character skin set
    public void ApplySkin(AnimationClip newIdleClip, AnimationClip newWalkClip)
    {
        // Replace the base clips with skin-specific versions
        overrideController[baseIdleClip] = newIdleClip;
        overrideController[baseWalkClip] = newWalkClip;
    }
}
```

---

## 7. Animator State Machine Behaviors

State Machine Behaviors are scripts that attach to **states** inside the Animator, triggering code when entering, updating, or exiting that specific state.

```csharp
// Attach this to a specific state node inside the Animator Controller
// Right-click a state → Add Behaviour → Create new SMB script
public class DeathStateBehaviour : StateMachineBehaviour
{
    // Called once when entering the Death state
    public override void OnStateEnter(Animator animator, AnimatorStateInfo stateInfo, int layerIndex)
    {
        // Disable player input
        animator.GetComponent<PlayerInputHandler>()?.DisableInput();
    }

    // Called every frame the Death state is active
    public override void OnStateUpdate(Animator animator, AnimatorStateInfo stateInfo, int layerIndex)
    {
        // When the death animation is 95% done, trigger respawn
        if (stateInfo.normalizedTime >= 0.95f)
        {
            animator.GetComponent<PlayerRespawn>()?.RespawnPlayer();
        }
    }

    // Called once when exiting the Death state
    public override void OnStateExit(Animator animator, AnimatorStateInfo stateInfo, int layerIndex)
    {
        animator.SetBool("death", false);
    }
}
```

---

## 8. Sprite Animation Without Animator (Lightweight)

For simple objects (UI effects, item pickups, debris), using an Animator component is overkill. A manual sprite swapper costs less memory:

```csharp
public class SpriteAnimator : MonoBehaviour
{
    private SpriteRenderer spriteRenderer;

    [SerializeField] private Sprite[] frames;
    [SerializeField] private float framesPerSecond = 12f;
    [SerializeField] private bool loop = true;

    private float frameTimer;
    private int currentFrame;

    private void Awake() => spriteRenderer = GetComponent<SpriteRenderer>();

    private void Update()
    {
        if (frames == null || frames.Length == 0) return;

        frameTimer += Time.deltaTime;
        float frameDuration = 1f / framesPerSecond;

        if (frameTimer >= frameDuration)
        {
            frameTimer -= frameDuration;
            currentFrame++;

            if (currentFrame >= frames.Length)
            {
                if (loop) currentFrame = 0;
                else { currentFrame = frames.Length - 1; enabled = false; return; }
            }

            spriteRenderer.sprite = frames[currentFrame];
        }
    }

    public void Play() { currentFrame = 0; frameTimer = 0; enabled = true; }
    public void Stop() => enabled = false;
}
```

---

## 9. Sprite Flipping & Direction Control

```csharp
// Flip the sprite to face the movement direction — without rotating the transform
// This is important because rotating the Transform also rotates colliders and child objects!
public class FacingDirectionController : MonoBehaviour
{
    private SpriteRenderer spriteRenderer;
    private Rigidbody2D rb;

    private void Awake()
    {
        spriteRenderer = GetComponent<SpriteRenderer>();
        rb = GetComponent<Rigidbody2D>();
    }

    private void Update()
    {
        float horizontal = rb.linearVelocity.x;

        if (horizontal > 0.1f)
            spriteRenderer.flipX = false; // Facing right
        else if (horizontal < -0.1f)
            spriteRenderer.flipX = true;  // Facing left
    }
}
```

---

## 10. Animation Performance Rules

```
┌──────────────────────────────────────────────────────────────────────────┐
│                    ANIMATION PERFORMANCE RULES                           │
├──────────────────────────────────────────────────────────────────────────┤
│ ✅ Cache Animator.StringToHash() results as static readonly ints         │
│ ✅ Disable Animator component on off-screen objects (cullingMode)        │
│ ✅ Use Animator.cullingMode = AlwaysAnimate / CullUpdateTransforms       │
│ ✅ Use Animator Override Controllers instead of multiple Animators        │
│ ✅ Prefer StateMachineBehaviours for state-specific logic                │
│ ✅ Uncheck "Apply Root Motion" if you drive position via Rigidbody2D    │
│ ❌ Don't use strings in SetFloat/SetBool/SetTrigger in Update()          │
│ ❌ Don't use Animator on hundreds of small particles/debris objects      │
│ ❌ Don't call GetCurrentAnimatorStateInfo() multiple times per frame     │
└──────────────────────────────────────────────────────────────────────────┘
```
