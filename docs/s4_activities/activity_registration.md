# **Register** an Activity in `AndroidManifest.xml`

## 1. Activity vs Manifest

`AndroidManifest.xml` là **“hợp đồng”** giữa **ứng dụng** và **Android OS**. Đây là nơi khai báo các component như `Activity`, `Service`, `Receiver` và `Provider`.

Một **`Activity` không tự động trở thành screen mà Android có thể mở**. Để OS nhận biết một `Activity`, nó phải được **đăng ký** (khai báo) trong `AndroidManifest.xml` bằng tag `<activity>`

> _Activity không được khai báo sẽ không được hệ thống nhìn thấy và **không thể chạy**._

Cấu trúc khai báo `activity` trong **Manifest**:

```
<manifest>
    └── <application>
            ├── <activity>
            ├── <activity>
            └── <activity>
```

Khai báo cơ bản:

```xml
<!-- Màn hình khởi động -->
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:label="@string/app_name"
    android:theme="@style/Theme.MyApp">

    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>

</activity>


<!-- Màn hình khác -->
<activity
        android:name=".DetailActivity"
        android:exported="false" />
```

---

## 2. Các thuộc tính quan trọng

### 2.1. `android:name` — tên class Activity

> _Dùng dấu `.` phía trước nghĩa là relative với `package` đã khai báo trong **Manifest**._

Hai cách viết tương đương:

```xml
android:name=".MainActivity"
android:name="com.example.myapp.MainActivity"
```

### 2.2. `android:exported`

`android:exported` là thuộc tính quan trọng liên quan đến bảo mật, nó trả lời câu hỏi: **Activity này có được phép `mở từ bên ngoài` app hay không?**

```xml
<!-- Activity Launcher
    -> Cho phép component bên ngoài app
        khởi chạy Activity này
-->
android:exported="true"

<!-- Internal Activity -> Chỉ dùng nội bộ trong app -->
android:exported="false"
```

> _Nếu một `Activity` có `intent-filter`, thì **bắt buộc phải khai báo `android:exported` tường minh** (API 31+)._

### 2.3. Activity **Khởi động**

```xml
<!-- launcher cần exported=true -->
<activity ...
    android:exported="true">

    <!-- khai báo activity là launcher -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

Trong đó:

- `android:name="android.intent.action.MAIN"`: nghĩa là Activity này là **entry point chính**.
- `android:name="android.intent.category.LAUNCHER"`: nghĩa là Activity này **xuất hiện trong launcher**, tức danh sách icon ứng dụng trên thiết bị.

> Có thể có nhiều Activity có `MAIN + LAUNCHER` với ý nghĩa: **app có thể xuất hiện với nhiều entry point**

Tuy nhiên, thường chỉ nên **có một Activity có `MAIN + LAUNCHER`**

### 2.4. `android:launchMode`

Khi một **Activity được mở**, `android:launchMode` sẽ quyết định Android sẽ **tạo instance mới** hay **sử dụng lại một instance đã tồn tại** trong back stack?

```xml
<activity
    android:name=".MainActivity"
    android:launchMode="singleTask"
    android:exported="true">
```

Android hiện có `5` launch mode:

- `standard` (_mặc định_): Mỗi lần mở sẽ tạo một instance mới.
- `singleTop`: Nếu instance nằm ở **top của back stack**, sẽ **`reuse`** instance đó, không tạo mới. Nếu instance nằm ở vị trí khác hoặc chưa tồn tại, sẽ tạo mới.
- `singleTask`: Nếu instance đã tồn tại ở **bất kỳ đâu** trong back stack, sẽ **reuse** instance đó và đưa nó lên top, không tạo mới. Đồng thời **`destroy` tất cả instance nằm trên** nó.
- `singleInstance`: Tương tự `singleTask`, nhưng instance này sẽ **luôn ở một task riêng biệt**, không có Activity nào khác cùng task.
- `singleInstancePerTask` (API 31+): Tương tự `singleInstance`, nhưng cho phép có một instance trong mỗi task.

**Handle reuse instance**: Khi một instance được reuse, Android sẽ gọi callback `onNewIntent()` để Activity có thể xử lý Intent mới:

- **Tạo instance mới** → `onCreate()`
- **Reuse instance** → `onNewIntent()`

```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)

    setIntent(intent)

    val productId = intent.getLongExtra("product_id", -1L)

    renderProduct(productId)
}
```

### 2.5. `android:screenOrientation` — Kiểm soát **hướng màn hình**

```xml
android:screenOrientation="portrait"      <!-- chỉ dọc -->
android:screenOrientation="landscape"     <!-- chỉ ngang -->
android:screenOrientation="fullSensor"    <!-- theo sensor thiết bị -->
android:screenOrientation="unspecified"   <!-- mặc định, hệ thống quyết định -->
```

### 2.6. `android:configChanges` — Tự xử lý **configuration change**

```xml
<activity
    android:name=".VideoPlayerActivity"
    android:configChanges="orientation|screenSize|keyboardHidden"
    android:exported="false" />
```

Khi khai báo `configChanges`, Android **không recreate Activity khi configuration thay đổi** — thay vào đó **gọi callback `onConfigurationChanged()`**:

```kotlin
override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        // Tự xử lý chuyển sang landscape
    } else {
        // Tự xử lý chuyển sang portrait
    }
}
```

> _**Khi nào dùng**: Video player, game — những **Activity mà recreate sẽ làm mất trạng thái phức tạp không thể lưu bằng `savedInstanceState`**. Trong hầu hết trường hợp thông thường, **không nên dùng** — dùng `ViewModel` để handle configuration change thay thế._

### 2.7. `android:windowSoftInputMode` — **Behavior bàn phím**

`android:windowSoftInputMode` kiểm soát **hành vi của layout khi bàn phím mở**, điều khiển 2 vấn đề độc lập:

- Bàn phím ảo có **hiển thị ngay khi Activity được focus** hay không?
  - `stateHidden` → **ẩn bàn phím** khi Activity mở
  - `stateVisible` → **hiện bàn phím** khi Activity mở
- Cửa sổ Activity có phản ứng như thế nào khi bàn phím chiếm không gian màn hình?
  - `adjustResize` → **resize** Activity khi bàn phím mở (layout co lại)
  - `adjustPan` → **pan** (dịch chuyển) layout lên khi bàn phím mở
  - `adjustNothing` → **không làm gì** (bàn phím che mất nội dung phía dưới)

```xml
android:windowSoftInputMode="adjustResize"
<!--
    adjustResize   → resize Activity khi bàn phím mở (layout co lại)
    adjustPan      → pan (dịch chuyển) layout lên khi bàn phím mở
    adjustNothing  → không làm gì
    stateHidden    → bàn phím ẩn khi Activity mở
    stateVisible   → bàn phím hiện khi Activity mở
-->
```

example:

```xml
<!-- Login/Register form: resize để không che mất Button -->
<activity
    android:name=".LoginActivity"
    android:windowSoftInputMode="adjustResize"
    android:exported="false" />

<!-- Chat screen: pan để EditText không bị che -->
<activity
    android:name=".ChatActivity"
    android:windowSoftInputMode="adjustPan"
    android:exported="false" />
```

### 2.8. `android:parentActivityName` — Hỗ trợ Up Navigation

```
┌──────────────────────────────┐
│ ←  Chi tiết sản phẩm         │
├──────────────────────────────┤
│                              │
│  Nội dung                    │
│                              │
└──────────────────────────────┘
```

`android:parentActivityName` cho phép khai báo **Activity cha** để hỗ trợ **Up Navigation** (nút back ở ActionBar), quyết định khi ấn nút `←` sẽ quay về Activity nào:

```xml
<activity
    android:name=".DetailActivity"
    android:parentActivityName=".MainActivity"
    android:exported="false">

    <!-- Cần thêm meta-data cho API < 16 -->
    <meta-data
        android:name="android.support.PARENT_ACTIVITY"
        android:value=".MainActivity" />

</activity>
```

**Khác với Back** luôn trở về Activity trước đó trong back stack, **Up Navigation** sẽ luôn quay về `parentActivity` đã khai báo, bất kể nó có nằm trong back stack hay không.

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityDetailBinding.inflate(layoutInflater)
    setContentView(binding.root)

    // Enable nút Up trên Toolbar
    setSupportActionBar(binding.toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
}

override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
        // Tương đương nhấn Up
        onBackPressedDispatcher.onBackPressed()
        return true
    }
    return super.onOptionsItemSelected(item)
}
```
