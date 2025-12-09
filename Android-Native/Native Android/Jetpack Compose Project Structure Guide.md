# Jetpack Compose Project Structure Guide (2025)

## Table of Contents

1. [Introduction](#introduction)
2. [Core Architectural Principles](#core-architectural-principles)
3. [Recommended Architectures](#recommended-architectures)
4. [Detailed File Structures](#detailed-file-structures)
5. [Best Practices](#best-practices)
6. [Common Patterns](#common-patterns)
7. [Real-World Examples](#real-world-examples)
8. [References](#references)

---

## Introduction

Jetpack Compose is Android's modern declarative UI toolkit. Unlike the traditional View-based system, Compose encourages a different approach to project organization. This guide provides comprehensive information on structuring Jetpack Compose projects in 2025.

### Key Differences from Traditional Android

- **Single Activity Architecture**: Most Compose apps use one Activity with multiple Composable screens
- **Declarative UI**: UI is described as functions, not XML layouts
- **State-driven**: UI automatically updates when state changes
- **Composables as Building Blocks**: Small, reusable functions replace Fragments/Views

---

## Core Architectural Principles

### 1. Separation of Concerns

Don't put all your code in Activities or Composables. Each component should have a single, well-defined responsibility.

**Key Points:**

- Activities/Composables should only handle UI
- ViewModels manage UI state and business logic
- Repositories handle data operations
- Use Cases (optional) contain business logic

### 2. Single Source of Truth (SSOT)

Each piece of data should have one owner that modifies it and exposes it as immutable.

### 3. Unidirectional Data Flow (UDF)

- **State flows down**: From ViewModel to UI
- **Events flow up**: From UI to ViewModel

```
┌─────────────┐
│  ViewModel  │ ──► State flows down
└─────────────┘
       ▲
       │ Events flow up
┌─────────────┐
│     UI      │
└─────────────┘
```

### 4. Layered Architecture

Modern Android apps typically have 2-3 layers:

1. **UI Layer** (Presentation): Displays data (Composables, ViewModels)
2. **Domain Layer** (Optional): Business logic (Use Cases)
3. **Data Layer**: Manages app data (Repositories, Data Sources)

---

## Recommended Architectures

### Architecture 1: Clean Architecture with MVVM (Most Popular)

**Best for:** Medium to large projects, teams, scalable apps

```
com.example.bookstore/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   └── BookDao.kt
│   │   ├── entity/
│   │   │   └── BookEntity.kt
│   │   └── database/
│   │       └── AppDatabase.kt
│   ├── remote/
│   │   ├── api/
│   │   │   └── BookApiService.kt
│   │   ├── dto/
│   │   │   └── BookDto.kt
│   │   └── interceptor/
│   │       └── AuthInterceptor.kt
│   └── repository/
│       └── BookRepositoryImpl.kt
│
├── domain/
│   ├── model/
│   │   └── Book.kt
│   ├── repository/
│   │   └── BookRepository.kt (interface)
│   └── usecase/
│       ├── GetBooksUseCase.kt
│       └── AddBookUseCase.kt
│
├── presentation/
│   ├── navigation/
│   │   ├── NavGraph.kt
│   │   └── Screen.kt
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── components/
│   │   ├── BookCard.kt
│   │   └── LoadingIndicator.kt
│   ├── screens/
│   │   ├── home/
│   │   │   ├── HomeScreen.kt
│   │   │   ├── HomeViewModel.kt
│   │   │   ├── HomeState.kt
│   │   │   └── HomeEvent.kt
│   │   ├── detail/
│   │   │   ├── DetailScreen.kt
│   │   │   └── DetailViewModel.kt
│   │   └── auth/
│   │       ├── login/
│   │       │   ├── LoginScreen.kt
│   │       │   └── LoginViewModel.kt
│   │       └── register/
│   │           ├── RegisterScreen.kt
│   │           └── RegisterViewModel.kt
│   └── common/
│       └── utils/
│           └── StringExtensions.kt
│
├── di/
│   ├── AppModule.kt
│   ├── NetworkModule.kt
│   └── DatabaseModule.kt
│
└── MainActivity.kt
```

**Characteristics:**

- Three clear layers: Data, Domain, Presentation
- Domain layer contains business logic (Use Cases)
- Each screen has its own ViewModel, State, and Events
- Dependency Injection organized in separate modules

---

### Architecture 2: Feature-First Modularization (2025 Trend)

**Best for:** Large apps, multiple teams, clear feature boundaries

```
com.example.bookstore/
├── core/
│   ├── data/
│   │   ├── local/
│   │   └── remote/
│   ├── domain/
│   │   └── model/
│   ├── ui/
│   │   ├── components/
│   │   │   ├── AppButton.kt
│   │   │   └── AppTextField.kt
│   │   └── theme/
│   │       ├── Color.kt
│   │       ├── Theme.kt
│   │       └── Type.kt
│   ├── navigation/
│   │   └── NavGraph.kt
│   ├── di/
│   │   └── CoreModule.kt
│   └── utils/
│       └── NetworkUtils.kt
│
├── feature/
│   ├── auth/
│   │   ├── data/
│   │   │   ├── repository/
│   │   │   │   └── AuthRepositoryImpl.kt
│   │   │   └── remote/
│   │   │       └── AuthApiService.kt
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   └── User.kt
│   │   │   ├── repository/
│   │   │   │   └── AuthRepository.kt
│   │   │   └── usecase/
│   │   │       ├── LoginUseCase.kt
│   │   │       └── RegisterUseCase.kt
│   │   ├── presentation/
│   │   │   ├── login/
│   │   │   │   ├── LoginScreen.kt
│   │   │   │   ├── LoginViewModel.kt
│   │   │   │   └── LoginState.kt
│   │   │   └── register/
│   │   │       ├── RegisterScreen.kt
│   │   │       └── RegisterViewModel.kt
│   │   └── di/
│   │       └── AuthModule.kt
│   │
│   ├── book_list/
│   │   ├── data/
│   │   ├── domain/
│   │   ├── presentation/
│   │   └── di/
│   │
│   └── book_detail/
│       ├── data/
│       ├── domain/
│       ├── presentation/
│       └── di/
│
└── app/
    └── MainActivity.kt
```

**Characteristics:**

- Each feature is self-contained with its own layers
- Core module contains shared code
- Better for parallel development
- Easier to test and maintain feature boundaries

---

### Architecture 3: Simple MVVM (Small Projects)

**Best for:** Small apps, prototypes, learning projects

```
com.example.bookstore/
├── data/
│   ├── model/
│   │   └── Book.kt
│   ├── repository/
│   │   └── BookRepository.kt
│   └── api/
│       └── BookApiService.kt
│
├── ui/
│   ├── screens/
│   │   ├── home/
│   │   │   ├── HomeScreen.kt
│   │   │   └── HomeViewModel.kt
│   │   ├── detail/
│   │   │   ├── DetailScreen.kt
│   │   │   └── DetailViewModel.kt
│   │   └── auth/
│   │       ├── LoginScreen.kt
│   │       └── LoginViewModel.kt
│   ├── components/
│   │   ├── BookCard.kt
│   │   └── CustomButton.kt
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── navigation/
│       └── NavGraph.kt
│
├── di/
│   └── AppModule.kt
│
└── MainActivity.kt
```

**Characteristics:**

- Two layers: Data and UI
- No domain layer (simpler)
- ViewModel directly uses Repository
- Good for learning and small projects

---

## Detailed File Structures

### UI Layer (Presentation)

#### Screen Organization

Each screen typically contains:

**HomeScreen.kt** (Composable)

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    
    HomeContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateToDetail = onNavigateToDetail
    )
}

@Composable
private fun HomeContent(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    // UI implementation
}
```

**HomeViewModel.kt** (State Management)

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getBooksUseCase: GetBooksUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()
    
    fun onEvent(event: HomeEvent) {
        when(event) {
            is HomeEvent.LoadBooks -> loadBooks()
            is HomeEvent.SearchBooks -> searchBooks(event.query)
        }
    }
}
```

**HomeState.kt** (UI State)

```kotlin
data class HomeState(
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)
```

**HomeEvent.kt** (User Actions)

```kotlin
sealed class HomeEvent {
    object LoadBooks : HomeEvent()
    data class SearchBooks(val query: String) : HomeEvent()
    data class BookClicked(val bookId: String) : HomeEvent()
}
```

#### Navigation

**NavGraph.kt**

```kotlin
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToDetail = { bookId ->
                    navController.navigate(Screen.Detail.createRoute(bookId))
                }
            )
        }
        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("bookId") { type = NavType.StringType }
            )
        ) {
            DetailScreen()
        }
    }
}
```

**Screen.kt** (Navigation Routes)

```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Detail : Screen("detail/{bookId}") {
        fun createRoute(bookId: String) = "detail/$bookId"
    }
    object Login : Screen("login")
}
```

#### Components

**Reusable Composables**

```
presentation/components/
├── BookCard.kt
├── LoadingIndicator.kt
├── ErrorMessage.kt
├── SearchBar.kt
└── CustomButton.kt
```

#### Theme

```
presentation/theme/
├── Color.kt         // Color definitions
├── Theme.kt         // Theme configuration
├── Type.kt          // Typography
└── Shape.kt         // Shape styles
```

---

### Domain Layer

**Purpose:** Business logic, independent of Android framework

#### Models

**Book.kt** (Domain Model)

```kotlin
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val price: Double,
    val coverUrl: String
)
```

#### Use Cases

**GetBooksUseCase.kt**

```kotlin
class GetBooksUseCase @Inject constructor(
    private val repository: BookRepository
) {
    suspend operator fun invoke(): Result<List<Book>> {
        return repository.getBooks()
    }
}
```

**AddBookUseCase.kt**

```kotlin
class AddBookUseCase @Inject constructor(
    private val repository: BookRepository
) {
    suspend operator fun invoke(book: Book): Result<Unit> {
        // Business logic validation
        if (book.title.isBlank()) {
            return Result.failure(Exception("Title cannot be empty"))
        }
        return repository.addBook(book)
    }
}
```

#### Repository Interfaces

**BookRepository.kt** (Interface)

```kotlin
interface BookRepository {
    suspend fun getBooks(): Result<List<Book>>
    suspend fun getBookById(id: String): Result<Book>
    suspend fun addBook(book: Book): Result<Unit>
}
```

---

### Data Layer

#### Repository Implementation

**BookRepositoryImpl.kt**

```kotlin
class BookRepositoryImpl @Inject constructor(
    private val remoteDataSource: BookRemoteDataSource,
    private val localDataSource: BookLocalDataSource
) : BookRepository {
    
    override suspend fun getBooks(): Result<List<Book>> {
        return try {
            // Try remote first
            val books = remoteDataSource.getBooks()
            // Cache locally
            localDataSource.saveBooks(books)
            Result.success(books.map { it.toDomain() })
        } catch (e: Exception) {
            // Fallback to local
            val cachedBooks = localDataSource.getBooks()
            Result.success(cachedBooks.map { it.toDomain() })
        }
    }
}
```

#### Remote Data Source

```
data/remote/
├── api/
│   └── BookApiService.kt       // Retrofit interface
├── dto/
│   └── BookDto.kt              // Network models
└── interceptor/
    └── AuthInterceptor.kt      // Auth handling
```

**BookApiService.kt** (Retrofit)

```kotlin
interface BookApiService {
    @GET("books")
    suspend fun getBooks(): List<BookDto>
    
    @GET("books/{id}")
    suspend fun getBookById(@Path("id") id: String): BookDto
    
    @POST("books")
    suspend fun addBook(@Body book: BookDto): BookDto
}
```

#### Local Data Source

```
data/local/
├── dao/
│   └── BookDao.kt              // Room DAO
├── entity/
│   └── BookEntity.kt           // Room entity
└── database/
    └── AppDatabase.kt          // Room database
```

**BookDao.kt** (Room)

```kotlin
@Dao
interface BookDao {
    @Query("SELECT * FROM books")
    suspend fun getAllBooks(): List<BookEntity>
    
    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: String): BookEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)
}
```

---

### Dependency Injection

#### Using Hilt (Recommended 2025)

```
di/
├── AppModule.kt          // General app dependencies
├── NetworkModule.kt      // Network dependencies
├── DatabaseModule.kt     // Database dependencies
└── RepositoryModule.kt   // Repository bindings
```

**AppModule.kt**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
}
```

**NetworkModule.kt**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideBookApiService(retrofit: Retrofit): BookApiService {
        return retrofit.create(BookApiService::class.java)
    }
}
```

**RepositoryModule.kt**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindBookRepository(
        impl: BookRepositoryImpl
    ): BookRepository
}
```

---

## Best Practices

### 1. State Management

**✅ Do:**

```kotlin
// Use StateFlow for UI state
private val _state = MutableStateFlow(HomeState())
val state: StateFlow<HomeState> = _state.asStateFlow()

// Collect in Composable
val state by viewModel.state.collectAsState()
```

**❌ Don't:**

```kotlin
// Don't use LiveData in new Compose projects
val state: LiveData<HomeState> = _state

// Don't expose mutable state
val state: MutableStateFlow<HomeState> = _state
```

### 2. Screen Organization

**✅ Do:**

- One screen = one file (unless very large)
- Separate stateful and stateless composables
- Use preview functions for UI development

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel) { /* Stateful */ }

@Composable
private fun HomeContent(state: HomeState) { /* Stateless */ }

@Preview
@Composable
private fun HomeContentPreview() {
    HomeContent(state = HomeState())
}
```

### 3. Component Organization

**✅ Do:**

- Create reusable components in `components/` folder
- Keep components small and focused
- Use meaningful names

```
components/
├── BookCard.kt         ✅ Specific
├── LoadingIndicator.kt ✅ Clear purpose
└── ErrorMessage.kt     ✅ Reusable
```

**❌ Don't:**

```
components/
├── Widget.kt           ❌ Too generic
├── Stuff.kt            ❌ Unclear
└── MyComponent.kt      ❌ Non-descriptive
```

### 4. Navigation

**✅ Do:**

- Use type-safe navigation
- Define routes in sealed class
- Pass minimal data through navigation

**❌ Don't:**

- Hardcode routes as strings
- Pass entire objects through navigation
- Create circular navigation dependencies

### 5. Dependency Injection

**✅ Do:**

- Use Hilt (recommended for 2025)
- Inject into ViewModels, not Composables
- Keep DI modules organized

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val useCase: GetBooksUseCase
) : ViewModel()
```

**❌ Don't:**

```kotlin
@Composable
fun HomeScreen(useCase: GetBooksUseCase) { // Don't inject here
    // ...
}
```

### 6. Error Handling

**✅ Do:**

```kotlin
sealed class Result<T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error<T>(val message: String) : Result<T>()
    data class Loading<T> : Result<T>()
}
```

### 7. Testing Structure

```
test/
├── data/
│   └── repository/
│       └── BookRepositoryTest.kt
├── domain/
│   └── usecase/
│       └── GetBooksUseCaseTest.kt
└── presentation/
    └── viewmodel/
        └── HomeViewModelTest.kt

androidTest/
└── ui/
    └── HomeScreenTest.kt
```

---

## Common Patterns

### Pattern 1: State and Events

**State**: What the UI looks like **Events**: What the user does

```kotlin
// State
data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

// Events
sealed class LoginEvent {
    data class EmailChanged(val email: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    object LoginClicked : LoginEvent()
}

// ViewModel
class LoginViewModel : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()
    
    fun onEvent(event: LoginEvent) {
        when(event) {
            is LoginEvent.EmailChanged -> {
                _state.update { it.copy(email = event.email) }
            }
            is LoginEvent.PasswordChanged -> {
                _state.update { it.copy(password = event.password) }
            }
            is LoginEvent.LoginClicked -> login()
        }
    }
}
```

### Pattern 2: Side Effects

**For one-time events** (navigation, show snackbar):

```kotlin
sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
}

class HomeViewModel : ViewModel() {
    private val _eventFlow = Channel<UiEvent>()
    val eventFlow = _eventFlow.receiveAsFlow()
    
    fun showError(message: String) {
        viewModelScope.launch {
            _eventFlow.send(UiEvent.ShowSnackbar(message))
        }
    }
}

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when(event) {
                is UiEvent.ShowSnackbar -> {
                    // Show snackbar
                }
                is UiEvent.Navigate -> {
                    // Navigate
                }
            }
        }
    }
}
```

### Pattern 3: Repository Pattern

```kotlin
// Interface in domain layer
interface BookRepository {
    suspend fun getBooks(): Flow<List<Book>>
}

// Implementation in data layer
class BookRepositoryImpl(
    private val api: BookApiService,
    private val dao: BookDao
) : BookRepository {
    
    override suspend fun getBooks(): Flow<List<Book>> = flow {
        // Emit cached data first
        emit(dao.getAllBooks().map { it.toDomain() })
        
        // Fetch fresh data
        val freshBooks = api.getBooks()
        dao.insertBooks(freshBooks.map { it.toEntity() })
        
        // Emit fresh data
        emit(freshBooks.map { it.toDomain() })
    }
}
```

---

## Real-World Examples

### Example 1: E-commerce App Structure

```
com.example.shop/
├── core/
│   ├── data/
│   ├── ui/
│   ├── navigation/
│   └── utils/
├── feature/
│   ├── auth/
│   ├── home/
│   ├── product_list/
│   ├── product_detail/
│   ├── cart/
│   ├── checkout/
│   └── profile/
└── app/
```

### Example 2: Social Media App Structure

```
com.example.social/
├── core/
│   ├── data/
│   ├── ui/
│   └── navigation/
├── feature/
│   ├── auth/
│   ├── feed/
│   ├── post_create/
│   ├── profile/
│   ├── messages/
│   └── notifications/
└── app/
```

### Example 3: News App Structure

```
com.example.news/
├── data/
│   ├── local/
│   ├── remote/
│   └── repository/
├── domain/
│   ├── model/
│   └── usecase/
├── presentation/
│   ├── screens/
│   │   ├── home/
│   │   ├── article/
│   │   ├── category/
│   │   └── search/
│   ├── components/
│   ├── theme/
│   └── navigation/
└── di/
```

---

## References

### Official Documentation

1. **Android Developers - Guide to App Architecture** https://developer.android.com/topic/architecture
    
    - Official guide from Google on modern Android app architecture
2. **Jetpack Compose - UI Architecture** https://developer.android.com/develop/ui/compose/architecture
    
    - Understanding UDF and state management in Compose
3. **Android Architecture Components** https://developer.android.com/topic/architecture/intro
    
    - ViewModel, LiveData, Room, and other components
4. **Modern Android App Architecture** https://developer.android.com/courses/pathways/android-architecture
    
    - Comprehensive learning path from Google

### Community Resources

5. **Now in Android (Google Sample)** https://github.com/android/nowinandroid
    
    - Production-quality app by Google showing best practices
6. **Jonas Rodehorst - How to Structure Jetpack Compose Project** https://jonas-rodehorst.dev/blog/how-to-structure-your-jetpack-compose-project
    
    - Detailed blog post on different structure approaches
7. **Medium - Clean Architecture Guide** Multiple articles on implementing Clean Architecture with Compose
    

### Key Principles Summary

1. **Separation of Concerns**: Each component has one responsibility
2. **Single Source of Truth**: One owner per data type
3. **Unidirectional Data Flow**: State down, events up
4. **Layered Architecture**: UI → Domain → Data
5. **Testability**: Design for easy testing
6. **Scalability**: Structure that grows with your app

---

## Quick Decision Guide

### When to use which architecture?

|Project Size|Recommended Architecture|Complexity|
|---|---|---|
|Small/Prototype|Simple MVVM|Low|
|Medium|Clean Architecture|Medium|
|Large/Team|Feature-First Modular|High|

### Key Questions to Ask:

1. **How many developers?**
    
    - Solo: Simple MVVM
    - Small team: Clean Architecture
    - Large team: Feature-First
2. **Expected app size?**
    
    - <10 screens: Simple MVVM
    - 10-30 screens: Clean Architecture
    - 30+ screens: Feature-First
3. **Long-term maintenance?**
    
    - Short-term: Simple MVVM
    - Long-term: Clean Architecture or Feature-First
4. **Need multi-module support?**
    
    - No: Clean Architecture
    - Yes: Feature-First

---

## Conclusion

There's no "one-size-fits-all" architecture. The best structure depends on:

- Your app's complexity
- Team size
- Long-term goals
- Performance requirements

**For 2025**, the trends are:

- ✅ Feature-first modularization for large apps
- ✅ Clean Architecture with MVVM for most projects
- ✅ Hilt for dependency injection
- ✅ StateFlow over LiveData
- ✅ Use Cases for complex business logic
- ✅ Type-safe navigation

**Start simple, evolve as needed!**

---

_Last Updated: November 2025_ _Based on official Android documentation and current industry practices_