# **`LiveData`** - **_Observable_ data holder** class

## 1. **What** is **`LiveData`?**

`LiveData` là một **Observable data holder** — **giữ 1 giá trị**, và **thông báo** cho observer mỗi khi giá trị **thay đổi**.

```kotlin
class UserViewModel : ViewModel() {
    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    fun loadUsers() {
        _users.value = listOf(User("An"), User("Binh"))
    }
}
```

Điểm khác biệt của `LiveData` so với các observable thông thường là đặc tính **Lifecycle-aware**

```kotlin
// observe LiveData — lifecycle-aware
viewModel.users.observe(viewLifecycleOwner) { users ->
    adapter.submitList(users)  // An toàn — chỉ chạy khi STARTED+
}
```

Cơ chế bên dưới: `LiveData` tự kiểm tra `Lifecycle.State` của owner - Activity/Fragment - trước khi gọi callback.

| `Lifecycle.State`   | `LiveData` behavior                         |
| ------------------- | ------------------------------------------- |
| **CREATED**         | KHÔNG gọi callback do chưa start            |
| **STARTED/RESUMED** | **Gọi callback**                            |
| **PAUSED**          | **Gọi callback** do vẫn được coi là active. |
| **STOPPED**         | KHÔNG gọi callback                          |
| **DESTROYED**       | UNREGISTER automatically                    |

> _Đây chính là lý do **`LiveData` không bao giờ gây crash app bởi việc update UI khi Activity/Fragment đã destroyed** — LiveData tự biết khi nào nên và không nên gọi observer._

---

## 2. `MutableLiveData` vs `LiveData`

Thông thường, trong `ViewModel`:

- **Giá trị** được giữ lại trong `MutableLiveData` (`private val`), cho phép **gán giá trị mới** bằng `value` hoặc `postValue()`
- **Expose** ra ngoài dưới dạng `LiveData` (`public val`), chỉ cho phép **đọc giá trị**.

để **bảo vệ dữ liệu** khỏi việc bị thay đổi từ bên ngoài.

```kotlin
class UserViewModel : ViewModel() {

    // Riêng tư — chỉ ViewModel được sửa giá trị
    private val _users = MutableLiveData<List<User>>()

    // Public — View chỉ được đọc, không sửa được
    val users: LiveData<List<User>> = _users

    fun loadUsers() {
        _users.value = listOf(...)       // OK — gọi trên Main Thread
        _users.postValue(listOf(...))    // OK — gọi từ background thread
    }
}
```

> _Pattern **backing property** này (`_users` private & `users` public) là **best practice** khi sử dụng `LiveData` trong `ViewModel`, mang ý nghĩa: **chỉ `ViewModel` được quyền GHI**, View chỉ được quyền ĐỌC._

### **Set value** via `value` & `postValue()`

- `_prop.value = newValue` — gán giá trị mới, **chạy ngay lập tức**, trên **Main Thread**

  ```kotlin
  _users.value = data        // Phải gọi trên Main Thread — set ngay lập tức
  ```

- `_prop.postValue(newValue)` — gán giá trị mới, gọi được từ bất kì thread nào, `LiveData` tự **post** giá trị mới lên **Main Thread** để gọi observer.

  > _Dùng trong: coroutine Dispatcher.IO, callback từ thread khác, ..._

  ```kotlin
  _users.postValue(data)  // Gọi được từ bất kỳ thread nào
                          // → LiveData tự switch sang Main Thread để set
  ```

---

## 3. `observe()` vs `observeForever()`

`observe()` là **lifecycle-aware** — gắn với lifecycle, chỉ gọi callback khi owner đang **STARTED+**, và tự **unregister** khi owner bị destroyed.

```kotlin
// observe() — gắn với LifecycleOwner — TỰ ĐỘNG unsubscribe
viewModel.users.observe(viewLifecycleOwner) { users ->
    adapter.submitList(users)
}
```

- Dùng trong **đa số trường hợp** trong Activity/Fragment
- Tự cleanup, không cần lo leak.

`observeForever()` là observer **không gắn lifecycle** — luôn gọi callback, và **không tự unregister** khi owner bị destroyed.

```kotlin
// observeForever() — KHÔNG gắn lifecycle — phải tự unsubscribe
val observer = Observer<List<User>> { users -> ... }
viewModel.users.observeForever(observer)

override fun onDestroy() {
    super.onDestroy()
    viewModel.users.removeObserver(observer)  // Bắt buộc — nếu quên → LEAK
}
```

- Dùng trong **class không có lifecycle** như `Service`, `Repository`, `Singleton`, ...
- Phải tự `removeObserver()` thủ công khi không cần nữa, nếu không sẽ **leak memory**.

---

## 4. **`Transformations`** — Biến đổi giá trị

### 4.1. `Transformations.map()` — biến đổi giá trị của `LiveData` thành **giá trị mới**.

Cơ chế: mỗi khi giá trị của `LiveData` thay đổi, `Transformations.map()` được gọi lại, sẽ **tạo ra giá trị mới** dựa trên giá trị cũ, và **emit** giá trị mới này.

```kotlin
class UserViewModel : ViewModel() {

    private val _users = MutableLiveData<List<User>>()

    // map() — tạo LiveData mới, transform mỗi khi _users thay đổi
    val userCount: LiveData<Int> = Transformations.map(_users) { users ->
        users.size
    }

    val userNames: LiveData<List<String>> = Transformations.map(_users) { users ->
        users.map { it.name }
    }
}
```

Cú pháp **Kotlin extension** hiện đại hơn (thay vì gọi `Transformations.map`):

```kotlin
val userCount: LiveData<Int> = _users.map { users -> users.size }
```

### 4.2. `Transformations.switchMap()` — biến đổi giá trị của `LiveData` thành **1 LiveData mới**.

`Transformations.switchMap()` có ý nghĩa **đổi nguồn `LiveDtata`**

```kotlin
class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _selectedUserId = MutableLiveData<Long>()

    // switchMap — mỗi khi _selectedUserId thay đổi,
    // HỦY observe LiveData cũ, tạo và observe LiveData MỚI
    val userDetail: LiveData<User> = Transformations.switchMap(_selectedUserId) { id ->
        repository.getUserById(id)  // Trả về LiveData<User> — nguồn MỚI mỗi lần
    }

    fun selectUser(id: Long) {
        _selectedUserId.value = id
    }
}
```

### Compare:

|               | input         | return        | meaning                         |
| ------------- | ------------- | ------------- | ------------------------------- |
| `map()`       | `LiveData<T>` | `LiveData<R>` | Biến đổi input                  |
| `switchMap()` | `LiveData<T>` | `LiveData<R>` | transform sang **LiveData mới** |

---

## 5. **`MediatorLiveData`** — **observe nhiều LiveData** cùng lúc

Bài toán: cần **1 LiveData tổng hợp từ nhiều nguồn** khác nhau.

```kotlin
class SearchViewModel : ViewModel() {

    private val _searchQuery = MutableLiveData<String>()
    private val _selectedCategory = MutableLiveData<String>()

    // MediatorLiveData — observe nhiều LiveData, gộp logic lại
    val searchResults = MediatorLiveData<List<Item>>()

    init {
        searchResults.addSource(_searchQuery) { query ->
            searchResults.value = performSearch(query, _selectedCategory.value)
        }
        searchResults.addSource(_selectedCategory) { category ->
            searchResults.value = performSearch(_searchQuery.value, category)
        }
    }

    private fun performSearch(query: String?, category: String?): List<Item> {
        // Logic search kết hợp cả query và category
        return emptyList()
    }
}
```

> `searchResults` là **LiveData tổng** hợp, được update từ nhi

**Note**:

- `addSource(liveData) {}` — mỗi khi `liveData` thay đổi, chạy callback.
- `map()` & `switchMap()` thực chất được **implement dựa trên `MedaitorLiveData`** bên dưới.

> _`MediatorLiveData` là công cụ tổng quát nhất._
