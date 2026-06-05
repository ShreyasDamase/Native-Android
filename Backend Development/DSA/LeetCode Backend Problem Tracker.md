# LeetCode Backend Problem Tracker

> **Goal:** Become so fluent in these patterns that you implement solutions without AI — saving tokens, carbon, and building real backend instinct.

Status: `Todo` · `Solved` · `Review` · `Redo`

---

## Progress Summary

| Category | Total | Solved | Review | Redo |
| --- | --- | --- | --- | --- |
| Array / Two Pointers | 10 | 0 | 0 | 0 |
| HashMap / HashSet | 10 | 0 | 0 | 0 |
| Sliding Window | 7 | 0 | 0 | 0 |
| Stack | 8 | 0 | 0 | 0 |
| Binary Search | 9 | 0 | 0 | 0 |
| Linked List | 9 | 0 | 0 | 0 |
| Trees | 13 | 0 | 0 | 0 |
| Heap / Priority Queue | 7 | 0 | 0 | 0 |
| Intervals | 7 | 0 | 0 | 0 |
| Graph / BFS / DFS | 12 | 0 | 0 | 0 |
| Union Find | 6 | 0 | 0 | 0 |
| Trie | 3 | 0 | 0 | 0 |
| Backtracking | 9 | 0 | 0 | 0 |
| Dynamic Programming | 17 | 0 | 0 | 0 |
| Greedy | 7 | 0 | 0 | 0 |
| Math / Bit Manipulation | 8 | 0 | 0 | 0 |
| Design Problems | 12 | 0 | 0 | 0 |
| **Total** | **154** | **0** | **0** | **0** |

---

## How to Use

1. Pick a problem → answer the **Related Questions** before coding
2. Code it cold (no hints) → time yourself
3. Fill the **Review Template** at the bottom
4. Update status + progress table

---

## Array / Two Pointers

| Status | Problem | Pattern | Why Backend |
| --- | --- | --- | --- |
| Todo | [1. Two Sum](https://leetcode.com/problems/two-sum/) | HashMap | Fast lookup, caches, ID maps |
| Todo | [167. Two Sum II - Input Array Is Sorted](https://leetcode.com/problems/two-sum-ii-input-array-is-sorted/) | Two Pointers | In-place pair finding on sorted data |
| Todo | [15. 3Sum](https://leetcode.com/problems/3sum/) | Two Pointers + Sort | Deduplication and multi-key lookup |
| Todo | [11. Container With Most Water](https://leetcode.com/problems/container-with-most-water/) | Two Pointers | Greedy shrinking of search space |
| Todo | [42. Trapping Rain Water](https://leetcode.com/problems/trapping-rain-water/) | Two Pointers / Stack | Space efficiency in bounded systems |
| Todo | [75. Sort Colors](https://leetcode.com/problems/sort-colors/) | Dutch National Flag | 3-way partition, in-place sorting |
| Todo | [128. Longest Consecutive Sequence](https://leetcode.com/problems/longest-consecutive-sequence/) | HashSet | O(n) range detection, useful for gap analysis |
| Todo | [169. Majority Element](https://leetcode.com/problems/majority-element/) | Boyer-Moore Vote | Majority voting in distributed consensus |
| Todo | [238. Product of Array Except Self](https://leetcode.com/problems/product-of-array-except-self/) | Prefix/Suffix | Running product, no division |
| Todo | [560. Subarray Sum Equals K](https://leetcode.com/problems/subarray-sum-equals-k/) | Prefix Sum + HashMap | Range sum queries, analytics |

---

## HashMap / HashSet

| Status | Problem | Pattern | Why Backend |
| --- | --- | --- | --- |
| Todo | [217. Contains Duplicate](https://leetcode.com/problems/contains-duplicate/) | HashSet | Idempotency key deduplication |
| Todo | [242. Valid Anagram](https://leetcode.com/problems/valid-anagram/) | Frequency Map | Text normalization, search preprocessing |
| Todo | [49. Group Anagrams](https://leetcode.com/problems/group-anagrams/) | HashMap Grouping | Aggregation, bucketing |
| Todo | [347. Top K Frequent Elements](https://leetcode.com/problems/top-k-frequent-elements/) | HashMap + Heap | Analytics, trending queries |
| Todo | [36. Valid Sudoku](https://leetcode.com/problems/valid-sudoku/) | HashSet per Row/Col/Box | Constraint validation at ingestion |
| Todo | [525. Contiguous Array](https://leetcode.com/problems/contiguous-array/) | Prefix Sum + HashMap | Balance detection in event streams |
| Todo | [438. Find All Anagrams in a String](https://leetcode.com/problems/find-all-anagrams-in-a-string/) | Sliding Window + Map | Pattern matching in log streams |
| Todo | [383. Ransom Note](https://leetcode.com/problems/ransom-note/) | Frequency Map | Resource availability checking |
| Todo | [350. Intersection of Two Arrays II](https://leetcode.com/problems/intersection-of-two-arrays-ii/) | HashMap | Set intersection, feature flags |
| Todo | [290. Word Pattern](https://leetcode.com/problems/word-pattern/) | Bidirectional Map | Config key-value pattern matching |

---

## Sliding Window

| Status | Problem | Pattern | Why Backend |
| --- | --- | --- | --- |
| Todo | [3. Longest Substring Without Repeating Characters](https://leetcode.com/problems/longest-substring-without-repeating-characters/) | Sliding Window + Set | Rate limit windows |
| Todo | [121. Best Time to Buy and Sell Stock](https://leetcode.com/problems/best-time-to-buy-and-sell-stock/) | One-pass Window | Tracking best previous state |
| Todo | [209. Minimum Size Subarray Sum](https://leetcode.com/problems/minimum-size-subarray-sum/) | Sliding Window | Shortest event burst triggering alert |
| Todo | [567. Permutation in String](https://leetcode.com/problems/permutation-in-string/) | Fixed Window + Freq | Fixed-size token matching |
| Todo | [239. Sliding Window Maximum](https://leetcode.com/problems/sliding-window-maximum/) | Monotonic Deque | Rolling max metrics |
| Todo | [76. Minimum Window Substring](https://leetcode.com/problems/minimum-window-substring/) | Sliding Window + Map | Minimum log segment containing all required fields |
| Todo | [424. Longest Repeating Character Replacement](https://leetcode.com/problems/longest-repeating-character-replacement/) | Sliding Window | Max valid window with at most K mutations |

---

## Stack

| Status | Problem | Pattern | Why Backend |
| --- | --- | --- | --- |
| Todo | [20. Valid Parentheses](https://leetcode.com/problems/valid-parentheses/) | Stack | Parser validation, DSL rules |
| Todo | [155. Min Stack](https://leetcode.com/problems/min-stack/) | Stack | Rollback, undo operations |
| Todo | [739. Daily Temperatures](https://leetcode.com/problems/daily-temperatures/) | Monotonic Stack | Next event resolution |
| Todo | [84. Largest Rectangle in Histogram](https://leetcode.com/problems/largest-rectangle-in-histogram/) | Monotonic Stack | Max resource allocation window |
| Todo | [150. Evaluate Reverse Polish Notation](https://leetcode.com/problems/evaluate-reverse-polish-notation/) | Stack | Expression evaluation, formula engines |
| Todo | [22. Generate Parentheses](https://leetcode.com/problems/generate-parentheses/) | Backtracking + Stack | Generating valid nested configs |
| Todo | [394. Decode String](https://leetcode.com/problems/decode-string/) | Stack | Nested template/config decoding |
| Todo | [853. Car Fleet](https://leetcode.com/problems/car-fleet/) | Monotonic Stack | Grouping tasks by completion time |

---

## Binary Search

| Status | Problem | Pattern | Why Backend |
| --- | --- | --- | --- |
| Todo | [704. Binary Search](https://leetcode.com/problems/binary-search/) | Binary Search | B-tree index lookup |
| Todo | [33. Search in Rotated Sorted Array](https://leetcode.com/problems/search-in-rotated-sorted-array/) | Binary Search | Circular buffer / rotated log search |
| Todo | [153. Find Minimum in Rotated Sorted Array](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/) | Binary Search | Find pivot in rotated data |
| Todo | [74. Search a 2D Matrix](https://leetcode.com/problems/search-a-2d-matrix/) | Binary Search on Matrix | 2D index lookup |
| Todo | [162. Find Peak Element](https://leetcode.com/problems/find-peak-element/) | Binary Search | Local maxima in metric data |
| Todo | [875. Koko Eating Bananas](https://leetcode.com/problems/koko-eating-bananas/) | Binary Search on Answer | Binary search on rate/capacity |
| Todo | [1011. Capacity to Ship Packages Within D Days](https://leetcode.com/problems/capacity-to-ship-packages-within-d-days/) | Binary Search on Answer | Minimum capacity planning |
| Todo | [410. Split Array Largest Sum](https://leetcode.com/problems/split-array-largest-sum/) | Binary Search on Answer | Load balancing across workers |
| Todo | [981. Time Based Key-Value Store](https://leetcode.com/problems/time-based-key-value-store/) | HashMap + Binary Search | Versioned storage, MVCC |

---

## Linked List

| Status | Problem | Pattern | Why Backend |
| --- | --- | --- | --- |
| Todo | [206. Reverse Linked List](https://leetcode.com/problems/reverse-linked-list/) | Pointer Reversal | Fundamental pointer manipulation |
| Todo | [21. Merge Two Sorted Lists](https://leetcode.com/problems/merge-two-sorted-lists/) | Two Pointers | Merging sorted streams |
| Todo | [23. Merge k Sorted Lists](https://leetcode.com/problems/merge-k-sorted-lists/) | Heap | Merging k sorted event streams |
| Todo | [141. Linked List Cycle](https://leetcode.com/problems/linked-list-cycle/) | Fast/Slow Pointers | Detecting infinite loops in pipelines |
| Todo | [142. Linked List Cycle II](https://leetcode.com/problems/linked-list-cycle-ii/) | Floyd's Algorithm | Finding cycle entry, memory leak detection |
| Todo | [19. Remove Nth Node From End of List](https://leetcode.com/problems/remove-nth-node-from-end-of-list/) | Two Pointers | One-pass removal |
| Todo | [143. Reorder List](https://leetcode.com/problems/reorder-list/) | Find Mid + Reverse + Merge | Multi-step pointer manipulation |
| Todo | [2. Add Two Numbers](https://leetcode.com/problems/add-two-numbers/) | Carry Simulation | Arbitrary-precision arithmetic |
| Todo | [25. Reverse Nodes in k-Group](https://leetcode.com/problems/reverse-nodes-in-k-group/) | Group Reversal | Batch processing in fixed-size chunks |

---

## Trees

| Status | Problem | Pattern | Why Backend |
| --- | --- | --- | --- |
| Todo | [104. Maximum Depth of Binary Tree](https://leetcode.com/problems/maximum-depth-of-binary-tree/) | DFS | Recursive depth reasoning |
| Todo | [102. Binary Tree Level Order Traversal](https://leetcode.com/problems/binary-tree-level-order-traversal/) | BFS | Layered dependency resolution |
| Todo | [98. Validate Binary Search Tree](https://leetcode.com/problems/validate-binary-search-tree/) | DFS + Bounds | Index invariant validation |
| Todo | [230. Kth Smallest Element in a BST](https://leetcode.com/problems/kth-smallest-element-in-a-bst/) | Inorder | Sorted traversal from tree index |
| Todo | [236. Lowest Common Ancestor of a Binary Tree](https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-tree/) | Tree Recursion | Org chart, category hierarchy |
| Todo | [199. Binary Tree Right Side View](https://leetcode.com/problems/binary-tree-right-side-view/) | BFS | Last node per level — rightmost visible |
| Todo | [543. Diameter of Binary Tree](https://leetcode.com/problems/diameter-of-binary-tree/) | DFS | Network diameter, longest path |
| Todo | [572. Subtree of Another Tree](https://leetcode.com/problems/subtree-of-another-tree/) | DFS + Match | Sub-config validation |
| Todo | [105. Construct Binary Tree from Preorder and Inorder Traversal](https://leetcode.com/problems/construct-binary-tree-from-preorder-and-inorder-traversal/) | DFS | Deserialization / tree reconstruction |
| Todo | [297. Serialize and Deserialize Binary Tree](https://leetcode.com/problems/serialize-and-deserialize-binary-tree/) | BFS / DFS | Tree serialization to DB/cache |
| Todo | [124. Binary Tree Maximum Path Sum](https://leetcode.com/problems/binary-tree-maximum-path-sum/) | DFS Post-order | Maximum weighted path in a graph |
| Todo | [110. Balanced Binary Tree](https://leetcode.com/problems/balanced-binary-tree/) | DFS | Balanced state validation |
| Todo | [112. Path Sum](https://leetcode.com/problems/path-sum/) | DFS | Root-to-leaf rule validation |

---

## Heap / Priority Queue

| Status | Problem | Pattern | Why Backend |
| --- | --- | --- | --- |
| Todo | [215. Kth Largest Element in an Array](https://leetcode.com/problems/kth-largest-element-in-an-array/) | Heap / Quickselect | p99 latency, top-k analytics |
| Todo | [621. Task Scheduler](https://leetcode.com/problems/task-scheduler/) | Heap + Greedy | Job queues with cooldowns |
| Todo | [295. Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/) | Two Heaps | Streaming p50 computation |
| Todo | [973. K Closest Points to Origin](https://leetcode.com/problems/k-closest-points-to-origin/) | Heap | Nearest-server selection |
| Todo | [1046. Last Stone Weight](https://leetcode.com/problems/last-stone-weight/) | Max Heap | Greedy resource consumption |
| Todo | [703. Kth Largest Element in a Stream](https://leetcode.com/problems/kth-largest-element-in-a-stream/) | Min Heap | Live ranking in event stream |
| Todo | [23. Merge k Sorted Lists](https://leetcode.com/problems/merge-k-sorted-lists/) | Heap | Merging sorted data partitions |

---

## Intervals

| Status | Problem | Pattern | Why Backend |
| --- | --- | --- | --- |
| Todo | [56. Merge Intervals](https://leetcode.com/problems/merge-intervals/) | Sort + Merge | Booking slot consolidation |
| Todo | [57. Insert Interval](https://leetcode.com/problems/insert-interval/) | Interval Insert | Schedule insertion |
| Todo | [435. Non-overlapping Intervals](https://leetcode.com/problems/non-overlapping-intervals/) | Greedy | Max non-conflicting jobs |
| Todo | [252. Meeting Rooms](https://leetcode.com/problems/meeting-rooms/) | Sort | Overlap detection |
| Todo | [253. Meeting Rooms II](https://leetcode.com/problems/meeting-rooms-ii/) | Heap / Sweep | Minimum server/room count |
| Todo | [986. Interval List Intersections](https://leetcode.com/problems/interval-list-intersections/) | Two Pointers | Overlapping schedule merging |
| Todo | [1851. Minimum Interval to Include Each Query](https://leetcode.com/problems/minimum-interval-to-include-each-query/) | Heap + Sorting | Range query optimization |

---

## Graph / BFS / DFS

| Status | Problem | Pattern | Why Backend |
| --- | --- | --- | --- |
| Todo | [200. Number of Islands](https://leetcode.com/problems/number-of-islands/) | DFS/BFS | Connected component detection |
| Todo | [994. Rotting Oranges](https://leetcode.com/problems/rotting-oranges/) | Multi-source BFS | Failure/event propagation |
| Todo | [207. Course Schedule](https://leetcode.com/problems/course-schedule/) | Topo Sort / Cycle | Circular dependency detection |
| Todo | [210. Course Schedule II](https://leetcode.com/problems/course-schedule-ii/) | Topological Sort | Build/deploy order |
| Todo | [743. Network Delay Time](https://leetcode.com/problems/network-delay-time/) | Dijkstra | Network latency routing |
| Todo | [695. Max Area of Island](https://leetcode.com/problems/max-area-of-island/) | DFS | Largest connected zone |
| Todo | [417. Pacific Atlantic Water Flow](https://leetcode.com/problems/pacific-atlantic-water-flow/) | Multi-source DFS | Reachability from multiple sources |
| Todo | [127. Word Ladder](https://leetcode.com/problems/word-ladder/) | BFS | Shortest transformation path |
| Todo | [269. Alien Dictionary](https://leetcode.com/problems/alien-dictionary/) | Topo Sort | Custom ordering / version ordering |
| Todo | [332. Reconstruct Itinerary](https://leetcode.com/problems/reconstruct-itinerary/) | Eulerian Path / DFS | Event replay ordering |
| Todo | [787. Cheapest Flights Within K Stops](https://leetcode.com/problems/cheapest-flights-within-k-stops/) | Bellman-Ford / BFS | Constrained shortest path |
| Todo | [1584. Min Cost to Connect All Points](https://leetcode.com/problems/min-cost-to-connect-all-points/) | Prim's / Kruskal | Minimum spanning network |

---

## Union Find

| Status | Problem | Pattern | Why Backend |
| --- | --- | --- | --- |
| Todo | [684. Redundant Connection](https://leetcode.com/problems/redundant-connection/) | Union Find | Circular dependency in graphs |
| Todo | [721. Accounts Merge](https://leetcode.com/problems/accounts-merge/) | Union Find | Identity resolution / user merging |
| Todo | [547. Number of Provinces](https://leetcode.com/problems/number-of-provinces/) | Union Find / DFS | Cluster counting |
| Todo | [130. Surrounded Regions](https://leetcode.com/problems/surrounded-regions/) | Union Find / DFS | Boundary-connected reachability |
| Todo | [323. Number of Connected Components in an Undirected Graph](https://leetcode.com/problems/number-of-connected-components-in-an-undirected-graph/) | Union Find | Component count in network |
| Todo | [1202. Smallest String With Swaps](https://leetcode.com/problems/smallest-string-with-swaps/) | Union Find | Grouping swappable elements |

---

## Trie

| Status | Problem | Pattern | Why Backend |
| --- | --- | --- | --- |
| Todo | [208. Implement Trie (Prefix Tree)](https://leetcode.com/problems/implement-trie-prefix-tree/) | Trie | Autocomplete, prefix search |
| Todo | [1268. Search Suggestions System](https://leetcode.com/problems/search-suggestions-system/) | Trie / Binary Search | Product search autocomplete |
| Todo | [212. Word Search II](https://leetcode.com/problems/word-search-ii/) | Trie + DFS | Multi-pattern search |

---

## Backtracking

| Status | Problem | Pattern | Why Backend |
| --- | --- | --- | --- |
| Todo | [78. Subsets](https://leetcode.com/problems/subsets/) | Backtracking | Power set, feature flag combinations |
| Todo | [90. Subsets II](https://leetcode.com/problems/subsets-ii/) | Backtracking + Dedup | Unique combinations |
| Todo | [46. Permutations](https://leetcode.com/problems/permutations/) | Backtracking | All orderings / request routing combos |
| Todo | [47. Permutations II](https://leetcode.com/problems/permutations-ii/) | Backtracking + Dedup | Unique orderings |
| Todo | [39. Combination Sum](https://leetcode.com/problems/combination-sum/) | Backtracking | Exact total coverage (pricing) |
| Todo | [40. Combination Sum II](https://leetcode.com/problems/combination-sum-ii/) | Backtracking + Dedup | Unique exact coverage |
| Todo | [131. Palindrome Partitioning](https://leetcode.com/problems/palindrome-partitioning/) | Backtracking + DP | All ways to split a string |
| Todo | [79. Word Search](https://leetcode.com/problems/word-search/) | DFS + Backtracking | Grid-based pattern matching |
| Todo | [17. Letter Combinations of a Phone Number](https://leetcode.com/problems/letter-combinations-of-a-phone-number/) | Backtracking | Cartesian product of sets |

---

## Dynamic Programming

| Status | Problem | Pattern | Why Backend |
| --- | --- | --- | --- |
| Todo | [70. Climbing Stairs](https://leetcode.com/problems/climbing-stairs/) | DP Basics | Recurrence, memoization |
| Todo | [198. House Robber](https://leetcode.com/problems/house-robber/) | DP | Skip/pick state transitions |
| Todo | [213. House Robber II](https://leetcode.com/problems/house-robber-ii/) | DP Circular | Circular array DP |
| Todo | [322. Coin Change](https://leetcode.com/problems/coin-change/) | DP | Minimum cost optimization |
| Todo | [518. Coin Change II](https://leetcode.com/problems/coin-change-ii/) | DP | Count ways to reach target |
| Todo | [300. Longest Increasing Subsequence](https://leetcode.com/problems/longest-increasing-subsequence/) | DP / Binary Search | Sequence optimization |
| Todo | [1143. Longest Common Subsequence](https://leetcode.com/problems/longest-common-subsequence/) | 2D DP | Diff-like comparison |
| Todo | [91. Decode Ways](https://leetcode.com/problems/decode-ways/) | DP | Protocol decoding, parsing |
| Todo | [139. Word Break](https://leetcode.com/problems/word-break/) | DP + HashSet | Valid token segmentation |
| Todo | [416. Partition Equal Subset Sum](https://leetcode.com/problems/partition-equal-subset-sum/) | 0/1 Knapsack | Resource partition |
| Todo | [494. Target Sum](https://leetcode.com/problems/target-sum/) | DP / DFS | Count of ways to reach target |
| Todo | [62. Unique Paths](https://leetcode.com/problems/unique-paths/) | DP Grid | Grid traversal counting |
| Todo | [63. Unique Paths II](https://leetcode.com/problems/unique-paths-ii/) | DP Grid + Obstacles | Blocked grid traversal |
| Todo | [5. Longest Palindromic Substring](https://leetcode.com/problems/longest-palindromic-substring/) | DP / Expand | String analysis |
| Todo | [647. Palindromic Substrings](https://leetcode.com/problems/palindromic-substrings/) | Expand Around Center | Substring enumeration |
| Todo | [312. Burst Balloons](https://leetcode.com/problems/burst-balloons/) | Interval DP | Optimal order of operations |
| Todo | [115. Distinct Subsequences](https://leetcode.com/problems/distinct-subsequences/) | 2D DP | Sequence matching count |

---

## Greedy

| Status | Problem | Pattern | Why Backend |
| --- | --- | --- | --- |
| Todo | [55. Jump Game](https://leetcode.com/problems/jump-game/) | Greedy | Reachability check |
| Todo | [45. Jump Game II](https://leetcode.com/problems/jump-game-ii/) | Greedy | Minimum hops to destination |
| Todo | [134. Gas Station](https://leetcode.com/problems/gas-station/) | Greedy | Circular resource loop viability |
| Todo | [135. Candy](https://leetcode.com/problems/candy/) | Greedy Two-pass | Fair distribution with constraints |
| Todo | [763. Partition Labels](https://leetcode.com/problems/partition-labels/) | Greedy | Partitioning by last occurrence |
| Todo | [678. Valid Parenthesis String](https://leetcode.com/problems/valid-parenthesis-string/) | Greedy Range | Range-based validation |
| Todo | [316. Remove Duplicate Letters](https://leetcode.com/problems/remove-duplicate-letters/) | Greedy + Stack | Lexicographically smallest unique result |

---

## Math / Bit Manipulation

| Status | Problem | Pattern | Why Backend |
| --- | --- | --- | --- |
| Todo | [191. Number of 1 Bits](https://leetcode.com/problems/number-of-1-bits/) | Bit Manipulation | Bit counting, flag checking |
| Todo | [338. Counting Bits](https://leetcode.com/problems/counting-bits/) | DP + Bits | Popcount table |
| Todo | [268. Missing Number](https://leetcode.com/problems/missing-number/) | XOR / Math | Data integrity checks |
| Todo | [371. Sum of Two Integers](https://leetcode.com/problems/sum-of-two-integers/) | Bit Manipulation | Arithmetic without operators |
| Todo | [136. Single Number](https://leetcode.com/problems/single-number/) | XOR | Detecting unique item in duplicates |
| Todo | [190. Reverse Bits](https://leetcode.com/problems/reverse-bits/) | Bit Manipulation | Bitwise operations |
| Todo | [7. Reverse Integer](https://leetcode.com/problems/reverse-integer/) | Math + Overflow | Integer overflow handling |
| Todo | [50. Pow(x, n)](https://leetcode.com/problems/powx-n/) | Fast Exponentiation | Efficient modular exponentiation |

---

## Design Problems

| Status | Problem | Pattern | Why Backend |
| --- | --- | --- | --- |
| Todo | [146. LRU Cache](https://leetcode.com/problems/lru-cache/) | HashMap + DLL | Cache eviction policy |
| Todo | [460. LFU Cache](https://leetcode.com/problems/lfu-cache/) | Multi-HashMap + DLL | Advanced eviction, Redis LFU |
| Todo | [355. Design Twitter](https://leetcode.com/problems/design-twitter/) | Heap + HashMap | Feed design, fan-out |
| Todo | [380. Insert Delete GetRandom O(1)](https://leetcode.com/problems/insert-delete-getrandom-o1/) | HashMap + Array | O(1) mutable index |
| Todo | [295. Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/) | Two Heaps | Streaming percentiles |
| Todo | [138. Copy List with Random Pointer](https://leetcode.com/problems/copy-list-with-random-pointer/) | HashMap | Deep copy object graph |
| Todo | [133. Clone Graph](https://leetcode.com/problems/clone-graph/) | DFS + HashMap | Graph deep copy |
| Todo | [705. Design HashSet](https://leetcode.com/problems/design-hashset/) | Chaining | Build a HashSet from scratch |
| Todo | [706. Design HashMap](https://leetcode.com/problems/design-hashmap/) | Chaining | Build a HashMap from scratch |
| Todo | [1472. Design Browser History](https://leetcode.com/problems/design-browser-history/) | Doubly Linked List / Array | Stack-based navigation |
| Todo | [622. Design Circular Queue](https://leetcode.com/problems/design-circular-queue/) | Ring Buffer | Circular buffer for event queues |
| Todo | [641. Design Circular Deque](https://leetcode.com/problems/design-circular-deque/) | Ring Buffer | Double-ended circular queue |

---

# Related Questions Per Problem

> **Rule:** If you can't answer these cold → mark `Redo`

---

## Array / Two Pointers

### [1. Two Sum](https://leetcode.com/problems/two-sum/)
**Conceptual**
- [ ] Why HashMap over nested loop? What's the trade-off?
- [ ] Average vs worst-case time complexity of HashMap lookup? Why?
- [ ] What causes hash collisions? How does Java's `HashMap` handle them (separate chaining → treeify at 8)?
- [ ] What is load factor? What happens when threshold is exceeded (rehash)?
- [ ] Why iterate once using complement check instead of two passes?

**Implementation**
- [ ] Can you solve with two pointers on sorted array? What changes?
- [ ] What if multiple valid pairs exist? Return all?
- [ ] Does your solution handle `[3,3]` with target `6`?

**Backend**
- [ ] How does Two Sum relate to cache lookup? (complement = "have I seen the other half?")
- [ ] Where is HashMap used in Spring Boot? (session maps, ID lookups, config stores)
- [ ] How does idempotency key deduplication work using a HashMap?
- [ ] How would you build O(1) user session lookup in Redis?

---

### [167. Two Sum II](https://leetcode.com/problems/two-sum-ii-input-array-is-sorted/)
**Conceptual**
- [ ] Why does two-pointer work on sorted arrays?
- [ ] What is the invariant — when do you move left vs right pointer?
- [ ] Time complexity: O(n). Space: O(1). Can you achieve both simultaneously?

**Backend**
- [ ] How does binary search on sorted data relate to B-tree index lookups?
- [ ] If user IDs are stored in a sorted array for quick membership check, how would you find a pair matching a sum?

---

### [15. 3Sum](https://leetcode.com/problems/3sum/)
**Conceptual**
- [ ] Why sort first? What does it enable?
- [ ] How do you skip duplicates for the fixed element AND the two-pointer scan?
- [ ] Time complexity: O(n²). Can you do better? (No — proven lower bound for this problem)

**Backend**
- [ ] How does deduplication logic here relate to deduplicating multi-key compound indexes?
- [ ] If you have three microservices whose latencies must sum to ≤ threshold, how does this model?

---

### [11. Container With Most Water](https://leetcode.com/problems/container-with-most-water/)
**Conceptual**
- [ ] Why move the shorter line's pointer? Prove correctness.
- [ ] What's the greedy argument here?
- [ ] Is there any way to do better than O(n)?

**Backend**
- [ ] How does maximizing area under constraints relate to capacity planning?
- [ ] How would you find the two servers with maximum combined throughput?

---

### [42. Trapping Rain Water](https://leetcode.com/problems/trapping-rain-water/)
**Conceptual**
- [ ] What's the brute force? O(n²)
- [ ] What's the precomputation approach (prefix max left, suffix max right)? O(n) space.
- [ ] What's the two-pointer O(1) space approach? How does it work?
- [ ] What's the monotonic stack approach?

**Backend**
- [ ] How does this relate to computing "available buffer space" between high-water marks in a queue?
- [ ] How does memory fragmentation visualization use this pattern?

---

### [75. Sort Colors (Dutch National Flag)](https://leetcode.com/problems/sort-colors/)
**Conceptual**
- [ ] What are the three pointers? What invariant does each maintain?
- [ ] Why is this O(n) with O(1) space?
- [ ] How does Dijkstra's 3-way partition differ from 2-way quicksort partition?

**Backend**
- [ ] How does 3-way partitioning relate to priority queues with 3 priority levels?
- [ ] How does Kafka message partitioning by priority use similar logic?

---

### [128. Longest Consecutive Sequence](https://leetcode.com/problems/longest-consecutive-sequence/)
**Conceptual**
- [ ] Why O(n) using HashSet? How do you avoid re-counting?
- [ ] Key insight: only start counting from sequence starts (num-1 not in set).
- [ ] Why not sort? (that would be O(n log n))

**Backend**
- [ ] How does gap detection in a sequence of IDs (missing records) relate to this?
- [ ] How would you find the longest unbroken chain of versions in a deployment history?

---

### [169. Majority Element](https://leetcode.com/problems/majority-element/)
**Conceptual**
- [ ] What is Boyer-Moore Voting? How does it work?
- [ ] Why does the majority element survive the cancellation process?
- [ ] Time: O(n), Space: O(1). Can you prove correctness?

**Backend**
- [ ] How does majority voting relate to distributed consensus (Raft, Paxos quorum)?
- [ ] How would you detect the most-voted option in a real-time poll with O(1) space?

---

### [238. Product of Array Except Self](https://leetcode.com/problems/product-of-array-except-self/)
**Conceptual**
- [ ] Why can't you just divide total product by each element?
- [ ] How do you compute prefix products (left) and suffix products (right)?
- [ ] Can you do it with O(1) extra space (excluding output)?

**Backend**
- [ ] How does this relate to computing running totals excluding current record in SQL (`SUM() OVER`)?
- [ ] How would you compute "all-except-one" aggregate in a distributed system?

---

### [560. Subarray Sum Equals K](https://leetcode.com/problems/subarray-sum-equals-k/)
**Conceptual**
- [ ] What is prefix sum? How does `prefixSum[j] - prefixSum[i] = k` work?
- [ ] Why store `count` of prefix sums (not just whether seen)?
- [ ] Why initialize map with `{0: 1}`?
- [ ] Does sliding window work here? (No — negative numbers break it)

**Backend**
- [ ] How does prefix sum relate to range query optimization in analytics?
- [ ] How does this apply to finding time windows where cumulative events equal a target?
- [ ] How do database window functions (`SUM OVER`) use prefix sums internally?

---

## HashMap / HashSet (Additional)

### [36. Valid Sudoku](https://leetcode.com/problems/valid-sudoku/)
**Conceptual**
- [ ] What three constraint sets must be valid? (rows, cols, 3×3 boxes)
- [ ] How do you compute box index from (row, col)? `boxIndex = (row/3)*3 + col/3`
- [ ] Single-pass: use 27 sets simultaneously

**Backend**
- [ ] How does multi-dimensional constraint validation relate to database check constraints?
- [ ] How would you validate a configuration object where keys must be unique across multiple dimensions?

---

### [525. Contiguous Array](https://leetcode.com/problems/contiguous-array/)
**Conceptual**
- [ ] How do you transform the problem? (0 → -1, find longest subarray with sum 0)
- [ ] How does prefix sum + HashMap solve it in O(n)?
- [ ] What do you store in the map? (prefix_sum → first_seen_index)

**Backend**
- [ ] How does balance detection in a binary event stream (0/1 events) relate to this?
- [ ] How would you detect the longest period of balanced read/write operations?

---

### [438. Find All Anagrams in a String](https://leetcode.com/problems/find-all-anagrams-in-a-string/)
**Conceptual**
- [ ] Fixed window size = len(p). Slide over s.
- [ ] Compare frequency arrays efficiently — what's the comparison cost?
- [ ] Optimize by tracking `matches` counter instead of full array comparison.

**Backend**
- [ ] How does fixed-window pattern matching apply to log parsing (find all occurrences of a pattern)?
- [ ] How would you find all time windows in a metric stream matching a specific signature?

---

### [290. Word Pattern](https://leetcode.com/problems/word-pattern/)
**Conceptual**
- [ ] Why do you need bijective mapping (both directions)?
- [ ] What happens if you only check one direction? (fails: "aab" vs "dog dog dog")

**Backend**
- [ ] How does bidirectional mapping relate to ORM bidirectional relationships?
- [ ] How does config key-alias validation use bijective mapping?

---

## Sliding Window (Additional)

### [76. Minimum Window Substring](https://leetcode.com/problems/minimum-window-substring/)
**Conceptual**
- [ ] What is the `have` vs `need` counter approach?
- [ ] When do you shrink the left pointer?
- [ ] Time: O(n + m). Why not O(n*m)?

**Backend**
- [ ] How does minimum window relate to finding the smallest log segment containing all required error codes?
- [ ] How would you find the shortest time period containing all required event types?

---

### [424. Longest Repeating Character Replacement](https://leetcode.com/problems/longest-repeating-character-replacement/)
**Conceptual**
- [ ] Key insight: `windowLen - maxFreqChar <= k` → window is valid.
- [ ] Why does max frequency only need to be tracked (not decremented when shrinking)?
- [ ] Why is this O(n)?

**Backend**
- [ ] How does "at most K mutations" relate to schema migration tolerance (allow K field renames)?
- [ ] How does this apply to finding the longest valid token window with at most K exceptions?

---

## Stack (Additional)

### [84. Largest Rectangle in Histogram](https://leetcode.com/problems/largest-rectangle-in-histogram/)
**Conceptual**
- [ ] What does the monotonic stack store? (indices of bars in increasing order)
- [ ] When do you pop and compute area?
- [ ] How do sentinel values (0 at start and end) simplify edge cases?
- [ ] Time: O(n). Every bar is pushed/popped exactly once.

**Backend**
- [ ] How does largest rectangle relate to maximum throughput window in a capacity histogram?
- [ ] How would you find the largest uninterrupted time block in a server utilization chart?

---

### [150. Evaluate Reverse Polish Notation](https://leetcode.com/problems/evaluate-reverse-polish-notation/)
**Conceptual**
- [ ] What's RPN? How does postfix avoid parentheses?
- [ ] Stack-based evaluation: push operands, pop on operator.
- [ ] Integer division in Java/Python: how does truncation toward zero work?

**Backend**
- [ ] How do expression evaluators (formula engines, rule engines) use RPN internally?
- [ ] How does a stack-based JVM bytecode interpreter work?
- [ ] How does a database query planner evaluate expressions?

---

### [394. Decode String](https://leetcode.com/problems/decode-string/)
**Conceptual**
- [ ] What two stacks do you maintain? (counts and strings)
- [ ] When do you push/pop from each?
- [ ] Handle multi-digit counts: build number before `[`.

**Backend**
- [ ] How does nested template expansion (e.g., Helm templates, Jinja) use similar stack logic?
- [ ] How would you decode a nested config format like `3[ab2[cd]]`?

---

### [853. Car Fleet](https://leetcode.com/problems/car-fleet/)
**Conceptual**
- [ ] Sort by position descending. Compute arrival time for each car.
- [ ] Monotonic stack: if current car's time ≤ top of stack → same fleet.
- [ ] What does the stack represent? (distinct fleet arrival times)

**Backend**
- [ ] How does fleet grouping relate to batching jobs that complete together?
- [ ] How would you group deliveries that arrive at the same time given different start positions?

---

## Binary Search (Additional)

### [153. Find Minimum in Rotated Sorted Array](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/)
**Conceptual**
- [ ] How do you determine which half to search?
- [ ] When is `mid` the minimum?
- [ ] What's the termination condition?

**Backend**
- [ ] How does finding the rotation pivot relate to finding the start of a circular log buffer?

---

### [875. Koko Eating Bananas](https://leetcode.com/problems/koko-eating-bananas/)
**Conceptual**
- [ ] What is "binary search on the answer"? What property must the answer space have? (monotonic feasibility)
- [ ] What is the feasibility check function?
- [ ] lo and hi bounds: `1` and `max(piles)`.

**Backend**
- [ ] How does binary search on rate/capacity apply to finding minimum throughput required?
- [ ] How would you binary search to find the minimum batch size that processes all jobs within SLA?
- [ ] How does this relate to capacity planning (minimum instance count to handle load in T time)?

---

### [1011. Capacity to Ship Packages](https://leetcode.com/problems/capacity-to-ship-packages-within-d-days/)
**Conceptual**
- [ ] lo = max(weights), hi = sum(weights). Why?
- [ ] Feasibility check: simulate greedy packing.
- [ ] Why is the answer space monotonic?

**Backend**
- [ ] Direct mapping to: "minimum bandwidth to transfer all data in D days"
- [ ] How would you binary search for minimum worker count to process a task queue in time T?

---

### [410. Split Array Largest Sum](https://leetcode.com/problems/split-array-largest-sum/)
**Conceptual**
- [ ] Same template as 1011. lo = max(arr), hi = sum(arr).
- [ ] Feasibility: can you split into ≤ k subarrays each ≤ mid?
- [ ] This is minimax — minimize the maximum subarray sum.

**Backend**
- [ ] How does this map to load balancing: split work across k workers to minimize max worker load?
- [ ] How would you optimally shard data across k database nodes to minimize max shard size?

---

## Linked List

### [206. Reverse Linked List](https://leetcode.com/problems/reverse-linked-list/)
**Conceptual**
- [ ] Iterative with 3 pointers (prev, curr, next). Walk through step-by-step.
- [ ] Recursive approach. What's the base case? What's the recursive call?
- [ ] Space: O(1) iterative, O(n) recursive (call stack).

**Backend**
- [ ] Where does pointer reversal appear in real systems? (reversing log entry chains, rollback chains)
- [ ] How does reversing a singly linked list relate to undoing a transaction log?

---

### [21. Merge Two Sorted Lists](https://leetcode.com/problems/merge-two-sorted-lists/)
**Conceptual**
- [ ] Dummy head trick: why does it simplify the code?
- [ ] Iterative vs recursive. Which is safer for large inputs? (iterative — no stack overflow)

**Backend**
- [ ] How does this relate to merging two sorted event streams in real-time?
- [ ] How does a merge step in merge-sort work? (same logic)
- [ ] How does PostgreSQL merge join algorithm work?

---

### [23. Merge k Sorted Lists](https://leetcode.com/problems/merge-k-sorted-lists/)
**Conceptual**
- [ ] Min-heap of size k: store (val, listIndex, node).
- [ ] Time: O(n log k) where n = total nodes.
- [ ] Why not merge two at a time repeatedly? (O(nk) — worse)

**Backend**
- [ ] How does merging k sorted partitions relate to external sort in databases?
- [ ] How does Elasticsearch merge sorted posting lists during query execution?
- [ ] How does a distributed sort (MapReduce) use k-way merge in the reduce phase?

---

### [141. Linked List Cycle](https://leetcode.com/problems/linked-list-cycle/)
**Conceptual**
- [ ] Floyd's cycle detection: fast moves 2, slow moves 1. When do they meet?
- [ ] Why does fast pointer catch slow if there's a cycle?
- [ ] Space: O(1). Alternative: HashSet of visited nodes — O(n) space.

**Backend**
- [ ] How does cycle detection apply to detecting infinite redirect loops in HTTP?
- [ ] How would you detect a circular dependency in a runtime object graph?
- [ ] How does garbage collection (mark-and-sweep) handle cyclic references?

---

### [142. Linked List Cycle II](https://leetcode.com/problems/linked-list-cycle-ii/)
**Conceptual**
- [ ] After fast/slow meet inside cycle: why does resetting one to head and moving both at speed 1 lead to cycle entry?
- [ ] Prove the math: let `F` = distance to cycle start, `C` = cycle length, `a` = position inside cycle at meeting point. Prove `F = C - a`.

**Backend**
- [ ] How does finding cycle entry relate to finding where a circular import begins?
- [ ] In memory leak detection, how does the "first recurring allocation point" correspond to cycle entry?

---

### [19. Remove Nth Node From End](https://leetcode.com/problems/remove-nth-node-from-end-of-list/)
**Conceptual**
- [ ] Two pointers: fast moves n+1 steps ahead. When fast reaches null, slow is at (n+1)th from end.
- [ ] Why n+1 and not n? (you need to stop at node before the target)
- [ ] One-pass, O(1) space.

**Backend**
- [ ] How does this "two-pointer with fixed gap" pattern relate to sliding window monitoring?
- [ ] How would you remove the Nth-most-recent event from a fixed-size event buffer?

---

### [143. Reorder List](https://leetcode.com/problems/reorder-list/)
**Conceptual**
- [ ] Three steps: (1) find mid, (2) reverse second half, (3) merge two halves alternately.
- [ ] Why find mid first? What happens if list has even length?
- [ ] Space: O(1).

**Backend**
- [ ] Multi-step pointer manipulation: how does this pattern appear in in-place data restructuring?

---

### [2. Add Two Numbers](https://leetcode.com/problems/add-two-numbers/)
**Conceptual**
- [ ] Simulate grade-school addition with carry.
- [ ] Handle different list lengths and final carry.
- [ ] Does Java's `int` overflow matter here? Why not?

**Backend**
- [ ] How does arbitrary-precision arithmetic (BigInteger, BigDecimal) use similar carry propagation?
- [ ] How does financial transaction amount handling avoid floating-point errors?

---

### [25. Reverse Nodes in k-Group](https://leetcode.com/problems/reverse-nodes-in-k-group/)
**Conceptual**
- [ ] How do you check if k nodes remain before reversing?
- [ ] Reverse k nodes iteratively. How to track group connections?
- [ ] Time: O(n). Space: O(1).

**Backend**
- [ ] How does batch processing in fixed-size chunks (micro-batching) relate to this?
- [ ] How does Kafka's batch.size config relate to processing k records at a time?

---

## Trees (Additional)

### [199. Binary Tree Right Side View](https://leetcode.com/problems/binary-tree-right-side-view/)
**Conceptual**
- [ ] BFS level-order: take the last node of each level.
- [ ] DFS alternative: visit right child first, record first visit per depth.

**Backend**
- [ ] How does "rightmost visible node per level" relate to showing the latest state in a versioned tree?

---

### [543. Diameter of Binary Tree](https://leetcode.com/problems/diameter-of-binary-tree/)
**Conceptual**
- [ ] Diameter at each node = left depth + right depth.
- [ ] Track global max during DFS post-order.
- [ ] The diameter may not pass through root.

**Backend**
- [ ] How does network diameter computation apply to finding the longest path in a service dependency graph?
- [ ] How would you compute the maximum latency path across a microservice call tree?

---

### [572. Subtree of Another Tree](https://leetcode.com/problems/subtree-of-another-tree/)
**Conceptual**
- [ ] For each node in main tree, check if subtree matches `subRoot`.
- [ ] `isSameTree` helper: O(n*m) overall. Can you do better with tree hashing?

**Backend**
- [ ] How does subtree matching relate to detecting matching sub-configurations in a nested config tree?

---

### [105. Construct Binary Tree from Preorder and Inorder](https://leetcode.com/problems/construct-binary-tree-from-preorder-and-inorder-traversal/)
**Conceptual**
- [ ] Preorder[0] = root. Find root in inorder to split left/right subtrees.
- [ ] Recursive construction. Use HashMap for O(1) inorder lookup.
- [ ] Time: O(n). Space: O(n) for HashMap + recursion stack.

**Backend**
- [ ] How does tree reconstruction from serialized data relate to deserializing nested JSON into a tree?

---

### [297. Serialize and Deserialize Binary Tree](https://leetcode.com/problems/serialize-and-deserialize-binary-tree/)
**Conceptual**
- [ ] BFS serialization: level-order with nulls.
- [ ] DFS serialization: preorder with null markers.
- [ ] Handle null nodes explicitly to enable faithful reconstruction.

**Backend**
- [ ] How does binary tree serialization relate to storing tree structures in a database?
- [ ] How does adjacency list vs nested set model store trees in SQL?
- [ ] How does Protocol Buffers serialize nested message structures?
- [ ] How does Redis store tree-like data? (no native tree — use Hash + sorted sets or JSON)

---

### [124. Binary Tree Maximum Path Sum](https://leetcode.com/problems/binary-tree-maximum-path-sum/)
**Conceptual**
- [ ] For each node: max gain including node = node.val + max(0, leftGain) + max(0, rightGain)
- [ ] Max path through node = leftGain + node.val + rightGain (cannot fork on the way up)
- [ ] DFS post-order, global max update.

**Backend**
- [ ] How does maximum path sum relate to finding the most profitable route through a transaction graph?
- [ ] How does critical path analysis in project scheduling use similar DFS?

---

### [110. Balanced Binary Tree](https://leetcode.com/problems/balanced-binary-tree/)
**Conceptual**
- [ ] Balanced: every node's left/right subtree heights differ by ≤ 1.
- [ ] Bottom-up DFS: return -1 as sentinel for "unbalanced".
- [ ] Avoid computing height redundantly (top-down is O(n log n), bottom-up is O(n)).

**Backend**
- [ ] How does balance validation relate to checking if a B-tree is balanced after insertions?
- [ ] How does load balancer validation check if cluster nodes are balanced?

---

## Heap / Priority Queue (Additional)

### [973. K Closest Points to Origin](https://leetcode.com/problems/k-closest-points-to-origin/)
**Conceptual**
- [ ] Use max-heap of size k. Evict farthest when heap exceeds k.
- [ ] Or quickselect O(n) average.
- [ ] No need to sort — heap gives top k in O(n log k).

**Backend**
- [ ] How does k-nearest-neighbor relate to geospatial nearest-server selection?
- [ ] How does PostGIS use spatial indexes for nearest neighbor queries?

---

### [703. Kth Largest Element in a Stream](https://leetcode.com/problems/kth-largest-element-in-a-stream/)
**Conceptual**
- [ ] Min-heap of size k. Root = kth largest.
- [ ] On `add(val)`: push, pop if heap > k.
- [ ] Why min-heap? (smallest of top k = kth largest)

**Backend**
- [ ] How does live leaderboard maintenance use a min-heap of size k?
- [ ] How would you maintain top-100 products by sales in a streaming pipeline?

---

### [253. Meeting Rooms II](https://leetcode.com/problems/meeting-rooms-ii/)
**Conceptual**
- [ ] Sort by start. Min-heap of end times.
- [ ] If current start ≥ heap top (earliest ending room) → reuse room, else add room.
- [ ] Heap size at end = minimum rooms needed.

**Backend**
- [ ] How does minimum room count relate to minimum thread pool size for concurrent requests?
- [ ] How does Kubernetes pod scheduling compute minimum node count for a batch of jobs?
- [ ] How does connection pool sizing use similar logic?

---

## Intervals (Additional)

### [252. Meeting Rooms](https://leetcode.com/problems/meeting-rooms/)
**Conceptual**
- [ ] Sort by start. Check adjacent pairs for overlap.
- [ ] Overlap condition: `intervals[i].start < intervals[i-1].end`

**Backend**
- [ ] How does overlap detection apply to detecting conflicting database locks?
- [ ] How would you validate that a cron job schedule has no overlapping runs?

---

### [986. Interval List Intersections](https://leetcode.com/problems/interval-list-intersections/)
**Conceptual**
- [ ] Two pointers on both lists. Intersection: `[max(a.start, b.start), min(a.end, b.end)]` if valid.
- [ ] Advance the pointer with smaller end.

**Backend**
- [ ] How does schedule intersection apply to finding common availability windows for two users?
- [ ] How does this relate to database range lock intersection detection?

---

### [1851. Minimum Interval to Include Each Query](https://leetcode.com/problems/minimum-interval-to-include-each-query/)
**Conceptual**
- [ ] Sort intervals by start, queries by value (offline processing).
- [ ] Min-heap of (size, end). Add intervals whose start ≤ query. Remove expired.

**Backend**
- [ ] How does offline query processing with sorting relate to query optimization with sorted indexes?

---

## Graph / BFS / DFS (Additional)

### [210. Course Schedule II](https://leetcode.com/problems/course-schedule-ii/)
**Conceptual**
- [ ] Topological sort output order. DFS post-order reversal or Kahn's BFS.
- [ ] Return empty if cycle detected.

**Backend**
- [ ] How does topo sort order map to microservice startup sequence (service A depends on B)?
- [ ] How does Gradle's task dependency resolution compute execution order?
- [ ] How does Kubernetes init container ordering use dependency ordering?

---

### [695. Max Area of Island](https://leetcode.com/problems/max-area-of-island/)
**Conceptual**
- [ ] DFS/BFS counting cells in each island.
- [ ] Mark visited by setting to 0 (in-place) or using visited set.

**Backend**
- [ ] How does largest connected zone relate to largest service cluster or geographic delivery zone?

---

### [417. Pacific Atlantic Water Flow](https://leetcode.com/problems/pacific-atlantic-water-flow/)
**Conceptual**
- [ ] Reverse thinking: BFS/DFS from both oceans inward (reverse flow).
- [ ] Find cells reachable from both Pacific and Atlantic.

**Backend**
- [ ] How does multi-source reachability relate to finding servers reachable from two different network segments?

---

### [127. Word Ladder](https://leetcode.com/problems/word-ladder/)
**Conceptual**
- [ ] BFS for shortest path. Each word = node, edge = 1 character change.
- [ ] Pre-build adjacency using wildcard patterns: `h*t`, `ho*`.
- [ ] Bidirectional BFS optimization.

**Backend**
- [ ] How does shortest transformation path relate to computing minimum schema migration steps?
- [ ] How does edit distance (Levenshtein) relate to word ladder?

---

### [269. Alien Dictionary](https://leetcode.com/problems/alien-dictionary/)
**Conceptual**
- [ ] Extract ordering from adjacent word pairs: compare first differing characters.
- [ ] Build directed graph → topological sort.
- [ ] Edge cases: same word prefix but longer word comes first → invalid.

**Backend**
- [ ] How does custom character ordering relate to custom sort key derivation in databases?
- [ ] How would you determine API version precedence from a sorted version list?

---

### [332. Reconstruct Itinerary](https://leetcode.com/problems/reconstruct-itinerary/)
**Conceptual**
- [ ] Hierholzer's algorithm: Eulerian path (uses every edge exactly once).
- [ ] DFS with backtracking, build result in reverse (post-order).
- [ ] Sort adjacency lists lexicographically.

**Backend**
- [ ] How does Eulerian path relate to replaying a sequence of events where each event consumes the previous?
- [ ] How does message queue replay with ordering constraints use similar logic?

---

### [787. Cheapest Flights Within K Stops](https://leetcode.com/problems/cheapest-flights-within-k-stops/)
**Conceptual**
- [ ] Bellman-Ford with k+1 relaxation rounds.
- [ ] Modified Dijkstra with state = (cost, node, stops).
- [ ] Why standard Dijkstra fails here? (greedy shortest path ignores stop constraint)

**Backend**
- [ ] How does constrained shortest path relate to routing with hop limits?
- [ ] How does TTL (Time To Live) in IP routing limit hops?

---

### [1584. Min Cost to Connect All Points](https://leetcode.com/problems/min-cost-to-connect-all-points/)
**Conceptual**
- [ ] Minimum Spanning Tree: Prim's (heap-based) or Kruskal's (Union Find).
- [ ] All pairs are edges → dense graph. Prim's is O(n²) or O(n² log n).
- [ ] Cost = Manhattan distance.

**Backend**
- [ ] How does MST relate to minimum network infrastructure cost to connect all servers?
- [ ] How does Prim's algorithm compare to Kruskal's for dense vs sparse graphs?

---

## Union Find (Additional)

### [547. Number of Provinces](https://leetcode.com/problems/number-of-provinces/)
**Conceptual**
- [ ] Union all directly connected cities. Count distinct roots.
- [ ] Same as Number of Islands on adjacency matrix.

**Backend**
- [ ] How does province/cluster counting relate to counting isolated microservice groups?

---

### [130. Surrounded Regions](https://leetcode.com/problems/surrounded-regions/)
**Conceptual**
- [ ] Mark all 'O' connected to borders as safe. Flip remaining 'O' to 'X'.
- [ ] Union Find: connect border 'O's to a virtual node.

**Backend**
- [ ] How does boundary-connected reachability relate to firewall zone isolation?

---

### [323. Number of Connected Components](https://leetcode.com/problems/number-of-connected-components-in-an-undirected-graph/)
**Conceptual**
- [ ] Union Find or DFS. Count number of distinct components.
- [ ] Union Find: count nodes whose parent is themselves.

**Backend**
- [ ] How does component count relate to number of independent service clusters?
- [ ] How would you detect how many isolated partitions exist in a network after failures?

---

### [1202. Smallest String With Swaps](https://leetcode.com/problems/smallest-string-with-swaps/)
**Conceptual**
- [ ] Union Find all swappable indices into groups.
- [ ] Sort characters within each group, place smallest first.

**Backend**
- [ ] How does grouping by swap connectivity relate to grouping data that can be freely reorganized?

---

## Backtracking

### [78. Subsets](https://leetcode.com/problems/subsets/)
**Conceptual**
- [ ] Two approaches: iterative (add each element to existing subsets) or backtracking.
- [ ] Backtracking: at each step, include or exclude current element.
- [ ] Total subsets: 2^n. Time: O(n * 2^n).

**Backend**
- [ ] How does power set generation relate to feature flag combination testing?
- [ ] How would you generate all subsets of optional API query parameters?

---

### [46. Permutations](https://leetcode.com/problems/permutations/)
**Conceptual**
- [ ] Swap-based backtracking: swap element into current position, recurse, swap back.
- [ ] Track used elements with boolean array.
- [ ] Total permutations: n!. Time: O(n * n!).

**Backend**
- [ ] How does permutation generation relate to testing all orderings of operations?
- [ ] How would you generate all possible request orderings for concurrency testing?

---

### [39. Combination Sum](https://leetcode.com/problems/combination-sum/)
**Conceptual**
- [ ] Candidates can be reused. Start DFS from current index (not i+1).
- [ ] Pruning: sort candidates, break if candidate > remaining target.

**Backend**
- [ ] How does combination sum relate to finding all sets of discount tiers that sum to a target price?
- [ ] How would you find all combinations of microservices whose latencies sum to ≤ SLA budget?

---

### [131. Palindrome Partitioning](https://leetcode.com/problems/palindrome-partitioning/)
**Conceptual**
- [ ] Backtracking: at each position, try all palindromic prefixes.
- [ ] Precompute palindrome check with DP: `dp[i][j]` = is s[i..j] palindrome.

**Backend**
- [ ] How does partitioning into valid segments relate to tokenizing a DSL string into valid tokens?

---

### [17. Letter Combinations of a Phone Number](https://leetcode.com/problems/letter-combinations-of-a-phone-number/)
**Conceptual**
- [ ] Cartesian product of character sets per digit.
- [ ] Backtracking: build combination character by character.

**Backend**
- [ ] How does Cartesian product generation relate to generating all combinations of environment × region × tier?
- [ ] How would you generate all possible config variations for A/B testing?

---

## Dynamic Programming (Additional)

### [213. House Robber II](https://leetcode.com/problems/house-robber-ii/)
**Conceptual**
- [ ] Circular array: can't take both first and last.
- [ ] Run House Robber I twice: once excluding first, once excluding last.
- [ ] Take the max of both results.

**Backend**
- [ ] How does circular constraint DP relate to scheduling in circular pipelines?

---

### [91. Decode Ways](https://leetcode.com/problems/decode-ways/)
**Conceptual**
- [ ] `dp[i]` = number of ways to decode s[0..i-1].
- [ ] Transition: single digit (1-9) + double digit (10-26).
- [ ] Handle leading zeros.

**Backend**
- [ ] How does counting decode ways relate to counting valid parse paths for an ambiguous protocol message?

---

### [139. Word Break](https://leetcode.com/problems/word-break/)
**Conceptual**
- [ ] `dp[i]` = can s[0..i-1] be segmented.
- [ ] For each i, try all j < i: if dp[j] and s[j..i] in wordSet → dp[i] = true.
- [ ] Time: O(n² * m) where m = avg word length.

**Backend**
- [ ] How does word break relate to validating that a URL path can be segmented into valid route tokens?
- [ ] How does token segmentation in NLP relate to API endpoint tokenization?

---

### [416. Partition Equal Subset Sum](https://leetcode.com/problems/partition-equal-subset-sum/)
**Conceptual**
- [ ] 0/1 Knapsack: can you pick elements summing to total/2?
- [ ] `dp[j]` = can we achieve sum j using available elements.
- [ ] Iterate backwards to avoid reusing elements.

**Backend**
- [ ] How does partition equal subset relate to splitting work equally across two workers?
- [ ] How would you check if a dataset can be split into two equal-size balanced shards?

---

### [518. Coin Change II](https://leetcode.com/problems/coin-change-ii/)
**Conceptual**
- [ ] Count ways (not minimum). Unbounded knapsack.
- [ ] `dp[j] += dp[j - coin]` for each coin.
- [ ] Order matters: iterate coins in outer loop for combinations (not permutations).

**Backend**
- [ ] How does counting ways relate to counting the number of valid payment combinations?

---

### [62. Unique Paths](https://leetcode.com/problems/unique-paths/)
**Conceptual**
- [ ] `dp[i][j] = dp[i-1][j] + dp[i][j-1]`.
- [ ] Optimized to O(n) space using 1D DP.
- [ ] Mathematical formula: C(m+n-2, m-1).

**Backend**
- [ ] How does grid path counting relate to counting valid routing paths in a network grid?

---

### [5. Longest Palindromic Substring](https://leetcode.com/problems/longest-palindromic-substring/)
**Conceptual**
- [ ] Expand around center: 2n-1 centers (odd + even length).
- [ ] DP: `dp[i][j]` = is s[i..j] palindrome. O(n²) time and space.
- [ ] Manacher's algorithm: O(n) — understand the concept.

**Backend**
- [ ] How does palindrome detection relate to detecting symmetric patterns in data streams?

---

### [312. Burst Balloons](https://leetcode.com/problems/burst-balloons/)
**Conceptual**
- [ ] Interval DP: think about which balloon to burst LAST in range [l, r].
- [ ] `dp[l][r]` = max coins from bursting all balloons in (l, r).
- [ ] Add virtual balloons of value 1 at boundaries.

**Backend**
- [ ] How does "optimal order of operations" DP relate to query optimization (join order selection)?
- [ ] How does a database query planner decide the optimal join order?

---

## Greedy (Additional)

### [55. Jump Game](https://leetcode.com/problems/jump-game/)
**Conceptual**
- [ ] Track max reachable index. If current index > maxReach → can't proceed.
- [ ] O(n) greedy. No backtracking needed.

**Backend**
- [ ] How does reachability greedy relate to checking if a distributed pipeline can complete given resource constraints?

---

### [45. Jump Game II](https://leetcode.com/problems/jump-game-ii/)
**Conceptual**
- [ ] Track current range end and farthest reachable.
- [ ] Jump count increments when you exhaust current range.
- [ ] O(n) BFS-like greedy.

**Backend**
- [ ] How does minimum hops relate to minimum number of service calls to reach a destination?

---

### [134. Gas Station](https://leetcode.com/problems/gas-station/)
**Conceptual**
- [ ] If total gas ≥ total cost → solution exists (and is unique).
- [ ] Track running sum; reset start when it goes negative.
- [ ] O(n) single pass.

**Backend**
- [ ] How does circular resource loop viability relate to detecting if a circular job pipeline is self-sustaining?

---

### [763. Partition Labels](https://leetcode.com/problems/partition-labels/)
**Conceptual**
- [ ] Record last occurrence of each character.
- [ ] Greedily extend current partition to include all occurrences of seen characters.

**Backend**
- [ ] How does partitioning by last occurrence relate to partitioning a log file such that each partition contains complete records for all mentioned entities?

---

## Math / Bit Manipulation

### [191. Number of 1 Bits](https://leetcode.com/problems/number-of-1-bits/)
**Conceptual**
- [ ] `n & (n-1)` trick: clears the lowest set bit. Count operations until n = 0.
- [ ] `Integer.bitCount()` in Java. Know what it does.

**Backend**
- [ ] How does bit counting relate to permission flag checking (bitmask permissions)?
- [ ] How does Java's `EnumSet` use bitmasking internally?

---

### [338. Counting Bits](https://leetcode.com/problems/counting-bits/)
**Conceptual**
- [ ] `dp[i] = dp[i >> 1] + (i & 1)`. O(n).
- [ ] Or: `dp[i] = dp[i & (i-1)] + 1`.

**Backend**
- [ ] How does precomputed popcount table relate to Bloom filter implementation?

---

### [268. Missing Number](https://leetcode.com/problems/missing-number/)
**Conceptual**
- [ ] Math: expected sum = n*(n+1)/2 − actual sum.
- [ ] XOR: XOR all indices 0..n with all values. Missing number remains.

**Backend**
- [ ] How does missing number detection relate to data integrity checks (finding missing IDs)?
- [ ] How would you detect a missing record in a sequence of auto-increment IDs?

---

### [136. Single Number](https://leetcode.com/problems/single-number/)
**Conceptual**
- [ ] XOR: x ^ x = 0, x ^ 0 = x. XOR all elements → non-duplicate remains.
- [ ] O(n) time, O(1) space.

**Backend**
- [ ] How does XOR deduplication relate to finding the unique event in an event stream where all others appear twice?

---

### [50. Pow(x, n)](https://leetcode.com/problems/powx-n/)
**Conceptual**
- [ ] Fast exponentiation: `x^n = (x^(n/2))^2`. O(log n).
- [ ] Handle negative n: `x^(-n) = 1 / x^n`.
- [ ] Handle n = INT_MIN carefully (overflow).

**Backend**
- [ ] How does modular exponentiation relate to RSA encryption? (compute `a^b mod m`)
- [ ] How does fast power relate to computing hash functions efficiently?

---

## Design Problems (Additional)

### [705. Design HashSet](https://leetcode.com/problems/design-hashset/)
**Conceptual**
- [ ] Array of buckets (linked lists for chaining) or open addressing.
- [ ] Hash function: `key % bucketCount`.
- [ ] Handle collisions.

**Backend**
- [ ] What is separate chaining vs open addressing? Trade-offs?
- [ ] How does Java's `HashSet` / `HashMap` implementation work internally?
- [ ] What is the default initial capacity and load factor in Java's HashMap?

---

### [706. Design HashMap](https://leetcode.com/problems/design-hashmap/)
**Conceptual**
- [ ] Extend HashSet design to store (key, value) pairs.
- [ ] Collision: chain of (key, val) pairs in each bucket.

**Backend**
- [ ] How does HashMap's `put`, `get`, `remove` work at O(1) average?
- [ ] What triggers rehashing in Java's HashMap? What's the new capacity?

---

### [1472. Design Browser History](https://leetcode.com/problems/design-browser-history/)
**Conceptual**
- [ ] Array with pointer: `visit` truncates forward history. `back`/`forward` clamp to bounds.
- [ ] Or doubly linked list.

**Backend**
- [ ] How does browser history stack relate to undo/redo in a collaborative editor?
- [ ] How does command pattern implement undo/redo?

---

### [622. Design Circular Queue](https://leetcode.com/problems/design-circular-queue/)
**Conceptual**
- [ ] Array with head and tail pointers, modulo arithmetic.
- [ ] Track size separately or distinguish full vs empty by capacity.

**Backend**
- [ ] How does ring buffer relate to Disruptor (LMAX) high-performance event queue?
- [ ] How does Linux kernel's circular buffer for I/O use the same pattern?
- [ ] How does Kafka's log segment act as an infinite circular log?

---

### [641. Design Circular Deque](https://leetcode.com/problems/design-circular-deque/)
**Conceptual**
- [ ] Extend circular queue to support front/back insertion and deletion.
- [ ] Used in sliding window maximum (monotonic deque).

**Backend**
- [ ] Where is a deque used in real backend systems? (work-stealing queues in thread pools — Java's ForkJoinPool)

---

# Bonus: Backend-Only Patterns (No Direct LeetCode Problem)

## Rate Limiting
- [ ] Implement **Fixed Window** counter (HashMap<windowId, count> + TTL)
- [ ] Implement **Sliding Window Log** (TreeMap of timestamps, evict old)
- [ ] Implement **Token Bucket** (tokens + lastRefillTime, refill on access)
- [ ] Implement **Leaky Bucket** (queue drain at fixed rate)
- [ ] Implement **Sliding Window Counter** (use two fixed windows with interpolation)
- [ ] How does Redis implement rate limiting? (`INCR` + `EXPIRE` for fixed window, `ZADD` + `ZRANGEBYSCORE` for sliding log)
- [ ] What are the trade-offs of each rate limiter type?
- [ ] How does Nginx rate limiting use leaky bucket? (`limit_req_zone`)

## Consistent Hashing
- [ ] What problem does consistent hashing solve over modulo hashing?
- [ ] How do virtual nodes improve distribution?
- [ ] How does adding/removing a node affect key redistribution? (only 1/n keys move)
- [ ] How does Cassandra use consistent hashing for partition placement?
- [ ] How does Redis Cluster use hash slots (0–16383)?

## Bloom Filter
- [ ] What is a Bloom filter? False positive vs false negative?
- [ ] What is the false positive rate formula? `(1 - e^(-kn/m))^k`
- [ ] How does Cassandra use Bloom filters to avoid disk reads for non-existent rows?
- [ ] How does Chrome's safe browsing use a Bloom filter?
- [ ] Where does the `RedisBloom` module expose Bloom filter commands?

## Distributed Lock
- [ ] How do you implement a distributed lock with Redis? (`SET key val NX PX ttl`)
- [ ] What is Redlock? Why is it controversial? (Martin Kleppmann's critique)
- [ ] What is a fencing token? How does it prevent split-brain?
- [ ] When would you use a distributed lock vs optimistic concurrency (version field)?

## Count-Min Sketch (Approximate Top-K)
- [ ] How does Count-Min Sketch estimate frequency?
- [ ] What's the trade-off vs exact HashMap frequency counting?
- [ ] Where is it used? (Twitter trending topics, Flink, Spark streaming)

---

# Review Template

```text
Problem:
Link:
Date solved:
Time taken:
Pattern:
Core invariant:
Time complexity:
Space complexity:
Backend connection:
Key mistake I made:
Edge cases I missed:
Need to redo? yes / no
Confidence (1–5):
```

---

*Tags: #DSA #LeetCode #BackendEngineering #Java #SpringBoot #SystemDesign*
