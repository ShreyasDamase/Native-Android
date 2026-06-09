# 4. `rememberCoroutineScope`

## What is it?

Gives you a **coroutine scope** that you can use **outside** of composable functions — like inside button click handlers. The scope is automatically cancelled when the composable leaves.

## Why not just use `LaunchedEffect`?

`LaunchedEffect` runs automatically. `rememberCoroutineScope` runs **on demand** triggered by user actions.

## Examples

**Button click API call:**

kotlin

```kotlin
val scope = rememberCoroutineScope()

Button(onClick = {
    scope.launch {
        viewModel.saveBook() // user triggered
    }
}) {
    Text("Save")
}
```

**Show snackbar on button click:**

kotlin

```kotlin
val scope = rememberCoroutineScope()
val snackbarHostState = remember { SnackbarHostState() }

Button(onClick = {
    scope.launch {
        snackbarHostState.showSnackbar("Book saved!")
    }
}) {
    Text("Save")
}
```

**Scroll to top on button click:**

kotlin

````kotlin
val scope = rememberCoroutineScope()
val listState = rememberLazyListState()

Button(onClick = {
    scope.launch {
        listState.animateScrollToItem(0) // scroll to top
    }
}) {
    Text("Back to top")
}
```

## Rule of Thumb
```
Automatic on screen load  → LaunchedEffect
Triggered by user action  → rememberCoroutineScope
````