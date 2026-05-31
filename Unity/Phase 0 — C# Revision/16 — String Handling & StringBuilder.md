# 16 — String Handling & StringBuilder
### 🟡 Block C — Memory Management & Performance

> [!NOTE]
> Strings are **immutable reference types** in C#. Every time you modify or concatenate a string, a brand-new string object is created on the heap. In a game that updates text every frame (HUD, score, timer), this causes continuous GC pressure. This chapter covers the right tools for every string scenario.

---

## 16.1 — Why Strings Are Special

```csharp
// String is a reference type BUT behaves like a value type
string a = "Hello";
string b = a;      // b points to the same object
b = "World";       // b now points to a NEW string — 'a' is unchanged

Debug.Log(a); // "Hello" — unchanged
Debug.Log(b); // "World" — different object

// String interning — identical literals share the same object
string s1 = "game";
string s2 = "game";
Debug.Log(object.ReferenceEquals(s1, s2)); // true — same object in memory!

// String CONCATENATION always creates NEW objects
string result = "HP: " + 100 + "/" + 200; 
// Created: "HP: " + "100" → "HP: 100"
// Created: "HP: 100" + "/" → "HP: 100/"
// Created: "HP: 100/" + "200" → "HP: 100/200"
// 3 temporary strings allocated and immediately garbage!
```

---

## 16.2 — The Five Ways to Build Strings

### 1. String Concatenation (`+`) — Only for Compile-Time Strings
```csharp
// ✅ OK — both parts are known at compile time — compiler optimizes this to ONE string
string path = Application.persistentDataPath + "/saves/";

// ❌ BAD in Update() — runtime values cause allocation every call
void Update()
{
    hpText.text = "HP: " + currentHP + "/" + maxHP; // allocates every frame!
}
```

### 2. `$""` String Interpolation — Cleaner, Same Cost as `+`
```csharp
// Cleaner syntax — same performance as concatenation
// Use in Start/Awake for one-time setup, not in Update
string playerInfo = $"Name: {playerName} | Level: {level} | HP: {hp}/{maxHP}";

// With formatting
string timerText = $"{minutes:00}:{seconds:00}"; // "03:47" format
string damageText = $"{damage:F1} damage!";        // "25.5 damage!"
string percentText = $"{healthPercent:P0}";         // "75%" format
```

### 3. `string.Format()` — Most Flexible, Supports All Format Specifiers
```csharp
// string.Format — good for localization systems
string result = string.Format("Player {0} dealt {1:F1} damage!", playerName, damage);
string score = string.Format("Score: {0:N0}", 1500000); // "Score: 1,500,000"
string time = string.Format("{0:00}:{1:00}.{2:00}", hours, minutes, seconds);

// Useful format specifiers
// {0:F2}  — 2 decimal places: 3.14
// {0:N0}  — thousands separator: 1,500,000
// {0:P1}  — percentage 1 decimal: 75.5%
// {0:X}   — hex: FF
// {0:00}  — zero-padded 2 digits: 07
// {0:0.##}— up to 2 decimals, no trailing zeros: 3.1 or 3
```

### 4. `StringBuilder` — For Building Dynamic Strings Without Allocation
```csharp
// StringBuilder reuses its internal buffer — far fewer allocations
private System.Text.StringBuilder sb = new System.Text.StringBuilder(128);

public string BuildInventoryText(List<Item> items)
{
    sb.Clear(); // reset without allocation — the buffer stays allocated
    sb.AppendLine("=== Inventory ===");
    
    for (int i = 0; i < items.Count; i++)
    {
        sb.Append(i + 1);
        sb.Append(". ");
        sb.Append(items[i].Name);
        sb.Append(" x");
        sb.AppendLine(items[i].Count.ToString());
    }
    
    sb.Append("Total items: ");
    sb.Append(items.Count);
    
    return sb.ToString(); // ONE allocation at the end
}
```

### 5. `TextMeshPro` with Caching — Zero Allocation UI Text
```csharp
// Best practice for HUD numbers: event-driven updates only
public class HUDManager : MonoBehaviour
{
    [SerializeField] private TMPro.TextMeshProUGUI hpText;
    [SerializeField] private TMPro.TextMeshProUGUI scoreText;
    [SerializeField] private TMPro.TextMeshProUGUI timerText;
    
    private int cachedHP = -1;
    private int cachedScore = -1;
    private int cachedSeconds = -1;
    
    void OnEnable()
    {
        GameEvents.OnHealthChanged += UpdateHP;
        GameEvents.OnScoreChanged += UpdateScore;
    }
    
    void OnDisable()
    {
        GameEvents.OnHealthChanged -= UpdateHP;
        GameEvents.OnScoreChanged -= UpdateScore;
    }
    
    // Only called when health actually changes — not every frame
    void UpdateHP(float hp)
    {
        int hpInt = Mathf.CeilToInt(hp);
        if (hpInt == cachedHP) return; // no change — skip
        cachedHP = hpInt;
        hpText.text = $"{hpInt}"; // one allocation — acceptable since rare
    }
    
    void UpdateScore(int score)
    {
        if (score == cachedScore) return;
        cachedScore = score;
        scoreText.text = score.ToString(); // ToString() of int is efficient
    }
    
    // Timer — only update when the second changes
    void Update()
    {
        int seconds = Mathf.FloorToInt(gameTime);
        if (seconds == cachedSeconds) return; // no change — no allocation!
        cachedSeconds = seconds;
        
        int mins = seconds / 60;
        int secs = seconds % 60;
        timerText.text = $"{mins:00}:{secs:00}"; // only once per second
    }
}
```

---

## 16.3 — String Comparison

```csharp
// ✅ Correct string comparisons
string name = "Player";

// Case-sensitive comparison (most common)
bool exact = name == "Player";                    // true
bool notEqual = name != "Enemy";                  // true

// Case-insensitive comparison
bool ci = string.Equals(name, "player", System.StringComparison.OrdinalIgnoreCase);

// Check start/end
bool starts = name.StartsWith("Play");            // true
bool ends = name.EndsWith("er");                  // true

// Contains
bool has = name.Contains("lay");                  // true

// String.IsNullOrEmpty — check for empty/null safely
if (string.IsNullOrEmpty(playerName))
    playerName = "Unknown";

// String.IsNullOrWhiteSpace — also catches " " strings
if (string.IsNullOrWhiteSpace(inputField.text))
    ShowError("Please enter a name");
```

---

## 16.4 — String Manipulation

```csharp
string raw = "  Hello, World!  ";

// Trimming whitespace
string trimmed = raw.Trim();              // "Hello, World!"
string leftTrim = raw.TrimStart();        // "Hello, World!  "
string rightTrim = raw.TrimEnd();         // "  Hello, World!"

// Case conversion
string upper = raw.ToUpper();             // "  HELLO, WORLD!  "
string lower = raw.ToLower();             // "  hello, world!  "

// Splitting
string csv = "sword,shield,potion,gold";
string[] items = csv.Split(',');          // ["sword", "shield", "potion", "gold"]

// Joining
string rejoined = string.Join(" | ", items); // "sword | shield | potion | gold"

// Replace
string censored = "bad word here".Replace("bad word", "***"); // "*** here"

// Substring
string hello = "Hello, World!";
string sub = hello.Substring(7, 5);      // "World" (start at 7, take 5 chars)
string afterComma = hello.Substring(7);  // "World!" (start at 7, to end)

// IndexOf
int idx = hello.IndexOf("World");        // 7
int notFound = hello.IndexOf("Unity");   // -1 if not found

// Length
int len = hello.Length;                  // 13
```

---

## 16.5 — Number Formatting for Game UIs

```csharp
// Score formatting (thousands separators)
int score = 1250000;
string scoreStr = score.ToString("N0");   // "1,250,000"

// Health as fraction
int hp = 73, maxHp = 100;
string hpStr = $"{hp}/{maxHp}";           // "73/100"

// Percentage
float percent = 0.735f;
string pct = percent.ToString("P0");      // "74%" (rounds)
string pct2 = percent.ToString("P1");     // "73.5%"
string manual = $"{percent * 100:F0}%";   // "74%"

// Timer formatting (mm:ss)
float timeRemaining = 247.5f; // seconds
int m = (int)(timeRemaining / 60);
int s = (int)(timeRemaining % 60);
string timer = $"{m:00}:{s:00}";          // "04:07"

// Distance formatting
float dist = 1523.7f;
string distStr = dist >= 1000f 
    ? $"{dist / 1000f:F1}km" 
    : $"{dist:F0}m";                      // "1.5km" or "523m"

// Damage numbers
float dmg = 156.25f;
string dmgStr = dmg >= 1000f 
    ? $"{dmg / 1000f:F1}k" 
    : $"{dmg:F0}";                        // "156" or "1.2k"
```

---

## 16.6 — Parsing Strings to Values

```csharp
// Parse string → number
string input = "42";
int parsed = int.Parse(input);            // throws if fails
float parsedF = float.Parse("3.14");

// Safe parsing — returns false instead of throwing
bool ok = int.TryParse(input, out int result);
if (ok) Debug.Log($"Parsed: {result}");

// Parse player input safely
void OnLevelInputSubmit(string input)
{
    if (int.TryParse(input, out int levelNum) && levelNum >= 1 && levelNum <= maxLevels)
    {
        LoadLevel(levelNum);
    }
    else
    {
        ShowError("Please enter a level number between 1 and " + maxLevels);
    }
}
```

---

## 16.7 — 🎮 2D vs 3D: String Patterns

### 🎮 2D — HUD for Platformer
```csharp
public class PlatformerHUD : MonoBehaviour
{
    [SerializeField] private TMPro.TextMeshProUGUI livesText;
    [SerializeField] private TMPro.TextMeshProUGUI coinsText;
    [SerializeField] private TMPro.TextMeshProUGUI levelText;
    
    private int cachedLives, cachedCoins;
    private System.Text.StringBuilder sb = new System.Text.StringBuilder(32);
    
    public void UpdateLives(int lives)
    {
        if (lives == cachedLives) return;
        cachedLives = lives;
        
        // Build "♥♥♥" style display
        sb.Clear();
        for (int i = 0; i < lives; i++) sb.Append("♥");
        for (int i = lives; i < maxLives; i++) sb.Append("♡");
        livesText.text = sb.ToString();
    }
    
    public void UpdateCoins(int coins)
    {
        if (coins == cachedCoins) return;
        cachedCoins = coins;
        coinsText.text = $"✦ {coins:N0}";  // "✦ 1,200"
    }
    
    public void SetLevel(int world, int level)
    {
        levelText.text = $"World {world} - Level {level}"; // set once, no caching needed
    }
}
```

### 🎮 3D — HUD for FPS/TPS
```csharp
public class FPSHud : MonoBehaviour
{
    [SerializeField] private TMPro.TextMeshProUGUI ammoText;
    [SerializeField] private TMPro.TextMeshProUGUI healthText;
    [SerializeField] private TMPro.TextMeshProUGUI interactPromptText;
    
    private int cachedAmmo = -1, cachedReserve = -1;
    private int cachedHP = -1;
    
    public void UpdateAmmo(int current, int reserve)
    {
        if (current == cachedAmmo && reserve == cachedReserve) return;
        cachedAmmo = current;
        cachedReserve = reserve;
        ammoText.text = $"{current} / {reserve}"; // "15 / 45"
    }
    
    public void UpdateHealth(int hp)
    {
        if (hp == cachedHP) return;
        cachedHP = hp;
        healthText.text = hp.ToString(); // "73"
        
        // Color feedback
        healthText.color = hp > 50 ? Color.white : hp > 25 ? Color.yellow : Color.red;
    }
    
    public void ShowInteractPrompt(string objectName, string action)
    {
        interactPromptText.text = $"[E] {action} {objectName}"; // "[E] Open Chest"
        interactPromptText.gameObject.SetActive(true);
    }
    
    public void HideInteractPrompt() => interactPromptText.gameObject.SetActive(false);
}
```

---

## 📝 Summary

| Scenario | Use | Notes |
| :--- | :--- | :--- |
| Compile-time strings | `+` or `$""` | Compiler may optimize |
| One-time setup strings | `$""` interpolation | One allocation, acceptable |
| Per-event UI updates | `$""` or `.ToString()` | Fine — happens rarely |
| Per-frame string building | Cache + compare | Only rebuild when value changes |
| Building from many parts | `StringBuilder` | Reuse the same builder |
| Parsing user input | `int.TryParse()` | Never use `.Parse()` directly |
| Tag comparison | `CompareTag()` | Never use `gameObject.tag == "..."` |
| Score formatting | `N0` format | `"1,250,000"` with thousands separator |
| Timer formatting | `{m:00}:{s:00}` | Zero-padded "04:07" |

**Previous:** [[15 — Memory Management & GC]] | **Next:** [[17 — Object Pooling in C#]]
