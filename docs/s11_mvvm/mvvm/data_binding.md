# **Data Binding**

## 1. **Data Binding** & **View Binding**

|                        | ViewBinding                                               | DataBinding                                                                                       |
| ---------------------- | --------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| **Chức năng** chính    | Thay thế `findViewById()`                                 | ViewBinding + **Binding data trực tiếp** (UI <-> Data) vào XML layout qua **Expression Language** |
| Cú pháp **XML**        | _Không thay đổi_                                          | Cần bọc trong `<layout>`, khai báo `<data>`/`<variable>` tag                                      |
| **Set value** for UI   | Trong **code**<br/>_eg: `binding.tvName.text = "vduczz"`_ | **Auto qua `@{expression}`** trong XML layout                                                     |
| **Build time**         | **NHANH** hơn                                             | **CHẬM** hơn (_annotation processor phức tạp hơn_)                                                |
| **`LiveData` support** | Không hỗ trợ                                              | **NATIVE**, auto update UI khi data thay đổi nếu `setLifecycleOwner()`                            |
| **Debug**              | **Dễ hơn** (logic trong code, dễ trace)                   | **KHÓ hơn** (_lỗi maybe nằm trong XML expression, khó trace hơn_)                                 |
| **Xu hướng**           | Được **khuyến khích** cho hầu hết case                    | Dùng khi cần **binding phức tạp trong XML** or legacy project                                     |

Example use **View Binding**:

```kotlin
// ViewBinding — CHỈ thay thế findViewById(), không có logic gì thêm
class UserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Vẫn phải TỰ set giá trị, TỰ observe, TỰ update UI thủ công
        lifecycleScope.launch {
            viewModel.user.collect { user ->
                binding.tvName.text = user.name       // set thủ công
                binding.tvEmail.text = user.email      // set thủ công
            }
        }
    }
}
```

Example use **Data Binding**:

- **XML layout**:

  ```xml
  <!-- DataBinding — XML biết TRỰC TIẾP về ViewModel/data qua "binding expression" -->
  <layout xmlns:android="http://schemas.android.com/apk/res/android">

      <data>
          <variable
              name="user"
              type="com.example.User" />
      </data>

      <TextView
          android:layout_width="wrap_content"
          android:layout_height="wrap_content"
          android:text="@{user.name}" />        <!-- Bind trực tiếp trong XML -->

  </layout>
  ```

- **Binding in `Activity`/`Fragment`**:

  ```kotlin
  class UserActivity : AppCompatActivity() {
      private lateinit var binding: ActivityUserBinding

      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          binding = ActivityUserBinding.inflate(layoutInflater)
          setContentView(binding.root)

          // Chỉ cần SET biến — XML tự render, không cần set từng field thủ công
          binding.user = someUser
      }
  }
  ```

> _**Cộng đồng** Android hiện tại (đặc biệt sau khi Jetpack Compose phổ biến) **nghiêng về ViewBinding + code Kotlin thuần** hơn là nhét logic vào XML qua DataBinding. <br/>**Binding Expression** phức tạp trong XML **khó test**, **khó đọc** hơn so với code Kotlin tương đương._

## 2 **Setup** & **Enable** Data Binding

### **Bước 1**: Enable Data Binding trong `build.gradle` (Module: app)

```kotlin
// build.gradle (app level)
android {
    buildFeatures {
        // enable data binding
        dataBinding = true

        // Có thể bật CẢ 2 cùng lúc nếu cần
        viewBinding = true
    }
}
```

### **Bước 2**: Bọc layout trong `<layout>` tag

**Data binding** yêu cầu **chuyển layout XML thường** sang **bọc bằng `<layout>`** tag (_là **root tag** của XML layout_), bên trong có 2 phần:

1. `<data>`: **khai báo biến sẽ dùng** trong binding expression
2. **Layout gốc**: y hệt như file XML thường, nằm bên trong `<layout>`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- BƯỚC 1: Root tag PHẢI là <layout>
        để kích hoạt Data Binding cho file này
-->
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <!-- BƯỚC 2: khai báo data binding variables
         - <data> tag nằm ngay dưới <layout>
         - <variable> tag khai báo biến sẽ dùng trong binding expression
    -->
    <data>
        <!--
        <variable> - khai báo biến sẽ dùng
        trong binding expression
            - name: tên biến dùng trong XML
            - type: kiểu dữ liệu (class) của biến,
                    dùng Full Qualified Name (FQN)
        -->
        <variable
            name="viewModel"
            type="com.example.contactapp.ui.detail.ContactDetailViewModel" />
    </data>

    <!-- BƯỚC 3: Layout gốc —
        y hệt như file XML thường, nằm bên trong <layout>
    -->
    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <!-- sử dụng Binding Expression: @{} -->
        <TextView
            android:id="@+id/tv_name"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@{viewModel.user.name}" />

    </androidx.constraintlayout.widget.ConstraintLayout>

</layout>
```

### **Bước 3**: Khởi tạo binding class trong `Activity`/`Fragment`

Cần thực hiện:

- **Inflate layout** cho Binding class
- **Set variable** đã khai báo
- **Set LifecycleOwner** nếu muốn **binding tự observe theo lifecycle** (_`LiveData` auto update UI khi data thay đổi_)

```kotlin
class ContactDetailFragment : Fragment() {

    private var _binding: FragmentContactDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // BƯỚC 1: inflate layout cho binding class
        // DataBindingUtil hoặc generated inflate() — tương tự ViewBinding
        _binding = FragmentContactDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // BƯỚC 2: Set variable đã khai báo trong <data>
        binding.viewModel = viewModel

        // BƯỚC 3: BẮT BUỘC — để LiveData trong binding tự observe theo lifecycle
        binding.lifecycleOwner = viewLifecycleOwner
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

> `binding.lifecycleOwner` là **bước hay bị quên nhất** — _thiếu dòng này, `LiveData` trong Binding Expression sẽ **không bao giờ tự cập nhật UI**, dù data có thay đổi_.

---

## 3. **Binding Expression** trong XML

### 3.1. **Khai báo `<variable>`** trong `<data>` tag

Mọi **binding expression** trong XML đều **dựa trên `<variable>` khai báo trong `<data>` tag**. <br/>

```kotlin
<data>
    <variable name="user" type="com.example.User" />
    <variable name="isLoading" type="Boolean" />
</data>
```

### 3.2. Cách sử dụng **binding expression**: [Details](./data_binding_expression.md)
