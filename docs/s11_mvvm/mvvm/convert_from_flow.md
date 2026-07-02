# `stateIn()`, `shareIn()` & **Lifecycle-aware Collection**

## 1. Convert `Flow` into `StateFlow`: `stateIn()`

Detail: [stateIn()](./stateflow.md#4-convert-flow-into-stateflow-statein)

```kotlin
fun <T> Flow<T>.stateIn(
    scope: CoroutineScope,        // StateFlow sống trong scope nào
    started: SharingStarted,      // Khi nào upstream Flow chạy/dừng
    initialValue: T               // Giá trị trước khi Flow emit lần đầu
): StateFlow<T>
```

Convert:

```kotlin
val users: StateFlow<List<User>> = repository.allUsers
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
```

---

## 2. Convert `Flow` into `SharedFlow`: `shareIn()`

Tương tự `stateIn()`, nhưng `shareIn()` dùng cho `SharedFlow`, và **không có initialValue**:

```kotlin
fun <T> Flow<T>.shareIn(
    scope: CoroutineScope,
    started: SharingStarted,
    replay: Int = 0               // Khác stateIn() — KHÔNG có initialValue,
                                   // thay vào đó là replay
): SharedFlow<T>
```

Example — **chia sẻ 1 upstream Flow** cho nhiều collector

```kotlin
class StockPriceViewModel(private val repository: StockRepository) : ViewModel() {

    // Giả sử repository.observeStockPrice() là Flow đắt đỏ
    // (mở WebSocket connection, hoặc polling liên tục)
    val stockPrice: SharedFlow<Double> = repository.observeStockPrice("AAPL")
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 1   // Collector mới vẫn muốn thấy giá hiện tại ngay
        )
}
```

### **`stateIn()` vs `shareIn()`**: usecases

- `stateIn()` → Dùng khi cần "**current state**" rõ ràng — _UI luôn cần 1 giá trị để render (danh sách, loading status...)_
- `shareIn()` → Dùng khi chỉ cần **CHIA SẺ 1 upstream Flow tốn kém - `heavy upstream Flow` - cho nhiều subscriber**, nhưng bản chất dữ liệu là **"event stream"** hoặc không có khái niệm "giá trị mặc định" rõ ràng

Trong **MVVM thực tế**:

- `stateIn()` → dùng cho **State** (_đa số trường hợp_)
- `shareIn()` → ít dùng hơn, chủ yếu cho **stream data realtime tốn kém** (_`WebSocket`, sensor, ..._)

---

## 3. `SharingStarted` - Khi nào **upstream Flow chạy/dừng**: [Details](./stateflow.md#4-convert-flow-into-stateflow-statein)

### 3.1. `SharingStarted.WhileSubscribed(stopTimeoutMillis: Long = 0L)`

```kotlin
.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
)
```

Cơ chế:

- **Upstream Flow** chỉ chạy khi có **ít nhất 1 collector**.
- Khi **không còn collector**, sẽ **dừng upstream Flow** sau `stopTimeoutMillis`

> _**Tại sao chọn `5000ms` cụ thể**? Đây là con số Google khuyến nghị — **đủ dài để cover thời gian configuration change** (~vài trăm ms đến 1-2s), **nhưng đủ ngắn để không giữ tài nguyên** (network, DB connection) quá lâu khi user thực sự đã rời màn hình_.

### 3.2. `SharingStarter.Eagerly`

**Upstream Flow** chạy **NGAY KHI** `stateIn()`/`shareIn()` được gọi, **bất kể có collector hay không**.

- **KHÔNG đợi collector**
- **KHÔNG bao giờ** cancel, trừ khi `viewModelScope` bị cancel

```kotlin
.stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = emptyList()
)
```

Dùng khi: **Data cần fetch ngay từ đầu**, dù UI có hiển thị hay chưa

> _vd: Badge notification count cần update ngầm dù user đang ở màn hình khác trong app_

### 3.3. `SharingStarted.Lazily`

Tương tự `WhileSubscribed()`, **upstream Flow** chỉ chạy khi có **ít nhất 1 collector**, nhưng **KHÔNG có cơ chế timeout dừng - `stopTimeoutMillis`**:

```kotlin
.stateIn(
    scope = viewModelScope,
    started = SharingStarted.Lazily,
    initialValue = emptyList()
)
```

Dùng khi: **Một lần fetch là đủ**, muốn giữ kết quả luôn, nhưng **không cần fetch ngay từ lúc `ViewModel` tạo**
