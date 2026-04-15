# Chapter 12: Android App Lifecycle in This Repository

## Code Files To Open

- `composeApp/src/androidMain/AndroidManifest.xml`
- `composeApp/src/androidMain/kotlin/com/github/jetbrains/rssreader/App.kt`
- `composeApp/src/androidMain/kotlin/com/github/jetbrains/rssreader/AppActivity.kt`
- `composeApp/src/commonMain/kotlin/com/github/jetbrains/rssreader/RssReaderApp.kt`
- `composeApp/src/commonMain/kotlin/com/github/jetbrains/rssreader/ui/Screen.kt`
- `composeApp/src/androidMain/kotlin/com/github/jetbrains/rssreader/sync/RefreshWorker.kt`


## 12.1 Android manifest

The Android manifest declares:

- internet permission
- custom application class
- launcher activity
- app icons and theme

This is the Android system registration layer.

## 12.2 `App.kt`

This is the Android `Application` subclass.

It is the first app-level Kotlin class Android creates.

Its `onCreate()` does two important things:

- starts Koin
- launches background sync scheduling

This is a major lifecycle point.

Before the activity screen exists, app-wide dependencies are already prepared.

## 12.3 `AppActivity.kt`

This is the Android activity entry point.

Its `onCreate()`:

- enables edge-to-edge
- installs splash screen
- calls `setContent { RssReaderApp() }`

This is the moment where the Android view world becomes the Compose world.

## 12.4 `RssReaderApp()`

This is the root Compose app container.

It creates:

- theme
- scaffold
- top app bar
- snackbar host
- navigation host

This is the UI shell.

## 12.5 First visible loading flow on Android

When `MainScreen()` first appears:

- it gets the shared `FeedStore` from Koin
- it starts collecting state
- it dispatches `FeedAction.Refresh(false)` inside `LaunchedEffect(Unit)`

That causes the first feed load.

This is the real Android runtime flow:

`Application -> Activity -> Compose root -> MainScreen -> Store refresh -> Shared load -> State update -> Recompose`

## 12.6 Pull-to-refresh

The main screen uses pull-to-refresh.

When the user refreshes:

- `FeedAction.Refresh(true)` is dispatched

The difference is:

- `true` forces network refresh
- `false` allows cache-first logic

This is a simple but important bit of app behavior.

## 12.7 Android background sync

`RefreshWorker` schedules periodic refresh through WorkManager.

Its job:

- call `rssReader.getAllFeeds(true)`

This keeps feeds refreshed in the background on Android.

This feature is Android-specific and does not exist in the same way for iOS in this sample.

## 12.8 Android-specific `buildRssReader`

This helper constructs the shared `RssReader` with Android-specific dependencies:

- `SharedPreferences`
- Android context access
- Napier debug logging

This is a perfect example of how shared logic is created using platform-specific building blocks.

---

