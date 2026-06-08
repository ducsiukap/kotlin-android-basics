# Basic **_attributes_** of `XML` and `View`

## 1. **Namespace**

Bất kì thuộc tính nào trong XML đều phải có `namespace`, nếu không sẽ bị lỗi.

**Namespace** được khai báo ở đầu file XML:

```xml
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools">
```

Trong đó:

- `xmlns:android` — namespace cho **thuộc tính Android chuẩn như** `android:text`, `android:id`, ...
- `xmlns:app` — namespace cho **thuộc tính thư viện** như **ConstrainLayout**, **Material**, ...
- `xmlns:tools` — namespace chỉ dùng lúc design time, không ảnh hưởng runtime, cho **thuộc tính công cụ** như `tools:text`, ... để preview

---

## 2. `android:id` — định danh View

```xml
<Button
    android:id="@+id/btnLogin"
    ... />
```

- `@+id/btnLogin` — **tạo mới** một id có tên `btnLogin`
- `@id/btnLogin` — **sử dụng** id đã tồn tại có tên `btnLogin`

Quy ước **đặt tên ID** theo `view + Name`:

```
btnLogin        → Button
tvUsername      → TextView
etPassword      → EditText  (et = EditText)
ivAvatar        → ImageView (iv = ImageView)
rvProductList   → RecyclerView (rv = RecyclerView)
```

---

## 3. `layout_width` & `layout_height` — quan trọng nhất

```xml
<Button
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    ... />
```

Mọi View đều **bắt buộc có hai thuộc tính này**. Có `3` giá trị (_áp dụng được cho cả `width` và `height`_):

- `"match_parent"`— View **chiếm toàn bộ không gian còn lại của View cha** theo chiều đó
- `"wrap_content"` — View chỉ chiếm đúng **không gian cần thiết để hiển thị nội dung** của nó.
- `"100dp"` (giá trị cụ thể + `dp`) — kích thước cố định theo đơn vị `dp`.

---

## 4. `dp` & `sp` — đơn vị đo lường

Android có nhiều đơn vị, nhưng thường chỉ cần quan tâm `2` loại:

- **Density-independent Pixels (`dp`)** — Đơn vị chuẩn cho **kích thước layout** (_width_, _height_, _margin_, _padding_)
  - Với màn hình **`160dpi` (mdpi)**, `1dp = 1px`
  - Với màn hình **`320dpi` (xhdpi)**, `1dp = 2px`

  Dùng `dp` **đảm bảo UI trông nhất quán** trên mọi mật độ màn hình.

- **Scale-independent Pixels (`sp`)** — Dùng chỉ cho **kích thước `text`** (`android:textSize`)
  > _`sp` tương tự `dp`, nhưng còn được điều chỉnh theo **cài đặt kích thước chữ (`font size`) của người dùng**_.

```xml
android:layout_width="200dp"   ✅ đúng
android:textSize="16sp"        ✅ đúng
android:textSize="16dp"        ❌ sai — text không scale theo accessibility
```

---

## 5. **Padding** & **Margin** — khoảng cách

```
┌─────────────────────────────┐
│         MARGIN              │  ← Khoảng cách với View bên ngoài
│  ┌───────────────────────┐  │
│  │       PADDING         │  │  ← Khoảng cách nội dung với border
│  │  ┌─────────────────┐  │  │
│  │  │    NỘI DUNG     │  │  │
│  │  └─────────────────┘  │  │
│  └───────────────────────┘  │
└─────────────────────────────┘
```

- `android:padding` — khoảng cách giữa **nội dung** và **border** của View

  ```xml
  <!-- Padding: áp dụng bên TRONG view, đẩy nội dung ra xa border -->
  android:padding="16dp"                    <!-- 4 phía -->
  android:paddingTop="8dp"
  android:paddingHorizontal="16dp"          <!-- trái + phải -->
  ```

  > _**Note**: không giống **Jetpack Compose**, thứ tự khái báo `padding` và `background` không ảnh hưởng lẫn nhau_

- `android:margin` — khoảng cách giữa **View** và **các View khác** (bao gồm cả View cha)

  ```xml
  <!-- Margin: áp dụng bên NGOÀI view, đẩy view ra xa các view xung quanh -->
  android:layout_margin="16dp"             <!-- 4 phía -->
  android:layout_marginTop="8dp"
  android:layout_marginStart="16dp"        <!-- dùng Start/End thay Left/Right (RTL support) -->
  ```

---

## 6. `android:visibility` — Visibility

Có `3` giá trị cho `visibility`:

- `"visible"` (_default_) — View hiển thị bình thường
- `"invisible"` — View **không hiển thị** nhưng vẫn **`chiếm`** không gian
- `"gone"` — View **không hiển thị** và **`không` chiếm** không gian

```xml
android:visibility="visible"    <!-- hiển thị bình thường (default) -->
android:visibility="invisible"  <!-- ẩn nhưng VẪN CHIẾM không gian -->
android:visibility="gone"       <!-- ẩn và KHÔNG CHIẾM không gian -->
```
