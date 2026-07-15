# Setup `WorkRequest` - request thực thi **task**

> `WorkRequest` là định nghĩa **một request** thực thi **task** (`Worker`)

## 1. `OneTimeWorkRequest`

```kotlin
// Tạo input data
val inputData = workDataOf(
    UploadWorker.KEY_IMAGE_URI to "content://media/images/42"
)

// Tạo WorkRequest
val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>()
    .setInputData(inputData)
    .build()

// Enqueue — đưa vào hàng đợi
WorkManager.getInstance(context).enqueue(uploadRequest)
```

Sử dụng `OneTimeWorkRequest` khi **task chỉ cần chạy 1 lần**, sử dụng `OneTimeWorkRequestBuilder<T>` để tạo `WorkRequest` cho **`worker` T**.

`OneTimeWorkRequest` được đưa vào `WorkManager` queue bằng `enqueue()`.

---

## 2. `PeriodicWorkRequest`

```kotlin
// Tối thiểu 15 phút — Android giới hạn để tiết kiệm pin
val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
    repeatInterval = 6, // là khoảng thời gian lặp lại
    repeatIntervalTimeUnit = TimeUnit.HOURS // đơn vị thời gian
)
    .build()

WorkManager.getInstance(context)
    .enqueueUniquePeriodicWork(
        "sync_work",                          // tên unique
        ExistingPeriodicWorkPolicy.KEEP,      // nếu đã có → giữ cái cũ
        syncRequest
    )
```

Sử dụng `PeriodicWorkRequest` khi **task cần chạy định kỳ**, sử dụng `PeriodicWorkRequestBuilder<T>` để tạo `WorkRequest` cho **`worker` T**.

`PeriodicWorkRequest` được đưa vào `WorkManager` queue bằng `enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy, workRequest)` để với:

- `name`: tên **unique** của task định kỳ
- `ExistingPeriodicWorkPolicy`: nếu đã có **task cùng tên** → **giữ cái cũ** hay **thay thế** bằng cái mới?

  | Policy                               | Mô tả                                          |
  | ------------------------------------ | ---------------------------------------------- |
  | `ExistingPeriodicWorkPolicy.KEEP`    | **Giữ task cũ**, bỏ qua task mới               |
  | `ExistingPeriodicWorkPolicy.REPLACE` | **Hủy task cũ**, thay bằng task mới            |
  | `ExistingPeriodicWorkPolicy.UPDATE`  | **Cập nhật task cũ** với thông tin từ task mới |

- `workRequest`: `PeriodicWorkRequest` cần enqueue

---

## 3. `Constraints` - **điều kiện** để task chạy

```kotlin
val constraints = Constraints.Builder() // Builder pattern
    .setRequiredNetworkType(NetworkType.CONNECTED)     // cần mạng
    .setRequiresCharging(true)                         // đang sạc
    .setRequiresBatteryNotLow(true)                    // pin không thấp
    .setRequiresStorageNotLow(true)                    // bộ nhớ không thấp
    .setRequiresDeviceIdle(true)                       // thiết bị idle (API 23+)
    .build()
```

`Constraints` là **điều kiện** để một **workRequest** được thực thi. <br>
Nếu **constraint** không thỏa → task sẽ **bị delay** cho đến khi **constraint** thỏa.

Đưa `Constraints` vào `WorkRequest` bằng `setConstraints(constraints)`:

```kotlin
val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>()
    .setConstraints(constraints)
    .setInputData(inputData)
    .build()
```

---

## 4. **Delay** & **Backoff** Policy

```kotlin
val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>()
    .setInputData(inputData)

    // Delay trước khi chạy
    .setInitialDelay(10, TimeUnit.MINUTES)

    // Backoff policy — khi Result.retry()
    .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,   // EXPONENTIAL hoặc LINEAR
        WorkRequest.MIN_BACKOFF_MILLIS,  // backoffDelay - thời gian chờ tối thiểu
        TimeUnit.MILLISECONDS
    )
    // EXPONENTIAL: 10s → 20s → 40s → 80s...
    // LINEAR:      10s → 20s → 30s → 40s...

    .build()
```

trong đó, `MIN_BACKOFF_MILLIS` là **thời gian chờ tối thiểu** được định nghĩa sẵn bên trong `WorkRequest`:

```kotlin
// Định nghĩa trong WorkManager library (bạn không viết cái này)
abstract class WorkRequest {
    companion object {
        const val MIN_BACKOFF_MILLIS = 10_000L    // 10 giây
        const val MAX_BACKOFF_MILLIS = 18_000_000L // 5 giờ
        const val DEFAULT_BACKOFF_DELAY_MILLIS = 30_000L // 30 giây
    }
}
```

`BackoffPolicy` là **chiến lược** để **delay** khi task **retry**:

- `BackoffPolicy.LINEAR`: **delay = `backoffDelay` \* `retryCount`**
- `BackoffPolicy.EXPONENTIAL`: **delay = `backoffDelay` \* 2^(`retryCount` - 1)**

trong đó, `backoffDelay` là **thời gian chờ tối thiểu**.

---

## 5. **Tags** — Nhóm và tìm `WorkRequest`

```kotlin
val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>()
    .addTag("upload")
    .addTag("media")
    .build()

// Cancel theo tag
WorkManager.getInstance(context).cancelAllWorkByTag("upload")

// Observe theo tag
WorkManager.getInstance(context)
    .getWorkInfosByTagLiveData("upload")
    .observe(lifecycleOwner) { workInfos ->
        // xử lý
    }
```

