---

## title: Rapido Product Engineer – Android Developer Interview type: practice-exam candidate_name: date: total_marks: 100 time_allowed: 2 Hours 30 Minutes instructions: Attempt all sections in order. Set a timer per round. Do not skip ahead.

---

# RAPIDO — PRODUCT ENGINEER (ANDROID)

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
> 1. This paper has **2 Technical Rounds** based on a real Rapido Product Engineer Android interview.
> 2. Set a timer strictly per section. Do not exceed the time limit.
> 3. Rapido tests Kotlin nuances deeply — be precise with your answers.
> 4. For coding questions, write full working Kotlin code.
> 5. Self-evaluate your score at the end of each round.

---

---

## ROUND 1 — KOTLIN FUNDAMENTALS + ANDROID BASICS + COROUTINES

**Time Allowed:** 75 Minutes     **Marks:** 50

---

### SECTION A — Kotlin Fundamentals (30 Minutes)   _(25 marks)_

---

**Q1.** What is **delegation** in Kotlin? Explain `by lazy` vs `lateinit`. When would you use each? What happens if you access a `lateinit` variable before initialising it? _(5 marks)_

```
Answer:



```

```kotlin
// Show usage of both:
// by lazy example:


// lateinit example:


```

---

**Q2.** What are **higher-order functions** and **closures** in Kotlin? Write a higher-order function that takes a lambda, executes it, and returns a transformed result. _(4 marks)_

```
Answer:


```

```kotlin
// Higher-order function example:



```

---

**Q3.** Explain **sealed classes** vs **enums** in Kotlin. When would you choose a sealed class over an enum? _(4 marks)_

```
Answer:



```

```kotlin
// Sealed class example (e.g., for a network Result type):



```

---

**Q4.** Explain **null safety** in Kotlin. What is the difference between `?.`, `!!`, `?:`, and `let`? When is `!!` dangerous? _(4 marks)_

```
Answer:



```

```kotlin
// Show safe vs unsafe null handling:



```

---

**Q5.** What are **OOP concepts** in Kotlin? Explain the difference between `open`, `abstract`, `interface`, and `sealed`. Write a brief example using inheritance with an `abstract` class. _(4 marks)_

```
Answer:


```

```kotlin
// Inheritance example:



```

---

**Q6.** What are **modifiers** in Kotlin (`private`, `protected`, `internal`, `public`)? How does `internal` differ from `public`? _(2 marks)_

```
Answer:


```

---

**Q7.** What are **enums with properties** in Kotlin? Write an enum `Direction` with a property `degrees: Int` and a method `opposite()`. _(2 marks)_

```kotlin
// Enum with properties:



```

---

### SECTION B — Android Basics (20 Minutes)   _(15 marks)_

---

**Q1.** Explain the **Activity Lifecycle**. What is the difference between `onStop()` and `onPause()`? When is `onDestroy()` NOT called? _(4 marks)_

```
Answer:



```

---

**Q2.** Explain the **Fragment Lifecycle** in relation to its host Activity. At what point is the Fragment's view created and destroyed separately from the Fragment itself? _(3 marks)_

```
Answer:


```

---

**Q3.** What is an **Intent**? What is the difference between an explicit and implicit Intent? Give a real-world use case for each. _(3 marks)_

```
Answer:


```

---

**Q4.** What is a **Service** in Android? What is the difference between a `started` service and a `bound` service? When would you use a `IntentService` vs `JobIntentService`? _(5 marks)_

```
Answer:



```

---

### SECTION C — Coroutines Code Analysis (25 Minutes)   _(10 marks)_

**Task:** Read the coroutine snippet below carefully. Answer the questions that follow.

```kotlin
fun main() = runBlocking {
    val job = launch {
        repeat(5) { i ->
            println("Coroutine: step $i")
            delay(500)
        }
    }

    delay(1200)
    println("Cancelling...")
    job.cancel()
    job.join()
    println("Done")
}
```

---

**Q1.** Walk through the execution step by step. What will be printed and in what order? Include approximate timing. _(4 marks)_

```
Expected Output (with timing):
0ms  →
500ms →
1000ms →
1200ms →
       →
       →
```

---

**Q2.** What happens if you remove `job.join()`? Does the output change? _(2 marks)_

```
Answer:


```

---

**Q3.** Suggest one optimisation or improvement to this code. Rewrite the improved version below. _(4 marks)_

```
Improvement suggestion:


```

```kotlin
// Improved code:



```

---

**Round 1 Self Score:**     _____ / 50

---

---

## ROUND 2 — SOLID REFACTORING + COROUTINE CONCURRENCY + SYSTEM DESIGN + DSA

**Time Allowed:** 75 Minutes     **Marks:** 50

---

### SECTION A — SOLID Principles: Code Refactoring (20 Minutes)   _(15 marks)_

**Task:** The class below violates **multiple SOLID principles**. Identify each violation, name the principle, and refactor the code.

```kotlin
class UserManager {

    fun getUser(userId: String): User {
        val db = DatabaseConnection()
        return db.query("SELECT * FROM users WHERE id = $userId")
    }

    fun saveUser(user: User) {
        val db = DatabaseConnection()
        db.execute("INSERT INTO users VALUES (${user.id}, ${user.name})")
        sendWelcomeEmail(user.email)
        logActivity("User ${user.id} saved")
    }

    fun sendWelcomeEmail(email: String) {
        val emailService = SmtpEmailService()
        emailService.send(email, "Welcome!")
    }

    fun logActivity(message: String) {
        println("[LOG]: $message")
    }

    fun generateUserReport(userId: String): String {
        val user = getUser(userId)
        return "Report for ${user.name}: active since ${user.createdAt}"
    }
}
```

---

**Q1.** List all SOLID violations you can identify (name the principle + describe the violation). _(5 marks)_

```
Violation 1 — Principle:
Description:

Violation 2 — Principle:
Description:

Violation 3 — Principle:
Description:

Violation 4 — Principle:
Description:
```

---

**Q2.** Refactor the code to fix all violations. Write your clean version below. _(10 marks)_

```kotlin
// Refactored code:




```

---

### SECTION B — Coroutine Concurrency Deep Dive (15 Minutes)   _(10 marks)_

**Task:** Read the coroutine snippet below and answer all questions.

```kotlin
suspend fun fetchData(): String {
    return withContext(Dispatchers.IO) {
        delay(1000)
        "Result"
    }
}

fun processData(callback: (String) -> Unit) {
    CoroutineScope(Dispatchers.Main).launch {
        val result = fetchData()
        callback(result)
    }
}
```

---

**Q1.** What potential issue exists with `CoroutineScope(Dispatchers.Main).launch` in `processData`? What happens if the calling Activity is destroyed before `fetchData()` completes? _(4 marks)_

```
Answer:



```

---

**Q2.** Rewrite `processData` to fix the issue using structured concurrency (e.g., using `viewModelScope` or a properly scoped coroutine). _(3 marks)_

```kotlin
// Fixed version:



```

---

**Q3.** What is `SupervisorJob`? When would you use it over a regular `Job`? Write a brief example. _(3 marks)_

```
Answer:


```

```kotlin
// SupervisorJob example:



```

---

### SECTION C — System Design: Concurrent + Fault-Tolerant (20 Minutes)   _(15 marks)_

**Task:** Design a **ride-status update system** for the Rapido Android app.

_The app must show real-time driver location and ride status. The solution must handle concurrent updates, exceptions, and network failures gracefully._

---

**Q1.** Describe your architecture and the key components involved. _(4 marks)_

```
Architecture:



Key Components:
1.
2.
3.
```

---

**Q2.** How will you handle **concurrent location updates** (e.g., WebSocket messages arriving every 2 seconds while the UI is updating)? _(4 marks)_

```kotlin
// Code or pseudocode showing concurrent update handling:



```

---

**Q3.** How will you implement **exception handling and fault tolerance** — what happens if the WebSocket drops or throws an error? _(4 marks)_

```kotlin
// Exception handling strategy:



```

---

**Q4.** How will you ensure the system is **near-production ready** (thread safety, no memory leaks, clean teardown)? _(3 marks)_

```
Answer:


```

---

### SECTION D — DSA (20 Minutes)   _(10 marks)_

---

#### Problem 1 — Medium DSA (10 Minutes)   _(5 marks)_

**Problem:** Given an integer array `nums` and an integer `k`, return the `k` most frequent elements.

```
Input:  nums = [1,1,1,2,2,3], k = 2
Output: [1, 2]
```

**Approach:**

```
Approach:



```

**Code:**

```kotlin
fun topKFrequent(nums: IntArray, k: Int): IntArray {



}
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|

---

#### Problem 2 — Binary Search / Git Bisect (10 Minutes)   _(5 marks)_

**Problem:** You are given a sorted array of commit hashes `[c1, c2, c3 ... cn]`. At some point, a bug was introduced. Given a function `isBuggy(commitIndex: Int): Boolean` that returns `true` if the commit at that index is buggy, find the **first commit that introduced the bug** (minimum index where `isBuggy` returns `true`).

_This models `git bisect` — binary search on a commit history._

**Approach:**

```
Approach:


```

**Code:**

```kotlin
fun findFirstBuggyCommit(n: Int, isBuggy: (Int) -> Boolean): Int {



}
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|

---

**Round 2 Self Score:**     _____ / 50

---

---

## FINAL SCORECARD

|Round|Section|Max Marks|Your Score|
|---|---|---|---|
|Round 1|Kotlin Fundamentals|25||
|Round 1|Android Basics|15||
|Round 1|Coroutines Code Analysis|10||
|Round 2|SOLID Refactoring|15||
|Round 2|Coroutine Concurrency|10||
|Round 2|System Design|15||
|Round 2|DSA|10||
|**TOTAL**||**100**||

---

## POST-EXAM REFLECTION

**What went well?**

**What needs improvement?**

**Peer Feedback:**

---

## PREPARATION CHECKLIST

- [ ] Kotlin — `by lazy` vs `lateinit`, sealed classes, enums with properties
- [ ] Kotlin — higher-order functions, closures, null safety operators
- [ ] Kotlin — OOP modifiers: `open`, `abstract`, `sealed`, `internal`
- [ ] Android — Activity and Fragment lifecycle edge cases
- [ ] Android — Services: started vs bound, IntentService vs JobIntentService
- [ ] Coroutines — `runBlocking`, `launch`, `delay`, cancellation behaviour
- [ ] Coroutines — `withContext`, `SupervisorJob`, structured concurrency
- [ ] SOLID — identify violations in real code, practice refactoring
- [ ] Concurrency — thread safety, WebSocket handling, coroutine scoping
- [ ] DSA — Top K Frequent Elements (heap/bucket sort)
- [ ] DSA — Binary Search variants (first bad version, git bisect pattern)

---

_Based on real Rapido Product Engineer Android interview — Apr 2025._