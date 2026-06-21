# Create **Navigation Graph** - `nav_graph.xml`

## 1. `nav_graph.xml` overview

`nav_graph.xml` là file XML đặt tại `res/navigation/` — mô tả toàn bộ **cấu trúc navigation của app**:

- màn hình nào tồn tại
- từ màn hình nào có thể đi đến màn hình nào
- truyền data gì khi đi.

### Có `4` loại **destination**: `<fragment>`, `<activity>`, `<dialog>`, `<navigation>`.

```xml
<?xml version="1.0" encoding="utf-8"?>

<!--
<navigation>: khai báo nav-graph
- :id -> ID của graph
- app:startDestination="@id/startDestination"
    -> start destination of graph
    // Bắt buộc phải có, chỉ được có một
-->
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/nav_graph"
    app:startDestination="@id/...">

    <!-- Nav Graph hỗ trợ 4 loại destination:
            - <fragment>: the most popular
            - <activity>
            - <dialog>
            - <navigation>: for nested navigation
     -->


    <!-- <fragment> là destination phổ biến nhất
        + :id       // ID duy nhất của destination trong graph.
                    -> Dùng để navigate và reference trong code.
        + :name     // Fully qualified class name của Fragment
                    -> NavController dùng tên này để instantiate Fragment.
        + :label    // tên hiển thị
                    -> dùng bởi setupActionBarWithNavController()
                        để set title của Toolbar tự động
        + :layout   //— Chỉ dùng lúc design time trong Android Studio
                    ->  cho Visual Editor biết layout nào để preview.
                        Không ảnh hưởng runtime.
    -->
    <fragment
        android:id="@+id/homeFragment"
        android:name="com.vduczz.navigationcomponent.HomeFragment"
        android:label="Trang chủ"
        tools:layout="@layout/fragment_home">

        <!--
         <argument ... />   : tham số nhận vào
                // Các tham số:
                // :name, :defaultValue, :argType, :nullable
         -->
        <argument />

        <!--
         <action .../>      : action gửi đi
        -->
        <action />

        <!--
        <deeplink .../>    : deep links vào fragment này
                // tham số: :action, :uri, :mimeType, :autoVerify
        -->
        <deepLink />

    </fragment>

    <!--
    <activity>: navigate tới activity khác
        // tham số: id, name, label, ...

        // Dùng khi app có multi-Activity và
        // cần navigate từ Fragment sang Activity khác
        // thông qua NavController thay vì Intent thủ công.
    -->
    <activity />

    <!--
    <dialog>: DialogFragment

        // Navigate đến DialogFragment —
        // dialog xuất hiện trên màn hình hiện tại
        // mà không replace Fragment đó.
    -->
    <dialog
        android:id="@+id/confirmDeleteDialog"
        android:name="com.example.app.ConfirmDeleteDialogFragment"
        android:label="Xác nhận xóa">

        <argument
            android:name="itemId"
            app:argType="string" />
    </dialog>

    <!--
    <navigation> : Nested Graph

        // Nested graph giúp nhóm các destination liên quan
        // thành một flow riêng biệt

    Lợi ích:
        - Tổ chức code rõ ràng hơn - auth flow tách biệt với main flow
        - Reuse — có thể include graph này vào nhiều graph khác
        - Encapsulation — các destination bên trong
                không bị expose ra ngoài graph
    -->
    <!-- nav_graph.xml (root) -->
    <navigation
        android:id="@+id/auth_graph"
        app:startDestination="@id/loginFragment">
        <!--
            <fragment android:id="@+id/loginFragment" ... />
            <fragment android:id="@+id/registerFragment" ... />
            <fragment android:id="@+id/forgotPasswordFragment" ... />
        -->
    </navigation>
</navigation>
```

---

## 2. Các thành phần trong destination

### 2.1. `<argument>`: khai báo **tham số** nhận vào

```xml
<fragment
    android:id="@+id/productDetailFragment"
    android:name="com.example.app.ProductDetailFragment">

    <!-- Required argument — không có default value -->
    <argument
        android:name="productId"
        app:argType="string" />

    <!-- Optional argument — có default value -->
    <argument
        android:name="productName"
        app:argType="string"
        android:defaultValue="Unknown" />

    <!-- Integer argument -->
    <argument
        android:name="quantity"
        app:argType="integer"
        android:defaultValue="1" />

    <!-- Boolean argument -->
    <argument
        android:name="isEditable"
        app:argType="boolean"
        android:defaultValue="false" />

    <!-- Nullable argument -->
    <argument
        android:name="imageUrl"
        app:argType="string"
        app:nullable="true"
        android:defaultValue="@null" />

    <!-- Enum / custom Parcelable -->
    <argument
        android:name="product"
        app:argType="com.example.app.model.Product" />

</fragment>
```

- `app:argType`: **kiểu dữ liệu** của tham số
  ```
  string        → String
  integer       → Int
  float         → Float
  boolean       → Boolean
  long          → Long
  reference     → @DrawableRes / @ColorRes / resource ID
  <classname>   → Parcelable hoặc Serializable custom class
  <enum>        → Enum class
  ```
- `android:defaultValue`:
  - nếu **có** → argument là **optional**, caller không cần truyền.
  - nếu **không có** → argument là **required**, caller bắt buộc phải truyền.
- `app:nullable="true"` — chỉ áp dụng cho `string` và **custom `class`** — cho phép giá trị `null`

### 2.2. `<action>` — Khai Báo Luồng Đi

`<action>` mô tả một đường đi cụ thể **từ destination này sang destination khác**.

```xml
<fragment
    android:id="@+id/homeFragment"
    android:name="com.example.app.HomeFragment">

    <action
        android:id="@+id/action_home_to_detail"
        app:destination="@id/productDetailFragment"

        <!-- Animation -->
        app:enterAnim="@anim/slide_in_right"
        app:exitAnim="@anim/slide_out_left"
        app:popEnterAnim="@anim/slide_in_left"
        app:popExitAnim="@anim/slide_out_right"

        <!-- Back stack behavior -->
        app:popUpTo="@id/homeFragment"
        app:popUpToInclusive="false"

        <!-- Launch behavior -->
        app:launchSingleTop="true" />

</action>
```

- `android:id` — ID của action, dùng trong code
  ```kotlin
  findNavController().navigate(R.id.action_home_to_detail)
  ```
- `app:destination` — ID của destination đích khi action được trigger
- **Animation**: có `4` thuộc tính:
  - `app:enterAnim` → animation của **destination MỚI** khi đi **TỚI**
  - `app:exitAnim` → animation của **destination HIỆN TẠI** khi đi **TỚI**
  - `app:popEnterAnim` → animation của **destination CŨ** khi nhấn **BACK**
  - `app:popExitAnim` → animation của **destination HIỆN TẠI** khi nhấn **BACK**

  ```
  Navigate tới:     [Home --exitAnim--> ] [--enterAnim--> Detail]
  Nhấn Back:        [Detail --popExitAnim--> ] [--popEnterAnim--> Home]
  ```

  Android cung **cấp sẵn một số animation** trong `androidx.navigation`:

  ```xml
  app:enterAnim="@anim/nav_default_enter_anim"
  app:exitAnim="@anim/nav_default_exit_anim"
  app:popEnterAnim="@anim/nav_default_pop_enter_anim"
  app:popExitAnim="@anim/nav_default_pop_exit_anim"
  ```

- **Back stack behavior**:
  - `popUpTo` — **pop tất cả** destination trong back stack cho **đến khi gặp destination được chỉ định**.
  - `popUpToInclusive` — nếu `true`, **destination được chỉ định cũng bị pop** luôn. Nếu `false`, destination đó vẫn còn trong back stack.
- `app:launchSingleTop="true"` — tương đương `launchMode="singleTop"`

#### **Global Action**

`<action>` dùng được từ **mọi nơi**. Khi khai báo `<action>` ở cấp **root `<navigation>`** thay vì bên trong một destination cụ thể — gọi là **global action**, có thể **trigger từ bất kỳ destination nào** trong graph:

```xml
<navigation
    android:id="@+id/nav_graph"
    app:startDestination="@id/homeFragment">

    <!-- Global action: từ bất kỳ đâu → về Home -->
    <action
        android:id="@+id/action_global_to_home"
        app:destination="@id/homeFragment"
        app:popUpTo="@id/nav_graph"
        app:popUpToInclusive="true" />

    <!-- Global action: từ bất kỳ đâu → mở Error screen -->
    <action
        android:id="@+id/action_global_error"
        app:destination="@id/errorFragment" />

    <fragment android:id="@+id/homeFragment" ... />
    <fragment android:id="@+id/detailFragment" ... />
    <fragment android:id="@+id/errorFragment" ... />

</navigation>
```

Có thể gọi từ bất kì `Fragment` nào:

```kotlin
// Gọi từ bất kỳ Fragment nào
findNavController().navigate(R.id.action_global_to_home)
findNavController().navigate(R.id.action_global_error)
```

### 2.3. `<deepLink>` — Khai Báo Deep Link

`<deepLink>` mô tả một đường dẫn URL cụ thể có thể **deep link vào destination này**.

```xml
<fragment
    android:id="@+id/productDetailFragment"
    android:name="com.example.app.ProductDetailFragment">

    <argument android:name="productId" app:argType="string" />

    <!-- URI Deep Link -->
    <deepLink
        android:id="@+id/deepLink_product"
        app:uri="https://example.com/product/{productId}"
        app:mimeType="*/*"
        app:action="android.intent.action.VIEW" />

    <!-- Custom scheme -->
    <deepLink
        app:uri="myapp://product/{productId}" />

</deepLink>
```

**`app:uri` — URI pattern**. `{productId}` là **placeholder** — tự động map sang **argument cùng tên**.

Các **URI pattern** hợp lệ:

```
https://example.com/product/{productId}   → https scheme
myapp://product/{productId}               → custom scheme
example.com/product/{productId}           → không có scheme → match cả http và https
```

Với `<deeplink>`, cần bắt buộc khai báo trong **Manifest** để android có thể handle:

```xml
<activity android:name=".MainActivity" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>

    <!-- Khai báo nav graph — tự động generate intent-filter cho deep links -->
    <nav-graph android:value="@navigation/nav_graph" />
</activity>
```