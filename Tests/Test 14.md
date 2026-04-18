# FLIPKART — SDE-2 ANDROID

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
> 1. This exam has **5 Rounds** matching the actual Flipkart SDE-2 Android process.
> 2. Use a strict timer per round. Do not exceed limits.
> 3. For coding questions, write full working code — not pseudocode.
> 4. Self-evaluate at the end of each round.

---

---

## ROUND 1 — TECHNICAL PHONE SCREEN

**Time Allowed:** 20 Minutes      **Marks:** 5

> Conversational. Discuss your background, role, and experience clearly and confidently.

---

**Q1.** Walk me through your background and what you've built so far. What is your current role and tech stack? _(2 marks)_

```
Answer:




```

**Q2.** What is the most technically challenging thing you've built? What trade-offs did you make? _(3 marks)_

```
Answer:




```

---

**Round 1 Self Score:** _____ / 5

---

---

## ROUND 2 — MACHINE CODING (Android App)

**Time Allowed:** 180 Minutes      **Marks:** 25

> Build a fully functional Android app using any technology. Evaluated on: functionality, architecture, clean code, and scalability. Use what you know best — don't over-experiment under time pressure.

---

### Task: Build a Product Listing App

You are given the following API contract. Build a working Android app.

**API Contract:**

```
GET /products?page={page}&category={category}
Response:
{
  "products": [
    {
      "id": "string",
      "name": "string",
      "price": number,
      "discountPercent": number,
      "imageUrl": "string",
      "rating": number,
      "category": "ELECTRONICS | FASHION | HOME | GROCERY"
    }
  ],
  "hasNextPage": boolean
}

POST /wishlist
Body: { "productId": "string" }

DELETE /wishlist/{productId}

GET /wishlist
Response: { "productIds": ["string"] }
```

---

**Q1.** List features you will implement and prioritise them. _(2 marks)_

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

Key classes:
```

---

**Q3.** Implement the data layer — models, API service, repository. _(6 marks)_

```kotlin
// === DATA MODELS ===
data class Product(...)


// === API SERVICE ===
interface ProductApiService {



}

// === REPOSITORY ===
class ProductRepository(private val api: ProductApiService) {



}
```

---

**Q4.** Implement the ViewModel with pagination and wishlist state. _(7 marks)_

```kotlin
class ProductViewModel(private val repository: ProductRepository) : ViewModel() {

    // Product list state with pagination



    // Wishlist state



    // Load next page



    // Toggle wishlist



}
```

---

**Q5.** Describe your UI layer. How do you show loading, error, and empty states? _(4 marks)_

```kotlin
// UI State:



// LazyColumn / RecyclerView approach:



// Loading / error / empty handling:


```

---

**Q6.** How would you extend this to support offline wishlist (works without internet)? _(3 marks)_

```
Answer:


```

---

**Round 2 Self Score:** _____ / 25

---

---

## ROUND 3 — DSA ROUND

**Time Allowed:** 60 Minutes      **Marks:** 20

> 2 Medium problems involving binary trees, priority queues, and maps. Solve both with 2 different approaches each.

---

### Problem 1 — Binary Tree: Top View (30 min)

Given a binary tree, print the nodes visible when the tree is viewed from the top (left to right). Use vertical distance (HD = horizontal distance) to determine visibility.

**Example:**

```
        1
       / \
      2   3
       \   \
        4   5

Top View: 2 1 3 5
```

**Step 1 — Approach 1 (BFS + Map)**

```
Approach:


```

**Step 2 — Code (Approach 1)**

```kotlin
class TreeNode(val `val`: Int, var left: TreeNode? = null, var right: TreeNode? = null)

fun topView(root: TreeNode?): List<Int> {



}
```

**Step 3 — Approach 2 (DFS + Map)**

```kotlin
fun topViewDFS(root: TreeNode?): List<Int> {



}
```

**Step 4 — Dry Run**

```
Input tree: 1 → left:2, right:3, 2→right:4, 3→right:5

HD assignments:



Top view output:
```

||Approach 1 (BFS)|Approach 2 (DFS)|
|---|---|---|
|Time Complexity|O( )|O( )|
|Space Complexity|O( )|O( )|

_(10 marks — 5 per approach)_

---

### Problem 2 — Priority Queue + Map: Top K Frequent Elements (30 min)

Given an integer array, return the K most frequent elements. The answer may be returned in any order.

**Example:**

```
Input:  nums = [1,1,1,2,2,3], k = 2
Output: [1,2]
```

**Step 1 — Approach 1 (Min-Heap of size K)**

```
Approach:


```

**Step 2 — Code (Approach 1)**

```kotlin
fun topKFrequent(nums: IntArray, k: Int): IntArray {



}
```

**Step 3 — Approach 2 (Bucket Sort)**

```kotlin
fun topKFrequentBucket(nums: IntArray, k: Int): IntArray {



}
```

**Step 4 — Dry Run**

```
Input: [1,1,1,2,2,3], k=2

Frequency map:

Heap / bucket trace:

Output:
```

||Approach 1 (Heap)|Approach 2 (Bucket)|
|---|---|---|
|Time Complexity|O( )|O( )|
|Space Complexity|O( )|O( )|

_(10 marks — 5 per approach)_

---

**Round 3 Self Score:** _____ / 20

---

---

## ROUND 4 — ANDROID CORE CONCEPTS

**Time Allowed:** 60 Minutes      **Marks:** 25

> Deep Android internals. Scenario-based questions included. Resume topics may be probed.

---

**Q1.** Activity Lifecycle — what happens in each scenario? _(4 marks)_

**(a)** User opens app, navigates to a second Activity, then presses Back:

```
Activity A callbacks:

Activity B callbacks:
```

**(b)** Activity is in background and system is low on memory:

```
Answer:

```

---

**Q2.** What are Intents? Difference between explicit and implicit intents? What is an Intent Filter? _(3 marks)_

```
Explicit Intent:

Implicit Intent:

Intent Filter:
```

---

**Q3.** What are Broadcast Receivers? What is the difference between static and dynamic registration? When would you use each? _(3 marks)_

```
Answer:



```

```kotlin
// Dynamic registration example:



```

---

**Q4.** Types of Context in Android — explain each and give a scenario where using the wrong one causes a problem. _(4 marks)_

```
ApplicationContext:

ActivityContext:

Service Context:

Wrong usage example (memory leak scenario):
```

---

**Q5.** Services — explain the difference between `Service`, `IntentService`, `Foreground Service`, and `Bound Service`. _(4 marks)_

```
Service:

IntentService (deprecated):

Foreground Service:

Bound Service:
```

---

**Q6.** Garbage Collection in Android — how does it work? What is the generational GC model? How do you avoid triggering excessive GC? _(4 marks)_

```
Answer:




```

---

**Q7.** Scenario question — Your app shows ANR (Application Not Responding) dialogs in production. Walk me through your entire debugging process. _(3 marks)_

```
Step 1:

Step 2:

Step 3:

Step 4:
```

---

**Round 4 Self Score:** _____ / 25

---

---

## ROUND 5 — SYSTEM DESIGN

**Time Allowed:** 60 Minutes      **Marks:** 20

> Started as event logging library design, shifted to e-commerce cart + wishlist HLD. Be flexible and pivot gracefully.

---

### Part A — Event Logging Library Design (20 min)

**Task:** Design an analytics/event logging SDK that any Android app can integrate.

**Q1.** What is the public API surface? _(2 marks)_

```kotlin
// Public API:
object AnalyticsLogger {
    fun init(...)
    fun logEvent(...)
    fun flush()
}
```

**Q2.** How do you batch events and flush them efficiently? _(3 marks)_

```
Batching strategy:

Flush triggers (size / time / lifecycle):

Persistence (what if app is killed before flush?):
```

**Q3.** How do you ensure the SDK never blocks the main thread? _(2 marks)_

```
Answer:

```

---

### Part B — E-Commerce Cart + Wishlist HLD (40 min)

**Task:** Design the high-level architecture for the Cart and Wishlist features of a Flipkart-scale e-commerce Android app.

---

**Q4.** Functional and non-functional requirements. _(2 marks)_

```
Functional:
1.
2.
3.

Non-functional:
1.
2.
```

---

**Q5.** High-level architecture — client, API, caching, database. _(4 marks)_

```
Architecture Description:




Data flow (add to cart → checkout):


```

---

**Q6.** How do you handle cart sync across devices? _(2 marks)_

```
Answer:


```

---

**Q7.** How do you handle the case where a product goes out of stock while it is in a user's cart? _(3 marks)_

```
Answer:



```

---

**Q8.** Wishlist — how is the data model different from Cart? How do you handle a wishlisted item going on sale? (push notification design) _(2 marks)_

```
Answer:


```

---

**Round 5 Self Score:** _____ / 20

---

---

## ROUND 6 — HIRING MANAGER ROUND

**Time Allowed:** 60 Minutes      **Marks:** 5

> Focused on your contributions, ownership, and end-to-end process thinking.

---

**Q1.** Describe a project you owned end-to-end — from requirements to deployment. What decisions did you make and what was the impact? _(3 marks)_

```
Answer:




```

**Q2.** What have you achieved in your current/previous role that you are most proud of? _(2 marks)_

```
Answer:


```

---

**Round 6 Self Score:** _____ / 5

---

---

## FINAL SCORECARD

|Round|Description|Max Marks|Your Score|
|---|---|---|---|
|Round 1|Technical Phone Screen|5||
|Round 2|Machine Coding — Product Listing App|25||
|Round 3|DSA — Binary Tree + Priority Queue|20||
|Round 4|Android Core Concepts|25||
|Round 5|System Design — Event Logger + Cart/Wishlist|20||
|Round 6|Hiring Manager|5||
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

### Machine Coding

- [ ] Build a product listing app with pagination + wishlist in under 3 hours
- [ ] Practice Clean Architecture + MVVM with Retrofit + Room + Hilt
- [ ] Use technology you know best — don't experiment under time pressure
- [ ] Practice Jetpack Compose if you plan to use it: LazyColumn, state hoisting

### DSA

- [ ] Binary Trees — top view, bottom view, left/right view, LCA, diameter
- [ ] Priority Queue — top K frequent, K closest, merge K sorted lists, median stream
- [ ] HashMap patterns — frequency counting, subarray sum, anagram grouping
- [ ] Solve Medium problems in under 25 minutes with 2 approaches

### Android Core

- [ ] Full Activity + Fragment lifecycle — all edge cases from memory
- [ ] Intents — explicit vs implicit, Intent Filter, PendingIntent
- [ ] Broadcast Receivers — static vs dynamic registration, ordered broadcasts
- [ ] Context types — Application, Activity, Service — when each is safe
- [ ] Services — Service, Foreground, Bound, WorkManager
- [ ] GC and memory — generational GC, avoiding allocations in loops/onDraw
- [ ] ANR debugging — StrictMode, systrace, main thread monitoring

### System Design

- [ ] Event logging SDK design — batching, flushing, persistence, threading
- [ ] E-commerce cart and wishlist — sync, out-of-stock handling, notifications
- [ ] Design patterns for HLD: CQRS, event-driven, caching strategies
- [ ] Practice pivoting gracefully when the interviewer changes the problem

### Behavioural

- [ ] Prepare end-to-end project ownership story (requirements → deployment)
- [ ] Quantify your impact — users affected, latency improved, crashes reduced
- [ ] Research Flipkart's tech blog and engineering culture

---

_Template based on real Flipkart SDE-2 Android interview experience — Aug 2024._