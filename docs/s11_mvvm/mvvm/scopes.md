# `viewModelScope`, `lifecycleScope` & **đặt `Dispatchers` ở đâu?**

## 1. **`Dispatcher.Main.immediate`**

### 1.1. `Dispatcher.Main.immediate` & `Dispatcher.Main`?

- `Dispatcher.Main`: luôn **POST** vào **message queue** của Main thread **dù đang gọi từ chính Main thread**
  > _Vì vậy, luôn có **delay nhỏ** do cần chờ tới lượt trong message queue_
- `Dispatcher.Main.immediate`:
  - Nếu **đã ở Main Thread** (đang gọi từ Main Thread), task **chạy ngay lập tức**, không cần post và chờ tới lượt trong message queue
  - Nếu **đang ở Thread KHÁC**, task được **post vào message queue** của như behavior bình thường của `Dispatcher.Main`

  Do vậy, `Dispatcher.Main.immediate` tối ưu hơn nhờ việc **loại bỏ delay nhỏ** khi đang ở Main thread.

### 1.2. `viewModelScope` & `lifecycleScope` mặc định dùng `Dispatcher.Main.immediate`

Cả `viewModelScope` và `lifecycleScope` đều **chạy trên `Dispatchers.Main.immediate` (Main thread)** theo mặc định.

```kotlin
viewModelScope.launch {
    // Chạy trên Main Thread — mặc định
}

lifecycleScope.launch {
    // Chạy trên Main Thread — mặc định
}
```

**Lý do**: `viewModelScope` và `lifecycleScope` thường dùng để:

- Gọi `suspend fun`, sau đó **update UI/StateFlow** ngay sau đó.
- Nếu default là **background thread** → mỗi lần update UI
  phải tự `withContext(Dispatchers.Main)` khiến code dài dòng, khó đọc, dễ quên.

Tuy nhiên, dù chạy trên **Main Thread** nhưng **KHÔNG tự block Main** vì `suspend fun` bên trong thường được cài đặt **tự chuyển `Dispatchers`**

---

## 2. Quy tắc: **Đặt `Dispatcher` ở TẦNG THẤP NHẤT**

Nghe có vẻ không hợp lý:

- Khuyến khích: **các thao tác blocking nên chạy trên background thread**
- `ViewModel` thường gọi `Repository` để lấy dữ liệu từ **Data Source** (Room/Retrofit/File) → các thao tác này là **blocking** nhưng lại chạy trên **MAIN thead**.

**Giải thích**:

> _Nguyên tắc: **đặt Dispatcher ở tầng thấp nhất** — nơi thực sự thực hiện tác vụ blocking (**Data Source: `Room`/`Retrofit`/`File`**). <br/>_
> _KHÔNG đặt ở `ViewModel`, `Repository` thường cũng không cần tự đặt nếu Data Source đã làm đúng._

- `ViewModel`: **KHÔNG BAO GIỜ** tự viết `Dispatcher.Default/IO`
- `Repository`: **thường KHÔNG cần tự đặt `Dispatcher`**, trừ khi nó tự làm **heavy task**
- **Data Source** → ĐÂY là nơi đặt `Dispatcher`

### 2.1. Tại sao **KHÔNG** đặt `Dispatcher` ở `ViewModel`?

```kotlin
// ❌ Sai — ViewModel biết về threading detail của tầng dưới
class UserViewModel(private val repository: UserRepository) : ViewModel() {
    fun loadUsers() {
        viewModelScope.launch {
            val users = withContext(Dispatchers.IO) {
                repository.getUsers()  // ViewModel phải "đoán" là Repository
                                        // cần chạy trên IO — vi phạm encapsulation
            }
            _uiState.value = users
        }
    }
}
```

**Vấn đề**:

1. **`ViewModel` phải BIẾT chi tiết implementation của `Repository`**:<br/>
   Nếu `Repository` đổi từ `Room` sang **in-memory cache** (không cần IO nữa) → **phải sửa LẠI `ViewModel`** — sai tầng chịu trách nhiệm
2. **Nếu 10 `ViewModel` đều gọi cùng 1 `Repository` method** → phải **lặp lại `withContext(Dispatchers.IO)`** ở CẢ 10 nơi → dễ quên, dễ inconsistent
3. `ViewModel` nên **CHỈ** quan tâm: "**gọi hàm này, nhận kết quả,
   update UI**" — _không nên biết "hàm này chạy ở thread nào"_

### 2.2. Đặt `Dispatcher` ở tầng thấp nhất: **Data Source**

Cả `Room` và `Retrofit` đều **tự động switch `Dispatcher` - chạy trên background thread** cho các `suspend fun`:

```kotlin
// Room — TỰ ĐỘNG dùng background thread cho suspend fun
@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    suspend fun getUsers(): List<User>
    // Room's suspend fun luôn chạy trên Dispatchers.IO của
    // riêng Room (Executor nội bộ) — không cần bạn tự khai báo
}

// Retrofit — TỰ ĐỘNG tương tự với suspend fun
interface ApiService {
    @GET("/users")
    suspend fun getUsers(): List<UserDto>
    // Retrofit tự chạy network call trên OkHttp's background thread
}
```

> _**NOTE**: `Room`/`Retrofit` **chỉ tự động switch `Dispatcher` cho `suspend fun`** (hoặc `Flow` với `Room`)_<br/>
> Với `fun` bình thường, **nó chạy trực tiếp trên thread đang gọi** nó.

Ngoài ra:

- `DataStore` cũng tự switch `Dispatcher.IO` cho các `suspend fun`
- Firebase `FireStore`/`Realtime Database` không dùng **suspend fun/coroutine**
- **File I/O** thủ công (`java.io.File`) **KHÔNG** tự switch, **PHẢI `withContext(Dispatchers.IO)` thủ công**.
  ```kotlin
  // ✅ Phải tự bọc
  suspend fun readConfig(): String {
      return withContext(Dispatchers.IO) {
          File(path).readText()
      }
  }
  ```
- **Sensor/Location/BluetoothLE** — Callback-based, cần `callbackFlow`

  ```kotlin
  // Location Provider — hoàn toàn callback-based, không suspend
  fun observeLocation(): Flow<Location> = callbackFlow {
      val callback = object : LocationCallback() {
          override fun onLocationResult(result: LocationResult) {
              trySend(result.lastLocation!!)  // Đẩy giá trị vào Flow
          }
      }
      fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

      awaitClose {
          fusedLocationClient.removeLocationUpdates(callback)  // Cleanup
      }
  }
  // callbackFlow tự nằm trên thread callback được gọi (thường Main
  // hoặc thread riêng của SDK) — cần tự cân nhắc nếu muốn đổi
  ```

Khi này, cả `Repository` và `ViewModel` **KHÔNG cần quan tâm** đến `Dispatcher`:

```kotlin
// Repository — KHÔNG cần withContext nếu chỉ "pass through"
class UserRepository(
    private val dao: UserDao,
    private val api: ApiService
) {
    suspend fun getUsers(): List<User> {
        return dao.getUsers()  // Room tự IO — Repository không cần làm gì thêm
    }
}
```

```kotlin
// ViewModel — hoàn toàn "ngây thơ" về threading, chỉ gọi suspend fun
class UserViewModel(private val repository: UserRepository) : ViewModel() {
    fun loadUsers() {
        viewModelScope.launch {
            val users = repository.getUsers()  // Không cần withContext
            _uiState.value = users              // Tự động ở lại Main
        }
    }
}
```

### 2.3. **When** `Repository` needs to set `Dispatcher`?

Chỉ khi **`Repository` tự làm việc blocking**, không delegate cho Room/Retrofit:

```kotlin
class ImageRepository {
    // ✅ Đúng — Repository chủ động switch
    // vì đây là tác vụ tự viết
    suspend fun decodeImage(bytes: ByteArray): Bitmap {
        return withContext(Dispatchers.Default) {  // CPU-bound → Default
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }
}

class FileRepository(private val context: Context) {

    // File I/O tự viết — Room/Retrofit không lo giúp việc này
    suspend fun readFile(fileName: String): String {
        return withContext(Dispatchers.IO) {  // I/O-bound → IO
            File(context.filesDir, fileName).readText()
        }
    }
}
```

**Quy tắc**:

- Nếu **`Repository` gọi thẳng `Room`/`Retrofit`'s suspend fun** → không cần withContext.
- Nếu **`Repository` tự viết code blocking** (_File, Bitmap decode, tính toán nặng, thư viện Java cũ không hỗ trợ suspend_) → phải **tự `withContext()`** ngay tại nơi viết code đó.
