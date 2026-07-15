# Setup `WorkRequest` - mô tả **task** cần thực thi

> `Worker` là code chứa **task** cần thực thi

## 1. **Các loại `Worker`**

| Phân loại          | Khi nào dùng                                                  |
| ------------------ | ------------------------------------------------------------- |
| `Worker`           | **Synchronous**, blocking                                     |
| `CoroutineWorker`  | **Asynchronous**, `suspend fun`, non-blocking, dùng Coroutine |
| `RxWorker`         | Dùng **RxJava** (ít dùng)                                     |
| `ListenableWorker` | Base class, custom async                                      |

Thực tế **luôn dùng `CoroutineWorker`**.

---

## 2. `CoroutineWorker` cơ bản

```kotlin
class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Chạy trên Dispatchers.Default (background thread) tự động
        // KHÔNG cần withContext(Dispatchers.IO) — WorkManager lo

        return try {
            // Lấy input data
            val imageUri = inputData.getString(KEY_IMAGE_URI)
                ?: return Result.failure()

            // Thực thi tác vụ
            uploadImage(imageUri)

            // Thành công
            Result.success()

        } catch (e: Exception) {
            // Thất bại — retry
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun uploadImage(uri: String) {
        // Logic upload...
    }

    companion object {
        const val KEY_IMAGE_URI = "key_image_uri"
    }
}
```

**`Result` có 3 trạng thái**:

- `Result.success()` → tác vụ hoàn thành **thành công**, `WorkManager` sẽ **xóa task** khỏi queue.
- `Result.failure()` → tác vụ **thất bại**, `WorkManager` sẽ đánh dấu **FAILED**
- `Result.retry()` → tác vụ **thất bại**, yêu cầu retry sau -> `WOrkManager` sẽ **tự retry** sau một khoảng thời gian (theo **backoff policy**).

---

## 3. **Input & Output** Data

`Worker` có thể nhận **input data** và trả về **output data**:

```kotlin
class ProcessImageWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Nhận input
        // inputData tự có sẵn — lấy từ WorkerParameters
        val imageUri  = inputData.getString(KEY_INPUT_URI)
            ?: return Result.failure()
        val quality   = inputData.getInt(KEY_QUALITY, 80)
        val maxWidth  = inputData.getInt(KEY_MAX_WIDTH, 1920)

        return try {
            val outputPath = processImage(imageUri, quality, maxWidth)

            // Trả về output
            val outputData = workDataOf(
                KEY_OUTPUT_PATH to outputPath
            )
            Result.success(outputData)

        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val KEY_INPUT_URI   = "key_input_uri"
        const val KEY_QUALITY     = "key_quality"
        const val KEY_MAX_WIDTH   = "key_max_width"
        const val KEY_OUTPUT_PATH = "key_output_path"
    }
}
```

`inputData` là **property có sẵn** trong class `Worker`/`CoroutineWorker` (_kế thừa từ `ListenableWorker`_), <br>
truyền vào qua `setInputData()` khi build `WorkRequest`:

```kotlin
// Tạo data
val inputData = workDataOf(
    "image_uri" to "content://media/images/42",
    "quality"   to 80
)

// Gắn vào WorkRequest
val request = OneTimeWorkRequestBuilder<UploadWorker>()
    .setInputData(inputData)   // ← gắn ở đây
    .build()
```

Đọc `outputData` trả về từ `Result.success(outputData)` bằng cách **observe `WorkInfo`**.
