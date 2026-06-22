# **_Bound_ Service**

## 1. **What** is **Bound Service**

Cả **Started** và **Foreground** services đều là **one-way** — `Activity` bắn lệnh rồi thôi, không nhận lại gì.<br/>
**Bound Service** hoạt động theo mô hình **Client-Server**, giải quyết bài toán **giao tiếp 2 chiều** giữa `Activity`/`Fragment` với `Service`. Trong đó, `Activity` có thể **gọi method trực tiếp** trên `Service` và **nhận kết quả** trả về.

**Usecase**:

- **Music player**: Activity gọi `play()`, `pause()`, `seekTo()`, .. trên Service
- **Timer**: Activity đọc `getCurrentTime()` từ Service đang đếm.
- **Bluetooth**: Activity query trạng thái kết nối từ Service đang quản lý.

---

## 2. Các phương pháp triển khai **Bound Service** - `Binder`, `Messenger` & `AIDL`

Có 2 cách triển khai **Bound Service**, phụ thuộc việc Client nằm **CHUNG** hay **KHÁC** `process`:

- **Giải pháp 1**: kế thừa từ lớp `Binder` (_dành cho **Local Process**_)
  - Nếu **Service** và **Client** (`Activity`) nằm chung trong **cùng ứng dụng** & **cùng process**, sử dụng `Binder` để triển khai **Bound service**.
  - **Cơ chế**: Client nhận trực tiếp thực thể `Binder` từ `onBind()`, **ép kiểu (cast) về class `Binder`** cụ thể và gọi thẳng các hàm public của **Service** với tốc độ thực thi tức thì (**không tốn chi phí IPC**).
- **Giải pháp 2**: sử dụng `Messenger` (_dành cho **liên tiến trình - `ICP` (Inter-Process Communication)** an toàn_)
  - Sử dụng khi cần giao tiếp với **app khác**, nhưng muốn **xử lý đơn luồng**.
  - **Cơ chế**: `Messenger` tạo ra một **hàng đợi** nằm trên một **luồng duy nhất - `HandlerThread`**. Tất cả **request** từ Process khác đều được xếp hàng xử lý tuần tự (**Thread-safe mặc định**)
- **Giải pháp 3**: sử dụng **`AIDL` - Android Interface Definition Language** - _giải pháp **`tối ưu`** cho **IPC phức tạp, đa luồng**_
  - **Cơ chế**: **Tách biệt hoàn toàn** giao diện lập trình.
    - **Service** có thể **xử lý đồng thời** nhiều request từ nhiều tiến trình khác nhau bay vào cùng một lúc.
    - **Lập trình viên** phải **tự quản lý** đa luồng (Thread-safety).

---

## 3. **Lifecycle** và Cơ chế **Binding** — `Binder`

### Cơ chế hoạt động

Để thực hiện được **giao tiếp 2 chiều**, giữa `Activity` và `Service` cần một object trung gian — gọi là **`Binder`**

![Communication between Activity and Service](/res/s7/img/binder.png)

```
Activity                                Service
   │                                        │
   │────  bindService(intent, conn)  ─────> │
   │                                        │
   │                                onBind() trả về Binder
   │<----  onServiceConnected(binder)  ---- │
   │                                        │
   │                                        │
   │────── binder.getService() ───────────> │
   │                                        │
   │<-----  trả về Service instance  ------ │
   │                                        │
   │                                        │
   │──────── service.play() ──────────────> │  // Gọi method trực tiếp
   │                                        │
   │                                        │
   │────── service.getCurrentTime() ──────> │
   │                                        │
   │<------  trả về Long ------------------ │
```

### **Lifecycle** của **Bound Service**

Vòng đời của Bound Service **gắn liền chặt chẽ với các Client kết nối tới** nó. Nó **chỉ tồn tại khi `ít nhất còn một` Client** duy trì kết nối.

Các `callback` cốt lõi:

- `onCreate()`: khởi tạo **tài nguyên hệ thống**. Callback được gọi **một lần duy nhất** khi có **client đầu tiên** gọi `bindService()`.
- `onBind(Intent)`: hệ thống gọi callback này khi client đầu tiên thực hiện kết nối.
  - Bắt buộc phải trả về `IBinder` để client có thể giao tiếp với service.
  - Nếu trả về `null`, Service sẽ biến thành một **Bound service `không cho phép bind`**

    > _Hệ quả kỹ thuật: Hàm `onServiceConnected(ComponentName, IBinder)` ở phía Client **vẫn được hệ thống kích hoạt**, nhưng tham số `IBinder` truyền về sẽ bị `null`. **Client sẽ không thể gọi bất kỳ hàm public nào của Service**_

    Nó thường dùng khi chỉ muốn **"mượn" vòng đời của Bound Service để giữ app sống** mà không cần tương tác qua lại

  **`IBinder` - instance** trả về được giữ lại trong **Service** và được **tái sử dụng** cho các Client khác bind vào.

- `onUnbind(Intent)`: Gọi khi **TẤT CẢ các Client đã ngắt kết nối** thông qua hàm `unbindService()`. Trả về **`boolean`** để quyết định có gọi `onRebind()` hay không nếu có Client mới bind lại:
  - `true`: Gọi `onRebind()` nếu có Client mới bind lại.
  - `false`: Không gọi `onRebind()`:
    - Nếu **service** đã rơi vào trạng thái **`destroy`** (_bị **hủy** khi không còn Client nào bind_), chu trình chạy lại từ `onCreate()`.
    - Nếu **service** chưa bị hủy, các **Client mới** sẽ tái sử dụng lại **instance `IBinder`** đã được sinh ra trong `onBind()` ở client đầu tiên để sử dụng.
- `onDestroy()`: được gọi khi **service** được hủy hoàn toàn, không còn connection, giải phóng khỏi bộ nhớ.

```
bindService()
      ↓
  onCreate()          ← Chỉ gọi 1 lần
      ↓
  onBind()            ← Trả về Binder cho client
      ↓
  [Service bound]     ← Tồn tại chừng nào còn client
      ↓
  onUnbind()          ← Tất cả client đã unbind
      ↓
  onDestroy()
```

---

## 4. Triển khai **Bound Service** - `Binder`

### **Bước 1**: tạo service với `Binder`

```kotlin
class MusicService : Service() {

    // Binder — cầu nối cho Activity
    private val binder = MusicBinder()

    // State của Service
    private var isPlaying = false
    private var currentPosition = 0
    private var songTitle = ""

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Inner class kế thừa Binder
     * Cung cấp method getService() để Activity lấy reference
     */
    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    // ───── lifecycle ─────
    /**
     * onBind() — gọi khi client bindService()
     * PHẢI return Binder — khác với Started Service return null
     */
    override fun onBind(intent: Intent): IBinder = binder
    /**
     * onDestroy -> cancel jobs
     */
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    // ── Public API cho Activity gọi ──────────────────────────
    fun play(title: String) {
        songTitle = title
        isPlaying = true
        Log.d("MusicService", "Playing: $title")

        scope.launch {
            while (isPlaying) {
                delay(1000)
                currentPosition++
            }
        }
    }
    fun pause() {
        isPlaying = false
        Log.d("MusicService", "Paused at: $currentPosition s")
    }
    fun stop() {
        isPlaying = false
        currentPosition = 0
        songTitle = ""
    }
    fun isPlaying(): Boolean = isPlaying
    fun getCurrentPosition(): Int = currentPosition
    fun getSongTitle(): String = songTitle
}
```

### **Bước 2**: đăng kí **service** trong `Manifest`

```xml
<service
    android:name=".MusicService"
    android:exported="false" />
```

### **Bước 3**: Bind từ `Activity`

```kotlin
class MainActivity : AppCompatActivity() {

    private var musicService: MusicService? = null
    private var isBound = false

    /**
     * ServiceConnection — callback khi bind/unbind
     * Đây là "cầu nhận" Binder từ Service
     */
    private val connection = object : ServiceConnection {

        // Gọi khi bind thành công
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val musicBinder = binder as MusicService.MusicBinder
            musicService = musicBinder.getService() // Lấy Service instance
            isBound = true

            Log.d("MainActivity", "Service connected")
        }

        // Gọi khi Service bị disconnect ĐỘT NGỘT (crash, bị kill)
        // KHÔNG gọi khi unbindService() bình thường
        override fun onServiceDisconnected(name: ComponentName) {
            musicService = null
            isBound = false

            Log.d("MainActivity", "Service disconnected unexpectedly")
        }
    }


    /**
     * bindService() khi activity start
     */
    override fun onStart() {
        super.onStart()
        // Bind khi Activity visible
        Intent(this, MusicService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        // BIND_AUTO_CREATE → tự động onCreate() nếu Service chưa tồn tại
    }

    /**
     * bindService() khi activity start
     */
    override fun onStop() {
        super.onStop()
        // Unbind khi Activity không visible — bắt buộc
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }


    // Gọi method trên Service — luôn check isBound trước
    private fun onPlayClick() {
        if (isBound) {
            musicService?.play("Bohemian Rhapsody")
        }
    }
    private fun onPauseClick() {
        if (isBound) {
            musicService?.pause()
        }
    }
    private fun onQueryClick() {
        if (isBound) {
            val position = musicService?.getCurrentPosition()
            val title = musicService?.getSongTitle()
            Log.d("MainActivity", "$title — ${position}s")
        }
    }
}
```

### **Note**: `BIND_AUTO_CREATE` và các **flag** khác

```kotlin
// Activity.onCreate()
Intent(this, MusicService::class.java).also { intent ->
    bindService(intent, connection, Context.BIND_AUTO_CREATE)
}
```

| Flag                  | Hành vi                                                                     |
| --------------------- | --------------------------------------------------------------------------- |
| `BIND_AUTO_CREATE`    | Tự động `onCreate()` nếu **Service chưa tồn tại**, hầu hết sử dụng flag này |
| `BIND_NOT_FOREGROUND` | **Không** cho service được promote lên **foreground priority**              |
| `BIND_ABOVE_CLIENT`   | Service có priority cao hơn client, _hiếm khi dùng_                         |

---

## 5. **Bound Service** + **Started Service**

Thực tế, có thể cần **kết hợp cả 2 loại service**: Giả sử, với `Music Player`:

- **Foreground** service: dành cho notification, không bị kill.
- **Bound** service: `Activity` điều khiển `play`/`pause`/`stop`

```kotlin
class MusicService : Service() {

    private val binder = MusicBinder()

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    // Gọi khi startForegroundService() từ Activity
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    // Gọi khi bindService() từ Activity
    override fun onBind(intent: Intent): IBinder = binder

    // Gọi khi tất cả client unbind — nhưng Service vẫn chạy
    // vì đã được start bằng startForegroundService()
    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }
}
```

**Lifecycle kết hơp**:

```
startForegroundService() → onCreate() → onStartCommand()
                                              ↓
bindService()            →            onBind() → [Activity kết nối]
                                              ↓
unbindService()          →            onUnbind() → Service VẪN CHẠY
                                              ↓      (vì đã startForeground)
stopService()            →            onDestroy()
```

|                 | **Started**    | **Foreground**    | **Bound**              |
| --------------- | -------------- | ----------------- | ---------------------- |
| **Start**       | `startService` | `startForeground` | `bindService`          |
| **Stop**        | `stopSelf()`   | `stopSelf()`      | **auto** khi unbind    |
| Notification    | Không          | **Bắt buộc**      | Không                  |
| Bị Android kill | Có thể         | **Rất hiếm**      | Không (gắn với client) |
| Giao tiếp       | 1 chiều        | 1 chiều           | **2 chiều**            |
| Use case        | Sync, log      | Music, GPS        | Music control, BT      |
