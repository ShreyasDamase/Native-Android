# Jetpack Compose Navigation 3: Type-Safe Routes

Modern Jetpack Compose Navigation (introduced in Navigation 2.8.0+ and evolving into Navigation 3) has shifted from string-based routes (prone to runtime errors, typos, and cumbersome formatting) to a robust, compile-time checked **Type-Safe Routing** system powered by Kotlin Serialization.

---

## 1. Why Type-Safe Navigation?

Historically, navigating in Jetpack Compose required defining string patterns like `"profile/{userId}?age={age}"`, manually parsing parameters from a bundle, and ensuring type conversions didn't crash.

| Feature | Legacy Navigation (String-Based) | Type-Safe Navigation (Serialization) |
| :--- | :--- | :--- |
| **Route Definition** | String constants (`"home"`, `"profile/{id}"`) | Kotlin objects/classes (`@Serializable object Home`) |
| **Argument Passing** | String query parameters and templates | Strongly-typed constructor arguments |
| **Compile-Time Safety** | ❌ None (typos cause runtime crashes) |  Compile-time validation |
| **IDE Refactoring** | ❌ Manual search & replace in strings |  Auto-refactor works across the codebase |
| **Custom Object Passing** | ❌ Highly complex custom NavTypes |  Natively supported via `@Serializable` |

---

## 2. Gradle Setup & Configuration

To use type-safe navigation, you must configure the Kotlin Serialization plugin and import the correct Navigation Compose dependencies in your `build.gradle.kts`.

### Project-level `build.gradle.kts`
```kotlin
plugins {
    // Enable Kotlin Serialization compiler plugin
    kotlin("plugin.serialization") version "2.0.0" apply false
}
```

### Module-level `build.gradle.kts`
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Apply the serialization plugin
    kotlin("plugin.serialization")
}

dependencies {
    // Use Navigation Compose 2.8.0 or higher
    val navVersion = "2.8.0"
    implementation("androidx.navigation:navigation-compose:$navVersion")
    
    // Kotlin Serialization library
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
```

---

## 3. Defining Routes

Routes are declared as standard Kotlin classes or objects annotated with `@Serializable`.

```kotlin
import kotlinx.serialization.Serializable

// 1. Static Screen (No Arguments)
@Serializable
object Home

// 2. Screen with Required Arguments
@Serializable
data class Profile(
    val userId: String,
    val rank: Int
)

// 3. Screen with Optional Arguments (Must have default values)
@Serializable
data class SearchResults(
    val query: String,
    val sortBy: String = "relevance",  // Optional argument with default
    val page: Int = 1                 // Optional argument with default
)

// 4. Screen with Nullable Arguments
@Serializable
data class Detail(
    val itemId: String,
    val promoCode: String? = null     // Nullable argument
)
```

> [!NOTE]
> * Use `object` for routes that do not accept parameters.
> * Use `data class` for routes that require parameters.
> * Any field with a default value is automatically treated as an optional parameter.

---

## 4. Navigating and Setting Up NavHost

Instead of navigating with strings, you pass instances of your serialized routes to the `NavController`.

### Setup `NavHost`
```kotlin
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute

@Composable
fun MainNavigationApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Home // Pass the object class definition as the start route
    ) {
        // Define destination for Home route
        composable<Home> {
            HomeScreen(
                onNavigateToProfile = { id, rank ->
                    // Navigate using class instance!
                    navController.navigate(Profile(userId = id, rank = rank))
                },
                onSearch = { query ->
                    navController.navigate(SearchResults(query = query))
                }
            )
        }

        // Define destination for Profile route
        composable<Profile> { backStackEntry ->
            // Extract type-safe arguments using toRoute<T>()!
            val profileArgs = backStackEntry.toRoute<Profile>()
            
            ProfileScreen(
                userId = profileArgs.userId,
                rank = profileArgs.rank,
                onBack = { navController.popBackStack() }
            )
        }

        // Define destination for Search Results route
        composable<SearchResults> { backStackEntry ->
            val searchArgs = backStackEntry.toRoute<SearchResults>()
            
            SearchScreen(
                query = searchArgs.query,
                sortBy = searchArgs.sortBy,
                page = searchArgs.page
            )
        }
    }
}
```

---

## 5. Advanced Configuration

### A. Extracting Route from BackStack Entry (Shared Screen / Bottom Sheet / Scoped ViewModel)
If you need to retrieve arguments within a ViewModel or a parent layout, you can extract the route via the `NavBackStackEntry`.

```kotlin
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute

// ViewModel retrieving type-safe arguments directly from SavedStateHandle
class ProfileViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    // Use the toRoute extension on SavedStateHandle to retrieve parameters type-safely
    private val profileArgs = savedStateHandle.toRoute<Profile>()
    
    val userId: String = profileArgs.userId
    val rank: Int = profileArgs.rank
}
```

### B. Nested Navigation Graphs
Nested graphs are fully type-safe. Declare a serializable object to represent the nested graph root itself.

```kotlin
// Graph root identifier
@Serializable
object AuthGraph

// Auth screens
@Serializable
object Login

@Serializable
object Register

// Setup in NavHost:
NavHost(navController = navController, startDestination = AuthGraph) {
    navigation<AuthGraph>(startDestination = Login) {
        composable<Login> { LoginScreen(onLoginSuccess = { navController.navigate(Home) }) }
        composable<Register> { RegisterScreen() }
    }
    
    composable<Home> { HomeScreen() }
}
```

### C. Passing Complex/Custom Object Types
By default, the library supports primitive types (`Int`, `Float`, `Boolean`, `Long`, `Double`, `String`) and their arrays/lists. To pass custom objects, you must create a custom `NavType` and serialize the object as JSON.

```kotlin
import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.net.URLEncoder

@Serializable
data class UserMetadata(
    val email: String,
    val isAdmin: Boolean
)

// Custom NavType implementation
val UserMetadataType = object : NavType<UserMetadata>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): UserMetadata? {
        return bundle.getString(key)?.let { Json.decodeFromString(it) }
    }

    override fun parseValue(value: String): UserMetadata {
        // Decode URL to handle special characters properly
        val decoded = URLDecoder.decode(value, "UTF-8")
        return Json.decodeFromString(decoded)
    }

    override fun put(bundle: Bundle, key: String, value: UserMetadata) {
        bundle.putString(key, Json.encodeToString(UserMetadata.serializer(), value))
    }

    override fun serializeAsValue(value: UserMetadata): String {
        // Encode URL to prevent breaking path templates
        return URLEncoder.encode(Json.encodeToString(UserMetadata.serializer(), value), "UTF-8")
    }
}

// Defining a route using the custom type
@Serializable
data class AdvancedProfile(val metadata: UserMetadata)

// Registering in Composable
composable<AdvancedProfile>(
    typeMap = mapOf(typeOf<UserMetadata>() to UserMetadataType)
) { backStackEntry ->
    val args = backStackEntry.toRoute<AdvancedProfile>()
    val email = args.metadata.email
    // Render Screen...
}
```

---

## 6. Type-Safe Bottom Sheets and Dialogs

Just like composable destinations, dialogs and bottom sheets use the exact same type-safe pattern:

```kotlin
import androidx.navigation.compose.dialog

@Serializable
object RatingDialog

// Inside NavHost
dialog<RatingDialog> {
    RatingDialogContent(
        onDismiss = { navController.popBackStack() }
    )
}
```

---

## 7. Migration Checklist: Legacy -> Type-Safe

When refactoring a legacy string-based application:
1. [ ] Remove all hardcoded string routes (e.g. `const val ROUTE_HOME = "home"`).
2. [ ] Define a `@Serializable object` or `@Serializable data class` for each destination.
3. [ ] Replace `navController.navigate("route_name/123")` with `navController.navigate(RouteClass(123))`.
4. [ ] Replace `composable("route/{id}") { entry -> val id = entry.arguments?.getString("id") }` with `composable<RouteClass> { entry -> val args = entry.toRoute<RouteClass>() }`.
5. [ ] Pass `typeMap` map when using custom parcelables or non-primitive objects.
