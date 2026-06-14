# `Fragment` & `Activity` **communication**

**Vấn đề**: khi `Fragment` cần **thông báo ngược** lên `Activity` (hoặc `Fragment` khác)

> _ví dụ: user chọn một item trong Fragment, Activity cần biết để cập nhật Fragment khác_.

## 2.1. **Cách 1: Interface Callback** (_cách cũ_)

```kotlin
// Định nghĩa interface trong Fragment
class ProductListFragment : Fragment() {

    // Interface để giao tiếp với Activity
    interface OnProductSelectedListener {
        fun onProductSelected(productId: String)
    }

    private var listener: OnProductSelectedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Activity phải implement interface này
        listener = context as? OnProductSelectedListener
            ?: throw ClassCastException("$context phải implement OnProductSelectedListener")
    }

    override fun onDetach() {
        super.onDetach()
        listener = null   // tránh memory leak
    }

    private fun onItemClick(productId: String) {
        listener?.onProductSelected(productId)
    }
}
```

Khi này, `Activity` phải implement `OnProductSelectedListener` để nhận event từ `Fragment`:

```kotlin
// Activity implement interface
class MainActivity : AppCompatActivity(),
    ProductListFragment.OnProductSelectedListener {

    override fun onProductSelected(productId: String) {
        // Nhận event từ Fragment
        // Mở Detail Fragment với productId
        val detailFragment = ProductDetailFragment.newInstance(productId, "")
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, detailFragment)
            .addToBackStack(null)
            .commit()
    }
}
```

> _**Nhược điểm**: `Fragment` bị **coupled** với `Activity` — phải biết Activity implement interface nào. Khó test, khó tái sử dụng._

---

## 2.2. **Cách 2: Shared ViewModel** (_cách hiện đại, **recommended**_)

`Fragment A` và `Fragment B` **trong cùng Activity** có thể **chia sẻ data qua `ViewModel`** có _scope là Activity_:

```kotlin
// ViewModel chung — scope theo Activity
class SharedViewModel : ViewModel() {
    private val _selectedProductId = MutableLiveData<String>()
    val selectedProductId: LiveData<String> = _selectedProductId

    fun selectProduct(productId: String) {
        _selectedProductId.value = productId
    }
}

// Fragment A — gửi event
class ProductListFragment : Fragment() {

    // Dùng activityViewModels() → ViewModel scope theo Activity
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private fun onItemClick(productId: String) {
        sharedViewModel.selectProduct(productId)
    }
}

// Fragment B — nhận event
class ProductDetailFragment : Fragment() {

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.selectedProductId.observe(viewLifecycleOwner) { productId ->
            loadProduct(productId)
        }
    }
}
```

> _**Ưu điểm**: `Fragment` **không biết nhau, không coupled** — chỉ biết `ViewModel`. Đây là pattern được Google khuyến nghị._

---

## 2.3. **Cách 3: Fragment Result API** (_recommended for `one-time` result_)

**Fragment Result API** được thêm vào từ **Fragment 1.3.0** — cơ chế **giao tiếp một chiều**, kiểu "`request`-`response`", an toàn với **lifecycle**.

> _Phù hợp cho: **kết quả một lần từ `Fragment` con về `Fragment` cha** — giống `ActivityResultLauncher` nhưng cho `Fragment`._

```kotlin
// Fragment GỬI kết quả (Fragment con)
class DatePickerFragment : Fragment() {

    companion object {
        const val REQUEST_KEY = "date_picker_request"
        const val RESULT_DATE = "result_date"
    }

    private fun onDateSelected(date: String) {
        // Gửi result về Fragment/Activity cha
        val result = Bundle().apply {
            putString(RESULT_DATE, date)
        }
        setFragmentResult(REQUEST_KEY, result)
        parentFragmentManager.popBackStack()   // đóng Fragment này
    }
}

// Fragment NHẬN kết quả (Fragment cha)
class BookingFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Đăng ký listener — tự động cleanup theo lifecycle
        setFragmentResultListener(DatePickerFragment.REQUEST_KEY) { requestKey, bundle ->
            val date = bundle.getString(DatePickerFragment.RESULT_DATE) ?: return@setFragmentResultListener
            binding.tvSelectedDate.text = date
        }
    }

    private fun openDatePicker() {
        val datePickerFragment = DatePickerFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, datePickerFragment)
            .addToBackStack(null)
            .commit()
    }
}
```

**Ưu điểm** so với interface callback:
- Không cần `interface`, không `coupled` 
- **Lifecycle-aware**: result chỉ **delivered khi Fragment ở `STARTED` state**
- Result được lưu lại nếu **Fragment chưa active**, delivered ngay khi Fragment active lại
- Code gọn, ít boilerplate hơn

---

## **Usecase**

```
Interface Callback    → Legacy code, cần backward compat
                         Không dùng trong code mới

Shared ViewModel      → Giao tiếp 2 chiều, liên tục giữa Fragment
                         Fragment A update data → Fragment B tự cập nhật
                         Ví dụ: List + Detail cùng màn hình (tablet)

Fragment Result API   → Kết quả một lần (one-shot result)
                         Fragment con → Fragment cha
                         Ví dụ: Date picker, image picker, confirm dialog
```