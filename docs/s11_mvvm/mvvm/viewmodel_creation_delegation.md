# `ViewModel`: creation & delegation

## 1. Create a `ViewModel` class

### **Step 1**: add dependencies in `build.gradle` (app)

```kotlin
dependencies {
    val lifecycle_version = "2.11.0" // Check version mới nhất nếu cần

    // Thư viện cốt lõi cho ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    // thư viện đã bao gồm coroutines.core & coroutines.android (StateFlow, ..)

    // tuy nhiên, nếu cần có thể thêm riêng coroutines.core & coroutines.android để kiểm soat verson
    // val coroutines_version = "1.7.3"
    // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version")

    // Khuyến nghị: Thêm LiveData (hoặc dùng StateFlow) để quan sát dữ liệu
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")

    // Thư viện này để dùng được delegate 'by viewModels()' cực tiện
    implementation("androidx.activity:activity-ktx:1.13.0") // by viewModels() in Activity
    implementation("androidx.fragment:fragment-ktx:1.8.9") // by viewModels() in Fragment
}
```

### **Step 2**: create a `ViewModel` class

```kotlin
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CounterViewModel : ViewModel() {

    // MutableLiveData chỉ để ViewModel sửa đổi (Private)
    private val _count = MutableLiveData<Int>(0)
    // LiveData công khai để Activity/Fragment lắng nghe (Chỉ đọc)
    val count: LiveData<Int> get() = _count

    // Hàm xử lý logic
    fun incrementCount() {
        _count.value = (_count.value ?: 0) + 1
    }
}
```

> _**Note**: Luôn dùng pattern **Backing Property** (có **1 biến `_private` có thể sửa** và **1 biến `public` chỉ đọc**) để bảo vệ dữ liệu, tránh việc Activity tự ý sửa data từ bên ngoài._

### **Step 3**: use `ViewModel` in `Activity`/`Fragment`

Sử dụng delegate `by viewModels()` hoặc `by activityViewModels()` để tạo instance của `ViewModel` trong `Activity` hoặc `Fragment`.

```kotlin
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // 1. Khởi tạo ViewModel. Nó sẽ tự động được giữ lại khi xoay màn hình!
    private val viewModel: CounterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvCount = findViewById<TextView>(R.id.tvCount)
        val btnAdd = findViewById<Button>(R.id.btnAdd)

        // 2. Lắng nghe sự thay đổi của dữ liệu (Observe)
        viewModel.count.observe(this) { currentCount ->
            // Khối lệnh này sẽ chạy mỗi khi giá trị count thay đổi
            tvCount.text = currentCount.toString()
        }

        // 3. Tương tác gửi sự kiện lên ViewModel
        btnAdd.setOnClickListener {
            viewModel.incrementCount()
        }
    }
}
```

> _Không dùng trực tiếp `CounterViewModel()` để tạo instance, vì sẽ **re-create `ViewModel`** khi configuration changes._

---

## **2. Delegate `ViewModel` with `by viewModels()` / `by activityViewModels()`**

**Android** cung cấp nhiều **property delegate** khác nhau để lấy `ViewModel` — mỗi loại gắn với một **scope** khác nhau:

- `by viewModels()` - scope là **Activity** hoặc **Fragment** hiện tại.
- `by activityViewModels()` - scope là **Activity** hiện tại.
  > _Khi dùng trong **Fragment** để share cùng `ViewModel` với `Activity`._
- `by navGraphViewModels()` - scope là **NavGraph** hiện tại.
- `by viewModels({ ownerProducer })` - custom scope, hiến khi dùng.

### 2.1. `by viewModels()` — Scope riêng

Delegate `by viewModels()` được dùng với ý nghĩa: **mỗi `Activity`/`Fragment` có `ViewModel` riêng**, không chia sẻ với ai.

```kotlin
// Activity
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
}

// Fragment
class HomeFragment : Fragment() {
    private val viewModel: HomeViewModel by viewModels()
}
```

> _`by viewModels()` lấy instance từ `ViewModelStore` của chính `Activity`/`Fragment` đó. Do vậy, dù **`Fragment` nằm trong `Activity`**, sử dụng **chung `XxxViewModel`** nhưng **gọi `by viewModels()` thì sẽ tạo INSTANCE RIÊNG so với `Activity`**._

**Use cases**: sử dụng `by viewModels()` delegations **khi `ViewModel` chỉ phục vụ riêng cho `Activity` hoặc `Fragment` đó** — không cần share data. Đây là trường hợp phổ biến nhất.

### 2.2. `by activityViewModels()` — Share giữa các **Fragment**

Delegate `by activityViewModels()` được dùng với ý nghĩa: **các `Fragment` trong cùng một `Activity` share cùng một instance của `ViewModel`**.

```kotlin
// SharedViewModel — định nghĩa 1 lần
class CheckoutViewModel : ViewModel() {
    private val _cartItems = MutableStateFlow<List<Item>>(emptyList())
    val cartItems: StateFlow<List<Item>> = _cartItems.asStateFlow()

    fun addItem(item: Item) {
        _cartItems.value = _cartItems.value + item
    }
}
```

```kotlin
// Fragment A — thêm item vào giỏ hàng
class ProductListFragment : Fragment() {

    // activityViewModels() — scope = Activity, không phải Fragment
    private val sharedViewModel: CheckoutViewModel by activityViewModels()

    private fun onAddToCartClicked(item: Item) {
        sharedViewModel.addItem(item)
    }
}

// Fragment B — hiển thị giỏ hàng đã thêm
class CartFragment : Fragment() {

    // Cùng activityViewModels() → nhận CÙNG instance với ProductListFragment
    private val sharedViewModel: CheckoutViewModel by activityViewModels()

    override fun onViewCreated(...) {
        lifecycleScope.launch {
            sharedViewModel.cartItems.collect { items ->
                // Tự động thấy item mà ProductListFragment vừa thêm
                updateCartUI(items)
            }
        }
    }
}
```

> _**Note**: **Truyền data qua Navigation `argument` (Bundle) vẫn là cách được khuyến nghị cho data đơn giản (`id`, `string`, ...)**. `activityViewModels()` phù hợp hơn khi cần **share state phức tạp** hoặc **2 chiều** (List và Detail cùng sửa, cùng đọc)._

**Use cases**: sử dụng `by activityViewModels()` delegations **khi nhiều Fragment cần đọc/ghi chung 1 state** — giỏ hàng, bộ lọc tìm kiếm, wizard nhiều bước (step 1 → step 2 → step 3 cùng share data).

### 2.3. `by navGraphViewModels()` — Share giữa các **Fragment** trong cùng **NavGraph**

Khi dùng **Navigation Component** với **nested graph**, `by navGraphViewModels()` cho phép chia sẻ giữa **nhóm Fragment** trong cùng một NavGraph cụ thể, không phải toàn bộ Acitvity..

```kotlin
// nav_graph.xml có nested graph "checkout_flow"
// chứa: AddressFragment → PaymentFragment → ConfirmFragment

class AddressFragment : Fragment() {
    // Scope = nested graph "checkout_flow", không phải toàn Activity
    private val viewModel: CheckoutFlowViewModel by navGraphViewModels(R.id.checkout_flow)
}

class PaymentFragment : Fragment() {
    // Cùng graph ID → nhận CÙNG instance
    private val viewModel: CheckoutFlowViewModel by navGraphViewModels(R.id.checkout_flow)
}
```

**Use cases**: delegate `by navGraphViewModels()` được dùng cho **multi-step flow** (checkout, onboarding, form nhiều bước) — cần **share data** giữa các bước nhưng **không** muốn nó **sống** lâu bằng **toàn bộ `Activity`** (rời khỏi flow → ViewModel tự cleanup).

### 2.4. Tóm tắt **lifecycle**

| Delegate                  | **create** at                                                | **destroy** at                                                     |
| ------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------------ |
| `by viewModels()`         | `Activity`/`Fragment` **lần đầu tiên gọi** `by viewModels()` | `Activity`/`Fragment` bị remove hẳn (destroy/detach)               |
| `by activityViewModels()` | `Fragment` **lần đầu tiên gọi** `by activityViewModels()`    | `Activity` bị `finish()`                                           |
| `by navGraphViewModels()` | `Fragment` **lần đầu tiên gọi** `by navGraphViewModels()`    | Rời khỏi nested graph (pop hết toàn bộ destination trong graph đó) |
