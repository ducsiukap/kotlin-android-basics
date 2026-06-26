package com.vduczz.s5intents.implicitintent

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vduczz.s5intents.databinding.ActivityDestinationImplicitBinding

class DestinationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDestinationImplicitBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDestinationImplicitBinding.inflate(layoutInflater)
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

        // handle incoming intent
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            binding.tvSharedText.text = "Shared Text: $it"
        }
    }
}