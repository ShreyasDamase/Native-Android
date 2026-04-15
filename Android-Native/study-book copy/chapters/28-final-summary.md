# Chapter 28: Final Summary

## Code Files To Open

- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/app/FeedStore.kt`
- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/core/RssReader.kt`
- `composeApp/src/androidMain/kotlin/com/github/jetbrains/rssreader/App.kt`
- `iosApp/iosApp/RSSApp.swift`


This repository is a study-worthy first KMP app because it demonstrates a real multiplatform architecture without drowning you in unnecessary complexity.

The most important things to remember are:

- `shared` is the true heart of the project
- `FeedStore` is the shared state brain
- `RssReader` is the shared business coordinator
- `FeedLoader` and `FeedStorage` are the data layer
- Android and iOS are consumers of the shared engine
- Android uses Compose and app-level Kotlin wiring
- iOS uses SwiftUI plus a Kotlin bridge layer
- source sets explain what code is shared and what is platform-specific

If you understand those ideas deeply, you are not just learning this sample. You are learning the foundation of how to structure your own KMP application.

---

