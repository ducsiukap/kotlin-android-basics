# **_Started_ Service**

## 1. **What** is `Started Service`?

**Started Service** là một nhánh **cực kỳ quan trọng** trong cơ chế vận hành của Android Service:

- **Started Service** là loại `Service` được **kích hoạt bằng `startService()` hoặc `startForegroundService()`**.
- **Fire & Forget**: sau khi được start, nó **chạy độc lập hoàn toàn** — không quan tâm component nào start nó có còn sống hay không. <br/>
  Nó **tiếp tục** thực thi tác vụ cho đến khi: `stopSelf()`/`stopSelfResult(int startId)` hoặc thành phần khác gọi `stopService()`.

---

## 2. **Lifecycle** của **Started Service**:

Vòng đời của **Started Service** rất đơn giản nhưng có những **điểm mấu chốt** sau:

```
startService()
      |
      ↓
  onCreate()        ← Chỉ gọi 1 lần duy nhất        // [START of lifecycle]
      ↓
  onStartCommand()  ← Gọi MỖI LẦN startService()
      |
      | (running)
      ↓
  stopSelf() / stopService()
      ↓
  onDestroy()       ← Dọn dẹp resource              // [END of lifecycle]
```

- **`onCreate()`** được gọi **`một lần` duy nhất bởi `hệ thống`** (_qua `ActivityManagerService`_) khi service được tạo ra.

  > Đây là nơi lý tưởng để **khởi tạo các tài nguyên dùng chung**, như _khởi tạo `CoroutineScope`, tạo **Notification Channel**, cấu hình **Database helper**_.

- **`onStartCommand(Intent? intent, int flags, int startId)`** được gọi **`mỗi lần`** service được **start** (_có thành phần gọi `startService()` hoặc `startForegroundService()`_). <br/>
  Đây là nơi **nhận dữ liệu (`Intent`)** và **xử lý các tác vụ** chính của service.

  ***

  Các **tham số đầu vào**:
  - `Intent? intent`: dữ liệu được truyền từ component gọi `startService()`.<br/>
    **Note**: Nếu Service **bị OS kill** do thiếu RAM và sau đó được **tái khởi động** (recreated), tham số `intent` này **có thể bị `null`** tùy thuộc vào giá trị return của hàm.
  - `int flags`: các cờ được hệ thống truyền vào, có thể dùng để **xác định cách service được restart**:
    - `START_FLAG_REDELIVERY`: Cho biết **`intent` hiện tại** là do hệ thống **gửi lại vì trước đó Service bị kill khi đang xử lý dở**.
    - `START_FLAG_RETRY`: Cho biết **`intent` hiện tại** là do hệ thống **gửi lại sau khi một lần thử `onStartCommand()` thất bại** đột ngột.
  - `int startId`: **ID duy nhất (`token`)** được hệ thống cấp, đại diện cho **mỗi request tới service** (cho mỗi lần gọi `startService()`). <br/>
    > _Mỗi lần chạy có `startId` khác nhau, nên bạn có thể **dừng service theo từng tác vụ** bằng `stopSelf(startId)`_.

  ***

  Cơ chế điều khiển hệ thống qua **giá trị trả về**:
  - `START_NOT_STICKY`: Nếu bị kill, hệ thống **KHÔNG tự động khởi động lại** Service, **trừ khi có một lệnh `startService()` mới** được gọi từ ứng dụng.. <br/>
    Thích hợp cho các tác vụ mang tính **xử lý 1 lần** như download file, sync data định kì, ... _nếu chết giữa chừng thì thôi, đợi người dùng trigger lại sau._
  - `START_STICKY`: Nếu bị kill, hệ thống **cố gắng khởi động lại** Service, nhưng **KHÔNG gửi lại `Intent` cũ** (restart với `intent=null`). <br/>
    Thích hợp cho các tác vụ **chạy ngầm vô hạn** và **không phụ thuộc dữ liệu đầu vào**, như **Music Player**, **Location Tracking**, **Download Manager**, ...
  - `START_REDELIVER_INTENT`: Nếu bị kill, hệ thống **cố gắng khởi động lại** Service và **gửi lại `Intent` cũ**. <br/>
    Thích hợp cho các tác vụ **quan trọng**, cần **bắt buộc hoàn thành** và **phụ thuộc chặt trẽ vào dữ liệu đầu vào**, như Upload large file, download file, Sync dữ liệu quan trọng, ...

- **`onDestroy()`** được gọi **`một lần` duy nhất bởi `hệ thống`** khi service bị dừng thông qua `stopService()` hoặc `stopSelf()`. <br/>
  Đây là nơi **giải phóng các tài nguyên** đã được khởi tạo trong `onCreate()`.

---

## 3. **Usage** của **Started Service**:

**Bước 1**: Khai báo Service trong `AndroidManifest.xml`:

```xml
<service
    android:name=".DownloadService"
    android:exported="false" />
```

**Bước 2**: Tạo `Service` class kế thừa từ `Service`:

```kotlin
class DownloadService : Service() {

    // Khởi tạo scope để chạy background task
    //   Download file -> I/O task
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Gọi 1 lần khi Service được tạo lần đầu
    override fun onCreate() {
        super.onCreate()
        Log.d("DownloadService", "onCreate")
    }

    // Gọi mỗi lần startService() được trigger
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val url = intent?.getStringExtra("url") ?: return START_NOT_STICKY

        scope.launch {
            downloadFile(url)   // Tác vụ nặng trên IO thread
            stopSelf(startId)   // Dừng đúng instance này
        }

        return START_NOT_STICKY
    }

    // Started Service bắt buộc override
    // nhưng return null
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel() // Bắt buộc — tránh coroutine leak
        Log.d("DownloadService", "onDestroy")
    }

    private suspend fun downloadFile(url: String) {
        // Giả lập download
        delay(3000)
        Log.d("DownloadService", "Downloaded: $url")
    }
}
```

**Bước 3**: Start Service từ `Activity` hoặc `Fragment`:

```kotlin
val intent = Intent(this, DownloadService::class.java).apply {
    putExtra("url", "https://example.com/file.zip")
}
startService(intent)
```

---

## 4. **Stopping** a service

### 4.1. Stop from **internal**: `stopSelf()` & `stopSelf(int startId)`:

- `stopSelf()`: Dừng service ngay lập tức, **bất kể có bao nhiêu task đang chạy**.
- `stopSelf(int startId)`: Dừng service **chỉ khi `startId` hiện tại trùng với `startId` của request đang chạy**. <br/>

  > _Điều này giúp **ngăn chặn việc dừng service khi vẫn còn các task khác đang chạy**, đặc biệt khi service được start nhiều lần._

  Kịch bản: **Request 1** (`startId = 1`) đang chạy mất 3 giây, giây thứ 2, có **Request 2** (`startId = 2`) bay vào.
  - Khi **Request 1** thực hiện xong, gọi `stopSelf(1)`, Android OS kiểm tra và thấy **hệ thống đã ghi nhận đến `startId = 2`**, lệnh **`stopSelf(1)` sẽ bị OS bỏ qua**. `Service` tiếp tục **sống để xử lý xong Request 2**.
  - Đến khi **Request 2** gọi `stopSelf(2)`, OS thấy đây là **`ID` mới nhất** (không còn request nào đang thực hiện), lúc này **Service mới thực sự bị hủy an toàn**.

```
startService() → startId = 1 → launch coroutine A
startService() → startId = 2 → launch coroutine B

Coroutine A xong → stopSelf(1)
  → Hệ thống thấy startId mới nhất là 2, không phải 1
  → KHÔNG dừng Service → Coroutine B vẫn tiếp tục

Coroutine B xong → stopSelf(2)
  → startId 2 là mới nhất → DỪNG Service ✅
```

> _**Luôn dùng `stopSelf(startId)` thay vì `stopSelf()`** khi Service có thể nhận nhiều request._

### 4.2. Stop from **external**: `stopService()`:

```kotlin
// Từ Activity dừng Service
val intent = Intent(this, DownloadService::class.java)
stopService(intent)
```

**`stopService()` trigger `onDestroy()` ngay lập tức** — kể cả khi coroutine bên trong chưa xong.

> _Đây là lý do `onDestroy()` phải gọi `scope.cancel()` để cleanup coroutine đang chạy._

---

## 5. **Passing data** into service

Service không có constructor parameter — data được truyền qua **`Intent` extras**:

- Truyền:

  ```kotlin
  // Gửi từ Activity
  val intent = Intent(this, DownloadService::class.java).apply {
      putExtra("url", "https://example.com/file.zip")
      putExtra("fileName", "file.zip")
      putExtra("priority", 1)
  }
  startService(intent)
  ```

- Nhận:

  ```kotlin
  // Nhận trong Service
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

      // intent có thể null nếu START_STICKY restart sau khi bị kill
      val url = intent?.getStringExtra("url") ?: run {
          stopSelf(startId)
          return START_NOT_STICKY
      }

      val fileName = intent.getStringExtra("fileName") ?: "unknown"
      val priority = intent.getIntExtra("priority", 0)

      // Xử lý...
      return START_NOT_STICKY
  }
  ```
