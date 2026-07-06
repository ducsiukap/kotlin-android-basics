# **Multiple `ViewHolder` types**

usecase: **Contacts app**

```text
┌─────────────────────┐
│  ⭐ Favorites       │  ← Header đặc biệt
├─────────────────────┤
│  😊 Kimberly Lee    │
│  😊 Mindy Russell   │
├─────────────────────┤
│  A                  │  ← Section header (chữ A)
├─────────────────────┤
│  😊 Abby Chris      |
│  😊 Adi Thakkar     │
├─────────────────────┤
│  B                  │  ← Section header (chữ B)
├─────────────────────┤
│  😊 Bill Xu         │
│  😊 Brianna Levin   │
└─────────────────────┘
```

Vấn đề: **`RecyclerView` chỉ nhận 1 KIỂU DATA cho `Adapter` thông thường (`List<Contact>`)** — _nhưng ở đây cần XEN KẼ 2 loại hoàn toàn khác nhau: **"Section Header"** (`String`) và **"Contact"** (`object`)_

→ Đây chính là bài toán CẦN `sealed class` để biểu diễn "**1 item trong `RecyclerView` có thể là 1 trong NHIỀU loại khác nhau**"

---

## 1. Sử dụng `sealed class` làm **Wrapper** cho item

```kotlin
// Định nghĩa 1 "ListItem" đại diện cho MỌI THỨ có thể xuất hiện
// trong RecyclerView — không chỉ Contact thuần túy
sealed class ContactListItem {

    // Loại 1 — Header của section (vd: "A", "B", "Favorites")
    data class SectionHeader(val title: String) : ContactListItem()

    // Loại 2 — 1 Contact thực sự
    data class ContactItem(val contact: Contact) : ContactListItem()
}
```

---

## 2. **Data transforming** - convert `List<Contact>` into `List<ContactListItem>`

```kotlin
// Hàm transform — nhóm Contact theo chữ cái đầu, chèn Header
fun buildContactListItems(contacts: List<Contact>): List<ContactListItem> {
    val result = mutableListOf<ContactListItem>()

    // Contact được đánh dấu Favorite — hiện riêng ở đầu, không cần group
    val favorites = contacts.filter { it.isFavorite }
    if (favorites.isNotEmpty()) {
        result.add(ContactListItem.SectionHeader("⭐ Favorites"))
        favorites.forEach { result.add(ContactListItem.ContactItem(it)) }
    }

    // Nhóm phần còn lại theo chữ cái đầu tiên của tên
    val grouped = contacts.filter { !it.isFavorite }
        .sortedBy { it.name }
        .groupBy { it.name.first().uppercaseChar() }

    grouped.forEach { (letter, contactsInGroup) ->
        result.add(ContactListItem.SectionHeader(letter.toString()))
        contactsInGroup.forEach { result.add(ContactListItem.ContactItem(it)) }
    }

    return result
}
```

Hàm **transform** nên được gọi từ `ViewModel` trước khi đưa data ra `View`.

```kotlin
// Trong ViewModel — transform data TRƯỚC khi expose ra View
class ContactListViewModel(private val repository: ContactRepository) : ViewModel() {

    val listItems: StateFlow<List<ContactListItem>> = repository.allContacts
        .map { contacts -> buildContactListItems(contacts) }  // Transform ở ĐÂY
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

> _Nguyên tắc quan trọng: Việc **"biến đổi data thành cấu trúc hiển thị"** (thêm Header, group...) nên làm trong `ViewModel` (dùng `.map{}` trên Flow), **KHÔNG làm trong `Adapter`**._

`View`/`Adapter` chỉ nên biết cách **expose data** và **tương tác với user** — không nên tự quyết định logic nhóm/sắp xếp.

---

## 3. Implement `Adapter`

Hàm `getItemViewType` dựa trên `sealed class`

```kotlin
class ContactListAdapter(
    private val onContactClick: (Contact) -> Unit
) : ListAdapter<ContactListItem, RecyclerView.ViewHolder>(ContactListDiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CONTACT = 1
    }

    // Bước 1 — Xác định viewType dựa vào LOẠI của sealed class
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ContactListItem.SectionHeader -> TYPE_HEADER
            is ContactListItem.ContactItem -> TYPE_CONTACT
        }
    }

    // Bước 2 — Tạo ĐÚNG ViewHolder tương ứng với viewType
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemSectionHeaderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                HeaderViewHolder(binding)
            }
            TYPE_CONTACT -> {
                val binding = ItemContactBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ContactViewHolder(binding, onContactClick)
            }
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    // Bước 3 — Bind ĐÚNG data cho ĐÚNG loại ViewHolder
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ContactListItem.SectionHeader -> {
                (holder as HeaderViewHolder).bind(item)
            }
            is ContactListItem.ContactItem -> {
                (holder as ContactViewHolder).bind(item.contact)
            }
        }
    }

    // ── ViewHolder cho Header ──────────────────────────────────
    class HeaderViewHolder(
        private val binding: ItemSectionHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: ContactListItem.SectionHeader) {
            binding.tvSectionTitle.text = header.title
        }
    }

    // ── ViewHolder cho Contact ──────────────────────────────────
    class ContactViewHolder(
        private val binding: ItemContactBinding,
        private val onContactClick: (Contact) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: Contact) {
            binding.tvName.text = contact.name
            binding.tvPhone.text = contact.phone
            binding.root.setOnClickListener { onContactClick(contact) }
        }
    }
}
```

---

## 4. `DiffUtil.ItemCallback` cho `sealed class`

```kotlin
class ContactListDiffCallback : DiffUtil.ItemCallback<ContactListItem>() {

    override fun areItemsTheSame(oldItem: ContactListItem, newItem: ContactListItem): Boolean {
        // PHẢI xử lý từng loại RIÊNG — không thể so sánh "ID" chung
        // chung được vì SectionHeader và ContactItem không cùng cấu trúc
        return when {
            oldItem is ContactListItem.SectionHeader && newItem is ContactListItem.SectionHeader ->
                oldItem.title == newItem.title  // Header dùng title làm "khóa"

            oldItem is ContactListItem.ContactItem && newItem is ContactListItem.ContactItem ->
                oldItem.contact.id == newItem.contact.id  // Contact dùng id

            else -> false  // 2 item khác LOẠI (1 cái Header, 1 cái Contact)
                            // → chắc chắn KHÔNG PHẢI "cùng 1 thứ"
        }
    }

    override fun areContentsTheSame(oldItem: ContactListItem, newItem: ContactListItem): Boolean {
        // Ở bước này, ĐÃ CHẮC CHẮN 2 item CÙNG LOẠI (vì areItemsTheSame
        // đã return true trước đó) — data class tự có equals() đúng
        return oldItem == newItem
    }
}
```

---

## 5. Bài toán: **scroll tới cuối list -> load thêm** (pagination)

**Item**:

```kotlin
sealed class ContactListItem {
    data class SectionHeader(val title: String) : ContactListItem()
    data class ContactItem(val contact: Contact) : ContactListItem()
    object LoadingFooter : ContactListItem()   // Không cần data — chỉ là "cờ hiệu"
}
```

**Adapter**:
```kotlin
class ContactListAdapter(...) : ListAdapter<ContactListItem, RecyclerView.ViewHolder>(...) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CONTACT = 1
        private const val TYPE_LOADING = 2   // Thêm loại mới
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ContactListItem.SectionHeader -> TYPE_HEADER
            is ContactListItem.ContactItem -> TYPE_CONTACT
            is ContactListItem.LoadingFooter -> TYPE_LOADING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(ItemSectionHeaderBinding.inflate(...))
            TYPE_CONTACT -> ContactViewHolder(ItemContactBinding.inflate(...), onContactClick)
            TYPE_LOADING -> {
                val binding = ItemLoadingBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                LoadingViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ContactListItem.SectionHeader -> (holder as HeaderViewHolder).bind(item)
            is ContactListItem.ContactItem -> (holder as ContactViewHolder).bind(item.contact)
            is ContactListItem.LoadingFooter -> Unit  // Không cần bind gì —
                                                        // layout chỉ có ProgressBar tĩnh
        }
    }

    class LoadingViewHolder(binding: ItemLoadingBinding)
        : RecyclerView.ViewHolder(binding.root)
}
```

**Trigger loading** khi phát hiện **"scroll gần tới cuối"** với `RecyclerView.OnScrollListener`: 

```kotlin
binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val visibleItemCount = layoutManager.childCount
        val totalItemCount = layoutManager.itemCount
        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

        // Điều kiện: đã cuộn gần đến item CUỐI CÙNG (còn ~5 item nữa
        // là hết) — trigger load sớm để user không thấy giật khi
        // cuộn đến đáy thật sự
        if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
            viewModel.loadNextPage()
        }
    }
})
```

