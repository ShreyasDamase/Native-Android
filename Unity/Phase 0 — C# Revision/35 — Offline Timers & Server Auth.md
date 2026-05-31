# 35 — Offline Timers & Server Auth
### 🔶 Block G — Game Algorithms & DSA

> [!CAUTION]
> If you are building Clash of Clans, your game will be hacked on Day 1 if you do not understand **Server Authority**. The golden rule of multiplayer game development is: **Never trust the client.**

---

## 35.1 — The Problem with Client-Side Logic

Imagine a player clicks "Upgrade Town Hall". The upgrade takes 3 days.

**❌ The Bad Way (Client Authority)**
```csharp
void StartUpgrade() {
    SaveData.TownHallLevel = 5;
    SaveData.UpgradeFinishTime = Time.time + (3 * 24 * 60 * 60);
    SaveToJSON(SaveData);
}
```
*Why this fails:*
1. The player can open `save.json` in a text editor and change `UpgradeFinishTime` to 0.
2. The player can go to their phone's settings and change the System Clock forward by 3 days.
3. The player can use a memory editor (like Cheat Engine on PC or GameGuardian on Android) to change the memory values while the game is running.

---

## 35.2 — Server Authority (The Only Way)

To stop cheating, the phone (Client) is no longer allowed to make decisions. The phone is just a "dumb screen" that sends requests to a Server (Firebase, PlayFab, AWS, custom Node.js).

**✅ The Secure Way (Server Authority)**
1. **Client:** User taps "Upgrade". Phone sends an HTTP Request to Server: `POST /api/upgrade { buildingId: 12 }`
2. **Server:** Looks up the player in the secure database. Checks if they have enough gold. Checks if the builder is free. 
3. **Server:** Calculates the finish time based on a secure NTP (Network Time Protocol) server clock. Saves this to the database.
4. **Server:** Replies to Client: `200 OK. Upgrade finishes at Unix Timestamp 1717200000.`
5. **Client:** Starts a visual countdown timer on the screen.

Even if the player hacks their phone's clock or memory, the Server doesn't care. When the player tries to do a Level 5 action, the Server checks the database, sees the upgrade isn't finished yet, and rejects the action.

---

## 35.3 — Offline Timers (Unix Timestamps)

Unity's `Time.time` only counts how long the game has been open. It resets when the app closes.
Unity's `System.DateTime.Now` can be easily hacked by changing the phone's clock.

To calculate how much time passed while the app was closed, you must use **Unix Timestamps** fetched from an independent time server.

A Unix Timestamp is a single integer: The number of seconds that have passed since January 1, 1970.

```csharp
// Example Timestamp: 1717156800 (May 31, 2026)

public class TimeManager
{
    // Fetched from your server at login, NOT from the phone's clock!
    public static long CurrentServerUnixTime; 
}

public class Building : MonoBehaviour
{
    public long finishTimestamp; // Fetched from server DB
    
    void Update()
    {
        long secondsRemaining = finishTimestamp - TimeManager.CurrentServerUnixTime;
        
        if (secondsRemaining <= 0)
        {
            // The upgrade is finished!
            // Wait for the server to confirm it, then update visuals.
        }
        else
        {
            // Format the remaining time for the UI UI
            TimeSpan time = TimeSpan.FromSeconds(secondsRemaining);
            uiText.text = $"{time.Days}d {time.Hours}h {time.Minutes}m";
        }
    }
}
```

---

## 35.4 — Deterministic Lockstep (How CoC Battles Work)

In an FPS (Call of Duty), the server simulates the game 60 times a second and sends the exact position of every bullet to all players.

In Clash of Clans, there are 300 Barbarians on screen. If the server sent the position of 300 Barbarians 60 times a second, the phone's internet data would explode. 

Instead, CoC uses **Deterministic Lockstep**.

### What is Determinism?
Determinism means: If you start with the exact same inputs, the simulation will produce the exact same result every single time. `1 + 1 = 2` is deterministic. 

### How CoC battles are sent over the network:
1. You deploy a Barbarian at grid `(15, 10)` at tick `450`.
2. The phone sends a tiny message to the server: `[Deploy, Barbarian, x:15, y:10, tick:450]`.
3. The Server saves this input to the database. It does NOT simulate the battle.
4. On your screen, the battle plays out using local C# logic. 
5. When the battle is over, the Server sends the list of your taps to the Defender's phone when they log in to watch the replay.
6. The Defender's phone runs the *exact same simulation* using the *exact same taps*. Because the C# code is perfectly deterministic, the Barbarian will walk the exact same path, hit the exact same walls, and the base will take the exact same damage.

### The Problem with Unity Physics
Unity's `Rigidbody2D` and `PhysX` are **NOT DETERMINISTIC** across different devices. Floating point math (`0.1 + 0.2`) calculates slightly differently on an iPhone ARM processor vs an Android Snapdragon processor. Over a 3-minute battle, a 0.0001f difference in a Barbarian's position will cause it to target a different wall, and the whole replay will desync!

If you want to build Clash of Clans, you **cannot use Unity's built-in physics or Pathfinding**. You must write your own custom fixed-point integer math grid and A* pathfinding (like we did in Files 31 and 32) so that 100% of the game logic uses integers, guaranteeing identical results on every device.

---

## 📝 Final Summary: To Build Clash of Clans...

1. You need a **Generic 2D Grid System** (File 31).
2. You need an integer-based **A* Pathfinding Algorithm** (File 32) that factors in wall-breaking penalties.
3. You need **Spatial Partitioning (Quadtrees)** (File 33) to allow hundreds of troops to find targets without lagging the phone.
4. You need **Finite State Machines** (File 34) for troop AI.
5. You need a **Server Backend (Firebase/PlayFab/Custom)** to store save data and timestamps securely.
6. You cannot use `Rigidbody` or `NavMesh` for combat if you want accurate replays. You must use pure custom math.

*Return to Index → [[00 — Index & Learning Path]]*
