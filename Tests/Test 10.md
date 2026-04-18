# PAYTM — SENIOR SOFTWARE ENGINEER (ANDROID)

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
> 1. This exam has **2 Rounds** matching the actual Paytm Senior Android process.
> 2. Use a strict timer per round.
> 3. For coding questions, write full working code — not pseudocode.
> 4. Self-evaluate at the end of each round.

---

---

## ROUND 1 — HACKERRANK ASSESSMENT

**Time Allowed:** 60 Minutes      **Marks:** 35

> 2 DSA problems + Android/Kotlin MCQs. Solve DSA first, then attempt MCQs.

---

### Part A — DSA Problems

---

### Problem 1 — Easy/Medium (20 min)

**Valid Parentheses**

Given a string containing just `(`, `)`, `{`, `}`, `[`, `]`, determine if the input string is valid. An input string is valid if open brackets are closed in the correct order.

**Example:**

```
Input:  s = "()[]{}"
Output: true

Input:  s = "([)]"
Output: false
```

**Step 1 — Approach**

```
Approach:


```

**Step 2 — Code**

```kotlin
fun isValid(s: String): Boolean {



}
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|

_(8 marks)_

---

### Problem 2 — Easy/Medium (20 min)

**Merge Intervals**

Given an array of intervals, merge all overlapping intervals and return an array of the non-overlapping intervals.

**Example:**

```
Input:  intervals = [[1,3],[2,6],[8,10],[15,18]]
Output: [[1,6],[8,10],[15,18]]
```

**Step 1 — Approach**

```
Approach:


```

**Step 2 — Code**

```kotlin
fun merge(intervals: Array<IntArray>): Array<IntArray> {



}
```

**Step 3 — Dry Run**

```
Input: [[1,3],[2,6],[8,10],[15,18]]

Trace:



Output:
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|

_(9 marks)_

---

### Part B — Android / Kotlin MCQs (20 min)

Answer each question clearly and concisely as if it were a written MCQ with justification.

---

**Q1.** What is the difference between `ApplicationContext` and `ActivityContext`? When should you use each? _(3 marks)_

```
Answer:



```

---

**Q2.** What are Activity launch modes? Describe each with a use case. _(3 marks)_

```
standard:

singleTop:

singleTask:

singleInstance:
```

---

**Q3.** What is the difference between `AppCompatActivity` and `ComponentActivity`? When would you choose one over the other? _(3 marks)_

```
Answer:



```

---

**Q4.** What is the Activity lifecycle? List all callbacks in order for: _(3 marks)_

**(a)** App first launch:

```
Answer:
```

**(b)** User presses Home button:

```
Answer:
```

**(c)** User rotates screen:

```
Answer:
```

---

**Q5.** What is the difference between `Fragment.onAttach()`, `onCreateView()`, and `onViewCreated()`? Where should you initialise views and why? _(3 marks)_

```
Answer:



```

---

**Round 1 Self Score:** _____ / 35

---

---

## ROUND 2 — TECHNICAL INTERVIEW (DSA + Android Concepts)

**Time Allowed:** 60 Minutes      **Marks:** 65

> Video call format. 2 LeetCode problems + deep Android discussion. Think aloud throughout.

---

### Part A — DSA (30 min)

---

### Problem 1 — Easy/Medium (15 min)

**Best Time to Buy and Sell Stock**

Given an array `prices` where `prices[i]` is the price of a stock on day `i`, find the maximum profit you can achieve by buying on one day and selling on a later day. If no profit is possible, return 0.

**Example:**

```
Input:  prices = [7,1,5,3,6,4]
Output: 5  (buy at 1, sell at 6)
```

**Step 1 — Approach**

```
Approach:

```

**Step 2 — Code**

```kotlin
fun maxProfit(prices: IntArray): Int {



}
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|

_(10 marks)_

---

### Problem 2 — Easy/Medium (15 min)

**Find All Duplicates in an Array**

Given an integer array of length n where all integers are in range [1, n] and each integer appears once or twice, return all integers that appear twice.

**Example:**

```
Input:  nums = [4,3,2,7,8,2,3,1]
Output: [2,3]
```

**Step 1 — Approach**

```
Approach (aim for O(n) time, O(1) extra space):

```

**Step 2 — Code**

```kotlin
fun findDuplicates(nums: IntArray): List<Int> {



}
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|

_(10 marks)_

---

### Part B — Android Deep Dive (30 min)

---

**Q1.** Deep dive: `ApplicationContext` vs `ActivityContext` — what are the risks of passing `ActivityContext` to a singleton? How does this cause a memory leak? _(5 marks)_

```
Answer:




```

---

**Q2.** Explain Activity launch modes with a real-world scenario for each. What happens to the back stack in `singleTask`? _(5 marks)_

```
Answer:




```

---

**Q3.** Fragment performance optimisation — how do you avoid unnecessary fragment recreation? What is `setRetainInstance` (deprecated) and what replaced it? _(5 marks)_

```
Answer:



```

---

**Q4.** What is overdraw in Android? How do you detect and reduce it? _(5 marks)_

```
Answer:



```

**Q5.** You have a RecyclerView with 10,000 items loading images from the network. Walk me through every optimisation you would apply. _(5 marks)_

```
Answer:




```

---

**Q6.** Write a thread-safe Singleton in Kotlin. Why is `object` in Kotlin thread-safe? _(5 marks)_

```kotlin
// Option 1 — Kotlin object:



// Option 2 — Double-checked locking (Java-style, for comparison):



// Why is Kotlin object thread-safe?

```

---

**Round 2 Self Score:** _____ / 65

---

---

## FINAL SCORECARD

|Round|Description|Max Marks|Your Score|
|---|---|---|---|
|Round 1A|HackerRank — DSA Problems|17||
|Round 1B|HackerRank — Android/Kotlin MCQs|18||
|Round 2A|Technical Interview — DSA|20||
|Round 2B|Technical Interview — Android Concepts|45||
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

### DSA

- [ ] Stack-based problems — valid parentheses, largest rectangle, daily temperatures
- [ ] Array manipulation — merge intervals, find duplicates, rotate array
- [ ] Sliding window — max profit, longest substring
- [ ] Solve 2 Easy/Medium problems daily under 20-minute timer

### Android

- [ ] Context types — Application vs Activity vs Service, memory leak implications
- [ ] All 4 launch modes with back stack diagrams
- [ ] AppCompatActivity vs ComponentActivity vs Activity hierarchy
- [ ] Activity + Fragment lifecycle — draw from memory for all scenarios
- [ ] RecyclerView optimisation — ViewHolder, DiffUtil, image loading, prefetch
- [ ] Overdraw detection — GPU Overdraw tool in Developer Options

---

_Template based on real Paytm Senior Android Engineer interview experience — Apr 2025._