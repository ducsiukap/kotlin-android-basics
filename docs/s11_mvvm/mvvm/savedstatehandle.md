# ViewModel & `SavedStateHandle`

## 1. **Configuration changes** vs **Process death**

Cần phân biệt **Configuration changes** và **Process death**:

|                 | Configuration changes                | Process death                                                                                                   |
| --------------- | ------------------------------------ | --------------------------------------------------------------------------------------------------------------- |
| When **occurs** | Rotate screen, change language, etc. | - Android **kills app** in background to free memory<br/>- User đang dùng app khác và quay lại app cũ<br/>- ... |
| Process         | **NOT killed**                       | **KILLED** -> `ViewModel` chết theo                                                                             |
| `ViewModel`     | **RETAINED**                         | **Bị kill** theo, không giải quyết được -> cần sử dụng `SavedStateHandle`                                       |

---

## 2. `onSavedInstanceState()` callback

Mặc dù `onSaveInstanceState()` bị giới hạn dung lượng và không lưu được `Flow`. Tuy nhiên, đây là **cách duy nhất hoạt động với Process death**.

Khi process bị kill do thiếu RAM:

- Android gọi `onSaveInstanceState()` trước khi kill.
- `Bundle` được lưu vào hệ thống (**`SavedStateRegistry`**), ngoài process.
- Khi process mới được tạo lại, `Bundle` được restore.

`SavedStateHandle` chính là cách Google **wrap `Bundle này vào trong `ViewModel``** — kết hợp ưu điểm của cả 2:

- Sống qua **Process Death** (như `Bundle`)
- **API hiện đại** dùng được trong `ViewModel` (như `StateFlow`/`LiveData`).

---

## 3. `SavedStateHandle` - Giải pháp cho **Process death**

`SavedStateHandle` bản chất có thể coi là `Map<String, Any?>` đặc biệt:

- `set("key", value)` → lưu giá trị, đồng thời đăng ký để
  tự động serialize vào Bundle
- `get("key")` → đọc giá trị hiện tại
- `getLiveData("key")` → trả về LiveData — observe thay đổi
- `getStateFlow("key")` → trả về StateFlow — bản hiện đại hơn

```kotlin
class SearchViewModel(
    // savedStateHandle được Android TỰ ĐỘNG inject vào ViewModel
    private val savedStateHandle: SavedStateHandle,
    private val repository: SearchRepository
) : ViewModel() {

    companion object {
        private const val KEY_QUERY = "search_query"
    }

    // getStateFlow — đọc giá trị cũ nếu có (sau process death),
    // hoặc dùng default value nếu là lần đầu
    val query: StateFlow<String> =
        savedStateHandle.getStateFlow(KEY_QUERY, "")

    fun onQueryChanged(newQuery: String) {
        // set() — vừa update giá trị, vừa tự động lưu vào Bundle
        savedStateHandle[KEY_QUERY] = newQuery
    }

    val searchResults: StateFlow<List<Result>> = query
        .debounce(300)
        .flatMapLatest { q -> repository.search(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

`SavedStateHandle` có thể lưu được:

- **Primitive types**: String, Int, Boolean, Long, Float, Double, ...
- **Parcelable** / **Serializable**
- `ArrayList` of aboves

KHÔNG lưu được:

- Complex objects không implement `Parcelable`/`Serializable`
- **Large data**
- Coroutine Job, Flow object, ...

> **Note**: `getStateFlow()` và `getLiveData()` chỉ là **wrapper** quanh **giá trị đã lưu**, và tạo ra một `StateFlow`/`LiveData` mới mỗi khi `ViewModel` được khởi tạo. <br/>
> _KHÔNG có ý nghĩa là có thể lưu trực tiếp `StateFlow`/`LiveData` vào `SavedStateHandle`, vì bản chất chúng là **object** và không thể serialize._

### 3.1. **Khởi tạo `no-args` constructor `ViewModel` với `SavedStateHandle`**

```kotlin
class SearchViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    // ...
}
```

Android **nhận diện** constructor có tham số `SavedStateHandle` và **tự inject**:

```kotlin
class SearchActivity : AppCompatActivity() {
    // by viewModels() TỰ ĐỘNG cung cấp SavedStateHandle
    // — không cần Factory thủ công, giống AndroidViewModel
    private val viewModel: SearchViewModel by viewModels()
}
```

### 3.2. **Khởi tạo `ViewModel` có thêm dependencies ngoài `SavedStateHandle`**

```kotlin
class SearchViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SearchRepository
) : ViewModel() { ... }
```

#### **3.2.1. Với `Factory`**:

Khi định nghĩa `Factory` cho `ViewModel` có thêm dependencies ngoài `SavedStateHandle`, cần **extends `AbstractSavedStateViewModelFactory`**:

```kotlin
// Cần Factory — nhưng Factory đặc biệt: AbstractSavedStateViewModelFactory
class SearchViewModelFactory(
    private val repository: SearchRepository,
    owner: SavedStateRegistryOwner
) : AbstractSavedStateViewModelFactory(owner, null) {

    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,

        // SavedStateHandle được tự động thêm vào hàm create() của Factory
        // không cần truyền qua constructor của Factory
        handle: SavedStateHandle
    ): T {
        @Suppress("UNCHECKED_CAST")
        return SearchViewModel(handle, repository) as T
    }
}
```

Khởi tạo:

```kotlin
class SearchActivity : AppCompatActivity() {
    private val viewModel: SearchViewModel by viewModels {
        SearchViewModelFactory(
            repository = (application as MyApplication).searchRepository,
            owner = this  // Activity là SavedStateRegistryOwner
        )
    }
}
```

#### **3.2.2. Với `viewModelFactory` DSL**:

```kotlin
class SearchActivity : AppCompatActivity() {

    private val viewModel: SearchViewModel by viewModels {
        viewModelFactory {
            initializer {
                val repo = (application as MyApplication).searchRepository
                // createSavedStateHandle() — extension function có sẵn
                // trong CreationExtras, hoạt động bên trong initializer
                SearchViewModel(createSavedStateHandle(), repo)
            }
        }
    }
}
```
