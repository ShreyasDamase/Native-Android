# Chapter 11: Dependency Injection in This Project

## Code Files To Open

- `composeApp/src/androidMain/kotlin/com/github/jetbrains/rssreader/App.kt`
- `composeApp/src/androidMain/kotlin/com/github/jetbrains/rssreader/core/RssReader.kt`
- `shared/src/iosMain/kotlin/com/github/jetbrains/rssreader/KoinHelper.kt`
- `composeApp/src/jvmMain/kotlin/com/github/jetbrains/rssreader/Main.kt`


## 11.1 Which DI library is used

The app uses:

- Koin

Koin is a lightweight DI library and fits well for samples and many real apps.

## 11.2 Android DI setup

On Android, DI is started in the custom `Application` class.

That class provides:

- `RssReader`
- `FeedStore`

`RssReader` is built using the Android-specific helper `buildRssReader`.

## 11.3 iOS DI setup

On iOS, DI is started in the Kotlin `initKoin()` helper exposed to Swift.

The iOS Koin module provides:

- `RssReader`
- `FeedStorage`
- `FeedStore`
- `FeedLoader`
- `HttpClient`

This is needed because the iOS app still wants to consume the same shared dependencies, just through Swift.

## 11.4 Desktop DI setup

On desktop, DI is started in `Main.kt`.

This mirrors the same idea:

- create the dependency graph
- then start the UI

## 11.5 Why DI matters here

DI enables:

- platform-specific construction
- shared usage
- clean architecture
- easier future testing

KMP apps often need this because some parts are shared but some concrete implementations are platform-specific.

---

