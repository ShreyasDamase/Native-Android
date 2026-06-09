 Koin Dependency Injection — Complete Study Guide

#   Koin Quick-Start — Minimal Example

> Attach this as Sub-Note to the main Koin Study Guide. Follow top to bottom. App runs in under 10 minutes.

---

## Step 1 — Add Dependencies

`build.gradle.kts` (app level)

```kotlin
dependencies {
    val koin_version = "3.5.3"

    implementation("io.insert-koin:koin-android:$koin_version")
    implementation("io.insert-koin:koin-androidx-compose:$koin_version")
}
```

---

## Step 2 — Create Your Classes (3 files)

```kotlin
// ApiService.kt — what you call over network
interface ApiService {
    fun fetchData(): String
}

class ApiServiceImpl : ApiService {
    override fun fetchData() = "Hello from API"
}
```

```kotlin
// Repository.kt — middle layer between ViewModel and data
interface Repository {
    fun getData(): String
}

class RepositoryImpl(private val api: ApiService) : Repository {
    override fun getData() = api.fetchData()
}
```

```kotlin
// MyViewModel.kt — what your UI talks to
class MyViewModel(private val repository: Repository) : ViewModel() {
    fun load() = repository.getData()
}
```

---

## Step 3 — Write the Koin Module (1 file)

```kotlin
// appModule.kt
val appModule = module {

    single<ApiService> { ApiServiceImpl() }      // one instance, lives forever

    factory<Repository> { RepositoryImpl(get()) } // new each time, get() = ApiService

    viewModel { MyViewModel(get()) }              // ViewModel-aware, get() = Repository
}
```

---

## Step 4 — Start Koin in Application

```kotlin
// MyApp.kt
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApp)
            modules(appModule)
        }
    }
}
```

```xml
<!-- AndroidManifest.xml — CRITICAL, don't forget -->
<application android:name=".MyApp" ...>
```

---

## Step 5 — Use It

**In Activity:**

```kotlin
class MainActivity : ComponentActivity() {
    private val viewModel: MyViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("TAG", viewModel.load()) // "Hello from API"
    }
}
```

**In Compose:**

```kotlin
@Composable
fun HomeScreen(viewModel: MyViewModel = koinViewModel()) {
    val text = viewModel.load()
    Text(text)
}
```

---

## The Full Dependency Chain

```
MyViewModel
    └── Repository  (factory → new each time)
            └── ApiService  (single → reused always)
```

Koin builds this chain automatically via `get()`.

---

## Cheatsheet — 3 Keywords

|Keyword|When|Lifetime|
|---|---|---|
|`single`|Retrofit, DB, Firebase|Whole app|
|`factory`|Repository, UseCase|Per request|
|`viewModel`|Every ViewModel|Screen lifecycle|

---

## If It Crashes — Check This Order

1. ✅ Is `android:name=".MyApp"` in AndroidManifest?
2. ✅ Is the module added to `modules(appModule)`?
3. ✅ ViewModel uses **interface**, not `Impl`?
4. ✅ All types in constructor registered in module?
5. ✅ No typo in interface name?



### _From Zero to Production. Written for Future-You, Three Years Later._

---

> **Who this is for:** You — someone who has used Koin in a real project (TreadTrack), watched Philipp Lackner's video, and wants to _understand every single thing_ that's happening, not just copy-paste it. Every micro-curiosity answered here.

---

## 📖 Table of Contents

1. [What Problem Does Koin Solve?](https://claude.ai/chat/4c91a881-cc45-4da0-8b81-443940581645#1-what-problem-does-koin-solve)
2. [What is Dependency Injection?](https://claude.ai/chat/4c91a881-cc45-4da0-8b81-443940581645#2-what-is-dependency-injection)
3. [What is Koin Exactly?](https://claude.ai/chat/4c91a881-cc45-4da0-8b81-443940581645#3-what-is-koin-exactly)
4. [How Koin Works Internally](https://claude.ai/chat/4c91a881-cc45-4da0-8b81-443940581645#4-how-koin-works-internally)
5. [App Lifecycle — Where Koin Lives](https://claude.ai/chat/4c91a881-cc45-4da0-8b81-443940581645#5-app-lifecycle--where-koin-lives)
6. [The Core DSL Keywords Explained](https://claude.ai/chat/4c91a881-cc45-4da0-8b81-443940581645#6-the-core-dsl-keywords-explained)
7. [get() — The Magic Function](https://claude.ai/chat/4c91a881-cc45-4da0-8b81-443940581645#7-get--the-magic-function)
8. [Koin Modules — Real World Structure](https://claude.ai/chat/4c91a881-cc45-4da0-8b81-443940581645#8-koin-modules--real-world-structure)
9. [Scopes — Advanced Lifecycle Control](https://claude.ai/chat/4c91a881-cc45-4da0-8b81-443940581645#9-scopes--advanced-lifecycle-control)
10. [Injection Styles in Android](https://claude.ai/chat/4c91a881-cc45-4da0-8b81-443940581645#10-injection-styles-in-android)
11. [Your DI Project — Full Code Walkthrough](https://claude.ai/chat/4c91a881-cc45-4da0-8b81-443940581645#11-your-di-project--full-code-walkthrough)
12. [TreadTrack Context — Firebase + Koin](https://claude.ai/chat/4c91a881-cc45-4da0-8b81-443940581645#12-treadtrack-context--firebase--koin)
13. [Common Mistakes & Crashes Explained](https://claude.ai/chat/4c91a881-cc45-4da0-8b81-443940581645#13-common-mistakes--crashes-explained)
14. [Koin vs Hilt — When to Use What](https://claude.ai/chat/4c91a881-cc45-4da0-8b81-443940581645#14-koin-vs-hilt--when-to-use-what)
15. [Mental Models to Never Forget](https://claude.ai/chat/4c91a881-cc45-4da0-8b81-443940581645#15-mental-models-to-never-forget)
16. [Glossary](https://claude.ai/chat/4c91a881-cc45-4da0-8b81-443940581645#16-glossary)

---

## 1. What Problem Does Koin Solve?

### The world WITHOUT dependency injection

Imagine you're building an app. You have a `ViewModel` that needs a `Repository`. The `Repository` needs a `NetworkClient`. The `NetworkClient` needs a `Retrofit` instance.

Without DI, every class creates its own dependencies:

```kotlin
class HomeViewModel : ViewModel() {
    // ❌ The ViewModel knows HOW to build everything
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.treadtrack.com")
        .build()
    
    private val api = retrofit.create(TreadTrackApi::class.java)
    private val repository = TreadTrackRepository(api)
    
    fun loadRuns() = repository.getRuns()
}
```

**Problems with this approach:**

- Every ViewModel creates its own Retrofit — **wasteful** (should be one shared instance)
- If you want to test, you **can't swap** the real API for a fake one
- If you change how Retrofit is built, you must **change every class** that creates it
- Code is **tightly coupled** — everything knows about everything else's internals

### The world WITH dependency injection (Koin)

```kotlin
// ✅ ViewModel just ASKS for what it needs — doesn't care how it's built
class HomeViewModel(private val repository: TreadTrackRepository) : ViewModel() {
    fun loadRuns() = repository.getRuns()
}
```

Koin is responsible for building and providing `TreadTrackRepository` when `HomeViewModel` is created. The ViewModel knows **nothing** about Retrofit, APIs, or how things are constructed.

**Think of it like a restaurant:**

- The waiter (your ViewModel) says "I need a steak"
- The kitchen (Koin) figures out how to make it
- The waiter doesn't know anything about cooking

---

## 2. What is Dependency Injection?

**Dependency** = something a class needs to work.

**Injection** = giving it to the class from the outside instead of letting the class create it.

```kotlin
// NOT injected — class creates its own dependency
class Car {
    private val engine = Engine() // Car builds its own engine
}

// INJECTED — engine is given from outside
class Car(private val engine: Engine) // Koin gives this engine
```

### The 3 Rules of Clean DI

1. **Classes should receive their dependencies, not create them**
2. **Classes should depend on interfaces, not implementations**
3. **One place in the app builds everything** (the DI module)

```kotlin
// Rule 2 example: depend on interface
class HomeViewModel(
    private val repository: RunRepository  // ← interface, not RunRepositoryImpl
) : ViewModel()
```

This way you can swap `RunRepositoryImpl` for `FakeRunRepository` in tests, and the ViewModel never knows the difference.

---

## 3. What is Koin Exactly?

Koin is a **Dependency Injection framework** written in pure Kotlin. Created in 2017 by Kotzilla.

### What makes Koin different

|Property|Koin|Hilt/Dagger|
|---|---|---|
|Language|Pure Kotlin|Java + annotations|
|Code Generation|❌ None|✅ Generates Java code at compile time|
|Errors caught|At runtime|At compile time|
|Setup complexity|Very simple|Complex|
|Build speed|Fast|Slower (annotation processing)|
|KMP (iOS + Android)|✅ Yes|❌ Android only|

### The Service Locator debate

You may hear: _"Koin is a Service Locator, not real DI."_

The truth: **Koin is both.** It supports constructor injection (real DI) AND a global registry you can pull from (Service Locator). The best practice is to use constructor injection for most of your code, and let Koin's Android integration handle the Android-specific parts (Activity, Fragment) which require some Service Locator behavior because you can't inject into Android system components via constructor.

---

## 4. How Koin Works Internally

This is what happens inside Koin when your app runs.

### The Koin Container

When you call `startKoin { }`, Koin creates an internal **container** — think of it as a big dictionary / registry:

```
Key (Type)           →  Value (How to build it)
─────────────────────────────────────────────────
TreadTrackApi        →  { build Retrofit, create API }
RunRepository        →  { new RunRepositoryImpl(get<TreadTrackApi>()) }
HomeViewModel        →  { new HomeViewModel(get<RunRepository>()) }
String (in scope)    →  { "Hello" — lives only in MainActivity scope }
```

### How get() resolves a dependency

When `HomeViewModel` is needed:

```
1. Koin looks up HomeViewModel in its registry
2. Finds: HomeViewModel needs RunRepository
3. Koin looks up RunRepository
4. Finds: RunRepository needs TreadTrackApi
5. Koin looks up TreadTrackApi
6. Finds: build Retrofit → create API (it's a singleton, reuse it)
7. Injects TreadTrackApi into RunRepositoryImpl
8. Injects RunRepositoryImpl into HomeViewModel
9. Returns HomeViewModel to you ✅
```

This chain is called **dependency resolution**. Koin walks the entire chain automatically.

### No reflection, no magic

Koin doesn't use Java reflection (unlike older DI frameworks). Your lambda `{ RunRepositoryImpl(get()) }` is just a regular Kotlin function. Koin stores it and calls it when needed. This is why Koin is fast.

---

## 5. App Lifecycle — Where Koin Lives

Understanding when Koin starts, lives, and ends.

```
📱 APP STARTS
     │
     ▼
MyApplication.onCreate()
     │
     ├── startKoin { }           ← Koin container is created here
     │      │
     │      ├── single { }       ← Registered, NOT yet created
     │      ├── factory { }      ← Registered, NOT yet created
     │      └── viewModel { }    ← Registered, NOT yet created
     │
     ▼
MainActivity is created
     │
     ├── by viewModel<HomeViewModel>()
     │      └── Koin builds HomeViewModel NOW (lazy)
     │             └── Koin builds RunRepository
     │                    └── Koin reuses TreadTrackApi (singleton)
     │
     ▼
User navigates to RunDetailActivity
     │
     ├── by inject<RunRepository>()
     │      └── Koin reuses SAME RunRepository (if single)
     │         OR creates new one (if factory)
     │
     ▼
Activity is destroyed → ViewModel scope closed
     │
     └── scoped objects tied to this Activity → destroyed
     
📱 APP KILLED
     └── single { } objects → destroyed
```

**Key insight:** Registration (telling Koin how to build) happens at startup. Actual object creation happens lazily — only when first requested.

---

## 6. The Core DSL Keywords Explained

### `single { }` — The Singleton

```kotlin
single<TreadTrackApi> {
    Retrofit.Builder()
        .baseUrl("https://api.treadtrack.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TreadTrackApi::class.java)
}
```

- Created **once**, the first time it's needed
- Every class that asks for `TreadTrackApi` gets the **exact same object**
- Lives as long as the **entire app** lives
- Perfect for: Retrofit, OkHttpClient, Room Database, Firebase instances

**Mental model:** Like a company's CEO. There's only one. Everyone shares access to the same person.

---

### `factory { }` — New Every Time

```kotlin
factory<RunRepository> {
    RunRepositoryImpl(get())
}
```

- Creates a **brand new instance** every single time it's requested
- Two ViewModels asking for `RunRepository` get two different objects
- The old instance is garbage collected when nothing holds a reference to it
- Perfect for: Repositories (in learning projects), Presenters, Use Cases

**Mental model:** Like a disposable cup. Every time you need one, you get a fresh one.

**When to use single vs factory in real projects:**

```
single  →  Retrofit, Database, ApiService, SharedPreferences
factory →  UseCases, Presenters
single  →  Repository (in most real apps — they're stateless, reuse is fine)
```

> **Note:** In Philipp Lackner's video and your learning project, you used `factory` for Repository to understand the difference. In production, repositories are usually `single` because they're stateless.

---

### `viewModel { }` — ViewModel Aware

```kotlin
viewModel {
    HomeViewModel(get())
}
```

- Tells Koin this is a ViewModel
- Koin automatically creates a `ViewModelProvider.Factory` for it
- The ViewModel follows **Android's ViewModel lifecycle** (survives screen rotation)
- Use `by viewModel()` in Activity/Fragment to get it

**Why not just use `single` for ViewModels?**

ViewModels have a special lifecycle — they survive rotation and must be scoped to an Activity/Fragment, not the whole app. `viewModel { }` handles all of this. `single { }` would give you one ViewModel for the entire app — wrong behavior.

---

### `scoped { }` — Lifecycle Bound

```kotlin
scope<MainActivity> {
    scoped {
        "Hello from MainActivity scope"
    }
}
```

- Lives only as long as a **specific Android component** (Activity, Fragment)
- When the Activity is destroyed → scoped objects are destroyed
- Access via `by inject()` inside a class that implements `AndroidScopeComponent`
- Perfect for: objects that should live with one screen but not app-wide

---

### `bind` — Register Under Multiple Types

```kotlin
single<RunRepositoryImpl>() bind RunRepository::class
```

- Registers one object that can be retrieved by multiple types
- Ask for `RunRepository` → get `RunRepositoryImpl`
- Ask for `RunRepositoryImpl` → same object

---

## 7. `get()` — The Magic Function

Inside every Koin definition, `get()` means: **"Go find this dependency in the Koin container."**

```kotlin
factory<RunRepository> {
    RunRepositoryImpl(get())  // get() = "give me TreadTrackApi"
}
```

Koin looks at `RunRepositoryImpl`'s constructor, sees it needs a `TreadTrackApi`, finds it in the registry, and provides it.

### Explicit get()

```kotlin
factory<RunRepository> {
    RunRepositoryImpl(get<TreadTrackApi>())  // explicit — recommended for clarity
}
```

### get() with parameters

```kotlin
// Passing runtime values at injection time
factory { (runId: String) ->
    RunDetailViewModel(get(), runId)
}

// Usage:
val viewModel: RunDetailViewModel by viewModel { parametersOf(runId) }
```

---

## 8. Koin Modules — Real World Structure

In small apps, one module is fine. In real apps, you split modules by responsibility.

### Pattern 1: Single module (your learning project)

```kotlin
val appModule = module {
    single<TreadTrackApi> { /* Retrofit */ }
    factory<RunRepository> { RunRepositoryImpl(get()) }
    viewModel { HomeViewModel(get()) }
}
```

### Pattern 2: Multi-module by layer (production standard)

```kotlin
// networkModule.kt
val networkModule = module {
    single { OkHttpClient.Builder().build() }
    single {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    single { get<Retrofit>().create(TreadTrackApi::class.java) }
}

// dataModule.kt
val dataModule = module {
    single { TreadTrackDatabase.getInstance(get()) }  // Room DB
    single { get<TreadTrackDatabase>().runDao() }     // DAO
    single<RunRepository> { RunRepositoryImpl(get(), get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
}

// viewModelModule.kt
val viewModelModule = module {
    viewModel { HomeViewModel(get()) }
    viewModel { RunDetailViewModel(get()) }
    viewModel { ProfileViewModel(get(), get()) }
}

// MyApplication.kt
startKoin {
    androidContext(this@MyApplication)
    modules(networkModule, dataModule, viewModelModule)
}
```

**Why split modules?** Easier to read, test independently, and in multi-module Android projects, each Gradle module can have its own Koin module.

### `androidContext()` and `androidApplication()`

Inside any module, you can access the Android `Context`:

```kotlin
val dataModule = module {
    // Get the Application context
    single { TreadTrackDatabase.getInstance(androidContext()) }
    
    // Get it as Application class
    single { get<Application>() as MyApplication }
}
```

---

## 9. Scopes — Advanced Lifecycle Control

Scopes are Koin's most powerful feature and often the most misunderstood.

### Why scopes exist

- `single` → too long (whole app lifetime)
- `factory` → too short (new every time)
- `scoped` → just right (tied to a specific lifecycle)

### Scope hierarchy (from official Koin docs)

```
Application Scope   (single → lives forever)
└── Activity Retained Scope   (survives rotation)
    └── Activity Scope        (destroyed on finish)
        ├── Fragment Scope 1
        └── Fragment Scope 2
            └── ViewModel Scope (can't access Activity/Fragment scope)
```

**Key rule:** Child scopes can access parent scope definitions, but not vice versa.

### Your code explained line by line

```kotlin
// activityModule.kt
var activityModule = module {
    scope<MainActivity> {        // "Define a scope that is tied to MainActivity's lifecycle"
        scoped {
            "Hello "             // A String that lives only while MainActivity is alive
        }
    }
}
```

```kotlin
// MainActivity.kt
class MainActivity : ComponentActivity(), AndroidScopeComponent {
    // "I declare that I own a Koin scope"
    // activityScope() → automatically opens when Activity is created
    //                 → automatically closes when Activity is destroyed
    override val scope: Scope by activityScope()
    
    // This String is injected FROM the activity scope above
    // Koin says: "MainActivity's scope has a String? Give it this one"
    private val hello by inject<String>()
}
```

### Real-world scope use case: User Session

```kotlin
val sessionModule = module {
    scope(named("UserSession")) {
        scoped { UserSessionManager(get()) }
        scoped { CartRepository(get()) }
    }
}

// When user logs in:
val sessionScope = getKoin().createScope("user_session_id", named("UserSession"))
// Objects live for the session

// When user logs out:
sessionScope.close()
// All scoped objects are destroyed — no memory leaks
```

### `activityScope` vs `activityRetainedScope`

```kotlin
// Destroyed when Activity is finished or rotated
override val scope: Scope by activityScope()

// Survives rotation (backed by ViewModel lifecycle)
override val scope: Scope by activityRetainedScope()
```

Use `activityRetainedScope` when your scoped objects have state that should survive rotation.

---

## 10. Injection Styles in Android

### In Activity / Fragment — `by inject()`

```kotlin
class MainActivity : ComponentActivity() {
    // Lazy injection — object is created only when first accessed
    private val repository: RunRepository by inject()
    
    // Immediate ViewModel injection
    private val viewModel: HomeViewModel by viewModel()
    
    // Shared ViewModel across Activity and all its Fragments
    private val sharedViewModel: SharedViewModel by viewModel()
}
```

### In Fragment — `by viewModel()` and `by activityViewModel()`

```kotlin
class RunListFragment : Fragment() {
    // Fragment's own ViewModel — separate instance from Activity
    private val viewModel: RunListViewModel by viewModel()
    
    // ViewModel shared WITH the Activity (same instance)
    private val sharedViewModel: SharedViewModel by activityViewModel()
}
```

### In Jetpack Compose — `koinViewModel()`

```kotlin
// The modern Compose way (recommended over getViewModel())
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel()
) {
    val runs by viewModel.runs.collectAsState()
    // ...
}
```

```kotlin
// Your current usage (works, but older API)
setContent {
    val viewModel = getViewModel<MainViewModel>()
    viewModel.doNetWorkCall()
}
```

Both work. `koinViewModel()` is the cleaner, recommended approach.

### As a regular class (KoinComponent)

```kotlin
// When you're NOT in an Activity/Fragment/Composable
class MyWorker : CoroutineWorker(), KoinComponent {
    private val repository: RunRepository by inject()
    
    override suspend fun doWork(): Result {
        repository.syncRuns()
        return Result.success()
    }
}
```

### Direct (eager) vs Lazy injection

```kotlin
// LAZY — created when first accessed (preferred)
private val repository: RunRepository by inject()

// EAGER — created immediately when line runs
private val repository: RunRepository = get()
```

Use `by inject()` (lazy) almost always. It's more efficient and follows Android lifecycle patterns.

---

## 11. Your DI Project — Full Code Walkthrough

This is your `dependencyinjection` learning project. Every file explained.

### File 1: `MyApi.kt`

```kotlin
interface MyApi {
    @GET("my/endpoint")
    suspend fun callApi()  // suspend = runs in coroutine, doesn't block main thread
}
```

**What this is:** The contract for network calls. Retrofit uses this interface to generate actual HTTP code. Koin manages the Retrofit instance that implements this.

---

### File 2: `MainRepository.kt`

```kotlin
interface MainRepository {
    suspend fun doNetworkCall()
}
```

**What this is:** The contract that ViewModel talks to. ViewModel only knows about this interface — not the implementation. This is what makes the code testable and clean.

---

### File 3: `MainRepositoryImpl.kt`

```kotlin
class MainRepositoryImpl(
    private val api: MyApi  // ← Koin injects this
) : MainRepository {
    override suspend fun doNetworkCall() {
        api.callApi()
    }
}
```

**What this is:** The actual implementation. It knows about `MyApi` and does the real work. Registered in Koin as `factory<MainRepository>`.

**Why the class is named `Impl`?** Convention. `MainRepository` = interface. `MainRepositoryImpl` = implementation. When you have multiple implementations (e.g., `FakeMainRepository` for tests), this naming makes it clear.

---

### File 4: `MainViewModel.kt`

```kotlin
class MainViewModel(
    private val repository: MainRepository  // ← interface, NOT impl
) : ViewModel() {
    fun doNetWorkCall() {
        viewModelScope.launch {            // ← coroutine scope tied to ViewModel lifecycle
            repository.doNetworkCall()
            Log.d("MainViewModel", "Network call finished")
        }
    }
}
```

**What this is:** The ViewModel that the UI talks to. Notice it depends on `MainRepository` (the interface), not `MainRepositoryImpl`. This is the **Dependency Inversion Principle** — and it's critical.

**Why viewModelScope?** When ViewModel is cleared (user navigates away), `viewModelScope` automatically cancels all running coroutines. No memory leaks.

---

### File 5: `appModule.kt`

```kotlin
val appModule = module {

    // LAYER 1: Network
    // single<MyApi> means: registered as type MyApi
    // Koin will return this same instance every time MyApi is needed
    single<MyApi> {
        Retrofit.Builder()
            .baseUrl("https://google.com/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(MyApi::class.java)  // Retrofit generates MyApi implementation
    }

    // LAYER 2: Data
    // factory<MainRepository> means: registered as type MainRepository
    // Creates new MainRepositoryImpl every time MainRepository is requested
    // get() inside = "give me MyApi" (resolved from single above)
    factory<MainRepository> {
        MainRepositoryImpl(get())
    }

    // LAYER 3: ViewModel
    // viewModel { } tells Koin to handle Android ViewModel lifecycle
    // get() inside = "give me MainRepository" (resolved from factory above)
    viewModel {
        MainViewModel(get())
    }
}
```

**The chain Koin builds:**

```
HomeScreen requests HomeViewModel
    └── Koin builds MainViewModel
            └── Koin builds MainRepositoryImpl (new instance - factory)
                    └── Koin reuses MyApi (same instance - single)
```

---

### File 6: `activityModule.kt`

```kotlin
var activityModule = module {
    scope<MainActivity> {        // Define a scope tied to MainActivity's lifecycle
        scoped {
            "Hello "             // This String exists only while MainActivity is alive
        }
    }
}
```

This demonstrates **scoped dependencies**. In real projects you wouldn't scope a String — you'd scope things like `SessionManager`, `CheckoutCart`, or `FormState`.

---

### File 7: `MyApplication.kt`

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApplication)  // Give Koin access to Android Context
            modules(appModule, activityModule)   // Register all modules
        }
    }
}
```

**This is the entry point.** Everything starts here. `startKoin` must be called before any `by inject()` or `by viewModel()` anywhere in the app. That's why it lives in `Application.onCreate()`.

**Don't forget AndroidManifest.xml:**

```xml
<application
    android:name=".MyApplication"  ← Tell Android to use your Application class
    ...>
```

---

### File 8: `MainActivity.kt`

```kotlin
class MainActivity : ComponentActivity(), AndroidScopeComponent {
    
    // "I own a scope. Open it when I'm created, close it when I'm destroyed."
    override val scope: Scope by activityScope()
    
    // Inject from the MainActivity scope (not app-wide)
    private val hello by inject<String>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", hello)  // prints "Hello "
        
        setContent {
            // Compose way to get ViewModel
            val viewModel: MainViewModel = koinViewModel()
            viewModel.doNetWorkCall()
        }
    }
}
```

---

## 12. TreadTrack Context — Firebase + Koin

Your TreadTrack app uses Firebase Auth + Google OAuth. Here's how Koin fits into that architecture.

### How Firebase instances should be registered

```kotlin
val firebaseModule = module {
    // Firebase Auth — one instance for the whole app
    single { FirebaseAuth.getInstance() }
    
    // Firestore — one instance for the whole app
    single { FirebaseFirestore.getInstance() }
    
    // Google Sign-In Client — needs Context
    single {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(androidContext().getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(androidContext(), gso)
    }
}
```

### How Auth Repository should look

```kotlin
interface AuthRepository {
    suspend fun signInWithGoogle(idToken: String): Result<User>
    fun getCurrentUser(): User?
    fun signOut()
    fun isLoggedIn(): Boolean
}

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,     // ← Koin injects this
    private val googleSignInClient: GoogleSignInClient  // ← Koin injects this
) : AuthRepository {
    // ...implementation
}
```

### Full TreadTrack module structure (recommended)

```kotlin
// firebaseModule.kt
val firebaseModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single { FirebaseStorage.getInstance() }
    single {
        GoogleSignIn.getClient(
            androidContext(),
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(androidContext().getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )
    }
}

// repositoryModule.kt
val repositoryModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<RunRepository> { RunRepositoryImpl(get()) }
    single<UserRepository> { UserRepositoryImpl(get()) }
}

// viewModelModule.kt
val viewModelModule = module {
    viewModel { AuthViewModel(get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { RunDetailViewModel(get()) }
    viewModel { ProfileViewModel(get(), get()) }
}

// MyApplication.kt
startKoin {
    androidContext(this@MyApplication)
    modules(
        firebaseModule,
        repositoryModule,
        viewModelModule
    )
}
```

---

## 13. Common Mistakes & Crashes Explained

These are the real mistakes from your project with full explanations.

### ❌ Mistake 1: Typo in interface name

```kotlin
// You wrote:
factory<MainReposotory> { MainRepositoryImpl(get()) }

// Your ViewModel asked for:
class MainViewModel(val repository: MainRepository)

// Koin stores by TYPE. "MainReposotory" ≠ "MainRepository"
// Result: NoBeanDefFoundException at runtime
```

**Rule:** Koin is a dictionary. Wrong key = nothing found.

---

### ❌ Mistake 2: ViewModel depends on Implementation, not Interface

```kotlin
// WRONG — ViewModel asks for the concrete class
class MainViewModel(val repository: MainRepositoryImpl) : ViewModel()

// But Koin registered the interface:
factory<MainRepository> { MainRepositoryImpl(get()) }

// Koin registered "MainRepository", not "MainRepositoryImpl"
// ViewModel asks for "MainRepositoryImpl" → NoBeanDefFoundException
```

**Fix:**

```kotlin
// CORRECT — ViewModel asks for interface
class MainViewModel(val repository: MainRepository) : ViewModel()
```

**Rule:** ViewModels depend on interfaces. DI module provides implementations.

---

### ❌ Mistake 3: Forgot to add module to startKoin

```kotlin
startKoin {
    modules(appModule)  // forgot activityModule!
}

// Using scoped { } from activityModule → NoBeanDefFoundException
```

---

### ❌ Mistake 4: Using print() instead of Log.d()

```kotlin
fun doNetWorkCall() {
    print("Something")  // ❌ Won't appear in Android Logcat
}

fun doNetWorkCall() {
    Log.d("MainViewModel", "Something")  // ✅ Appears in Logcat
}
```

---

### ❌ Mistake 5: Retrofit method not suspend

```kotlin
// ❌ Won't actually execute a network call
interface MyApi {
    @GET("my/endpoint")
    fun callApi()
}

// ✅ Correct
interface MyApi {
    @GET("my/endpoint")
    suspend fun callApi()
}
```

---

### ❌ Mistake 6: Forgot AndroidManifest entry

```xml
<!-- ❌ Koin won't start — MyApplication.onCreate() is never called -->
<application android:name=".MainActivity">

<!-- ✅ Correct -->
<application android:name=".MyApplication">
```

---

## 14. Koin vs Hilt — When to Use What

This is important context for your career as a developer.

### Side-by-side comparison

||Koin|Hilt|
|---|---|---|
|Language|Pure Kotlin|Kotlin + Annotations|
|Setup|5 minutes|30+ minutes|
|Error detection|Runtime|Compile time|
|Build speed|Faster|Slower (code gen)|
|Multiplatform (iOS)|✅|❌|
|Google recommended|❌|✅|
|Learning curve|Low|Medium-High|
|Boilerplate|Very low|Medium|

### When to choose Koin

- Small-medium Android projects
- Kotlin Multiplatform (iOS + Android shared code)
- Learning and prototyping
- You want fast setup with minimal boilerplate
- Team prefers Kotlin-native solutions

### When to choose Hilt

- Large enterprise Android apps
- Your team already knows Dagger
- You need compile-time safety guarantees at scale
- The project already uses Google's Jetpack libraries deeply

**For TreadTrack and most indie/medium apps: Koin is the right choice.**

---

## 15. Mental Models to Never Forget

Lock these in. They will save you from every Koin bug you'll ever encounter.

---

### Mental Model 1: Koin resolves by TYPE

```
"MainRepository" and "MainRepositoryImpl" are DIFFERENT keys.
One typo → NoBeanDefFoundException.
```

---

### Mental Model 2: The Three Lifetimes

```
single  → whole app lifetime  → like a CEO (one, permanent)
factory → request lifetime    → like a disposable cup (new every time)
scoped  → component lifetime  → like a hotel room key (valid during your stay)
```

---

### Mental Model 3: The Chain Rule

```
If class A needs B and B needs C:
    → Register C first? No — Koin handles order.
    → Just register ALL three.
    → Koin resolves the chain automatically via get().
```

---

### Mental Model 4: Interface on top, Implementation inside

```
ViewModel/Fragment/Activity → sees only INTERFACE
DI Module                   → provides IMPLEMENTATION
Tests                       → swap with FAKE implementation
```

---

### Mental Model 5: startKoin = the moment Koin breathes

```
Before startKoin { }  →  nothing exists
After startKoin { }   →  everything is registered, ready to provide
First by inject()     →  object actually built (lazy)
```

---

### Mental Model 6: Scope = lifetime bubble

```
scope<MainActivity> {
    scoped { SomeObject() }  ←  lives inside a bubble
}                             ←  bubble created when Activity opens
                              ←  bubble POPPED when Activity is destroyed
                              ←  SomeObject → garbage collected
```

---

## 16. Glossary

|Term|Meaning|
|---|---|
|**Dependency**|Something a class needs to work|
|**Injection**|Providing that dependency from outside|
|**Module**|A block where you tell Koin what to provide|
|**Container**|Koin's internal registry (the dictionary)|
|**single**|One instance, lives for the whole app|
|**factory**|New instance every time|
|**scoped**|One instance, lives within a specific scope|
|**viewModel**|Special factory for Android ViewModels|
|**get()**|"Go find this type in Koin's container"|
|**by inject()**|Lazy delegate — create on first use|
|**by viewModel()**|Get ViewModel from Koin with lifecycle handling|
|**koinViewModel()**|Compose equivalent of by viewModel()|
|**startKoin {}**|Initialize the Koin container (call once, in Application)|
|**androidContext()**|Give Koin access to Android Application context|
|**AndroidScopeComponent**|Interface to attach a Koin scope to an Activity/Fragment|
|**activityScope()**|Scope tied to Activity lifecycle|
|**NoBeanDefFoundException**|Koin can't find a registration for the type you requested|
|**KMP**|Kotlin Multiplatform — share code between Android and iOS|
|**DSL**|Domain Specific Language — the `module { single { } }` syntax|
|**Service Locator**|Pattern where classes pull their own dependencies from a registry|
|**DI**|Dependency Injection — dependencies pushed into classes from outside|

---

_Guide created February 2026. Based on your dependency injection learning project + TreadTrack app context. References: Koin official docs, Philipp Lackner's Koin video, your debugging sessions._

