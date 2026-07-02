# `AndroidViewModel` — ViewModel with **Application context**

## 1. **Vấn đề**: `ViewModel` không được phép có **Context**

**Quy tắc** cốt lõi: `ViewModel` **không được import** bất kỳ thứ gì của Android `View`/`Context`.

```kotlin
class UserViewModel(
    // ❌ SAI — memory leak nguy hiểm
    // ViewModel giữ ref tới Activity/Fragment
    // -> Activity/Fragment không được GC
    private val context: Context
) : ViewModel() {

    fun loadAvatar() {
        val drawable = ContextCompat.getDrawable(context, R.drawable.avatar)
    }
}
```

Tuy nhiên, trong một số trường hợp, **`ViewModel` thực sự cần `Context`**:

- Đọc String resources: `context.getString(R.string.app_name)`
- Kiểm tra internet connection: `ConnectivityManager`
- Truy cập SharedPreferences: `context.getSharedPreferences(...)`
- Build Room Database: `Room.databaseBuilder(context, ...)`
- Đọc file từ assets: `context.assets`
- ...

> _Giải pháp **không phải là cấm hoàn toàn `Context` trong `ViewModel`** — mà là **dùng đúng** loại Context: **Application Context**, không phải Activity Context._

---

## 2. **Giải pháp**: `AndroidViewModel` — `ViewModel` có **Application context**

Android cung cấp sẵn một class đặc biệt: `AndroidViewModel`, kế thừa từ `ViewModel`, có **sẵn Application context**.

```kotlin
// EXAMPLE
// Source code của AndroidViewModel (rút gọn)
abstract class AndroidViewModel(application: Application) : ViewModel() {
    private val mApplication: Application = application

    fun <T : Application> getApplication(): T {
        return mApplication as T
    }
}
```

Usage:

```kotlin
class UserViewModel(application: Application) : AndroidViewModel(application) {

    fun loadAvatarDescription(): String {
        // getApplication() trả về Application Context — AN TOÀN
        return getApplication<Application>().getString(R.string.avatar_desc)
    }
}
```

### 2.1. Khởi tạo `AndroidViewModel` - KHÔNG cần Factory thủ công

`AndroidViewModel` được Android **hỗ trợ Factory sẵn** — không cần tự viết `ViewModelProvider.Factory`:

```kotlin
class UserActivity : AppCompatActivity() {

    // KHÔNG cần factory — by viewModels() tự nhận biết
    // đây là AndroidViewModel và tự truyền Application vào
    private val viewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hoạt động bình thường — không cần khai báo gì thêm
    }
}
```

### 2.2. Example: khởi tạo `Repository` ngay trong `AndroidViewModel` với **Application context**

```kotlin
class TodoViewModel(application: Application) : AndroidViewModel(application) {

    // Khởi tạo Repository ngay trong ViewModel — dùng Application context
    private val repository: TodoRepository by lazy {
        val db = AppDatabase.getInstance(getApplication())
        TodoRepository(db.todoDao())
    }

    val todos: StateFlow<List<Todo>> = repository.allTodos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insert(todo: Todo) {
        viewModelScope.launch {
            repository.insert(todo)
        }
    }
}
```

```kotlin
class TodoActivity : AppCompatActivity() {
    // Không cần Factory, không cần MyApplication custom
    private val viewModel: TodoViewModel by viewModels()
}
```

---

## 2.3. **When** use `AndroidViewModel`?

Dùng `AndroidViewModel` khi bạn cần **Application context** trong `ViewModel`:

- Project nhỏ, **không có DI** framework
- Chỉ cần đọc string resource, SharedPreferences đơn giản
- Muốn code gọn, ít boilerplate
- **Không quan tâm nhiều đến Unit Test** cho ViewModel ??????

Dùng `ViewModel` khi:

- Project lớn, có **Repository pattern** rõ ràng
- **Cần Unit Test ViewModel** (mock Repository dễ hơn mock Application)
- Dùng **Dependency Injection (`Hilt`/`Koin`)** — pattern chuẩn ở đây
  thực ra không dùng cả 2 cách trên, mà inject thẳng qua DI
- Muốn **tách biệt rõ ViewModel khỏi Android Framework** hoàn toàn

> _Thực tế **production**: Phần lớn dự án lớn **dùng `Hilt` để inject Repository** — không cần AndroidViewModel lẫn Factory thủ công._
