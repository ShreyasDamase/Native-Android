# Hilt Dependency Injection — Exhaustive Mastery Guide
### Every Concept. Every Example. Every Error. Root-First.

---

> This guide is written for one purpose: you will never be confused about Hilt again.  
> Not because you memorized it. Because you understood it from the ground up.

---

## Table of Contents

1. [The Problem — Why DI Exists](#1-the-problem--why-di-exists)
2. [Dependency Injection — The Concept, Not the Framework](#2-dependency-injection--the-concept-not-the-framework)
3. [What Hilt Actually Is Under The Hood](#3-what-hilt-actually-is-under-the-hood)
4. [The Mental Model That Makes Everything Click](#4-the-mental-model-that-makes-everything-click)
5. [@HiltAndroidApp — The Root](#5-hiltandroidapp--the-root)
6. [@AndroidEntryPoint — The Gateway](#6-androidentrypoint--the-gateway)
7. [@Inject — The Most Overloaded Annotation](#7-inject--the-most-overloaded-annotation)
8. [@Module — The Recipe Book](#8-module--the-recipe-book)
9. [@InstallIn — The Lifetime Decider](#9-installin--the-lifetime-decider)
10. [Hilt Components — Deep Dive](#10-hilt-components--deep-dive)
11. [Scopes — The Lifecycle of Objects](#11-scopes--the-lifecycle-of-objects)
12. [@Binds — Interfaces, The Hardest Part](#12-binds--interfaces-the-hardest-part)
13. [@Provides — Third Party and Complex Objects](#13-provides--third-party-and-complex-objects)
14. [@Binds vs @Provides — The Complete Picture](#14-binds-vs-provides--the-complete-picture)
15. [Qualifiers and @Named — Multiple Implementations](#15-qualifiers-and-named--multiple-implementations)
16. [@ApplicationContext and @ActivityContext](#16-applicationcontext-and-activitycontext)
17. [Hilt with ViewModel — Full Theory](#17-hilt-with-viewmodel--full-theory)
18. [Hilt with Room Database](#18-hilt-with-room-database)
19. [Hilt with Retrofit and OkHttp](#19-hilt-with-retrofit-and-okhttp)
20. [Hilt with Repository Pattern](#20-hilt-with-repository-pattern)
21. [Hilt with Use Cases (Clean Architecture)](#21-hilt-with-use-cases-clean-architecture)
22. [Hilt with Fragments](#22-hilt-with-fragments)
23. [Hilt with WorkManager](#23-hilt-with-workmanager)
24. [Every Compile Error Explained From Root](#24-every-compile-error-explained-from-root)
25. [Testing with Hilt](#25-testing-with-hilt)
26. [The Complete Production App Architecture](#26-the-complete-production-app-architecture)
27. [7-Day Learning Plan With Daily Exercises](#27-7-day-learning-plan-with-daily-exercises)
28. [Interview Mastery — Questions and Deep Answers](#28-interview-mastery--questions-and-deep-answers)

---

## 1. The Problem — Why DI Exists

Before touching Hilt, you must feel the pain it solves. If you don't feel the pain, the solution won't make sense.

### 1.1 The Naive Way — Manual Object Creation

Imagine building a simple app. You need to fetch users from a server and show them.

```
App needs:
  UserScreen
    → needs UserViewModel
      → needs UserRepository
        → needs ApiService
          → needs Retrofit
            → needs OkHttpClient
              → needs logging interceptor
```

This is a dependency chain. Let's write it the naive way:

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Building from the bottom up — manually
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        val userRepository = UserRepositoryImpl(apiService)

        val viewModel = UserViewModel(userRepository)

        // Now you can use viewModel
    }
}
```

This looks fine until you have a second screen.

```kotlin
class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // You copy-paste the ENTIRE chain again
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        val userRepository = UserRepositoryImpl(apiService) // new instance every time!

        val profileViewModel = ProfileViewModel(userRepository)
    }
}
```

**Problems:**

1. **Duplication** — You build Retrofit in every Activity. 10 Activities = 10 Retrofit instances.
2. **No sharing** — `UserRepository` is created fresh every time. If it caches data, the cache is lost.
3. **Tight coupling** — `MainActivity` knows about `OkHttpClient`, which has nothing to do with UI.
4. **Impossible to test** — To test `MainActivity`, you must create a real Retrofit. You can't swap it for a fake.
5. **Fragile to changes** — If `UserRepository` needs a new parameter (like a `UserDao`), you must update every Activity that creates it.

### 1.2 The Naive Fix — Singleton Pattern

You might think: put everything in a singleton.

```kotlin
object AppDependencies {
    val okHttpClient = OkHttpClient.Builder().build()
    val retrofit = Retrofit.Builder().baseUrl("...").client(okHttpClient).build()
    val apiService = retrofit.create(ApiService::class.java)
    val userRepository = UserRepositoryImpl(apiService)
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userRepository = AppDependencies.userRepository
    }
}
```

This solves duplication but creates new problems:

1. **Hard to test** — `AppDependencies` is global state. You cannot swap `userRepository` for a fake in tests.
2. **No lifecycle awareness** — Some things should be created per-Activity, not for the whole app.
3. **Eager initialization** — Everything initializes at app start, even if never used.
4. **No context access** — What if `userRepository` needs an Android `Context`?

### 1.3 The Right Solution — Dependency Injection

The fundamental insight:

> **A class should not know HOW to build what it needs. It should only declare WHAT it needs.**

```kotlin
// MainActivity declares what it needs
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var userRepository: UserRepository  // "I need this. Someone provide it."
}

// UserViewModel declares what it needs
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository   // "I need this. Someone provide it."
) : ViewModel()

// Hilt figures out the entire chain and provides everything
```

The "someone" who provides things is Hilt. And Hilt knows the entire dependency graph at compile time.

---

## 2. Dependency Injection — The Concept, Not the Framework

DI is a design pattern. Hilt is just a tool that automates it.

### 2.1 What "Injection" Means

Injection means: **the dependency is passed in from outside, not created inside.**

**Without injection (bad):**
```kotlin
class UserViewModel {
    // Creates its own dependency INSIDE
    private val userRepository = UserRepositoryImpl()
}
```

**With injection (good):**
```kotlin
class UserViewModel(
    // Receives its dependency FROM OUTSIDE
    private val userRepository: UserRepository
)
```

The word "injection" comes from the idea of something being "injected into" a class, like a needle injecting medicine — the class receives something it didn't create.

### 2.2 The Three Forms of Injection

#### Form 1 — Constructor Injection

The dependency is passed through the constructor. This is the cleanest form.

```kotlin
// Simple case — no dependencies
class Logger @Inject constructor() {
    fun log(message: String) = println(message)
}

// One dependency
class UserRepository @Inject constructor(
    private val apiService: ApiService
) {
    fun getUser(id: String) = apiService.fetchUser(id)
}

// Multiple dependencies
class OrderRepository @Inject constructor(
    private val apiService: ApiService,
    private val orderDao: OrderDao,
    private val logger: Logger
) {
    fun placeOrder(order: Order) {
        logger.log("Placing order: $order")
        orderDao.save(order)
        apiService.submitOrder(order)
    }
}
```

**When to use:** Any class you control — ViewModels, Repositories, UseCases, Services you write.

**Why it's the best form:** Dependencies are explicit. You can see exactly what a class needs just by looking at its constructor. Also, in tests, you just pass fake dependencies through the constructor.

#### Form 2 — Field Injection

The dependency is set directly on a field after the object is created.

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var analyticsService: AnalyticsService
}
```

**When to use:** Only when you cannot control the constructor — Activities, Fragments, Services, BroadcastReceivers. Android creates these, not you.

**Why you can't use constructor injection in Activities:**

Android's system calls `Activity()` (no-arg constructor) internally. You have zero control over that. The OS then calls `onCreate()`. Between these two moments, Hilt performs field injection.

**Important:** Fields annotated with `@Inject` must be `lateinit var`, never `val`, and never private (Hilt needs to set them).

```kotlin
// WRONG — val, can't be set after creation
@Inject
val userRepository: UserRepository  // compile error

// WRONG — private, Hilt can't access it
@Inject
private lateinit var userRepository: UserRepository  // won't be injected

// CORRECT
@Inject
lateinit var userRepository: UserRepository
```

#### Form 3 — Method Injection

The dependency is passed through a method call.

```kotlin
class SomeClass {
    private lateinit var userRepository: UserRepository

    @Inject
    fun init(userRepository: UserRepository) {
        this.userRepository = userRepository
    }
}
```

**When to use:** Almost never in Android. This is the least clean form. Avoid it unless you have a very specific reason.

### 2.3 Why Interfaces Over Implementations

Always inject the interface, not the concrete class.

```kotlin
// BAD — tight coupling to implementation
@Inject
lateinit var userRepositoryImpl: UserRepositoryImpl

// GOOD — depends only on contract (interface)
@Inject
lateinit var userRepository: UserRepository
```

**Why this matters:**

```kotlin
// In production
class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : UserRepository {
    override fun getUser(id: String) = apiService.fetchUser(id)
}

// In tests — a fake with no network calls
class FakeUserRepository : UserRepository {
    override fun getUser(id: String) = User(id = id, name = "Test User")
}
```

If your ViewModel depends on `UserRepository` (interface), you can swap `UserRepositoryImpl` for `FakeUserRepository` in tests without changing a single line of ViewModel code.

If your ViewModel depends on `UserRepositoryImpl` (concrete), you are stuck with the real implementation in tests.

---

## 3. What Hilt Actually Is Under The Hood

### 3.1 Hilt is Built on Dagger

Dagger is a compile-time DI framework from Google. It is extremely powerful but requires massive amounts of boilerplate — you manually write component interfaces, subcomponents, component factories, and more.

Hilt is an opinionated layer on top of Dagger that:
- Pre-defines standard Android components (Application, Activity, Fragment, etc.)
- Auto-generates the Dagger boilerplate
- Ties components to Android lifecycle automatically

**You write:** Annotations on your classes  
**Hilt generates:** All the Dagger code  
**Dagger generates:** The actual factory code that creates your objects

### 3.2 What Happens at Compile Time

When you build your app, Hilt's annotation processor reads all your annotations and generates code.

For example, when you write:

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var userRepository: UserRepository
}
```

Hilt generates something like this (you never see this, but it exists in `build/generated/`):

```java
// Auto-generated — you never write this
public final class MainActivity_GeneratedInjector {
    void injectMainActivity(MainActivity activity);
}
```

And a factory:
```java
// Auto-generated
public final class UserRepositoryImpl_Factory implements Factory<UserRepositoryImpl> {
    public UserRepositoryImpl get() {
        return new UserRepositoryImpl(apiServiceProvider.get());
    }
}
```

**The key insight:** All of this runs at compile time. If the dependency graph is broken (something can't be provided), you get a compile error — not a crash at runtime. This is one of Hilt's most important safety guarantees.

### 3.3 The Dependency Graph

Hilt builds a directed acyclic graph (DAG) of all your dependencies.

```
MainActivity
    ├── UserRepository (interface)
    │       └── UserRepositoryImpl
    │               └── ApiService
    │                       └── Retrofit
    │                               └── OkHttpClient
    │                                       └── LoggingInterceptor
    └── Logger
            (no dependencies)
```

When you ask for `UserRepository`, Hilt walks this graph from bottom to top, creating each thing and passing it up. It creates `LoggingInterceptor` first, then `OkHttpClient`, then `Retrofit`, then `ApiService`, then `UserRepositoryImpl`, then hands it to `MainActivity` as `UserRepository`.

---

## 4. The Mental Model That Makes Everything Click

Think of Hilt as a **warehouse with a smart inventory system**.

```
The Warehouse = Hilt's DI container
The Inventory = All the objects Hilt knows how to create
The Workers = Factory classes Hilt generates
The Orders = @Inject requests from your classes
The Catalog = Your @Module files (recipes for building things)
The Shelf Labels = Scopes (@Singleton, @ActivityScoped, etc.)
```

When you write `@Inject lateinit var userRepository: UserRepository`:

1. Your class places an **order** with the warehouse: "I need a `UserRepository`"
2. The warehouse checks its **catalog** (modules): "How do I make `UserRepository`?"
3. It finds the recipe: "`UserRepository` → use `UserRepositoryImpl`" (from `@Binds`)
4. It checks if it has the parts: "To make `UserRepositoryImpl`, I need `ApiService`"
5. It builds the parts: Creates `Retrofit` → `ApiService`
6. It assembles: Creates `UserRepositoryImpl(apiService)`
7. It **delivers** to your class

If anything in steps 2-6 is unknown, the warehouse says: "I don't know how to fulfill this order" — **compile error**.

---

## 5. @HiltAndroidApp — The Root

### 5.1 What It Does

`@HiltAndroidApp` is the entry point for the entire Hilt system. It must be placed on your `Application` class.

```kotlin
@HiltAndroidApp
class MyApplication : Application() {
    // You don't need to write anything here usually
    // Hilt sets up everything automatically
}
```

**What Hilt generates from this:**

- The root DI component (`SingletonComponent`)
- All the generated factory classes
- The base application class that connects to Hilt

**Without `@HiltAndroidApp`:**
- Nothing works
- All `@Inject` fields will crash with `UninitializedPropertyAccessException`
- You'll get compile errors everywhere

### 5.2 Registering in the Manifest

You must tell Android to use your custom Application class:

```xml
<!-- AndroidManifest.xml -->
<application
    android:name=".MyApplication"
    android:label="@string/app_name"
    android:icon="@mipmap/ic_launcher"
    ... >

    <activity android:name=".MainActivity" ... />
</application>
```

If you forget `android:name=".MyApplication"`, Android uses the default `Application` class, Hilt never initializes, and nothing works.

### 5.3 Custom Initialization in Application

```kotlin
@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // You can do app-level initialization here
        // But DO NOT create dependencies manually — let Hilt handle that
        Timber.plant(Timber.DebugTree())
    }
}
```

If you need a dependency inside your Application class (rare), you can inject it:

```kotlin
@HiltAndroidApp
class MyApplication : Application() {

    @Inject
    lateinit var analyticsService: AnalyticsService

    override fun onCreate() {
        super.onCreate()
        // analyticsService is available here — Hilt injects it before super.onCreate() returns
        analyticsService.initialize()
    }
}
```

---

## 6. @AndroidEntryPoint — The Gateway

### 6.1 What It Does

`@AndroidEntryPoint` marks an Android class as a "Hilt injection target". It hooks into the Android lifecycle to perform field injection at the right moment.

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity()

@AndroidEntryPoint
class UserFragment : Fragment()

@AndroidEntryPoint
class MyService : Service()

@AndroidEntryPoint
class MyBroadcastReceiver : BroadcastReceiver()
```

### 6.2 When Injection Happens

The injection timing differs per class type:

| Class | Injection Happens | Before/After |
|---|---|---|
| `Activity` | At `super.onCreate()` | Before your `onCreate` code runs |
| `Fragment` | At `onAttach()` | Before `onCreateView` |
| `Service` | At `onCreate()` | Before your service logic |
| `BroadcastReceiver` | At `onReceive()` | At the start of `onReceive` |

This is why you can safely use injected fields anywhere in `onCreate` or later.

### 6.3 The Parent Rule

If an `Activity` uses `@AndroidEntryPoint`, any `Fragment` inside it that uses Hilt **also** needs `@AndroidEntryPoint`.

```kotlin
// Activity with Hilt
@AndroidEntryPoint
class MainActivity : AppCompatActivity()

// Fragment inside — must ALSO have @AndroidEntryPoint
@AndroidEntryPoint
class UserFragment : Fragment() {
    @Inject
    lateinit var userRepository: UserRepository
}
```

If the Fragment has `@AndroidEntryPoint` but the Activity doesn't, you get a crash.

### 6.4 Common Mistake — Forgetting @AndroidEntryPoint

```kotlin
// Forgot @AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userRepository.getUser("123")  // CRASH: lateinit property not initialized
    }
}
```

The field will never be set. The annotation processor sees no `@AndroidEntryPoint`, so it generates no injection code.

---

## 7. @Inject — The Most Overloaded Annotation

`@Inject` does completely different things depending on where it's placed. This is the single biggest source of confusion. Let's eliminate it completely.

### 7.1 @Inject on a Constructor — "I Am Available"

```kotlin
class Logger @Inject constructor() {
    fun log(message: String) = println(message)
}
```

**What this tells Hilt:** "I exist in the dependency graph. You know how to create me: just call my constructor."

**When Hilt sees this, it generates:**
```java
// Auto-generated factory
public final class Logger_Factory implements Factory<Logger> {
    public Logger get() {
        return new Logger(); // Simple, no params
    }
}
```

**With dependencies:**
```kotlin
class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val logger: Logger
) {
    fun getUser(id: String): User {
        logger.log("Fetching user $id")
        return apiService.fetchUser(id)
    }
}
```

**Hilt generates:**
```java
// Auto-generated factory
public final class UserRepository_Factory implements Factory<UserRepository> {
    private final Provider<ApiService> apiServiceProvider;
    private final Provider<Logger> loggerProvider;

    public UserRepository get() {
        return new UserRepository(
            apiServiceProvider.get(),
            loggerProvider.get()
        );
    }
}
```

Hilt knows to look for providers of `ApiService` and `Logger` to fulfill the constructor parameters.

### 7.2 @Inject on a Field — "I Need This"

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var userRepository: UserRepository  // "Give me a UserRepository"

    @Inject
    lateinit var logger: Logger  // "Give me a Logger"

    @Inject
    lateinit var analyticsService: AnalyticsService  // "Give me an AnalyticsService"
}
```

**What this tells Hilt:** "When you inject into `MainActivity`, please provide values for these fields."

**The complete flow:**

```
1. Android creates MainActivity() using no-arg constructor
2. Android calls super.onCreate(savedInstanceState) in your onCreate
3. Hilt intercepts this call
4. Hilt looks at MainActivity — finds @Inject fields
5. Hilt resolves each dependency from the graph
6. Hilt sets each field on the MainActivity instance
7. Your onCreate code continues running
8. All @Inject fields are now ready
```

### 7.3 @Inject on a Method — "Call Me After Injection"

```kotlin
class Analytics {
    private lateinit var tracker: Tracker

    @Inject
    fun initialize(tracker: Tracker) {
        this.tracker = tracker
        this.tracker.start()
    }
}
```

**What this tells Hilt:** "After creating this object, call this method and pass in these dependencies."

This is rarely needed. Constructor injection covers 95% of cases. Field injection covers Activities/Fragments. Method injection is mostly a legacy pattern.

### 7.4 The Golden Rule of @Inject

```
@Inject constructor()    =    OFFER ("I can be created")
@Inject lateinit var     =    REQUEST ("I need this")
```

Never mix these up mentally. They are completely different operations.

---

## 8. @Module — The Recipe Book

### 8.1 What It Is

A module is a class that contains instructions for Hilt on how to create things it can't figure out automatically.

Hilt can figure out how to create a class automatically only if:
- The class has `@Inject constructor()`
- All constructor parameters are also things Hilt knows about

For everything else, you need a module.

### 8.2 When You Need a Module

| Situation | Module Needed? | Why |
|---|---|---|
| Class with `@Inject constructor()` | No | Hilt figures it out |
| Interface | Yes | Can't instantiate an interface |
| Third-party class (Retrofit) | Yes | Can't add `@Inject` to someone else's code |
| Class needing complex setup | Yes | `@Inject constructor()` can't express logic |
| Class needing `Context` | Special | Use `@ApplicationContext` or `@ActivityContext` |

### 8.3 Module Structure

```kotlin
// Using 'object' — for @Provides (non-abstract methods)
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder().baseUrl("...").build()
    }
}

// Using 'abstract class' — for @Binds (abstract methods)
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}

// Mixing both — use abstract class with companion object
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    // @Binds in the abstract class body
    @Binds
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    companion object {
        // @Provides in the companion object
        @Provides
        fun provideRetrofit(): Retrofit {
            return Retrofit.Builder().baseUrl("...").build()
        }
    }
}
```

### 8.4 Why object vs abstract class?

- `@Provides` methods have a body (they contain code), so they can't be `abstract`. The module must be an `object` (or regular class) so the method body is valid.
- `@Binds` methods are `abstract` (no body — Hilt generates the body). So the module must be `abstract class` to allow abstract methods.

---

## 9. @InstallIn — The Lifetime Decider

### 9.1 What It Does

`@InstallIn` tells Hilt: "Put this module's dependencies into THIS component (container)."

The component you choose determines:
- How long the provided objects live
- Where they can be injected

```kotlin
@Module
@InstallIn(SingletonComponent::class)   // "This module belongs to the app-level container"
object NetworkModule { ... }

@Module
@InstallIn(ActivityComponent::class)    // "This module belongs to the activity-level container"
abstract class ActivityModule { ... }
```

### 9.2 All Available Components

```kotlin
@InstallIn(SingletonComponent::class)        // Lives with the Application
@InstallIn(ActivityRetainedComponent::class) // Lives across configuration changes
@InstallIn(ActivityComponent::class)         // Lives with the Activity
@InstallIn(FragmentComponent::class)         // Lives with the Fragment
@InstallIn(ViewComponent::class)             // Lives with the View
@InstallIn(ViewWithFragmentComponent::class) // Lives with a View inside a Fragment
@InstallIn(ServiceComponent::class)          // Lives with the Service
```

### 9.3 The Most Common Mistake

```kotlin
// WRONG — using the scope annotation, not the component
@InstallIn(Singleton::class)          // javax.inject.Singleton — this is NOT a Hilt component!

// CORRECT — using the component class
@InstallIn(SingletonComponent::class) // dagger.hilt.components.SingletonComponent
```

These are two completely different things:
- `@Singleton` = a scope annotation, goes on `@Provides`/`@Binds` methods or classes
- `SingletonComponent::class` = the name of Hilt's app-level DI container, goes in `@InstallIn`

### 9.4 Choosing the Right Component

**Rule of thumb: use the lowest scope that works.**

For most repositories, API services, databases — use `SingletonComponent` because you want one instance shared across the whole app.

For things tied to screen state — use `ActivityComponent` or `FragmentComponent`.

For things tied to a ViewModel — use `ViewModelComponent`.

---

## 10. Hilt Components — Deep Dive

### 10.1 The Component Hierarchy

```
Application scope
        │
        ▼
SingletonComponent
        │
        ▼
ActivityRetainedComponent  ◄── This survives rotation!
        │
        ▼
ActivityComponent ◄── Dies on rotation
        │
        ├──────────────────┐
        ▼                  ▼
FragmentComponent     ViewComponent
        │
        ▼
ViewWithFragmentComponent
```

Also separate (not in hierarchy):
```
ServiceComponent  ◄── For Service classes
```

### 10.2 What Each Component Does

**SingletonComponent**
- Tied to the `Application`
- Created when the app starts
- Destroyed when the app process is killed
- Use for: databases, network clients, global repositories

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "db").build()
    }
}
```

**ActivityRetainedComponent**
- Survives configuration changes (screen rotation)
- Destroyed when the Activity finishes (not rotates)
- This is the same lifecycle as a `ViewModel`
- Use for: things a ViewModel needs that should survive rotation

**ActivityComponent**
- Tied to a specific Activity instance
- Dies when the Activity is destroyed OR rotated
- Use for: things specific to one Activity session

**FragmentComponent**
- Tied to a specific Fragment instance
- Use for: things specific to one Fragment

**ViewModelComponent**
- Tied to a ViewModel
- Survives rotation (same as ViewModel)
- Preferred for ViewModel-scoped things

### 10.3 What Can Be Injected Where

A dependency from a parent component can be injected into a child component, but NOT vice versa.

```
SingletonComponent  →  can be injected anywhere
ActivityComponent   →  can be injected into Activity, Fragment, View
FragmentComponent   →  can only be injected into Fragment, View inside Fragment
```

**Example:**
```kotlin
// A @Singleton dependency...
@Singleton
class UserRepository @Inject constructor() { }

// ...can be injected into an Activity (child of SingletonComponent)
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var userRepository: UserRepository  // WORKS
}

// ...can be injected into a Fragment (grandchild of SingletonComponent)
@AndroidEntryPoint
class UserFragment : Fragment() {
    @Inject
    lateinit var userRepository: UserRepository  // WORKS
}
```

But you cannot inject a Fragment-scoped thing into an Activity — the Fragment might not exist yet.

---

## 11. Scopes — The Lifecycle of Objects

### 11.1 What a Scope Is

A scope annotation on a `@Provides` or `@Binds` function tells Hilt: "Only create ONE instance of this per [component lifetime]."

Without a scope: every injection request creates a new instance.

With `@Singleton`: one instance for the entire app lifetime.

### 11.2 Unscoped vs Scoped

**Unscoped (no annotation):**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideLogger(): Logger {  // No scope annotation
        return Logger()
    }
}
```

Every class that requests `Logger` gets a **brand new** `Logger` instance.

```kotlin
class ClassA @Inject constructor(val logger: Logger)
class ClassB @Inject constructor(val logger: Logger)

// logger in ClassA != logger in ClassB (different objects)
```

**Scoped with @Singleton:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton    // ← scope annotation
    fun provideLogger(): Logger {
        return Logger()
    }
}
```

Every class that requests `Logger` gets the **same** `Logger` instance.

```kotlin
class ClassA @Inject constructor(val logger: Logger)
class ClassB @Inject constructor(val logger: Logger)

// logger in ClassA == logger in ClassB (same object)
```

### 11.3 All Scope Annotations

| Scope Annotation | Component | Meaning |
|---|---|---|
| `@Singleton` | `SingletonComponent` | One instance per app |
| `@ActivityRetainedScoped` | `ActivityRetainedComponent` | One per activity session (survives rotation) |
| `@ActivityScoped` | `ActivityComponent` | One per activity instance |
| `@FragmentScoped` | `FragmentComponent` | One per fragment instance |
| `@ViewScoped` | `ViewComponent` | One per view instance |
| `@ViewModelScoped` | `ViewModelComponent` | One per ViewModel instance |
| `@ServiceScoped` | `ServiceComponent` | One per service instance |

### 11.4 Scope Must Match Component

The scope annotation must correspond to the component in `@InstallIn`.

```kotlin
// CORRECT — @Singleton matches SingletonComponent
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton        // Correct — matches SingletonComponent
    fun provideRetrofit(): Retrofit { ... }
}

// CORRECT — @ActivityScoped matches ActivityComponent
@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {
    @Provides
    @ActivityScoped   // Correct — matches ActivityComponent
    fun provideSessionManager(): SessionManager { ... }
}

// WRONG — scope doesn't match component
@Module
@InstallIn(ActivityComponent::class)
object WrongModule {
    @Provides
    @Singleton   // WRONG — can't use @Singleton with ActivityComponent
    fun provideSomething(): Something { ... }
}
```

### 11.5 Should Everything Be @Singleton?

No. Only things that:
1. Are expensive to create (database, network client)
2. Need to share state across the app (user session, cache)
3. Are stateless but heavy (Retrofit, OkHttpClient)

Don't make everything Singleton. If an object is lightweight and doesn't need shared state, unscoped is fine — Hilt will create new instances as needed.

---

## 12. @Binds — Interfaces, The Hardest Part

### 12.1 The Root Problem

An interface is a contract. It cannot be created with `new Interface()`. It has no constructor.

```kotlin
interface UserRepository {
    fun getUser(id: String): User
    fun saveUser(user: User)
    fun deleteUser(id: String)
}
```

When Hilt sees a request for `UserRepository`, it thinks:

> "Someone needs `UserRepository`. Can I create it? No — it has no constructor. It's an interface. I need someone to tell me which implementation to use."

`@Binds` is how you tell Hilt which implementation to use.

### 12.2 How @Binds Works

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl   // ← The concrete class Hilt will create
    ): UserRepository              // ← The interface Hilt will return
}
```

**Reading this:** "When someone asks for `UserRepository`, create a `UserRepositoryImpl` and return it as `UserRepository`."

**What Hilt generates from this (conceptually):**
```kotlin
// Hilt generates this
fun provideUserRepository(impl: UserRepositoryImpl): UserRepository = impl
```

### 12.3 Full @Binds Example — Step by Step

**Step 1: Define the interface**
```kotlin
// data/repository/UserRepository.kt
interface UserRepository {
    fun getUser(id: String): User
    fun getAllUsers(): List<User>
    fun saveUser(user: User)
}
```

**Step 2: Create the implementation**
```kotlin
// data/repository/UserRepositoryImpl.kt
class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao
) : UserRepository {

    override fun getUser(id: String): User {
        return apiService.fetchUser(id)
    }

    override fun getAllUsers(): List<User> {
        return userDao.getAllUsers()
    }

    override fun saveUser(user: User) {
        userDao.insertUser(user)
    }
}
```

**Step 3: Create the binding module**
```kotlin
// di/RepositoryModule.kt
package com.example.app.di

import com.example.app.data.repository.UserRepository
import com.example.app.data.repository.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository
}
```

**Step 4: Inject the interface, not the implementation**
```kotlin
// In a ViewModel
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository  // Interface, not Impl
) : ViewModel() {
    fun loadUser(id: String) = userRepository.getUser(id)
}

// In an Activity
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var userRepository: UserRepository  // Interface, not Impl
}
```

### 12.4 Multiple Repositories — Real App Pattern

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
```

### 12.5 The DependencyCycle Error — Deep Explanation

You hit this error. Let's kill it forever.

**The wrong code:**
```kotlin
@Binds
abstract fun bindUserRepository(
    impl: UserRepository   // ← MISTAKE: This is the interface itself!
): UserRepository
```

**What Hilt tries to do:**
```
To provide UserRepository:
    → Run bindUserRepository(impl: UserRepository)
    → But to call this, I need a UserRepository first
    → To provide UserRepository:
        → Run bindUserRepository(impl: UserRepository)
        → But to call this, I need a UserRepository first
        → To provide UserRepository:
            → ...infinite loop
```

**The error message:**
```
[Dagger/DependencyCycle] Found a dependency cycle:
  UserRepository is injected at bindUserRepository(impl)
  UserRepository is injected at bindUserRepository(impl)
  ...
```

**The fix:** The parameter must be the IMPLEMENTATION, never the interface.

```kotlin
@Binds
abstract fun bindUserRepository(
    impl: UserRepositoryImpl   // ← Concrete class, not interface
): UserRepository
```

**How to remember:** The arrow flows from concrete → abstract.

```
UserRepositoryImpl  →  UserRepository
(parameter)            (return type)
(concrete)             (interface)
```

### 12.6 Rules for @Binds (Never Forget)

1. The module class must be `abstract class` (not `object`)
2. The function must be `abstract` (no body, no `{}`)
3. There must be exactly ONE parameter
4. The parameter type must implement/extend the return type
5. The parameter type must be available in Hilt's graph (have `@Inject constructor()` or its own `@Provides`)

---

## 13. @Provides — Third Party and Complex Objects

### 13.1 When @Inject Constructor Isn't Enough

You cannot use `@Inject constructor()` when:
- You don't own the class (it's from a library)
- The class needs complex setup before use
- You need to call a factory method to get the instance

```kotlin
// Retrofit — from a library. You can't add @Inject to it.
val retrofit = Retrofit.Builder()
    .baseUrl("https://api.example.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()  // Created via builder, not constructor

// Room — from a library
val database = Room.databaseBuilder(context, AppDatabase::class.java, "db").build()

// SharedPreferences — needs Context
val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
```

For all of these, use `@Provides`.

### 13.2 @Provides Basics

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        // okHttpClient is automatically injected by Hilt!
        return Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        // retrofit is automatically injected by Hilt!
        return retrofit.create(ApiService::class.java)
    }
}
```

**How parameter passing works:**

When `provideRetrofit` has `okHttpClient: OkHttpClient` as a parameter, Hilt looks for another provider of `OkHttpClient`. It finds `provideOkHttpClient()`. It calls that first, then passes the result into `provideRetrofit()`. This is automatic.

The dependency chain:

```
ApiService
    ↓ needs
Retrofit
    ↓ needs
OkHttpClient
    ↓ no dependencies (Hilt creates it directly)
```

### 13.3 Providing Context

You never create `Context` — it's an Android system object. Hilt provides two contexts:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context  // ← This annotation tells Hilt to inject the app context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }
}
```

`@ApplicationContext` provides the `Application` context — safe for long-lived objects.

`@ActivityContext` provides the `Activity` context — only use in Activity-scoped objects.

### 13.4 Providing DAOs from Room

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "db")
            .fallbackToDestructiveMigration()
            .build()
    }

    // Each DAO is provided separately
    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()  // AppDatabase is auto-injected by Hilt
    }

    @Provides
    fun provideProductDao(database: AppDatabase): ProductDao {
        return database.productDao()
    }

    @Provides
    fun provideOrderDao(database: AppDatabase): OrderDao {
        return database.orderDao()
    }
}
```

Note: DAOs don't need `@Singleton` because the database is already a singleton, and `database.userDao()` returns the same DAO object each time.

### 13.5 Providing With Complex Logic

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val cache = Cache(
            directory = File(context.cacheDir, "http_cache"),
            maxSize = 10L * 1024L * 1024L  // 10 MB
        )

        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
```

This complexity cannot be expressed in a constructor. `@Provides` handles it cleanly.

---

## 14. @Binds vs @Provides — The Complete Picture

### 14.1 Decision Tree

```
Do you need to provide a dependency?
        │
        ▼
Is it a class you OWN (your code)?
        │
   YES  │  NO
   ↓         ↓
Does it have a      → @Provides
simple constructor?
        │
   YES  │  NO
   ↓         ↓
Use           → @Provides
@Inject
constructor()
        │
Is it an interface?
   YES  → @Binds
   NO   → @Inject constructor() is enough
```

### 14.2 Side-by-Side Examples

```kotlin
// ===== Situation 1: Your class, simple constructor =====
// No module needed at all!
class Logger @Inject constructor() {
    fun log(message: String) = println(message)
}

// ===== Situation 2: Interface binding =====
// Need @Binds
interface AuthRepository {
    fun login(email: String, password: String): Result<User>
}

class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : AuthRepository {
    override fun login(email: String, password: String): Result<User> {
        return try { Result.success(apiService.login(email, password)) }
        catch (e: Exception) { Result.failure(e) }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}

// ===== Situation 3: Third-party class =====
// Need @Provides
@Module
@InstallIn(SingletonComponent::class)
object ThirdPartyModule {
    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().setDateFormat("yyyy-MM-dd").create()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
}

// ===== Situation 4: Interface with complex construction =====
// Need @Provides (can't use @Binds because it's complex)
@Module
@InstallIn(SingletonComponent::class)
object CacheModule {
    @Provides
    @Singleton
    fun provideCacheRepository(
        @ApplicationContext context: Context
    ): CacheRepository {
        val cacheDir = File(context.cacheDir, "user_cache")
        cacheDir.mkdirs()
        return DiskCacheRepository(cacheDir, maxSizeBytes = 5_000_000)
    }
}
```

### 14.3 Can @Provides Replace @Binds?

Yes, technically. But don't.

```kotlin
// This works but is wasteful
@Provides
fun provideUserRepository(impl: UserRepositoryImpl): UserRepository = impl

// This is better — @Binds generates less code and is more explicit
@Binds
abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
```

At compile time, `@Binds` generates a more efficient binding than `@Provides` for simple delegation. Always prefer `@Binds` for interface-to-implementation mappings.

---

## 15. Qualifiers and @Named — Multiple Implementations

### 15.1 The Problem

What if you have two implementations of the same interface and need both?

```kotlin
interface ApiService {
    fun fetchUser(id: String): User
}

class ProdApiService @Inject constructor() : ApiService {
    override fun fetchUser(id: String) = // real network call
}

class MockApiService @Inject constructor() : ApiService {
    override fun fetchUser(id: String) = User("test-id", "Mock User")
}
```

If you try to `@Binds` both, Hilt will complain — it doesn't know which one to use.

### 15.2 Solution 1 — @Named

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class ApiModule {

    @Binds
    @Named("prod")  // Tag this with "prod"
    abstract fun bindProdApiService(impl: ProdApiService): ApiService

    @Binds
    @Named("mock")  // Tag this with "mock"
    abstract fun bindMockApiService(impl: MockApiService): ApiService
}

// Injecting with @Named
class UserRepository @Inject constructor(
    @Named("prod") private val apiService: ApiService
) { ... }

class TestUserRepository @Inject constructor(
    @Named("mock") private val apiService: ApiService
) { ... }
```

### 15.3 Solution 2 — Custom Qualifier (Better)

`@Named` uses strings which can have typos. Custom qualifiers are type-safe.

```kotlin
// Define qualifier annotations
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProdApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MockApi

// Use them in module
@Module
@InstallIn(SingletonComponent::class)
abstract class ApiModule {

    @Binds
    @ProdApi
    abstract fun bindProdApiService(impl: ProdApiService): ApiService

    @Binds
    @MockApi
    abstract fun bindMockApiService(impl: MockApiService): ApiService
}

// Inject with custom qualifier
class UserRepository @Inject constructor(
    @ProdApi private val apiService: ApiService
) { ... }
```

### 15.4 Real-World Example — Two Retrofit Instances

```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    @AuthRetrofit
    fun provideAuthRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://auth.api.example.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @PublicRetrofit
    fun providePublicRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://public.api.example.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(@AuthRetrofit retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePublicApiService(@PublicRetrofit retrofit: Retrofit): PublicApiService {
        return retrofit.create(PublicApiService::class.java)
    }
}
```

---

## 16. @ApplicationContext and @ActivityContext

### 16.1 The Problem with Context

`Context` is an Android concept with different lifetimes:
- `Application` context lives as long as the app
- `Activity` context lives as long as the Activity

If you inject `Activity` context into a `@Singleton` object, the singleton holds a reference to the Activity — even after the Activity is destroyed. This is a **memory leak**.

Hilt provides qualifiers for safe context injection.

### 16.2 @ApplicationContext

Use when the dependency lives in `SingletonComponent`.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context  // Safe — app-level context
    ): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "db").build()
    }

    @Provides
    @Singleton
    fun provideSharedPrefs(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    }
}
```

### 16.3 @ActivityContext

Use when the dependency lives in `ActivityComponent`.

```kotlin
@Module
@InstallIn(ActivityComponent::class)
object UiModule {

    @Provides
    @ActivityScoped
    fun provideLayoutInflater(
        @ActivityContext context: Context  // Activity context is fine here — same scope
    ): LayoutInflater {
        return LayoutInflater.from(context)
    }
}
```

### 16.4 Using Context in a Class With @Inject Constructor

```kotlin
// Wrong — raw Context isn't injectable without qualifier
class FileManager @Inject constructor(
    private val context: Context  // Won't compile — which Context?
)

// Correct
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context  // Hilt knows this is Application context
) {
    fun readFile(name: String): String {
        return File(context.filesDir, name).readText()
    }
}
```

---

## 17. Hilt with ViewModel — Full Theory

### 17.1 Why ViewModels Are Special

ViewModels survive configuration changes (screen rotation). They are not created by Android directly — they go through `ViewModelProvider` and a `ViewModelFactory`.

Without Hilt, injecting into a ViewModel requires a custom `ViewModelFactory` — lots of boilerplate.

With Hilt, you just add `@HiltViewModel` and it handles everything.

### 17.2 @HiltViewModel in Detail

```kotlin
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,    // From your DI graph
    private val savedStateHandle: SavedStateHandle // Provided by Hilt automatically
) : ViewModel() {

    private val userId: String = savedStateHandle.get<String>("userId") ?: ""

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    init {
        loadUser()
    }

    fun loadUser() {
        viewModelScope.launch {
            val result = userRepository.getUser(userId)
            _user.value = result
        }
    }
}
```

`SavedStateHandle` is a special type that Hilt knows how to provide — it gives access to the saved state bundle. You don't need to create it or configure it.

### 17.3 Consuming ViewModel in Activity

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Hilt-aware ViewModel delegation
    private val viewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel.user.observe(this) { user ->
            // Update UI
        }
    }
}
```

### 17.4 Consuming ViewModel in Fragment

```kotlin
@AndroidEntryPoint
class UserFragment : Fragment() {

    // Fragment-owned ViewModel (destroyed with fragment)
    private val viewModel: UserViewModel by viewModels()

    // ViewModel shared with Activity (survives fragment recreation)
    private val sharedViewModel: UserViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.user.observe(viewLifecycleOwner) { user -> ... }
    }
}
```

### 17.5 Multiple ViewModels — Real App

```kotlin
// Auth flow ViewModel
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun login(email: String, password: String) { ... }
    fun logout() { ... }
}

// User profile ViewModel
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val userId = savedStateHandle.get<String>("userId")!!
    fun loadProfile() { ... }
}

// Products list ViewModel
@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    val products = productRepository.getAllProducts()
}

// Activity uses all three
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private val productsViewModel: ProductsViewModel by viewModels()
}
```

---

## 18. Hilt with Room Database

### 18.1 Complete Room + Hilt Setup

**Entity:**
```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

**DAO:**
```kotlin
@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}
```

**Database:**
```kotlin
@Database(entities = [UserEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
```

**Module:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }
}
```

**Repository using the DAO:**
```kotlin
interface UserRepository {
    fun getAllUsers(): Flow<List<User>>
    suspend fun getUser(id: String): User?
    suspend fun saveUser(user: User)
}

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao  // Hilt provides this automatically
) : UserRepository {

    override fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getUser(id: String): User? {
        return userDao.getUserById(id)?.toDomain()
    }

    override suspend fun saveUser(user: User) {
        userDao.insertUser(user.toEntity())
    }
}
```

---

## 19. Hilt with Retrofit and OkHttp

### 19.1 Complete Network Setup

```kotlin
// API service interface
interface UserApiService {
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): UserResponse

    @GET("users")
    suspend fun getAllUsers(): List<UserResponse>

    @POST("users")
    suspend fun createUser(@Body user: CreateUserRequest): UserResponse
}

// Auth interceptor
class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage  // This is also injected!
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStorage.getToken()
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}

// Network module
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        authInterceptor: AuthInterceptor  // Hilt creates this — it has @Inject constructor
    ): AuthInterceptor = authInterceptor

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BODY
                else
                    HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideUserApiService(retrofit: Retrofit): UserApiService {
        return retrofit.create(UserApiService::class.java)
    }
}
```

### 19.2 Complete Dependency Graph

```
UserViewModel
    ↓
UserRepository (interface)
    ↓
UserRepositoryImpl
    ├── UserApiService
    │       ↓
    │   Retrofit
    │       ↓
    │   OkHttpClient
    │       ├── AuthInterceptor
    │       │       ↓
    │       │   TokenStorage (@Inject constructor)
    │       └── HttpLoggingInterceptor (setup inline)
    └── UserDao
            ↓
        AppDatabase
            ↓
        Context (@ApplicationContext)
```

Hilt resolves this entire graph automatically. You just declare what you need.

---

## 20. Hilt with Repository Pattern

### 20.1 Single Source of Truth Pattern

```kotlin
interface ProductRepository {
    fun getProducts(): Flow<List<Product>>
    suspend fun refreshProducts()
    suspend fun getProductById(id: String): Product?
}

class ProductRepositoryImpl @Inject constructor(
    private val apiService: ProductApiService,   // Remote data source
    private val productDao: ProductDao,           // Local data source
    private val networkChecker: NetworkChecker   // Utility
) : ProductRepository {

    override fun getProducts(): Flow<List<Product>> {
        // Emit from local DB always (single source of truth)
        return productDao.getAllProducts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshProducts() {
        // Fetch from network, save to local DB
        if (networkChecker.isConnected()) {
            val response = apiService.getProducts()
            productDao.insertAll(response.map { it.toEntity() })
        }
    }

    override suspend fun getProductById(id: String): Product? {
        // Try local first, then remote
        return productDao.getById(id)?.toDomain()
            ?: apiService.getProduct(id).toDomain().also {
                productDao.insert(it.toEntity())
            }
    }
}
```

---

## 21. Hilt with Use Cases (Clean Architecture)

### 21.1 What Are Use Cases

Use cases (also called Interactors) represent a single business operation. They sit between the ViewModel and the Repository.

```
ViewModel → UseCase → Repository → Data Sources
```

Use cases have `@Inject constructor()` — no module needed usually.

```kotlin
// Individual use cases
class GetUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): Result<User> {
        return try {
            Result.success(userRepository.getUser(userId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class SaveUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val validator: UserValidator
) {
    suspend operator fun invoke(user: User): Result<Unit> {
        if (!validator.validate(user)) {
            return Result.failure(InvalidUserException())
        }
        return try {
            userRepository.saveUser(user)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DeleteUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): Result<Unit> {
        return try {
            userRepository.deleteUser(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ViewModel using use cases
@HiltViewModel
class UserViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase,
    private val saveUserUseCase: SaveUserUseCase,
    private val deleteUserUseCase: DeleteUserUseCase
) : ViewModel() {

    fun loadUser(id: String) = viewModelScope.launch {
        getUserUseCase(id).onSuccess { user ->
            // update UI
        }.onFailure { error ->
            // show error
        }
    }
}
```

The beauty: `UserViewModel` doesn't know about `UserRepository` at all. It only knows about use cases.

---

## 22. Hilt with Fragments

### 22.1 Basic Fragment Injection

```kotlin
@AndroidEntryPoint
class ProductsFragment : Fragment(R.layout.fragment_products) {

    // Field injection works in fragments
    @Inject
    lateinit var analyticsService: AnalyticsService

    // ViewModel injection using delegation
    private val viewModel: ProductsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // analyticsService is ready here
        analyticsService.trackScreen("ProductsFragment")

        viewModel.products.observe(viewLifecycleOwner) { products ->
            // update list
        }
    }
}
```

### 22.2 Shared ViewModel Between Fragment and Activity

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Activity creates and owns the ViewModel
    private val sharedViewModel: SharedViewModel by viewModels()
}

@AndroidEntryPoint
class ProductDetailFragment : Fragment() {

    // Fragment gets the SAME ViewModel from the Activity
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.selectedProduct.observe(viewLifecycleOwner) { product ->
            // Access product selected in another fragment
        }
    }
}
```

---

## 23. Hilt with WorkManager

### 23.1 Setup

Add dependency:
```kotlin
implementation "androidx.hilt:hilt-work:1.0.0"
kapt "androidx.hilt:hilt-compiler:1.0.0"
```

### 23.2 Worker with Injection

```kotlin
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val userRepository: UserRepository,  // Regular injection!
    private val logger: Logger
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            logger.log("Starting sync")
            userRepository.sync()
            logger.log("Sync complete")
            Result.success()
        } catch (e: Exception) {
            logger.log("Sync failed: ${e.message}")
            Result.retry()
        }
    }
}
```

### 23.3 Initialize WorkManager with Hilt

```kotlin
@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}
```

---

## 24. Every Compile Error Explained From Root

### Error 1: MissingBinding

```
[Dagger/MissingBinding] com.example.UserRepository cannot be provided
without an @Provides-annotated method.
```

**Root cause:** You're requesting something Hilt doesn't know how to create.

**Common causes and fixes:**

```kotlin
// Cause A: Injecting an interface without a binding
@Inject lateinit var repo: UserRepository
// Fix: Create @Binds module for UserRepository → UserRepositoryImpl

// Cause B: Using a class without @Inject constructor and without @Provides
class SomeClass(val name: String)  // No @Inject
@Inject lateinit var something: SomeClass
// Fix: Add @Inject constructor() or create @Provides for it

// Cause C: Wrong component — binding is in a different component than injection point
@Module @InstallIn(FragmentComponent::class)  // Only available in Fragment
// But trying to inject into Activity
// Fix: Move to SingletonComponent or ActivityComponent
```

---

### Error 2: DependencyCycle

```
[Dagger/DependencyCycle] Found a dependency cycle
```

**Root cause:** A → B → A (circular dependency).

**Most common cause — wrong @Binds parameter:**
```kotlin
// Wrong: interface as parameter = cycle
@Binds
abstract fun bind(impl: UserRepository): UserRepository  // ← cycle!

// Fix: concrete class as parameter
@Binds
abstract fun bind(impl: UserRepositoryImpl): UserRepository
```

**Real circular dependency (rare but real):**
```kotlin
class A @Inject constructor(val b: B)
class B @Inject constructor(val a: A)
// A needs B, B needs A — impossible to resolve

// Fix: Inject Provider<B> instead
class A @Inject constructor(val bProvider: Provider<B>)
// bProvider.get() creates B lazily, breaking the cycle
```

---

### Error 3: @InstallIn Wrong Class

```
[Hilt] @InstallIn, can only be used with @DefineComponent-annotated classes,
but found: [javax.inject.Singleton]
```

```kotlin
// Wrong
@InstallIn(Singleton::class)  // javax.inject.Singleton — NOT a component

// Correct
@InstallIn(SingletonComponent::class)  // dagger.hilt.components.SingletonComponent
```

---

### Error 4: @Binds Must Be Abstract

```
@Binds methods must be abstract
```

```kotlin
// Wrong — module is object, function is not abstract
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Binds
    fun bindRepo(impl: UserRepositoryImpl): UserRepository = impl  // Error
}

// Correct — module is abstract class, function is abstract
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindRepo(impl: UserRepositoryImpl): UserRepository
}
```

---

### Error 5: @Binds Parameter Must Be a Subtype

```
[Hilt] @Binds parameter type UserRepositoryImpl must be assignable to the return type UserRepository
```

This means `UserRepositoryImpl` doesn't implement `UserRepository`.

```kotlin
// Check your class
class UserRepositoryImpl : UserRepository { ... }  // Must explicitly implement
```

---

### Error 6: Multiple Bindings

```
[Dagger/DuplicateBindings] UserRepository is bound multiple times
```

You accidentally provided the same type more than once.

```kotlin
// Two modules both bind UserRepository
@Binds abstract fun bindUserRepo1(impl: UserRepositoryImpl): UserRepository  // in Module1
@Binds abstract fun bindUserRepo2(impl: UserRepositoryImpl): UserRepository  // in Module2
```

**Fix:** Keep only one binding. Or use `@Named`/custom qualifiers to differentiate.

---

### Error 7: lateinit Not Initialized

```
kotlin.UninitializedPropertyAccessException: lateinit property userRepository has not been initialized
```

**Root causes:**

```kotlin
// Cause A: Missing @HiltAndroidApp on Application
class MyApplication : Application()  // Forgot annotation

// Cause B: Missing @AndroidEntryPoint on Activity
class MainActivity : AppCompatActivity() {  // Forgot annotation
    @Inject lateinit var repo: UserRepository  // Never gets injected
}

// Cause C: Accessing field before injection
class MainActivity : AppCompatActivity() {
    @Inject lateinit var repo: UserRepository

    val something = repo.getUser("123")  // Accessing at class initialization — too early!

    // Correct — access in onCreate or later
    override fun onCreate(...) {
        super.onCreate(savedInstanceState)
        repo.getUser("123")  // Now it's injected
    }
}
```

---

## 25. Testing with Hilt

### 25.1 Why Hilt Makes Testing Easier

Without Hilt, you create real objects in tests — real database, real network calls. With Hilt, you replace real implementations with fakes.

### 25.2 Adding Test Dependencies

```kotlin
// build.gradle (app)
testImplementation "com.google.dagger:hilt-android-testing:2.48"
kaptTest "com.google.dagger:hilt-android-compiler:2.48"
androidTestImplementation "com.google.dagger:hilt-android-testing:2.48"
kaptAndroidTest "com.google.dagger:hilt-android-compiler:2.48"
```

### 25.3 Replacing Modules in Tests

```kotlin
// The real module
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}

// A fake repository for tests
class FakeUserRepository : UserRepository {
    private val users = mutableMapOf<String, User>()

    override suspend fun getUser(id: String): User? = users[id]

    override suspend fun saveUser(user: User) {
        users[user.id] = user
    }

    // Test helpers
    fun addUser(user: User) { users[user.id] = user }
    fun clear() { users.clear() }
}

// Test module that replaces the real module
@Module
@InstallIn(SingletonComponent::class)
abstract class TestRepositoryModule {
    @Binds
    abstract fun bindUserRepository(impl: FakeUserRepository): UserRepository
}

// In your test
@HiltAndroidTest
@UninstallModules(RepositoryModule::class)   // Remove real module
class UserViewModelTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeUserRepository: FakeUserRepository  // The fake!

    @Before
    fun setUp() {
        hiltRule.inject()
        fakeUserRepository.addUser(User("1", "Test User"))
    }

    @Test
    fun `loadUser returns correct user`() {
        val viewModel = UserViewModel(fakeUserRepository)
        viewModel.loadUser("1")
        assertEquals("Test User", viewModel.user.value?.name)
    }
}
```

---

## 26. The Complete Production App Architecture

### 26.1 Complete File Structure

```
app/
├── MyApplication.kt                    (@HiltAndroidApp)
│
├── di/                                 (All Hilt modules)
│   ├── DatabaseModule.kt               (Room setup)
│   ├── NetworkModule.kt                (Retrofit, OkHttp)
│   ├── RepositoryModule.kt             (@Binds for all repositories)
│   └── AppModule.kt                    (Misc singletons)
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   │   ├── UserDao.kt
│   │   │   └── ProductDao.kt
│   │   └── entity/
│   │       ├── UserEntity.kt
│   │       └── ProductEntity.kt
│   │
│   ├── remote/
│   │   ├── UserApiService.kt
│   │   ├── ProductApiService.kt
│   │   └── dto/
│   │       ├── UserDto.kt
│   │       └── ProductDto.kt
│   │
│   └── repository/
│       ├── UserRepository.kt           (interface)
│       ├── UserRepositoryImpl.kt       (implementation)
│       ├── ProductRepository.kt        (interface)
│       └── ProductRepositoryImpl.kt    (implementation)
│
├── domain/
│   ├── model/
│   │   ├── User.kt
│   │   └── Product.kt
│   └── usecase/
│       ├── user/
│       │   ├── GetUserUseCase.kt
│       │   ├── SaveUserUseCase.kt
│       │   └── DeleteUserUseCase.kt
│       └── product/
│           ├── GetProductsUseCase.kt
│           └── SearchProductsUseCase.kt
│
└── ui/
    ├── user/
    │   ├── UserViewModel.kt            (@HiltViewModel)
    │   ├── UserActivity.kt             (@AndroidEntryPoint)
    │   └── UserFragment.kt             (@AndroidEntryPoint)
    └── product/
        ├── ProductsViewModel.kt        (@HiltViewModel)
        └── ProductsFragment.kt         (@AndroidEntryPoint)
```

### 26.2 Complete Wiring Example

```kotlin
// MyApplication.kt
@HiltAndroidApp
class MyApplication : Application()

// di/DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "app.db").build()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()
}

// di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder().baseUrl("https://api.example.com/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()

    @Provides @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApiService =
        retrofit.create(UserApiService::class.java)

    @Provides @Singleton
    fun provideProductApi(retrofit: Retrofit): ProductApiService =
        retrofit.create(ProductApiService::class.java)
}

// di/RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindUserRepo(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun bindProductRepo(impl: ProductRepositoryImpl): ProductRepository
}

// domain/usecase/GetUserUseCase.kt
class GetUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(id: String) = userRepository.getUser(id)
}

// ui/user/UserViewModel.kt
@HiltViewModel
class UserViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase
) : ViewModel() {
    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    fun loadUser(id: String) {
        viewModelScope.launch {
            _user.value = getUserUseCase(id)
        }
    }
}

// ui/user/UserActivity.kt
@AndroidEntryPoint
class UserActivity : AppCompatActivity() {
    private val viewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        viewModel.user.observe(this) { user ->
            // update UI
        }

        viewModel.loadUser("user_123")
    }
}
```

---

## 27. 7-Day Learning Plan With Daily Exercises

### Day 1 — Feel the Pain, Understand the Solution

**Theory:** Read sections 1, 2, 3 of this guide.

**Exercise — The Pain:**
```kotlin
// Build a 3-screen app WITHOUT Hilt
// Create a UserRepository, manually
// Wire it to a ViewModel manually
// Notice how much code is repeated or duplicated
```

**Exercise — The Insight:**
- Count how many places you'd need to change if `UserRepository` needed a new dependency
- Count how many objects you create more than once

**Goal:** By end of day, you should feel WHY DI is needed.

---

### Day 2 — First Hilt App

**Theory:** Read sections 4, 5, 6, 7 of this guide.

**Setup Gradle:**
```kotlin
// project-level build.gradle
buildscript {
    dependencies {
        classpath 'com.google.dagger:hilt-android-gradle-plugin:2.48'
    }
}

// app-level build.gradle
plugins {
    id 'com.google.dagger.hilt.android'
    id 'kotlin-kapt'
}

dependencies {
    implementation "com.google.dagger:hilt-android:2.48"
    kapt "com.google.dagger:hilt-android-compiler:2.48"
}
```

**Exercise:**
```kotlin
// Step 1: Create Application class
@HiltAndroidApp
class App : Application()

// Step 2: Create a simple injectable class
class Greeter @Inject constructor() {
    fun greet(name: String) = "Hello, $name!"
}

// Step 3: Inject it into Activity
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var greeter: Greeter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println(greeter.greet("Shreyas"))  // Should print "Hello, Shreyas!"
    }
}
```

**What to observe:** The `greeter` field is set automatically. You never called `Greeter()`.

---

### Day 3 — Interfaces and @Binds

**Theory:** Read sections 8, 9, 10, 12 of this guide.

**Exercise — Reproduce your original error then fix it:**
```kotlin
// Step 1: Create interface
interface MessageService {
    fun sendMessage(to: String, content: String)
}

// Step 2: Create implementation
class EmailMessageService @Inject constructor() : MessageService {
    override fun sendMessage(to: String, content: String) {
        println("Sending email to $to: $content")
    }
}

// Step 3: Try injecting the interface WITHOUT a module
// Build. Observe the MissingBinding error.
// Read the error carefully.

// Step 4: Create the module
@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    @Binds
    abstract fun bindMessageService(impl: EmailMessageService): MessageService
}

// Step 5: Build again. It works now.

// Step 6: Add a second implementation
class SmsMessageService @Inject constructor() : MessageService {
    override fun sendMessage(to: String, content: String) {
        println("Sending SMS to $to: $content")
    }
}

// Step 7: Change the binding to use SMS instead. Notice how the Activity doesn't change.
```

**What to observe:** You switched implementations without touching the Activity.

---

### Day 4 — @Provides and Third-Party Libraries

**Theory:** Read sections 13, 14 of this guide.

**Exercise:**
```kotlin
// Provide a SharedPreferences
@Module
@InstallIn(SingletonComponent::class)
object PrefsModule {
    @Provides
    @Singleton
    fun provideSharedPrefs(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
    }
}

// Use it in a class
class UserSession @Inject constructor(
    private val prefs: SharedPreferences
) {
    fun saveUserId(id: String) = prefs.edit().putString("user_id", id).apply()
    fun getUserId(): String? = prefs.getString("user_id", null)
}

// Inject UserSession into Activity
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var userSession: UserSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userSession.saveUserId("12345")
        println(userSession.getUserId())  // 12345
    }
}
```

**Advanced exercise:** Chain two `@Provides`. Make `UserSession` depend on `SharedPreferences` which you provide. Then make another class depend on `UserSession`. Watch Hilt resolve the chain.

---

### Day 5 — Scopes

**Theory:** Read sections 11 of this guide thoroughly.

**Exercise — See Scoping in Action:**
```kotlin
// Scoped class
@Singleton
class Counter @Inject constructor() {
    private var count = 0
    fun increment() = ++count
    fun getCount() = count
}

// Inject Counter into two different classes
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var counter: Counter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        counter.increment()
        println("MainActivity counter: ${counter.getCount()}")  // 1
        // Start SecondActivity
    }
}

@AndroidEntryPoint
class SecondActivity : AppCompatActivity() {
    @Inject lateinit var counter: Counter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        counter.increment()
        println("SecondActivity counter: ${counter.getCount()}")  // 2! Same object!
    }
}
```

**Remove `@Singleton` and repeat.** The counter in `SecondActivity` will be 1 — a fresh `Counter` was created.

**Goal:** Physically see the difference between scoped and unscoped instances.

---

### Day 6 — ViewModel + Full Flow

**Theory:** Read sections 17 of this guide.

**Exercise — Complete mini app:**
```kotlin
// Repository
interface NoteRepository {
    fun getNotes(): List<String>
    fun addNote(note: String)
}

class NoteRepositoryImpl @Inject constructor() : NoteRepository {
    private val notes = mutableListOf<String>()
    override fun getNotes() = notes.toList()
    override fun addNote(note: String) { notes.add(note) }
}

// Module
@Module
@InstallIn(SingletonComponent::class)
abstract class NoteModule {
    @Binds @Singleton
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository
}

// ViewModel
@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {
    private val _notes = MutableLiveData<List<String>>(emptyList())
    val notes: LiveData<List<String>> = _notes

    fun addNote(text: String) {
        noteRepository.addNote(text)
        _notes.value = noteRepository.getNotes()
    }
}

// Activity
@AndroidEntryPoint
class NoteActivity : AppCompatActivity() {
    private val viewModel: NoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.notes.observe(this) { notes ->
            println("Notes: $notes")
        }
        viewModel.addNote("Learn Hilt")
        viewModel.addNote("Master DI")
    }
}
```

**Rotate the screen.** The notes survive because they're in the ViewModel.

---

### Day 7 — Build the Full Architecture

**Theory:** Read sections 26 of this guide.

**Exercise:** Build a complete User Management app with:
- Room database for local storage
- Retrofit for remote data (can use a public free API like jsonplaceholder.typicode.com)
- Repository pattern with interface + implementation
- Use cases for each operation
- ViewModel with Hilt
- Two screens: User List and User Detail

**The entire Hilt wiring must be done correctly from scratch. No copy-pasting — type every annotation yourself.**

---

## 28. Interview Mastery — Questions and Deep Answers

### Q1: What is Dependency Injection and why is it important?

**Answer:** Dependency Injection is a design pattern where a class receives its dependencies from an external source rather than creating them internally. A class declares what it needs (through constructor parameters or field annotations), and a DI framework or manual wiring provides those dependencies.

It matters because it achieves loose coupling — a class only depends on interfaces, not concrete implementations. This makes code testable (swap real implementations for fakes), maintainable (change implementations without touching consumers), and scalable (add new implementations without modifying existing classes).

---

### Q2: What is the difference between @Inject constructor and @Inject on a field?

**Answer:** They are completely opposite operations.

`@Inject constructor()` is an **offer** — it tells the DI system "I know how to create this class; use my constructor." The DI framework will generate a factory that calls this constructor.

`@Inject lateinit var` is a **request** — it tells the DI system "please provide me a value for this field." The field must be `lateinit var` because Hilt sets it after object creation.

Constructor injection is preferred because dependencies are explicit and classes remain testable without a DI framework. Field injection is only necessary for Android classes (Activities, Fragments) where the OS controls construction.

---

### Q3: Why do you need @Binds for interfaces?

**Answer:** Interfaces cannot be instantiated. When Hilt resolves a dependency graph and encounters an interface type, it cannot call `new UserRepository()` — that's not valid in Kotlin or Java. `@Binds` tells Hilt which concrete implementation to use when a particular interface is requested. Without it, Hilt raises a compile-time `MissingBinding` error. The parameter of a `@Binds` function is the implementation, and the return type is the interface — this is the explicit mapping from abstraction to concretion.

---

### Q4: What is the difference between @Singleton and SingletonComponent?

**Answer:** These are from entirely different systems and serve different purposes.

`@Singleton` is a scope annotation from `javax.inject`. It is placed on a `@Provides` or `@Binds` function (or a class with `@Inject constructor`) to tell Hilt: "Create only one instance of this type per SingletonComponent lifetime."

`SingletonComponent::class` is a Hilt component class from `dagger.hilt.components`. It is used inside `@InstallIn()` to specify which DI container a module belongs to.

A common mistake is writing `@InstallIn(Singleton::class)` — this fails at compile time because `Singleton` (the scope annotation) is not a Hilt component.

---

### Q5: When would you use @Provides over @Binds?

**Answer:** Use `@Binds` for mapping an interface to an implementation you own. Use `@Provides` when:

1. The class is from a third-party library (Retrofit, Room, OkHttp) — you can't add `@Inject constructor()` to code you don't own.
2. Object construction requires multiple steps or builder patterns.
3. You need to run logic before returning the object (setting up interceptors, configuring options).
4. The "implementation" is actually a complex object returned by a factory method.

`@Binds` is preferred over `@Provides` for interface-to-implementation mappings because it generates less bytecode and is more semantically clear.

---

### Q6: What is the Hilt component hierarchy and why does it matter?

**Answer:** Hilt defines a hierarchy of DI containers tied to Android lifecycle: `SingletonComponent` (Application) → `ActivityRetainedComponent` → `ActivityComponent` (Activity) → `FragmentComponent` (Fragment).

It matters for two reasons:

**Lifetime:** Objects installed in `SingletonComponent` live as long as the app. Objects in `ActivityComponent` are destroyed when the Activity is destroyed. Choosing wrong scope causes memory leaks (injecting Activity context into a Singleton) or waste (recreating expensive objects unnecessarily).

**Accessibility:** A lower component can access bindings from higher components, but not vice versa. A Fragment can use a `@Singleton` database. But a `SingletonComponent` module cannot use a Fragment-scoped binding — the Fragment doesn't exist yet at that level.

---

### Q7: What is the difference between @ActivityScoped and @ActivityRetainedScoped?

**Answer:** Both live "per Activity session" but with a critical difference: `@ActivityScoped` objects are destroyed on screen rotation. `@ActivityRetainedScoped` objects survive rotation — they share the lifecycle of a ViewModel.

In practice, this matters for things like user session state. If you want "one user session per Activity run" that survives rotation, use `@ActivityRetainedScoped`. If you want "one instance per Activity window that resets on rotation," use `@ActivityScoped`.

---

### Q8: What causes a DependencyCycle error and how do you fix it?

**Answer:** A dependency cycle occurs when Hilt cannot resolve a dependency graph because it's circular — A depends on B, B depends on A. The most common cause in practice is a `@Binds` function where the parameter type is the same as the return type:

```kotlin
@Binds
abstract fun bind(impl: UserRepository): UserRepository  // Cycle!
```

Hilt needs `UserRepository` to provide `UserRepository` — an infinite loop. The fix is to always use the concrete implementation as the parameter:

```kotlin
@Binds
abstract fun bind(impl: UserRepositoryImpl): UserRepository  // Correct
```

For genuine circular dependencies between classes, the fix is to inject `Provider<T>` instead of `T` for one of the dependencies, making one dependency lazy.

---

## Final Rules — Carry These Always

```
Rule 1: Never create dependencies inside a class. Declare them. Receive them.

Rule 2: Always inject interfaces, never implementations.
         @Inject lateinit var repo: UserRepository     ✓
         @Inject lateinit var repo: UserRepositoryImpl  ✗

Rule 3: @Inject constructor() = "Hilt can create me"
         @Inject lateinit var = "Hilt, give me this"
         These are not the same thing.

Rule 4: @Binds parameter = implementation, return = interface.
         Never the reverse. Never the interface as parameter.

Rule 5: @InstallIn takes a Component (SingletonComponent::class),
         NOT a scope (@Singleton). These are completely different.

Rule 6: @Singleton scope goes on @Provides/@Binds methods,
         NOT on @InstallIn.

Rule 7: Abstract methods (@Binds) need abstract class.
         Non-abstract methods (@Provides) need object or regular class.

Rule 8: @AndroidEntryPoint is required on every Android class
         that uses @Inject fields. Without it, fields are never set.

Rule 9: Hilt errors are compile-time. Read them completely.
         The error tells you exactly what is missing and where.

Rule 10: The dependency graph flows one direction.
          MainActivity → ViewModel → Repository → ApiService → Retrofit
          Hilt builds this from bottom to top, automatically.
```

---

> You now have the theory. You have the examples. You have the error explanations.  
> The next step is yours — open Android Studio and build.  
> Every concept in this guide will become 10x clearer after you type it yourself.

---

*This guide covers Hilt 2.48+. The concepts apply to all modern Hilt versions.*
