# Chapter 13 & 14 — Channel States & Exception Handling

---

## 📖 Definition

> Every channel exists in one of three **lifecycle states**: `OPEN`, `CLOSED`, or `CANCELLED`.  
> The buffer independently has a sub-state: `EMPTY`, `PARTIAL`, or `FULL`.  
> Each combination of lifecycle state + buffer state determines exactly what `send()` and `receive()` will do — and what exception, if any, will be thrown.

---

## 🔹 Key Bullets — States

- A channel starts in `OPEN` state — both `send()` and `receive()` work normally
- `close()` transitions to `CLOSED` — no new sends, but buffer is preserved
- `cancel()` transitions to `CANCELLED` — buffer is destroyed, all operations throw
- State transitions are **one-way**: `OPEN → CLOSED`, `OPEN → CANCELLED` (no going back)
- You cannot re-open a closed or cancelled channel
- `isClosedForSend` and `isClosedForReceive` are properties to check state programmatically

## 🔹 Key Bullets — Exceptions

- `ClosedSendChannelException` — thrown when `send()` is called on a closed/cancelled channel
- `ClosedReceiveChannelException` — thrown when `receive()` is called on a closed + empty channel
- `CancellationException` — thrown when any operation is called on a cancelled channel, or when a waiting coroutine is cancelled
- **Always re-throw `CancellationException`** — never catch and suppress it
- Exceptions from channels are **not** automatically propagated to the parent scope unless the coroutine itself is not wrapped in try/catch

---

## 🔄 Channel Lifecycle Diagram

```
Channel Created
       ↓
   [OPEN] ←─────────────── normal operations
     ↙          ↘
close()        cancel()
  ↓                ↓
[CLOSED]       [CANCELLED]
  ↓                ↓
Drain buffer    Buffer destroyed
with receive()  All operations throw
  ↓
Buffer Empty
  ↓
receive() → ClosedReceiveChannelException
```

---

## ⚙️ State 1: OPEN

```kotlin
val channel = Channel<Int>()
// State: OPEN
```

| Operation | Buffer Empty | Buffer Partial | Buffer Full |
|---|---|---|---|
| `send()` | Suspends (Rendezvous) | Stores value | Suspends |
| `receive()` | Suspends | Returns value | Returns value |

---

## ⚙️ State 2: CLOSED

```kotlin
channel.close()
// State: CLOSED
```

| Buffer State | `send()` | `receive()` |
|---|---|---|
| Has values | ❌ `ClosedSendChannelException` | ✅ Returns buffered value |
| Empty | ❌ `ClosedSendChannelException` | ❌ `ClosedReceiveChannelException` |

---

## ⚙️ State 3: CANCELLED

```kotlin
channel.cancel()
// State: CANCELLED
```

| Operation | Result |
|---|---|
| `send()` | ❌ `CancellationException` |
| `receive()` | ❌ `CancellationException` |
| Buffer | Destroyed — contents lost |

---

## 📦 Buffer Sub-States

These exist independently of the lifecycle state:

| Sub-State | Buffer | Effect |
|---|---|---|
| EMPTY | `[]` | `receive()` suspends (if OPEN) |
| PARTIAL | `[A]` | Normal — both `send()` and `receive()` work |
| FULL | `[A, B]` | `send()` suspends (or drops, per strategy) |

---

## 📋 Complete Operation Matrix

| Lifecycle State | Buffer | `send()` | `receive()` |
|---|---|---|---|
| OPEN | Empty | Suspends | Suspends |
| OPEN | Partial | ✅ Stores | ✅ Returns |
| OPEN | Full | Suspends (or drops) | ✅ Returns |
| CLOSED | Has values | ❌ `ClosedSendChannelException` | ✅ Returns |
| CLOSED | Empty | ❌ `ClosedSendChannelException` | ❌ `ClosedReceiveChannelException` |
| CANCELLED | Any | ❌ `CancellationException` | ❌ `CancellationException` |

---

## 🚨 Exception Reference

### ClosedSendChannelException
```
Thrown: send() on CLOSED or CANCELLED channel
Recovery: Cannot — the channel is done accepting values
```
```kotlin
try {
    channel.send(42)
} catch (e: ClosedSendChannelException) {
    Log.e("ch", "Cannot send — channel is closed")
}
```

### ClosedReceiveChannelException
```
Thrown: receive() on CLOSED channel with empty buffer
Recovery: No more values will ever arrive — stop consuming
```
```kotlin
try {
    val value = channel.receive()
} catch (e: ClosedReceiveChannelException) {
    Log.e("ch", "Cannot receive — channel is closed and empty")
}
```

### CancellationException
```
Thrown: any operation on CANCELLED channel
        OR: waiting coroutine is cancelled externally
Recovery: Re-throw it — do NOT suppress!
```
```kotlin
try {
    val value = channel.receive()
} catch (e: CancellationException) {
    Log.e("ch", "Channel was cancelled")
    throw e  // ← ALWAYS re-throw!
} catch (e: ClosedReceiveChannelException) {
    Log.e("ch", "Channel closed and empty")
}
```

---

## 🔍 Checking Channel State Programmatically

```kotlin
if (channel.isClosedForSend) {
    Log.d("ch", "Cannot send — channel is closed or cancelled")
}

if (channel.isClosedForReceive) {
    Log.d("ch", "Cannot receive — channel is closed and empty, or cancelled")
}
```

Use these checks to guard sends/receives in complex flows.

---

## 🌍 Real-Life Android Use Cases

### 1. Safe Channel Consumer with Full Exception Handling
```kotlin
// ViewModel
viewModelScope.launch {
    try {
        for (event in eventChannel) {
            handleEvent(event)
        }
        // for loop exits here — channel was closed normally
        Log.d("vm", "Event stream completed normally")
    } catch (e: CancellationException) {
        Log.d("vm", "Event stream cancelled (user left screen?)")
        throw e  // always re-throw
    } catch (e: Exception) {
        Log.e("vm", "Unexpected channel error", e)
    }
}
```

### 2. ViewModel Cleanup — Correct Pattern
```kotlin
class MyViewModel : ViewModel() {

    private val _events = Channel<UiEvent>(capacity = 10)
    val events = _events.receiveAsFlow()

    fun sendEvent(event: UiEvent) {
        viewModelScope.launch {
            try {
                _events.send(event)
            } catch (e: ClosedSendChannelException) {
                // ViewModel is being cleared — safe to ignore
                Log.d("vm", "ViewModel cleared, event dropped: $event")
            }
        }
    }

    override fun onCleared() {
        _events.cancel()  // CANCELLED state — clean up
        super.onCleared()
    }
}
```

### 3. Retry on Channel Failure
```kotlin
suspend fun sendWithRetry(channel: Channel<String>, value: String) {
    var attempts = 0
    while (attempts < 3) {
        try {
            channel.send(value)
            return  // success
        } catch (e: ClosedSendChannelException) {
            Log.e("ch", "Channel closed — giving up")
            return  // no point retrying on closed channel
        } catch (e: CancellationException) {
            throw e  // always re-throw cancellation
        } catch (e: Exception) {
            attempts++
            delay(1000L * attempts)  // exponential backoff
        }
    }
}
```

---

## 📊 Full Channel Types — Final Reference

| Type | Capacity | `send()` suspends? | Values lost? | Use When |
|---|---|---|---|---|
| Rendezvous | 0 | Yes (no receiver) | Never | Synchronization |
| Buffered | N | Yes (buffer full) | Never | Speed decoupling |
| Unlimited | ∞ | Never | Never | Known finite bursts |
| Conflated | 1 (latest) | Never | Yes (older) | Latest value only |

---

## ✅ Final Checklist — Complete Channel Knowledge

| # | Topic | Covered? |
|---|---|---|
| 1 | What is a Channel? | ✅ |
| 2 | `send()` — suspend behavior | ✅ |
| 3 | `receive()` — mirror of send, FIFO, non-deterministic logs | ✅ |
| 4 | Rendezvous — meeting point, synchronization | ✅ |
| 5 | Buffered — parking lot, auto-resume on space | ✅ |
| 6 | Unlimited — never suspends send, OOM risk | ✅ |
| 7 | Conflated — only latest survives, silent drops | ✅ |
| 8 | `close()` — graceful shutdown, buffer preserved | ✅ |
| 9 | `for(item in channel)` — exits on closed + empty | ✅ |
| 10 | Buffer Overflow (SUSPEND / DROP_OLDEST / DROP_LATEST) | ✅ |
| 11 | Multiple producers/consumers — fan-out, work distribution | ✅ |
| 12 | `cancel()` — emergency stop, buffer destroyed | ✅ |
| 13 | Channel States (OPEN / CLOSED / CANCELLED) | ✅ |
| 14 | Exception Handling (ClosedSend / ClosedReceive / Cancellation) | ✅ |

---

**← Back to** [README — Index](Android-Native/Native%20Android/JetPack%20Compose/02%20State%20Management/02.1%20Core%20State/Channel/README.md)
