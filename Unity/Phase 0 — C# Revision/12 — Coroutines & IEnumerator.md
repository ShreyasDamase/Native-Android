# 12 — Coroutines & IEnumerator
### 🟡 Block B — Unity-Critical C# Features

> [!IMPORTANT]
> Coroutines are Unity's built-in way to **spread work across multiple frames** without blocking the game. Every timer, cutscene, spawn delay, fade effect, and loading sequence uses coroutines. You will write these daily.

---

## 12.1 — What is a Coroutine?

A coroutine is a method that can **pause its own execution** and resume later, without blocking the main thread.

```
Normal Method:          Coroutine:
┌──────────┐            ┌──────────┐    ← Frame 1: runs to yield
│ Line 1   │            │ Line 1   │
│ Line 2   │            │ yield    │    ← PAUSES here, returns control to Unity
│ Line 3   │            └──────────┘
│ Line 4   │
│ returns  │            ┌──────────┐    ← Frame 2 (or later): resumes
└──────────┘            │ Line 3   │
                        │ yield    │    ← PAUSES again
                        └──────────┘

                        ┌──────────┐    ← Frame 3: finishes
                        │ Line 5   │
                        │ return   │
                        └──────────┘
```

---

## 12.2 — Your First Coroutine

```csharp
using System.Collections;
using UnityEngine;

public class DoorController : MonoBehaviour
{
    // Must return IEnumerator to be a coroutine
    IEnumerator OpenDoorSequence()
    {
        Debug.Log("Door opening...");
        
        // Wait 2 seconds before continuing — doesn't block other scripts
        yield return new WaitForSeconds(2f);
        
        Debug.Log("Door fully open!");
        PlayOpenAnimation();
        
        // Wait another second
        yield return new WaitForSeconds(1f);
        
        Debug.Log("Door closing...");
        PlayCloseAnimation();
    }
    
    void OnTriggerEnter2D(Collider2D other)
    {
        if (other.CompareTag("Player"))
        {
            // Start the coroutine — it runs alongside everything else
            StartCoroutine(OpenDoorSequence());
        }
    }
}
```

---

## 12.3 — All `yield return` Types

| yield return | What It Waits For |
| :--- | :--- |
| `yield return null` | Next frame (one frame) |
| `yield return new WaitForSeconds(t)` | t real seconds (affected by Time.timeScale) |
| `yield return new WaitForSecondsRealtime(t)` | t real seconds (NOT affected by timeScale — pauses work) |
| `yield return new WaitForFixedUpdate()` | Next physics step (FixedUpdate) |
| `yield return new WaitForEndOfFrame()` | End of current frame (after rendering) |
| `yield return new WaitUntil(() => condition)` | Until a lambda condition becomes true |
| `yield return new WaitWhile(() => condition)` | While a lambda condition is true |
| `yield return StartCoroutine(OtherRoutine())` | Until another coroutine finishes |
| `yield break` | Exit the coroutine immediately |

```csharp
IEnumerator DemoAllYields()
{
    // Wait one frame
    yield return null;
    Debug.Log("One frame later");
    
    // Wait 2 seconds (affected by slow-motion Time.timeScale)
    yield return new WaitForSeconds(2f);
    Debug.Log("2 scaled seconds later");
    
    // Wait 2 seconds real-time (NOT affected by timeScale — use for paused menu)
    yield return new WaitForSecondsRealtime(2f);
    Debug.Log("2 real seconds later (even if game is paused)");
    
    // Wait until player is grounded
    yield return new WaitUntil(() => isGrounded);
    Debug.Log("Player landed!");
    
    // Wait while enemy is alive
    yield return new WaitWhile(() => boss.IsAlive);
    Debug.Log("Boss is dead!");
    
    // Wait for another coroutine to finish
    yield return StartCoroutine(PlayCutscene());
    Debug.Log("Cutscene done!");
    
    // Wait for next physics update
    yield return new WaitForFixedUpdate();
    Debug.Log("Physics stepped");
    
    // Early exit
    if (shouldStop)
        yield break;
    
    Debug.Log("This won't run if shouldStop was true");
}
```

---

## 12.4 — Starting and Stopping Coroutines

```csharp
public class EnemySpawner : MonoBehaviour
{
    // Store the reference to stop it later
    private Coroutine spawnRoutine;
    private Coroutine flashRoutine;
    
    void StartSpawning()
    {
        // Start — begins the coroutine
        spawnRoutine = StartCoroutine(SpawnLoop());
    }
    
    void StopSpawning()
    {
        // Stop a specific coroutine by reference
        if (spawnRoutine != null)
        {
            StopCoroutine(spawnRoutine);
            spawnRoutine = null;
        }
    }
    
    void StopAll()
    {
        // Stop ALL coroutines on this MonoBehaviour
        StopAllCoroutines();
    }
    
    // Restart pattern — stop if running, then start fresh
    void RestartSpawning()
    {
        StopSpawning();
        spawnRoutine = StartCoroutine(SpawnLoop());
    }
    
    IEnumerator SpawnLoop()
    {
        while (true) // infinite loop — exits when StopCoroutine is called
        {
            SpawnEnemy();
            yield return new WaitForSeconds(3f);
        }
    }
}
```

> [!WARNING]
> **Coroutines stop automatically when the GameObject is disabled or destroyed.** If you need a persistent coroutine that survives disable/enable cycles, start it on a separate manager GameObject that is never disabled.

---

## 12.5 — Passing Parameters to Coroutines

```csharp
// Coroutines can take parameters just like normal methods
IEnumerator FadeOut(CanvasGroup group, float duration)
{
    float elapsed = 0f;
    float startAlpha = group.alpha;
    
    while (elapsed < duration)
    {
        elapsed += Time.deltaTime;
        group.alpha = Mathf.Lerp(startAlpha, 0f, elapsed / duration);
        yield return null; // wait one frame
    }
    
    group.alpha = 0f; // ensure exactly 0 at end
    group.gameObject.SetActive(false);
}

IEnumerator FadeIn(CanvasGroup group, float duration)
{
    group.gameObject.SetActive(true);
    group.alpha = 0f;
    float elapsed = 0f;
    
    while (elapsed < duration)
    {
        elapsed += Time.deltaTime;
        group.alpha = Mathf.Lerp(0f, 1f, elapsed / duration);
        yield return null;
    }
    
    group.alpha = 1f;
}

// Chain them: fade out main menu, then fade in game UI
IEnumerator TransitionToGame()
{
    yield return StartCoroutine(FadeOut(mainMenuGroup, 0.5f));
    yield return new WaitForSeconds(0.2f);
    LoadGameScene();
    yield return new WaitForSeconds(0.1f);
    yield return StartCoroutine(FadeIn(gameUIGroup, 0.5f));
}
```

---

## 12.6 — Real-World Coroutine Patterns

### Pattern 1 — Delayed Action (Timer)
```csharp
IEnumerator DestroyAfterDelay(float delay)
{
    yield return new WaitForSeconds(delay);
    Destroy(gameObject);
}

// Shorter version using Destroy's optional delay parameter
// Destroy(gameObject, 2f); // same as above, but can't chain other logic

// Invoke an action after delay
IEnumerator DelayedAction(float delay, System.Action action)
{
    yield return new WaitForSeconds(delay);
    action?.Invoke();
}

// Usage
StartCoroutine(DelayedAction(2f, () => SpawnReinforcementsWave()));
```

### Pattern 2 — Move/Interpolate Over Time
```csharp
// Smoothly move an object from A to B over 'duration' seconds
IEnumerator MoveToPosition(Vector3 target, float duration)
{
    Vector3 startPos = transform.position;
    float elapsed = 0f;
    
    while (elapsed < duration)
    {
        elapsed += Time.deltaTime;
        float t = elapsed / duration;
        
        // Smooth step — ease in/out
        float smoothT = t * t * (3f - 2f * t);
        
        transform.position = Vector3.Lerp(startPos, target, smoothT);
        yield return null;
    }
    
    transform.position = target; // snap to exact position
}

// Smooth rotation
IEnumerator RotateTo(Quaternion targetRot, float duration)
{
    Quaternion startRot = transform.rotation;
    float elapsed = 0f;
    
    while (elapsed < duration)
    {
        elapsed += Time.deltaTime;
        transform.rotation = Quaternion.Slerp(startRot, targetRot, elapsed / duration);
        yield return null;
    }
    
    transform.rotation = targetRot;
}
```

### Pattern 3 — Cooldown Timer
```csharp
public class PlayerAbility : MonoBehaviour
{
    [SerializeField] private float cooldownDuration = 5f;
    
    public bool IsOnCooldown { get; private set; }
    public float CooldownProgress { get; private set; } // 0 = ready, 1 = just used
    
    private Coroutine cooldownRoutine;
    
    public bool TryActivate()
    {
        if (IsOnCooldown) return false;
        
        ActivateAbility();
        cooldownRoutine = StartCoroutine(CooldownRoutine());
        return true;
    }
    
    IEnumerator CooldownRoutine()
    {
        IsOnCooldown = true;
        float elapsed = 0f;
        
        while (elapsed < cooldownDuration)
        {
            elapsed += Time.deltaTime;
            CooldownProgress = 1f - (elapsed / cooldownDuration);
            yield return null;
        }
        
        CooldownProgress = 0f;
        IsOnCooldown = false;
    }
}
```

### Pattern 4 — Spawn Waves with Delay
```csharp
IEnumerator SpawnWave(WaveData wave)
{
    Debug.Log($"Wave {wave.WaveNumber} starting!");
    
    for (int i = 0; i < wave.EnemyCount; i++)
    {
        Vector3 spawnPos = GetRandomSpawnPoint();
        Instantiate(wave.EnemyPrefab, spawnPos, Quaternion.identity);
        
        // Wait between each enemy spawn
        yield return new WaitForSeconds(wave.SpawnInterval);
    }
    
    Debug.Log("All enemies spawned. Waiting for clear...");
    
    // Wait until all enemies are dead
    yield return new WaitUntil(() => AreAllEnemiesDead());
    
    Debug.Log("Wave cleared!");
    OnWaveCompleted?.Invoke(wave.WaveNumber);
    
    // Break before next wave
    yield return new WaitForSeconds(3f);
    
    // Next wave
    if (currentWave < waves.Count - 1)
        StartCoroutine(SpawnWave(waves[currentWave + 1]));
}
```

### Pattern 5 — Flash/Blink Effect
```csharp
IEnumerator FlashDamage(SpriteRenderer renderer, Color flashColor, int count, float interval)
{
    Color originalColor = renderer.color;
    
    for (int i = 0; i < count; i++)
    {
        renderer.color = flashColor;
        yield return new WaitForSeconds(interval);
        renderer.color = originalColor;
        yield return new WaitForSeconds(interval);
    }
}

// Usage: Flash red 3 times on hit
StartCoroutine(FlashDamage(spriteRenderer, Color.red, 3, 0.1f));
```

### Pattern 6 — Slow Motion Effect
```csharp
IEnumerator SlowMotion(float targetTimeScale, float transitionDuration, float holdDuration)
{
    // Slow down
    float startScale = Time.timeScale;
    float elapsed = 0f;
    
    while (elapsed < transitionDuration)
    {
        elapsed += Time.unscaledDeltaTime; // unscaled — works even when timeScale=0
        Time.timeScale = Mathf.Lerp(startScale, targetTimeScale, elapsed / transitionDuration);
        Time.fixedDeltaTime = 0.02f * Time.timeScale; // keep physics consistent
        yield return null;
    }
    
    // Hold slow-mo
    yield return new WaitForSecondsRealtime(holdDuration);
    
    // Return to normal
    elapsed = 0f;
    startScale = Time.timeScale;
    
    while (elapsed < transitionDuration)
    {
        elapsed += Time.unscaledDeltaTime;
        Time.timeScale = Mathf.Lerp(startScale, 1f, elapsed / transitionDuration);
        Time.fixedDeltaTime = 0.02f * Time.timeScale;
        yield return null;
    }
    
    Time.timeScale = 1f;
    Time.fixedDeltaTime = 0.02f;
}

// Usage: kill-cam slow-mo
StartCoroutine(SlowMotion(0.2f, 0.1f, 0.5f));
```

---

## 12.7 — Coroutine Pitfalls

```csharp
// ❌ PITFALL 1: Coroutine on null/destroyed object
IEnumerator DangerousRoutine()
{
    yield return new WaitForSeconds(2f);
    target.TakeDamage(10f); // target might be destroyed by now!
}

// ✅ FIX: Check validity before using
IEnumerator SafeRoutine()
{
    yield return new WaitForSeconds(2f);
    if (target != null && target.IsAlive)
        target.TakeDamage(10f);
}

// ❌ PITFALL 2: Allocating WaitForSeconds every frame
void Update()
{
    // Don't start coroutines in Update without checking if already running!
    StartCoroutine(DoSomething()); // starts a NEW coroutine EVERY frame!
}

// ✅ FIX: Cache WaitForSeconds objects (they allocate on heap)
private readonly WaitForSeconds wait1s = new WaitForSeconds(1f);
private readonly WaitForSeconds wait3s = new WaitForSeconds(3f);

IEnumerator EfficientRoutine()
{
    yield return wait1s; // reuses the cached object — no allocation!
    DoSomething();
    yield return wait3s;
    DoSomethingElse();
}
```

---

## 12.8 — 🎮 2D vs 3D: Coroutine Use Cases

### 🎮 2D — Knockback & Invincibility Frame
```csharp
IEnumerator Knockback2D(Vector2 direction, float force, float duration)
{
    rb2d.AddForce(direction * force, ForceMode2D.Impulse);
    yield return new WaitForSeconds(duration);
    rb2d.linearVelocity = Vector2.zero;
}

IEnumerator InvincibilityFrames2D(float duration)
{
    isInvincible = true;
    
    // Flash the sprite during invincibility
    float elapsed = 0f;
    while (elapsed < duration)
    {
        spriteRenderer.enabled = !spriteRenderer.enabled;
        yield return new WaitForSeconds(0.08f);
        elapsed += 0.08f;
    }
    
    spriteRenderer.enabled = true;
    isInvincible = false;
}
```

### 🎮 3D — Ragdoll Death & Respawn
```csharp
IEnumerator RagdollAndRespawn3D(float ragdollDuration)
{
    // Enable ragdoll physics
    foreach (Rigidbody rb in ragdollBodies)
    {
        rb.isKinematic = false;
        rb.AddExplosionForce(deathForce, deathPoint, 2f);
    }
    
    // Disable character controller
    characterController.enabled = false;
    animator.enabled = false;
    
    yield return new WaitForSeconds(ragdollDuration);
    
    // Fade out
    yield return StartCoroutine(FadeOut(characterRenderer, 0.5f));
    
    // Reset and respawn
    transform.position = respawnPoint.position;
    foreach (Rigidbody rb in ragdollBodies)
        rb.isKinematic = true;
    
    characterController.enabled = true;
    animator.enabled = true;
    
    yield return StartCoroutine(FadeIn(characterRenderer, 0.5f));
}
```

---

## 📝 Summary

| Concept | Key Point |
| :--- | :--- |
| `IEnumerator` | Return type that enables yield |
| `yield return null` | Pause for one frame |
| `yield return new WaitForSeconds(t)` | Pause for t seconds (affected by timeScale) |
| `yield return new WaitForSecondsRealtime(t)` | Pause t real seconds (use for paused menus) |
| `yield return new WaitUntil(lambda)` | Pause until condition is true |
| `StartCoroutine()` | Begin coroutine execution |
| `StopCoroutine(reference)` | Stop a specific coroutine |
| `yield break` | Exit coroutine early |
| Cache `WaitForSeconds` | Avoid heap allocation — reuse instances |
| Coroutines stop on disable | Run on a persistent manager if needed |

**Previous:** [[11 — Enums & Flags]] | **Next:** [[13 — Async Await & Tasks]]
