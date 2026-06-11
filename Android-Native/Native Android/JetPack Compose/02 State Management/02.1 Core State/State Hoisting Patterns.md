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


---

## 🚀 Mastery Deep Dive (Added 2026)

> [!NOTE]
> The following deep dive notes were generated to provide mastery-level understanding, complementing the original notes above.

# State Hoisting Patterns

> [!NOTE]
> State hoisting is THE architectural principle behind how Compose composables are designed. If you understand this, you understand why the Compose API is designed the way it is — and why you should design your own composables the same way.

---

## 🧠 Mental Model — Read This First

**Hoisting state is like moving the TV remote from inside the TV to the coffee table.**

The TV still displays the same picture. The picture still changes based on channels. But now:
- **Anyone in the room can control it** (reusable component)
- **You can see what channel it's on from the coffee table** (single source of truth)
- **The TV doesn't decide what to show** (stateless, testable)

A composable that owns its own state is like a TV with the controls glued to its side — it works, but only one person can use it and you can't test it without turning on the TV.

---

## 🔬 What is State Hoisting?

State hoisting = **moving state from a child composable UP to its parent** by replacing:
- The internal `var state by remember { mutableStateOf(...) }` 
- With TWO parameters: `value: T` (the current state) and `onValueChange: (T) -> Unit` (the event callback)

```
Before hoisting:          After hoisting:
Child owns state          Parent owns state
    ┌─────────┐               ┌─────────────┐
    │  Child  │               │   Parent    │
    │  state ●│               │  state ●    │  ← truth lives here
    │  owns   │               └──────┬──────┘
    │  reads  │               value  │  onEvent
    │  writes │               ┌──────▼──────┐
    └─────────┘               │   Child     │
                              │  (stateless)│  ← just displays & reports
                              └─────────────┘
```

---

## ⚙️ Unidirectional Data Flow (UDF) — The Architecture

UDF is the principle that state flows in ONE direction (downward) and events flow in the OTHER direction (upward):

```
       ┌─────────────────────────────────┐
       │           ViewModel             │
       │   private _state (MutableState) │
       │   public state (read-only)      │
       └──────────────┬──────────────────┘
                      │
              state (data flows DOWN)
                      │
       ┌──────────────▼──────────────────┐
       │         Screen Composable       │
       │    val state = collect(...)     │
       └──────────────┬──────────────────┘
                      │
                      ├─── passes state down to children
                      │
       ┌──────────────▼──────────────────┐
       │         Child Composable        │
       │    value = state.someField      │
       │    onEvent = { /* lambda */ }   │
       └──────────────┬──────────────────┘
                      │
             events (flow UP as lambdas)
                      │
       ┌──────────────▼──────────────────┐
       │           ViewModel             │
       │    fun onEvent(event) { ... }   │
       └─────────────────────────────────┘
```

**The UDF guarantee:** Every state change has ONE path. There is ONE source of truth. The UI is always a pure function of state: `UI = f(State)`.

---

## ✅ Level 1: Composable-to-Composable Hoisting

### Before Hoisting (Stateful — not reusable)

```kotlin
// This composable OWNS its state — it decides everything
@Composable
fun StatefulUsernameInput() {
    var username by remember { mutableStateOf("") }

    OutlinedTextField(
        value = username,
        onValueChange = { username = it },
        label = { Text("Username") }
    )
    // Problem: if another composable needs the username value, it can't get it
    // Problem: to test this, you need a running composable
    // Problem: you can't pre-populate it from a parent
}
```

### After Hoisting (Stateless — reusable)

```kotlin
// Child: just a display + event reporter. No state ownership.
@Composable
fun UsernameInput(
    username: String,               // value: the current state (flows DOWN from parent)
    onUsernameChange: (String) -> Unit,  // onEvent: reports user action (flows UP to parent)
    modifier: Modifier = Modifier   // always include Modifier as the last parameter with default
) {
    OutlinedTextField(
        value = username,
        onValueChange = onUsernameChange,  // forward the event up — don't modify it here
        label = { Text("Username") },
        modifier = modifier
    )
}

// Parent: OWNS the state. Decides what happens on events.
@Composable
fun SignupScreen() {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Column {
        Text("Hello, $username")   // parent can use the state in multiple places

        UsernameInput(
            username = username,
            onUsernameChange = { username = it }  // parent decides how to update
        )

        // SAME stateless composable reused for email — complete reuse!
        UsernameInput(
            username = email,
            onUsernameChange = { email = it },
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
```

---

## ✅ Level 2: Hoisting to ViewModel

For state that represents real business data (not just UI behavior like "is this menu open"), hoist all the way to the ViewModel:

```kotlin
// ViewModel — owns all business-relevant state
class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun onUsernameChange(newUsername: String) {
        // Business logic can live here: validation, transformation, etc.
        if (newUsername.length <= 30) {
            _uiState.update { it.copy(username = newUsername) }
        }
    }

    fun onSaveClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                repository.saveProfile(_uiState.value.username)
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}

data class ProfileUiState(
    val username: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

// Screen: connects ViewModel state to UI
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileContent(
        uiState = uiState,
        onUsernameChange = viewModel::onUsernameChange,   // method reference → cleaner
        onSaveClicked = viewModel::onSaveClicked
    )
}

// Content: completely stateless — pure function of its parameters
@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    onUsernameChange: (String) -> Unit,
    onSaveClicked: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        UsernameInput(
            username = uiState.username,
            onUsernameChange = onUsernameChange
        )
        if (uiState.error != null) {
            Text(uiState.error, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onSaveClicked,
            enabled = !uiState.isSaving
        ) {
            if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp))
            else Text("Save Profile")
        }
    }
}
```

**Why separate `ProfileScreen` from `ProfileContent`?**
- `ProfileScreen` knows about ViewModel (Android-specific, hard to test)
- `ProfileContent` is a pure function — testable with fake data, previewable without a ViewModel

---

## ✅ Level 3: Plain State Holder Class — UI State Too Complex for ViewModel

Some composables have complex UI-specific state that:
- Doesn't belong in a ViewModel (purely visual — not business logic)
- Has too many fields to manage inline in the composable

Solution: a plain class with Compose state inside it.

```kotlin
// State holder class — manages complex UI-only state
@Stable   // hint to Compose that this class's changes are observable
class MultiStepFormState(
    initialStep: Int = 0
) {
    var currentStep by mutableIntStateOf(initialStep)
        private set   // only this class can modify it

    var personalInfo by mutableStateOf(PersonalInfo())
    var address by mutableStateOf(Address())
    var paymentInfo by mutableStateOf(PaymentInfo())

    val totalSteps = 3
    val isFirstStep get() = currentStep == 0
    val isLastStep get() = currentStep == totalSteps - 1
    val progress get() = (currentStep + 1).toFloat() / totalSteps

    fun goToNextStep() {
        if (!isLastStep) currentStep++
    }
    fun goToPreviousStep() {
        if (!isFirstStep) currentStep--
    }
    fun isStepComplete(step: Int): Boolean = when (step) {
        0 -> personalInfo.isComplete
        1 -> address.isComplete
        2 -> paymentInfo.isComplete
        else -> false
    }
}

// Companion factory function — following Compose naming convention
@Composable
fun rememberMultiStepFormState(initialStep: Int = 0): MultiStepFormState {
    return remember { MultiStepFormState(initialStep) }
}

// Composable uses the state holder
@Composable
fun MultiStepFormScreen(onSubmit: (PersonalInfo, Address, PaymentInfo) -> Unit) {
    val formState = rememberMultiStepFormState()

    Column {
        LinearProgressIndicator(
            progress = formState.progress,
            modifier = Modifier.fillMaxWidth()
        )

        when (formState.currentStep) {
            0 -> PersonalInfoStep(
                info = formState.personalInfo,
                onInfoChange = { formState.personalInfo = it }
            )
            1 -> AddressStep(
                address = formState.address,
                onAddressChange = { formState.address = it }
            )
            2 -> PaymentStep(
                payment = formState.paymentInfo,
                onPaymentChange = { formState.paymentInfo = it }
            )
        }

        Row {
            if (!formState.isFirstStep) {
                OutlinedButton(onClick = formState::goToPreviousStep) { Text("Back") }
            }
            Spacer(Modifier.weight(1f))
            if (formState.isLastStep) {
                Button(onClick = { onSubmit(formState.personalInfo, formState.address, formState.paymentInfo) }) {
                    Text("Submit")
                }
            } else {
                Button(
                    onClick = formState::goToNextStep,
                    enabled = formState.isStepComplete(formState.currentStep)
                ) {
                    Text("Next")
                }
            }
        }
    }
}
```

---

## 🎯 Event vs State — The Critical Distinction

This is frequently confused and frequently asked in interviews.

**State** = "What is currently true." Survives configuration changes. Shown to the user continuously. Should be re-delivered on screen rotation.
```kotlin
// Examples of STATE:
data class ProfileUiState(
    val username: String,      // what's in the field RIGHT NOW
    val isLoading: Boolean,    // is the screen currently loading?
    val users: List<User>      // the list currently displayed
)
```

**Event** = "Something happened once." One-time. Should NOT be re-delivered on rotation. Navigation, toasts, snackbars.
```kotlin
// Examples of EVENTS:
sealed class ProfileUiEvent {
    data class ShowSnackbar(val message: String) : ProfileUiEvent()   // toast/snackbar: one-time
    object NavigateToHome : ProfileUiEvent()                          // navigation: one-time
    data class ShowDialog(val title: String) : ProfileUiEvent()       // dialog: one-time
}
```

**Why events should NOT go in StateFlow:**
```kotlin
// ❌ WRONG — error as state
data class UiState(val errorMessage: String?)

// User saves → error occurs → _state.value = UiState(error = "Failed")
// User ROTATES screen → StateFlow replays last value → error snackbar shows AGAIN!
// Even though the error already happened before rotation!

// ✅ CORRECT — error as event
private val _events = MutableSharedFlow<UiEvent>()
val events: SharedFlow<UiEvent> = _events.asSharedFlow()

// SharedFlow (replay=0) → rotation doesn't re-deliver past events ✅
```

### The Full ViewModel Pattern (State + Events)

```kotlin
class ProfileViewModel : ViewModel() {

    // STATE — what is currently true (re-delivered on rotation)
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // EVENTS — one-time signals (NOT re-delivered on rotation)
    private val _events = MutableSharedFlow<ProfileUiEvent>()
    val events: SharedFlow<ProfileUiEvent> = _events.asSharedFlow()

    fun onSaveClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                repository.save(_uiState.value.username)
                _events.emit(ProfileUiEvent.ShowSnackbar("Profile saved!"))  // one-time event
                _events.emit(ProfileUiEvent.NavigateToHome)                  // one-time event
            } catch (e: Exception) {
                _events.emit(ProfileUiEvent.ShowSnackbar("Save failed: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
```

---

## 🔺 The Three Levels of State in Compose

| Level | Where State Lives | Who Manages It | Use For |
|---|---|---|---|
| 1 — Composable | `var x by remember {...}` inside the composable | The composable itself | Pure UI mechanics: is dropdown open, scroll offset, animation progress |
| 2 — State Holder | Plain class with `mutableStateOf` fields | `rememberXState()` | Complex UI-only state with multiple related fields: multi-step form, drag-and-drop state |
| 3 — ViewModel | `MutableStateFlow` in ViewModel | ViewModel + Repository | All business data: loaded content, user data, app-wide settings |

### Choosing the Right Level

```
Is it purely visual UI state?
    Yes → Does it need multiple related fields or complex logic?
        Yes → State Holder class (Level 2)
        No  → remember in composable (Level 1)
    No  → Does it need to survive rotation or involve business rules?
        Yes → ViewModel (Level 3)
```

---

## ❌ Common Mistakes

### Mistake 1: Over-hoisting (Prop Drilling)

```kotlin
// ❌ Hoisting too high creates prop drilling — passing through many composables that don't use it
@Composable
fun AppRoot() {
    var searchQuery by remember { mutableStateOf("") }
    MainScreen(searchQuery = searchQuery, onQueryChange = { searchQuery = it })
}
@Composable
fun MainScreen(searchQuery: String, onQueryChange: (String) -> Unit) {
    // MainScreen doesn't use searchQuery — just passing it down
    ContentArea(searchQuery = searchQuery, onQueryChange = onQueryChange)
}
@Composable
fun ContentArea(searchQuery: String, onQueryChange: (String) -> Unit) {
    // ContentArea doesn't use it either — just passing down
    SearchBar(query = searchQuery, onQueryChange = onQueryChange)
}
// Solutions: CompositionLocal, ViewModel, or keep state closer to where it's used
```

### Mistake 2: Calling ViewModel functions in composable body (side effects!)

```kotlin
// ❌ WRONG — calling viewModel functions during composition is a side effect
@Composable
fun ProductScreen(productId: String, viewModel: ProductViewModel = viewModel()) {
    viewModel.loadProduct(productId)   // Called during composition — will fire on EVERY recomposition!

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProductContent(state)
}

// ✅ CORRECT — use LaunchedEffect for side effects triggered by composition
@Composable
fun ProductScreen(productId: String, viewModel: ProductViewModel = viewModel()) {
    LaunchedEffect(productId) {   // runs once when productId changes
        viewModel.loadProduct(productId)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProductContent(state)
}
```

### Mistake 3: Multiple `onXxx` lambdas when a sealed event class is cleaner

```kotlin
// ❌ Too many lambdas — hard to read and maintain for complex screens
@Composable
fun ChatScreen(
    onSendMessage: (String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onReactToMessage: (String, Emoji) -> Unit,
    onForwardMessage: (String, UserId) -> Unit,
    onReplyToMessage: (String) -> Unit,
    // ...10 more lambdas
)

// ✅ Sealed event class — single lambda, extensible, readable
sealed class ChatUiEvent {
    data class SendMessage(val content: String) : ChatUiEvent()
    data class DeleteMessage(val messageId: String) : ChatUiEvent()
    data class ReactToMessage(val messageId: String, val emoji: Emoji) : ChatUiEvent()
    // ...
}

@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onEvent: (ChatUiEvent) -> Unit   // single lambda for ALL events
)

// Usage:
onEvent(ChatUiEvent.SendMessage("Hello!"))
onEvent(ChatUiEvent.DeleteMessage(messageId))
```

---

## ✅ Stateless Composable — Why It's Testable

```kotlin
// Stateless composable — 100% testable without running a ViewModel
@Composable
fun LoginForm(
    email: String,
    password: String,
    isLoading: Boolean,
    errorMessage: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClicked: () -> Unit
) {
    // ... UI code
}

// In tests — no ViewModel needed:
@Test
fun loginForm_showsError() {
    composeTestRule.setContent {
        LoginForm(
            email = "test@test.com",
            password = "pass",
            isLoading = false,
            errorMessage = "Invalid credentials",  // ← directly inject test state
            onEmailChange = {},
            onPasswordChange = {},
            onLoginClicked = {}
        )
    }
    composeTestRule.onNodeWithText("Invalid credentials").assertIsDisplayed()
}
```

---

## 🔗 Connections

- **State primitives**: [[States]] — the `mutableStateOf` and `remember` the child composable stops owning
- **StateFlow**: [[State vs StateFlow]] — Level 3 hoisting — ViewModel uses `StateFlow` not Compose `State`
- **Events**: [[Android Jetpack Flow Study Notes]] — SharedFlow is used for UI events
- **Testing**: Stateless composables connect directly to Compose UI testing

---

## 💬 Interview Master Q&A

**Q: What is state hoisting and why is it important?**
> State hoisting is moving state from a child composable to its parent by replacing internal `mutableStateOf` variables with two parameters: the current value and a callback to request changes. It's important because it creates a single source of truth (the state exists in exactly one place), makes composables stateless and reusable in different contexts, enables proper Unidirectional Data Flow (state flows down, events flow up), and makes composables testable without real state management infrastructure.

**Q: What is UDF (Unidirectional Data Flow) in Compose?**
> UDF is the architectural principle where state always flows downward through the composable tree (from parent to child as parameters) and user events always flow upward (from child to parent as lambda callbacks). This one-way flow means there is always a single source of truth for any piece of state, changes happen through a defined path, and the UI is always a pure function of its state: `UI = f(State)`. In practice, state lives in a ViewModel's `StateFlow`, the composable collects it and passes it down to stateless child composables, and child events are passed back up to the ViewModel as function calls.

**Q: What is the difference between UI State and UI Events in a ViewModel?**
> UI State represents what is currently true and should be displayed — it's modeled as `StateFlow` which always holds the latest value and replays it on new collectors. This means if the screen rotates, the new composition immediately gets the current state. UI Events are one-time signals — navigation, showing a Snackbar, displaying a dialog — they should NOT replay on rotation because showing a navigation event twice would navigate the user twice. Events are modeled as `SharedFlow` with `replay = 0`, so new collectors only get future events, not past ones.

**Q: Should ALL state be hoisted to the ViewModel?**
> No. There are three levels of state in Compose. ViewModel-level (StateFlow) is for business data — content loaded from APIs, user authentication state, form data that needs validation or persistence. A plain State Holder class (remember { MyState() }) is for complex UI-only state with multiple related fields that doesn't involve business logic, like a multi-step form's current step and navigation. Individual `remember` in a composable is for purely visual state that only matters to that composable — is a dropdown open, animation progress, local scroll offset. Hoisting everything to the ViewModel creates bloated ViewModels and breaks the principle of only keeping business logic there.
