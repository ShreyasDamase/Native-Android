# Chapter 6 — Unlimited Channel

---

## 📖 Definition

> An **Unlimited Channel** is a channel with an **unbounded buffer** that can grow dynamically.  
> `send()` **never suspends** — values are always accepted immediately regardless of how many are already buffered.  
> `receive()` still suspends when the buffer is empty.

```kotlin
val channel = Channel<Int>(Channel.UNLIMITED)
```

---

## 🔹 Key Bullets

- `send()` **never suspends** — the buffer grows as needed
- `receive()` **still suspends** if the buffer is empty (same as always)
- There is **no upper bound** on how many items can be buffered
- The buffer is backed by a **linked list** internally — grows on demand
- Risk: if the producer is always faster than the consumer, memory grows indefinitely → `OutOfMemoryError`
- Values are still delivered in **FIFO order**
- Useful for **known-finite, bursty** workloads where you don't want the producer to block
- NOT suitable for high-frequency, unbounded streams

---

## ⚙️ How It Behaves

```kotlin
val channel = Channel<Int>(Channel.UNLIMITED)

channel.send(1)   // [1]      — never suspends ✅
channel.send(2)   // [1, 2]   — never suspends ✅
channel.send(3)   // [1,2,3]  — never suspends ✅
// ...send 1000 items... — still never suspends ✅
```

No matter how many items you send, `send()` always returns immediately.

---

## 🆚 Unlimited vs Buffered

| | Buffered(N) | Unlimited |
|---|---|---|
| `send()` suspends? | Yes, when buffer is full | Never |
| Buffer size | Fixed (N) | Grows dynamically |
| Risk | Producer blocked if consumer slow | OutOfMemoryError |
| Use case | Controlled backpressure | Finite, bursty workloads |

---

## ⚠️ The OOM Risk — Visualized

```
Producer: sends 1000 items/sec
Consumer: processes 10 items/sec

After 10 seconds:
  Buffer contains: ~10,000 items → memory pressure
After 60 seconds:
  Buffer contains: ~60,000 items → OutOfMemoryError 💥
```

This is why Unlimited should only be used when you **know** the producer will stop or the consumer will catch up.

---

## 🌍 Real-Life Android Use Cases

### 1. Collecting All User Interactions for Session Replay
```kotlin
val interactionChannel = Channel<UserInteraction>(Channel.UNLIMITED)

// Tap/scroll/type events fire rapidly — we never want to block UI
fun onUserInteraction(event: UserInteraction) {
    viewModelScope.launch {
        interactionChannel.send(event)  // never suspends — UI stays responsive
    }
}

// Background worker uploads when session ends
viewModelScope.launch {
    val session = mutableListOf<UserInteraction>()
    for (event in interactionChannel) {
        session.add(event)
    }
    // session ends → channel closed → upload full session
    sessionReplayService.upload(session)
}
```
> Safe here because a user session has a bounded number of interactions.

### 2. Fan-Out Task Distribution
```kotlin
val taskChannel = Channel<WorkItem>(Channel.UNLIMITED)

// Batch job pushes all tasks upfront (known count)
viewModelScope.launch {
    tasks.forEach { taskChannel.send(it) }
    taskChannel.close()
}

// Multiple workers consume from the same channel
repeat(4) { workerId ->
    launch {
        for (task in taskChannel) {
            processTask(workerId, task)
        }
    }
}
```

### 3. Event Collection During Offline Mode
```kotlin
val offlineEventChannel = Channel<AnalyticsEvent>(Channel.UNLIMITED)

// Events fire even without internet
fun trackEvent(event: AnalyticsEvent) {
    viewModelScope.launch {
        offlineEventChannel.send(event)
    }
}

// When connection is restored — flush all events
viewModelScope.launch {
    networkMonitor.awaitConnection()
    offlineEventChannel.close()
    val events = mutableListOf<AnalyticsEvent>()
    for (event in offlineEventChannel) {
        events.add(event)
    }
    analyticsService.batchUpload(events)
}
```

---

## ✅ Chapter Summary

| Concept | Answer |
|---|---|
| Does `send()` ever suspend? | No — never |
| Does `receive()` ever suspend? | Yes — when empty |
| Buffer limit? | None — grows as needed |
| Main risk? | OutOfMemoryError if producer outpaces consumer long-term |
| Values in order? | Yes — FIFO |

---

**Next →** [Chapter 7: Conflated Channel](Chapter_07_Conflated_Channel.md)
