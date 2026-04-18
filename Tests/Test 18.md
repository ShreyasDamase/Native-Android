# GROWW SDE 1 — ANDROID DEVELOPER

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
> 1. This exam has **2 Rounds** matching the actual Groww interview process.
> 2. Use a timer strictly. Do not exceed the time limit per round.
> 3. Write your answers in the blank spaces provided below each question.
> 4. For coding questions, write full working code — not just pseudocode.
> 5. Self-evaluate your score at the end of each round.
> 6. After completing, share this sheet with a peer for feedback.

---

---

## ROUND 1 — DSA ROUND

**Time Allowed:** 60 Minutes  **Marks:** 40

---

### PROBLEM 1 — Substring Matching  _(15 marks)_

**Time Limit: 15 minutes**

**Problem Statement:** How would you check if a string `pattern` is a substring of another string `text`? _(Assume max string length = 100 characters for Part A, and 10⁵ characters for Part B.)_

---

**Part A — Brute Force Approach (max length = 100)**

**Step 1 — Write your approach FIRST (2 min)**

```
Approach:
1.
2.
3.
```

**Step 2 — Write working code**

```kotlin
fun isSubstring(text: String, pattern: String): Boolean {



}
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|

---

**Part B — Follow-up: What if text length is 10⁵? (KMP Algorithm)**

**Step 1 — Explain the KMP algorithm**

```
Explanation:
1.
2.
3.
```

**Step 2 — Write KMP code**

```kotlin
fun kmpSearch(text: String, pattern: String): Boolean {



}
```

**Step 3 — Dry run with example**

```
text    = "ababcabcabababd"
pattern = "ababd"

LPS Array trace:


Search trace:


Expected Output: true
Your Output:
```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|
|Code complete?|Yes / No|
|Correct output on example?|Yes / No|

---

### PROBLEM 2 — Warm-Up / Follow-Up Problem  _(10 marks)_

**Time Limit: 10 minutes**

_The interviewer may assign a second DSA problem at this point. Write the problem you were given, then solve it._

**Problem chosen / given:**

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
|Finished within time limit?|Yes / No|

---

### DSA Self Score: _____ / 25

---

---

## ROUND 1 — ANDROID CORE CONCEPTS (Same Round, Continued)

**Time Allowed:** 30 Minutes  **Marks:** 75 (15 marks × 5 = 75, normalised to /75)

_Answer each question clearly. Aim for depth over breadth._

---

**Q1.** What design patterns have you used in Android? Why would you choose Clean Architecture? _(Discuss MVVM, Repository Pattern, Singleton, Observer / LiveData)_ _(8 marks)_

```
Answer:




```

---

**Q2.** What are ViewModels and why are they used? How does a ViewModel survive recomposition / configuration changes? _(8 marks)_

```
Answer:




```

---

**Q3.** What are coroutines and suspend functions in Kotlin? How do they differ from threads? _(8 marks)_

```
Answer:




```

---

**Q4.** What are extension functions in Kotlin? How do they work internally (at the bytecode level)? _(8 marks)_

```
Answer:




```

**Write an example extension function:**

```kotlin
// Example:



```

---

**Q5.** How is data shared between Fragments? Explain the Shared ViewModel approach. _(6 marks)_

```
Answer:




```

---

**Q6.** How can two Android applications communicate with each other? _(Discuss AIDL, Intents, Content Providers)_ _(6 marks)_

```
Answer:




```

---

**Q7.** What are inline functions in Kotlin? When and why would you use them? _(6 marks)_

```
Answer:




```

---

**Q8.** How does Kotlin ensure null safety? What is the difference between `?.`, `!!`, and `?:`? _(6 marks)_

```
Answer:




```

---

**Q9.** What are the different types of Services in Android? Explain the difference between Bound Service and IntentService. _(6 marks)_

```
Answer:




```

---

**Q10.** When would you use WorkManager? How is it different from JobScheduler? _(8 marks)_

```
Answer:




```

---

**Android Core Self Score:** _____ / 75

---

---

## FINAL SCORECARD

|Section|Description|Max Marks|Your Score|
|---|---|---|---|
|Round 1 — DSA|Substring Matching + Follow-Up|25||
|Round 1 — Android|Core Concept Questions (10 Qs)|75||
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

**Peer / Friend Feedback:** _(Ask someone to review your code & answers)_

---

## PREPARATION CHECKLIST

Use this after completing the exam to guide your next steps.

### DSA

- [ ] Implement KMP from scratch without hints in under 20 minutes
- [ ] Dry-run KMP on at least 3 custom examples
- [ ] Solve 1 string-based problem daily with strict timer

### Android & Kotlin

- [ ] Explain Clean Architecture layers (Data / Domain / Presentation) to a friend
- [ ] Build a mini app using MVVM + Repository pattern
- [ ] Write 3 custom Extension Function examples from scratch
- [ ] Explain `viewModelScope` and how ViewModel survives configuration changes
- [ ] Revise coroutines — `launch`, `async`, `suspend`, `withContext`
- [ ] Explain null safety operators (`?.`, `!!`, `?:`, `let`) with examples
- [ ] Compare Bound Service vs Started Service vs IntentService
- [ ] Compare WorkManager vs JobScheduler — list 3 differences
- [ ] Explain AIDL vs Intent vs Content Provider for IPC
- [ ] Explain inline functions and when they matter for performance

### Soft Skills

- [ ] Prepare a 60-second crisp self-introduction
- [ ] Practise asking one clarifying question before jumping to code

---

_Template based on real Groww SDE 1 Android interview experience — September 2025._	