# Kotlin Channels — Study Index

> Practice-based notes built from hands-on experimentation.  
> Each chapter is a separate file. Start here for navigation and quick reference.

---

## 📚 Chapters

| # | Topic | File |
|---|---|---|
| 1 | What is a Channel? | [Chapter_01_What_is_a_Channel.md](Chapter_01_What_is_a_Channel.md) |
| 2 | send() | [Chapter_02_send.md](Chapter_02_send.md) |
| 3 | receive() | [Chapter_03_receive.md](Chapter_03_receive.md) |
| 4 | Rendezvous Channel | [Chapter_04_Rendezvous_Channel.md](Chapter_04_Rendezvous_Channel.md) |
| 5 | Buffered Channel | [Chapter_05_Buffered_Channel.md](Chapter_05_Buffered_Channel.md) |
| 6 | Unlimited Channel | [Chapter_06_Unlimited_Channel.md](Chapter_06_Unlimited_Channel.md) |
| 7 | Conflated Channel | [Chapter_07_Conflated_Channel.md](Chapter_07_Conflated_Channel.md) |
| 8 | close() | [Chapter_08_close.md](Chapter_08_close.md) |
| 9 | for(item in channel) | [Chapter_09_for_loop.md](Chapter_09_for_loop.md) |
| 10 | Buffer Overflow Strategy | [Chapter_10_Buffer_Overflow_Strategy.md](Chapter_10_Buffer_Overflow_Strategy.md) |
| 11 | Multiple Producers & Consumers | [Chapter_11_Multiple_Producers_Consumers.md](Chapter_11_Multiple_Producers_Consumers.md) |
| 12 | cancel() | [Chapter_12_cancel.md](Chapter_12_cancel.md) |
| 13 & 14 | Channel States & Exception Handling | [Chapter_13_14_States_and_Exceptions.md](Chapter_13_14_States_and_Exceptions.md) |

---

## ⚡ Quick Reference

### Channel Types

| Type | Code | send() suspends? | Values lost? | Best For |
|---|---|---|---|---|
| Rendezvous | `Channel()` | Yes (no receiver) | Never | Synchronization |
| Buffered | `Channel(N)` | Yes (buffer full) | Never | Decoupling speeds |
| Unlimited | `Channel(UNLIMITED)` | Never | Never | Finite bursts |
| Conflated | `Channel(CONFLATED)` | Never | Older values | Latest value only |

---

### Operations

| Operation | Suspends when | Throws when |
|---|---|---|
| `send()` | Buffer full / no receiver (Rendezvous) | Channel closed or cancelled |
| `receive()` | Buffer empty / no sender | Channel closed+empty or cancelled |
| `for(item in channel)` | Buffer empty (and channel open) | Never — exits on close+empty |
| `close()` | — | — |
| `cancel()` | — | — |

---

### Buffer Overflow Strategies

| Strategy | send() suspends? | What drops? |
|---|---|---|
| `SUSPEND` (default) | Yes | Nothing |
| `DROP_OLDEST` | No | Oldest buffered value |
| `DROP_LATEST` | No | Incoming new value |

---

### Lifecycle States

| State | Entered via | send() | receive() | Buffer |
|---|---|---|---|---|
| OPEN | Creation | Works | Works | Intact |
| CLOSED | `close()` | ❌ Exception | ✅ until empty | Preserved |
| CANCELLED | `cancel()` | ❌ Exception | ❌ Exception | Destroyed |

---

### Exceptions

| Exception | When |
|---|---|
| `ClosedSendChannelException` | `send()` on closed/cancelled channel |
| `ClosedReceiveChannelException` | `receive()` on closed+empty channel |
| `CancellationException` | Any op on cancelled channel — **always re-throw!** |

---

## 🧠 Key Rules to Remember

1. **Channel = Pipe, not List** — it transfers data, not stores it
2. **Rendezvous = Meeting Point** — sender and receiver must be ready at the same time
3. **Suspended send() auto-resumes** — the moment a receive() frees a buffer slot
4. **FIFO order** — values come out in the order they went in
5. **Log order ≠ execution order** — concurrent coroutines have non-deterministic log interleaving
6. **close() ≠ cancel()** — close is graceful (buffer preserved), cancel is emergency (buffer destroyed)
7. **Always re-throw CancellationException** — never swallow it
8. **Channel = Work distribution** — each value goes to exactly one receiver (not broadcast)
9. **Arrival order, not launch order** — with multiple producers, whoever reaches send() first wins
10. **for(item in channel) never exits unless close() is called** — an open empty channel waits forever

---

## 🌍 Real-Life Use Case Map

| Android Scenario | Best Channel Type |
|---|---|
| Navigation events (ViewModel → UI) | Rendezvous / Buffered(1) |
| Download progress bar | Conflated |
| GPS location on map | Conflated |
| Image processing pipeline | Buffered(N) + multi-consumer |
| File chunk download | Buffered(N) |
| Upload queue (max N concurrent) | Buffered(N) + SUSPEND |
| Live stock prices | Buffered(N) + DROP_OLDEST |
| Error reporting | Buffered(N) + SUSPEND |
| Work queue with multiple workers | Unlimited + multiple consumers |
| Coroutine synchronization | Rendezvous |
| Session event collection | Unlimited |
