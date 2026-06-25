# Room's `@Entity` - **Định nghĩa table**

Cơ bản:

```kotlin
@Entity(tableName = "notes")   // tên table trong SQLite
data class Note(
    @PrimaryKey(autoGenerate = true)   // tự tăng ID
    val id: Long = 0,

    @ColumnInfo(name = "title")        // tên column trong SQLite
    val title: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
```

## 1. Các **annotation** của `@Entity`:

### 1.1. `@PrimaryKey`: đánh dấu **column** là **primary key** của table.

```kotlin
// Auto increment
@PrimaryKey(autoGenerate = true)
val id: Long = 0

// Manual primary key — bạn tự set
@PrimaryKey
val id: String   // UUID chẳng hạn

// Composite primary key — nhiều cột làm primary key
@Entity(primaryKeys = ["userId", "productId"])
data class CartItem(
    val userId: Long,
    val productId: Long,
    val quantity: Int
)
```

### 1.2. `@ColumnInfo`: đánh dấu **column** trong table, có thể set `name` khác với tên property trong class.

```kotlin
@ColumnInfo(
    name = "user_name",           // tên column trong SQLite (mặc định = tên property)
    typeAffinity = ColumnInfo.TEXT, // kiểu dữ liệu SQLite
    index = true                  // tạo index cho column này — tăng tốc query
)
val userName: String
```

### 1.3. `@Ignore`: đánh dấu **property** không được lưu vào database.

```kotlin
@Ignore   // property này KHÔNG được lưu vào database
val isSelected: Boolean = false   // chỉ dùng cho UI state
```

### 1.4. `@Embedded`: đánh dấu **property** là **object**, `Room` sẽ **flatten** các field của object này thành các column trong table.

```kotlin
data class Address(
    val street: String,
    val city: String,
    val zipCode: String
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,

    @Embedded   // các field của Address được flatten vào table users
    val address: Address
)
// Table users sẽ có columns: id, name, street, city, zipCode
```

Có thể dùng `@Embedded` với `prefix` để tránh tên column trùng:

```kotlin
@Entity
data class Employee(
    @PrimaryKey val id: Long,
    val name: String,

    @Embedded(prefix = "home_")
    val homeAddress: Address,

    @Embedded(prefix = "work_")
    val workAddress: Address
)
// Columns: id, name, home_street, home_city, home_zipCode,
//          work_street, work_city, work_zipCode
```

---

## 2. **Indices** — Tăng tốc query

```kotlin
@Entity(
    tableName = "products",
    indices = [
        Index(value = ["name"]),                    // index đơn
        Index(value = ["category", "price"]),       // composite index
        Index(value = ["sku"], unique = true)       // unique index — không cho trùng
    ]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val price: Double,
    val sku: String
)
```

## 3. **Foreign Key** — Ràng buộc quan hệ giữa các table

```kotlin
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "orders",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],       // column trong bảng cha (users)
            childColumns = ["user_id"],   // column trong bảng con (orders)
            onDelete = ForeignKey.CASCADE // xóa user → tự xóa orders của user đó
            // onDelete options: CASCADE, SET_NULL, SET_DEFAULT, RESTRICT, NO_ACTION
        )
    ],
    indices = [Index("user_id")]   // index foreign key — bắt buộc để tránh full table scan
)
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val total: Double,
    val createdAt: Long = System.currentTimeMillis()
)
```
