# `Configuration` class

## 1. **What** is `Configuration` class?

`Configuration` là **object chứa thông tin cấu hình hiện tại** của thiết bị — _ngôn ngữ, orientation, screen size, font size, dark mode..._

```kotlin
val config = resources.configuration

// Ngôn ngữ
val locale = config.locales[0]   // API 24+
val language = locale.language   // "vi", "en", "ja"...

// Orientation
val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
val isPortrait  = config.orientation == Configuration.ORIENTATION_PORTRAIT

// Screen size
val screenWidthDp  = config.screenWidthDp   // chiều rộng màn hình tính bằng dp
val screenHeightDp = config.screenHeightDp
val isTablet = config.screenWidthDp >= 600  // tablet thường >= 600dp

// Night mode (dark mode)
val isNightMode = config.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES

// Font scale (user điều chỉnh cỡ chữ trong Settings)
val fontScale = config.fontScale   // 1.0 = normal, 1.3 = large...
```

---

## 2. **Configuration Change**

### 2.1. **Định nghĩa**

**Configuration Change** xảy ra khi cấu hình thiết bị thay đổi:

- Xoay màn hình (_orientation change_) phổ biến nhất
- Đổi ngôn ngữ, font size, dark/light mode, ...
- Connect/Disconnect bàn phím ngoài
- Thay đổi kích thước cửa sổ (multi-window)

Khi **configuration change xảy ra**, **`Activity` bị RECREATE hoàn toàn**:

> `onPause()` -> `onStop()` -> `onDestroy()` -> `onCreate()` -> `onStart()` -> `onResume()`

Lí do: **Android cần load lại đúng `resource` cho configuration mới** (_layout-land, values-vi..._).

### 2.2. **Xử lý Configuration Change**

Có nhiều cách để xử lý configuration change:

- **Cách 1**: sử dụng [`ViewModel`](/docs/s11_mvvm/readme.md) (**recommended**)
- **Cách 2**: sử dụng `savedStateInstance` (_cho **UI state nhỏ**_)
- **Cách 3**: `android:configChanges` (hạn chế dùng):

  ```xml
  <!-- Tự xử lý configuration change, không recreate Activity -->
  <activity
      android:name=".VideoPlayerActivity"
      android:configChanges="orientation|screenSize|keyboardHidden" />
  ```

  khi này, cần **override `onConfigurationChanged(newConfig: Configuration)`** để xử lý:

  ```kotlin
  override fun onConfigurationChanged(newConfig: Configuration) {
      super.onConfigurationChanged(newConfig)
      // Tự xử lý UI thay đổi
      if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
          showFullscreenPlayer()
      } else {
          showNormalPlayer()
      }
  }
  ```

  chỉ phù hợp cho **Video player, game** — recreate sẽ mất **trạng thái phức tạp**

---

## 3. **Dark Mode**

Từ **Android 10 (API 29)**, Android giới thiệu **Dark Mode** chính thức

### 3.1. Enable Dark Mode

**Bước 1** — Khai báo `theme` hỗ trợ **dark mode**:

```xml
<!-- res/values/themes.xml -->
<style name="Theme.MyApp" parent="Theme.Material3.DayNight">
    <!-- DayNight tự động switch giữa light/dark -->
</style>
```

**Bước 2** — **Color resource** tự động theo mode:

- `res/values/colors.xml` — **Light** mode:
  ```xml
  <!-- res/values/colors.xml — Light mode -->
  <color name="background">#FFFFFF</color>
  <color name="text_primary">#000000</color>
  ```
- `res/values-night/colors.xml` — **Dark** mode
  ```xml
  <!-- res/values-night/colors.xml — Dark mode -->
  <color name="background">#121212</color>
  <color name="text_primary">#FFFFFF</color>
  ```

### 3.2. **Kiểm tra** và **set Dark Mode** bằng code

**Kiểm tra** dark mode hiện tại:

```kotlin
// Kiểm tra dark mode hiện tại
fun isDarkMode(context: Context): Boolean {
    return context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}
```

**Chủ động set** dark mode:

```kotlin
// Set dark mode bằng code
fun setDarkMode(isDark: Boolean) {
    AppCompatDelegate.setDefaultNightMode(
        if (isDark) AppCompatDelegate.MODE_NIGHT_YES
        else AppCompatDelegate.MODE_NIGHT_NO
    )
    // → Tự động recreate Activity để apply theme mới
}
```

Set dark mode theo **system setting**:

```kotlin
fun followSystemDarkMode() {
    AppCompatDelegate.setDefaultNightMode(
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    )
}
```

### 3.3. Lưu **dark mode preferences** (setting) với `DataStore`

```kotlin
// Trong Application class — setup DataStore
class MyApplication : Application() {
    val dataStore by preferencesDataStore(name = "settings")
}

// Repository
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
    }

    val isDarkMode: Flow<Boolean> = dataStore.data
        .map { it[KEY_DARK_MODE] ?: false }

    suspend fun setDarkMode(isDark: Boolean) {
        dataStore.edit { it[KEY_DARK_MODE] = isDark }
    }
}

// Trong MainActivity — apply dark mode trước khi setContentView
override fun onCreate(savedInstanceState: Bundle?) {
    // Apply dark mode TRƯỚC super.onCreate() để tránh flash
    val isDark = runBlocking {
        settingsRepository.isDarkMode.first()
    }
    AppCompatDelegate.setDefaultNightMode(
        if (isDark) AppCompatDelegate.MODE_NIGHT_YES
        else AppCompatDelegate.MODE_NIGHT_NO
    )

    super.onCreate(savedInstanceState)
    // ...
}
```
