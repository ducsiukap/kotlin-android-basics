# **_Layout_**: `LinearLayout`

`LinearLayout` là `ViewGroup` **sắp xếp** các View con **tuần tự theo một hướng duy nhất** — hoặc dọc (`vertical`) hoặc ngang (`horizontal`). Đây là layout đơn giản nhất.

**Nguyên tắc hoạt động**: `LinearLayout` nhận lần lượt từng View con, đặt chúng **nối tiếp nhau theo hướng đã chỉ định**, không có View nào chồng lên View nào.

```
orientation="vertical"        orientation="horizontal"

┌──────────────┐              ┌────┬────┬────┐
│   View 1     │              │ V1 │ V2 │ V3 │
├──────────────┤              └────┴────┴────┘
│   View 2     │
├──────────────┤
│   View 3     │
└──────────────┘
```

**Demo:** [LinearLayout](/src/app/src/main/res/layout/linear_layout.xml).<br/>
Các thuộc tính quan trọng:

- Cho **ViewGroup** - `layout`:
  - `orientation`: hướng sắp xếp (dọc hoặc ngang).
  - `gravity`: căn chỉnh các View con trong layout.
  - `weightSum`: tổng trọng số của tất cả View con (nếu sử dụng `layout_weight`).
- Cho **View con**:
  - `layout_width` và `layout_height`: kích thước của View con.
  - `layout_weight`: trọng số để phân chia không gian còn lại giữa các View con, đặt chiều tương ứng (`width`/`height` = `0dp`) để tối ưu hiệu năng, tránh double measurement.
  - `layout_gravity`: căn chỉnh View con trong layout (ghi đè `gravity` của ViewGroup).
