Here is your **complete, comparison-based guide** that shows exactly what you did wrong vs what works, so you never forget the mistakes again.

---

# KSP + Hilt + Room Setup Guide: What I Did Wrong vs What Works

## The Problem I Faced

```
Unable to load class 'com.google.devtools.ksp.gradle.KspTaskJvm'
IllegalStateException: The KSP plugin was detected to be applied but its task class could not be found.
```

My app kept crashing with this error. After hours of debugging, I finally fixed it. Here's exactly what I learned.

---

## My Mistakes (And How to Fix Each One)

### ❌ Mistake #1: Inconsistent Plugin Declaration

**What I did wrong:**
- Applied Hilt plugin at the **root** level (with `apply false`)
- Applied KSP plugin **only** in the app module
- This created a class loader mismatch

**The error I saw:**
```
The Hilt Gradle Plugin is using a different class loader because
it was declared at the root while KSP was declared in a sub-project.
```

**✅ Fix:** Declare ALL plugins in the SAME scope.

| File | What I Did Wrong | What Works |
|------|------------------|-------------|
| **Root `build.gradle.kts`** | Had some plugins with `apply false`, some without | ALL plugins with `apply false` |
| **App `build.gradle.kts`** | Applied only some plugins | Apply ALL plugins here |

---

### ❌ Mistake #2: Wrong Dependency Syntax for Compilers

**What I did wrong:**
```kotlin
// ❌ I used this (WRONG)
implementation(libs.androidx.room.compiler)
implementation(libs.hilt.compiler)
```

**Why it's wrong:** Room and Hilt compilers need to run at **compile time** to generate code. Using `implementation` puts them in the APK (wasted space) and they don't run properly.

**✅ Fix:** Use `ksp` for ALL annotation processors:
```kotlin
// ✅ CORRECT
ksp(libs.androidx.room.compiler)
ksp(libs.hilt.compiler)
```

---

### ❌ Mistake #3: Missing `kotlinOptions`

**What I did wrong:**
```kotlin
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
// ❌ Forgot to set kotlinOptions
```

**Why it's wrong:** Kotlin code needs to know which JVM target to compile for. Without this, you might get compatibility issues.

**✅ Fix:**
```kotlin
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlinOptions {  // ✅ ADD THIS
    jvmTarget = "11"
}
```

---

### ❌ Mistake #4: Version Mismatch (Kotlin vs KSP)

**What I did wrong:**
```
kotlin = "2.2.10"  // Different version
ksp = "2.3.2"      // Different version
```

**Why it's wrong:** KSP version MUST match your Kotlin version exactly.

**✅ Fix:** Always check the [KSP compatibility chart](https://github.com/google/ksp/releases).

| Kotlin Version | Correct KSP Version |
|----------------|---------------------|
| 2.0.21 | 2.0.21-1.0.26 |
| 2.0.20 | 2.0.20-1.0.25 |
| 1.9.0 | 1.9.0-1.0.13 |

**My working versions:**
```
kotlin = "2.0.21"
ksp = "2.0.21-1.0.26"
```

---

### ❌ Mistake #5: Missing KSP Plugin in Version Catalog

**What I did wrong:**
```toml
[plugins]
# ❌ Forgot to add ksp plugin
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

**✅ Fix:** Add KSP to `[plugins]` section:
```toml
[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }           # ✅ ADD THIS
hiltAndroid = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
room = { id = "androidx.room", version.ref = "room" }
```

---

## The FINAL Working Configuration

### File 1: `gradle/libs.versions.toml`

```toml
[versions]

# Build tools - VERSIONS MUST MATCH!
agp = "8.5.2"
kotlin = "2.0.21"           # Kotlin version
ksp = "2.0.21-1.0.26"       # KSP must match Kotlin!

# AndroidX
coreKtx = "1.13.1"
lifecycleRuntimeKtx = "2.8.7"
activityCompose = "1.9.3"

# Compose
composeBom = "2025.02.00"

# Testing
junit = "4.13.2"
junitVersion = "1.1.5"
espressoCore = "3.5.1"

# DI + DB
hilt = "2.51.1"
room = "2.8.4"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }

# Compose
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }

# Room
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }          # ✅ MUST HAVE
hiltAndroid = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
room = { id = "androidx.room", version.ref = "room" }
```

---

### File 2: `build.gradle.kts` (Project Level / Root)

```kotlin
// ✅ ALL plugins with apply false (SAME SCOPE)
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hiltAndroid) apply false
    alias(libs.plugins.ksp) apply false      // ✅ KSP here too
    alias(libs.plugins.room) apply false
}
```

---

### File 3: `app/build.gradle.kts` (App Level)

```kotlin
// ✅ Apply ALL plugins here
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)                 // ✅ KSP applied
    alias(libs.plugins.hiltAndroid)         // ✅ Hilt applied
    alias(libs.plugins.room)                // ✅ Room plugin (optional)
}

android {
    namespace = "com.example.state"
    compileSdk = 36
    
    // Optional: Room schema directory
    room {
        schemaDirectory("$projectDir/schemas")
    }
    
    defaultConfig {
        applicationId = "com.example.state"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    // ✅ Java compatibility
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    // ✅ Kotlin compatibility - DON'T FORGET THIS!
    kotlinOptions {
        jvmTarget = "11"
    }
    
    buildFeatures {
        compose = true
    }
    
    // Optional: Force JDK 11 toolchain
    kotlin {
        jvmToolchain(11)
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // Room - ✅ ksp, NOT implementation
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)      // ✅ KSP for Room
    
    // Hilt - ✅ ksp, NOT implementation
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)               // ✅ KSP for Hilt
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    
    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

---

## Quick Reference: What I Learned

| What I Did Wrong | Correct Way |
|------------------|--------------|
| Applied plugins in different scopes | All plugins with `apply false` at root, all applied in app module |
| Used `implementation` for compilers | Use `ksp` for Room and Hilt compilers |
| Forgot `kotlinOptions { jvmTarget }` | Always add `kotlinOptions { jvmTarget = "11" }` |
| Mismatched Kotlin and KSP versions | KSP version must match Kotlin exactly |
| Missing KSP in `[plugins]` section | Add `ksp` plugin to version catalog |

---

## About `jvmToolchain(11)` - Do I Need It?

| You have... | Do you need `jvmToolchain(11)`? |
|-------------|--------------------------------|
| JDK 11 installed as default | ❌ No, `compileOptions` + `kotlinOptions` is enough |
| JDK 17 or 21 as default | ✅ Yes, to force Gradle to use JDK 11 |
| Team members with different JDKs | ✅ Yes, ensures consistency |

**My experience:** The app ran fine WITHOUT `jvmToolchain(11)` because my system already had JDK 11. I added it later for safety.

---

## Checklist for Next Time

- [ ] Kotlin and KSP versions match exactly?
- [ ] All plugins in root with `apply false`?
- [ ] All plugins applied in app module?
- [ ] Compilers use `ksp(...)` not `implementation(...)`?
- [ ] `kotlinOptions { jvmTarget = "11" }` added?
- [ ] `compileOptions` set to Java 11?
- [ ] Synced Gradle after changes?

---

**Save this guide in Obsidian. Next time you'll get it right in 5 minutes instead of 5 hours!**