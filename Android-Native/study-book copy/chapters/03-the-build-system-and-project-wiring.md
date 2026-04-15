# Chapter 3: The Build System and Project Wiring

## Code Files To Open

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle/libs.versions.toml`
- `gradle.properties`
- `shared/build.gradle.kts`
- `composeApp/build.gradle.kts`


## 3.1 Why the build system matters

Many beginners focus only on source files. But in KMP, the build files are part of the architecture.

They define:

- which platforms are supported
- which source sets exist
- which dependencies go to which platform
- how modules connect

If you do not understand the Gradle setup, you will struggle later while creating your own app.

## 3.2 `settings.gradle.kts`

This file does several important things:

- names the root project
- configures plugin repositories
- configures dependency repositories
- includes Gradle modules

Most importantly, it includes:

- `:composeApp`
- `:shared`

Notice what it does not include:

- `iosApp`

This teaches an important KMP concept:

- Android/Desktop Kotlin modules are managed by Gradle
- the iOS app itself is managed by Xcode
- Gradle produces a Kotlin framework that Xcode consumes

## 3.3 `build.gradle.kts` at root

This root build file mainly sets up plugins at the top level without applying them everywhere.

Why do that?

- to centralize plugin versions
- to avoid loading the same plugin repeatedly in each child classloader

It also applies the dependency updates plugin to all projects.

That is a maintenance convenience feature.

## 3.4 `gradle/libs.versions.toml`

This is the version catalog.

This file is extremely important because it centralizes:

- Kotlin version
- Android Gradle Plugin version
- Ktor version
- Compose version
- Koin version
- Coroutines version
- serialization version
- platform library versions

This is cleaner than hardcoding versions in multiple files.

As your own app grows, this pattern becomes very valuable.

## 3.5 `gradle.properties`

This file contains project-level Gradle settings, including:

- JVM memory
- configuration cache
- build cache
- Kotlin code style
- AndroidX flags

These are not app features, but they influence build behavior and performance.

## 3.6 `shared/build.gradle.kts`

This file defines `shared` as a multiplatform library.

Key ideas inside it:

- Android target exists
- iOS targets exist
- JVM target exists
- iOS framework binary is built
- dependencies are split by source set

This file is your “how shared code is built for many platforms” textbook.

## 3.7 `composeApp/build.gradle.kts`

This file defines the UI app module for Android and Desktop.

Key ideas:

- it is multiplatform too
- it has Android and JVM targets
- common Compose UI lives in `commonMain`
- Android-specific dependencies go to `androidMain`
- Desktop-specific dependencies go to `jvmMain`
- this module depends on `projects.shared`

This is how the app UI layer sits on top of the shared engine.

## 3.8 KMP source sets: the most important build concept

The source set model is the KMP learning foundation.

Typical source sets in this repo are:

- `commonMain`
- `androidMain`
- `iosMain`
- `jvmMain`

Meaning:

- `commonMain`: code shared by all targets in that module
- `androidMain`: Android-only code
- `iosMain`: iOS-only code
- `jvmMain`: JVM-only code, here used for desktop

When you see a class in `commonMain`, ask:

- can every target use this?

When you see a class in `androidMain`, ask:

- what platform-specific dependency or API forced this to live here?

This question will help you understand KMP file placement correctly.

---

