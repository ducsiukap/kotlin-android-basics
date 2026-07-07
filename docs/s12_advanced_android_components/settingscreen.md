# **Setting screen - `PreferenceFragmentCompat`**

## 1. **What** is the **`PreferenceFragmentCompat`**?

**`PreferenceFragmentCompat`** là **1 KHUNG (framework) CÓ SẴN** để:

- **Tự động đọc/ghi SharedPreferences** — KHÔNG cần code thủ công
- Tự động **hiển thị UI ĐÚNG loại** cho từng Preference
  (Switch, Dialog chọn, EditText Dialog...)
- Định nghĩa TOÀN BỘ màn hình Settings chỉ bằng **1 FILE XML**

---

## 2. Triển khai

### 2.1. **Setup** — add dependency

```gradle
// build.gradle (app level)
dependencies {
    implementation 'androidx.preference:preference-ktx:1.2.1'
}
```

### 2.2. Định nghĩa **Settings** bằng **XML** - `res/xml/preferences.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <PreferenceCategory app:title="Giao diện">

        <SwitchPreferenceCompat
            app:key="dark_mode"
            app:title="Chế độ tối"
            app:summary="Bật giao diện tối cho toàn bộ ứng dụng"
            app:defaultValue="false" />

        <ListPreference
            app:key="font_size"
            app:title="Cỡ chữ"
            app:entries="@array/font_size_entries"
            app:entryValues="@array/font_size_values"
            app:defaultValue="medium" />

    </PreferenceCategory>

    <PreferenceCategory app:title="Tài khoản">

        <EditTextPreference
            app:key="display_name"
            app:title="Tên hiển thị"
            app:summary="Tên hiện ra trong ứng dụng" />

        <Preference
            app:key="change_password"
            app:title="Đổi mật khẩu"
            app:fragment="com.example.contactapp.ui.settings.ChangePasswordFragment" />

    </PreferenceCategory>

    <PreferenceCategory app:title="Khác">

        <Preference
            app:key="app_version"
            app:title="Phiên bản ứng dụng"
            app:summary="1.0.0"
            app:enabled="false" />

    </PreferenceCategory>

</PreferenceScreen>
```

Các **thành phần**:

| Component                | Ý nghĩa                                                                                                                                  |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `PreferenceScreen`       | **ROOT CONTAINER**, đại diện cho 1 màn hình Settings                                                                                     |
| `PreferenceCategory`     | **Nhóm các Preference** liên quan, có thể hiển thị title phân cách                                                                       |
| `Preference` (base)      | **khối xây dựng cơ bản**, đại diện cho 1 setting đơn lẻ. Nếu ``"persist"`, nó có 1 cặp **key-value** tương ứng trong `SharedPreferences` |
| `SwitchPreferenceCompat` | **Preference dạng Switch** (bật/tắt)                                                                                                     |
| `ListPreference`         | **Preference dạng List** (chọn 1 trong nhiều giá trị)                                                                                    |
| `EditTextPreference`     | **Preference dạng EditText** (người dùng nhập text)                                                                                      |

Với `ListPreference`, bạn cần định nghĩa **2 mảng** trong `res/values/arrays.xml`:

```xml
<!-- res/values/arrays.xml — dữ liệu cho ListPreference -->
<resources>
    <string-array name="font_size_entries">
        <item>Nhỏ</item>
        <item>Vừa</item>
        <item>Lớn</item>
    </string-array>

    <string-array name="font_size_values">
        <item>small</item>
        <item>medium</item>
        <item>large</item>
    </string-array>
</resources>
```

trong đó:

- **entries**: là **danh sách hiển thị** cho người dùng
- **entryValues**: là **giá trị thực sự** được lưu trong `SharedPreferences`

> _Cần **TÁCH BIỆT** 2 thứ này — vì text hiển thị có thể ĐA NGÔN NGỮ nhưng giá trị lưu trữ PHẢI cố định._

### 2.3. Implemement `PreferenceFragmentCompat`:

```kotlin
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Nạp toàn bộ hierarchy từ file XML — TƯƠNG TỰ setContentView()
        // nhưng dành riêng cho Preference
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
```

trong đó:

- `onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)` là **lifecycle callback** duy nhất của `PreferenceFragmentCompat`, **BẮT BUỘC override** để nạp hierarchy từ file XML
- `setPreferencesFromResource(R.xml.preferences, rootKey)`: Đọc file XML, **TỰ ĐỘNG build ra toàn bộ UI** (Switch, List Dialog...) VÀ **tự động BIND với SharedPreferences**

Để gắn vào `Activity`:

```xml
<!-- activity_settings.xml -->
<FrameLayout
    android:id="@+id/settingsContainer"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

```kotlin
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settingsContainer, SettingsFragment())
                .commit()
        }
    }
}
```

---

## 3. **Read saved setting values**

`PreferenceFragmentCompat` lưu giá trị tại **`SharedPreferences` mặc định** của app. Vì vậy, có thể đọc từ bất cứ đâu:

```kotlin
// SharedPreferences MẶC ĐỊNH của app — TẤT CẢ Preference đều
// tự động lưu vào ĐÂY (KHÔNG cần chỉ định tên file riêng)
val prefs = PreferenceManager.getDefaultSharedPreferences(context)

val isDarkMode = prefs.getBoolean("dark_mode", false)
val fontSize = prefs.getString("font_size", "medium")
```

**Quan trọng**:

- Để **LƯU** giá trị, không cần code. `PreferenceFragmentCompat` sẽ **tự động lưu** khi người dùng thay đổi setting.
- Để **ĐỌC** giá trị, chỉ cần dùng `PreferenceManager.getDefaultSharedPreferences(context)` và đọc theo **key** tương ứng đã khai báo trong XML (_eg. `app:key="dark_mode"`_)

---

## 4. **Observe changes** — lắng nghe khi người dùng thay đổi setting

`SharedPreferences.OnSharedPreferenceChangeListener` cho phép **listener** lắng nghe khi người dùng thay đổi setting:

```kotlin
class SettingsFragment : PreferenceFragmentCompat(),
    // implement listener để lắng nghe
    // khi người dùng thay đổi setting
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }


    // register listener khi fragment hiển thị
    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences
            ?.registerOnSharedPreferenceChangeListener(this)
    }

    // unregister listener khi fragment bị ẩn
    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences
            ?.unregisterOnSharedPreferenceChangeListener(this)
    }

    // callback khi người dùng thay đổi setting
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "dark_mode" -> {
                val isDark = sharedPreferences?.getBoolean("dark_mode", false) ?: false
                // Áp dụng Dark Mode NGAY LẬP TỨC
                AppCompatDelegate.setDefaultNightMode(
                    if (isDark) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }
    }
}
```

---

## 5. Hiện **`summary`** cho `ListPreference`

`ListPreference` mặc định **CHỈ hiện title** cố định, không hiện giá trị đang chọn làm summary.

Giải pháp: dùng **`Preference.SummaryProvider`** để **tự động hiển thị summary** dựa trên giá trị đang chọn:

```kotlin
override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.preferences, rootKey)

    val fontSizePref = findPreference<ListPreference>("font_size")
    fontSizePref?.summaryProvider = Preference.SummaryProvider<ListPreference> { pref ->
        pref.entry  // "entry" (số ít) = TEXT hiển thị tương ứng
                     // với giá trị ĐANG được chọn
    }
}
```

- `findPreference<T>("key")`: tìm lại **Preference** cụ thể từ hierarchy đã nạp dựa theo **key** đã khai báo trong XML.
- `SummaryProvider`: Interface CÓ SẴN — **TỰ ĐỘNG cập nhật summary MỖI KHI giá trị thay đổi**, KHÔNG cần tự viết `onSharedPreferenceChangeListener` riêng chỉ để update summary

---

## 6. **Navigate to sub-screen**

Khi có **QUÁ NHIỀU Preference**, nên **tách thành nhiều `PreferenceFragmentCompat` riêng**, liên kết qua `app:fragment`.

```kotlin
<Preference
    app:key="change_password"
    app:title="Đổi mật khẩu"
    app:fragment="com.example.contactapp.ui.settings.ChangePasswordFragment" />
```

```kotlin
// Activity PHẢI implement callback này để việc điều hướng
// sub-screen hoạt động đúng (theo tài liệu chính thức)
class SettingsActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader, pref.fragment ?: return false
        )
        fragment.arguments = pref.extras

        supportFragmentManager.beginTransaction()
            .replace(R.id.settingsContainer, fragment)
            .addToBackStack(null)
            .commit()
        return true
    }
}
```
