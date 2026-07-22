# `Context` of Application

## 1. **What** is the **Context**?

### 1.1. **Định nghĩa `Context`**

`Context` là một **`abstract class`** trong Android — đại diện cho **môi trường** mà code đang chạy trong đó (**execution environment**). <br>
Nó là **cầu nối giữa code** của bạn **và Android OS**, hầu hết mọi **thao tác với Android system** đều cần Context:

- Start **Activity**, **Service**, ... send **Broadcast**
- **Resources** accessing (string, color, drawable, ...)
- Access to `SharedPreferences`, `DataStore`, File System, ...
- Lấy **System Service** (`NotificationManager`, `AlarmManager`, ...)
- Inflate layout, tạo View, ...

### 1.2. **`Context` hierarchy**

Về mặt **kĩ thuật**, `Context` không phải là một object đơn giản — nó là **`abstract class` có nhiều implementation khác nhau** trong Android:

![Context hierarchy in Android](/res/s1/img/android_context_hierarchy.png)

trong đó:

- `Context` là `abstract class` chịu trách nhiệm **định nghĩa các API**
  - `ContextWrapper` là class **bọc một `Context` thật sự** (`ContextImpl`)
    - `Application`, `Service` hoặc `Activity` (_qua `ContextThemeWrapper`_)
      đều là **con của `Context`**

-> điều này có nghĩa: `Application`, `Service` hay `Activity` cũng **CHÍNH LÀ `CONTEXT`** (quan hệ **IS-A** `Context`, _nhờ vậy, có thể pass `this` (`Acitivity`) ở bất cứ nơi nào cần `Context`_)

---

## 2. `ApplicationContext` & `ActivityContext`

### 2.1. `ApplicationContext` & `ActivityContext`

Tuy đều **kế thừa từ `Context`**, nhưng vòng đời (**Lifecycle**) và **mục đích sử dụng** của chúng lại hoàn toàn khác nhau.

| Tiêu chí             | `ApplicationContext`                                                             | `ActivityContext`                                                                                                               |
| -------------------- | -------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| **Lifecycle**        | Toàn app, sống từ khi **app khởi chạy** cho tới khi **app kill hoàn toàn**       | Cùng với `Acitivity` đó (đang **mở**), ngắn                                                                                     |
| **Theme/UI**         | **KHÔNG có Theme UI** (dùng Theme mặc định của hệ thống)                         | **CÓ Theme UI**, chứa thông tin về font, style, ...                                                                             |
| **Mục đích sử dụng** | Dùng cho các **Singleton**, **Database**, **Repository**, SDK khởi tạo ngầm, ... | Dùng cho `View`, inflate layout, `Toast`, mở Activity mới, ...                                                                  |
| **Cách lấy**         | `applicationContext` / `getApplicationContext()`                                 | Trong **Activity**, sử dụng `this`, hoặc trong **Fragment** sử dụng `requireContext()` để lấy context của **Activity hiện tại** |

### 2.2. Vấn đề **Memory Leak**

**Memory Leak** xảy ra khi **object đáng lẽ PHẢI BỊ `GC` huỷ** nhưng **`GC` không thực hiện được do vẫn bị THAM CHIẾU ở nơi khác**.

Lỗi phổ biến nhất **gây memory leak** của dev Android là **truyền `ActivityContext`** vào một Class dạng **Singleton** hoặc **Static variable**.

```kotlin
// SAI — Singleton giữ Activity Context
object DatabaseManager {
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context   // nếu truyền Activity → leak
    }
}

// Gọi từ Activity:
DatabaseManager.init(this)   // ← this = Activity Context → LEAK
```

> _Dẫn tới `Activity` KHÔNG bao giờ bị huỷ dù đáng lẽ nó nên bị huỷ do không còn sử dụng_

Cách đúng đắn là sử dụng **`ApplicationContext`**:

```kotlin
// ĐÚNG — Singleton giữ Application Context
object DatabaseManager {
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext   // ← convert sang Application Context
    }
}
```

### 2.3. **Lấy `Context`**

#### 2.3.1. Trong `Activty`

Sử dụng:

- `this` cho `ActivityContext`
- `applicationContext` hoặc `this.applicationContext` cho `ApplicationContext`

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Activity Context — this = MainActivity instance
        val activityContext: Context = this

        // Application Context
        val appContext: Context = applicationContext
        // hoặc:
        val appContext2: Context = this.applicationContext

        // Base Context (ít dùng)
        val baseCtx: Context = baseContext
    }
}
```

#### 2.3.2. Trong `Fragment`

Sử dụng:

- `requireContext()` cho `ActivityContext`
- `requireActivity()` để lấy tham chiếu tới `Activity`
- `requireContext().applicationContext` cho `ApplicationContext`

```kotlin
class HomeFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Activity Context (Fragment không phải Context)
        val activityContext: Context = requireContext()
        // hoặc:
        val activityContext2: Context? = context   // nullable version

        // Activity reference
        val activity: FragmentActivity = requireActivity()

        // Application Context
        val appContext: Context = requireContext().applicationContext
    }
}
```

#### 2.3.3. Trong `Service`: tương tự `Activity` (_vì cùng là context_)

```kotlin
class MyService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Service IS-A Context → dùng this
        val serviceContext: Context = this

        // Application Context
        val appContext: Context = applicationContext

        return START_STICKY
    }
}
```

### 2.4. Một số pattern gây **memory leak** với `Context`

```kotlin
// LEAK PATTERN 1: Static reference đến Activity Context
object SomeManager {
    var context: Context? = null   // static → tồn tại mãi
}
SomeManager.context = this   // this = Activity → LEAK



// LEAK PATTERN 2: Inner class giữ implicit reference
class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())

    inner class MyRunnable : Runnable {
        // inner class giữ implicit reference đến MainActivity
        override fun run() {
            updateUI()
        }
    }

    // Nếu post delay dài + Activity destroy → LEAK
    handler.postDelayed(MyRunnable(), 60000)
}



// LEAK PATTERN 3: Callback không được unregister
class MainActivity : AppCompatActivity() {
    override fun onCreate(...) {
        SomeLibrary.registerCallback { data ->
            // Lambda giữ reference đến MainActivity (qua binding)
            binding.tvData.text = data
        }
        // Không unregister → library giữ reference → LEAK
    }
}
```

Fix:

```kotlin
// Fix 1: Dùng applicationContext cho static/singleton
object SomeManager {
    var context: Context? = null
}
SomeManager.context = applicationContext   // ✅ Application Context



// Fix 2: WeakReference cho long-lived callback
class MainActivity : AppCompatActivity() {
    private val weakActivity = WeakReference(this)

    private val myRunnable = Runnable {
        weakActivity.get()?.updateUI()   // null nếu Activity đã destroy
    }
}



// Fix 3: Unregister trong onDestroy/onStop
override fun onDestroy() {
    super.onDestroy()
    SomeLibrary.unregisterCallback()
}
```