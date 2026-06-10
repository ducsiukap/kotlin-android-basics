# **View Binding**

## 1. View Binding

**View Binding** là tính năng của **Android build system** — nó **tự động generate** một **`Binding` class** tương ứng với mỗi **file `XML layout`**. Binding class này chứa **direct reference** đến tất cả **View có `android:id`** trong layout, với đúng kiểu dữ liệu.

Quy tắc đặt tên **Binding class**: chuyển file XML sang **PascalCase** + **`Binding`**

```
activity_main.xml       →  ActivityMainBinding
fragment_home.xml       →  FragmentHomeBinding
item_product.xml        →  ItemProductBinding
dialog_confirm.xml      →  DialogConfirmBinding
```

### **Setup**: enable view binding

Mặc định, **View Binding không tự động bật**, phải được khai báo trong `build.gradle.kts` (module `:app`)

```kotlin
android {
    ...
    buildFeatures {
        viewBinding = true   // bật View Binding
    }
}
```

**Lưu ý**: Nếu một file XML layout **không muốn generate Binding class**, thêm `tools:viewBindingIgnore="true"` vào root View của file đó:

```xml
<LinearLayout
    ...
    tools:viewBindingIgnore="true">
```

---

## 2. **Binding class** structure

Với mỗi **auto-generated Binding class**, sẽ có:

- `root` property: trỏ tới **View gốc** của layout (ViewGroup)
- Mỗi view có `android:id` trở thành **property** tương ứng, **đúng kiểu**, **non-null**
  > _Với `android:id="@+id/btnSubmit"` → property có dạng `val btnSubmit: Button` (**bỏ prefix `@+id/`** và **chuyển sang camelCase**)_
- `View` không có **ID** → **KHÔNG có trong Binding class**

Giả sử, file `activity_main.xml` có nội dung:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <TextView
        android:id="@+id/tvTitle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Hello" />

    <!-- View không có ID → KHÔNG có trong Binding class -->
    <View
        android:layout_width="match_parent"
        android:layout_height="1dp" />

</LinearLayout>
```

**Binding class** được generate:

```kotlin
// Auto-generated — ActivityMainBinding.kt
class ActivityMainBinding private constructor(
    val root: LinearLayout,          // root View của layout
    val tvTitle: TextView,           // ánh xạ @+id/tvTitle
    // View không có ID → không có ở đây
) {
    companion object {
        fun inflate(inflater: LayoutInflater): ActivityMainBinding { ... }
        fun inflate(inflater: LayoutInflater, parent: ViewGroup?, attachToParent: Boolean): ActivityMainBinding { ... }
        fun bind(rootView: View): ActivityMainBinding { ... }
    }
}
```

---

## 3. Sử dụng **View Binding** trong **`Activity`**

Theo **`4` bước**:

- **Bước 1**: khai báo `lateinit var binding: ActivityMainBinding`
- **Bước 2**: inflate layout thông qua binding class `ActivityMainBinding.inflate(layoutInflater)`
- **Bước 3**: gọi `setContentView(binding.root)` để thiết lập root View làm content của Activity
- **Bước 4**: sử dụng `binding` để truy cập trực tiếp đến các View đã khai báo ID

```kotlin
class MainActivity : AppCompatActivity() {

    // Bước 1: Khai báo binding — lateinit vì chưa thể khởi tạo ngay
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bước 2: Inflate layout thông qua Binding class
        binding = ActivityMainBinding.inflate(layoutInflater)

        // Bước 3: Set root View làm content của Activity
        setContentView(binding.root)

        // Bước 4: Truy cập View trực tiếp qua binding
        setupUI()
    }

    private fun setupUI() {
        // Trực tiếp, type-safe, không null
        binding.tvTitle.text = "Chào mừng"
        binding.etEmail.hint = "Nhập email"

        binding.btnLogin.setOnClickListener {
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            performLogin(email, password)
        }
    }
}
```

---

## 4. Sử dụng **View Binding** trong **`Fragment`**

**`Fragment` phức tạp hơn `Activity`** vì Fragment có thể **outlive View** của nó. Cụ thể:

```
Fragment lifecycle:    onAttach → onCreate → onCreateView → onViewCreated
                                                                  ↓
                                          Fragment đang sống, View đang tồn tại
                                                                  ↓
                                             onDestroyView   ← View BỊ HỦY
                                                                  ↓
                       Fragment vẫn còn sống (trong back stack)
                                                                  ↓
                                              onDestroy     ← Fragment bị hủy
```

**Vấn đề**: Dù View bị hủy (`onDestroyView`), nhưng **Fragment vẫn còn sống**, nếu giữ **reference đến binding** (và qua đó là View), sẽ gây **memory leak**.

### Solution: **nullable backing field `_binding`**

```kotlin
class HomeFragment : Fragment() {

    // Dùng nullable backing field
    private var _binding: FragmentHomeBinding? = null

    // Property tiện lợi — chỉ dùng giữa onCreateView và onDestroyView
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate layout và assign vào _binding
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup UI ở đây — View đã sẵn sàng
        binding.tvWelcome.text = "Xin chào"
        binding.btnAction.setOnClickListener {
            // handle click
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // NULL binding để giải phóng reference đến View
        // Fragment vẫn còn sống nhưng View đã bị hủy
        _binding = null
    }
}
```

Tại sao sử dụng `_binding` và `binding`?

```kotlin
private var _binding: FragmentHomeBinding? = null   // backing field — nullable
private val binding get() = _binding!!              // property tiện lợi — non-null
```

- `_binding`: là **nullable backing `field`** — cho phép set về `null` trong `onDestroyView` để xử lý trường hợp **Memory leak**.
- `binding`: là **computed property**, trả về `_binding!!`— dùng trong code để tránh phải viết `_binding!!` khắp nơi.
  > _Nếu vô tình **gọi `binding` sau `onDestroyView`** → throw `IllegalStateException` ngay lập tức thay vì silent null behavior → dễ debug hơn._

---

## 5. **`inflate()` vs `bind()`**

Trong **Binding class** có 2 `static method` quan trọng để khởi tạo: `bind()` và `inflate()`:

1. `inflate()`: — Tạo Binding class **_từ đầu_**, `inflate` **XML layout** thành **View tree**:

   ```kotlin
   // Dùng trong Activity
   binding = ActivityMainBinding.inflate(layoutInflater)

   // Dùng trong Fragment
   _binding = FragmentHomeBinding.inflate(inflater, container, false)

   // Dùng trong Adapter (ViewHolder)
   val binding = ItemProductBinding.inflate(
       LayoutInflater.from(parent.context),
       parent,
       false
   )
   ```

2. `bind()` — Tạo Binding class **từ View đã tồn tại sẵn** — **không `inflate`** lại:

   ```kotlin
   // Dùng khi View đã được inflate bởi nơi khác
   // Ví dụ: trong custom View, hoặc khi dùng setContentView(R.layout.xxx) trước
   val rootView = layoutInflater.inflate(R.layout.activity_main, null)
   val binding = ActivityMainBinding.bind(rootView)
   ```

Quy tắc chọn:

- Chưa có view → dùng `inflate()`
- Đã có view → dùng `bind()`

---

## 6. View Binding trong **Adapter** (`RecyclerView`)

View Binding **cực kỳ hữu ích** trong `RecyclerView.Adapter` — **loại bỏ toàn bộ `findViewById`** trong `ViewHolder`:

```kotlin
class ProductAdapter(
    private val products: List<Product>
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    // ViewHolder giữ Binding thay vì từng View riêng lẻ
    inner class ProductViewHolder(
        private val binding: ItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.tvProductName.text  = product.name
            binding.tvProductPrice.text = product.formattedPrice
            binding.tvProductRating.text = product.rating.toString()

            Glide.with(binding.root.context)
                .load(product.imageUrl)
                .centerCrop()
                .into(binding.ivProductImage)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false   // false: không attach ngay vào parent — RecyclerView tự quản lý
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount() = products.size
}
```
