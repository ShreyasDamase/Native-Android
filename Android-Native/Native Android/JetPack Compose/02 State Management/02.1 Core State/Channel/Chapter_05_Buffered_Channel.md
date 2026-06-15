# Chapter 5 — Buffered Channel

---

## 📖 Definition

> A **Buffered Channel** is a channel with a **fixed-size internal buffer**.  
> The sender can store up to `capacity` items without waiting for a receiver.  
> `send()` only suspends when the buffer is **full**. `receive()` only suspends when the buffer is **empty**.

```kotlin
val channel = Channel<Int>(capacity = N)
```

---

## 🔹 Key Bullets

- Buffer acts like a **queue with a size limit**
- `send()` completes immediately if there is space in the buffer
- `send()` **suspends** only when the buffer is completely full
- `receive()` completes immediately if there is a buffered value
- `receive()` suspends only when the buffer is empty
- When a suspended `send()` is waiting and a `receive()` frees a slot → **the suspended send() automatically resumes**
- Buffering **decouples** producer and consumer speeds — producer can run ahead up to `capacity` items
- Values are always delivered in **FIFO** order (first stored = first received)
- Unlike Rendezvous, the sender does **not** need the receiver to be ready at the same time

---

## 🅿️ The Parking Lot Mental Model

```
Capacity 0  → No parking spots → send() must wait for a driver to collect immediately
Capacity 1  → 1 parking spot   → 1 car parked, 2nd must wait
Capacity 2  → 2 spots          → 2 cars parked, 3rd must wait
Capacity N  → N spots          → N cars parked, N+1th must wait
```

---

## 📦 Buffer State Tracing

### Channel(2) — Trace

```
Initial:    []
send("A"):  [A]         → send completes ✅ → "After A" prints
send("B"):  [A, B]      → send completes ✅ → "After B" prints
send("C"):  FULL ⏸      → coroutine suspends
```

After a `receive()`:
```
receive():  takes A → [B]  → slot freed
            send("C") resumes automatically → [B, C]
            "After C" prints
```

---

## ⚙️ Code Example: Capacity 2

```kotlin
val channel = Channel<String>(2)

launch {
    channel.send("A")
    println("After A")    // ✅ prints immediately

    channel.send("B")
    println("After B")    // ✅ prints immediately

    channel.send("C")     // ⏸ suspends — buffer full
    println("After C")    // ❌ waits for a receive()
}

launch {
    delay(3000)
    channel.receive()     // takes A → frees slot → "After C" prints
}
```

---

## 🔁 Suspended send() Auto-Resumes

This is one of the most important concepts:

```kotlin
val channel = Channel<Int>(2)

launch {
    channel.send(1)   // [1]
    channel.send(2)   // [1, 2] — full
    channel.send(3)   // suspends ⏸ — waiting for space
    Log.d("ch", "3 sent")  // prints only after receive() frees space
}

launch {
    delay(3000)
    channel.receive()  // removes 1 → buffer: [2] → space freed
                       // → send(3) resumes → buffer: [2, 3]
}
```

---

## 🧪 Scenario 1: Fast Producer, Slow Consumer

```kotlin
val channel = Channel<Int>(3)

// Producer — fast (sends immediately)
viewModelScope.launch {
    repeat(5) {
        Log.d("channel", "Sending $it")
        channel.send(it)
    }
    Log.d("channel", "Producer Finished")
}

// Consumer — slow (waits 5 seconds before starting)
viewModelScope.launch {
    delay(5000)
    repeat(5) {
        val value = channel.receive()
        Log.d("channel", "Received $value")
        delay(1000)
    }
}
```

**What you observe:**
```
0 sec  → Sending 0, Sending 1, Sending 2  (buffer: [0,1,2])
0 sec  → Sending 3 → suspends (buffer full)

5 sec  → Received 0  (buffer: [1,2] → space freed)
         Sending 3 resumes → buffer: [1,2,3]
         Sending 4 → suspends again

6 sec  → Received 1, Sending 4 resumes
         ...and so on
```

---

## 🧪 Scenario 2: Slow Producer, Fast Consumer

```kotlin
val channel = Channel<Int>(3)

// Consumer — starts immediately
viewModelScope.launch {
    repeat(5) {
        Log.d("channel", "Consumer waiting")
        val value = channel.receive()   // suspends immediately (empty)
        Log.d("channel", "Received $value")
    }
}

// Producer — starts after 5 seconds
viewModelScope.launch {
    delay(5000)
    repeat(5) {
        Log.d("channel", "Sending $it")
        channel.send(it)
        delay(1000)
    }
}
```

**What you observe:**
```
Immediately → "Consumer waiting" → receive() suspends
5 sec       → "Sending 0" → handoff → "Received 0"
             "Consumer waiting" → suspends again
6 sec       → "Sending 1" → handoff → "Received 1"
             ...
```

---

## 🧪 Scenario 3: Different Speeds (Producer 300ms, Consumer 1500ms)

```kotlin
val channel = Channel<Int>(2)

launch {
    repeat(10) {
        Log.d("channel", "Send $it")
        channel.send(it)
        delay(300)
    }
}

launch {
    repeat(10) {
        delay(1500)
        val value = channel.receive()
        Log.d("channel", "Receive $value")
    }
}
```

Producer is 5× faster. Buffer absorbs the initial speed difference, then producer gets blocked. This makes buffering behavior very visible in logs.

---

## 🌍 Real-Life Android Use Cases

### 1. Image Compression Pipeline
```kotlin
val rawImageChannel = Channel<Bitmap>(capacity = 10)

// Camera callback sends raw frames
onFrameAvailable = { bitmap ->
    viewModelScope.launch { rawImageChannel.send(bitmap) }
}

// Compressor receives and processes
viewModelScope.launch {
    for (bitmap in rawImageChannel) {
        val compressed = ImageCompressor.compress(bitmap)
        saveToGallery(compressed)
    }
}
```
> Buffer of 10 absorbs bursts — camera doesn't block if the compressor is momentarily slow.

### 2. Network Request Queue with Rate Limiting
```kotlin
val requestChannel = Channel<ApiRequest>(capacity = 5)

// UI sends requests (max 5 queued at a time)
fun fetchData(query: String) {
    viewModelScope.launch {
        requestChannel.send(ApiRequest(query))  // suspends if 5 already pending
    }
}

// Worker processes one at a time
viewModelScope.launch {
    for (request in requestChannel) {
        val result = apiService.execute(request)
        _results.emit(result)
    }
}
```

### 3. Log Aggregator
```kotlin
val logChannel = Channel<LogEntry>(capacity = 100)

// Many parts of the app log events
fun log(message: String) {
    viewModelScope.launch {
        logChannel.send(LogEntry(message, System.currentTimeMillis()))
    }
}

// Background uploader batches logs
viewModelScope.launch {
    val batch = mutableListOf<LogEntry>()
    for (entry in logChannel) {
        batch.add(entry)
        if (batch.size >= 50) {
            logUploader.upload(batch.toList())
            batch.clear()
        }
    }
}
```

---

## ✅ Chapter Summary

| Concept | Answer |
|---|---|
| `send()` suspends when? | Buffer is full |
| `receive()` suspends when? | Buffer is empty |
| What happens to suspended `send()` when space opens? | Resumes automatically |
| Values always FIFO? | Yes |
| Buffer decouples what? | Producer and consumer speeds |

---

**Next →** [Chapter 6: Unlimited Channel](Chapter_06_Unlimited_Channel.md)
