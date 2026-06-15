# Chapter 9 — for(item in channel)

---

## 📖 Definition

> `for(item in channel)` is the **idiomatic way** to consume all values from a channel.  
> It is NOT a simple loop over existing contents — it **suspends and waits** for new values until the channel is both **closed** and **drained** (empty).

```kotlin
for (item in channel) {
    // process item
}
// Code here runs ONLY after: channel is closed AND buffer is empty
```

---

## 🔹 Key Bullets

- The loop does **NOT exit** just because the buffer is empty — it waits for more
- The loop exits **only when**: channel is `close()`d **AND** buffer is empty
- Under the hood, it repeatedly calls `receive()` until the channel is exhausted
- It is equivalent to: `while (!channel.isClosedForReceive) { val item = channel.receive() }`
- Works best as a consumer paired with a producer that eventually calls `close()`
- Does **not** catch exceptions — wrap in `try/catch` if `cancel()` is possible
- Can be used in the same coroutine as a standard `for` loop
- Channel must be closed for the loop to ever finish — without `close()`, it loops forever

---

## ⚠️ Critical Rule — It Waits, It Does NOT Exit Early

```kotlin
val channel = Channel<Int>()

launch {
    for (item in channel) {
        Log.d("ch", "Received: $item")
    }
    Log.d("ch", "Done")  // ← NEVER prints unless channel.close() is called
}
```

Even though the buffer is empty at start, the loop **does not exit**.  
It waits indefinitely for either a value or a `close()` signal.

---

## ✅ Exit Condition — Both Must Be True

```
channel is CLOSED   ✅
buffer is EMPTY     ✅

→ Loop exits
```

If only one is true:
```
Channel closed but buffer has [1,2] → loop continues, receives 1 then 2 then exits
Channel open but buffer empty       → loop waits for next send()
```

---

## ⚙️ Full Working Example

```kotlin
val channel = Channel<Int>()

// Consumer
viewModelScope.launch {
    for (item in channel) {
        Log.d("ch", "Received: $item")
    }
    Log.d("ch", "Done")
}

// Producer
viewModelScope.launch {
    delay(2000); channel.send(10)
    delay(2000); channel.send(20)
    channel.close()  // ← triggers loop exit after 20 is consumed
}
```

**Timeline:**
```
0 sec  → loop starts → receive() suspends (empty, open)
2 sec  → send(10) → loop receives 10 → suspends again
4 sec  → send(20) → loop receives 20 → suspends again
4 sec  → close() called
4 sec  → loop detects: closed + empty → exits ✅
4 sec  → "Done" prints
```

---

## 🔁 Manual vs for Loop

```kotlin
// Manual — must know count in advance
val a = channel.receive()
val b = channel.receive()
val c = channel.receive()

// Idiomatic — handles any count, exits automatically on close
for (item in channel) {
    process(item)
}
```

Use `for` when:
- You don't know how many items will come
- The producer calls `close()` when done
- You want clean code without manual loop management

---

## 🐛 From Practice — The Conflated Bug

```kotlin
val channel = Channel<Int>(Channel.CONFLATED)  // ← you had this
channel.send(1)
channel.send(2)
channel.close()

for (item in channel) {
    println(item)
}
// Output: 2   (not "1 2" — because 1 was replaced by 2 before loop started)
```

The loop only saw `2` because the Conflated channel had already discarded `1`.  
This is not a bug in `for` — it correctly consumed everything in the buffer.  
The lesson: the **channel type** determines what's in the buffer, not the `for` loop.

---

## 🌍 Real-Life Android Use Cases

### 1. File Download — Process All Chunks
```kotlin
val chunkChannel = Channel<ByteArray>(capacity = 10)

// Network sends all chunks then closes
viewModelScope.launch {
    networkService.streamFile(url).forEach { chunk ->
        chunkChannel.send(chunk)
    }
    chunkChannel.close()
}

// Writer processes all chunks — loop exits automatically when download is done
viewModelScope.launch {
    File(savePath).outputStream().use { stream ->
        for (chunk in chunkChannel) {
            stream.write(chunk)
        }
    }
    showToast("Download complete ✅")
}
```

### 2. Database Import — All Records
```kotlin
val recordChannel = Channel<CsvRow>(Channel.UNLIMITED)

// Parser sends all rows then closes
viewModelScope.launch {
    csvParser.parse(file).forEach { row -> recordChannel.send(row) }
    recordChannel.close()
}

// DB writer processes all rows — count them automatically
viewModelScope.launch {
    var count = 0
    for (row in recordChannel) {
        database.insert(row.toEntity())
        count++
    }
    Log.d("import", "Imported $count records")
}
```

### 3. Real-Time Log Processor (Finite Session)
```kotlin
val logChannel = Channel<String>(Channel.UNLIMITED)

// Log producer sends logs during a session
fun log(msg: String) = viewModelScope.launch { logChannel.send(msg) }

// When session ends, close
fun endSession() = logChannel.close()

// Log writer processes everything and uploads
viewModelScope.launch {
    val allLogs = buildString {
        for (line in logChannel) {
            appendLine(line)
        }
    }
    uploadLogs(allLogs)  // called only after all logs are collected
}
```

---

## ✅ Chapter Summary

| Concept | Answer |
|---|---|
| Does loop exit when buffer is empty? | No — waits for `close()` |
| When does the loop exit? | When channel is closed AND buffer is empty |
| What if `close()` is never called? | Loop runs forever (potential leak) |
| What does `for(item in channel)` call internally? | Repeated `receive()` until channel is exhausted |
| Conflated channel with `for` loop — gets all values? | No — only what's in the buffer at time of receive |

---

**Next →** [Chapter 10: Buffer Overflow Strategy](Chapter_10_Buffer_Overflow_Strategy.md)
