# Chapter 9: Local Storage with `FeedStorage`

## Code Files To Open

- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/datasource/storage/FeedStorage.kt`
- `composeApp/src/androidMain/kotlin/com/github/jetbrains/rssreader/core/RssReader.kt`
- `shared/src/iosMain/kotlin/com/github/jetbrains/rssreader/KoinHelper.kt`
- `composeApp/src/jvmMain/kotlin/com/github/jetbrains/rssreader/Main.kt`


## 9.1 What `FeedStorage` is

`FeedStorage` is the local persistence layer.

It stores feeds in:

- key-value settings storage

The feeds are serialized as:

- JSON string containing a list of `RssFeed`

This is a lightweight cache strategy.

## 9.2 Why this design is educational

This sample does not use:

- SQLDelight
- Room
- Core Data
- Realm

Instead it uses a much simpler persistence model.

That is good for a learning sample because it lets you focus on architecture first.

## 9.3 Disk cache and memory cache

`FeedStorage` has:

- `diskCache`
- `memCache`

`diskCache` reads/writes the serialized JSON from settings.

`memCache` is a lazy in-memory mutable map initialized from disk.

This means:

- reads after startup can be fast
- writes update both memory and persistent storage

## 9.4 The key used for storage

The key is:

- `key_feed_cache`

This is the settings key under which the serialized feed list lives.

## 9.5 Methods

The file exposes:

- `getFeed(url)`
- `saveFeed(feed)`
- `deleteFeed(url)`
- `getAllFeeds()`

These are simple CRUD-style cache operations.

## 9.6 How storage differs by platform

The API in `FeedStorage` is shared.

But the actual settings backend differs:

- Android uses `SharedPreferencesSettings`
- iOS uses `NSUserDefaultsSettings`
- Desktop uses `PropertiesSettings`

This is a beautiful KMP lesson:

- the storage service logic stays shared
- the platform storage provider is injected

That is exactly the kind of architecture you want to learn.

---

