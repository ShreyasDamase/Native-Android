

These constraints are **hints disguised as rules**. Here's how to decode them:

---

### `1 <= arr.length <= 10⁴`

|What it means|What it tells you|
|---|---|
|Array has at least 1 element|No need to handle empty array edge case|
|Array has at most **10,000** elements|**O(n²) is risky** (~10⁸ ops), aim for O(n log n) or O(n)|

---

### `1 <= arr[i] <= 10⁵`

|What it means|What it tells you|
|---|---|
|No negative numbers, no zero|No need to handle negatives — simplifies logic|
|Max value is **100,000**|You can use a **frequency array** of size 10⁵ safely|

---

## The "Big O from Constraints" Cheat Sheet

This is the most useful skill to develop:

|Input size (n)|Max acceptable complexity|Typical approach|
|---|---|---|
|n ≤ 10|O(n!)|Brute force, permutations|
|n ≤ 20|O(2ⁿ)|Bitmask DP, backtracking|
|n ≤ 100|O(n³)|Triple nested loops|
|n ≤ 10³|O(n²)|Double nested loops|
|n ≤ 10⁴|O(n² ) risky, O(n log n) safe|Sorting, binary search|
|n ≤ 10⁵|O(n log n)|Heap, merge sort, segment tree|
|n ≤ 10⁶|O(n)|Two pointers, sliding window, hash map|
|n ≤ 10⁹|O(log n)|Binary search, math|

> **Rule of thumb:** LeetCode allows ~**10⁸ operations/second**. If n=10⁴ and you write O(n²), that's 10⁸ — borderline TLE.

---

## Other Common Constraints & What They Signal

|Constraint|Hidden hint|
|---|---|
|`-10⁹ <= arr[i] <= 10⁹`|Use `long`/`int64`, watch for **overflow** on addition|
|`arr[i]` is **distinct**|No duplicates — simplifies searching, hashing|
|Array is **sorted**|Think **binary search** or two pointers|
|`1 <= n <= 10⁵`, values `1..n`|Classic **cycle detection** or index-as-hash trick|
|Values are `0` or `1`|Bit manipulation or XOR tricks|
|`k` is small (k ≤ 20)|DP over subsets, sliding window of size k|

---

## Quick Decision Flow

```
See constraint → Ask yourself:
      │
      ├─ n ≤ 10⁴?  → O(n²) might pass, but O(n log n) is safer
      ├─ values ≤ 10⁵? → frequency/bucket array is fine
      ├─ all positive? → no negative index issues, simpler math
      └─ no duplicates? → direct mapping / set operations work cleanly
```

Once you internalize this, constraints stop being fine print and start being **the first thing you read** before even touching the problem.