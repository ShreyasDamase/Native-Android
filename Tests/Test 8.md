# PHONEPE — SENIOR SOFTWARE ENGINEER (ANDROID)

# MOCK INTERVIEW EXAMINATION PAPER

---

|Field|Details|
|---|---|
|Candidate Name||
|Date||
|Time Started||
|Time Ended||
|Total Score|/ 100|
|Experience Level|~4 Years Android|

---

> **INSTRUCTIONS TO CANDIDATE**
> 
> 1. This exam has **5 Rounds** matching the actual PhonePe Senior Android onsite process.
> 2. Use a strict timer per round. Do not exceed limits.
> 3. For coding rounds, write full working code — not pseudocode.
> 4. For design rounds, write structured notes as if talking to an interviewer.
> 5. Self-evaluate at the end of each round.

---

---

## ROUND 1 — MACHINE CODING (App Implementation)

**Time Allowed:** 120 Minutes      **Marks:** 20

> Build a fully working Android app. Focus: **functionality, architecture, design principles, and extensibility**. The actual ask was a tile-swapping matrix game. Implement it below as a design + code exercise.

---

### Problem: Tile Swap Puzzle Game

Build an Android app with an N×N grid of numbered tiles. The user can tap two tiles to swap them. The goal is to arrange tiles in order (1 to N²-1, with one empty tile). Track moves and show a win state.

---

**Q1.** List all features and edge cases you plan to handle before writing code. _(2 marks)_

```
Features:
1.
2.
3.
4.
5.

Edge Cases:
1.
2.
3.
```

---

**Q2.** Choose your architecture and justify it. _(2 marks)_

```
Architecture (MVVM / MVI / MVP):

Justification:

Layers planned:
1.
2.
3.
```

---

**Q3.** Implement the core game logic — the matrix model, swap logic, and win detection. _(8 marks)_

```kotlin
// === GAME STATE MODEL ===



// === TILE / BOARD CLASS ===



// === GAME LOGIC ===
class PuzzleGame(private val size: Int) {

    // Initialize board



    // Swap two tiles



    // Check win condition



    // Shuffle board (ensure solvable)



}
```

---

**Q4.** Implement the ViewModel and describe your UI layer. _(5 marks)_

```kotlin
// === VIEWMODEL ===
class PuzzleViewModel : ViewModel() {



}
```

```
UI Layer Description (RecyclerView / GridLayout / Custom View):



How would you animate the tile swap?


```

---

**Q5.** How would you extend this to support: _(3 marks)_

**(a)** Different board sizes (3×3, 4×4, 5×5) selectable by user

```
Answer:

```

**(b)** A leaderboard with best move counts stored locally

```
Answer:

```

**(c)** A hint system that suggests the next best move

```
Answer:

```

---

**Round 1 Self Score:** _____ / 20

---

---

## ROUND 2 — PROBLEM SOLVING (DSA)

**Time Allowed:** 60 Minutes      **Marks:** 25

> 4 problems. Time-box each one strictly.

---

### Problem 1 — Rain Water Trapping (15 min)

Given an array of non-negative integers representing an elevation map where each bar has width 1, compute how much water it can trap after raining.

**Example:**

```
Input:  height = [0,1,0,2,1,0,1,3,2,1,2,1]
Output: 6
```

**Step 1 — Approach**

```
Approach:


```

**Step 2 — Code**

```kotlin
fun trap(height: IntArray): Int {



}
```

**Step 3 — Dry Run**

```
Input: [0,1,0,2,1,0,1,3,2,1,2,1]

Trace:



Output:
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|

_(6 marks)_

---

### Problem 2 — Add Two Numbers as Linked Lists (10 min)

Two non-negative integers are stored in linked lists in reverse order. Add them and return the sum as a linked list (also in reverse order).

**Example:**

```
Input:  l1 = [2,4,3]  (represents 342)
        l2 = [5,6,4]  (represents 465)
Output: [7,0,8]       (represents 807)
```

**Step 1 — Approach**

```
Approach:


```

**Step 2 — Code**

```kotlin
class ListNode(var `val`: Int, var next: ListNode? = null)

fun addTwoNumbers(l1: ListNode?, l2: ListNode?): ListNode? {



}
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Handles carry at end?|Yes / No|
|Time Taken|___ min|

_(5 marks)_

---

### Problem 3 — Kth Largest Element in a Stream (15 min)

Design a class that finds the Kth largest element in a stream of numbers. It must support `add(val)` which adds a number and returns the Kth largest.

**Example:**

```
KthLargest(3, [4,5,8,2])
add(3)  → 4
add(5)  → 5
add(10) → 8
add(9)  → 8
add(4)  → 8
```

**Step 1 — Approach**

```
Approach:


```

**Step 2 — Code**

```kotlin
class KthLargest(private val k: Int, nums: IntArray) {



    fun add(`val`: Int): Int {



    }
}
```

**Step 3 — Follow-up: What is the time complexity of each `add()` call?**

```
Answer:

```

|||
|---|---|
|Time Complexity per add()|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|

_(7 marks)_

---

### Problem 4 — Implement rand16() using rand4() (20 min)

You are given `rand4()` which returns a uniformly random integer from 1 to 4 (inclusive). Implement `rand16()` which returns a uniformly random integer from 1 to 16 with equal probability. You may only use `rand4()`.

**Step 1 — Approach**

```
Approach (think in terms of base-4 arithmetic):



```

**Step 2 — Code**

```kotlin
fun rand4(): Int = (1..4).random() // given

fun rand16(): Int {



}
```

**Step 3 — Prove Uniformity**

```
Why is every number from 1–16 equally likely?



```

**Step 4 — Follow-up: Using rand4(), implement rand7()**

```kotlin
// Hint: Use rejection sampling

fun rand7(): Int {



}
```

|||
|---|---|
|rand16() uniform?|Yes / No|
|rand7() uses rejection sampling?|Yes / No|
|Time Taken|___ min|

_(7 marks)_

---

**Round 2 Self Score:** _____ / 25

---

---

## ROUND 3 — ANDROID SYSTEM DESIGN (App Store Feature)

**Time Allowed:** 60 Minutes      **Marks:** 25

> Design a **Play Store-like App Listing & Discovery** feature for an Android app. Think of all features first, then deep dive as directed.

---

### Step 1 — Feature Identification (5 min)

**Q1.** List all features you would consider for this system. _(3 marks)_

```
Features:
1. Categories (browsing apps by category)
2. Recommended for user (personalised)
3. Editor's choice (curated list)
4. Search
5. App metadata (name, icon, rating, size, screenshots)
6. Post-install actions: Update / Uninstall
7.
8.
```

---

### Step 2 — Architecture & Data Flow (10 min)

**Q2.** Describe the high-level architecture for this feature — client, API, caching, and database layers. _(4 marks)_

```
Architecture:




```

---

**Q3.** Deep dive: Editor's Choice vs Recommended For You — how does the data differ, and how does this affect your API and caching strategy? _(4 marks)_

```
Editor's Choice (same for everyone):
- API design:
- Caching strategy:

Recommended For You (personalised):
- API design:
- Caching strategy:
```

---

### Step 3 — Client-Side Caching with Versioning (10 min)

**Q4.** You implemented versioning in the data so you don't fetch the complete list every time. Describe this mechanism. _(4 marks)_

```
Versioning Strategy:




API Contract (request/response sketch):




How do you handle partial updates?


```

---

### Step 4 — Auto Update (Background Work) (10 min)

**Q5.** How do you implement auto-update for installed apps — including when the app is killed? _(4 marks)_

```
Approach (WorkManager / JobScheduler / AlarmManager — justify your choice):




How do you check which apps have updates available?


How do you download and apply the update silently?


```

---

### Step 5 — Dynamic Banner (Remote Config) (10 min)

**Q6.** You want to show a dynamic banner to users that is configurable without an app release. Design this. _(6 marks)_

**Interviewer questions to answer:**

**(a)** How much do you want to be configurable?

```
Answer:

```

**(b)** At what frequency do you want to add new configurations?

```
Answer:

```

**(c)** If partial configuration, how do you fetch it?

```
Answer:

```

**(d)** At what times (triggers) do you fetch updated config?

```
Answer:

```

**(e)** How do you implement this — new API or server-side proxy that hijacks responses?

```
Answer:


```

---

**Round 3 Self Score:** _____ / 25

---

---

## ROUND 4 — ANDROID DEEP DIVE (Technical Concepts)

**Time Allowed:** 45 Minutes      **Marks:** 20

---

### Security

**Q1.** Man-in-the-Middle (MITM) Attack — how can it happen on Android and how do you prevent it? _(3 marks)_

```
Answer:



```

**Q2.** A phone is rooted. An attacker decompiles your APK, modifies the code, and recompiles it. How do you detect this on the server side? _(3 marks)_

```
Detection strategies:
1.
2.
3.
```

---

### SQLite

**Q3.** What is the difference between table-level locking and database-level locking in SQLite? When does each occur? _(3 marks)_

```
Answer:



```

**Q4.** Write a query to create a table and add an index. When would you use an index on a mobile database? _(2 marks)_

```sql
-- Create table:



-- Add index:



-- When to use index on mobile:

```

---

### Activity & Fragment Lifecycle

**Q5.** What happens to an Activity when the system is low on memory? What callbacks are triggered and in what order? _(3 marks)_

```
Answer:



```

**Q6.** Why must Activities be declared in the AndroidManifest.xml? What would happen if they weren't? _(2 marks)_

```
Answer:


```

**Q7.** Explain the Fragment lifecycle in relation to its host Activity lifecycle. Where do Fragment transactions go wrong most often? _(4 marks)_

```
Answer:



Common pitfalls:
1.
2.
```

---

**Round 4 Self Score:** _____ / 20

---

---

## ROUND 5 — HIRING MANAGER ROUND

**Time Allowed:** 30 Minutes      **Marks:** 10

> Conversational round. Focus on your story, expectations, and culture fit.

---

**Q1.** Tell me about yourself — your journey as an Android developer over the past ~4 years. What have you built and what are you most proud of? _(3 marks)_

```
Answer:




```

---

**Q2.** What are your expectations from PhonePe — technically and culturally? What kind of problems do you want to work on? _(3 marks)_

```
Answer:



```

---

**Q3.** PhonePe operates at massive scale (hundreds of millions of users). How does that excite or challenge you? _(2 marks)_

```
Answer:


```

---

**Q4.** Do you have any questions for us? Write 2–3 thoughtful questions. _(2 marks)_

```
Questions:
1.

2.

3.
```

---

**Round 5 Self Score:** _____ / 10

---

---

## FINAL SCORECARD

|Round|Description|Max Marks|Your Score|
|---|---|---|---|
|Round 1|Machine Coding — Tile Swap Puzzle App|20||
|Round 2|Problem Solving — 4 DSA Problems|25||
|Round 3|Android System Design — App Store Feature|25||
|Round 4|Android Deep Dive — Security, SQLite, Lifecycle|20||
|Round 5|Hiring Manager — Story & Expectations|10||
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

### Machine Coding

- [ ] Build a tile/grid-based game with clean MVVM architecture in under 2 hours
- [ ] Practice: Snake, 2048, Tic-Tac-Toe, Sudoku Validator
- [ ] Focus on extensibility — always ask "how would I add a new feature?"
- [ ] Use ViewBinding, clean separation of UI and logic

### DSA

- [ ] Rain Water Trapping — two pointer O(n) solution
- [ ] Add Two Numbers as Linked List — handle carry edge case
- [ ] Kth Largest in Stream — Min-Heap of size K
- [ ] rand(N) from rand(M) — understand rejection sampling
- [ ] Sliding Window, Two Pointers, Heap problems (LeetCode Medium/Hard)

### Android System Design

- [ ] Design App Store / Play Store — categories, recommendations, caching
- [ ] Client-side caching with versioning / ETags
- [ ] Background work: WorkManager vs JobScheduler vs AlarmManager
- [ ] Remote config: Firebase Remote Config internals, server-side feature flags
- [ ] Offline-first architecture and sync strategies

### Android Deep Dive

- [ ] MITM prevention: Certificate pinning, OkHttp CertificatePinner
- [ ] APK tamper detection: SafetyNet / Play Integrity API, server-side signature checks
- [ ] SQLite: WAL mode, locking modes, indexing strategy on mobile
- [ ] Activity/Fragment lifecycle edge cases: low memory, back stack, transactions
- [ ] Why AndroidManifest — intent resolution, system registration

### Behavioural

- [ ] Prepare your 4-year Android journey as a 2-minute story
- [ ] Research PhonePe — payments infrastructure, Indus Appstore, scale challenges
- [ ] Prepare 3 thoughtful questions for the HM

---

_Template based on real PhonePe Senior Android Engineer interview experience — June 2019._