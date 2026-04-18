# MEESHO SDE 1 — ANDROID DEVELOPER

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
> 1. This exam has **4 Rounds** matching the actual Meesho SDE 1 interview process.
> 2. Use a strict timer for each round. Do not exceed the time limit.
> 3. Write your answers in the blank spaces provided below each question.
> 4. For coding questions, write full working code — not pseudocode.
> 5. Self-evaluate your score at the end of each round.
> 6. After completing, share this sheet with a peer for review and feedback.

---

## ROUND 1 — ONLINE ASSESSMENT (OA)

**Time Allowed:** 90 Minutes      **Marks:** 25

> This round has 4 DSA problems: 2 Medium, 1 Medium-Hard, 1 Hard. Solve them in order. Use a strict timer.

---

### Problem 1 — MEDIUM (20 min)

**Given an array of integers and a target sum, return the indices of the two numbers that add up to the target. Each input has exactly one solution.**

**Example:**

```
Input:  nums = [2, 7, 11, 15], target = 9
Output: [0, 1]
```

**Step 1 — Approach (2 min)**

```
Approach:



```

**Step 2 — Code**

```kotlin
fun twoSum(nums: IntArray, target: Int): IntArray {



}
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|

_(5 marks)_

---

### Problem 2 — MEDIUM (20 min)

**Given a linked list, detect if it contains a cycle. If it does, return the node where the cycle begins. If no cycle, return null.**

**Example:**

```
Input:  3 → 2 → 0 → -4 → (back to node 2)
Output: Node with value 2
```

**Step 1 — Approach (2 min)**

```
Approach:



```

**Step 2 — Code**

```kotlin
class ListNode(var `val`: Int) {
    var next: ListNode? = null
}

fun detectCycle(head: ListNode?): ListNode? {



}
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|

_(5 marks)_

---

### Problem 3 — MEDIUM-HARD (25 min)

**Given a binary array and a window of size K, you can flip at most one window of K consecutive elements (0s become 1s). Find the maximum sum of the array after the flip.**

**Example:**

```
Input:  arr = [0, 1, 0, 0, 1, 1, 0], k = 3
Output: 6  (flip indices 2,3,4 → [0,1,1,1,1,1,0])
```

**Step 1 — Approach (3 min)**

```
Approach:



```

**Step 2 — Code**

```kotlin
fun maxSumAfterFlip(arr: IntArray, k: Int): Int {



}
```

**Step 3 — Dry Run**

```
Input: arr = [0,1,0,0,1,1,0], k = 3

Trace:



Output:
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|

_(7 marks)_

---

### Problem 4 — HARD (25 min)

**Maximum Sum Circular Subarray — Given a circular integer array, find the maximum possible sum of a non-empty subarray.**

**Example:**

```
Input:  nums = [1, -2, 3, -2]
Output: 3  (subarray [3])

Input:  nums = [5, -3, 5]
Output: 10  (subarray [5, 5] wrapping around)
```

**Step 1 — Approach (3 min)**

```
Approach:



```

**Step 2 — Code**

```kotlin
fun maxSubarraySumCircular(nums: IntArray): Int {



}
```

**Step 3 — Dry Run**

```
Input: nums = [5, -3, 5]

Trace:



Output:
```

**Step 4 — Edge Cases**

```
Edge case 1 (all negative):

Edge case 2 (single element):
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|
|Handles circular wrap?|Yes / No|
|Handles all-negative?|Yes / No|

_(8 marks)_

---

**Round 1 Self Score:** _____ / 25

---

---

## ROUND 2 — DSA INTERVIEW

**Time Allowed:** 45 Minutes      **Marks:** 25

> Interviewer is present for the entire duration. Think aloud. Explain your approach before coding.

---

### Problem 1 — Binary Array Flip Window (20 min)

**This is an extended/deeper version of OA Problem 3. The interviewer may present it differently or add follow-ups.**

**Problem:** Given a binary array and a window size K, find the starting index of the window you should flip (0→1) to maximise the total sum of the array.

**Step 1 — Clarifying Questions (write 2–3 questions you would ask)**

```
1.
2.
3.
```

**Step 2 — Approach**

```
Approach:



```

**Step 3 — Code**

```kotlin
fun bestFlipStart(arr: IntArray, k: Int): Int {



}
```

**Step 4 — Follow-up: What if you can flip at most 2 windows of size K?**

```kotlin
// Extended solution:



```

|||
|---|---|
|Base Time Complexity|O( )|
|Base Space Complexity|O( )|
|Follow-up Time Complexity|O( )|
|Time Taken|___ min|

_(12 marks — 6 base + 6 follow-up)_

---

### Problem 2 — Maximum Sum Circular Subarray (25 min)

**Same problem as OA Problem 4, but now with the interviewer. You must explain your Kadane's algorithm extension clearly.**

**Step 1 — Explain your approach aloud (write key points)**

```
Key points:
1.
2.
3.
```

**Step 2 — Code (clean, production-quality)**

```kotlin
fun maxSubarraySumCircular(nums: IntArray): Int {



}
```

**Step 3 — Walk the interviewer through this test case**

```
Input: nums = [3, -1, 2, -1]

Trace (show maxSum and minSum calculation):



Output:
```

**Step 4 — Follow-up: What is the time and space complexity? Can it be done in one pass?**

```
Answer:


```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|One-pass possible?|Yes / No|
|Time Taken|___ min|

_(13 marks — 8 solution + 5 explanation/follow-up)_

---

**Round 2 Self Score:** _____ / 25

---

---

## ROUND 3 — LOW LEVEL DESIGN (LLD)

**Time Allowed:** 60 Minutes      **Marks:** 30

> Design and implement a **Cab Management System** in Kotlin/Java. You have 1 hour to write working code. The interviewer evaluates correctness, OOP design, extensibility, and code quality.

---

### Requirements Gathering (5 min)

**Q1.** List the entities and key operations you identify from the problem. _(3 marks)_

```
Entities:
1.
2.
3.
4.

Key Operations:
1.
2.
3.
4.
```

---

### Class Design (10 min)

**Q2.** Draw or describe the class diagram — classes, attributes, methods, and relationships. _(5 marks)_

```
Class Diagram / Description:




```

---

### Implementation (40 min)

**Q3.** Implement the full Cab Management System. Write clean, compilable Kotlin code. _(18 marks)_

```kotlin
// === ENUMS ===



// === DATA CLASSES / MODELS ===



// === INTERFACES ===



// === DRIVER / CAB CLASS ===



// === RIDER CLASS ===



// === TRIP CLASS ===



// === CAB MANAGER (Core System) ===



// === MAIN / DEMO ===
fun main() {



}
```

---

### Design Questions (5 min)

**Q4.** Answer the following: _(4 marks)_

**(a)** Which design patterns did you use, and why?

```
Answer:


```

**(b)** How would you extend this system to support ride-pooling (multiple riders per cab)?

```
Answer:


```

---

### Self-Evaluation Checklist

|Criterion|Done?|
|---|---|
|All entities modelled as classes|Yes / No|
|Booking flow implemented end-to-end|Yes / No|
|Cab status updated on booking/completion|Yes / No|
|Used at least one design pattern|Yes / No|
|Code is readable and well-named|Yes / No|
|Edge cases handled (no available cab, etc.)|Yes / No|

---

**Round 3 Self Score:** _____ / 30

---

---

## ROUND 4 — HIRING MANAGER ROUND

**Time Allowed:** 30 Minutes      **Marks:** 20

> Questions span past projects, design patterns, high-level design, and behavioural scenarios.

---

### Section A — Past Projects & Behavioural (15 min)

**Q1.** Tell me about your most impactful project. What was the problem, what did you build, and what was the outcome? _(4 marks)_

```
Answer:




```

---

**Q2.** Tell me about a time you disagreed with a technical decision in your team. How did you handle it? _(4 marks)_

```
Answer:




```

---

### Section B — Design Patterns (5 min)

**Q3.** Name 2 design patterns you have used in a real project. For each, explain the problem it solved. _(4 marks)_

```
Pattern 1:


Pattern 2:


```

---

### Section C — High Level Design (10 min)

**Q4.** Design the high-level architecture for Meesho's **Product Search & Feed** feature (the home feed showing products to a user). Cover: client, API layer, caching, database, and ranking. _(4 marks)_

```
Architecture Description / Diagram:




```

**Key components to mention:**

|Component|Your Answer|
|---|---|
|Client-side caching strategy||
|API design (REST / GraphQL)||
|Feed ranking approach||
|Handling 10M+ products||

---

**Q5.** Do you have questions for us? Write 2–3 thoughtful questions you would ask the Hiring Manager. _(4 marks)_

```
Questions:
1.

2.

3.
```

---

**Round 4 Self Score:** _____ / 20

---

---

## FINAL SCORECARD

|Round|Description|Max Marks|Your Score|
|---|---|---|---|
|Round 1|Online Assessment — 4 DSA Problems|25||
|Round 2|DSA Interview — Flip Window + Circular Subarray|25||
|Round 3|Low Level Design — Cab Management System|30||
|Round 4|Hiring Manager — Projects, Patterns, HLD|20||
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

### DSA

- [ ] Solve Kadane's Algorithm variants (circular subarray, max product subarray)
- [ ] Sliding window problems — fixed size and variable size
- [ ] Linked List cycle detection (Floyd's algorithm)
- [ ] Two pointer and HashMap patterns
- [ ] Practice solving Hard problems within 25 minutes
- [ ] Practice explaining your approach aloud before coding

### Low Level Design

- [ ] Implement Cab/Ride Sharing System from scratch
- [ ] Implement Parking Lot System
- [ ] Implement Library Management System
- [ ] Know and apply: Strategy, Observer, Factory, Singleton patterns
- [ ] Practice writing clean OOP code under time pressure (60 min)
- [ ] Know SOLID principles with examples

### High Level Design

- [ ] Design a product search feed (Meesho-style)
- [ ] Understand caching strategies: CDN, Redis, in-memory
- [ ] Understand database choices: SQL vs NoSQL for different use cases
- [ ] Understand pagination, infinite scroll, and feed ranking

### Behavioural

- [ ] Prepare 2–3 strong project stories using STAR format
- [ ] Prepare "Why Meesho?" answer (research their mission and products)
- [ ] Prepare compensation answer with justification
- [ ] Prepare 3 thoughtful questions for the Hiring Manager

---

_Template based on real Meesho SDE 1 Android interview experience — Feb 2024._