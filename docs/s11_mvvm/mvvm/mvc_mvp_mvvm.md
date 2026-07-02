# **MVC**, **MVP** & **MVVM** patterns

## 1. **Why** need architecture patterns? - **God `Activity`** problem

### 1.1. God Activity

**God-Activity** là **`anti-pattern`** phổ biến với newbie Android developers. Nó xảy ra khi bạn **viết quá nhiều logic** trong `Activity` hoặc `Fragment`, dẫn đến code **khó maintain**, **test** và **scale**.

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Gọi API
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.example.com")
            .build()
        val api = retrofit.create(ApiService::class.java)

        // Xử lý response
        lifecycleScope.launch {
            val products = api.getProducts()
            binding.recyclerView.adapter = ProductAdapter(products)
        }

        // Validate form
        binding.btnSubmit.setOnClickListener {
            val name = binding.etName.text.toString()
            if (name.isEmpty()) {
                binding.tilName.error = "Required"
                return@setOnClickListener
            }
            // Lưu database
            val db = Room.databaseBuilder(this, AppDatabase::class.java, "app.db").build()
            lifecycleScope.launch {
                db.productDao().insert(Product(name = name))
            }
        }

        // Xử lý UI state
        binding.btnFilter.setOnClickListener {
            // filter logic...
        }
    }
}
```

### 1.2. **Hậu quả**:

- **Vấn đề 1: Khó test**: Không thể unit test logic vì nó gắn chặt với Android UI
- **Vấn đề 2: Khó maintain**: Khi logic tăng lên, `Activity` trở nên quá dài, khó đọc và khó sửa
- **Vấn đề 3: Khó scale**: Khi muốn thêm tính năng mới, bạn phải sửa `Activity`, dẫn đến rủi ro phá vỡ các tính năng hiện có
- **Vấn đề 4: Data mất khi configuration change**
- **Vấn đề 5: Không tái sử dụng code**: Logic bị gắn chặt với `Activity`, không thể tái sử dụng ở nơi khác
- **Vấn đề 6: Memory leak**: Nếu bạn giữ reference đến `Activity` trong các callback, giả sử network callback, sẽ dẫn đến memory leak

### 1.3. **Seperation of Concerns** — Nguyên tắc cốt lõi

**Android Architecture** được xây dựng trên nguyên tắc **Separation of Concerns (`SoC`)** — mỗi class chỉ chịu trách nhiệm cho **một việc duy nhất**:

- `Activity`/`Fragment`: Chịu trách nhiệm **UI** và **lifecycle**, <br/>
  KHÔNG call api, query db, ... một cách trực tiếp
- `ViewModel`: Chịu trách nhiệm **business/UI logic**, **states** & **data**, <br/>
  KHÔNG biết gì về View
- `Repository`: Chịu trách nhiệm **data source** (network, database, cache), <br/>
  KHÔNG biết gì về UI
- `Data` layer: `Room`, `Retrofit`, `DataStore`, etc. <br/>
  KHÔNG biết gì về UI, business logic

---

## 2. **Histories** of Android **Architecture Patterns**

### 2.1. **`MVC`** (Model-View-**Controller**)

**MVC** là **`design pattern`** đầu tiên, phổ biến trong **web development**. Nó chia ứng dụng thành 3 phần:

```text
┌─────────┐    User Input    ┌────────────┐
│  View   │ ───────────────► │ Controller │
│  (UI)   │                  │            │
│         │ ◄─────────────── │            │
└─────────┘   Update View    └────────────┘
                                    │
                              Read/Write
                                    │
                             ┌──────▼─────┐
                             │   Model    │
                             │   (Data)   │
                             └────────────┘
```

Trong **`Android`**:

- **View**: XML layout
- **Controller**: `Activity`/`Fragment`
- **Model**: Data classes, db, ...

**Vấn đề**: **Controller** cần biết quá nhiều:

- Biết về **View**: inflate layout, update UI, handle user input, ...
- Biết về **Model**: call API, query db, ...
- Biết xử lý user input, validation, business logic, ...

-> `Activity`/`Fragment` trở thành **God Activity**.

### 2.2. **`MVP`** (Model-View-**Presenter**)

**`MVP`** là **`design pattern`** được phát triển từ **MVC**, nhằm giải quyết vấn đề **God Activity - tách logic ra khỏi `Activity`**.<br/>
Nó chia ứng dụng thành 3 phần:

```text
┌─────────┐   Interface   ┌───────────┐   Interface   ┌─────────┐
│  View   │ ◄──────────── │ Presenter │ ────────────► │  Model  │
│(Activity│               │           │               │  (Data) │
│Fragment)│ ────────────► │           │ ◄──────────── │         │
└─────────┘  User Action  └───────────┘  Data Result  └─────────┘
```

#### **Implementation**

- **View**:

  ```kotlin
  // View interface — Activity implement
  interface LoginView {
      fun showLoading()
      fun hideLoading()
      fun onLoginSuccess(user: User)
      fun onLoginError(message: String)
  }

  // Activity
  class LoginActivity : AppCompatActivity(), LoginView {
      private val presenter = LoginPresenter(this)

      override fun showLoading() { binding.progressBar.visibility = View.VISIBLE }
      override fun onLoginSuccess(user: User) { navigateToHome() }
  }
  ```

- **Presenter**:

  ```kotlin
  // Presenter — không phụ thuộc Android
  class LoginPresenter(private val view: LoginView) {
      fun login(username: String, password: String) {
          view.showLoading()
          // gọi API...
          view.onLoginSuccess(user)
      }
  }
  ```

#### MVP's problems

- `Presenter` vẫn **giữ reference** đến `View` -> Memory leak nếu không xử lý cẩn thận
- Khi `Activity` bị **recreated** (ví dụ: rotate screen), `Presenter` mất **states**
- `interface` quá nhiều **boilerplate code**
- Khó handle **lifecycle**

### 2.3. **`MVVM`** (Model-View-**ViewModel**)

**`MVVM`** là **`design pattern`** là pattern hiện đại, được **Google chính thức khuyến nghị từ 2017**:

```text
┌─────────────┐              ┌─────────────┐
│    VIEW     │  observes    │  VIEWMODEL  │
│  Activity   │ ◄─────────── │             │
│  Fragment   │              │  LiveData/  │
│             │  user events │  StateFlow  │
│             │ ───────────► │             │
└─────────────┘              └──────┬──────┘
                                    │
                               calls│
                                    │
                             ┌──────▼──────┐
                             │ REPOSITORY  │
                             │             │
                             └──────┬──────┘
                                    │
                         ┌──────────┼──────────┐
                         │          │          │
                    ┌────▼───┐ ┌────▼───┐ ┌────▼────┐
                    │  Room  │ │Retrofit│ │DataStore│
                    └────────┘ └────────┘ └─────────┘
```

Điểm **khác biệt cốt lõi** giữa **`MVVM`** và **`MVP`**:

|               | MVP                                           | MVVM                                                     |
| ------------- | --------------------------------------------- | -------------------------------------------------------- |
| **Reference** | `Presenter` **giữ reference** đến `View`      | `ViewModel` **không biết** đến sự tồn tại của `View`     |
| **State**     | `Presenter` **mất state** khi `View` recreate | `ViewModel` **states SURVIVE** qua configuration changes |

---

## 3. **`3` layers** in **MVVM** architecture

Google đề xuất **`3` layers** chính **MVVM / Android** architecture:

- **`UI` layer**: `Activity`/`Fragment`, `Adapter` + `ViewModel`<br/>Chịu trách nhiệm:
  - UI - `Activity`/`Fragment`: **observe states** và **gửi event**
  - `ViewModel`: giữ Ui states, xử lý business logic nhẹ, là cầu nối giữa `View` và `Repository`
- **`Domain` layer** (_optional_): là các **Use cases / Interactors** chứa business logic phức tạp.
  > _Chỉ cần khi app rất lớn_
- **Data layer**: `Repository`,**Data Sources** - `Room`, `Retrofit`, etc. <br/>
  Nhiệm vụ: quản lý data, lưu trữ, sync, không được biết tới UI

---

## 4. **`Repository` pattern**

**MVVM** thuần chỉ có 3 tàng. Nhưng thực tế, **`Repository`** luôn được thêm vào giữa `ViewModel` và `Data Sources` để **tách biệt concerns**:

```kotlin
class UserRepository(
    private val dao: UserDao,          // Room
    private val apiService: ApiService  // Retrofit
) {
    // Repository quyết định lấy từ đâu — ViewModel không biết
    fun getAllUsers(): Flow<List<User>> = dao.getAllUsers()

    suspend fun syncFromNetwork() {
        val users = apiService.getUsers()
        dao.insertAll(users)
    }
}
```

- **`ViewModel`**: Chỉ quan tâm đến **UI states** và **business logic**, gọi `repository` để request data.

  ```kotlin
  class UserViewModel(private val repository: UserRepository) : ViewModel() {

      // ViewModel không biết data đến từ Room hay API
      val users = repository.getAllUsers()
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
  }
  ```

- **Data source**: lưu trữ, đồng bộ, cung cấp data thông qua `Repository`.
