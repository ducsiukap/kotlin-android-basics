# **Implement `ContentProvider`**

## 1. **When** need to implement `ContentProvider`?

Trong thực tế, **ít khi cần tự tạo `ContentProvider`** — chỉ cần thiết trong một số trường hợp:

- Muôn **expose data** cho app khác truy cập.
- Kết hợp với `SyncAdapter` để **sync data** với server.
- Muốn **expose data theo interface chuẩn** (_tương tự các system providers như `ContactsContract`, `MediaStore`, ..._) để các **widget**, **shortcut**, ... có thể truy cập.
- Kết hợp với `SearchManager` để làm **search suggestions**

Trong trường hợp **chỉ dùng nội bộ app**, **`Room Database`** hoặc **`SharedPreferences`**, ... là đủ, không cần `ContentProvider`.

---

## 2. **Structure** of `ContentProvider`

Tạo **`ContentProvider`** cần 4 bước:

- **Bước 1**: định nghĩa `URI` và `UriMatcher` để **xác định mapping `uri` <-> `data`** cần truy cập.
- **Bước 2**: tạo class `extends ContentProvider` và **override 6 method** cần thiết, implement **CRUD operations** trong các method override.
- **Bước 3**: khai báo `ContentProvider` trong `AndroidManifest.xml` để **Android biết** về `ContentProvider` này.

---

## 3. Usecase: **Note-taking provider**

Quy ước: bảng `notes(id, title, content, created_at)`

### **Bước 1**. tạo **`NoteContract` class** để định nghĩa **URI** & **table structure**:

#### **Định nghĩa `uri`**

```kotlin
// NoteContract.kt
object NoteContract {

    // Authority — package name của app, phải unique toàn hệ thống
    const val AUTHORITY = "com.example.myapp.provider"

    // Base URI
    val BASE_URI: Uri = Uri.parse("content://$AUTHORITY")

    // Table: notes
    object Notes {
        const val TABLE_NAME  = "notes"
        val CONTENT_URI: Uri  = Uri.withAppendedPath(BASE_URI, TABLE_NAME)
        // → content://com.example.myapp.provider/notes

        // MIME types
        const val CONTENT_TYPE      = "vnd.android.cursor.dir/vnd.$AUTHORITY.notes"   // nhiều rows
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.$AUTHORITY.notes"  // 1 row

        // Columns
        const val _ID        = "_id"
        const val TITLE      = "title"
        const val CONTENT    = "content"
        const val CREATED_AT = "created_at"
    }
}
```

#### **Addition**: `DBHelper` class để tạo **table**:

```kotlin
// NoteDatabase.kt
class NoteDatabase(context: Context) : SQLiteOpenHelper(
    context,
    "notes.db",
    null,
    1   // version
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE ${NoteContract.Notes.TABLE_NAME} (
                ${NoteContract.Notes._ID}        INTEGER PRIMARY KEY AUTOINCREMENT,
                ${NoteContract.Notes.TITLE}      TEXT NOT NULL,
                ${NoteContract.Notes.CONTENT}    TEXT,
                ${NoteContract.Notes.CREATED_AT} INTEGER NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS ${NoteContract.Notes.TABLE_NAME}")
        onCreate(db)
    }
}
```

### **Bước 2**. tạo **`NoteProvider` class** extends `ContentProvider`:

#### Tạo `UriMatcher` để map **URI** → **code**:

```kotlin
// Trong NoteProvider.kt
companion object {
    // Mã định danh cho từng loại URI
    private const val NOTES      = 1   // content://authority/notes       → nhiều rows
    private const val NOTE_BY_ID = 2   // content://authority/notes/{id}  → 1 row

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(NoteContract.AUTHORITY, NoteContract.Notes.TABLE_NAME, NOTES)
        addURI(NoteContract.AUTHORITY, "${NoteContract.Notes.TABLE_NAME}/#", NOTE_BY_ID)
        // # → wildcard khớp với số nguyên (ID)
        // * → wildcard khớp với chuỗi bất kỳ
    }
}
```

Các URI có thể **request** → `uriMatcher.match()` trả về

- `content://com.example.myapp.provider/notes` → **NOTES** (1)
- `content://com.example.myapp.provider/notes/5` → **NOTE_BY_ID** (2)
- `content://com.example.myapp.provider/unknown` → **NO_MATCH** (-1)

#### **Implement `ContentProvider`**:

```kotlin
class NoteProvider : ContentProvider() {

    private lateinit var database: NoteDatabase

    companion object {
        private const val NOTES      = 1
        private const val NOTE_BY_ID = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(NoteContract.AUTHORITY, NoteContract.Notes.TABLE_NAME, NOTES)
            addURI(NoteContract.AUTHORITY, "${NoteContract.Notes.TABLE_NAME}/#", NOTE_BY_ID)
        }
    }

    // ── Bắt buộc override ────────────────────────────

    override fun onCreate(): Boolean {
        // Khởi tạo database
        // context!! vì onCreate() luôn được gọi sau khi Provider được attach
        database = NoteDatabase(context!!)
        return true   // true = khởi tạo thành công
    }

    override fun getType(uri: Uri): String {
        // Trả về MIME type tương ứng với URI
        return when (uriMatcher.match(uri)) {
            NOTES      -> NoteContract.Notes.CONTENT_TYPE        // nhiều rows
            NOTE_BY_ID -> NoteContract.Notes.CONTENT_ITEM_TYPE   // 1 row
            else       -> throw IllegalArgumentException("URI không hợp lệ: $uri")
        }
    }

    // ── CRUD ─────────────────────────────────────────

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val db = database.readableDatabase

        val cursor = when (uriMatcher.match(uri)) {
            NOTES -> {
                // Query toàn bộ bảng
                db.query(
                    NoteContract.Notes.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder ?: "${NoteContract.Notes.CREATED_AT} DESC"
                )
            }
            NOTE_BY_ID -> {
                // Query 1 row theo ID từ URI
                val id = ContentUris.parseId(uri)
                db.query(
                    NoteContract.Notes.TABLE_NAME,
                    projection,
                    "${NoteContract.Notes._ID} = ?",
                    arrayOf(id.toString()),
                    null,
                    null,
                    null
                )
            }
            else -> throw IllegalArgumentException("URI không hợp lệ: $uri")
        }

        // Notify observers khi data thay đổi
        cursor?.setNotificationUri(context!!.contentResolver, uri)

        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val db = database.writableDatabase

        return when (uriMatcher.match(uri)) {
            NOTES -> {
                val id = db.insert(NoteContract.Notes.TABLE_NAME, null, values)
                if (id == -1L) throw SQLException("Không thể insert vào $uri")

                // Notify observers
                context!!.contentResolver.notifyChange(uri, null)

                // Trả về URI của record vừa tạo
                ContentUris.withAppendedId(uri, id)
            }
            else -> throw IllegalArgumentException("URI không hợp lệ cho insert: $uri")
        }
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val db = database.writableDatabase

        val rowsUpdated = when (uriMatcher.match(uri)) {
            NOTES -> {
                db.update(
                    NoteContract.Notes.TABLE_NAME,
                    values,
                    selection,
                    selectionArgs
                )
            }
            NOTE_BY_ID -> {
                val id = ContentUris.parseId(uri)
                db.update(
                    NoteContract.Notes.TABLE_NAME,
                    values,
                    "${NoteContract.Notes._ID} = ?",
                    arrayOf(id.toString())
                )
            }
            else -> throw IllegalArgumentException("URI không hợp lệ cho update: $uri")
        }

        if (rowsUpdated > 0) {
            context!!.contentResolver.notifyChange(uri, null)
        }

        return rowsUpdated
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val db = database.writableDatabase

        val rowsDeleted = when (uriMatcher.match(uri)) {
            NOTES -> {
                db.delete(
                    NoteContract.Notes.TABLE_NAME,
                    selection,
                    selectionArgs
                )
            }
            NOTE_BY_ID -> {
                val id = ContentUris.parseId(uri)
                db.delete(
                    NoteContract.Notes.TABLE_NAME,
                    "${NoteContract.Notes._ID} = ?",
                    arrayOf(id.toString())
                )
            }
            else -> throw IllegalArgumentException("URI không hợp lệ cho delete: $uri")
        }

        if (rowsDeleted > 0) {
            context!!.contentResolver.notifyChange(uri, null)
        }

        return rowsDeleted
    }
}
```

#### **Bước 3**. khai báo `ContentProvider` trong `AndroidManifest.xml`:

```xml
<application ...>

    <provider
        android:name=".provider.NoteProvider"
        android:authorities="com.example.myapp.provider"
        android:exported="false"   <!-- false: chỉ dùng nội bộ trong app -->
        android:enabled="true" />

    <!-- Nếu muốn app khác truy cập được -->
    <provider
        android:name=".provider.NoteProvider"
        android:authorities="com.example.myapp.provider"
        android:exported="true"
        android:readPermission="com.example.myapp.READ_NOTES"   <!-- permission để đọc -->
        android:writePermission="com.example.myapp.WRITE_NOTES" <!-- permission để ghi -->
        android:enabled="true" />

    <!-- Khai báo custom permission -->
    <permission
        android:name="com.example.myapp.READ_NOTES"
        android:protectionLevel="normal" />

    <permission
        android:name="com.example.myapp.WRITE_NOTES"
        android:protectionLevel="normal" />

</application>
```

---

## 4. Dùng `ContentProvider` từ app khác:

Sau khi **Provider** đã setup, client (`Activity`/`Fragment`) dùng bình thường qua `ContentResolver`:

```kotlin
// INSERT — thêm note mới
private fun insertNote(title: String, content: String) {
    val values = ContentValues().apply {
        put(NoteContract.Notes.TITLE,      title)
        put(NoteContract.Notes.CONTENT,    content)
        put(NoteContract.Notes.CREATED_AT, System.currentTimeMillis())
    }

    val newUri = contentResolver.insert(NoteContract.Notes.CONTENT_URI, values)
    // newUri = content://com.example.myapp.provider/notes/1
}

// QUERY — đọc tất cả notes
private fun loadNotes(): List<Note> {
    val notes = mutableListOf<Note>()

    contentResolver.query(
        NoteContract.Notes.CONTENT_URI,
        null,   // null = lấy tất cả cột
        null,
        null,
        "${NoteContract.Notes.CREATED_AT} DESC"
    )?.use { cursor ->
        while (cursor.moveToNext()) {
            val id      = cursor.getLong(cursor.getColumnIndexOrThrow(NoteContract.Notes._ID))
            val title   = cursor.getString(cursor.getColumnIndexOrThrow(NoteContract.Notes.TITLE))
            val content = cursor.getString(cursor.getColumnIndexOrThrow(NoteContract.Notes.CONTENT))
            notes.add(Note(id, title, content))
        }
    }

    return notes
}

// QUERY — đọc 1 note theo ID
private fun getNoteById(noteId: Long): Note? {
    val uri = ContentUris.withAppendedId(NoteContract.Notes.CONTENT_URI, noteId)

    return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val id      = cursor.getLong(cursor.getColumnIndexOrThrow(NoteContract.Notes._ID))
            val title   = cursor.getString(cursor.getColumnIndexOrThrow(NoteContract.Notes.TITLE))
            val content = cursor.getString(cursor.getColumnIndexOrThrow(NoteContract.Notes.CONTENT))
            Note(id, title, content)
        } else null
    }
}

// UPDATE — cập nhật note
private fun updateNote(noteId: Long, newTitle: String, newContent: String) {
    val uri = ContentUris.withAppendedId(NoteContract.Notes.CONTENT_URI, noteId)

    val values = ContentValues().apply {
        put(NoteContract.Notes.TITLE,   newTitle)
        put(NoteContract.Notes.CONTENT, newContent)
    }

    val rowsUpdated = contentResolver.update(uri, values, null, null)
    Log.d("NoteClient", "Updated $rowsUpdated rows")
}

// DELETE — xóa note
private fun deleteNote(noteId: Long) {
    val uri = ContentUris.withAppendedId(NoteContract.Notes.CONTENT_URI, noteId)
    val rowsDeleted = contentResolver.delete(uri, null, null)
    Log.d("NoteClient", "Deleted $rowsDeleted rows")
}
```
