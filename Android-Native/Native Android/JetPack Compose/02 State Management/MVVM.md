# Jetpack Compose System Design

## From Beginner to Advanced - Complete Learning Guide

---

# 📖 Table of Contents

## Part 1: Foundation (Beginner)

- [Chapter 1: Introduction to System Design in Android](#chapter-1)
- [Chapter 2: MVVM Architecture Pattern](#chapter-2)
- [Chapter 3: State Management Fundamentals](#chapter-3)
- [Chapter 4: Unidirectional Data Flow (UDF)](#chapter-4)

## Part 2: Intermediate Concepts

- [Chapter 5: Repository Pattern](#chapter-5)
- [Chapter 6: Complex State Management](#chapter-6)
- [Chapter 7: Event-Driven Architecture](#chapter-7)
- [Chapter 8: Form Validation Patterns](#chapter-8)

## Part 3: Advanced Topics

- [Chapter 9: State Machines](#chapter-9)
- [Chapter 10: Undo/Redo System](#chapter-10)
- [Chapter 11: Testing Architecture](#chapter-11)
- [Chapter 12: Real-World Best Practices](#chapter-12)

---

# PART 1: FOUNDATION (BEGINNER)

---

# Chapter 1: Introduction to System Design in Android {#chapter-1}

## 1.1 What is System Design?

**System Design** in Android means: _"How do we organize code so the app doesn't become a mess?"_

### For Beginners, System Design Means:

- **App Architecture**: How to structure your Android app
- **Not Backend**: No servers, databases, or scaling (that comes later)
- **Clean Code Organization**: Making code maintainable and testable

### The Big Question You Must Answer:

> **"Where should this code live?"**

Every time you write code, ask:

- Should this be in UI (Composable)?
- Should this be in ViewModel?
- Should this be in Repository?

## 1.2 Why Architecture Matters

### ❌ Bad Architecture (What Beginners Do)

```kotlin
@Composable
fun TaskScreen() {
    var tasks = remember { mutableStateListOf<Task>() }
    var isLoading by remember { mutableStateOf(false) }
    
    // ❌ Business logic in UI
    Button(onClick = {
        isLoading = true
        // ❌ Direct data operations in Composable
        tasks.add(Task("New Task"))
        isLoading = false
    }) { Text("Add") }
}
```

**Problems:**

- State lost on rotation
- Can't test logic
- Code mixed together
- Hard to maintain

### ✅ Good Architecture

```kotlin
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val state by viewModel.state.collectAsState()
    
    // ✅ UI only displays and sends events
    Button(onClick = { viewModel.onEvent(AddTask) }) {
        Text("Add")
    }
}
```

**Benefits:**

- State survives rotation
- Logic is testable
- Clear separation
- Easy to maintain

## 1.3 The 4 Core Principles

### Principle 1: Single Source of Truth (SSOT)

**Definition:** State lives in ONE place only.

✅ **DO:**

```kotlin
// State in ViewModel (ONE place)
class TaskViewModel : ViewModel() {
    private val _state = MutableStateFlow(TaskState())
    val state: StateFlow<TaskState> = _state.asStateFlow()
}
```

❌ **DON'T:**

```kotlin
// State in multiple places
var tasks in ViewModel
var tasks in Repository
var tasks in UI
// Now you have 3 sources of truth = bugs!
```

### Principle 2: Unidirectional Data Flow (UDF)

**Definition:** Data flows in ONE direction only.

```
State flows DOWN:    ViewModel → UI
Events flow UP:      UI → ViewModel
```

### Principle 3: Immutability

**Definition:** State cannot be modified directly, only replaced with new copies.

✅ **DO:**

```kotlin
data class TaskState(val tasks: List<Task>)  // val = immutable
_state.update { it.copy(tasks = newTasks) }
```

### Principle 4: Separation of Concerns

**Definition:** Each layer has ONE responsibility.

|Layer|Responsibility|
|---|---|
|**UI**|Display state, send events|
|**ViewModel**|Own state, business logic|
|**Repository**|Data operations|
|**Data Source**|Actual storage|

---

# Chapter 2: MVVM Architecture Pattern {#chapter-2}

## 2.1 What is MVVM?

**MVVM** = Model - View - ViewModel

```
┌─────────────────────────────────────────────┐
│           UNIDIRECTIONAL FLOW               │
├─────────────────────────────────────────────┤
│                                             │
│  ┌─────────────┐                           │
│  │  ViewModel  │                           │
│  │   (State)   │                           │
│  └──────┬──────┘                           │
│         │ State flows DOWN                 │
│         ▼                                   │
│  ┌─────────────┐                           │
│  │     UI      │                           │
│  │ (Composable)│                           │
│  └──────┬──────┘                           │
│         │ Events flow UP                   │
│         ▼                                   │
│  ┌─────────────┐                           │
│  │  ViewModel  │                           │
│  └─────────────┘                           │
│                                             │
└─────────────────────────────────────────────┘
```

## 2.2 Each Component Explained

### MODEL: Business Data & Operations

```kotlin
// Business entity
data class Task(
    val id: String,
    val title: String,
    val isCompleted: Boolean
)

// Data operations
interface TaskRepository {
    suspend fun getTasks(): List<Task>
    suspend fun addTask(task: Task)
}
```

### VIEW: UI Layer (Composables)

```kotlin
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val state by viewModel.state.collectAsState()
    
    Text(text = "Tasks: ${state.tasks.size}")
    Button(onClick = { 
        viewModel.onEvent(AddTask("New Task")) 
    }) {
        Text("Add")
    }
}
```

### VIEWMODEL: The Brain

```kotlin
class TaskViewModel : ViewModel() {
    private val _state = MutableStateFlow(TaskState())
    val state: StateFlow<TaskState> = _state.asStateFlow()
    
    fun onEvent(event: TaskEvent) {
        when (event) {
            is AddTask -> addTask(event.title)
        }
    }
    
    private fun addTask(title: String) {
        viewModelScope.launch {
            repository.addTask(Task(title = title))
            _state.update { it.copy(tasks = repository.getTasks()) }
        }
    }
}
```

---

# Chapter 3: State Management Fundamentals {#chapter-3}

## 3.1 What is State?

**State** = Any data that can change over time and affects what the UI displays.

### Examples:

- List of tasks
- Loading indicator
- Error messages
- Form input values
- Selected filter option

## 3.2 StateFlow Pattern

```kotlin
class TaskViewModel : ViewModel() {
    // Private mutable state
    private val _state = MutableStateFlow(TaskState())
    // Public read-only state
    val state: StateFlow<TaskState> = _state.asStateFlow()
}
```

## 3.3 The .update { } Pattern

```kotlin
// Update single property
_state.update { it.copy(isLoading = true) }

// Update multiple properties
_state.update { it.copy(
    isLoading = false,
    tasks = newTasks,
    error = null
)}

// Update list (add item)
_state.update { it.copy(
    tasks = it.tasks + newTask
)}
```

## 3.4 Derived State

```kotlin
data class TaskScreenState(
    val tasks: List<Task>,
    val searchQuery: String
) {
    val filteredTasks: List<Task>
        get() = tasks.filter { 
            it.title.contains(searchQuery, ignoreCase = true) 
        }
    
    val completedCount: Int
        get() = tasks.count { it.isCompleted }
}
```

---

# Chapter 4: Unidirectional Data Flow (UDF) {#chapter-4}

## 4.1 What is UDF?

**Unidirectional Data Flow** = Data flows in ONE direction only.

- State flows DOWN: ViewModel → UI
- Events flow UP: UI → ViewModel

## 4.2 Event Definition

```kotlin
sealed class TaskEvent {
    data class AddTask(val title: String) : TaskEvent()
    data class DeleteTask(val id: String) : TaskEvent()
    data class ToggleComplete(val id: String) : TaskEvent()
    data object Refresh : TaskEvent()
}
```

## 4.3 Complete UDF Example

```kotlin
// STATE (flows down)
data class CounterState(
    val count: Int = 0,
    val message: String = ""
)

// EVENTS (flow up)
sealed class CounterEvent {
    data object Increment : CounterEvent()
    data object Decrement : CounterEvent()
    data object Reset : CounterEvent()
}

// VIEWMODEL
class CounterViewModel : ViewModel() {
    private val _state = MutableStateFlow(CounterState())
    val state: StateFlow<CounterState> = _state.asStateFlow()
    
    fun onEvent(event: CounterEvent) {
        when (event) {
            CounterEvent.Increment -> {
                _state.update { it.copy(count = it.count + 1) }
            }
            CounterEvent.Decrement -> {
                _state.update { it.copy(count = it.count - 1) }
            }
            CounterEvent.Reset -> {
                _state.update { CounterState() }
            }
        }
    }
}

// UI
@Composable
fun CounterScreen(viewModel: CounterViewModel) {
    val state by viewModel.state.collectAsState()
    
    Column {
        Text("Count: ${state.count}")
        Button(onClick = { viewModel.onEvent(CounterEvent.Increment) }) {
            Text("+")
        }
    }
}
```

---

# PART 2: INTERMEDIATE CONCEPTS

---

# Chapter 5: Repository Pattern {#chapter-5}

## 5.1 What is Repository?

**Repository** = A layer that abstracts data operations.

```
ViewModel → Repository Interface → Implementation → Data Source
```

## 5.2 Creating a Repository

### Step 1: Define Interface

```kotlin
interface TaskRepository {
    suspend fun getTasks(): List<Task>
    suspend fun getTask(id: String): Task?
    suspend fun addTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(id: String)
}
```

### Step 2: Create Implementation

```kotlin
class TaskRepositoryImpl(
    private val taskDao: TaskDao
) : TaskRepository {
    
    override suspend fun getTasks(): List<Task> {
        return taskDao.getAllTasks()
    }
    
    override suspend fun addTask(task: Task) {
        taskDao.insert(task)
    }
    
    // ... other methods
}
```

### Step 3: Use in ViewModel

```kotlin
class TaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {
    
    private fun loadTasks() {
        viewModelScope.launch {
            val tasks = repository.getTasks()
            _state.update { it.copy(tasks = tasks) }
        }
    }
}
```

## 5.3 In-Memory Repository (For Learning)

```kotlin
class InMemoryTaskRepository : TaskRepository {
    private val tasks = mutableListOf<Task>()
    
    override suspend fun getTasks(): List<Task> {
        return tasks.toList()
    }
    
    override suspend fun addTask(task: Task) {
        tasks.add(task)
    }
}
```

---

# Chapter 6: Complex State Management {#chapter-6}

## 6.1 Grouped State

```kotlin
// Group related state
data class TaskScreenState(
    val formState: TaskFormState,
    val filterState: FilterState
)

data class TaskFormState(
    val title: String = "",
    val description: String = "",
    val titleError: String? = null
)
```

## 6.2 Loading States

```kotlin
sealed class LoadingState<out T> {
    data object Idle : LoadingState<Nothing>()
    data object Loading : LoadingState<Nothing>()
    data class Success<T>(val data: T) : LoadingState<T>()
    data class Error(val message: String) : LoadingState<Nothing>()
}
```

## 6.3 Managing Lists

```kotlin
// Add item
_state.update { it.copy(tasks = it.tasks + task) }

// Remove item
_state.update { it.copy(
    tasks = it.tasks.filter { t -> t.id != taskId }
)}

// Update item
_state.update { current ->
    current.copy(
        tasks = current.tasks.map { task ->
            if (task.id == taskId) {
                task.copy(isCompleted = !task.isCompleted)
            } else task
        }
    )
}
```

---

# Chapter 7: Event-Driven Architecture {#chapter-7}

## 7.1 Event Design

```kotlin
sealed class TaskEvent {
    // Task operations
    data class AddTask(
        val title: String,
        val description: String
    ) : TaskEvent()
    
    data class DeleteTask(val taskId: String) : TaskEvent()
    
    // Form events
    data class UpdateTitle(val title: String) : TaskEvent()
    data class UpdateDescription(val text: String) : TaskEvent()
    
    // UI events
    data class ShowDialog(val show: Boolean) : TaskEvent()
    data object Submit : TaskEvent()
}
```

## 7.2 Event Handling

```kotlin
class TaskViewModel : ViewModel() {
    fun onEvent(event: TaskEvent) {
        when (event) {
            is TaskEvent.AddTask -> handleAddTask(event)
            is TaskEvent.DeleteTask -> handleDeleteTask(event)
            is TaskEvent.UpdateTitle -> handleUpdateTitle(event)
        }
    }
    
    private fun handleAddTask(event: TaskEvent.AddTask) {
        // Validation
        if (event.title.isBlank()) {
            _state.update { it.copy(error = "Title required") }
            return
        }
        
        // Business logic
        viewModelScope.launch {
            repository.addTask(Task(title = event.title))
            _state.update { it.copy(
                tasks = repository.getTasks()
            )}
        }
    }
}
```

---

# Chapter 8: Form Validation Patterns {#chapter-8}

## 8.1 Form State Design

```kotlin
data class TaskFormState(
    val title: String = "",
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    
    // Validation errors
    val titleError: String? = null,
    
    // Form metadata
    val isSubmitting: Boolean = false
) {
    val isValid: Boolean
        get() = title.isNotBlank() && titleError == null
}
```

## 8.2 Live Validation

```kotlin
fun updateTitle(title: String) {
    _state.update { current ->
        current.copy(
            formTitle = title,
            titleError = validateTitle(title)
        )
    }
}

private fun validateTitle(title: String): String? {
    return when {
        title.isBlank() -> "Title is required"
        title.length < 3 -> "Title too short (min 3)"
        title.length > 100 -> "Title too long (max 100)"
        else -> null
    }
}
```

## 8.3 Form Submission

```kotlin
fun submitForm() {
    val state = _state.value
    
    // Final validation
    if (!state.isFormValid) {
        return
    }
    
    viewModelScope.launch {
        _state.update { it.copy(isSubmitting = true) }
        
        try {
            val task = Task(
                title = state.formTitle,
                description = state.formDescription
            )
            repository.addTask(task)
            
            // Reset form on success
            _state.update { it.copy(
                formTitle = "",
                formDescription = "",
                isSubmitting = false
            )}
        } catch (e: Exception) {
            _state.update { it.copy(
                error = e.message,
                isSubmitting = false
            )}
        }
    }
}
```

---

# PART 3: ADVANCED TOPICS

---

# Chapter 9: State Machines {#chapter-9}

## 9.1 What is a State Machine?

A **State Machine** defines all possible states and transitions between them.

### Example: Task Detail Screen

```kotlin
sealed class TaskDetailState {
    data object Loading : TaskDetailState()
    data class Viewing(val task: Task) : TaskDetailState()
    data class Editing(val task: Task, val formState: FormState) : TaskDetailState()
    data class Deleting(val task: Task) : TaskDetailState()
    data class Error(val message: String) : TaskDetailState()
}
```

## 9.2 State Transitions

```kotlin
class TaskDetailViewModel : ViewModel() {
    private val _state = MutableStateFlow<TaskDetailState>(
        TaskDetailState.Loading
    )
    val state = _state.asStateFlow()
    
    fun loadTask(taskId: String) {
        viewModelScope.launch {
            _state.value = TaskDetailState.Loading
            
            try {
                val task = repository.getTask(taskId)
                _state.value = TaskDetailState.Viewing(task)
            } catch (e: Exception) {
                _state.value = TaskDetailState.Error(e.message ?: "Error")
            }
        }
    }
    
    fun startEditing() {
        val current = _state.value
        if (current is TaskDetailState.Viewing) {
            _state.value = TaskDetailState.Editing(
                task = current.task,
                formState = FormState.from(current.task)
            )
        }
    }
    
    fun cancelEditing() {
        val current = _state.value
        if (current is TaskDetailState.Editing) {
            _state.value = TaskDetailState.Viewing(current.task)
        }
    }
}
```

## 9.3 UI Based on State

```kotlin
@Composable
fun TaskDetailScreen(viewModel: TaskDetailViewModel) {
    val state by viewModel.state.collectAsState()
    
    when (state) {
        TaskDetailState.Loading -> {
            LoadingScreen()
        }
        is TaskDetailState.Viewing -> {
            ViewingScreen(
                task = state.task,
                onEdit = { viewModel.startEditing() }
            )
        }
        is TaskDetailState.Editing -> {
            EditingScreen(
                task = state.task,
                formState = state.formState,
                onSave = { viewModel.saveChanges() },
                onCancel = { viewModel.cancelEditing() }
            )
        }
        is TaskDetailState.Error -> {
            ErrorScreen(message = state.message)
        }
    }
}
```

---

# Chapter 10: Undo/Redo System {#chapter-10}

## 10.1 How Undo/Redo Works

Store immutable state copies in history stacks:

```
History Stack:    [State1, State2, State3] ← Current
Future Stack:     []

After Undo:
History Stack:    [State1, State2]
Current:          State3
Future Stack:     [State3]

After Redo:
History Stack:    [State1, State2, State3] ← Current
Future Stack:     []
```

## 10.2 Implementation

```kotlin
class TaskViewModel : ViewModel() {
    private val _state = MutableStateFlow(TaskScreenState())
    val state = _state.asStateFlow()
    
    private val history = mutableListOf<TaskScreenState>()
    private val future = mutableListOf<TaskScreenState>()
    
    private fun saveToHistory() {
        history.add(_state.value)
        future.clear()
        updateHistoryFlags()
    }
    
    fun undo() {
        if (history.isEmpty()) return
        
        future.add(_state.value)
        _state.value = history.removeLast()
        updateHistoryFlags()
    }
    
    fun redo() {
        if (future.isEmpty()) return
        
        history.add(_state.value)
        _state.value = future.removeLast()
        updateHistoryFlags()
    }
    
    private fun updateHistoryFlags() {
        _state.update { it.copy(
            canUndo = history.isNotEmpty(),
            canRedo = future.isNotEmpty()
        )}
    }
}
```

## 10.3 Using Undo/Redo

```kotlin
// Before modifying state
fun deleteTask(taskId: String) {
    saveToHistory()  // Save current state
    
    viewModelScope.launch {
        repository.deleteTask(taskId)
        _state.update { it.copy(
            tasks = repository.getTasks()
        )}
    }
}

// In UI
@Composable
fun TopBar(state: TaskScreenState, onEvent: (Event) -> Unit) {
    IconButton(
        onClick = { onEvent(TaskEvent.Undo) },
        enabled = state.canUndo
    ) {
        Icon(Icons.Default.Undo, "Undo")
    }
    
    IconButton(
        onClick = { onEvent(TaskEvent.Redo) },
        enabled = state.canRedo
    ) {
        Icon(Icons.Default.Redo, "Redo")
    }
}
```

---

# Chapter 11: Testing Architecture {#chapter-11}

## 11.1 Why Test?

- Catch bugs early
- Refactor with confidence
- Document behavior
- Faster development (in the long run)

## 11.2 Testing ViewModel

```kotlin
class TaskViewModelTest {
    
    private lateinit var viewModel: TaskViewModel
    private lateinit var fakeRepository: FakeTaskRepository
    
    @Before
    fun setup() {
        fakeRepository = FakeTaskRepository()
        viewModel = TaskViewModel(fakeRepository)
    }
    
    @Test
    fun `adding task updates state`() = runTest {
        // Given
        val initialSize = viewModel.state.value.tasks.size
        
        // When
        viewModel.onEvent(TaskEvent.AddTask("New Task"))
        
        // Then
        assertEquals(initialSize + 1, viewModel.state.value.tasks.size)
        assertEquals("New Task", viewModel.state.value.tasks.last().title)
    }
    
    @Test
    fun `adding task with blank title shows error`() = runTest {
        // When
        viewModel.onEvent(TaskEvent.AddTask(""))
        
        // Then
        assertNotNull(viewModel.state.value.titleError)
        assertEquals(0, viewModel.state.value.tasks.size)
    }
    
    @Test
    fun `deleting task removes it from state`() = runTest {
        // Given
        viewModel.onEvent(TaskEvent.AddTask("Task 1"))
        val taskId = viewModel.state.value.tasks.first().id
        
        // When
        viewModel.onEvent(TaskEvent.DeleteTask(taskId))
        
        // Then
        assertEquals(0, viewModel.state.value.tasks.size)
    }
}
```

## 11.3 Fake Repository

```kotlin
class FakeTaskRepository : TaskRepository {
    private val tasks = mutableListOf<Task>()
    var shouldReturnError = false
    
    override suspend fun getTasks(): List<Task> {
        if (shouldReturnError) throw Exception("Test error")
        return tasks.toList()
    }
    
    override suspend fun addTask(task: Task) {
        if (shouldReturnError) throw Exception("Test error")
        tasks.add(task)
    }
    
    override suspend fun deleteTask(id: String) {
        tasks.removeIf { it.id == id }
    }
}
```

## 11.4 Testing State Transformations

```kotlin
@Test
fun `filtering tasks by category works`() {
    // Given
    val state = TaskScreenState(
        tasks = listOf(
            Task(id = "1", title = "Work Task", categoryId = "work"),
            Task(id = "2", title = "Personal Task", categoryId = "personal")
        ),
        selectedCategory = "work"
    )
    
    // When
    val filtered = state.filteredTasks
    
    // Then
    assertEquals(1, filtered.size)
    assertEquals("Work Task", filtered.first().title)
}
```

---

# Chapter 12: Real-World Best Practices {#chapter-12}

## 12.1 Project Structure

```
app/
├── data/
│   ├── local/
│   │   ├── TaskDao.kt
│   │   └── TaskDatabase.kt
│   ├── remote/
│   │   └── TaskApi.kt
│   └── repository/
│       ├── TaskRepository.kt
│       └── TaskRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   ├── Task.kt
│   │   └── Category.kt
│   └── usecase/
│       └── GetTasksUseCase.kt
├── presentation/
│   ├── task_list/
│   │   ├── TaskListScreen.kt
│   │   ├── TaskListViewModel.kt
│   │   ├── TaskListState.kt
│   │   └── TaskListEvent.kt
│   └── task_detail/
│       ├── TaskDetailScreen.kt
│       └── TaskDetailViewModel.kt
└── di/
    └── AppModule.kt
```

## 12.2 Dependency Injection

```kotlin
// Hilt example
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideTaskDatabase(app: Application): TaskDatabase {
        return Room.databaseBuilder(
            app,
            TaskDatabase::class.java,
            "task_db"
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideTaskRepository(db: TaskDatabase): TaskRepository {
        return TaskRepositoryImpl(db.taskDao())
    }
}

// ViewModel
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {
    // ...
}
```

## 12.3 Error Handling

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

class TaskViewModel : ViewModel() {
    private val _tasksResult = MutableStateFlow<Result<List<Task>>>(
        Result.Loading
    )
    val tasksResult = _tasksResult.asStateFlow()
    
    fun loadTasks() {
        viewModelScope.launch {
            _tasksResult.value = Result.Loading
            
            try {
                val tasks = repository.getTasks()
                _tasksResult.value = Result.Success(tasks)
            } catch (e: Exception) {
                _tasksResult.value = Result.Error(
                    e.message ?: "Unknown error"
                )
            }
        }
    }
}
```

## 12.4 Performance Tips

### 1. Avoid Unnecessary Recompositions

```kotlin
@Composable
fun TaskItem(
    task: Task,
    onToggle: (String) -> Unit
) {
    // ✅ Use remember for lambda
    val onClick = remember(task.id) {
        { onToggle(task.id) }
    }
    
    Card(onClick = onClick) {
        Text(task.title)
    }
}
```

### 2. Use Keys in Lists

```kotlin
LazyColumn {
    items(
        items = tasks,
        key = { task -> task.id }  // ✅ Always use keys
    ) { task ->
        TaskItem(task)
    }
}
```

### 3. Derive State Instead of Storing

```kotlin
// ✅ GOOD: Compute on read
val filteredTasks: List<Task>
    get() = tasks.filter { it.isCompleted }

// ❌ BAD: Store duplicate data
val filteredTasks: List<Task> = emptyList()
```

## 12.5 Common Pitfalls

### 1. Don't Store Context in ViewModel

```kotlin
// ❌ WRONG
class TaskViewModel(
    private val context: Context  // ❌ Memory leak!
) : ViewModel()

// ✅ CORRECT
class TaskViewModel(
    private val application: Application  // ✅ OK
) : ViewModel()
```

### 2. Don't Expose Mutable State

```kotlin
// ❌ WRONG
class TaskViewModel : ViewModel() {
    val state = MutableStateFlow(TaskState())  // ❌ Public mutable
}

// ✅ CORRECT
class TaskViewModel : ViewModel() {
    private val _state = MutableStateFlow(TaskState())
    val state = _state.asStateFlow()  // ✅ Public immutable
}
```

### 3. Don't Block Main Thread

```kotlin
// ❌ WRONG
fun loadTasks() {
    val tasks = runBlocking {  // ❌ Blocks UI
        repository.getTasks()
    }
    _state.update { it.copy(tasks = tasks) }
}

// ✅ CORRECT
fun loadTasks() {
    viewModelScope.launch {  // ✅ Async
        val tasks = repository.getTasks()
        _state.update { it.copy(tasks = tasks) }
    }
}
```

---

# 📚 Learning Path

## Week 1: Foundation

- Study Chapters 1-4
- Understand MVVM, State, and UDF
- Build a simple counter app
- Practice state updates

## Week 2: Intermediate

- Study Chapters 5-8
- Learn Repository pattern
- Build a note-taking app
- Add form validation

## Week 3: Advanced

- Study Chapters 9-12
- Implement state machines
- Add undo/redo
- Write tests

## Week 4: Build Your Own App

- Apply all concepts
- Choose a domain (fitness, finance, etc.)
- Build complete MVVM architecture
- Test thoroughly

---

# 🎯 Practice Exercises

## Exercise 1: Counter App

Build a counter with:

- Increment/Decrement
- Reset
- Undo/Redo
- State persistence

## Exercise 2: Todo App

Build a todo app with:

- Add/Delete tasks
- Mark complete
- Filter by category
- Search functionality

## Exercise 3: Shopping List

Build a shopping list with:

- Add items with quantity
- Categories
- Price tracking
- Budget warnings

## Exercise 4: Habit Tracker

Build a habit tracker with:

- Daily habit checklist
- Streak counting
- Statistics dashboard
- Reminder system
- Progress charts

## Exercise 5: Expense Tracker

Build an expense tracker with:

- Add income/expenses
- Category-based filtering
- Monthly budgets
- Charts and reports
- Export to CSV

---

# 🚀 Advanced Topics Deep Dive

## A1: Navigation with State

### Problem: Losing State on Navigation

```kotlin
// ❌ BAD: State lost when navigating
@Composable
fun TaskListScreen() {
    var searchQuery by remember { mutableStateOf("") }
    // Lost when navigating away!
}
```

### Solution: State in ViewModel

```kotlin
// ✅ GOOD: State preserved
class TaskListViewModel : ViewModel() {
    private val _state = MutableStateFlow(TaskListState())
    val state = _state.asStateFlow()
}

// State survives navigation
@Composable
fun TaskListScreen(viewModel: TaskListViewModel) {
    val state by viewModel.state.collectAsState()
}
```

### Navigation Events

```kotlin
sealed class TaskEvent {
    data class NavigateToDetail(val taskId: String) : TaskEvent()
    data object NavigateToSettings : TaskEvent()
    data object NavigateBack : TaskEvent()
}

class TaskViewModel : ViewModel() {
    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()
    
    fun onEvent(event: TaskEvent) {
        when (event) {
            is TaskEvent.NavigateToDetail -> {
                viewModelScope.launch {
                    _navigationEvent.send(
                        NavigationEvent.ToDetail(event.taskId)
                    )
                }
            }
        }
    }
}

// In UI
LaunchedEffect(Unit) {
    viewModel.navigationEvent.collect { event ->
        when (event) {
            is NavigationEvent.ToDetail -> {
                navController.navigate("detail/${event.taskId}")
            }
        }
    }
}
```

## A2: Side Effects Management

### LaunchedEffect for One-Time Events

```kotlin
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val state by viewModel.state.collectAsState()
    
    // Execute once when screen appears
    LaunchedEffect(Unit) {
        viewModel.loadTasks()
    }
    
    // Execute when specific value changes
    LaunchedEffect(state.selectedCategory) {
        viewModel.loadTasksForCategory()
    }
}
```

### Handling Snackbar Messages

```kotlin
class TaskViewModel : ViewModel() {
    private val _snackbarMessage = Channel<String>()
    val snackbarMessage = _snackbarMessage.receiveAsFlow()
    
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                repository.deleteTask(taskId)
                _snackbarMessage.send("Task deleted")
            } catch (e: Exception) {
                _snackbarMessage.send("Error: ${e.message}")
            }
        }
    }
}

@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { /* ... */ }
}
```

## A3: Pagination Pattern

```kotlin
data class PaginationState<T>(
    val items: List<T> = emptyList(),
    val page: Int = 0,
    val isLoading: Boolean = false,
    val isLastPage: Boolean = false,
    val error: String? = null
)

class TaskViewModel : ViewModel() {
    private val _state = MutableStateFlow(
        PaginationState<Task>()
    )
    val state = _state.asStateFlow()
    
    fun loadNextPage() {
        if (_state.value.isLoading || _state.value.isLastPage) return
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                val nextPage = _state.value.page + 1
                val newTasks = repository.getTasks(
                    page = nextPage,
                    pageSize = 20
                )
                
                _state.update { it.copy(
                    items = it.items + newTasks,
                    page = nextPage,
                    isLoading = false,
                    isLastPage = newTasks.isEmpty()
                )}
            } catch (e: Exception) {
                _state.update { it.copy(
                    error = e.message,
                    isLoading = false
                )}
            }
        }
    }
}

@Composable
fun InfiniteTaskList(viewModel: TaskViewModel) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    
    LazyColumn(state = listState) {
        items(state.items) { task ->
            TaskItem(task)
        }
        
        if (state.isLoading) {
            item { LoadingItem() }
        }
    }
    
    // Detect when user scrolls to bottom
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val lastVisibleItem = visibleItems.lastOrNull()
                val shouldLoadMore = lastVisibleItem?.index == state.items.size - 1
                
                if (shouldLoadMore && !state.isLoading) {
                    viewModel.loadNextPage()
                }
            }
    }
}
```

## A4: Search with Debouncing

```kotlin
class TaskViewModel : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    
    init {
        // Debounce search: wait 300ms after user stops typing
        searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                searchTasks(query)
            }
            .launchIn(viewModelScope)
    }
    
    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }
    
    private fun searchTasks(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            
            val results = repository.searchTasks(query)
            
            _state.update { it.copy(
                searchResults = results,
                isSearching = false
            )}
        }
    }
}
```

## A5: Caching Strategy

```kotlin
class TaskRepositoryImpl(
    private val localDataSource: TaskDao,
    private val remoteDataSource: TaskApi
) : TaskRepository {
    
    // Cache duration
    private val cacheTimeout = 5.minutes
    private var lastFetchTime: Instant? = null
    
    override suspend fun getTasks(): List<Task> {
        // Check if cache is fresh
        val now = Clock.System.now()
        val cacheExpired = lastFetchTime?.let { 
            now - it > cacheTimeout 
        } ?: true
        
        return if (cacheExpired) {
            // Fetch from network
            try {
                val remoteTasks = remoteDataSource.getTasks()
                // Update local cache
                localDataSource.deleteAll()
                localDataSource.insertAll(remoteTasks)
                lastFetchTime = now
                remoteTasks
            } catch (e: Exception) {
                // Network error: fallback to cache
                localDataSource.getAllTasks()
            }
        } else {
            // Return cached data
            localDataSource.getAllTasks()
        }
    }
}
```

## A6: Optimistic Updates

```kotlin
fun toggleTaskCompletion(taskId: String) {
    // 1. Update UI immediately (optimistic)
    _state.update { current ->
        current.copy(
            tasks = current.tasks.map { task ->
                if (task.id == taskId) {
                    task.copy(isCompleted = !task.isCompleted)
                } else task
            }
        )
    }
    
    // 2. Update backend
    viewModelScope.launch {
        try {
            val task = _state.value.tasks.find { it.id == taskId }
            if (task != null) {
                repository.updateTask(task)
            }
        } catch (e: Exception) {
            // 3. Revert on error
            _state.update { current ->
                current.copy(
                    tasks = current.tasks.map { task ->
                        if (task.id == taskId) {
                            task.copy(isCompleted = !task.isCompleted)
                        } else task
                    },
                    error = "Failed to update: ${e.message}"
                )
            }
        }
    }
}
```

---

# 💡 Design Patterns Comparison

## Pattern 1: MVI vs MVVM

### MVVM (What We Use)

```kotlin
// State
data class TaskState(val tasks: List<Task>)

// Events
sealed class TaskEvent {
    data class AddTask(val title: String) : TaskEvent()
}

// ViewModel
class TaskViewModel : ViewModel() {
    private val _state = MutableStateFlow(TaskState())
    val state = _state.asStateFlow()
    
    fun onEvent(event: TaskEvent) { }
}
```

### MVI (Alternative)

```kotlin
// Intent (same as Event)
sealed class TaskIntent {
    data class AddTask(val title: String) : TaskIntent()
}

// State (same)
data class TaskState(val tasks: List<Task>)

// ViewModel
class TaskViewModel : ViewModel() {
    private val _state = MutableStateFlow(TaskState())
    val state = _state.asStateFlow()
    
    // MVI: Intents processed through single channel
    private val intentChannel = Channel<TaskIntent>()
    
    init {
        processIntents()
    }
    
    fun sendIntent(intent: TaskIntent) {
        viewModelScope.launch {
            intentChannel.send(intent)
        }
    }
    
    private fun processIntents() {
        viewModelScope.launch {
            intentChannel.consumeAsFlow().collect { intent ->
                when (intent) {
                    is TaskIntent.AddTask -> handleAddTask(intent)
                }
            }
        }
    }
}
```

**Conclusion:** MVVM is simpler for most apps. MVI adds complexity with channels.

## Pattern 2: UseCase Layer

### Without UseCase (Simple)

```kotlin
class TaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {
    fun loadTasks() {
        viewModelScope.launch {
            val tasks = repository.getTasks()
            _state.update { it.copy(tasks = tasks) }
        }
    }
}
```

### With UseCase (Complex Business Logic)

```kotlin
class GetTasksUseCase(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(
        sortBy: SortOption,
        filterCompleted: Boolean
    ): List<Task> {
        val tasks = repository.getTasks()
        
        // Complex business logic
        return tasks
            .filter { if (filterCompleted) it.isCompleted else !it.isCompleted }
            .sortedWith(
                when (sortBy) {
                    SortOption.PRIORITY -> compareByDescending { it.priority }
                    SortOption.DATE -> compareByDescending { it.createdAt }
                    SortOption.TITLE -> compareBy { it.title }
                }
            )
    }
}

class TaskViewModel(
    private val getTasksUseCase: GetTasksUseCase
) : ViewModel() {
    fun loadTasks() {
        viewModelScope.launch {
            val tasks = getTasksUseCase(
                sortBy = _state.value.sortBy,
                filterCompleted = _state.value.filterCompleted
            )
            _state.update { it.copy(tasks = tasks) }
        }
    }
}
```

**When to use UseCases:**

- ✅ Complex business logic
- ✅ Logic reused across ViewModels
- ✅ Large enterprise apps
- ❌ Simple CRUD operations

---

# 🎓 Interview Questions & Answers

## Q1: Why use StateFlow instead of LiveData?

**Answer:** StateFlow is:

1. **Kotlin-native**: Works in multiplatform projects
2. **Coroutine-based**: Better integration with suspend functions
3. **Always has value**: No null initial state issues
4. **Type-safe**: Better compile-time checks
5. **Recommended by Google**: Modern best practice for Compose

## Q2: Why make state immutable?

**Answer:**

1. **Thread-safety**: No race conditions
2. **Predictable**: State changes are explicit
3. **Time-travel debugging**: Can track all state changes
4. **Undo/Redo**: Easy to implement with state snapshots
5. **Compose optimization**: Compose can detect changes efficiently

## Q3: What's the difference between State and Event?

**Answer:**

- **State**: Data that persists (current view of data)
- **Event**: One-time action (user clicks, API calls)

```kotlin
// State: Persists
data class TaskState(val tasks: List<Task>)

// Event: Happens once
sealed class TaskEvent {
    data class ShowSnackbar(val message: String) : TaskEvent()
}
```

## Q4: Should validation be in ViewModel or Repository?

**Answer:** **ViewModel**

- **Repository**: Data operations only (get, save, delete)
- **ViewModel**: Business logic including validation

```kotlin
// ✅ CORRECT
class TaskViewModel : ViewModel() {
    fun addTask(title: String) {
        // Validation in ViewModel
        if (title.length < 3) {
            _state.update { it.copy(error = "Title too short") }
            return
        }
        repository.addTask(Task(title = title))
    }
}
```

## Q5: How to handle configuration changes?

**Answer:** Use ViewModel - it survives configuration changes automatically.

```kotlin
// ViewModel survives rotation
class TaskViewModel : ViewModel() {
    private val _state = MutableStateFlow(TaskState())
    val state = _state.asStateFlow()
}

// UI recreates, but ViewModel persists
@Composable
fun TaskScreen(
    viewModel: TaskViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    // State is preserved across rotations
}
```

---

# 📝 Common Mistakes & Solutions

## Mistake 1: Storing Context in ViewModel

```kotlin
// ❌ WRONG: Memory leak!
class TaskViewModel(private val context: Context) : ViewModel()

// ✅ CORRECT: Use Application
class TaskViewModel(private val app: Application) : ViewModel()
```

## Mistake 2: Blocking Main Thread

```kotlin
// ❌ WRONG: Freezes UI
fun loadTasks() {
    runBlocking {
        val tasks = repository.getTasks()
    }
}

// ✅ CORRECT: Async
fun loadTasks() {
    viewModelScope.launch {
        val tasks = repository.getTasks()
    }
}
```

## Mistake 3: Exposing Mutable State

```kotlin
// ❌ WRONG: UI can modify
class TaskViewModel : ViewModel() {
    val state = MutableStateFlow(TaskState())
}

// ✅ CORRECT: Private mutable, public immutable
class TaskViewModel : ViewModel() {
    private val _state = MutableStateFlow(TaskState())
    val state = _state.asStateFlow()
}
```

## Mistake 4: Duplicate State

```kotlin
// ❌ WRONG: Same data in multiple places
data class TaskState(
    val tasks: List<Task>,
    val completedTasks: List<Task>  // Duplicate!
)

// ✅ CORRECT: Compute on read
data class TaskState(
    val tasks: List<Task>
) {
    val completedTasks: List<Task>
        get() = tasks.filter { it.isCompleted }
}
```

## Mistake 5: Business Logic in UI

```kotlin
// ❌ WRONG
@Composable
fun TaskScreen() {
    Button(onClick = {
        if (title.length < 3) {  // ❌ Validation in UI
            showError()
        }
    })
}

// ✅ CORRECT
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    Button(onClick = {
        viewModel.onEvent(AddTask(title))  // ✅ Logic in ViewModel
    })
}
```

---

# 🔧 Debugging Tips

## Tip 1: Log State Changes

```kotlin
class TaskViewModel : ViewModel() {
    private val _state = MutableStateFlow(TaskState())
    val state = _state.asStateFlow()
    
    init {
        viewModelScope.launch {
            state.collect { newState ->
                Log.d("TaskViewModel", "State: $newState")
            }
        }
    }
}
```

## Tip 2: Use Timber for Logging

```kotlin
class TaskViewModel : ViewModel() {
    fun onEvent(event: TaskEvent) {
        Timber.d("Event received: $event")
        when (event) {
            is TaskEvent.AddTask -> {
                Timber.d("Adding task: ${event.title}")
                handleAddTask(event)
            }
        }
    }
}
```

## Tip 3: State Inspection in Compose

```kotlin
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val state by viewModel.state.collectAsState()
    
    // Debug: Print state on every recomposition
    SideEffect {
        println("Current state: $state")
    }
}
```

---

# 🎯 Final Checklist

Before considering yourself proficient, ensure you can:

## Foundation

- [ ] Explain MVVM architecture
- [ ] Understand Single Source of Truth
- [ ] Implement StateFlow pattern
- [ ] Use Unidirectional Data Flow
- [ ] Create sealed class events

## Intermediate

- [ ] Implement Repository pattern
- [ ] Manage complex state
- [ ] Handle form validation
- [ ] Work with lists immutably
- [ ] Use derived state correctly

## Advanced

- [ ] Implement state machines
- [ ] Build undo/redo system
- [ ] Write unit tests for ViewModels
- [ ] Handle side effects properly
- [ ] Implement pagination

## Real-World

- [ ] Structure large projects
- [ ] Use dependency injection
- [ ] Handle errors gracefully
- [ ] Optimize performance
- [ ] Avoid common pitfalls

---

# 📚 Additional Resources

## Official Documentation

- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Jetpack Compose State](https://developer.android.com/jetpack/compose/state)
- [ViewModel Overview](https://developer.android.com/topic/libraries/architecture/viewmodel)

## Books

- "Android Development with Kotlin" by Marcin Moskala
- "Jetpack Compose Internals" by Jorge Castillo

## Video Courses

- Philipp Lackner (YouTube): MVVM & Clean Architecture
- Android Developers (YouTube): MAD Skills series

---

# 🎉 Conclusion

You now have a complete guide to Jetpack Compose system design!

Remember:

1. **Start simple**: Don't over-engineer at the beginning
2. **Practice**: Build real projects to solidify concepts
3. **Iterate**: Refactor as you learn better patterns
4. **Test**: Write tests to validate your architecture
5. **Stay updated**: Android development evolves constantly

**Next Steps:**

1. Build the practice exercises
2. Study the companion code file
3. Create your own app using these patterns
4. Join Android development communities
5. Keep learning and improving!

Good luck on your Android development journey! 🚀