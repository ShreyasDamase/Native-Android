# Room Database — Complete Master Notes (Expanded Edition)

### For Android Jetpack Compose Developers

  

---

  

## Table of Contents

  

1. [The Story — Why Room Exists](#1-the-story--why-room-exists)

2. [What SQLite Is (The Foundation)](#2-what-sqlite-is-the-foundation)

3. [KSP vs KAPT vs annotationProcessor — Deep Dive](#3-ksp-vs-kapt-vs-annotationprocessor--deep-dive)

4. [Version History — How Room Evolved](#4-version-history--how-room-evolved)

5. [The 3 Core Components — Mental Model](#5-the-3-core-components--mental-model)

6. [@Entity — Table Definition, Every Annotation Explained](#6-entity--table-definition-every-annotation-explained)

7. [@Dao — Every Annotation and Query Explained](#7-dao--every-annotation-and-query-explained)

8. [SQL Query Reference — Complete with Theory](#8-sql-query-reference--complete-with-theory)

9. [@Database — The Glue Class](#9-database--the-glue-class)

10. [Type Converters — Deep Dive](#10-type-converters--deep-dive)

11. [Relationships — Theory + All Types](#11-relationships--theory--all-types)

12. [Flow + Coroutines — How They Work Together](#12-flow--coroutines--how-they-work-together)

13. [Database Migrations — Theory + All Cases](#13-database-migrations--theory--all-cases)

14. [Room + Hilt — Complete Production Setup](#14-room--hilt--complete-production-setup)

15. [Room + Koin — Complete Setup and Comparison](#15-room--koin--complete-setup-and-comparison)

16. [How Room Works Internally — Step by Step](#16-how-room-works-internally--step-by-step)

17. [Testing Room Database](#17-testing-room-database)

18. [Database Inspector (Android Studio Tool)](#18-database-inspector-android-studio-tool)

19. [Performance Tips](#19-performance-tips)

20. [Common Mistakes — with Why They Happen](#20-common-mistakes--with-why-they-happen)

21. [Interview Questions — Deep Answers](#21-interview-questions--deep-answers)

22. [Quick Reference Cheat Sheet](#22-quick-reference-cheat-sheet)

  

---

  

---

  

# 1. The Story — Why Room Exists

  

## Before Room — The Pain of Raw SQLite (2008–2017)

  

Android has had SQLite built in since version 1.0 (2008). The tool for working with it was `SQLiteOpenHelper` — a class you had to subclass, override `onCreate()` and `onUpgrade()`, and manually write every query.

  

Here is what a real query looked like without Room:

  

```

1. Get a writable database reference: db.getWritableDatabase()

2. Build the SQL string manually: "SELECT * FROM users WHERE id = ?"

3. Execute and get back a Cursor object

4. Check: if (cursor.moveToFirst()) { ... }

5. Extract each column by index: cursor.getString(cursor.getColumnIndex("name"))

6. Cast every value manually

7. Build your Kotlin/Java object from those values

8. cursor.close()       ← forget this → memory leak

9. db.close()           ← forget this → database locked

10. Do all of this on a background thread (or crash)

11. Post result back to main thread manually

```

  

That's 50–100 lines for a single query. Now imagine having 30 queries.

  

****The biggest pain points:****

```

1. No compile-time validation

   → Typo "SELCT * FROM users" → app runs fine → one user hits that path → CRASH

   → You shipped a bug that only crashes in production

  

2. Boilerplate explosion

   → Every query = open cursor + loop + close cursor + convert

   → Error-prone, repetitive, tedious

  

3. No thread safety

   → Forget to move to background thread → app crashes on main thread

  

4. Manual schema migration

   → App update changes DB structure → existing user's DB version mismatches

   → You must write ALTER TABLE SQL by hand or users lose their data

  

5. No reactive updates

   → Query result is a snapshot — you must re-query manually every time data changes

```

  

## The Solution (2017)

  

Google released ****Room**** in May 2017 as part of ****Android Architecture Components****, a collection of Jetpack libraries designed to solve the most common Android development pains. Room was made stable in November 2017 and has been the standard since.

  

Room's core idea: ****you describe what you want, Room writes the implementation.****

  

```

You write:   @Query("SELECT * FROM users WHERE id = :userId")

             suspend fun getUserById(userId: Int): UserEntity?

  

Room writes: The entire Cursor management, null checking, column extraction,

             object creation, thread handling — ALL of it.

```

  

****The breakthrough feature:**** Room reads your `@Query` SQL at ****compile time****, not runtime. If your SQL has a typo, your build fails. The user never sees it.

  

## Who Made It and Why

  

- ****Company:**** Google, Android Jetpack team

- ****First alpha:**** May 2017

- ****First stable:**** November 2017

- ****License:**** Apache 2.0 (free for commercial use)

- ****Part of:**** AndroidX (`androidx.room`)

- ****Philosophy:**** "SQLite with superpowers" — keep SQLite's speed and power, remove the pain

  

The name "Room" is a metaphor for a structured, organized space for data storage — like a room in a house where everything has a designated place.

  

---

  

---

  

# 2. What SQLite Is (The Foundation)

  

## What SQLite Actually Is

  

SQLite is a C-language library that implements a self-contained, serverless, zero-configuration SQL database engine. It is not a separate server process — it's a library that reads and writes to a single `.db` file on the filesystem.

  

****Key facts:****

- Written in C, extremely fast

- Single file on disk: `databases/app_database.db` in your app's private storage

- Built into every Android device since version 1.0

- The most widely deployed database engine in the world (used in Firefox, Chrome, iOS, Python, Airbus A350, etc.)

- Serverless — no port, no network, no configuration

- ACID compliant — transactions are Atomic, Consistent, Isolated, Durable

  

## How Data Is Stored — The Table Model

  

SQLite organizes data into tables. A table is exactly like a spreadsheet:

  

```

TABLE: users

┌────┬──────────┬───────────────────────┬─────┬──────────────────────┐

│ id │ name     │ email                 │ age │ created_at           │

├────┼──────────┼───────────────────────┼─────┼──────────────────────┤

│  1 │ Rahul    │ rahul@example.com     │  24 │ 1700000000000        │

│  2 │ Priya    │ priya@example.com     │  22 │ 1700000001000        │

│  3 │ Arjun    │ arjun@example.com     │  28 │ 1700000002000        │

└────┴──────────┴───────────────────────┴─────┴──────────────────────┘

  

TABLE: posts

┌────┬─────────┬──────────────────────────────┬──────────┐

│ id │ user_id │ title                        │ body     │

├────┼─────────┼──────────────────────────────┼──────────┤

│  1 │    1    │ My first post                │ Hello... │

│  2 │    1    │ Room database is great       │ I tried..│

│  3 │    2    │ Kotlin is amazing            │ Today... │

└────┴─────────┴──────────────────────────────┴──────────┘

```

  

`user_id` in posts = `id` in users. This connection is the ****foreign key**** relationship — Rahul (id=1) has posts 1 and 2.

  

## How Room Maps Kotlin to SQLite Types

  

SQLite has only 5 storage types. Room converts Kotlin types automatically:

  

| Kotlin Type | SQLite Type | How Room Stores It |

|---|---|---|

| `Int` | `INTEGER` | Direct |

| `Long` | `INTEGER` | Direct (use Long for large numbers, timestamps) |

| `Float` | `REAL` | Direct |

| `Double` | `REAL` | Direct |

| `String` | `TEXT` | Direct |

| `Boolean` | `INTEGER` | `true` → `1`, `false` → `0` |

| `ByteArray` | `BLOB` | Raw bytes |

| `Date` | requires `@TypeConverter` | You convert to `Long` (milliseconds) |

| `Enum` | requires `@TypeConverter` | You convert to `String` (enum name) |

| `List<*>` | requires `@TypeConverter` | You convert to JSON or comma-separated String |

  

## What a .db File Actually Is

  

When Room creates a database, it creates 3 files:

  

```

/data/data/com.yourapp/databases/

├── app_database.db        ← the main database file (your data)

├── app_database.db-shm    ← shared memory file (WAL mode indexing)

└── app_database.db-wal    ← write-ahead log (uncommitted changes buffer)

```

  

WAL (Write-Ahead Logging) is SQLite's default write mode. Instead of modifying the main file directly, changes are first written to the WAL file. This allows reading while writing simultaneously — much better performance.

  

---

  

---

  

# 3. KSP vs KAPT vs annotationProcessor — Deep Dive

  

## Why Annotation Processing Exists — The Real Story

  

Libraries like Room, Hilt, Moshi need to generate code based on your annotations. But code generation at ****runtime**** using Reflection is slow — every time you call `userDao.getUsers()` it would have to inspect your class structure. Too slow for mobile.

  

So these libraries generate code at ****build time**** instead. A tool reads your annotated code, generates the implementation `.kt` or `.java` files, and those files get compiled into your app. At runtime, there's no reflection cost — it's all precompiled.

  

The tool that does this reading + generating is called an ****Annotation Processor****.

  

```

Your annotated code

        ↓

  Annotation Processor reads it at BUILD TIME

        ↓

  Generates implementation code (e.g., UserDao_Impl.kt)

        ↓

  Generated code is compiled into your app

        ↓

  At RUNTIME: fast, no reflection, no generation

```

  

## Option 1: `annotationProcessor` (Java APT)

  

The original Java Annotation Processing Tool, built into `javac`.

  

```kotlin

// Java projects only:

annotationProcessor("androidx.room:room-compiler:$room_version")

```

  

****How it works:**** Reads compiled `.class` files (Java bytecode) and generates more Java code.

  

****Problem:**** Cannot read Kotlin source files at all. Kotlin has to be compiled to Java first, which KAPT does for us.

  

****Status:**** Still valid for Java-only modules. Never use in Kotlin projects.

  

---

  

## Option 2: `kapt` (Kotlin Annotation Processing Tool)

  

Created in ~2015 as a bridge between Kotlin code and Java annotation processors.

  

```kotlin

// Kotlin projects (old way):

plugins { id("kotlin-kapt") }

kapt("androidx.room:room-compiler:$room_version")

```

  

### The KAPT Hack Explained Step by Step

  

KAPT faces one fundamental problem: Java annotation processors read Java bytecode, but your code is in Kotlin. Kotlin bytecode is not the same as Java bytecode.

  

KAPT's solution:

  

```

Step 1 — STUB GENERATION:

Your Kotlin source code is analyzed by the Kotlin compiler.

For every class, interface, function:

  - A simplified Java "stub" is created

  - The stub looks like Java: public class UserDao { ... }

  - The stub captures type signatures but has NO implementation bodies

  

Step 2 — APT PROCESSING:

The Java annotation processor (Room's compiler) reads those stubs.

It finds @Dao, @Entity, @Query etc.

It generates UserDao_Impl.java based on what it sees in the stubs.

  

Step 3 — COMPILATION:

The generated .java files are compiled.

Your Kotlin source is compiled.

Both are combined into your app.

```

  

### What Gets Lost in KAPT Stubs

  

This is why KAPT causes subtle bugs:

  

```

Kotlin:  fun getUser(id: Int?): User?

         ↓ (KAPT stub generation)

Java:    @Nullable public User getUser(@Nullable Integer id)

  

Lost:

- Kotlin's inline functions → can't be represented in Java stubs

- Some generic type variance (in/out) → approximated

- Kotlin coroutine internals → partially lost

- Suspend modifier → converted to Java callback-style

- Some null safety nuances

```

  

### KAPT Performance Cost

  

The stub generation step alone adds significant time:

```

On a medium project:

  Total build time: ~60 seconds

  Stub generation: ~20-25 seconds (30-40% of total!)

  

On a large project with many annotated classes:

  Total build time: ~180 seconds  

  Stub generation: ~70-80 seconds

```

  

****Google officially announced KAPT as "in maintenance mode" in 2023.**** It will not receive new features. Bug fixes only. KSP is the future.

  

---

  

## Option 3: `ksp` (Kotlin Symbol Processing)

  

Created by Google and announced in 2020, stable since ****October 2022****.

  

```kotlin

// Kotlin projects (new way — always use this):

plugins { id("com.google.devtools.ksp") version "2.0.0-1.0.21" }

ksp("androidx.room:room-compiler:$room_version")

```

  

### How KSP Works — Truly Native

  

KSP is not a bridge. It is built directly into the Kotlin compiler pipeline.

  

```

Your Kotlin source code

        ↓

  Kotlin Compiler starts parsing (building the AST — Abstract Syntax Tree)

        ↓

  KSP hooks into the compiler at this stage — BEFORE any compilation

        ↓

  KSP reads the Kotlin AST directly (real Kotlin types, not Java stubs)

        ↓

  Room's KSP processor sees your @Entity, @Dao etc. in their true form

        ↓

  Generates UserDao_Impl.kt (Kotlin, not Java!)

        ↓

  Kotlin compiler compiles everything together

```

  

No stubs. No Java translation. No information loss.

  

### KSP Benefits in Detail

  

```

1. Speed

   KSP is typically 2x faster than KAPT for annotation processing

   No stub generation step → directly saves 30-40% build time

2. Correctness

   Reads actual Kotlin types → suspend functions understood properly

   Null safety: knows Int vs Int? → better generated code

   Inline/reified generics → handled correctly

3. Kotlin Multiplatform Ready

   KSP works on JVM, Android, iOS (via Kotlin/Native), JS

   KAPT only works on JVM (Android) — cannot be used for KMP

4. Generated Code Quality

   Room + KSP generates .kt files (Kotlin)

   Room + KAPT generates .java files (Java that Kotlin uses)

   Kotlin-generated code is more idiomatic, smaller, faster

5. Memory Usage

   KAPT holds Kotlin + Java representations simultaneously in memory

   KSP only holds one (Kotlin) → 30-50% less memory during build

```

  

### KSP Version Numbering — Important

  

KSP versions are tied to the Kotlin version:

  

```

ksp version format: {kotlin-version}-{ksp-version}

  

Example: "2.0.0-1.0.21"

          ↑↑↑↑↑  ↑↑↑↑↑

       Kotlin    KSP patch

       version   version

  

Rule: KSP version's Kotlin part MUST match your Kotlin version.

If your project uses Kotlin 2.0.0, use KSP 2.0.0-x.x.x

If your project uses Kotlin 1.9.20, use KSP 1.9.20-x.x.x

```

  

## The Full Comparison

  

| Feature | `annotationProcessor` | `kapt` | `ksp` |

|---|---|---|---|

| Language | Java only | Java + Kotlin | Kotlin only |

| Reads | Java bytecode | Java stubs from Kotlin | Kotlin AST directly |

| Build speed | Fast (Java) | Slow (stub gen) | Fast (2x vs kapt) |

| Kotlin features | None | Partial | Full |

| Generated code | `.java` | `.java` | `.kt` (native Kotlin) |

| Memory cost | Low | High (dual representation) | Low |

| KMP support | No | No | Yes |

| Google status | Active (Java) | Maintenance mode | Active — the future |

| Room support since | Never | Room 1.0 | Room 2.4.0 (2022) |

| Hilt support since | Never | Hilt 1.0 | Hilt 2.48 (2023) |

  

## Seeing KSP Generated Code

  

After building, generated files are in:

```

app/build/generated/ksp/debug/kotlin/com/yourapp/data/local/dao/

└── UserDao_Impl.kt     ← generated by KSP, readable Kotlin code

```

  

You can open and read this file in Android Studio — it shows exactly what SQL Room is executing.

  

---

  

---

  

# 4. Version History — How Room Evolved

  

## Room 1.0 (November 2017)

First stable release. Solved the raw SQLite pain.

  

****What it had:****

- `@Entity`, `@Dao`, `@Database` annotations

- `@Query` with compile-time SQL validation (the breakthrough)

- `@Insert`, `@Update`, `@Delete` convenience annotations

- `LiveData<T>` return type for reactive data

- Only KAPT supported

- Java-first API (Kotlin worked but was awkward)

- No `suspend` function support → used callbacks or RxJava

  

****What was painful:****

- Migrations were entirely manual — write all SQL by hand

- No coroutines → had to use `AsyncTask` (now deprecated) or callbacks

- Java-style API in Kotlin felt unnatural

  

---

  

## Room 2.0 (September 2018)

Jetpack repackaging — moved from `android.arch.persistence.room` to `androidx.room`.

  

****What changed:**** Package name only + minor internal improvements. Same API.

  

****Why it matters:**** This was the "Jetpack era" when Google unified its libraries. From this version, Room is part of AndroidX.

  

---

  

## Room 2.1 (June 2019)

****The Kotlin Coroutines release**** — the most important Kotlin upgrade.

  

****What it added:****

- `suspend` function support in DAOs:

  ```kotlin

  // Before 2.1 — had to use callbacks or RxJava

  @Query("SELECT * FROM users")

  fun getAllUsers(): LiveData<List<UserEntity>>

  // After 2.1 — natural Kotlin coroutine syntax

  @Query("SELECT * FROM users")

  suspend fun getAllUsers(): List<UserEntity>

  ```

- `Flow<T>` return type added — reactive queries that update automatically

- `CoroutineScope` transaction support

  

This release made Room feel truly Kotlin-native for the first time.

  

---

  

## Room 2.2 (October 2019)

  

****What it added:****

- `@ColumnInfo(defaultValue = "value")` — specify default column values

- Pre-populated databases — ship your app with an existing `.db` file

- `expand projection` query verification — Room warns when `SELECT *` might return unexpected columns

- Better `@ForeignKey` support

  

---

  

## Room 2.3 (April 2021)

  

****What it added:****

- `@Upsert` annotation preview (formal in 2.5)

- Kotlin extensions improvements

- Paging 3 integration — `PagingSource<K, V>` return type

- Enum support improved (can use enums with string affinity directly in some cases)

- `@ColumnInfo(index = true)` shorthand

  

---

  

## Room 2.4 (January 2022)

****The KSP release**** — second most important release after 2.1.

  

****What it added:****

- ****KSP support**** — `ksp("androidx.room:room-compiler")` now works

- ****Auto-migration**** — Room generates migration SQL automatically for simple changes

  ```kotlin

  autoMigrations = [AutoMigration(from = 1, to = 2)]

  ```

- ****Kotlin code generation**** — Room + KSP generates `.kt` files instead of `.java`

- `@DeleteColumn` and `@RenameTable` auto-migration spec annotations

- Multimap return types — query can return `Map<UserEntity, List<PostEntity>>`

  

---

  

## Room 2.5 (December 2022)

  

****What it added:****

- `@Upsert` annotation (stable) — insert if not exists, update if exists

- Kotlin `value class` support (experimental) — use Kotlin inline value classes

- Better null safety for queries returning nullable types

- Performance improvements to the SQLite WAL checkpoint process

  

---

  

## Room 2.6 (October 2023)

  

****What it added:****

- ****Room Gradle Plugin**** — cleaner schema configuration:

  ```kotlin

  plugins { id("androidx.room") }

  android { room { schemaDirectory("$projectDir/schemas") } }

  ```

- `room.generateKotlin = true` by default when using KSP — generates Kotlin code

- `@RenameColumn` auto-migration spec

- Better incremental compilation support

  

---

  

## Room 2.7 (February 2024)

****The Kotlin Multiplatform release.****

  

****What it added:****

- ****KMP support**** — Room now works on Android AND iOS from shared code

- `SQLiteDriver` interface — new lower-level driver API for cross-platform

- `BundledSQLiteDriver` — ships SQLite from source (consistent behavior across platforms, not dependent on OS's SQLite)

- `@ConstructedBy` annotation for KMP database builders

- `InstantTaskExecutorRule` for testing updated

  

---

  

## Room 2.8 (September 2025, current)

  

****What it added:****

- Min SDK raised from API 21 to ****API 23**** (Android 6.0)

- WatchOS and TvOS KMP targets

- `room-sqlite-wrapper` new artifact — compatibility bridge for migrating from `SupportSQLiteDatabase` to `SQLiteDriver`

- Prepared statement cache in connection pool — significant performance improvement for repeated queries

- Deadlock fixes for auto-closed databases

  

---

  

## Feature Timeline Summary

  

| Year | Version | Key Addition |

|---|---|---|

| 2017 | 1.0 | Launch, compile-time SQL validation, LiveData |

| 2019 | 2.1 | `suspend` functions, `Flow` — Kotlin native |

| 2019 | 2.2 | Default values, pre-populated DB |

| 2022 | 2.4 | KSP support, Auto-migrations |

| 2022 | 2.5 | `@Upsert` stable |

| 2023 | 2.6 | Room Gradle Plugin, Kotlin codegen default |

| 2024 | 2.7 | Kotlin Multiplatform |

| 2025 | 2.8 | Min API 23, WatchOS/TvOS, performance |

  

---

  

---

  

# 5. The 3 Core Components — Mental Model

  

Every Room database, no matter how complex, is built from exactly 3 building blocks.

  

```

┌──────────────────────────────────────────────────────────────────┐

│                         @Database                               │

│                    AppDatabase.kt                               │

│                                                                  │

│  "I know about all tables and all query interfaces"             │

│  @Database(entities = [UserEntity::class, PostEntity::class])   │

│  abstract fun userDao(): UserDao                                │

│  abstract fun postDao(): PostDao                                │

└──────────────┬───────────────────────────┬───────────────────────┘

               │                           │

               ▼                           ▼

    ┌─────────────────┐          ┌──────────────────────┐

    │    @Entity      │          │       @Dao            │

    │  UserEntity.kt  │          │    UserDao.kt         │

    │                 │          │                       │

    │  "I AM a table" │          │  "I am the remote    │

    │                 │          │   control for the    │

    │  id: Int        │          │   users table"       │

    │  name: String   │          │                       │

    │  email: String  │          │  getAllUsers()        │

    │                 │          │  getUserById()       │

    │  Creates:       │          │  insertUser()        │

    │  users table    │          │  deleteUser()        │

    └─────────────────┘          └──────────────────────┘

```

  

## The Analogy That Makes It Click

  

Think of a restaurant:

  

```

@Entity  = the menu item blueprint

           (defines what a "Burger" is: bun, patty, toppings)

@Dao     = the waiter

           (takes orders: "get me a burger", "add new order", "delete table 5's order")

@Database = the restaurant itself

             (owns the kitchen (SQLite), knows all menu items, has all waiters)

```

  

## Why 3 Separate Things?

  

****Single Responsibility Principle:****

  

| Component | Responsibility | Changes when... |

|---|---|---|

| `@Entity` | Data shape | You add/remove a field |

| `@Dao` | Query logic | You need a new query |

| `@Database` | Database configuration | You add a new entity or migration |

  

If you need to add a query for users, you only touch `UserDao`. The entity and database don't care. If you rename a column, you only touch `UserEntity` (and write a migration). The DAO functions don't change if column names match.

  

---

  

---

  

# 6. @Entity — Table Definition, Every Annotation Explained

  

## What @Entity Actually Does

  

When Room's annotation processor (KSP) reads `@Entity` on a class, it:

1. Takes the class name → becomes the table name (unless `tableName` specified)

2. Takes each `val`/`var` field → becomes a column

3. Checks for `@PrimaryKey` → sets up the primary key constraint

4. Reads other annotations → applies indices, foreign keys, etc.

5. Generates SQL: `CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, ...)`

  

## Basic @Entity

  

```kotlin

@Entity(tableName = "users")

data class UserEntity(

    @PrimaryKey(autoGenerate = true)

    val id: Int = 0,          // = 0 means "not assigned yet, let SQLite choose"

    val name: String,

    val email: String,

    val age: Int,

    val isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis()  // timestamp

)

```

  

****Generated SQL (you can see this in the schema JSON file):****

```sql

CREATE TABLE IF NOT EXISTS `users` (

    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,

    `name` TEXT NOT NULL,

    `email` TEXT NOT NULL,

    `age` INTEGER NOT NULL,

    `is_active` INTEGER NOT NULL DEFAULT 1,

    `created_at` INTEGER NOT NULL

)

```

  

## @Entity Properties in Detail

  

### `tableName`

  

```kotlin

@Entity(tableName = "users")           // explicit — recommended

@Entity                                 // uses class name: "userentity" (lowercased)

@Entity(tableName = "user_profiles")   // snake_case recommended for SQL

```

  

****Why explicit tableName matters:**** If you rename your Kotlin class from `UserEntity` to `AppUserEntity`, without `tableName` your table is also renamed — which requires a migration. With explicit `tableName`, renaming the class has no database impact.

  

### `indices`

  

An index is a hidden sorted list Room maintains so it can find rows faster.

  

```

Without index: "Find user where email = 'test@test.com'"

→ SQLite reads EVERY row until it finds a match (full table scan)

→ 1,000 users = check 1,000 rows

  

With index on email column:

→ SQLite uses the index (like a book's index)

→ Jumps directly to the matching row

→ 1,000,000 users = still fast

```

  

```kotlin

@Entity(

    tableName = "users",

    indices = [

        Index(value = ["email"], unique = true),  // unique: no two users with same email

        Index(value = ["name"]),                   // non-unique: faster searches by name

        Index(value = ["city", "age"])             // composite: faster when filtering by BOTH

    ]

)

```

  

****When to add an index:****

- Any column you use in `WHERE` clauses frequently

- Foreign key columns (user_id in posts table) — always index these

- Columns you sort by frequently (`ORDER BY`)

  

****When NOT to add index:****

- Columns you never search/sort by — indexes take up disk space and slow down writes

  

### `foreignKeys`

  

Foreign keys enforce a relationship between tables. They prevent invalid data.

  

```kotlin

@Entity(

    tableName = "posts",

    foreignKeys = [

        ForeignKey(

            entity = UserEntity::class,

            parentColumns = ["id"],        // the referenced column in UserEntity

            childColumns = ["user_id"],    // the column in THIS table

            onDelete = ForeignKey.CASCADE, // when user deleted → delete their posts too

            onUpdate = ForeignKey.CASCADE  // when user id changes → update post's user_id

        )

    ],

    indices = [Index("user_id")]           // always index foreign key columns!

)

data class PostEntity(

    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val userId: Int,           // this links to UserEntity.id

    val title: String,

    val body: String

)

```

  

******`**onDelete**` **behavior options:****

  

| Option | What happens when parent row is deleted |

|---|---|

| `CASCADE` | Child rows are automatically deleted too |

| `SET_NULL` | Child's foreign key column is set to NULL (column must be nullable) |

| `SET_DEFAULT` | Child's foreign key column is set to its default value |

| `RESTRICT` | Deletion is prevented if child rows exist (throws exception) |

| `NO_ACTION` | Nothing happens — orphan rows remain (dangerous, usually wrong) |

  

****Rule:**** For most parent-child relationships (user → posts), use `CASCADE`. For optional relationships, `SET_NULL`.

  

## Field-Level Annotations

  

### `@PrimaryKey`

  

```kotlin

// Auto-increment integer (most common)

@PrimaryKey(autoGenerate = true)

val id: Int = 0

  

// Manual integer ID (you provide the ID yourself)

@PrimaryKey

val id: Int

  

// String UUID (good for sync with remote API)

@PrimaryKey

val id: String = UUID.randomUUID().toString()

  

// Composite primary key (in @Entity annotation, not field)

@Entity(primaryKeys = ["userId", "courseId"])

data class EnrollmentEntity(

    val userId: Int,

    val courseId: Int,

    val enrolledAt: Long = System.currentTimeMillis()

)

```

  

****Why** `**id = 0**` **default for autoGenerate?****

  

When you set `autoGenerate = true`, Room ignores the `id` value when inserting if it's `0`. SQLite then assigns the next available ID. If you use `-1` or any other non-zero value, Room treats it as a real ID and tries to insert it, which may cause conflicts.

  

### `@ColumnInfo`

  

```kotlin

@ColumnInfo(

    name = "first_name",         // SQL column name (Kotlin field can differ)

    defaultValue = "Unknown",    // used in SQL INSERT if column not provided

    index = true,                // shorthand for adding index on this column

    collate = ColumnInfo.NOCASE  // case-insensitive text comparisons

)

val firstName: String

```

  

****Why** `**name**` **matters:**** The SQL column name `first_name` is in the database file. Your Kotlin field name `firstName` is in your code. You can rename the Kotlin field without a migration (it's just a code refactor). But renaming the SQL column name requires a migration.

  

****Collation options:****

```

UNSPECIFIED (default) — use SQLite's default

BINARY       — byte-by-byte comparison (case-sensitive, 'A' ≠ 'a')

NOCASE       — case-insensitive ('A' == 'a', good for name searches)

RTRIM        — ignores trailing whitespace

```

  

### `@Ignore`

  

```kotlin

@Entity

data class UserEntity(

    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val name: String,

    val email: String,

    @Ignore

    val fullDisplayName: String = ""    // computed at runtime, not stored

)

  

// If you have multiple constructors (Room needs @Ignore on extra ones)

@Entity

data class UserEntity(

    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val name: String

) {

    @Ignore

    constructor(name: String) : this(0, name)  // Room uses the primary constructor

}

```

  

### `@Embedded`

  

`@Embedded` is a space-saver. Instead of creating a separate table for `Address`, you flatten its fields directly into the `users` table.

  

```kotlin

data class Address(

    val street: String,

    val city: String,

    val state: String,

    val pincode: String

)

  

@Entity(tableName = "users")

data class UserEntity(

    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val name: String,

    @Embedded val address: Address  // no separate table!

)

  

// Resulting users table columns:

// id, name, street, city, state, pincode

```

  

****When to use @Embedded vs a separate @Entity:****

  

| Use `@Embedded` when | Use separate `@Entity` when |

|---|---|

| The nested object is ONLY used by this entity | The nested object is shared between entities |

| You never query the nested object independently | You need to query by nested object fields independently |

| The relationship is "part of" (user HAS an address) | The relationship is "connected to" (user POSTS in posts) |

  

****Prefix to avoid column name conflicts:****

```kotlin

@Embedded(prefix = "home_")

val homeAddress: Address      // columns: home_street, home_city, home_state, home_pincode

  

@Embedded(prefix = "work_")

val workAddress: Address      // columns: work_street, work_city, work_state, work_pincode

```

  

---

  

---

  

# 7. @Dao — Every Annotation and Query Explained

  

## What DAO Means

  

DAO = ****Data Access Object****. It's a design pattern from enterprise Java (early 2000s) that separates data access logic from business logic. The idea: your ViewModel doesn't know *_how_* data is stored — it only knows *_what operations are available_*. That "what" is the DAO.

  

```

ViewModel: "give me all users"

               ↓

           UserRepository: "ask the DAO"

               ↓

           UserDao: "SELECT * FROM users"  ← knows HOW

               ↓

           Room/SQLite: executes SQL

```

  

## @Dao Interface vs Abstract Class

  

```kotlin

// Option 1: interface (most common)

@Dao

interface UserDao {

    @Insert suspend fun insertUser(user: UserEntity)

}

  

// Option 2: abstract class (use when you need shared helper functions)

@Dao

abstract class UserDao {

    @Insert

    abstract suspend fun insertUser(user: UserEntity)

    // Non-abstract helper — can contain logic

    suspend fun insertUserIfValid(user: UserEntity) {

        if (user.name.isNotBlank()) {

            insertUser(user)

        }

    }

}

```

  

Use abstract class when you need helper logic that combines multiple DAO operations.

  

## @Insert in Depth

  

```kotlin

// Insert single

@Insert

suspend fun insertUser(user: UserEntity): Long    // returns new row ID

  

// Insert multiple

@Insert

suspend fun insertUsers(users: List<UserEntity>): List<Long>  // returns list of IDs

  

// Insert with vararg

@Insert

suspend fun insertUsers(vararg users: UserEntity)

  

// Insert and handle conflict

@Insert(onConflict = OnConflictStrategy.REPLACE)

suspend fun upsertUser(user: UserEntity)

  

@Insert(onConflict = OnConflictStrategy.IGNORE)

suspend fun insertIfNotExists(user: UserEntity): Long  // returns -1 if ignored

```

  

****OnConflict Strategy explained with real examples:****

  

```

Imagine the users table has: id=1, email="test@test.com", name="Rahul"

Now you try to INSERT: id=1, email="new@test.com", name="NewName"

  

REPLACE  → DELETE row with id=1, INSERT the new one

           Result: id=1, email="new@test.com", name="NewName"

           ⚠️ If new object has fewer fields, missing fields become default values!

  

IGNORE   → Keep: id=1, email="test@test.com", name="Rahul"

           The new insert is silently dropped

           Returns: -1

  

ABORT    → throws SQLiteConstraintException

           App crashes if not caught

  

FAIL     → throws exception but lets other inserts in the batch continue

  

ROLLBACK → throws exception and reverts ALL inserts in the current transaction

```

  

## @Update in Depth

  

```kotlin

@Update

suspend fun updateUser(user: UserEntity): Int   // returns: number of rows updated (0 if not found)

  

@Update

suspend fun updateUsers(users: List<UserEntity>): Int

```

  

****How @Update works internally:****

Room generates SQL: `UPDATE users SET name=?, email=?, age=? WHERE id=?`

  

Room matches by primary key. It takes the `id` from your `UserEntity` object, finds the row with that `id`, and updates ALL other columns. You must pass the complete entity object — not just the changed fields.

  

****What if you only want to update one column?**** Use `@Query`:

```kotlin

@Query("UPDATE users SET name = :name WHERE id = :id")

suspend fun updateName(id: Int, name: String)

```

  

## @Delete in Depth

  

```kotlin

@Delete

suspend fun deleteUser(user: UserEntity): Int   // returns number of rows deleted

  

@Delete

suspend fun deleteUsers(users: List<UserEntity>)

```

  

Room generates: `DELETE FROM users WHERE id=?`

  

Only the primary key matters. Even if other fields in the passed object are different from the database, Room still deletes by ID.

  

****Delete by query (more practical):****

```kotlin

@Query("DELETE FROM users WHERE id = :userId")

suspend fun deleteUserById(userId: Int)

  

@Query("DELETE FROM users")

suspend fun deleteAllUsers()

  

@Query("DELETE FROM users WHERE created_at < :cutoff")

suspend fun deleteOldUsers(cutoff: Long)

```

  

## @Upsert (Room 2.5+) — The Smart Insert-or-Update

  

```kotlin

@Upsert

suspend fun upsertUser(user: UserEntity): Long

  

@Upsert

suspend fun upsertUsers(users: List<UserEntity>): List<Long>

```

  

****Why @Upsert is better than** `**@Insert(onConflict = REPLACE)**`**:****

  

```

@Insert(onConflict = REPLACE):

  Step 1: DELETE the old row (all foreign key children pointing to it may be affected!)

  Step 2: INSERT the new row with a new ROWID

  → Foreign keys with CASCADE delete may trigger on the DELETE step!

  → The row gets a new internal ROWID (affects some SQLite operations)

  

@Upsert:

  Step 1: Try INSERT

  Step 2: If conflict → UPDATE the existing row (no delete!)

  → Foreign key children are safe

  → Row keeps its original ROWID

  → Cleaner, safer operation

```

  

## @Query — The Most Powerful

  

```kotlin

// Parameters with : prefix

@Query("SELECT * FROM users WHERE id = :userId")

suspend fun getUserById(userId: Int): UserEntity?

  

// Multiple parameters

@Query("SELECT * FROM users WHERE age >= :minAge AND city = :city")

suspend fun getUsersByAgeAndCity(minAge: Int, city: String): List<UserEntity>

  

// IN clause — Room handles List expansion automatically

@Query("SELECT * FROM users WHERE id IN (:userIds)")

suspend fun getUsersByIds(userIds: List<Int>): List<UserEntity>

// Room generates: WHERE id IN (?, ?, ?) and binds each value

  

// Returning non-entity data class

@Query("SELECT id, name FROM users")

suspend fun getUserNameList(): List<UserNameOnly>

// UserNameOnly is a plain data class: data class UserNameOnly(val id: Int, val name: String)

  

// Flow for reactive queries

@Query("SELECT * FROM users ORDER BY name ASC")

fun getUsersFlow(): Flow<List<UserEntity>>

```

  

## @Transaction — When It's Required

  

```kotlin

// Use case 1: Multiple operations that must succeed together

@Transaction

suspend fun moveUserToNewCity(userId: Int, newCityId: Int) {

    removeFromCurrentCity(userId)      // if this succeeds but next fails...

    addToNewCity(userId, newCityId)    // ...with @Transaction both are rolled back

}

  

// Use case 2: ALL @Relation queries MUST have @Transaction

// Without it: Room reads users (snapshot A), then reads posts (snapshot B)

// If data changed between the two reads → inconsistent result

// With @Transaction: both reads happen atomically → always consistent

@Transaction

@Query("SELECT * FROM users")

fun getUsersWithPosts(): Flow<List<UserWithPosts>>

  

// Use case 3: Atomic read-then-write

@Transaction

suspend fun incrementLoginCount(userId: Int) {

    val user = getUserById(userId)

    user?.let { updateLoginCount(it.id, it.loginCount + 1) }

    // Both reads and writes are atomic

}

```

  

---

  

---

  

# 8. SQL Query Reference — Complete with Theory

  

## Why SQL Matters in Room

  

Room lets you write real SQL in `@Query`. Understanding SQL means you can write any query yourself without guessing. SQLite supports most standard SQL.

  

## SELECT — Theory + All Variants

  

### The SELECT Execution Order

  

SQL is processed in this order (not the order you write it):

```

1. FROM    → which table(s)

2. JOIN    → combine tables

3. WHERE   → filter rows

4. GROUP BY → group rows

5. HAVING  → filter groups

6. SELECT  → choose columns

7. ORDER BY → sort

8. LIMIT   → restrict count

```

  

Knowing this order helps you understand why `HAVING` comes after `GROUP BY`, and why you can't use column aliases in `WHERE`.

  

### Basic Queries

  

```sql

-- All columns, all rows

SELECT * FROM users

  

-- Specific columns only (more efficient than SELECT *)

SELECT id, name, email FROM users

  

-- With table alias (useful in JOINs)

SELECT u.id, u.name FROM users AS u

  

-- Distinct values only (no duplicates)

SELECT DISTINCT city FROM users

  

-- Calculated column

SELECT name, age, (age * 365) AS age_in_days FROM users

```

  

### WHERE Clause

  

```sql

-- Equality

SELECT * FROM users WHERE id = :userId

  

-- Comparison operators

SELECT * FROM users WHERE age > :minAge

SELECT * FROM users WHERE age >= :minAge

SELECT * FROM users WHERE age < :maxAge

SELECT * FROM users WHERE age BETWEEN :min AND :max   -- inclusive

  

-- NULL checks

SELECT * FROM users WHERE nickname IS NULL

SELECT * FROM users WHERE nickname IS NOT NULL

  

-- Multiple conditions

SELECT * FROM users WHERE age > 18 AND city = :city

SELECT * FROM users WHERE age < 18 OR city = :city

  

-- IN list — useful for fetching specific IDs

SELECT * FROM users WHERE id IN (:ids)

SELECT * FROM users WHERE city NOT IN (:excludedCities)

  

-- LIKE — pattern matching

SELECT * FROM users WHERE name LIKE :pattern

-- Patterns: 'Ra%' = starts with Ra, '%ul' = ends with ul, '%ahu%' = contains ahu

-- In Kotlin: dao.search("%${searchTerm}%")

  

-- CASE insensitive search (SQLite LIKE is case-insensitive by default for ASCII)

SELECT * FROM users WHERE lower(name) LIKE lower(:search)

```

  

### ORDER BY

  

```sql

-- Single column ascending

SELECT * FROM users ORDER BY name ASC

  

-- Single column descending (newest first)

SELECT * FROM users ORDER BY created_at DESC

  

-- Multiple columns (sort by city, then within each city sort by name)

SELECT * FROM users ORDER BY city ASC, name ASC

  

-- Nulls last (SQLite puts NULLs first by default)

SELECT * FROM users ORDER BY nickname ASC NULLS LAST

```

  

### LIMIT and OFFSET (Pagination)

  

```sql

-- First 20 rows

SELECT * FROM users LIMIT 20

  

-- Skip first 20, get next 20 (page 2)

SELECT * FROM users LIMIT 20 OFFSET 20

  

-- Page formula:

-- Page 1: LIMIT 20 OFFSET 0

-- Page 2: LIMIT 20 OFFSET 20

-- Page N: LIMIT 20 OFFSET ((N-1) * 20)

```

  

In Room:

```kotlin

@Query("SELECT * FROM users ORDER BY name ASC LIMIT :limit OFFSET :offset")

suspend fun getUsersPaged(limit: Int, offset: Int): List<UserEntity>

```

  

### Aggregate Functions

  

These compute a single value from multiple rows:

  

```sql

SELECT COUNT(*) FROM users                     -- count all rows

SELECT COUNT(nickname) FROM users              -- count non-NULL values only

SELECT COUNT(DISTINCT city) FROM users         -- count unique cities

  

SELECT AVG(age) FROM users                     -- arithmetic mean

SELECT MAX(age) FROM users                     -- highest value

SELECT MIN(age) FROM users                     -- lowest value

SELECT SUM(balance) FROM accounts              -- total sum

SELECT TOTAL(balance) FROM accounts            -- like SUM but returns 0.0 if no rows (not NULL)

  

-- Multiple aggregates in one query

SELECT COUNT(*) as total, AVG(age) as avgAge, MAX(age) as maxAge FROM users

```

  

In Room:

```kotlin

data class UserStats(val total: Int, val avgAge: Double, val maxAge: Int)

  

@Query("SELECT COUNT(*) as total, AVG(age) as avgAge, MAX(age) as maxAge FROM users")

suspend fun getUserStats(): UserStats

```

  

### GROUP BY and HAVING

  

GROUP BY collapses multiple rows into groups and lets you aggregate per group:

  

```sql

-- Count users per city

SELECT city, COUNT(*) as user_count

FROM users

GROUP BY city

  

-- Average age per department

SELECT department, AVG(age) as avg_age, COUNT(*) as employee_count

FROM employees

GROUP BY department

  

-- HAVING: filter GROUPS (not individual rows — that's WHERE)

-- Only show cities with more than 5 users

SELECT city, COUNT(*) as user_count

FROM users

GROUP BY city

HAVING user_count > 5

  

-- Combine WHERE (filter rows) and HAVING (filter groups)

SELECT city, COUNT(*) as user_count

FROM users

WHERE age > 18                    -- filter: only adults

GROUP BY city                     -- group by city

HAVING user_count >= 10           -- only cities with 10+ adults

ORDER BY user_count DESC

```

  

```kotlin

data class CityUserCount(val city: String, val userCount: Int)

  

@Query("""

    SELECT city, COUNT(*) as userCount 

    FROM users 

    GROUP BY city 

    HAVING userCount >= :minUsers

    ORDER BY userCount DESC

""")

suspend fun getCitiesWithMinUsers(minUsers: Int): List<CityUserCount>

```

  

### JOINs — Theory First

  

A JOIN combines rows from two tables based on a matching condition.

  

```

TABLE: users          TABLE: posts

┌────┬──────┐         ┌────┬─────────┬────────────┐

│ id │ name │         │ id │ user_id │ title      │

├────┼──────┤         ├────┼─────────┼────────────┤

│  1 │ Rahul│         │  1 │    1   │ Post by R  │

│  2 │ Priya│         │  2 │    1   │ Post 2 R   │

│  3 │ Arjun│         │  3 │    2   │ Post by P  │

└────┴──────┘         └────┴─────────┴────────────┘

  

INNER JOIN result (only matching rows):

┌─────────┬──────────────┬──────────┐

│ user.id │ user.name    │ post title│

├─────────┼──────────────┼──────────┤

│    1    │ Rahul        │ Post by R │

│    1    │ Rahul        │ Post 2 R  │

│    2    │ Priya        │ Post by P │

└─────────┴──────────────┴──────────┘

Note: Arjun (id=3) is excluded — he has no posts.

  

LEFT JOIN result (all users, posts or null):

┌─────────┬──────────────┬──────────────┐

│ user.id │ user.name    │ post title   │

├─────────┼──────────────┼──────────────┤

│    1    │ Rahul        │ Post by R    │

│    1    │ Rahul        │ Post 2 R     │

│    2    │ Priya        │ Post by P    │

│    3    │ Arjun        │ NULL         │

└─────────┴──────────────┴──────────────┘

Note: Arjun IS included, with NULL for post.

```

  

```sql

-- INNER JOIN: only rows with a match in BOTH tables

SELECT users.name, posts.title

FROM posts

INNER JOIN users ON posts.user_id = users.id

  

-- LEFT JOIN: all from left table, matching from right (or NULL)

SELECT users.name, posts.title

FROM users

LEFT JOIN posts ON posts.user_id = users.id

  

-- Multiple JOINs

SELECT users.name, posts.title, comments.body

FROM comments

INNER JOIN posts ON comments.post_id = posts.id

INNER JOIN users ON posts.user_id = users.id

WHERE users.id = :userId

ORDER BY comments.created_at DESC

```

  

### Subqueries

  

```sql

-- Users who have at least one post

SELECT * FROM users

WHERE id IN (SELECT DISTINCT user_id FROM posts)

  

-- Users who have never posted

SELECT * FROM users

WHERE id NOT IN (SELECT user_id FROM posts WHERE user_id IS NOT NULL)

  

-- Users whose age is above the average

SELECT * FROM users

WHERE age > (SELECT AVG(age) FROM users)

  

-- Correlated subquery: for each user, count their posts

SELECT u.*, (SELECT COUNT(*) FROM posts WHERE posts.user_id = u.id) as post_count

FROM users u

```

  

### Multi-line Queries in Room

  

```kotlin

@Query("""

    SELECT u.id as userId, 

           u.name as userName,

           COUNT(p.id) as postCount,

           MAX(p.created_at) as lastPostAt

    FROM users u

    LEFT JOIN posts p ON p.user_id = u.id

    WHERE u.is_active = 1

    GROUP BY u.id

    ORDER BY postCount DESC

    LIMIT :limit

""")

suspend fun getActiveUsersWithPostStats(limit: Int): List<UserPostStats>

```

  

---

  

---

  

# 9. @Database — The Glue Class

  

## What @Database Does Internally

  

When KSP processes your `@Database` class:

1. Validates all entity classes listed in `entities = [...]`

2. Validates all DAO classes (checks their queries against the entities)

3. Generates `AppDatabase_Impl.kt` which creates and manages the actual SQLite database

4. Generates schema JSON files (if `exportSchema = true`)

  

## Complete @Database Example

  

```kotlin

@Database(

    entities = [

        UserEntity::class,

        PostEntity::class,

        CommentEntity::class

    ],

    version = 3,

    exportSchema = true,

    autoMigrations = [

        AutoMigration(from = 1, to = 2),

        AutoMigration(from = 2, to = 3, spec = AppDatabase.Migration2To3::class)

    ]

)

@TypeConverters(Converters::class)     // register type converters here

abstract class AppDatabase : RoomDatabase() {

  

    // One abstract function per DAO

    abstract fun userDao(): UserDao

    abstract fun postDao(): PostDao

    abstract fun commentDao(): CommentDao

  

    // Auto-migration spec for complex changes

    @DeleteColumn(tableName = "users", columnName = "old_field")

    class Migration2To3 : AutoMigrationSpec

  

    companion object {

        // Singleton pattern (manual, without DI)

        @Volatile

        private var INSTANCE: AppDatabase? = null

  

        fun getDatabase(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(

                    context.applicationContext,

                    AppDatabase::class.java,

                    "app_database"

                ).build()

                INSTANCE = instance

                instance

            }

        }

    }

}

```

  

## Room.databaseBuilder Options

  

```kotlin

Room.databaseBuilder(context, AppDatabase::class.java, "app_database")

  

    // Migrations

    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)

    // If version mismatch and no migration: wipe database (DEV ONLY)

    .fallbackToDestructiveMigration()

    // Wipe only when downgrading version (rare)

    .fallbackToDestructiveMigrationOnDowngrade()

    // Pre-populate from an existing database file in assets/

    .createFromAsset("prepopulated.db")

    // Pre-populate from a file path

    .createFromFile(File(context.filesDir, "seed.db"))

    // Write-ahead logging (default on API 16+, better performance)

    .enableMultiInstanceInvalidation()

    // Allow queries on main thread (NEVER in production, only for testing)

    .allowMainThreadQueries()

  

    .build()

```

  

## exportSchema and the JSON Files

  

When `exportSchema = true`, Room generates a JSON file after every build:

  

```

app/schemas/com.yourapp.AppDatabase/

├── 1.json    ← schema when version was 1

├── 2.json    ← schema when version was 2

└── 3.json    ← schema when version is 3 (current)

```

  

The JSON file looks like:

```json

{

  "formatVersion": 1,

  "database": {

    "version": 1,

    "entities": [

      {

        "tableName": "users",

        "createSql": "CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)",

        "fields": [ ... ]

      }

    ]

  }

}

```

  

****Why commit these to git:****

- Auto-migrations compare JSON files to know what changed

- You can see exactly what your DB looked like at any version

- Code review can catch unintended schema changes

- Room validates at build time that your entities match the schema file

  

---

  

---

  

# 10. Type Converters — Deep Dive

  

## Why Type Converters Exist

  

SQLite has 5 types. Kotlin has thousands. Whenever you use a type that isn't one of the 5, Room has no idea how to store or read it. `@TypeConverter` tells Room: "here's how to convert this Kotlin type to a SQLite type, and back."

  

## Complete Converters Reference

  

```kotlin

class Converters {

  

    // ── Date / Time ──────────────────────────────────────────────

    @TypeConverter

    fun longToDate(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter

    fun dateToLong(date: Date?): Long? = date?.time

  

    // LocalDate (Java 8 / Android API 26+)

    @TypeConverter

    fun stringToLocalDate(value: String?): LocalDate? =

        value?.let { LocalDate.parse(it) }

    @TypeConverter

    fun localDateToString(date: LocalDate?): String? = date?.toString()

  

    // ── Lists ─────────────────────────────────────────────────────

    @TypeConverter

    fun stringListToString(list: List<String>?): String? =

        list?.joinToString(separator = "|||")  // use separator that won't appear in values

    @TypeConverter

    fun stringToStringList(value: String?): List<String>? =

        value?.split("|||")?.filter { it.isNotEmpty() }

  

    // List<Int> — as JSON

    @TypeConverter

    fun intListToJson(list: List<Int>?): String? =

        if (list == null) null else Gson().toJson(list)

    @TypeConverter

    fun jsonToIntList(json: String?): List<Int>? =

        if (json == null) null else Gson().fromJson(json, object : TypeToken<List<Int>>() {}.type)

  

    // ── Enums ─────────────────────────────────────────────────────

    @TypeConverter

    fun userStatusToString(status: UserStatus?): String? = status?.name

    @TypeConverter

    fun stringToUserStatus(value: String?): UserStatus? =

        value?.let { runCatching { UserStatus.valueOf(it) }.getOrNull() }

    // runCatching handles the case where an old enum value no longer exists

  

    // ── Bitmap ────────────────────────────────────────────────────

    // (Usually you store image URLs, not Bitmaps. But for small thumbnails:)

    @TypeConverter

    fun bitmapToByteArray(bitmap: Bitmap?): ByteArray? {

        if (bitmap == null) return null

        val stream = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)

        return stream.toByteArray()

    }

    @TypeConverter

    fun byteArrayToBitmap(bytes: ByteArray?): Bitmap? =

        bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }

}

```

  

## Registering at Different Levels

  

```kotlin

// Level 1: @Database — applies to ALL entities in this database (most common)

@Database(entities = [...], version = 1)

@TypeConverters(Converters::class)

abstract class AppDatabase : RoomDatabase() { ... }

  

// Level 2: @Entity class — applies only to this entity

@TypeConverters(DateConverters::class)

@Entity

data class EventEntity(...)

  

// Level 3: Field — applies only to this specific field

@Entity

data class UserEntity(

    @TypeConverters(StatusConverter::class)

    val status: UserStatus

)

```

  

****Use Level 1 (database-level) unless you have a specific reason to be more granular.**** It's simpler and avoids confusion.

  

---

  

---

  

# 11. Relationships — Theory + All Types

  

## The Theory: Room Is NOT an ORM

  

An ORM (Object-Relational Mapper) like Hibernate automatically traverses relationships — you load a `User` and its `posts` are automatically loaded too (sometimes lazily, sometimes eagerly).

  

Room deliberately chose NOT to do this. The reason: automatic lazy-loading causes invisible N+1 query problems (loading 100 users triggers 100 separate queries for posts).

  

Room's approach: ****you explicitly define relationship queries****. You know exactly how many SQL queries run.

  

## Understanding @Relation

  

```kotlin

data class UserWithPosts(

    @Embedded val user: UserEntity,           // SELECT * FROM users

    @Relation(

        parentColumn = "id",                  // UserEntity.id

        entityColumn = "userId"               // PostEntity.userId

    )

    val posts: List<PostEntity>               // SELECT * FROM posts WHERE userId IN (...)

)

```

  

When Room sees this, it generates TWO queries:

1. `SELECT * FROM users` (or whatever your @Query says)

2. `SELECT * FROM posts WHERE userId IN (1, 2, 3, ...)` (for all loaded user IDs at once)

  

It does NOT do N+1 (one query per user). It loads all posts in one batched query.

  

## One-to-One Relationship

  

```kotlin

// Setup: Each UserEntity has exactly one ProfileEntity

@Entity(tableName = "profiles")

data class ProfileEntity(

    @PrimaryKey val userId: Int,          // same as UserEntity.id (no autoGenerate!)

    val bio: String,

    val avatarUrl: String,

    val websiteUrl: String?

)

  

// Result class — NOT an @Entity

data class UserWithProfile(

    @Embedded val user: UserEntity,

    @Relation(

        parentColumn = "id",

        entityColumn = "userId"

    )

    val profile: ProfileEntity?           // nullable in case profile doesn't exist yet

)

  

// DAO

@Dao

interface UserDao {

    @Transaction

    @Query("SELECT * FROM users WHERE id = :userId")

    suspend fun getUserWithProfile(userId: Int): UserWithProfile?

    @Transaction

    @Query("SELECT * FROM users")

    fun getAllUsersWithProfiles(): Flow<List<UserWithProfile>>

}

```

  

## One-to-Many Relationship

  

```kotlin

// Setup: One user has many posts

@Entity(

    tableName = "posts",

    foreignKeys = [ForeignKey(

        entity = UserEntity::class,

        parentColumns = ["id"],

        childColumns = ["user_id"],

        onDelete = ForeignKey.CASCADE

    )],

    indices = [Index("user_id")]

)

data class PostEntity(

    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    @ColumnInfo(name = "user_id") val userId: Int,

    val title: String,

    val body: String,

    val createdAt: Long = System.currentTimeMillis()

)

  

// Result class

data class UserWithPosts(

    @Embedded val user: UserEntity,

    @Relation(

        parentColumn = "id",

        entityColumn = "user_id"    // must match @ColumnInfo name, not Kotlin field name

    )

    val posts: List<PostEntity>

)

  

// DAO

@Dao

interface UserDao {

    @Transaction

    @Query("SELECT * FROM users")

    fun getAllUsersWithPosts(): Flow<List<UserWithPosts>>

  

    @Transaction

    @Query("SELECT * FROM users WHERE id = :userId")

    suspend fun getUserWithPosts(userId: Int): UserWithPosts?

}

```

  

## Many-to-Many Relationship

  

```kotlin

// Books can have multiple authors; Authors can write multiple books

  

@Entity(tableName = "books")

data class BookEntity(

    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val title: String,

    val publishedYear: Int

)

  

@Entity(tableName = "authors")

data class AuthorEntity(

    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val name: String

)

  

// Junction table (cross-reference)

@Entity(

    tableName = "book_author_cross_ref",

    primaryKeys = ["book_id", "author_id"]   // composite key prevents duplicates

)

data class BookAuthorCrossRef(

    @ColumnInfo(name = "book_id") val bookId: Int,

    @ColumnInfo(name = "author_id") val authorId: Int

)

  

// Result classes

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

  

data class AuthorWithBooks(

    @Embedded val author: AuthorEntity,

    @Relation(

        parentColumn = "id",

        entityColumn = "id",

        associateBy = Junction(

            value = BookAuthorCrossRef::class,

            parentColumn = "author_id",

            entityColumn = "book_id"

        )

    )

    val books: List<BookEntity>

)

  

// DAO

@Dao

interface BookDao {

    @Insert suspend fun insertBook(book: BookEntity): Long

    @Insert suspend fun insertAuthor(author: AuthorEntity): Long

    @Insert suspend fun insertCrossRef(crossRef: BookAuthorCrossRef)

  

    @Transaction

    @Query("SELECT * FROM books")

    fun getAllBooksWithAuthors(): Flow<List<BookWithAuthors>>

  

    @Transaction

    @Query("SELECT * FROM authors WHERE id = :authorId")

    suspend fun getAuthorWithBooks(authorId: Int): AuthorWithBooks?

}

```

  

## Nested Relationships (Three Levels Deep)

  

```kotlin

// User → Posts → Comments (three levels)

data class PostWithComments(

    @Embedded val post: PostEntity,

    @Relation(

        parentColumn = "id",

        entityColumn = "post_id"

    )

    val comments: List<CommentEntity>

)

  

data class UserWithPostsAndComments(

    @Embedded val user: UserEntity,

    @Relation(

        parentColumn = "id",

        entityColumn = "user_id",

        entity = PostEntity::class  // specify the entity class explicitly for nested @Relation

    )

    val posts: List<PostWithComments>

)

  

// DAO — MUST have @Transaction

@Transaction

@Query("SELECT * FROM users")

fun getAllUsersWithPostsAndComments(): Flow<List<UserWithPostsAndComments>>

```

  

---

  

---

  

# 12. Flow + Coroutines — How They Work Together

  

## The Problem Flow Solves

  

Without Flow, you'd have to manually refresh:

```

User opens app → fetch data → show data

User adds a note → insert to DB → manually call fetch again → show new data

User deletes a note → delete from DB → manually call fetch again → show updated data

```

  

With Flow:

```

User opens app → start collecting Flow → data shows

User adds a note → insert to DB → Flow automatically emits new list → UI updates

User deletes → Flow automatically emits updated list → UI updates

```

  

## How Room's Flow Works Under the Hood

  

```

1. You call userDao.getAllUsers() which returns Flow<List<UserEntity>>

2. Room creates an InvalidationTracker for the "users" table

3. When the coroutine collects the Flow:

   a. Room runs the SQL query

   b. Room emits the result

   c. Room registers an observer on the "users" table

4. When ANY write happens to the "users" table (insert/update/delete):

   a. SQLite notifies Room's InvalidationTracker

   b. Room re-runs the SQL query automatically

   c. Room emits the new result through the Flow

5. The Flow never completes by itself — it keeps observing

   until the collecting coroutine is cancelled (e.g., ViewModel is cleared)

```

  

## DAO Return Types Summary

  

```kotlin

// One-shot reads

@Query("SELECT * FROM users WHERE id = :id")

suspend fun getUserById(id: Int): UserEntity?        // null if not found

  

@Query("SELECT * FROM users")

suspend fun getAllUsersOnce(): List<UserEntity>       // empty list if none

  

@Query("SELECT COUNT(*) FROM users")

suspend fun getUserCount(): Int

  

// Reactive reads (auto-update)

@Query("SELECT * FROM users")

fun getAllUsers(): Flow<List<UserEntity>>             // emits on every change

  

@Query("SELECT * FROM users WHERE id = :id")

fun getUserFlow(id: Int): Flow<UserEntity?>          // emits null if deleted

  

// Write operations

@Insert

suspend fun insertUser(user: UserEntity): Long       // returns new ID

  

@Update

suspend fun updateUser(user: UserEntity): Int        // returns rows updated

  

@Delete

suspend fun deleteUser(user: UserEntity): Int        // returns rows deleted

  

@Query("DELETE FROM users")

suspend fun deleteAll()                              // no return value

```

  

## ViewModel Pattern with Flow

  

```kotlin

@HiltViewModel

class UserViewModel @Inject constructor(

    private val userRepository: UserRepository

) : ViewModel() {

  

    // Convert Flow from repository to StateFlow for Compose

    val users: StateFlow<List<User>> = userRepository.getAllUsers()

        .stateIn(

            scope = viewModelScope,

            started = SharingStarted.WhileSubscribed(5000L),

            // WhileSubscribed(5000): keeps Flow active for 5 seconds after

            // last subscriber leaves (handles screen rotation gracefully)

            initialValue = emptyList()

        )

  

    // One-shot operations triggered by user action

    fun addUser(name: String, email: String) {

        viewModelScope.launch {

            userRepository.insertUser(User(name = name, email = email))

            // After insert, users StateFlow automatically updates!

            // No need to call getAllUsers() again.

        }

    }

  

    fun deleteUser(user: User) {

        viewModelScope.launch {

            userRepository.deleteUser(user)

        }

    }

}

```

  

## Collecting in Compose

  

```kotlin

@Composable

fun UserListScreen(viewModel: UserViewModel = hiltViewModel()) {

    val users by viewModel.users.collectAsStateWithLifecycle()

    // collectAsStateWithLifecycle is better than collectAsState

    // It stops collecting when the app is in background (saves battery)

    LazyColumn {

        items(users) { user ->

            UserCard(user = user)

        }

    }

}

```

  

---

  

---

  

# 13. Database Migrations — Theory + All Cases

  

## Why Migration Exists

  

Your database is a file on the user's device. When they install an update:

- The `.db` file remains from the old version

- Your new app code expects a different schema

- Room compares the `version` number in your `@Database` with the version stored in the database file

  

```

Scenario: User has your app v1.0, they update to v1.1

DB file on device: version=1 (has columns: id, name, email)

Your new code: version=2 (expects columns: id, name, email, age)

  

Room sees: code version (2) ≠ database version (1)

Room needs: a migration to bring version 1 → version 2

```

  

Without migration:

```

Option A: App crashes with IllegalStateException (if no migration provided)

Option B: Database wiped and recreated (fallbackToDestructiveMigration) — user loses ALL data

```

  

## Manual Migration — Every SQL Case

  

```kotlin

// ── ADDING A COLUMN ─────────────────────────────────────────────

val MIGRATION_1_2 = object : Migration(1, 2) {

    override fun migrate(database: SupportSQLiteDatabase) {

        // NOT NULL column needs a DEFAULT value — you can't add NOT NULL without default

        database.execSQL("ALTER TABLE users ADD COLUMN age INTEGER NOT NULL DEFAULT 0")

        // Nullable column — no default needed

        database.execSQL("ALTER TABLE users ADD COLUMN nickname TEXT")

    }

}

  

// ── ADDING A NEW TABLE ──────────────────────────────────────────

val MIGRATION_2_3 = object : Migration(2, 3) {

    override fun migrate(database: SupportSQLiteDatabase) {

        database.execSQL("""

            CREATE TABLE IF NOT EXISTS posts (

                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,

                user_id INTEGER NOT NULL,

                title TEXT NOT NULL,

                body TEXT NOT NULL,

                created_at INTEGER NOT NULL,

                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE

            )

        """)

        database.execSQL("CREATE INDEX IF NOT EXISTS index_posts_user_id ON posts(user_id)")

    }

}

  

// ── DROPPING A TABLE ────────────────────────────────────────────

val MIGRATION_3_4 = object : Migration(3, 4) {

    override fun migrate(database: SupportSQLiteDatabase) {

        database.execSQL("DROP TABLE IF EXISTS old_table")

    }

}

  

// ── RENAMING A TABLE ────────────────────────────────────────────

val MIGRATION_4_5 = object : Migration(4, 5) {

    override fun migrate(database: SupportSQLiteDatabase) {

        database.execSQL("ALTER TABLE user RENAME TO users")

    }

}

  

// ── RENAMING A COLUMN (SQLite doesn't support ALTER COLUMN) ─────

// Must: create new table → copy data → drop old → rename new

val MIGRATION_5_6 = object : Migration(5, 6) {

    override fun migrate(database: SupportSQLiteDatabase) {

        // Create new table with correct column name

        database.execSQL("""

            CREATE TABLE users_new (

                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,

                full_name TEXT NOT NULL,   -- renamed from 'name'

                email TEXT NOT NULL,

                age INTEGER NOT NULL DEFAULT 0

            )

        """)

        // Copy data from old to new (mapping old column to new)

        database.execSQL("""

            INSERT INTO users_new (id, full_name, email, age)

            SELECT id, name, email, age FROM users

        """)

        // Drop the old table

        database.execSQL("DROP TABLE users")

        // Rename new table

        database.execSQL("ALTER TABLE users_new RENAME TO users")

    }

}

  

// ── CHANGING COLUMN TYPE (also requires table recreation) ───────

val MIGRATION_6_7 = object : Migration(6, 7) {

    override fun migrate(database: SupportSQLiteDatabase) {

        database.execSQL("CREATE TABLE users_new (...)")  // with new column type

        database.execSQL("INSERT INTO users_new SELECT id, CAST(age AS TEXT), ... FROM users")

        database.execSQL("DROP TABLE users")

        database.execSQL("ALTER TABLE users_new RENAME TO users")

    }

}

  

// Register in database builder:

Room.databaseBuilder(context, AppDatabase::class.java, "app_db")

    .addMigrations(

        MIGRATION_1_2,

        MIGRATION_2_3,

        MIGRATION_3_4,

        MIGRATION_4_5,

        MIGRATION_5_6,

        MIGRATION_6_7

    )

    .build()

```

  

## Auto-Migration (Room 2.4+)

  

Auto-migration works by comparing the OLD schema JSON file with the NEW schema JSON file and generating the migration SQL automatically.

  

```kotlin

@Database(

    entities = [UserEntity::class],

    version = 4,

    exportSchema = true,           // REQUIRED — must have old schema JSON files

    autoMigrations = [

        AutoMigration(from = 1, to = 2),    // added column with default → auto

        AutoMigration(from = 2, to = 3),    // added new table → auto

        AutoMigration(from = 3, to = 4, spec = AppDatabase.Migration3To4::class)

    ]

)

abstract class AppDatabase : RoomDatabase() {

  

    // For operations Room can't figure out alone, provide specs:

    @RenameTable(fromTableName = "user", toTableName = "users")

    class Migration3To4 : AutoMigrationSpec

    // Other spec annotations:

    // @DeleteTable(tableName = "old_table")

    // @RenameColumn(tableName = "users", fromColumnName = "name", toColumnName = "full_name")

    // @DeleteColumn(tableName = "users", columnName = "old_field")

    abstract fun userDao(): UserDao

}

```

  

****Auto-migration works for:****

- Adding a column (with `@ColumnInfo(defaultValue = "...")`)

- Adding a table

- Deleting a table (`@DeleteTable` spec)

- Renaming a table (`@RenameTable` spec)

- Deleting a column (`@DeleteColumn` spec)

- Renaming a column (`@RenameColumn` spec)

  

****Auto-migration does NOT work for:****

- Changing a column's type

- Complex data transformations

- Operations needing custom logic → use manual `Migration` object

  

---

  

---

  

# 14. Room + Hilt — Complete Production Setup

  

## Gradle (build.gradle.kts)

  

```kotlin

plugins {

    id("com.android.application")

    id("org.jetbrains.kotlin.android")

    id("com.google.devtools.ksp") version "2.0.0-1.0.21"

    id("com.google.dagger.hilt.android")

    id("androidx.room")

}

  

android {

    room {

        schemaDirectory("$projectDir/schemas")

    }

}

  

dependencies {

    val room_version = "2.8.4"

    val hilt_version = "2.52"

  

    // Room

    implementation("androidx.room:room-runtime:$room_version")

    implementation("androidx.room:room-ktx:$room_version")       // Flow, coroutines support

    ksp("androidx.room:room-compiler:$room_version")             // use ksp, never kapt

  

    // Hilt

    implementation("com.google.dagger:hilt-android:$hilt_version")

    ksp("com.google.dagger:hilt-compiler:$hilt_version")

  

    // Testing

    testImplementation("androidx.room:room-testing:$room_version")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")

}

```

  

## Entity, DAO, Database

  

```kotlin

// data/local/entity/UserEntity.kt

@Entity(tableName = "users")

data class UserEntity(

    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val name: String,

    val email: String,

    val createdAt: Long = System.currentTimeMillis()

)

  

// data/local/dao/UserDao.kt

@Dao

interface UserDao {

    @Query("SELECT * FROM users ORDER BY name ASC")

    fun getAllUsers(): Flow<List<UserEntity>>

  

    @Query("SELECT * FROM users WHERE id = :id")

    suspend fun getUserById(id: Int): UserEntity?

  

    @Insert(onConflict = OnConflictStrategy.REPLACE)

    suspend fun insertUser(user: UserEntity): Long

  

    @Update

    suspend fun updateUser(user: UserEntity)

  

    @Delete

    suspend fun deleteUser(user: UserEntity)

  

    @Query("DELETE FROM users")

    suspend fun deleteAll()

}

  

// data/local/AppDatabase.kt

@Database(entities = [UserEntity::class], version = 1, exportSchema = true)

@TypeConverters(Converters::class)

abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

}

```

  

## di/DatabaseModule.kt (Hilt)

  

```kotlin

@Module

@InstallIn(SingletonComponent::class)

object DatabaseModule {

  

    @Provides

    @Singleton

    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =

        Room.databaseBuilder(

            context.applicationContext,

            AppDatabase::class.java,

            "app_database"

        )

        .build()

  

    @Provides

    @Singleton                    // actually OK to @Singleton DAOs too — Room caches them anyway

    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

}

```

  

## RepositoryModule.kt (Hilt)

  

```kotlin

@Module

@InstallIn(SingletonComponent::class)

abstract class RepositoryModule {

    @Binds

    @Singleton

    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

}

```

  

---

  

---

  

# 15. Room + Koin — Complete Setup and Comparison

  

## What Koin Is (Quick Recap)

  

Koin is a lightweight dependency injection framework for Kotlin. Unlike Hilt which uses annotation processing (KSP) to generate code at build time, Koin uses Kotlin's DSL and resolves dependencies at ****runtime****.

  

```

Hilt: Generates code at BUILD time → compile errors if DI is wrong

Koin: Resolves dependencies at RUNTIME → errors appear when app starts (or when ViewModel is first created)

```

  

Both produce the same app architecture. Only the wiring is different.

  

## Gradle (build.gradle.kts with Koin)

  

```kotlin

plugins {

    id("com.android.application")

    id("org.jetbrains.kotlin.android")

    id("com.google.devtools.ksp") version "2.0.0-1.0.21"  // still need KSP for Room

    id("androidx.room")

    // NO hilt plugin!

}

  

android {

    room { schemaDirectory("$projectDir/schemas") }

}

  

dependencies {

    val room_version = "2.8.4"

    val koin_version = "3.5.6"

  

    // Room (same as with Hilt)

    implementation("androidx.room:room-runtime:$room_version")

    implementation("androidx.room:room-ktx:$room_version")

    ksp("androidx.room:room-compiler:$room_version")

  

    // Koin (instead of Hilt)

    implementation("io.insert-koin:koin-android:$koin_version")

    implementation("io.insert-koin:koin-androidx-compose:$koin_version")

}

```

  

## Application Class (Koin)

  

```kotlin

// MyApplication.kt

class MyApplication : Application() {

    override fun onCreate() {

        super.onCreate()

        startKoin {

            androidContext(this@MyApplication)

            androidLogger(Level.DEBUG)       // log DI resolution (use Level.NONE in release)

            modules(databaseModule, repositoryModule, viewModelModule)

        }

    }

}

```

  

```xml

<!-- AndroidManifest.xml -->

<application android:name=".MyApplication" ...>

```

  

## di/DatabaseModule.kt (Koin)

  

```kotlin

val databaseModule = module {

  

    // single { } = singleton — created once, reused for the app's lifetime

    single<AppDatabase> {

        Room.databaseBuilder(

            androidContext(),              // get() retrieves Context via Koin

            AppDatabase::class.java,

            "app_database"

        ).build()

    }

  

    // DAOs — get the database, call the DAO function

    single<UserDao> { get<AppDatabase>().userDao() }

    single<PostDao> { get<AppDatabase>().postDao() }

}

```

  

## di/RepositoryModule.kt (Koin)

  

```kotlin

val repositoryModule = module {

  

    // single<Interface> { Implementation(get(), get()) }

    // get() = "ask Koin to provide this dependency"

    single<UserRepository> {

        UserRepositoryImpl(

            userDao = get(),       // Koin provides UserDao from databaseModule

        )

    }

}

```

  

## di/ViewModelModule.kt (Koin)

  

```kotlin

val viewModelModule = module {

  

    // viewModel { } = creates new instance per ViewModel scope

    viewModel {

        UserViewModel(

            getUsersUseCase = get()

        )

    }

  

    // OR — Koin can auto-inject by type

    viewModel { UserViewModel(get()) }

}

```

  

## ViewModel (Koin)

  

```kotlin

// With Koin — NO @HiltViewModel, NO @Inject

class UserViewModel(

    private val getUsersUseCase: GetUsersUseCase

) : ViewModel() {

  

    val users: StateFlow<List<User>> = getUsersUseCase()

        .stateIn(

            scope = viewModelScope,

            started = SharingStarted.WhileSubscribed(5000L),

            initialValue = emptyList()

        )

}

```

  

## Composable (Koin)

  

```kotlin

@Composable

fun UserListScreen() {

    // Hilt: hiltViewModel()

    // Koin: koinViewModel()

    val viewModel: UserViewModel = koinViewModel()

    val users by viewModel.users.collectAsStateWithLifecycle()

    LazyColumn {

        items(users) { user -> UserCard(user) }

    }

}

```

  

## Koin Keywords Reference

  

| Keyword | What it creates | Lives as long as |

|---|---|---|

| `single { }` | Singleton — one instance | App lifetime |

| `factory { }` | New instance every time it's requested | Until no longer referenced |

| `viewModel { }` | ViewModel — Koin-managed | ViewModel scope |

| `scoped { }` | Lives within a Koin scope | Scope is closed |

| `get()` | Asks Koin to provide a dependency | — |

| `androidContext()` | Provides Android Context | — |

| `getKoin().get<T>()` | Manually retrieve from Koin | — |

  

## Hilt vs Koin — Full Comparison

  

| | Hilt | Koin |

|---|---|---|

| DI resolution time | Build time (compile-time safe) | Runtime |

| Error when wrong | Build fails | App crashes at runtime |

| Boilerplate | More (modules, @InstallIn, @Binds) | Less (one module block per concept) |

| Build speed impact | Adds KSP processing time | None |

| Kotlin Multiplatform | No (Android only) | Yes |

| Learning curve | Steeper | Gentler |

| Google recommendation | Yes — official Android recommendation | Not official, but widely used |

| Performance | Slightly faster at runtime (prebuilt) | Tiny overhead at startup (resolution) |

| Testing | @HiltAndroidTest | `KoinTestRule` |

| Community size | Very large | Large |

  

## When to Choose Koin

  

- Learning/personal projects — simpler setup, faster to get going

- Kotlin Multiplatform projects — Koin works on all platforms

- Smaller teams / solo developers — less boilerplate

- Backend/server Kotlin (Ktor) — Koin is popular there too

  

## When to Choose Hilt

  

- Production Android apps (especially at a company)

- Large teams — compile-time safety catches mistakes early

- Existing Dagger codebases — Hilt is Dagger with less boilerplate

- Interview / company standard — most Android job descriptions mention Hilt

  

---

  

---

  

# 16. How Room Works Internally — Step by Step

  

## Build Time (KSP Annotation Processing)

  

```

Step 1: You write your annotated classes

        @Entity UserEntity.kt

        @Dao UserDao.kt  

        @Database AppDatabase.kt

  

Step 2: Gradle runs KSP

        KSP plugin hooks into Kotlin compiler's parsing phase

        Reads all annotations in your source files

Step 3: Room's KSP processor validates everything:

        - Does every @Entity have a @PrimaryKey? (error if not)

        - Does every @Query SQL reference valid tables and columns?

        - Do return types match what the query returns?

        If anything is wrong → BUILD FAILS with clear error message

  

Step 4: Room generates implementation files:

        UserDao_Impl.kt       ← complete working DAO with all SQL

        AppDatabase_Impl.kt   ← database setup and DAO creation

        Placed in: build/generated/ksp/debug/kotlin/...

  

Step 5: Generated files are compiled along with your source

        Your app now has real working database code

```

  

## Runtime (App Running)

  

```

Step 6: App starts

        Room.databaseBuilder() is called (via Hilt or Koin)

        Creates an instance of AppDatabase_Impl

Step 7: First database operation triggers:

        AppDatabase_Impl opens the .db file (or creates it)

        Checks: file's version == @Database version?

        If version matches: proceed normally

        If version doesn't match: run migrations or crash/wipe

  

Step 8: You call userDao.getAllUsers() which returns Flow<List<UserEntity>>

  

Step 9: UserDao_Impl.getAllUsers() runs:

        val sql = "SELECT * FROM users ORDER BY name ASC"

        Room prepares the statement (uses a cache for performance)

Step 10: Flow is returned, but not yet executed

Step 11: Your coroutine collects the Flow:

         Room executes the SQL on a background dispatcher

         Gets back a Cursor (raw SQLite result pointer)

Step 12: Room reads the Cursor row by row:

         cursor.moveToFirst()

         Creates UserEntity: id = cursor.getInt(0), name = cursor.getString(1), ...

         Adds to result list

         cursor.close()

Step 13: Room registers an InvalidationTracker observer on "users" table

Step 14: Flow emits the List<UserEntity> to your collector

         (mapped to domain model in repository, displayed in UI)

  

Step 15: You call userDao.insertUser(user)

         SQL executes: INSERT INTO users (name, email...) VALUES (?, ?...)

         SQLite notifies InvalidationTracker: "users table changed"

Step 16: Room's InvalidationTracker fires

         Re-executes the SELECT query

         Flow emits the new List<UserEntity> automatically

         UI updates without any manual code

```

  

## The Generated Code — What It Looks Like

  

After building, you can find and read the generated files:

  

```kotlin

// UserDao_Impl.kt (simplified for readability):

  

public class UserDao_Impl(private val __db: RoomDatabase) : UserDao {

  

    override fun getAllUsers(): Flow<List<UserEntity>> {

        val _sql = "SELECT * FROM users ORDER BY name ASC"

        return flow {

            while (true) {

                __db.invalidationTracker.createFlow("users").collect {

                    val _stmt = __db.query(_sql)

                    try {

                        val result = mutableListOf<UserEntity>()

                        while (_stmt.moveToNext()) {

                            result.add(UserEntity(

                                id = _stmt.getInt(0),

                                name = _stmt.getString(1),

                                email = _stmt.getString(2),

                                createdAt = _stmt.getLong(3)

                            ))

                        }

                        emit(result)

                    } finally {

                        _stmt.close()

                    }

                }

            }

        }

    }

  

    override suspend fun insertUser(user: UserEntity): Long {

        val _sql = "INSERT OR REPLACE INTO users (name, email, created_at) VALUES (?, ?, ?)"

        return __db.withTransaction {

            val _stmt = __db.compileStatement(_sql)

            _stmt.bindString(1, user.name)

            _stmt.bindString(2, user.email)

            _stmt.bindLong(3, user.createdAt)

            _stmt.executeInsert()

        }

    }

}

```

  

This is what you would have to write yourself without Room. Room writes it for you, perfectly, every time.

  

---

  

---

  

# 17. Testing Room Database

  

## Why In-Memory Database for Tests

  

Real database files persist between test runs. Test B might fail because Test A left dirty data. An in-memory database:

- Is created fresh for each test

- Lives only in RAM — disappears when the test finishes

- Runs faster (no disk I/O)

- Tests are isolated from each other

  

## Setup — DAO Test

  

```kotlin

// androidTest/java/com/yourapp/data/local/dao/UserDaoTest.kt

@RunWith(AndroidJUnit4::class)           // requires device or emulator

@SmallTest                               // marks as small/unit test

class UserDaoTest {

  

    private lateinit var database: AppDatabase

    private lateinit var userDao: UserDao

  

    @Before

    fun setupDatabase() {

        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(

            context,

            AppDatabase::class.java

        )

        .allowMainThreadQueries()        // OK for tests only — not production!

        .build()

        userDao = database.userDao()

    }

  

    @After

    fun closeDatabase() {

        database.close()

    }

  

    @Test

    fun insertUser_and_retrieveById() = runTest {

        // Given

        val user = UserEntity(name = "Rahul", email = "rahul@test.com")

  

        // When

        val id = userDao.insertUser(user)

  

        // Then

        val retrieved = userDao.getUserById(id.toInt())

        assertThat(retrieved).isNotNull()

        assertThat(retrieved?.name).isEqualTo("Rahul")

        assertThat(retrieved?.email).isEqualTo("rahul@test.com")

    }

  

    @Test

    fun insertUser_withDuplicateId_replaces() = runTest {

        val user1 = UserEntity(id = 1, name = "Rahul", email = "rahul@test.com")

        val user2 = UserEntity(id = 1, name = "NewName", email = "new@test.com")

  

        userDao.insertUser(user1)

        userDao.insertUser(user2)  // @Insert(onConflict = REPLACE)

  

        val result = userDao.getUserById(1)

        assertThat(result?.name).isEqualTo("NewName")

    }

  

    @Test

    fun deleteUser_removesFromDb() = runTest {

        val user = UserEntity(name = "Rahul", email = "rahul@test.com")

        val id = userDao.insertUser(user)

        val inserted = userDao.getUserById(id.toInt())!!

  

        userDao.deleteUser(inserted)

  

        val result = userDao.getUserById(id.toInt())

        assertThat(result).isNull()

    }

  

    @Test

    fun getAllUsers_returnsAllInserted() = runTest {

        userDao.insertUser(UserEntity(name = "Rahul", email = "a@a.com"))

        userDao.insertUser(UserEntity(name = "Priya", email = "b@b.com"))

        userDao.insertUser(UserEntity(name = "Arjun", email = "c@c.com"))

  

        val users = userDao.getAllUsersOnce()  // suspend fun returning List

        assertThat(users).hasSize(3)

    }

  

    @Test

    fun getAllUsersFlow_emitsUpdatedList() = runTest {

        // Collect from Flow

        val results = mutableListOf<List<UserEntity>>()

        val job = launch {

            userDao.getAllUsers().collect { results.add(it) }

        }

  

        // Insert some users

        userDao.insertUser(UserEntity(name = "Rahul", email = "a@a.com"))

        userDao.insertUser(UserEntity(name = "Priya", email = "b@b.com"))

  

        // Let the coroutines run

        advanceUntilIdle()

        job.cancel()

  

        // Flow should have emitted at least the final state

        assertThat(results.last()).hasSize(2)

    }

}

```

  

## Test Dependencies

  

```kotlin

// build.gradle.kts

dependencies {

    // Room testing

    testImplementation("androidx.room:room-testing:$room_version")

    // Instrumentation tests

    androidTestImplementation("androidx.test.ext:junit:1.1.5")

    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    androidTestImplementation("com.google.truth:truth:1.1.5")  // assertThat().isEqualTo()

    // Coroutines testing

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

}

```

  

## Migration Testing

  

```kotlin

@RunWith(AndroidJUnit4::class)

class MigrationTest {

  

    @get:Rule

    val migrationTestHelper = MigrationTestHelper(

        InstrumentationRegistry.getInstrumentation(),

        AppDatabase::class.java

    )

  

    @Test

    fun migrate1To2() {

        // Create database at version 1

        val db = migrationTestHelper.createDatabase("test_db", 1)

        // Insert data in old schema

        db.execSQL("INSERT INTO users (name, email) VALUES ('Rahul', 'rahul@test.com')")

        db.close()

        // Run migration

        migrationTestHelper.runMigrationsAndValidate(

            "test_db",

            2,

            true,           // validate schema after migration

            MIGRATION_1_2

        )

        // If validation passes, migration is correct

    }

}

```

  

---

  

---

  

# 18. Database Inspector (Android Studio Tool)

  

Android Studio 4.1+ includes the ****Database Inspector**** — a live tool to inspect your Room database while your app is running on a device or emulator.

  

## How to Open It

  

```

Android Studio → View → Tool Windows → App Inspection

→ Select your running app/process

→ Click "Database Inspector" tab

```

  

## What You Can Do

  

```

1. Browse tables

   → Click any table to see all rows

   → See live data updating in real-time as your app runs

  

2. Run custom SQL queries

   → Enter any SELECT, UPDATE, DELETE

   → Results show immediately

3. Gutter actions

   → In your DAO interface, click the run icon next to @Query

   → Room runs that exact query on your live database

   → Results show in Database Inspector

4. Export database

   → Download the .db file to your computer

   → Open with any SQLite browser (DB Browser for SQLite, DBeaver, etc.)

  

5. Forcefully refresh

   → "Refresh" button re-runs the current query

```

  

## When to Use It

  

- ****Debugging:**** "Why is this query returning nothing?" → Check the table in Inspector

- ****Verifying inserts:**** "Did my insert actually work?" → Open the table and look

- ****Testing queries:**** Write complex SQL in Inspector before putting in `@Query`

- ****Schema inspection:**** Verify column names, types, and structure are what you expect

  

---

  

---

  

# 19. Performance Tips

  

## Use Flow for Lists That Change

  

```kotlin

// BAD: one-shot query in a loop

fun refreshData() {

    viewModelScope.launch {

        val users = userDao.getAllUsersOnce()  // gets data ONCE

        _uiState.update { it.copy(users = users) }

    }

    // Every change requires manually calling refreshData() again

}

  

// GOOD: reactive Flow

val users = userDao.getAllUsers()  // automatically updates on every DB change

    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

```

  

## Index Foreign Key Columns

  

```kotlin

// ALWAYS add an index to foreign key columns

@Entity(

    foreignKeys = [ForeignKey(entity = UserEntity::class, ...)],

    indices = [Index("user_id")]   // without this, every query joining users → posts is slow

)

```

  

## Select Only What You Need

  

```kotlin

// BAD: fetches all columns even if you only show name and email

@Query("SELECT * FROM users")

fun getAllUsers(): Flow<List<UserEntity>>

  

// GOOD: only fetch what the UI needs (faster, less memory)

@Query("SELECT id, name, email FROM users")

fun getUserSummaries(): Flow<List<UserSummary>>

// data class UserSummary(val id: Int, val name: String, val email: String)

```

  

## Batch Inserts in Transactions

  

```kotlin

// BAD: separate transaction per insert (100 inserts = 100 transactions = slow)

users.forEach { user -> userDao.insertUser(user) }

  

// GOOD: one transaction for all inserts (100 inserts = 1 transaction = fast)

@Insert

suspend fun insertUsers(users: List<UserEntity>)

// or explicitly:

db.withTransaction {

    users.forEach { user -> userDao.insertUser(user) }

}

```

  

## Avoid N+1 Queries

  

```kotlin

// BAD: N+1 — one query per user to get their posts

val users = userDao.getAllUsersOnce()

users.map { user ->

    val posts = postDao.getPostsByUserId(user.id)  // N queries for N users!

    UserWithPosts(user, posts)

}

  

// GOOD: use @Relation — Room batches into 2 queries total

@Transaction

@Query("SELECT * FROM users")

fun getAllUsersWithPosts(): Flow<List<UserWithPosts>>

```

  

---

  

---

  

# 20. Common Mistakes — with Why They Happen

  

| Mistake | Why It Happens | Fix |

|---|---|---|

| Not incrementing `version` after schema change | Forgot it's required | Build will succeed but app crashes on update. Add lint rule or always version bump with entity change |

| No migration for version change | Didn't know migrations were needed | App crashes with `IllegalStateException`. Always add migration. Use `fallbackToDestructiveMigration()` during dev only |

| Multiple AppDatabase instances | Created in Activity or ViewModel instead of Singleton | Memory waste, potential data corruption. Use `@Singleton` in Hilt/Koin |

| Querying on main thread | Forgot to use `suspend` or `Flow` | App crashes with `IllegalStateException`. All DAO functions must be `suspend` or return `Flow` |

| `@Relation` without `@Transaction` | Didn't know it's required | Possible inconsistent data (race condition between two queries). Always pair `@Relation` with `@Transaction` |

| Storing `List<T>` directly in entity field | Expected Room to handle it | Compile error — Room doesn't know how to serialize lists. Use `@TypeConverter` or a separate entity |

| Using `@Entity` class in ViewModel | Didn't separate concerns | Breaks clean architecture. Map to domain model in repository layer |

| `exportSchema = false` | Suppressed the warning without understanding | Loses migration history, auto-migration can't work. Always set `true` and commit schema JSON to git |

| Wrong `onConflict` strategy | Used default `ABORT` | App crashes on duplicate insert. Always explicitly choose `REPLACE`, `IGNORE`, or handle `ABORT` |

| Foreign key without index | Didn't know to add it | JOIN queries are very slow. Always add `Index("foreign_key_col")` |

| `@Insert(onConflict = REPLACE)` for upsert on entities with foreign key children | Didn't know REPLACE = delete+insert | Child rows (posts for a user) may be deleted. Use `@Upsert` instead |

| Missing entity in `@Database(entities=[...])` | Added entity but forgot to register | Runtime crash when trying to use that table. Room generates error: "no table found" |

  

---

  

---

  

# 21. Interview Questions — Deep Answers

  

****Q: What is Room and why do we use it instead of SQLite directly?****

  

Room is an abstraction layer over SQLite that adds three major improvements: compile-time SQL validation (typos in @Query cause build failures, not runtime crashes), automatic boilerplate elimination (Room generates all cursor management, column reading, and object creation code), and native Kotlin coroutines/Flow integration (queries and inserts work naturally as suspend functions and reactive streams). Direct SQLite requires 50+ lines per query and has no compile-time safety.

  

---

  

****Q: Explain the 3 Room components.****

  

`@Entity` is a data class that represents a database table. Each field becomes a column. `@Dao` is an interface where you define queries using annotations like `@Query`, `@Insert`, `@Update`, `@Delete`. Room generates the implementation at compile time. `@Database` is an abstract class that extends `RoomDatabase`, lists all entities, version number, and returns abstract DAO instances. Room generates the concrete implementation.

  

---

  

****Q: What is KSP and why should you use it over KAPT for Room?****

  

KAPT (Kotlin Annotation Processing Tool) generates Java stubs from Kotlin code so Java annotation processors can read them — this stub generation adds 30-50% to build time and loses some Kotlin type information like suspend function signatures. KSP (Kotlin Symbol Processing) reads Kotlin AST directly without Java stubs — it's up to 2x faster, generates Kotlin files (not Java), fully understands Kotlin-specific features, and works in Kotlin Multiplatform. Google has put KAPT in maintenance mode; KSP is the future.

  

---

  

****Q: Why must you use** `**@Transaction**` **with** `**@Relation**` **queries?****

  

When Room processes a relationship query, it executes at least two SELECT statements — one for the parent entities, one (batched) for the related entities. Without `@Transaction`, these two queries are independent: the data could change between them, resulting in inconsistent results (a user's posts might reflect a state from before or after another concurrent write). `@Transaction` wraps both queries in a single SQLite transaction, guaranteeing both reads see the same snapshot of data.

  

---

  

****Q: What is the difference between** `**suspend fun T**` **and** `**fun Flow**<T>` **as DAO return types?****

  

`suspend fun T` is a one-shot operation: it executes the query once, returns the result, and the function is done. `fun Flow<T>` is a reactive stream: it executes the initial query, emits the result, then registers an observer on the queried table. Whenever that table changes (any INSERT/UPDATE/DELETE), Room re-runs the query and emits the new result through the Flow. Use `suspend fun` for writes and one-time reads. Use `Flow` for list screens and any data that should stay current.

  

---

  

****Q: What happens if you change your entity schema without providing a migration?****

  

Room compares the version number stored in the database file with the version number in your `@Database` annotation. If they don't match, Room throws `IllegalStateException: Room cannot verify the data integrity. Looks like you've changed schema but forgot to update the version number`. Users who update your app will have their app crash on launch. If you call `.fallbackToDestructiveMigration()`, Room wipes the database and recreates it instead of crashing — but users lose all their data.

  

---

  

****Q: How does Room's compile-time query validation work?****

  

During the build process, Room's annotation processor (via KSP) reads all `@Query` annotations in your DAO. For each query string, it parses the SQL and checks it against the known schema (derived from your `@Entity` classes). It verifies: table names exist, column names exist, parameter types match Kotlin parameter types, return types are compatible with the query. If any check fails, the KSP code generation step fails → the build fails → you see a compile error with the exact problem description.

  

---

  

****Q: What is the difference between** `**@Upsert**` **and** `**@Insert(onConflict = REPLACE)**`**?****

  

Both insert-or-update, but differently. `@Insert(onConflict = REPLACE)` actually deletes the existing row and inserts a new one — this resets the row's internal ROWID and can trigger `ON DELETE CASCADE` on foreign keys (accidentally deleting child rows). `@Upsert` tries an INSERT first, and if there's a conflict, performs an UPDATE on the existing row. The row keeps its ROWID, no delete happens, foreign key children are safe. Use `@Upsert` whenever the entity has child entities referencing it.

  

---

  

---

  

# 22. Quick Reference Cheat Sheet

  

## Gradle (always use KSP)

```

plugins:  id("com.google.devtools.ksp")

          id("androidx.room")

  

android: room { schemaDirectory("$projectDir/schemas") }

  

deps:     room-runtime + room-ktx → implementation

          room-compiler           → ksp (never kapt)

```

  

## Entity Annotations

```

@Entity(tableName, indices, foreignKeys, ignoredColumns)

@PrimaryKey(autoGenerate = true)

@ColumnInfo(name, defaultValue, index, collate)

@Ignore                    → skip this field

@Embedded(prefix)          → flatten nested object

@TypeConverters            → for Date, List, Enum, Bitmap

@ForeignKey                → referential integrity

```

  

## DAO Annotations

```

@Insert(onConflict = REPLACE/IGNORE/ABORT)  → returns Long or List<Long>

@Update                                      → returns Int (rows affected)

@Delete                                      → returns Int (rows deleted)

@Upsert                                      → insert or update (safe, no delete)

@Query("SQL")                               → validated at compile time

@Transaction                                 → required for @Relation and atomic ops

```

  

## Return Types

```

suspend fun T                → one-shot fetch, throws on error

suspend fun T?               → one-shot, null if not found

suspend fun List<T>          → one-shot list

suspend fun Unit             → write (no return)

suspend fun Int/Long         → rows affected / new ID

fun Flow<T?>                 → live, auto-updates, null when empty

fun Flow<List<T>>            → live list, auto-updates on ANY table change

```

  

## SQL Reference

```sql

SELECT * FROM t

SELECT col1, col2 FROM t WHERE col = :param

SELECT * FROM t ORDER BY col DESC LIMIT :n OFFSET :offset

SELECT COUNT(*), AVG(col), MAX(col), SUM(col) FROM t

SELECT * FROM t WHERE col LIKE :pattern          -- pattern: "%search%"

SELECT * FROM t WHERE col IN (:list)

SELECT * FROM t1 INNER JOIN t2 ON t1.id = t2.foreign_id

SELECT * FROM t1 LEFT JOIN t2 ON t1.id = t2.foreign_id

SELECT col, COUNT(*) as cnt FROM t GROUP BY col HAVING cnt > :min

DELETE FROM t WHERE id = :id

UPDATE t SET col = :val WHERE id = :id

```

  

## Hilt vs Koin Wiring

```

HILT:                               KOIN:

@Module                             val dbModule = module {

@InstallIn(SingletonComponent)        single { Room.databaseBuilder(...).build() }

object DatabaseModule {               single { get<AppDatabase>().userDao() }

  @Provides @Singleton              }

  fun provideDb(ctx): AppDatabase

  @Provides

  fun provideDao(db): UserDao

}

```

  

## Migration Rule

```

Changed @Entity?

  → increment @Database version

  → add Migration(oldVersion, newVersion) { execSQL(...) }

  → .addMigrations(migration) in databaseBuilder

Simple change? Use AutoMigration(from = X, to = Y)

Complex change? Write manual Migration object

```

  

## KSP vs KAPT

```

KAPT: Kotlin → Java stubs → Java annotation processor → .java generated files

KSP:  Kotlin → KSP reads AST directly → .kt generated files

Speed: KSP is ~2x faster. KAPT in maintenance mode. Always use KSP.

```

  

---

  

*_Sources: developer.android.com/training/data-storage/room, developer.android.com/build/migrate-to-ksp, kotlinlang.org/docs/ksp-why-ksp, insert-koin.io, androidx Room release notes v2.8.4_*