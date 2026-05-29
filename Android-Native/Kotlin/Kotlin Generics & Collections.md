# Kotlin Generics & Collections

This guide provides deep technical documentation on Kotlin Generics, type safety systems, variance models, collection processing, and performance optimizations for collections.

---

## 1. Generics & Type Variance

### The Type Safety Problem
Generics on the JVM suffer from **type erasure** (generic types are stripped away at runtime, e.g. `List<String>` becomes a raw `List`). Type systems must also handle **variance** — how subtyping relationships of type parameters affect subtyping relationships of the generic class itself.

By default, generics in Kotlin are **invariant**. This means `Box<String>` is **not** a subtype of `Box<Any>`, even though `String` is a subtype of `Any`.

---

### Covariance (`out`)
Allows you to treat a generic class containing a subtype as a supertype.
*   **Syntax**: `class Producer<out T>`
*   **Rule**: The type parameter `T` can only be returned (used as an **output** in function return positions). It cannot be passed as an input argument (used as an **input** in function parameter positions).
*   **Relationship**: `Producer<String>` is a subtype of `Producer<Any>`.

```kotlin
// Covariant Interface
interface Source<out T> {
    fun nextT(): T // Allowed: T is in 'out' (return) position
    // fun consume(item: T) // Compile Error: T cannot be in 'in' (input) position
}
```

---

### Contravariance (`in`)
Allows you to treat a generic class containing a supertype as a subtype.
*   **Syntax**: `class Consumer<in T>`
*   **Rule**: The type parameter `T` can only be consumed (used as an **input** in function parameter positions). It cannot be returned as an output.
*   **Relationship**: `Consumer<Any>` is a subtype of `Consumer<String>`.

```kotlin
// Contravariant Interface
interface Comparer<in T> {
    fun compare(first: T, second: T): Int // Allowed: T is in 'in' (parameter) position
    // fun getBest(): T // Compile Error: T cannot be in 'out' (return) position
}
```

---

### Declaration-site vs. Use-site Variance
*   **Declaration-site**: You declare the variance directly where the class or interface is defined (e.g. `interface List<out E>`). Kotlin supports this natively, reducing verbosity for callers.
*   **Use-site** (Type Projections): Used when you cannot declare variance globally at the class declaration because it uses both inputs and outputs. Declare it where you use the type parameter (equivalent to Java wildcards `? extends T` and `? super T`).

```kotlin
// Use-site variance projection (out projection)
fun copy(from: Array<out Any>, to: Array<Any>) {
    for (i in from.indices) {
        to[i] = from[i]
    }
}
```

---

### Star Projection (`<*>`)
Used when you know nothing about the type argument but still want to interact with the collection safely.
*   For `Foo<out T : Upper>`, `Foo<*>` compiles to `Foo<out Upper>`, meaning you can safely read `Upper` values.
*   For `Foo<in T>`, `Foo<*>` compiles to `Foo<in Nothing>`, meaning you cannot write anything to it.

---

### Reified Type Parameters
Because of JVM type erasure, generic type checks (like `item is T` or `T::class.java`) are compile errors. Kotlin bypasses this limitation using `reified` type parameters, which must always be combined with `inline` functions.

When a function is marked `inline`, the compiler copies the function's bytecode directly into the caller's call site. This allows the compiler to replace the generic `T` with the actual concrete type used at the call site.

```kotlin
// Reified inline function
inline fun <reified T> Any.isType(): Boolean {
    return this is T // Compiles safely!
}

// Usage
val isString = "Hello".isType<String>() // Returns true
```

---

## 2. Collection Processing & Functional APIs

### List, Set, Map, and Sequence
Kotlin standardizes collections into Read-Only and Mutable hierarchies:

*   `List`: Ordered collection of elements.
*   `Set`: Unordered collection of unique elements.
*   `Map`: Key-value association pairs.
*   `Sequence`: Represents a lazy-evaluated collection (comparable to Java streams).

---

### Sequence (Lazy Evaluation) vs. List (Eager Evaluation)
*   **List Operations**: Eager evaluation. Each step (e.g. `map`, `filter`) completes entirely and allocates a temporary, intermediate list before moving to the next step.
*   **Sequence Operations**: Lazy evaluation. Elements are processed one-by-one through the entire chain of operations (using iterators under the hood). No intermediate collections are allocated.

```kotlin
val list = listOf(1, 2, 3, 4)

// List: Allocates intermediate list for filter, then another for map
val listResult = list.filter { it % 2 == 0 }.map { it * 2 }

// Sequence: Elements processed one-by-one. Zero allocation overhead
val seqResult = list.asSequence()
    .filter { it % 2 == 0 }
    .map { it * 2 }
    .toList() // Terminal operator initiates execution
```

> [!TIP]
> Use `Sequence` for large collections (e.g. >1000 items) or chains with multiple intermediate transformations. Use standard `List` operations for small collections, as the iterator wrapping overhead of Sequences outweighs the allocation savings.

---

### Core Functional Operators Reference

*   `map`: Transforms each element into a new representation.
*   `filter`: Returns elements matching the given predicate.
*   `flatMap`: Transforms each element into a collection, then flattens all resulting collections into a single list.
*   `groupBy`: Groups elements by a key selector and returns a `Map<K, List<T>>`.
*   `fold`: Accumulates values starting with an **initial value** and applying an operation from left to right.
*   `reduce`: Similar to `fold`, but uses the **first element** of the collection as the initial value.

```kotlin
// Example: flatMap and fold
val nestedList = listOf(listOf(1, 2), listOf(3, 4))
val flat = nestedList.flatMap { it } // [1, 2, 3, 4]

val sum = flat.fold(10) { acc, num -> acc + num } // Starts at 10; output = 20
```

---

## 3. Collections & Jetpack Compose Stability

In Jetpack Compose, the compiler tracks the stability of types to determine if it can skip recomposing them when inputs haven't changed.

### The List Instability Trap
Standard Kotlin interface collections (`List<T>`, `Set<T>`, `Map<T>`) are treated as **unstable** by the Compose Compiler. This is because the interfaces are read-only, not immutable. Compose cannot guarantee that the underlying implementation isn't a mutable reference (like `ArrayList`) that will change without notifying Compose.

If a Composable accepts a standard `List<T>`, Compose will **not** skip its recomposed frames when state changes, leading to performance degradation.

### The Fix: kotlinx.collections.immutable
To resolve list instability, you should use the explicit immutable collection implementations provided by the `kotlinx.collections.immutable` library.

```kotlin
// Installs immutable collection types
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun UserListScreen(
    users: ImmutableList<User> // Compose compiler marks this as Stable!
) {
    LazyColumn {
        items(users) { user ->
            UserRow(user)
        }
    }
}
```

By enforcing `ImmutableList<T>` as the parameter type, the compiler guarantees the collection cannot be modified, enabling composition skip optimizations.
