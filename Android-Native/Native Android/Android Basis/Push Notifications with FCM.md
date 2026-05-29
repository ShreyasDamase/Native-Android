# Push Notifications with FCM

Firebase Cloud Messaging is Android's standard push notification platform for most apps.

This note focuses on the production concepts most likely to appear in interviews and app work:

- token lifecycle
- notification vs data messages
- notification channels
- runtime notification permission
- backend responsibilities

---

## 1. What FCM Does

FCM lets your backend send messages to a specific device, topic, or group.

Typical uses:

- chat message alerts
- order updates
- marketing campaigns
- sync nudges

High-level path:

1. App gets an FCM registration token
2. App sends token to backend
3. Backend targets that token
4. Google delivers the message to the device

---

## 2. Setup Basics

Typical setup includes:

- add Firebase to the app
- place `google-services.json`
- apply Firebase Gradle plugin
- add messaging dependency
- implement `FirebaseMessagingService`

---

## 3. `FirebaseMessagingService`

```kotlin
class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        sendTokenToBackend(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.data["title"] ?: "Update"
        val body = message.data["body"] ?: "You have a new notification"

        showNotification(title, body)
    }

    private fun sendTokenToBackend(token: String) = Unit
    private fun showNotification(title: String, body: String) = Unit
}
```

### `onNewToken()`

Called when:

- app is installed for the first time
- token is rotated
- app data is cleared
- Firebase refreshes the token

Your backend must keep token mappings updated.

---

## 4. Notification Message vs Data Message

### Notification Message

- contains a predefined notification payload
- system may display it automatically when app is backgrounded
- simpler but less flexible

### Data Message

- contains custom key/value payload
- app handles it in `onMessageReceived()`
- preferred for custom logic and consistent behavior

Many production apps prefer data messages so they fully control:

- notification style
- navigation target
- analytics
- deduplication

---

## 5. Notification Channels

Android 8+ requires channels.

Without a proper channel, notifications may not appear as expected.

```kotlin
fun createOrderChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "orders",
            "Order Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for order progress"
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
```

Best practice:

- one stable channel ID per notification category
- create channels at app startup
- choose importance carefully

---

## 6. Showing a Notification Manually

```kotlin
private fun showNotification(context: Context, title: String, body: String) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("destination", "orders")
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        2001,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, "orders")
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(body)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .build()

    NotificationManagerCompat.from(context).notify(2001, notification)
}
```

---

## 7. Android 13+ `POST_NOTIFICATIONS`

On Android 13+, posting notifications requires runtime permission in most cases.

```kotlin
private val notificationPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            enableNotifications()
        } else {
            showNotificationEducationUi()
        }
    }

fun askForNotifications() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
```

Important:

- do not ask on first frame without explanation
- ask when the feature value is clear

---

## 8. High Priority vs Normal Priority

FCM supports different delivery priorities.

### High Priority

- more urgent delivery attempt
- useful for time-sensitive user-visible updates
- should not be abused

### Normal Priority

- energy-friendlier
- okay for non-urgent updates

Examples:

- chat message from a real user: high priority may be justified
- marketing campaign: normal priority

---

## 9. Foreground, Background, and Killed-App Behavior

Behavior differs depending on:

- app state
- notification payload type
- OEM behavior
- battery restrictions

That is why teams prefer predictable server + app logic and usually test:

- app in foreground
- app in background
- app swiped away
- device in Doze

---

## 10. Token Management on the Backend

The app should send the token to backend together with:

- authenticated user ID
- app version
- device/platform info if relevant

The backend should:

- update tokens on refresh
- remove invalid tokens
- support logout by unlinking token from user

This is a real production concern. A lot of push bugs are backend token hygiene bugs, not client bugs.

---

## 11. Topic Messaging

Topics are useful when many users subscribe to the same category.

```kotlin
Firebase.messaging.subscribeToTopic("breaking_news")
```

Good for:

- news categories
- sports teams
- regional broadcast updates

Not ideal for:

- private user-specific data

---

## 12. Security Notes

- Never trust the notification payload alone for sensitive actions
- Use notifications as a wake-up or navigation hint, not as proof of authorization
- Sensitive operations should still be validated by backend after app opens
- Avoid placing secrets or privileged decisions directly in push payloads

---

## 13. Interview Answer

FCM gives the app a registration token, the client sends that token to the backend, and the backend targets that token or a topic through Firebase. On Android, you typically handle token refresh in `onNewToken()`, receive custom payloads in `onMessageReceived()`, create notification channels for Android 8+, and request `POST_NOTIFICATIONS` on Android 13+.

---

## Common Mistakes

- forgetting to send refreshed token to backend
- relying only on notification payloads and losing custom logic
- not creating channels on Android 8+
- asking notification permission too early
- treating push as guaranteed immediate delivery in all device states
