# **_Layout_**: `FrameLayout`

## 1. `FrameLayout`

`FrameLayout` là **ViewGroup đơn giản nhất** trong Android. Thiết kế gốc của nó **chỉ dành để chứa một View con duy nhất**.

Khi có nhiều View con, chúng xếp chồng lên nhau (`stack`) theo thứ tự khai báo trong XML — View khai báo sau sẽ nằm trên View khai báo trước

- **Không có** cơ chế **sắp xếp tự động** như `LinearLayout`.
- **Không** có `constraint` như `ConstraintLayout`.

> _`FrameLayout` không quan tâm đến việc các View con có đè lên nhau hay không — đó là trách nhiệm của developer_.

---

## 2. **`02` usecase** chính

### Usecase 1: **Fragment container**

Đây là use case **phổ biến nhất** của `FrameLayout` trong thực tế.

- `FragmentManager` cần một **ViewGroup** để **đặt Fragment** vào
- `FrameLayout` là **lựa chọn chuẩn** vì nó không áp đặt bất kỳ rule sắp xếp nào lên Fragment bên trong.

**(1) `layout`:**

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize" />

    <!-- Fragment container:
    - FrameLayout chiếm toàn bộ phần còn lại
    - Fragment sẽ được đặt vào Frame này
     -->
    <FrameLayout
        android:id="@+id/fragmentContainer"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />
</LinearLayout>
```

**(2) Đặt `Fragment` vào `FrameLayout`:**

```kotlin
supportFragmentManager // lấy FragmentManager từ Activity
    .beginTransaction()
    .replace(
        R.id.fragmentContainer, // tìm FrameLayout bằng id
        HomeFragment()          // Thay thế FrameLayout bằng HomeFragment
    )
    .commit()
```

- `FragmentManager` sẽ **inflate `Fragment`** và **đặt View** của nó vào trong `fragmentContainer`.
- Khi `replace()` được gọi, **Fragment cũ bị gỡ** ra, **Fragment mới được đặt vào**

> _`FrameLayout` chỉ đóng vai trò là "**chỗ chứa**", không can thiệp vào gì cả._

### Usecase 2: **Overlay / Stack Views**

Khi bạn cần một **View nằm đè lên View khác** — _ảnh với text caption_, _loading spinner_ che phủ nội dung, _badge_ số thông báo trên icon, ... — `FrameLayout` là **công cụ phù hợp nhất**.

**Ví dụ:**

- Image + text caption

  ```xml
  <!-- Ảnh với text caption chồng lên góc dưới -->
  <FrameLayout
      android:layout_width="match_parent"
      android:layout_height="200dp">

      <!-- Tầng 1: ảnh nền -->
      <ImageView
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          android:src="@drawable/banner_image"
          android:scaleType="centerCrop" />

      <!-- Tầng 2: gradient overlay để text dễ đọc hơn -->
      <View
          android:layout_width="match_parent"
          android:layout_height="80dp"
          android:layout_gravity="bottom"
          android:background="@drawable/gradient_bottom" />

      <!-- Tầng 3: text caption -->
      <TextView
          android:layout_width="wrap_content"
          android:layout_height="wrap_content"
          android:layout_gravity="bottom|start"
          android:layout_margin="12dp"
          android:text="Tiêu đề bài viết"
          android:textColor="#FFFFFF"
          android:textSize="18sp"
          android:textStyle="bold" />

  </FrameLayout>
  ```

- Loading stage: _nội dung chính_ + _loading indicator_ + _empty state_.

  ```xml
  <FrameLayout
      android:layout_width="match_parent"
      android:layout_height="match_parent">

      <!-- Tầng 1: nội dung chính -->
      <androidx.recyclerview.widget.RecyclerView
          android:id="@+id/recyclerView"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          android:visibility="gone" />

      <!-- Tầng 2: loading spinner -->
      <ProgressBar
          android:id="@+id/progressBar"
          android:layout_width="wrap_content"
          android:layout_height="wrap_content"
          android:layout_gravity="center"
          android:visibility="visible" />

      <!-- Tầng 3: empty state view
      + visibility: "gone"
        -> khi không có dữ liệu
        -> set visibility thành "visible"
      -->
      <LinearLayout
          android:id="@+id/layoutEmpty"
          android:layout_width="wrap_content"
          android:layout_height="wrap_content"
          android:layout_gravity="center"
          android:orientation="vertical"
          android:gravity="center"
          android:visibility="gone">

          <ImageView
              android:layout_width="80dp"
              android:layout_height="80dp"
              android:src="@drawable/ic_empty" />

          <TextView
              android:layout_width="wrap_content"
              android:layout_height="wrap_content"
              android:text="Không có dữ liệu"
              android:layout_marginTop="8dp" />

      </LinearLayout>

  </FrameLayout>
  ```

  Điều khiển `state`:

  ```kotlin
  // Đang loading
  fun showLoading() {
      binding.progressBar.visibility = View.VISIBLE
      binding.recyclerView.visibility = View.GONE
      binding.layoutEmpty.visibility = View.GONE
  }

  // Có data
  fun showContent() {
      binding.progressBar.visibility = View.GONE
      binding.recyclerView.visibility = View.VISIBLE
      binding.layoutEmpty.visibility = View.GONE
  }

  // Không có data
  fun showEmpty() {
      binding.progressBar.visibility = View.GONE
      binding.recyclerView.visibility = View.GONE
      binding.layoutEmpty.visibility = View.VISIBLE
  }
  ```

---

## 3. `layout_gravity`

**Thuộc tính duy nhất** có ý nghĩa để **căn chỉnh View con** trong `FrameLayout` là `android:layout_gravity`.

> _Mặc định nếu không khai báo, View con sẽ nằm ở góc trên trái -> `top|start`._

```xml
<FrameLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <View android:layout_width="60dp" android:layout_height="60dp"
        android:background="#FF0000"
        android:layout_gravity="top|start" />        <!-- góc trên trái (mặc định) -->

    <View android:layout_width="60dp" android:layout_height="60dp"
        android:background="#00FF00"
        android:layout_gravity="top|end" />          <!-- góc trên phải -->

    <View android:layout_width="60dp" android:layout_height="60dp"
        android:background="#0000FF"
        android:layout_gravity="center" />           <!-- chính giữa -->

    <View android:layout_width="60dp" android:layout_height="60dp"
        android:background="#FFFF00"
        android:layout_gravity="bottom|start" />     <!-- góc dưới trái -->

    <View android:layout_width="60dp" android:layout_height="60dp"
        android:background="#FF00FF"
        android:layout_gravity="bottom|end" />       <!-- góc dưới phải -->

</FrameLayout>
```

Kết quả:

```
┌─────────────────────────────┐
│ 🔴                      🟢 │
│                             │
│           🔵               │
│                             │
│ 🟡                      🟣 │
└─────────────────────────────┘
```
