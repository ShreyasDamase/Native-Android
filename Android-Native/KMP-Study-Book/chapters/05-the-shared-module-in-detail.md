# Chapter 5: The Shared Module in Detail

## Code Files To Open

- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/Settings.kt`
- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/app/NanoRedux.kt`
- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/app/FeedStore.kt`
- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/core/RssReader.kt`
- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/domain/RssFeed.kt`


## 5.1 Why `shared` is the most important module

If you only study one module deeply, study `shared`.

It contains the real reusable logic of the app.

The UI modules mostly consume what `shared` exposes.

## 5.2 The package layout inside `shared`

Main packages include:

- `com.github.jetbrains.rssreader`
- `com.github.jetbrains.rssreader.app`
- `com.github.jetbrains.rssreader.core`
- `com.github.jetbrains.rssreader.datasource.network`
- `com.github.jetbrains.rssreader.datasource.storage`
- `com.github.jetbrains.rssreader.domain`

Each package has a fairly clean responsibility.

## 5.3 `Settings.kt`

This file is tiny but conceptually important.

It wraps:

- default feed URLs

and exposes:

- `isDefault(feedUrl)`

This is simple configuration, not persistent settings.

It is used to know whether a feed is one of the built-in feeds.

Why does that matter?

- default feeds may be treated differently in the UI
- for example, deletion may be restricted

## 5.4 `NanoRedux.kt`

This is a minimal interface layer defining:

- `State`
- `Action`
- `Effect`
- `Store`

This is not a full Redux framework. It is a tiny architecture contract.

This teaches a very nice KMP lesson:

- you do not always need a big framework
- sometimes a small shared abstraction is enough

The `Store` interface says a store must:

- expose a `StateFlow` of state
- expose a flow of side effects
- accept dispatched actions

That is the state contract for the whole app.

---

