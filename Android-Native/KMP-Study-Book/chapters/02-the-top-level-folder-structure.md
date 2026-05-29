# Chapter 2: The Top-Level Folder Structure

## Code Files To Open

- `settings.gradle.kts`
- `shared/build.gradle.kts`
- `composeApp/build.gradle.kts`
- `iosApp/iosApp.xcodeproj/project.pbxproj`


## 2.1 The important top-level folders

At the root of the repository, the most important folders are:

- `shared`
- `composeApp`
- `iosApp`
- `gradle`
- `.github`
- `media`

Each one has a very different purpose.

## 2.2 `shared`

This is the heart of the project.

It contains:

- shared business logic
- shared models
- shared storage logic
- shared networking logic
- shared state management
- iOS-specific helpers for bridging Kotlin to Swift

If you are learning KMP architecture, this is the most important module in the whole repo.

## 2.3 `composeApp`

This is the app-side Kotlin UI module for:

- Android
- Desktop JVM

It depends on `shared`.

This module contains:

- Android app entry point
- Android application class
- Android manifest
- Android resources
- Compose UI
- Desktop main function

Important thought:

- `shared` is the reusable engine
- `composeApp` is one consumer of that engine

## 2.4 `iosApp`

This is the native iOS Xcode project.

It is not a Gradle module in the same way as `shared` and `composeApp`.

It contains:

- SwiftUI app entry point
- SwiftUI views
- iOS assets
- iOS plist files
- iOS tests
- Xcode project files

This is how the iPhone app consumes the Kotlin framework produced from the shared module.

## 2.5 `gradle`

This folder contains Gradle infrastructure, especially:

- wrapper files
- version catalog

This is the build system support layer.

## 2.6 `.github`

This contains GitHub Actions CI workflows.

It helps answer:

- what does the repo test
- what does the repo build automatically

## 2.7 `media`

This contains images used in the README.

These are not runtime code files. They are documentation support files.

## 2.8 Generated and tool folders

You will also see folders like:

- `build`
- `.gradle`
- `.idea`
- `.kotlin`

These are not the main learning targets.

They are generated or tool-specific support folders.

As a beginner, do not study these first.

---

