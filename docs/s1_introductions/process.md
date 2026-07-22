# **Process** & Application lifecycle

## 1. **Android Process**

### 1.1. **Android Process**

Mỗi khi **user mở app**, Android OS tạo **Linux process** riêng biệt cho app đó — với **bộ nhớ**, **user ID** riêng. <br>
Đây là **cơ sở** của Android **sandbox**, ví dụ:

**Android OS**

- **Process**: `com.example.myapp`
  - **Main Thread** (UI)
  - Background Thread 1
  - Background Thread 2
- **Process**: `com.google.gmail`
- **Process**: `com.facebook.android`
- **Process**: `android` (system)

Hệ quả: các app **không thể truy cập memory của nhau**.

> Đây là lí do `ContentProvider`, `Intent`, **AIDL** tồn tại. Giao tiếp giữa các app phải qua **Android OS**

### 1.2. **Process Priority**

Khi thiết bị thiếu RAM, Android OS **kill process theo thứ tự ưu tiên** — process ưu tiên thấp bị kill trước

| Process                | Mô tả                                                                                                                           | **Priority** <br>(_tỉ lệ nghịch với khả năng bị kill_) |
| ---------------------- | ------------------------------------------------------------------------------------------------------------------------------- | :----------------------------------------------------: |
| **Foreground** process | - **App đang hiển thị** trên màn hình, hoặc<br>- Có `Activity` đang ở `onResume`, hoặc<br>- Có **Foreground Service** đang chạy |                      **CAO NHẤT**                      |
| **Visible** process    | **`Activity` visible** nhưng KHÔNG ở foreground (_bị dialog che, multi-window, .._)                                             |                        **CAO**                         |
| **Service** process    | Có **Background Service** đang chạy                                                                                             |               **GIỚI HẠN**<br> (API 26+)               |
| **Cached** process     | App **đã bị đóng** nhưng còn trong memory, không có component nào đang chạy                                                     |          **THẤP NHẤT**<br> (bị kill đầu tiên)          |

---

## 2. **Application lifecycle**

### 2.1. `Application` lifecycle & `Activity` lifecycle

**Application** có vòng đời riêng, gắn với toàn bộ vòng đời của app:

- **`Application` lifecycle**:
  - `onCreate()` -> **app start** lần đầu tiên
  - `onTerminate()` -> KHÔNG đáng tin cậy, **chỉ gọi trong emulator/test env**, không được gọi trên physical device
- **`Activity` lifecycle**:
  - `onCreate` -> `onStart` -> `onResume` -> `onPause` -> `onStop` -> `onDestroy`
  - Có thể **xảy ra nhiều lần** trong suốt vòng đời app

```
App start:
  Application.onCreate()     ← gọi 1 lần
        |
        ↓
  MainActivity.onCreate()    ← gọi mỗi khi Activity tạo
        |
        |
  [User dùng app]
        |
        |
        ↓ [User nhấn Home]
  MainActivity.onStop()      ← Activity stop
        |
        | Application VẪN SỐNG
        |
        ↓ [User quay lại]
  MainActivity.onRestart()
        |
        ↓ [User nhấn Back, đóng app]
  MainActivity.onDestroy()
        |
        | Application VẪN SỐNG (cached process)
        |
        ↓ Hệ thống kill process:
  Application bị hủy
```

### 2.2. `ProcessLifecycleOwner` — **Observe** Application lifecycle

**Jetpack** cung cấp `ProcessLifecycleOwner` để **observe khi app chuyển giữa foreground và background** — không phải từng Activity mà là **toàn bộ app**:

```kotlin
// build.gradle.kts
implementation("androidx.lifecycle:lifecycle-process:2.7.0")
```

**Cài đặt**: override `onStart()`/`onStop()` trong **custom `Application`**:

```kotlin
class MyApplication : Application(), DefaultLifecycleObserver {

    override fun onCreate() {
        super.onCreate()

        // Đăng ký observe lifecycle của toàn bộ app
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    // Gọi khi app ra FOREGROUND (user mở app)
    override fun onStart(owner: LifecycleOwner) {
        Log.d("AppLifecycle", "App vào foreground")
        // Refresh data, reconnect WebSocket...
    }

    // Gọi khi app vào BACKGROUND (user nhấn Home)
    override fun onStop(owner: LifecycleOwner) {
        Log.d("AppLifecycle", "App vào background")
        // Pause sync, disconnect WebSocket...
    }
}
```

### 2.3. **App Start** — Cold / Warm / Hot: **performance optimization**

#### 2.3.1. **Cold Start** — khởi động **từ đầu** hoàn toàn:

1. Android **tạo Process** mới
2. `Application.onCreate()` ← **tốn thời gian** nhất
3. `Activity.onCreate()`
4. **UI hiển thị**

> Thời gian: 500ms - 2000ms+<br>
> Xảy ra khi: **lần đầu mở app**, sau khi hệ thống kill process

#### 2.3.2. **Warm Start** — Activity bị **destroy nhưng Process còn**:

1. **Process vẫn còn trong memory**, `Application` object vẫn còn
2. `Activity.onCreate()` ← không cần tạo lại Application
3. UI hiển thị

> Thời gian: 200ms - 500ms<br>
> Xảy ra khi: back ra rồi mở lại, **Activity bị recreate**

#### 2.3.3. **Hot Start** — **Activity vẫn còn trong memory**:

1. **Process** và `Activity` đều còn
2. `Activity.onRestart()` → `onStart()` → `onResume()`
3. UI hiển thị

> Thời gian: < 200ms<br>
> Xảy ra khi: **nhấn Home rồi quay lại**

### 2.4. **Tối ưu Cold Start** — `Application.onCreate()` phải **nhanh**

`Application.onCreate()` chạy trên **Main Thread** — nếu **chậm** → **app load lâu** → user thấy màn hình trắng:

```kotlin
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // SAI — khởi tạo nặng trên Main Thread
        val heavyLibrary = HeavyLibrary.init(this)   // tốn 500ms
        val database = Room.build(...)               // tốn 300ms
        // → Cold start mất 800ms+ chỉ riêng phần này

        // ĐÚNG — lazy initialization
        // Chỉ khởi tạo khi thực sự cần
    }

    // Lazy — chỉ tạo khi được access lần đầu
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "app.db").build()
    }

    val repository: NoteRepository by lazy {
        NoteRepository(database.noteDao())
    }
}
```

**Khởi tạo nặng** trên **background thread**:

```kotlin
override fun onCreate() {
    super.onCreate()

    // Khởi tạo thứ bắt buộc ngay (nhẹ)
    setupTimber()
    createNotificationChannels()

    // Khởi tạo nặng → chạy trên background thread
    CoroutineScope(Dispatchers.IO).launch {
        initAnalytics()
        prefetchCommonData()
    }
}
```

---

## 3. **Process Death** & **State restoration**

**Process Death** xảy ra khi Android OS **kill process** của app để lấy lại RAM.<br>
Tuy nhiên, từ góc nhìn của user:

- App **vẫn hiện** trong Recent Apps
- User tap vào App trong Recent Apps -> **expect tiếp tục từ chỗ đã dừng**

Android xử lý bằng cách: **restore UI state** — nhưng chỉ những gì được lưu đúng cách:

- **TỰ ĐỘNG** restore:
  - **Backstack** của Activity/Fragment
  - **`View`'s state** như scroll pos, checked state, text trong EditText, ...
  - `savedInstanceState` bundle
- **KHÔNG** tự restore:
  - **Data** trong `ViewModel`
  - **Variable** trong Activity/Fragment
  - **Singleton data** trong Application.

### `SavedStateHandle` — `ViewModel` **survive process death**

`SavedStateHandle` là **giải pháp** để `ViewModel` **lưu và restore data qua cả process death**:

```kotlin
class DetailViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_SELECTED_TAB = "selected_tab"
        private const val KEY_SCROLL_POS   = "scroll_position"
    }

    // Tự động persist qua process death
    var selectedTab: Int
        get() = savedStateHandle.get<Int>(KEY_SELECTED_TAB) ?: 0
        set(value) = savedStateHandle.set(KEY_SELECTED_TAB, value)

    // StateFlow backed by SavedStateHandle
    val selectedTabFlow: StateFlow<Int> = savedStateHandle.getStateFlow(
        KEY_SELECTED_TAB, 0
    )

    fun updateSelectedTab(tab: Int) {
        savedStateHandle[KEY_SELECTED_TAB] = tab
    }
}

// Tạo ViewModel với SavedStateHandle
// → viewModels() tự động inject SavedStateHandle
private val viewModel: DetailViewModel by viewModels()
```

Phân biệt **các cơ chế lưu state**:

```
                    Configuration    Process      App
                    Change (rotate)  Death        Close
─────────────────────────────────────────────────────────
ViewModel           ✅ survive       ❌ mất        ❌ mất
savedInstanceState  ✅ survive       ✅ survive    ❌ mất
SavedStateHandle    ✅ survive       ✅ survive    ❌ mất
DataStore/Room      ✅ survive       ✅ survive    ✅ survive

→ Kết hợp đúng cách:
  UI state tạm thời → ViewModel + SavedStateHandle
  Data quan trọng   → Room / DataStore
```
