# **Navigate** giữa các `Fragment`

## 1. Lấy `NavController` trong `Fragment`

Trước khi **navigate phải có `NavController`**. Có `3` cách để lấy trong `Fragment`:

- Cách 1: Extension function (khuyến nghị)

  ```kotlin
  // Cách 1: Extension function — ngắn gọn nhất, khuyến nghị
  val navController = findNavController()
  ```

- Cách 2: Từ View

  ```kotlin
  // Cách 2: Từ View — dùng khi không trong Fragment trực tiếp
  val navController = view.findNavController()
  ```

- Cách 3: Từ Activity

  ```kotlin
  // Cách 3: Từ Activity — dùng trong Activity
  val navController = findNavController(R.id.navHostFragment)
  ```

**Note**: `findNavController()` chỉ được gọi sau `onCreateView()`, tức là **từ `onViewCreated()`**, _nếu không, sẽ throw `IllegalStateException`_.

```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // AN TOÀN — gọi sau onCreateView
    binding.btnNext.setOnClickListener {
        findNavController().navigate(R.id.action_home_to_detail)
    }
}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // KHÔNG gọi findNavController() ở đây — chưa có View
}
```

---

## 2. **Navigate** giữa các `Fragment`

Để navigate giữa các `Fragment`, sử dụng `NavController` để gọi **`navigate()` với `<action>.ID`** đã khai báo trong `nav_graph.xml`.

```kotlin
class HomeFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnViewDetail.setOnClickListener {
            // Navigate theo action ID khai báo trong nav_graph.xml
            findNavController().navigate(R.id.action_home_to_detail)
        }
    }
}
```

> _**Navigate bằng `action` hay bằng `destination` ID**?_<br/>
> **Cả hai đều có thể dùng**, nhưng **`action` là cách rõ ràng hơn** vì nó mô tả đúng **đường đi đã khai báo** trong graph.<br/>
> Tài liệu Android cho biết:
>
> - `navigate(int)` có thể nhận **resource ID** của `action` hoặc `destination`
> - **`actions` được khuyến khích** vì chúng biểu diễn mối nối giữa các fragment rõ ràng hơn

---

## 3. **Passing _data_** between **Fragments** with `Bundle`

Gửi dữ liệu từ **src fragment** tới **dst fragment** bằng `Bundle` thủ công:

```kotlin
val bundle = Bundle().apply {
    putString("productId", "p001")
    putInt("quantity", 2)
    putBoolean("isEditable", true)
}


findNavController().navigate(
    R.id.action_home_to_detail,
    bundle  // gửi kèm bundle
            // data sẽ được truyền vào arguments của destination
)
```

Nhận data phía **destination**:

```kotlin
// DetailFragment
val productId = arguments?.getString("productId") ?: ""
val quantity  = arguments?.getInt("quantity", 1) ?: 1
```

**`NavOptions`** - custome behavior khi navigate:

```kotlin
val navOptions = NavOptions.Builder()
    // animations
    .setEnterAnim(R.anim.slide_in_right)
    .setExitAnim(R.anim.slide_out_left)
    .setPopEnterAnim(R.anim.slide_in_left)
    .setPopExitAnim(R.anim.slide_out_right)
    // singleTop (launchMode)
    .setLaunchSingleTop(true)
    // back stack
    .setPopUpTo(R.id.homeFragment, false)
    .build()

findNavController().navigate(
    R.id.action_home_to_detail,
    bundle,     // data
    navOptions  // custom navigation behavior
)
```

---

## 4. **Navigate** và **Passing _data_** với **`Safe Args`**

**Safe Args** (type-safe arguments) là `Gradle` plugin **tự động generate class helper để truyền argument type-safe** — không dùng string key, không risk `ClassCastException`.

- `Bundle` thủ công sử `String` key dễ bị **lỗi typo** và lỗi này chỉ biết lúc **runtime**..
- **Safe Args** tự generate class helper với **method type-safe** để truyền argument, giúp **phát hiện lỗi tại compile-time**.
  - `...FragmentDirections` class: **dành cho phía GỬI**, chứa các method để tạo `NavDirections` với **argument type-safe**, sử dụng cho `navigate()` để **truyền dữ liệu RA** destination.
  - `...FragmentArgs` class: **dành cho phía NHẬN**, chứa các method để **lấy argument type-safe**, sử dụng cho các **arguments được truyền VÀO** fragment.

### 4.1. Thêm `Safe Args` plugin

Khai báo **Safe Args** plugin trong `build.gradle` ở **Project Level**:

```gradle
// build.gradle.kts (Project level)
plugins {
    id("androidx.navigation.safeargs.kotlin") version "2.7.7" apply false
}
```

Sử dụng **Safe Args** trong module `app`:

```gradle

// build.gradle.kts (Module: app)
plugins {
    id("com.android.application")
    id("kotlin-android")
    id("androidx.navigation.safeargs.kotlin")  // thêm vào đây
}
```

Sau khi `sync` project, **Safe Args** sẽ tự động **generate các class helper** dựa trên `nav_graph.xml`.

### 4.2. **Class helper** được generated

Với `nav_graph.xml`:

```xml
<fragment android:id="@+id/homeFragment" ...>
    <action
        android:id="@+id/action_home_to_detail"
        app:destination="@id/productDetailFragment" />
</fragment>

<fragment android:id="@+id/productDetailFragment" ...>
    <argument android:name="productId" app:argType="string" />
    <argument android:name="quantity"  app:argType="integer" android:defaultValue="1" />
</fragment>
```

**Safe Args** sẽ generate:

```kotlin
// Từ HomeFragment — class Directions
HomeFragmentDirections
    .actionHomeToDetail( // ứng với action_home_to_detail
        productId = "p001"    // required, không có default
        // quantity không cần truyền vì có defaultValue = 1
    )

// Từ ProductDetailFragment — class Args
class ProductDetailFragmentArgs( // fragment có argument
    val productId: String,
    val quantity: Int = 1
)
```

### 4.3. Gửi `argument` với **Directions** class

```kotlin
// HomeFragment
binding.btnViewDetail.setOnClickListener {
    val action = HomeFragmentDirections.actionHomeToDetail(
        productId = "p001"
    )
    findNavController().navigate(action)
}

// Truyền nhiều argument
binding.btnViewDetail.setOnClickListener {
    val action = HomeFragmentDirections.actionHomeToDetail(
        productId = product.id,
        quantity  = 2
    )
    findNavController().navigate(action)
}
```

### 4.4. Nhận `argument` với **`Args` class + `navArgs()`**

```kotlin
class ProductDetailFragment : Fragment() {

    // navArgs() delegate — tự động đọc từ arguments Bundle
    private val args: ProductDetailFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Truy cập trực tiếp — type-safe, không null (trừ nullable argument)
        val productId = args.productId   // String
        val quantity  = args.quantity    // Int

        binding.tvProductId.text = productId
        binding.tvQuantity.text  = quantity.toString()

        loadProduct(productId)
    }
}
```

> _`by navArgs()` là **property delegate** của Navigation KTX — **lazy load arguments từ Bundle**, tự động cast đúng kiểu._

### 4.5. **Safe Args** với **`Parcelable`**

Có thể truyền object phức tạp (data class) giữa các `Fragment` bằng cách:

- Khai báo argument có `app:argType` là **fully qualified name** của class đó
- class phải implement `Parcelable`.

Định nghĩa class ( và `argType`):

```kotlin
@Parcelize
data class Product(
    val id: String,
    val name: String,
    val price: Double
) : Parcelable
```

```kotlin
<fragment android:id="@+id/detailFragment" ...>
    <argument
        android:name="product"
        app:argType="com.example.app.Product" />  <!-- fully qualified name -->
</fragment>
```

Gửi / nhận object `Parcelable`:

```kotlin
// Gửi
val action = HomeFragmentDirections.actionHomeToDetail(
    product = Product("p001", "Áo thun", 150000.0)
)
findNavController().navigate(action)

// Nhận
val product: Product = args.product
```

---

## 5. **Back stack** management với `NavController`

### 5.1. `popBackStack()`

- `popBackStack()`: **pop Fragment hiện tại** ra khỏi back stack, tương đương với việc nhấn **Back**
- `popBackStack(destinationId, inclusive)`:
  - pop tới `destinationId`
  - nếu `inclusive=true` thì cũng pop luôn `destinationId`

  ```kotlin
  // Pop Fragment hiện tại — tương đương nhấn Back
  findNavController().popBackStack()

  // Pop về destination cụ thể
  findNavController().popBackStack(
      destinationId = R.id.homeFragment,
      inclusive = false   // false: giữ homeFragment | true: xóa luôn
  )
  ```

Ngoài ra, có thể **check backable** trước khi `popBackStack`:

```kotlin
// Kiểm tra trước khi pop
if (findNavController().previousBackStackEntry != null) {
    findNavController().popBackStack()  // còn Fragment để pop
} else {
    requireActivity().finish()          // không còn Fragment phía dưới,
                                        // kết thúc Activity
}
```

Bên cạnh đó, có thể dùng `currentDestination` (`currentBackStackEntry`) để biết `Fragment` hiện tại đang ở đâu:

```kotlin
// Hoặc dùng currentBackStackEntry để biết destination hiện tại
val currentDestination = findNavController().currentDestination
val currentDestinationId = currentDestination?.id
```

### 5.2. `navigateUp()`

- `navigateUp()`: tương đương với việc nhấn **Up** (trên `ActionBar`), sẽ pop tới parent destination trong graph.

  ```kotlin
  // Tương đương Up button trên Toolbar
  // Khác popBackStack(): navigateUp() có thể navigate về parent Activity
  // nếu Fragment này là start destination
  findNavController().navigateUp()
  ```

  > _Khác với `popBackStack()`, `navigateUp()` có thể navigate về **parent Activity** nếu đây là start destination của graph._

---

## 6. Handle **Back** button

Mặc định `NavHostFragment` với `app:defaultNavHost="true"` đã **tự handle Back button**. Nhưng đôi khi bạn cần **custom hành vi Back** trong `Fragment`:

```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // Intercept Back button trong Fragment
    requireActivity().onBackPressedDispatcher.addCallback(
        viewLifecycleOwner,   // lifecycle-aware: tự remove khi Fragment destroyed
        object : OnBackPressedCallback(enabled = true) {
            override fun handleOnBackPressed() {
                // Custom logic trước khi Back

                //
                // isEnabled = false   // disable callback này
                // requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    )
}
```

---

## 7. Navigate với **Deep Link**

Khai báo `<deeplink>` trong `nav_graph.xml`:

```xml
<fragment
    android:id="@+id/productDetailFragment"
    android:name="com.example.app.ProductDetailFragment">

    <argument android:name="productId" app:argType="string" />

    <!-- Deep link URI -->
    <deepLink
        android:id="@+id/deepLink_productDetail"
        app:uri="https://example.com/product/{productId}"
        app:action="android.intent.action.VIEW"
        app:mimeType="*/*" />

</fragment>
```

Cần đưa **nav graph** vào `AndroidManifest.xml` để hệ thống biết `NavController` có thể handle deep link:

```xml
<activity android:name=".MainActivity" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>

    <!-- Deep link intent filter — tự động generate bởi Navigation Component -->
    <nav-graph android:value="@navigation/nav_graph" />
</activity>
```

Handle **deep link** trong `Fragment`:

```kotlin
// Navigate đến URI — NavController tự tìm destination match
val uri = "https://example.com/product/p001".toUri()
findNavController().navigate(uri)

// Với NavDeepLinkRequest — chi tiết hơn
val request = NavDeepLinkRequest.Builder
    .fromUri("https://example.com/product/p001".toUri())
    .build()
findNavController().navigate(request)

// Tạo PendingIntent cho Notification deep link
val pendingIntent = NavDeepLinkBuilder(requireContext())
    .setGraph(R.navigation.nav_graph)
    .setDestination(R.id.productDetailFragment)
    .setArguments(Bundle().apply {
        putString("productId", "p001")
    })
    .createPendingIntent()
```

---

## 8. **Một số Pattern**

### Navigate + Xóa Fragment hiện tại khỏi stack

```kotlin
// Tình huống: Splash → Home, không cho Back về Splash
val navOptions = NavOptions.Builder()
    .setPopUpTo(R.id.splashFragment, true)   // true: xóa luôn splashFragment
    .build()

findNavController().navigate(
    R.id.action_splash_to_home,
    null,
    navOptions
)
```

### Navigate từ `Adapter` (`RecycleView` item click)

```kotlin
// Trong Fragment setup Adapter
class ProductAdapter(
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {
    // ...
}

// Trong Fragment
val adapter = ProductAdapter { product ->
    // Lambda nhận product, navigate trong Fragment
    val action = HomeFragmentDirections.actionHomeToDetail(
        productId = product.id
    )
    findNavController().navigate(action)
}
```

### Tránh **double navigation** — Multiple rapid taps

```kotlin
// Vấn đề: user tap nhanh 2 lần → navigate 2 lần → 2 instance Detail
binding.btnViewDetail.setOnClickListener {
    findNavController().navigate(R.id.action_home_to_detail)
    // Nếu tap nhanh lần 2 trước khi animation xong → navigate lần 2
}

// Fix 1: currentDestination check
binding.btnViewDetail.setOnClickListener {
    if (findNavController().currentDestination?.id == R.id.homeFragment) {
        findNavController().navigate(R.id.action_home_to_detail)
    }
}

// Fix 2: launchSingleTop trong NavOptions
val navOptions = NavOptions.Builder()
    .setLaunchSingleTop(true)
    .build()

binding.btnViewDetail.setOnClickListener {
    findNavController().navigate(R.id.action_home_to_detail, null, navOptions)
}
```

## **Observe navigation result** — Trả kết quả từ Fragment B về Fragment A

**KHÔNG** dùng **Fragment Result API** được khi dùng **Navigation Component**? Có thể dùng `savedStateHandle`:

```kotlin
// Fragment B — gửi result
findNavController().previousBackStackEntry
    ?.savedStateHandle
    ?.set("result_key", "some_result")
findNavController().popBackStack()

// Fragment A — nhận result
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    findNavController().currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<String>("result_key")
        ?.observe(viewLifecycleOwner) { result ->
            // Nhận result từ Fragment B
            binding.tvResult.text = result
        }
}
```
