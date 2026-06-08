# Android SDK - Software Development Kit

## 1. Android `SDK`

**`SDK` (Software Development Kit)** là bộ công cụ Google cung cấp để **build Android app**. Android SDK là tập hợp nhiều `package` phục vụ phát triển ứng dụng, bao gồm:

- **Android API** — tập hợp các class, interface, method bạn gọi trong code (Activity, Intent, View...)
- **Build tools** — compiler, packager (tạo APK/AAB)
- **Emulator** — giả lập thiết bị Android
- **ADB (Android Debug Bridge)** — giao tiếp với thiết bị thật/giả lập

Cấu trúc thường thấy:

```
Android SDK/
├── platforms/
├── platform-tools/
├── build-tools/
├── cmdline-tools/
├── emulator/
├── system-images/
├── sources/
└── ndk/              ← tùy chọn
```

Các package có thể được cài và cập nhật bằng **SDK Manager** của Android Studio hoặc command-line tool `sdkmanager`.

---

## 2. **API Level**, `compileSdk`, `minSdk`, `targetSdk`

Mỗi phiên bản Android được định danh bằng một **API Level** — số nguyên tăng dần tương ứng với **Android Framework API**:

```
API Level  │  Android Version  │  Tên
───────────┼───────────────────┼──────────────────
    21     │   Android 5.0     │  Lollipop
    23     │   Android 6.0     │  Marshmallow   ← Runtime Permissions ra đời
    26     │   Android 8.0     │  Oreo          ← NotificationChannel bắt buộc
    28     │   Android 9.0     │  Pie
    29     │   Android 10      │  Q             ← Scoped Storage
    31     │   Android 12      │  S
    33     │   Android 13      │  Tiramisu
    34     │   Android 14      │  Upside Down Cake
```

> _**API Level** cần thiết để compile app theo version đó_

Trong `build.gradle`, cần định nghĩa **3 giá trị**:

```gradle
android {
    compileSdk = 35   // (1)
    defaultConfig {
        minSdk = 24   // (2)
        targetSdk = 35  // (3)
    }
}
```

- `compileSdk` — API Level dùng để **compile code**.<br/>
  **Ý nghĩa**: bạn có thể gọi các API tồn tại đến API level này.
  > _Luôn đặt bằng **API level mới nhất** để truy cập được API mới nhất_.
- `minSdk` — API Level thấp nhất mà app của bạn hỗ trợ.<br/>
  **Ý nghĩa**: thiết bị chạy Android **thấp hơn `minSdk`** sẽ không thể cài app của bạn
- `targetSdk` — API Level mà app của bạn được **thiết kế** và **test**.

> _**Quy tắc thực tế**: `compileSdk` = `targetSdk` = **API mới nhất**. `minSdk` = tùy yêu cầu dự án._

---

## 3. Các `package` quan trọng trong **Android SDK**

### 3.1. SDK Platform

Mỗi **SDK Platform tương ứng một API level**. Nó cần thiết để compile app theo version đó.<br/>
_eg: `platforms/android-36/`_

**SDK Manager** cũng cung cấp source code platform và system images phục vụ debug, emulator.

### 3.2. **Platform** Tools

**Platform Tools** chứa các công cụ command-line như `adb` (Android Debug Bridge) và `fastboot` để **giao tiếp với thiết bị** thật hoặc giả lập.<br/>
_eg: `android_sdk/platform-tools/`_

**Android Debug Bridge (`adb`)** có thể **quản lý trạng thái** emulator hoặc thiết bị Android và **cài APK** lên thiết bị.

> _`logcat` được dùng để xem log app và system_

Các lệnh thường gặp:

```bash
adb devices
adb install app-debug.apk
adb uninstall com.example.app
adb shell
adb logcat
adb shell dumpsys activity
adb shell dumpsys package com.example.app
```

> _`dumpsys` lấy **thông tin chẩn đoán** từ các system service trên thiết bị và thường được gọi qua `adb`_

### 3.3. **Build** Tools

**Build Tools** là package **_bắt buộc_** để build app Android.<br/>
_eg: `android_sdk/build-tools/<version>/`_

Một số tool quan trọng:
| Tool | Mục đích |
| --- | --- |
| `aapt` / `aapt2` | Android Asset Packaging Tool: **xử lý resource** và đóng gói vào `R.java` |
| `d8` | Chuyển bytecode thành **DEX**|
| `apksigner` | Ký và kiểm tra chữ ký APK |
| `zipalign` | Tối ưu alignment dữ liệu trong APK để giảm bộ nhớ khi chạy |

> _Thông thường, **Gradle plugin** sẽ tự động gọi các build tool này._

### 3.4. **Command-line** Tools

_eg: `android_sdk/cmdline-tools/<version>/bin/`_

**Tools** hay gặp:
| Tool | Mục đích |
| --- | --- |
| `sdkmanager` | **Quản lý các package SDK** (cài, cập nhật, gỡ) |
| `avdmanager` | **Quản lý Android Virtual Device (`AVD`)** cho emulator |
| `apkanalyzer` | Phân tích APK (thành phần, kích thước, dependencies) |
| `retrace` | Đọc stack trace đã bị obfuscate bởi R8 |

### 3.5. **Emulator** & System Images

- **Emulator mô phỏng thiết bị Android** trên máy tính.
- Mỗi **system image** đại diện cho một **Android API level** và **loại thiết bị**.

```
system-images/
└── android-36/
    └── google_apis/
        └── x86_64/
```

### 3.6. **NDK** (optional)

**`NDK` là Native Development Kit**. Nó chỉ cần khi project có code `C/C++`
