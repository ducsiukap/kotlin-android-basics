# **`Room` Database**

## 1. `SQLite` vs `Room`: lí do **SQLite** dần bị thay thế bởi **Room**

1. SQL **không** được **validate** lúc **compile**:

   ```kotlin
   // Typo trong tên column → chỉ crash lúc runtime
   db.rawQuery("SELECT * FROM notes WHERE titlee LIKE ?", ...)
   //                                          ↑ typo!
   ```

2. **Boilerplate code** quá nhiều: <br/>
   Ví dụ, để **đọc một row** phải:
   1. `query()` -> cursor
   2. `moveToFirst()`/`moveToNext()` -> check cursor
   3. `getColumnIndexOrThrow()` cho **từng column**
   4. `getXxx()` cho **từng column**
   5. Tạo **object** thủ công.
   6. `close()` cursor.

   > _`Room` làm tất cả những việc trên **tự động**._

3. Không **thread-safe**:

   ```kotlin
   // Gọi trên Main Thread → ANR
   val notes = getAllNotes()   // database I/O trên Main Thread!
   ```

4. Không tích hợp `Coroutines`/`Flow`/`LiveData` → khó viết code **reactive**, không thể observe thay đổi, ...

---

## 2.**What** is **`Room` database**

`Room` là **thư viện Jetpack** — **`abstraction layer` trên SQLite**.

- `Room` **KHÔNG thay thế** `SQLite` **mà WRAP** nó lại.
- Tự động generate boilerplate code, và tích hợp sẵn với `Coroutines`/`Flow`.

### **`KSP` vs `KAPT`**

`ksp` và `kapt` đều là **annotation processor** quan trọng khi setup `Room` nhưng có một số khác biệt:

- **`KAPT` - Kotlin Annotation Processing Tool**: là **công cụ cũ**, chạy trên JVM.
  - Chậm hơn, **deprecated** dần
  - Vẫn hoạt động nhưng không được khuyến nghị.
- **`KSP` - Kotlin Symbol Processing**: là **công cụ mớ i**, chạy trực tiếp trên **`Kotlin compiler`** (**native Kotlin**).
  - Nhanh hơn `KAPT` đáng kể (~2x faster)
  - Google **khuyến nghị** sử dụng `KSP` từ **`Room 2.4.0+`** và **các project mới**.

---

## 3. Setup `Room`

### 3.1. Thêm **KSP plugin**

```kotlin
// build.gradle.kts (Project level)
plugins {
    // ...
    id("com.google.devtools.ksp") version "2.3.9" apply false
                                            // ↑ nên khớp
                                            // với Kotlin version
}


// build.gradle.kts (Module: app)
plugins {
    // ...
    id("com.google.devtools.ksp")   // thêm KSP plugin
}
```

### 3.2. Thêm **Dependencies**

```kotlin
// build.gradle.kts (Module: app)
dependencies {
    val roomVersion = "2.8.4"

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")       // Coroutines + Flow support
    ksp("androidx.room:room-compiler:$roomVersion")             // KSP — generate code
    // annotationProcessor thay vì ksp nếu dùng KAPT (không khuyến nghị)
}
```

> _Có thể kiếm tra **version mới của Room** tại: [https://developer.android.com/jetpack/androidx/releases/room](https://developer.android.com/jetpack/androidx/releases/room)_

---

## 4. **Cấu trúc** của `Room` database

Với **`Room`** database, có `3` thành phần chính:

- **`@Entity`**: đại diện cho **table** trong database, `1 instance = 1 row`.
- **`@Dao`**: **Data Access Object**, là interface định nghĩa các **query** để thao tác với database, **`Room` generate tự động**.
- **`Database` (entry point)**: là **abstract class** kế thừa `RoomDatabase`, quản lý kết nối tới SQLite database file

**Luồng hoạt động**:

1. Định nghĩa `@Entity`, `@Dao`, `Database`
2. **KSP** đọc các annotation lúc **compile**, generate code
3. **Room** generate implementation code.
4. **Runtime**: gọi `@Dao`'s methods -> **Room** sẽ thực thi **query** và trả kết quả

---

## 5. [`@Entity`](./room_entity.md)

---

## 6. [`@Dao`](./room_dao.md)

---

## 7. **`@Database` & Relationships**

### 7.1. **`@Database` - `Room`'s entry point**

#### 7.1.1. **Vai trò**:

- `@Entity`: ánh xạ **data class** <-> **table**
- `@Dao`: định nghĩa query operations
- `@Database`: là **entry point**, quản lý **`SQLite` connection**, tạo implementation cho `@Dao` interface.

#### 7.1.2. **Implementation**:

```kotlin
@Database(
    entities = [
        User::class,
        Post::class,
        Comment::class
    ],
    version = 1,
    exportSchema = true   // Export schema ra file JSON — dùng để track migration
)
abstract class AppDatabase : RoomDatabase() {

    // Room tự generate implementation — chỉ cần khai báo abstract
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao

    companion object {

        // Volatile — đảm bảo mọi thread đều thấy giá trị mới nhất
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            // Double-checked locking — thread-safe singleton
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,  // ApplicationContext — tránh leak Activity
                AppDatabase::class.java,
                "app_database.db"            // Tên file SQLite
            )
            .fallbackToDestructiveMigration()  // Dev only — xóa DB khi tăng version
            .build()
        }
    }
}
```

> **Tại sao sử dụng `applicationContext`?**: <br/>
> _Database tồn tại suốt vòng đời app. Nếu truyền `Activity context` → **Activity không bao giờ được GC** → memory leak_

#### 7.1.3. `version` & `exportSchema`

```kotlin
@Database(
    entities = [...],
    version = 2,          // Tăng version mỗi khi thay đổi schema
    exportSchema = true   // Lưu schema vào app/schemas/
)
```

**Version**:

- `version=1` → schema ban đầu
- `version=2` → schema thay đổi (thêm column, table, ...) → phải có **`Migration 1 - 2`** để **migrate** dữ liệu cũ sang schema mới.
- `version=3` → schema thay đổi tiếp → phải có **`Migration 2 - 3`**, ...

**`exportSchema = true`**: sinh ra file **JSON**:

```json
// app/schemas/com.example.AppDatabase/1.json
{
  "version": 1,
  "entities": [
    {
      "tableName": "users",
      "fields": [...]
    }
  ]
}
```

> _File này commit lên git — là **lịch sử schema**, dùng để viết Migration và review thay đổi DB._

#### 7.1.4. **Migration** - nâng cấp schema mà **không mất data**

```kotlin
// Migration từ version 1 → 2
// Bài toán: thêm column "phone" vào bảng users
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE users ADD COLUMN phone TEXT"
        )
    }
}

// Migration từ version 2 → 3
// Bài toán: tạo bảng mới "addresses"
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS addresses (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                user_id INTEGER NOT NULL,
                street TEXT NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
        """.trimIndent())
    }
}

// Đăng ký vào Database builder
Room.databaseBuilder(context, AppDatabase::class.java, "app_database.db")
    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
    .build()
```

> Hàm `fallbackToDestructiveMigration()` — chỉ dùng trong **development**. <br/>
> **Production** app **phải có Migration đầy đủ** — không thì user mất sạch data khi update app.

### 7.2. **Relationships** - quan hệ giữa các table

**`Room` không tự động handle relationships** như `Hibernate` hay `ORM` framework khác.

Relationships phải được **khai báo tường minh**, thuộc **`1` trong `3` loại**:

- **One-to-One**
- **One-to-Many**
- **Many-to-Many**

#### 7.2.1. **One-to-One** relationship

`@Entity`:

```kotlin
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val email: String
)

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey(autoGenerate = true)
    val profileId: Long = 0,

    // Foreign key trỏ về users.id
    @ColumnInfo(name = "user_id")
    val userId: Long,

    val bio: String,
    val avatarUrl: String
)
```

**Relation class**:

```kotlin
data class UserWithProfile(
    @Embedded
    val user: User,             // Embed toàn bộ columns của User

    @Relation(
        parentColumn = "id",        // Column của User (parent)
        entityColumn = "user_id"    // Column của UserProfile trỏ về User
    )
    val profile: UserProfile?   // Nullable — user có thể chưa có profile
)
```

`@Dao`:

```kotlin
@Dao
interface UserDao {

    @Transaction  // Bắt buộc với @Relation — đảm bảo atomic read
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserWithProfile(userId: Long): UserWithProfile?

    @Transaction
    @Query("SELECT * FROM users")
    suspend fun getAllUsersWithProfile(): List<UserWithProfile>
}
```

> _**`@Transaction` bắt buộc với `@Relation`** — Room thực hiện nhiều query bên dưới (1 cho User, 1 cho Profile), `@Transaction` đảm bảo chúng chạy trong cùng một transaction — **data nhất quán**_.

#### 7.2.2. **One-to-Many** relationship

`@Entity`:

```kotlin
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "posts",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],     // PK của User
            childColumns = ["user_id"], // FK trong Post
            onDelete = ForeignKey.CASCADE  // Xóa User → xóa luôn Posts
        )
    ],
    indices = [Index("user_id")]  // Index FK — tăng performance query
)
data class Post(
    @PrimaryKey(autoGenerate = true)
    val postId: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

`onDelete` options:

```
ForeignKey.CASCADE     → Xóa parent → xóa luôn children
ForeignKey.SET_NULL    → Xóa parent → set FK = null
ForeignKey.RESTRICT    → Không cho xóa parent nếu còn children
ForeignKey.NO_ACTION   → Không làm gì — có thể gây inconsistency
```

**Relation class**:

```kotlin
data class UserWithPosts(
    @Embedded
    val user: User,

    @Relation(
        parentColumn = "id",
        entityColumn = "user_id"
    )
    val posts: List<Post>   // List — one-to-MANY
)
```

`@Dao`:

```kotlin
@Dao
interface UserDao {

    @Transaction
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserWithPosts(userId: Long): UserWithPosts?

    @Transaction
    @Query("SELECT * FROM users")
    fun getAllUsersWithPosts(): Flow<List<UserWithPosts>>
    // Flow → tự emit mỗi khi data thay đổi

    // Insert riêng lẻ
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long  // Trả về id vừa insert

    @Insert
    suspend fun insertPost(post: Post)

    // Insert user + posts trong 1 transaction
    @Transaction
    suspend fun insertUserWithPosts(user: User, posts: List<Post>) {
        val userId = insertUser(user)
        posts.forEach { post ->
            insertPost(post.copy(userId = userId))
        }
    }
}
```

#### 7.2.3. **Many-to-Many** relationship

**Many-to-Many cần bảng trung gian** (junction table / cross-reference table).

`@Entity`:

```kotlin
@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true)
    val studentId: Long = 0,
    val name: String
)

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true)
    val courseId: Long = 0,
    val title: String,
    val instructor: String
)

// Junction table — bảng trung gian
@Entity(
    tableName = "student_course_cross_ref",
    primaryKeys = ["studentId", "courseId"],  // Composite PK
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["studentId"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Course::class,
            parentColumns = ["courseId"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StudentCourseCrossRef(
    val studentId: Long,
    val courseId: Long,
    val enrolledAt: Long = System.currentTimeMillis()  // Extra data của relationship
)
```

**Relation class** - 2 chiều:

```kotlin
// Student → Courses (student đang học những course nào)
data class StudentWithCourses(
    @Embedded
    val student: Student,

    @Relation(
        parentColumn = "studentId",
        entityColumn = "courseId",
        associateBy = Junction(StudentCourseCrossRef::class)
        // Junction — báo cho Room biết đây là many-to-many
        // cần đi qua junction table
    )
    val courses: List<Course>
)

// Course → Students (course này có những student nào)
data class CourseWithStudents(
    @Embedded
    val course: Course,

    @Relation(
        parentColumn = "courseId",
        entityColumn = "studentId",
        associateBy = Junction(StudentCourseCrossRef::class)
    )
    val students: List<Student>
)
```

`@Dao`:

```kotlin
@Dao
interface StudentDao {

    // Insert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enrollStudentInCourse(crossRef: StudentCourseCrossRef)

    // Query — Student → Courses
    @Transaction
    @Query("SELECT * FROM students WHERE studentId = :studentId")
    suspend fun getStudentWithCourses(studentId: Long): StudentWithCourses?

    // Query — Course → Students
    @Transaction
    @Query("SELECT * FROM courses WHERE courseId = :courseId")
    suspend fun getCourseWithStudents(courseId: Long): CourseWithStudents?

    // Query với extra data từ junction
    @Query("""
        SELECT * FROM student_course_cross_ref
        WHERE studentId = :studentId
        ORDER BY enrolledAt DESC
    """)
    suspend fun getEnrollmentHistory(studentId: Long): List<StudentCourseCrossRef>

    // Unenroll
    @Delete
    suspend fun unenrollStudent(crossRef: StudentCourseCrossRef)
}
```

### 7.3. **Nested relationships** - quan hệ lồng nhau

```kotlin
data class PostWithComments(
    @Embedded
    val post: Post,

    @Relation(
        parentColumn = "postId",
        entityColumn = "postId"
    )
    val comments: List<Comment>
)

data class UserWithPostsAndComments(
    @Embedded
    val user: User,

    @Relation(
        parentColumn = "id",
        entityColumn = "user_id",
        entity = Post::class  // Chỉ định rõ entity khi nest relationship
    )
    val postsWithComments: List<PostWithComments>
)
```

`@Dao`:

```kotlin
@Dao
interface UserDao {
    @Transaction
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserWithPostsAndComments(userId: Long): UserWithPostsAndComments?
}
```

---

## 8. **CRUD operations** với `Coroutines`: [CRUD & Coroutines](./room_crud_coroutines.md)
