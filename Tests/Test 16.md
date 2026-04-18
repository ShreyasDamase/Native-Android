# PAYTM — SENIOR ANDROID DEVELOPER

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
> 1. This exam has **4 Rounds + 1 Coding Round** matching the actual Paytm Senior Android process.
> 2. Use a strict timer per round. Do not exceed limits.
> 3. Write full working code — not pseudocode.
> 4. Self-evaluate at the end of each round.

---

---

## CODING ROUND — DSA SCREENING

**Time Allowed:** 60 Minutes      **Marks:** 15

---

### Problem 1 — Spiral Matrix Traversal (25 min)

Given an m×n matrix, return all elements in spiral order.

**Example:**

```
Input:
[[ 1, 2, 3],
 [ 4, 5, 6],
 [ 7, 8, 9]]

Output: [1,2,3,6,9,8,7,4,5]
```

**Approach:**

```
Approach (4 boundaries — top, bottom, left, right):


```

**Code:**

```kotlin
fun spiralOrder(matrix: Array<IntArray>): List<Int> {



}
```

**Dry Run:**

```
Matrix: [[1,2,3],[4,5,6],[7,8,9]]

Trace boundaries:



Output:
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|

_(7 marks)_

---

### Problem 2 — Longest Increasing Subsequence (35 min)

Given an integer array, return the length of the longest strictly increasing subsequence.

**Example:**

```
Input:  nums = [10,9,2,5,3,7,101,18]
Output: 4  ([2,3,7,101])
```

**Step 1 — Approach 1: DP O(n²)**

```
Approach:

```

```kotlin
fun lisDP(nums: IntArray): Int {



}
```

**Step 2 — Approach 2: Binary Search O(n log n)**

```
Approach (patience sorting / tails array):

```

```kotlin
fun lisBinarySearch(nums: IntArray): Int {



}
```

**Dry Run:**

```
Input: [10,9,2,5,3,7,101,18]

DP array trace:



Tails array trace:



Output:
```

||DP|Binary Search|
|---|---|---|
|Time Complexity|O( )|O( )|
|Space Complexity|O( )|O( )|

_(8 marks)_

---

**Coding Round Self Score:** _____ / 15

---

---

## ROUND 1 — DSA INTERVIEW

**Time Allowed:** 45 Minutes      **Marks:** 15

---

### Problem 1 — Longest Line of 1s in a 2D Binary Array (20 min)

Given a 2D binary array (only 0s and 1s), find the longest line of consecutive 1s — horizontally or vertically.

**Example:**

```
Array:
4 4
0 1 1 1
0 1 0 1
0 1 1 0
0 1 0 1

Longest horizontal line: 3 (row 0: [1,1,1])
Longest vertical line:   4 (col 1: all 1s)
Output: 4
```

**Approach:**

```
Approach:


```

**Code:**

```kotlin
fun longestLine(matrix: Array<IntArray>): Int {



}
```

**Dry Run:**

```
Trace horizontal scan:

Trace vertical scan:

Output:
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|

_(7 marks)_

---

### Problem 2 — Number of Islands (25 min)

Given a 2D binary grid of '1's (land) and '0's (water), count the number of islands.

**Example:**

```
Input:
[["1","1","0","0","0"],
 ["1","1","0","0","0"],
 ["0","0","1","0","0"],
 ["0","0","0","1","1"]]

Output: 3
```

**Approach (BFS or DFS — justify):**

```
Approach:


```

**Code:**

```kotlin
fun numIslands(grid: Array<CharArray>): Int {



}
```

**Dry Run:**

```
Trace island discovery:



Output:
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|

_(8 marks)_

---

**Round 1 Self Score:** _____ / 15

---

---

## ROUND 2 — ANDROID + JAVA + DSA MIX

**Time Allowed:** 45 Minutes      **Marks:** 20

---

### Part A — Android & Java Basics (20 min)

**Q1.** What is the difference between `RecyclerView` and `ListView`? Why is RecyclerView preferred? _(3 marks)_

```
Answer:



```

**Q2.** What is a `Fragment`? Explain the Fragment lifecycle and how it relates to the Activity lifecycle. _(3 marks)_

```
Answer:



```

**Q3.** What are `Services` in Android? Difference between `Service`, `Foreground Service`, and `Bound Service`? _(3 marks)_

```
Answer:



```

**Q4.** Java threading basics — what is `synchronized`? What is the difference between `Thread` and `Runnable`? _(3 marks)_

```
Answer:



```

---

### Part B — DSA Problems (25 min)

**Problem 1 — Sort Array of 0s, 1s and 2s (Dutch National Flag)** (10 min)

```
Input:  [0,1,2,0,1,2,1,0]
Output: [0,0,0,1,1,1,2,2]
```

**Code (must be O(n) time, O(1) space):**

```kotlin
fun sortColors(nums: IntArray) {



}
```

|Time Complexity|O( )|Space Complexity|O( )|
|---|---|---|---|

_(4 marks)_

---

**Problem 2 — Check if String is Palindrome** (15 min)

**(a)** Basic check (ignore case and non-alphanumeric):

```kotlin
fun isPalindrome(s: String): Boolean {



}
```

**(b)** Follow-up: Valid Palindrome II — can you remove at most 1 character to make it a palindrome?

```kotlin
fun validPalindrome(s: String): Boolean {



}
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|

_(4 marks)_

---

**Round 2 Self Score:** _____ / 20

---

---

## ROUND 3 — SYSTEM DESIGN + JAVA INTERNALS

**Time Allowed:** 45 Minutes      **Marks:** 20

---

### Part A — System Design (25 min)

**Q1.** Design the architecture of **Paytm Marketplace** — a platform where buyers browse products, add to cart, and make payments. Cover: client, API layer, database, and key services. _(8 marks)_

```
Key Services:
1.
2.
3.
4.

Architecture Description:




Database choices (SQL vs NoSQL — justify per service):




API design (key endpoints):


```

**Q2.** How would you handle payment failures and ensure idempotency in the payment service? _(4 marks)_

```
Answer:



```

---

### Part B — Java Internals (20 min)

**Q3.** Why does `Hashtable` not allow `null` keys or values, but `HashMap` does? _(4 marks)_

```
Answer:



```

**Q4.** Java Collections — explain the internal working of `HashMap`. What happens during a hash collision? What is the load factor? _(4 marks)_

```
Answer:




```

---

**Round 3 Self Score:** _____ / 20

---

---

## ROUND 4 — ANDROID DEEP DIVE + DSA + PATTERNS

**Time Allowed:** 60 Minutes      **Marks:** 30

---

### Part A — Design Patterns & Data Structures (25 min)

**Q1.** Implement the **Singleton Pattern** in Java/Kotlin — thread-safe version. _(3 marks)_

```kotlin
// Thread-safe Singleton:



```

**Q2.** Design your own **ArrayList** — implement `add()`, `get()`, `remove()`, and dynamic resizing. _(5 marks)_

```kotlin
class MyArrayList<T> {
    private var data: Array<Any?> = arrayOfNulls(4)
    private var size = 0

    fun add(element: T) {



    }

    fun get(index: Int): T {



    }

    fun remove(index: Int) {



    }

    private fun resize() {



    }
}
```

**Q3.** Sort a Linked List (merge sort approach). _(4 marks)_

```kotlin
class ListNode(var `val`: Int, var next: ListNode? = null)

fun sortList(head: ListNode?): ListNode? {



}
```

**Q4.** Array Rotation — rotate array to the right by K steps. _(3 marks)_

```kotlin
fun rotate(nums: IntArray, k: Int) {



}
```

**Q5.** LRU Cache — implement with O(1) get and put. _(5 marks)_

```kotlin
class LRUCache(private val capacity: Int) {

    // Use LinkedHashMap or custom doubly linked list + hashmap



    fun get(key: Int): Int {



    }

    fun put(key: Int, value: Int) {



    }
}
```

---

### Part B — Android Advanced (20 min)

**Q6.** What is `retainInstance()` in Fragments (now deprecated)? What problem did it solve and what replaced it? _(3 marks)_

```
Answer:



```

**Q7.** What is the `AsyncTask` rotation problem? How do you solve it in modern Android? _(3 marks)_

```
Answer:



```

**Q8.** App optimisation — name 5 concrete techniques to optimise an Android app's performance. _(3 marks)_

```
1.
2.
3.
4.
5.
```

---

### Part C — DSA (15 min)

**Q9.** Graph traversal — detect a cycle in a directed graph (circular loop). _(4 marks)_

```kotlin
fun hasCycle(graph: Map<Int, List<Int>>): Boolean {
    // Use DFS with visited + recursion stack



}
```

**Q10.** Find the maximum of a stack in O(1) time. _(4 marks)_

```kotlin
class MaxStack {
    // Design a stack that supports push, pop, and getMax in O(1)



    fun push(x: Int) {}
    fun pop(): Int { return 0 }
    fun getMax(): Int { return 0 }
}
```

---

**Round 4 Self Score:** _____ / 30

---

---

## FINAL SCORECARD

|Round|Description|Max Marks|Your Score|
|---|---|---|---|
|Coding Round|Spiral Matrix + LIS|15||
|Round 1|DSA — Longest Line + Number of Islands|15||
|Round 2|Android/Java Basics + Sort Colors + Palindrome|20||
|Round 3|System Design + Java Internals|20||
|Round 4|Patterns, Data Structures, Android Deep Dive, DSA|30||
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

- [ ] Spiral matrix traversal — 4-boundary approach
- [ ] LIS — both O(n²) DP and O(n log n) binary search
- [ ] Longest line of 1s — horizontal + vertical scan
- [ ] Number of Islands — BFS and DFS
- [ ] Dutch National Flag — sort 0s, 1s, 2s in O(n)
- [ ] Palindrome check + Valid Palindrome II
- [ ] Linked List sort — merge sort approach
- [ ] Array rotation — reversal trick O(n) O(1)
- [ ] LRU Cache — HashMap + doubly linked list
- [ ] Max Stack — auxiliary stack approach
- [ ] Cycle detection in directed graph — DFS + recursion stack
- [ ] Find missing number in array

### Android

- [ ] RecyclerView vs ListView — ViewHolder pattern, DiffUtil, RecycledViewPool
- [ ] Fragment lifecycle — all callbacks, relationship to Activity
- [ ] Service vs Foreground Service vs Bound Service vs WorkManager
- [ ] retainInstance() — what it was, ViewModel as replacement
- [ ] AsyncTask rotation problem — ViewModel + Coroutines fix
- [ ] App optimisation — overdraw, memory, hierarchy, lazy loading, ProGuard

### Java / Kotlin

- [ ] HashMap internals — hashing, collision (chaining/open addressing), load factor, rehashing
- [ ] Hashtable vs HashMap — null handling, synchronisation
- [ ] Thread vs Runnable, synchronized keyword, volatile
- [ ] Java Collections — ArrayList, LinkedList, HashMap, TreeMap, PriorityQueue internals

### Design Patterns

- [ ] Singleton — double-checked locking, Kotlin object
- [ ] Observer, Factory, Builder, Repository
- [ ] Implement ArrayList from scratch

### System Design

- [ ] Paytm Marketplace — product catalogue, cart, payment, notifications
- [ ] Payment idempotency — idempotency keys, retry logic
- [ ] SQL vs NoSQL trade-offs for different services

---

_Template based on real Paytm Senior Android Developer interview experience — GeeksForGeeks Set 14._