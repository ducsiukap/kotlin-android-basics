# **`ViewModel`** & Configuration changes

## 1. **Configuration changes** problem

Configuration Change là khi Android bị **destroy - `onDestroy`** và **recreate - `onCreate`** Activity do **thay đổi cấu hình**, có thể là:

- **Rotate screen** (`portrait`/`landscape`)
- **Change language**
- **Change font size** of system
- **Change theme** of system
- Connect/disconnect **keyboard**
- ...

**Flow** khi xảy ra **configuration change**:

1. **User rotate screen**
2. Android gọi `Activity.onDestroy()` -> Activity bị hủy.
3. Android gọi `Activity.onCreate()` -> Activity được tạo lại với **toàn bộ `field=null`** trong Activity

**Hệ quả**: mất data khi configuration change:

```kotlin
class UserActivity : AppCompatActivity() {

    // List này bị mất khi rotate
    private var users: List<User> = emptyList()
    private var isLoading: Boolean = false
    private var currentPage: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Gọi lại API mỗi lần rotate → tốn bandwidth, UX tệ
        loadUsers()
    }

    private fun loadUsers() {
        isLoading = true
        // Fetch API tốn 2 giây
        // User rotate trong lúc đang fetch → coroutine bị cancel
        // Activity mới → gọi lại từ đầu
        lifecycleScope.launch {
            users = repository.getUsers()
            isLoading = false
            showUsers(users)
        }
    }
}
```

---

## 2. **`onSaveInstanceState()` - wrong solution**

Có thể **lưu lại data** trước khi Activity bị destroy bằng cách override callback `onSaveInstanceState()`:

```kotlin
override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    // Bundle chỉ chứa được data nhỏ — serializable
    outState.putParcelableArrayList("users", ArrayList(users))
}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    users = savedInstanceState?.getParcelableArrayList("users") ?: emptyList()
}
```

Tuy nhiên, đây là giải pháp **không tối ưu** / **sai** do:

- `Bundle` bị **giới hạn kích thước** - 1MB (Binder transaction limit)<br/>
  -> khi này, **list 1000 users + image** sẽ bị exception `TransactionTooLargeException` -> **CRASH**
- `Bundle` chỉ lưu được `Parcelable` / `Serializable`<br/>
  -> **không lưu được object phức tạp** như `Flow`, `LiveData`, coroutine state, ...
- `onSaveInstanceState()` KHÔNG được gọi khi **user press back** hoặc **`finish()`** Activity<br/>
  -> chỉ dùng trong trường hợp OS kill app, **không phải configuration change**

---

## 3. `ViewModel` - **the _RIGHT_ solution**

Android cung cấp một cơ chế đặc biệt: **`ViewModel` _TỒN TẠI XUYÊN SUỐT_ configuration change**.

- **`Activity.onCreate()` lần đầu**:
  - `ViewModel` được tạo.
  - `ViewModel` fetch API -> lưu data vào `StateFlow` / `LiveData`.
- **on Configuration change**:
  - `Activity` bị destroy
  - `ViewModel` **VẪN tồn tại**.
- **`Activity` recreate**:
  - `Activity.onCreate()` được gọi lại -> `ViewModel` được **reuse** thay vì tạo mới.
  - `Activity` subscribe lại vào `StateFlow` / `LiveData` trong `ViewModel` -> **data vẫn còn nguyên**, không tốn time để fetch lại API.

```kotlin
class UserViewModel(private val repository: UserRepository) : ViewModel() {

    // StateFlow tồn tại trong ViewModel → sống qua rotate
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    init {
        loadUsers() // Chỉ gọi 1 lần — khi ViewModel được tạo lần đầu
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _users.value = repository.getUsers()
        }
    }
}

class UserActivity : AppCompatActivity() {

    // by viewModels() — lấy ViewModel từ ViewModelStore
    // Nếu đã tồn tại → trả về instance cũ (sau rotate)
    // Nếu chưa tồn tại → tạo mới
    private val viewModel: UserViewModel by viewModels { ... }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Chỉ observe — không fetch lại
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.users.collect { users ->
                    adapter.submitList(users)
                }
            }
        }
    }
}
```

### 3.1. `ViewModelStore` - **cơ chế bên dưới / nơi lưu trữ `ViewModel`**

Câu hỏi đặt ra: **tại sao `ViewModel` lại tồn tại xuyên suốt configuration change**?

```
Activity
└── ViewModelStore          ← Map<String, ViewModel>
      └── UserViewModel
```

Khi **configuration change** xảy ra:

- `Activity` bị destroy
- `ViewModelStore` được **RETAIN**

Khi **`Activity` mới được tạo**: khởi tạo `viewModel` bằng **`by viewModels()` delegates**:

- Nếu `ViewModelStore` đã có `UserViewModel` → **reuse** instance cũ
- Nếu chưa có → tạo mới `UserViewModel` và lưu vào `ViewModelStore`

Nhờ cơ chế **retain** của `ViewModelStore` này, `ViewModel` **tồn tại xuyên suốt configuration change**.

**Note**: `ViewModelStore` bị **clear** khi:

- User press **back**/`finish()` Activity được gọi
- Trong Activity, `isFinishing = true`

```text
Activity 1          rotate       Activity 2
        onCreate()    ─────────────────→ onCreate()
        onStart()                        onStart()
        onResume()                       onResume()
        onPause()                        onPause()
        onStop()                         onStop()
        onDestroy()                      onDestroy()
             │                                │
             │                                │
        ─────┼────────────────────────────────┼─────→ finish()
             │                                │          │
       ViewModel  ←──── vẫn sống ────→  ViewModel   onCleared()
       created                                         called
```

### 3.2. `onCleared()`

`onCleared()` là **callback duy nhất** của `ViewModel` lifecycle — được **gọi khi `ViewModel` thực sự bị hủy**.

> _Dùng để **cleanup resource**: cancel coroutine scope tự tạo, close connection..._

```kotlin
class UserViewModel : ViewModel() {

    // viewModelScope tự cancel trong onCleared() — không cần tự cancel
    // Nhưng nếu tạo scope riêng thì phải cancel thủ công

    private val customScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCleared() {
        super.onCleared()
        customScope.cancel()  // Bắt buộc nếu tự tạo scope
    }
}
```
