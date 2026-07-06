# **`ProgressBar` component**

## 1. **What** is the `ProgressBar` component?

`ProgressBar` là một **`View` hiển thị tiến trình** của một tác vụ đang chạy, được **nhúng trực tiếp trong XML layout**.

Có **2 loại `ProgressBar`**, bao gồm **circular** và **horizontal**.

### 1.1. **Circular `ProgressBar`**

Theo mặc định, `ProgressBar` sẽ hiển thị **circular** — Spinner tròn xoay.

```xml
<ProgressBar
    android:id="@+id/progressBar"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    style="?android:attr/progressBarStyle" />
```

Trong đó, `style="?android:attr/progressBarStyle"`: là **style mặc định** (_không cần khai báo lại_) của `ProgressBar`, hiển thị **circular**. Kích thước của `ProgressBar` được **tự động điều chỉnh** theo **style** khác nhau.

- `?android:attr/progressBarStyleSmall` — hiển thị **circular NHỎ** (_dùng trong item list, button_).
- `?android:attr/progressBarStyleLarge` — hiển thị **circular LỚN** (_dùng trong fullscreen loading, ..._)
- `?android:attr/progressBarStyle` — hiển thị **circular VỪA** (_mặc định_).

### 1.2. **Horizontal `ProgressBar`**

```xml
<ProgressBar
    android:id="@+id/progressBarDownload"
    style="?android:attr/progressBarStyleHorizontal"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:max="100"
    android:progress="0" />
```

Với **Horizontal `ProgressBar`**, tham số **`style` BẮT BUỘC phải khai báo tường minh** - `style="?android:attr/progressBarStyleHorizontal"`.

> _Nếu không, `ProgressBar` sẽ **mặc định hiển thị circular** dù có set `progress`._

- `android:max` — giá trị **TỐI ĐA** của `ProgressBar`, thường là 100 để tính theo %.
- `android:progress` — giá trị **ban đầu** / **HIỆN TẠI** của `ProgressBar`.

### 1.3. `isIndeterminate` property — Phân biệt **"không biết tiến trình"** và **"biết tiến trình"**

Thuộc tính `isIndeterminate` là **một trong những thuộc tính QUAN TRỌNG NHẤT** của `ProgressBar`. Nó sẽ **quyết định** xem `ProgressBar` cách hiển thị **circular**/**horizontal** như tế nào:

```kotlin
binding.progressBar.isIndeterminate = true
```

Với `isIndeterminate = true` — `ProgressBar` được hiểu là **KHÔNG biết tiến trình**:

- **Circular `ProgressBar`**: hiển thị **Spinner tròn xoay** vô định, không dừng, không biểu thị tiến trình.
- **Horizontal `ProgressBar`**: hiển thị **thanh ngang chạy liên tục**, không biểu thị %.

Với `isIndeterminate = false` — `ProgressBar` được hiểu là **BIẾT tiến trình**:

- **Circular `ProgressBar`** (_hiếm dùng ở dạng có %_).
- **Horizontal `ProgressBar`**: hiển thị **thanh ngang có % CHẠY THEO GIA TRỊ `progress`** được set, có ý nghĩa biểu thị tiến trình của task hiện tại.

### 1.4. **Update `progress`**

```kotlin
class DownloadFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.progressBarDownload.apply {
            isIndeterminate = false
            max = 100
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // update progress bar
                viewModel.downloadProgress.collect { percent ->
                    // cập nhật tiến trình hiện tại
                    binding.progressBarDownload.progress = percent
                }
            }
        }
    }
}
```

> _Điểm quan trọng: **`progress` KHÔNG tự động animate mượt khi set trực tiếp qua thuộc tính "progress"** — nó NHẢY THẲNG đến giá trị mới._ <br/>
> _Nếu **muốn animation mượt** (chạy từ từ), cần dùng `setProgress(value, animated: Boolean)` (**CHỈ CÓ ở `CircularProgressIndicator`/`LinearProgressIndicator`**, KHÔNG có ở ProgressBar cũ)_

```kotlin
// ViewModel — tính % dựa trên byte đã tải / tổng byte
class DownloadViewModel : ViewModel() {

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    fun startDownload() {
        viewModelScope.launch {
            repository.downloadFile { bytesDownloaded, totalBytes ->
                // tính % dựa trên byte đã tải / tổng byte
                val percent = ((bytesDownloaded.toFloat() / totalBytes) * 100).toInt()
                _downloadProgress.value = percent
            }
        }
    }
}
```

### 1.5. **Vị trí** đặt `ProgressBar` trong layout

```xml
<androidx.constraintlayout.widget.ConstraintLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- ProgressBar CHỒNG LÊN chính giữa màn hình, cùng vị trí
         với RecyclerView — 2 View "xếp lớp" nhau, ẩn/hiện thay
         phiên nhau qua isVisible -->
    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

Kỹ thuật: **đặt 2 `View` CÙNG VỊ TRÍ** (_cùng constraint_) và **toggle ẩn/hiện thay phiên nhau** qua `isVisible` (_hoặc `visibility`_).

---

## 2. `CircularProgressIndicator` & `LinearProgressIndicator` — **Material Design `ProgressBar`**

```xml
<com.google.android.material.progressindicator.CircularProgressIndicator
    android:id="@+id/progressIndicator"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:indicatorColor="@color/purple_500"
    app:trackThickness="4dp"
    app:indicatorSize="48dp" />
```

`CircularProgressIndicator`/`LinearProgressIndicator` cho phép Animation mượt khi update `progress` — **có thể dùng `setProgressCompat(value, animated: Boolean)`**:

```kotlin
// Animation MƯỢT khi đổi % — API RIÊNG chỉ có ở bản Material
binding.progressIndicator.setProgressCompat(75, true)
//                                              ▲
//                                    animated = true → chạy
//                                    mượt từ giá trị cũ đến 75,
//                                    KHÔNG nhảy giật cục
```

Ngoài ra, `CircularProgressIndicator`/`LinearProgressIndicator` còn **có thể customize**:

- `indicatorColor` — màu của thanh tiến trình, **phần đã chạy**
- `trackColor` — màu của nền, **phần chưa chạy**
- `trackThickness` — **độ dày** của thanh tiến trình

> _ProgressBar cũ **không tách bạch rõ ràng** như vậy, phải tùy biến qua Drawable phức tạp hơn nhiều_
