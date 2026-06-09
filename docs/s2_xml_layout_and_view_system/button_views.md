# **_Button_ & _Clickable_** Views

## 1. **Button** group

**`Button` kế thừa từ `TextView`** — tức là mọi **thuộc tính** của `TextView` đều **dùng được trên Button**: `textSize`, `textColor`, `textStyle`, `drawableStart`...

```
View
└── TextView
    └── Button
        ├── MaterialButton   ← Material Design, dùng trong thực tế
        └── (AppCompatButton)
```

### 1.1. Các loại **Button** theo **Material Design 3**: `MaterialButton`

> _**Material Design 3** định nghĩa **`5` loại Button** với **mức độ nhấn mạnh khác nhau**, được chỉ định thông qua `style`_

| Button type              | Description                              | Example use case |
| ------------------------ | ---------------------------------------- | ---------------- |
| **`Filled`** (_default_) | **Hành động chính**, quan trọng nhất     | [ Login ]        |
| **`Outlined`**           | Hành động phụ, cùng cấp với **`Filled`** | [ Cancel ]       |
| **`Elevated`**           | **Filled** nhưng có thêm shadow          | [ Xem thêm ]     |
| **`Tonal`**              | Hành động **quan trọng thứ hai**         | [ Tiếp tục ]     |
| **`Text`**               | Hành động phụ, ít quan trọng hơn         | Bỏ qua           |

```xml
<!-- Filled — mặc định khi dùng <Button> với Material theme -->
<com.google.android.material.button.MaterialButton
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Đăng nhập"
    style="@style/Widget.Material3.Button" />

<!-- Tonal -->
<com.google.android.material.button.MaterialButton
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Tiếp tục"
    style="@style/Widget.Material3.Button.TonalButton" />

<!-- Outlined -->
<com.google.android.material.button.MaterialButton
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Hủy bỏ"
    style="@style/Widget.Material3.Button.OutlinedButton" />

<!-- Elevated -->
<com.google.android.material.button.MaterialButton
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Xem thêm"
    style="@style/Widget.Material3.Button.ElevatedButton" />

<!-- Text -->
<com.google.android.material.button.MaterialButton
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Bỏ qua"
    style="@style/Widget.Material3.Button.TextButton" />
```

### 1.2. **Icon Button**

Có thể **thêm icon vào Button** bằng cách bổ sung vào `MaterialButton` các thuộc tính:

- `app:icon`: chỉ định icon (dùng `@drawable/` hoặc `@mipmap/`)
- `app:iconGravity`: vị trí của icon so với text (`textStart`, `textEnd`, `top`, `textTop`, ...)
- `app:iconPadding`: khoảng cách giữa icon và text

```xml
<com.google.android.material.button.MaterialButton
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Thêm ảnh"
    app:icon="@drawable/ic_add_photo"
    app:iconGravity="textStart"       <!-- vị trí icon: textStart|textEnd|top|textTop -->
    app:iconPadding="8dp"             <!-- khoảng cách icon ↔ text -->
    style="@style/Widget.Material3.Button.OutlinedButton" />
```

**Icon-only Button** — chỉ có icon, không có text:

```xml
<com.google.android.material.button.MaterialButton
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:icon="@drawable/ic_favorite"
    style="@style/Widget.Material3.Button.IconButton" />
```

### 1.3. **Các thuộc tính khác của Button**

- `android:enabled`: bật/tắt button
- `android:minHeight`: chiều cao tối thiểu của button (theo Material Design, nên là `48dp` để đảm bảo touch target đủ lớn)
- `app:cornerRadius`: bo góc button
- `app:rippleColor`: màu hiệu ứng ripple khi tap vào button

```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btnSubmit"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Xác nhận"
    android:enabled="true"              <!-- bật/tắt button -->
    android:minHeight="48dp"            <!-- touch target tối thiểu theo Material -->
    app:cornerRadius="8dp"              <!-- bo góc -->
    app:rippleColor="@color/ripple"     <!a-- màu hiệu ứng ripple khi tap -->
    style="@style/Widget.Material3.Button" />
```

### 1.4. Click handlers

- `setOnClickListener`: xử lý sự kiện click thông thường

  ```kotlin
  // Cách 1: setOnClickListener — phổ biến nhất
  binding.btnLogin.setOnClickListener {
      performLogin()
  }

  // Cách 2: setOnClickListener với View parameter
  binding.btnLogin.setOnClickListener { view ->
      view.isEnabled = false   // disable sau khi click để tránh double-click
      performLogin()
  }
  ```

- `setOnLongClickListener`: xử lý sự kiện long click (nhấn giữ)

  ```kotlin
  // Long click handler
  binding.btnLogin.setOnLongClickListener {
      Toast.makeText(this, "Bạn đã nhấn giữ nút Login", Toast.LENGTH_SHORT).show()

      // return
      //    + true nếu đã xử lý sự kiện
      //    + false để cho phép tiếp tục xử lý click thông thường
      true
  }
  ```

- `isEnabled`: bật/tắt button (disable sẽ làm button bị mờ đi và không click được)

  ```kotlin
  // Enable/Disable
  binding.btnSubmit.isEnabled = false // disable — grayed out, không click được
  binding.btnSubmit.isEnabled = true // enable lại
  ```

- `performClick()`: trigger click -> gọi `OnClickListener` đã set trước đó trên button được trigger mà không cần tương tác từ người dùng.

  ```kotlin
  // Programmatically trigger click
  binding.btnSubmit.performClick()
  ```

### 1.5. `ButtonGroup` — Nhóm Button liên quan

```
MaterialButtonGroup
├── MaterialButtonToggleGroup
└── MaterialSplitButton
```

**Material Design 3** định nghĩa một số **nhóm Button** để nhóm các Button liên quan với nhau:

- `MaterialButtonGroup`: container cơ bản cho một nhóm `MaterialButton` liên quan với nhau, là **base** cho các `ButtonGroup` cụ thể hơn.
- `MaterialButtonToggleGroup`: nhóm các `MaterialButton` có thể **toggle** (chọn 1 hoặc nhiều button trong nhóm)
  - `:singleSelection="true"`: chỉ cho phép **chọn 1 button** trong nhóm (giống RadioGroup)
  - `:selectionRequired="true"`: yêu cầu **phải chọn ít nhất 1 button** trong nhóm
- `MaterialSplitButton`: nhóm các `MaterialButton` có một button chính (**primary**) và một button phụ (**secondary**) thường dùng cho các hành động liên quan đến nhau, ví dụ: `[ Main Action ]` + `[ ▼ Options ]`

**`MaterialButtonToggleGroup`** — là loại thường dùng nhất khi nói đến Button Group.

```xml
<com.google.android.material.button.MaterialButtonToggleGroup
    android:id="@+id/toggleGroup"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:singleSelection="true"          <!-- chỉ chọn 1 button tại 1 thời điểm -->
    app:selectionRequired="true">       <!-- bắt buộc phải có 1 button được chọn -->

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnDay"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Ngày"
        style="@style/Widget.Material3.Button.OutlinedButton" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnWeek"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Tuần"
        style="@style/Widget.Material3.Button.OutlinedButton" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnMonth"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Tháng"
        style="@style/Widget.Material3.Button.OutlinedButton" />

</com.google.android.material.button.MaterialButtonToggleGroup>
```

Xử lý:

- Lắng nghe sự kiện click button trong group bằng `setOnButtonCheckedListener`:

  ```kotlin
  binding.toggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
      if (isChecked) {
          when (checkedId) {
              R.id.btnDay   -> loadDayData()
              R.id.btnWeek  -> loadWeekData()
              R.id.btnMonth -> loadMonthData()
          }
      }
  }
  ```

- Lấy `id` của **button đang được chọn** bằng `checkedButtonId`:

  ```kotlin
  val checkedId = binding.groupViewMode.checkedButtonId
  ```

---

## 2. **ImageButton** — Button chỉ có icon, không có text

**Vai trò**: `ImageButton` đại diện cho loại button **chỉ hiển thị icon**, không có text. Thường dùng trong _Toolbar_, _header_, hoặc các _action nhỏ_.

```xml
<ImageButton
    android:id="@+id/btnBack"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:src="@drawable/ic_arrow_back"
    android:contentDescription="Quay lại"    <!-- bắt buộc cho accessibility -->
    android:background="?attr/selectableItemBackgroundBorderless" />
    <!--
        selectableItemBackgroundBorderless → ripple effect hình tròn
        selectableItemBackground           → ripple effect hình chữ nhật
    -->
```

Thực tế, hiện nay `ImageButton` gần như **được thay thế hoàn toàn** bởi `MaterialButton` với `style="@style/Widget.Material3.Button.IconButton"` vì tích hợp tốt hơn với **Material theme**.

---

## 3. **`FAB` - Floating Action Button**

### 3.1. **`FloatingActionButton`**

**Vai trò**: `FloatingActionButton` (FAB) là nút **tròn nổi**, đại diện cho **hành động chính** và **nổi bật nhất** trên màn hình.

> _Theo **Material Design**, mỗi **màn hình** chỉ nên có **một FAB duy nhất**._

```xml
<com.google.android.material.floatingactionbutton.FloatingActionButton
    android:id="@+id/fab"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_margin="16dp"

    android:layout_gravity="bottom|end" <!-- vị trí của FAB trong container -->
    android:src="@drawable/ic_add" <!-- icon hiển thị trên FAB -->
    android:contentDescription="Thêm mới"
    app:fabSize="normal"   <!-- normal | mini | auto -->
    />
```

`FAB` thường nằm trong `FrameLayout` hoặc `CoordinatorLayout` để có thể **nổi lên trên** / **tránh** các nội dung khác như `SnackBar`, ....

```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- Content -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior" />

    <!-- FAB tự động dịch lên khi Snackbar xuất hiện -->
    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fab"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_margin="16dp"
        android:src="@drawable/ic_add" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### 3.2. **ExtendedFloatingActionButton** - FAB có text

```xml
<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
    android:id="@+id/extFab"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_gravity="bottom|end"
    android:layout_margin="16dp"
    android:text="Tạo mới"
    app:icon="@drawable/ic_add" />
```

---

## 4. `RadioButton` & Radio Group

**Nguyên tắc**: **`RadioButton` phải được đặt bên trong `RadioGroup`**. `RadioGroup` đảm bảo **chỉ có một `RadioButton` được chọn** tại một thời điểm — chọn cái này sẽ tự động bỏ chọn cái kia.

```xml
<RadioGroup
    android:id="@+id/rgGender"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:orientation="horizontal">

    <RadioButton
        android:id="@+id/rbMale"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Nam"
        android:checked="true" />      <!-- mặc định chọn -->

    <RadioButton
        android:id="@+id/rbFemale"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Nữ" />

    <RadioButton
        android:id="@+id/rbOther"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Khác" />

</RadioGroup>
```

Xử lý:

- Lắng nghe sự kiện thay đổi lựa chọn bằng `setOnCheckedChangeListener`:

  ```kotlin
  // Lắng nghe thay đổi
  binding.rgGender.setOnCheckedChangeListener { group, checkedId ->
      when (checkedId) {
          R.id.rbMale   -> selectedGender = "male"
          R.id.rbFemale -> selectedGender = "female"
          R.id.rbOther  -> selectedGender = "other"
      }
  }
  ```

- **Đọc** giá trị đang chọn, **set** chọn theo ID, hoặc **bỏ chọn tất cả**:

  ```kotlin
  // Đọc giá trị đang chọn
  val checkedId = binding.rgGender.checkedRadioButtonId
  val radioButton = findViewById<RadioButton>(checkedId)
  val selectedText = radioButton.text.toString()

  // Set chọn theo ID
  binding.rgGender.check(R.id.rbFemale)

  // Bỏ chọn tất cả
  binding.rgGender.clearCheck()
  ```

---

## 5. `CheckBox` - Button có thể chọn nhiều

**Đặc điểm**: Khác với `RadioButton`, các **`CheckBox` độc lập với nhau** — chọn cái này không ảnh hưởng cái kia. Dùng khi user **có thể chọn nhiều option cùng lúc**.

```xml
<CheckBox
    android:id="@+id/cbTerms"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Tôi đồng ý với điều khoản dịch vụ"
    android:checked="false" />
```

Xử lý:

```kotlin
// Đọc trạng thái
val isChecked = binding.cbTerms.isChecked     // true / false

// Set trạng thái
binding.cbTerms.isChecked = true

// Lắng nghe thay đổi
binding.cbTerms.setOnCheckedChangeListener { buttonView, isChecked ->
    binding.btnRegister.isEnabled = isChecked   // enable button chỉ khi tick
}

// Toggle
binding.cbRememberMe.toggle()
```

---

## 6. **`Switch` - Button chuyển trạng thái bật/tắt**

**Đặc điểm**: `Switch` biểu diễn **trạng thái bật/tắt** — dùng cho _setting_, _preferences_.

> _Về mặt logic **giống `CheckBox`**, nhưng UX phù hợp hơn cho **setting toggle**._

```xml
<com.google.android.material.switchmaterial.SwitchMaterial
    android:id="@+id/switchNotification"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Nhận thông báo"
    android:checked="true"
    android:layout_marginVertical="8dp" />
```

> _Dùng `SwitchMaterial` (từ **Material library**) thay vì `Switch` thuần để **đảm bảo style nhất quán** với Material theme._

Xử lý:

```kotlin
binding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
    if (isChecked) {
        enableNotifications()
    } else {
        disableNotifications()
    }
}

// Đọc trạng thái
val isOn = binding.switchNotification.isChecked

// Set trạng thái mà không trigger listener
binding.switchNotification.setOnCheckedChangeListener(null)
binding.switchNotification.isChecked = true
binding.switchNotification.setOnCheckedChangeListener { _, isChecked -> ... }
```

---

## 7. `Chip`

**Vai trò**: `Chip` là component **Material Design** — dạng `tag` nhỏ compact, đa năng. <br/>
`Chip` có **`4` loại** phục vụ các mục đích khác nhau:

- **`Action` Chip**: Trigger một hành động (như Button nhỏ)
- **`Filter` Chip**: Lọc danh sách, có thể chọn/bỏ chọn
- **`Input` Chip**: Tag input của user, có thể xóa (`X`)
- **`Suggestion` Chip**: Gợi ý từ hệ thống, không có trạng thái chọn

```xml
<!-- Filter Chip — chọn/bỏ chọn để lọc -->
<com.google.android.material.chip.Chip
    android:id="@+id/chipKotlin"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Kotlin"
    style="@style/Widget.Material3.Chip.Filter"
    app:chipIcon="@drawable/ic_kotlin"
    android:checkable="true" />

<!-- Input Chip — có nút X để xóa -->
<com.google.android.material.chip.Chip
    android:id="@+id/chipTag"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Android"
    style="@style/Widget.Material3.Chip.Input"
    app:closeIconVisible="true" />
```

### **`ChipGroup` - Nhóm các Chip liên quan**

```xml
<com.google.android.material.chip.ChipGroup
    android:id="@+id/chipGroup"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:singleSelection="false"         <!-- true: chỉ chọn 1 | false: chọn nhiều -->
    app:chipSpacingHorizontal="8dp">

    <com.google.android.material.chip.Chip
        android:id="@+id/chipKotlin"
        style="@style/Widget.Material3.Chip.Filter"
        android:text="Kotlin"
        android:checkable="true" />

    <com.google.android.material.chip.Chip
        android:id="@+id/chipAndroid"
        style="@style/Widget.Material3.Chip.Filter"
        android:text="Android"
        android:checkable="true" />

    <com.google.android.material.chip.Chip
        android:id="@+id/chipJetpack"
        style="@style/Widget.Material3.Chip.Filter"
        android:text="Jetpack"
        android:checkable="true" />

</com.google.android.material.chip.ChipGroup>
```

Xử lý:

- Lắng nghe sự kiện thay đổi lựa chọn bằng `setOnCheckedStateChangeListener`:

  ```kotlin
  // Lắng nghe check change trên ChipGroup
  binding.chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
      val selectedChips = checkedIds.map { id ->
          group.findViewById<Chip>(id).text.toString()
      }
      // selectedChips = ["Kotlin", "Android"]
  }
  ```

- Thêm Chip động vào ChipGroup:

  ```kotlin

  // Thêm Chip động vào ChipGroup
  val chip = Chip(this).apply {
      text = "Tag mới"
      isCloseIconVisible = true
      setOnCloseIconClickListener {
          binding.chipGroup.removeView(this)
      }
  }
  binding.chipGroup.addView(chip)
  ```

- Đọc tất cả Chip đang được chọn:

  ```kotlin
  // Đọc tất cả Chip đang được chọn
  val selected = binding.chipGroup.checkedChipIds.map { id ->
      binding.chipGroup.findViewById<Chip>(id).text.toString()
  }
  ```
