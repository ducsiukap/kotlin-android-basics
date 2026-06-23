# `ContentResolver`: **Client**-side `API` để truy cập `ContentProvider`

## 1. Lấy `ContentResolver`

```kotlin
// Trong Activity/Fragment
val resolver = contentResolver               // Activity
val resolver = requireContext().contentResolver  // Fragment
val resolver = context.contentResolver       // Bất kỳ context nào
```

---

## 2. `04` operations **CRUD**

| Operation  | `API`      |
| ---------- | ---------- |
| **Create** | `insert()` |
| **Read**   | `query()`  |
| **Update** | `update()` |
| **Delete** | `delete()` |

### 2.1. **`query()`**: đọc data

```kotlin
// READ — query()
val cursor = contentResolver.query(
    uri,            // URI của data cần đọc
    projection,     // String[]? — cột nào cần lấy (null = tất cả)
    selection,      // String? — điều kiện WHERE (null = không filter)
    selectionArgs,  // Array<String>? — giá trị cho ? trong selection
    sortOrder       // String? — ORDER BY (null = không sort)
)
```

Example: read contacts

```kotlin
val projection = arrayOf(
    ContactsContract.Contacts._ID,
    ContactsContract.Contacts.DISPLAY_NAME,
    ContactsContract.Contacts.HAS_PHONE_NUMBER
)

val selection = "${ContactsContract.Contacts.HAS_PHONE_NUMBER} = ?"
val selectionArgs = arrayOf("1")   // chỉ lấy contact có số điện thoại
val sortOrder = "${ContactsContract.Contacts.DISPLAY_NAME} ASC"

// query() trả về Cursor — con trỏ tới kết quả
val cursor = contentResolver.query(
    ContactsContract.Contacts.CONTENT_URI,
    projection,
    selection,
    selectionArgs,
    sortOrder
)
```

Handle result với `Cursor`:

```kotlin
cursor?.use { c ->
    // use {} tự động close cursor sau khi xong

    if (c.moveToFirst()) {   // di chuyển đến row đầu tiên
        do {
            // Đọc giá trị từng cột
            val id   = c.getLong(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
            val name = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))

            contacts.add(Contact(id, name))

        } while (c.moveToNext())   // chuyển sang row tiếp theo
    }
}
// cursor tự đóng sau use {}
```

- `moveToFirst()` — di chuyển con trỏ đến **row đầu tiên** của kết quả, return `false` nếu empty
- `moveToLast()` — di chuyển con trỏ đến **row cuối cùng**
- `moveToNext()` — di chuyển con trỏ đến **row tiếp theo**, trả về `false` nếu đã hết row
- `moveToPrevious()` — di chuyển con trỏ đến **row trước đó**
- `moveToPosition(int position)` — di chuyển con trỏ đến **row tại vị trí position**
- `getCount()` — trả về **số row** trong kết quả

### 2.2.2. **`insert()`**: thêm data

```kotlin
// CREATE — insert()
val newUri = resolver.insert(
    uri, contentValues
)
```

Example: insert new contact

```kotlin
val values = ContentValues().apply {
    put("name",  "Nguyễn Văn A")
    put("phone", "0123456789")
    put("email", "nguyenvana@email.com")
}

val newUri = contentResolver.insert(
    MyProvider.CONTENT_URI,   // URI của provider
    values
)
// newUri = content://com.example.myapp.provider/contacts/6
// → URI của record vừa được tạo
```

### 2.3. **`update()`**: cập nhật data

```kotlin
// UPDATE — update()
val rowsUpdated = resolver.update(
    uri, contentValues,
    selection, selectionArgs
)
```

Example: update contact

```kotlin
val values = ContentValues().apply {
    put("name", "Nguyễn Văn B")   // chỉ update cột name
}

// update contact có id=6
val rowsUpdated = contentResolver.update(
    MyProvider.CONTENT_URI,
    values,
    "_id = ?",          // WHERE _id = ?
    arrayOf("6")        // id = 6
)

// Update toàn bộ records
val rowsUpdated = contentResolver.update(
    MyProvider.CONTENT_URI,
    values,
    null,   // không có WHERE → update tất cả
    null
)
```

### 2.4. **`delete()`**: xóa data

```kotlin
// DELETE — delete()
val rowsDeleted = resolver.delete(
    uri,
    selection, selectionArgs
)
```

Example: delete contact

```kotlin
// Xóa một record cụ thể
val rowsDeleted = contentResolver.delete(
    MyProvider.CONTENT_URI,
    "_id = ?",
    arrayOf("6")
)

// Xóa theo URI trực tiếp (nếu URI có id)
val specificUri = ContentUris.withAppendedId(MyProvider.CONTENT_URI, 6)
val rowsDeleted = contentResolver.delete(specificUri, null, null)
```
