# **Types** of Broadcasts: **System**, **Custom** & **Ordered**

## 1. **System** broadcasts

### 1.1 **Bản chất**

**System Broadcasts** là các `Intent`-events được publish bởi **Android OS** khi có **system events** xảy ra.

- **Nguồn phát**: Do các **service** ở tầng hệ thống như `BatteryService`, `ConnectivityManager`, `WifiService`... phát ra thông qua lệnh của **`AMS` - Activity Manager Service**.
- Các **Application** chỉ có thể lắng nghe, **không thể tạo ra chúng**.
- **Cơ chế nhận**: toàn bộ các **System Broadcasts quan trong** hiện tại đều **BẮT BUỘC** phải đăng ký receiver bằng **Dynamic Receiver**.

### 1.2. **Protected Broadcasts**

**Protected Broadcasts** là các broadcast chỉ **Android OS** mới có quyền publish, `sendBroadcast(Intent(Intent.ACTION_BOOT_COMPLETED))` từ **application** sẽ gây **`SecurityException`**.

> _Đây là **cơ chế bảo mật** — ngăn app giả mạo sự kiện hệ thống để trigger receiver của app khác._

**TOÀN BỘ System Broadcasts** đều là **Protected Broadcasts**.

### 1.3. Các **System Broadcasts quan trọng**

- **Intent.`ACTION_BOOT_COMPLETED`**: được publish khi **device boot** xong.
  > _Đây là ngoại lệ hiếm hoi cho phép **receiver** được đăng ký **Static Receiver** trong **`AndroidManifest.xml`**._
- **Intent.`ACTION_BATTERY_LOW` / Intent.`ACTION_BATTERY_CHANGED`**: được publish khi trạng thái **battery** thay đổi.
- **Intent.`ACTION_SCREEN_ON` / Intent.`ACTION_SCREEN_OFF`**: được publish khi **screen** bật/tắt.

### 1.3. Usecase 1 — **Network Change** Receiver

#### **Bước 1**: Định nghĩa object làm **network's state holder** cho **UI**:

```kotlin
// NetworkStateHolder — singleton đơn giản để share state
object NetworkStateHolder {
    val isConnected = MutableLiveData<Boolean>()
}
```

#### **Bước 2**: Định nghĩa **Receiver** class

```kotlin
class NetworkReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            val isConnected = isNetworkAvailable(context)
            // Notify UI
            NetworkStateHolder.isConnected.value = isConnected
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // API 23+ dùng NetworkCapabilities
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            // API cũ hơn
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }
}
```

#### **Bước 3**: Đăng ký **Dynamic Receiver** trong **`Activity`**:

```kotlin
// Đăng ký dynamic trong Activity
class MainActivity : AppCompatActivity() {

    private val networkReceiver = NetworkReceiver()

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)

        /**
         * ĐĂNG KÝ Receiver
         * - API 33+ cần thêm flag `RECEIVER_NOT_EXPORTED`
         *      để receiver không bị export ra ngoài
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(networkReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(networkReceiver, filter)
        }
    }

    // Hủy đăng ký receiver khi Activity bị stop
    override fun onStop() {
        super.onStop()
        unregisterReceiver(networkReceiver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Observe state thay đổi
        NetworkStateHolder.isConnected.observe(this) { isConnected ->
            bannerOffline.visibility = if (isConnected) View.GONE else View.VISIBLE
        }
    }
}
```

### 1.4. Usecase 2 — **Boot Receiver** : Static Receiver

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<receiver
    android:name=".BootReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <!-- Nhận cả khi máy reboot ở chế độ mã hóa (direct boot) -->
        <action android:name="android.intent.action.LOCKED_BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

```kotlin
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.LOCKED_BOOT_COMPLETED" -> {
                // Chỉ restart nếu user đã từng bật Service
                val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val wasRunning = prefs.getBoolean("service_was_running", false)

                if (wasRunning) {
                    val serviceIntent = Intent(context, MusicService::class.java)
                    context.startForegroundService(serviceIntent)
                }
            }
        }
    }
}
```

---

## 2. **Custom** Broadcasts

### 2.1. **Bản chất**

**Custom Broadcast** là broadcast do **app tự định nghĩa và phát** — dùng để giao tiếp giữa các **component trong app**, hoặc giữa **các app khác nhau**.

Dev tự quy định:

- `action` name để nhận biết broadcast.
- **Intent extras** (_nếu có_) để truyền dữ liệu đi kèm.
- Thực hiện lệnh phát `sendBroadcast()` để gửi broadcast đi.

### 2.2. `Action`'s naming convention:

Naming convention chuẩn cho `action` của **Custom Broadcast** là `package_name.ACTION_NAME`:

```kotlin
// Format chuẩn: package_name.ACTION_NAME
"com.example.myapp.DOWNLOAD_COMPLETE"
"com.example.myapp.SYNC_FAILED"
"com.example.myapp.USER_LOGGED_OUT"

// Không dùng tên ngắn chung chung — dễ conflict với app khác
"DONE"          // ❌
"SYNC"          // ❌
```

### 2.3. **Cơ chế** phát broadcast

#### 2.3.1. **Global custom broadcast**

**Global Custom Broadcast** là cơ chế phát broadcast cho **toàn bộ app khác** có đăng ký receiver với đúng `action` tương ứng.

```kotlin
sendBroadcast(intent)
```

- **Hành vi**: bất kì app nào có **đăng ký receiver với đúng `action`** sẽ nhận được broadcast.
- **Rủi ro**: dễ bị **tấn công rò rỉ dữ liệu** hoặc bị **giả mạo broadcast** từ app khác.

Từ **Android 14**, bắt buộc phải khai báo `RECEIVER_NOT_EXPORTED` khi đăng ký **dynamic receiver** để tránh bị app khác can thiệp.

#### 2.3.2. **Local custom broadcast**

**Local Custom Broadcast** là cơ chế phát broadcast **chỉ trong app** — không bị app khác nghe được.

- Google đã **deprecated** `LocalBroadcastManager` cũ kỹ từ **Android 11** vì không tối ưu.
- Kiến trúc hiện đại **khuyến khích thay thế** hoàn toàn việc phát broadcast nội bộ bằng **`SharedFlow`/`StateFlow`** trong **Coroutines** hoặc dùng **`EventBus`** ở tầng architecture để truyền dữ liệu bất đồng bộ giữa các component trong app.

### 2.4. Implementation — **`Service` thông báo về `Activity`**

**Bài toán**: `DownloadService` cần thông báo cho `Activity` biết **download đã hoàn tất** để update UI.

#### **Bước 1**: Định nghĩa **Custom Broadcast Action**:

```kotlin
// Định nghĩa constants tập trung — tránh hardcode string
object BroadcastConstants {
    const val ACTION_DOWNLOAD_COMPLETE = "com.example.myapp.DOWNLOAD_COMPLETE"
    const val ACTION_DOWNLOAD_FAILED   = "com.example.myapp.DOWNLOAD_FAILED"

    const val EXTRA_FILE_URL    = "extra_file_url"
    const val EXTRA_FILE_SIZE   = "extra_file_size"
    const val EXTRA_ERROR_MSG   = "extra_error_msg"
}
```

#### **Bước 2**: Trong `DownloadService`, phát **Custom Broadcast** khi download hoàn tất:

```kotlin
// Trong DownloadService — phát broadcast
class DownloadService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra("url") ?: return START_NOT_STICKY

        scope.launch {
            try {
                val fileSize = downloadFile(url)
                sendDownloadComplete(url, fileSize)   // Thành công
            } catch (e: Exception) {
                sendDownloadFailed(url, e.message ?: "Unknown error")  // Thất bại
            } finally {
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    private fun sendDownloadComplete(url: String, fileSize: Long) {
        val intent = Intent(BroadcastConstants.ACTION_DOWNLOAD_COMPLETE).apply {
            putExtra(BroadcastConstants.EXTRA_FILE_URL, url)
            putExtra(BroadcastConstants.EXTRA_FILE_SIZE, fileSize)
            // Giới hạn chỉ app của mình nhận
            `package` = packageName
        }
        sendBroadcast(intent)
    }

    private fun sendDownloadFailed(url: String, errorMsg: String) {
        val intent = Intent(BroadcastConstants.ACTION_DOWNLOAD_FAILED).apply {
            putExtra(BroadcastConstants.EXTRA_FILE_URL, url)
            putExtra(BroadcastConstants.EXTRA_ERROR_MSG, errorMsg)
            `package` = packageName
        }
        sendBroadcast(intent)
    }

    private suspend fun downloadFile(url: String): Long {
        delay(3000) // Giả lập
        return 1024L * 1024 * 50 // 50MB
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
```

#### **Bước 3**: nhận broadcast trong `Activity`:

```kotlin
// Trong MainActivity — nhận broadcast
class MainActivity : AppCompatActivity() {

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {

                BroadcastConstants.ACTION_DOWNLOAD_COMPLETE -> {
                    val url = intent.getStringExtra(BroadcastConstants.EXTRA_FILE_URL)
                    val size = intent.getLongExtra(BroadcastConstants.EXTRA_FILE_SIZE, 0)
                    tvStatus.text = "Download xong: ${size / 1024 / 1024}MB"
                }

                BroadcastConstants.ACTION_DOWNLOAD_FAILED -> {
                    val error = intent.getStringExtra(BroadcastConstants.EXTRA_ERROR_MSG)
                    tvStatus.text = "Lỗi: $error"
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(BroadcastConstants.ACTION_DOWNLOAD_COMPLETE)
            addAction(BroadcastConstants.ACTION_DOWNLOAD_FAILED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(downloadReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(downloadReceiver)
    }
}
```

### 2.5. **Security** cho **Custom Broadcasts**

```kotlin
// Phát — giới hạn receiver phải có permission
sendBroadcast(intent, "com.example.myapp.RECEIVE_DOWNLOAD")

// Nhận — chỉ nhận broadcast từ sender có permission
val filter = IntentFilter(BroadcastConstants.ACTION_DOWNLOAD_COMPLETE)
registerReceiver(receiver, filter, "com.example.myapp.SEND_DOWNLOAD", null)

// Giới hạn trong nội bộ app — đơn giản nhất
intent.`package` = packageName  // Chỉ app cùng package mới nhận
```

---

## 3. **Ordered** Broadcasts

`sendBroadcast()` thông thường phát **unordered broadcast** — tất cả các **receiver** nhận **cùng lúc**, không có thứ tự.<br/>
Trong khi đó, `sendOrderedBroadcast()` phát **ordered broadcast** — delivery tuần tự theo `priority`.

```
sendOrderedBroadcast(intent)
        ↓
  Receiver A (priority=100) → xử lý → setResult() → tiếp tục / abortBroadcast()
        ↓
  Receiver B (priority=50)  → xử lý → setResult() → tiếp tục
        ↓
  Receiver C (priority=1)   → xử lý xong
        ↓
  Final Receiver (luôn nhận — dù bị abort)
```

### 3.1. Cơ chế vận hành

1. **Sắp xếp Priority Queue**: Hệ thống dựa vào thuộc tính `android:priority` (khai báo trong `IntentFilter` từ $-1000$ đến $1000$) để xếp hạng các Receiver từ cao xuống thấp.
2. **Hàm phát broadcast**: kích hoạt bằng `context.sendOrderedBroadcast(intent, null)` hoặc `context.sendOrderedBroadcast(intent, permission, ...)`.
3. **Truyền dẫn** và **Đồng bộ**: hệ thống chuyển intent đến từng receiver theo thứ tự, chờ receiver xử lý xong mới chuyển sang receiver tiếp theo.
   > _**Receiver** có độ ưu tiên cao hơn nhận broadcast trước, có thể **modify** dữ liệu trong `Intent` bằng hàm `setResultData()` hoặc `setResultExtras()`._
4. Cơ chế **Abort**: receiver có thể **dừng broadcast** bằng cách gọi `abortBroadcast()`, ngăn các receiver tiếp theo nhận được broadcast.

### 3.2. **Implementation**

**Bài toán**: App nhận thông báo đẩy, cần qua 3 bước — **validate** → **enrich data** → **hiển thị**. Mỗi bước là một **`Receiver` độc lập**.

#### **Bước 1: Định nghĩa các Receiver**

- Receiver 1 — `ValidateReceiver`: **_highest_ priority**:

  ```kotlin
  // Receiver 1 — Validate (priority cao nhất)
  class ValidationReceiver : BroadcastReceiver() {

      override fun onReceive(context: Context, intent: Intent) {
          val payload = intent.getStringExtra("payload")

          if (payload.isNullOrEmpty()) {
              Log.d("Ordered", "Validation failed — abort")
              abortBroadcast()  // Dừng chain — B và C không nhận
              return
          }

          // Validation pass — set result cho receiver tiếp theo
          setResult(
              Activity.RESULT_OK,
              payload.trim(),     // resultData — truyền sang receiver sau
              null                // resultExtras
          )

          Log.d("Ordered", "Validation passed")
      }
  }
  ```

- Receiver 2 — `EnrichmentReceiver`: **_medium_ priority**

  ```kotlin
  // Receiver 2 — Enrich data (priority trung bình)
  class EnrichmentReceiver : BroadcastReceiver() {

      override fun onReceive(context: Context, intent: Intent) {
          // Nhận data từ ValidationReceiver
          val validatedPayload = resultData ?: return

          // Enrich — thêm timestamp, user info, ...
          val enriched = "$validatedPayload | ts=${System.currentTimeMillis()}"

          setResult(Activity.RESULT_OK, enriched, null)

          Log.d("Ordered", "Enriched: $enriched")
      }
  }
  ```

- Receiver 3 — `DisplayReceiver`: **_lowest_ priority**

  ```kotlin
  // Receiver 3 — Display (priority thấp nhất)
  class DisplayReceiver : BroadcastReceiver() {

      override fun onReceive(context: Context, intent: Intent) {
          val finalData = resultData ?: return
          Log.d("Ordered", "Displaying: $finalData")
          // Hiển thị notification, update UI, ...
      }
  }
  ```

#### **Bước 2: Khai báo trong `AndroidManifest.xml`**:

```xml
<!-- Khai báo trong Manifest với priority -->
<receiver android:name=".ValidationReceiver" android:exported="false">
    <intent-filter android:priority="100">
        <action android:name="com.example.myapp.PROCESS_NOTIFICATION" />
    </intent-filter>
</receiver>

<receiver android:name=".EnrichmentReceiver" android:exported="false">
    <intent-filter android:priority="50">
        <action android:name="com.example.myapp.PROCESS_NOTIFICATION" />
    </intent-filter>
</receiver>

<receiver android:name=".DisplayReceiver" android:exported="false">
    <intent-filter android:priority="1">
        <action android:name="com.example.myapp.PROCESS_NOTIFICATION" />
    </intent-filter>
</receiver>
```

#### **Bước 3: Phát Ordered Broadcast từ `Service` hoặc `Activity`**:

```kotlin
// Phát Ordered Broadcast — với Final Receiver
val intent = Intent("com.example.myapp.PROCESS_NOTIFICATION").apply {
    putExtra("payload", "  Hello World  ")
    `package` = packageName
}

sendOrderedBroadcast(
    intent,
    null,                    // receiverPermission

    object : BroadcastReceiver() {
        // Final Receiver — luôn được gọi cuối cùng
        // Kể cả khi abortBroadcast() đã được gọi
        override fun onReceive(context: Context, intent: Intent) {
            val finalResult = resultData
            val resultCode = resultCode

            Log.d("Ordered", "Final result: $finalResult, code: $resultCode")
            // Dùng để logging, cleanup, fallback handling
        }
    },

    null,                    // scheduler
    Activity.RESULT_OK,      // initialCode
    null,                    // initialData
    null                     // initialExtras
)
```

### 3.3. Các **`API` truyền data** giữa các Receiver

- **Ghi đè data** cho receiver tiếp theo:

  ```kotlin
  // Set result — ghi đè data cho receiver tiếp theo
  setResult(resultCode, resultData, resultExtras)

  // Hoặc riêng lẻ
  setResultCode(Activity.RESULT_OK)
  setResultData("processed data")
  setResultExtras(bundle)
  ```

- **Đọc data** từ receiver trước:

  ```kotlin
  // Đọc data từ receiver trước
  val code = resultCode
  val data = resultData
  val extras = resultExtras
  ```

- **Dừng chain** — receiver sau không nhận:

  ```kotlin
  // Dừng chain — receiver sau không nhận
  abortBroadcast()
  ```

- **Kiểm tra trạng thái** của broadcast:

  ```kotlin
  // Kiểm tra có bị abort không (dùng trong Final Receiver)
  isOrderedBroadcast // true nếu đang trong ordered broadcast
  isInitialStickyBroadcast
  ```

---

## 4. **Summary**

### **Compare**

|                  | System     | Custom           | Ordered                 |
| ---------------- | ---------- | ---------------- | ----------------------- |
| Ai phát          | Android OS | App tự phát      | App tự phát             |
| Thứ tự           | Song song  | Song song        | Tuần tự theo priority   |
| Có thể **abort** | Không      | Không            | Có — `abortBroadcast()` |
| Truyền data      | Không      | Qua `Intent`     | Qua `setResult()`       |
| Protected        | Có         | Không            | Không                   |
| Dùng cho         | Sự kiện OS | Giao tiếp nội bộ | Pipeline xử lý          |

### **Decision** tree

```
Cần giao tiếp giữa component?
│
├── Sự kiện từ OS (pin, mạng, boot)?
│     └── System Broadcast — lắng nghe, không phát
│
├── Giao tiếp trong nội bộ app?
│     ├── Đơn giản, 1 receiver?  → Custom Broadcast (hoặc LiveData)
│     └── Cần xử lý tuần tự?    → Ordered Broadcast
│
└── Cần giao tiếp với app khác?
      └── Custom Broadcast + permission
```
