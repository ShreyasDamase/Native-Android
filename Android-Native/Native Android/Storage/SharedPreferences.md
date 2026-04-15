# 🗂️ SharedPreferences — Complete Mastery Textbook

### _From First Principles to System Internals to Production Patterns_

> Written for Android developers using Kotlin. Covers everything from Android 1.0 history  
> through modern usage, internal mechanics, all APIs, real-world patterns, and known dangers.

---

## Table of Contents

**Part I — History & Foundation**

1. [What Is SharedPreferences?](#chapter-1-what-is-sharedpreferences)
2. [The History — Why It Was Created](#chapter-2-the-history--why-it-was-created)
3. [The XML File on Disk](#chapter-3-the-xml-file-on-disk)
4. [How SharedPreferences Loads Into Memory](#chapter-4-how-sharedpreferences-loads-into-memory)

**Part II — System Level Internals** 5. [The In-Memory HashMap — How Reads Are Instant](#chapter-5-the-in-memory-hashmap--how-reads-are-instant) 6. [The Editor — How Writes Work Internally](#chapter-6-the-editor--how-writes-work-internally) 7. [commit() vs apply() — The Deep Difference](#chapter-7-commit-vs-apply--the-deep-difference) 8. [The QueuedWork Trap — The Hidden ANR Mechanism](#chapter-8-the-queuedwork-trap--the-hidden-anr-mechanism) 9. [The Listener System — How Change Callbacks Work](#chapter-9-the-listener-system--how-change-callbacks-work)

**Part III — Complete API Reference** 10. [Getting a SharedPreferences Instance](#chapter-10-getting-a-sharedpreferences-instance) 11. [All Access Modes Explained](#chapter-11-all-access-modes-explained) 12. [All Data Types You Can Store](#chapter-12-all-data-types-you-can-store) 13. [Reading Data — Every Method](#chapter-13-reading-data--every-method) 14. [Writing Data — Every Method](#chapter-14-writing-data--every-method) 15. [Deleting Data](#chapter-15-deleting-data) 16. [Checking If a Key Exists](#chapter-16-checking-if-a-key-exists) 17. [Change Listeners](#chapter-17-change-listeners)

**Part IV — Architecture & Patterns** 18. [SharedPreferences in Clean Architecture](#chapter-18-sharedpreferences-in-clean-architecture) 19. [SharedPreferences in Kotlin — Modern Wrappers](#chapter-19-sharedpreferences-in-kotlin--modern-wrappers) 20. [SharedPreferences with Koin DI](#chapter-20-sharedpreferences-with-koin-di) 21. [Multi-Process SharedPreferences](#chapter-21-multi-process-sharedpreferences)

**Part V — Dangers & Best Practices** 22. [The 7 Dangers of SharedPreferences](#chapter-22-the-7-dangers-of-sharedpreferences) 23. [Common Mistakes & How to Avoid Them](#chapter-23-common-mistakes--how-to-avoid-them) 24. [Security Considerations](#chapter-24-security-considerations)

**Part VI — Real-World Examples** 25. [Real-World Patterns — 10 Complete Examples](#chapter-25-real-world-patterns--10-complete-examples) 26. [SharedPreferences vs DataStore — When to Use What](#chapter-26-sharedpreferences-vs-datastore--when-to-use-what) 27. [Migrating From SharedPreferences to DataStore](#chapter-27-migrating-from-sharedpreferences-to-datastore) 28. [Quick Reference Cheat Sheet](#chapter-28-quick-reference-cheat-sheet)

---

# PART I — HISTORY & FOUNDATION

---

## Chapter 1: What Is SharedPreferences?

SharedPreferences is Android's built-in, simple, key-value data storage system. It stores small pieces of data as named pairs — a key and a value — and persists them to disk as an XML file so they survive app restarts, device reboots, and even Android version updates.

Think of it as a persistent dictionary built into every Android app. You put something in — it stays there. You come back tomorrow, next week, after a reboot — it's still there.

```
App Memory (HashMap)          Disk (XML file)
┌──────────────────┐          ┌────────────────────────────────┐
│ dark_mode: true  │ ←──────→ │ <boolean name="dark_mode"      │
│ username: "John" │          │   value="true" />              │
│ launch_count: 5  │          │ <string name="username"        │
└──────────────────┘          │   value="John" />              │
                              │ <int name="launch_count"       │
                              │   value="5" />                 │
                              └────────────────────────────────┘
```

The in-memory HashMap provides fast reads. The XML file provides persistence.

### What SharedPreferences Is For

SharedPreferences is designed specifically for **small, simple data**:

- User preferences (dark mode, language, font size)
- App state flags (onboarding completed, first launch, terms accepted)
- Simple cached values (last selected tab, last known username)
- Settings that control app behavior

### What SharedPreferences Is NOT For

- Large datasets → use Room
- Sensitive credentials (passwords, tokens) → use EncryptedSharedPreferences or Keystore
- Lists or collections → use Room
- Complex objects or relationships → use Room
- Large binary data → use files directly

---

## Chapter 2: The History — Why It Was Created

### 2008 — The Problem Android Launched With

When Android 1.0 shipped in September 2008, developers needed a way to persist small pieces of data. The existing options were:

- **SQLite** — powerful but heavyweight for simple key-value needs. Writing SQL for a boolean flag felt absurd.
- **Raw files** — full control but required managing FileOutputStreams, parsing, threading, error handling. Far too complex for simple data.
- **Java Properties files** — available in the JVM but not integrated with Android's Context system.

There was a clear gap: developers needed something as simple as a HashMap but that survived app restarts.

### The Design Decision

The Android team made SharedPreferences to fill this gap. The key design decisions were:

1. **XML storage** — human-readable, debuggable, compatible with Java's existing XML tools
2. **Synchronous reads** — for simplicity. Reading from memory should be instant.
3. **Two write modes** — `commit()` for synchronous confirmed writes, `apply()` for "fire and forget" async writes
4. **Activity and Application scoping** — preferences could be per-Activity (`getPreferences()`) or app-wide (`getSharedPreferences()`)

This design made perfect sense in 2008. Phones had single-core CPUs, apps were simple, and Kotlin Coroutines did not exist. Synchronous APIs were the norm.

### 2008–2014 — Widespread Adoption

SharedPreferences became the universal solution for persistence in Android. Every tutorial, every book, every example used it. By 2012, it was embedded in virtually every Android app ever written. Google's own apps used it. Third-party SDKs used it. It was the default solution.

### 2014–2018 — The ANR Epidemic

As Android apps grew more complex — multiple screens, background services, complex lifecycles — and as the Android user base expanded to include cheap, slow devices with poor I/O performance, a pattern emerged in crash reporting tools:

**ANR (Application Not Responding)** errors, with stack traces pointing to:

```
ActivityThread.handleStopActivity → QueuedWork.waitToFinish
```

Bumble (the dating app) published a detailed post-mortem showing SharedPreferences was causing **6x more ANRs**than expected. Their engineering team spent months tracing the root cause — it was buried in `QueuedWork.java`, a hidden Android system class that most developers had never heard of.

The critical insight (covered deeply in Chapter 8): `apply()` appears async but actually blocks the main thread during Activity lifecycle transitions.

### 2019 — Google Acknowledges the Problems

At Google I/O 2019, Google officially acknowledged SharedPreferences had fundamental design flaws and announced DataStore as its replacement.

The official Android documentation now says:

> **"Caution: DataStore is a modern data storage solution that you should use instead of SharedPreferences."**

### 2021 — DataStore Becomes the Recommendation

With DataStore 1.0 stable released in August 2021, Google formally deprecated SharedPreferences as the recommended solution for new code. However, SharedPreferences itself was NOT removed from the Android API — it still exists and works. Legacy code using it still runs.

### Where SharedPreferences Stands Today (2024+)

SharedPreferences is:

- Still in the Android SDK — not removed, not deprecated at the API level
- Still used in billions of existing apps
- Still used in many third-party SDKs you cannot control
- Still acceptable for very simple use cases where DataStore setup is overkill
- **Not recommended for new code** — DataStore is the official modern replacement

Understanding SharedPreferences thoroughly is still essential because:

1. You will encounter it in legacy codebases
2. Third-party SDKs you use internally use it
3. Understanding why it was replaced makes you a better developer
4. Some codebases still use it intentionally for simplicity

---

## Chapter 3: The XML File on Disk

### File Location

Every SharedPreferences file is stored at:

```
/data/data/com.yourapp/shared_prefs/YOUR_FILE_NAME.xml
```

Breaking this down:

- `/data/data/com.yourapp/` — your app's private sandbox (sandboxed by Android, other apps cannot read this)
- `shared_prefs/` — subdirectory created automatically by the framework
- `YOUR_FILE_NAME.xml` — the file you named when calling `getSharedPreferences("YOUR_FILE_NAME", ...)`

### The XML Format

Here is a real SharedPreferences XML file:

```xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="dark_mode" value="true" />
    <string name="username">john_doe</string>
    <int name="launch_count" value="42" />
    <long name="last_login_ms" value="1710000000000" />
    <float name="text_scale" value="1.5" />
    <set name="selected_tags">
        <string>android</string>
        <string>kotlin</string>
        <string>compose</string>
    </set>
</map>
```

Key observations:

- It's valid XML with a root `<map>` element
- Each key-value pair is one XML element
- The element tag name is the data type
- The key is the `name` attribute
- The value is either the `value` attribute (primitives) or child elements (`Set<String>`)
- The file is human-readable — useful for debugging, but also a security concern (readable by root on rooted devices)

### File Naming

```kotlin
// File will be: /shared_prefs/user_settings.xml
getSharedPreferences("user_settings", Context.MODE_PRIVATE)

// File will be: /shared_prefs/com.wallstreet.app_preferences.xml  
getSharedPreferences("com.wallstreet.app_preferences", Context.MODE_PRIVATE)

// File will be: /shared_prefs/MyActivity.xml (activity class name)
// Called from an Activity only:
getPreferences(Context.MODE_PRIVATE)  // uses Activity class name as filename

// File will be: /shared_prefs/com.yourapp_preferences.xml
PreferenceManager.getDefaultSharedPreferences(context)  // standard settings file
```

### The File is Loaded Entirely

This is critical: **the entire XML file is loaded into memory at once**. There is no lazy loading of individual keys. When you call `getSharedPreferences()` for the first time, the framework reads the entire XML file and parses it into a HashMap.

This has two implications:

1. All keys are available immediately once loaded (reads are instant from memory)
2. Large preference files slow down first access — loading 100KB of XML on the main thread is a performance problem

---

## Chapter 4: How SharedPreferences Loads Into Memory

### The Lazy Loading Mechanism

SharedPreferences uses lazy initialization. The file is NOT loaded when you call `getSharedPreferences()`. It starts loading in the background at that point, but the actual file parsing may not be complete immediately.

Here is the sequence:

```
getSharedPreferences("prefs", MODE_PRIVATE) is called
         │
         ▼
Android checks if this file is already cached in memory
         │
    ┌────┴────┐
   YES        NO
    │          │
    ▼          ▼
Returns      Creates SharedPreferencesImpl object
cached       Starts background thread to read XML file
instance     Returns SharedPreferencesImpl immediately (file may not be loaded yet!)
         │
         ▼
You call getString("key", null)
         │
         ▼
SharedPreferencesImpl.awaitLoadedLocked() is called internally
         │
         ▼
IF the background thread is still reading the file:
    THE CURRENT THREAD BLOCKS AND WAITS ← potential ANR if on main thread!
         │
         ▼
Once loaded: HashMap lookup → returns value instantly
```

The dangerous part: if you call `getSharedPreferences()` and immediately access a value, and the file hasn't finished loading yet, **your thread blocks**. On the main thread, this is an ANR source.

### The In-Memory Cache

Once loaded, SharedPreferences keeps the entire HashMap in memory for the lifetime of the process. This is why reads after the initial load are instant — they're just HashMap lookups, no disk I/O.

```
First read: disk → parse XML → populate HashMap → return value  (slow)
All subsequent reads: HashMap lookup → return value              (instant, microseconds)
```

### Multiple Instances Are the Same Object

Android caches SharedPreferences instances. If you call `getSharedPreferences("prefs", MODE_PRIVATE)` from two different places, you get the same underlying object with the same HashMap.

```kotlin
val prefs1 = context.getSharedPreferences("app", MODE_PRIVATE)
val prefs2 = context.getSharedPreferences("app", MODE_PRIVATE)
// prefs1 === prefs2 — they are literally the same object in memory
```

---

# PART II — SYSTEM LEVEL INTERNALS

---

## Chapter 5: The In-Memory HashMap — How Reads Are Instant

The heart of SharedPreferences is a `HashMap<String, Object>` stored in `SharedPreferencesImpl`. Every key-value pair from the XML file is loaded into this map.

```java
// Simplified from AOSP SharedPreferencesImpl.java
final class SharedPreferencesImpl implements SharedPreferences {
    private Map<String, Object> mMap;  // ← the in-memory store
    private final Object mLock = new Object();
    private boolean mLoaded = false;
    // ...
}
```

### The Read Path

When you call `prefs.getString("username", null)`:

```java
// Simplified AOSP source
public String getString(String key, @Nullable String defValue) {
    synchronized (mLock) {
        awaitLoadedLocked();         // blocks if file not loaded yet
        String v = (String) mMap.get(key);   // HashMap.get() — O(1)
        return v != null ? v : defValue;
    }
}
```

The `synchronized (mLock)` is a lock to prevent concurrent reads from racing with ongoing writes. For pure reads, this lock is very briefly held.

This is why reads feel instant — it's just `HashMap.get(key)` once loaded.

### Thread Safety of Reads

SharedPreferences uses the `mLock` object to synchronize reads and writes. This means:

- Multiple threads can read simultaneously (they all wait for the lock briefly)
- A read during a write will wait for the write to update the HashMap
- This is a single-writer, many-readers pattern with a simple mutex

---

## Chapter 6: The Editor — How Writes Work Internally

Writing to SharedPreferences requires going through the `Editor` interface. Here is what happens internally:

### Step 1: Create an Editor

```kotlin
val editor = prefs.edit()
```

This creates an `EditorImpl` object with its own **temporary HashMap** — a copy of the current preferences for staging changes.

```java
// Simplified AOSP EditorImpl
public final class EditorImpl implements Editor {
    private final Object mEditorLock = new Object();
    private final Map<String, Object> mModified = new HashMap<>();  // ← staging area
    private boolean mClear = false;
    // ...
}
```

### Step 2: Put Values

```kotlin
editor.putString("username", "John")
editor.putBoolean("dark_mode", true)
```

These calls simply add to `mModified` — **no disk I/O, no file access, just HashMap.put()**.

```java
public Editor putString(String key, @Nullable String value) {
    synchronized (mEditorLock) {
        mModified.put(key, value);  // just a HashMap put — instant
        return this;
    }
}
```

### Step 3: commit() or apply()

This is where the write actually happens. The Editor applies `mModified` on top of the main `mMap` and schedules a disk write.

```
mModified (staging):     { username: "John", dark_mode: true }
mMap (current in-memory): { username: "Alice", launch_count: 5 }

After merge:
mMap (updated in-memory): { username: "John", dark_mode: true, launch_count: 5 }
```

The merge is called `commitToMemory()` internally. After this, any new reads immediately return the new values — even before the disk write completes.

---

## Chapter 7: commit() vs apply() — The Deep Difference

This is the most important distinction in SharedPreferences. Most developers use `apply()` without understanding what it actually does.

### commit() — Synchronous Write

```kotlin
val success = editor.commit()  // returns Boolean
```

**What happens internally:**

```
editor.commit() is called on calling thread
         │
         ▼
commitToMemory() — updates in-memory HashMap synchronously
         │
         ▼
writeToDiskRunnable runs ON THE CALLING THREAD
         │
         ▼
Serializes HashMap to XML string
         │
         ▼
Writes XML to .xml.bak (backup file)
         │
         ▼
Renames .xml.bak to .xml (atomic rename)
         │
         ▼
Calls fsync() — waits for kernel to confirm physical disk write
         │
         ▼
Returns true (success) or false (failure) to your code
```

**Key facts about commit():**

- Blocks the calling thread until disk write completes
- Returns `true` on success, `false` on failure — you know if it worked
- If called on the main thread: **instant ANR risk** on slow devices
- Safe to call on a background thread
- Guarantees data is on disk when it returns

### apply() — "Asynchronous" Write (The Dangerous One)

```kotlin
editor.apply()  // returns void
```

**What happens internally:**

```
editor.apply() is called
         │
         ▼
commitToMemory() — updates in-memory HashMap SYNCHRONOUSLY
         │
         ▼ (in-memory update is immediate — reads see new values instantly)
         │
         ▼
Creates awaitCommit Runnable (a CountDownLatch)
         │
         ▼
QueuedWork.addFinisher(awaitCommit)  ← THIS IS THE TRAP
         │
         ▼
Schedules writeToDiskRunnable on background thread
         │
         ▼
apply() returns immediately — your code continues
         │
         ▼ (somewhere later, on background thread)
         │
         ▼
writeToDiskRunnable runs: serialize XML → write → fsync()
         │
         ▼
CountDownLatch counts down (signals completion)
         │
         ▼
QueuedWork.removeFinisher(awaitCommit)
```

apply() looks fast — it returns immediately. But the `awaitCommit` Runnable added to `QueuedWork` is the hidden trap.

### The apply() Return Value Problem

`apply()` returns `void`. You have no way to know if the write succeeded or failed. If the disk is full, if there's a permissions error, if the device runs out of battery mid-write — `apply()` gives you absolutely no indication. Your data is silently lost.

```kotlin
editor.apply()
// Did it work? You have no idea. No return value, no callback, no error.
```

---

## Chapter 8: The QueuedWork Trap — The Hidden ANR Mechanism

This is the most important chapter in this book for understanding SharedPreferences dangers. This explains the root cause of thousands of ANRs in production apps.

### What Is QueuedWork?

`QueuedWork` is an Android framework class (`android.app.QueuedWork`) that maintains a list of pending asynchronous work items that **must complete before certain lifecycle events**.

### The Trap: waitToFinish()

`QueuedWork` has a method called `waitToFinish()`. The Android framework calls this method automatically at the following lifecycle points:

```
Activity.onStop()          → ActivityThread.handleStopActivity()     → QueuedWork.waitToFinish()
Activity.onPause()         → ActivityThread.handlePauseActivity()    → QueuedWork.waitToFinish()
Service.onStartCommand()   → ActivityThread.handleServiceArgs()      → QueuedWork.waitToFinish()
Service.onDestroy()        → ActivityThread.handleStopService()      → QueuedWork.waitToFinish()
BroadcastReceiver.onReceive() ends → QueuedWork.waitToFinish()
```

**These are all called on the main thread.**

When `waitToFinish()` is called, it **blocks the main thread** until every item in the `QueuedWork` finisher list is complete.

When you call `apply()`, it adds `awaitCommit` (a CountDownLatch waiter) to `QueuedWork`'s finisher list. This means:

```
User rotates device → Activity.onStop() triggered
         │
         ▼
ActivityThread.handleStopActivity() runs ON MAIN THREAD
         │
         ▼
QueuedWork.waitToFinish() is called
         │
         ▼
Main thread BLOCKS waiting for pending apply() disk writes to complete
         │
         ▼
If disk write takes 200ms → UI frozen for 200ms
If disk write takes 2 seconds (slow device, large file) → ANR!
```

### The Stack Trace You'll See in Crash Reports

```
"main" prio=5 tid=1 WAIT
  at java.lang.Object.wait(Native Method)
  at android.app.QueuedWork.waitToFinish(QueuedWork.java:88)
  at android.app.ActivityThread.handleStopActivity(ActivityThread.java:3929)
  at android.app.ActivityThread.access$1200(ActivityThread.java:172)
  at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1615)
  at android.os.Handler.dispatchMessage(Handler.java:111)
  at android.os.Looper.loop(Looper.java:194)
  at android.app.ActivityThread.main(ActivityThread.java:5637)
```

Notice: the stack trace does NOT mention SharedPreferences. It points at `ActivityThread.handleStopActivity`. This is why these ANRs are so hard to diagnose — you're looking for a SharedPreferences call but the stack trace shows Activity lifecycle code.

### The Frequency of the Problem

The `waitToFinish()` is called:

- **Every time an Activity stops** — this happens on EVERY screen navigation, EVERY rotation, EVERY time the user presses Home
- **Every time a Service starts or stops**
- On some Android versions, even during `onPause()`

If you call `apply()` even once per screen — which is very common — the ANR risk is present on every single screen transition.

### The fsync() Factor

The actual time taken is determined by `fsync()` — the Linux system call that flushes the kernel's write buffer to physical storage. `fsync()` time varies enormously:

- Fast device (SSD-equivalent flash): 1–5ms
- Average device: 10–50ms
- Slow/cheap device: 100–500ms
- Device under heavy I/O load: 1000ms+

On a device under load or with slow storage, `waitToFinish()` waiting for `fsync()` to complete can easily exceed the 5-second ANR threshold.

### Android 8.0 Optimization

Android 8.0 (Oreo) partially improved this by changing `waitToFinish()` to actively process the work queue rather than just waiting, reducing but not eliminating the block time. The fundamental problem — main thread blocking on disk I/O — remains.

### Why You Cannot Fix This With apply()

Many developers believe using `apply()` instead of `commit()` solves the ANR problem. It does NOT. The `QueuedWork`mechanism means you will always have some main-thread block. The only real fix is to not use SharedPreferences at all, or to use it only for non-critical data where brief pauses are acceptable.

---

## Chapter 9: The Listener System — How Change Callbacks Work

SharedPreferences provides `OnSharedPreferenceChangeListener` to be notified when values change.

### How It Works Internally

SharedPreferences uses a `WeakHashMap` to store listeners:

```java
// Simplified AOSP
private final WeakHashMap<OnSharedPreferenceChangeListener, Object> mListeners = 
    new WeakHashMap<>();
```

The `WeakHashMap` means listeners can be garbage collected if you don't hold a strong reference to them — a common source of bugs.

### Registration and Callback Timing

```kotlin
val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
    // Called on the MAIN THREAD
    // key is the changed key (or null on clear())
    when (key) {
        "dark_mode" -> updateTheme(prefs.getBoolean(key, false))
        "language"  -> updateLanguage(prefs.getString(key, "en") ?: "en")
    }
}

prefs.registerOnSharedPreferenceChangeListener(listener)
// Later:
prefs.unregisterOnSharedPreferenceChangeListener(listener)
```

The callback fires:

- **After** `apply()` updates the in-memory map (before disk write)
- **After** `commit()` completes (after disk write)
- On the **main thread** always

---

# PART III — COMPLETE API REFERENCE

---

## Chapter 10: Getting a SharedPreferences Instance

There are three ways to get a SharedPreferences instance. Each has a different scope.

### Method 1: getSharedPreferences() — Most Common

```kotlin
val prefs = context.getSharedPreferences("file_name", Context.MODE_PRIVATE)
```

- Use from any `Context` (Activity, Service, Application, etc.)
- `"file_name"` becomes the XML filename: `shared_prefs/file_name.xml`
- Shared across your entire app — same file, same object, anywhere you use the same name
- **This is what you should use for app-wide preferences**

### Method 2: getPreferences() — Activity-Specific

```kotlin
// Only callable from an Activity
val prefs = activity.getPreferences(Context.MODE_PRIVATE)
```

- Automatically uses the Activity's class name as the file name
- `MainActivity` → `shared_prefs/MainActivity.xml`
- Only use this for data specific to one Activity

### Method 3: PreferenceManager.getDefaultSharedPreferences() — Standard Settings

```kotlin
val prefs = PreferenceManager.getDefaultSharedPreferences(context)
```

- Returns the app's default shared preferences file
- File name: `{package_name}_preferences.xml` (e.g., `com.wallstreet_preferences.xml`)
- This is what Android's Preferences UI framework uses automatically
- Use this when your data relates to the app's Settings screen

### Naming Best Practice

Use your package name as a prefix to avoid conflicts when your preferences file name might collide with other app files:

```kotlin
// Good — uniquely identifies your file
context.getSharedPreferences("com.wallstreet.user_settings", Context.MODE_PRIVATE)

// OK — simple but could collide in unusual scenarios
context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
```

---

## Chapter 11: All Access Modes Explained

The second parameter to `getSharedPreferences()` is the mode. Understanding each is important.

```kotlin
getSharedPreferences("name", Context.MODE_PRIVATE)      // ← always use this
getSharedPreferences("name", Context.MODE_WORLD_READABLE) // deprecated, dangerous
getSharedPreferences("name", Context.MODE_WORLD_WRITEABLE) // deprecated, dangerous
getSharedPreferences("name", Context.MODE_MULTI_PROCESS)  // use with caution
```

### MODE_PRIVATE (Always Use This)

```kotlin
Context.MODE_PRIVATE  // value = 0
```

- Only your app can read or write this file
- This is the only mode you should ever use in modern apps
- All other modes are either deprecated or have serious security implications

### MODE_WORLD_READABLE and MODE_WORLD_WRITEABLE (Never Use)

```kotlin
Context.MODE_WORLD_READABLE   // DEPRECATED since API 17
Context.MODE_WORLD_WRITEABLE  // DEPRECATED since API 17
```

- Allowed other apps to read/write your preferences file
- Removed behavior in Android 7.0 (API 24) — throws `SecurityException` if used
- **Never use these.** They are a major security vulnerability even on older devices.

### MODE_MULTI_PROCESS (Legacy, Unreliable)

```kotlin
Context.MODE_MULTI_PROCESS  // value = 4
```

- Intended for accessing SharedPreferences across multiple processes
- Deprecated since API 23
- Was unreliable and prone to data corruption even when it worked
- If you need multi-process data sharing, use ContentProvider or a database instead

---

## Chapter 12: All Data Types You Can Store

SharedPreferences natively supports 6 types. All are stored as XML elements.

|Type|Method|XML|Default|
|---|---|---|---|
|`Boolean`|`putBoolean()` / `getBoolean()`|`<boolean name="k" value="true"/>`|`false`|
|`Int`|`putInt()` / `getInt()`|`<int name="k" value="42"/>`|`0`|
|`Long`|`putLong()` / `getLong()`|`<long name="k" value="1234567"/>`|`0L`|
|`Float`|`putFloat()` / `getFloat()`|`<float name="k" value="1.5"/>`|`0.0f`|
|`String`|`putString()` / `getString()`|`<string name="k">val</string>`|`null`|
|`Set<String>`|`putStringSet()` / `getStringSet()`|`<set name="k"><string>v</string></set>`|`null`|

**Note:** There is no native support for `Double`. Use `Long` with `Double.toBits()` / `Double.fromBits()` as a workaround.

### Storing Non-Native Types

**Double:**

```kotlin
// Store
editor.putLong("latitude", 37.7749.toBits())

// Read
val latitude = Double.fromBits(prefs.getLong("latitude", 0.0.toBits()))
```

**Enum:**

```kotlin
enum class ThemeMode { LIGHT, DARK, SYSTEM }

// Store
editor.putString("theme", ThemeMode.DARK.name)

// Read
val theme = ThemeMode.valueOf(prefs.getString("theme", "SYSTEM") ?: "SYSTEM")
```

**Data class (serialized to JSON):**

```kotlin
// Requires kotlinx.serialization or Gson
@Serializable
data class UserProfile(val name: String, val email: String)

// Store
editor.putString("profile", Json.encodeToString(profile))

// Read
val profile = Json.decodeFromString<UserProfile>(
    prefs.getString("profile", null) ?: return
)
```

---

## Chapter 13: Reading Data — Every Method

All read methods follow the same pattern: `getType(key, defaultValue)`

```kotlin
// Boolean
val isDark: Boolean = prefs.getBoolean("dark_mode", false)

// Int
val count: Int = prefs.getInt("launch_count", 0)

// Long
val timestamp: Long = prefs.getLong("last_login", 0L)

// Float
val scale: Float = prefs.getFloat("text_scale", 1.0f)

// String (nullable — key might not exist)
val username: String? = prefs.getString("username", null)

// String with non-null default
val language: String = prefs.getString("language", "en") ?: "en"

// Set<String> (nullable)
val tags: Set<String>? = prefs.getStringSet("tags", null)

// Set<String> with default
val tags: Set<String> = prefs.getStringSet("tags", emptySet()) ?: emptySet()
```

### Read All Keys

```kotlin
// Get the entire Map — snapshot of current preferences
val allPrefs: Map<String, *> = prefs.all
// Returns Map<String, Any?> — values are Any (Boolean, Int, String, etc.)

// Iterate all entries
for ((key, value) in prefs.all) {
    println("$key = $value (${value?.javaClass?.simpleName})")
}
```

### The Default Value Matters

The second parameter to every get method is the **default value** — returned when the key does not exist in the file. Choose defaults carefully:

```kotlin
// Bad default — null means callers must null-check everywhere
val name: String? = prefs.getString("name", null)

// Better — provide a sensible fallback
val name: String = prefs.getString("name", "Guest") ?: "Guest"

// The ?: "Guest" is needed because getString is nullable in Kotlin even with a default
// (due to Java interop — the annotation says it could be null)
```

---

## Chapter 14: Writing Data — Every Method

### The Editor Pattern

All writes go through `SharedPreferences.Editor`. You must call `apply()` or `commit()` at the end — without it, nothing is written.

```kotlin
val editor = prefs.edit()

// All the put methods:
editor.putBoolean("dark_mode", true)
editor.putInt("launch_count", 5)
editor.putLong("last_login", System.currentTimeMillis())
editor.putFloat("text_scale", 1.5f)
editor.putString("username", "john_doe")
editor.putStringSet("favorite_tags", setOf("kotlin", "android"))

// MUST call one of these — nothing is written without it:
editor.apply()    // async (with hidden dangers — see Chapter 8)
// OR
editor.commit()   // sync (safe on background thread)
```

### Method Chaining (Fluent API)

All Editor methods return `this` — you can chain them:

```kotlin
prefs.edit()
    .putBoolean("dark_mode", true)
    .putString("language", "en")
    .putInt("launch_count", 1)
    .apply()
```

This is cleaner and equivalent — the chain creates one Editor and applies all changes at once.

### Kotlin Extension Function (apply{} block)

Kotlin's standard library provides an extension for cleaner syntax:

```kotlin
// Using Kotlin's edit extension — cleaner than chaining
prefs.edit {
    putBoolean("dark_mode", true)
    putString("language", "en")
    putInt("launch_count", 1)
    // apply() is called automatically when the block ends
}

// This uses commit() instead of apply()
prefs.edit(commit = true) {
    putBoolean("dark_mode", true)
}
```

The `edit { }` extension function is from `androidx.core:core-ktx`. It calls `apply()` by default. Use `edit(commit = true) { }` for synchronous commits.

---

## Chapter 15: Deleting Data

### Remove a Single Key

```kotlin
prefs.edit {
    remove("username")
}
// After this, prefs.getString("username", null) returns null
```

### Clear All Data

```kotlin
prefs.edit {
    clear()
}
// All keys removed — the XML file will be nearly empty: <map></map>
```

### Check Before Remove (Defensive)

```kotlin
if (prefs.contains("username")) {
    prefs.edit { remove("username") }
}
// But this is usually unnecessary — remove() on a non-existent key does nothing
```

### Partial Clear (Selective Logout)

```kotlin
prefs.edit {
    // Keep device-level settings, clear user-specific data
    remove("user_id")
    remove("auth_token")
    remove("user_email")
    // dark_mode, language, etc. remain
}
```

---

## Chapter 16: Checking If a Key Exists

```kotlin
// Check if a key exists
val hasUsername: Boolean = prefs.contains("username")

// Common pattern — do something only if key doesn't exist
if (!prefs.contains("onboarding_shown")) {
    showOnboarding()
    prefs.edit { putBoolean("onboarding_shown", true) }
}

// Alternative — use the default value approach
val isFirstLaunch: Boolean = !prefs.getBoolean("launched_before", false)
if (isFirstLaunch) {
    prefs.edit { putBoolean("launched_before", true) }
}
```

---

## Chapter 17: Change Listeners

### Registering a Listener

```kotlin
class MyActivity : AppCompatActivity() {
    
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            "dark_mode" -> {
                val isDark = prefs.getBoolean(key, false)
                applyTheme(isDark)
            }
            "language" -> {
                val lang = prefs.getString(key, "en") ?: "en"
                applyLanguage(lang)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }
    
    override fun onPause() {
        super.onPause()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }
}
```

### The WeakReference Trap

SharedPreferences stores listeners in a `WeakHashMap`. If you create a listener as a lambda and don't store a reference to it, the garbage collector removes it and you stop receiving callbacks:

```kotlin
// ❌ WRONG — listener can be garbage collected immediately
prefs.registerOnSharedPreferenceChangeListener { prefs, key ->
    // This may never be called — no strong reference held
}

// ✅ CORRECT — store reference as a member variable
private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
    // This is held strongly — will receive callbacks
}
prefs.registerOnSharedPreferenceChangeListener(listener)
```

### Always Unregister

Always unregister in the opposite lifecycle method to prevent leaks and ghost callbacks:

```kotlin
// Register in onStart, unregister in onStop
override fun onStart()  { prefs.registerOnSharedPreferenceChangeListener(listener) }
override fun onStop()   { prefs.unregisterOnSharedPreferenceChangeListener(listener) }

// OR register in onResume, unregister in onPause
override fun onResume() { prefs.registerOnSharedPreferenceChangeListener(listener) }
override fun onPause()  { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
```

---

# PART IV — ARCHITECTURE & PATTERNS

---

## Chapter 18: SharedPreferences in Clean Architecture

If you still use SharedPreferences (legacy code, migration path), wrap it properly.

### Layer Structure

```
PRESENTATION: ViewModel — uses use cases
DOMAIN:       Use Cases + Repository Interface
DATA:         Repository Implementation wrapping SharedPreferences
```

### The Interface (Domain Layer)

```kotlin
// domain/repository/UserPreferencesRepository.kt
interface UserPreferencesRepository {
    fun isDarkMode(): Boolean
    fun setDarkMode(enabled: Boolean)
    fun getLanguage(): String
    fun setLanguage(lang: String)
    fun isOnboardingCompleted(): Boolean
    fun setOnboardingCompleted()
    fun clearUserData()
}
```

### The Implementation (Data Layer)

```kotlin
// data/repository/UserPreferencesRepositoryImpl.kt
class UserPreferencesRepositoryImpl(
    private val prefs: SharedPreferences
) : UserPreferencesRepository {

    companion object {
        private const val KEY_DARK_MODE    = "dark_mode"
        private const val KEY_LANGUAGE     = "language"
        private const val KEY_ONBOARDING   = "onboarding_completed"
    }

    override fun isDarkMode(): Boolean =
        prefs.getBoolean(KEY_DARK_MODE, false)

    override fun setDarkMode(enabled: Boolean) =
        prefs.edit { putBoolean(KEY_DARK_MODE, enabled) }

    override fun getLanguage(): String =
        prefs.getString(KEY_LANGUAGE, "en") ?: "en"

    override fun setLanguage(lang: String) =
        prefs.edit { putString(KEY_LANGUAGE, lang) }

    override fun isOnboardingCompleted(): Boolean =
        prefs.getBoolean(KEY_ONBOARDING, false)

    override fun setOnboardingCompleted() =
        prefs.edit { putBoolean(KEY_ONBOARDING, true) }

    override fun clearUserData() = prefs.edit {
        remove(KEY_DARK_MODE)
        remove(KEY_LANGUAGE)
        // Keep KEY_ONBOARDING — device-level flag
    }
}
```

---

## Chapter 19: SharedPreferences in Kotlin — Modern Wrappers

### Property Delegate Wrapper

Kotlin's property delegates let you access SharedPreferences as if they were regular properties — no boilerplate:

```kotlin
// Generic delegate for any SharedPreferences value
class SharedPreference<T>(
    private val prefs: SharedPreferences,
    private val key: String,
    private val default: T
) {
    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return prefs.all[key] as? T ?: default
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        prefs.edit {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Int     -> putInt(key, value)
                is Long    -> putLong(key, value)
                is Float   -> putFloat(key, value)
                is String  -> putString(key, value)
                else       -> throw IllegalArgumentException("Unsupported type: ${value?.javaClass}")
            }
        }
    }
}

// Usage — preferences as properties
class AppSettings(prefs: SharedPreferences) {
    var isDarkMode: Boolean by SharedPreference(prefs, "dark_mode", false)
    var language: String    by SharedPreference(prefs, "language", "en")
    var launchCount: Int    by SharedPreference(prefs, "launch_count", 0)
}

// Use it:
val settings = AppSettings(prefs)
settings.isDarkMode = true    // writes to SharedPreferences automatically
val dark = settings.isDarkMode // reads from SharedPreferences automatically
```

### Extension Functions

```kotlin
// Extension to simplify write syntax (already in AndroidX KTX)
fun SharedPreferences.edit(
    commit: Boolean = false,
    action: SharedPreferences.Editor.() -> Unit
) {
    val editor = edit()
    action(editor)
    if (commit) editor.commit() else editor.apply()
}

// Usage
prefs.edit {
    putBoolean("dark_mode", true)
    putString("language", "en")
}
```

### Inline Operator Functions

```kotlin
// Access preferences like a map
operator fun SharedPreferences.contains(key: String) = this.contains(key)

operator fun SharedPreferences.Editor.set(key: String, value: Any?) {
    when (value) {
        is Boolean    -> putBoolean(key, value)
        is Int        -> putInt(key, value)
        is Long       -> putLong(key, value)
        is Float      -> putFloat(key, value)
        is String     -> putString(key, value)
        is Set<*>     -> @Suppress("UNCHECKED_CAST") putStringSet(key, value as Set<String>)
        null          -> remove(key)
    }
}

// Usage
prefs.edit {
    this["dark_mode"] = true
    this["username"]  = "john"
    this["count"]     = 42
}
```

---

## Chapter 20: SharedPreferences with Koin DI

```kotlin
// di/PreferencesModule.kt
val preferencesModule = module {

    // SharedPreferences instance
    single<SharedPreferences> {
        androidContext().getSharedPreferences(
            "com.yourapp.preferences",
            Context.MODE_PRIVATE
        )
    }

    // Repository
    single<UserPreferencesRepository> {
        UserPreferencesRepositoryImpl(get())
    }

    // Use Cases
    factory { GetDarkModeUseCase(get()) }
    factory { SetDarkModeUseCase(get()) }
    factory { IsOnboardingCompletedUseCase(get()) }

    // ViewModel
    viewModel {
        SettingsViewModel(get(), get())
    }
}
```

### Why `single` for SharedPreferences

```kotlin
// ✅ single — one instance, same HashMap everywhere, correct
single<SharedPreferences> { context.getSharedPreferences("prefs", MODE_PRIVATE) }

// ❌ factory — multiple instances pointing to same file
// Not data corruption like DataStore, but wasteful and inconsistent
factory<SharedPreferences> { context.getSharedPreferences("prefs", MODE_PRIVATE) }
```

Unlike DataStore, multiple SharedPreferences instances pointing to the same file won't corrupt data (Android caches them at the framework level), but using `single` is still the correct pattern — it's explicit, efficient, and consistent.

---

## Chapter 21: Multi-Process SharedPreferences

If your app has multiple processes (unusual but possible with services declared with `android:process`), SharedPreferences behaves unexpectedly.

### The Problem

Each process has its own in-memory cache of the HashMap. When Process A writes a value, Process B's cache is NOT updated. Process B will return the old value until it reloads the file.

```
Process A writes: dark_mode = true
  → HashMap in Process A updated ✅
  → XML file updated ✅
  → HashMap in Process B: still has dark_mode = false ❌
```

### MODE_MULTI_PROCESS (Deprecated "Fix")

`Context.MODE_MULTI_PROCESS` was supposed to fix this by reloading the file on every access. But it was unreliable and deprecated in API 23 because it still didn't guarantee consistency.

### The Right Solution for Multi-Process

If you genuinely need cross-process data sharing:

- Use a `ContentProvider`
- Use a bound Service with a Messenger
- Use Room (SQLite handles multi-process safely)
- Use DataStore with proper multi-process configuration (DataStore 1.1.x+)

---

# PART V — DANGERS & BEST PRACTICES

---

## Chapter 22: The 7 Dangers of SharedPreferences

### Danger 1: ANR from QueuedWork (Most Dangerous)

As explained in Chapter 8, `apply()` blocks the main thread during Activity and Service lifecycle events via `QueuedWork.waitToFinish()`.

```
Risk level: HIGH
Frequency: Every app transition if you call apply()
Symptom: Frozen UI, ANR in crash reports
Fix: Migrate to DataStore, or call commit() on a background thread
```

### Danger 2: Silent Write Failures

`apply()` returns void. Write failures are swallowed silently.

```kotlin
prefs.edit { putString("user_data", importantData) }
.apply()
// Did it succeed? You will NEVER know.
```

```
Risk level: MEDIUM
Frequency: Rare — disk full, hardware error
Symptom: Data disappears without error
Fix: Use commit() and check return value, or use DataStore
```

### Danger 3: No Type Safety

```kotlin
prefs.edit().putBoolean("user_id", true)  // wrong type
val id: Int = prefs.getInt("user_id", 0)  // ClassCastException at runtime
```

```
Risk level: MEDIUM
Frequency: Common in large codebases with string keys scattered everywhere
Symptom: ClassCastException crash at runtime
Fix: Centralize all keys as constants, use a typed wrapper
```

### Danger 4: The WeakReference Listener Trap

```kotlin
prefs.registerOnSharedPreferenceChangeListener { _, _ ->
    updateUI()  // This lambda has no strong reference — GC removes it
}
// Listener may never fire
```

```
Risk level: LOW-MEDIUM
Frequency: Easy to hit for beginners
Symptom: Listener stops firing randomly
Fix: Store listener as member variable
```

### Danger 5: Main Thread Loading Block

```kotlin
// On main thread, during app startup:
val prefs = context.getSharedPreferences("prefs", MODE_PRIVATE)
val name = prefs.getString("name", null)  // may block if file not loaded yet
```

```
Risk level: MEDIUM
Frequency: On first access, especially during app startup
Symptom: Slow startup on older/cheaper devices
Fix: Move first access off main thread, or use DataStore
```

### Danger 6: Unprotected Sensitive Data

```kotlin
// BAD — stored as plain text in readable XML
prefs.edit { putString("auth_token", "eyJhbGciOiJIUzI1...") }
// Readable by root on rooted devices
// Readable by ADB on debug builds
```

```
Risk level: HIGH for sensitive data
Frequency: Common mistake
Symptom: Data exposed on rooted/debuggable devices
Fix: Use EncryptedSharedPreferences or Keystore
```

### Danger 7: XML Corruption

Unlike DataStore's atomic rename, SharedPreferences uses a backup file approach that can fail. If power is lost at the wrong moment, the XML file can become invalid — partially written, non-parseable XML.

```
Risk level: LOW
Frequency: Rare — requires power loss at exact moment of write
Symptom: ClassCastException or NumberFormatException on read
Fix: Wrap reads in try-catch, or use DataStore
```

---

## Chapter 23: Common Mistakes & How to Avoid Them

### Mistake 1: Using commit() on the Main Thread

```kotlin
// ❌ WRONG — blocks UI thread
prefs.edit().putBoolean("dark_mode", true).commit()

// ✅ CORRECT — use apply() or commit() on background thread
prefs.edit { putBoolean("dark_mode", true) }  // uses apply()

// ✅ CORRECT — commit() on background thread if you need confirmation
viewModelScope.launch(Dispatchers.IO) {
    val success = prefs.edit()
        .putBoolean("dark_mode", true)
        .commit()
}
```

### Mistake 2: Forgetting to Call apply() or commit()

```kotlin
// ❌ WRONG — nothing is saved
val editor = prefs.edit()
editor.putString("username", "John")
// apply() or commit() never called — data discarded!

// ✅ CORRECT
prefs.edit {
    putString("username", "John")
    // apply() called automatically by the extension
}
```

### Mistake 3: Lambda Listener Not Held

```kotlin
// ❌ WRONG — garbage collected
prefs.registerOnSharedPreferenceChangeListener { _, key ->
    println("Changed: $key")
}

// ✅ CORRECT
private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
    println("Changed: $key")
}
// In onResume:
prefs.registerOnSharedPreferenceChangeListener(listener)
// In onPause:
prefs.unregisterOnSharedPreferenceChangeListener(listener)
```

### Mistake 4: String Keys Scattered Everywhere

```kotlin
// ❌ WRONG — typos cause silent bugs
prefs.getBoolean("darkMode", false)   // somewhere
prefs.edit { putBoolean("dark_mode", true) }  // different spelling! Different key!

// ✅ CORRECT — constants in one place
object PrefsKeys {
    const val DARK_MODE = "dark_mode"
    const val LANGUAGE  = "language"
}

prefs.getBoolean(PrefsKeys.DARK_MODE, false)
prefs.edit { putBoolean(PrefsKeys.DARK_MODE, true) }
```

### Mistake 5: Modifying a Set<String> In-Place

```kotlin
// ❌ WRONG — mutating the returned set is undefined behavior
val tags = prefs.getStringSet("tags", null)
tags?.add("new_tag")  // This MAY or MAY NOT be saved — implementation detail
prefs.edit { putStringSet("tags", tags) }

// ✅ CORRECT — create a new set
val tags = prefs.getStringSet("tags", null) ?: emptySet()
val newTags = tags.toMutableSet().also { it.add("new_tag") }
prefs.edit { putStringSet("tags", newTags) }
```

The Android documentation explicitly warns: "Note that you must not modify the set instance returned, as the store does not guarantee that its contents will not be modified."

### Mistake 6: Large Data in SharedPreferences

```kotlin
// ❌ WRONG — entire preferences file loaded into memory
// If this string is 100KB, your whole prefs file becomes heavy
prefs.edit { putString("trade_history_json", hugeJson) }

// ✅ CORRECT — large data belongs in Room or files
// Store only simple flags/settings in SharedPreferences
```

### Mistake 7: Different File Names for Same Data

```kotlin
// Activity A:
context.getSharedPreferences("settings", MODE_PRIVATE).getBoolean("dark_mode", false)

// Activity B — different file name!
context.getSharedPreferences("app_settings", MODE_PRIVATE).getBoolean("dark_mode", false)
// This reads from a DIFFERENT file — always returns default!
```

---

## Chapter 24: Security Considerations

### What's Readable

On a **rooted device** or with **ADB on a debug build**, your SharedPreferences XML files are readable by:

- Other apps with root privileges
- Any tool connected via ADB to a debuggable app

```bash
# On a rooted device or via ADB (debug builds only):
adb shell run-as com.yourapp cat /data/data/com.yourapp/shared_prefs/prefs.xml
# Output: your entire preferences file in plain XML
```

### What NOT to Store in SharedPreferences

```kotlin
// ❌ NEVER store these in plain SharedPreferences:
prefs.edit { putString("password", userPassword) }          // plain text password
prefs.edit { putString("auth_token", jwtToken) }             // auth token
prefs.edit { putString("credit_card", cardNumber) }          // payment data
prefs.edit { putString("api_key", secretApiKey) }            // API secrets
prefs.edit { putString("ssn", socialSecurityNumber) }        // personal identity
```

### EncryptedSharedPreferences (The Secure Alternative)

For sensitive data that you must store locally, use `EncryptedSharedPreferences` from the AndroidX Security library:

```kotlin
// build.gradle.kts
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

```kotlin
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// Create master key (backed by Android Keystore)
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

// Create encrypted SharedPreferences
val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "secret_prefs",           // file name
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,   // key encryption
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM  // value encryption
)

// Use exactly like regular SharedPreferences
encryptedPrefs.edit { putString("auth_token", token) }
val token = encryptedPrefs.getString("auth_token", null)
```

**How it works internally:**

- Keys are encrypted with AES-256-SIV (deterministic encryption — same key produces same ciphertext, so the HashMap can look them up)
- Values are encrypted with AES-256-GCM (authenticated encryption)
- The master key is stored in the Android Keystore — hardware-backed on supported devices
- The XML file contains only encrypted bytes — no readable values

---

# PART VI — REAL-WORLD EXAMPLES

---

## Chapter 25: Real-World Patterns — 10 Complete Examples

### Example 1: App Settings Manager

```kotlin
class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences(
        "com.yourapp.settings", Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_DARK_MODE      = "dark_mode"
        private const val KEY_LANGUAGE       = "language"
        private const val KEY_NOTIFICATIONS  = "notifications_enabled"
        private const val KEY_FONT_SIZE      = "font_size_sp"
        private const val KEY_ONBOARDING     = "onboarding_completed"
    }

    // Dark mode
    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit { putBoolean(KEY_DARK_MODE, value) }

    // Language
    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit { putString(KEY_LANGUAGE, value) }

    // Notifications
    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFICATIONS, value) }

    // Font size
    var fontSizeSp: Int
        get() = prefs.getInt(KEY_FONT_SIZE, 14)
        set(value) = prefs.edit { putInt(KEY_FONT_SIZE, value) }

    // Onboarding
    val isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)

    fun markOnboardingCompleted() = prefs.edit {
        putBoolean(KEY_ONBOARDING, true)
    }
}
```

### Example 2: Login Session (Non-Sensitive Data Only)

```kotlin
class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID    = "user_id"
        private const val KEY_USER_NAME  = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_IS_LOGGED  = "is_logged_in"
    }

    val isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED, false)

    val userId: String?
        get() = prefs.getString(KEY_USER_ID, null)

    val userName: String
        get() = prefs.getString(KEY_USER_NAME, "Guest") ?: "Guest"

    fun saveSession(userId: String, name: String, email: String) {
        prefs.edit {
            putBoolean(KEY_IS_LOGGED, true)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
        }
    }

    fun clearSession() = prefs.edit {
        remove(KEY_IS_LOGGED)
        remove(KEY_USER_ID)
        remove(KEY_USER_NAME)
        remove(KEY_USER_EMAIL)
    }
}
```

### Example 3: Launch Counter + First Launch Detection

```kotlin
class LaunchTracker(context: Context) {

    private val prefs = context.getSharedPreferences("launch_tracker", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAUNCH_COUNT  = "launch_count"
        private const val KEY_FIRST_LAUNCH  = "first_launch_ms"
        private const val KEY_LAST_LAUNCH   = "last_launch_ms"
    }

    val launchCount: Int
        get() = prefs.getInt(KEY_LAUNCH_COUNT, 0)

    val isFirstEverLaunch: Boolean
        get() = !prefs.contains(KEY_FIRST_LAUNCH)

    val daysSinceFirstLaunch: Int
        get() {
            val firstLaunchMs = prefs.getLong(KEY_FIRST_LAUNCH, 0L)
            return ((System.currentTimeMillis() - firstLaunchMs) / 86_400_000L).toInt()
        }

    fun recordLaunch() {
        val now = System.currentTimeMillis()
        prefs.edit {
            val count = prefs.getInt(KEY_LAUNCH_COUNT, 0)
            putInt(KEY_LAUNCH_COUNT, count + 1)
            putLong(KEY_LAST_LAUNCH, now)
            if (!prefs.contains(KEY_FIRST_LAUNCH)) {
                putLong(KEY_FIRST_LAUNCH, now)
            }
        }
    }

    fun shouldShowRatingPrompt(): Boolean {
        val count = launchCount
        return count == 10 || count == 30 || count == 60
    }
}
```

### Example 4: Onboarding State Machine

```kotlin
class OnboardingManager(context: Context) {

    private val prefs = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)

    enum class OnboardingStep {
        NOT_STARTED, INTRO_SEEN, PERMISSIONS_ASKED, PROFILE_CREATED, COMPLETED
    }

    var currentStep: OnboardingStep
        get() = prefs.getString("step", null)
            ?.let { runCatching { OnboardingStep.valueOf(it) }.getOrNull() }
            ?: OnboardingStep.NOT_STARTED
        set(value) = prefs.edit { putString("step", value.name) }

    val isCompleted: Boolean
        get() = currentStep == OnboardingStep.COMPLETED

    fun advanceToNextStep() {
        val next = when (currentStep) {
            OnboardingStep.NOT_STARTED       -> OnboardingStep.INTRO_SEEN
            OnboardingStep.INTRO_SEEN        -> OnboardingStep.PERMISSIONS_ASKED
            OnboardingStep.PERMISSIONS_ASKED -> OnboardingStep.PROFILE_CREATED
            OnboardingStep.PROFILE_CREATED   -> OnboardingStep.COMPLETED
            OnboardingStep.COMPLETED         -> OnboardingStep.COMPLETED
        }
        currentStep = next
    }
}
```

### Example 5: Selected Tab / Last Screen State

```kotlin
class NavigationState(context: Context) {

    private val prefs = context.getSharedPreferences("nav_state", Context.MODE_PRIVATE)

    var lastSelectedTab: Int
        get() = prefs.getInt("last_tab", 0)
        set(value) = prefs.edit { putInt("last_tab", value) }

    var lastScrollPosition: Int
        get() = prefs.getInt("scroll_pos", 0)
        set(value) = prefs.edit { putInt("scroll_pos", value) }

    fun clearNavigationState() = prefs.edit { clear() }
}
```

### Example 6: Filter and Sort Preferences

```kotlin
class FilterPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("filters", Context.MODE_PRIVATE)

    enum class SortOrder { DATE_DESC, DATE_ASC, PROFIT_DESC, PROFIT_ASC }

    var sortOrder: SortOrder
        get() = prefs.getString("sort_order", null)
            ?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() }
            ?: SortOrder.DATE_DESC
        set(value) = prefs.edit { putString("sort_order", value.name) }

    var showProfitablOnly: Boolean
        get() = prefs.getBoolean("show_profitable_only", false)
        set(value) = prefs.edit { putBoolean("show_profitable_only", value) }

    var selectedStrategies: Set<String>
        get() = prefs.getStringSet("selected_strategies", emptySet()) ?: emptySet()
        set(value) = prefs.edit { putStringSet("selected_strategies", value) }

    fun addStrategy(strategy: String) {
        val current = selectedStrategies.toMutableSet()
        current.add(strategy)
        selectedStrategies = current
    }

    fun removeStrategy(strategy: String) {
        val current = selectedStrategies.toMutableSet()
        current.remove(strategy)
        selectedStrategies = current
    }

    fun resetFilters() = prefs.edit { clear() }
}
```

### Example 7: Feature Flags (Local)

```kotlin
class LocalFeatureFlags(context: Context) {

    private val prefs = context.getSharedPreferences("feature_flags", Context.MODE_PRIVATE)

    // Default to false — features off until explicitly enabled
    val isNewDashboardEnabled: Boolean
        get() = prefs.getBoolean("new_dashboard", false)

    val isBetaModeEnabled: Boolean
        get() = prefs.getBoolean("beta_mode", false)

    val maxTradesPerDay: Int
        get() = prefs.getInt("max_trades_per_day", 10)

    // Update from remote config or admin screen
    fun updateFlags(newDashboard: Boolean, betaMode: Boolean, maxTrades: Int) {
        prefs.edit {
            putBoolean("new_dashboard", newDashboard)
            putBoolean("beta_mode", betaMode)
            putInt("max_trades_per_day", maxTrades)
        }
    }
}
```

### Example 8: StrictMode-Compatible Background Read

```kotlin
// For legacy code that must keep SharedPreferences but wants to avoid StrictMode violations
class SafePrefsReader(
    private val prefs: SharedPreferences,
    private val scope: CoroutineScope
) {
    // Read on IO thread, return via callback
    fun getBooleanAsync(key: String, default: Boolean, callback: (Boolean) -> Unit) {
        scope.launch(Dispatchers.IO) {
            val value = prefs.getBoolean(key, default)
            withContext(Dispatchers.Main) {
                callback(value)
            }
        }
    }

    // Suspend version for coroutine callers
    suspend fun getBooleanSuspend(key: String, default: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            prefs.getBoolean(key, default)
        }
}
```

### Example 9: Notification Settings Per Channel

```kotlin
class NotificationSettings(context: Context) {

    private val prefs = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)

    // Separate key per notification type
    fun isChannelEnabled(channelId: String): Boolean =
        prefs.getBoolean("notif_$channelId", true)

    fun setChannelEnabled(channelId: String, enabled: Boolean) =
        prefs.edit { putBoolean("notif_$channelId", enabled) }

    fun getEnabledChannels(allChannelIds: List<String>): List<String> =
        allChannelIds.filter { isChannelEnabled(it) }

    fun enableAll(allChannelIds: List<String>) = prefs.edit {
        allChannelIds.forEach { putBoolean("notif_$it", true) }
    }

    fun disableAll(allChannelIds: List<String>) = prefs.edit {
        allChannelIds.forEach { putBoolean("notif_$it", false) }
    }
}
```

### Example 10: A/B Testing Variant Assignment

```kotlin
class AbTestingPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("ab_testing", Context.MODE_PRIVATE)

    // Assign user to a variant once and persist it
    fun getVariant(testName: String, variants: List<String>): String {
        val stored = prefs.getString(testName, null)
        if (stored != null && stored in variants) return stored

        // Assign randomly and persist
        val assigned = variants.random()
        prefs.edit { putString(testName, assigned) }
        return assigned
    }

    fun clearVariants() = prefs.edit { clear() }
}

// Usage:
val variant = abPrefs.getVariant("home_cta_test", listOf("control", "variant_a", "variant_b"))
// User always gets the same variant on subsequent app opens
```

---

## Chapter 26: SharedPreferences vs DataStore — When to Use What

### Modern Recommendation

```
New Code:
    Always → DataStore (Preferences DataStore for simple key-value)

Legacy Code:
    Already using SharedPreferences → OK to keep for now
    Adding new preferences → Add to DataStore, not SharedPreferences
    Performance problems / ANRs → Migrate to DataStore

Third-party SDK uses SharedPreferences:
    → You cannot change it, just be aware of the implications
```

### Side-by-Side Comparison

|Aspect|SharedPreferences|DataStore (Preferences)|
|---|---|---|
|API style|Synchronous|Asynchronous (Flow + suspend)|
|Main thread safety|❌ Blocks on load + fsync|✅ Never blocks|
|Write confirmation|`commit()` only|Always (suspend returns after write)|
|Error handling|❌ Silent apply()|✅ Exposed via Flow|
|Type safety|❌ Runtime ClassCastException|✅ Typed keys, compile-time|
|Atomic writes|Partial (backup mechanism)|✅ Full (temp file + atomic rename)|
|Reactive updates|Listener (main thread only)|✅ Flow (any thread)|
|Compose integration|Listener → manual state|✅ collectAsStateWithLifecycle()|
|File format|XML (human readable)|Binary protobuf|
|File size|Larger|~7x smaller|
|Setup complexity|Very simple|Moderate|
|Android version|Since API 1|Requires modern dependencies|

### Use SharedPreferences If:

- You're in a legacy codebase with extensive SharedPreferences usage and no time to migrate
- You need to support an old Android API level where DataStore might have dependency issues
- The data is truly trivial and you need zero setup (one flag in a quick prototype)

### Use DataStore If:

- Starting a new project
- Building in Jetpack Compose (Flow integrates naturally)
- You need guaranteed write confirmation
- You've had ANR issues related to SharedPreferences
- You want proper error handling
- You need reactive UI updates

---

## Chapter 27: Migrating From SharedPreferences to DataStore

DataStore has built-in migration support. It handles the migration automatically on first access.

### Automatic Migration

```kotlin
// Your new DataStore with migration from old SharedPreferences
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_prefs",
    migrations = listOf(
        SharedPreferencesMigration(
            context = this,
            sharedPreferencesName = "com.yourapp.settings"  // your old SP file name
        )
    )
)
```

### What the Migration Does

1. First time `dataStore.data` is accessed:
    - Reads all data from the old SharedPreferences XML file
    - Converts each key-value pair to DataStore format
    - Writes to the new `.preferences_pb` file
    - Deletes the old SharedPreferences XML file
2. On all subsequent accesses: migration is skipped (file doesn't exist anymore)

### Migrating With Key Remapping

If you want to rename keys during migration:

```kotlin
SharedPreferencesMigration(
    context = context,
    sharedPreferencesName = "old_settings",
    migrate = { sharedPrefsView, mutablePreferences ->
        // Map old keys to new keys
        if (DARK_MODE_KEY !in mutablePreferences) {
            val oldValue = sharedPrefsView.getBoolean("isDarkMode", false)  // old name
            mutablePreferences[DARK_MODE_KEY] = oldValue                    // new name
        }
        mutablePreferences
    }
)
```

### Gradual Migration Strategy

For large codebases, migrate file by file:

```kotlin
// Step 1: Create DataStore with migration for the most critical file
val Context.userPrefsStore by preferencesDataStore(
    name = "user_prefs",
    migrations = listOf(SharedPreferencesMigration(this, "user_settings"))
)

// Step 2: Update all code that wrote to "user_settings" to use userPrefsStore

// Step 3: Next file...
val Context.appConfigStore by preferencesDataStore(
    name = "app_config",
    migrations = listOf(SharedPreferencesMigration(this, "app_settings"))
)
```

---

## Chapter 28: Quick Reference Cheat Sheet

### Get Instance

```kotlin
// App-wide (use this most often)
context.getSharedPreferences("name", Context.MODE_PRIVATE)

// Activity-specific
activity.getPreferences(Context.MODE_PRIVATE)

// Default settings file
PreferenceManager.getDefaultSharedPreferences(context)
```

### Read

```kotlin
prefs.getBoolean("key", false)
prefs.getInt("key", 0)
prefs.getLong("key", 0L)
prefs.getFloat("key", 0f)
prefs.getString("key", null)
prefs.getStringSet("key", emptySet())
prefs.contains("key")
prefs.all  // Map<String, *> of all entries
```

### Write (with KTX extension)

```kotlin
prefs.edit {
    putBoolean("key", true)
    putInt("key", 42)
    putLong("key", 1000L)
    putFloat("key", 1.5f)
    putString("key", "value")
    putStringSet("key", setOf("a", "b"))
}
```

### Write with commit() (synchronous)

```kotlin
prefs.edit(commit = true) {
    putString("key", "value")
}
```

### Delete

```kotlin
prefs.edit { remove("key") }
prefs.edit { clear() }
```

### Change Listener

```kotlin
private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
    // called on main thread
}
prefs.registerOnSharedPreferenceChangeListener(listener)   // in onResume/onStart
prefs.unregisterOnSharedPreferenceChangeListener(listener) // in onPause/onStop
```

### Encrypted SharedPreferences

```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context, "secure_prefs", masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

### In Koin

```kotlin
single<SharedPreferences> {
    androidContext().getSharedPreferences("prefs", Context.MODE_PRIVATE)
}
```

---

## Final Summary

```
SharedPreferences
├── Introduced: Android 1.0, 2008
├── Storage: XML file at /shared_prefs/name.xml
├── In-memory: HashMap<String, Object>
├── Read path: HashMap.get() — instant after load
├── Write path:
│   ├── commit() — synchronous, returns Boolean, blocks caller thread
│   └── apply()  — "async" but blocks main thread via QueuedWork on lifecycle events
├── Supports: Boolean, Int, Long, Float, String, Set<String>
├── Dangerous because:
│   ├── apply() → QueuedWork → ANR on main thread during lifecycle
│   ├── No write confirmation from apply()
│   ├── No type safety (runtime ClassCastException)
│   └── Plain text (readable on rooted devices)
├── Use when: legacy code, quick prototypes, truly trivial data
├── Don't use when: new code, Compose, need reactive updates, sensitive data
└── Migrate to: DataStore (official Google recommendation since 2021)

The 5 Rules:
1. Always MODE_PRIVATE — never other modes
2. Never commit() on main thread — use apply() or commit() on IO thread
3. Always store listener as member variable — WeakReference trap
4. Never store sensitive data — use EncryptedSharedPreferences
5. Never modify a returned Set<String> in place — create a new set
```

---

_This guide covers SharedPreferences as of Android API 34. The API itself has not changed significantly since API 1 — the problems described have existed since 2008 and persist today._