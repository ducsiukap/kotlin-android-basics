# **`FragmentManager` & `FragmentTransaction`**

## 1. **Container `Fragment`**

`Fragment` phải có nơi để hiển thị, thông thường được đặt trong một **container** như `FrameLayout`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <!-- Fragment container -->
    <FrameLayout
        android:id="@+id/fragmentContainer"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <!-- Bottom Navigation -->
    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottomNavigation"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:menu="@menu/bottom_nav_menu" />

</LinearLayout>
```

**Google** hiện nay khuyến khích dùng `FragmentContainerView` thay vì `FrameLayout` để làm container cho `Fragment` vì **nó được thiết kế riêng cho `Fragment`** và hỗ trợ tốt hơn cho các tính năng như **animation** và **transition**:

```xml
<androidx.fragment.app.FragmentContainerView
    android:id="@+id/fragmentContainer"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>
```

---

## 2. `FragmentManager`

`FragmentManager` là class chịu trách nhiệm **quản lý toàn bộ `Fragment`** trong một **`Activity` hoặc `Fragment` host** — bao gồm: _**thêm**, **xóa**, **thay thế**, **back stack** và **lưu trạng thái** của các `Fragment`_.

Mỗi **host** (`Activity`/`Fragment`) có một **`FragmentManager` riêng** để quản lý các `Fragment` con của nó.

```
Activity
├── supportFragmentManager ← quản lý Fragment trực tiếp của Activity
│ ├── HomeFragment
│ │ └── childFragmentManager ← quản lý Fragment con của HomeFragment
│ │ └── NestedFragment
│ └── ProfileFragment
```

### Có `2` loại `FragmentManager` chính:

1. Bên trong **`Activity`**:
   `Activity` sử dụng `supportFragmentManager` để quản lý các `Fragment` trực tiếp của `Activity`.

   ```kotlin
   // Trong Activity → quản lý Fragment cấp 1
   val fm = supportFragmentManager
   ```

2. Bên trong **`Fragment`**:
   Mỗi `Fragment` có:
   - Một `childFragmentManager` để quản lý các **`Fragment` con** của nó.
   - Một `parentFragmentManager` để truy cập **`FragmentManager` của host**

   ```kotlin
   // Trong Fragment

   // → quản lý Fragment con (nested)
   val fm = childFragmentManager
   // → truy cập FM của Activity host
   val fm = parentFragmentManager
   ```

---

## 3. `FragmentTransaction`

`FragmentTransaction` là một **tập hợp các THAO TÁC được THỰC HIỆN CÙNG NHAU** trên `Fragment` — như một "**unit of work**".

> _**Tất cả thao tác** trong một `transaction` được **commit cùng lúc**, đảm bảo tính `atomic`._

```kotlin
supportFragmentManager.beginTransaction() // start a transaction
    .add(R.id.container, HomeFragment()) // manipulate
    .commit() // commit the transaction (execute all operations together)
```

Các thao tác phổ biến trong `FragmentTransaction`:

### 3.1. **`add()` -Thêm `Fragment`**:

Thao tác　`add()` — thêm Fragment **chồng lên** Fragment đang có.

- Cả hai **cùng tồn tại** trong container.
- Fragment bên dưới **vẫn còn sống** nhưng **bị che khuất**.

```kotlin
supportFragmentManager.beginTransaction()
    .add( // Thêm HomeFragment vào container
        R.id.container,     // container ID
        HomeFragment(),     // Fragment instance
        "home_fragment"     // tag (optional) — để tìm lại Fragment sau
    ).commit()
```

### 3.2. **`replace()` - Thay thế `Fragment`**:

Thao tác `replace()` — **xóa** `Fragment` hiện tại và **thêm** `Fragment` mới vào container.

```kotlin
// Xóa Fragment hiện tại trong container, thay bằng Fragment mới
supportFragmentManager.beginTransaction()
    .replace(
        R.id.fragmentContainer,
        ProfileFragment(),
        "profile_fragment"
    ).commit()
```

> _**`Fragment` cũ** bị `onDestroyView` → `onDestroy` → `onDetach` (kết thúc vòng đời)_

> _**`Fragment` mới** bắt đầu vòng đời từ `onAttach` → `onCreate` → `onCreateView` → `onViewCreated` → `onStart` → `onResume` ._

### 3.3. **`remove()` - Xóa `Fragment`**:

Thao tác `remove()` — xóa `Fragment` khỏi container và kết thúc vòng đời của nó.

```kotlin
val fragment = supportFragmentManager.findFragmentByTag("home_fragment") // tìm Fragment theo tag
fragment?.let {
    // không null → xóa Fragment khỏi container
    supportFragmentManager.beginTransaction()
        .remove(it)
        .commit()
}
```

### 3.4. **`hide()`/`show()` - Ẩn/Hiện `Fragment`**:

Thao tác `hide`/`show` — Fragment **không bị destroy, chỉ `visibility = GONE/VISIBLE`**. Dùng khi muốn **giữ state** của Fragment và **switch nhanh** giữa các Fragment

```kotlin
// Ẩn Fragment (không destroy, chỉ ẩn View)
supportFragmentManager.beginTransaction()
    .hide(homeFragment)
    .commit()

// Hiện lại Fragment đã ẩn
supportFragmentManager.beginTransaction()
    .show(homeFragment)
    .commit()
```

### 3.5. **Back Stack Fragment - `addToBackStack()`**:

Thao tác `addToBackStack()` — thêm `FragmentTransaction` vào **back stack** để cho phép user **quay lại** `Fragment` trước đó bằng nút back.

> _Đây là thao tác **rất quan trọng** để đảm bảo **trải nghiệm người dùng mượt mà** khi điều hướng giữa các `Fragment` — cho phép user **quay lại** trạng thái trước đó thay vì bị thoát app đột ngột_

```kotlin
supportFragmentManager.beginTransaction()
    .replace(R.id.fragmentContainer, DetailFragment())
    .addToBackStack("detail")   // tên entry (optional, có thể null)
    .commit()
```

Mặc định, `FragmentTransaction` **không được thêm vào back stack**:

- **KHÔNG** có `addToBackStack()`: khi user ấn **back**, `Fragment` sẽ **không quay lại** mà sẽ **thoát Activity** (`Activity.finish()`) _do không có gì để quay lại_.
- **Có** `addToBackStack()`: khi user ấn **back**, **Transaction bị `reverse`**, `Fragment` sẽ **quay lại** `Fragment` trước đó trong back stack, nếu không còn `Fragment` nào nữa thì mới thoát Activity.

Có thể **log** back stack để debug:

```kotlin
val fm = supportFragmentManager
Log.d(
    "FragmentManager",
    "Back stack entry count: ${fm.backStackEntryCount}"
)
```

#### `addToBackStack(null)`.

Tham số `null` thường được dùng nhất trong `addToBackStack()` vì **tên entry không bắt buộc** và thường không cần thiết

```kotlin
supportFragmentManager.beginTransaction()
    .replace(R.id.fragmentContainer, DetailFragment())
    .addToBackStack(null)   // không đặt tên entry
    .commit()
```

Tuy nhiên, trong trường hợp bạn muốn **quản lý back stack phức tạp** hoặc **pop back stack đến một entry cụ thể**, có thể đặt tên entry để dễ dàng thao tác sau này:

```kotlin
.addToBackStack("DETAIL")   // đặt tên entry
```

#### **`popBackStack()` - Pop back stack manually**:

Thao tác `popBackStack()` — pop back stack **thủ công** để quay lại `Fragment` trước đó mà không cần user ấn nút back.

> _Có thể chỉ **định tên entry (dựa vào tên đã đặt khi thêm vào back stack)** để pop đến một điểm cụ thể trong back stack._

```kotlin
// Pop back stack thủ công
supportFragmentManager.popBackStack()

// Pop về một entry cụ thể
supportFragmentManager.popBackStack(
    "detail",

    FragmentManager.POP_BACK_STACK_INCLUSIVE
    // POP_BACK_STACK_INCLUSIVE: xóa cả entry "detail"
    // 0: chỉ xóa đến trước entry "detail"
)

// Số lượng entry trong back stack
val count = supportFragmentManager.backStackEntryCount
```

### 3.6. **`commit()` và các biến thể**:

```kotlin
// commit() — schedule transaction
// -> thực thi sau khi main thread idle
.commit()

// commitNow() — thực thi ngay lập tức, synchronous
// KHÔNG thể dùng với addToBackStack()
.commitNow()

// commitAllowingStateLoss() — như commit() nhưng không throw exception
// nếu Activity đã save state
// Dùng khi: thực hiện transaction trong callback async (network, timer)
.commitAllowingStateLoss()

// commitNowAllowingStateLoss()
.commitNowAllowingStateLoss()
```

#### `commit()` & `IllegalStateException` - `commitAllowingStateLoss()`:

Khi gọi `.commit()`, **Android** thương không execute ngay mà **đưa transaction vào hàng đợi**, sau đó `FragmentManager` sẽ thực hiện ở **vòng lặp UI tiếp theo**.

`IllegalStateException` thường xảy ra khi bạn cố gắng commit một transaction sau khi `Activity` đã **save instance state** (thường là sau `onSaveInstanceState()`)

> _**Note**: `onSaveInstanceState()` được gọi khi `Activity` vào **background** hoặc khi **configuration change** (xoay màn, multi-window) hoặc trong trường hợp **system chuẩn bị destroy Activity**._

Lúc này, nếu transaction được thực hiện, **Android** sẽ ném `IllegalStateException` để cảnh báo rằng **transaction có thể bị mất** do `Activity` đã save state.

```
IllegalStateException:
Can not perform this action after onSaveInstanceState
```

Ví dụ: `onSaveInstanceState()` đã lưu `HomeFragment`, nhưng ngay sau đó gọi:

```kotlin
.replace(Home, Detail)
.commit()
```

Nếu điều này được phép:

- `savedState`: đã lưu `HomeFragment`
- UI thực tế: `DetailFragment` đã hiển thị

Khi process bị kill và restore sẽ gặp trường hợp **mismatch** giữa `savedState` và UI thực tế.

**Scenario gây `IllegalStateException`**:

```kotlin
// Scenario gây IllegalStateException:
override fun onPause() {
    super.onPause()
    // Activity đang save state
    // commit() ở đây → IllegalStateException
    supportFragmentManager.beginTransaction()
        .replace(R.id.container, NewFragment())
        .commit()   // CRASH
}
```

Để fix, có thể dùng `commitAllowingStateLoss()` để cho phép commit transaction ngay cả khi `Activity` đã save state:

```kotlin
// Fix:
// thay .commit() thành .commitAllowingStateLoss()
.commitAllowingStateLoss()   // an toàn hơn trong async context

// nhưng 99% không nên dùng
// vì có thể gây mât transaction
// nếu Activity bị kill sau khi save state
```

hoặc có thể check `isStateSaved()` trước khi commit:

```kotlin
if (!supportFragmentManager.isStateSaved) {
    // .commit()
} else {
    // Activity đã save state → không commit để tránh IllegalStateException
}
```

hoặc, cách hiện đại hơn, dùng `lifecycleScope` để đảm bảo transaction chỉ được thực hiện khi `Activity` đang ở trạng thái **STARTED** hoặc **RESUMED**:

```kotlin
repeatOnLifecycle(
    Lifecycle.State.RESUMED
){
    // Chỉ thực hiện transaction khi Activity đang RESUMED
    supportFragmentManager.beginTransaction()
        .replace(R.id.container, NewFragment())
        .commit()
}
```

### 3.7. **`setReorderingAllowed(true)` - Tối ưu hóa transaction**:

```kotlin
supportFragmentManager.beginTransaction()
    .setReorderingAllowed(true)   // khuyến nghị luôn bật
    .replace(R.id.container, HomeFragment())
    .commit()
```

Phương thức `setReorderingAllowed(true)` **cho phép FragmentManager `tối ưu hóa` các thao tác trong transaction** — `reorder` và `combine` operations để tránh **intermediate state** không cần thiết. _Google khuyến nghị luôn bật khi dùng với `Navigation Component`_.

---

## 4. **Tìm kiếm `Fragment`**

Các phương thức `findFragmentById()` / `findFragmentByTag()` - cho phép tìm `Fragment` theo **ID / Tag** container:

```kotlin
// Tìm theo container ID
val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

// Tìm theo tag
val homeFragment = supportFragmentManager.findFragmentByTag("home_fragment")

// Cast về đúng kiểu
val homeFragment = supportFragmentManager
    .findFragmentByTag("home_fragment") as? HomeFragment
```

---

## 5. `Activity.onCreate()` vs `sfm.beginTransaction().add()`

Pattern quan trọng khi **add `Fragment` trong `onCreate()` của `Activity`**: chỉ add `Fragment` khi lần đầu tạo `Activity` (_khi `savedInstanceState == null`_)

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    // SAII — luôn add Fragment mỗi lần onCreate() được gọi
    // onCreate() được gọi lại khi xoay màn hình → Fragment bị add trùng
    supportFragmentManager.beginTransaction()
        .add(R.id.container, HomeFragment())
        .commit()

    // ĐÚNG — chỉ add Fragment khi chưa có (lần đầu tạo Activity)
    if (savedInstanceState == null) {
        supportFragmentManager.beginTransaction()
            .add(R.id.container, HomeFragment())
            .commit()
    }
}
```

- `savedInstanceState == null` → `Activity` được tạo lần đầu, **chưa có `Fragment`** nào.
- Khi xoay màn hình, `savedInstanceState != null` → **`FragmentManager` tự khôi phục `Fragment` cũ**, không cần add lại.

---

## 6. Example code: manipulate `Fragment` với `FragmentManager`:

```kotlin
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Giữ reference để hide/show
    private var homeFragment: HomeFragment? = null
    private var searchFragment: SearchFragment? = null
    private var profileFragment: ProfileFragment? = null

    // Track Fragment đang active
    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            setupFragments()
        } else {
            // Khôi phục reference sau khi Activity recreate
            restoreFragments()
        }

        setupBottomNavigation()
    }

    private fun setupFragments() {
        homeFragment   = HomeFragment()
        searchFragment = SearchFragment()
        profileFragment = ProfileFragment()

        // Add tất cả Fragment vào container
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, profileFragment!!, "profile")
            .add(R.id.fragmentContainer, searchFragment!!, "search")
            .add(R.id.fragmentContainer, homeFragment!!, "home")
            // Ẩn tất cả trừ home
            .hide(profileFragment!!)
            .hide(searchFragment!!)
            .commit()

        activeFragment = homeFragment
    }

    private fun restoreFragments() {
        // FragmentManager đã khôi phục Fragment — tìm lại bằng tag
        homeFragment    = supportFragmentManager.findFragmentByTag("home") as? HomeFragment
        searchFragment  = supportFragmentManager.findFragmentByTag("search") as? SearchFragment
        profileFragment = supportFragmentManager.findFragmentByTag("profile") as? ProfileFragment
        activeFragment  = homeFragment
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home    -> showFragment(homeFragment!!)
                R.id.nav_search  -> showFragment(searchFragment!!)
                R.id.nav_profile -> showFragment(profileFragment!!)
            }
            true
        }
    }

    private fun showFragment(fragment: Fragment) {
        if (fragment == activeFragment) return   // đã đang hiển thị

        supportFragmentManager.beginTransaction()
            .hide(activeFragment!!)
            .show(fragment)
            .commit()

        activeFragment = fragment
    }
}
```
