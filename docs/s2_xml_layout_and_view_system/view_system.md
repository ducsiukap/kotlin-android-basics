# **View system**

## 1. View system

**View System** là hệ thống **UI** truyền thống của **Android**, tồn tại từ `API 1`.<br/>
Toàn bộ giao diện người dùng trong Android (theo hướng `XML`) được xây dựng từ hai khái niệm cốt lõi: `View` và `ViewGroup`.

Mô hình tổng quát:

```
View hierarchy
│
└── ViewGroup
    ├── View
    ├── View
    └── ViewGroup
        ├── View
        └── View
```

---

## 2. `View` - **UI unit** cơ bản

`View` là **đơn vị UI cơ bản nhất** trong View System:

- Đại diện cho một phần tử giao diện người dùng (ví dụ: `TextView`, `Button`, `ImageView`), chịu trách nhiệm **vẽ nội dung** và **xử lý tương tác người dùng**.
- Là 1 vùng **hình chữ nhật** trên màn hình, được xác định bởi vị trí `(x, y)` và kích thước `(width, height)`.
  > _`View` biết cách tự vẽ mình lên `Canvas` và biết cách phản hồi sự kiện chạm._

**Mọi widget** bạn nhìn thấy trên màn hình đều là một `View` hoặc **kế thừa** từ `View` như: `TextView`, `ImageView`, `Button`, `EditText`, `CheckBox`, `RadioButton`, `Switch`, `ProgressBar`, v.v.

---

## 3. `ViewGroup` - **Container** của `View`

**`ViewGroup`** cũng **là một `View`**, nhưng có thêm khả năng **chứa** và **sắp xếp** các **`View` con** bên trong nó.<br/>
`ViewGroup` chính là các `Layout`.

- `ViewGroup` **extends** `View`
- `ViewGroup` là các **Layout**:
  - `LinearLayout`: Sắp **xếp con theo hàng** hoặc **cột**.
  - `RelativeLayout`: Sắp xếp con **tương đối** với nhau.
  - `ConstraintLayout`: Sắp xếp con **theo ràng buộc**.
  - `FrameLayout`: Chứa con **chồng lên nhau**.
  - `RecyclerView`: Hiển thị danh sách con **có thể cuộn**.

---

## 4. View **hierarchy** - Cây UI

Toàn bộ UI của một màn hình được **tổ chức thành cây phân cấp** (`tree`) — gọi là **View Hierarchy**:<br/>
eg:

```
ConstraintLayout (root ViewGroup)
├── LinearLayout (ViewGroup)
│   ├── TextView ("Đăng nhập")
│   └── ImageView (logo)
├── EditText (username)
├── EditText (password)
└── Button ("Login")
```

Khi Android **render** màn hình, nó duyệt cây này **từ gốc xuống** lá theo **`3` bước**:

- **Measure** — tính toán **kích thước** mỗi View
- **Layout** — xác định **vị trí** mỗi View
- **Draw** — **vẽ** lên màn hình

> _**Hệ quả** thực tế: **View Hierarchy càng sâu** (nhiều tầng lồng nhau) → **render càng chậm**.
> Đây là lý do `ConstraintLayout` ra đời — cho phép **xây dựng UI phức tạp với hierarchy phẳng** (ít tầng lồng nhau nhất có thể)._

---

## 5. File XML `layout`

UI được khai báo trong file `.xml` đặt tại `res/layout/`.
Android đọc file XML này, tạo ra các đối tượng `View`/`ViewGroup` tương ứng trong bộ nhớ, và hiển thị lên màn hình.
Quá trình chuyển đổi **XML** → **View object** gọi là `inflation`.

`res/layout/activity_main.xml` → **LayoutInflater** → View objects trong RAM
