# **_Pending_** Intent

## 1. `PendingIntent`

**Vai trò**: `PendingIntent` là **`Intent` được đóng gói**, cho phép **component khác** (_Notification_, _AlarmManager_, _Widget_) **thực thi `Intent`** thay mặt app của bạn vào **một thời điểm sau**.

### `Intent` vs `PendingIntent`

- `Intent`: App của bạn gọi `startActivity()` **thực thi ngay lập tức**

  ```kotlin
  val intent =
      Intent(this, DetailActivity::class.java)

  startActivity(intent)
  ```

- `PendingIntent`: Giao `Intent` cho **hệ thống / app khác**, hệ thống sẽ **execute sau**

  ```kotlin
  val intent =
      Intent(this, DetailActivity::class.java)

  val pendingIntent =
      PendingIntent.getActivity(
          this,
          0,
          intent,
          PendingIntent.FLAG_IMMUTABLE
      )
  ```

---

## 2. Ví dụ: **Notification**

### 2.1. **Định nghĩa `PendingIntent`**

```kotlin
// Khởi tạo intent
val intent = Intent(this, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
}

// Đóng gói intent thành PendingIntent
val pendingIntent = PendingIntent.getActivity(
    this,
    0,                                    // requestCode
    intent,
    PendingIntent.FLAG_IMMUTABLE          // bắt buộc từ API 31
    // FLAG_IMMUTABLE: PendingIntent không thể bị modify
    // FLAG_MUTABLE: có thể bị modify — chỉ dùng khi thực sự cần
)
```

Có **`4` loại `PendingIntent`** phụ thuộc hành động muốn thực thi sau đó:

| Loại `PendingIntent`                   | Mục đích sử dụng                 |
| -------------------------------------- | -------------------------------- |
| `PendingIntent.getActivity()`          | Mở **`Activity`**                |
| `PendingIntent.getBroadcast()`         | Gửi **`Broadcast`**              |
| `PendingIntent.getService()`           | Bắt đầu **`Service`**            |
| `PendingIntent.getForegroundService()` | Bắt đầu **`Foreground Service`** |

**Cấu trúc của `PendingIntent`**

```kotlin
PendingIntent.getActivity(
    context,
    requestCode,
    intent,
    flags
)
```

- `context`: Context để tạo `PendingIntent` (token)
- `requestCode`: Mã định danh cho `PendingIntent` (dùng khi cần phân biệt nhiều `PendingIntent`)
- `intent`: `Intent` gốc để thực thi sau
- `flags`: Cờ điều khiển hành vi của `PendingIntent`
  - PendingIntent.`FLAG_IMMUTABLE`: phía giữ `PendingIntent` **không thể sửa nội dung intent** đã đóng gói (**bắt buộc từ API 31**)
  - PendingIntent.`FLAG_MUTABLE`: phía giữ `PendingIntent` **có thể bị modify** sau khi tạo (chỉ **dùng khi thực sự cần**)
  - PendingIntent.`FLAG_UPDATE_CURRENT`: nếu `PendingIntent` đã tồn tại, cập nhật `Intent` mới vào `PendingIntent` cũ
    > _**token** cũ được giữ lại, chỉ cập nhật `Intent` mới vào token đó_
  - PendingIntent.`FLAG_CANCEL_CURRENT`: nếu `PendingIntent` đã tồn tại, hủy `PendingIntent` cũ và tạo mới
  - PendingIntent.`FLAG_ONE_SHOT`: `PendingIntent` chỉ có thể sử dụng **một lần duy nhất** — sau khi thực thi, `PendingIntent` sẽ tự động hủy
  - PendingIntent.`FLAG_NO_CREATE`: nếu `PendingIntent` đã tồn tại, trả về token đó; nếu chưa tồn tại, trả về `null` (không tạo mới)

    ```kotlin
    val existingPendingIntent =
        PendingIntent.getBroadcast(
            this,
            REMINDER_REQUEST_CODE,
            reminderIntent,
            PendingIntent.FLAG_NO_CREATE or
                PendingIntent.FLAG_IMMUTABLE
        )

    if (existingPendingIntent != null) {
        // Alarm hoặc token tương ứng đã tồn tại
    }
    ```

### 2.2. Đưa vào **Notification**

```kotlin
// Dùng trong NotificationCompat.Builder
val notification = NotificationCompat.Builder(this, CHANNEL_ID)
    .setSmallIcon(R.drawable.ic_notification)
    .setContentTitle("Tiêu đề")
    .setContentText("Nội dung thông báo")
    .setContentIntent(pendingIntent)     // tap notification → mở app
    .setAutoCancel(true) // notification tự biến mất sau khi tap
    .build()
```

---

## 3. `PendingIntent` & kiểm soát **Back stack**

### **Regular Activity** vs **Special Activity**

- **Regular** activity là màn hình thuộc **flow bình thường**: ex: `Main` -> `List` -> `Detail`
  > _Khi ấn **back** từ `Detail`, người dùng sẽ **quay lại** `List`_
- **Special** activity là màn hình chỉ **tồn tại để được mở từ bên ngoài**, ex: notification -> `Detail`
  > _Khi ấn **back** từ `Detail`, người dùng **thoát app** ngay_

### **TaskStackBuilder**: tạo `PendingIntent` với **back stack**

1. Sử dụng `parentActivityName` trong `AndroidManifest.xml` để khai báo **mối quan hệ cha-con** giữa các activity:

   ```xml
   <activity
       android:name=".MainActivity"
       android:exported="true">
       ...
   </activity>

   <activity
       android:name=".ProductListActivity"
       android:parentActivityName=".MainActivity" />

   <activity
       android:name=".ProductDetailActivity"
       android:parentActivityName=".ProductListActivity" />
   ```

2. Tạo `PendingIntent` có **back stack**

```kotlin
val detailIntent =
    Intent(
        this,
        ProductDetailActivity::class.java
    ).apply {
        putExtra(EXTRA_PRODUCT_ID, productId)
    }

val detailPendingIntent =
    TaskStackBuilder.create(this).run {
        // tạo back stack dựa trên
        // parentActivityName đã khai báo
        addNextIntentWithParentStack(
            detailIntent
        )

        // trả về pending intent
        // đã được tạo với back stack
        getPendingIntent(
            productId.toInt(),
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )
    }
```
