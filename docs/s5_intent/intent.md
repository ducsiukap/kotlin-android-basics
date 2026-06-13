# **_Intent_**

## 1. **Intent**

`Intent` là một đối tượng **messaging — cơ chế giao tiếp** trung tâm của Android:

- `Intent` mô tả một **hành động cần thực hiện**, và
- `Android OS` chịu trách nhiệm **tìm component** phù hợp để **thực hiện hành động** đó.

**`Intent`** có thể được sử dụng để:

- (1) **Khởi chạy một Activity** khác
- (2) **Khởi chạy một Service** để thực hiện một tác vụ nền
- (3) **Gửi broadcast**

---

## 2. **_Explicit_** Intent

Đây là **thành phần cốt lõi nhất** của `Intent`:

- **Explicit** intents: là những `Intent` mà **chỉ định rõ ràng component đích** xử lý task bằng `class name`.
- **Implicit** intents: là những `Intent` mà **_chỉ khai báo_** action, **_không chỉ định_** component đích, để **Android tự tìm kiếm component phù hợp** để xử lý task.

**Note**:

- Bạn **không tự tạo Activity bằng `constructor`** như một object thông thường.
- Bạn **gửi Intent cho Android** để hệ điều hành **quản lý việc mở Activity, lifecycle và back stack**.

### 2.1.1. **Khởi tạo `Intent`**

**Explicit Intent** là `Intent` chỉ định rõ `Activity` cần mở.

```kotlin
// Tạo Intent từ Activity nguồn đến Activity đích
val intent = Intent(this, DetailActivity::class.java)
startActivity(intent)
```

`Intent(context, activity)` — constructor của Explicit Intent:

- `Context`: là context của Activity nguồn (thường là `this` - **Activity hiện tại**).`
- `Activity`: là class của **Activity đích** mà bạn muốn mở.

### 2.1.2. **Gửi Intent** cho Android:

```kotlin
startActivity(intent)
```

- Android kiểm tra Activity đích
- Android mở `DetailActivity`
- Android đưa Activity mới lên back stack

### 2.1.3. **Có thể truyền dữ liệu qua `Intent`** (_trước khi gửi đi_)

`Intent` có thể **mang theo data dưới dạng `key-value`** thông qua `Bundle` được đính kèm.

- Gửi data:

  ```kotlin
  val intent = Intent(this, DetailActivity::class.java)

  // Các kiểu primitive
  intent.putExtra("key_string",  "Hello")
  intent.putExtra("key_int",     42)
  intent.putExtra("key_boolean", true)
  intent.putExtra("key_float",   3.14f)
  intent.putExtra("key_long",    100L)

  // ArrayList
  intent.putStringArrayListExtra("key_list", arrayListOf("A", "B", "C"))
  intent.putIntegerArrayListExtra("key_int_list", arrayListOf(1, 2, 3))

  startActivity(intent)
  ```

- Nhận data ở Activity đích thông qua property: `intent`

  ```kotlin
  class DetailActivity : AppCompatActivity() {

      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          binding = ActivityDetailBinding.inflate(layoutInflater)
          setContentView(binding.root)

          // Đọc từ intent — LUÔN dùng intent (property của Activity)
          val name    = intent.getStringExtra("key_string") ?: ""
          val age     = intent.getIntExtra("key_int", 0)        // tham số 2 = default value
          val isAdmin = intent.getBooleanExtra("key_boolean", false)
          val price   = intent.getFloatExtra("key_float", 0f)

          binding.tvName.text = name
          binding.tvAge.text  = age.toString()
      }
  }
  ```

  > _**Lưu ý:** `getStringExtra()` trả về `String?` (**nullable**) — luôn xử lý null bằng `?: ""` hoặc `?.let`. Các kiểu primitive (`getIntExtra`, `getBooleanExtra`) có **default value** — không nullable._

### 2.1.4. **Truyền Object** qua `Intent` — `Parcelable`

Để **truyền object phức tạp qua `Intent`**, object phải `implement Parcelable` — cơ chế **serialization của Android**, nhanh hơn Java Serializable.

```kotlin
// Cần plugin trong build.gradle.kts:
// id("kotlin-parcelize")

@Parcelize
data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val imageUrl: String
) : Parcelable
```

> _Kotlin cung cấp annotation `@Parcelize` để **tự động generate `Parcelable` implementation**_

**Gửi và nhận object**:

```kotlin
// Gửi object
val product = Product("p001", "Áo thun", 150000.0, "https://...")
val intent = Intent(this, ProductDetailActivity::class.java)
intent.putExtra("key_product", product)
startActivity(intent)

// Nhận object
val product = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    intent.getParcelableExtra("key_product", Product::class.java)
} else {
    @Suppress("DEPRECATION")
    intent.getParcelableExtra("key_product")
}
```

### 2.1.5. Dùng `companion object` để quản lý `key`

Việc dùng **string literal** làm `key` trực tiếp **dễ gây lỗi typo và khó maintain**. Pattern chuẩn trong thực tế:

```kotlin
class ProductDetailActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_PRODUCT    = "extra_product"
        private const val EXTRA_PRODUCT_ID = "extra_product_id"

        // Factory method — tạo Intent với đúng data
        fun newIntent(context: Context, product: Product): Intent {
            return Intent(context, ProductDetailActivity::class.java).apply {
                putExtra(EXTRA_PRODUCT, product)
            }
        }

        fun newIntent(context: Context, productId: String): Intent {
            return Intent(context, ProductDetailActivity::class.java).apply {
                putExtra(EXTRA_PRODUCT_ID, productId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Đọc data — dùng key từ companion object
        val productId = intent.getStringExtra(EXTRA_PRODUCT_ID) ?: run {
            finish()   // không có data → đóng Activity
            return
        }

        loadProduct(productId)
    }
}
```

```kotlin
// Caller — gọi factory method thay vì tự tạo Intent
binding.btnViewDetail.setOnClickListener {
    startActivity(ProductDetailActivity.newIntent(this, product))
}
```

Lợi ích của pattern này:

- Key tập trung tại một chỗ — không bị typo
- Caller không cần biết key là gì
- Dễ refactor nếu cần thêm/bớt data

---

## 3. **_Implicit_** Intent

Implicit Intent **không chỉ đích danh component** — nó khai báo `action` và `data`, **Android OS tìm component có `IntentFilter` phù hợp**. Nếu nhiều app cùng xử lý được → hiện **App Chooser** để user chọn.

```
App của bạn                  Android OS              App khác
sendBroadcast/           →   Tìm IntentFilter    →   Browser, Maps,
startActivity(implicit)       phù hợp                Camera...
```

### 3.1. Các **implicit intent** phổ biến: [Readmore](https://developer.android.com/reference/android/content/Intent)

`Intent(action, data)` — constructor của Implicit Intent:

1. **`action`**: The general action to be performed, such as **ACTION_VIEW**, **ACTION_EDIT**, **ACTION_MAIN**, etc.
   - `Intent.ACTION_VIEW`: mở URL, file, địa điểm trên map...
   - `Intent.ACTION_DIAL`: mở dial pad với số đã điền sẵn, không gọi điên...
   - `Intent.ACTION_SEND`: chia sẻ text, hình ảnh...
   - `Intent.ACTION_SENDTO`: gửi email, SMS...
2. **`data`**: The data to operate on, such as a person record in the contacts database, expressed as a `Uri`.

#### 3.1.1. Mở `URL`

```kotlin
val url = "https://www.google.com"
val intent = Intent(
    Intent.ACTION_VIEW, Uri.parse(url)
)
startActivity(intent)
```

#### 3.1.2. Gọi điện

```kotlin
// Mở dial pad với số điền sẵn (không cần permission)
val intent = Intent(
    Intent.ACTION_DIAL,
    Uri.parse("tel:0123456789")
)
startActivity(intent)

// Gọi trực tiếp (cần CALL_PHONE permission)
val intent = Intent(
    Intent.ACTION_CALL,
    Uri.parse("tel:0123456789")
)
startActivity(intent)
```

#### 3.1.3. Mở `mail`

```kotlin
val intent = Intent(Intent.ACTION_SENDTO).apply {
    data = Uri.parse("mailto:")
    putExtra(Intent.EXTRA_EMAIL, arrayOf("support@example.com"))
    putExtra(Intent.EXTRA_SUBJECT, "Phản hồi từ app")
    putExtra(Intent.EXTRA_TEXT, "Nội dung email...")
}
startActivity(intent)
```

#### 3.1.4. Chia sẻ `text`

```kotlin
val intent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_TEXT, "Nội dung muốn chia sẻ")
    putExtra(Intent.EXTRA_TITLE, "Chia sẻ qua")   // title cho chooser
}
startActivity(Intent.createChooser(intent, "Chia sẻ qua"))
```

> _`Intent.createChooser()` tạo giao diện để người dùng chọn ứng dụng nhận dữ liệu._

#### 3.1.5. Mở **Google Map** với địa điểm

```kotlin
val lat = 21.0285
val lng = 105.8542
val label = "Hà Nội"
val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
val intent = Intent(Intent.ACTION_VIEW, uri)
intent.setPackage("com.google.android.apps.maps")   // force mở Maps
startActivity(intent)
```

#### 3.1.6. Mở **setting**

```kotlin
// Settings tổng quát
startActivity(Intent(Settings.ACTION_SETTINGS))

// Settings của app hiện tại
val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
    data = Uri.parse("package:$packageName")
}
startActivity(intent)

// Settings Wifi
startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
```

### 3.2. Xử lý trường hợp **không có App nào phù hợp**

Với **implicit intent**, không phải lúc nào thiết bị cũng có ứng dụng có khả năng xử lý yêu cầu.

> _Nếu không có app nào xử lý được Implicit Intent → **app crash** với `ActivityNotFoundException`_

Cần xử lý **kiểm tra** trước khi gửi với `resolveActivity()`:

```kotlin
val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))

// API 33+ dùng resolveActivity với ResolveInfoFlags
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    }
} else {
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    } else {
        Toast.makeText(this, "Không có app xử lý được", Toast.LENGTH_SHORT).show()
    }
}
```

Hoặc đơn giản hơn, handle với `try-catch`:

```kotlin
// Cách gọn hơn — dùng try-catch
try {
    startActivity(intent)
} catch (e: ActivityNotFoundException) {
    Toast.makeText(this, "Không tìm thấy app phù hợp", Toast.LENGTH_SHORT).show()
}
```

---

## 4. **Trạng thái** của Activity sau khi gửi Intent:

Giả sử, từ Activity `A`, ta gửi một `Intent` để mở Activity `B`:

```kotlin
startActivity(
    Intent(this, ActivityB::class.java)
)
```

Khi này, trạng thái của các activities:

- **Activity `A`**: `onPause()`
- **Activity `B`**: `onCreate()` → `onStart()` → `onResume()`
- Nếu **`B` che phủ toàn bộ màn hình**, `A` tiếp tục chuyển sang trạng thái `onStop()`

Khi back lại từ Activity `B` về `A`:

- **Activity `B`**: `onPause()`
- **Activity `A`**: `onRestart()` → `onStart()` → `onResume()`
- **Activity `B`**: `onStop()` → `onDestroy()`
