# 26 — Unity Folder Structure & Project Organization
### 🔶 Block F — Architecture & Best Practices

> [!NOTE]
> Coming from Android development, you're used to strict architectural boundaries (app, data, domain, presentation). Unity starts as the Wild West — everything is just an "Asset". If you don't organize your Unity project rigorously from Day 1, it will become an unmaintainable mess. This file shows the professional standard.

---

## 26.1 — The Standard Unity Project Structure

Never just dump files into the root `Assets/` folder. This is the industry-standard folder structure for a medium-to-large Unity project:

```text
Assets/
├── 📁 Animations/          # .anim clips, Animator Controllers
│   ├── Player/
│   └── Enemies/
├── 📁 Audio/               # .wav, .mp3, AudioMixers
│   ├── Music/
│   └── SFX/
├── 📁 Materials/           # .mat files
├── 📁 Models/              # 3D models (.fbx, .obj)
├── 📁 Plugins/             # Third-party code (MUST be named exactly "Plugins")
├── 📁 Prefabs/             # .prefab files (Grouped by feature)
│   ├── UI/
│   ├── Characters/
│   └── Environment/
├── 📁 Resources/           # SPECIAL UNITY FOLDER (Avoid using if possible)
├── 📁 Scenes/              # .unity files
│   ├── Levels/
│   └── Menus/
├── 📁 Scripts/             # All your C# code (Structured by Domain/Feature)
│   ├── Core/               # Game loop, Managers, DI
│   ├── Data/               # ScriptableObjects, Save formats
│   ├── Player/             # Player logic
│   ├── Enemies/            # Enemy AI
│   └── UI/                 # Menus, HUD
├── 📁 ScriptableObjects/   # .asset files (Instances of your Data scripts)
├── 📁 Settings/            # Project settings, Input settings
├── 📁 Sprites/             # 2D art assets
└── 📁 UI/                  # Fonts, raw UI textures, UI Toolkit assets
```

### Folder Rules:
1. **Sort by Asset Type at the Root:** Audio, Scripts, Prefabs, etc.
2. **Sort by Feature inside the Type:** `Scripts/Player/`, `Prefabs/Player/`.
3. **Avoid the `Resources` folder:** Unity loads everything in the `Resources` folder into memory at startup. It blows up memory and load times. Use **Addressables** or direct references instead.

---

## 26.2 — Namespaces (Your Code Folders)

By default, Unity puts all scripts in the global namespace. **Do not do this.** 

Just like Android packages (`com.yourcompany.appname`), wrap all your code in namespaces to prevent naming collisions (e.g., your `Camera` class colliding with Unity's `Camera` class).

```csharp
// Scripts/Player/PlayerController.cs
namespace MyGame.Player
{
    using UnityEngine;
    using MyGame.Core; // Access managers

    public class PlayerController : MonoBehaviour
    {
        // code
    }
}
```

**Namespace Convention:** `[ProjectName].[Feature/Domain]`

---

## 26.3 — Assembly Definitions (.asmdef) — Unity's Gradle Modules

In Android, you use Gradle modules (`:app`, `:data`, `:domain`) to enforce dependency rules and speed up compilation. 
In Unity, if you put all scripts in `Assets/Scripts/`, they compile into a single massive assembly (`Assembly-CSharp.dll`). If you change ONE file, Unity recompiles the entire project.

**The Solution: Assembly Definitions (`.asmdef`)**

An `.asmdef` file defines a boundary. Scripts in an `.asmdef` folder compile into their own DLL.

### Why use them?
1. **Compilation Speed:** If you change a UI script, only the UI assembly recompiles. The Player and Core assemblies don't.
2. **Strict Architecture:** You can prevent the `Core` module from accidentally referencing the `UI` module, enforcing Clean Architecture (Domain cannot know about Presentation).

### How to set them up:
Right-click a folder → **Create** → **Assembly Definition**.

```text
Scripts/
├── Core/
│   ├── Core.asmdef         (No dependencies)
│   └── GameManager.cs
├── Player/
│   ├── Player.asmdef       (Depends on Core.asmdef)
│   └── PlayerHealth.cs
└── UI/
    ├── UI.asmdef           (Depends on Core.asmdef and Player.asmdef)
    └── HealthBar.cs
```

If `GameManager.cs` tries to reference `HealthBar.cs`, it will throw a compile error. This enforces a strict, one-way dependency graph (UI depends on Core, Core depends on nothing), exactly like Clean Architecture.

---

## 26.4 — Where to Store Data (No SQL/Room here!)

In Android, you use Room/SQLite for local data. In Unity, data storage depends on what kind of data it is.

### 1. Static Game Data (Stats, Item definitions, Enemy types)
**Use ScriptableObjects.** (See File 28 for a deep dive).
These are data containers that live as files in your project. They are read-only at runtime in a built game. Do not use JSON or SQLite for static game config.

### 2. Player Progress / Save Data (High scores, inventory, unlocked levels)
**Use JSON serialization to `Application.persistentDataPath`.**

```csharp
using UnityEngine;
using System.IO;

[System.Serializable]
public class SaveData
{
    public int level;
    public int gold;
}

public class SaveSystem
{
    public static void Save(SaveData data)
    {
        string json = JsonUtility.ToJson(data); // Built-in Unity JSON serializer
        string path = Path.Combine(Application.persistentDataPath, "save.json");
        File.WriteAllText(path, json);
    }
    
    public static SaveData Load()
    {
        string path = Path.Combine(Application.persistentDataPath, "save.json");
        if (File.Exists(path))
        {
            string json = File.ReadAllText(path);
            return JsonUtility.FromJson<SaveData>(json);
        }
        return new SaveData(); // Default
    }
}
```

### 3. Simple User Preferences (Volume, Graphic settings)
**Use `PlayerPrefs`.** (Similar to Android `SharedPreferences`).
```csharp
// Save
PlayerPrefs.SetFloat("MasterVolume", 0.8f);
PlayerPrefs.Save();

// Load
float volume = PlayerPrefs.GetFloat("MasterVolume", 1.0f); // 1.0f is default
```
*Never use PlayerPrefs for save data or sensitive info (it's easily hackable and unencrypted).*

---

## 📝 Summary — App Dev to Game Dev Translation

| Android / App Dev Concept | Unity Equivalent |
| :--- | :--- |
| Gradle Modules (`:core`, `:ui`) | Assembly Definitions (`.asmdef`) |
| Java/Kotlin Packages | C# Namespaces |
| SharedPreferences | `PlayerPrefs` |
| Room / SQLite (Static Data) | ScriptableObjects (Assets) |
| Room / SQLite (User Save Data) | JSON to `Application.persistentDataPath` |
| `res/drawable` | `Assets/Sprites` or `Assets/Textures` |
| `res/values/strings.xml` | Localization package or ScriptableObjects |
| Dagger / Hilt | VContainer or Zenject (See File 29) |

**Next:** [[27 — Architecture — MVC, MVP & MVVM in Unity]]
