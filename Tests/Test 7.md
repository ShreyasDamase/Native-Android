# MEESHO SDE 1 — OPEN CAMPUS 2025

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
> 1. This exam has **4 Rounds** matching the actual Meesho Open Campus 2025 SDE-1 process.
> 2. Use a strict timer for each round. Do not exceed time limits.
> 3. For coding questions, write full working code — not pseudocode.
> 4. Self-evaluate your score at the end of each round.
> 5. After completing, share this sheet with a peer for review and feedback.

---

## ROUND 1 — ONLINE ASSESSMENT (HackerEarth)

**Time Allowed:** 165 Minutes      **Marks:** 25

> 3 problems: 2 Medium + 1 Hard. Solve in order. Manage your time carefully.

---

### Problem 1 — MEDIUM (35 min)

**Longest Subarray with Equal 0s and 1s**

Given a binary array, find the maximum length of a subarray with an equal number of 0s and 1s.

**Example:**

```
Input:  arr = [0, 1, 0, 1, 1, 0, 0]
Output: 6  (subarray [0,1,0,1,1,0])
```

**Step 1 — Approach (3 min)**

```
Approach:



```

**Step 2 — Code**

```kotlin
fun findMaxLength(nums: IntArray): Int {



}
```

**Step 3 — Dry Run**

```
Input: [0, 1, 0, 1, 1, 0, 0]

Trace:



Output:
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|
|All test cases pass?|Yes / No|

_(8 marks)_

---

### Problem 2 — MEDIUM (35 min)

**Rotting Oranges (BFS on Grid)**

You are given an m×n grid where 0 = empty, 1 = fresh orange, 2 = rotten orange. Every minute, a rotten orange infects adjacent (4-directional) fresh ones. Return the minimum minutes to rot all oranges, or -1 if impossible.

**Example:**

```
Input:  grid = [[2,1,1],[1,1,0],[0,1,1]]
Output: 4
```

**Step 1 — Approach (3 min)**

```
Approach:



```

**Step 2 — Code**

```kotlin
fun orangesRotting(grid: Array<IntArray>): Int {



}
```

**Step 3 — Dry Run**

```
Input: [[2,1,1],[1,1,0],[0,1,1]]

Trace (minute by minute):



Output:
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|
|Handles impossible case (-1)?|Yes / No|

_(8 marks)_

---

### Problem 3 — HARD (60 min)

**Minimum Cost to Reach Destination with At Most K Stops (Modified Dijkstra / Bellman-Ford)**

There are n cities connected by flights. You are given flights[i] = [from, to, price]. Find the cheapest price from src to dst with at most k stops. Return -1 if no such route exists.

**Example:**

```
Input:  n=4, flights=[[0,1,100],[1,2,100],[2,3,100],[0,2,500]], src=0, dst=3, k=1
Output: -1  (cannot reach 3 with at most 1 stop)
```

**Step 1 — Approach (5 min)**

```
Approach:



```

**Step 2 — Code**

```kotlin
fun findCheapestPrice(n: Int, flights: Array<IntArray>, src: Int, dst: Int, k: Int): Int {



}
```

**Step 3 — Dry Run**

```
Input: n=4, flights=[[0,1,100],[1,2,100],[2,3,100],[0,2,500]], src=0, dst=3, k=2

Trace:



Output:
```

**Step 4 — Edge Cases**

```
No path exists:

k = 0 (direct flight only):
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|
|Handles k=0?|Yes / No|
|Handles no path?|Yes / No|

_(9 marks)_

---

**Round 1 Self Score:** _____ / 25

---

---

## ROUND 2 — TECHNICAL INTERVIEW (DSA — Bipartite Graph)

**Time Allowed:** 30 Minutes      **Marks:** 20

> Interviewer is present throughout. Think aloud. Discuss edge cases before coding.

---

**Q1.** What is a Bipartite Graph? How do you check if a graph is bipartite? _(3 marks)_

```
Answer:



```

---

**Q2.** Implement the bipartite check using BFS (2-coloring approach). _(8 marks)_

**Step 1 — Clarifying Questions (write 2 you would ask)**

```
1.
2.
```

**Step 2 — Approach**

```
Approach:



```

**Step 3 — Code**

```kotlin
fun isBipartite(graph: Array<IntArray>): Boolean {



}
```

**Step 4 — Dry Run**

```
Input: graph = [[1,3],[0,2],[1,3],[0,2]]
(0-1, 0-3, 1-2, 2-3 — a cycle of 4 nodes)

Trace:



Output:
```

---

**Q3.** Follow-up: Can you also solve this using DFS? What changes? _(5 marks)_

```kotlin
// DFS-based bipartite check:



```

---

**Q4.** Follow-up 2: What is the time and space complexity? What if the graph is disconnected? _(4 marks)_

```
Time Complexity:

Space Complexity:

Disconnected graph handling:
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Handles disconnected graph?|Yes / No|
|Time Taken|___ min|

---

**Round 2 Self Score:** _____ / 20

---

---

## ROUND 3 — MACHINE CODING (LLD — Car Pooling System)

**Time Allowed:** 60 Minutes      **Marks:** 30

> Build a working Car Pooling System. Evaluator checks: class design, OOP, code cleanliness, execution, and edge case handling.

---

### Requirements Gathering (5 min)

**Q1.** List the entities, operations, and constraints you identify. _(3 marks)_

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

Constraints:
1.
2.
```

---

### Class Design (8 min)

**Q2.** Describe or draw your class diagram — classes, attributes, methods, and relationships. _(5 marks)_

```
Class Diagram / Description:




```

---

### Implementation (40 min)

**Q3.** Implement the full Car Pooling System in Kotlin. Write clean, compilable code. _(18 marks)_

```kotlin
// === ENUMS ===
// e.g., TripStatus, DriverStatus



// === DATA CLASSES / MODELS ===
// Location, Trip, Passenger



// === DRIVER CLASS ===



// === CAR / VEHICLE CLASS ===



// === POOLING MANAGER (Core Logic) ===
// - Book a seat
// - Complete a trip
// - Cancel a booking
// - Find available cars for route



// === MAIN / DEMO ===
fun main() {
    // Demonstrate: add drivers, book seats, pool multiple passengers, complete trip



}
```

---

### Design Questions (7 min)

**Q4.** Answer the following: _(4 marks)_

**(a)** Which design patterns did you use and why?

```
Answer:


```

**(b)** How would you extend this to support dynamic pricing (surge pricing during peak hours)?

```
Answer:


```

---

### Self-Evaluation Checklist

|Criterion|Done?|
|---|---|
|All entities modelled as classes|Yes / No|
|Pooling logic (multiple passengers per car) implemented|Yes / No|
|Car capacity constraints respected|Yes / No|
|Trip status updates correctly|Yes / No|
|Edge cases handled (full car, no driver, cancellation)|Yes / No|
|Code is clean and well-named|Yes / No|
|At least one design pattern applied|Yes / No|

---

**Round 3 Self Score:** _____ / 30

---

---

## ROUND 4 — HR + TECHNICAL FINAL ROUND

**Time Allowed:** 30 Minutes      **Marks:** 25

> Mix of behavioural, CS fundamentals, and project discussion.

---

### Section A — Projects & Behavioural (12 min)

**Q1.** Walk me through your most significant project or internship. What problem did you solve, what was your role, and what was the impact? _(5 marks)_

```
Answer:




```

---

**Q2.** Tell me about a time you handled failure — academic, project, or team. What did you do? _(4 marks)_

```
Answer:




```

---

**Q3.** Why Meesho? What about the company or its mission excites you? _(3 marks)_

```
Answer:



```

---

### Section B — CS Fundamentals (12 min)

**Q4.** Operating Systems — answer the following: _(4 marks)_

**(a)** What is the difference between a Process and a Thread?

```
Answer:


```

**(b)** What is deadlock? What are the four necessary conditions for it?

```
Answer:


```

---

**Q5.** DBMS & SQL — answer the following: _(4 marks)_

**(a)** What is the difference between INNER JOIN, LEFT JOIN, and FULL OUTER JOIN?

```
Answer:


```

**(b)** Write a SQL query to find the second-highest salary from an `employees` table.

```sql
-- Query:



```

---

**Q6.** Computer Networks — answer the following: _(4 marks)_

**(a)** What happens when you type `www.meesho.com` in a browser and hit Enter?

```
Answer:



```

**(b)** What is the difference between TCP and UDP? When would you use UDP?

```
Answer:


```

---

### Section C — Closing (6 min)

**Q7.** Do you have any questions for us? Write 2–3 thoughtful questions. _(5 marks)_

```
Questions:
1.

2.

3.
```

---

**Round 4 Self Score:** _____ / 25

---

---

## FINAL SCORECARD

|Round|Description|Max Marks|Your Score|
|---|---|---|---|
|Round 1|Online Assessment — 2 Medium + 1 Hard|25||
|Round 2|Technical Interview — Bipartite Graph|20||
|Round 3|Machine Coding — Car Pooling System (LLD)|30||
|Round 4|HR + Technical — Projects, CS Fundamentals|25||
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

### DSA (Striver's SDE Sheet Recommended)

- [ ] Arrays & Strings — sliding window, prefix sum, hashing
- [ ] Linked Lists — cycle detection, reversal, merge
- [ ] Trees — BFS, DFS, LCA, diameter
- [ ] Graphs — BFS, DFS, Bipartite check, Dijkstra, Bellman-Ford, Topological Sort
- [ ] Dynamic Programming — knapsack, LCS, matrix DP
- [ ] Solve 1 Hard problem per day under timed conditions (60 min max)

### Low Level Design (Code With Aryan LLD Series Recommended)

- [ ] Implement Car Pooling System from scratch
- [ ] Implement Parking Lot System
- [ ] Implement Cab Booking System (Uber/Ola)
- [ ] Implement Library Management System
- [ ] Know and apply: Strategy, Observer, Factory, Singleton, Decorator patterns
- [ ] SOLID principles with real examples
- [ ] Practice writing full working code in 60 minutes

### CS Fundamentals

- [ ] OS: Processes vs Threads, Scheduling, Deadlock, Memory Management, Virtual Memory
- [ ] DBMS: Normalization, Transactions, ACID, Indexing, SQL Joins, Query Optimization
- [ ] CN: OSI Model, TCP/IP, HTTP vs HTTPS, DNS, what happens on browser request

### Behavioural

- [ ] Prepare 3 project/internship stories in STAR format
- [ ] Prepare "handle a failure" story honestly
- [ ] Research Meesho's mission, products, and recent news
- [ ] Prepare 3 thoughtful questions for the interviewer

---

_Template based on real Meesho SDE-1 Open Campus 2025 interview experience shared by Thota Adinarayana._