# Chapter 26: Frequently Confusing Points for Beginners

## Code Files To Open

- `settings.gradle.kts`
- `composeApp/build.gradle.kts`
- `shared/src/iosMain/kotlin/com/github/jetbrains/rssreader/core/CFlow.kt`
- `iosApp/iosApp/RSSApp.swift`


## 26.1 “Why is iOS not in `settings.gradle.kts`?”

Because the iOS app itself is an Xcode project, not a regular Gradle application module.

Gradle builds the shared framework for iOS.
Xcode builds the final iOS app shell.

## 26.2 “Why do Android and iOS both use the same `FeedStore`?”

Because `FeedStore` is pure shared Kotlin state logic. Both UIs can dispatch actions and observe state from it.

## 26.3 “Why is Compose code in `commonMain`?”

Because that UI is shared across Android and desktop, both of which can run Compose Multiplatform.

## 26.4 “Why is SwiftUI not shared?”

Because this sample chose native iOS UI. That is a deliberate architecture choice, not a limitation of shared logic.

## 26.5 “Why is storage shared if actual backends differ?”

Because the storage service API and behavior are shared, while the settings implementation is platform-specific and injected.

## 26.6 “Why do we need `CFlow`?”

Because Kotlin flows need a friendlier bridge for Swift consumption.

## 26.7 “Why is there a store at all?”

Because once apps have async loading, selection, refreshing, add/delete, and error reporting, a central state manager becomes very valuable.

---

