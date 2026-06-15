# 📦 Gradle Dependencies Master List

> [!NOTE]
> **Sub-note for Gradle Dependencies.** For implementation setup, see [[State]] for local state dependencies and [[StateFlow]] / [[SharedFlow]] for coroutine state flow dependencies.

---

## 🔷 Android-Only Projects (BOM Approach)

Use the Compose Bill of Materials (BOM) to automatically align Compose library versions:

```kotlin
// build.gradle.kts (app module)
dependencies {
    // ── Compose BOM (manages all Compose versions together) ──────
    val composeBom = platform("androidx.compose:compose-bom:2025.05.00")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
    // Note: runtime-saveable is usually transitively included via material3 or foundation

    // ── Lifecycle / ViewModel ──────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")  // collectAsStateWithLifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.7") // SavedStateHandle

    // ── Coroutines ─────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // ── Optional: Immutable collections (performance stability) ──────────
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")

    // ── Optional: LiveData in Compose (legacy integrations) ─────────────────
    implementation("androidx.compose.runtime:runtime-livedata")  // via BOM

    // ── Optional: Hilt (dependency injection) ──────────────────
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ── Optional: Room (database with Flow) ────────────────────
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")  // Flow support
    kapt("androidx.room:room-compiler:2.6.1")
    
    // ── Optional: Turbine (Testing Flow) ──────────────────────
    testImplementation("app.cash.turbine:turbine:1.2.0")
}
```

---

## 🔷 KMP Projects (Multiplatform Setup)

For Kotlin Multiplatform projects, dependencies are split between the `commonMain` shared logic source set and target platform source sets:

```kotlin
// shared/build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("co.touchlab.skie") version "0.8.4"  // Optional: iOS Flow bridging plugin
}

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            // JetBrains cross-platform lifecycle dependencies:
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.9.0")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-savedstate:2.9.0")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")
        }
        
        androidMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
            implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
        }
    }
}
```

---

## 🔷 Minimum Versions Reference

| Library | Min Kotlin | Min Android API | KMP Supported? |
| :--- | :---: | :---: | :---: |
| `kotlinx-coroutines-core` 1.8+ | 1.8 | 21 | ✅ Yes |
| `lifecycle-viewmodel` 2.8+ | 1.8 | 21 | ✅ Yes (via JetBrains KMP) |
| `lifecycle-runtime-compose` 2.6+ | 1.8 | 21 | ✅ Yes (via JetBrains KMP) |
| `rememberSaveable` | Any Compose | 21 | ❌ No (Android-only) |
| `rememberSerializable` | Kotlin 2.0 | 21 | ✅ Yes (JetBrains) |
| `SavedStateHandle` KMP | Kotlin 2.0 | 21 | ✅ Yes (JetBrains 2.9+) |
| Compose Multiplatform | Kotlin 2.0 | 21 | ✅ Yes |
