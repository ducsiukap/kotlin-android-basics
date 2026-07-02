# Data Binding **Expressions**

## 1. **_Basic_** Binding expression

### 1.1. **Expression cơ bản - `@{object.property}`/`@{object.method()}`**: truy cập props/method call

- `@{user.name}` — truy cập **prop** của object

  ```xml
  <TextView
      android:text="@{user.name}" />
  ```

- `@{user.getFullName()}` — gọi **method** của object

  ```xml
  <TextView
      android:text="@{user.getFullName()}" />
  ```

Có thể kết hợp **expression** với **`String` template**:

```xml
<TextView
    android:text="@{`Xin chào, ` + user.name}" />
```

### 1.2. **Expression điều kiện - `@{condition? value_if_true : value_if_false}`**: tương đương `if`/`else`

```xml
<ProgressBar
    android:visibility="@{isLoading ? View.VISIBLE : View.GONE}" />

<TextView
    android:text="@{user.email != null ? user.email : `Chưa có email`}" />
```

**Note**: để dùng được `View.VISIBLE`/`View.GONE`, cần **import class - `<import type>` -** trong `<data>` tag:

```xml
<data>
    <import type="android.view.View" />
    <variable name="isLoading" type="Boolean" />
</data>
```

### 1.3. **Expression _null-safe_ - `@{object?.member}`/`@{expression ?? fallback_value_if_null}`**

Lưu ý:

- `@{object?.member}` — _null-safe operator_ (trả về `null` nếu object là `null`)<br/>
  _eg: `android:text="@{user?.name}"` sẽ **nhận `null` nếu user là `null`**_
- `@{expression ?? fallback_value_if_null}` — _null-coalescing operator_ (trả về **fallback value** nếu expression là `null`)<br/>
  _eg: Có thể **kết hợp với `?.`**: `android:text="@{user?.name ?? `Chưa có tên`}"` sẽ hiển thị "Chưa có tên" nếu user là `null`_

```xml
<!--
?? tương đương Elvis operator
?. là safe call operator
 -->
<TextView
    android:text="@{user?.email ?? `Chưa có email`}" />
    <!-- Giả sử cả user (và/hoặc email) là nullable
        -> nếu user là null, hoặc user.email là null,
        -> fallback về default value
        -> sẽ hiển thị "Chưa có email"
    -->
```

### 1.4. **Call method from Click listener**: `@{() -> viewModel.onClick()}`

```xml
<data>
    <variable name="viewModel" type="com.example.UserViewModel" />
</data>

<!-- Thay `setOnClickListener` trong code
        -> click -> gọi trực tiếp
-->
<Button
    android:text="Xóa"
    android:onClick="@{() -> viewModel.deleteUser()}" />
```

---

## 2. Binding with **_`LiveData`_**: **auto observe**

### 2.1. **Auto observe `LiveData`**: `@{viewModel.liveData}`

Với `ViewModel` sử dụng `LiveData`, **Data Binding** có thể **tự động observe**:

```kotlin
class UserViewModel : ViewModel() {
    val userName: LiveData<String> = repository.getUserName()
}
```

Trong **XML layout**, có thể bind trực tiếp `LiveData`:

```xml
<data>
    <variable name="viewModel" type="com.example.UserViewModel" />
</data>

<TextView
    android:text="@{viewModel.userName}" />
    <!-- userName là LiveData<String> trong ViewModel -->
```

Khi này, trong `Activity`/`Fragment`, **không cần observe** `LiveData` nữa, chỉ cần **bind `ViewModel`** vào layout và set `lifecycleOwner`:

```kotlin
// Trong Fragment/Activity — CHỈ cần set lifecycleOwner, KHÔNG cần
// tự viết observe(){} nữa — Binding tự làm điều đó
binding.viewModel = viewModel
binding.lifecycleOwner = viewLifecycleOwner
```

> Đây là **điểm `DataBinding` mạnh hơn `ViewBinding` rõ rệt** — không cần viết `viewModel.userName.observe(this) { binding.tv.text = it }` thủ công.

Nhưng lưu ý: **auto observe CHỈ hoạt động native với `LiveData`**, không tự động với `StateFlow`/`SharedFlow`

### 2.2. Với **`StateFlow`/`SharedFlow`**

Mặc định, **DataBinding KHÔNG tự observe `StateFlow`/`SharedFlow`**

#### 2.2.1. **Cách thủ công**: `setLifecycleOwner` + **convert sang LiveData** bằng `asLiveData()`

```kotlin
class UserViewModel : ViewModel() {
    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    // Tạo thêm bản LiveData CHỈ để dùng cho DataBinding
    val userNameLiveData: LiveData<String> = userName.asLiveData()
}
```

Khi này, trong **XML layout**, bind trực tiếp `LiveData`:

```xml
<TextView android:text="@{viewModel.userNameLiveData}" />
```

Vấn đề:

- Phải viết thêm 1 property riêng cho mỗi `StateFlow`/`SharedFlow` cần bind.
- Code trùng lặp, `ViewModel` phải thay đổi chỉ để phục vụ **Data Binding** / XML

#### 2.2.2. Google **chính thức hỗ trợ `StateFlow`** từ `lifecycle-viewmodel-ktx` — nhưng vẫn **cần set `lifecycleOwner`**

Từ 1 số phiên bản gần đây, Google có thêm **hỗ trợ** generated code cho **`StateFlow` tương tự `LiveData`** — nhưng **điều kiện bắt buộc** vẫn là `setLifecycleOwner()`:

```kotlin
binding.viewModel = viewModel
binding.lifecycleOwner = viewLifecycleOwner  // Bắt buộc — giống LiveData
```

```xml
<TextView android:text="@{viewModel.userName}" />
<!-- userName: StateFlow<String> — CÓ THỂ tự observe NẾU
     project cấu hình đúng dependency + Android Studio đủ mới -->
```

> _Thực tế: Hỗ trợ này **KHÔNG ổn định**/phổ biến bằng LiveData_

Với `StateFlow`/`SharedFlow`, **không** nên cố gắng **bind trực tiếp** vào layout:

- Sử dụng **ViewBinding** và **observe thủ công** với `StateFlow`/`SharedFlow`
- Sử dụng **DataBinding** cho phần **set giá trị** (không auto-observe)

**ViewBinding** là công nghệ mới hơn, và được **sử dụng phổ biến hơn**. _**DataBinding** vẫn được lựa chọn nhưng **không còn được ưu tiên** do mất đi ưu điểm auto-observe với Flow_

---

## 3. **Two-way binding**: `@={}`

Với **One-way Binding - `@{}`**, chỉ đọc từ `ViewModel` -> UI, **KHÔNG ghi** ngược lại:

```xml
<EditText
    android:text="@{viewModel.searchQuery}" />
<!-- searchQuery đổi → EditText đổi theo
     User gõ vào EditText → searchQuery KHÔNG tự đổi -->
```

Với **Two-way Binding - `@={}`**, dữ liệu được **đọc/ghi ĐỒNG BỘ 2 CHIỀU** giữa `ViewModel` <-> UI

```xml
<EditText
    android:text="@={viewModel.searchQuery}" />
<!-- searchQuery đổi → EditText đổi theo
     User gõ vào EditText → searchQuery TỰ ĐỘNG cập nhật theo -->
```

Two-way binding cần `MutableLiveData`:

```kotlin
class SearchViewModel : ViewModel() {
    // Two-way binding cần MutableLiveData — không phải LiveData (read-only)
    val searchQuery = MutableLiveData<String>("")
}
```

---

## 4. **Binding Adapter** - custom attribute

### 4.1. **What** is `BindingAdapter`

Vấn đề: **Data Binding chỉ hỗ trợ sẵn 1 số attribute cơ bản** như (`text`, `visibility`...). <br/>
Muốn **bind data vào attribute không có sẵn** (_vd: load ảnh từ URL vào ImageView bằng Glide_) → cần viết `BindingAdapter` riêng.

Định nghĩa: `BindingAdapter` là 1 **hàm `static`**, được annotation processor của Data Binding **generate thành code gọi tự động mỗi khi attribute XML tương ứng thay đổi giá trị**.

```kotlin
// Định nghĩa custom attribute "app:imageUrl"
object BindingAdapters {

    @JvmStatic
    @BindingAdapter("imageUrl")
    fun loadImage(imageView: ImageView, url: String?) {
        if (!url.isNullOrEmpty()) {
            Glide.with(imageView)
                .load(url)
                .placeholder(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.ic_default_avatar)
        }
    }
}
```

```xml
<data>
    <variable name="contact" type="com.example.contactapp.data.local.Contact" />
</data>

<ImageView
    android:id="@+id/iv_avatar"
    android:layout_width="48dp"
    android:layout_height="48dp"
    app:imageUrl="@{contact.avatarUri}" />
    <!-- Gọi thẳng BindingAdapters.loadImage() — không cần code Kotlin
         trong Fragment như đã làm ở ContactAdapter.kt trước đây -->
```

Bản chất thật sự — **KHÔNG có gì "ma thuật"**: `app:imageUrl="@{contact.avatarUri}"` trong XML được **Annotation processor SINH RA code** Java/Kotlin **tương đương**:

```kotlin
BindingAdapters.loadImage(imageView, contact.avatarUri)
```

→ Được **gọi lại MỖI KHI `contact.avatarUri` thay đổi** giá trị nếu:

- `contact` là `LiveData`/`Observable`, hoặc
- mỗi lần bạn gọi `binding.contact = newContact`

### 4.2. **Signature**

Trong hàm với annotation `BindingAdapter("attr")`, **tham số đầu tiên** luôn là `View` hoặc subclass của nó **tương ứng với tag trong layout được gắn attribute `app:attr` đó**:

```kotlin
@BindingAdapter("imageUrl")
fun loadImage(
    view: ImageView,      // Tham số 1 — LUÔN là View (hoặc subclass của View)
                           // ứng với TAG XML mà attribute này gắn vào
    url: String?           // Tham số 2 — kiểu dữ liệu của giá trị bind vào
) {
    Glide.with(view).load(url).into(view)
}
```

`@JvmStatic` là **annotation BẮT BUỘC** nếu định nghĩa **Kotlin `object`**:

```kotlin
// ✅ Đúng — object + @JvmStatic
object BindingAdapters {

    @JvmStatic
    @BindingAdapter("imageUrl")
    fun loadImage(view: ImageView, url: String?) {
        Glide.with(view).load(url).into(view)
    }
}
```

Hoặc sử dụng **top-level function**:

```kotlin
// ✅ Cũng đúng — top-level function, KHÔNG cần @JvmStatic
@BindingAdapter("imageUrl")
fun loadImage(view: ImageView, url: String?) {
    Glide.with(view).load(url).into(view)
}
```

### 4.3. **`BindingAdapter` cho NHIỀU tham số**

```kotlin
@BindingAdapter(
    value = ["errorText", "hasError"],  // danh sách tham số
    requireAll = false                  // bắt buộc View phải có toàn bộ các tham số này?
)
fun setError(
    editText: TextInputEditText,
    errorText: String?,
    hasError: Boolean
) {
    val layout = editText.parent.parent as? TextInputLayout
    layout?.error = if (hasError) errorText else null
}
```

Tham số `requireAll`:

- `requireAll = true`: yêu cầu View phải có toàn bộ các tham số adapter
  > _Nếu thiếu, `BindingAdapter` không được gọi._
- `requireAll = false`: View chỉ cần ít nhất **MỘT `attribute` được khai báo** để trigger adapter.
  > _Trong trường hợp này, các **attribute còn thiếu** sẽ nhận **default-value** như `null`/`false`/`0`, ..._

```xml
<!-- Trường hợp 1 — có ĐỦ cả 2 attribute -->
<TextInputEditText
    app:errorText="@{viewModel.phoneError}"
    app:hasError="@{viewModel.hasPhoneError}" />

<!-- Trường hợp 2 — chỉ có 1 attribute, attribute còn lại
     dùng giá trị default (null cho object, false cho Boolean) -->
<TextInputEditText
    app:errorText="@{viewModel.phoneError}" />
```

Ví dụ:

```kotlin
// Bind toàn bộ trạng thái của 1 nút Like — icon + text + màu cùng lúc
@BindingAdapter(value = ["isLiked", "likeCount"], requireAll = true)
fun bindLikeButton(button: MaterialButton, isLiked: Boolean, likeCount: Int) {
    button.text = likeCount.toString()
    button.setIconResource(
        if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
    )
    button.setIconTintResource(
        if (isLiked) R.color.red else R.color.gray
    )
}
```

```xml
<com.google.android.material.button.MaterialButton
    app:isLiked="@{viewModel.isLiked}"
    app:likeCount="@{viewModel.likeCount}" />
```

> _Cách này **gom nhiều attribute liên quan vào 1 hàm xử lý**, đảm bảo l**ogic nhất quán** (icon + text + màu luôn đồng bộ) — thay vì viết **3 `BindingAdapter` riêng lẻ** dễ dẫn đến **trạng thái không khớp nhau**._

### 4.3. **`BindingAdapter` với attribute CÓ SẴN**

Có thể **override default behavior** khi tạo `BindingAdapter` trên attribute có sẵn:

Ví dụ:

- `android:src` mặc định **CHỈ nhận** `@drawable/xxx` (_fixed resouce ID_)
- Khi muốn **binding động** → cần định nghĩa lại attribute "`android:src`" bằng `BindingAdapter`

```kotlin
// Không dùng tên custom "app:xxx" — dùng LẠI attribute Android
// gốc "android:src" để CHÈN THÊM logic khi giá trị thay đổi
@BindingAdapter("android:src")
fun setImageResource(view: ImageView, resId: Int) {
    view.setImageResource(resId)
}
```

```xml
<!-- Giờ android:src có thể nhận biến động, không chỉ resource tĩnh -->
<ImageView android:src="@{viewModel.iconResId}" />
```

### 4.4. `oldValue`: optimize performance

```kotlin
@BindingAdapter("imageUrl")
fun loadImage(view: ImageView, oldUrl: String?, newUrl: String?) {
    // Nếu khai báo 2 tham số String? liên tiếp (không phải View + 1 giá trị)
    // → Data Binding TỰ ĐỘNG truyền cả giá trị CŨ và MỚI

    if (oldUrl == newUrl) return  // Không đổi gì — bỏ qua, tránh load lại

    Glide.with(view).load(newUrl).into(view)
}
```

Dùng khi: **thao tác trong `BindingAdapter` có COST cao** (network call, animation nặng...) — muốn **tránh chạy lại** nếu giá trị thực ra KHÔNG đổi (dù DataBinding gọi lại do re-bind)

### 4.5. **`@BindingMethods`**

Khác với `BindingAdapter` (viết hàm mới), `BindingMethod` chỉ **map lại tên 1 `method đã tồn tại` sẵn trên View thành `attribute`**:

```kotlin
@BindingMethods(
    BindingMethod(
        type = SwipeRefreshLayout::class,
        attribute = "app:refreshing",
        method = "setRefreshing"   // Method GỐC đã có sẵn trong SwipeRefreshLayout
    )
)
class BindingAdapters
```

```xml
<androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    app:refreshing="@{viewModel.isRefreshing}" />
<!-- Tương đương gọi: swipeRefreshLayout.setRefreshing(viewModel.isRefreshing) -->
```

---

## 5. `InverseBindingAdapter` — Hỗ trợ **Two-way Binding** cho **Custom View**

Nếu muốn `@={}` (two-way binding) **hoạt động với 1 `custom attribute` tự định nghĩa** (_không phải attribute có sẵn như android:text_), cần **thêm cặp `@InverseBindingAdapter` + `@InverseBindingListener`**:

```kotlin
// Chiều 1 — SET giá trị vào View (giống BindingAdapter thường)
@BindingAdapter("app:rating")
fun setRating(view: RatingBar, rating: Float) {
    if (view.rating != rating) {
        view.rating = rating
    }
}

// Chiều 2 — ĐỌC giá trị TỪ View để đẩy ngược lại ViewModel
@InverseBindingAdapter(attribute = "app:rating")
fun getRating(view: RatingBar): Float {
    return view.rating
}

// Đăng ký LISTENER để biết KHI NÀO cần đọc lại (user tương tác)
@BindingAdapter("app:ratingAttrChanged")
fun setRatingListener(view: RatingBar, listener: InverseBindingListener?) {
    view.setOnRatingBarChangeListener { _, _, _ ->
        listener?.onChange()  // Báo cho Data Binding: "giá trị vừa đổi, đọc lại đi"
    }
}
```

Với `app:attribute` và sử dụng **two-way binding** thì DataBinding quy ước thêm `app:attributeAttrChanged` để **listen thay đổi ngược từ view**.

```xml
<RatingBar app:rating="@={viewModel.userRating}" />
```
