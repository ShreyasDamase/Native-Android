# Chapter 18: iOS App Architecture

## Code Files To Open

- `iosApp/iosApp/RSSApp.swift`
- `iosApp/iosApp/View/RootView.swift`
- `iosApp/iosApp/View/MainFeedView.swift`
- `iosApp/iosApp/View/FeedsList.swift`


## 18.1 Why the iOS app is special in KMP

Android stays in the Kotlin/Gradle world.

iOS is different:

- app shell is native Swift/SwiftUI
- shared Kotlin is compiled into a framework
- Swift calls into that framework

This is the fundamental KMP iOS integration story.

## 18.2 `RSSApp.swift`

This is the iOS app entry point.

It does several critical things:

- initializes Kotlin DI
- gets shared dependencies from Kotlin
- wraps `FeedStore` into a SwiftUI-friendly `ObservableObject`
- injects that object into the environment

This file is the bridge between SwiftUI world and Kotlin shared world.

## 18.3 `ObservableFeedStore`

This class is extremely important for iOS learning.

It:

- owns the shared `FeedStore`
- subscribes to shared state flow
- subscribes to side-effect flow
- publishes updates with `@Published`
- exposes `dispatch(_ action:)`

This turns a Kotlin store into something SwiftUI can naturally consume.

That is one of the most important interop lessons in the repo.

## 18.4 Why `watchState()` and `watchSideEffect()` exist

Swift does not directly consume Kotlin `Flow` as nicely as Compose does.

So the repo adds helper wrappers:

- Kotlin flow is wrapped in `CFlow`
- Swift gets callback-style watch functions

This reduces friction on the iOS side.

## 18.5 `RootView.swift`

This root view:

- hosts the main navigation view
- listens to side effects
- shows temporary error message UI

This is the iOS equivalent of the Compose snackbar strategy.

## 18.6 `MainFeedView.swift`

This is the main iOS content screen.

It uses the `ConnectedView` pattern to:

- map global shared state into view props
- keep view rendering separate from store wiring

This is conceptually similar to container/presentation separation.

## 18.7 `FeedsList.swift`

This is the iOS feed management screen.

It splits feeds into:

- default feeds
- user feeds

and allows:

- adding
- deleting user feeds

## 18.8 Supporting SwiftUI files

Other files serve focused roles:

- `FeedRow.swift`: one feed row
- `PostRow.swift`: one post row
- `StringExtensions.swift`: HTML decoding helper
- `AlertView.swift`: text input alert bridge
- `NavigationLazyView.swift`: lazy navigation helper

These are important for the iOS app, but the main KMP lessons stay centered on:

- `RSSApp.swift`
- the observable store bridge
- the state mapping pattern

---

