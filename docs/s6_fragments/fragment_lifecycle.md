# `Fragment`'s **lifecycle**

## 1. Khởi tạo `Fragment`

Có `2` cách để khởi tạo `Fragment`:

1. Sử dụng **constructor mặc định** (_không tham số_) + `override onCreateView` .

   ```kotlin
   // Cách 1: Constructor mặc định + override onCreateView
   class HomeFragment : Fragment() {
       override fun onCreateView(
           inflater: LayoutInflater,
           container: ViewGroup?,
           savedInstanceState: Bundle?
       ): View {
           _binding = FragmentHomeBinding.inflate(inflater, container, false)
           return binding.root
       }
   }
   ```

2. Sử dụng **constructor có tham số**

   ```kotlin
   // Cách 2: Constructor có tham số
   class HomeFragment : Fragment(R.layout.fragment_home)
   ```

   `Fragment(R.layout.fragment_home)` là một **convenience constructor** được Google thêm vào từ **Fragment 1.1.0 (`AndroidX`)**

   > _Bên dưới, nó tự động thực hiện đúng một việc: **inflate layout** đó trong `onCreateView()`._

   Source code tương đương:

   ```kotlin
   // Fragment.kt (AndroidX source — simplified)
   class Fragment(@LayoutRes private val contentLayoutId: Int = 0) {

       fun onCreateView(
           inflater: LayoutInflater,
           container: ViewGroup?,
           savedInstanceState: Bundle?
       ): View? {
           return if (contentLayoutId != 0) {
               inflater.inflate(contentLayoutId, container, false)
           } else null
       }
   }
   ```

---

## 2. `Activity` và quan hệ với **`Fragment` lifecycle**

**`Fragment` không chỉ có một lifecycle**. Nó có **`2` lớp lifecycle** bạn phải tách riêng trong đầu:

- **Fragment's lifecycle**
- **View lifecycle** của Fragment

Ngoài ra, `Fragment` còn có một mối quan hệ chặt chẽ với `Activity` mà nó được gắn vào. Điều này có nghĩa là **lifecycle của `Fragment` phụ thuộc vào lifecycle của `Activity`**.

| `Activity` lifecycle | `Fragment` lifecycle                                            |
| -------------------- | --------------------------------------------------------------- |
| `onCreate()`         | `onAttach()`, `onCreate()`, `onCreateView()`, `onViewCreated()` |
| `onStart()`          | `onStart()`                                                     |
| `onResume()`         | `onResume()`                                                    |
| `onPause()`          | `onPause()`                                                     |
| `onStop()`           | `onStop()`                                                      |
| `onDestroy()`        | `onDestroyView()` , `onDestroy()`, `, onDetach()`               |

### **Ý nghĩa** của từng state:

| Trạng thái              | Ý nghĩa                                                                                                           |
| ----------------------- | ----------------------------------------------------------------------------------------------------------------- |
| `onAttach()`            | `Fragment` được **gắn** vào `Activity`.                                                                           |
| `onCreate()`            | `Fragment` được **tạo ra**, _chưa có view_.                                                                       |
| `onCreateView()`        | **Tạo view** cho `Fragment`: inflate/bind layout, trả về **View**                                                 |
| `onViewCreated()`       | View đã sẵn sàng, có thể **setup UI tại đây**                                                                     |
| `onViewStateRestored()` | `State` của View được khôi phục                                                                                   |
| `onStart()`             | `Fragment` **visible**                                                                                            |
| `onResume()`            | `Fragment` **active** (có thể tương tác)                                                                          |
|                         | Fragment đang hoạt động bình thường                                                                               |
| `onPause()`             | `Fragment` **bị tạm dừng**, mất focus                                                                             |
| `onStop()`              | `Fragment` **bị dừng**, không còn visible                                                                         |
| `onDestroyView()`       | **Hủy view** của `Fragment` (giải phóng tài nguyên liên quan đến view). <br/>**Quan trọng**: `null` binding ở đây |
| `onDestroy()`           | `Fragment` object bị hủy                                                                                          |
| `onDetach()`            | `Fragment` bị **gỡ khỏi `Activity`**                                                                              |

### Các **flow cơ bản** của `Fragment`:

1. Khởi tạo lần đầu:

   **on`Attach` -> on`Create` -> on`CreateView` -> on`ViewCreated` -> on`Start` -> on`Resume`**

2. Fragment bị gỡ khỏi **màn hình** / **host**

   **on`Pause` -> on`Stop` -> on`DestroyView` -> on`Destroy` -> on`Detach`**

3. ...

---

## 3. `onAttach()`

**Callback `onAttach()`** - đây là lúc **Fragment vừa được gắn vào host Activity**.

`override` khi cần:

- lấy tham chiếu host
- khởi tạo các thành phần cần context (_ex: `viewModel`, data, ..._) liên quan tới việc `Fragment` **đã vào** `Activity`

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Fragment object được khởi tạo
    // CHƯA CÓ VIEW ở bước này — không truy cập binding/view

    // Dùng cho:
    // → Khởi tạo ViewModel
    // → Nhận arguments (data truyền vào Fragment)
    // → Khởi tạo data không liên quan đến View

    val productId = arguments?.getString(ARG_PRODUCT_ID)
}
```

---

## 4. `onCreate()`

**Callback `onCreate()`** - đây là nơi khởi tạo các thứ **không phụ thuộc vào `View`**:

- **init** dữ liệu, adapter logic,...
- **lấy arguments** (data truyền vào `Fragment`)
- setup các thành phần **không liên quan đến view**

> _Tài liệu Android khuyên code “**other initialization**” nên đặt ở `onCreate()`, còn code **chạm vào View** nên đặt ở `onViewCreated()`_

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val userId = arguments?.getLong("userId")
}
```

---

## 5. `onCreateView()` (\***QUAN TRỌNG**)

**Callback `onCreateView()`** - đây là nơi tạo ra **View hierarchy** của `Fragment`.

> _Theo API reference, `onCreateView()` **tạo và trả về view hierarchy** gắn với `Fragment`_.

- **Cách 1**: nếu dùng `Fragment(R.layout.fragment_home)` thì `onCreateView()` **đã được Google `override` sẵn** để tự động inflate layout đó, bạn không cần override lại nữa.

  > _Tuy nhiên, khi cần khởi tạo **View Binding** thì bạn vẫn phải `override onCreateView()` để khởi tạo **`binding`**_

  ```kotlin
  class HomeFragment : Fragment(R.layout.fragment_home) {

      private var _binding: FragmentHomeBinding? = null
      private val binding get() = _binding!!

      // override onCreateView để setup binding
      // -> không trả về View
      override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
          super.onViewCreated(view, savedInstanceState)

          // View đã được inflate bởi constructor
          // → phải dùng bind() thay vì inflate()
          _binding = FragmentHomeBinding.bind(view)

          binding.tvTitle.text = "Hello"
      }
  }
  ```

- **Cách 2**: nếu dùng constructor mặc định thì bạn phải `override onCreateView()` để inflate layout và trả về view đó.

  ```kotlin
  // Cách 2: Constructor mặc định + override onCreateView
  class HomeFragment : Fragment() {

      private var _binding: FragmentHomeBinding? = null
      private val binding get() = _binding!!

      // override onCreateView để inflate layout, tạo View tree
      // Trả về root View của Fragment
      override fun onCreateView(
          inflater: LayoutInflater,
          container: ViewGroup?,
          savedInstanceState: Bundle?
      ): View {
          // sử dụng inflate() để tạo View tree từ XML
          _binding = FragmentHomeBinding.inflate(inflater, container, false)
          // + inflater: LayoutInflater để inflate XML
          // + container: ViewGroup cha (FragmentContainer trong Activity)
          // + attachToRoot: KHÔNG attach trực tiếp — false ở tham số 3
          // + savedInstanceState: state đã lưu trước đó

          return binding.root // trả về root View của Fragment
      }
  }
  ```

  Tai sao `attachToRoot = false`?
  - `FragmentManager` tự quản lí việc **attach View vào container**
  - Nếu pass `attachToRoot = true`, `inflate()` sẽ attach View vào container ngay lập tức, sau đó `FragmentManager` sẽ cố attach lại lần nữa dẫn tới **View bị attach 2 lần -> `crash`**

---

## 6. `onViewCreated()` (\***Setup UI**)

**Callback `onViewCreated()`** - đây là callback rất quan trọng, nơi **setup UI** của `Fragment` sau khi view đã được tạo ra.

> **Android** gọi `onViewCreated()` ngay **_SAU_ khi `onCreateView()` trả về view, _TRƯỚC_ khi `saved state` được khôi phục**.

```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    // View đã được tạo và gắn vào hierarchy
    // Đây là nơi ĐÚNG để setup UI

    // Dùng cho:
    // → Setup click listener
    // → Observe LiveData
    // → Populate data vào View
    // → Setup RecyclerView, Adapter

    setupListeners()
    setupRecyclerView()
    observeViewModel()
}
```

> _Về mặt **kỹ thuật** có thể làm được việc setup UI trong `onCreateView()`, nhưng **`onViewCreated()` tách biệt rõ ràng hơn**:<br/>- `onCreateView()` chỉ lo **inflate**<br/>- `onViewCreated()` lo **setup**<br/> Đây gọi là **single responsibility** giúprõ ràng hơn, code dễ đọc hơn._

### **Note**: **`viewLifecycleOwner` - Fragment lifecycle _object_**

Từ **Fragment 1.3.0 (Jetpack)**, `Fragment` có thêm **`viewLifecycleOwner`** — một **`LifecycleOwner` gắn với View lifecycle** (từ `onCreateView` đến `onDestroyView`), **tách biệt với lifecycle của Fragment object**.

```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // SAI — observe theo Fragment lifecycle
    // LiveData sẽ tiếp tục observe dù View đã bị hủy
    viewModel.data.observe(this) { data ->
        binding.tvData.text = data
    }

    // ĐÚNG — observe theo viewLifecycleOwner
    // Observer tự hủy khi View bị hủy (onDestroyView)
    viewModel.data.observe(viewLifecycleOwner) { data ->
        binding.tvData.text = data
    }
}
```

> **Quy tắc**: Khi **observe** `LiveData` trong `Fragment` → **LUÔN dùng `viewLifecycleOwner`**, _KHÔNG dùng `this`_.

---

## 7. `onViewStateRestored()`

```kotlin
override fun onViewStateRestored(savedInstanceState: Bundle?) {
    super.onViewStateRestored(savedInstanceState)
    // State của View (scroll position, checked state...)
    // đã được khôi phục vào View tree
    // Hiếm khi cần override
}
```

---

## 8. `onStart()`, `onResume()`, `onPause()`, `onStop()`

**Hoàn toàn tương tự `Activity`** — gọi khi _`Fragment` chuyển sang các trạng thái tương ứng_

```kotlin
override fun onStart() {
    super.onStart()
    // Fragment visible
}

override fun onResume() {
    super.onResume()
    // Fragment active, nhận tương tác, input từ user
    // Resume sensor, camera, animation
}

override fun onPause() {
    super.onPause()
    // Fragment mất focus
    // Pause sensor, camera, animation
}

override fun onStop() {
    super.onStop()
    // Fragment không visible
}
```

---

## 9. `onDestroyView()` (\***QUAN TRỌNG** với `_binding`)

**Callback `onDestroyView()`** - đây là nơi **hủy view** của `Fragment` (giải phóng tài nguyên liên quan đến view).

Đây là **callback cực kỳ quan trọng với View Binding**, bắt buộc gán **`_binding = null`** để giải phóng tài nguyên, tránh **memory leak**.

```kotlin
override fun onDestroyView() {
    super.onDestroyView()
    // View bị HỦY
    // Fragment object CÓ THỂ vẫn còn sống (trong back stack)

    // BẮT BUỘC: null binding để tránh memory leak
    _binding = null

    // Dùng cho:
    // → Null binding (bắt buộc)
    // → Giải phóng resource gắn với View
    // → Hủy Adapter của RecyclerView
}
```

Tại sao **`onDestroyView()` tồn tại riêng biệt**?

> _`View` và `Fragment` có **lifecycle khác nhau**. Đây là lý do tại sao **`onDestroyView()` tồn tại riêng biệt so với `onDestroy()`**._

- Use case: **`Fragment B` đang hiển thị**, user nhấn **Back** về `Fragment A` -> _`B` bị pop khỏi **back stack**_
  - `onDestroyView()` của `B` được gọi để **hủy view** của `B`
  - `onDestroy()` của `B` **được gọi** để hủy `Fragment` object của `B`
  - `onDetach()` của `B` được gọi để gỡ `B` khỏi `Activity`
- Use case: `Fragment B` trong back stack, `Fragment C` được **add lên trên** -> **`B` còn sống** nhưng **view** của `B` **bị hủy**:
  - `onDestroyView()` của `B` được gọi để hủy view của `B`
    > _`Fragment` object của `B` **VẪN CÒN** trong memory (back stack)_
  - Khi **`B` quay lại**:
    - `onCreateView()` của `B` được gọi để **tạo lại view mới** của `B`
    - `onViewCreated()`

---

## 10. `onDestroy()`, `onDetach()`

```kotlin
override fun onDestroy() {
    super.onDestroy()
    // Fragment object sắp bị hủy
    // Final cleanup những gì không liên quan đến View
}

override fun onDetach() {
    super.onDetach()
    // Fragment tách khỏi Activity
    // Sau đây không còn truy cập được Activity host
}
```

---

## Example **Skeleton code** của `Fragment` với đầy đủ các callback:

```kotlin
class HomeFragment : Fragment() {

    // Binding — nullable backing field pattern
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // ViewModel
    private val viewModel: HomeViewModel by viewModels()

    companion object {
        private const val ARG_USER_ID = "arg_user_id"
        private val TAG = HomeFragment::class.java.simpleName

        // Factory method — tạo Fragment với arguments
        fun newInstance(userId: String): HomeFragment {
            return HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ID, userId)
                }
            }
        }
    }

    // ── Lifecycle callbacks theo thứ tự ──────────────────

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "onAttach")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        // Đọc arguments ở đây — trước khi có View
        val userId = arguments?.getString(ARG_USER_ID)
        Log.d(TAG, "userId: $userId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        setupUI()
        setupListeners()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        _binding = null   // BẮT BUỘC
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "onDetach")
    }

    // ── Private methods ───────────────────────────────────

    private fun setupUI() {
        binding.tvTitle.text = "Trang chủ"
    }

    private fun setupListeners() {
        binding.btnAction.setOnClickListener {
            viewModel.performAction()
        }
    }

    private fun observeViewModel() {
        // LUÔN dùng viewLifecycleOwner khi observe trong Fragment
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.tvContent.text = state.content
        }
    }
}
```
