 # Android Master Engineering Checklist

## Deep, Accurate, Production-Level — Chapter-wise Tracking Index

> **Priority Legend:** 🟥 **MUST KNOW** — Industry standard, asked in every interview, used daily 🟨  **MEDIUM** — Important for senior roles, real-world projects 🟩  **GOOD TO KNOW** — Specialized, niche, or rarely tested but valuable

> **How to use:** Tick an item only when you can demonstrate it in code or explain it from memory without notes. Each section ends with a Proof Task — build it before moving forward. This is not a reading list. It is a doing list.

---

## TABLE OF CONTENTS

1. [PHASE 0 — KOTLIN LANGUAGE MASTERY](https://claude.ai/chat/fedcf8ab-6c00-4669-8a95-e5b38014c82f#phase-0)
    - 0.1 Kotlin Core Language
    - 0.2 Kotlin Coroutines (Full Depth)
2. [PHASE 1 — ANDROID SYSTEM INTERNALS](https://claude.ai/chat/fedcf8ab-6c00-4669-8a95-e5b38014c82f#phase-1)
    - 1.1 Android OS Fundamentals
    - 1.2 Memory & Process Management
3. [PHASE 2 — APPLICATION COMPONENTS](https://claude.ai/chat/fedcf8ab-6c00-4669-8a95-e5b38014c82f#phase-2)
    - 2.1 Activity Deep Dive
    - 2.2 Fragment Deep Dive
    - 2.3 Services
    - 2.4 BroadcastReceiver
    - 2.5 ContentProvider
    - 2.6 Intents & Manifest
4. [PHASE 3 — UI ENGINEERING](https://claude.ai/chat/fedcf8ab-6c00-4669-8a95-e5b38014c82f#phase-3)
    - 3.1 View System (Legacy Foundation)
    - 3.2 Jetpack Compose (Deep Engineering)
5. [PHASE 4 — APP ARCHITECTURE](https://claude.ai/chat/fedcf8ab-6c00-4669-8a95-e5b38014c82f#phase-4)
    - 4.1 Layered Architecture (MVVM / Clean / MVI)
    - 4.2 Multi-Module Architecture
6. [PHASE 5 — DEPENDENCY INJECTION](https://claude.ai/chat/fedcf8ab-6c00-4669-8a95-e5b38014c82f#phase-5)
    - 5.1 Hilt (Primary)
    - 5.2 Koin (Alternative — Industry Widely Used)
    - 5.3 Dagger 2 Internals
7. [PHASE 6 — DATA LAYER](https://claude.ai/chat/fedcf8ab-6c00-4669-8a95-e5b38014c82f#phase-6)
    - 6.1 Room Database
    - 6.2 DataStore
    - 6.3 Paging 3
    - 6.4 Offline-First Architecture
8. [PHASE 7 — NETWORKING](https://claude.ai/chat/fedcf8ab-6c00-4669-8a95-e5b38014c82f#phase-7)
    - 7.1 OkHttp
    - 7.2 Retrofit
    - 7.3 JSON Serialization
    - 7.4 Authentication & Security
    - 7.5 Error Handling & Resilience
    - 7.6 WebSockets & Real-Time
    - 7.7 GraphQL & gRPC
9. [PHASE 8 — BACKGROUND WORK & CONCURRENCY](https://claude.ai/chat/fedcf8ab-6c00-4669-8a95-e5b38014c82f#phase-8)
    - 8.1 WorkManager
    - 8.2 AlarmManager
    - 8.3 Foreground Services vs WorkManager
10. [PHASE 9 — PERFORMANCE ENGINEERING](https://claude.ai/chat/fedcf8ab-6c00-4669-8a95-e5b38014c82f#phase-9)
    - 9.1 Memory
    - 9.2 CPU & Rendering
    - 9.3 App Startup
    - 9.4 Network & Battery
11. [PHASE 10 — TESTING MASTERY](https://claude.ai/chat/fedcf8ab-6c00-4669-8a95-e5b38014c82f#phase-10)
    - 10.1 Unit Testing
    - 10.2 Coroutine Testing
    - 10.3 ViewModel & Repository Testing
    - 10.4 Instrumentation & UI Testing
    - 10.5 Screenshot & Advanced Testing
    - 10.6 CI/CD
12. [PHASE 11 — SECURITY & PRIVACY](https://claude.ai/chat/fedcf8ab-6c00-4669-8a95-e5b38014c82f#phase-11)
    - 11.1 Secure Storage
    - 11.2 Network Security
    - 11.3 Runtime Permissions
    - 11.4 Data Privacy & Scoped Storage
    - 11.5 Code Security
    - 11.6 Play Integrity & Anti-Tamper
13. [PHASE 12 — BUILD & RELEASE](https://claude.ai/chat/fedcf8ab-6c00-4669-8a95-e5b38014c82f#phase-12)
    - 12.1 Gradle Deep Dive
    - 12.2 R8 / ProGuard
    - 12.3 App Signing & Distribution
    - 12.4 Play Console & Production
    - 12.5 Crash Reporting & Analytics
14. [PHASE 13 — ACCESSIBILITY & INCLUSIVITY](https://claude.ai/chat/fedcf8ab-6c00-4669-8a95-e5b38014c82f#phase-13)
15. [PHASE 14 — ADVANCED DEVICE APIS](https://claude.ai/chat/fedcf8ab-6c00-4669-8a95-e5b38014c82f#phase-14)
    - 14.1 Camera
    - 14.2 Location
    - 14.3 Bluetooth
    - 14.4 Biometrics
    - 14.5 Push Notifications (FCM)
    - 14.6 Widgets & Shortcuts
    - 14.7 Media3 / ExoPlayer
    - 14.8 NFC & Sensors
16. [PHASE 15 — KOTLIN MULTIPLATFORM & MODERN ECOSYSTEM](https://claude.ai/chat/fedcf8ab-6c00-4669-8a95-e5b38014c82f#phase-15)
    - 15.1 Kotlin Multiplatform (KMP)
    - 15.2 On-Device ML
17. [PHASE 16 — ADVANCED FORM FACTORS](https://claude.ai/chat/fedcf8ab-6c00-4669-8a95-e5b38014c82f#phase-16)
    - 16.1 Large Screens & Foldables
    - 16.2 Wear OS
18. [PHASE 17 — TOOLING & DEBUGGING](https://claude.ai/chat/fedcf8ab-6c00-4669-8a95-e5b38014c82f#phase-17)
    - 17.1 Android Studio & Profilers
    - 17.2 ADB & Command Line
    - 17.3 Localization & i18n
19. [CAPSTONE PROJECTS](https://claude.ai/chat/fedcf8ab-6c00-4669-8a95-e5b38014c82f#capstone)
20. [INTERVIEW READINESS CHECKLIST](https://claude.ai/chat/fedcf8ab-6c00-4669-8a95-e5b38014c82f#interview)

---

## PHASE 0 — KOTLIN LANGUAGE MASTERY

### Chapter 0.1 — Kotlin Core Language

#### 0.1.1 Basics & Types

- [x] 🟥 Understand how Kotlin compiles to JVM bytecode and what `.class` files are produced
- [x] 🟥 `val` vs `var` — immutability at reference level vs mutation; understand why `val` does not guarantee deep immutability
- [x] 🟥 Primitive types vs Kotlin's type system (`Int`, `Long`, `Double`, `Boolean`, `Char`) and how they map to JVM primitives vs boxed types
- [x] 🟥 String templates and multi-line strings (`trimIndent`, `trimMargin`)
- [x] 🟥 Type inference — when Kotlin infers types and when you must annotate explicitly
- [ ] 🟥 Nullable types: `?`, `!!`, `?.`, `?:`, `let`, `also`, `run`, `apply`, `with`
- [x] 🟥 Smart casts: how the compiler tracks nullability and type after checks
- [ ] 🟥 `when` expression — exhaustive sealed class matching, guard conditions, range checks, destructuring in `when`
- [ ] 🟨  Nothing type — `TODO()`, `throw` as an expression, infinite loop as `Nothing`
- [ ] 🟨  `Any`, `Unit`, `Nothing` — understand the Kotlin type hierarchy

#### 0.1.2 Functions

- [x] 🟥 Named parameters and default argument values
- [ ] 🟥 Single-expression functions
- [ ] 🟥 Extension functions — how they compile (static dispatch), when to use them, limitations
- [ ] 🟨  Infix functions — syntax rules and common stdlib examples (`to`, `and`)
- [ ] 🟨  Operator overloading — `plus`, `minus`, `invoke`, `get`, `set`, `compareTo`
- [ ] 🟩  Tail-recursive functions (`tailrec`)
- [ ] 🟨  Local functions (functions inside functions)
- [x] 🟥 Higher-order functions — passing functions as parameters, returning functions
- [ ] 🟥 Lambdas with receivers — the basis for DSLs, how `this` is captured
- [x] 🟥 `it` vs named lambda parameters — readability rules
- [ ] 🟥 Function types: `(Int, String) -> Boolean`, `suspend () -> Unit`

#### 0.1.3 OOP

- [x] 🟥 Classes, constructors (primary/secondary), `init` blocks
- [x] 🟥 `data class` — `copy()`, `equals()`, `hashCode()`, `toString()`, destructuring, component functions
- [x] 🟥 `sealed class` vs `enum class` — when to use which for state modeling
- [x] 🟥 `object` declarations — singletons, companion objects, anonymous objects
- [x] 🟥 `interface` vs `abstract class` — default method implementations, multiple inheritance
- [x] 🟥 Delegation: `by` keyword, implementing interface via a delegate, property delegation
- [ ] 🟨  `lazy {}` — thread safety modes (`NONE`, `SYNCHRONIZED`, `PUBLICATION`)
- [ ] 🟨  `observable` and `vetoable` property delegates
- [ ] 🟨  Value classes / inline classes — purpose, JVM representation, limitations
- [ ] 🟨  Type aliases — `typealias`

#### 0.1.4 Generics

- [ ] 🟥 Generic classes and functions
- [ ] 🟥 Type variance: `out` (covariance), `in` (contravariance), invariance
- [ ] 🟨  Use-site vs declaration-site variance
- [ ] 🟨  Star projection `<*>`
- [ ] 🟥 `reified` type parameters — why they require `inline`, how they bypass type erasure
- [ ] 🟨  Generic constraints (`where T : Comparable<T>, T : Serializable`)

#### 0.1.5 Collections & Functional

- [x] 🟥 `List`, `MutableList`, `Set`, `Map`, `Sequence` — differences and when to prefer each
- [ ] 🟥 `map`, `filter`, `flatMap`, `fold`, `reduce`, `groupBy`, `associateBy`, `zip`, `partition`
- [ ] 🟥 `Sequence` vs `List` operations — lazy evaluation, when sequences are better
- [ ] 🟨  `buildList`, `buildMap`, `buildSet`
- [ ] 🟨  Destructuring declarations
- [x] 🟥 Range operators: `..`, `until`, `downTo`, `step`
- [ ] 🟨  `kotlinx.collections.immutable` — `ImmutableList`, `PersistentMap` — required for Compose stability

#### 0.1.6 Advanced Kotlin

- [ ] 🟨  Arrow library — functional programming (`Either`, `Option`, `Validated`) for error handling
- [ ] 🟥 KSP (Kotlin Symbol Processing) vs KAPT — how annotation processing works; KSP is 2x faster
- [ ] 🟨  Kotlin DSL building — using lambdas with receivers to build type-safe DSLs

---

### Chapter 0.2 — Kotlin Coroutines (Full Depth)

#### 0.2.1 Fundamentals

- [ ] 🟥 What a coroutine actually is — a suspendable computation, NOT a thread
- [ ] 🟥 `suspend` keyword — what it does at bytecode level (continuation-passing style transformation)
- [ ] 🟥 `CoroutineScope` — the owner of coroutine lifecycles, every coroutine needs a scope
- [ ] 🟥 `CoroutineContext` — a map of key-value elements (`Job`, `Dispatcher`, `CoroutineName`, `ExceptionHandler`)
- [ ] 🟥 `Job` — lifecycle states: New, Active, Completing, Completed, Cancelling, Cancelled
- [ ] 🟥 `launch` vs `async` — fire-and-forget vs deferred result
- [ ] 🟥 `Deferred<T>` — `await()` suspends until result, `await()` rethrows exceptions
- [ ] 🟥 `coroutineScope {}` vs `supervisorScope {}` — how exceptions propagate differently
- [ ] 🟥 `withContext()` — switch dispatcher without creating a new coroutine

#### 0.2.2 Dispatchers

- [ ] 🟥 `Dispatchers.Main` — UI thread, must not be blocked
- [ ] 🟥 `Dispatchers.IO` — thread pool for blocking I/O, sized to 64 threads by default
- [ ] 🟥 `Dispatchers.Default` — CPU-bound work, thread pool sized to CPU cores
- [ ] 🟨  `Dispatchers.Unconfined` — runs in the caller thread until first suspension
- [ ] 🟨  Custom dispatchers with `newSingleThreadContext` and `Executors.asCoroutineDispatcher()`
- [ ] 🟨  `limitedParallelism()` for controlling concurrency on existing dispatchers

#### 0.2.3 Structured Concurrency

- [ ] 🟥 Why structured concurrency exists — prevents coroutine leaks
- [ ] 🟥 Parent-child relationship: parent waits for all children; child cancellation does not cancel parent (with SupervisorJob)
- [ ] 🟥 `SupervisorJob` — child failures are independent; use in `viewModelScope`, `lifecycleScope`
- [ ] 🟥 Exception propagation rules: in `launch` exceptions propagate to parent; in `async` exceptions rethrow on `await()`
- [ ] 🟥 `CoroutineExceptionHandler` — last-resort handler, only for `launch` at root level
- [ ] 🟥 Cancellation propagation — cooperative cancellation, `isActive`, `ensureActive()`, `yield()`
- [ ] 🟥 `CancellationException` — must never be swallowed in `catch (e: Exception)`
- [ ] 🟥 `withTimeout` vs `withTimeoutOrNull`
- [ ] 🟨  `NonCancellable` context — for cleanup code that must run even during cancellation

#### 0.2.4 Flow (Cold Streams)

- [ ] 🟥 `Flow<T>` — cold stream: code runs only when collected
- [ ] 🟥 Flow builders: `flow {}`, `flowOf()`, `asFlow()`, `callbackFlow {}`, `channelFlow {}`
- [ ] 🟥 Terminal operators: `collect`, `toList`, `first`, `single`, `last`, `reduce`, `fold`
- [ ] 🟥 Intermediate operators: `map`, `filter`, `flatMapConcat`, `flatMapMerge`, `flatMapLatest`, `transform`
- [ ] 🟥 `onEach` vs `collect` — side effects in a chain
- [ ] 🟥 `catch` operator — catches upstream exceptions, must be placed before `collect`
- [ ] 🟨  `onCompletion` — runs on normal and exceptional completion
- [ ] 🟨  `buffer()` — decouples producer and consumer speeds
- [ ] 🟨  `conflate()` — skip intermediate values when collector is slow
- [ ] 🟨  `debounce()`, `throttle()`, `sample()` — time-based operators
- [ ] 🟥 `zip()` vs `combine()` — zip waits for both, combine emits on any change
- [ ] 🟥 `flowOn()` — changes upstream context, does NOT affect downstream
- [ ] 🟥 `distinctUntilChanged()` — suppress duplicate consecutive emissions
- [ ] 🟥 Flow cancellation — automatically cancelled when the collecting coroutine is cancelled

#### 0.2.5 StateFlow & SharedFlow

- [ ] 🟥 `StateFlow<T>` — hot, stateful, always has a value, replays last to new collectors
- [ ] 🟥 `MutableStateFlow` — `value` property, `update {}` for atomic updates
- [ ] 🟥 `SharedFlow<T>` — hot, configurable replay, no initial value requirement
- [ ] 🟥 `MutableSharedFlow` — `emit()` (suspends if buffer full), `tryEmit()` (non-suspending)
- [ ] 🟨  `replay` parameter in `SharedFlow` — how many past values new subscribers receive
- [ ] 🟥 `SharingStarted.WhileSubscribed(5000)` — start/stop behavior to avoid leaks
- [ ] 🟥 Difference between `StateFlow` and `SharedFlow` — when to use each
- [ ] 🟥 `stateIn()` — convert a cold Flow to StateFlow
- [ ] 🟨  `shareIn()` — convert a cold Flow to SharedFlow
- [ ] 🟥 `collectAsState()` and `collectAsStateWithLifecycle()` in Compose

#### 0.2.6 Channels

- [ ] 🟨  `Channel<T>` — a hot communication primitive between coroutines
- [ ] 🟨  Channel capacity types: `RENDEZVOUS` (0), `BUFFERED`, `CONFLATED`, `UNLIMITED`
- [ ] 🟨  `send()` vs `offer()` vs `trySend()` — suspending vs non-suspending
- [ ] 🟨  `receive()` vs `poll()` vs `tryReceive()`
- [ ] 🟨  `for (item in channel)` — iteration until channel is closed
- [ ] 🟨  `close()` — graceful shutdown; `Channel.isClosedForSend` / `isClosedForReceive`
- [ ] 🟩  `produce {}` coroutine builder — returns a `ReceiveChannel<T>`
- [ ] 🟩  Fan-out (multiple receivers) and fan-in (multiple senders) patterns
- [ ] 🟨  Why Channels are often replaced by SharedFlow for event buses

#### 0.2.7 Coroutine Testing

- [ ] 🟥 `runTest {}` (replaces deprecated `runBlockingTest`) — automatically advances virtual time
- [ ] 🟥 `TestCoroutineDispatcher` vs `StandardTestDispatcher` vs `UnconfinedTestDispatcher`
- [ ] 🟥 `advanceTimeBy()`, `advanceUntilIdle()`, `runCurrent()`
- [ ] 🟥 Testing StateFlow and SharedFlow emissions with `turbine` library
- [ ] 🟥 Injecting `TestDispatcher` via constructor injection for testability
- [ ] 🟨  Testing cancellation behavior

---

## PHASE 1 — ANDROID SYSTEM INTERNALS

### Chapter 1.1 — Android OS Fundamentals

- [ ] 🟥 Android OS layers: Linux kernel → HAL → Android Runtime → Framework → Applications
- [ ] 🟨  Linux kernel role: process isolation, memory management, device drivers, security
- [ ] 🟥 Android sandbox: each app runs in its own Linux user ID (UID), own process, own JVM instance
- [ ] 🟥 Zygote process — template process that all app processes are forked from; why this speeds up startup
- [ ] 🟨  `app_process` — how an app's main process is started from Zygote
- [ ] 🟥 ART (Android Runtime) vs Dalvik — ART uses AOT compilation; Dalvik used JIT
- [ ] 🟨  ART profile-guided optimization — cloud profiles, how Google Play optimizes installed apps
- [ ] 🟨  DEX format — how Kotlin/Java compiles to `.dex` files, `multidex` for apps exceeding 64K method limit
- [ ] 🟥 APK structure: `AndroidManifest.xml`, `classes.dex`, `res/`, `assets/`, `lib/`, `META-INF/`
- [ ] 🟨  AAB (Android App Bundle) structure: how Play Store splits and delivers per-device
- [ ] 🟥 Binder IPC — the fundamental inter-process communication mechanism on Android, how `AIDL` works
- [ ] 🟨  Activity Manager Service (AMS) — manages the Activity back stack, process lifecycles
- [ ] 🟨  Window Manager Service (WMS) — owns surfaces and drawing
- [ ] 🟨  PackageManager — resolves intents, manages installed packages

### Chapter 1.2 — Memory & Process Management

- [ ] 🟥 `LMK` (Low Memory Killer) — how Android reclaims memory by killing processes based on importance hierarchy
- [ ] 🟥 Process importance levels: Foreground > Visible > Service > Cached
- [ ] 🟨  `onTrimMemory()` callback — levels (`TRIM_MEMORY_RUNNING_CRITICAL`, etc.) and correct responses
- [ ] 🟨  `ActivityManager.MemoryInfo` — check available memory programmatically
- [ ] 🟨  Heap memory: `dalvikPrivateDirty`, native heap, graphics memory (in Profiler)
- [ ] 🟥 Memory leak patterns: static references to Context, anonymous inner classes, unregistered listeners, bitmap caches
- [ ] 🟨  `WeakReference` and `SoftReference` — when to use (sparingly)

---

## PHASE 2 — APPLICATION COMPONENTS

### Chapter 2.1 — Activity Deep Dive

- [ ] 🟥 Complete lifecycle: `onCreate` → `onStart` → `onResume` → `onPause` → `onStop` → `onDestroy`
- [ ] 🟥 `onSaveInstanceState(Bundle)` vs `ViewModel` — what gets saved to Bundle vs ViewModel
- [ ] 🟥 Configuration changes: rotation, locale, keyboard, multi-window — which trigger recreation
- [ ] 🟨  `android:configChanges` manifest attribute — when to declare it and what it prevents
- [ ] 🟥 `SavedStateHandle` in ViewModel — survives process death, unlike plain ViewModel fields
- [ ] 🟨  `onRestoreInstanceState` vs restoring in `onCreate`
- [ ] 🟥 Task and back stack: what a Task is, `FLAG_ACTIVITY_NEW_TASK` and `FLAG_ACTIVITY_CLEAR_TOP`
- [ ] 🟥 Launch modes: `standard`, `singleTop`, `singleTask`, `singleInstance` — behavioral differences
- [ ] 🟥 `ActivityResult API` (`registerForActivityResult`) vs deprecated `startActivityForResult`
- [ ] 🟨  Multi-window and freeform mode — handling `onMultiWindowModeChanged`
- [ ] 🟨  `Activity.finish()` vs `finishAndRemoveTask()` vs `finishAffinity()`
- [ ] 🟩  `onUserLeaveHint()` and `onWindowFocusChanged()` — PiP use cases
- [ ] 🟨  Predictive Back Gesture (Android 13+) — `OnBackPressedCallback`, `predictiveBackProgress`

### Chapter 2.2 — Fragment Deep Dive

- [ ] 🟥 Fragment lifecycle vs Activity lifecycle — `onAttach`, `onCreate`, `onCreateView`, `onViewCreated`, `onStart`, `onResume`, `onPause`, `onStop`, `onDestroyView`, `onDestroy`, `onDetach`
- [ ] 🟥 Why `onDestroyView` ≠ `onDestroy` — view is destroyed on back stack, fragment instance survives
- [ ] 🟥 `viewLifecycleOwner` vs `this` (fragment) as LifecycleOwner — always use `viewLifecycleOwner` for UI observers
- [ ] 🟥 Fragment arguments: always use `newInstance()` factory pattern with Bundle arguments
- [ ] 🟥 `FragmentManager` and `FragmentTransaction`: `add`, `replace`, `remove`, `hide`, `show`
- [ ] 🟥 Back stack management: `addToBackStack()`, `popBackStack()`
- [ ] 🟥 Fragment result API (`setFragmentResult`, `setFragmentResultListener`) — replaces interface callbacks
- [ ] 🟨  `DialogFragment` — correct lifecycle, `show()`, `dismiss()`
- [ ] 🟨  `BottomSheetDialogFragment` — `BottomSheetBehavior` states
- [ ] 🟨  Nested fragments and child `FragmentManager`
- [ ] 🟨  Retain instance fragments (deprecated) vs ViewModel for state retention
- [ ] 🟩  `FragmentFactory` — dependency injection for fragments

### Chapter 2.3 — Services

- [ ] 🟥 `Service` lifecycle: `onCreate`, `onStartCommand`, `onBind`, `onUnbind`, `onDestroy`
- [ ] 🟥 Started service: `START_STICKY`, `START_NOT_STICKY`, `START_REDELIVER_INTENT` return values
- [ ] 🟨  Bound service: `bindService()`, `ServiceConnection`, `IBinder` — connection lifecycle
- [ ] 🟥 Foreground service: mandatory `startForeground()` with notification, types required since Android 14
- [ ] 🟨  `JobIntentService` (deprecated) → use `WorkManager` instead
- [ ] 🟩  AIDL (Android Interface Definition Language) for cross-process bound services
- [ ] 🟥 Android 8+ background execution limits — when services are killed, when they can start

### Chapter 2.4 — BroadcastReceiver

- [ ] 🟥 Manifest-declared vs dynamically registered receivers — implicit broadcasts in Android 8+
- [ ] 🟥 `LocalBroadcastManager` (deprecated) → use `Flow` or `LiveData` instead
- [ ] 🟨  Common system broadcasts: `ACTION_BOOT_COMPLETED`, `ACTION_BATTERY_CHANGED`, `CONNECTIVITY_ACTION`
- [ ] 🟨  Ordered broadcasts and `abortBroadcast()`
- [ ] 🟩  `goAsync()` — for receivers that need more than 10 seconds (rare)
- [ ] 🟩  Permissions on broadcasts: `sendBroadcast(intent, permission)`, `receiverPermission`

### Chapter 2.5 — ContentProvider

- [ ] 🟨  `ContentProvider` methods: `query()`, `insert()`, `update()`, `delete()`, `getType()`
- [ ] 🟨  `ContentUris` and URI patterns: `content://authority/path/#`
- [ ] 🟨  `UriMatcher` — match incoming URIs to table patterns
- [ ] 🟨  `ContentResolver` — how clients query providers across processes
- [ ] 🟨  `Cursor` — `moveToFirst()`, `moveToNext()`, column indices, closing cursors properly
- [ ] 🟥 `FileProvider` — share files via content URIs (required since Android 7 for file sharing)
- [ ] 🟥 `MediaStore` — access photos, videos, audio with scoped storage APIs

### Chapter 2.6 — Intents & Manifest

- [ ] 🟥 Explicit intent vs Implicit intent — when to use each
- [ ] 🟥 Intent flags: `FLAG_ACTIVITY_NEW_TASK`, `FLAG_ACTIVITY_CLEAR_TASK`, `FLAG_GRANT_READ_URI_PERMISSION`
- [ ] 🟥 `PendingIntent` — snapshot of an intent for later execution (notifications, widgets, alarms)
- [ ] 🟥 Intent filters: `<action>`, `<category>`, `<data>` and how they determine app selection
- [ ] 🟥 Deep links: `<data android:scheme="https">`, `App Links` (verified), Custom schemes
- [ ] 🟥 `android:exported` flag — required in Android 12+ for all components with intent filters
- [ ] 🟨  `android:process` — running components in separate processes
- [ ] 🟥 Permission declaration: `<uses-permission>`, `<permission>`, `protectionLevel`

---

## PHASE 3 — UI ENGINEERING

### Chapter 3.1 — View System (Legacy Foundation)

#### 3.1.1 Layout & Drawing

- [ ] 🟥 View measurement: `MeasureSpec` modes (`EXACTLY`, `AT_MOST`, `UNSPECIFIED`), `onMeasure()` contract
- [ ] 🟨  Layout pass: `onLayout()` — positioning children in a ViewGroup
- [ ] 🟨  Draw pass: `onDraw(Canvas)` — `Paint`, `Path`, `Canvas` operations
- [ ] 🟥 `requestLayout()` vs `invalidate()` — when each is needed
- [ ] 🟥 `ConstraintLayout` — chains, barriers, groups, `Guidelines`, `ConstraintSet` for programmatic changes
- [ ] 🟥 `LinearLayout` — weight, `weightSum`, vertical vs horizontal
- [ ] 🟨  `FrameLayout` — stacking, gravity
- [ ] 🟨  `CoordinatorLayout` — behavior-based child interactions, `AppBarLayout` collapsing
- [ ] 🟨  `MotionLayout` — constraint sets, transitions, keyframes

#### 3.1.2 Custom Views

- [ ] 🟥 Creating a custom `View` — constructor variants (1-arg, 2-arg, 3-arg, 4-arg for XML inflation)
- [ ] 🟨  Custom attributes with `<declare-styleable>`, reading with `TypedArray`
- [ ] 🟨  `onSaveInstanceState()` / `onRestoreInstanceState()` in custom Views
- [ ] 🟨  `ViewGroup` — `onInterceptTouchEvent()`, `onTouchEvent()`, touch event dispatch order
- [ ] 🟨  Accessibility: `AccessibilityNodeInfoCompat`, `importantForAccessibility`
- [ ] 🟩  `VelocityTracker` — tracking fling gestures
- [ ] 🟨  `GestureDetector` and `ScaleGestureDetector`

#### 3.1.3 RecyclerView Deep Dive

- [ ] 🟥 `RecyclerView.Adapter` — `onCreateViewHolder`, `onBindViewHolder`, `getItemCount`, `getItemViewType`
- [ ] 🟥 `ViewHolder` pattern — view caching to avoid `findViewById` on every bind
- [ ] 🟥 `DiffUtil` — `calculateDiff()`, `DiffUtil.Callback` — `areItemsTheSame` vs `areContentsTheSame`
- [ ] 🟥 `ListAdapter` with `DiffUtil.ItemCallback` — asynchronous diffing on background thread
- [ ] 🟨  `ItemDecoration` — custom dividers, spacing
- [ ] 🟨  `ItemAnimator` — `DefaultItemAnimator`, custom animations
- [ ] 🟨  `ItemTouchHelper` — swipe to dismiss, drag to reorder
- [ ] 🟥 `LayoutManager` — `LinearLayoutManager`, `GridLayoutManager`, `StaggeredGridLayoutManager`
- [ ] 🟩  `RecycledViewPool` — sharing pool across multiple RecyclerViews
- [ ] 🟩  `PrefetchCount` and `setMaxRecycledViews` for performance tuning

#### 3.1.4 View Binding & Data Binding

- [ ] 🟥 `ViewBinding` — generated binding class, `inflate()` vs `bind()`, using in Activity and Fragment correctly
- [ ] 🟨  `DataBinding` — `@{}` expressions in XML, two-way binding with `@={}`, `BindingAdapter`, `InverseBindingAdapter`
- [ ] 🟥 When to choose ViewBinding over DataBinding — simplicity vs power

#### 3.1.5 Resources & Styling

- [ ] 🟥 Resource types: `string`, `dimen`, `color`, `drawable`, `style`, `attr`, `plurals`, `array`
- [ ] 🟥 Resource qualifiers: `sw600dp`, `w600dp`, `land`, `night`, `xxhdpi`, locale
- [ ] 🟥 Themes vs Styles — theme attributes (`?attr/colorPrimary`) vs style properties
- [ ] 🟥 Material3 theme setup — `Theme.Material3.DayNight`, color scheme slots
- [ ] 🟨  `ColorStateList` — state-aware colors
- [ ] 🟨  `VectorDrawable` and `AnimatedVectorDrawable`
- [ ] 🟥 Night mode: `AppCompatDelegate.setDefaultNightMode()`, `DayNight` themes

---

### Chapter 3.2 — Jetpack Compose (Deep Engineering)

#### 3.2.1 Compose Fundamentals

- [ ] 🟥 What Compose actually is — a Kotlin compiler plugin + runtime, NOT a View wrapper
- [ ] 🟥 `@Composable` functions — idempotent, no side effects by design
- [ ] 🟥 Composition, layout, and drawing phases — three passes per frame
- [ ] 🟨  Slot table (internal) — how Compose remembers the UI tree using gap buffers
- [ ] 🟨  Gap buffer / positional memoization — how `remember` stores state in slot table by call site
- [ ] 🟨  Composition Tree — composable call hierarchy tracked by Compose runtime
- [ ] 🟩  Skia rendering — Compose uses Skia for drawing (same as Chrome)
- [ ] 🟩  RenderNode — hardware-accelerated drawing layers in Compose

#### 3.2.2 Recomposition & Stability

- [ ] 🟥 Recomposition triggers — any `State<T>` read during composition that changes
- [ ] 🟥 Recomposition scope — the smallest `@Composable` lambda that reads the state
- [ ] 🟥 Stable vs unstable types — Compose's stability inference, `@Stable`, `@Immutable` annotations
- [ ] 🟥 Why `List<T>` is unstable by default — use `ImmutableList` from `kotlinx.collections.immutable`
- [ ] 🟥 Skipping recomposition — Compose skips a composable if all inputs are equal AND it is stable
- [ ] 🟥 Lambda capture stability — unstable lambdas cause recomposition; use `remember { lambda }`
- [ ] 🟨  Compose compiler reports — enable `reportsDestination` to audit stability
- [ ] 🟩  `@NonSkippableComposable` — opt out of skipping (rare, for debugging)
- [ ] 🟨  Strong skipping mode (experimental as of 2024)
- [ ] 🟥 Layout Inspector → Compose tab — shows recomposition counts per composable

#### 3.2.3 State Management

- [ ] 🟥 `mutableStateOf()` — observable state wrapper, triggers recomposition on change
- [ ] 🟥 `remember { }` — survives recomposition but NOT configuration changes
- [ ] 🟥 `rememberSaveable { }` — survives configuration changes AND process death (via Bundle)
- [ ] 🟨  Custom `Saver` for `rememberSaveable` with complex types
- [ ] 🟥 State hoisting — lifting state up to caller; composables should be stateless where possible
- [ ] 🟥 `ViewModel` + `StateFlow` → `collectAsState()` / `collectAsStateWithLifecycle()` as standard pattern
- [ ] 🟥 `collectAsStateWithLifecycle()` — stops collecting when UI is in background
- [ ] 🟥 `derivedStateOf {}` — memoizes a computed value, only recomposes when output changes
- [ ] 🟨  `snapshotFlow {}` — converts `State<T>` into a `Flow<T>`

#### 3.2.4 Side Effects

- [ ] 🟥 `LaunchedEffect(key)` — starts a coroutine scoped to composition; re-launched when key changes
- [ ] 🟥 `DisposableEffect(key)` — setup + teardown (onDispose); for registering/unregistering listeners
- [ ] 🟨  `SideEffect {}` — runs after every successful recomposition
- [ ] 🟥 `rememberUpdatedState(value)` — captures latest value inside a long-running effect
- [ ] 🟥 `rememberCoroutineScope()` — get a `CoroutineScope` tied to composition
- [ ] 🟨  `produceState(initialValue) {}` — converts non-Compose async sources to `State<T>`

#### 3.2.5 Layout Primitives & Modifiers

- [ ] 🟥 `Column`, `Row`, `Box` — vertical, horizontal, and overlay layouts
- [ ] 🟥 `Arrangement` — `SpaceBetween`, `SpaceAround`, `SpaceEvenly`, `Center`, `Start`, `End`
- [ ] 🟥 `Alignment` — `CenterHorizontally`, `CenterVertically`, `TopStart`, etc.
- [ ] 🟥 `Modifier` — ordered chain of instructions; order matters (padding before background vs after)
- [ ] 🟨  Modifier internals — `composed {}`, `Modifier.Node` (new API)
- [ ] 🟨  Custom layouts — `Layout {}` composable, `MeasurePolicy`, `Placeable`
- [ ] 🟨  `SubcomposeLayout` — lazy layout that measures children before knowing their sizes
- [ ] 🟨  `BoxWithConstraints` — get available constraints at composition time
- [ ] 🟩  Intrinsic measurements — `IntrinsicSize.Min / Max` for cross-axis sizing
- [ ] 🟨  Compose `Canvas` API — custom drawing in Compose (equivalent to `onDraw`)
- [ ] 🟨  `HorizontalPager` / `VerticalPager` — ViewPager2 equivalent in Compose

#### 3.2.6 Lazy Lists & Grids

- [ ] 🟥 `LazyColumn`, `LazyRow` — equivalent to RecyclerView
- [ ] 🟥 `items(list)`, `itemsIndexed(list)`, `item {}` — DSL for adding content
- [ ] 🟥 `key` parameter — stable identity for items, enables correct animations and state preservation
- [ ] 🟥 `LazyListState` — `rememberLazyListState()`, `firstVisibleItemIndex`, `scrollToItem()`
- [ ] 🟥 `LazyGrid` — `LazyVerticalGrid`, `LazyHorizontalGrid`, `GridCells.Fixed` vs `GridCells.Adaptive`
- [ ] 🟨  `LazyStaggeredGrid` — variable height items
- [ ] 🟨  `StickyHeader` in `LazyListScope`
- [ ] 🟥 Paging 3 integration with `LazyPagingItems` (`collectAsLazyPagingItems()`)

#### 3.2.7 Compose Navigation

- [ ] 🟥 `NavController` + `NavHost` + `composable()` route DSL
- [ ] 🟨  Route strings vs typed routes (Navigation 2.8+ with `@Serializable`)
- [ ] 🟥 Passing arguments: route string parameters, `NavArgument`, `NavType`
- [ ] 🟥 `ViewModel` scoped to nav graph — `hiltViewModel()` vs `viewModel()`
- [ ] 🟥 `BackHandler {}` — intercepting back press
- [ ] 🟨  Deep link integration with Navigation component
- [ ] 🟨  Nested navigation graphs
- [ ] 🟥 `NavController.popBackStack()`, `navigate(route) { launchSingleTop = true; popUpTo() }`

#### 3.2.8 Theming & Material3

- [ ] 🟥 `MaterialTheme` — provides `colorScheme`, `typography`, `shapes` to entire tree via `CompositionLocal`
- [ ] 🟥 `CompositionLocal` — ambient values propagated implicitly down the tree
- [ ] 🟨  `CompositionLocalProvider` — override values for a subtree
- [ ] 🟨  Dynamic color (Material You) — `dynamicDarkColorScheme()`, `dynamicLightColorScheme()`
- [ ] 🟨  `LocalContentColor`, `LocalTextStyle` — inherited styling

#### 3.2.9 Animation

- [ ] 🟥 `animateFloatAsState`, `animateDpAsState`, `animateColorAsState` — simple value animations
- [ ] 🟨  `Animatable` — manual animation control
- [ ] 🟨  `updateTransition` — multiple animated values driven by a single state change
- [ ] 🟥 `AnimatedVisibility` — enter/exit transitions
- [ ] 🟨  `AnimatedContent` — cross-fade and other transitions between content
- [ ] 🟩  `infiniteTransition` — repeating animations
- [ ] 🟨  `SpringSpec`, `TweenSpec`, `KeyframesSpec` — animation specs
- [ ] 🟨  Shared element transitions (Compose 1.7+) — `SharedTransitionLayout`, `sharedElement()`
- [ ] 🟨  Lottie Compose — `LottieAnimation` composable for JSON-based animations

#### 3.2.10 Compose Testing

- [ ] 🟥 `ComposeTestRule` — `createComposeRule()`, `createAndroidComposeRule<Activity>()`
- [ ] 🟥 `onNode(matcher)`, `onNodeWithText()`, `onNodeWithTag()` — finding composables
- [ ] 🟥 `performClick()`, `performTextInput()`, `performScrollTo()`
- [ ] 🟥 Assertions: `assertIsDisplayed()`, `assertTextEquals()`, `assertIsEnabled()`
- [ ] 🟥 Semantics and `testTag` — add `Modifier.testTag("id")` for testability
- [ ] 🟨  `waitUntil {}` — wait for async state changes in tests
- [ ] 🟨  Compose screenshot testing (Paparazzi or Showkase)

#### 3.2.11 Compose & Views Interop

- [ ] 🟥 `AndroidView {}` — embed a View inside Compose
- [ ] 🟥 `ComposeView` — embed Compose inside a legacy View hierarchy
- [ ] 🟨  `AbstractComposeView` — create a reusable Compose-backed View
- [ ] 🟨  Sharing state between View world and Compose world
- [ ] 🟥 `Coil` Compose integration — `AsyncImage {}` composable with `rememberAsyncImagePainter`

---

## PHASE 4 — APP ARCHITECTURE

### Chapter 4.1 — Layered Architecture

#### 4.1.1 MVVM

- [ ] 🟥 `ViewModel` — survives configuration changes, NOT process death (unless SavedStateHandle is used)
- [ ] 🟨  `ViewModelProvider.Factory` — custom VM instantiation with parameters
- [ ] 🟥 `viewModels()` / `activityViewModels()` / `hiltViewModel()` — scope differences
- [ ] 🟥 `SavedStateHandle` — survives process death, read `StateFlow<T?>` from it
- [ ] 🟨  `LiveData` vs `StateFlow` — when to use each, converting with `asLiveData()` / `asFlow()`
- [ ] 🟥 Expose UI state as a single `UiState` sealed class or data class from ViewModel
- [ ] 🟥 `StateFlow` for state, `SharedFlow` (or `Channel`) for one-time events

#### 4.1.2 Clean Architecture

- [ ] 🟥 Three layers: Presentation (UI + ViewModel), Domain (UseCases), Data (Repositories + Sources)
- [ ] 🟥 Dependency rule — inner layers must NOT know about outer layers
- [ ] 🟥 `UseCase` / `Interactor` — encapsulate a single business operation
- [ ] 🟥 `Repository` interface defined in domain layer, implemented in data layer
- [ ] 🟥 Domain models vs data models — map at layer boundaries, never expose Room entities to UI
- [ ] 🟥 Data Source pattern — `RemoteDataSource` and `LocalDataSource` injected into Repository
- [ ] 🟨  Repository pattern with optimistic updates — update UI immediately, rollback on failure

#### 4.1.3 MVI (Model-View-Intent)

- [ ] 🟥 Intent → ViewModel → State cycle — predictable, unidirectional
- [ ] 🟥 Single immutable `UiState` — `copy()` to produce new state
- [ ] 🟥 `UiEvent` (one-time effects) — `Channel<Event>` consumed exactly once
- [ ] 🟨  Orbit MVI, Circuit, or manual implementation — tradeoffs
- [ ] 🟨  When MVI is overkill (simple screens) vs when it shines (complex flows)

### Chapter 4.2 — Multi-Module Architecture

- [ ] 🟥 Why multi-module: build speed (parallel compilation), enforced boundaries, reusability
- [ ] 🟥 Module types: `:app`, `:feature:home`, `:feature:profile`, `:core:ui`, `:core:data`, `:core:network`
- [ ] 🟥 `api` vs `implementation` dependencies in Gradle — `api` exposes transitively, `implementation` does not
- [ ] 🟥 Dependency inversion across modules — feature modules depend on `:core:` not each other
- [ ] 🟨  Navigation between feature modules — shared navigation graph or navigation abstraction
- [ ] 🟨  Convention plugins — `build-logic` module with Gradle plugins to share build config
- [ ] 🟨  `buildSrc` vs `build-logic` included build — prefer included builds for better caching
- [ ] 🟥 Version catalogs (`libs.versions.toml`) — centralized dependency version management

---

## PHASE 5 — DEPENDENCY INJECTION

### Chapter 5.1 — Hilt (Primary)

- [ ] 🟥 `@HiltAndroidApp` — annotates `Application` class; generates component hierarchy
- [ ] 🟥 `@AndroidEntryPoint` — annotates Activity, Fragment, Service, ViewModel for injection
- [ ] 🟥 `@Inject` on constructor — Hilt knows how to build the class without a `@Module`
- [ ] 🟥 `@Module` + `@InstallIn(...)` — declares how to provide instances Hilt cannot construct directly
- [ ] 🟥 `@Provides` — method that returns an instance; use for third-party classes
- [ ] 🟥 `@Binds` — abstract method binding interface to implementation; compile-time efficient
- [ ] 🟥 `@Singleton` — single instance per app component
- [ ] 🟥 `@ActivityScoped`, `@FragmentScoped`, `@ViewModelScoped` — scopes tied to component lifetimes
- [ ] 🟨  `@ActivityRetainedScoped` — survives configuration change but NOT process death
- [ ] 🟥 `@Qualifier` — distinguish between two `@Provides` methods that return the same type
- [ ] 🟥 `@HiltViewModel` — inject ViewModel with Hilt; `hiltViewModel()` in Compose
- [ ] 🟨  Assisted injection (`@AssistedInject`, `@AssistedFactory`) — inject runtime parameters alongside DI
- [ ] 🟨  `EntryPoint` — inject into non-Hilt-managed classes (e.g., `ContentProvider`, `WorkManager`)
- [ ] 🟥 `@TestInstallIn` — replace a module in tests
- [ ] 🟨  `HiltAndroidTest` + `HiltTestApplication` for instrumentation tests
- [ ] 🟨  Component hierarchy: `SingletonComponent` → `ActivityRetainedComponent` → `ViewModelComponent` → `ActivityComponent` → `FragmentComponent`

---

### Chapter 5.2 — Koin (Alternative — Industry Widely Used)

- [ ] 🟥 What Koin is — a pure Kotlin DI framework using DSL, no annotation processing, no code generation
- [ ] 🟥 `startKoin {}` — initialize Koin in `Application.onCreate()`; `androidContext(this)`
- [ ] 🟥 `module {}` — declare a Koin module; `single {}`, `factory {}`, `viewModel {}`
- [ ] 🟥 `single {}` — provides a singleton (one instance for the entire app lifetime)
- [ ] 🟥 `factory {}` — provides a new instance every time it is requested
- [ ] 🟥 `viewModel {}` — provides a ViewModel instance scoped to `ViewModelStore`
- [ ] 🟥 `by inject()` — lazy property delegation for field injection
- [ ] 🟥 `get()` — eager retrieval inside a module or lambda
- [ ] 🟥 `by viewModel()` — delegate to inject ViewModel in Activity/Fragment
- [ ] 🟨  `koinViewModel()` — inject ViewModel in Compose (equivalent to `hiltViewModel()`)
- [ ] 🟥 Parameters with `parametersOf(...)` — pass runtime arguments to `factory` or `viewModel`
- [ ] 🟨  Named qualifiers — `named("qualifier")` to distinguish bindings of the same type
- [ ] 🟨  `scope {}` block — define a custom Koin scope for Activity/Fragment/custom lifecycle
- [ ] 🟨  `KoinComponent` interface — inject into classes that are not Activities/Fragments
- [ ] 🟥 Koin vs Hilt — Koin: no code gen, slower at runtime (reflection-based); Hilt: compile-time safety, faster runtime
- [ ] 🟨  Multi-module Koin — `loadKoinModules()`, `unloadKoinModules()` for dynamic feature modules
- [ ] 🟥 Testing with Koin — `KoinTest`, `declare {}` to override bindings in tests, `stopKoin()` in teardown
- [ ] 🟨  `androidLogger()` — Koin's built-in Android logger for debug logging
- [ ] 🟩  `checkModules {}` — verify all dependencies are satisfied at compile/test time

---

### Chapter 5.3 — Dagger 2 Internals (Understanding the Foundation)

- [ ] 🟨  `@Component` — the bridge between dependency graph and injection target
- [ ] 🟨  `@Subcomponent` — child component that inherits parent bindings
- [ ] 🟨  `@Component.Builder` and `@Component.Factory`
- [ ] 🟨  Dagger-generated code — understand what `DaggerAppComponent.java` looks like
- [ ] 🟥 Why Hilt exists — Hilt standardizes Dagger component hierarchy for Android
- [ ] 🟩  `Lazy<T>` injection — defer instantiation until first use

---

## PHASE 6 — DATA LAYER

### Chapter 6.1 — Room Database

- [ ] 🟥 `@Entity` — maps to a database table; `tableName`, `indices`, `foreignKeys`
- [ ] 🟥 `@PrimaryKey` — `autoGenerate = true` for auto-increment
- [ ] 🟨  `@ColumnInfo` — custom column name, `defaultValue`
- [ ] 🟨  `@Ignore` — exclude field from table
- [ ] 🟨  `@Embedded` — inline a nested object's columns into the parent table
- [ ] 🟥 `@Relation` — define relationships; use with `@Transaction` to avoid partial reads
- [ ] 🟥 One-to-one, one-to-many, many-to-many relationships
- [ ] 🟨  `@Junction` — junction entity for many-to-many
- [ ] 🟥 `@Dao` — annotated interface or abstract class for database operations
- [ ] 🟥 `@Query`, `@Insert`, `@Update`, `@Delete`, `@Upsert` (Room 2.5+)
- [ ] 🟥 `@Transaction` — wrap multiple operations in a single transaction
- [ ] 🟥 Returning `Flow<T>` from DAO — Room emits new values on database change
- [ ] 🟥 Returning `suspend` functions from DAO — use with coroutines
- [ ] 🟨  `@TypeConverter` — convert non-primitive types (e.g., `Date`, `List<String>`)
- [ ] 🟥 `RoomDatabase.Builder` — `addMigrations()`, `fallbackToDestructiveMigration()`
- [ ] 🟥 `Migration(fromVersion, toVersion)` — write `ALTER TABLE` or `CREATE TABLE` SQL
- [ ] 🟨  `AutoMigration` (Room 2.4+) — schema-driven migrations with `@AutoMigration`
- [ ] 🟨  Database inspector in Android Studio
- [ ] 🟥 Testing Room: `inMemoryDatabaseBuilder`, `InstantTaskExecutorRule`

### Chapter 6.2 — DataStore

- [ ] 🟥 `Preferences DataStore` — key-value, no schema, uses `Preferences.Key<T>`
- [ ] 🟨  `Proto DataStore` — typed, schema-defined (`.proto` file), safer for complex data
- [ ] 🟥 `DataStore<Preferences>` creation: `preferencesDataStore` property delegate (singleton per file)
- [ ] 🟥 `data: Flow<Preferences>` — reactive reads
- [ ] 🟥 `edit {}` — transactional writes (atomic, handles write failures)
- [ ] 🟥 `IOExceptions` — handle in collect with `catch`
- [ ] 🟥 Why `DataStore` replaces `SharedPreferences` — handles async correctly, no ANRs on main thread
- [ ] 🟥 Do NOT create multiple instances for the same file — use DI to provide a single instance

### Chapter 6.3 — Paging 3

- [ ] 🟥 `PagingSource<Key, Value>` — implement `load()` to return `LoadResult.Page` or `LoadResult.Error`
- [ ] 🟥 `RemoteMediator<Key, Value>` — load from network and cache to Room; offline-first paging
- [ ] 🟥 `Pager` — builds `PagingData` flow with `pageSize`, `prefetchDistance`, `initialLoadSize`
- [ ] 🟥 `cachedIn(viewModelScope)` — caches `PagingData` in ViewModel to survive recomposition
- [ ] 🟥 `LazyPagingItems` in Compose (`collectAsLazyPagingItems()`)
- [ ] 🟥 Load states: `LoadState.Loading`, `LoadState.NotLoading`, `LoadState.Error`
- [ ] 🟨  `PagingDataAdapter` (Views) vs `LazyPagingItems` (Compose)
- [ ] 🟨  `invalidate()` — force a full refresh of the `PagingSource`

### Chapter 6.4 — Offline-First Architecture

- [ ] 🟥 Single source of truth: Room is the source of truth; network responses written to Room, UI reads from Room
- [ ] 🟥 Network-bound resource pattern: `emit(Loading)` → fetch network → save to DB → `emit(db.flow())`
- [ ] 🟨  Conflict resolution strategies — timestamp-based, server-wins, last-write-wins
- [ ] 🟨  Sync policies — when to sync (on demand, periodic, on connectivity restored)
- [ ] 🟥 `RemoteMediator` as the recommended Paging 3 offline-first pattern

---

## PHASE 7 — NETWORKING

### Chapter 7.1 — OkHttp

- [ ] 🟥 `OkHttpClient.Builder()` — configure timeouts, interceptors, authenticator, cache
- [ ] 🟥 `Interceptor` — modify request/response; `Chain.proceed(request)` for passthrough
- [ ] 🟥 `Application Interceptor` vs `Network Interceptor` — application interceptors see redirects as one call
- [ ] 🟥 `HttpLoggingInterceptor` — log request/response bodies (never in release builds)
- [ ] 🟥 `Authenticator` — refresh token and retry request automatically on 401
- [ ] 🟨  `Cache(directory, maxSize)` — HTTP caching; `Cache-Control` header handling
- [ ] 🟨  `ConnectionPool` — reuses TCP connections; configure `maxIdleConnections` and `keepAliveDuration`
- [ ] 🟨  `CertificatePinner` — pin specific certificates for endpoints
- [ ] 🟨  `SSLSocketFactory` + `TrustManager` — custom TLS configuration

### Chapter 7.2 — Retrofit

- [ ] 🟥 `@GET`, `@POST`, `@PUT`, `@DELETE`, `@PATCH`, `@HEAD` annotations
- [ ] 🟥 `@Path`, `@Query`, `@QueryMap`, `@Body`, `@Header`, `@HeaderMap`, `@Field`, `@FieldMap`, `@Part`
- [ ] 🟨  `@Multipart` and `@FormUrlEncoded`
- [ ] 🟥 `suspend` function return types — Retrofit 2.6+ supports coroutines natively
- [ ] 🟥 `Response<T>` wrapper — access `isSuccessful`, `code()`, `errorBody()`
- [ ] 🟨  `Call<T>` for non-coroutine (legacy) usage
- [ ] 🟥 Converters: `MoshiConverterFactory`, `GsonConverterFactory`, `KotlinSerializationConverterFactory`
- [ ] 🟨  `CallAdapter` — custom result types (e.g., `NetworkResult<T>` sealed class)
- [ ] 🟨  `@Url` dynamic URLs — override base URL per request

### Chapter 7.3 — JSON Serialization

- [ ] 🟥 **Moshi** — Kotlin-first; `@JsonClass(generateAdapter = true)`, `@Json(name = "field_name")`
- [ ] 🟥 **kotlinx.serialization** — compile-time code generation, `@Serializable`, `Json.decodeFromString()`
- [ ] 🟨  **Gson** — Java-based, reflection-heavy, R8 compatibility issues (avoid in new projects)
- [ ] 🟨  Custom adapters in Moshi — `@ToJson` / `@FromJson`
- [ ] 🟨  Null handling strategies, unknown keys, default values

### Chapter 7.4 — Authentication & Security

- [ ] 🟥 JWT structure — header.payload.signature, decoding without verification, expiry (`exp` claim)
- [ ] 🟥 Token storage: `EncryptedSharedPreferences` or `Keystore` — NEVER plain SharedPreferences
- [ ] 🟥 Token refresh flow: intercept 401 → acquire new token (with mutex) → retry request
- [ ] 🟨  OAuth 2.0 flow: Authorization Code + PKCE for mobile apps (via `AppAuth` library)
- [ ] 🟥 `Mutex` in coroutines for token refresh — `withLock {}` prevents concurrent refresh calls

### Chapter 7.5 — Error Handling & Resilience

- [ ] 🟥 Sealed class `NetworkResult<T>` — `Success(data)`, `Error(code, message)`, `Exception(e)`
- [ ] 🟥 `runCatching {}` — Kotlin's `Result<T>` for exception wrapping
- [ ] 🟨  Exponential backoff — retry delays: 1s, 2s, 4s, 8s with jitter
- [ ] 🟨  `OkHttp` retry on connection failure
- [ ] 🟥 Graceful degradation — show stale data when network unavailable
- [ ] 🟥 `ConnectivityManager` + `NetworkCallback` — observe network availability reactively

### Chapter 7.6 — WebSockets & Real-Time

- [ ] 🟨  `OkHttpClient.newWebSocket(request, listener)` — open a WebSocket
- [ ] 🟨  `WebSocketListener` — `onOpen`, `onMessage`, `onClosing`, `onClosed`, `onFailure`
- [ ] 🟨  Reconnection strategies — exponential backoff on `onFailure`
- [ ] 🟨  Converting WebSocket messages to a `Flow<String>` using `callbackFlow {}`

### Chapter 7.7 — GraphQL & gRPC (Modern Protocols)

- [ ] 🟨  **Apollo Kotlin** — GraphQL client; `@Query`, `@Mutation`, `@Subscription` codegen from `.graphql` files
- [ ] 🟨  Apollo cache — `InMemoryNormalizedCache`, `SqlNormalizedCacheFactory` for offline
- [ ] 🟩  **gRPC-Kotlin** — protobuf over HTTP/2; `proto` files, generated stubs, `ManagedChannel`
- [ ] 🟩  gRPC vs REST — streaming support, binary protocol, schema-first
- [ ] 🟩  **MQTT** — lightweight publish/subscribe for IoT; `Eclipse Paho` Android client

---

## PHASE 8 — BACKGROUND WORK & CONCURRENCY

### Chapter 8.1 — WorkManager

- [ ] 🟥 `OneTimeWorkRequest` and `PeriodicWorkRequest` — one-off vs recurring tasks
- [ ] 🟥 `WorkRequest.Builder` — `setConstraints()`, `setBackoffCriteria()`, `setInitialDelay()`, `addTag()`
- [ ] 🟥 `Constraints.Builder()` — `requiresNetworkType`, `requiresBatteryNotLow()`, `requiresCharging()`
- [ ] 🟥 `doWork()` in `CoroutineWorker` — runs on `Dispatchers.IO`, return `Result.success()`, `Result.failure()`, `Result.retry()`
- [ ] 🟨  `setForeground(ForegroundInfo)` in `CoroutineWorker` — run as foreground for long tasks
- [ ] 🟨  `setExpedited()` — expedited work for high-priority one-off tasks
- [ ] 🟥 Work chaining — `then()` sequential, `combine()` parallel
- [ ] 🟨  `WorkContinuation` — managing complex chains
- [ ] 🟥 Unique work — `enqueueUniqueWork()` with `ExistingWorkPolicy` (`KEEP`, `REPLACE`, `APPEND`)
- [ ] 🟥 `WorkManager.observe(id)` — observe `WorkInfo` state as `LiveData` or `Flow`
- [ ] 🟨  `WorkInfo.State` — `ENQUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `BLOCKED`, `CANCELLED`
- [ ] 🟨  `setInputData(Data)` / `outputData` — pass data between workers
- [ ] 🟥 WorkManager vs `AlarmManager` — WorkManager is battery-aware and deferred
- [ ] 🟨  Periodic work minimum interval — 15 minutes (OS restriction on Android)
- [ ] 🟨  Testing: `TestWorkerBuilder`, `TestListenableWorkerBuilder`

### Chapter 8.2 — AlarmManager

- [ ] 🟨  `setExact()`, `setExactAndAllowWhileIdle()` — precise timing, Doze-aware
- [ ] 🟨  `setWindow()` — approximate timing with OS flexibility
- [ ] 🟥 `PendingIntent` with `BroadcastReceiver` for alarm delivery
- [ ] 🟨  `BOOT_COMPLETED` receiver — reschedule alarms after reboot
- [ ] 🟥 `SCHEDULE_EXACT_ALARM` permission (Android 12+) — required for `setExact()`
- [ ] 🟨  When to use AlarmManager: calendar reminders, user-configured exact time notifications

### Chapter 8.3 — Foreground Services vs WorkManager

- [ ] 🟥 Foreground service: immediate start, visible to user (notification), not deferred
- [ ] 🟥 WorkManager: deferred, batched by OS, survives process death and reboots
- [ ] 🟥 Music playback → Foreground Service; file upload → WorkManager with `setForeground`
- [ ] 🟥 Android 12+ foreground service restrictions — must specify `foregroundServiceType`
- [ ] 🟥 Android 14+ restrictions — most foreground service types require declared permission

---

## PHASE 9 — PERFORMANCE ENGINEERING

### Chapter 9.1 — Memory

- [ ] 🟥 Memory leak anatomy — GC root → reference chain → leaked object
- [ ] 🟥 Common leak patterns: Activity/Fragment context in static fields, anonymous inner classes, non-cancelled coroutines
- [ ] 🟥 `LeakCanary` — automatic leak detection in debug builds; reading HeapDump analysis
- [ ] 🟥 Android Studio Memory Profiler — heap dump, allocation tracking, memory timeline
- [ ] 🟨  `ActivityManager.getMemoryClass()` — per-app heap limit in MB
- [ ] 🟩  Large Heap (`android:largeHeap="true"`) — use only if justified, increases GC pressure
- [ ] 🟨  Bitmap memory — `BitmapFactory.Options.inSampleSize`, `inBitmap` for reuse
- [ ] 🟥 `Coil` / `Glide` — image loading with automatic bitmap pooling and caching

### Chapter 9.2 — CPU & Rendering

- [ ] 🟥 Systrace / Perfetto — system-wide CPU trace; find jank, identify slow methods
- [ ] 🟥 Android Studio CPU Profiler — method traces, flame charts
- [ ] 🟥 16ms budget per frame at 60fps; 11.1ms at 90fps; 8.3ms at 120fps
- [ ] 🟨  `Choreographer` — frame callback scheduling
- [ ] 🟨  `FrameMetricsAggregator` / `JankStats` — measure UI smoothness in production
- [ ] 🟥 `StrictMode.setThreadPolicy` and `VmPolicy` — detect disk/network on main thread in debug
- [ ] 🟨  Overdraw — GPU renders pixels multiple times; use "Debug GPU overdraw" in developer options
- [ ] 🟨  Layout hierarchy depth — flatten layouts to reduce measure/layout passes
- [ ] 🟩  `ViewTreeObserver.addOnPreDrawListener` for custom drawing optimization
- [ ] 🟨  Hardware acceleration layers — `View.setLayerType(LAYER_TYPE_HARDWARE, null)` for animating views
- [ ] 🟨  `AndroidX Tracing` — `Trace.beginSection()` for custom Perfetto slices

### Chapter 9.3 — App Startup

- [ ] 🟥 Cold start vs warm start vs hot start — cold is most expensive (Zygote fork + DEX load + Application.onCreate)
- [ ] 🟨  `App Startup` library — lazy initialization of components in `Application.onCreate()`
- [ ] 🟨  `Initializer<T>` interface — define initialization order with `dependencies()`
- [ ] 🟥 Baseline Profiles — pre-compile hot code paths; generated with `Macrobenchmark`
- [ ] 🟨  `ProfileInstaller` library — install baseline profiles from APK/AAB
- [ ] 🟥 `Macrobenchmark` — measure cold start, frame timing in CI
- [ ] 🟨  `MicrobenchmarkRule` — micro-level performance benchmarks for individual functions
- [ ] 🟥 Reduce `Application.onCreate()` work — defer non-critical initialization
- [ ] 🟥 `SplashScreen` API (Android 12+) — proper splash screen implementation

### Chapter 9.4 — Network & Battery

- [ ] 🟥 `ConnectivityManager.NetworkCallback` — react to connectivity changes
- [ ] 🟨  Batching network requests — avoid many small requests
- [ ] 🟨  `HTTP/2` multiplexing via OkHttp — single connection for multiple requests
- [ ] 🟥 Doze mode and App Standby — restrictions on background network and wakelock
- [ ] 🟨  Battery Historian — analyze battery usage offline
- [ ] 🟨  `PowerManager.isInteractive()` — check if screen is on before starting work
- [ ] 🟨  Wakelocks — `PARTIAL_WAKE_LOCK`; avoid entirely where WorkManager can substitute

---

## PHASE 10 — TESTING MASTERY

### Chapter 10.1 — Unit Testing

- [ ] 🟥 JUnit 4 vs JUnit 5 — Android currently defaults to JUnit 4; JUnit 5 requires plugin
- [ ] 🟥 `@Test`, `@Before`, `@After`, `@BeforeClass`, `@AfterClass`
- [ ] 🟥 `assertEquals`, `assertTrue`, `assertThrows`, `assertNull`
- [ ] 🟥 **MockK** — Kotlin-native mocking: `mockk<T>()`, `every { } returns`, `coEvery { } returns`
- [ ] 🟥 `relaxed = true` parameter — auto-stubs all calls
- [ ] 🟨  `slot<T>()` — capture argument values for assertion
- [ ] 🟨  `spyk {}` — partial mock wrapping real objects
- [ ] 🟨  `mockkObject(SomeObject)` — mock Kotlin singletons
- [ ] 🟨  `mockkStatic(...)` — mock Kotlin extension functions or Java static methods
- [ ] 🟨  **Mockito** — Java mocking, less idiomatic for Kotlin
- [ ] 🟨  **Kotest** — Kotlin-native test framework; `shouldBe`, `shouldThrow`, behavior-driven specs
- [ ] 🟩  `Robolectric` — run Android unit tests on JVM without emulator

### Chapter 10.2 — Coroutine Testing

- [ ] 🟥 `runTest { }` — virtual time, auto-advances delays
- [ ] 🟥 `StandardTestDispatcher` — does NOT auto-advance; `advanceUntilIdle()` required
- [ ] 🟥 `UnconfinedTestDispatcher` — immediately executes; use for simple state collection tests
- [ ] 🟨  `TestScope` — provides `backgroundScope` for non-test coroutines
- [ ] 🟥 Injecting dispatchers — constructor-inject `CoroutineDispatcher` and replace with `TestDispatcher`
- [ ] 🟥 Testing `Flow` emissions — use `turbine` library: `flow.test { assertThat(awaitItem()) }`

### Chapter 10.3 — ViewModel & Repository Testing

- [ ] 🟨  `InstantTaskExecutorRule` — makes `LiveData` synchronous in tests
- [ ] 🟥 Testing `StateFlow` — `viewModel.uiState.value` after calling action
- [ ] 🟥 Fake vs Mock — prefer `Fake` implementations for repositories over mocks for complex tests
- [ ] 🟥 Test doubles taxonomy: Dummy, Stub, Fake, Mock, Spy — know the difference
- [ ] 🟥 `FakeRepository` pattern — in-memory implementation backed by a `MutableList`

### Chapter 10.4 — Instrumentation & UI Testing

- [ ] 🟥 `@RunWith(AndroidJUnit4::class)` — instrumentation test runner
- [ ] 🟥 `ActivityScenario.launch<MyActivity>()` — launch Activity in test
- [ ] 🟥 **Espresso** — `onView(withId(R.id.button))`, `perform(click())`, `check(matches(isDisplayed()))`
- [ ] 🟨  `IdlingResource` — tell Espresso to wait for async operations
- [ ] 🟥 **Compose Test** — `createComposeRule()`, `onNodeWithText()`, `performClick()`, `assertIsDisplayed()`
- [ ] 🟥 `MockWebServer` (OkHttp) — fake HTTP server for integration tests
- [ ] 🟨  **UIAutomator** — cross-app testing; `UiDevice`, `UiObject2`, `By.res()`, `By.text()`

### Chapter 10.5 — Screenshot & Advanced Testing

- [ ] 🟨  **Paparazzi** — screenshot testing without emulator; fast, CI-friendly
- [ ] 🟨  **Maestro** — mobile UI testing framework using simple YAML flows; alternative to Espresso
- [ ] 🟩  **Showkase** — Compose component browser + snapshot testing
- [ ] 🟨  Snapshot testing strategies — golden files, tolerance thresholds

### Chapter 10.6 — CI/CD

- [ ] 🟥 GitHub Actions — workflow `.yml` syntax, `actions/checkout`, `actions/setup-java`, `gradle test`
- [ ] 🟥 Run unit tests: `./gradlew test`; instrumentation tests: requires emulator or Firebase Test Lab
- [ ] 🟥 `./gradlew lint` — static analysis; `lint.xml` for configuration
- [ ] 🟥 KtLint / Detekt — Kotlin code style and static analysis
- [ ] 🟨  **Kover** — JetBrains Kotlin code coverage (modern alternative to Jacoco)
- [ ] 🟨  Jacoco — code coverage reports; `jacocoTestReport` task
- [ ] 🟨  Firebase Test Lab — run instrumentation tests on real devices in CI

---

## PHASE 11 — SECURITY & PRIVACY

### Chapter 11.1 — Secure Storage

- [ ] 🟥 Android Keystore System — hardware-backed (TEE/StrongBox) key storage; keys never leave hardware
- [ ] 🟨  `KeyPairGenerator` / `KeyGenerator` with `KeyGenParameterSpec` — create keys in Keystore
- [ ] 🟥 `EncryptedSharedPreferences` (Jetpack Security) — wraps SharedPreferences with Keystore encryption
- [ ] 🟨  `EncryptedFile` — encrypt files using Keystore-derived keys
- [ ] 🟥 What NOT to do: hardcode keys, store tokens in plain `SharedPreferences`, store sensitive data in plain files

### Chapter 11.2 — Network Security

- [ ] 🟥 TLS — all network communication must use HTTPS
- [ ] 🟨  `network_security_config.xml` — `<domain-config>`, `<trust-anchors>`, `<pin-set>`
- [ ] 🟨  Certificate pinning: pin the leaf cert OR intermediate cert; rotate before expiry
- [ ] 🟥 Cleartext traffic — `android:usesCleartextTraffic="false"` in manifest
- [ ] 🟩  Certificate Transparency

### Chapter 11.3 — Runtime Permissions

- [ ] 🟥 Normal vs Dangerous vs Signature permissions
- [ ] 🟥 `ActivityResultContracts.RequestPermission()` and `RequestMultiplePermissions()` — modern API
- [ ] 🟥 `shouldShowRequestPermissionRationale()` — show explanation UI if true
- [ ] 🟥 Permanently denied detection — if false AND `checkSelfPermission` is DENIED
- [ ] 🟨  `ACTION_APPLICATION_DETAILS_SETTINGS` — direct user to settings for permanently denied
- [ ] 🟥 Android 12+ Bluetooth permissions: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`
- [ ] 🟥 Android 13+ media permissions: `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`
- [ ] 🟥 Android 14: `READ_MEDIA_VISUAL_USER_SELECTED` — partial media access

### Chapter 11.4 — Data Privacy & Scoped Storage

- [ ] 🟥 Scoped storage (Android 10+) — use `MediaStore` for shared media, SAF for user-chosen files
- [ ] 🟨  `Storage Access Framework (SAF)` — `ACTION_OPEN_DOCUMENT`, `ACTION_CREATE_DOCUMENT`
- [ ] 🟥 `MediaStore` API — query, insert, update media
- [ ] 🟨  GDPR and data minimization — collect only what you need, purge on request
- [ ] 🟨  `Privacy Dashboard` (Android 12+) — shows recent permission usage to users
- [ ] 🟥 `Data safety section` on Play Console — declare what data you collect and why
- [ ] 🟥 **Photo Picker** (Android 13+) — `PickVisualMedia` contract; no permissions required; replaces `READ_MEDIA_IMAGES` for media selection

### Chapter 11.5 — Code Security

- [ ] 🟥 R8 — full-mode obfuscation, shrinking, and optimization
- [ ] 🟥 `proguard-rules.pro` — `-keep` rules for reflection, Gson/Moshi models, Parcelable, JNI
- [ ] 🟥 Mapping files — always save `mapping.txt` for each release; required for Crashlytics deobfuscation
- [ ] 🟥 `android:debuggable="false"` — must be false in release builds
- [ ] 🟨  Detecting root — `RootBeer` library for basic root detection
- [ ] 🟨  Preventing backup of sensitive data — `android:allowBackup="false"`
- [ ] 🟨  SSL/TLS version enforcement

### Chapter 11.6 — Play Integrity & Anti-Tamper

- [ ] 🟥 **Play Integrity API** — replaces SafetyNet; verifies device integrity, app integrity, account licensing
- [ ] 🟥 `IntegrityTokenProvider` — `requestIntegrityToken()` and send to backend for verification
- [ ] 🟨  Verdict types: `MEETS_DEVICE_INTEGRITY`, `MEETS_BASIC_INTEGRITY`, `MEETS_STRONG_INTEGRITY`
- [ ] 🟨  **Firebase AppCheck** — protect backend APIs from abuse; uses Play Integrity under the hood
- [ ] 🟩  App hibernation (Android 12+) — permissions reset, impact on background work and notifications

---

## PHASE 12 — BUILD & RELEASE

### Chapter 12.1 — Gradle Deep Dive

- [ ] 🟥 Gradle lifecycle: initialization → configuration → execution
- [ ] 🟥 `settings.gradle.kts` — declare submodules
- [ ] 🟥 `build.gradle.kts` (module level) — `plugins {}`, `android {}`, `dependencies {}`
- [ ] 🟥 `android {}` block: `compileSdk`, `minSdk`, `targetSdk`, `defaultConfig`, `buildTypes`, `productFlavors`
- [ ] 🟥 `buildTypes` — `debug` (debuggable, not obfuscated) vs `release` (minified, signed)
- [ ] 🟨  `productFlavors` — different API endpoints, app IDs, or feature sets per variant
- [ ] 🟨  `flavorDimensions` — combine multiple dimensions
- [ ] 🟨  Build variant = buildType + productFlavor combination
- [ ] 🟥 `versionCode` and `versionName` — automating with CI build number
- [ ] 🟥 Dependency configurations: `implementation`, `api`, `testImplementation`, `androidTestImplementation`, `ksp`, `kapt`
- [ ] 🟨  Gradle caching — local build cache, remote build cache in CI
- [ ] 🟨  Configuration cache — `--configuration-cache` flag
- [ ] 🟨  Convention plugins — writing custom Gradle plugins in Kotlin in `build-logic`

### Chapter 12.2 — R8 / ProGuard

- [ ] 🟥 R8 is the default minifier in AGP; ProGuard rules are compatible
- [ ] 🟥 Shrinking — removes unused classes and methods
- [ ] 🟥 Obfuscation — renames classes/methods/fields to single letters
- [ ] 🟨  Optimization — inlines methods, removes dead code branches
- [ ] 🟥 `proguard-rules.pro` common rules: `-keep class * implements android.os.Parcelable`
- [ ] 🟨  `@Keep` annotation — alternative to `-keep` rules
- [ ] 🟥 Testing release builds — always test APK/AAB with `minifyEnabled true` before submitting

### Chapter 12.3 — App Signing & Distribution

- [ ] 🟥 Keystore — JKS or PKCS12 format; `keytool -genkeypair`
- [ ] 🟥 `signingConfigs` in `build.gradle.kts` — reference keystore in release build type
- [ ] 🟥 Store keystore credentials in `local.properties` or environment variables — NEVER in version control
- [ ] 🟥 Play App Signing — Google stores your app signing key; you upload with an upload key
- [ ] 🟥 AAB vs APK — AAB is required for Play Store
- [ ] 🟨  Dynamic delivery — install-time vs on-demand feature modules

### Chapter 12.4 — Play Console & Production

- [ ] 🟥 Internal testing → Closed testing (alpha) → Open testing (beta) → Production
- [ ] 🟥 Staged rollout — `0.1%` → `1%` → `5%` → `20%` → `50%` → `100%`; halt if crash rate spikes
- [ ] 🟨  Store listing: short description, full description, screenshots, feature graphic, promo video
- [ ] 🟨  App content rating — IARC questionnaire
- [ ] 🟨  Pre-launch reports — Google runs automated tests on Firebase Test Lab devices
- [ ] 🟥 `Play Core Library` — in-app updates (`AppUpdateManager`), in-app reviews (`ReviewManager`)

### Chapter 12.5 — Crash Reporting & Analytics

- [ ] 🟥 `Firebase Crashlytics` — `FirebaseCrashlytics.getInstance()`, `recordException()`, custom keys
- [ ] 🟨  NDK crash reporting — add `com.google.firebase:firebase-crashlytics-ndk`
- [ ] 🟥 Mapping file upload — `uploadCrashlyticsSymbolFile` Gradle task
- [ ] 🟥 Non-fatal exceptions — `Crashlytics.recordException(e)` for handled errors
- [ ] 🟥 `Firebase Analytics` — `logEvent()`, custom parameters, audiences, funnels
- [ ] 🟨  `Firebase Performance Monitoring` — automatic HTTP traces, custom traces
- [ ] 🟨  `Remote Config` — server-side feature flags, A/B testing

---

## PHASE 13 — ACCESSIBILITY & INCLUSIVITY

- [ ] 🟥 TalkBack — screen reader; test every screen with TalkBack enabled
- [ ] 🟥 `contentDescription` — required for all images and icons; must be meaningful
- [ ] 🟨  `importantForAccessibility` — `yes`, `no`, `noHideDescendants`, `auto`
- [ ] 🟥 `Modifier.semantics {}` in Compose — `contentDescription`, `role`, `stateDescription`, `heading()`
- [ ] 🟥 `Role` — `Button`, `Checkbox`, `Switch`, `Image`, `Tab`, `RadioButton`
- [ ] 🟨  Focus order — `traversalIndex`, `isTraversalGroup` in Compose for custom ordering
- [ ] 🟥 Minimum touch target size — 48dp × 48dp (Material guideline)
- [ ] 🟥 Color contrast ratio — AA standard: 4.5:1 for normal text, 3:1 for large text
- [ ] 🟥 Do not convey information by color alone — add icons or text labels
- [ ] 🟥 Dynamic type — `sp` units for text, support font size scaling
- [ ] 🟨  Accessibility Scanner — Google's app that audits accessibility issues
- [ ] 🟨  `ViewCompat.setAccessibilityDelegate()` — customize accessibility in custom Views

---

## PHASE 14 — ADVANCED DEVICE APIS

### Chapter 14.1 — Camera

- [ ] 🟥 `CameraX` — Jetpack library abstracting Camera2; use cases: `Preview`, `ImageCapture`, `VideoCapture`, `ImageAnalysis`
- [ ] 🟥 `ProcessCameraProvider` — binds camera to lifecycle
- [ ] 🟨  `CameraSelector` — front vs back camera
- [ ] 🟥 `ImageAnalysis.Analyzer` — per-frame analysis (ML, QR codes)
- [ ] 🟨  `ImageCapture.takePicture()` — `OutputFileOptions` for file or in-memory
- [ ] 🟨  `VideoCapture<Recorder>` with `Recorder.prepareRecording()`
- [ ] 🟩  `Camera2` — lower-level; `CameraManager`, `CameraDevice`, `CaptureSession`, manual focus/exposure
- [ ] 🟩  `CameraCharacteristics` — query camera capabilities (HDR, RAW, supported modes)

### Chapter 14.2 — Location

- [ ] 🟥 `FusedLocationProviderClient` — `getLastLocation()`, `requestLocationUpdates()`
- [ ] 🟥 `LocationRequest.Builder` — `priority`, `intervalMillis`, `minUpdateIntervalMillis`
- [ ] 🟨  `LocationCallback` vs `PendingIntent` for background location
- [ ] 🟥 `ACCESS_FINE_LOCATION` vs `ACCESS_COARSE_LOCATION` vs `ACCESS_BACKGROUND_LOCATION`
- [ ] 🟥 Android 11+ background location — must be granted separately; redirect to system settings
- [ ] 🟨  Geofencing: `GeofencingClient.addGeofences()`, `GeofenceRequest`, `GeofencingEvent`
- [ ] 🟨  Maps: `Google Maps SDK`, `Jetpack Maps Compose` (`GoogleMap {}` composable)

### Chapter 14.3 — Bluetooth

- [ ] 🟨  Classic Bluetooth vs BLE (Bluetooth Low Energy) — different APIs and use cases
- [ ] 🟨  BLE scanning: `BluetoothLeScanner.startScan()`, `ScanFilter`, `ScanSettings`
- [ ] 🟨  GATT (Generic Attribute Profile): services, characteristics, descriptors
- [ ] 🟨  `BluetoothGatt` — `connectGatt()`, `discoverServices()`, `readCharacteristic()`, `writeCharacteristic()`
- [ ] 🟨  Notifications/Indications — subscribe to characteristic changes
- [ ] 🟥 Android 12+ permissions: `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT`; `neverForLocation` flag

### Chapter 14.4 — Biometrics

- [ ] 🟥 `BiometricPrompt` — fingerprint, face, iris; single API for all modalities
- [ ] 🟥 `BiometricManager.canAuthenticate(authenticators)` — check what's available
- [ ] 🟥 `Authenticators.BIOMETRIC_STRONG` vs `BIOMETRIC_WEAK` vs `DEVICE_CREDENTIAL`
- [ ] 🟨  `BiometricPrompt.CryptoObject` — tie authentication to a Keystore key operation
- [ ] 🟨  `PromptInfo.Builder` — title, subtitle, description, negative button text
- [ ] 🟥 **Credential Manager API** (Android 14+) — unified API for passkeys, passwords, federated sign-in; replaces SmartLock

### Chapter 14.5 — Push Notifications (FCM)

- [ ] 🟥 Firebase Cloud Messaging setup — `google-services.json`, `FirebaseMessagingService`
- [ ] 🟥 `onMessageReceived(RemoteMessage)` — handle data messages and notification messages
- [ ] 🟥 `onNewToken(token)` — send new FCM token to backend
- [ ] 🟥 Notification channels (`NotificationChannel`) — required for Android 8+
- [ ] 🟥 `NotificationCompat.Builder` — `setSmallIcon`, `setContentTitle`, `setContentText`, `setPriority`, `setAutoCancel`, `setContentIntent(PendingIntent)`
- [ ] 🟥 Foreground notification — required for foreground services
- [ ] 🟨  `NotificationManagerCompat.notify()` vs `NotificationManager`
- [ ] 🟨  Topics subscriptions, device groups, upstream messages
- [ ] 🟥 `POST_NOTIFICATIONS` permission (Android 13+) — must request at runtime

### Chapter 14.6 — Widgets & Shortcuts

- [ ] 🟨  `AppWidgetProvider` — `onUpdate()`, `onEnabled()`, `AppWidgetManager`
- [ ] 🟨  Glance (Jetpack) — Compose-based widgets (`GlanceAppWidget`, `GlanceAppWidgetReceiver`)
- [ ] 🟨  `RemoteViews` — limited View subset for widget layouts
- [ ] 🟨  Dynamic shortcuts: `ShortcutManagerCompat.pushDynamicShortcut()`
- [ ] 🟩  Pinned shortcuts on launcher

### Chapter 14.7 — Media3 / ExoPlayer (Critical for Media Apps)

- [ ] 🟥 **Media3** — Jetpack unification of ExoPlayer, MediaSession, Transformer
- [ ] 🟥 `ExoPlayer` setup — `ExoPlayer.Builder(context).build()`, `MediaItem.fromUri()`
- [ ] 🟥 `Player.Listener` — `onPlaybackStateChanged`, `onPlayerError`, `onIsPlayingChanged`
- [ ] 🟥 `PlayerView` (XML) vs `AndroidExternalSurface` / `PlayerSurface` in Compose
- [ ] 🟥 Background playback — `MediaSessionService` + `MediaSession` + notification
- [ ] 🟨  `MediaSource` types — `ProgressiveMediaSource`, `HlsMediaSource`, `DashMediaSource`
- [ ] 🟨  Custom `DataSource.Factory` — for authenticated streams, cookies, headers
- [ ] 🟨  `ConcatenatingMediaSource` / `MediaItem` playlist — queue management
- [ ] 🟩  DRM — `DefaultDrmSessionManager`, Widevine L1/L3 for protected content
- [ ] 🟨  `Transformer` — media transcoding, trimming, applying effects
- [ ] 🟥 `MediaPlayer` (legacy) vs `ExoPlayer` — when to use which (prefer ExoPlayer always)

### Chapter 14.8 — NFC & Sensors

- [ ] 🟩  NFC — `NfcAdapter`, NDEF messages, `ACTION_NDEF_DISCOVERED`, `ACTION_TAG_DISCOVERED`
- [ ] 🟩  HCE (Host Card Emulation) — emulate NFC cards on Android
- [ ] 🟨  `SensorManager` — `TYPE_ACCELEROMETER`, `TYPE_GYROSCOPE`, `TYPE_STEP_DETECTOR`, `TYPE_STEP_COUNTER`
- [ ] 🟨  `SensorEventListener` — `onSensorChanged()`, `onAccuracyChanged()`
- [ ] 🟩  `SpeechRecognizer` — on-device speech to text
- [ ] 🟩  `TextToSpeech` — on-device text to speech

---

## PHASE 15 — KOTLIN MULTIPLATFORM & MODERN ECOSYSTEM

### Chapter 15.1 — Kotlin Multiplatform (KMP)

- [ ] 🟨  KMP project structure: `commonMain`, `androidMain`, `iosMain` source sets
- [ ] 🟨  `expect` / `actual` declarations — platform-specific implementations
- [ ] 🟨  Shared business logic: domain models, use cases, repositories (pure Kotlin, no Android APIs)
- [ ] 🟨  `Ktor` — multiplatform HTTP client for KMP networking
- [ ] 🟨  `SQLDelight` — multiplatform database (generates type-safe Kotlin from SQL)
- [ ] 🟨  `kotlinx.coroutines` — multiplatform coroutines
- [ ] 🟨  `kotlinx.serialization` — multiplatform JSON serialization
- [ ] 🟩  KMP + Compose Multiplatform — share UI across Android, iOS, Desktop

### Chapter 15.2 — On-Device ML

- [ ] 🟥 `ML Kit` — Google's on-device ML SDK; text recognition, face detection, barcode scanning
- [ ] 🟨  `TensorFlow Lite` / `LiteRT` — run custom `.tflite` models; interpreter API
- [ ] 🟩  `GPU Delegate` — hardware acceleration for TFLite
- [ ] 🟨  Model quantization — INT8 quantization reduces model size and improves latency
- [ ] 🟥 `CameraX ImageAnalysis` + ML Kit — real-time camera analysis pipeline

---

## PHASE 16 — ADVANCED FORM FACTORS

### Chapter 16.1 — Large Screens & Foldables

- [ ] 🟥 Window size classes — `WindowWidthSizeClass.Compact/Medium/Expanded`
- [ ] 🟨  `WindowManager` Jetpack library — `calculateCurrentWindowMetrics()`
- [ ] 🟨  Adaptive layouts: `ListDetailPaneScaffold`, `SupportingPaneScaffold` (Material3)
- [ ] 🟨  Foldable features: `FoldingFeature` — posture (flat vs half-opened), orientation, bounds
- [ ] 🟨  Canonical layouts: list-detail, supporting panel, feed
- [ ] 🟨  Multi-window support — `onMultiWindowModeChanged`, resizable activity

### Chapter 16.2 — Wear OS

- [ ] 🟩  `WearApp` project setup vs phone companion
- [ ] 🟩  Compose for Wear OS — `WearApp {}`, `ScalingLazyColumn`, `SwipeDismissableNavHost`
- [ ] 🟩  Complications — data providers for watch faces
- [ ] 🟩  Health Services API — heart rate, steps, sleep

---

## PHASE 17 — TOOLING & DEBUGGING

### Chapter 17.1 — Android Studio & Profilers

- [ ] 🟥 Memory Profiler — heap dumps, allocation tracking, GC roots
- [ ] 🟥 CPU Profiler — method traces, flame charts, system traces
- [ ] 🟥 Network Inspector — inspect HTTP requests, WebSocket frames
- [ ] 🟥 Layout Inspector — live view hierarchy, Compose recomposition counts
- [ ] 🟥 Database Inspector — query Room database on device live
- [ ] 🟨  Energy Profiler — battery usage, wakelock detection
- [ ] 🟥 Logcat — filtering by tag, level, package; structured logging

### Chapter 17.2 — ADB & Command Line

- [ ] 🟥 `adb logcat` — filtering: `adb logcat *:E`, tag filters, `--pid`
- [ ] 🟥 `adb install / uninstall` — push APK, clear data
- [ ] 🟥 `adb shell` — enter device shell, run commands
- [ ] 🟨  `adb shell dumpsys activity` — inspect activity stack
- [ ] 🟨  `adb shell dumpsys meminfo <package>` — memory breakdown
- [ ] 🟨  `adb shell am start -n <component>` — start activity from terminal
- [ ] 🟨  `adb shell pm` — package manager: list, clear, grant permissions
- [ ] 🟨  `adb pull / push` — transfer files to/from device
- [ ] 🟨  `adb reverse tcp:<port>` — reverse port forwarding for local server testing

### Chapter 17.3 — Localization & i18n

- [ ] 🟥 `strings.xml` localization — `values-es/`, `values-ar/`, `values-zh-rCN/` directories
- [ ] 🟥 RTL (Right-to-Left) layout support — `android:supportsRtl="true"`, `start/end` instead of `left/right`
- [ ] 🟥 Plurals — `<plurals>` resource with `one`, `other`, `few`, `many` quantities
- [ ] 🟨  `LocaleList` — multiple locale support in Android 7+
- [ ] 🟨  Locale-aware number and date formatting — `NumberFormat`, `DateTimeFormatter`
- [ ] 🟨  Pseudolocale testing — `en-XA` (stretched text) and `ar-XB` (RTL) for i18n testing
- [ ] 🟨  App language picker (Android 13+) — `LocaleManager.setApplicationLocales()` per-app language

---

## CAPSTONE PROJECTS (Proof of Mastery)

### Beginner

- [ ] **Notes App** — Room (entities, DAO, migrations), DataStore (theme preference), Compose UI (list, detail, edit), ViewModel + StateFlow, Hilt
- [ ] **Quiz App** — local JSON data, single-Activity with Compose Navigation, score tracking with SavedStateHandle

### Intermediate

- [ ] **News Reader** — Retrofit + Moshi + OkHttp interceptors, Paging 3 + Room (RemoteMediator), offline-first, Hilt, MVVM + Clean Architecture, Compose UI
- [ ] **Weather App** — Location API + FusedLocationProvider, Retrofit, DataStore for last location, WorkManager for periodic refresh, widget

### Advanced

- [ ] **Chat / Real-Time App** — WebSocket (OkHttp), FCM push notifications, Room message caching, Compose UI, multi-module, unit tests + Compose UI tests, CI (GitHub Actions)
- [ ] **Delivery / Tracking App** — Background location (foreground service), Google Maps Compose, WorkManager for sync, offline-first, multi-module, Crashlytics + Analytics, Play Store release with staged rollout

### Specialized

- [ ] **Camera + ML App** — CameraX + ImageAnalysis + ML Kit barcode scanner or TFLite custom model
- [ ] **Biometric Auth App** — BiometricPrompt + Keystore `CryptoObject` + EncryptedSharedPreferences token storage
- [ ] **Media Player App** — ExoPlayer/Media3, background playback with MediaSessionService, notification controls, playlist management
- [ ] **Koin DI App** — Full app built using Koin instead of Hilt; multi-module, test overrides, scoped modules

---

## INTERVIEW READINESS CHECKLIST

### Core Android

- [ ] Explain the full Activity lifecycle with process death scenario
- [ ] Difference between `onSaveInstanceState` and `ViewModel` — when each is appropriate
- [ ] Fragment `viewLifecycleOwner` vs `this` — why it matters for memory leaks
- [ ] How Binder IPC works at a high level
- [ ] Why `startActivity` needs `FLAG_ACTIVITY_NEW_TASK` from non-Activity contexts

### Kotlin & Coroutines

- [ ] How coroutines are implemented under the hood (continuation-passing style)
- [ ] Difference between `StateFlow` and `SharedFlow` — when to use each
- [ ] Why `CancellationException` should never be caught silently
- [ ] `supervisorScope` vs `coroutineScope` — exception propagation difference
- [ ] How `flowOn` works — does it affect downstream collection? (No)

### Architecture

- [ ] Draw a complete Clean Architecture diagram with all dependencies
- [ ] Why domain layer entities must not be Room entities
- [ ] How to handle one-time UI events in MVVM with Compose (Channel or SharedFlow)
- [ ] Why multi-module architecture improves build times
- [ ] Difference between Hilt and Koin — when to choose each

### Performance

- [ ] How to profile an ANR — what tools, what to look for
- [ ] What causes recomposition in Compose and how to prevent unnecessary ones
- [ ] How Baseline Profiles improve cold start time

### DI

- [ ] Difference between `@Provides` and `@Binds` in Hilt
- [ ] When to use `@ViewModelScoped` vs `@ActivityRetainedScoped` vs `@Singleton`
- [ ] How `@TestInstallIn` replaces production modules in tests
- [ ] How Koin `single {}` differs from `factory {}` — lifecycle difference

---

## HOW TO TRACK EFFECTIVELY

1. **Tick only on demonstration** — write code, explain it, or pass a test. Not on reading.
2. **One chapter = one branch** in your learning repo. Merge only when Capstone task compiles and runs.
3. **Keep a daily log** — one sentence: what you built or learned.
4. **Re-read your ticked items monthly** — if you can't explain it, un-tick it.
5. **Use the Interview Readiness section** as a weekly self-quiz — speak answers out loud.
6. **Priority order** — complete all 🟥 items in a chapter before moving to 🟨  or 🟩 .