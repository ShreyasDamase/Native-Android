 # Android Navigation - Complete Study Notes

**Jetpack Compose Navigation Guide**

---

## Table of Contents

### Chapter 1: Navigation Fundamentals

- 1.1 What is Navigation?
- 1.2 Why Navigation Component?
- 1.3 Core Architecture
- 1.4 Navigation Principles
- 1.5 Back Stack Concept

### Chapter 2: Navigation 2 - String-Based Routes

- 2.1 Overview & When to Use
- 2.2 Setup & Dependencies
- 2.3 Defining Routes
- 2.4 Creating Navigation Graph
- 2.5 NavHost & NavController
- 2.6 Passing Arguments
- 2.7 Navigation Actions
- 2.8 Nested Navigation
- 2.9 Bottom Navigation
- 2.10 Deep Linking
- 2.11 Best Practices

### Chapter 3: Navigation 3 - Type-Safe Routes

- 3.1 Overview & When to Use
- 3.2 Setup & Dependencies
- 3.3 Defining Routes with Serialization
- 3.4 Creating Navigation Graph
- 3.5 NavHost & NavController
- 3.6 Passing Arguments
- 3.7 Navigation Actions
- 3.8 Nested Navigation
- 3.9 Bottom Navigation
- 3.10 Deep Linking
- 3.11 Best Practices

### Chapter 4: Common Concepts (Both Versions)

- 4.1 Back Stack Management
- 4.2 Navigation Options (popUpTo, launchSingleTop, etc.)
- 4.3 Shared ViewModel
- 4.4 Save & Restore State
- 4.5 Testing Navigation
- 4.6 Common Patterns

### Chapter 5: Quick Reference

- 5.1 Navigation 2 Cheat Sheet
- 5.2 Navigation 3 Cheat Sheet
- 5.3 Comparison Table
- 5.4 Decision Guide

---

# Chapter 1: Navigation Fundamentals

## 1.1 What is Navigation?

Navigation in Android refers to the interactions that allow users to navigate across, into, and back out from different pieces of content within your app.

**Key Components:**

- Moving between screens
- Passing data between destinations
- Managing the back stack
- Deep linking to specific content

---

## 1.2 Why Navigation Component?

### Before Navigation Component

Developers had to manually:

- Manage Fragment transactions
- Handle back stack themselves
- Write custom deep link handling
- Deal with inconsistent navigation behavior
- Write lots of boilerplate code

### After Navigation Component

Navigation Component provides:

- **Simplified Fragment/Composable transactions**
- **Automatic back stack management**
- **Type-safe argument passing** (Nav 3)
- **Built-in deep link support**
- **Visual navigation editor** (for XML)
- **Animation support**
- **Testing utilities**

---

## 1.3 Core Architecture

Navigation Component consists of three main parts:

### 1. Navigation Graph

- Central navigation map of your app
- Contains all destinations and actions
- **Nav 2:** String-based routes
- **Nav 3:** Type-safe Kotlin objects

### 2. NavHost

- Container that displays destinations
- Acts as a placeholder in your UI
- Swaps destinations as user navigates
- In Compose: `NavHost()` composable

### 3. NavController

- The brain/orchestrator
- Manages navigation between destinations
- Controls back stack
- Handles arguments
- Triggers navigation actions

**Visual Representation:**

```
┌─────────────────────────────────────────────────────┐
│          NAVIGATION GRAPH                           │
│     (All routes and destinations)                   │
│                                                     │
│   ┌──────────┐    ┌──────────┐    ┌──────────┐      │
│   │ Screen A │───▶│ Screen B │───▶│ Screen C │      │
│   └──────────┘    └──────────┘    └──────────┘      │
└─────────────────────────────────────────────────────┘
         ▲                                  ▲
         │                                  │
         ▼                                  ▼
┌──────────────────┐              ┌──────────────────┐
│     NavHost      │              │  NavController   │
│   (Container)    │◀────────────▶│  (Orchestrator)  │
│  Shows screens   │              │  Controls nav    │
└──────────────────┘              └──────────────────┘
```

---

## 1.4 Navigation Principles

### Principle 1: Fixed Start Destination

Your app should always have the same starting point.

```kotlin
// ❌ Bad - unpredictable start
startDestination = if (isLoggedIn) "home" else "login"

// ✅ Good - fixed start
startDestination = "splash"  // Always starts at splash
```

**Why?** Users should always know what to expect when launching your app.

---

### Principle 2: Up vs Back Button

**Up Button (App Bar):**

- Stays within your app
- Navigates within app hierarchy
- Never exits the app
- Never appears on start destination
- App-controlled

**Back Button (System):**

- Can exit the app (from start destination)
- Navigates in reverse chronological order
- System-controlled
- Always available

```
┌──────────────────────────────────────────────┐
│          App Bar                             │
│  ← Up Button (Never exits app)               │
└──────────────────────────────────────────────┘

┌──────────────────────────────────────────────┐
│      System Navigation Bar                   │
│  ← Back Button (Can exit app)                │
└──────────────────────────────────────────────┘
```

---

### Principle 3: Back Stack is Navigation State

```kotlin
// Current state is in the back stack
val currentRoute = navController.currentBackStackEntry?.destination?.route
```

**Important:**

- Don't maintain separate state for navigation
- Back stack represents your navigation state
- Trust the back stack

---

### Principle 4: Deep Links Create Realistic Stacks

When user deep links to a screen, create a realistic back stack.

**Normal Flow:**

```
Home → Category → Product → Details
```

**Deep Link to Product Details Should Create:**

```
Home → Category → Product → Details
```

**Not:**

```
Details (alone)
```

**Why?** So user can navigate back naturally.

---

## 1.5 Back Stack Concept

The back stack is a **stack data structure** (LIFO - Last In, First Out) that holds navigation history.

### How It Works

**Initial State:**

```
┌─────────────────────────┐
│  Home (Bottom)          │  ← Start Destination
└─────────────────────────┘
```

**Navigate to Profile:**

```
┌─────────────────────────┐
│  Profile (Top)          │  ← Current Screen
├─────────────────────────┤
│  Home (Bottom)          │
└─────────────────────────┘
```

**Navigate to Settings:**

```
┌─────────────────────────┐
│  Settings (Top)         │  ← Current Screen
├─────────────────────────┤
│  Profile                │
├─────────────────────────┤
│  Home (Bottom)          │  ← Start Destination
└─────────────────────────┘
```

**Press Back:**

```
┌─────────────────────────┐
│  Profile (Top)          │  ← Back to Profile
├─────────────────────────┤
│  Home (Bottom)          │
└─────────────────────────┘
```

**Press Back Again:**

```
┌─────────────────────────┐
│  Home (Top/Bottom)      │  ← Back to Home
└─────────────────────────┘
```

**Press Back Again:**

```
Empty stack → App exits
```

### Key Points

- Stack grows upward when navigating forward
- Back button pops top destination
- Start destination is always at bottom
- Empty stack means app exits
- Each entry has its own saved state

---

# Chapter 2: Navigation 2 - String-Based Routes

## 2.1 Overview & When to Use

### What is Navigation 2?

Traditional navigation approach using string-based routes.

**Characteristics:**

- Routes are strings: `"home"`, `"profile/{userId}"`
- Manual argument parsing
- Runtime type checking
- More boilerplate code

### When to Use Navigation 2

✅ **Use Navigation 2 when:**

- Working on existing/legacy projects already using it
- Quick prototypes where type safety isn't critical
- Team is unfamiliar with Kotlin Serialization
- Simple navigation with few screens
- Android Studio version doesn't support Nav 3

❌ **Don't use Navigation 2 for:**

- New projects (use Nav 3 instead)
- Complex apps with many screens
- When type safety is important

---

## 2.2 Setup & Dependencies

### Gradle Setup

```gradle
// build.gradle.kts (Module level)
dependencies {
    val navVersion = "2.9.7"
    
    // Navigation Compose library
    implementation("androidx.navigation:navigation-compose:$navVersion")
}
```

**That's it!** No serialization plugin needed for Nav 2.

### Version Compatibility

|Nav Compose Version|Compose Version|Minimum API|
|---|---|---|
|2.9.7|1.9.0|21|
|2.8.0+|1.7.0|21|
|2.7.0+|1.5.0|21|

---

## 2.3 Defining Routes

Create a sealed class to define all routes centrally.

### Basic Route Structure

```kotlin
// navigation/Screen.kt
package com.example.app.navigation

sealed class Screen(val route: String) {
    // Static routes (no arguments)
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Login : Screen("login")
    object Register : Screen("register")
    object Settings : Screen("settings")
}
```

### Routes with Arguments

```kotlin
sealed class Screen(val route: String) {
    // ... other routes
    
    // Single argument
    object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    
    // Multiple arguments
    object BookDetails : Screen("book/{bookId}/{title}") {
        fun createRoute(bookId: String, title: String) = 
            "book/$bookId/$title"
    }
    
    // Optional arguments with query parameters
    object Search : Screen("search?query={query}") {
        fun createRoute(query: String = "") = 
            if (query.isEmpty()) "search" else "search?query=$query"
    }
}
```

### Pattern Explanation

**Route Template:**

```kotlin
"profile/{userId}"  // Template with placeholder
```

**Helper Function:**

```kotlin
fun createRoute(userId: String) = "profile/$userId"
```

**Usage:**

```kotlin
// Instead of manually building: "profile/user123"
// Use helper:
Screen.Profile.createRoute("user123")  // Returns "profile/user123"
```

---

## 2.4 Creating Navigation Graph

### Simple Navigation Graph

```kotlin
// navigation/AppNavGraph.kt
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }
        
        // Login screen
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route)
                }
            )
        }
        
        // Home screen
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToProfile = { userId ->
                    navController.navigate(
                        Screen.Profile.createRoute(userId)
                    )
                }
            )
        }
    }
}
```

### Graph with Arguments

```kotlin
NavHost(
    navController = navController,
    startDestination = Screen.Home.route
) {
    composable(Screen.Home.route) {
        HomeScreen()
    }
    
    // Screen with single argument
    composable(
        route = Screen.Profile.route,
        arguments = listOf(
            navArgument("userId") { 
                type = NavType.StringType 
            }
        )
    ) { backStackEntry ->
        // Extract argument
        val userId = backStackEntry.arguments?.getString("userId") ?: ""
        
        ProfileScreen(userId = userId)
    }
    
    // Screen with multiple arguments
    composable(
        route = Screen.BookDetails.route,
        arguments = listOf(
            navArgument("bookId") { 
                type = NavType.StringType 
            },
            navArgument("title") { 
                type = NavType.StringType 
            }
        )
    ) { backStackEntry ->
        val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
        val title = backStackEntry.arguments?.getString("title") ?: ""
        
        BookDetailsScreen(
            bookId = bookId,
            title = title
        )
    }
}
```

---

## 2.5 NavHost & NavController

### Creating NavController

```kotlin
@Composable
fun AppNavigation() {
    // Create NavController
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // Define destinations
    }
}
```

### NavHost Parameters

```kotlin
NavHost(
    navController: NavHostController,    // Required
    startDestination: String,            // Required
    modifier: Modifier = Modifier,       // Optional
    route: String? = null,               // Optional - for nested graphs
    builder: NavGraphBuilder.() -> Unit  // Required - destinations
)
```

### Getting NavController in Child Composables

**Option 1: Pass Callbacks (Recommended)**

```kotlin
@Composable
fun HomeScreen(
    onNavigateToProfile: (String) -> Unit
) {
    Button(onClick = { onNavigateToProfile("user123") }) {
        Text("Go to Profile")
    }
}

// In NavHost
composable(Screen.Home.route) {
    HomeScreen(
        onNavigateToProfile = { userId ->
            navController.navigate(Screen.Profile.createRoute(userId))
        }
    )
}
```

**Option 2: Pass NavController (Not Recommended)**

```kotlin
@Composable
fun HomeScreen(navController: NavHostController) {
    Button(onClick = { 
        navController.navigate(Screen.Profile.createRoute("user123")) 
    }) {
        Text("Go to Profile")
    }
}
```

**Why Option 1 is better:**

- Decouples screen from navigation
- Easier to test
- Reusable composables
- Better architecture

---

## 2.6 Passing Arguments

### Argument Types

```kotlin
// String argument
navArgument("name") { 
    type = NavType.StringType 
}

// Int argument
navArgument("age") { 
    type = NavType.IntType 
}

// Boolean argument
navArgument("isEnabled") { 
    type = NavType.BoolType 
}

// Float argument
navArgument("rating") { 
    type = NavType.FloatType 
}

// Long argument
navArgument("timestamp") { 
    type = NavType.LongType 
}
```

### Required Arguments

```kotlin
sealed class Screen(val route: String) {
    object UserProfile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
}

// In NavHost
composable(
    route = Screen.UserProfile.route,
    arguments = listOf(
        navArgument("userId") { 
            type = NavType.StringType 
        }
    )
) { backStackEntry ->
    val userId = backStackEntry.arguments?.getString("userId") ?: ""
    ProfileScreen(userId)
}

// Navigate
navController.navigate(Screen.UserProfile.createRoute("user123"))
```

### Optional Arguments

```kotlin
sealed class Screen(val route: String) {
    object Search : Screen("search?query={query}&page={page}") {
        fun createRoute(
            query: String = "",
            page: Int = 1
        ) = buildString {
            append("search")
            val params = mutableListOf<String>()
            if (query.isNotEmpty()) params.add("query=$query")
            if (page > 1) params.add("page=$page")
            if (params.isNotEmpty()) {
                append("?")
                append(params.joinToString("&"))
            }
        }
    }
}

// In NavHost
composable(
    route = Screen.Search.route,
    arguments = listOf(
        navArgument("query") {
            type = NavType.StringType
            defaultValue = ""  // Default value
            nullable = true
        },
        navArgument("page") {
            type = NavType.IntType
            defaultValue = 1
        }
    )
) { backStackEntry ->
    val query = backStackEntry.arguments?.getString("query") ?: ""
    val page = backStackEntry.arguments?.getInt("page") ?: 1
    
    SearchScreen(query = query, page = page)
}

// Navigate with all arguments
navController.navigate(Screen.Search.createRoute("kotlin", 2))

// Navigate with some arguments
navController.navigate(Screen.Search.createRoute("kotlin"))

// Navigate with defaults
navController.navigate(Screen.Search.createRoute())
```

### Argument Extraction Patterns

**Pattern 1: Direct Extraction**

```kotlin
val userId = backStackEntry.arguments?.getString("userId") ?: ""
```

**Pattern 2: Safe Extraction with Validation**

```kotlin
val userId = backStackEntry.arguments?.getString("userId")
if (userId.isNullOrEmpty()) {
    ErrorScreen(message = "Invalid user ID")
} else {
    ProfileScreen(userId)
}
```

**Pattern 3: Extension Function**

```kotlin
fun NavBackStackEntry.getStringArg(key: String, default: String = ""): String {
    return arguments?.getString(key) ?: default
}

// Usage
val userId = backStackEntry.getStringArg("userId")
```

---

## 2.7 Navigation Actions

### Basic Navigation

```kotlin
// Navigate to a screen
navController.navigate(Screen.Profile.route)

// Navigate with arguments
navController.navigate(Screen.Profile.createRoute("user123"))
```

### Navigate Back

```kotlin
// Go back to previous screen
navController.popBackStack()

// Go back to specific destination
navController.popBackStack(
    route = Screen.Home.route,
    inclusive = false  // Keep Home in stack
)
```

### Clear Back Stack

```kotlin
// Navigate and clear everything
navController.navigate(Screen.Home.route) {
    popUpTo(navController.graph.startDestinationId) {
        inclusive = true
    }
}
```

### Replace Current Screen

```kotlin
navController.navigate(Screen.Home.route) {
    popUpTo(Screen.Login.route) {
        inclusive = true  // Remove Login too
    }
}
```

---

## 2.8 Nested Navigation

Nested navigation allows you to group related screens into sub-graphs.

### Creating Nested Graph

```kotlin
// Define graph routes
const val AUTH_ROUTE = "auth_graph"
const val MAIN_ROUTE = "main_graph"

// Auth graph function
fun NavGraphBuilder.authNavGraph(navController: NavHostController) {
    navigation(
        startDestination = Screen.Login.route,
        route = AUTH_ROUTE
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(MAIN_ROUTE) {
                        popUpTo(AUTH_ROUTE) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen()
        }
        
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen()
        }
    }
}

// Main graph function
fun NavGraphBuilder.mainNavGraph(navController: NavHostController) {
    navigation(
        startDestination = Screen.Home.route,
        route = MAIN_ROUTE
    ) {
        composable(Screen.Home.route) {
            HomeScreen()
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen()
        }
    }
}
```

### Using Nested Graphs

```kotlin
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = AUTH_ROUTE
    ) {
        authNavGraph(navController)
        mainNavGraph(navController)
    }
}
```

### Navigation Between Graphs

```kotlin
// From auth graph to main graph
navController.navigate(MAIN_ROUTE) {
    popUpTo(AUTH_ROUTE) { inclusive = true }
}

// From main graph to auth graph
navController.navigate(AUTH_ROUTE) {
    popUpTo(MAIN_ROUTE) { inclusive = true }
}
```

---

## 2.9 Bottom Navigation

### Bottom Navigation Setup

```kotlin
@Composable
fun MainScreen() {
    // Separate NavController for bottom tabs
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen()
            }
            
            composable(Screen.Search.route) {
                SearchScreen()
            }
            
            composable(Screen.Profile.route) {
                ProfileScreen()
            }
        }
    }
}
```

### Bottom Navigation Bar

```kotlin
@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem(Screen.Home.route, "Home", Icons.Default.Home),
        BottomNavItem(Screen.Search.route, "Search", Icons.Default.Search),
        BottomNavItem(Screen.Profile.route, "Profile", Icons.Default.Person)
    )
    
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop to start destination
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Avoid multiple copies
                        launchSingleTop = true
                        // Restore state when reselecting
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) }
            )
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
```

---

## 2.10 Deep Linking

### Setup Deep Links

```kotlin
composable(
    route = Screen.BookDetails.route,
    arguments = listOf(
        navArgument("bookId") { type = NavType.StringType }
    ),
    deepLinks = listOf(
        navDeepLink { 
            uriPattern = "myapp://book/{bookId}" 
        },
        navDeepLink {
            uriPattern = "https://myapp.com/book/{bookId}"
        }
    )
) { backStackEntry ->
    val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
    BookDetailsScreen(bookId)
}
```

### AndroidManifest.xml Configuration

```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
    
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        
        <data
            android:scheme="myapp"
            android:host="book" />
    </intent-filter>
    
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        
        <data
            android:scheme="https"
            android:host="myapp.com"
            android:pathPrefix="/book" />
    </intent-filter>
</activity>
```

### Testing Deep Links

```bash
# Test custom scheme
adb shell am start -W -a android.intent.action.VIEW \
  -d "myapp://book/123"

# Test https scheme
adb shell am start -W -a android.intent.action.VIEW \
  -d "https://myapp.com/book/123"
```

---

## 2.11 Best Practices

### ✅ DO:

**1. Use Sealed Class for Routes**

```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
}
```

**2. Provide Helper Functions**

```kotlin
object Profile : Screen("profile/{userId}") {
    fun createRoute(userId: String) = "profile/$userId"
}

// Usage
navController.navigate(Screen.Profile.createRoute("user123"))
```

**3. Handle Null Arguments**

```kotlin
val userId = backStackEntry.arguments?.getString("userId")
if (userId.isNullOrEmpty()) {
    ErrorScreen()
} else {
    ProfileScreen(userId)
}
```

**4. Pass Callbacks, Not NavController**

```kotlin
@Composable
fun HomeScreen(onNavigateToProfile: (String) -> Unit) {
    Button(onClick = { onNavigateToProfile("user123") })
}
```

**5. Use Constants for Graph Routes**

```kotlin
object NavConstants {
    const val AUTH_ROUTE = "auth_graph"
    const val MAIN_ROUTE = "main_graph"
}
```

### ❌ DON'T:

**1. Don't Hardcode Strings**

```kotlin
// ❌ Bad
navController.navigate("profile/user123")

// ✅ Good
navController.navigate(Screen.Profile.createRoute("user123"))
```

**2. Don't Use Force Unwrap**

```kotlin
// ❌ Bad - can crash
val userId = backStackEntry.arguments?.getString("userId")!!

// ✅ Good
val userId = backStackEntry.arguments?.getString("userId") ?: ""
```

**3. Don't Pass Complex Objects**

```kotlin
// ❌ Bad
object BookDetails : Screen("book/{book}") {
    // Can't pass Book object in URL
}

// ✅ Good - pass ID instead
object BookDetails : Screen("book/{bookId}") {
    fun createRoute(bookId: String) = "book/$bookId"
}
```

**4. Don't Forget Argument Types**

```kotlin
// ❌ Bad - type mismatch
navController.navigate("book/abc")  // String passed

composable(
    route = "book/{id}",
    arguments = listOf(
        navArgument("id") { type = NavType.IntType }  // Expects Int
    )
)
// This will crash!

// ✅ Good - match types
navArgument("id") { type = NavType.StringType }
```

---

# Chapter 3: Navigation 3 - Type-Safe Routes

## 3.1 Overview & When to Use

### What is Navigation 3?

Modern navigation approach using Kotlin Serialization for type-safe routes.

**Characteristics:**

- Routes are Kotlin objects/data classes
- Automatic argument handling
- Compile-time type checking
- Less boilerplate code
- Better IDE support

### When to Use Navigation 3

✅ **Use Navigation 3 when:**

- Starting a new project (2024+)
- Complex navigation with many screens and arguments
- Type safety is important
- Large codebase (refactoring safety)
- Multiple developers (compile-time error detection)
- Want less boilerplate code

❌ **Don't use Navigation 3 for:**

- Very old Android Studio versions
- Team unfamiliar with Kotlin Serialization
- Quick prototypes where setup time matters

---

## 3.2 Setup & Dependencies

### Gradle Setup

```gradle
// build.gradle.kts (Project level)
plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
}

// build.gradle.kts (Module level)
plugins {
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    val navVersion = "2.9.7"
    
    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:$navVersion")
    
    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
```

### Minimum Requirements

- Navigation Compose 2.8.0+
- Kotlin 1.9.0+
- Android Gradle Plugin 8.0+

---

## 3.3 Defining Routes with Serialization

### Simple Routes (No Arguments)

```kotlin
// navigation/Route.kt
package com.example.app.navigation

import kotlinx.serialization.Serializable

// Static screens (no data)
@Serializable
object Splash

@Serializable
object Login

@Serializable
object Register

@Serializable
object Home

@Serializable
object Settings
```

### Routes with Arguments

```kotlin
import kotlinx.serialization.Serializable

// Single argument
@Serializable
data class Profile(val userId: String)

// Multiple arguments
@Serializable
data class BookDetails(
    val bookId: String,
    val title: String
)

// Mix of required and optional arguments
@Serializable
data class Search(
    val query: String,
    val category: String = "all",
    val page: Int = 1
)

// Complex arguments
@Serializable
data class Filter(
    val minPrice: Double,
    val maxPrice: Double,
    val inStock: Boolean = true,
    val sortBy: String = "price"
)
```

### Navigation Graphs as Routes

```kotlin
@Serializable
object AuthGraph

@Serializable
object MainGraph

@Serializable
object SettingsGraph
```

### Important Rules

**✅ DO:**

- Use `@Serializable` on all route classes
- Use `object` for screens without arguments
- Use `data class` for screens with arguments
- Use primitive types (String, Int, Boolean, etc.)
- Provide default values for optional arguments

**❌ DON'T:**

- Forget `@Serializable` annotation
- Use complex objects as arguments
- Use nullable types unnecessarily
- Pass entire objects (pass IDs instead)

---

## 3.4 Creating Navigation Graph

### Simple Navigation Graph

```kotlin
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Splash
    ) {
        composable<Splash> {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Login)
                }
            )
        }
        
        composable<Login> {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Home)
                }
            )
        }
        
        composable<Home> {
            HomeScreen(
                onNavigateToProfile = { userId ->
                    navController.navigate(Profile(userId))
                }
            )
        }
    }
}
```

### Graph with Arguments

```kotlin
NavHost(
    navController = navController,
    startDestination = Home
) {
    composable<Home> {
        HomeScreen(
            onNavigateToBook = { bookId ->
                navController.navigate(
                    BookDetails(
                        bookId = bookId,
                        title = "Sample Book"
                    )
                )
            }
        )
    }
    
    // Extract arguments automatically
    composable<BookDetails> { backStackEntry ->
        val args = backStackEntry.toRoute<BookDetails>()
        
        BookDetailsScreen(
            bookId = args.bookId,
            title = args.title
        )
    }
    
    // With optional arguments
    composable<Search> { backStackEntry ->
        val args = backStackEntry.toRoute<Search>()
        
        SearchScreen(
            query = args.query,
            category = args.category,
            page = args.page
        )
    }
}
```

---

## 3.5 NavHost & NavController

### Creating NavController

```kotlin
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Home  // Type-safe!
    ) {
        // Define destinations
    }
}
```

### NavHost Parameters (Nav 3)

```kotlin
NavHost(
    navController: NavHostController,
    startDestination: Any,               // Any serializable object
    modifier: Modifier = Modifier,
    route: KClass<*>? = null,           // For nested graphs
    builder: NavGraphBuilder.() -> Unit
)
```

### Getting Current Destination

```kotlin
val navBackStackEntry by navController.currentBackStackEntryAsState()
val currentRoute = navBackStackEntry?.destination?.route

// Type-safe check
val isOnHome = currentRoute == Home::class.qualifiedName
```

---

## 3.6 Passing Arguments

### Required Arguments

```kotlin
@Serializable
data class Profile(val userId: String)

// Navigate
navController.navigate(Profile(userId = "user123"))

// Receive
composable<Profile> { backStackEntry ->
    val args = backStackEntry.toRoute<Profile>()
    ProfileScreen(userId = args.userId)
}
```

### Multiple Arguments

```kotlin
@Serializable
data class BookDetails(
    val bookId: String,
    val title: String,
    val authorId: String
)

// Navigate with named arguments
navController.navigate(
    BookDetails(
        bookId = "book123",
        title = "Kotlin Guide",
        authorId = "author456"
    )
)

// Receive
composable<BookDetails> { entry ->
    val args = entry.toRoute<BookDetails>()
    
    BookDetailsScreen(
        bookId = args.bookId,
        title = args.title,
        authorId = args.authorId
    )
}
```

### Optional Arguments

```kotlin
@Serializable
data class Search(
    val query: String,
    val category: String = "all",
    val page: Int = 1,
    val sortBy: String = "relevance"
)

// Navigate with all arguments
navController.navigate(
    Search(
        query = "kotlin",
        category = "books",
        page = 2,
        sortBy = "date"
    )
)

// Navigate with only required arguments
navController.navigate(Search(query = "kotlin"))

// Navigate with some optional arguments
navController.navigate(
    Search(
        query = "kotlin",
        page = 3
    )
)
```

### Nullable Arguments

```kotlin
@Serializable
data class UserProfile(
    val userId: String,
    val highlightSection: String? = null
)

// Navigate without optional
navController.navigate(UserProfile(userId = "123"))

// Navigate with optional
navController.navigate(
    UserProfile(
        userId = "123",
        highlightSection = "posts"
    )
)
```

### Lists as Arguments

```kotlin
@Serializable
data class Gallery(
    val imageIds: List<String>
)

// Navigate
navController.navigate(
    Gallery(imageIds = listOf("img1", "img2", "img3"))
)

// Receive
composable<Gallery> { entry ->
    val args = entry.toRoute<Gallery>()
    GalleryScreen(imageIds = args.imageIds)
}
```

### Enums as Arguments

```kotlin
enum class SortOrder {
    ASCENDING,
    DESCENDING
}

@Serializable
data class ProductList(
    val category: String,
    val sortOrder: SortOrder = SortOrder.ASCENDING
)

// Navigate
navController.navigate(
    ProductList(
        category = "electronics",
        sortOrder = SortOrder.DESCENDING
    )
)
```

---

## 3.7 Navigation Actions

### Basic Navigation

```kotlin
// Navigate to screen without arguments
navController.navigate(Home)

// Navigate with arguments
navController.navigate(Profile(userId = "user123"))

// Navigate with multiple arguments
navController.navigate(
    BookDetails(
        bookId = "book456",
        title = "Sample"
    )
)
```

### Navigate Back

```kotlin
// Go back to previous screen
navController.popBackStack()

// Go back to specific destination
navController.popBackStack<Home>(inclusive = false)
```

### Clear Back Stack

```kotlin
// Navigate and clear everything
navController.navigate(Home) {
    popUpTo(navController.graph.startDestinationId) {
        inclusive = true
    }
}

// Clear specific graph
navController.navigate(MainGraph) {
    popUpTo<AuthGraph> {
        inclusive = true
    }
}
```

### Replace Current Screen

```kotlin
navController.navigate(Home) {
    popUpTo<Login> {
        inclusive = true
    }
}
```

### Navigation with Options

```kotlin
navController.navigate(Profile(userId = "123")) {
    // Pop up to start destination
    popUpTo(navController.graph.startDestinationId) {
        saveState = true
    }
    
    // Avoid multiple copies of same destination
    launchSingleTop = true
    
    // Restore state when navigating back
    restoreState = true
}
```

---

## 3.8 Nested Navigation

### Defining Nested Graphs

```kotlin
@Serializable object AuthGraph
@Serializable object MainGraph

@Serializable object Login
@Serializable object Register
@Serializable object ForgotPassword

@Serializable object Home
@Serializable object Profile
@Serializable object Settings
```

### Creating Nested Graph

```kotlin
fun NavGraphBuilder.authNavGraph(
    onNavigateToMain: () -> Unit
) {
    navigation<AuthGraph>(
        startDestination = Login
    ) {
        composable<Login> {
            LoginScreen(
                onLoginSuccess = { email ->
                    onNavigateToMain()
                },
                onNavigateToRegister = {
                    // Navigate within auth graph
                }
            )
        }
        
        composable<Register> {
            RegisterScreen(
                onNavigateBack = {
                    // Pop back
                }
            )
        }
        
        composable<ForgotPassword> {
            ForgotPasswordScreen()
        }
    }
}

fun NavGraphBuilder.mainNavGraph() {
    navigation<MainGraph>(
        startDestination = Home
    ) {
        composable<Home> {
            HomeScreen()
        }
        
        composable<Profile> { entry ->
            val args = entry.toRoute<Profile>()
            ProfileScreen(userId = args.userId)
        }
        
        composable<Settings> {
            SettingsScreen()
        }
    }
}
```

### Using Nested Graphs

```kotlin
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = AuthGraph
    ) {
        authNavGraph(
            onNavigateToMain = {
                navController.navigate(MainGraph) {
                    popUpTo<AuthGraph> { inclusive = true }
                }
            }
        )
        
        mainNavGraph()
    }
}
```

---

## 3.9 Bottom Navigation

### Bottom Navigation Setup

```kotlin
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Home,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable<Home> {
                HomeScreen()
            }
            
            composable<Search> {
                SearchScreen()
            }
            
            composable<Profile> {
                ProfileScreen()
            }
        }
    }
}
```

### Bottom Navigation Bar

```kotlin
@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem(Home, "Home", Icons.Default.Home),
        BottomNavItem(Search, "Search", Icons.Default.Search),
        BottomNavItem(Profile(), "Profile", Icons.Default.Person)
    )
    
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        
        items.forEach { item ->
            val isSelected = currentDestination?.route == 
                item.route::class.qualifiedName
            
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) }
            )
        }
    }
}

data class BottomNavItem(
    val route: Any,  // Can be any serializable object
    val label: String,
    val icon: ImageVector
)
```

---

## 3.10 Deep Linking

### Setup Type-Safe Deep Links

```kotlin
import androidx.navigation.navDeepLink

composable<BookDetails>(
    deepLinks = listOf(
        navDeepLink<BookDetails>(
            basePath = "myapp://book"
        ),
        navDeepLink<BookDetails>(
            basePath = "https://myapp.com/book"
        )
    )
) { entry ->
    val args = entry.toRoute<BookDetails>()
    BookDetailsScreen(
        bookId = args.bookId,
        title = args.title
    )
}
```

### AndroidManifest.xml

```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
    
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        
        <data
            android:scheme="myapp"
            android:host="book" />
    </intent-filter>
    
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        
        <data
            android:scheme="https"
            android:host="myapp.com"
            android:pathPrefix="/book" />
    </intent-filter>
</activity>
```

### Testing Deep Links

```bash
# Format: myapp://book?bookId=123&title=Sample
adb shell am start -W -a android.intent.action.VIEW \
  -d "myapp://book?bookId=123&title=Sample"
```

---

## 3.11 Best Practices

### ✅ DO:

**1. Use Objects for Simple Screens**

```kotlin
@Serializable
object Home

@Serializable
object Settings
```

**2. Use Data Classes for Screens with Arguments**

```kotlin
@Serializable
data class Profile(val userId: String)

@Serializable
data class BookDetails(val bookId: String, val title: String)
```

**3. Provide Default Values**

```kotlin
@Serializable
data class Search(
    val query: String,
    val page: Int = 1,
    val sortBy: String = "relevance"
)
```

**4. Keep Routes in Separate File**

```kotlin
// navigation/Route.kt
@Serializable object Home
@Serializable data class Profile(val userId: String)
@Serializable object AuthGraph
```

**5. Use Type-Safe Navigation**

```kotlin
// ✅ Compile-time safe
navController.navigate(Profile(userId = "123"))

// ❌ Not possible - won't compile
navController.navigate(Profile())  // Error: missing userId
```

### ❌ DON'T:

**1. Don't Forget @Serializable**

```kotlin
// ❌ Won't work
data class Profile(val userId: String)

// ✅ Correct
@Serializable
data class Profile(val userId: String)
```

**2. Don't Pass Complex Objects**

```kotlin
// ❌ Bad - complex object
@Serializable
data class BookDetails(val book: Book)

// ✅ Good - pass ID
@Serializable
data class BookDetails(val bookId: String)
```

**3. Don't Use Object for Routes with Data**

```kotlin
// ❌ Bad - can't hold data
@Serializable
object Profile

// ✅ Good - use data class
@Serializable
data class Profile(val userId: String)
```

**4. Don't Mix Navigation Styles**

```kotlin
// ❌ Bad - mixing Nav 2 and Nav 3
composable<Home> { }  // Nav 3
composable("profile/{id}") { }  // Nav 2

// ✅ Good - consistent style
composable<Home> { }
composable<Profile> { }
```

---

# Chapter 4: Common Concepts (Both Versions)

## 4.1 Back Stack Management

### Understanding Back Stack

The back stack is shared between both Nav 2 and Nav 3.

**Basic Operations:**

```kotlin
// Get current entry
val currentEntry = navController.currentBackStackEntry

// Get previous entry
val previousEntry = navController.previousBackStackEntry

// Check if can go back
val canGoBack = navController.previousBackStackEntry != null
```

### Pop Operations

**Nav 2:**

```kotlin
// Pop current
navController.popBackStack()

// Pop to specific route
navController.popBackStack(
    route = "home",
    inclusive = false
)

// Pop to start
navController.popBackStack(
    route = navController.graph.startDestinationRoute!!,
    inclusive = false
)
```

**Nav 3:**

```kotlin
// Pop current
navController.popBackStack()

// Pop to specific destination
navController.popBackStack<Home>(inclusive = false)

// Pop to start
navController.popBackStack(
    route = navController.graph.startDestinationId,
    inclusive = false
)
```

### Clear Entire Back Stack

**Both versions:**

```kotlin
navController.navigate(destination) {
    popUpTo(navController.graph.startDestinationId) {
        inclusive = true
    }
}
```

---

## 4.2 Navigation Options

### popUpTo

Removes destinations from back stack up to a certain point.

**Nav 2:**

```kotlin
navController.navigate("home") {
    popUpTo("login") {
        inclusive = true   // Remove "login" too
        saveState = true   // Save state for restoration
    }
}
```

**Nav 3:**

```kotlin
navController.navigate(Home) {
    popUpTo<Login> {
        inclusive = true
        saveState = true
    }
}
```

**Before:**

```
Login → Register → Verification → Home
```

**After (inclusive = true):**

```
Home
```

**After (inclusive = false):**

```
Login → Home
```

### launchSingleTop

Prevents creating duplicate instances.

```kotlin
navController.navigate(destination) {
    launchSingleTop = true
}
```

**Without launchSingleTop:**

```
Home → Profile → Profile → Profile
```

**With launchSingleTop:**

```
Home → Profile  (reuses existing Profile)
```

### restoreState

Restores previously saved state.

```kotlin
navController.navigate(destination) {
    popUpTo(startDestination) {
        saveState = true
    }
    restoreState = true  // Restore when coming back
}
```

**Use Case:** Bottom navigation tabs maintaining scroll position, form data, etc.

### Complete Example

```kotlin
// Typical bottom nav pattern
navController.navigate(destination) {
    popUpTo(navController.graph.startDestinationId) {
        saveState = true      // Save current state
    }
    launchSingleTop = true   // Don't duplicate
    restoreState = true      // Restore previous state
}
```

---

## 4.3 Shared ViewModel

Share ViewModel between multiple destinations.

### Scope to Navigation Graph

**Nav 2:**

```kotlin
composable("details") { backStackEntry ->
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry("parent_route")
    }
    
    val sharedViewModel: SharedViewModel = hiltViewModel(parentEntry)
    
    DetailsScreen(viewModel = sharedViewModel)
}
```

**Nav 3:**

```kotlin
composable<Details> { backStackEntry ->
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry<ParentRoute>()
    }
    
    val sharedViewModel: SharedViewModel = hiltViewModel(parentEntry)
    
    DetailsScreen(viewModel = sharedViewModel)
}
```

### Scope to Activity

```kotlin
@Composable
fun MyScreen() {
    val viewModel: MyViewModel = hiltViewModel()  // Activity-scoped
    
    // ...
}
```

---

## 4.4 Save & Restore State

### SavedStateHandle

```kotlin
class ProfileViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    var scrollPosition by savedStateHandle.saveable { mutableStateOf(0) }
    
    var formData by savedStateHandle.saveable {
        mutableStateOf(FormData())
    }
}
```

### rememberSaveable

```kotlin
@Composable
fun MyScreen() {
    var scrollState by rememberSaveable { mutableStateOf(0) }
    
    LazyColumn(
        state = rememberLazyListState(
            initialFirstVisibleItemIndex = scrollState
        )
    ) {
        // ...
    }
}
```

---

## 4.5 Testing Navigation

### Nav 2 Testing

```kotlin
@Test
fun testNavigation() {
    val navController = TestNavHostController(
        ApplicationProvider.getApplicationContext()
    )
    
    navController.navigatorProvider.addNavigator(
        ComposeNavigator()
    )
    
    composeTestRule.setContent {
        navController.graph = navController.createGraph(
            startDestination = "home"
        ) {
            composable("home") { HomeScreen() }
            composable("profile") { ProfileScreen() }
        }
    }
    
    navController.navigate("profile")
    
    assertEquals("profile", navController.currentDestination?.route)
}
```

### Nav 3 Testing

```kotlin
@Test
fun testNavigation() {
    val navController = TestNavHostController(
        ApplicationProvider.getApplicationContext()
    )
    
    navController.navigatorProvider.addNavigator(
        ComposeNavigator()
    )
    
    composeTestRule.setContent {
        NavHost(navController, startDestination = Home) {
            composable<Home> { HomeScreen() }
            composable<Profile> { ProfileScreen() }
        }
    }
    
    navController.navigate(Profile(userId = "123"))
    
    val route = navController.currentBackStackEntry?.destination?.route
    assertEquals(Profile::class.qualifiedName, route)
}
```

---

## 4.6 Common Patterns

### Authentication Flow

```
Splash → Check Auth → Login/Home
           ↓
        [Logged In?]
        ↓         ↓
       Yes        No
        ↓         ↓
      Home      Login
```

**Implementation:**

```kotlin
composable<Splash> {
    SplashScreen(
        onNavigateToLogin = {
            navController.navigate(Login) {
                popUpTo<Splash> { inclusive = true }
            }
        },
        onNavigateToHome = {
            navController.navigate(Home) {
                popUpTo<Splash> { inclusive = true }
            }
        }
    )
}
```

### Multi-Step Form (Wizard)

```
Step1 → Step2 → Step3 → Complete
```

**Implementation:**

```kotlin
@Serializable data class Step2(val step1Data: String)
@Serializable data class Step3(val step1Data: String, val step2Data: String)

composable<Step1> {
    Step1Screen(
        onNext = { data ->
            navController.navigate(Step2(step1Data = data))
        }
    )
}

composable<Step2> { entry ->
    val args = entry.toRoute<Step2>()
    Step2Screen(
        step1Data = args.step1Data,
        onNext = { step2Data ->
            navController.navigate(
                Step3(
                    step1Data = args.step1Data,
                    step2Data = step2Data
                )
            )
        }
    )
}
```

### Master-Detail Pattern

```
List → Details → Edit
         ↓
       Share
```

**Implementation:**

```kotlin
composable<ItemList> {
    ItemListScreen(
        onItemClick = { itemId ->
            navController.navigate(ItemDetails(itemId))
        }
    )
}

composable<ItemDetails> { entry ->
    val args = entry.toRoute<ItemDetails>()
    ItemDetailsScreen(
        itemId = args.itemId,
        onEdit = {
            navController.navigate(EditItem(args.itemId))
        },
        onShare = {
            navController.navigate(ShareItem(args.itemId))
        }
    )
}
```

---

# Chapter 5: Quick Reference

## 5.1 Navigation 2 Cheat Sheet

### Define Routes

```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
}
```

### Create NavHost

```kotlin
NavHost(
    navController = navController,
    startDestination = Screen.Home.route
) {
    composable(Screen.Home.route) {
        HomeScreen()
    }
    
    composable(
        route = Screen.Profile.route,
        arguments = listOf(
            navArgument("userId") { type = NavType.StringType }
        )
    ) { entry ->
        val userId = entry.arguments?.getString("userId") ?: ""
        ProfileScreen(userId)
    }
}
```

### Navigate

```kotlin
// Simple
navController.navigate(Screen.Home.route)

// With arguments
navController.navigate(Screen.Profile.createRoute("user123"))

// With options
navController.navigate(Screen.Home.route) {
    popUpTo(Screen.Login.route) { inclusive = true }
    launchSingleTop = true
}
```

### Navigate Back

```kotlin
navController.popBackStack()
navController.popBackStack("home", inclusive = false)
```

---

## 5.2 Navigation 3 Cheat Sheet

### Define Routes

```kotlin
@Serializable
object Home

@Serializable
data class Profile(val userId: String)
```

### Create NavHost

```kotlin
NavHost(
    navController = navController,
    startDestination = Home
) {
    composable<Home> {
        HomeScreen()
    }
    
    composable<Profile> { entry ->
        val args = entry.toRoute<Profile>()
        ProfileScreen(userId = args.userId)
    }
}
```

### Navigate

```kotlin
// Simple
navController.navigate(Home)

// With arguments
navController.navigate(Profile(userId = "user123"))

// With options
navController.navigate(Home) {
    popUpTo<Login> { inclusive = true }
    launchSingleTop = true
}
```

### Navigate Back

```kotlin
navController.popBackStack()
navController.popBackStack<Home>(inclusive = false)
```

---

## 5.3 Comparison Table

|Feature|Navigation 2|Navigation 3|
|---|---|---|
|**Route Type**|String|Kotlin Object|
|**Arguments**|Manual `navArgument()`|Automatic via properties|
|**Type Safety**|Runtime|Compile-time|
|**Boilerplate**|High|Low|
|**IDE Support**|Basic|Excellent (autocomplete)|
|**Refactoring**|Manual|Automatic|
|**Setup**|Simple|Requires serialization plugin|
|**Learning Curve**|Easy|Medium|
|**Best For**|Legacy, Simple|New projects, Complex|

---

## 5.4 Decision Guide

```
Starting a new project?
    ├─ Yes → Use Navigation 3 (Type-Safe)
    └─ No
        ├─ Already using Nav 2? → Keep using Nav 2
        └─ Complex navigation? → Migrate to Nav 3

Is type safety important?
    ├─ Yes → Use Navigation 3
    └─ No → Either is fine

How many screens?
    ├─ < 10 screens → Either is fine
    └─ > 10 screens → Use Navigation 3

Multiple developers?
    ├─ Yes → Use Navigation 3 (compile-time safety)
    └─ No → Either is fine

Android Studio version?
    ├─ Latest → Use Navigation 3
    └─ Old → Use Navigation 2
```

---

## Summary

### Key Takeaways

**Navigation Fundamentals:**

- Three core components: NavGraph, NavHost, NavController
- Back stack manages navigation history
- Up button ≠ Back button
- Fixed start destination is important

**Navigation 2 (String Routes):**

- ✅ Mature, stable, well-documented
- ✅ Simple setup
- ❌ Runtime type checking
- ❌ More boilerplate
- 👉 Good for: Legacy projects, simple navigation

**Navigation 3 (Type-Safe):**

- ✅ Compile-time type safety
- ✅ Less boilerplate
- ✅ Better refactoring support
- ❌ Requires serialization setup
- 👉 Good for: New projects, complex navigation

**Common Concepts:**

- Back stack management same in both
- Navigation options: popUpTo, launchSingleTop, restoreState
- Shared ViewModels across destinations
- Testing with TestNavHostController

### Next Steps

1. Choose the navigation style for your project
2. Set up dependencies
3. Define your routes
4. Create navigation graph
5. Implement navigation actions
6. Test your navigation
7. Refer back to these notes when needed!

---

**End of Android Navigation Study Notes**

Keep this guide handy for quick reference! 📚