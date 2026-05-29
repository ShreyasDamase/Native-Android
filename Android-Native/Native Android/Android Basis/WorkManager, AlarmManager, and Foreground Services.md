# WorkManager, AlarmManager, and Foreground Services

This note covers one of the most common Android decision areas:

- When should I use WorkManager?
- When do I need AlarmManager?
- When is a foreground service the right tool?

If you choose the wrong one, the app becomes unreliable, battery-heavy, or blocked by OS restrictions.

---

## 1. Quick Decision Rule

### Use WorkManager when

- work can be deferred
- reliability matters
- the task should survive process death
- constraints matter like network or charging

Examples:

- upload retry
- sync
- cleanup
- periodic refresh

### Use AlarmManager when

- timing must be exact or near-exact
- the user expects a specific wall-clock trigger

Examples:

- medicine reminder at 8:00 AM
- calendar reminder
- alarm clock

### Use a Foreground Service when

- the task must start immediately
- the user is actively aware of it
- a persistent notification is acceptable and required

Examples:

- turn-by-turn navigation
- workout tracking
- music playback
- active call recording

---

## 2. WorkManager

WorkManager is the Jetpack API for deferrable, guaranteed background work.

Important ideas:

- persists work in a local database
- survives process death
- can apply constraints
- supports retries and chaining
- is the modern default for most background jobs

---

## 3. One-Time Work

```kotlin
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            syncRepository()
            Result.success()
        } catch (e: IOException) {
            Result.retry()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private suspend fun syncRepository() {
        // network + database sync
    }
}
```

Create and enqueue:

```kotlin
val request = OneTimeWorkRequestBuilder<SyncWorker>()
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
    )
    .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        30,
        TimeUnit.SECONDS
    )
    .addTag("sync")
    .build()

WorkManager.getInstance(context).enqueue(request)
```

---

## 4. Periodic Work

```kotlin
val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(
    15, TimeUnit.MINUTES
)
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()
    )
    .build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "periodic_sync",
    ExistingPeriodicWorkPolicy.KEEP,
    periodicRequest
)
```

Notes:

- minimum interval is 15 minutes
- execution time is not exact
- the OS batches it for battery efficiency

---

## 5. Unique Work

Unique work prevents duplicates.

```kotlin
WorkManager.getInstance(context).enqueueUniqueWork(
    "upload_profile_photo",
    ExistingWorkPolicy.REPLACE,
    request
)
```

Policies:

- `KEEP`
- `REPLACE`
- `APPEND`

---

## 6. Observe Work State

```kotlin
WorkManager.getInstance(context)
    .getWorkInfoByIdLiveData(request.id)
    .observe(this) { workInfo ->
        when (workInfo.state) {
            WorkInfo.State.ENQUEUED -> Unit
            WorkInfo.State.RUNNING -> Unit
            WorkInfo.State.SUCCEEDED -> Unit
            WorkInfo.State.FAILED -> Unit
            WorkInfo.State.CANCELLED -> Unit
            WorkInfo.State.BLOCKED -> Unit
        }
    }
```

Useful for:

- upload progress UI
- retry UI
- showing sync status

---

## 7. Chaining Work

```kotlin
val compressRequest = OneTimeWorkRequestBuilder<CompressWorker>().build()
val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>().build()

WorkManager.getInstance(context)
    .beginWith(compressRequest)
    .then(uploadRequest)
    .enqueue()
```

This is good for:

- resize image
- then upload
- then mark database row complete

---

## 8. Foreground Work Inside WorkManager

Long-running user-important work can still use WorkManager with foreground mode.

```kotlin
override suspend fun doWork(): Result {
    setForeground(createForegroundInfo())
    return try {
        uploadLargeFile()
        Result.success()
    } catch (e: Exception) {
        Result.failure()
    }
}
```

This is common for:

- large uploads
- long downloads

---

## 9. AlarmManager

AlarmManager is for time-based triggers where exactness matters more than batching.

### Exact Alarm Example

```kotlin
val alarmManager = context.getSystemService(AlarmManager::class.java)

val intent = Intent(context, ReminderReceiver::class.java)
val pendingIntent = PendingIntent.getBroadcast(
    context,
    42,
    intent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)

alarmManager.setExactAndAllowWhileIdle(
    AlarmManager.RTC_WAKEUP,
    triggerAtMillis,
    pendingIntent
)
```

### Common Methods

- `set()`
- `setWindow()`
- `setExact()`
- `setExactAndAllowWhileIdle()`

### Important Caveats

- exact alarms have tighter platform restrictions now
- Android 12+ may require `SCHEDULE_EXACT_ALARM`
- exact alarms should be justified by user expectation

Use AlarmManager sparingly.

---

## 10. `BOOT_COMPLETED` and Rescheduling

Alarms are not always automatically restored after reboot. If your app relies on exact reminders, you often need a boot receiver:

```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<receiver
    android:name=".ReminderBootReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

Inside the receiver, rebuild alarms from persistent storage.

---

## 11. Foreground Services

A foreground service is a service that:

- starts immediately
- keeps running while doing user-visible work
- must show a persistent notification

### Basic Example

```kotlin
class TrackingService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1001, buildNotification())
        startLocationTracking()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startLocationTracking() = Unit

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, "tracking")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Tracking active")
            .setContentText("Your workout is being tracked")
            .build()
    }
}
```

### Modern Restrictions

- Android 8+: background service starts are heavily restricted
- call `startForegroundService()` when needed
- call `startForeground()` quickly after start
- Android 12+ and Android 14+ add stronger type and permission restrictions

Manifest example:

```xml
<service
    android:name=".TrackingService"
    android:exported="false"
    android:foregroundServiceType="location" />
```

---

## 12. Foreground Service vs WorkManager

### Choose WorkManager if

- task can wait a little
- user does not need to see constant progress
- reliability matters more than immediacy

Examples:

- sync drafts
- retry failed uploads
- refresh cache

### Choose Foreground Service if

- task must begin now
- task is long-running and user-visible
- stopping it would break a live user experience

Examples:

- navigation
- music playback
- live workout recording

### Simple Rule

- Music playback: foreground service
- Chat sync retry: WorkManager
- Reminder at exact time: AlarmManager
- Big file upload from a share action: WorkManager with foreground info

---

## 13. Interview Answer

If asked to compare them:

WorkManager is the default for guaranteed, deferrable background work with constraints and retries. AlarmManager is for exact time-based execution. Foreground services are for immediate, user-visible, long-running work and must show a notification.

---

## Common Mistakes

- using AlarmManager for normal sync
- using foreground services for invisible background work
- expecting periodic WorkManager to run at an exact minute
- forgetting unique work and creating duplicate jobs
- not using constraints for network-dependent work
