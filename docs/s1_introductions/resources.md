# Resource: `res/` & `R` class

## 1. `res/` directory - **Resources System**

Trong Android, phần **giao diện** và các **dữ liệu tĩnh** thường **không viết cứng trực tiếp trong code** Java/Kotlin.<br/>
Thư mục `res/` chứa toàn bộ **tài nguyên tĩnh** của app.

> _**Android** có **resource system** rất mạnh — thay vì hardcode giá trị trực tiếp trong code, bạn **khai báo** chúng trong `res/` và **tham chiếu** qua `R` class._

```plaintext
res/
├── layout/        → File XML định nghĩa UI
├── drawable/      → Ảnh (PNG, JPG), icon (SVG vector), shape XML
├── mipmap/        → Icon launcher (nhiều độ phân giải: hdpi, xhdpi, xxhdpi...)
├── values/        → values -> các file XML chứa giá trị
│   ├── strings.xml   → chuỗi text
│   ├── colors.xml    → màu sắc
│   ├── dimens.xml    → kích thước (dp, sp)
│   └── themes.xml    → app's themes
├── menu/          → menu XML
├── xml/           → XML cấu hình tổng quát
├── raw/           → file media (audio, video) hoặc file tùy ý
├── font/          → font files (TTF, OTF)
├── navigation/    → navigation graph XML
└── anim/          → animations
```

Trong `res/`, ta có các **sub-folder**:

- `layout/`: chứa các file XML định nghĩa **giao diện người dùng (UI)**.
- `drawable/`: chứa các **tài nguyên đồ họa** như ảnh (PNG, JPG), icon (SVG vector), hoặc shape XML.
- `raw/`: chứa các file media (audio, video) hoặc file tùy ý mà bạn muốn **giữ nguyên định dạng gốc**.
  > _**Khác** với `drawable`, file trong `raw/` **không được Android xử lý như hình ảnh giao diện**._
- `mipmap/`: chứa các **icon launcher** với nhiều độ phân giải khác nhau (hdpi, xhdpi, xxhdpi...).
- `values/`: chứa các file XML định nghĩa **giá trị** như:
  - `strings.xml`: chứa các chuỗi text.
  - `colors.xml`: chứa các màu sắc.
  - `dimens.xml`: chứa các kích thước (dp, sp).
  - `themes.xml`: chứa các themes của app.
- `anim/`: chứa các file định nghĩa **animations**.

Tại sao phải tách `res/` thay vì **hardcode**?

- Tư duy **DI**: việc tách biệt dữ liệu và logic giúp code dễ bảo trì, tái sử dụng: **sửa/tham chiếu tới 1 nơi thay vì phải định nghĩa ở mỗi nơi sử dụng**
- Hỗ trợ **đa ngôn ngữ**: dễ dàng tạo các file `strings.xml` cho từng ngôn ngữ, giúp app hỗ trợ đa ngôn ngữ.

  > _eg. `res/values/strings.xml` (default), `res/values-es/strings.xml` (Spanish), `res/values-vi/strings.xml` (Vietnamese)_

  Android **tự chọn ngôn ngữ phù hợp theo `locale`** của thiết bị. Đây là cơ chế **resource qualifier**.

---

## 2. `R` class - **Resource ID**

`R` là class **được sinh tự động** khi `build` project.

Mỗi lần bạn **thêm** một resource vào `res/`, Android build system tự động generate class `R` với các **integer `ID`** tương ứng:

```kotlin
// Được auto-generate, bạn KHÔNG viết file này
object R {
    object layout {
        val activity_main: Int = 0x7f0b001c
    }
    object id {
        val btnLogin: Int = 0x7f080123
    }
    object string {
        val app_name: Int = 0x7f0e0045
    }
    object color {
        val purple_500: Int = 0x7f060012
    }
}
```

Trong code, có thể **tham chiếu** tới resource qua `R.type.name` class:

```kotlin
setContentView(R.layout.activity_main)   // load layout XML
val btn = findViewById<Button>(R.id.btnLogin)
val name = getString(R.string.app_name)
```

Trong XML, tham chiếu qua `@type/name` syntax:

```xml
android:text="@string/app_name"
android:background="@color/purple_500"
android:layout_width="@dimen/button_width"
```

**Note**:

- `@id/...`: tham chiếu tới ID đã tồn tại
- `@+id/...`: tạo ID mới

**Resource's name convention**:

- `snake_case` format, gồm: chữ thường (`a-z`), số (`0-9`), dấu gạch dưới (`_`)
- **Cấu trúc**: `[type]_[description]`
  - `type`: loại resource (string, color, dimen...)
  - `description`: mô tả ngắn gọn về resource (app_name, purple_500...)

eg: `activity_main.xml`, `btn_login`, `ic_user_profile`, `color_primary`, ..
