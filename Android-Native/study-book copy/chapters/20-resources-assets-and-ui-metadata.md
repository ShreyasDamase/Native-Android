# Chapter 20: Resources, Assets, and UI Metadata

## Code Files To Open

- `composeApp/src/commonMain/composeResources/values/strings.xml`
- `composeApp/src/androidMain/res/values/strings.xml`
- `composeApp/src/androidMain/res/values/themes.xml`
- `iosApp/iosApp/Info.plist`
- `iosApp/iosApp/Assets.xcassets/Contents.json`


## 20.1 Compose shared resources

In `composeApp/src/commonMain/composeResources/values/strings.xml`, the project defines shared UI strings such as:

- app name
- feed list title
- add/remove text
- all
- back button

This allows Compose resource access in shared UI code.

## 20.2 Android resources

Android-specific resources include:

- manifest strings
- themes
- launcher icons
- vector drawables
- splash resources

These live in:

- `composeApp/src/androidMain/res`

These are Android platform app resources, not shared KMP resources.

## 20.3 iOS assets

iOS assets include:

- app icons
- color assets
- launch screen

These live under the Xcode app directory and are managed the iOS way.

## 20.4 Why resource handling differs

This difference is normal in KMP:

- some resources can be shared
- some platform app metadata must remain native

For example:

- Android manifest and themes are Android-specific
- iOS plist and asset catalogs are iOS-specific

---

