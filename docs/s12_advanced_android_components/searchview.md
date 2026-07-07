# `SearchView`

`SearchView` là **Widget CÓ SẴN của Android**, chuyên cho việc **NHẬP TỪ KHÓA tìm kiếm** — tự có: `icon` kính lúp, `input` text,
nút xóa (`X`), **animation** mở rộng/thu gọn

Thông thường, có **2 cách đặt `SearchView` trong layout**: đặt trong **ToolBar** và trong **Layout thường**.

## 1. Đặt `SearchView` trong **ToolBar**

```xml
<!-- res/menu/menu_contact_list.xml -->
<menu xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <item
        android:id="@+id/action_search"
        android:title="Tìm kiếm"
        android:icon="@android:drawable/ic_menu_search"
        app:showAsAction="ifRoom|collapseActionView"
        app:actionViewClass="androidx.appcompat.widget.SearchView" />

</menu>
```

Trong đó:

- `app:actionViewClass="androidx.appcompat.widget.SearchView"` báo cho hệ thống: "Menu item NÀY, khi hiển thị, **dùng `SearchView`** THAY VÌ icon/text thông thường"
- `app:showAsAction="ifRoom|collapseActionView"`:
  - **`ifRoom`**: **hiện TRỰC TIẾP** trên `Toolbar` nếu CÒN CHỖ, không thì đẩy vào **overflow menu** (dấu `⋮`)
  - **`collapseActionView`**: SearchView **BAN ĐẦU chỉ hiện ICON** kính lúp (thu gọn) — user **TAP vào icon** → **`SearchView` MỚI "nở rộng"** ra chiếm hết chiều rộng Toolbar

```kotlin
private fun setupToolbar() {
    binding.toolbar.inflateMenu(R.menu.menu_contact_list)

    val searchItem = binding.toolbar.menu.findItem(R.id.action_search)
    val searchView = searchItem.actionView as SearchView

    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            // Gọi khi user bấm nút "Enter"/"Tìm" trên bàn phím
            return false  // false = KHÔNG tự xử lý thêm, để hệ
                           // thống mặc định (thường không cần vì
                           // đã xử lý ở onQueryTextChange)
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            // Gọi MỖI KHI user GÕ 1 KÝ TỰ — "real-time search"
            viewModel.onSearchQueryChanged(newText.orEmpty())
            return true  // true = ĐÃ xử lý xong sự kiện này
        }
    })
}
```

---

## 2. Đặt `SearchView` trong **layout**

Trong **XML layout**, có thể sử dụng trực tiếp `<SearchView>` tag:

```xml
<androidx.appcompat.widget.SearchView
    android:id="@+id/searchView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:queryHint="Tìm kiếm contact..."
    app:iconifiedByDefault="false" />
```

Trong code, cấu hình cho `SearchView` tương tự cách trên, thông qua `setOnQueryTextListener()`:

```kotlin
binding.searchView.setOnQueryTextListener(...)  // Giống hệt cách 1
```

### Phân biệt - **sử dụng `SearchView`**:

Trong **`ToolBar` menu**:

- **Tiết kiệm không gian**, mặc định **thu gọn thành icon**, chỉ mở ra khi cần.
- Phù hợp: màn hình chính có **nhiều chức năng** khác trên `ToolBar`(_add/filter, ... buttons_)

Trong **layout thường**:

- **Luôn hiển thị sẵn**, không cần tap để mở.
- **BẮT BUỘC set `app:iconifiedByDefault="false"`** để tránh `SearchView` hiển thị dạng thu gọn.
- Phù hợp: màn hình **CHUYÊN DÙNG để search**

---

## 3. **`onTextQueryChange`: kết hợp với `ViewModel` + `Flow.debounce`**

```kotlin
class ContactListViewModel(private val repository: ContactRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val contacts: StateFlow<List<Contact>> = _searchQuery
        .debounce(300L)  // Đợi 300ms sau khi ngừng gõ mới thực sự search
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allContacts
            else repository.searchContacts(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
}
```

**Debounce** là cần thiết để **tránh trigger Room/API liên tục** khi gõ query.

---

## 4. `SearchView.onCloseListener`

```kotlin
searchView.setOnCloseListener {
    // Gọi khi user BẤM nút "X" để xóa hết text VÀ đóng SearchView
    viewModel.onSearchQueryChanged("")  // Reset lại danh sách đầy đủ
    false  // false = cho phép hành vi đóng MẶC ĐỊNH tiếp tục xảy ra
} 
```

---



