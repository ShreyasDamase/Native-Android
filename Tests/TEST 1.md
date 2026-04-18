---

## title: Swiggy SDE 1 – Android Developer Interview type: practice-exam candidate_name: date: total_marks: 100 time_allowed: 3 Hours instructions: Attempt all sections in order. Set a timer per round. Do not skip ahead.

---

# SWIGGY SDE 1 — ANDROID DEVELOPER

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
> 1. This exam has **4 Rounds** matching the actual Swiggy interview process.
> 2. Use a timer strictly. Do not exceed the time limit per round.
> 3. Write your answers in the blank spaces provided below each question.
> 4. For coding questions, write full working code — not just pseudocode.
> 5. Self-evaluate your score at the end of each round.
> 6. After completing, share this sheet with a peer for feedback.

---

---

## ROUND 1 — RECRUITER SCREENING CALL

**Time Allowed:** 10 Minutes    **Marks:** 5

---

**Q1.** Briefly introduce yourself — mention your YOE, current role, and notice period.  
_(2 marks)_

```
Answer:




```

---

**Q2.** Why do you want to join Swiggy specifically? What excites you about this role?  
_(3 marks)_

```
Answer:




```

---

**Round 1 Self Score:**     _____ / 5

---

---

## ROUND 2 — MACHINE CODING ROUND (InterviewVector)

**Time Allowed:** 60 Minutes    **Marks:** 25

---

### SECTION A — System Design & Architecture   _(5 marks)_

**Problem Statement:**  
Design and build a **Library Management Application**.

Before coding, write your architecture plan below.

**Q1.** Which architecture pattern will you use and why?  
_(e.g., MVVM, MVI, Clean Architecture)_

```
Answer:



```

**Q2.** List the key components / classes you plan to create.

```
Components:
1.
2.
3.
4.
5.
```

**Q3.** What data layer approach will you use? (Room / In-memory / Repository pattern?)

```
Answer:


```

---

### SECTION B — Implementation   _(15 marks)_

**Task:** Write working Kotlin code for the Library Management App.

**Minimum requirements:**

- [ ] Add and remove books
- [ ] Search books by title or author
- [ ] Borrow a book (mark as unavailable)
- [ ] Return a book (mark as available)
- [ ] Track which user has borrowed which book

```kotlin
// ─────────────────────────────────────────
// LIBRARY MANAGEMENT — YOUR CODE BELOW
// ─────────────────────────────────────────




```

---

### SECTION C — Post-Coding Discussion   _(5 marks)_

**Q1.** How would you scale this app if it had to support 10,000 books and 500 concurrent users?

```
Answer:


```

**Q2.** What tradeoffs did you make in your current implementation?

```
Answer:


```

---

**Round 2 Self Score:**     _____ / 25

---

---

## ROUND 3 — DATA STRUCTURES & ALGORITHMS + ANDROID

**Time Allowed:** 60 Minutes (30 min DSA + 30 min Android)    **Marks:** 40

---

### PART A — DSA (30 Minutes)

---

#### Problem 1 — Warm-Up Easy Problem   _(5 marks)_

**Time Limit: 10 minutes**

_Use any easy LeetCode problem you haven't practised recently (e.g., Two Sum, Valid Parentheses, Best Time to Buy/Sell Stock). Write the problem you chose, then solve it._

**Problem chosen:**

```
Problem Statement:



```

**Your Solution:**

```kotlin
// CODE HERE



```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|
|Finished within 10 min?|Yes / No|

---

#### Problem 2 — Edit Distance   _(15 marks)_

**Time Limit: 20 minutes**   |   LeetCode #72

**Problem Statement:**  
Given two strings `word1` and `word2`, return the minimum number of operations (insert, delete, replace) required to convert `word1` to `word2`.

---

**Step 1 — Write your approach FIRST (2 min)**

```
Approach:
1.
2.
3.
4.
```

---

**Step 2 — Write working code (18 min)**

```kotlin
fun minDistance(word1: String, word2: String): Int {



}
```

---

**Step 3 — Dry run with example**

```
word1 = "horse"   word2 = "ros"

Dry run / DP table trace:




Expected Output: 3
Your Output:
```

---

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|
|Code complete?|Yes / No|
|Correct output on example?|Yes / No|

---

### PART B — ANDROID QUESTIONS (30 Minutes)

_Answer each question as clearly as possible. Aim to answer at least 4 out of 5._

---

**Q1.** What is the difference between `viewModelScope` and `lifecycleScope`? When would you use each?  
_(4 marks)_

```
Answer:



```

---

**Q2.** How does `StateFlow` differ from `LiveData`? Which would you prefer in a new project and why?  
_(4 marks)_

```
Answer:



```

---

**Q3.** Explain the difference between `launch` and `async` in Kotlin Coroutines. How do you handle exceptions in each?  
_(4 marks)_

```
Answer:



```

---

**Q4.** What is the RecyclerView `ViewHolder` pattern? Why is it important for performance?  
_(4 marks)_

```
Answer:



```

---

**Q5.** How does Dagger Hilt handle dependency scoping? Explain `@Singleton` vs `@ActivityScoped`.  
_(4 marks)_

```
Answer:



```

---

**Round 3 Self Score:**     _____ / 40

---

---

## ROUND 4 — HIRING MANAGER ROUND

**Time Allowed:** 60 Minutes    **Marks:** 30

---

### SECTION A — Resume & Current Role Discussion   _(5 marks)_

**Q1.** Pick one key project from your resume. Describe your role, the technical challenges, and the decisions you made.

```
Project Name:

Your Role:

Technical Challenge:

Decision Made & Why:


```

---

### SECTION B — Deep Android & Kotlin   _(15 marks)_

---

**Q1.** Explain Kotlin **Extension Functions**. Write an extension function that formats a Unix timestamp into a readable date string.  
_(5 marks)_

```
Explanation:



```

```kotlin
// Extension Function Code:



```

---

**Q2.** Explain **Dagger Hilt** — how does it generate code at compile time? Show a code example of providing a dependency that depends on another.  
_(5 marks)_

```
Explanation:



```

```kotlin
// Dagger Hilt Code Example:



```

---

**Q3.** Explain **Structured Concurrency** in Kotlin Coroutines. What is `SupervisorJob` and when would you use it?  
_(5 marks)_

```
Explanation:



```

```kotlin
// Code example (optional):



```

---

### SECTION C — DSA: Next Permutation   _(10 marks)_

**Time Limit: 10 minutes**   |   LeetCode #31

**Problem Statement:**  
Given an array of integers `nums`, rearrange it to the next lexicographically greater permutation. If no such arrangement is possible, rearrange as the lowest possible order (sorted ascending). Must be done **in-place**.

---

**Step 1 — Write your approach FIRST (2 min)**

```
Step 1:
Step 2:
Step 3:
Step 4:
```

---

**Step 2 — Write working code (8 min)**

```kotlin
fun nextPermutation(nums: IntArray): Unit {



}
```

---

**Step 3 — Dry run with example**

```
Input:  [1, 2, 3]
Expected Output: [1, 3, 2]

Your trace:


Input:  [3, 2, 1]
Expected Output: [1, 2, 3]

Your trace:

```

---

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|
|Code complete?|Yes / No|

---

**Round 4 Self Score:**     _____ / 30

---

---

## FINAL SCORECARD

|Round|Description|Max Marks|Your Score|
|---|---|---|---|
|Round 1|Recruiter Screening|5||
|Round 2|Machine Coding|25||
|Round 3 Part A|DSA|20||
|Round 3 Part B|Android Q&A|20||
|Round 4|Hiring Manager|30||
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

**Peer / Friend Feedback:** _(Ask someone to review your code & answers)_

---

## PREPARATION CHECKLIST

Use this after completing the exam to guide your next steps.

### DSA

- [ ] Solve Edit Distance (LC 72) without hints in under 15 minutes
- [ ] Solve Next Permutation (LC 31) without hints in under 10 minutes
- [ ] Solve 1 easy problem daily with strict 10-minute timer

### Android & Kotlin

- [ ] Write 3 custom Extension Function examples from scratch
- [ ] Build a small Dagger Hilt app with multiple scopes
- [ ] Explain Coroutines (launch, async, SupervisorJob) to a friend
- [ ] Revise StateFlow vs LiveData differences
- [ ] Revise MVVM + Repository pattern architecture

### Machine Coding

- [ ] Rebuild Library Management App in 45 minutes from scratch
- [ ] Practice explaining architecture decisions clearly

### Soft Skills

- [ ] Prepare a crisp "Why Swiggy?" answer (< 2 minutes)
- [ ] Prepare resume walkthrough for every bullet point

---

_Template based on real Swiggy SDE 1 Android interview experience — May 2025._