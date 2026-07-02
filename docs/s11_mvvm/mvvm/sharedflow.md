# **`SharedFlow` - event stream**

## 1. **Vấn đề của `LiveData` và `StateFlow`**

Cả `LiveData` và `StateFlow` đều gặp phải vấn đề khi xử lý **event stream** (luồng sự kiện) trong **MVVM architecture**.

```kotlin
// ❌ Dùng StateFlow cho Event —
// gây bug "re-fire khi rotate"
class UserViewModel : ViewModel() {

    private val _showToast = MutableStateFlow<String?>(null)
    val showToast: StateFlow<String?> = _showToast.asStateFlow()

    fun deleteUser() {
        _showToast.value = "Đã xóa user"
    }
}
```

Cụ thể:

> _Khi **configuration changes** xảy ra dẫn tới các **observer** thực hiện **re-subcribe**, trở thành **new subscriber**. <br/>`LiveData` và `StateFlow` sẽ **replay last value cho new subscriber**, dẫn đến việc **event** được **replay** lại mặc dù đã được **consumed** trước đó._

```kotlin
class UserActivity : AppCompatActivity() {
    override fun onCreate(...) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.showToast.collect { message ->
                    message?.let { Toast.makeText(this@UserActivity, it, Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }
}
```

**Nguyên nhân gốc**: `StateFlow` theo đúng bản chất của nó — **luôn giữ và emit giá trị hiện tại cho collector mới**.

- Điều này **ĐÚNG và cần thiết cho `State`** (danh sách user nên hiện ngay khi rotate xong)
- Nhưng lại **SAI với `Event`** (toast không nên hiện lại khi không có hành động mới).

---

## 2. `SharedFlow` - **solution** cho **`Event` Stream**

**Bản chất của `SharedFlow`**:

- **KHÔNG** có **giá trị hiện tại** để giữ lại như `StateFlow`.
- Chỉ đơn thuần là **`emit` - phát** sự kiện cho **TẤT CẢ** collector đang lắng nghe tại thời điểm emit.
- Collector mới subcribe **SAU `emit`** đã chạy, mặc định **KHÔNG** nhận được **events** trước đó.

```kotlin
class UserViewModel : ViewModel() {

    // SharedFlow — KHÔNG giữ giá trị "hiện tại" như StateFlow
    private val _event = MutableSharedFlow<String>()
    val event: SharedFlow<String> = _event.asSharedFlow()

    fun deleteUser() {
        viewModelScope.launch {
            // emit() là suspend fun — khác với StateFlow.value = ... (đồng bộ)
            _event.emit("Đã xóa user")
        }
    }
}
```

### 2.1. **suspend fun `emit()`**

Trong `StateFlow`, **`set` operation** là **synchronous**, không cần coroutine:

```kotlin
// StateFlow — set đồng bộ, KHÔNG cần coroutine
_users.value = newList
```

Với `SharedFlow`:

- **`emit()` là suspend function**, cần được gọi trong **coroutine**:

  ```kotlin
  // SharedFlow — emit() là suspend fun,
  // BẮT BUỘC trong coroutine
  viewModelScope.launch {
      _event.emit("message")
  }
  ```

- Có thể dùng `tryEmit()` để **emit đồng bộ**, nhưng **không đảm bảo thành công** nếu buffer đầy:

  ```kotlin
  // Có thể dùng tryEmit() nếu không muốn dùng coroutine
  // (ít phổ biến hơn)
  _event.tryEmit("message") // Không suspend —
                            //nhưng có thể fail nếu buffer đầy
  ```

**Why `emit()` is suspend function?** - Cơ chế **backpressure: kiểm soát luồng dữ liệu**:

- `SharedFlow` **có thể có buffer** giới hạn
- Nếu **buffer đầy**, `emit()` sẽ **suspend** cho đến khi có collector nào đó **consume** giá trị, giải phóng buffer.

### 2.2. **Collector `collect()` from `SharedFlow`**

```kotlin
class UserActivity : AppCompatActivity() {
    override fun onCreate(...) {

        // collect() là suspend fun — cần trong coroutine
        lifecycleScope.launch {

            // Lifecycle-aware collector
            // -> chỉ collect khi STARTED+
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // collect() là suspend fun — cần trong coroutine
                viewModel.event.collect { event ->
                    // handle event
                    Toast.makeText(this@UserActivity, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
```

---

## 3. **`sealed class` - event types**

Trong thực tế, 1 `ViewModel` thường cần **phát nhiều loại event** khác nhau — dùng `sealed class` để **type-safe**:

**Định nghĩa `sealed class` cho event types**:

```kotlin
sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class NavigateToDetail(val userId: Long) : UiEvent()
    object NavigateBack : UiEvent()
    data class ShowError(val message: String) : UiEvent()
}
```

**Sử dụng `SharedFlow` với `sealed class` trong `ViewModel`**:

```kotlin
class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _event = MutableSharedFlow<UiEvent>()
    val event: SharedFlow<UiEvent> = _event.asSharedFlow()

    fun deleteUser(user: User) {
        viewModelScope.launch {
            try {
                repository.delete(user)
                _event.emit(UiEvent.ShowToast("Đã xóa ${user.name}"))
            } catch (e: Exception) {
                _event.emit(UiEvent.ShowError("Lỗi: ${e.message}"))
            }
        }
    }

    fun onUserClick(user: User) {
        viewModelScope.launch {
            _event.emit(UiEvent.NavigateToDetail(user.id))
        }
    }
}
```

**Collector trong `Activity`/`Fragment`**: `sealed class` giúp **type-safe** và **`when` exhaustive**:

```kotlin
class UserActivity : AppCompatActivity() {
    override fun onCreate(...) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->

                    // when với sealed class —
                    // compiler bắt buộc handle đủ case
                    when (event) {
                        is UserViewModel.UiEvent.ShowToast ->
                            Toast.makeText(this@UserActivity, event.message, Toast.LENGTH_SHORT).show()

                        is UserViewModel.UiEvent.NavigateToDetail ->
                            findNavController().navigate(
                                R.id.action_to_detail,
                                bundleOf("userId" to event.userId)
                            )

                        is UserViewModel.UiEvent.NavigateBack ->
                            findNavController().navigateUp()

                        is UserViewModel.UiEvent.ShowError ->
                            Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()

                        // không cần else branch
                    }
                }
            }
        }
    }
}
```

---

## 4. **Config `SharedFlow`**: `replay`, `extraBufferCapacity`, ...

By default, `SharedFlow`:

- **Không có buffer** (`buffer = 0`) -> `emit()` sẽ **suspend cho tới khi có collector** thực hiện `collect()`.
- **Không có replay** (`replay = 0`) -> Collector mới **chỉ nhận events mới nhất**, KHÔNG nhận được các **events đã `emit()` trước đó**

Tuy nhiện, hoàn toàn có thể **config** `SharedFlow` để có **behavior khác**:

```kotlin
MutableSharedFlow<UiEvent>(
    replay = 0,                    // Số lượng giá trị cũ gửi cho collector MỚI
    extraBufferCapacity = 0,       // Buffer thêm ngoài replay
    onBufferOverflow = BufferOverflow.SUSPEND  // Hành vi khi buffer đầy
)
```

Các **`arguments`**:

- `replay`:
  - **Mặc định**: `replay = 0` → Collector mới **KHÔNG nhận được event cũ** — đúng ý nghĩa "event"
    > _→ Đây là **config phù hợp nhất cho UI Event** (Toast, Navigate...)_
  - `replay = 1` → Collector mới **NHẬN ĐƯỢC giá trị cuối cùng đã `emit`** -> **behavior** gần giống với `StateFlow`
    > _→ **Hiếm dùng** cho UI Event, có thể dùng cho cache logic riêng_
  - Ngoài ra, có thể cấu hình `replay > 1`
- `extraBufferCapacity`:
  - **Mặc định**: `extraBufferCapacity = 0` → `emit()` sẽ **SUSPEND nếu không có collector** nào đang lắng nghe
  - Cấu hình `extraBufferCapacity > 0` → `emit()` sẽ **không suspend** ngay.
    - Nếu **buffer CHƯA đầy**, `emit()` sẽ được thực hiện ngay.
    - Nếu **buffer ĐÃ đầy**, `emit()` sẽ **suspend** cho tới khi collector nào đó **consume** giá trị, giải phóng buffer.

    > _→ Hay dùng để tránh trường hợp event bị mất do timing_

- `onBufferOverflow`:
  - **Mặc định**: `onBufferOverflow = BufferOverflow.SUSPEND` → Khi buffer đầy, `emit()` sẽ **suspend** cho tới khi collector nào đó **consume** giá trị, giải phóng buffer.
  - Cấu hình `onBufferOverflow = BufferOverflow.DROP_OLDEST` → Khi buffer đầy, **giá trị CŨ NHẤT sẽ bị drop**, giá trị mới sẽ được emit ngay.
  - Cấu hình `onBufferOverflow = BufferOverflow.DROP_LATEST` → Khi buffer đầy, **giá trị MỚI NHẤT sẽ bị drop**, giá trị cũ vẫn giữ lại.

Cấu hình phổ biến cho **UI Event**:

```kotlin
private val _event = MutableSharedFlow<UiEvent>(
    extraBufferCapacity = 1  // Tránh mất event do timing race condition
)
```
