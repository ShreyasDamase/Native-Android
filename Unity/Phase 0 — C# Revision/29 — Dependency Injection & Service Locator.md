# 29 — Dependency Injection & Service Locator
### 🔶 Block F — Architecture & Best Practices

> [!NOTE]
> In Android, you use Dagger or Hilt to provide dependencies (like Repositories or APIs) to your ViewModels. In Unity, beginners use `FindObjectOfType` or massive `Singletons`. As your game grows, these become a nightmare. Here is how professional Unity games handle dependencies.

---

## 29.1 — The Problem: How Scripts Find Each Other

Imagine an `Enemy.cs` that needs to play a sound when it dies. It needs the `AudioManager`. How does it get it?

### ❌ The Beginner Way: `FindObjectOfType`
```csharp
void Die()
{
    // Extremely slow! Scans the entire scene hierarchy.
    // Tightly couples Enemy to AudioManager.
    FindObjectOfType<AudioManager>().PlaySound("Death"); 
}
```

### ❌ The "Intermediate" Way: The Singleton
```csharp
void Die()
{
    // Fast, but creates global state.
    // What if we want 2 AudioManagers (Music vs SFX)? We can't.
    // What if AudioManager.Instance is destroyed? Null reference!
    AudioManager.Instance.PlaySound("Death");
}
```

### ⚠️ The ScriptableObject Way (See File 28)
Use a Void Event Channel SO. (Great for events, but bad if `Enemy` actually needs to call a method that *returns* a value from `AudioManager`).

---

## 29.2 — The Service Locator Pattern (Simple DI)

A Service Locator is a global dictionary of interfaces. It provides the benefits of Singletons without locking you into concrete implementations.

### 1. Define the Interface
```csharp
public interface IAudioService
{
    void PlaySound(string soundName);
}
```

### 2. Create the Locator
```csharp
using System;
using System.Collections.Generic;

public static class ServiceLocator
{
    private static readonly Dictionary<Type, object> services = new Dictionary<Type, object>();

    public static void Register<T>(T service)
    {
        services[typeof(T)] = service;
    }

    public static T Get<T>()
    {
        if (services.TryGetValue(typeof(T), out object service))
        {
            return (T)service;
        }
        throw new Exception($"Service {typeof(T)} not found.");
    }
    
    public static void Clear() => services.Clear();
}
```

### 3. Register the Service (At boot)
```csharp
public class AudioManager : MonoBehaviour, IAudioService
{
    void Awake()
    {
        // Register myself as the IAudioService provider
        ServiceLocator.Register<IAudioService>(this);
    }
    
    public void PlaySound(string name) { /* play audio */ }
}
```

### 4. Use the Service
```csharp
public class Enemy : MonoBehaviour
{
    private IAudioService audioService;
    
    void Start()
    {
        // Fetch dependency at start
        audioService = ServiceLocator.Get<IAudioService>();
    }
    
    void Die()
    {
        audioService.PlaySound("Death");
    }
}
```

**Why this is better:**
If you want to mute all sounds for a unit test, you just register a `DummyAudioService : IAudioService` in the Service Locator. The `Enemy` doesn't care, it just talks to the interface.

---

## 29.3 — Real Dependency Injection (VContainer / Zenject)

If your game gets large, manually registering things in a Service Locator gets messy. You want **Inversion of Control (IoC)**, just like Dagger/Hilt.

The two industry standards in Unity are:
1. **Zenject / Extenject:** Older, very feature-rich, but uses reflection (slower startup).
2. **VContainer:** Newer, lightweight, uses code generation, much faster. (Highly recommended).

### How VContainer Works (Conceptual Example)

You create a "Lifetime Scope" (like a Dagger Module/Component) and bind interfaces to classes.

```csharp
// 1. The Binder (Runs before anything else)
public class GameLifetimeScope : VContainer.Unity.LifetimeScope
{
    protected override void Configure(IContainerBuilder builder)
    {
        // Bind the interface to the MonoBehaviour implementation
        // Similar to @Binds in Dagger
        builder.RegisterComponentInHierarchy<AudioManager>().As<IAudioService>();
        
        // Pure C# class binding (no MonoBehaviour!)
        builder.Register<PlayerModel>(Lifetime.Singleton);
    }
}
```

```csharp
// 2. The Consumer (Your script)
public class Enemy : MonoBehaviour
{
    private IAudioService audio;
    private PlayerModel playerModel;

    // VContainer automatically finds this method and injects the dependencies!
    // Similar to @Inject in Android.
    [VContainer.Inject]
    public void Construct(IAudioService audioService, PlayerModel model)
    {
        this.audio = audioService;
        this.playerModel = model;
    }
    
    void Die()
    {
        audio.PlaySound("Death");
        playerModel.AddScore(100);
    }
}
```

### Why use a DI Framework?
- **Zero global state:** No singletons.
- **Clear dependencies:** You can look at the `Construct()` method and immediately see what a script needs to function.
- **Easy mocking:** Swapping out implementations for testing is trivial.
- **Pure C# injection:** You can inject dependencies into non-MonoBehaviour classes easily (great for MVP Presenters!).

---

## 📝 Summary 

1. **Avoid `FindObjectOfType`**: It's an O(N) search across the entire scene hierarchy.
2. **Avoid Singletons**: They create rigid, untestable global state.
3. **Use Service Locator**: A quick, lightweight pattern to decouple systems via Interfaces.
4. **Use VContainer**: If your project is large and you want true Android/Dagger-style Dependency Injection.

**Next:** [[30 — Clean Code & SOLID in Unity]]
