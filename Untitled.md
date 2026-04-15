**Step 1 — Add DataStore dependency** in `build.gradle`:

```kotlin
implementation("androidx.datastore:datastore-preferences:1.1.1")
```

**Step 2 — Create `OnboardingPreferences.kt`:**

```kotlin
package com.wallstreet.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

class OnboardingPreferences(private val context: Context) {

    private val ONBOARDING_KEY = booleanPreferencesKey("onboarding_completed")

    suspend fun isOnboardingCompleted(): Boolean {
        return context.dataStore.data.first()[ONBOARDING_KEY] ?: false
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { it[ONBOARDING_KEY] = true }
    }
}
```

**Step 3 — Update `MainActivity.kt`** to check it:

```kotlin
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !SplashGate.isReady }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            decideStartDestination()
        }

        setContent {
            WallStreetAndroidTheme {
                AppNavigation(startDestination = SplashGate.startDestination)
            }
        }
    }

    private suspend fun decideStartDestination() {
        val user = FirebaseAuth.getInstance().currentUser
        val prefs = OnboardingPreferences(applicationContext)
        val onboardingDone = prefs.isOnboardingCompleted()

        SplashGate.startDestination = when {
            user == null && !onboardingDone -> StartDestination.Onboarding
            user == null && onboardingDone -> StartDestination.Auth
            user != null && !user.isEmailVerified -> StartDestination.Otp
            else -> StartDestination.Home
        }

        SplashGate.isReady = true
    }
}
```

**Step 4 — Add `Auth` to `StartDestination`** (you currently only have `Onboarding`):

```kotlin
sealed interface StartDestination {
    data object Unknown : StartDestination
    data object Home : StartDestination
    data object Onboarding : StartDestination  // ← shows onboarding slides
    data object Auth : StartDestination        // ← goes straight to login
    data object Otp : StartDestination
}
```

**Step 5 — Update `AppNavigation.kt`** to handle it:

```kotlin
val initialRoute = when (startDestination) {
    StartDestination.Home -> AppRoute.Home
    StartDestination.Onboarding -> AppRoute.OnBoarding  // starts at onboarding slides
    StartDestination.Auth -> AppRoute.OnBoarding        // starts at login directly
    StartDestination.Otp -> AppRoute.OnBoarding
    StartDestination.Unknown -> AppRoute.OnBoarding
}
```

Then pass it down to `OnboardingNavigation`:

```kotlin
entry<AppRoute.OnBoarding> {
    OnboardingNavigation(
        skipToLogin = startDestination == StartDestination.Auth,  // ← add this
        goToOtp = startDestination == StartDestination.Otp,
        onLogin = {
            backStack.remove(AppRoute.OnBoarding)
            backStack.add(AppRoute.Home)
        }
    )
}
```

**Step 6 — Update `OnboardingNavigation.kt`:**

```kotlin
@Composable
fun OnboardingNavigation(
    onLogin: () -> Unit,
    goToOtp: Boolean = false,
    skipToLogin: Boolean = false,   // ← add this
    modifier: Modifier = Modifier
) {
    // ... backStack same ...

    LaunchedEffect(skipToLogin) {
        if (skipToLogin) onBoardingBackStack.add(AppRoute.OnBoarding.Login)
    }

    LaunchedEffect(goToOtp) {
        if (goToOtp) onBoardingBackStack.add(AppRoute.OnBoarding.OtpScreen)
    }
    // ... rest same ...
```

**Step 7 — Mark onboarding complete in `OnboardingScreen.kt`** when user taps Skip or Get Started:

```kotlin
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onboardingPreferences: OnboardingPreferences  // ← add this
) {
    val scope = rememberCoroutineScope()

    // replace both onFinish() calls with:
    val finish = {
        scope.launch { onboardingPreferences.setOnboardingCompleted() }
        onFinish()
    }
    // then use finish() instead of onFinish() on Skip and Get Started buttons
```

Pass it from `OnboardingNavigation`:

```kotlin
entry<AppRoute.OnBoarding.Onboarding> {
    val context = LocalContext.current
    OnboardingScreen(
        onboardingPreferences = OnboardingPreferences(context),
        onFinish = { onBoardingBackStack.add(AppRoute.OnBoarding.Login) }
    )
}
```

**The full flow now:**

```
First install
  → onboardingDone = false, no user → StartDestination.Onboarding
  → Shows onboarding slides
  → User taps Skip/Get Started → setOnboardingCompleted() → goes to Login

Second open (logged out)
  → onboardingDone = true, no user → StartDestination.Auth
  → Skips slides, goes straight to Login ✅

Logged in
  → Goes straight to Home ✅
```