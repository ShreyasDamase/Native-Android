---
title: Valid anagram
tags:
  - hashmapLookUp
---

## :LiLayoutPanelLeft:  Pattern Name: hashmap look up pattern
 

---

## When to Use

- i want to store the values as index of map and number of its occurrence as its value the number of time a letter repeied  - 

---

## Key Idea

- if both lenth S1 != S2 return it false this is one case
- traverse first string S1 store it letter and its number of occurrence 
- traverse second string S2 and minus the number of occurrence in map 
- traverse map and if ant occurrence come to be grater than 0 (first string has extra char) or less than 0 (probably second has extra) then return false else return true (when all has 0 count means equal numbs of characters) 

---

## Template (Kotlin)
```kotlin
val map = mutalbeMapOf<Char,Int>()
if(s.length!=t.length) return false
s.forEach{
map[it] = (map[it] ?: 0) + 1 //get map current index value or assign 0 if not and increment it as 
}
t.forEach{
map[it]= (map[it] ?: 0) - 1
}
map.forEach{
if(it.value != 0){
return false}
}

return true as anagram 


