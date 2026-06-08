# **_Layout_**: `ConstraintLayout`

## 1. **`ConstrainLayout`**

**Quy tắc bắt buộc**: Mỗi `View` trong `ConstraintLayout` **phải có**:

- Ít nhất **1 constraint theo trục `ngang`** (`start`/`end`)
- Ít nhất **1 constraint theo trục dọc** (`top`/`bottom`).

**Vi phạm** quy tắc này → `View` sẽ bị **render tại vị trí `(0,0)`** — đây là bug rất phổ biến với người mới.

---

## 2. Cú pháp Constrain cơ bản:

```xml
app:layout_constraint{srcEdge}_to{dstEdge}Of="{target}"
```

Các constrain hay dùng:

```xml
<!-- constrain for top -->
app:layout_constraintTop_toTopOf
app:layout_constraintTop_toBottomOf

<!-- constrain for bottom -->
app:layout_constraintBottom_toBottomOf
app:layout_constraintBottom_toTopOf

<!-- constrain for start -->
app:layout_constraintStart_toStartOf
app:layout_constraintStart_toEndOf

<!-- constrain for end -->
app:layout_constraintEnd_toEndOf
app:layout_constraintEnd_toStartOf
```

---

## 3. `0dp`: Match constraint

| Giá trị        | `LinearLayout`                                 | `ConstraintLayout`                                                  |
| -------------- | ---------------------------------------------- | ------------------------------------------------------------------- |
| `0dp`          | Chỉ có ý nghĩa khi kết hợp với `layout_weight` | Luôn có ý nghĩa → `match_constraint` (_kéo dài theo **constrain**_) |
| `match_parent` | Chiếm toàn bộ parent                           | **KHÔNG** có ý nghĩa, được coi là `wrap_content`                    |

> _`0dp` chỉ có tác dụng với constrain 2 đầu tương ứng, nếu không, sẽ bị coi là `wrap_content`_

```xml
<Button
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toEndOf="parent" />
```

---

## 4. `bias`: Cân bằng vị trí

Khi một `View` có cả **2 constraint đối lập** (`top+bottom` hoặc `start+end`), **`ConstraintLayout` sẽ mặc định căn giữa** — giống như 2 lực kéo bằng nhau.

- `bias` mặc định là `0.5` → **căn giữa**
- Có thể điều chỉnh `bias` trong phạm vi `0.0` đến `1.0` để **đẩy về 1 phía**

```xml
<!-- Button ở giữa dọc — bias mặc định = 0.5 -->
<Button
    app:layout_constraintTop_toTopOf="parent"
    app:layout_constraintBottom_toBottomOf="parent" />

<!-- Điều chỉnh bias theo hướng -->
<Button
    app:layout_constraintVertical_bias="0.3"
    app:layout_constraintHorizontal_bias="0.0" />
```

---

## 5. `Guideline`— **Đường kẻ tham chiếu** ảo

`Guideline` là một **`View` đặc biệt**

- **không render** ra màn hình
- chỉ **tồn tại để làm điểm neo** cho các View khác.

```xml
<androidx.constraintlayout.widget.Guideline
    android:id="@+id/guideline"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    app:layout_constraintGuide_percent="0.5" />
```

Trong đó:

- `orientation` xác định hướng của `guideline` (dọc hoặc ngang)
- Có `2` cách **định vị Guideline**:
  - `app:layout_constraintGuide_percent="0.3"` — tại 30% chiều rộng/cao
  - `app:layout_constraintGuide_begin="100dp"` — cách đầu 100dp cố định

Sau đó, các `View` khác **neo** vào **`guideline`** này:

```xml
<Button
    app:layout_constraintEnd_toStartOf="@id/guideline" />
<!-- Button kết thúc đúng tại đường giữa màn hình -->
```

---

## 6. `Barrier` — Đường kẻ động

**`Barrier` khác `Guideline`** ở chỗ: **vị trí** của nó tự động **điều chỉnh theo kích thước của một nhóm `View`**.

**Bài toán thực tế**:

- Có 2 **label** (`tvName`, `tvEmail`), chiều rộng của chúng thay đổi theo nội dung.
- Bạn muốn một `EditText` luôn bắt đầu sau **label rộng hơn** trong 2 cái đó.

```xml
<androidx.constraintlayout.widget.Barrier
    android:id="@+id/barrier"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:barrierDirection="end"
    app:constraint_referenced_ids="tvName,tvEmail" />

<EditText
    app:layout_constraintStart_toEndOf="@id/barrier" />
```

> _`Barrier` tự dịch chuyển đến cạnh `end` của view rộng nhất trong `{tvName, tvEmail}`. **EditText** luôn nằm sau đó — dù nội dung label thay đổi_

---

## 7. **Chain** — Phân bố nhóm View

**Chain** là khi một **nhóm View** được `liên kết 2 chiều` với nhau theo một trục.

> _Không phải chỉ A neo vào B, mà `A ↔ B ↔ C` tạo thành chuỗi._

```xml
<!-- Button A <-> B
    -> end neo tới start của B
-->
<Button android:id="@+id/btnA"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toStartOf="@id/btnB" />

<!-- Button B : A <-> B <-> C
    -> start neo tới end của A
    -> end neo tới start của C
-->
<Button android:id="@+id/btnB"
    app:layout_constraintStart_toEndOf="@id/btnA"
    app:layout_constraintEnd_toStartOf="@id/btnC" />

<!-- Button C : B <-> C
    -> start neo tới end của B
    -> end neo tới parent
-->
<Button android:id="@+id/btnC"
    app:layout_constraintStart_toEndOf="@id/btnB"
    app:layout_constraintEnd_toEndOf="parent" />
```

**`3` kiểu phân bố Chain** — set trên **`view` đầu tiên** của chain:

```xml
app:layout_constraintHorizontal_chainStyle="spread"
```

- `spread` (_mặc định_): phân bố đều, **khoảng cách** giữa các view **bằng nhau**
- `spread_inside`: phân bố đều, nhưng khoảng cách 2 đầu (tới phần tử ngoài chain) **bằng 0**
- `packed`: các view **dồn sát vào nhau** (không khoảng cách), có thể điều chỉnh `bias` để đẩy về 1 phía

---

Demo: [LoginScreen](/src/app/src/main/res/layout/constrain_layout.xml)
