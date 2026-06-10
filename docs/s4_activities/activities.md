# **Activity**

## 1. **Activity** in Android

`Activity` là **một trong 4 Android Component**. Cụ thể hơn, `Activity` là component đại diện cho **một màn hình duy nhất có giao diện người dùng** — nơi user nhìn thấy và tương tác.

Về mặt kỹ thuật, `Activity` là một class **kế thừa** từ `AppCompatActivity` (hoặc `ComponentActivity`), được **Android OS quản lý vòng đời**, và phải khai báo trong `AndroidManifest.xml`.

Example:

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
```

Giải thích:

- **`MainActivity` kế thừa `AppCompatActivity`**. Nhờ đó, class này có **hành vi của một Activity** và sử dụng được các **tiện ích** tương thích từ **AndroidX**
- `override fun onCreate(...)` là **phương thức khởi tạo** của Activity, nơi bạn thiết lập giao diện và logic ban đầu. Android sẽ **gọi hàm này khi tạo màn hình**.
- `super.onCreate(savedInstanceState)`: gọi hàm của lớp cha để đảm bảo hoạt động bình thường của Activity.
  > _Khi **override lifecycle callback**, bạn gần như luôn cần gọi phiên bản tương ứng của `super`._

### **Activity** trong Android system

**Activity KHÔNG tự khởi tạo**:

- **Android OS** là người quyết định **khi nào tạo**, **khi nào hủy** Activity.
- Bạn **KHÔNG gọi `MainActivity()`** — bạn **khai báo** Activity trong `Manifest` và **Android OS gọi nó khi cần**.

```
User tap icon app
        ↓
Android OS đọc AndroidManifest.xml
        ↓
Tìm Activity có MAIN + LAUNCHER intent-filter
        ↓
Android OS tạo instance của Activity đó
        ↓
Gọi onCreate() → bạn setup UI ở đây
        ↓
Màn hình hiển thị cho user
```

### Mô hình `1 Activity = 1 screen`

- Mô hình **đơn giản**: `1 screen = 1 Activity`

  ```
  App thương mại điện tử:
  ├── SplashActivity       → màn hình loading
  ├── LoginActivity        → đăng nhập
  ├── MainActivity         → trang chủ (thường chứa Fragment)
  ├── ProductDetailActivity → chi tiết sản phẩm
  ├── CartActivity         → giỏ hàng
  ├── CheckoutActivity     → thanh toán
  └── OrderSuccessActivity → đặt hàng thành công
  ```

- Ứng dụng **hiện đại**: 1 Activity có thể có nhiều Fragment hoặc nhiều màn hình Compose.
  > _Tuy vậy, `Activity` vẫn là thành phần cốt lõi **kết nối giao diện** của app với **hệ điều hành**._

