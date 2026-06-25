# **`File` Storage**

## 1. **Android File System**

Android Storage **phân vùng storage** gồm `2` loại: **Internal Storage** và **External Storage**.

- **Internal Storage**: `private` hoàn toàn, chỉ app đọc được.
  > _ex: `data/data/com.example.app/files/`, `data/data/com.example.app/cache/`_
- **External Storage**: lại được phân thành `2` loại nhỏ hơn:
  - **Private External Storage**: `private` với app, nhưng nằm trên **External Storage**.
    > _ex: `sdcard/Android/data/com.example.app/files/`, `sdcard/Android/data/com.example.app/cache/`_
  - **Public External Storage**: `public` - **shared** với mọi app, chỉ cần **permission** để đọc được.
    > _ex: `sdcard/Download/`, `sdcard/Pictures/`, `sdcard/Music/`_

### 1.1. **Internal Storage**

**Đặc điểm**:

- Luôn **available**, không phụ thuộc SD card, ....
- **Private hoàn toàn**, app khác không đọc được, kể cả **`root`**.
- **Không cần permission** để đọc/ghi.
- **Bị xóa khi UNINSTALL app**.

> _Tuy nhiên, đổi lại, **Internal Storage** có **dung lượng nhỏ**, nên chỉ dùng để lưu **data nhỏ**._

#### **Files Directory** vs **Cache Directory**

- **Files Directory - `context.filesDir`**: dùng để lưu **data `quan trọng`**, cần **lưu trữ `lâu dài`**.
- **Cache Directory - `context.cacheDir`**: dùng để lưu **data tạm thời**, có thể bị **xóa bất cứ lúc nào**.

```kotlin
// Files Directory — data quan trọng, tồn tại lâu dài
context.filesDir
// → /data/data/com.example.myapp/files/
// Chỉ bị xóa khi uninstall

// Cache Directory — data tạm thời
context.cacheDir
// → /data/data/com.example.myapp/cache/
// Bị xóa khi:
//   - User xóa cache trong Settings
//   - Hệ thống thiếu storage → tự động dọn
//   - Uninstall app

// Thư mục con tùy chỉnh
val subDir = File(context.filesDir, "exports")
if (!subDir.exists()) subDir.mkdirs()
// → /data/data/com.example.app/files/exports/
```

#### **Đọc** / **Ghi** file trong **Internal Storage**

```kotlin
class InternalStorageHelper(private val context: Context) {

    // ── Ghi file ──────────────────────────────────────────────

    fun writeTextFile(fileName: String, content: String) {
        // File tự động tạo trong filesDir
        val file = File(context.filesDir, fileName)

        file.writeText(content, Charsets.UTF_8)
        // Hoặc dùng OutputStream nếu cần control nhiều hơn:
        // file.outputStream().use { it.write(content.toByteArray()) }
    }

    fun appendToFile(fileName: String, content: String) {
        val file = File(context.filesDir, fileName)
        file.appendText(content, Charsets.UTF_8)
    }

    // ── Đọc file ──────────────────────────────────────────────

    fun readTextFile(fileName: String): String? {
        val file = File(context.filesDir, fileName)

        if (!file.exists()) return null

        return file.readText(Charsets.UTF_8)
    }

    // ── Ghi vào subdirectory ──────────────────────────────────

    fun writeToSubDirectory(dirName: String, fileName: String, content: String) {
        val dir = File(context.filesDir, dirName)

        // Tạo directory nếu chưa tồn tại
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, fileName)
        file.writeText(content)
    }

    // ── Cache file ────────────────────────────────────────────

    fun writeCacheFile(fileName: String, data: ByteArray) {
        val file = File(context.cacheDir, fileName)
        file.writeBytes(data)
    }

    // ── Xóa file ──────────────────────────────────────────────

    fun deleteFile(fileName: String): Boolean {
        val file = File(context.filesDir, fileName)
        return file.delete()
    }

    // ── List files ────────────────────────────────────────────

    fun listFiles(): List<String> {
        return context.filesDir.listFiles()
            ?.map { it.name }
            ?: emptyList()
    }
}
```

`openFileOutput` - **legacy API**:

```kotlin
// Ghi
context.openFileOutput("data.txt", Context.MODE_PRIVATE).use { output ->
    output.write("Hello".toByteArray())
}

// Đọc
context.openFileInput("data.txt").use { input ->
    val content = input.readBytes().toString(Charsets.UTF_8)
}

// MODE_PRIVATE   → ghi đè nếu file tồn tại
// MODE_APPEND    → append vào cuối file
```

> _**API này chỉ làm việc với `filesDir`** — không linh hoạt bằng `File(context.filesDir, name)`. Code mới **nên dùng `File` trực tiếp**._

### 1.2. **External Storage**

#### **Private** external storage

```
/sdcard/Android/data/com.example.myapp/files/

/sdcard/Android/data/com.example.myapp/cache/
```

**Đặc điểm**:

- Dung lượng lớn hơn so với **Internal Storage**.
- **KHÔNG** cần **permission** để đọc/ghi.
- Bị xóa khi **UNINSTALL app**, giống Internal.
- **KHÔNG available** nếu SD card bị tháo.
- Trên một số thiết bị, user có thể đọc bằng **file manager**.

```kotlin
// Private External Files
context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
// → /sdcard/Android/data/com.example.myapp/files/Pictures/

context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
// → /sdcard/Android/data/com.example.myapp/files/Downloads/

context.getExternalFilesDir(null)
// → /sdcard/Android/data/com.example.myapp/files/

// Private External Cache
context.externalCacheDir
// → /sdcard/Android/data/com.example.myapp/cache/
```

Có thể kiểm tra **External Storage** có **available** hay không bằng:

```kotlin
fun isExternalStorageAvailable(): Boolean {
    return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
}

fun isExternalStorageReadable(): Boolean {
    val state = Environment.getExternalStorageState()
    return state == Environment.MEDIA_MOUNTED ||
           state == Environment.MEDIA_MOUNTED_READ_ONLY
}
```

#### **Public** external storage

Thay đổi về **permission** trong các version android:

- **`API < 29` (Android 9-)**:
  - Cần **permission `READ`/`WRITE_EXTERNAL_STORAGE`** để đọc/ghi.
  - Có thể đọc/ghi **TOÀN BỘ** `/sdcard/`
- **`API 29` (Android 10)** - **Scoped Storage** ra đời:
  - App chỉ **đọc/ghi** được trong thư mục của mình.
  - Dùng `MediaStore` API để truy cập file của app khác.
- **`API 30+` (Android 11+)** - **Scoped Storage** bắt buộc:
  - **Permission** `WRITE_EXTERNAL_STORAGE` bị **deprecated**, không còn tác dụng.
  - Phải dùng `MediaStore` API hoặc **SAF - Storage Access Framework**.

### 1.3. **`MediaStore` API - truy cập public Media**

```kotlin
class MediaStoreHelper(private val context: Context) {

    // ── Lưu ảnh vào thư viện ảnh ─────────────────────────────

    fun saveImageToGallery(bitmap: Bitmap, displayName: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return null

        context.contentResolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        }

        // API 29+ — đánh dấu file đã hoàn thành
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }

        return uri
    }

    // ── Query ảnh từ thư viện ─────────────────────────────────

    fun queryImages(): List<Uri> {
        val uris = mutableListOf<Uri>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                uris.add(uri)
            }
        }

        return uris
    }
}
```

---

## 2. **`FileProvider` - chia sẻ file giữa các app**

**`FileProvider`** là một **`ContentProvider`** đặc biệt, cho phép **chia sẻ file** giữa các app mà không cần **permission**.

Cụ thể, `FileProvider` expose **file nội bộ** của app ra ngoài qua `content://URI` thay vì `file://URI` kèm theo **permission tạm thời** được cấp cho app nhận.

Example: App muốn mở Camera, sau đó lưu ảnh về file của app:

- **Trước `API 24`**:

  ```kotlin
  val file = File(context.filesDir, "photo.jpg")
  val uri = Uri.fromFile(file)  // file:// URI
  intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
  // → Camera app KHÔNG có quyền đọc file:// URI của app khác
  // → FileUriExposedException từ API 24
  ```

- Cách đúng từ **API 24**:

  ```kotlin
  val uri = FileProvider.getUriForFile(context, authority, file)
  // → content:// URI
  // → FileProvider cấp quyền tạm thời cho Camera app đọc file
  ```

### 2.1. **Setup `FileProvider`**

**Bước 1**: khai báo `FileProvider` trong `AndroidManifest.xml`:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="com.example.myapp.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">

    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_provider_paths" />

</provider>
```

**Bước 2**: tạo file `res/xml/file_provider_paths.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>

    <!-- Internal files dir: context.filesDir -->
    <files-path
        name="internal_files"
        path="." />

    <!-- Internal cache dir: context.cacheDir -->
    <cache-path
        name="internal_cache"
        path="." />

    <!-- Private External files: context.getExternalFilesDir() -->
    <external-files-path
        name="external_files"
        path="." />

    <!-- Private External cache: context.externalCacheDir -->
    <external-cache-path
        name="external_cache"
        path="." />

    <!--
        name → tên alias (xuất hiện trong URI thay cho path thực)
        path → subdirectory trong thư mục đó ("." = tất cả)

        URI được tạo ra:
        content://com.example.myapp.fileprovider/internal_files/photo.jpg
        thay vì:
        /data/data/com.example.myapp/files/photo.jpg  ← ẩn path thực
    -->

</paths>
```

### 2.2. **Use `FileProvider`**

#### **usecase 1**: chụp ảnh bằng Camera

```kotlin
class CameraActivity : AppCompatActivity() {

    private lateinit var photoUri: Uri
    private lateinit var photoFile: File

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Ảnh đã được lưu vào photoFile
            displayPhoto(photoUri)
        }
    }

    private fun openCamera() {
        // Bước 1 — Tạo file để Camera ghi vào
        photoFile = createImageFile()

        // Bước 2 — Convert File → content:// URI qua FileProvider
        photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",  // Phải match authority trong Manifest
            photoFile
        )

        // Bước 3 — Launch Camera với URI
        takePictureLauncher.launch(photoUri)
        // Camera app nhận content:// URI
        // FileProvider tự cấp quyền WRITE cho Camera
        // Camera ghi ảnh vào file thông qua URI
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        val fileName = "PHOTO_${timestamp}.jpg"

        // Lưu vào cache — ảnh tạm thời trước khi user confirm
        return File(cacheDir, fileName)
    }

    private fun displayPhoto(uri: Uri) {
        // Dùng Glide hoặc load trực tiếp
        imageView.setImageURI(uri)
    }
}
```

#### **usecase 2**: share file với app khác

```kotlin
fun shareFile(file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(
        this,
        "${packageName}.fileprovider",
        file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)

        // Cấp quyền đọc tạm thời cho app nhận
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    startActivity(Intent.createChooser(shareIntent, "Chia sẻ file"))
}

// Dùng:
shareFile(File(filesDir, "report.pdf"), "application/pdf")
shareFile(File(cacheDir, "photo.jpg"), "image/jpeg")
```

#### **usecase 3**: mở file bằng app khác

```kotlin
fun openFileWithExternalApp(file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(
        this,
        "${packageName}.fileprovider",
        file
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    // Kiểm tra có app nào handle được không
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    } else {
        Toast.makeText(this, "Không có app nào mở được file này", Toast.LENGTH_SHORT).show()
    }
}
```
