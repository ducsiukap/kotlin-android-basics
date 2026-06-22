# **Introduction** to **`BroadcastReceiver`**

## 1. **What** is `BroadcastReceiver`?

`BroadcastReceiver` là **một trong 4 Android Component** — đại diện cho component cho phép app **lắng nghe và phản hồi các sự kiện** (`broadcast`) được phát ra từ **hệ thống** hoặc từ **app khác**, thậm chí từ **chính app hiện tại**.

- `Activity`: UI
- `Service`: background task
- `BroadcastReceiver`: **lắng nghe sự kiện**
- `ContentProvider`: chia sẻ dữ liệu

### **Cơ chế `pub`-`sub`**

**Android** dùng **mô hình `pub`/`sub`** để truyền thông tin giữa các thành phần:

- **`PUBLISHER`**: **OS**/**app** phát ra `Intent` kèm theo một `action` (đại diện cho hành động cụ thể) và **dữ liệu** (nếu có).
- **`SUBSCRIBER`**: Các `BroadcastReceiver` đã đăng ký lắng nghe `action` đó sẽ được hệ thống đánh thức và nhận được `Intent` để thực hiện **xử lý**.

```
PUBLISHER                    ANDROID OS               SUBSCRIBER
(Ai đó phát broadcast)       (Message broker)         (App của bạn)

Hệ thống phát hiện           Tìm tất cả               BroadcastReceiver
pin yếu            →         Receiver đăng ký    →    onReceive() được gọi
                             lắng nghe event này
```

> _**Không có kết nối trực tiếp** giữa `publisher` và `subscriber` — Android OS đóng vai trò trung gian._

### Một số **đặc điểm quan trọng** của `BroadcastReceiver`

- `BroadcastReceiver` **không có UI**.
- **Nhận broadcast** kể cả khi **app không chạy** (_Static Receiver, cần khai báo trong `AndroidManifest.xml`_).
- `onReceive()` được gọi khi **nhận broadcast**:
  - `onReceive()` chạy trên **MAIN thread**.
  - `onReceive()` **phải hoàn thành nhanh** (thường `< 10s`), nếu không sẽ bị **ANR**.

  Do vậy:
  - Nếu cần thực hiện **heavy task**, cần **start Service** bên trong `onReceive()`.
  - Không dùng `Coroutine` trực tiếp trong `onReceive()` mà không `goAsync()`

---

## 2. **Phân loại `Broadcast`**

Khi một component thực hiện **phát sự kiện**, nó có thể chọn 1 trong 2 cơ chế:

### 2.1. **`Normal Broadcast` - phát KHÔNG tuần tự**

**Normal Broadcast** được phát ra **không theo thứ tự**, sự kiện sẽ được **gửi cùng lúc tới tất cả** các `BroadcastReceiver` đã đăng ký cùng lúc.

Nhờ vậy, đạt được **tốc độ xử lý nhanh**, nhưng các **Receiver** không thể truyền dữ liệu cho nhau và **không thể ngăn chặn** tín hiệu.

## 2.2. **`Ordered Broadcast` - phát tuần tự**

**Ordered Broadcast** phát ra sự kiên, tín hiệu được gửi tới từng `Receiver` theo một **thứ tự ưu tiên** - `android:priority` (mặc định là `0`, giá trị càng cao thì **ưu tiên càng cao**).

**Đặc điểm**: receiver nào nhận trước có thể:

- **Xử lý** dữ liệu, **ghi đè** dữ liệu vào `Intent` và chuyển tiếp.
- **Ngăn chặn tín hiệu** tiếp tục được gửi tới các receiver khác bằng cách gọi `abortBroadcast()`.

---

## 3. **Phân loại `BroadcastReceiver`**

Tương tự **Service**, Google ngày càng bóp `BroadcastReceiver` để **giảm rủi ro bảo mật** và **tiết kiệm pin**. Có 2 cách để **đăng ký** một `BroadcastReceiver`:

### 3.1. **Static Receiver** - đăng ký trong `AndroidManifest.xml`

**Static Receiver** là **`BroadcastReceiver` được khai báo trong `AndroidManifest.xml`**.

Đặc điểm:

- Do **OS quản lý hoàn toàn**. Khi app **không chạy**, hệ thống vẫn có thể **đánh thức app** để gọi `onReceive()`.
- `<receiver>` phải có `android:exported="true"` để hệ thống có thể **đánh thức** khi có broadcast.
- Hạn chế từ **Android 8.0 (`API 26`)**: Google cấm **Static Receiver** cho hầu hết các sự kiện thông thường như `ACTION_SCREEN_ON`, `ACTION_WIFI_SCAN_CHANGED`, ... chỉ nhận được **một số broadcast** cho một vài ngoại lệ như `ACTION_BOOT_COMPLETED`, ...
  > _**Mục đích**: tránh việc quá nhiều app cùng được wake up cùng lúc, gây **tốn pin** và **giảm hiệu năng**._

#### **Triển khai Static `Receiver`**:

#### **Bước 1**: đăng ký `<receiver>` trong `AndroidManifest.xml`

```xml
<!-- AndroidManifest.xml -->
<receiver
    android:name=".receiver.BootReceiver"
    android:exported="true">       <!-- exported=true: nhận broadcast từ hệ thống -->

    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>

</receiver>
```

#### **Bước 2**: định nghĩa `BroadcastReceiver`

Các **Receiver** được triển khai bằng cách **kế thừa `BroadcastReceiver`** và **override `onReceive()`**.

```kotlin
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Thiết bị vừa boot xong
            // Restart Service nếu cần
            val serviceIntent = Intent(context, MyService::class.java)
            context.startService(serviceIntent)
        }
    }
}
```

### 3.2. **Dynamic Receiver** - đăng ký trong **Code**/**Context-registered**

**Dynamic Receiver** là **`BroadcastReceiver` được đăng ký trong code** (thường trong `Activity` hoặc `Service`) bằng cách gọi `Context.registerReceiver()`.

**Đặc điểm**:

- **Lifecycle** của receiver **phụ thuộc hoàn toàn** vào **lifecycle của component** mà nó được đăng ký.
  - Khi **component bị destroy**, receiver cũng sẽ bị **unregister**.
  - Nếu đăng ký `receiver` trong `onStart()`, **bắt buộc** phải **unregister** trong `onStop()` để tránh **memory leak**.
- **Ưu điểm**: Nhận được tất cả các **broadcast** từ hệ thống, không bị giới hạn.

#### **Triển khai Dynamic `Receiver`**:

**Dynamic Receiver** không cần đăng ký trong `AndroidManifest.xml`, được triển khai bằng cách **kế thừa `BroadcastReceiver`** và **override `onReceive()`**, sau đó **đăng ký** trong code.

#### **Bước 1**: định nghĩa `BroadcastReceiver`

```kotlin

class NetworkChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val isConnected = isNetworkAvailable(context)
        // Cập nhật UI, notify user...
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        return cm.activeNetworkInfo?.isConnected == true
    }
}
```

#### **Bước 2**: đăng ký `BroadcastReceiver` trong `Activity`

```kotlin
class MainActivity : AppCompatActivity() {

    private lateinit var networkReceiver: NetworkChangeReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        networkReceiver = NetworkChangeReceiver()
    }

    override fun onStart() {
        super.onStart()
        // Đăng ký khi Activity visible
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(networkReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(networkReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        // HỦY đăng ký khi Activity không visible — tránh memory leak
        unregisterReceiver(networkReceiver)
    }
}
```

#### **Lifecycle của Dynamic Receiver**

| Muốn nhận broadcast khi               | Register ở        | Unregister ở      |
| ------------------------------------- | ----------------- | ----------------- |
| Activity **visible**                  | `onStart()`       | `onStop()`        |
| Activity **tồn tại** (kể cả `paused`) | `onCreate()`      | `onDestroy()`     |
| Fragment View tồn tại                 | `onViewCreated()` | `onDestroyView()` |

---

## 4. **Ràng buộc kỹ thuật** của `onReceive()`

Hàm `onReceive()` chạy trực tiếp trên **Main thread** của `process`.<br/>
OS cấp cho `onReceive()` **một lượng thời gian giới hạn** để xử lý (thường là **`10s`**). Nếu **quá thời gian**, OS sẽ **kill process** và báo **ANR**. Do vậy:

- **Quy tắc**: `onReceive()` phải **hoàn thành nhanh**, không được thực hiện **heavy task** hay các tác vụ **blocking**.
- **Giải pháp**: nếu cần thực hiện **heavy task**, cần **start `Foreground` Service** để đẩy lệnh vào giải quyết hoặc **kích hoạt `WorkManager`**.

---

## 5. **Note**

### 5.1. App có thể **có nhiều `BroadcastReceiver`** không?

Application có thể **có nhiều `BroadcastReceiver`**. Mỗi receiver sẽ **lắng nghe một hoặc nhiều `action`** khác nhau.

Trong trường hợp có **nhiều receiver lắng nghe cùng một `action`**, OS sẽ gọi **TẤT CẢ các receiver** theo:

- **Thứ tự ưu tiên** (nếu là `Ordered Broadcast`), tuân theo `priority`, receiver trước có thể **modify**/**abort** tín hiệu.
- **Cùng lúc** (nếu là `Normal Broadcast`), không đảm bảo thứ tự.

### 5.2. **Dynamic Receiver** có cần đăng ký trong `AndroidManifest.xml` không?

**Dynamic Receiver KHÔNG cần** đăng ký trong `AndroidManifest.xml`. Nó được **đăng ký trực tiếp trong code** bằng cách gọi `Context.registerReceiver()`.

- OS biết tới `receiver` từ lúc gọi **`registerReceiver()`**.
- Cho tới khi gọi **`unregisterReceiver()`**, OS sẽ **quên** receiver đó.
