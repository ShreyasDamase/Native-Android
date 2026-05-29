# 🧩 MonoBehaviour & Component System
### GameObjects, Querying Components & Object Lifecycle

Unity is built on a **Component-Based Architecture** (Composition over Inheritance). Everything that exists in a scene is a generic **GameObject**, and its custom behaviors, graphics, and physics are defined by attaching modular **Components**.

---

## 1. GameObjects & Components

*   **GameObject:** The base container. It has a name, a tag, a layer, an active state, and a list of attached components. Every GameObject contains at least a `Transform` component.
*   **Component:** The functional classes attached to a GameObject.
*   **MonoBehaviour:** The base class from which every custom script you write in Unity must inherit. This base class enables your script to be attached as a component, exposes fields to the Unity Editor Inspector, and connects into Unity's main thread lifecycle hooks.

---

## 2. Component Querying & Caching Best Practices

Scripts must regularly communicate with other components on the same GameObject or elsewhere in the hierarchy. Querying components incorrectly degrades performance.

### Query Methods

*   **`GetComponent<T>()`**: Searches the active GameObject for a component of type `T`. Returns `null` if not found.
*   **`TryGetComponent<T>(out T component)` (Highly Recommended)**: A faster, safer query that returns a `bool`. It avoids creating internal allocation garbage in the Editor when checking for components that might not exist.
*   **`GetComponentInChildren<T>()`**: Traverses **down** the hierarchy tree to find the component on child objects.
*   **`GetComponentInParent<T>()`**: Traverses **up** the hierarchy tree to find the component on parent objects.

### ⚠️ The Query Rules

1.  **Never call query methods inside Update or FixedUpdate**:
    ```csharp
    // BAD: Querying every frame drains CPU resources
    void Update()
    {
        GetComponent<SpriteRenderer>().color = Color.red; 
    }
    ```
2.  **Cache references in `Awake` or `Start`**:
    ```csharp
    // GOOD: Query once and cache the reference on the heap
    private SpriteRenderer spriteRenderer;

    void Awake()
    {
        spriteRenderer = GetComponent<SpriteRenderer>();
    }

    void Update()
    {
        spriteRenderer.color = Color.red; // Extremely fast cached reference access
    }
    ```
3.  **Avoid `FindObjectOfType<T>()` and `GameObject.Find()`**: These methods search the *entire scene* list of objects, which can freeze the game in large levels. Use event systems, direct inspector configuration, or manager Singletons instead.

---

## 3. MonoBehaviour Lifecycle Flowchart

Unity calls lifecycle methods in a strict, predefined order on the main execution thread. 

```
                                [ AWAKE ]
                    (Called when script instance is loaded)
                                   │
                                   ▼
                              [ ONENABLE ]
                    (Called when object becomes active)
                                   │
                                   ▼
                               [ START ]
               (Called before the first frame Update starts)
                                   │
            ┌──────────────────────┴──────────────────────┐
            ▼                                             ▼
     [ PHYSICS LOOP ]                              [ GAMEPLAY LOOP ]
    (Fixed Timestep)                               (Variable Timestep)
            │                                             │
    ┌───────┴───────┐                                     │
    │  FixedUpdate  │                                     │
    └───────┬───────┘                                     │
            │                                             ▼
            │                                         [ INPUTS ]
            │                                    (Internal input poll)
            │                                             │
            │                                             ▼
            │                                         [ UPDATE ]
            │                                    (Main gameplay frame)
            │                                             │
            │                                             ▼
            │                                       [ LATEUPDATE ]
            │                                    (Camera follow, anims)
            ▼                                             │
    [ PHYSICS SOLVER ]                                    │
    (Internal collisions)                                 │
            │                                             │
            └──────────────────────┬──────────────────────┘
                                   │
                           Loop continues
                                   │
                      (When deactivated/destroyed)
                                   │
                                   ├──────────────────────┐
                                   ▼                      ▼
                              [ ONDISABLE ]          [ ONDESTROY ]
                           (Object disabled)      (Object destroyed)
```

### Hook Details

#### A. Initialization
*   **`Awake()`**: Initialize references and state internal variables. This is called even if the script component is disabled.
*   **`OnEnable()`**: Subscribe to event managers and reset runtime stats. Called every time the object is toggled from inactive to active.
*   **`Start()`**: Query and communicate with *other* GameObjects. At this point, all scene objects have completed `Awake()`, preventing null reference setup errors.

#### B. The Frame Loop
*   **`FixedUpdate()`**: Physics calculations. Runs at a reliable, fixed interval (default 0.02s).
*   **`Update()`**: Reads user keyboard/controller input, ticks timers, and updates UI values. Runs once per frame, rendering-dependent.
*   **`LateUpdate()`**: Camera position adjustments. Runs *after* all `Update()` code is complete, ensuring the target object has finished moving for that frame.

#### C. Editor Hook
*   **`OnValidate()`**: Runs in the Editor when the script is loaded or when a value changes in the Inspector. Perfect for validating configurations.
    ```csharp
    private void OnValidate()
    {
        // Enforce boundaries inside the inspector editor
        speed = Mathf.Max(0.1f, speed); 
    }
    ```

#### D. Teardown
*   **`OnDisable()`**: Unsubscribe from events to prevent memory leaks.
*   **`OnDestroy()`**: Clean up resources (e.g. dynamic materials or manually instantiated meshes) that the Garbage Collector cannot automatically recover.

---

## 4. Customizing Execution Order

By default, Unity does not guarantee whether script `EnemyAI.cs` or `PlayerController.cs` runs its `Update` method first. This can create frame lag or frame-behind bugs.
*   **The Fix:** Go to `Edit -> Project Settings -> Script Execution Order`.
*   Click `+` to add scripts and assign numeric execution values. A lower value (e.g., `-100`) runs earlier; a higher value (e.g., `100`) runs later.

---

## 5. Transform & Hierarchy Management

Every GameObject has a `Transform` component defining its Position, Rotation, and Scale. In Unity, parenting organizes these transforms dynamically.

```
GameObject (Lander Ship) ◄─── Parent (Coordinates: World relative)
  └── Child Object (Thruster Engine) ◄─── Child (Coordinates: Local relative)
```

*   **World Coordinates**: Absolute positions relative to the scene's `(0,0,0)` origin.
*   **Local Coordinates**: Offset positions relative to the parent transform's pivot.
*   **Dynamic Parenting**:
    ```csharp
    // Set object to be a child of another transform
    // Best Practice: Pass true to keep world position; false to snap to local parent origin
    transform.SetParent(parentTransform, worldPositionStays: true);
    
    // Set object back to the root of the scene (no parent)
    transform.SetParent(null);
    ```
*   **Traversing Children**:
    ```csharp
    // Get child by hierarchy path name (expensive, use sparingly)
    Transform muzzle = transform.Find("Visuals/Gun/Muzzle");

    // Loop through all direct children
    foreach (Transform child in transform)
    {
        Debug.Log($"Child name: {child.name}");
    }
    ```
