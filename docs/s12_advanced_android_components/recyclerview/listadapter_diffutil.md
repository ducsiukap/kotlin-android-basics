# **`RecyclerView`'s adapter**: `ListAdapter` + `DiffUtil`

## 1. **`notifyDataSetChanged()` / `notifyItem...()` problems**

Với `notifyDataSetChanged()`, khi được gọi, `RecyclerView` cần **rebind lại TẤT CẢ các items** trong danh dách, _**ngay cả khi chỉ có 1 item thay đổi**_. <br/>

> _Điều này dẫn tới vấn đề lớn về **performance**, đồng thời gây **lãng phí tài nguyên**, đặc biệt là khi danh sách có nhiều item và **KHÔNG hoạt động tốt với `animation`**._

Với `notifyItemInserted()` / `notifyRemoved()` / `notifyItemChanged()`, cso một số **vấn đề lớn khi áp dụng thực tế**:

- **Vấn đề 1**: phải **TỰ TÍNH `position` chính xác** một cách thủ công:

  ```kotlin
  fun removeContact(position: Int) {
      contacts.removeAt(position)
      notifyItemRemoved(position) // tự tính toán position
  }
  ```

  Khi `position` bị **truyền SAI** (_do async, list bị sửa ở nơi khác, ..._) có thể dẫn tới **app CRASH** hoặc **`RecyclerView` hiển thị SAI data**

- **Vấn đề 2**: với `data` tới từ `Flow`/`API`, **không biết chính xác CÁI GÌ đổi**:

  > eg. **Room** emit ra **`List<Contact>` mới hoàn toàn** khi có thay đổi (_không phải "thêm/xóa 1 item" tường minh_) vì **`Flow` luôn trả TOÀN BỘ list mới** thay vì chỉ ra "item nào thay đổi".

  Vì vậy, **không có cách nào** gọi `notifyItemInserted()` (_hoặc tương đương_) với đúng `position` của item thay đổi.

- **Vấn đề 3**: không hoạt động tốt với `sort`/`filter`/`search`/...<br/>
  Với mỗi **chức năng mới** (sort/filter/search), phải tự viết lại logic tính toán position đúng với operation đó khiến code rời rạc, dễ bug, khó maintain.

---

## 2. `DiffUtil`

### 2.1. **What** is the **`DiffUtil`**?

`DiffUtil` là một **utility class** / **thuật toán (`algorithm`)** so sánh **2 danh sách** và tính toán **sự khác biệt (`diff`)** giữa chúng. <br/>

- **Input**: `DiffUltil` nhận vào **2 danh sách**: `oldList` và `newList`
  - `oldList` là danh sách **hiện tại** đang hiển thị trong `RecyclerView`
  - `newList` là danh sách **mới** muốn hiển thị
- **Output**: `DiffUtil` trả về một **tập hợp các operations** cần làm để **biến `oldList` thành `newList`** (_thêm/xóa/sửa item, ..._). <br/>
  - Các operations này được gọi là **`DiffResult`** và có thể được dùng để **update `RecyclerView`** một cách **hiệu quả**.
  - ví dụ:
    - "`notifyItemInserted(3)`" nếu item mới được thêm vào vị trí 3
    - "`notifyItemRemoved(5)`" nếu item ở vị trí 5 bị xóa
    - "`notifyItemChanged(2)`" nếu item ở vị trí 2 bị thay đổi
    - ...

**Thuật toán của `DiffUtil` dựa trên `Eugene W. Myers’ difference algorithm`** (_được phát triển bởi Eugene W. Myers năm 1986, tương tự `git` để tính diff giữa 2 version của file_) để **tính khoảng cách chỉnh sửa ngắn nhất** giữa 2 danh sách.

### 2.2. `DiffUtil.ItemCallback<T>` (**bắt buộc**)

Để `DiffUtil` biết cách **so sánh `2 object` cùng kiểu `T`**, phải cung cấp cho nó **2 quy tắc so sánh** — thông qua việc `implement DiffUtil.ItemCallback<T>`:

```kotlin
class ContactDiffCallback : DiffUtil.ItemCallback<Contact>() {

    override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
        // Câu hỏi: "2 item này CÓ PHẢI LÀ CÙNG 1 THỰC THỂ không?"
        // (không phải "nội dung có giống nhau", mà là "có phải
        // là CÙNG 1 record trong đời thực" — dựa vào ID/khóa duy nhất)
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
        // Câu hỏi: "Nếu ĐÃ LÀ cùng 1 thực thể (areItemsTheSame = true),
        // thì NỘI DUNG của nó có gì thay đổi không?"
        return oldItem == newItem  // So sánh TOÀN BỘ field (data class tự
                                     // có equals() theo từng field)
    }
}
```

**Flow**:

- **Bước 1**: `DiffUtil` gọi `areItemsTheSame(old, new)` để xác định **2 item có cùng thực thể hay không**.
- **Bước 2**: Nếu `areItemsTheSame = true`, `DiffUtil` gọi tiếp `areContentsTheSame(old, new)` để xác định **nội dung của item có thay đổi hay không**.

Kết quả cuối cùng: `DiffUtil` quyết định:

- Đây là **item mới** (`inserted`) hay **item cũ** (`removed`) hay **item cũ nhưng nội dung thay đổi** (`changed`)
- Gọi các method tương ứng: `notifyItemInserted()`, `notifyItemRemoved()`, `notifyItemChanged()` để **update `RecyclerView`** một cách **hiệu quả**.
- `RecyclerView` sẽ **gọi lại `onBindViewHolder()`** chỉ với **những item thực sự thay đổi**, giúp **tăng performance** và **hỗ trợ animation mượt mà**.

> _**Note**: với hàm `areItemsTheSame()`, nên sử dụng các **`property` định danh**(id, ...) để xác định thực thể thay đổi, thay vì so sánh toàn bộ object._

---

## 3. `ListAdapter<T, VH>` - **thay thế `RecyclerView.Adapter`**

### 3.1. `ListAdapter`

`ListAdapter` là một **subclass của `RecyclerView.Adapter`** được Google cung cấp (_có sẵn của Android_), giúp **tích hợp sẵn `DiffUtil`** (_tự động hóa việc chạy `DiffUtil`_) để **update danh sách hiệu quả**.

```kotlin
class ContactAdapter(
    private val onItemClick: (Contact) -> Unit
) : ListAdapter<Contact, ContactAdapter.ContactViewHolder>(ContactDiffCallback()) {
    //              ▲                                        ▲
    //         Kiểu data (T)                      ItemCallback truyền vào
    //                                             CONSTRUCTOR của ListAdapter

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        // getItem(position) — method CÓ SẴN từ ListAdapter,
        // KHÔNG cần tự quản lý List như trước
        holder.bind(getItem(position))
    }

    // ❌ KHÔNG cần override getItemCount() — ListAdapter tự làm

    inner class ContactViewHolder(private val binding: ItemContactBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: Contact) {
            binding.tvName.text = contact.name
            binding.tvPhone.text = contact.phone
            binding.root.setOnClickListener { onItemClick(contact) }
        }
    }
}
```

### 3.2. `ListAdapter` & `RecyclerView.Adapter`

So sánh `ListAdapter<T, VH` với `RecyclerView.Adapter` truyền thống:

| Feature               | `RecyclerView.Adapter<VH>`                                                                                  | `ListAdapter<T, VH>`                                                                             |
| --------------------- | ----------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| **Quản lý danh sách** | PHẢI **tự khai báo** `List<T>` và **tự quản lý** danh sách                                                  | **`ListView` tự quản lý `List<T>` nội bộ**, không cần khai báo lại                               |
| `getItemCount()`      | PHẢI **override** và trả về `list.size`                                                                     | **KHÔNG cần override**                                                                           |
| **Update data**       | Phải tự viết hàm `updateData(newList: List<T>)` và **tự CHỊU TRÁCH NHIỆM tính `diff` và gọi `notify...()`** | Chỉ cần gọi `submitList(newList)` — **ListAdapter tự chạy `DiffUtil`** và gọi đúng `notify...()` |
| --                    | --                                                                                                          | Tự có `getItem(position)` để lấy data tại `position` trong **list**                              |

### 3.3. `submitList(newList: List<T>)` - **update danh sách hiệu quả**

`ListView` cung cấp **method `submitList(newList: List<T>)`** để **update danh sách** một cách **hiệu quả**, tự đon `DiffUtil` và gọi các method `notify...()` tương ứng:

```kotlin
// Trong Fragment/Activity — MỖI KHI có data mới (từ Flow, API...)
viewLifecycleOwner.lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.contacts.collect { contacts ->
            adapter.submitList(contacts)  // CHỈ 1 DÒNG — thay thế
                                           // toàn bộ logic notify thủ công
        }
    }
}
```

> _**Note - điểm mấu chốt về `threading`**: `submitList()` **tự động** chạy phần **tính toán `diff`** trên 1 **background thread pool** riêng (không phải Main Thread) — không cần `withContext(Dispatchers.Default)` thủ công, ...._

Đây là lý do **`ListAdapter` an toàn để gọi trực tiếp từ `collect{}`** (đang chạy trên Main Thread) mà không lo block UI

---

## 4. `getChangePayload()` - **TỐI ƯU performance**

**Vấn đề**: **`notifyItemChanged()` mặc định gọi LẠI TOÀN BỘ `onBindViewHolder()`
cho item đó** — dù chỉ 1 field nhỏ thay đổi (_ví dụ chỉ đổi trạng thái
"đã đọc/chưa đọc" của 1 tin nhắn, không cần redraw lại avatar, tên..._)

```kotlin
class ContactDiffCallback : DiffUtil.ItemCallback<Contact>() {

    override fun areItemsTheSame(oldItem: Contact, newItem: Contact) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Contact, newItem: Contact) =
        oldItem == newItem

    // Trả về "cái gì cụ thể đã đổi" — để onBindViewHolder có thể
    // CHỈ update phần đó, không redraw toàn bộ item
    override fun getChangePayload(oldItem: Contact, newItem: Contact): Any? {
        return if (oldItem.name != newItem.name) "NAME_CHANGED" else null
    }
}
```

Khi này, `onBindViewHolder(holder, position, payloads)` sẽ được gọi thay vì `onBindViewHolder(holder, position)` với phần `payloads` chứa thông tin về **cái gì đã thay đổi** được trả về từ `getChangePayload()`. <br/>

```kotlin
// onBindViewHolder có OVERLOAD nhận thêm "payloads"
override fun onBindViewHolder(
    holder: ContactViewHolder,
    position: Int,
    payloads: List<Any>
) {
    if (payloads.isNotEmpty() && payloads[0] == "NAME_CHANGED") {
        // CHỈ update TextView tên — không redraw avatar, phone...
        holder.bindNameOnly(getItem(position))
    } else {
        // Bind đầy đủ như bình thường (lần đầu hoặc thay đổi lớn)
        super.onBindViewHolder(holder, position, payloads)
    }
}
```
