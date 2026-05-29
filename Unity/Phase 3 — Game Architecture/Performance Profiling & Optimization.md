# 📈 Performance Profiling & Optimization
### Unity Profiler, Frame Debugger, Draw Calls & Physics Optimization

Professional game developers write code that keeps frame rates stable and avoids garbage collection overhead. Profiling helps locate actual bottlenecks so you optimize only where it matters.

---

## 1. Profiling Tools

### A. The Unity Profiler (`Window -> Analysis -> Profiler`)
The Profiler tracks resource usage in real-time.
*   **CPU Usage Timeline View**: Displays execution timelines for scripts. Look for `GC.Alloc` spikes. Any allocation in update loops will eventually trigger the garbage collector, freezing frames.
*   **Physics 2D**: Shows active Rigidbodies, overlap query counts, and contact checks.
*   **Memory**: Tracks total allocations. Take a memory snapshot using the **Memory Profiler** package to check for native asset leaks (e.g. textures remaining in RAM).

#### Custom Profiler Markers
Isolate profiling sections in your C# code using the `Unity.Profiling` namespace:
```csharp
using UnityEngine;
using Unity.Profiling; // Required namespace

public class Pathfinder : MonoBehaviour
{
    // Define a static marker for CPU tracing
    private static readonly ProfilerMarker pathfindingMarker = new ProfilerMarker("Enemy.CalculatePath");

    void Update()
    {
        using (pathfindingMarker.Auto())
        {
            // The code in this block will show up as a separate entry in the CPU Profiler timeline
            ExecuteComplexPathfinding();
        }
    }
}
```

### B. The Frame Debugger (`Window -> Analysis -> Frame Debugger`)
Freezes the current frame rendering sequence, letting you step through the individual GPU **Draw Calls** (draw instructions sent by CPU to GPU) to check why objects are not batching.

---

## 2. Rendering Optimization (Draw Call Reduction)

Fewer draw calls mean less CPU-to-GPU context shifting. Group graphics together to draw them in batches.

*   **Sprite Atlases**: A Sprite Atlas packs multiple independent textures into a single large master texture page. This allows Unity to render different sprites using a single draw call.
    *   *Setup:* Create Sprite Atlas asset -> Add your folders of sprites -> Click **Pack Preview**.
*   **SRP Batcher (Universal Render Pipeline / URP)**: Standardizes material data in GPU memory. When using URP, keep materials on the same URP shader. The SRP Batcher updates only the position data per sprite, bypassing expensive material context binding changes.
*   **GPU Instancing**: Enables drawing identical sprite models (e.g. bullets, foliage elements) with varying positions, rotations, and scales in a single draw call. Check the **Enable GPU Instancing** box on the Sprite material.
*   **Avoid Interleaving Layer Orders**: Batching fails if drawing order requires rendering background, foreground, and middleground items in alternating sequence. Keep elements utilizing the same texture sheet adjacent in Sorting Layer and Order in Layer configurations.

---

## 3. CPU & Memory Optimization

*   **Delete Empty Update Methods**: Even if `Update()` is empty, Unity's C++ core registers hook callbacks. Tracing thousands of scripts with empty `Update` declarations drains CPU performance.
    ```csharp
    // BAD: Still incurs overhead
    void Update() {} 
    ```
*   **Pre-size Arrays and Collections**: Set initial capacities for Lists, Dictionaries, and Queues during creation to prevent garbage-producing memory re-allocations on expansion.
    ```csharp
    // GOOD: Holds 50 elements without resizing
    List<Vector2> waypoints = new List<Vector2>(50); 
    ```
*   **Cache Component References**: Never call `GetComponent<T>()` or tag-checking evaluations in loops. Initialize values in `Awake()`.
*   **Use StringBuilder for Dynamic Text**: If string values are frequently edited in update loops (e.g., scoring overlays), use `System.Text.StringBuilder` or format values through updates only.

---

## 4. Physics 2D Optimization

*   **Layer Collision Matrix (`Project Settings -> Physics 2D`)**: Uncheck unnecessary intersection boxes. For example, a `PlayerBullet` layer does not need to run physics checks against other `PlayerBullet` layers.
*   **Composite Collider 2D**: Combine separate tile squares on Tilemaps to form a single continuous outline shape, reducing total collision vertices.
*   **NonAlloc Queries**: Re-use array buffers for spatial searches:
    ```csharp
    // Reuses this buffer across calls, eliminating garbage allocation
    private readonly Collider2D[] resultsBuffer = new Collider2D[10];

    void CheckSurroundings()
    {
        int count = Physics2D.OverlapCircleNonAlloc(transform.position, 2f, resultsBuffer);
    }
    ```

---

## 5. Mobile & Low-End Platform Optimizations

*   **Canvas Slicing (UI Optimization)**: When any UI value updates (like health or timers), Unity reconstructs the entire Canvas geometry mesh.
    *   *The Fix:* Place static background menus on one Canvas, and dynamic HUD indicators (text, score, meters) on a separate **child Canvas**. Modifying the HUD will only rebuild the smaller child mesh.
*   **Target Frame Rate**: Cap the frame rate to prevent mobile battery drainage and heat build-up.
    ```csharp
    void Start()
    {
        QualitySettings.vSyncCount = 0; // Disable VSync
        Application.targetFrameRate = 60; // Cap rate at 60 FPS
    }
    ```
*   **Disable Mipmaps for 2D Sprites**: Mipmaps generate scaled-down versions of textures for distance rendering. Since 2D cameras are orthographic, sprites are drawn at fixed distances. Disabling mipmaps on sprites saves 33% of texture memory.
