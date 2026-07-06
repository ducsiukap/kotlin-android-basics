# **Fragment Result API**

Các cách nhận kết quả từ `Fragment`: [Result Fragment](../../s6_fragments/result_fragment.md)

## Triển khai **Fragment Result API** cho `DialogFragment`

Nguyên lý: **`DialogFragment` gửi 1 Bundle kết quả LÊN `FragmentManager` chung** — `Fragment`/`Activity` cha lắng nghe (**subscribe**) FragmentManager đó theo 1 **"request key"** (String), KHÔNG cần implement interface.

### **`DialogFragment` - gửi kết quả qua `setFragmentResult()`**

```kotlin
class DeleteConfirmDialogFragment : DialogFragment() {

    companion object {
        const val REQUEST_KEY = "delete_confirm_request"
        const val RESULT_CONFIRMED = "result_confirmed"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Xóa contact")
            .setMessage("Bạn có chắc muốn xóa?")
            .setPositiveButton("Xóa") { _, _ ->
                // Gửi kết quả lên FragmentManager — KHÔNG cần
                // interface, KHÔNG cần onAttach() tìm listener
                setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(RESULT_CONFIRMED to true)
                )
            }
            .setNegativeButton("Hủy") { _, _ ->
                setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(RESULT_CONFIRMED to false)
                )
            }
            .create()
    }
}
```

Callback `setFragmentResult(requestKey, bundle)` là **function CÓ SẴN của `Fragment`**, gửi kết quả lên `FragmentManager` gắn với `requestKey`.

### **`Fragment`/`Activity` cha - lắng nghe kết quả qua `setFragmentResultListener()`**

```kotlin
class ContactListFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Đăng ký lắng nghe — PHẢI làm TRƯỚC khi show Dialog
        // (thường đặt trong onViewCreated, hoặc onCreate)
        childFragmentManager.setFragmentResultListener(
            DeleteConfirmDialogFragment.REQUEST_KEY,
            viewLifecycleOwner    // ← Lifecycle owner — callback tự
                                    //   ngưng nếu View đã destroy
        ) { requestKey, bundle ->
            val confirmed = bundle.getBoolean(DeleteConfirmDialogFragment.RESULT_CONFIRMED)
            if (confirmed) {
                viewModel.deleteContact(selectedContact)
            }
        }
    }

    private fun showDeleteDialog() {
        DeleteConfirmDialogFragment().show(childFragmentManager, "delete_confirm")
    }
}
```

Hàm `setFragmentResultListener(requestKey, lifecycleOwner, callback)` là **function CÓ SẴN của `FragmentManager`**, lắng nghe kết quả từ `DialogFragment` theo `requestKey`. Callback nhận vào **3 tham số**:

- `requestKey`: String — phải **KHỚP CHÍNH XÁC** với `requestKey` mà `DialogFragment` sử dụng khi gọi `setFragmentResult()`.
- `lifecycleOwner`: LifecycleOwner — thường là `viewLifecycleOwner` của Fragment, để callback tự ngưng khi View destroy.
- `listener`: lambda `(requestKey: String, bundle: Bundle) -> Unit` — callback nhận kết quả từ `DialogFragment` qua `bundle`.
