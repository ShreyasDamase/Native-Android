# Chapter 4: The Module Architecture

## Code Files To Open

- `shared/build.gradle.kts`
- `composeApp/build.gradle.kts`
- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/app/FeedStore.kt`
- `composeApp/src/commonMain/kotlin/com/github/jetbrains/rssreader/RssReaderApp.kt`
- `iosApp/iosApp/RSSApp.swift`


## 4.1 How the modules depend on each other

The dependency direction is:

- `composeApp -> shared`
- `iosApp -> shared framework`

The `shared` module does not depend on the UI modules.

That is good architecture.

Why?

- business logic should not know about screens
- storage should not know about navigation
- network code should not know about buttons

This separation keeps the app maintainable.

## 4.2 The architectural layers inside `shared`

The shared module can be understood as these layers:

- domain models
- data sources
- business/use-case layer
- app state layer
- settings/config layer

These are not separate Gradle modules, but they are separated by packages and responsibilities.

## 4.3 Domain layer

This is represented mainly by the RSS model classes.

Its job:

- describe the shape of app data
- remain mostly UI-independent

Examples:

- `RssFeed`
- `Channel`
- `Item`
- `Image`
- `MediaContent`

## 4.4 Data layer

This layer includes:

- network loading
- local persistence

In this repo:

- `FeedLoader` is the network data source
- `FeedStorage` is the local storage data source

## 4.5 Business layer

This is centered around `RssReader`.

Its job:

- orchestrate storage and network
- decide when to refresh
- decide default feed behavior
- expose app-friendly functions like `getAllFeeds`, `addFeed`, `deleteFeed`

## 4.6 State layer

This is centered around `FeedStore`.

Its job:

- hold the current app state
- accept actions
- run async work
- update state
- emit side effects

This is the part that lets both Android and iOS use the same state logic.

## 4.7 Platform bridge layer

For iOS, there are helper classes in `iosMain` to make Kotlin flows easier to consume from Swift.

This layer exists because:

- Kotlin and Swift interop is powerful
- but some Kotlin patterns need wrapping for a nice Swift experience

This is why `CFlow` and Koin helper files exist.

---

