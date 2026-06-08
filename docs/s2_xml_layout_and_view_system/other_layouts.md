# **_Layout_**: `ScrollView`, `NestedScrollView`, `CoordinatorLayout`, `DrawerLayout`, ...

## 1. `ScrollView`, `HorizontalScrollView` & `NestedScrollView`

**Vấn đề nó giải quyết**: `ConstraintLayout` hay `LinearLayout` đều **render cố định** trong vùng màn hình. Khi **nội dung dài** hơn màn hình → cần **container có khả năng `scroll`**.

- `ScrollView` → scroll theo chiều dọc
- `HorizontalScrollView` → scroll theo chiều ngang
- `NestedScrollView` → scroll theo chiều dọc, hỗ trợ nested scrolling (scroll lồng nhau)

```xml
<ScrollView
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- Child
    + thường là LinearLayout hoặc ConstraintLayout
        để chứa nội dung dài
    + thường có height=wrap_context
        -> ScrollView cần biết nội dung thực tế
        -> dài đến đâu để cho phép cuộn
    -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <!-- Nội dung dài tùy ý -->

    </LinearLayout>

</ScrollView>
```

**Quy tắc bắt buộc**: `ScrollView` chỉ chấp nhận **đúng 1 View con trực tiếp**. Vì vậy thực tế luôn đặt một `LinearLayout` hoặc `ConstraintLayout` bên trong.

---

## 2. `RyclcerView`

**Vấn đề nó giải quyết**: Giả sử bạn có danh sách `1000 items`. Nếu dùng `ScrollView` + `LinearLayout` và inflate 1000 View cùng lúc → **OOM (Out of Memory)**, app crash.

Cơ chế của `RecyclerView` là **tái sử dụng View**: Chỉ inflate đủ số View hiển thị:<br>

eg:
Màn hình chỉ hiển thị `~8 item`

- `RecyclerView` chỉ giữ `~10 View` trong bộ nhớ
- Khi scroll, View ra khỏi màn hình được "**tái sử dụng**"
  cho item mới scroll vào — không tạo View mới
  > _→ 1000 item nhưng chỉ tốn RAM của ~10 View_

**Khi nào dùng**: _Danh sách_, _grid_, _feed_ — bất kỳ tập **dữ liệu động** và có thể **lớn**. Trong thực tế, `RecyclerView` xuất hiện ở hầu hết mọi màn hình của app.

---

## 3. `CoordinatorLayout`

Thuộc thư viện **Material Design** (`androidx.coordinatorlayout`), không phải Android core. Đây là layout chuyên dụng cho các **interaction phức tạp giữa các View**.

Giải quyết các bài toán:

- `AppBar` thu nhỏ/ẩn đi khi scroll nội dung xuống
- `FloatingActionButton` tự né lên khi `Snackbar` xuất hiện
- `CollapsingToolbarLayout` — header ảnh lớn co lại thành toolbar khi scroll

**Cơ chế hoạt động**: `CoordinatorLayout` dùng `Behavior` — mỗi `View con` có thể đăng ký một `Behavior object` để **lắng nghe sự kiện** từ các `View khác` và **phản ứng tương ứng**.

---

## 4. `DrawerLayout`

Chuyên dụng cho **Navigation Drawer** — menu kéo ra từ cạnh màn hình.

```xml
<DrawerLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- Nội dung chính — phải là View con ĐẦU TIÊN -->
    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <!-- Navigation Drawer — phải có gravity -->
    <NavigationView
        android:layout_gravity="start"
        android:layout_width="wrap_content"
        android:layout_height="match_parent" />

</DrawerLayout>
```

**Quy tắc**: `DrawerLayout` phải có **đúng 2 View con**:

- View con đầu tiên là **main content**
- View con thứ hai có `android:layout_gravity="start"` (hoặc `end`) là **drawer panel**.

Dùng khi **App có navigation drawer** — hiện tại pattern này đang dần nhường chỗ cho `BottomNavigationView`, nhưng vẫn rất phổ biến trong các app lớn.
