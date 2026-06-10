# `findViewById()` & its problems

Trước khi có **View Binding**, cách **duy nhất** để truy cập các View trong layout là sử dụng `findViewById()`:

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Truy cập View thông qua
        // findViewById() với ID của View
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val etEmail  = findViewById<EditText>(R.id.etEmail)
        val tvTitle  = findViewById<TextView>(R.id.tvTitle)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
        }
    }
}
```

Tuy nhiên, `findViewById()` có **`3` vấn đề nghiêm trọng**:

### **Vấn đề 1**: Null safety

Hàm `findViewById()` trả về `null` nếu ID không tồn tại.

> _Do vậy, nếu không được xử lý cẩn thận, việc truy cập View sẽ gây ra lỗi `NullPointerException` tại runtime_.

### **Vấn đề 2**: Type safety

Với `findViewById()`, bạn phải **ép kiểu thủ công** (cast) về đúng loại View, nếu sai kiểu sẽ gây ra lỗi `ClassCastException` tại runtime.

```kotlin
// ID đúng nhưng kiểu sai → compile thành công → crash lúc chạy
val tv = findViewById<Button>(R.id.tvTitle)  // tvTitle là TextView, cast thành Button
tv.text = "Hello"  // ClassCastException
```

### **Vấn đề 3**: Boilerplate

**Mỗi `View`** cần một dòng `findViewById()`. Activity/Fragment phức tạp có thể có hàng chục View → code dài, dễ nhầm.

```kotlin
val tvTitle       = findViewById<TextView>(R.id.tvTitle)
val tvSubtitle    = findViewById<TextView>(R.id.tvSubtitle)
val etEmail       = findViewById<EditText>(R.id.etEmail)
val etPassword    = findViewById<EditText>(R.id.etPassword)
val cbRememberMe  = findViewById<CheckBox>(R.id.cbRememberMe)
val btnLogin      = findViewById<Button>(R.id.btnLogin)
val tvRegister    = findViewById<TextView>(R.id.tvRegister)
val ivLogo        = findViewById<ImageView>(R.id.ivLogo)
// ... còn nữa
```
