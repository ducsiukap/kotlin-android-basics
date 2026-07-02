# `Flow` replaces `LiveData` in **MVVM architecture**

## 1. **Bối cảnh**:

- `2017` → **`LiveData` ra mắt** (cùng Architecture Components)
- `2018` → **Kotlin Coroutines** trở thành **stable**
- `2019` → **Kotlin `Flow`** phát triển mạnh, tích hợp sâu vào Android.

`LiveData` được thiết kế khi **Android CHƯA có coroutines/Flow**

> _Đây là lí do vì sao `LiveData` có API riêng (observe, Transformations, ...) thay vì dùng chung với Kotlin `Flow`_

---

## 2. **Vấn đề của `LiveData`**

### **Vấn đề 1**: `LiveData` chỉ chạy trên **Main Thread**

```kotlin
class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    fun loadUsers() {
        // setValue() chỉ gọi được trên Main Thread
        // Muốn set từ background thread → phải dùng postValue()
        Thread {
            val data = repository.getUsersBlocking() // blocking call
            _users.postValue(data)  // Switch ngầm sang Main Thread
        }.start()
    }
}
```

Dù `postValue()` có thể set từ background thread, có vấn đề **tinh vi**: nếu gọi `postValue()` nhiều lần trong **1 khoảng thời gian ngắn** (_trước khi Main Thread kịp xử lý_), chỉ có **giá trị CUỐI CÙNG** được set và gửi đến observer — các giá trị ở giữa bị mất.

### **Vấn đề 2**: `LiveData` thiếu **operator mạnh**

```kotlin
// LiveData — operator giới hạn
val searchResults = Transformations.switchMap(_query) { query ->
    repository.search(query)
}
// Không có debounce — gõ mỗi ký tự đều trigger search ngay lập tức
```

- `LiveData` chỉ có `map()`, `switchMap()`.
- `Flow` có đầy đủ **toàn bộ operator của Kotlin Coroutines**: debounce, filter, distinctUntilChanged, combine, zip, flatMapConcat, flatMapMerge, flatMapLatest, retry, catch, flowOn, buffer, conflate, ...

### **Vấn đề 3**: `LiveData` không phải **pure `Kotlin`**

`LiveData` thuộc `androidx.lifecycle` package -> **CHỈ chạy được trên Android**. Dẫn tới:

- **`Domain` layer** đáng lẽ chỉ chứa **pure Kotlin** code
- Nếu **`Repository` trả về `LiveData`**, thì nó bị dính Android -> khó reuse logic ở other modules (backend, KMP, ...).

Ngược lại, `Flow` thuộc `kotlinx.coroutines` package - là **pure Kotlin** package. Nhờ vậy, nó có thể giữ cho `Domain` layer giữ được tính **pure Kotlin code**

### **Vấn đề 4**: `LiveData` chỉ là `State`, **không có khái niệm `Event` tốt**

- `LiveData` chỉ làm tốt nhiệm vụ làm **state holder** - giữ `STATE`.
- Khi cần biểu diễn **`Event` (one-time)** như show Toast, navigate, ... thì **`LiveData` KHÔNG có cơ chế native** cho việc này

Cụ thể hơn: `LiveData` luông giữ **giá trị mới nhất** thay vì trả lời câu hỏi **event đã được xử lý chưa**.<br/>
Vấn đề xảy ra khi có **configuration change** như rotate screen, ...

> _Khi một **observer mới được đăng ký** và **ở trạng thái `active`** (STARTED hoặc RESUMED), `LiveData` sẽ **ngay lập tức gửi giá trị mới nhất** mà nó đang giữ (nếu có)._

Điều này khiến callback của observer được gọi **một lần nữa** (_và mỗi lần configuration change xảy ra, dù event đã được xử lý trước đó_) → dẫn tới **bug**/**bad experience**.

**Usecase: Login**:

ViewModel:

```kotlin
class LoginViewModel : ViewModel() {

    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    fun login(username: String, password: String) {
        // gọi API...

        // API trả về thành công
        _loginSuccess.value = true
    }
}
```

Observer:

```kotlin
viewModel.loginSuccess.observe(viewLifecycleOwner) { success ->
    if (success) {
        Toast.makeText(
            requireContext(),
            "Đăng nhập thành công",
            Toast.LENGTH_SHORT
        ).show()

        findNavController().navigate(
            R.id.action_login_to_home
        )
    }
}
```

**Normal flow**:

1. User bấm nút Login.
2. API trả về **thành công**.
3. `_loginSuccess.value = true`.
4. Observer nhận `true`.
5. Hiện **Toast**.
6. **Chuyển sang Home**.

Tuy nhiên, giả sử sau khi API trả về thành công: `loginSuccess = true` nhưng trước khi **navigation hoàn tất**, user thực hiện **rotate screen**. <br/>
Khi đó, observer được **re-register** và nhận giá trị `true` từ `LiveData` → dẫn tới **chạy lại `Toast` và `navigate()` lần nữa** → **bug**.

Khi này, cần dùng **workaround**: `SingleLiveEvent`, `Event wrapper` class, ... (**_cách xử lý truyền thống_**)

- `Event` Wraper:

  ```kotlin
  class Event<out T>(private val content: T) {

      private var hasBeenHandled = false

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

  ViewModel:

  ```kotlin
  private val _toastEvent =
      MutableLiveData<Event<String>>()

  val toastEvent: LiveData<Event<String>>
      get() = _toastEvent

  fun save() {
      _toastEvent.value =
          Event("Lưu thành công")
  }
  ```

  Fragment/Activity:

  ```kotlin
  viewModel.toastEvent.observe(viewLifecycleOwner) { event ->
      event.getContentIfNotHandled()?.let {
          Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      }
  }
  ```

- `SingleLiveEvent`:

  ```kotlin
  class SingleLiveEvent<T> : MutableLiveData<T>() {

      private val pending = AtomicBoolean(false)

      @MainThread
      override fun observe(
          owner: LifecycleOwner,
          observer: Observer<in T>
      ) {
          super.observe(owner) { t ->
              if (pending.compareAndSet(true, false)) {
                  observer.onChanged(t)
              }
          }
      }

      @MainThread
      override fun setValue(t: T?) {
          pending.set(true)
          super.setValue(t)
      }
  }
  ```

  ViewModel:

  ```kotlin
  private val _navigateEvent = SingleLiveEvent<Unit>()
  ```

  > _Google từng dùng mẫu này trong ví dụ MVVM cũ. Tuy nhiên nó có một số hạn chế, đặc biệt với nhiều observer_
