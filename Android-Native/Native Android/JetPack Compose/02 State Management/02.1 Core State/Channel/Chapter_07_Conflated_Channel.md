# Chapter 7 — Conflated Channel

---

## 📖 Definition

> A **Conflated Channel** stores **only the most recent value**.  
> When a new value is sent before the previous one is received, the **older value is silently discarded** and replaced by the newer one.  
> `send()` never suspends. The channel acts like a "latest value snapshot".

```kotlin
val channel = Channel<Int>(Channel.CONFLATED)
```

---

## 🔹 Key Bullets

- Buffer size is effectively **1** — but it is NOT a normal 1-slot buffer
- When a new value arrives before the existing one is consumed → **old value is replaced**
- `send()` **never suspends** — same as Unlimited in that regard
- `receive()` **still suspends** when the buffer is empty
- Values are NOT queued — only the **latest** survives
- Older intermediate values are **permanently lost** — no recovery
- Similar to how `StateFlow` works (always emits the latest value)
- Best for scenarios where only the **most recent state** matters, not historical values

---

## 🔄 How Buffer State Changes

```
send(1) → buffer: [1]
send(2) → buffer: [2]   ← 1 is replaced (discarded)
send(3) → buffer: [3]   ← 2 is replaced (discarded)
receive() → returns 3, buffer: []
```

---

## ⚙️ Basic Example

```kotlin
val channel = Channel<Int>(Channel.CONFLATED)

channel.send(10)
channel.send(20)
channel.send(30)

println(channel.receive())  // → 30 only (10 and 20 were lost)
```

---

## ⏱️ With Delay — Shows Value Replacement Clearly

```kotlin
val channel = Channel<Int>(Channel.CONFLATED)

viewModelScope.launch {
    channel.send(1)   // [1]
    channel.send(2)   // [2] ← replaces 1
    channel.send(3)   // [3] ← replaces 2
    Log.d("ch", "Producer Finished")
}

viewModelScope.launch {
    delay(5000)
    val value = channel.receive()
    Log.d("ch", "Received $value")  // → 3
}
```

Producer sends 1, 2, 3 quickly. Consumer only ever sees **3**.

---

## 🔀 Interleaved receive() — Normal Behavior

When the buffer is empty between sends, nothing is lost:

```kotlin
val channel = Channel<Int>(Channel.CONFLATED)

channel.send(10)        // buffer: [10]
channel.send(20)        // buffer: [20]  ← 10 replaced

println(channel.receive())  // → 20, buffer: []

channel.send(30)        // buffer: [30]  (nothing to replace — was empty)

println(channel.receive())  // → 30, buffer: []
```

> Conflated only drops values when they pile up **before being consumed**.

---

## 🆚 Conflated vs Others

| | Rendezvous | Buffered(N) | Unlimited | Conflated |
|---|---|---|---|---|
| Storage | None | N slots | Grows | 1 slot (latest only) |
| `send()` suspends? | Yes (no receiver) | Yes (full) | Never | Never |
| Values lost? | Never | Never | Never | Yes (older ones) |
| Consumer gets | All | All in order | All in order | Only the latest |

---

## 🐛 From Practice — The Conflated Bug

You accidentally had:
```kotlin
val channel = Channel<Int>(Channel.CONFLATED)
```
When you expected:
```kotlin
val channel = Channel<Int>(2)
```

So when you sent 1, 2 and then did a `for(item in channel)`:
```
send(1) → buffer: [1]
send(2) → buffer: [2]  ← 1 replaced
close()
for loop → only receives 2
```

This revealed the core behavior: conflated channel silently **ate** value 1.

---

## ✅ Good Use Cases

| Scenario | Why Conflated Works |
|---|---|
| Download progress (%) | UI only needs current % |
| GPS coordinates | Only latest position matters |
| Sensor readings | Latest reading supersedes older ones |
| Network strength indicator | Only show current signal level |
| Live stock price | Only latest price is relevant |
| Loading state (true/false) | Latest state is all that matters |

## ❌ Bad Use Cases

| Scenario | Why Conflated Fails |
|---|---|
| Chat messages | Every message must be shown |
| Payment transactions | Cannot afford to lose any |
| Push notifications | Every notification must be delivered |
| File upload queue | Every file must be uploaded |
| Error reporting | Every error must be logged |

---

## 🌍 Real-Life Android Use Cases

### 1. Download Progress Bar
```kotlin
val progressChannel = Channel<Int>(Channel.CONFLATED)

// Network layer sends progress updates very frequently
viewModelScope.launch {
    downloadManager.start(url) { percentComplete ->
        viewModelScope.launch {
            progressChannel.send(percentComplete)  // never suspends
        }
    }
}

// UI collects and shows — if UI is slow, it only shows latest %
viewModelScope.launch {
    for (progress in progressChannel) {
        _uiState.value = _uiState.value.copy(downloadProgress = progress)
    }
}
```
> If network fires 100 progress events and UI processes 10 per second, the UI still shows recent progress — not stale data.

### 2. Live GPS Location on Map
```kotlin
val locationChannel = Channel<LatLng>(Channel.CONFLATED)

// GPS sends updates frequently (every 1 second)
locationManager.onLocationUpdate = { latLng ->
    viewModelScope.launch {
        locationChannel.send(latLng)  // old location replaced if not yet displayed
    }
}

// Map updates with the latest location
viewModelScope.launch {
    for (location in locationChannel) {
        mapController.moveCameraTo(location)
    }
}
```

### 3. Search Bar Typing — Latest Query Only
```kotlin
val queryChannel = Channel<String>(Channel.CONFLATED)

// Each keystroke sends a query
onQueryChange = { text ->
    viewModelScope.launch { queryChannel.send(text) }
}

// Search only uses the latest query (no point searching for old incomplete text)
viewModelScope.launch {
    for (query in queryChannel) {
        delay(300)  // debounce-like behavior
        val results = searchApi.search(query)
        _searchResults.value = results
    }
}
```

---

## ✅ Chapter Summary

| Concept | Answer |
|---|---|
| How many values stored? | 1 — the latest only |
| `send()` ever suspends? | No — never |
| `receive()` ever suspends? | Yes — when empty |
| What happens to old value when new arrives? | Silently discarded |
| `channel.send(10); send(20); receive()` → what prints? | 20 |
| `channel.send(10); receive(); send(30); receive()` → what prints? | 10, then 30 |

---

**Next →** [Chapter 8: close()](Chapter_08_close.md)
