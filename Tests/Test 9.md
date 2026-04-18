# PHONEPE — ANDROID ENGINEER

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
> 1. This exam has **4 Rounds** — all elimination rounds, matching the actual PhonePe Android process.
> 2. Use a strict timer per round. Do not exceed limits.
> 3. For coding questions, write full working code — not pseudocode.
> 4. Be honest — only answer what you truly know. Depth > breadth.
> 5. Self-evaluate at the end of each round.

---

---

## ROUND 1 — TECHNICAL ROUND (Android Deep Dive)

**Time Allowed:** 60 Minutes      **Marks:** 30

> Pure Android internals. You need to explain how things work under the hood — not just what they do.

---

### Fragments & Activities

**Q1.** Explain the Fragment lifecycle in relation to its host Activity. What happens to the Fragment when the Activity is stopped? _(3 marks)_

```
Answer:




```

**Q2.** What are the best practices for communicating between a Fragment and its Activity? Why should Fragments not hold a direct reference to the Activity? _(2 marks)_

```
Answer:


```

---

### ViewModel & State Management

**Q3.** How does ViewModel survive a configuration change (like screen rotation)? What is `ViewModelStore` and who owns it? _(3 marks)_

```
Answer:




```

**Q4.** What is the difference between `StateFlow`, `SharedFlow`, and `LiveData`? When would you choose each? _(3 marks)_

```
Answer:




```

---

### Coroutines & Flow

**Q5.** What is structured concurrency in Kotlin Coroutines? What happens to child coroutines if the parent is cancelled? _(3 marks)_

```
Answer:



```

**Q6.** How do you handle exceptions in coroutines? What is the difference between `CoroutineExceptionHandler`, `try/catch`, and `supervisorScope`? _(3 marks)_

```kotlin
// Write a code example showing proper exception handling in coroutines:



```

---

### Dependency Injection — Dagger & Hilt

**Q7.** What is the lifecycle of a `@Singleton` scoped dependency in Hilt? How does Hilt know when to create and destroy it? _(2 marks)_

```
Answer:


```

**Q8.** What is the difference between `@ActivityScoped` and `@ViewModelScoped` in Hilt? _(2 marks)_

```
Answer:


```

---

### KSP & KAPT

**Q9.** What is the difference between KSP and KAPT? Why is KSP faster? _(2 marks)_

```
Answer:


```

---

### Jetpack Compose

**Q10.** What triggers a recomposition in Jetpack Compose? How does the Compose compiler decide what to skip recomposing? _(3 marks)_

```
Answer:



```

**Q11.** What is `remember` vs `rememberSaveable`? When does `remember` lose its value? _(2 marks)_

```
Answer:


```

---

### Memory & Security

**Q12.** How does Android's garbage collector work? What is the difference between a memory leak and an OOM error? Name two common causes of memory leaks in Android. _(2 marks)_

```
Answer:


```

---

**Round 1 Self Score:** _____ / 30

---

---

## ROUND 2 — DSA ROUND

**Time Allowed:** 60 Minutes      **Marks:** 25

> At least 2 problems. Medium to Hard difficulty. Solve fully if possible — if stuck, give a clear approach.

---

### Problem 1 — Dynamic Programming: Longest Palindromic Substring (30 min)

Given a string `s`, return the longest palindromic substring in `s`.

**Example:**

```
Input:  s = "babad"
Output: "bab"  (or "aba" — both valid)

Input:  s = "cbbd"
Output: "bb"
```

**Step 1 — Approach (3 min)**

```
Approach (DP table / expand around center — justify your choice):



```

**Step 2 — Code**

```kotlin
fun longestPalindrome(s: String): String {



}
```

**Step 3 — Dry Run**

```
Input: "babad"

Trace:



Output:
```

**Step 4 — Follow-up: Can you do this in O(n) using Manacher's Algorithm? Explain the idea.**

```
Answer:


```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|
|Handles single char?|Yes / No|
|Handles all same chars?|Yes / No|

_(12 marks — 8 solution + 4 follow-up)_

---

### Problem 2 — Graphs: BFS/DFS Problem (30 min)

**Number of Islands**

Given an m×n grid of '1's (land) and '0's (water), count the number of islands. An island is surrounded by water and formed by connecting adjacent lands horizontally or vertically.

**Example:**

```
Input:
grid = [
  ["1","1","0","0","0"],
  ["1","1","0","0","0"],
  ["0","0","1","0","0"],
  ["0","0","0","1","1"]
]
Output: 3
```

**Step 1 — Approach (2 min)**

```
Approach (BFS / DFS — justify your choice):


```

**Step 2 — Code**

```kotlin
fun numIslands(grid: Array<CharArray>): Int {



}
```

**Step 3 — Dry Run**

```
Input: (the grid above)

Trace:



Output:
```

**Step 4 — Follow-up: What if the grid is too large to fit in memory (distributed system)? How would you approach this?**

```
Answer:


```

|||
|---|---|
|Time Complexity|O( )|
|Space Complexity|O( )|
|Time Taken|___ min|
|Handles all water (output 0)?|Yes / No|
|Handles all land (output 1)?|Yes / No|

_(13 marks — 9 solution + 4 follow-up)_

---

**Round 2 Self Score:** _____ / 25

---

---

## ROUND 3 — MACHINE CODING ROUND (Android App)

**Time Allowed:** 90 Minutes      **Marks:** 25

> You will be given an API contract and asked to build a fully functional Android app. Focus on: clean architecture, MVVM/MVI, Clean Code, scalability, and best practices.

---

### Task: Build a News Feed App from API Contract

**API Contract (given):**

```
GET /api/news
Response:
{
  "articles": [
    {
      "id": "string",
      "title": "string",
      "description": "string",
      "imageUrl": "string",
      "publishedAt": "ISO8601 timestamp",
      "source": "string",
      "category": "TECHNOLOGY | SPORTS | BUSINESS | HEALTH"
    }
  ],
  "nextCursor": "string | null"
}
```

**Constraints:**

- Implement pagination using cursor
- Support filtering by category
- Cache articles for offline viewing
- Handle loading, error, and empty states

---

**Q1.** List all features you will implement and how you will prioritise them. _(2 marks)_

```
Must-have:
1.
2.
3.

Good-to-have:
1.
2.
```

---

**Q2.** Define your architecture and project structure. _(3 marks)_

```
Architecture (MVVM / MVI):

Package structure:
- data/
- domain/
- presentation/

Key classes in each layer:
```

---

**Q3.** Implement the data layer — API service, repository, and data model. _(6 marks)_

```kotlin
// === DATA MODEL ===
data class Article(...)

// === API SERVICE (Retrofit) ===
interface NewsApiService {



}

// === REPOSITORY ===
class NewsRepository(
    private val api: NewsApiService,
    private val dao: ArticleDao
) {



}
```

---

**Q4.** Implement the ViewModel with pagination and filtering. _(6 marks)_

```kotlin
class NewsViewModel(private val repository: NewsRepository) : ViewModel() {

    // State



    // Fetch news with cursor pagination



    // Filter by category



}
```

---

**Q5.** Describe your UI layer — how do you handle loading, error, and empty states in Compose or XML? _(4 marks)_

```kotlin
// UI State sealed class:



// Composable or XML description:



```

---

**Q6.** How would you extend this app to support: _(4 marks)_

**(a)** Push notifications for breaking news

```
Answer:

```

**(b)** A search feature with debounce

```kotlin
// Debounced search implementation:


```

---

**Round 3 Self Score:** _____ / 25

---

---

## ROUND 4 — HIRING MANAGER ROUND

**Time Allowed:** 45 Minutes      **Marks:** 20

> Mix of behavioural and an unexpected technical question. Frame all answers positively. Think out loud.

---

### Section A — Behavioural (25 min)

**Q1.** Walk me through your career journey. How did you get to where you are today? _(4 marks)_

```
Answer:




```

**Q2.** What is the most challenging technical problem you have tackled in your career? How did you solve it? _(4 marks)_

```
Answer (use STAR format — Situation, Task, Action, Result):




```

**Q3.** What are your strengths and weaknesses? For your weakness, how are you working to improve it? _(3 marks)_

```
Strength:


Weakness + how you're improving it:


```

**Q4.** How do you stay updated with the latest Android and industry trends? Name 2–3 recent Android updates that excite you. _(3 marks)_

```
Answer:



```

---

### Section B — Technical (Unexpected!) (20 min)

**Q5.** Design an SDK — you are building an Analytics SDK that third-party Android apps can integrate. Explain your thought process and approach. Do NOT write code — explain your design decisions. _(6 marks)_

**Key points to cover:**

**(a)** What is the public API surface of the SDK? What should developers be able to call?

```
Answer:


```

**(b)** How do you batch and flush events to avoid constant network calls?

```
Answer:


```

**(c)** How do you ensure the SDK does not block the main thread?

```
Answer:


```

**(d)** How do you handle the app being killed before events are flushed?

```
Answer:


```

**(e)** How do you version and maintain backward compatibility of the SDK?

```
Answer:


```

---

**Round 4 Self Score:** _____ / 20

---

---

## FINAL SCORECARD

|Round|Description|Max Marks|Your Score|
|---|---|---|---|
|Round 1|Technical — Android Deep Dive|30||
|Round 2|DSA — Palindrome DP + Graph BFS/DFS|25||
|Round 3|Machine Coding — News Feed Android App|25||
|Round 4|Hiring Manager — Behavioural + SDK Design|20||
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

### Android Technical

- [ ] Fragment & Activity lifecycle — draw from memory, including edge cases
- [ ] ViewModel internals — ViewModelStore, ViewModelProvider, config changes
- [ ] Coroutines — structured concurrency, exception handling, SupervisorJob
- [ ] StateFlow vs SharedFlow vs LiveData — differences and use cases
- [ ] Hilt scopes — Singleton, ActivityScoped, ViewModelScoped
- [ ] KSP vs KAPT — why KSP is faster (no stub generation)
- [ ] Jetpack Compose — recomposition triggers, `remember` vs `rememberSaveable`
- [ ] Memory management — GC, memory leaks, LeakCanary
- [ ] Security — encryption, secure storage, certificate pinning

### DSA (LeetCode + Resources below)

- [ ] Dynamic Programming — 1D and 2D DP, Longest Palindromic Substring, LCS, Knapsack
- [ ] Graphs — BFS, DFS, number of islands, bipartite check, shortest path
- [ ] Binary Search — standard + rotated arrays + search space problems
- [ ] Two Pointers & Sliding Window
- [ ] Backtracking — permutations, subsets, N-Queens
- [ ] Solve 1 Medium + 1 Hard per day under timed conditions

### Machine Coding

- [ ] Build a full app with Clean Architecture + MVVM in under 90 minutes
- [ ] Practice: News Feed, Weather App, Movie Listing, E-Commerce Product List
- [ ] Master Retrofit + Room + Hilt + Coroutines + Paging3
- [ ] Study design patterns: Refactoring.Guru, NeetCode Course

### System Design / SDK Design

- [ ] Understand how Analytics SDKs work (batching, flushing, persistence)
- [ ] Design a Crash Reporting SDK
- [ ] Design a Feature Flag SDK
- [ ] Study: Gaurav Sen Course, ByteByteGo, Exponent

### Behavioural

- [ ] Prepare your career journey as a 2-minute structured story
- [ ] Prepare STAR answers for: biggest challenge, failure, conflict, achievement
- [ ] Research PhonePe — UPI infrastructure, Indus App Store, scale
- [ ] Never lie on your resume — depth over breadth

### Recommended Resources

- **DSA:** Kunal Kushwaha (foundations), NeetCode (patterns), TUF/Striver (DP)
- **Machine Coding / LLD:** Refactoring.Guru, NeetCode Course, Gaurav Sen Course
- **System Design:** Gaurav Sen Course, Exponent, ByteByteGo

---

_Template based on real PhonePe Android Engineer interview experience by Sahil Thakar — March 2025._