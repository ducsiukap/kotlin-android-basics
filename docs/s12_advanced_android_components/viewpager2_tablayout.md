# `ViewPager2` and `TabLayout`

## 1. `ViewPager2` — **Swipe** between pages

### 1.1. **What** is the `ViewPager2`?

`ViewPager2` là **component cho phép user vuốt** ngang (hoặc dọc) giữa các trang (page) **để chuyển trang**. Mỗi **page** có thể là một `Fragment` hoặc một `View`.

### 1.2. **Tại sao lại là `2` (_`ViewPager2`_) thay vì `ViewPager`?**

Với `ViewPager`, có **nhiều hạn chế**:

- Không hỗ trợ **vertical swipe** (vuốt dọc) hay **RTL layout** (Right-to-Left layout)
- **`DiffUtil`** không hoạt động, dẫn tới **khi dataset thay đổi**, phải gọi `notifyDataSetChanged()` không ổn định để **REBUILD TOÀN BỘ** các page.
- Khó dùng với `Fragment` mới (_Fragment 1.1+_)

Với `ViewPager2`, **tất cả những hạn chế trên đã được giải quyết**:

- Hỗ trợ **vertical swipe** và **RTL layout**
- Dùng `RecyclerView` làm **backbone**, nên **`DiffUtil` hoạt động tốt**, kể cả `notifyDataSetChanged()` cũng hoạt động ổn định hơn.
- `FragmentStateAdapter` mới, được **thiết kế lại** để **tương thích với Fragment 1.1+** và quản lý **`Fragment` lifecycle** đúng chuẩn.

> _`ViewPager2` là **phiên bản nâng cấp** của `ViewPager`, được **REBUILD HOÀN TOÀN** dựa trên `RecyclerView`_

### 1.3. **Usecases**

Thực tế, `ViewPager2` được dùng trong **nhiều case phổ biến**:

- **Onboarding screen**: màn hình giới thiệu app
- **Image slider**: trình chiếu ảnh
- **Tab layout**: hiển thị nhiều tab, mỗi tab là một page
- **Story** / **Reels**: cho phép **vertical swipe** giữa các story / reels
- **Calendar** view: hiển thị **month view** và **week view**, cho phép **vertical swipe** giữa các view
- ...

### 1.4. Setup `ViewPager2` XML

```xml
<androidx.viewpager2.widget.ViewPager2
    android:id="@+id/viewPager"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="horizontal" />  <!-- horizontal (default) hoặc vertical -->
```

Thuộc tính `android:orientation` quyết định **hướng vuốt**: `horizontal` (ngang) (_mặc định_) hoặc `vertical` (dọc).

### 1.5. **`Adapter`** - trái tim của `ViewPager2`

`ViewPager2` cần một **Adapter** để biết **hiển thị gì ở mỗi page**. <br/>
Có **2 loại Adapter**:

- `FragmentStateAdapter`: dùng khi mỗi page là một `Fragment`

  ```kotlin
  class OnboardingAdapter(
      fragment: Fragment   // hoặc FragmentActivity
  ) : FragmentStateAdapter(fragment) {

      private val pages = listOf(
          OnboardingPage1Fragment(),
          OnboardingPage2Fragment(),
          OnboardingPage3Fragment()
      )

      // Tổng số page
      override fun getItemCount(): Int = pages.size

      // Trả về Fragment cho mỗi position
      override fun createFragment(position: Int): Fragment = pages[position]
  }
  ```

- `RecyclerView.Adapter`: dùng khi mỗi page là một `View`

  ```kotlin
  class ImagePagerAdapter(
      private val images: List<String>
  ) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

      inner class ImageViewHolder(
          private val binding: ItemImagePageBinding
      ) : RecyclerView.ViewHolder(binding.root) {

          fun bind(imageUrl: String) {
              Glide.with(binding.root)
                  .load(imageUrl)
                  .centerCrop()
                  .into(binding.ivImage)
          }
      }

      override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
          val binding = ItemImagePageBinding.inflate(
              LayoutInflater.from(parent.context), parent, false
          )
          return ImageViewHolder(binding)
      }

      override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
          holder.bind(images[position])
      }

      override fun getItemCount() = images.size
  }
  ```

#### **Gắn `Adapter` vào `ViewPager2`**

```kotlin
// Trong Fragment/Activity
val adapter = OnboardingAdapter(this)
binding.viewPager.adapter = adapter
```

#### **Điều hướng `page` bằng code**

```kotlin
// Điều hướng trang bằng code
binding.viewPager.currentItem = 2         // nhảy đến page index 2
binding.viewPager.currentItem = binding.viewPager.currentItem + 1  // next page
```

### 1.6. Các **thuộc tính và API quan trọng** của `ViewPager2`

```kotlin
binding.viewPager.apply {

    // Số page được pre-load trước/sau page hiện tại
    // Mặc định = 1 (load thêm 1 page mỗi bên)
    offscreenPageLimit = 2

    // Tắt vuốt tay — chỉ navigate bằng code
    isUserInputEnabled = false

    // Lắng nghe sự kiện chuyển trang
    registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

        override fun onPageSelected(position: Int) {
            // Trang mới được chọn
            updateIndicator(position)
        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            // Đang vuốt — gọi liên tục trong quá trình scroll
        }

        override fun onPageScrollStateChanged(state: Int) {
            // SCROLL_STATE_IDLE     → đứng yên
            // SCROLL_STATE_DRAGGING → đang kéo
            // SCROLL_STATE_SETTLING → đang settle về vị trí
        }
    })
}
```

### 1.7. **`PageTransformer` — Animation khi chuyển trang**

```kotlin
// Zoom out animation khi chuyển trang
binding.viewPager.setPageTransformer { page, position ->
    // position: -1.0 (trái) → 0.0 (hiện tại) → 1.0 (phải)
    val scale = 0.85f + (1 - Math.abs(position)) * 0.15f
    page.scaleX = scale
    page.scaleY = scale
    page.alpha  = 0.5f + (1 - Math.abs(position)) * 0.5f
}

// Slide animation
binding.viewPager.setPageTransformer { page, position ->
    page.translationX = -position * page.width * 0.25f
}
```

---

## 2. `TabLayout` — **Hiển thị tab** cho `ViewPager2`

### 2.1. **What** is the `TabLayout`?

`TabLayout` là **component hiển thị thanh tab ngang** — **mỗi tab** tương ứng với **một page** trong `ViewPager2`. <br/>
User có thể:

- Tap vào tab
- Vuốt ViewPager2

để **chuyển page** — hai thao tác này được đồng bộ tự động.

### 2.2. Setup `TabLayout` XML

```xml
<com.google.android.material.tabs.TabLayout
    android:id="@+id/tabLayout"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:tabMode="fixed"
    app:tabGravity="fill"
    app:tabIndicatorColor="@color/primary"
    app:tabIndicatorFullWidth="true"
    app:tabSelectedTextColor="@color/primary"
    app:tabTextColor="@color/gray"
    app:tabIconTint="@color/tab_icon_selector"
    app:tabRippleColor="@color/ripple" />
```

Có **2 loại tab**:

- `app:tabMode="fixed"`: tab có **width cố định**, bằng nhau, phù hợp khi ít tab (2-4)

  ```xml
  app:tabMode="fixed"
  app:tabGravity="fill"    <!-- mỗi tab giãn ra để fill toàn bộ chiều rộng -->
  ```

  kết hợp với `app:tabGravity="fill"` để **mỗi tab giãn ra** và **fill toàn bộ chiều rộng**.

- `app:tabMode="scrollable"`: tab **có thể cuộn ngang**, phù hợp khi nhiều tab (5+)

  ```xml
  app:tabMode="scrollable"
  ```

### 2.3. **Kết hợp `TabLayout` với `ViewPager2` bằng `TabLayoutMediator`**

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <com.google.android.material.tabs.TabLayout
        android:id="@+id/tabLayout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:tabMode="fixed"
        app:tabGravity="fill" />

    <androidx.viewpager2.widget.ViewPager2
        android:id="@+id/viewPager"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

</LinearLayout>
```

`TabLayoutMediator` là **cầu nối** đồng bộ `TabLayout` và `ViewPager2`:

```kotlin
// Setup ViewPager2
val adapter = HomePagerAdapter(this)
binding.viewPager.adapter = adapter

// Kết nối TabLayout với ViewPager2
TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
    // Cấu hình từng tab theo position
    when (position) {
        0 -> {
            tab.text = "Tất cả"
            tab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_all)
        }
        1 -> {
            tab.text = "Yêu thích"
            tab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_favorite)
        }
        2 -> tab.text = "Lịch sử"
    }
}.attach()   // BẮT BUỘC gọi attach() để kích hoạt
```

### 2.4. **Custom tab layout** — Tạo tab **theo ý muốn**

Nếu tab mặc định không đủ — ví dụ muốn badge, layout phức tạp:

```kotlin
TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
    // Inflate custom layout cho tab
    val customTab = LayoutInflater.from(requireContext())
        .inflate(R.layout.item_custom_tab, null)

    // setup custom tab
    customTab.findViewById<TextView>(R.id.tvTabTitle).text = tabTitles[position]
    customTab.findViewById<ImageView>(R.id.ivTabIcon).setImageResource(tabIcons[position])

    // gắn custom view vào tab
    tab.customView = customTab
}.attach()
```

### 2.5. **`TabLayout` without `ViewPager2`**

Đôi khi **dùng TabLayout độc lập** — không kết hợp với `ViewPager2`, có thể:

- **Add tab manually**:

  ```kotlin
  // Thêm tab thủ công
  binding.tabLayout.apply {
      addTab(newTab().setText("Tab 1"))
      addTab(newTab().setText("Tab 2").setIcon(R.drawable.ic_home))
      addTab(newTab().setText("Tab 3"))
  }
  ```

- **Observe tab selection event**:

  ```kotlin
  // Lắng nghe khi user chọn tab
  binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
      override fun onTabSelected(tab: TabLayout.Tab) {
          when (tab.position) {
              0 -> showContent1()
              1 -> showContent2()
              2 -> showContent3()
          }
      }

      override fun onTabUnselected(tab: TabLayout.Tab) {}
      override fun onTabReselected(tab: TabLayout.Tab) {}
  })
  ```
