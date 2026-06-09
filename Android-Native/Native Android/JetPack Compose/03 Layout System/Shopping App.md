 /*
 * ═══════════════════════════════════════════════════════════════════════════════
 * 🛒 SHOPPING LIST APP - ADVANCED MVVM IMPLEMENTATION
 * Learn Complex State Management Like a Pro!
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 🎯 WHY THIS APP IS AWESOME FOR LEARNING:
 * 
 * Counter App taught you basics → This teaches you REAL-WORLD complexity!
 * 
 * NEW CONCEPTS YOU'LL MASTER:
 * ✨ Repository Pattern (data abstraction)
 * ✨ Complex State (multiple related data types)
 * ✨ Derived State (computed properties)
 * ✨ Form Validation (with live feedback)
 * ✨ Categories & Filtering
 * ✨ Budget Tracking (business logic)
 * ✨ Statistics Dashboard
 * 
 * 🚀 PROGRESSION:
 * Counter App: 1 number
 * Todo App: List of items
 * Shopping App: Items + Categories + Budget + Statistics (REAL complexity!)
 * 
 * 📚 GUIDE CHAPTERS: 5, 6, 7, 8 (Intermediate Level)
 * 
 * HOW TO USE:
 * 1. Create new Android Studio project (Empty Compose Activity)
 * 2. Replace MainActivity.kt with this entire file
 * 3. Change package name at top
 * 4. Run and explore!
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 */

package com.example.shoppingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

// ═══════════════════════════════════════════════════════════════════════════════
// 📦 STEP 1: DATA MODELS
// ═══════════════════════════════════════════════════════════════════════════════

/*
 * 💡 TIP: Multiple Data Types
 * 
 * Counter: 1 type (int)
 * Shopping: 3 types (Item, Category, Budget)
 * 
 * Real apps have MULTIPLE related data!
 */

data class ShoppingItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val quantity: Int,
    val price: Double,
    val categoryId: String,
    val isPurchased: Boolean = false
) {
    val totalPrice: Double get() = quantity * price
}

data class Category(
    val id: String,
    val name: String,
    val emoji: String,
    val color: Color
)

data class ShoppingListState(
    val items: List<ShoppingItem> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isAddDialogOpen: Boolean = false,
    
    // Form
    val formName: String = "",
    val formQuantity: String = "1",
    val formPrice: String = "",
    val formCategoryId: String = "",
    val nameError: String? = null,
    val priceError: String? = null,
    
    // Filters
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    
    // Budget
    val budgetLimit: Double = 200.0
) {
    // 🧠 DERIVED STATE - Computed on demand!
    val filteredItems: List<ShoppingItem>
        get() = items
            .filter { item ->
                (selectedCategoryId == null || item.categoryId == selectedCategoryId) &&
                (searchQuery.isEmpty() || item.name.contains(searchQuery, true))
            }
            .sortedBy { it.isPurchased }
    
    val totalSpent: Double get() = items.filter { it.isPurchased }.sumOf { it.totalPrice }
    val totalPlanned: Double get() = items.filterNot { it.isPurchased }.sumOf { it.totalPrice }
    val totalAll: Double get() = items.sumOf { it.totalPrice }
    val remainingBudget: Double get() = budgetLimit - totalAll
    val isOverBudget: Boolean get() = totalAll > budgetLimit
    val budgetPercentage: Float get() = (totalAll / budgetLimit).toFloat().coerceIn(0f, 1f)
    
    val isFormValid: Boolean
        get() = formName.isNotBlank() && 
                nameError == null && 
                priceError == null &&
                formQuantity.toIntOrNull() != null &&
                formCategoryId.isNotEmpty()
}

// ═══════════════════════════════════════════════════════════════════════════════
// 🎬 STEP 2: EVENTS
// ═══════════════════════════════════════════════════════════════════════════════

sealed class ShoppingEvent {
    data class AddItem(val name: String, val quantity: Int, val price: Double, val categoryId: String) : ShoppingEvent()
    data class TogglePurchased(val itemId: String) : ShoppingEvent()
    data class DeleteItem(val itemId: String) : ShoppingEvent()
    data class UpdateFormName(val name: String) : ShoppingEvent()
    data class UpdateFormQuantity(val quantity: String) : ShoppingEvent()
    data class UpdateFormPrice(val price: String) : ShoppingEvent()
    data class UpdateFormCategory(val categoryId: String) : ShoppingEvent()
    data class ShowAddDialog(val show: Boolean) : ShoppingEvent()
    data class SelectCategory(val categoryId: String?) : ShoppingEvent()
    data class UpdateSearch(val query: String) : ShoppingEvent()
}

// ═══════════════════════════════════════════════════════════════════════════════
// 🗄️ STEP 3: REPOSITORY PATTERN
// ═══════════════════════════════════════════════════════════════════════════════

/*
 * 💡 WHY REPOSITORY?
 * 
 * Separates "HOW to store" from "WHAT to store"
 * 
 * Tomorrow: Want Room DB? Just swap implementation!
 * ViewModel code stays THE SAME!
 */

interface ShoppingRepository {
    suspend fun getItems(): List<ShoppingItem>
    suspend fun addItem(item: ShoppingItem)
    suspend fun updateItem(item: ShoppingItem)
    suspend fun deleteItem(id: String)
    suspend fun getCategories(): List<Category>
}

class InMemoryShoppingRepository : ShoppingRepository {
    private val items = mutableListOf<ShoppingItem>()
    private val categories = listOf(
        Category("1", "Groceries", "🥗", Color(0xFF4CAF50)),
        Category("2", "Electronics", "📱", Color(0xFF2196F3)),
        Category("3", "Clothing", "👕", Color(0xFFE91E63)),
        Category("4", "Home", "🏠", Color(0xFFFF9800)),
        Category("5", "Health", "💊", Color(0xFF9C27B0))
    )
    
    override suspend fun getItems() = items.toList()
    override suspend fun addItem(item: ShoppingItem) { items.add(item) }
    override suspend fun updateItem(item: ShoppingItem) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index != -1) items[index] = item
    }
    override suspend fun deleteItem(id: String) { items.removeIf { it.id == id } }
    override suspend fun getCategories() = categories
}

// ═══════════════════════════════════════════════════════════════════════════════
// 🧠 STEP 4: VIEWMODEL
// ═══════════════════════════════════════════════════════════════════════════════

class ShoppingViewModel(
    private val repository: ShoppingRepository = InMemoryShoppingRepository()
) : ViewModel() {
    
    private val _state = MutableStateFlow(ShoppingListState())
    val state: StateFlow<ShoppingListState> = _state.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            val items = repository.getItems()
            val categories = repository.getCategories()
            _state.update { it.copy(
                items = items,
                categories = categories,
                formCategoryId = categories.firstOrNull()?.id ?: ""
            )}
        }
    }
    
    fun onEvent(event: ShoppingEvent) {
        when (event) {
            is ShoppingEvent.AddItem -> addItem(event)
            is ShoppingEvent.TogglePurchased -> togglePurchased(event.itemId)
            is ShoppingEvent.DeleteItem -> deleteItem(event.itemId)
            is ShoppingEvent.UpdateFormName -> updateFormName(event.name)
            is ShoppingEvent.UpdateFormQuantity -> _state.update { it.copy(formQuantity = event.quantity) }
            is ShoppingEvent.UpdateFormPrice -> updateFormPrice(event.price)
            is ShoppingEvent.UpdateFormCategory -> _state.update { it.copy(formCategoryId = event.categoryId) }
            is ShoppingEvent.ShowAddDialog -> showAddDialog(event.show)
            is ShoppingEvent.SelectCategory -> _state.update { it.copy(selectedCategoryId = event.categoryId) }
            is ShoppingEvent.UpdateSearch -> _state.update { it.copy(searchQuery = event.query) }
        }
    }
    
    private fun addItem(event: ShoppingEvent.AddItem) {
        if (event.name.isBlank()) {
            _state.update { it.copy(nameError = "Name required") }
            return
        }
        
        viewModelScope.launch {
            val item = ShoppingItem(
                name = event.name,
                quantity = event.quantity,
                price = event.price,
                categoryId = event.categoryId
            )
            repository.addItem(item)
            _state.update { it.copy(
                items = repository.getItems(),
                isAddDialogOpen = false,
                formName = "",
                formQuantity = "1",
                formPrice = "",
                nameError = null,
                priceError = null
            )}
        }
    }
    
    private fun togglePurchased(itemId: String) {
        viewModelScope.launch {
            val item = _state.value.items.find { it.id == itemId } ?: return@launch
            repository.updateItem(item.copy(isPurchased = !item.isPurchased))
            _state.update { it.copy(items = repository.getItems()) }
        }
    }
    
    private fun deleteItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteItem(itemId)
            _state.update { it.copy(items = repository.getItems()) }
        }
    }
    
    private fun updateFormName(name: String) {
        _state.update { it.copy(
            formName = name,
            nameError = when {
                name.isBlank() -> "Name required"
                name.length < 2 -> "Too short"
                else -> null
            }
        )}
    }
    
    private fun updateFormPrice(price: String) {
        _state.update { it.copy(
            formPrice = price,
            priceError = when {
                price.toDoubleOrNull() == null -> "Invalid price"
                price.toDouble() <= 0 -> "Must be positive"
                else -> null
            }
        )}
    }
    
    private fun showAddDialog(show: Boolean) {
        _state.update { it.copy(
            isAddDialogOpen = show,
            formName = if (!show) "" else it.formName,
            formQuantity = if (!show) "1" else it.formQuantity,
            formPrice = if (!show) "" else it.formPrice,
            nameError = null,
            priceError = null
        )}
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 🎨 STEP 5: UI LAYER
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListApp(viewModel: ShoppingViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Shopping List 🛒")
                        Text(
                            "${state.filteredItems.size} items • $${String.format("%.2f", state.totalAll)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(ShoppingEvent.ShowAddDialog(true)) }
            ) {
                Icon(Icons.Default.Add, "Add")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Budget Bar
            BudgetBar(state)
            
            // Search
            SearchBar(state, viewModel::onEvent)
            
            // Category Filters
            CategoryFilters(state, viewModel::onEvent)
            
            // Item List
            if (state.filteredItems.isEmpty()) {
                EmptyState()
            } else {
                ItemsList(state, viewModel::onEvent)
            }
        }
    }
    
    if (state.isAddDialogOpen) {
        AddItemDialog(state, viewModel::onEvent)
    }
}

@Composable
fun BudgetBar(state: ShoppingListState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (state.isOverBudget) 
                Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Spent: $${String.format("%.2f", state.totalAll)}", fontWeight = FontWeight.Bold)
                Text("Budget: $${String.format("%.2f", state.budgetLimit)}", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = state.budgetPercentage,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.medium),
                color = if (state.isOverBudget) Color.Red else Color.Green
            )
            if (state.isOverBudget) {
                Text(
                    "⚠️ Over budget!",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SearchBar(state: ShoppingListState, onEvent: (ShoppingEvent) -> Unit) {
    OutlinedTextField(
        value = state.searchQuery,
        onValueChange = { onEvent(ShoppingEvent.UpdateSearch(it)) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        placeholder = { Text("Search items...") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        singleLine = true
    )
}

@Composable
fun CategoryFilters(state: ShoppingListState, onEvent: (ShoppingEvent) -> Unit) {
    LazyRow(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = state.selectedCategoryId == null,
                onClick = { onEvent(ShoppingEvent.SelectCategory(null)) },
                label = { Text("All") }
            )
        }
        items(state.categories) { category ->
            FilterChip(
                selected = state.selectedCategoryId == category.id,
                onClick = { onEvent(ShoppingEvent.SelectCategory(category.id)) },
                label = { Text("${category.emoji} ${category.name}") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = category.color.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
fun ItemsList(state: ShoppingListState, onEvent: (ShoppingEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(state.filteredItems, key = { it.id }) { item ->
            ShoppingItemCard(item, state.categories, onEvent)
        }
    }
}

@Composable
fun ShoppingItemCard(
    item: ShoppingItem,
    categories: List<Category>,
    onEvent: (ShoppingEvent) -> Unit
) {
    val category = categories.find { it.id == item.categoryId }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEvent(ShoppingEvent.TogglePurchased(item.id)) }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isPurchased,
                onCheckedChange = { onEvent(ShoppingEvent.TogglePurchased(item.id)) }
            )
            
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            ) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (item.isPurchased) TextDecoration.LineThrough else null
                )
                Text(
                    "${category?.emoji} ${category?.name} • Qty: ${item.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$${String.format("%.2f", item.totalPrice)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    "$${String.format("%.2f", item.price)} each",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            IconButton(onClick = { onEvent(ShoppingEvent.DeleteItem(item.id)) }) {
                Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
            }
        }
    }
}

@Composable
fun AddItemDialog(state: ShoppingListState, onEvent: (ShoppingEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(ShoppingEvent.ShowAddDialog(false)) },
        title = { Text("Add New Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.formName,
                    onValueChange = { onEvent(ShoppingEvent.UpdateFormName(it)) },
                    label = { Text("Item Name *") },
                    isError = state.nameError != null,
                    supportingText = state.nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.formQuantity,
                        onValueChange = { onEvent(ShoppingEvent.UpdateFormQuantity(it)) },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    
                    OutlinedTextField(
                        value = state.formPrice,
                        onValueChange = { onEvent(ShoppingEvent.UpdateFormPrice(it)) },
                        label = { Text("Price *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = state.priceError != null,
                        supportingText = state.priceError?.let { { Text(it) } },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Text("Category", style = MaterialTheme.typography.labelMedium)
                state.categories.chunked(2).forEach { rowCategories ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowCategories.forEach { category ->
                            FilterChip(
                                selected = state.formCategoryId == category.id,
                                onClick = { onEvent(ShoppingEvent.UpdateFormCategory(category.id)) },
                                label = { Text("${category.emoji} ${category.name}") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onEvent(ShoppingEvent.AddItem(
                        name = state.formName,
                        quantity = state.formQuantity.toIntOrNull() ?: 1,
                        price = state.formPrice.toDoubleOrNull() ?: 0.0,
                        categoryId = state.formCategoryId
                    ))
                },
                enabled = state.isFormValid
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(ShoppingEvent.ShowAddDialog(false)) }) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🛒", fontSize = 64.sp)
            Text("No items yet", style = MaterialTheme.typography.titleLarge)
            Text("Tap + to add your first item", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MAIN ACTIVITY
// ═══════════════════════════════════════════════════════════════════════════════

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    ShoppingListApp()
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 🎓 LEARNING EXERCISES
// ═══════════════════════════════════════════════════════════════════════════════
/*
 * 🎯 MASTER THESE CONCEPTS:
 * 
 * ✅ Complex State Management
 *    - Multiple data types (Items, Categories, Budget)
 *    - Derived state (totals, filters computed automatically)
 *    - Form state with validation
 * 
 * ✅ Repository Pattern
 *    - Abstraction layer between ViewModel and data
 *    - Easy to swap implementations
 *    - Testable architecture
 * 
 * ✅ Business Logic
 *    - Budget tracking in ViewModel
 *    - Category filtering
 *    - Price calculations
 * 
 * 💪 EXERCISES:
 * □ Add "Edit Item" functionality
 * □ Add per-category budgets
 * □ Add sorting options (by price, name, category)
 * □ Add item notes/descriptions
 * □ Add shopping history/analytics
 * □ Persist data with Room database
 * □ Add item images
 * □ Add barcode scanning
 * 
 * 🚀 NEXT LEVEL:
 * After mastering this, you're ready for:
 * - Task Manager (Undo/Redo system)
 * - Real database integration (Room)
 * - Network calls (Retrofit)
 * - Authentication flows
 */