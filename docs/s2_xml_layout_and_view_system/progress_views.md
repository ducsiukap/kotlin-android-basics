# **Progress** & **Loading** views

## 1. `ProgressBar`

**Vai trò**: `ProgressBar` hiển thị **tiến trình của một tác vụ**. Có hai chế độ hoàn toàn khác nhau về mặt ngữ nghĩa.

### 1.1. **Indeterminate** ProgressBar

**Indeterminate** `ProgressBar` được sử dụng cho các **task không đo lương được**: không biết _tiến độ hiện tại_, không biết _điểm kết thúc_, ...

**Usecases**: Gọi API, load dữ liệu lần đầu, xử lý file không rõ size, ...

Có `2` loại **Indeterminate** `ProgressBar`:

- **Circular** (_default_): là spinner tròn.

  ```xml
  <!-- Spinner tròn — mặc định khi không set style -->
  <ProgressBar
      android:id="@+id/progressBar"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:layout_gravity="center"
      android:visibility="gone" />
  ```

- **Horizontal**: là thanh ngang, cần set `style`:

  ```xml
  <!-- Thanh ngang indeterminate
    + style="@android:style/Widget.ProgressBar.Horizontal"
    + android:indeterminate="true"
  -->
  <ProgressBar
      android:id="@+id/progressBarHorizontal"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      style="@android:style/Widget.ProgressBar.Horizontal"
      android:indeterminate="true" />
  ```

### 1.2. **Determinate** ProgressBar

**Determinate** `ProgressBar` được sử dụng cho các **task đo lường được**: biết _tiến độ hiện tại_, biết _điểm kết thúc_, ...

**Usecases**: Download file, upload ảnh, cài đặt, import data, ...

```xml
<ProgressBar
    android:id="@+id/progressBarDownload"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    style="@android:style/Widget.ProgressBar.Horizontal"
    android:indeterminate="false"      <!-- tắt indeterminate -->
    android:max="100"                  <!-- giá trị tối đa -->
    android:progress="0"               <!-- giá trị hiện tại -->
    android:min="0" />                 <!-- giá trị tối thiểu (API 26+) -->
```

**Cập nhật tiến độ** của `ProgressBar`:

```kotlin
// Set progress trực tiếp (không có animation)
binding.progressBarDownload.progress = 75

// Set progress CÓ animation (API 24+)
binding.progressBarDownload.setProgress(75, true)

// Đọc giá trị hiện tại
val current = binding.progressBarDownload.progress
```

### 1.3. Kích thước của `ProgressBar`

```xml
<!-- Small -->
<ProgressBar
    style="@android:style/Widget.ProgressBar.Small"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content" />

<!-- Normal (mặc định) -->
<ProgressBar
    android:layout_width="wrap_content"
    android:layout_height="wrap_content" />

<!-- Large -->
<ProgressBar
    style="@android:style/Widget.ProgressBar.Large"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content" />
```

---

## 2. Material Progress Indicators

`CircularProgressIndicator` và `LinearProgressIndicator` là phiên bản **Material Design 3** của `ProgressBar` — đẹp hơn, nhiều option hơn, nhất quán với Material theme. Đây là lựa chọn được khuyến nghị trong dự án hiện đại.

### 2.1. `CircularProgressIndicator`

```xml
<!-- Indeterminate — spinner quay -->
<com.google.android.material.progressindicator.CircularProgressIndicator
    android:id="@+id/circularProgress"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:indeterminate="true"
    app:indicatorSize="48dp"           <!-- đường kính vòng tròn -->
    app:trackThickness="4dp"           <!-- độ dày của track -->
    app:indicatorColor="@color/primary" />

<!-- Determinate — hiện % -->
<com.google.android.material.progressindicator.CircularProgressIndicator
    android:id="@+id/circularProgressDeterminate"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:indeterminate="false"
    android:max="100"
    android:progress="65"
    app:indicatorSize="80dp"
    app:trackThickness="8dp"
    app:indicatorColor="@color/primary"
    app:trackColor="@color/surface_variant" />  <!-- màu phần chưa load -->
```

### 2.2. `LinearProgressIndicator`

```xml
<!-- Indeterminate — thanh chạy qua lại -->
<com.google.android.material.progressindicator.LinearProgressIndicator
    android:id="@+id/linearProgress"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:indeterminate="true"
    app:trackThickness="4dp"
    app:indicatorColor="@color/primary"
    app:trackColor="@color/surface_variant" />

<!-- Determinate -->
<com.google.android.material.progressindicator.LinearProgressIndicator
    android:id="@+id/linearProgressDeterminate"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:indeterminate="false"
    android:max="100"
    android:progress="40"
    app:trackThickness="8dp"
    app:trackCornerRadius="4dp"        <!-- bo góc đầu thanh -->
    app:indicatorColor="@color/primary"
    app:trackColor="@color/surface_variant" />
```

### 2.3. Xử lý tiến độ `ProgressBar`

Tương tự `ProgressBar` thuần:

```kotlin
// Cập nhật progress có animation
binding.linearProgressDeterminate.setProgressCompat(75, true)

// Switch giữa indeterminate và determinate
binding.linearProgress.isIndeterminate = false
binding.linearProgress.setProgressCompat(50, true)
```

---

## 3. `SeekBar`

`SeekBar` là `ProgressBar` có thể **kéo để chọn giá trị** — user tương tác trực tiếp. <br/>
**Usecase** điển hình: `volume`, `brightness`, video `timeline`, `filter` giá, ....

```xml
<SeekBar
    android:id="@+id/seekBarVolume"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:max="100"                  <!-- giá trị tối đa -->
    android:min="0"                    <!-- giá trị tối thiểu (API 26+) -->
    android:progress="50"              <!-- giá trị mặc định -->
    android:progressTint="@color/primary"   <!-- màu phần đã qua -->
    android:thumbTint="@color/primary"      <!-- màu thumb (●) -->
    android:trackTint="@color/surface_variant" />  <!-- màu track -->
```

Event handlers for `SeekBar`:

```kotlin
// SeekBar listener
binding.seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        // Gọi liên tục khi đang kéo
        // fromUser = true nếu do user kéo, false nếu do code set
        binding.tvVolume.text = "$progress%"
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        // User bắt đầu chạm vào thumb
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        // User nhả tay — gọi 1 lần khi xong
        // Nên làm việc nặng ở đây thay vì onProgressChanged
        saveVolumeSetting(seekBar?.progress ?: 0)
    }
})
```

Một số loại **`SeekBar`** khác:

1. `DiscreteSeekBar`: chỉ cho phép chọn giá trị rời rạc (**Material Slider**), từ `valueFrom` đến `valueTo` với bước nhảy `stepSize`.

   ```xml
   <com.google.android.material.slider.Slider
       android:id="@+id/sliderRating"
       android:layout_width="match_parent"
       android:layout_height="wrap_content"
       android:valueFrom="0"
       android:valueTo="10"
       android:stepSize="1"               <!-- bước nhảy = 1 → chỉ chọn 0,1,2...10 -->
       android:value="5"
       app:labelBehavior="floating" />    <!-- label nổi hiện giá trị khi kéo -->
   ```

   Event handler:

   ```kotlin
   // Material Slider listener
   binding.sliderRating.addOnChangeListener { slider, value, fromUser ->
       binding.tvRating.text = value.toInt().toString()
   }
   ```

2. `RangeSlider`: cho phép chọn **khoảng giá trị** (min-max) thay vì một giá trị duy nhất.

   ```xml
   <com.google.android.material.slider.RangeSlider
       android:id="@+id/rangeSliderPrice"
       android:layout_width="match_parent"
       android:layout_height="wrap_content"
       android:valueFrom="0"
       android:valueTo="10000000"
       app:values="@array/price_range_default"   <!-- [1000000, 5000000] -->
       app:stepSize="500000"
       app:labelBehavior="floating" />
   ```

   Event handler:

   ```kotlin
   // RangeSlider
   binding.rangeSliderPrice.addOnChangeListener { slider, value, fromUser ->
       val values = slider.values   // List<Float> [minValue, maxValue]
       val min = values[0].toInt()
       val max = values[1].toInt()
       binding.tvPriceRange.text = "${formatPrice(min)} – ${formatPrice(max)}"
   }
   ```

---

## 4. `RatingBar`

`RatingBar` hiển thị và cho phép user chọn **đánh giá dạng sao ⭐**. <br/>
**Use case**: đánh giá sản phẩm, review app, feedback.

Có 2 loại `RatingBar`:

- **Interactive** `RatingBar`: user **có thể tương tác**, chạm để chọn số sao.

  ```xml
  <!-- Interactive RatingBar — user có thể tap để đánh giá -->
  <RatingBar
      android:id="@+id/ratingBar"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:numStars="5"               <!-- số sao tối đa -->
      android:rating="3.5"               <!-- giá trị mặc định -->
      android:stepSize="0.5"             <!-- bước nhảy: 1.0 = sao nguyên, 0.5 = nửa sao -->
      style="@style/Widget.AppCompat.RatingBar" />
  ```

- **Indicator** `RatingBar`: chỉ để hiển thị, user **không thể tương tác**, chỉ định bằng cách set `android:isIndicator="true"`.

  ```xml
  <!-- Indicator — chỉ hiển thị, không tương tác -->
  <RatingBar
      android:id="@+id/ratingBarDisplay"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:numStars="5"
      android:rating="4.2"
      android:isIndicator="true"         <!-- chỉ đọc -->
      style="@style/Widget.AppCompat.RatingBar.Small" />
  ```

**Styles**:

- `Widget.AppCompat.RatingBar` → kích thước bình thường
- `Widget.AppCompat.RatingBar.Small` → nhỏ, dùng trong list item
- `Widget.AppCompat.RatingBar.Indicator` → chỉ hiển thị (không tương tác)

**Event handler**:

```kotlin
// Lắng nghe thay đổi
binding.ratingBar.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
    if (fromUser) {
        binding.tvRatingValue.text = "$rating / 5"
        submitRating(rating)
    }
}

// Đọc giá trị
val currentRating = binding.ratingBar.rating   // Float

// Set giá trị
binding.ratingBarDisplay.rating = 4.5f
```
