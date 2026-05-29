# Appendix A: Fast File Map

## Root

- `README.md`: project explanation
- `settings.gradle.kts`: module inclusion
- `build.gradle.kts`: root plugin setup
- `gradle/libs.versions.toml`: dependency versions
- `gradle.properties`: build flags

## Shared module

- `shared/build.gradle.kts`: multiplatform library definition
- `shared/src/commonMain/.../Settings.kt`: default feed config
- `shared/src/commonMain/.../app/NanoRedux.kt`: store contracts
- `shared/src/commonMain/.../app/FeedStore.kt`: shared app state logic
- `shared/src/commonMain/.../core/HttpClient.kt`: Ktor XML client
- `shared/src/commonMain/.../core/RssReader.kt`: shared business/service logic
- `shared/src/commonMain/.../datasource/network/FeedLoader.kt`: network fetcher
- `shared/src/commonMain/.../datasource/storage/FeedStorage.kt`: storage layer
- `shared/src/commonMain/.../domain/RssFeed.kt`: RSS models
- `shared/src/iosMain/.../KoinHelper.kt`: iOS DI bridge
- `shared/src/iosMain/.../IosReduxUtils.kt`: iOS flow helpers
- `shared/src/iosMain/.../core/CFlow.kt`: flow-to-Swift wrapper

## Compose app

- `composeApp/build.gradle.kts`: Android/Desktop app module
- `composeApp/src/commonMain/.../RssReaderApp.kt`: root Compose app shell
- `composeApp/src/commonMain/.../ui/Screen.kt`: screens and app bar
- `composeApp/src/commonMain/.../ui/MainFeed.kt`: main feed content
- `composeApp/src/commonMain/.../ui/FeedList.kt`: feed management UI
- `composeApp/src/commonMain/.../ui/PostList.kt`: post list UI
- `composeApp/src/commonMain/.../ui/Dialogs.kt`: add/delete dialogs
- `composeApp/src/commonMain/.../ui/FeedIcon.kt`: feed icon components
- `composeApp/src/commonMain/.../ui/AppTheme.kt`: theme
- `composeApp/src/commonMain/.../ui/Previews.kt`: preview data
- `composeApp/src/androidMain/.../App.kt`: Android application class
- `composeApp/src/androidMain/.../AppActivity.kt`: Android entry activity
- `composeApp/src/androidMain/.../core/RssReader.kt`: Android-specific builder
- `composeApp/src/androidMain/.../sync/RefreshWorker.kt`: Android background sync
- `composeApp/src/jvmMain/.../Main.kt`: desktop entry point

## iOS app

- `iosApp/iosApp/RSSApp.swift`: SwiftUI app entry point
- `iosApp/iosApp/View/RootView.swift`: root view and side-effect toast
- `iosApp/iosApp/View/MainFeedView.swift`: main content view
- `iosApp/iosApp/View/FeedsList.swift`: feed list management
- `iosApp/iosApp/View/FeedRow.swift`: feed row UI
- `iosApp/iosApp/View/PostRow.swift`: article row UI
- `iosApp/iosApp/View/StringExtensions.swift`: HTML decode helper
- `iosApp/iosApp/View/Basic/AlertView.swift`: text alert bridge
- `iosApp/iosApp/View/Basic/NavigationLazyView.swift`: lazy navigation helper

---

