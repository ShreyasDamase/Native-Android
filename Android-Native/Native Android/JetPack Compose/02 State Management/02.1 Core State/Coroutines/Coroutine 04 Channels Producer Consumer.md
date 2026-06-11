# Coroutine 04 Channels Producer Consumer

This note covers Channels and producer-consumer patterns.

---

## What Is a Channel?

### Meaning

- A Channel is a coroutine communication primitive.
- It works like a queue between coroutines.
- One coroutine sends values.
- Another coroutine receives values.
- Channels are hot: they exist independently of a collector.

```kotlin
val channel = Channel<Int>()

launch {
    channel.send(1)
    channel.send(2)
    channel.close()
}

launch {
    for (value in channel) {
        println(value)
    }
}
```

### Interview explanation

- "A Channel is like a coroutine-friendly queue. It is useful when one coroutine produces work and another consumes it."

---

## Channel Capacity Types

| Capacity | Meaning | Use case |
|---|---|---|
| `Channel.RENDEZVOUS` | No buffer. Sender waits for receiver. | Strict handoff |
| `Channel.BUFFERED` | Has buffer. Sender waits when full. | Normal queue |
| `Channel.CONFLATED` | Keeps latest value only. | Latest progress/state |
| `Channel.UNLIMITED` | Unlimited buffer. Risk of memory growth. | Rare; use carefully |

---

## Producer-Consumer Example

```kotlin
fun CoroutineScope.produceNumbers(): ReceiveChannel<Int> = produce {
    for (i in 1..5) {
        send(i)
    }
}

fun consumeNumbers() = runBlocking {
    val numbers = produceNumbers()
    for (number in numbers) {
        println(number)
    }
}
```

---

## Android Real-Life Example: Analytics Queue

```kotlin
class AnalyticsTracker {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val events = Channel<AnalyticsEvent>(Channel.BUFFERED)

    init {
        scope.launch {
            for (event in events) {
                sendToServer(event)
            }
        }
    }

    fun track(event: AnalyticsEvent) {
        events.trySend(event)
    }

    fun close() {
        scope.cancel()
    }
}
```

### Why this is useful

- UI can quickly enqueue analytics events.
- One background coroutine sends events one by one.
- App avoids launching unlimited network coroutines.

---

## Channel vs Flow

| Topic | Channel | Flow |
|---|---|---|
| Main idea | Queue between coroutines | Stream API |
| Cold/hot | Hot | Usually cold |
| Consumers | Usually one receiver | Many operators/collectors possible |
| Android UI | Less common | More common |
| Use case | Work queue | UI/data observation |

### Interview explanation

- "Flow is preferred for UI streams and reactive data. Channel is better for producer-consumer queues or one-at-a-time background work."

---

## Common Mistakes

- Using Channel for UI state when `StateFlow` is better.
- Using unlimited channels without backpressure plan.
- Forgetting to close/cancel the scope that owns the channel.
- Assuming Channel is lifecycle-aware by itself.
