# Chapter 7: `RssReader` as the Shared Business Layer

## Code Files To Open

- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/core/RssReader.kt`
- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/Settings.kt`
- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/datasource/network/FeedLoader.kt`
- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/datasource/storage/FeedStorage.kt`


## 7.1 What `RssReader` does

`RssReader` is the main shared service/use-case class.

It combines:

- `FeedLoader`
- `FeedStorage`
- `Settings`

It decides:

- when to use cached data
- when to load from network
- what the default feeds are

This means `RssReader` is not just a loader. It is a coordinator.

## 7.2 Constructor dependencies

It receives:

- `feedLoader`
- `feedStorage`
- `settings`

This is dependency injection by constructor.

That gives it these strengths:

- testability
- platform flexibility
- clear separation of concerns

## 7.3 `getAllFeeds(forceUpdate)`

This is the most important function in the file.

Its behavior is:

1. read all feeds from storage
2. if storage is empty or force update is true, fetch from network
3. save newly fetched feeds to storage
4. return the feed list

This is a simple cache-first strategy.

## 7.4 Default-feed logic

If storage is empty:

- use `settings.defaultFeedUrls`

If storage already has feeds:

- use the `sourceUrl` values from stored feeds

This means first launch behavior is different from later launches.

That is a key app lifecycle detail.

First launch:

- app knows only built-in default feeds

Later launches:

- app reloads whatever feeds are already stored

## 7.5 Parallel loading

The method uses `mapAsync` with coroutines.

This means multiple feed URLs can be fetched concurrently.

This is a nice touch because it improves load performance when several feeds exist.

It also shows a great KMP lesson:

- common Kotlin coroutines can coordinate concurrency across platforms

## 7.6 `addFeed(url)`

This:

- loads one feed from network
- marks whether it is default
- stores it

There is no extra business complication here.

It is intentionally simple.

## 7.7 `deleteFeed(url)`

This:

- removes the feed from storage

Also simple by design.

## 7.8 Why `RssReader` is useful for your future app

For your history app, this class suggests a pattern like:

- `HistoryReader`
- `HistoryRepository`
- `HistoryStore`

Where shared business logic can orchestrate:

- network source
- local source
- app rules

without caring about Android and iOS screens.

---

