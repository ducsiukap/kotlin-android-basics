# **`ContentObserver` & `notifyChange()`**

## 1. **Overview**

Khi **`data` trong Content Provider thay đổi**, các **component khác cần biết** để cập nhật UI.<br/>
**Android** giải quyết bằng **cơ chế `Observer` Pattern** tích hợp sẵn trong Content Provider

```
Provider thay đổi data
        ↓
notifyChange(uri)   ← Provider gọi để thông báo
        ↓
ContentResolver nhận tín hiệu
        ↓
Tất cả ContentObserver đã đăng ký uri này
→ onChange() được gọi
        ↓
Client reload data / cập nhật UI
```

---

## 2. **`notifyChange()`**

### 2.1. Vai trò

`contentResolver.notifyChange(uri, observer)` — **Provider** gọi hàm này để **phát tín hiệu "data tại URI này đã thay đổi"** đến toàn bộ `observer` đang lắng nghe.

```kotlin
// Trong ContentProvider sau mỗi write operation
override fun insert(uri: Uri, values: ContentValues?): Uri? {
    val id = db.insert(TABLE_NAME, null, values)

    // Phát tín hiệu: data tại uri này đã thay đổi
    context!!.contentResolver.notifyChange(uri, null)

    return ContentUris.withAppendedId(uri, id)
}

override fun update(...): Int {
    val rows = db.update(...)
    if (rows > 0)
        // Phát tín hiệu: data tại uri này đã thay đổi
        context!!.contentResolver.notifyChange(uri, null)
    return rows
}

override fun delete(...): Int {
    val rows = db.delete(...)
    if (rows > 0)
        // Phát tín hiệu: data tại uri này đã thay đổi
        context!!.contentResolver.notifyChange(uri, null)
    return rows
}
```

### 2.2. Các **`args` của `notifyChange()`**

```kotlin
contentResolver.notifyChange(
    uri,       // URI nào thay đổi
    observer,  // ContentObserver? — null nếu không muốn loại trừ observer nào
               // non-null → observer đó sẽ KHÔNG nhận thông báo (tránh self-notify)
    flags      // Int — API 24+, thường dùng ContentResolver.NOTIFY_UPDATE
               //        hoặc để mặc định
)
```

Thực tế phổ biến:

```kotlin
context!!.contentResolver.notifyChange(uri, null)
// null = thông báo cho TẤT CẢ observer, không loại trừ ai
```

### 2.3. **URI notify** ảnh hưởng tới URI con

```kotlin
// Provider notify URI cha
contentResolver.notifyChange(
    Uri.parse("content://com.example.app.provider/notes"),
    null
)

// Observer đăng ký với notifyForDescendants = true
// → Nhận notify khi URI cha hoặc BẤT KỲ URI CON nào thay đổi
// content://com.example.app.provider/notes/1 → NHẬN
// content://com.example.app.provider/notes/2 → NHẬN

// Observer đăng ký với notifyForDescendants = false
// → Chỉ nhận khi đúng URI đó thay đổi
// content://com.example.app.provider/notes/1 → KHÔNG nhận
```

---

## 3. **`setNotificationUri()`**

### 3.1. Vai trò

`cursor.setNotificationUri()` gắn **URI** vào `Cursor` — khi URI đó nhận `notifyChange()`, Cursor **tự động báo cho observer** biết để invalidate và reload:

```kotlin
override fun query(uri: Uri, ...): Cursor? {
    val cursor = db.query(...)

    // Gắn URI vào cursor
    // Khi URI này có notifyChange() → cursor tự notify observers của nó
    cursor?.setNotificationUri(context!!.contentResolver, uri)

    return cursor
}
```

### 3.2. Tại sao cần `setNotificationUri()`?

Nếu **không** gắn **URI** vào `cursor`: khi `notifyChange()` được gọi → `ContentObserver.onChange()` được gọi và cần **reload thủ công**.

> _Hoạt động bình thường nếu đang dùng `ContentObserver` trực tiếp_

Nếu **CÓ** gắn **URI** vào `cursor`: khi `notifyChange()` được gọi → `Cursor` **tự động biết** data cũ đã bị **stale** → `CursorAdapter`/`CursorLoader` **tự động reload**

> _Chủ yếu hữu ích khi dùng `CursorAdapter` (cách cũ). _

Với `Room` + `LiveData` / `Flow` → **ít dùng `setNotificationUri` hơn**

---

## 4. **`ContentObserver`**

### 4.1. Tạo `ContentObserver`

```kotlin
class NoteObserver(
    handler: Handler,
    private val onDataChanged: () -> Unit
) : ContentObserver(handler) {

    // onChange(selfChange) — API cũ
    override fun onChange(selfChange: Boolean) {
        onChange(selfChange, null)
    }

    // onChange(selfChange, uri) — API 16+, biết URI nào thay đổi
    override fun onChange(selfChange: Boolean, uri: Uri?) {
        // selfChange = true nếu chính observer này trigger notifyChange
        // uri = URI cụ thể đã thay đổi (null nếu không rõ)
        onDataChanged()
    }
}
```

`handler` xác định **thread** nào mà `onChange()` được gọi. <br/>
Nếu **null** → `onChange()` được gọi trên **main thread**.

```kotlin
// onChange() chạy trên Main Thread — để update UI trực tiếp
val handler = Handler(Looper.getMainLooper())

// onChange() chạy trên thread hiện tại khi đăng ký
val handler = Handler(Looper.myLooper()!!)
```

### 4.2. **Register** && **Unregister** `ContentObserver`

```kotlin
class NotesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotesBinding
    private lateinit var noteObserver: NoteObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        noteObserver = NoteObserver(
            Handler(Looper.getMainLooper())
        ) {
            // Callback này chạy trên Main Thread
            loadNotes()
        }
    }

    override fun onStart() {
        super.onStart()
        contentResolver.registerContentObserver(
            NoteContract.Notes.CONTENT_URI,   // URI cần lắng nghe
            true,                              // notifyForDescendants
            noteObserver
        )
        loadNotes()
    }

    override fun onStop() {
        super.onStop()
        contentResolver.unregisterContentObserver(noteObserver)
    }

    private fun loadNotes() {
        // reload data từ Provider
    }
}
```

`notifyForDescendants`: **true** / **false** ?

```kotlin
contentResolver.registerContentObserver(
    NoteContract.Notes.CONTENT_URI,
    true,   // true: lắng nghe URI này VÀ tất cả URI con
    observer
)
```

Khi này, với **URI đăng ký**: `content://com.example.app.provider/notes`

- `notifyForDescendants = true`:
  ```
  notifyChange(content://.../notes)     → onChange() ĐƯỢC GỌI
  notifyChange(content://.../notes/1)   → onChange() ĐƯỢC GỌI
  notifyChange(content://.../notes/42)  → onChange() ĐƯỢC GỌI
  ```
- `notifyForDescendants = false`:
  ```
  notifyChange(content://.../notes) → onChange() ĐƯỢC GỌI
  notifyChange(content://.../notes/1) → onChange() KHÔNG được gọi
  ```

**Quy tắc chọn**:

- **Chọn `true`** → Muốn biết **bất kỳ thay đổi** nào trong "bộ sưu tập"<br/>
  _Ví dụ: List screen cần reload khi bất kỳ note nào thay đổi_
- **Chọn `false`** → Chỉ **quan tâm đúng URI đó**<br/>
  _Ví dụ: Detail screen chỉ cần reload khi đúng note đó thay đổi_
