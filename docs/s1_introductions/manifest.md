# `AndroidManifest.xml` **_contract_**

## 1. Vai trò

`AndroidManifest.xml` là **hợp đồng** giữa `app` của bạn và `Android OS`.<br/>
Trước khi Android khởi chạy bất kỳ component nào của app (Activity, Service...), nó đọc file này để biết:

- App này gồm những **component** nào?
- App này cần những quyền (**permission**) nào?
- Activity nào là màn hình khởi động?
- App hỗ trợ thiết bị/tính năng phần cứng nào?

**Note**: Android yêu cầu mỗi app project phải có `Manifest`; file này khai báo component, permission và yêu cầu tương thích thiết bị.

> _Nếu một component không được khai báo trong Manifest → Android không biết đến sự tồn tại của nó → không thể khởi chạy._

## 2. Structure

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.myapp">  <!-- (1) Package name -->

    <!-- (2) Khai báo permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

    <!-- (3) Khai báo yêu cầu phần cứng -->
    <uses-feature android:name="android.hardware.camera" android:required="false" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"          <!-- icon app -->
        android:label="@string/app_name"            <!-- tên app -->
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.MyApp">         <!-- theme mặc định -->

        <!-- (4) Khai báo Activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true">   <!-- exported=true: cho phép system launch -->
            <intent-filter>
                <!-- Đây là Activity khởi động khi user mở app -->
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Activity khác không cần intent-filter MAIN/LAUNCHER -->
        <activity android:name=".DetailActivity" android:exported="false" />

        <!-- (5) Khai báo Service -->
        <service android:name=".MyService" android:exported="false" />

        <!-- (6) Khai báo BroadcastReceiver -->
        <receiver android:name=".MyReceiver" android:exported="false">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

        <!-- (7) Khai báo ContentProvider -->
        <provider
            android:name=".MyProvider"
            android:authorities="com.example.myapp.provider"
            android:exported="false" />

    </application>
</manifest>
```

Chi tiết:

1. **Package name** — **định danh** duy nhất của app trên toàn hệ sinh thái Android:

   ```xml
   <manifest xmlns:android="http://schemas.android.com/apk/res/android"
   package="com.example.myapp">
   ```

   **Convention** cho package name: `com.domainname.appname` (ví dụ: `com.google.maps`).

   > _Một khi đã publish lên Play Store, **không được đổi package name** — đổi nghĩa là app mới hoàn toàn._

   ***

2. **Permissions** — khai báo quyền mà app cần để truy cập tính năng nhạy cảm (camera, location...):
   - **Normal** permissions: `INTERNET`, `ACCESS_NETWORK_STATE` (**cấp tự động**)
   - **Dangerous** permissions: `ACCESS_FINE_LOCATION`, `READ_CONTACTS` (**cần runtime request**)

   ```xml
   <!-- Normal permission: tự động cấp, không hỏi user -->
   <uses-permission android:name="android.permission.INTERNET" />

   <!-- Dangerous permission: phải hỏi user lúc runtime (API 23+) -->
   <uses-permission android:name="android.permission.READ_CONTACTS" />
   ```

   **Note**: Khai báo trong `Manifest` là **điều kiện cần** nhưng **chưa đủ** với **dangerous** permission — bạn còn phải gọi `requestPermissions()` lúc runtime.

   ***

3. `android:exported` — chỉ định xem component có thể được khởi chạy **bởi hệ thống hoặc app khác** hay không:
   - `true`: cho phép system/app khác launch component này.
   - `false`: chỉ app của bạn mới có thể launch component này.

   > _Từ **API 31 (Android 12)** trở đi, nếu một component có `intent-filter`, bạn **phải khai báo `android:exported` rõ ràng**. Nếu không, app sẽ không build được._

   **Note**: Từ Android 12 trở đi, nếu một component có `intent-filter`, bạn phải khai báo `android:exported` rõ ràng.

   ***

4. `<intent-filter>` — xác định cách component có thể được khởi chạy bởi hệ thống hoặc app khác:
   - `<action android:name="android.intent.action.MAIN" />` — chỉ định đây là **entry point** chính của app.
   - `<category android:name="android.intent.category.LAUNCHER" />` — cho phép component này xuất hiện trong launcher

   ```xml
   <intent-filter>
       <action android:name="android.intent.action.MAIN" />
       <category android:name="android.intent.category.LAUNCHER" />
   </intent-filter>
   ```

   Cụ thể:
   - `ACTION_MAIN` = **app's entry point**
   - `CATEGORY_LAUNCHER` = hiển thị icon app trong launcher (danh sách app)

   > _Thiếu một trong hai → app không có icon launcher_
