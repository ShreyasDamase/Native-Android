# 📺 LiveData — Complete Guide

> [!NOTE]
> **Sub-note for LiveData (Legacy).** LiveData is still widely found in existing codebases. For modern state management, use [[StateFlow]] instead. For the full migration guide, see the bottom of this file.

---

## 🧠 Mental Model — Read This First

**LiveData is a smart TV that only shows updates when you're in the room.**

- **Lifecycle-aware:** It automatically knows when the observer (Activity/Fragment) is visible (STARTED/RESUMED) or not (STOPPED/DESTROYED).
- **Safe delivery:** It delivers updates only when the observer can receive them — never to a destroyed Activity.
- **Last-value sticky:** If you were away and a new value came in, you get it the moment you return.

**Why it was created (2017):** Before LiveData, developers had to manually remove observers in `onStop()`/`onDestroy()` to prevent memory leaks and crashes from updating destroyed UI. LiveData automated this entirely.

---

## 📌 Table of Contents
1. [What LiveData Is](#1-what-livedata-is)
2. [MutableLiveData vs LiveData](#2-mutablelivedata-vs-livedata)
3. [Observing in Activity / Fragment](#3-observing)
4. [Posting Values — value vs postValue](#4-posting-values)
5. [LiveData Transformations](#5-transformations)
6. [MediatorLiveData — Combining Sources](#6-mediatorlivedata)
7. [LiveData with Room and Flow](#7-with-room-and-flow)
8. [LiveData Lifecycle Internals](#8-lifecycle-internals)
9. [LiveData Pros and Cons](#9-pros-and-cons)
10. [When You Still See LiveData](#10-when-you-still-see-it)
11. [Migration to StateFlow](#11-migration-to-stateflow)
12. [Full Comparison: LiveData vs StateFlow](#12-comparison-table)
13. [Interview Q&A](#13-interview)

---

## 1. What LiveData Is

`LiveData` is a **lifecycle-aware, observable data holder** from `androidx.lifecycle`. Introduced as part of Android Architecture Components in 2017.

**The Golden Rule:**
> LiveData ONLY delivers updates when the observer is in an **ACTIVE** lifecycle state (`STARTED` or `RESUMED`). It auto-removes observers when the lifecycle reaches `DESTROYED`.

```
Observer STARTED/RESUMED  → updates delivered ✅
Observer STOPPED          → updates stored, NOT delivered ⛔
Observer returns to STARTED → last stored value delivered immediately ✅
Observer DESTROYED        → observer automatically removed, no leaks ✅
```

---

## 2. MutableLiveData vs LiveData

Exactly mirrors the StateFlow pattern:

```kotlin
class UserViewModel : ViewModel() {

    // MutableLiveData — writable, always keep PRIVATE
    private val _user = MutableLiveData<User>()

    // LiveData — read-only, exposed PUBLICLY
    val user: LiveData<User> = _user

    fun loadUser(id: String) {
        viewModelScope.launch {
            val result = repository.getUser(id)
            _user.value = result    // update from Main thread
        }
    }
}
```

| | MutableLiveData | LiveData |
|---|---|---|
| Can write | ✅ Yes | ❌ No |
| Can read | ✅ Yes | ✅ Yes |
| Typical visibility | `private` | `public val` |
| How to create | `MutableLiveData<T>()` | `_x` (read-only view) |

---

## 3. Observing in Activity / Fragment

### Activity

```kotlin
class UserActivity : AppCompatActivity() {

    private val viewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        // ✅ 'this' passes the Activity as the LifecycleOwner
        // LiveData auto-removes this observer when Activity is DESTROYED
        viewModel.user.observe(this) { user ->
            binding.nameText.text = user.name
            binding.emailText.text = user.email
        }
    }
}
```

### Fragment — Use viewLifecycleOwner, NOT 'this'

```kotlin
class UserFragment : Fragment(R.layout.fragment_user) {

    private val viewModel: UserViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ CORRECT: viewLifecycleOwner — tied to the Fragment VIEW lifecycle
        viewModel.user.observe(viewLifecycleOwner) { user ->
            binding.nameText.text = user.name
        }

        // ❌ WRONG: 'this' — tied to FRAGMENT lifecycle (not view lifecycle)
        // The fragment can be detached/reattached but the view is recreated.
        // Using 'this' means the observer still exists even after the view is gone,
        // causing memory leaks and duplicate observers after re-attach.
        // viewModel.user.observe(this) { ... }
    }
}
```

> [!CAUTION]
> Always use `viewLifecycleOwner` in Fragments, **never** `this`. The Fragment's lifecycle outlives its view — between `onDestroyView()` and `onCreateView()`, the view is null but the Fragment is not destroyed, leading to null pointer crashes if you access binding inside the observer.

---

## 4. Posting Values — value vs postValue

```kotlin
// value — synchronous, MUST be called on Main thread
private val _count = MutableLiveData(0)
_count.value = 1     // ✅ safe on Main thread
_count.value++       // ✅ safe on Main thread

// postValue — asynchronous, safe from ANY thread
// Internally schedules the update to run on Main thread
viewModelScope.launch(Dispatchers.IO) {
    val result = heavyOperation()
    _count.postValue(result)    // ✅ safe from IO thread
}

// ❌ WRONG — value on background thread
Thread {
    _count.value = result   // throws CalledFromWrongThreadException!
}.start()
```

| | `.value` | `.postValue()` |
|---|---|---|
| Thread | Main only | Any thread |
| Synchronous | ✅ Yes | ❌ No (queued) |
| Multiple rapid calls | Each value is set | Only last value is set (previous ones may be dropped) |

> [!WARNING]
> If you call `postValue()` multiple times rapidly from a background thread, only the **last** value is guaranteed to be delivered. Intermediate values may be dropped. This is a known limitation of LiveData.

---

## 5. LiveData Transformations

`Transformations` functions let you create new LiveData derived from existing ones:

### map — Transform the Value

```kotlin
class UserViewModel : ViewModel() {

    private val _user = MutableLiveData<User>()

    // Derived LiveData: always uppercased version of user name
    val displayName: LiveData<String> = _user.map { user ->
        "${user.firstName} ${user.lastName}".uppercase()
    }

    // Another derived LiveData
    val isAdult: LiveData<Boolean> = _user.map { it.age >= 18 }
}
```

### switchMap — Swap to a Different LiveData

```kotlin
// Use when the argument LiveData controls WHICH data source to observe
class UserViewModel : ViewModel() {

    val selectedUserId = MutableLiveData<String>()

    // When selectedUserId changes, switch to observing a DIFFERENT LiveData
    val userDetails: LiveData<User> = selectedUserId.switchMap { id ->
        repository.getUserById(id)   // returns LiveData<User>
    }
}

// Usage: changing selectedUserId automatically triggers a new DB query
viewModel.selectedUserId.value = "user_123"
// → userDetails now observes repository.getUserById("user_123")
viewModel.selectedUserId.value = "user_456"
// → userDetails now observes repository.getUserById("user_456")
// → Previous observation is automatically disposed
```

### distinctUntilChanged — Skip Duplicates

```kotlin
// Only notify observer if the new value is different from the previous
val uniqueUser: LiveData<User> = _user.distinctUntilChanged()
```

---

## 6. MediatorLiveData — Combining Multiple Sources

`MediatorLiveData` can observe multiple `LiveData` sources and merge them:

```kotlin
class SearchViewModel : ViewModel() {

    private val searchQuery = MutableLiveData<String>()
    private val filterActive = MutableLiveData<Boolean>(false)

    // Combine search query + filter into one results LiveData
    val searchResults = MediatorLiveData<List<Product>>().apply {
        val update = {
            val query = searchQuery.value ?: ""
            val filter = filterActive.value ?: false
            value = repository.search(query, filter)
        }
        addSource(searchQuery) { update() }
        addSource(filterActive) { update() }
    }
}
```

> [!NOTE]
> In modern code, `combine()` on Flow (with `stateIn()`) is preferred over `MediatorLiveData`. It's cleaner and more powerful.

---

## 7. LiveData with Room and Flow

Room natively supports both `LiveData` and `Flow` return types. The query automatically re-runs when the underlying table changes:

```kotlin
@Dao
interface UserDao {
    // LiveData — older approach
    @Query("SELECT * FROM users")
    fun getAllUsersLiveData(): LiveData<List<User>>

    // Flow — modern approach (preferred)
    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<User>>
}
```

### Converting Flow to LiveData (Bridge for Legacy UIs)

```kotlin
class UserRepository(private val dao: UserDao) {
    // Returns Flow (modern)
    fun getUsers(): Flow<List<User>> = dao.getAllUsersFlow()
}

class UserViewModel(private val repo: UserRepository) : ViewModel() {
    // Option 1: Convert Flow → LiveData for Fragment using XML DataBinding
    val usersLiveData: LiveData<List<User>> = repo.getUsers().asLiveData()

    // Option 2: Keep as StateFlow for modern Compose or repeatOnLifecycle collection
    val users: StateFlow<List<User>> = repo.getUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

---

## 8. LiveData Lifecycle Internals

```
Activity / Fragment lifecycle:

CREATED
    │
STARTED ──────────────────────────────────── LiveData: start observing ✅
    │
RESUMED ──────────────────────────────────── User interacts with UI
    │
PAUSED ────────────────────────────────────── (still observed)
    │
STOPPED ──────────────────────────────────── LiveData: pause delivery ⛔
    │                                          (values stored, not delivered)
STARTED ──────────────────────────────────── LiveData: resume, deliver last value ✅
    │
DESTROYED ────────────────────────────────── LiveData: auto-remove observer ✅
                                              (no memory leak, no NPE)
```

**What happens on rotation:**
1. Activity DESTROYED → LiveData removes observer
2. Activity CREATED/STARTED → new observer added
3. LiveData immediately delivers last stored value
4. UI is fully restored

---

## 9. LiveData Pros and Cons

### Pros
- ✅ **Simple API** — `observe()` is intuitive for beginners
- ✅ **Lifecycle-aware built-in** — no manual lifecycle management
- ✅ **Works with DataBinding** — `android:text="@{viewModel.name}"` directly
- ✅ **Proven in production** — mature, well-documented, millions of apps

### Cons
- ❌ **Java-first design** — not idiomatic Kotlin, verbose for complex transformations
- ❌ **No error handling** — you must wrap results manually (use `Result<T>`)
- ❌ **Limited operators** — `map` and `switchMap` only; no `debounce`, `filter`, `combine` etc.
- ❌ **Android-only** — can't use in Kotlin Multiplatform
- ❌ **Always replays last value** — can't control replay behavior (problem for events)
- ❌ **postValue drops intermediate values** — rapid background updates may be lost
- ❌ **Hard to unit test** — requires `InstantTaskExecutorRule` and `TestLifecycleOwner`
- ❌ **No duplicate filtering** — same value triggers observer again

---

## 10. When You Still See LiveData

Even in 2025, you'll encounter LiveData in:

1. **Legacy codebases** — projects started before 2020 that haven't migrated
2. **XML DataBinding** — `@{viewModel.user.name}` in layout files requires LiveData
3. **Third-party libraries** — some libraries (especially older Room integrations) return LiveData
4. **Paging 3** — still supports `LiveData<PagingData<T>>` (though Flow is preferred)
5. **WorkManager** — `WorkManager.getWorkInfoByIdLiveData()` still used

### Reading LiveData code you encounter

```kotlin
// When you see this in existing code:
class OldViewModel : ViewModel() {
    private val _items = MutableLiveData<List<Item>>()
    val items: LiveData<List<Item>> = _items

    fun load() {
        viewModelScope.launch {
            _items.value = repository.getItems()
        }
    }
}

// In old Fragment:
viewModel.items.observe(viewLifecycleOwner) { items ->
    adapter.submitList(items)
}

// This is completely valid — just the older pattern.
// You don't need to immediately rewrite it to StateFlow.
```

---

## 11. Migration to StateFlow

### Step-by-Step Migration

```kotlin
// BEFORE: LiveData
class UserViewModel : ViewModel() {
    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    fun updateUser(user: User) {
        _user.value = user
    }
}

// In Fragment:
viewModel.user.observe(viewLifecycleOwner) { user ->
    binding.nameText.text = user.name
}

// AFTER: StateFlow
class UserViewModel : ViewModel() {
    private val _user = MutableStateFlow<User?>(null)    // ← requires initial value!
    val user: StateFlow<User?> = _user.asStateFlow()

    fun updateUser(user: User) {
        _user.value = user
    }
}

// In Fragment:
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.user.collect { user ->
            user?.let { binding.nameText.text = it.name }
        }
    }
}

// In Composable:
val user by viewModel.user.collectAsStateWithLifecycle()
```

### Migration for Room LiveData

```kotlin
// BEFORE: Room returning LiveData
@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAll(): LiveData<List<User>>
}

// In ViewModel:
val users: LiveData<List<User>> = dao.getAll()

// AFTER: Room returning Flow, converted to StateFlow in ViewModel
@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAll(): Flow<List<User>>    // ← change return type
}

// In ViewModel:
val users: StateFlow<List<User>> = dao.getAll()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

### Key Gotchas When Migrating

| LiveData behavior | StateFlow behavior | Action needed |
|---|---|---|
| No initial value required | **Initial value REQUIRED** | Choose a sensible default or use `null` |
| Does not filter duplicates | Filters identical values | Usually a benefit, no action |
| Delivers last value on rotation | Same | No change |
| Observer removed on DESTROYED | Must use `repeatOnLifecycle` | Add `repeatOnLifecycle` wrapper |
| `map` transform | Use Flow `.map{}` before `stateIn` | Refactor transformation chain |
| `switchMap` | Use `flatMapLatest` | Refactor with Flow operators |
| `MediatorLiveData` | Use Flow `combine` | Refactor to combine flows |

---

## 12. Full Comparison: LiveData vs StateFlow

| Feature | LiveData | StateFlow |
|---|---|---|
| **Language** | Java-first | Kotlin-first |
| **Initial value** | Optional (nullable) | **Required** |
| **Lifecycle aware** | ✅ Built-in | ❌ Needs `repeatOnLifecycle` |
| **Duplicate filtering** | ❌ No | ✅ Yes (`==` check) |
| **Operators** | `map`, `switchMap` only | Full Flow operators |
| **Error handling** | ❌ None | ✅ `catch {}` |
| **Multiple rapid updates** | May drop | Conflates (keeps latest) |
| **Unit testing** | Needs `InstantTaskExecutorRule` | Pure coroutine (`Turbine`) |
| **Background threads** | `postValue()` needed | Thread-safe `.update{}` |
| **Android dependency** | ✅ Yes | ❌ Pure Kotlin |
| **Kotlin Multiplatform** | ❌ No | ✅ Yes |
| **DataBinding** | ✅ Direct support | ❌ Needs `.asLiveData()` |
| **Replay behavior** | Always replays last | Replays last (same) |
| **Event delivery** | Can't prevent re-delivery | Use SharedFlow for events |
| **Modern recommendation** | Legacy | ✅ **Preferred** |

---

## 13. Interview Q&A

**Q: What is LiveData and what problem does it solve?**
> LiveData is a lifecycle-aware observable data holder. It solves two problems: (1) preventing memory leaks by automatically removing observers when the Activity/Fragment is destroyed, and (2) preventing crashes from updating UI that is no longer visible by only delivering values when the observer is in STARTED or RESUMED state. Before LiveData, developers manually managed observer cleanup in lifecycle callbacks, leading to frequent bugs.

**Q: Why should you use `viewLifecycleOwner` instead of `this` in a Fragment?**
> A Fragment has two lifecycles: its own lifecycle and its view's lifecycle. The Fragment itself can exist (be in the back stack) while its view has been destroyed and recreated. If you pass `this` (the Fragment) as the LifecycleOwner, the observer persists across view recreations, leading to duplicate observers and potential crashes when the observer tries to update a view that no longer exists. `viewLifecycleOwner` is correctly tied to the view's lifecycle — it's destroyed when the view is destroyed.

**Q: What is the difference between `value` and `postValue()` in MutableLiveData?**
> `.value` is synchronous and must be called on the Main (UI) thread. `.postValue()` is thread-safe — it can be called from any thread and internally schedules the update to run on the Main thread. However, if `postValue()` is called multiple times rapidly, only the last value is guaranteed to be delivered — intermediate values may be lost.

**Q: Why is StateFlow preferred over LiveData for new Android projects?**
> StateFlow is pure Kotlin (no Android dependency), making ViewModels fully testable without the Android framework. It supports all Flow operators (`debounce`, `combine`, `flatMapLatest`) that LiveData's limited `map`/`switchMap` can't match. It has built-in duplicate value filtering. It works with Kotlin Multiplatform for iOS/Desktop. The only trade-off is that StateFlow requires `repeatOnLifecycle` in Fragments (compared to LiveData's automatic lifecycle management), but this is a one-time boilerplate that's now standard in all modern Android projects.

**Q: What is MediatorLiveData and what is its modern equivalent?**
> `MediatorLiveData` observes multiple `LiveData` sources and merges their emissions into a single LiveData. The modern equivalent is `combine()` on Kotlin Flow, which is more powerful (supports more than 2 sources, supports all Flow operators, and works with coroutines for async transformations).

---

## 📦 Gradle Dependencies

```kotlin
// LiveData
implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")

// For asLiveData() extension (Flow → LiveData bridge)
implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")

// For ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")

// For DataBinding (if using LiveData with DataBinding)
buildFeatures { dataBinding = true }
```

---

## 🔗 Connections

- **Modern replacement**: [[StateFlow]] — use instead of LiveData for new code
- **Events**: [[SharedFlow]] — use instead of LiveData for one-time events
- **Cold Flow bridge**: [[Cold Flow]] — `Flow.asLiveData()` to bridge legacy XML UIs
- **Compose**: [[State]] — `collectAsStateWithLifecycle()` is the Compose equivalent of `observe()`
