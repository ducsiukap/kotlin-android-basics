# **Activity instance**: Configuration change & `savedInstanceState`

## 1. Configuration changes

### 1.1. Activity instance

Khi Android tạo **Activity** (screen), nó tạo ra **một object** trong RAM, ex: `MainActivity instance #1`

Nếu xoay màn hình, Android sẽ **destroy** và tạo ra **một instance mới**:

```
MainActivity instance #1
    ↓ bị destroy
MainActivity instance #2
    ↓ được create
```

Dù đều là `MainActivity`, nhưng chúng là **2 object khác nhau** trong RAM.

### 1.2. **Configuration changes**

**Configuration Change** là sự kiện **thay đổi cấu hình thiết bị** — phổ biến nhất là **xoay màn hình**. Khi xảy ra, Android hủy và tạo lại Activity hoàn toàn:

```
Xoay màn hình:
onPause() → onStop() → onDestroy() → [Activity mới] → onCreate() → onStart() → onResume()
```

> _**Tại sao Android làm vậy thay vì chỉ thay đổi layout?** Vì nhiều **resource phụ thuộc vào configuration**: layout (`layout-land/`), string (`values-vi/`), drawable (`drawable-hdpi/`)... **Recreate Activity là cách đơn giản nhất để load đúng resource mới**._

Hệ quả: **Mọi data lưu trong field của Activity bị `mất` khi rotate**.

```kotlin
class CounterActivity : AppCompatActivity() {
    private var count = 0   // BỊ MẤT khi xoay màn hình

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // count luôn = 0 sau khi rotate
    }
}
```

**Note**:

- Chỉ các **`state` của activity** mới bị mất sau destroy.
- Các **state quan trọng riêng** của View như `text` đang nhập trong `EditText`, `CheckBox`, ... được Android **tự lưu** và **tự restore** trong trường hợp **Configuration change**
  > Yêu cầu: View có `android:id` và `:saveEnable=true` (_default_). <br/>Có thể tắt tính năng **auto-save + auto-restore** bằng cách set `saveEnable=False`

```
State nằm trong View
→ Android có thể tự save một phần

State nằm trong Activity
→ mất khi Activity instance bị destroy
```

---

## 2. **Giải pháp: `savedInstanceState`**

**Android** cung cấp cơ chế `savedInstanceState` để **lưu `trạng thái UI` trước khi Activity bị hủy** và **khôi phục sau khi recreate**.

- **Trước khi hủy**: `onSaveInstanceState(outState: Bundle)` được gọi, bạn lưu data vào `outState`:
- **Sau khi recreate**: `onCreate(savedInstanceState: Bundle?)` được gọi, bạn lấy data từ `savedInstanceState`

> _`savedInstanceState` là một `Bundle`, có thể hiểu `Bundle` như một chiếc **túi nhỏ chứa dữ liệu** theo cặp: `key -> value`_

#### **Cách lưu và truy vấn dữ liệu từ `savedInstanceState`**

- **Bước 1**: tạo `key`, tránh dùng trực tiếp **`"key"`**

  ```kotlin
  companion object {
      private const val TAG = "ActivityLifecycle"
      private const val KEY_COUNT = "count"
  }
  ```

- **Bước 2**: override `onSaveInstanceState` và lưu data vào `outState`

  ```kotlin
  override fun onSaveInstanceState(outState: Bundle) {
      outState.putInt(KEY_COUNT, count) // đưa count vào outState
                                        // với key là KEY_COUNT ("count")
      super.onSaveInstanceState(outState)
  }
  ```

- **Bước 3**: lấy data từ `savedInstanceState` trong `onCreate`

  ```kotlin
  override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.activity_counter)

      // Lấy count từ savedInstanceState nếu có
      // nếu không thì mặc định là 0
      count = savedInstanceState?.getInt(KEY_COUNT) ?: 0 // null-safety
  }
  ```

**Theo tài liệu khuyến nghị:**

- Dùng `savedInstanceState` để lưu **trạng thái UI tạm thời**, nhẹ và gắn với **input** hoặc **navigation** như _vị trí scroll_, _text đã nhập_, _id item đang xem_, _lựa chọn đang thao tác_, ...
  > _Phù hợp với: `String`, `Int`, `Boolean`, `Float`, Parcelable object nhỏ, UI states, ..._
- Với **dữ liệu lớn / quan trọng / `business logic`**, nên lưu vào **`ViewModel`** và **`SavedStateHandle`**
