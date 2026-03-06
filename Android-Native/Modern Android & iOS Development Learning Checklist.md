

**Based on Official Documentation (2024-2025)**

---

## 📱 ANDROID DEVELOPMENT CHECKLIST

_Based on developer.android.com - Modern, Non-Legacy Topics Only_

---

### 🎯 **1. KOTLIN FUNDAMENTALS**

- [x] Variables & Constants (val, var)
- [x] Data Types (Int, String, Boolean, etc.)
- [ ] Null Safety (?, !!, ?:, let, also, apply)
- [x] Functions & Lambda Expressions
- [x] Classes, Objects, Data Classes
- [x] Inheritance & Interfaces
- [x] Sealed Classes & Enums
- [ ] Extension Functions
- [ ] Higher-Order Functions
- [ ] Coroutines Basics
- [ ] Flow & StateFlow
- [ ] Scope Functions (let, run, with, apply, also)

**Resources:** developer.android.com/kotlin

---

### 🎨 **2. JETPACK COMPOSE (UI)**

- [ ] Composable Functions Basics
- [ ] State Management (@State, remember)
- [ ] Layouts (Row, Column, Box)
- [ ] Modifiers
- [ ] LazyColumn & LazyRow (Lists)
- [ ] Scaffold & TopAppBar
- [ ] Material 3 Components
- [ ] Themes & Typography
- [ ] Animation Basics
- [ ] Navigation Compose
- [ ] Side Effects (LaunchedEffect, DisposableEffect)
- [ ] ViewModel Integration
- [ ] Custom Composables
- [ ] Performance Optimization

**Resources:** developer.android.com/jetpack/compose

**⚠️ SKIP:** XML Layouts (Legacy), View-based UI

---

### 🏗️ **3. ARCHITECTURE COMPONENTS**

#### **ViewModel**

- [ ] ViewModel Basics
- [ ] ViewModelProvider
- [ ] SavedStateHandle
- [ ] ViewModel Scope

#### **LiveData / State**

- [ ] StateFlow (Recommended)
- [ ] SharedFlow
- [ ] MutableStateFlow
- [ ] ⚠️ SKIP: LiveData (Use StateFlow instead)

#### **Repository Pattern**

- [ ] Data Layer Architecture
- [ ] Domain Layer (Use Cases)
- [ ] Presentation Layer

#### **Navigation**

- [ ] Navigation Component
- [ ] NavHost & NavController
- [ ] Type-Safe Navigation
- [ ] Deep Links
- [ ] Bottom Navigation
- [ ] Nested Navigation
- [ ] Back Stack Management

**Resources:** developer.android.com/topic/architecture

---

### 💾 **4. DATA MANAGEMENT**

#### **Room Database**

- [ ] Entity, DAO, Database
- [ ] CRUD Operations
- [ ] TypeConverters
- [ ] Migrations
- [ ] Flow with Room
- [ ] Relationships (1-to-1, 1-to-many)

#### **DataStore (Preferences)**

- [ ] Preferences DataStore
- [ ] Proto DataStore
- [ ] ⚠️ SKIP: SharedPreferences (Use DataStore)

#### **Security**

- [ ] EncryptedSharedPreferences
- [ ] Encrypted File
- [ ] BiometricPrompt

**Resources:** developer.android.com/guide/topics/data

---

### 🌐 **5. NETWORKING**

#### **Retrofit**

- [ ] API Service Interface
- [ ] GET, POST, PUT, DELETE
- [ ] Query Parameters
- [ ] Request Body
- [ ] Response Handling
- [ ] Error Handling

#### **OkHttp**

- [ ] Interceptors
- [ ] Authentication
- [ ] Logging
- [ ] Timeout Configuration

#### **Kotlin Serialization**

- [ ] @Serializable
- [ ] JSON Parsing
- [ ] Custom Serializers
- [ ] ⚠️ SKIP: Gson (Use Kotlin Serialization)

#### **Coil (Image Loading)**

- [ ] AsyncImage
- [ ] Image Loading
- [ ] Caching
- [ ] Transformations

**Resources:** developer.android.com/training/data-access

---

### 💉 **6. DEPENDENCY INJECTION**

#### **Hilt (Recommended)**

- [ ] @HiltAndroidApp
- [ ] @AndroidEntryPoint
- [ ] @Inject
- [ ] @Module & @InstallIn
- [ ] @Provides & @Binds
- [ ] ViewModel Injection
- [ ] Scopes (Singleton, ViewModelScoped)

**Resources:** developer.android.com/training/dependency-injection/hilt-android

**⚠️ SKIP:** Manual DI, Dagger2 (Use Hilt instead)

---

### 🔄 **7. ASYNCHRONOUS PROGRAMMING**

#### **Kotlin Coroutines**

- [ ] launch, async, await
- [ ] Dispatchers (Main, IO, Default)
- [ ] CoroutineScope
- [ ] viewModelScope
- [ ] lifecycleScope
- [ ] SupervisorJob
- [ ] Exception Handling
- [ ] withContext

#### **Flow**

- [ ] Flow Basics
- [ ] StateFlow
- [ ] SharedFlow
- [ ] collect, map, filter
- [ ] Flow Operators

**Resources:** developer.android.com/kotlin/coroutines

**⚠️ SKIP:** AsyncTask, RxJava (Use Coroutines)

---

### 🧪 **8. TESTING**

#### **Unit Testing**

- [ ] JUnit 5
- [ ] MockK
- [ ] Truth Assertions
- [ ] Coroutine Testing
- [ ] Flow Testing

#### **UI Testing**

- [ ] Compose Testing
- [ ] Semantics
- [ ] ComposeTestRule
- [ ] UI Assertions

**Resources:** developer.android.com/training/testing

---

### 🚀 **9. MODERN ANDROID FEATURES**

#### **Background Work**

- [ ] WorkManager
- [ ] PeriodicWorkRequest
- [ ] OneTimeWorkRequest
- [ ] Constraints
- [ ] ⚠️ SKIP: Services for background work

#### **Permissions**

- [ ] Runtime Permissions
- [ ] Permission Launcher
- [ ] Accompanist Permissions

#### **Material Design 3**

- [ ] Material You Theming
- [ ] Dynamic Colors
- [ ] Material Components

#### **App Startup**

- [ ] Splash Screen API
- [ ] App Startup Library

**Resources:** developer.android.com/develop/background-work

---

### 📦 **10. BUILD & DEPLOYMENT**

#### **Gradle**

- [ ] Build Configuration
- [ ] Dependencies Management
- [ ] Build Variants
- [ ] ProGuard/R8

#### **Version Catalogs**

- [ ] libs.versions.toml
- [ ] Centralized Dependencies

#### **App Bundles**

- [ ] Android App Bundle (.aab)
- [ ] Dynamic Feature Modules

**Resources:** developer.android.com/studio/build

---

### 🎯 **11. BEST PRACTICES**

#### **Architecture**

- [ ] MVVM Pattern
- [ ] Clean Architecture
- [ ] Single Source of Truth
- [ ] Unidirectional Data Flow

#### **Performance**

- [ ] Lazy Initialization
- [ ] Remember in Compose
- [ ] Avoid Recomposition
- [ ] Baseline Profiles

#### **Security**

- [ ] Certificate Pinning
- [ ] Network Security Config
- [ ] Code Obfuscation

**Resources:** developer.android.com/topic/performance

---

### 📱 **12. ADAPTIVE DESIGN**

- [ ] Window Size Classes
- [ ] Responsive Layouts
- [ ] Foldables Support
- [ ] Tablet Optimization
- [ ] Navigation Rail & Drawer

**Resources:** developer.android.com/guide/topics/large-screens

---

### 🤖 **13. AI & ML (Optional)**

- [ ] ML Kit
- [ ] TensorFlow Lite
- [ ] Gemini API Integration

**Resources:** developer.android.com/ai

---

## 🍎 IOS/SWIFT DEVELOPMENT CHECKLIST

_Based on developer.apple.com - Modern, Non-Legacy Topics Only_

---

### 🎯 **1. SWIFT FUNDAMENTALS**

- [ ] Variables & Constants (var, let)
- [ ] Data Types (Int, String, Bool, etc.)
- [ ] Optionals (?, !, if let, guard let)
- [ ] Functions & Closures
- [ ] Structs vs Classes
- [ ] Protocols & Extensions
- [ ] Enums with Associated Values
- [ ] Generics
- [ ] Error Handling (try, catch, throw)
- [ ] Property Wrappers
- [ ] Result Type
- [ ] Codable Protocol

**Resources:** developer.apple.com/swift

---

### 🎨 **2. SWIFTUI (UI)**

- [ ] Views & Modifiers
- [ ] State Management (@State, @Binding)
- [ ] Layouts (VStack, HStack, ZStack)
- [ ] List & ForEach
- [ ] Navigation (NavigationStack, NavigationLink)
- [ ] Forms & Controls
- [ ] Sheets & Alerts
- [ ] TabView & Navigation
- [ ] GeometryReader
- [ ] Custom Views
- [ ] Animation Basics
- [ ] Environment Values
- [ ] PreferenceKey
- [ ] ViewBuilder

**Resources:** developer.apple.com/swiftui

**⚠️ SKIP:** Storyboards, XIB files (Legacy)

---

### 🏗️ **3. ARCHITECTURE & STATE**

#### **Observation Framework (iOS 17+)**

- [ ] @Observable
- [ ] @State
- [ ] @Bindable
- [ ] ⚠️ SKIP: ObservableObject (Use @Observable)

#### **State Management**

- [ ] @State (Local State)
- [ ] @Binding (Two-way Binding)
- [ ] @Environment (Environment Values)
- [ ] @StateObject (Deprecated - Use @State)
- [ ] @ObservedObject (Deprecated - Use @Observable)

#### **MVVM Pattern**

- [ ] Model Layer
- [ ] View Layer
- [ ] ViewModel Layer
- [ ] Data Flow

**Resources:** developer.apple.com/documentation/observation

---

### 💾 **4. DATA MANAGEMENT**

#### **SwiftData (iOS 17+)**

- [ ] @Model Macro
- [ ] ModelContainer
- [ ] ModelContext
- [ ] Queries
- [ ] Relationships
- [ ] Migrations
- [ ] ⚠️ SKIP: Core Data (Use SwiftData)

#### **UserDefaults**

- [ ] Simple Key-Value Storage
- [ ] AppStorage Property Wrapper

#### **Keychain**

- [ ] Secure Storage
- [ ] KeychainWrapper

**Resources:** developer.apple.com/documentation/swiftdata

---

### 🌐 **5. NETWORKING**

#### **URLSession**

- [ ] GET, POST, PUT, DELETE
- [ ] async/await Pattern
- [ ] Data Tasks
- [ ] Upload/Download Tasks
- [ ] Error Handling

#### **Codable**

- [ ] JSON Encoding/Decoding
- [ ] Custom CodingKeys
- [ ] Nested JSON

#### **Combine (Optional)**

- [ ] Publishers & Subscribers
- [ ] Operators
- [ ] ⚠️ NOTE: async/await is preferred

#### **Kingfisher / AsyncImage**

- [ ] Image Loading
- [ ] Caching
- [ ] AsyncImage (Native)

**Resources:** developer.apple.com/documentation/foundation/urlsession

---

### 🔄 **6. CONCURRENCY**

#### **Swift Concurrency (Modern)**

- [ ] async/await
- [ ] Task & Task Groups
- [ ] Actors
- [ ] MainActor
- [ ] Sendable Protocol
- [ ] Structured Concurrency
- [ ] AsyncSequence
- [ ] AsyncStream

**Resources:** developer.apple.com/documentation/swift/concurrency

**⚠️ SKIP:** Grand Central Dispatch (GCD) - Use async/await

---

### 🧭 **7. NAVIGATION**

#### **NavigationStack (iOS 16+)**

- [ ] NavigationStack
- [ ] NavigationLink
- [ ] NavigationPath
- [ ] navigationDestination
- [ ] Deep Linking
- [ ] ⚠️ SKIP: NavigationView (Deprecated)

#### **Sheet & Presentation**

- [ ] .sheet modifier
- [ ] .fullScreenCover
- [ ] .alert
- [ ] .confirmationDialog

**Resources:** developer.apple.com/documentation/swiftui/navigationstack

---

### 🧪 **8. TESTING**

#### **Unit Testing**

- [ ] XCTest Framework
- [ ] Test Functions (testExample)
- [ ] Assertions
- [ ] Async Testing (await)
- [ ] Mock Objects

#### **UI Testing**

- [ ] XCUITest
- [ ] UI Element Queries
- [ ] Interactions
- [ ] Accessibility Identifiers

**Resources:** developer.apple.com/documentation/xctest

---

### 📱 **9. MODERN IOS FEATURES**

#### **Widgets (WidgetKit)**

- [ ] Widget Configuration
- [ ] Timeline Provider
- [ ] Widget Families
- [ ] AppIntents

#### **App Intents (iOS 16+)**

- [ ] Shortcuts Integration
- [ ] Siri Integration
- [ ] App Intents Protocol

#### **Live Activities**

- [ ] ActivityKit
- [ ] Dynamic Island

#### **Push Notifications**

- [ ] UNUserNotificationCenter
- [ ] Remote Notifications
- [ ] Local Notifications

**Resources:** developer.apple.com/documentation/widgetkit

---

### 🎬 **10. MEDIA & GRAPHICS**

#### **SwiftUI Graphics**

- [ ] Shapes & Paths
- [ ] Canvas API
- [ ] Custom Drawing
- [ ] Gradients & Effects

#### **AVFoundation**

- [ ] AVPlayer (Video)
- [ ] AVAudioPlayer (Audio)
- [ ] Camera Capture

#### **Core Image**

- [ ] Image Filters
- [ ] Effects

**Resources:** developer.apple.com/documentation/avfoundation

---

### 🔐 **11. SECURITY & PRIVACY**

#### **Authentication**

- [ ] Face ID / Touch ID
- [ ] LocalAuthentication Framework
- [ ] Sign in with Apple

#### **App Tracking Transparency**

- [ ] ATT Framework
- [ ] Privacy Manifest

#### **Security**

- [ ] Certificate Pinning
- [ ] Keychain Services
- [ ] App Transport Security

**Resources:** developer.apple.com/documentation/security

---

### 📦 **12. BUILD & DEPLOYMENT**

#### **Xcode**

- [ ] Project Structure
- [ ] Schemes & Configurations
- [ ] Build Settings
- [ ] Swift Package Manager

#### **Swift Package Manager**

- [ ] Package.swift
- [ ] Dependencies Management
- [ ] Local Packages

#### **App Store**

- [ ] App Store Connect
- [ ] TestFlight
- [ ] App Review Guidelines
- [ ] Version Management

**Resources:** developer.apple.com/app-store

**⚠️ SKIP:** CocoaPods, Carthage (Use SPM)

---

### 🎯 **13. BEST PRACTICES**

#### **Architecture**

- [ ] MVVM Pattern
- [ ] Clean Architecture
- [ ] Dependency Injection
- [ ] Repository Pattern

#### **SwiftUI Best Practices**

- [ ] View Composition
- [ ] Extract Subviews
- [ ] Avoid State Duplication
- [ ] Use Environment Properly

#### **Performance**

- [ ] Lazy Loading
- [ ] Instruments Profiling
- [ ] Memory Management

**Resources:** developer.apple.com/documentation/xcode

---

### 📱 **14. CROSS-PLATFORM**

- [ ] iPad Optimization
- [ ] macOS Catalyst (Optional)
- [ ] watchOS Basics
- [ ] tvOS Basics
- [ ] Universal Links

**Resources:** developer.apple.com/design/human-interface-guidelines

---

### 🤖 **15. AI & ML (Optional)**

- [ ] Core ML
- [ ] CreateML
- [ ] Vision Framework
- [ ] Natural Language

**Resources:** developer.apple.com/machine-learning

---

## 📊 LEARNING PATH RECOMMENDATION

### **Android Path (4-6 months)**

```
Week 1-2:   Kotlin Fundamentals
Week 3-4:   Jetpack Compose Basics
Week 5-6:   Navigation & Architecture
Week 7-8:   Room Database & Networking
Week 9-10:  Coroutines & Flow
Week 11-12: Hilt DI
Week 13-16: Build Real Projects
Week 17-20: Advanced Topics & Testing
Week 21-24: Portfolio & Interview Prep
```

### **iOS Path (4-6 months)**

```
Week 1-2:   Swift Fundamentals
Week 3-4:   SwiftUI Basics
Week 5-6:   Navigation & State Management
Week 7-8:   SwiftData & Networking
Week 9-10:  Swift Concurrency (async/await)
Week 11-12: Advanced SwiftUI
Week 13-16: Build Real Projects
Week 17-20: Advanced Topics & Testing
Week 21-24: Portfolio & Interview Prep
```

---

## ✅ WHAT TO SKIP (LEGACY/DEPRECATED)

### ❌ **Android - DO NOT LEARN:**

- XML Layouts (Use Jetpack Compose)
- View Binding (Use Compose)
- Fragments (for new apps - Use Compose Navigation)
- LiveData (Use StateFlow)
- AsyncTask (Use Coroutines)
- RxJava (Use Coroutines & Flow)
- Gson (Use Kotlin Serialization)
- Manual Dependency Injection (Use Hilt)
- Services for background work (Use WorkManager)

### ❌ **iOS - DO NOT LEARN:**

- Storyboards & XIBs (Use SwiftUI)
- UIKit for new apps (Use SwiftUI)
- Core Data (Use SwiftData)
- ObservableObject (Use @Observable)
- NavigationView (Use NavigationStack)
- Grand Central Dispatch (Use async/await)
- CocoaPods (Use Swift Package Manager)
- Objective-C (Unless maintaining legacy code)

---

## 📚 OFFICIAL RESOURCES

### **Android:**

- Official Docs: https://developer.android.com
- Jetpack Compose: https://developer.android.com/jetpack/compose
- Kotlin: https://kotlinlang.org
- Codelabs: https://developer.android.com/codelabs
- Sample Apps: https://github.com/android

### **iOS:**

- Official Docs: https://developer.apple.com/documentation
- SwiftUI: https://developer.apple.com/swiftui
- Swift: https://swift.org
- WWDC Videos: https://developer.apple.com/videos
- Sample Code: https://developer.apple.com/sample-code

---

## 🎓 CERTIFICATION PATHS

### **Android:**

- Associate Android Developer Certification
- Professional Android Developer Certification

### **iOS:**

- No official certification, but build portfolio on:
    - GitHub
    - App Store (Published Apps)
    - TestFlight Beta Testing

---

## 💡 QUICK TIPS

### **Android:**

✅ Always use Jetpack Compose for new projects ✅ Use Hilt for DI ✅ Use Coroutines + Flow for async operations ✅ Follow Material Design 3 guidelines ✅ Use StateFlow over LiveData

### **iOS:**

✅ Always use SwiftUI for new projects ✅ Use @Observable for state management ✅ Use async/await for concurrency ✅ Use SwiftData over Core Data ✅ Follow Human Interface Guidelines ✅ Use Swift Package Manager

---

**Last Updated:** January 2025 **Based on:** Android 15, iOS 18, Swift 6 **Created from:** Official Android & Apple Developer Documentation

---

## 🚀 START YOUR JOURNEY!

Pick your platform, mark your progress, and build amazing apps!

**Remember:**

- Don't learn everything at once
- Build projects while learning
- Practice daily coding
- Join developer communities
- Read official documentation
- Watch conference videos (Google I/O, WWDC)

**Good luck! 🎯**