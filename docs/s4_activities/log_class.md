# **`Log`** class

`Log` là **utility class** của Android SDK dùng để **ghi thông tin ra Logcat** — console của Android Studio. Đây là công cụ debug cơ bản và quan trọng nhất trong Android development

Có `5` mức log theo thứ tự tăng dần về **mức độ nghiêm trọng**:

- `Log.v()`: **Verbose**, mức thấp nhất, thông tinrất chi tiết, thường dùng để debug
- `Log.d()`: **Debug**, thông tin debug trong development, ít chi tiết hơn verbose
- `Log.i()`: **Info** (thông tin chung, không phải lỗi)
- `Log.w()`: **Warning** (cảnh báo, có thể là vấn đề nhưng không phải lỗi nghiêm trọng)
- `Log.e()`: **Error** (lỗi nghiêm trọng, cần chú ý)

### Cú pháp:

```kotlin
Log.d(TAG, message)
Log.e(TAG, message, throwable)  // throwable optional — dùng khi log exception
```

Có `2` tham số bắt buộc:

- `TAG` — nhãn để **filter** trong Logcat, **thường là tên class**
- `message` — nội dung cần log

### Đặt tên `TAG`:

```kotlin
class MainActivity : AppCompatActivity() {

    // Cách 1: String thủ công — đơn giản nhất
    private val TAG = "MainActivity"

    // Cách 2: Dùng tên class — tránh typo
    private val TAG = MainActivity::class.java.simpleName

    // Cách 3: companion object — dùng được ở cả static context
    companion object {
        private const val TAG = "MainActivity"
    }
}
```

> _Cách 2 được khuyến nghị nhất — tên **`TAG` tự động đổi nếu đổi tên class**._

### Example:

```kotlin
class NetworkManager {

    companion object {
        private val TAG = NetworkManager::class.java.simpleName
    }

    fun fetchUser(userId: String) {
        Log.v(TAG, "fetchUser called — userId: $userId")   // VERBOSE: log mọi thứ

        Log.d(TAG, "Starting network request...")           // DEBUG: theo dõi flow

        try {
            // ... gọi API
            val user = api.getUser(userId)
            Log.i(TAG, "User fetched successfully: ${user.name}")  // INFO: kết quả quan trọng

        } catch (e: TimeoutException) {
            Log.w(TAG, "Request timeout — retrying...")             // WARN: chưa fail hẳn

        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user: ${e.message}", e)    // ERROR: thất bại
        }
    }
}
```

### `Log` trong lifecycle — Cách debug thực tế

```kotlin
class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate — savedInstanceState: ${savedInstanceState != null}")
    }

    override fun onStart()   { super.onStart();   Log.d(TAG, "onStart")   }
    override fun onResume()  { super.onResume();  Log.d(TAG, "onResume")  }
    override fun onPause()   { super.onPause();   Log.d(TAG, "onPause")   }
    override fun onStop()    { super.onStop();    Log.d(TAG, "onStop")    }
    override fun onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy — isFinishing: $isFinishing") }
}
```

### `Log` exception

```kotlin
try {
    val result = riskyOperation()
} catch (e: Exception) {
    // SAI — mất stack trace
    Log.e(TAG, "Error: " + e.message)

    // ĐÚNG — giữ toàn bộ stack trace
    Log.e(TAG, "Error occurred", e)
}
```

> _Luôn truyền `Throwable` làm tham số thứ 3 khi **log exception** — Logcat sẽ in đầy đủ **stack trace**, giúp trace nguyên nhân lỗi chính xác._
