# Chapter 12 — cancel()

---

## 📖 Definition

> `cancel()` performs an **immediate, forceful shutdown** of a channel.  
> All buffered values are **destroyed**, all waiting sender and receiver coroutines are cancelled, and all subsequent operations throw a `CancellationException`.  
> It is the emergency stop — contrasted with `close()` which is a graceful shutdown.

```kotlin
channel.cancel()
channel.cancel(cause = CancellationException("User navigated away"))
```

---

## 🔹 Key Bullets

- `cancel()` immediately transitions the channel to `CANCELLED` state
- All **buffered values are discarded** — no recovery possible
- All coroutines **suspended** on `send()` or `receive()` are cancelled
- Subsequent `send()` or `receive()` throw `CancellationException`
- You can optionally pass a `cause` parameter with a reason
- Unlike `close()`, the consumer cannot drain remaining values after `cancel()`
- `cancel()` is typically called when the **scope** (e.g., ViewModel) is destroyed
- In practice, channels tied to `viewModelScope` are automatically cancelled when the ViewModel is cleared — you often don't need to call `cancel()` manually

---

## 🆚 close() vs cancel() — The Core Distinction

```
close()  = Graceful shutdown   "Finished. Drain what's left."
cancel() = Emergency stop      "Stop everything right now."
```

| | `close()` | `cancel()` |
|---|---|---|
| New sends | ❌ Exception | ❌ Exception |
| Buffer after? | ✅ Preserved, can drain | ❌ Destroyed immediately |
| Waiting senders | Handled gracefully | Cancelled with exception |
| Waiting receivers | Can drain buffer | Cancelled with exception |
| Exception type | `ClosedSendChannelException` | `CancellationException` |

---

## 🏗️ Mental Model

```
close():
  Store closing:
  ❌ No new customers
  ✅ Existing customers can check out
  Store fully closes after last customer leaves

cancel():
  Building demolished:
  ❌ No new customers
  ❌ Everyone already inside must leave immediately
  ❌ Everything inside is destroyed
```

---

## ⚙️ Behavior After cancel()

### Buffered values — Gone
```kotlin
channel.send(1)
channel.send(2)
channel.cancel()

channel.receive()  // ← throws CancellationException (buffer is gone)
```

### Sending after cancel() — Exception
```kotlin
channel.cancel()
channel.send(10)  // ← throws CancellationException
```

### Waiting receiver — Cancelled
```kotlin
launch {
    val value = channel.receive()   // suspended here
}

delay(1000)
channel.cancel()  // ← suspended receive() throws CancellationException
```

---

## 🧪 Playground

```kotlin
fun cancelExample() {
    val channel = Channel<Int>(10)

    viewModelScope.launch {
        channel.send(1)
        channel.send(2)
        channel.send(3)
        Log.d("channel", "Cancelling now")
        channel.cancel()
    }

    viewModelScope.launch {
        delay(3000)
        try {
            repeat(3) {
                val value = channel.receive()
                Log.d("channel", "Received $value")
            }
        } catch (e: Exception) {
            Log.d("channel", "Exception: ${e::class.simpleName}")
            // Output: Exception: CancellationException
        }
    }
}
```

**Result:** After 3 seconds, you get `CancellationException`. Values 1, 2, 3 are gone.

---

## 🔁 Comparing close() and cancel() Side by Side

```kotlin
// With close()
channel.send(1)
channel.send(2)
channel.close()

channel.receive()  // → 1  ✅
channel.receive()  // → 2  ✅
channel.receive()  // → ClosedReceiveChannelException (buffer empty)
```

```kotlin
// With cancel()
channel.send(1)
channel.send(2)
channel.cancel()

channel.receive()  // → CancellationException ❌ (buffer destroyed)
```

---

## ⚠️ Always Re-Throw CancellationException!

```kotlin
try {
    val value = channel.receive()
} catch (e: CancellationException) {
    Log.d("ch", "Cancelled")
    throw e  // ← MUST re-throw — never swallow CancellationException!
} catch (e: Exception) {
    Log.d("ch", "Other error: ${e.message}")
}
```

Swallowing `CancellationException` breaks Kotlin's cooperative cancellation mechanism.

---

## 🌍 Real-Life Android Use Cases

### 1. Screen Destroyed — Cancel Active Streaming
```kotlin
class LiveDataViewModel : ViewModel() {

    private val liveChannel = Channel<SensorData>(capacity = 50)

    init {
        viewModelScope.launch {
            for (data in liveChannel) {
                _sensorState.value = data
            }
        }
    }

    // Called when user navigates away
    fun stopStreaming() {
        liveChannel.cancel()  // all buffered data dropped, consumer stops
    }

    override fun onCleared() {
        liveChannel.cancel()  // always cancel on ViewModel death
        super.onCleared()
    }
}
```

### 2. API Request Cancelled by User
```kotlin
val resultChannel = Channel<SearchResult>(capacity = 10)

val searchJob = viewModelScope.launch {
    api.streamResults(query).collect { result ->
        resultChannel.send(result)
    }
}

// User clears search or navigates away
fun onSearchCancelled() {
    searchJob.cancel()
    resultChannel.cancel()  // discard any buffered partial results
    _searchState.value = SearchState.Idle
}
```

### 3. Timeout — Cancel Channel After Deadline
```kotlin
val dataChannel = Channel<DataPacket>(capacity = 100)

viewModelScope.launch {
    withTimeout(5000L) {  // 5 second timeout
        try {
            for (packet in dataChannel) {
                processPacket(packet)
            }
        } catch (e: TimeoutCancellationException) {
            dataChannel.cancel()  // clean up on timeout
            showError("Connection timed out")
        }
    }
}
```

---

## 📋 When to Use Each

| Scenario | Use |
|---|---|
| Producer finished sending all data | `close()` |
| Screen/ViewModel destroyed | `cancel()` |
| User navigated away mid-stream | `cancel()` |
| Error occurred — abort everything | `cancel()` |
| Network request completed normally | `close()` |
| Request timed out | `cancel()` |
| All pages of an API have been loaded | `close()` |

---

## ✅ Chapter Summary

| Concept | Answer |
|---|---|
| What does `cancel()` do to the buffer? | Destroys it immediately |
| Can you drain buffer after `cancel()`? | No — everything is gone |
| Exception after `cancel()`? | `CancellationException` |
| Should you re-throw `CancellationException`? | Always yes |
| `close()` vs `cancel()` in one line? | close = graceful drain, cancel = emergency stop |

---

**Next →** [Chapter 13 & 14: States & Exception Handling](Chapter_13_14_States_and_Exceptions.md)
