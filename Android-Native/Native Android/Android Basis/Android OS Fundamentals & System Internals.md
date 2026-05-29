# Android OS Fundamentals & System Internals

This guide documents the low-level architecture of the Android Operating System, compiler runtimes, inter-process communication (IPC) frameworks, process lifecycles, and memory management mechanics.

---

## 1. Android OS Architecture Layers

Android is a layered operating system built on top of a customized Linux Kernel.

```text
┌─────────────────────────────────────────────────────────┐
│                    Applications                         │ (Kotlin, Java, C++)
├─────────────────────────────────────────────────────────┤
│              Application Framework (Java APIs)          │ (ActivityManager, PackageManager)
├─────────────────────────┬───────────────────────────────┤
│  Android Runtime (ART)  │       Native C/C++ Libraries  │ (libart.so, sqlite, WebKit)
├─────────────────────────┴───────────────────────────────┤
│            Hardware Abstraction Layer (HAL)             │ (Audio, Camera, Bluetooth drivers)
├─────────────────────────────────────────────────────────┤
│                      Linux Kernel                       │ (Process isolation, memory, drivers)
└─────────────────────────────────────────────────────────┘
```

### The 5 Architectural Layers
1.  **Linux Kernel**: Serves as the foundation. Provides hardware drivers, virtual memory management, thread scheduling, security, and process isolation.
2.  **Hardware Abstraction Layer (HAL)**: Exposes standard Java-accessible interfaces to specific hardware vendors (e.g. camera, audio, sensors) without exposing low-level kernel driver code.
3.  **Android Runtime (ART)**: The execution environment containing compiler tools and garbage collectors that run DEX bytecode.
4.  **Native C/C++ Libraries**: Heavyweight engine code written in native code (e.g., SQLite, WebKit, OpenGL ES, Media Framework) exposed to the framework via JNI.
5.  **Application Framework**: The set of Java/Kotlin APIs that developers use to build apps (e.g., Activity Manager, Content Providers, View hierarchy, Notification Manager).

---

## 2. Process Isolation & The Sandbox Model

### Linux UID Sandboxing
On standard Linux systems, many apps run under a single user account. Android implements a strict **Application Sandbox**:
*   At installation time, the system assigns a unique **Linux User ID (UID)** to each application (e.g. `u0_a124`).
*   Each app runs in its own Linux process, separate sandboxed file system directory (`/data/data/your.package`), and separate instance of the virtual machine (ART).
*   Apps cannot access each other's data, memory space, or resources unless permissions are explicitly declared in the manifest and granted by the user at runtime.

### The Zygote Process
On standard JVM systems, launching an app requires loading the entire runtime environment and core classes from scratch, which is slow and consumes memory. Android uses the **Zygote Process** to optimize this:
1.  During device boot, the system initializes the Zygote process. It starts a virtual machine instance, pre-loads thousands of core Android framework classes, resources, and themes into memory.
2.  When a user launches an application, the **Activity Manager Service (AMS)** requests Zygote to fork.
3.  Zygote forks its own process (`fork()` system call) to create a new app process. This new process instantly inherits all pre-loaded classes and resources in memory via **Copy-on-Write (CoW)** memory mapping, significantly speeding up app cold start times.

---

## 3. Compilation Pipeline: Dalvik vs. ART

Android applications compile down to Dalvik Executable (`.dex`) files containing bytecode.

### Dalvik VM (Legacy - Android 4.4 and below)
*   **Compilation**: Used **Just-In-Time (JIT)** compilation.
*   **Mechanic**: Every time the app was opened, bytecode was compiled into native machine code on-the-fly as the code was executed.
*   **Drawback**: Heavy CPU load, high battery usage, and slower execution speeds during launch and scrolling.

### ART Runtime (Modern - Android 5.0+)
*   **Compilation**: Uses a hybrid **Ahead-Of-Time (AOT)** and **Just-In-Time (JIT)** model.
*   **AOT Model**: During device idle and charging times, the system compiles DEX bytecode into native machine code (`.oat` files) beforehand.
*   **JIT Model**: JIT handles hot code loops during execution.
*   **Profile-Guided Optimizations**: When the app runs, JIT records profiles of which methods are executed most frequently. When the device is charging, ART compiles only those "hot" methods into native code, leaving cold code as bytecode. This reduces storage footprint while maintaining high performance.

### APK vs. AAB
*   **APK (Android Package)**: The final compiled zip file containing all resource configurations (all screen densities, CPU architectures, languages).
*   **AAB (Android App Bundle)**: An upload format containing the entire compiled code and resources. When downloaded, Google Play uses **Dynamic Delivery** to generate and sign a custom APK optimized specifically for that device's architecture, language, and screen density, reducing download sizes by up to 60%.

---

## 4. Inter-Process Communication (IPC): Binder & AIDL

Android components (Activities, Services) often interact across different processes. Because processes are isolated in memory, they must use IPC.

### Binder IPC
The **Binder** is a custom Linux kernel driver (`/dev/binder`) that handles IPC on Android.
*   It allows secure, high-performance transactional data passing between processes via memory sharing.
*   It copies data exactly **once** from the client process user space to the kernel space, then maps it directly to the target server process space.
*   **Transaction Limit**: Binder transactions have a strict 1MB buffer limit per process. Passing larger data packages (like large bitmaps) causes a `TransactionTooLargeException`.

### AIDL (Android Interface Definition Language)
AIDL is the syntax used to define programming interfaces that clients and services agree upon to communicate with each other using IPC. The compiler generates stub and proxy classes that marshal and unmarshal parameters across process boundaries.

---

## 5. System Services

Core system components run in separate system server processes. Applications communicate with them via Binder IPC:
*   **Activity Manager Service (AMS)**: Tracks process lifecycles, activity stacks, tasks, and handles component launches.
*   **Window Manager Service (WMS)**: Manages window layers, coordinates screen animations, input event dispatching, and view container layout boundaries.
*   **Package Manager Service (PMS)**: Manages installed packages, apk verification, permission enforcement, and intent resolutions.

---

## 6. Process Lifecycle & Memory Management

Android processes do not control their own lifecycles. The OS reclaims memory when resources are scarce by terminating processes using the **Low Memory Killer (LMK)**.

### Process Importance Hierarchy

| Priority Level | Process Class | Description |
| :--- | :--- | :--- |
| **1 (Highest)**| **Foreground Process** | Screen activity interacting with user, foreground service, or service binding to a foreground activity. |
| **2** | **Visible Process** | Activity visible but partially obscured (e.g., behind a dialog, split-screen mode). |
| **3** | **Service Process** | Running background services (e.g. music playing in background, active sync service). |
| **4 (Lowest)** | **Cached Process** | Minimized activities that are stopped. Saved to background stack. Eligible for immediate LMK termination. |

### Memory Reclamation & `onTrimMemory()`
Apps receive warning callbacks via `onTrimMemory(level)` to release resources before the LMK kills their process:
*   `TRIM_MEMORY_RUNNING_MODERATE` / `CRITICAL`: App is running, but device is low on memory. Release unnecessary caches.
*   `TRIM_MEMORY_BACKGROUND` / `COMPLETE`: App is now cached. Release heavy resources (like images, caches, network sockets).

---

## 7. Memory Leak Patterns & Reference Models

A memory leak occurs when an object is no longer needed in the app but is still held by a reference path originating from a static root (e.g. `GC Root`), preventing the garbage collector from reclaiming its memory.

### Common Android Memory Leak Scopes
*   **Static Context References**: Storing an Activity instance inside a static companion variable.
*   **Unregistered Observers / Listeners**: Registering a location callback, sensor listener, or BroadcastReceiver inside `onResume` but failing to unregister it in `onPause`.
*   **Inner Classes / Anonymous Runnables**: Non-static inner classes (like raw `Handler` or `AsyncTask`) hold an implicit reference to their outer class (the Activity). If a long-running thread is launched, it keeps the Activity alive in memory even after it has been destroyed.

---

### Reference Models: Strong, Weak, Soft
To prevent leaks while managing caches, choose reference wrappers carefully:

1.  **Strong Reference (Default)**: `val user = User()`
    *   The GC will **never** reclaim this object as long as a strong reference chain exists, even if the system is running out of memory.
2.  **Soft Reference**: `val softUser = SoftReference(user)`
    *   The GC keeps the object alive unless the system is actively running out of memory (reclaimed as a last resort before throwing an `OutOfMemoryError`). Useful for memory-sensitive caches.
3.  **Weak Reference**: `val weakUser = WeakReference(user)`
    *   The GC reclaims the object on the **very next** garbage collection cycle if it has no strong references left. Crucial for referencing short-lived components (like Activities) inside long-running handlers or background threads.
