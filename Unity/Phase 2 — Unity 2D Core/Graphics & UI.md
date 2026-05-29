# 🖼️ Graphics & UI
### Sprites, Animation, Cinemachine, UI Canvas, Tilemaps, VFX & Audio

A professional game environment requires visual layering, camera framing, responsive UI configurations, painted grid levels, and spatialized audio.

---

## 1. Sprites & Rendering Order

2D depth is managed through sorting configurations rather than physical Z coordinate positions:

*   **SpriteRenderer**: The core component that draws the sprite.
*   **Sorting Layers**: Global sorting categories (configured in `Tags and Layers`). Example order: `Background` -> `Default` -> `Middleground` -> `Enemies` -> `Player` -> `Foreground` -> `UI`.
*   **Order in Layer**: Fine-tuned rendering order within the same layer. Higher integers draw on top of lower integers.
*   **Sorting Group**: Attached to composite character parent objects. It groups child sprite layers (head, arm, weapon) together, forcing Unity to render them as a single object relative to other sprites, preventing visual interleaving bugs.

---

## 2. Animation Mecanim State Machine

Unity's **Animator Controller** maps sprite animations to a visual finite state machine driven by script parameters.

```
                  ┌──────────────┐
                  │    Spawn     │  (Default State)
                  └──────┬───────┘
                         │
                         ▼
                  ┌──────────────┐
                  │     Idle     │
                  └─┬──────────▲─┘
     isMoving=true  │          │  isMoving=false
     (Transition)   ▼          │ (Transition)
                  ┌──────────────┐
                  │     Walk     │
                  └──────────────┘
```

*   **Parameters**: Variables driving transitions (`Float`, `Int`, `Bool`, `Trigger`).
*   **Transition Settings**: Uncheck **Has Exit Time** for immediate transitions (e.g. going from walking to jumping instant-response). Set **Transition Duration** to `0` for 2D sprites to skip frame blending.
*   **Animator Override Controllers**: Reuses an existing animator state layout but swaps out the source `.anim` clips (essential for characters that share movement layouts but use different skins or weapons).

```csharp
private Animator animator;
private Rigidbody2D rb;

void Awake()
{
    animator = GetComponent<Animator>();
    rb = GetComponent<Rigidbody2D>();
}

void Update()
{
    // Pass movement speed to drive walk transitions
    float currentSpeed = Mathf.Abs(rb.linearVelocity.x);
    animator.SetFloat("speed", currentSpeed);
}
```

---

## 3. Cinemachine Cameras (Cinemachine v3)

Modern camera scripting leverages Cinemachine. *Note:* Cinemachine v3 (standard in Unity 6) replaces `CinemachineVirtualCamera` with the unified `CinemachineCamera` component.

*   **Follow / LookAt**: Assign your Player Transform here.
*   **Damping**: Controls camera latency. Lower values make the camera stickier; higher values allow smooth camera lag behind the player.
*   **Dead Zone**: The center-screen box area. The player can move freely within this boundary without forcing the camera to shift.
*   **Cinemachine Confiner 2D**: Keeps the camera from showing bounds outside your level. Attach a `PolygonCollider2D` outlining the level bounds, and drag it into the Confiner slot.
*   **Cinemachine Impulse Listener**: Allows the camera to react to camera shake events (e.g. rocket thrust or crashes).

---

## 4. UI Canvas & TextMeshPro (TMP)

UI components must reside within a parent **Canvas** GameObject.

```
Canvas (Render Mode: Screen Space - Overlay)
  ├── Canvas Scaler (UI Scale Mode: Scale With Screen Size)
  └── TextMeshPro - Text (Anchors: Top-Left, Pivot: 0, 1)
```

*   **Render Modes**:
    1.  **Screen Space - Overlay**: Rendered on top of the viewport (HUD, menus, score bars).
    2.  **Screen Space - Camera**: Similar to overlay, but renders at a specific distance from the camera (allows 3D particles or game elements to pass in front of UI).
    3.  **World Space**: Renders directly in the scene (damage text, NPC dialogue bubbles).
*   **Canvas Scaler**: Always set **UI Scale Mode** to `Scale With Screen Size`. Define a reference resolution (e.g. `1920x1080`) and set **Match** to `0.5` (equally scaling by width and height) to make UI look consistent across mobile/PC monitors.
*   **TextMeshPro (TMP)**: Vector-based SDF rendering. Use rich text tags in script text updates:
    ```csharp
    scoreText.text = $"Score: <color=yellow>{newScore}</color> | <size=120%>COMBO!</size>";
    ```

---

## 5. Tilemaps & Rule Tiles

Tilemaps paint grid environments efficiently:

*   **Tilemap Renderer**: Renders tile graphics.
*   **Tilemap Collider 2D + Composite Collider 2D**: Generates solid colliders around painted grid cells.
    *   *Optimization:* Attach `Composite Collider 2D` to merge adjacent grid squares into a single outline collider. Set the Tilemap Collider's **Used By Composite** property to true. This stops the player from snagging on interior seams between grid cells.
*   **Rule Tiles**: Automatically calculates sprite selection depending on neighboring tile configurations (draws grass on borders, dirt inside).

---

## 6. VFX: Particle Systems

The `Particle System` component is an efficient C++ sub-engine used to spawn dynamic details (thrust fire, sparks, dust).

*   **Modules**:
    *   **Emission**: Configures spawn rates or bursts.
    *   **Shape**: Spawns particles inside cone, box, or circle shapes.
    *   **Size over Lifetime / Color over Lifetime**: Interpolates size and transparency to zero over existence, simulating smoke dispersal.
*   **Optimization**: Check **Max Particles** and select **Culling Mode** = `Pause` or `Automatic` to save CPU resources when the particle emitter goes off-screen.

---

## 7. Audio System & Routing

Manage audio layers using the `AudioMixer` routing tree:

```
                  ┌───────────────────────────────┐
                  │          AUDIO MIXER          │
                  │             Master            │
                  └───────┬───────────────┬───────┘
                          ▼               ▼
                       Effects          Music
```

*   **AudioListener**: The receiver (place only one in the scene, usually on the Main Camera).
*   **AudioSource**: Renders clips. Uncheck **Play On Awake** for event-based sounds. Set **Spatial Blend** to `1.0` for spatial 3D audio or `0.0` for 2D UI audio.
*   **Mixer Snapshot Scripting**: Mix volumes dynamically (e.g., dampening music when the game is paused).

```csharp
[SerializeField] private AudioMixer mixer;
[SerializeField] private AudioMixerSnapshot gamePlayingSnapshot;
[SerializeField] private AudioMixerSnapshot gamePausedSnapshot;

public void TogglePause(bool isPaused)
{
    // Blend audio mixer properties smoothly over 0.5 seconds
    if (isPaused)
    {
        gamePausedSnapshot.TransitionTo(0.5f);
    }
    else
    {
        gamePlayingSnapshot.TransitionTo(0.5f);
    }
}
```
