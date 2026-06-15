# Lambdas in Kotlin

> Based on official Kotlin documentation: https://kotlinlang.org/docs/lambdas.html

---

## 1. What Is a Lambda?

A **lambda expression** is a function literal — a function that is not declared with `fun`, but instead passed immediately as an expression. It is also called an **anonymous function** because it has no name.

Kotlin functions are **first-class**, meaning they can be:

- Stored in variables
- Passed as arguments to other functions
- Returned from other functions

```kotlin
// Named function
fun add(a: Int, b: Int): Int = a + b

// Same logic as a lambda
val add: (Int, Int) -> Int = { a, b -> a + b }
```

---

## 2. Lambda Syntax

Per the official Kotlin docs, the full syntactic form of a lambda expression is:

```kotlin
val sum: (Int, Int) -> Int = { x: Int, y: Int -> x + y }
```

Rules:

- A lambda is always surrounded by **curly braces `{ }`**
- Parameters are declared **inside** the curly braces with optional type annotations
- The body goes **after the `->` arrow**
- If the return type is not `Unit`, the **last expression** in the body is the return value — no `return` keyword needed

Stripped of optional annotations, it looks like:

```kotlin
val sum = { x: Int, y: Int -> x + y }
```

### Function Type Notation

```
(ParameterType) -> ReturnType
```

|Lambda|Function Type|Description|
|---|---|---|
|`{ println("hi") }`|`() -> Unit`|No input, no return|
|`{ x: Int -> x * 2 }`|`(Int) -> Int`|One input, returns Int|
|`{ a: Int, b: Int -> a + b }`|`(Int, Int) -> Int`|Two inputs, returns Int|
|`{ name: String -> println(name) }`|`(String) -> Unit`|One input, no return|

`Unit` is Kotlin's equivalent of `void` — it means the function returns nothing meaningful.

---

## 3. Higher-Order Functions

A **higher-order function** is a function that either:

- Takes another function (lambda) as a parameter, or
- Returns a function

```kotlin
fun doWork(onComplete: () -> Unit) {
    println("Working...")
    onComplete() // invoking the lambda
}

doWork {
    println("Done!")
}
// Output:
// Working...
// Done!
```

The critical rule here: **accepting a lambda as a parameter does not execute it.** You must explicitly invoke it inside the function body.

---

## 4. The `= { }` Trap — Returning vs. Executing

This is one of the most common mistakes in Kotlin, and it is subtle.

### Wrong:

```kotlin
fun getInfo(id: Long, onComplete: (String) -> Unit) = {
    val result = "User #$id"
    onComplete(result)
}
```

### What Kotlin actually understands:

```kotlin
fun getInfo(id: Long, onComplete: (String) -> Unit): () -> Unit {
    return {
        val result = "User #$id"
        onComplete(result)
    }
}
```

Using `= { }` tells Kotlin to **return a lambda**, not execute code. When you call `getInfo(10) { ... }`, you get back a lambda object — but nothing runs.

### Correct:

```kotlin
fun getInfo(id: Long, onComplete: (String) -> Unit) {
    val result = "User #$id"
    onComplete(result) // explicitly invoke the lambda
}
```

### Summary of the difference:

```kotlin
// Executes code directly
fun process() {
    println("running")
}

// Returns a lambda — code runs only when the returned lambda is invoked
fun process() = {
    println("running")
}
```

If you do want to return a lambda and invoke it, you would need to explicitly call `.invoke()` on the result:

```kotlin
getInfo(10) { user -> display(user) }.invoke()
// or
getInfo(10) { user -> display(user) }()
```

But this is not the pattern for callbacks. Use a regular function body `{ }` without `=`.

---

## 5. Passing a Lambda to a Function

```kotlin
fun filter(items: List<Int>, condition: (Int) -> Boolean): List<Int> {
    val result = mutableListOf<Int>()
    for (item in items) {
        if (condition(item)) result.add(item)
    }
    return result
}

val numbers = listOf(1, 2, 3, 4, 5, 6)
val evens = filter(numbers) { it % 2 == 0 }
println(evens) // [2, 4, 6]
```

The lambda `{ it % 2 == 0 }` is passed as the `condition` parameter and invoked with `condition(item)` inside the function.

---

## 6. Trailing Lambda Syntax

Per Kotlin convention, if the **last parameter** of a function is a function type, the lambda can be placed **outside the parentheses**:

```kotlin
// Normal call
items.fold(0, { acc, i -> acc + i })

// Trailing lambda syntax — same result, cleaner
items.fold(0) { acc, i -> acc + i }
```

If the lambda is the **only argument**, parentheses can be omitted entirely:

```kotlin
run({ println("hello") })

// Simplified
run { println("hello") }
```

You use trailing lambda syntax constantly in Android:

```kotlin
// Button composable — onClick and content are both lambdas
Button(onClick = { viewModel.submit() }) {
    Text("Submit")
}

// Coroutine launch
viewModelScope.launch {
    // coroutine body
}

// Observers
viewModel.userData.observe(viewLifecycleOwner) { user ->
    binding.nameText.text = user.name
}
```

---

## 7. `it` — Implicit Name for Single Parameter

Per the official docs: if a lambda has **only one parameter** and the compiler can infer the type, the parameter does not need to be declared and `->` can be omitted. The parameter is implicitly named **`it`**.

```kotlin
// Explicit parameter name
val doubled = listOf(1, 2, 3).map { number -> number * 2 }

// Using `it` — same result
val doubled = listOf(1, 2, 3).map { it * 2 }
```

Prefer explicit names when:

- The lambda is multi-line
- The parameter's meaning is not obvious from context
- There are nested lambdas (nested `it` references become ambiguous)

---

## 8. Returning a Value from a Lambda

The **last expression** in a lambda is automatically treated as its return value. No `return` keyword is needed or valid at the top level of a lambda.

```kotlin
val classify: (Int) -> String = { number ->
    if (number > 0) "positive"
    else if (number < 0) "negative"
    else "zero"
    // last expression is the return value
}

println(classify(5))   // positive
println(classify(-3))  // negative
```

To return early from inside a lambda, use a **labeled return**:

```kotlin
listOf(1, 2, 3, 4, 5).forEach { number ->
    if (number == 3) return@forEach // returns from this lambda iteration only
    println(number)
}
// Output: 1 2 4 5
```

---

## 9. Storing a Lambda and Invoking It Later

A lambda can be stored in a variable and called later using either direct call syntax or `.invoke()`.

```kotlin
val greet: (String) -> Unit = { name -> println("Hello, $name!") }

greet("Alice")         // direct call
greet.invoke("Alice")  // explicit invoke — identical result
```

For nullable lambda variables:

```kotlin
var callback: (() -> Unit)? = null

// Assign later
callback = { println("Callback executed") }

// Safe call — does nothing if null
callback?.invoke()
```

This pattern is directly related to coroutine Job management:

```kotlin
private var myJob: Job? = null

// WRONG — job is never stored
viewModelScope.launch {
    // ...
}.also { job ->
    // job exists here but is never assigned to myJob
}
myJob?.cancel() // myJob is still null — cancel does nothing

// CORRECT
myJob = viewModelScope.launch {
    // ...
}
myJob?.cancel() // works correctly
```

---

## 10. Function References (`::`)

If a named function already exists, you can pass it using the `::` operator instead of writing a lambda that just delegates to it.

```kotlin
fun isPositive(n: Int): Boolean = n > 0

val numbers = listOf(-1, 2, -3, 4)

// Lambda
val result = numbers.filter { n -> isPositive(n) }

// Function reference — same result, more concise
val result = numbers.filter(::isPositive)
```

Member function references:

```kotlin
val words = listOf("hello", "world")
val upperCase = words.map(String::uppercase)
```

---

## 11. Closures — Capturing Variables

A lambda can access and **modify** variables from its enclosing scope. This is called a **closure**.

```kotlin
var counter = 0

val increment = { counter++ }

increment()
increment()
increment()

println(counter) // 3
```

Per the official docs: _"The variables captured in the closure can be modified in the lambda."_

This is important in Android callbacks:

```kotlin
fun test() {
    var userResponse = UserResponse(id = 0, name = "")

    viewModel.getInfo(10) { user ->
        userResponse = user // modifying the outer variable from inside the lambda
    }
}
```

---

## 12. Common Android Patterns

### Callback for async result

```kotlin
// ViewModel
fun fetchUser(id: Long, onSuccess: (User) -> Unit, onError: (Throwable) -> Unit) {
    viewModelScope.launch {
        try {
            val user = repository.getUser(id)
            onSuccess(user)
        } catch (e: Exception) {
            onError(e)
        }
    }
}

// Fragment
viewModel.fetchUser(
    id = 10L,
    onSuccess = { user ->
        binding.nameText.text = user.name
    },
    onError = { error ->
        showToast(error.message)
    }
)
```

### Click listeners

```kotlin
binding.submitButton.setOnClickListener {
    viewModel.onSubmitClicked()
}
```

### Jetpack Compose

```kotlin
Button(
    onClick = { viewModel.onSubmit() }
) {
    Text("Submit")
}
```

### Collections

```kotlin
val users = listOf(User("Alice", 30), User("Bob", 25), User("Charlie", 35))

val adults      = users.filter { it.age >= 18 }
val names       = users.map { it.name }
val totalAge    = users.sumOf { it.age }
val sorted      = users.sortedBy { it.name }
```

### Coroutines

```kotlin
viewModelScope.launch(Dispatchers.IO) {
    val data = repository.fetchData()
    withContext(Dispatchers.Main) {
        _uiState.value = data
    }
}
```

---

## 13. Three Mistakes to Avoid

### Mistake 1 — Defining a lambda parameter but never calling it

```kotlin
// WRONG — onComplete is received but never invoked
fun getInfo(id: Long, onComplete: (User) -> Unit) {
    val user = User(id, "name")
    // onComplete is missing — the callback never fires
}

// CORRECT
fun getInfo(id: Long, onComplete: (User) -> Unit) {
    val user = User(id, "name")
    onComplete(user) // explicitly invoke
}
```

### Mistake 2 — Using `= { }` when you mean to execute, not return

```kotlin
// WRONG — this returns a lambda object; nothing executes on call
fun process(callback: () -> Unit) = {
    callback()
}

// CORRECT — this executes the body when called
fun process(callback: () -> Unit) {
    callback()
}
```

### Mistake 3 — Not storing a Job/lambda reference before trying to cancel it

```kotlin
// WRONG
viewModelScope.launch { heavyWork() }
    .also { /* job variable never assigned to a field */ }

cancelButton.setOnClickListener {
    myJob?.cancel() // myJob is null — does nothing
}

// CORRECT
myJob = viewModelScope.launch { heavyWork() }

cancelButton.setOnClickListener {
    myJob?.cancel() // works
}
```

---

## 14. Quick Reference

```kotlin
// Basic lambda stored in a variable
val greet: (String) -> Unit = { name -> println("Hello, $name!") }

// No parameters
val sayHi: () -> Unit = { println("Hi!") }

// Multiple parameters
val add: (Int, Int) -> Int = { a, b -> a + b }

// Single parameter with `it`
val double: (Int) -> Int = { it * 2 }

// Passing a lambda to a function
fun execute(action: () -> Unit) { action() }
execute { println("executed") }

// Trailing lambda
listOf(1, 2, 3).forEach { println(it) }

// Nullable lambda with safe invoke
var onDone: (() -> Unit)? = null
onDone?.invoke()

// Function reference
listOf("a", "b").map(String::uppercase)

// Labeled return inside lambda
list.forEach { if (it == 3) return@forEach }
```

---

## References

- [Kotlin Official Docs — Higher-order functions and lambdas](https://kotlinlang.org/docs/lambdas.html)
- [Kotlin Official Docs — Functions](https://kotlinlang.org/docs/functions.html)
- [Kotlin Official Docs — Inline functions](https://kotlinlang.org/docs/inline-functions.html)