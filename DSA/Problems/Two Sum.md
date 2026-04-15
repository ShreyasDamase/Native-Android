---
title: Two Sum
tags:
  - hashmapLookUp
Link: https://leetcode.com/problems/two-sum/
---

## :LiLayoutPanelLeft:  Pattern Name: HashMap
 

---

## When to Use

- When we have to store a pair of number index as value and value as it array index
- traversing array to remember existing traverse element 
- as we need look up o(1) 

---

## Key Idea

- traverse the element store it in to map if condition not satisfied ( if target - value on index  not exist in map)
- if condition (target - value is present in map ) then return value in map and current index 


---

## Template (Kotlin)
```kotlin
val map = mutableMapOf<Int, Int>()
//for loop
if(map.containsKey(target-index)){
retrun intArrayOf(map.[target-index]!! , index})
else map.put(value,index)