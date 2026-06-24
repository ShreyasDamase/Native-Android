 # Birthday Reminder - Updated Project Structure with iOS Liquid Glass Navigation

```
birthdayreminder/
│
├── shared/
│   ├── src/
│   │   ├── commonMain/
│   │   │   ├── kotlin/com/birthdayreminder/
│   │   │   │   ├── domain/
│   │   │   │   │   ├── entity/
│   │   │   │   │   │   ├── Birthday.kt
│   │   │   │   │   │   ├── Profile.kt
│   │   │   │   │   │   ├── Settings.kt
│   │   │   │   │   │   ├── User.kt
│   │   │   │   │   │   ├── ReminderRule.kt
│   │   │   │   │   │   └── Friend.kt
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── BirthdayRepository.kt
│   │   │   │   │   │   ├── ProfileRepository.kt
│   │   │   │   │   │   ├── SettingsRepository.kt
│   │   │   │   │   │   ├── ContactRepository.kt
│   │   │   │   │   │   └── AuthRepository.kt
│   │   │   │   │   └── usecase/
│   │   │   │   │       ├── onboarding/
│   │   │   │   │       │   ├── CheckOnboardingStatusUseCase.kt
│   │   │   │   │       │   ├── SaveProfileUseCase.kt
│   │   │   │   │       │   ├── CreateAccountUseCase.kt
│   │   │   │   │       │   └── CompleteOnboardingUseCase.kt
│   │   │   │   │       ├── birthday/
│   │   │   │   │       │   ├── GetUpcomingBirthdaysUseCase.kt
│   │   │   │   │       │   ├── GetAllBirthdaysUseCase.kt
│   │   │   │   │       │   ├── AddBirthdayUseCase.kt
│   │   │   │   │       │   ├── EditBirthdayUseCase.kt
│   │   │   │   │       │   ├── DeleteBirthdayUseCase.kt
│   │   │   │   │       │   └── ScheduleReminderUseCase.kt
│   │   │   │   │       ├── contacts/
│   │   │   │   │       │   ├── ReadDeviceContactsUseCase.kt
│   │   │   │   │       │   ├── SyncContactsUseCase.kt
│   │   │   │   │       │   └── GetMatchedFriendsUseCase.kt
│   │   │   │   │       ├── settings/
│   │   │   │   │       │   ├── GetSettingsUseCase.kt
│   │   │   │   │       │   └── UpdateSettingsUseCase.kt
│   │   │   │   │       └── profile/
│   │   │   │   │           ├── GetProfileUseCase.kt
│   │   │   │   │           ├── UpdateProfileUseCase.kt
│   │   │   │   │           └── LogoutUseCase.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── database/
│   │   │   │   │   │   │   ├── BirthdayDatabase.kt (expect)
│   │   │   │   │   │   │   ├── BirthdayDao.kt
│   │   │   │   │   │   │   ├── GiftIdeaDao.kt
│   │   │   │   │   │   │   └── FriendDao.kt
│   │   │   │   │   │   └── datastore/
│   │   │   │   │   │       ├── SettingsDataStore.kt
│   │   │   │   │   │       └── ProfileDataStore.kt
│   │   │   │   │   ├── remote/
│   │   │   │   │   │   ├── api/
│   │   │   │   │   │   │   ├── AuthApi.kt
│   │   │   │   │   │   │   ├── BirthdayApi.kt
│   │   │   │   │   │   │   └── ContactApi.kt
│   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   │   ├── BirthdayDto.kt
│   │   │   │   │   │   │   └── ContactSyncRequest.kt
│   │   │   │   │   │   └── interceptor/
│   │   │   │   │   │       ├── AuthInterceptor.kt
│   │   │   │   │   │       └── LoggingInterceptor.kt
│   │   │   │   │   ├── mapper/
│   │   │   │   │   │   ├── BirthdayMapper.kt
│   │   │   │   │   │   └── ProfileMapper.kt
│   │   │   │   │   └── repository/
│   │   │   │   │       ├── BirthdayRepositoryImpl.kt
│   │   │   │   │       ├── ProfileRepositoryImpl.kt
│   │   │   │   │       ├── SettingsRepositoryImpl.kt
│   │   │   │   │       ├── ContactRepositoryImpl.kt
│   │   │   │   │       └── AuthRepositoryImpl.kt
│   │   │   │   ├── presentation/
│   │   │   │   │   ├── common/
│   │   │   │   │   │   ├── theme/
│   │   │   │   │   │   │   ├── Color.kt
│   │   │   │   │   │   │   ├── Typography.kt
│   │   │   │   │   │   │   └── Theme.kt
│   │   │   │   │   │   ├── navigation/                          🆕 NEW NAVIGATION FOLDER
│   │   │   │   │   │   │   ├── NavigationDestinations.kt        🆕 Sealed class routing
│   │   │   │   │   │   │   ├── NativeTabNavigation.kt           🆕 NativeTab enum for iOS
│   │   │   │   │   │   │   ├── TabBackstackManager.kt           🆕 Per-tab backstack logic
│   │   │   │   │   │   │   ├── NavigationCallbacks.kt           🆕 Callbacks to SwiftUI
│   │   │   │   │   │   │   ├── NavGraph.kt                      (Updated for Navigation 3)
│   │   │   │   │   │   │   ├── Screen.kt                        (Deprecated - use Destinations)
│   │   │   │   │   │   │   └── NavController.kt                 (Deprecated - use TabBackstackManager)
│   │   │   │   │   │   ├── components/
│   │   │   │   │   │   │   ├── CommonButton.kt
│   │   │   │   │   │   │   ├── CommonTextField.kt
│   │   │   │   │   │   │   ├── LoadingIndicator.kt
│   │   │   │   │   │   │   ├── ErrorDialog.kt
│   │   │   │   │   │   │   ├── ComposeTabBar.kt                 🆕 Compose-based tab bar (iOS 18-25 fallback)
│   │   │   │   │   │   │   └── NavigationScreenRouter.kt        🆕 Routes screens based on destination
│   │   │   │   │   │   └── util/
│   │   │   │   │   │       ├── Extensions.kt
│   │   │   │   │   │       └── Constants.kt
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── BirthdayUiModel.kt
│   │   │   │   │   │   ├── ProfileUiModel.kt
│   │   │   │   │   │   ├── SettingsUiModel.kt
│   │   │   │   │   │   └── UiState.kt
│   │   │   │   │   ├── onboarding/
│   │   │   │   │   │   ├── OnboardingScreen.kt
│   │   │   │   │   │   ├── OnboardingViewModel.kt
│   │   │   │   │   │   ├── OnboardingState.kt
│   │   │   │   │   │   └── OnboardingEvent.kt
│   │   │   │   │   ├── home/
│   │   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   │   ├── HomeViewModel.kt
│   │   │   │   │   │   ├── HomeState.kt
│   │   │   │   │   │   └── HomeEvent.kt
│   │   │   │   │   ├── explore/                                  🆕 NEW SCREEN (Maps to NativeTab.EXPLORE)
│   │   │   │   │   │   ├── ExploreScreen.kt
│   │   │   │   │   │   ├── ExploreViewModel.kt
│   │   │   │   │   │   ├── ExploreState.kt
│   │   │   │   │   │   └── ExploreEvent.kt
│   │   │   │   │   ├── addbirthday/
│   │   │   │   │   │   ├── AddBirthdayScreen.kt
│   │   │   │   │   │   ├── AddBirthdayViewModel.kt
│   │   │   │   │   │   ├── AddBirthdayState.kt
│   │   │   │   │   │   └── AddBirthdayEvent.kt
│   │   │   │   │   ├── detail/
│   │   │   │   │   │   ├── BirthdayDetailScreen.kt
│   │   │   │   │   │   ├── BirthdayDetailViewModel.kt
│   │   │   │   │   │   ├── BirthdayDetailState.kt
│   │   │   │   │   │   └── BirthdayDetailEvent.kt
│   │   │   │   │   ├── calendar/
│   │   │   │   │   │   ├── CalendarScreen.kt
│   │   │   │   │   │   ├── CalendarViewModel.kt
│   │   │   │   │   │   ├── CalendarState.kt
│   │   │   │   │   │   └── CalendarEvent.kt
│   │   │   │   │   ├── favorites/                                🆕 NEW SCREEN (Maps to NativeTab.FAVORITES)
│   │   │   │   │   │   ├── FavoritesScreen.kt
│   │   │   │   │   │   ├── FavoritesViewModel.kt
│   │   │   │   │   │   ├── FavoritesState.kt
│   │   │   │   │   │   └── FavoritesEvent.kt
│   │   │   │   │   ├── profile/
│   │   │   │   │   │   ├── ProfileScreen.kt
│   │   │   │   │   │   ├── ProfileViewModel.kt
│   │   │   │   │   │   ├── ProfileState.kt
│   │   │   │   │   │   └── ProfileEvent.kt
│   │   │   │   │   └── settings/
│   │   │   │   │       ├── SettingsScreen.kt
│   │   │   │   │       ├── SettingsViewModel.kt
│   │   │   │   │       ├── SettingsState.kt
│   │   │   │   │       └── SettingsEvent.kt
│   │   │   │   ├── di/
│   │   │   │   │   ├── AppModule.kt
│   │   │   │   │   ├── NetworkModule.kt
│   │   │   │   │   ├── DatabaseModule.kt
│   │   │   │   │   ├── RepositoryModule.kt
│   │   │   │   │   ├── UseCaseModule.kt
│   │   │   │   │   └── ViewModelModule.kt
│   │   │   │   ├── platform/
│   │   │   │   │   ├── PermissionManager.kt (expect)
│   │   │   │   │   ├── ContactReader.kt (expect)
│   │   │   │   │   ├── NotificationHandler.kt (expect)
│   │   │   │   │   └── ReminderScheduler.kt (expect)
│   │   │   │   ├── worker/
│   │   │   │   │   ├── ReminderWorker.kt (expect)
│   │   │   │   │   └── SyncContactsWorker.kt (expect)
│   │   │   │   ├── util/
│   │   │   │   │   ├── DateUtils.kt
│   │   │   │   │   ├── StringUtils.kt
│   │   │   │   │   ├── Logger.kt
│   │   │   │   │   └── Validators.kt
│   │   │   │   └── App.kt                                        (Updated for native navigation detection)
│   │   │   └── composeResources/
│   │   │       └── drawable/
│   │   ├── androidMain/
│   │   │   └── kotlin/com/birthdayreminder/
│   │   │       ├── platform/
│   │   │       │   ├── PermissionManager.android.kt
│   │   │       │   ├── ContactReader.android.kt
│   │   │       │   ├── NotificationHandler.android.kt
│   │   │       │   └── ReminderScheduler.android.kt
│   │   │       ├── worker/
│   │   │       │   ├── ReminderWorker.kt
│   │   │       │   └── SyncContactsWorker.kt
│   │   │       ├── data/
│   │   │       │   ├── local/
│   │   │       │   │   └── BirthdayDatabase.android.kt
│   │   │       │   └── remote/
│   │   │       │       └── ApiClient.android.kt
│   │   │       └── AndroidContext.kt
│   │   ├── iosMain/
│   │   │   └── kotlin/com/birthdayreminder/
│   │   │       ├── platform/
│   │   │       │   ├── PermissionManager.ios.kt
│   │   │       │   ├── ContactReader.ios.kt
│   │   │       │   ├── NotificationHandler.ios.kt
│   │   │       │   └── ReminderScheduler.ios.kt
│   │   │       ├── data/
│   │   │       │   ├── local/
│   │   │       │   │   └── BirthdayDatabase.ios.kt
│   │   │       │   └── remote/
│   │   │       │       └── ApiClient.ios.kt
│   │   │       ├── navigation/                                   🆕 iOS-SPECIFIC NAVIGATION
│   │   │       │   └── MainViewController.kt                    🆕 Entry point for ComposeUIViewController
│   │   │       └── IosContext.kt
│   │   ├── jvmMain/
│   │   │   └── kotlin/com/birthdayreminder/
│   │   │       ├── platform/
│   │   │       │   ├── PermissionManager.jvm.kt
│   │   │       │   ├── ContactReader.jvm.kt
│   │   │       │   ├── NotificationHandler.jvm.kt
│   │   │       │   └── ReminderScheduler.jvm.kt
│   │   │       ├── data/
│   │   │       │   ├── local/
│   │   │       │   │   └── BirthdayDatabase.jvm.kt
│   │   │       │   └── remote/
│   │   │       │       └── ApiClient.jvm.kt
│   │   │       └── DesktopContext.kt
│   │   ├── androidTest/
│   │   │   └── kotlin/com/birthdayreminder/
│   │   │       ├── domain/
│   │   │       ├── data/
│   │   │       └── presentation/
│   │   ├── commonTest/
│   │   │   └── kotlin/com/birthdayreminder/
│   │   │       ├── navigation/                                   🆕 NEW
│   │   │       │   └── TabBackstackManagerTest.kt
│   │   │       ├── domain/
│   │   │       ├── data/
│   │   │       └── presentation/
│   │   ├── iosTest/
│   │   │   └── kotlin/com/birthdayreminder/
│   │   │       └── platform/
│   │   └── jvmTest/
│   │       └── kotlin/com/birthdayreminder/
│   │           └── platform/
│   └── build.gradle.kts                                           (Updated with Navigation 3 + SwiftUI deps)
│
├── androidApp/
│   ├── src/
│   │   └── main/
│   │       ├── kotlin/com/birthdayreminder/
│   │       │   └── MainActivity.kt
│   │       ├── AndroidManifest.xml
│   │       └── res/
│   │           ├── drawable/
│   │           ├── mipmap/
│   │           └── values/
│   └── build.gradle.kts
│
├── iosApp/                                                        🆕 UPDATED iOS App Structure
│   ├── iosApp/
│   │   ├── iOSApp.swift                                          🆕 Version check + route to proper view
│   │   ├── ContentView.swift                                     🆕 Legacy fallback (iOS 18-25)
│   │   ├── navigation/                                           🆕 NEW NAVIGATION FOLDER
│   │   │   ├── LiquidGlassTabView.swift                         🆕 SwiftUI TabView (iOS 26+)
│   │   │   ├── MainViewControllerHolder.swift                   🆕 UIViewControllerRepresentable bridge
│   │   │   └── NavigationBridge.swift                           🆕 Callbacks from Compose to SwiftUI
│   │   ├── util/                                                 🆕 NEW UTILITY FOLDER
│   │   │   └── PreviewHelper.swift                              🆕 Preview support
│   │   ├── Assets.xcassets/
│   │   ├── Info.plist
│   │   └── Localization/                                        🆕 NEW (if needed)
│   │       └── Localizable.strings
│   └── iosApp.xcodeproj/
│       ├── project.pbxproj
│       ├── xcuserdata/
│       └── project.xcworkspace/
│           └── xcshareddata/
│
├── desktopApp/
│   ├── src/
│   │   └── main/
│   │       └── kotlin/com/birthdayreminder/
│   │           └── main.kt
│   └── build.gradle.kts
│
├── gradle/
│   ├── libs.versions.toml                                         (Updated with Navigation 3)
│   └── wrapper/
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── README.md                                                       (Updated with iOS 26 navigation info)
```

---

## 📋 File Addition Summary

### 🆕 New Kotlin Files (commonMain - Navigation)

|File|Purpose|Type|
|---|---|---|
|`NavigationDestinations.kt`|Type-safe routing with sealed class|Core|
|`NativeTabNavigation.kt`|iOS tab enum & state management|iOS-specific|
|`TabBackstackManager.kt`|Per-tab backstack logic|Core|
|`NavigationCallbacks.kt`|Bridge callbacks to SwiftUI|iOS-specific|
|`ComposeTabBar.kt`|Compose tab bar (fallback)|UI Component|
|`NavigationScreenRouter.kt`|Routes screens based on destination|Core|

### 🆕 New Kotlin Files (iosMain)

|File|Purpose|
|---|---|
|`MainViewController.kt`|Creates ComposeUIViewController entry point|

### 🆕 New Swift Files (iOS App)

|File|Purpose|Availability|
|---|---|---|
|`LiquidGlassTabView.swift`|Native SwiftUI TabView with glass effect|iOS 26+|
|`MainViewControllerHolder.swift`|Bridges SwiftUI to Compose UIViewController|All iOS|
|`NavigationBridge.swift`|Handles callbacks from Compose|All iOS|

### 🆕 New Test Files

|File|Purpose|
|---|---|
|`TabBackstackManagerTest.kt`|Unit tests for backstack logic|

---

## 📝 Updated Files

### Kotlin (commonMain)

|File|Changes|
|---|---|
|`App.kt`|Added `useNativeNavigation` parameter + iOS version detection|
|`build.gradle.kts`|Added Navigation 3 + SwiftUI interop dependencies|

### Swift (iOS App)

|File|Changes|
|---|---|
|`iOSApp.swift`|Checks iOS version and routes to appropriate view|
|`ContentView.swift`|Now serves as fallback for iOS 18-25|
|`Info.plist`|iOS 26 minimum for native navigation features|

---

## 🔧 File Organization Rationale

### Why This Structure?

```
commonMain/
├── navigation/                    # Shared navigation logic
│   ├── NavigationDestinations.kt  # All platforms: how to define routes
│   ├── NativeTabNavigation.kt     # iOS-specific: tab definitions
│   ├── TabBackstackManager.kt     # All platforms: backstack logic
│   └── ...
└── common/components/
    ├── ComposeTabBar.kt           # Fallback for iOS 18-25

iosMain/
└── navigation/
    └── MainViewController.kt       # iOS entry point only

iosApp/
└── navigation/
    ├── LiquidGlassTabView.swift   # SwiftUI implementation (iOS 26+ only)
    ├── MainViewControllerHolder.swift  # Bridge pattern
    └── NavigationBridge.swift     # Callbacks
```

### Benefits

1. **Separation of Concerns**: Navigation logic in `commonMain`, UI in `iosApp`
2. **Platform Detection**: Version checks at Swift layer; Kotlin layer agnostic
3. **Code Reuse**: `TabBackstackManager` used on all platforms
4. **Maintainability**: Changes to navigation don't require dual updates

---

## 📦 Gradle Dependencies to Add

**`shared/build.gradle.kts`**:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Navigation 3 (Multiplatform)
            implementation(libs.navigation3.ui)
            implementation(libs.navigation3.viewmodel)
            
            // Compose Multiplatform (if not already present)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
    }
}
```

**`gradle/libs.versions.toml`**:

```toml
[versions]
compose = "1.8.0"
navigation3 = "1.0.0"
kotlin = "2.1.0"

[libraries]
# Compose
compose-foundation = { module = "androidx.compose.foundation:foundation", version.ref = "compose" }
compose-material3 = { module = "androidx.compose.material3:material3", version.ref = "compose" }
compose-ui = { module = "androidx.compose.ui:ui", version.ref = "compose" }

# Navigation 3 (Multiplatform)
navigation3-ui = { module = "org.jetbrains.androidx.navigation3:navigation3-ui", version.ref = "navigation3" }
navigation3-viewmodel = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "navigation3" }
```

---

## 📋 Migration Checklist

### Phase 1: Add Navigation Infrastructure

- [ ] Create `NavigationDestinations.kt`
- [ ] Create `NativeTabNavigation.kt`
- [ ] Create `TabBackstackManager.kt`
- [ ] Create `NavigationCallbacks.kt`
- [ ] Create `ComposeTabBar.kt`
- [ ] Create `NavigationScreenRouter.kt`

### Phase 2: Update Kotlin App Layer

- [ ] Update `App.kt` with version detection
- [ ] Add `useNativeNavigation` parameter
- [ ] Integrate `TabBackstackManager`

### Phase 3: Create iOS Navigation

- [ ] Create `MainViewController.kt` (iosMain)
- [ ] Create `LiquidGlassTabView.swift` (iOS 26+)
- [ ] Create `MainViewControllerHolder.swift`
- [ ] Create `NavigationBridge.swift`

### Phase 4: Update iOS App

- [ ] Update `iOSApp.swift` with version check
- [ ] Update `ContentView.swift` as fallback
- [ ] Update `Info.plist` deployment target

### Phase 5: Testing

- [ ] Create `TabBackstackManagerTest.kt`
- [ ] Test iOS 18-25 on device/simulator
- [ ] Test iOS 26+ on device/simulator
- [ ] Test tab switching behavior
- [ ] Test back navigation per tab

### Phase 6: Cleanup

- [ ] Remove deprecated `NavGraph.kt` references
- [ ] Update `NavController.kt` to use new system
- [ ] Add navigation documentation to README.md

---

## 🚀 Implementation Order

### Step 1: Foundation (Day 1)

1. Update gradle with Navigation 3
2. Add `NavigationDestinations.kt`
3. Add `NativeTabNavigation.kt`
4. Add `TabBackstackManager.kt`

### Step 2: Compose Layer (Day 2)

5. Update `App.kt`
6. Add `NavigationScreenRouter.kt`
7. Add `ComposeTabBar.kt` (fallback)

### Step 3: iOS Kotlin (Day 3)

8. Create `MainViewController.kt` (iosMain)
9. Add `NavigationCallbacks.kt`

### Step 4: SwiftUI Layer (Day 4)

10. Create `LiquidGlassTabView.swift`
11. Create `MainViewControllerHolder.swift`
12. Create `NavigationBridge.swift`

### Step 5: Integration (Day 5)

13. Update `iOSApp.swift`
14. Update `ContentView.swift`
15. Add version detection

### Step 6: Testing & Polish (Day 6)

16. Test all platforms
17. Add unit tests
18. Update documentation

---

## 🎯 Key Relationships

```
App.kt (commonMain)
├── useNativeNavigation: Boolean (parameter from iOS)
├── currentNativeTab: String (from SwiftUI)
└── NavigationScreenRouter
    ├── TabBackstackManager
    │   └── backstacks: Map<NativeTab, List<NavigationDestination>>
    └── ComposeScreens
        ├── HomeScreen
        ├── ExploreScreen
        ├── FavoritesScreen
        └── ProfileScreen

MainViewController.kt (iosMain)
└── Creates ComposeUIViewController
    └── Calls App(useNativeNavigation = true)

LiquidGlassTabView.swift (iOS 26+)
├── TabView(selection: $selectedTab)
├── Home → MainViewControllerHolder
├── Explore → MainViewControllerHolder
├── Favorites → MainViewControllerHolder
└── Profile → MainViewControllerHolder

MainViewControllerHolder.swift
└── UIViewControllerRepresentable
    └── MainViewControllerKt.MainViewController()
        └── ComposeUIViewController
```

---

## 📖 Documentation Updates

### README.md should include:

```markdown
## Navigation Architecture

### iOS 26+ (Native Liquid Glass)
- SwiftUI TabView handles tab switching
- Compose handles screen rendering
- Native blur effect on tab bar
- Per-tab independent backstacks

### iOS 18-25 & Other Platforms
- Compose handles both tabs and routing
- Simulated blur effect
- Single global backstack

### Key Files
- `NavigationDestinations.kt` - Type-safe routing
- `TabBackstackManager.kt` - Per-tab backstack
- `LiquidGlassTabView.swift` - Native iOS 26+ navigation
- `MainViewController.kt` - iOS entry point
```

---

## ✅ Summary of Changes

|Aspect|Before|After|
|---|---|---|
|**Navigation**|String-based routes|Type-safe sealed classes|
|**iOS Tabs**|Compose NavigationBar|SwiftUI TabView (iOS 26+)|
|**Glass Effect**|Manual blur filter|Native system behavior|
|**Backstack**|Single global|Per-tab independent|
|**Test Coverage**|None|TabBackstackManagerTest|
|**Files**|~45|~52 (+7 navigation)|
|**iOS Version Support**|18-26|18+ (with native 26+)|

---

**Created**: March 2026 | **CMP Version**: 1.8.0+ | **Navigation**: 3.0+