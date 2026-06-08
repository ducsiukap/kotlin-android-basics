# Android OS & System Architecture

## 1. Android OS

**Android** là một **software stack** mã nguồn mở dựa trên **Linux Kernel**, được thiết kế cho nhiều loại thiết bị và form factor: `mobile`, `tablet`, `TV`, `Android Automative`, `wearable` (watch), `IoT`, v.v..

Khi bạn viết một Android app, code của bạn không chạy trực tiếp trên phần cứng. **Giữa code của bạn và phần cứng là nhiều tầng trừu tượng** — và hiểu các tầng đó giúp bạn lý giải được tại sao Android hoạt động theo cách nó hoạt động.

## 2. Android System Architecture

Android OS được tổ chức thành **nhiều tầng**, mỗi tầng có một vai trò cụ thể trong việc cung cấp các dịch vụ và API cho ứng dụng của bạn. Dưới đây là một cái nhìn tổng quan về kiến trúc hệ thống Android:

![Android System Architecture](/res/s1/img/android_architecture.png)

Details:
![Android Architecture Layered](/res/s1/img/android-stack.jpg)

### 2.1. **Application** layer

**Application** layer là tầng cao nhất, nơi các ứng dụng Android của chạy.

- Mỗi ứng dụng chạy trong một **sandbox** riêng biệt, đảm bảo an toàn và bảo mật:
  - **Mỗi app được gán một `UID` riêng** và thông thường chạy trong **process riêng**
  - Nhờ vậy, các app mặc định không thể đọc trực tiếp dữ liệu private nhau hoặc tự do truy cập hệ điều hành
  - Android còn dùng **SELinux** để tăng cường ranh giới sandbox.
    > _**SELinux** được áp dụng thêm từ `Android 4.3` và chuyển sang **enforced** đầy đủ từ `Android 5.0`_
- Android provides a set of core applications (**system apps**): Email Client, SMS Program, Calendar, Maps, Browser, Contacts, v.v..

### 2.2. **Application Framework API** layer

**Framework API** là bộ API cấp cao cho phép app **gọi các chức năng của OS** mà không phải xử lý kernel, driver hay HAL.

> _Android cung cấp API cho **UI**, **resource**, **notification**, **lifecycle**, **navigation back stack** và nhiều chức năng khác._

**Android SDK** gồm:

- `ActivityManager` — quản lý vòng đời Activity
- `WindowManager` — quản lý cửa sổ hiển thị
- `PackageManager` — quản lý ứng dụng đã cài
- `NotificationManager` — quản lý thông báo
- Content Providers — quản lý dữ liệu chia sẻ giữa các app
- View System — quản lý giao diện người dùng
- Location Manager, Resource Manager, Telephony Manager, ....

> _[**`4 core` app components**](./4_core_android_app_components.md): `Activity`, `Service`, `BroadcastReceiver`, `ContentProvider` — là những building block cơ bản nhất của app, được hệ thống Android quản lý vòng đời và tương tác._

Các class thường gặp:

| Nhóm            | API                                                                                                   |
| --------------- | ----------------------------------------------------------------------------------------------------- |
| App lifecycle   | ActivityManager/`Activity`, `Application`, `Service`                                                  |
| Navigation      | `Intent`, `PendingIntent`                                                                             |
| OS accessing    | `Context`                                                                                             |
| Background task | `Service`, `BroatcastReceiver`, WorkManager                                                           |
| Shared data     | `ContentProvider`, Room                                                                               |
| UI              | - **_Truyền thống_**: `View`, `TextView`, `RecyclerView`, v.v.. <br>- **_Hiện đại_**: Jetpack Compose |

### 2.3. **Native Libraries** & **Android Runtime (`ART`)** layer

#### 2.3.1. **Native Libraries** & native deamons layer

Một phần lớn **Android OS** được viết bằng `C/C++.` Tầng native có:

- Các **thư viện** như `libc`, `liblog`, `libbinder`, `libselinux`, ...
- Các **daemon** như: `init` (khởi tạo hệ thống khi boot), `healthd`, `logd`, `storaged`, ...

> _Phần lớn app Kotlin **không đụng trực tiếp tầng này**._

Các **Native libraries** được **exposed to developers thông qua `Android Application Framework`** như: Sufface Manager, Media Framework, OpenGL ES, SQLite, WebKit, v.v...

#### 2.3.2. **Android Runtime - `ART`** layer

**Android Runtime - `ART`** là môi trường thực thi code. Đây là runtime **quản lý việc thực thi app và một số system service**.

##### _2.3.2.1. **DalvikVirtualMachine (`Dalvik`)**_

`ART` chạy **bytecode ở định dạng `DEX` — _Dalvik Executable_**

Quá trình **build app**:

```plaintext
Kotlin source code (.kt)
    ↓   kotlinc compiler
Bytecode (.class)
    ↓   D8 / R8 compiler
DEX bytecode (.dex)   ← Dalvik Executable
    ↓   ART
Machine code (chạy trực tiếp trên CPU)
```

**ART** dùng kỹ thuật **`AOT` - Ahead-of-Time compilation**— biên dịch DEX bytecode thành machine code ngay lúc cài app giúp app chạy nhanh hơn và tốn ít CPU hơn khi runtime.

- Từ `Android 5.0`, **`ART` thay Dalvik (`JIT`)** làm runtime chính
- ART cũng hỗ trợ **`JIT` - Just-in-Time compilation** và **garbage collection** để tối ưu hiệu năng và quản lý bộ nhớ.

##### _2.3.2.2. **Core Libraries**_

**ART** có thành phần **Core Libraries**, cung cấp hầu hết các chức năng có sẵn (**functionality available**) của **core libraries**, viết bằng `C/C++` và `Java`, cung cấp các APIs cơ bản cho app:

- Data Structures
- Utilities
- File Access
- Network Access
- Graphics
- ...

### 2.4. **`HAL` - Hardware Abstract Layer**

**`HAL` - Tầng trừu tượng hóa phần cứng**: Khi app muốn truy cập phần cứng như camera, audio, ...

- **App** gọi tới **Android Camera API**
- **`HAL`** xử lý giao tiếp với **driver**.
  > _eg. Driver có thể được cung cấp bởi nhiều nhà sản xuất -> HAL lo việc giao tiếp với driver camera cụ thể của từng nhà sản xuất, **định nghĩa `interface` chuẩn** để vendor cài đặt bên dưới._

```plaintext
Android Framework
        ↓ interface chuẩn
Camera HAL
        ↓ implementation tùy hãng
Driver + phần cứng cụ thể
```

### 2.5. **Linux Kernel** layer

**Kernel** cung cấp các **chức năng nền tảng** như quản lý `process`, `thread`, `memory`, `networking`, `file system`, `security` và _giao tiếp với phần cứng qua driver_.

- **Android** không phải là một bản Linux desktop thu nhỏ. Nhưng **phần lõi của Android dựa trên Linux kernel**.
  > _Android **kế thừa Linux kernel** nhưng không phải là Linux distribution — không có glibc, không chạy được phần mềm Linux thông thường._
- ART cũng dựa vào kernel để xử lý threading và quản lý bộ nhớ cấp thấp.

#### App's **Sandbox**

Android tận dụng cơ chế **`user-based` protection** của Linux: **mỗi app** được gán **một `UID` riêng** và thông thường chạy trong **process riêng**. _Nhờ vậy app A mặc định không thể đọc trực tiếp dữ liệu private của app B hoặc tự do truy cập hệ điều hành_

```
App Facebook       → UID 10123
App Banking        → UID 10124
Your App           → UID 10125
```

> _Android còn dùng **`SELinux`** để tăng cường **ranh giới sandbox**. SELinux được áp dụng thêm từ Android 4.3 và chuyển sang **enforced** đầy đủ từ `Android 5.0`_

#### **Binder `IPC` - Inter-Process Communication**

Mỗi **App** thường nằm trong **sandbox và process riêng**.<br/>
**System services** cũng nằm trong các **process khác**.

Nhưng app vẫn cần nhờ OS làm việc như bật camera, rung máy, tạo notification, truy vấn package hoặc phát audio: Android dùng **Binder** làm cơ chế **`IPC` — Inter-Process Communication**:

- **App camera** dùng **Binder** để nói chuyện với **Camera server** ở process khác
- **Camera server** tiếp tục dùng **Binder** để giao tiếp với **Camera HAL**

```
App process
    ↓ Binder IPC
System service
    ↓ Binder IPC
HAL process
    ↓
Hardware
```
