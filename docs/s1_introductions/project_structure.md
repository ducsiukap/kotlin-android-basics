# Android Project structure

## Create project

Following steps:

- Step 1: Open Android Studio.
- Step 2: **New Project** > **Empty Views Activity** > **Next**
- Step 3: Enter name, select Language = `Kotlin`, select Build configuration language > **Finish**

## Project Structure

**_`principle`_**:

- Code Java/Kotlin xử lý logic -> `java/xxx.xxx.projectname/`
- Code XML giao diện -> `res/layout`

```text
MyApp/
├── app/
│   ├── manifests/
│   │   └── AndroidManifest.xml
│   ├── kotlin+java/
│   │   └── com.example.myapp/
│   │       └── MainActivity.kt
│   └── res/
│       ├── drawable/
│       ├── layout/
│       │   └── activity_main.xml
│       ├── mipmap/       (icon launcher)
│       ├── values/
│       │   ├── colors.xml
│       │   ├── strings.xml
│       │   └── themes.xml
│       └── xml/
└── Gradle Scripts/
    ├── build.gradle.kts  (Project level)
    ├── build.gradle.kts  (Module: app)
    └── gradle.properties
```

#### `manifests/AndroidManifest.xml`

Như giấy khai sinh - `AndroidManifest.xml` là file cấu hình của ứng dụng, dùng để khai báo:

- App's name, icon, ...
- Activity, Service, Receiver,
- permission: Internet, Camera, GPS ...

#### `app/kotlin+java/ ` -> code logic

- `kotlin/com.example.projectname/MainActivity.kt`: được coi là file khởi chạy của project.

- `java/ui` quản lý giao diện (màn hình)
  - Activity: đại diện cho 1 màng hình, có lifecycle, gắn được layout
  - Fragment: là mảnh UI gắn vào Activity => cần reusable, navigation..

  > _**note**_: không bao giờ viết code logic tính toán, gọi database ngay trong file Activity.  
  > `Principle`: Activity chỉ làm nhiệm vụ:
  - Hiển thị dữ liệu lên screen.
  - Bắt sự kiện click.

- `java/data`

  ```text
  ├── data/
  │   ├── model/ -> Java Object thuần túy (POJO)
  │   ├── repository/
  │   └── api/
  ```

- `java/utils` -> static utility methods.

#### `app/res/` -> resources

- `res/layout/` -> chứa các file XML giao diện
- `res/values/` -> chứa các file XML định nghĩa các giá trị như colors.xml, strings.xml, styles.xml, ...
- `res/drawable/` -> images, icons, shape xml, vector xml, ... -> thành phần của layout
- `res/mipmap` -> App icon.

#### `Gradle` scripts

- `build.gradle` (project) : cấu hình chung cho toàn bộ dự án // ít dùng.
- `app/build.gradle` (or `app/build.gradle.kts`) -> quản lý app:
  - applicationId: id của app, ex: com.example.appnname
  - minSdk: minimum android version mà app hỗ trợ
  - targetSdk: phiên bản android tối ưu
  - dependencies: thư viện/package ngoài mà app dùng
