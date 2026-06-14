# **`Fragment`**

## 1. **What** is a **`Fragment`**?

`Fragment` là một **modular UI component** — đại diện cho **một phần của giao diện** hoặc **một hành vi** (`behavior`) có thể **được đặt vào trong `Activity`**

> _`Fragment` là một **mảnh giao diện** có **lifecycle riêng**, sống bên trong `Activity`._

Trong đó, `Fragment` có:

- **Layout** riêng
- **Lifecycle** riêng, gắn với lifecycle của **`Activity` host**
- Có **logic xử lý riêng**, class riêng.
- Có thể **reuse** trong nhiều `Activity` khác nhau.

**`NOTE`**: **`Fragment` không thể tồn tại độc lập**, nó phải được host bới một `Activity` (_hoặc `Fragment` khác - nested Fragment_).

> _Khi **`Activity` host** bị hủy, tất cả `Fragment` bên trong cũng sẽ bị hủy theo._

---

## 2. **Why** do we need `Fragment`?

`Fragment` được giới thiệu từ **Android 3.0 (API 11)** để giải quyết bài toán **tablet** — _màn hình lớn cần hiển thị nhiều "panel" cùng lúc, mỗi panel có logic riêng_

```
Phone (1 Activity = 1 màn hình):
┌─────────────────┐
│   List Fragment │
└─────────────────┘
        ↓ tap item
┌─────────────────┐
│  Detail Fragment│
└─────────────────┘

Tablet (1 Activity = 2 Fragment cùng lúc):
┌────────────┬────────────────┐
│   List     │    Detail       │
│  Fragment  │    Fragment     │
│            │                 │
└────────────┴────────────────┘
```

> _Theo thời gian, **`Fragment` phát triển thành công cụ chính cho `MỌI` loại UI modular** — không chỉ tablet: Bottom Navigation, Tab, ViewPager, Navigation Component.._

`Fragment` sinh ra giúp **app** có thể:

- **chia nhỏ giao diện** thành nhiều phần, mỗi phần có lifecycle riêng, dễ quản lý hơn.
- **tái sử dụng UI**
- dễ quản lý các màn hình con
- phù hợp với **bottom navigation**, **tab**, **master-detail**.

Ví dụ: _`BottomNavigationView` có 3 tab, mỗi tab là một `Fragment` khác nhau, có layout và logic riêng, bao gồm: `HomeFragment`, `SearchFragment`, `ProfileFragment`_.

> _Nhờ có `Fragment`, thay vì phải tạo 3 `Activity` khác nhau cho 3 tab, ta chỉ **cần 1 `Activity` duy nhất**, bên trong chứa 3 `Fragment` khác nhau._

---

## 3. `Fragment` vs `Activity`

| Đặc điểm                             | `Activity`                       | `Fragment`                                                                             |
| ------------------------------------ | -------------------------------- | -------------------------------------------------------------------------------------- |
| Tồn tại **độc lập**                  | Có                               | Không, cần **`Activity` host**                                                         |
| Khai báo trong `AndroidManifest.xml` | Bắt buộc                         | Không cần                                                                              |
| **Lifecycle**                        | Có, `6 callback` chính           | Có, nhưng gắn với lifecycle của `Activity`, _nhiều callback hơn_.                      |
| **Layout**                           | `setContentView()`               | inflate trong `onCreateView()`                                                         |
| **Reuse**                            | Khó, `1 Activity = 1 screen`     | Dễ dàng, có thể dùng lại trong nhiều `Activity` khác nhau.                             |
| **Navigation**                       | **`Intent` + `startActivity()`** | Phức tạp hơn, cần **`FragmentManager` + Transaction** hoặc **`Navigation Component`**. |

---

## 4. **When** to use `Fragment`?

### 4.1. **Single Activity Architecture**

Pattern "**Single Activity Architecture**" — **quy tắc** phổ biến trong app hiện đại:

- **`1 Activity` duy nhất**: gọi là **Host Activity** hoặc **Single Activity**.
- **`Nhiều Fragment`**: đại diện cho từng màn hình logic.

Ví dụ: `MainActivity` chứa các **fragments**: `HomeFragment`, `SearchFragment`, `ProfileFragment`, `SettingsFragment`, ...

### 4.2. **When** use `Fragment`?

Dùng `Fragment` khi:

- Một `Activty` có **nhiều vùng UI**
- Đổi nội dung trong cùng 1 khung
- Dùng **bottom navigation**, **tab**, **viewpager**, làm **master-detail**
- Muốn **tái sử dụng** UI trong nhiều `Activity` khác nhau.

**`Lý do`** áp dụng **Single Activity Architecture** pattern:

- `(1)` **Navigation** giữa các `Fragment` **nhẹ hơn** so với giữa các `Activity` (_**không** tạo `Activity` mới, **không** qua `Intent`_).
- `(2)` **Chia sẻ data** giữa các `Fragment` dễ dàng hơn thông qua nhờ cùng **`ViewModel` scope** theo `Activity`.
- `(3)` `Animation` chuyển đổi giữa các `Fragment` mượt mà hơn, dễ custom.
- `(4)` Phù hợp với **Navigation Component** — công cụ điều hướng hiện đại của Android, hoạt động tốt nhất với **Single Activity**.

Khi nào vẫn dùng **`Activity` riêng**?

- Cần **screen hoàn toàn độc lập** về luồng, ex: _Login có flow riêng biệt so với phần còn lại của app_.
- Cần **launch từ bên ngoài app**.
- **Splash screen**

### 4.3. `Fragment` in **modern Android Apps**

- `BottomNavigationView`: **Mỗi tab = 1 Fragment**, swap qua lại trong cùng container
- `ViewPager2` + `TabLayout`: **Mỗi tab/page = 1 Fragment**
- `Navigation Drawer`: **Mỗi menu item = 1 Fragment**
- `Master-Detail` (tablet): **List Fragment + Detail Fragment hiển thị song song**
- `Dialog`: **DialogFragment** — Fragment hiển thị dạng popup
- ...

---

## 5. `Fragment` **KHÔNG** phải là `mini-Activity` theo nghĩa **inheritance**

> _`Fragment` **KHÔNG phải** là `Activity` nhỏ_

`Fragment` = **UI component** có lifecycle riêng, nhưng chạy trong **`Activity` host**. Có:

- **layout** riêng, **life cycle** riêng, **logic** riêng.
- Vẫn cần **`Activity` host** bọc bên ngoài.
