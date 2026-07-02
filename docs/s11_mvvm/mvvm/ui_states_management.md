# **_UiState_ Pattern**

## 1. **What** is **`UiState`**?

### 1.1. Vấn đề: **khi UI có nhiều _trạng thái_**

Giả sử, `ViewModel` dạng:

```kotlin
val users: StateFlow<List<User>> = repository.allUsers
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

`List<User>` chỉ trả lời được câu hỏi: **Data là gì**. <br/>
Nhưng, thực tế UI cần nhiều hơn thế:

- Đang load data? -> `isLoading: Boolean`
- Load gặp thất bại? -> `errorMessage: String?`
- Load rỗng vì **chưa load** hay **load xong nhưng không có gì** -> khó nhận biết được 2 case.

Cách làm sai: `ViewModel` với nhiều **`StateFlow`-flags** rời rạc:

```kotlin
class UserViewModel : ViewModel() {
    val users: StateFlow<List<User>> = ...
    val isLoading: StateFlow<Boolean> = ...
    val errorMessage: StateFlow<String?> = ...
}
```

Khi này:

- Có thể xảy ra nhiều trạng thái **thiếu hợp lí**.<br/>
  eg. `isLoading=true` & `errorMessage="Network error"` cùng lúc -> **UI** vừa hiện **loading** lại vừa hiện **error** cùng lúc?
- Collector phải **collect nhiều `Flow` riêng biệt** -> code rối, dễ quên, ...
- Không có **Single Source of Truth**: 3 mảnh trạng thái rời rạc, đông đảm bảo đồng bộ với nhau.

```kotlin
// View phải tự phối hợp NHIỀU StateFlow — dễ ra trạng thái "vô lý"
lifecycleScope.launch {
    repeatOnLifecycle(STARTED) {
        launch { viewModel.isLoading.collect { progressBar.isVisible = it } }
        launch { viewModel.users.collect { adapter.submitList(it) } }
        launch { viewModel.errorMessage.collect { it?.let { msg -> showError(msg) } } }
    }
}
```

### 1.2. **`UiState` pattern**

**`UiState` pattern** là cách **gom toàn bộ state** của screen về **1 `sealed class`/`sealed interface` duy nhất**

> _Mỗi **State** tự quản lí data riêng của nó._

```kotlin
sealed class UserUiState {
    object Loading : UserUiState()
    data class Success(val users: List<User>) : UserUiState()
    data class Error(val message: String) : UserUiState()
}
```

Khi này, trong `ViewModel` chỉ giữ **state**:

```kotlin
class UserViewModel(private val repository: UserRepository) : ViewModel() {

    // VM giữ state
    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    // Single Source of Truth
    // Chỉ VM có quyền quản lí sate
    private fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            try {
                val users = repository.getUsers()
                _uiState.value = UserUiState.Success(users)
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }
}
```

**Collector** chỉ cần collect **`1 Flow` duy nhất**:

```kotlin
class UserActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // CHỈ collect 1 Flow duy nhất
                viewModel.uiState.collect { state ->
                    // when{} với sealed class — compiler BẮT BUỘC handle đủ case
                    when (state) {
                        is UserUiState.Loading -> {
                            progressBar.isVisible = true
                            recyclerView.isVisible = false
                            tvError.isVisible = false
                        }
                        is UserUiState.Success -> {
                            progressBar.isVisible = false
                            recyclerView.isVisible = true
                            tvError.isVisible = false
                            adapter.submitList(state.users)
                        }
                        is UserUiState.Error -> {
                            progressBar.isVisible = false
                            recyclerView.isVisible = false
                            tvError.isVisible = true
                            tvError.text = state.message
                        }
                    }
                }
            }
        }
    }
}
```

**Ưu điểm**: Tại 1 thời điểm, **uiState.value** chỉ có thể là **đúng 1 trong 3 trạng thái** — không bao giờ có chuyện vừa Loading vừa Error cùng lúc. Đây chính là **Single Source of Truth**.

---

## 2. **Loading**, **Success**, **Error** — Pattern chuẩn, mở rộng thêm case

**`3` trạng thái cơ bản** là điểm khởi đầu, nhưng **thực tế thường cần bổ sung thêm**:

### **`Empty`** state

```kotlin
sealed class UserUiState {
    object Loading : UserUiState()
    data class Success(val users: List<User>) : UserUiState()
    data class Error(val message: String) : UserUiState()

    // Case thường bị bỏ sót — "load xong nhưng không có data"
    object Empty : UserUiState()
}
```

```kotlin
private fun loadUsers() {
    viewModelScope.launch {
        _uiState.value = UserUiState.Loading
        try {
            val users = repository.getUsers()
            _uiState.value = if (users.isEmpty()) {
                UserUiState.Empty // empty result
            } else {
                UserUiState.Success(users)
            }
        } catch (e: Exception) {
            _uiState.value = UserUiState.Error(e.message ?: "Lỗi không xác định")
        }
    }
}
```

### **`Refreshing`/`Pagination`** — thêm **sub-state** bên trong **Success**

Thêm cờ `isRefreshing` giúp **UI**/**UX** tốt hơn:

```kotlin
sealed class UserUiState {
    object Loading : UserUiState()
    data class Success(
        val users: List<User>,
        val isRefreshing: Boolean = false  // Pull-to-refresh trong lúc đã có data
    ) : UserUiState()
    data class Error(val message: String) : UserUiState()
    object Empty : UserUiState()
}
```

Khi **lỗi refresh** xảy ra, **display `data` cũ + báo lỗi nhẹ**:

```kotlin
fun refresh() {
    val current = _uiState.value
    if (current is UserUiState.Success) {
        // Giữ data cũ hiển thị, chỉ bật cờ isRefreshing
        _uiState.value = current.copy(isRefreshing = true)
    }
    viewModelScope.launch {
        try {
            val users = repository.getUsers()
            _uiState.value = UserUiState.Success(users, isRefreshing = false)
        } catch (e: Exception) {
            // Refresh lỗi — KHÔNG chuyển hẳn sang Error,
            // giữ data cũ + báo lỗi nhẹ
            (_uiState.value as? UserUiState.Success)?.let {
                _uiState.value = it.copy(isRefreshing = false)
            }
            // Lỗi refresh nên báo qua Event,
            // không phá UiState hiện tại
        }
    }
}
```

> _Đây là lý do **`Success` mang theo `isRefreshing: Boolean`** thay vì tạo object Refreshing riêng — **refresh không phải là 1 trạng thái khác Success, mà là 1 chi tiết bên trong Success** (vẫn có data cũ để hiển thị, chỉ thêm spinner nhỏ)._

---

## 3. **Single Source of Truth**

**Định nghĩa**: Tại **bất kỳ thời điểm nào**, chỉ có **1 nơi** duy nhất được coi là **"sự thật" về trạng thái hiện tại của UI** — mọi thứ khác chỉ là **phản chiếu (derive)** từ nguồn đó.

- **Sai**: **MULTIPLE** source of truth - `isLoading`, `users`, `errorMessage`, ...
  > _Các **state độc lập**, không đảm bảo nhất quán_
- **Đúng**: **SINGLE** suorce of truth - `uiState: StateFlow<UserUiState>`
  > _Chỉ **một biến**, luôn đảm bảo đúng 1 trạng thái hợp lệ._

Không chỉ trong `ViewModel`:

- `Room DB`→ **Single Source of Truth cho DATA** (_không phải Retrofit — API chỉ là nguồn ĐỒNG BỘ vào Room, UI luôn đọc từ Room_)
- `ViewModel` → **Single Source of Truth cho UI STATE** (_View không tự giữ state riêng, luôn đọc từ ViewModel.uiState_)

---

## 4. **Event Wrapper** — Vấn đề **re-emit** khi rotate

**Bài toán**: Giả sử dùng `StateFlow` để chứa cả **state** lẫn 1 sự kiện "**hiện lỗi 1 lần**":

```kotlin
// ❌ Sai — nhét error message vào UiState, dùng StateFlow
data class UserUiState(
    val users: List<User> = emptyList(),
    val errorMessage: String? = null  // Set 1 lần để hiện Toast
)
```

> _Khi **configuration change** xảy ra, observer **re-subcribe** state dẫn tới hiện lại `errorMessage` dù đã hiện trước đó_

### 4.1. **Legacy Solution - `Event` wrapper class**

**Event wrapper class** hay còn gọi là **Consumable Event**:

```kotlin
// Pattern cũ — "consumable" event
class Event<out T>(private val content: T) {

    private var hasBeenHandled = false // flag đánh dấu đã xử lý

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }
}
```

Khi này, `UiState` bọc `errorMessage` vào `Event<String?>`:

```kotlin
data class UserUiState(
    val users: List<User> = emptyList(),
    val errorEvent: Event<String>? = null  // Bọc trong Event
)
```

`View` chỉ xử lý khi **chưa được handle**:

```kotlin
// View — chỉ xử lý nếu CHƯA được handle
viewModel.uiState.collect { state ->
    state.errorEvent?.getContentIfNotHandled()?.let { message ->
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    // Nếu đã handled (do rotate) → getContentIfNotHandled() trả null
    // → không hiện Toast lại
}
```

**Nhược điểm của `Event` wrapper**:

- Thêm 1 lớp bọc phức tạp, dễ quên gọi đúng cách
- Có race condition tiềm ẩn khi nhiều collector cùng đọc 1 lúc (mutable state **`hasBeenHandled` không thread-safe hoàn toàn**).

### 4.2. **Modern Solution - `SharedFlow`**

`Flow` cho phép **tách hẳn `Event` ra khỏi `State`**, dùng `SharedFlow`:

```kotlin
class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    // Event tách RIÊNG, dùng SharedFlow — không cần Event wrapper nữa
    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()

    private fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            try {
                val users = repository.getUsers()
                _uiState.value = UserUiState.Success(users)
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error(e.message ?: "Lỗi")
                _errorEvent.emit(e.message ?: "Lỗi không xác định")
            }
        }
    }
}
```

---

## 5. `UiEffect` & `UiState` — phân biệt **state** / **effect**

**`UiState`**:

- Biểu diễn **Ui đang ở trạng thái nào** (Loading, Success, Error, Empty, ...) **tại một thời điểm**.
- **CÓ THỂ đọc lại nhiều lần**, giá trị luôn "đúng" tại mọi thời điểm.
- Công cụ: `StateFlow`

**`UiEffect`** (aka **UiEvent** / **SideEffect**):

- Biểu diễn **HÀNH ĐỘNG cần xảy ra MỘT LẦN DUY NHẤT**
- **KHÔNG nên đọc lại** — chỉ có ý nghĩa tại đúng thời điểm phát sinh
- Công cụ: `SharedFlow`

### **`Error`** nên là **state** hay **effect**? - based on context:

- **`Error` as state**:

  ```kotlin
  // Trường hợp A — Error THAY THẾ toàn bộ màn hình (full-screen error)
  // → Đây LÀ State, vì UI cần biết "đang ở trạng thái lỗi" liên tục,
  //   kể cả sau khi rotate vẫn nên hiện lại màn hình lỗi y hệt
  sealed class UserUiState {
      object Loading : UserUiState()
      data class Success(val users: List<User>) : UserUiState()
      data class Error(val message: String) : UserUiState()  // ← State
  }
  ```

- **`Error` as effect**:

  ```kotlin
  // Trường hợp B — Error hiện dưới dạng Toast/Snackbar thoáng qua,
  // KHÔNG thay thế nội dung màn hình (data cũ vẫn hiện phía sau)
  // → Đây LÀ Effect, vì Toast không nên hiện lại sau rotate
  class UserViewModel : ViewModel() {
      private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Success(emptyList()))
      val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

      private val _effect = MutableSharedFlow<UserUiEffect>()
      val effect: SharedFlow<UserUiEffect> = _effect.asSharedFlow()

      sealed class UserUiEffect {
          data class ShowToast(val message: String) : UserUiEffect()  // ← Effect
      }

      fun refresh() {
          viewModelScope.launch {
              try {
                  val users = repository.getUsers()
                  _uiState.value = UserUiState.Success(users)
              } catch (e: Exception) {
                  // Refresh lỗi — giữ data cũ, chỉ báo Toast thoáng qua
                  _effect.emit(UserUiEffect.ShowToast("Lỗi khi làm mới: ${e.message}"))
              }
          }
      }
  }
  ```

---

## Example: Pattern kết hợp đầy đủ **state** + **effect**

`ViewModel`: **state** + **effect** management

```kotlin
class UserViewModel(private val repository: UserRepository) : ViewModel() {

    // ── State — StateFlow, luôn có giá trị, re-render OK khi rotate ──
    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    // ── Effect — SharedFlow, chỉ xảy ra 1 lần, KHÔNG re-fire khi rotate ──
    private val _effect = MutableSharedFlow<UserUiEffect>()
    val effect: SharedFlow<UserUiEffect> = _effect.asSharedFlow()

    sealed class UserUiState {
        object Loading : UserUiState()
        data class Success(val users: List<User>) : UserUiState()
        data class Error(val message: String) : UserUiState()
    }

    sealed class UserUiEffect {
        data class ShowToast(val message: String) : UserUiEffect()
        data class NavigateToDetail(val userId: Long) : UserUiEffect()
        object NavigateBack : UserUiEffect()
    }

    fun onUserClick(user: User) {
        viewModelScope.launch {
            _effect.emit(UserUiEffect.NavigateToDetail(user.id))
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            try {
                repository.delete(user)
                _effect.emit(UserUiEffect.ShowToast("Đã xóa ${user.name}"))
                // Reload state sau khi xóa thành công
                loadUsers()
            } catch (e: Exception) {
                _effect.emit(UserUiEffect.ShowToast("Lỗi khi xóa: ${e.message}"))
            }
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            try {
                _uiState.value = UserUiState.Success(repository.getUsers())
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error(e.message ?: "Lỗi")
            }
        }
    }
}
```

`Activity`: **state/effect** collector

```kotlin
class UserActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Collect STATE — render UI mỗi khi state đổi
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UserViewModel.UserUiState.Loading -> showLoading()
                            is UserViewModel.UserUiState.Success -> showUsers(state.users)
                            is UserViewModel.UserUiState.Error -> showFullScreenError(state.message)
                        }
                    }
                }

                // Collect EFFECT — thực hiện hành động 1 lần
                launch {
                    viewModel.effect.collect { effect ->
                        when (effect) {
                            is UserViewModel.UserUiEffect.ShowToast ->
                                Toast.makeText(this@UserActivity, effect.message, Toast.LENGTH_SHORT).show()

                            is UserViewModel.UserUiEffect.NavigateToDetail ->
                                findNavController().navigate(
                                    R.id.action_to_detail,
                                    bundleOf("userId" to effect.userId)
                                )

                            is UserViewModel.UserUiEffect.NavigateBack ->
                                findNavController().navigateUp()
                        }
                    }
                }
            }
        }
    }
}
```
