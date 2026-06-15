# Chapter 10 — Buffer Overflow Strategy

---

## 📖 Definition

> **Buffer Overflow Strategy** controls what happens when `send()` is called on a **full buffer**.  
> By default the sender coroutine **suspends** (waits for space). But you can configure the channel to instead **drop values** — either the oldest or the newest — so that `send()` never suspends.

```kotlin
val channel = Channel<T>(
    capacity = N,
    onBufferOverflow = BufferOverflow.SUSPEND        // default
    // or: BufferOverflow.DROP_OLDEST
    // or: BufferOverflow.DROP_LATEST
)
```

---

## 🔹 Key Bullets

- By default, a full buffer causes `send()` to **suspend** — this is `SUSPEND` strategy
- `DROP_OLDEST` removes the **oldest** buffered value to make room for the new one
- `DROP_LATEST` discards the **incoming** (new) value and keeps the existing buffer unchanged
- `DROP_OLDEST` and `DROP_LATEST` both make `send()` **never suspend**
- With drop strategies, values are **silently lost** — no exception is thrown
- The `onBufferOverflow` parameter requires **capacity ≥ 1** (doesn't apply to Rendezvous)
- Choosing the right strategy depends on whether **old data or new data** is more important
- Conflated channel is essentially `Channel(1, DROP_OLDEST)` under the hood

---

## 🧩 Three Strategies Explained

### Strategy 1: SUSPEND (Default)
```kotlin
Channel<Int>(3, onBufferOverflow = BufferOverflow.SUSPEND)
```

Buffer full → sender **waits** until space is available.  
Nothing is lost. Producer is slowed down to match consumer speed.

```
Buffer: [1, 2, 3] ← full
send(4) → SUSPENDS ⏸ → waits
receive() removes 1 → [2, 3]
send(4) resumes → [2, 3, 4]
```

---

### Strategy 2: DROP_OLDEST
```kotlin
Channel<Int>(3, onBufferOverflow = BufferOverflow.DROP_OLDEST)
```

Buffer full → **oldest item dropped**, new item stored immediately.  
`send()` never suspends. Latest data always wins.

```
Buffer: [1, 2, 3] ← full
send(4) → 1 dropped → buffer: [2, 3, 4]  ← immediately ✅
send(5) → 2 dropped → buffer: [3, 4, 5]  ← immediately ✅
```

---

### Strategy 3: DROP_LATEST
```kotlin
Channel<Int>(3, onBufferOverflow = BufferOverflow.DROP_LATEST)
```

Buffer full → **new incoming item dropped**, buffer unchanged.  
`send()` never suspends. Existing queue protected.

```
Buffer: [1, 2, 3] ← full
send(4) → 4 dropped → buffer: [1, 2, 3] (unchanged)
send(5) → 5 dropped → buffer: [1, 2, 3] (unchanged)
```

---

## 📊 Comparison Table

| Strategy | Buffer Full? | What happens | `send()` suspends? | Data lost? |
|---|---|---|---|---|
| `SUSPEND` | Yes | Sender waits | ✅ Yes | Never |
| `DROP_OLDEST` | Yes | Oldest item removed | ❌ No | Oldest value |
| `DROP_LATEST` | Yes | New item discarded | ❌ No | Newest value |

---

## 🎯 Choosing the Right Strategy

| Situation | Strategy |
|---|---|
| Every item must be processed (no loss acceptable) | `SUSPEND` |
| Latest data is most important (discard stale data) | `DROP_OLDEST` |
| First-come-first-served (preserve existing queue) | `DROP_LATEST` |
| GPS / sensor / progress updates | `DROP_OLDEST` |
| Task queue with strict ordering | `SUSPEND` |
| UI refresh throttling (show latest state) | `DROP_OLDEST` |

---

## 🌍 Real-Life Android Use Cases

### 1. Live Stock Price Feed — DROP_OLDEST
```kotlin
val priceChannel = Channel<StockPrice>(
    capacity = 5,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)

// WebSocket pushes prices very frequently
webSocket.onMessage = { json ->
    viewModelScope.launch {
        priceChannel.send(StockPrice.from(json))  // always accepted, stale prices dropped
    }
}

// UI updates with prices — always gets freshest data
viewModelScope.launch {
    for (price in priceChannel) {
        _priceState.value = price
    }
}
```
> Even if the UI is slow, it always displays the **latest** price, not a stale one from 5 seconds ago.

### 2. Keyboard Input Debounce Buffer — DROP_LATEST
```kotlin
val inputChannel = Channel<String>(
    capacity = 1,
    onBufferOverflow = BufferOverflow.DROP_LATEST
)

// Fires on every keystroke, but only 1 queued at a time
onTextChange = { text ->
    viewModelScope.launch {
        inputChannel.send(text)  // new input dropped if previous not yet processed
    }
}

// Processes inputs with artificial delay (debounce-like)
viewModelScope.launch {
    for (text in inputChannel) {
        delay(300)
        val results = searchApi.search(text)
        _results.value = results
    }
}
```
> Prevents flooding the search API — keeps the first queued input and ignores rapid subsequent ones.

### 3. Error Reporting with Guaranteed Delivery — SUSPEND
```kotlin
val errorChannel = Channel<ErrorReport>(
    capacity = 20,
    onBufferOverflow = BufferOverflow.SUSPEND   // never drop an error
)

// App-wide error collection — producers wait if buffer is full
fun reportError(error: Throwable) {
    viewModelScope.launch {
        errorChannel.send(ErrorReport(error))  // suspends if queue full — that's fine
    }
}

// Background uploader
viewModelScope.launch {
    for (report in errorChannel) {
        crashlyticsService.send(report)
        delay(100)  // rate limit uploads
    }
}
```
> Every error must reach Crashlytics — no silent drops allowed → `SUSPEND` is correct.

---

## ✅ Chapter Summary

| Question | Answer |
|---|---|
| Default overflow strategy? | `SUSPEND` |
| Which strategy never suspends `send()`? | `DROP_OLDEST` and `DROP_LATEST` |
| `DROP_OLDEST` — which value is removed? | The oldest (earliest) buffered value |
| `DROP_LATEST` — which value is removed? | The incoming (newest) value — buffer unchanged |
| Is the drop silent? | Yes — no exception, no notification |
| Conflated channel is similar to? | `Channel(1, DROP_OLDEST)` |

---

**Next →** [Chapter 11: Multiple Producers & Consumers](Chapter_11_Multiple_Producers_Consumers.md)
