---

## title: Zomato SDE 1 – Android Developer Interview type: practice-exam candidate_name: date: total_marks: 100 time_allowed: 2 Hours 30 Minutes instructions: Attempt all sections in order. Set a timer per round. Do not skip ahead.

---

# ZOMATO — SDE 1 ANDROID DEVELOPER

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
> 1. This paper has **2 Technical Rounds** based on a real Zomato SDE 1 Android interview.
> 2. Set a timer strictly per section. Do not exceed the time limit.
> 3. For every concept — explain the **WHY and HOW**, not just the WHAT.
> 4. For coding questions, write full working Kotlin/Java code.
> 5. Self-evaluate your score at the end of each round.
> 6. Share with a peer after completion for feedback.

---

---

## ROUND 1 — ANDROID FUNDAMENTALS + DSA

**Time Allowed:** 75 Minutes (55 min Android + 20 min DSA)     **Marks:** 55

---

### WARM-UP (5 Minutes)   _(5 marks)_

**Q1.** Briefly introduce your previous team, the best task you worked on, and your reason for seeking a change.

```
Answer:




```

---

### PART A — Android Internals (50 Minutes)   _(40 marks)_

> **Note:** Zomato focuses heavily on INTERNALS — explain the "why" and "how" behind each concept.

---

**Q1.** Explain the complete **Fragment Lifecycle** internals.

- What is the lifecycle sequence from creation to destruction?
- How does a Fragment behave during **screen rotation**?
- What happens when the user presses the **back button** while a Fragment is on the back stack? _(8 marks)_

```
Answer:




```

```
Lifecycle Sequence (list all callbacks in order):
1.                          6.
2.                          7.
3.                          8.
4.                          9.
5.
```

**Q2.** What is `FragmentManager`? What is a `FragmentTransaction`? List 2 common pitfalls when using them. _(4 marks)_

```
Answer:



```

---

**Q3.** Explain **RecyclerView** optimisation internals.

- How does `RecycledViewPool` work during scrolling?
- What is the difference between `notifyDataSetChanged()` and `DiffUtil`?
- What layout and binding optimisations can you apply? _(6 marks)_

```
Answer:




```

```kotlin
// Write a DiffUtil.ItemCallback implementation for a list of User objects:
data class User(val id: Int, val name: String)

class UserDiffCallback : DiffUtil.ItemCallback<User>() {



}
```

---

**Q4.** Explain **ViewModel internals**.

- How does ViewModel survive a screen rotation (configuration change)?
- What is `ViewModelStore` and `ViewModelProvider`?
- What happens when the Activity is **finished** (not rotated)? _(6 marks)_

```
Answer:




```

---

**Q5.** Explain **WorkManager** internals.

- How does WorkManager decide which underlying API to use (JobScheduler / AlarmManager / Firebase JobDispatcher)?
- What is the difference between `OneTimeWorkRequest` and `PeriodicWorkRequest`?
- How do you chain multiple workers? _(6 marks)_

```
Answer:



```

```kotlin
// Write a WorkManager chain: Worker A → Worker B → Worker C



```

---

**Q6.** What is a **ContentProvider**? Explain how it works internally.

- How does the CRUD URI matching work?
- When would you use a ContentProvider vs a Room database directly? _(5 marks)_

```
Answer:



```

---

**Q7.** What is a **ForegroundService**? How does it differ from a background service?

- When is it required?
- What is the mandatory notification requirement and why? _(5 marks)_

```
Answer:



```

---

### PART B — DSA: Priority Queue (20 Minutes)   _(10 marks)_

**Problem:** Given a list of tasks where each task has a name and a priority value, implement a system that always processes the **highest priority task first**. If two tasks have the same priority, process them in the order they were added.

**Step 1 — Write your approach (3 min)**

```
Approach:



```

**Step 2 — Write working code**

```kotlin
data class Task(val name: String, val priority: Int)

fun processTasksInOrder(tasks: List<Task>): List<String> {



}

fun main() {
    val tasks = listOf(
        Task("EmailSync", 2),
        Task("PaymentCallback", 5),
        Task("LogUpload", 1),
        Task("OrderUpdate", 5),
    )
    println(processTasksInOrder(tasks))
    // Expected: [PaymentCallback, OrderUpdate, EmailSync, LogUpload]
}
```

**Step 3 — Dry run**

```
Input Tasks:  EmailSync(2), PaymentCallback(5), LogUpload(1), OrderUpdate(5)

Trace through priority queue state:


Expected Output: [PaymentCallback, OrderUpdate, EmailSync, LogUpload]
Your Output:
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|
|Code complete?|Yes / No|
|Correct output?|Yes / No|

---

**Round 1 Self Score:**     _____ / 55

---

---

## ROUND 2 — SYSTEM DESIGN + DESIGN PATTERNS + ANDROID DEEP DIVE

**Time Allowed:** 75 Minutes     **Marks:** 45

> **Note:** This round has two interviewers. Be prepared to explain every decision.

---

### SECTION A — Android Deep Dive (20 Minutes)   _(10 marks)_

---

**Q1.** Explain **Dependency Injection** with Dagger/Hilt.

- How does Hilt generate code at compile time?
- How does DI work for an **anonymous object** (edge case)? _(5 marks)_

```
Answer:



```

```kotlin
// Show how you would inject into a non-standard component (e.g., a custom View or anonymous object):



```

---

**Q2.** Explain **OkHttp Interceptors**.

- What is the difference between an **Application Interceptor** and a **Network Interceptor**?
- Give a real-world use case for each type. _(5 marks)_

```
Answer:



```

```kotlin
// Write a logging interceptor that logs request URL and response code:

class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {



    }
}
```

---

### SECTION B — Design Pattern Implementation (20 Minutes)   _(15 marks)_

---

**Q1.** Implement a **thread-safe Singleton** in Kotlin. Show at least two approaches and explain the tradeoffs. _(5 marks)_

```kotlin
// Approach 1 — Object declaration:



// Approach 2 — Double-checked locking (if needed for lazy init with params):



```

```
Tradeoff explanation:


```

---

**Q2.** Design a **custom LiveData** from scratch.

- Implement the Observer pattern.
- Make it lifecycle-aware (explain how you would hook into `LifecycleOwner`).
- You do not need to write the full framework — write the core skeleton with key methods. _(10 marks)_

```kotlin
// CustomLiveData implementation:

class CustomLiveData<T> {

    // Observer storage


    // setValue — called on main thread


    // postValue — called from background thread


    // observe — registers observer with lifecycle awareness


    // removeObserver


}

// Observer interface:
interface Observer<T> {

}
```

```
Explain how lifecycle awareness is achieved:


```

---

### SECTION C — System Design: Zomato Feature (35 Minutes)   _(20 marks)_

**Task:** Design the **Restaurant Search + Feed** feature for the Zomato Android app. _(The feature a user sees when they open Zomato — search bar, filters, restaurant cards, pagination.)_

---

**Q1.** List functional and non-functional requirements. _(2 marks)_

```
Functional:
1.
2.
3.

Non-Functional:
1.
2.
```

---

**Q2.** Describe your overall architecture. Which layers will you have and what are their responsibilities? _(4 marks)_

```
Architecture (MVVM / MVI / Clean — justify your choice):


Layers:
1. UI Layer:
2. Domain/UseCase Layer:
3. Data Layer:
4. Network Layer:
```

---

**Q3.** How will you implement **pagination** for the restaurant list? Compare Paging 3 vs manual pagination. _(4 marks)_

```
Answer:



```

---

**Q4.** How will you implement **search with debounce** so the API is not called on every keystroke? _(3 marks)_

```kotlin
// Debounced search using Flow:



```

---

**Q5.** How will you handle **offline support** — what happens when there is no internet? _(3 marks)_

```
Answer:


```

---

**Q6.** What are the **performance considerations** for rendering the restaurant card list smoothly? _(4 marks)_

```
Answer (cover: image loading, ViewHolder reuse, DiffUtil, layout complexity):



```

---

**Round 2 Self Score:**     _____ / 45

---

---

## FINAL SCORECARD

|Round|Section|Max Marks|Your Score|
|---|---|---|---|
|Round 1|Warm-Up|5||
|Round 1|Android Internals|40||
|Round 1|DSA — Priority Queue|10||
|Round 2|Android Deep Dive|10||
|Round 2|Design Patterns|15||
|Round 2|System Design|20||
|**TOTAL**||**100**||

---

## POST-EXAM REFLECTION

**What went well?**

**What needs improvement?**

**Peer Feedback:**

---

## PREPARATION CHECKLIST

- [ ] Fragment Lifecycle — all callbacks, rotation, back stack (explain without notes)
- [ ] FragmentManager and FragmentTransaction pitfalls
- [ ] RecyclerView — RecycledViewPool, DiffUtil, ViewHolder reuse
- [ ] ViewModel — ViewModelStore, configuration change survival
- [ ] WorkManager — internal API selection, chaining
- [ ] ContentProvider — URI matching, CRUD
- [ ] ForegroundService — when required, notification rules
- [ ] Dagger Hilt — compile-time code gen, scoping, edge cases
- [ ] OkHttp Interceptors — application vs network, use cases
- [ ] Singleton — thread-safe approaches in Kotlin
- [ ] Custom LiveData — Observer pattern + lifecycle awareness
- [ ] System Design — pagination, debounce search, offline support
- [ ] DSA — Priority Queue (min/max heap) problems

---

_Based on real Zomato SDE 1 Android interview — Apr 2025._