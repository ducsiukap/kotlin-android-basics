# **`URI`, `ContentResolver` & `ContentProvider`**

## 1. **`URI`** trong **Content Provider**

### 1.1. **`URI` - Uniform Resource Identifier**

`URI` là **ngôn ngữ chung** giữa `client` và `provider`, có cấu trúc:

```
content://<authority>/<path>/<id>
```

đây là "**địa chỉ**" để **xác định chính xác data** nào cần truy cập. Trong đó:

- `content://` — scheme chuẩn của **Content Provider**, luôn là `content://`
- `<authority>` — **tên định danh duy nhất** của **Provider**, thường là **package name** của app cung cấp data
- `<path>` — **đường dẫn** xác định **table** hoặc **resource** cụ thể trong **Provider**
- `<id>` (_optional_) — xác định **`record`** cụ thể trong **table/resource** đó

example:

```
content://com.example.myapp.provider/users/42
    │              │                    │   │
 scheme         authority            path  id

-> provider tự viết, bảng users, record có id=42
```

Một số `URI` được **Android cung cấp sẵn**:

- `content://com.android.contacts/contacts` — truy cập toàn bộ danh bạ
- `content://com.android.contacts/contacts/5` — truy cập contact có `id=5`
- `content://media/external/images/media` — toàn bộ ảnh trên thiết bị (**`MediaStore`**)
- `content://media/external/images/media/42` — ảnh có `id=42` trong **`MediaStore`**
- ...

### 1.2. Tạo `URI` cho **Content Provider**

```kotlin
// Dùng Uri.parse() cho URI cố định
val contactsUri = Uri.parse("content://com.android.contacts/contacts")

// Dùng ContentUris.withAppendedId() để thêm ID
val singleContactUri = ContentUris.withAppendedId(
    ContactsContract.Contacts.CONTENT_URI,
    contactId   // Long
)
// → content://com.android.contacts/contacts/5

// Dùng Uri.Builder cho URI phức tạp
val uri = Uri.Builder()
    .scheme("content")
    .authority("com.example.myapp.provider")
    .appendPath("products")
    .appendPath("electronics")
    .build()
// → content://com.example.myapp.provider/products/electronics
```

### 1.3. **`UriMatcher`**: map **URI** → **code** để xử lý

Khi **Provider** nhận một `URI`, cần xác định request đó **muốn gì**. `UriMatcher` làm việc này:

```kotlin
object UserContract {

    const val AUTHORITY = "com.example.myapp.provider"

    // URI codes — định danh loại request
    const val CODE_USERS_ALL  = 1   // /users
    const val CODE_USER_ITEM  = 2   // /users/42

    val BASE_URI: Uri = Uri.parse("content://$AUTHORITY")

    object Users {
        const val PATH = "users"
        val CONTENT_URI: Uri = Uri.withAppendedPath(BASE_URI, PATH)

        // Column names
        const val TABLE_NAME  = "users"
        const val COL_ID      = "_id"
        const val COL_NAME    = "name"
        const val COL_EMAIL   = "email"
        const val COL_AGE     = "age"
    }

    // UriMatcher — map URI pattern → code
    val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(AUTHORITY, "users",    CODE_USERS_ALL)  // /users
        addURI(AUTHORITY, "users/#",  CODE_USER_ITEM)  // /users/42 — # = số
        addURI(AUTHORITY, "users/*",  3)               // /users/abc — * = string
    }
}
```

---

## 2. **`ContentResolver`**: **Client**-side `API`

`ContentResolver` là **cầu nối** giữa `client` và `provider`, cung cấp các phương thức để **thực hiện CRUD** trên data thông qua `URI`.

Details: [ContentResolver](./content_resolver.md)

---

## 3. **`ContentProvider`**: **Provider**-side implementation

`ContentProvider` là **lớp cơ sở** mà các app tạo ra để **cung cấp data** cho các app khác. <br/>
Nó định nghĩa các phương thức như: `query()`, `insert()`, `update()`, `delete()` để xử lý các request từ `ContentResolver`.

Details: [ContentProvider](./implement_content_provider.md)
