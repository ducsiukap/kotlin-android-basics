# `BottomNavigationView`

`BottomNavigationView` là **navigation pattern** cho phép user chuyển giữa **3–5 destination chính** bằng cách **tap vào icon** ở thanh dưới cùng màn hình (_gọi là **bottom navigation**_).

## 1. Khai báo `BottomNavigationView` trong **layout**

```xml
<com.google.android.material.bottomnavigation.BottomNavigationView
    android:id="@+id/bottomNavigation"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_gravity="bottom"
    app:menu="@menu/bottom_nav_menu"
    app:labelVisibilityMode="labeled" />
```

trong đó, `labelVisibilityMode` có thể là:

- **"labeled"**: luôn hiển thị label
- **"unlabeled"**: không hiển thị label
- **"selected"**: chỉ hiển thị label của item đang được chọn
- **"auto"**: tự quyết định dựa trên số lượng
  > _Hiển thị label của **item đang được chọn nếu số lượng item <= 3**, ngược lại sẽ không hiển thị label_

### **Build menu** - `bottom_nav_menu.xml`

```xml
<!-- res/menu/bottom_nav_menu.xml -->
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
     <!-- ID phải khớp với Fragment ID trong nav_graph -->
    <item
        android:id="@+id/homeFragment"
        android:icon="@drawable/ic_home"
        android:title="Trang chủ" />

    <item
        android:id="@+id/searchFragment"
        android:icon="@drawable/ic_search"
        android:title="Tìm kiếm" />

    <item
        android:id="@+id/profileFragment"
        android:icon="@drawable/ic_person"
        android:title="Hồ sơ" />

</menu>
```

**Note**: với mỗi `item` trong **menu**, `id` của nó **PHẢI KHỚP CHÍNH XÁC** với `id` của **Fragment** trong `nav_graph.xml` nếu muốn sử dụng `BottomNavigationView` với **Navigation Component**.

---

## 2. Sử dụng `BottomNavigationView` với **Navigation Component**

### **Kết nối với `NavController`**

```kotlin
binding.bottomNavigation.setupWithNavController(navController)
```

với `setupWithNavController()`, **BottomNavigationView** sẽ TỰ ĐỘNG có khả năng:

- **Chuyển `Fragment`** khi tap icon tương ứng
- **Highlight icon** của `item` đang được chọn
- **Handle back button**: handle **backstack** đúng cách (_pop về `startDestination` của **tab**_)

### **Show/Hide `BottomNavigationView` by `destination`**

`NavController` cung cấp **callback** `addOnDestinationChangedListener()` để **lắng nghe sự kiện chuyển đổi destination**. <br/>
Từ đó, có thể **show/hide `BottomNavigationView`** dựa trên `destination` hiện tại, trong đó:

- `destination.id` là **ID của Fragment** trong `nav_graph.xml`
- `destination.parent.id` là **ID của NavGraph** (_root_) chứa `destination` đó

```kotlin
navController.addOnDestinationChangedListener { _, destination, _ ->
    val hideOnDestinations = setOf(
        R.id.productDetailFragment,
        R.id.checkoutFragment,
        R.id.loginFragment
    )

    binding.bottomNavigation.visibility =
        if (destination.id in hideOnDestinations) View.GONE
        else View.VISIBLE
}
```

### **`Badge` — Số thông báo trên tab**

```kotlin
// Thêm badge số
val badge = binding.bottomNavigation.getOrCreateBadge(R.id.homeFragment)
badge.number = 5
badge.isVisible = true

// Badge chấm (không có số)
val dotBadge = binding.bottomNavigation.getOrCreateBadge(R.id.searchFragment)
dotBadge.isVisible = true   // không set number → hiện chấm

// Xóa badge
binding.bottomNavigation.removeBadge(R.id.homeFragment)
```

### **Customize `BottomNavigationView`**

```xml
<com.google.android.material.bottomnavigation.BottomNavigationView
    app:itemIconTint="@color/bottom_nav_color"       <!-- màu icon -->
    app:itemTextColor="@color/bottom_nav_color"      <!-- màu text -->
    app:itemActiveIndicatorStyle="@style/..."        <!-- background khi active -->
    android:background="@color/white"
    app:elevation="8dp" />
```

trong đó:

- `app:itemIconTint` và `app:itemTextColor`: là **màu icon và text**<br/>
  Có thể là **`selector`** để thay đổi màu khi **active/inactive**<br/>
  _eg. `color/bottom_nav_color.xml`_:

  ```xml
  <!-- res/color/bottom_nav_color.xml — selector -->
  <?xml version="1.0" encoding="utf-8"?>
  <selector xmlns:android="http://schemas.android.com/apk/res/android">
      <item android:color="@color/primary" android:state_checked="true" />
      <item android:color="@color/gray" />
  </selector>
  ```

- `app:itemActiveIndicatorStyle`: là **background** khi **item đang được chọn**
- `android:background`: là **background** tổng thể của **`BottomNavigationView`**
- `app:elevation`: là **độ cao** của **`BottomNavigationView`**, giúp tạo **shadow** phía dưới
