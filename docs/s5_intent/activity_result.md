# **Activity** for **_Result_**

## 1. Vấn đề giải quyết

**Activity Result** giải quyết vấn đề: _đôi khi bạn không chỉ mở Activity mới — bạn **cần Activity đó trả về kết quả**_

```
MainActivity
    ↓ mở màn hình chỉnh sửa tên
EditNameActivity
    ↓ người dùng nhập “An”
    ↓ nhấn Lưu
MainActivity nhận lại “An”
```

### **Cách cũ**: `startActivityForResult()` + `onActivityResult()` (**Deprecated**)

Android vẫn còn các API cũ như:

```kotlin
startActivityForResult(...)
onActivityResult(...)
```

nhưng tài liệu Android chính thức **khuyến nghị dùng `Activity Result APIs` từ `AndroidX`**.

> _Cách cũ dùng `startActivityForResult()` và `onActivityResult()` đã **deprecated từ API 29**._

API mới tách rõ ba bước:

- **`(1)` đăng ký `callback`**
- **`(2)` launch yêu cầu**
- **`(3)` xử lý kết quả** khi hệ thống trả về

### Tại sao **`startActivity()` không đủ**?

Giả sử:

```kotlin
startActivity(
    Intent(this, EditNameActivity::class.java)
)
```

Khi `EditNameActivity` kết thúc, `MainActivity` **không tự động nhận được dữ liệu** từ `EditNameActivity`.

Nếu `EditNameActivity` muốn trả giá trị, cần:

- `MainActivity` phải **đăng ký** một `callback` để nhận kết quả.
- Mở `EditNameActivity`
- `EditNameActivity` phải **tạo result `Intent`**, sau đó gọi `setResult()` để gửi dữ liệu về `MainActivity`.
- `EditNameActivity` phải gọi `finish()` để kết thúc và kích hoạt `callback` đã đăng ký trong `MainActivity`.
- `MainActivity` sẽ nhận được dữ liệu trong `callback` đã đăng ký.

---

## 2. **Activity Result API**

**Activity Result API** có `3` thành phần chính:

- `registerForActivityResult()`: đăng ký `callback` để nhận kết quả.
- `ActivityResultContract`: định nghĩa cách tạo `Intent` và xử lý kết quả.
- `ActivityResultLauncher`: dùng để `launch` yêu cầu.

Theo tài liệu Android, **`registerForActivityResult()`**:

- **nhận** một `ActivityResultContract` và `callback`
- trả về một `ActivityResultLauncher`.

Bạn dùng `launcher` để **bắt đầu quá trình tạo kết quả**

```kotlin
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Bước 1: Khai báo launcher — PHẢI khai báo trước onCreate
    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Bước 4: Xử lý kết quả trả về
        if (result.resultCode == Activity.RESULT_OK) {
            val updatedName = result.data?.getStringExtra("updated_name") ?: return@registerForActivityResult
            binding.tvUserName.text = updatedName
            Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
        } else {
            // User cancel hoặc có lỗi
            Toast.makeText(this, "Đã hủy", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEditProfile.setOnClickListener {
            // Bước 2: Launch Activity đích
            val intent = EditProfileActivity.newIntent(this, currentUser)
            editProfileLauncher.launch(intent)
        }
    }
}
```

Component đích trả kết quả:

```kotlin
class EditProfileActivity : AppCompatActivity() {

    companion object {
        fun newIntent(context: Context, user: User): Intent {
            return Intent(context, EditProfileActivity::class.java).apply {
                putExtra("extra_user", user)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSave.setOnClickListener {
            val updatedName = binding.etName.text.toString().trim()

            if (updatedName.isEmpty()) {
                binding.tilName.error = "Tên không được để trống"
                return@setOnClickListener
            }

            // Bước 3: Set kết quả và đóng Activity
            val resultIntent = Intent().apply {
                putExtra("updated_name", updatedName)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        binding.btnCancel.setOnClickListener {
            // Cancel — không trả data
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
}
```

> _**Note**: vì là **Explicit Intent** nên ở Activity đích **không cần khai báo `<intent-filter>`** trong `AndroidManifest.xml`._

### 2.1. built-in `ActivityResultContracts`

Android **cung cấp sẵn** nhiều `contract` phổ biến — không cần tự tạo `Intent`:

- **Chọn ảnh** từ gallery: `ActivityResultContracts.GetContent()`

  ```kotlin
  // Chọn ảnh từ gallery
  private val pickImageLauncher = registerForActivityResult(
      ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
      uri?.let {
          Glide.with(this).load(it).into(binding.ivAvatar)
      }
  }

  // trigger launcher
  binding.btnPickImage.setOnClickListener {
      pickImageLauncher.launch("image/*")   // MIME type
  }
  ```

- **Chụp ảnh** bằng camera: `ActivityResultContracts.TakePicturePreview()`

  ```kotlin
  // Chụp ảnh
  private val takePictureLauncher = registerForActivityResult(
      ActivityResultContracts.TakePicturePreview()
  ) { bitmap: Bitmap? ->
      bitmap?.let {
          binding.ivPhoto.setImageBitmap(it)
      }
  }
  ```

- **Request permission**: `ActivityResultContracts.RequestPermission()`

  ```kotlin

  // Request permission
  private val requestPermissionLauncher = registerForActivityResult(
      ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
      if (isGranted) {
          accessLocation()
      } else {
          showPermissionDeniedMessage()
      }
  }

  binding.btnRequestLocation.setOnClickListener {
      requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
  }

  // Request nhiều permission cùng lúc
  private val requestMultiplePermissionsLauncher = registerForActivityResult(
      ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions: Map<String, Boolean> ->
      val cameraGranted   = permissions[Manifest.permission.CAMERA] ?: false
      val storageGranted  = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false

      if (cameraGranted && storageGranted) {
          openCamera()
      }
  }
  ```

Tổng hợp:

| Contract                       | Input                  | Output                         | Dùng cho                                  |
| ------------------------------ | ---------------------- | ------------------------------ | ----------------------------------------- |
| `StartActivityForResult()`     | `Intent`               | `ActivityResult` (code + data) | Mở Activity và nhận kết quả (_tổng quát_) |
| `GetContent()`                 | **MIME type** `String` | `Uri?`                         | Chọn file (ảnh, video, v.v.)              |
| `GetMultipleContents()`        | **MIME type** `String` | `List<Uri>`                    | Chọn nhiều file                           |
| `OpenDocument()`               | Mảng MIME type         | `Uri?`                         | Mở file                                   |
| `CreateDocument()`             | Tên file               | `Uri?`                         | Tạo file mới                              |
| `RequestPermission()`          | Tên permission         | `Boolean`                      | Request một permission                    |
| `RequestMultiplePermissions()` | Mảng permission        | `Map<String, Boolean>`         | Request nhiều permission cùng lúc         |
| `TakePicture()`                | `Uri`                  | `Boolean`                      | Chụp ảnh và lưu vào URI                   |
| `TakePicturePreview()`         | Không có               | `Bitmap?`                      | Lấy preview nhỏ                           |

### 2.2. Callback `{result -> ...}`

```kotlin
{ result ->
    // xử lý dữ liệu trả về
}
```

`result` chứa **`2` thành phần quan trọng**:

- **`result.resultCode`**: là trạng thái trả về của request, gồm `Activity.RESULT_OK`, `Activity.RESULT_CANCELED`, hoặc custom code
- **`result.data`**: `Intent` chứa dữ liệu trả về (_nếu có_)

Có thể **custom result code**:

```kotlin
Activity.RESULT_OK       // = -1: thành công, có data
Activity.RESULT_CANCELED // = 0:  user cancel hoặc không có kết quả

// Custom result code — dùng khi cần nhiều hơn 2 trạng thái
companion object {
    const val RESULT_DELETED = 100
    const val RESULT_UPDATED = 101
}
```

- Tại nơi trả về (**Activity đích**):

  ```kotlin
  setResult(RESULT_UPDATED, resultIntent)
  ```

- Tại nơi nhận kết quả:

  ```kotlin
  if (result.resultCode == RESULT_UPDATED) {
      // xử lý kết quả cập nhật
  }
  ```

### 2.3. `launcher.launch()`

Khác `startActivity()`, `launcher.launch()` **không chỉ mở Activity mới mà còn kích hoạt callback đã đăng ký**.

Để trả về kết quả, Activity có thể:

- `setResult()` với `Intent` chứa dữ liệu
- sau đó **gọi `finish()`** để chủ động kết thúc và gửi dữ liệu về Activity gọi `launch()`.

> _**Note**: nếu ấn **Back**, Activity đích không gọi `setResult()`, và thường mặc định trả về `RESULT_CANCELED`. **Callback** vẫn có thể chạy_
