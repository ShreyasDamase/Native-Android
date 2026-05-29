# Android Activity Lifecycle & Intent System

An **Activity** is a core Android component that provides a window in which the app draws its UI. It is the entry point for user interaction, managing its own lifecycle, task backstack representation, and inter-component communication through intents.

---

## 1. The Activity Lifecycle

The Android OS manages activities using a state machine. When an activity transitions between states, the system invokes lifecycle callbacks. Understanding these transitions is crucial for preventing memory leaks, saving user progress, and managing system resources (camera, location, sensors).

### Activity Lifecycle Flowchart

```text
       [ Activity Launched ]
                 |
                 v
            +----------+
            | onCreate |  <-- Initialize UI, ViewModels, Bind Data
            +----------+
                 |
                 v
            +----------+
            | onStart  |  <-- UI becomes visible to user
            +----------+
                 |
                 v
            +----------+
            | onResume |  <-- App starts interacting, gains input focus
            +----------+
                 |
        [ Activity Running ]
                 |
     (User opens a dialog, split-screen, or leaves focus)
                 |
                 v
            +----------+
            | onPause  |  <-- App loses focus, visible in background
            +----------+
                 |
     (Activity is no longer visible - app minimized)
                 |
                 v
            +----------+
            | onStop   |  <-- Release heavy resources (GPS, sensors)
            +----------+
            /          \
           /            \
  (App reopened)    (App killed/finished)
         /                \
        v                  v
  +-----------+      +-----------+
  | onRestart |      | onDestroy | <-- Clean up final references
  +-----------+      +-----------+
        |                  |
        v                  v
  +----------+      [ Activity Shut Down ]
  | onStart  |
  +----------+
```

### Lifecycle Methods Explained

| Callback | Description | Practical Scenario |
| :--- | :--- | :--- |
| `onCreate()` | Triggered when the activity is first created. Perform static setups here (e.g. `setContent` in Compose, setting up ViewModels). | Restores state from `savedInstanceState` if recreated. |
| `onStart()` | Called when the activity becomes visible to the user. | Prepares the app to enter the foreground. |
| `onResume()` | Called when the activity gains focus and is ready to receive user input. | Registers location listeners, starts camera previews, resumes animations. |
| `onPause()` | Called when the user leaves the activity (loses focus but remains partially visible, e.g., in split-screen or multi-window mode). | Pause animations, save quick draft changes, release hardware sensors. |
| `onStop()` | Called when the activity is no longer visible to the user. | Save heavy data to database, disconnect network listeners. |
| `onRestart()` | Called after the activity has been stopped, prior to starting it again. | Re-initializes state variables that were stopped. |
| `onDestroy()` | Called before the activity is destroyed (either manually finished or destroyed by the OS to reclaim memory). | Cancel pending coroutine jobs, remove references to avoid leaks. |

---

## 2. Common Lifecycle Scenarios

### A. Screen Rotation (Configuration Change)
When a device is rotated, the current activity is completely destroyed and recreated.
1. `onPause()` -> `onStop()` -> `onDestroy()` (Current instance destroyed)
2. `onCreate()` -> `onStart()` -> `onResume()` (New instance created)

> [!TIP]
> Use `ViewModel` (which survives configuration changes) or `rememberSaveable` to preserve data across rotation.

### B. User Presses Home Button (App Minimized)
1. `onPause()` -> `onStop()`
2. (Re-opening the app): `onRestart()` -> `onStart()` -> `onResume()`

### C. Dialog Appears
* **Compose / Standard Dialog**: If it's a dialog container layered on the current window, the activity **does not** trigger `onPause()` because it retains focus.
* **Translucent Activity Dialog**: If a dialog-themed *Activity* opens on top, the base activity triggers `onPause()`.

---

## 3. The Intent System

An **Intent** is an asynchronous messaging object used to request an action from another app component (Activity, Service, Broadcast Receiver).

### A. Explicit Intents
Used to start a specific component within your own application (using the target class name).

```kotlin
// Launching TargetActivity from MainActivity
val intent = Intent(this, TargetActivity::class.java).apply {
    putExtra("EXTRA_USER_ID", "user_abc_123")
    putExtra("EXTRA_IS_PREMIUM", true)
}
startActivity(intent)
```

**Parsing Extras in `TargetActivity`:**
```kotlin
class TargetActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Retrieve extras safely
        val userId = intent.getStringExtra("EXTRA_USER_ID") ?: "default_id"
        val isPremium = intent.getBooleanExtra("EXTRA_IS_PREMIUM", false)
        
        // Use the parsed data...
    }
}
```

### B. Implicit Intents
Used to declare a general action you want performed, letting the OS determine which installed application handles it.

```kotlin
// Open a Web URL in a browser
val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://developer.android.com"))
if (webIntent.resolveActivity(packageManager) != null) {
    startActivity(webIntent)
}

// Dial a Phone Number
val callIntent = Intent(Intent.ACTION_DIAL).apply {
    data = Uri.parse("tel:+123456789")
}
startActivity(callIntent)
```

---

## 4. Modern Activity Result API

Instead of the deprecated `startActivityForResult` and `onActivityResult()`, modern Android uses the **Activity Result API**. This decouples result handling from the Activity class, making it clean and reusable.

### A. Launching an Activity for a Result

```kotlin
class MainActivity : ComponentActivity() {

    // 1. Register the contract and callback
    private val pickContactLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val contactUri = data?.data
            // Handle contact Uri
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 2. Launch when user clicks a button
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        pickContactLauncher.launch(intent)
    }
}
```

### B. Requesting System Permissions

```kotlin
class CameraActivity : ComponentActivity() {

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission approved! Open camera
        } else {
            // Permission denied. Show rationale to user
        }
    }

    fun requestCamera() {
        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
}
```

---

## 5. Activity Launch Modes

Launch modes define how a new instance of an activity is associated with the current **Task** (a collection of activities the user interacts with in a stack). Set them in the `AndroidManifest.xml` or via Intent flags.

```xml
<activity
    android:name=".MainActivity"
    android:launchMode="standard" />
```

### 1. `standard` (Default)
* **Behavior**: The system always creates a new instance of the activity in the task stack from which it was started.
* **Stack**: A -> B -> launch B -> A -> B -> B

### 2. `singleTop`
* **Behavior**: If an instance of the activity already exists at the **top** of the target task stack, the system routes the intent through `onNewIntent()` instead of creating a new instance.
* **Stack**: A -> B -> launch B -> A -> B (with `onNewIntent` called in B)
* **Stack**: A -> B -> launch A -> A -> B -> A (new instance of A created since B was on top)

### 3. `singleTask`
* **Behavior**: The system creates a new task and instantiates the activity at the root of the new task. If an instance already exists in an existing task, the system pops all activities on top of it to bring it to the top, routing the intent to `onNewIntent()`.
* **Stack**: A -> B -> C -> launch B (singleTask) -> A -> B (C is popped and destroyed!)

### 4. `singleInstance`
* **Behavior**: Same as `singleTask`, except the system does not launch any other activities into the task holding the instance. The activity is always the single and only member of its task stack.
* **Use Case**: Used for apps that function strictly as standalone screens (e.g. Launcher, Dialer, Calculator).
