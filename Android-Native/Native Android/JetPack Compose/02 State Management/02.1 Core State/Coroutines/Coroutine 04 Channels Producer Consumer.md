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


---

## 🚀 Mastery Deep Dive (Added 2026)

> [!NOTE]
> The following deep dive notes were generated to provide mastery-level understanding, complementing the original notes above.

# Coroutine 04 — Channels, callbackFlow & channelFlow

> [!NOTE]
> Channels are the lower-level primitive that Flow is built on. Understanding them makes you understand WHY `callbackFlow` and `channelFlow` exist, and when to reach for them over regular `flow { }`.

---

## 🧠 Mental Model — Read This First

**Think of a factory conveyor belt.**

- A **Channel** is the conveyor belt — it connects two workers (coroutines). One worker (producer) puts boxes on the belt. The other (consumer) takes boxes off.
- The belt has a **buffer** — it can hold a limited number of boxes. If the belt is full, the producer must wait. If the belt is empty, the consumer must wait.
- This is fundamentally different from `Flow`, which is more like a **pipe** — data flows through when someone is ready to receive it (pull-based on demand).

The key insight: **Channel is hot** (the belt exists independently of whether a consumer is watching). **Flow is cold** (the pipe only flows when a consumer pulls).

---

## 📦 What Is a Channel?

A `Channel<T>` is a coroutine-friendly **queue** between a producer coroutine and a consumer coroutine.

```kotlin
val channel = Channel<Int>()   // create a channel

// Producer coroutine
launch {
    for (i in 1..5) {
        channel.send(i)        // send suspends if channel buffer is full
        println("Sent: $i")
    }
    channel.close()            // signal: no more values coming
}

// Consumer coroutine
launch {
    for (value in channel) {   // iterates until channel is closed
        println("Received: $value")
    }
}

// Output: Sent: 1, Received: 1, Sent: 2, Received: 2, ...
```

**Key operations:**

| Operation | Behavior | Type |
|---|---|---|
| `channel.send(value)` | Sends a value; suspends if buffer is full | `suspend` |
| `channel.receive()` | Receives a value; suspends if channel is empty | `suspend` |
| `channel.trySend(value)` | Non-suspending send; returns `ChannelResult` (success/failure) | Regular |
| `channel.tryReceive()` | Non-suspending receive; returns `ChannelResult` | Regular |
| `channel.close()` | Marks channel as done; consumer loop ends | Regular |
| `for (v in channel)` | Receives until closed | Uses `receive()` |

---

## 🔧 Channel Capacity Types — Choosing the Right Buffer

```kotlin
// 1. RENDEZVOUS (default, capacity = 0)
// Producer and consumer must meet at the same time.
// send() suspends until receive() is called, and vice versa.
val channel = Channel<String>()   // or Channel(Channel.RENDEZVOUS)
// Use when: strict handoff, like a relay race baton pass

// 2. BUFFERED (capacity = 64 by default)
// Has a fixed-size buffer. Producer can send without waiting until buffer is full.
val channel = Channel<String>(Channel.BUFFERED)   // or Channel(64)
// Use when: producer is faster than consumer, you want to decouple them

// 3. UNLIMITED
// Never suspends sender — buffer grows without bound.
// DANGEROUS: producer could fill up memory if consumer is slow
val channel = Channel<String>(Channel.UNLIMITED)
// Use when: you're certain the total number of items is bounded

// 4. CONFLATED (capacity = 1, overwrites instead of queuing)
// New value overwrites old if consumer hasn't read it yet.
// Consumer always gets the LATEST value, may miss intermediate ones.
val channel = Channel<String>(Channel.CONFLATED)
// Use when: only the latest value matters (like current location, latest stock price)
```

**Visual:**
```
RENDEZVOUS:  Producer ──wait──► Consumer  (no buffer, synchronized handoff)
BUFFERED:    Producer ──[□□□□□□□]──► Consumer  (buffer = 64 items)
UNLIMITED:   Producer ──[□□□□□□□□□□□□□...]──► Consumer  (infinite buffer)
CONFLATED:   Producer ──[latest]──► Consumer  (always replaces, no queue)
```

---

## 🏭 Producer-Consumer Pattern with `produce {}`

The `produce {}` coroutine builder creates a coroutine that produces values into a `ReceiveChannel<T>`:

```kotlin
// produce{} is an extension on CoroutineScope
fun CoroutineScope.generateNumbers(max: Int): ReceiveChannel<Int> = produce {
    for (i in 1..max) {
        send(i)          // send values into the channel
        delay(100)       // simulate work
    }
    // channel is automatically closed when produce{} block ends
}

// Consumer
fun main() = runBlocking {
    val numbers = generateNumbers(5)
    for (number in numbers) {
        println("Processing: $number")
    }
}
```

**Note:** `produce {}` automatically closes the channel when its block completes (or throws). You don't need to call `close()` manually.

---

## 📱 Real Android Use Case: Analytics Event Queue

This is one of the most practical Channel use cases in real apps:

```kotlin
class AnalyticsManager {
    // A supervised scope for this manager's lifetime
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // BUFFERED: UI can quickly enqueue events without waiting for network
    private val eventChannel = Channel<AnalyticsEvent>(Channel.BUFFERED)

    init {
        scope.launch {
            // Single consumer: sends events one by one, in order
            for (event in eventChannel) {
                try {
                    analyticsApi.track(event)   // network call on IO
                } catch (e: Exception) {
                    // Log the failure but keep processing
                    Log.e("Analytics", "Failed to track $event", e)
                }
            }
        }
    }

    // Called from UI thread — must be non-suspending for easy use
    fun track(event: AnalyticsEvent) {
        val result = eventChannel.trySend(event)  // non-suspending
        if (result.isFailure) {
            Log.w("Analytics", "Analytics queue full, dropping: $event")
        }
    }

    fun shutdown() {
        scope.cancel()
        eventChannel.close()
    }
}

// Usage:
analytics.track(AnalyticsEvent.ButtonClicked("buy_now"))
analytics.track(AnalyticsEvent.ScreenViewed("product_detail"))
```

**Why Channel here instead of just `launch { analyticsApi.track(event) }` for each call?**
- Launching individual coroutines per event = N simultaneous network calls = server may rate-limit or get overwhelmed
- Channel serializes the calls: events are sent one at a time, in order, from a single background coroutine
- The BUFFERED capacity decouples the UI (fast) from the network (slower)

---

## 🌉 `callbackFlow` — Bridging Callbacks to Flow

This is one of the most important real-world builders. Many Android APIs (Location, Bluetooth, Sensors, Firebase) use callbacks. `callbackFlow` wraps them in a Flow.

### The Problem it Solves

```kotlin
// Traditional callback API — hard to integrate with Flow-based architecture
locationManager.requestLocationUpdates(
    provider,
    minTime,
    minDistance,
    object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // how do we get this into our StateFlow? Callbacks are messy
        }
    }
)
```

### `callbackFlow` — The Solution

```kotlin
// Convert any callback API into a Flow
fun LocationManager.locationFlow(provider: String): Flow<Location> = callbackFlow {
    // [1] Create and register the callback
    val listener = LocationListener { location ->
        trySend(location)   // send into the channel (non-suspending, safe in callbacks)
    }
    requestLocationUpdates(provider, 1000L, 10f, listener)

    // [2] awaitClose runs when the collector cancels or the flow completes
    // This is where you MUST clean up (unregister, disconnect, cancel, etc.)
    awaitClose {
        removeUpdates(listener)   // CRITICAL: always clean up in awaitClose!
    }
}

// Usage — now you have a proper Flow
class LocationViewModel(private val locationManager: LocationManager) : ViewModel() {

    val currentLocation: StateFlow<Location?> = locationManager
        .locationFlow(LocationManager.GPS_PROVIDER)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
```

**Anatomy of `callbackFlow`:**

```kotlin
callbackFlow {
    // 'this' is a ProducerScope<T> — it has:
    //   send(T) — suspending send
    //   trySend(T) — non-suspending send (use in callbacks, returns ChannelResult)
    //   close(cause?) — signals completion

    val callback = SomeCallback { value ->
        trySend(value)   // ← use trySend in callbacks (they're not suspend contexts)
    }

    registerCallback(callback)

    awaitClose {
        // Called when:
        //   - The flow collector cancels (e.g., composable leaves screen)
        //   - The CoroutineScope is cancelled (e.g., ViewModel cleared)
        //   - channel.close() is called
        unregisterCallback(callback)
    }
    // awaitClose SUSPENDS until closed — this keeps the flow alive while registered
}
```

> [!IMPORTANT]
> **`awaitClose {}` is not optional.** Without it, `callbackFlow` completes immediately. The callback would be registered but the flow would be done, causing the collector to stop receiving events. Also, without `awaitClose`, you NEVER unregister the callback → memory leak.

### Real App: Bluetooth Device Scanning

```kotlin
fun BluetoothAdapter.scanFlow(): Flow<BluetoothDevice> = callbackFlow {
    val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            trySend(result.device)
        }
        override fun onScanFailed(errorCode: Int) {
            close(IOException("Scan failed: $errorCode"))  // close flow with error
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        bluetoothLeScanner?.startScan(callback) ?: close(IOException("Scanner unavailable"))
    }

    awaitClose {
        bluetoothLeScanner?.stopScan(callback)
    }
}
```

### Real App: Firebase Realtime Database

```kotlin
fun DatabaseReference.dataFlow(): Flow<DataSnapshot> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            trySend(snapshot)
        }
        override fun onCancelled(error: DatabaseError) {
            close(error.toException())
        }
    }
    addValueEventListener(listener)
    awaitClose { removeEventListener(listener) }
}
```

---

## 🔀 `channelFlow` — Concurrent Emissions

`channelFlow` is like `flow { }` but allows emissions from **multiple coroutines inside it** (regular `flow { }` only allows emission from one coroutine).

```kotlin
// ❌ Regular flow — you CANNOT launch inside it and emit from a different coroutine
val myFlow = flow {
    launch {             // ❌ COMPILE ERROR: Flow invariant is violated
        emit("from launch")
    }
}

// ✅ channelFlow — designed for concurrent producers
val myFlow = channelFlow {
    launch {             // ✅ OK — can launch and send from within channelFlow
        send("from launch 1")   // note: 'send' not 'emit'
    }
    launch {
        send("from launch 2")
    }
    delay(100)           // wait for both to complete
}
```

### Real App: Fetch from both cache and network (race strategy)

```kotlin
fun getProductDetails(id: String): Flow<Product> = channelFlow {
    // Launch both: first one to respond wins, then the fresher one updates
    launch {
        val cached = cacheRepo.getProduct(id)
        if (cached != null) send(cached)   // send cache immediately if available
    }
    launch {
        val fresh = networkRepo.getProduct(id)
        dao.insert(fresh)                  // update cache
        send(fresh)                        // send fresh data
    }
}
// Consumer receives cache immediately, then receives fresh data when it arrives
// This is the "offline-first" pattern
```

---

## ⚖️ Channel vs Flow — The Decision Guide

| Dimension | `Channel` | `Flow` (cold) | `callbackFlow` | `channelFlow` |
|---|---|---|---|---|
| Hot or Cold? | Hot | Cold | Cold (wraps hot source) | Cold |
| Multiple collectors? | Usually one receiver | Each gets own stream | Each gets own registration | Each gets own stream |
| Backpressure? | ✅ Buffer/suspend | ✅ Pull-based | ✅ Via buffer | ✅ Via buffer |
| Concurrent emission? | ✅ Yes | ❌ No | ✅ Yes (trySend) | ✅ Yes (send in launch) |
| Best for | Work queues, actor pattern | Data pipelines, transformations | Callback APIs | Parallel producers |

**Decision flowchart:**
```
Do you need to wrap a callback API (LocationManager, Bluetooth, Firebase)?
    └─► callbackFlow

Do you need to emit from multiple concurrent coroutines inside the flow?
    └─► channelFlow

Do you need a shared queue between two coroutines (producer/consumer)?
    └─► Channel

Is this a simple sequential stream of values or transformations?
    └─► flow { }
```

---

## 🎭 `actor {}` — Serialized State Manager (Advanced)

`actor {}` creates a coroutine with an incoming `Channel`. It's the receiver side of the Channel pattern — useful for managing shared mutable state without locks:

```kotlin
sealed class CounterAction {
    object Increment : CounterAction()
    object Decrement : CounterAction()
    data class Set(val value: Int) : CounterAction()
    data class GetValue(val response: CompletableDeferred<Int>) : CounterAction()
}

fun CoroutineScope.counterActor() = actor<CounterAction> {
    var counter = 0   // this state is ONLY touched by this single coroutine — no race conditions!

    for (action in channel) {   // process actions one at a time, in order
        when (action) {
            is CounterAction.Increment -> counter++
            is CounterAction.Decrement -> counter--
            is CounterAction.Set       -> counter = action.value
            is CounterAction.GetValue  -> action.response.complete(counter)
        }
    }
}

// Usage
val counter = scope.counterActor()
counter.send(CounterAction.Increment)
counter.send(CounterAction.Increment)
val response = CompletableDeferred<Int>()
counter.send(CounterAction.GetValue(response))
println(response.await())   // prints: 2
```

**Why actor is safe (no Mutex needed):** All mutations go through a single coroutine that processes actions sequentially. No two "threads" ever touch `counter` at the same time.

> [!NOTE]
> In modern Android, `StateFlow` with `.update {}` (which is atomic) handles most shared state needs without needing `actor`. Use `actor` when you have complex stateful protocols or need strict ordering guarantees.

---

## 🛠️ `send` vs `trySend` — When to Use Which

```kotlin
// send() — suspending, use inside coroutines
viewModelScope.launch {
    channel.send(event)   // suspends if buffer is full
}

// trySend() — non-suspending, use in callbacks, click listeners, non-coroutine contexts
button.setOnClickListener {
    val result = channel.trySend(ButtonClickEvent)
    if (result.isFailure) {
        // channel was closed or buffer was full
        Log.w("Channel", "Failed to enqueue click event")
    }
}
```

---

## ❌ Common Mistakes

### Mistake 1: Forgetting `awaitClose` in `callbackFlow`

```kotlin
// ❌ BAD — channel closes immediately, callbacks never received
fun badLocationFlow() = callbackFlow<Location> {
    val listener = LocationListener { trySend(it) }
    locationManager.requestLocationUpdates(provider, 0, 0f, listener)
    // No awaitClose! Flow completes immediately after registration.
    // Callback is NEVER unregistered → memory leak
}

// ✅ GOOD
fun goodLocationFlow() = callbackFlow<Location> {
    val listener = LocationListener { trySend(it) }
    locationManager.requestLocationUpdates(provider, 0, 0f, listener)
    awaitClose { locationManager.removeUpdates(listener) }
}
```

### Mistake 2: Using `emit()` inside `callbackFlow` instead of `trySend()`

```kotlin
// ❌ COMPILE ERROR — callbackFlow uses 'send', not 'emit'
callbackFlow<String> {
    val callback = SomeApi.Callback { value ->
        emit(value)      // ❌ can't call emit() in a callback (not a suspend context)
    }
}

// ✅ CORRECT
callbackFlow<String> {
    val callback = SomeApi.Callback { value ->
        trySend(value)   // ✅ non-suspending, safe in any context
    }
}
```

### Mistake 3: Using `Channel.UNLIMITED` without bounds

```kotlin
// ❌ DANGER — if producer is much faster than consumer, OOM is possible
val channel = Channel<LargeBitmap>(Channel.UNLIMITED)

// ✅ SAFER — bounded buffer, producer waits when full
val channel = Channel<LargeBitmap>(10)  // at most 10 bitmaps in flight
```

### Mistake 4: Not closing the Channel

```kotlin
// ❌ Consumer loop hangs forever waiting for more values
launch {
    channel.send(1)
    channel.send(2)
    // forgot channel.close()!
}
launch {
    for (value in channel) {  // loops forever, never terminates
        process(value)
    }
}

// ✅ Always close the channel when done producing
launch {
    channel.send(1)
    channel.send(2)
    channel.close()   // signals end — consumer loop terminates after receiving 2
}
```

---

## 🔗 Connections

- **Previous**: [[Coroutine 03 Sequential Parallel Async Patterns]] — async and parallel patterns
- **Next**: [[Coroutine 05 Testing Debugging Shared State]] — testing coroutines including channels and flows
- **Flow basics**: [[Android Jetpack Flow Study Notes]] — the higher-level API built on channels
- **Using callbackFlow in ViewModels**: [[State vs StateFlow]] — how repository flows become StateFlow

---

## 💬 Interview Master Q&A

**Q: What is a Channel and how does it differ from Flow?**
> A Channel is a hot coroutine-safe queue connecting a producer coroutine to a consumer coroutine. It's like a physical conveyor belt — it exists independently of whether anyone is consuming from it. Flow is cold — it doesn't produce values until someone collects it, and each collector gets its own independent stream execution. In practice, Channels are used for work queues (like an analytics event queue that serializes network calls) while Flows are used for reactive data pipelines with operators and backpressure.

**Q: What is `callbackFlow` and when do you use it?**
> `callbackFlow` bridges callback-based APIs (like Android's LocationManager, Bluetooth, or Firebase) into the Flow ecosystem. It creates a `ChannelProducerScope` where you register the callback and use `trySend()` to forward values. The critical part is `awaitClose {}`, which both keeps the flow alive while the callback is registered AND provides a hook to clean up (unregister the callback) when the collector cancels or the scope ends. Without `awaitClose`, the flow would complete immediately after registration and the callback would never be unregistered.

**Q: What is `channelFlow` and how is it different from `flow {}`?**
> Regular `flow { }` only allows sequential emission from a single coroutine — you cannot call `launch` inside it and emit from a different coroutine. `channelFlow` removes this restriction by backing the flow with a Channel, allowing multiple concurrent coroutines inside it to `send()` values. This is useful for the "fetch from cache and network simultaneously" offline-first pattern, where you launch two parallel coroutines and the consumer receives results as each one completes.

**Q: When would you use Channel instead of Flow?**
> I'd use a raw Channel when I need a producer-consumer work queue where ordering and backpressure matter but I don't need Flow operators. A classic example is an analytics event queue: the UI enqueues events quickly via `trySend()`, and a single background coroutine processes them one at a time by iterating over the channel. This prevents flooding the server with concurrent requests while decoupling UI speed from network speed. For everything else — reactive data streams, transformations, UI state — Flow is the better abstraction.
