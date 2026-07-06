# `BottomSheetDialogFragment` in Android

`BottomSheetDialogFragment` là 1 **subclass ĐẶC BIỆT của `DialogFragment`**: <br/>
→ Thay vì Dialog NỔI GIỮA màn hình, nội dung **"trượt lên" từ CẠNH DƯỚI màn hình** — pattern rất phổ biến trong Material Design (menu chia sẻ, action sheet, chọn tùy chọn...)

Để implement, cần kết thừa `BottomSheetDialogFragment` thay vì `DialogFragment`, và override `onCreateView()` để trả về **custom layout**.

```xml
<!-- res/layout/bottom_sheet_contact_options.xml -->
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:id="@+id/option_call"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="12dp"
        android:text="📞 Gọi điện" />

    <TextView
        android:id="@+id/option_edit"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="12dp"
        android:text="✏️ Sửa" />

    <TextView
        android:id="@+id/option_delete"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="12dp"
        android:text="🗑️ Xóa" />

</LinearLayout>
```

```kotlin
class ContactOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetContactOptionsBinding? = null
    private val binding get() = _binding!!

    // Override onCreateView() — GIỐNG Fragment thông thường,
    // KHÔNG dùng onCreateDialog() như AlertDialog
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetContactOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Override onViewCreated() —
    // KHÔNG dùng onCreateDialog()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionCall.setOnClickListener {
            setFragmentResult("contact_option", bundleOf("action" to "call"))
            dismiss()
        }
        binding.optionEdit.setOnClickListener {
            setFragmentResult("contact_option", bundleOf("action" to "edit"))
            dismiss()
        }
        binding.optionDelete.setOnClickListener {
            setFragmentResult("contact_option", bundleOf("action" to "delete"))
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ContactOptionsBottomSheet"
    }
}
```

Điểm **khác biệt CỐT LÕI** với **`AlertDialog`-based `DialogFragment`**:

- **AlertDialog-based `DialogFragment`** → `override onCreateDialog()` — trả về Dialog
- **`BottomSheetDialogFragment`** → `override onCreateView()` — trả về View, GIỐNG Fragment thông thường
  > _Class cha (BottomSheetDialogFragment) **TỰ ĐỘNG "bọc" View đó vào 1 Dialog được style theo dạng bottom sheet**_

Để hiển thị, dùng `show()` giống `DialogFragment` thông thường:

```kotlin
private fun showContactOptions() {
    ContactOptionsBottomSheet().show(childFragmentManager, ContactOptionsBottomSheet.TAG)
}
```

Có thể **cấu hình hành vi** — Trạng thái **Collapsed**/**Expanded**/**Hidden**:

```kotlin
class ContactOptionsBottomSheet : BottomSheetDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Truy cập BottomSheetBehavior để tùy chỉnh hành vi
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED  // Mở RỘNG NGAY, thay vì bắt đầu
                                                          // ở trạng thái  "half-expanded"
            isDraggable = true   // Cho phép kéo tay để đóng/mở rộng
            skipCollapsed = true // Bỏ qua trạng thái "collapsed" (thu nhỏ 1 phần) — kéo xuống là ĐÓNG LUÔN,
                                  // không dừng ở trạng thái lửng chừng
        }
    }
}
```

Với `state`, có **3 trạng thái** chính:

- `STATE_EXPANDED` — Mở rộng **toàn bộ nội dung**
- `STATE_COLLAPSED` — chỉ hiện **MỘT PHẦN** nội dung
- `STATE_HIDDEN` — Ẩn hoàn toàn (tương tự dismiss)

Với `skipCollapsed = true`, khi kéo xuống **dưới ngưỡng**, bottom sheet sẽ **bỏ qua trạng thái collapsed** và **đóng luôn**.
