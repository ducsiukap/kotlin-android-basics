# **`UDF` - Undirectional Data Flow**

## 1. **UDF** - Undirectional Data Flow

**UDF** là khái niệm: **`data` chỉ chạy 1 chiều**.

> _`UDF` là **nguyên tắc trung tâm** của `MVVM` hiện đại_

```text
Events (user actions)
              │
              │ User tap, input, scroll...
              ▼
        ┌──────────┐
        │ViewModel │
        │          │
        │ process  │
        │ + update │
        │  state   │
        └──────────┘
              │
              │ State (UI state)
              ▼
        ┌──────────┐
        │   View   │
        │          │
        │ renders  │
        │  state   │
        └──────────┘
```

Nguyên tắc **undirectional**:

- `DATA`: chỉ đi từ `ViewModel` → `View`
- `EVENTS`: chỉ đi từ `View` → `ViewModel`

## 2. **Why** UDF is **important**?

### **Không** theo UDF:

Khi **data** đi theo **2 chiều** (View ↔ ViewModel), sẽ dẫn đến tình trạng khó kiểm soát **(difficult to tracking)**:

```kotlin
// KHÔNG theo UDF — data đi 2 chiều, khó track
class BadFragment : Fragment() {
    fun onButtonClick() {
        val name = binding.etName.text.toString()
        viewModel.name = name              // Fragment SET data vào ViewModel
        val result = viewModel.process()   // Fragment GỌI trực tiếp
        binding.tvResult.text = result     // Fragment nhận kết quả trực tiếp
    }
}
```

### **Theo** UDF:

Với **UDF**, **data** chỉ đi theo **1 chiều** (ViewModel → View), giúp dễ dàng kiểm soát **state** của UI:

```kotlin
// THEO UDF — data chỉ đi 1 chiều
class GoodFragment : Fragment() {
    override fun onViewCreated(...) {
        // 1. View observe state từ ViewModel
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.tvResult.text = state.result   // View render state
        }

        // 2. View gửi event lên ViewModel
        binding.btnSubmit.setOnClickListener {
            viewModel.onSubmitClicked(binding.etName.text.toString())
            // Không quan tâm kết quả trực tiếp — chờ state update
        }
    }
}
```

### **Lợi ích** khi dùng `UDF`:

- **`State`** luôn nhất quán do được **quản lý tập trung** tại một nguồn duy nhất - **single source of truth** - `ViewModel`
- **Dễ `debug`**: biết chính xác **data tới từ đâu**.
- **Dễ `test`**: chỉ cần test `ViewModel`, không cần test `View` bởi **`ViewModel` không phụ thuộc vào `View`**.
- **Predictable**: `View` chỉ render theo `state`, không có logic xử lý, nên **dễ dự đoán** hành vi của UI.

```text
Events (up)              State (down)
View ────────────────→ ViewModel ────────────────→ View
     "user click Add"            "đây là list mới"

                 Repository
                     │
                  Database
                   / API
```
