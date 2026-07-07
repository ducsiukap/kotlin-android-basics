# **`ActionBar` & `Toolbar`**

## 1. **ActionBar**

### 1.1. Phân biệt **`ActionBar`** và **`Toolbar`**

**ActionBar** là một thành phần UI được **cung cấp sẵn bởi Android**, được tính hợp vào **`Activity`'s theme**.

**`Toolbar`** (or `MaterialToolbar`) là một **`View` bình thường**, được đặt trong **layout** của `Activity` hoặc `Fragment`, có khả năng **tùy biến cao** và có thể được sử dụng như một **`ActionBar`**.

> _**`Toolbar`** có thể được nâng cấp lên thành **ActionBar** bằng cách gọi `setSupportActionBar(toolbar)` trong `Activity`_.

### 1.2. **Cấu trúc của `ActionBar`**

**ActionBar**/**Toolbar** được chia thành các vùng từ trái sang phải:

```
[Navigation Icon] [Title / Subtitle] [Content Area] [Menu Items] [Overflow]
```

trong đó:

- **Navigation Icon**: là biểu tượng **điều hướng** (_ví dụ: nút back, nút hamburger menu_).

  ```xml
  app:navigationIcon="@drawable/ic_arrow_back"
  app:navigationIconTint="@color/white"
  ```

  ```kotlin
  binding.toolbar.setNavigationOnClickListener {
      findNavController().popBackStack()
  }
  ```

- **Title / Subtitle**: là **tiêu đề** và **phụ đề** của `ActionBar`/`Toolbar`.

  ```xml
  app:title="Trang chủ"
  app:subtitle="Phụ đề nhỏ hơn bên dưới title"
  <!-- color -->
  app:titleTextColor="@color/white"
  app:subtitleTextColor="@color/white_70"
  app:titleCentered="true"        <!-- đẩy title ra giữa toàn Toolbar -->
  ```

  hoặc có thể set bằng code:

  ```kotlin
  binding.toolbar.title    = "Trang chủ"
  binding.toolbar.subtitle = "12 sản phẩm"

  // Hoặc qua supportActionBar
  supportActionBar?.title    = "Trang chủ"
  supportActionBar?.subtitle = "12 sản phẩm"
  ```

- **Content Area**: là khu vực **chứa các `View` khác** (_ví dụ: `SearchView`, `Spinner`, `TabLayout`_).

  ```xml
  <com.google.android.material.appbar.MaterialToolbar
      android:id="@+id/toolbar"
      android:layout_width="match_parent"
      android:layout_height="?attr/actionBarSize">

      <!-- SearchView đặt thẳng vào Toolbar -->
      <androidx.appcompat.widget.SearchView
          android:id="@+id/searchView"
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:layout_gravity="center" />

  </com.google.android.material.appbar.MaterialToolbar>
  ```

- **Menu Items**: là các **`action` quan trọng**, hiển thị dưới dạng **icon**.

```xml
<!-- res/menu/toolbar_menu.xml -->
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/action_search"
        android:icon="@drawable/ic_search"
        android:title="Tìm kiếm"
        app:showAsAction="ifRoom" />     <!-- hiện trực tiếp nếu đủ chỗ -->
</menu>
```

- **Overflow menu**: là các **`action` phụ**, hiển thị dưới dạng **3 chấm dọc** hoặc dropdown menu.

  ```xml
  <item
      android:id="@+id/action_settings"
      android:title="Cài đặt"
      app:showAsAction="never" />     <!-- luôn nằm trong overflow -->
  ```

### 1.3. **Kích thước chuẩn**

Chiều cao chuẩn của **`ActionBar`** là **56dp** trên điện thoại và **64dp** trên máy tính bảng, **NÊN được lấy từ `?attr/actionBarSize`**

Một số **kích thước chuẩn** khác:

- **Chiều cao `Toolbar`/`ActionBar`**: `?attr/actionBarSize` (_**56dp** trên phone, **64dp** trên tablet_)
- **Navigation icon** / **Menu icon**: `24dp`
- **Touch target**: `48dp` (_kích thước tối thiểu để người dùng thao tác dễ dàng_)
- **Padding ngang**: `16dp` (_start_), `8dp` (_end_ sau menu items)

### 1.4. **Theme** — Ẩn ActionBar mặc định

Khi dùng **`Toolbar`** thay cho **`ActionBar`**, PHẢI **ẩn ActionBar mặc định** bằng cách set theme của `Activity`:

```xml
<!-- res/values/themes.xml -->
<style name="Theme.MyApp" parent="Theme.Material3.DayNight.NoActionBar">
    <!-- NoActionBar → ẩn ActionBar mặc định -->
</style>
```

hoặc có thể set bằng code trong `Activity`:

```kotlin
supportActionBar?.hide()
```

---

## 2. **`Toolbar` / `MaterialToolbar`**

### 2.1. Khai báo **layout**

```xml
<com.google.android.material.appbar.AppBarLayout
    android:id="@+id/appBarLayout"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        app:title="Trang chủ"
        app:titleCentered="false"
        app:navigationIcon="@drawable/ic_arrow_back"
        app:menu="@menu/toolbar_menu" />

</com.google.android.material.appbar.AppBarLayout>
```

`AppBarLayout` là **wrapper** giúp `Toolbar` **phối hợp với CoordinatorLayout (scroll behavior)**. Không bắt buộc nếu không cần scroll behavior.

### 2.2. Setup **`Toolbar`** trong `Activity`

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    // Nâng cấp Toolbar thành ActionBar của Activity
    setSupportActionBar(binding.toolbar)

    // Sau khi setSupportActionBar → dùng supportActionBar để config
    supportActionBar?.apply {
        title = "Trang chủ"
        setDisplayHomeAsUpEnabled(true)    // hiện nút Back/Up
        setDisplayShowTitleEnabled(true)
    }
}

// Xử lý nút navigation (Back/Up) và menu item
override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
        android.R.id.home -> {
            onBackPressedDispatcher.onBackPressed()
            true
        }
        R.id.action_search -> {
            openSearch()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
```

Hoặc setup **`Toolbar`** trong `Fragment`:

```kotlin
class HomeFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Cách 1: Dùng toolbar từ Activity
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Trang chủ"

        // Cách 2: Nếu Toolbar nằm trong Fragment layout
        binding.toolbar.apply {
            title = "Trang chủ"
            setNavigationOnClickListener {
                findNavController().popBackStack()
            }
        }
    }
}
```

### 2.3. **Menu** — Tạo menu cho `Toolbar`

```xml
<!-- res/menu/toolbar_menu.xml -->
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <!-- Hiện trực tiếp trên Toolbar -->
    <item
        android:id="@+id/action_search"
        android:icon="@drawable/ic_search"
        android:title="Tìm kiếm"
        app:showAsAction="ifRoom" />

    <!-- Nằm trong overflow menu (3 chấm) -->
    <item
        android:id="@+id/action_settings"
        android:title="Cài đặt"
        app:showAsAction="never" />

    <item
        android:id="@+id/action_logout"
        android:title="Đăng xuất"
        app:showAsAction="never" />

</menu>
```

Thuộc tính **`app:showAsAction`** có các giá trị:

- `always`: **luôn hiển thị** trên Toolbar
- `ifRoom`: hiển thị trên Toolbar **nếu còn chỗ**. Nếu không, cho vào **overflow**.
- `never`: **luôn nằm trong overflow menu**.
- `withText`: hiển thị **cả icon và text** trên Toolbar.

**Inflate menu** trong `Activity`:

```kotlin
override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.toolbar_menu, menu)
    return true
}

override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
        R.id.action_search   -> { openSearch(); true }
        R.id.action_settings -> { openSettings(); true }
        R.id.action_logout   -> { logout(); true }
        else -> super.onOptionsItemSelected(item)
    }
}
```

hoặc **inflate trực tiếp trong `Toolbar`** thông qua `inflateMenu()`:

```kotlin
binding.toolbar.apply {
    inflateMenu(R.menu.toolbar_menu)
    setOnMenuItemClickListener { item ->
        when (item.itemId) {
            R.id.action_search -> { openSearch(); true }
            else -> false
        }
    }
}
```

### 2.4. **Navigation Component** + `ToolBar`

```kotlin
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var appBarConfig: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // AppBarConfig -> defines Top-level destinations —
        // không hiện nút Back
        appBarConfig = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.searchFragment, R.id.profileFragment)
        )

        // Kết nối Toolbar với NavController
        // → title tự đổi theo android:label trong nav_graph
        // → nút Back tự hiện/ẩn theo destination
        setupActionBarWithNavController(navController, appBarConfig)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfig) || super.onSupportNavigateUp()
    }
}
```

### 2.5. **`MaterialToolbar`'s attributes**

```xml
<com.google.android.material.appbar.MaterialToolbar
    app:title="Tiêu đề"
    app:subtitle="Phụ đề"
    app:titleCentered="true"                          <!-- căn giữa title -->
    app:titleTextColor="@color/white"
    app:subtitleTextColor="@color/white_70"
    app:navigationIcon="@drawable/ic_arrow_back"       <!-- icon nút Back -->
    app:navigationIconTint="@color/white"
    app:menu="@menu/toolbar_menu"                      <!-- menu trực tiếp từ XML -->
    android:background="@color/primary"
    android:elevation="4dp" />
```

trong đó:

- Nhóm **Text**/title:
  - `app:title` / `app:subtitle`: set **title**/**subtitle**
  - `app:titleCentered`: **căn giữa** title
  - `app:titleTextColor` / `app:subtitleTextColor`: set **màu chữ**
- **Navigation Icon**:
  - `app:navigationIcon`: set **icon** cho nút Back/Up
  - `app:navigationIconTint`: set **màu icon**
- **Menu**:
  - `app:menu`: set **menu trực tiếp từ XML** - `"@menu/toolbar_menu"`
- **Background & Elevation**:
  - `android:background`: set **màu nền**
  - `android:elevation`: set **độ nổi** của Toolbar
