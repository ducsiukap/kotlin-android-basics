# **`built-in` Content Providers**

## 1. `ContactsContract` - hệ thống quản lí **danh bạ**

Cấu trúc **`3` tầng dữ liệu**:

- **`ContactsContract.Data`**: là tầng **thấp nhất**, lưu trữ tất cả các chi tiết nhỏ lẻ **dưới dạng các dòng - rows**, mỗi dòng lưu **một loại thông tin** như tên, số điện thoại, email,... của một contact.
  > _**Loại dữ liệu** được xác định bởi cột `MIMETYPE` trong mỗi dòng, ví dụ: `vnd.android.cursor.item/phone_v2` cho số điện thoại, `vnd.android.cursor.item/email_v2` cho email,..._
- **`ContactsContract.RawContacts`**: là tầng **trung gian**, đại diện cho **một contact cụ thể** gắn liền với **một loại tài khoản**, ví dụ cùng một `contact` nhưng có thể có nhiều `RawContact` với ý nghĩa khác nhau như _đồng bộ từ Google Account, danh bạ trên SIM, ..._.
- **`ContactsContract.Contacts`**: là tầng **cao nhất**, đại diện cho **một contact tổng thể**, có thể có nhiều `RawContact` khác nhau nhưng cùng **liên kết với một `contact` duy nhất**.
  > _**Lưu ý**: tầng `ContactsContract.Contacts` chỉ lưu trữ **các thông tin tổng quan** như `tên`, `avatar`, số lượng số điện thoại, `email`,... chứ **KHÔNG lưu chi tiết** từng loại thông tin._

### **Triển khai**

#### **Bước 1**: Thêm quyền truy cập danh bạ vào `AndroidManifest.xml`

```xml
<!-- READ_CONTACTS : quyền đọc danh bạ -->
<uses-permission android:name="android.permission.READ_CONTACTS" />
```

#### **Bước 2**: Triển khai đọc danh bạ trong `Activity`: check permission, query `ContactsContract`, đọc dữ liệu từ `Cursor` trả về.

```kotlin
class ContactsActivity : AppCompatActivity() {

    private fun readContacts() {

        // Bước 1 — Kiểm tra permission
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 100)
            return
        }

        // Bước 2 — Query ContactsProvider
        //  + uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI -> tầng Data, lấy ra các số điện thoại
        //  + uri = ContactsContract.Contacts.CONTENT_URI -> tầng Contacts,
        //      có thể dùng hasPhoneNumber  ở result
        //      để kiểm tra xem contact có số điện thoại hay không
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )

        // Bước 3 — Đọc Cursor
        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(0)
                val phone = it.getString(1)
                Log.d("Contacts", "$name: $phone")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            readContacts()
        }
    }
}
```

---

## 2. **`MediaStore` - hệ thống quản lí `media` (ảnh, video, audio)**

Các `URI` phân cấp theo **loại media**:

- `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`: Kho **ảnh** trên bộ nhớ ngoài.
- `MediaStore.Video.Media.EXTERNAL_CONTENT_URI`: Kho **video**.
- `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`: Kho **nhạc**/**file audio**, ghi âm, ...
- `MediaStore.Downloads.EXTERNAL_CONTENT_URI`: Thư mục **Downloads** (Từ **Android 10 `API 29+`**).
- `MediaStore.Files`: tất cả files (**`API 29+`**).

### Triển khai:

#### **Bước 1**: Thêm quyền truy cập media vào `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

#### **Bước 2**: Triển khai đọc media trong `Activity`: check permission, query `MediaStore`, đọc dữ liệu từ `Cursor` trả về.

```kotlin
import android.content.ContentUris
import android.provider.MediaStore
import android.net.Uri

class MediaReader {
    fun fetchLatestImages(context: Context) {
        // uri
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        // projection: các cột dữ liệu cần lấy ra
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE
        )

        // sort-order: sắp xếp theo ngày thêm vào giảm dần
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        // query(uri, projection, selection, selectionArgs, sortOrder)
        context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor -> // handle result
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

            // iterate through the cursor to get image details
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)

                // Hợp nhất ID thành một URI chuẩn để hiển thị lên ImageView (Glide/Coil)
                val contentUri: Uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                println("Image Uri: $contentUri - Name: $name")
            }
        }
    }
}
```

---

## 3. **`Telephony` / `Calendar` providers** (SMS, Call Log, Calendar)

### **`Telephony`**: quản lí **SMS**, **MMS** (tin nhắn đi và đến thiết bị).

- **URI**:
  - **Hộp thư đi**: `content://sms/inbox` or `Telephony.Sms.CONTENT_URI` (SMS)
  - **Hộp thư đến**: `content://sms/sent` or `Telephony.Mms.CONTENT_URI` (MMS).
- Ràng buộc: Từ **Android 4.4** trở đi, chỉ có **app được người dùng chọn** làm **Default SMS App** mới có **quyền Ghi/Xóa (`insert`, `delete`)** vào Provider này.
  > _Các app thường chỉ có quyền đọc nếu được cấp quyền `READ_SMS`._

### **`CallLog`**: quản lí **lịch sử cuộc gọi**.

- **URI**: `content://call_log/calls` or `android.provider.CallLog.Calls.CONTENT_URI`.
- **Permission**: `android.permission.READ_CALL_LOG` (đọc lịch sử cuộc gọi).

### **`CalendarContract`**: quản lí **sự kiện/lịch**.

Cho phép ứng dụng **đọc** hoặc **chèn lịch hẹn** vào lịch của hệ thống.

**URI** chính:

- `CalendarContract.Events.CONTENT_URI` (Quản lý các **Event** cụ thể)
- `CalendarContract.Calendars.CONTENT_URI` (Quản lý các **tài khoản lịch**).

---

## 4. **Note**: chạy `query` trên **Backend thread**

Query **`ContentProvider`** có thể **tốn thời gian** — đặc biệt khi danh bạ hoặc thư viện ảnh lớn. **KHÔNG được chạy trên Main Thread**.

Pattern đúng là sử dụng kết hợp `ViewModel` + `Coroutine` (+ `LiveData`) để query trên **Background Thread** ( và update UI trên **Main Thread**).

```kotlin
class ContactsViewModel : ViewModel() {

    private val _contacts = MutableLiveData<List<Contact>>()
    val contacts: LiveData<List<Contact>> = _contacts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadContacts(contentResolver: ContentResolver) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = withContext(Dispatchers.IO) {
                // Query chạy trên IO thread
                queryContacts(contentResolver)
            }

            _contacts.value = result
            _isLoading.value = false
        }
    }

    private fun queryContacts(resolver: ContentResolver): List<Contact> {
        val contacts = mutableListOf<Contact>()

        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
            ),
            "${ContactsContract.Contacts.HAS_PHONE_NUMBER} = ?",
            arrayOf("1"),
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val name = cursor.getString(1) ?: "Unknown"
                contacts.add(Contact(id, name))
            }
        }

        return contacts
    }
}
```

**UI observer `ViewModel`**:

```kotlin
// Fragment
class ContactsFragment : Fragment() {

    private val viewModel: ContactsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.contacts.observe(viewLifecycleOwner) { contacts ->
            adapter.submitList(contacts)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility =
                if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.loadContacts(requireContext().contentResolver)
    }
}
```
