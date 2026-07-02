# `collectAsState`, **Lifecycle-aware Collection**

## 1. `repeatOnLifecycle()` - **BẮT BUỘC** khi collect `Flow` trong `View`

### 1.1. **`repeatOnLifecycle()` là gì**?

```kotlin
// ❌ Sai — dùng lifecycleScope.launch trực tiếp,
// KHÔNG bọc repeatOnLifecycle
class UserActivity : AppCompatActivity() {
    override fun onCreate(...) {
        lifecycleScope.launch {
            viewModel.users.collect { users ->
                adapter.submitList(users)
            }
        }
    }
}
```

**Vấn đề**: `lifecycleScope` chỉ **tự cancel khi `Activity` bị DESTROY**. Khi **`Activity` STOPPED** (_vd: user nhấn Home, app vào background_), **`collect{}` VẪN ĐANG CHẠY**:

- lãng phí tài nguyên - **waste CPU** - khi user không thấy gì.
- `collect {}` vẫn nhận dữ liệu mới và **cố update UI**, có thể gây crash

```kotlin
// ✅ Đúng — bọc bằng repeatOnLifecycle
class UserActivity : AppCompatActivity() {
    override fun onCreate(...) {
        lifecycleScope.launch {

            // chỉ collect khi Activity STARTED+
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.users.collect { users ->
                    adapter.submitList(users)
                }
            }
        }
    }
}
```

Cơ chế của `repeatOnLifecycle(Lifecycle.State.STARTED)`:

- `Lifecycle` đạt **STARTED** → block bên trong **`{ }` bắt đầu chạy** (bao gồm collect)
- `Lifecycle` xuống **STOPPED** → coroutine bên trong **`{ }` bị CANCEL** hoàn toàn

Chi tiết:

| Activity Lifecycle | behavior of `repeatOnLifecycle(Lifecycle.State.STARTED)` |
| ------------------ | -------------------------------------------------------- |
| `onCreate()`       | chưa STARTED → chưa chạy                                 |
| `onStart()`        | STARTED → **bắt đầu CHẠY**                               |
| `onResume()`       | RESUMED > STARTED → **vẫn đang CHẠY**                    |
| `onPause()`        | PAUSED → STARTED → **vẫn đang CHẠY**                     |
| `onStop()`         | STOPPED → cancel coroutine                               |
| `onDestroy()`      | DESTROYED → cancle lifecycleScope.                       |

> _**Note**: `Lifecycle` lên **STARTED lại sau khi DESTROY** → **block `{ }` chạy LẠI TỪ ĐẦU** (collect lại)_

### 1.2. **Tại sao dùng `launch { repeatOnLifecycle { } }` thay vì gọi trực tiếp**?

Bản thân `repeatOnLifecycle` là **suspend function**, phải được chạy trong **Coroutine scope** nào đó.

Pattern chuẩn:

- `lifecycleScope.launch { }` → Tạo 1 coroutine "ngoài cùng", tồn tại đến khi Activity DESTROYED
- `repeatOnLifecycle(STARTED) { }` → Bên trong coroutine ngoài, tạo và hủy "coroutine con" lặp đi lặp lại mỗi khi qua lại ngưỡng STARTED/STOPPED

```kotlin
// Pattern chuẩn — luôn đi theo cặp này
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        // Có thể launch NHIỀU coroutine con ở đây
        // để collect NHIỀU Flow song song
        launch { viewModel.users.collect { ... } }
        launch { viewModel.event.collect { ... } }
    }
}
```

### 1.3. Trong `Fragment`, dùng `viewLifecycleOwner.lifecycleScope`

`Fragment` có **2 lifecycle** riêng biệt:

- **`Fragment` lifecycle - `lifecycleScope`**: tồn tại lâu hơn, có thể giữ trong backstack.
- **`View` lifecycle - `viewLifecycleOwner.lifecycleScope`**: ngắn hơn, gắn với `onCreateView`/`onDestroyView`.

Nếu dùng `lifecycleScope` (**Fragment lifecycle**), khi `Fragment` **bị đưa vào backstack** (_`View` bị destroy nhưng `Fragment` vẫn tồn tại_), `collect{}` coroutine vẫn chạy và **update View không còn tồn tại** -> CRASH

Vì vậy, **luôn dùng `viewLifecycleOwner.lifecycleScope`** khi collect trong `onViewCreated()` của Fragment — KHÔNG dùng lifecycleScope trần

```kotlin
class UserFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ ĐÚNG — dùng viewLifecycleOwner
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.users.collect { ... }
            }
        }
    }
}
```

---

## 2. `flowWithLifecycle()`

`flowWithLifecycle()` là **cách viết gọn hơn cho MỘT `Flow`** so với `repeatOnLifecycle()`

```kotlin
lifecycleScope.launch {
    viewModel.users
        // gắn lifecycle vào Flow
        .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
        // chỉ collect khi STARTED+
        .collect { users ->
            adapter.submitList(users)
        }
}
```

**Cơ chế bên dưới** — `flowWithLifecycle` thực chất **wrap `repeatOnLifecycle`**:

```kotlin
// Source code rút gọn của flowWithLifecycle
fun <T> Flow<T>.flowWithLifecycle(
    lifecycle: Lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED
): Flow<T> = callbackFlow {
    // wrap repeatOnLifecycle bên trong callbackFlow
    lifecycle.repeatOnLifecycle(minActiveState) {
        this@flowWithLifecycle.collect { send(it) }
    }
    close()
}
```

**NOTE**: **KHÔNG** dùng chung `combine` với `flowWithLifecycle` sai cách:

```kotlin
// ⚠️ Coi chừng — nếu combine NHIỀU StateFlow rồi mới flowWithLifecycle
// thì tất cả bị gộp vào 1 lifecycle check duy nhất — thường vẫn OK
// nhưng cần đảm bảo combine() được đặt TRƯỚC flowWithLifecycle()
.combine(viewModel.users, viewModel.filter) { users, filter -> ... }
.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED) // chạy sau combine
.collect { ... }
```

---

## 3. `collectAsState()` - **Jetpack Compose** only + **Lifecycle-aware Collection**

> _**Lưu ý**: **API này chỉ dùng khi UI viết bằng Jetpack Compose** — không dùng trong View system (XML + Activity/Fragment)._

```kotlin
@Composable
fun UserScreen(viewModel: UserViewModel) {
    // collectAsState() — thay thế cho repeatOnLifecycle + collect
    val users by viewModel.users.collectAsState()

    LazyColumn {
        items(users) { user ->
            Text(user.name)
        }
    }
}
```

`collectAsState()` bên dưới dùng `LaunchedEffect` + `collect`,
**tự cancel khi Composable rời khỏi Composition** → Tương tự tinh thần với `repeatOnLifecycle`, nhưng là **API
RIÊNG cho Compose** — không thể dùng ngoài `@Composable` function

Biến thể `collectAsStateWithLifecycle()` — **lifecycle-aware** hơn, tự động cancel khi `Activity`/`Fragment` STOPPED:

```kotlin
@Composable
fun UserScreen(viewModel: UserViewModel) {
    // Bản "an toàn hơn" — có tích hợp thêm lifecycle check
    val users by viewModel.users.collectAsStateWithLifecycle()
    ...
}
```
