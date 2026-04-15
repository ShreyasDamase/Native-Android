# Chapter 16: Compose UI Components and Utility Files

## Code Files To Open

- `composeApp/src/commonMain/kotlin/com/github/jetbrains/rssreader/ui/Dialogs.kt`
- `composeApp/src/commonMain/kotlin/com/github/jetbrains/rssreader/ui/PostList.kt`
- `composeApp/src/commonMain/kotlin/com/github/jetbrains/rssreader/ui/FeedIcon.kt`
- `composeApp/src/commonMain/kotlin/com/github/jetbrains/rssreader/ui/AppTheme.kt`
- `composeApp/src/commonMain/kotlin/com/github/jetbrains/rssreader/ui/Previews.kt`


## 16.1 `Dialogs.kt`

Contains:

- `AddFeedDialog`
- `DeleteFeedDialog`

Both are Compose `Dialog` wrappers with lightweight content.

These are straightforward examples of modal UI.

## 16.2 `PostList.kt`

Contains:

- `PostList`
- `PostItem`

This handles:

- lazy scrolling
- draggable interaction
- card layout for articles
- optional image loading with Coil
- text truncation
- pubDate display

## 16.3 `FeedIcon.kt`

Contains:

- circular feed icon UI
- selected-state styling
- text fallback initials
- edit icon component

This file teaches reusable UI component design.

## 16.4 `AppTheme.kt`

This defines a light and dark Material 3 color scheme.

This sample keeps theme logic minimal.

It is enough to show:

- theme wrapper
- color scheme selection
- material surface root

## 16.5 `Previews.kt`

This contains Compose preview data and preview composables.

Its job:

- let developers preview components without running the full app

This is a developer productivity file, not app runtime logic.

Still, it is valuable:

- preview files teach how to make UI work independently of live backend state

---

