# **`Toast` & `Snackbar`**

Cả `Toast` và `Snackbar` thuộc nhóm **Transient Messages** — là dạng **thông báo NGẮN, TỰ ĐỘNG BIẾN MẤT** sau 1 khoảng thời gian, **KHÔNG
chặn (block) tương tác** của user với phần còn lại màn hình

## 1. `Toast`

### 1.1. **Create and display a `Toast`**

`Toast` là một **popup nhỏ**, xuất hiện vài giây và tự biến mất.

```kotlin
Toast.makeText(
    requireContext(),
    "Đã lưu contact",
    Toast.LENGTH_SHORT
).show()
```

`Toast.makeText()` là **cách duy nhất nên dùng** để tạo `Toast`:

> _Trước đây, có thể dùng `Toast()` constructor để tạo `Toast`, sau đó gọi `setView()` để thiết lập view cho toast, nhưng cách này **hiện tại đã bị hạn chế**._

Hàm `makeText(context, text, duration)` nhận vào **3 tham số**:

- `context`: có thể dùng context của `Activity`/`Fragment` hoặc Application context.
- `text`: **nội dung hiển thị** trên `Toast`
- `duration`: **thời gian hiển thị**. Có 2 tuỳ chọn: `Toast.LENGTH_SHORT` (2s) hoặc `Toast.LENGTH_LONG` (3.5s)
  > _**`duration` KHÔNG có option set số giây TUỲ Ý**, chỉ chấp nhận một trong 2 lựa chọn đó._

Để có thể hiển thị `Toast`, **BẮT BUỘC phải gọi `show()`** sau khi tạo. _Nếu không, nó chỉ tạo ta **`Toast` object**, không hiển thị lên màn hình._

### 1.2. **Vị trí hiển thị - `setGravity()`**

Có thể **custom vị trí hiển thị** của `Toast` bằng cách gọi `setGravity(gravity, xOffset, yOffset)`:

```kotlin
val toast = Toast.makeText(requireContext(), "Đã lưu", Toast.LENGTH_SHORT)
toast.setGravity(Gravity.TOP, 0, 100)  // Hiện ở TRÊN thay vì mặc định (dưới)
toast.show()
```

Tuy nhiên, với app có **TARGET API 30+** và **`Toast` chạy ở BACKGROUND**: `setGravity()` sẽ **không còn hiệu lực**, `Toast` sẽ luôn hiển thị ở **dưới màn hình**.

> _Hàm `setGravity()` chỉ đáng tin cậy khi **app đang ở FOREGROUND**._

### 1.3. **Custom Toast View** (_disabled from `API 30+`_)

```kotlin
// KHÔNG dùng trong code mới — dù VẪN compile được
val customView = layoutInflater.inflate(R.layout.custom_toast, null)
val toast = Toast(requireContext())

// custom view cho toast
toast.view = customView

toast.duration = Toast.LENGTH_LONG
toast.show()
```

Từ **Android 11 (_API 30_)** trở đi, **"Custom toast views blocked"**.<br/>
Tương tự như `setGravity()`, với **custom view, `Toast` chỉ hiển thị nếu app ở FOREGROUND**

> _Với `Toast`, chỉ nên dùng với **TEXT** thông thường qua `makeText()`. Nếu muốn hiển thị **custom view**, nên dùng `SnackBar` thay thế._

### 1.4. **Limitations** from **Android 12 (`API 31+`)**

Từ **API 31+**, `Toast` TỰ ĐỘNG:

- Giới hạn **TỐI ĐA 2 dòng Text** (_dài hơn sẽ bị CẮT, không kiểm soát được_)
- **Tự động kèm ICON của app** cạnh text, KHÔNG thể tắt.
- **Layout** chi tiết được **quyết định bởi hệ thống**, không thể custom.

> _**Hệ quả thực tế**: Vì các **giới hạn này ngày càng chặt** (API 30 chặn custom view, API 31 giới hạn text + tự thêm icon), **`Toast` ngày càng chỉ phù hợp cho thông báo TEXT NGẮN, ĐƠN GIẢN** — mọi nhu cầu phức tạp hơn nên chuyển sang `Snackbar`._

---

## 2. `Snackbar`

Tương tự `Toast`, component `Snackbar` cũng là **transient message** / **temporary notification**, nhưng mạnh hơn và có nhiều **tính năng hơn**.

### 2.1. **Create and display a `Snackbar`**

Để sử dụng `Snackbar`, cần dependency **Material Components library**:

```gradle
// build.gradle (app level)
dependencies {
    implementation 'com.google.android.material:material:1.11.0'
}
```

Các khởi tạo cơ bản - `Snackbar.make(view, text, duration)`:

```kotlin
Snackbar.make(
    binding.root,
    "Đã xóa contact",
    Snackbar.LENGTH_LONG
).show()
```

hàm `make(view, text, duration)` nhận vào **3 tham số**:

- `view`: **BẮT BUỘC**, phải là **`View` ĐANG hiển thị** (_foreground_) thật sự trên màn hình.
  - `Snackbar` dùng view để tìm ra **container** phù hợp (_thường tìm lên `CondinatorLayout` gần nhất_) để hiển thị **đúng vị trí** và **đúng hành vi** (_đẩy FAB lên, ..._).
  - Thường truyền **`binding.root` (root view)** của `Activity`/`Fragment` làm tham số.
- `text`: **nội dung hiển thị** trên `Snackbar`
- `duration`: **thời gian hiển thị**. Có 3 tuỳ chọn:
  - `Snackbar.LENGTH_SHORT` (2s)
  - `Snackbar.LENGTH_LONG` (3.5s)
  - `Snackbar.LENGTH_INDEFINITE`: là trạng thái **hiển thị vô thời hạn** (_cho tới khi code gọi `dismiss()` hoặc **BẮT BUỘC user phải tương tác** để dismiss_)

### 2.2. **Add action button to `Snackbar` - `setAction()`**

`setAction()` cho phép **thêm button** vào `Snackbar`, để user có thể **tương tác** với thông báo.

```kotlin
Snackbar
    .make( binding.root, "Đã xóa contact", Snackbar.LENGTH_LONG)
    .setAction("Hoàn tác") { viewModel.insertContact(deletedContact) }
    .show()
```

Với `setAction(actionText, listener)` — **CHỈ NÊN có 1 action DUY NHẤT**.

> _Nếu cảm thấy **cần 2+ hành động** cho user chọn → đây là **DẤU HIỆU nên chuyển sang `Dialog`**_

### 2.3. **Customize `Snackbar`**

#### **Custom Action color, background color — `setActionTextColor()`, `setBackgroundTint()`**

```kotlin
val snackbar = Snackbar.make(binding.root, "Lỗi kết nối", Snackbar.LENGTH_LONG)
    .setAction("Thử lại") { viewModel.retry() }

// custom màu cho action button và background
snackbar.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.yellow))
snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.red))

snackbar.show()
```

- `setBackgroundTint(color)`: custom **background color** của toàn bộ `Snackbar`
- `setActionTextColor(color)`: custom **text color** của **action button**

Muốn đổi màu **TEXT CHÍNH** (không phải action) → PHẢI tự **`findViewById()` vào bên trong `snackbar.view`**.

> _Đây là "mẹo" hay dùng vì Snackbar API KHÔNG có method setTextColor() trực tiếp cho phần text chính_

```kotlin
// take Snackbar view
val snackbarView = snackbar.view

// find TextView & set text color
val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
```

#### **Custom Snackbar layout — `setCustomView`** (_custom view_)

`Snackbar` **KHÔNG có `setView()` built-in** như `AlertDialog`, phải "hack" bằng cách **cast sang `SnackbarLayout` rồi `addView()` thủ công**

```kotlin
val snackbar = Snackbar.make(binding.root, "", Snackbar.LENGTH_LONG)
val customLayout = layoutInflater.inflate(R.layout.custom_snackbar, null)

val snackbarLayout = snackbar.view as Snackbar.SnackbarLayout
snackbarLayout.setPadding(0, 0, 0, 0)  // Xóa padding mặc định
snackbarLayout.addView(customLayout, 0)  // Chèn custom view vào

snackbar.show()
```

### 2.4. **Callback on `Snackbar` dismiss - `addCallback()`**

```kotlin
Snackbar.make(binding.root, "Đã xóa", Snackbar.LENGTH_LONG)
    .setAction("Hoàn tác") { viewModel.undoDelete() }
    .addCallback(object : Snackbar.Callback() {
        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
            super.onDismissed(transientBottomBar, event)
            when (event) {
                DISMISS_EVENT_ACTION -> {
                    // User đã bấm action ("Hoàn tác")
                }
                DISMISS_EVENT_TIMEOUT -> {
                    // Tự biến mất do hết thời gian — user KHÔNG bấm gì
                    // → Đây là lúc THỰC SỰ xóa hẳn (nếu dùng pattern
                    //   optimistic delete + undo)
                }
                DISMISS_EVENT_SWIPE -> {
                    // User vuốt để tắt sớm
                }
            }
        }
    })
    .show()
```

Các loại `event` trong `onDismissed()`:

- `DISMISS_EVENT_ACTION`: user bấm **action button** để đóng
- `DISMISS_EVENT_TIMEOUT`: tự biến mất do **hết thời gian** tự động
- `DISMISS_EVENT_SWIPE`: user **vuốt** để dismiss sớm
- `DISMISS_EVENT_MANUAL`: code tự gọi `dismiss()` để tắt
- `DISMISS_EVENT_CONSECUTIVE`: `Snackbar` bị **replace** bởi 1 `Snackbar` khác

Dùng `addCallback()` khi **cần biết CHÍNH XÁC "vì sao" Snackbar đóng**, không chỉ đơn giản "đã đóng" — hữu ích cho pattern Undo phức tạp cần phân biệt "user bấm Undo" và "user để tự trôi qua"

### 2.5. `Snackbar` vs `FloatingActionButton` (FAB)

```xml
<!-- Sử dụng CoordinatorLayout -->
<androidx.coordinatorlayout.widget.CoordinatorLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.recyclerview.widget.RecyclerView ... />

    <!-- FAB -->
    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fab"
        android:layout_gravity="bottom|end"
        android:layout_margin="16dp" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

Cơ chế: với `CoordinatorLayout`, và sử dụng `Snackbar`:

- Khi `Snackbar` xuất hiện, nó **TỰ ĐỘNG nhận diện FAB** đang có mặt trong cùng `CoordinatorLayout` và **FAB sẽ tự động di chuyển lên trên** đúng bằng chiều cao Snackbar để tránh bị che khuất.
- Đây là **hành vi built-in** của **Material Components** khi dùng `CoordinatorLayout`.

> _**Note**: với **`Snackbar` được gọi với view KHÔNG nằm trong `CoordinatorLayout`** (ví dụ chỉ là LinearLayout thường) — Snackbar VẪN hiển thị được, nhưng **KHÔNG có hành vi tự đẩy FAB** này._
