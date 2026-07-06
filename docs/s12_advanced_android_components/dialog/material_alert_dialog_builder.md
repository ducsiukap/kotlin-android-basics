# `MaterialAlertDialogBuilder`

`AlertDialog.Builder()` thường **không tự theo Material Design**, sử dụng **style mặc định từ `AppCompat`**:

```kotlin
// AlertDialog.Builder thường — style mặc định từ AppCompat,
// KHÔNG tự động áp dụng Material Design 3 (bo góc, màu sắc,
// typography theo Material Theme của app)
AlertDialog.Builder(requireContext())
    .setTitle("Xóa contact")
    .create()
```

> _**`MaterialAlertDialogBuilder` KẾ THỪA TRỰC TIẾP từ `AlertDialog.Builder`** — API giống HỆT 100% (setTitle, setMessage, setPositiveButton...) → **CHỈ khác ở STYLE hiển thị**, KHÔNG khác ở cách dùng_

```kotlin
// ✅ Từ ContactApp — đã dùng đúng cách này
MaterialAlertDialogBuilder(requireContext())
    .setTitle("Xóa contact")
    .setMessage("Xóa ${contact.name}?")
    .setPositiveButton("Xóa") { _, _ -> viewModel.deleteContact(contact) }
    .setNegativeButton("Hủy", null)
    .show()
```

**Khác biệt CHÍNH**:

|                   | `AlertDialog.Builder`                       |                                                                 | `MaterialAlertDialogBuilder` |
| ----------------- | ------------------------------------------- | --------------------------------------------------------------- | ---------------------------- |
| **Border radius** | Không / bo góc nhẹ                          | **Bo góc theo Material Shape Theme** (`shapeAppearance`)        |
| **Button color**  | Màu theo `colorAccent` cũ                   | **Màu button tự lấy từ `colorPrimary`** của Material Theme      |
| **Typography**    | Không, sử dụng Typography mặc định hệ thống | **Tự áp dụng `typography` theo Material Type Scale**            |
| App theme         | Kế thừa `Theme.AppCompat.*`                 | Kế thừa `Theme.Material3.*` (hoặc `Theme.MaterialComponents.*`) |

Có thể **custom style** riêng cho từng Dialog qua `R.style`:

```xml
<style name="DestructiveDialogStyle" parent="ThemeOverlay.MaterialComponents.MaterialAlertDialog">
    <item name="colorPrimary">@color/red</item>  <!-- Nút "Xóa" màu đỏ -->
</style>
```

```kotlin
MaterialAlertDialogBuilder(requireContext(), R.style.DestructiveDialogStyle)
    .setTitle("Xóa vĩnh viễn")
    .setMessage("Hành động này KHÔNG THỂ hoàn tác")
    .setPositiveButton("Xóa", null)
    .setNegativeButton("Hủy", null)
    .show()
```

> _Đây là cách chuẩn để tô **màu đỏ riêng cho Dialog "hành động nguy hiểm" (`destructive action`)** — theo đúng khuyến nghị Material Design: hành động xóa/không thể hoàn tác nên có màu cảnh báo khác biệt._
