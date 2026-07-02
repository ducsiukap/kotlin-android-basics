# **`Repository` pattern**

## 1. **`Repository` - bridge between `ViewModel` and Data Source**

### 1.1. **KHÔNG** có `Repository`

Nếu không có `Repository`, **`ViewModel` sẽ phải trực tiếp gọi đến Data Source**:

```kotlin
class UserViewModel(
    private val dao: UserDao,           // Room
    private val api: ApiService         // Retrofit
) : ViewModel() {

    fun loadUsers() {
        viewModelScope.launch {
            // ViewModel phải TỰ quyết định: lấy từ đâu? đồng bộ thế nào?
            val localUsers = dao.getUsers()
            if (localUsers.isEmpty()) {
                val remoteUsers = api.getUsers()
                dao.insertAll(remoteUsers.map { it.toEntity() })
            }
        }
    }
}
```

Vấn đề:

1. `ViewModel` phải **làm quá nhiều việc**, vừa quản lí **ui states**, vừa quyết định **lấy dữ liệu từ đâu**, vừa **xử lí đồng bộ** -> Vi phạm **Single Responsibility Principle**.
2. **Logic sync** bị lặp lại ở nhiều `ViewModel` khác nhau.
3. **Khó test**
4. **Khó thay đổi nguồn data**

### 1.2. **CÓ** `Repository`

```kotlin
class UserViewModel(private val repository: UserRepository) : ViewModel() {

    val users: StateFlow<List<User>> = repository.getUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refresh() {
        viewModelScope.launch {
            repository.refreshUsers()  // Không biết bên trong dùng Room hay API
        }
    }
}
```

**Định nghĩa vai trò**:

- `Repository` là **tầng duy nhất biết "data thực sự đến từ đâu"** — _`Room`, `Retrofit`, `DataStore`, cache, hay kết hợp nhiều nguồn_.
- `ViewModel` chỉ cần biết "**cần data -> gọi API tương ứng từ `Repository`**" — không quan tâm implementation.

---

## 2. `Repository` tích hợp `Room` + `Retrofit`

### 2.1. Các thao tác

Với:

- **READ operation**: luôn đọc từ `Room` - **Single Source of Truth**
- **SYNC operation**: đồng bộ kết quả từ `Retrofit` về `Room`
- **WRITE operation**: ghi vào `Room` (_và có thể đồng bộ lên `Retrofit`_)

```kotlin
class UserRepository(
    private val dao: UserDao,
    private val api: ApiService
) {

    // ── READ — luôn đọc từ Room (Single Source of Truth) ──────
    fun getUsers(): Flow<List<User>> {
        return dao.getAllUsers().map { entities ->
            entities.map { it.toDomainModel() }  // Entity → Domain model
        }
    }

    fun getUserById(id: Long): Flow<User?> {
        return dao.getUserById(id).map { it?.toDomainModel() }
    }

    // ── SYNC — gọi API, ghi kết quả vào Room ───────────────────
    suspend fun refreshUsers() {
        val remoteUsers = api.getUsers()               // Gọi network
        val entities = remoteUsers.map { it.toEntity() } // DTO → Entity
        dao.insertAll(entities)                          // Ghi vào Room
        // KHÔNG return gì cho UI trực tiếp — UI tự động thấy
        // data mới vì đang collect Flow từ Room (getUsers())
    }

    // ── WRITE — có thể ghi cả 2 nơi, hoặc chỉ 1 nơi tùy bài toán ──
    suspend fun createUser(user: User) {
        val dto = user.toDto()
        val created = api.createUser(dto)      // Server tạo, trả về ID thật
        dao.insert(created.toEntity())          // Lưu vào Room với ID đó
    }

    suspend fun deleteUser(id: Long) {
        api.deleteUser(id)       // Xóa trên server trước
        dao.deleteById(id)        // Rồi xóa local
    }
}
```

### 2.2. **Mapper function**:

Cần tách biệt **3 loại models**:

- `Dto`: data from **API** (JSON), cần **thay đổi theo API contract**
- `Entity`: data from **Room** (DB), phụ thuộc vào **schema Room**
- `Domain` model: object thực sự được dùng trong `ViewModel` & `View`, chỉ thay đổi khi **business logic thay đổi**

```kotlin
// DTO — hình dạng dữ liệu API trả về (JSON)
data class UserDto(
    val id: Long,
    val full_name: String,   // snake_case từ server
    val email_address: String
)

// Entity — hình dạng lưu trong Room
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val email: String
)

// Domain model — hình dạng ViewModel/View thực sự dùng
data class User(
    val id: Long,
    val name: String,
    val email: String
)

// Mapper functions
fun UserDto.toEntity() = UserEntity(id = id, name = full_name, email = email_address)
fun UserEntity.toDomainModel() = User(id = id, name = name, email = email)
fun User.toDto() = UserDto(id = id, full_name = name, email_address = email)
```

---

## 3. **Single Source of Truth** & vấn đề **API Sync**

Vấn đề: **nếu UI đọc TRỰC TIẾP từ API**, không qua Room:

- Không có **data** khi offline -> **`Empty` state** ngay cả khi trước đó đã load thành công.
- Mỗi lần vào lại screen -> **load lại API** -> tốn bandwidth, latency, ...
- Không có **cache** tự nhiên, phải viết logic cache thủ công.

**Giải pháp**: `Room` là **smart cache**, `API` chỉ **nạp data**:

```text
Giải pháp — Room là "cache thông minh", API chỉ "nạp" dữ liệu:

┌─────────────┐     refresh()      ┌──────────┐
│     API     │ ──────────────>    │   Room   │
│  (Retrofit) │   ghi vào Room     │ (Source  │
└─────────────┘                    │ of Truth)│
                                   └─────┬─────┘
                                         │ Flow — UI luôn
                                         │ đọc từ đây
                                         ▼
                                    ┌──────────┐
                                    │    UI    │
                                    └──────────┘
```

```kotlin
class UserRepository(private val dao: UserDao, private val api: ApiService) {

    // UI gọi hàm này để đọc data - from Room
    fun getUsers(): Flow<List<User>> = dao.getAllUsers().map { it.toDomainModel() }

    // UI gọi hàm này để "yêu cầu làm mới" — from API
    // nhưng KHÔNG nhận kết quả trực tiếp từ đây,
    // mà tự thấy qua Flow ở trên
    suspend fun refreshUsers() {
        try {
            val remote = api.getUsers()
            dao.insertAll(remote.map { it.toEntity() })
        } catch (e: IOException) {

            // Mất mạng — KHÔNG throw lên UI, Room vẫn còn data cũ
            // để hiển thị — đây chính là lợi ích của pattern này

            throw e  // (hoặc xử lý riêng — bàn ở mục 5, Error handling)
        }
    }
}
```

Ở `ViewModel`:

```kotlin
class UserViewModel(private val repository: UserRepository) : ViewModel() {

    // Luôn hiển thị data từ Room — kể cả khi refresh() đang lỗi
    val users: StateFlow<List<User>> = repository.getUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Gọi refresh ngay khi ViewModel tạo — nhưng UI không "chờ"
        // kết quả này để hiển thị, vì đã đang collect Room rồi
        viewModelScope.launch {
            try {
                repository.refreshUsers()
            } catch (e: Exception) {
                // Chỉ báo lỗi nhẹ (Toast) — KHÔNG làm mất data đang hiển thị
                _effect.emit(UiEffect.ShowToast("Không thể làm mới: ${e.message}"))
            }
        }
    }
}
```

> _**Kết quả**: Nếu user mở app khi **không có mạng**, họ vẫn **thấy data cũ** (từ lần load trước, lưu trong Room) thay vì màn hình trắng/lỗi. Đây chính là nền tảng của **Offline-first**._

---

## 4. **Offline-first** strategy

**Định nghĩa**: **`Offline-first` strategy** là cách **thiết kế app coi local database (`Room`) là nguồn dữ liệu chính**, network chỉ là nguồn đồng bộ theo cơ hội (khi có mạng) — không phải điều kiện bắt buộc để app hoạt động.

**Implement - kết hợp `NetworkBoundResource` pattern**:

```kotlin
class UserRepository(
    private val dao: UserDao,
    private val api: ApiService,
    private val networkMonitor: NetworkMonitor  // Kiểm tra có mạng không
) {

    fun getUsers(): Flow<List<User>> = flow {
        // 1. Luôn emit data local NGAY LẬP TỨC — không đợi network
        val localData = dao.getAllUsers().first()
        emit(localData.map { it.toDomainModel() })

        // 2. Nếu có mạng — thử đồng bộ ngầm
        if (networkMonitor.isConnected()) {
            try {
                val remote = api.getUsers()
                dao.insertAll(remote.map { it.toEntity() })
            } catch (e: IOException) {
                // Lỗi mạng khi đồng bộ — im lặng bỏ qua,
                // data local vẫn còn nguyên, không phá UI
            }
        }

        // 3. Emit lại — lần này lấy TỪ Room (đã có data mới nếu sync thành công)
        emitAll(dao.getAllUsers().map { it.map { e -> e.toDomainModel() } })
    }
}
```

---

## 5. **Error Handling** trong `Repository` — **`Result<T>` wrapper**

### 5.1. Vấn đề khi **`throw Exception`** trực tiếp từ `Repository` lên `ViewModel`:

```kotlin
class UserRepository(private val api: ApiService) {
    suspend fun getUsers(): List<User> {
        return api.getUsers().map { it.toDomainModel() }
        // Nếu lỗi network → throw exception thẳng lên ViewModel
    }
}
```

Khi này, ở `ViewModel`, bạn phải **try-catch MỌI LẦN gọi**:

```kotlin
class UserViewModel(private val repository: UserRepository) : ViewModel() {
    fun load() {
        viewModelScope.launch {
            try {
                val users = repository.getUsers()  // Phải try-catch MỌI LẦN gọi
                _uiState.value = UiState.Success(users)
            } catch (e: IOException) {
                _uiState.value = UiState.Error("Lỗi mạng")
            } catch (e: HttpException) {
                _uiState.value = UiState.Error("Lỗi server: ${e.code()}")
            }
            // Lặp lại try-catch này ở MỌI ViewModel gọi Repository — dễ quên
        }
    }
}
```

### 5.2. Giải pháp: **`Result<T>` wrapper**

`Repository` được phép **tự bắt lỗi** và trả kết quả **type-safe** - `Result<T>` thay vì throw exception:

#### **Bước 1**: Tạo `Result<T>` wrapper:

```kotlin
// Định nghĩa Result wrapper riêng cho app (khác kotlin.Result built-in
// — dùng sealed class để dễ mở rộng thêm case nếu cần)
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Failure(val exception: Throwable, val message: String) : AppResult<Nothing>()
}
```

#### **Bước 2**: `Repository` trả về `AppResult<T>`:

```kotlin
class UserRepository(private val api: ApiService, private val dao: UserDao) {

    suspend fun refreshUsers(): AppResult<Unit> {
        return try {
            val remote = api.getUsers()
            dao.insertAll(remote.map { it.toEntity() })
            AppResult.Success(Unit)
        } catch (e: IOException) {
            AppResult.Failure(e, "Không có kết nối mạng")
        } catch (e: HttpException) {
            AppResult.Failure(e, "Lỗi server (${e.code()})")
        } catch (e: Exception) {
            AppResult.Failure(e, "Lỗi không xác định: ${e.message}")
        }
    }
}
```

#### **Bước 3**: `ViewModel` xử lý `AppResult<T>`:

```kotlin
class UserViewModel(private val repository: UserRepository) : ViewModel() {

    fun refresh() {
        viewModelScope.launch {
            // Không cần try-catch nữa — Repository đã xử lý sẵn
            when (val result = repository.refreshUsers()) {
                is AppResult.Success ->
                    _effect.emit(UiEffect.ShowToast("Đã cập nhật"))
                is AppResult.Failure ->
                    _effect.emit(UiEffect.ShowToast(result.message))
            }
        }
    }
}
```

### 5.3. `CoroutineExceptionHandler` - **bắt lỗi toàn cục trong `ViewModel`**

**`try-catch` chỉ bắt được lỗi trong đúng khối code đó**. <br/>
Với `viewModelScope.launch{}`, nếu **quên `try-catch`**, **exception sẽ làm crash toàn bộ coroutine** — đôi khi cả app.

```kotlin
class UserViewModel(private val repository: UserRepository) : ViewModel() {

    // Handler bắt MỌI exception không được catch
    // bên trong launch{}
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        viewModelScope.launch {
            _effect.emit(UiEffect.ShowToast("Lỗi không mong muốn: ${throwable.message}"))
        }
        Log.e("UserViewModel", "Unhandled exception", throwable)
    }

    fun riskyOperation() {
        // Gắn handler vào — nếu code bên trong throw mà quên catch,
        // handler sẽ bắt thay vì crash app
        viewModelScope.launch(exceptionHandler) {
            val data = repository.someRiskyCall()  // Lỡ quên xử lý lỗi ở đây
            _uiState.value = UiState.Success(data)
        }
    }
}
```

**Note**: `CoroutineExceptionHandler` **KHÔNG** thay thế `try-catch`, chỉ dùng cho **LỖI KHÔNG LƯỜNG TRƯỚC ĐƯỢC** như bug, edge case, ... như lớp bảo vệ cuối cùng tránh **crash app**
