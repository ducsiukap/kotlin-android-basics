package com.vduczz.s3viewbinding

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vduczz.s3viewbinding.databinding.CommonLayoutBinding

class WithViewBindingActivity : AppCompatActivity() {

    // declare binding
    // from activity ref to view layout
    private lateinit var binding: CommonLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        /** initial binding
         * - with the existing view -> ...Binding.bind(view)
         * - without existing view -> ...Binding.inflate(inflater)
         */
        binding = CommonLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUi()
    }

    private fun setupUi() {

        // access to element via binding
        // -> compile-time, more safety than findViewBindId()
        binding.tvCount1.text = "Count: $COUNT"

        setupListeners()
    }

    private fun setupListeners() {
        // access to element via binding
        binding.btnIncreaseCount1.setOnClickListener {
            COUNT++

            binding.tvCount1.text = "Count: $COUNT"
        }
    }

    companion object {
        private var COUNT = 0
    }

}