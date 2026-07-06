# **`RecyclerView` Overview**

## 1. `RecyclerView` vs `ListView`/`GridView`

**Trước `RecyclerView`**, Android chỉ có `ListView`/`GridView` — hoạt động nhưng có **vấn đề nghiêm trọng về hiệu năng** nếu code sai.

`ListView` — vấn đề cốt lõi: **KHÔNG BẮT BUỘC dùng `ViewHolder` pattern**

```kotlin
override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
    val view = inflater.inflate(R.layout.item, parent, false)  // ❌ Luôn inflate mới
    val tvName = view.findViewById<TextView>(R.id.tvName)      // ❌ findViewById mỗi lần scroll
    tvName.text = data[position].name
    return view
}
```

Hệ quả: khi scroll, `ListView` sẽ **inflate view mới** và **findViewById** mỗi lần, dẫn đến **giật, lag và tốn CPU nghiêm trọng**.

`RecyclerView` giải quyết bằng cách **BẮT BUỘC kiến trúc 3 thành phần tách biệt** rõ ràng: `LayoutManager`, `Adapter`, `ViewHolder`.

---

## 2. Cách **core components** của `RecyclerView`

```text
RecyclerView
├── LayoutManager    → "Sắp xếp item NHƯ THẾ NÀO" (dọc, ngang, lưới...)
├── Adapter          → "Data này BIẾN THÀNH view như thế nào"
└── ViewHolder        → "Giữ reference đến view của 1 item — TÁI SỬ DỤNG"
```

### 2.1. `RecyclerView` - **ViewGroup / container**

`RecyclerView` TỰ NÓ **không biết** cách **sắp xếp item**, không biết **data** là gì — nó chỉ là "cái khung" quản lý `scroll` + `recycling`.

```xml
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/recyclerView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

Phải cung cấp cho `RecyclerView` 2 thứ:

1. **`LayoutManager`**: set thông qua `recyclerView.layoutManager` đại diện cho **cách sắp xếp item**
2. **`Adapter`**: set thông qua `recyclerView.adapter` là adapter mapping giữa **data** và **view**.

### 2.2. `LayoutManager` - **Sắp xếp** item **NHƯ THẾ NÀO**

`LayoutManager` là **interface** định nghĩa cách **sắp xếp item** trong `RecyclerView`. Android cung cấp sẵn 3 `LayoutManager`:

- **Vertical** / **Horizontal** layout:

  ```kotlin
  // Danh sách dọc/ngang — phổ biến nhất
  recyclerView.layoutManager = LinearLayoutManager(context)   // VERTICAL
  recyclerView.layoutManager =                                // HORIZONTAL
      LinearLayoutManager(
          context,
          LinearLayoutManager.HORIZONTAL,
          false
      )
  ```

- **Grid** layout:

  ```kotlin
  // Lưới — số cột cố định
  recyclerView.layoutManager = GridLayoutManager(context, 2) // 2 cột
  ```

- **Staggered grid** layout:

  ```kotlin
  // Lưới "lệch" — số cột cố định
  // kiểu Pinterest, item cao thấp khác nhau
  recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
  ```

**Cơ chế quan trọng**: `LayoutManager` không chỉ "sắp xếp" — nó còn quyết định:

- **Khi nào 1 item cần được _recycle_** (khi item đó ra khỏi vùng hiển thị)
- **Khi nào cần request 1 view mới** từ `Adapter` (khi item mới cuộn vào vùng hiển thị).

### 2.3. `ViewHolder` - **Giữ reference đến view của 1 item**

`ViewHolder` là component có vai trò **giữ reference đến các `View` con** của 1 item layout — để **không phải gọi `findViewById()` lại mỗi lần item đó được recycle**.

```kotlin
// ViewHolder class
// extends RecyclerView.ViewHolder
class ContactViewHolder(private val binding: ItemContactBinding)
    : RecyclerView.ViewHolder(binding.root) {

    // Bind data vào view
    fun bind(contact: Contact) {
        binding.tvName.text = contact.name
        binding.tvPhone.text = contact.phone
    }
}
```

- `itemView` (tham ssoo truyền vào constructor của cha - `RecyclerView.ViewHolder`) là **root view của 1 item layout**.
- Với **ViewBinding**, sử dụng `binding.root` thay cho `itemView`.

### 2.4. `Adapter` - **bridge** giữa **data** và **view**

```kotlin
// Adapter class
// extends RecyclerView.Adapter<ViewHolder>
class ContactAdapter(private val contacts: List<Contact>)
    : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    // ViewHolder of Adapter
    // -> for 1 item layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount(): Int = contacts.size

    inner class ContactViewHolder(private val binding: ItemContactBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: Contact) {
            binding.tvName.text = contact.name
            binding.tvPhone.text = contact.phone
        }
    }
}
```

Với `Adapter`, bạn cần **BẮT BUỘC `override` 3 method**:

- `onCreateViewHolder(parent: ViewGroup, viewType: Int)` — Gọi khi **CẦN 1 ViewHolder MỚI** (do chưa có sẵn để recycle).
  > _Tại đây, chỉ **inflate layout**, **tạo ViewHolder**, KHÔNG bind data._
- `onBindViewHolder(holder: ViewHolder, position: Int)` — Gọi khi **CẦN bind data** vào 1 item `ViewHolder` (có thể là mới hoặc recycle).
  > _Tại đây, **bind data** vào view thông qua `holder`._
- `getItemCount()` — Trả về **số lượng item** trong data, `RecyclerView` cần dùng để biết **scrollable range**.

---

## 3. Cơ chế **Recycling**

Khác với `ListView`: **tạo toàn bộ view** hoặc **tạo mới view mỗi lần scroll** gây ảnh hưởng performance với list dài.

VỚi `RecycleView`, cơ chế **recycling** cho phép:

- Tạo **đủ số lượng** view **có thể hiển thị trên màn hình**.
- Và **một số view dự phòng** (để scroll mượt).

ex: màn hình hiển thị được **8 items**, `RecyclerView` sẽ tạo **8 (và 3-4 view dự phòng)**.

Nhờ vậy, dù list dài nhưng `RecyclerView` chỉ tạo một lượng nhỏ **`ViewHolder`** tồn tại trong RAM và **recycle** chúng khi cuộn, giúp **scroll mượt mà** và **tiết kiệm CPU**.

Kết quả:

- **Inflate layout** chỉ xảy ra ~10-12 lần (fill screen + buffer)
- **Scroll KHÔNG tạo thêm `ViewHolder`** mới, chỉ recycle các ViewHolder đã tồn tại.
- Giúp **"phẳng" về mặt hiệu năng** dù list rất dài.

---

## 4. `getItemViewType()` - **Đa dạng item layout**

Giả sử cần **render khác nhau với nhiều loại item**, example:

- **Message của MÌNH**: item nằm bên trái
- **Message của NGƯỜI KHÁC**: item nằm bên phải

Với `getItemViewType()`, có thể **handle this case** bằng cách **xác định loại Item** tại position đó.

```kotlin
class ChatAdapter(private val messages: List<Message>)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>()
        // sử dụng
{

    // Định nghĩa Item's types
    companion object {
        private const val TYPE_SENT = 1
        private const val TYPE_RECEIVED = 2
    }

    // Xác định loại item TẠI vị trí này —
    // gọi TRƯỚC onCreateViewHolder
    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isSentByMe) TYPE_SENT else TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // Tạo ViewHolder dựa trên viewType
        // -> cần định nghĩa nhiều ViewHolder ứng với layout cho type mong muốn
        return when (viewType) {
            TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                // send layout
                SentViewHolder(binding)
            }
            TYPE_RECEIVED -> {
                val binding = ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                // received layout
                ReceivedViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is SentViewHolder -> holder.bind(message)
            is ReceivedViewHolder -> holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    // định nghĩa các loại ViewHolder
    // ứng với layout cho type mong muốn
    class SentViewHolder(private val binding: ItemMessageSentBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) { binding.tvContent.text = message.content }
    }

    class ReceivedViewHolder(private val binding: ItemMessageReceivedBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) { binding.tvContent.text = message.content }
    }
}
```

> _Khi này, `Adapter` phải extend `RecyclerView.Adapter<RecyclerView.ViewHolder>` thay vì **Custom `ViewHolder`**._

Cơ chế **Recycle Pool** khi có **nhiều loại `viewType`**:

- `RecyclerView` tạo **pool riêng** cho mỗi loại `viewType` — giúp **recycle đúng loại view** khi cuộn.

  Khi cần tạo `ViewHolder` mới, `RecyclerView` sẽ **check pool** của loại `viewType`:
  - Nếu còn `ViewHolder` trong pool → **recycle**.
  - Nếu pool trống → **onCreateViewHolder** được gọi để **inflate mới**.
    ```kotlin
    // RecyclerView truyền đúng viewType vào onCreateViewHolder
    // eg. getItemViewType -> TYPE_DATE
    onCreateViewHolder(parent, TYPE_DATE);
    ```

- Mỗi **`viewType`'s pool** thường **giữ khoảng 5 view** mặc định.

  Có thể **custom pool size** cho từng `viewType` bằng `RecyclerView.RecycledViewPool`:

  ```kotlin
  // tạo pool -> RecyclerView.RecycledViewPool
  val pool = RecyclerView.RecycledViewPool();

  // set custom pool size cho từng viewType
  pool.setMaxRecycledViews(TYPE_ME, 20);
  pool.setMaxRecycledViews(TYPE_OTHER, 20);
  pool.setMaxRecycledViews(TYPE_DATE, 5);

  // set pool cho RecyclerView
  recyclerView.setRecycledViewPool(pool);
  ```

---

## 5. `notifyDataSetChanged()` — Cách cũ (**Tránh dùng**)

```kotlin
// Cách cũ — hay gặp trong tutorial lỗi thời
class ContactAdapter(private var contacts: List<Contact>) : RecyclerView.Adapter<...>() {

    fun updateData(newContacts: List<Contact>) {
        contacts = newContacts

        // notify
        notifyDataSetChanged()  // ❌ Nên tránh
    }
}
```

**Vấn đề** của `notifyDataSetChanged()`:

1. `notifyDataSetChanged()` báo cho `RecyclerView` rằng **TOÀN BỘ data đã thay đổi** → điều này làm cho `RecyclerView` thực hiện **rebind/redraw tất cả các item** đang hiển thị dù chỉ 1 item thay đổi.
2. Không hợp với **animation** — `RecyclerView` không biết item nào thay đổi, nên **không animate**.
3. **Tốn hiệu năng** không cần thiết với list lớn.

Cách khuyến nghị hiện tại: `notifyItemInserted()`, `notifyItemRemoved()`, `notifyItemChanged()` hoặc **DiffUtil**.

```kotlin
fun addContact(contact: Contact, position: Int) {
    contacts.add(position, contact)
    notifyItemInserted(position)     // Chỉ báo "1 item mới ở vị trí này"
}

fun removeContact(position: Int) {
    contacts.removeAt(position)
    notifyItemRemoved(position)      // Chỉ báo "1 item bị xóa"
}

fun updateContact(position: Int, contact: Contact) {
    contacts[position] = contact
    notifyItemChanged(position)      // Chỉ báo "item này đổi data"
}
```

Cách này chính xác hơn, **có animation tự nhiên** (insert/remove có transition mượt) — nhưng vẫn **PHẢI tự tính toán đúng `position` thủ công**, dễ sai nếu logic phức tạp.

> _Đây chính là lý do `DiffUtil`/`ListAdapter` ra đời._
