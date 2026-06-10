# **_Activity_**'s lifecycle

## 1. Why `Activity` need **lifecycle**?

**Android** là môi trường **resource-constrained** — _RAM có hạn_, _pin có hạn_.

- OS có quyền **kill process** của app bất kỳ lúc nào khi cần tài nguyên.
- Ngoài ra, user liên tục **_chuyển đổi giữa các app_**, **_xoay màn hình_**, **_nhận cuộc gọi_**, ....

> _Màn hình app **KHÔNG luôn ở trạng thái “đang mở” hoặc “đã đóng”**. Nó trải qua nhiều **trạng thái trung gian**._

**Lifecycle** là hệ thống `callback` Android OS gọi vào app của bạn để thông báo **trạng thái thay đổi** — cho bạn cơ hội phản ứng đúng cách: _lưu data khi sắp bị hủy_, _dừng animation khi không visible_, _giải phóng resource khi không cần_, ...<br/>
Các trạng thái chính:

- (1) Activity được khởi tạo
- (2) `onCreate()`: khởi tạo/thiết lập UI, binding, ViewModel, load data, ...
- (3) `onStart()`: App visible, **chưa focus**, chuẩn bị tương tác.
- (4) `onResume()`: Activity **đang được focus**, nhận tương tác
- (5) User đang dùng app
- (6) `onPause()`: **Mất focus** (ví dụ: có dialog, chuyển app, ... )
- (7) `onStop()`: Không còn visible
- (8) `onDestroy()`: Sắp bị hủy
- (9) Activity bị hủy hoàn toàn

Ngoài ra, còn có các callback khác như `onRestart()`, `onSaveInstanceState()`, ... để xử lý các tình huống đặc biệt.

### **Không phải lúc nào cũng đi qua toàn bộ `6` bước — tùy tình huống:**

| Tình huống             | Trạng thái                                                                                      |
| ---------------------- | ----------------------------------------------------------------------------------------------- |
| User **mở app**        | `onCreate() → onStart() → onResume()`                                                           |
| User **chuyển app**    | `onPause() → onStop()`                                                                          |
| User **quay lại**      | `onRestart() → onStart() → onResume()`                                                          |
| User **nhấn back**     | `onPause() → onStop() → onDestroy()`                                                            |
| User **xoay màn hình** | `onPause() → onStop() → onDestroy() → onCreate() → onStart() → onResume()` (destroy + recreate) |

---

## 2. `onCreate()` — tạo Activity

Callback `onCreate()` được Android OS gọi **khi hệ thống tạo một Activity instance** chứa nó.

Tại đây, thường là nơi:

- `inflate` **View Binding**, gọi `setContentView()`
- Khởi tạo UI:
  - Thiết lập **listener** cho button/clickable, ...
  - Khởi tạo các thành phần cần thiết, **observer**, **viewmodel**, ...
- Đọc dữ liệu trạng thái được lưu trước đó (nếu có) từ `savedInstanceState`

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    // luôn gọi super.onCreate() đầu tiên
    // để đảm bảo Activity được thiết lập đúng cách
    super.onCreate(savedInstanceState)

    // inflate layout + set content view
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root) // setup UI

    // lấy ViewModel, thiết lập listener, observer
    viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
    setupListeners()                 // setup click listener
    observeData()                    // observe LiveData

    // savedInstanceState: Bundle? chứa data đã lưu trước đó
    //  + null nếu Activity được tạo lần đầu
    //  + non-null nếu Activity được recreate (xoay màn hình, ...)
}
```

> _Không nên thực hiện: **Network call**, **heavy computation**, ... tại đây — vì **`onCreate` block `main` thread** cho đến khi xong._

> _Tài liệu Android mô tả `onCreate()` là nơi thực hiện **logic khởi tạo cơ bản CHẠY MỘT LẦN** trong vòng đời của Activity instance_

---

## 3. `onStart()` — Activity **visible**, _non-interactive_

Sau khi **`onCreate()` hoàn thành**, Android OS gọi `onStart()`, báo hiệu rằng Activity **đang trở nên visible** với user, tại đây:

- Activity đã **visible**
- Nhưng **chưa được focus**, chưa nhận tương tác trực tiếp

> _Tài liệu Android mô tả `onStart()` là lúc **Activity trở nên hiển thị** và **chuẩn bị vào `foreground`** để tương tác_

```kotlin
override fun onStart() {
    super.onStart()
    // Activity trở nên VISIBLE với user
    // Nhưng chưa ở foreground, chưa nhận input

    // Dùng cho:
    // → Bắt đầu animation không cần input
    // → Đăng ký BroadcastReceiver nếu cần khi visible
    // → Refresh UI data nhẹ
}
```

---

## 4. `onResume()` — Activity **đang được focus**, người dùng **có thể tương tác**

Khi **Activity** ở trạng thái `resumed`:

- Activity nằm ở **foreground**
- User có thể **tương tác trực tiếp với UI**, có thể bấm nút, nhập text, vuốt, ...

> _Đây thường là trạng thái mà người dùng gọi đơn giản là “**màn hình đang mở**”_.

```kotlin
override fun onResume() {
    super.onResume()
    // Activity ở FOREGROUND
    // Đang nhận input từ user (touch, keyboard)
    // Đây là trạng thái "đang hoạt động bình thường"

    // Dùng cho:
    // → Resume camera preview
    // → Resume sensor listener (accelerometer, GPS)
    // → Resume animation/game loop
    // → Resume video playback
}
```

> _Tài liệu Android xác định **`onResume()` là lúc Activity `vào foreground` và `tương tác` với người dùng**. Activity ở trạng thái này cho tới khi một sự kiện làm app mất focus_

Một ví dụ sử dụng thực tế là bật camera preview khi màn hình thực sự sẵn sàng tương tác:

```kotlin
override fun onResume() {
    super.onResume()

    // Ví dụ minh họa:
    startCameraPreview()
}
```

---

## 5. `onPause()` — Activity **mất focus**, _có thể vẫn visible_

Trạng thái `onPause()` là dấu hiệu đầu tiên cho thấy **người dùng đang rời Activity**.

Tại đây, Activity **không còn được focus**, nhưng vẫn có thể _visible một phần_:

- Một **dialog** hoặc **Activity** xuất hiện phía trên
- App chạy trong chế độ **multi-window**, và user chuyển sang app khác -> focus chuyển đi
- User chuyển màn hình khác, ...

> _Tài liệu Android lưu ý rằng **`onPause()` không nhất thiết đồng nghĩa với Activity sắp bị hủy**. Activity có thể vẫn visible nhưng không còn foreground._

```kotlin
override fun onPause() {
    super.onPause()
    // Activity MẤT FOCUS nhưng vẫn có thể visible một phần
    // (dialog che một phần, multi-window mode)

    // Dùng cho:
    // → Pause camera preview
    // → Pause sensor listener
    // → Lưu data chưa lưu (draft)
    // → Pause animation/game loop

    // KHÔNG làm:
    // → Việc nặng, blocking — vì onPause phải xong nhanh
    //   để Activity tiếp theo có thể onResume
}
```

**Lưu ý**: không nên thực hiện **heavy task** tại `onPause()` vì nó block `main thread`, **callback này cần hoàn thành nhanh** để màn hình tiếp theo xuất hiện mượt mà

---

## 6. `onStop()` — Activity **không còn visible**

Trạng thái `onStop()` báo hiệu rằng **Activity không còn visible** với user.

```kotlin
override fun onStop() {
    super.onStop()
    // Activity KHÔNG CÒN VISIBLE
    // User đã chuyển sang app khác hoặc màn hình khác

    // Dùng cho:
    // → Dừng network call không quan trọng
    // → Giải phóng resource nặng
    // → Lưu data quan trọng hơn (persist to database)
    // → Hủy đăng ký BroadcastReceiver

    // Activity object VẪN còn trong RAM (chưa bị hủy)
    // → Có thể quay lại mà không cần onCreate lại
}
```

> _Đây là nơi phù hợp hơn để **dừng những tác vụ không cần chạy khi màn hình bị che hoàn toàn**. Tài liệu Android đưa ra ví dụ như **dừng animation** hoặc điều chỉnh **cập nhật vị trí**._

---

## 7. `onRestart()` — quay lại Activity đã bị stop (`onStop`)

Callback `onRestart()` xảy ra khi **Activity đã đi vào `onStop()`** nhưng **chưa bị hủy**, sau đó **quay trở lại**.

```kotlin
override fun onRestart() {
    super.onRestart()
    // Gọi khi Activity quay lại từ trạng thái onStop
    // (không phải từ onCreate)
    // Thứ tự: onStop → onRestart → onStart → onResume

    // Ít dùng trong thực tế
    // Dùng khi cần làm gì đó CHỈ khi quay lại, không phải lần đầu mở
}
```

---

## 8. `onDestroy()` — Activity sắp bị hủy

Trạng thái `onDestroy()` báo hiệu rằng **Activity sắp bị hủy hoàn toàn**, phục vụ mục đích:

- Final cleanup trước khi Activity bị hủy
- Cancel background job, giải phóng resource chưa giải phóng, ...

```kotlin
override fun onDestroy() {
    super.onDestroy()

    // Dọn các tài nguyên chưa được giải phóng trước đó
}
```

Android có thể gọi `onDestroy()` trong **hai trường hợp phổ biến**:

- Activity **thực sự kết thúc**, ví dụ do `finish()`.
- Activity bị **tái tạo tạm thời** do configuration change, chẳng hạn xoay màn hình.

Có thể phân biệt hai trường hợp này bằng cách kiểm tra `isFinishing`:

```kotlin
override fun onDestroy() {
    super.onDestroy()
    // Activity sắp bị HỦY HOÀN TOÀN
    // Có 2 trường hợp:
    // 1. User nhấn Back / gọi finish() → hủy thực sự
    // 2. Configuration change (xoay màn hình) → recreate

    // Phân biệt 2 trường hợp:
    if (isFinishing) {
        // Trường hợp 1: hủy thực sự → cleanup toàn bộ
    } else {
        // Trường hợp 2: sắp recreate → không cần cleanup nhiều
    }

    // Dùng cho:
    // → Giải phóng resource chưa giải phóng
    // → Cancel background job
}
```

> _**Note**: **KHÔNG** được coi `onDestroy()` là nơi bảo đảm chắc chắn sẽ chạy để **lưu dữ liệu quan trọng**_

Nếu **Android cần giải phóng RAM**, hệ thống có thể **kill toàn bộ process chứa Activity**. Theo tài liệu chính thức, hệ thống _không trực tiếp kill một Activity để lấy RAM_; nó **kill process chứa Activity** và những **thành phần đang chạy bên trong process đó**
