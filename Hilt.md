 # Complete Guide: Setting Up Hilt + JWT Authentication in Android Jetpack Compose

**A comprehensive, beginner-friendly guide for dependency injection and authentication implementation**

---

## Table of Contents

1. [Understanding the Core Concepts](#1-understanding-the-core-concepts)
2. [Initial Project Setup](#2-initial-project-setup)
3. [Step-by-Step Implementation](#3-step-by-step-implementation)
4. [Testing & Verification](#4-testing--verification)
5. [Common Errors & Solutions](#5-common-errors--solutions)
6. [Interview Preparation Notes](#6-interview-preparation-notes)

---

## 1. Understanding the Core Concepts

### What is Dependency Injection (DI)?

Dependency Injection is a design pattern where dependencies (objects that a class needs) are provided from outside rather than created inside the class. This makes code more testable, maintainable, and follows the principle of inversion of control.

**Simple Example:**

```kotlin
// ❌ Without DI - tightly coupled
class LoginViewModel {
    private val repository = AuthRepository() // Creates dependency itself
}

// ✅ With DI - loosely coupled
class LoginViewModel(
    private val repository: AuthRepository // Dependency injected from outside
)
```

### What is Hilt?

Hilt is a dependency injection library built on top of Dagger, specifically designed for Android. It reduces boilerplate code and provides automatic lifecycle management for Android components like Activities, Fragments, and ViewModels.

**Key Benefits:**

- Automatic component generation
- Lifecycle-aware injection
- Compile-time safety
- Less boilerplate than pure Dagger

### Relationship: Dagger vs Hilt

Dagger is the core DI framework (the engine), while Hilt is an Android-friendly wrapper built on top of Dagger. Hilt uses Dagger internally but provides simpler APIs and automatic scoping for Android components.

**Think of it as:**

```
Dagger (Core Engine)
    ↓
Hilt (Android Wrapper Layer)
    ↓
Your App (Easy DI)
```

---

## 2. Initial Project Setup

### Prerequisites Checklist

- [ ] Android Studio installed (latest stable version recommended)
- [ ] New or existing Jetpack Compose project
- [ ] Minimum SDK 24 or higher
- [ ] Internet connection for downloading dependencies

### File Structure Overview

```
YourProject/
├── settings.gradle.kts          # Plugin management & repositories
├── gradle/
│   └── libs.versions.toml       # Version catalog (dependencies)
├── app/
│   ├── build.gradle.kts         # Module-level configuration
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/example/yourapp/
│   │       ├── App.kt                    # Application class
│   │       ├── MainActivity.kt           # Entry point
│   │       ├── di/                       # Hilt modules
│   │       │   ├── NetworkModule.kt
│   │       │   └── DataModule.kt
│   │       ├── data/
│   │       │   ├── remote/
│   │       │   │   ├── ApiService.kt
│   │       │   │   └── AuthInterceptor.kt
│   │       │   ├── repository/
│   │       │   │   └── AuthRepository.kt
│   │       │   └── local/
│   │       │       └── TokenManager.kt
│   │       └── ui/
│   │           └── viewmodel/
│   │               └── LoginViewModel.kt
```

---

## 3. Step-by-Step Implementation

### STEP 1: Configure Plugin Management (Critical First Step)

**File:** `settings.gradle.kts` (root level)

```kotlin
pluginManagement {
    repositories {
        google()           // Required for Hilt plugin
        mavenCentral()     // General dependencies
        gradlePluginPortal() // Gradle plugins
    }
    
    // CRITICAL: Declare Hilt plugin version here
    plugins {
        id("com.google.dagger.hilt.android") version "2.52"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Book-Store"
include(":app")
```

**Why This Matters:** Without declaring the plugin in pluginManagement, Gradle cannot resolve the Hilt plugin and will throw a "Plugin was not found" error. The plugin must include a version number and be discoverable from the specified repositories.

**Common Mistake:**

```kotlin
// ❌ Missing plugin declaration - will cause errors
pluginManagement {
    repositories {
        google()
        mavenCentral()
    }
    // No plugins {} block - ERROR!
}
```

---

### STEP 2: Setup Version Catalog

**File:** `gradle/libs.versions.toml`

```toml
[versions]
agp = "8.13.1"
kotlin = "2.0.21"
composeBom = "2024.09.00"
coreKtx = "1.17.0"
lifecycleRuntimeKtx = "2.6.1"
activityCompose = "1.12.0"
navigation = "2.9.6"

# Coroutines
coroutines = "1.8.1"

# Data Storage
dataStore = "1.1.1"
securityCrypto = "1.1.0-alpha06"

# Networking
retrofit = "2.9.0"
okhttp = "4.12.0"

# Dependency Injection - SEPARATE KEYS!
hilt = "2.52"              # For library dependencies
hiltPlugin = "2.52"        # For plugin (must be separate)
hiltCompose = "1.2.0"      # Compose integration

[libraries]
# Core Android
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }

# Compose
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-navigation = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }

# Hilt - Dependency Injection
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltCompose" }

# Retrofit - API Calls
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }

# OkHttp - HTTP Client
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }

# DataStore - Modern Key-Value Storage
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "dataStore" }

# Security - Encrypted Storage
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto" }

# Coroutines - Async Operations
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hiltPlugin" }
```

**Key Insights:**

1. **Why Separate hilt and hiltPlugin?** Libraries and plugins resolve through different Gradle mechanisms. Using the same version key for both can cause plugin resolution failures because Gradle treats plugin metadata differently from library artifacts.
    
2. **Purpose of Each Library:**
    

|Library|Purpose|Why Needed|
|---|---|---|
|**hilt-android**|Core Hilt runtime|Provides DI container and annotations|
|**hilt-compiler**|Annotation processor|Generates DI code at compile time (kapt)|
|**hilt-navigation-compose**|Compose integration|Enables `hiltViewModel()` in Composables|
|**retrofit**|HTTP client|Makes API calls easy with interfaces|
|**retrofit-gson**|JSON converter|Converts JSON ↔ Kotlin objects|
|**okhttp**|Low-level HTTP|Powers Retrofit, handles connections|
|**okhttp-logging**|Request/response logger|Debug API calls (see what's sent/received)|
|**datastore-preferences**|Key-value storage|Modern replacement for SharedPreferences|
|**security-crypto**|Encrypted storage|Secure token storage|
|**coroutines-android**|Async operations|Background tasks without blocking UI|

---

### STEP 3: Configure Module Build File

**File:** `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    
    // Hilt plugin - resolves from pluginManagement
    alias(libs.plugins.hilt)
    
    // KAPT - NO VERSION HERE (version comes from Kotlin plugin)
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example.book_store"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.book_store"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
    
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose BOM (Bill of Materials - manages Compose versions)
    implementation(platform(libs.androidx.compose.bom))
    
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose UI
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation)
    
    // Hilt - Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)  // Annotation processor
    implementation(libs.hilt.navigation.compose)
    
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    
    // Data Storage
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
}
```

**Critical Points:**

1. **Plugin Order Matters:**
    
    - Apply `kapt` AFTER Hilt plugin
    - Apply Hilt plugin AFTER Kotlin plugin
2. **kapt vs ksp:** Hilt currently requires kapt (Kotlin Annotation Processing Tool) for code generation. KSP (Kotlin Symbol Processing) is newer and faster, but Hilt doesn't fully support it yet.
    
3. **Why No Version for kapt?**
    
    - kapt version is tied to Kotlin compiler version
    - Automatically managed by `kotlin-android` plugin

---

### STEP 4: Create Application Class

**File:** `app/src/main/java/com/example/book_store/App.kt`

```kotlin
package com.example.book_store

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Hilt components initialized automatically
        // You can add app-wide initialization here
    }
}
```

**Update AndroidManifest.xml:**

```xml
<application
    android:name=".App"
    android:allowBackup="true"
    android:icon="@mipmap/ic_launcher"
    ...>
```

**What @HiltAndroidApp Does:** This annotation triggers Hilt's code generation and creates the application-level Dagger component. It's the entry point for Hilt's dependency graph.

---

### STEP 5: Implement Token Management

**File:** `app/src/main/java/com/example/book_store/data/local/TokenManager.kt`

```kotlin
package com.example.book_store.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// Extension property for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

/**
 * TokenManager handles JWT token storage and retrieval.
 * 
 * Architecture:
 * - Stores token persistently in DataStore (survives app restarts)
 * - Caches token in-memory via StateFlow (for fast synchronous access)
 * - Provides both suspend and non-suspend access methods
 * 
 * @param context Application context (injected by Hilt)
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
    }
    
    // In-memory cache for token (accessed synchronously by OkHttp interceptor)
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()
    
    init {
        // Load token from DataStore into memory cache on initialization
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.data
                .map { preferences -> preferences[TOKEN_KEY] }
                .collect { tokenValue ->
                    _token.value = tokenValue
                }
        }
    }
    
    /**
     * Save JWT token to persistent storage.
     * Automatically updates in-memory cache via Flow.
     */
    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }
    
    /**
     * Clear JWT token (logout).
     */
    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
        }
    }
    
    /**
     * Synchronous token access for OkHttp interceptor.
     * Safe because it reads from in-memory cache (StateFlow).
     */
    fun getTokenSync(): String? = _token.value
}
```

**Design Pattern Explained:**

```
User Login
    ↓
saveToken() → DataStore (persistent)
    ↓
DataStore Flow emits → updates _token (in-memory)
    ↓
OkHttp Interceptor → getTokenSync() reads from _token
    ↓
Token attached to API requests
```

**Why This Pattern?**

- **DataStore:** Persistent storage (survives app kills)
- **StateFlow:** In-memory cache (fast, synchronous access)
- **Flow Collection:** Automatically syncs DataStore → StateFlow

---

### STEP 6: Create Auth Interceptor

**File:** `app/src/main/java/com/example/book_store/data/remote/AuthInterceptor.kt`

```kotlin
package com.example.book_store.data.remote

import com.example.book_store.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp Interceptor that automatically attaches JWT token to requests.
 * 
 * How it works:
 * 1. Intercepts every outgoing HTTP request
 * 2. Reads token from TokenManager (synchronous cache)
 * 3. Adds "Authorization: Bearer <token>" header if token exists
 * 4. Proceeds with modified request
 * 
 * @param tokenManager Provides token via Hilt DI
 */
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenManager.getTokenSync()
        
        return if (token.isNullOrBlank()) {
            // No token - proceed with original request
            chain.proceed(originalRequest)
        } else {
            // Add Authorization header with token
            val newRequest = originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(newRequest)
        }
    }
}
```

**Request Flow:**

```
App makes API call
    ↓
Retrofit creates HTTP request
    ↓
OkHttp interceptor chain begins
    ↓
AuthInterceptor.intercept() called
    ↓
Reads token from TokenManager
    ↓
Adds "Authorization: Bearer eyJhbG..." header
    ↓
Request sent to server
    ↓
Server validates token
    ↓
Response returned to app
```

---

### STEP 7: Define API Service Interface

**File:** `app/src/main/java/com/example/book_store/data/remote/ApiService.kt`

```kotlin
package com.example.book_store.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
    
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse
    
    @GET("books")
    suspend fun getBooks(): BooksResponse
    
    // Add more endpoints as needed
}

// Request/Response Models
data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val userId: String,
    val email: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String
)

data class RegisterResponse(
    val message: String,
    val userId: String
)

data class BooksResponse(
    val books: List<Book>
)

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val price: Double
)
```

**Retrofit Annotations:**

- `@POST` / `@GET` / `@PUT` / `@DELETE` - HTTP methods
- `@Body` - Request body (auto-serialized to JSON)
- `@Query` - URL query parameter
- `@Path` - URL path variable
- `@Header` - Custom header (rarely needed with interceptor)

---

### STEP 8: Create Hilt Network Module

**File:** `app/src/main/java/com/example/book_store/di/NetworkModule.kt`

```kotlin
package com.example.book_store.di

import com.example.book_store.data.local.TokenManager
import com.example.book_store.data.remote.ApiService
import com.example.book_store.data.remote.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module that provides network-related dependencies.
 * 
 * @InstallIn(SingletonComponent::class) means these dependencies
 * live as long as the application (singleton scope).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    private const val BASE_URL = "https://api.yourdomain.com/"
    
    /**
     * Provides logging interceptor for debugging.
     * Logs all HTTP requests/responses in Logcat.
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }
    
    /**
     * Provides custom auth interceptor.
     * Hilt automatically injects TokenManager dependency.
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor {
        return AuthInterceptor(tokenManager)
    }
    
    /**
     * Provides configured OkHttpClient.
     * Interceptors are applied in order:
     * 1. AuthInterceptor (adds token)
     * 2. LoggingInterceptor (logs request with token)
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)      // Runs first
            .addInterceptor(loggingInterceptor)   // Runs second (logs modified request)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Provides Gson converter for JSON serialization.
     */
    @Provides
    @Singleton
    fun provideGsonConverterFactory(): GsonConverterFactory {
        return GsonConverterFactory.create()
    }
    
    /**
     * Provides Retrofit instance.
     * All dependencies (OkHttp, Gson) injected automatically.
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gsonConverterFactory: GsonConverterFactory
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(gsonConverterFactory)
            .build()
    }
    
    /**
     * Provides API service implementation.
     * Retrofit generates the implementation at runtime.
     */
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
```

**Hilt Annotations Explained:**

|Annotation|Purpose|
|---|---|
|`@Module`|Marks class as Hilt module (provides dependencies)|
|`@InstallIn(SingletonComponent::class)`|Dependencies live as long as app|
|`@Provides`|Method provides a dependency|
|`@Singleton`|Only one instance created (cached)|
|`@Inject`|Request dependency injection|

**Dependency Graph:**

```
NetworkModule
    ↓
provides LoggingInterceptor
provides AuthInterceptor (needs TokenManager)
provides OkHttpClient (needs both interceptors)
provides GsonConverter
provides Retrofit (needs OkHttp + Gson)
provides ApiService (needs Retrofit)
```

---

### STEP 9: Create Repository Layer

**File:** `app/src/main/java/com/example/book_store/data/repository/AuthRepository.kt`

```kotlin
package com.example.book_store.data.repository

import com.example.book_store.data.local.TokenManager
import com.example.book_store.data.remote.ApiService
import com.example.book_store.data.remote.LoginRequest
import com.example.book_store.data.remote.RegisterRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository handles data operations.
 * Single source of truth for authentication data.
 * 
 * Follows Clean Architecture:
 * - ViewModel calls Repository
 * - Repository calls API or Database
 * - ViewModel doesn't know about network/storage details
 */
@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    
    /**
     * Login user with email and password.
     * 
     * @return Result.success on successful login
     * @return Result.failure on error (network, auth, etc.)
     */
    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            
            // Save token on successful login
            tokenManager.saveToken(response.token)
            
            Result.success("Login successful")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Register new user.
     */
    suspend fun register(email: String, password: String, name: String): Result<String> {
        return try {
            val response = apiService.register(
                RegisterRequest(email, password, name)
            )
            Result.success(response.message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Logout user by clearing stored token.
     */
    suspend fun logout() {
        tokenManager.clearToken()
    }
    
    /**
     * Check if user is logged in (has valid token).
     */
    fun isLoggedIn(): Boolean {
        return !tokenManager.getTokenSync().isNullOrBlank()
    }
}
```

**Repository Benefits:**

- **Separation of Concerns:** ViewModel doesn't know about API/DB
- **Testability:** Easy to mock for unit tests
- **Single Source of Truth:** One place for data logic
- **Error Handling:** Centralized error management

---

### STEP 10: Create ViewModel with Hilt

**File:** `app/src/main/java/com/example/book_store/ui/viewmodel/LoginViewModel.kt`

```kotlin
package com.example.book_store.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.book_store.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for login screen.
 * 
 * @HiltViewModel enables Hilt injection + automatic ViewModel scoping.
 * Dependencies (AuthRepository) injected via constructor.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // UI State (Compose observes this)
    var uiState by mutableStateOf(LoginUiState())
        private set
    
    // User input handlers
    fun onEmailChange(email: String) {
        uiState = uiState.copy(email = email, error = null)
    }
    
    fun onPasswordChange(password: String) {
        uiState = uiState.copy(password = password, error = null)
    }
    
    /**
     * Attempt login with current email/password.
     * Launched in viewModelScope (auto-cancelled when ViewModel destroyed).
     */
    fun onLogin() {
        // Basic validation
        if (uiState.email.isBlank() || uiState.password.isBlank()) {
            uiState = uiState.copy(error = "Email and password required")
            return
        }
        
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            
            val result = authRepository.login(
                email = uiState.email,
                password = uiState.password
            )
            
            result.fold(
                onSuccess = { message ->
                    uiState = uiState.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        error = null
                    )
                },
                onFailure = { exception ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = exception.localizedMessage ?: "Login failed"
                    )
                }
            )
        }
    }
}

/**
 * UI state data class.
 * Single source of truth for UI state.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null
)
```

**ViewModel Pattern:**

```
User Input (Compose)
    ↓
ViewModel.onEmailChange()
    ↓
Update uiState
    ↓
Compose recomposes UI

User Clicks Login
    ↓
ViewModel.onLogin() 
    ↓ 
Repository.login() 
    ↓ 
API call with token injection 
    ↓ 
Update uiState (success/error) 
    ↓ 
Compose shows result
```



````

---

### STEP 11: Setup MainActivity and Compose

**File:** `app/src/main/java/com/example/book_store/MainActivity.kt`

```kotlin
package com.example.book_store

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.book_store.ui.theme.BookStoreTheme
import com.example.book_store.ui.viewmodel.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity entry point.
 * 
 * @AndroidEntryPoint enables Hilt injection in Activity.
 * Required for hiltViewModel() to work in Composables.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookStoreTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen()
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel() // Hilt provides ViewModel
) {
    val uiState = viewModel.uiState
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = viewModel::onLogin,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Login")
            }
        }
        
        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        
        if (uiState.isLoggedIn) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Login Successful!", color = MaterialTheme.colorScheme.primary)
        }
    }
}
````

**Key Points:**

- `@AndroidEntryPoint` on Activity enables Hilt
- `hiltViewModel()` retrieves Hilt-injected ViewModel
- No manual ViewModel instantiation needed
- ViewModel survives configuration changes automatically

---

## 4. Testing & Verification

### Build and Sync Steps

1. **Sync Gradle Files:**
    
    ```
    File → Sync Project with Gradle Files
    ```
    
2. **Clean Build:**
    
    ```bash
    ./gradlew clean
    ./gradlew build --stacktrace
    ```
    
3. **Run App:**
    
    ```
    Run → Run 'app'
    ```
    

### Verification Checklist

- [ ] App builds without errors
- [ ] Login screen appears
- [ ] Can enter email/password
- [ ] Clicking login triggers API call
- [ ] Loading indicator shows during request
- [ ] Token saved to DataStore on success
- [ ] Error message shows on failure
- [ ] Logcat shows "Authorization: Bearer ..." header

### Debugging Tools

**View Network Requests (Logcat):**

```
Filter: okhttp
Shows: Request headers, body, response
```

**Check DataStore:**

```kotlin
// In TokenManager or ViewModel
viewModelScope.launch {
    tokenManager.token.collect { token ->
        Log.d("TOKEN", "Current token: $token")
    }
}
```

---

## 5. Common Errors & Solutions

### Error 1: Plugin [id: 'com.google.dagger.hilt.android'] was not found

**Cause:** Hilt plugin not declared in pluginManagement

**Solution:**

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.google.dagger.hilt.android") version "2.52"
    }
}
```

---

### Error 2: Unresolved reference 'kapt'

**Cause:** kapt plugin not applied

**Solution:**

```kotlin
// app/build.gradle.kts
plugins {
    // ... other plugins
    id("org.jetbrains.kotlin.kapt") // NO VERSION
}
```

---

### Error 3: @HiltViewModel class not generated

**Cause:** kapt compiler dependency missing

**Solution:**

```kotlin
dependencies {
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler) // REQUIRED
    implementation(libs.hilt.navigation.compose)
}
```

Then: Build → Clean Project → Rebuild Project

---

### Error 4: Cannot access 'javax.inject.Inject' class

**Cause:** Version conflict between dependencies

**Solution:**

- Ensure all Hilt dependencies use same version
- Check libs.versions.toml for consistent versions
- Run `./gradlew :app:dependencies` to check conflicts

---

### Error 5: Token not attached to requests

**Cause:** TokenManager not loaded or interceptor not added

**Debug Steps:**

1. Log token in AuthInterceptor:
    
    ```kotlin
    val token = tokenManager.getTokenSync()
    Log.d("AUTH", "Token: $token")
    ```
    
2. Verify interceptor order in NetworkModule:
    
    ```kotlin
    .addInterceptor(authInterceptor)      // Should be before logging
    .addInterceptor(loggingInterceptor)
    ```
    
3. Check DataStore initialization in TokenManager init block
    

---

## 6. Interview Preparation Notes

### Dependency Injection Questions

**Q: What is Dependency Injection?** A: A design pattern where a class receives its dependencies from external sources rather than creating them itself. This promotes loose coupling, testability, and follows the Dependency Inversion Principle.

**Q: Why use Hilt over manual DI?** A:

- Compile-time verification (type-safe)
- Automatic lifecycle management
- Scoped dependencies (Singleton, Activity, Fragment scopes)
- Less boilerplate than pure Dagger
- Android-specific components pre-configured

**Q: Explain the difference between @Inject constructor and @Provides method.** A:

- `@Inject constructor`: Used when you own the class and can modify it
- `@Provides`: Used for third-party classes (like Retrofit) or complex initialization

**Q: What is @Singleton scope in Hilt?** A: Ensures only one instance of the dependency exists for the entire application lifetime. Created on first request and cached.

---

### Architecture Questions

**Q: Explain MVVM architecture.** A:

- **Model:** Data layer (Repository, API, Database)
- **View:** UI layer (Composables, Activities)
- **ViewModel:** Bridge between Model and View, holds UI state

**Q: Why use Repository pattern?** A:

- Single source of truth for data
- Abstracts data sources (API, database, cache)
- Easier to test ViewModels (mock Repository)
- Centralized error handling

**Q: How does StateFlow differ from LiveData?** A:

- StateFlow is part of Kotlin Coroutines (not Android-specific)
- StateFlow requires initial value; LiveData doesn't
- StateFlow can be used outside Android (in common Kotlin Multiplatform code)
- LiveData is lifecycle-aware by default; StateFlow needs manual collection

---

### Networking Questions

**Q: What is an OkHttp Interceptor?** A: A mechanism to observe, modify, or retry HTTP requests/responses. Useful for adding headers (like auth tokens), logging, or handling errors globally.

**Q: Why store JWT in DataStore instead of SharedPreferences?** A:

- DataStore is asynchronous (doesn't block UI thread)
- Type-safe with Kotlin coroutines
- Handles errors gracefully
- Supports migrations
- Google recommends it as SharedPreferences replacement

**Q: How would you implement token refresh?** A: Use OkHttp Authenticator:

```kotlin
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApi: AuthApi
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // If 401, refresh token
        val newToken = runBlocking { authApi.refreshToken() }
        tokenManager.saveToken(newToken)
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }
}
```

---

### Security Questions

**Q: Is DataStore secure by default?** A: No, DataStore stores plain text. For sensitive data like tokens, use:

```kotlin
implementation("androidx.security:security-crypto:1.1.0-alpha06")
// Use EncryptedSharedPreferences or MasterKey encryption
```

**Q: What are best practices for storing JWT?** A:

- Use DataStore/EncryptedSharedPreferences (not plain SharedPreferences)
- Don't store refresh token and access token together
- Clear tokens on logout
- Implement token expiry checking
- Use HTTPS only (never HTTP)

---

## Summary Checklist

Before submitting your app or moving to production:

### Configuration

- [ ] pluginManagement configured in settings.gradle.kts
- [ ] Hilt plugin declared with version
- [ ] kapt plugin applied in app/build.gradle.kts
- [ ] All dependencies added with correct versions
- [ ] Version catalog (TOML) properly structured

### Code Structure

- [ ] @HiltAndroidApp on Application class
- [ ] Application class registered in AndroidManifest
- [ ] @AndroidEntryPoint on Activities
- [ ] NetworkModule provides Retrofit + OkHttp
- [ ] DataModule provides DataStore/TokenManager
- [ ] AuthInterceptor attaches tokens
- [ ] Repository handles data operations
- [ ] @HiltViewModel on ViewModels
- [ ] Proper error handling in Repository

### Testing

- [ ] App builds without errors
- [ ] Login flow works end-to-end
- [ ] Token persists across app restarts
- [ ] Logout clears token
- [ ] API requests include Authorization header
- [ ] Error messages display correctly

### Security

- [ ] Using HTTPS (not HTTP)
- [ ] Token stored securely (consider encryption)
- [ ] No hardcoded credentials
- [ ] Proper input validation
- [ ] Error messages don't leak sensitive info

---

## Next Steps

1. **Add Navigation:**
    
    - Implement Navigation Compose for multi-screen app
    - Pass ViewModels between screens via `hiltViewModel()`
2. **Implement Refresh Token:**
    
    - Add OkHttp Authenticator for automatic token refresh
    - Handle concurrent requests during refresh
3. **Add Loading States:**
    
    - Use sealed classes for Result/State
    - Show proper loading/error/success states
4. **Write Tests:**
    
    - Unit test ViewModels (mock Repository)
    - Integration test Repository (mock ApiService)
    - UI test Composables with `hiltViewModel()` mock
5. **Improve Security:**
    
    - Implement EncryptedSharedPreferences
    - Add biometric authentication
    - Certificate pinning for API calls

---

**Remember:** Keep this guide as reference. The order matters:

1. Plugin management first
2. Version catalog setup
3. Apply plugins correctly
4. Add dependencies
5. Create Application class
6. Build Hilt modules
7. Implement data layer
8. Create ViewModels
9. Wire up UI

Follow these steps exactly, and you'll avoid 99% of common Hilt setup errors. Good luck! 🚀