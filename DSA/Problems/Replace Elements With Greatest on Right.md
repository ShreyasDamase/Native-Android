---
title: Replace Elements With Greatest on Right
tags:
  - rightToLeftPass
---

## :LiLayoutPanelLeft:  Pattern Name: Right-to-left pass
 

---

## When to Use

- we need to traverse nested in array 

---

## Key Idea

- use nested for loop where outer loop will point current index and inner loop will point the value from current index and traverse onward and get max num 
- and assign that max num and move forward to next element and wise versa 

---

## Template (Kotlin)
```kotlin
for ( i in arr.indices){
var tem=-1
for (j in i+1 until arr.size ){
temp =max(temp,arr[j])
}
arr[i]= temp
}
return arr