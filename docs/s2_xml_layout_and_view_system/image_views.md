# **_Image / Media_** views

```
View
└── ImageView
    └── ShapeableImageView   ← Material Design
    └── ImageButton          ← đã học ở nhóm Button
```

## 1. **`ImageView`**

`ImageView` là **View hiển thị ảnh tĩnh** — từ _resource drawable_, _bitmap trong RAM_, hoặc _URL_ (thông qua thư viện như `Glide`). Đây là View được dùng nhiều thứ hai sau TextView trong hầu hết mọi app.

### 1.1. `src` vs `background`

```xml
android:src="@drawable/ic_logo"         <!-- ảnh NỘI DUNG của ImageView -->
android:background="@drawable/ic_logo"  <!-- ảnh NỀN của ImageView -->
```

- `android:src`: ảnh được scale theo `scaleType`, tôn trọng tỷ lệ gốc
- `android:background`: ảnh được kéo giãn để lấp đầy toàn bộ View, bỏ qua tỉ lệ

> _Quy tắc: **Luôn dùng `android:src` cho `ImageView`**. `android:background` chỉ dùng khi bạn thực sự muốn ảnh nền kéo giãn toàn bộ View_.

### 1.2. `scaleType`

`scaleType` kiểm soát **cách ảnh được scale và căn chỉnh** bên trong vùng của `ImageView` khi kích thước ảnh và kích thước `ImageView` **không khớp** nhau.

```xml
<ImageView
    android:layout_width="200dp"
    android:layout_height="200dp"
    android:src="@drawable/photo"
    android:scaleType="centerCrop" />
```

Có **`8` giá trị `scaleType`**, nhưng hầu hết sử dụng **`5` type chính**:

- `centerCrop`: Scale ảnh để **lấp đầy toàn bộ ImageView**, giữ tỷ lệ, **crop phần thừa** ra ngoài.<br/>
  > _used for: `thumbnail`, `avater`, `cover photo`, .. — khi muốn ImageView luôn được lấp đầy._
- `centerInside`: Scale ảnh để **vừa khít bên trong ImageView**, giữ tỷ lệ, **không crop**, **có thể có khoảng trống**.<br/>
  > _used for: `logo`, `icon`, ... — khi muốn thấy toàn bộ ảnh không bị crop._
- `fitXY`: Scale ảnh theo **2 chiều** để **lấp đầy toàn bộ ImageView**, bỏ qua tỷ lệ, **có thể bị méo**.<br/>
  > _used for: `background`, ... — khi muốn ảnh lấp đầy toàn bộ View, không quan tâm đến tỷ lệ._
- `fitCenter` (**default**): Scale ảnh theo 1 chiều để **vừa khít bên trong ImageView**, giữ tỷ lệ, **không crop**, **căn giữa**, có khoảng trống.<br/>
- `center` — Không scale, chỉ căn giữa. Nếu ảnh **lớn hơn** ImageView → bị **crop**. Nếu **nhỏ hơn** → có **khoảng trống**.

### 1.3. **`ImageView`** props

Các thuộc tính quan trọng của `ImageView`:

- `android:src`: **Nguồn ảnh** (drawable, bitmap, URL)
- `android:scaleType`: Cách **scale và căn chỉnh** ảnh
- `android:contentDescription`: Mô tả ảnh cho **Accessibility** (_bắt buộc_)
- `android:tint`: **Màu phủ** lên ảnh (dùng cho **icon** đơn sắc)
- `android:adjustViewBounds`: Cho phép ImageView **điều chỉnh kích thước** để giữ tỷ lệ ảnh khi `wrap_content`
- `android:alpha`: **Độ mờ** của ảnh (0.0 - 1.0)
- `android:elevation`: Độ cao để tạo **bóng đổ**, eg: `4dp` (Material Design, API 21+)
- `android:clipToOutline`: Cho phép ảnh **bị cắt theo outline** của View (eg: `shapeAppearance` của `MaterialShapeDrawable`)

```xml
<ImageView
    android:id="@+id/ivPhoto"
    android:layout_width="200dp"
    android:layout_height="200dp"
    android:src="@drawable/photo"
    android:scaleType="centerCrop"
    android:contentDescription="@string/photo_desc"   <!-- accessibility, bắt buộc -->
    android:adjustViewBounds="true"                    <!-- tự adjust bounds theo tỷ lệ ảnh -->
    android:tint="@color/primary"                      <!-- tô màu lên ảnh (dùng cho icon) -->
    android:alpha="0.8"                                <!-- độ trong suốt 0.0–1.0 -->
    android:elevation="4dp"                            <!-- shadow (API 21+) -->
    android:clipToOutline="true" />                    <!-- clip theo outline (dùng với bo góc) -->
```

> _`adjustViewBounds` — Khi set `true` và `layout_width` hoặc `layout_height` là `wrap_content`, `ImageView` sẽ **tự điều chỉnh kích thước** của mình để **giữ đúng tỷ lệ** của ảnh._

```xml
<!-- ImageView full width, height tự động theo tỷ lệ ảnh -->
<ImageView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:adjustViewBounds="true"
    android:scaleType="fitCenter"
    android:src="@drawable/banner" />
```

### 1.4. Thao tác với `ImageView`

```kotlin
// Set ảnh từ resource
binding.ivPhoto.setImageResource(R.drawable.photo)

// Set ảnh từ Drawable object
val drawable = ContextCompat.getDrawable(this, R.drawable.photo)
binding.ivPhoto.setImageDrawable(drawable)

// Set ảnh từ Bitmap
binding.ivPhoto.setImageBitmap(bitmap)

// Set ảnh từ URI
binding.ivPhoto.setImageURI(uri)

// Tint — tô màu (thường dùng cho icon vector)
binding.ivIcon.imageTintList = ColorStateList.valueOf(
    ContextCompat.getColor(this, R.color.primary)
)

// Xóa ảnh
binding.ivPhoto.setImageDrawable(null)

// Load ảnh từ URL — PHẢI dùng thư viện (Glide, Picasso, Coil)
// Không thể load URL trực tiếp bằng setImageURI trên main thread
```

### 1.5. Load ảnh từ URL với `Glide`

> _**Note**: `ImageView` thuần **không thể tự load ảnh** từ internet, cần thư viện. `Glide` là lựa chọn phổ biến nhất hiện nay._

**Dependency**:

```kotlin
// build.gradle.kts
implementation("com.github.bumptech.glide:glide:4.16.0")
```

**Sử dụng**:

- Load cơ bản:

  ```kotlin
  // Load cơ bản
  Glide.with(context)
      .load("https://example.com/photo.jpg")
      .into(binding.ivPhoto) // into -> target ImageView
  ```

- Load kèm **placeholder** (loading) và **error** (load failed)

  ```kotlin
  // Load với placeholder và error
  Glide.with(context)
      .load(imageUrl)
      .placeholder(R.drawable.ic_placeholder)   // hiện trong khi load
      .error(R.drawable.ic_error)               // hiện khi load thất bại
      .into(binding.ivPhoto)
  ```

- **Load** + **Transform** ảnh:

  ```kotlin
  // Load với transform — bo góc
  Glide.with(context)
      .load(imageUrl)
      .transform(RoundedCorners(16)) // sử dụng transform()
                                     // để transform ảnh
      .placeholder(R.drawable.ic_placeholder)
      .into(binding.ivPhoto)

  // Load ảnh tròn — dùng cho avatar
  Glide.with(context)
      .load(avatarUrl)

      .circleCrop() // circleCrop() -> bo tròn ảnh

      .placeholder(R.drawable.ic_avatar_default)
      .into(binding.ivAvatar)

  // Load với custom size
  Glide.with(context)
      .load(imageUrl)
      .override(200, 200)   // override()
                            // resize trước khi cache
      .centerCrop()
      .into(binding.ivPhoto)
  ```

> _Lưu ý quan trọng về **lifecycle**: **Luôn truyền context phù hợp vào `Glide.with()`**. Trong `Fragment`, dùng `viewLifecycleOwner` hoặc `requireContext()`. <br/>
> **Tránh** dùng `applicationContext` cho UI — **Glide sẽ không tự dừng load khi Fragment bị destroy**._

---

## 2. `ShapeableImageView`

**Vai trò**: `ShapeableImageView` là **subclass** của `ImageView` từ **Material Design library**. <br/>
`ShapeableImageView` bổ sung thêm **khả năng định hình** (shape) cho `ImageView` — _bo góc_, _cắt hình tròn_, _hình thoi_ — mà **không cần code** Kotlin, **chỉ dùng XML**.

Một số thuộc tính quan trọng:

- `app:shapeAppearanceOverlay`: **Áp dụng style** shape đã định nghĩa sẵn (eg: bo góc, cắt tròn)
- `app:strokeColor`: **Màu viền** ngoài của shape
- `app:strokeWidth`: **Độ dày viền** ngoài của shape

Các **shape** phổ biến:

1. **Rounded corners - bo góc tròn:**

   ```xml
   <com.google.android.material.imageview.ShapeableImageView
       android:id="@+id/sivPhoto"
       android:layout_width="120dp"
       android:layout_height="120dp"
       android:src="@drawable/photo"
       android:scaleType="centerCrop"
       android:padding="2dp"                  <!-- padding để tránh ảnh bị clip sát viền -->
       app:shapeAppearanceOverlay="@style/ShapeAppearance.Rounded" />
   ```

   Định nghĩa `ShapeAppearance.Rounded` trong `res/values/styles.xml`:

   ```xml
   <style name="ShapeAppearance.Rounded" parent="">
       <item name="cornerFamily">rounded</item>
       <item name="cornerSize">16dp</item>     <!-- bo 4 góc 16dp -->
   </style>
   ```

2. **Circle crop - cắt hình tròn:**

   Định nghĩa `style`

   ```xml
   <!-- styles.xml -->
   <style name="ShapeAppearance.Circle" parent="">
       <item name="cornerFamily">rounded</item>
       <item name="cornerSize">50%</item>      <!-- 50% = hình tròn hoàn hảo -->
   </style>
   ```

   Áp dụng:

   ```xml
   <com.google.android.material.imageview.ShapeableImageView
       android:id="@+id/sivAvatar"
       android:layout_width="80dp"
       android:layout_height="80dp"
       android:src="@drawable/avatar"
       android:scaleType="centerCrop"
       android:padding="2dp"
       app:shapeAppearanceOverlay="@style/ShapeAppearance.Circle"
       app:strokeColor="@color/primary"       <!-- viền ngoài -->
       app:strokeWidth="2dp" />               <!-- độ dày viền -->
   ```

3. **Cut corner - Hình thoi:**

   Định nghĩa `style`:

   ```xml
   <!-- styles.xml -->
   <style name="ShapeAppearance.Cut" parent="">
       <item name="cornerFamily">cut</item>    <!-- cut thay vì rounded -->
       <item name="cornerSize">16dp</item>
   </style>
   ```

### So sánh cách tạo **Circle Image**

| Cách                           | Ưu điểm             | Nhược điểm            |
| ------------------------------ | ------------------- | --------------------- |
| `ShapeableImageView`           | Linh hoạt, Material | Cần **padding**       |
| `Glide.circleCrop()`           | Đơn giản, 1 dòng    | Chỉ dùng khi load URL |
| `ShapeableImageView` + `Glide` | Tốt nhất, kết hợp   | Cần cả hai setup      |

ex: kết hợp `ShapeableImageView` + `Glide`

- `ShapeableImageView`
  ```xml
  <com.google.android.material.imageview.ShapeableImageView
      android:id="@+id/sivAvatar"
      android:layout_width="56dp"
      android:layout_height="56dp"
      android:scaleType="centerCrop"
      android:padding="1dp"
      app:shapeAppearanceOverlay="@style/ShapeAppearance.Circle"
      app:strokeColor="@color/outline"
      app:strokeWidth="1dp" />
  ```
- `Glide`:

  ```kotlin
  Glide.with(this)
      .load(user.avatarUrl)
      .placeholder(R.drawable.ic_avatar_default)
      .error(R.drawable.ic_avatar_default)
      .into(binding.sivAvatar)
  // ShapeableImageView tự clip tròn — không cần circleCrop()
  ```

---

## 3. `VideoView`

`VideoView` là Widget tích hợp sẵn để **phát video đơn giản** — từ `file local` hoặc `URL`. Nó wrap `MediaPlayer` bên dưới.

```xml
<VideoView
    android:id="@+id/videoView"
    android:layout_width="match_parent"
    android:layout_height="200dp" />
```

Xử lý:

- Load và phát video:

  ```kotlin
  // Load và phát video từ URL
  val videoUri = Uri.parse("https://example.com/video.mp4")
  binding.videoView.setVideoURI(videoUri)

  // Load từ raw resource
  val videoUri = Uri.parse("android.resource://${packageName}/${R.raw.intro_video}")
  binding.videoView.setVideoURI(videoUri)

  // Thêm MediaController — nút play/pause/seek
  val mediaController = MediaController(this)
  mediaController.setAnchorView(binding.videoView)
  binding.videoView.setMediaController(mediaController)
  ```

- Callback:

  ```kotlin
  // Callback khi video sẵn sàng
  binding.videoView.setOnPreparedListener { mediaPlayer ->
      mediaPlayer.isLooping = true    // loop video
                                      // auto-restart
      binding.videoView.start()
  }

  // Callback khi video kết thúc
  binding.videoView.setOnCompletionListener {
      // video đã phát xong
  }

  // Callback khi có lỗi
  binding.videoView.setOnErrorListener { _, what, extra ->
      true   // true = error đã được xử lý
  }
  ```

- Control:

  ```kotlin
  // Control
  binding.videoView.start()
  binding.videoView.pause()
  binding.videoView.stopPlayback()
  binding.videoView.seekTo(5000)        // seek đến 5 giây (milliseconds)

  // Lưu và khôi phục vị trí khi xoay màn hình
  override fun onSaveInstanceState(outState: Bundle) {
      super.onSaveInstanceState(outState)
      outState.putInt("position", binding.videoView.currentPosition)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      savedInstanceState?.let {
          binding.videoView.seekTo(it.getInt("position"))
      }
  }
  ```

**Giới hạn của `VideoView`**: `VideoView` **phù hợp cho use case đơn giản**: _video ngắn_, _intro screen_, _demo_.

> _Với các yêu cầu phức tạp hơn, dùng `ExoPlayer` (thư viện của **Google**)_

```
VideoView      → Video đơn giản, không cần nhiều control
ExoPlayer      → Streaming, adaptive bitrate (HLS/DASH), playlist,
                 custom UI player, DRM — app thực tế production
```
