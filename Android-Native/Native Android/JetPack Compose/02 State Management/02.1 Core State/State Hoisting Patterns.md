# State Hoisting Patterns

## 📌 Purpose
**State Hoisting** is a pattern of moving state from a child composable to its parent to make the child composable stateless. This makes the child highly reusable, easier to test, and enables a single source of truth.

> [!NOTE]
> "Hoisting" means "lifting up." You lift the state up out of the composable that displays the UI, and push it into the caller (parent composable or ViewModel).

## 📋 How it works

When you hoist state, you replace internal state variables with two parameters:
1. **`value: T`** - the current value of the state to display.
2. **`onValueChange: (T) -> Unit`** - an event that requests the state to change.

## ✅ Basic Example

### Before Hoisting (Stateful, not reusable)
```kotlin
@Composable
fun StatefulUsernameInput() {
    // The composable owns its state
    var username by remember { mutableStateOf("") }

    OutlinedTextField(
        value = username,
        onValueChange = { username = it },
        label = { Text("Username") }
    )
}
```

### After Hoisting (Stateless, highly reusable)
```kotlin
// Child: Stateless and reusable
@Composable
fun StatelessUsernameInput(
    username: String,
    onUsernameChange: (String) -> Unit
) {
    OutlinedTextField(
        value = username,
        onValueChange = onUsernameChange,
        label = { Text("Username") }
    )
}

// Parent: Holds the state (Source of Truth)
@Composable
fun ProfileScreen() {
    var currentUsername by remember { mutableStateOf("") }

    Column {
        Text("Hello, $currentUsername")
        StatelessUsernameInput(
            username = currentUsername,
            onUsernameChange = { currentUsername = it }
        )
    }
}
```

## 🚀 Advanced Examples

### Hoisting to a ViewModel
In most modern Android architectures, state is hoisted all the way up to a `ViewModel`. This separates business logic entirely from the UI.

```kotlin
class ProfileViewModel : ViewModel() {
    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    fun updateUsername(newName: String) {
        // Business logic can be added here (e.g., validation)
        if (newName.length <= 20) {
            _username.value = newName
        }
    }
}

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel()) {
    // State hoisted to ViewModel
    val username by viewModel.username.collectAsStateWithLifecycle()

    StatelessUsernameInput(
        username = username,
        onUsernameChange = viewModel::updateUsername
    )
}
```

### Hoisting with a Plain State Holder Class
Sometimes a composable has complex UI state that doesn't belong in a ViewModel (e.g., dragging, scrolling, animations). You can hoist state into a plain class.

```kotlin
class CustomSliderState(initialValue: Float) {
    var value by mutableFloatStateOf(initialValue)
        private set

    fun updateValue(newValue: Float) {
        value = newValue.coerceIn(0f, 100f)
    }
}

@Composable
fun rememberCustomSliderState(initialValue: Float = 0f): CustomSliderState {
    return remember { CustomSliderState(initialValue) }
}

@Composable
fun CustomSliderScreen() {
    // State is hoisted into a state holder class
    val sliderState = rememberCustomSliderState(50f)

    Slider(
        value = sliderState.value,
        onValueChange = { sliderState.updateValue(it) }
    )
}
```

## ⚠️ Common Gotchas

1. **Over-hoisting**: You don't need to hoist *every* piece of state to the ViewModel. If a state is purely for UI purposes (like whether a dropdown menu is expanded or an animation progress), hoist it to the nearest common parent Composable or use a UI State Holder class. Only hoist business-relevant state to the ViewModel.
2. **Prop Drilling**: If you hoist state too high up, you might have to pass state and callbacks through many intermediate composables that don't need them. This is called "prop drilling." Solutions include using `CompositionLocal` or passing higher-order functions / slots.
3. **Ignoring the Event**: When you hoist state, the child *must* call the `onValueChange` lambda when the user interacts with it. If the child tries to mutate the state directly or forgets to fire the event, the UI won't update.

## 💡 Interview Q&A

**Q: What is State Hoisting?**
A: State hoisting is the process of moving state from a composable down to its parent. It involves replacing internal `mutableStateOf` variables with two parameters: the state value itself, and a lambda function to request a change to that state.

**Q: Why should we favor stateless composables over stateful ones?**
A: Stateless composables are completely decoupled from how state is managed. This makes them highly reusable in different contexts, easily testable by passing mock data, and compliant with Unidirectional Data Flow, ensuring a single source of truth.

**Q: Should all state be hoisted to the ViewModel?**
A: No. UI-specific state, such as scroll position, expanded state of a menu, or animation state, should usually be kept within the Compose layer (either in the composable or a plain UI State Holder class). Only state that represents app data or business logic should be hoisted to the ViewModel.
