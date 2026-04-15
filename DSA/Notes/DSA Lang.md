# Kotlin DSA Reference Guide

### Chapters 1–5 | Pure API & Syntax Reference | No Problem Solutions

---

## COMPLEXITY REFERENCE TABLE

> Read this first. When picking a data structure, this is the decision anchor.

### Time Complexity by Operation

|Operation|IntArray|MutableList|HashMap|HashSet|TreeMap|TreeSet|PriorityQueue|ArrayDeque|
|---|---|---|---|---|---|---|---|---|
|Access by index|O(1)|O(1)|—|—|—|—|—|O(1)|
|Search / contains|O(n)|O(n)|O(1) avg|O(1) avg|O(log n)|O(log n)|O(n)|O(n)|
|Insert at end|—|O(1) amort|O(1) avg|O(1) avg|O(log n)|O(log n)|O(log n)|O(1)|
|Insert at front|—|O(n)|—|—|—|—|—|O(1)|
|Insert at middle|—|O(n)|—|—|—|—|—|O(n)|
|Delete by value|—|O(n)|O(1) avg|O(1) avg|O(log n)|O(log n)|O(n)|O(n)|
|Delete at index|—|O(n)|—|—|—|—|—|O(n)|
|Delete first|—|O(n)|—|—|—|—|—|O(1)|
|Delete last|—|O(1)|—|—|—|—|—|O(1)|
|Get min/max|O(n)|O(n)|—|—|O(log n)|O(log n)|O(1)|—|
|Sort|O(n log n)|O(n log n)|—|—|always sorted|always sorted|—|—|

### Space Complexity Notes

| Structure             | Memory                                | Notes                      |
| --------------------- | ------------------------------------- | -------------------------- |
| `IntArray`            | compact — stores raw primitives       | best for pure numbers      |
| `Array<Int>`          | more — stores boxed `Integer` objects | avoid for numeric DSA      |
| `MutableList<Int>`    | more — boxed integers internally      | flexible but heavier       |
| `HashMap`             | O(n) + hash table overhead            | fast lookup                |
| `HashSet`             | O(n) + hash table overhead            | fast lookup                |
| `TreeMap` / `TreeSet` | O(n) — red-black tree nodes           | always sorted, slower      |
| `PriorityQueue`       | O(n) — binary heap array              | always ordered at top only |

---






## MASTER INDEX

| #   | Chapter                                                                    |
| --- | -------------------------------------------------------------------------- |
| 1   | [Kotlin vs Java — Quick Differences](#1-kotlin-vs-java--quick-differences) |
| 2   | [Variables, Types & Casting](#2-variables-types--casting)                  |
| 3   | [Arrays](#3-arrays)                                                        |
| 4   | [Lists — MutableList / List](#4-lists--mutablelist--list)                  |
| 5   | [HashMap — Map / MutableMap](#5-hashmap--map--mutablemap)                  |
| 6   | HashSet _(Ch 6 — next file)_                                               |
| 7   | Stack — ArrayDeque _(Ch 7 — next file)_                                    |
| 8   | Queue & Deque _(Ch 8 — next file)_                                         |
| 9   | PriorityQueue _(Ch 9 — next file)_                                         |
| 10  | String & StringBuilder _(Ch 10 — next file)_                               |
| ... | _continuing per full index_                                                |

---

# 1. Kotlin vs Java — Quick Differences

## 1.1 Syntax Differences

| Java                             | Kotlin                          | Notes                                         |
| -------------------------------- | ------------------------------- | --------------------------------------------- |
| `int x = 5;`                     | `val x = 5` or `var x = 5`      | no semicolons, type inferred                  |
| `final int x = 5;`               | `val x = 5`                     | `val` = cannot reassign                       |
| `int x = 5;` (mutable)           | `var x = 5`                     | `var` = can reassign                          |
| `if (a) { } else { }`            | same — also works as expression | `val r = if (a) 1 else 2`                     |
| `a ? b : c`                      | `if (a) b else c`               | no ternary operator in Kotlin                 |
| `void`                           | `Unit`                          | functions returning nothing return `Unit`     |
| `Object`                         | `Any`                           | top of type hierarchy                         |
| `null` safety — crash at runtime | `String?` vs `String`           | compiler-enforced at compile time             |
| `instanceof`                     | `is`                            | `if (x is String)`                            |
| `(String) obj`                   | `obj as String`                 | hard cast                                     |
| `switch`                         | `when`                          | more powerful, see Chapter 20                 |
| `new ArrayList<>()`              | `mutableListOf()`               | no `new` keyword                              |
| `System.out.println()`           | `println()`                     |                                               |
| `for (int i=0; i<n; i++)`        | `for (i in 0 until n)`          |                                               |
| `str.length()`                   | `str.length`                    | property, no `()`                             |
| `.equals()`                      | `==`                            | `==` compares value in Kotlin (not reference) |
| `==` (reference)                 | `===`                           | reference equality in Kotlin                  |

## 1.2 `.size` vs `.length`

> This is the #1 source of compilation errors when switching from Java.

| Type            | Property  | Example              |
| --------------- | --------- | -------------------- |
| `String`        | `.length` | `"hello".length` → 5 |
| `StringBuilder` | `.length` | `sb.length`          |
| `IntArray`      | `.size`   | `arr.size`           |
| `Array<T>`      | `.size`   | `arr.size`           |
| `MutableList`   | `.size`   | `list.size`          |
| `HashMap`       | `.size`   | `map.size`           |
| `HashSet`       | `.size`   | `set.size`           |
| `ArrayDeque`    | `.size`   | `deque.size`         |
| `PriorityQueue` | `.size`   | `heap.size`          |

**Rule:** Only `String` and `StringBuilder` use `.length`. Everything else uses `.size`.

## 1.3 `val` vs `var`

```kotlin
val x = 5         // immutable reference — cannot reassign x to another value
var y = 10        // mutable reference — y can be reassigned

val list = mutableListOf(1, 2, 3)
list.add(4)       // ✅ allowed — you're mutating the object, not the reference
list = mutableListOf() // ❌ not allowed — cannot reassign val
```

> **Rule:** Use `val` by default. Switch to `var` only when you need to reassign (e.g., pointer variables like `left`, `right`, `curr`).

## 1.4 `when` vs `switch`

```kotlin
// Java switch
switch (x) {
    case 1: ... break;
    case 2: ... break;
    default: ...
}

// Kotlin when
when (x) {
    1 -> ...
    2 -> ...
    else -> ...
}
```

Key differences:

- No `break` needed
- Can match ranges: `in 1..10 ->`
- Can match multiple values: `1, 2, 3 ->`
- Can be an expression: `val r = when (x) { ... }`
- Can check types: `is String ->`
- Full detail in Chapter 20

## 1.5 Kotlin Null Safety

```kotlin
// Java: any reference can be null — crash at runtime
String s = null;
s.length();  // NullPointerException at runtime

// Kotlin: null must be declared explicitly — caught at compile time
var a: String = "hello"   // non-null — compiler won't allow null
var b: String? = null     // nullable — must handle null explicitly

b.length        // ❌ compile error — b might be null
b?.length       // ✅ safe call — returns null if b is null
b!!.length      // ✅ force — throws NPE if b is null (use only when certain)
b?.length ?: 0  // ✅ elvis — returns 0 if b?.length is null
```

Full null safety reference → Chapter 13.

## 1.6 Kotlin Collection Differences vs Java

|Java|Kotlin|Notes|
|---|---|---|
|`ArrayList` is mutable by default|`listOf()` is immutable|must use `mutableListOf()` for write access|
|`HashMap` is mutable by default|`mapOf()` is immutable|must use `mutableMapOf()`|
|`HashSet` is mutable by default|`setOf()` is immutable|must use `mutableSetOf()`|
|`java.util.Stack`|`ArrayDeque`|don't use Stack in Kotlin|
|`java.util.LinkedList`|`ArrayDeque`|Kotlin's ArrayDeque handles both|
|`Collections.sort(list)`|`list.sort()`|built-in on MutableList|
|`Arrays.sort(arr)`|`arr.sort()`|built-in on IntArray|

---

# 2. Variables, Types & Casting

## 2.1 Variable Declaration

```kotlin
val name: String = "Kotlin"    // explicit type, immutable
var count: Int = 0              // explicit type, mutable
val x = 42                      // type inferred as Int
var flag = true                 // type inferred as Boolean
```

## 2.2 Primitive Types

|Type|Bits|Range|Literal|
|---|---|---|---|
|`Byte`|8|-128 to 127|`1`|
|`Short`|16|-32768 to 32767|`1`|
|`Int`|32|-2,147,483,648 to 2,147,483,647|`42`|
|`Long`|64|-9.2×10^18 to 9.2×10^18|`42L`|
|`Float`|32|~7 decimal digits|`3.14f`|
|`Double`|64|~15 decimal digits|`3.14`|
|`Boolean`||`true` / `false`|`true`|
|`Char`|16|Unicode character|`'a'` ← single quotes|

> ⚠️ `Char` uses **single quotes**. `String` uses **double quotes**. Never mix them.

## 2.3 Numeric Limits

```kotlin
Int.MAX_VALUE       // 2_147_483_647
Int.MIN_VALUE       // -2_147_483_648
Long.MAX_VALUE      // 9_223_372_036_854_775_807
Long.MIN_VALUE      // -9_223_372_036_854_775_808
Double.MAX_VALUE    // ~1.8 × 10^308
Float.MAX_VALUE     // ~3.4 × 10^38

// Readable number literals (underscores for clarity)
val million = 1_000_000
val mod = 1_000_000_007
```

> **DSA Rule:** Use `Long` whenever the result of multiplication or addition could exceed ~2 billion. `n * n`overflows `Int` when `n > 46340`.

## 2.4 Number-to-Number Conversions

```kotlin
val i: Int = 42

i.toByte()        // Int → Byte
i.toShort()       // Int → Short
i.toLong()        // Int → Long
i.toFloat()       // Int → Float
i.toDouble()      // Int → Double
i.toChar()        // Int → Char  (value 97 → 'a')

val l: Long = 1000L
l.toInt()         // Long → Int   ⚠️ may lose data if > Int.MAX_VALUE

val d: Double = 3.99
d.toInt()         // Double → Int  → 3  (truncates, does NOT round)
d.toLong()        // Double → Long → 3
```

> ⚠️ Kotlin does **NOT** do implicit type conversion. You must call `.toX()` explicitly. Java: `int x = longVal;`compiles (implicit). Kotlin: compile error — must write `val x = longVal.toInt()`.

## 2.5 String ↔ Number Conversion

```kotlin
// String → Number
"42".toInt()             // 42        ← throws NumberFormatException if invalid
"42".toIntOrNull()       // 42        ← returns null if invalid (safe)
"abc".toIntOrNull()      // null

"3.14".toDouble()
"3.14".toDoubleOrNull()

"100".toLong()
"100".toLongOrNull()

// Number → String
42.toString()            // "42"
3.14.toString()          // "3.14"
true.toString()          // "true"

// Format with padding
"%05d".format(42)        // "00042"  — left-pad with zeros to width 5
```

## 2.6 Char ↔ Int Conversion

```kotlin
// Char → Int (ASCII / Unicode code point)
'a'.code          // 97
'A'.code          // 65
'0'.code          // 48
'z'.code          // 122

// Int → Char
97.toChar()       // 'a'
65.toChar()       // 'A'

// Char arithmetic (very common in DSA)
'a' - 'a'         // 0   ← index from 'a' (for freq arrays)
'e' - 'a'         // 4
'z' - 'a'         // 25

'7' - '0'         // 7   ← digit char → int value
'0' - '0'         // 0
'9' - '0'         // 9

// Char → uppercase / lowercase
'a'.uppercaseChar()   // 'A'
'A'.lowercaseChar()   // 'a'
```

## 2.7 Char Checks

```kotlin
val ch: Char = 'a'

ch.isLetter()           // true  — a-z or A-Z
ch.isDigit()            // false — 0-9
ch.isLetterOrDigit()    // true
ch.isWhitespace()       // false — space, tab, newline
ch.isUpperCase()        // false
ch.isLowerCase()        // true
ch.isLowerCase()        // true

// Range checks (alternative)
ch in 'a'..'z'          // true  — lowercase
ch in 'A'..'Z'          // false — uppercase
ch in '0'..'9'          // false — digit
```

---

# 3. Arrays

## 3.1 IntArray vs Array<Int>

||`IntArray`|`Array<Int>`|
|---|---|---|
|Equivalent Java|`int[]`|`Integer[]`|
|Stores|primitive `int` values|boxed `Integer` objects|
|Memory|compact — no object overhead|each element is a heap object|
|Speed|faster — no boxing/unboxing|slower|
|Default value|`0`|`null` (if `arrayOfNulls`)|
|Sort in place|`arr.sort()`|`arr.sort()`|
|Use when|storing numbers|storing objects or when null needed|

```kotlin
val a = IntArray(5)          // int[] in Java — use this for numbers
val b = Array<Int>(5) { 0 } // Integer[] in Java — avoid for pure numbers
```

> **Rule:** For numeric DSA problems, always use `IntArray`. Only use `Array<T>` when storing objects like strings, pairs, or custom types.

There is also:

```kotlin
BooleanArray(n)    // boolean[]
CharArray(n)       // char[]
LongArray(n)       // long[]
DoubleArray(n)     // double[]
```

## 3.2 Array Creation

### IntArray Creation

```kotlin
IntArray(5)                      // [0, 0, 0, 0, 0]  — all zeros by default
IntArray(5) { 0 }                // [0, 0, 0, 0, 0]  — explicit zero init
IntArray(5) { it }               // [0, 1, 2, 3, 4]  — it = index
IntArray(5) { it + 1 }          // [1, 2, 3, 4, 5]
IntArray(5) { it * it }         // [0, 1, 4, 9, 16]
IntArray(5) { -1 }              // [-1, -1, -1, -1, -1]

intArrayOf(3, 1, 4, 1, 5)      // known values
intArrayOf()                     // empty IntArray
```

### Other Primitive Arrays

```kotlin
BooleanArray(5)                  // [false, false, false, false, false]
BooleanArray(5) { true }         // [true, true, true, true, true]
CharArray(5) { 'a' }             // ['a', 'a', 'a', 'a', 'a']
LongArray(5)                     // [0L, 0L, 0L, 0L, 0L]
```

### Array<T> Creation

```kotlin
arrayOf("a", "b", "c")                      // Array<String>
arrayOf(1, 2, 3)                             // Array<Int> (boxed)
Array(3) { "" }                              // ["", "", ""]
Array(3) { i -> "item$i" }                  // ["item0", "item1", "item2"]
arrayOfNulls<String>(5)                      // [null, null, null, null, null]

// 2D — Array of IntArray
Array(3) { IntArray(4) }                     // 3 rows × 4 cols, all 0
Array(3) { IntArray(4) { it } }             // each row: [0,1,2,3]
```

## 3.3 Accessing Elements

```kotlin
val arr = intArrayOf(10, 20, 30, 40, 50)

arr[0]               // 10  — first element
arr[4]               // 50  — last element
arr[arr.size - 1]    // 50  — last element (safe way)
arr.size             // 5   ← property, no ()
arr[2] = 99          // set index 2 to 99

arr.first()          // 10  — throws if empty
arr.last()           // 50  — throws if empty
arr.firstOrNull()    // 10  — null if empty
arr.lastOrNull()     // 50  — null if empty
```

> ⚠️ Accessing `arr[i]` where `i >= arr.size` or `i < 0` throws `ArrayIndexOutOfBoundsException`. Always validate index: `if (i in arr.indices)` before accessing.

## 3.4 Array Methods

### Information

|Method|Returns|What it does|
|---|---|---|
|`arr.size`|`Int`|number of elements|
|`arr.indices`|`IntRange`|`0 until arr.size`|
|`arr.lastIndex`|`Int`|`arr.size - 1`|
|`arr.isEmpty()`|`Boolean`|true if size == 0|
|`arr.isNotEmpty()`|`Boolean`|true if size > 0|
|`arr.contains(x)`|`Boolean`|linear scan O(n)|
|`x in arr`|`Boolean`|same as contains|
|`arr.indexOf(x)`|`Int`|first index of x, -1 if not found|
|`arr.lastIndexOf(x)`|`Int`|last index of x|

### Aggregation

|Method|Returns|What it does|
|---|---|---|
|`arr.sum()`|`Int`|sum of all elements|
|`arr.sumOf { it.toLong() }`|`Long`|sum with transform|
|`arr.max()`|`Int`|max value — throws if empty|
|`arr.min()`|`Int`|min value — throws if empty|
|`arr.maxOrNull()`|`Int?`|null if empty|
|`arr.minOrNull()`|`Int?`|null if empty|
|`arr.average()`|`Double`|mean|
|`arr.count()`|`Int`|same as size|
|`arr.count { it > 3 }`|`Int`|count matching predicate|

### Sorting (IN PLACE — modifies original)

|Method|Returns|What it does|
|---|---|---|
|`arr.sort()`|`Unit`|ascending, in place|
|`arr.sortDescending()`|`Unit`|descending, in place|
|`arr.sort(from, to)`|`Unit`|sort index from..to-1 only|
|`arr.reverse()`|`Unit`|reverse in place|
|`arr.fill(v)`|`Unit`|fill all elements with v|
|`arr.fill(v, from, to)`|`Unit`|fill index from..to-1 with v|

### Returning New Collection (original unchanged)

|Method|Returns|What it does|
|---|---|---|
|`arr.sorted()`|`List<Int>`|new sorted list ascending|
|`arr.sortedDescending()`|`List<Int>`|new sorted list descending|
|`arr.reversed()`|`List<Int>`|new reversed list|
|`arr.copyOf()`|`IntArray`|full copy|
|`arr.copyOf(n)`|`IntArray`|copy first n elements|
|`arr.copyOfRange(f, t)`|`IntArray`|copy index f..t-1|
|`arr.toList()`|`List<Int>`|convert to List|
|`arr.toMutableList()`|`MutableList<Int>`|convert to MutableList|
|`arr.toSet()`|`Set<Int>`|convert, removes duplicates|
|`arr.toHashSet()`|`HashSet<Int>`|convert to HashSet|
|`arr.toSortedSet()`|`SortedSet<Int>`|sorted unique set|

### Joining / Printing

```kotlin
arr.joinToString()          // "10, 20, 30, 40, 50"
arr.joinToString("")        // "1020304050"
arr.joinToString(", ")      // "10, 20, 30, 40, 50"
arr.joinToString(" | ")     // "10 | 20 | 30 | 40 | 50"
```

## 3.5 Array Looping Patterns

### Index Loop — when you need the index

```kotlin
for (i in arr.indices) {
    val value = arr[i]
}
// arr.indices = 0 until arr.size = 0, 1, 2, ..., arr.size-1
```

> Use when: two pointers, modifying elements, index arithmetic.

### Element Loop — when you only need the value

```kotlin
for (value in arr) {
    // use value
}
```

> Use when: reading, summing, comparing values.

### Index + Value Together

```kotlin
for ((i, value) in arr.withIndex()) {
    println("arr[$i] = $value")
}
```

> Use when: you need both index and value without writing `arr[i]` each time.

### Reverse Loop

```kotlin
for (i in arr.lastIndex downTo 0) {
    val value = arr[i]
}
// arr.lastIndex = arr.size - 1
```

> Use when: processing from end, some stack/DP patterns.

### Step Loop

```kotlin
for (i in 0 until arr.size step 2) {
    // i = 0, 2, 4, ...  — every other element
}
```

### ⚠️ forEach Trap

```kotlin
// forEach cannot break, cannot return from outer function cleanly
arr.forEach { value ->
    if (value == 5) return@forEach   // only skips current iteration (like continue)
    // cannot break out of arr.forEach entirely
}

// ✅ Use for loop when you need break or early return
for (value in arr) {
    if (value == 5) break    // works
}
```

## 3.6 2D Arrays

### Creation

```kotlin
// Rows × Cols, all zeros
val matrix = Array(3) { IntArray(4) }      // 3 rows, 4 cols

// With init values
val grid = arrayOf(
    intArrayOf(1, 2, 3),
    intArrayOf(4, 5, 6),
    intArrayOf(7, 8, 9)
)

// Dynamic size
val rows = 4; val cols = 5
val dp = Array(rows) { IntArray(cols) }
val dp2 = Array(rows) { IntArray(cols) { -1 } }   // all -1
```

### Accessing

```kotlin
grid.size          // number of rows
grid[0].size       // number of columns
grid[r][c]         // element at row r, col c
grid[r][c] = 99   // set element
grid[0]            // entire row 0 (returns IntArray)
```

### Traversal

```kotlin
// Row by row
for (row in grid) {
    for (cell in row) { }
}

// With indices — most used in DSA
for (r in grid.indices) {
    for (c in grid[r].indices) {
        val val_ = grid[r][c]
    }
}

// With explicit bounds
val rows = grid.size
val cols = grid[0].size
for (r in 0 until rows) {
    for (c in 0 until cols) {
        val val_ = grid[r][c]
    }
}
```

### Grid Traversal — 4 Directions

```kotlin
val dirs = arrayOf(
    intArrayOf(0, 1),    // right
    intArrayOf(0, -1),   // left
    intArrayOf(1, 0),    // down
    intArrayOf(-1, 0)    // up
)

// Bounds check before accessing
for (d in dirs) {
    val nr = r + d[0]
    val nc = c + d[1]
    if (nr in 0 until rows && nc in 0 until cols) {
        // safe to access grid[nr][nc]
    }
}
```

### Grid Traversal — 8 Directions

```kotlin
val dirs8 = arrayOf(
    intArrayOf(0,1), intArrayOf(0,-1),
    intArrayOf(1,0), intArrayOf(-1,0),
    intArrayOf(1,1), intArrayOf(1,-1),
    intArrayOf(-1,1), intArrayOf(-1,-1)
)
```

## 3.7 Common Array Patterns

### Prefix Sum Array

```kotlin
val nums = intArrayOf(1, 2, 3, 4, 5)
val prefix = IntArray(nums.size + 1)          // prefix[0] = 0

for (i in nums.indices) {
    prefix[i + 1] = prefix[i] + nums[i]
}
// prefix = [0, 1, 3, 6, 10, 15]
// sum from index l to r (inclusive) = prefix[r+1] - prefix[l]
```

### Difference Array

```kotlin
val diff = IntArray(n + 1)
// To add val v to range [l, r] inclusive:
diff[l] += v
diff[r + 1] -= v
// After all updates, prefix sum of diff gives the final array
```

### Frequency Array (for lowercase letters)

```kotlin
val freq = IntArray(26)          // index 0 = 'a', index 25 = 'z'
for (ch in s) freq[ch - 'a']++
```

### Swap Two Elements

```kotlin
// Option 1 — temp variable
val temp = arr[i]; arr[i] = arr[j]; arr[j] = temp

// Option 2 — also idiom
arr[i] = arr[j].also { arr[j] = arr[i] }
```

## 3.8 Array Traps

```kotlin
// ❌ Trap 1 — arr.sorted() returns List, NOT IntArray
val sorted: List<Int> = arr.sorted()      // cannot use as IntArray
// ✅ Use arr.sort() to sort in place, or .sorted().toIntArray()

// ❌ Trap 2 — comparing arrays with ==
arr1 == arr2      // compares REFERENCE, not contents
// ✅ Use
arr1.contentEquals(arr2)              // for 1D
arr1.contentDeepEquals(arr2)          // for 2D

// ❌ Trap 3 — copying array with =
val copy = arr    // not a copy — both point to same array!
// ✅ Use
val copy = arr.copyOf()

// ⚠️ Trap 4 — IntArray vs Array<Int> in function signatures
fun process(arr: IntArray) { }    // takes primitive int[]
fun process(arr: Array<Int>) { }  // takes boxed Integer[]
// These are NOT interchangeable

// ⚠️ Trap 5 — 2D array shorthand does NOT work for IntArray rows
val wrong = Array(3) { IntArray(4) }     // ✅ correct
// intArrayOf() can't create 2D directly
```

---

# 4. Lists — MutableList / List

## 4.1 Creation

```kotlin
// Immutable — read-only
listOf(1, 2, 3)              // List<Int>
listOf<Int>()                // empty immutable list
emptyList<Int>()             // same

// Mutable — read + write (use this for DSA)
mutableListOf(1, 2, 3)       // MutableList<Int>
mutableListOf<Int>()         // empty mutable list
ArrayList<Int>()             // same as MutableList (Java-style)
ArrayList<Int>(capacity)     // pre-allocated capacity (no size change yet)

// From array
intArrayOf(1, 2, 3).toList()          // List<Int>
intArrayOf(1, 2, 3).toMutableList()   // MutableList<Int>

// From range
(1..5).toMutableList()       // [1, 2, 3, 4, 5]

// With init
MutableList(5) { 0 }         // [0, 0, 0, 0, 0]
MutableList(5) { it * 2 }    // [0, 2, 4, 6, 8]
```

## 4.2 Adding Elements

|Method|What it does|Notes|
|---|---|---|
|`list.add(e)`|append e to end|O(1) amortized|
|`list.add(i, e)`|insert e at index i|O(n) — shifts elements|
|`list.addAll(col)`|append all from collection||
|`list.addAll(i, col)`|insert collection at index i|O(n)|
|`list.addFirst(e)`|add to front|O(n) — use ArrayDeque if frequent|
|`list.addLast(e)`|add to end|same as `add(e)`|

```kotlin
val list = mutableListOf(1, 2, 3)
list.add(4)          // [1, 2, 3, 4]
list.add(1, 99)      // [1, 99, 2, 3, 4]
list.addAll(listOf(5, 6))   // [1, 99, 2, 3, 4, 5, 6]
```

## 4.3 Removing Elements

|Method|Returns|What it does|Notes|
|---|---|---|---|
|`list.remove(e)`|`Boolean`|remove first occurrence of VALUE e|O(n)|
|`list.removeAt(i)`|`T`|remove at INDEX i, returns removed element|O(n)|
|`list.removeFirst()`|`T`|remove first element, return it|throws if empty|
|`list.removeLast()`|`T`|remove last element, return it|throws if empty|
|`list.removeFirstOrNull()`|`T?`|safe version|null if empty|
|`list.removeLastOrNull()`|`T?`|safe version|null if empty|
|`list.removeAll { pred }`|`Boolean`|remove all matching predicate||
|`list.removeAll(col)`|`Boolean`|remove all elements in col||
|`list.retainAll { pred }`|`Boolean`|keep only matching, remove rest||
|`list.clear()`|`Unit`|remove all elements||

```kotlin
val list = mutableListOf(10, 20, 30, 20, 40)
list.remove(20)       // removes first 20 → [10, 30, 20, 40]
list.removeAt(0)      // removes index 0 → [30, 20, 40]
list.removeLast()     // removes 40 → [30, 20]
```

> ⚠️ `remove(e)` removes by **value**. `removeAt(i)` removes by **index**. Don't confuse them.

## 4.4 Accessing Elements

|Method|Returns|What it does|Notes|
|---|---|---|---|
|`list[i]`|`T`|get at index|throws if out of bounds|
|`list.get(i)`|`T`|same as `list[i]`||
|`list.getOrNull(i)`|`T?`|null if index out of bounds|safe ← use this|
|`list.getOrElse(i) { default }`|`T`|default if out of bounds||
|`list.first()`|`T`|first element|throws if empty|
|`list.last()`|`T`|last element|throws if empty|
|`list.firstOrNull()`|`T?`|null if empty||
|`list.lastOrNull()`|`T?`|null if empty||
|`list.first { pred }`|`T`|first matching|throws if none|
|`list.firstOrNull { pred }`|`T?`|null if none match||
|`list.last { pred }`|`T`|last matching|throws if none|
|`list.lastOrNull { pred }`|`T?`|null if none match||

## 4.5 List Information Methods

|Method|Returns|What it does|
|---|---|---|
|`list.size`|`Int`|number of elements|
|`list.indices`|`IntRange`|`0 until list.size`|
|`list.lastIndex`|`Int`|`list.size - 1`|
|`list.isEmpty()`|`Boolean`||
|`list.isNotEmpty()`|`Boolean`||
|`list.contains(e)`|`Boolean`|O(n) linear scan|
|`e in list`|`Boolean`|same|
|`e !in list`|`Boolean`|not present|
|`list.containsAll(col)`|`Boolean`|all elements of col in list|
|`list.indexOf(e)`|`Int`|first index of e, -1 if not found|
|`list.lastIndexOf(e)`|`Int`|last index of e|
|`list.indexOfFirst { pred }`|`Int`|first index matching pred|
|`list.indexOfLast { pred }`|`Int`|last index matching pred|
|`list.count()`|`Int`|same as size|
|`list.count { pred }`|`Int`|count matching predicate|

## 4.6 List Modification (In Place)

|Method|Returns|What it does|
|---|---|---|
|`list[i] = e`||set element at index|
|`list.set(i, e)`|`T`|same, returns old value|
|`list.sort()`|`Unit`|sort ascending IN PLACE|
|`list.sortDescending()`|`Unit`|sort descending IN PLACE|
|`list.sortBy { key }`|`Unit`|sort by key IN PLACE|
|`list.sortByDescending { key }`|`Unit`|sort by key desc IN PLACE|
|`list.sortWith(comparator)`|`Unit`|custom sort IN PLACE|
|`list.reverse()`|`Unit`|reverse IN PLACE|
|`list.shuffle()`|`Unit`|random shuffle IN PLACE|
|`list.fill(e)`|`Unit`|set all elements to e|
|`list.replaceAll { transform }`|`Unit`|replace each element in place|

## 4.7 List Transformation (Returns New Collection)

|Method|Returns|What it does|
|---|---|---|
|`list.sorted()`|`List<T>`|new sorted ascending|
|`list.sortedDescending()`|`List<T>`|new sorted descending|
|`list.sortedBy { key }`|`List<T>`|new sorted by key|
|`list.sortedByDescending { key }`|`List<T>`|new sorted by key desc|
|`list.sortedWith(comp)`|`List<T>`|new sorted by comparator|
|`list.reversed()`|`List<T>`|new reversed list|
|`list.distinct()`|`List<T>`|remove duplicates (keep first)|
|`list.distinctBy { key }`|`List<T>`|deduplicate by key|
|`list.subList(f, t)`|`List<T>`|view of index f..t-1 ⚠️ see trap|
|`list.take(n)`|`List<T>`|first n elements|
|`list.takeLast(n)`|`List<T>`|last n elements|
|`list.takeWhile { pred }`|`List<T>`|take while predicate true|
|`list.drop(n)`|`List<T>`|skip first n|
|`list.dropLast(n)`|`List<T>`|skip last n|
|`list.dropWhile { pred }`|`List<T>`|skip while predicate true|
|`list.filter { pred }`|`List<T>`|keep matching|
|`list.filterNot { pred }`|`List<T>`|keep NOT matching|
|`list.map { transform }`|`List<R>`|transform each element|
|`list.flatMap { }`|`List<R>`|map then flatten|
|`list.chunked(n)`|`List<List<T>>`|split into groups of n|
|`list.windowed(n)`|`List<List<T>>`|sliding windows of size n|
|`list.zip(other)`|`List<Pair<T,U>>`|pair elements by index|
|`list.zipWithNext()`|`List<Pair<T,T>>`|consecutive pairs|

## 4.8 List Looping Patterns

### Index Loop

```kotlin
for (i in list.indices) {
    val value = list[i]
}
```

> Use when: two pointers, need index for math, modifying by index.

### Element Loop

```kotlin
for (value in list) { }
```

> Use when: read-only, summing, searching.

### Index + Value

```kotlin
for ((i, value) in list.withIndex()) { }
```

> Use when: need both index and value.

### Reverse Loop

```kotlin
for (i in list.lastIndex downTo 0) {
    val value = list[i]
}
```

### Nested (2D List)

```kotlin
for (row in matrix) {
    for (cell in row) { }
}

for (i in matrix.indices) {
    for (j in matrix[i].indices) {
        matrix[i][j]
    }
}
```

## 4.9 Conversion Methods

|Method|Returns|What it does|
|---|---|---|
|`list.toIntArray()`|`IntArray`|List<Int> → IntArray|
|`list.toLongArray()`|`LongArray`||
|`list.toTypedArray()`|`Array<T>`|List<T> → Array<T>|
|`list.toSet()`|`Set<T>`|removes duplicates|
|`list.toMutableSet()`|`MutableSet<T>`||
|`list.toHashSet()`|`HashSet<T>`||
|`list.toSortedSet()`|`SortedSet<T>`|sorted unique|
|`list.toMutableList()`|`MutableList<T>`|copy to mutable|
|`list.joinToString(sep)`|`String`|combine elements to string|

## 4.10 List Performance Notes

|Operation|MutableList|Note|
|---|---|---|
|`add(e)` at end|O(1) amortized|fast — use freely|
|`add(i, e)` at middle|O(n)|shifts all after i|
|`addFirst(e)`|O(n)|use `ArrayDeque` if frequent|
|`removeAt(i)` middle|O(n)|shifts all after i|
|`removeLast()`|O(1)|fast|
|`removeFirst()`|O(n)|use `ArrayDeque` if frequent|
|`get(i)` / `list[i]`|O(1)|fast|
|`contains(e)`|O(n)|use `HashSet` if frequent|

---

# 5. HashMap — Map / MutableMap

## 5.1 Creation

```kotlin
// Mutable — use for DSA (read + write)
mutableMapOf<String, Int>()                   // empty, Kotlin-preferred style
mutableMapOf("a" to 1, "b" to 2)            // with initial entries
HashMap<String, Int>()                         // same, Java style
hashMapOf("a" to 1, "b" to 2)               // shorthand

// Immutable — read-only
mapOf("a" to 1, "b" to 2)

// Insertion-order preserved
linkedMapOf<String, Int>()
LinkedHashMap<String, Int>()

// Always sorted by key — use TreeMap (Chapter 21)
// import java.util.TreeMap
TreeMap<Int, String>()
```

> ⚠️ `mapOf()` is **read-only**. Calling `.put()` or `map[key] = value` on it will throw `UnsupportedOperationException`. Always use `mutableMapOf()` for DSA.

## 5.2 Writing Operations

|Method|Returns|What it does|
|---|---|---|
|`map[key] = value`||put or update|
|`map.put(key, value)`|`V?`|same; returns old value or null|
|`map.putAll(otherMap)`|`Unit`|copy all entries from otherMap|
|`map.putIfAbsent(key, value)`|`V?`|insert only if key not present; returns existing value|
|`map.getOrPut(key) { default }`|`V`|return value if exists; else insert default and return it|
|`map.remove(key)`|`V?`|remove key; returns old value or null|
|`map.remove(key, value)`|`Boolean`|remove only if current value matches|
|`map.clear()`|`Unit`|remove all entries|

### Safe Increment — the Most Common Map Pattern

```kotlin
// ❌ Wrong — crashes if key not present (map[key] returns null)
map[key] = map[key]!! + 1

// ✅ Option 1 — Elvis operator
map[key] = (map[key] ?: 0) + 1

// ✅ Option 2 — getOrDefault
map[key] = map.getOrDefault(key, 0) + 1

// ✅ Option 3 — getOrPut (inserts 0 first if missing, then you update)
map.getOrPut(key) { 0 }
map[key] = map[key]!! + 1    // safe now because getOrPut guarantees it exists
```

## 5.3 Reading Operations

|Method|Returns|What it does|Notes|
|---|---|---|---|
|`map[key]`|`V?`|value or **null** if not found|always nullable!|
|`map.get(key)`|`V?`|same||
|`map.getOrDefault(key, default)`|`V`|value or default ← most used||
|`map.getOrElse(key) { default }`|`V`|value or lambda result||
|`map.getValue(key)`|`V`|value — throws if key missing|only if you're certain|

> ⚠️ `map[key]` always returns a nullable type `V?`, even if the value cannot actually be null. You must always handle the null case.

## 5.4 Checking Methods

|Method|Returns|What it does|
|---|---|---|
|`map.containsKey(key)`|`Boolean`|O(1)|
|`key in map`|`Boolean`|same as containsKey|
|`key !in map`|`Boolean`|key not present|
|`map.containsValue(value)`|`Boolean`|O(n) — linear scan through values|
|`map.isEmpty()`|`Boolean`||
|`map.isNotEmpty()`|`Boolean`||
|`map.size`|`Int`|number of key-value pairs|

## 5.5 Map Iteration

### Key + Value (most common)

```kotlin
for ((key, value) in map) {
    println("$key → $value")
}
```

### Keys Only

```kotlin
for (key in map.keys) { }
```

### Values Only

```kotlin
for (value in map.values) { }
```

### Entry Object

```kotlin
for (entry in map.entries) {
    entry.key
    entry.value
}
```

### forEach

```kotlin
map.forEach { (key, value) ->
    println("$key → $value")
}
```

> ⚠️ `forEach` has the same return-trap as list `forEach`. Cannot `break`. Cannot `return` from outer function cleanly. Use `for ((k, v) in map)` when you need those.

### Sorted Iteration

```kotlin
// Iterate in key order
for ((k, v) in map.entries.sortedBy { it.key }) { }

// Iterate in value order (e.g. frequency descending)
for ((k, v) in map.entries.sortedByDescending { it.value }) { }
```

## 5.6 Map Transformations

|Method|Returns|What it does|
|---|---|---|
|`map.filter { (k,v) -> pred }`|`Map<K,V>`|new map with entries matching pred|
|`map.filterKeys { pred }`|`Map<K,V>`|new map filtering by keys|
|`map.filterValues { pred }`|`Map<K,V>`|new map filtering by values|
|`map.mapValues { (k,v) -> expr }`|`Map<K,V2>`|new map, values transformed|
|`map.mapKeys { (k,v) -> expr }`|`Map<K2,V>`|new map, keys transformed|
|`map.entries`|`Set<Map.Entry<K,V>>`|set of key-value entry objects|
|`map.entries.toList()`|`List<Map.Entry<K,V>>`|sortable list of entries|
|`map.keys`|`Set<K>`|all keys as a Set|
|`map.values`|`Collection<V>`|all values (duplicates allowed)|
|`map.toList()`|`List<Pair<K,V>>`|list of key-value pairs|
|`map.toSortedMap()`|`SortedMap<K,V>`|sorted by key (TreeMap)|

## 5.7 Frequency Map Patterns

### Manual Build

```kotlin
val freq = mutableMapOf<Char, Int>()
for (ch in s) {
    freq[ch] = (freq[ch] ?: 0) + 1
}
```

### One-liner with groupingBy

```kotlin
val freq: Map<Char, Int> = s.groupingBy { it }.eachCount()
```

### groupBy (gives list of elements, not count)

```kotlin
val groups: Map<Int, List<Int>> = nums.groupBy { it }
// groups[2] = [2, 2, 2] — all occurrences grouped
```

### Max frequency element

```kotlin
val maxEntry = freq.maxByOrNull { it.value }
// maxEntry?.key = element with highest count
// maxEntry?.value = count
```

### Sort entries by frequency

```kotlin
freq.entries.sortedByDescending { it.value }
```

## 5.8 Map Traps

```kotlin
// ❌ Trap 1 — map[key] is NULLABLE even if value type is non-null
val map = mutableMapOf("a" to 1)
val v: Int = map["a"]        // ❌ compile error — type is Int?, not Int
val v: Int = map["a"]!!      // ✅ force — only if you're certain key exists
val v: Int = map["a"] ?: 0   // ✅ safe — use default if null

// ❌ Trap 2 — using mapOf() and then trying to write to it
val map = mapOf("a" to 1)
map["b"] = 2    // ❌ throws UnsupportedOperationException at runtime
// ✅ Use mutableMapOf()

// ❌ Trap 3 — iterating and modifying map at the same time
for (key in map.keys) {
    map.remove(key)    // ❌ ConcurrentModificationException
}
// ✅ Collect keys first, then remove
val toRemove = map.keys.filter { pred }
toRemove.forEach { map.remove(it) }

// ⚠️ Trap 4 — containsValue is O(n) not O(1)
map.containsValue(x)    // scans ALL values — slow for large maps
// Use a reverse map (value→key) if you need O(1) value lookup

// ⚠️ Trap 5 — HashMap iteration order is NOT guaranteed
// If you need insertion order: use LinkedHashMap
// If you need sorted order: use TreeMap or sort entries each time
```

---

## CHAPTER SUMMARY — Quick Lookup

### When to Use Which Structure (Chapters 3–5 scope)

|You need|Use|
|---|---|
|Fixed-size numbers, fast access|`IntArray`|
|Fixed-size booleans|`BooleanArray`|
|Dynamic sequence, add/remove|`MutableList<T>`|
|Key → Value lookup O(1)|`HashMap` / `mutableMapOf()`|
|Key → Value, insertion order|`LinkedHashMap`|
|Count occurrences of elements|`HashMap<T, Int>` with frequency pattern|
|Check existence only, O(1)|`HashSet` (Chapter 6)|

### In-Place vs New Collection

|Operation|In-place (mutates)|New (original unchanged)|
|---|---|---|
|Sort list|`list.sort()`|`list.sorted()`|
|Sort array|`arr.sort()`|`arr.sorted()` → List|
|Reverse list|`list.reverse()`|`list.reversed()`|
|Reverse array|`arr.reverse()`|`arr.reversed()` → List|
|Reverse string|not possible|`str.reversed()`|

---

_End of Chapters 1–5 | Next: Ch 6 HashSet · Ch 7 Stack · Ch 8 Queue & Deque · Ch 9 PriorityQueue · Ch 10 String & StringBuilder_

# Kotlin DSA Reference Guide

### Chapters 6–10 | Pure API & Syntax Reference | No Problem Solutions

---

## MASTER INDEX (This File)

|#|Chapter|
|---|---|
|6|[HashSet](#6-hashset)|
|7|[Stack — ArrayDeque](#7-stack--arraydeque)|
|8|[Queue & Deque — ArrayDeque](#8-queue--deque--arraydeque)|
|9|[PriorityQueue — Heap](#9-priorityqueue--heap)|
|10|[String & StringBuilder](#10-string--stringbuilder)|

---

# 6. HashSet

## 6.1 Creation

```kotlin
// Mutable — read + write (use for DSA)
mutableSetOf<Int>()                    // empty — Kotlin preferred style
mutableSetOf(1, 2, 3)                  // with initial values
HashSet<Int>()                         // same, Java style
hashSetOf(1, 2, 3)                     // shorthand with values
HashSet<Int>(initialCapacity)          // pre-allocated (performance hint)
HashSet<Int>(initialCapacity, loadFactor)

// Immutable — read-only
setOf(1, 2, 3)
setOf<Int>()
emptySet<Int>()

// Insertion-order preserved (iterates in insertion order)
linkedSetOf(1, 2, 3)
LinkedHashSet<Int>()

// Always sorted (TreeSet — uses red-black tree)
sortedSetOf(3, 1, 2)                   // {1, 2, 3} — always sorted
// import java.util.TreeSet
TreeSet<Int>()
```

### From Other Collections

```kotlin
listOf(1, 2, 2, 3, 3).toMutableSet()      // {1, 2, 3} — removes duplicates
listOf(1, 2, 2, 3).toHashSet()            // {1, 2, 3}
intArrayOf(1, 2, 2, 3).toHashSet()        // {1, 2, 3}
(1..5).toMutableSet()                      // {1, 2, 3, 4, 5}
"hello".toSet()                            // Set<Char> {h, e, l, o} — note: 'l' once
```

> ⚠️ `setOf()` is **read-only**. Calling `.add()` or `.remove()` on it throws `UnsupportedOperationException`. Always use `mutableSetOf()` or `HashSet()` for DSA.

---

## 6.2 Basic Methods

### Adding

|Method|Returns|What it does|Notes|
|---|---|---|---|
|`set.add(e)`|`Boolean`|adds e; returns `true` if added, `false` if already present|O(1) avg|
|`set.addAll(col)`|`Boolean`|adds all from collection; returns true if set changed||

```kotlin
val set = mutableSetOf(1, 2, 3)
set.add(4)      // true  — added,   set = {1,2,3,4}
set.add(2)      // false — already present, set unchanged
set.addAll(listOf(5, 6, 2))   // true — 5 and 6 added, 2 skipped
```

### Removing

|Method|Returns|What it does|Notes|
|---|---|---|---|
|`set.remove(e)`|`Boolean`|removes e; returns `true` if removed, `false` if not present|O(1) avg|
|`set.removeAll(col)`|`Boolean`|removes all elements in col||
|`set.removeAll { pred }`|`Boolean`|removes all matching predicate||
|`set.retainAll(col)`|`Boolean`|keeps only elements in col, removes rest||
|`set.retainAll { pred }`|`Boolean`|keeps only matching, removes rest||
|`set.clear()`|`Unit`|removes all elements||

```kotlin
val set = mutableSetOf(1, 2, 3, 4, 5)
set.remove(3)           // true  — {1,2,4,5}
set.remove(99)          // false — not present, set unchanged
set.removeAll(setOf(1, 2))   // true — {4,5}
set.retainAll(setOf(4, 10))  // true — {4}
```

### Checking / Reading

|Method|Returns|What it does|Notes|
|---|---|---|---|
|`set.contains(e)`|`Boolean`|O(1) average lookup|fast — use over List.contains|
|`e in set`|`Boolean`|same as contains|preferred Kotlin style|
|`e !in set`|`Boolean`|not present||
|`set.containsAll(col)`|`Boolean`|all elements of col in set||
|`set.isEmpty()`|`Boolean`|||
|`set.isNotEmpty()`|`Boolean`|||
|`set.size`|`Int`|number of elements||
|`set.first()`|`T`|first in iteration order|throws if empty|
|`set.last()`|`T`|last in iteration order|throws if empty|
|`set.firstOrNull()`|`T?`|null if empty||
|`set.lastOrNull()`|`T?`|null if empty||
|`set.elementAt(i)`|`T`|element at position i|O(n) — avoid in hot loops|
|`set.elementAtOrNull(i)`|`T?`|null if out of bounds||
|`set.random()`|`T`|random element||
|`set.count()`|`Int`|same as size||
|`set.count { pred }`|`Int`|count matching predicate||
|`set.any { pred }`|`Boolean`|true if any element matches||
|`set.all { pred }`|`Boolean`|true if all elements match||
|`set.none { pred }`|`Boolean`|true if no elements match||

---

## 6.3 Set Operations

> Kotlin has **infix operators** for set math. These return immutable `Set<T>`. To get a mutable result, call `.toMutableSet()` on the result.

### Union — all elements from both sets

```kotlin
val a = setOf(1, 2, 3, 4)
val b = setOf(3, 4, 5, 6)

val result = a union b          // {1, 2, 3, 4, 5, 6}
val result2 = a + b             // same — plus operator also works
```

### Intersection — elements present in BOTH sets

```kotlin
val result = a intersect b      // {3, 4}
```

### Difference — elements in a but NOT in b

```kotlin
val result = a subtract b       // {1, 2}   — in a, not in b
val result2 = a - b             // same
val result3 = b subtract a      // {5, 6}   — in b, not in a
```

### Subset Check

```kotlin
// Is {3, 4} a subset of b?
b.containsAll(setOf(3, 4))      // true
setOf(3, 4).all { it in b }     // same
```

### Summary Table

|Operation|Infix / Operator|Returns|
|---|---|---|
|Union|`a union b` or `a + b`|`Set<T>`|
|Intersection|`a intersect b`|`Set<T>`|
|Difference (a − b)|`a subtract b` or `a - b`|`Set<T>`|
|Add one element|`a + element`|`Set<T>`|
|Remove one element|`a - element`|`Set<T>`|

> ⚠️ All set math operators return **immutable** `Set<T>`. If you need to add/remove further, do `.toMutableSet()`.

---

## 6.4 Set Looping

### Basic Element Loop

```kotlin
val set = mutableSetOf(3, 1, 2)

for (e in set) {
    println(e)
}
```

> ⚠️ `HashSet` does **not** guarantee iteration order. If you need:
> 
> - insertion order → use `LinkedHashSet`
> - sorted order → use `TreeSet` or `set.sorted()`

### forEach

```kotlin
set.forEach { e -> println(e) }
set.forEach { println(it) }
```

> ⚠️ Same `forEach` trap as List — cannot `break`, `return@forEach` only skips current element (like `continue`). Use `for (e in set)` when you need `break`.

### Sorted Iteration

```kotlin
for (e in set.sorted()) { }              // iterate in ascending order
for (e in set.sortedDescending()) { }    // descending order
```

### No Index in Set

```kotlin
// ❌ Sets have no index — this does NOT exist:
set[0]           // compile error

// ✅ If you need index + value:
for ((i, e) in set.withIndex()) {
    println("$i: $e")
}
// ⚠️ Index here is just iteration count, NOT a stable position
```

---

## 6.5 Set Conversion

|Method|Returns|What it does|
|---|---|---|
|`set.toList()`|`List<T>`|order not guaranteed for HashSet|
|`set.toMutableList()`|`MutableList<T>`||
|`set.sorted()`|`List<T>`|sorted ascending list|
|`set.sortedDescending()`|`List<T>`|sorted descending list|
|`set.sortedBy { key }`|`List<T>`|sorted by key|
|`set.toIntArray()`|`IntArray`|only for `Set<Int>`|
|`set.toLongArray()`|`LongArray`|only for `Set<Long>`|
|`set.toSortedSet()`|`SortedSet<T>`|TreeSet — always sorted|
|`set.toHashSet()`|`HashSet<T>`|copy to HashSet|
|`set.toTypedArray()`|`Array<T>`||
|`set.joinToString(sep)`|`String`|combine to string|

---

## 6.6 Set Use Cases

### When to Use HashSet Instead of List

|Situation|List|HashSet|
|---|---|---|
|Check if element exists|O(n)|O(1) ← use Set|
|Add without duplicates|O(n) to check|O(1) ← use Set|
|Count unique elements|O(n) + sort|O(n) ← use Set|
|Need index access|✅|❌|
|Need order preserved|✅|❌ (use LinkedHashSet)|

### Duplicate Detection

```kotlin
// add() returns false if element already exists
val seen = HashSet<Int>()
for (n in nums) {
    if (!seen.add(n)) {
        // n is a duplicate
    }
}

// Alternative: compare sizes
val hasDuplicate = nums.size != nums.toHashSet().size
```

### Visited Tracking (BFS / DFS)

```kotlin
val visited = HashSet<Int>()

if (node !in visited) {
    visited.add(node)
    // process node
}
```

### Visited for Grid (row, col) State

```kotlin
val visited = HashSet<Pair<Int, Int>>()

if (Pair(r, c) !in visited) {
    visited.add(Pair(r, c))
}

// Or use a string key
val visited = HashSet<String>()
if ("$r,$c" !in visited) {
    visited.add("$r,$c")
}
```

### Fast Lookup from Array/List

```kotlin
val nums = intArrayOf(3, 1, 4, 1, 5, 9)
val numSet = nums.toHashSet()       // build once — O(n)
numSet.contains(4)                  // O(1) lookup
```

---

# 7. Stack — ArrayDeque

## Why Not `java.util.Stack`?

||`java.util.Stack`|`ArrayDeque`|
|---|---|---|
|Thread-safe|✅ synchronized|❌ not synchronized|
|Speed|slower (lock overhead)|faster|
|API clarity|confusing (inherits Vector)|clean|
|Kotlin idiomatic|❌|✅|

> **Rule:** Always use `ArrayDeque` for stack in Kotlin. `java.util.Stack` is legacy. Mental model: **last = top of stack**.

---

## 7.1 Creation

```kotlin
val stack = ArrayDeque<Int>()
val stack = ArrayDeque<String>()
val stack = ArrayDeque<Pair<Int,Int>>()     // for coordinate states

// Pre-fill
val stack = ArrayDeque(listOf(1, 2, 3))    // [1, 2, 3] — 3 is top
```

---

## 7.2 Stack Operations

### Push — Add to Top

|Method|Returns|What it does|
|---|---|---|
|`stack.addLast(e)`|`Unit`|push e onto top of stack|

```kotlin
val stack = ArrayDeque<Int>()
stack.addLast(10)    // stack: [10]       — top = 10
stack.addLast(20)    // stack: [10, 20]   — top = 20
stack.addLast(30)    // stack: [10, 20, 30] — top = 30
```

### Pop — Remove from Top

|Method|Returns|What it does|Notes|
|---|---|---|---|
|`stack.removeLast()`|`T`|remove and return top element|throws `NoSuchElementException` if empty|
|`stack.removeLastOrNull()`|`T?`|remove and return top; null if empty|safe version|

```kotlin
stack.removeLast()          // 30 — removed from top, stack: [10, 20]
stack.removeLastOrNull()    // 20 — safe pop
stack.removeLastOrNull()    // 10
stack.removeLastOrNull()    // null — stack is empty, no crash
```

### Peek — Look at Top Without Removing

|Method|Returns|What it does|Notes|
|---|---|---|---|
|`stack.last()`|`T`|return top element (no remove)|throws if empty|
|`stack.lastOrNull()`|`T?`|return top or null|safe version|

```kotlin
stack.addLast(5)
stack.addLast(10)
stack.last()            // 10 — top, stack unchanged: [5, 10]
stack.lastOrNull()      // 10 — safe peek
```

### Info

|Method|Returns|What it does|
|---|---|---|
|`stack.isEmpty()`|`Boolean`|true if no elements|
|`stack.isNotEmpty()`|`Boolean`|true if has elements|
|`stack.size`|`Int`|number of elements|

### Complete Quick Reference

```kotlin
val stack = ArrayDeque<Int>()

stack.addLast(e)          // PUSH
stack.removeLast()        // POP  (throws if empty)
stack.removeLastOrNull()  // POP  (safe — null if empty)
stack.last()              // PEEK (throws if empty)
stack.lastOrNull()        // PEEK (safe — null if empty)
stack.isEmpty()           // check empty
stack.isNotEmpty()        // check not empty
stack.size                // count
```

---

## 7.3 Stack Iteration

> Stack iteration with `for` goes **bottom → top** (front → back of internal array).

```kotlin
val stack = ArrayDeque<Int>()
stack.addLast(1); stack.addLast(2); stack.addLast(3)

// Bottom to top (default iteration)
for (e in stack) {
    print(e)    // 1 2 3
}

// Top to bottom (reverse)
for (e in stack.reversed()) {
    print(e)    // 3 2 1
}

// Just peek all elements without removing — use toList()
val snapshot = stack.toList()     // [1, 2, 3]  — copy, stack unchanged
```

> ⚠️ Iterating does NOT remove elements. Use `removeLast()` in a while loop to drain.

```kotlin
// Drain entire stack
while (stack.isNotEmpty()) {
    val top = stack.removeLast()
    // process top
}
```

---

## 7.4 Monotonic Stack Pattern

> A Monotonic Stack is a stack that stays in sorted order (increasing or decreasing). This section is **syntax-only** — the pattern itself, not a problem solution.

### Structure: Monotonic Decreasing Stack (stores indices)

```kotlin
val stack = ArrayDeque<Int>()   // stores INDICES, not values

for (i in arr.indices) {
    // While top of stack is less than current — pop (violates decreasing order)
    while (stack.isNotEmpty() && arr[stack.last()] < arr[i]) {
        val poppedIndex = stack.removeLast()
        // at this point: arr[poppedIndex] < arr[i]
        // do something with poppedIndex and i
    }
    stack.addLast(i)
}
```

### Structure: Monotonic Increasing Stack (stores indices)

```kotlin
val stack = ArrayDeque<Int>()   // stores INDICES

for (i in arr.indices) {
    // While top of stack is greater than current — pop
    while (stack.isNotEmpty() && arr[stack.last()] > arr[i]) {
        val poppedIndex = stack.removeLast()
        // do something
    }
    stack.addLast(i)
}
```

### When Each Stack Type Is Used

|Stack Type|Pop condition|Useful for|
|---|---|---|
|Monotonic Decreasing|current > top|next greater element|
|Monotonic Increasing|current < top|next smaller element|
|Monotonic Decreasing|current >= top|next greater or equal|
|Monotonic Increasing|current <= top|next smaller or equal|

> The stack always stores **indices** (not values) so you can calculate distances and access values via `arr[index]`.

---

## 7.5 Stack Traps

```kotlin
// ❌ Trap 1 — removeLast() on empty stack throws NoSuchElementException
val stack = ArrayDeque<Int>()
stack.removeLast()    // ❌ throws!
// ✅ Always check first OR use safe version
if (stack.isNotEmpty()) stack.removeLast()
stack.removeLastOrNull()

// ❌ Trap 2 — last() on empty stack throws NoSuchElementException
stack.last()          // ❌ throws if empty
// ✅ Use
stack.lastOrNull()

// ❌ Trap 3 — confusing iteration direction
for (e in stack) { }   // goes BOTTOM to TOP — not top to bottom!
// ✅ For top to bottom:
for (e in stack.reversed()) { }

// ❌ Trap 4 — using java.util.Stack
import java.util.Stack
val s = Stack<Int>()
s.push(1); s.peek(); s.pop()   // works but SLOW and legacy
// ✅ Use ArrayDeque

// ⚠️ Trap 5 — stack.contains() is O(n) linear scan
// If you frequently check "is X in the stack", maintain a separate HashSet
val stack = ArrayDeque<Int>()
val inStack = HashSet<Int>()
stack.addLast(x); inStack.add(x)      // push
stack.removeLast(); inStack.remove(x)  // pop
inStack.contains(x)                    // O(1) check
```

---

# 8. Queue & Deque — ArrayDeque

## Mental Model

```
Queue  — add to BACK, remove from FRONT (FIFO — First In First Out)
Stack  — add to TOP (back), remove from TOP (back) (LIFO)
Deque  — add/remove from BOTH ends

ArrayDeque naming:
  "First" = front of queue
  "Last"  = back of queue / top of stack
```

---

## 8.1 Queue Operations (FIFO)

### Creation

```kotlin
val queue = ArrayDeque<Int>()
val queue = ArrayDeque<String>()
val queue = ArrayDeque<IntArray>()       // for BFS with state arrays
```

### Core Queue Methods

|Operation|Method|Returns|Notes|
|---|---|---|---|
|Enqueue (add to back)|`queue.addLast(e)`|`Unit`||
|Dequeue (remove from front)|`queue.removeFirst()`|`T`|throws if empty|
|Dequeue safe|`queue.removeFirstOrNull()`|`T?`|null if empty|
|Peek front|`queue.first()`|`T`|no remove; throws if empty|
|Peek front safe|`queue.firstOrNull()`|`T?`|null if empty|
|Is empty|`queue.isEmpty()`|`Boolean`||
|Is not empty|`queue.isNotEmpty()`|`Boolean`||
|Size|`queue.size`|`Int`||

```kotlin
val queue = ArrayDeque<Int>()

queue.addLast(10)       // enqueue → queue: [10]
queue.addLast(20)       // enqueue → queue: [10, 20]
queue.addLast(30)       // enqueue → queue: [10, 20, 30]

queue.first()           // 10 — peek front (no remove)
queue.removeFirst()     // 10 — dequeue  → queue: [20, 30]
queue.removeFirst()     // 20 — dequeue  → queue: [30]
```

### Quick Reference

```kotlin
queue.addLast(e)           // ENQUEUE — add to back
queue.removeFirst()        // DEQUEUE — remove from front (throws if empty)
queue.removeFirstOrNull()  // DEQUEUE — safe (null if empty)
queue.first()              // PEEK front (throws if empty)
queue.firstOrNull()        // PEEK front safe
queue.isEmpty()
queue.size
```

---

## 8.2 Deque Operations (Double-Ended Queue)

> A Deque lets you add and remove from **both** ends efficiently — O(1) for all four operations.

### All Deque Methods

|Operation|Method|Returns|Notes|
|---|---|---|---|
|Add to front|`deque.addFirst(e)`|`Unit`|O(1)|
|Add to back|`deque.addLast(e)`|`Unit`|O(1)|
|Remove from front|`deque.removeFirst()`|`T`|O(1); throws if empty|
|Remove from back|`deque.removeLast()`|`T`|O(1); throws if empty|
|Remove front safe|`deque.removeFirstOrNull()`|`T?`|O(1); null if empty|
|Remove back safe|`deque.removeLastOrNull()`|`T?`|O(1); null if empty|
|Peek front|`deque.first()`|`T`|throws if empty|
|Peek back|`deque.last()`|`T`|throws if empty|
|Peek front safe|`deque.firstOrNull()`|`T?`|null if empty|
|Peek back safe|`deque.lastOrNull()`|`T?`|null if empty|
|Size|`deque.size`|`Int`||
|Is empty|`deque.isEmpty()`|`Boolean`||
|Contains|`deque.contains(e)`|`Boolean`|O(n)|

```kotlin
val deque = ArrayDeque<Int>()

deque.addLast(1)     // [1]
deque.addLast(2)     // [1, 2]
deque.addFirst(0)    // [0, 1, 2]
deque.addFirst(-1)   // [-1, 0, 1, 2]

deque.first()        // -1 — peek front
deque.last()         // 2  — peek back
deque.removeFirst()  // -1 — remove front → [0, 1, 2]
deque.removeLast()   // 2  — remove back  → [0, 1]
```

### ArrayDeque as Each Data Structure

```kotlin
// As STACK (LIFO)
deque.addLast(e)      // push
deque.removeLast()    // pop
deque.last()          // peek top

// As QUEUE (FIFO)
deque.addLast(e)       // enqueue
deque.removeFirst()    // dequeue
deque.first()          // peek front

// As DEQUE (both ends)
deque.addFirst(e)      // add front
deque.addLast(e)       // add back
deque.removeFirst()    // remove front
deque.removeLast()     // remove back
```

---

## 8.3 BFS Queue Pattern

> This is the **structural template** for BFS — the queue API usage, not a solution.

### Standard BFS Structure

```kotlin
val queue = ArrayDeque<Int>()
val visited = BooleanArray(n)    // or HashSet<Int>()

queue.addLast(startNode)
visited[startNode] = true

while (queue.isNotEmpty()) {
    val node = queue.removeFirst()     // dequeue current
    
    // process node here
    
    for (neighbor in graph[node]) {
        if (!visited[neighbor]) {
            visited[neighbor] = true
            queue.addLast(neighbor)    // enqueue neighbor
        }
    }
}
```

### Level-by-Level BFS (Tree / Grid)

```kotlin
val queue = ArrayDeque<Int>()
queue.addLast(root)

while (queue.isNotEmpty()) {
    val levelSize = queue.size      // ← snapshot size before processing level
    
    repeat(levelSize) {             // process exactly one level
        val node = queue.removeFirst()
        // process node
        // add children to queue
    }
    // after repeat: entire level processed
}
```

> The `repeat(queue.size)` pattern is the key to level-order traversal. `queue.size` is captured **before** the loop so newly added children don't pollute the current level count.

---

## 8.4 Deque Use Cases

|Use Case|Operation|Why Deque|
|---|---|---|
|BFS traversal|FIFO (addLast / removeFirst)|O(1) both ends|
|Monotonic deque (sliding window max)|remove from both ends|need back-removal too|
|Palindrome check|add all, remove front & back|symmetric access|
|Level-order tree traversal|FIFO queue|enqueue children, dequeue level|
|Undo / redo|two stacks OR deque|access both ends|

### Monotonic Deque Pattern (Syntax Only)

> Maintains a deque where values are in decreasing order (for sliding window maximum).

```kotlin
val deque = ArrayDeque<Int>()    // stores INDICES

for (i in arr.indices) {
    // Remove indices outside the window
    while (deque.isNotEmpty() && deque.first() < i - windowSize + 1) {
        deque.removeFirst()
    }
    // Remove indices whose values are smaller than current (useless)
    while (deque.isNotEmpty() && arr[deque.last()] < arr[i]) {
        deque.removeLast()
    }
    deque.addLast(i)
    // deque.first() is always the index of the max in current window
}
```

---

# 9. PriorityQueue — Heap

```kotlin
import java.util.PriorityQueue
```

> A PriorityQueue is a **binary heap**. It always gives you the highest-priority element first. Default in Java/Kotlin: **min-heap** (smallest element has highest priority).

---

## 9.1 Min Heap (default — smallest at top)

```kotlin
val minHeap = PriorityQueue<Int>()         // natural ordering = ascending = min first

minHeap.offer(5)
minHeap.offer(1)
minHeap.offer(3)
// Internal order is heap-ordered, NOT sorted

minHeap.peek()    // 1 — smallest, NOT removed
minHeap.poll()    // 1 — removes and returns smallest
minHeap.peek()    // 3
minHeap.poll()    // 3
minHeap.poll()    // 5
```

---

## 9.2 Max Heap (largest at top)

```kotlin
// Option 1 — reverseOrder() — cleanest
val maxHeap = PriorityQueue<Int>(reverseOrder())

// Option 2 — Comparator.reverseOrder()
val maxHeap = PriorityQueue<Int>(Comparator.reverseOrder())

// Option 3 — lambda comparator
val maxHeap = PriorityQueue<Int> { a, b -> b - a }
// ⚠️ subtraction can overflow for large Int — see traps section

// Option 4 — Integer.compare (always safe)
val maxHeap = PriorityQueue<Int> { a, b -> Integer.compare(b, a) }
```

```kotlin
maxHeap.offer(5)
maxHeap.offer(1)
maxHeap.offer(3)

maxHeap.peek()    // 5 — largest, NOT removed
maxHeap.poll()    // 5
maxHeap.poll()    // 3
maxHeap.poll()    // 1
```

---

## 9.3 Custom Comparator

### Min-heap by specific field

```kotlin
// Min-heap of IntArray by first element
val heap = PriorityQueue<IntArray> { a, b -> a[0] - b[0] }

// Safe version (avoids overflow)
val heap = PriorityQueue<IntArray> { a, b -> Integer.compare(a[0], b[0]) }

// Min-heap of Pair<Int,Int> by second element
val heap = PriorityQueue<Pair<Int,Int>> { a, b -> a.second - b.second }

// Max-heap of Pair<Int,Int> by first element
val heap = PriorityQueue<Pair<Int,Int>> { a, b -> b.first - a.first }
```

### Multi-key comparator (sort by first, then by second)

```kotlin
val heap = PriorityQueue<IntArray>(
    compareBy({ it[0] }, { it[1] })    // min by [0], break ties by [1]
)

// Descending by value, then ascending by index
val heap = PriorityQueue<IntArray>(
    compareByDescending<IntArray> { it[0] }.thenBy { it[1] }
)
```

### Custom Data Class in Heap

```kotlin
data class Node(val dist: Int, val id: Int)

// Min-heap by distance
val heap = PriorityQueue<Node>(compareBy { it.dist })

heap.offer(Node(5, 2))
heap.offer(Node(1, 3))
heap.poll()    // Node(1, 3) — smallest dist
```

### compareBy vs Lambda Comparator

```kotlin
// compareBy — clean, readable
PriorityQueue<IntArray>(compareBy { it[0] })           // min by first element
PriorityQueue<IntArray>(compareByDescending { it[0] }) // max by first element

// Lambda — flexible
PriorityQueue<IntArray> { a, b -> a[0] - b[0] }       // min — ⚠️ overflow risk
PriorityQueue<IntArray> { a, b -> Integer.compare(a[0], b[0]) }  // min — safe
PriorityQueue<IntArray> { a, b -> Integer.compare(b[0], a[0]) }  // max — safe
```

---

## 9.4 Heap Operations

|Method|Returns|What it does|Time|
|---|---|---|---|
|`heap.offer(e)`|`Boolean`|insert e into heap|O(log n)|
|`heap.add(e)`|`Boolean`|same as offer|O(log n)|
|`heap.peek()`|`T?`|look at top (min or max); does NOT remove|O(1)|
|`heap.poll()`|`T?`|remove and return top element|O(log n)|
|`heap.remove()`|`T`|same as poll; throws if empty|O(log n)|
|`heap.remove(e)`|`Boolean`|remove specific element e|O(n)|
|`heap.contains(e)`|`Boolean`|check if e exists in heap|O(n)|
|`heap.size`|`Int`|number of elements|O(1)|
|`heap.isEmpty()`|`Boolean`||O(1)|
|`heap.isNotEmpty()`|`Boolean`||O(1)|
|`heap.clear()`|`Unit`|remove all elements|O(n)|

> ⚠️ `peek()` and `poll()` return **nullable** `T?` because they return `null` if the heap is empty (unlike `remove()` which throws).

```kotlin
val heap = PriorityQueue<Int>()
heap.offer(5); heap.offer(2); heap.offer(8)

heap.size        // 3
heap.peek()      // 2 — min, NOT removed
heap.poll()      // 2 — removed
heap.size        // 2
heap.peek()      // 5
```

---

## 9.5 Heap Iteration

### ⚠️ Critical: `toList()` Does NOT Return Sorted Order

```kotlin
val heap = PriorityQueue<Int>()
heap.offer(5); heap.offer(1); heap.offer(3); heap.offer(2)

heap.toList()    // [1, 3, 2, 5] or similar — HEAP ORDER, not sorted!
// ❌ Do NOT rely on toList() for sorted output
```

### Correct: Drain with poll() for Sorted Order

```kotlin
val result = mutableListOf<Int>()
while (heap.isNotEmpty()) {
    result.add(heap.poll()!!)
}
// result = [1, 2, 3, 5] — sorted ascending (for min-heap)
```

### Peek Without Modifying (make a copy)

```kotlin
// To iterate without draining:
val copy = PriorityQueue(heap)   // creates a copy
while (copy.isNotEmpty()) {
    println(copy.poll())
}
// original heap is unchanged
```

### for loop on heap

```kotlin
// for loop iterates in heap-internal order — not sorted
for (e in heap) {
    println(e)    // NOT in sorted order
}
```

---

## 9.6 Heap Traps

```kotlin
// ❌ Trap 1 — subtraction overflow in comparator
PriorityQueue<Int> { a, b -> a - b }
// Breaks when a = Int.MIN_VALUE and b > 0: MIN_VALUE - 1 overflows to positive
// ✅ Always use Integer.compare
PriorityQueue<Int> { a, b -> Integer.compare(a, b) }   // min heap — safe
PriorityQueue<Int> { a, b -> Integer.compare(b, a) }   // max heap — safe

// ❌ Trap 2 — toList() not sorted
heap.toList()   // heap-internal order only
// ✅ Drain with poll() for sorted output

// ❌ Trap 3 — peek() and poll() return nullable T?
val top: Int = heap.peek()    // ❌ compile error — type is Int?, not Int
val top: Int = heap.peek()!!  // ✅ force unwrap (only if certain not empty)
val top: Int = heap.peek() ?: return   // ✅ safe with fallback

// ❌ Trap 4 — remove(element) is O(n), not O(log n)
heap.remove(specificValue)    // linear scan!
// If frequent removal of specific elements is needed, consider lazy deletion:
// Mark elements as "deleted" in a HashSet, skip them when polling

// ⚠️ Trap 5 — heap.size > k trick — remember what gets removed
val minHeap = PriorityQueue<Int>()
for (n in nums) {
    minHeap.offer(n)
    if (minHeap.size > k) minHeap.poll()   // removes SMALLEST — keeps k largest
}
// To keep k smallest: use maxHeap and poll when size > k
```

---

# 10. String & StringBuilder

## 10.1 String Basics

```kotlin
val s = "Hello, World!"

s.length              // 13  ← .length (NOT .size — only strings use .length)
s[0]                  // 'H' — Char at index
s[s.length - 1]       // '!' — last char
s.first()             // 'H'
s.last()              // '!'
s.isEmpty()           // false — true only if ""
s.isBlank()           // false — true if "" or "   " (only whitespace)
s.isNotEmpty()        // true
s.isNotBlank()        // true
```

### String is Immutable

> Every String method returns a **new** String. The original is never changed. For building strings in a loop → use `StringBuilder`.

```kotlin
var s = "hello"
s.uppercase()           // "HELLO" — new String, s is still "hello"
s = s.uppercase()       // now s is "HELLO" — you must reassign
```

### String Equality

```kotlin
// ✅ In Kotlin, == compares VALUE (content), not reference
"hello" == "hello"                          // true
"Hello".equals("hello", ignoreCase = true)  // true — case insensitive

// Reference equality (rarely needed)
"hello" === "hello"    // depends on JVM string interning
```

---

## 10.2 String Methods

### Case & Reversal

|Method|Returns|What it does|
|---|---|---|
|`s.uppercase()`|`String`|all uppercase|
|`s.lowercase()`|`String`|all lowercase|
|`s.reversed()`|`String`|reverse the string|
|`s.repeat(n)`|`String`|"ab".repeat(3) → "ababab"|

### Checking / Searching

|Method|Returns|What it does|
|---|---|---|
|`s.contains("sub")`|`Boolean`|substring check|
|`s.contains('c')`|`Boolean`|char check|
|`s.startsWith("pre")`|`Boolean`||
|`s.endsWith("suf")`|`Boolean`||
|`s.startsWith("pre", i)`|`Boolean`|check from offset i|
|`s.indexOf('c')`|`Int`|first index of char; -1 if not found|
|`s.indexOf("str")`|`Int`|first index of substring|
|`s.indexOf('c', fromIndex)`|`Int`|search from index|
|`s.lastIndexOf('c')`|`Int`|last index of char|
|`s.lastIndexOf("str")`|`Int`|last index of substring|
|`s.count { it == 'a' }`|`Int`|count chars matching predicate|

### Substrings

|Method|Returns|What it does|Notes|
|---|---|---|---|
|`s.substring(i)`|`String`|from index i to end||
|`s.substring(i, j)`|`String`|from i to j-1|j is **exclusive**|
|`s.take(n)`|`String`|first n chars||
|`s.takeLast(n)`|`String`|last n chars||
|`s.drop(n)`|`String`|skip first n chars||
|`s.dropLast(n)`|`String`|skip last n chars||
|`s.takeWhile { pred }`|`String`|take chars while pred is true||
|`s.dropWhile { pred }`|`String`|skip chars while pred is true||
|`s.slice(range)`|`String`|chars at range positions||
|`s.slice(indices)`|`String`|chars at specific indices||

### Trimming & Replacing

|Method|Returns|What it does|
|---|---|---|
|`s.trim()`|`String`|remove leading + trailing whitespace|
|`s.trimStart()`|`String`|remove leading whitespace only|
|`s.trimEnd()`|`String`|remove trailing whitespace only|
|`s.trim { it == '#' }`|`String`|remove leading/trailing matching char|
|`s.replace("a", "b")`|`String`|replace ALL occurrences of "a" with "b"|
|`s.replace('a', 'b')`|`String`|replace all char 'a' with 'b'|
|`s.replaceFirst("a", "b")`|`String`|replace only first occurrence|
|`s.replace(Regex("\\d"), "x")`|`String`|regex replace|
|`s.filter { it.isLetter() }`|`String`|keep only matching chars|
|`s.filterNot { it == ' ' }`|`String`|remove matching chars|

### Padding

|Method|Returns|What it does|
|---|---|---|
|`s.padStart(n)`|`String`|pad with spaces on left to total length n|
|`s.padStart(n, '0')`|`String`|pad with '0' on left|
|`s.padEnd(n)`|`String`|pad with spaces on right|
|`s.padEnd(n, '.')`|`String`|pad with '.' on right|

```kotlin
"42".padStart(5, '0')    // "00042"
"hi".padEnd(5, '-')      // "hi---"
```

### Comparison

|Method|Returns|What it does|
|---|---|---|
|`s.compareTo(other)`|`Int`|lexicographic: negative/0/positive|
|`s.compareTo(other, ignoreCase = true)`|`Int`|case-insensitive|
|`s == other`|`Boolean`|content equality|
|`s.equals(other, ignoreCase = true)`|`Boolean`|case-insensitive equality|

---

## 10.3 String Traversal

### Char Loop (most common)

```kotlin
val s = "hello"
for (ch in s) {
    println(ch)    // iterates Char: 'h','e','l','l','o'
}
```

### Index Loop

```kotlin
for (i in s.indices) {
    val ch: Char = s[i]
}
// s.indices = 0 until s.length
```

### Index + Char Together

```kotlin
for ((i, ch) in s.withIndex()) {
    println("s[$i] = $ch")
}
```

### forEach

```kotlin
s.forEach { ch -> println(ch) }
s.forEachIndexed { i, ch -> println("$i: $ch") }
```

> ⚠️ Same forEach trap — cannot `break`. Use `for` loop when you need to exit early.

### Reverse Traversal

```kotlin
for (i in s.lastIndex downTo 0) {
    val ch = s[i]
}
// s.lastIndex = s.length - 1
```

### Convert to CharArray, then modify

```kotlin
val chars = s.toCharArray()      // mutable char array
chars[0] = 'H'                   // modify
val result = String(chars)       // back to String
```

---

## 10.4 String Splitting

### split() — Returns List<String>

```kotlin
"a,b,c".split(",")              // ["a", "b", "c"]
"a,b,,c".split(",")             // ["a", "b", "", "c"]  — note empty string!
"hello world".split(" ")        // ["hello", "world"]
"a  b   c".split(" ")           // ["a", "", "b", "", "", "c"]  — empties!
```

### Split on Regex (handles multiple spaces etc.)

```kotlin
"a  b   c".split("\\s+".toRegex())     // ["a", "b", "c"]  — no empties
"a,b;c".split("[,;]".toRegex())        // ["a", "b", "c"]  — multiple delimiters
```

### Split with Limit

```kotlin
"a,b,c,d".split(",", limit = 2)    // ["a", "b,c,d"]  — max 2 parts
```

### lines() — Split by Newline

```kotlin
"line1\nline2\nline3".lines()    // ["line1", "line2", "line3"]
```

### toCharArray() — Split into Chars

```kotlin
"hello".toCharArray()            // CharArray ['h','e','l','l','o']
"hello".toList()                 // List<Char>  ['h','e','l','l','o']
```

### ⚠️ Split Traps

```kotlin
// ❌ split on "." does NOT work as expected — "." is a regex metachar
"a.b.c".split(".")       // ❌ splits on ANY char — ["", "", "", "", "", ""]
// ✅ Escape the dot for literal split
"a.b.c".split("\\.")     // ✅ ["a", "b", "c"]
"a.b.c".split('.')       // ✅ char overload — no regex — ["a", "b", "c"]

// ❌ split produces empty strings for consecutive delimiters
"a,,b".split(",")        // ["a", "", "b"]
// ✅ Filter empties
"a,,b".split(",").filter { it.isNotEmpty() }
```

---

## 10.5 Joining

### joinToString — List / Array → String

```kotlin
listOf("a", "b", "c").joinToString(",")          // "a,b,c"
listOf("a", "b", "c").joinToString("")           // "abc"
listOf("a", "b", "c").joinToString(", ")         // "a, b, c"
listOf("a", "b", "c").joinToString(
    separator = ", ",
    prefix = "[",
    postfix = "]"
)                                                 // "[a, b, c]"

// With transform
listOf(1, 2, 3).joinToString(",") { it * 2 }     // Nope — transform is { (it*2).toString() }
listOf(1, 2, 3).joinToString(",") { (it * 2).toString() }   // "2,4,6"

intArrayOf(1, 2, 3).joinToString("-")            // "1-2-3"
```

### CharArray / Array → String

```kotlin
charArrayOf('h','e','l','l','o').concatToString()  // "hello"
String(charArrayOf('h','i'))                        // "hi"
charArrayOf('a','b','c').joinToString("")            // "abc"
```

### buildString

```kotlin
val result = buildString {
    append("start")
    append("_")
    append(42)
    append("_end")
}
// "start_42_end"
```

---

## 10.6 StringBuilder

> Use `StringBuilder` when building a string in a loop or with many appends. Each `+` on a regular String creates a new String object — O(n) per operation → O(n²) total. `StringBuilder` builds in O(1) per append.

### Creation

```kotlin
val sb = StringBuilder()               // empty
val sb = StringBuilder("initial")     // with starting content
val sb = StringBuilder(capacity)      // pre-allocated capacity (hint)
```

### All Methods

|Method|Returns|What it does|
|---|---|---|
|`sb.append(x)`|`StringBuilder`|append String, Char, Int, Long, Boolean, etc.|
|`sb.appendLine(x)`|`StringBuilder`|append x + newline|
|`sb.insert(i, x)`|`StringBuilder`|insert x at index i|
|`sb.delete(i, j)`|`StringBuilder`|delete chars from i to j-1 (j exclusive)|
|`sb.deleteCharAt(i)`|`StringBuilder`|delete char at index i|
|`sb.replace(i, j, s)`|`StringBuilder`|replace chars i..j-1 with string s|
|`sb.reverse()`|`StringBuilder`|reverse all chars in place|
|`sb.setCharAt(i, ch)`|`Unit`|set char at index i to ch|
|`sb[i]`|`Char`|get char at index|
|`sb[i] = ch`||set char at index|
|`sb.length`|`Int`|current length|
|`sb.isEmpty()`|`Boolean`||
|`sb.isNotEmpty()`|`Boolean`||
|`sb.clear()`|`StringBuilder`|remove all content|
|`sb.toString()`|`String`|convert to immutable String|
|`sb.indexOf("str")`|`Int`|first index of substring|
|`sb.lastIndexOf("str")`|`Int`|last index of substring|
|`sb.substring(i)`|`String`|substring from i to end|
|`sb.substring(i, j)`|`String`|substring from i to j-1|

### Usage Examples

```kotlin
val sb = StringBuilder()

sb.append("hello")          // "hello"
sb.append(' ')              // "hello "
sb.append("world")          // "hello world"
sb.append(42)               // "hello world42"
sb.insert(5, ",")           // "hello, world42"
sb.delete(12, 14)           // "hello, world"
sb.reverse()                // "dlrow ,olleh"
sb.setCharAt(0, 'D')        // "Dlrow ,olleh"
sb.toString()               // "Dlrow ,olleh" — final String

// Chaining — append returns StringBuilder
sb.append("a").append("b").append("c")

// Check and modify by index
if (sb[0] == 'D') sb[0] = 'd'

// Delete last character
sb.deleteCharAt(sb.length - 1)

// Clear and reuse
sb.clear()
sb.append("fresh start")
```

---

## 10.7 String Performance Notes

|Situation|Approach|Why|
|---|---|---|
|Single concat `a + b`|`String`|fine, one allocation|
|Concat in loop N times|`StringBuilder`|`String +` in loop = O(n²)|
|Build result from chars|`StringBuilder` or `buildString`|efficient|
|Many `replace` on same string|`StringBuilder`|avoids many allocations|
|Char frequency counting|`IntArray(26)`|faster than `HashMap<Char,Int>`|
|Check palindrome|two-pointer on string directly|no extra allocation|
|Reverse string|`s.reversed()` or `StringBuilder.reverse()`||
|Split then rejoin|do in one pass if possible|avoids intermediate list|

### String + in Loop is Slow

```kotlin
// ❌ O(n²) — every + creates a new String
var result = ""
for (ch in chars) {
    result += ch     // BAD in a loop
}

// ✅ O(n) — StringBuilder appends in amortized O(1)
val sb = StringBuilder()
for (ch in chars) {
    sb.append(ch)
}
val result = sb.toString()
```

### Char Operations (Fast Patterns)

```kotlin
// Frequency array — faster than HashMap for lowercase letters
val freq = IntArray(26)
for (ch in s) freq[ch - 'a']++

// Check if two strings are anagrams — no HashMap needed
fun isAnagram(s: String, t: String): Boolean {
    if (s.length != t.length) return false
    val freq = IntArray(26)
    for (ch in s) freq[ch - 'a']++
    for (ch in t) {
        freq[ch - 'a']--
        if (freq[ch - 'a'] < 0) return false
    }
    return true
}
```

---

## CHAPTER SUMMARY (Ch 6–10)

### Collection Cheat Sheet

|You need|Structure|Key method|
|---|---|---|
|O(1) existence check|`HashSet`|`e in set`|
|Unique elements from array|`arr.toHashSet()`||
|LIFO push/pop|`ArrayDeque`|`addLast` / `removeLast`|
|FIFO enqueue/dequeue|`ArrayDeque`|`addLast` / `removeFirst`|
|Both ends access|`ArrayDeque`|`addFirst/Last`, `removeFirst/Last`|
|Min element always at top|`PriorityQueue<T>()`|`poll()`|
|Max element always at top|`PriorityQueue<T>(reverseOrder())`|`poll()`|
|Custom ordering at top|`PriorityQueue<T> { a, b -> ... }`|`poll()`|
|Build string in loop|`StringBuilder`|`append()`|
|Immutable string ops|`String`|every method returns new String|

### Safe vs Unsafe Method Pairs

|Structure|Throws if empty|Safe (returns null)|
|---|---|---|
|Stack peek|`stack.last()`|`stack.lastOrNull()`|
|Stack pop|`stack.removeLast()`|`stack.removeLastOrNull()`|
|Queue peek|`queue.first()`|`queue.firstOrNull()`|
|Queue dequeue|`queue.removeFirst()`|`queue.removeFirstOrNull()`|
|Heap peek|`heap.element()`|`heap.peek()` (returns null)|
|Heap poll|`heap.remove()`|`heap.poll()` (returns null)|

---

_End of Chapters 6–10_ _Next: Ch 11 Loops & Iteration · Ch 12 Ranges · Ch 13 Null Safety · Ch 14 Sorting · Ch 15 Math Utilities_


# Kotlin DSA Reference Guide

### Chapters 11–15 | Pure API & Syntax Reference | No Problem Solutions

---

## MASTER INDEX (This File)

|#|Chapter|
|---|---|
|11|[Loops & Iteration](#11-loops--iteration)|
|12|[Ranges](#12-ranges)|
|13|[Null Safety](#13-null-safety)|
|14|[Sorting](#14-sorting)|
|15|[Math Utilities](#15-math-utilities)|

---

# 11. Loops & Iteration

## 11.1 For Loops

### Basic Element Loop

```kotlin
val arr = intArrayOf(10, 20, 30)

for (e in arr) {
    println(e)      // 10, 20, 30
}
```

### Index Loop with `indices`

```kotlin
for (i in arr.indices) {
    println(arr[i])
}
// arr.indices = IntRange(0, arr.size - 1) = 0 until arr.size
```

### Index + Value with `withIndex()`

```kotlin
for ((i, value) in arr.withIndex()) {
    println("arr[$i] = $value")
}
```

---

### `until` — Exclusive Upper Bound

```kotlin
for (i in 0 until 5) { }
// i = 0, 1, 2, 3, 4  — 5 is NOT included

for (i in 0 until arr.size) { }
// same as for (i in arr.indices)

// Common use: loop n times starting at 0
for (i in 0 until n) { }
```

> ⚠️ `0..4` includes 4. `0 until 5` excludes 5. Both give 0,1,2,3,4. Prefer `until` when looping over array indices — avoids off-by-one errors.

---

### `downTo` — Reverse / Descending Loop

```kotlin
for (i in 5 downTo 0) { }
// i = 5, 4, 3, 2, 1, 0  — both ends inclusive

for (i in arr.lastIndex downTo 0) { }
// reverse array traversal

for (i in 10 downTo 1) { }
// i = 10, 9, 8, ..., 1
```

> ⚠️ `downTo` is **inclusive** on both ends. `5 downTo 0` includes 0.

---

### `step` — Custom Increment / Decrement

```kotlin
for (i in 0..10 step 2) { }
// i = 0, 2, 4, 6, 8, 10

for (i in 0 until 10 step 3) { }
// i = 0, 3, 6, 9

for (i in 10 downTo 0 step 2) { }
// i = 10, 8, 6, 4, 2, 0

// step must be positive — even with downTo
for (i in 10 downTo 0 step 3) { }
// i = 10, 7, 4, 1
```

> ⚠️ `step` value must always be **positive**, even with `downTo`. ❌ `for (i in 10 downTo 0 step -2)` — compile error.

---

### `..` — Inclusive Range Loop

```kotlin
for (i in 1..5) { }
// i = 1, 2, 3, 4, 5  — both ends inclusive

for (ch in 'a'..'z') { }
// ch = 'a', 'b', ..., 'z'
```

---

### All For Loop Forms — Quick Reference

```kotlin
for (i in 0..n)           // 0 to n inclusive   (careful: includes n)
for (i in 0 until n)      // 0 to n-1           (excludes n) ← prefer for arrays
for (i in n downTo 0)     // n down to 0 inclusive
for (i in n-1 downTo 0)   // n-1 down to 0      (reverse array traversal)
for (i in 0..n step k)    // 0, k, 2k, ..., up to n
for (i in 0 until n step k) // 0, k, 2k, ..., up to n-1
for (i in n downTo 0 step k) // n, n-k, n-2k, ..., down to 0

for (e in collection)             // iterate elements
for (i in collection.indices)     // iterate indices
for ((i, e) in collection.withIndex()) // index + element
for ((k, v) in map)               // map key-value pairs
for (ch in string)                // iterate String chars
for ((i, ch) in string.withIndex()) // String index + char
```

---

### Nested For Loops

```kotlin
// 2D array traversal
for (r in matrix.indices) {
    for (c in matrix[r].indices) {
        val cell = matrix[r][c]
    }
}

// All pairs (i, j) where i < j
for (i in 0 until n) {
    for (j in i + 1 until n) {
        // pair (i, j)
    }
}

// All pairs (i, j) — full grid
for (i in 0 until rows) {
    for (j in 0 until cols) { }
}
```

---

## 11.2 While Loop

### Basic while

```kotlin
var i = 0
while (i < n) {
    // body
    i++
}
```

### do-while — Executes at Least Once

```kotlin
var i = 0
do {
    // body runs at least once even if condition is false
    i++
} while (i < n)
```

### Two-Pointer Pattern (while)

```kotlin
var left = 0
var right = arr.size - 1

while (left < right) {
    // process arr[left] and arr[right]
    left++
    right--
}
```

### Shrink Window Pattern (while inside for)

```kotlin
var left = 0
for (right in arr.indices) {
    // expand window with arr[right]
    while (/* window is invalid */) {
        // shrink from left
        left++
    }
    // window arr[left..right] is valid here
}
```

### Traverse Linked List (while)

```kotlin
var curr = head     // curr: ListNode?
while (curr != null) {
    // process curr.val
    curr = curr.next
}
```

### Digit Extraction (while)

```kotlin
var n = 12345
while (n > 0) {
    val digit = n % 10    // last digit
    n /= 10               // remove last digit
}
```

---

## 11.3 repeat()

> `repeat(n)` executes a block exactly `n` times. Cleaner than `for (i in 0 until n)` when you don't need the index.

```kotlin
repeat(5) {
    println("hello")    // prints 5 times
}

repeat(n) { i ->
    println(i)          // i = 0, 1, 2, ..., n-1
}

// Without index param
repeat(3) { println("tick") }

// With index param (i is the iteration count)
repeat(5) { i ->
    arr[i] = i * 2
}
```

### repeat in BFS Level Processing

```kotlin
// Process exactly one level of a BFS queue
repeat(queue.size) {
    val node = queue.removeFirst()
    // process node, add children
}
```

> ⚠️ `repeat(-1)` or `repeat(0)` — does nothing, no crash. Safe to call with any non-negative Int. ⚠️ `repeat(n)` does NOT have `break`. If you need to exit early, use a `for` loop.

---

## 11.4 break & continue

### break — Exit Entire Loop

```kotlin
for (i in 0..10) {
    if (i == 5) break      // stops loop at i=5
    println(i)             // prints 0 1 2 3 4
}

var i = 0
while (true) {
    if (i >= 5) break
    i++
}
```

### continue — Skip Current Iteration

```kotlin
for (i in 0..10) {
    if (i % 2 == 0) continue   // skip even numbers
    println(i)                  // prints 1 3 5 7 9 11... wait only 1 3 5 7 9
}
```

### break and continue in Nested Loops

```kotlin
// ⚠️ break/continue only affect the INNERMOST loop by default
for (i in 0..3) {
    for (j in 0..3) {
        if (j == 2) break    // only breaks inner loop, outer continues
    }
}
```

---

## 11.5 Labeled Loops

> Labels allow `break` and `continue` to target an **outer** loop — critical for nested loop DSA patterns.

### Labeled break — Exit Outer Loop

```kotlin
outer@ for (i in 0..3) {
    for (j in 0..3) {
        if (i == 2 && j == 1) break@outer   // exits the outer loop entirely
        println("$i, $j")
    }
}
// Once break@outer fires, execution continues AFTER the outer for loop
```

### Labeled continue — Skip Outer Iteration

```kotlin
outer@ for (i in 0..3) {
    for (j in 0..3) {
        if (j == 2) continue@outer    // skip remaining j iterations, go to next i
    }
    println("i = $i")    // never prints because continue@outer skips this
}
```

### Label Naming Rules

```kotlin
// Any identifier followed by @ is a valid label
loop@ for (...) { }
search@ while (...) { }
process@ for (...) { }

// Must match exactly: break@loop, continue@loop
```

### When to Use Labels

```kotlin
// Matrix search — exit both loops when found
found@ for (r in matrix.indices) {
    for (c in matrix[r].indices) {
        if (matrix[r][c] == target) {
            result = Pair(r, c)
            break@found
        }
    }
}
```

> ✅ Use labeled break when you need to exit a nested loop on a condition. ✅ Alternative to labeled break: use a function and `return` from it — often cleaner.

```kotlin
// Alternative — function return (often cleaner than labeled break)
fun searchMatrix(matrix: Array<IntArray>, target: Int): Pair<Int,Int>? {
    for (r in matrix.indices) {
        for (c in matrix[r].indices) {
            if (matrix[r][c] == target) return Pair(r, c)
        }
    }
    return null
}
```

---

## 11.6 forEach Traps

> `forEach` is a higher-order function. It is **not** a loop construct. This distinction causes subtle bugs in DSA.

### Trap 1 — `return` inside forEach exits the LAMBDA, not the outer function

```kotlin
fun findFirst(nums: List<Int>): Int {
    nums.forEach { n ->
        if (n == 5) return n    // ← this DOES work — returns from findFirst
                                // (non-local return — only works in inline functions)
    }
    return -1
}
// ↑ This actually compiles and works for forEach specifically because it's inline
// BUT it's confusing — avoid relying on this behavior
```

```kotlin
// The trap version — inside a non-inline lambda
fun findFirst(nums: List<Int>): Int {
    val process = { n: Int ->
        if (n == 5) return n    // ❌ compile error — can't return from outer here
    }
    nums.forEach(process)
    return -1
}
```

### Trap 2 — `return@forEach` is "continue", not "break"

```kotlin
fun example(nums: List<Int>) {
    nums.forEach { n ->
        if (n % 2 == 0) return@forEach    // skips even — like "continue"
        println(n)                          // only prints odds
    }
    // ← execution continues here after forEach
}
```

### Trap 3 — No `break` in forEach

```kotlin
// ❌ There is NO way to break out of forEach mid-iteration
nums.forEach { n ->
    if (n == target) {
        // found it — but cannot stop forEach here!
        // return@forEach only skips current element
    }
}

// ✅ Use for loop when you need break
for (n in nums) {
    if (n == target) break    // works correctly
}

// ✅ Or use find / firstOrNull
val found = nums.firstOrNull { it == target }
```

### Trap 4 — forEach on Map

```kotlin
map.forEach { (key, value) ->
    // same traps apply — no break, return@forEach = continue
}

// ✅ Use for loop when you need to break on a map
for ((key, value) in map) {
    if (key == target) break
}
```

### forEach vs for Loop — Decision Table

|Need|forEach|for loop|
|---|---|---|
|Simple iteration, no early exit|✅ fine|✅ fine|
|Need to `break`|❌ not possible|✅ use for|
|Need to `return` from outer function|⚠️ works for inline (forEach) but confusing|✅ clear|
|Need `continue` (skip)|`return@forEach`|`continue`|
|Nested labels|❌ messy|✅ use labeled break|
|Debugging ease|❌ harder to step through|✅ easier|

> **DSA Rule:** Default to `for` loops. Use `forEach` only for simple, no-exit-needed iterations.

---

# 12. Ranges

## `..` — Closed Range (Both Ends Inclusive)

```kotlin
val r = 1..10
// Represents: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10

for (i in 1..5) { }           // 1, 2, 3, 4, 5
for (ch in 'a'..'z') { }      // 'a' to 'z' inclusive
for (ch in 'A'..'Z') { }      // 'A' to 'Z' inclusive

// Type: IntRange, CharRange, LongRange depending on operands
val intRange: IntRange = 1..10
val charRange: CharRange = 'a'..'z'
val longRange: LongRange = 1L..100L
```

---

## `until` — Half-Open Range (Excludes Upper Bound)

```kotlin
val r = 0 until 10
// Represents: 0, 1, 2, ..., 9  — 10 NOT included

for (i in 0 until n) { }        // indices 0 to n-1
for (i in 0 until arr.size) { } // all valid array indices
// equivalent: for (i in arr.indices)
```

> ⚠️ `until` only works with a step of 1 (ascending). For step or downTo, use `..` and adjust.

---

## `downTo` — Descending Range

```kotlin
val r = 10 downTo 1
// Represents: 10, 9, 8, ..., 1  — both ends inclusive

for (i in 5 downTo 0) { }       // 5, 4, 3, 2, 1, 0
for (i in arr.lastIndex downTo 0) { }  // reverse array
```

> ⚠️ `downTo` is always **inclusive** on both ends. ⚠️ `5 downTo 6` is an empty range (no iterations).

---

## `step` — Custom Step Size

```kotlin
for (i in 0..10 step 2) { }       // 0, 2, 4, 6, 8, 10
for (i in 0 until 10 step 3) { }  // 0, 3, 6, 9
for (i in 10 downTo 0 step 2) { } // 10, 8, 6, 4, 2, 0
for (i in 10 downTo 0 step 3) { } // 10, 7, 4, 1

// step must be positive
// ❌ step(-1) — compile error
// ❌ step(0)  — runtime error (IllegalArgumentException)
```

---

## Range Methods

### Membership Check — O(1)

```kotlin
val r = 1..100

5 in r            // true  — O(1), not a linear scan
105 in r          // false
5 !in r           // false

// Char range
'e' in 'a'..'z'   // true
'E' in 'a'..'z'   // false

// Use for bounds check
if (i in arr.indices) arr[i]      // safe array access
if (i in 0 until n) { }           // bounds check
```

### Range Properties

```kotlin
val r = 1..10

r.first        // 1
r.last         // 10
r.step         // 1 (default)

val r2 = 0..20 step 5
r2.first       // 0
r2.last        // 20
r2.step        // 5
```

### Range Functions

|Method|Returns|What it does|
|---|---|---|
|`r.first`|`Int`|first value|
|`r.last`|`Int`|last value|
|`r.step`|`Int`|step value|
|`r.isEmpty()`|`Boolean`|true if no elements (e.g., `5 downTo 6`)|
|`r.contains(x)`|`Boolean`|same as `x in r` — O(1)|
|`r.count()`|`Int`|number of elements|
|`r.sum()`|`Int`|sum of all elements|
|`r.average()`|`Double`|average|
|`r.min()`|`Int`|minimum value|
|`r.max()`|`Int`|maximum value|
|`r.toList()`|`List<Int>`|materialize into list|
|`r.toMutableList()`|`MutableList<Int>`||
|`r.toIntArray()`|`IntArray`||
|`r.toSet()`|`Set<Int>`||
|`r.reversed()`|`IntProgression`|same range reversed|
|`r.random()`|`Int`|random element in range|
|`r.filter { }`|`List<Int>`|filter elements|
|`r.map { }`|`List<R>`|transform elements|
|`r.forEach { }`|`Unit`|iterate|
|`r.any { }`|`Boolean`||
|`r.all { }`|`Boolean`||

```kotlin
(1..5).sum()          // 15
(1..5).toList()       // [1, 2, 3, 4, 5]
(1..10).filter { it % 2 == 0 }  // [2, 4, 6, 8, 10]
(1..10).random()      // random Int from 1 to 10
(1..5).reversed()     // 5 downTo 1
(0..10 step 2).count() // 6
```

---

## Range Summary Table

|Expression|Includes|Type|
|---|---|---|
|`1..10`|1,2,...,10 (both inclusive)|`IntRange`|
|`1 until 10`|1,2,...,9 (excludes 10)|`IntRange`|
|`10 downTo 1`|10,9,...,1 (both inclusive)|`IntProgression`|
|`0..10 step 2`|0,2,4,6,8,10|`IntProgression`|
|`10 downTo 0 step 3`|10,7,4,1|`IntProgression`|
|`'a'..'z'`|'a' to 'z'|`CharRange`|
|`1L..100L`|1 to 100 (Long)|`LongRange`|

---

## Common Range Patterns in DSA

```kotlin
// Safe index access
if (i in arr.indices) arr[i]               // arr.indices = 0 until arr.size
if (r in 0 until rows && c in 0 until cols) // 2D grid bounds check

// Char category checks
ch in 'a'..'z'     // lowercase letter
ch in 'A'..'Z'     // uppercase letter
ch in '0'..'9'     // digit char

// Generate a list
val squares = (1..10).map { it * it }      // [1,4,9,16,...,100]

// Sum formula (faster than range.sum() for large n)
val n = 1_000_000L
val sum = n * (n + 1) / 2                  // Gauss formula

// Iterate backwards without step
(n - 1 downTo 0).forEach { i -> }
```

---

# 13. Null Safety

## Nullable Types

```kotlin
// Non-null type — cannot hold null
var a: String = "hello"
a = null              // ❌ compile error

// Nullable type — CAN hold null (append ?)
var b: String? = null
var c: Int? = null
var d: MutableList<Int>? = null

// Any type can be made nullable with ?
var node: TreeNode? = null
var map: HashMap<Int,Int>? = null
```

### Where Nulls Come From in DSA

```kotlin
// Map access always returns nullable
val map = mapOf("a" to 1)
val v: Int? = map["a"]        // Int? even though value is 1
val v2: Int? = map["z"]       // null — key not present

// List methods
list.firstOrNull { it > 5 }   // Int? — null if nothing matches
list.lastOrNull()             // Int? — null if list empty
list.getOrNull(10)            // Int? — null if index out of bounds

// Array methods
arr.maxOrNull()               // Int? — null if array empty
arr.minOrNull()               // Int? — null if array empty

// Heap
heap.peek()                   // T?  — null if heap empty
heap.poll()                   // T?  — null if heap empty

// String methods
"abc".toIntOrNull()           // Int? — null for non-numeric strings
```

---

## Operators

### `?.` — Safe Call

> Calls the method only if the receiver is not null. Returns `null` if it is.

```kotlin
val s: String? = null
s?.length         // null  — no crash, returns null
s?.uppercase()    // null  — no crash

val s2: String? = "hello"
s2?.length        // 5

// Chain safe calls
val len = user?.address?.city?.length    // null if any link is null
```

### `?:` — Elvis Operator (Null Default)

> Returns the left side if not null, otherwise the right side.

```kotlin
val s: String? = null
val len = s?.length ?: 0          // 0 (s is null)

val s2: String? = "hello"
val len2 = s2?.length ?: 0        // 5 (s2 is not null)

// Common patterns
val count = map[key] ?: 0          // default 0 if key missing
val node = list.firstOrNull() ?: return   // early exit if empty
val value = input ?: throw IllegalArgumentException("null input")

// Chain
val result = a?.b?.c ?: "default"
```

### `!!` — Not-Null Assertion (Force Unwrap)

> Asserts the value is not null. Throws `NullPointerException` if it IS null.

```kotlin
val s: String? = "hello"
val len = s!!.length      // 5 — works because s is not null

val s2: String? = null
val len2 = s2!!.length    // ❌ throws NullPointerException at runtime

// When to use !!
// Only when you are 100% certain the value is not null
// AND the compiler cannot infer it from context

val top = heap.peek()!!     // acceptable if you just checked heap.isNotEmpty()
```

> **Rule:** `!!` should be rare in clean code. Every `!!` is a potential crash. If you find yourself using `!!` often, restructure to use `?:` or safe calls.

---

## Safe Casting

### `as?` — Safe Cast (Returns Null on Failure)

```kotlin
val obj: Any = "hello"
val s: String? = obj as? String     // "hello" — cast succeeded
val n: Int? = obj as? Int           // null — not an Int, no crash

// Contrast with hard cast:
val n2: Int = obj as Int            // ❌ throws ClassCastException
```

### `is` — Type Check

```kotlin
val obj: Any = 42

if (obj is Int) {
    println(obj + 1)    // obj is smart-cast to Int inside the if block
}

if (obj !is String) {
    println("not a string")
}

when (obj) {
    is Int    -> println("Int: $obj")
    is String -> println("String: $obj")
    is List<*> -> println("List")
    else      -> println("other")
}
```

### Smart Cast — After Null Check, No `!!` Needed

```kotlin
var s: String? = "hello"

// After null check, compiler knows s is not null inside the block
if (s != null) {
    println(s.length)    // ✅ no !! needed — s is smart-cast to String
}

// Also works with ?: return / ?: throw
fun process(s: String?): Int {
    s ?: return 0        // if s is null, return 0
    return s.length      // ✅ compiler knows s is not null here
}
```

---

## Null Patterns

### Pattern 1 — Safe Map Access with Default

```kotlin
val map = mutableMapOf<Char, Int>()

// ❌ crash if key not present
val count = map['a']!! + 1

// ✅ safe — use ?: default
val count = (map['a'] ?: 0) + 1

// ✅ safe — getOrDefault
val count = map.getOrDefault('a', 0) + 1

// ✅ safe — getOrElse
val count = map.getOrElse('a') { 0 } + 1
```

### Pattern 2 — Safe List Access

```kotlin
val list = listOf(1, 2, 3)

// ❌ crash if index out of bounds
val e = list[10]

// ✅ safe
val e = list.getOrNull(10)           // Int? — null if out of bounds
val e = list.getOrNull(10) ?: -1     // Int — -1 as fallback
val e = list.getOrElse(10) { -1 }   // Int — -1 as fallback
```

### Pattern 3 — Early Return on Null

```kotlin
fun process(node: TreeNode?): Int {
    node ?: return 0          // if null, return 0 immediately
    // from here, node is smart-cast to TreeNode (non-null)
    return node.`val`
}

fun bfs(start: Int?): List<Int> {
    val s = start ?: return emptyList()    // early exit
    // s is Int from here
    return listOf(s)
}
```

### Pattern 4 — let Block (Execute Only If Non-Null)

```kotlin
val s: String? = "hello"

s?.let { nonNullS ->
    println(nonNullS.length)    // only runs if s is not null
    // nonNullS is String (not String?) inside here
}

// Short form
s?.let { println(it.length) }

// let with Elvis for null case
s?.let { doSomethingWith(it) } ?: run { handleNullCase() }
```

### Pattern 5 — Filter Nulls from Collections

```kotlin
val list: List<Int?> = listOf(1, null, 2, null, 3)

val nonNulls: List<Int> = list.filterNotNull()     // [1, 2, 3]

// mapNotNull — transform + filter nulls in one pass
val results: List<Int> = list.mapNotNull { it?.times(2) }   // [2, 4, 6]
```

### Pattern 6 — Chained Safe Calls

```kotlin
// If any link is null, entire chain returns null
val len: Int? = user?.profile?.name?.length

// With default at end
val len: Int = user?.profile?.name?.length ?: 0
```

### Pattern 7 — Lateinit (for var, non-null, initialized later)

```kotlin
lateinit var adj: Array<MutableList<Int>>

// Initialize later
adj = Array(n) { mutableListOf() }

// Check if initialized (avoid crash)
if (::adj.isInitialized) { }
```

### Pattern 8 — also, apply for null-safe init

```kotlin
// also — perform action on non-null value, return same value
val result = map[key]?.also { println("found: $it") } ?: defaultValue
```

---

## Null Safety — Operator Quick Reference

|Operator|Syntax|Behavior|
|---|---|---|
|Safe call|`obj?.method()`|null if obj is null, else calls method|
|Elvis|`expr ?: default`|default if expr is null|
|Not-null assert|`obj!!`|throws NPE if null|
|Safe cast|`obj as? Type`|null if cast fails|
|Type check|`obj is Type`|Boolean, enables smart cast|
|Null check|`obj != null`|enables smart cast in block|
|Early exit|`obj ?: return`|return from function if null|
|Early throw|`obj ?: throw Ex()`|throw if null|
|let block|`obj?.let { }`|execute block only if non-null|

---

# 14. Sorting

## 14.1 Sorting Arrays

### `IntArray` — Sort IN PLACE

```kotlin
val arr = intArrayOf(3, 1, 4, 1, 5, 9, 2, 6)

arr.sort()                  // [1, 1, 2, 3, 4, 5, 6, 9] — ascending, IN PLACE
arr.sortDescending()        // [9, 6, 5, 4, 3, 2, 1, 1] — descending, IN PLACE

// Sort a subrange only — index from (inclusive) to to (exclusive)
arr.sort(2, 6)              // sorts only indices 2, 3, 4, 5 — IN PLACE

// Returns Unit — do NOT assign the return value
// ❌ val sorted = arr.sort()    // sorted = Unit, not what you want
```

### `IntArray` — Get New Sorted List (Original Unchanged)

```kotlin
val arr = intArrayOf(3, 1, 4, 1, 5)

val asc: List<Int> = arr.sorted()              // [1, 1, 3, 4, 5] — new List
val desc: List<Int> = arr.sortedDescending()   // [5, 4, 3, 1, 1] — new List
// arr is still [3, 1, 4, 1, 5]

// Back to IntArray if needed
val sortedArr: IntArray = arr.sorted().toIntArray()
```

### `LongArray`, `DoubleArray` etc.

```kotlin
val lArr = longArrayOf(3L, 1L, 4L)
lArr.sort()                // same API — in place

val dArr = doubleArrayOf(1.5, 0.5, 2.5)
dArr.sort()
```

---

## 14.2 Sorting Lists

### Sort MutableList IN PLACE

```kotlin
val list = mutableListOf(3, 1, 4, 1, 5, 9)

list.sort()                             // ascending, IN PLACE
list.sortDescending()                   // descending, IN PLACE
list.sortBy { it }                      // ascending by key, IN PLACE
list.sortByDescending { it }            // descending by key, IN PLACE
list.sortWith(compareBy { it })         // with Comparator, IN PLACE
list.sortWith(compareByDescending { it })
list.reverse()                          // reverse current order, IN PLACE
```

### Sort List → New List (Original Unchanged)

```kotlin
val list = listOf(3, 1, 4, 1, 5)

list.sorted()                           // new List<T> ascending
list.sortedDescending()                 // new List<T> descending
list.sortedBy { it }                    // new List<T> by key ascending
list.sortedByDescending { it }          // new List<T> by key descending
list.sortedWith(compareBy { it })       // new List<T> with Comparator
list.reversed()                         // new List<T> reversed
```

---

## 14.3 Sorting Objects (Array<T>)

> For `Array<T>` (non-primitive arrays), `sortedBy` and `sortWith` work directly. For `IntArray`, you must use `sortedBy`which returns a `List`, not `IntArray`.

```kotlin
val words = arrayOf("banana", "apple", "cherry", "date")

// Sort in place
words.sort()                              // alphabetical, IN PLACE
words.sortWith { a, b -> a.length - b.length }  // by length, IN PLACE

// Sort to new list
words.sortedBy { it.length }             // new List<String> by length
words.sortedByDescending { it.length }   // by length descending
words.sortedWith(compareBy { it })       // alphabetical, new list
```

### Array of Arrays (Common in DSA)

```kotlin
val intervals = arrayOf(
    intArrayOf(1, 3),
    intArrayOf(0, 4),
    intArrayOf(2, 5)
)

// Sort by first element — IN PLACE
intervals.sortWith { a, b -> a[0] - b[0] }

// Safe version (no overflow)
intervals.sortWith { a, b -> Integer.compare(a[0], b[0]) }

// Sort by first element, break ties by second — IN PLACE
intervals.sortWith(compareBy({ it[0] }, { it[1] }))

// New sorted list
val sorted = intervals.sortedBy { it[0] }            // List<IntArray>
val sorted = intervals.sortedWith(compareBy { it[0] }) // same
```

---

## 14.4 Sorting by Keys

### `sortedBy` — Single Key

```kotlin
data class Person(val name: String, val age: Int)
val people = mutableListOf(Person("Charlie", 30), Person("Alice", 25), Person("Bob", 30))

// Sort by single field
people.sortBy { it.age }            // ascending by age, IN PLACE
people.sortByDescending { it.name } // descending by name, IN PLACE

// New sorted list
people.sortedBy { it.age }
people.sortedByDescending { it.age }
```

### `sortedWith` + `compareBy` — Multi-Key Sort

```kotlin
// Sort by age ascending, then name ascending (for ties)
people.sortWith(compareBy({ it.age }, { it.name }))

// Sort by age descending, then name ascending
people.sortWith(compareByDescending<Person> { it.age }.thenBy { it.name })

// Sort by age ascending, then name descending
people.sortWith(compareBy<Person> { it.age }.thenByDescending { it.name })
```

### `compareBy` — Building Comparators

```kotlin
// Single key
compareBy { it.age }                        // ascending by age
compareByDescending { it.age }              // descending by age

// Multiple keys
compareBy({ it.age }, { it.name })          // age asc, then name asc
compareByDescending { it.age }              // age desc

// Chain with thenBy / thenByDescending
compareBy<Person> { it.age }
    .thenBy { it.name }
    .thenByDescending { it.age }            // (unusual but valid)
```

### `sortedWith` on IntArray (via `Comparator`)

```kotlin
val arr = intArrayOf(3, 1, 4, 1, 5)

// sortedWith on IntArray → returns List<Int>
arr.sortedWith(compareBy { it })            // ascending
arr.sortedWith(compareByDescending { it })  // descending
```

---

## 14.5 Comparator Rules

### Lambda Comparator Contract

```kotlin
// Comparator { a, b -> ... } must return:
//   negative Int  → a comes BEFORE b
//   0             → a and b are equal
//   positive Int  → a comes AFTER b

// Min-heap / ascending
{ a: Int, b: Int -> a - b }            // ⚠️ overflow risk
{ a: Int, b: Int -> Integer.compare(a, b) }  // ✅ safe

// Max-heap / descending
{ a: Int, b: Int -> b - a }            // ⚠️ overflow risk
{ a: Int, b: Int -> Integer.compare(b, a) }  // ✅ safe

// By field
{ a: IntArray, b: IntArray -> a[0] - b[0] }         // ⚠️ overflow risk
{ a: IntArray, b: IntArray -> Integer.compare(a[0], b[0]) }  // ✅
```

### ⚠️ Subtraction Overflow in Comparators

```kotlin
// WHY subtraction is dangerous:
// If a = Int.MIN_VALUE (-2147483648) and b = 1:
// a - b = -2147483648 - 1 = +2147483647 (OVERFLOW → positive!)
// Comparator now thinks a > b — WRONG result

// ❌ Overflow-prone
PriorityQueue<Int> { a, b -> a - b }
intervals.sortWith { a, b -> a[0] - b[0] }

// ✅ Safe alternatives
PriorityQueue<Int> { a, b -> Integer.compare(a, b) }
intervals.sortWith { a, b -> Integer.compare(a[0], b[0]) }
compareBy { it[0] }                     // always safe
```

### `Integer.compare` vs Subtraction

||`a - b`|`Integer.compare(a, b)`|
|---|---|---|
|Returns|difference|-1, 0, or 1|
|Overflow risk|✅ YES for extreme values|❌ None|
|Performance|marginally faster|negligible difference|
|Safe to use|only for small known values|always|

### Sorting Stability

> Kotlin's `sort()` and `sortedWith()` are **stable** — equal elements keep their original relative order. `Array.sort()` for primitives (IntArray) may NOT be stable (uses dual-pivot quicksort). `MutableList.sort()` and `sortedWith()` ARE stable.

---

## Sorting — Quick Reference Table

|What you have|What you want|Method|Modifies?|
|---|---|---|---|
|`IntArray`|sort ascending|`arr.sort()`|✅ in place|
|`IntArray`|sort descending|`arr.sortDescending()`|✅ in place|
|`IntArray`|new sorted List|`arr.sorted()`|❌ new|
|`IntArray`|new sorted IntArray|`arr.sorted().toIntArray()`|❌ new|
|`MutableList<T>`|sort ascending|`list.sort()`|✅ in place|
|`MutableList<T>`|sort descending|`list.sortDescending()`|✅ in place|
|`MutableList<T>`|sort by key|`list.sortBy { key }`|✅ in place|
|`MutableList<T>`|multi-key sort|`list.sortWith(compareBy({},{})`|✅ in place|
|`List<T>` (immutable)|new sorted|`list.sorted()`|❌ new|
|`Array<T>`|sort in place|`arr.sort()` or `arr.sortWith {}`|✅ in place|
|`Array<IntArray>`|sort by field|`arr.sortWith { a, b -> Integer.compare(a[0], b[0]) }`|✅ in place|

---

# 15. Math Utilities

```kotlin
import kotlin.math.*    // imports abs, sqrt, pow, ceil, floor, log, etc.
```

---

## 15.1 `abs` — Absolute Value

```kotlin
abs(-5)          // 5     (Int)
abs(-5L)         // 5L    (Long)
abs(-3.14)       // 3.14  (Double)
abs(-2.5f)       // 2.5f  (Float)

(-5).absoluteValue   // 5  — extension property (Kotlin style)

// ⚠️ abs(Int.MIN_VALUE) == Int.MIN_VALUE (still negative!)
// Int.MIN_VALUE = -2147483648
// abs(-2147483648) = -2147483648  ← OVERFLOW, no positive equivalent
// ✅ Use Long if the value might be Int.MIN_VALUE
abs(Int.MIN_VALUE.toLong())   // 2147483648L — correct
```

---

## 15.2 `min` and `max`

### Two-Argument Forms

```kotlin
min(3, 7)          // 3  — smaller
max(3, 7)          // 7  — larger

min(3L, 7L)        // 3L
min(3.0, 7.0)      // 3.0

// Kotlin infix style (extension functions)
3.coerceAtMost(7)  // 3  — min(3, 7)
7.coerceAtLeast(3) // 7  — max(7, 3)
5.coerceIn(1, 10)  // 5  — clamp: max(1, min(5, 10))
15.coerceIn(1, 10) // 10 — clamped to max
-5.coerceIn(1, 10) // 1  — clamped to min
```

### Multi-Argument Forms (Kotlin)

```kotlin
maxOf(1, 2, 3)          // 3
minOf(1, 2, 3)          // 1
maxOf(a, b, c, d)        // max of 4 values

// On collections
listOf(3,1,4,1,5).max()          // 5 (throws if empty)
listOf(3,1,4,1,5).maxOrNull()    // 5 (null if empty)
listOf(3,1,4,1,5).min()          // 1
listOf(3,1,4,1,5).minOrNull()    // 1

// Max/min with key selector
listOf("apple","fig","banana").maxBy { it.length }   // "banana"
listOf("apple","fig","banana").minBy { it.length }   // "fig"
listOf("apple","fig","banana").maxOf { it.length }   // 6 (the length, not the word)
listOf("apple","fig","banana").minOf { it.length }   // 3
```

---

## 15.3 `pow` — Exponentiation

```kotlin
// kotlin.math.pow — works on Double only
2.0.pow(10.0)          // 1024.0
2.0.pow(0.5)           // ~1.414 (square root of 2)
(-2.0).pow(3.0)        // -8.0

// Must convert to Double for pow
val base = 2
val exp = 10
base.toDouble().pow(exp.toDouble())   // 1024.0

// Integer power (write manually — no built-in Int pow)
fun pow(base: Long, exp: Int): Long {
    var result = 1L
    var b = base
    var e = exp
    while (e > 0) {
        if (e % 2 == 1) result *= b
        b *= b
        e /= 2
    }
    return result
}
```

---

## 15.4 `sqrt` — Square Root

```kotlin
sqrt(16.0)       // 4.0
sqrt(2.0)        // 1.4142135623730951
sqrt(0.0)        // 0.0
sqrt(-1.0)       // NaN  — negative input

// Integer sqrt (floor)
val n = 25
val root = sqrt(n.toDouble()).toInt()    // 5
root * root == n                         // true — perfect square check

// ⚠️ Floating point precision — use with care for exact checks
sqrt(99999999.0).toInt()    // be careful — might be off by 1
// ✅ Safer perfect square check:
fun isPerfectSquare(n: Int): Boolean {
    val r = sqrt(n.toDouble()).toInt()
    return r * r == n || (r + 1) * (r + 1) == n
}
```

---

## 15.5 `ceil` and `floor`

```kotlin
ceil(3.2)     // 4.0  — round UP to nearest integer
ceil(3.0)     // 3.0  — already integer, unchanged
ceil(-3.2)    // -3.0 — round UP (toward zero for negatives)
ceil(3.9)     // 4.0

floor(3.9)    // 3.0  — round DOWN to nearest integer
floor(3.0)    // 3.0
floor(-3.2)   // -4.0 — round DOWN (away from zero for negatives)
floor(-3.9)   // -4.0

// Both return Double — convert if needed
ceil(3.2).toInt()    // 4
floor(3.9).toInt()   // 3

// Integer ceiling division (no floating point needed)
// ceil(a / b) for positive integers a, b:
(a + b - 1) / b      // integer arithmetic — exact, no floating point

// Examples
(7 + 2) / 3 = 3      // ceil(7/3) = ceil(2.33) = 3 ✅
(6 + 2) / 3 = 2      // ceil(6/3) = 2 ✅
(7 + 3) / 4 = 2      // ceil(7/4) = ceil(1.75) = 2 ✅
```

---

## 15.6 `round`

```kotlin
round(3.5)     // 4.0 — rounds to nearest, ties go UP
round(4.5)     // 5.0
round(3.4)     // 3.0
round(-3.5)    // -3.0 — ties round toward positive infinity
round(3.5).toInt()    // 4
```

---

## 15.7 `log` — Logarithms

```kotlin
// Natural log (base e)
ln(1.0)         // 0.0
ln(E)           // 1.0
ln(100.0)       // 4.605...

// Log base 10
log10(10.0)     // 1.0
log10(1000.0)   // 3.0
log10(1.0)      // 0.0

// Log base 2
log2(8.0)       // 3.0
log2(1024.0)    // 10.0
log2(1.0)       // 0.0

// Log with arbitrary base
log(100.0, 10.0)   // 2.0 — log base 10 of 100

// Integer log2 (floor) — how many bits needed to represent n
fun log2Floor(n: Int): Int = 31 - Integer.numberOfLeadingZeros(n)
// OR
fun log2Floor(n: Int): Int = ln(n.toDouble()).div(ln(2.0)).toInt()

// E constant
Math.E    // 2.718281828...
E         // same (from kotlin.math)
```

---

## 15.8 Other Math Functions

```kotlin
// Trigonometry
sin(Math.PI / 2)   // 1.0
cos(0.0)           // 1.0
tan(Math.PI / 4)   // ~1.0

// Constants
PI        // 3.14159...  (from kotlin.math)
E         // 2.71828...

// Hypot — sqrt(a² + b²) without overflow
hypot(3.0, 4.0)    // 5.0

// Sign
sign(-5.0)    // -1.0
sign(0.0)     //  0.0
sign(5.0)     //  1.0

// Integer sign (Kotlin extension)
(-5).sign     // -1
0.sign        //  0
5.sign        //  1

// Truncate
truncate(3.7)    // 3.0 — remove decimal (toward zero)
truncate(-3.7)   // -3.0

// Remainder with IEEE semantics
6.0.rem(4.0)    // 2.0
```

---

## Integer Math Patterns (No Import Needed)

```kotlin
// Integer division — truncates toward zero
7 / 2       // 3  (not 3.5, not 4)
-7 / 2      // -3 (truncates toward zero)

// Modulo — result has same sign as dividend
7 % 3       // 1
-7 % 3      // -1  ← can be negative in Kotlin/Java!
7 % -3      // 1

// Safe mod (always non-negative)
fun mod(a: Int, m: Int): Int = ((a % m) + m) % m
mod(-7, 3)   // 2  — always non-negative

// GCD — Euclidean algorithm
fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
gcd(12, 8)   // 4
gcd(100, 75) // 25

// LCM
fun lcm(a: Int, b: Int): Long = a.toLong() * b / gcd(a, b)
lcm(4, 6)    // 12

// Ceiling division (positive integers only)
fun ceilDiv(a: Int, b: Int): Int = (a + b - 1) / b

// Check if n is even/odd
n % 2 == 0    // even
n % 2 != 0   // odd
n and 1 == 0  // even — bit trick
n and 1 == 1  // odd  — bit trick
```

---

## Modular Arithmetic (for large number problems)

```kotlin
val MOD = 1_000_000_007L

// All intermediate values must be Long to avoid overflow
var result = 1L
result = (result * a) % MOD
result = (result + b) % MOD

// ⚠️ Always apply MOD after each multiplication, not just at the end
// (a * b) % MOD is correct
// a * b % MOD might overflow before % is applied if a,b are large

// Power with modular arithmetic
fun powMod(base: Long, exp: Long, mod: Long): Long {
    var result = 1L
    var b = base % mod
    var e = exp
    while (e > 0) {
        if (e % 2L == 1L) result = result * b % mod
        b = b * b % mod
        e /= 2
    }
    return result
}
```

---

## Math — Complete Quick Reference

|Function|Returns|Use|Notes|
|---|---|---|---|
|`abs(x)`|same type|absolute value|⚠️ abs(MIN_INT) overflow|
|`max(a,b)`|same type|larger of two||
|`min(a,b)`|same type|smaller of two||
|`maxOf(a,b,c)`|same type|max of varargs||
|`minOf(a,b,c)`|same type|min of varargs||
|`x.coerceIn(lo,hi)`|same type|clamp x to [lo,hi]||
|`sqrt(x)`|`Double`|square root|arg must be Double|
|`x.pow(n)`|`Double`|x to the n|both must be Double|
|`ceil(x)`|`Double`|round up|returns Double|
|`floor(x)`|`Double`|round down|returns Double|
|`round(x)`|`Long`|round to nearest||
|`ln(x)`|`Double`|natural log||
|`log2(x)`|`Double`|log base 2||
|`log10(x)`|`Double`|log base 10||
|`log(x, base)`|`Double`|log any base||
|`sign(x)`|`Double`|-1.0, 0.0, 1.0|use `.sign` for Int|
|`hypot(x, y)`|`Double`|sqrt(x²+y²)||
|`PI`|`Double`|π constant|from kotlin.math|
|`E`|`Double`|e constant|from kotlin.math|

---

## CHAPTER SUMMARY (Ch 11–15)

### Loop Selection Guide

|Need|Use|
|---|---|
|Count from 0 to n-1|`for (i in 0 until n)`|
|Inclusive range|`for (i in 1..n)`|
|Reverse|`for (i in n-1 downTo 0)`|
|Every other element|`for (i in 0 until n step 2)`|
|Index + value|`for ((i, v) in arr.withIndex())`|
|Break on condition|`for` loop with `break`|
|Exit outer loop|labeled `break@label`|
|Skip iteration|`continue` (for) or `return@forEach`|
|Fixed count, no index|`repeat(n)`|
|While condition unknown|`while (cond)`|

### Null Safety Quick Decisions

|Situation|Pattern|
|---|---|
|Map access|`map[key] ?: 0`|
|List safe access|`list.getOrNull(i) ?: default`|
|Skip if null|`val x = expr ?: return`|
|Execute if not null|`expr?.let { }`|
|Chain nullable fields|`a?.b?.c ?: default`|
|Remove nulls from list|`.filterNotNull()`|
|Cast safely|`obj as? Type ?: default`|

### Sorting Quick Decisions

|Situation|Method|
|---|---|
|Sort IntArray in place|`arr.sort()`|
|Sort MutableList in place|`list.sort()`|
|Get new sorted, keep original|`.sorted()`|
|Sort by custom field|`.sortBy { it.field }`|
|Multi-key sort|`sortWith(compareBy({},{})`|
|Comparator (safe)|`Integer.compare(a, b)`|
|Comparator (unsafe, overflow risk)|`a - b`|

---

_End of Chapters 11–15_ _Next: Ch 16 Bit Operations · Ch 17 Lambdas & Collection Ops · Ch 18 Pair, Triple & Destructuring · Ch 19 Type Conversion Table · Ch 20 when Expression_

# Kotlin DSA Reference Guide

### Chapters 16–20 | Pure API & Syntax Reference | No Problem Solutions

---

## MASTER INDEX (This File)

|#|Chapter|
|---|---|
|16|[Bit Operations](#16-bit-operations)|
|17|[Lambdas & Collection Operations](#17-lambdas--collection-operations)|
|18|[Pair, Triple & Destructuring](#18-pair-triple--destructuring)|
|19|[Type Conversion Table](#19-type-conversion-table)|
|20|[when Expression](#20-when-expression)|

---

# 16. Bit Operations

## 16.1 Bit Operators

> In Kotlin, bitwise operators are **named functions / infix operators**, not symbols like in Java. Java `&`, `|`, `^`, `~`, `<<`, `>>`, `>>>` → Kotlin `and`, `or`, `xor`, `inv()`, `shl`, `shr`, `ushr`

### Operator Reference Table

|Operation|Kotlin|Java Equivalent|Example|Result|
|---|---|---|---|---|
|AND|`a and b`|`a & b`|`5 and 3`|`1`|
|OR|`a or b`|`a \| b`|`5 or 3`|`7`|
|XOR|`a xor b`|`a ^ b`|`5 xor 3`|`6`|
|NOT (invert)|`a.inv()`|`~a`|`5.inv()`|`-6`|
|Left Shift|`a shl n`|`a << n`|`1 shl 3`|`8`|
|Signed Right Shift|`a shr n`|`a >> n`|`8 shr 2`|`2`|
|Unsigned Right Shift|`a ushr n`|`a >>> n`|`-1 ushr 1`|`2147483647`|

### How Each Operator Works

```kotlin
val a = 5   // binary: 0101
val b = 3   // binary: 0011

// AND — bit is 1 only if BOTH bits are 1
a and b     // 0101 AND 0011 = 0001 = 1

// OR — bit is 1 if EITHER bit is 1
a or b      // 0101 OR  0011 = 0111 = 7

// XOR — bit is 1 if bits are DIFFERENT
a xor b     // 0101 XOR 0011 = 0110 = 6

// NOT — flips all bits (inverts)
a.inv()     // ~0101 = ...11111010 = -6  (two's complement)
// inv() result = -(a + 1) for signed integers

// Left Shift — shift bits left, fill with 0 on right (multiply by 2^n)
1 shl 3     // 0001 → 1000 = 8    (1 × 2³)
a shl 1     // 0101 → 1010 = 10   (5 × 2)
a shl 2     // 0101 → 10100 = 20  (5 × 4)

// Signed Right Shift — shift bits right, fill with sign bit on left (divide by 2^n)
8 shr 2     // 1000 → 0010 = 2    (8 ÷ 4)
-8 shr 1    // fills with 1 on left (preserves sign)  = -4

// Unsigned Right Shift — shift bits right, ALWAYS fill with 0 (ignores sign)
-1 ushr 1   // 11...1 → 01...1 = 2147483647 (Int.MAX_VALUE)
```

### Long Bitwise Operations

```kotlin
// Same operators work on Long
val a = 5L
val b = 3L
a and b     // 1L
a or b      // 7L
a xor b     // 6L
a.inv()     // -6L
1L shl 40   // useful for large bit manipulation
```

---

## 16.2 Bit Tricks

### Check if Number is Even or Odd

```kotlin
n and 1 == 0    // true if n is EVEN
n and 1 == 1    // true if n is ODD
n and 1 != 0    // true if n is ODD

// Same as n % 2 == 0, but slightly faster
```

### Power of Two Check

```kotlin
// A power of two in binary has exactly ONE bit set: 1, 10, 100, 1000...
// n - 1 flips all bits up to and including that bit: 0, 01, 011, 0111...
// n AND (n-1) clears the lowest set bit
// If result is 0, exactly one bit was set → power of two

fun isPowerOfTwo(n: Int): Boolean = n > 0 && (n and (n - 1)) == 0

isPowerOfTwo(1)    // true  (2^0)
isPowerOfTwo(2)    // true  (2^1)
isPowerOfTwo(4)    // true  (2^2)
isPowerOfTwo(6)    // false (110 — two bits set)
isPowerOfTwo(0)    // false (0 is not a power of two)
isPowerOfTwo(-4)   // false (negative → not a power of two)

// Nearest power of two ≥ n (useful for segment trees)
var p = 1
while (p < n) p = p shl 1      // p = smallest power of 2 ≥ n
```

### Set a Bit — Turn Bit i ON

```kotlin
// Set bit at position i (0-indexed from right)
// OR with a mask that has only bit i set (1 shl i)

fun setBit(n: Int, i: Int): Int = n or (1 shl i)

setBit(5, 1)   // 5 = 0101, i=1 → mask=0010 → 0111 = 7
setBit(5, 3)   // 5 = 0101, i=3 → mask=1000 → 1101 = 13
setBit(0, 0)   // 0 = 0000, i=0 → mask=0001 → 0001 = 1
```

### Clear a Bit — Turn Bit i OFF

```kotlin
// Clear bit at position i
// AND with a mask that has all bits set EXCEPT bit i
// inv() of (1 shl i) gives ...1111101111... (all 1s except position i)

fun clearBit(n: Int, i: Int): Int = n and (1 shl i).inv()

clearBit(7, 1)   // 7 = 0111, i=1 → mask=...11111101 → 0101 = 5
clearBit(5, 0)   // 5 = 0101, i=0 → mask=...11111110 → 0100 = 4
clearBit(5, 2)   // 5 = 0101, i=2 → mask=...11111011 → 0001 = 1
```

### Check a Bit — Is Bit i Set?

```kotlin
// Check if bit at position i is 1
// Shift right by i to bring bit i to position 0, then AND with 1

fun checkBit(n: Int, i: Int): Boolean = (n shr i) and 1 == 1

// Alternative — mask approach
fun checkBit2(n: Int, i: Int): Boolean = (n and (1 shl i)) != 0

checkBit(5, 0)   // 5 = 0101, bit 0 = 1 → true
checkBit(5, 1)   // 5 = 0101, bit 1 = 0 → false
checkBit(5, 2)   // 5 = 0101, bit 2 = 1 → true
checkBit(5, 3)   // 5 = 0101, bit 3 = 0 → false
```

### Toggle a Bit — Flip Bit i

```kotlin
// XOR with mask: if bit is 0 → 1, if bit is 1 → 0
fun toggleBit(n: Int, i: Int): Int = n xor (1 shl i)

toggleBit(5, 0)   // 5 = 0101, flip bit 0 → 0100 = 4
toggleBit(5, 1)   // 5 = 0101, flip bit 1 → 0111 = 7
```

### Clear Lowest Set Bit

```kotlin
// n AND (n-1) removes the lowest 1-bit
// Used in power-of-two check and counting set bits

n and (n - 1)

// Example:
// 12 = 1100
// 11 = 1011
// 12 AND 11 = 1000 = 8  (lowest set bit removed)

// Count set bits using this trick
fun countSetBits(n: Int): Int {
    var count = 0
    var x = n
    while (x != 0) {
        x = x and (x - 1)    // remove lowest set bit
        count++
    }
    return count
}
```

### Get Lowest Set Bit

```kotlin
// n AND (-n) isolates the lowest set bit
// -n in two's complement flips bits and adds 1

fun lowestSetBit(n: Int): Int = n and (-n)

lowestSetBit(12)   // 12 = 1100, -12 = 0100 → 12 AND -12 = 0100 = 4
lowestSetBit(6)    // 6 = 0110  → lowest bit = 0010 = 2
lowestSetBit(8)    // 8 = 1000  → lowest bit = 1000 = 8
```

### Count Set Bits (Built-In)

```kotlin
Integer.bitCount(n)         // Java built-in — always works
n.countOneBits()            // Kotlin built-in (Kotlin 1.5+)

Integer.bitCount(7)         // 7 = 0111 → 3 set bits
Integer.bitCount(255)       // 255 = 11111111 → 8 set bits
Integer.bitCount(0)         // 0
```

### XOR Properties (Very Useful in DSA)

```kotlin
// Key properties of XOR:
// a XOR a = 0    (same values cancel out)
// a XOR 0 = a    (XOR with 0 returns same value)
// XOR is commutative and associative

// Cancel duplicates — XOR all values; paired values cancel, lone value remains
var result = 0
for (n in nums) result = result xor n
// result = the single unpaired value

// Swap without temp variable
a = a xor b
b = a xor b     // b becomes original a (since (a xor b) xor b = a)
a = a xor b     // a becomes original b
// ⚠️ Only works if a and b are different variables (i.e., different memory locations)
// arr[i] = arr[i] xor arr[i] — breaks if i == j

// Bit difference between two numbers
a xor b         // bits that are DIFFERENT in a and b are set to 1
Integer.bitCount(a xor b)   // count positions where a and b differ
```

### Bit Shifting as Multiplication / Division

```kotlin
// Left shift = multiply by power of 2
n shl 1     // n × 2
n shl 2     // n × 4
n shl k     // n × 2^k

// Right shift = divide by power of 2 (floor division for positive numbers)
n shr 1     // n ÷ 2
n shr 2     // n ÷ 4
n shr k     // n ÷ 2^k

// Middle of range (safe from overflow)
val mid = left + ((right - left) shr 1)    // same as (left+right)/2 but no overflow

// Check if number is negative (sign bit)
(n shr 31) and 1    // 1 if negative, 0 if positive (for Int)
n < 0               // cleaner way to check
```

### Bit Mask Patterns

```kotlin
// Create mask with lowest k bits set: 0...0111...1 (k ones)
val mask = (1 shl k) - 1

// Example: k=3 → (1 shl 3) - 1 = 8 - 1 = 7 = 0111

// Extract lowest k bits of n
n and ((1 shl k) - 1)

// Check if all k lowest bits are set
n and ((1 shl k) - 1) == (1 shl k) - 1

// Iterate all subsets of a bitmask m
var sub = m
while (sub > 0) {
    // process sub
    sub = (sub - 1) and m    // next subset
}
```

### Bit Operations — All Tricks Summary

|Trick|Expression|Notes|
|---|---|---|
|Check even|`n and 1 == 0`||
|Check odd|`n and 1 == 1`||
|Check power of 2|`n > 0 && (n and (n-1)) == 0`||
|Set bit i|`n or (1 shl i)`||
|Clear bit i|`n and (1 shl i).inv()`||
|Toggle bit i|`n xor (1 shl i)`||
|Check bit i|`(n shr i) and 1 == 1`||
|Get lowest set bit|`n and (-n)`||
|Clear lowest set bit|`n and (n - 1)`||
|Count set bits|`Integer.bitCount(n)`||
|Multiply by 2^k|`n shl k`||
|Divide by 2^k|`n shr k`|floor division|
|Safe midpoint|`left + ((right - left) shr 1)`|no overflow|
|XOR cancel pairs|`nums.fold(0) { acc, x -> acc xor x }`||
|k-bit mask|`(1 shl k) - 1`|lowest k bits = 1|

---

# 17. Lambdas & Collection Operations

## 17.1 Lambda Syntax

### Basic Lambda

```kotlin
// Full syntax: { parameters -> body }
val double: (Int) -> Int = { x: Int -> x * 2 }

// Type inferred from context
val double = { x: Int -> x * 2 }

// Single parameter — use `it` shorthand
val double: (Int) -> Int = { it * 2 }

// Two parameters
val add: (Int, Int) -> Int = { a, b -> a + b }

// No parameters
val greet: () -> String = { "hello" }

// Multi-line lambda — last expression is the return value
val process: (Int) -> Int = { x ->
    val doubled = x * 2
    doubled + 1       // implicit return — last expression
}
```

### Function Types

```kotlin
(Int) -> Int              // takes Int, returns Int
(Int, String) -> Boolean  // takes Int and String, returns Boolean
() -> Unit                // takes nothing, returns nothing
(Int) -> Unit             // takes Int, returns nothing
((Int) -> Boolean) -> Int // takes a lambda, returns Int — higher-order
```

### Passing Lambda as Last Argument (Trailing Lambda)

```kotlin
// Standard call
list.filter({ it > 3 })

// Trailing lambda — move lambda outside parentheses
list.filter { it > 3 }

// If lambda is the ONLY argument, drop parentheses entirely
list.filter { it > 3 }    // same as above

// Named lambda argument
list.sortWith(comparator = compareBy { it })
```

### Lambda with Receiver (`apply`, `also`, `let`, `run`, `with`)

```kotlin
// apply — receiver is `this`, returns receiver
val sb = StringBuilder().apply {
    append("hello")      // this = StringBuilder
    append(" world")
}

// also — receiver is `it`, returns receiver
val list = mutableListOf(1,2,3).also {
    it.add(4)
    println("list has ${it.size} elements")
}

// let — receiver is `it`, returns lambda result
val len = "hello"?.let { it.length * 2 }    // 10

// run — receiver is `this`, returns lambda result
val result = "hello".run { length + 2 }     // 7

// with — like run but receiver passed as argument
val result = with(StringBuilder()) {
    append("a"); append("b")
    toString()
}   // "ab"
```

---

## 17.2 Transformations

### `map` — Transform Every Element

```kotlin
val nums = listOf(1, 2, 3, 4, 5)

// Returns List<R> — one output per input, same count
nums.map { it * 2 }                  // [2, 4, 6, 8, 10]
nums.map { it.toString() }           // ["1","2","3","4","5"]
nums.map { it to it * it }           // [(1,1),(2,4),(3,9),(4,16),(5,25)]

// With index
nums.mapIndexed { i, v -> i to v }   // [(0,1),(1,2),(2,3),(3,4),(4,5)]

// Null-safe — transform + skip nulls in one pass
val mixed: List<String?> = listOf("1","x","2",null,"3")
mixed.mapNotNull { it?.toIntOrNull() }   // [1, 2, 3] — invalid and null skipped

// On IntArray — need toList() first OR use map directly (returns List)
intArrayOf(1,2,3).map { it * 2 }     // List<Int> [2,4,6]
intArrayOf(1,2,3).map { it * 2 }.toIntArray()  // IntArray [2,4,6]
```

### `filter` — Keep Matching Elements

```kotlin
val nums = listOf(1, 2, 3, 4, 5, 6)

nums.filter { it > 3 }               // [4, 5, 6]
nums.filter { it % 2 == 0 }          // [2, 4, 6]
nums.filterNot { it % 2 == 0 }       // [1, 3, 5] — keep NON-matching

// With index
nums.filterIndexed { i, v -> i % 2 == 0 }   // [1, 3, 5] — even indices

// Type filter
val mixed: List<Any> = listOf(1, "a", 2, "b", 3)
mixed.filterIsInstance<Int>()        // [1, 2, 3]
mixed.filterIsInstance<String>()     // ["a", "b"]

// Remove nulls
val nullable: List<Int?> = listOf(1, null, 2, null)
nullable.filterNotNull()             // [1, 2]
```

### `flatMap` — Map Then Flatten

```kotlin
val words = listOf("hello", "world")

// map produces List<List<Char>>, flatMap flattens to List<Char>
words.flatMap { it.toList() }
// ['h','e','l','l','o','w','o','r','l','d']

// With lists of lists
val nested = listOf(listOf(1,2), listOf(3,4), listOf(5))
nested.flatten()                     // [1,2,3,4,5] — just flatten, no transform
nested.flatMap { it.map { x -> x * 2 } }   // [2,4,6,8,10] — transform then flatten

// Generate pairs
(1..3).flatMap { i -> (1..3).map { j -> Pair(i, j) } }
// [(1,1),(1,2),(1,3),(2,1),(2,2),...,(3,3)]
```

### `map` vs `flatMap` Comparison

```kotlin
val words = listOf("hi", "bye")

words.map { it.toList() }       // [['h','i'], ['b','y','e']]  — List<List<Char>>
words.flatMap { it.toList() }   // ['h','i','b','y','e']       — List<Char> flattened
```

### `distinct` and `distinctBy`

```kotlin
listOf(1,2,2,3,3,3).distinct()            // [1,2,3] — remove duplicates
listOf("apple","avocado","banana","cherry")
    .distinctBy { it.first() }            // ["apple","banana","cherry"] — by first char
```

---

## 17.3 Aggregations

### `sum`, `sumOf`

```kotlin
val nums = listOf(1, 2, 3, 4, 5)

nums.sum()                           // 15 — works for List<Int>, List<Long>, etc.
nums.sumOf { it.toLong() }          // 15L — sum with transform
nums.sumOf { it * it }              // 55  — sum of squares
nums.sumOf { it.toDouble() }        // 15.0

intArrayOf(1,2,3).sum()             // 6 — works directly on IntArray
```

### `count`

```kotlin
val nums = listOf(1, 2, 3, 4, 5, 6)

nums.count()                         // 6 — same as size
nums.count { it > 3 }               // 3 — count matching predicate
nums.count { it % 2 == 0 }          // 3

"hello world".count { it == 'l' }   // 3 — on String
```

### `any` — True If at Least One Matches

```kotlin
val nums = listOf(1, 2, 3, 4, 5)

nums.any { it > 4 }                  // true  — 5 > 4
nums.any { it > 10 }                 // false — none > 10
nums.any()                           // true  — collection is non-empty
emptyList<Int>().any()               // false — empty collection

"hello".any { it == 'x' }           // false
```

### `all` — True If ALL Match

```kotlin
nums.all { it > 0 }                  // true  — all positive
nums.all { it > 3 }                  // false — 1,2,3 are not > 3
emptyList<Int>().all { it > 0 }     // true  — vacuously true (empty)
```

### `none` — True If NO Elements Match

```kotlin
nums.none { it > 10 }                // true  — none exceed 10
nums.none { it > 0 }                 // false — all exceed 0
emptyList<Int>().none { it > 0 }    // true  — empty = none match
```

### `reduce` — Combine Without Initial Value

```kotlin
val nums = listOf(1, 2, 3, 4, 5)

nums.reduce { acc, x -> acc + x }    // 15  — acc starts as first element
nums.reduce { acc, x -> acc * x }    // 120 — factorial
nums.reduce { acc, x -> maxOf(acc, x) }  // 5 — max

// ⚠️ Throws NoSuchElementException if list is EMPTY
// ✅ Use reduceOrNull for safety
nums.reduceOrNull { acc, x -> acc + x }   // Int? — null if empty
```

### `fold` — Combine With Initial Value

```kotlin
val nums = listOf(1, 2, 3, 4, 5)

// fold(initial) { acc, element -> newAcc }
nums.fold(0) { acc, x -> acc + x }   // 15   — sum, start = 0
nums.fold(1) { acc, x -> acc * x }   // 120  — product, start = 1
nums.fold(0L) { acc, x -> acc + x }  // 15L  — start as Long = result is Long
nums.fold("") { acc, x -> acc + x }  // "12345" — concatenation

// Build a collection
nums.fold(mutableListOf<Int>()) { acc, x ->
    if (x % 2 == 0) acc.add(x)
    acc
}   // [2, 4]

// ✅ fold works on empty lists (returns initial value)
emptyList<Int>().fold(0) { acc, x -> acc + x }   // 0 — no crash
```

### `fold` vs `reduce`

||`reduce`|`fold`|
|---|---|---|
|Initial value|First element of collection|You provide it|
|Empty collection|❌ throws|✅ returns initial value|
|Return type|Same as element type|Can be different type|

### `scan` — Fold but Keep Intermediates

```kotlin
val nums = listOf(1, 2, 3, 4, 5)

nums.scan(0) { acc, x -> acc + x }
// [0, 1, 3, 6, 10, 15]  — running prefix sums!
// First element is always the initial value

nums.scan(1) { acc, x -> acc * x }
// [1, 1, 2, 6, 24, 120]
```

### `max`, `min`, `maxOf`, `minOf`

```kotlin
val nums = listOf(3, 1, 4, 1, 5)

nums.max()                           // 5  — throws if empty
nums.maxOrNull()                     // 5  — null if empty ← prefer this
nums.min()                           // 1
nums.minOrNull()                     // 1

nums.maxBy { -it }                   // 1  — element with max key (-1)
nums.minBy { it }                    // 1  — element with min key
nums.maxOf { it * it }              // 25 — max KEY value, not element
nums.minOf { it * it }              // 1

// Multiple arguments
maxOf(3, 7, 2, 9)                    // 9
minOf(3, 7, 2, 9)                    // 2
```

---

## 17.4 Grouping

### `groupBy` — Partition into Map<K, List<V>>

```kotlin
val nums = listOf(1, 2, 3, 4, 5, 6)

// Returns Map<K, List<T>>
nums.groupBy { it % 3 }
// {1=[1,4], 2=[2,5], 0=[3,6]}  — key = it%3, value = list of matching elements

val words = listOf("apple", "banana", "avocado", "cherry", "blueberry")
words.groupBy { it.first() }
// {'a'=["apple","avocado"], 'b'=["banana","blueberry"], 'c'=["cherry"]}

// groupBy with value transform
words.groupBy(
    keySelector = { it.first() },
    valueTransform = { it.uppercase() }
)
// {'a'=["APPLE","AVOCADO"], 'b'=["BANANA","BLUEBERRY"], ...}
```

### `groupingBy` + `eachCount` — Frequency Map

```kotlin
val s = "hello world"

// Most efficient way to count frequency
s.groupingBy { it }.eachCount()
// {' '=1, 'h'=1, 'e'=1, 'l'=3, 'o'=2, 'w'=1, 'r'=1, 'd'=1}

listOf(1,2,2,3,3,3).groupingBy { it }.eachCount()
// {1=1, 2=2, 3=3}
```

### `groupingBy` — Other Aggregations

```kotlin
val nums = listOf(1, 2, 3, 4, 5, 6)

// Count — same as eachCount but on Grouping object
nums.groupingBy { it % 2 }.eachCount()
// {1=3, 0=3}

// Fold per group
nums.groupingBy { it % 2 }.fold(0) { acc, e -> acc + e }
// {1=9 (1+3+5), 0=12 (2+4+6)}

// Reduce per group
nums.groupingBy { it % 2 }.reduce { _, acc, e -> acc + e }
// {1=9, 0=12}

// Aggregate — most flexible
nums.groupingBy { it % 2 }.aggregate { key, acc: Int?, e, _ ->
    (acc ?: 0) + e
}
```

---

## 17.5 Partitioning

### `partition` — Split into Two Lists Based on Predicate

```kotlin
val nums = listOf(1, 2, 3, 4, 5, 6)

// Returns Pair<List<T>, List<T>>
// first  = elements matching predicate
// second = elements NOT matching predicate
val (evens, odds) = nums.partition { it % 2 == 0 }
// evens = [2, 4, 6]
// odds  = [1, 3, 5]

val (passed, failed) = scores.partition { it >= 60 }

// Without destructuring
val result = nums.partition { it > 3 }
result.first    // [4, 5, 6] — matching
result.second   // [1, 2, 3] — not matching
```

### `zip` — Pair Elements from Two Collections

```kotlin
val a = listOf(1, 2, 3)
val b = listOf("a", "b", "c")

a.zip(b)                  // [(1,"a"), (2,"b"), (3,"c")]  — List<Pair<Int,String>>

// With transform
a.zip(b) { x, y -> "$x:$y" }   // ["1:a","2:b","3:c"]

// ⚠️ If lengths differ — stops at the SHORTER one
listOf(1,2,3).zip(listOf("a","b"))    // [(1,"a"),(2,"b")] — 3 is dropped
```

### `zipWithNext` — Consecutive Pairs

```kotlin
val nums = listOf(1, 2, 3, 4, 5)

nums.zipWithNext()                 // [(1,2),(2,3),(3,4),(4,5)]
nums.zipWithNext { a, b -> b - a } // [1,1,1,1]  — differences
```

### `chunked` — Split into Groups of Size n

```kotlin
val nums = listOf(1, 2, 3, 4, 5, 6, 7)

nums.chunked(3)            // [[1,2,3],[4,5,6],[7]]  — last chunk may be smaller
nums.chunked(2)            // [[1,2],[3,4],[5,6],[7]]

// With transform per chunk
nums.chunked(3) { it.sum() }   // [6, 15, 7]
```

### `windowed` — Sliding Windows

```kotlin
val nums = listOf(1, 2, 3, 4, 5)

nums.windowed(3)            // [[1,2,3],[2,3,4],[3,4,5]]  — size 3, step 1
nums.windowed(3, step = 2)  // [[1,2,3],[3,4,5]]
nums.windowed(3, partialWindows = true)  // includes partial windows at end

// With transform
nums.windowed(3) { it.sum() }   // [6, 9, 12]
```

---

### Collection Operations — Full Reference Table

|Method|Returns|What it does|
|---|---|---|
|`map { }`|`List<R>`|transform each element|
|`mapIndexed { i, e -> }`|`List<R>`|transform with index|
|`mapNotNull { }`|`List<R>`|transform + skip nulls|
|`filter { }`|`List<T>`|keep matching|
|`filterNot { }`|`List<T>`|keep not matching|
|`filterNotNull()`|`List<T>`|remove nulls|
|`filterIsInstance<T>()`|`List<T>`|keep specific type|
|`filterIndexed { i, e -> }`|`List<T>`|filter with index|
|`flatMap { }`|`List<R>`|map + flatten|
|`flatten()`|`List<T>`|flatten List<List<T>>|
|`distinct()`|`List<T>`|remove duplicates|
|`distinctBy { }`|`List<T>`|dedup by key|
|`groupBy { }`|`Map<K,List<T>>`|partition into groups|
|`groupingBy { }.eachCount()`|`Map<K,Int>`|frequency count|
|`partition { }`|`Pair<List,List>`|split into two lists|
|`zip(other)`|`List<Pair<T,U>>`|pair elements|
|`zipWithNext()`|`List<Pair<T,T>>`|consecutive pairs|
|`chunked(n)`|`List<List<T>>`|split into size-n groups|
|`windowed(n)`|`List<List<T>>`|sliding windows|
|`sum()`|numeric|sum|
|`sumOf { }`|numeric|sum with transform|
|`count()`|`Int`|size|
|`count { }`|`Int`|count matching|
|`any { }`|`Boolean`|any match|
|`all { }`|`Boolean`|all match|
|`none { }`|`Boolean`|none match|
|`reduce { }`|`T`|combine (no initial)|
|`fold(init) { }`|`R`|combine (with initial)|
|`scan(init) { }`|`List<R>`|fold keeping intermediates|
|`associate { }`|`Map<K,V>`|build map from elements|
|`associateWith { }`|`Map<T,V>`|element → value map|
|`associateBy { }`|`Map<K,T>`|key → element map|
|`take(n)`|`List<T>`|first n|
|`takeLast(n)`|`List<T>`|last n|
|`takeWhile { }`|`List<T>`|take while true|
|`drop(n)`|`List<T>`|skip first n|
|`dropLast(n)`|`List<T>`|skip last n|
|`dropWhile { }`|`List<T>`|skip while true|
|`first { }`|`T`|first matching (throws)|
|`firstOrNull { }`|`T?`|first matching (null)|
|`last { }`|`T`|last matching (throws)|
|`lastOrNull { }`|`T?`|last matching (null)|
|`single { }`|`T`|exactly one match (throws)|
|`singleOrNull { }`|`T?`|exactly one (null)|
|`indexOf(e)`|`Int`|first index|
|`indexOfFirst { }`|`Int`|first index matching|
|`indexOfLast { }`|`Int`|last index matching|
|`sortedBy { }`|`List<T>`|new sorted by key|
|`sortedWith(comp)`|`List<T>`|new sorted by comparator|

---

# 18. Pair, Triple & Destructuring

## 18.1 Pair

### Creation

```kotlin
val p1 = Pair(1, "hello")          // explicit constructor
val p2 = 1 to "hello"              // infix `to` — same result, preferred in Kotlin
val p3: Pair<Int, String> = 1 to "hello"

// Type: Pair<A, B>
val coords: Pair<Int, Int> = 3 to 4
val entry: Pair<String, Int> = "score" to 100
```

### Accessing

```kotlin
val p = 1 to "hello"

p.first     // 1       — first element
p.second    // "hello" — second element
```

### Destructuring

```kotlin
val p = 1 to "hello"
val (num, str) = p       // num = 1, str = "hello"

// Ignore a component with _
val (_, str) = p         // only need second element

// In a for loop
val pairs = listOf(1 to "a", 2 to "b", 3 to "c")
for ((num, letter) in pairs) {
    println("$num: $letter")
}
```

### Pair in Map Operations

```kotlin
// Map.toList() returns List<Pair<K,V>>
val map = mapOf("a" to 1, "b" to 2)
val pairs: List<Pair<String, Int>> = map.toList()

// Reconstruct map from list of pairs
val backToMap: Map<String, Int> = pairs.toMap()

// Sort entries by value
map.toList().sortedBy { it.second }   // sort by value

// Map entries as pairs
for ((key, value) in map) { }         // destructured automatically
```

---

## 18.2 Triple

### Creation

```kotlin
val t = Triple(1, "hello", true)
val t2: Triple<Int, String, Boolean> = Triple(1, "hello", true)
```

### Accessing

```kotlin
val t = Triple(1, "hello", true)

t.first    // 1
t.second   // "hello"
t.third    // true
```

### Destructuring

```kotlin
val t = Triple(1, "hello", true)
val (a, b, c) = t       // a=1, b="hello", c=true

val (x, _, z) = t       // skip second element
```

---

## 18.3 Destructuring

### Destructuring in Various Contexts

```kotlin
// Variable assignment
val (x, y) = Pair(3, 4)
val (a, b, c) = Triple(1, 2, 3)

// For loop
for ((key, value) in map) { }
for ((index, element) in list.withIndex()) { }
for ((a, b) in listOf(1 to 2, 3 to 4)) { }

// Function return (return Pair/Triple and destructure at call site)
fun getRange(arr: IntArray): Pair<Int, Int> = arr.min() to arr.max()
val (lo, hi) = getRange(intArrayOf(3, 1, 4, 1, 5))

// Ignore components with _
val (_, second, third) = Triple(1, 2, 3)
val (first, _) = 10 to 20
```

### Destructuring with `componentN()` Functions

> Kotlin destructuring works on any class that defines `component1()`, `component2()`, etc. Data classes generate these automatically.

```kotlin
// Works on:
// Pair          → component1(), component2()
// Triple        → component1(), component2(), component3()
// Data classes  → component1()...componentN() for each property
// Map.Entry     → component1() = key, component2() = value
// List (first 5 elements only via extension)
val list = listOf(1, 2, 3)
val (a, b, c) = list   // a=1, b=2, c=3 — only up to 5 components for List
```

---

## 18.4 Data Classes

> Data classes are the right way to represent structured state in DSA — BFS states, graph nodes, etc.

### Declaration

```kotlin
data class Point(val x: Int, val y: Int)
data class State(val row: Int, val col: Int, val steps: Int)
data class Edge(val from: Int, val to: Int, val weight: Int)
data class Interval(val start: Int, val end: Int)
```

### Auto-Generated Functions

```kotlin
data class Point(val x: Int, val y: Int)
val p = Point(3, 4)

// toString()
println(p)               // "Point(x=3, y=4)"

// equals() — compares by value, not reference
Point(3, 4) == Point(3, 4)   // true
Point(3, 4) === Point(3, 4)  // false (different objects)

// hashCode() — consistent with equals — can be used in HashMap/HashSet keys
val set = HashSet<Point>()
set.add(Point(1, 2))
set.contains(Point(1, 2))   // true — because hashCode and equals match

// copy() — create modified copy
val p2 = p.copy(x = 10)     // Point(x=10, y=4)  — y unchanged
val p3 = p.copy()           // exact copy

// componentN() — enables destructuring
val (x, y) = p              // x=3, y=4
```

### Data Class in HashMap / HashSet (As a Key)

```kotlin
// ✅ Data class works as HashMap key — auto equals() and hashCode()
val visited = HashSet<Point>()
visited.add(Point(0, 0))
visited.contains(Point(0, 0))    // true

val distMap = HashMap<Point, Int>()
distMap[Point(3, 4)] = 5
distMap[Point(3, 4)]             // 5

// ⚠️ Regular class (non-data) uses reference equality by default
// class Point(val x: Int, val y: Int)  — two Point(0,0) are NOT equal!
// Data class Point(val x: Int, val y: Int) — two Point(0,0) ARE equal ✅
```

### Data Class as BFS State

```kotlin
data class State(val r: Int, val c: Int, val steps: Int)

val queue = ArrayDeque<State>()
val visited = HashSet<Pair<Int,Int>>()   // visit by position, not full state

queue.addLast(State(0, 0, 0))

while (queue.isNotEmpty()) {
    val (r, c, steps) = queue.removeFirst()   // destructure directly
    // process...
}
```

### Data Class in PriorityQueue

```kotlin
data class Node(val dist: Int, val id: Int)

val heap = PriorityQueue<Node>(compareBy { it.dist })

heap.offer(Node(5, 2))
heap.offer(Node(1, 3))
val (dist, id) = heap.poll()!!     // destructure
```

---

# 19. Type Conversion Table

## 19.1 Number Conversions

### Int ↔ Other Numeric Types

```kotlin
val i: Int = 42

i.toByte()      // Int → Byte   (truncates — may lose data)
i.toShort()     // Int → Short  (truncates — may lose data)
i.toLong()      // Int → Long   (always safe)
i.toFloat()     // Int → Float  (may lose precision for large values)
i.toDouble()    // Int → Double (safe for all Int values)

val l: Long = 9_999_999_999L
l.toInt()       // Long → Int   ⚠️ truncates to lower 32 bits — may be wrong!
l.toDouble()    // Long → Double (safe, though may lose precision for huge longs)

val d: Double = 3.99
d.toInt()       // Double → Int  → 3 (truncates toward zero, does NOT round)
d.toLong()      // Double → Long → 3 (same)
d.toFloat()     // Double → Float

val f: Float = 3.14f
f.toDouble()    // Float → Double (safe)
f.toInt()       // Float → Int   → 3 (truncates)
```

### Char ↔ Int

```kotlin
'a'.code        // Char → Int (Unicode code point)  → 97
'A'.code        // → 65
'0'.code        // → 48
'9'.code        // → 57

97.toChar()     // Int → Char → 'a'
65.toChar()     // → 'A'

// Char arithmetic (does NOT auto-convert — you get Int result)
'e' - 'a'       // → Int 4   (index in alphabet)
'a' + 3         // ⚠️ returns Int (97 + 3 = 100), NOT Char
('a' + 3)       // Int 100
('a'.code + 3).toChar()   // → 'd'
// OR
'a' + 3         // Int — if you need Char: ('a' + 3).toChar()
```

### Explicit Conversion Required (No Implicit)

```kotlin
// ❌ Kotlin does NOT do implicit numeric conversion
val l: Long = 42        // ❌ compile error — 42 is Int, not Long
val l: Long = 42L       // ✅ literal suffix
val l: Long = 42.toLong() // ✅ explicit conversion
val d: Double = 42      // ❌ compile error
val d: Double = 42.0    // ✅ literal
val d: Double = 42.toDouble() // ✅
```

---

## 19.2 String Conversions

### Number → String

```kotlin
42.toString()           // "42"
42L.toString()          // "42"
3.14.toString()         // "3.14"
true.toString()         // "true"
'a'.toString()          // "a"
null.toString()         // "null" (works on nullable without crash)

// Format
"%d".format(42)         // "42"
"%05d".format(42)       // "00042"  — zero-padded to width 5
"%.2f".format(3.14159)  // "3.14"   — 2 decimal places
"%s and %d".format("hello", 42)  // "hello and 42"

// String templates (most common in Kotlin)
val n = 42
"value is $n"             // "value is 42"
"double is ${n * 2}"      // "double is 84"
```

### String → Number

```kotlin
"42".toInt()             // 42     — throws NumberFormatException if invalid
"42".toIntOrNull()       // 42     — returns null if invalid ← safe
"abc".toIntOrNull()      // null

"42".toLong()
"42".toLongOrNull()
"3.14".toDouble()
"3.14".toDoubleOrNull()
"3.14".toFloat()
"3.14".toFloatOrNull()
"true".toBoolean()       // true
"TRUE".toBoolean()       // true — case insensitive
"yes".toBoolean()        // false — only "true" (case insensitive) returns true

// Parse with radix (base)
"ff".toInt(16)           // 255 — hex to Int
"1010".toInt(2)          // 10  — binary to Int
"17".toInt(8)            // 15  — octal to Int
```

### String ↔ Char Array

```kotlin
"hello".toCharArray()           // CharArray ['h','e','l','l','o']
"hello".toList()                // List<Char>
"hello".toMutableList()         // MutableList<Char>

charArrayOf('h','i').concatToString()    // "hi"
String(charArrayOf('h','i'))             // "hi"
charArrayOf('h','i').joinToString("")    // "hi"

listOf('h','i').joinToString("")         // "hi"  (from List<Char>)
```

---

## 19.3 Array / List / Set Conversions

### Array Conversions

```kotlin
// IntArray ↔ Others
intArrayOf(1,2,3).toList()           // List<Int>
intArrayOf(1,2,3).toMutableList()    // MutableList<Int>
intArrayOf(1,2,3).toSet()            // Set<Int>
intArrayOf(1,2,3).toHashSet()        // HashSet<Int>
intArrayOf(1,2,3).toSortedSet()      // SortedSet<Int> (TreeSet)
intArrayOf(1,2,3).toTypedArray()     // Array<Int> (boxed)
intArrayOf(1,2,2,3).toHashSet()      // HashSet removes duplicates → {1,2,3}

// Array<T> ↔ Others
arrayOf("a","b").toList()            // List<String>
arrayOf("a","b").toMutableList()     // MutableList<String>
arrayOf(1,2,3).toIntArray()          // IntArray (unboxed)
```

### List Conversions

```kotlin
// List<Int> ↔ Others
listOf(1,2,3).toIntArray()           // IntArray
listOf(1,2,3).toLongArray()          // LongArray
listOf(1,2,3).toDoubleArray()        // DoubleArray
listOf(1,2,3).toTypedArray()         // Array<Int> (boxed)
listOf(1,2,3).toSet()                // Set<Int> (immutable)
listOf(1,2,3).toMutableSet()         // MutableSet<Int>
listOf(1,2,3).toHashSet()            // HashSet<Int>
listOf(1,2,3).toSortedSet()          // SortedSet<Int>
listOf(1,2,2,3).distinct()           // List<Int> [1,2,3] — no duplicates

// Mutable ↔ Immutable
listOf(1,2,3).toMutableList()        // List → MutableList
mutableListOf(1,2,3).toList()        // MutableList → immutable List
```

### Set Conversions

```kotlin
setOf(1,2,3).toList()               // List<Int> — order not guaranteed for HashSet
setOf(1,2,3).toMutableList()        // MutableList<Int>
setOf(1,2,3).toIntArray()           // IntArray
setOf(1,2,3).sorted()               // List<Int> sorted
setOf(1,2,3).toSortedSet()          // SortedSet (TreeSet)
setOf(1,2,3).toMutableSet()         // MutableSet
mutableSetOf(3,1,2).toHashSet()     // copy to HashSet
```

### Range Conversions

```kotlin
(1..5).toList()              // [1,2,3,4,5]
(1..5).toIntArray()          // IntArray [1,2,3,4,5]
(1..5).toSet()               // Set<Int>
(1..5).toMutableList()       // MutableList<Int>
('a'..'e').toList()          // ['a','b','c','d','e']
```

---

## 19.4 Map Conversions

```kotlin
val map = mapOf("a" to 1, "b" to 2)

// Map → List
map.toList()                   // List<Pair<String,Int>>  [("a",1),("b",2)]
map.entries.toList()           // List<Map.Entry<String,Int>>
map.keys.toList()              // List<String>
map.values.toList()            // List<Int>

// List<Pair> → Map
listOf("a" to 1, "b" to 2).toMap()          // Map<String,Int> (immutable)
listOf("a" to 1, "b" to 2).toMutableMap()   // MutableMap<String,Int> ← use for DSA

// Array<Pair> → Map
arrayOf("a" to 1, "b" to 2).toMap()

// Map → Sorted Map (by key)
map.toSortedMap()                            // SortedMap (TreeMap), sorted by key

// Mutable ↔ Immutable
mutableMapOf("a" to 1).toMap()              // MutableMap → immutable Map
mapOf("a" to 1).toMutableMap()              // Map → MutableMap ← useful!

// Reconstruct from separate key/value lists
val keys = listOf("a", "b", "c")
val values = listOf(1, 2, 3)
val map2 = keys.zip(values).toMap()          // {"a":1,"b":2,"c":3}
```

---

## 19.5 Master Conversion Quick Reference

```
Number conversions:
  Int → Long       : .toLong()
  Int → Double     : .toDouble()
  Int → String     : .toString()
  Int → Char       : .toChar()
  Long → Int       : .toInt()         ⚠️ may truncate
  Double → Int     : .toInt()         ⚠️ truncates (not rounds)
  Char → Int       : .code
  String → Int     : .toInt() / .toIntOrNull()

IntArray conversions:
  IntArray → List<Int>          : .toList()
  IntArray → MutableList<Int>   : .toMutableList()
  IntArray → Set<Int>           : .toSet() / .toHashSet()
  IntArray → Array<Int>         : .toTypedArray()

List<Int> conversions:
  List<Int> → IntArray          : .toIntArray()
  List<Int> → Array<Int>        : .toTypedArray()
  List<Int> → Set<Int>          : .toSet()
  List<Int> → MutableList<Int>  : .toMutableList()

Set conversions:
  Set<Int> → List<Int>          : .toList()
  Set<Int> → IntArray           : .toIntArray()
  Set<Int> → sorted List        : .sorted()
  Set<Int> → SortedSet          : .toSortedSet()

Map conversions:
  Map<K,V> → List<Pair<K,V>>    : .toList()
  Map<K,V> → MutableMap         : .toMutableMap()
  List<Pair<K,V>> → Map         : .toMap()
  List<Pair<K,V>> → MutableMap  : .toMutableMap()

String conversions:
  String → Int                  : .toInt() / .toIntOrNull()
  String → CharArray            : .toCharArray()
  String → List<Char>           : .toList()
  CharArray → String            : String(arr) / .concatToString()
  List<Char> → String           : .joinToString("")
```

---

# 20. when Expression

## 20.1 Basic Usage

### `when` with Argument (Like Enhanced Switch)

```kotlin
val x = 3

when (x) {
    1 -> println("one")
    2 -> println("two")
    3 -> println("three")
    else -> println("other")    // else = default (required when used as expression)
}
```

### Multiple Values on One Branch

```kotlin
when (x) {
    0, 1 -> println("zero or one")
    2, 3 -> println("two or three")
    else -> println("other")
}
```

### Range Check

```kotlin
when (x) {
    in 1..10    -> println("1 to 10")
    in 11..20   -> println("11 to 20")
    !in 1..100  -> println("outside 1-100")
    else        -> println("21 to 100")
}
```

### `when` Without Argument (Replaces if-else Chain)

```kotlin
when {
    x < 0      -> println("negative")
    x == 0     -> println("zero")
    x < 100    -> println("small positive")
    else       -> println("large positive")
}
```

---

## 20.2 Pattern Matching

### Type Check

```kotlin
val obj: Any = "hello"

when (obj) {
    is String  -> println("String of length ${obj.length}")   // smart cast!
    is Int     -> println("Int: $obj")
    is List<*> -> println("List of size ${obj.size}")
    else       -> println("Unknown")
}
```

> Smart cast: inside a `is String` branch, `obj` is automatically treated as `String`.

### Null Check in when

```kotlin
val s: String? = null

when (s) {
    null -> println("null")
    ""   -> println("empty string")
    else -> println("has content: $s")
}
```

### Type + Value Combined

```kotlin
when (obj) {
    is String    -> if (obj.length > 5) "long string" else "short string"
    is Int       -> if (obj > 0) "positive int" else "non-positive int"
    null         -> "null"
    else         -> "other"
}
```

### Sealed Class Matching (Exhaustive)

```kotlin
sealed class Result
class Success(val value: Int) : Result()
class Failure(val error: String) : Result()

fun handle(result: Result): String = when (result) {
    is Success -> "value: ${result.value}"
    is Failure -> "error: ${result.error}"
    // no else needed — sealed class is exhaustive
}
```

---

## 20.3 Expression vs Statement

### `when` as Statement (No Return Value)

```kotlin
// No return value assigned — just executes branches
when (x) {
    1 -> doThis()
    2 -> doThat()
    // else is NOT required when used as statement
}
```

### `when` as Expression (Returns a Value)

```kotlin
// Assigned to a variable — MUST have else (unless sealed/enum is exhaustive)
val label = when (x) {
    1    -> "one"
    2    -> "two"
    else -> "other"    // ← REQUIRED when used as expression
}

// Returned directly from function
fun describe(n: Int): String = when {
    n < 0  -> "negative"
    n == 0 -> "zero"
    else   -> "positive"
}

// In an argument
println(when (x) {
    1 -> "one"
    else -> "other"
})
```

### Multi-Line Branch Body

```kotlin
val result = when (x) {
    1 -> {
        println("processing one")
        "one"     // last expression = branch return value
    }
    2 -> {
        val doubled = x * 2
        "two ($doubled)"
    }
    else -> "other"
}
```

### `when` vs `if-else`

```kotlin
// Both are expressions in Kotlin:
val max = if (a > b) a else b

// when is better for 3+ branches:
val grade = when {
    score >= 90 -> "A"
    score >= 80 -> "B"
    score >= 70 -> "C"
    score >= 60 -> "D"
    else        -> "F"
}
```

---

## 20.4 `when` with Enum

```kotlin
enum class Direction { NORTH, SOUTH, EAST, WEST }

fun move(dir: Direction): String = when (dir) {
    Direction.NORTH -> "up"
    Direction.SOUTH -> "down"
    Direction.EAST  -> "right"
    Direction.WEST  -> "left"
    // No else needed — all enum values covered = exhaustive
}
```

---

## 20.5 `when` in DSA — Common Patterns

### State Machine / Direction Mapping

```kotlin
val dirs = when (command) {
    'U' -> Pair(-1, 0)
    'D' -> Pair(1, 0)
    'L' -> Pair(0, -1)
    'R' -> Pair(0, 1)
    else -> Pair(0, 0)
}

// Or with type when (any)
val dr: Int
val dc: Int
when (command) {
    'U' -> { dr = -1; dc = 0  }
    'D' -> { dr =  1; dc = 0  }
    'L' -> { dr =  0; dc = -1 }
    'R' -> { dr =  0; dc =  1 }
    else -> { dr = 0; dc = 0  }
}
```

### Bracket Matching

```kotlin
val match = when (ch) {
    ')' -> '('
    '}' -> '{'
    ']' -> '['
    else -> null    // not a closing bracket
}
```

### Categorical Logic

```kotlin
val type = when {
    n < 0       -> "negative"
    n == 0      -> "zero"
    n % 2 == 0  -> "positive even"
    else        -> "positive odd"
}
```

---

## `when` Quick Reference

|Feature|Syntax|
|---|---|
|Single value|`x -> action`|
|Multiple values|`x, y -> action`|
|Range|`in 1..10 -> action`|
|Not in range|`!in 1..10 -> action`|
|Type check|`is String -> action`|
|Null check|`null -> action`|
|No argument|`when { condition -> action }`|
|Default|`else -> action`|
|Expression|`val r = when (x) { ... else -> ... }`|
|Statement|`when (x) { ... }` — else optional|
|Multi-line branch|`x -> { ... lastExpr }`|

---

## CHAPTER SUMMARY (Ch 16–20)

### Bit Operations — When to Use

|Goal|Expression|
|---|---|
|Check even|`n and 1 == 0`|
|Check power of 2|`n > 0 && (n and (n-1)) == 0`|
|Set bit i|`n or (1 shl i)`|
|Clear bit i|`n and (1 shl i).inv()`|
|Toggle bit i|`n xor (1 shl i)`|
|Check bit i|`(n shr i) and 1 == 1`|
|Count set bits|`Integer.bitCount(n)`|
|Safe midpoint|`left + ((right - left) shr 1)`|
|XOR cancel pairs|`fold(0) { acc, x -> acc xor x }`|

### Lambda & Collection — Key Rules

|Rule|Detail|
|---|---|
|`map` count|output size = input size always|
|`flatMap`|map then flatten — output may differ in size|
|`filter`|output size ≤ input size|
|`fold` vs `reduce`|fold has initial + works on empty; reduce does not|
|`scan`|like fold but keeps all intermediates|
|`partition`|splits into exactly two lists|
|`groupBy`|returns `Map<K, List<T>>`|
|`eachCount()`|shortcut for frequency map|

### when — Key Rules

|Rule|Detail|
|---|---|
|As expression|`else` is **required** (unless exhaustive)|
|As statement|`else` is optional|
|Smart cast|`is Type` branch auto-casts inside branch|
|No argument|used as if-else chain|
|Multi-value|`1, 2, 3 ->`|
|Range|`in lo..hi ->`|
|Null|`null ->` branch|

---

_End of Chapters 16–20_ _Next: Ch 21 Sorted Collections (TreeMap/TreeSet) · Ch 22 Looping Cheat Sheet · Ch 23 Algorithm Templates · Ch 24 Graph Templates · Ch 25 Tree Templates_


# Kotlin DSA Reference Guide

### Chapters 21–25 | Pure API & Syntax Reference | No Problem Solutions

---

## MASTER INDEX (This File)

|#|Chapter|
|---|---|
|21|[Sorted Collections — TreeMap & TreeSet](#21-sorted-collections--treemap--treeset)|
|22|[Looping Cheat Sheet](#22-looping-cheat-sheet)|
|23|[Algorithm Templates](#23-algorithm-templates)|
|24|[Graph Templates](#24-graph-templates)|
|25|[Tree Templates](#25-tree-templates)|

---

# 21. Sorted Collections — TreeMap & TreeSet

```kotlin
import java.util.TreeMap
import java.util.TreeSet
```

> `TreeMap` and `TreeSet` use a **red-black tree** internally. Every insertion, deletion, and lookup is **O(log n)**. The key feature: keys/elements are **always in sorted order**. Use when you need sorted order AND range queries — not just lookup.

---

## 21.1 TreeMap

### Creation

```kotlin
// Natural ordering (ascending by key)
val tmap = TreeMap<Int, String>()
val tmap = TreeMap<String, Int>()

// Custom key ordering
val tmap = TreeMap<Int, String>(reverseOrder())          // descending keys
val tmap = TreeMap<String, Int>(compareBy { it.length }) // sort by length
val tmap = TreeMap<Int, String> { a, b -> b - a }        // descending lambda

// From existing map
val tmap = TreeMap<Int, String>(existingMap)             // copies and sorts

// Pre-filled
val tmap = TreeMap<Int, String>().apply {
    put(3, "three")
    put(1, "one")
    put(2, "two")
}
// Iteration order: 1→"one", 2→"two", 3→"three"
```

### Basic Operations (Same as HashMap)

```kotlin
val tmap = TreeMap<Int, String>()

// Write
tmap[1] = "one"
tmap.put(2, "two")
tmap.putIfAbsent(3, "three")
tmap.remove(2)
tmap.clear()

// Read
tmap[1]                          // "one" or null
tmap.getOrDefault(99, "missing") // "missing" if key absent
tmap.containsKey(1)              // true
1 in tmap                        // same
tmap.containsValue("one")        // true — O(n)
tmap.size
tmap.isEmpty()
```

### TreeMap-Exclusive Navigation Methods

> These are the reason to use TreeMap over HashMap.

|Method|Returns|What it does|
|---|---|---|
|`tmap.firstKey()`|`K`|smallest key — throws if empty|
|`tmap.lastKey()`|`K`|largest key — throws if empty|
|`tmap.firstEntry()`|`Map.Entry<K,V>?`|entry with smallest key|
|`tmap.lastEntry()`|`Map.Entry<K,V>?`|entry with largest key|
|`tmap.floorKey(k)`|`K?`|largest key **≤** k — null if none|
|`tmap.ceilingKey(k)`|`K?`|smallest key **≥** k — null if none|
|`tmap.lowerKey(k)`|`K?`|largest key **<** k (strictly) — null if none|
|`tmap.higherKey(k)`|`K?`|smallest key **>** k (strictly) — null if none|
|`tmap.floorEntry(k)`|`Map.Entry<K,V>?`|entry with largest key ≤ k|
|`tmap.ceilingEntry(k)`|`Map.Entry<K,V>?`|entry with smallest key ≥ k|
|`tmap.lowerEntry(k)`|`Map.Entry<K,V>?`|entry with largest key < k|
|`tmap.higherEntry(k)`|`Map.Entry<K,V>?`|entry with smallest key > k|
|`tmap.pollFirstEntry()`|`Map.Entry<K,V>?`|**remove** + return smallest entry|
|`tmap.pollLastEntry()`|`Map.Entry<K,V>?`|**remove** + return largest entry|

```kotlin
val tmap = TreeMap<Int, String>()
tmap[1] = "a"; tmap[3] = "c"; tmap[5] = "e"; tmap[7] = "g"

tmap.firstKey()      // 1
tmap.lastKey()       // 7
tmap.floorKey(4)     // 3  — largest key ≤ 4
tmap.ceilingKey(4)   // 5  — smallest key ≥ 4
tmap.lowerKey(3)     // 1  — largest key < 3
tmap.higherKey(3)    // 5  — smallest key > 3
tmap.floorKey(0)     // null — no key ≤ 0
tmap.ceilingKey(8)   // null — no key ≥ 8
```

### Range Queries (SubMaps)

|Method|Returns|What it does|
|---|---|---|
|`tmap.headMap(toKey)`|`SortedMap<K,V>`|keys **<** toKey (exclusive)|
|`tmap.headMap(toKey, inclusive)`|`NavigableMap<K,V>`|keys ≤ toKey if inclusive=true|
|`tmap.tailMap(fromKey)`|`SortedMap<K,V>`|keys **≥** fromKey (inclusive)|
|`tmap.tailMap(fromKey, inclusive)`|`NavigableMap<K,V>`|keys > fromKey if inclusive=false|
|`tmap.subMap(from, to)`|`SortedMap<K,V>`|keys: from ≤ k < to|
|`tmap.subMap(from, fromInc, to, toInc)`|`NavigableMap<K,V>`|full control of inclusivity|
|`tmap.descendingMap()`|`NavigableMap<K,V>`|reversed view of map|
|`tmap.descendingKeySet()`|`NavigableSet<K>`|keys in descending order|

```kotlin
val tmap = TreeMap<Int, String>()
(1..10).forEach { tmap[it] = "v$it" }

tmap.headMap(5)          // keys 1,2,3,4     (< 5, exclusive)
tmap.headMap(5, true)    // keys 1,2,3,4,5   (≤ 5, inclusive)
tmap.tailMap(7)          // keys 7,8,9,10    (≥ 7, inclusive)
tmap.tailMap(7, false)   // keys 8,9,10      (> 7, exclusive)
tmap.subMap(3, 7)        // keys 3,4,5,6     (3 ≤ k < 7)
tmap.subMap(3, true, 7, true)   // keys 3,4,5,6,7  (3 ≤ k ≤ 7)

// ⚠️ SubMaps are VIEWS — changes to them affect the original TreeMap
// ✅ To get an independent copy: TreeMap(tmap.subMap(3, 7))
```

### TreeMap Iteration

```kotlin
val tmap = TreeMap<Int, String>()
tmap[3] = "c"; tmap[1] = "a"; tmap[2] = "b"

// Ascending (default) — always in sorted key order
for ((k, v) in tmap) {
    println("$k → $v")   // 1→a, 2→b, 3→c
}

// Descending
for ((k, v) in tmap.descendingMap()) {
    println("$k → $v")   // 3→c, 2→b, 1→a
}

// Keys only
for (k in tmap.keys) { }            // ascending
for (k in tmap.descendingKeySet()) { } // descending

// Values only (ascending key order)
for (v in tmap.values) { }
```

---

## 21.2 TreeSet

### Creation

```kotlin
// Natural ordering (ascending)
val tset = TreeSet<Int>()
val tset = TreeSet<String>()

// Custom ordering
val tset = TreeSet<Int>(reverseOrder())            // descending
val tset = TreeSet<String>(compareBy { it.length }) // by length
val tset = TreeSet<Int> { a, b -> b - a }           // descending lambda

// From collection
val tset = TreeSet<Int>(listOf(3, 1, 4, 1, 5))     // {1, 3, 4, 5} — sorted, no dups
val tset = sortedSetOf(3, 1, 4, 1, 5)               // shorthand — {1, 3, 4, 5}
```

### Basic Operations

```kotlin
val tset = TreeSet<Int>()

tset.add(5); tset.add(1); tset.add(3)    // {1, 3, 5}
tset.remove(3)          // {1, 5}
tset.contains(5)        // true
5 in tset               // same
tset.size               // 2
tset.isEmpty()
tset.clear()
```

### TreeSet-Exclusive Navigation Methods

|Method|Returns|What it does|
|---|---|---|
|`tset.first()`|`T`|smallest element — throws if empty|
|`tset.last()`|`T`|largest element — throws if empty|
|`tset.floor(e)`|`T?`|largest element **≤** e — null if none|
|`tset.ceiling(e)`|`T?`|smallest element **≥** e — null if none|
|`tset.lower(e)`|`T?`|largest element **<** e (strictly) — null if none|
|`tset.higher(e)`|`T?`|smallest element **>** e (strictly) — null if none|
|`tset.pollFirst()`|`T?`|**remove** + return smallest — null if empty|
|`tset.pollLast()`|`T?`|**remove** + return largest — null if empty|

```kotlin
val tset = TreeSet<Int>()
listOf(1, 3, 5, 7, 9).forEach { tset.add(it) }   // {1,3,5,7,9}

tset.first()      // 1
tset.last()       // 9
tset.floor(4)     // 3  — largest ≤ 4
tset.ceiling(4)   // 5  — smallest ≥ 4
tset.lower(5)     // 3  — largest < 5
tset.higher(5)    // 7  — smallest > 5
tset.floor(0)     // null — no element ≤ 0
tset.ceiling(10)  // null — no element ≥ 10

tset.pollFirst()  // 1 — removed, set becomes {3,5,7,9}
tset.pollLast()   // 9 — removed, set becomes {3,5,7}
```

### Range Queries (SubSets)

|Method|Returns|What it does|
|---|---|---|
|`tset.headSet(to)`|`SortedSet<T>`|elements **<** to (exclusive)|
|`tset.headSet(to, inclusive)`|`NavigableSet<T>`|elements ≤ to if inclusive=true|
|`tset.tailSet(from)`|`SortedSet<T>`|elements **≥** from (inclusive)|
|`tset.tailSet(from, inclusive)`|`NavigableSet<T>`|elements > from if inclusive=false|
|`tset.subSet(from, to)`|`SortedSet<T>`|elements: from ≤ e < to|
|`tset.subSet(from, fromInc, to, toInc)`|`NavigableSet<T>`|full inclusivity control|
|`tset.descendingSet()`|`NavigableSet<T>`|reversed view|

```kotlin
val tset = TreeSet<Int>()
(1..10).forEach { tset.add(it) }   // {1,2,3,4,5,6,7,8,9,10}

tset.headSet(5)           // {1,2,3,4}      (< 5, exclusive)
tset.headSet(5, true)     // {1,2,3,4,5}    (≤ 5, inclusive)
tset.tailSet(7)           // {7,8,9,10}     (≥ 7, inclusive)
tset.tailSet(7, false)    // {8,9,10}       (> 7, exclusive)
tset.subSet(3, 7)         // {3,4,5,6}      (3 ≤ e < 7)
tset.subSet(3, true, 7, true)  // {3,4,5,6,7}
tset.descendingSet()           // {10,9,8,...,1}
```

### TreeSet Iteration

```kotlin
val tset = TreeSet<Int>()
listOf(3, 1, 4, 1, 5, 9).forEach { tset.add(it) }   // {1,3,4,5,9}

// Ascending (default)
for (e in tset) { println(e) }        // 1,3,4,5,9

// Descending
for (e in tset.descendingSet()) { println(e) }   // 9,5,4,3,1
```

---

## 21.3 HashMap vs TreeMap vs LinkedHashMap

|Feature|`HashMap`|`TreeMap`|`LinkedHashMap`|
|---|---|---|---|
|Order|❌ none|✅ sorted by key|✅ insertion order|
|get/put|O(1) avg|O(log n)|O(1) avg|
|Navigation (floor/ceil)|❌|✅|❌|
|Range queries|❌|✅|❌|
|Use when|fast lookup|sorted + range|insertion order|

## HashSet vs TreeSet vs LinkedHashSet

|Feature|`HashSet`|`TreeSet`|`LinkedHashSet`|
|---|---|---|---|
|Order|❌ none|✅ sorted|✅ insertion order|
|contains|O(1) avg|O(log n)|O(1) avg|
|Navigation (floor/ceil)|❌|✅|❌|
|Range queries|❌|✅|❌|
|Use when|fast existence check|sorted + range|insertion order|

---

# 22. Looping Cheat Sheet

> Reference for every collection type — all loop forms in one place.

---

## 22.1 Array Loops (IntArray / Array<T>)

```kotlin
val arr = intArrayOf(10, 20, 30, 40, 50)

// ── Element only ──────────────────────────────
for (e in arr) { }

// ── Index only ────────────────────────────────
for (i in arr.indices) { }                      // 0, 1, 2, 3, 4
for (i in 0 until arr.size) { }                 // same
for (i in 0..arr.lastIndex) { }                 // same (inclusive lastIndex)

// ── Index + Element ───────────────────────────
for ((i, e) in arr.withIndex()) { }

// ── Reverse ───────────────────────────────────
for (i in arr.lastIndex downTo 0) { }           // lastIndex=4 → 4,3,2,1,0
for (e in arr.reversed()) { }                   // element, reverse order (new list)

// ── Step ──────────────────────────────────────
for (i in 0 until arr.size step 2) { }          // 0,2,4 — every other

// ── While style ───────────────────────────────
var i = 0
while (i < arr.size) { i++ }

// ── forEach (no break) ────────────────────────
arr.forEach { e -> }
arr.forEachIndexed { i, e -> }

// ── 2D Array ──────────────────────────────────
for (r in matrix.indices) {
    for (c in matrix[r].indices) {
        val cell = matrix[r][c]
    }
}
```

---

## 22.2 List Loops (MutableList / List)

```kotlin
val list = mutableListOf(10, 20, 30, 40, 50)

// ── Element only ──────────────────────────────
for (e in list) { }

// ── Index only ────────────────────────────────
for (i in list.indices) { }
for (i in 0 until list.size) { }

// ── Index + Element ───────────────────────────
for ((i, e) in list.withIndex()) { }

// ── Reverse ───────────────────────────────────
for (i in list.lastIndex downTo 0) { }
for (e in list.reversed()) { }                  // new reversed list

// ── Step ──────────────────────────────────────
for (i in 0 until list.size step 2) { }

// ── forEach (no break) ────────────────────────
list.forEach { e -> }
list.forEachIndexed { i, e -> }

// ── Break/Continue ────────────────────────────
for (e in list) {
    if (condition) break       // ✅ works in for
    if (condition) continue    // ✅ works in for
}
// ❌ forEach has no break — use for loop when you need break
```

---

## 22.3 Map Loops

```kotlin
val map = mutableMapOf("a" to 1, "b" to 2, "c" to 3)

// ── Key + Value (most common) ─────────────────
for ((key, value) in map) { }

// ── Keys only ─────────────────────────────────
for (key in map.keys) { }

// ── Values only ───────────────────────────────
for (value in map.values) { }

// ── Entry object ──────────────────────────────
for (entry in map.entries) {
    val k = entry.key
    val v = entry.value
}

// ── forEach ───────────────────────────────────
map.forEach { (key, value) -> }

// ── Sorted iteration ──────────────────────────
for ((k, v) in map.entries.sortedBy { it.key }) { }
for ((k, v) in map.entries.sortedByDescending { it.value }) { }

// ── TreeMap — always sorted ───────────────────
val tmap = TreeMap<Int, String>()
for ((k, v) in tmap) { }                        // ascending by key always
for ((k, v) in tmap.descendingMap()) { }        // descending

// ── With index counter ────────────────────────
for ((i, entry) in map.entries.withIndex()) {
    val (key, value) = entry
}
```

---

## 22.4 Set Loops

```kotlin
val set = mutableSetOf(30, 10, 20)      // HashSet — order not guaranteed
val tset = TreeSet<Int>().also { it.addAll(setOf(30,10,20)) }  // sorted

// ── Element only ──────────────────────────────
for (e in set) { }                       // order not guaranteed for HashSet

// ── forEach ───────────────────────────────────
set.forEach { e -> }

// ── Sorted iteration from HashSet ─────────────
for (e in set.sorted()) { }             // new sorted list, then iterate
for (e in set.sortedDescending()) { }

// ── TreeSet — always sorted ───────────────────
for (e in tset) { }                     // always ascending
for (e in tset.descendingSet()) { }     // always descending

// ── With index (position in iteration) ───────
for ((i, e) in set.withIndex()) { }     // i = iteration count (NOT stable index)

// ⚠️ No index access — sets have no [i] operator
// set[0] — compile error
```

---

## 22.5 Stack Loops (ArrayDeque as Stack)

```kotlin
val stack = ArrayDeque<Int>()
stack.addLast(1); stack.addLast(2); stack.addLast(3)   // top = 3

// ── Default for loop: BOTTOM → TOP ───────────
for (e in stack) { println(e) }        // 1, 2, 3 (bottom to top)

// ── TOP → BOTTOM ──────────────────────────────
for (e in stack.reversed()) { println(e) }   // 3, 2, 1

// ── Drain entire stack (destructive) ──────────
while (stack.isNotEmpty()) {
    val top = stack.removeLast()
    // process top (order: 3, 2, 1)
}

// ── Peek-only snapshot (non-destructive) ──────
val snapshot = stack.toList()          // [1, 2, 3] — copy, stack unchanged
for (e in snapshot.reversed()) { }    // process in stack order

// ⚠️ for loop does NOT remove elements — use removeLast() to drain
```

---

## 22.6 Queue Loops (ArrayDeque as Queue)

```kotlin
val queue = ArrayDeque<Int>()
queue.addLast(10); queue.addLast(20); queue.addLast(30)  // front = 10

// ── Default for loop: FRONT → BACK ───────────
for (e in queue) { println(e) }        // 10, 20, 30 (front to back)

// ── Drain entire queue (destructive — FIFO) ──
while (queue.isNotEmpty()) {
    val front = queue.removeFirst()
    // process front (order: 10, 20, 30)
}

// ── BFS Level-by-level ────────────────────────
while (queue.isNotEmpty()) {
    val levelSize = queue.size         // snapshot before expanding
    repeat(levelSize) {
        val node = queue.removeFirst()
        // process node
        // queue.addLast(children...)
    }
    // entire level processed
}

// ── Peek-only snapshot ────────────────────────
val snapshot = queue.toList()          // copy, queue unchanged
```

---

## 22.7 PriorityQueue Loops

```kotlin
val heap = PriorityQueue<Int>()
heap.offer(5); heap.offer(1); heap.offer(3)

// ── for loop: HEAP ORDER (not sorted) ─────────
for (e in heap) { println(e) }         // ⚠️ NOT sorted order — heap-internal only

// ── Drain in sorted order (destructive) ───────
while (heap.isNotEmpty()) {
    val top = heap.poll()!!
    // processes in sorted order: 1, 3, 5 (min-heap)
}

// ── Copy then drain (non-destructive) ─────────
val copy = PriorityQueue(heap)
while (copy.isNotEmpty()) {
    val top = copy.poll()!!
    // sorted order, original heap unchanged
}
```

---

## 22.8 String Loops

```kotlin
val s = "hello"

// ── Char only ─────────────────────────────────
for (ch in s) { }

// ── Index only ────────────────────────────────
for (i in s.indices) { }               // 0 until s.length
for (i in 0 until s.length) { }        // same

// ── Index + Char ──────────────────────────────
for ((i, ch) in s.withIndex()) { }

// ── Reverse ───────────────────────────────────
for (i in s.lastIndex downTo 0) {
    val ch = s[i]
}

// ── forEach ───────────────────────────────────
s.forEach { ch -> }
s.forEachIndexed { i, ch -> }

// ── As CharArray (mutable) ────────────────────
val chars = s.toCharArray()
for (i in chars.indices) {
    chars[i] = chars[i].uppercaseChar()
}
val result = String(chars)
```

---

## 22.9 Range Loops

```kotlin
// ── Ascending ─────────────────────────────────
for (i in 1..5) { }               // 1,2,3,4,5
for (i in 0 until n) { }          // 0,1,...,n-1
for (i in 0..n-1) { }             // same (prefer until)

// ── Descending ────────────────────────────────
for (i in 5 downTo 1) { }         // 5,4,3,2,1
for (i in n-1 downTo 0) { }       // reverse index

// ── Stepped ───────────────────────────────────
for (i in 0..10 step 2) { }       // 0,2,4,6,8,10
for (i in 10 downTo 0 step 3) { } // 10,7,4,1

// ── Char range ────────────────────────────────
for (ch in 'a'..'z') { }
for (ch in 'A'..'Z') { }
```

---

## 22.10 Loop Trap Summary

|Trap|Wrong|Correct|
|---|---|---|
|`forEach` break|no break in `forEach`|use `for` loop|
|`forEach` return|`return@forEach` = continue|use `for` + `return` for early exit|
|Stack direction|`for (e in stack)` = bottom→top|`stack.reversed()` for top→bottom|
|Heap `for` loop|`for (e in heap)` = NOT sorted|drain with `poll()`|
|`toList()` heap|NOT sorted output|drain with `poll()`|
|`sorted()` array|returns `List`, not `IntArray`|`.sorted().toIntArray()`|
|Map concurrent modify|modify map while iterating|collect keys first, then modify|
|Set no index|`set[0]` = compile error|use `elementAt(0)` O(n) or toList|
|`subList` is a view|modifying subList modifies original|copy if needed|

---

# 23. Algorithm Templates

> Pure structural templates — skeleton code only. Fill in the logic for the specific problem you are solving.

---

## 23.1 Binary Search

### Standard — Search for Exact Value

```kotlin
fun binarySearch(arr: IntArray, target: Int): Int {
    var left = 0
    var right = arr.size - 1                      // inclusive right bound

    while (left <= right) {                        // ← <= not <
        val mid = left + (right - left) / 2        // ← avoids overflow vs (l+r)/2

        when {
            arr[mid] == target -> return mid        // found
            arr[mid] < target  -> left = mid + 1   // target in right half
            else               -> right = mid - 1  // target in left half
        }
    }

    return -1                                      // not found
}
```

### Find First True (Left Boundary)

> Array is `[false, false, ..., true, true, true]` — find first `true` index.

```kotlin
fun findFirstTrue(arr: IntArray): Int {
    var left = 0
    var right = arr.size - 1
    var result = -1                               // default if no true found

    while (left <= right) {
        val mid = left + (right - left) / 2

        if (condition(arr[mid])) {                // condition is "true" side
            result = mid                          // record potential answer
            right = mid - 1                       // try to find earlier true
        } else {
            left = mid + 1                        // too early — move right
        }
    }

    return result
}
```

### Find Last True (Right Boundary)

> Array is `[true, true, ..., true, false, false]` — find last `true` index.

```kotlin
fun findLastTrue(arr: IntArray): Int {
    var left = 0
    var right = arr.size - 1
    var result = -1

    while (left <= right) {
        val mid = left + (right - left) / 2

        if (condition(arr[mid])) {                // condition is "true" side
            result = mid                          // record potential answer
            left = mid + 1                        // try to find later true
        } else {
            right = mid - 1                       // too late — move left
        }
    }

    return result
}
```

### Binary Search on Answer Space

> When the answer itself is the search space (not an array).

```kotlin
fun binarySearchAnswer(): Int {
    var left = minPossibleAnswer
    var right = maxPossibleAnswer
    var result = right                            // or -1, depending on problem

    while (left <= right) {
        val mid = left + (right - left) / 2

        if (canAchieve(mid)) {                    // predicate on candidate answer
            result = mid                          // valid answer — try better
            right = mid - 1                       // minimize (or left = mid+1 to maximize)
        } else {
            left = mid + 1                        // can't achieve — need more
        }
    }

    return result
}
```

### Built-In Binary Search

```kotlin
val arr = intArrayOf(1, 3, 5, 7, 9)

val idx = arr.binarySearch(5)       // returns index 2 if found
                                    // returns negative value if not found
                                    // -(insertionPoint) - 1

// List version
val list = listOf(1, 3, 5, 7, 9)
val idx2 = list.binarySearch(5)     // same contract

// ⚠️ Array must be sorted before using binarySearch
// ⚠️ Negative return means not found — do NOT use as index directly
if (idx >= 0) { /* found at arr[idx] */ }
```

---

## 23.2 Two Pointers

### Opposite Ends — Converging

```kotlin
fun twoPointers(arr: IntArray): ReturnType {
    var left = 0
    var right = arr.size - 1

    while (left < right) {                   // ← strict < (stops when they meet)
        // use arr[left] and arr[right]

        when {
            /* condition A */ -> left++
            /* condition B */ -> right--
            else              -> {
                left++
                right--
            }
        }
    }
}
```

### Same Direction — Slow/Fast Pointers

```kotlin
fun slowFast(head: ListNode?): ReturnType {
    var slow = head
    var fast = head

    while (fast != null && fast.next != null) {
        slow = slow?.next              // moves 1 step
        fast = fast.next?.next         // moves 2 steps
    }

    // When fast reaches end: slow is at middle
    return slow
}
```

### Two Pointers on Two Separate Arrays

```kotlin
fun mergeTwoSorted(a: IntArray, b: IntArray): IntArray {
    val result = IntArray(a.size + b.size)
    var i = 0; var j = 0; var k = 0

    while (i < a.size && j < b.size) {
        if (a[i] <= b[j]) result[k++] = a[i++]
        else               result[k++] = b[j++]
    }

    while (i < a.size) result[k++] = a[i++]
    while (j < b.size) result[k++] = b[j++]

    return result
}
```

---

## 23.3 Sliding Window

### Fixed-Size Window

```kotlin
fun fixedWindow(arr: IntArray, k: Int): Int {
    var windowVal = 0
    var result = 0

    // Build first window
    for (i in 0 until k) {
        windowVal += arr[i]            // or whatever the window state is
    }
    result = windowVal

    // Slide window
    for (i in k until arr.size) {
        windowVal += arr[i]            // add incoming element (right side)
        windowVal -= arr[i - k]        // remove outgoing element (left side)
        result = maxOf(result, windowVal)
    }

    return result
}
```

### Variable-Size Window (Expand/Shrink)

```kotlin
fun variableWindow(arr: IntArray): Int {
    var left = 0
    var result = 0
    var windowState = 0               // whatever tracks window validity

    for (right in arr.indices) {
        // ── Expand: include arr[right] in window ──
        windowState += arr[right]     // update state with new element

        // ── Shrink: while window is invalid ───────
        while (/* window invalid */) {
            windowState -= arr[left]  // remove left element from state
            left++
        }

        // ── Update result with valid window ────────
        result = maxOf(result, right - left + 1)
    }

    return result
}
```

### Variable Window with HashMap State

```kotlin
fun windowWithMap(s: String): Int {
    val freq = mutableMapOf<Char, Int>()
    var left = 0
    var result = 0

    for (right in s.indices) {
        // Add right character
        freq[s[right]] = (freq[s[right]] ?: 0) + 1

        // Shrink while invalid
        while (/* window invalid — e.g. freq.size > k */) {
            freq[s[left]] = (freq[s[left]] ?: 0) - 1
            if (freq[s[left]] == 0) freq.remove(s[left])
            left++
        }

        result = maxOf(result, right - left + 1)
    }

    return result
}
```

---

## 23.4 Prefix Sum

### 1D Prefix Sum

```kotlin
val nums = intArrayOf(1, 2, 3, 4, 5)
val n = nums.size
val prefix = IntArray(n + 1)          // prefix[0] = 0 (empty prefix)

// Build — O(n)
for (i in 0 until n) {
    prefix[i + 1] = prefix[i] + nums[i]
}
// prefix = [0, 1, 3, 6, 10, 15]

// Query: sum of nums[l..r] inclusive — O(1)
fun rangeSum(l: Int, r: Int): Int = prefix[r + 1] - prefix[l]

rangeSum(1, 3)   // nums[1]+nums[2]+nums[3] = 2+3+4 = 9
rangeSum(0, 4)   // 1+2+3+4+5 = 15
```

### 2D Prefix Sum

```kotlin
val grid = Array(rows) { IntArray(cols) }   // original grid
val pre = Array(rows + 1) { IntArray(cols + 1) }

// Build
for (r in 1..rows) {
    for (c in 1..cols) {
        pre[r][c] = grid[r-1][c-1] +
                    pre[r-1][c] +
                    pre[r][c-1] -
                    pre[r-1][c-1]
    }
}

// Query: sum of rectangle (r1,c1) to (r2,c2) inclusive — O(1)
// Note: grid uses 0-indexed (r1,c1)-(r2,c2); pre uses 1-indexed internally
fun rectSum(r1: Int, c1: Int, r2: Int, c2: Int): Int {
    return pre[r2+1][c2+1] -
           pre[r1][c2+1] -
           pre[r2+1][c1] +
           pre[r1][c1]
}
```

### Difference Array (Range Updates)

```kotlin
val diff = IntArray(n + 1)

// Add value v to all elements in range [l, r] inclusive — O(1) per update
fun rangeAdd(l: Int, r: Int, v: Int) {
    diff[l] += v
    diff[r + 1] -= v
}

// Reconstruct final array after all updates — O(n)
fun build(): IntArray {
    val result = IntArray(n)
    var running = 0
    for (i in 0 until n) {
        running += diff[i]
        result[i] = running
    }
    return result
}
```

---

## 23.5 Backtracking

### Core Template

```kotlin
val result = mutableListOf<List<Int>>()

fun backtrack(
    path: MutableList<Int>,       // current choices made
    /* other state parameters */
) {
    // ── Base case: solution is complete ───────
    if (/* done condition */) {
        result.add(path.toList()) // ← copy before adding (path is mutable)
        return
    }

    // ── Try each choice ───────────────────────
    for (choice in /* available choices */) {
        // ── Pruning: skip invalid choices ─────
        if (/* invalid */) continue

        // ── Make choice ───────────────────────
        path.add(choice)

        // ── Recurse ───────────────────────────
        backtrack(path)

        // ── Undo choice (backtrack) ───────────
        path.removeLast()
    }
}

// Entry point
backtrack(mutableListOf())
```

### With Used/Visited Array

```kotlin
val used = BooleanArray(nums.size)

fun backtrack(path: MutableList<Int>) {
    if (path.size == nums.size) {
        result.add(path.toList())
        return
    }

    for (i in nums.indices) {
        if (used[i]) continue                     // skip already used

        used[i] = true
        path.add(nums[i])

        backtrack(path)

        path.removeLast()
        used[i] = false
    }
}
```

### With Start Index (Combinations / No Reuse)

```kotlin
fun backtrack(path: MutableList<Int>, start: Int) {
    if (path.size == k) {                         // or another done condition
        result.add(path.toList())
        return
    }

    for (i in start until nums.size) {            // ← start from `start` not 0
        if (/* pruning condition */) continue

        path.add(nums[i])
        backtrack(path, i + 1)                    // i+1 = no reuse; i = can reuse
        path.removeLast()
    }
}
```

### With String Building

```kotlin
fun backtrack(sb: StringBuilder, /* state */) {
    if (/* done */) {
        result.add(sb.toString())
        return
    }

    for (ch in /* choices */) {
        sb.append(ch)                             // make choice
        backtrack(sb)                             // recurse
        sb.deleteCharAt(sb.length - 1)            // undo choice
    }
}
```

---

## 23.6 Greedy Pattern

> Greedy: at each step, make the locally optimal choice. Structure varies — but these are the common skeleton forms.

### Sort Then Iterate

```kotlin
// Sort by some criterion, then greedily process in order
intervals.sortWith { a, b -> Integer.compare(a[0], b[0]) }  // sort by start

var end = intervals[0][1]
var count = 0

for (i in 1 until intervals.size) {
    if (intervals[i][0] >= end) {      // no overlap — greedy select
        end = intervals[i][1]
        count++
    }
    // otherwise: skip (greedy discard)
}
```

### Priority Queue Greedy

```kotlin
// Use min/max heap to always pick the best available option
val heap = PriorityQueue<Int>()                  // or custom comparator

for (item in items) {
    heap.offer(item)                             // add candidate

    if (heap.size > k) heap.poll()              // discard worst
}

// heap now contains k best items
```

### Two Pointer Greedy

```kotlin
// Match from both ends greedily
var left = 0; var right = s.length - 1

while (left < right) {
    if (/* left is fine */) {
        left++
    } else if (/* right is fine */) {
        right--
    } else {
        // must use one resource (swap, replace, etc.)
        left++; right--
    }
}
```

---

# 24. Graph Templates

## 24.1 Graph Representation

### Adjacency List — Standard

```kotlin
val n = 5   // number of nodes (0-indexed)

// Directed graph
val graph = Array(n) { mutableListOf<Int>() }

// Add directed edge u → v
fun addEdge(u: Int, v: Int) {
    graph[u].add(v)
}

// Undirected graph — add both directions
fun addEdgeUndirected(u: Int, v: Int) {
    graph[u].add(v)
    graph[v].add(u)
}
```

### Weighted Adjacency List

```kotlin
// Pair<neighbor, weight>
val graph = Array(n) { mutableListOf<Pair<Int, Int>>() }

fun addEdge(u: Int, v: Int, w: Int) {
    graph[u].add(Pair(v, w))
    graph[v].add(Pair(u, w))   // undirected
}

// Or use IntArray for [neighbor, weight]
val graph = Array(n) { mutableListOf<IntArray>() }
fun addEdge(u: Int, v: Int, w: Int) {
    graph[u].add(intArrayOf(v, w))
    graph[v].add(intArrayOf(u, w))
}
```

### Build from Edge List Input

```kotlin
// Input: edges = [[0,1],[1,2],[2,3]]
val graph = Array(n) { mutableListOf<Int>() }

for (edge in edges) {
    val (u, v) = edge
    graph[u].add(v)
    graph[v].add(u)    // remove for directed
}
```

### Adjacency Matrix

```kotlin
val adj = Array(n) { BooleanArray(n) }

fun addEdge(u: Int, v: Int) {
    adj[u][v] = true
    adj[v][u] = true   // remove for directed
}

// Check edge existence — O(1)
adj[u][v]

// Weighted matrix
val adjW = Array(n) { IntArray(n) { Int.MAX_VALUE } }  // infinity default
adjW[u][v] = weight
```

### Graph from HashMap (Sparse / String nodes)

```kotlin
val graph = mutableMapOf<String, MutableList<String>>()

fun addEdge(u: String, v: String) {
    graph.getOrPut(u) { mutableListOf() }.add(v)
    graph.getOrPut(v) { mutableListOf() }.add(u)
}
```

---

## 24.2 BFS

### Standard BFS — Shortest Distance

```kotlin
fun bfs(graph: Array<MutableList<Int>>, start: Int): IntArray {
    val n = graph.size
    val dist = IntArray(n) { -1 }              // -1 = unvisited
    val queue = ArrayDeque<Int>()

    dist[start] = 0
    queue.addLast(start)

    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()

        for (neighbor in graph[node]) {
            if (dist[neighbor] == -1) {        // not visited
                dist[neighbor] = dist[node] + 1
                queue.addLast(neighbor)
            }
        }
    }

    return dist                                // dist[i] = shortest hops from start to i
}
```

### BFS with HashSet Visited (String/Pair nodes)

```kotlin
val visited = HashSet<Int>()
val queue = ArrayDeque<Int>()

visited.add(start)
queue.addLast(start)

while (queue.isNotEmpty()) {
    val node = queue.removeFirst()

    for (neighbor in graph[node]) {
        if (neighbor !in visited) {
            visited.add(neighbor)
            queue.addLast(neighbor)
        }
    }
}
```

### BFS Multi-Source (Start from Multiple Nodes)

```kotlin
val dist = IntArray(n) { -1 }
val queue = ArrayDeque<Int>()

// Add all sources at once
for (source in sources) {
    dist[source] = 0
    queue.addLast(source)
}

while (queue.isNotEmpty()) {
    val node = queue.removeFirst()
    for (neighbor in graph[node]) {
        if (dist[neighbor] == -1) {
            dist[neighbor] = dist[node] + 1
            queue.addLast(neighbor)
        }
    }
}
```

### BFS Level-by-Level

```kotlin
var level = 0
val queue = ArrayDeque<Int>()
val visited = BooleanArray(n)

queue.addLast(start)
visited[start] = true

while (queue.isNotEmpty()) {
    val levelSize = queue.size              // snapshot BEFORE processing

    repeat(levelSize) {
        val node = queue.removeFirst()
        // process node at current `level`

        for (neighbor in graph[node]) {
            if (!visited[neighbor]) {
                visited[neighbor] = true
                queue.addLast(neighbor)
            }
        }
    }

    level++
}
```

---

## 24.3 DFS

### Recursive DFS

```kotlin
val visited = BooleanArray(n)

fun dfs(node: Int) {
    visited[node] = true

    // process node here (pre-order)

    for (neighbor in graph[node]) {
        if (!visited[neighbor]) {
            dfs(neighbor)
        }
    }

    // process node here (post-order)
}

// Call
dfs(startNode)
```

### Iterative DFS (Stack-Based)

```kotlin
val visited = BooleanArray(n)
val stack = ArrayDeque<Int>()

stack.addLast(startNode)

while (stack.isNotEmpty()) {
    val node = stack.removeLast()

    if (visited[node]) continue
    visited[node] = true

    // process node

    for (neighbor in graph[node]) {
        if (!visited[neighbor]) {
            stack.addLast(neighbor)
        }
    }
}
```

### DFS Returning Value (e.g. component size)

```kotlin
fun dfs(node: Int, visited: BooleanArray): Int {
    visited[node] = true
    var size = 1                              // count this node

    for (neighbor in graph[node]) {
        if (!visited[neighbor]) {
            size += dfs(neighbor, visited)    // accumulate from subtree
        }
    }

    return size
}
```

### DFS on All Components (Disconnected Graph)

```kotlin
val visited = BooleanArray(n)
var components = 0

for (node in 0 until n) {
    if (!visited[node]) {
        dfs(node, visited)                    // explore entire component
        components++
    }
}
```

---

## 24.4 Grid BFS

### Grid BFS — 4-Directional

```kotlin
fun gridBfs(grid: Array<IntArray>, startR: Int, startC: Int) {
    val rows = grid.size
    val cols = grid[0].size
    val dirs = arrayOf(
        intArrayOf(0, 1),    // right
        intArrayOf(0, -1),   // left
        intArrayOf(1, 0),    // down
        intArrayOf(-1, 0)    // up
    )

    val visited = Array(rows) { BooleanArray(cols) }
    val queue = ArrayDeque<IntArray>()          // [row, col]

    visited[startR][startC] = true
    queue.addLast(intArrayOf(startR, startC))

    while (queue.isNotEmpty()) {
        val (r, c) = queue.removeFirst()

        // process grid[r][c]

        for (d in dirs) {
            val nr = r + d[0]
            val nc = c + d[1]

            // Bounds check + visited check + optional condition
            if (nr in 0 until rows &&
                nc in 0 until cols &&
                !visited[nr][nc]  &&
                /* grid[nr][nc] == target */) {

                visited[nr][nc] = true
                queue.addLast(intArrayOf(nr, nc))
            }
        }
    }
}
```

### Grid BFS — State Includes Extra Info

```kotlin
data class State(val r: Int, val c: Int, val steps: Int)

val queue = ArrayDeque<State>()
queue.addLast(State(startR, startC, 0))

while (queue.isNotEmpty()) {
    val (r, c, steps) = queue.removeFirst()

    if (r == targetR && c == targetC) return steps   // found

    for (d in dirs) {
        val nr = r + d[0]; val nc = c + d[1]
        if (nr in 0 until rows && nc in 0 until cols && !visited[nr][nc]) {
            visited[nr][nc] = true
            queue.addLast(State(nr, nc, steps + 1))
        }
    }
}
```

---

## 24.5 Visited Patterns

### BooleanArray (index-based nodes)

```kotlin
val visited = BooleanArray(n)          // false by default
visited[node] = true
if (!visited[node]) { }
```

### HashSet (arbitrary node types)

```kotlin
val visited = HashSet<Int>()
val visited = HashSet<String>()
val visited = HashSet<Pair<Int,Int>>() // for (row,col) grid states

visited.add(node)
if (node !in visited) { }
```

### Distance Array as Visited (when distance needed anyway)

```kotlin
val dist = IntArray(n) { -1 }          // -1 = not visited
dist[start] = 0

if (dist[neighbor] == -1) {            // not visited check
    dist[neighbor] = dist[node] + 1
}
```

### Visited 2D Grid

```kotlin
val visited = Array(rows) { BooleanArray(cols) }
visited[r][c] = true
if (!visited[nr][nc]) { }
```

### Mark Grid In-Place (avoid extra space)

```kotlin
// Modify grid itself to mark visited — restore if needed
val original = grid[r][c]
grid[r][c] = '#'                       // mark as visited
// ... recurse ...
grid[r][c] = original                  // restore (if needed for backtracking)
```

---

# 25. Tree Templates

## 25.1 Node Definitions

```kotlin
// Standard LeetCode TreeNode
class TreeNode(var `val`: Int) {
    var left: TreeNode? = null
    var right: TreeNode? = null
}

// With constructor shorthand
class TreeNode(
    var `val`: Int,
    var left: TreeNode? = null,
    var right: TreeNode? = null
)

// Linked List node
class ListNode(var `val`: Int) {
    var next: ListNode? = null
}
```

> ⚠️ In Kotlin, `val` is a keyword — must use backticks: `node.\`val``

---

## 25.2 Binary Tree Traversal

### Preorder — Root → Left → Right

```kotlin
// Recursive
fun preorder(root: TreeNode?): List<Int> {
    if (root == null) return emptyList()
    val result = mutableListOf<Int>()

    fun dfs(node: TreeNode?) {
        if (node == null) return
        result.add(node.`val`)     // ← process BEFORE children (pre)
        dfs(node.left)
        dfs(node.right)
    }

    dfs(root)
    return result
}

// Iterative — Stack
fun preorderIterative(root: TreeNode?): List<Int> {
    val result = mutableListOf<Int>()
    if (root == null) return result

    val stack = ArrayDeque<TreeNode>()
    stack.addLast(root)

    while (stack.isNotEmpty()) {
        val node = stack.removeLast()
        result.add(node.`val`)             // process

        // Push right first — left will be processed first (LIFO)
        node.right?.let { stack.addLast(it) }
        node.left?.let { stack.addLast(it) }
    }

    return result
}
```

---

### Inorder — Left → Root → Right

```kotlin
// Recursive
fun inorder(root: TreeNode?): List<Int> {
    val result = mutableListOf<Int>()

    fun dfs(node: TreeNode?) {
        if (node == null) return
        dfs(node.left)
        result.add(node.`val`)     // ← process BETWEEN children (in)
        dfs(node.right)
    }

    dfs(root)
    return result
}

// Iterative — Stack
fun inorderIterative(root: TreeNode?): List<Int> {
    val result = mutableListOf<Int>()
    val stack = ArrayDeque<TreeNode>()
    var curr: TreeNode? = root

    while (curr != null || stack.isNotEmpty()) {
        // Go as left as possible
        while (curr != null) {
            stack.addLast(curr)
            curr = curr.left
        }

        // Process node
        curr = stack.removeLast()
        result.add(curr.`val`)

        // Move to right subtree
        curr = curr.right
    }

    return result
}
```

> ⭐ Inorder of a **BST** produces elements in **sorted ascending order**.

---

### Postorder — Left → Right → Root

```kotlin
// Recursive
fun postorder(root: TreeNode?): List<Int> {
    val result = mutableListOf<Int>()

    fun dfs(node: TreeNode?) {
        if (node == null) return
        dfs(node.left)
        dfs(node.right)
        result.add(node.`val`)     // ← process AFTER children (post)
    }

    dfs(root)
    return result
}

// Iterative — Two Stacks
fun postorderIterative(root: TreeNode?): List<Int> {
    val result = mutableListOf<Int>()
    if (root == null) return result

    val stack = ArrayDeque<TreeNode>()
    stack.addLast(root)

    while (stack.isNotEmpty()) {
        val node = stack.removeLast()
        result.add(node.`val`)              // add to result

        // Push left first — right will be processed first
        node.left?.let { stack.addLast(it) }
        node.right?.let { stack.addLast(it) }
    }

    return result.reversed()               // reverse gives postorder
}
```

---

### All Three — Comparison

|Traversal|Order|Push Order (Iterative)|Key Use|
|---|---|---|---|
|Preorder|Root → L → R|Push Right then Left|tree copy, serialization|
|Inorder|L → Root → R|Go left, then process|BST sorted order|
|Postorder|L → R → Root|result.reversed()|tree deletion, evaluate expr|

---

## 25.3 Level Order Traversal (BFS)

### Basic Level Order — All Nodes in One List

```kotlin
fun levelOrder(root: TreeNode?): List<Int> {
    if (root == null) return emptyList()

    val result = mutableListOf<Int>()
    val queue = ArrayDeque<TreeNode>()
    queue.addLast(root)

    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        result.add(node.`val`)

        node.left?.let { queue.addLast(it) }
        node.right?.let { queue.addLast(it) }
    }

    return result
}
```

### Level Order — Grouped by Level

```kotlin
fun levelOrderGrouped(root: TreeNode?): List<List<Int>> {
    if (root == null) return emptyList()

    val result = mutableListOf<List<Int>>()
    val queue = ArrayDeque<TreeNode>()
    queue.addLast(root)

    while (queue.isNotEmpty()) {
        val levelSize = queue.size              // ← snapshot size before loop
        val level = mutableListOf<Int>()

        repeat(levelSize) {                     // ← process exactly this level
            val node = queue.removeFirst()
            level.add(node.`val`)

            node.left?.let { queue.addLast(it) }
            node.right?.let { queue.addLast(it) }
        }

        result.add(level)
    }

    return result
}
```

### Level Order — Right Side View (Last Node Each Level)

```kotlin
fun rightSideView(root: TreeNode?): List<Int> {
    if (root == null) return emptyList()

    val result = mutableListOf<Int>()
    val queue = ArrayDeque<TreeNode>()
    queue.addLast(root)

    while (queue.isNotEmpty()) {
        val levelSize = queue.size

        repeat(levelSize) { i ->
            val node = queue.removeFirst()

            if (i == levelSize - 1) result.add(node.`val`)    // last in level

            node.left?.let { queue.addLast(it) }
            node.right?.let { queue.addLast(it) }
        }
    }

    return result
}
```

---

## 25.4 Common Tree DFS Patterns

### Return Value Up the Tree (Post-Order Pattern)

```kotlin
// Most tree problems follow this shape:
// 1. handle null base case
// 2. recurse left
// 3. recurse right
// 4. compute and return answer using left + right results

fun solve(root: TreeNode?): Int {
    if (root == null) return 0        // base case — return neutral value

    val left  = solve(root.left)      // get answer from left subtree
    val right = solve(root.right)     // get answer from right subtree

    // compute current node's contribution using left, right, root.`val`
    return /* combine left, right, root.`val` */
}
```

### Pass Value Down the Tree (Pre-Order Pattern)

```kotlin
// Pass accumulated state downward (e.g. path sum, depth, parent value)

fun solve(root: TreeNode?, accumulated: Int): ReturnType {
    if (root == null) return /* base */

    val newAccumulated = accumulated + root.`val`    // update going down

    // process current node with newAccumulated

    solve(root.left, newAccumulated)
    solve(root.right, newAccumulated)
}
```

### Global Variable Pattern (Track Max/Min Across Tree)

```kotlin
var globalMax = Int.MIN_VALUE

fun dfs(root: TreeNode?): Int {
    if (root == null) return 0

    val left  = maxOf(0, dfs(root.left))     // ignore negative paths
    val right = maxOf(0, dfs(root.right))

    // Update global answer at each node
    globalMax = maxOf(globalMax, left + right + root.`val`)

    // Return the best single path through this node (upward)
    return root.`val` + maxOf(left, right)
}
```

---

## 25.5 BST Operations

```kotlin
// BST Property: left.val < node.val < right.val

// Search
fun search(root: TreeNode?, target: Int): TreeNode? {
    if (root == null || root.`val` == target) return root
    return if (target < root.`val`) search(root.left, target)
           else                     search(root.right, target)
}

// Insert
fun insert(root: TreeNode?, `val`: Int): TreeNode {
    if (root == null) return TreeNode(`val`)
    if (`val` < root.`val`) root.left  = insert(root.left, `val`)
    else                    root.right = insert(root.right, `val`)
    return root
}

// Find minimum (leftmost node)
fun findMin(root: TreeNode): TreeNode {
    var curr = root
    while (curr.left != null) curr = curr.left!!
    return curr
}

// Validate BST
fun isValidBST(root: TreeNode?, min: Long = Long.MIN_VALUE, max: Long = Long.MAX_VALUE): Boolean {
    if (root == null) return true
    if (root.`val` <= min || root.`val` >= max) return false
    return isValidBST(root.left, min, root.`val`.toLong()) &&
           isValidBST(root.right, root.`val`.toLong(), max)
}
```

---

## 25.6 Tree Property Templates

### Height / Depth of Tree

```kotlin
fun height(root: TreeNode?): Int {
    if (root == null) return 0
    return 1 + maxOf(height(root.left), height(root.right))
}
```

### Count Nodes

```kotlin
fun countNodes(root: TreeNode?): Int {
    if (root == null) return 0
    return 1 + countNodes(root.left) + countNodes(root.right)
}
```

### Check if Balanced

```kotlin
fun isBalanced(root: TreeNode?): Boolean {
    fun checkHeight(node: TreeNode?): Int {
        if (node == null) return 0
        val left = checkHeight(node.left)
        if (left == -1) return -1                 // propagate failure
        val right = checkHeight(node.right)
        if (right == -1) return -1
        if (Math.abs(left - right) > 1) return -1 // unbalanced
        return 1 + maxOf(left, right)
    }
    return checkHeight(root) != -1
}
```

### Lowest Common Ancestor (LCA)

```kotlin
fun lca(root: TreeNode?, p: TreeNode?, q: TreeNode?): TreeNode? {
    if (root == null || root == p || root == q) return root

    val left  = lca(root.left, p, q)
    val right = lca(root.right, p, q)

    return if (left != null && right != null) root   // p and q in different subtrees
           else left ?: right                        // both in same subtree
}
```

---

## CHAPTER SUMMARY (Ch 21–25)

### TreeMap vs TreeSet — Navigation Methods

|Goal|TreeMap|TreeSet|
|---|---|---|
|Smallest|`firstKey()`|`first()`|
|Largest|`lastKey()`|`last()`|
|Largest ≤ k|`floorKey(k)`|`floor(k)`|
|Smallest ≥ k|`ceilingKey(k)`|`ceiling(k)`|
|Largest < k|`lowerKey(k)`|`lower(k)`|
|Smallest > k|`higherKey(k)`|`higher(k)`|
|Remove smallest|`pollFirstEntry()`|`pollFirst()`|
|Remove largest|`pollLastEntry()`|`pollLast()`|
|Keys < k|`headMap(k)`|`headSet(k)`|
|Keys ≥ k|`tailMap(k)`|`tailSet(k)`|
|Keys in range|`subMap(lo, hi)`|`subSet(lo, hi)`|

### Algorithm Template Skeleton Reminder

|Algorithm|Key Invariant|
|---|---|
|Binary Search|`left <= right`; `mid = left + (right-left)/2`|
|Two Pointers|`left < right`; move toward each other|
|Sliding Window Fixed|slide: `add arr[right]`, `remove arr[right-k]`|
|Sliding Window Variable|expand right, shrink left while invalid|
|Prefix Sum|`prefix[i+1] = prefix[i] + nums[i]`; query = `prefix[r+1] - prefix[l]`|
|Backtracking|make choice → recurse → undo choice|
|BFS|`visited` before enqueue; use `queue.size` for level-by-level|
|DFS|mark visited before recursing; post-order = compute after children return|

### Tree Traversal Order

```
Preorder:   ROOT → left → right   (top-down)
Inorder:    left → ROOT → right   (BST = sorted order)
Postorder:  left → right → ROOT   (bottom-up)
Level-order: width-first (BFS)
```

---

_End of Chapters 21–25_ _Next: Ch 26 Data Structure Templates (Trie, Union-Find, Segment Tree, Fenwick Tree)_

 # Kotlin DSA Reference Guide

### Chapters 26–30 | Final Section | Pure API & Syntax Reference | No Problem Solutions

---

## MASTER INDEX (This File)

|#|Chapter|
|---|---|
|26|[Data Structure Templates](#26-data-structure-templates)|
|27|[Performance Tips](#27-performance-tips)|
|28|[Common DSA Patterns](#28-common-dsa-patterns)|
|29|[Debugging Helpers](#29-debugging-helpers)|
|30|[Quick Decision Tables](#30-quick-decision-tables)|

---

# 26. Data Structure Templates

> These are complete, reusable class templates. Copy them directly into your solution file. No modification needed for standard use — only customise where marked.

---

## 26.1 Trie (Prefix Tree)

### What It Does

- Stores strings character by character
- `insert` — O(L) where L = string length
- `search` — O(L) exact match
- `startsWith` — O(L) prefix check
- Space — O(total characters across all strings)

### Standard Trie with HashMap Children

```kotlin
class TrieNode {
    val children = HashMap<Char, TrieNode>()
    var isEnd = false                              // marks end of a word
}

class Trie {
    private val root = TrieNode()

    // Insert a word — O(L)
    fun insert(word: String) {
        var curr = root
        for (ch in word) {
            curr = curr.children.getOrPut(ch) { TrieNode() }
        }
        curr.isEnd = true
    }

    // Search for exact word — O(L)
    fun search(word: String): Boolean {
        var curr = root
        for (ch in word) {
            curr = curr.children[ch] ?: return false
        }
        return curr.isEnd
    }

    // Check if any word starts with prefix — O(L)
    fun startsWith(prefix: String): Boolean {
        var curr = root
        for (ch in prefix) {
            curr = curr.children[ch] ?: return false
        }
        return true                               // reached end of prefix — at least one word exists
    }

    // Get node at end of prefix — O(L)
    // Returns null if prefix doesn't exist
    private fun getNode(prefix: String): TrieNode? {
        var curr = root
        for (ch in prefix) {
            curr = curr.children[ch] ?: return null
        }
        return curr
    }

    // Count words with given prefix — O(L + subtree size)
    fun countWordsWithPrefix(prefix: String): Int {
        val node = getNode(prefix) ?: return 0
        return countWords(node)
    }

    private fun countWords(node: TrieNode): Int {
        var count = if (node.isEnd) 1 else 0
        for (child in node.children.values) {
            count += countWords(child)
        }
        return count
    }

    // Delete a word — O(L)
    fun delete(word: String): Boolean {
        return deleteHelper(root, word, 0)
    }

    private fun deleteHelper(node: TrieNode, word: String, depth: Int): Boolean {
        if (depth == word.length) {
            if (!node.isEnd) return false         // word not found
            node.isEnd = false
            return node.children.isEmpty()        // true = safe to delete this node
        }
        val ch = word[depth]
        val child = node.children[ch] ?: return false
        val shouldDelete = deleteHelper(child, word, depth + 1)
        if (shouldDelete) {
            node.children.remove(ch)
            return !node.isEnd && node.children.isEmpty()
        }
        return false
    }
}
```

### Array-Based Trie (Faster — Only for Lowercase a-z)

```kotlin
class TrieNode {
    val children = arrayOfNulls<TrieNode>(26)     // index 0='a', 25='z'
    var isEnd = false
}

class Trie {
    private val root = TrieNode()

    fun insert(word: String) {
        var curr = root
        for (ch in word) {
            val i = ch - 'a'
            if (curr.children[i] == null) curr.children[i] = TrieNode()
            curr = curr.children[i]!!
        }
        curr.isEnd = true
    }

    fun search(word: String): Boolean {
        var curr = root
        for (ch in word) {
            curr = curr.children[ch - 'a'] ?: return false
        }
        return curr.isEnd
    }

    fun startsWith(prefix: String): Boolean {
        var curr = root
        for (ch in prefix) {
            curr = curr.children[ch - 'a'] ?: return false
        }
        return true
    }
}
```

### Trie — Quick API Reference

|Method|Returns|Time|What it does|
|---|---|---|---|
|`insert(word)`|`Unit`|O(L)|add word|
|`search(word)`|`Boolean`|O(L)|exact match|
|`startsWith(prefix)`|`Boolean`|O(L)|prefix exists|
|`countWordsWithPrefix(prefix)`|`Int`|O(L + subtree)|count matching|
|`delete(word)`|`Boolean`|O(L)|remove word|

---

## 26.2 Union Find (Disjoint Set Union — DSU)

### What It Does

- Groups elements into disjoint sets
- `find(x)` — which set does x belong to?
- `union(x, y)` — merge the sets containing x and y
- Both operations nearly O(1) with path compression + union by rank

### Standard Union Find — Path Compression + Union by Rank

```kotlin
class UnionFind(private val n: Int) {

    private val parent = IntArray(n) { it }       // parent[i] = i initially (each is its own root)
    private val rank   = IntArray(n) { 0 }        // rank = upper bound on height of subtree
    var components = n                             // track number of connected components

    // Find root of x with path compression — O(α(n)) ≈ O(1)
    fun find(x: Int): Int {
        if (parent[x] != x) {
            parent[x] = find(parent[x])            // path compression: point directly to root
        }
        return parent[x]
    }

    // Union sets containing x and y — O(α(n)) ≈ O(1)
    // Returns true if they were in different sets (a merge happened)
    fun union(x: Int, y: Int): Boolean {
        val rootX = find(x)
        val rootY = find(y)
        if (rootX == rootY) return false           // already in same set

        // Union by rank — attach smaller tree under larger tree
        when {
            rank[rootX] < rank[rootY] -> parent[rootX] = rootY
            rank[rootX] > rank[rootY] -> parent[rootY] = rootX
            else -> {
                parent[rootY] = rootX
                rank[rootX]++
            }
        }

        components--
        return true
    }

    // Check if x and y are in the same set — O(α(n))
    fun connected(x: Int, y: Int): Boolean = find(x) == find(y)

    // Get size of set containing x
    // ⚠️ Requires adding a 'size' array — see weighted version below
}
```

### Weighted Union Find (Tracks Component Size)

```kotlin
class UnionFind(private val n: Int) {

    private val parent = IntArray(n) { it }
    private val size   = IntArray(n) { 1 }        // size[root] = size of that component
    var components = n

    fun find(x: Int): Int {
        if (parent[x] != x) parent[x] = find(parent[x])
        return parent[x]
    }

    fun union(x: Int, y: Int): Boolean {
        val rootX = find(x)
        val rootY = find(y)
        if (rootX == rootY) return false

        // Attach smaller component under larger
        if (size[rootX] < size[rootY]) {
            parent[rootX] = rootY
            size[rootY] += size[rootX]
        } else {
            parent[rootY] = rootX
            size[rootX] += size[rootY]
        }

        components--
        return true
    }

    fun connected(x: Int, y: Int): Boolean = find(x) == find(y)

    // Get size of component containing x
    fun getSize(x: Int): Int = size[find(x)]
}
```

### Usage

```kotlin
val uf = UnionFind(5)         // nodes: 0,1,2,3,4

uf.union(0, 1)                // merge 0 and 1
uf.union(1, 2)                // merge 1 and 2 → {0,1,2} are connected
uf.connected(0, 2)            // true — 0 and 2 are in same component
uf.connected(0, 3)            // false — 3 is separate
uf.components                 // 3 — {0,1,2}, {3}, {4}
uf.find(2)                    // root of component containing 2

// Build from edge list
val uf = UnionFind(n)
for (edge in edges) {
    uf.union(edge[0], edge[1])
}
val componentCount = uf.components
```

### Union Find — Quick API Reference

|Method|Returns|Time|What it does|
|---|---|---|---|
|`find(x)`|`Int`|O(α(n))|root of x's component|
|`union(x, y)`|`Boolean`|O(α(n))|merge; true if merged|
|`connected(x, y)`|`Boolean`|O(α(n))|same component check|
|`getSize(x)`|`Int`|O(α(n))|size of x's component|
|`components`|`Int`|O(1)|number of components|

> α(n) = inverse Ackermann — effectively constant, never exceeds 4 for any practical input.

---

## 26.3 Segment Tree

### What It Does

- Range queries — sum / min / max over any subarray [l, r] in O(log n)
- Point updates — update a single element in O(log n)
- Space — O(4n) for the tree array

### Range Sum Segment Tree

```kotlin
class SegmentTree(private val n: Int) {

    private val tree = IntArray(4 * n)             // 4n is safe size

    // Build from array — O(n)
    fun build(arr: IntArray, node: Int = 1, start: Int = 0, end: Int = n - 1) {
        if (start == end) {
            tree[node] = arr[start]                 // leaf node = array value
            return
        }
        val mid = (start + end) / 2
        build(arr, 2 * node, start, mid)            // build left child
        build(arr, 2 * node + 1, mid + 1, end)      // build right child
        tree[node] = tree[2 * node] + tree[2 * node + 1]  // merge: sum
    }

    // Point update: set arr[idx] = val — O(log n)
    fun update(idx: Int, `val`: Int, node: Int = 1, start: Int = 0, end: Int = n - 1) {
        if (start == end) {
            tree[node] = `val`
            return
        }
        val mid = (start + end) / 2
        if (idx <= mid) update(idx, `val`, 2 * node, start, mid)
        else            update(idx, `val`, 2 * node + 1, mid + 1, end)
        tree[node] = tree[2 * node] + tree[2 * node + 1]  // re-merge
    }

    // Range query: sum of arr[l..r] inclusive — O(log n)
    fun query(l: Int, r: Int, node: Int = 1, start: Int = 0, end: Int = n - 1): Int {
        if (r < start || end < l) return 0         // completely outside — identity for sum
        if (l <= start && end <= r) return tree[node]  // completely inside
        val mid = (start + end) / 2
        return query(l, r, 2 * node, start, mid) +
               query(l, r, 2 * node + 1, mid + 1, end)
    }
}
```

### Range Min Segment Tree

```kotlin
class SegmentTreeMin(private val n: Int) {

    private val tree = IntArray(4 * n) { Int.MAX_VALUE }

    fun build(arr: IntArray, node: Int = 1, start: Int = 0, end: Int = n - 1) {
        if (start == end) { tree[node] = arr[start]; return }
        val mid = (start + end) / 2
        build(arr, 2 * node, start, mid)
        build(arr, 2 * node + 1, mid + 1, end)
        tree[node] = minOf(tree[2 * node], tree[2 * node + 1])  // ← min merge
    }

    fun update(idx: Int, `val`: Int, node: Int = 1, start: Int = 0, end: Int = n - 1) {
        if (start == end) { tree[node] = `val`; return }
        val mid = (start + end) / 2
        if (idx <= mid) update(idx, `val`, 2 * node, start, mid)
        else            update(idx, `val`, 2 * node + 1, mid + 1, end)
        tree[node] = minOf(tree[2 * node], tree[2 * node + 1])  // ← min merge
    }

    fun query(l: Int, r: Int, node: Int = 1, start: Int = 0, end: Int = n - 1): Int {
        if (r < start || end < l) return Int.MAX_VALUE             // identity for min
        if (l <= start && end <= r) return tree[node]
        val mid = (start + end) / 2
        return minOf(
            query(l, r, 2 * node, start, mid),
            query(l, r, 2 * node + 1, mid + 1, end)
        )
    }
}
```

### Usage

```kotlin
val arr = intArrayOf(1, 3, 5, 7, 9, 11)
val st = SegmentTree(arr.size)
st.build(arr)

st.query(1, 3)          // sum of arr[1..3] = 3+5+7 = 15
st.update(2, 10)        // arr[2] = 10
st.query(1, 3)          // now = 3+10+7 = 20
```

### Segment Tree — Quick API Reference

|Method|Time|What it does|
|---|---|---|
|`build(arr)`|O(n)|build from array|
|`update(idx, val)`|O(log n)|point update|
|`query(l, r)`|O(log n)|range query|

### Adapting the Merge Operation

```kotlin
// Change only these two lines in build/update/query:
// Sum:    tree[node] = tree[2*node] + tree[2*node+1]    identity: 0
// Min:    tree[node] = minOf(...)                        identity: Int.MAX_VALUE
// Max:    tree[node] = maxOf(...)                        identity: Int.MIN_VALUE
// GCD:    tree[node] = gcd(tree[2*node], tree[2*node+1]) identity: 0
// Count:  tree[node] = tree[2*node] + tree[2*node+1]    same as sum
```

---

## 26.4 Fenwick Tree (Binary Indexed Tree — BIT)

### What It Does

- **Prefix sum queries** — sum of arr[0..i] in O(log n)
- **Point updates** — add delta to arr[i] in O(log n)
- Simpler and faster constant than Segment Tree for prefix sum use cases
- Space — O(n)

> Use Fenwick Tree when: only prefix/range sums needed + point updates. Use Segment Tree when: range min/max, or range updates needed.

### Standard Fenwick Tree (1-Indexed)

```kotlin
class FenwickTree(private val n: Int) {

    private val tree = IntArray(n + 1)             // 1-indexed internally

    // Add delta to position i (1-indexed) — O(log n)
    fun update(i: Int, delta: Int) {
        var x = i
        while (x <= n) {
            tree[x] += delta
            x += x and (-x)                        // move to next responsible node
        }
    }

    // Prefix sum: sum of arr[1..i] (1-indexed) — O(log n)
    fun prefixSum(i: Int): Int {
        var x = i
        var sum = 0
        while (x > 0) {
            sum += tree[x]
            x -= x and (-x)                        // move to parent
        }
        return sum
    }

    // Range sum: sum of arr[l..r] (1-indexed) — O(log n)
    fun rangeSum(l: Int, r: Int): Int = prefixSum(r) - prefixSum(l - 1)

    // Point query: value at position i — O(log n)
    fun pointQuery(i: Int): Int = rangeSum(i, i)
}
```

### 0-Indexed Wrapper

```kotlin
class FenwickTree0(n: Int) {

    private val bit = FenwickTree(n)

    // 0-indexed: update position i
    fun update(i: Int, delta: Int) = bit.update(i + 1, delta)

    // 0-indexed: prefix sum arr[0..i]
    fun prefixSum(i: Int): Int = bit.prefixSum(i + 1)

    // 0-indexed: range sum arr[l..r]
    fun rangeSum(l: Int, r: Int): Int = bit.rangeSum(l + 1, r + 1)
}
```

### Usage

```kotlin
val bit = FenwickTree(6)              // supports indices 1..6

// Build from array
val arr = intArrayOf(1, 3, 5, 7, 9, 11)
for (i in arr.indices) bit.update(i + 1, arr[i])   // 1-indexed

bit.prefixSum(3)      // sum arr[1..3] = 1+3+5 = 9
bit.rangeSum(2, 5)    // sum arr[2..5] = 3+5+7+9 = 24
bit.update(3, 4)      // arr[3] += 4  (now arr[3] = 9)
bit.prefixSum(3)      // 1+3+9 = 13
```

### Fenwick Tree — Quick API Reference

|Method|Time|What it does|
|---|---|---|
|`update(i, delta)`|O(log n)|add delta to position i|
|`prefixSum(i)`|O(log n)|sum of positions 1..i|
|`rangeSum(l, r)`|O(log n)|sum of positions l..r|

### Fenwick vs Segment Tree

|Feature|Fenwick Tree|Segment Tree|
|---|---|---|
|Prefix / range sum|✅|✅|
|Range min / max|❌|✅|
|Range updates|❌ (extra trick)|✅ (lazy prop)|
|Space|O(n)|O(4n)|
|Code complexity|Simple|More complex|
|Constant factor|Faster|Slower|
|When to use|prefix sum + point update|everything else|

---

# 27. Performance Tips

## 27.1 IntArray vs List<Int>

```kotlin
// IntArray — stores raw primitives (int[])
val arr = IntArray(n)            // no boxing — compact memory

// List<Int> — stores boxed Integer objects
val list = mutableListOf<Int>()  // each Int becomes a heap object
```

|Aspect|`IntArray`|`MutableList<Int>`|
|---|---|---|
|Memory|~4 bytes per element|~16 bytes per element (boxed)|
|Cache performance|✅ contiguous memory|❌ pointer-chasing|
|Fixed size|✅ yes|❌ (resizes dynamically)|
|Dynamic add/remove|❌|✅|
|Functional ops|need `.toList()` first|direct|
|Speed in tight loops|✅ faster|❌ slower|

**Rule:** Use `IntArray` for all numeric DSA arrays. Use `MutableList<Int>` only when you need dynamic resizing.

```kotlin
// ❌ Slower — boxing overhead
val dp = MutableList(n) { 0 }
for (i in 0 until n) dp[i] = dp[i] + 1

// ✅ Faster — no boxing
val dp = IntArray(n)
for (i in 0 until n) dp[i] = dp[i] + 1

// ❌ Array<Int> = boxed — avoid for numbers
val arr = Array(n) { 0 }         // stores Integer objects

// ✅ IntArray = primitive — use this
val arr = IntArray(n)             // stores int primitives
```

---

## 27.2 HashMap vs TreeMap

```kotlin
// HashMap — O(1) average for get/put/containsKey
val map = HashMap<Int, Int>()

// TreeMap — O(log n) for get/put/containsKey, but always sorted
val tmap = TreeMap<Int, Int>()
```

|Operation|`HashMap`|`TreeMap`|When to use TreeMap|
|---|---|---|---|
|get / put|O(1) avg|O(log n)|need sorted keys|
|containsKey|O(1) avg|O(log n)|need floor/ceiling|
|iteration|unordered|sorted|need ordered output|
|floorKey/ceilingKey|❌|O(log n)|range queries|

**Rule:** Use `HashMap` by default. Switch to `TreeMap` ONLY when you need sorted iteration, floor/ceiling queries, or range queries.

```kotlin
// ❌ Overkill — TreeMap when HashMap would do
val freq = TreeMap<Char, Int>()     // O(log n) for every op
for (ch in s) freq[ch] = (freq[ch] ?: 0) + 1

// ✅ Correct — HashMap is fine for frequency counting
val freq = HashMap<Char, Int>()     // O(1) average
for (ch in s) freq[ch] = (freq[ch] ?: 0) + 1
```

---

## 27.3 Set vs List Search

```kotlin
val list = listOf(1, 2, 3, 4, 5)
val set  = hashSetOf(1, 2, 3, 4, 5)

// List.contains — O(n) linear scan
list.contains(3)    // scans from beginning until found
3 in list           // same

// HashSet.contains — O(1) average
set.contains(3)     // hash lookup
3 in set            // same
```

**Rule:** If you call `.contains()` more than once on a collection that doesn't change, convert to `HashSet` first.

```kotlin
// ❌ O(n) per lookup — if called many times
for (query in queries) {
    if (query in list) { }         // O(n) each time
}

// ✅ O(1) per lookup — convert once, then query
val set = list.toHashSet()         // O(n) once
for (query in queries) {
    if (query in set) { }          // O(1) each time
}
```

---

## 27.4 Avoid Heavy Lambda Chains in Tight Loops

```kotlin
// ❌ Creates multiple intermediate lists — slow for large n
val result = nums
    .filter { it > 0 }             // allocates new list
    .map { it * 2 }                // allocates another list
    .filter { it < 100 }           // allocates another list
    .sum()

// ✅ Single pass — no intermediate allocations
var result = 0
for (n in nums) {
    if (n > 0) {
        val doubled = n * 2
        if (doubled < 100) result += doubled
    }
}

// ✅ Alternative — use sequences (lazy, no intermediate lists)
val result = nums.asSequence()
    .filter { it > 0 }
    .map { it * 2 }
    .filter { it < 100 }
    .sum()                         // single pass, no intermediate lists
```

### Sequences vs Collections

```kotlin
// Collection ops — eager (each step creates full list)
list.filter { }.map { }.take(5)

// Sequence ops — lazy (processes element by element, stops early if possible)
list.asSequence().filter { }.map { }.take(5).toList()
```

||Collection (eager)|Sequence (lazy)|
|---|---|---|
|Intermediate lists|✅ creates them|❌ no allocations|
|Short-circuit|❌ processes all|✅ stops early|
|When to use|small collections|large + chained ops|
|Overhead|allocation cost|sequence wrapping cost|

**Rule:** For chains of 3+ operations on large lists (>1000 elements), use `.asSequence()`.

---

## 27.5 Avoid Boxing

```kotlin
// Boxing = wrapping a primitive in an object (Int → Integer)
// This happens silently in many Kotlin/JVM contexts

// ❌ Causes boxing — generic type parameter forces boxing
fun <T : Comparable<T>> max(a: T, b: T): T = if (a > b) a else b
max(3, 5)     // 3 and 5 are boxed to Integer

// ✅ Specific type — no boxing
fun maxInt(a: Int, b: Int): Int = if (a > b) a else b
maxInt(3, 5)  // no boxing

// ❌ Array<Int> — boxed Integer[]
val arr = Array(n) { 0 }

// ✅ IntArray — primitive int[]
val arr = IntArray(n)

// ❌ List<Int> — boxed in internal array
val list = mutableListOf<Int>()

// ✅ IntArray — use when size is known
val arr = IntArray(n)

// ❌ Nullable Int? — always boxed (even if stored in IntArray context)
var x: Int? = null    // boxed Integer? on JVM

// ✅ Non-null Int — not boxed
var x: Int = 0

// ❌ Pair<Int,Int> — both Ints are boxed
val p = Pair(3, 4)    // two Integer objects on heap

// ✅ IntArray for coordinate pairs — avoids boxing
val p = intArrayOf(3, 4)
```

### Common Boxing Traps

|Code|Boxing?|Fix|
|---|---|---|
|`Array<Int>(n) { 0 }`|✅ yes|use `IntArray(n)`|
|`MutableList<Int>()`|✅ yes|use `IntArray` if size known|
|`Pair<Int,Int>`|✅ yes|use `IntArray(2)` or data class|
|`HashMap<Int,Int>`|✅ yes|unavoidable for maps|
|`PriorityQueue<Int>`|✅ yes|unavoidable for generic structures|
|`IntArray`|❌ no|primitive array|
|`Int` local var|❌ no|primitive on stack|
|`Int?` nullable|✅ yes|avoid nullable Int if possible|

---

## 27.6 String Building

```kotlin
// ❌ O(n²) — String concatenation in loop creates new String each time
var result = ""
for (i in 0 until n) result += chars[i]

// ✅ O(n) — StringBuilder
val sb = StringBuilder()
for (i in 0 until n) sb.append(chars[i])
val result = sb.toString()

// ✅ O(n) — buildString DSL
val result = buildString {
    for (i in 0 until n) append(chars[i])
}

// ✅ O(n) — joinToString (for collections)
val result = chars.joinToString("")
```

---

## 27.7 Pre-allocate Collections When Size is Known

```kotlin
// ❌ Starts small, resizes multiple times
val list = mutableListOf<Int>()
repeat(1000) { list.add(it) }

// ✅ Pre-allocate — no resizing
val list = ArrayList<Int>(1000)    // capacity hint
repeat(1000) { list.add(it) }

// ✅ Even better — IntArray if size is exactly known
val arr = IntArray(1000)
for (i in 0 until 1000) arr[i] = i

// ❌ HashMap starts with default capacity 16 — resizes at 0.75 load
val map = HashMap<Int, Int>()

// ✅ Pre-allocate for large maps
val map = HashMap<Int, Int>(expectedSize * 2)   // avoid rehashing
```

---

## 27.8 Avoid Repeated Computation

```kotlin
// ❌ Recomputes list.size on every iteration
for (i in 0 until list.size) { }   // size is O(1) so this is fine actually

// ❌ Recomputes expensive function on every iteration
for (i in 0 until arr.size) {
    if (someExpensiveCheck(arr[i])) { }   // called n times
}

// ✅ Compute once
val n = arr.size
val limit = computeOnce()
for (i in 0 until n) {
    if (arr[i] < limit) { }
}

// ❌ Repeated substring creation in loop
for (i in 0 until s.length) {
    val sub = s.substring(0, i)   // O(i) allocation each time
}

// ✅ Use indices and access chars directly
for (i in s.indices) {
    val ch = s[i]                  // O(1) — no allocation
}
```

---

# 28. Common DSA Patterns

> These are **structural skeletons** — the shape of the pattern, not a solution. Fill in the problem-specific logic where indicated.

---

## 28.1 Frequency Counting

### With HashMap

```kotlin
// Count frequency of each element
val freq = mutableMapOf<Int, Int>()
for (n in nums) {
    freq[n] = (freq[n] ?: 0) + 1
}

// Access
freq[x] ?: 0          // frequency of x (0 if not seen)
freq.getOrDefault(x, 0)  // same

// Most frequent
freq.maxByOrNull { it.value }?.key    // element with highest frequency
freq.entries.sortedByDescending { it.value }  // all sorted by freq

// Filter by frequency
freq.filter { (_, v) -> v >= 2 }      // elements appearing ≥ 2 times
```

### With IntArray (Faster for Characters / Small Range)

```kotlin
// For lowercase letters only
val freq = IntArray(26)
for (ch in s) freq[ch - 'a']++
freq['e' - 'a']    // frequency of 'e'

// For digits 0-9
val freq = IntArray(10)
for (ch in s) if (ch.isDigit()) freq[ch - '0']++

// For values in range [0, MAX]
val MAX = 10001
val freq = IntArray(MAX)
for (n in nums) freq[n]++
```

### One-Liner with groupingBy

```kotlin
val freq: Map<Int, Int> = nums.groupingBy { it }.eachCount()
val freq: Map<Char, Int> = s.groupingBy { it }.eachCount()
```

---

## 28.2 Sliding Window

> See full template in Chapter 23. This is a condensed structural reference.

### Fixed Window — Expand Then Slide

```kotlin
// Step 1: build initial window of size k
// Step 2: slide — add right, remove left, record

var window = /* initial k elements */
for (i in k until arr.size) {
    window = window + arr[i] - arr[i - k]  // slide
    best = update(best, window)
}
```

### Variable Window — Shrink When Invalid

```kotlin
var left = 0
var windowState = initialState

for (right in arr.indices) {
    windowState = expand(windowState, arr[right])    // add right element

    while (/* window is invalid */) {
        windowState = shrink(windowState, arr[left]) // remove left element
        left++
    }

    best = update(best, right - left + 1)
}
```

### Window State Types

|What tracks the window|State variable|
|---|---|
|Sum|`var sum = 0`|
|Character frequency|`HashMap<Char,Int>`|
|Distinct count|`HashMap<K,Int>` with `.size`|
|Max/min|`ArrayDeque` (monotonic)|
|Boolean condition|custom logic|

---

## 28.3 Prefix Sum

> See full template in Chapter 23. Condensed reference here.

```kotlin
// Build
val prefix = IntArray(n + 1)
for (i in 0 until n) prefix[i + 1] = prefix[i] + nums[i]

// Query: sum of nums[l..r]
prefix[r + 1] - prefix[l]

// Query: any subarray sum equal to target
// → use prefix + HashMap
val seen = mutableMapOf(0 to 1)     // prefix sum → count of occurrences
var running = 0
for (n in nums) {
    running += n
    count += seen.getOrDefault(running - target, 0)
    seen[running] = (seen[running] ?: 0) + 1
}
```

---

## 28.4 Greedy Selection

> Common structural shapes for greedy approaches.

### Sort-Based Greedy

```kotlin
// Step 1: sort by the criterion that makes greedy valid
items.sortBy { it.criterion }          // or sortWith for multi-key

// Step 2: iterate in sorted order, make greedy choice at each step
var state = initialState
for (item in items) {
    if (greedyCondition(item, state)) {
        state = update(state, item)    // take item
    }
    // else: skip item (greedy discard)
}
```

### Heap-Based Greedy (Always Pick Best Available)

```kotlin
val heap = PriorityQueue<Int>(reverseOrder())  // or custom comparator

for (item in items) {
    heap.offer(item)
    if (/* heap too large or other condition */) {
        heap.poll()                            // discard worst
    }
}
// heap contains the greedily selected items
```

### Interval Greedy (Sort by End Time)

```kotlin
val sorted = intervals.sortedBy { it[1] }     // sort by end time
var lastEnd = Int.MIN_VALUE
var count = 0

for (interval in sorted) {
    if (interval[0] >= lastEnd) {             // no overlap with last selected
        lastEnd = interval[1]
        count++
    }
}
```

---

## 28.5 BFS Shortest Path

> BFS gives shortest path in unweighted graphs (each edge = cost 1).

```kotlin
// Setup
val dist = IntArray(n) { -1 }
val queue = ArrayDeque<Int>()

dist[source] = 0
queue.addLast(source)

// BFS
while (queue.isNotEmpty()) {
    val node = queue.removeFirst()

    if (node == target) return dist[node]      // early exit if target found

    for (neighbor in graph[node]) {
        if (dist[neighbor] == -1) {
            dist[neighbor] = dist[node] + 1
            queue.addLast(neighbor)
        }
    }
}

return dist[target]    // -1 if unreachable

// Grid version — replace node with (row, col)
// State version — replace Int with data class State(...)
```

---

## 28.6 Topological Sort

### BFS-Based (Kahn's Algorithm — Using In-Degree)

```kotlin
fun topologicalSort(n: Int, edges: Array<IntArray>): IntArray {
    val graph  = Array(n) { mutableListOf<Int>() }
    val inDeg  = IntArray(n)

    // Build graph + compute in-degrees
    for ((u, v) in edges) {
        graph[u].add(v)
        inDeg[v]++
    }

    // Start with all nodes that have no incoming edges
    val queue = ArrayDeque<Int>()
    for (i in 0 until n) {
        if (inDeg[i] == 0) queue.addLast(i)
    }

    val order = mutableListOf<Int>()

    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        order.add(node)

        for (neighbor in graph[node]) {
            inDeg[neighbor]--
            if (inDeg[neighbor] == 0) queue.addLast(neighbor)
        }
    }

    // If order.size != n — cycle exists (not a valid DAG)
    return if (order.size == n) order.toIntArray() else IntArray(0)
}
```

### DFS-Based Topological Sort

```kotlin
val visited = IntArray(n)    // 0=unvisited, 1=in-progress, 2=done
val order = mutableListOf<Int>()
var hasCycle = false

fun dfs(node: Int) {
    if (visited[node] == 1) { hasCycle = true; return }  // cycle
    if (visited[node] == 2) return                         // already done

    visited[node] = 1                          // mark in-progress

    for (neighbor in graph[node]) dfs(neighbor)

    visited[node] = 2                          // mark done
    order.add(node)                            // post-order add
}

for (i in 0 until n) if (visited[i] == 0) dfs(i)

val topoOrder = order.reversed()               // reverse post-order = topological
```

---

## 28.7 Dynamic Programming — Structural Patterns

### Bottom-Up 1D DP

```kotlin
val dp = IntArray(n + 1)
dp[0] = baseCase

for (i in 1..n) {
    dp[i] = /* transition from dp[i-1], dp[i-2], etc. */
}

return dp[n]
```

### Bottom-Up 2D DP

```kotlin
val dp = Array(m + 1) { IntArray(n + 1) }

// Initialize base cases
for (i in 0..m) dp[i][0] = /* base */
for (j in 0..n) dp[0][j] = /* base */

// Fill
for (i in 1..m) {
    for (j in 1..n) {
        dp[i][j] = /* transition: dp[i-1][j], dp[i][j-1], dp[i-1][j-1] */
    }
}

return dp[m][n]
```

### Top-Down (Memoization)

```kotlin
val memo = HashMap<Int, Long>()          // or IntArray(-1) for dense keys

fun dp(state: Int): Long {
    if (/* base case */) return baseValue
    memo[state]?.let { return it }       // cached

    val result = /* recursive transitions */
    memo[state] = result
    return result
}
```

### Space-Optimized 1D DP (Rolling Array)

```kotlin
// When dp[i] only depends on dp[i-1]
var prev = baseCase
var curr = 0

for (i in 1..n) {
    curr = /* transition using prev */
    prev = curr
}

return prev

// When dp[i] depends on dp[i-1] AND dp[i-2]
var prevPrev = base0
var prev     = base1

for (i in 2..n) {
    val curr = /* transition using prev, prevPrev */
    prevPrev = prev
    prev = curr
}

return prev
```

---

# 29. Debugging Helpers

## 29.1 `joinToString` — Print Any Collection

```kotlin
// IntArray
intArrayOf(1, 2, 3).joinToString()        // "1, 2, 3"
intArrayOf(1, 2, 3).joinToString(" ")     // "1 2 3"
intArrayOf(1, 2, 3).joinToString(", ", "[", "]")  // "[1, 2, 3]"

// List
listOf(1, 2, 3).joinToString()            // "1, 2, 3"
listOf("a","b","c").joinToString(" | ")   // "a | b | c"

// With transform
intArrayOf(1,2,3).joinToString { "($it)" }  // "(1), (2), (3)"

// CharArray
charArrayOf('a','b','c').joinToString()   // "a, b, c"
charArrayOf('a','b','c').concatToString() // "abc" (no separator)
```

## 29.2 Print Collections

```kotlin
// Direct println — works for most Kotlin collections
println(listOf(1, 2, 3))               // [1, 2, 3]
println(mutableMapOf("a" to 1))        // {a=1}
println(setOf(1, 2, 3))               // [1, 2, 3]
println(arrayOf(1, 2, 3).toList())     // [1, 2, 3]

// IntArray — must convert first (println(IntArray) prints address)
println(intArrayOf(1, 2, 3).toList())              // [1, 2, 3]
println(intArrayOf(1, 2, 3).joinToString())        // 1, 2, 3
println(intArrayOf(1, 2, 3).contentToString())     // [1, 2, 3] ← clean

// 2D array
val matrix = arrayOf(intArrayOf(1,2), intArrayOf(3,4))
println(matrix.map { it.toList() })                // [[1, 2], [3, 4]]
println(matrix.contentDeepToString())              // [[1, 2], [3, 4]] ← clean

// ArrayDeque (stack/queue)
val dq = ArrayDeque(listOf(1,2,3))
println(dq)                                        // [1, 2, 3]

// PriorityQueue — toList() gives heap-order (NOT sorted)
val heap = PriorityQueue<Int>()
heap.offer(3); heap.offer(1); heap.offer(2)
println(heap.toList())                             // [1, 3, 2] — heap internal order
// ✅ To see sorted: drain
println(generateSequence { heap.poll() }.toList()) // [1, 2, 3]
```

## 29.3 Labelled Debug Print

```kotlin
// Quick labelled print (useful in LeetCode custom test)
fun <T> debug(label: String, value: T): T {
    println("$label: $value")
    return value
}

// Usage
val result = debug("dp", dp.toList())              // prints and returns
val sum = debug("sum", nums.sum())
```

## 29.4 Print 2D Grid / Matrix

```kotlin
fun printGrid(grid: Array<IntArray>) {
    for (row in grid) {
        println(row.joinToString(" ") { it.toString().padStart(3) })
    }
}

fun printGrid(grid: Array<CharArray>) {
    for (row in grid) println(row.joinToString(" "))
}

fun printGrid(grid: Array<BooleanArray>) {
    for (row in grid) println(row.joinToString(" ") { if (it) "T" else "." })
}
```

## 29.5 Trace Variables in a Loop

```kotlin
// Print state on each iteration
for (i in arr.indices) {
    println("i=$i val=${arr[i]} dp=${dp[i]}")    // trace
}

// Conditional trace (only when something interesting happens)
for (i in arr.indices) {
    if (arr[i] > threshold) {
        println("⚡ i=$i: arr[$i]=${arr[i]} exceeds threshold")
    }
}
```

## 29.6 Fast Test Cases in LeetCode

```kotlin
// Build IntArray quickly
val nums = intArrayOf(3, 1, 4, 1, 5, 9, 2, 6)

// Build 2D array
val grid = arrayOf(
    intArrayOf(1, 0, 1),
    intArrayOf(0, 0, 0),
    intArrayOf(1, 0, 1)
)

// Build String from chars
val s = "aababba"

// Build tree manually
val root = TreeNode(1).apply {
    left = TreeNode(2).apply {
        left  = TreeNode(4)
        right = TreeNode(5)
    }
    right = TreeNode(3)
}

// Build LinkedList
fun buildList(vararg vals: Int): ListNode? {
    val dummy = ListNode(0)
    var curr = dummy
    for (v in vals) { curr.next = ListNode(v); curr = curr.next!! }
    return dummy.next
}
val head = buildList(1, 2, 3, 4, 5)

// Print LinkedList
fun printList(head: ListNode?) {
    var curr = head
    val parts = mutableListOf<Int>()
    while (curr != null) { parts.add(curr.`val`); curr = curr.next }
    println(parts.joinToString(" → "))
}
```

## 29.7 Useful Assertions for Local Testing

```kotlin
// Quick equality check
fun assertEquals(expected: Any?, actual: Any?) {
    if (expected != actual) println("❌ Expected $expected but got $actual")
    else println("✅ Pass")
}

// Array equality check
fun assertArrayEquals(expected: IntArray, actual: IntArray) {
    if (!expected.contentEquals(actual))
        println("❌ Expected ${expected.contentToString()} but got ${actual.contentToString()}")
    else println("✅ Pass")
}

// Usage
assertEquals(15, myFunction(intArrayOf(1,2,3,4,5)))
assertArrayEquals(intArrayOf(1,2,3), mySort(intArrayOf(3,1,2)))
```

---

# 30. Quick Decision Tables

## 30.1 Data Structure Selection

|You need to...|Best Structure|Why|
|---|---|---|
|Fast access by index|`IntArray` / `MutableList`|O(1) random access|
|Dynamic add/remove at end|`MutableList`|O(1) amortized add|
|Dynamic add/remove at front|`ArrayDeque`|O(1) both ends|
|Dynamic add/remove in middle|`MutableList`|O(n) but simplest|
|Key → Value, O(1) lookup|`HashMap`|O(1) average|
|Key → Value, sorted by key|`TreeMap`|O(log n), sorted|
|Key → Value, insertion order|`LinkedHashMap`|O(1), ordered|
|Count frequencies|`HashMap<T,Int>` + `?: 0`|O(1) per update|
|Existence check, O(1)|`HashSet`|O(1) contains|
|Unique elements, sorted|`TreeSet`|O(log n), sorted|
|Floor/ceiling queries|`TreeMap` / `TreeSet`|O(log n) navigation|
|Min always at top|`PriorityQueue<T>()`|O(log n) insert/poll|
|Max always at top|`PriorityQueue<T>(reverseOrder())`|O(log n)|
|Custom priority|`PriorityQueue<T> { a,b -> }`|O(log n)|
|LIFO stack|`ArrayDeque` (addLast/removeLast)|O(1)|
|FIFO queue|`ArrayDeque` (addLast/removeFirst)|O(1)|
|Both ends O(1)|`ArrayDeque`|O(1) all end ops|
|Range sum queries|`FenwickTree` / `prefix array`|O(log n) / O(1)|
|Range min/max queries|`SegmentTree`|O(log n)|
|Prefix matching|`Trie`|O(L) where L=length|
|Connected components|`UnionFind`|O(α(n)) ≈ O(1)|
|Shortest path (unweighted)|`BFS` + queue|O(V + E)|
|Shortest path (weighted)|`Dijkstra` + PriorityQueue|O((V+E) log V)|
|Cycle detection (directed)|`DFS` + visited state|O(V + E)|
|Topological order|`Kahn's BFS` or `DFS post-order`|O(V + E)|

---

## 30.2 Mutable vs Immutable Collections

|You have|Mutable equivalent|When to use mutable|
|---|---|---|
|`listOf()`|`mutableListOf()`|when you need add/remove|
|`mapOf()`|`mutableMapOf()`|when you need put/remove|
|`setOf()`|`mutableSetOf()`|when you need add/remove|
|`arrayOf()`|always mutable|—|
|`IntArray`|always mutable|—|

---

## 30.3 In-Place vs New Collection

|You want|In-place (mutates original)|New (original unchanged)|
|---|---|---|
|Sort list|`list.sort()`|`list.sorted()`|
|Sort descending list|`list.sortDescending()`|`list.sortedDescending()`|
|Sort list by key|`list.sortBy { }`|`list.sortedBy { }`|
|Sort IntArray|`arr.sort()`|`arr.sorted()` → List|
|Sort Array<T>|`arr.sort()` / `arr.sortWith { }`|`arr.sortedWith { }` → List|
|Reverse list|`list.reverse()`|`list.reversed()`|
|Reverse IntArray|`arr.reverse()`|`arr.reversed()` → List|
|Reverse String|not possible|`s.reversed()`|
|Fill IntArray|`arr.fill(v)`|—|
|Fill List|`list.fill(v)`|—|
|Shuffle List|`list.shuffle()`|`list.shuffled()`|

---

## 30.4 Time Complexity Reference

### Core Operations

|Structure|Access|Search|Insert End|Insert Front|Delete|Sort|
|---|---|---|---|---|---|---|
|`IntArray`|O(1)|O(n)|—|—|—|O(n log n)|
|`MutableList`|O(1)|O(n)|O(1)*|O(n)|O(n)|O(n log n)|
|`ArrayDeque`|O(1)|O(n)|O(1)|O(1)|O(1) ends|—|
|`HashMap`|—|O(1)*|O(1)*|—|O(1)*|—|
|`TreeMap`|—|O(lg n)|O(lg n)|—|O(lg n)|always sorted|
|`HashSet`|—|O(1)*|O(1)*|—|O(1)*|—|
|`TreeSet`|—|O(lg n)|O(lg n)|—|O(lg n)|always sorted|
|`PriorityQueue`|O(1) peek|O(n)|O(lg n)|—|O(lg n) top|—|
|`Trie`|O(L)|O(L)|O(L)|—|O(L)|—|
|`UnionFind`|—|O(α)|—|—|—|—|
|`SegmentTree`|—|O(lg n)|O(lg n)|—|O(lg n)|—|
|`FenwickTree`|—|O(lg n)|O(lg n)|—|O(lg n)|—|

`*` = amortized average | `α` = inverse Ackermann ≈ O(1) | `L` = string length

### Algorithm Complexities

|Algorithm|Time|Space|Notes|
|---|---|---|---|
|Binary Search|O(log n)|O(1)|array must be sorted|
|Two Pointers|O(n)|O(1)|array usually sorted|
|Sliding Window|O(n)|O(k) or O(1)|k = window or alphabet size|
|Prefix Sum build|O(n)|O(n)|query O(1) after build|
|BFS|O(V + E)|O(V)|shortest path unweighted|
|DFS|O(V + E)|O(V)|recursion stack = O(depth)|
|Topological Sort|O(V + E)|O(V)|Kahn's or DFS|
|Backtracking|O(b^d)|O(d)|b=branching, d=depth|
|Merge Sort|O(n log n)|O(n)|stable|
|Quick Sort|O(n log n)*|O(log n)|not stable, in-place|
|Heap Sort|O(n log n)|O(1)|not stable|
|Dijkstra|O((V+E) log V)|O(V)|with PriorityQueue|
|Floyd-Warshall|O(V³)|O(V²)|all-pairs shortest path|
|Bellman-Ford|O(VE)|O(V)|handles negative edges|

---

## 30.5 Space Complexity Reference

|Structure|Space|
|---|---|
|`IntArray(n)`|O(n) — primitive|
|`MutableList<Int>(n)`|O(n) — boxed|
|`HashMap<K,V>` with n entries|O(n)|
|`HashSet<T>` with n elements|O(n)|
|`TreeMap` / `TreeSet` with n|O(n)|
|`PriorityQueue` with n|O(n)|
|Recursive call stack (depth d)|O(d)|
|BFS queue|O(V) — width of graph|
|Prefix sum array|O(n)|
|Segment tree|O(4n)|
|Fenwick tree|O(n)|
|Trie (n words, avg length L)|O(n × L)|
|Union Find|O(n)|
|Adjacency list|O(V + E)|
|Adjacency matrix|O(V²)|

---

## 30.6 Kotlin-Specific Gotchas — Master List

```
TYPES & DECLARATIONS
  ✗ arr.length    → ✓ arr.size          (arrays/collections)
  ✗ str.size      → ✓ str.length        (strings only)
  ✗ implicit cast → ✓ .toInt()/.toLong() etc.
  ✗ Array<Int>    → ✓ IntArray          (for numbers)

NULL SAFETY
  ✗ map[key] is Int, not Int?  → map[key] is ALWAYS Int? — use ?: 0
  ✗ stack.last() on empty      → ✓ stack.lastOrNull()
  ✗ heap.poll() forced unwrap  → ✓ heap.poll() ?: return

COLLECTIONS
  ✗ listOf() is read-only      → ✓ mutableListOf() for DSA
  ✗ mapOf() is read-only       → ✓ mutableMapOf() for DSA
  ✗ setOf() is read-only       → ✓ mutableSetOf() for DSA
  ✗ java.util.Stack            → ✓ ArrayDeque

SORTING
  ✗ arr.sorted() returns List  → ✓ arr.sort() for in-place IntArray sort
  ✗ a - b comparator overflow  → ✓ Integer.compare(a, b)
  ✗ heap.toList() = sorted     → ✗ NOT sorted — drain with poll()

LOOPS
  ✗ forEach has no break       → ✓ for loop when break needed
  ✗ return@forEach = return    → return@forEach = CONTINUE only
  ✗ for (e in stack) = top→btm → for loop goes BOTTOM→TOP

STRINGS
  ✗ "a.b".split(".")           → "." is regex — use split('.')
  ✗ String + in loop = O(n²)  → ✓ StringBuilder

MATH
  ✗ -7 % 3 == 1                → -7 % 3 == -1 in Kotlin (same sign as dividend)
  ✗ abs(Int.MIN_VALUE)         → still negative (overflow)
  ✗ pow(2, 10) for Int         → must use 2.0.pow(10.0).toInt()

BIT OPS
  ✗ Java: a & b                → ✓ Kotlin: a and b
  ✗ Java: a | b                → ✓ Kotlin: a or b
  ✗ Java: ~a                   → ✓ Kotlin: a.inv()
  ✗ Java: a << n               → ✓ Kotlin: a shl n
  ✗ Java: a >> n               → ✓ Kotlin: a shr n

TREE
  ✗ node.val                   → ✓ node.`val` (backtick — val is keyword)

2D ARRAYS
  ✗ matrix.contentEquals()     → ✓ matrix.contentDeepEquals()
  ✗ matrix.toString()          → ✓ matrix.contentDeepToString()
  ✗ val copy = arr             → shallow ref copy — ✓ arr.copyOf()
```

---

## 30.7 When to Use What — 1-Line Rules

```
Need O(1) lookup?              → HashMap / HashSet
Need sorted order?             → TreeMap / TreeSet
Need sorted + floor/ceiling?   → TreeMap / TreeSet
Need LIFO?                     → ArrayDeque (addLast/removeLast)
Need FIFO?                     → ArrayDeque (addLast/removeFirst)
Need min/max always at top?    → PriorityQueue
Need range sum in O(1)?        → Build prefix sum array
Need range sum + updates?      → FenwickTree
Need range min/max + updates?  → SegmentTree
Need prefix matching?          → Trie
Need connected components?     → UnionFind
Need string count chars?       → IntArray(26) for a-z, HashMap for general
Need to build string in loop?  → StringBuilder, not String +=
Need to sort custom objects?   → sortWith(compareBy { }) or Comparator
Need index + element?          → withIndex() in for loop
Need to check if already seen? → HashSet
Need frequency of elements?    → HashMap + (map[k] ?: 0) + 1
```

---

## COMPLETE GUIDE INDEX

|File|Chapters|
|---|---|
|`Kotlin_DSA_Guide_Ch1-5.md`|1. Kotlin vs Java · 2. Variables & Types · 3. Arrays · 4. MutableList · 5. HashMap|
|`Kotlin_DSA_Guide_Ch6-10.md`|6. HashSet · 7. Stack · 8. Queue & Deque · 9. PriorityQueue · 10. String & StringBuilder|
|`Kotlin_DSA_Guide_Ch11-15.md`|11. Loops · 12. Ranges · 13. Null Safety · 14. Sorting · 15. Math Utilities|
|`Kotlin_DSA_Guide_Ch16-20.md`|16. Bit Operations · 17. Lambdas · 18. Pair & Triple · 19. Type Conversions · 20. when|
|`Kotlin_DSA_Guide_Ch21-25.md`|21. TreeMap & TreeSet · 22. Looping Cheat Sheet · 23. Algorithm Templates · 24. Graph Templates · 25. Tree Templates|
|`Kotlin_DSA_Guide_Ch26-30.md`|26. Data Structure Templates · 27. Performance Tips · 28. DSA Patterns · 29. Debugging · 30. Decision Tables|

---

_End of Kotlin DSA Reference Guide — All 30 Chapters Complete_