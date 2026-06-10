# **Back stack & tasks**

## 1. **Task**

**Task** là **tập hợp các `Activity`** mà user tương tác để **hoàn thành một mục tiêu**. Task được tổ chức theo cấu trúc **`stack` (LIFO — Last In First Out)** — gọi là **Back Stack**.

```
             TOP (foreground)
┌─────────────────────────┐
│     CartActivity        │  ← user đang thấy
├─────────────────────────┤
│  ProductDetailActivity  │  ← onStop, không visible
├─────────────────────────┤
│     MainActivity        │  ← onStop, không visible
└─────────────────────────┘
             BOTTOM
```

Khi nhấn **Back**, `CartActivity` sẽ bị `onDestroy()`, `ProductDetailActivity` sẽ trở thành foreground, được `onRestart()` -> `onStart()` -> `onResume()`.

---

## 2. `launchMode` — Kiểm soát cách Activity vào stack

Mặc định mỗi lần `startActivity()` → **tạo instance mới đẩy vào stack**. `launchMode` thay đổi behavior này:

```xml
<!-- Khai báo trong AndroidManifest.xml -->
<activity
    android:name=".MainActivity"
    android:launchMode="singleTop" />
```

Các `launchMode` phổ biến:

- `standard` (_mặc định_): Luôn tạo instance mới.

  ```
  Stack: [A, B]
  startActivity(B) →  [A, B, B]   ← B mới được tạo
  ```

- `singleTop`: Nếu instance đã ở top → reuse, không tạo mới.

  ```
  Stack: [A, B]
  startActivity(B) →  [A, B]      ← B đang ở top → không tạo mới
                                  onNewIntent() được gọi trên B hiện tại
  Stack: [A, B]
  startActivity(A) →  [A, B, A]   ← A không ở top → tạo mới bình thường
  ```

- `singleTask`: Nếu instance đã tồn tại trong stack → reuse, không tạo mới, và **clear** tất cả activity trên nó.

  ```
  Stack: [A, B, C]
  startActivity(A) →  [A]         ← B và C bị pop và destroy
                                  onNewIntent() được gọi trên A
  ```

- `singleInstance`: Tương tự `singleTask`, nhưng instance chạy trong là **task riêng biệt**.
  > _Dùng cho: màn hình đặc biệt cần tách biệt hoàn toàn (call screen, alarm...)._

---

## 3. `finish()` — Đóng Activity chủ động

```kotlin
// Đóng Activity hiện tại, quay về Activity trước trong stack
binding.btnBack.setOnClickListener {
    finish()
}

// Tương đương nhấn Back button
// onPause → onStop → onDestroy
```
