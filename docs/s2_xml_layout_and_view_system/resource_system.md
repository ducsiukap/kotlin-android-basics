# **Resources** system

## 1. `strings.xml` — quản lý **text**

### 1.1. Định nghĩa trong `strings.xml`

```xml
<!-- res/values/strings.xml -->
<resources>
    <string name="app_name">MyApp</string>
    <string name="btn_login">Đăng nhập</string>
    <string name="hint_email">Nhập email của bạn</string>
</resources>
```

String với **placeholder** - dynamic string:

```xml
<resources>
    <!-- String với placeholder -->
    <string name="welcome_user">Xin chào, %1$s!</string>
</resources>
```

Cấu trúc: `%[vị_trí]$[kiểu_dữ_liệu]`, trong đó:

- `vị_trí`: thứ tự của placeholder trong chuỗi, ứng với vị trí tham số truyền vào.<br/>
  Dù không bắt buộc nhưng **nên sử dụng** để tránh lỗi khi có nhiều placeholder:<br/>
  eg:

  ```xml
  <!-- strings-vi.xml -->
  <resources>
      <string name="welcome_user">Xin chào, %1$s! Bạn có %2$d thông báo.</string>
  </resources>
  ```

  ```xml
  <!-- string-en.xml -->
  <resources>
      <string name="welcome_user">You have %2$d notifications, %1$s.</string>
  </resources>
  ```

  Khi này, vẫn truyền theo thứ tự: `getString(R.string.welcome_user, userName, notificationCount)`, nhưng sẽ hiển thị đúng theo ngôn ngữ.

- `kiểu_dữ_liệu`: xác định kiểu dữ liệu của placeholder, giúp đảm bảo tính nhất quán và tránh lỗi khi truyền tham số.<br/>
  Các kiểu dữ liệu phổ biến:
  - `$s`: String
  - `$d`: Integer
  - `$f`: Float, `$.2f` để định dạng float với 2 chữ số thập phân
- Sử dụng `%%` cho ký tự `%` thực sự: _50%%_ -> _50%_

### 1.2. **Dùng trong code:**

```kotlin
// welcome_user: Xin chào, %1$s!
val welcome = getString(R.string.welcome_user, "Nguyễn Văn A")
// Kết quả: "Xin chào, Nguyễn Văn A!"
```

---

## 2. `colors.xml` — quản lý **màu sắc**

```xml
<!-- res/values/colors.xml -->
<resources>
    <color name="primary">#6200EE</color>
    <color name="primary_dark">#3700B3</color>
    <color name="accent">#03DAC5</color>
    <color name="white">#FFFFFF</color>
    <color name="black">#000000</color>

    <!-- Màu với alpha: #AARRGGBB -->
    <color name="overlay">#80000000</color>  <!-- 50% transparent black -->
</resources>
```

---

## 3. `dimens.xml` — quản lý **kích thước**

```xml
<!-- res/values/dimens.xml -->
<resources>
    <dimen name="margin_standard">16dp</dimen>
    <dimen name="margin_small">8dp</dimen>
    <dimen name="text_size_body">16sp</dimen>
    <dimen name="text_size_title">24sp</dimen>
    <dimen name="button_height">48dp</dimen>
</resources>
```
