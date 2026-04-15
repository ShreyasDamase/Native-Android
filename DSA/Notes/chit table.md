# Kotlin Collection Functions — Complete Cheat Sheet by Type

This guide covers `Array`, `List`, `Set`, and `Map` (including mutable/immutable variants).  
- `[In-place]` means the function modifies the original collection – only works on mutable types.  
- All other functions return a new collection or value and work on both mutable and immutable collections.

---

## 1. Array (including IntArray, DoubleArray, etc.)

Works on all array types (`Array<T>`, `IntArray`, `CharArray`, etc.).  
Primitive arrays have the same functions except those that return `Array<T>` (use `toTypedArray()` if needed).

### Access & Safe Access

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `get(index)` / `[index]` | Get element; throws if out of bounds | `arr[1]` | `"B"` |
| `first()` | First element; throws if empty | `arrayOf(1,2,3).first()` | `1` |
| `last()` | Last element; throws if empty | `arrayOf(1,2,3).last()` | `3` |
| `size` | Number of elements (property) | `arrayOf(1,2,3).size` | `3` |
| `getOrNull(index)` | Element or `null` if out of bounds | `arr.getOrNull(10)` | `null` |
| `getOrElse(index) { default }` | Element or default from lambda | `arr.getOrElse(10) { -1 }` | `-1` |
| `firstOrNull()` | First element or `null` if empty | `arrayOf<Int>().firstOrNull()` | `null` |
| `firstOrNull { cond }` | First matching or `null` | `arr.firstOrNull { it > 3 }` | `4` |
| `lastOrNull()` | Last element or `null` if empty | `arrayOf<Int>().lastOrNull()` | `null` |
| `lastOrNull { cond }` | Last matching or `null` | `arr.lastOrNull { it > 3 }` | `5` |

### Search

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `find { cond }` | First matching or `null` | `arr.find { it > 2 }` | `3` |
| `findLast { cond }` | Last matching or `null` | `arr.findLast { it > 2 }` | `5` |
| `indexOf(value)` | First index of value, or -1 | `arr.indexOf("B")` | `1` |
| `lastIndexOf(value)` | Last index of value, or -1 | `arrayOf(1,2,2).lastIndexOf(2)` | `2` |
| `indexOfFirst { cond }` | First index matching condition, or -1 | `arr.indexOfFirst { it > 2 }` | `2` |
| `indexOfLast { cond }` | Last index matching condition, or -1 | `arr.indexOfLast { it > 2 }` | `4` |
| `contains(value)` / `in` | Check existence | `3 in arr` | `true` |

### Condition Checks

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `any()` | `true` if not empty | `arrayOf(1).any()` | `true` |
| `any { cond }` | At least one matches | `arr.any { it > 4 }` | `true` |
| `all { cond }` | All match | `arr.all { it > 0 }` | `true` |
| `none()` | `true` if empty | `arrayOf<Int>().none()` | `true` |
| `none { cond }` | No element matches | `arr.none { it > 10 }` | `true` |
| `isEmpty()` / `isNotEmpty()` | Size check | `arrayOf(1).isNotEmpty()` | `true` |

### Transform (return new collection)

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `map { transform }` | Transform each element | `arr.map { it * 2 }` | `[2,4,6]` |
| `mapIndexed { i, v -> }` | Map with index | `arr.mapIndexed { i, v -> "$i:$v" }` | `["0:1","1:2"]` |
| `mapNotNull { }` | Map, drop null results | `arr.mapNotNull { if (it>1) it else null }` | `[2,3]` |
| `flatMap { }` | Map + flatten | `arr.flatMap { listOf(it, it*10) }` | `[1,10,2,20]` |
| `flatten()` | Flatten nested collections | `listOf(listOf(1,2), listOf(3)).flatten()` | `[1,2,3]` |
| `forEach { }` | Iterate (no return) | `arr.forEach { println(it) }` | prints each |
| `forEachIndexed { i, v -> }` | Iterate with index | `arr.forEachIndexed { i,v -> println("$i=$v") }` | `0=1, 1=2 …` |
| `reversed()` | Reverse order | `arrayOf(1,2,3).reversed()` | `[3,2,1]` |
| `zip(other)` | Pair with another collection | `listOf(1,2).zip(listOf("A","B"))` | `[(1,A),(2,B)]` |
| `unzip()` | Split list of pairs | `listOf(1 to "A",2 to "B").unzip()` | `([1,2],[A,B])` |

### Filter

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `filter { cond }` | Keep matching | `arr.filter { it > 2 }` | `[3,4,5]` |
| `filterNot { cond }` | Keep non-matching | `arr.filterNot { it > 2 }` | `[1,2]` |
| `filterNotNull()` | Remove nulls | `listOf(1,null,2).filterNotNull()` | `[1,2]` |
| `filterIndexed { i, v -> }` | Filter with index | `arr.filterIndexed { i,_ -> i%2==0 }` | `[1,3,5]` |
| `partition { cond }` | Split into pair of lists | `arr.partition { it > 2 }` | `([3,4,5],[1,2])` |
| `take(n)` | First n elements | `arr.take(2)` | `[1,2]` |
| `takeLast(n)` | Last n elements | `arr.takeLast(2)` | `[4,5]` |
| `takeWhile { cond }` | Take from start while true | `arr.takeWhile { it < 3 }` | `[1,2]` |
| `drop(n)` | Skip first n | `arr.drop(2)` | `[3,4,5]` |
| `dropLast(n)` | Skip last n | `arr.dropLast(2)` | `[1,2,3]` |
| `dropWhile { cond }` | Skip from start while true | `arr.dropWhile { it < 3 }` | `[3,4,5]` |
| `slice(range)` | Subrange as List | `arr.slice(1..3)` | `[2,3,4]` |
| `sliceArray(range)` | Subrange as Array | `arr.sliceArray(0..2)` | `[1,2,3]` |
| `distinct()` | Remove duplicates | `arrayOf(1,1,2,3).distinct()` | `[1,2,3]` |
| `distinctBy { key }` | Remove duplicates by key | `arr.distinctBy { it % 2 }` | `[1,2]` |

### Aggregation (single value)

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `count()` | Number of elements | `arr.count()` | `5` |
| `count { cond }` | Number matching condition | `arr.count { it > 2 }` | `3` |
| `sum()` | Sum of numeric elements | `intArrayOf(1,2,3).sum()` | `6` |
| `sumOf { }` | Sum after transform | `arr.sumOf { it * 2 }` | `30` |
| `average()` | Average | `intArrayOf(1,2,3).average()` | `2.0` |
| `minOrNull()` | Smallest or null | `arr.minOrNull()` | `1` |
| `maxOrNull()` | Largest or null | `arr.maxOrNull()` | `5` |
| `minByOrNull { key }` | Element with smallest key | `arr.minByOrNull { it }` | `1` |
| `maxByOrNull { key }` | Element with largest key | `arr.maxByOrNull { it }` | `5` |
| `reduce { acc, v -> }` | Combine left to right (throws if empty) | `arr.reduce { acc,i -> acc+i }` | `15` |
| `fold(initial) { acc, v -> }` | Reduce with initial value | `arr.fold(0) { acc,i -> acc+i }` | `15` |

### Sorting

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `sort()` [In-place] | Sort ascending (mutates) | `intArrayOf(3,1,2).sort()` | original becomes `[1,2,3]` |
| `sorted()` | Return new sorted list (ascending) | `arr.sorted()` | `[1,2,3,4,5]` |
| `sortedDescending()` | Return new sorted list (descending) | `arr.sortedDescending()` | `[5,4,3,2,1]` |
| `sortedBy { key }` | Sort by selector | `arr.sortedBy { it }` | `[1,2,3,4,5]` |
| `sortedByDescending { key }` | Sort by selector descending | `arr.sortedByDescending { it }` | `[5,4,3,2,1]` |
| `sortedWith(comparator)` | Sort with custom comparator | `arr.sortedWith(compareBy { it })` | `[1,2,3,4,5]` |

### Grouping & Mapping to Map

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `groupBy { key }` | Group into Map<Key, List<T>> | `arr.groupBy { if(it%2==0) "even" else "odd" }` | `{odd=[1,3,5], even=[2,4]}` |
| `associate { pair }` | Transform each to Pair and collect Map | `arr.associate { it to it*10 }` | `{1=10,2=20,3=30,4=40,5=50}` |
| `associateBy { key }` | Use selector as key, element as value | `arr.associateBy { it*10 }` | `{10=1,20=2,30=3,40=4,50=5}` |
| `associateWith { value }` | Element as key, selector as value | `arr.associateWith { it*2 }` | `{1=2,2=4,3=6,4=8,5=10}` |

### Chunking & Windowing

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `chunked(n)` | Split into chunks of size n | `(1..6).toList().chunked(2)` | `[[1,2],[3,4],[5,6]]` |
| `chunked(n) { transform }` | Chunk then transform each | `(1..6).chunked(2) { it.sum() }` | `[3,7,11]` |
| `windowed(n)` | Sliding window of size n | `(1..5).windowed(3)` | `[[1,2,3],[2,3,4],[3,4,5]]` |
| `windowed(n, step, partial)` | Window with step and partial | `(1..5).windowed(3,2,true)` | `[[1,2,3],[3,4,5],[5]]` |

### Array-specific (copy, fill, reverse, content utils)

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `copyOf()` | Full copy | `arrayOf(1,2,3).copyOf()` | `[1,2,3]` |
| `copyOf(newSize)` | Copy with new size (pads null/0 or truncates) | `arrayOf(1,2,3).copyOf(5)` | `[1,2,3,null,null]` |
| `copyOfRange(from, to)` | Copy range | `arrayOf(1,2,3,4).copyOfRange(1,3)` | `[2,3]` |
| `fill(value)` [In-place] | Fill all elements | `arr.fill(0)` | `[0,0,0,0,0]` |
| `fill(value, from, to)` [In-place] | Fill range | `arr.fill(9,1,3)` | `[1,9,9,4,5]` |
| `reverse()` [In-place] | Reverse in-place | `arr.reverse()` | original reversed |
| `contentEquals(other)` | Compare arrays | `a.contentEquals(b)` | `true`/`false` |
| `contentToString()` | Pretty print | `arrayOf(1,2,3).contentToString()` | `"[1, 2, 3]"` |

### Conversion

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `toList()` | To immutable List | `arr.toList()` | `List<Int>` |
| `toMutableList()` | To MutableList | `arr.toMutableList()` | `MutableList<Int>` |
| `toSet()` | To Set (removes duplicates) | `arr.toSet()` | `Set<Int>` |
| `toMutableSet()` | To MutableSet | `arr.toMutableSet()` | `MutableSet<Int>` |
| `asList()` | Array as List (backed, O(1)) | `arr.asList()` | `List<Int>` |
| `toIntArray()` (for collections) | Convert to IntArray | `listOf(1,2,3).toIntArray()` | `IntArray` |
| `toTypedArray()` (for primitive arrays) | Convert to Array<T> | `intArrayOf(1,2).toTypedArray()` | `Array<Int>` |

### String Output

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `joinToString()` | Join with default ", " | `arr.joinToString()` | `"1, 2, 3"` |
| `joinToString(separator)` | Custom separator | `arr.joinToString(" - ")` | `"1 - 2 - 3"` |
| `joinToString(sep, prefix, postfix)` | With prefix/suffix | `arr.joinToString(", ","[","]")` | `"[1, 2, 3]"` |
| `joinToString { transform }` | Transform each element | `arr.joinToString { "($it)" }` | `"(1), (2), (3)"` |

---

## 2. List (List vs MutableList)

`List` is read-only (immutable size/content). `MutableList` adds functions that modify the list.

### Functions that work on both List and MutableList (all non-modifying)

All functions from Array sections **Access, Safe Access, Search, Condition Checks, Transform, Filter, Aggregation, Sorting (sorted\* family), Grouping, Chunking, Conversion (to\*), String Output** work exactly the same on `List`. Examples are identical; just replace `arrayOf` with `listOf`.

### Functions unique to MutableList (in-place modifications)

| Function | Purpose | Example | Effect |
|----------|---------|---------|--------|
| `add(element)` | Append element | `list.add(4)` | list becomes `[1,2,3,4]` |
| `add(index, element)` | Insert at index | `list.add(1, 99)` | `[1,99,2,3]` |
| `addAll(collection)` | Append all | `list.addAll(listOf(4,5))` | `[1,2,3,4,5]` |
| `addAll(index, collection)` | Insert all at index | `list.addAll(1, listOf(99,100))` | `[1,99,100,2,3]` |
| `remove(element)` | Remove first occurrence | `list.remove(2)` | `[1,3]` |
| `removeAt(index)` | Remove by index | `list.removeAt(1)` | `[1,3]` |
| `removeAll(collection)` | Remove all matching | `list.removeAll(listOf(2,3))` | `[1]` |
| `retainAll(collection)` | Keep only those in collection | `list.retainAll(listOf(2,3))` | `[2,3]` |
| `clear()` | Remove all elements | `list.clear()` | `[]` |
| `set(index, element)` | Replace element | `list.set(1, 99)` | `[1,99,3]` |
| `sort()` [In-place] | Sort ascending | `list.sort()` | `[1,2,3]` |
| `sortDescending()` [In-place] | Sort descending | `list.sortDescending()` | `[3,2,1]` |
| `shuffle()` [In-place] | Randomize order | `list.shuffle()` | random order |
| `reverse()` [In-place] | Reverse in-place | `list.reverse()` | `[3,2,1]` |
| `fill(value)` [In-place] | Fill all with value | `list.fill(0)` | `[0,0,0]` |

### List-specific: `listOfNotNull`

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `listOfNotNull(vararg elements)` | Create list ignoring nulls | `listOfNotNull(1, null, 2)` | `[1, 2]` |

---

## 3. Set (Set vs MutableSet)

`Set` stores unique elements. `MutableSet` adds modification functions.

### Creating Sets

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `setOf(elements)` | Immutable set | `setOf(1,2,2,3)` | `[1,2,3]` |
| `mutableSetOf(elements)` | Mutable set | `mutableSetOf(1,2,3)` | `[1,2,3]` |
| `hashSetOf()` | HashSet (mutable) | `hashSetOf(1,2,3)` | `[1,2,3]` |
| `linkedSetOf()` | LinkedHashSet (maintains order) | `linkedSetOf(3,1,2)` | `[3,1,2]` |
| `sortedSetOf()` | TreeSet (sorted) | `sortedSetOf(3,1,2)` | `[1,2,3]` |

### Common Set Operations (return new Set)

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `union(other)` | Elements in either set | `setOf(1,2).union(setOf(2,3))` | `[1,2,3]` |
| `intersect(other)` | Elements in both | `setOf(1,2).intersect(setOf(2,3))` | `[2]` |
| `subtract(other)` | Elements in first but not second | `setOf(1,2).subtract(setOf(2,3))` | `[1]` |
| `plus(element)` / `+` | Add element (new set) | `setOf(1,2) + 3` | `[1,2,3]` |
| `minus(element)` / `-` | Remove element (new set) | `setOf(1,2,3) - 2` | `[1,3]` |

### MutableSet specific (in-place)

| Function | Purpose | Example | Effect |
|----------|---------|---------|--------|
| `add(element)` | Add element (if not present) | `set.add(4)` | set now contains 4 |
| `remove(element)` | Remove element | `set.remove(2)` | 2 removed |
| `addAll(collection)` | Add all | `set.addAll(listOf(4,5))` | adds 4,5 |
| `removeAll(collection)` | Remove all | `set.removeAll(listOf(2,3))` | removes 2,3 |
| `retainAll(collection)` | Keep only those | `set.retainAll(listOf(1,2))` | keeps 1,2 |
| `clear()` | Remove all | `set.clear()` | empty set |

### Set-specific functions

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `containsAll(collection)` | Check if contains all | `setOf(1,2,3).containsAll(listOf(1,2))` | `true` |
| `elementAt(index)` | Get element at position (order depends on implementation) | `setOf(1,2,3).elementAt(1)` | `2` (if LinkedHashSet) |

> Note: Most transform/filter functions (`map`, `filter`, etc.) work on `Set` and return a `List` by default. Use `toSet()` to convert back to `Set`.

---

## 4. Map (Map vs MutableMap)

`Map` stores key-value pairs. `MutableMap` adds modification functions.

### Creating Maps

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `mapOf(pairs)` | Immutable map | `mapOf(1 to "A", 2 to "B")` | `{1=A, 2=B}` |
| `mutableMapOf(pairs)` | Mutable map | `mutableMapOf(1 to "A")` | `{1=A}` |
| `hashMapOf()` | HashMap | `hashMapOf(1 to "A")` | `{1=A}` |
| `linkedMapOf()` | LinkedHashMap (preserves order) | `linkedMapOf(2 to "B", 1 to "A")` | `{2=B, 1=A}` |
| `sortedMapOf()` | TreeMap (sorted by key) | `sortedMapOf(2 to "B", 1 to "A")` | `{1=A, 2=B}` |

### Access & Safe Access

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `get(key)` / `[key]` | Get value or null | `map[1]` | `"A"` |
| `getValue(key)` | Get value or throw | `map.getValue(1)` | `"A"` |
| `getOrDefault(key, default)` | Value or default | `map.getOrDefault(3, "X")` | `"X"` |
| `getOrElse(key) { default }` | Value or computed default | `map.getOrElse(3) { "X" }` | `"X"` |
| `keys` | Set of keys | `map.keys` | `[1,2]` |
| `values` | Collection of values | `map.values` | `["A","B"]` |
| `entries` | Set of Map.Entry | `map.entries` | `[1=A, 2=B]` |

### Search & Condition Checks

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `containsKey(key)` / `key in map` | Check key existence | `1 in map` | `true` |
| `containsValue(value)` | Check value existence | `map.containsValue("A")` | `true` |
| `isEmpty()` / `isNotEmpty()` | Size check | `map.isNotEmpty()` | `true` |
| `any { condition }` | Any entry matches | `map.any { it.key > 1 }` | `true` |
| `all { condition }` | All entries match | `map.all { it.value is String }` | `true` |

### Transform (return new Map/Collection)

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `mapKeys { transform }` | Transform keys (new map) | `map.mapKeys { it.key * 10 }` | `{10=A, 20=B}` |
| `mapValues { transform }` | Transform values (new map) | `map.mapValues { it.value.lowercase() }` | `{1=a, 2=b}` |
| `map { (k,v) -> newPair }` | Transform to list of pairs | `map.map { (k,v) -> k to v+v }` | `[(1,AA), (2,BB)]` |
| `flatMap { }` | Flatten after transform | `map.flatMap { (k,v) -> listOf(k, v) }` | `[1,"A",2,"B"]` |
| `filter { condition }` | Keep entries matching | `map.filter { it.key > 1 }` | `{2=B}` |
| `filterKeys { condition }` | Filter by key | `map.filterKeys { it == 1 }` | `{1=A}` |
| `filterValues { condition }` | Filter by value | `map.filterValues { it == "A" }` | `{1=A}` |
| `plus(pair)` / `+` | Add entry (new map) | `map + (3 to "C")` | `{1=A,2=B,3=C}` |
| `minus(key)` / `-` | Remove key (new map) | `map - 1` | `{2=B}` |

### Aggregation

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `count()` | Number of entries | `map.count()` | `2` |
| `count { cond }` | Number matching condition | `map.count { it.key > 1 }` | `1` |
| `keys.minOrNull()` / `keys.maxOrNull()` | Min/max key | `map.keys.minOrNull()` | `1` |
| `values.sumOf { }` | Sum of values (if numeric) | `mapOf(1 to 10, 2 to 20).values.sum()` | `30` |

### MutableMap specific (in-place)

| Function | Purpose | Example | Effect |
|----------|---------|---------|--------|
| `put(key, value)` / `[key] = value` | Add or replace | `map[3] = "C"` | map now has key 3 |
| `putAll(fromMap)` | Add all from another map | `map.putAll(mapOf(3 to "C"))` | adds entry |
| `remove(key)` | Remove entry by key | `map.remove(1)` | removes 1=A |
| `remove(key, value)` | Remove only if key-value matches | `map.remove(1, "A")` | removes only if matches |
| `clear()` | Remove all entries | `map.clear()` | empty map |
| `replace(key, newValue)` | Replace value if key exists | `map.replace(1, "X")` | `{1=X, 2=B}` |
| `replace(key, oldValue, newValue)` | Replace only if current matches | `map.replace(1, "A", "X")` | `{1=X}` |
| `getOrPut(key) { defaultValue }` | Get or compute and put | `map.getOrPut(3) { "C" }` | returns "C", map now has 3=C |

### Conversion

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `toMap()` | Convert to immutable Map | `mutableMapOf(1 to "A").toMap()` | `Map<Int,String>` |
| `toMutableMap()` | Convert to MutableMap | `map.toMutableMap()` | `MutableMap<Int,String>` |
| `toList()` | Convert entries to list of pairs | `map.toList()` | `[(1,A), (2,B)]` |
| `toPair()` | For single entry map | `mapOf(1 to "A").toPair()` | `(1, A)` |

### String Output

| Function | Purpose | Example | Output |
|----------|---------|---------|--------|
| `joinToString()` | Join entries | `map.joinToString()` | `"1=A, 2=B"` |
| `joinToString(separator, prefix, postfix) { (k,v) -> ... }` | Custom transform | `map.joinToString(separator = " | ") { "${it.key}->${it.value}" }` | `"1->A | 2->B"` |

---

## Common Mistakes / Functions That Do Not Exist

| What you typed | Correct alternative |
|----------------|-------------------|
| `findFirst {}` | `firstOrNull {}` or `find {}` |
| `firstIndexOf()` | `indexOf()` (already means first) |
| `getOrDefault()` on Array/List | `getOrElse {}` (or use `getOrDefault` only for Map) |
| `sort()` on immutable List | Use `sorted()` (returns new list) |
| `add()` on immutable List | Use `toMutableList()` first or `plus` operator |
| `filterOn {}` | `filter {}` |
| `forEachIndexed` on Map | Use `forEach { (k,v) -> }` |
| `mapKeys` on List | That’s for Map only – use `associate` or `map` |

---

 
