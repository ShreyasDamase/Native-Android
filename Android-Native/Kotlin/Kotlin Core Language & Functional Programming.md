# Kotlin Core Language & Functional Programming

This guide covers the core compiler mechanics, types, functions, and object-oriented paradigms in Kotlin, targeting senior-level design patterns and interview readiness.

---

## 1. Compiler Mechanics, Basics & Types

### JVM Compilation Lifecycle
Kotlin compiles down to JVM bytecode (`.class` files) via the Kotlin Compiler (`kotlinc`). When targeting the Android JVM:
1. `kotlinc` translates Kotlin source files (`.kt`) into Java class files (`.class`) containing standard bytecode.
2. The Android SDK build tool (`d8` or `dx`) compiles the `.class` files into a single or multiple `.dex` (Dalvik Executable) files containing Dalvik bytecode.
3. The `.dex` files are packaged inside the APK/AAB and run inside **ART (Android Runtime)**.

### Immutability: `val` vs `var`
*   `val` defines a **read-only reference** (equivalent to `final` in Java). It does not guarantee deep immutability. The object it references can still have mutable internal state (e.g., a mutable list assigned to a `val`).
*   `var` defines a **mutable reference** that can be reassigned to a different instance.

```kotlin
val list = mutableListOf(1, 2)
list.add(3) // Works! The internal structure of the list mutated.
// list = mutableListOf(4) // Compile Error: Reassignment of read-only variable.
```

### Primitives vs. Boxed Types
Kotlin does not expose separate primitive types. Instead, it uses `Int`, `Double`, `Boolean`, etc., and optimizes them under the hood:
*   If a variable is non-nullable (`val x: Int = 10`), the compiler maps it directly to a JVM primitive (`int`) in the bytecode for maximum performance.
*   If a variable is nullable (`val x: Int? = null`) or used in generics (`List<Int>`), it is forced to compile to the JVM boxed representation (`java.lang.Integer`), which incurs object allocation overhead.

---

## 2. Null Safety & Scope Functions

### The Nullable Type System
Kotlin distinguishes between nullable (`Type?`) and non-nullable (`Type`) types. 

*   `?.` (Safe Call): Evaluates the right side only if the receiver is non-null; otherwise returns null.
*   `?:` (Elvis Operator): Evaluates to the right-side expression if the left-side expression evaluates to null.
*   `!!` (Double Bang): Asserts that the expression is non-null, throwing a `NullPointerException` if it is. **Avoid in production code.**
*   Smart Casts: The compiler automatically casts a nullable reference to its non-nullable equivalent after a null check has succeeded (if the variable is local or thread-safe).

### Scope Functions Reference

Scope functions execute a block of code within the context of an object. They differ on whether they refer to the context object as `this` or `it`, and whether they return the context object or the lambda result.

| Function | Context Object | Return Value | Typical Use Case |
| :--- | :--- | :--- | :--- |
| `let` | `it` (argument) | Lambda result | Null-checking and mapping values. |
| `run` | `this` (receiver)| Lambda result | Computing data and initializing setups. |
| `with` | `this` (receiver)| Lambda result | Running multiple operations on a single instance. |
| `apply` | `this` (receiver)| Context object | Object configuration (builder-style). |
| `also` | `it` (argument) | Context object | Side effects, logging, and logging intermediates. |

```kotlin
// Example: apply vs let
val person = Person().apply {
    name = "Shreyas" // referred to as 'this'
    age = 25
}

val upperName = person.let {
    it.name.uppercase() // referred to as 'it', returns lambda result
}
```

---

## 3. Standard Types: `Any`, `Unit`, `Nothing`

*   `Any`: The root of the Kotlin type hierarchy. All non-nullable classes inherit from `Any`. Maps to `java.lang.Object` on the JVM.
*   `Unit`: Represents a function that returns no meaningful value (equivalent to `void` in Java). However, unlike `void`, `Unit` is a singleton object subclass of `Any`.
*   `Nothing`: A type that has **no instances**. It represents a value that never exists. Used to indicate functions that never return (e.g., functions throwing an exception, or infinite loops).

```kotlin
fun throwError(message: String): Nothing {
    throw IllegalArgumentException(message) // Returns Nothing
}
```

---

## 4. Functions & Functional Programming

### Extension Functions
Extension functions allow you to add functionality to an existing class without inheriting from it.
*   **Static Dispatch**: Extension functions are resolved statically at compile time. The compiler translates them to static Java methods taking the receiver as the first parameter. They do not override actual class member methods.

```kotlin
fun String.shout() = this.uppercase() + "!"
// Compiles to: public static String shout(String receiver)
```

### Infix Functions & Operator Overloading
*   `infix` functions allow syntax without dots or parentheses (e.g. `1 to "one"`). They must be member or extension functions and accept exactly one argument.
*   Operator overloading binds predefined operators (like `+`, `-`, `*`) to custom function signatures.

```kotlin
infix fun String.joinWith(other: String): String = "$this $other"
// Usage: "Hello" joinWith "World"

// Operator Overloading
data class Point(val x: Int, val y: Int) {
    operator fun plus(other: Point) = Point(this.x + other.x, this.y + other.y)
}
```

### Lambdas with Receivers (DSL Builder Basics)
A lambda with a receiver behaves like a member function of that receiver class. Inside the lambda block, the context is bound to the receiver instance via `this`.

```kotlin
class HTML {
    fun body() = println("<body>")
}

// Function with receiver lambda parameter
fun html(init: HTML.() -> Unit) {
    val h = HTML()
    h.init() // execute block with 'h' as context
}

// DSL Call
html {
    body() // implicit 'this' is HTML
}
```

---

## 5. OOP Patterns & Delegation

### Sealed Classes vs. Enums
*   `enum class` represents a closed set of **concrete instances** (e.g. `Color.RED`, `Color.BLUE`).
*   `sealed class` represents a closed set of **subtypes** (classes or objects). Sealed class subclasses can have their own unique state and constructor arguments, making them ideal for representing state machines (e.g. `UiState.Success(data)` vs `UiState.Error(exception)`).

### Interface & Property Delegation
Delegation allows one object to forward method calls or property state accessors to an underlying delegate.

```kotlin
interface Printer { fun printMessage() }
class BasicPrinter : Printer { override fun printMessage() = println("Hello") }

// Class delegation: Printer implementation is delegated to 'basicPrinter'
class SmartDevice(basicPrinter: Printer) : Printer by basicPrinter

// Property delegation
class CustomDelegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return "Delegated Value"
    }
}
val delegatedProp: String by CustomDelegate()
```

### Thread Safety Modes in `lazy {}`
Kotlin's `lazy` delegate initializes a property only upon first access. You can specify a `LazyThreadSafetyMode` to control sync patterns:
1.  `LazyThreadSafetyMode.SYNCHRONIZED` (Default): Uses a double-checked locking mechanism to ensure only a single thread initializes the value. Completely thread-safe.
2.  `LazyThreadSafetyMode.PUBLICATION`: Allows multiple threads to execute the initialization concurrently, but only the first returned value is stored and shared among all threads.
3.  `LazyThreadSafetyMode.NONE`: No thread synchronization is used. High performance but unsafe for multi-threaded environments.

---

## 6. Advanced Patterns: Value Classes & Inline Attributes

### Value Classes (`@JvmInline value class`)
Used to wrap basic types to enforce domain safety (e.g. `UserId`, `Email`) without the memory overhead of allocating wrapper objects. The compiler replaces references to the value class with the underlying primitive type in the compiled bytecode wherever possible.

```kotlin
@JvmInline
value class Password(val value: String)
```
