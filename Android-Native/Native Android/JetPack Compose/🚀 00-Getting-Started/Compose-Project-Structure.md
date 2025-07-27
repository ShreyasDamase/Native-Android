# **Jetpack Compose Project Structure: Complete Guide for 2025**

## **1. Core Architecture Principles**

### **Unidirectional Data Flow (UDF)**

The foundation of Compose architecture follows the unidirectional data flow pattern:

- **Event**: UI generates events (button clicks, text changes)
- **Update State**: Event handlers change the state
- **Display State**: UI displays the updated state

```kotlin
// Event flows up
Button(onClick = { onButtonClick() }) { Text("Click me") }

// State flows down
@Composable
fun MyScreen(uiState: UiState, onEvent: (Event) -> Unit) {
    when (uiState) {
        is UiState.Loading -> LoadingComponent()
        is UiState.Success -> SuccessComponent(uiState.data)
        is UiState.Error -> ErrorComponent(uiState.message)
    }
}
```

## **2. Recommended Project Structure**

### **Clean Architecture Layers**

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/yourpackage/
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── database/
│   │   │   │   │   │   ├── dao/
│   │   │   │   │   │   ├── entities/
│   │   │   │   │   │   └── AppDatabase.kt
│   │   │   │   │   └── preferences/
│   │   │   │   ├── remote/
│   │   │   │   │   ├── api/
│   │   │   │   │   ├── dto/
│   │   │   │   │   └── NetworkService.kt
│   │   │   │   ├── repository/
│   │   │   │   │   ├── impl/
│   │   │   │   │   └── interfaces/
│   │   │   │   └── mappers/
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   ├── repository/
│   │   │   │   ├── usecase/
│   │   │   │   └── util/
│   │   │   ├── presentation/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screen/
│   │   │   │   │   │   ├── home/
│   │   │   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   │   │   ├── HomeViewModel.kt
│   │   │   │   │   │   │   ├── HomeUiState.kt
│   │   │   │   │   │   │   └── components/
│   │   │   │   │   │   ├── profile/
│   │   │   │   │   │   └── settings/
│   │   │   │   │   ├── components/
│   │   │   │   │   │   ├── common/
│   │   │   │   │   │   │   ├── AppButton.kt
│   │   │   │   │   │   │   ├── AppTextField.kt
│   │   │   │   │   │   │   └── LoadingIndicator.kt
│   │   │   │   │   │   └── specific/
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   ├── AppNavigation.kt
│   │   │   │   │   │   ├── NavigationScreens.kt
│   │   │   │   │   │   └── NavigationArgs.kt
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Color.kt
│   │   │   │   │       ├── Shape.kt
│   │   │   │   │       ├── Theme.kt
│   │   │   │   │       └── Type.kt
│   │   │   │   └── MainActivity.kt
│   │   │   ├── di/
│   │   │   │   ├── DatabaseModule.kt
│   │   │   │   ├── NetworkModule.kt
│   │   │   │   ├── RepositoryModule.kt
│   │   │   │   └── UseCaseModule.kt
│   │   │   └── util/
│   │   │       ├── extensions/
│   │   │       ├── constants/
│   │   │       └── helpers/
│   │   └── res/
│   │       ├── drawable/
│   │       ├── values/
│   │       └── xml/
│   └── test/
└── build.gradle.kts
```

## **3. Layer-by-Layer Breakdown**

### **Data Layer**

Handles data operations and business logic:

```kotlin
// Repository Implementation
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val userApi: UserApi
) : UserRepository {
    
    override fun getUsers(): Flow<List<User>> = flow {
        emit(userDao.getAllUsers())
        try {
            val remoteUsers = userApi.getUsers()
            userDao.insertUsers(remoteUsers)
            emit(userDao.getAllUsers())
        } catch (e: Exception) {
            // Handle error
        }
    }
}
```

### **Domain Layer**

Contains business logic and use cases:

```kotlin
// Use Case
@Singleton
class GetUsersUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading())
        userRepository.getUsers()
            .catch { emit(Resource.Error(it.message ?: "Unknown error")) }
            .collect { emit(Resource.Success(it)) }
    }
}
```

### **Presentation Layer**

Contains UI components and ViewModels:

```kotlin
// ViewModel
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getUsersUseCase: GetUsersUseCase
) : ViewModel() {
    
    private val _uiState = mutableStateOf(HomeUiState())
    val uiState: State<HomeUiState> = _uiState
    
    init {
        getUsers()
    }
    
    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.RefreshUsers -> getUsers()
            is HomeEvent.UserClicked -> navigateToUserDetail(event.userId)
        }
    }
    
    private fun getUsers() {
        getUsersUseCase().onEach { result ->
            _uiState.value = when (result) {
                is Resource.Loading -> uiState.value.copy(isLoading = true)
                is Resource.Success -> uiState.value.copy(
                    isLoading = false,
                    users = result.data ?: emptyList()
                )
                is Resource.Error -> uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }.launchIn(viewModelScope)
    }
}

// UI State
data class HomeUiState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val error: String? = null
)

// Events
sealed class HomeEvent {
    object RefreshUsers : HomeEvent()
    data class UserClicked(val userId: String) : HomeEvent()
}
```

## **4. Screen Structure Pattern**

### **Screen Composable**

```kotlin
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState
    
    HomeContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToProfile = { userId ->
            navController.navigate("profile/$userId")
        }
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when {
            uiState.isLoading -> LoadingComponent()
            uiState.error != null -> ErrorComponent(
                message = uiState.error,
                onRetry = { onEvent(HomeEvent.RefreshUsers) }
            )
            else -> UserList(
                users = uiState.users,
                onUserClick = { user ->
                    onEvent(HomeEvent.UserClicked(user.id))
                    onNavigateToProfile(user.id)
                }
            )
        }
    }
}
```

## **5. Component Organization**

### **Common Components**

Reusable UI components used across the app:

```kotlin
// Common Button
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonType: ButtonType = ButtonType.Primary
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = when (buttonType) {
                ButtonType.Primary -> MaterialTheme.colorScheme.primary
                ButtonType.Secondary -> MaterialTheme.colorScheme.secondary
            }
        )
    ) {
        Text(text = text)
    }
}

enum class ButtonType { Primary, Secondary }
```

### **Screen-Specific Components**

Components used only in specific screens:

```kotlin
// components/home/UserCard.kt
@Composable
fun UserCard(
    user: User,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

## **6. Navigation Structure**

### **Navigation Setup**

```kotlin
// navigation/AppNavigation.kt
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = NavigationScreens.Home.route
    ) {
        composable(NavigationScreens.Home.route) {
            HomeScreen(navController)
        }
        
        composable(
            route = "${NavigationScreens.Profile.route}/{userId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ProfileScreen(
                navController = navController,
                userId = userId
            )
        }
    }
}

// navigation/NavigationScreens.kt
sealed class NavigationScreens(val route: String) {
    object Home : NavigationScreens("home")
    object Profile : NavigationScreens("profile")
    object Settings : NavigationScreens("settings")
}
```

## **7. Theme and Styling**

### **Theme Setup**

```kotlin
// theme/Theme.kt
@Composable
fun MyAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) 
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
```

## **8. Dependency Injection Structure**

### **Hilt Modules**

```kotlin
// di/DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }
    
    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()
}
```

## **9. Testing Structure**

### **Test Organization**

```
test/
├── unit/
│   ├── data/
│   ├── domain/
│   └── presentation/
├── integration/
└── ui/
    ├── screen/
    └── component/
```

### **Composable Testing**

```kotlin
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun homeScreen_displaysUsers_whenDataLoaded() {
        val testUsers = listOf(
            User("1", "John Doe", "john@example.com"),
            User("2", "Jane Smith", "jane@example.com")
        )
        
        composeTestRule.setContent {
            MyAppTheme {
                HomeContent(
                    uiState = HomeUiState(users = testUsers),
                    onEvent = {},
                    onNavigateToProfile = {}
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("John Doe")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Jane Smith")
            .assertIsDisplayed()
    }
}
```

## **10. Best Practices Summary**

### **State Management**

- Use `remember` for UI-only state
- Use `rememberSaveable` for state that survives configuration changes
- Use ViewModel for business logic state
- Follow unidirectional data flow

### **Performance**

- Keep composables pure and stateless when possible
- Use `LaunchedEffect` for side effects
- Implement proper key usage in `LazyColumn`/`LazyRow`
- Avoid unnecessary recompositions

### **Code Organization**

- Separate screen logic from UI composition
- Create reusable components
- Use sealed classes for events and UI states
- Implement proper error handling

### **Architecture**

- Follow Clean Architecture principles
- Use dependency injection (Hilt/Dagger)
- Implement repository pattern
- Use use cases for complex business logic

This structure ensures scalability, maintainability, and testability while following the latest Jetpack Compose best practices for 2025.