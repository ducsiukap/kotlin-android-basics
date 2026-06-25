# Tổng quan **Storage** trong Android

## 1. Phân loại storage

Android cung cấp **nhiều cơ chế lưu trữ** — mỗi cái phù hợp với một **loại data** khác nhau:

| Phân loại                      | Cách lưu trữ                               | --                                                             |
| ------------------------------ | ------------------------------------------ | -------------------------------------------------------------- |
| `SharedPreference`/`DataStore` | `key`-`value` đơn giản                     | Settings, preferences, token, flags, ...                       |
| **File Storage**               | Lưu **file**: text, JSON, binary, ảnh, ... | **Internal Storage** (private) / **External Storage** (public) |
| **`Room` Database**            | **Structured data**, query phức tạp        | Abstract layer trên `SQLite`                                   |
| `Content Provider`             |                                            | **Chia sẻ data** giữa các app                                  |

**Usecase**:

- **Settings** đơn giản (dark mode, language) → `DataStore`
- Auth **token**, user ID → `DataStore` (**Encrypted**)
- Danh sách sản phẩm, lịch sử đơn hàng → `Room` Database
- **File** ảnh, PDF, JSON export → `File Storage`
- **Data chia sẻ** với app khác → `Content Provider`
- **Cache** tạm thời nhỏ → `SharedPreferences`
  (hoặc `DataStore`)

---

## 2. Các loại bộ nhớ

### 2.1. **Internal Storage** vs **External Storage**

|                            | **Internal** storage                               | **External** storage                                                                     |
| -------------------------- | -------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| Storage                    | Bộ nhớ **trong** của thiết bị, **PRIVATE** với app | **Bộ nhớ ngoài** (SD card, USB, ...) hoặc **bộ nhớ trong** nhưng **PUBLIC** với app khác |
| Permission                 | Không cần permission                               | Cần permission (**API < 29**) hoặc **Scoped Storage (API 29+)**                          |
| Xóa khi uninstall          | Bị **xóa** khi **uninstall** app                   | **Vẫn còn** sau khi uninstall (nếu không dùng `getExternalFilesDir()`)                   |
| Duyệt qua **File Manager** | **KHÔNG** thể duyệt qua **File Manager**           | **CÓ** thể duyệt qua **File Manager**                                                    |

Tóm lại:

- **Internal**: private, KHÔNG cần permission, xóa khi uninstall
- **External**: public (or semi-public), cần permission

### 2.2. **Scoped Storage** (API 29+)

Từ **Android 10 (`API 29`)**, Google giới thiệu **Scoped Storage** — thay đổi cách app truy cập **External Storage**:

- Trước `API 29`: permission `READ_EXTERNAL_STORAGE` và `WRITE_EXTERNAL_STORAGE` cho phép app truy cập **toàn bộ external storage**.
- Từ `API 29+`: app chỉ có thể truy cập:
  - **Thư mục riêng của nó** trong external storage: `getExternalFilesDir()`, không cần permission.
  - **`MediaStore`** (ảnh, video, audio)
  - **Documents** qua Storage Access Framework (**`SAF`**)

  App **KHÔNG** còn quyền **truy cập toàn bộ** external storage, `READ`/`WRITE_EXTERNAL_STORAGE` dần bị xóa bỏ.

---

## 3. Các **Folder** quan trọng

Với **Internal Storage**: mặc định **KHÔNG** cần permission.

- **`context.filesDir`** → **File** private của app → _`/data/data/com.example.app/files/`_
- **`context.cacheDir`** → **Cache** private của app → _`/data/data/com.example.app/cache/`_
  > _Hệ thống có thể **tự xóa cache** khi thiết bị thiếu bộ nhớ_

Với **External Storage**:

- **KHÔNG CẦN `permission`**:
  - **`context.getExternalFilesDir(Enviroment.DIRECTORY_PICTURES)`** → **File** riêng của app, → _`/sdcard/Android/data/com.example.app/files/Pictures/`_
    > **BỊ XÓA** khi uninstall app
  - **`context.getExternalCacheDir`** → **Cache** riêng của app→ _`/sdcard/Android/data/com.example.app/cache/`_
- **CẦN `permission`**: public folder
  - **`Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)`** → **File** public, → _`/sdcard/Pictures/`_
    > **KHÔNG** bị xóa khi uninstall app
