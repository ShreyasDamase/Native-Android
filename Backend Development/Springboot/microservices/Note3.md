# SSE Complete Reference Notes

### Android (Kotlin + Compose) + Backend (Kotlin + Spring Boot)

> Use this as a living reference while building. Every section has code, rules, and traps.

---

## Table of Contents

1. [Folder Structure](#1-folder-structure)
2. [Backend — Spring Boot](#2-backend--spring-boot)
3. [Android Frontend](#3-android-frontend)
4. [Room + SSE Integration](#4-room--sse-integration)
5. [ViewModel Patterns](#5-viewmodel-patterns)
6. [Lifecycle Management](#6-lifecycle-management)
7. [Reconnect Strategy](#7-reconnect-strategy)
8. [Do's and Don'ts](#8-dos-and-donts)
9. [Pros and Cons of SSE](#9-pros-and-cons-of-sse)
10. [Common Bugs and Fixes](#10-common-bugs-and-fixes)
11. [Quick Cheatsheet](#11-quick-cheatsheet)

---

## 1. Folder Structure

### Backend (Spring Boot)

```
content-service/
└── src/main/kotlin/com/yourapp/content/
    ├── sse/
    │   ├── SseController.kt        ← HTTP endpoint only, no logic
    │   ├── SseBroadcaster.kt       ← manages emitters, sends events
    │   └── SseEvent.kt             ← all SSE event data classes
    ├── post/
    │   ├── PostController.kt
    │   ├── PostService.kt          ← calls sseBroadcaster.broadcast()
    │   ├── PostRepository.kt
    │   └── PostEntity.kt
    ├── comment/
    │   ├── CommentService.kt       ← also calls sseBroadcaster after writes
    │   └── ...
    └── config/
        └── SseConfig.kt           ← CORS, timeout config for SSE
```

**Rule:** SSE folder is isolated. Services call into it. It never calls back into services. One-way dependency.

---

### Android (Kotlin + Compose)

```
app/src/main/java/com/yourapp/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── PostDao.kt
│   │   │   └── CommentDao.kt
│   │   ├── entity/
│   │   │   ├── PostEntity.kt
│   │   │   └── CommentEntity.kt
│   │   └── AppDatabase.kt
│   ├── remote/
│   │   ├── api/
│   │   │   └── PostApiService.kt   ← Retrofit calls (like, comment POST)
│   │   └── sse/
│   │       ├── SseClient.kt        ← OkHttp EventSource wrapper
│   │       ├── SseEvent.kt         ← mirrored data classes from backend
│   │       └── SseManager.kt      ← optional: global SSE lifecycle manager
│   └── repository/
│       └── PostRepository.kt      ← combines remote + local
├── di/
│   └── NetworkModule.kt            ← provides OkHttpClient, SseClient
├── ui/
│   └── postdetail/
│       ├── PostDetailScreen.kt
│       ├── PostDetailViewModel.kt
│       └── PostDetailUiState.kt
└── util/
    └── NetworkMonitor.kt           ← observe connectivity for reconnect
```

**Rule:** SSE lives in `data/remote/sse/`. ViewModel never imports OkHttp directly. Repository or SseClient is the boundary.

---

## 2. Backend — Spring Boot

### SseConfig.kt — Set this up first

```kotlin
// config/SseConfig.kt
@Configuration
class SseConfig : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins("*")              // lock this down in production
            .allowedMethods("GET", "POST")
            .allowedHeaders("*")
            .exposedHeaders("Content-Type")   // CRITICAL: expose for SSE clients
    }
}
```

> ⚠️ Without proper CORS, SSE connections silently fail on web clients and some proxy setups.

---

### SseEvent.kt — All your event shapes in one place

```kotlin
// sse/SseEvent.kt

// The envelope — only send fields that changed, leave others null
data class PostUpdateEvent(
    val postId: String,
    val likeCount: Int? = null,
    val commentCount: Int? = null,
    val bookmarkCount: Int? = null,
    val newComment: CommentPayload? = null,
    val eventType: String = "post_update"   // useful for client-side filtering
)

data class CommentPayload(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val content: String,
    val createdAt: String              // ISO 8601: "2024-01-15T10:30:00Z"
)

// Add more event types as your app grows:
// data class StoryUpdateEvent(...)
// data class NotificationEvent(...)
```

---

### SseBroadcaster.kt — The core component

```kotlin
// sse/SseBroadcaster.kt
@Component
class SseBroadcaster {

    private val subscribers: ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> =
        ConcurrentHashMap()

    // Track total connections for monitoring
    private val totalConnections = AtomicInteger(0)

    fun register(postId: String, emitter: SseEmitter) {
        val list = subscribers.getOrPut(postId) { CopyOnWriteArrayList() }
        list.add(emitter)
        totalConnections.incrementAndGet()

        // Always register all three cleanup callbacks
        emitter.onCompletion {
            remove(postId, emitter)
            totalConnections.decrementAndGet()
        }
        emitter.onTimeout {
            remove(postId, emitter)
            totalConnections.decrementAndGet()
        }
        emitter.onError {
            remove(postId, emitter)
            totalConnections.decrementAndGet()
        }

        // Send initial heartbeat so client knows connection is alive
        try {
            emitter.send(
                SseEmitter.event()
                    .name("connected")
                    .data(mapOf("postId" to postId, "status" to "connected"))
            )
        } catch (e: Exception) {
            remove(postId, emitter)
        }
    }

    fun broadcast(postId: String, event: PostUpdateEvent) {
        val list = subscribers[postId] ?: return   // no subscribers, exit fast
        if (list.isEmpty()) return

        val dead = mutableListOf<SseEmitter>()

        list.forEach { emitter ->
            try {
                emitter.send(
                    SseEmitter.event()
                        .id(System.currentTimeMillis().toString()) // Last-Event-ID support
                        .name(event.eventType)
                        .data(event)
                        .reconnectTime(3000)    // tells client to wait 3s before reconnect
                )
            } catch (e: Exception) {
                dead.add(emitter)
            }
        }

        // Clean dead connections outside the loop
        dead.forEach { remove(postId, it) }
    }

    // Send heartbeat to all connections for a post (call this on a schedule)
    fun sendHeartbeat(postId: String) {
        val list = subscribers[postId] ?: return
        val dead = mutableListOf<SseEmitter>()
        list.forEach { emitter ->
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"))
            } catch (e: Exception) {
                dead.add(emitter)
            }
        }
        dead.forEach { remove(postId, it) }
    }

    fun getSubscriberCount(postId: String): Int = subscribers[postId]?.size ?: 0
    fun getTotalConnections(): Int = totalConnections.get()

    private fun remove(postId: String, emitter: SseEmitter) {
        subscribers[postId]?.remove(emitter)
        // Clean up empty lists to prevent memory leak
        if (subscribers[postId]?.isEmpty() == true) {
            subscribers.remove(postId)
        }
    }
}
```

---

### SseController.kt — Thin, no logic

```kotlin
// sse/SseController.kt
@RestController
@RequestMapping("/api/posts")
class SseController(private val sseBroadcaster: SseBroadcaster) {

    @GetMapping("/{postId}/live", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribeToPost(
        @PathVariable postId: String,
        @RequestHeader(value = "Last-Event-ID", required = false) lastEventId: String?
    ): SseEmitter {
        // 60s timeout — client must reconnect after this
        // -1L = infinite (don't use in production, leaks memory)
        val emitter = SseEmitter(60_000L)
        sseBroadcaster.register(postId, emitter)

        // Optional: replay missed events if client sends Last-Event-ID
        // lastEventId?.let { replayMissedEvents(postId, it, emitter) }

        return emitter
    }

    // Debug — remove before production
    @GetMapping("/debug/sse-count/{postId}")
    fun debugCount(@PathVariable postId: String): Map<String, Any> =
        mapOf(
            "postId" to postId,
            "subscribers" to sseBroadcaster.getSubscriberCount(postId),
            "totalConnections" to sseBroadcaster.getTotalConnections()
        )
}
```

---

### Heartbeat Scheduler — Prevents proxy timeouts

```kotlin
// sse/SseHeartbeatScheduler.kt
@Component
class SseHeartbeatScheduler(private val sseBroadcaster: SseBroadcaster) {

    // Many proxies (nginx, AWS ALB) kill connections idle for 60s
    // Send a heartbeat every 30s to keep them alive
    @Scheduled(fixedDelay = 30_000)
    fun sendHeartbeats() {
        sseBroadcaster.broadcastHeartbeatToAll()
    }
}
```

```kotlin
// Add this to SseBroadcaster:
fun broadcastHeartbeatToAll() {
    subscribers.keys.forEach { postId -> sendHeartbeat(postId) }
}
```

> Enable scheduling in your main app: `@EnableScheduling` on your `@SpringBootApplication` class.

---

### Calling broadcaster from services

```kotlin
// post/LikeService.kt
@Service
class LikeService(
    private val likeRepository: LikeRepository,
    private val postRepository: PostRepository,
    private val sseBroadcaster: SseBroadcaster
) {
    @Transactional
    fun likePost(postId: String, userId: String) {
        // 1. Write to DB
        likeRepository.save(Like(postId = postId, userId = userId))
        val newCount = postRepository.incrementAndGetLikeCount(postId)

        // 2. Broadcast AFTER successful DB write
        // If DB fails, @Transactional rolls back, no broadcast happens
        sseBroadcaster.broadcast(postId, PostUpdateEvent(
            postId = postId,
            likeCount = newCount
        ))
    }
}
```

> ⚠️ Always broadcast AFTER the DB write, never before. If broadcast is inside `@Transactional` and DB rolls back, the event was already sent — inconsistency.

---

## 3. Android Frontend

### Dependencies

```kotlin
// build.gradle.kts (app)
dependencies {
    // OkHttp SSE
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")

    // JSON parsing
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
}
```

---

### SseEvent.kt — Mirror your backend model exactly

```kotlin
// data/remote/sse/SseEvent.kt
@Serializable
data class PostUpdateEvent(
    val postId: String,
    val likeCount: Int? = null,
    val commentCount: Int? = null,
    val bookmarkCount: Int? = null,
    val newComment: CommentPayload? = null,
    val eventType: String = "post_update"
)

@Serializable
data class CommentPayload(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String? = null,
    val content: String,
    val createdAt: String
)
```

> Keep Android and backend event models in sync. If you add a field on backend, add it here too (with a default value so old events don't crash).

---

### SseClient.kt — The OkHttp wrapper

```kotlin
// data/remote/sse/SseClient.kt
class SseClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @Named("baseUrl") private val baseUrl: String
) {
    private var eventSource: EventSource? = null
    private val json = Json { ignoreUnknownKeys = true }   // IMPORTANT: future-proof

    fun connect(
        postId: String,
        onConnected: () -> Unit = {},
        onEvent: (PostUpdateEvent) -> Unit,
        onFailure: (Throwable?) -> Unit = {},
        onClosed: () -> Unit = {}
    ) {
        disconnect()

        val request = Request.Builder()
            .url("$baseUrl/api/posts/$postId/live")
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")    // prevent caching of SSE stream
            .header("Authorization", "Bearer ${TokenManager.getToken()}")
            .build()

        eventSource = EventSources.createFactory(okHttpClient)
            .newEventSource(request, object : EventSourceListener() {

                override fun onOpen(eventSource: EventSource, response: Response) {
                    onConnected()
                }

                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    if (data.isBlank()) return   // ignore heartbeat comments
                    try {
                        val event = json.decodeFromString<PostUpdateEvent>(data)
                        onEvent(event)
                    } catch (e: SerializationException) {
                        // Log parse error but don't crash — unknown event shape
                        Log.w("SseClient", "Unknown event format: $data", e)
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    onClosed()
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?
                ) {
                    Log.e("SseClient", "SSE failure: ${response?.code}", t)
                    onFailure(t)
                }
            })
    }

    fun disconnect() {
        eventSource?.cancel()
        eventSource = null
    }
}
```

---

### NetworkModule.kt — Provide OkHttpClient with right timeouts

```kotlin
// di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)     // ← CRITICAL: 0 = no read timeout for SSE
            .writeTimeout(10, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)   // WebSocket pings (helps keep SSE alive too)
            .retryOnConnectionFailure(true)
            .build()

    @Provides
    @Singleton
    fun provideSseClient(
        okHttpClient: OkHttpClient,
        @Named("baseUrl") baseUrl: String
    ): SseClient = SseClient(okHttpClient, baseUrl)
}
```

> `readTimeout(0)` is the most important line. Default timeout is 10s — your SSE connection will die every 10 seconds without this.

---

## 4. Room + SSE Integration

### PostEntity.kt

```kotlin
// data/local/entity/PostEntity.kt
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorName: String,
    val content: String,
    val imageUrl: String? = null,

    // These three are updated by SSE events
    @ColumnInfo(name = "like_count") val likeCount: Int = 0,
    @ColumnInfo(name = "comment_count") val commentCount: Int = 0,
    @ColumnInfo(name = "bookmark_count") val bookmarkCount: Int = 0,

    // User-specific state (NOT updated by SSE — SSE is for aggregate counts)
    @ColumnInfo(name = "is_liked_by_me") val isLikedByMe: Boolean = false,
    @ColumnInfo(name = "is_bookmarked_by_me") val isBookmarkedByMe: Boolean = false,

    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String
)
```

---

### PostDao.kt

```kotlin
// data/local/dao/PostDao.kt
@Dao
interface PostDao {

    // Observe a single post — re-emits on any column change
    @Query("SELECT * FROM posts WHERE id = :postId")
    fun observePost(postId: String): Flow<PostEntity?>

    // Observe feed — re-emits when ANY post in feed changes
    @Query("SELECT * FROM posts ORDER BY created_at DESC LIMIT :limit")
    fun observeHomeFeed(limit: Int = 50): Flow<List<PostEntity>>

    @Upsert
    suspend fun upsert(post: PostEntity)

    @Upsert
    suspend fun upsertAll(posts: List<PostEntity>)

    // Surgical updates — only touch what SSE changed
    @Query("UPDATE posts SET like_count = :count WHERE id = :postId")
    suspend fun updateLikeCount(postId: String, count: Int)

    @Query("UPDATE posts SET comment_count = :count WHERE id = :postId")
    suspend fun updateCommentCount(postId: String, count: Int)

    @Query("UPDATE posts SET bookmark_count = :count WHERE id = :postId")
    suspend fun updateBookmarkCount(postId: String, count: Int)

    @Query("UPDATE posts SET is_liked_by_me = :liked WHERE id = :postId")
    suspend fun updateIsLikedByMe(postId: String, liked: Boolean)

    @Query("SELECT like_count FROM posts WHERE id = :postId")
    suspend fun getLikeCount(postId: String): Int?
}
```

---

### CommentDao.kt

```kotlin
// data/local/dao/CommentDao.kt
@Dao
interface CommentDao {

    @Query("SELECT * FROM comments WHERE post_id = :postId ORDER BY created_at ASC")
    fun observeComments(postId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)  // IGNORE prevents duplicates
    suspend fun insert(comment: CommentEntity)

    @Upsert
    suspend fun upsertAll(comments: List<CommentEntity>)
}
```

---

## 5. ViewModel Patterns

### PostDetailViewModel.kt — Full pattern

```kotlin
// ui/postdetail/PostDetailViewModel.kt
@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val sseClient: SseClient,
    private val postRepository: PostRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val postId: String = checkNotNull(savedStateHandle["postId"])

    // SSE connection state — useful for showing a "live" indicator in UI
    private val _isLive = MutableStateFlow(false)
    val isLive: StateFlow<Boolean> = _isLive.asStateFlow()

    // Room-backed state — UI observes this, never SSE directly
    val post: StateFlow<PostEntity?> = postDao
        .observePost(postId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val comments: StateFlow<List<CommentEntity>> = commentDao
        .observeComments(postId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Error state for UI to show snackbar
    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    // Called from Composable when screen becomes visible
    fun onScreenVisible() {
        openSseConnection()
    }

    // Called from Composable when screen is hidden/destroyed
    fun onScreenHidden() {
        sseClient.disconnect()
        _isLive.value = false
        reconnectJob?.cancel()
    }

    private fun openSseConnection() {
        sseClient.connect(
            postId = postId,
            onConnected = {
                _isLive.value = true
                retryCount = 0
            },
            onEvent = { event ->
                viewModelScope.launch(Dispatchers.IO) {
                    handleSseEvent(event)
                }
            },
            onFailure = {
                _isLive.value = false
                scheduleReconnect()
            },
            onClosed = {
                _isLive.value = false
            }
        )
    }

    private suspend fun handleSseEvent(event: PostUpdateEvent) {
        event.likeCount?.let     { postDao.updateLikeCount(postId, it) }
        event.commentCount?.let  { postDao.updateCommentCount(postId, it) }
        event.bookmarkCount?.let { postDao.updateBookmarkCount(postId, it) }
        event.newComment?.let    { commentDao.insert(it.toCommentEntity(postId)) }
    }

    // Optimistic like toggle
    fun toggleLike() {
        viewModelScope.launch {
            val current = post.value ?: return@launch
            val wasLiked = current.isLikedByMe
            val newCount = if (wasLiked) current.likeCount - 1 else current.likeCount + 1

            // Optimistic update — instant UI response
            postDao.updateLikeCount(postId, newCount)
            postDao.updateIsLikedByMe(postId, !wasLiked)

            try {
                if (wasLiked) postRepository.unlikePost(postId)
                else postRepository.likePost(postId)
                // SSE will eventually confirm the new count from server
            } catch (e: Exception) {
                // Rollback
                postDao.updateLikeCount(postId, current.likeCount)
                postDao.updateIsLikedByMe(postId, wasLiked)
                _error.emit("Failed to update like. Try again.")
            }
        }
    }

    // Reconnect logic
    private var reconnectJob: Job? = null
    private var retryCount = 0

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = viewModelScope.launch {
            if (retryCount >= 5) {
                _error.emit("Live updates unavailable. Pull to refresh.")
                return@launch
            }
            val backoffMs = minOf(
                (2.0.pow(retryCount) * 1_000L).toLong(),
                30_000L    // cap at 30s
            )
            delay(backoffMs)
            retryCount++
            openSseConnection()
        }
    }

    override fun onCleared() {
        super.onCleared()
        sseClient.disconnect()
    }
}
```

---

## 6. Lifecycle Management

### PostDetailScreen.kt — Correct Compose pattern

```kotlin
// ui/postdetail/PostDetailScreen.kt
@Composable
fun PostDetailScreen(
    postId: String,
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val post by viewModel.post.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val isLive by viewModel.isLive.collectAsStateWithLifecycle()

    // CORRECT lifecycle hook for SSE
    // Runs on: first composition, recomposition with different postId
    // Cleans up on: navigation away, activity destroy, screen rotation
    DisposableEffect(postId) {
        viewModel.onScreenVisible()
        onDispose {
            viewModel.onScreenHidden()
        }
    }

    // Collect one-shot errors
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // Live indicator
            if (isLive) {
                Text(
                    text = "● LIVE",
                    color = Color.Green,
                    fontSize = 12.sp
                )
            }

            post?.let { p ->
                PostCard(
                    post = p,
                    onLike = { viewModel.toggleLike() }
                )
            }

            LazyColumn {
                items(
                    items = comments,
                    key = { it.id }   // stable keys prevent flicker on insert
                ) { comment ->
                    CommentItem(comment = comment)
                }
            }
        }
    }
}
```

---

### What NOT to do with lifecycle

```kotlin
// ❌ WRONG — opens SSE in init, never closes
@HiltViewModel
class PostDetailViewModel : ViewModel() {
    init {
        openSseConnection()  // leaks when user navigates away
    }
}

// ❌ WRONG — LaunchedEffect doesn't guarantee cleanup
LaunchedEffect(postId) {
    viewModel.onScreenVisible()
    // no cleanup path!
}

// ❌ WRONG — collectAsState instead of collectAsStateWithLifecycle
val post by viewModel.post.collectAsState()
// continues collecting even when app is in background

// ✅ CORRECT — DisposableEffect + collectAsStateWithLifecycle
DisposableEffect(postId) {
    viewModel.onScreenVisible()
    onDispose { viewModel.onScreenHidden() }
}
val post by viewModel.post.collectAsStateWithLifecycle()
```

---

## 7. Reconnect Strategy

```kotlin
// Exponential backoff with jitter (prevents thundering herd)
private fun scheduleReconnect() {
    reconnectJob?.cancel()
    reconnectJob = viewModelScope.launch {
        if (retryCount >= MAX_RETRIES) {
            _isLive.value = false
            return@launch
        }
        
        // Add jitter: randomize ±20% of backoff to spread reconnect load
        val baseBackoff = (2.0.pow(retryCount) * 1_000L).toLong()
        val jitter = (baseBackoff * 0.2 * Random.nextDouble(-1.0, 1.0)).toLong()
        val backoff = (baseBackoff + jitter).coerceIn(1_000L, 30_000L)
        
        delay(backoff)
        retryCount++
        openSseConnection()
    }
}

// Also reconnect when network comes back
class NetworkMonitor @Inject constructor(@ApplicationContext context: Context) {
    val isOnline: Flow<Boolean> = callbackFlow {
        val manager = context.getSystemService<ConnectivityManager>()!!
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
        }
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }
}

// In ViewModel — watch network and reconnect
init {
    viewModelScope.launch {
        networkMonitor.isOnline
            .distinctUntilChanged()
            .filter { it }           // only on "back online" events
            .drop(1)                 // skip initial emission
            .collect {
                retryCount = 0       // reset backoff
                openSseConnection()
            }
    }
}
```

---

## 8. Do's and Don'ts

### ✅ DO — Backend

|What|Why|
|---|---|
|Set `readTimeout(0)` on OkHttpClient|Without it, connections die at the default timeout|
|Use `CopyOnWriteArrayList` for emitters|Safe for concurrent add/remove without locks|
|Register onCompletion + onTimeout + onError|If any is missing, dead connections leak memory|
|Send heartbeats every 30s|Proxies (nginx, ALB) kill idle connections at 60s|
|Broadcast AFTER `@Transactional` DB write|Ensures client never gets an event for data that rolled back|
|Use `null` for unchanged fields in event|Don't send the full object — only send what changed|
|Set `reconnectTime` in SSE event|Gives client a hint on how long to wait before reconnect|
|Remove empty subscriber lists|Prevents unbounded memory growth in `ConcurrentHashMap`|

### ❌ DON'T — Backend

|What|Why|
|---|---|
|Use `SseEmitter(-1L)` (infinite timeout)|Memory leak — connections accumulate if clients crash silently|
|Broadcast inside `@Transactional` before commit|Clients see events for data that may roll back|
|Share one `SseEmitter` across requests|Each HTTP request must get its own emitter instance|
|Use `ArrayList` for emitters|Not thread-safe — will crash under concurrent adds/removes|
|Block the main thread in broadcast|Send async or on a separate thread if broadcasting is heavy|
|Put business logic in SseController|Controller only creates emitter and registers it|

---

### ✅ DO — Android

|What|Why|
|---|---|
|`ignoreUnknownKeys = true` in Json config|Backend adds new fields → client won't crash on old version|
|Use `DisposableEffect` for SSE lifecycle|Guaranteed cleanup on navigation, rotation, and destroy|
|Write SSE events to Room, not StateFlow|All screens sharing the same post update automatically|
|Use `collectAsStateWithLifecycle`|Cancels collection in background, saves battery|
|Use stable `key` in `LazyColumn items`|Prevents list jumping when SSE inserts new comments|
|Do Room writes on `Dispatchers.IO`|SSE callback is on OkHttp thread, Room needs IO dispatcher|
|Optimistic update + rollback on failure|Feels instant; correct on error|
|Cap reconnect backoff at 30s|Prevents infinitely long waits on bad networks|

### ❌ DON'T — Android

|What|Why|
|---|---|
|Import OkHttp in ViewModel|ViewModel shouldn't know about HTTP layer|
|Use default `readTimeout` for SSE OkHttpClient|10s default kills SSE every 10 seconds|
|Share OkHttpClient between SSE and REST|SSE needs `readTimeout(0)`, REST needs a normal timeout|
|Collect SSE events in UI layer|UI only reads Room. SSE writes Room. Separation of concerns|
|Use `collectAsState()` without lifecycle|Keeps collecting in background, wastes battery and network|
|Connect SSE on app start for all posts|Only connect on the screen that needs live data|
|Parse JSON with `try/catch` around the whole event loop|Catch only parse errors — let other exceptions propagate|
|Forget to cancel `reconnectJob` in `onCleared`|Causes coroutine leak after ViewModel is destroyed|

---

## 9. Pros and Cons of SSE

### ✅ Pros

- **Simple protocol** — plain HTTP GET. Works through every proxy, CDN, and load balancer that supports HTTP/2.
- **No upgrade handshake** — unlike WebSocket, no protocol upgrade. Falls back to HTTP/1.1 automatically.
- **Automatic reconnect built into browser spec** — browser SSE clients reconnect automatically (Android OkHttp needs manual reconnect).
- **Works with HTTP/2 multiplexing** — multiple SSE streams share one TCP connection in HTTP/2.
- **One-way is enough for most social features** — likes, comment counts, new comments don't need the client to push anything in real time.
- **Stateless server** — each SSE connection is independent. Scales horizontally (with a pub/sub layer like Redis for multi-node).
- **Fire-and-forget for server** — server pushes and forgets. No acknowledgment needed.

### ❌ Cons

- **Server memory per connection** — each open SSE emitter holds a thread or memory slot. At 10k concurrent users on one post, you need a strategy (Redis pub/sub + async emitters).
- **One-way only** — client can never push data on the SSE connection. Use REST for client actions.
- **No built-in acknowledgment** — server doesn't know if client received the event. If client drops mid-event, the event is lost unless you implement Last-Event-ID replay.
- **Mobile network gaps** — reconnects happen often. You need backoff logic and to re-fetch state on reconnect (SSE doesn't guarantee delivery during disconnect).
- **Proxy/firewall issues** — some corporate proxies buffer the response, breaking streaming. Set `X-Accel-Buffering: no`in nginx.
- **Not for high-frequency data** — if you're pushing 100 events/sec per client (game state, cursors), WebSocket is more efficient.

---

### When to switch away from SSE

|Scenario|Switch to|
|---|---|
|You need client to push data continuously (game, collab editor)|WebSocket|
|Delivery guarantee is critical (financial transactions)|WebSocket + acks OR polling with idempotency|
|Sub-100ms latency required|WebSocket over WebTransport|
|Scale to millions of concurrent connections|SSE + Redis pub/sub OR push notifications|

---

## 10. Common Bugs and Fixes

### Bug: SSE connection dies every 10 seconds

**Cause:** Default OkHttp read timeout is 10s. **Fix:** `readTimeout(0, TimeUnit.SECONDS)` on the OkHttpClient used for SSE.

---

### Bug: Memory leak — subscriber list grows forever

**Cause:** Emitter's `onTimeout`/`onError` callbacks not registered, or empty lists not removed. **Fix:** Register all three callbacks. Remove the list key when it's empty.

---

### Bug: UI doesn't update after SSE event

**Cause:** Room write happening on wrong thread, or `collectAsState()` instead of `collectAsStateWithLifecycle()`. **Fix:** `Dispatchers.IO`for Room writes. `collectAsStateWithLifecycle()` in Composables.

---

### Bug: Comment appears twice after user posts

**Cause:** Optimistic insert + SSE broadcasts the new comment back to the author. **Fix:** Server should not send SSE for the post_update back to the actor who triggered it. OR use `OnConflictStrategy.IGNORE` in `CommentDao.insert()`.

---

### Bug: SSE stream broken by nginx proxy

**Cause:** Nginx buffers responses by default. **Fix:** Add to nginx location block:

```nginx
proxy_buffering off;
proxy_cache off;
proxy_set_header X-Accel-Buffering no;
```

---

### Bug: App crashes on unknown event type after backend update

**Cause:** Backend added a new event field, Android's strict JSON parser throws. **Fix:** `Json { ignoreUnknownKeys = true }` in Kotlin Serialization config.

---

### Bug: Like count flickers (optimistic → SSE value → correct)

**Cause:** Optimistic update sets count to N+1, then SSE arrives with the real value from DB (also N+1) but gets applied again. **Fix:** The SSE value IS the truth. Apply it directly. Room will update if it changed, no-op if it's the same.

---

### Bug: Multiple SSE connections open for the same post

**Cause:** `connect()` called multiple times without `disconnect()` first. **Fix:** `disconnect()` is always the first line in `connect()`. One `SseClient` per ViewModel.

---

## 11. Quick Cheatsheet

```
SSE Connection Lifecycle:
Screen enters → DisposableEffect → onScreenVisible() → sseClient.connect()
Screen exits  → onDispose        → onScreenHidden()  → sseClient.disconnect()

Data Flow:
SSE event → ViewModel.handleSseEvent() → postDao.updateX() → Room notifies → Flow emits → UI recomposes

Optimistic Update Flow:
User action → update Room immediately → API call → success: SSE confirms / failure: rollback Room

Reconnect Flow:
onFailure → scheduleReconnect() → exponential backoff → connect() again
Network restored → reset retryCount → connect() again

Thread Rules:
SSE callback    → OkHttp thread  → launch(Dispatchers.IO) for Room
Room queries    → Dispatchers.IO
UI state        → Main thread (StateFlow handles this automatically)
```

---

### Backend endpoint summary

|Endpoint|Method|Purpose|
|---|---|---|
|`/api/posts/{postId}/live`|GET|SSE subscription|
|`/api/posts/{postId}/like`|POST|Like action (triggers SSE broadcast)|
|`/api/posts/{postId}/comments`|POST|New comment (triggers SSE broadcast)|
|`/api/debug/sse-count/{postId}`|GET|Debug: active subscriber count|

---

### Verify the full system is working (in order)

1. `curl -N -H "Accept: text/event-stream" http://localhost:8080/api/posts/TEST-123/live` → should hang open and print events
2. `GET /api/debug/sse-count/TEST-123` → should return `{"subscribers": 1}`
3. Like the post from another device → curl terminal should print `event: post_update data: {...}`
4. Android Studio → App Inspection → Database Inspector → watch `like_count` column increment live
5. Android UI → like count updates without pull-to-refresh
   
   
   
   
   Good. Now this is solid. You’re no longer guessing—you’re designing a real system.

👉 You’ve basically chosen **SSE (Server-Sent Events)** as your “Uber-like event system”.  
And honestly — **this is the closest correct answer to what you were trying to achieve**.

I went through your plan:  
Let’s validate it like a senior engineer.

---

# 🧠 First: Your decision (SSE) — is it correct?

## ✅ YES — for your use case

You want:

- real-time-ish updates
    
- no constant polling
    
- no WebSocket complexity
    
- event-driven backend
    

👉 SSE fits perfectly because:

- one-way (server → client) → exactly your need
    
- lightweight vs WebSocket infra
    
- easy to scale per feature (per post)
    

---

# ⚙️ Why this solves your original problem

Your concern was:

> “User A interacts → User B should see immediately without polling”

### SSE flow:

```text
User A likes
→ Backend updates DB
→ SSE broadcast(postId)
→ User B (subscribed to that post)
→ receives event
→ updates Room
→ UI updates instantly
```

👉 That is **true real-time behavior without WebSocket**

---

# 🧠 What you got VERY RIGHT

## 1. Scoped subscriptions (CRITICAL)

```text
subscribe(postId)
```

👉 Not global feed

This is **exactly like Uber zones / partitions**

✔ scalable  
✔ avoids unnecessary updates

---

## 2. Event-driven backend

```kotlin
sseBroadcaster.broadcast(postId, PostUpdateEvent(...))
```

👉 This is your **event system**

This is what you were trying to describe earlier.

---

## 3. Partial updates (VERY GOOD)

```kotlin
likeCount: Int? = null
```

👉 Only send changed fields

✔ efficient  
✔ avoids full object overwrite  
✔ reduces bugs

---

## 4. Room as source of truth

```text
SSE → DAO update → Flow → UI
```

👉 This is **industry pattern**

---

## 5. Lifecycle-aware connection

```kotlin
DisposableEffect → connect / disconnect
```

👉 You avoided:

- memory leaks
    
- unnecessary connections
    

---

# ⚠️ Now the real engineering feedback (important)

This is where you level up.

---

# ⚠️ 1. SSE is NOT reliable (same as FCM issue)

You already raised this earlier — same applies here.

### Problem:

- network drop
    
- app background
    
- connection closed
    
- missed events
    

👉 SSE does NOT guarantee delivery

---

## ✅ Fix (MANDATORY)

You still need:

```http
GET /updates?since=timestamp
```

👉 SAME as I told you before

---

### Combine:

```text
SSE → fast updates
Delta API → correctness
```

---

# ⚠️ 2. You must prevent stale overwrite

Right now your DAO:

```kotlin
UPDATE posts SET like_count = :count
```

👉 This is dangerous.

---

## ✅ Fix:

Add `updatedAt` check:

```kotlin
UPDATE posts 
SET like_count = :count, updated_at = :updatedAt
WHERE id = :postId AND updated_at < :updatedAt
```

👉 Prevents old SSE event overwriting new data

---

# ⚠️ 3. Actor duplication problem

You already noted:

> comment appears twice

---

## Fix options:

### Option A (best)

Backend:

- don’t send SSE to same user
    

### Option B

Room:

```kotlin
@Insert(onConflict = IGNORE)
```

---

# ⚠️ 4. Connection scaling

Your current model:

```text
1 post = N connections
```

👉 Works fine initially

BUT:

If:

- 1 viral post = 10k users watching
    

👉 your server holds 10k emitters

---

## Future solution (not now)

- Redis pub/sub
    
- Kafka
    
- horizontal scaling
    

👉 Don’t over-engineer yet

---

# ⚠️ 5. Feed screen problem (IMPORTANT)

Right now SSE is per post.

👉 But your HOME FEED?

---

## Problem:

User scrolling feed → 50 posts  
You cannot open 50 SSE connections ❌

---

## ✅ Solution

Use SSE ONLY for:

- post detail screen
    
- highly interactive views
    

For feed:

- use delta sync or light polling
    

---

# 🧠 Final architecture (your system — correct version)

```text
User A action
→ backend update
→ SSE broadcast(postId)

User B (on post screen)
→ receives SSE
→ updates Room
→ UI updates instantly

User C (not on screen)
→ no SSE
→ later opens app
→ delta sync
→ gets correct data
```

---

# ⚔️ Final verdict

Your approach is:

## ✅ Strong

- event-driven ✔
    
- scoped ✔
    
- efficient ✔
    
- real-time feel ✔
    

## ❗ But must include:

- delta sync (non-negotiable)
    
- updatedAt conflict protection
    
- fallback logic
    

---

# 🧠 Final truth (very important)

You finally reached the correct model:

👉 Not polling  
👉 Not pure push  
👉 Not WebSocket

👉 **Event system (SSE) + local DB + sync fallback**

---

# 🚀 What I’d tell you as your mentor

You’re no longer thinking like a frontend dev.

You’re now thinking like:  
👉 **backend + mobile system designer**

---

# Next step (if you want to go deeper)

I can help you design:

- Redis-based SSE scaling
    
- multi-device sync guarantees
    
- feed-level optimization strategy
    
- conflict resolution patterns (versioning vs timestamp)
    

Just say.