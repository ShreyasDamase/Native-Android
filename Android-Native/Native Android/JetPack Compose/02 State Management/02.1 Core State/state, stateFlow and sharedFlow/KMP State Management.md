# 🌐 KMP State Management — Complete Guide

> [!NOTE]
> **Sub-note for Kotlin Multiplatform (KMP) State Management.** For standard Android StateFlow usage, see [[StateFlow]]. For local Compose UI state, see [[State]].

---

## 🧠 What Is KMP and Why Does State Management Change?

**Kotlin Multiplatform (KMP)** allows you to write shared Kotlin code that compiles to multiple targets:
* JVM bytecode for Android
* Native code for iOS (via Kotlin/Native)
* JavaScript for the web
* Desktop JVM for macOS, Windows, and Linux

The shared logic lives in `commonMain`, while platform-specific implementations live in `androidMain`, `iosMain`, etc.

The challenge: **Android-specific state management APIs do not exist in `commonMain`**. This includes:
* `rememberSaveable` (which depends on Android's `Bundle`)
* `LiveData` (an Android-specific observable pattern)
* `collectAsStateWithLifecycle()` (which depends on the Android lifecycle architecture)
* `AndroidX ViewModel` (which depends on Android's `ViewModelProvider`)

To manage state in KMP projects, we use **JetBrains Compose Multiplatform** and cross-platform KMP lifecycle libraries.

---

## 🔷 What Works in `commonMain`

| State Tool | KMP `commonMain`? | Notes |
| :--- | :---: | :--- |
| `mutableStateOf` | ✅ Yes | With Compose Multiplatform |
| `remember` | ✅ Yes | With Compose Multiplatform |
| `derivedStateOf` | ✅ Yes | With Compose Multiplatform |
| `mutableStateListOf` | ✅ Yes | With Compose Multiplatform |
| `Flow` / `StateFlow` / `SharedFlow` | ✅ Yes | From `kotlinx-coroutines-core` |
| `ViewModel` | ✅ Yes | From `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel` |
| `SavedStateHandle` | ✅ Yes | From `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-savedstate` (2.9+) |
| `collectAsState` | ✅ Yes | Standard `compose-runtime` |
| `rememberSaveable` | ❌ No | Android-only (needs Bundle) |
| `collectAsStateWithLifecycle` | ✅ Yes | Supported via JetBrains KMP version (2.8+) |
| `LiveData` | ❌ No | Android-only |

---

## 🔷 KMP Gradle Dependencies Setup

Here is how you configure your project dependencies inside `libs.versions.toml` and your shared module's `build.gradle.kts`:

### 1. `libs.versions.toml`
```toml
[versions]
kotlin = "2.1.0"
coroutines = "1.10.2"
lifecycle = "2.9.0"                    # JetBrains KMP Lifecycle
compose-multiplatform = "1.7.0"

[libraries]
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }

# JetBrains KMP Lifecycle library (Not AndroidX)
lifecycle-viewmodel-kmp = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel", version.ref = "lifecycle" }
lifecycle-savedstate-kmp = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-savedstate", version.ref = "lifecycle" }
lifecycle-runtime-compose-kmp = { module = "org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
```

### 2. `shared/build.gradle.kts`
```kotlin
kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.lifecycle.viewmodel.kmp)
            implementation(libs.lifecycle.savedstate.kmp)
            implementation(libs.lifecycle.runtime.compose.kmp)  // collectAsState for KMP
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
        }
    }
}
```

---

## 🔷 Shared ViewModel Example (`commonMain`)

### 1. The Shared ViewModel (Lives in `commonMain`)
```kotlin
// shared/src/commonMain/kotlin/viewmodel/SharedCounterViewModel.kt
import org.jetbrains.androidx.lifecycle.ViewModel
import org.jetbrains.androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SharedCounterViewModel : ViewModel() {  // JetBrains KMP ViewModel

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    fun increment() { _count.update { it + 1 } }
    fun decrement() { _count.update { if (it > 0) it - 1 else 0 } }
    fun reset() { _count.value = 0 }
}
```

### 2. Android Composable (`androidMain` or `shared`)
```kotlin
@Composable
fun AndroidCounterScreen(viewModel: SharedCounterViewModel = viewModel()) {
    // Uses Android-specific lifecycle-aware flow collection
    val count by viewModel.count.collectAsStateWithLifecycle()
    CounterUI(count, viewModel::increment, viewModel::decrement)
}
```

### 3. iOS Composable (`iosMain`)
```kotlin
@Composable
fun IosCounterScreen(viewModel: SharedCounterViewModel) {
    // Uses platform-neutral Compose collection (or JetBrains KMP lifecycle-runtime-compose)
    val count by viewModel.count.collectAsState()
    CounterUI(count, viewModel::increment, viewModel::decrement)
}
```

### 4. Shared UI Composable (`commonMain`)
```kotlin
@Composable
fun CounterUI(
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", style = MaterialTheme.typography.displayLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onDecrement) { Text("−") }
            Button(onClick = onIncrement) { Text("+") }
        }
    }
}
```

---

## 🔷 The iOS Bridge Problem — Consuming Flows in Swift

Swift does not natively support Kotlin's coroutine suspension model (`suspend`) or Flow collection out of the box.

### Option 1: SKIE (Recommended)
**SKIE (Swift Kotlin Interface Enhancer)** is a Gradle plugin that transforms Kotlin Flows into Swift-native `AsyncSequence` and maps suspend functions to Swift `async/await`.

#### Gradle Configuration:
```kotlin
// Root build.gradle.kts
plugins {
    id("co.touchlab.skie") version "0.8.4" apply false
}

// shared/build.gradle.kts
plugins {
    id("co.touchlab.skie")
}
```

#### Consumption in Swift Code:
With SKIE enabled, Swift can simply iterate over the flow as a native `AsyncSequence`:

```swift
// Swift UI Code
@MainActor
class CounterViewController: UIViewController {
    let viewModel = SharedCounterViewModel()

    override func viewDidLoad() {
        super.viewDidLoad()
        Task {
            // SKIE turns StateFlow<Int> into an AsyncSequence<Int> in Swift
            for await count in viewModel.count {
                countLabel.text = "\(count)"
            }
        }
    }
}
```

### Option 2: `KMP-NativeCoroutines`
Another popular library that generates Swift-compatible wrappers (`AnyPublisher` or `async/await`) for Kotlin coroutines.

```kotlin
import com.rickclephas.kmm.foundation.coroutines.NativeCoroutinesState
import com.rickclephas.kmm.foundation.coroutines.NativeCoroutines

class MySharedViewModel : ViewModel() {
    @NativeCoroutinesState
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    @NativeCoroutines
    val events: SharedFlow<Event> = _events.asSharedFlow()
}
```

---

## 🔗 Connections

- **StateFlow Architecture**: [[StateFlow]] — StateFlow usage in ViewModels
- **Coroutines & Lifecycles**: [[Coroutines/Coroutines in Kotlin Complete Notes]] — dispatchers, scopes
- **Comparison Guide**: [[Comparison — State vs Flow vs StateFlow vs LiveData vs SharedFlow]]
