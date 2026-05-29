# Chapter 25: A Practical Reading Order for Learning

## Code Files To Open

- `README.md`
- `settings.gradle.kts`
- `shared/build.gradle.kts`
- `composeApp/build.gradle.kts`
- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/app/FeedStore.kt`
- `iosApp/iosApp/RSSApp.swift`


## 25.1 Best reading order if you are a beginner

Read in this order:

1. `README.md`
2. `settings.gradle.kts`
3. `shared/build.gradle.kts`
4. `composeApp/build.gradle.kts`
5. `shared/.../app/NanoRedux.kt`
6. `shared/.../app/FeedStore.kt`
7. `shared/.../core/RssReader.kt`
8. `shared/.../datasource/network/FeedLoader.kt`
9. `shared/.../datasource/storage/FeedStorage.kt`
10. `shared/.../domain/RssFeed.kt`
11. `composeApp/.../App.kt`
12. `composeApp/.../AppActivity.kt`
13. `composeApp/.../RssReaderApp.kt`
14. `composeApp/.../ui/Screen.kt`
15. `composeApp/.../ui/MainFeed.kt`
16. `composeApp/.../ui/FeedList.kt`
17. `iosApp/iosApp/RSSApp.swift`
18. `shared/src/iosMain/.../KoinHelper.kt`
19. `shared/src/iosMain/.../CFlow.kt`
20. remaining SwiftUI files

## 25.2 Why this order works

Because it follows the natural stack:

- project setup
- build setup
- shared architecture
- shared runtime logic
- Android consumption
- iOS consumption

This is a much better order than randomly opening files.

---

