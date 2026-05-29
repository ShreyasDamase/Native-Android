# Chapter 27: Important Design Lessons You Should Carry Forward

## Code Files To Open

- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/app/FeedStore.kt`
- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/core/RssReader.kt`
- `composeApp/src/androidMain/kotlin/com/github/jetbrains/rssreader/core/RssReader.kt`
- `shared/src/iosMain/kotlin/com/github/jetbrains/rssreader/KoinHelper.kt`


## 27.1 Share logic, not confusion

Only share code that actually benefits from being shared.

This repo shares:

- business logic
- app state
- models
- data layer

That is usually high-value sharing.

## 27.2 Keep platform-specific construction out of core business logic

The repo does this well.

Example:

- `RssReader` is shared
- `buildRssReader(context, withLog)` is Android-specific

This is a pattern worth copying.

## 27.3 Separate long-lived state from one-time events

The split between:

- `FeedState`
- `FeedSideEffect`

is very important.

This prevents many UI bugs.

## 27.4 Prefer small, understandable architecture over over-engineering

This sample is not huge, but it is teachable.

That is a strength.

As a beginner, clarity beats complexity.

## 27.5 Let source sets communicate architecture

File placement itself should tell a story:

- `commonMain` if reusable across all architectures
- `androidMain` if relying on Android specifically (OkHttp, SharedPreferences, Intents, etc.)
- `iosMain` if relying on Darwin or Swift interoperability
- `jvmMain` if desktop-specific JVM logic

> [!TIP]
> This repo enforces a clean rule: never put UI dependencies inside `shared`. Keep `shared` entirely oblivious of whether it will be painted by Compose or SwiftUI. Let `composeApp` and `iosApp` handle all of the visual rendering.

---

