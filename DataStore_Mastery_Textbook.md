# 📦 Jetpack DataStore — Complete Mastery Textbook
### *From History to System Internals to Production Patterns*
> Written for Android developers using Kotlin + Jetpack Compose + Clean Architecture + Koin.  
> Version: DataStore 1.1.1 | Kotlin 2.x | Compose BOM 2024+

---

## Table of Contents

**Part I — Foundation & History**
1. [The History of Android Data Storage](#chapter-1-the-history-of-android-data-storage)
2. [Why SharedPreferences Was Broken](#chapter-2-why-sharedpreferences-was-broken)
3. [Birth of DataStore](#chapter-3-birth-of-datastore)
4. [What DataStore Actually Is](#chapter-4-what-datastore-actually-is)

**Part II — System Level Internals**

5. [How DataStore Works at the OS Level](#chapter-5-how-datastore-works-at-the-os-level)
6. [The Flow Architecture Inside DataStore](#chapter-6-the-flow-architecture-inside-datastore)
7. [Atomic Writes — How DataStore Guarantees Safety](#chapter-7-atomic-writes--how-datastore-guarantees-safety)
8. [The File on Disk — Protocol Buffers Explained](#chapter-8-the-file-on-disk--protocol-buffers-explained)

**Part III — Two Types of DataStore**

9. [Preferences DataStore — Full Deep Dive](#chapter-9-preferences-datastore--full-deep-dive)
10. [All Key Types You Can Store](#chapter-10-all-key-types-you-can-store)
11. [Reading Data — Every Method Explained](#chapter-11-reading-data--every-method-explained)
12. [Writing Data — Every Method Explained](#chapter-12-writing-data--every-method-explained)
13. [Deleting Data](#chapter-13-deleting-data)
14. [Error Handling & Corruption Recovery](#chapter-14-error-handling--corruption-recovery)
15. [Proto DataStore — Complete Deep Dive](#chapter-15-proto-datastore--complete-deep-dive)

**Part IV — Architecture & Integration**

16. [DataStore in Clean Architecture](#chapter-16-datastore-in-clean-architecture)
17. [DataStore with ViewModel + Compose](#chapter-17-datastore-with-viewmodel--compose)
18. [DataStore with Koin Dependency Injection](#chapter-18-datastore-with-koin-dependency-injection)
19. [Migrating from SharedPreferences](#chapter-19-migrating-from-sharedpreferences)
20. [DataStore vs Room — The Complete Decision Guide](#chapter-20-datastore-vs-room--the-complete-decision-guide)

**Part V — Real-World Mastery**

21. [Common Mistakes & How to Avoid Them](#chapter-21-common-mistakes--how-to-avoid-them)
22. [Real-World Patterns & Examples](#chapter-22-real-world-patterns--examples)
23. [Advanced Patterns](#chapter-23-advanced-patterns)
24. [Quick Reference Cheat Sheet](#chapter-24-quick-reference-cheat-sheet)

---

# PART I — FOUNDATION & HISTORY

---

## Chapter 1: The History of Android Data Storage

To fully understand why DataStore exists, you need to understand what came before it and why those solutions eventually broke down.

### 2008 — Android 1.0: The Beginning

When Android launched, developers had very few options for persisting data:

- **SQLite** — a full relational database, great for structured data, but heavy for simple key-value needs
- **Raw Files** — writing directly to the filesystem with `FileOutputStream`, complex and error-prone
- **SharedPreferences** — a simple XML-based key-value store introduced in Android 1.0

SharedPreferences was a revelation at the time. It was simple:

```kotlin
val prefs = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
prefs.edit().putBoolean("dark_mode", true).apply()
val isDark = prefs.getBoolean("dark_mode", false)
```

Three lines. It worked. Developers loved it.

### 2008–2019 — The Golden Age of SharedPreferences

For over a decade, SharedPreferences was the universal solution for storing app settings, user preferences, flags, tokens, and small data. It powered billions of apps. The Android documentation itself recommended it. Every Android tutorial taught it.

The API was beginner-friendly, the behavior was predictable for simple use cases, and it felt safe — because Google said it was safe to call from the main thread.

But as Android grew more complex, cracks began to appear.

### 2012–2018 — The ANR Problem Emerges

As apps got more complex and devices more varied, developers started seeing a mysterious bug in their crash reports: **ANR (Application Not Responding)** errors, with stack traces pointing at SharedPreferences.

```
ANR in com.yourapp
Reason: Input dispatching timed out
    at android.app.SharedPreferencesImpl.awaitLoadedLocked (SharedPreferencesImpl.java)
    at android.app.SharedPreferencesImpl.getString (SharedPreferencesImpl.java)
```

Bumble (the dating app) published a detailed post-mortem showing SharedPreferences was **the number one cause of ANRs** in their app. They traced it to a single hidden behavior: `apply()` — which looks async — was blocking the main thread during `fsync()` calls.

Companies like Snap, Bumble, and countless others spent months debugging this. The root cause was buried deep in Android's internal source code, invisible to app developers.

### 2019 — Google Acknowledges the Problem

At Google I/O 2019, the Android team acknowledged the fundamental flaws in SharedPreferences and announced they were working on a replacement. The Jetpack team began designing what would become DataStore.

Their goals were clear:
1. Never block the main thread
2. Provide proper async APIs using Kotlin Coroutines
3. Handle errors properly
4. Support typed data
5. Guarantee atomic writes

### 2020 — DataStore Alpha

DataStore was released in alpha in June 2020, introducing a completely new mental model: instead of a synchronous get/set API, DataStore exposes a `Flow` — you subscribe to data, and the data comes to you.

### 2021 — DataStore 1.0 Stable

DataStore reached stable `1.0.0` in August 2021, and Google officially marked it as the recommended replacement for SharedPreferences.

### 2024 — DataStore 1.1 + Kotlin Multiplatform

DataStore 1.1.x added support for **Kotlin Multiplatform (KMP)**, meaning you can now use the same DataStore code on Android, iOS, and desktop. It also added multi-process support and the new encryption extension via the Tink library.

```
Timeline:
2008 ─── SharedPreferences introduced (Android 1.0)
2012 ─── ANR reports start appearing in large apps
2019 ─── Google announces DataStore at I/O
2020 ─── DataStore alpha released
2021 ─── DataStore 1.0 stable
2024 ─── DataStore 1.1 + Kotlin Multiplatform support
```

---

## Chapter 2: Why SharedPreferences Was Broken

Understanding the exact technical failures of SharedPreferences is essential. These aren't opinions — they are documented, reproducible bugs.

### Problem 1: The fsync() Trap (The Hidden Main Thread Block)

This is the most dangerous and least understood problem.

When you call `apply()`, it feels safe because it says "async":

```kotlin
prefs.edit()
    .putBoolean("dark_mode", true)
    .apply()  // "async" — right?
```

Here is what actually happens internally inside Android's `SharedPreferencesImpl.java`:

```
apply() is called
    │
    ▼
Starts background thread to write to disk ← good
    │
    ▼
Background thread calls fsync() ← this is the trap
    │
    ▼
fsync() waits for kernel to confirm disk write ← can take 100ms–500ms
    │
    ▼
MEANWHILE: your Activity/Service starts or stops
    │
    ▼
Android's ActivityThread calls QueuedWork.waitToFinish()
    │
    ▼
THE MAIN THREAD BLOCKS waiting for that fsync() to complete ← ANR!
```

The key insight: `apply()` enqueues a disk write. Android's `ActivityThread` — which runs on the main thread — calls `QueuedWork.waitToFinish()` during **every Activity start/stop and every Service start/stop**. This means even though you used `apply()`, the main thread still ends up waiting for the write to complete.

On older or cheaper devices where disk I/O is slow, this wait can exceed 5 seconds — triggering an ANR.

### Problem 2: The Loading Block

When you first access SharedPreferences, Android loads the entire XML file into memory:

```kotlin
// This line loads the ENTIRE XML file from disk on the calling thread
val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
```

If you call this on the main thread during startup (as most code does), and the file is large or the device is slow, the main thread blocks while reading from disk. Another ANR source.

### Problem 3: No Type Safety

```kotlin
prefs.edit().putBoolean("user_id", true)  // writes Boolean
val id = prefs.getInt("user_id", 0)       // reads Int from Boolean key
// Result: ClassCastException at runtime — not at compile time
```

SharedPreferences uses raw strings as keys and stores everything in an untyped XML file. The compiler cannot catch type mismatches. They only surface as crashes at runtime.

### Problem 4: No Transactions — Data Corruption Possible

```kotlin
// Thread A:
prefs.edit().putString("name", "Alice").apply()

// Thread B (simultaneously):
prefs.edit().putString("name", "Bob").apply()
```

There is no locking, no ordering guarantee. If two threads write at the same time, you can get partial writes or lost updates. SharedPreferences has no transactional semantics.

### Problem 5: Silent Errors

```kotlin
prefs.edit().putString("key", value).apply()
// If this fails — disk full, permission error, anything — 
// you get NO error. apply() returns nothing.
// Your data is silently lost.
```

`apply()` has no error callback, no exception, no way to know if the write succeeded.

### Problem 6: Runtime Parsing Exceptions

SharedPreferences stores data as XML. If the XML file becomes corrupt (power loss during write, filesystem error), the next read throws a runtime exception — with no recovery mechanism.

```
java.lang.ClassCastException: Cannot cast...
    at SharedPreferencesImpl.getStringSet(SharedPreferencesImpl.java)
```

### The Full Comparison

```
SharedPreferences vs DataStore

ISSUE                    SharedPreferences        DataStore
─────────────────────────────────────────────────────────────
Main thread safety       ❌ Blocks on fsync()      ✅ Always async
Error handling           ❌ Silent failures        ✅ Caught IOExceptions
Type safety              ❌ Runtime crashes        ✅ Typed keys
Atomic writes            ❌ Race conditions        ✅ Fully transactional
Corruption recovery      ❌ Crashes with XML       ✅ Corruption handler
Kotlin Flow support      ❌ Listener callbacks     ✅ First-class Flow
Structured concurrency   ❌ None                   ✅ Coroutines-based
```

---

## Chapter 3: Birth of DataStore

### The Design Principles

When the Jetpack team designed DataStore, they started from first principles. They asked: "What would a perfect Android persistence library look like if we designed it today, knowing what we know about Kotlin Coroutines and Flow?"

They landed on four pillars:

**Pillar 1: Async-first**  
Every single operation — read or write — must be non-blocking by design. The API should make it impossible to accidentally block the main thread.

**Pillar 2: Reactive**  
Instead of "fetch the value", you "subscribe to the value". Changes propagate automatically to all observers. This aligns naturally with Compose's reactive UI model.

**Pillar 3: Transactional**  
Every write must be atomic. Either the entire write succeeds, or nothing changes. Partial writes are impossible.

**Pillar 4: Observable errors**  
Errors must be surfaced through the same stream as data — not silently swallowed, not thrown as exceptions in unexpected places.

### Why Coroutines and Flow?

The Jetpack team chose Kotlin Coroutines and Flow because by 2019–2020, they had become the standard for async programming on Android:

- `suspend` functions make async code look synchronous — no callback hell
- `Flow` provides a reactive stream that fits naturally into Compose's state model
- Structured concurrency means coroutines are automatically cancelled when their scope dies — no memory leaks

DataStore is built on top of `Dispatchers.IO` internally — all file operations are dispatched to the IO thread pool automatically.

---

## Chapter 4: What DataStore Actually Is

DataStore is a **persistent, reactive, asynchronous, transactional key-value store** for Android.

Let's break each word down:

- **Persistent** — data survives app restarts, stored on disk
- **Reactive** — you subscribe to data changes, not poll for them
- **Asynchronous** — never blocks the calling thread
- **Transactional** — writes are atomic, all-or-nothing
- **Key-value** — data is stored as named pairs (Preferences DataStore)

### What DataStore Is NOT

- Not a database — use Room for lists, relations, complex queries
- Not for large data — use Room for anything with hundreds of records
- Not for secrets — use Android Keystore or Encrypted DataStore for sensitive data
- Not a cache — use a proper caching layer for network responses

### The Two Implementations

```
Jetpack DataStore
├── Preferences DataStore
│   ├── Stores: key-value pairs
│   ├── Schema: not required
│   ├── Type safety: at key level only
│   ├── File format: .preferences_pb (protobuf internally)
│   └── Best for: flags, settings, simple preferences
│
└── Proto DataStore
    ├── Stores: typed objects
    ├── Schema: required (.proto file)
    ├── Type safety: full compile-time safety
    ├── File format: .pb (pure protobuf)
    └── Best for: complex settings objects, structured data
```

---

# PART II — SYSTEM LEVEL INTERNALS

---

## Chapter 5: How DataStore Works at the OS Level

This chapter goes deep. Understanding what happens at the operating system level will make you a far better developer — you'll understand the "why" behind every API decision.

### The File Location

When you create a DataStore with name `"app_prefs"`, Android creates this file:

```
/data/data/com.yourapp/files/datastore/app_prefs.preferences_pb
```

Breaking this down:
- `/data/data/com.yourapp/` — your app's private sandbox (only your app can access this)
- `files/` — the app's files directory, returned by `context.filesDir`
- `datastore/` — a subdirectory created automatically by DataStore
- `app_prefs.preferences_pb` — your data file. `.preferences_pb` is a Protocol Buffer binary file

Unlike SharedPreferences which stored data in:
```
/data/data/com.yourapp/shared_prefs/my_prefs.xml  ← human-readable XML
```

DataStore uses binary Protocol Buffer format — smaller, faster, and more reliable than XML.

### The Write Sequence at OS Level

When you call `dataStore.edit { }`, here is the full sequence of events from Kotlin all the way down to the kernel:

```
Your Code (Main Thread or any thread)
    │
    ▼
dataStore.edit { prefs -> prefs[KEY] = value }
    │
    ▼ suspend — suspends calling coroutine, does NOT block thread
    │
    ▼
DataStore internal: dispatches to Dispatchers.IO
    │
    ▼ (now on IO thread pool)
    │
    ▼
DataStore reads current Preferences from memory (cached)
    │
    ▼
Applies your mutations from the edit { } lambda
    │
    ▼
Serializes new Preferences object to Protocol Buffer bytes
    │
    ▼
Writes bytes to a TEMP file first:
    app_prefs.preferences_pb.tmp
    │
    ▼
Calls FileOutputStream.write() → Linux write() syscall
    │
    ▼
Linux kernel buffers the write in page cache (fast — in memory)
    │
    ▼
Calls fsync() to flush kernel buffer → physical disk write
    │
    ▼
Once confirmed: atomically renames .tmp file to .preferences_pb
    │
    ▼
The rename is atomic at the OS level — either the new file
exists or the old one does. NEVER partial state.
    │
    ▼
DataStore emits new Preferences value on the data Flow
    │
    ▼
All collectors (ViewModels, Composables) receive the update
    │
    ▼
Your calling coroutine resumes — edit { } returns
```

**Key insight**: The temp file + atomic rename is the mechanism that makes DataStore corruption-proof. If the app crashes at any point during the write, the original file is untouched. The new file only replaces the old one after it is completely written and flushed to disk.

### Why the Old File Stays Safe

```
Before write:  app_prefs.preferences_pb  ← original, intact
During write:  app_prefs.preferences_pb  ← original, intact
               app_prefs.preferences_pb.tmp  ← new data being written

Power loss here? → .tmp is deleted on next read. Original survives. ✅

After write:   app_prefs.preferences_pb  ← new data (rename is atomic)
               (no .tmp file)
```

This is fundamentally different from SharedPreferences which wrote directly to the XML file — if power was lost mid-write, the XML could be partially written and become unparseable.

### The Read Sequence at OS Level

```
Your Code collects from dataStore.data
    │
    ▼
DataStore checks: is data in memory cache?
    │
    ├── YES → emits cached value immediately (no disk read!)
    │
    └── NO → dispatches to Dispatchers.IO
                  │
                  ▼
              Linux read() syscall on .preferences_pb file
                  │
                  ▼
              Kernel copies file from disk to page cache (first time)
              Subsequent reads served from page cache (fast!)
                  │
                  ▼
              DataStore deserializes Protocol Buffer bytes → Preferences object
                  │
                  ▼
              Caches in memory
                  │
                  ▼
              Emits on data Flow
                  │
                  ▼
              Your code receives value
```

After the first read, subsequent reads are served from memory — no disk I/O. DataStore keeps an in-memory cache of the current `Preferences` object and only re-reads from disk when the file changes (which it detects by watching the file modification timestamp).

### Thread Model

```
UI Thread (Main)          IO Thread Pool              Disk
     │                         │                        │
     │  collect(dataStore.data) │                        │
     │─────────────────────────►│                        │
     │                         │  read .pb file          │
     │                         │────────────────────────►│
     │                         │◄────────────────────────│
     │                         │  deserialize            │
     │◄─────────────────────────│  emit Preferences       │
     │  UI updates             │                        │
     │                         │                        │
     │  edit { }               │                        │
     │─────────────────────────►│                        │
     │  (suspends, not blocks) │  write .pb.tmp          │
     │                         │────────────────────────►│
     │                         │◄────────────────────────│
     │                         │  rename .tmp → .pb      │
     │                         │  emit new Preferences   │
     │◄─────────────────────────│                        │
     │  resumes                │                        │
```

The UI thread is **never blocked**. It either suspends (when writing) or receives values via Flow emissions (when reading).

---

## Chapter 6: The Flow Architecture Inside DataStore

DataStore's reactive behavior is built on a `SharedFlow` — a hot flow that multicasts to all active collectors.

### What is a Hot Flow?

There are two types of Flows:

**Cold Flow** — starts from scratch for each collector:
```kotlin
// Every time you collect this, it starts over
val coldFlow = flow {
    emit(readFromDisk())  // called fresh for each collector
}
```

**Hot Flow (SharedFlow/StateFlow)** — runs once, broadcasts to all collectors:
```kotlin
// Runs once, shares results with everyone
val hotFlow = MutableSharedFlow<Int>()
// All collectors receive the same emissions
```

DataStore uses a `SharedFlow` internally. This means:
- The file is read once on first access
- All collectors (multiple ViewModels, multiple screens) share the same data stream
- When a write happens, all collectors are notified simultaneously

### The Data Flow Pipeline

```kotlin
// What you write:
val myFlow: Flow<Boolean> = context.dataStore.data
    .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
    .map { prefs -> prefs[MY_KEY] ?: false }
```

This creates a pipeline of Flow operators. Here's what each layer does:

```
dataStore.data
    ↓  (SharedFlow<Preferences> — emits on every change)

.catch { }
    ↓  (intercepts IOException, emits emptyPreferences() as fallback)
    ↓  (all other exceptions propagate up and cancel the flow)

.map { prefs -> prefs[MY_KEY] ?: false }
    ↓  (transforms Preferences → Boolean)
    ↓  (only emits when the Boolean value actually changes)

Your ViewModel collects Flow<Boolean>
    ↓  (converts to StateFlow with stateIn())

Compose UI observes StateFlow
    ↓  (collectAsStateWithLifecycle())

Recomposition triggered when value changes
```

### Why `.map {}` Doesn't Re-emit on Equal Values

By default, `dataStore.data` emits every time any preference changes. But your `.map {}` extracts only one value. If you change a *different* key, your mapped flow won't re-emit.

```kotlin
// Changing LANGUAGE_KEY won't cause DARK_MODE flow to re-emit
val darkModeFlow = dataStore.data.map { it[DARK_MODE_KEY] ?: false }
```

This is because Flow's `.map {}` uses structural equality — if the output of the map is the same as the previous emission, it doesn't emit again. This is highly efficient for UI — no unnecessary recompositions.

### distinctUntilChanged

DataStore applies `distinctUntilChanged()` on the data flow internally. This means:

```
Preferences changes:  { dark: false } → { dark: false } → { dark: true }

Your darkMode Flow:    false           → (skipped)        → true
```

You never get duplicate emissions. The UI only updates when the actual value you care about changes.

---

## Chapter 7: Atomic Writes — How DataStore Guarantees Safety

"Atomic" means indivisible — the operation either happens completely or not at all. There is no in-between state.

### The Mutex — Serializing Concurrent Writes

DataStore uses a `Mutex` internally to serialize all write operations:

```
Coroutine A: edit { prefs[KEY_1] = "hello" }  ←─ acquires mutex, writes, releases
                                                │
Coroutine B: edit { prefs[KEY_2] = "world" }  ←─ waits for mutex, then writes
```

Even if 10 coroutines call `edit {}` simultaneously, they are queued and executed one at a time. There are no concurrent writes, no race conditions, no data corruption.

### The Edit Block is a Transaction

Everything inside `edit { }` is one transaction:

```kotlin
// This is ONE atomic transaction
dataStore.edit { prefs ->
    prefs[FIRST_NAME_KEY] = "John"    // ─┐
    prefs[LAST_NAME_KEY]  = "Doe"     //  ├── All written together
    prefs[AGE_KEY]        = 30        // ─┘   or not at all
}
```

If your app crashes between the first and second line inside the edit block, none of the three writes happen. The old data remains completely intact.

### ACID Properties

DataStore provides all four ACID guarantees:

| Property | Meaning | DataStore Implementation |
|---|---|---|
| **Atomicity** | All writes succeed or none do | `edit {}` block is one transaction |
| **Consistency** | Data is always valid | Schema enforced by typed keys |
| **Isolation** | Concurrent writes don't interfere | Internal `Mutex` serializes writes |
| **Durability** | Committed writes survive crashes | fsync() + atomic file rename |

This is the same guarantee that production databases like PostgreSQL provide — it's not a coincidence. DataStore was designed with database-level reliability in mind.

---

## Chapter 8: The File on Disk — Protocol Buffers Explained

### What Are Protocol Buffers?

Protocol Buffers (protobuf) is a data serialization format created by Google in 2001 (open-sourced in 2008). It's used internally at Google for almost all internal data storage and network communication.

The core idea: instead of storing data as text (XML, JSON), store it as binary. Binary is:
- **Smaller** — a boolean is 1 byte in protobuf vs 18+ bytes in XML (`<bool>true</bool>`)
- **Faster** to read and write — no text parsing required
- **More reliable** — binary formats have no encoding issues, no escape character bugs

### How Preferences DataStore Uses Protobuf

Even though you use a simple key-value API, Preferences DataStore stores your data in a `.preferences_pb` file using a predefined protobuf schema defined by Google:

```protobuf
// This is the internal schema DataStore uses (simplified)
message PreferenceMap {
    map<string, Value> preferences = 1;
}

message Value {
    oneof value {
        bool    boolean_value   = 1;
        int32   integer_value   = 2;
        int64   long_value      = 3;
        float   float_value     = 4;
        double  double_value    = 5;
        string  string_value    = 6;
        StringSet string_set    = 7;
    }
}
```

Your key-value pairs are stored as entries in this `PreferenceMap`. The file is binary, not human-readable — you can't open it in a text editor and read it directly.

### Comparing File Sizes

For a typical settings file with 10 keys:

```
SharedPreferences XML:   ~800 bytes  (human readable but verbose)
DataStore protobuf:      ~120 bytes  (binary, ~7x smaller)
```

For large preference files, this size difference compounds significantly.

### What Proto DataStore Adds

Proto DataStore uses your own custom protobuf schema. You define the structure:

```protobuf
// your_settings.proto
syntax = "proto3";
message UserSettings {
    bool   dark_mode        = 1;
    string language         = 2;
    int32  font_size        = 3;
    bool   notifications    = 4;
}
```

This gives you full compile-time type safety and a self-documenting data schema.

---

# PART III — TWO TYPES OF DATASTORE

---

## Chapter 9: Preferences DataStore — Full Deep Dive

### Setup

**Step 1: Dependency** (you already have this in your WallStreet project):
```toml
# libs.versions.toml
datastorePreferences = "1.1.1"
androidx-datastore-preferences = { 
    group = "androidx.datastore", 
    name = "datastore-preferences", 
    version.ref = "datastorePreferences" 
}
```

```kotlin
// build.gradle.kts
implementation(libs.androidx.datastore.preferences)
```

**Step 2: Create the DataStore instance**

```kotlin
// TOP LEVEL of a Kotlin file — outside any class
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")
```

**The `by` keyword here is Kotlin property delegation.** Every time you access `context.dataStore`, the delegate ensures you always get the same instance. This is not just a convenience — it's a correctness requirement. Creating multiple `DataStore` instances pointing to the same file **will corrupt your data**.

```kotlin
// How property delegation works internally (simplified):
private val dataStoreMap = mutableMapOf<String, DataStore<Preferences>>()

fun Context.getDataStore(name: String): DataStore<Preferences> {
    return dataStoreMap.getOrPut(name) {
        PreferenceDataStoreFactory.create { filesDir.resolve("datastore/$name.preferences_pb") }
    }
}
```

The `preferencesDataStore` delegate does exactly this — caches the instance by name so you always get the same one.

### The Complete AppPreferences Class

```kotlin
package com.yourapp.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

// ← ONE instance via property delegate
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

class AppPreferences(private val context: Context) {

    // ─── KEY DEFINITIONS ─────────────────────────────────────────────────────
    companion object {
        val ONBOARDING_COMPLETED  = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE            = stringPreferencesKey("theme_mode")
        val LANGUAGE              = stringPreferencesKey("language")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val LAUNCH_COUNT          = intPreferencesKey("launch_count")
        val LAST_SYNC_MS          = longPreferencesKey("last_sync_ms")
        val FONT_SIZE_SCALE       = floatPreferencesKey("font_size_scale")
        val FAVORITE_IDS          = stringSetPreferencesKey("favorite_ids")
    }

    // ─── REACTIVE READS (return Flow — use in ViewModels) ─────────────────────
    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[ONBOARDING_COMPLETED] ?: false }

    val themeMode: Flow<String> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[THEME_MODE] ?: "system" }

    val language: Flow<String> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[LANGUAGE] ?: "en" }

    // ─── ONE-SHOT READS (suspend — use for startup decisions) ──────────────────
    suspend fun getOnboardingCompleted(): Boolean =
        context.dataStore.data.first()[ONBOARDING_COMPLETED] ?: false

    // ─── WRITES (suspend functions) ────────────────────────────────────────────
    suspend fun setOnboardingCompleted(done: Boolean = true) =
        context.dataStore.edit { it[ONBOARDING_COMPLETED] = done }

    suspend fun setThemeMode(mode: String) =
        context.dataStore.edit { it[THEME_MODE] = mode }

    suspend fun setLanguage(lang: String) =
        context.dataStore.edit { it[LANGUAGE] = lang }

    // ─── ATOMIC MULTI-KEY WRITE ───────────────────────────────────────────────
    suspend fun saveAppSettings(theme: String, language: String, notifications: Boolean) =
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE]            = theme
            prefs[LANGUAGE]              = language
            prefs[NOTIFICATIONS_ENABLED] = notifications
        }

    // ─── LOGOUT CLEANUP ───────────────────────────────────────────────────────
    suspend fun clearUserData() = context.dataStore.edit { prefs ->
        val onboardingDone = prefs[ONBOARDING_COMPLETED] // save device-level flag
        prefs.clear()
        if (onboardingDone == true) prefs[ONBOARDING_COMPLETED] = true
    }
}
```

---

## Chapter 10: All Key Types You Can Store

DataStore enforces type safety through **typed key functions**. The key carries both the name and the type — if you use the wrong type, it won't compile.

```kotlin
// Every key type:
val BOOL_KEY    = booleanPreferencesKey("is_dark_mode")     // Boolean
val INT_KEY     = intPreferencesKey("launch_count")          // Int
val LONG_KEY    = longPreferencesKey("last_login_ms")        // Long
val FLOAT_KEY   = floatPreferencesKey("text_size_scale")     // Float
val DOUBLE_KEY  = doublePreferencesKey("gps_latitude")       // Double
val STRING_KEY  = stringPreferencesKey("username")           // String
val SET_KEY     = stringSetPreferencesKey("selected_tags")   // Set<String>
```

### Type Safety Comparison

```kotlin
// SharedPreferences — compiles, crashes at runtime
prefs.edit().putBoolean("user_id", true)
val id: Int = prefs.getInt("user_id", 0)  // ClassCastException at runtime!

// DataStore — won't compile if types don't match
val USER_ID_KEY = intPreferencesKey("user_id")
dataStore.edit { it[USER_ID_KEY] = true }   // ← Compile error: Type mismatch
```

### Storing Non-Primitive Types

DataStore only natively supports the 7 types above. For enums, sealed classes, and data classes, you convert to/from `String` or `Int`.

**Enum storage:**
```kotlin
enum class ThemeMode { LIGHT, DARK, SYSTEM }

val THEME_KEY = stringPreferencesKey("theme")

// Write
suspend fun setTheme(mode: ThemeMode) {
    context.dataStore.edit { it[THEME_KEY] = mode.name }
}

// Read
val themeFlow: Flow<ThemeMode> = context.dataStore.data
    .map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[THEME_KEY] ?: "SYSTEM") }
            .getOrDefault(ThemeMode.SYSTEM)
    }
```

**Sealed class storage via ordinal:**
```kotlin
sealed class SortOrder {
    object DateAsc  : SortOrder()
    object DateDesc : SortOrder()
    object NameAsc  : SortOrder()
}

val SORT_KEY = intPreferencesKey("sort_order")

suspend fun setSortOrder(order: SortOrder) {
    val ordinal = when (order) {
        is SortOrder.DateAsc  -> 0
        is SortOrder.DateDesc -> 1
        is SortOrder.NameAsc  -> 2
    }
    context.dataStore.edit { it[SORT_KEY] = ordinal }
}

val sortOrderFlow: Flow<SortOrder> = context.dataStore.data
    .map { prefs ->
        when (prefs[SORT_KEY] ?: 0) {
            0    -> SortOrder.DateAsc
            1    -> SortOrder.DateDesc
            2    -> SortOrder.NameAsc
            else -> SortOrder.DateAsc
        }
    }
```

---

## Chapter 11: Reading Data — Every Method Explained

### Method 1: Reactive Read (`.data.map { }`)

This is the primary method. Use it whenever you want the UI to stay in sync with the stored value.

```kotlin
val isDarkModeFlow: Flow<Boolean> = context.dataStore.data
    .catch { e ->
        if (e is IOException) emit(emptyPreferences())
        else throw e
    }
    .map { preferences ->
        preferences[DARK_MODE_KEY] ?: false   // ?: provides default value
    }
```

- Returns `Flow<Boolean>` — never finishes, keeps emitting as value changes
- Every subscriber gets the current value immediately upon subscription
- All future changes are pushed automatically — no polling needed

### Method 2: One-Shot Read (`.first()`)

Use when you need a value exactly once and don't want to observe future changes.

```kotlin
suspend fun checkIfOnboarded(): Boolean {
    return context.dataStore.data.first()[ONBOARDING_KEY] ?: false
}
```

- `.first()` collects the flow and cancels after the first emission
- Still `suspend` — still runs on IO thread, never blocks main thread
- Use in `MainActivity.onCreate()`, startup logic, one-time checks

### Method 3: Read Multiple Keys at Once

Instead of creating separate flows for each key, combine them into one data class:

```kotlin
data class AppSettings(
    val isDarkMode: Boolean,
    val language: String,
    val fontSize: Float,
    val notificationsOn: Boolean
)

val appSettingsFlow: Flow<AppSettings> = context.dataStore.data
    .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
    .map { prefs ->
        AppSettings(
            isDarkMode      = prefs[DARK_MODE_KEY]     ?: false,
            language        = prefs[LANGUAGE_KEY]      ?: "en",
            fontSize        = prefs[FONT_SIZE_KEY]     ?: 14f,
            notificationsOn = prefs[NOTIF_KEY]         ?: true
        )
    }
```

This is more efficient — one flow, one emission per change, one collection in the ViewModel.

### Method 4: Read with Timeout (Advanced)

For startup scenarios where you need a value but can't wait forever:

```kotlin
suspend fun getSettingWithTimeout(): Boolean = withTimeoutOrNull(3000L) {
    context.dataStore.data.first()[MY_KEY] ?: false
} ?: false  // default if timeout
```

---

## Chapter 12: Writing Data — Every Method Explained

### Method 1: Basic Write

```kotlin
suspend fun setDarkMode(enabled: Boolean) {
    context.dataStore.edit { preferences ->
        preferences[DARK_MODE_KEY] = enabled
    }
}
```

### Method 2: Conditional Write (Read-then-Write in one transaction)

```kotlin
// Read and write in one atomic transaction
suspend fun incrementLaunchCount() {
    context.dataStore.edit { preferences ->
        val current = preferences[LAUNCH_COUNT_KEY] ?: 0
        preferences[LAUNCH_COUNT_KEY] = current + 1
    }
}
```

The `edit {}` block receives the **current** `MutablePreferences`. You can read from it inside the block and your read is guaranteed to be consistent with your write.

### Method 3: Multi-Key Atomic Write

```kotlin
// All three written together — atomic
suspend fun saveUserProfile(name: String, email: String, language: String) {
    context.dataStore.edit { prefs ->
        prefs[NAME_KEY]     = name
        prefs[EMAIL_KEY]    = email
        prefs[LANGUAGE_KEY] = language
    }
}
```

If your app crashes after the first line but before the third, none of the three writes happen. The previous values remain unchanged.

### Method 4: Conditional Write (only write if changed)

```kotlin
suspend fun setLanguageIfDifferent(newLanguage: String) {
    context.dataStore.edit { prefs ->
        val current = prefs[LANGUAGE_KEY]
        if (current != newLanguage) {
            prefs[LANGUAGE_KEY] = newLanguage
        }
    }
}
```

This avoids unnecessary disk writes when the value hasn't changed.

### Method 5: Toggle

```kotlin
suspend fun toggleDarkMode() {
    context.dataStore.edit { prefs ->
        val current = prefs[DARK_MODE_KEY] ?: false
        prefs[DARK_MODE_KEY] = !current
    }
}
```

---

## Chapter 13: Deleting Data

### Remove a Single Key

```kotlin
suspend fun clearTheme() {
    context.dataStore.edit { preferences ->
        preferences.remove(THEME_KEY)
    }
}
```

After removal, reading this key returns `null` (or your default value via `?:`).

### Clear All Data

```kotlin
suspend fun clearAll() {
    context.dataStore.edit { preferences ->
        preferences.clear()
    }
}
```

### Selective Clear (Logout Pattern)

```kotlin
// On logout: clear user data but keep device-level settings
suspend fun clearOnLogout() {
    context.dataStore.edit { prefs ->
        // Save device-level flags before clearing
        val onboardingDone = prefs[ONBOARDING_KEY]
        val deviceTheme    = prefs[THEME_KEY]

        prefs.clear()

        // Restore device-level flags
        if (onboardingDone == true) prefs[ONBOARDING_KEY] = true
        if (deviceTheme != null)    prefs[THEME_KEY]      = deviceTheme
    }
}
```

---

## Chapter 14: Error Handling & Corruption Recovery

### Types of Errors

**IOException** — file system errors: disk full, permissions, hardware failure. These are recoverable — you can provide a fallback value.

**Other exceptions** — programming errors, serialization bugs. These should crash — they indicate a code bug, not a runtime environment issue.

### Handling Errors in Flows

```kotlin
val safeFlow: Flow<Boolean> = context.dataStore.data
    .catch { exception ->
        when (exception) {
            is IOException -> {
                // File couldn't be read — emit empty prefs as fallback
                emit(emptyPreferences())
            }
            else -> {
                // Something else went wrong — let it crash
                throw exception
            }
        }
    }
    .map { preferences ->
        preferences[MY_KEY] ?: false
    }
```

### Corruption Handler

If the `.preferences_pb` file itself becomes corrupted (extremely rare, but possible on hardware failure or forced process kill during fsync):

```kotlin
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { producedData ->
        // producedData is emptyPreferences() by default
        // You can provide initial values here:
        emptyPreferences()
    }
)
```

When DataStore detects corruption:
1. It calls your `corruptionHandler`
2. The handler returns the initial data to use
3. DataStore writes this as the new file, discarding the corrupted data
4. The app continues normally — no crash

Without a corruption handler, a corrupt file throws `CorruptionException` and the DataStore is unusable until the file is deleted.

---

## Chapter 15: Proto DataStore — Complete Deep Dive

### When to Use Proto DataStore

Use Proto DataStore when:
- Your preferences form a natural object (multiple related fields)
- You need compile-time type safety at the schema level (not just key level)
- Your data structure might evolve over time with new fields
- You want the cleanest API for complex settings

### Step 1: Add Dependencies

```kotlin
// build.gradle.kts (app)
plugins {
    id("com.google.protobuf") version "0.9.4"
}

dependencies {
    implementation("androidx.datastore:datastore:1.1.1")
    implementation("com.google.protobuf:protobuf-javalite:3.25.3")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.3"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") { option("lite") }
            }
        }
    }
}
```

### Step 2: Define the Schema

Create a file at `src/main/proto/user_settings.proto`:

```protobuf
syntax = "proto3";

option java_package = "com.yourapp.core.preferences";
option java_multiple_files = true;

message UserSettings {
    bool   dark_mode         = 1;  // field number — NEVER change these
    string language          = 2;
    int32  font_size         = 3;
    bool   notifications_on  = 4;
    string theme_color       = 5;
}
```

**Field numbers (= 1, = 2, etc.) are critical.** They identify fields in the binary format. Once you define them and have data on disk, **never change a field number**. You can add new fields with new numbers, but changing existing ones corrupts old data.

### Step 3: Create a Serializer

```kotlin
object UserSettingsSerializer : Serializer<UserSettings> {

    override val defaultValue: UserSettings = UserSettings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): UserSettings {
        return try {
            UserSettings.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read UserSettings proto", e)
        }
    }

    override suspend fun writeTo(t: UserSettings, output: OutputStream) {
        t.writeTo(output)
    }
}
```

### Step 4: Create the DataStore Instance

```kotlin
val Context.userSettingsStore: DataStore<UserSettings> by dataStore(
    fileName = "user_settings.pb",
    serializer = UserSettingsSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler {
        UserSettings.getDefaultInstance()
    }
)
```

### Step 5: Read and Write

```kotlin
// Read
val settingsFlow: Flow<UserSettings> = context.userSettingsStore.data

// Read one field
val darkModeFlow: Flow<Boolean> = context.userSettingsStore.data
    .map { it.darkMode }

// Write — use updateData (the Proto equivalent of edit)
suspend fun setDarkMode(enabled: Boolean) {
    context.userSettingsStore.updateData { current ->
        current.toBuilder()
            .setDarkMode(enabled)
            .build()
    }
}

// Write multiple fields
suspend fun saveSettings(isDark: Boolean, language: String, fontSize: Int) {
    context.userSettingsStore.updateData { current ->
        current.toBuilder()
            .setDarkMode(isDark)
            .setLanguage(language)
            .setFontSize(fontSize)
            .build()
    }
}
```

### Preferences DataStore vs Proto DataStore — Side by Side

```kotlin
// PREFERENCES DATASTORE
val DARK_MODE = booleanPreferencesKey("dark_mode")
val LANGUAGE  = stringPreferencesKey("language")
val FONT_SIZE = intPreferencesKey("font_size")

// Read
dataStore.data.map { it[DARK_MODE] ?: false }
dataStore.data.map { it[LANGUAGE]  ?: "en"  }

// Write
dataStore.edit {
    it[DARK_MODE] = true
    it[LANGUAGE]  = "en"
}

// ─────────────────────────────────────────────

// PROTO DATASTORE
// Read
userSettingsStore.data.map { it.darkMode  }
userSettingsStore.data.map { it.language  }

// Write
userSettingsStore.updateData {
    it.toBuilder()
        .setDarkMode(true)
        .setLanguage("en")
        .build()
}
```

Proto DataStore is slightly more verbose to set up but gives you a cleaner reading API and full compile-time type safety across the entire schema.

---

# PART IV — ARCHITECTURE & INTEGRATION

---

## Chapter 16: DataStore in Clean Architecture

### Layer Responsibilities

```
PRESENTATION LAYER
    ViewModel — collects StateFlow, calls use cases
         ↕
DOMAIN LAYER
    Use Cases — business logic
    Repository Interface — contract
         ↕
DATA LAYER
    Repository Implementation — connects to DataStore
    AppPreferences — DataStore wrapper
```

### The Repository Interface (Domain Layer)

```kotlin
// domain/repository/PreferencesRepository.kt
interface PreferencesRepository {
    // Reactive reads — return Flow
    val isDarkMode: Flow<Boolean>
    val language: Flow<String>
    val isOnboardingCompleted: Flow<Boolean>
    val appSettings: Flow<AppSettings>

    // Suspend writes
    suspend fun setDarkMode(enabled: Boolean)
    suspend fun setLanguage(lang: String)
    suspend fun setOnboardingCompleted()
    suspend fun clearUserData()

    // One-shot reads for startup
    suspend fun getOnboardingCompleted(): Boolean
}
```

### The Repository Implementation (Data Layer)

```kotlin
// data/repository/PreferencesRepositoryImpl.kt
class PreferencesRepositoryImpl(
    private val appPreferences: AppPreferences
) : PreferencesRepository {

    override val isDarkMode          = appPreferences.isDarkMode
    override val language            = appPreferences.language
    override val isOnboardingCompleted = appPreferences.isOnboardingCompleted
    override val appSettings         = appPreferences.appSettings

    override suspend fun setDarkMode(enabled: Boolean)   = appPreferences.setDarkMode(enabled)
    override suspend fun setLanguage(lang: String)        = appPreferences.setLanguage(lang)
    override suspend fun setOnboardingCompleted()         = appPreferences.setOnboardingCompleted()
    override suspend fun clearUserData()                  = appPreferences.clearUserData()

    override suspend fun getOnboardingCompleted(): Boolean = appPreferences.getOnboardingCompleted()
}
```

### Use Cases (Domain Layer)

```kotlin
// One use case per operation — keeps each class focused

class IsOnboardingCompletedUseCase(private val repo: PreferencesRepository) {
    suspend operator fun invoke(): Boolean = repo.getOnboardingCompleted()
}

class SetOnboardingCompletedUseCase(private val repo: PreferencesRepository) {
    suspend operator fun invoke() = repo.setOnboardingCompleted()
}

class GetDarkModeUseCase(private val repo: PreferencesRepository) {
    operator fun invoke(): Flow<Boolean> = repo.isDarkMode
}

class SetDarkModeUseCase(private val repo: PreferencesRepository) {
    suspend operator fun invoke(enabled: Boolean) = repo.setDarkMode(enabled)
}

class GetAppSettingsUseCase(private val repo: PreferencesRepository) {
    operator fun invoke(): Flow<AppSettings> = repo.appSettings
}
```

---

## Chapter 17: DataStore with ViewModel + Compose

### The ViewModel

```kotlin
data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val language: String = "en",
    val fontSizeScale: Float = 1.0f,
    val isLoading: Boolean = false,
    val error: String? = null
)

class SettingsViewModel(
    private val getDarkMode: GetDarkModeUseCase,
    private val setDarkMode: SetDarkModeUseCase,
    private val getSettings: GetAppSettingsUseCase
) : ViewModel() {

    // stateIn converts Flow → StateFlow for Compose
    val uiState: StateFlow<SettingsUiState> = getSettings()
        .map { settings ->
            SettingsUiState(
                isDarkMode    = settings.isDarkMode,
                language      = settings.language,
                fontSizeScale = settings.fontSizeScale
            )
        }
        .catch { e ->
            emit(SettingsUiState(error = e.message))
        }
        .stateIn(
            scope   = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),  // 5s grace period
            initialValue = SettingsUiState(isLoading = true)
        )

    fun onDarkModeToggle(enabled: Boolean) = viewModelScope.launch {
        setDarkMode(enabled)
    }
}
```

### Understanding `stateIn` Parameters

```kotlin
.stateIn(
    scope   = viewModelScope,          // tied to ViewModel lifecycle
    started = SharingStarted.WhileSubscribed(5_000),
    //         └── starts collecting when first subscriber appears
    //             stops collecting 5s after last subscriber leaves
    //             5s grace = survives screen rotation without restarting
    initialValue = SettingsUiState(isLoading = true)
    //             └── what to emit before first value arrives from disk
)
```

| `SharingStarted` Value | Starts | Stops | Best For |
|---|---|---|---|
| `Eagerly` | Immediately | Never | Critical data needed before any UI |
| `Lazily` | First subscriber | Never | Data that's always needed but not immediately |
| `WhileSubscribed(5000)` | First subscriber | 5s after last leaves | **Recommended for most cases** |

### The Composable

```kotlin
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null -> {
            Text("Error: ${uiState.error}")
        }
        else -> {
            SettingsContent(
                uiState  = uiState,
                onToggle = viewModel::onDarkModeToggle
            )
        }
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onToggle: (Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Dark Mode")
            Switch(
                checked  = uiState.isDarkMode,
                onCheckedChange = onToggle
            )
        }
    }
}
```

### Why `collectAsStateWithLifecycle()` Instead of `collectAsState()`

```kotlin
// collectAsState() — always collecting, even when app is in background
val state by viewModel.uiState.collectAsState()

// collectAsStateWithLifecycle() — stops collecting when screen is not visible
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

`collectAsStateWithLifecycle()` is lifecycle-aware — it pauses collection when the Composable is not visible (app backgrounded, screen off) and resumes when visible again. This saves battery and prevents unnecessary work.

---

## Chapter 18: DataStore with Koin Dependency Injection

### The Module

```kotlin
// di/PreferencesModule.kt
val preferencesModule = module {

    // DataStore wrapper — MUST be single, never factory
    single {
        AppPreferences(androidContext())
    }

    // Repository — single because it holds a reference to AppPreferences
    single<PreferencesRepository> {
        PreferencesRepositoryImpl(get())
    }

    // Use Cases — factory is fine, they're stateless
    factory { IsOnboardingCompletedUseCase(get()) }
    factory { SetOnboardingCompletedUseCase(get()) }
    factory { GetDarkModeUseCase(get()) }
    factory { SetDarkModeUseCase(get()) }
    factory { GetAppSettingsUseCase(get()) }

    // ViewModels
    viewModel {
        SettingsViewModel(
            getDarkMode  = get(),
            setDarkMode  = get(),
            getSettings  = get()
        )
    }
}
```

### Why `single` vs `factory` Matters

```
single { AppPreferences(androidContext()) }
    │
    ▼ Koin creates ONE instance and reuses it everywhere
    │
    ▼ Every class that injects AppPreferences gets THE SAME object
    │
    ▼ ONE DataStore instance → correct ✅

factory { AppPreferences(androidContext()) }
    │
    ▼ Koin creates a NEW instance every time it's injected
    │
    ▼ ViewModel A gets AppPreferences instance #1
    ▼ ViewModel B gets AppPreferences instance #2
    │
    ▼ TWO DataStore instances pointing at the same file → CORRUPTION ❌
```

### Register in Application Class

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MyApp)
            modules(
                preferencesModule,
                authModule,
                repositoryModule
                // other modules...
            )
        }
    }
}
```

---

## Chapter 19: Migrating from SharedPreferences

### Automatic Migration (Recommended)

DataStore has built-in migration support. It will:
1. On first access, read all data from the old SharedPreferences file
2. Copy it to the new DataStore file
3. Delete the old SharedPreferences file

```kotlin
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_prefs",
    migrations = listOf(
        SharedPreferencesMigration(
            context = this,
            sharedPreferencesName = "com.yourapp_preferences"  // the old SP file name
        )
    )
)
```

### Key Mapping Migration

If you renamed keys between SharedPreferences and DataStore:

```kotlin
SharedPreferencesMigration(
    context = context,
    sharedPreferencesName = "old_prefs",
    migrate = { sharedPrefs, currentData ->
        // Manually map old keys to new keys
        val builder = currentData.toMutablePreferences()

        if (DARK_MODE_KEY !in currentData) {
            val oldValue = sharedPrefs.getBoolean("isDark", false)  // old key name
            builder[DARK_MODE_KEY] = oldValue                       // new key name
        }

        builder.toPreferences()
    }
)
```

### Migrating Only Specific Keys

```kotlin
SharedPreferencesMigration(
    context = context,
    sharedPreferencesName = "old_prefs",
    keysToMigrate = setOf("dark_mode", "language")  // only these, ignore others
)
```

---

## Chapter 20: DataStore vs Room — The Complete Decision Guide

### Decision Tree

```
Need to persist data locally?
         │
         ▼
Is it a single value or small set of values
with no relationships between them?
    YES → DataStore
    NO  ↓

Is it a list, table, or collection?
    YES → Room
    NO  ↓

Does it involve joins, foreign keys, or relationships?
    YES → Room
    NO  ↓

Will you query it with filters, sorts, or conditions?
    YES → Room
    NO  → DataStore is probably fine
```

### Feature Comparison

| Feature | DataStore | Room |
|---|---|---|
| Key-value storage | ✅ | ❌ |
| Typed objects | ✅ (Proto) | ✅ |
| SQL queries | ❌ | ✅ |
| Relations (joins) | ❌ | ✅ |
| Lists / collections | ❌ | ✅ |
| Partial updates | ❌ | ✅ |
| Indexes | ❌ | ✅ |
| Migrations (versioned) | Limited | ✅ Full |
| Flow support | ✅ | ✅ |
| Setup complexity | Low | Medium |
| File size | Very small | Larger |
| Performance for simple reads | Faster | Slightly slower |

### Practical Examples from WallStreet App

| Data | Use |
|---|---|
| Dark mode toggle | DataStore |
| Onboarding completed flag | DataStore |
| Default currency ("USD"/"INR") | DataStore |
| Language preference | DataStore |
| Biometric auth enabled | DataStore |
| List of trades | Room |
| Trading strategies | Room |
| Journal entries | Room |
| Equity metrics history | Room |
| Last selected bottom nav tab | DataStore |
| Notification settings | DataStore |

---

# PART V — REAL-WORLD MASTERY

---

## Chapter 21: Common Mistakes & How to Avoid Them

### Mistake 1: Multiple DataStore Instances (Critical)

```kotlin
// ❌ WRONG — new AppPreferences every time = multiple DataStore instances
class MyViewModel(context: Context) : ViewModel() {
    private val prefs = AppPreferences(context)  // creates new instance every ViewModel!
}

// ❌ ALSO WRONG — factory in Koin
factory { AppPreferences(androidContext()) }  // new instance per injection!

// ✅ CORRECT — singleton via property delegate
val Context.dataStore by preferencesDataStore(name = "prefs")  // one instance always

// ✅ CORRECT — single in Koin
single { AppPreferences(androidContext()) }
```

Multiple instances are **the most dangerous mistake** with DataStore. It leads to silent data corruption that is very hard to debug.

### Mistake 2: Using runBlocking

```kotlin
// ❌ WRONG — blocks the calling thread (ANR if on main thread)
val value = runBlocking {
    context.dataStore.data.first()[MY_KEY] ?: false
}

// ✅ CORRECT — suspend properly
suspend fun getValue(): Boolean {
    return context.dataStore.data.first()[MY_KEY] ?: false
}

// ✅ CORRECT — launch coroutine
viewModelScope.launch {
    val value = context.dataStore.data.first()[MY_KEY] ?: false
    // use value
}
```

### Mistake 3: No Error Handling on Flows

```kotlin
// ❌ WRONG — crash if file corrupted
val flow = context.dataStore.data.map { it[KEY] ?: false }

// ✅ CORRECT — catch IOException
val flow = context.dataStore.data
    .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
    .map { it[KEY] ?: false }
```

### Mistake 4: Reading Inside edit { } Using dataStore.data

```kotlin
// ❌ WRONG — DEADLOCK — edit holds mutex, data tries to acquire mutex
dataStore.edit { prefs ->
    val other = dataStore.data.first()  // this will deadlock forever!
}

// ✅ CORRECT — edit already gives you current prefs
dataStore.edit { prefs ->
    val current = prefs[MY_KEY] ?: 0    // read from the prefs parameter
    prefs[MY_KEY] = current + 1
}
```

### Mistake 5: Storing Large Data

```kotlin
// ❌ WRONG — DataStore is not designed for large binary data
suspend fun saveImage(bitmap: Bitmap) {
    val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
    val base64 = Base64.encodeToString(bytes, Base64.DEFAULT)
    context.dataStore.edit { it[IMAGE_KEY] = base64 }  // could be megabytes!
}

// ✅ CORRECT — store the file path, not the file
suspend fun saveImagePath(path: String) {
    context.dataStore.edit { it[IMAGE_PATH_KEY] = path }
}
```

### Mistake 6: Creating Keys Inside Functions

```kotlin
// ❌ WRONG — creates a new key object on every call (minor issue, but wasteful)
suspend fun setDarkMode(enabled: Boolean) {
    val key = booleanPreferencesKey("dark_mode")  // created every call
    context.dataStore.edit { it[key] = enabled }
}

// ✅ CORRECT — keys as companion object constants
companion object {
    val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")  // created once
}

suspend fun setDarkMode(enabled: Boolean) {
    context.dataStore.edit { it[DARK_MODE_KEY] = enabled }
}
```

### Mistake 7: Observing DataStore Directly in Composables (Without ViewModel)

```kotlin
// ❌ WRONG — directly in composable, no lifecycle management
@Composable
fun MyScreen() {
    val context = LocalContext.current
    val isDark by context.dataStore.data
        .map { it[DARK_MODE_KEY] ?: false }
        .collectAsState(initial = false)
    // This has no lifecycle awareness, can leak, hard to test
}

// ✅ CORRECT — through ViewModel
@Composable
fun MyScreen(viewModel: MyViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
}
```

---

## Chapter 22: Real-World Patterns & Examples

### Pattern 1: App Theme Manager

A complete reactive theme system that applies across the entire app:

```kotlin
// AppPreferences.kt — theme keys
val THEME_MODE    = stringPreferencesKey("theme_mode")   // "light"/"dark"/"system"
val PRIMARY_COLOR = stringPreferencesKey("primary_color") // hex: "#2196F3"

// ThemeViewModel.kt
class ThemeViewModel(
    private val getTheme: GetThemeModeUseCase,
    private val setTheme: SetThemeModeUseCase
) : ViewModel() {

    val themeMode: StateFlow<String> = getTheme()
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")
        //                                  ↑ Eagerly because we need theme BEFORE first frame

    fun changeTheme(mode: String) = viewModelScope.launch {
        setTheme(mode)
    }
}

// MainActivity.kt — apply theme at root
@Composable
fun WallStreetApp(themeViewModel: ThemeViewModel = koinViewModel()) {
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()

    val isDark = when (themeMode) {
        "dark"   -> true
        "light"  -> false
        else     -> isSystemInDarkTheme()
    }

    WallStreetTheme(darkTheme = isDark) {
        AppNavigation(...)
    }
}
```

When the user changes the theme in Settings, the entire app re-themes instantly — no restart needed.

### Pattern 2: Onboarding Gate

```kotlin
// AppPreferences.kt
val ONBOARDING_KEY = booleanPreferencesKey("onboarding_completed")

suspend fun isOnboardingDone(): Boolean =
    context.dataStore.data.first()[ONBOARDING_KEY] ?: false

suspend fun markOnboardingDone() =
    context.dataStore.edit { it[ONBOARDING_KEY] = true }

// MainActivity.kt — startup routing
private suspend fun decideStartDestination(): StartDestination {
    val user    = FirebaseAuth.getInstance().currentUser
    val prefs   = OnboardingPreferences(applicationContext)
    val onboarded = prefs.isOnboardingDone()

    return when {
        user == null && !onboarded      -> StartDestination.Onboarding
        user == null && onboarded       -> StartDestination.Auth
        user != null && !user.isEmailVerified -> StartDestination.Otp
        else                            -> StartDestination.Home
    }
}
```

### Pattern 3: Feature Flags / App Config

```kotlin
object FeatureFlags {
    val BETA_FEATURES_ENABLED = booleanPreferencesKey("beta_features")
    val NEW_DASHBOARD_ENABLED = booleanPreferencesKey("new_dashboard")
    val MAX_TRADES_PER_DAY    = intPreferencesKey("max_trades_per_day")
}

class FeatureFlagPreferences(private val context: Context) {

    val isBetaEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[FeatureFlags.BETA_FEATURES_ENABLED] ?: false }

    val isNewDashboardEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[FeatureFlags.NEW_DASHBOARD_ENABLED] ?: false }

    val maxTradesPerDay: Flow<Int> = context.dataStore.data
        .map { it[FeatureFlags.MAX_TRADES_PER_DAY] ?: 10 }

    // Update from remote config
    suspend fun updateFromRemoteConfig(config: Map<String, Any>) {
        context.dataStore.edit { prefs ->
            (config["beta_enabled"] as? Boolean)?.let {
                prefs[FeatureFlags.BETA_FEATURES_ENABLED] = it
            }
            (config["new_dashboard"] as? Boolean)?.let {
                prefs[FeatureFlags.NEW_DASHBOARD_ENABLED] = it
            }
            (config["max_trades"] as? Int)?.let {
                prefs[FeatureFlags.MAX_TRADES_PER_DAY] = it
            }
        }
    }
}
```

### Pattern 4: User Session Info (NOT Tokens)

```kotlin
// Safe to store in DataStore: non-sensitive user info
object SessionKeys {
    val USER_ID      = stringPreferencesKey("user_id")
    val USER_NAME    = stringPreferencesKey("user_name")
    val USER_EMAIL   = stringPreferencesKey("user_email")
    val LAST_LOGIN   = longPreferencesKey("last_login_ms")
}

// DO NOT store tokens in DataStore!
// Use EncryptedSharedPreferences or Android Keystore for tokens

class SessionPreferences(private val context: Context) {

    val currentUserId: Flow<String?> = context.dataStore.data
        .map { it[SessionKeys.USER_ID] }

    suspend fun saveSession(userId: String, name: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[SessionKeys.USER_ID]    = userId
            prefs[SessionKeys.USER_NAME]  = name
            prefs[SessionKeys.USER_EMAIL] = email
            prefs[SessionKeys.LAST_LOGIN] = System.currentTimeMillis()
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(SessionKeys.USER_ID)
            prefs.remove(SessionKeys.USER_NAME)
            prefs.remove(SessionKeys.USER_EMAIL)
            prefs.remove(SessionKeys.LAST_LOGIN)
        }
    }
}
```

### Pattern 5: Rating / Review Prompt Logic

```kotlin
object RatingKeys {
    val LAUNCH_COUNT     = intPreferencesKey("launch_count")
    val HAS_RATED        = booleanPreferencesKey("has_rated")
    val LAST_PROMPT_MS   = longPreferencesKey("last_rating_prompt_ms")
    val DECLINED_COUNT   = intPreferencesKey("declined_count")
}

class RatingManager(private val context: Context) {

    // Returns true if we should show the rating dialog
    suspend fun shouldShowRating(): Boolean {
        val prefs        = context.dataStore.data.first()
        val count        = prefs[RatingKeys.LAUNCH_COUNT]     ?: 0
        val hasRated     = prefs[RatingKeys.HAS_RATED]        ?: false
        val lastPrompt   = prefs[RatingKeys.LAST_PROMPT_MS]   ?: 0L
        val declines     = prefs[RatingKeys.DECLINED_COUNT]   ?: 0

        if (hasRated) return false
        if (declines >= 3) return false
        if (count < 5) return false

        val daysSinceLastPrompt = (System.currentTimeMillis() - lastPrompt) / 86_400_000L
        return daysSinceLastPrompt >= 30
    }

    suspend fun onLaunch() {
        context.dataStore.edit { prefs ->
            prefs[RatingKeys.LAUNCH_COUNT] = (prefs[RatingKeys.LAUNCH_COUNT] ?: 0) + 1
        }
    }

    suspend fun onRated() {
        context.dataStore.edit { prefs ->
            prefs[RatingKeys.HAS_RATED] = true
        }
    }

    suspend fun onDeclined() {
        context.dataStore.edit { prefs ->
            prefs[RatingKeys.DECLINED_COUNT]   = (prefs[RatingKeys.DECLINED_COUNT] ?: 0) + 1
            prefs[RatingKeys.LAST_PROMPT_MS]   = System.currentTimeMillis()
        }
    }
}
```

### Pattern 6: Filter and Sort Preferences (Real App Pattern from Android Docs)

This is the exact pattern from Google's official DataStore codelab:

```kotlin
// Persisting UI state across restarts
enum class SortOrder { NONE, BY_DEADLINE, BY_PRIORITY, BY_DEADLINE_AND_PRIORITY }

object FilterKeys {
    val SHOW_COMPLETED = booleanPreferencesKey("show_completed")
    val SORT_ORDER     = stringPreferencesKey("sort_order")
}

data class FilterState(
    val showCompleted: Boolean = false,
    val sortOrder: SortOrder = SortOrder.NONE
)

class FilterPreferences(private val context: Context) {

    val filterState: Flow<FilterState> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            FilterState(
                showCompleted = prefs[FilterKeys.SHOW_COMPLETED] ?: false,
                sortOrder     = runCatching {
                    SortOrder.valueOf(prefs[FilterKeys.SORT_ORDER] ?: "NONE")
                }.getOrDefault(SortOrder.NONE)
            )
        }

    suspend fun setShowCompleted(show: Boolean) =
        context.dataStore.edit { it[FilterKeys.SHOW_COMPLETED] = show }

    suspend fun setSortOrder(order: SortOrder) =
        context.dataStore.edit { it[FilterKeys.SORT_ORDER] = order.name }

    // Atomic update of both
    suspend fun applyFilter(show: Boolean, order: SortOrder) =
        context.dataStore.edit { prefs ->
            prefs[FilterKeys.SHOW_COMPLETED] = show
            prefs[FilterKeys.SORT_ORDER]     = order.name
        }
}
```

### Pattern 7: Set<String> for Multi-Select Preferences

```kotlin
val ENABLED_NOTIFICATIONS = stringSetPreferencesKey("enabled_notif_types")

// Save a set
suspend fun setEnabledNotifications(types: Set<String>) {
    context.dataStore.edit { it[ENABLED_NOTIFICATIONS] = types }
}

// Add to set
suspend fun enableNotificationType(type: String) {
    context.dataStore.edit { prefs ->
        val current = prefs[ENABLED_NOTIFICATIONS] ?: emptySet()
        prefs[ENABLED_NOTIFICATIONS] = current + type
    }
}

// Remove from set
suspend fun disableNotificationType(type: String) {
    context.dataStore.edit { prefs ->
        val current = prefs[ENABLED_NOTIFICATIONS] ?: emptySet()
        prefs[ENABLED_NOTIFICATIONS] = current - type
    }
}

// Read
val enabledTypesFlow: Flow<Set<String>> = context.dataStore.data
    .map { it[ENABLED_NOTIFICATIONS] ?: emptySet() }
```

---

## Chapter 23: Advanced Patterns

### Multiple DataStore Files

You can have more than one DataStore in your app — each for a different concern:

```kotlin
// Separate files for separate concerns
val Context.userPrefsStore: DataStore<Preferences>
        by preferencesDataStore(name = "user_prefs")

val Context.appConfigStore: DataStore<Preferences>
        by preferencesDataStore(name = "app_config")

val Context.featureFlagStore: DataStore<Preferences>
        by preferencesDataStore(name = "feature_flags")
```

Each file is independent. Modifying `userPrefsStore` won't trigger emissions from `appConfigStore`.

### Combining DataStore with Other Flows

```kotlin
// Combine DataStore preferences with Room data
val filteredTradesFlow: Flow<List<Trade>> = combine(
    filterPreferences.filterState,   // Flow<FilterState> from DataStore
    tradeRepository.allTrades        // Flow<List<Trade>> from Room
) { filterState, trades ->
    trades
        .filter { filterState.showCompleted || !it.isCompleted }
        .sortedWith(
            when (filterState.sortOrder) {
                SortOrder.BY_DEADLINE  -> compareBy { it.deadline }
                SortOrder.BY_PRIORITY  -> compareByDescending { it.priority }
                else                   -> compareBy { it.id }
            }
        )
}
```

This is powerful — when either the filter changes OR the trades change, the UI automatically gets the correctly filtered and sorted list.

### Encrypted DataStore (1.1.x+)

For sensitive data that needs encryption:

```kotlin
// Add dependency
implementation("androidx.datastore:datastore-tink:1.1.1")
implementation("com.google.crypto.tink:tink-android:1.10.0")

// Create encrypted DataStore
val aead = TinkConfig.registerAndGetAead(context)
val encryptedSerializer = AeadSerializer(PreferencesSerializer, aead)

val Context.encryptedStore: DataStore<Preferences> by dataStore(
    fileName   = "encrypted_prefs.pb",
    serializer = encryptedSerializer
)
```

The data is encrypted at rest using AES-256-GCM. Use this for auth tokens, session data, or any sensitive user information.

---

## Chapter 24: Quick Reference Cheat Sheet

### Setup
```kotlin
// Top level of a file, outside any class
val Context.dataStore by preferencesDataStore(name = "prefs")
```

### All Key Types
```kotlin
val BOOL   = booleanPreferencesKey("key")
val INT    = intPreferencesKey("key")
val LONG   = longPreferencesKey("key")
val FLOAT  = floatPreferencesKey("key")
val DOUBLE = doublePreferencesKey("key")
val STRING = stringPreferencesKey("key")
val SET    = stringSetPreferencesKey("key")
```

### Read (Reactive — use in ViewModel)
```kotlin
val flow: Flow<Boolean> = context.dataStore.data
    .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
    .map { it[MY_KEY] ?: false }
```

### Read (One-shot — use for startup checks)
```kotlin
suspend fun get(): Boolean = context.dataStore.data.first()[MY_KEY] ?: false
```

### Write
```kotlin
context.dataStore.edit { it[MY_KEY] = newValue }
```

### Delete One Key
```kotlin
context.dataStore.edit { it.remove(MY_KEY) }
```

### Delete All
```kotlin
context.dataStore.edit { it.clear() }
```

### With Corruption Handler
```kotlin
val Context.dataStore by preferencesDataStore(
    name = "prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)
```

### In ViewModel (stateIn)
```kotlin
val state: StateFlow<Boolean> = repo.myFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
```

### In Compose
```kotlin
val value by viewModel.state.collectAsStateWithLifecycle()
```

### In Koin
```kotlin
single { AppPreferences(androidContext()) }          // DataStore wrapper
single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }
factory { SetDarkModeUseCase(get()) }
viewModel { SettingsViewModel(get(), get()) }
```

---

## Final Mental Model

```
┌────────────────────────────────────────────────────────────────┐
│                    DATASTORE MENTAL MODEL                      │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  Disk (.preferences_pb)                                        │
│       ↕ (atomic read/write via temp file + rename)             │
│  DataStore (in-memory cache + SharedFlow broadcaster)          │
│       ↕ (Flow<Preferences>)                                    │
│  Your Repository (maps to Flow<YourType>)                      │
│       ↕ (StateFlow via stateIn)                                │
│  ViewModel (UiState)                                           │
│       ↕ (collectAsStateWithLifecycle)                          │
│  Composable (UI)                                               │
│                                                                │
│  WRITE direction: Composable → ViewModel → Repository →        │
│      DataStore → disk (all async via coroutines)               │
│                                                                │
│  READ direction: disk → DataStore → Repository → ViewModel →   │
│      Composable (pushed via Flow, never pulled)                │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### The 5 Rules to Always Remember

1. **One instance per file** — always use property delegate (`by preferencesDataStore`)
2. **Always use `single`** in Koin — never `factory` for DataStore
3. **Always add `.catch`** to Flow reads for IOException
4. **Never use `runBlocking`** — always properly suspend or launch a coroutine
5. **DataStore for settings, Room for data** — never store lists or collections in DataStore

---

*This guide covers DataStore 1.1.1 — the version in your WallStreet project (libs.versions.toml: `datastorePreferences = "1.1.1"`).*
