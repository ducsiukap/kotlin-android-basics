package com.vduczz.s3viewbinding

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class FindViewByIdActivity : AppCompatActivity() {

    // without viewBind -> should to
    // pre-declare view's component
    private lateinit var tvCount: TextView
    private lateinit var btnIncreaseCount: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // to resolve view
        // -> use R class
        setContentView(R.layout.common_layout)
        // -> or findViewById
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // initialize view's component
        tvCount = findViewById(R.id.tv_count_1)
        btnIncreaseCount = findViewById(R.id.btn_increase_count_1)

        // -> findViewById(id): KHÔNG AN TOÀN, dễ nhầm, không bind trực tiếp với layout
        // -> recommended to use viewBinding

        setupUi()
    }

    private fun setupUi() {
        tvCount.text = "Count: 0"

        btnIncreaseCount.setOnClickListener {
            // increase count
            COUNT++
            tvCount.text = "Count: $COUNT"
        }
    }

    companion object {
        private var COUNT = 0
    }

}