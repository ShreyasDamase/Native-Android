# 🕹️ Physics & Input & Essential Packages
### A Beginner's Guide to Movement, Inputs, and Packages

Welcome! If you have just started with Unity 2D, the sheer number of code methods, settings, and packages can feel overwhelming. This note is written to explain **what** these components are, **why** they are used, and **how** to write the code step-by-step.

---

## 📦 Part 1: Essential Unity 2D Packages to Install

When you start a new Unity project, it comes with basic rendering features. To build modern 2D games, you must install additional official packages via the **Package Manager** (`Window -> Package Manager` -> Select `Packages: Unity Registry` in the drop-down).

Here are the essential packages you should know and install:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ESSENTIAL 2D PACKAGES                           │
├───────────────────┬────────────────────────────────────────────────────┤
│ Package Name      │ What it does (in plain English)                    │
├───────────────────┼────────────────────────────────────────────────────┤
│ 2D Tilemap Extras │ Adds "Rule Tiles" so you can paint ground grids    │
│                   │ and have corners/edges paint themselves.           │
├───────────────────┼────────────────────────────────────────────────────┤
│ Cinemachine       │ An smart camera follower. It prevents camera       │
│                   │ jitter and handles focus zones automatically.      │
├───────────────────┼────────────────────────────────────────────────────┤
│ 2D Pixel Perfect  │ Stops pixel-art sprites from warping, shearing,    │
│                   │ or flickering when the camera moves.               │
├───────────────────┼────────────────────────────────────────────────────┤
│ 2D SpriteShape    │ Allows drawing organic curved shapes (like rolling │
│                   │ hills, ropes, or grass paths) rather than blocks.  │
├───────────────────┼────────────────────────────────────────────────────┤
│ 2D Animation      │ Allows rigging sprite graphics with virtual bones  │
│                   │ for cut-out animation, instead of drawing frames.   │
├───────────────────┼────────────────────────────────────────────────────┤
│ 2D Aseprite       │ Lets you import .aseprite files directly into      │
│ Importer          │ Unity without exporting them as PNG sheets first.  │
└───────────────────┴────────────────────────────────────────────────────┘
```

### Details & Use Cases

1.  **2D Tilemap Extras (Rule Tiles)**: 
    *   *Why you need it:* If you paint ground blocks manually, you have to choose corner sprites, edge sprites, and fill sprites. A Rule Tile checks adjacent blocks and picks the correct sprite automatically.
2.  **Cinemachine (Dynamic Camera)**:
    *   *Why you need it:* Writing custom camera follow scripts leads to stutters. Cinemachine does the work for you—adding dead zones, tracking smoothing, and camera shake.
3.  **2D Pixel Perfect**:
    *   *Why you need it:* If you move pixel art at subpixel coordinates, the pixels will stretch and look distorted. This package locks the camera viewport calculations to a grid of retro pixels, keeping sprites looking sharp.
4.  **2D SpriteShape**:
    *   *Why you need it:* Tilemaps are locked to square grids. SpriteShape acts like a vector drawing pen; you draw a curved line, and Unity fills it in with grass and dirt texture colliders.
5.  **2D Lights (Universal RP 2D Renderer)**:
    *   *Why you need it:* By default, sprites are unlit. Under the Universal Render Pipeline (URP), you can place 2D spotlights, ambient lights, and normal maps to make your game look moody and high-end.

---

## 🕹️ Part 2: The New Input System (`UnityEngine.InputSystem`)

### Why use the New Input System?
In legacy Unity, you read keys directly in code using `Input.GetKey(KeyCode.Space)`. 
*   **The Problem:** If you want to change keys, add gamepad support, or support mobile touches, you have to rewrite your C# code scripts.
*   **The Solution:** The New Input System separates input keys from your scripts:
    ```
    Spacebar (Keyboard)  ───┐
    Button South (Gamepad)  ├──► "Jump" Action ──► Player Script -> Jump()
    Touchscreen Tap (Mobile)───┘
    ```
    Your script doesn't care *what* key was pressed; it only listens for the "Jump" Action.

---

### 💡 Core Methods & APIs Explained

When you import `UnityEngine.InputSystem`, you will use several key methods. Here is what they actually mean in plain English:

#### 1. `InputSystem.actions`
*   **What it is:** A global lookup dictionary containing all active inputs configured in your project.
*   **When to use it:** When you want a quick way to read keys without writing setup code.
*   **Example:** `InputSystem.actions["Move"]` references the action mapping named "Move".

#### 2. `.ReadValue<T>()`
*   **What it does:** Reads the current value of the input.
*   **What `<T>` means:** This is a C# Generic. You replace `T` with the type of data you expect:
    *   `ReadValue<Vector2>()`: Used for movement joysticks or WASD keys. Returns a direction offset like `(x: 1, y: 0)`.
    *   `ReadValue<float>()`: Used for analog triggers (e.g. throttle). Returns a value between `0.0` (not pressed) and `1.0` (pressed all the way).
*   **Example:**
    ```csharp
    // Reads keyboard directions. If pressing W and D, moveDirection will be (1.0f, 1.0f)
    Vector2 moveDirection = InputSystem.actions["Move"].ReadValue<Vector2>();
    ```

#### 3. `.IsPressed()`
*   **What it does:** Returns `true` continuously as long as the button is held down, and `false` when it is not.
*   **When to use it:** Ideal for continuous actions, like firing a machine gun, sprinting, or applying constant rocket engine thrust.
*   **Example:**
    ```csharp
    if (InputSystem.actions["Thrust"].IsPressed())
    {
        ApplyEngineThrust(); // Runs every frame the key is held down
    }
    ```

#### 4. `.WasPressedThisFrame()`
*   **What it does:** Returns `true` **only once** on the exact frame the user pressed the key down. If they hold it, it returns `false` on subsequent frames.
*   **When to use it:** Ideal for one-shot actions where you want to prevent rapid repeated actions, like opening menus, pausing, or jumping.
*   **Example:**
    ```csharp
    if (InputSystem.actions["Jump"].WasPressedThisFrame())
    {
        TriggerJumpImpulse(); // Only jumps once per key tap
    }
    ```

#### 5. `.WasReleasedThisFrame()`
*   **What it does:** Returns `true` **only once** on the exact frame the user lets go of the button.
*   **When to use it:** Ideal for actions that occur upon release, like releasing a charged weapon shot or ending a jump early to cut jump height.

#### 6. `InputAction.CallbackContext`
*   **What it is:** A packet of event details sent by Unity when an event-based action triggers.
*   **When to use it:** When using callbacks (Approach B & C below). It tells you the state of the interaction:
    *   `context.started`: The user just started pressing the button.
    *   `context.performed`: The action was completed (e.g., button pressed all the way, or held for a charge).
    *   `context.canceled`: The user let go of the button.
    *   `context.ReadValue<T>()`: Reads the value inside the event callback.
*   **Example:**
    ```csharp
    public void OnJumpInput(InputAction.CallbackContext context)
    {
        if (context.performed) // Has the jump key been pressed down?
        {
            PerformJump();
        }
    }
    ```

---

## 🛠️ Part 3: Step-by-Step Implementation Tutorials

Here is how to set up the code step-by-step using the three main approaches.

### Approach A: Direct Polling (easiest for beginners)
This approach reads values directly in `Update()` without writing complex setup scripts. Recommended for prototyping.

```csharp
using UnityEngine;
using UnityEngine.InputSystem; // 1. Always import this namespace

[RequireComponent(typeof(Rigidbody2D))]
public class SimplePlayerController : MonoBehaviour
{
    private Rigidbody2D rb;
    private Vector2 movementDirection;
    private bool isThrusting;
    private bool jumpRequested;

    [SerializeField] private float walkSpeed = 5f;
    [SerializeField] private float jumpForce = 8f;

    private void Awake()
    {
        // Cache the Rigidbody2D component so we don't have to query it repeatedly
        rb = GetComponent<Rigidbody2D>();
    }

    private void Update()
    {
        // 2. Read joystick / WASD coordinates (returns x and y offsets)
        movementDirection = InputSystem.actions["Move"].ReadValue<Vector2>();

        // 3. Check if Space/Button is currently held down
        isThrusting = InputSystem.actions["Thrust"].IsPressed();

        // 4. Check if Space/Button was tapped on this exact frame
        if (InputSystem.actions["Jump"].WasPressedThisFrame())
        {
            jumpRequested = true;
        }
    }

    private void FixedUpdate()
    {
        // 5. Apply horizontal walking velocity (independent of framerate)
        rb.linearVelocity = new Vector2(movementDirection.x * walkSpeed, rb.linearVelocity.y);

        // 6. Apply constant thrust if held
        if (isThrusting)
        {
            rb.AddForce(transform.up * 10f, ForceMode2D.Force);
        }

        // 7. Apply instant jump force if tapped
        if (jumpRequested)
        {
            rb.AddForce(Vector2.up * jumpForce, ForceMode2D.Impulse);
            jumpRequested = false; // Reset the flag
        }
    }
}
```

---

### Approach B: C# Wrapper Events (best for clean, larger games)
This approach generates a type-safe script from your input settings asset, letting you subscribe to event notifications rather than checking values every frame.

1.  Select your input actions asset (e.g. `PlayerControls.inputactions`).
2.  Check the **Generate C# Class** box in the inspector, click **Apply**.
3.  Write the script:

```csharp
using UnityEngine;
using UnityEngine.InputSystem;

public class EventPlayerController : MonoBehaviour
{
    private Rigidbody2D rb;
    
    // This is the class generated automatically by Unity
    private PlayerControls inputActions;
    private Vector2 moveInput;

    private void Awake()
    {
        rb = GetComponent<Rigidbody2D>();
        inputActions = new PlayerControls();
    }

    private void OnEnable()
    {
        // Activate the input maps
        inputActions.Enable();

        // Subscribe to the Jump action's performed event (when it is pressed)
        inputActions.Player.Jump.performed += OnJumpTriggered;
    }

    private void OnDisable()
    {
        // ALWAYS unsubscribe from events when disabling to prevent memory leaks!
        inputActions.Player.Jump.performed -= OnJumpTriggered;
        
        // Disable the input maps
        inputActions.Disable();
    }

    private void Update()
    {
        // Read movement values continuously
        moveInput = inputActions.Player.Move.ReadValue<Vector2>();
    }

    private void FixedUpdate()
    {
        rb.linearVelocity = new Vector2(moveInput.x * 5f, rb.linearVelocity.y);
    }

    // This method is called automatically when the Jump button is pressed
    private void OnJumpTriggered(InputAction.CallbackContext context)
    {
        rb.AddForce(Vector2.up * 8f, ForceMode2D.Impulse);
    }
}
```

---

### Approach C: `PlayerInput` Component (best for local multiplayer)
This approach uses a visual inspector component to hook inputs up to your scripts.

1.  Add the **Player Input** component to your Player GameObject.
2.  Drag your `PlayerControls` input asset into the **Actions** slot.
3.  Set **Behavior** to `Invoke Unity Events`.
4.  Write the script, then drag the script into the event list on the component:

```csharp
using UnityEngine;
using UnityEngine.InputSystem;

public class VisualInputController : MonoBehaviour
{
    private Rigidbody2D rb;
    private Vector2 moveVector;

    private void Awake() => rb = GetComponent<Rigidbody2D>();

    // 1. Drag this method into the "Move" Event slot in the Inspector
    public void OnMove(InputAction.CallbackContext context)
    {
        moveVector = context.ReadValue<Vector2>();
    }

    // 2. Drag this method into the "Jump" Event slot in the Inspector
    public void OnJump(InputAction.CallbackContext context)
    {
        // context.performed checks if the button transition was pressed down
        if (context.performed)
        {
            rb.AddForce(Vector2.up * 8f, ForceMode2D.Impulse);
        }
    }

    private void FixedUpdate()
    {
        rb.linearVelocity = new Vector2(moveVector.x * 5f, rb.linearVelocity.y);
    }
}
```
