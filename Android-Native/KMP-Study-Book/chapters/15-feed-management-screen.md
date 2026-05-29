# Chapter 15: Feed Management Screen

## Code Files To Open

- `composeApp/src/commonMain/kotlin/com/github/jetbrains/rssreader/ui/FeedList.kt`
- `composeApp/src/commonMain/kotlin/com/github/jetbrains/rssreader/ui/Dialogs.kt`
- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/app/FeedStore.kt`


## 15.1 `FeedListScreen()`

This is a thin function that:

- injects `FeedStore`
- passes it to `FeedList`

This is a simple screen entry wrapper.

## 15.2 `FeedList`

Responsibilities:

- show current feeds
- show add dialog
- show delete dialog
- dispatch add/delete actions

This is the feed-management UI.

## 15.3 Add feed flow

When the floating action button is pressed:

- add dialog opens

When the user confirms:

- URL is normalized from `http://` to `https://`
- `FeedAction.Add(url)` is dispatched

That normalization is a tiny but useful implementation detail.

## 15.4 Delete feed flow

When a feed is tapped in the list:

- delete dialog opens

Important behavior:

- default feeds are not clickable for deletion

This respects the built-in/default feed rule.

## 15.5 `FeedItem`

Each feed item shows:

- feed icon
- title
- description

This is simple but enough for a management screen.

---

