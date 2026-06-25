# **DataStore**

## 1. **`DataStore`** vs **`SharedPreferences`**

**`SharedPreferences`** có một số vấn đề khiến nó bị thay thế bởi **`DataStore`**:

- **Synchronous API**: **`SharedPreferences`** sử dụng một số API đồng bộ:
  - `getSharedPreferences()` block Main Thread ở **lần load đầu**
  - `commit()` block Main Thread khi **lưu dữ liệu**
- **Không `type-safe`**: **`SharedPreferences`** lưu dữ liệu dưới dạng **key-value pairs**, không có cơ chế cảnh báo từ compiler khi get với key sai.
- **Không có error handling**: **`SharedPreferences`** không có cơ chế xử lý lỗi khi đọc/ghi dữ liệu, không có callback.
- **Không safe với multi-process**: nhiều `process` đọc/ghi cùng lúc có thể dẫn tới **data loss** hoặc **data corruption**.

**`DataStore`** được thiết kế để giải quyết các vấn đề trên:

- **Asynchronous API**:
  - **`DataStore`** sử dụng **Kotlin Coroutines (`suspend fun`)** và **`Flow`**
  - Không block Main Thread.
- **Type-safe** (`Proto` DataStore): **Schema** được định nghĩa rõ ràng.
- **Error handling**: exception được propagate qua **`Flow`**.
- **Transactional**: `write` là **atomic operation**, không bị **corrupt**.

---

## 2. **`DataStore`** types

Có 2 loại **`DataStore`**:

- **Preferences DataStore**: lưu dữ liệu dạng **key-value pairs**, tương tự **`SharedPreferences`** nhưng tốt hơn.
  > _Không cần `schema` định trước, dùng cho settings, flags, tokens, ...._
- **Proto DataStore**: lưu **typed object** theo **Protocol Buffers** schema.
  - `Type-safe` hoàn toàn, cần định nghĩa `.proto` file trước.
  - Phức tạp hơn, dùng khi cần **strict typing**

Thông thường, **Preferences DataStore** phổ biến hơn và đủ cho hầu hết các usecases.

---

## 3. **Implementation**

### 3.1. **Dependencies**

```gradle
// build.gradle.kts (Module: app)
dependencies {
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Nếu dùng Proto DataStore
    // implementation("androidx.datastore:datastore:1.1.1")


    // Coroutines — DataStore yêu cầu
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Lifecycle — để collect Flow trong Activity/Fragment
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}
```

### 3.2. Tạo **`DataStore` instance**

```kotlin
// Cách chuẩn: top-level property trong file riêng
// Không đặt trong class — đảm bảo singleton

// AppDataStore.kt
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings"   // tên file, tương đương tên trong SharedPreferences
)
```

`preferencesDataStore` là **Kotlin property delegate** — tạo một **`DataStore` _singleton_ gắn với `Context`** (Application).

> _Gọi nhiều lần trên **cùng Context** → trả về **cùng instance**._

File được lưu tại:

```
/data/data/com.example.app/files/datastore/app_settings.preferences_pb
```

### 3.3. Định nghĩa **`key`**

Khác với **`SharedPreferences`**, **`DataStore`** không dùng `String` làm key mà dùng **typed key** - `Preferences.Key<T>`.

```kotlin
object AppPreferencesKeys {
    // Mỗi key có type rõ ràng
    val IS_DARK_MODE   = booleanPreferencesKey("is_dark_mode")
    val LANGUAGE       = stringPreferencesKey("language")
    val AUTH_TOKEN     = stringPreferencesKey("auth_token")
    val USER_ID        = intPreferencesKey("user_id")
    val FONT_SIZE      = intPreferencesKey("font_size")
    val LAST_SYNC      = longPreferencesKey("last_sync")
    val IS_ONBOARDED   = booleanPreferencesKey("is_onboarded")
}
```

Các **hàm tạo `key`**:

```kotlin
booleanPreferencesKey("name")       → Preferences.Key<Boolean>
stringPreferencesKey("name")        → Preferences.Key<String>
intPreferencesKey("name")           → Preferences.Key<Int>
longPreferencesKey("name")          → Preferences.Key<Long>
floatPreferencesKey("name")         → Preferences.Key<Float>
doublePreferencesKey("name")        → Preferences.Key<Double>
stringSetPreferencesKey("name")     → Preferences.Key<Set<String>>
```

> _**Công thức**: `<type>PreferencesKey("name")` → `Preferences.Key<type>`_

### 3.4. **Read** data - `Flow`

Đây là **điểm khác biệt lớn nhất** với `SharedPreferences` — **`DataStore` expose data qua `Flow`**, KHÔNG phải trả về giá trị trực tiếp:

```kotlin
// Đọc một giá trị — trả về Flow<Boolean>
val isDarkModeFlow: Flow<Boolean> = context.dataStore.data
    .map { preferences ->
        preferences[AppPreferencesKeys.IS_DARK_MODE] ?: false
    }
```

Có thể đọc nhiều giá trị cùng lúc:

```kotlin
// Đọc nhiều giá trị cùng lúc
data class AppSettings(
    val isDarkMode: Boolean,
    val language: String,
    val fontSize: Int
)

val appSettingsFlow: Flow<AppSettings> = context.dataStore.data
    .map { preferences ->
        AppSettings(
            isDarkMode = preferences[AppPreferencesKeys.IS_DARK_MODE] ?: false,
            language   = preferences[AppPreferencesKeys.LANGUAGE] ?: "vi",
            fontSize   = preferences[AppPreferencesKeys.FONT_SIZE] ?: 14
        )
    }
```

#### **Error handling** khi đọc dữ liệu

```kotlin
val isDarkModeFlow: Flow<Boolean> = context.dataStore.data
    .catch { exception ->
        // IOException xảy ra khi không đọc được file
        if (exception is IOException) {
            emit(emptyPreferences())   // emit giá trị rỗng thay vì crash
        } else {
            throw exception   // re-throw nếu là lỗi khác
        }
    }
    .map { preferences ->
        preferences[AppPreferencesKeys.IS_DARK_MODE] ?: false
    }
```

#### Example **Collect `Flow` trong `ViewModel`**

```kotlin
class SettingsViewModel(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[AppPreferencesKeys.IS_DARK_MODE] ?: false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val language: StateFlow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[AppPreferencesKeys.LANGUAGE] ?: "vi" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "vi"
        )
}
```

Hàm `stateIn()` giúp **chuyển `Flow` thành `StateFlow`**, có `value` có thể đọc đồng bộ và phù hợp để observe từ UI.

#### **Collect `StateFlow` từ `ViewModel` trong `Fragment`**

```kotlin
class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Collect StateFlow từ ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isDarkMode.collect { isDark ->
                    binding.switchDarkMode.isChecked = isDark
                }
            }
        }
    }
}
```

### 3.5. **Write** data - `edit`

#### **`edit()` - suspend function**

Hàm `edit()` là **suspend function**, phải được gọi trong **Coroutine**, đồng thời không block **Main Thread**. <br/>
Nó nhận một **`lambda`** với tham số là `MutablePreferences` để **update giá trị**.

- Ghi giá trị:

  ```kotlin
  // Ghi một giá trị
  suspend fun saveDarkMode(isDark: Boolean) {
      context.dataStore.edit { preferences ->
          preferences[AppPreferencesKeys.IS_DARK_MODE] = isDark
      }
  }

  // Ghi nhiều giá trị cùng lúc — atomic
  suspend fun saveUserSession(token: String, userId: Int) {
      context.dataStore.edit { preferences ->
          preferences[AppPreferencesKeys.AUTH_TOKEN] = token
          preferences[AppPreferencesKeys.USER_ID]    = userId
      }
  }
  ```

- Xóa giá trị:

  ```kotlin
  // Xóa một key
  suspend fun clearAuthToken() {
      context.dataStore.edit { preferences ->
          preferences.remove(AppPreferencesKeys.AUTH_TOKEN)
      }
  }

  // Xóa tất cả
  suspend fun clearAll() {
      context.dataStore.edit { preferences ->
          preferences.clear()
      }
  }
  ```

#### Gọi `edit()` trong **`ViewModel`**:

```kotlin
class SettingsViewModel(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    fun setDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AppPreferencesKeys.IS_DARK_MODE] = isDark
            }
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AppPreferencesKeys.LANGUAGE] = language
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs.remove(AppPreferencesKeys.AUTH_TOKEN)
                prefs.remove(AppPreferencesKeys.USER_ID)
            }
        }
    }
}
```

---

## 4. **Pattern chuẩn - `Repository`**

Pattern tốt nhất — **tách `DataStore` vào `Repository`**, `ViewModel` **không biết** `DataStore` tồn tại:

- **`Activity`/`Fragment` chỉ biết `ViewModel`**, không quan tâm DataStore.
- **`ViewModel` chỉ biết repository**, không cần biết `DataStore` tồn tại
- **`Repository`** chịu trách nhiệm đóng gói **access DataStore**

#### **Bước 1**: Tạo `Repository`:

```kotlin
// UserPreferencesRepository.kt
class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {

    // ── Read ─────────────────────────────────────

    val isDarkMode: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[AppPreferencesKeys.IS_DARK_MODE] ?: false }

    val language: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[AppPreferencesKeys.LANGUAGE] ?: "vi" }

    val authToken: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[AppPreferencesKeys.AUTH_TOKEN] }

    val isOnboarded: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[AppPreferencesKeys.IS_ONBOARDED] ?: false }

    // Đọc một lần — không cần observe liên tục
    suspend fun getAuthTokenOnce(): String? {
        return dataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { it[AppPreferencesKeys.AUTH_TOKEN] }
            .first()   // lấy giá trị hiện tại rồi cancel flow
    }

    // ── Write ────────────────────────────────────

    suspend fun setDarkMode(isDark: Boolean) {
        dataStore.edit { it[AppPreferencesKeys.IS_DARK_MODE] = isDark }
    }

    suspend fun setLanguage(language: String) {
        dataStore.edit { it[AppPreferencesKeys.LANGUAGE] = language }
    }

    suspend fun saveUserSession(token: String, userId: Int) {
        dataStore.edit {
            it[AppPreferencesKeys.AUTH_TOKEN] = token
            it[AppPreferencesKeys.USER_ID]    = userId
        }
    }

    suspend fun setOnboarded(onboarded: Boolean) {
        dataStore.edit { it[AppPreferencesKeys.IS_ONBOARDED] = onboarded }
    }

    suspend fun clearUserSession() {
        dataStore.edit {
            it.remove(AppPreferencesKeys.AUTH_TOKEN)
            it.remove(AppPreferencesKeys.USER_ID)
        }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
```

#### **Bước 2**: Tiếp theo, **inject** `Repository` vào `ViewModel`:

```kotlin
class SettingsViewModel(
    private val repository: UserPreferencesRepository
) : ViewModel() {

    // Convert Flow → StateFlow để UI observe
    val isDarkMode: StateFlow<Boolean> = repository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val language: StateFlow<String> = repository.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "vi")

    fun onDarkModeToggled(isDark: Boolean) {
        viewModelScope.launch {
            repository.setDarkMode(isDark)
        }
    }

    fun onLanguageSelected(language: String) {
        viewModelScope.launch {
            repository.setLanguage(language)
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            repository.clearUserSession()
        }
    }
}
```

#### **Bước 3**: Cuối cùng, tạo **`Application` wrapper class** cung cấp `DataStore` singleton và `Repository`:

```kotlin
class MyApplication : Application() {

    // DataStore singleton
    val dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

    // Repository singleton
    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(dataStore)
    }
}
```

#### **Bước 4**: Sử dụng:

- Ở **Activity**/**Fragment**, lấy `repository` từ `Application`:

  ```kotlin
  // Trong Fragment — lấy repository
  val repository = (requireActivity().application as MyApplication).userPreferencesRepository
  ```

- Sau đó, lấy `ViewModel` với `repository`:

  ```kotlin
  // Trong Fragment — tạo ViewModel với repository
  val viewModel: SettingsViewModel by viewModels {
      object : ViewModelProvider.Factory {
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
              return SettingsViewModel(repository) as T
          }
      }
  }
  ```

---

## 5. **Migration** từ `SharedPreferences` sang `DataStore`

```kotlin
// Tự động migrate data từ SharedPreferences cũ sang DataStore
val dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context,
                "app_settings"   // tên SharedPreferences cũ
            )
        )
    }
)
// Lần đầu tiên DataStore được tạo → tự động đọc SP cũ và migrate
// Sau khi migrate xong → SP file cũ bị xóa
```
