## Overview
[[LaunchedEffect-DisposableEffect]]
[[SideEffect-produceState]]
[[rememberCoroutineScope]]
[[snapshotFlow]]



# Side Effects in Jetpack Compose

---

## 1. `LaunchedEffect`

**"Run a coroutine when the composable enters the screen"**

```kotlin
LaunchedEffect(Unit) {
    viewModel.loadData() // runs once on screen open
}
// re-runs if key changes
LaunchedEffect(userId) {
    viewModel.loadUser(userId) // re-runs when userId changes
}
```

---

## 2. `DisposableEffect`

**"Run something when composable enters, and clean it up when it leaves"**

```kotlin
DisposableEffect(Unit) {
    val listener = // register listener
    onDispose {
        // cleanup when screen leaves
        listener.unregister()
    }
}
```

---

## 3. `SideEffect`

**"Run something on every recomposition"**

```kotlin
SideEffect {
    // runs every time the composable recomposes
    analytics.logScreenView("ProfileScreen")
}
```

---

## 4. `rememberCoroutineScope`

**"Get a coroutine scope to launch coroutines from non-composable events like button clicks"**

```kotlin
val scope = rememberCoroutineScope()

Button(onClick = {
    scope.launch {
        viewModel.saveData() // triggered by user action
    }
}) {
    Text("Save")
}
```

---

## 5. `rememberUpdatedState`

**"Remember the latest value of something without restarting the effect"**

```kotlin
val currentOnTimeout by rememberUpdatedState(onTimeout)

LaunchedEffect(Unit) {
    delay(3000)
    currentOnTimeout() // always calls latest version
}
```

---

## 6. `produceState`

**"Convert non-Compose state (like a Flow or callback) into Compose state"**

```kotlin
val uiState by produceState<UserState>(initialValue = Loading) {
    value = repo.getUser() // runs in coroutine, result becomes state
}
```

---

## 7. `derivedStateOf`

**"Create a state that is computed from other states, only updates when result changes"**

```kotlin
val isButtonEnabled by remember {
    derivedStateOf {
        email.isNotEmpty() && password.length >= 6
    }
}
```

---

## Quick Reference Table

|Effect|When to use|
|---|---|
|`LaunchedEffect`|API call on screen open|
|`DisposableEffect`|Listeners, sensors that need cleanup|
|`SideEffect`|Analytics, logging on recompose|
|`rememberCoroutineScope`|Coroutine from button click|
|`rememberUpdatedState`|Latest lambda inside long effect|
|`produceState`|Convert Flow/callback to State|
|`derivedStateOf`|Computed state from other states|

---

React equivalent comparison:

|Compose|React|
|---|---|
|`LaunchedEffect`|`useEffect`|
|`rememberCoroutineScope`|`useCallback`|
|`derivedStateOf`|`useMemo`|
|`produceState`|`useState` + `useEffect` combined|
# Master Summary Table

|Effect|Runs When|Use Case|Coroutine|
|---|---|---|---|
|`LaunchedEffect`|Screen opens / key changes|API calls, navigation|✅ Yes|
|`DisposableEffect`|Screen opens / leaves|Listeners, sensors|❌ No|
|`SideEffect`|Every recomposition|Sync external systems|❌ No|
|`rememberCoroutineScope`|User triggered|Button clicks|✅ Yes|
|`rememberUpdatedState`|Value changes|Stale lambda fix|❌ No|
|`produceState`|Screen opens|Convert Flow to State|✅ Yes|
|`derivedStateOf`|Source state changes|Computed/filtered state|❌ No|

---

# React vs Compose Cheatsheet

|Scenario|React|Compose|
|---|---|---|
|Run on mount|`useEffect(() => {}, [])`|`LaunchedEffect(Unit)`|
|Run on value change|`useEffect(() => {}, [val])`|`LaunchedEffect(val)`|
|Cleanup on unmount|`useEffect(() => { return () => cleanup() })`|`DisposableEffect`|
|Memoized value|`useMemo`|`derivedStateOf`|
|Manual async trigger|`useCallback`|`rememberCoroutineScope`|
|State from async source|`useState` + `useEffect`|`produceState`|