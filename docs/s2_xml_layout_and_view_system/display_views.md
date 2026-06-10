# **_Display_ / _Decoration_** views

## 1. **Empty `View`**

`View` là **base class** của toàn bộ hệ thống UI Android, nhưng khi **dùng trực tiếp** trong XML mà không kế thừa, nó chỉ là một **hình chữ nhật vô hình** — _không vẽ gì_, _không xử lý gì_. Dùng làm: `Divider`, `Spacer`, `Background`, ...

### 1.1. `View` as **Divider**

```xml
<!-- Divider ngang -->
<View
    android:layout_width="match_parent"
    android:layout_height="1dp"
    android:background="#E0E0E0"
    android:layout_marginVertical="8dp" />

<!-- Divider dọc — trong LinearLayout horizontal -->
<View
    android:layout_width="1dp"
    android:layout_height="match_parent"
    android:background="#E0E0E0"
    android:layout_marginHorizontal="8dp" />

<!-- Divider có indent — không chạm 2 cạnh -->
<View
    android:layout_width="match_parent"
    android:layout_height="1dp"
    android:background="#E0E0E0"
    android:layout_marginStart="72dp"    <!-- indent theo avatar width -->
    android:layout_marginEnd="16dp" />
```

### 1.2. `View` as **Spacer**

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView ... />
    <EditText ... />

    <!-- Spacer: đẩy Button xuống bottom -->
    <View
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <Button
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Submit" />

</LinearLayout>
```

### 1.3. `View` as **Overlay Background**

```xml
<!-- Overlay tối phủ lên ảnh -->
<FrameLayout
    android:layout_width="match_parent"
    android:layout_height="200dp">

    <ImageView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:scaleType="centerCrop"
        android:src="@drawable/photo" />

    <!-- 50% transparent black overlay -->
    <View
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#80000000" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:text="Text trên ảnh"
        android:textColor="#FFFFFF"
        android:textSize="20sp" />

</FrameLayout>
```

---

## 2. `Space` — **Spacer chuyên dụng**

`Space` là View đặc biệt **chỉ chiếm không gian**, _không vẽ gì — kể cả background_. Nhẹ hơn View trống vì bỏ qua bước `onDraw()`.

```xml
<!-- Khoảng trống cố định giữa hai View -->
<Space
    android:layout_width="match_parent"
    android:layout_height="24dp" />

<!-- Spacer linh hoạt với weight -->
<Space
    android:layout_width="0dp"
    android:layout_height="0dp"
    android:layout_weight="1" />
```

Thực tế: sự khác biệt **hiệu năng** giữa `View` và `Space` là **không đáng kể** trong hầu hết trường hợp. _Dùng cái nào cũng được_:

- `Space` thể hiện **intent rõ hơn**, không cần background.
- `View` linh hoạt hơn (có thể thêm background sau).

---

## 3. `MaterialCardView` / `CardView` — **Card / Container có border & shadow**

`CardView` (và **phiên bản Material: `MaterialCardView`**) là ViewGroup hiển thị nội dung bên trong một **card có bo góc** và **elevation (shadow)**.

> _Đây là container UI cực kỳ phổ biến trong Android — hầu hết mọi item trong list, dashboard, profile đều dùng card._

```xml
<!-- MaterialCardView — khuyến nghị thay vì CardView thuần -->
<com.google.android.material.card.MaterialCardView
    android:id="@+id/cardProduct"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="8dp"
    app:cardCornerRadius="12dp"           <!-- bo góc -->
    app:cardElevation="4dp"               <!-- shadow -->
    app:cardBackgroundColor="@color/white"
    app:strokeColor="@color/outline"      <!-- viền (optional) -->
    app:strokeWidth="1dp"
    app:rippleColor="@color/ripple">      <!-- ripple khi tap (nếu card clickable) -->

    <!-- Một View con duy nhất — thường là LinearLayout hoặc ConstraintLayout -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Tiêu đề card"
            android:textSize="16sp"
            android:textStyle="bold" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Mô tả ngắn về nội dung"
            android:textSize="14sp"
            android:layout_marginTop="4dp" />

    </LinearLayout>

</com.google.android.material.card.MaterialCardView>
```

Các `style` của `MaterialCardView`:

```xml
<!-- Elevated — có shadow, không viền (mặc định) -->
style="@style/Widget.Material3.CardView.Elevated"

<!-- Filled — có nền màu, không shadow -->
style="@style/Widget.Material3.CardView.Filled"

<!-- Outlined — có viền, không shadow -->
style="@style/Widget.Material3.CardView.Outlined"
```

### **Clickable Card**:

Để card có **hiệu ứng ripple** khi tap, cần đặt `android:clickable="true"` và `app:rippleColor` (nếu muốn custom màu ripple):

```xml
<com.google.android.material.card.MaterialCardView
    android:id="@+id/cardArticle"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:clickable="true"              <!-- bắt buộc để nhận click -->
    android:focusable="true"              <!-- bắt buộc để hiện ripple -->
    app:cardCornerRadius="12dp"
    app:cardElevation="2dp"
    app:rippleColor="@color/ripple">

    ...

</com.google.android.material.card.MaterialCardView>
```

Event listener:

```kotlin
binding.cardArticle.setOnClickListener {
    val intent = Intent(this, ArticleDetailActivity::class.java)
    intent.putExtra("article_id", article.id)
    startActivity(intent)
}
```

### **Checked state** — Card có thể chọn

`MaterialCardView` hỗ trợ **checked state tích hợp sẵn** — hữu ích cho **_multi-select_**:

```kotlin
// Toggle checked
binding.cardProduct.isChecked = true

// Lắng nghe
binding.cardProduct.setOnCheckedChangeListener { card, isChecked ->
    if (isChecked) {
        selectedItems.add(product)
    } else {
        selectedItems.remove(product)
    }
}
```

---

## 4. `Badge`

**Vai trò**: `Badge` là indicator **nhỏ hình tròn** hiển thị số **thông báo** hoặc **trạng thái** trên một `View` khác — thường là icon trong `BottomNavigationView` hoặc **toolbar icon**.

**Sử dụng với `BottomNavigationView`**:

```kotlin
// Thêm badge số vào tab của BottomNavigationView
val badge = binding.bottomNavigation.getOrCreateBadge(R.id.nav_notifications)
badge.number = 5                              // hiện số
badge.backgroundColor = ContextCompat.getColor(this, R.color.red)
badge.badgeTextColor = ContextCompat.getColor(this, R.color.white)

// Badge dot — không có số
val dotBadge = binding.bottomNavigation.getOrCreateBadge(R.id.nav_messages)
dotBadge.isVisible = true
// không set number → hiện dot

// Xóa badge
binding.bottomNavigation.removeBadge(R.id.nav_notifications)

// Ẩn badge
badge.isVisible = false
```

**`BadgeDrawable`**: sử dụng badge với bất kỳ View nào

```kotlin
// Gắn badge lên một View tùy ý (toolbar icon, ImageView...)
val badgeDrawable = BadgeDrawable.create(this)
badgeDrawable.number = 12
badgeDrawable.backgroundColor = ContextCompat.getColor(this, R.color.error)

// Cần ViewTreeObserver để gắn vào View sau khi layout xong
binding.ivNotification.viewTreeObserver.addOnGlobalLayoutListener {
    BadgeUtils.attachBadgeDrawable(
        badgeDrawable,
        binding.ivNotification
    )
}
```
