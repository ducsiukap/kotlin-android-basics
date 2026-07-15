# `IntentService` & `WorkManager`

## 1. `IntentService`

`IntentService` là **subclass** của `Service` — ra đời để **giải quyết vấn đề threading** mà Started Service không tự xử lý.

- **Started Service**: `onStartCommand()` chạy trên **main thread** → phải tự tạo `Thread`/`Coroutine`
- **`IntentService`**:
  - Tự tạo **worker thread riêng** để `onHandleIntent()` chạy
  - Xử lý `Intent` tuần tự
  - Tự gọi `stopSelf()` sau khi xử lý xong

  → Không cần lo **threading**, không cần `stopSelf()`

```kotlin
// DEPRECATED từ API 30 — chỉ cần đọc hiểu
class DownloadIntentService : IntentService("DownloadIntentService") {

    companion object {
        const val EXTRA_URL = "extra_url"

        fun newIntent(context: Context, url: String): Intent {
            return Intent(context, DownloadIntentService::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
        }
    }

    // Chạy trên worker thread tự động — KHÔNG phải Main Thread
    override fun onHandleIntent(intent: Intent?) {
        val url = intent?.getStringExtra(EXTRA_URL) ?: return
        // Download file...
        // Xong → tự gọi stopSelf()
    }
}

// Start
startService(DownloadIntentService.newIntent(this, "https://..."))
```

**Vấn đề** của `IntentService`:

- **Deprecated** từ API 30 (Android 11).
- Chỉ **xử lý tuần tự** — không parallel.
- Không tích hợp Coroutine.
- Không đảm bảo hoàn thành nếu app bị kill.
- Không có retry mechanism.

**Thay thế** hiện đại:

- Tác vụ nền **ngắn**, **trong app** -> `Coroutine` + `Service`
- Tác vụ **cần đảm bảo hoàn thành** -> `WorkManager` (có retry, constraints, parallel, Coroutine support)

---

## 2. `WorkManager`

### 2.1. **What** is `WorkManager`?

`WorkManager` là Jetpack library để **lên lịch và thực thi tác vụ nền được đảm bảo (guaranteed)** — tác vụ sẽ **hoàn thành** kể cả khi **app bị kill** hoặc **thiết bị restart**.

Điểm khác biệt cốt lõi với các cơ chế **background work** khác:

- **Coroutine** trong `ViewModel`: App bị kill → coroutine bị hủy → tác vụ mất
- **Service** + **Coroutine**:
  - Hệ thống **kill Service** → tác vụ mất
  - **START_STICKY** restart Service nhưng **không đảm bảo**
- **`WorkManager`**:
  - App killed -> **`WorkManager` tự restart** khi điều kiện phù hợp
  - Thiết bị restart -> **`WorkManager` tự chạy lại**
  - **ĐƯỢC ĐẢM BẢO hoàn thành** (deferred guaranteed execution)

### 2.2. `WorkManager` hoạt động như thế nào?

`WorkManager` **không tự implement background execution** — nó là **smart scheduler** chọn cơ chế phù hợp **tùy API level**:

- **API 23+**: `JobScheduler`
- **API < 23**: `AlarmManager` + `BroadcastReceiver`

`WorkManager` lưu trạng thái tác vụ vào **`Room` Database nội bộ** — đây là lý do nó **có thể restart** sau khi thiết bị reboot.

### 2.3. Khi nào dùng `WorkManager`?

```
DÙNG WorkManager:
├── Upload ảnh/file lên server — phải hoàn thành dù app bị kill
├── Sync database với server định kỳ
├── Compress/resize ảnh trước khi upload
├── Gửi log analytics khi có mạng
├── Backup data theo lịch
└── Bất kỳ tác vụ nào cần ĐẢMM BẢO hoàn thành
```

```
KHÔNG DÙNG WorkManager:
├── Tác vụ cần chạy NGAY LẬP TỨC → Coroutine
├── Tác vụ cần chạy LIÊN TỤC khi app mở → Service
├── Tác vụ cần chạy đúng giờ chính xác → AlarmManager
└── Push notification → FCM (Firebase Cloud Messaging)
```

### 2.4. Các khái niệm

| Khái niệm     | Mô tả                                                             |
| ------------- | ----------------------------------------------------------------- |
| `Worker`      | Class chứa code **tác vụ cần thực thi**                           |
| `WorkRequest` | Yêu cầu thực thi: `OneTimeWorkRequest` hoặc `PeriodicWorkRequest` |
| `Constraints` | Điều kiện để tác vụ chạy (có mạng, đang sạc...)                   |
| `WorkManager` | **Entry point** — enqueue và quản lý `WorkRequest`                |
| `WorkInfo`    | Thông tin **trạng thái tác vụ** (RUNNING, SUCCEEDED, FAILED...)   |

---

## 2. Dependency

```kotlin
// build.gradle.kts (Module: app)
dependencies {
    val workVersion = "2.9.0"

    // WorkManager với Coroutines (khuyến nghị)
    implementation("androidx.work:work-runtime-ktx:$workVersion")
}
```

---

## 3. Setup `Worker` - mô tả **task** cần thực thi: [Xem chi tiết](/setup_worker.md)

---

## 4. Setup `WorkRequest`: [Xem chi tiết](/setup_workrequest.md) - request thực thi task

---

## 5. `WorkManager`

Để lấy `WorkManager` instance:

```kotlin
val workManager = WorkManager.getInstance(context)
```

Để đẩy `WorkRequest` vào hàng đợi:

```kotlin
// OneTimeWorkRequest
workManager.enqueue(workRequest)

// PeriodicWorkRequest
workManager.enqueueUniquePeriodicWork(
    "sync_work",                          // tên unique
    ExistingPeriodicWorkPolicy.KEEP,      // nếu đã có → giữ cái cũ
    periodicWorkRequest
)
```

---

## 6. **Observe** trạng thái task: [Xem chi tiết](/observe_workinfo.md) - `WorkInfo`

---

## 7. **Work Chaining** - Chuỗi tác vụ: [Xem chi tiết](/work_chaining.md)

---

## 8. `setForeground()` — Tích hợp **Notification**

Với **tác vụ dài**, cần hiển thị `Notification` để hệ thống **không kill process**:

```kotlin
class LongUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Hiển thị như Foreground Service
        setForeground(createForegroundInfo("Đang upload..."))

        var progress = 0
        while (progress < 100) {
            // Upload từng chunk
            uploadChunk(progress)
            progress += 10

            // Cập nhật notification
            setForeground(createForegroundInfo("Đang upload... $progress%"))

            // Cập nhật progress để UI observe
            setProgress(workDataOf("progress" to progress))
        }

        return Result.success()
    }

    private fun createForegroundInfo(message: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Upload ảnh")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_upload)
            .setOngoing(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID     = "upload_channel"
        const val NOTIFICATION_ID = 1001
    }
}
```
