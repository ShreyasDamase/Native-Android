# Chapter 24: How to Adapt This Repo for Your History App

## Code Files To Open

- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/app/FeedStore.kt`
- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/core/RssReader.kt`
- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/domain/RssFeed.kt`
- `composeApp/src/commonMain/kotlin/com/github/jetbrains/rssreader/RssReaderApp.kt`


## 24.1 The idea transfer

Your app is different in domain, but many architecture ideas can be reused almost directly.

Possible translation:

- `RssReader` becomes `HistoryRepository` or `HistoryEngine`
- `FeedStore` becomes `HistoryStore`
- `RssFeed` becomes your domain models like `HistoricalPeriod`, `Event`, `Figure`, `TimelineEntry`
- `FeedLoader` becomes API/network loader for history content
- `FeedStorage` becomes local storage for cached history content

## 24.2 What should stay shared

For your history app, the following are excellent candidates for shared code:

- API models
- domain models
- repository/use-case layer
- local storage layer
- state store
- filtering/sorting logic
- bookmarks/favorites/history logic

## 24.3 What can remain native

Depending on your choice, native layers can still handle:

- navigation shells
- platform-specific widgets
- notifications
- background tasks
- sharing intents
- platform-specific polish

## 24.4 A likely package structure for your app

A future shared package structure could look like:

- `domain`
- `datasource/network`
- `datasource/storage`
- `repository`
- `app/store`
- `model`
- `settings`

This repo gives you a solid starting point for that.

## 24.5 Biggest caution while adapting

Do not rename RSS classes and stop there.

Instead, carry over the architectural ideas:

- state flow
- side effects
- repository orchestration
- platform-specific dependency assembly
- shared logic, native shell

That is the real value.

---

