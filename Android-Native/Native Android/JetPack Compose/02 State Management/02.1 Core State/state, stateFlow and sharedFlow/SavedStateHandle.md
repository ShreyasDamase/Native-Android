# 💾 SavedStateHandle — Complete Guide

> [!NOTE]
> **Sub-note for SavedStateHandle.** For ViewModel state management basics, see [[StateFlow]]. For local UI persistence like `rememberSaveable`, see [[State]].

---

## 🧠 The Problem — ViewModel Does NOT Survive Process Death

This is a subtle but critical distinction in Android lifecycle management:

```
Screen Rotation:
  OS kills Activity → OS recreates Activity → SAME ViewModel instance returned
  ✅ ViewModel state survives

Process Death (low memory):
  OS kills the entire app process → User returns → App cold starts
  ❌ ViewModel is gone — a new one is created
  ❌ All MutableStateFlow values reset to defaults
```

**Process death** happens when the OS needs memory for other apps in the background. The user might have been on a checkout form, filled in their credit card number, put the phone down for a few minutes to reply to a message, and returned — only to find the app re-initialized and the form completely empty.

`SavedStateHandle` solves this: it saves key-value pairs to Android's **saved instance state Bundle**, which survives process death. When the app is cold-started back to the exact activity or navigation destination where the user left off, this Bundle is restored.

---

## 🔷 Dependency & Import

```kotlin
// Usually included transitively with lifecycle-viewmodel-ktx
implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.7")
```

```kotlin
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
```

---

## 🔷 How to Inject SavedStateHandle

### With Hilt (Automatic)
No extra code or factory is required. Hilt automatically injects the correct `SavedStateHandle` for the current destination:

```kotlin
@HiltViewModel
class FormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,  // Hilt provides this automatically
    private val repository: FormRepository
) : ViewModel()
```

### Without Hilt
If you use Compose's standard `viewModel()` helper, it automatically passes the `SavedStateHandle` behind the scenes if you have a constructor that accepts it:

```kotlin
class FormViewModel(
    private val savedStateHandle: SavedStateHandle  // Compose's viewModel() provides this
) : ViewModel()
```

---

## 🔷 Usage Patterns

### Pattern 1: `getStateFlow()` — Observing State updates

This is the standard way to create state in ViewModels that survives process death:

```kotlin
class CheckoutViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Backed by saved state — survives process death
    val shippingAddress: StateFlow<String> =
        savedStateHandle.getStateFlow("shipping_address", "")

    val paymentMethod: StateFlow<String> =
        savedStateHandle.getStateFlow("payment_method", "")

    fun onAddressChanged(address: String) {
        savedStateHandle["shipping_address"] = address  // Auto-saved to Bundle & emits
    }

    fun onPaymentMethodChanged(method: String) {
        savedStateHandle["payment_method"] = method
    }
}
```

### Pattern 2: `saveable` Delegate — Modern API (Lifecycle 2.9.0+)

The cleanest syntax — it lets you use Compose's delegation syntax directly inside the ViewModel, backed by `SavedStateHandle`:

```kotlin
class ProfileEditViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // These survive process death AND look like regular Compose state
    var displayName by savedStateHandle.saveable { mutableStateOf("") }
        private set

    var bio by savedStateHandle.saveable { mutableStateOf("") }
        private set

    var selectedAvatarIndex by savedStateHandle.saveable { mutableStateOf(0) }
        private set

    fun onDisplayNameChange(name: String) { displayName = name }
    fun onBioChange(bio: String) { this.bio = bio }
    fun onAvatarSelected(index: Int) { selectedAvatarIndex = index }
}
```

### Pattern 3: Navigation Arguments — Automatic Integration

When using Navigation Compose, `SavedStateHandle` automatically receives navigation arguments passed to the destination route:

```kotlin
// Navigation route: "product/{productId}/edit?tab={tab}"

class ProductEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ProductRepository
) : ViewModel() {

    // Automatically populated from the nav argument keys
    private val productId: String = checkNotNull(savedStateHandle["productId"])
    private val initialTab: String = savedStateHandle["tab"] ?: "details"

    init {
        loadProduct(productId)
    }
}
```

---

## 🔷 Full Practical Example — Multi-Step Checkout Form

```kotlin
import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShippingAddress(
    val fullName: String = "",
    val street: String = "",
    val city: String = "",
    val zipCode: String = "",
    val country: String = "India"
) : Parcelable

class CheckoutViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository
) : ViewModel() {

    companion object {
        const val KEY_STEP = "checkout_step"
        const val KEY_ADDRESS = "shipping_address"
        const val KEY_PAYMENT = "payment_method"
    }

    // Step state (Int)
    val currentStep: StateFlow<Int> =
        savedStateHandle.getStateFlow(KEY_STEP, 1)

    // Form data state (Parcelable Custom Object)
    val shippingAddress: StateFlow<ShippingAddress> =
        savedStateHandle.getStateFlow(KEY_ADDRESS, ShippingAddress())

    // Payment method state (String)
    val paymentMethod: StateFlow<String> =
        savedStateHandle.getStateFlow(KEY_PAYMENT, "")

    private val _events = MutableSharedFlow<CheckoutEvent>()
    val events: SharedFlow<CheckoutEvent> = _events.asSharedFlow()

    fun onAddressUpdated(address: ShippingAddress) {
        savedStateHandle[KEY_ADDRESS] = address
    }

    fun onPaymentMethodSelected(method: String) {
        savedStateHandle[KEY_PAYMENT] = method
    }

    fun onNextStep() {
        val step = currentStep.value
        if (step < 3) savedStateHandle[KEY_STEP] = step + 1
    }

    fun onPreviousStep() {
        val step = currentStep.value
        if (step > 1) savedStateHandle[KEY_STEP] = step - 1
    }

    fun onPlaceOrder() {
        viewModelScope.launch {
            try {
                val address = shippingAddress.value
                val payment = paymentMethod.value
                orderRepository.placeOrder(address, payment)
                _events.emit(CheckoutEvent.OrderPlaced)
            } catch (e: Exception) {
                _events.emit(CheckoutEvent.ShowError(e.message ?: "Order failed"))
            }
        }
    }
}
```
> [!NOTE]
> If the OS terminates the app process in step 2 with the address filled in, when the user returns, they are put back at step 2 with their address intact.

---

## 🔷 What Data SavedStateHandle Supports

| Type | Directly Supported? |
| :--- | :---: |
| `Int`, `Long`, `Float`, `Double`, `Boolean`, `String` | ✅ Yes |
| Primitive Arrays (`IntArray`, `StringArray`, etc.) | ✅ Yes |
| `Parcelable` objects (via `@Parcelize`) | ✅ Yes |
| `Serializable` objects | ✅ Yes (slower than Parcelable) |
| `ArrayList<Parcelable>` | ✅ Yes |
| Large Bitmaps / Very Large Lists | ❌ No (will cause `TransactionTooLargeException`) |

> [!WARNING]
> **Golden Rule:** Never store large datasets (such as lists or heavy bitmap data) inside `SavedStateHandle`. Store only simple **identifiers** (IDs, search queries, keys) and use those identifiers to re-fetch the fresh data from your repository on startup.

---

## 🔗 Connections

- **State Flows in ViewModels**: [[StateFlow]] — standard StateFlow lifecycle and updates
- **Saved UI State**: [[State]] — `rememberSaveable` for Compose-side local state persistence
- **Comparative Decision Tree**: [[Comparison — State vs Flow vs StateFlow vs LiveData vs SharedFlow]]
