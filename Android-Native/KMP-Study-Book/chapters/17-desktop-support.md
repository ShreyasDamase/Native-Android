# Chapter 17: Desktop Support

## Code Files To Open

- `composeApp/src/jvmMain/kotlin/com/github/jetbrains/rssreader/Main.kt`
- `composeApp/src/commonMain/kotlin/com/github/jetbrains/rssreader/RssReaderApp.kt`
- `shared/build.gradle.kts`
- `composeApp/build.gradle.kts`


## 17.1 Why desktop exists here

This sample is not only Android+iOS. It also includes a JVM desktop target in the Compose app module.

That means the shared logic and Compose UI can also run as a desktop app.

## 17.2 `Main.kt`

Desktop startup does:

- start Koin
- construct dependencies
- open a Compose window
- render `RssReaderApp()`

So the same Compose root used by Android is reused for desktop.

That is a powerful demonstration of shared UI inside the Kotlin ecosystem.

## 17.3 Desktop storage

Desktop uses:

- `PropertiesSettings(Properties())`

This is a simple settings backend.

It keeps the sample easy to run without introducing a bigger desktop persistence system.

## 17.4 What this teaches you

KMP plus Compose Multiplatform can let you share:

- app logic
- possibly some UI

across more than mobile platforms.

Even if your current target is Android+iOS, this repo quietly teaches extensibility.

---

