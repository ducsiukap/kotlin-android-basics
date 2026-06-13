# **Intent Filter**

**Intent Filter (`<intent-filter>`)** trả lời câu hỏi: "**_Android dựa vào đâu để biết `Activity` nào có thể xử lý một `implicit intent`?_**"

Demo: [Activity nhận share text](/src/app/src/main/java/com/vduczz/androidkotlin/s5_intent/IntentFilterDemoActivity.kt)

---

## 1. Tổng quan

Khi App A yêu cầu mở website:

```kotlin
val intent =
    Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://example.com")
    )

startActivity(intent)
```

Khi này:

- **App `A`** không chỉ rõ mở browser nào, mà chỉ yêu cầu: **Hiển thị `URL` này**
- Trên điện thoại có nhiều **browser**: _Chrome, Firefox, Edge, ..._ => mỗi browser khai báo với Android **có khả năng xử lý `ACTION_VIEW`** thông qua `<intent-filter>` của mình.
- **Android** sẽ dựa vào `<intent-filter>` của các browser để **xác định** và **hiển thị** danh sách các browser có thể xử lý `ACTION_VIEW` => người dùng chọn browser nào để mở `URL`.

---

## 2. **Intent Filter `<intent-filter>`**

`<intent-filter>` là phần khai báo trong `AndroidManifest.xml` mô tả:

> _Component này có khả năng tiếp nhận những loại Intent nào?_

> _Theo tài liệu Android, `intent filter` khai báo **khả năng của component cha**: một **Activity** hoặc **Service** có thể làm gì, hoặc **BroadcastReceiver** có thể xử lý loại broadcast nào._

```xml
<activity
    android:name=".ShareReceiverActivity"
    android:exported="true">

    <intent-filter>
        <!-- Nhận share text từ app khác -->
        <action android:name="android.intent.action.SEND" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="text/plain" />
    </intent-filter>

    <intent-filter>
        <!-- Nhận share image -->
        <action android:name="android.intent.action.SEND" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="image/*" />
    </intent-filter>

</activity>
```

---

## 3. Các **thành phần quan trọng** của `<intent-filter>`

Có `3` thành phần chính, bao gồm: `<action>`, `<category>`, và `<data>`

| Thành phần   | Mô tả                                          | Ví dụ                                                                                               |
| ------------ | ---------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| `<action>`   | Mô tả **hành động** mà component có thể xử lý. | Ví dụ: `android.intent.action.SEND` cho phép nhận dữ liệu được chia sẻ từ app khác.                 |
| `<category>` | Mô tả **nhóm / bối cảnh** bổ sung của intent.  | Ví dụ: `android.intent.category.DEFAULT` cho phép component được chọn khi không có category cụ thể. |
| `<data>`     | Mô tả **dữ liệu** mà component có thể xử lý.   | Ví dụ: `android:mimeType="text/plain"` cho phép nhận dữ liệu văn bản.                               |

### 3.1. `<action>` — muốn làm gì?

`<action>` mô tả **hành động** mà component có thể xử lý. Đây là **phần quan trọng nhất của `<intent-filter>`**.

Một số `action` phổ biến:

| `<action>`                   | Ý nghĩa                       |
| ---------------------------- | ----------------------------- |
| `android.intent.action.MAIN` | **Entry point** chính của app |
| `android.intent.action.VIEW` | **Hiển thị** nội dung         |
| `android.intent.action.SEND` | **Gửi / Share** dữ liệu       |
| `android.intent.action.DIAL` | **Mở** ứng dụng gọi điện      |
| `android.intent.action.EDIT` | **Chỉnh sửa** dữ liệu         |

Example: Với **implicit intent**

```kotlin
val intent =
    Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://developer.android.com")
    )
```

Khi này:

- `action` = `ACTION_VIEW`
- `data` = `https://developer.android.com`

**Browser** có **filter phù hợp** nên Android có thể mở browser.

### 3.2. `<category>` — nhóm / bối cảnh bổ sung

**Category (`<category>`)** bổ sung thông tin về **bối cảnh** mà component được phép xử lý `Intent`.

Có `3` loại category chính: `DEFAULT`, `LAUNCHER`, và `BROWSABLE`.

#### 3.2.1. `category.DEFAULT`

```xml
<category android:name="android.intent.category.DEFAULT" />
```

Một Activity muốn **nhận implicit intent thông thường từ `startActivity()`** cần có category `DEFAULT` trong filter.

> _Android tự coi **Intent truyền vào `startActivity()`** là cần **khớp với `CATEGORY_DEFAULT`**_

Nếu **quên `DEFAULT`**, Activity có thể **không xuất hiện trong danh sách xử lý implicit intent** thông thường.

Example: share text

```xml
<intent-filter>
    <action android:name="android.intent.action.SEND" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:mimeType="text/plain" />
</intent-filter>
```

#### 3.2.2. `category.LAUNCHER`

```xml
<category android:name="android.intent.category.LAUNCHER" />
```

`LAUNCHER` dùng cho **Activity** xuất hiện trong launcher (_danh sách ứng dụng_)

- **Ý nghĩa**: Mỗi `Activiy` có `category.LAUNCHER` sẽ **hiển thị icon của nó trong launcher**, thường đi kèm với `action.MAIN` với ý nghĩa là **một entry point** của app
- **Yêu cầu**: Activity có `category.LAUNCHER` phải **được export ra ngoài (`android:exported="true"`)**
  > _Đơn giản vì **launcher là component bên ngoài app**, export là **yêu cầu bắt buộc** để launcher có thể truy cập và mở Activity._

#### 3.2.3. `category.BROWSABLE`

```xml
<category android:name="android.intent.category.BROWSABLE" />
```

`BROWSABLE` dùng khi **`Activity` có thể được mở từ browser hoặc liên kết web**

Example:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />

    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />

    <data
        android:scheme="https"
        android:host="example.com" />
</intent-filter>
```

Ý nghĩa: `Activity` này là **ứng viên xử lý** khi người dùng truy cập liên kết `https://example.com`.

### 3.3. `<data>` — xử lý loại dữ liệu nào?

`<data>` mô tả **dữ liệu** mà component có thể xử lý, thường là **URI** hoặc **MIME type**.

Một số ví dụ:

- **Activity** xử lý `plaintext`:

  ```xml
  <data android:mimeType="text/plain" />
  ```

- **Activity** xử lý `image`:

  ```xml
  <data android:mimeType="image/*" />
  ```

- **Activity** xử lý `URL`:

  ```xml
  <data
      android:scheme="https"
      android:host="example.com"
      android:pathPrefix="/products" />
  <!-- URL: https://example.com/products/... -->
  ```

---

Một **IntentFilter** có thể **khớp** dựa trên `action`, `category` và `data`. <br/>
`data` có thể được mô tả bằng **MIME type**, **scheme** hoặc **path**

---

## 4. `Activity` xử lý

Example: **đọc data được share vào**

```kotlin
class ShareReceiverActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareReceiverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handleIncomingIntent()
    }

    private fun handleIncomingIntent() {
        if (intent.action == Intent.ACTION_SEND) {
            when {
                intent.type == "text/plain" -> {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                    binding.tvSharedContent.text = sharedText
                }
                intent.type?.startsWith("image/") == true -> {
                    val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                    imageUri?.let {
                        Glide.with(this).load(it).into(binding.ivSharedImage)
                    }
                }
            }
        }
    }
}
```
