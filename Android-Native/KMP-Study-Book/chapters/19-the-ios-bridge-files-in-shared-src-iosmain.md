# Chapter 19: The iOS Bridge Files in `shared/src/iosMain`

## Code Files To Open

- `shared/src/iosMain/kotlin/com/github/jetbrains/rssreader/KoinHelper.kt`
- `shared/src/iosMain/kotlin/com/github/jetbrains/rssreader/app/IosReduxUtils.kt`
- `shared/src/iosMain/kotlin/com/github/jetbrains/rssreader/core/CFlow.kt`
- `iosApp/iosApp/RSSApp.swift`


## 19.1 Why `iosMain` exists inside `shared`

Even though the iOS app UI is written in Swift, the shared Kotlin module still needs some iOS-specific helper code.

That code lives in:

- `shared/src/iosMain`

This is a very useful KMP pattern.

## 19.2 `KoinHelper.kt`

This file:

- defines the Koin module for iOS shared services
- starts Koin
- exposes injected objects via a helper class

This is what lets Swift call into Kotlin and ask for:

- `RssReader`
- `FeedStore`

without manually constructing everything in Swift.

## 19.3 `IosReduxUtils.kt`

This file adds:

- `watchState()`
- `watchSideEffect()`

to `FeedStore`.

It is tiny, but it makes Swift integration cleaner.

## 19.4 `CFlow.kt`

This file wraps Kotlin `Flow` and exposes:

- `watch(block)`

This creates a cancelable subscription from Swift-friendly code.

Conceptually:

- Kotlin emits values
- Swift callback receives values
- returned `Closeable` lets Swift stop watching

This is a beautiful example of interop adaptation.

---

