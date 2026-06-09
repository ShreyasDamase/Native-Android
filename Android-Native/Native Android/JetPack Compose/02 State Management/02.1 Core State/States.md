# Compose State Primitives

## 📌 Purpose
To manage local state within a Composable, Jetpack Compose provides several primitives. These functions allow Composables to "remember" data across recompositions and trigger UI updates when that data changes.

> [!NOTE]
> Jetpack Compose UI is a function of its state. If a Composable does not use a state primitive, it will forget its variable values every time it recomposes.

## 🔧 Core Functions

### `mutableStateOf`
```kotlin
fun <T> mutableStateOf(
    value: T,
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy()
): MutableState<T>
```
Creates an observable `MutableState<T>`. When the value is modified, Compose schedules a recomposition of any Composable reading it.

### Primitive specific versions (Performance Optimization)
```kotlin
fun mutableIntStateOf(value: Int): MutableIntState
fun mutableFloatStateOf(value: Float): MutableFloatState
fun mutableDoubleStateOf(value: Double): MutableDoubleState
fun mutableLongStateOf(value: Long): MutableLongState
```
> [!TIP]
> Always use primitive specific states (e.g., `mutableIntStateOf`) instead of `mutableStateOf(0)` to avoid autoboxing overhead.

### `remember`
```kotlin
@Composable
inline fun <T> remember(crossinline calculation: () -> T): T
```
Caches the result of `calculation`. During recomposition, it returns the cached value instead of executing `calculation` again. 

### `rememberSaveable`
```kotlin
@Composable
fun <T : Any> rememberSaveable(
    vararg inputs: Any?,
    saver: Saver<T, out Any> = autoSaver(),
    key: String? = null,
    init: () -> T
): T
```
Like `remember`, but it survives Activity or Process recreation (e.g., configuration changes like screen rotation) by saving the value in the Android `Bundle`.

## ✅ Basic Example

```kotlin
@Composable
fun NameInput() {
    // 1. mutableStateOf creates the state.
    // 2. remember caches it across recompositions.
    // 3. 'by' delegation allows us to use 'name' as a normal String.
    var name by remember { mutableStateOf("") }

    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Enter your name") }
    )
}
```

## 🚀 Advanced Examples

### Using `rememberSaveable` for configuration changes
If you rotate the device, standard `remember` will lose its value because the Activity is destroyed and recreated. `rememberSaveable` fixes this.

```kotlin
@Composable
fun RobustCounter() {
    // This value will survive screen rotations and dark mode toggles!
    var count by rememberSaveable { mutableIntStateOf(0) }

    Button(onClick = { count++ }) {
        Text("Count: $count")
    }
}
```

### Custom `Saver` for `rememberSaveable`
`rememberSaveable` out-of-the-box only supports types that can be saved in an Android `Bundle` (Strings, primitives, Parcelables). For custom data classes, you need a custom Saver.

```kotlin
data class User(val id: Int, val name: String)

val UserSaver = Saver<User, Map<String, Any>>(
    save = { mapOf("id" to it.id, "name" to it.name) },
    restore = { User(id = it["id"] as Int, name = it["name"] as String) }
)

@Composable
fun UserProfile() {
    var user by rememberSaveable(stateSaver = UserSaver) {
        mutableStateOf(User(1, "Alice"))
    }
    
    // UI here...
}
```

## ⚠️ Common Gotchas

1. **Forgetting `remember`**: `var state = mutableStateOf("")` (without remember) will recreate the state object on every single recomposition, losing any user input.
2. **Mutating Collections**: If you do `val list by remember { mutableStateOf(mutableListOf<String>()) }` and call `list.add("item")`, Compose **WILL NOT** recompose. `MutableState` only tracks when the instance itself is reassigned. Instead, use `remember { mutableStateListOf<String>() }`.
3. **Using `rememberSaveable` for large data**: `rememberSaveable` writes to the Android Bundle, which has a strict size limit (usually ~500kb). Do not store large lists or bitmaps here; use a ViewModel for that.

## 💡 Interview Q&A

**Q: What is the difference between `remember` and `rememberSaveable`?**
A: `remember` caches a value across recompositions, but its state is lost if the Composable leaves the composition or if the Activity is recreated (e.g., screen rotation). `rememberSaveable` saves the value into the Android `Bundle`, allowing it to survive configuration changes and process death.

**Q: Why should I use `mutableIntStateOf(0)` instead of `mutableStateOf(0)`?**
A: `mutableStateOf<T>` works with objects. Passing an `Int` to it causes autoboxing (converting primitive `int` to object `Integer`), which incurs a memory and performance penalty. `mutableIntStateOf` is optimized to work directly with the primitive `int`.

**Q: How does `remember` work under the hood?**
A: During the initial composition, `remember` executes its lambda and stores the result in the composition's slot table. During recomposition, it skips the lambda and simply retrieves the previously stored value from the slot table.
