# `DatePickerDialog` / `TimePickerDialog`

## 1. `DatePickerDialog`

### 1.1. `DatePickerDialog` basic

Android **cung cấp sẵn** `DatePickerDialog` để hiển thị **UI chọn ngày/tháng/năm**.

```kotlin
class BirthdayPickerDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val calendar = Calendar.getInstance()

        return DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                // Callback — gọi khi user bấm "OK"
                // month là 0-based (0 = Tháng 1, 11 = Tháng 12)
                setFragmentResult(
                    "date_picked",
                    bundleOf("year" to year, "month" to month, "day" to dayOfMonth)
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
}
```

Constructor của `DatePickerDialog(context, listener, year, month, day)` nhận vào:

- `context`: `Context` để hiển thị dialog.
- `listener`: **callback được gọi khi user bấm "OK"**, <br/>
  Listener nhận vào **4 tham số**: `context`, `year`, `month`, `dayOfMonth`, trong đó:
  - `month` là **0-based** (0 = Jan, 11 = Dec)
  - `dayOfMonth` là **1-based** (1 = 1st, 31 = 31st)
- `year`, `month`, `day`: **giá trị mặc định** hiển thị (chọn) sẵn khi mở dialog.

### 1.2. Limit **min/max date**

Có thể sử set `.minDate` và `.maxDate` cho `DatePickerDialog` để **giới hạn ngày CÓ THỂ CHỌN**.

```kotlin
override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = DatePickerDialog(requireContext(), listener, year, month, day)

    // Không cho chọn ngày trong TƯƠNG LAI (vd: ngày sinh)
    dialog.datePicker.maxDate = System.currentTimeMillis()

    return dialog
}
```

---

## 2. `TimePickerDialog`

Tương tự `DatePickerDialog`, Android cũng cung cấp sẵn `TimePickerDialog` để hiển thị **UI chọn giờ/phút**.

```kotlin
class TimePickerDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val calendar = Calendar.getInstance()

        return TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                setFragmentResult(
                    "time_picked",
                    bundleOf("hour" to hourOfDay, "minute" to minute)
                )
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true  // true = định dạng 24h, false = 12h (AM/PM)
        )
    }
}
```

Constructor của `TimePickerDialog(context, listener, hour, minute, is24HourView)` nhận vào:

- `context`, `listener` tương tự như `DatePickerDialog`.<br/>
  Trong đó, **listener** nhận vào **3 tham số**: `context`, `hourOfDay`, `minute`.
- `hour`, `minute`: **giá trị mặc định** hiển thị (chọn) sẵn khi mở dialog.
- `is24HourView`: `Boolean` là chế **độ hiển thị giờ** — _`true` = định dạng 24h, `false` = 12h (AM/PM)._

Nên dùng `DateFormat.is24HourFormat(context)` để check **chế độ hiển thị giờ của user**, và set `is24HourView` tương ứng.

```kotlin
// get system setting của user để set is24HourView
val is24h = DateFormat.is24HourFormat(requireContext())
TimePickerDialog(requireContext(), listener, hour, minute, is24h)
```

---

## 3. **Customize** `DatePickerDialog` / `TimePickerDialog`

### 3.1. **Customize theme**

`DatePickerDialog` là 1 subclass của `AlertDialog`, nên có thể customize layout, theme, title, button text, ...

```kotlin
class BirthdayPickerDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val calendar = Calendar.getInstance()

        // Constructor CÓ overload nhận thêm themeResId
        return DatePickerDialog(
            requireContext(),                       // context

            R.style.CustomDatePickerTheme,          // ← Custom theme

            { _, year, month, day -> /* ... */ },   // listener
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
}
```

example theme:

```xml
<style name="CustomDatePickerTheme" parent="ThemeOverlay.MaterialComponents.Dialog.Alert">
    <item name="colorAccent">@color/purple_500</item>       <!-- Màu ngày được chọn -->
    <item name="android:textColorPrimary">@color/black</item>
    <item name="buttonBarPositiveButtonStyle">@style/DatePickerButtonStyle</item>
</style>

<style name="DatePickerButtonStyle" parent="Widget.MaterialComponents.Button.TextButton">
    <item name="android:textColor">@color/purple_500</item>
</style>
```

Có thể **customize** được:

- Màu **accent** (selected date, highlight, ...)
- Màu text, button, title, ...
- General style, ...

### 3.2. Có thể **custom layout hoàn toàn** với việc dùng `DatePicker` / `TimePicker` trực tiếp:

Nếu **cần custom sâu hơn theme** cho phép (ví dụ: DatePicker + ghi chú thêm, hoặc bố trí lại), dùng View `DatePicker`/`TimePicker` và **nhúng vào AlertDialog** thông qua `setView()`.

```xml
<!-- dialog_custom_date_picker.xml -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:gravity="center">

    <TextView
        android:text="Chọn ngày sinh của bạn"
        android:textStyle="bold"
        android:layout_marginBottom="8dp" />

    <!-- DatePicker LÀ 1 View bình thường — nhúng được vào bất kỳ layout nào -->
    <DatePicker
        android:id="@+id/datePicker"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:datePickerMode="spinner" />

</LinearLayout>
```

`android:datePickerMode="spinner"` có **2 chế độ hiển thị**:

- `spinner` (dạng cuộn)
- `calendar` (dạng lịch).

Nhúng view vào `AlertDialog` qua `setView()`:

```kotlin
class CustomDatePickerDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogCustomDatePickerBinding.inflate(requireActivity().layoutInflater)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)   // Nhúng DatePicker view + TextView tùy ý
            .setPositiveButton("Chọn") { _, _ ->
                val picker = binding.datePicker
                setFragmentResult(
                    "date_picked",
                    bundleOf(
                        "year" to picker.year,
                        "month" to picker.month,
                        "day" to picker.dayOfMonth
                    )
                )
            }
            .setNegativeButton("Hủy", null)
            .create()
    }
}
```

---

## 4. Gộp **`DatePickerDialog` + `TimePickerDialog`** vào 1 dialog

Có thể tạo `DateTimePickerHelper` để **nối tiếp 2 dialog**, hiện DatePicker trước, sau đó hiện TimePicker, và **trả về 1 `Calendar` đầy đủ cả ngày lẫn giờ**.

```kotlin
class DateTimePickerHelper(private val fragment: Fragment) {

    fun showDateTimePicker(onResult: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance()

        // Bước 1 — hiện DatePicker TRƯỚC
        DatePickerDialog(
            fragment.requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)

                // Bước 2 — SAU KHI chọn ngày xong, hiện TIẾP TimePicker
                TimePickerDialog(
                    fragment.requireContext(),
                    { _, hour, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE, minute)
                        onResult(calendar)   // Trả về Calendar ĐẦY ĐỦ cả
                                              // ngày lẫn giờ
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    DateFormat.is24HourFormat(fragment.requireContext())
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
```
