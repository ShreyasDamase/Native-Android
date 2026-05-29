# ✅ Android Interview Questions — Study Checklist

> Track your prep progress. Check off each question once you can answer it confidently.  
> **160 questions** across 12 topics.

---

## 📊 Progress Overview

|#|Topic|Questions|
|---|---|---|
|1|Fundamentals|20|
|2|Kotlin Language|18|
|3|Concurrency, Coroutines & Flow|18|
|4|UI, Layouts, RecyclerView & Compose|19|
|5|Architecture & Design Patterns|18|
|6|Data Storage, DB & Caching|14|
|7|Networking & APIs|13|
|8|Background Processing, Scheduling & Push|10|
|9|Performance, Memory, Profiling & Security|14|
|10|Testing & QA|10|
|11|Build System, Release & Play Store|8|
|12|System Design, Architecture & Scenarios|12|

---

## 🟢 1. Fundamentals — Core Components & Basics

> _20 Questions — The floor of every Android interview_

- [ ] 1. What are the main Android app components? Describe each briefly.
- [ ] 2. Explain the Activity lifecycle and give typical uses for each callback.
- [ ] 3. Explain the Fragment lifecycle and how it interacts with the Activity lifecycle.
- [ ] 4. What is an Intent? Difference between implicit and explicit intents.
- [ ] 5. What is a Service? Types of services (foreground, background, bound).
- [ ] 6. What is a BroadcastReceiver? When and how would you register one?
- [ ] 7. What is a ContentProvider and when would you use it?
- [ ] 8. Explain application and process lifecycle on Android. How does the system kill processes?
- [ ] 9. How does Android handle permissions? Explain runtime permissions and the permission model.
- [ ] 10. What is the AndroidManifest.xml and what must be declared there?
- [ ] 11. Explain activities' launch modes (standard, singleTop, singleTask, singleInstance).
- [ ] 12. What is a PendingIntent and typical use-cases?
- [ ] 13. How do you pass data between Activities/Fragments? Discuss Parcelable vs Serializable.
- [ ] 14. What are resource qualifiers? How do you provide multiple screen-size resources?
- [ ] 15. Explain view inflation — what happens under the hood?
- [ ] 16. How does Android render UI on the main thread? Why must UI updates happen on the main thread?
- [ ] 17. What is ANR? Common causes and how to prevent it.
- [ ] 18. Describe the project structure of an Android app (manifests, java/kotlin, res, Gradle files).
- [ ] 19. Explain how Android apps are packaged (APK vs AAB).
- [ ] 20. What is the role of the Application class and how is it different from Activity?

---

## 🟡 2. Kotlin Language & Idiomatic Android Kotlin

> _18 Questions — Know the language deeply, not just syntactically_

- [ ] 1. Why use Kotlin for Android? Key advantages over Java.
- [ ] 2. Explain `val` vs `var`, `lateinit`, and `by lazy`.
- [ ] 3. What are extension functions and where are they useful?
- [ ] 4. Explain higher-order functions and function types in Kotlin.
- [ ] 5. What are sealed classes and when would you use them?
- [ ] 6. Explain coroutines' `suspend` keyword at a high level.
- [ ] 7. Explain null-safety in Kotlin: `?`, `!!`, `?:`, `let` usage.
- [ ] 8. What are data classes and what methods are generated for them?
- [ ] 9. What is `inline` and when is it beneficial?
- [ ] 10. What is `reified` type parameter and when to use it?
- [ ] 11. What are typealiases and when are they helpful?
- [ ] 12. Explain Kotlin Flow basics vs RxJava observable patterns.
- [ ] 13. What is Kotlin Multiplatform and when is it appropriate?
- [ ] 14. Explain `apply`, `also`, `let`, `run`, `with` — differences and common use-cases.
- [ ] 15. What is a coroutine `Dispatcher`? Compare `Dispatchers.Main`, `IO`, `Default`.
- [ ] 16. What are `StateFlow` and `SharedFlow`? When to use each?
- [ ] 17. How to create a custom DSL in Kotlin? Example scenarios.
- [ ] 18. How to avoid memory leaks with Kotlin lambdas and captured references?

---

## 🔵 3. Concurrency, Coroutines & Flow

> _18 Questions — Where mid-level devs are separated from seniors_

- [ ] 1. What is a coroutine? How is it different from a thread?
- [ ] 2. Explain structured concurrency and why it matters.
- [ ] 3. Compare `launch` vs `async` and when to use each.
- [ ] 4. How do you cancel a coroutine and properly handle cancellation?
- [ ] 5. Explain `SupervisorJob` and when you would use it.
- [ ] 6. How does exception handling work in coroutines? (`CoroutineExceptionHandler`)
- [ ] 7. What is the difference between cold and hot streams?
- [ ] 8. Explain backpressure and strategies in Flow.
- [ ] 9. How to bridge callbacks to coroutines? (e.g., `suspendCancellableCoroutine`)
- [ ] 10. How to test coroutine-based code? (unit tests, `runBlocking`, TestDispatchers)
- [ ] 11. How to handle concurrency with shared mutable state? (Mutex, Actors)
- [ ] 12. When to prefer `Flow` vs `LiveData` vs callbacks?
- [ ] 13. Explain `collectLatest` semantics.
- [ ] 14. How do you do parallel async work and aggregate results safely?
- [ ] 15. What are channels and how do they differ from Flow?
- [ ] 16. How do `viewModelScope`, `lifecycleScope`, `GlobalScope` differ?
- [ ] 17. Explain `withContext` and thread switching in coroutines.
- [ ] 18. How to implement retry/backoff with Flow for network operations?

---

## 🟠 4. UI, Layouts, RecyclerView & Compose

> _19 Questions — Both XML legacy and modern Compose_

- [ ] 1. What are common layout types and when to use each? (LinearLayout, ConstraintLayout, FrameLayout)
- [ ] 2. What is ConstraintLayout and what are chains/guidelines?
- [ ] 3. How does a RecyclerView work? Explain the ViewHolder pattern and recycling.
- [ ] 4. Explain DiffUtil and why you should use it.
- [ ] 5. How do you implement pagination efficiently with RecyclerView? (Paging 3)
- [ ] 6. How to implement sticky headers in RecyclerView?
- [ ] 7. What is a custom view and when to build one? How to draw with Canvas?
- [ ] 8. How to implement touch gestures and handle nested scrolling?
- [ ] 9. What is Jetpack Compose? Compare declarative Compose vs XML UI.
- [ ] 10. Explain Compose state management (`remember`, `State`, `mutableStateOf`, `derivedStateOf`).
- [ ] 11. How to integrate Compose with existing XML-based code?
- [ ] 12. How to test Compose UI? (unit + instrumentation)
- [ ] 13. How to implement animations in Compose? (`AnimatedVisibility`, `animate*AsState`)
- [ ] 14. Explain recomposition — what causes it and how to optimize?
- [ ] 15. How to implement theming and dynamic color (Material3) in Compose?
- [ ] 16. How does Compose handle layouts? (`Modifier`, `Row`, `Column`, `Box`)
- [ ] 17. How to manage large lists with Compose (`LazyColumn`, `LazyRow`) and pagination?
- [ ] 18. What is the best approach for complex UI state in Compose? (state hoisting)
- [ ] 19. How to measure and reduce UI overdraw? Tools and techniques.

---

## 🔴 5. Architecture & Design Patterns

> _18 Questions — This is what separates job offers from rejections_

- [ ] 1. Compare MVC, MVP, MVVM and MVI — pros/cons of each for Android.
- [ ] 2. What is Clean Architecture and how would you apply it to an Android project?
- [ ] 3. What is a repository pattern and how does it fit into MVVM?
- [ ] 4. Explain single source of truth and offline-first strategies.
- [ ] 5. How to structure feature modules and multi-module projects? Benefits?
- [ ] 6. Explain SOLID principles with Android examples.
- [ ] 7. What is dependency injection? Why use it? (Hilt/Dagger example)
- [ ] 8. How to manage state across process death and configuration changes?
- [ ] 9. How do you design a large-scale app to be testable and maintainable?
- [ ] 10. How to choose boundaries between UI, domain, and data layers?
- [ ] 11. How to implement use-cases/interactors in Android?
- [ ] 12. How to handle cross-cutting concerns? (logging, analytics, error handling)
- [ ] 13. Explain event-driven vs state-driven UI and when to use each.
- [ ] 14. How to design for feature toggles and A/B testing?
- [ ] 15. How and when to use reactive programming (RxJava/Flow) in architecture?
- [ ] 16. How to handle multi-window and foldable devices in architecture?
- [ ] 17. What are pros/cons of single-activity architecture?
- [ ] 18. How would you migrate a legacy app to Compose + modern architecture?

---

## 🟣 6. Data Storage, DB & Caching

> _14 Questions — Room, DataStore, encryption and sync_

- [ ] 1. What are the storage options in Android? (SharedPreferences, DataStore, Files, SQLite/Room, Cloud)
- [ ] 2. What is Room? Explain Entities, DAO, Database.
- [ ] 3. How to run queries in Room off the main thread? How to observe changes?
- [ ] 4. Explain migrations in Room. How to write safe migrations?
- [ ] 5. Compare Room vs raw SQLite vs Realm vs ObjectBox (tradeoffs).
- [ ] 6. What is DataStore? Preferences vs Proto DataStore. When to use DataStore over SharedPreferences?
- [ ] 7. How to implement an efficient cache strategy for network-backed lists?
- [ ] 8. Explain paging + RemoteMediator (Paging 3) and network + DB syncing.
- [ ] 9. How to store large binary data (images, audio)? Best practices.
- [ ] 10. How to encrypt local data? (SQLCipher, EncryptedFile, EncryptedSharedPreferences)
- [ ] 11. How to design sync for offline-first apps? (conflict resolution, last-write-wins)
- [ ] 12. How to minimize DB contention and improve query performance? (indexes, transactions)
- [ ] 13. How to handle multi-user data separation on a single device?
- [ ] 14. When and how to use the NDK for storage or performance reasons?

---

## 🌐 7. Networking & APIs

> _13 Questions — Retrofit, OkHttp, auth, and real-world patterns_

- [ ] 1. How to perform network calls in Android safely? (avoid blocking UI)
- [ ] 2. Explain Retrofit + OkHttp architecture and common customizations. (interceptors, timeouts)
- [ ] 3. How to implement authentication (OAuth2) and token refresh in a mobile app?
- [ ] 4. Explain certificate pinning and network security config. Why and how to use it?
- [ ] 5. How to implement WebSockets or real-time messaging in Android?
- [ ] 6. How to handle large file upload/download with progress and resume?
- [ ] 7. How to throttle or debounce network requests? (search suggestions)
- [ ] 8. How to design API responses for mobile? (pagination, partial updates)
- [ ] 9. How to implement exponential backoff and retry strategies for unreliable networks?
- [ ] 10. How to monitor and test network conditions? (offline, low bandwidth, airplane mode)
- [ ] 11. What is GraphQL and how does it compare to REST for mobile clients?
- [ ] 12. How to use caching headers and OkHttp caching to reduce bandwidth?
- [ ] 13. When to use gRPC for mobile and what to watch out for?

---

## ⚙️ 8. Background Processing, Scheduling & Push

> _10 Questions — WorkManager, FCM, battery optimizations_

- [ ] 1. Compare WorkManager, JobScheduler, AlarmManager, Firebase Cloud Messaging usage.
- [ ] 2. When to use foreground service vs WorkManager for long-running tasks?
- [ ] 3. How to schedule exact alarms and what are Doze/Battery optimization implications?
- [ ] 4. How to handle background location updates and related permission changes?
- [ ] 5. How does FCM work? Handling high-priority vs normal messages.
- [ ] 6. How to handle background processing when the app is killed? (WorkManager constraints)
- [ ] 7. How to implement periodic sync and ensure it's battery-friendly?
- [ ] 8. How to schedule background tasks across API levels and OEM differences?
- [ ] 9. How to debug and test background jobs locally?
- [ ] 10. What are the security considerations for background work? (data at rest, credentials)

---

## 🔍 9. Performance, Memory, Profiling & Security

> _14 Questions — What keeps apps at 4.8★ vs 3.2★_

- [ ] 1. How to find and fix memory leaks? (LeakCanary, heap dumps)
- [ ] 2. How to diagnose and fix ANRs? (traces, systrace)
- [ ] 3. How to use Android Profiler (CPU, Memory, Network) in Android Studio?
- [ ] 4. What is overdraw? How to measure and reduce it?
- [ ] 5. How to reduce APK/AAB size? (resources, R8, split APKs)
- [ ] 6. How to optimize RecyclerView for smooth scrolling? (view types, prefetch, image loading)
- [ ] 7. How to secure sensitive data on device? (Keystore, EncryptedSharedPreferences)
- [ ] 8. Explain code obfuscation and why use R8/ProGuard. What to watch out for?
- [ ] 9. How to implement certificate pinning and SSL/TLS best practices?
- [ ] 10. How to handle 3rd-party SDK risk? (privacy, performance)
- [ ] 11. How to profile and reduce app startup time? (cold, warm, hot start)
- [ ] 12. How to measure battery impact and optimize power usage?
- [ ] 13. How to design for accessibility and test for it? (TalkBack, content descriptions)
- [ ] 14. How to protect your app from tampering and reverse engineering basics?

---

## 🧪 10. Testing & QA

> _10 Questions — The topic most devs under-prepare_

- [ ] 1. What kinds of tests should an Android app have? (unit, integration, instrumentation, UI)
- [ ] 2. How to write unit tests for ViewModels and repositories? (tools and mocking strategies)
- [ ] 3. How to use Espresso for UI tests? How to synchronize with background threads?
- [ ] 4. What is Robolectric and when to use it?
- [ ] 5. How to write reliable, flake-free UI tests? (idling resources, test tags)
- [ ] 6. How to do end-to-end testing for apps that use network? (mock server, dependency injection)
- [ ] 7. How to implement CI pipelines for Android tests? (GitHub Actions, Bitrise, Firebase Test Lab)
- [ ] 8. How to test database migrations?
- [ ] 9. How to test Compose UIs? (`composeTestRule`)
- [ ] 10. How to measure and test performance regressions in CI?

---

## 🚀 11. Build System, Release & Play Store

> _8 Questions — Often ignored until it breaks in production_

- [ ] 1. Explain Gradle build variants, flavors and build types.
- [ ] 2. How to configure ProGuard/R8 rules for libraries and generated code?
- [ ] 3. What is App Bundle (AAB) and why prefer it over APK?
- [ ] 4. How to implement feature modularization and dynamic feature modules? (Play Feature Delivery)
- [ ] 5. How to set up signing configs and manage keystore securely?
- [ ] 6. What is staged rollout and how to monitor crash/ANR metrics after release?
- [ ] 7. How to automate releases? (Fastlane, Play Developer API)
- [ ] 8. How to handle backward-compatible migrations for released apps?

---

## 🏗️ 12. System Design, Architecture & Scenario Questions

> _12 Questions — Senior/Staff level. Design out loud._

- [ ] 1. Design the architecture for a chat app supporting offline messages and push notifications.
- [ ] 2. How would you design an offline-first news feed with images, pagination, and sync?
- [ ] 3. How to design a media streaming app? (buffering, adaptive bitrate, caching)
- [ ] 4. Design a photo gallery app that can handle millions of images efficiently.
- [ ] 5. How to design feature flags and A/B testing in an Android app?
- [ ] 6. How would you implement a sync system that resolves conflicts between server and client edits?
- [ ] 7. Design an architecture that supports multiple product flavors with shared features and different resources.
- [ ] 8. How to design app telemetry/analytics for privacy? (PII minimization)
- [ ] 9. How to design a secure in-app purchase flow and validate purchases server-side?
- [ ] 10. How to design for low-latency interaction with sensors (BLE, GPS) and conserve battery?
- [ ] 11. How to plan a migration strategy from a monolith Android app to modular architecture with minimal user impact?
- [ ] 12. How to instrument and monitor feature health (errors, performance) in production?

---

## 🎯 Study Strategy

```
Phase 1 — Foundation (Week 1–2)
  ✅ Section 1: Fundamentals
  ✅ Section 2: Kotlin Language

Phase 2 — Core Android (Week 3–4)
  ✅ Section 3: Coroutines & Flow
  ✅ Section 4: UI & Compose
  ✅ Section 5: Architecture

Phase 3 — Deep Dive (Week 5–6)
  ✅ Section 6: Storage & DB
  ✅ Section 7: Networking
  ✅ Section 8: Background Work

Phase 4 — Polish (Week 7–8)
  ✅ Section 9: Performance & Security
  ✅ Section 10: Testing
  ✅ Section 11: Build & Release
  ✅ Section 12: System Design
```

---

_160 questions total. One check at a time. You've got this. 💪_