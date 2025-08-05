 

---

### ❌ **Mistake: Function Reference with Named & Default Arguments**

```kotlin
fun add(a1: Int, a2: Int = 12, a3: Int) = a1 + a2 + a3
val addNum = ::add
addNum(10, 20, a3 = 50) // ❌ Error: No value passed for parameter 'a3'
```

---

### 🧩 **What Went Wrong:**

1. 🔸 You used a **function reference** (`::add`) with a function that has **default and non-default parameters in the wrong order**.
    
2. 🔸 In Kotlin, **non-default parameters must come before default ones** unless you're **explicitly naming every parameter**.
    
3. 🔸 Function references **don’t allow mixing named and positional arguments easily**.
    
4. 🔸 `a3` does **not** have a default value, but was placed after `a2`, which does — **this breaks the rule**.
    

---

### ✅ **Correct Version 1: Reorder Parameters**

```kotlin
fun add(a1: Int, a3: Int, a2: Int = 12) = a1 + a2 + a3
val addNum = ::add
addNum(10, 50) // a1 = 10, a3 = 50, a2 uses default = 12
```

- Now all **required parameters come first**
    
- `a2` (default) is last — ✅ Kotlin Best Practice
    

---

### ✅ **Correct Version 2: Use Named Parameters for All**

```kotlin
fun add(a1: Int, a2: Int = 12, a3: Int) = a1 + a2 + a3
val addNum = ::add
addNum(a1 = 10, a2 = 20, a3 = 50) // ✅ All parameters named
```

- Works fine, but **less clean** than reordering
    

---

### 🔖 **Obsidian Structure Suggestion**

````markdown
# Kotlin / Functions / Function References

## 🧠 Common Mistake: Using function reference with default + non-default parameters

### ❌ Incorrect
```kotlin
fun add(a1: Int, a2: Int = 12, a3: Int)
val addNum = ::add
addNum(10, 20, a3 = 50) // ❌
````

### ✅ Fix 1: Reorder parameters

```kotlin
fun add(a1: Int, a3: Int, a2: Int = 12)
val addNum = ::add
addNum(10, 50) // ✅
```

### ✅ Fix 2: Use full named arguments

```kotlin
val addNum = ::add
addNum(a1 = 10, a2 = 20, a3 = 50) // ✅
```

### 🔍 Notes

- Default parameters should be placed **at the end**
    
- Function references work best with **positional arguments**
    
- Named arguments in function reference calls require **all** arguments to be named
    

```

Let me know if you want a `.md` file version of this to drop directly into Obsidian.
```