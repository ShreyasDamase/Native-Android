# 🔄 Coroutines, Async/Await & Timing in Unity
### IEnumerator, Coroutine Patterns, async/await, UniTask & Time Management

Unity's concurrency model differs fundamentally from multi-threading. This guide explains every timing primitive and when to use each one.

---

## 1. Unity's Execution Model

Unity is **single-threaded on the main thread**. There are no true parallel coroutines — they interleave by yielding control back to the engine at specific moments each frame.

```
Frame N:
  ├── Update() on all MonoBehaviours
  ├── Coroutine: resume after WaitForEndOfFrame/WaitForSeconds
  ├── FixedUpdate() (may run 0 or multiple times per frame)
  └── Render

Frame N+1:
  └── ... (same cycle)
```

---

## 2. IEnumerator Coroutines

### How Coroutines Work

A coroutine is a C# iterator method that returns `IEnumerator`. When it hits `yield return`, it **pauses**, hands control back to Unity, and **resumes** next frame (or after the yield condition is met).

```csharp
// Start a coroutine
StartCoroutine(MyCoroutine());           // Pass IEnumerator
StartCoroutine("MyCoroutineByName");     // Pass method name string (slower)

// Store reference to stop it later
Coroutine handle = StartCoroutine(MyCoroutine());
StopCoroutine(handle); // Stop by handle (preferred)
StopCoroutine("MyCoroutineByName");      // Stop by name
StopAllCoroutines();                     // Stop all coroutines on this MonoBehaviour
```

### Yield Types Reference

```csharp
yield return null;                          // Wait until next Update() frame
yield return new WaitForFixedUpdate();      // Wait until next FixedUpdate()
yield return new WaitForEndOfFrame();       // Wait until after rendering (use for screenshots)
yield return new WaitForSeconds(2.5f);      // Wait 2.5 real seconds (paused by Time.timeScale)
yield return new WaitForSecondsRealtime(1f);// Wait 1 second ignoring timeScale (menu timers)
yield return new WaitUntil(() => isReady);  // Wait until a condition is true
yield return new WaitWhile(() => isLoading);// Wait while a condition is true
yield return StartCoroutine(OtherRoutine()); // Wait for another coroutine to finish
yield return new AsyncOperationHandle();    // Wait for Addressable/async operation
```

---

## 3. Coroutine Patterns

### Pattern A: Timed Sequence (Cutscene / Tutorial)

```csharp
private IEnumerator PlayTutorialSequence()
{
    // Step 1: Show dialog
    dialogBox.Show("Welcome, Commander!");
    yield return new WaitForSeconds(2.5f);

    // Step 2: Highlight button
    uiHighlight.Activate(fireButton);
    yield return new WaitUntil(() => InputSystem.actions["Fire"].WasPressedThisFrame());

    // Step 3: Celebrate
    uiHighlight.Deactivate();
    dialogBox.Show("Great shot!");
    yield return new WaitForSeconds(1.5f);

    dialogBox.Hide();
    OnTutorialComplete?.Invoke();
}
```

### Pattern B: Smooth Fade (UI / Camera)

```csharp
// Fade a CanvasGroup (alpha 0 = transparent, 1 = opaque)
private IEnumerator FadeCanvasGroup(CanvasGroup group, float from, float to, float duration)
{
    float elapsed = 0f;
    group.alpha = from;

    while (elapsed < duration)
    {
        elapsed += Time.unscaledDeltaTime; // Use unscaled for UI (works when paused)
        group.alpha = Mathf.Lerp(from, to, elapsed / duration);
        yield return null;
    }

    group.alpha = to;
}

// Usage:
// yield return StartCoroutine(FadeCanvasGroup(screenFader, 0f, 1f, 0.5f)); // Fade in
// yield return StartCoroutine(FadeCanvasGroup(screenFader, 1f, 0f, 0.5f)); // Fade out
```

### Pattern C: Spawn Wave System

```csharp
[SerializeField] private GameObject enemyPrefab;
[SerializeField] private Transform[] spawnPoints;

private IEnumerator SpawnWave(int enemyCount, float delayBetweenSpawns)
{
    for (int i = 0; i < enemyCount; i++)
    {
        Transform spawnPoint = spawnPoints[i % spawnPoints.Length];
        Instantiate(enemyPrefab, spawnPoint.position, Quaternion.identity);
        yield return new WaitForSeconds(delayBetweenSpawns);
    }

    // After all enemies are spawned, wait until all are defeated
    yield return new WaitUntil(() => FindObjectsByType<Enemy>(FindObjectsSortMode.None).Length == 0);
    Debug.Log("Wave cleared!");
    StartCoroutine(SpawnWave(enemyCount + 3, delayBetweenSpawns * 0.9f)); // Next wave
}
```

### Pattern D: Pooled Coroutine Timer (No Garbage)

Standard `WaitForSeconds` allocates a new object every call. Cache it to eliminate garbage:

```csharp
public class CachedWaits : MonoBehaviour
{
    // Pre-allocate common wait objects as static members
    private static readonly WaitForFixedUpdate WaitFixed = new WaitForFixedUpdate();
    private static readonly WaitForEndOfFrame WaitEndFrame = new WaitForEndOfFrame();

    // Cache WaitForSeconds by duration using a dictionary
    private static readonly Dictionary<float, WaitForSeconds> waitCache = new Dictionary<float, WaitForSeconds>();

    public static WaitForSeconds Wait(float seconds)
    {
        if (!waitCache.TryGetValue(seconds, out var wait))
        {
            wait = new WaitForSeconds(seconds);
            waitCache[seconds] = wait;
        }
        return wait;
    }
}

// Usage (zero garbage):
// yield return CachedWaits.Wait(0.5f);
```

### Pattern E: Safe External Coroutine (Survives Scene Loads)

Coroutines are tied to their MonoBehaviour. If it's destroyed, the coroutine stops. Use a persistent runner for scene-independent coroutines:

```csharp
[RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.BeforeSceneLoad)]
private static void CreateCoroutineRunner()
{
    var go = new GameObject("[CoroutineRunner]");
    Object.DontDestroyOnLoad(go);
    go.AddComponent<CoroutineRunner>();
}

public class CoroutineRunner : MonoBehaviour
{
    public static CoroutineRunner Instance { get; private set; }
    private void Awake() => Instance = this;

    public static Coroutine Run(IEnumerator routine)
        => Instance.StartCoroutine(routine);
}

// Usage from anywhere (even non-MonoBehaviour classes):
// CoroutineRunner.Run(SomeCoroutine());
```

---

## 4. async/await in Unity

C# `async/await` works in Unity but requires careful use — Unity's API is **not thread-safe** and must be called only on the main thread.

### async void vs async Task

```csharp
// async void: Fire-and-forget. Exceptions are silently swallowed. Use rarely.
private async void Start()
{
    await LoadDataAsync();
}

// async Task: Returns a Task. Exceptions propagate. Always prefer this for game logic.
private async Task LoadDataAsync()
{
    Debug.Log("Loading...");
    await Task.Delay(1000); // Non-blocking 1-second wait (uses system clock, not Unity time)
    Debug.Log("Loaded!");
}
```

### Awaiting Unity Operations

```csharp
// Loading a scene asynchronously without blocking the main thread
private async Task LoadSceneAsync(string sceneName)
{
    var operation = UnityEngine.SceneManagement.SceneManager.LoadSceneAsync(sceneName);
    operation.allowSceneActivation = false;

    // Wait until almost fully loaded
    while (operation.progress < 0.9f)
    {
        float progress = operation.progress / 0.9f;
        loadingBar.value = progress;
        await Task.Yield(); // Resume on next frame — equivalent to yield return null
    }

    // Show press-any-key prompt, then activate
    pressAnyKeyPanel.SetActive(true);
    await Task.Run(() => { /* Wait for input — but DON'T call Unity API here! */ });
    operation.allowSceneActivation = true;
}
```

### CancellationToken — Cancelling Async Operations

```csharp
private CancellationTokenSource cts;

private void OnEnable()
{
    cts = new CancellationTokenSource();
    _ = RunTimedAbilityAsync(cts.Token);
}

private void OnDisable()
{
    // Cancel when this object is disabled (e.g., scene change)
    cts.Cancel();
    cts.Dispose();
}

private async Task RunTimedAbilityAsync(CancellationToken token)
{
    Debug.Log("Ability activated!");
    try
    {
        await Task.Delay(TimeSpan.FromSeconds(5), token); // Cancel-aware delay
        Debug.Log("Ability expired.");
    }
    catch (OperationCanceledException)
    {
        Debug.Log("Ability was cancelled before expiry.");
    }
}
```

---

## 5. Time Management

### Time Variables Reference

```csharp
// ─── Frame time ───────────────────────────────────────────────────────────────
Time.deltaTime         // Seconds since last Update() (varies per frame)
Time.fixedDeltaTime    // Seconds since last FixedUpdate() (constant, default 0.02s)
Time.unscaledDeltaTime // deltaTime ignoring timeScale (works in pause menus)
Time.unscaledTime      // Total elapsed time since start, ignoring timeScale

// ─── Game time ────────────────────────────────────────────────────────────────
Time.time              // Total elapsed game time since start (respects timeScale)
Time.timeScale         // Multiplier: 0 = paused, 1 = normal, 2 = 2x speed

// ─── Frame counters ───────────────────────────────────────────────────────────
Time.frameCount        // Total frames since start (never resets)
Time.realtimeSinceStartup // Real wall-clock seconds since app launch (never affected by timeScale)
```

### Pause / Slow-Motion System

```csharp
public class TimeManager : MonoBehaviour
{
    [SerializeField] private float slowMotionScale = 0.2f;
    [SerializeField] private float slowMotionDuration = 1.5f;

    public void PauseGame()
    {
        Time.timeScale = 0f;
        AudioListener.pause = true;
    }

    public void ResumeGame()
    {
        Time.timeScale = 1f;
        AudioListener.pause = false;
    }

    public void TriggerSlowMotion()
    {
        StartCoroutine(SlowMotionCoroutine());
    }

    private IEnumerator SlowMotionCoroutine()
    {
        // Enter slow-motion
        Time.timeScale = slowMotionScale;
        Time.fixedDeltaTime = 0.02f * slowMotionScale; // Keep physics accurate

        // Wait in real time (not game time), so duration feels correct
        yield return new WaitForSecondsRealtime(slowMotionDuration);

        // Smoothly ramp back to normal speed
        while (Time.timeScale < 1f)
        {
            Time.timeScale = Mathf.MoveTowards(Time.timeScale, 1f, Time.unscaledDeltaTime * 3f);
            Time.fixedDeltaTime = 0.02f * Time.timeScale;
            yield return null;
        }

        Time.timeScale = 1f;
        Time.fixedDeltaTime = 0.02f;
    }
}
```

### Frame-Rate Independent Cooldown Timer

```csharp
// Countdown timer that works regardless of framerate changes
public class CooldownTimer
{
    private float remaining;
    private readonly float duration;

    public bool IsReady => remaining <= 0f;
    public float Progress => Mathf.Clamp01(1f - remaining / duration);

    public CooldownTimer(float duration) => this.duration = duration;

    // Call this every Update()
    public void Tick(float deltaTime) => remaining = Mathf.Max(0, remaining - deltaTime);

    // Reset the cooldown
    public void Trigger() => remaining = duration;
}

// Usage:
// private CooldownTimer fireCooldown = new CooldownTimer(0.25f);
// void Update() {
//     fireCooldown.Tick(Time.deltaTime);
//     if (Input.GetButton("Fire") && fireCooldown.IsReady) {
//         fireCooldown.Trigger();
//         Shoot();
//     }
// }
```

---

## 6. Invoke & InvokeRepeating (Simple Scheduling)

```csharp
// Call a method once after a delay (no coroutine boilerplate needed)
Invoke("SpawnEnemy", 3f);

// Call a method repeatedly at a fixed interval
InvokeRepeating("SpawnEnemy", startDelay: 2f, repeatRate: 5f);

// Cancel a scheduled invocation
CancelInvoke("SpawnEnemy"); // Cancel specific
CancelInvoke();             // Cancel ALL scheduled calls on this MonoBehaviour

// ⚠️ InvokeRepeating uses string names — typos cause silent failures.
// Prefer coroutines or timers for anything complex.
```

---

## 7. Coroutine vs. async/await — Decision Guide

```
┌────────────────────────────────┬──────────────────────────────────────────┐
│        COROUTINE               │           async / await                  │
├────────────────────────────────┼──────────────────────────────────────────┤
│ ✅ Time-based waits (physics)  │ ✅ I/O: file reads, web requests         │
│ ✅ Frame-by-frame animations   │ ✅ Structured cancellation                │
│ ✅ Unity's yield types work    │ ✅ Cleaner error propagation              │
│ ✅ Stop with StopCoroutine()   │ ✅ Parallel execution with Task.WhenAll   │
│ ❌ Hard to cancel externally   │ ❌ Task.Delay uses system clock, not      │
│ ❌ Stops if MonoBehaviour dies │      Unity's Time.deltaTime               │
│ ❌ No return values            │ ❌ Unity API must stay on main thread     │
└────────────────────────────────┴──────────────────────────────────────────┘
```
