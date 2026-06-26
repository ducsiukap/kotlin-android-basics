package com.vduczz.s5intents.activityresult

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vduczz.s5intents.databinding.ActivityDestinationResultBinding

class DestinationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDestinationResultBinding

    companion object {
        const val EXTRA_UPDATED_NAME = "updated_name"

        fun newIntent(context: Context, currentName: String) = Intent(
            context, DestinationActivity::class.java
        ).apply {
            putExtra(EXTRA_UPDATED_NAME, currentName)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDestinationResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                systemBarsInsets.bottom
            )
            insets
        }

        val currentName = intent.getStringExtra(EXTRA_UPDATED_NAME) ?: ""
        binding.etName.setText(currentName)

        binding.btnSaveName.setOnClickListener {
            val updatedName = binding.etName.text.toString()
            val resultIntent = Intent().apply {
                putExtra(EXTRA_UPDATED_NAME, updatedName)
            }

            // setResult(resultCode, resultIntent?)
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        binding.btnCancel.setOnClickListener {
//            setResult(RESULT_CANCELED)
            finish() // finish mà chưa setResult -> resultCode=CANCEL
        }
    }
}