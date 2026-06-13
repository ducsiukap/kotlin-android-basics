# Intent **_flags_**

## 1. **Task & Back stack** ([_remind_](../s4_activities/back_stack.md))

**Task** là **tập hợp các `Activity`** mà user tương tác để **hoàn thành một mục tiêu**.

**Note**:

1. _`Task` là **luồng công việc mà user đang trải nghiệm**_, bao gồm tất cả các `Activity` mà user đã mở để hoàn thành một mục tiêu cụ thể.
2. `Task` có thể chứa một hoặc nhiều `Activity`
3. Các `Activity` trong một `Task` **không nhất thiết phải thuộc cùng một ứng dụng**. Một `Task` có thể chứa `Activity` từ nhiều ứng dụng khác nhau, miễn là chúng liên quan đến cùng một mục tiêu của user.

> Ex: người dùng đang ở **Gmail** và _click mở link_ → mở **Browser** (giả sử là `BrowserActivity` của Chrome). Khi này, **`BrowserActivity` sẽ được push vào cùng Task hiện tại**

Task được tổ chức theo cấu trúc **`stack` (LIFO — Last In First Out)** — gọi là **Back Stack**.

> Nhấn **Back** từ `BrowserActivity` sẽ quay lại `EmailActivity` — _**như thể chúng thuộc cùng một luồng**_.

> **Với `User`: đây gọi là _CÙNG MỘT MỤC TIÊU_ / _CÙNG MỘT LUỒNG TRẢI NGHIỆM_**

**Note**:

- Mặc định: hầu hết `Activity` trong 1 `Task` đều thuộc cùng một ứng dụng.
- Ngoại lệ: với **Implicit Intent**, `Activity` của app khác được join vào `Task`.
- **Recent Apps**: hiển thị **theo `Task`**, không phải theo App
  > _Một App có thể xuất hiện nhiều card_

---

## 2. `launchMode`

`launchMode` là thuộc tính được sử dụng khi khai báo `Activity` trong **Manifest**.

```xml
<activity
    android:name=".MainActivity"
    android:launchMode="standard" />
```

`launchMode` xác định cách thức `Activity` được khởi tạo và quản lý trong **`Task` nào** và sắp xếp trong **Back Stack** ra sao khi được gọi.

Các **giá trị** của `launchMode`:

| `launchMode`            | Mô tả                                                                                                 |
| ----------------------- | ----------------------------------------------------------------------------------------------------- |
| `standard` (_mặc định_) | Tạo instance mới của `Activity` mỗi lần gọi                                                           |
| `singleTop`             | Nếu `Activity` đã ở **`top`** của Back Stack, **không tạo instance mới**. Nếu không, giống `standard` |
| `singleTask`            | Chỉ **một instance duy nhất** của App được tồn tại, nằm ở `root` của **Task riêng**                   |
| `singleInstance`        | Tương tự `singleTask`, nhưng **không cho phép bất kỳ `Activity` nào khác** được đặt trong cùng Task   |

### launchMode=`standard`

**Quy tắc**:

- Luôn tạo **instance mới** ở mỗi lần gọi, `onCreate` luôn được gọi.
- Luôn **push vào Task của caller** (_`Activity` gọi nó_)
- **_Mặc định_** nếu không khai báo `launchMode`

```
Stack: [A]
A gọi startActivity(B)  →  [A, B]
B gọi startActivity(B)  →  [A, B, B]   ← 2 instance B khác nhau
```

**Dùng cho**: Hầu hết Activity trong app — Detail screen, Edit screen, các màn hình mà mỗi lần mở là một **"phiên" độc lập**.

### launchMode=`singleTop`

**Quy tắc**: chỉ check liệu **`Activity` đích đã ở `top` của Task hiện tại chưa**:

- Nếu **instance** của `Activity` đích đã ở đỉnh (**top**) của **`Task` hiện tại** → **không tạo mới**, gọi `onNewIntent()` trên instance đó.
- Nếu không ở **top**, hoạt động giống như `standard` → tạo instance mới và push vào `Task` của caller.

```
Stack: [A, B]
B ở top, gọi startActivity(B)  →  [A, B]        ← reuse, onNewIntent()
                                                    KHÔNG tạo B mới

Stack: [A, B, C]
B KHÔNG ở top, gọi startActivity(B)  →  [A, B, C, B]   ← tạo mới (giống standard)
```

**Dùng cho**: `Notification` Activity, `Search` Activity — tránh việc tap notification nhiều lần tạo ra nhiều instance chồng lên nhau.

Trong trường hợp **reuse** instance, `Activity` đích nhận **`intent` mới** qua `onNewIntent()`:

```kotlin
// Activity nhận intent mới qua onNewIntent
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)   // cập nhật intent hiện tại
    val newId = intent.getStringExtra("notification_id")
    refreshContent(newId)
}
```

### launchMode=`singleTask`

**Quy tắc**:

- Đảm bảo **CHỈ MỘT instance** của `Activity` tồn tại trong **toàn hệ thống**.
- Instance này luôn nằm ở **`root` của một `Task` riêng biệt**.
- Khi `Activity` được gọi:
  - Nếu instance (Task) **`đã tồn tại`**:
    1.  Reuse: **Không tạo mới**, clear toàn bộ `Activity` phía trên trong **Task** của instance đó
    2.  Gọi `onNewIntent()` trên instance đó và **đưa Task đó lên foreground**.
  - Nếu Task **chưa tồn tại** → **tạo mới Task** và **đặt Activity làm root** của Task mới.

```
Trước: Task A = [MainActivity, DetailActivity, EditActivity]
MainActivity launchMode="singleTask"

startActivity(MainActivity)
→ Task A = [MainActivity]
   DetailActivity và EditActivity bị pop và destroy
   MainActivity.onNewIntent() được gọi
```

**Dùng cho**: `MainActivity` / `HomeActivity` — **màn hình "trung tâm"** mà mọi luồng đều quay về.

### launchMode=`singleInstance`

**Quy tắc**:

- Giống `singleTask`, chỉ 1 instance tồn tại, nằm ở root của Task riêng biệt, clear stack phía trên mỗi lần gọi.
- **Task cô lập**: Task này **không cho phép bất kỳ `Activity` nào khác** được đặt trong cùng Task đó.
  > _Mọi `Activity` được gọi từ instance này được chạy trong **Task** khác_
- **_Ít dùng trong thực tế_**

**Dùng cho**: Các trường hợp đặc biệt — màn hình gọi điện đến (incoming call screen), nơi cần hoàn toàn tách biệt khỏi luồng app chính.

---

## 3. **`Intent` _flags_**

### 3.1. Intent **flags**, `addFlags()` / set `flags`

**Flags** kiểm soát **cách Activity được thêm vào Back Stack** — tương tự `launchMode` nhưng **linh hoạt hơn** vì set per-call (**trong `code`**) thay vì cố định trong **Manifest**.

```kotlin
// FLAG_ACTIVITY_NEW_TASK
// Tạo Task mới hoặc reuse Task đang có của Activity đó
val intent = Intent(this, MainActivity::class.java)
intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
startActivity(intent)
```

- `launchMode`: phù hợp khi **behavior này không bao giờ thay đổi** — _ví dụ `MainActivity` luôn cần **unique instance**_.
- **Intent Flags**: phù hợp cho **behavior THEO NGỮ CẢNH** — _ví dụ: đôi khi muốn tạo instance mới, đôi khi muốn reuse_.

> _Trong dự án **thực tế**, **Intent Flags được dùng nhiều hơn** vì linh hoạt hơn — phần lớn `Activity` giữ `launchMode="standard"` (mặc định, không cần khai báo), và xử lý các trường hợp đặc biệt bằng Flag tại nơi gọi._

### 3.2. Các **flags** phổ biến

#### 3.2.1. Nhóm **Task Management** flags

1. **`FLAG_ACTIVITY_NEW_TASK`**

   ```kotlin
   intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
   ```

   **Ý nghĩa**: `Activity` đích sẽ trở thành **root** của một **Task mới**, hoặc _**Task của Activity đã tồn tại** được đưa lên trước_

   **Bắt buộc khi**: `startActivity()` được gọi từ **Context không phải `Activity`** (ví dụ: `Service`, `BroadcastReceiver`), vì không có Task hiện tại để push vào.

   ```kotlin
   // Trong BroadcastReceiver
   class MyReceiver : BroadcastReceiver() {
       override fun onReceive(context: Context, intent: Intent) {
           val launchIntent = Intent(context, MainActivity::class.java)
           launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK  // BẮT BUỘC
           context.startActivity(launchIntent)
       }
   }
   ```

2. **`FLAG_ACTIVITY_CLEAR_TASK`**

   ```kotlin
   intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
   ```

   **Ý nghĩa**: Xóa **toàn bộ** `Activity` hiện có trong **Task đích** trước khi đặt `Activity` mới vào. `Activity` mới trở thành **root** duy nhất.

   **Yêu cầu**: Luôn phải đi kèm `FLAG_ACTIVITY_NEW_TASK` — nếu thiếu, **flag này bị ignore**.

   ```
   Trước: [SplashActivity, LoginActivity, MainActivity, SettingsActivity]

   startActivity(LoginActivity) với NEW_TASK | CLEAR_TASK:
   Sau:   [LoginActivity]
   ```

   **Dùng cho**: trường hợp cần **xóa sạch session**, không cho Back về màn hình trước, ex: _sau khi đăng xuất / đăng nhập_.

3. **`FLAG_ACTIVITY_CLEAR_TOP`**

   ```kotlin
   intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
   ```

   **Ý nghĩa**:
   - Nếu `Activity` đích **đã có** trong _stack hiện tại_ — **pop toàn bộ Activity phía trên** nó.
   - Nếu `Activity` đích **chưa có** → hành xử như bình thường (tạo mới).

   > **Note**: _Mặc định_, `Activity` đích sẽ bị **destroy rồi tạo lại** (gọi `onCreate()` mới). **Để `Activity` đích không bị recreate** mà chỉ nhận `onNewIntent()` → kết hợp với **`FLAG_ACTIVITY_SINGLE_TOP`**.

   ```
   Stack: [A, B, C, D]

   startActivity(B) với CLEAR_TOP (không SINGLE_TOP):
   → [A, B']   C, D bị destroy. B cũ cũng bị destroy, B' (instance mới) được tạo

   startActivity(B) với CLEAR_TOP | SINGLE_TOP:
   → [A, B]    C, D bị destroy. B giữ nguyên instance, chỉ onNewIntent()
   ```

   **Dùng cho**: `Deep link`, `notification` dẫn về **một màn hình đã có** trong stack — quay lại

4. **`FLAG_ACTIVITY_SINGLE_TOP`**

   ```kotlin
   intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
   ```

   **Ý nghĩa**: giống `launchMode="singleTop"`, nếu `Activity` đích đã ở **top** của Task → **reuse**, nếu không → tạo mới.

   ```
   Stack: [A, B, C]   C ở top

   startActivity(C) với SINGLE_TOP:
   → [A, B, C]   reuse C, onNewIntent()

   startActivity(A) với SINGLE_TOP:
   → [A, B, C, A]   A không ở top → tạo mới (flag không có tác dụng)
   ```

#### 3.2.2. Nhóm **History & Recent** flags

1. **`FLAG_ACTIVITY_NO_HISTORY`**

   ```kotlin
   intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
   ```

   **Ý nghĩa**: `Activity` này sẽ **không được lưu vào Back Stack**. Khi user rời khỏi `Activity` này (bằng cách nhấn **Back** hoặc chuyển app), `Activity` sẽ bị destroy, **gọi `finish()` tự động** và **không thể quay lại**

   **Dùng cho**: `Splash` Screen, màn hình `OTP`/`Verification` **dùng một lần**, màn hình thông báo tạm.

2. **`FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS`**

   ```kotlin
   intent.flags = Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
   ```

   **Ý nghĩa**: Task chứa `Activity` này sẽ **không hiển thị** trong màn hình **Recent Apps** (Overview).

   **Dùng cho**: `Activity` hiển thị **thông tin nhạy cảm** (mã PIN, thông tin thanh toán) mà **không muốn xuất hiện** dưới dạng screenshot trong **Recent Apps**.

3. **`FLAG_ACTIVITY_RETAIN_IN_RECENTS`**

   ```kotlin
   intent.flags = Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS
   ```

   **Ý nghĩa**: Ngược lại với `NO_HISTORY` về mặt **Recents** — **giữ Task trong Recent Apps** dù `Activity` dùng `NO_HISTORY` hoặc các flag khác _thường loại trừ khỏi Recents_.

   **Dùng cho**: **Hiếm** gặp — các trường hợp đặc biệt cần giữ Task hiển thị trong Recents.

#### 3.2.3. Nhóm **Reordering** flags

1. **`FLAG_ACTIVITY_REORDER_TO_FRONT`**

   ```kotlin
   intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
   ```

   **Ý nghĩa**: Nếu `Activity` đích **đã tồn tại** trong stack (ở vị trí bất kỳ) — **di chuyển nó lên top**, **_KHÔNG pop_** các `Activity` khác, **_KHÔNG recreate_**.

   ```
   Stack: [A, B, C, D]

   startActivity(B) với REORDER_TO_FRONT:
   → [A, C, D, B]   B move lên top, C và D vẫn còn (chỉ đổi vị trí)

   startActivity(B) với CLEAR_TOP:
   → [A, B]         C và D bị xóa hoàn toàn
   ```

   **Dùng cho**: **Multi-tasking** trong app — **chuyển đổi** giữa các **"phiên làm việc"** mà **không mất `state`** của bất kỳ phiên nào.

#### 3.2.4. Nhóm **Result Forwarding** flags

1. **`FLAG_ACTIVITY_FORWARD_RESULT`**

   ```kotlin
   intent.flags = Intent.FLAG_ACTIVITY_FORWARD_RESULT
   ```

   **Ý nghĩa**: Nếu `Activity` hiện tại (`A`) đang **chờ kết quả** (được mở bằng `registerForActivityResult + launch`), và **`A` mở Activity `B` với flag này** — thì khi **`B` gọi `setResult()`**, kết quả sẽ được **chuyển thẳng về caller gốc của `A`**, _không phải về A_.

   ```
   X mở A (chờ result)
   A mở B với FORWARD_RESULT, A.finish()
   B gọi setResult(RESULT_OK, data)
   → X nhận result trực tiếp (A bị bỏ qua hoàn toàn)
   ```

   **Dùng cho**: Activity **trung gian** (`redirect`/`proxy` pattern) — hiếm gặp trong app thông thường

#### 3.2.5. Nhóm **Documents & Multi-window** flags (_ít dùng trong app cơ bản_)

- `FLAG_ACTIVITY_NEW_DOCUMENT`: Tạo **Task riêng** cho mỗi "document" (Recent Apps hiện nhiều card)
- `FLAG_ACTIVITY_MULTIPLE_TASK`: Cho phép **nhiều Task cùng `affinity`** (kết hợp `NEW_TASK`)
- `FLAG_ACTIVITY_LAUNCH_ADJACENT`: Trong chế độ **multi-window**, mở `Activity` ở **split-screen pane** bên cạnh (`tablet`/`foldable`)

---

## Tổng hợp:

```
══════════════════════════════ LAUNCHMODE (Manifest) ══════════════════════════════

standard        │ Luôn tạo mới                          │ Mặc định, Detail/Edit screens
singleTop       │ Reuse nếu đang ở TOP                  │ Notification, Search
singleTask      │ 1 instance/hệ thống, clear stack trên │ MainActivity/Home
singleInstance  │ Như singleTask + Task hoàn toàn riêng │ Incoming call screen

══════════════════════════════ INTENT FLAGS (Code) ════════════════════════════════

┌─ Task Management ─────────────────────────────────────────────────────────────┐
│ NEW_TASK              │ Tạo/reuse Task riêng    │ Bắt buộc khi start từ non-Activity│
│ NEW_TASK + CLEAR_TASK │ Xóa hết stack, fresh    │ Login/Logout                     │
│ CLEAR_TOP             │ Pop activity phía trên  │ Deep link (kèm SINGLE_TOP)        │
│ SINGLE_TOP            │ Reuse nếu đang ở TOP    │ Notification tap                  │
└──────────────────────────────────────────────────────────────────────────────┘

┌─ History & Recents ────────────────────────────────────────────────────────────┐
│ NO_HISTORY            │ Không lưu vào stack     │ Splash, OTP screen                │
│ EXCLUDE_FROM_RECENTS  │ Ẩn khỏi Recent Apps     │ Màn hình nhạy cảm                  │
│ RETAIN_IN_RECENTS     │ Giữ trong Recents       │ Hiếm dùng                          │
└──────────────────────────────────────────────────────────────────────────────┘

┌─ Reordering ───────────────────────────────────────────────────────────────────┐
│ REORDER_TO_FRONT      │ Move lên top, giữ stack │ Multi-session trong app           │
└──────────────────────────────────────────────────────────────────────────────┘

┌─ Result Forwarding ────────────────────────────────────────────────────────────┐
│ FORWARD_RESULT        │ Chuyển result tiếp      │ Activity trung gian (hiếm)        │
└──────────────────────────────────────────────────────────────────────────────┘
```
