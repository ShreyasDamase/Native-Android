# 📦 Publishing the Library

## 📌 Purpose
Once your `RetroUI` custom UI library is built, you likely want to share it across multiple projects or make it open-source for the Android community. This file covers three methods: Local Maven, JitPack, and Maven Central.

---

## 🏠 1. Local Testing (Maven Local)
Before pushing to the public internet, you can publish your library to your local machine's Maven repository (`~/.m2`). This allows other apps on your same laptop to depend on it.

### Step 1: Add Publishing Plugin
In your library's `build.gradle.kts`:

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    `maven-publish` // Add this
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.yourname"
            artifactId = "retroui"
            version = "1.0.0-SNAPSHOT"
            
            // Wait for components to be created by Android plugin
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
```

### Step 2: Execute Task
Run this in your terminal:
```bash
./gradlew :retroui:publishToMavenLocal
```

### Step 3: Consume in App
In your testing app's `settings.gradle.kts`, ensure `mavenLocal()` is included:
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal() // MUST be here
    }
}
```
Then add the dependency: `implementation("com.yourname:retroui:1.0.0-SNAPSHOT")`.

---

## 🐙 2. JitPack (Easy Open Source)
JitPack is the easiest way to publish Android libraries hosted on GitHub. It builds the AAR for you.

### Step 1: Add JitPack Plugin
In your library `build.gradle.kts`:
```kotlin
plugins {
    id("com.github.dcendents.android-maven") version "2.1"
}
// Note: group is automatically taken from GitHub username
```

### Step 2: Push & Release
1. Push your code to GitHub.
2. Go to your GitHub repository -> Releases -> **Draft a new release**.
3. Create a tag (e.g., `1.0.0`) and publish.

### Step 3: Consume via JitPack
In the consumer app's `settings.gradle.kts`:
```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}
```
Dependency: `implementation("com.github.YourUsername:RetroUI:1.0.0")`

---

## 🏛️ 3. Maven Central (Production)
Publishing to Maven Central is the official standard but requires a rigorous process.

1. **Create Sonatype Account:** Register at [issues.sonatype.org](https://issues.sonatype.org/).
2. **Claim Namespace:** Open a JIRA ticket to claim your `groupId` (e.g., `io.github.yourusername`).
3. **GPG Signing:** You must generate a GPG key, publish it to a public keyserver, and use it to sign your artifacts.
4. **Nexus Staging Plugin:** Use the `maven-publish` and `signing` plugins alongside a script (like `gradle-nexus/publish-plugin`) to upload to Sonatype Nexus.
5. **Verification:** Close and release the repository via the Nexus UI.

> [!TIP]
> If you plan to go the Maven Central route, look into **Vanniktech's Maven Publish Plugin**, which automates 90% of the GPG signing and POM generation boilerplate.

---

## 📄 README Template
When open-sourcing, a good `README.md` is critical. Here is a template:

```markdown
# 📻 RetroUI for Jetpack Compose

A fully custom, skeuomorphic Vintage UI component library for Jetpack Compose. No Material dependencies, just raw Canvas drawing for that classic analog feel.

![Demo GIF Placeholder](/docs/demo.gif)

## 📦 Installation

```kotlin
dependencies {
    implementation("com.github.YourUsername:RetroUI:1.0.0")
}
```

## 🛠️ Quick Start

Wrap your UI in the `VintageTheme` and start using components:

```kotlin
VintageTheme {
    Column {
        VintageMeter(value = currentVolume)
        VintageKnob(value = gain, onValueChange = { gain = it })
        VintageToggle(checked = powerOn, onCheckedChange = { powerOn = it })
    }
}
```

## 🎛️ Component Roster
| Component | Description |
|-----------|-------------|
| `VintageButton` | Physical press mechanism, inverted lighting on press. |
| `VintageKnob` | Rotary dial with precision tick marks. |
| `VintageMeter` | Analog VU meter with spring physics. |
| `VintageToggle` | Snap-action lever switch. |
| `VintageLED` | Glass dome indicator with outer glow. |
| `VintageSlider`| Horizontal fader with deep track grooves. |

## 📄 License
MIT License.
```
