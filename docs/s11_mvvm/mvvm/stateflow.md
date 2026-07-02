# **`StateFlow` - State Holder**

## 1. **What** is the **`StateFlow`**?

**Định nghĩa**: `StateFlow` là một **`hot`**, **`observable`** state-holder Flow — **luôn giữ và emit giá trị hiện tại** cho bất kỳ observer nào subscribe, dù sớm hay muộn.

> _`StateFlow` là **phiên bản hiện đại, mạnh hơn của `LiveData`**._

Về tư tưởng **encapsulation**, `StateFlow` cũng giống như `LiveData`:

- Sử dụng `_state` ( là `private`, `MutableStateFlow`) làm **backing property**, chỉ cho phép **emit giá trị** mới từ bên **trong `ViewModel`**.
- Sử dụng `state` ( là `public`, `StateFlow`, khởi tạo bằng cách `_state.asStateFlow()`) làm **read-only property**, cho phép các observer bên ngoài thực hiện **observe giá trị**.

```kotlin
class UserViewModel : ViewModel() {
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()
}
```

**Note**: khác với `LiveData`, `StateFlow` **LUÔN** cần có giá trị khởi tạo:

```kotlin
// Không compile được —
// StateFlow LUÔN cần initial value
val users: MutableStateFlow<List<User>> = MutableStateFlow()

// Bắt buộc truyền giá trị ban đầu
val users: MutableStateFlow<List<User>> = MutableStateFlow(emptyList())
```

Nhờ vậy:

- Với `LiveData<T>`, **CÓ THỂ** chưa có giá trị (`null`) -> **observer** phải **tự handle trường hợp null**.
- Với `StateFlow<T>`, **LUÔN CÓ** giá trị khởi tạo -> **observer không cần handle null**.

> _Đây là lý do **`StateFlow` phù hợp để biểu diễn UI State** — UI luôn cần **render một trạng thái cụ thể** nào đó (Loading, hoặc danh sách rỗng, hoặc danh sách có data) — **không bao giờ** có trạng thái "**không có gì cả**"._

---

## 2. Convert between **`LiveData`** and **`StateFlow`**

- From `LiveData` to `StateFlow`:

  ```kotlin
  // LiveData → Flow
  val flow = liveData.asFlow()
  ```

- From `StateFlow` to `LiveData`:

  ```kotlin
  // Flow → LiveData
  val liveData = flow.asLiveData()

  // Dùng khi: phải tương tác với code cũ dùng LiveData,
  // nhưng muốn viết logic mới bằng Flow
  ```

---

## 3. **Read**/**Write** value into **`StateFlow`**

### 3.1. **Read**/**Write** via **`value`** property

Tương tự `LiveData`, `StateFlow` cũng có **`value`** property để **read**/**write** đồng bộ:

```kotlin
class UserViewModel : ViewModel() {
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    fun getUserCount(): Int {
        // Đọc giá trị NGAY LẬP TỨC —
        // không cần collect, không cần suspend
        return _users.value.size
    }

    fun addUser(user: User) {
        // Set giá trị mới —
        // đồng bộ, không cần coroutine
        _users.value = _users.value + user
    }
}
```

### 3.2. `StateFlow` chỉ **emit** khi **giá trị THỰC SỰ thay đổi** (value changed):

`StateFlow` có cơ chế **conflation** — **tự động loại bỏ giá trị trùng lặp liên tiếp**. Giúp tránh re-render UI không cần thiết khi set cùng 1 giá trị nhiều lần.

```kotlin
_count.value = 5
_count.value = 5  // ❌ KHÔNG emit lại — giá trị giống hệt, bị bỏ qua
_count.value = 6  // ✅ Emit — giá trị khác
```

Hoạt đông tương tự với **object**:

```kotlin
// So sánh dùng equals() —
// Data class với cùng field → coi là "giống nhau"
data class User(val name: String, val age: Int)

_user.value = User("An", 20)
_user.value = User("An", 20)  // ❌ Không emit — equals() = true
_user.value = User("An", 21)  // ✅ Emit — equals() = false
```

---

## 4. Convert `Flow` into `StateFlow`— `stateIn()`

`stateIn()` - signature:

```kotlin
public fun <T> Flow<T>.stateIn(
    scope: CoroutineScope,
    started: SharingStarted,
    initialValue: T
): StateFlow<T>
```

Trong đó:

- `scope: CoroutineScope` — scope quyết định **lifecycle** của `StateFlow` (_Upstream Flow bị cancel khi scope bị cancel_).
  > _Trong `ViewModel`, thường dùng `viewModelScope` làm scope, Upstream Flow cancel tự động khi `ViewModel.onCleared()` được gọi._
- `started: SharingStarted` — quyết định **khi nào `StateFlow` bắt đầu collect upstream Flow**. Có 3 option:
  - `SharingStarted.Eagerly` — **luôn collect upstream ngay lập tức** khi `stateIn()` được gọi.
  - `SharingStarted.Lazily` — chỉ collect upstream **khi có ít nhất 1 collector**.
  - `SharingStarted.WhileSubscribed(stopTimeoutMillis = 0)` — chỉ collect upstream **khi có ít nhất 1 collector**, và **cancel** upstream khi **không còn collector**.

    > _Đây là option phổ biến nhất, giúp tránh lãng phí tài nguyên khi không có collector._

    Các tham số của `SharingStarted.WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = Long.MAX_VALUE)`:
    - `stopTimeoutMillis` — **thời gian chờ tối đa** trước khi **cancel Upstream Flow khi không còn collector** (_default là `0` - cancel ngay lập tức, **Google khuyến nghị `5000ms`**_).
    - `replayExpirationMillis` — thời gian **giữ cache** tối đa sau khi **cancel Upstream Flow**
      > _Sau khi **cancel Upstream Flow**, `StateFlow` vẫn **giữ giá trị cuối cùng** trong `.value` (cached). Tham số `replayExpirationMillis` guyết định giữ cached value đó bao lâu trước khi **reset về initial value**._

    Default là `replayExpirationMillis = Long.MAX_VALUE` - giữ cache vô thời hạn.

- `initialValue: T` — giá trị khởi tạo của `StateFlow`. Tham số này là **bắt buộc** và được sử sử dụng khi:
  - Trước khi **upstream** flow emit giá trị lần đầu tiên.
  - **Chưa có collector nào subscribe** để trigger upstream chạy với `WhileSubscribed()`.

Example convert `Flow` into `StateFlow`:

```kotlin
class UserViewModel(private val repository: UserRepository) : ViewModel() {

    // repository.allUsers là Flow<List<User>> (cold) — đến từ Room
    // user là StateFlow<List<User>> (hot) — có thể collect bất kỳ lúc nào, luôn có giá trị hiện tại
    val users: StateFlow<List<User>> = repository.allUsers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
```

---

## 5. **Collect `StateFlow`** trong `View` — `repeatOnLifecycle`

Khác với `LiveData` có **lifecycle-aware** tự động, built-in,<br/>
`StateFlow` **KHÔNG lifecycle-aware** — phải tự bọc bằng `repeatOnLifecycle()` khi collect trong `View` (Activity/Fragment):

> _Đây là điểm "**kém tiện**" duy nhất của `StateFlow` so với `LiveData`_

```kotlin
class UserActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // repeatOnLifecycle — tự pause/resume collect theo lifecycle
            // tương đương cơ chế lifecycle-aware của LiveData.observe()
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.users.collect { users ->
                    adapter.submitList(users)
                }
            }
        }
    }
}
```
