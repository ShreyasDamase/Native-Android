# GROWW — SDE ANDROID INTERN (2025)

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
> 1. This exam replicates the actual Groww Android Intern interview — no DSA, entirely assignment + Android concepts.
> 2. Answer every question as if you are the one who built the stock display app.
> 3. The key feedback from the real interview: **go deeper into how things work under the hood**. Every answer here should reflect that.
> 4. Self-evaluate at the end.

---

---

## PHASE 0 — ASSIGNMENT (Pre-Interview)

> This is not timed in the exam but you must complete it before attempting the interview rounds below. Build this app first, then answer the interview questions about it.

---

### Assignment: Stock Display App

**Task:** Build a stock display Android app in Kotlin with the following requirements:

- Fetch a list of stocks from a mock/real API
- Display them in a RecyclerView or LazyColumn
- Implement MVVM architecture with a Repository layer
- Cache the API response — use cache if available, else make an API call
- Add a bottom navigation bar with at least 2 tabs (e.g., Stocks, Watchlist)
- Allow selecting a stock to view details

**Self-checklist before the interview:**

|Item|Done?|
|---|---|
|App runs without crashes|Yes / No|
|MVVM layers clearly separated|Yes / No|
|Repository handles cache + API logic|Yes / No|
|Bottom navigation implemented|Yes / No|
|Can explain every line of code written|Yes / No|
|Numbers on resume are accurate and explainable|Yes / No|

---

---

## ROUND 1 — ASSIGNMENT DISCUSSION + ANDROID CONCEPTS

**Time Allowed:** 60 Minutes      **Marks:** 100

> The entire interview revolves around your assignment. Answer as if explaining to a Senior Android Engineer who will probe every claim you make.

---

### Section A — Architecture (20 marks)

---

**Q1.** What is MVVM architecture? Explain each layer and what it is responsible for. _(5 marks)_

```
Model:


ViewModel:


View:


Why MVVM over MVP or MVC for Android?

```

---

**Q2.** How does your ViewModel help retain data across screen rotations? What actually happens under the hood? _(5 marks)_

```
Surface answer (most candidates stop here):


Under the hood (what the interviewer wants):
- What is ViewModelStore?
- Who owns the ViewModelStore?
- When is the ViewModel actually destroyed?
- How does ViewModelProvider find an existing ViewModel vs create a new one?




```

---

**Q3.** What is the Repository pattern? Why did you add it as a separate layer instead of calling the API directly from the ViewModel? _(5 marks)_

```
Answer:



Single Responsibility benefit:

Testability benefit:

Caching benefit:
```

---

**Q4.** What is Dependency Injection? Did you use a DI framework (Hilt/Koin) or manual DI? Justify your choice. _(5 marks)_

```
What is DI (explain the concept, not just the definition):


Manual DI vs Hilt vs Koin — trade-offs:


How DI is set up in your app:


```

---

### Section B — Screen Rotation (10 marks)

---

**Q5.** What happens step by step when the user rotates the screen in your app? _(6 marks)_

```
Step 1 — System side:

Step 2 — Activity callbacks triggered (in order):

Step 3 — ViewModel:

Step 4 — UI resubscribes to ViewModel:

Step 5 — Data displayed again:

What would happen WITHOUT ViewModel (e.g., if you stored data in the Activity)?
```

---

**Q6.** Your stock list was loaded from the API before rotation. After rotation, does it make another API call? Why or why not? _(4 marks)_

```
Answer (trace through your Repository + ViewModel):



```

---

### Section C — Caching Logic (15 marks)

---

**Q7.** Explain your caching implementation. Where exactly did you put the logic to check the cache before making an API call? _(5 marks)_

```
Where the cache check lives (Repository / ViewModel / elsewhere):


Cache validation logic (how do you know cache is stale?):


What you use as the cache (in-memory / Room / SharedPreferences):
```

---

**Q8.** Walk me through the full data flow for the first app launch vs a subsequent launch with valid cache. _(5 marks)_

```
First launch:
1.
2.
3.
4.

Subsequent launch (cache valid):
1.
2.
3.
```

---

**Q9.** How would you handle the case where the cache is stale (e.g., older than 5 minutes) but the network is unavailable? _(5 marks)_

````
Strategy:



Code sketch:
```kotlin
// Cache staleness check + offline fallback:



````

---

### Section D — Scenario Question: Single Selection in RecyclerView (15 marks)

---

**Q10.** A screen shows a RecyclerView with payment options (PhonePe, Cash on Delivery, UPI, etc.). Only one card can be selected at a time. How do you implement this? _(8 marks)_

**Step 1 — State management approach:**

```
Where does the selected state live (ViewModel / Adapter / elsewhere)?

Why NOT in the Adapter?


```

**Step 2 — Code the ViewModel state:**

```kotlin
class PaymentViewModel : ViewModel() {

    // Selected payment option state



    fun selectPaymentOption(option: PaymentOption) {



    }
}
```

**Step 3 — Code the Adapter:**

```kotlin
class PaymentAdapter(
    private val options: List<PaymentOption>,
    private val selectedOption: PaymentOption?,
    private val onSelect: (PaymentOption) -> Unit
) : RecyclerView.Adapter<PaymentAdapter.ViewHolder>() {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {



    }

    // Rest of adapter:
}
```

---

**Q11.** How do you persist the user's payment choice across app restarts? _(7 marks)_

````
Option 1 — SharedPreferences:


Option 2 — Room Database:


Which would you choose for a payment selection and why?


Code sketch:
```kotlin
// Saving the selection:



// Restoring on launch:



````

---

### Section E — Bottom Navigation (5 marks)

---

**Q12.** How did you implement the bottom navigation bar? Did you use `BottomNavigationView` + Fragments or Jetpack Navigation Component? _(3 marks)_

```
Approach used:


How tab switching works (Fragment transactions / NavController):


How you preserved Fragment state when switching tabs:
```

---

**Q13.** What happens to a Fragment's ViewModel when the user switches tabs and comes back? _(2 marks)_

```
Answer:

```

---

### Section F — Kotlin Concepts (15 marks)

---

**Q14.** What is a `data class` in Kotlin? What does the compiler auto-generate for it? _(5 marks)_

```
Auto-generated methods:
1.
2.
3.
4.
5.

How data class differs from a regular class:
```

---

**Q15.** Can a data class be empty (no properties)? What happens if you try? _(3 marks)_

```kotlin
// Try this — what happens?
data class Empty()

// Answer:

// What is the minimum requirement for a data class?
```

---

**Q16.** What are lambda functions in Kotlin? Give 3 real use cases from your app. _(4 marks)_

```
Definition:

Use case 1 (click listeners):

Use case 2 (higher-order functions like map/filter):

Use case 3 (coroutines / callbacks):
```

```kotlin
// Write a lambda example from your assignment:



```

---

**Q17.** What is the Activity lifecycle? Write all callbacks in order for: _(3 marks)_

**(a)** App first launch:

**(b)** User presses Home:

**(c)** User returns to app:

---

### Section G — Resume Numbers (5 marks)

---

**Q18.** The interviewer asks about a specific number on your resume (e.g., "reduced load time by 40%" or "handled 10,000 records"). How do you justify it? _(5 marks)_

> Write the number from your resume below and explain how you measured and calculated it.

```
Number/claim on resume:


How it was measured:


What tools or methods you used to measure it:


What it meant in practice:


What you would do differently to improve it further:
```

---

## FINAL SCORECARD

|Section|Description|Max Marks|Your Score|
|---|---|---|---|
|A|Architecture — MVVM, ViewModel, Repository, DI|20||
|B|Screen Rotation — deep dive|10||
|C|Caching Logic|15||
|D|Scenario — Single Selection in RecyclerView|15||
|E|Bottom Navigation|5||
|F|Kotlin — data class, lambdas, lifecycle|15||
|G|Resume Numbers|5||
|Assignment|App built and explainable end-to-end|15||
|**TOTAL**||**100**||

---

## POST-EXAM REFLECTION

**What went well?**

**What needs improvement?**

**The key feedback from the real interview:**

> _"Could have gone deeper into how things actually work under the hood."_

For every concept below, ask yourself: can I explain the **internals**, not just the surface?

- [ ] I can explain ViewModelStore and who owns it
- [ ] I can explain what happens byte-by-byte during screen rotation
- [ ] I can explain how Repository cache validation works in my code
- [ ] I can explain every line of my assignment code
- [ ] I can justify every number on my resume with measurements

---

## PREPARATION CHECKLIST

### Assignment

- [ ] Build the stock display app end-to-end before the interview
- [ ] Use MVVM + Repository + clean package structure
- [ ] Implement real caching with staleness check
- [ ] Add bottom navigation with state preservation
- [ ] Be able to explain every architectural decision

### Android Internals (go deeper than surface)

- [ ] ViewModelStore — who creates it, who owns it, when it's destroyed
- [ ] Activity lifecycle — all callbacks, all scenarios
- [ ] Fragment lifecycle vs Activity lifecycle
- [ ] Context types — Application vs Activity, memory leak risks

### RecyclerView

- [ ] Single selection state management — always in ViewModel, not Adapter
- [ ] DiffUtil for efficient updates
- [ ] RecycledViewPool, ViewHolder pattern

### Kotlin

- [ ] data class — all 5 auto-generated methods, `copy()`, `componentN()`
- [ ] Lambda functions — trailing lambda, higher-order functions, use in collections
- [ ] Coroutines basics — suspend functions, viewModelScope

### Dependency Injection

- [ ] Manual DI vs Hilt — when to use each for an internship project
- [ ] What problem DI actually solves (testability, decoupling)

### Resume

- [ ] Every number on your resume must be measurable and explainable
- [ ] Know the tools used to measure (Profiler, Logcat, analytics, etc.)

---

_Template based on real Groww SDE Android Intern interview experience — July 2025._