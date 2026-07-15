# **Work Chaining**

## 1. Sequential chaining

```kotlin
// Compress → Upload → Notify
// Chạy lần lượt, output của tác vụ trước là input của tác vụ sau

val compressWork = OneTimeWorkRequestBuilder<CompressWorker>()
    .setInputData(workDataOf("uri" to imageUri))
    .build()

val uploadWork = OneTimeWorkRequestBuilder<UploadWorker>()
    .build()   // nhận output từ CompressWorker tự động

val notifyWork = OneTimeWorkRequestBuilder<NotifyWorker>()
    .build()

WorkManager.getInstance(context)
    .beginWith(compressWork) // task 1
    .then(uploadWork)        // task 2, nhận output từ task 1 TỰ ĐỘNG
    .then(notifyWork)        // task 3
    .enqueue()
// chạy tuần tự
```

---

## 2. Parallel chaining

```kotlin
// Compress nhiều ảnh song song, sau đó upload tất cả

val compress1 = OneTimeWorkRequestBuilder<CompressWorker>()
    .setInputData(workDataOf("uri" to uri1))
    .build()

val compress2 = OneTimeWorkRequestBuilder<CompressWorker>()
    .setInputData(workDataOf("uri" to uri2))
    .build()

val compress3 = OneTimeWorkRequestBuilder<CompressWorker>()
    .setInputData(workDataOf("uri" to uri3))
    .build()

val uploadAll = OneTimeWorkRequestBuilder<UploadAllWorker>()
    .build()

WorkManager.getInstance(context)
    .beginWith(listOf(compress1, compress2, compress3))  // song song
    .then(uploadAll)                                      // sau khi tất cả xong
    .enqueue()
```

truyền vào `beginWith()` / `then()` có thể là 1 `WorkRequest` (để chạy **tuần tự**) hoặc 1 `List<WorkRequest>` (để chạy **song song**).

Mỗi bước `.then().then()...` là **một bước tuần tự**, và nhận **output của bước trước** làm **input của bước sau**.

---

## 3. **Unique work** - tránh trùng

```kotlin
// Đảm bảo chỉ có 1 instance của tác vụ này tại một thời điểm
WorkManager.getInstance(context)
    .enqueueUniqueWork(
        "upload_photos",                    // tên unique
        ExistingWorkPolicy.KEEP,            // nếu đã có → giữ, bỏ qua request mới
        uploadRequest
    )

// ExistingWorkPolicy:
// KEEP    → giữ work đang chạy
// REPLACE → hủy work cũ, thay bằng mới
// APPEND  → thêm vào sau work đang chạy
// APPEND_OR_REPLACE → APPEND nếu đang chạy, REPLACE nếu đang FAILED/CANCELLED
```
