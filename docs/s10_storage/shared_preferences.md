# **Shared Preferences**

## 1. **What** is **`SharedPreferences`**?

**SharedPreferences** là `API` lưu trữ `key`-`value` đơn giản dưới dạng **file `XML` trong Internal Storage** của app. Được thiết kế cho **data nhỏ**, đơn giản — `settings`, `preferences`, `token`, `flag`, ...

Về bản chất, Android sẽ lưu các dữ liệu này vào một **file XML** nằm trong **thư mục nội bộ của ứng dụng** (`/data/data/tên_package/shared_prefs/`).

```
/data/data/com.example.app/shared_prefs/
└── app_settings.xml       ← file XML tự động tạo
```

Nội dung được `SharedPreferences` generate trong **file XML**:

```xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="is_dark_mode" value="true" />
    <string name="auth_token">eyJhbGciOiJIUzI1NiJ9...</string>
    <int name="user_id" value="42" />
    <long name="last_sync" value="1718123456789" />
</map>
```

> _**Note**: Google hiện tại đã khuyến khích **thay thế `SharedPreferences` bằng Jetpack `DataStore`** (sử dụng Kotlin Coroutines và Flow)._

Các `datatype` được hỗ trợ:

| dtype            | method                        |
| ---------------- | ----------------------------- |
| **String**       | `putString`/`getString`       |
| **Int**          | `putInt`/`getInt`             |
| **Boolean**      | `putBoolean`/`getBoolean`     |
| **Float**        | `putFloat`/`getFloat`         |
| **Long**         | `putLong`/`getLong`           |
| **Set\<String>** | `putStringSet`/`getStringSet` |

> _**Chỉ hỗ trợ primitive types** — không lưu được **object phức tạp** trực tiếp (phải **serialize thành `JSON`** String trước)._

---

## 2. **How** to use **`SharedPreferences`**?

### 2.1. Cách **lấy instance** của **`SharedPreferences`**

#### **Cách 1**: `getSharedPreferences` tạo/lấy file preference riêng, với tên cụ thể:

```kotlin
// Lấy SharedPreferences với tên cụ thể
// Nhiều nơi trong app dùng cùng tên → cùng file → cùng data
val prefs = context.getSharedPreferences(
    "app_settings",        // tên file
    Context.MODE_PRIVATE   // chỉ app này đọc được — luôn dùng MODE_PRIVATE
)
```

#### **Cách 2**: `getDefaultSharedPreferences` lấy **file preference mặc định** của app:

```kotlin
// File mặc định — tên = packageName + "_preferences"
val prefs = PreferenceManager.getDefaultSharedPreferences(context)
```

> Chỉ có 1 file mặc định toàn app

#### **Cách 3**: `Activity.getPreferences` lấy **file preference _riêng_ của Activity**:

```kotlin
// File riêng cho Activity này — ít dùng
// gắn liền với activity, không dùng chung với các Activity khác
val prefs = activity?.getPreferences(Context.MODE_PRIVATE)
```

#### **Tóm tắt**:

- `getSharedPreferences(name, mode)` → Phổ biến nhất, có thể tạo nhiều file, phân loại theo mục đích: `"user_prefs"`, `"app_settings"`...
- `getDefaultSharedPreferences(context)` → Khi chỉ cần **1 file duy nhất cho TOÀN APP**
- `getPreferences(mode)` → Khi chỉ cần **1 file duy nhất cho Activity**, hiếm dùng

### 2.2. **Read/Write** data into `SharedPreferences`

#### 2.2.1. **Ghi data - `Editor`**:

- **Cách 1**: `edit()` → `apply()` (**asynchronous**, recommended)

  ```kotlin
  val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

  // Cách 1: edit() + apply() — ASYNC, khuyến nghị
  prefs.edit()
      .putBoolean("is_dark_mode", true)
      .putString("language", "vi")
      .putInt("font_size", 16)
      .putLong("last_login", System.currentTimeMillis())
      .apply()   // ghi xuống file bất đồng bộ, không block Main Thread
  ```

- **Cách 2**: `edit()` → `commit()` (**synchronous, `block` Main Thread**)

  ```kotlin
  // Cách 2: edit() + commit() — SYNC, block Main Thread
  val success = prefs.edit()
      .putString("critical_data", "value")
      .commit()   // trả về Boolean — true nếu ghi thành công
                  // BLOCK Main Thread → chỉ dùng khi thực sự cần biết kết quả ngay
  ```

- **Cách 3**: `edit {}` - Kotlin DSL extension (KTX), **tự động gọi `apply()`**:

  ```kotlin
  // Cách 3: Kotlin DSL extension (KTX)
  prefs.edit {
      putBoolean("is_dark_mode", true)
      putString("language", "vi")
  }
  // edit { } tự động gọi apply()
  ```

#### 2.2.2. **Đọc data**:

```kotlin
val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

// Tham số 2: default value — trả về khi key chưa tồn tại
val isDarkMode = prefs.getBoolean("is_dark_mode", false)
val language   = prefs.getString("language", "en") ?: "en"
val fontSize   = prefs.getInt("font_size", 14)
val lastLogin  = prefs.getLong("last_login", 0L)

// Kiểm tra key có tồn tại không
val hasToken = prefs.contains("auth_token")

// Đọc tất cả entries
val allEntries: Map<String, *> = prefs.all
```

#### 2.2.3. **Xóa data**:

```kotlin
// Xóa một key cụ thể
prefs.edit {
    remove("auth_token")
}

// Xóa tất cả data trong file
prefs.edit {
    clear()
}
```

### 2.3. **Observe** changes in `SharedPreferences`

```kotlin
// Đăng ký listener — nhận callback khi bất kỳ value nào thay đổi
val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
    when (key) {
        "is_dark_mode" -> {
            val isDark = prefs.getBoolean(key, false)
            applyTheme(isDark)
        }
        "language" -> {
            val lang = prefs.getString(key, "en")
            applyLanguage(lang)
        }
    }
}

// Đăng ký
prefs.registerOnSharedPreferenceChangeListener(listener)

// Hủy đăng ký — BẮT BUỘC để tránh memory leak
// SharedPreferences giữ WeakReference đến listener
// nhưng vẫn nên unregister tường minh
prefs.unregisterOnSharedPreferenceChangeListener(listener)
```

---

## 3. **`Wrapper` class pattern**

Thay vì gọi `getSharedPreferences` và dùng **string `key`** trực tiếp khắp nơi — pattern chuẩn là tạo **wrapper class** tập trung logic:

```kotlin
class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(
        "app_settings",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_DARK_MODE   = "is_dark_mode"
        private const val KEY_LANGUAGE    = "language"
        private const val KEY_AUTH_TOKEN  = "auth_token"
        private const val KEY_USER_ID     = "user_id"
        private const val KEY_FONT_SIZE   = "font_size"
        private const val KEY_ONBOARDED   = "is_onboarded"
    }

    // Dark mode
    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit { putBoolean(KEY_DARK_MODE, value) }

    // Language
    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "vi") ?: "vi"
        set(value) = prefs.edit { putString(KEY_LANGUAGE, value) }

    // Auth token — nullable vì chưa login thì không có
    var authToken: String?
        get() = prefs.getString(KEY_AUTH_TOKEN, null)
        set(value) {
            prefs.edit {
                if (value != null) putString(KEY_AUTH_TOKEN, value)
                else remove(KEY_AUTH_TOKEN)
            }
        }

    // User ID
    var userId: Int
        get() = prefs.getInt(KEY_USER_ID, -1)
        set(value) = prefs.edit { putInt(KEY_USER_ID, value) }

    // Onboarding flag
    var isOnboarded: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDED, value) }

    // Logout — clear toàn bộ user data
    fun clearUserData() {
        prefs.edit {
            remove(KEY_AUTH_TOKEN)
            remove(KEY_USER_ID)
        }
    }

    // Clear tất cả
    fun clearAll() {
        prefs.edit { clear() }
    }
}
```

Dùng **wrapper class**:

```kotlin
val appPrefs = AppPreferences(context)

// Ghi
appPrefs.isDarkMode = true
appPrefs.authToken  = "eyJhbGciOiJIUzI1NiJ9..."
appPrefs.userId     = 42

// Đọc
if (appPrefs.isDarkMode) applyDarkTheme()
if (!appPrefs.isOnboarded) showOnboarding()
if (appPrefs.authToken == null) navigateToLogin()

// Logout
appPrefs.clearUserData()
```

### **Inject `AppPreferences`** qua `Application` class

Để không phải tạo `AppPreferences(context)` ở mọi nơi:

```kotlin
class MyApplication : Application() {

    // Khởi tạo một lần, dùng mọi nơi
    val appPreferences: AppPreferences by lazy {
        AppPreferences(this)
    }
}

// Trong Activity/Fragment
val prefs = (application as MyApplication).appPreferences
// hoặc
val prefs = (requireActivity().application as MyApplication).appPreferences
```

---

## 4. **Lưu `object` phức tạp bằng `JSON`**

`SharedPreferences` **không hỗ trợ object trực tiếp** — phải **serialize** thành **`JSON` String**:

```kotlin
// Data class
data class UserProfile(
    val id: Int,
    val name: String,
    val email: String,
    val avatarUrl: String
)

// Lưu object bằng Gson
fun saveUserProfile(profile: UserProfile) {
    val json = Gson().toJson(profile)
    prefs.edit { putString("user_profile", json) }
}

// Đọc object
fun getUserProfile(): UserProfile? {
    val json = prefs.getString("user_profile", null) ?: return null
    return try {
        Gson().fromJson(json, UserProfile::class.java)
    } catch (e: Exception) {
        null
    }
}
```

---

## 5. **Hạn chế** của `SharedPreferences`

- **Vấn đề 1**: `type-safety`
  → `getString("key", null)` trả về `String?` dù bạn biết chắc nó là `String`
- **Vấn đề 2**: không safe với Coroutines:
  - `apply()` là **`async`**, nhưng không có cách nào biết khi nào nó hoàn tất (**không có await**)
  - `commit()` là **`sync`** nhưng **block MAIN thread**
- **Vấn đề 3**: không có **error handling** → Không biết khi nào ghi thất bại (với `apply`)
- **Vấn đề 4**: không hỗ trợ `Flow`/**reactive**
- **Vấn đề 5**: không an toàn khi **concurrent access** từ nhiều thread
