# 27 — Architecture — MVC, MVP & MVVM in Unity
### 🔶 Block F — Architecture & Best Practices

> [!IMPORTANT]
> If you come from Android, you are used to MVVM (Model-View-ViewModel) with LiveData, Flow, and Data Binding. Unity's standard UI (uGUI) **does not have built-in data binding**. Therefore, forcing strict Android-style MVVM into standard Unity often results in massive overhead and boilerplate. 

---

## 27.1 — Why Unity Architecture is Different

In Android:
- **Activity/Fragment** = The View.
- **ViewModel** = Survives configuration changes, holds StateFlow/LiveData.
- **Data Binding / Compose** = Automatically updates the UI when StateFlow changes.

In Unity:
- **MonoBehaviour** = By default, it acts as the View, the Controller, AND the Model all at once. This leads to the infamous "God Class" anti-pattern (a `Player.cs` that handles input, physics, health, audio, and updating the UI).
- **Data Binding** = Doesn't exist in standard uGUI. You have to manually write `healthText.text = newHealth.ToString()`. *(Note: Unity's newer UI Toolkit does have some binding features, but uGUI is still dominant for in-game UI).*

Because we lack auto-binding, the **Model-View-Presenter (MVP)** pattern is usually the cleanest choice for Unity UI and gameplay systems.

---

## 27.2 — The Problem: The "God" MonoBehaviour (Anti-Pattern)

```csharp
// ❌ WRONG: How beginners write Unity code
// This script knows about input, physics, data, and UI rendering.
// It is impossible to unit test and hard to reuse.

public class Player : MonoBehaviour
{
    public int health = 100;
    public TMPro.TextMeshProUGUI healthText; // View dependency inside logic!
    
    void Update()
    {
        if (Input.GetKeyDown(KeyCode.Space)) // Input
        {
            health -= 10;                    // Data modification
            healthText.text = "HP: " + health; // UI updating
        }
    }
}
```

---

## 27.3 — MVP (Model-View-Presenter) in Unity

In MVP, we separate the logic from the Unity Engine dependencies.

1. **Model:** Pure C# class (or ScriptableObject). Holds data. Knows NOTHING about Unity.
2. **View:** A `MonoBehaviour`. Only handles rendering (Canvas, Text, Animations) and capturing input. Knows nothing about game rules.
3. **Presenter:** A pure C# class. Listens to the View's input, modifies the Model, and tells the View what to display.

### Step 1: The Model (Pure C#)
```csharp
// Pure C# class. Highly testable. No MonoBehaviour.
public class PlayerModel
{
    public int Health { get; private set; }
    
    public event System.Action<int> OnHealthChanged;
    public event System.Action OnDied;
    
    public PlayerModel(int startHealth)
    {
        Health = startHealth;
    }
    
    public void TakeDamage(int amount)
    {
        Health -= amount;
        OnHealthChanged?.Invoke(Health);
        
        if (Health <= 0)
            OnDied?.Invoke();
    }
}
```

### Step 2: The View Interface & View Implementation
```csharp
// Interface allows the Presenter to talk to the View without knowing it's a MonoBehaviour
public interface IPlayerView
{
    void UpdateHealthDisplay(int currentHealth);
    void PlayDeathAnimation();
    event System.Action OnDamageRequested; // Input event
}

// The actual MonoBehaviour attached to the GameObject
public class PlayerView : MonoBehaviour, IPlayerView
{
    [SerializeField] private TMPro.TextMeshProUGUI healthText;
    [SerializeField] private Animator animator;
    
    public event System.Action OnDamageRequested;
    
    void Update()
    {
        // Capture input, but don't process logic! Just pass it up.
        if (Input.GetKeyDown(KeyCode.Space))
        {
            OnDamageRequested?.Invoke();
        }
    }
    
    // Called by the Presenter
    public void UpdateHealthDisplay(int currentHealth)
    {
        healthText.text = $"HP: {currentHealth}";
    }
    
    public void PlayDeathAnimation()
    {
        animator.SetTrigger("Die");
    }
}
```

### Step 3: The Presenter (The Brain)
```csharp
// Pure C# class. Glues Model and View together.
public class PlayerPresenter
{
    private readonly PlayerModel model;
    private readonly IPlayerView view;
    
    public PlayerPresenter(PlayerModel model, IPlayerView view)
    {
        this.model = model;
        this.view = view;
        
        // 1. Subscribe to View inputs
        this.view.OnDamageRequested += HandleDamageRequest;
        
        // 2. Subscribe to Model changes
        this.model.OnHealthChanged += this.view.UpdateHealthDisplay;
        this.model.OnDied += this.view.PlayDeathAnimation;
        
        // 3. Initial View setup
        this.view.UpdateHealthDisplay(this.model.Health);
    }
    
    private void HandleDamageRequest()
    {
        // Business logic happens here
        model.TakeDamage(10); 
    }
    
    public void Cleanup()
    {
        // Always unsubscribe to prevent memory leaks!
        view.OnDamageRequested -= HandleDamageRequest;
        model.OnHealthChanged -= view.UpdateHealthDisplay;
        model.OnDied -= view.PlayDeathAnimation;
    }
}
```

### Step 4: The Bootstrapper (Composition Root)
Someone has to create the Model and Presenter and wire them to the View. In Unity, this is usually a manager or a simple bootstrapper script on the same GameObject.

```csharp
public class PlayerBootstrapper : MonoBehaviour
{
    [SerializeField] private PlayerView view; // Drag in Inspector
    
    private PlayerPresenter presenter;
    
    void Start()
    {
        // Initialize the triad
        PlayerModel model = new PlayerModel(100);
        presenter = new PlayerPresenter(model, view);
    }
    
    void OnDestroy()
    {
        presenter?.Cleanup();
    }
}
```

---

## 27.4 — MVVM in Unity (UniRx / UniTask)

If you REALLY want Android-style MVVM in Unity, you need to use a Reactive library. The industry standard for this is **UniRx** (Reactive Extensions for Unity). It gives you something very similar to Kotlin Flows.

```csharp
// Requires the UniRx package
using UniRx;

public class PlayerViewModel
{
    // ReactiveProperty = similar to MutableStateFlow / MutableLiveData
    public ReactiveProperty<int> Health = new ReactiveProperty<int>(100);
    
    public void TakeDamage(int amount)
    {
        Health.Value -= amount;
    }
}

public class PlayerView : MonoBehaviour
{
    [SerializeField] private TMPro.TextMeshProUGUI healthText;
    private PlayerViewModel viewModel;
    
    void Start()
    {
        viewModel = new PlayerViewModel();
        
        // Data Binding! Automatically updates UI when Health changes
        // 'AddTo' ensures the subscription is cancelled when this GameObject is destroyed (prevents leaks)
        viewModel.Health
            .Subscribe(newHealth => healthText.text = $"HP: {newHealth}")
            .AddTo(this);
    }
    
    void Update()
    {
        if (Input.GetKeyDown(KeyCode.Space))
            viewModel.TakeDamage(10);
    }
}
```

> [!TIP]
> **Should I use UniRx?** If you are building a UI-heavy game (card game, management sim, gacha menu system), UniRx + MVVM is incredible. If you are building an action platformer or FPS, MVP or standard Component-based design is usually faster and cleaner.

---

## 📝 Summary 

1. **Don't put UI references in your physics/gameplay scripts.** 
2. **MVP** is the most native-feeling clean architecture for standard Unity uGUI.
3. Keep your **Models** as pure C# classes (or ScriptableObjects). They should not inherit from `MonoBehaviour`.
4. Your **Views** (`MonoBehaviours`) should only handle `GetComponent`, `Input`, and updating `UI.Text`. They contain ZERO game math.
5. If you miss Android's `LiveData` and data binding, install **UniRx** and use the MVVM pattern.

**Next:** [[28 — The ScriptableObject Architecture]]
