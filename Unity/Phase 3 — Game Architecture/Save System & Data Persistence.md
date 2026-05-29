# 💾 Save System & Data Persistence
### JSON Serialization, Encryption, ScriptableObject Data, PlayerPrefs & Binary Saves

A production save system must be fast, reliable, version-tolerant, and secure. This guide covers every layer from simple key-value stores to encrypted binary files with checksums.

---

## 1. Save System Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────┐
│                      SAVE SYSTEM LAYERS                              │
├────────────────────────┬─────────────────────────────────────────────┤
│ Layer                  │ Best For                                     │
├────────────────────────┼─────────────────────────────────────────────┤
│ PlayerPrefs            │ Lightweight settings (volume, resolution)    │
│ JSON File (plain)      │ Small games, mod support, debug readability  │
│ JSON File (encrypted)  │ Preventing basic save-file cheating          │
│ Binary File            │ Fastest read/write, smallest file size       │
│ Cloud Save (Firebase)  │ Cross-device sync, online leaderboards       │
└────────────────────────┴─────────────────────────────────────────────┘
```

---

## 2. PlayerPrefs (Simple Key-Value Settings)

`PlayerPrefs` stores primitive types in the OS registry / plist. Use it only for settings — never for critical gameplay state (it can be wiped by OS, crashes, or uninstall).

```csharp
public static class SettingsManager
{
    // ─── Store ────────────────────────────────────────────────────────
    public static void SaveVolume(float volume)   => PlayerPrefs.SetFloat("MasterVolume", volume);
    public static void SaveFullscreen(bool value) => PlayerPrefs.SetInt("Fullscreen", value ? 1 : 0);
    public static void SavePlayerName(string name) => PlayerPrefs.SetString("PlayerName", name);

    // ─── Load ────────────────────────────────────────────────────────
    public static float  GetVolume()      => PlayerPrefs.GetFloat("MasterVolume", 1f); // 1f = default
    public static bool   GetFullscreen()  => PlayerPrefs.GetInt("Fullscreen", 1) == 1;
    public static string GetPlayerName()  => PlayerPrefs.GetString("PlayerName", "Player");

    // ─── Always call this to flush to disk ───────────────────────────
    public static void CommitSettings() => PlayerPrefs.Save();

    // ─── Reset all settings ──────────────────────────────────────────
    public static void ResetAll() { PlayerPrefs.DeleteAll(); PlayerPrefs.Save(); }
}
```

> [!WARNING]
> `PlayerPrefs.Save()` is synchronous and writes to disk on the calling thread. Avoid calling it every frame. Call it only when the player changes a setting or quits the game.

---

## 3. JSON Save System (Recommended for Most Games)

### Step 1: Define the Save Data Model

```csharp
[System.Serializable]
public class SaveData
{
    // Versioning: increment when you add/remove fields
    public int saveVersion = 1;
    public string saveTimestamp;

    // Player state
    public float playerPositionX;
    public float playerPositionY;
    public int   currentHealth;
    public int   maxHealth;
    public int   currentLevel;
    public long  totalScore;

    // Inventory
    public List<string> collectedItemIds = new List<string>();

    // World state
    public List<string> completedObjectiveIds = new List<string>();
    public Dictionary<string, bool> defeatedEnemies = new Dictionary<string, bool>();
}
```

### Step 2: The Save Manager

```csharp
using System;
using System.IO;
using UnityEngine;

public class SaveManager : MonoBehaviour
{
    // Persistent data path: survives app updates. Platform-specific.
    // Windows: C:\Users\<User>\AppData\LocalLow\<Company>\<Product>\
    // Android: /data/data/<packagename>/files/
    // iOS:     <Application.persistentDataPath>
    private static string SavePath => Path.Combine(Application.persistentDataPath, "save_slot_1.json");

    public static void Save(SaveData data)
    {
        try
        {
            data.saveTimestamp = DateTime.UtcNow.ToString("yyyy-MM-ddTHH:mm:ssZ");
            string json = JsonUtility.ToJson(data, prettyPrint: true);
            File.WriteAllText(SavePath, json);
            Debug.Log($"[SaveManager] Game saved to: {SavePath}");
        }
        catch (Exception ex)
        {
            Debug.LogError($"[SaveManager] Save failed: {ex.Message}");
        }
    }

    public static SaveData Load()
    {
        if (!File.Exists(SavePath))
        {
            Debug.Log("[SaveManager] No save file found. Creating new game.");
            return new SaveData(); // Fresh game
        }

        try
        {
            string json = File.ReadAllText(SavePath);
            SaveData data = JsonUtility.FromJson<SaveData>(json);

            // Version migration: handle old saves missing new fields
            if (data.saveVersion < 1) MigrateFromV0(data);

            return data;
        }
        catch (Exception ex)
        {
            Debug.LogError($"[SaveManager] Load failed: {ex.Message}. Returning fresh save.");
            return new SaveData();
        }
    }

    public static bool HasSave() => File.Exists(SavePath);

    public static void DeleteSave()
    {
        if (File.Exists(SavePath)) File.Delete(SavePath);
    }

    // Version migration: called when loading an older save format
    private static void MigrateFromV0(SaveData data)
    {
        data.saveVersion = 1;
        if (data.maxHealth == 0) data.maxHealth = 100; // Backfill missing field
        Debug.Log("[SaveManager] Migrated save from v0 → v1");
    }
}
```

### Step 3: Auto-save on Quit

```csharp
public class GameManager : MonoBehaviour
{
    private SaveData currentSave;

    private void Start()
    {
        currentSave = SaveManager.Load();
        ApplySaveToWorld(currentSave);
    }

    // Unity calls this when the app is about to quit
    private void OnApplicationQuit()
    {
        CollectWorldState(currentSave);
        SaveManager.Save(currentSave);
    }

    // Unity calls this when the app loses focus (mobile background)
    private void OnApplicationPause(bool isPaused)
    {
        if (isPaused)
        {
            CollectWorldState(currentSave);
            SaveManager.Save(currentSave);
        }
    }

    private void ApplySaveToWorld(SaveData data)
    {
        // Apply saved data to your game entities
        Player.Instance.SetPosition(new Vector2(data.playerPositionX, data.playerPositionY));
        Player.Instance.SetHealth(data.currentHealth, data.maxHealth);
    }

    private void CollectWorldState(SaveData data)
    {
        data.playerPositionX = Player.Instance.Position.x;
        data.playerPositionY = Player.Instance.Position.y;
        data.currentHealth   = Player.Instance.CurrentHealth;
    }
}
```

---

## 4. Encrypted JSON Save (Anti-Cheat)

Use AES-256 encryption to prevent players from editing save files in a text editor.

> [!NOTE]
> This is NOT cryptographically secure against determined hackers (the key is inside the app), but it stops casual cheating with Notepad.

```csharp
using System;
using System.IO;
using System.Security.Cryptography;
using System.Text;
using UnityEngine;

public static class EncryptedSaveManager
{
    // ⚠️ In a real game, derive this from a device fingerprint or store in a secure config
    private const string AesKey = "MyGame_K3y_32Bytes_PadToLength!1"; // Must be 32 chars for AES-256
    private const string AesIV  = "InitVector16Byte";                   // Must be 16 chars

    private static string SavePath => Path.Combine(Application.persistentDataPath, "save.enc");

    public static void Save(SaveData data)
    {
        string json = JsonUtility.ToJson(data, prettyPrint: false);
        byte[] encrypted = AesEncrypt(json);
        File.WriteAllBytes(SavePath, encrypted);
    }

    public static SaveData Load()
    {
        if (!File.Exists(SavePath)) return new SaveData();

        try
        {
            byte[] encrypted = File.ReadAllBytes(SavePath);
            string json = AesDecrypt(encrypted);
            return JsonUtility.FromJson<SaveData>(json);
        }
        catch
        {
            Debug.LogError("[EncryptedSave] Failed to decrypt save file. File may be corrupted.");
            return new SaveData();
        }
    }

    private static byte[] AesEncrypt(string plainText)
    {
        using var aes = Aes.Create();
        aes.Key = Encoding.UTF8.GetBytes(AesKey);
        aes.IV  = Encoding.UTF8.GetBytes(AesIV);

        using var ms = new MemoryStream();
        using (var cs = new CryptoStream(ms, aes.CreateEncryptor(), CryptoStreamMode.Write))
        using (var sw = new StreamWriter(cs))
            sw.Write(plainText);

        return ms.ToArray();
    }

    private static string AesDecrypt(byte[] cipherBytes)
    {
        using var aes = Aes.Create();
        aes.Key = Encoding.UTF8.GetBytes(AesKey);
        aes.IV  = Encoding.UTF8.GetBytes(AesIV);

        using var ms = new MemoryStream(cipherBytes);
        using var cs = new CryptoStream(ms, aes.CreateDecryptor(), CryptoStreamMode.Read);
        using var sr = new StreamReader(cs);
        return sr.ReadToEnd();
    }
}
```

---

## 5. Binary Save System (Fastest, Smallest)

For large amounts of data (thousands of entities, level tile maps), binary serialization using `BinaryWriter`/`BinaryReader` is much faster than JSON.

```csharp
using System.IO;
using UnityEngine;

public static class BinarySaveManager
{
    private static string SavePath => Path.Combine(Application.persistentDataPath, "save.bin");

    public static void Save(SaveData data)
    {
        using var stream = new FileStream(SavePath, FileMode.Create);
        using var writer = new BinaryWriter(stream);

        writer.Write(data.saveVersion);
        writer.Write(data.playerPositionX);
        writer.Write(data.playerPositionY);
        writer.Write(data.currentHealth);
        writer.Write(data.maxHealth);
        writer.Write(data.currentLevel);
        writer.Write(data.totalScore);

        // Write list of item IDs
        writer.Write(data.collectedItemIds.Count);
        foreach (var id in data.collectedItemIds)
            writer.Write(id);
    }

    public static SaveData Load()
    {
        if (!File.Exists(SavePath)) return new SaveData();

        using var stream = new FileStream(SavePath, FileMode.Open);
        using var reader = new BinaryReader(stream);

        var data = new SaveData
        {
            saveVersion      = reader.ReadInt32(),
            playerPositionX  = reader.ReadSingle(),
            playerPositionY  = reader.ReadSingle(),
            currentHealth    = reader.ReadInt32(),
            maxHealth        = reader.ReadInt32(),
            currentLevel     = reader.ReadInt32(),
            totalScore       = reader.ReadInt64()
        };

        int itemCount = reader.ReadInt32();
        for (int i = 0; i < itemCount; i++)
            data.collectedItemIds.Add(reader.ReadString());

        return data;
    }
}
```

---

## 6. Multiple Save Slots

```csharp
public class SaveSlotManager : MonoBehaviour
{
    private const int MaxSlots = 3;

    private static string SlotPath(int slot)
        => Path.Combine(Application.persistentDataPath, $"save_slot_{slot}.json");

    public static void SaveToSlot(int slot, SaveData data)
    {
        if (slot < 0 || slot >= MaxSlots) return;
        File.WriteAllText(SlotPath(slot), JsonUtility.ToJson(data, true));
    }

    public static SaveData LoadFromSlot(int slot)
    {
        string path = SlotPath(slot);
        return File.Exists(path) ? JsonUtility.FromJson<SaveData>(File.ReadAllText(path)) : null;
    }

    public static bool SlotExists(int slot) => File.Exists(SlotPath(slot));

    public static void DeleteSlot(int slot)
    {
        string path = SlotPath(slot);
        if (File.Exists(path)) File.Delete(path);
    }

    // Get all available slot metadata for the Save/Load UI
    public static SaveData[] GetAllSlotMetadata()
    {
        var slots = new SaveData[MaxSlots];
        for (int i = 0; i < MaxSlots; i++)
            slots[i] = LoadFromSlot(i);
        return slots;
    }
}
```

---

## 7. ScriptableObject Runtime Data Pattern

ScriptableObjects can act as live data containers that survive scene loads — useful for sharing runtime game state without singletons.

```csharp
// Define a shared data container as a ScriptableObject
[CreateAssetMenu(fileName = "PlayerData", menuName = "Game/PlayerData")]
public class PlayerDataSO : ScriptableObject
{
    public int CurrentHealth;
    public int MaxHealth;
    public float Score;
    public List<string> CollectedItems;

    // Reset to default values when a new game starts
    public void Reset()
    {
        CurrentHealth = MaxHealth = 100;
        Score = 0;
        CollectedItems = new List<string>();
    }

    // Populate from a save file
    public void FromSave(SaveData data)
    {
        CurrentHealth   = data.currentHealth;
        MaxHealth       = data.maxHealth;
        Score           = data.totalScore;
        CollectedItems  = new List<string>(data.collectedItemIds);
    }

    // Convert runtime state back into a saveable struct
    public SaveData ToSave(SaveData existing)
    {
        existing.currentHealth     = CurrentHealth;
        existing.maxHealth         = MaxHealth;
        existing.totalScore        = (long)Score;
        existing.collectedItemIds  = new List<string>(CollectedItems);
        return existing;
    }
}
```

---

## 8. File Safety — Atomic Writes

A power-loss or crash mid-write can corrupt the save file. Use atomic writes: write to a `.tmp` file, then replace the original only on success.

```csharp
public static void AtomicSave(SaveData data, string path)
{
    string tempPath = path + ".tmp";
    string backupPath = path + ".bak";

    try
    {
        // Step 1: Write to a temp file
        string json = JsonUtility.ToJson(data, true);
        File.WriteAllText(tempPath, json);

        // Step 2: Backup the existing save
        if (File.Exists(path)) File.Copy(path, backupPath, overwrite: true);

        // Step 3: Atomic replace (on most OS, File.Move is atomic within same drive)
        File.Move(tempPath, path);
        Debug.Log("[Save] Atomic save successful.");
    }
    catch (Exception ex)
    {
        Debug.LogError($"[Save] Atomic save failed: {ex.Message}");

        // Step 4: Restore backup if the main file was corrupted
        if (File.Exists(backupPath) && !File.Exists(path))
            File.Move(backupPath, path);
    }
}
```
