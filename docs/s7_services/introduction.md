# **`Service` in _Android_** - introduction

## 1. **What** is a `Service`?

`Service` là một trong **4 core components** của Android.

- `Service` là một component **đại diện cho các tác vụ nền - `background tasks`**.
- Không có **giao diện người dùng - `ui`**.

> _`Service` có thể tiếp tục chạy ngay cả khi user thoát app, tắt màn, ..._

---

## 2. **Why** do we need `Service`?

**Android** có quy tắc **không thực hiện `heavy tasks` trên `main thread` (UI)**.

> _Nếu **Main Thread bị block** bởi một tác vụ nặng (quá `5s`), app sẽ bị **ANR - Application Not Responding**._

`Thread`/`Coroutine` có thể được sử dụng trong `Activity` nhưng là **không đủ** — vì **khi Activity bị destroy**, mọi thứ gắn với nó (Coroutine scope, callback...) cũng **bị hủy**. <br/>
Do vậy, cần một component **tồn tại độc lập, không phụ thuộc** với `Activity` — đó là `Service`.

---

## 3. **Where** does `Service` run?

`Service` mặc định chạy trên **Main (`UI`) Thread**, không phải **background thread**

```kotlin
class MyService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Đây đang chạy trên MAIN THREAD
        // Nếu làm việc nặng ở đây → block UI → ANR
        heavyWork()   // SAI — block main thread

        return START_STICKY
    }
}
```

**Hệ quả**: `Service` chỉ giải quyết vấn đề **lifetime** (**tồn tại độc lập** với `Activity`) — **không** tự động giải quyết vấn đề **threading**. <br/>
Bạn vẫn phải tự tạo **background thread** bên trong `Service`:

```kotlin
class MyService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            // Giờ mới đang chạy trên background thread
            heavyWork()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()   // cancel tất cả coroutine khi Service bị hủy
    }
}
```

---

## 4. **Types** of `Service`

### 4.1. Phân loại theo **Cơ chế vận hành** (`lifecycle` - _khởi động, giao tiếp, ..._)

Nếu chia `Service` theo cách nó **hoạt động**, **giao tiếp**, cách nó được hệ thống **quản lý** trong bộ nhớ, thì **`Service` chỉ có `2` loại**:

- **Started Service**: `Service` được **start** bằng `startService()` hoặc `startForegroundService()`.
  - Khi được start, `Service` sẽ **chạy độc lập**, không phụ thuộc vào component nào.
  - Khi rời app, **`Service` vẫn tiếp tục chạy** cho đến khi **tự stop** bằng `stopSelf()` hoặc bị app tắt `stopService()`.

  _Ví dụ: **Music Player**, **Download Manager**, ..._

- **Bound Service**: `Service` được **bind** bằng `bindService()`.
  - Hoạt động theo cơ chế **client-server**: Các `binder` **bind** vào **service** để **tương tác**, **gửi request** (_gọi `method`_) và **nhận response** từ service thông qua giao diện `IBinder`. (_giao tiếp 2 chiều_)
  - Khi được bind, `Service` sẽ **chạy phụ thuộc vào component** (ví dụ: `Activity`, `Fragment`, ...).
  - Khi component bị destroy, `Service` cũng sẽ **tự hủy**.

  _Ví dụ: **Music Player**, **Location Tracker**, ..._

### 4.2. Phân loại theo **độ ưu tiên với hệ thống** (`priority` - _mức độ hiển thị_)

Đây là nhánh phân loại `Service` theo **mức độ ưu tiên với hệ thống**, đồng thời là yêu cầu **hiển thị Notification** khi service chạy.

> _**Mục đích**: để đối phó với việc các **app chạy ngầm** làm **tốn pin** và **RAM**, Google mới **chia Service theo mức độ hiển thị** để kiểm soát_

- **Foreground Service**: `Service` chạy **background task** nhưng **BẮT BUỘC** phải hiện thông báo cho user.
  - **Foreground Service** được hệ thống coi là service với **độ ưu tiên cao**, điều này giúp nó **hiếm khi bị kill** khi OS cần giải phóng memory.
  - Phải gọi `startForeground()` trong vòng **vài giây** kể từ khi service được start, nếu không sẽ bị **ANR**.
  - **BẮT BUỘC** phải hiển thị **Notification** để user biết service đang chạy.
- **Background Service**: `Service` chạy **background task** nhưng **`KHÔNG` hiển thị thông báo** cho user.
  - Chạy ngầm **hoàn toàn**, không có thông báo rõ ràng.
  - Từ **Android `8.0` (API `26`)** trở đi, **Background Service** bị giới hạn nghiêm ngặt → **không thể chạy ngầm** nếu app đang ở **background**.
    > _Bị OS coi là **ưu tiên thấp**, hệ thống có thể **kill bất cứ lúc nào** nếu app đang không ở **foreground**._
  - Hiện tại để làm tác vụ ngầm, người ta ưu tiên dùng **`WorkManager`** thay vì Background Service.

### 4.3. Kết hợp các **phân loại** trên theo từng **mục đích** và **behavior** khác nhau

| **Lifecycle** / **Priority** | **Foreground Service**                                                                                | **Background Service**                                                                             |
| ---------------------------- | ----------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| **Started Service**          | **Sync** nhanh, không cần Notification, **chạy ngắn**<br/>(_hiếm dùng do bị giới hạn nặng từ API 26_) | **Music Player**, **GPS Tracking**, **Large download**, ...<br/>_PHỔ BIẾN NHẤT trong thực tế_      |
| **Bound Service**            | `Activity` bind để **query data tạm thời**, không cần chạy khi Activity đóng                          | **Ít gặp**, thường dùng **Bound Service** không cần **Foreground** vì chỉ sống khi có client bind. |

```
Bạn KHỞI ĐỘNG Service như thế nào?
│
├── startService()/startForegroundService()  → STARTED SERVICE
│   │
│   └── Có gọi startForeground() + Notification không?
│       ├── Có  → FOREGROUND SERVICE
│       └── Không → BACKGROUND SERVICE (bị giới hạn nặng từ API 26+)
│
└── bindService()                             → BOUND SERVICE
    │
    └── (Bound Service CŨNG có thể chạy như Foreground
         nếu cần, nhưng ít gặp trong thực tế)
```

---

## 5. **Thread** vs **Service**

### 5.1. **`Thread`**

**Thread** là **đơn vị thực thi mã nguồn nhỏ nhất** (smallest unit of execution) được quản lý và phân phối thời gian xử lý (CPU scheduling) bởi hệ điều hành.

- Nguyên lý hoạt động: Mỗi `process` khi chạy, sẽ có:
  - **Main/UI Thread**: luồng mặc định, chịu trách nhiệm xử lý UI, event listener, ....<br/>
    Về nguyên tắc, **KHÔNG** được thực hiện các **heavy tasks** gây block main thread, tránh `ANR`.
  - **Background Thread**: là luồng phụ. Mỗi khi khởi tạo một `Thread` mới, bản chất là yêu cầu OS cấp pháp thêm một **luồng độc lập**, chạy song song với Main thread, **chia sẻ memory space** của process
- Vòng đời: **phụ thuộc process sở hữu** nó và cách được quản lý (start, stop, ...), **không có cơ chế vòng đời** từ Android OS

  ```kotlin
  class MainActivity : AppCompatActivity() {
      override fun onCreate(...) {
          Thread {
              streamMusic() // Chạy nền, ổn
          }.start()
      }
  }
  ```

  > _Vấn đề chính với **`Thread`** là nó **không có vòng đời độc lập** với component tạo ra nó. Khi host killed, không có cơ chế nào đảm bảo thread còn được giữ lại._

### 5.2. **`Service`**

**Service** là một **Application Component** được đăng ký trong `AndroidManifest.xml`, được quản lý trực tiếp bởi **`AMS` - ActivityManagerService** của Android OS.

- Nguyên lý: **`Service` không tạo ra process riêng biệt** (_trừ khi cấu hình `android:process`_), đồng thời cũng không phải luồng độc lập.
  > _Khi `Service` được khởi chạy thông qua **AMS**, các hàm `callback` trong vòng đời của nó (`onCreate`, `onStartCommand`,...) được chạy trực tiếp trên **MAIN thread** của process ứng dụng._
- Vòng đời: được định nghĩa chặt chẽ bởi **hệ thống** qua các trạng thái `Created`, `Started`, `Bound`, `Destroyed`....
  > _`Service` không phụ thuộc **Component chứa nó**, nó có **lifecycle độc lập**, và cả 2 **phụ thuộc song song** vào tiến trình ứng dụng - **application process** dưới sự điều khiển của Android OS_

```kotlin
class MusicService : Service() {
    override fun onStartCommand(...): Int {
        streamMusic() // ❌ Block Main Thread → ANR
        return START_STICKY
    }
}
```

> _**Vấn đề** chính của `Service` là **nó không tự làm tác vụ nặng được**. Nó chỉ đảm bảo component tồn tại._

### 5.3. **Multi-threading architecture pattern**: mô hình phối hợp đa luồng

> _Thực tế, **Thread/`Coroutine`** và **`Service`** không loại trừ lẫn nhau mà **bổ sung cho nhau**._

Để xử lý các tác vụ **I/O blocking** (_Network call, Database query, File I/O_) hoặc **CPU-bound tasks** (_Image processing, Cryptography_) bên trong `Service` mà **không gây block UI Thread** (dẫn đến hiện tượng ANR), cấu trúc chuẩn bắt buộc phải tách biệt vai trò:

- **`Service`**: chịu trách nhiệm về **đảm bảo lifecycle**, bảo vệ process trước cơ chế dọn dẹp của OS, và **giao tiếp** với các component khác thông qua các **`Intent` điều hướng**.
- **Worker Execution Pool - `Thread`/`Coroutine`**: chịu trách nhiệm về **thực thi các tác vụ nặng**, đảm bảo **không block Main Thread** và **tối ưu hiệu suất** trên các luồng xử lý luân phiên - Worker Thread.

```kotlin
class MusicService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(...): Int {
        scope.launch {
            streamMusic() // ✅ Background thread,
                          // vòng đời do Service quản lý
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
    }
}
```

```
[Android OS (AMS)]
       │
  (Start / Bind Command)
       ▼
┌─────────────────────────────────────────┐
│              YOUR APP PROCESS           │
│                                         │
│   Main Thread (UI Thread)               │
│   ┌─────────────────────────────────┐   │
│   │  Service Lifecycle Callbacks    │   │
│   │  (onCreate, onStartCommand)     │   │
│   └────────────────┬────────────────┘   │
│                    │ (Dispatch Task)    │
│                    ▼                    │
│   Worker Threads (Background)           │
│   ┌─────────────────────────────────┐   │
│   │  Coroutine Scope / Thread Pool  │   │
│   │  [Execute Heavy I/O or CPU]     │   │
│   └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

### 5.4. Khi nào chỉ cần `Thread`/`Coroutine`, **KHÔNG** cần `Service`?:

Khi **tác vụ gắn liền với UI** — user phải đang dùng app thì tác vụ mới có ý nghĩa:

- Fetch API để hiển thị danh sách → dùng `ViewModel` + `Coroutine`, không cần Service
- Xử lý ảnh sau khi user chọn → Thread/`Coroutine` trong `Activity`/`Fragment`
- Download file mà user cần thấy progress ngay trên màn hình → `Coroutine` trong `ViewModel`

---

## 6. **Đăng ký** `Service` trong `AndroidManifest.xml`

Vì là **Application Component**, `Service` phải được **đăng ký trong `AndroidManifest.xml`** để hệ thống biết về nó.

```xml
<application ...>

    <!-- Started / Foreground Service -->
    <service
        android:name=".MusicService"
        android:exported="false" />
        <!-- exported=false: chỉ app của bạn mới start được Service này -->

    <!-- Service có thể được start từ app khác -->
    <service
        android:name=".PublicService"
        android:exported="true">
        <intent-filter>
            <action android:name="com.example.PUBLIC_ACTION" />
        </intent-filter>
    </service>

</application>
```

> Nếu không khai báo trong Manifest → `startService()` sẽ throw `ServiceNotFoundException` → crash.
