# Backend DSA Pattern Roadmap

## 1. HashMap / HashSet

Backend use:

- Idempotency keys
- Session lookup
- Duplicate request detection
- Product ID to inventory lookup
- Frequency counting

Core idea:

```text
Trade memory for O(1) average lookup.
```

Solve:

- Two Sum
- Contains Duplicate
- Valid Anagram
- Group Anagrams
- Top K Frequent Elements

## 2. Two Pointers

Backend use:

- Merging sorted data
- Comparing streams
- Deduplicating ordered records
- Compacting arrays/lists

Core idea:

```text
Use two positions to avoid nested loops.
```

Solve:

- Valid Palindrome
- 3Sum
- Container With Most Water
- Merge Sorted Array

## 3. Sliding Window

Backend use:

- Rate limiting
- Rolling metrics
- Recent activity windows
- Fraud detection windows

Core idea:

```text
Maintain a valid window while moving through data once.
```

Solve:

- Best Time to Buy and Sell Stock
- Longest Substring Without Repeating Characters
- Minimum Size Subarray Sum
- Permutation in String

## 4. Stack / Monotonic Stack

Backend use:

- Parsing
- Undo/redo
- Nested validation
- Next greater/smaller logic

Core idea:

```text
Keep unresolved items on a stack until a future item resolves them.
```

Solve:

- Valid Parentheses
- Min Stack
- Daily Temperatures
- Evaluate Reverse Polish Notation

## 5. Queue / BFS

Backend use:

- Job processing
- Level-order traversal
- Shortest path in unweighted graph
- Workflow expansion

Core idea:

```text
Process in first-in-first-out order.
```

Solve:

- Binary Tree Level Order Traversal
- Rotting Oranges
- Number of Islands
- Clone Graph

## 6. Binary Search

Backend use:

- Searching sorted indexes
- Finding capacity thresholds
- Pagination boundaries
- Time-based lookups

Core idea:

```text
Repeatedly cut the search space in half.
```

Solve:

- Binary Search
- Search Insert Position
- Search in Rotated Sorted Array
- Find Minimum in Rotated Sorted Array

## 7. Heap / PriorityQueue

Backend use:

- Priority jobs
- Top-k analytics
- Nearest delivery partner candidates
- Expiring reservations

Core idea:

```text
Keep the next best item available in O(log n).
```

Solve:

- Kth Largest Element in an Array
- Top K Frequent Elements
- Merge K Sorted Lists
- Task Scheduler

## 8. Intervals

Backend use:

- Delivery slot overlap
- Booking systems
- Calendar availability
- Inventory reservation windows

Core idea:

```text
Sort by start time, then merge or detect overlap.
```

Solve:

- Merge Intervals
- Insert Interval
- Non-overlapping Intervals
- Meeting Rooms
- Meeting Rooms II

## 9. Graphs

Backend use:

- Service dependency graph
- Delivery route graph
- Workflow states
- Recommendation relationships

Core idea:

```text
Model entities as nodes and relationships as edges.
```

Solve:

- Number of Islands
- Clone Graph
- Course Schedule
- Pacific Atlantic Water Flow
- Network Delay Time

## 10. Trie

Backend use:

- Search autocomplete
- Prefix matching
- Query suggestions
- Dictionary lookup

Core idea:

```text
Store strings by shared prefixes.
```

Solve:

- Implement Trie
- Word Search II
- Search Suggestions System

## 11. Dynamic Programming

Backend use:

- Optimization
- Cost calculation
- Route or resource planning
- Caching repeated subproblems

Core idea:

```text
Remember previous answers to avoid recomputation.
```

Solve:

- Climbing Stairs
- House Robber
- Coin Change
- Longest Increasing Subsequence
- Longest Common Subsequence

## 12. Union Find

Backend use:

- Grouping connected components
- Region/service-zone grouping
- Detecting connected clusters
- Fast merge/find operations

Core idea:

```text
Track which items belong to the same group.
```

Solve:

- Number of Connected Components in an Undirected Graph
- Redundant Connection
- Accounts Merge
- Number of Islands

