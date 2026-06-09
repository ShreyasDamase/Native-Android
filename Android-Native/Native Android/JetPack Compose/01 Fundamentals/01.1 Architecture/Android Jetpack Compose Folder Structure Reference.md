# Android Jetpack Compose — Folder Structure Reference

---

## How to Read This Guide

Every file has an arrow explanation like this:

```
└── UserDao.kt        ← WHAT it is | WHY it lives here | WHAT it contains
```

**Lost & don't know where to put a file?** → Jump to the [Lost File? Lookup Table](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#lost-file-lookup) at the bottom.

---

---

# VARIANT 1 — Full Stack App (Room + Retrofit + Hilt)

> Use when: app has both a backend API AND local database (most real-world apps)

```
com.yourapp/
│
├── MyApplication.kt
│   └── ← Entry point of the entire app
│       WHY here: Hilt needs @HiltAndroidApp on Application to generate DI code
│       Contains: @HiltAndroidApp annotation, app-level setup (Timber, etc.)
│
├── MainActivity.kt
│   └── ← The one and only Activity in your app
│       WHY here: Root level because it's not data/domain/ui — it IS the app shell
│       Contains: @AndroidEntryPoint, setContent { }, NavHost setup
│
│
├── data/                         ← EVERYTHING that touches external sources
│   │                                Rule: ViewModel never imports anything from here directly
│   │
│   ├── local/                    ← Anything saved ON the device
│   │   │
│   │   ├── dao/
│   │   │   └── UserDao.kt        ← The "question asker" for your database
│   │   │                            WHY in dao/: All database queries live together
│   │   │                            Contains: @Dao interface, @Query, @Insert, @Delete functions
│   │   │
│   │   ├── entity/
│   │   │   └── UserEntity.kt     ← The "table blueprint" for Room
│   │   │                            WHY in entity/: Separate from domain model on purpose
│   │   │                            Contains: @Entity, @PrimaryKey, column fields
│   │   │                            ⚠️ Never use this class in ViewModel or UI
│   │   │
│   │   └── AppDatabase.kt        ← The database itself
│   │                                WHY at local/ root: It's the container for all DAOs
│   │                                Contains: @Database(entities=[...]), abstract dao() functions
│   │
│   ├── remote/                   ← Anything that goes OVER the network
│   │   │
│   │   ├── api/
│   │   │   └── UserApi.kt        ← The "menu" of available API endpoints
│   │   │                            WHY in api/: All Retrofit interfaces grouped together
│   │   │                            Contains: @GET, @POST, @PUT, @DELETE functions
│   │   │                            Returns: DTOs (never domain models)
│   │   │
│   │   └── dto/
│   │       └── UserDto.kt        ← The "JSON shape" — matches exactly what server sends
│   │                                WHY in dto/: Isolated so API changes don't ripple into app
│   │                                Contains: @SerializedName fields, nullable types for safety
│   │                                ⚠️ Never use this class in ViewModel or UI
│   │
│   ├── repository/
│   │   └── UserRepositoryImpl.kt ← The "middleman" that combines local + remote
│   │                                WHY in data/: It's the implementation detail (how to fetch)
│   │                                WHY not in domain/: domain only has the interface (contract)
│   │                                Contains: implements UserRepository interface from domain/
│   │                                Does: reads Room → maps to domain model → returns to UseCase
│   │
│   └── mapper/
│       └── UserMapper.kt         ← The "translator" between layers
│                                    WHY its own folder: Keeps Entity/DTO classes out of domain
│                                    Contains: extension functions
│                                    UserDto.toDomain() → converts API response to clean model
│                                    UserEntity.toDomain() → converts DB row to clean model
│                                    User.toEntity() → converts domain model to DB row
│
│
├── domain/                       ← PURE BUSINESS LOGIC — zero Android imports allowed here
│   │                                Rule: No Context, no Room, no Retrofit, no Hilt in this folder
│   │
│   ├── model/
│   │   └── User.kt               ← The "clean version" of your data
│   │                                WHY in domain/: This is what your app actually works with
│   │                                Contains: plain data class, only fields your UI/logic needs
│   │                                ⚠️ No @Entity, no @SerializedName — just pure Kotlin
│   │
│   ├── repository/
│   │   └── UserRepository.kt     ← The "contract" — what operations are possible
│   │                                WHY in domain/: domain defines WHAT, data defines HOW
│   │                                Contains: interface with function signatures only
│   │                                WHY interface: lets you swap implementations (Room→Firebase)
│   │
│   └── usecase/
│       ├── GetUsersUseCase.kt     ← One specific thing the app can do
│       │                            WHY exists: keeps ViewModel thin, logic is testable alone
│       │                            Contains: calls repository, applies business rules
│       │                            Rule: one file = one action
│       │
│       └── LoginUseCase.kt        ← Another specific action
│                                    WHY separate file: easy to find, easy to test in isolation
│
│
├── presentation/                 ← EVERYTHING the user sees and interacts with
│   │                                Rule: no direct Room/Retrofit calls here — only ViewModel
│   │
│   ├── ui/
│   │   │
│   │   ├── home/                 ← One folder per screen
│   │   │   │
│   │   │   ├── HomeScreen.kt     ← The Composable UI for this screen
│   │   │   │                        WHY with ViewModel: easy to find both at once
│   │   │   │                        Contains: @Composable functions, observes uiState
│   │   │   │
│   │   │   ├── HomeViewModel.kt  ← The "brain" of this screen
│   │   │   │                        WHY with Screen: they are a pair, never separated
│   │   │   │                        Contains: @HiltViewModel, StateFlow<HomeUiState>
│   │   │   │                        Calls: UseCases (never Repository directly)
│   │   │   │
│   │   │   └── HomeUiState.kt    ← A snapshot of what the screen should show
│   │   │                            WHY separate file: clear contract between VM and UI
│   │   │                            Contains: data class with isLoading, error, list fields
│   │   │
│   │   ├── detail/               ← Same pattern for every screen
│   │   │   ├── DetailScreen.kt
│   │   │   └── DetailViewModel.kt
│   │   │
│   │   └── components/           ← Reusable UI pieces used across multiple screens
│   │       ├── UserCard.kt       ← Used in HomeScreen AND maybe SearchScreen
│   │       └── LoadingIndicator.kt ← Used everywhere — lives here not in home/
│   │                                WHY its own folder: if it's used in 2+ screens → move here
│   │
│   ├── navigation/
│   │   ├── NavGraph.kt           ← The "map" of all screens and how to reach them
│   │   │                            WHY in navigation/: separate from screens for clarity
│   │   │                            Contains: NavHost, all composable() destinations
│   │   │
│   │   └── Screen.kt             ← The "names" of every destination
│   │                                WHY separate: single source of truth for route strings
│   │                                Contains: sealed class with route string per screen
│   │
│   └── theme/
│       ├── Color.kt              ← All color values for the whole app
│       ├── Typography.kt         ← All text styles
│       └── Theme.kt              ← MaterialTheme wrapper — used in MainActivity
│
│
└── di/                           ← HILT WIRING — tells Hilt how to build your objects
    │                                Rule: nothing outside di/ should know how objects are created
    │
    ├── NetworkModule.kt          ← "Here is how to build Retrofit and API services"
    │                                WHY needed: you don't own Retrofit, Hilt can't auto-create it
    │                                Contains: @Provides for OkHttpClient, Retrofit, UserApi
    │
    ├── DatabaseModule.kt         ← "Here is how to build Room and its DAOs"
    │                                WHY needed: same reason — you don't own Room
    │                                Contains: @Provides for AppDatabase, UserDao
    │
    └── RepositoryModule.kt       ← "When someone asks for UserRepository, give UserRepositoryImpl"
                                     WHY needed: domain has interface, data has impl — Hilt needs to know which
                                     Contains: @Binds abstract functions
```

---

---

# VARIANT 2 — API Only (No Local Database)

> Use when: simple app, data doesn't need offline support, always fetch fresh

```
com.yourapp/
│
├── MyApplication.kt              ← @HiltAndroidApp
├── MainActivity.kt               ← @AndroidEntryPoint, NavHost
│
├── data/
│   ├── remote/                   ← only remote, no local/ folder needed
│   │   ├── api/
│   │   │   └── PostApi.kt        ← Retrofit interface for posts
│   │   └── dto/
│   │       └── PostDto.kt        ← JSON shape from server
│   ├── repository/
│   │   └── PostRepositoryImpl.kt ← fetches from API, maps to domain model
│   └── mapper/
│       └── PostMapper.kt         ← PostDto.toDomain()
│
├── domain/
│   ├── model/
│   │   └── Post.kt               ← clean model ViewModel works with
│   ├── repository/
│   │   └── PostRepository.kt     ← interface: suspend fun getPosts(): List<Post>
│   └── usecase/
│       └── GetPostsUseCase.kt
│
├── presentation/
│   ├── ui/
│   │   ├── feed/
│   │   │   ├── FeedScreen.kt
│   │   │   ├── FeedViewModel.kt
│   │   │   └── FeedUiState.kt
│   │   └── components/
│   ├── navigation/
│   └── theme/
│
└── di/
    ├── NetworkModule.kt          ← still needed (Retrofit)
    └── RepositoryModule.kt       ← still needed (@Binds)
    ✗ No DatabaseModule.kt        ← skip — no Room
```

---

---

# VARIANT 3 — Room Only (Offline App, No Network)

> Use when: note app, todo app, diary — fully local, no server

```
com.yourapp/
│
├── MyApplication.kt              ← @HiltAndroidApp
├── MainActivity.kt               ← @AndroidEntryPoint, NavHost
│
├── data/
│   ├── local/                    ← only local, no remote/ folder needed
│   │   ├── dao/
│   │   │   └── NoteDao.kt        ← @Insert, @Delete, @Query for notes
│   │   ├── entity/
│   │   │   └── NoteEntity.kt     ← @Entity, maps to "notes" table
│   │   └── AppDatabase.kt        ← @Database(entities=[NoteEntity::class])
│   ├── repository/
│   │   └── NoteRepositoryImpl.kt ← reads from Room, maps to domain Note
│   └── mapper/
│       └── NoteMapper.kt         ← NoteEntity.toDomain(), Note.toEntity()
│
├── domain/
│   ├── model/
│   │   └── Note.kt               ← clean model: id, title, body, createdAt
│   ├── repository/
│   │   └── NoteRepository.kt     ← interface: Flow<List<Note>>, add, delete
│   └── usecase/
│       ├── GetNotesUseCase.kt
│       ├── AddNoteUseCase.kt
│       └── DeleteNoteUseCase.kt  ← one file per action
│
├── presentation/
│   ├── ui/
│   │   ├── notelist/
│   │   │   ├── NoteListScreen.kt
│   │   │   ├── NoteListViewModel.kt
│   │   │   └── NoteListUiState.kt
│   │   └── addnote/
│   │       ├── AddNoteScreen.kt
│   │       └── AddNoteViewModel.kt
│   ├── navigation/
│   └── theme/
│
└── di/
    ├── DatabaseModule.kt         ← still needed (Room)
    └── RepositoryModule.kt       ← still needed (@Binds)
    ✗ No NetworkModule.kt         ← skip — no Retrofit
```

---

---

# VARIANT 4 — App with Authentication

> Use when: app has login/register screens and a JWT or session token

```
com.yourapp/
│
├── MyApplication.kt
├── MainActivity.kt               ← checks if logged in → navigates to Login or Home
│
├── data/
│   ├── remote/
│   │   ├── api/
│   │   │   ├── AuthApi.kt        ← login(), register(), refreshToken() endpoints
│   │   │   └── UserApi.kt        ← getProfile(), updateProfile() endpoints
│   │   └── dto/
│   │       ├── LoginRequestDto.kt   ← { email, password } sent TO server
│   │       ├── LoginResponseDto.kt  ← { token, userId } received FROM server
│   │       └── UserDto.kt
│   │
│   ├── local/
│   │   └── preferences/
│   │       └── TokenManager.kt   ← saves/reads JWT token from DataStore
│   │                                WHY here not in di/: it's a data source, not DI config
│   │                                Used by: NetworkModule (adds token to request headers)
│   │
│   ├── repository/
│   │   ├── AuthRepositoryImpl.kt ← calls AuthApi, saves token via TokenManager
│   │   └── UserRepositoryImpl.kt
│   └── mapper/
│       └── UserMapper.kt
│
├── domain/
│   ├── model/
│   │   └── User.kt
│   ├── repository/
│   │   ├── AuthRepository.kt     ← interface: login(), logout(), isLoggedIn()
│   │   └── UserRepository.kt
│   └── usecase/
│       ├── LoginUseCase.kt       ← validates input, calls AuthRepository
│       ├── LogoutUseCase.kt      ← clears token, clears local DB if needed
│       ├── RegisterUseCase.kt
│       └── IsLoggedInUseCase.kt  ← checked on app start to decide first screen
│
├── presentation/
│   ├── ui/
│   │   ├── auth/                 ← group auth screens together
│   │   │   ├── login/
│   │   │   │   ├── LoginScreen.kt
│   │   │   │   ├── LoginViewModel.kt
│   │   │   │   └── LoginUiState.kt
│   │   │   └── register/
│   │   │       ├── RegisterScreen.kt
│   │   │       └── RegisterViewModel.kt
│   │   └── home/
│   │       ├── HomeScreen.kt
│   │       └── HomeViewModel.kt
│   ├── navigation/
│   │   ├── NavGraph.kt           ← has two graphs: AuthGraph and MainGraph
│   │   └── Screen.kt
│   └── theme/
│
└── di/
    ├── NetworkModule.kt          ← OkHttpClient reads token from TokenManager for headers
    ├── DataStoreModule.kt        ← provides DataStore<Preferences> instance
    └── RepositoryModule.kt       ← @Binds AuthRepository, UserRepository
```

---

---

# VARIANT 5 — App with Background Work (WorkManager)

> Use when: periodic sync, upload queue, downloading files, scheduled tasks

```
com.yourapp/
│
├── MyApplication.kt              ← @HiltAndroidApp + implements Configuration.Provider
│                                    WHY different: WorkManager needs HiltWorkerFactory injected here
│
├── data/
│   ├── local/    ...
│   ├── remote/   ...
│   ├── repository/ ...
│   │
│   └── worker/                   ← background task definitions
│       ├── SyncWorker.kt         ← @HiltWorker, extends CoroutineWorker
│       │                            WHY in data/: it's a data operation (sync/upload)
│       │                            WHY not in presentation/: user doesn't trigger it directly
│       │                            Contains: doWork() — calls repository, returns Result
│       │
│       └── UploadWorker.kt       ← another background task
│
├── domain/   ...
├── presentation/   ...
│
└── di/
    ├── NetworkModule.kt
    ├── DatabaseModule.kt
    ├── RepositoryModule.kt
    └── WorkerModule.kt           ← tells WorkManager to use HiltWorkerFactory
                                     WHY needed: without this, Hilt can't inject into Workers
```

---

---

# VARIANT 6 — App with Foreground Service (Music / Location / Camera)

> Use when: something must run while app is in background and show a notification

```
com.yourapp/
│
├── MyApplication.kt
├── MainActivity.kt
│
├── data/   ...
├── domain/   ...
├── presentation/   ...
│
├── service/                      ← long-running operations visible to user
│   ├── MusicPlayerService.kt     ← extends Service (or LifecycleService)
│   │                                WHY not in data/: it's not a data source, it's a process
│   │                                WHY not in presentation/: has no UI itself
│   │                                ⚠️ Must be declared in AndroidManifest.xml
│   │                                ⚠️ Add @AndroidEntryPoint for Hilt injection
│   │
│   └── LocationTrackingService.kt
│
└── di/   ...
```

---

---

# VARIANT 7 — Large App, Feature-First

> Use when: multiple developers, many screens, want strict boundaries between features

```
com.yourapp/
│
├── core/                         ← shared code used by ALL features
│   ├── network/                  ← Retrofit setup lives here
│   │   ├── RetrofitClient.kt     ← OkHttpClient, Gson, base URL
│   │   └── NetworkModule.kt
│   ├── database/                 ← Room setup lives here
│   │   ├── AppDatabase.kt        ← references entities from ALL features
│   │   └── DatabaseModule.kt
│   ├── preferences/
│   │   └── DataStoreModule.kt
│   └── ui/
│       ├── components/           ← Button, LoadingIndicator used by all features
│       └── theme/
│
├── feature/                      ← one sub-folder per feature
│   │
│   ├── auth/                     ← everything login/register needs
│   │   ├── data/
│   │   │   ├── api/AuthApi.kt
│   │   │   ├── dto/LoginDto.kt
│   │   │   ├── repository/AuthRepositoryImpl.kt
│   │   │   └── mapper/AuthMapper.kt
│   │   ├── domain/
│   │   │   ├── model/AuthUser.kt
│   │   │   ├── repository/AuthRepository.kt
│   │   │   └── usecase/LoginUseCase.kt
│   │   ├── presentation/
│   │   │   ├── login/LoginScreen.kt
│   │   │   └── login/LoginViewModel.kt
│   │   └── di/
│   │       └── AuthModule.kt     ← @Binds for AuthRepository
│   │
│   ├── home/                     ← same pattern
│   │   ├── data/
│   │   ├── domain/
│   │   ├── presentation/
│   │   └── di/
│   │
│   └── profile/                  ← same pattern
│       ├── data/
│       ├── domain/
│       ├── presentation/
│       └── di/
│
├── di/
│   └── AppModule.kt              ← only app-level bindings, each feature has its own di/
│
├── MyApplication.kt
└── MainActivity.kt
    AppNavGraph.kt                ← imports all feature nav graphs
```

---

---

# VARIANT 8 — Minimal App (Single Screen, No DI, Learning)

> Use when: prototype, tutorial, learning Compose basics

```
com.yourapp/
│
├── MainActivity.kt               ← setContent, no NavHost needed
│
├── data/
│   └── repository/
│       └── UserRepository.kt     ← no interface, just the class directly
│                                    calls Retrofit or Room directly
│
├── ui/                           ← skip presentation/ nesting, keep it flat
│   ├── HomeScreen.kt
│   ├── HomeViewModel.kt
│   └── theme/
│       └── Theme.kt
│
└── (no di/ folder)               ← manually create objects in ViewModel
                                     or use simple companion object singletons
```

> ⚠️ This is fine for learning. Do NOT ship a real app structured this way.

---

---

# Lost File? Lookup Table

> You have a file and don't know where to put it — find it here.

|I have a file that...|Put it in...|
|---|---|
|Is the Retrofit API interface (`@GET`, `@POST`)|`data/remote/api/`|
|Is the JSON response shape from server|`data/remote/dto/`|
|Is a Room table definition (`@Entity`)|`data/local/entity/`|
|Is a Room query interface (`@Dao`)|`data/local/dao/`|
|Is the Room database class (`@Database`)|`data/local/` (root of local)|
|Converts DTO or Entity → domain model|`data/mapper/`|
|Implements a repository interface|`data/repository/`|
|Reads/writes DataStore or SharedPreferences|`data/local/preferences/`|
|Is a plain Kotlin data class (no annotations)|`domain/model/`|
|Is a repository interface (just a contract)|`domain/repository/`|
|Does ONE business action (login, sync, get)|`domain/usecase/`|
|Is a Composable screen|`presentation/ui/screenname/`|
|Is a ViewModel|`presentation/ui/screenname/` (same as screen)|
|Holds screen state (isLoading, error, list)|`presentation/ui/screenname/`|
|Is a Composable used on 2+ screens|`presentation/ui/components/`|
|Defines all routes / screen names|`presentation/navigation/Screen.kt`|
|Sets up the navigation graph|`presentation/navigation/NavGraph.kt`|
|Is Color.kt, Typography.kt, Theme.kt|`presentation/theme/`|
|Provides Retrofit / OkHttpClient via Hilt|`di/NetworkModule.kt`|
|Provides Room database / DAO via Hilt|`di/DatabaseModule.kt`|
|Binds interface to implementation via Hilt|`di/RepositoryModule.kt`|
|Provides DataStore via Hilt|`di/DataStoreModule.kt`|
|Is a background task (`CoroutineWorker`)|`data/worker/`|
|Runs in background with a notification|`service/`|
|Handles push notifications (FCM)|`service/`|
|Is the Application class|root of package (next to MainActivity)|

---

# The One Rule That Explains Everything

```
API / Database
     ↓
  data/         ← "HOW we get data" (implementation details)
     ↓
  domain/       ← "WHAT the app does" (business rules, no Android)
     ↓
  presentation/ ← "WHAT the user sees" (Compose, ViewModel)
```

**Arrows only go downward.** `presentation` depends on `domain`. `domain` depends on nothing. `data` implements `domain` contracts.

If you're unsure where a file goes — ask: _"Is this about fetching/storing data, business logic, or showing UI?"_ The answer tells you the layer.