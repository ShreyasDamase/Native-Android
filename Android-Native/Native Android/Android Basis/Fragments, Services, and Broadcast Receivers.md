# Fragments, Services, and Broadcast Receivers

This guide covers core Android components (Fragments, Services, Broadcast Receivers, and Content Providers) detailing lifecycle structures, communication APIs, background limitations, and secure data sharing.

---

## 1. Fragment Deep Dive

A **Fragment** represents a modular, reusable portion of the UI inside an Activity.

### Fragment Lifecycle vs. View Lifecycle
A Fragment has two distinct lifecycles: the **Fragment Instance Lifecycle** and the **Fragment View Lifecycle**.

```text
       Fragment Lifecycle             Fragment View Lifecycle
     ┌─────────────────────┐
     │      onAttach()     │
     │      onCreate()     │
     ├─────────────────────┤          ┌─────────────────────┐
     │                     │ ───────> │    onCreateView()   │
     │                     │          │    onViewCreated()  │
     │      onStart()      │ ───────> │      onStart()      │
     │      onResume()     │ ───────> │      onResume()     │
     │                     │          │                     │
     │                     │ <─────── │      onPause()      │
     │      onStop()       │ <─────── │      onStop()       │
     │                     │          │    onDestroyView()  │ <-- View is destroyed, but
     ├─────────────────────┤          └─────────────────────┘     Fragment instance stays alive!
     │     onDestroy()     │
     │      onDetach()     │
     └─────────────────────┘
```

> [!IMPORTANT]
> Because a Fragment can be placed on the backstack (where its View is destroyed to save memory but the Fragment instance remains alive), **always use `viewLifecycleOwner`** (instead of `this` or the fragment instance) when registering LiveData or Flow observers. This prevents duplicate observations and leaks when returning from the backstack.

---

### Transaction Management: `add()` vs. `replace()`
*   `add()`: Places a new Fragment container on top of the existing layout container. The underlying Fragment is not destroyed or paused; it remains active in the stack.
*   `replace()`: Removes the existing Fragment from the layout container and mounts the new Fragment. The replaced Fragment's View is destroyed.
*   `addToBackStack(name)`: Adds the transaction to the backstack, enabling the user to pop/reverse the transaction (e.g. recreating the replaced Fragment's View) when pressing the back button.

---

### Communicating between Fragments: Fragment Result API
Do not use interfaces or shared view models for simple, one-off communication. Use the **Fragment Result API**:

```kotlin
// 1. Fragment A (The Listener)
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setFragmentResultListener("REQUEST_KEY") { requestKey, bundle ->
        val result = bundle.getString("RESULT_EXTRA")
        // Handle result...
    }
}

// 2. Fragment B (The Producer)
fun sendResultAndClose() {
    val bundle = bundleOf("RESULT_EXTRA" to "Updated Data")
    setFragmentResult("REQUEST_KEY", bundle)
    parentFragmentManager.popBackStack()
}
```

---

## 2. Services: Background Processing

A **Service** is an application component that can perform long-running operations in the background. It does **not** provide a UI and runs on the **Main (UI) Thread** of its host process by default.

---

### Started Services: `onStartCommand()` Return Flags
When launched via `startService()`, a service runs indefinitely. The value returned by `onStartCommand` defines how the system handles the service if the process is killed due to low memory:

1.  `START_STICKY`: OS restarts the service if killed, passing a `null` intent. Useful for services that run continuously (e.g., background music player).
2.  `START_NOT_STICKY`: OS does not restart the service. Useful for one-time operations.
3.  `START_REDELIVER_INTENT`: OS restarts the service and redelivers the original intent. Useful for jobs that must complete (e.g., file downloads).

---

### Bound Services: `onBind()`
Allows other components (like Activities) to bind to the service, establishing a client-server interface.
*   The client establishes connection using `bindService()`, passing a `ServiceConnection` callback.
*   Returns an `IBinder` interface to the client for direct method calls.
*   When all clients unbind (`unbindService()`), the service is destroyed by the system.

---

### Foreground Services (Android 14+ Requirements)
A Foreground Service performs operations that are visible to the user (e.g. tracking a workout, playing audio).
*   **Notification**: Must display a persistent, non-dismissible status bar notification.
*   **Android 14 types**: Starting in Android 14, developers must declare the exact service type in the manifest and request the corresponding permission (e.g., `camera`, `location`, `microphone`, `mediaPlayback`, `health`, `specialUse`).

```xml
<!-- Manifest declaration for Android 14+ -->
<service
    android:name=".LocationService"
    android:foregroundServiceType="location" />
```

*   **Background Execution Limits**: Standard background services cannot start from the background on Android 8+ (throws an `IllegalStateException`). You must use `ContextCompat.startForegroundService()` and call `startForeground()` within 5 seconds of the service launching.

---

## 3. BroadcastReceiver: Listening for System Events

A **BroadcastReceiver** allows your app to register for system or application-wide broadcast intents.

### Manifest-declared (Static) vs. Programmatic (Dynamic) Receivers
*   **Static (Manifest)**: Declared inside `AndroidManifest.xml`. Since Android 8.0, static receivers **cannot** receive implicit system broadcasts (except for a whitelist of exceptions like `ACTION_BOOT_COMPLETED`) to protect battery.
*   **Dynamic (Programmatic)**: Registered at runtime using `registerReceiver()`. Must be unregistered in `onDestroy` or `onPause` to prevent memory leaks.

```kotlin
// Registering a dynamic receiver with export flag (Android 13+)
val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
```

### Async Operations in Receivers: `goAsync()`
A BroadcastReceiver's `onReceive()` execution limit is 10 seconds. If you need to perform an asynchronous task (e.g. writing to disk) without blocking the UI thread, call `goAsync()` to obtain a `PendingResult` and complete it when finished.

```kotlin
override fun onReceive(context: Context, intent: Intent) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Background task...
        } finally {
            pendingResult.finish() // Must call finish!
        }
    }
}
```

---

## 4. ContentProvider & Secure File Sharing

A **ContentProvider** manages access to a structured set of data (e.g. database, files) and exposes it to other applications securely.

---

### CRUD Methods & ContentResolver
Clients query a ContentProvider via the `ContentResolver` utilizing a unique Content URI scheme (`content://authority/path`).
The provider implements CRUD operations:
*   `query()`: Retrieve data (returns a `Cursor`).
*   `insert()`: Add data.
*   `update()`: Modify data.
*   `delete()`: Remove data.

---

### Secure File Sharing: `FileProvider`
Sharing raw file paths (e.g., `file:///path/to/image.jpg`) across apps is blocked on Android 7.0+ (throws a `FileUriExposedException`). Instead, you must use a **FileProvider** (a specialized `ContentProvider` subclass) to generate content URIs (`content://...`) and grant temporary access permissions.

#### 1. Declare in `AndroidManifest.xml`
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="com.example.app.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

#### 2. Create `@xml/file_paths.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <!-- Map internal cache directories to a public path -->
    <cache-path name="shared_images" path="images/" />
</paths>
```

#### 3. Share the file programmatically
```kotlin
val imageFile = File(cacheDir, "images/pic.jpg")
val contentUri: Uri = FileProvider.getUriForFile(
    context,
    "com.example.app.fileprovider",
    imageFile
)

val shareIntent = Intent(Intent.ACTION_SEND).apply {
    type = "image/jpeg"
    putExtra(Intent.EXTRA_STREAM, contentUri)
    // Grant read permission explicitly
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
```
