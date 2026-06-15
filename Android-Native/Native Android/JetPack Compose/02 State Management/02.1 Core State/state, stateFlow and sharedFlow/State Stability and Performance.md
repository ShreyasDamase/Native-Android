# ⚡ State Stability and Performance — Complete Guide

> [!NOTE]
> **Sub-note for State Stability & Performance.** For standard local state rules, see [[State]]. For performance of Flow collection in UI, see [[StateFlow]] and [[SharedFlow]].

---

## 🧠 Smart Recomposition & Skip Optimization

Compose has a powerful optimization called **smart recomposition**: when a composable function is invoked during a recomposition pass, if all its parameters are evaluated as **stable** and remain **unchanged**, Compose will **skip** that composable execution entirely, saving significant rendering and execution time.

```kotlin
@Composable
fun UserCard(user: User, onFollowClick: () -> Unit) {
    // If both `user` and `onFollowClick` parameters haven't changed since the last composition,
    // Compose skips executing UserCard entirely
}
```

This skip optimization is critical for maintaining high performance (60fps/120fps) in complex UIs. However, it only works if the Compose compiler can guarantee that the parameters are **stable**.

---

## 🔷 What Is "Stable" — The Exact Rules

The Compose compiler classifies a type as **stable** if it meets the following criteria:

1. **Consistent equals()**: Calling `equals()` between two instances must always return a consistent result.
2. **Snapshot-notifying mutations**: When any public property of the type changes, Compose must be notified through the Snapshot System (so that it can trigger recompositions of dependent readers).
3. **Stable public properties**: All public properties of the class must also be stable types.

### Automatically Stable Types
* All primitive types: `Int`, `Long`, `Boolean`, `Float`, `Double`, `Char`
* `String`
* Kotlin `data class` with only `val` properties that are all themselves stable
* Compose `State<T>` types (`MutableState`, `MutableIntState`, etc.)
* Classes annotated with `@Immutable`
* Classes annotated with `@Stable`

### Types Marked "Unstable" by the Compose Compiler
* **Collections** (`List<T>`, `Map<K, V>`, `Set<T>`): Because these are standard Kotlin interfaces, they technically allow mutable implementations underneath. The compiler cannot guarantee that they won't be mutated in-place without notification.
* **Classes from Third-Party Libraries**: If a class comes from an external library that does not run the Compose compiler plugin, it is marked unstable by default.
* **Classes with `var` Properties**: Any class that exposes a public mutable property (`var`) is considered unstable because it can change without the Snapshot System being notified.

---

## 🔷 `@Immutable` Annotation

```kotlin
import androidx.compose.runtime.Immutable
```

`@Immutable` is a strict promise to the Compose compiler: *"I guarantee that all properties of this class will never change after construction."* Compose trusts this declaration and treats the type as stable.

```kotlin
// ❌ UNSTABLE — List<Product> makes the whole UI State unstable
data class ProductsUiState(
    val products: List<Product>  // ← unstable!
)
// Result: ProductsUiState is marked unstable, causing ProductList to recompose on every state trigger

// ✅ FIXED — Marking the whole UI state wrapper as @Immutable
@Immutable
data class ProductsUiState(
    val products: List<Product>  // ← stable holder makes products list stable
)
```

---

## 🔷 `@Stable` Annotation

```kotlin
import androidx.compose.runtime.Stable
```

`@Stable` is a softer promise: *"This type may have mutable properties, but it will notify Compose via the Snapshot System when a change occurs."* 

Use `@Stable` for classes that manage internal mutable state properties through observable primitives:

```kotlin
@Stable
class CartState(
    initialItems: List<CartItem> = emptyList()
) {
    private val _items = mutableStateListOf<CartItem>(*initialItems.toTypedArray())
    val items: List<CartItem> = _items  // Observable snapshot list

    fun addItem(item: CartItem) { _items.add(item) }
    fun removeItem(id: String) { _items.removeIf { it.id == id } }
    val totalPrice: Double get() = _items.sumOf { it.price * it.quantity }
}
```

---

## 🔷 `ImmutableList` — The Production Solution

For large collections in production applications, you should use `kotlinx-collections-immutable` to enforce type-level stability:

```kotlin
// build.gradle.kts
implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")
```

```kotlin
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
```

```kotlin
// ✅ ImmutableList is explicitly recognized as stable by the Compose compiler
data class SearchUiState(
    val results: ImmutableList<SearchResult> = persistentListOf(),
    val isLoading: Boolean = false
)

// In your ViewModel:
val uiState: StateFlow<SearchUiState> = searchResultsFlow
    .map { results ->
        SearchUiState(results = results.toImmutableList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())
```

---

## 🔷 Full Practical Example — Diagnosing & Fixing Recompositions

Let's look at a common scenario where unnecessary recompositions happen due to parameter instability:

```kotlin
// ❌ Product is stable, but onAddToCart (lambda) may cause recompositions:
@Composable
fun ProductCard(product: Product, onAddToCart: () -> Unit) {
    Card {
        Column {
            Text(product.name)
            Text("₹${product.price}")
            Button(onClick = onAddToCart) { Text("Add to Cart") }
        }
    }
}
```

### Diagnosis
If `ProductCard` is inside a list, passing a raw lambda like `{ viewModel.onAddToCart(product.id) }` creates a new lambda instance on every recomposition of the parent screen. Since lambdas are not implicitly verified as stable by the compiler, `ProductCard` is forced to recompose because the `onAddToCart` parameter reference changed.

### The Fix: Remember the Lambda
```kotlin
@Composable
fun ProductListScreen(viewModel: ProductListViewModel = viewModel()) {
    val products by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn {
        items(products.items, key = { it.id }) { product ->
            ProductCard(
                product = product,
                // ✅ Stable lambda: cached by remember using product.id as invalidation key
                onAddToCart = remember(product.id) {
                    { viewModel.onAddToCart(product.id) }
                }
            )
        }
    }
}
```

---

## 🔗 Connections

- **Compose State Engine**: [[State]] — `mutableStateOf` and Snapshot mechanics
- **Snapshot Collections**: [[Snapshot Collections]] — observable maps and lists
- **Decision Guide**: [[Comparison — State vs Flow vs StateFlow vs LiveData vs SharedFlow]]
