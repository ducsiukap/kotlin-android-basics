# **`Dialog` component**

## 1. **What** is the **`Dialog`**?

`Dialog` = **1 cửa sổ NHỎ, NỔI** (float) trên nội dung hiện tại của `Activity`:

- Không chiếm toàn màn hình
- Yêu cầu **user PHẢI tương tác** (hoặc tap ra ngoài để đóng) trước khi tiếp tục dùng app

**Usecase**:

- Confirm action
- Yêu cầu nhập liệu nhanh
- Hiển thị danh sách lựa chọn
- Báo lỗi/thành công **cần user xác nhận đã đọc**
- ...

---

## 2. **How** to use **`Dialog`**?

### 2.1. Vì sao **KHÔNG tự tạo `Dialog`** trực tiếp trong `Activity`

```kotlin
class MainActivity : AppCompatActivity() {

    private fun showDeleteDialog() {
        // ❌ Tạo Dialog TRỰC TIẾP, không qua DialogFragment
        AlertDialog.Builder(this)
            .setTitle("Xóa contact")
            .setMessage("Bạn có chắc muốn xóa?")
            .setPositiveButton("Xóa") { _, _ -> deleteContact() }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
```

Việc tự tạo **Dialog** trực tiếp với `AlertDialog.Builder` trong `Activity` gặp **vấn đề về lifecycel**:

- `AlertDialog.Builder` nhận vào `Context` của `Activity` (ở đây là `this`, tức là **instance của Activity hiện tại**)
- Khi **configuration change** xảy ra (ví dụ: xoay màn hình), `Activity` sẽ bị **destroy** và **recreate**, nếu đang hiển thị dialog:
  - `Activity` mới được tạo lại.
  - `AlertDialog` cũ vẫn tồn tại và **reference đến `Activity` cũ**

  Điều này gây ra `WindowManager$BadTokenException` — **crash app**, vì Dialog cố gắng attach vào 1 Window đã không còn tồn tại

  > _Đây là 1 trong những **crash PHỔ BIẾN NHẤT liên quan `Dialog`** trong lịch sử Android_

### 2.2. **Solution: `DialogFragment`**

**`DialogFragment` = 1 subclass ĐẶC BIỆT của Fragment**, được **thiết kế RIÊNG** để tạo và quản lý `Dialog`

> _Theo tài liệu chính thức: **`DialogFragment` giúp tránh `IllegalStateException` và leaked window crash**_

Vì là **subclass của `Fragment`**, `DialogFragment` được **THAM GIA** vào lifecycle của `Activity`/`Fragment` cha (_thông qua `FragmentManager`_), nên **tự động handle** các vấn đề về lifecycle:

- **Đóng Dialog** khi `Activity` bị destroy
- **Khôi phục Dialog** sau configuration change.
- **Quản lý toàn bộ `state` của Dialog** qua saved instance state như Fragment thông thường.

#### **Implement `DialogFragment` cơ bản - `onCreateDialog()`**

```kotlin
class DeleteConfirmDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Xóa contact")
            .setMessage("Bạn có chắc muốn xóa contact này?")
            .setPositiveButton("Xóa") { _, _ ->
                // Xử lý khi click xóa
            }
            .setNegativeButton("Hủy", null)
            .create()
    }

    companion object {
        const val TAG = "DeleteConfirmDialog"
    }
}
```

`onCreateDialog(savedInstanceState)` là **lifecycle callback** của `DialogFragment`, **bắt buộc `override`**:

- Tương tự `onCreateView()` của Fragment, nhưng **trả về 1 Dialog** thay vì View
- Callback này là **nơi duy nhất nên tạo Dialog**

#### **Show `DialogFragment` — dùng `show()`**

```kotlin
class ContactListFragment : Fragment() {

    private fun showDeleteConfirmDialog() {
        DeleteConfirmDialogFragment()
            .show(childFragmentManager, DeleteConfirmDialogFragment.TAG)
        //        ▲                     ▲
        //   FragmentManager        Tag — dùng để tìm lại Dialog sau này
    }
}
```

`show(fragmentManger, tag)` là **1 extension function CÓ SẴN** của `DialogFragment`, giúp:

- Tự tạo `FragmentTransaction` và **commit**
- Tự quản lý việc **show Dialog** lên màn hình

Về mặt kỹ thuật:

```kotlin
fragmentManager.beginTransaction()
    .add(dialogFragment, tag)
    .commit()
```

vẫn có thể hoạt động, nhưng **KHÔNG nên dùng**, vì `show()` đã **làm đúng, đủ mọi bước** và **handle** hết các vấn đề liên quan đến **lifecycle và transaction**.

### 2.3. `onCreateDialog()` vs `onCreateView()`

`DialogFragment` có thể hoạt động **vừa như `Dialog`**, **vừa như `Fragment`** nhúng bình thường (embeddable) tùy ngữ cảnh:

- `onCreateDialog(savedInstanceState): Dialog`: dùng khi cần hiển thị **dạng Dialog** (_`AlertDialog` hoặc tùy biến_), hầu hết trường hợp dùng callback này
- `onCreateView(inflater, container, savedInstanceState): View?`: dùng khi muốn **embed DialogFragment như 1 Fragment bình thường** trong layout (_ví dụ: responsive layout, hiện dialog ở mobile, side panel ở tablet_), lúc này **Dialog sẽ KHÔNG nổi lên**, mà **hiển thị trực tiếp trong layout cha**.

### 2.4. **Truyền data** vào `DialogFragment` — **dùng `arguments` Bundle**:

```kotlin
class DeleteConfirmDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_CONTACT_NAME = "contact_name"

        // Factory method — cách chuẩn để tạo instance CÓ DATA
        fun newInstance(contactName: String): DeleteConfirmDialogFragment {
            return DeleteConfirmDialogFragment().apply {
                arguments = bundleOf(ARG_CONTACT_NAME to contactName)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val contactName = arguments?.getString(ARG_CONTACT_NAME) ?: ""

        return AlertDialog.Builder(requireContext())
            .setTitle("Xóa contact")
            .setMessage("Bạn có chắc muốn xóa \"$contactName\"?")
            .setPositiveButton("Xóa") { _, _ -> listener?.onDeleteConfirmed() }
            .setNegativeButton("Hủy", null)
            .create()
    }
}
```

Khi này, caller gọi `newInstance(contactName)` để tạo `DialogFragment` với **data truyền vào** thay vì khởi tạo bằng constructor:

```kotlin
// Cách gọi — dùng factory method, KHÔNG gọi constructor trực tiếp
DeleteConfirmDialogFragment.newInstance(contact.name)
    .show(childFragmentManager, "delete_confirm")
```

### 2.5. **`DialogFragment`'s lifecycle**

```text
onAttach()          → Gắn vào Activity/Fragment cha, lấy listener
        ↓
onCreate()           → Khởi tạo (không liên quan UI)
        ↓
onCreateDialog()      → Tạo Dialog (THAY VÌ onCreateView() như
                         Fragment thường)
        ↓
Dialog hiển thị lên màn hình,
User tương tác (bấm nút, tap ra ngoài, bấm Back)
        ↓
onDismiss()  hoặc  onCancel()  → Dialog đóng lại
        ↓
onDestroyView()  →  onDestroy()  →  onDetach()
```

Điểm **khác biệt chính** so với `Fragment` nằm ở `onDismiss()` và `onCancel()`:

```kotlin
class DeleteConfirmDialogFragment : DialogFragment() {

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // Gọi khi Dialog đóng theo BẤT KỲ CÁCH NÀO — bấm nút,
        // tap ra ngoài, bấm Back, hoặc tự gọi dismiss()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        // CHỈ gọi khi Dialog bị đóng do user CHỦ ĐỘNG HỦY —
        // tap ra ngoài vùng Dialog, hoặc bấm nút Back
        // KHÔNG gọi nếu đóng bằng cách bấm 1 action button
        // (setPositiveButton/setNegativeButton)
    }
}
```

- `onCancel` **LUÔN kéo theo** `onDismiss`
- `onDismiss` **KHÔNG phải lúc nào cũng có `onCancel` đi kèm**

Vì vậy:

- Dùng `onDismiss()` nếu muốn **xử lý "dialog đã đóng, bất kể lý do gì"**
- Dùng `onCancel()` nếu CHỈ muốn **xử lý riêng trường hợp "user chủ động bỏ qua, không chọn action nào"**

### 2.6. `isCancelable` — cho phép **tap ra ngoài để dismiss dialog**

Có thể **kiểm soát việc đóng Dialog "ngoài ý muốn"** thông qua `setCancelable()`:

```kotlin
override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = AlertDialog.Builder(requireContext())
        .setTitle("Đang xử lý")
        .setMessage("Vui lòng đợi...")
        .create()

    isCancelable = false  // Property của DialogFragment — KHÔNG cho
                            // đóng bằng tap ra ngoài HAY bấm Back
    return dialog
}
```

Với `isCancelable = true` (**default**), user có thể dismiss Dialog bằng cách **tap ra ngoài vùng Dialog** hoặc **click Back**.

Ngược lại, `isCancelable = false` buộc **Dialog CHỈ đóng được qua action button**.

---

## 3. **`AlertDialog` structure**

```text
┌────────────────────────────────┐
│  Xóa contact                   │  ← Title (tùy chọn)
├────────────────────────────────┤
│                                │
│  Bạn có chắc muốn xóa contact  │  ← Content area (message,
│  này? Hành động không thể      │     list, hoặc custom layout)
│  hoàn tác.                     │
│                                │
├────────────────────────────────┤
│                 Hủy      Xóa   │  ← Action buttons (tối đa 3)
└────────────────────────────────┘
```

Trong đó:

- **Title** (_optional_): **CHỈ** nên dùng khi **content area có nội dung PHỨC TẠP** (_message dài, list, custom layout_)
  > _Với câu hỏi/thông báo đơn giản → KHÔNG CẦN title, dùng `setMessage()` là đủ_
- **Content**: có thể sử dụng:
  - `setMessage()` — hiển thị **text message**
  - `setItems()` — hiển thị **list lựa chọn** và chọn 1
  - `setMultiChoiceItems()` — hiển thị **list lựa chọn nhiều** và chọn nhiều
  - `setView()` — hiển thị **custom layout** (ví dụ: form nhập liệu)
- **Action buttons**: **TỐI ĐA 3 button**, gồm:
  - **`Positive` button** — hành động **CHÍNH / XÁC NHẬN** (ví dụ: Xóa, Đồng ý, Gửi)
  - **`Negative` button** — hành động **HỦY** (ví dụ: Hủy, Không)
  - **`Neutral` button** — hành động **TRUNG LẬP** (ví dụ: Xem thêm, Tìm hiểu thêm)

```kotlin
override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return AlertDialog.Builder(requireContext())
        // Title
        .setTitle("Cập nhật ứng dụng")
        // Content
        .setMessage("Phiên bản mới có nhiều cải tiến. Cập nhật ngay?")
        // Action buttons
        .setPositiveButton("Cập nhật") { _, _ -> /* mở store */ }
        .setNegativeButton("Không") { _, _ -> /* đóng dialog */ }
        .setNeutralButton("Để sau") { _, _ -> /* nhắc lại sau */ }
        .create()
}
```

> _`setXxxButton(text, listener)` có thể đặt tham số `listener=null` nếu **không cần xử lý gì**, hệ thống sẽ tự **dismiss dialog** khi click button._

---

## 4. `Dialog` báo kết quả về `Activity`/`Fragment` cha - **interface pattern**

### **Bước 1**: định nghĩa **interface** handle result

Trong `DialogFragment`, định nghĩa **interface**, trong đó có các callback để **xử lý kết quả**:

```kotlin
class DeleteConfirmDialogFragment : DialogFragment() {

    // Interface định nghĩa các "sự kiện" Dialog có thể phát ra
    interface DeleteConfirmListener {
        fun onDeleteConfirmed()
        fun onDeleteCancelled()
    }

    // Listener — được gán TỪ BÊN NGOÀI (Fragment/Activity cha)
    private var listener: DeleteConfirmListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // onAttach() — thời điểm Dialog "gắn" vào Activity/Fragment cha
        // Đây là nơi AN TOÀN để lấy reference đến listener

        listener = when {
            parentFragment is DeleteConfirmListener -> parentFragment as DeleteConfirmListener
            context is DeleteConfirmListener -> context
            else -> throw ClassCastException(
                "$context phải implement DeleteConfirmListener"
            )
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Xóa contact")
            .setMessage("Bạn có chắc muốn xóa?")
            .setPositiveButton("Xóa") { _, _ -> listener?.onDeleteConfirmed() }
            .setNegativeButton("Hủy") { _, _ -> listener?.onDeleteCancelled() }
            .create()
    }
}
```

`listener` được **gán trong `onAttach()`**, thời điểm **DialogFragment đã attach vào Activity/Fragment cha**, nên đảm bảo lấy **đúng `listener` hiện tại**.

> _Nếu truyền qua constructor, `listener` bị gán **`null`** khi recreate sau configuration change (khởi tạo fragment với **no-args constructor**), dẫn đến **`NullPointerException`** khi click button._

Trong `onAttach()` — `listener` được lấy từ `parentFragment` vs `context` — **khi nào dùng cái nào**?

- `parentFragment` — nếu `DialogFragment` được show từ **1 Fragment khác** (_dùng `childFragmentManager`_) -> listener nên là chính Fragment đó.
- `context` — nếu `DialogFragment` được show từ **Activity** (_dùng `supportFragmentManager`_) -> listener là Activity.

### **Bước 2**: `Activity`/`Fragment` cha **implement interface**:

Khi này, `Activity` hoặc `Fragment` cha cần **implement interface** để nhận kết quả từ Dialog:

```kotlin
// Fragment cha — PHẢI implement interface để nhận sự kiện
class ContactListFragment : Fragment(), DeleteConfirmDialogFragment.DeleteConfirmListener {

    private fun showDeleteDialog() {
        DeleteConfirmDialogFragment().show(childFragmentManager, "delete_confirm")
    }

    override fun onDeleteConfirmed() {
        viewModel.deleteContact(selectedContact)
    }

    override fun onDeleteCancelled() {
        // Không làm gì, hoặc log analytics
    }
}
```
