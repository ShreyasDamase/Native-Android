# 📚 Complete Guide to androidx.navigation:navigation-compose

**Latest Version: 2.9.6 (December 2024)**

> This guide is specifically for **beginners** with lots of comments, explanations, and real-world examples.

---

## 📖 Table of Contents

1. [Chapter 1: Setup & Basic Concepts](https://claude.ai/chat/53ab4b18-205a-4213-b8ee-8948d7c1063e#chapter-1)
2. [Chapter 2: Navigation Lifecycle](https://claude.ai/chat/53ab4b18-205a-4213-b8ee-8948d7c1063e#chapter-2)
3. [Chapter 3: Basic Navigation](https://claude.ai/chat/53ab4b18-205a-4213-b8ee-8948d7c1063e#chapter-3)
4. [Chapter 4: Passing Data Between Screens](https://claude.ai/chat/53ab4b18-205a-4213-b8ee-8948d7c1063e#chapter-4)
5. [Chapter 5: Type-Safe Navigation (Modern Approach)](https://claude.ai/chat/53ab4b18-205a-4213-b8ee-8948d7c1063e#chapter-5)
6. [Chapter 6: Authentication Flow](https://claude.ai/chat/53ab4b18-205a-4213-b8ee-8948d7c1063e#chapter-6)
7. [Chapter 7: Bottom Navigation](https://claude.ai/chat/53ab4b18-205a-4213-b8ee-8948d7c1063e#chapter-7)
8. [Chapter 8: Navigation Drawer](https://claude.ai/chat/53ab4b18-205a-4213-b8ee-8948d7c1063e#chapter-8)
9. [Chapter 9: Nested Navigation Graphs](https://claude.ai/chat/53ab4b18-205a-4213-b8ee-8948d7c1063e#chapter-9)
10. [Chapter 10: Industry Best Practices](https://claude.ai/chat/53ab4b18-205a-4213-b8ee-8948d7c1063e#chapter-10)

---

<a name="chapter-1"></a>

## 📦 Chapter 1: Setup & Basic Concepts

### What is Navigation Compose?

Navigation Compose is the official library for handling navigation (moving between screens) in Jetpack Compose apps. Think of it as a "GPS" for your app - it knows:

- Where you are (current screen)
- Where you've been (back stack)
- How to go to different places (routes)

### Setup

**Step 1:** Add to your `build.gradle.kts` (Module: app):

```kotlin
dependencies {
    // Latest stable version
    implementation("androidx.navigation:navigation-compose:2.9.6")
}
```

**Step 2:** Sync your project.

### Core Components

```kotlin
// 1. NavController - The "brain" that manages navigation
//    Think of it as the captain of a ship
val navController = rememberNavController()

// 2. NavHost - The "container" that shows current screen
//    Think of it as the stage where actors (screens) perform
NavHost(
    navController = navController,  // Who controls navigation
    startDestination = "home"       // First screen to show
) {
    // 3. composable() - Defines each screen
    //    Think of it as registering an actor for the stage
    composable("home") { 
        HomeScreen() 
    }
}
```

---

<a name="chapter-2"></a>

## 🔄 Chapter 2: Navigation Lifecycle

### Understanding the Back Stack

Imagine a stack of plates 🥞:

- **Navigate forward** → Add a new plate on top
- **Navigate back** → Remove the top plate
- **Current screen** → The top plate

```
[Screen C] ← Current (top)
[Screen B]
[Screen A] ← Start (bottom)
```

### Lifecycle Events

```kotlin
@Composable
fun MyScreen() {
    // This runs when screen is created
    LaunchedEffect(Unit) {
        println("Screen entered")
    }
    
    // This runs when screen is destroyed
    DisposableEffect(Unit) {
        onDispose {
            println("Screen left")
        }
    }
}
```

### Important Lifecycle Points

1. **onCreate** - Screen is created (composable is called)
2. **onStart** - Screen becomes visible
3. **onResume** - Screen is actively displayed
4. **onPause** - User leaves screen
5. **onStop** - Screen is no longer visible
6. **onDestroy** - Screen is removed from back stack

---

<a name="chapter-3"></a>

## 🚀 Chapter 3: Basic Navigation

### Simple Navigation Example

```kotlin
@Composable
fun AppNavigation() {
    // Step 1: Create the navigation controller
    // rememberNavController() survives recomposition
    val navController = rememberNavController()
    
    // Step 2: Set up the navigation host
    NavHost(
        navController = navController,
        startDestination = "welcome"  // First screen user sees
    ) {
        // Step 3: Register each screen with a route (unique name)
        
        // Welcome screen
        composable("welcome") {
            WelcomeScreen(
                // Pass navController so this screen can navigate
                onNavigateToLogin = {
                    // Navigate to login screen
                    navController.navigate("login")
                }
            )
        }
        
        // Login screen
        composable("login") {
            LoginScreen(
                // Navigate to home after successful login
                onLoginSuccess = {
                    navController.navigate("home")
                }
            )
        }
        
        // Home screen
        composable("home") {
            HomeScreen(navController)
        }
    }
}

// Example screen that uses navigation
@Composable
fun WelcomeScreen(onNavigateToLogin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to MyApp!")
        
        Button(onClick = onNavigateToLogin) {
            Text("Go to Login")
        }
    }
}
```

### Navigation Methods

```kotlin
// 1. Navigate forward (adds to back stack)
navController.navigate("details")

// 2. Navigate back (removes current screen)
navController.navigateUp()  // Returns true if successful
navController.popBackStack()  // Same but returns Boolean

// 3. Navigate and clear history (user can't go back)
navController.navigate("home") {
    // Remove everything up to and including "login"
    popUpTo("login") { inclusive = true }
}

// 4. Navigate without adding duplicate
navController.navigate("profile") {
    // If "profile" already exists in stack, reuse it
    launchSingleTop = true
}

// 5. Check if we can go back
if (navController.previousBackStackEntry != null) {
    // There's something to go back to
    navController.navigateUp()
}
```

---

<a name="chapter-4"></a>

## 📦 Chapter 4: Passing Data Between Screens

### Method 1: Path Parameters (Required Data)

```kotlin
// Define route with parameter placeholder
// {userId} is a placeholder - it will be replaced with actual value
composable("profile/{userId}") { backStackEntry ->
    
    // Step 1: Extract the parameter from the back stack entry
    // backStackEntry.arguments contains all route parameters
    val userId = backStackEntry.arguments?.getString("userId")
    
    // Step 2: Pass it to your screen
    ProfileScreen(userId = userId ?: "unknown")
}

// Navigate and pass the actual value
// Replace {userId} with "12345"
navController.navigate("profile/12345")
```

**Real Example:**

```kotlin
@Composable
fun UserListScreen(navController: NavController) {
    Column {
        // List of users
        listOf("user1", "user2", "user3").forEach { userId ->
            Button(
                onClick = {
                    // Navigate to profile with specific user ID
                    navController.navigate("profile/$userId")
                }
            ) {
                Text("View $userId")
            }
        }
    }
}

@Composable
fun ProfileScreen(userId: String) {
    Text("Showing profile for: $userId")
    // In real app: fetch user data using userId
}
```

### Method 2: Query Parameters (Optional Data)

```kotlin
// Define route with optional query parameter
composable(
    route = "search?query={query}",  // ? means optional
    arguments = listOf(
        navArgument("query") {
            type = NavType.StringType  // Data type
            defaultValue = ""          // Default if not provided
            nullable = false           // Can't be null
        }
    )
) { backStackEntry ->
    // Get the query parameter
    val query = backStackEntry.arguments?.getString("query") ?: ""
    SearchScreen(query = query)
}

// Navigate with query parameter
navController.navigate("search?query=android")

// Navigate without query parameter (uses default)
navController.navigate("search")
```

### Method 3: Multiple Parameters

```kotlin
// Multiple required parameters
composable(
    route = "post/{postId}/{authorId}",
    arguments = listOf(
        navArgument("postId") {
            type = NavType.IntType  // Integer parameter
        },
        navArgument("authorId") {
            type = NavType.StringType  // String parameter
        }
    )
) { backStackEntry ->
    val postId = backStackEntry.arguments?.getInt("postId") ?: 0
    val authorId = backStackEntry.arguments?.getString("authorId") ?: ""
    
    PostDetailScreen(postId = postId, authorId = authorId)
}

// Navigate with multiple parameters
navController.navigate("post/42/john_doe")
```

### Method 4: Complex Data (Don't Do This!)

```kotlin
// ❌ WRONG - Don't pass complex objects
data class User(val name: String, val age: Int)
navController.navigate("profile/${user}")  // Won't work!

// ✅ CORRECT - Pass IDs only, fetch data in destination
navController.navigate("profile/${user.id}")

// In ProfileScreen, fetch the user data
@Composable
fun ProfileScreen(userId: String, viewModel: ProfileViewModel = viewModel()) {
    // ViewModel fetches user data using the ID
    val user by viewModel.getUser(userId).collectAsState()
    
    user?.let {
        Text("Name: ${it.name}")
        Text("Age: ${it.age}")
    }
}
```

---

<a name="chapter-5"></a>

## 🎯 Chapter 5: Type-Safe Navigation (Modern Approach - 2024)

### Why Type-Safe Navigation?

**Old Way (String-based):**

```kotlin
navController.navigate("profile/123")  // Easy to make typos!
navController.navigate("profil/123")   // Compiles but crashes!
```

**New Way (Type-safe):**

```kotlin
navController.navigate(Screen.Profile(userId = "123"))  // Compiler checks!
```

### Setting Up Type-Safe Navigation

**Step 1:** Add Kotlin Serialization plugin to `build.gradle.kts` (Project level):

```kotlin
plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0" apply false
}
```

**Step 2:** Apply in `build.gradle.kts` (Module: app):

```kotlin
plugins {
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}
```

### Creating Type-Safe Routes

```kotlin
import kotlinx.serialization.Serializable

// Define your screens as serializable objects
@Serializable
object Home  // Simple screen with no parameters

@Serializable
object Login

@Serializable
data class Profile(
    val userId: String  // Parameter is type-safe!
)

@Serializable
data class PostDetail(
    val postId: Int,
    val authorId: String
)
```

### Using Type-Safe Navigation

```kotlin
@Composable
fun AppNavigationTypeSafe() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Home  // Use object directly!
    ) {
        // Home screen
        composable<Home> {
            HomeScreen(
                onNavigateToProfile = { userId ->
                    // Type-safe navigation - compiler checks!
                    navController.navigate(Profile(userId = userId))
                }
            )
        }
        
        // Profile screen
        composable<Profile> { backStackEntry ->
            // Type-safe parameter extraction
            val profile: Profile = backStackEntry.toRoute<Profile>()
            
            ProfileScreen(userId = profile.userId)
        }
        
        // Post detail screen
        composable<PostDetail> { backStackEntry ->
            val postDetail = backStackEntry.toRoute<PostDetail>()
            
            PostDetailScreen(
                postId = postDetail.postId,
                authorId = postDetail.authorId
            )
        }
    }
}
```

**Benefits:**

- ✅ Compiler catches typos
- ✅ Auto-complete in IDE
- ✅ Refactoring is safe
- ✅ No string formatting errors

---

<a name="chapter-6"></a>

## 🔐 Chapter 6: Authentication Flow (Industry Pattern)

### Goal: Protect Certain Screens

**Flow:**

1. User opens app → Check if logged in
2. If NOT logged in → Show Login
3. After login → Show Home (and remove Login from back stack)
4. User can't press back to return to Login

```kotlin
@Composable
fun AuthApp() {
    // In real app, get this from DataStore or ViewModel
    // This is just for demonstration
    var isLoggedIn by remember { mutableStateOf(false) }
    
    val navController = rememberNavController()
    
    // Decide start destination based on login status
    val startDestination = if (isLoggedIn) "main" else "auth"
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ========================================
        // AUTH GRAPH - Screens before login
        // ========================================
        navigation(
            startDestination = "login",
            route = "auth"  // Parent route
        ) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        isLoggedIn = true
                        
                        // Navigate to main app
                        navController.navigate("main") {
                            // Clear entire auth flow from back stack
                            // User can't go back to login by pressing back
                            popUpTo("auth") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate("register")
                    }
                )
            }
            
            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = {
                        isLoggedIn = true
                        navController.navigate("main") {
                            popUpTo("auth") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigateUp()
                    }
                )
            }
            
            composable("forgot_password") {
                ForgotPasswordScreen(
                    onPasswordReset = {
                        navController.navigateUp()
                    }
                )
            }
        }
        
        // ========================================
        // MAIN GRAPH - Screens after login
        // ========================================
        navigation(
            startDestination = "home",
            route = "main"  // Parent route
        ) {
            composable("home") {
                HomeScreen(
                    navController = navController,
                    onLogout = {
                        isLoggedIn = false
                        
                        // Go back to login
                        navController.navigate("auth") {
                            // Clear main app from back stack
                            popUpTo("main") { inclusive = true }
                        }
                    }
                )
            }
            
            composable("profile") {
                ProfileScreen(navController)
            }
            
            composable("settings") {
                SettingsScreen(navController)
            }
        }
    }
}

// Example Login Screen
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Login", style = MaterialTheme.typography.headlineLarge)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                // In real app: validate and call API
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    onLoginSuccess()
                }
            }
        ) {
            Text("Login")
        }
        
        TextButton(onClick = onNavigateToRegister) {
            Text("Don't have an account? Register")
        }
    }
}
```

---

<a name="chapter-7"></a>

## 📱 Chapter 7: Bottom Navigation (Tabs)

### What is Bottom Navigation?

Think Instagram, YouTube, or Twitter - they have tabs at the bottom:

- Home 🏠
- Search 🔍
- Profile 👤

### Complete Bottom Navigation Example

```kotlin
@Composable
fun BottomNavigationApp() {
    // Create nav controller for bottom tabs
    val navController = rememberNavController()
    
    // Track current screen to highlight correct tab
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination
    
    // Define tab items
    val tabs = listOf(
        BottomNavItem(
            route = "feed",
            label = "Feed",
            icon = Icons.Default.Home
        ),
        BottomNavItem(
            route = "search",
            label = "Search",
            icon = Icons.Default.Search
        ),
        BottomNavItem(
            route = "notifications",
            label = "Notifications",
            icon = Icons.Default.Notifications
        ),
        BottomNavItem(
            route = "profile",
            label = "Profile",
            icon = Icons.Default.Person
        )
    )
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        // Highlight if this is current tab
                        selected = currentDestination?.route == tab.route,
                        
                        // Handle tab click
                        onClick = {
                            navController.navigate(tab.route) {
                                // Go back to start destination of current graph
                                // to avoid building up back stack
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true  // Save tab state
                                }
                                
                                // Avoid multiple copies of same tab
                                launchSingleTop = true
                                
                                // Restore state when coming back to tab
                                restoreState = true
                            }
                        },
                        
                        // Tab icon
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        
                        // Tab label
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        // Content area (above bottom bar)
        NavHost(
            navController = navController,
            startDestination = "feed",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("feed") {
                FeedScreen(navController)
            }
            
            composable("search") {
                SearchScreen(navController)
            }
            
            composable("notifications") {
                NotificationsScreen()
            }
            
            composable("profile") {
                ProfileScreen(navController)
            }
        }
    }
}

// Data class for tab items
data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

// Example Feed Screen
@Composable
fun FeedScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Feed Screen", style = MaterialTheme.typography.headlineLarge)
        
        // Navigate to detail screen
        Button(
            onClick = {
                navController.navigate("post/123")
            }
        ) {
            Text("Open Post")
        }
    }
}
```

### Why saveState and restoreState?

```kotlin
// User is on Feed tab, scrolls down
// User clicks Search tab
// User clicks Feed tab again
// WITHOUT restoreState: Feed starts from top
// WITH restoreState: Feed remembers scroll position!
```

---

<a name="chapter-8"></a>

## 🍔 Chapter 8: Navigation Drawer (Side Menu)

### What is Navigation Drawer?

A menu that slides from the left side (like Gmail, Google Drive).

### Complete Navigation Drawer Example

```kotlin
@Composable
fun DrawerNavigationApp() {
    val navController = rememberNavController()
    
    // Controls drawer open/closed state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    // CoroutineScope to launch drawer open/close animations
    val scope = rememberCoroutineScope()
    
    // Track current route to highlight selected item
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    
    // Define drawer menu items
    val menuItems = listOf(
        DrawerMenuItem("home", "Home", Icons.Default.Home),
        DrawerMenuItem("favorites", "Favorites", Icons.Default.Favorite),
        DrawerMenuItem("settings", "Settings", Icons.Default.Settings),
        DrawerMenuItem("help", "Help", Icons.Default.Info)
    )
    
    // Drawer wrapper
    ModalNavigationDrawer(
        drawerState = drawerState,
        
        // Drawer content (the menu)
        drawerContent = {
            ModalDrawerSheet {
                // Drawer header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "My App",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        "user@example.com",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Divider()
                
                // Menu items
                menuItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            // Close drawer
                            scope.launch { drawerState.close() }
                            
                            // Navigate
                            navController.navigate(item.route) {
                                // Avoid multiple copies
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        // Main screen content
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("My App") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                // Open drawer when hamburger icon clicked
                                scope.launch { drawerState.open() }
                            }
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("home") {
                    HomeScreen()
                }
                
                composable("favorites") {
                    FavoritesScreen()
                }
                
                composable("settings") {
                    SettingsScreen()
                }
                
                composable("help") {
                    HelpScreen()
                }
            }
        }
    }
}

// Data class for drawer items
data class DrawerMenuItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
```

---

<a name="chapter-9"></a>

## 🗂️ Chapter 9: Nested Navigation Graphs

### Why Use Nested Graphs?

Imagine a shopping app:

```
App
├── Auth Flow
│   ├── Login
│   ├── Register
│   └── Forgot Password
└── Main Flow
    ├── Shop Flow
    │   ├── Products
    │   ├── Product Detail
    │   └── Cart
    ├── Orders Flow
    │   ├── Order List
    │   └── Order Detail
    └── Profile Flow
        ├── Profile
        └── Edit Profile
```

**Benefits:**

- Organized code
- Reusable flows
- Clear structure
- Easy to navigate entire flows

### Example: E-Commerce App

```kotlin
@Composable
fun EcommerceApp() {
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf(false) }
    
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "main" else "auth"
    ) {
        // ========================================
        // AUTH GRAPH
        // ========================================
        navigation(
            startDestination = "login",
            route = "auth"
        ) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        isLoggedIn = true
                        navController.navigate("main") {
                            popUpTo("auth") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate("register")
                    }
                )
            }
            
            composable("register") {
                RegisterScreen(
                    onSuccess = {
                        isLoggedIn = true
                        navController.navigate("main") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                )
            }
        }
        
        // ========================================
        // MAIN GRAPH
        // ========================================
        navigation(
            startDestination = "shop",
            route = "main"
        ) {
            // SHOP SUB-GRAPH
            navigation(
                startDestination = "products",
                route = "shop"
            ) {
                composable("products") {
                    ProductsScreen(
                        onProductClick = { productId ->
                            navController.navigate("product_detail/$productId")
                        },
                        onCartClick = {
                            navController.navigate("cart")
                        }
                    )
                }
                
                composable("product_detail/{productId}") { backStackEntry ->
                    val productId = backStackEntry.arguments?.getString("productId")!!
                    ProductDetailScreen(
                        productId = productId,
                        onAddToCart = {
                            navController.navigate("cart")
                        }
                    )
                }
                
                composable("cart") {
                    CartScreen(
                        onCheckout = {
                            navController.navigate("checkout")
                        }
                    )
                }
                
                composable("checkout") {
                    CheckoutScreen(
                        onOrderComplete = { orderId ->
                            // Navigate to orders flow
                            navController.navigate("order_detail/$orderId") {
                                // Clear shop flow
                                popUpTo("shop") { inclusive = false }
                            }
                        }
                    )
                }
            }
            
            // ORDERS SUB-GRAPH
            navigation(
                startDestination = "order_list",
                route = "orders"
            ) {
                composable("order_list") {
                    OrderListScreen(
                        onOrderClick = { orderId ->
                            navController.navigate("order_detail/$orderId")
                        }
                    )
                }
                
                composable("order_detail/{orderId}") { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString("orderId")!!
                    OrderDetailScreen(orderId = orderId)
                }
            }
            
            // PROFILE SUB-GRAPH
            navigation(
                startDestination = "profile",
                route = "profile_flow"
            ) {
                composable("profile") {
                    ProfileScreen(
                        onEditProfile = {
                            navController.navigate("edit_profile")
                        },
                        onLogout = {
                            isLoggedIn = false
                            navController.navigate("auth") {
                                popUpTo("main") { inclusive = true }
                            }
                        }
                    )
                }
                
                composable("edit_profile") {
                    EditProfileScreen(
                        onSave = {
                            navController.navigateUp()
                        }
                    )
                }
            }
        }
    }
}
```

---
# 📚 Chapter 10 Continuation: Industry Best Practices

---

## 🏆 Chapter 10: Industry Best Practices (Continued)

### 5. Handle Back Button Properly

```kotlin
@Composable
fun FormScreen(navController: NavController) {
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    
    // Intercept back button press
    BackHandler(enabled = hasUnsavedChanges) {
        // Show confirmation dialog instead of leaving
        showExitDialog = true
    }
    
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved changes. Are you sure you want to leave?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        navController.navigateUp()  // Actually leave
                    }
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Stay")
                }
            }
        )
    }
    
    // Form content
    Column(modifier = Modifier.fillMaxSize()) {
        TextField(
            value = "",
            onValueChange = { hasUnsavedChanges = true },
            label = { Text("Enter data") }
        )
    }
}
```

### 6. Organize Navigation in Separate Files

**❌ Bad:** Everything in one file

**✅ Good:** Organized structure

```
app/
├── navigation/
│   ├── NavGraph.kt              // Main navigation setup
│   ├── Screen.kt                // Route definitions
│   ├── graphs/
│   │   ├── AuthNavGraph.kt      // Auth flow
│   │   ├── MainNavGraph.kt      // Main app flow
│   │   ├── ShopNavGraph.kt      // Shopping flow
│   │   └── ProfileNavGraph.kt   // Profile flow
```

**Screen.kt:**

```kotlin
sealed class Screen(val route: String) {
    // Auth screens
    data object Login : Screen("login")
    data object Register : Screen("register")
    
    // Main screens
    data object Home : Screen("home")
    data object Feed : Screen("feed")
    
    // Profile screens
    data object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    
    // Shop screens
    data object Products : Screen("products")
    data object ProductDetail : Screen("product/{productId}") {
        fun createRoute(productId: String) = "product/$productId"
    }
    data object Cart : Screen("cart")
}
```

**NavGraph.kt:**

```kotlin
@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "auth"
    ) {
        authNavGraph(navController)
        mainNavGraph(navController)
    }
}
```

**AuthNavGraph.kt:**

```kotlin
fun NavGraphBuilder.authNavGraph(navController: NavController) {
    navigation(
        startDestination = Screen.Login.route,
        route = "auth"
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("main") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }
    }
}
```

**MainNavGraph.kt:**

```kotlin
fun NavGraphBuilder.mainNavGraph(navController: NavController) {
    navigation(
        startDestination = Screen.Home.route,
        route = "main"
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        
        composable(Screen.Feed.route) {
            FeedScreen(navController)
        }
        
        composable(Screen.Profile.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")!!
            ProfileScreen(userId = userId, navController = navController)
        }
    }
}
```

### 7. Deep Linking

Deep links allow users to navigate directly to specific screens from:

- Notifications
- Web links
- Other apps

**Step 1: Define deep link in route**

```kotlin
composable(
    route = "product/{productId}",
    deepLinks = listOf(
        navDeepLink {
            uriPattern = "myapp://product/{productId}"
        },
        navDeepLink {
            uriPattern = "https://myapp.com/product/{productId}"
        }
    )
) { backStackEntry ->
    val productId = backStackEntry.arguments?.getString("productId")!!
    ProductDetailScreen(productId = productId)
}
```

**Step 2: Add intent filter in AndroidManifest.xml**

```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
    
    <!-- Regular launcher intent -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    
    <!-- Deep link intent filters -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        
        <!-- Custom scheme: myapp://product/123 -->
        <data
            android:scheme="myapp"
            android:host="product" />
    </intent-filter>
    
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        
        <!-- HTTPS link: https://myapp.com/product/123 -->
        <data
            android:scheme="https"
            android:host="myapp.com"
            android:pathPrefix="/product" />
    </intent-filter>
</activity>
```

**Step 3: Test deep link**

```bash
# Using ADB (Android Debug Bridge)
adb shell am start -W -a android.intent.action.VIEW -d "myapp://product/123" com.yourapp.package
```

### 8. Navigation with Animation (Custom Transitions)

```kotlin
@Composable
fun AnimatedNavGraph() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // Slide animation
        composable(
            route = "home",
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            }
        ) {
            HomeScreen(navController)
        }
        
        // Fade animation
        composable(
            route = "details",
            enterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            }
        ) {
            DetailsScreen(navController)
        }
        
        // Scale animation
        composable(
            route = "profile",
            enterTransition = {
                scaleIn(
                    initialScale = 0.9f,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                scaleOut(
                    targetScale = 0.9f,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            ProfileScreen(navController)
        }
    }
}
```

### 9. Testing Navigation

**Example test:**

```kotlin
class NavigationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var navController: TestNavHostController
    
    @Before
    fun setupNavHost() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            
            AppNavGraph(navController = navController)
        }
    }
    
    @Test
    fun navHost_verifyStartDestination() {
        // Verify start destination is correct
        composeTestRule
            .onNodeWithContentDescription("Home Screen")
            .assertIsDisplayed()
    }
    
    @Test
    fun navHost_clickButton_navigatesToDetails() {
        // Click button that navigates
        composeTestRule
            .onNodeWithText("Go to Details")
            .performClick()
        
        // Verify navigation happened
        val route = navController.currentBackStackEntry?.destination?.route
        assertEquals("details", route)
    }
    
    @Test
    fun navHost_navigateBack_returnsToHome() {
        // Navigate to details
        navController.navigate("details")
        
        // Navigate back
        navController.navigateUp()
        
        // Verify we're back at home
        val route = navController.currentBackStackEntry?.destination?.route
        assertEquals("home", route)
    }
}
```

### 10. Navigation State Management with ViewModel

**Best practice: Use ViewModel to trigger navigation**

```kotlin
// Navigation Event sealed class
sealed class NavigationEvent {
    data object NavigateToDetails : NavigationEvent()
    data class NavigateToProfile(val userId: String) : NavigationEvent()
    data object NavigateBack : NavigationEvent()
}

// ViewModel
class HomeViewModel : ViewModel() {
    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()
    
    fun onDetailsClick() {
        viewModelScope.launch {
            _navigationEvent.send(NavigationEvent.NavigateToDetails)
        }
    }
    
    fun onProfileClick(userId: String) {
        viewModelScope.launch {
            _navigationEvent.send(NavigationEvent.NavigateToProfile(userId))
        }
    }
}

// Composable
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    // Observe navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                NavigationEvent.NavigateToDetails -> {
                    navController.navigate("details")
                }
                is NavigationEvent.NavigateToProfile -> {
                    navController.navigate("profile/${event.userId}")
                }
                NavigationEvent.NavigateBack -> {
                    navController.navigateUp()
                }
            }
        }
    }
    
    // UI
    Column {
        Button(onClick = { viewModel.onDetailsClick() }) {
            Text("Go to Details")
        }
        
        Button(onClick = { viewModel.onProfileClick("123") }) {
            Text("Go to Profile")
        }
    }
}
```

### 11. Memory Management

**Clear back stack when appropriate:**

```kotlin
// Example: After logout, clear everything
navController.navigate("login") {
    popUpTo(0) { inclusive = true }  // Clear entire back stack
}

// Example: After payment, clear checkout flow
navController.navigate("order_success") {
    popUpTo("cart") { inclusive = true }  // Clear cart and checkout screens
}
```

### 12. Handle System Back Button for Specific Screens

```kotlin
@Composable
fun ExitConfirmationScreen(navController: NavController) {
    var showExitDialog by remember { mutableStateOf(false) }
    
    // If this is the last screen (start destination), show exit dialog
    BackHandler {
        if (navController.previousBackStackEntry == null) {
            showExitDialog = true
        } else {
            navController.navigateUp()
        }
    }
    
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit App?") },
            text = { Text("Are you sure you want to exit?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Exit app
                        (navController.context as? Activity)?.finish()
                    }
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
```

---

## 🎯 Complete Checklist for Production-Ready Navigation

✅ **Structure:**

- [ ] Separate navigation code from UI
- [ ] Use sealed classes for routes
- [ ] Organize navigation graphs in separate files
- [ ] Use nested graphs for complex flows

✅ **Data Passing:**

- [ ] Pass only IDs/primitives, not objects
- [ ] Use ViewModel to fetch data in destination
- [ ] Define argument types explicitly
- [ ] Handle missing/null arguments gracefully

✅ **State Management:**

- [ ] Use `saveState` and `restoreState` for tabs
- [ ] Use `launchSingleTop` to avoid duplicates
- [ ] Clear back stack after auth flows
- [ ] Handle configuration changes properly

✅ **User Experience:**

- [ ] Implement proper back button handling
- [ ] Add confirmation dialogs for unsaved changes
- [ ] Use appropriate animations
- [ ] Handle deep links

✅ **Testing:**

- [ ] Write navigation tests
- [ ] Test back stack behavior
- [ ] Test deep links
- [ ] Test edge cases (empty states, errors)

✅ **Performance:**

- [ ] Clear unnecessary screens from back stack
- [ ] Use `popUpTo` intelligently
- [ ] Avoid passing large data through navigation
- [ ] Profile navigation performance

---

## 🚀 Final Example: Complete Production App

Here's a minimal but complete production-ready navigation setup:

```kotlin
// 1. Screen.kt - Route definitions
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Home : Screen("home")
    data object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
}

// 2. NavGraph.kt - Main navigation
@Composable
fun AppNavGraph(startDestination: String) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                }
            )
        }
        
        composable(Screen.Profile.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")!!
            ProfileScreen(userId = userId)
        }
    }
}

// 3. MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyAppTheme {
                AppNavGraph(startDestination = Screen.Splash.route)
            }
        }
    }
}
```

---

## 📚 Summary

**Key Takeaways:**

1. **Always use type-safe navigation** (sealed classes or Kotlin Serialization)
2. **Separate navigation logic** from UI components
3. **Pass only IDs**, fetch data in destination screens
4. **Use `popUpTo`** strategically to manage back stack
5. **Handle back button** properly for better UX
6. **Organize navigation** in separate files/graphs
7. **Test navigation flows** thoroughly
8. **Use ViewModel** for triggering navigation events
9. **Implement deep linking** for better integration
10. **Profile and optimize** navigation performance

---

🎉 **You're now ready to build production-grade navigation in Jetpack Compose!**

Need help with specific navigation patterns? Just ask! 🚀
<a name="chapter-10"></a>

 