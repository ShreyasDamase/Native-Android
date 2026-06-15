# 📦 Snapshot State Collections — Complete Guide

> [!NOTE]
> **Sub-note for Snapshot Collections.** For local state primitives like `mutableStateOf` and `derivedStateOf`, see [[State]]. For Flow-based state, see [[StateFlow]] and [[SharedFlow]].

---

## 🧠 Why Do We Need Special Collections?

### The Problem with Regular Collections

Kotlin's standard `mutableListOf()`, `mutableMapOf()` etc. are **not observable**. When you add or remove an item from them, nothing in the outside world (including Compose) is notified. Compose has no way to know the collection changed.

```kotlin
// ❌ This does NOT trigger recomposition when items change
var items by remember { mutableStateOf(mutableListOf("A", "B")) }
items.add("C")  // Compose doesn't see this — UI stuck showing only A and B!
```

You might try the workaround of reassigning:

```kotlin
// This technically works, but it's awkward and error-prone
items = items.toMutableList().also { it.add("C") }
```

`mutableStateListOf` and friends solve this properly.

### How Snapshot Collections Work

Snapshot collections are built on the same Compose Snapshot system as `mutableStateOf`. Every mutation (`add`, `remove`, `set`, etc.) is automatically recorded as a state change, causing Compose to recompose any composable that was reading the collection.

They behave exactly like regular Kotlin collections (they implement the same interfaces), but every mutation notifies Compose.

---

## 🔷 1. `mutableStateListOf`

### Which Library
**Compose Runtime** — no extra dependency.

```kotlin
import androidx.compose.runtime.mutableStateListOf
```

### Basic Usage

```kotlin
@Composable
fun SimpleTaskList() {
    val tasks = remember { mutableStateListOf("Buy groceries", "Pay bills") }

    Column {
        tasks.forEach { task ->
            Text(text = "• $task")
        }
        Button(onClick = { tasks.add("New task ${tasks.size + 1}") }) {
            Text("Add Task")
        }
    }
}
```

### Full Practical Example — Todo App

```kotlin
data class Todo(
    val id: Int,
    val text: String,
    val isDone: Boolean = false
)

@Composable
fun TodoApp() {
    val todos = remember {
        mutableStateListOf(
            Todo(1, "Read a book"),
            Todo(2, "Go for a walk"),
            Todo(3, "Write code")
        )
    }
    var newTodoText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("My Todos", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Add new todo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newTodoText,
                onValueChange = { newTodoText = it },
                label = { Text("New todo") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (newTodoText.isNotBlank()) {
                        todos.add(Todo(System.currentTimeMillis().toInt(), newTodoText))
                        newTodoText = ""
                    }
                }
            ) {
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Todo list
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(todos, key = { it.id }) { todo ->
                TodoItem(
                    todo = todo,
                    onToggle = {
                        val index = todos.indexOf(todo)
                        // ✅ Replace the item (immutable copy pattern)
                        todos[index] = todo.copy(isDone = !todo.isDone)
                    },
                    onDelete = { todos.remove(todo) }
                )
            }
        }

        // Summary
        val doneCount = todos.count { it.isDone }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$doneCount of ${todos.size} completed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TodoItem(
    todo: Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = todo.isDone, onCheckedChange = { onToggle() })
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = todo.text,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (todo.isDone) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
```

### ⚠️ The Immutable Item Rule

```kotlin
// ❌ WRONG — data class with var properties
// Mutating a field does NOT notify Compose
data class Task(var name: String, var isDone: Boolean)
val tasks = remember { mutableStateListOf(Task("Buy milk", false)) }
tasks[0].isDone = true  // Compose WON'T see this — UI doesn't update!

// ✅ CORRECT — data class with val properties
// Use copy() to create a new instance
data class Task(val name: String, val isDone: Boolean)
val index = 0
tasks[index] = tasks[index].copy(isDone = true)  // New object = Compose sees it
```

This is because `mutableStateListOf` only observes **item replacement** (via `set`, `add`, `remove`). It cannot observe internal mutations to items. The items themselves must be immutable.

---

## 🔷 2. `mutableStateMapOf`

### Why It Exists
When you need a key-value mapping where changes to the map (adding/removing entries or updating values) should trigger recomposition.

### Which Library
**Compose Runtime** — no extra dependency.

```kotlin
import androidx.compose.runtime.mutableStateMapOf
```

### Full Practical Example — Online User Status Tracker

```kotlin
@Composable
fun OnlineUsersPanel() {
    // Map of userId → online status
    val userStatus = remember {
        mutableStateMapOf(
            "alice" to true,
            "bob" to false,
            "charlie" to true
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Team Status", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        userStatus.entries.sortedBy { it.key }.forEach { (userId, isOnline) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (isOnline) Color.Green else Color.Gray,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(userId.replaceFirstChar { it.uppercase() })
                }
                Switch(
                    checked = isOnline,
                    onCheckedChange = { userStatus[userId] = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val onlineCount = userStatus.values.count { it }
        Text(
            text = "$onlineCount / ${userStatus.size} online",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
```

### Full Practical Example — Form Field Errors Map

```kotlin
@Composable
fun ValidatedForm() {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    val errors = remember { mutableStateMapOf<String, String>() }

    fun validate(): Boolean {
        errors.clear()
        if (name.isBlank()) errors["name"] = "Name is required"
        if (!email.contains("@")) errors["email"] = "Enter a valid email"
        if (age.toIntOrNull() == null) errors["age"] = "Enter a valid age"
        else if (age.toInt() < 18) errors["age"] = "Must be 18 or older"
        return errors.isEmpty()
    }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormField("Name", name, { name = it }, errors["name"])
        FormField("Email", email, { email = it }, errors["email"])
        FormField("Age", age, { age = it }, errors["age"])

        Button(
            onClick = { if (validate()) { /* submit */ } },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit")
        }
    }
}

@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = error != null,
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
```

---

## 🔷 3. `toMutableStateList()` — Converting Existing Lists

When you have an existing list (from ViewModel, from a database, from an API) and want to make it mutable and observable inside a composable:

```kotlin
@Composable
fun EditableList(initialItems: List<String>) {
    // Convert the incoming immutable list to a snapshot list
    val items = remember(initialItems) {
        initialItems.toMutableStateList()
    }

    LazyColumn {
        items(items) { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(item)
                TextButton(onClick = { items.remove(item) }) {
                    Text("Remove")
                }
            }
        }
    }
}
```

> [!NOTE]
> `remember(initialItems)` means the list is re-created when `initialItems` changes. Use this pattern when the source data can change.

---

## 🔗 Connections

- **Compose State**: [[State]] — `remember` and `mutableStateOf` details
- **StateFlow representation**: [[StateFlow]] — how lists are held in ViewModels as StateFlow
- **Comparative Decision**: [[Comparison — State vs Flow vs StateFlow vs LiveData vs SharedFlow]]
