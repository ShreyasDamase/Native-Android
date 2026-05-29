# Native Android Coverage Audit

This audit uses these references without modifying them:

- `JetPack Compose/🏔️ Goal.md`
- `Android Interview Questions — Study Checklist`

The purpose of this file is to track note coverage inside `Android-Native/Native Android`.

---

## Coverage Summary

### Strong Coverage

- Kotlin coroutines and Flow
- Jetpack Compose fundamentals, state, side effects, and many UI primitives
- Room
- DataStore
- Hilt and Koin
- Basic Activity, Fragment, Service, BroadcastReceiver, and Android OS internals

### Partial Coverage

- Intents and Activity results
- ContentProvider and FileProvider
- MVVM and architecture
- Retrofit and networking basics
- Build system basics

### Missing or Weak Coverage

- Runtime permissions as a dedicated production guide
- `PendingIntent`
- `Parcelable` vs `Serializable` for Android component data passing
- RecyclerView deep dive
- DiffUtil and ListAdapter
- Paging 3 as a dedicated guide
- WorkManager as a dedicated guide
- AlarmManager exact-alarm usage
- Foreground services vs WorkManager decision-making
- Firebase Cloud Messaging and notification channels
- Testing stack: Espresso, Robolectric, MockWebServer, Compose testing strategy
- Performance tooling: LeakCanary, Macrobenchmark, Baseline Profiles, startup profiling
- Security topics: Keystore, encrypted storage, network security config, certificate pinning
- Device APIs: CameraX, FusedLocationProviderClient, BLE, BiometricPrompt, Media3
- Release topics: R8/ProGuard, signing, staged rollout, Play production workflow

---

## First Batch Added

- `Android Basis/Runtime Permissions, PendingIntent, and Parcelable.md`
- `Android Basis/RecyclerView, DiffUtil, and Paging 3.md`
- `Android Basis/WorkManager, AlarmManager, and Foreground Services.md`
- `Android Basis/Push Notifications with FCM.md`

---

## Recommended Next Batch

### Interview-Critical

- Testing in Android: unit, integration, Espresso, Robolectric, MockWebServer, Compose tests
- Performance and memory: ANR, LeakCanary, Profiler, overdraw, startup
- Network security and local security: Keystore, EncryptedSharedPreferences, TLS, pinning

### Core Android Gaps

- View system and custom views
- ContentProvider and MediaStore deep dive
- Build and release: R8, signing, AAB, staged rollout

### Advanced APIs

- CameraX
- Location
- Bluetooth BLE
- Biometrics
- Media3 / ExoPlayer

---

## Working Rule

When a topic already exists in a deep note, we should prefer strengthening that note only if it is clearly the canonical place. If not, create a dedicated topic note in `Native Android` so interview revision stays fast.
