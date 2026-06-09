# **_Selection_ / _Picker_** views

## 1. `Spinner` - select

`Spinner` là **dropdown** cho phép user **chọn một option** từ danh sách. Khi tap vào, một popup xuất hiện liệt kê tất cả option — user chọn xong popup đóng lại, Spinner hiển thị option đã chọn.

```xml
<Spinner
    android:id="@+id/spinnerCity"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:spinnerMode="dropdown"     <!-- dropdown (mặc định) | dialog -->
    android:prompt="@string/select_city" /> <!-- tiêu đề khi mode=dialog -->
```

`spinnerMode` có 2 giá trị:

- `dropdown` (_mặc định_): hiển thị **popup** ngay **dưới Spinner**.
- `dialog`: hiển thị **AlertDialog** giữa màn hình, có tiêu đề (được set bằng `android:prompt`) và danh sách option.

### Setup **Adapter** cho `Spinner`

`Spinner` không tự biết hiển thị gì — **phải cung cấp data** thông qua **Adapter**: `ArrayAdapter`, ...

- **Cách 1**: từ `List` trong code -> **dữ liệu động**

  ```kotlin
  // Cách 1: Từ List trong code
  val cities = listOf("Hà Nội", "TP. Hồ Chí Minh", "Đà Nẵng", "Cần Thơ", "Huế")

  val adapter = ArrayAdapter(
      this,
      android.R.layout.simple_spinner_item,          // layout cho item đang hiển thị
      cities
  )
  adapter.setDropDownViewResource(
      android.R.layout.simple_spinner_dropdown_item  // layout cho item trong dropdown
  )
  binding.spinnerCity.adapter = adapter
  ```

- **Cách 2**: từ `string-array` trong XML -> **dữ liệu tĩnh**

  ```xml
  <!--
      Từ string-array trong
      res/values/arrays.xml
  -->
  <string-array name="cities">
      <item>Hà Nội</item>
      <item>TP. Hồ Chí Minh</item>
      <item>Đà Nẵng</item>
      <item>Cần Thơ</item>
      <item>Huế</item>
  </string-array>
  ```

  Adapter:

  ```kotlin
  val adapter = ArrayAdapter.createFromResource(
      this,
      R.array.cities,
      android.R.layout.simple_spinner_item
  )
  adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
  binding.spinnerCity.adapter = adapter
  ```

### **Event handler** cho `Spinner`

```kotlin
// Lắng nghe thay đổi
binding.spinnerCity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

    override fun onItemSelected(
        parent: AdapterView<*>,
        view: View?,
        position: Int,
        id: Long
    ) {
        val selected = parent.getItemAtPosition(position).toString()
        // hoặc:
        val selected = cities[position]
        binding.tvSelectedCity.text = "Đã chọn: $selected"
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Gọi khi không có item nào được chọn
        // Rất ít khi xảy ra với Spinner
    }
}

// Đọc giá trị đang chọn
val selectedPosition = binding.spinnerCity.selectedItemPosition  // Int
val selectedItem = binding.spinnerCity.selectedItem.toString()   // String

// Set lựa chọn theo vị trí
binding.spinnerCity.setSelection(2)   // chọn item index 2

// Set lựa chọn theo giá trị
val targetCity = "Đà Nẵng"
val position = cities.indexOf(targetCity)
if (position >= 0) binding.spinnerCity.setSelection(position)
```

### `Spinner` vs `AutoCompleteTextView`+`TextInputLayout`

Trong thực tế hiện đại, `Spinner` gần như **bị thay thế** bởi `AutoCompleteTextView` bên trong `TextInputLayout` với **style** `ExposedDropdownMenu`. Lý do:

```
Spinner                          AutoCompleteTextView + TIL
├── Không theo Material Design   ├── Material Design 3
├── Khó custom style             ├── Dễ style, nhất quán với form
├── Không có floating hint       ├── Có floating hint
└── Không có error message       └── Có error message
```

Chi tiết: [Text group](./text_views.md#4-autocompletetextview--multiautocompletetextview)

---

## 2. `DatePicker`

`DatePicker` cho phép user chọn **ngày tháng năm**. Có 2 mode hiển thị:

- **Inline** → Nhúng trực tiếp vào layout — chiếm không gian cố định
- **Dialog** → Hiện trong **popup** khi cần → **phổ biến hơn**

### Inline `DatePicker`: `calendarViewShown=true` + `spinnersShown=false`

```xml
<DatePicker
    android:id="@+id/datePicker"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:calendarViewShown="true"    <!-- hiện dạng calendar -->
    android:spinnersShown="false"       <!-- ẩn dạng spinner -->
    android:minDate="01/01/2000"        <!-- ngày tối thiểu (mm/dd/yyyy) -->
    android:maxDate="12/31/2030" />     <!-- ngày tối đa -->
```

Đọc giá trị từ `DatePicker`:

```kotlin
val year  = binding.datePicker.year
val month = binding.datePicker.month   // 0-based: 0=January, 11=December
val day   = binding.datePicker.dayOfMonth

// Tạo Calendar object từ giá trị đọc được
val calendar = Calendar.getInstance()
calendar.set(year, month, day)
val selectedDate = calendar.time   // Date object

// Format thành String
val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
val dateString = sdf.format(selectedDate)
```

### `DatePickerDialog`: hiển thị `DatePicker` trong dialog

```kotlin
private fun showDatePickerDialog() {
    val calendar = Calendar.getInstance()

    val dialog = DatePickerDialog(
        this,
        { _, year, month, dayOfMonth ->
            // month là 0-based → +1 để hiển thị đúng
            val selected = "$dayOfMonth/${month + 1}/$year"
            binding.tvSelectedDate.text = selected
        },
        calendar.get(Calendar.YEAR),    // năm mặc định = năm hiện tại
        calendar.get(Calendar.MONTH),   // tháng mặc định = tháng hiện tại
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Giới hạn ngày
    dialog.datePicker.minDate = System.currentTimeMillis()  // không cho chọn ngày quá khứ

    dialog.show()
}

// trigger
binding.btnPickDate.setOnClickListener {
    showDatePickerDialog()
}
```

### `MaterialDatePicker`

**Google** khuyến nghị dùng `MaterialDatePicker` từ **Material Design library** thay cho `DatePickerDialog` thuần vì đẹp hơn và nhất quán với Material theme:

```kotlin
// Single date picker
private fun showMaterialDatePicker() {
    val picker = MaterialDatePicker.Builder.datePicker()
        .setTitleText("Chọn ngày sinh")
        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())   // mặc định hôm nay
        .build()

    picker.addOnPositiveButtonClickListener { selection ->
        // selection là Long (milliseconds UTC)
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateString = sdf.format(Date(selection))
        binding.tvSelectedDate.text = dateString
    }

    picker.addOnNegativeButtonClickListener {
        // user nhấn Cancel
    }

    picker.show(supportFragmentManager, "DATE_PICKER")
}
```

```kotlin
// Date range picker — chọn khoảng ngày (check-in / check-out)
private fun showDateRangePicker() {
    val picker = MaterialDatePicker.Builder.dateRangePicker()
        .setTitleText("Chọn ngày đặt phòng")
        .setSelection(
            Pair(
                MaterialDatePicker.todayInUtcMilliseconds(),
                MaterialDatePicker.todayInUtcMilliseconds()
            )
        )
        .build()

    picker.addOnPositiveButtonClickListener { selection ->
        val startDate = selection.first    // Long
        val endDate   = selection.second   // Long
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.tvCheckIn.text  = sdf.format(Date(startDate))
        binding.tvCheckOut.text = sdf.format(Date(endDate))
    }

    picker.show(supportFragmentManager, "DATE_RANGE_PICKER")
}
```

---

## 3. `TimePicker`

Tương tự `DatePicker`, `TimePicker` cũng có 2 mode: **inline** và **dialog**.

### Inline `TimePicker`:

```xml
<TimePicker
    android:id="@+id/timePicker"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:timePickerMode="clock"     <!-- clock (mặc định) | spinner -->
    />
```

Đọc giá trị từ `TimePicker`:

```kotlin
// API 23+
val hour   = binding.timePicker.hour    // 0–23
val minute = binding.timePicker.minute  // 0–59

val timeString = String.format("%02d:%02d", hour, minute)
binding.tvSelectedTime.text = timeString
```

### `TimePickerDialog`:

```kotlin
private fun showTimePickerDialog() {
    val calendar = Calendar.getInstance()
    val currentHour   = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)

    val dialog = TimePickerDialog(
        this,
        { _, hourOfDay, minute ->
            val timeString = String.format("%02d:%02d", hourOfDay, minute)
            binding.tvSelectedTime.text = timeString
        },
        currentHour,
        currentMinute,
        true    // true = 24h format | false = 12h (AM/PM)
    )

    dialog.show()
}
```

### `MaterialTimePicker`

```kotlin
private fun showMaterialTimePicker() {
    val picker = MaterialTimePicker.Builder()
        .setTitleText("Chọn giờ hẹn")
        .setHour(12)
        .setMinute(0)
        .setTimeFormat(TimeFormat.CLOCK_24H)    <!-- CLOCK_24H | CLOCK_12H -->
        .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)   <!-- CLOCK | KEYBOARD -->
        .build()

    picker.addOnPositiveButtonClickListener {
        val hour   = picker.hour
        val minute = picker.minute
        val time   = String.format("%02d:%02d", hour, minute)
        binding.tvSelectedTime.text = time
    }

    picker.show(supportFragmentManager, "TIME_PICKER")
}
```

---

## 4. `NumberPicker`

`NumberPicker` cho phép user **cuộn để chọn số** trong một khoảng xác định. <br/>
**Usecase**: _chọn số lượng_, _chọn tuổi_, _chọn tầng lầu_,....

```xml
<NumberPicker
    android:id="@+id/numberPicker"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content" />
```

Xử lý `NumberPicker` trong:

- Cấu hình `NumberPicker`:

  ```kotlin
  binding.numberPicker.apply {
      minValue = 1          // giá trị tối thiểu
      maxValue = 10         // giá trị tối đa
      value    = 1          // giá trị mặc định
      wrapSelectorWheel = true   // true: cuộn vòng (10→1→2...) | false: dừng ở đầu/cuối
  }

  // Hiển thị giá trị tùy chỉnh thay vì số
  binding.numberPicker.displayedValues = arrayOf(
      "XS", "S", "M", "L", "XL", "XXL"
  )
  binding.numberPicker.minValue = 0
  binding.numberPicker.maxValue = 5
  ```

- Event handler & đọc giá trị:

  ```kotlin
  // Lắng nghe thay đổi
  binding.numberPicker.setOnValueChangedListener { picker, oldVal, newVal ->
      binding.tvQuantity.text = "Số lượng: $newVal"
  }

  // Đọc giá trị
  val selected = binding.numberPicker.value
  ```

---

## 5. `CalendarView`

`CalendarView` hiển thị **lịch tháng đầy đủ** và **cho phép chọn ngày**. <br/>
Khác với `DatePicker` (compact hơn), **`CalendarView` chiếm nhiều không gian hơn** và phù hợp cho các **màn hình lịch chuyên biệt**.

> _**Thực tế**: **`CalendarView` rất hạn chế về khả năng tùy chỉnh**. Các app cần calendar phức tạp (đánh dấu ngày, event, range) thường dùng thư viện bên thứ ba như `Kizitonwose`/`Calendar`._

```xml
<CalendarView
    android:id="@+id/calendarView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:minDate="01/01/2024"
    android:maxDate="12/31/2025" />
```

```kotlin
// Lắng nghe chọn ngày
binding.calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
    // month là 0-based
    val selected = "$dayOfMonth/${month + 1}/$year"
    binding.tvSelectedDate.text = selected
}

// Set ngày được chọn (milliseconds)
val calendar = Calendar.getInstance().apply {
    set(2024, 5, 15)   // 15/6/2024
}
binding.calendarView.date = calendar.timeInMillis

// Đọc ngày đang chọn
val selectedDate = binding.calendarView.date  // Long (milliseconds)
```
