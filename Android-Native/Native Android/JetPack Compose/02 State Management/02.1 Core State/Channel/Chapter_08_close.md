# Chapter 8 — close()

---

## 📖 Definition

> `close()` is a method that **signals** a channel that **no more values will be sent**.  
> It is a **graceful shutdown** — existing buffered values are **preserved** and can still be consumed.  
> After `close()`, `send()` throws an exception but `receive()` continues to work until the buffer is drained.

```kotlin
channel.close()
```

---

## 🔹 Key Bullets

- `close()` marks the channel as "finished sending"
- **Buffered values survive** — they are not discarded
- After `close()`, calling `send()` throws `ClosedSendChannelException`
- After `close()`, calling `receive()` still works **if the buffer has items**
- Once closed **and the buffer is empty**, `receive()` throws `ClosedReceiveChannelException`
- `for(item in channel)` **automatically exits** when the channel is closed and drained
- `close()` is a **one-way transition** — you cannot re-open a closed channel
- Does NOT cancel waiting receiver coroutines immediately — they can still drain the buffer
- Closing is cooperative — the channel signals "done", consumers still need to drain it

---

## 🆚 close() vs cancel()

| | `close()` | `cancel()` |
|---|---|---|
| New sends allowed? | ❌ | ❌ |
| Buffer preserved? | ✅ Yes | ❌ Destroyed |
| Existing consumers? | Drain remaining values | Cancelled immediately |
| Exception | `ClosedSendChannelException` | `CancellationException` |
| Mental model | Graceful shutdown | Emergency stop |

---

## ⚙️ Behavior After close()

### Sending after close() → Exception
```kotlin
channel.close()
channel.send(10)  // ← throws ClosedSendChannelException
```

### Receiving after close() when buffer has values → Works Fine
```kotlin
channel.send(1)
channel.send(2)
channel.close()

channel.receive()  // → 1  ✅
channel.receive()  // → 2  ✅
```

### Receiving after close() when buffer is empty → Exception
```kotlin
channel.close()
// buffer is empty
channel.receive()  // ← throws ClosedReceiveChannelException
```

---

## 🏪 The Store Closing Analogy

```
channel.close() is like a store announcing it's closing:
  ❌ No new customers may enter (no more sends)
  ✅ Existing customers may still check out (receive remaining values)
  Once everyone leaves + shelves are empty → store fully closed
```

---

## 📋 State After close()

```
State:    CLOSED
Buffer:   [existing values — still available]
send()  → ❌ ClosedSendChannelException
receive() → ✅ if buffer has values
receive() → ❌ ClosedReceiveChannelException if buffer empty
```

---

## 🔗 close() + for loop

The `for(item in channel)` loop is designed to work perfectly with `close()`:

```kotlin
val channel = Channel<Int>(Channel.UNLIMITED)

launch {
    for (item in channel) {
        Log.d("ch", "Received: $item")
    }
    Log.d("ch", "Done — channel was closed and drained")
}

launch {
    delay(2000); channel.send(10)
    delay(2000); channel.send(20)
    channel.close()  // ← triggers loop exit after 20 is consumed
}
```

**Timeline:**
```
0 sec  → loop starts, suspends (empty)
2 sec  → send(10) → loop receives 10
4 sec  → send(20) → loop receives 20
4 sec  → close() called
4 sec  → loop detects: closed + empty → exits
4 sec  → "Done" prints
```

---

## 🌍 Real-Life Android Use Cases

### 1. File Download — Signal Completion
```kotlin
val chunkChannel = Channel<ByteArray>(capacity = 10)

// Network layer sends file chunks
viewModelScope.launch {
    val chunks = networkService.downloadChunks(url)
    chunks.forEach { chunkChannel.send(it) }
    chunkChannel.close()  // ← signals "download complete"
}

// File writer processes all chunks then closes file
viewModelScope.launch {
    val outputStream = File(path).outputStream()
    for (chunk in chunkChannel) {          // loop auto-exits after close()
        outputStream.write(chunk)
    }
    outputStream.close()
    Log.d("download", "File saved ✅")
}
```

### 2. Batch Processing with Known End
```kotlin
val recordChannel = Channel<DatabaseRecord>(Channel.UNLIMITED)

// Import job sends all records then signals done
viewModelScope.launch {
    csvParser.parse(csvFile).forEach { record ->
        recordChannel.send(record)
    }
    recordChannel.close()  // ← "no more records"
}

// Processor saves each record
viewModelScope.launch {
    for (record in recordChannel) {
        database.insert(record)
    }
    Log.d("import", "All ${count} records imported ✅")
}
```

### 3. Paged API — All Pages Fetched
```kotlin
val pageChannel = Channel<List<Post>>(capacity = 5)

// Fetcher loads all pages and closes when done
viewModelScope.launch {
    var page = 1
    while (true) {
        val posts = api.getPosts(page)
        if (posts.isEmpty()) break
        pageChannel.send(posts)
        page++
    }
    pageChannel.close()  // ← all pages loaded
}

// UI collector processes pages
viewModelScope.launch {
    for (posts in pageChannel) {
        _allPosts.value = _allPosts.value + posts
    }
    _isLoading.value = false  // ← runs only after all pages processed
}
```

---

## ✅ Chapter Summary

| Concept | Answer |
|---|---|
| What does `close()` signal? | No more values will be sent |
| Buffer after `close()`? | Preserved — can still be drained |
| `send()` after `close()`? | `ClosedSendChannelException` |
| `receive()` after `close()` (buffer not empty)? | Works fine |
| `receive()` after `close()` (buffer empty)? | `ClosedReceiveChannelException` |
| `for(item in channel)` after `close()`? | Exits automatically once drained |

---

**Next →** [Chapter 9: for(item in channel)](Chapter_09_for_loop.md)
