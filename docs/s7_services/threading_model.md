# Android **Threading Model**

Trong kỹ thuật phần mềm, **Thread Modeling** (Mô hình hóa Luồng thực thi) là một **kỹ thuật phân tích và thiết kế hệ thống ở mức độ thấp** (_`Low-level` System Design_). Mục tiêu của nó là:

- Lập bản đồ trạng thái, định lượng tài nguyên và kiểm soát cách các luồng xử lý (Threads)
- Tương tác với bộ nhớ và phần cứng

nhằm **đảm bảo tính toàn vẹn dữ liệu, hiệu năng tối ưu và tránh các lỗi bất đồng bộ** kinh điển.

---

## 1. **Android _Threading Model_**

**Android Threading Model** là quy định cốt lõi về **cách hệ thống quản lý, phân phối và thực thi** các luồng xử lý bên trong một tiến trình ứng dụng.

Mô hình này được xây dựng dựa trên kiến trúc **`Single-Thread` Model** (Mô hình đơn luồng) kết hợp với cơ chế **`Event-Driven`** (Dựa trên sự kiện) nhằm đảm bảo tính toàn vẹn của giao diện người dùng (UI) và hiệu năng của hệ thống.

Cụ thể, khi Android khởi chạy app, hệ thống tạo ra một **Linux process** và bên trong đó có **một thread duy nhất**:

```
App Process (Linux Process)
└── Main Thread
      ├── UI Rendering
      ├── Event handling (click, touch, scroll)
      └── Lifecycle callbacks (onCreate, onResume, ...)
```

Do là mô hình **Single Thread**, có một số quy tắc **BẮT BUỘC TUÂN THỦ**:

- **Do not block the UI thread**: Tất cả các **tác vụ tốn thời gian** (I/O, Network, Database, Heavy Computation) **không được phép chạy trên UI Thread**.

  > _Nếu luồng này bị **block quá `5s`**, hệ thống sẽ kích hoạt hộp thoại **ANR** (Application Not Responding)_.

  **Background Thread** là giải pháp cho việc xử lý các **tác vụ nặng** bên ngoài Main thread.

- **Do not access the Android UI toolkit from outside the UI thread** (_Không truy cập các thành phần UI từ luồng khác_): Tất cả **các thao tác thay đổi giao diện** (đổi chữ TextView, hiển thị Button, vẽ lại Custom View) bắt buộc **phải thực thi trên UI Thread**.
  > _Android UI toolkit không phải là thread-safe_.

Quy tắc gia tiếp giữa **Background Thread** và **UI Thread**:

```kotlin
// ❌ Sai:
Thread {                    // background task
    val data = fetchApi()
    tvResult.text = data    // Crash: "Only Main Thread can touch UI"
}.start()

// ✅ Đúng:
Thread {                    // background task
    val data = fetchApi()
    runOnUiThread {         // Gửi task về Main Thread
        tvResult.text = data
    }
}.start()
```

> _Để **switch thread**, có thể dùng `Handler`, `runOnUiThread` hoặc `withContext(Dispatchers.Main)` để đưa **result** về **Main thread**._

---

## 2. **Looper** & **MessageQueue** — Cơ chế bên trong Main Thread

Main Thread không chỉ đơn giản là một thread chạy tuần tự rồi thoát, nó hoạt động theo mô hình **event loop**:

```
Main Thread
└── Looper
      └── MessageQueue
            ├── Message: "User click button"
            ├── Message: "Draw frame"
            ├── Message: "onCreate() của Activity"
            └── Message: "Network callback trả về"
```

**Main Thread** vận hành dựa trên 3 thành phần kỹ thuật:

- **`Looper`** là một vòng lặp vô hạn (_infinite loop_) liên tục **lấy ra các Message từ `MessageQueue`** và thực thi chúng (_dispatch tới `Handler`_) theo thứ tự.
  ```kotlin
  // Looper example
  while (true) {
      val msg = MessageQueue.next()   // Chờ nếu queue rỗng
      msg.target.dispatchMessage(msg) // Xử lý message
  }
  ```
- **`MessageQueue`** là một hàng đợi (_queue_) chứa các **`Message`**/**`Runnable`** được gửi từ các luồng khác hoặc từ chính Main Thread. Mỗi Message đại diện cho một **tác vụ cần thực thi**.
- **`Handler`** là thành phần đóng vai trò là **cầu nối**, chịu trách nhiệm **gửi là xử lý message**. Nó cho phép các luồng khác gửi message thông qua `sendMessage()` hoặc `post(Runnable)` vào **`MessageQueue`** của Main Thread từ background thread hoặc bất kỳ thread nào.

  ```kotlin
  Handler(Looper.getMainLooper()).post { }
  ```

---

## 3. Các cơ chế tạo **Background Thread**

### 3.1. `Thread` thuần - low level

```kotlin
Thread {
    val data = fetchFromNetwork()
    Handler(Looper.getMainLooper()).post {
        tvResult.text = data
    }
}.start()
```

Đơn giản nhưng **không có lifecycle management** — thread không tự dừng khi Activity destroy, **dễ gây memory leak**.

### 3.2. **Legacy ways** - hiện tại đã bị deprecated

- `AsyncTask` (_deprecated từ **Android 11 (API 30)**_): Cho phép **thực thi tác vụ ngầm** và **tự động gửi kết quả** về UI Thread thông qua các callback `onPostExecute()`. <br/>
  Nhược điểm của nó là đễ gây **Memory Leak** nặng khi `Activity` bị hủy, **khó quản lý lifecycle**.
- `IntentService`: Một dạng **`Service` tự động tạo một Worker Thread riêng** biệt để xử lý các việc tuần tự và tự tắt.
  > _Đã bị khai tử từ Android 11 do không tối ưu bằng các giải pháp hiện đại._

### 3.2. **Modern Android _Concurrency_**

#### 3.2.1. `HandlerThread` - quản lý **thread riêng biệt**

`HandlerThread` là một lớp con của `Thread` **tích hợp sẵn một Looper bên trong**. <br/>
Thường dùng để **xây dựng các kiến trúc xử lý tác vụ tuần tự** chạy ngầm liên tục mà không cần tạo luồng mới liên tục, giảm thiểu chi phí Context Switching.

#### 3.2.2. `ThreadPoolExecutor` - quản lý **pool thread**

Tạo Thread mới cho mỗi tác vụ rất **tốn kém**. `ThreadPoolExecutor` duy trì sẵn một pool các thread, tái sử dụng chúng giúp **tối ưu hiệu năng, giảm chi phí tạo thread** mới:

```kotlin
val executor = Executors.newFixedThreadPool(4) // Pool 4 thread

executor.execute {
    val data = fetchFromNetwork()
    Handler(Looper.getMainLooper()).post {
        tvResult.text = data
    }
}
```

`ThreadPoolExecutor` có thể cấu hình:

- `corePoolSize` - số thread tối thiểu **luôn tồn tại**
- `maximumPoolSize` - số thread tối đa **có thể tạo ra**
- `keepAliveTime` - thời gian **thread dư thừa** (thời gian rảnh tối đa) được giữ sống trước khi bị hủy

```
ThreadPool (4 threads):
├── Thread-1: đang xử lý task A
├── Thread-2: đang xử lý task B
├── Thread-3: rảnh — chờ task mới
└── Thread-4: rảnh — chờ task mới

Task mới đến → giao cho Thread-3, không tạo thread mới
```

### 3.2.3. Kotlin `Coroutines` (**recommended**) - quản lý **asynchronous task** hiện đại

`Coroutine` không phải thread — nó là **lightweight concurrency primitive** chạy trên thread pool có sẵn. **Một thread có thể chạy hàng nghìn coroutine**.

> _Android hiện tại **ƯU TIÊN TỐI ĐA `Coroutines`** cho việc quản lý **Concurrency**._

`Coroutine` vận hành dựa trên cơ chế **Suspending Function - `suspend fun`** thay vì blocking, được quản lý bởi các `Dispatchers`:

- `Dispatchers.Main` - thực thi trực tiếp trên **UI Thread**
- `Dispatchers.IO` - thực thi trên **thread pool** tối ưu cho `I/O tasks`, có thể scale lên đến **64 threads**.
- `Dispatchers.Default` - thực thi trên **thread pool** tối ưu cho **CPU-intensive / CPU-bound tasks**, giới hạn số luồng bằng số core của chip (có thể scale lên đến **n threads**, với `n = số lõi CPU`).
- `Dispatchers.Unconfined` - thực thi ngay trên **thread hiện tại**, chuyển thread ở suspension point đầu tiên, không giới hạn, nhưng **không an toàn** cho các tác vụ liên quan đến UI.

```kotlin
// Trong ViewModel
viewModelScope.launch {
    val data = withContext(Dispatchers.IO) {
        fetchFromNetwork()      // Chạy trên IO thread pool
    }
    tvResult.text = data        // Tự động về Main Thread
}
```

| `Dispatcher`             | Thread Pool              | Used for                                                   |
| ------------------------ | ------------------------ | ---------------------------------------------------------- |
| `Dispatchers.Main`       | `1`-Main Thread          | UI tasks, lifecycle callbacks, observer LiveData, ...      |
| `Dispatchers.IO`         | `64` threads             | I/O tasks, network, database, file read/write              |
| `Dispatchers.Default`    | `CPU-core count` threads | CPU-intensive tasks, heavy computation                     |
| `Dispatchers.Unconfined` | **Không cố định**        | **NOT recommended**, used for _testing_ or _special cases_ |
