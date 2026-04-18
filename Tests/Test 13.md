# INCRED — SOFTWARE ENGINEER (MOBILE)

# MOCK INTERVIEW EXAMINATION PAPER

---

|Field|Details|
|---|---|
|Candidate Name||
|Date||
|Time Started||
|Time Ended||
|Total Score|/ 100|

---

> **INSTRUCTIONS TO CANDIDATE**
> 
> 1. This exam has **2 Rounds** matching InCred's actual Mobile interview process.
> 2. Round 1 is 2 hours — DSA + heavy Kotlin/Android. Manage time carefully.
> 3. Round 2 is a System Design/Managerial deep dive.
> 4. Self-evaluate at the end of each round.

---

---

## ROUND 1 — TECHNICAL INTERVIEW (DSA + Kotlin + Android)

**Time Allowed:** 120 Minutes      **Marks:** 55

---

### Part A — DSA Problems (30 min)

> 3 Easy/Medium problems. Pseudocode is acceptable here but working code is better.

---

**Problem 1 — Two Sum** (8 min)

```
Given an array of integers and a target, return indices of two numbers that add up to target.

Input:  nums = [2,7,11,15], target = 9
Output: [0,1]
```

```kotlin
fun twoSum(nums: IntArray, target: Int): IntArray {



}
```

|Time Complexity|O( )|Space Complexity|O( )|
|---|---|---|---|

_(5 marks)_

---

**Problem 2 — Climbing Stairs** (10 min)

```
You are climbing a staircase with n steps. Each time you can climb 1 or 2 steps.
How many distinct ways can you reach the top?

Input:  n = 5
Output: 8
```

```kotlin
fun climbStairs(n: Int): Int {



}
```

|Time Complexity|O( )|Space Complexity|O( )|
|---|---|---|---|

_(6 marks)_

---

**Problem 3 — Subarray Sum Equals K** (12 min)

```
Given an integer array and an integer k, return the total number of subarrays
whose sum equals k.

Input:  nums = [1,1,1], k = 2
Output: 2
```

**Approach:**

```
Approach (prefix sum + hashmap):


```

```kotlin
fun subarraySum(nums: IntArray, k: Int): Int {



}
```

|Time Complexity|O( )|Space Complexity|O( )|
|---|---|---|---|

_(7 marks)_

---

### Part B — Kotlin Deep Dive (45 min)

---

**Q1.** What is the difference between a regular `class`, `data class`, and `object` in Kotlin? _(3 marks)_

```
class:

data class (what does the compiler auto-generate?):

object:
```

---

**Q2.** What is the difference between `enum class` and `sealed class`? When would you choose sealed over enum? _(4 marks)_

```
enum class:

sealed class:

When to choose sealed:

```

```kotlin
// Write a sealed class for a network Result type:



```

---

**Q3.** What is the difference between `const val` and `val`? Where is each stored in memory? _(3 marks)_

```
val:

const val:

Memory location difference:
```

---

**Q4.** Explain Kotlin scope functions — `let`, `run`, `with`, `apply`, `also`. What is the key difference between them? _(5 marks)_

|Function|Receiver (`this`/`it`)|Returns|Best used for|
|---|---|---|---|
|`let`||||
|`run`||||
|`with`||||
|`apply`||||
|`also`||||

```kotlin
// Write one example using apply and one using let:



```

---

**Q5.** What are Kotlin data types and where are they stored — stack or heap? _(3 marks)_

```
Int, Long, Boolean (primitive wrappers):

String:

Custom class instances:

Nullable types (Int?):
```

---

**Q6.** Coroutines — Flows and Dispatchers _(7 marks)_

**(a)** What is the difference between `Flow`, `StateFlow`, and `SharedFlow`?

```
Flow:

StateFlow:

SharedFlow:
```

**(b)** What is the difference between `Dispatchers.IO`, `Dispatchers.Default`, and `Dispatchers.Main`?

```
IO:

Default:

Main:
```

**(c)** Write a code example using `Flow` with `flowOn` to switch dispatchers:

```kotlin
// Example:



```

---

### Part C — Android & Jetpack Compose Basics (30 min)

---

**Q1.** What is the difference between `remember` and `rememberSaveable` in Jetpack Compose? _(2 marks)_

```
Answer:


```

---

**Q2.** What triggers recomposition in Compose? How does Compose know which composables to skip? _(3 marks)_

```
Answer:



```

---

**Q3.** What is the Android Activity lifecycle? Write all callbacks in order for: _(4 marks)_

**(a)** First launch:

**(b)** Back button pressed:

**(c)** App goes to background (Home button):

**(d)** Screen rotation:

---

**Q4.** What are the basic differences between `Service`, `IntentService`, and `WorkManager`? When would you use each? _(4 marks)_

```
Service:

IntentService (deprecated but may be asked):

WorkManager:
```

---

**Round 1 Self Score:** _____ / 55 (DSA 18 + Kotlin 25 + Compose/Android 13)

---

---

## ROUND 2 — SYSTEM DESIGN / MANAGERIAL ROUND

**Time Allowed:** 60 Minutes      **Marks:** 45

> Deeper Android, Kotlin advanced topics, security, memory, Compose, KMM, and scenario-based questions.

---

### Section A — Advanced Android & Kotlin (30 min)

---

**Q1.** Flows — advanced usage _(5 marks)_

**(a)** What is the difference between `cold` and `hot` flows?

```
Answer:


```

**(b)** What is `conflate()` and when would you use it?

```
Answer:

```

**(c)** Write a debounced search using `Flow`:

```kotlin
// Debounced search (debounce 300ms, distinctUntilChanged):



```

---

**Q2.** Application Security in Android _(5 marks)_

**(a)** How do you store sensitive data (API keys, tokens) securely on Android?

```
Answer:


```

**(b)** What is certificate pinning? How do you implement it with OkHttp?

````
Answer:


```kotlin
// OkHttp certificate pinning setup:



````

---

**Q3.** Memory Leak Prevention _(5 marks)_

**(a)** List 3 common causes of memory leaks in Android apps.

```
1.
2.
3.
```

**(b)** How do you detect memory leaks? Name the tools.

```
Answer:

```

**(c)** A ViewModel holds a reference to a Fragment's View. Why is this a memory leak and how do you fix it?

```
Answer:



```

---

**Q4.** Handling Timeouts — write code on this sheet as if writing on Google Docs. _(5 marks)_

The interviewer asks: "Show me different ways to handle a network timeout in an Android app."

**(a)** OkHttp timeout configuration:

```kotlin
// OkHttp client with timeouts:



```

**(b)** Coroutine `withTimeout`:

```kotlin
// Using withTimeout:



```

**(c)** Retrofit + coroutine timeout with retry logic:

```kotlin
// Timeout + retry:



```

---

### Section B — Jetpack Compose Advanced + KMM (15 min)

---

**Q5.** Compose performance optimisations _(5 marks)_

**(a)** What is `derivedStateOf` and when should you use it?

```
Answer:

```

**(b)** What is the difference between `LazyColumn` and `RecyclerView`? When would you still prefer RecyclerView?

```
Answer:


```

**(c)** What is `key()` in Compose and why does it matter for lists?

```
Answer:

```

---

**Q6.** Kotlin Multiplatform Mobile (KMM) _(5 marks)_

**(a)** What does KMM share between Android and iOS, and what does it NOT share?

```
Shared:

Not shared (platform-specific):
```

**(b)** What is `expect` and `actual` in KMM? Write a simple example.

```kotlin
// expect declaration:



// actual implementation (Android):



```

**(c)** What are the current limitations of KMM for production apps?

```
Answer:


```

---

### Section C — Scenario-Based Questions (15 min)

---

**Q7.** Your app's RecyclerView scrolls janky (drops frames). Walk me through your entire debugging process step by step. _(5 marks)_

```
Step 1:

Step 2:

Step 3:

Step 4:

Step 5:
```

---

**Q8.** A user reports that your app crashes on low-memory devices when returning from background. How do you investigate and fix this? _(5 marks)_

```
Investigation:




Fix:


```

---

**Round 2 Self Score:** _____ / 45

---

---

## FINAL SCORECARD

|Round|Description|Max Marks|Your Score|
|---|---|---|---|
|Round 1A|DSA — 3 Problems|18||
|Round 1B|Kotlin Deep Dive|25||
|Round 1C|Android + Compose Basics|13||
|Round 2A|Advanced Android, Security, Memory, Timeouts|20||
|Round 2B|Compose Advanced + KMM|10||
|Round 2C|Scenario-Based Questions|15||
|**TOTAL**||**100**||

---

## POST-EXAM REFLECTION

**What went well?**

**What needs improvement?**

**Topics to revise:**

- [ ]
- [ ]
- [ ]

---

## PREPARATION CHECKLIST

### Kotlin

- [ ] class vs data class vs object vs companion object
- [ ] enum class vs sealed class — with network Result example
- [ ] const val vs val — memory location
- [ ] All 5 scope functions — receiver, return value, use case
- [ ] Kotlin data types and nullability — stack vs heap

### Coroutines & Flow

- [ ] Cold Flow vs Hot Flow (StateFlow, SharedFlow)
- [ ] Dispatchers — IO, Default, Main, Unconfined
- [ ] flowOn, conflate, debounce, distinctUntilChanged
- [ ] withTimeout, withTimeoutOrNull, retry operators
- [ ] Structured concurrency, SupervisorJob, CoroutineExceptionHandler

### Android

- [ ] Full Activity + Fragment lifecycle — all edge cases
- [ ] Service vs IntentService vs WorkManager
- [ ] Memory leaks — common causes + LeakCanary + fix patterns
- [ ] Security — EncryptedSharedPreferences, Keystore, certificate pinning

### Jetpack Compose

- [ ] remember vs rememberSaveable
- [ ] Recomposition triggers — what Compose skips and why
- [ ] derivedStateOf, key(), LazyColumn optimisations

### KMM

- [ ] What is shared, what is not
- [ ] expect/actual pattern
- [ ] Current production readiness and limitations

### DSA

- [ ] Prefix sum + hashmap (subarray sum = k pattern)
- [ ] 1D DP (climbing stairs, house robber, coin change)
- [ ] Two pointer and sliding window

---

_Template based on real InCred Mobile Engineer interview experience by Gaurav — Apr 2025._