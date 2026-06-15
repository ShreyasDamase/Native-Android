# Chapter 11 — Multiple Producers & Multiple Consumers

---

## 📖 Definition

> A **Channel** is thread-safe and can be safely shared by **multiple producer coroutines** (senders) and **multiple consumer coroutines** (receivers) simultaneously.  
> When multiple consumers exist, each value is received by **exactly one** consumer — the channel acts as a **work distribution queue**, not a broadcast mechanism.

---

## 🔹 Key Bullets

- Channels are **thread-safe** — concurrent `send()` and `receive()` calls are safe
- Multiple producers can send to the same channel without any synchronization code
- Multiple consumers split the work — each value goes to **exactly one** consumer
- This is called **fan-out** (one channel, many consumers)
- **Arrival order ≠ launch order** — whichever coroutine reaches `send()` first wins
- Channel guarantees **FIFO within its buffer** — values come out in the order they entered
- Consumers are served **fairly** — Kotlin's channel implementation handles fair scheduling
- Channel is NOT a broadcast — compare with `SharedFlow` which delivers to all collectors

---

## 🌀 Fan-Out Pattern

```
Producer A ↘
Producer B → [Channel] → Consumer 1
Producer C ↗              Consumer 2
```

vs SharedFlow (broadcast):
```
                    → Collector 1
Producer → [SharedFlow] → Collector 2
                    → Collector 3
(ALL collectors get the SAME value)
```

---

## 📐 Multiple Producers — Arrival Order Matters

```kotlin
val channel = Channel<String>(10)

launch { delay(300); channel.send("A") }  // arrives 3rd
launch { delay(100); channel.send("B") }  // arrives 1st
launch { delay(200); channel.send("C") }  // arrives 2nd

launch {
    repeat(3) { Log.d("ch", channel.receive()) }
}
```

**Output:**
```
B
C
A
```

The channel received B first (100ms), then C (200ms), then A (300ms).  
**Launch order was A, B, C — but channel received B, C, A.**

---

## 📐 Multiple Consumers — Work Distribution

```kotlin
val channel = Channel<Int>(10)

// Producer sends 10 items
viewModelScope.launch {
    repeat(10) { channel.send(it) }
}

// Consumer 1 takes 5 items
viewModelScope.launch {
    repeat(5) { Log.d("consumer1", "${channel.receive()}") }
}

// Consumer 2 takes 5 items
viewModelScope.launch {
    repeat(5) { Log.d("consumer2", "${channel.receive()}") }
}
```

**Expected distribution:**
```
consumer1 → 0
consumer2 → 1
consumer1 → 2
consumer2 → 3
...
```

Value `0` is received by consumer1 only — consumer2 never sees it.  
The 10 values are split across both consumers.

---

## 🆚 Channel vs SharedFlow

| | Channel | SharedFlow |
|---|---|---|
| Each value goes to | One consumer | All active collectors |
| Pattern | Work distribution / Fan-out | Broadcast / Pub-Sub |
| Missed events | Buffered until consumed | Dropped (unless replay used) |
| Use case | Task queues, pipelines | UI events, notifications to all |

---

## 🧪 Practical Work Queue with Multiple Workers

```kotlin
val taskChannel = Channel<String>(Channel.UNLIMITED)

// 3 workers consume from the same channel
repeat(3) { workerId ->
    launch {
        for (task in taskChannel) {
            Log.d("worker$workerId", "Processing: $task")
            delay(500)  // simulate work
        }
    }
}

// Single producer pushes 9 tasks then closes
launch {
    repeat(9) { taskChannel.send("Task $it") }
    taskChannel.close()
}
```

Each task goes to exactly one worker. The 9 tasks are distributed ~3 each.

---

## 🌍 Real-Life Android Use Cases

### 1. Image Processing Pipeline (Fan-Out)
```kotlin
val imageChannel = Channel<Uri>(capacity = 20)

// Single producer sends image URIs (from gallery selection)
viewModelScope.launch {
    selectedImages.forEach { uri -> imageChannel.send(uri) }
    imageChannel.close()
}

// 3 parallel workers compress images
repeat(3) { workerId ->
    viewModelScope.launch {
        for (uri in imageChannel) {
            val compressed = ImageCompressor.compress(uri)
            _processedImages.emit(compressed)
            Log.d("worker$workerId", "Compressed: $uri")
        }
    }
}
```
> 3 workers process images in parallel. Total time ≈ 1/3 of single-threaded time.

### 2. Multiple API Callers → Single Response Channel
```kotlin
val responseChannel = Channel<ApiResponse>(capacity = 100)

// Multiple coroutines produce API responses concurrently
listOf("users", "posts", "comments").forEach { endpoint ->
    launch {
        val response = api.fetch(endpoint)
        responseChannel.send(response)  // thread-safe — all send concurrently
    }
}

// Single aggregator collects all responses
launch {
    repeat(3) {
        val response = responseChannel.receive()
        database.save(response)
    }
}
```

### 3. Real-Time Multiplayer Input Processing
```kotlin
val inputChannel = Channel<PlayerInput>(capacity = 50)

// Each player's input coroutine sends to the same channel
players.forEach { player ->
    launch {
        player.inputStream.collect { input ->
            inputChannel.send(PlayerInput(player.id, input))
        }
    }
}

// Game engine processes all inputs in order
launch {
    for (input in inputChannel) {
        gameEngine.processInput(input)
    }
}
```
> All player inputs funnel into one processing pipeline — safe, ordered, and efficient.

---

## ✅ Chapter Summary

| Concept | Answer |
|---|---|
| Are channels thread-safe? | Yes |
| Multiple producers → safe? | Yes |
| Multiple consumers → each value received by? | Exactly one consumer |
| Arrival order vs launch order | Channel preserves arrival order, not launch order |
| Channel = broadcast? | No — use SharedFlow for broadcast |
| What pattern does multi-consumer channel implement? | Fan-out / Work distribution |

---

**Next →** [Chapter 12: cancel()](Chapter_12_cancel.md)
