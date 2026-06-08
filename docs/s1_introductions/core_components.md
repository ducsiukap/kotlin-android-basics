# **_4 Core_ components** of an Android app

Android app **không có một single entry point** như hàm `main()` trong Java/Kotlin thuần. Thay vào đó, **hệ thống có thể khởi động bất kỳ component nào trong số 4 loại sau**, tùy theo ngữ cảnh:

```
┌───────────────────────────────────────────────────────┐
│           4 ANDROID APPLICATION COMPONENTS            │
├───────────────────────┬───────────────────────────────┤
│   Activity            │  Màn hình UI, tương tác user  |
├───────────────────────┼───────────────────────────────┤
│   Service             │  Tác vụ nền, không có UI      │
├───────────────────────┼───────────────────────────────┤
│   BroadcastReceiver   │  Lắng nghe sự kiện hệ thống   │
├───────────────────────┼───────────────────────────────┤
│   Content Provider    │  Chia sẻ data giữa các app    │
└───────────────────────┴───────────────────────────────┘
```

**Quy tắc bắt buộc**: Tất cả 4 loại component đều **phải khai báo trong `AndroidManifest.xml`** — nếu không khai báo, Android OS không biết đến sự tồn tại của component đó.

---

## 1. **Activity** - for UI / Screen

**Activity** là component đại diện cho **một màn hình duy nhất** có **giao diện người dùng**.

> _`Mỗi màn` hình trong app `=` `một Activity`_

**Activity** là **component duy nhất** trong 4 loại có thể trực tiếp **hiển thị UI** và **nhận tương tác từ người dùng** (chạm, vuốt, gõ phím).

Khai báo trong `AndroidManifest.xml`:

```xml
<activity android:name=".MainActivity" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

---

## 2. **Service** - for background task

**Service** là component chạy **tác vụ nền (`background-task`)** lâu dài, **không có UI**. Khi chuyển app / tắt màn:

- **Activity** bị **dừng**
- **Service** vẫn **tiếp tục chạy**

Khai báo trong `AndroidManifest.xml`:

```xml
<service android:name=".MusicService" android:exported="false" />
```

Có `3` loại **Service**: `Started` Service, `Bound` Service, `Foreground` Service.

---

## 3. **BroadcastReceiver** - for broadcast events (system, other apps)

**BroadcastReceiver** là component **lắng nghe** và **phản hồi** các sự kiện (`broadcast`) từ hệ thống hoặc từ app khác.<br/>
eg. **Android OS** liên tục phát các `broadcast` khi có sự kiện xảy ra — _pin yếu_, _mất mạng_, _cắm sạc_, _thiết bị khởi động xong_...

> _**`BroadcastReceiver`** cho phép app của bạn "**đăng ký lắng nghe**" và phản hồi khi sự kiện đó xảy ra._

Khai báo trong `AndroidManifest.xml`:

```xml
<receiver android:name=".BootReceiver" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

**Note**:

- BroadcastReceiver **không có UI**
- BroadcastReceiver **không chạy lâu dài** — nó thực thi nhanh (`< 10s`) rồi kết thúc.

---

## 4. **Content Provider** - for data sharing between apps

**Content Provider** là component **quản lý và chia sẻ data** giữa các ứng dụng theo cách **có kiểm soát** và **an toàn**.

Cơ chế **Sandbox** khiến mỗi app chạy trong **process riêng** biệt — **Content Provider** là cơ chế chính thức để vượt qua rào cản đó.

- Giao tiếp với **Content Provider** thông qua `URI` có dạng `content://authority/path`
- **`ContentResolver`** — một client API chuẩn hóa với 4 operation: `query`, `insert`, `update`, `delete`.

Khai báo trong `AndroidManifest.xml`:

```xml
<provider
    android:name=".MyProvider"
    android:authorities="com.example.myapp.provider"
    android:exported="false" />
```
