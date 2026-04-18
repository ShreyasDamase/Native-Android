# PHONEPE — ANDROID DEVELOPER

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
> 1. This exam replicates PhonePe's DSA-focused first round for Android Developer.
> 2. Problems are non-standard — think carefully before coding.
> 3. Explain logic clearly. Interviewers give hints — note where you needed one.
> 4. Self-evaluate at the end.

---

---

## ROUND 1 — DSA INTERVIEW

**Time Allowed:** 60 Minutes      **Marks:** 100

> 2 Medium/Hard problems. Problems are **not standard LeetCode** — expect variations. Think aloud, explain clearly, write working code.

---

### Problem 1 — LIS Variation (30 min)

**Longest Increasing Subsequence with a Twist**

Given an array of integers, find the length of the longest strictly increasing subsequence where the **difference between consecutive elements is at most K**.

**Example:**

```
Input:  nums = [3, 10, 2, 1, 20], k = 10
Output: 4  (subsequence: [1, 2, 10, 20] — each consecutive diff ≤ 10)

Input:  nums = [1, 5, 2, 9, 3], k = 4
Output: 3  (subsequence: [1, 2, 3] or [1, 5, 9])
```

**Step 1 — Clarifying Questions (write 2–3 you would ask)**

```
1.
2.
3.
```

**Step 2 — Approach**

```
Naive approach (O(n²)):


Optimised approach:


```

**Step 3 — Code**

```kotlin
fun lisWithMaxDiff(nums: IntArray, k: Int): Int {



}
```

**Step 4 — Dry Run**

```
Input: nums = [3, 10, 2, 1, 20], k = 10

Trace (show dp array or key states):



Output:
```

**Step 5 — Complexity Analysis**

```
Time Complexity:
Space Complexity:
Can it be further optimised? How?
```

**Step 6 — Follow-up: What if you need to return the actual subsequence, not just its length?**

```kotlin
// Return the subsequence:



```

|||
|---|---|
|Base Time Complexity|O( )|
|Base Space Complexity|O( )|
|Follow-up implemented?|Yes / No|
|Hints needed?|Yes / No — how many: ___|
|Time Taken|___ min|

_(50 marks — 35 solution + 10 follow-up + 5 explanation clarity)_

---

### Problem 2 — Priority Queue Problem (30 min)

**K Closest Points to a Meeting Location**

You are given a list of employee locations `(x, y)` and a meeting point `(mx, my)`. Find the K employees closest to the meeting point using Euclidean distance. Return them in any order. **Do not sort the entire array.**

**Example:**

```
Input:  points = [(1,3),(-2,2),(5,8),(0,1)], meeting = (0,0), k = 2
Output: [(1,3),(-2,2)]  (distances: √10, √8, √89, √1 — closest 2 are (-2,2) and (0,1))
```

**Step 1 — Clarifying Questions**

```
1.
2.
3.
```

**Step 2 — Approach**

```
Why a Max-Heap of size K works here:


Alternative approaches and trade-offs:


```

**Step 3 — Code**

```kotlin
fun kClosest(points: Array<IntArray>, meeting: IntArray, k: Int): Array<IntArray> {



}
```

**Step 4 — Dry Run**

```
Input: points = [(1,3),(-2,2),(5,8),(0,1)], meeting = (0,0), k = 2

Trace (show heap state at each step):



Output:
```

**Step 5 — Complexity Analysis**

```
Time Complexity:
Space Complexity:
Why is this better than sorting (O(n log n))?
```

**Step 6 — Follow-up: What if new employee locations arrive as a stream? How do you maintain K closest in real time?**

```
Answer:


```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Streaming follow-up answered?|Yes / No|
|Hints needed?|Yes / No — how many: ___|
|Time Taken|___ min|

_(50 marks — 35 solution + 10 follow-up + 5 explanation clarity)_

---

## FINAL SCORECARD

|Problem|Description|Max Marks|Your Score|
|---|---|---|---|
|Problem 1|LIS Variation — Max Diff K|50||
|Problem 2|Priority Queue — K Closest Points|50||
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

### LIS Variants (DP)

- [ ] Standard LIS — O(n²) DP and O(n log n) patience sorting
- [ ] LIS with constraint (max diff K) — this problem
- [ ] Number of LIS
- [ ] Russian Doll Envelopes (2D LIS)
- [ ] Longest Bitonic Subsequence

### Priority Queue / Heap

- [ ] K closest points to origin (LC 973)
- [ ] Kth largest in stream (LC 703)
- [ ] Merge K sorted lists (LC 23)
- [ ] Top K frequent elements (LC 347)
- [ ] Find median from data stream (LC 295)
- [ ] Task Scheduler (LC 621)

### General DSA Mindset for PhonePe

- [ ] Expect non-standard problems — read carefully before jumping to code
- [ ] Always state brute force first, then optimise
- [ ] Practise thinking aloud — interviewers give hints if you're on the right track
- [ ] Dry run every solution on a non-trivial example before submitting

---

_Template based on real PhonePe Android Developer interview experience by Gaurav — Apr 2025._