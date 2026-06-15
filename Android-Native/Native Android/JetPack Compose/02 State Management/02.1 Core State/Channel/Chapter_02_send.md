# Chapter 2 — send()

---

## 📖 Definition

> `send()` is a **suspend function** that places a value into a channel.  
> It suspends the calling coroutine when the channel **cannot accept the value** — either because the buffer is full (for buffered channels) or because no receiver is currently waiting (for rendezvous channels).

```kotlin
suspend fun send(element: E)
```

---

## 🔹 Key Bullets

- `send()` is a **suspend function** — it can pause the coroutine without blocking the thread
- It suspends when the channel has **no room** to accept the value
- Once room becomes available (a receiver consumes an item), the suspended `send()` **resumes automatically**
- NOT suspended because "network is slow" or "I/O is happening" — suspended purely due to **capacity**
- What suspends is **the coroutine**, not the sender or the channel
- Any code written after `send()` may never execute if the channel is perpetually full
- A suspended `send()` resumes the moment a `receive()` frees up space

---

## 🔍 Why is send() a Suspend Function?

```
The channel may not currently have room to accept the value.
```

Not because:
- ❌ Sending is slow
- ❌ Channels are asynchronous by nature

But because:
- ✅ The buffer might be **full**
- ✅ No **receiver** might be waiting (in a Rendezvous channel)

---

## ⚙️ Behavior by Capacity

### Capacity = 0 (Rendezvous — Default)
```kotlin
val channel = Channel<String>()

launch {
    println("Before Send")
    channel.send("Hello")     // ← suspends — no receiver
    println("After Send")     // ← never reached until a receiver appears
}
```

**What happens:**
```
Before Send  → prints ✅
send("Hello") → no receiver → coroutine SUSPENDS ⏸
After Send   → never reached ❌
```

---

### Capacity = 1
```kotlin
val channel = Channel<String>(1)

launch {
    channel.send("A")     // [A] — stored immediately ✅
    println("After A")    // prints ✅

    channel.send("B")     // buffer full → SUSPENDS ⏸
    println("After B")    // never reached until A is received ❌
}
```

---

### Capacity = 2
```kotlin
val channel = Channel<String>(2)

launch {
    channel.send("A")     // [A] ✅
    println("After A")    // ✅

    channel.send("B")     // [A, B] ✅
    println("After B")    // ✅

    channel.send("C")     // buffer full → SUSPENDS ⏸
    println("After C")    // ❌
}
```

---

## 🅿️ The Parking Lot Mental Model

```
Capacity 0  →  No parking spots  →  send() waits for a driver to take the car
Capacity 1  →  1 parking spot    →  1 car parked, 2nd must wait
Capacity 2  →  2 parking spots   →  2 cars parked, 3rd must wait
Capacity N  →  N parking spots   →  N cars parked, N+1th must wait
```

---

## 🔎 Two Sends, One Receiver — Trace

```kotlin
val channel = Channel<String>()  // capacity = 0

launch {
    channel.send("A")   // suspends — no receiver yet
    channel.send("B")   // never reached until A is handed off
}
```

**Key insight:** With capacity = 0, the coroutine suspends at `send("A")`.  
`send("B")` is never even *executed* until A's handoff completes.

---

## ⚠️ Important Distinction

> A coroutine can **reach** `send()` but not yet **complete** it.

Always separate:
```
Entered function    ≠    Function completed
```

When debugging, "After Send" not printing means `send()` started but didn't finish — NOT that it was skipped.

---

## 🔁 Suspended send() Auto-Resumes

When a coroutine is suspended at `send()` because the buffer is full:
- It silently waits in a queue
- The moment a `receive()` frees a slot → **it resumes automatically**

```kotlin
val channel = Channel<Int>(2)

launch {
    channel.send(1)   // [1]
    channel.send(2)   // [1, 2] — full
    channel.send(3)   // suspends ⏸ → resumes once 1 is received
    println("3 sent") // prints AFTER receive() frees space
}

launch {
    delay(3000)
    channel.receive()  // removes 1 → [2] → triggers send(3) to resume
}
```

---

## 🌍 Real-Life Android Use Cases

### 1. Upload Queue — Rate Limiting
```kotlin
val uploadChannel = Channel<File>(capacity = 3)  // max 3 concurrent uploads

// UI thread sends files to upload
launch {
    selectedFiles.forEach { file ->
        uploadChannel.send(file)  // suspends if 3 uploads already pending
    }
}

// Worker processes uploads one by one
launch {
    for (file in uploadChannel) {
        uploadService.upload(file)
    }
}
```
> `send()` naturally **rate-limits** the producer — it won't overwhelm the upload service.

### 2. Frame Buffer (Camera / Video)
```kotlin
val frameChannel = Channel<Bitmap>(capacity = 5)

// Camera pushes frames
cameraCallback.onFrameAvailable = { bitmap ->
    viewModelScope.launch {
        frameChannel.send(bitmap)  // suspends if processor is slow
    }
}

// Processor reads and analyzes frames
launch {
    for (frame in frameChannel) {
        mlModel.analyze(frame)
    }
}
```

### 3. Form Validation Pipeline
```kotlin
val inputChannel = Channel<String>()

// User input sends text
onTextChange = { text ->
    viewModelScope.launch { inputChannel.send(text) }
}

// Validator receives and checks
launch {
    for (text in inputChannel) {
        val isValid = validator.check(text)
        _validationState.value = isValid
    }
}
```

---

## ✅ Chapter Summary

| Question | Answer |
|---|---|
| Why is `send()` suspend? | Channel may not have room |
| `Channel<String>()` + no receiver | `send()` suspends indefinitely |
| `Channel<String>(2)` — 2 values buffered? | Yes, sends complete immediately |
| What suspends? | The coroutine (not the channel or "the sender") |
| When does a suspended `send()` resume? | When `receive()` frees buffer space |

---

**Next →** [Chapter 3: receive()](Chapter_03_receive.md)
