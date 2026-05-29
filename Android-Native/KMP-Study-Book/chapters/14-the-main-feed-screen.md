# Chapter 14: The Main Feed Screen

## Code Files To Open

- `composeApp/src/commonMain/kotlin/com/github/jetbrains/rssreader/ui/Screen.kt`
- `composeApp/src/commonMain/kotlin/com/github/jetbrains/rssreader/ui/MainFeed.kt`
- `composeApp/src/commonMain/kotlin/com/github/jetbrains/rssreader/ui/PostList.kt`
- `composeApp/src/commonMain/kotlin/com/github/jetbrains/rssreader/ui/FeedIcon.kt`


## 14.1 `MainScreen()`

This function:

- injects `FeedStore`
- observes state
- opens URLs through `LocalUriHandler`
- triggers initial refresh
- hosts pull-to-refresh
- delegates actual content rendering to `MainFeed`

This is screen-controller style Compose code.

## 14.2 `MainFeed()`

This function builds the main content area:

- post list
- bottom feed selector bar
- edit button access

It also calculates the current posts list from state.

Important note:

This derived posts calculation is UI-side, even though there is also a helper in the store file.

In a production refactor you might centralize that more, but for learning this is still readable.

## 14.3 Feed selection bottom bar

The bottom bar includes:

- All
- one icon per feed
- Edit icon

This gives the user quick filtering and feed management access.

## 14.4 Scroll reset behavior

When a different feed is selected:

- the post list scrolls back to the top

This is a subtle UX improvement.

It shows attention to user experience.

## 14.5 Post opening

When a post is clicked:

- its `link` is opened via `LocalUriHandler`

This keeps the app simple. The app does not build an internal article reader screen.

---

