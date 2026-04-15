 # Room Database — Complete Notes

### Android Jetpack Compose | Structured for Mastery

---

## How to Read These Notes

```
Part 1 → Understand what Room is and why it exists
Part 2 → Implement Room (plain, no DI) — see the raw structure
Part 3 → Add Hilt — production setup
Part 4 → Add Koin — alternative DI
Part 5 → Master every annotation (@Entity, @Dao, @Database)
Part 6 → SQL Query Mastery — dedicated section
Part 7 → Advanced topics (Relationships, Migrations, Flow, Testing)
Part 8 → Interview prep + Cheat Sheet
```

---

---

# PART 1 — Understanding Room

---

## 1.1 What Problem Does Room Solve?

Before Room, every database query in Android required this:

```
1. db.getWritableDatabase()
2. Write SQL string manually:  "SELECT * FROM users WHERE id = ?"
3. Execute → get back a Cursor object
4. cursor.moveToFirst()
5. cursor.getString(cursor.getColumnIndex("name"))   ← extract each column manually
6. Cast every value manually
7. Build your Kotlin object from those values
8. cursor.close()     ← forget this = memory leak
9. db.close()         ← forget this = database locked
10. Do ALL of this on a background thread (or app crashes)
11. Post result back to main thread manually
```

That is **50–100 lines for a single query**. With no safety — a typo in your SQL string only crashes the app when a real user hits that code path in production.

**Room solves all of this:**

|Problem|Room's Solution|
|---|---|
|Typo in SQL → runtime crash|SQL validated at **compile time** — build fails|
|50 lines of boilerplate per query|Room generates all of it|
|Manual thread management|`suspend` functions handle threading|
|No reactive updates|`Flow<T>` auto-emits when data changes|
|Manual schema migration|`AutoMigration` + helpers|

---

## 1.2 What Room Is

Room is Google's official Jetpack library that wraps SQLite. It is **not a replacement** for SQLite — it still uses SQLite underneath. It is an **abstraction layer** that removes the pain.

```
Your Code (Kotlin)
      ↓
   Room
      ↓
  SQLite (.db file on device)
```

**The key principle:**

> You describe _what_ you want. Room writes the _how_.

```kotlin
// You write this:
@Query("SELECT * FROM users WHERE id = :userId")
suspend fun getUserById(userId: Int): UserEntity?

// Room generates all of this automatically at build time:
// → cursor management
// → null checking
// → column extraction
// → object creation
// → thread handling
```

---

## 1.3 SQLite Basics You Need to Know

SQLite stores data in **tables** — exactly like a spreadsheet.

```
TABLE: users
┌────┬──────────┬───────────────────────┬─────┐
│ id │ name     │ email                 │ age │
├────┼──────────┼───────────────────────┼─────┤
│  1 │ Rahul    │ rahul@example.com     │  24 │
│  2 │ Priya    │ priya@example.com     │  22 │
│  3 │ Arjun    │ arjun@example.com     │  28 │
└────┴──────────┴───────────────────────┴─────┘

TABLE: posts
┌────┬─────────┬──────────────────────┐
│ id │ user_id │ title                │
├────┼─────────┼──────────────────────┤
│  1 │    1    │ My first post        │
│  2 │    1    │ Room is great        │
│  3 │    2    │ Kotlin is amazing    │
└────┴─────────┴──────────────────────┘
```

`user_id` in posts links to `id` in users — this is a **foreign key** relationship. Rahul (id=1) wrote posts 1 and 2.

**SQLite only has 5 data types:**

|Kotlin Type|SQLite Type|Notes|
|---|---|---|
|`Int`, `Long`|`INTEGER`|Use `Long` for timestamps|
|`Float`, `Double`|`REAL`|Decimals|
|`String`|`TEXT`|All text|
|`Boolean`|`INTEGER`|`true`=1, `false`=0|
|`ByteArray`|`BLOB`|Raw bytes|
|`Date`, `List`, `Enum`|❌ Needs `@TypeConverter`|Room can't store these directly|

---

## 1.4 The 3 Core Components

Every Room database has **exactly 3 building blocks**. Always.

```
┌──────────────────────────────────────────────────────┐
│                    @Database                          │
│              AppDatabase.kt                           │
│                                                        │
│  "I own everything — I know all tables and DAOs"     │
│  version = 1                                          │
│  entities = [UserEntity::class]                       │
│  abstract fun userDao(): UserDao                      │
└────────────────┬────────────────────┬─────────────────┘
                 │                    │
                 ▼                    ▼
      ┌──────────────────┐   ┌────────────────────────┐
      │    @Entity        │   │        @Dao             │
      │  UserEntity.kt    │   │     UserDao.kt          │
      │                   │   │                         │
      │  "I AM a table"   │   │  "I am the remote       │
      │                   │   │   control for the       │
      │  id: Int          │   │   users table"          │
      │  name: String     │   │                         │
      │  email: String    │   │  getAllUsers()           │
      │                   │   │  insertUser()           │
      └──────────────────┘   └────────────────────────┘
```

**Restaurant analogy:**

|Component|Analogy|
|---|---|
|`@Entity`|Menu item blueprint — defines what a Burger IS|
|`@Dao`|Waiter — takes and delivers orders|
|`@Database`|The restaurant — owns everything|

---

## 1.5 KSP vs KAPT — Always Use KSP

Room needs to **generate code** at build time (it writes the DAO implementation for you). The tool that does this is called an **annotation processor**.

**KAPT (old way):**

```
Your Kotlin code → Convert to Java stubs → Java processor reads stubs → generates .java files
```

Problem: Converting Kotlin → Java stubs adds **30–50% to every build**. Google put KAPT in maintenance mode in 2023. No new features.

**KSP (new way — always use this):**

```
Your Kotlin code → KSP reads Kotlin directly → generates .kt files
```

2x faster. Native Kotlin. Supports Kotlin Multiplatform.

```kotlin
// build.gradle.kts
plugins {
    id("com.google.devtools.ksp") version "2.0.0-1.0.21"
}
dependencies {
    ksp("androidx.room:room-compiler:2.8.4")    // ✅ correct
    // kapt("androidx.room:room-compiler")      // ❌ never use this
}
```

**KSP version rule:** The first part of KSP's version must match your Kotlin version.

```
ksp version:  "2.0.0-1.0.21"
               ↑↑↑↑↑ = must match your project's Kotlin version
```

---

---

# PART 2 — Plain Room Implementation (No DI)

> This section shows you the raw structure of Room with zero DI magic. Master this first. Hilt/Koin just automates the wiring in Part 3 and 4.

---

## 2.1 Gradle Setup

```kotlin
// build.gradle.kts (app module)
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "2.0.0-1.0.21"
    id("androidx.room")
}

android {
    // Saves schema JSON files — needed for migrations, always enable
    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    val room_version = "2.8.4"

    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")   // adds Flow + coroutines
    ksp("androidx.room:room-compiler:$room_version")

    // Testing
    testImplementation("androidx.room:room-testing:$room_version")
}
```

---

## 2.2 Step 1 — Create the Entity

```kotlin
// data/local/entity/UserEntity.kt

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,          // = 0 means "not set yet, let SQLite pick the ID"
    val name: String,
    val email: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

Room reads this class and generates:

```sql
CREATE TABLE IF NOT EXISTS `users` (
    `id`         INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `name`       TEXT NOT NULL,
    `email`      TEXT NOT NULL,
    `created_at` INTEGER NOT NULL
)
```

---

## 2.3 Step 2 — Create the DAO

```kotlin
// data/local/dao/UserDao.kt

@Dao
interface UserDao {

    // Flow = reactive — auto-emits a new list every time the users table changes
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Int): UserEntity?      // null if not found

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long      // returns new row ID

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
```

---

## 2.4 Step 3 — Create the Database

```kotlin
// data/local/AppDatabase.kt

@Database(
    entities = [UserEntity::class],   // list EVERY entity/table here
    version = 1,
    exportSchema = true               // always true — needed for migrations
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao   // one abstract function per DAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Singleton — there must be only ONE instance in the whole app
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"          // name of the .db file on disk
                ).build().also { INSTANCE = it }
            }
        }
    }
}
```

---

## 2.5 Step 4 — Repository

```kotlin
// data/repository/UserRepository.kt

class UserRepository(private val userDao: UserDao) {

    // Expose Flow to ViewModel — maps entity to domain model
    val allUsers: Flow<List<User>> = userDao.getAllUsers()
        .map { list -> list.map { it.toDomain() } }

    suspend fun insert(user: User) = userDao.insertUser(user.toEntity())
    suspend fun delete(user: User) = userDao.deleteUser(user.toEntity())
}
```

---

## 2.6 Step 5 — ViewModel

```kotlin
// presentation/UserViewModel.kt

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    val users: StateFlow<List<User>> = repository.allUsers
        .stateIn(
            scope = UserEntity,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 
        )

    fun addUser(name: String, email: String) {
        viewModelScope.launch {
            repository.insert(User(name = name, email = email))
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch { repository.delete(user) }
    }
}

// Manual ViewModel factory needed without DI
class UserViewModelFactory(private val repository: UserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return UserViewModel(repository) as T
    }
}
```

---

## 2.7 Step 6 — Wire in Activity / Composable

```kotlin
// In your Activity or top-level Composable:

val database = AppDatabase.getDatabase(applicationContext)
val repository = UserRepository(database.userDao())
val viewModel: UserViewModel by viewModels { UserViewModelFactory(repository) }

// In Composable:
@Composable
fun UserListScreen(viewModel: UserViewModel) {
    val users by viewModel.users.collectAsStateWithLifecycle()

    LazyColumn {
        items(users) { user ->
            Text(text = user.name)
        }
    }
}
```

---

## 2.8 Plain Implementation — File Structure

```
app/
├── data/
│   ├── local/
│   │   ├── entity/
│   │   │   └── UserEntity.kt       ← @Entity
│   │   ├── dao/
│   │   │   └── UserDao.kt          ← @Dao
│   │   └── AppDatabase.kt          ← @Database (singleton here)
│   └── repository/
│       └── UserRepository.kt       ← maps entity ↔ domain
├── domain/
│   └── model/
│       └── User.kt                 ← domain model (no Room annotations)
└── presentation/
    └── UserViewModel.kt            ← uses repository, exposes StateFlow
```

---

---

# PART 3 — Room with Hilt

> Hilt replaces the manual singleton and factory boilerplate. The Entity, DAO, and Database classes are **identical** to Part 2. Only the wiring changes.

---

## 3.1 Additional Gradle for Hilt

```kotlin
plugins {
    // ... all plugins from Part 2, plus:
    id("com.google.dagger.hilt.android")
}

dependencies {
    // ... all deps from Part 2, plus:
    val hilt_version = "2.52"
    implementation("com.google.dagger:hilt-android:$hilt_version")
    ksp("com.google.dagger:hilt-compiler:$hilt_version")
}
```

---

## 3.2 Application Class

```kotlin
// MyApplication.kt
@HiltAndroidApp
class MyApplication : Application()
```

```xml
<!-- AndroidManifest.xml -->
<application android:name=".MyApplication" ...>
```

---

## 3.3 Entity, DAO, Database

Same as Part 2. No changes needed.

Remove the `companion object` from `AppDatabase` — Hilt manages the singleton now.

```kotlin
@Database(entities = [UserEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    // No companion object needed — Hilt handles singleton
}
```

---

## 3.4 Hilt Database Module

```kotlin
// di/DatabaseModule.kt

@Module
@InstallIn(SingletonComponent::class)   // lives for the entire app lifetime
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "app_database"
        ).build()

    @Provides
    @Singleton
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
}
```

---

## 3.5 Hilt Repository Module

```kotlin
// di/RepositoryModule.kt

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
```

```kotlin
// data/repository/UserRepositoryImpl.kt

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserRepository {

    override val allUsers: Flow<List<User>> =
        userDao.getAllUsers().map { it.map { e -> e.toDomain() } }

    override suspend fun insert(user: User) = userDao.insertUser(user.toEntity())
    override suspend fun delete(user: User) = userDao.deleteUser(user.toEntity())
}
```

---

## 3.6 Hilt ViewModel

```kotlin
@HiltViewModel
class UserViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    val users: StateFlow<List<User>> = repository.allUsers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun addUser(name: String, email: String) {
        viewModelScope.launch {
            repository.insert(User(name = name, email = email))
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch { repository.delete(user) }
    }
}
```

---

## 3.7 Hilt Composable

```kotlin
@AndroidEntryPoint   // required on Activity that hosts Compose
class MainActivity : ComponentActivity() { ... }

@Composable
fun UserListScreen(viewModel: UserViewModel = hiltViewModel()) {
    val users by viewModel.users.collectAsStateWithLifecycle()

    LazyColumn {
        items(users) { user -> Text(user.name) }
    }
}
```

---

## 3.8 Hilt — File Structure

```
app/
├── di/
│   ├── DatabaseModule.kt        ← @Provides AppDatabase + DAOs
│   └── RepositoryModule.kt      ← @Binds repository interface
├── data/
│   ├── local/
│   │   ├── entity/UserEntity.kt
│   │   ├── dao/UserDao.kt
│   │   └── AppDatabase.kt       ← no companion object
│   └── repository/
│       ├── UserRepository.kt    ← interface
│       └── UserRepositoryImpl.kt ← @Inject constructor
├── domain/model/User.kt
└── presentation/
    └── UserViewModel.kt         ← @HiltViewModel + @Inject
```

---

---

# PART 4 — Room with Koin

> Koin replaces Hilt's annotations with a simple Kotlin DSL. Entity, DAO, Database — still identical to Part 2.

---

## 4.1 Additional Gradle for Koin

```kotlin
plugins {
    // ... all plugins from Part 2
    // NO hilt plugin needed
}

dependencies {
    // ... all deps from Part 2, plus:
    val koin_version = "3.5.6"
    implementation("io.insert-koin:koin-android:$koin_version")
    implementation("io.insert-koin:koin-androidx-compose:$koin_version")
}
```

---

## 4.2 Application Class

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApplication)
            androidLogger(Level.DEBUG)    // use Level.NONE in release build
            modules(databaseModule, repositoryModule, viewModelModule)
        }
    }
}
```

---

## 4.3 Entity, DAO, Database

Identical to Part 2. No `companion object` needed — Koin handles the singleton.

---

## 4.4 Koin Modules

```kotlin
// di/DatabaseModule.kt

val databaseModule = module {

    // single { } = singleton — created once, lives for app lifetime
    single<AppDatabase> {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "app_database"
        ).build()
    }

    // get<AppDatabase>() = "Koin, give me the AppDatabase you already created"
    single<UserDao> { get<AppDatabase>().userDao() }
}
```

```kotlin
// di/RepositoryModule.kt

val repositoryModule = module {
    single<UserRepository> {
        UserRepositoryImpl(userDao = get())  // get() = Koin provides UserDao
    }
}
```

```kotlin
// di/ViewModelModule.kt

val viewModelModule = module {
    // viewModel { } = new instance per ViewModel scope
    viewModel { UserViewModel(repository = get()) }
}
```

---

## 4.5 Koin Repository

```kotlin
// No @Inject — just a normal constructor
class UserRepositoryImpl(
    private val userDao: UserDao
) : UserRepository {

    override val allUsers: Flow<List<User>> =
        userDao.getAllUsers().map { it.map { e -> e.toDomain() } }

    override suspend fun insert(user: User) = userDao.insertUser(user.toEntity())
    override suspend fun delete(user: User) = userDao.deleteUser(user.toEntity())
}
```

---

## 4.6 Koin ViewModel

```kotlin
// No @HiltViewModel, No @Inject
class UserViewModel(
    private val repository: UserRepository
) : ViewModel() {

    val users: StateFlow<List<User>> = repository.allUsers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun addUser(name: String, email: String) {
        viewModelScope.launch {
            repository.insert(User(name = name, email = email))
        }
    }
}
```

---

## 4.7 Koin Composable

```kotlin
@Composable
fun UserListScreen() {
    val viewModel: UserViewModel = koinViewModel()   // koin, not hilt
    val users by viewModel.users.collectAsStateWithLifecycle()

    LazyColumn {
        items(users) { user -> Text(user.name) }
    }
}
```

---

## 4.8 Koin Keywords Reference

|Keyword|What it creates|Lifecycle|
|---|---|---|
|`single { }`|Singleton — one instance forever|App lifetime|
|`factory { }`|New instance every time requested|Until dereferenced|
|`viewModel { }`|ViewModel-scoped|ViewModel scope|
|`get()`|Ask Koin to provide a dependency|—|
|`androidContext()`|Provides Android Context|—|

---

## 4.9 Hilt vs Koin — Full Comparison

||Hilt|Koin|
|---|---|---|
|DI errors caught at|**Build time** — compile-safe|Runtime — app crash|
|Boilerplate|More (`@Module`, `@InstallIn`, `@Binds`)|Less — simple DSL|
|Build speed impact|Adds KSP processing time|Zero|
|Google official recommendation|✅ Yes|No|
|Kotlin Multiplatform support|❌ No|✅ Yes|
|Learning curve|Steeper|Gentler|
|Best for|Company / production apps|Personal / KMP / learning|

---

---

# PART 5 — Annotations Deep Dive

---

## 5.1 @Entity Annotations

### Basic @Entity

```kotlin
@Entity(tableName = "users")
data class UserEntity(...)
```

**Why always set `tableName` explicitly?** Without it, Room uses the class name (`userentity`). If you rename the class, the table renames too — which requires a migration. With explicit `tableName`, class renames have zero database impact.

---

### @PrimaryKey

```kotlin
// Auto-increment — most common, Room assigns IDs
@PrimaryKey(autoGenerate = true)
val id: Int = 0          // = 0 is the "I don't have an ID yet" signal

// Manual integer — you assign the ID
@PrimaryKey
val id: Int

// UUID string — good when syncing with a backend API
@PrimaryKey
val id: String = UUID.randomUUID().toString()

// Composite primary key — set in @Entity, not on fields
@Entity(primaryKeys = ["userId", "courseId"])
data class EnrollmentEntity(
    val userId: Int,
    val courseId: Int
)
```

**Why `= 0` for autoGenerate?** Room ignores the id when inserting if it is 0, letting SQLite assign the next ID. Any non-zero value is treated as a real ID and may cause a conflict.

---

### @ColumnInfo

```kotlin
@ColumnInfo(
    name = "first_name",         // SQL column name — can differ from Kotlin field
    defaultValue = "Unknown",    // used in INSERT if column is not provided
    index = true,                // adds an index on this column
    collate = ColumnInfo.NOCASE  // case-insensitive text comparison
)
val firstName: String
```

**Rename Kotlin field** → no migration needed (code only) **Rename `name` value** → migration required (changes database column name)

**Collation options:**

```
NOCASE   → 'Rahul' == 'rahul' == 'RAHUL'  (use for name searches)
BINARY   → case-sensitive (default)
RTRIM    → ignores trailing spaces
```

---

### @Ignore

```kotlin
@Entity
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    @Ignore val displayLabel: String = ""   // computed at runtime, NOT stored in DB
)
```

---

### @Embedded

Flattens a nested object's fields directly into the parent table. No separate table created.

```kotlin
data class Address(
    val street: String,
    val city: String,
    val pincode: String
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    @Embedded val address: Address    // adds street, city, pincode as columns in users table
)

// With prefix — prevents column name conflicts when embedding twice:
@Embedded(prefix = "home_") val homeAddress: Address   // home_street, home_city...
@Embedded(prefix = "work_") val workAddress: Address   // work_street, work_city...
```

**When to embed vs separate table:**

|Embed when|Separate table when|
|---|---|
|Object only belongs to this one entity|Object shared across multiple entities|
|You never query it independently|You need to query it on its own|
|It IS part of the entity (address)|It IS its own thing (post, comment)|

---

### @Entity Indices

An index is a hidden sorted lookup table. Without it, SQLite reads every row.

```kotlin
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),  // enforces unique emails
        Index(value = ["name"]),                   // speeds up search by name
        Index(value = ["city", "age"])             // composite: fast when filtering by BOTH
    ]
)
```

**Index these always:**

- Columns used in `WHERE` clauses frequently
- Foreign key columns — **always**
- Columns in `ORDER BY`

---

### @ForeignKey

```kotlin
@Entity(
    tableName = "posts",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],          // column in UserEntity
            childColumns = ["user_id"],      // column in THIS table
            onDelete = ForeignKey.CASCADE,   // delete user → auto-delete their posts
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("user_id")]             // always index FK columns!
)
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val title: String,
    val body: String
)
```

**onDelete options:**

|Option|What happens when parent is deleted|
|---|---|
|`CASCADE`|Child rows auto-deleted — most common|
|`SET_NULL`|FK column set to NULL (must be nullable)|
|`RESTRICT`|Delete blocked if children exist|
|`NO_ACTION`|Nothing — orphan rows stay (usually wrong)|

---

## 5.2 @Dao Annotations

### @Insert

```kotlin
@Insert
suspend fun insertUser(user: UserEntity): Long              // returns new row ID

@Insert
suspend fun insertUsers(users: List<UserEntity>): List<Long>

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertOrReplace(user: UserEntity)

@Insert(onConflict = OnConflictStrategy.IGNORE)
suspend fun insertIfNotExists(user: UserEntity): Long       // returns -1 if ignored
```

**OnConflict strategies:**

|Strategy|What happens on duplicate primary key|
|---|---|
|`REPLACE`|Deletes old row, inserts new — ⚠️ may trigger CASCADE deletes|
|`IGNORE`|Silently drops the insert, returns -1|
|`ABORT`|Throws `SQLiteConstraintException` (default)|

---

### @Update

```kotlin
@Update
suspend fun updateUser(user: UserEntity): Int   // returns count of rows updated
```

Room generates: `UPDATE users SET name=?, email=? WHERE id=?`

Matches by **primary key only**. You must pass the complete entity. The entire row is updated — not just changed fields.

**Update only one column:**

```kotlin
@Query("UPDATE users SET name = :name WHERE id = :id")
suspend fun updateName(id: Int, name: String)
```

---

### @Delete

```kotlin
@Delete
suspend fun deleteUser(user: UserEntity): Int    // uses only the primary key

// More practical — delete by ID or condition:
@Query("DELETE FROM users WHERE id = :id")
suspend fun deleteById(id: Int)

@Query("DELETE FROM users")
suspend fun deleteAll()
```

---

### @Upsert (Room 2.5+)

```kotlin
@Upsert
suspend fun upsertUser(user: UserEntity): Long

@Upsert
suspend fun upsertUsers(users: List<UserEntity>): List<Long>
```

**Why @Upsert is better than @Insert(REPLACE):**

```
@Insert(REPLACE):
  1. DELETE the existing row              ← triggers ON DELETE CASCADE!
  2. INSERT new row with new ROWID

@Upsert:
  1. Try INSERT
  2. If conflict → UPDATE the existing row   ← no delete, FK children are safe
```

Use `@Upsert` whenever the entity has child rows pointing to it.

---

### @Query

```kotlin
// Named parameter with : prefix
@Query("SELECT * FROM users WHERE id = :userId")
suspend fun getUserById(userId: Int): UserEntity?

// Multiple parameters
@Query("SELECT * FROM users WHERE age >= :minAge AND city = :city")
suspend fun getByAgeAndCity(minAge: Int, city: String): List<UserEntity>

// IN clause — Room automatically expands a List into (?, ?, ?)
@Query("SELECT * FROM users WHERE id IN (:ids)")
suspend fun getUsersByIds(ids: List<Int>): List<UserEntity>

// Partial result — return only some columns using a plain data class
@Query("SELECT id, name FROM users")
suspend fun getUserNames(): List<UserNameOnly>
// data class UserNameOnly(val id: Int, val name: String)

// Reactive — auto-emits on table change
@Query("SELECT * FROM users ORDER BY name ASC")
fun getAllUsers(): Flow<List<UserEntity>>
```

---

### @Transaction

```kotlin
// Use case 1: @Relation queries — ALWAYS required
@Transaction
@Query("SELECT * FROM users")
fun getUsersWithPosts(): Flow<List<UserWithPosts>>
// Without @Transaction: two separate reads — data can change between them

// Use case 2: Multiple operations that must all succeed together
@Transaction
suspend fun transferUser(userId: Int, newCity: String) {
    removeFromOldCity(userId)   // if this succeeds but next fails →
    addToNewCity(userId, newCity)   // @Transaction rolls BOTH back
}

// Use case 3: Atomic read-then-write
@Transaction
suspend fun incrementCount(userId: Int) {
    val user = getUserById(userId)
    user?.let { updateCount(it.id, it.count + 1) }
}
```

---

### @Transaction — Interface vs Abstract Class

```kotlin
// Interface — use for most cases
@Dao
interface UserDao { ... }

// Abstract class — use when you need helper logic
@Dao
abstract class UserDao {
    @Insert abstract suspend fun insert(user: UserEntity)

    // Can have non-abstract helper functions
    suspend fun insertIfNameNotBlank(user: UserEntity) {
        if (user.name.isNotBlank()) insert(user)
    }
}
```

---

## 5.3 @Database Annotation

```kotlin
@Database(
    entities = [
        UserEntity::class,
        PostEntity::class      // list EVERY entity that should have a table
    ],
    version = 1,               // increment this every time schema changes
    exportSchema = true        // always true — saves schema JSON for migrations
)
@TypeConverters(Converters::class)   // register type converters at DB level
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao    // one per DAO
    abstract fun postDao(): PostDao
}
```

**Room.databaseBuilder options:**

```kotlin
Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)   // register migrations
    .fallbackToDestructiveMigration()              // DEV ONLY — wipes DB if no migration
    .createFromAsset("seed.db")                    // pre-populate from assets/
    .allowMainThreadQueries()                      // TEST ONLY — never in production
    .build()
```

**exportSchema and schema JSON files:**

When `exportSchema = true`, Room saves a JSON file after every build:

```
app/schemas/com.yourapp.AppDatabase/
├── 1.json    ← exact schema at version 1
├── 2.json    ← exact schema at version 2
└── 3.json    ← current
```

Always commit these to git. Auto-migration uses them to know what changed. Code review will catch unintended schema changes.

---

## 5.4 @TypeConverter

SQLite only has 5 types. Anything else needs a converter.

```kotlin
class Converters {

    // ── Date ──────────────────────────────────────────
    @TypeConverter fun longToDate(v: Long?): Date? = v?.let { Date(it) }
    @TypeConverter fun dateToLong(d: Date?): Long? = d?.time

    // ── Enum ──────────────────────────────────────────
    @TypeConverter fun statusToString(s: UserStatus?): String? = s?.name
    @TypeConverter fun stringToStatus(v: String?): UserStatus? =
        v?.let { runCatching { UserStatus.valueOf(it) }.getOrNull() }
    // runCatching handles removed enum values gracefully

    // ── List<String> ──────────────────────────────────
    @TypeConverter fun listToString(l: List<String>?): String? = l?.joinToString("|||")
    @TypeConverter fun stringToList(v: String?): List<String>? =
        v?.split("|||")?.filter { it.isNotEmpty() }

    // ── List<Int> via Gson ────────────────────────────
    @TypeConverter fun intListToJson(l: List<Int>?): String? =
        l?.let { Gson().toJson(it) }
    @TypeConverter fun jsonToIntList(json: String?): List<Int>? =
        json?.let { Gson().fromJson(it, object : TypeToken<List<Int>>() {}.type) }
}
```

**Register at database level — applies to all entities:**

```kotlin
@Database(entities = [...], version = 1)
@TypeConverters(Converters::class)     // ← here
abstract class AppDatabase : RoomDatabase() { ... }
```

---

---

# PART 6 — SQL Query Mastery

> SQL is a separate skill from Room. Master these queries and you can write any `@Query` yourself without guessing.

---

## 6.1 SELECT Basics

```sql
-- All rows, all columns
SELECT * FROM users

-- Specific columns only (faster, uses less memory)
SELECT id, name, email FROM users

-- No duplicate values
SELECT DISTINCT city FROM users

-- Calculated column
SELECT name, age, (age * 365) AS age_in_days FROM users

-- Table alias — useful in JOINs
SELECT u.id, u.name FROM users AS u
```

---

## 6.2 WHERE Filtering

```sql
-- Equality
SELECT * FROM users WHERE id = 5

-- Comparison
SELECT * FROM users WHERE age > 18
SELECT * FROM users WHERE age >= 18
SELECT * FROM users WHERE age BETWEEN 18 AND 30   -- 18 and 30 inclusive

-- NULL checks
SELECT * FROM users WHERE nickname IS NULL
SELECT * FROM users WHERE nickname IS NOT NULL

-- AND / OR
SELECT * FROM users WHERE age > 18 AND city = 'Mumbai'
SELECT * FROM users WHERE city = 'Delhi' OR city = 'Mumbai'

-- IN list
SELECT * FROM users WHERE id IN (1, 2, 3)
SELECT * FROM users WHERE city NOT IN ('Delhi', 'Mumbai')

-- LIKE — pattern matching
-- % = any number of characters
-- _ = exactly one character
SELECT * FROM users WHERE name LIKE 'Ra%'       -- starts with Ra
SELECT * FROM users WHERE name LIKE '%ul'       -- ends with ul
SELECT * FROM users WHERE name LIKE '%ahu%'     -- contains ahu
SELECT * FROM users WHERE name LIKE 'R_hul'     -- R, any one char, hul
```

In Room:

```kotlin
@Query("SELECT * FROM users WHERE name LIKE :pattern")
suspend fun searchUsers(pattern: String): List<UserEntity>

// Usage — always wrap with % for contains search:
userDao.searchUsers("%${searchTerm}%")
```

---

## 6.3 ORDER BY + LIMIT + OFFSET

```sql
-- Single column
SELECT * FROM users ORDER BY name ASC             -- A to Z
SELECT * FROM users ORDER BY created_at DESC      -- newest first

-- Multiple columns
SELECT * FROM users ORDER BY city ASC, name ASC   -- city first, then name within city

-- First N rows
SELECT * FROM users LIMIT 20

-- Pagination
SELECT * FROM users LIMIT 20 OFFSET 0    -- page 1 (rows 1–20)
SELECT * FROM users LIMIT 20 OFFSET 20   -- page 2 (rows 21–40)
SELECT * FROM users LIMIT 20 OFFSET 40   -- page 3 (rows 41–60)
```

**Pagination formula:** `OFFSET = (pageNumber - 1) * pageSize`

In Room:

```kotlin
@Query("SELECT * FROM users ORDER BY name ASC LIMIT :limit OFFSET :offset")
suspend fun getUsersPaged(limit: Int, offset: Int): List<UserEntity>
```

---

## 6.4 Aggregate Functions

These compute **one result from many rows**.

```sql
SELECT COUNT(*) FROM users                      -- total row count
SELECT COUNT(nickname) FROM users               -- count non-NULL values only
SELECT COUNT(DISTINCT city) FROM users          -- count unique cities

SELECT AVG(age) FROM users                      -- average
SELECT MAX(age) FROM users                      -- highest value
SELECT MIN(age) FROM users                      -- lowest value
SELECT SUM(balance) FROM accounts               -- total

-- Multiple aggregates in one query
SELECT
    COUNT(*) AS total,
    AVG(age) AS avg_age,
    MAX(age) AS max_age,
    MIN(age) AS min_age
FROM users
```

In Room:

```kotlin
data class UserStats(val total: Int, val avgAge: Double, val maxAge: Int, val minAge: Int)

@Query("SELECT COUNT(*) as total, AVG(age) as avgAge, MAX(age) as maxAge, MIN(age) as minAge FROM users")
suspend fun getUserStats(): UserStats
```

---

## 6.5 GROUP BY and HAVING

GROUP BY collapses rows into groups so you can aggregate per group.

```sql
-- Count users per city
SELECT city, COUNT(*) AS user_count
FROM users
GROUP BY city

-- Average age per department
SELECT department, AVG(age) AS avg_age, COUNT(*) AS total
FROM employees
GROUP BY department
ORDER BY avg_age DESC
```

**WHERE vs HAVING — critical difference:**

```
WHERE   → filters individual ROWS   (runs BEFORE grouping)
HAVING  → filters GROUPS            (runs AFTER grouping)
```

```sql
-- Step by step:
SELECT city, COUNT(*) AS user_count
FROM users
WHERE age > 18              -- Step 1: filter rows — keep only adults
GROUP BY city               -- Step 2: group remaining rows by city
HAVING user_count >= 10     -- Step 3: keep only groups (cities) with 10+ adults
ORDER BY user_count DESC    -- Step 4: sort
```

In Room:

```kotlin
data class CityCount(val city: String, val userCount: Int)

@Query("""
    SELECT city, COUNT(*) as userCount
    FROM users
    WHERE age > 18
    GROUP BY city
    HAVING userCount >= :minUsers
    ORDER BY userCount DESC
""")
suspend fun getActiveCities(minUsers: Int): List<CityCount>
```

---

## 6.6 JOINs

A JOIN combines rows from two tables based on a matching condition.

```
TABLE: users              TABLE: posts
id | name                 id | user_id | title
 1 | Rahul                 1 |    1    | Post A
 2 | Priya                 2 |    1    | Post B
 3 | Arjun                 3 |    2    | Post C
                           (Arjun has no posts)
```

### INNER JOIN — only rows with a match in BOTH tables

```sql
SELECT users.name, posts.title
FROM posts
INNER JOIN users ON posts.user_id = users.id
```

Result: Rahul+PostA, Rahul+PostB, Priya+PostC **Arjun is excluded** — he has no matching post.

### LEFT JOIN — all rows from left table, match or NULL from right

```sql
SELECT users.name, posts.title
FROM users
LEFT JOIN posts ON posts.user_id = users.id
```

Result: Rahul+PostA, Rahul+PostB, Priya+PostC, **Arjun+NULL** Arjun IS included, post.title is NULL.

### Multiple JOINs

```sql
-- users → posts → comments
SELECT users.name, posts.title, comments.body
FROM comments
INNER JOIN posts ON comments.post_id = posts.id
INNER JOIN users ON posts.user_id = users.id
WHERE users.id = :userId
ORDER BY comments.created_at DESC
```

In Room:

```kotlin
@Query("""
    SELECT users.name, posts.title
    FROM users
    LEFT JOIN posts ON posts.user_id = users.id
    WHERE users.id = :userId
""")
suspend fun getUserPosts(userId: Int): List<UserPostRow>
// data class UserPostRow(val name: String, val title: String?)
```

---

## 6.7 UPDATE and DELETE Queries

```sql
-- Update one column
UPDATE users SET name = 'NewName' WHERE id = 5

-- Update multiple columns
UPDATE users SET name = 'NewName', email = 'new@email.com' WHERE id = 5

-- Delete one row
DELETE FROM users WHERE id = 5

-- Delete by condition
DELETE FROM users WHERE created_at < 1700000000

-- Delete all
DELETE FROM users
```

In Room:

```kotlin
@Query("UPDATE users SET name = :name WHERE id = :id")
suspend fun updateName(id: Int, name: String)

@Query("DELETE FROM users WHERE id = :id")
suspend fun deleteById(id: Int)

@Query("DELETE FROM users WHERE created_at < :cutoff")
suspend fun deleteOldUsers(cutoff: Long)
```

---

## 6.8 Subqueries

A query nested inside another query.

```sql
-- Users who have at least one post
SELECT * FROM users
WHERE id IN (SELECT DISTINCT user_id FROM posts)

-- Users who have never posted
SELECT * FROM users
WHERE id NOT IN (SELECT user_id FROM posts WHERE user_id IS NOT NULL)

-- Users older than the average age
SELECT * FROM users
WHERE age > (SELECT AVG(age) FROM users)

-- Per-row count — for each user, count their posts
SELECT u.*, (SELECT COUNT(*) FROM posts WHERE posts.user_id = u.id) AS post_count
FROM users u
```

---

## 6.9 Complex Multi-line Query Example

```kotlin
@Query("""
    SELECT u.id AS userId,
           u.name AS userName,
           COUNT(p.id) AS postCount,
           MAX(p.created_at) AS lastPostAt
    FROM users u
    LEFT JOIN posts p ON p.user_id = u.id
    WHERE u.is_active = 1
    GROUP BY u.id
    ORDER BY postCount DESC
    LIMIT :limit
""")
suspend fun getActiveUsersWithStats(limit: Int): List<UserPostStats>

// data class UserPostStats(
//     val userId: Int,
//     val userName: String,
//     val postCount: Int,
//     val lastPostAt: Long?
// )
```

---

## 6.10 SQL Execution Order

SQL is processed in this order — **not** the order you write it:

```
1. FROM       → which table(s)
2. JOIN       → combine tables
3. WHERE      → filter individual rows
4. GROUP BY   → group rows
5. HAVING     → filter groups
6. SELECT     → pick columns
7. ORDER BY   → sort
8. LIMIT      → restrict count
```

This is why:

- `HAVING` must come after `GROUP BY`
- You can't use a `SELECT` alias in a `WHERE` clause (SELECT runs after WHERE)
- `WHERE` filters rows before grouping, `HAVING` filters after

---

---

# PART 7 — Advanced Topics

---

## 7.1 Relationships

Room is **not an ORM** — it does not auto-load related entities. You define relationships explicitly. Room batches child queries (never N+1).

### One-to-One

```kotlin
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val userId: Int,   // matches UserEntity.id
    val bio: String,
    val avatarUrl: String
)

// Result class — NOT an @Entity
data class UserWithProfile(
    @Embedded val user: UserEntity,
    @Relation(parentColumn = "id", entityColumn = "userId")
    val profile: ProfileEntity?    // nullable — profile may not exist yet
)

@Dao
interface UserDao {
    @Transaction
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserWithProfile(id: Int): UserWithProfile?

    @Transaction
    @Query("SELECT * FROM users")
    fun getAllUsersWithProfiles(): Flow<List<UserWithProfile>>
}
```

---

### One-to-Many

```kotlin
data class UserWithPosts(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "user_id"    // must match @ColumnInfo name, not Kotlin field name
    )
    val posts: List<PostEntity>
)

@Dao
interface UserDao {
    @Transaction                    // @Transaction is REQUIRED for all @Relation queries
    @Query("SELECT * FROM users")
    fun getAllUsersWithPosts(): Flow<List<UserWithPosts>>
}
```

---

### Many-to-Many

```kotlin
// Junction table links the two entities
@Entity(
    tableName = "book_author_cross_ref",
    primaryKeys = ["book_id", "author_id"]   // composite key prevents duplicate links
)
data class BookAuthorCrossRef(
    @ColumnInfo(name = "book_id") val bookId: Int,
    @ColumnInfo(name = "author_id") val authorId: Int
)

data class BookWithAuthors(
    @Embedded val book: BookEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = BookAuthorCrossRef::class,
            parentColumn = "book_id",
            entityColumn = "author_id"
        )
    )
    val authors: List<AuthorEntity>
)

@Dao
interface BookDao {
    @Transaction
    @Query("SELECT * FROM books")
    fun getAllBooksWithAuthors(): Flow<List<BookWithAuthors>>
}
```

---

### Why @Transaction is Required with @Relation

Room runs at minimum **two separate queries**:

1. `SELECT * FROM users`
2. `SELECT * FROM posts WHERE userId IN (1, 2, 3...)`

Without `@Transaction`: data can change between query 1 and query 2 → inconsistent result. With `@Transaction`: both queries are atomic — same data snapshot guaranteed.

---

## 7.2 Flow + Coroutines

### How Room's Flow Works

```
1. You call userDao.getAllUsers() → returns Flow<List<UserEntity>>
2. Coroutine starts collecting the Flow
3. Room executes the SQL query, emits the result list
4. Room registers an InvalidationTracker observer on the "users" table
5. Any write (INSERT / UPDATE / DELETE) on "users" →
      Room re-runs the query → Flow emits new result automatically
6. Flow never completes — runs until the coroutine is cancelled
```

### DAO Return Type Guide

```kotlin
// One-shot reads
suspend fun getUserById(id: Int): UserEntity?         // null if not found
suspend fun getAllUsers(): List<UserEntity>            // empty list if none
suspend fun getCount(): Int

// Reactive (auto-updates on table change)
fun getAllUsers(): Flow<List<UserEntity>>
fun getUserById(id: Int): Flow<UserEntity?>           // emits null if deleted

// Write operations
suspend fun insertUser(user: UserEntity): Long         // returns new row ID
suspend fun updateUser(user: UserEntity): Int          // rows updated
suspend fun deleteUser(user: UserEntity): Int          // rows deleted
```

### ViewModel + StateFlow Pattern

```kotlin
val users: StateFlow<List<User>> = repository.allUsers
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        // 5000ms: keeps Flow active 5s after last subscriber leaves
        // handles screen rotation without restarting the query
        initialValue = emptyList()
    )
```

### Collecting in Compose

```kotlin
// collectAsStateWithLifecycle — stops collecting when app goes to background
// better than collectAsState — saves battery
val users by viewModel.users.collectAsStateWithLifecycle()
```

---

## 7.3 Database Migrations

### Why Migrations Exist

The database is a **file on the user's device**. When they install an update:

- Old file: `version = 1` — has columns `id, name, email`
- New code: `version = 2` — expects `id, name, email, age`

Room sees version mismatch. Without a migration:

- No `fallbackToDestructiveMigration()` → **app crashes on launch**
- With it → **user loses all their data**

**Rule: Every @Entity change = increment version + provide migration.**

---

### Manual Migration — All Cases

```kotlin
// ADD A COLUMN
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // NOT NULL column needs a default — can't add NOT NULL without one
        database.execSQL("ALTER TABLE users ADD COLUMN age INTEGER NOT NULL DEFAULT 0")
        // Nullable column — no default needed
        database.execSQL("ALTER TABLE users ADD COLUMN nickname TEXT")
    }
}

// ADD A NEW TABLE
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS posts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                user_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
        """)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_posts_user_id ON posts(user_id)")
    }
}

// RENAME A TABLE
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE user RENAME TO users")
    }
}

// RENAME A COLUMN — SQLite cannot rename columns directly, must recreate table
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE users_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                full_name TEXT NOT NULL,
                email TEXT NOT NULL
            )
        """)
        database.execSQL("INSERT INTO users_new (id, full_name, email) SELECT id, name, email FROM users")
        database.execSQL("DROP TABLE users")
        database.execSQL("ALTER TABLE users_new RENAME TO users")
    }
}

// Register:
Room.databaseBuilder(context, AppDatabase::class.java, "app_db")
    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
    .build()
```

---

### Auto-Migration (Room 2.4+)

Room compares old schema JSON with new schema JSON and generates SQL automatically.

```kotlin
@Database(
    entities = [UserEntity::class],
    version = 3,
    exportSchema = true,          // REQUIRED — Room needs the JSON files
    autoMigrations = [
        AutoMigration(from = 1, to = 2),     // added a column → auto
        AutoMigration(from = 2, to = 3, spec = AppDatabase.Migration2To3::class)
    ]
)
abstract class AppDatabase : RoomDatabase() {

    @RenameTable(fromTableName = "user", toTableName = "users")
    class Migration2To3 : AutoMigrationSpec

    // Other spec annotations:
    // @DeleteTable(tableName = "old_table")
    // @RenameColumn(tableName = "users", fromColumnName = "name", toColumnName = "full_name")
    // @DeleteColumn(tableName = "users", columnName = "old_field")

    abstract fun userDao(): UserDao
}
```

|Works automatically|Needs manual Migration|
|---|---|
|Adding columns (with default value)|Changing a column type|
|Adding / deleting tables|Complex data transformation|
|Renaming tables / columns (with spec)|Custom logic|

---

## 7.4 Performance Tips

**1. Use Flow for data that changes — never poll manually**

```kotlin
// ❌ Bad — manual refresh needed after every change
suspend fun getAllUsersOnce(): List<UserEntity>

// ✅ Good — auto-updates
fun getAllUsers(): Flow<List<UserEntity>>
```

**2. Always index foreign key columns**

```kotlin
// Without index: every JOIN does a full table scan
indices = [Index("user_id")]    // always add this for FK columns
```

**3. Select only the columns you need**

```kotlin
// ❌ Fetches all columns even if only showing name
@Query("SELECT * FROM users")

// ✅ Faster, less memory
@Query("SELECT id, name FROM users")
suspend fun getUserNames(): List<UserNameOnly>
```

**4. Batch inserts in a single transaction**

```kotlin
// ❌ 100 inserts = 100 transactions = slow
users.forEach { userDao.insertUser(it) }

// ✅ 100 inserts = 1 transaction = fast
@Insert
suspend fun insertUsers(users: List<UserEntity>)
```

**5. Use @Relation instead of N+1 queries**

```kotlin
// ❌ N queries for N users
users.map { user ->
    val posts = postDao.getPostsForUser(user.id)   // called once per user!
    UserWithPosts(user, posts)
}

// ✅ Room batches this into 2 queries total
@Transaction
@Query("SELECT * FROM users")
fun getUsersWithPosts(): Flow<List<UserWithPosts>>
```

---

## 7.5 Testing Room

### In-Memory Database for Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()    // OK for tests only
            .build()
        userDao = database.userDao()
    }

    @After
    fun teardown() = database.close()

    @Test
    fun insertAndRetrieve() = runTest {
        val user = UserEntity(name = "Rahul", email = "r@r.com")
        val id = userDao.insertUser(user)

        val result = userDao.getUserById(id.toInt())
        assertThat(result?.name).isEqualTo("Rahul")
    }

    @Test
    fun deleteUser_removesFromDb() = runTest {
        val id = userDao.insertUser(UserEntity(name = "Rahul", email = "r@r.com"))
        val inserted = userDao.getUserById(id.toInt())!!
        userDao.deleteUser(inserted)

        assertThat(userDao.getUserById(id.toInt())).isNull()
    }
}
```

```kotlin
// Test dependencies
testImplementation("androidx.room:room-testing:$room_version")
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("com.google.truth:truth:1.1.5")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
```

---

---

# PART 8 — Interview Prep + Cheat Sheet

---

## 8.1 Interview Questions

**Q: What is Room and why use it over raw SQLite?**

Room is a Jetpack abstraction over SQLite with three key benefits: (1) SQL validated at compile time — a typo fails the build, not the app; (2) zero boilerplate — Room generates all cursor management; (3) native Kotlin coroutines/Flow — queries are suspend functions and reactive streams. Raw SQLite is 50+ lines per query with no compile-time safety.

---

**Q: Explain the 3 Room components.**

`@Entity` is a data class representing a table — each field is a column. `@Dao` is an interface with `@Query`, `@Insert`, `@Update`, `@Delete` — Room generates the implementation. `@Database` is an abstract class that registers all entities, the version number, and exposes DAO instances. Room generates the concrete implementation.

---

**Q: Why use KSP over KAPT?**

KAPT converts Kotlin to Java stubs so Java annotation processors can read them — this adds 30–50% to build time and loses Kotlin-specific type info. KSP reads Kotlin AST directly — 2x faster, generates `.kt` files, supports Kotlin Multiplatform. Google put KAPT in maintenance mode in 2023.

---

**Q: Why does @Relation require @Transaction?**

Room runs at least two queries for a relationship — one for parents, one (batched) for children. Without `@Transaction`, data can change between them, giving inconsistent results. `@Transaction` makes both reads atomic.

---

**Q: Difference between `suspend fun T` and `fun Flow<T>` in a DAO?**

`suspend fun T` runs once and returns. `fun Flow<T>` runs the query, emits the result, then watches the table. Every INSERT/UPDATE/DELETE re-runs the query and re-emits. Use `suspend` for writes and one-time reads. Use `Flow` for anything that should stay live in the UI.

---

**Q: What happens if you change an entity without a migration?**

Room compares the version in the DB file to the version in `@Database`. Mismatch → `IllegalStateException` — app crashes on launch for users who are updating. If `fallbackToDestructiveMigration()` is set, DB is wiped instead — user loses all data.

---

**Q: Difference between @Upsert and @Insert(onConflict = REPLACE)?**

`REPLACE` deletes the old row then inserts a new one — can trigger `ON DELETE CASCADE` on FK children, accidentally deleting child rows. `@Upsert` tries INSERT first; on conflict, UPDATEs the existing row without deleting — FK children are safe.

---

**Q: What does exportSchema do and why does it matter?**

It saves a JSON snapshot of the schema after every build. Auto-migration uses these files to know what changed between versions. Committing them to git lets you see the exact schema at any version and catch unintended changes in code review.

---

## 8.2 Quick Cheat Sheet

### Gradle

```
plugin:    com.google.devtools.ksp
           androidx.room
android:   room { schemaDirectory("$projectDir/schemas") }
deps:      room-runtime + room-ktx → implementation
           room-compiler           → ksp  (never kapt)
```

### @Entity

```
@Entity(tableName, indices, foreignKeys)
@PrimaryKey(autoGenerate = true)       → id: Int = 0
@ColumnInfo(name, defaultValue, index, collate)
@Ignore                                → skip this field
@Embedded(prefix)                      → flatten nested object into same table
@TypeConverters                        → Date, List, Enum, etc.
@ForeignKey(onDelete = CASCADE)        → referential integrity
```

### @Dao

```
@Insert(onConflict = REPLACE/IGNORE/ABORT) → Long / List<Long>
@Update                                    → Int (rows affected)
@Delete                                    → Int (rows deleted)
@Upsert                                    → insert or UPDATE (safe, no FK risk)
@Query("SQL")                             → compile-time validated
@Transaction                              → required for @Relation
```

### Return Types

```
suspend fun T?           → one-shot, nullable
suspend fun List<T>      → one-shot list
suspend fun Int/Long     → rows affected / new row ID
fun Flow<T?>             → live, auto-updates
fun Flow<List<T>>        → live list, re-emits on any change
```

### SQL Quick Reference

```sql
SELECT id, name FROM t WHERE col = :param
SELECT * FROM t WHERE col LIKE :pattern          -- "%term%"
SELECT * FROM t WHERE col IN (:list)
SELECT * FROM t ORDER BY col DESC LIMIT :n OFFSET :offset
SELECT COUNT(*), AVG(col), MAX(col), SUM(col) FROM t
SELECT col, COUNT(*) FROM t GROUP BY col HAVING COUNT(*) > :min
FROM t1 INNER JOIN t2 ON t1.id = t2.fk
FROM t1 LEFT JOIN t2 ON t1.id = t2.fk
UPDATE t SET col = :val WHERE id = :id
DELETE FROM t WHERE id = :id
```

### Migration Rule

```
Changed @Entity?
  → increment version in @Database
  → Simple change: AutoMigration(from = X, to = Y)
  → Complex change: manual Migration object { execSQL(...) }
  → Register: .addMigrations(...) in databaseBuilder
  → Dev only: .fallbackToDestructiveMigration()
```

### Hilt vs Koin

```
Hilt: errors at BUILD time | @Module @InstallIn @Provides @Singleton
Koin: errors at RUNTIME    | module { single { } viewModel { } get() }
```

---

_Sources: developer.android.com/training/data-storage/room · developer.android.com/build/migrate-to-ksp · insert-koin.io · androidx Room release notes 2.8.4_