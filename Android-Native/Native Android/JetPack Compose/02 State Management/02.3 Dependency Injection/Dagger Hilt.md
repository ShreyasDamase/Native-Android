# Dagger Hilt with Jetpack Compose

## 📌 Purpose
Dagger Hilt is the recommended Dependency Injection (DI) framework for Android. When using Jetpack Compose, Hilt seamlessly integrates to provide ViewModels and other dependencies directly into your Composable functions, managing lifecycles and scoping automatically.

> [!NOTE]
> Compose UI doesn't use XML or Fragments. Because Composables are just functions, Hilt provides specific extensions to inject ViewModels without needing standard Fragment/Activity lifecycle hooks.

## 🔧 Core Functions

### `hiltViewModel()`
```kotlin
@Composable
inline fun <reified VM : ViewModel> hiltViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null
): VM
```

## 📋 Props / Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `viewModelStoreOwner` | `ViewModelStoreOwner` | `LocalViewModelStoreOwner.current` | Determines the scope of the ViewModel (e.g., the current Activity or the current Navigation BackStackEntry). |
| `key` | `String?` | `null` | Optional key to differentiate multiple ViewModels of the same type. |

## ✅ Basic Example

### 1. The ViewModel
Annotate your ViewModel with `@HiltViewModel` and use `@Inject` for dependencies.
```kotlin
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    // ViewModel logic
}
```

### 2. The Composable
Use `hiltViewModel()` to get an instance. It will automatically be scoped to the parent Activity or Fragment.

```kotlin
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun UserScreen(
    // Inject the ViewModel here
    viewModel: UserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Text("Hello, ${uiState.userName}")
}
```

## 🚀 Advanced Examples

### Scoping ViewModels to the Navigation Graph
In modern Compose apps using Navigation Compose, `hiltViewModel()` scopes by default to the current **BackStackEntry** (the current screen). If you navigate away and back, the ViewModel is recreated.

If you want to share a ViewModel across multiple screens (e.g., an onboarding flow), you can scope it to a nested navigation graph.

```kotlin
NavHost(navController, startDestination = "auth_graph") {
    
    navigation(startDestination = "login", route = "auth_graph") {
        
        composable("login") { backStackEntry ->
            // Retrieve the NavBackStackEntry for the parent graph
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry("auth_graph")
            }
            
            // Scope the ViewModel to the parent graph
            val sharedViewModel: AuthViewModel = hiltViewModel(parentEntry)
            LoginScreen(sharedViewModel)
        }
        
        composable("register") { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry("auth_graph")
            }
            // Gets the EXACT SAME instance as the login screen
            val sharedViewModel: AuthViewModel = hiltViewModel(parentEntry)
            RegisterScreen(sharedViewModel)
        }
    }
}
```

### Injecting Standard Dependencies into Composables
Generally, you shouldn't inject raw dependencies directly into Composables. State and logic should flow through a ViewModel. However, if you *must* inject an entry point (e.g., an image loader or an analytics tracker), you can use Hilt Entry Points.

> [!WARNING]
> Only do this for pure utilities. Do not inject repositories or use-cases directly into Composables.

```kotlin
@EntryPoint
@InstallIn(ActivityComponent::class)
interface AnalyticsEntryPoint {
    fun getAnalyticsTracker(): AnalyticsTracker
}

@Composable
fun TrackedScreen() {
    val context = LocalContext.current
    
    // Retrieve dependency directly from Hilt graph
    val analyticsTracker = remember {
        EntryPointAccessors.fromActivity(
            context as Activity,
            AnalyticsEntryPoint::class.java
        ).getAnalyticsTracker()
    }

    LaunchedEffect(Unit) {
        analyticsTracker.trackScreenView("TrackedScreen")
    }
}
```

## ⚠️ Common Gotchas

1. **Wrong Import for `hiltViewModel`**: 
   Ensure you import `androidx.hilt.navigation.compose.hiltViewModel`. Do not import standard `viewModel()` if you are trying to inject dependencies via Hilt, or it will crash looking for a default constructor.
2. **Missing `@AndroidEntryPoint`**:
   Even though you use `hiltViewModel()` in Compose, the **Activity** hosting the Compose content `setContent { ... }` MUST be annotated with `@AndroidEntryPoint`.
3. **Passing ViewModels down the tree**:
   Do not pass ViewModels deeply down your Composable tree. Extract state at the screen level (Route level) and pass only primitive state and lambda callbacks down to UI components (State Hoisting).

## 💡 Interview Q&A

**Q: How does `hiltViewModel()` know what to scope the ViewModel to?**
A: `hiltViewModel()` uses `LocalViewModelStoreOwner.current`. In a standard Activity, this resolves to the Activity. If you are using Jetpack Navigation Compose, the `NavHost` overrides this `CompositionLocal` to provide the `NavBackStackEntry` of the current route. Thus, ViewModels are automatically scoped to the current navigation screen.

**Q: Can I use `@Inject` directly on Composable functions?**
A: No. Composable functions are top-level functions, not classes, so constructor injection isn't possible. You inject ViewModels using `hiltViewModel()`, and if you absolutely need an interface/class injected, you use Hilt `EntryPoints`.

**Q: Why shouldn't I pass ViewModels to child Composables?**
A: Passing ViewModels makes child Composables tightly coupled to the ViewModel, difficult to test (requiring a mocked ViewModel rather than simple data), and impossible to preview with `@Preview`. You should hoist the state and pass data/lambdas instead.
