# **_View_**: `TextView`, `EditText`, `AutoCompleteTextView`, `MultiAutoCompleteTextView`, `TextInputLayout`, `TextInputEditText`

## 1. **`TextView`**

**Vai trò**: `TextView` là một `View` dùng để hiển text **chỉ đọc (`read-only`)** trên màn hình

> _`TextView` là **base class** của toàn bộ nhóm **Text** — mọi View trong nhóm này đều **kế thừa từ `TextView`**_.

Demo: [TextView](/src/app/src/main/res/layout/view__textview.xml)

Thao tác từ code:

- set/get text

  ```kotlin
  // Set text
  binding.tvGreeting.text = "Xin chào"
  binding.tvGreeting.text = getString(R.string.greeting)

  // Set text với format
  binding.tvWelcome.text = getString(R.string.welcome_user, "Nguyễn Văn A")
  // strings.xml: <string name="welcome_user">Xin chào, %1$s!</string>

  // Đọc text hiện tại
  val current = binding.tvGreeting.text.toString()
  ```

- visibility, màu sắc, click listener

  ```kotlin
  // Visibility
  binding.tvError.visibility = View.VISIBLE
  binding.tvError.visibility = View.GONE

  // Thay đổi màu trong code
  binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.red))

  // Click listener trên TextView
  binding.tvRegister.setOnClickListener {
      // navigate to register screen
  }
  ```

**`Spannable`**: Đây là tính năng nâng cao của `TextView` — cho phép áp dụng **style khác nhau cho từng đoạn text** trong cùng một `TextView`:

```kotlin
val spannable = SpannableStringBuilder("Bằng cách tiếp tục, bạn đồng ý với Điều khoản dịch vụ")

// index của đoan "Điều khoản dịch vụ"
val start = spannable.indexOf("Điều khoản dịch vụ")
val end = start + "Điều khoản dịch vụ".length
// color
spannable.setSpan(
    ForegroundColorSpan(Color.BLUE),
    start, end,
    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
)
// underline
spannable.setSpan(
    UnderlineSpan(),
    start, end,
    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
)

binding.tvTerms.text = spannable
binding.tvTerms.movementMethod = LinkMovementMethod.getInstance()
```

---

## 2. **`EditText`**

`EditText` cho phép user **nhập text**. Kế thừa trực tiếp từ `TextView`, nên mọi thuộc tính của TextView đều áp dụng được.

### Các thuộc tính quan trọng:

- `android:inputType` **kiểm soát loại bàn phím** hiện ra và **cách xử lý input**:
  | Type | Mô tả |
  | --- | --- |
  | `text` | Bàn phím chữ thông thường |
  | `textEmailAddress` | Bàn phím có phím `@`, gợi ý email |
  | `textPassword` | Bàn phím **ẩn ký tự**, không gợi ý |
  | `textVisiblePassword` | Bàn phím password nhưng hiển thị ký tự |
  | `number` | Bàn phím chỉ **số nguyên** |
  | `numberDecimal` | Bàn phím **số thập phân** |
  | `numberPassword` | Bàn phím **PIN**, ẩn ký tự |
  | `phone` | Bàn phím **số điện thoại** |
  | `textMultiLine` | Cho phép **xuống dòng** |
  | `textCapSentences` | Tự **viết hoa đầu câu** |
  | `textCapWords` | Tự **viết hoa đầu từ** |
  | `textUri` | Bàn phím URL, có phím `/` và `.` |
  | `datetime` | Bàn phím ngày giờ |

  **Note**: có thể kết hợp nhiều `inputType` bằng cách sử dụng `|`, ví dụ: `android:inputType="textMultiLine|textCapSentences"`

- `imeOptions` kiểm soát **action** trên bàn phím (Enter, Next, Done, Search, ...)

  | Action         | Mô tả                                                             |
  | -------------- | ----------------------------------------------------------------- |
  | `actionNext`   | Hiển thị nút "**Next**", chuyển focus sang **EditText tiếp theo** |
  | `actionDone`   | Hiển thị nút "**Done**", ẩn bàn phím khi nhấn                     |
  | `actionSearch` | Hiển thị nút "**Search**", kích hoạt tìm kiếm khi nhấn            |
  | `actionSend`   | Hiển thị nút "\*_Send_", kích hoạt gửi khi nhấn                   |
  | `actionGo`     | Hiển thị nút "**Go**", kích hoạt hành động khi nhấn               |

- Others:

  ```xml
  <EditText
      android:maxLength="50"              <!-- giới hạn số ký tự tối đa -->
      android:maxLines="4"                <!-- giới hạn số dòng (textMultiLine) -->
      android:selectAllOnFocus="true"     <!-- select all khi focus vào -->
      android:digits="0123456789"         <!-- chỉ cho phép nhập các ký tự này -->
      android:autofillHints="emailAddress"<!-- hỗ trợ autofill OS -->
      android:enabled="false"             <!-- disable, không cho nhập -->
      android:focusable="false" />        <!-- không nhận focus -->
  ```

### Thao tác từ code:

- Đọc/Set text, xóa nội dung:

  ```kotlin
  // Đọc giá trị — luôn dùng .text.toString().trim()
  val email = binding.etEmail.text.toString().trim()
  val password = binding.etPassword.text.toString()

  // Set giá trị
  binding.etEmail.setText("default@email.com")
  // Lưu ý: dùng setText(), KHÔNG dùng .text = "..." với EditText

  // Xóa nội dung
  binding.etEmail.text?.clear()
  ```

- Lắng nghe **sự kiện thay đổi text**:

  ```kotlin
  // Lắng nghe text thay đổi real-time
  binding.etSearch.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
          val query = s.toString()
          // filter danh sách real-time
      }

      override fun afterTextChanged(s: Editable?) {}
  })
  ```

- Lắng nghe nút **action** trên bàn phím:

  ```kotlin
  // Lắng nghe nút action trên bàn phím
  binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_SEARCH) {
          performSearch(binding.etSearch.text.toString())
          true  // consumed
      } else {
          false
      }
  }
  ```

- **Đóng/mở** bàn phím:

  ```kotlin
  // Đóng bàn phím bằng code
  val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  imm.hideSoftInputFromWindow(binding.etEmail.windowToken, 0)

  // Mở bàn phím và focus vào EditText
  binding.etEmail.requestFocus()
  val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  imm.showSoftInput(binding.etEmail, InputMethodManager.SHOW_IMPLICIT)
  ```

### Validation input

```kotlin
private fun validateForm(): Boolean {
    val email = binding.etEmail.text.toString().trim()
    val password = binding.etPassword.text.toString()

    if (email.isEmpty()) {
        binding.etEmail.error = "Email không được để trống"
        binding.etEmail.requestFocus()
        return false
    }

    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        binding.etEmail.error = "Email không hợp lệ"
        binding.etEmail.requestFocus()
        return false
    }

    if (password.length < 6) {
        binding.etPassword.error = "Mật khẩu phải có ít nhất 6 ký tự"
        binding.etPassword.requestFocus()
        return false
    }

    return true
}

binding.btnLogin.setOnClickListener {
    if (validateForm()) {
        // proceed
    }
}
```

---

## 3. `TextInputLayout` + `TextInputEditText`

**Vai trò**: `TextInputLayout` là **wrapper** của **Material Design** bọc bên ngoài `TextInputEditText`. Nó **nâng cấp trải nghiệm `EditText`** lên đáng kể:

- `hint` nổi lên khi focus
- hiển thị **error message** đẹp
- đếm ký tự, toggle hiện/ẩn password.
- helper text hướng dẫn bên dưới field
- ...

> _Trong dự án thực tế hiện đại, cặp `TextInputLayout` + `TextInputEditText` gần như **thay thế hoàn toàn** `EditText` thuần._

```xml
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/tilEmail"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="Email"
    style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
    app:counterEnabled="true"
    app:counterMaxLength="50">

    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/etEmail"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="textEmailAddress"
        android:maxLength="50" />

</com.google.android.material.textfield.TextInputLayout>
```

### `TextInputLayout` props:

- **`style`**: **TextInputLayout** có `3` style chính theo **Material Design 3**:
  - `OutlinedBox` / `OutlinedBox.Dense`: có viền bao quanh, phổ biến nhất
  - `FilledBox`: có nền màu, không viền
  - `ExposedDropdownMenu`: Dùng khi kết hợp với `AutoCompleteTextView` để tạo dropdown chọn option
    > _eg: `style="@style/Widget.Material3.TextInputLayout.OutlinedBox"`_
- `:hint`: hint được đặt trực tiếp ở layout, không đặt ở edittext.
- `:errorEnabled="true"`: pre-allocate không gian cho error<br/>

  Khi này:

  ```kotlin
  // Hiện error — viền chuyển đỏ, text đỏ xuất hiện bên dưới
  binding.tilEmail.error = "Email không hợp lệ"

  // Xóa error
  binding.tilEmail.error = null

  // Hoặc disable hẳn error để thu hồi không gian
  binding.tilEmail.isErrorEnabled = false
  ```

- `:helperTextEnabled="true"` + `:helperText="..."` : text nhỏ màu xám bên dưới, dùng để hướng dẫn hoặc cung cấp thông tin thêm

  > _**Error** vs **Helper Text**: Hai thứ này **không hiện cùng lúc**. Khi có error, error message thay thế helper text. Khi error được xóa, helper text hiện lại._

- `:counterEnabled="true"` + `:counterMaxLength="50"`: hiển thị bộ đếm ký tự, ví dụ "`12/50`", ở góc phải.

  > _Khi **vượt `counterMaxLength`**, counter **chuyển đỏ** — nhưng **không tự block input**. Muốn block phải set `android:maxLength` trên `TextInputEditText`._

- `:endIconMode="..."`: hiển thị icon ở cuối field, có thể là:
  - `password_toggle`: icon mắt để **toggle hiện/ẩn `password`**
  - `clear_text`: icon `x` để xóa text nhanh
  - `dropdown_menu`: icon mũi tên xuống, thường dùng với `AutoCompleteTextView`
  - `custom`: tự cung cấp icon riêng bằng `app:endIconDrawable`
- `:startIconDrawable="..."` / `:endIconDrawable="..."`: hiển thị **icon ở đầu và cuối field**, thường dùng để biểu thị loại input (email, phone, ...)

### `TextInputEditText`:

Tương tự `EditText`, nhưng **bắt buộc phải đặt bên trong `TextInputLayout`** để hoạt động đúng. Mọi thuộc tính của `EditText` đều áp dụng được, đặc biệt là `inputType`.

> _`TextInputLayout` là **container** — nó **không chứa text**. Text nằm trong `TextInputEditText` bên trong._

Có `2` cách truy cập **text** của `TextInputEditText`:

- Qua `ID` của `TextInputEditText`:

  ```kotlin
  // Đặt android:id trực tiếp trên TextInputEditText
  // ex: android:id="@+id/etEmail"
  val email = binding.etEmail.text.toString().trim()
  ```

- Qua `TextInputLayout`:

  ```kotlin
  val email = binding.tilEmail.editText?.text.toString().trim()
  // editText là property của TextInputLayout, trỏ đến View con bên trong
  ```

---

## 4. `AutoCompleteTextView` / `MultiAutoCompleteTextView`

**Vai trò**: `EditText` có thêm **dropdown gợi ý** khi user gõ. Phổ biến trong các trường **tìm kiếm**, chọn thành phố, chọn tag, ...

```xml
<AutoCompleteTextView
    android:id="@+id/actvCity"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="Chọn thành phố"
    android:completionThreshold="1" />  <!-- gợi ý sau khi gõ 1 ký tự -->
```

Cung cấp danh sách từ gợi ý bằng `ArrayAdapter`:

```kotlin
// list of cities to suggest
val cities = listOf("Hà Nội", "Hải Phòng", "Hồ Chí Minh", "Huế", "Đà Nẵng")

// Use ArrayAdapter để
// cung cấp dữ liệu cho AutoCompleteTextView
val adapter = ArrayAdapter(
    this,
    android.R.layout.simple_dropdown_item_1line,
    cities
)
binding.actvCity.setAdapter(adapter)

binding.actvCity.setOnItemClickListener { _, _, position, _ ->
    val selected = cities[position]
}
```

Dùng với `TextInputLayout` với `style=ExposedDropdownMenu` để có trải nghiệm tốt hơn:

```xml
<com.google.android.material.textfield.TextInputLayout
    style="@style/Widget.Material3.TextInputLayout.OutlinedBox.ExposedDropdownMenu"
    android:hint="Thành phố">

    <!-- Dùng AutoCompleteTextView bên trong để có dropdown -->
    <AutoCompleteTextView
        android:id="@+id/actvCity"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="none" />  <!-- inputType="none": không cho gõ tự do -->

</com.google.android.material.textfield.TextInputLayout>
```
