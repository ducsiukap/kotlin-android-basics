# `Application` class

## 1. **Định nghĩa**

`Application` là class đại diện cho **toàn bộ app** — được **khởi tạo TRƯỚC** tất cả `Activity`, `Service`, `Receiver`:

- `Application` là một **Singleton duy nhất**, tồn tại xuyên suốt trong quá trình process của ứng dụng chạy.
- Được khởi tạo **SAU** `ContentProvider` nhưng **TRƯỚC** bất kì `Activity`, `Service`, `Receiver`, .. nào

---

## 2. **Custom `Application`**

### 2.1. **Mục đích** khi cần **custom `Application`** — _di, khởi tạo các component cần thiết chạy toàn app, ..._

```kotlin
class MyApplication : Application() {

    // ── Singleton instances ───────────────────────────

    // DataStore
    val dataStore: DataStore<Preferences> by preferencesDataStore(
        name = "app_settings"
    )

    // Repository
    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(dataStore)
    }

    // Room Database
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this,                    // Application Context
            AppDatabase::class.java,
            "app_database"
        ).build()
    }

    // Repository từ Room
    val noteRepository: NoteRepository by lazy {
        NoteRepository(database.noteDao())
    }

    override fun onCreate() {
        super.onCreate()

        // ── Khởi tạo thư viện third-party ────────────

        // Timber (logging)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Notification Channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val messageChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Tin nhắn",
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(messageChannel)
        }
    }

    companion object {
        const val CHANNEL_MESSAGES = "channel_messages"
    }
}
```

### 2.2. Tạo **custom `Application`**

Để **định nghĩa** một **custom `Application`**, cần:

1. **Kế thừa `Application`** class:

   ```kotlin
   class MyApplication : Application() {

       override fun onCreate() {
           super.onCreate()
           // Khởi tạo các thứ cần thiết cho toàn bộ app
           // Được gọi TRƯỚC khi bất kỳ Activity nào được tạo
       }
   }
   ```

2. **Khai báo** trong `Manifest` (băt buộc):

   ```xml
   <application
       android:name=".MyApplication"   <!-- tên class Application của bạn -->
       android:icon="@mipmap/ic_launcher"
       android:label="@string/app_name"
       ...>
   ```

   nếu không, **Android** dùng `Application` mặc định.

### 2.3. Lấy **`Application` instance**

Có thể **lấy `Application` instance từ bất kì đâu**, từ `Activity`, `Fragment`, ...

```kotlin
// Trong Activity
val myApp = application as MyApplication // cast sang custom application
val repo = myApp.noteRepository

// Trong Fragment
val myApp = requireActivity().application as MyApplication
val repo = myApp.noteRepository

// Trong ViewModel (AndroidViewModel)
class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val myApp = application as MyApplication
    private val repository = myApp.noteRepository
}
```

hoặc lấy bên trong `ViewModel`:

```kotlin
// ViewModel thường — KHÔNG có Context
class NoteViewModel : ViewModel() {
    // Không truy cập được Context ở đây
    // → Đúng nếu không cần Context
}

// AndroidViewModel — CÓ Application Context
class NoteViewModel(application: Application) : AndroidViewModel(application) {
    // getApplication<MyApplication>() → Application Context an toàn
    private val repository = getApplication<MyApplication>().noteRepository

    // KHÔNG dùng getApplication() để show UI (Dialog, Toast)
    // Application Context không có theme → crash
}

// Tạo AndroidViewModel trong Fragment
private val viewModel: NoteViewModel by viewModels()
// ViewModelProvider tự inject Application instance
```
