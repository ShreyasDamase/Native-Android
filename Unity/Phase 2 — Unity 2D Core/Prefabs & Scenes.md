# 🗂️ Prefabs & Scenes
### Prefab Templates, Multi-Scene Workflows & Data Persistence

Production-grade games use reusable templates called **Prefabs**, manage levels across multiple **Scenes**, and save player state dynamically to disk.

---

## 1. Prefab Templates & Variants

*   **Prefab**: A serialized template of a GameObject (including components, nested children, and configurations). Editing a prefab updates all instances of it across all scenes automatically.
*   **Nested Prefabs**: Prefabs containing references to other sub-prefabs (e.g. a `Car` prefab containing four instances of a `Wheel` prefab). Modifying the child `Wheel` prefab updates all instances inside the `Car`.
*   **Prefab Variants**: Reusable instances that inherit modifications from a parent prefab but override specific values.
    *   *Example:* Base parent `Enemy` prefab -> Variant `FastEnemy` (scales speed) and Variant `ArmoredEnemy` (scales health & shifts sprite color).

---

## 2. Asynchronous Scene Loading

Loading scenes instantly (`SceneManager.LoadScene`) freezes execution while the CPU loads assets, which can cause frames to freeze on mobile platforms. Always load scenes asynchronously in the background.

```csharp
using System.Collections;
using UnityEngine;
using UnityEngine.SceneManagement;
using UnityEngine.UI;
using TMPro;

public class LoadingScreenManager : MonoBehaviour
{
    [SerializeField] private GameObject loadingScreenUI;
    [SerializeField] private Slider progressBar;
    [SerializeField] private TextMeshProUGUI progressText;

    public void LoadNewLevel(string sceneName)
    {
        StartCoroutine(LoadSceneAsyncRoutine(sceneName));
    }

    private IEnumerator LoadSceneAsyncRoutine(string sceneName)
    {
        loadingScreenUI.SetActive(true);
        
        // Start background load
        AsyncOperation operation = SceneManager.LoadSceneAsync(sceneName);
        
        // Prevent instant scene activation to keep progress screen visible until fully ready
        operation.allowSceneActivation = false; 

        while (!operation.isDone)
        {
            // AsyncOperation progress caps at 0.9 when fully loaded
            float progress = Mathf.Clamp01(operation.progress / 0.9f);
            progressBar.value = progress;
            progressText.text = $"Loading: {Mathf.RoundToInt(progress * 100f)}%";

            // Check if load is complete
            if (operation.progress >= 0.9f)
            {
                progressText.text = "Press Any Key to Continue";
                if (Input.anyKey)
                {
                    operation.allowSceneActivation = true; // Activate loaded scene
                }
            }

            yield return null; // Wait for next frame
        }
    }
}
```

---

## 3. Multi-Scene (Additive) Workflows

Instead of putting all manager systems (UI, Audio, Game State, Networking) into every level scene, split the project into a persistent manager scene and additive geometry levels.

```
Persistent Scene (Managers, HUD UI)  ◄─── Loaded First, remains in memory
  └── Additive Level Scene (Geometry)  ◄─── Loaded on top, swapped per level
```

```csharp
public IEnumerator LoadLevelAdditive(string levelSceneName)
{
    // 1. Load level scene in background without unloading managers
    yield return SceneManager.LoadSceneAsync(levelSceneName, LoadSceneMode.Additive);

    // 2. Set the newly loaded scene as Active. 
    // This forces newly instantiated GameObjects to spawn into the level scene, not the managers scene.
    Scene activeLevelScene = SceneManager.GetSceneByName(levelSceneName);
    SceneManager.SetActiveScene(activeLevelScene);
}

public IEnumerator UnloadLevel(string levelSceneName)
{
    yield return SceneManager.UnloadSceneAsync(levelSceneName);
}
```

---

## 4. Safe Persistence: `DontDestroyOnLoad`

To pass managers across scene transitions, configure them to bypass scene unloads:
```csharp
public class GameManager : MonoBehaviour
{
    public static GameManager Instance { get; private set; }

    private void Awake()
    {
        if (Instance != null && Instance != this)
        {
            Destroy(gameObject); // Prevent duplicates when reloading scene
            return;
        }

        Instance = this;
        // Keeps this root GameObject alive across scene loads
        DontDestroyOnLoad(gameObject); 
    }
}
```

---

## 5. Security-Enhanced JSON Data Save System

For settings, use `PlayerPrefs` (easy to read/write, but easily editable by users). For game progression or inventory data, serialize C# state structures to JSON files and encrypt them.

```csharp
using System;
using System.IO;
using System.Text;
using UnityEngine;

[Serializable]
public class PlayerSaveData
{
    public int levelReached = 1;
    public int score = 0;
    public string playerName = "New Pilot";
    public string saveTimestamp;
}

public class SaveSystem : MonoBehaviour
{
    private string saveFilePath;
    private const string ObfuscationKey = "GravityFieldKey"; // XOR Encryption key

    private void Awake()
    {
        // Resolved path is cross-platform safe (Windows, Android, iOS sandboxes)
        saveFilePath = Path.Combine(Application.persistentDataPath, "savedata.dat");
    }

    public void SaveGame(PlayerSaveData data)
    {
        try
        {
            data.saveTimestamp = DateTime.UtcNow.ToString("o");
            
            // 1. Convert state to standard JSON string
            string jsonString = JsonUtility.ToJson(data, true);

            // 2. Encrypt string data using XOR
            string encryptedData = EncryptDecryptXOR(jsonString);

            // 3. Write data to persistent storage path
            File.WriteAllText(saveFilePath, encryptedData);
            Debug.Log($"Save completed. Path: {saveFilePath}");
        }
        catch (Exception ex)
        {
            Debug.LogError($"Save failed: {ex.Message}");
        }
    }

    public PlayerSaveData LoadGame()
    {
        if (!File.Exists(saveFilePath))
        {
            Debug.Log("No save file detected. Creating default values.");
            return new PlayerSaveData();
        }

        try
        {
            // 1. Read encrypted string data
            string encryptedData = File.ReadAllText(saveFilePath);

            // 2. Decrypt data
            string jsonString = EncryptDecryptXOR(encryptedData);

            // 3. Re-instantiate C# class from JSON values
            PlayerSaveData loadedData = JsonUtility.FromJson<PlayerSaveData>(jsonString);
            return loadedData;
        }
        catch (Exception ex)
        {
            Debug.LogError($"Load failed: {ex.Message}. Returning defaults.");
            return new PlayerSaveData();
        }
    }

    // A fast, lightweight XOR encryption utility preventing simple text-editor hacking
    private string EncryptDecryptXOR(string input)
    {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < input.Length; i++)
        {
            output.Append((char)(input[i] ^ ObfuscationKey[i % ObfuscationKey.Length]));
        }
        return output.ToString();
    }
}
```
