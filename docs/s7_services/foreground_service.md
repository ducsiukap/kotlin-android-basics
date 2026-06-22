# **_Foreground_ Service**

## 1. **What** is a **Foreground Service**

### 1.1. Cơ chế của **Foreground Service**

**Started Service** và background task có vấn đề: khi **app ở background** và **OS thiếu tài nguyên**, Android có thể **KILL Started Service bất cứ** lúc nào.

**Foreground Service** giải quyết bằng cách báo với **Android OS**:

> _"`Service` hiện tại đang làm việc mà **user biết và quan tâm**"_

nhờ cơ chế **Notification cố định trên `Status Bar`** — khi nào Notification còn hiện, Android coi Service là foreground và ưu tiên giữ sống.

So với **`Started` Service**, **`Foreground` Service** không thay thế. Nó là **Started Service được bổ sung thêm 2 thứ**:

- `Notification`: bắt buộc, không thể dismiss
- `startForeground(id, notification)`.

> _**Foreground Service** = **Started Service** + `Notification` + `startForeground(id, notification)`_

Toàn bộ **lifecycle** của **Foreground Service** vẫn tuân theo quy tắc của **Started Service**: `onStartCommand()`, `stopSelf()`, return value, ...

### 1.2. Yêu cầu với **Foreground Service**

- Từ **Android 8.0 (`API 26`)** trở đi, **Google** áp dụng cơ chế **Background Execution Limits** để bảo vệ pin và dung lượng RAM của thiết bị.

  > _Khi **app** bị đẩy xuống **Background**, hệ thống sẽ **kill tất cả các Background Service** trong vòng vài phút_.

  Để giữ cho một **tác vụ chạy liên tục** không ngắt quãng (_như stream nhạc, định vị GPS, đồng bộ file lớn_), ứng dụng **phải sử dụng Foreground Service** để **thông báo tường minh** với Android OS và Low Memory Killer (**`LMK`**)

  ***

- Từ **Android 10 (`API 29`)** trở lên, phải khai báo rõ ràng `foregroundType` tương ứng nếu muốn dùng **Foreground Service** trong `AndroidManifest.xml`. Các `foregroundType` phổ biến:
  - `mediaPlayback`: Phát nhạc/video ngầm.
  - `location`: Định vị, theo dõi vị trí.
  - `dataSync`: Download/Upload file, sync dữ liệu ngầm.
  - `connectedDevice`: kết nối & truyền data với thiết bị ngoại vi (bluetooth, đồng hồ thông minh, ...)

  ***

- Từ **Android 13 (`API 33`)**, phải xin **runtime permission** `POST_NOTIFICATION` để hiện notification của foreground service. <br/>
  Ngoài ra, mỗi một `foregroundServiceType` sẽ **đi kèm** với một quyền (`uses-permission`) tương ứng trong **Manifest**, và **một số loại** (như location) **còn yêu cầu xin quyền Runtime** từ người dùng trước khi gọi chạy Service.

  ***

- Từ **Android 14 (`API 34`)**, **Google** áp dụng thêm cơ chế **Foreground Service Task Manager** để quản lý tốt hơn các Foreground Service đang chạy, yêu cầu `foregroundServiceType` phải được khai báo rõ ràng, và **PHẢI match** với loại tác vụ thực tế.

---

## 2. **Triển khai** Foreground Service

### **Bước 1**: xin quyền `uses-permission` và khai báo `service` trong `AndroidManifest.xml`:

- Xin **permission**:

  ```xml
  <!-- Bắt buộc cho mọi Foreground Service -->
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

  <!-- Bắt buộc API 33+ để hiện Notification -->
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

  <!-- Bắt buộc API 34+ — phải match foregroundServiceType -->
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
  ```

- Khai báo `<service>`:

  ```xml
  <service
      android:name=".DownloadForegroundService"
      android:exported="false"
      android:foregroundServiceType="dataSync" />
  <!--
      foregroundServiceType phổ biến:
      dataSync        → download, upload, sync
      mediaPlayback   → music, video player
      location        → GPS tracking
      camera          → background camera
  -->
  ```

### **Bước 2**: tạo **`NotificationChannel`**

`API 26+` **bắt buộc** phải tạo `NotificationChannel` trước khi build Notification. **Không có channel → Notification không hiện**.

```kotlin
object NotificationHelper {

    const val CHANNEL_ID = "download_channel"
    const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        // NotificationChannel chỉ có từ API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Download Service",         // Tên hiện cho user trong Settings
                NotificationManager.IMPORTANCE_LOW  // Không phát âm thanh
            ).apply {
                description = "Hiển thị tiến trình download"
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(context: Context, progress: Int): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Đang download")
            .setContentText("Tiến trình: $progress%")
            .setSmallIcon(R.drawable.ic_download)
            .setProgress(100, progress, false)  // max, current, indeterminate
            .setOngoing(true)      // User không thể swipe dismiss
            .build()
    }
}
```

### **Bước 3**: **implement Service**

```kotlin
class DownloadForegroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val url = intent?.getStringExtra("url") ?: run {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        // Bước đầu tiên — promote lên Foreground NGAY LẬP TỨC
        // Không được delay, phải gọi trước khi làm bất cứ điều gì khác
        val notification = NotificationHelper.buildNotification(this, 0)
        startForeground(NotificationHelper.NOTIFICATION_ID, notification)

        scope.launch {
            downloadFile(url, startId)
        }

        return START_NOT_STICKY
    }

    private suspend fun downloadFile(url: String, startId: Int) {
        try {
            for (progress in 0..100 step 10) {
                delay(500) // Giả lập download

                // Update notification với progress mới
                val updatedNotification = NotificationHelper.buildNotification(
                    this@DownloadForegroundService,
                    progress
                )
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NotificationHelper.NOTIFICATION_ID, updatedNotification)
            }

            Log.d("ForegroundService", "Download hoàn thành: $url")

        } finally {
            // finally đảm bảo luôn chạy kể cả khi có exception
            stopSelf(startId)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
```

### **Bước 4**: Start từ `Activity`

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // API 33+ phải xin permission POST_NOTIFICATIONS trước
        requestNotificationPermissionIfNeeded()

        btnDownload.setOnClickListener {
            startForegroundService()
        }
    }

    private fun startForegroundService() {
        val intent = Intent(this, DownloadForegroundService::class.java).apply {
            putExtra("url", "https://example.com/file.zip")
        }

        // API 26+: PHẢI dùng startForegroundService
        // thay vì startService
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATION
                )
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_NOTIFICATION = 100
    }
}
```

---

## 3. `stopForeground` — Hạ cấp xuống **Started Service**

Khi tác vụ xong, đôi khi bạn **không muốn dừng Service hẳn** mà **chỉ hạ cấp** — bỏ Notification nhưng Service vẫn sống:

- `stopSelf()` / `stopService()`: **hủy bỏ hoàn toàn** Service đó.
- `stopForeground()`: thực hiện việc hạ cấp `Service` từ **Foreground** xuống thành **Background Service** thông thường

```kotlin
// API 33+
stopForeground(STOP_FOREGROUND_REMOVE)  // Xóa notification
stopForeground(STOP_FOREGROUND_DETACH)  // Giữ notification nhưng có thể dismiss

// API cũ hơn
@Suppress("DEPRECATION")
stopForeground(true)    // true = xóa notification
                        // false = STOP_FOREGROUND_DETACH
```

> _Sau `stopForeground()`, **Service** trở thành **Started Service** bình thường — Android có thể kill nó khi thiếu RAM._
