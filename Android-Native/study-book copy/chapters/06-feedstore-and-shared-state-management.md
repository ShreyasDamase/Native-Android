# Chapter 6: `FeedStore` and Shared State Management

## Code Files To Open

- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/app/NanoRedux.kt`
- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/app/FeedStore.kt`
- `composeApp/src/commonMain/kotlin/com/github/jetbrains/rssreader/RssReaderApp.kt`
- `iosApp/iosApp/RSSApp.swift`


## 6.1 Why `FeedStore` is one of the most important files

If `RssReader` is the business engine, `FeedStore` is the app brain.

This file contains:

- the state model
- the actions
- the side effects
- the reducer-like logic
- the async orchestration

If you master this file, you will understand the app’s runtime behavior.

## 6.2 `FeedState`

`FeedState` contains:

- `progress: Boolean`
- `feeds: List<RssFeed>`
- `selectedFeed: RssFeed?`

Meaning:

- `progress` tells whether work is in progress
- `feeds` is the loaded feed list
- `selectedFeed` tells which feed filter is active

`selectedFeed = null` means:

- all feeds are selected

That is a small but important state convention.

## 6.3 `mainFeedPosts()`

This helper function converts app state into the actual post list shown on the main screen.

If a feed is selected:

- use only that feed’s items

If no feed is selected:

- flatten all feed items into one list

Then:

- sort by `pubDate` descending

This is a good example of derived state.

The store keeps raw state, and helper functions derive UI-ready data from it.

## 6.4 `FeedAction`

Actions describe things that happen in the app.

The actions are:

- `Refresh(forceLoad)`
- `Add(url)`
- `Delete(url)`
- `SelectFeed(feed)`
- `Data(feeds)`
- `Error(error)`

You can divide them into two groups.

User-initiated or intent actions:

- `Refresh`
- `Add`
- `Delete`
- `SelectFeed`

Result actions:

- `Data`
- `Error`

This is a classic state-management pattern:

- user or system triggers an action
- async work happens
- result comes back as success or failure action

## 6.5 `FeedSideEffect`

The side effect type currently contains:

- `Error(error)`

Why have side effects when state already exists?

Because some events should not become long-lived state.

Example:

- show an error toast/snackbar once

That is not persistent screen state. It is a one-time event.

This is exactly what side effects are for.

## 6.6 Internal state containers

Inside `FeedStore`, two flow-based containers are used:

- `MutableStateFlow` for state
- `MutableSharedFlow` for side effects

Why this split is nice:

- state is current, persistent, replayable
- side effects are event-like, transient

This is a very good practical pattern.

## 6.7 The dispatch function

`dispatch(action)` is the center of the store.

It:

- logs the action
- reads the old state
- decides the new state
- may launch async work
- may emit side effects
- updates the `StateFlow`

This is basically the reducer plus coordinator combined together.

## 6.8 Refresh behavior

When `Refresh` is dispatched:

- if work is already in progress, emit `In progress` error side effect
- otherwise launch `loadAllFeeds(forceLoad)`
- set `progress = true`

This prevents overlapping refresh operations.

## 6.9 Add behavior

When `Add(url)` is dispatched:

- if already busy, emit `In progress`
- otherwise launch `addFeed(url)`
- set progress state to true

Then the async function:

- calls `rssReader.addFeed(url)`
- reloads all feeds from storage
- dispatches `Data`

Important design point:

- after mutation, the store reloads the source of truth
- it does not manually try to patch state in many places

That often leads to cleaner state handling.

## 6.10 Delete behavior

This mirrors add:

- prevent overlapping operations
- launch delete async work
- set progress
- reload all feeds
- dispatch `Data`

## 6.11 Select behavior

`SelectFeed(feed)` is synchronous.

It checks:

- if the feed is `null`, allow it
- if the feed exists in the current feed list, allow it
- otherwise emit `Unknown feed`

This is defensive programming.

## 6.12 Data behavior

`Data(feeds)` means async work finished successfully.

If the old state was in progress:

- set `progress = false`
- replace `feeds`
- preserve selected feed only if that feed still exists

That last part is a subtle but excellent detail.

If the user had selected a feed that no longer exists, the store safely resets selection.

## 6.13 Error behavior

`Error(error)` means async work failed.

If the old state was in progress:

- emit error side effect
- set `progress = false`
- keep old feeds

This is good UX:

- the user sees the old content
- the app stops showing loading
- the error is surfaced

## 6.14 Coroutine scope choice

`FeedStore` delegates to:

- `CoroutineScope(Dispatchers.Main)`

This means store-triggered async work is launched on the main dispatcher context.

That is okay here because:

- Ktor client work is suspend-based
- storage work is light
- this is a sample project

In a larger production app, you may want more explicit dispatcher control and lifecycle-aware cleanup.

## 6.15 Why `FeedStore` is a strong learning file

This file teaches:

- shared app state
- action-driven architecture
- side effects
- defensive state transitions
- async orchestration in KMP

For your future history app, this file is one of the best templates in the repo.

---

