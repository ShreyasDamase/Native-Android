# 6. `produceState`

## What is it?

Converts **non-Compose data sources** (like a network call, Flow, or callback) into **Compose State**. Runs in a coroutine.

## Structure

kotlin

```kotlin
val result by produceState<Type>(initialValue = someDefault) {
    value = // fetch or compute your data
}
```

## Examples

**Network call:**

kotlin

```kotlin
val user by produceState<UserState>(initialValue = UserState.Loading) {
    value = try {
        val data = repo.getUser()
        UserState.Success(data)
    } catch (e: Exception) {
        UserState.Error(e.message)
    }
}
```

**Collect a Flow:**

kotlin

```kotlin
val books by produceState<List<Book>>(initialValue = emptyList()) {
    repo.getBooksFlow().collect { books ->
        value = books
    }
}
```

**With cleanup:**

kotlin

```kotlin
val location by produceState<Location?>(initialValue = null) {
    val callback = LocationCallback { loc -> value = loc }
    locationManager.start(callback)

    awaitDispose {
        locationManager.stop(callback) // cleanup
    }
}
```

## vs ViewModel

Use `produceState` for **simple local state** in a single screen. Use ViewModel for **shared or complex state**.