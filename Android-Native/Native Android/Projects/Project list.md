# 🤖 Android Project Master Roadmap

> **37 Real Projects — Build Order — Everything You Need To Learn** Built with motivation-driven learning. Start when you're ready. Tick only when done.

---

## 🗂️ Tags

`#android` `#projects` `#roadmap` `#kotlin` `#learning`

---

## 📊 Priority Legend

|Symbol|Meaning|
|---|---|
|🟢|Easy — Build first, learn fundamentals|
|🟡|Medium — Core Android skills|
|🟠|Medium-Hard — Levelling up|
|🔴|Hard — Senior level|
|★|Your main project|
|🌍|Humanitarian — meaningful real-world impact|

---

## ⚡ Quick Index

|ID|Project|Difficulty|Tag|
|---|---|---|---|
|[[#P01 — 📡 P2P WiFi Chat App\|P01]]|📡 P2P WiFi Chat App|Medium-Hard|★ MAIN|
|[[#P02 — 🔩 Metal Detector\|P02]]|🔩 Metal Detector|Easy||
|[[#P03 — 📡 Morse Code Communicator\|P03]]|📡 Morse Code Communicator|Easy||
|[[#P04 — 😮‍💨 Breathing Trainer\|P04]]|😮‍💨 Breathing Trainer|Easy||
|[[#P05 — ⚡ Reaction Time Tester\|P05]]|⚡ Reaction Time Tester|Easy||
|[[#P06 — 📊 Internet Speed History\|P06]]|📊 Internet Speed History|Easy-Medium||
|[[#P07 — 🏥 Offline First Aid Guide\|P07]]|🏥 Offline First Aid Guide|Easy-Medium|🌍|
|[[#P08 — 🔊 Oscilloscope + Sound Level Meter\|P08]]|🔊 Oscilloscope + FFT|Medium||
|[[#P09 — 🌍 Earthquake Detector\|P09]]|🌍 Earthquake Detector|Medium||
|[[#P10 — 🔐 Steganography App\|P10]]|🔐 Steganography App|Medium||
|[[#P11 — 🎙️ Voice Changer\|P11]]|🎙️ Voice Changer|Medium||
|[[#P12 — 🌱 Plant Disease Scanner\|P12]]|🌱 Plant Disease Scanner|Medium||
|[[#P13 — 💊 Medication Interaction Checker\|P13]]|💊 Medication Checker|Medium||
|[[#P14 — 👁️ Vision Test App\|P14]]|👁️ Vision Test App|Medium||
|[[#P15 — 📈 Options Greeks Calculator\|P15]]|📈 Options Greeks Calculator|Medium||
|[[#P16 — 🧾 Invoice Receipt Scanner\|P16]]|🧾 Invoice / Receipt Scanner|Medium||
|[[#P17 — 🌐 LAN Network Scanner\|P17]]|🌐 LAN Network Scanner|Medium||
|[[#P18 — 📶 WiFi Channel Analyzer\|P18]]|📶 WiFi Channel Analyzer|Medium||
|[[#P19 — 🕵️ Canary Token Monitor\|P19]]|🕵️ Canary Token Monitor|Medium||
|[[#P20 — 🔍 Binary File Analyzer\|P20]]|🔍 Binary File Analyzer|Medium||
|[[#P21 — 🛣️ Road Surface Quality Mapper\|P21]]|🛣️ Road Surface Mapper|Medium||
|[[#P22 — 🗺️ War Driving WiFi Map Builder\|P22]]|🗺️ War Driving — WiFi Map|Medium||
|[[#P23 — 📅 Forensic Device Timeline\|P23]]|📅 Forensic Device Timeline|Medium||
|[[#P24 — 🚗 OBD-II Car Diagnostic Scanner\|P24]]|🚗 OBD-II Scanner|Medium||
|[[#P25 — 🗺️ Procedural Dungeon Game\|P25]]|🗺️ Procedural Dungeon Game|Medium-Hard||
|[[#P26 — 💓 Breathing Trainer + HRV Monitor\|P26]]|💓 Breathing + HRV Monitor|Medium-Hard||
|[[#P27 — 🧍 Posture Analyzer\|P27]]|🧍 Posture Analyzer|Medium-Hard||
|[[#P28 — 🤟 Sign Language Decoder\|P28]]|🤟 Sign Language Decoder|Medium-Hard||
|[[#P29 — ₿ Crypto Portfolio Tracker\|P29]]|₿ Crypto Portfolio Tracker|Medium-Hard||
|[[#P30 — ⏳ Falling Sand Physics Simulator\|P30]]|⏳ Falling Sand Simulator|Medium-Hard||
|[[#P31 — 📍 Family Safe Check-in\|P31]]|📍 Family Safe Check-in|Medium-Hard|🌍|
|[[#P32 — 📶 Offline Bluetooth Mesh Chat\|P32]]|📶 Offline Bluetooth Mesh Chat|Hard||
|[[#P33 — ⭐ Star Map Planetarium\|P33]]|⭐ Star Map / Planetarium|Hard||
|[[#P34 — 📻 Ham Radio Contact Logger\|P34]]|📻 Ham Radio Logger|Hard||
|[[#P35 — 🚗 Dashcam with Crash Detection\|P35]]|🚗 Dashcam + Crash Detection|Hard||
|[[#P36 — 🗺️ Safe Route Navigator\|P36]]|🗺️ Safe Route Navigator|Hard|🌍|
|[[#P37 — 🚨 Community Danger Alert System\|P37]]|🚨 Community Danger Alert|Hard|🌍|

---

---

# 🟢 EASY — Build These First

---

## P01 — 📡 P2P WiFi Chat App

> ★ **YOUR MAIN PROJECT** | Difficulty: `Medium-Hard` | Grows with you across all phases

**What it does:** Two or more phones communicate directly over WiFi — no internet, no server, no router dependency. Send text, files, and images between devices on the same local network.

**Progress**

- [ ] Socket POC — two devices, one sends "hello", other receives
- [ ] NSD device discovery working
- [ ] Foreground Service keeps server alive in background
- [ ] Text messaging working end-to-end
- [ ] File transfer working
- [ ] Compose UI complete
- [ ] Room message history
- [ ] Multi-user support
- [ ] Encryption layer

**What you learn:**

- `java.net.ServerSocket` / `Socket` / `InputStream` / `OutputStream`
- `NsdManager` — advertise and discover services on LAN (mDNS/Bonjour)
- Foreground Service — keep TCP server alive when app is backgrounded
- Coroutines + Flow — handle multiple concurrent socket connections
- Clean Architecture — socket layer fully separated from UI
- Hilt DI — inject socket manager, repository, use cases
- Room DB — store message history locally
- StateFlow + MVI — real-time UI updates from socket events
- WorkManager — retry failed message delivery
- BroadcastReceiver — detect WiFi connect/disconnect events

**Tech & Libraries:**

```
java.net.Socket / ServerSocket
android.net.nsd.NsdManager
Foreground Service + Notification
Kotlin Coroutines + Channels
Room Database
Hilt
Jetpack Compose
DataStore (settings)
ConnectivityManager + NetworkCallback
```

---

## P02 — 🔩 Metal Detector

> Difficulty: `Easy` | Category: 🟢 Easy

**What it does:** Uses phone magnetometer sensor to detect nearby metal objects. Beep frequency increases as magnetic field strength increases. Surprisingly accurate on most Android phones.

**Progress**

- [ ] SensorManager setup, reading magnetometer
- [ ] Baseline calibration working
- [ ] AudioTrack beep tone generation
- [ ] Frequency changes with field strength
- [ ] Compose UI with signal strength display
- [ ] App complete and polished

**What you learn:**

- `SensorManager` — register and unregister sensor listeners
- `TYPE_MAGNETIC_FIELD` — reading X, Y, Z axis values
- Sensor calibration — establish baseline, detect deviation
- `AudioTrack` — generate raw audio tones programmatically
- Frequency modulation — change beep pitch based on sensor value
- Compose State — react to sensor value changes in UI

**Tech & Libraries:**

```
SensorManager + SensorEventListener
TYPE_MAGNETIC_FIELD
AudioTrack (raw audio generation)
Kotlin Coroutines (sensor flow)
Jetpack Compose
```

---

## P03 — 📡 Morse Code Communicator

> Difficulty: `Easy` | Category: 🟢 Easy

**What it does:** Encode text to Morse code and flash it via the phone flashlight. Decode incoming Morse from mic or camera. Two phones can communicate Morse via flashlight + camera optical detection.

**Progress**

- [ ] Encode text → Morse code algorithm
- [ ] Decode Morse → text algorithm
- [ ] Flashlight flashing via CameraManager
- [ ] Audio tone output via AudioTrack
- [ ] Timing precision — dot vs dash detection
- [ ] Mic input decoding working
- [ ] App complete and polished

**What you learn:**

- `CameraManager` — control flashlight torch independent of camera
- `AudioRecord` — capture mic input for audio morse decoding
- State machine — dot vs dash detection by precise timing
- `AudioTrack` — generate tone for audio morse output
- Timing precision — Handler vs Coroutine delay for pulse control
- Encode/decode algorithm — text to morse and back

**Tech & Libraries:**

```
CameraManager (flashlight)
AudioRecord
AudioTrack
CameraX (optional light detection)
Kotlin Coroutines
Jetpack Compose
```

---

## P04 — 😮‍💨 Breathing Trainer

> Difficulty: `Easy` | Category: 🟢 Easy

**What it does:** Guided breathing exercises — box breathing (4-4-4-4), 4-7-8, Wim Hof method. Visual expanding/contracting circle pacer with smooth animation. Session tracking over time.

**Progress**

- [ ] Animated breathing circle (expand/contract)
- [ ] Box breathing pattern working
- [ ] 4-7-8 pattern working
- [ ] Wim Hof pattern working
- [ ] Session timer and completion screen
- [ ] Room DB — session history
- [ ] WorkManager — daily reminder notification
- [ ] App complete and polished

**What you learn:**

- Compose Animation — `animateFloatAsState`, `infiniteTransition`
- `AnimationSpec` — SpringSpec, TweenSpec for smooth organic feel
- Precise timing — coroutine delay for breath phase control
- Room DB — store session history, streak tracking
- DataStore — save preferred breathing patterns
- Notification — WorkManager scheduled reminders
- Canvas — draw animated circle with gradient

**Tech & Libraries:**

```
Jetpack Compose Animation
Canvas API
Room Database
WorkManager (reminders)
DataStore
```

---

## P05 — ⚡ Reaction Time Tester

> Difficulty: `Easy` | Category: 🟢 Easy

**What it does:** Measures true human reaction time in milliseconds with random delay to prevent anticipation. Statistical analysis over many trials — detect fatigue, time of day patterns, improvement over weeks.

**Progress**

- [ ] Random delay + stimulus display
- [ ] Millisecond-precision tap detection
- [ ] Single trial result screen
- [ ] Trial history in Room DB
- [ ] Statistics screen — mean, median, std dev
- [ ] Chart — reaction time over time
- [ ] App complete and polished

**What you learn:**

- `System.nanoTime()` — millisecond precision timing
- Random delay generation — prevent anticipation cheating
- Statistical analysis — mean, median, standard deviation in Kotlin
- Room DB — persist all trial results
- Canvas — visualize stats over time as line chart
- ViewModel — manage test state machine (waiting → ready → tap → result)

**Tech & Libraries:**

```
Jetpack Compose
Room Database
ViewModel + StateFlow
Canvas (charts)
Kotlin math/statistics
```

---

## P06 — 📊 Internet Speed History

> Difficulty: `Easy-Medium` | Category: 🟢 Easy

**What it does:** Scheduled background speed tests log your ISP's actual performance. Build a history chart showing speed by time of day. Detect throttling patterns automatically. No similar useful app exists.

**Progress**

- [ ] Manual speed test working (download measurement)
- [ ] WorkManager periodic test (every 2 hours)
- [ ] Room DB storing all results with timestamps
- [ ] WiFi-only constraint on WorkManager
- [ ] Line chart — speed over time in Compose
- [ ] Threshold alert notification
- [ ] App complete and polished

**What you learn:**

- WorkManager `PeriodicWorkRequest` — schedule background tasks
- OkHttp — download test file, measure throughput in bytes/sec
- Room DB — store all results with timestamp
- `ConnectivityManager` — run only on WiFi, not mobile data
- Compose Charts — render speed history as line graph
- Notifications — alert when speed drops below threshold

**Tech & Libraries:**

```
WorkManager
OkHttp
Room Database
ConnectivityManager
Jetpack Compose
DataStore (settings)
```

---

## P07 — 🏥 Offline First Aid Guide

> 🌍 **HUMANITARIAN** | Difficulty: `Easy-Medium` | Category: 🟢 Easy _Build this early — it can help real people right now_

**What it does:** Completely offline trauma first aid guide covering gunshot wounds, blast injuries, tourniquet application, burns, shock management. Step-by-step with illustrations. Multiple languages. Zero internet required. Can be shared as APK directly without Play Store.

**Progress**

- [ ] Content JSON loaded from assets
- [ ] Category screen — all injury types
- [ ] Step-by-step procedure screen
- [ ] TextToSpeech — reads steps aloud
- [ ] Search/filter working
- [ ] Arabic + Farsi + English language support
- [ ] Bookmarks saved in Room
- [ ] App complete and shareable as APK

**What you learn:**

- Assets folder — embed all content as JSON at build time, zero network
- Multilingual support — `strings.xml` for EN, AR, FA, HE locales
- `TextToSpeech` API — read steps aloud for injured/panicking users
- Compose Navigation — drill-down from category to procedure steps
- Search — filter procedures by keyword in Room
- Offline-first by design — no network calls anywhere in the app

**Tech & Libraries:**

```
Jetpack Compose
TextToSpeech API
Room (bookmarks + search)
DataStore (language preference)
Navigation Component
JSON assets (zero network)
```

---

---

# 🟡 MEDIUM — Core Android Skills

---

## P08 — 🔊 Oscilloscope + Sound Level Meter

> Difficulty: `Medium` | Category: 🟡 Medium

**What it does:** Real-time waveform visualizer from mic input. FFT frequency spectrum analyzer. Professional dB meter with peak hold. Shows exactly what sound looks like — useful for musicians, engineers, acoustic testing.

**Progress**

- [ ] AudioRecord capturing raw PCM samples
- [ ] Waveform drawing on Canvas at 60fps
- [ ] dB calculation working
- [ ] FFT integrated (JTransforms)
- [ ] Frequency spectrum bar display
- [ ] Peak hold indicator
- [ ] App complete and polished

**What you learn:**

- `AudioRecord` API — capture raw PCM audio samples from mic
- `ByteArray` → `FloatArray` — convert raw bytes to audio samples
- FFT algorithm — JTransforms library (free, open source)
- Canvas — draw waveform and frequency bars at 60fps
- Coroutines — continuous background audio capture loop
- dB calculation — `20 * log10(amplitude)` formula
- A-weighting filter — human ear frequency response curve

**Tech & Libraries:**

```
AudioRecord
JTransforms (FFT library — free)
Canvas API
Kotlin Coroutines + Flow
Jetpack Compose
```

---

## P09 — 🌍 Earthquake Detector

> Difficulty: `Medium` | Category: 🟡 Medium

**What it does:** Monitors accelerometer for seismic anomalies. Logs detected events with GPS location and timestamp. Shows event history on offline map. Ignores phone being picked up or walked with — only detects actual ground shaking.

**Progress**

- [ ] Accelerometer reading in foreground service
- [ ] High-pass filter to remove gravity
- [ ] Threshold + duration detection algorithm
- [ ] False positive filtering (walking vs shake)
- [ ] GPS location tagging on event
- [ ] Room DB — event history
- [ ] OSMDroid — events on map
- [ ] App complete and polished

**What you learn:**

- `TYPE_ACCELEROMETER` — raw X, Y, Z sensor readings
- High-pass filter — separate gravity from actual motion
- Threshold + duration detection — ignore walking, detect shaking
- `FusedLocationProviderClient` — GPS location at moment of event
- Foreground Service — keep sensor monitoring alive in background
- Room DB — persist detected events with coordinates
- OSMDroid — plot events on offline map

**Tech & Libraries:**

```
SensorManager
FusedLocationProviderClient
Foreground Service
Room Database
OSMDroid (offline maps — free)
Kotlin Coroutines
```

---

## P10 — 🔐 Steganography App

> Difficulty: `Medium` | Category: 🟡 Medium

**What it does:** Hide secret messages inside ordinary images using LSB (Least Significant Bit) encoding. The image looks completely normal to the eye. Recipient extracts the hidden message. Optional AES-256 encryption layer.

**Progress**

- [ ] Load image from gallery / camera
- [ ] LSB encode — hide text in pixel values
- [ ] LSB decode — extract hidden text from image
- [ ] AES-256 encryption on hidden message
- [ ] Save and share encoded image via FileProvider
- [ ] App complete and polished

**What you learn:**

- Bitmap pixel manipulation — `getPixels()`, `setPixels()`
- LSB steganography algorithm — encode bits into pixel color values invisibly
- AES-256 encryption — `javax.crypto` built into Android
- File I/O — read and write images from storage
- `FileProvider` — share encoded images with other apps securely
- Photo Picker / CameraX — source image selection

**Tech & Libraries:**

```
Android Bitmap API
javax.crypto (AES-256)
FileProvider
Photo Picker API
Jetpack Compose
```

---

## P11 — 🎙️ Voice Changer

> Difficulty: `Medium` | Category: 🟡 Medium

**What it does:** Real-time voice effects — robot, chipmunk, deep voice, echo, reverb. Mic input → DSP processing → speaker output. Latency must stay under 20ms for natural feel. Record and share altered clips.

**Progress**

- [ ] AudioRecord → AudioTrack passthrough (no effect yet)
- [ ] Latency optimized (minimize buffer size)
- [ ] TarsosDSP pitch shift effect working
- [ ] Robot / chipmunk / deep presets
- [ ] Reverb / echo effects
- [ ] Record processed audio to file
- [ ] App complete and polished

**What you learn:**

- `AudioRecord` — capture mic in real-time with minimal buffer
- `AudioTrack` — output processed audio with minimal latency
- TarsosDSP library — pitch shifting, reverb, time stretch
- Audio pipeline — producer/consumer pattern with Kotlin Channels
- Coroutines — buffer passing between capture and playback threads
- `MediaRecorder` — record the processed output to shareable file

**Tech & Libraries:**

```
AudioRecord + AudioTrack
TarsosDSP (DSP library — free)
Kotlin Coroutines + Channels
MediaRecorder
Jetpack Compose
```

---

## P12 — 🌱 Plant Disease Scanner

> Difficulty: `Medium` | Category: 🟡 Medium

**What it does:** Point camera at plant leaf → identifies disease and suggests treatment. PlantVillage TFLite model runs entirely on device. Works offline in fields with no internet or signal.

**Progress**

- [ ] CameraX preview working
- [ ] TFLite model loaded and running
- [ ] Image preprocessing (resize + normalize)
- [ ] ImageAnalysis pipeline feeding model
- [ ] Result display with confidence score
- [ ] Disease info + treatment suggestions
- [ ] Offline mode confirmed (airplane test)
- [ ] App complete and polished

**What you learn:**

- CameraX `ImageAnalysis` — per-frame image analysis use case
- TFLite Interpreter — load and run `.tflite` model on device
- PlantVillage model — open source disease classification model
- Image preprocessing — resize, normalize pixel values for model input
- GPU Delegate — hardware-accelerate model inference
- Confidence threshold — only show result above threshold %

**Tech & Libraries:**

```
CameraX
TensorFlow Lite
PlantVillage model (free download)
GPU Delegate
Jetpack Compose
ViewModel + StateFlow
```

---

## P13 — 💊 Medication Interaction Checker

> Difficulty: `Medium` | Category: 🟡 Medium

**What it does:** Add your medications — app checks all combinations for dangerous interactions using OpenFDA API (free, no account needed). Shows severity, mechanism, what to watch for. Barcode scan pill bottles for auto-identification.

**Progress**

- [ ] OpenFDA API integration (Retrofit)
- [ ] Drug search and add to list
- [ ] Interaction check between all drug pairs
- [ ] Local Room cache for offline use
- [ ] Severity display (major / moderate / minor)
- [ ] Barcode scanner for pill bottle auto-ID
- [ ] Disclaimer screen added
- [ ] App complete and polished

**What you learn:**

- OpenFDA API — free REST API, no key needed
- Retrofit + suspend functions — API calls in coroutines
- Room DB — cache drug data locally for offline use
- Graph traversal — check interactions between N drugs (not just pairs)
- ML Kit Barcode Scanning — scan pill bottle barcode
- Sealed class — Loading / Success / Error state handling

**Tech & Libraries:**

```
Retrofit + OkHttp
OpenFDA API (free, no account)
Room Database
ML Kit Barcode Scanner (free)
Jetpack Compose
Hilt
```

---

## P14 — 👁️ Vision Test App

> Difficulty: `Medium` | Category: 🟡 Medium

**What it does:** Clinical-style eye tests on your phone: Snellen chart (letter reading at calibrated distance), contrast sensitivity, color blindness (Ishihara plates), astigmatism grid. Track vision changes over time.

**Progress**

- [ ] DisplayMetrics — render letters at physical mm size
- [ ] Snellen chart test working
- [ ] Color blindness Ishihara plates
- [ ] Contrast sensitivity test
- [ ] Astigmatism grid test
- [ ] Room DB — track results over time
- [ ] App complete and polished

**What you learn:**

- `DisplayMetrics` — get exact DPI to render physical sizes correctly
- Screen calibration — render letters at clinical-standard sizes in mm
- Ishihara plate generation — procedural color dot patterns in Canvas
- Room DB — track results across multiple test sessions over time
- Precise Compose layouts — pixel-perfect rendering for medical accuracy

**Tech & Libraries:**

```
DisplayMetrics
Jetpack Compose (Canvas)
Room Database
ViewModel
DataStore
```

---

## P15 — 📈 Options Greeks Calculator

> Difficulty: `Medium` | Category: 🟡 Medium

**What it does:** Calculate Delta, Gamma, Theta, Vega, Rho using Black-Scholes formula. Interactive P&L curve charts. Strategy builder for multi-leg option trades. Live prices from Yahoo Finance (free, no account).

**Progress**

- [ ] Black-Scholes formula implemented in Kotlin
- [ ] All 5 Greeks calculating correctly
- [ ] P&L curve drawn on Canvas
- [ ] Interactive touch — drag to see values
- [ ] Yahoo Finance API integration
- [ ] Multi-leg strategy builder
- [ ] Room DB — saved strategies
- [ ] App complete and polished

**What you learn:**

- Black-Scholes formula — implement all 5 Greeks in pure Kotlin math
- Normal distribution CDF — required math for Black-Scholes
- Yahoo Finance API — free, no account, real-time price data
- Compose Canvas — draw interactive P&L curve chart
- Touch interaction on Canvas — drag to see value at any price point
- Room DB — save option strategies and watchlist

**Tech & Libraries:**

```
Kotlin math (Black-Scholes implementation)
Yahoo Finance API (free)
Canvas API
Retrofit
Room Database
Jetpack Compose
```

---

## P16 — 🧾 Invoice Receipt Scanner

> Difficulty: `Medium` | Category: 🟡 Medium

**What it does:** Photograph invoice or receipt → auto-extracts vendor, date, line items, totals. Saves to Room DB. Exports CSV. Runs entirely on device with ML Kit OCR — no backend, no cost.

**Progress**

- [ ] CameraX document capture
- [ ] ML Kit OCR extracting raw text
- [ ] Regex parsing — date, total, vendor extraction
- [ ] Structured data displayed in UI
- [ ] Room DB — invoice history
- [ ] CSV export via FileProvider
- [ ] App complete and polished

**What you learn:**

- CameraX `ImageCapture` — capture high-res document photo
- ML Kit Text Recognition — on-device OCR, free, no account needed
- Regex parsing — extract structured data from raw OCR output
- ML Kit Document Scanner — perspective correction built in
- Room DB — store parsed invoices with line items as relations
- CSV / PDF export — write files to storage via FileProvider

**Tech & Libraries:**

```
CameraX
ML Kit Text Recognition (free, on-device)
Regex (Kotlin)
Room Database
FileProvider
Jetpack Compose
```

---

## P17 — 🌐 LAN Network Scanner

> Difficulty: `Medium` | Category: 🟡 Medium

**What it does:** Scan your local WiFi network for all connected devices. Shows IP address, MAC address, hostname, device manufacturer. Ping devices and check open ports. Like Fing — but built by you.

**Progress**

- [ ] Get gateway IP and subnet from WifiManager
- [ ] Parallel ping all 254 IPs with coroutines
- [ ] MAC address resolution working
- [ ] OUI database lookup — manufacturer from MAC
- [ ] Port scanner for common ports
- [ ] Room DB — save known devices with custom names
- [ ] App complete and polished

**What you learn:**

- `WifiManager` — get current network gateway and subnet mask
- `InetAddress.isReachable()` — ping each IP in subnet range
- Coroutines `async` parallel — scan all 254 IPs simultaneously
- OUI database — map MAC prefix to manufacturer name (offline CSV)
- Port scanning — try TCP connect to common ports (22, 80, 443 etc.)
- Room DB — remember devices, let user name them

**Tech & Libraries:**

```
WifiManager
InetAddress / Socket
Kotlin Coroutines (async parallel)
OUI database (offline CSV file)
Room Database
Jetpack Compose
```

---

## P18 — 📶 WiFi Channel Analyzer

> Difficulty: `Medium` | Category: 🟡 Medium

**What it does:** Shows all nearby WiFi networks, signal strength, frequency band, channel occupation. Detect channel congestion. Recommend least crowded channel for your router setup.

**Progress**

- [ ] WifiManager scan results working
- [ ] Channel overlap visualization on Canvas
- [ ] 2.4GHz and 5GHz band separation
- [ ] Signal strength history chart
- [ ] Best channel recommendation logic
- [ ] App complete and polished

**What you learn:**

- `WifiManager.startScan()` and `getScanResults()`
- `ScanResult` fields — SSID, BSSID, frequency, RSSI level
- Channel mapping — frequency Hz → 2.4GHz or 5GHz channel number
- Canvas — draw channel overlap visualization like a waterfall chart
- Live chart — signal strength over time per network
- WiFi scan throttle — developer options workaround instruction

**Tech & Libraries:**

```
WifiManager
Canvas API
Kotlin Coroutines + Flow
Jetpack Compose
Room (scan history)
```

---

## P19 — 🕵️ Canary Token Monitor

> Difficulty: `Medium` | Category: 🟡 Medium

**What it does:** Create decoy files and folders on your device. FileObserver silently watches them. Any unauthorized access triggers an alert — detect malware, snooping apps, or physical device access by someone else.

**Progress**

- [ ] FileObserver setup on decoy directory
- [ ] OPEN / READ / ACCESS events detected
- [ ] Foreground Service keeping watch alive
- [ ] Notification alert on access
- [ ] Room DB — log all access events
- [ ] Decoy file generator (convincing fake files)
- [ ] App complete and polished

**What you learn:**

- `FileObserver` API — watch files/directories for access events
- Event types — `OPEN`, `READ`, `WRITE`, `ACCESS`, `DELETE`
- Foreground Service — keep file watching alive in background
- Notification — instant alert on suspicious access event
- Room DB — log all events with precise timestamp
- Decoy file generation — create convincing fake files (passwords.txt etc.)

**Tech & Libraries:**

```
FileObserver
Foreground Service
NotificationManager
Room Database
Jetpack Compose
```

---

## P20 — 🔍 Binary File Analyzer

> Difficulty: `Medium` | Category: 🟡 Medium

**What it does:** Load any file — visualize as a color-coded byte heatmap. Detect file type by magic bytes. Show entropy map where encrypted/compressed regions appear as high-entropy noise. Security and forensics tool.

**Progress**

- [ ] Storage Access Framework — open any file
- [ ] Read file as ByteArray
- [ ] Magic bytes detection — identify file type
- [ ] Byte frequency heatmap on Canvas
- [ ] Shannon entropy calculation per block
- [ ] Entropy heatmap rendering
- [ ] Strings extraction (printable ASCII runs)
- [ ] App complete and polished

**What you learn:**

- File I/O — read arbitrary files as `ByteArray`
- Magic bytes — identify file type from first bytes (PNG, PDF, ZIP, ELF etc.)
- Shannon entropy — calculate randomness per 256-byte block
- Canvas heatmap — render 256×256 byte frequency grid as color map
- Coroutines — process large files off main thread without blocking UI
- SAF — Storage Access Framework to open any file user chooses

**Tech & Libraries:**

```
File I/O (ByteArray)
Canvas API
Storage Access Framework (SAF)
Kotlin Coroutines
Jetpack Compose
```

---

## P21 — 🛣️ Road Surface Quality Mapper

> Difficulty: `Medium` | Category: 🟡 Medium

**What it does:** While driving, accelerometer detects bumps and potholes. GPS tags each event with precise location. Builds a map showing road quality across your city. Visualized as a heatmap on OSMDroid offline map.

**Progress**

- [ ] Accelerometer reading in foreground service
- [ ] High-pass filter — isolate vertical shocks
- [ ] Bump threshold detection calibrated
- [ ] GPS location tagging each event
- [ ] Room DB storing all events
- [ ] OSMDroid map with event overlay
- [ ] Heatmap rendering by severity
- [ ] App complete and polished

**What you learn:**

- `TYPE_ACCELEROMETER` — detect vertical shock events while driving
- High-pass filter — remove gravity, isolate sudden bumps
- `FusedLocationProviderClient` — GPS location at moment of bump
- Foreground Service — continuous monitoring during a drive
- OSMDroid — offline map with custom overlay layer for events
- Room DB — store all bump events with coordinates and severity
- Heatmap rendering — color-code severity on map tiles

**Tech & Libraries:**

```
SensorManager (Accelerometer)
FusedLocationProviderClient
Foreground Service
OSMDroid (offline maps — free)
Room Database
Jetpack Compose
```

---

## P22 — 🗺️ War Driving WiFi Map Builder

> Difficulty: `Medium` | Category: 🟡 Medium _Note: requires Developer Options → Disable WiFi Scan Throttling enabled once by user_

**What it does:** Walk or drive around and automatically map every WiFi network. Logs SSID, BSSID, signal strength, encryption type, GPS coordinates. Visualizes all networks as pins on an offline map. Export WiGLE-compatible CSV.

**Progress**

- [ ] WifiManager scan + BroadcastReceiver working
- [ ] GPS location tagging each scan result
- [ ] Room DB storing all network records
- [ ] OSMDroid map with network pins
- [ ] Signal strength color coding on map
- [ ] Encryption type indicator (Open / WPA2 / WPA3)
- [ ] WiGLE CSV export
- [ ] App complete and polished

**What you learn:**

- `WifiManager.startScan()` + `SCAN_RESULTS_AVAILABLE_ACTION` broadcast
- `ScanResult` — full network details per scan
- `FusedLocationProviderClient` — GPS tag each scan result
- OSMDroid — display thousands of network markers on offline map
- Room DB — large dataset storage and efficient querying
- CSV export — write WiGLE-compatible format to storage

**Tech & Libraries:**

```
WifiManager
FusedLocationProviderClient
OSMDroid
Room Database
BroadcastReceiver (scan results)
Jetpack Compose
```

---

## P23 — 📅 Forensic Device Timeline

> Difficulty: `Medium` | Category: 🟡 Medium

**What it does:** Collect all timestamped events from your own device — photos (EXIF), call logs, app usage, SMS. Build a visual timeline answering: what were you doing at any specific moment?

**Progress**

- [ ] ContentProvider query — call logs
- [ ] ContentProvider query — SMS
- [ ] MediaStore query — photos with EXIF timestamps
- [ ] UsageStatsManager — app usage events
- [ ] Room DB — consolidated timeline
- [ ] Timeline UI in Compose (LazyColumn)
- [ ] Date/time filter working
- [ ] App complete and polished

**What you learn:**

- ContentProvider — query `CallLog`, `Telephony.Sms`, `MediaStore`
- `ExifInterface` — extract photo timestamps from JPEG metadata
- `UsageStatsManager` — app usage times (requires special user permission)
- Room DB — consolidate all events from different sources into one DB
- Compose `LazyColumn` — render scrollable timeline efficiently
- Permission handling — sensitive permissions with clear rationale UI

**Tech & Libraries:**

```
ContentProvider
ExifInterface
UsageStatsManager
Room Database
Jetpack Compose
ActivityResultContracts (permissions)
```

---

## P24 — 🚗 OBD-II Car Diagnostic Scanner

> Difficulty: `Medium` | Category: 🟡 Medium ⚠️ _Requires ELM327 Bluetooth OBD dongle — ~₹500-800 on Amazon_

**What it does:** Connect to car's OBD-II port via cheap ELM327 Bluetooth dongle. Read and clear engine fault codes. Live data: RPM, speed, coolant temperature, fuel trim. Your personal car diagnostic tool.

**Progress**

- [ ] Bluetooth Classic pairing + RFCOMM socket
- [ ] ELM327 AT commands — initialization sequence
- [ ] Read DTC fault codes
- [ ] Decode DTC to human-readable description
- [ ] Live data PIDs — RPM, speed, coolant temp
- [ ] Clear fault codes working
- [ ] Live charts for sensor data
- [ ] App complete and polished

**What you learn:**

- Bluetooth Classic — `BluetoothAdapter`, `BluetoothDevice` pairing
- RFCOMM socket — communicate with Bluetooth serial device
- ELM327/OBD-II protocol — AT commands and Mode/PID codes
- Binary protocol parsing — decode OBD responses to real-world values
- Live charts — real-time sensor data visualization at 1Hz
- DTC codes — decode fault code hex strings from local database

**Tech & Libraries:**

```
BluetoothAdapter + RFCOMM Socket
OBD-II protocol (AT commands — documented)
Room (DTC code database)
Canvas / Charts
Jetpack Compose
Kotlin Coroutines
```

---

---

# 🟠 MEDIUM-HARD — Levelling Up

---

## P25 — 🗺️ Procedural Dungeon Game

> Difficulty: `Medium-Hard` | Category: 🟠 Medium-Hard

**What it does:** Generates infinite random dungeons algorithmically using BSP trees or cellular automata. Player explores rooms, fights enemies, finds items. Entire game runs on Canvas at 60fps with no game engine.

**Progress**

- [ ] BSP tree room generation algorithm
- [ ] Corridor connection between rooms
- [ ] Canvas game loop at 60fps (Choreographer)
- [ ] Player movement + collision detection
- [ ] Fog of war — only show explored areas
- [ ] Enemy AI basic movement
- [ ] Items / loot system
- [ ] Save game state in Room
- [ ] App complete and polished

**What you learn:**

- BSP tree algorithm — binary space partitioning for room generation
- Cellular automata — alternative organic cave generation method
- Game loop — Choreographer callback for consistent 60fps updates
- Collision detection — AABB bounding box checks
- Touch input — joystick overlay, swipe, tap gesture handling
- ECS (Entity Component System) — clean game architecture pattern
- Fog of war — 2D boolean array revealing explored tiles

**Tech & Libraries:**

```
Canvas API
Choreographer (game loop)
Touch input handling (MotionEvent)
Kotlin data structures (2D arrays)
Jetpack Compose (menus / UI)
Room (save game state)
```

---

## P26 — 💓 Breathing Trainer + HRV Monitor

> Difficulty: `Medium-Hard` | Category: 🟠 Medium-Hard

**What it does:** Breathing trainer plus real heart rate measurement via camera (rPPG — remote photoplethysmography). Place finger on back camera — app detects subtle color changes from blood pulse. No hardware needed at all.

**Progress**

- [ ] CameraX ImageAnalysis — raw frame extraction
- [ ] Green channel average calculation per frame
- [ ] Raw rPPG signal plotted
- [ ] Bandpass filter applied (0.5-4Hz)
- [ ] FFT → BPM calculation working
- [ ] HRV calculation from beat intervals
- [ ] Breathing pacer synced to HRV state
- [ ] Session history in Room
- [ ] App complete and polished

**What you learn:**

- CameraX `ImageAnalysis` — extract raw `ImageProxy` frame data
- rPPG algorithm — average green channel intensity over time series
- Bandpass filter — isolate 0.5-4Hz (heart rate frequency band)
- FFT on time signal — find dominant frequency → BPM
- HRV calculation — heart rate variability from R-R peak intervals
- Compose Animation — breathing pacer synced to live HRV state

**Tech & Libraries:**

```
CameraX (ImageAnalysis)
Signal processing (Kotlin)
JTransforms (FFT)
Jetpack Compose Animation
Room (session history)
```

---

## P27 — 🧍 Posture Analyzer

> Difficulty: `Medium-Hard` | Category: 🟠 Medium-Hard

**What it does:** Front camera + MediaPipe Pose detects slouching, head tilt, shoulder misalignment in real time. Vibrates when you slouch. Desk mode — phone sits watching you work. Posture score tracked over sessions.

**Progress**

- [ ] MediaPipe Pose integration with CameraX
- [ ] 33 landmarks extracted per frame
- [ ] Head forward detection (angle calculation)
- [ ] Shoulder drop detection
- [ ] Slouch detection algorithm
- [ ] Vibrator alert on bad posture
- [ ] Foreground Service — desk mode
- [ ] Session posture score in Room
- [ ] App complete and polished

**What you learn:**

- MediaPipe Pose — 33 body landmark detection running on-device
- Joint angle calculation — vector math between landmark coordinates
- Posture classification — define rules: head forward, shoulder drop etc.
- CameraX + `ImageProxy` — feed frames into MediaPipe pipeline
- Foreground Service + `Vibrator` — alert while running in background
- Room DB — posture score history per session over time

**Tech & Libraries:**

```
MediaPipe Pose (on-device — free)
CameraX
Vibrator API
Foreground Service
Room Database
Jetpack Compose
```

---

## P28 — 🤟 Sign Language Decoder

> Difficulty: `Medium-Hard` | Category: 🟠 Medium-Hard _Scope: ASL finger-spelling alphabet only — full sentences is PhD-level_

**What it does:** Camera watches hands in real time → recognizes American Sign Language alphabet letters. 21 hand landmarks from MediaPipe Hands → classify gesture → display letter. Spell words letter by letter.

**Progress**

- [ ] MediaPipe Hands integration with CameraX
- [ ] 21 landmarks extracted per frame
- [ ] Feature extraction (distances + angles between landmarks)
- [ ] TFLite classifier trained and integrated
- [ ] Letter detection with debounce
- [ ] Word assembly from sequential letters
- [ ] App complete and polished

**What you learn:**

- MediaPipe Hands — detect 21 hand landmarks per frame on-device
- Landmark feature extraction — distances and angles between key points
- Custom TFLite classifier — train simple model on landmark features
- CameraX `ImageAnalysis` — real-time frame-by-frame pipeline
- Debounce — accept letter only after holding gesture for N frames
- Word assembly — build words from sequential letter detections

**Tech & Libraries:**

```
MediaPipe Hands (on-device — free)
TFLite (custom classifier)
CameraX
Jetpack Compose
ViewModel + StateFlow
```

---

## P29 — ₿ Crypto Portfolio Tracker

> Difficulty: `Medium-Hard` | Category: 🟠 Medium-Hard

**What it does:** Add crypto holdings — live prices via WebSocket from Binance (free, no account). Real-time P&L calculation. Price alert notifications. Portfolio history chart. No backend — all runs client-side.

**Progress**

- [ ] CoinGecko REST API — initial price fetch
- [ ] Binance WebSocket — live price stream
- [ ] WebSocket reconnection with exponential backoff
- [ ] Room DB — holdings + price history
- [ ] Real-time P&L updating in StateFlow
- [ ] Portfolio chart on Canvas
- [ ] WorkManager — price alert notifications
- [ ] App complete and polished

**What you learn:**

- OkHttp WebSocket — subscribe to Binance live price streams
- WebSocket reconnection — exponential backoff on `onFailure`
- StateFlow — push price updates to Compose UI in real time
- Room DB — holdings, historical prices, alert thresholds
- WorkManager — periodic price check + push notification alerts
- Canvas charts — portfolio value over time line graph

**Tech & Libraries:**

```
OkHttp WebSocket
Binance WebSocket API (free)
CoinGecko REST API (free)
StateFlow + Coroutines
Room Database
WorkManager
Canvas (charts)
Jetpack Compose
```

---

## P30 — ⏳ Falling Sand Physics Simulator

> Difficulty: `Medium-Hard` | Category: 🟠 Medium-Hard

**What it does:** Draw sand, water, fire, lava, stone on a pixel grid. Each material follows physics rules via cellular automata. Sand falls and piles. Water flows sideways. Fire spreads and rises. Deeply satisfying to play with.

**Progress**

- [ ] 2D IntArray grid system
- [ ] Canvas rendering — IntArray as Bitmap at 60fps
- [ ] Sand material (falls, piles)
- [ ] Water material (flows left/right/down)
- [ ] Stone material (static, blocks others)
- [ ] Fire material (spreads, rises, burns out)
- [ ] Lava material (flows slowly, ignites fire)
- [ ] Touch drawing working
- [ ] App complete and polished

**What you learn:**

- Cellular automata — grid of cells, each updates based on neighbor rules
- Material rules — sand falls down, water fills sides, fire rises
- Canvas pixel rendering — draw `IntArray` as `Bitmap` efficiently at 60fps
- Choreographer game loop — consistent frame timing for simulation
- Touch input — draw selected material on touch and drag gesture
- `IntArray` grid — efficient 2D array operations for large simulation

**Tech & Libraries:**

```
Canvas API + Bitmap
Choreographer (game loop)
IntArray (2D simulation grid)
Touch input (MotionEvent)
Jetpack Compose (UI shell + material picker)
```

---

## P31 — 📍 Family Safe Check-in

> 🌍 **HUMANITARIAN** | Difficulty: `Medium-Hard` | Category: 🟠 Medium-Hard _Direct extension of your P2P WiFi Chat app_

**What it does:** Family members mark themselves SAFE / NEED HELP / UNKNOWN. Status spreads via P2P WiFi when nearby. Falls back to SMS when there is no internet. Dead man's switch — alerts family if no check-in in X hours.

**Progress**

- [ ] Family member list setup (DataStore)
- [ ] Status UI — SAFE / NEED HELP / UNKNOWN
- [ ] P2P WiFi status sync (from P01)
- [ ] SMS fallback via SmsManager
- [ ] ConnectivityManager — choose transport automatically
- [ ] WorkManager dead man's switch timer
- [ ] Notification on family status change
- [ ] Last known GPS location stored
- [ ] App complete and polished

**What you learn:**

- `SmsManager` — send/receive SMS without internet dependency
- WorkManager dead man's switch — alert if no check-in in X hours
- P2P WiFi status sync — extend your chat app for status broadcast
- Room DB — family member list, full status history
- `ConnectivityManager` — detect internet vs offline, choose best transport
- Notification — alert family on status change event

**Tech & Libraries:**

```
SmsManager
WorkManager
P2P WiFi (your P01 app as base)
Room Database
ConnectivityManager
Jetpack Compose
Hilt
```

---

---

# 🔴 HARD — Senior Level Projects

---

## P32 — 📶 Offline Bluetooth Mesh Chat

> Difficulty: `Hard` | Category: 🔴 Hard

**What it does:** Phones communicate over Bluetooth with no WiFi, no internet, no server. Messages hop between devices up to 100m per hop — range extends through multiple phones relaying messages. Fully encrypted and decentralized.

**Progress**

- [ ] Bluetooth Classic RFCOMM server socket
- [ ] Discovery and connection to nearby devices
- [ ] Be server and client simultaneously
- [ ] Message relay — forward to all connected peers
- [ ] Message deduplication — UUID seen-message cache
- [ ] Multi-hop routing working (3+ devices)
- [ ] RSA key exchange between devices
- [ ] AES message encryption end-to-end
- [ ] Foreground Service — keep alive in background
- [ ] App complete and polished

**What you learn:**

- Bluetooth Classic RFCOMM — socket-based serial communication
- `BluetoothAdapter` — discovery, pairing, connection management
- Multi-hop routing — flood routing then optimized path routing
- Message deduplication — UUID + seen-message cache to prevent loops
- Concurrent connections — be both server and client simultaneously
- End-to-end encryption — RSA key exchange + AES message encryption
- Backoff and reconnection — handle dropped Bluetooth connections gracefully

**Tech & Libraries:**

```
BluetoothAdapter
BluetoothServerSocket + BluetoothSocket (RFCOMM)
Kotlin Coroutines (multi-connection management)
javax.crypto (RSA + AES)
Room DB (message store)
Foreground Service
```

---

## P33 — ⭐ Star Map Planetarium

> Difficulty: `Hard` | Category: 🔴 Hard

**What it does:** Point phone at any part of the sky → see real-time star names, constellation overlays, planets, deep sky objects. Uses sensor fusion from gyroscope + accelerometer + magnetometer to know exact phone orientation.

**Progress**

- [ ] SensorManager TYPE_ROTATION_VECTOR working
- [ ] Rotation matrix from sensor data
- [ ] HYG star catalog loaded from CSV into Room
- [ ] Equatorial → horizontal coordinate transform
- [ ] Stars plotted on Canvas by magnitude
- [ ] Constellation line overlays
- [ ] Planet positions calculated (VSOP87 algorithm)
- [ ] App complete and polished

**What you learn:**

- Sensor fusion — combine gyroscope + accelerometer + compass
- Rotation matrix — convert sensor data to phone orientation in space
- Equatorial coordinates — RA/Dec astronomical coordinate system
- Coordinate transform — equatorial → horizontal → screen position math
- HYG star catalog — free CSV with 100,000+ stars and magnitudes
- Canvas — render stars with magnitude-based sizes and colors
- Low-pass filter — smooth noisy magnetometer/accelerometer data

**Tech & Libraries:**

```
SensorManager (TYPE_ROTATION_VECTOR)
HYG Star Catalog (offline CSV — free download)
Canvas API
Kotlin math (matrix operations)
Room DB (star catalog)
Jetpack Compose (UI overlay)
```

---

## P34 — 📻 Ham Radio Contact Logger

> Difficulty: `Hard` | Category: 🔴 Hard

**What it does:** Log amateur radio contacts — callsign, frequency, mode, signal report. Lookup callsign details via free HamDB API. Show all contact locations on offline map. Export industry-standard ADIF log format. Contest scoring mode.

**Progress**

- [ ] Contact log entry form
- [ ] Room DB — full contact database
- [ ] HamDB callsign lookup (Retrofit)
- [ ] ADIF file parser (import)
- [ ] ADIF file export
- [ ] Maidenhead grid locator calculation
- [ ] OSMDroid — contact location map
- [ ] Contest mode — dupe checking + scoring
- [ ] App complete and polished

**What you learn:**

- HamDB API — free callsign lookup, no account needed
- ADIF format parsing — read and write industry-standard log files
- Maidenhead grid locator — ham radio location system math
- OSMDroid offline maps — show contact locations globally
- Room DB — full contest logging with complex relation queries
- Retrofit + Moshi — callsign API integration
- FileProvider — export ADIF/CSV files to storage

**Tech & Libraries:**

```
Retrofit + OkHttp
HamDB API (free, no account)
ADIF parser (custom implementation)
OSMDroid (offline maps)
Room Database
Jetpack Compose
FileProvider
```

---

## P35 — 🚗 Dashcam with Crash Detection

> Difficulty: `Hard` | Category: 🔴 Hard

**What it does:** Continuous loop recording like a real dashcam. Accelerometer detects hard braking or crash → automatically saves that clip before it's overwritten. GPS coordinates overlay. Runs as foreground service in background.

**Progress**

- [ ] CameraX VideoCapture continuous recording
- [ ] Foreground Service — recording survives app close
- [ ] Loop recording — delete oldest clip at storage limit
- [ ] Accelerometer crash detection (G-force threshold)
- [ ] Auto-save clip on crash detection
- [ ] GPS overlay on saved clips
- [ ] Storage management — free space calculation
- [ ] MediaScanner — register clips in gallery
- [ ] App complete and polished

**What you learn:**

- CameraX `VideoCapture` + `Recorder` — continuous video recording
- Foreground Service — keep recording alive when app is in background
- Loop recording — manage rolling file storage, delete oldest segments
- Accelerometer crash detection — threshold + duration filter
- GPS overlay — `FusedLocationProviderClient` + video metadata
- Storage management — calculate free space, prune old clips smartly
- `MediaScannerConnection` — register saved videos to system gallery

**Tech & Libraries:**

```
CameraX (VideoCapture + Recorder)
Foreground Service
SensorManager (crash detection)
FusedLocationProviderClient
StorageManager
Jetpack Compose
```

---

## P36 — 🗺️ Safe Route Navigator

> 🌍 **HUMANITARIAN** | Difficulty: `Hard` | Category: 🔴 Hard

**What it does:** Community marks roads as safe/dangerous/blocked on an offline map. App calculates safest route avoiding danger zones. Works with zero internet. Danger markers sync phone-to-phone via P2P WiFi when devices are in range.

**Progress**

- [ ] OSMDroid offline map tiles pre-download
- [ ] Custom map markers (safe / danger / blocked)
- [ ] Tap to report location status
- [ ] GraphHopper offline routing working
- [ ] Custom routing weights — penalise dangerous segments
- [ ] P2P WiFi sync of danger markers (from P01)
- [ ] Room DB — all markers stored locally
- [ ] Turn-by-turn navigation
- [ ] Off-route detection and recalculation
- [ ] App complete and polished

**What you learn:**

- OSMDroid — offline map tiles, pre-downloaded for a region
- GraphHopper Android — offline routing engine, runs fully on device
- Custom routing weights — modify edge costs to avoid danger zones
- P2P sync — reuse your P01 WiFi chat to broadcast/receive markers
- Room DB — all markers stored locally, sync when peers connect
- GPS navigation — bearing calculation, off-route detection
- Offline tile download — let user pre-download their city/region

**Tech & Libraries:**

```
OSMDroid (offline maps — free)
GraphHopper (offline routing engine — free)
P2P WiFi sync (your P01 app)
Room Database
FusedLocationProviderClient
Jetpack Compose
Hilt
```

---

## P37 — 🚨 Community Danger Alert System

> 🌍 **HUMANITARIAN — BUILD LAST** | Difficulty: `Hard` | Category: 🔴 Hard _This is the final boss. It uses skills from every project before it._

**What it does:** Anyone nearby taps to report danger at their GPS location. Alert spreads phone-to-phone via Bluetooth mesh AND WiFi Direct — no internet, no server required. Works when cell towers and internet are deliberately cut. Shows danger zones on offline map.

**Progress**

- [ ] Bluetooth mesh relay (from P32)
- [ ] WiFi Direct — WifiP2pManager setup
- [ ] Alert message structure with GPS + type + UUID
- [ ] Message deduplication — no alert loops
- [ ] Foreground Service — receive and relay alerts while backgrounded
- [ ] OSMDroid — danger zone overlay on offline map
- [ ] Alert categories — bomb / fire / medical / blocked / safe zone
- [ ] Priority levels — severity scoring
- [ ] Room DB — local alert event log
- [ ] App complete — tested on real devices

**What you learn:**

- `WifiP2pManager` — WiFi Direct phone-to-phone without any router
- Bluetooth mesh — multi-hop alert propagation (built in P32)
- OSMDroid — offline map with danger zone overlays
- Message deduplication — UUID cache prevents relay loops
- Foreground Service — stay alive to receive and relay alerts
- Room DB — persistent local danger event log
- Alert priority system — severity classification and display

**Tech & Libraries:**

```
WifiP2pManager (WiFi Direct)
BluetoothAdapter (mesh fallback from P32)
OSMDroid (offline maps)
Room Database
Foreground Service
FusedLocationProviderClient
Jetpack Compose
Hilt
```

---

---

## 🔗 How These Projects Connect

```
P01 P2P WiFi Chat
 ├── P31 Family Safe Check-in     (add SMS fallback + status)
 ├── P36 Safe Route Navigator     (add offline routing + markers)
 └── P37 Community Danger Alert   (add mesh + WiFi Direct)

P32 Offline Mesh Chat
 └── P37 Community Danger Alert   (mesh backbone)

P02 Metal Detector
P08 Oscilloscope + FFT
P26 Breathing + HRV              (all teach audio/sensor pipelines)

P12 Plant Disease Scanner
P27 Posture Analyzer
P28 Sign Language Decoder        (all teach CameraX + ML pipeline)
```

---

## 📈 Your Current Progress

- [ ] Phase 0 Kotlin basics — partial (~12 items done)
- [ ] Phase 0.2 Coroutines — not started
- [ ] Phase 2 Android Components — not started
- [ ] Phase 3 Compose — not started
- [ ] Phase 4 Architecture — not started
- [ ] Phase 5 Hilt/Koin — not started
- [ ] Phase 6 Room + DataStore — not started
- [ ] **P01 P2P WiFi Chat** — not started
- [ ] First mini-project complete
- [ ] 5 projects complete
- [ ] 10 projects complete
- [ ] 20 projects complete
- [ ] P37 Community Danger Alert — final boss complete

---

_Build with motivation. Tick only when done in code. Not when read._