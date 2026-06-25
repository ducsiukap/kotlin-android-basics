# **`Room` CRUD operations** & Coroutines

**`Room` query** chạy trên Main Thread gây `StrictMode` exception / ANR.

Giải pháp:

- Trước `Coroutine`:
  - `AsyncTask` (deprecated)
  - `RxJava` (nặng)
- **Hiện đại**: `Kotlin Coroutines` - nhẹ, readable, **first-class** support trong `Room`

**`Room`** hỗ trợ **`Coroutines` navtive** từ version `2.1.0`:

- `suspend fun` cho **single-shot** operation: `insert`, `update`, `delete`, `query` 1 lần, ...
- `Flow<T>` cho **streaming** / **continuous observation**, tự `emit` khi data thay đổi.

---

## Example: **Todo App**

### 1. `@Entity`, `@Dao`, `@Database`

```kotlin
// Entity
@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val description: String = "",
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val priority: Int = 0   // 0 = Low, 1 = Medium, 2 = High
)
```

```kotlin
// Dao
@Dao
interface TodoDao {

    // ── CREATE ────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: Todo): Long
    // suspend → chạy trên coroutine, không block Main Thread
    // Long    → trả về id của row vừa insert

    @Insert
    suspend fun insertAll(todos: List<Todo>)

    // ── READ ──────────────────────────────────────────────────

    // Flow → emit lại mỗi khi data thay đổi
    @Query("SELECT * FROM todos ORDER BY createdAt DESC")
    fun getAllTodos(): Flow<List<Todo>>

    // Flow với filter
    @Query("SELECT * FROM todos WHERE isDone = :isDone ORDER BY priority DESC")
    fun getTodosByStatus(isDone: Boolean): Flow<List<Todo>>

    // Single-shot query — suspend, không phải Flow
    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: Long): Todo?

    @Query("SELECT COUNT(*) FROM todos WHERE isDone = 0")
    fun getPendingCount(): Flow<Int>

    // ── UPDATE ────────────────────────────────────────────────

    @Update
    suspend fun update(todo: Todo): Int
    // Int → số rows bị ảnh hưởng

    // Update partial — chỉ update 1 field
    @Query("UPDATE todos SET isDone = :isDone WHERE id = :id")
    suspend fun updateDoneStatus(id: Long, isDone: Boolean)

    @Query("UPDATE todos SET priority = :priority WHERE id = :id")
    suspend fun updatePriority(id: Long, priority: Int)

    // ── DELETE ────────────────────────────────────────────────

    @Delete
    suspend fun delete(todo: Todo): Int

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM todos WHERE isDone = 1")
    suspend fun deleteAllDone()

    @Query("DELETE FROM todos")
    suspend fun deleteAll()
}
```

```kotlin
// Database
@Database(entities = [Todo::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun todoDao(): TodoDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "todo_database.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
```

### 2. `Repository` - tầng trung gian

```kotlin
class TodoRepository(private val dao: TodoDao) {

    // ── READ — expose Flow thẳng từ Dao ──────────────────────

    val allTodos: Flow<List<Todo>> = dao.getAllTodos()

    val pendingCount: Flow<Int> = dao.getPendingCount()

    fun getTodosByStatus(isDone: Boolean): Flow<List<Todo>> =
        dao.getTodosByStatus(isDone)

    // ── Single-shot operations — suspend fun ──────────────────

    suspend fun getTodoById(id: Long): Todo? = dao.getTodoById(id)

    // ── CREATE ────────────────────────────────────────────────

    suspend fun insert(todo: Todo): Long = dao.insert(todo)

    suspend fun insertAll(todos: List<Todo>) = dao.insertAll(todos)

    // ── UPDATE ────────────────────────────────────────────────

    suspend fun update(todo: Todo) = dao.update(todo)

    suspend fun toggleDone(todo: Todo) {
        dao.updateDoneStatus(todo.id, !todo.isDone)
    }

    suspend fun updatePriority(id: Long, priority: Int) {
        dao.updatePriority(id, priority)
    }

    // ── DELETE ────────────────────────────────────────────────

    suspend fun delete(todo: Todo) = dao.delete(todo)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun deleteAllDone() = dao.deleteAllDone()
}
```

### 3. `ViewModel` - kết nối **Repository** với **UI**

```kotlin
class TodoViewModel(private val repository: TodoRepository) : ViewModel() {

    // ── State ─────────────────────────────────────────────────

    // StateFlow để UI observe
    val allTodos: StateFlow<List<Todo>> = repository.allTodos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pendingCount: StateFlow<Int> = repository.pendingCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Filter state
    private val _showDone = MutableStateFlow(true)
    val filteredTodos: StateFlow<List<Todo>> = _showDone
        .flatMapLatest { showDone ->
            if (showDone) repository.allTodos
            else repository.getTodosByStatus(isDone = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Operation result — thông báo lỗi về UI
    private val _operationResult = MutableSharedFlow<OperationResult>()
    val operationResult = _operationResult.asSharedFlow()

    sealed class OperationResult {
        data class Success(val message: String) : OperationResult()
        data class Error(val message: String) : OperationResult()
    }

    // ── CREATE ────────────────────────────────────────────────

    fun insertTodo(title: String, description: String, priority: Int) {
        if (title.isBlank()) {
            viewModelScope.launch {
                _operationResult.emit(OperationResult.Error("Tiêu đề không được trống"))
            }
            return
        }

        viewModelScope.launch {
            try {
                val todo = Todo(
                    title = title.trim(),
                    description = description.trim(),
                    priority = priority
                )
                val id = repository.insert(todo)
                _operationResult.emit(
                    OperationResult.Success("Đã thêm todo #$id")
                )
            } catch (e: Exception) {
                _operationResult.emit(
                    OperationResult.Error("Lỗi khi thêm: ${e.message}")
                )
            }
        }
    }

    // ── UPDATE ────────────────────────────────────────────────

    fun toggleDone(todo: Todo) {
        viewModelScope.launch {
            try {
                repository.toggleDone(todo)
            } catch (e: Exception) {
                _operationResult.emit(
                    OperationResult.Error("Lỗi khi cập nhật: ${e.message}")
                )
            }
        }
    }

    fun updateTodo(todo: Todo) {
        viewModelScope.launch {
            try {
                repository.update(todo)
                _operationResult.emit(OperationResult.Success("Đã cập nhật"))
            } catch (e: Exception) {
                _operationResult.emit(
                    OperationResult.Error("Lỗi khi cập nhật: ${e.message}")
                )
            }
        }
    }

    // ── DELETE ────────────────────────────────────────────────

    fun deleteTodo(todo: Todo) {
        viewModelScope.launch {
            try {
                repository.delete(todo)
                _operationResult.emit(OperationResult.Success("Đã xóa"))
            } catch (e: Exception) {
                _operationResult.emit(
                    OperationResult.Error("Lỗi khi xóa: ${e.message}")
                )
            }
        }
    }

    fun deleteAllDone() {
        viewModelScope.launch {
            try {
                repository.deleteAllDone()
                _operationResult.emit(
                    OperationResult.Success("Đã xóa tất cả todo hoàn thành")
                )
            } catch (e: Exception) {
                _operationResult.emit(
                    OperationResult.Error("Lỗi: ${e.message}")
                )
            }
        }
    }

    // ── Filter ────────────────────────────────────────────────

    fun setShowDone(show: Boolean) {
        _showDone.value = show
    }
}
```

### 4. `Activity` / `Fragment` - **observer** / **trigger** CRUD

```kotlin
class TodoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTodoBinding

    private val viewModel: TodoViewModel by viewModels {
        TodoViewModelFactory(
            TodoRepository(
                AppDatabase.getInstance(this).todoDao()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTodoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Observe danh sách todo
                launch {
                    viewModel.allTodos.collect { todos ->
                        // Update RecyclerView adapter
                        todoAdapter.submitList(todos)
                    }
                }

                // Observe pending count
                launch {
                    viewModel.pendingCount.collect { count ->
                        binding.tvPendingCount.text = "Còn lại: $count"
                    }
                }

                // Observe operation result — show Toast/Snackbar
                launch {
                    viewModel.operationResult.collect { result ->
                        when (result) {
                            is TodoViewModel.OperationResult.Success ->
                                Snackbar.make(binding.root, result.message, Snackbar.LENGTH_SHORT).show()
                            is TodoViewModel.OperationResult.Error ->
                                Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG)
                                    .setBackgroundTint(getColor(R.color.red))
                                    .show()
                        }
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {

        // INSERT
        binding.btnAdd.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val desc  = binding.etDescription.text.toString()
            viewModel.insertTodo(title, desc, priority = 1)
            binding.etTitle.text?.clear()
            binding.etDescription.text?.clear()
        }

        // DELETE ALL DONE
        binding.btnClearDone.setOnClickListener {
            viewModel.deleteAllDone()
        }

        // FILTER
        binding.chipShowAll.setOnCheckedChangeListener { _, checked ->
            viewModel.setShowDone(checked)
        }
    }
}
```
