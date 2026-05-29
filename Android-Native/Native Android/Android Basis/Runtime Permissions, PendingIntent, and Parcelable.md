# Runtime Permissions, PendingIntent, and Parcelable

This note covers three Android interview topics that show up constantly in real projects:

1. Runtime permissions
2. `PendingIntent`
3. `Parcelable` vs `Serializable`

---

## 1. Runtime Permissions

Android permissions are split into categories:

- Normal permissions: granted automatically at install time
- Dangerous permissions: must be requested at runtime
- Signature permissions: granted only to apps signed with the same certificate

Common dangerous permissions:

- `CAMERA`
- `POST_NOTIFICATIONS`
- `READ_MEDIA_IMAGES`
- `ACCESS_FINE_LOCATION`
- `RECORD_AUDIO`

### Production Permission Flow

1. Check whether the permission is already granted
2. If not granted, decide whether to show rationale
3. Launch the permission request
4. Handle granted vs denied
5. If permanently denied, guide the user to app settings

### Modern API with Activity Result Contracts

```kotlin
class CameraActivity : AppCompatActivity() {

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openCamera()
            } else {
                handleCameraDenied()
            }
        }

    fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showCameraRationaleDialog()
            }

            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() = Unit
    private fun showCameraRationaleDialog() = Unit
    private fun handleCameraDenied() = Unit
}
```

### Multiple Permissions

```kotlin
private val mediaPermissionsLauncher =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val imagesGranted = result[Manifest.permission.READ_MEDIA_IMAGES] == true
        val videoGranted = result[Manifest.permission.READ_MEDIA_VIDEO] == true

        if (imagesGranted && videoGranted) {
            loadMedia()
        } else {
            showPermissionDeniedUi()
        }
    }
```

### Detecting Permanent Denial

The common rule:

- Permission denied
- `shouldShowRequestPermissionRationale(...) == false`
- And the permission is still not granted

That usually means the user selected "Don't ask again" or the platform treats the request as blocked.

```kotlin
private fun isPermanentlyDenied(permission: String): Boolean {
    val denied = ContextCompat.checkSelfPermission(
        this,
        permission
    ) != PackageManager.PERMISSION_GRANTED

    return denied && !shouldShowRequestPermissionRationale(permission)
}
```

### Open App Settings

```kotlin
private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    context.startActivity(intent)
}
```

### Interview Points

- Always use `ActivityResultContracts.RequestPermission()` instead of deprecated APIs
- Ask only when the feature is needed, not at app launch without context
- For media selection, prefer Photo Picker when possible instead of broad storage permission
- Background location is a separate, higher-friction flow

---

## 2. PendingIntent

A `PendingIntent` is a token that lets another app or the Android system execute your app's intent later with your app's identity and permissions.

Common use cases:

- Notifications
- AlarmManager
- App widgets
- Foreground service actions

### Why Not Pass a Normal Intent?

A normal `Intent` is just a description of an action. It cannot execute itself later. A `PendingIntent` wraps that action and gives execution rights to the system or another app.

### Common Types

- `PendingIntent.getActivity(...)`
- `PendingIntent.getService(...)`
- `PendingIntent.getBroadcast(...)`
- `PendingIntent.getForegroundService(...)`

### Notification Example

```kotlin
val openDetailsIntent = Intent(context, DetailActivity::class.java).apply {
    putExtra("item_id", "42")
}

val contentPendingIntent = PendingIntent.getActivity(
    context,
    1001,
    openDetailsIntent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)

val notification = NotificationCompat.Builder(context, "updates")
    .setSmallIcon(R.drawable.ic_notification)
    .setContentTitle("Order updated")
    .setContentText("Tap to view details")
    .setContentIntent(contentPendingIntent)
    .setAutoCancel(true)
    .build()
```

### Mutable vs Immutable

- `FLAG_IMMUTABLE`: the wrapped intent cannot be changed later
- `FLAG_MUTABLE`: a receiver or the system can modify parts of it later

Default rule:

- Use `FLAG_IMMUTABLE` unless you specifically need mutability

This matters for security because mutable pending intents can be abused if exposed incorrectly.

### `FLAG_UPDATE_CURRENT` vs `FLAG_CANCEL_CURRENT`

- `FLAG_UPDATE_CURRENT`: keep the existing pending intent but replace extras
- `FLAG_CANCEL_CURRENT`: cancel the old one and create a new one

### AlarmManager Example

```kotlin
val alarmIntent = Intent(context, ReminderReceiver::class.java).apply {
    putExtra("reminder_id", 7)
}

val alarmPendingIntent = PendingIntent.getBroadcast(
    context,
    7,
    alarmIntent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)
```

### Interview Points

- `PendingIntent` is required when execution happens later or outside your process
- Notifications and alarms nearly always use it
- On modern Android, immutable is the safe default

---

## 3. Parcelable vs Serializable

Both are used to pass objects through Android component boundaries, but they are not equal.

### `Parcelable`

- Android-specific serialization mechanism
- Faster than `Serializable`
- Preferred for Activities, Fragments, Bundles, and saved state

### `Serializable`

- Standard Java mechanism
- Simpler to start with
- Slower and more reflection-heavy
- Usually avoided for Android UI navigation data

### `@Parcelize` Example

```kotlin
@Parcelize
data class UserSummary(
    val id: String,
    val name: String,
    val isPremium: Boolean
) : Parcelable
```

Gradle plugin:

```kotlin
plugins {
    id("kotlin-parcelize")
}
```

### Passing Parcelable Between Activities

```kotlin
val user = UserSummary("u1", "Shreyas", true)

val intent = Intent(this, ProfileActivity::class.java).apply {
    putExtra("user_summary", user)
}
startActivity(intent)
```

### Reading Parcelable Safely

```kotlin
val user = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    intent.getParcelableExtra("user_summary", UserSummary::class.java)
} else {
    @Suppress("DEPRECATION")
    intent.getParcelableExtra("user_summary")
}
```

### Fragment Arguments

```kotlin
class ProfileFragment : Fragment() {
    companion object {
        private const val ARG_USER = "arg_user"

        fun newInstance(user: UserSummary): ProfileFragment {
            return ProfileFragment().apply {
                arguments = bundleOf(ARG_USER to user)
            }
        }
    }
}
```

### When Not to Pass Large Objects

Do not pass:

- Large bitmaps
- Huge lists
- Full database entities with deep graphs

Why:

- Bundles go through Binder
- Binder transactions have size limits
- Large payloads can cause `TransactionTooLargeException`

Better alternatives:

- Pass IDs only
- Re-load data from Room or repository
- Use shared ViewModel for same-screen flows

### Interview Summary

| Topic | Best Practice |
| --- | --- |
| Component data passing | Prefer `Parcelable` |
| Quick Java-style fallback | `Serializable`, but not ideal |
| Large payloads | Pass identifiers, not full objects |
| Modern runtime requests | Use Activity Result APIs |
| Delayed execution | Use `PendingIntent` |

---

## Common Mistakes

- Requesting permissions too early without user context
- Forgetting to explain why a permission is needed
- Using mutable pending intents without a reason
- Passing huge objects through intents or bundles
- Using `Serializable` for hot navigation paths in Android apps
