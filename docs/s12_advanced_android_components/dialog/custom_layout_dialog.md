# **`Dialog`** with **custom layout**

## 1. **Custom layout** for `Dialog` - `setView()`

`AlertDialog.Builder` chuẩn không đủ cho **form nhập liệu**: `setMessage()` chỉ hiển thị **static text**, không thể hiển thị các thành phần tương tác như `EditText`, `CheckBox`, v.v.

Khi **cần NHÚNG 1 layout `XML`** tùy ý vào phần **Content Area** của `Dialog`, **`setView()` chính là API giải quyết**.

### 1.1. Chuẩn bị `layout` cho **Dialog**

```xml
<!-- res/layout/dialog_add_contact.xml -->
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp">

    <!-- EditText -->
    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Tên *">
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/et_name"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />
    </com.google.android.material.textfield.TextInputLayout>

    <!-- EditText -->
    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Số điện thoại *"
        android:layout_marginTop="12dp">
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/et_phone"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />
    </com.google.android.material.textfield.TextInputLayout>

</LinearLayout>
```

**Note**: với **`root` layout**:

- Không cần bọc `layout_width=match_parent` ra ngoài cùng giống `Fragment`/`Activity`, vì **`Dialog` TỰ ĐỘNG co giãn theo content bên trong**.
  > _Sử dụng `layout_width="match_parent"` ở root VẪN được — **Dialog sẽ tự giới hạn độ rộng theo màn hình** (không tràn ra ngoài)_
- Chỉ cần set `layout_height="wrap_content"` là đủ.

### 1.2. **Inflate layout** và gắn vào Dialog

```kotlin
class AddContactDialogFragment : DialogFragment() {

    private var _binding: DialogAddContactBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())

        // Inflate DÙNG layoutInflater của Activity cha (requireActivity())
        // — đảm bảo đúng theme/context nhất quán với toàn app
        _binding = DialogAddContactBinding.inflate(requireActivity().layoutInflater)

        return builder
            .setTitle("Thêm contact")
            .setView(binding.root)
            .setPositiveButton("Lưu", null)
            .setNegativeButton("Hủy") { _, _ -> dismiss() }
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null   // Tránh memory leak — pattern giống Fragment thường
    }
}
```

Sử dụng `requireActivity().layoutInflater` để **inflate layout** cho `Dialog` thay vì dùng trực tiếp `layoutInflater` của `DialogFragment` để **tránh sai theme, đảm bảo layout được inflate với đúng theme của Activity** (_bao gồm font, style, primary color, ..._)

### 1.3. **Đọc giá trị từ các `EditText` trong Dialog**

**`binding.etName.text.toString()` PHẢI đọc TRONG lambda của `setPositiveButton { }`** — tức là ĐÚNG lúc user bấm nút, KHÔNG đọc SỚM HƠN (_ví dụ ngay sau khi inflate, lúc đó EditText còn trống_)

```kotlin
override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    _binding = DialogAddContactBinding.inflate(requireActivity().layoutInflater)

    return AlertDialog.Builder(requireContext())
        .setTitle("Thêm contact")
        .setView(binding.root)
        .setPositiveButton("Lưu") { _, _ ->
            // ✅ Đọc giá trị TẠI THỜI ĐIỂM user bấm "Lưu"
            val name = binding.etName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()

            if (name.isBlank() || phone.isBlank()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            // Gọi callback để trả dữ liệu về
            // Activity/Fragment cha
            listener?.onContactAdded(name, phone)
        }
        .setNegativeButton("Hủy", null)
        .create()
}
```

### 1.4. Vấn đề **`setPositiveButton` tự động gọi `dismiss()` Dialog**

Hành vi mặc định của Dialog khi **user click positive button** là **tự động gọi `dismiss()`** ngay sau khi xử lý xong<br/>
Tuy nhiên, trong nhiều trường hợp, khi có **validate** và **validate trả về `false`**, ta muốn **Dialog còn mở**, chỉ hiện error text / toast.

**Giải pháp `override` hàm `setOnShowListener`**:

```kotlin
class AddContactDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddContactBinding.inflate(requireActivity().layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Thêm contact")
            .setView(binding.root)
            .setPositiveButton("Lưu", null)
            .setNegativeButton("Hủy") { _, _ -> dismiss() }
            .create()

        // setOnShowListener — chạy SAU KHI Dialog đã hiển thị lên màn hình,
        // lúc này các Button đã thực sự tồn tại để lấy reference
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = binding.etName.text.toString().trim()
                val phone = binding.etPhone.text.toString().trim()

                if (name.isBlank()) {
                    binding.etName.error = "Không được để trống"
                    return@setOnClickListener  // ← Dialog KHÔNG đóng, vì
                                                //   ta đã TỰ set listener,
                                                //   không dùng cơ chế mặc định
                }
                if (phone.isBlank()) {
                    binding.etPhone.error = "Không được để trống"
                    return@setOnClickListener
                }

                listener?.onContactAdded(name, phone)

                dismiss()  // Validate PASS → tự đóng Dialog thủ công
            }
        }

        return dialog
    }
}
```

---

## 2. **`List`-based Dialog**

### 2.1. `setItems()` — **Dialog hiển thị danh sách item**

Callback `setItems()` được sửu dụng với **Dialog hiển thị danh sách** items và **cho phép CHỌN 1 item**.<br/>

- `setItems()` nhận vào một **mảng các chuỗi** và trả về **index của item được chọn** (_không trả về giá trị item trực tiếp_)
- **Dialog tự động `dismiss()` ngay khi chọn**, không cần action buttons.

```kotlin
class SortOptionDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val options = arrayOf("Tên (A-Z)", "Tên (Z-A)", "Mới nhất", "Cũ nhất")

        return AlertDialog.Builder(requireContext())
            .setTitle("Sắp xếp theo")
            .setItems(options) { _, which ->
                // "which" là INDEX của item được chọn (0, 1, 2...)
                when (which) {
                    0 -> listener?.onSortSelected(SortType.NAME_ASC)
                    1 -> listener?.onSortSelected(SortType.NAME_DESC)
                    2 -> listener?.onSortSelected(SortType.NEWEST)
                    3 -> listener?.onSortSelected(SortType.OLDEST)
                }
                // Dialog TỰ ĐỘNG đóng ngay sau khi chọn — không cần
                // gọi dismiss() thủ công, không cần nút "OK"
            }
            .create()
    }
}
```

> _Note: `which` là **index của item được chọn**, KHÔNG phải giá trị item trực tiếp, dùng `options[which]` để lấy item._

Phù hợp cho **quick menu** như _sort, filter đơn giản, ..._

### 2.2.`setSingleChoiceItems()` — **Dialog hiển thị danh sách item với radio button**

Dialog với `setSingleChoiceItems()` cho phép :

- **Hiển thị danh sách và RADIO BUTTON cho từng item**
- Đồng thời cho phép **chọn 1 item** trong danh sách
- Và có **action buttons** để **Xác nhận / Hủy bỏ** lựa chọn.

```kotlin
class SortOptionDialogFragment : DialogFragment() {

    private var selectedIndex = 0  // Lưu tạm lựa chọn hiện tại

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val options = arrayOf("Tên (A-Z)", "Tên (Z-A)", "Mới nhất", "Cũ nhất")

        return AlertDialog.Builder(requireContext())
            .setTitle("Sắp xếp theo")
            .setSingleChoiceItems(options, selectedIndex) { _, which ->
                // Gọi MỖI KHI user tap vào 1 radio button khác
                // — CHƯA xác nhận, chỉ lưu tạm
                selectedIndex = which
            }
            .setPositiveButton("Áp dụng") { _, _ ->
                // Xác nhận THẬT SỰ khi bấm "Áp dụng"
                listener?.onSortSelected(mapIndexToSortType(selectedIndex))
            }
            .setNegativeButton("Hủy", null)
            .create()
    }
}
```

`setSingleChoiceItems(items, checkedItemIndex, listener)` nhận vào 3 tham số:

- `items`: mảng các item hiển thị
- `checkedItemIndex`: index của item **đang được check** mặc định khi Dialog hiển thị
- `listener`: callback **MỖI KHI user chọn 1 item**, trong đó có thông tin về **`which` - index của item được chọn**

Phân biệt với `setItems()`:

- `setItems()` → **chọn 1 item và tự động đóng Dialog**, không có nút xác nhận
- `setSingleChoiceItems()` → **chọn item chỉ là ĐÁNH DẤU**, phải xác nhận trực tiếp qua **action button**.

### 2.3. `setMultiChoiceItems()` — **Dialog hiển thị danh sách item với checkbox**

Dialog với `setMultiChoiceItems()` cho phép:

- **Hiển thị danh sách và CHECKBOX cho từng item**
- Đồng thời cho phép **chọn NHIỀU item** trong danh sách
- Và có **action buttons** để **Xác nhận / Hủy bỏ** lựa chọn.

```kotlin
class FilterDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val categories = arrayOf("Gia đình", "Công việc", "Bạn bè", "Khác")

        // Mảng Boolean — đánh dấu SẴN category nào đang được chọn
        val checkedItems = booleanArrayOf(true, false, true, false)

        // Dùng MutableSet để track lựa chọn hiện tại (dùng index)
        val selectedIndices = mutableSetOf<Int>().apply {
            checkedItems.forEachIndexed { index, checked ->
                if (checked) add(index)
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Lọc theo danh mục")
            .setMultiChoiceItems(categories, checkedItems) { _, which, isChecked ->
                // Gọi MỖI LẦN user tick/untick 1 checkbox
                if (isChecked) selectedIndices.add(which)
                else selectedIndices.remove(which)
            }
            .setPositiveButton("Áp dụng") { _, _ ->
                val selectedCategories = selectedIndices.map { categories[it] }
                listener?.onCategoriesSelected(selectedCategories)
            }
            .setNegativeButton("Hủy", null)
            .create()
    }
}
```

`setMultiChoiceItems(items, checkedItems, listener)` nhận vào 3 tham số:

- `items`: mảng các item hiển thị
- `checkedItems`: **mảng Boolean** đánh dấu **những item nào đang được check** mặc định khi Dialog hiển thị
- `listener`: callback **MỖI KHI user tick/untick 1 checkbox**, trong đó có thông tin về **`which` - index của item được chọn** và **`isChecked` - trạng thái mới của checkbox**.
  > _`isChecked = true` → user tick checkbox, `isChecked = false` → user untick checkbox_
