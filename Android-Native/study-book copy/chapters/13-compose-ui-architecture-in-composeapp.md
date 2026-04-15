# Chapter 13: Compose UI Architecture in `composeApp`

## Code Files To Open

- `composeApp/src/commonMain/kotlin/com/github/jetbrains/rssreader/RssReaderApp.kt`
- `composeApp/src/commonMain/kotlin/com/github/jetbrains/rssreader/ui/Screen.kt`
- `composeApp/src/commonMain/kotlin/com/github/jetbrains/rssreader/ui/AppTheme.kt`


## 13.1 Why Compose code lives in `commonMain`

The Compose UI for Android and Desktop is shared between those platforms.

That is why many UI files live in:

- `composeApp/src/commonMain`

This means:

- same Compose UI can run on Android and desktop
- Android-specific host code still lives in `androidMain`
- Desktop-specific host code still lives in `jvmMain`

## 13.2 `RssReaderApp.kt`

This is the root UI container.

Responsibilities:

- apply theme
- create nav controller
- show app bar
- host snackbar
- define navigation graph
- collect error side effects and show snackbar messages

This file is both layout shell and app-level UI coordinator.

## 13.3 Navigation

The app uses a very simple screen enum:

- `Main`
- `FeedList`

And `NavHost` connects them.

That keeps navigation beginner-friendly.

## 13.4 Why `Screen.kt` contains more than screen constants

This file contains:

- screen enum
- top app bar
- `MainScreen()`
- `FeedListScreen()`

This is a somewhat compact sample structure rather than a heavily separated production structure.

As a learner, do not assume every project will organize screens exactly like this.

Instead learn the intent:

- root navigation wiring
- screen-level state subscription
- event dispatching

## 13.5 Error handling in Compose

The root app collects `FeedSideEffect.Error` and shows it in a snackbar.

That is a great example of one-time event handling in Compose.

The UI does not keep an error message permanently in state just to show a toast.

Instead:

- side effect flow emits event
- UI consumes event
- snackbar shows once

That is a clean pattern.

---

