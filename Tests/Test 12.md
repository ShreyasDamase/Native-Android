# ZETA — ANDROID DEVELOPER

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
> 1. This exam replicates Zeta's DSA-first Android round.
> 2. The problem is **Hard difficulty** — accuracy under pressure is key.
> 3. All test cases must pass. Think before you code.
> 4. Self-evaluate at the end.

---

---

## ROUND 1 — DSA INTERVIEW (Hard)

**Time Allowed:** 60 Minutes      **Marks:** 100

> 1 Hard problem. Heavy emphasis on correctness — all test cases must pass.

---

### Problem — Aggressive Cows (Binary Search on Answer)

You are given an array of `n` stall positions (not necessarily sorted) and `c` cows. Place the cows in the stalls such that the **minimum distance between any two cows is maximised**. Return that maximum possible minimum distance.

**Example:**

```
Input:  stalls = [1, 2, 8, 4, 9], cows = 3
Output: 3
Explanation: Place cows at positions 1, 4, 9 → minimum gap = 3
```

---

**Step 1 — Clarifying Questions (write 2–3 you would ask)** _(5 marks)_

```
1.
2.
3.
```

---

**Step 2 — Identify the Pattern** _(5 marks)_

Answer the following before coding:

**(a)** Why is this a Binary Search problem?

```
Answer:

```

**(b)** What is your search space? What are the low and high boundaries?

```
Low  = 
High = 
Why?
```

**(c)** What is your feasibility check function? What question does it answer?

```
Function checks:

```

---

**Step 3 — Implement the Feasibility Check** _(20 marks)_

```kotlin
// Returns true if we can place 'cows' cows with at least 'minDist' gap between each
fun canPlace(stalls: IntArray, cows: Int, minDist: Int): Boolean {



}
```

**Dry run the feasibility check:**

```
stalls = [1, 2, 4, 8, 9], cows = 3, minDist = 3

Trace:



Result (true/false):
```

---

**Step 4 — Implement the Full Solution** _(30 marks)_

```kotlin
fun aggressiveCows(stalls: IntArray, cows: Int): Int {



}
```

---

**Step 5 — Full Dry Run** _(15 marks)_

```
Input: stalls = [1, 2, 8, 4, 9], cows = 3

Step 1 — Sort stalls:

Step 2 — Binary search iterations:
  low = ___, high = ___

  Iteration 1: mid = ___, canPlace? ___ → move low/high
  Iteration 2: mid = ___, canPlace? ___ → move low/high
  Iteration 3: mid = ___, canPlace? ___ → move low/high
  ...

Final answer:
```

---

**Step 6 — Complexity Analysis** _(10 marks)_

```
Time Complexity (explain each part):
- Sorting:        O(      )
- Binary Search:  O(      )
- canPlace check: O(      )
- Total:          O(      )

Space Complexity: O(      )
```

---

**Step 7 — Edge Cases** _(10 marks)_

Handle and explain each:

**(a)** Only 1 cow — what is the answer?

```
Answer:

```

**(b)** Number of cows equals number of stalls

```
Answer:

```

**(c)** All stalls at the same position

```
Answer:

```

**(d)** Stalls are already sorted vs unsorted — does it affect correctness?

```
Answer:

```

---

**Step 8 — Follow-up Variations** _(5 marks)_

**(a)** What if you want to **minimise** the maximum distance instead (opposite problem)?

```
Answer:

```

**(b)** Name 2 other problems that use the same "Binary Search on Answer" pattern.

```
1.
2.
```

---

## FINAL SCORECARD

|Section|Description|Max Marks|Your Score|
|---|---|---|---|
|Step 1|Clarifying Questions|5||
|Step 2|Pattern Identification|5||
|Step 3|Feasibility Check|20||
|Step 4|Full Solution|30||
|Step 5|Dry Run|15||
|Step 6|Complexity Analysis|10||
|Step 7|Edge Cases|10||
|Step 8|Follow-up Variations|5||
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

### Binary Search on Answer (Must-Know Pattern)

- [ ] Aggressive Cows — this problem
- [ ] Book Allocation Problem (similar pattern)
- [ ] Painter's Partition Problem
- [ ] Koko Eating Bananas (LC 875)
- [ ] Capacity to Ship Packages (LC 1011)
- [ ] Minimum Number of Days to Make m Bouquets (LC 1482)
- [ ] Find the Smallest Divisor (LC 1283)

### Key Insight to Memorise

> "If you can check feasibility of an answer in O(n), and the answer space is monotonic, use Binary Search on Answer."

### Practice Under Pressure

- [ ] Solve Aggressive Cows in under 25 minutes with all edge cases
- [ ] Solve Book Allocation blindfolded (without hints)
- [ ] Practice writing the feasibility function first, then wrap it in binary search

---

_Template based on real Zeta Android Developer interview experience by Gaurav — Apr 2025._