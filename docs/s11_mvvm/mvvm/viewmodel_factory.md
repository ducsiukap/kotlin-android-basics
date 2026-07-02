# `ViewModelProvider.Factory` - ViewModel with **parameters**

## 1. **What** is the **`ViewModelProvider`**

Delegate `by viewModels()` thực chất là **syntactic sugar** — bên dưới nó dùng `ViewModelProvider`. <br/>
**Code tương đương với cách viết tường minh**:

```kotlin
// Cách viết tường minh — KHÔNG dùng delegate
// by viewModels() = đoạn code trên, viết gọn lại
private val viewModel: UserViewModel by viewModels()

// Tương đương:
private val viewModel: UserViewModel by lazy {
    ViewModelProvider(this)[UserViewModel::class.java]
}
```

`by viewModels()` delegation — nó chỉ là **wrapper** gọi `ViewModelProvider` với đúng **scope**:

- `this` cho Activity
- `requireActivity()` cho **activityViewModels()**.

### Cơ chế **Get-or-Create** của `ViewModelProvider`

Khi sử dụng:

```kotlin
val provider = ViewModelProvider(this)
val viewModel = provider[UserViewModel::class.java]
```

Bên trong, `ViewModelProvider` thực hiện đúng **2 bước**:

- **Bước 1**: tìm kiếm `ViewModel` trong `ViewModelStore` của **scope**:
  - key = `"androidx.lifecycle.ViewModelProvider.DefaultKey:" + className`
  - **Tìm kiếm**: `ViewModelStore[key]`:
    - Nếu **CÓ**: return instance cũ.
    - Nếu không, sang **bước 2**.
- **Bước 2**: Tạo mới instance của `ViewModel` thông qua `Factory`

  ```kotlin
  // tạo mới instance của ViewModel thông qua Factory
  val newInstance = factory.create(UserViewModel::class.java)

  // đưa vào ViewModelStore
  ViewModelStore.map[key] = newInstance

  // trả về instance
  return newInstance
  ```

> _Đây chính là cơ chế giải thích **tại sao rotate màn hình không tạo `ViewModel` mới** — `ViewModelStore` được Android **retain**, nên một khi đã tạo, ở Bước 1 luôn tìm thấy instance cũ, Factory không bao giờ được gọi lại._

---

## 2. `Factory` interface

### 2.1. Vấn đề với `ViewModel` có **dependencies**

Thông thường, trong thực tế, `ViewModel` **CẦN dependencies**, thường luôn là `Repository`:

```kotlin
class UserViewModel(private val repository: UserRepository) : ViewModel() {
    // có constructor parameter
}
```

Khi này, khởi tạo bằng **delegation** sẽ **bị lỗi** bởi `viewModels()` không biết lấy các dependencies ở đâu để gọi constructor:

```kotlin
private val viewModel: UserViewModel by viewModels()
// ❌ CRASH — Android không biết lấy "repository" ở đâu để gọi constructor
// InstantiationException: UserViewModel does not have a no-arg constructor
```

### 2.2. Giải pháp - `ViewModelProvider.Factory`

```kotlin
interface ViewModelProvider.Factory {
    fun <T : ViewModel> create(modelClass: Class<T>): T
}
```

`Factory` là **interface** mà bạn có thể implement để tạo `ViewModel` với **parameters**. Nó chỉ có **một method duy nhất**, có nhiệm vụ:

- Nhận vào `Class<T>` đại diện cho **loại `ViewModel` cần tạo**.
- Trả về **instance của `ViewModel`** đã được khởi tạo với đầy đủ **dependencies**.

```kotlin
class UserViewModelFactory(
    private val repository: UserRepository
) : ViewModelProvider.Factory {


    // modelClass là Class object — Android truyền vào khi cần tạo
    // VD: modelClass = UserViewModel::class.java
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        // Cần check modelClass vì 1 Factory
        // có thể handle nhiều loại ViewModel.
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            // isAssignableFrom: check modelClass có phải là UserViewModel
            // hoặc parent class của nó hay không
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
```

Thực tế, `Factory` có thể handle nhiều loại `ViewModel` khác nhau:

```kotlin
class AppViewModelFactory(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(UserViewModel::class.java) ->
                UserViewModel(userRepository) as T

            modelClass.isAssignableFrom(PostViewModel::class.java) ->
                PostViewModel(postRepository) as T

            else -> throw IllegalArgumentException(
                "Unknown ViewModel class: ${modelClass.name}"
            )
        }
    }
}
```

Khi này, cần truyền `factory` vào `by viewModels()` để delegate biết cách tạo `ViewModel` với **dependencies**:

```kotlin
// Dùng chung 1 Factory cho nhiều màn hình
class UserActivity : AppCompatActivity() {
    private val userViewModel: UserViewModel by viewModels {
        AppViewModelFactory(userRepo, postRepo)
    }
}

class PostActivity : AppCompatActivity() {
    private val postViewModel: PostViewModel by viewModels {
        AppViewModelFactory(userRepo, postRepo)
    }
}
```

### 2.3. `ViewModelProvider.NewInstanceFactory` — Factory mặc định

Khi **gọi `by viewModels()` không truyền lambda**, Android dùng Factory mặc định:

```kotlin
// Source code rút gọn của Factory mặc định
object NewInstanceFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Dùng Reflection để gọi constructor không tham số
        return modelClass.getDeclaredConstructor().newInstance()
    }
}
```

Đây là lý do `by viewModels()` (không factory) **chỉ hoạt động với `ViewModel` có constructor rỗng**.

---

## 3. `AbstractSavedStateViewModelFactory` - Factory có **[`SavedStateHandle`](./savedstatehandle.md)**

---

## 4. **Generic** Factory & `viewModelFactory` delegate

### 4.1. **Generic** Factory

**Generic Factory** là pattern gọn hơn, hay dùng trong project nhỏ — **tránh** phải viết 1 **`Factory` class riêng** cho **mỗi `ViewModel`**:

```kotlin
class ViewModelFactory(
    // nhận vào lambda creator để tạo ViewModel
    private val creator: () -> ViewModel
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // gọi lambda creator để tạo ViewModel
        // ->   trao quyền định nghĩa cách tạo ViewModel cho caller
        //      thay vì tự implements
        return creator() as T
    }
}
```

Caller tự quyết định **cách tạo `ViewModel` cần sử dụng**:

```kotlin
// Mỗi Activity tự quyết định cách tạo ViewModel của mình
class UserActivity : AppCompatActivity() {
    private val viewModel: UserViewModel by viewModels {
        ViewModelFactory { UserViewModel(repository) }
    }
}

class PostActivity : AppCompatActivity() {
    private val viewModel: PostViewModel by viewModels {
        ViewModelFactory { PostViewModel(postRepository) }
    }
}
```

### 4.2. `viewModelFactory` DSL — Cách hiện đại nhất

Từ `lifecycle-viewmodel-ktx` bản mới, Google cung cấp **DSL gọn hơn** cả Generic Factory:

```kotlin
class UserActivity : AppCompatActivity() {

    private val viewModel: UserViewModel by viewModels {
        // factory DSL — gọn hơn Generic Factory
        viewModelFactory {
            // initialize ViewModel với dependencies
            initializer {
                val repo = (application as MyApplication).repository
                UserViewModel(repo)
            }
        }
    }
}
```

Cú pháp này **tránh hoàn toàn việc tự viết class Factory** — `initializer { }` **nhận lambda** và sau đó **trả về instance `ViewModel`**, tương tự Generic Factory nhưng là API chính thức từ Google.
