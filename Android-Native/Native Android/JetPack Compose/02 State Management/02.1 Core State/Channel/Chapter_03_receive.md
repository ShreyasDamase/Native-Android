# Chapter 3 — receive()

---

## 📖 Definition

> `receive()` is a **suspend function** that retrieves the next value from a channel.  
> It suspends the calling coroutine when the channel is **empty** — waiting until a sender provides a value.

```kotlin
suspend fun receive(): E
```

It is the **mirror image** of `send()`.

---

## 🔹 Key Bullets

- `receive()` suspends when the channel **has no value** to return
- `receive()` returns **immediately** if a value is already buffered
- The coroutine suspends at `receive()` — not before, not after
- Any code before `receive()` runs normally; code after may not if `receive()` suspends
- Channels follow **FIFO** (First-In, First-Out) — values come out in the order they went in
- **Log order from concurrent coroutines is non-deterministic** — don't trust it to prove execution order
- What suspends is **the coroutine**, not the receiver or the channel

---

## 📐 send() vs receive() — Mirror Image

```
send()     suspends when:  no space  (buffer full / no receiver waiting)
receive()  suspends when:  no value  (buffer empty / no sender waiting)
```

---

## ⚙️ Behavior

### Case 1: Channel Empty → Coroutine Suspends

```kotlin
launch {
    _uiText.value = "Before Receive"    // ← executes ✅
    val value = channel.receive()        // ← suspends ⏸ (no sender)
    _uiText.value = value                // ← never reached ❌
}
```

**Timeline:**
```
Line 1: _uiText = "Before Receive"  → runs successfully ✅
Line 2: channel.receive()           → empty channel → suspends ⏸
Line 3: _uiText = value             → never reached ❌
```

> ⚠️ "Before Receive" is **NOT cancelled** — it completed successfully.  
> The coroutine is simply **waiting** at `receive()`.

---

### Case 2: Value Already Exists → Returns Immediately

```kotlin
val channel = Channel<String>(1)
channel.send("Hello")          // buffered: [Hello]

val value = channel.receive()  // returns "Hello" immediately ✅ (no suspension)
```

**The rule:**
```
receive() asks: "Is there a value available?"
  YES → return immediately (no suspension)
  NO  → suspend and wait for a sender
```

---

## ⏱️ Timeline Experiment — Receiver Waits for Sender

```kotlin
val channel = Channel<String>()

launch {
    _uiText.value = "Receiver Waiting"
    val value = channel.receive()   // suspends
    _uiText.value = value
}

launch {
    delay(3000)
    channel.send("Hello")           // wakes up receiver
}
```

**Timeline:**
```
0 sec → "Receiver Waiting" displayed
0 sec → receive() suspends (channel empty)
3 sec → send("Hello") → handoff occurs
3 sec → receive() resumes → _uiText = "Hello"
```

---

## 📋 FIFO Order

Channels always deliver values in **First-In, First-Out** order:

```kotlin
val channel = Channel<String>(3)
channel.send("A")
channel.send("B")
channel.send("C")

println(channel.receive())  // A
println(channel.receive())  // B
println(channel.receive())  // C
```

The first value sent is the first value received.

---

## 🔁 Buffered Channel + receive() — Buffer Space Freed

```kotlin
val channel = Channel<Int>(2)

launch {
    channel.send(1)   // [1]
    channel.send(2)   // [1, 2] — full
    channel.send(3)   // ← suspends (buffer full)
    println("3 sent") // prints only after a receive() frees space
}

launch {
    delay(3000)
    channel.receive()  // takes 1 → buffer: [2] → send(3) resumes
}
```

---

## ⚠️ Log Order is Non-Deterministic

```kotlin
launch { channel.send("A");    Log.d("ch", "A sent") }
launch { channel.receive();    Log.d("ch", "A received") }
```

After the handshake, both coroutines are **runnable** at the same time. The scheduler picks who logs first. You might see:

```
A sent
A received
```
or:
```
A received
A sent
```

Both are valid. **Never use log order to prove execution order.**

```
Channels guarantee: data ordering   ✅
Coroutines do NOT guarantee: log ordering ❌
```

---

## 🌍 Real-Life Android Use Cases

### 1. One-Time UI Events (ViewModel → Composable)
```kotlin
// ViewModel
private val _uiEvent = Channel<UiEvent>()
val uiEvent = _uiEvent.receiveAsFlow()

fun onLoginClick() {
    viewModelScope.launch {
        val result = authRepository.login()
        if (result.isSuccess) {
            _uiEvent.send(UiEvent.NavigateToHome)
        } else {
            _uiEvent.send(UiEvent.ShowSnackbar("Login failed"))
        }
    }
}

// Composable — receive() under the hood via collect
LaunchedEffect(Unit) {
    viewModel.uiEvent.collect { event ->
        when (event) {
            is UiEvent.NavigateToHome -> navController.navigate("home")
            is UiEvent.ShowSnackbar  -> snackbarHostState.showSnackbar(event.message)
        }
    }
}
```

### 2. BLE / Sensor Data Stream
```kotlin
val sensorChannel = Channel<SensorReading>(capacity = 20)

// Sensor callback pushes readings
bluetoothSensor.onDataReceived = { bytes ->
    viewModelScope.launch {
        sensorChannel.send(SensorReading.from(bytes))
    }
}

// Processing coroutine receives readings
viewModelScope.launch {
    for (reading in sensorChannel) {
        processAndDisplay(reading)
    }
}
```
> `receive()` naturally **suspends** when no sensor data is available, using zero CPU while waiting.

### 3. Paginated Data Loading
```kotlin
val pageChannel = Channel<Int>()

// User scrolls to bottom → sends page number
onScrollToBottom = { pageNumber ->
    viewModelScope.launch { pageChannel.send(pageNumber) }
}

// Loader receives page requests
viewModelScope.launch {
    for (page in pageChannel) {
        val data = api.fetchPage(page)
        _items.value = _items.value + data
    }
}
```

---

## ✅ Chapter Summary

| Concept | Answer |
|---|---|
| Why is `receive()` suspend? | Channel may have no value to return |
| Empty channel → `receive()`? | Coroutine suspends |
| Value available → `receive()`? | Returns immediately |
| "Before Receive" printed then stuck? | `receive()` suspended — "Before Receive" was NOT cancelled |
| Channel ordering? | FIFO always |
| Can you trust log order? | No — concurrent coroutines have non-deterministic log order |

---

**Next →** [Chapter 4: Rendezvous Channel](Chapter_04_Rendezvous_Channel.md)
