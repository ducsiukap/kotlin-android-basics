# Room's `@Dao` - **Data Access Object**

`@Dao` là **`interface`** (_hoặc `abstract class`_) nơi **định nghĩa tất cả các query tương tác với database**.

> _`Room` đọc các **annotation** trong `Dao` và **tự động generate implementation** lúc compile_

```kotlin
@Dao
interface NoteDao {
    // Định nghĩa method với annotation
    // Room generate implementation tự động
}
```

## 1. Các **annotation** trong `@Dao`:

### 1.1. **`@Insert`** - Thêm row vào table

```kotlin
@Dao
interface NoteDao {

    // Insert 1 object
    @Insert
    suspend fun insert(note: Note): Long
    // Trả về rowId của record vừa insert

    // Insert nhiều object
    @Insert
    suspend fun insertAll(notes: List<Note>): List<Long>
}
```

**`@Insert`** có thể **handle conflict** khi insert:

```kotlin
@Dao
interface NoteDao {
    // Insert với conflict strategy
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(note: Note): Long
}
```

Các `onConflict` options:

```
ABORT    → default, throw exception khi conflict (vi phạm UNIQUE/PK)
REPLACE  → xóa row cũ, insert row mới — dùng cho upsert
IGNORE   → bỏ qua insert nếu conflict, trả về -1
FAIL     → throw exception, không rollback các insert trước đó
ROLLBACK → rollback toàn bộ transaction
```

### 1.2. **`@Update`** - Cập nhật row trong table

```kotlin
@Dao
interface NoteDao {

    // Update theo Primary Key
    // Room tìm row có ID khớp với note.id và update
    @Update
    suspend fun update(note: Note): Int
    // Trả về số rows bị affected

    @Update
    suspend fun updateAll(notes: List<Note>): Int
}
```

### 1.3. **`@Delete`** - Xóa row trong table

```kotlin
@Dao
interface NoteDao {

    @Delete
    suspend fun delete(note: Note): Int
    // Trả về số rows bị xóa

    @Delete
    suspend fun deleteAll(notes: List<Note>): Int
}
```

### 1.4. **`@Query`**

`@Query` là **annotation** linh hoạt nhất — **cho phép viết SQL tùy ý**:

```kotlin
@Dao
interface NoteDao {

    // SELECT tất cả
    @Query("SELECT * FROM notes ORDER BY created_at DESC")
    suspend fun getAllNotes(): List<Note>

    // SELECT theo điều kiện
    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): Note?
    // :noteId → bind với parameter noteId của method

    // SELECT với nhiều điều kiện
    @Query("SELECT * FROM notes WHERE title LIKE :keyword OR content LIKE :keyword")
    suspend fun searchNotes(keyword: String): List<Note>

    // SELECT trả về Flow — reactive, tự cập nhật khi data thay đổi
    @Query("SELECT * FROM notes ORDER BY created_at DESC")
    fun getAllNotesFlow(): Flow<List<Note>>
    // Không cần suspend — Flow tự quản lý coroutine

    // SELECT chỉ một số column — dùng data class riêng
    @Query("SELECT id, title FROM notes ORDER BY title ASC")
    suspend fun getNoteTitles(): List<NoteSummary>

    // COUNT
    @Query("SELECT COUNT(*) FROM notes")
    suspend fun countNotes(): Int

    // DELETE bằng @Query — linh hoạt hơn @Delete
    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteById(noteId: Long): Int

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes(): Int

    // UPDATE bằng @Query — linh hoạt hơn @Update
    @Query("UPDATE notes SET title = :title, content = :content WHERE id = :id")
    suspend fun updateFields(id: Long, title: String, content: String): Int
}
```

---

## 2. **`suspend`, `Flow` & Blocking**

### 2.1. `suspend fun`: one-shot, gọi 1 lần, chờ kết quả

```kotlin
// suspend fun → one-shot, gọi 1 lần, chờ kết quả
// Dùng cho: INSERT, UPDATE, DELETE, và SELECT khi chỉ cần lấy 1 lần
@Query("SELECT * FROM notes")
suspend fun getAllNotes(): List<Note>
```

### 2.2. `Flow`: reactive, observe data liên tục, tự động emit khi data thay đổi

```kotlin
// Flow → stream, observe liên tục
// Tự emit lại khi data trong table thay đổi
// Dùng cho: SELECT cần reactive — List screen, count...
// KHÔNG cần suspend vì Flow tự handle coroutine
@Query("SELECT * FROM notes")
fun getAllNotesFlow(): Flow<List<Note>>
```

### 2.3. `LiveData`: tương tự `FLow` nhưng **lifecycle-aware**, chỉ emit khi **UI active**.

```kotlin
// LiveData → tương tự Flow nhưng lifecycle-aware
// Ít dùng hơn Flow trong code mới
@Query("SELECT * FROM notes")
fun getAllNotesLiveData(): LiveData<List<Note>>
```

### 2.4. **Blocking**: không dùng `suspend`/`Flow` → crash nếu gọi trên main.

```kotlin
// Blocking (không suspend, không Flow) → KHÔNG DÙNG
// Sẽ crash nếu gọi trên Main Thread từ API 1
@Query("SELECT * FROM notes")
fun getAllNotesBlocking(): List<Note>   // ❌ tránh dùng
```

---

## 3. **Parameter binding** trong `@Query`

Bind parameter đơn giản:

```kotlin
// Dùng :paramName để bind
@Query("SELECT * FROM notes WHERE id = :id")
suspend fun getById(id: Long): Note?

// Nhiều parameter
@Query("SELECT * FROM notes WHERE user_id = :userId AND category = :category")
suspend fun getNotesByUserAndCategory(userId: Long, category: String): List<Note>
```

**Bind Collection** — `IN` clause

```kotlin
// Bind List vào IN clause
@Query("SELECT * FROM notes WHERE id IN (:ids)")
suspend fun getNotesByIds(ids: List<Long>): List<Note>

// Dùng:
val ids = listOf(1L, 2L, 3L)
val notes = noteDao.getNotesByIds(ids)
```

**`LIKE` query** — Tìm kiếm

```kotlin
// Phải thêm % ở phía caller, không thêm trong SQL
@Query("SELECT * FROM notes WHERE title LIKE :keyword")
suspend fun searchByTitle(keyword: String): List<Note>

// Gọi:
val results = noteDao.searchByTitle("%android%")
// Hoặc wrap trong repository:
suspend fun search(query: String) = noteDao.searchByTitle("%$query%")
```

---

## 4. **Return type** của `@Query`

### **`dto` - partial data / projection**

```kotlin
// Data class chỉ chứa các field cần thiết
data class NoteSummary(
    val id: Long,
    val title: String
)

// Query chỉ lấy 2 column
@Query("SELECT id, title FROM notes ORDER BY title ASC")
suspend fun getNoteSummaries(): List<NoteSummary>
```

### **Nullable**

```kotlin
// Trả về null nếu không tìm thấy
@Query("SELECT * FROM notes WHERE id = :id")
suspend fun getNoteById(id: Long): Note?   // nullable

// Trả về Flow<Note?> — emit null nếu không tìm thấy
@Query("SELECT * FROM notes WHERE id = :id")
fun observeNoteById(id: Long): Flow<Note?>
```

### **`Cursor`** - khi cần dùng **raw data**

```kotlin
// Ít dùng — chỉ khi cần access cursor trực tiếp
@Query("SELECT * FROM notes")
fun getAllNotesCursor(): Cursor
```

---

## 5. **Transaction** trong `@Dao`

```kotlin
@Dao
interface NoteDao {

    @Insert
    suspend fun insert(note: Note): Long

    @Query("DELETE FROM notes WHERE category = :category")
    suspend fun deleteByCategory(category: String): Int

    // @Transaction đảm bảo 2 operation dưới đây chạy trong cùng 1 transaction
    @Transaction
    suspend fun replaceCategoryNotes(category: String, newNotes: List<Note>) {
        deleteByCategory(category)
        newNotes.forEach { insert(it) }
    }
}
```

### **`@Transaction`** với **`@Query`** trên bảng có quan hệ:

`@Transaction` còn dùng khi query trả về **object có relationship**

```kotlin
data class UserWithOrders(
    @Embedded val user: User,
    @Relation(
        parentColumn = "id",
        entityColumn = "user_id"
    )
    val orders: List<Order>
)

@Transaction
@Query("SELECT * FROM users WHERE id = :userId")
suspend fun getUserWithOrders(userId: Long): UserWithOrders?
// @Transaction bắt buộc khi dùng @Relation
// để đảm bảo data nhất quán khi đọc từ nhiều bảng
```
