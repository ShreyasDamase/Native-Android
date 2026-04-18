\---

## title: Zomato SDE 1 – Android Developer Interview type: practice-exam candidate_name: date: total_marks: 100 time_allowed: 3 Hours 30 Minutes instructions: Attempt all sections in order. Set a timer per round. Do not skip ahead.

---

# ZOMATO SDE 1 — ANDROID DEVELOPER

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
> 1. This exam has **4 Rounds** matching the actual Zomato SDE 1 interview process.
> 2. Use a strict timer for each round. Do not exceed the time limit.
> 3. Write your answers in the blank spaces provided below each question.
> 4. For coding questions, write full working code — not just pseudocode.
> 5. Self-evaluate your score at the end of each round.
> 6. After completing, share this sheet with a peer for review and feedback.

---

---

## ROUND 1 — PHONE SCREENING (Engineering Manager Call)

**Time Allowed:** 20 Minutes     **Marks:** 10

---

**Q1.** Walk me through your current role. What are your day-to-day responsibilities and what tech stack do you work with? _(3 marks)_

```
Answer:




```

---

**Q2.** What is your most impactful Android project? Describe the problem, your contribution, and the outcome. _(4 marks)_

```
Answer:




```

---

**Q3.** Why Zomato? What excites you about working here specifically? _(3 marks)_

```
Answer:




```

---

**Round 1 Self Score:**     _____ / 10

---

---

## ROUND 2 — ANDROID FUNDAMENTALS + DSA

**Time Allowed:** 60 Minutes (45 min Android + 15 min DSA)     **Marks:** 35

---

### PART A — Android Fundamentals (45 Minutes)

---

**Q1.** Explain the complete **Activity Lifecycle**. What happens when the user rotates the screen? What callbacks are triggered and in what order? _(4 marks)_

```
Answer:




```

---

**Q2.** Explain **Coroutines Internals** in Kotlin. How does a coroutine suspension work under the hood? What is a `Continuation`? _(5 marks)_

```
Answer:




```

```kotlin
// Write a code example demonstrating suspend + continuation:



```

---

**Q3.** How does **ViewModel** work internally? What is `ViewModelStore`? How does ViewModel survive screen rotation? _(5 marks)_

```
Answer:




```

---

**Q4.** Explain the **RecyclerView** internals. How does `RecycledViewPool` work? What is the difference between `notifyDataSetChanged()` and `DiffUtil`? _(4 marks)_

```
Answer:




```

---

**Q5.** What are **Views and ViewGroups**? Explain the `measure → layout → draw` pass. How would you create a custom View? _(4 marks)_

```
Answer:




```

```kotlin
// Skeleton of a custom View:



```

---

**Q6.** Android App **Performance Optimisation** — answer the following:

**(a)** How do you detect and fix UI jank (dropped frames)?

```
Answer:


```

**(b)** What tools do you use for memory profiling? How do you detect a memory leak?

```
Answer:


```

**(c)** What is `StrictMode` and when would you use it?

```
Answer:


```

_(6 marks total — 2 per sub-question)_

---

**Q7.** What is **overdraw** in Android rendering? How do you reduce it? _(3 marks)_

```
Answer:


```

---

### PART B — DSA Problem (15 Minutes)

---

**Q8.** The interviewer will give you a problem and then **extend** it with a follow-up.

**Base Problem:** Given an array of integers, find the two numbers that sum to a target value. Return their indices.

**Step 1 — Write your approach (2 min)**

```
Approach:



```

**Step 2 — Write working code**

```kotlin
fun twoSum(nums: IntArray, target: Int): IntArray {



}
```

**Step 3 — Extended follow-up:** _What if the array is sorted? Can you improve the space complexity to O(1)?_

```kotlin
// Optimised solution for sorted array:



```

|||
|---|---|
|Base Time Complexity|O( )|
|Base Space Complexity|O( )|
|Optimised Time Complexity|O( )|
|Optimised Space Complexity|O( )|
|Time Taken|___ min|

_(7 marks — 3 base + 4 extended)_

---

**Round 2 Self Score:**     _____ / 35

---

---

## ROUND 3 — MOBILE SYSTEM DESIGN + COROUTINES + DESIGN PATTERNS + LEETCODE

**Time Allowed:** 75 Minutes     **Marks:** 35

---

### SECTION A — Mobile System Design (30 Minutes)   _(12 marks)_

**Task:** Design a **"Live Order Tracking"** feature for the Zomato Android app.

_(This is a small feature design — similar to what was asked in the actual interview.)_

---

**Q1.** List the key functional and non-functional requirements you would consider. _(2 marks)_

```
Functional Requirements:
1.
2.
3.

Non-Functional Requirements:
1.
2.
```

---

**Q2.** Draw or describe the high-level architecture for this feature (Layers, Components, Data Flow). _(4 marks)_

```
Architecture Diagram / Description:




```

---

**Q3.** How will you handle real-time location updates from the server to the app? _(WebSocket / SSE / Polling — justify your choice)_ _(3 marks)_

```
Answer:



```

---

**Q4.** How will you handle the case where the user goes offline mid-delivery? How do you sync state when they come back online? _(3 marks)_

```
Answer:



```

---

### SECTION B — Coroutines & Threads (15 Minutes)   _(8 marks)_

---

**Q1.** What is the difference between `Dispatchers.IO`, `Dispatchers.Main`, and `Dispatchers.Default`? When would you use each? _(3 marks)_

```
Answer:



```

---

**Q2.** What is the difference between a **Thread** and a **Coroutine**? Why are coroutines preferred on Android? _(2 marks)_

```
Answer:


```

---

**Q3.** What is `withContext`? How does it differ from `launch` and `async`? Write a code example. _(3 marks)_

```
Answer:


```

```kotlin
// Code example:



```

---

### SECTION C — Design Patterns (15 Minutes)   _(8 marks)_

**Task:** Implement the following two design patterns in Kotlin.

---

**Q1.** Implement the **Observer Pattern** (or use Kotlin `Flow` to demonstrate it). _(4 marks)_

```kotlin
// Observer Pattern implementation:



```

---

**Q2.** Implement the **Singleton Pattern** in a thread-safe way in Kotlin. _(2 marks)_

```kotlin
// Singleton implementation:



```

---

**Q3.** What is the **Repository Pattern**? Why is it used in Android architecture? _(2 marks)_

```
Answer:


```

---

### SECTION D — LeetCode Medium Problem (15 Minutes)   _(7 marks)_

**Task:** Solve the problem below. Write your solution on this sheet as if writing on Google Docs.

---

**Problem:** Given a string `s`, find the length of the **longest substring without repeating characters**.

**Example:**

```
Input:  s = "abcabcbb"
Output: 3  (the answer is "abc")

Input:  s = "pwwkew"
Output: 3  (the answer is "wke")
```

**Step 1 — Write your approach (2 min)**

```
Approach:



```

**Step 2 — Write working code**

```kotlin
fun lengthOfLongestSubstring(s: String): Int {



}
```

**Step 3 — Dry run**

```
Input: "abcabcbb"

Trace:



Output:
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|
|Code complete?|Yes / No|
|Correct output on example?|Yes / No|

---

**Round 3 Self Score:**     _____ / 35

---

---

## ROUND 4 — HIRING MANAGER ROUND

**Time Allowed:** 30 Minutes     **Marks:** 20

---

**Q1.** Where do you see yourself in the next 2–3 years? What kind of engineer do you want to become? _(4 marks)_

```
Answer:



```

---

**Q2.** Tell me about a time you disagreed with a technical decision made by your team. How did you handle it? _(4 marks)_

```
Answer:



```

---

**Q3.** How do you stay updated with the latest in Android development? Name 2–3 recent changes in Android that excite you. _(4 marks)_

```
Answer:



```

---

**Q4.** What are your compensation expectations? Are you open to relocation? _(Write your prepared answer)_ _(4 marks)_

```
Answer:



```

---

**Q5.** Do you have any questions for us? _(Write 2–3 thoughtful questions you would ask the Hiring Manager)_ _(4 marks)_

```
Questions:
1.

2.

3.
```

---

**Round 4 Self Score:**     _____ / 20

---

---

## FINAL SCORECARD

|Round|Description|Max Marks|Your Score|
|---|---|---|---|
|Round 1|Phone Screening (EM Call)|10||
|Round 2A|Android Fundamentals|28||
|Round 2B|DSA — Extended Problem|7||
|Round 3A|Mobile System Design|12||
|Round 3B|Coroutines & Threads|8||
|Round 3C|Design Patterns|8||
|Round 3D|LeetCode Medium|7||
|Round 4|Hiring Manager|20||
|**TOTAL**||**100**||

---

## POST-EXAM REFLECTION

**What went well?**

**What needs improvement?**

**Topics to revise before next attempt:**

- [ ]
- [ ]
- [ ]
- [ ]

**Peer / Friend Feedback:**

---

## PREPARATION CHECKLIST

Use this after completing the exam to plan your next steps.

### Android Fundamentals

- [ ] Explain Activity Lifecycle from memory, including rotation edge cases
- [ ] Explain ViewModel internals — ViewModelStore, ViewModelProvider
- [ ] Explain Coroutine Continuation and suspension mechanism
- [ ] Explain RecyclerView RecycledViewPool and DiffUtil
- [ ] Write a custom View from scratch (onMeasure, onDraw)
- [ ] Profile an app for memory leaks using LeakCanary and Android Profiler
- [ ] Explain overdraw and use GPU Overdraw tool in Developer Options

### System Design

- [ ] Design 2 Zomato features: Live Tracking, Restaurant Search Feed
- [ ] Practice explaining real-time update strategies (WebSocket vs SSE vs Polling)
- [ ] Practice offline-first sync design

### Coroutines & Threads

- [ ] Explain Dispatchers.IO vs Default vs Main with examples
- [ ] Explain structured concurrency, SupervisorJob, CoroutineExceptionHandler
- [ ] Write examples using withContext, async/await, Flow

### Design Patterns

- [ ] Implement Observer, Singleton, Factory, Repository patterns in Kotlin
- [ ] Explain SOLID principles with Android examples

### DSA

- [ ] Longest Substring Without Repeating Characters (LC 3) — solve in < 10 min
- [ ] Practice extending solutions with follow-up constraints
- [ ] Solve 1 Medium problem daily on a strict 20-minute timer

### Soft Skills

- [ ] Prepare "Why Zomato?" answer (< 2 minutes)
- [ ] Prepare compensation pitch with justification
- [ ] Prepare 3 thoughtful questions for the Hiring Manager

---

	_Template based on real Zomato SDE 1 Android interview experience — Feb 2024._