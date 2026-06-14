# **Passing data** into `Fragment`

## 1. Why don't we use **parameters in `Fragment` constructor**?

Truyền **data** như tham số của **constructor** nghe có vẻ hợp lý:

```kotlin
class ProductDetailFragment(val productId: String) : Fragment()
```

Tuy nhiên, **vấn đề NGHIÊM TRỌNG** xảy ra khi hệ thống **recreate `Fragment`** (_xoay màn hình_, _process death_), Android gọi **`no-arg` constructor** của `Fragment`

> _Khi này, instance mới tạo **không có `productId`** => **crash app** hoặc **mất data**._

---

## 2. **Arguments `Bundle`**

`Fragment.arguments` là một `Bundle` được Android **lưu và khôi phục tự động qua các configuration change** — đây là **cơ chế chính thức** để truyền data vào `Fragment`.

```kotlin
class ProductDetailFragment : Fragment() {

    companion object {
        private const val ARG_PRODUCT_ID   = "arg_product_id"
        private const val ARG_PRODUCT_NAME = "arg_product_name"

        // Factory method — pattern chuẩn
        fun newInstance(productId: String, productName: String): ProductDetailFragment {
            return ProductDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PRODUCT_ID,   productId)
                    putString(ARG_PRODUCT_NAME, productName)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Đọc arguments — an toàn, tồn tại qua rotation
        val productId   = arguments?.getString(ARG_PRODUCT_ID)   ?: ""
        val productName = arguments?.getString(ARG_PRODUCT_NAME) ?: ""
    }
}

// Sử dụng:
val fragment = ProductDetailFragment.newInstance("p001", "Áo thun Kotlin")
supportFragmentManager.beginTransaction()
    .replace(R.id.container, fragment)
    .addToBackStack(null)
    .commit()
```

### Các kiểu **data** có thể lưu trong `Bundle`:

```kotlin
arguments = Bundle().apply {
    // Primitive types
    putString("key",   "value")
    putInt("key",      42)
    putBoolean("key",  true)
    putFloat("key",    3.14f)
    putDouble("key",   3.14)
    putLong("key",     100L)

    // Arrays
    putStringArray("key",  arrayOf("A", "B", "C"))
    putIntArray("key",     intArrayOf(1, 2, 3))

    // ArrayList
    putStringArrayList("key",  arrayListOf("A", "B"))
    putIntegerArrayList("key", arrayListOf(1, 2, 3))

    // Parcelable object
    putParcelable("key", product)   // Product phải implement Parcelable

    // Serializable (ít dùng, chậm hơn Parcelable)
    putSerializable("key", someObject)
}
```

### **Đọc** `arguments`:

Nên đọc `arguments` trong `onCreate()` (_hoặc có thể ở `onViewCreated()`_) của `Fragment`:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Đọc ở onCreate() — ĐÚNG, arguments luôn available từ đây
    val productId = arguments?.getString(ARG_PRODUCT_ID) ?: run {
        // Không có argument → không nên hiển thị Fragment này
        // Thông báo lỗi hoặc pop back stack
        parentFragmentManager.popBackStack()
        return
    }
}

// Đọc ở onViewCreated() — cũng được, nhưng nên đọc ở onCreate()
// vì arguments không liên quan đến View
```

**KHÔNG** đọc `arguments` trước `onCreate()` vì có thể `arguments` **chưa được set** hoặc `null`:

```kotlin
// Lưu ý: KHÔNG đọc args trước onCreate()
// như onAttach() / init {} — arguments chưa được set
override fun onAttach(context: Context) {
    super.onAttach(context)
    // arguments CÓ THỂ null ở đây trong một số trường hợp
}
```
