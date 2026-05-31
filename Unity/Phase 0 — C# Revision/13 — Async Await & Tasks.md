# 13 — Async Await & Tasks
### 🔴 Block B — Unity-Critical C# Features

> [!NOTE]
> `async/await` is C#'s modern concurrency system. Unity 6 now has first-class `async/await` support via `Awaitable`. Use coroutines for **gameplay timers and frame-level logic**. Use async/await for **I/O, web requests, file loading, and Unity 6 features**.

---

## 13.1 — Coroutine vs. Async/Await — When to Use Which

| Situation | Use |
| :--- | :--- |
| Wait for seconds / frames | Coroutine (`WaitForSeconds`) |
| Spawn wave, UI animation | Coroutine |
| Load a file from disk | `async/await` |
| Web request (UnityWebRequest) | `async/await` |
| Load a scene asynchronously | `async/await` (Unity 6) or Coroutine |
| Parallel background work | `async/await` + `Task.Run` |
| AI behavior trees (advanced) | `async/await` |

---

## 13.2 — async/await Basics

```csharp
using System.Threading;
using System.Threading.Tasks;
using UnityEngine;

public class DataLoader : MonoBehaviour
{
    // 'async' marks this method as asynchronous
    // 'Task' is the return type (like void but awaitable)
    async Task LoadDataAsync()
    {
        Debug.Log("Starting load...");
        
        // 'await' pauses HERE and gives control back to the caller
        // Other code keeps running while we wait
        await Task.Delay(2000); // non-blocking 2 second wait
        
        Debug.Log("Load complete!");  // continues here after 2s
    }
    
    // Return Task<T> when the async method produces a value
    async Task<string> FetchPlayerNameAsync(int playerID)
    {
        // Simulate web request
        await Task.Delay(500);
        return $"Player_{playerID}";
    }
    
    // async void — ONLY for event handlers (fire-and-forget, can't be awaited)
    async void OnButtonClicked()
    {
        // ⚠️ Errors in async void are unhandled — use carefully
        await LoadDataAsync();
    }
    
    // Start is called by Unity — you can make it async in Unity 2023+
    async void Start()
    {
        string name = await FetchPlayerNameAsync(42);
        Debug.Log($"Player name: {name}");
    }
}
```

---

## 13.3 — CancellationToken — Stop Async Tasks Cleanly

The most important safety concept with async code — always pass a `CancellationToken` so the operation can be cancelled when the GameObject is destroyed.

```csharp
public class AsyncEnemy : MonoBehaviour
{
    // CancellationTokenSource — generates tokens to cancel linked tasks
    private CancellationTokenSource cts;
    
    async void Start()
    {
        // Create a new cancellation source
        cts = new CancellationTokenSource();
        
        try
        {
            await RunAIAsync(cts.Token);
        }
        catch (OperationCanceledException)
        {
            Debug.Log("AI task was cancelled cleanly.");
        }
    }
    
    void OnDestroy()
    {
        // Cancel all tasks when this object is destroyed — prevents errors!
        cts?.Cancel();
        cts?.Dispose();
    }
    
    async Task RunAIAsync(CancellationToken token)
    {
        while (!token.IsCancellationRequested)
        {
            await PatrolAsync(token);
            
            if (CanSeePlayer())
                await ChaseAsync(token);
        }
    }
    
    async Task PatrolAsync(CancellationToken token)
    {
        foreach (Vector3 waypoint in patrolPoints)
        {
            token.ThrowIfCancellationRequested(); // check between each waypoint
            await MoveToAsync(waypoint, token);
            await Task.Delay(1000, token); // wait at waypoint
        }
    }
    
    async Task MoveToAsync(Vector3 target, CancellationToken token)
    {
        while (Vector3.Distance(transform.position, target) > 0.1f)
        {
            token.ThrowIfCancellationRequested();
            transform.position = Vector3.MoveTowards(transform.position, target, speed * Time.deltaTime);
            await Task.Yield(); // wait one frame
        }
    }
}
```

---

## 13.4 — Unity 6: `Awaitable` (Unity's Native Async)

Unity 6 introduced `Awaitable` — a Unity-native async type that is more efficient than `Task` because it's pooled and doesn't allocate.

```csharp
// Unity 6+ — use Awaitable instead of Task for Unity-native async
public class SceneLoader : MonoBehaviour
{
    // Awaitable instead of Task
    async Awaitable LoadSceneAsync(string sceneName, CancellationToken token)
    {
        // Wait one frame (Unity-native, zero allocation)
        await Awaitable.NextFrameAsync(token);
        
        // Wait for a fixed update step
        await Awaitable.FixedUpdateAsync(token);
        
        // Wait for end of frame (useful for screenshots, render texture capture)
        await Awaitable.EndOfFrameAsync(token);
        
        // Load scene async
        var op = UnityEngine.SceneManagement.SceneManager.LoadSceneAsync(sceneName);
        while (!op.isDone)
        {
            float progress = op.progress;
            OnLoadProgress?.Invoke(progress);
            await Awaitable.NextFrameAsync(token);
        }
        
        OnLoadComplete?.Invoke();
    }
    
    async Awaitable<T> LoadAssetAsync<T>(string path, CancellationToken token) where T : UnityEngine.Object
    {
        var handle = UnityEngine.AddressableAssets.Addressables.LoadAssetAsync<T>(path);
        await handle.Task;
        return handle.Result;
    }
    
    public event System.Action<float> OnLoadProgress;
    public event System.Action OnLoadComplete;
}
```

---

## 13.5 — Running Background Work (Off Main Thread)

```csharp
// Task.Run — moves work to a background thread (NOT Unity main thread)
// ⚠️ NEVER access Unity APIs (Transform, GameObject etc.) from background thread!

public class HeavyComputation : MonoBehaviour
{
    async void Start()
    {
        Debug.Log("Starting heavy computation...");
        
        // Run on background thread — doesn't freeze the game
        int[] results = await Task.Run(() => ComputePathfindingGrid(100, 100));
        
        // Back on main thread here — safe to use Unity APIs
        ApplyGridToTilemap(results);
        Debug.Log("Grid applied!");
    }
    
    // This runs on a background thread — pure C#, no Unity calls
    int[] ComputePathfindingGrid(int width, int height)
    {
        int[] grid = new int[width * height];
        
        for (int x = 0; x < width; x++)
        {
            for (int y = 0; y < height; y++)
            {
                // Heavy computation — Perlin noise, flood fill, etc.
                grid[x * height + y] = CalculateCell(x, y);
            }
        }
        
        return grid;
    }
    
    int CalculateCell(int x, int y)
    {
        // Use System.Math, not Mathf (Mathf may not be thread-safe)
        return (int)(System.Math.Sin(x * 0.1) + System.Math.Cos(y * 0.1));
    }
}
```

---

## 13.6 — Running Tasks in Parallel

```csharp
// Sequential — total time = A + B + C
async Task LoadSequential()
{
    await LoadTextures();     // 2s
    await LoadAudio();        // 1s
    await LoadLevelData();    // 3s
    // Total: 6 seconds
}

// Parallel — total time = max(A, B, C)
async Task LoadParallel()
{
    await Task.WhenAll(
        LoadTextures(),       // 2s ┐
        LoadAudio(),          // 1s ├ all run simultaneously
        LoadLevelData()       // 3s ┘
    );
    // Total: 3 seconds
}

// WhenAny — continue when the FIRST task completes
async Task<string> GetFastestServer()
{
    Task<string> server1 = PingServer("https://server1.example.com");
    Task<string> server2 = PingServer("https://server2.example.com");
    Task<string> server3 = PingServer("https://server3.example.com");
    
    Task<string> fastest = await Task.WhenAny(server1, server2, server3);
    return await fastest; // get the result of the first completed task
}
```

---

## 13.7 — UnityWebRequest with async/await

```csharp
using UnityEngine.Networking;

public class LeaderboardManager : MonoBehaviour
{
    private CancellationTokenSource cts;
    
    void OnEnable()  => cts = new CancellationTokenSource();
    void OnDisable() { cts?.Cancel(); cts?.Dispose(); }
    
    async Task<string> GetLeaderboardAsync(string url, CancellationToken token)
    {
        using UnityWebRequest request = UnityWebRequest.Get(url);
        
        // Send and await completion
        await request.SendWebRequest();
        
        if (token.IsCancellationRequested)
            throw new OperationCanceledException(token);
        
        if (request.result != UnityWebRequest.Result.Success)
            throw new System.Exception($"Web request failed: {request.error}");
        
        return request.downloadHandler.text;
    }
    
    async Task PostScoreAsync(string url, int score, string playerName, CancellationToken token)
    {
        string json = JsonUtility.ToJson(new ScoreData { score = score, name = playerName });
        byte[] bodyRaw = System.Text.Encoding.UTF8.GetBytes(json);
        
        using UnityWebRequest request = new UnityWebRequest(url, "POST");
        request.uploadHandler = new UploadHandlerRaw(bodyRaw);
        request.downloadHandler = new DownloadHandlerBuffer();
        request.SetRequestHeader("Content-Type", "application/json");
        
        await request.SendWebRequest();
        
        if (request.result != UnityWebRequest.Result.Success)
            Debug.LogError($"Score post failed: {request.error}");
        else
            Debug.Log("Score posted successfully!");
    }
    
    async void OnPlayButtonClicked()
    {
        try
        {
            string data = await GetLeaderboardAsync("https://api.example.com/scores", cts.Token);
            DisplayLeaderboard(data);
        }
        catch (OperationCanceledException)
        {
            Debug.Log("Request cancelled — object probably destroyed.");
        }
        catch (System.Exception e)
        {
            Debug.LogError($"Leaderboard error: {e.Message}");
        }
    }
    
    [System.Serializable]
    class ScoreData { public int score; public string name; }
}
```

---

## 13.8 — async/await vs Coroutine — Side-by-Side

```csharp
// ===================== COROUTINE VERSION =====================
IEnumerator FadeInCoroutine(CanvasGroup group, float duration)
{
    group.alpha = 0f;
    group.gameObject.SetActive(true);
    float elapsed = 0f;
    
    while (elapsed < duration)
    {
        elapsed += Time.deltaTime;
        group.alpha = Mathf.Clamp01(elapsed / duration);
        yield return null;
    }
    
    group.alpha = 1f;
}
// Start: StartCoroutine(FadeInCoroutine(group, 1f));

// ===================== ASYNC VERSION =====================
async Awaitable FadeInAsync(CanvasGroup group, float duration, CancellationToken token)
{
    group.alpha = 0f;
    group.gameObject.SetActive(true);
    float elapsed = 0f;
    
    while (elapsed < duration)
    {
        elapsed += Time.deltaTime;
        group.alpha = Mathf.Clamp01(elapsed / duration);
        await Awaitable.NextFrameAsync(token); // Unity 6
    }
    
    group.alpha = 1f;
}
// Start: await FadeInAsync(group, 1f, cts.Token);
```

---

## 📝 Summary

| Concept | Key Point |
| :--- | :--- |
| `async Task` | Awaitable method that does work and may return |
| `async Task<T>` | Awaitable method that returns a value of type T |
| `async void` | Fire-and-forget — only for event handlers |
| `await` | Pause execution until the Task completes |
| `CancellationToken` | Always use — lets you cancel when object is destroyed |
| `Task.Run()` | Execute on background thread — NO Unity API calls there |
| `Task.WhenAll()` | Run multiple tasks in parallel, wait for all |
| `Task.WhenAny()` | Wait for the first of multiple tasks |
| `Awaitable` | Unity 6's native async type — zero allocation, use over Task |

**Previous:** [[12 — Coroutines & IEnumerator]] | **Next:** [[14 — Attributes & Reflection]]
