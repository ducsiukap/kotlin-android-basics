# **Navigation** component & `NavHostFragment`

## 1. **_Navigation_ component**

### 1.1. `FragmentManager` & vấn đề trước khi có **Navigation Component**

Quản lý `Fragment` thủ công bằng `FragmentManager` có nhiều vấn đề trong app lớn:

- **Vấn đề 1**: **Boilerplate** lặp, phải viết `beginTransaction().replace().addToBackStack().commit()` mỗi lần navigation.
- **Vấn đề 2**: Truyền **data** không có **type-safe**, `putExtra("key", value)` dễ bị lỗi **typo** nếu không quản lí tốt.
- **Vấn đề 3**: Quản lý **back stack** phức tạp, tự quản lý `addToBackStack`/`popBackStack`, dễ bug.
- **Vấn đề 4**: Không có hỗ trợ tốt cho **deep linking**, phải tự **parse URI**, tự navigate tới đúng `Fragment`
- **Vấn đề 5**: không hỗ trợ **navigation graph**, khó quản lý flow phức tạp.

**`Navigation Component` (Jetpack)** giải quyết toàn bộ các vấn đề này.

### 1.2. **Navigation Component**

**`Navigation Component`** gồm `3` thành phần:

- (1). **Navigation Graph** (`nav_graph.xml`): là file mô tả toàn bộ **screen** và **flow**.
- (2). **`NavHostFragment`**: là **container** trong `Activity`, cho phép các `Fragment` được swap vào/ra.
- (3). **`NavController`**: là object điều phối navigation, có `navigate()`, `popBackStack()`, `navigateUp()`, ...

---

## 2. Setup cơ bản

### 2.1. Thêm **dependency**

```gradle
// build.gradle.kts (Module: app)
dependencies {
    val navVersion = "2.7.7"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")
}
```

### 2.2. Tạo **Navigation Graph**: [nav_graph.xml](./nav_graph.md)

`nav_graph.xml` là file XML đặt tại `res/navigation/` — mô tả toàn bộ **cấu trúc navigation của app**:

- màn hình nào tồn tại
- từ màn hình nào có thể đi đến màn hình nào
- truyền data gì khi đi.

### 2.3. Setup `NavHostFragment` trong `Activity` layout - **Fragment container**

```xml
<!-- activity_main.xml -->
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <!-- NavHostFragment — container cho toàn bộ navigation -->
    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/navHostFragment"
        android:name="androidx.navigation.fragment.NavHostFragment"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        app:navGraph="@navigation/nav_graph"          <!-- gắn nav graph -->
        app:defaultNavHost="true" />                  <!-- intercept Back button -->

    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottomNavigation"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:menu="@menu/bottom_nav_menu" />

</LinearLayout>
```

`app:defaultNavHost="true"` — `NavHostFragment` sẽ intercept Back button của hệ thống, xử lý **pop Fragment** thay vì finish Activity.

### 2.4. Setup `NavController` trong `Activity`

```kotlin
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lấy NavController từ NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Kết nối BottomNavigationView với NavController
        binding.bottomNavigation.setupWithNavController(navController)

        // Kết nối ActionBar/Toolbar với NavController (Up navigation)
        setupActionBarWithNavController(navController)
    }

    // Handle Up button trên ActionBar
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
```

---

## 3. **Navigate** giữa các `Fragment`: [Fragment navigation](./fragment_navigation.md)

Summary:

- Lấy `NavController` trong `Fragment`
- Định nghĩa `Bundle` hoặc cài plugin **Safe Args** để truyền data
- Gọi `navigate()` với `<action>.ID` hoặc destination ID
- Quản lý **back stack** với `NavController`, handle **Back** button.
- **Deep link**
- Một số pattern: **navigate + xóa fragment** hiện tại, navigate từ `Adapter`, **double navigate** problem, **observe navigation result**, .

---

## 4. Tích hợp `NavController` với **UI Components**

### 4.1. `BottomNavigationView` + `NavController`

Khởi tạo **Menu items** cho `BottomNavigationView`:

> _**Note**: Menu `item ID` trong `bottom_nav_menu.xml` phải trùng với `Fragment ID` trong `nav_graph.xml`_

```xml
<!-- bottom_nav_menu.xml -->
<item android:id="@+id/homeFragment" .../>    <!-- ID phải khớp -->
<item android:id="@+id/searchFragment" .../>
<item android:id="@+id/profileFragment" .../>

<!-- nav_graph.xml -->
<fragment android:id="@+id/homeFragment" .../>   <!-- ID phải khớp -->
<fragment android:id="@+id/searchFragment" .../>
<fragment android:id="@+id/profileFragment" .../>
```

Bind **BottomNavigationView** & **NavController**: `setupWithNavController()`

```kotlin
// setupWithNavController tự động:
// → Chuyển Fragment khi tap tab
// → Highlight tab đang active
// → Handle Back stack
binding.bottomNavigation.setupWithNavController(navController)
```

### 4.2. `Toolbar` + `NavController`

```kotlin
// Tự động hiện tên Fragment (android:label trong nav_graph)
// Tự động hiện nút Back khi không phải start destination
setupActionBarWithNavController(navController)

// Custom AppBarConfiguration — xác định top-level destinations
// (không hiện nút Back khi ở các màn hình này)
val appBarConfig = AppBarConfiguration(
    setOf(R.id.homeFragment, R.id.searchFragment, R.id.profileFragment)
)
setupActionBarWithNavController(navController, appBarConfig)
binding.bottomNavigation.setupWithNavController(navController)
```

### 4.3. **Observe thay đổi destination**: `addOnDestinationChangedListener`

Observe thay đổi destination **để cập nhật UI** (ví dụ: ẩn/hiện `BottomNavigationView`)

```kotlin
navController.addOnDestinationChangedListener { controller, destination, arguments ->
    when (destination.id) {
        R.id.homeFragment -> {
            binding.bottomNavigation.visibility = View.VISIBLE
        }
        R.id.productDetailFragment -> {
            binding.bottomNavigation.visibility = View.GONE
        }
    }
}
```
