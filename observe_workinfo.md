# `WorkInfo` - observe **trạng thái** task

## 5.1. **`WorkInfo` States**

`WorkInfo` là **class** chứa thông tin trạng thái của task đang chạy. Nó có **5 trạng thái**:

- `ENQUEUED`: task **đã được enqueue**, và đang **chờ chạy**
- `RUNNING`: task **đang chạy**
- `SUCCEEDED`: task **đã chạy xong** và **THÀNH CÔNG**
- `FAILED`: task **đã chạy xong** nhưng **THẤT BẠI**
- `CANCELLED`: task **bị huỷ** trước khi chạy xong
- `BLOCKED`: task **bị block** bởi một task khác hoặc chờ pre-requisite task (trong `WorkChain`) chạy xong.

## 5.2. Lấy `WorkInfo` từ `WorkRequest`

```kotlin
val workManager = WorkManager.getInstance(context)

// Theo ID
workManager.getWorkInfoByIdFlow(workId)              // Flow<WorkInfo?>

// Theo tag
workManager.getWorkInfosByTagFlow("upload")          // Flow<List<WorkInfo>>

// Theo unique work name
workManager.getWorkInfosForUniqueWorkFlow("sync")    // Flow<List<WorkInfo>>
```

sử dụng hậu tố `Flow` hoặc `LiveData` để yêu cầu trả về dạng **`Flow<WorkInfo?>`** hoặc **`LiveData<WorkInfo?>`**, ví dụ:

```kotlin
workManager.getWorkInfoByIdLiveData(workId) // LiveData<WorkInfo?>

workManager.getWorkInfoByIdFlow(workId)     // Flow<WorkInfo?>
```

## 5.3. **Observe** trạng thái task bằng `LiveData`

Khởi chạy task bằng `WorkRequest` và **lưu ID** của nó để observe trạng thái task:

```kotlin
val workRequest = OneTimeWorkRequestBuilder<UploadWorker>()
    .setInputData(inputData)
    .build()

// Lưu ID để observe
val workId = workRequest.id

WorkManager.getInstance(context).enqueue(workRequest)
```

**Observe** trạng thái task bằng `LiveData`:

```kotlin
// Observe trong Fragment/Activity
WorkManager.getInstance(requireContext())
    .getWorkInfoByIdLiveData(workId) // workId đã lưu bên trên
    .observe(viewLifecycleOwner) { workInfo ->
        if (workInfo == null) return@observe

        when (workInfo.state) {
            WorkInfo.State.ENQUEUED  -> showMessage("Đang chờ...")
            WorkInfo.State.RUNNING   -> showLoading()
            WorkInfo.State.SUCCEEDED -> { // success có thể có output data
                hideLoading()
                // Đọc output data
                val outputPath = workInfo.outputData.getString(
                    ProcessImageWorker.KEY_OUTPUT_PATH
                )
                showSuccess(outputPath)
            }
            WorkInfo.State.FAILED    -> showError("Upload thất bại")
            WorkInfo.State.CANCELLED -> showMessage("Đã hủy")
            else -> {}
        }
    }
```

## 5.4. **Observe** trạng thái task bằng `Flow`

```kotlin
// Trong ViewModel
class UploadViewModel(
    private val context: Application
) : AndroidViewModel(application = context) {

    fun observeUpload(workId: UUID): StateFlow<WorkInfo?> {
        return WorkManager.getInstance(context)
            .getWorkInfoByIdFlow(workId) // workId đã lưu bên trên
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    }
}
```

### 5.4. Cancel task đang chạy

```kotlin
val workManager = WorkManager.getInstance(context)

// Cancel theo ID
workManager.cancelWorkById(workId)

// Cancel theo tag
workManager.cancelAllWorkByTag("upload")

// Cancel tất cả
workManager.cancelAllWork()
```
