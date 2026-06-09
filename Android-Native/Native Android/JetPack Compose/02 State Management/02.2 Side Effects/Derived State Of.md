# 7. `derivedStateOf`

## What is it?

Creates a state that is **computed from other states**. It only recomposes when the **result** changes, not when the source states change. Wrapped in `remember`.

## The Problem it Solves

kotlin

```kotlin
// ❌ BAD — recomposes on every keystroke even if button state doesn't change
val isEnabled = email.isNotEmpty() && password.length >= 6
```

kotlin

```kotlin
// ✅ GOOD — only recomposes when isEnabled result actually changes
val isEnabled by remember {
    derivedStateOf {
        email.isNotEmpty() && password.length >= 6
    }
}
```

## Examples

**Form validation:**

kotlin

```kotlin
var email by remember { mutableStateOf("") }
var password by remember { mutableStateOf("") }

val isButtonEnabled by remember {
    derivedStateOf {
        email.isNotEmpty() && password.length >= 6
    }
}

Button(
    onClick = { viewModel.login(email, password) },
    enabled = isButtonEnabled
) {
    Text("Login")
}
```

**List scroll detection:**

kotlin

```kotlin
val listState = rememberLazyListState()

val showScrollToTop by remember {
    derivedStateOf {
        listState.firstVisibleItemIndex > 0 // show button only when scrolled
    }
}

if (showScrollToTop) {
    Button(onClick = { /* scroll to top */ }) {
        Text("Back to top")
    }
}
```

**Filter a list:**

kotlin

```kotlin
var searchQuery by remember { mutableStateOf("") }
val books = viewModel.books

val filteredBooks by remember {
    derivedStateOf {
        if (searchQuery.isEmpty()) books
        else books.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }
}
```