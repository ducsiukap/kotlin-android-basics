# **`SQLite` database**

Kiến trúc:

```
App Code
    |
    ↓
SQLiteOpenHelper    ← quản lý tạo và upgrade database
    |
    ↓
SQLiteDatabase      ← thực thi query
    |
    ↓
.db file            ← file SQLite thực sự trên disk
                    /data/data/com.example.app/databases/myapp.db
```

## 1. **`SQLiteOpenHelper`**

Mọi app dùng **`SQLite` thuần** đều **phải** tạo class **kế thừa `SQLiteOpenHelper`**

```kotlin
class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,   // tên file .db
    null,            // CursorFactory — null = dùng mặc định
    DATABASE_VERSION // version — tăng lên khi thay đổi schema
) {
    companion object {
        const val DATABASE_NAME    = "myapp.db"
        const val DATABASE_VERSION = 1

        // Table & Column constants
        const val TABLE_NOTES   = "notes"
        const val COL_ID        = "_id"
        const val COL_TITLE     = "title"
        const val COL_CONTENT   = "content"
        const val COL_CREATED   = "created_at"
    }

    // Gọi khi database chưa tồn tại → tạo lần đầu
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_NOTES (
                $COL_ID      INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE   TEXT NOT NULL,
                $COL_CONTENT TEXT,
                $COL_CREATED INTEGER NOT NULL
            )
        """.trimIndent())
    }

    // Gọi khi DATABASE_VERSION tăng lên
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Cách đơn giản nhất — xóa và tạo lại
        // Production app nên migrate thay vì drop
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTES")
        onCreate(db)
    }
}
```

## 2. **CRUD operations**

### 2.1. Lấy **database instance**

```kotlin
val dbHelper = DatabaseHelper(context)

// db read-only
val readDb = dbHelper.readableDatabase
// db read + write
val writeDb = dbHelper.writableDatabase
```

### 2.2. **Insert**

```kotlin
fun insertNote(title: String, content: String): Long {
    val db = dbHelper.writableDatabase

    val values = ContentValues().apply {
        put(DatabaseHelper.COL_TITLE,   title)
        put(DatabaseHelper.COL_CONTENT, content)
        put(DatabaseHelper.COL_CREATED, System.currentTimeMillis())
    }

    // Trả về rowId của record vừa insert, -1 nếu thất bại
    return db.insert(DatabaseHelper.TABLE_NOTES, null, values)
}
```

### 2.3. **Query**

```kotlin
fun getAllNotes(): List<Note> {
    val notes = mutableListOf<Note>()
    val db = dbHelper.readableDatabase

    val cursor = db.query(
        DatabaseHelper.TABLE_NOTES,              // table
        null,                                    // columns (null = *)
        null,                                    // selection (WHERE)
        null,                                    // selectionArgs
        null,                                    // groupBy
        null,                                    // having
        "${DatabaseHelper.COL_CREATED} DESC"     // orderBy
    )

    cursor.use { c ->
        while (c.moveToNext()) {
            val id      = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_ID))
            val title   = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TITLE))
            val content = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_CONTENT))
            notes.add(Note(id, title, content))
        }
    }

    return notes
}

// Query với điều kiện
fun getNoteById(id: Long): Note? {
    val db = dbHelper.readableDatabase

    return db.query(
        DatabaseHelper.TABLE_NOTES,
        null,
        "${DatabaseHelper.COL_ID} = ?",   // WHERE _id = ?
        arrayOf(id.toString()),            // selectionArgs
        null, null, null
    ).use { cursor ->
        if (cursor.moveToFirst()) {
            Note(
                id      = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID)),
                title   = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TITLE)),
                content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTENT))
            )
        } else null
    }
}

// Raw SQL query — dùng khi logic phức tạp
fun searchNotes(keyword: String): List<Note> {
    val notes = mutableListOf<Note>()
    val db = dbHelper.readableDatabase

    val cursor = db.rawQuery(
        "SELECT * FROM ${DatabaseHelper.TABLE_NOTES} WHERE ${DatabaseHelper.COL_TITLE} LIKE ?",
        arrayOf("%$keyword%")
    )

    cursor.use { c ->
        while (c.moveToNext()) {
            val id      = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_ID))
            val title   = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TITLE))
            val content = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_CONTENT))
            notes.add(Note(id, title, content))
        }
    }

    return notes
}
```

### 2.4. **Update**

```kotlin
fun updateNote(id: Long, title: String, content: String): Int {
    val db = dbHelper.writableDatabase

    val values = ContentValues().apply {
        put(DatabaseHelper.COL_TITLE,   title)
        put(DatabaseHelper.COL_CONTENT, content)
    }

    // Trả về số rows bị affected
    return db.update(
        DatabaseHelper.TABLE_NOTES,
        values,
        "${DatabaseHelper.COL_ID} = ?",
        arrayOf(id.toString())
    )
}
```

### 2.5. **Delete**

```kotlin
fun deleteNote(id: Long): Int {
    val db = dbHelper.writableDatabase

    return db.delete(
        DatabaseHelper.TABLE_NOTES,
        "${DatabaseHelper.COL_ID} = ?",
        arrayOf(id.toString())
    )
}

fun deleteAllNotes(): Int {
    val db = dbHelper.writableDatabase
    return db.delete(DatabaseHelper.TABLE_NOTES, null, null)
}
```

---

## 3. **Transaction**

**Transaction** được sử dụng khi cần thực hiện **nhiều operation** cùng nhau — **tất cả thành công** hoặc **tất cả rollback**:

```kotlin
fun insertMultipleNotes(notes: List<Note>) {
    val db = dbHelper.writableDatabase

    db.beginTransaction()
    try {
        notes.forEach { note ->
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_TITLE,   note.title)
                put(DatabaseHelper.COL_CONTENT, note.content)
                put(DatabaseHelper.COL_CREATED, System.currentTimeMillis())
            }
            db.insert(DatabaseHelper.TABLE_NOTES, null, values)
        }
        db.setTransactionSuccessful()   // commit nếu không có exception
    } finally {
        db.endTransaction()   // rollback nếu setTransactionSuccessful() chưa được gọi
    }
}
```
