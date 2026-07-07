# `NavigationView` and **Navigation Drawer**

## 1. **Navigation Drawer** & `NavigationView`

### 1.1. **Navigation Drawer** là gì?

**Navigation Drawer** là tên gọi của một **UI PATTERN**, không phải class cụ thể.<br/>
Nó là một **panel chứa menu**, trượt ra từ **cạnh trái** (hoặc phải) của màn hình, thường ẩn đi. **Navigation Drawer** chỉ hiện ra khi:

- **User vuốt từ cạnh** màn hình vào trong
- **User nhấn vào nút menu** (thường là **hamburger icon `☰`**) trên **`ActionBar`** hoặc **`Toolbar`**.

Để xây dựng **pattern** này, cần **2 THÀNH PHẦN** kĩ thuật:

- `DrawerLayout` (class) - **container** quản lý việc **trượt** cho **Navigation Drawer**
- `NavigationView` (class) - **menu content** hiển thị trong panel **Navigation Drawer**

### 1.2. **`NavigationView`** là gì?

`NavigationView` là một **class** trong **Material Components library**, dùng để triển khai **Navigation Drawer pattern**

### 1.3. **So sánh** với các **Navigation pattern** khác

- **Bottom Navigation**: duy trì **3-5 `top-level` destinations**, luôn hiển thị **navigation menu**, truy cập ngay lập tức.
- **TabLayout**: các màn hình **NGANG HÀNG**, cuộn ngang được.
- **Navigation Drawer**: duy trì **nhiều destinations**, ẩn menu.

> _Theo **khuyến nghị** Material Design hiện tại, **Navigation Drawer KHÔNG còn là lựa chọn ưu tiên hàng đầu** cho điện thoại._

Theo xu hướng **Material Design** HIỆN TẠI, có **3 lý do** chính khiến **Navigation Drawer** đang **dần ít phổ biến**:

1. **Gesture Navigation** của hệ thống (Android 10+)<br/>
   → Android hiện đại dùng "vuốt từ cạnh để BACK" làm gesture
   TOÀN CỤC — XUNG ĐỘT TRỰC TIẾP với gesture "vuốt từ cạnh
   để mở Drawer" — 2 gesture CÙNG VÙNG, gây nhầm lẫn cho user
   và khó xử lý về mặt kỹ thuật (phải cân bằng độ nhạy, vùng
   kích hoạt)

2. **"2 bước để điều hướng"** — **mở panel + click item**: kém hiệu quả hơn `BottomNavigation`<br/>
   → Drawer LUÔN cần 2 THAO TÁC (mở → chọn), trong khi Bottom
   Navigation chỉ cần 1 TAP — với xu hướng UX ưu tiên "ít
   thao tác nhất có thể", Bottom Navigation being ưu tiên
   hơn cho các mục ĐIỀU HƯỚNG CHÍNH

3. **"Ẩn" thông tin quan trọng**<br/>
   → Các mục điều hướng CHÍNH bị GIẤU trong Drawer (không thấy
   ngay) — nghiên cứu UX cho thấy user THƯỜNG BỎ QUA những gì
   không hiển thị trực tiếp trên màn hình ("out of sight, out
   of mind")

Khuyến nghị Material Design hiện tại:

- **Bottom Navigation cho 3-5 mục CHÍNH**
- **Navigation Drawer** CHỈ nên dùng cho:
  1. **TABLET/ màn hình lớn** (nơi Drawer có thể ở dạng "cố định, luôn hiện" — `LOCK_MODE_LOCKED_OPEN`), hoặc
  2. THỰC SỰ **có NHIỀU mục điều hướng phụ** (settings, help, about...) không đủ chỗ ở Bottom Nav

---

## 2. `DrawerLayout` — **container** quản lý behavior **"trượt"**

### 2.1. **`DrawerLayout`** là gì?

`DrawerLayout` LÀ 1 ViewGroup ĐẶC BIỆT — **kế thừa cơ chế tương tự FrameLayout**, nhưng bổ sung **THÊM logic quản lý VIỆC TRƯỢT panel** con dựa theo **`gesture` (vuốt)** và **`trạng thái` mở/đóng**.<br/>
Nhiệm vụ chính của `DrawerLayout`:

1. Lắng nghe **gesture** vuốt từ CẠNH màn hình -> **tính toán vị trí trượt** của panel con theo thời gian thực (theo ngón tay)
2. Quản lý **trạng thái**: `OPEN` / `CLOSED` / `SETTLING` (_đang trong lúc animation trượt_)
3. Vẽ LỚP MỜ (**scrim/overlay**) phía sau panel khi đang mở — để làm nổi bật panel VÀ **cho phép tap ra ngoài để đóng**
4. **Chặn/cho** phép user **tương tác với nội dung CHÍNH phía sau** khi drawer đang mở (_mặc định: tap vào vùng mờ = đóng drawer_)

### 2.2. **Quy tắc `structure` của `DrawerLayout`**

`DrawerLayout` CHỈ CHẤP NHẬN **ĐÚNG 2 View con TRỰC TIẾP**, và phải theo **THỨ TỰ CỐ ĐỊNH**:

- **View con THỨ NHẤT**: là **main content**, luôn hiển thị và chiếm toàn bộ màn hình.
- **View con THỨ HAI**: là **drawer panel**, ẩn đi, trượt ra từ cạnh. **Phải** có thuộc tính `android:layout_gravity="start"` (hoặc `"left"`) để xác định **cạnh trượt ra**.

> _**Thứ tự này QUAN TRỌNG** vì `DrawerLayout` dùng chính **THỨ TỰ khai báo** (không phải gravity) để **phân biệt "cái nào là content chính, cái nào là drawer"** ở tầng thấp nhất_

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.drawerlayout.widget.DrawerLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/drawerLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- View con THỨ NHẤT — Main Content -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical">

        <androidx.appcompat.widget.Toolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="?attr/colorPrimary"
            android:theme="@style/ThemeOverlay.AppCompat.Dark.ActionBar" />

        <androidx.fragment.app.FragmentContainerView
            android:id="@+id/nav_host_fragment"
            android:name="androidx.navigation.fragment.NavHostFragment"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            app:navGraph="@navigation/nav_graph"
            app:defaultNavHost="true" />

    </LinearLayout>

    <!-- View con THỨ HAI — NavigationView, BẮT BUỘC layout_gravity -->
    <com.google.android.material.navigation.NavigationView
        android:id="@+id/navigationView"
        android:layout_width="wrap_content"
        android:layout_height="match_parent"
        android:layout_gravity="start"
        app:headerLayout="@layout/nav_header"
        app:menu="@menu/drawer_menu" />

</androidx.drawerlayout.widget.DrawerLayout>
```

### 2.3. **Lock Mode** — kiểm soát **hành vi vuốt** của `DrawerLayout`

`DrawerLayout` có **4 CHẾ ĐỘ khóa (LockMode)** — quyết định `GESTURE` vuốt có **được PHÉP hoạt động** hay không:

- `LOCK_MODE_UNLOCKED` (mặc định) — **cho phép** vuốt mở/đóng drawer bình thường
- `LOCK_MODE_LOCKED_CLOSED` — **Drawer luôn đóng**, user **KHÔNG thể vuốt để mở**, chỉ có thể **mở chủ động** bằng code hoặc **tab Hamberger icon** (_nếu đã set_)
  > _Dùng cho: **màn hình con** (ví dụ Detail screen) **KHÔNG nên cho user vuốt mở Drawer nhầm** khi đang thao tác (vd: đang vuốt ảnh trong Gallery, dễ TRÙNG gesture với mở Drawer)_
- `LOCK_MODE_LOCKED_OPEN` — ngược lại với **locked closed**, mode này **KHÔNG cho phép vuốt để đóng Drawer** — dùng cho
  TABLET (nơi Drawer thường hiển thị CỐ ĐỊNH bên cạnh, không
  cần ẩn/hiện).
- `LOCK_MODE_UNDEFINED` — là **giá trị mặc định nội bộ**, hiếm khi set thủ công.

### 2.4. **`DrawerListener`** — lắng nghe **trạng thái drawer**

`DrawerListener` là **interface** để lắng nghe **vòng đời trạng thái drawer**. aNó có 4 callback:

```kotlin
drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
        // Gọi LIÊN TỤC trong lúc TRƯỢT
        // slideOffset từ 0.0 (đóng hoàn toàn) đến 1.0 (mở hoàn toàn)
    }

    override fun onDrawerOpened(drawerView: View) {
        // Gọi 1 LẦN DUY NHẤT khi Drawer đã MỞ HẲN (animation xong)
    }

    override fun onDrawerClosed(drawerView: View) {
        // Gọi 1 LẦN khi Drawer đã ĐÓNG HẲN
    }

    override fun onDrawerStateChanged(newState: Int) {
        // STATE_IDLE / STATE_DRAGGING / STATE_SETTLING
        // — theo dõi TRẠNG THÁI TƯƠNG TÁC chi tiết hơn (đang
        // kéo tay, hay đang tự chạy nốt animation sau khi thả tay)
    }
})
```

Trong `onDrawerSlide()`, `slideOffset` là **giá trị float từ 0.0 đến 1.0**, thể hiện **tỉ lệ mở - `getsure` - của drawer** (_0 = đóng hoàn toàn, 1 = mở hoàn toàn_).<br/>
Phù hợp để dùng để làm **hiệu ứng PHỤ THUỘC theo tiến độ kéo** — ví dụ:

- Mờ dần nội dung chính khi Drawer trượt ra (fade content theo
  slideOffset),
- Hoặc icon Hamburger "xoay dần" thành mũi tên

---

## 3. `NavigationView` — **Content** hiển thị trong panel **Navigation Drawer**

### 3.1. **Thành phần** của `NavigationView`

```xml
<!-- View con THỨ HAI — NavigationView, BẮT BUỘC layout_gravity -->
<com.google.android.material.navigation.NavigationView
    android:id="@+id/navigationView"
    android:layout_width="wrap_content"
    android:layout_height="match_parent"
    android:layout_gravity="start"
    app:headerLayout="@layout/nav_header"
    app:menu="@menu/drawer_menu" />
```

**`NavigationView` là view con được đặt bên trong `DrawerLayout`**, chịu trách nhiệm làm **container** hiển thị:

1. **`Header`** (_tùy chọn_) — thường là **avatar, tên user, email**

   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <LinearLayout
       xmlns:android="http://schemas.android.com/apk/res/android"
       android:layout_width="match_parent"
       android:layout_height="180dp"
       android:orientation="vertical"
       android:background="?attr/colorPrimary"
       android:gravity="bottom"
       android:padding="16dp">
       <!-- avatar -->
       <ImageView
           android:id="@+id/ivAvatar"
           android:layout_width="64dp"
           android:layout_height="64dp"
           android:src="@drawable/ic_default_avatar"
           android:background="@drawable/circle_bg" />
       <!-- user name -->
       <TextView
           android:id="@+id/tvUserName"
           android:layout_width="wrap_content"
           android:layout_height="wrap_content"
           android:layout_marginTop="8dp"
           android:text="Nguyễn Văn A"
           android:textColor="@android:color/white"
           android:textStyle="bold" />
       <!-- user email -->
       <TextView
           android:id="@+id/tvUserEmail"
           android:layout_width="wrap_content"
           android:layout_height="wrap_content"
           android:text="a.nguyen@example.com"
           android:textColor="@android:color/white" />

   </LinearLayout>
   ```

2. **Menu `items`** — danh sách các mục menu (**danh sách lựa chọn**) để người dùng lựa chọn

   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <menu xmlns:android="http://schemas.android.com/apk/res/android">

       <!-- group dùng để TỰ ĐỘNG có radio-button behavior (chỉ 1
           item được chọn tại 1 thời điểm) qua checkableBehavior -->
       <group android:checkableBehavior="single">
           <item
               android:id="@+id/nav_home"
               android:icon="@drawable/ic_home"
               android:title="Trang chủ"
               android:checked="true" />
           <item
               android:id="@+id/nav_contacts"
               android:icon="@drawable/ic_contacts"
               android:title="Danh bạ" />
           <item
               android:id="@+id/nav_favorites"
               android:icon="@drawable/ic_star"
               android:title="Yêu thích" />
       </group>

       <!-- Divider TỰ ĐỘNG chèn giữa 2 <item> nằm NGOÀI cùng 1 <group> -->

       <item
           android:id="@+id/nav_settings"
           android:icon="@drawable/ic_settings"
           android:title="Cài đặt" />
       <item
           android:id="@+id/nav_logout"
           android:icon="@drawable/ic_logout"
           android:title="Đăng xuất" />

   </menu>
   ```

   > _Sử dụng `checkableBehavior="single"` trong `<group>` để tự động tạo **behavior radio-button (chỉ chọn 1 item tại 1 thời điểm)** cho các item trong cùng 1 group. Khi user chọn 1 item, item trước đó sẽ **bị bỏ chọn**._

3. **`Divider`** (_tùy chọn_) — **phân tách** các nhóm menu

### 3.2. **Tương tác** với `NavigationView` & `ActionBarDrawerToggle`

#### **Open/Close Navigation Drawer**

Về mặt lí thuyết, có **3 cách** để mở **Navigation Drawer**:

- **Vuốt từ cạnh** màn hình vào trong, **`DrawerLayout` TỰ ĐỘNG lắng nghe `gesture` này**
- Tap vào **hamburger icon** trên **`ActionBar`/`Toolbar`**, cần kết nối `ToolBar` với `DrawerLayout` bằng `ActionBarDrawerToggle`
- **Mở chủ động** bằng code:

  ```kotlin
  drawerLayout.openDrawer(GravityCompat.START)
  drawerLayout.closeDrawer(GravityCompat.START)
  ```

Sử dụng `GravityCompat` thay vì `Gravity` để **tương thích với layout direction** (LTR/RTL):

- `Gravity.START` = cạnh **bắt đầu** theo **ngôn ngữ đọc của thiết bị**
- `GravityCompat.START` = **PHIÊN BẢN "an toàn" hơn**, tự động
  xử lý ĐÚNG cho ngôn ngữ **RTL** (Right-To-Left — như tiếng Ả Rập,
  Hebrew)

#### **`ActionBarDrawerToggle`**

`ActionBarDrawerToggle` là **class tiện ích** giúp **kết nối `DrawerLayout` với `ActionBar`/`Toolbar`**, để:

- Vẽ **hamburger icon `☰`** đúng vị trí navigation icon trên **ActionBar/Toolbar**
- Lắng nghe **tap vào icon** để **mở/đóng drawer**
- Lắng nghe **trạng thái drawer** để **tự động chuyển icon** giữa **hamburger `☰`** và **back arrow `<-`** khi drawer mở/đóng.

```kotlin
val toggle = ActionBarDrawerToggle(
    this, drawerLayout, toolbar,
    R.string.nav_open, R.string.nav_close
)
drawerLayout.addDrawerListener(toggle)
toggle.syncState()  // Đồng bộ trạng thái icon NGAY LÚC KHỞI TẠO
                     // (quan trọng — nếu thiếu, icon có thể sai
                     // trạng thái ban đầu, ví dụ hiện mũi tên dù
                     // Drawer đang đóng)
```

### 3.3. **Xử lý sự kiện khi user chọn 1 item trong `NavigationView`**

**Note**: `NavigationView` không tự động xử lý **navigation** khi user chọn 1 item, mà chỉ **gửi callback** về cho **Activity/Fragment** để xử lý.

> _Để hỗ trợ navigation, cần tự code **xử lý navigation trong callback** `setNavigationItemSelectedListener()`._

```kotlin
private fun setupNavigationView() {
    // callback setNavigationItemSelectedListener()
    // để xử lý khi user chọn 1 item trong NavigationView
    binding.navigationView.setNavigationItemSelectedListener { menuItem -> // menuItem là item user vừa chọn

        // xử lý selected item
        when (menuItem.itemId) {
            R.id.nav_home -> {
                // navigation
                navigateToFragment(HomeFragment())
                menuItem.isChecked = true
            }
            R.id.nav_contacts -> {
                // navigation
                navigateToFragment(ContactListFragment())
                menuItem.isChecked = true
            }
            R.id.nav_favorites -> {
                // navigation
                navigateToFragment(FavoritesFragment())
                menuItem.isChecked = true
            }
            R.id.nav_settings -> {
                // navigation
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            R.id.nav_logout -> {
                // show Dialog
                showLogoutConfirmDialog()
            }
        }

        // Đóng Drawer SAU KHI xử lý xong — UX chuẩn:
        // chọn xong thì tự đóng lại, không cần user tự đóng
        binding.drawerLayout.closeDrawer(GravityCompat.START)

        // return true -> event đã được xử lý
        true
    }
}

// Tự định nghĩa navigation logic
private fun navigateToFragment(fragment: Fragment) {
    supportFragmentManager.beginTransaction()
        .replace(R.id.nav_host_fragment, fragment)
        .commit()
}
```

### 3.4. Full-setup `NavigationView` trong **Activity/Fragment**

```kotlin
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // drawerToggle
    private lateinit var drawerToggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupDrawerToggle()
        setupNavigationView()
        setupHeaderData()
    }

    // ── Bước 1 — Nối Toolbar với DrawerLayout ──────────────────
    private fun setupDrawerToggle() {
        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.drawer_open,   // content description — Accessibility
            R.string.drawer_close
        )

        // Đăng ký drawerToggle với DrawerLayout để lắng nghe sự kiện mở/đóng
        binding.drawerLayout.addDrawerListener(drawerToggle)

        drawerToggle.syncState()  // Đồng bộ icon Hamburger NGAY LÚC ĐẦU
    }

    // ── Bước 2 — Xử lý click item trong NavigationView ──────────
    private fun setupNavigationView() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    navigateToFragment(HomeFragment())
                    menuItem.isChecked = true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                R.id.nav_logout -> {
                    showLogoutConfirmDialog()
                }
            }

            // Đóng Drawer SAU KHI xử lý xong — UX chuẩn: chọn xong
            // thì tự đóng lại, không cần user tự đóng
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun navigateToFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    // ── Bước 3 — Đổ data thật vào Header ────────────────────────
    private fun setupHeaderData() {
        // take header view
        val headerView = binding.navigationView.getHeaderView(0)
        // take header's components
        val tvUserName = headerView.findViewById<TextView>(R.id.tvUserName) // username
        val tvUserEmail = headerView.findViewById<TextView>(R.id.tvUserEmail) // email

        // binding data
        tvUserName.text = "Nguyễn Văn A"       // Thực tế lấy từ ViewModel
        tvUserEmail.text = "a.nguyen@example.com"
    }

    private fun showLogoutConfirmDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc muốn đăng xuất?")
            .setPositiveButton("Đăng xuất") { _, _ -> /* logout logic */ }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // ── Bước 4 — Bấm nút Back HỆ THỐNG khi Drawer đang mở ───────
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // ── Bắt buộc — đồng bộ trạng thái icon Hamburger khi rotate ──
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Đồng bộ trạng thái icon Hamburger khi rotate
        drawerToggle.onConfigurationChanged(newConfig)
    }
}
```
