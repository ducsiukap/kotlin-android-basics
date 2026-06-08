# **`Gradle`** build system

## 1. **Gradle**

**`Gradle`** là **build automation tool** — phần mềm **tự động hóa** quá trình **_biên dịch_**, **_kiểm thử_**, và **_đóng gói_** app thành `APK`/`AAB`.

Khi bạn nhấn nút **Run** trong Android Studio, Gradle thực hiện toàn bộ chuỗi công việc:

```
Kotlin source → Compile → DEX → Merge resources → Sign → APK/AAB
```

**Gradle**, **Android Studio** & **AGP - android gradle plugin**:

- **Android Studio**: IDE
- **Gradle**: Build automation tool tổng quát
- **AGP**: Plugin Gradle chuyên dụng cho Android, cung cấp các task build cụ thể dạy Gradle cách build app Android.

**Gradle** dùng **Groovy (`build.gradle`)** hoặc **Kotlin DSL (`build.gradle.kts`)** để viết **build script**. Android Studio hiện tại mặc định dùng Kotlin DSL (file `.gradle.kts`).

---

## 2. `settings.gradle.kts`

`settings.gradle.kts` là file cấu hình **multi-module** của _Gradle_, nơi bạn khai báo các module con trong project:

- Nằm ở **root directory** của project.
- Dùng để **khai báo** project có những `module` nào

```kotlin
pluginManagement {
    // pluginManagement.repositories:
    // -> tìm Gradle plugins
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// dependencyResolutionManagement
//  -> tìm library dependencies (thư viện) cho project
dependencyResolutionManagement {
    repositoriesMode.set(
        RepositoriesMode.FAIL_ON_PROJECT_REPOS
    )

    repositories {
        google()
        mavenCentral()
    }
}

// rootProject.name = "HelloAndroid"
// -> đặt tên project, mặc định là tên thư mục gốc
rootProject.name = "HelloAndroid"

// include(":app")
// -> khai báo module con tên "app",
//      tương ứng với thư mục "app/"
include(":app")

// nếu project có thêm muodule
include(":core")
include(":feature-login")
```

> _`settings.gradle.kts` là file cấu hình cấp `root`, dùng để **khai báo repository** và **các module** tham gia `build`._

---

## 3. `build.gradle.kts`: root & module

### 3.1. `build.gradle`: Root/Project-level

`build.gradle.kts` ở **root** (_cùng cấp với `settings.gradle.kts`_) thường chứa **cấu hình áp dụng cho toàn bộ project, bao gồm tất cả module**.

```kotlin
plugins {
    // dùng version catalog
    // để quản lý plugin version
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    // hoặc
    // viết trực tiếp version
    id("com.android.application") version "..." apply false
    id("org.jetbrains.kotlin.android") version "..." apply false
}
```

`apply false` có ý nghĩa: biết `plugin + version` nhưng **chưa áp dụng** vào `root` project

> Module nào **cần** thì tự `apply` -> _**root** project **chỉ quản lý version**. Module app mới thực sự dùng plugin Android application._

Project Android Studio **hiện đại** thường **không** viết trực tiếp `plugin ID` và `version` ở `build.gradle.kts`. Thay vào đó, nó được định nghĩa trong `libs.versions.toml` (version catalog) để quản lý tập trung version của tất cả plugin và thư viện:

```toml
[versions]
agp = "9.2.0"

[plugins]
android-application = {
    id = "com.android.application",
    version.ref = "agp"
}

android-library = {
    id = "com.android.library",
    version.ref = "agp"
}
```

Khi này, trong `build.gradle.kts` sử dụng `alias`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}
```

### 3.2. `build.gradle`: App-level

`build.gradle.kts` ở **module app** (_thường nằm trong thư mục `app/`_) chứa **cấu hình cụ thể cho module app**, bao gồm:

```kotlin
// build.gradle.kts (Module: app)
plugins {
    alias(libs.plugins.android.application) // apply plugin
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.myapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapp"  // ID duy nhất trên Play Store
        minSdk = 24
        targetSdk = 35
        versionCode = 1      // Số nguyên, tăng mỗi lần release
        versionName = "1.0"  // Chuỗi hiển thị cho user
    }

    buildTypes {
        release {
            isMinifyEnabled = true   // Bật R8 shrinking cho bản release
            proguardFiles(...)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    // Bạn thêm thư viện bên thứ 3 ở đây
}
```

`plugins {}` block:

- Trong `build.gradle.kts` - **Project-level**: đăng ký plugin + version cho các module có thể áp dụng. Cần `apply false` để không áp dụng ngay vào root project.
- Trong `build.gradle.kts` - **Module-level**: khai báo sử dụng plugins -> _thực sự áp dụng plugin vào module đó, không cần version vì đã được quản lý ở root._ **Trong đó**:
  - `com.android.application`: plugin cho module **`app`**, cung cấp task build app Android. -> tạo `APK`/`AAB`
    > _Cần `applicationId=...` trong `defaultConfig` để định danh app khi cài lên thiết bị._
  - `com.android.library`: plugin cho module **`thư viện`**, tạo `AAR` (Android Archive) để chia sẻ code giữa các module.

**Dependencies** — khai báo thư viện: khi bạn muốn dùng một **thư viện bên ngoài** (`Retrofit`, `Glide`,...), bạn khai báo trong khối `dependencies`:

- `implementation("...")`: thư viện được dùng trong production code
- `testImplementation("...")`: thư viện chỉ dùng trong test
- `androidTestImplementation("...")`: thư viện chỉ dùng trong Android instrumentation test (trên thiết bị)

```kotlin
dependencies {
    // Thư viện dùng trong production code
    implementation("com.squareup.retrofit2:retrofit:2.11.0")

    // Chỉ dùng khi chạy test
    testImplementation("junit:junit:4.13.2")

    // Chỉ dùng khi chạy instrumented test (trên thiết bị)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
```

> **Thư viện UI** hay dùng và được khuyến nghị: **Material Components** `implementation("com.google.android.material:material:1.12.0")`