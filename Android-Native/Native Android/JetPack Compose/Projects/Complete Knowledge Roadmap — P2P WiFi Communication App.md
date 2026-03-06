 # Complete Knowledge Roadmap — P2P WiFi Communication App

---

Here's a curated table of LeetCode problems mapped directly to what you'll need for your P2P WiFi app, grouped by category:

---

## 🗂️ DSA Problems for P2P WiFi Communication App

### 📦 Arrays & Strings (Byte buffers, message parsing)

| #   | Problem                                 | Difficulty | Why It Matters                             |
| --- | --------------------------------------- | ---------- | ------------------------------------------ |
| 1   | Two Sum                                 | Easy       | HashMap lookups — device ID/port mapping   |
| 5   | Longest Palindromic Substring           | Medium     | String manipulation for packet parsing     |
| 14  | Longest Common Prefix                   | Easy       | Protocol prefix matching                   |
| 49  | Group Anagrams                          | Medium     | Grouping messages by type/thread           |
| 128 | Longest Consecutive Sequence            | Medium     | Sequence numbers in TCP packets            |
| 238 | Product of Array Except Self            | Medium     | Buffer manipulation logic                  |
| 271 | Encode and Decode Strings               | Medium     | **Direct** — message framing/serialization |
| 424 | Longest Repeating Character Replacement | Medium     | Sliding window for stream reading          |

---

### 🔗 Linked Lists (Message queues, buffers)

|#|Problem|Difficulty|Why It Matters|
|---|---|---|---|
|206|Reverse Linked List|Easy|Queue reversal for message ordering|
|19|Remove Nth Node From End|Medium|Evicting old messages from buffer|
|146|LRU Cache|Medium|**Direct** — caching recent messages/peers|
|23|Merge K Sorted Lists|Hard|Merging multiple socket streams|
|141|Linked List Cycle|Easy|Detecting stuck/looping socket states|

---

### 📚 Stack & Queue (Event handling, async pipelines)

| #   | Problem                      | Difficulty | Why It Matters                            |
| --- | ---------------------------- | ---------- | ----------------------------------------- |
| 20  | Valid Parentheses            | Easy       | Protocol packet validation                |
| 155 | Min Stack                    | Easy       | Priority message handling                 |
| 232 | Implement Queue Using Stacks | Easy       | **Direct** — message queue implementation |
| 239 | Sliding Window Maximum       | Hard       | Bandwidth/throughput monitoring           |
| 295 | Find Median from Data Stream | Hard       | Real-time latency stats                   |
| 362 | Design Hit Counter           | Medium     | **Direct** — rate limiting messages       |

---

### 🌳 Trees & Tries (Routing, service discovery)

| #   | Problem                       | Difficulty | Why It Matters                       |
| --- | ----------------------------- | ---------- | ------------------------------------ |
| 208 | Implement Trie                | Medium     | **Direct** — NSD service name lookup |
| 211 | Design Add and Search Words   | Medium     | Device name search/filter            |
| 212 | Word Search II                | Hard       | Multi-pattern service discovery      |
| 235 | Lowest Common Ancestor of BST | Easy       | Network hierarchy traversal          |

---

### 🕸️ Graphs (Network topology, peer discovery)

|#|Problem|Difficulty|Why It Matters|
|---|---|---|---|
|200|Number of Islands|Medium|Detecting connected device clusters|
|133|Clone Graph|Medium|**Direct** — cloning peer connection graph|
|207|Course Schedule|Medium|Service dependency resolution|
|684|Redundant Connection|Medium|Detecting duplicate connections|
|743|Network Delay Time|Medium|**Direct** — signal propagation on LAN|
|1319|Number of Operations to Make Network Connected|Medium|**Direct** — network connectivity check|
|1584|Min Cost to Connect All Points|Medium|Optimal peer mesh topology|
|1489|Critical Connections in a Network|Hard|**Direct** — finding single points of failure|

---

### ⚡ Sliding Window & Two Pointers (Stream processing)

|#|Problem|Difficulty|Why It Matters|
|---|---|---|---|
|3|Longest Substring Without Repeating|Medium|Deduplication in socket stream|
|76|Minimum Window Substring|Hard|Packet reassembly from stream|
|209|Minimum Size Subarray Sum|Medium|Chunk sizing for file transfer|
|567|Permutation in String|Medium|Byte pattern detection in buffers|

---

### 🔢 Binary Search & Sorting (File chunking, ordering)

|#|Problem|Difficulty|Why It Matters|
|---|---|---|---|
|33|Search in Rotated Sorted Array|Medium|Out-of-order packet handling|
|153|Find Minimum in Rotated Sorted Array|Medium|Finding earliest unack'd packet|
|215|Kth Largest Element|Medium|Prioritizing large file chunks|
|347|Top K Frequent Elements|Medium|Most active peers ranking|

---

### 🧵 Concurrency & Design (Critical for your app)

|#|Problem|Difficulty|Why It Matters|
|---|---|---|---|
|1114|Print in Order|Easy|**Direct** — coroutine ordering|
|1115|Print FooBar Alternately|Medium|**Direct** — producer/consumer sockets|
|1116|Print Zero Even Odd|Medium|**Direct** — thread sync (send/receive)|
|1117|Building H2O|Medium|Resource pool (connection pool)|
|1188|Design Bounded Blocking Queue|Medium|**Direct** — message queue with backpressure|
|1195|Fizz Buzz Multithreaded|Medium|Multi-client socket handling|

---

### 🗃️ Dynamic Programming (Compression, optimization)

|#|Problem|Difficulty|Why It Matters|
|---|---|---|---|
|70|Climbing Stairs|Easy|Retry backoff calculation|
|322|Coin Change|Medium|Optimal chunk size selection|
|416|Partition Equal Subset Sum|Medium|Load balancing data transfer|
|300|Longest Increasing Subsequence|Medium|Ordering received packets|

---

### 🏗️ System Design & OOP Problems (Architecture)

|#|Problem|Difficulty|Why It Matters|
|---|---|---|---|
|284|Peeking Iterator|Medium|Buffered stream reader pattern|
|341|Flatten Nested List Iterator|Medium|Nested message protocol parsing|
|380|Insert Delete GetRandom O(1)|Medium|Peer registry management|
|460|LFU Cache|Hard|Advanced peer/message caching|
|588|Design In-Memory File System|Hard|**Direct** — local file transfer storage|
|642|Design Search Autocomplete System|Hard|Chat history search|

---

## 🎯 Priority Order to Solve First

Start with these **before** writing a single line of app code:

1. **Concurrency problems** (1114–1195) — your app is pure multithreading
2. **LRU Cache (146)** + **LFU Cache (460)** — message caching
3. **Encode/Decode Strings (271)** — message framing
4. **Graph problems (743, 1319, 1489)** — network topology thinking
5. **Bounded Blocking Queue (1188)** — socket backpressure

These ~50 problems will build exactly the mental models you need to architect the socket layer, handle concurrent streams, and design your data pipeline cleanly.

## 🧱 LAYER 1 — Foundation (Must Know First)

### Kotlin Language

- Coroutines & Flow (this app is 80% async work)
- StateFlow / SharedFlow
- Sealed classes, data classes
- Extension functions
- Scope functions (let, apply, run, also, with)

### Jetpack Compose

- Composition, recomposition, remember, derivedStateOf
- Side effects — LaunchedEffect, DisposableEffect, SideEffect
- ViewModel + UiState pattern
- Navigation Component (Compose Nav)
- Theming — MaterialTheme, ColorScheme, Typography, Shapes
- Custom layouts, Canvas API (for call UI waveforms)

---

## 🌐 LAYER 2 — The Core Challenge (Networking)

This is the hardest part of your app. You have two paths:

### Path A — WiFi Direct (P2P without router)

```
What it is  → Android's built-in peer-to-peer WiFi (no router needed)
API         → android.net.wifi.p2p (WifiP2pManager)
Learn       → Device discovery, connection handshake, 
              creating a Group Owner (acts as soft-AP host)
Limitation  → Only 2 devices, battery heavy, complex handshake
```

### Path B — Same WiFi Network (via Router) ✅ Recommended

```
What it is  → Both devices on same WiFi router, communicate via IP
API         → Java Sockets / NIO + NSD (Network Service Discovery)
Learn       → ServerSocket, Socket, InetAddress
              NsdManager → advertise and discover services on LAN
              WebRTC for calls (via lib)
```

**NSD (Network Service Discovery) is your device discovery solution.** It broadcasts your app's presence on the network — like mDNS/Bonjour.

---

## 📡 LAYER 3 — Communication Protocols to Learn

| Feature                | Protocol / Approach | Learn                                   |
| ---------------------- | ------------------- | --------------------------------------- |
| Text messaging         | TCP Sockets         | ServerSocket, InputStream, OutputStream |
| File / Photo transfer  | TCP with chunking   | ByteArray streaming, progress tracking  |
| Video streaming (call) | WebRTC              | ICE candidates, SDP offer/answer        |
| Device discovery       | NSD / mDNS          | NsdManager, ServiceInfo                 |
| Real-time state sync   | Custom TCP protocol | Design your own message framing         |

### Message Framing (critical concept)

TCP is a stream — you must define your own packet structure:

```
[4 bytes: message length][1 byte: message type][N bytes: payload]
```

Learn how to read/write this with DataInputStream / DataOutputStream.

---

## 📹 LAYER 4 — Video & Audio Calls

### WebRTC — The standard for this

- Library: `io.getstream:stream-webrtc-android` or `org.webrtc:google-webrtc`
- Concepts to learn:
    - PeerConnection
    - SDP (Session Description Protocol) — offer / answer model
    - ICE (Interactive Connectivity Establishment)
    - MediaStream, VideoTrack, AudioTrack
    - DataChannel (can also use this for messaging)
- Signaling — you handle this yourself over your TCP socket

### Camera & Microphone

- CameraX library — for camera preview + capture
- AudioRecord / AudioTrack — raw audio if not using WebRTC
- Permissions handling — camera, mic, storage (runtime permissions)

---

## 💾 LAYER 5 — Local Data Storage

|What|Library|Learn|
|---|---|---|
|Message history|Room Database|Entity, DAO, Database, Relations|
|User preferences|DataStore (Preferences)|replaces SharedPreferences|
|Media files|Internal/External Storage|FileProvider, ContentResolver|
|In-memory state|ViewModel + StateFlow|—|

Room is your primary DB. Learn migrations, TypeConverters, and Flow queries.

---

## 🏗️ LAYER 6 — Architecture

### Pattern: Clean Architecture + MVI

```
UI Layer       → Compose Screens + ViewModel (UiState, UiEvent)
Domain Layer   → UseCases (pure Kotlin, no Android deps)
Data Layer     → Repository → (Socket Manager + Room DAO + DataStore)
```

### Key Concepts

- Repository pattern
- Dependency Injection — **Hilt** (mandatory for this scale)
- UiState sealed class per screen
- Single source of truth

---

## 🔔 LAYER 7 — Background & Services

Your socket server must run even when app is backgrounded.

|Component|Use Case|
|---|---|
|Foreground Service|Keep TCP server alive, show notification|
|BroadcastReceiver|Detect WiFi connect/disconnect events|
|WorkManager|Schedule retry logic, cleanup tasks|
|Notification API|Incoming message / call notifications|

**ConnectivityManager** — detect network changes, get current WiFi SSID.

---

## 🎨 LAYER 8 — UI Polish

- **Coil** — image loading (chat images, avatars)
- **Lottie** — animations for empty states, calling pulse
- **Compose Animations** — AnimatedVisibility, animateContentSize, Crossfade
- **ExoPlayer / Media3** — video message playback in chat
- **Accompanist** — permissions handling in Compose (or Compose itself now handles)

---

## 🔐 LAYER 9 — Security (Interview Bonus Points)

- Encrypt messages before sending over socket — **AES-256 / TLS**
- **SSLSocket** instead of plain Socket for encrypted channel
- Key exchange — Diffie-Hellman concept (learn even if simplified)
- FileProvider — secure file sharing between apps/storage

---

## 📚 Exact Libraries List

```gradle
// Core
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android")

// UI
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.navigation:navigation-compose")
implementation("io.coil-kt:coil-compose")           // image loading
implementation("com.airbnb.android:lottie-compose") // animations
implementation("androidx.media3:media3-exoplayer")  // video playback

// Architecture
implementation("com.google.dagger:hilt-android")
implementation("androidx.room:room-runtime")
implementation("androidx.room:room-ktx")
implementation("androidx.datastore:datastore-preferences")

// Networking
// → NsdManager (built-in Android API, no library)
// → java.net.Socket (built-in, no library)
implementation("io.getstream:stream-webrtc-android") // WebRTC for calls

// Background
implementation("androidx.work:work-runtime-ktx")

// Camera
implementation("androidx.camera:camera-camera2")
implementation("androidx.camera:camera-lifecycle")
implementation("androidx.camera:camera-view")
```

---

## 🗺️ Learning Order (Week by Week Suggestion)

```
Week 1-2  → Kotlin coroutines + Flow + Compose basics
Week 3    → Room + DataStore + Hilt DI
Week 4    → Java Sockets + NsdManager (build a mini chat POC first)
Week 5    → Foreground Service + BroadcastReceiver (WiFi events)
Week 6    → File/image transfer over socket
Week 7    → WebRTC basics → audio call first, then video
Week 8    → UI polish, theming, animations
Week 9    → Clean architecture refactor
Week 10   → Testing, edge cases, interview prep
```

---

## 🎯 The Single Hardest Part

**Socket lifecycle management inside Foreground Service + Coroutines + Clean Architecture all working together.**

Get this right first as a POC before building any UI. If the socket layer is unstable, nothing else matters.

Start small → two devices, one sends "hello", other receives it. That working cleanly is your foundation for everything else.