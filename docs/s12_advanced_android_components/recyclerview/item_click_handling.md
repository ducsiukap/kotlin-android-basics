# **Click listener** pattern & **`ItemTouchHelper`**

## 1. **Click Listener** pattern

Vấn đề: **"click listener trong `RecyclerView` nên đặt Ở ĐÂU?"**

> _Có thể đơn giản gọi `setOnClickListerner`. Nhưng, vì **`RecyclerView` có cơ chế recycle**, việc đặt listener sai chỗ dẫn đến bug rất khó phát hiện — click nhầm item, click gọi sai data._

**3 vị trí** có thể đặt click listener:

1. `onCreateViewHolder()` - **SAI, KHÔNG nên đặt**
2. `onBindViewHolder()` - **ĐÚNG, phổ biến**
3. Trong `bind()` cỉa `ViewHolder` (_được gọi từ `onBindViewHolder()`_) - **ĐÚNG, phổ biến & clean**

### 1.1 Đặt **listener** trong `onCreateViewHolder()` - **LỖI phổ biến**

```kotlin
class ContactAdapter(
    private val contacts: List<Contact>,

    // callback xử lý action click item
    // (và maybe truyền data item ra ngoài)
    private val onItemClick: (Contact) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = ContactViewHolder(binding)

        // ❌ Đặt click listener Ở ĐÂY,
        // dùng "position" truyền vào lúc này
        binding.root.setOnClickListener {
            val position = holder.adapterPosition   // hoặc tệ hơn: capture
                                                    // biến position cố định
            onItemClick(contacts[position])
        }

        return holder
    }
    // ...
}
```

Việc **fix cứng `position`** trong `onCreateViewHolder()` là **sai lầm phổ biến**. Vì `RecyclerView` có cơ chế **recycle**, khi scroll, các item được **tái sử dụng**, dẫn đến việc click nhầm item hoặc click gọi sai data. Cụ thể:

- `onCreateViewHolder()` chỉ gọi ~10-12 lần (_đủ lấp màn hình + buffer_)
- `onBindViewHolder()` được gọi **RẤT NHIỀU LẦN** (_mỗi lần recycle_)

Vì **`position` bị fix cứng** ngay từ khi tạo `ViewHolder` dẫn tới việc **khi reuse** (_`onBindViewHolder()`_) lại `ViewHolder` đó, **`position` thực tế đã thay đổi nhưng click listener vẫn được gắn trên position cũ** dẫn tới vấn đề click sai item.

### 1.2. Dùng `holder.bindingAdapterPosition`

`holder.bindingAdapterPosition` là **position** được đọc tại **THỜI ĐIỂM GỌI**.

- `holder.adapterPosition`: **deprecated**
- `holder.bindingAdapterPosition`: **recommended**, vị trí trong `Adapter` mà `ViewHolder` này đang **được bind**
  > _Dùng khi chỉ có **1 `Adapter` bind vào `RecyclerView`**._
- `holder.absoluteAdapterPosition`: **vị trí TUYỆT ĐỐI** trong toàn bộ **`RecyclerView`**, phù hợp hơn khi có nhiều `Adapter` bind vào.

```kotlin
override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
    val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    val holder = ContactViewHolder(binding)

    binding.root.setOnClickListener {
        // ✅ ĐÚNG — đọc position NGAY TẠI THỜI ĐIỂM CLICK xảy ra,
        // không phải lúc tạo ViewHolder
        val currentPosition = holder.bindingAdapterPosition
        if (currentPosition != RecyclerView.NO_POSITION) {
            // RecyclerView.NO_POSITION = -1,
            // nghĩa là item vừa bị remove khỏi adapter nhưng ViewHolder vẫn còn
            // cần check để tránh list[currentPosition] có thể throw IndexOutOfBoundsException
            onItemClick(contacts[currentPosition])
        }
    }

    return holder
}
```

Dù có thể khiến việc **đặt listener trong `onCreateViewHolder()`** hoạt động đúng hơn nhưng **vẫn không được khuyến khích**.

### 1.3. Đặt **listener** trong `onBindViewHolder()` hoặc `bind()` (_của Adapter_)- **ĐÚNG, phổ biến**

```kotlin
class ContactAdapter(
    private val onItemClick: (Contact) -> Unit,
    private val onDeleteClick: (Contact) -> Unit
) : ListAdapter<Contact, ContactAdapter.ContactViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)   // ❌ KHÔNG set listener ở đây
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))      // ✅ Set listener BÊN TRONG bind()
    }

    inner class ContactViewHolder(
        private val binding: ItemContactBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: Contact) {
            binding.tvName.text = contact.name
            binding.tvPhone.text = contact.phone

            // ✅ Set listener TẠI ĐÂY — mỗi lần bind() chạy,
            // "contact" LUÔN LÀ data ĐÚNG của lần bind này
            binding.root.setOnClickListener {
                onItemClick(contact)   // "contact" là tham số của bind(),
                                        // KHÔNG PHẢI biến toàn cục có thể
                                        // bị đổi giá trị — luôn đúng data
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(contact)
            }
        }
    }
}
```

Pattern này vừa khiến code **clean hơn**, bởi:

- Các function hoạt động đúng với context của nó:
  - `onCreateViewHolder()` chỉ tạo `ViewHolder`
  - `onBindViewHolder()` gọi hàm bind data vào `ViewHolder`
  - `Adapter.bind()` chịu trách nhiệm chính trong việc **bind data** và **set up listeners**

lại vừa **an toàn hơn**:

- Không sử dụng tới `bindingAdapterPosition` hay `absoluteAdapterPosition`.
- `position` được truyền vào hàm `onBindViewHolder()` là **position chính xác** của item trong adapter **tại thời điểm bind**.
- Hàm `bind()` xử lý trên **object data**, không phụ thuộc vào `position`.

### 1.4. Handle **nhiều loại click** trên cùng item

Khi này:

- `binding.root` đại diện cho **TOÀN BỘ item**
- `binding.btnDelete` đại diện cho **1 sub-view cụ thể** trong item

```kotlin
fun bind(contact: Contact) {
    binding.tvName.text = contact.name
    binding.tvPhone.text = contact.phone

    // Click vào CẢ ITEM (root layout) → xem chi tiết
    binding.root.setOnClickListener {
        onItemClick(contact)
    }

    // Click vào NÚT XÓA (con bên trong) → xóa, KHÔNG trigger click item
    binding.btnDelete.setOnClickListener {
        onDeleteClick(contact)
    }
}
```

Cơ chế của **Android** tự xử lý: **Android View system tự "chặn" (`consume`) touch event ở View con** trước:

> Nếu **sub-view đã handle** (_`onClick` return true_), touch event sẽ **KHÔNG được bubble up** lên View cha.

Nhờ vậy:

- click vào `btnDelete` -> chỉ chạy `onDeleteClick()`, không chạy `onItemClick()`
- Click vào **phần còn lại của item** -> chỉ chạy `onItemClick()`, không chạy `onDeleteClick()`

### 1.5. **Long click** — Context Menu / Selection Mode

```kotlin
fun bind(contact: Contact) {
    binding.tvName.text = contact.name

    binding.root.setOnClickListener {
        onItemClick(contact)
    }

    // Long click — trả về Boolean để báo "ĐÃ XỬ LÝ event này"
    binding.root.setOnLongClickListener {
        onItemLongClick(contact)
        true  // ← BẮT BUỘC return true nếu đã xử lý
              //   (false sẽ khiến Android coi như "chưa xử lý gì",
              //   có thể trigger thêm hành vi mặc định khác)
    }
}
```

Usecases thực tế:

- Hiện context menu (_delete, share, edit, ..._) khi long click
- Kích hoạt **selection mode** (_chọn nhiều item để xóa, share, ..._): **long press item đầu để bắt đầu chọn**

---

## 2. **`ItemTouchHelper`** — Swipe-to-Delete & Drag-to-Reorder

### 2.1. **What** is the **`ItemTouchHelper`**?

`ItemTouchHelper` là **class tiện ích** có sẵn của `RecyclerView`, chuyên xử lý **2 loại tương tác** phổ biến và phức tạp:

- **SWIPE**: vuốt item sang trái/phải → _thường dùng để XÓA_
- **DRAG**: kéo thả item → _thường dùng để SẮP XẾP lại danh sách_

Nếu không có `ItemTouchHelper`, phải:

- Bắt sự kiện **touch** (_`onTouchEvent()`_) của `RecyclerView` và xác định loại action (_ACtION_DOWN, ACTION_MOVE, ACTION_UP, ..._)
- Tự tính toán **khoảng cách vuốt**, **ngưỡng** để được coi là đã vuốt đủ.
- Tự vẽ **animation di chuyển** item theo ngón tay.
- Tự xử lý **collision detection** khi kéo item.

> _`ItemTouchHelper` đã **làm sẵn những phần phức tạp trên**, việc còn lại chỉ là **implement callback** tương ứng._

### 2.2. `ItemTouchHelper.SimpleCallback`

```kotlin
class SwipeToDeleteCallback(
    private val onSwiped: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(
    0,                                              // dragDirs — 0 = KHÔNG cho phép drag
    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT   // swipeDirs — cho phép
                                                    // vuốt cả 2 hướng
) {

    // call back cho DRAG
    // — KHÔNG dùng, nên trả về false
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        // Dùng cho DRAG — vì chỉ làm swipe nên method này KHÔNG dùng,
        // trả về false
        return false
    }

    // call back cho SWIPE
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // ĐƯỢC GỌI khi user đã vuốt ĐỦ xa (qua ngưỡng) — coi như
        // "xác nhận" hành động vuốt

        // bindingAdapterPosition — TƯƠNG TỰ đã học ở phần Click Listener
        val position = viewHolder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            onSwiped(position)
        }
    }
}
```

Constructor `ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs)` nhận vào **2 tham số**:

- `dragDirs`: **hướng drag** được phép (_`ItemTouchHelper.UP`, `DOWN`, `LEFT`, `RIGHT_`).
  - Có thể **kết hợp nhiều direction** bằng `or`. <br/>
    eg. `ItemTouchHelper.UP or ItemTouchHelper.DOWN` → _cho phép **drag lên/xuống**_.
  - Đặt là **0** nếu **không cho phép drag**.
- `swipeDirs`: **hướng swipe** được phép (_`ItemTouchHelper.LEFT`, `RIGHT`_).
  - Có thể **kết hợp nhiều direction** bằng `or`. <br/>
    eg. `ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT` → _cho phép **vuốt trái/phải**_.
  - Đặt là **0** nếu **không cho phép swipe**.

### 2.3. **Attach** `ItemTouchHelper` vào `RecyclerView`

```kotlin
class ContactListFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeToDelete()   // Gọi hàm setup riêng
    }

    // định nghĩa hàm setup Swipe-to-Delete
    private fun setupSwipeToDelete() {
        val swipeCallback = SwipeToDeleteCallback { position ->
            val contact = adapter.currentList[position]  // Lấy data tại vị trí
            viewModel.deleteContact(contact)             // Gọi ViewModel xóa
        }

        // ItemTouchHelper BỌC callback,
        val itemTouchHelper = ItemTouchHelper(swipeCallback)
        // rồi "gắn" vào RecyclerView
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }
}
```

> _Lưu ý quan trọng về **data flow**: **`onSwiped()` chỉ báo "user đã vuốt xong item ở vị trí X" — nó KHÔNG tự động xóa item khỏi Room/data source**. Bạn PHẢI tự gọi `viewModel.deleteContact()` (hoặc tương đương) để thực sự xóa data.<br/>_
> _Nếu quên, **`submitList()` ở lần collect tiếp theo sẽ hiện lại item đó** (vì data source chưa đổi gì), gây cảm giác "xóa không có tác dụng" hoặc tệ hơn — animation xóa xong rồi item "quay lại"._

### 2.4. `onChildDraw()` — **background** khi vuốt

Mặc định, `ItemTouchHelper` chỉ làm item trượt theo ngón tay — **không có background**.<br/>
_eg. red background + icon thùng rác_

Có thể **tự vẽ background** bằng `onChildDraw()`.

```kotlin
class SwipeToDeleteCallback(
    private val context: Context,
    private val onSwiped: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    // Chuẩn bị sẵn các Drawable/Paint TRƯỚC — tránh tạo lại mỗi frame
    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete)!!
    private val background = ColorDrawable(Color.RED)
    private val intrinsicWidth = deleteIcon.intrinsicWidth
    private val intrinsicHeight = deleteIcon.intrinsicHeight

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            onSwiped(position)
        }
    }

    // ĐƯỢC GỌI LIÊN TỤC trong lúc user đang vuốt (mỗi frame)
    // — đây là nơi TỰ VẼ nền đỏ + icon xóa
    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,          // Khoảng cách item đã di chuyển ngang
                             // (âm = vuốt trái, dương = vuốt phải)
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top

        // 1. Vẽ nền đỏ — full chiều rộng phần đã vuốt qua
        background.setBounds(
            itemView.left, itemView.top, itemView.right, itemView.bottom
        )
        background.draw(c)

        // 2. Vẽ icon thùng rác — canh giữa theo chiều dọc, lề theo
        //    hướng vuốt
        val iconMargin = (itemHeight - intrinsicHeight) / 2
        val iconTop = itemView.top + iconMargin
        val iconBottom = iconTop + intrinsicHeight

        if (dX > 0) {  // Vuốt sang PHẢI — icon bên trái
            val iconLeft = itemView.left + iconMargin
            val iconRight = iconLeft + intrinsicWidth
            deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        } else if (dX < 0) {  // Vuốt sang TRÁI — icon bên phải
            val iconRight = itemView.right - iconMargin
            val iconLeft = iconRight - intrinsicWidth
            deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        }
        deleteIcon.draw(c)

        // 3. GỌI SUPER để RecyclerView tự làm phần "item trượt theo tay"
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
```

> _**`super.onChildDraw()` bắt buộc gọi ở CUỐI cùng** — nếu quên, item sẽ không tự di chuyển theo ngón tay nữa (chỉ có nền đỏ + icon xuất hiện, item đứng yên)._

### 2.5. **Drag-to-Reorder**

```kotlin
class DragToReorderCallback(

    // callback swap / reorder items in range (fromPosition - toPosition)
    private val onItemMoved: (fromPosition: Int, toPosition: Int) -> Unit

) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN,  // dragDirs — kéo dọc
    0                                              // swipeDirs — KHÔNG cho vuốt
) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        // ĐƯỢC GỌI khi item đang kéo "chạm" vào 1 item khác
        val fromPosition = viewHolder.bindingAdapterPosition
        val toPosition = target.bindingAdapterPosition

        if (fromPosition != RecyclerView.NO_POSITION &&
            toPosition != RecyclerView.NO_POSITION) {
            onItemMoved(fromPosition, toPosition)
        }

        return true  // ← return true = "ĐÃ XỬ LÝ, cho phép hoán đổi vị trí"
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Không dùng cho drag-only — để trống
    }
}
```

> _Ở `Adapter`, cần có hàm định nghĩa **reorder nội bộ** để truyền vào `DragReorderCallback`_.

### 2.6. Additions...

#### **Kết hợp cả Drag & Swipe** 
```kotlin
class CombinedCallback(
    private val onSwiped: (Int) -> Unit,
    private val onMoved: (Int, Int) -> Unit
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN,     // drag dọc
    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT   // swipe ngang
) {
    override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        onMoved(vh.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        onSwiped(viewHolder.bindingAdapterPosition)
    }
}
```

#### **optimistic delete** - xóa lạc quan, cho phép **undo**

```kotlin
private fun setupSwipeToDelete() {
    val swipeCallback = SwipeToDeleteCallback(requireContext()) { position ->
        val contact = adapter.currentList[position]

        // Xóa ngay lập tức khỏi UI (DiffUtil sẽ tự animate)
        viewModel.deleteContact(contact)

        // Cho phép user HOÀN TÁC trong 3 giây
        Snackbar.make(binding.root, "Đã xóa ${contact.name}", Snackbar.LENGTH_LONG)
            .setAction("Hoàn tác") {
                viewModel.insertContact(contact)  // Thêm lại nếu user bấm Undo
            }
            .show()
    }

    ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerView)
}
```